/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{NOT_ACCEPTABLE, NOT_FOUND}
import play.api.libs.json.Json
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisParamValue, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.{CreateUndertakingApiRequest, GetUndertakingBalanceApiResponse, GetUndertakingBalanceRequest, RetrieveUndertakingAPIRequest, UpdateUndertakingApiRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.net.URL
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EisConnector @Inject() (
  val http: HttpClientV2,
  val servicesConfig: ServicesConfig
) extends DesHelpers
    with Logging {

  private lazy val eisURL: String = servicesConfig.baseUrl("eis")
  private val amendBusinessEntityPath = "scp/amendundertakingmemberdata/v1"

  def retrieveUndertaking(
    eori: EORI
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ConnectorError, UndertakingRetrieve]] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.retrieveUndertakingResponseReads
    val retrieveUndertakingPath = "scp/retrieveundertaking/v1"

    val eisTokenKey = "eis.token.scp04"
    val retrieveUndertakingRequest = RetrieveUndertakingAPIRequest(eori)

    val jsValue = RetrieveUndertakingAPIRequest.writes.writes(retrieveUndertakingRequest)

    logger.info(s"eori: $eori : ${jsValue.toString()}")

    val notFoundEisErrorCode = "107"
    val unacceptableEisErrorCode = "055"

    desPost[RetrieveUndertakingAPIRequest, HttpResponse](
      s"$eisURL/$retrieveUndertakingPath",
      retrieveUndertakingRequest,
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
      .map { response =>
        val body = response.body
        logger.info(s"Received the following Json Body for SCP04 - $body")
        Right(Json.parse(body).as[UndertakingRetrieve])
      }
      .recover {
        case e: EisBadResponseException if e.code == EisParamValue(notFoundEisErrorCode) =>
          logger.info(
            s"retrieveUndertaking NOT_FOUND - No undertaking found for $eori (EIS error code $notFoundEisErrorCode) - available for undertakingCreate"
          )
          Left(
            ConnectorError(
              NOT_FOUND,
              s"retrieveUndertaking NOT_FOUND - No undertaking found for $eori (EIS error code $notFoundEisErrorCode)"
            )
          )

        case e: EisBadResponseException if e.code == EisParamValue(unacceptableEisErrorCode) =>
          logger.error(
            s"retrieveUndertaking NOT_ACCEPTABLE - Eori:$eori (Eis error code $unacceptableEisErrorCode) - NOT available for undertakingCreate",
            e
          )
          Left(
            ConnectorError(
              NOT_ACCEPTABLE,
              s"retrieveUndertaking NOT_ACCEPTABLE - Eori:$eori (Eis error code $unacceptableEisErrorCode)"
            )
          )
      }
  }

  def createUndertaking(
    undertaking: UndertakingCreate
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingRef] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.createUndertakingResponseReads
    val createUndertakingPath = "scp/createundertaking/v1"

    logger.info(
      s"attempting createUndertaking ${undertaking.loggableString}"
    )

    val eisTokenKey = "eis.token.scp02"
    desPost[CreateUndertakingApiRequest, UndertakingRef](
      s"$eisURL/$createUndertakingPath",
      CreateUndertakingApiRequest(undertaking),
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def updateUndertaking(
    undertaking: UndertakingRetrieve,
    amendmentType: EisAmendmentType
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UndertakingRef] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.updateUndertakingResponseReads
    val updateUndertakingPath = "scp/updateundertaking/v1"

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

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberResponseReads

    val eisTokenKey = "eis.token.scp05"

    desPost[UndertakingBusinessEntityUpdate, Unit](
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
  }

  def deleteMember(
    undertakingRef: UndertakingRef,
    businessEntity: BusinessEntity
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberResponseReads

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
    val amendSubsidyPath = "scp/amendundertakingsubsidyusage/v1"
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
    val retrieveSubsidyPath = "scp/getundertakingtransactions/v1"
    val eisTokenKey = "eis.token.scp09"
    val defaultDateRange = Some((LocalDate.of(2000, 1, 1), LocalDate.now()))

    desPost[SubsidyRetrieve, UndertakingSubsidies](
      s"$eisURL/$retrieveSubsidyPath",
      SubsidyRetrieve(ref, dateRange.orElse(defaultDateRange)),
      eisTokenKey
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def getUndertakingBalance(
    request: GetUndertakingBalanceRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[GetUndertakingBalanceApiResponse]] = {
    val getUndertakingBalancePath = "scp/getsamundertakingbalance/v1"
    val eisTokenKey = "eis.token.scp08"

    http
      .post(new URL(s"$eisURL/$getUndertakingBalancePath"))
      .setHeader(headers(eisTokenKey): _*)
      .withBody(Json.toJson(request))
      .execute[HttpResponse](implicitly[HttpReads[HttpResponse]], implicitly[ExecutionContext])
      .map { res =>
        res.status match {
          case Status.OK => Json.parse(res.body).asOpt[GetUndertakingBalanceApiResponse]
          case _ => None
        }
      }
  }
}
