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


import play.api.libs.json.Writes
import play.api.{Logger, Mode}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{EisBadResponseException, updateUndertakingWrites}
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisParamValue, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EisConnector @Inject()(
  val http: HttpClient,
  val mode: Mode,
  val servicesConfig: ServicesConfig,
  val auditing: AuditConnector,
  ec: ExecutionContext
) extends DesHelpers {

  val logger: Logger = Logger(this.getClass)
  val eisURL: String = servicesConfig.baseUrl("eis")

  val retrieveUndertakingPath = "scp/retrieveundertaking/v1"
  val createUndertakingPath = "scp/createundertaking/v1"
  val updateUndertakingPath = "scp/updateundertaking/v1"
  val amendBusinessEntityPath = "scp/amendundertakingmemberdata/v1"

  val amendSubsidyPath = "scp/amendundertakingsubsidyusage/v1"
  val retrieveSubsidyPath = "scp/getundertakingtransactions/v1"

  def retrieveUndertaking(
    eori: EORI
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Undertaking] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{retrieveUndertakingEORIWrites, undertakingFormat}

    val eisTokenKey = "eis.token.scp04"
    desPost[EORI, Undertaking](
      s"$eisURL/$retrieveUndertakingPath",
      eori, eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly).recover {
      case e:EisBadResponseException if e.code == EisParamValue("107") =>
        logger.info(s"No undertaking found for $eori")
        throw UpstreamErrorResponse.apply("undertaking not found", 404)
    }
  }

  def createUndertaking(
    undertaking: Undertaking
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UndertakingRef] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{undertakingCreateResponseReads, undertakingFormat}
    val eisTokenKey = "eis.token.scp02"
    desPost[Undertaking, UndertakingRef](
      s"$eisURL/$createUndertakingPath",
      undertaking, eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def updateUndertaking(undertaking: Undertaking
                       )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingRef]= {

   import uk.gov.hmrc.eusubsidycompliance.models.json.digital.undertakingUpdateResponseReads
   val updateWrites: Writes[Undertaking] = updateUndertakingWrites()
    val eisTokenKey = "eis.token.scp03"
    desPost[Undertaking, UndertakingRef](
      s"$eisURL/$updateUndertakingPath",
      undertaking, eisTokenKey
    )(updateWrites, implicitly, addHeaders, implicitly)
  }

  def addMember(
     undertakingRef: UndertakingRef,
     businessEntity: BusinessEntity,
     amendmentType: AmendmentType
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{amendUndertakingMemberDataResponseReads, amendUndertakingMemberDataWrites}

    val eisTokenKey = "eis.token.scp05"
    desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingRef,
        true,
        List(BusinessEntityUpdate(amendmentType, LocalDate.now(), businessEntity))), eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def deleteMember(
     undertakingRef: UndertakingRef,
     businessEntity: BusinessEntity
   )(
     implicit hc: HeaderCarrier,
     ec: ExecutionContext
   ): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.{amendUndertakingMemberDataResponseReads, amendUndertakingMemberDataWrites}

    val eisTokenKey = "eis.token.scp05"
    desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingRef,
        true,
        List(BusinessEntityUpdate(AmendmentType.delete, LocalDate.now(), businessEntity))), eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def updateSubsidy(
    subsidyUpdate: SubsidyUpdate
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendSubsidyResponseReads

    val eisTokenKey = "eis.token.scp06"
    desPost[SubsidyUpdate, Unit](
      s"$eisURL/$amendSubsidyPath",
      subsidyUpdate, eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def retrieveSubsidies(
                       ref: UndertakingRef
                       )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
                       ): Future[UndertakingSubsidies] = {
    val eisTokenKey = "eis.token.scp09"
    import uk.gov.hmrc.eusubsidycompliance.models.json.eis.eisRetrieveUndertakingSubsidiesResponseWrite

    desPost[SubsidyRetrieve, UndertakingSubsidies](
      s"$eisURL/$retrieveSubsidyPath",
      SubsidyRetrieve(
        ref,
        Some((LocalDate.of(2000, 1, 1), LocalDate.now()))
      ), eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

}
