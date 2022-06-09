/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eusubsidycompliance.connectors

import play.api.http.Status.{NOT_ACCEPTABLE, NOT_FOUND}
import play.api.{Logger, Mode}
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisParamValue, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.{CreateUndertakingApiRequest, RetrieveUndertakingAPIRequest, UpdateUndertakingApiRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EisConnector @Inject() (
  val http: HttpClient,
  val mode: Mode,
  val servicesConfig: ServicesConfig,
  val auditing: AuditConnector
) extends DesHelpers {

  private val logger: Logger = Logger(this.getClass)
  private lazy val eisURL: String = servicesConfig.baseUrl("eis")

  private val retrieveUndertakingPath = "scp/retrieveundertaking/v1"
  private val createUndertakingPath = "scp/createundertaking/v1"
  private val updateUndertakingPath = "scp/updateundertaking/v1"
  private val amendBusinessEntityPath = "scp/amendundertakingmemberdata/v1"
  private val amendSubsidyPath = "scp/amendundertakingsubsidyusage/v1"
  private val retrieveSubsidyPath = "scp/getundertakingtransactions/v1"

  def retrieveUndertaking(
    eori: EORI
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ConnectorError, Undertaking]] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.retrieveUndertakingResponseReads

    val eisTokenKey = "eis.token.scp04"
    val retrieveUndertakingRequest = RetrieveUndertakingAPIRequest(eori)
    desPost[RetrieveUndertakingAPIRequest, Undertaking](
      s"$eisURL/$retrieveUndertakingPath",
      retrieveUndertakingRequest,
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
      .map(Right(_))
      .recover {
        case e: EisBadResponseException if e.code == EisParamValue("107") =>
          logger.info(s"No undertaking found for $eori")
          Left(ConnectorError(NOT_FOUND, s"Undertaking reference in the API not subscribed in ETMP."))

        case e: EisBadResponseException if e.code == EisParamValue("055") =>
          logger.info(s" Eori : $eori does not exist in ETMP")
          Left(ConnectorError(NOT_ACCEPTABLE, s"Eori : $eori does not exist in ETMP"))
      }
  }

  def createUndertaking(
    undertaking: Undertaking
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingRef] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{createUndertakingResponseReads}

    val eisTokenKey = "eis.token.scp02"
    desPost[CreateUndertakingApiRequest, UndertakingRef](
      s"$eisURL/$createUndertakingPath",
      CreateUndertakingApiRequest(undertaking),
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def updateUndertaking(
    undertaking: Undertaking,
    amendmentType: EisAmendmentType
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingRef] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.updateUndertakingResponseReads

    val eisTokenKey = "eis.token.scp12"
    desPost[UpdateUndertakingApiRequest, UndertakingRef](
      s"$eisURL/$updateUndertakingPath",
      UpdateUndertakingApiRequest(undertaking, amendmentType),
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def addMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity,
    amendmentType: AmendmentType
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{amendUndertakingMemberResponseReads}

    val eisTokenKey = "eis.token.scp05"

    val result = desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingIdentifier = undertakingRef,
        memberAmendments = List(BusinessEntityUpdate(amendmentType, LocalDate.now(), businessEntity))
      ),
      eisTokenKey
    )(
      implicitly,
      readFromJson(amendUndertakingMemberResponseReads, implicitly[Manifest[Unit]]),
      addHeaders,
      implicitly
    )
    result
  }

  def deleteMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{amendUndertakingMemberResponseReads}

    val eisTokenKey = "eis.token.scp05"

    desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingRef,
        undertakingComplete = true,
        List(BusinessEntityUpdate(AmendmentType.delete, LocalDate.now(), businessEntity))
      ),
      eisTokenKey
    )(
      implicitly,
      readFromJson(amendUndertakingMemberResponseReads, implicitly[Manifest[Unit]]),
      addHeaders,
      implicitly
    )
  }

  def upsertSubsidyUsage(
    subsidyUpdate: SubsidyUpdate
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendSubsidyUpdateResponseReads

    val eisTokenKey = "eis.token.scp06"

    desPost[SubsidyUpdate, Unit](
      s"$eisURL/$amendSubsidyPath",
      subsidyUpdate,
      eisTokenKey
    )(implicitly, readFromJson(amendSubsidyUpdateResponseReads, implicitly[Manifest[Unit]]), addHeaders, implicitly)
  }

  def retrieveSubsidies(
    ref: UndertakingRef,
    dateRange: Option[(LocalDate, LocalDate)]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingSubsidies] = {

    import uk.gov.hmrc.eusubsidycompliance.models.UndertakingSubsidies.eisRetrieveUndertakingSubsidiesResponseRead

    val eisTokenKey = "eis.token.scp09"

    val defaultDateRange = Some((LocalDate.of(2000, 1, 1), LocalDate.now()))

    desPost[SubsidyRetrieve, UndertakingSubsidies](
      s"$eisURL/$retrieveSubsidyPath",
      SubsidyRetrieve(ref, dateRange.orElse(defaultDateRange)),
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

}
