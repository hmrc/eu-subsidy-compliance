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

import cats.implicits._

import javax.inject.{Inject, Singleton}
import play.api.{Logger, Mode}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, BusinessEntityUpdate, Undertaking, UndertakingBusinessEntityUpdate}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.EisParamName.EisParamName
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisParamName, EisParamValue, UndertakingRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
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
  val amendBusinessEntityPath = "scp/amendundertakingmemberdata/v1"

  def retrieveUndertaking(
    eori: EORI
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Undertaking] = {
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.undertakingFormat
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.retrieveUndertakingEORIWrites

    desPost[EORI, Undertaking](
      s"$eisURL/$retrieveUndertakingPath",
      eori
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
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.undertakingFormat
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.undertakingCreateResponseReads

    desPost[Undertaking, UndertakingRef](
      s"$eisURL/$createUndertakingPath",
      undertaking
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def addMember(
     undertakingRef: UndertakingRef,
     businessEntity: BusinessEntity,
     amendmentType: AmendmentType
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberDataWrites
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberDataResponseReads
    desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingRef,
        true,
        List(BusinessEntityUpdate(amendmentType, LocalDate.now(), businessEntity)))
    )(implicitly, implicitly, addHeaders, implicitly)
  }

  def deleteMember(
     undertakingRef: UndertakingRef,
     businessEntity: BusinessEntity
   )(
     implicit hc: HeaderCarrier,
     ec: ExecutionContext
   ): Future[Unit] = {

    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberDataWrites
    import uk.gov.hmrc.eusubsidycompliance.models.json.digital.amendUndertakingMemberDataResponseReads

    desPost[UndertakingBusinessEntityUpdate, Unit](
      s"$eisURL/$amendBusinessEntityPath",
      UndertakingBusinessEntityUpdate(
        undertakingRef,
        true,
        List(BusinessEntityUpdate(AmendmentType.delete, LocalDate.now(), businessEntity)))
    )(implicitly, implicitly, addHeaders, implicitly)
  }
}
