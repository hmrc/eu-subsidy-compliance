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

package uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.RequestCommon
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliance.models.types.UndertakingName

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class CreateUndertakingApiRequest(createUndertakingRequest: CreateUndertakingRequest)
object CreateUndertakingApiRequest {

  def apply(undertaking: Undertaking): CreateUndertakingApiRequest = {
    val lead: BusinessEntity =
      undertaking.undertakingBusinessEntity
        .filter(_.leadEORI)
        .headOption
        .fold(sys.error("Lead missing in undertaking"))(identity)
    CreateUndertakingApiRequest(
      CreateUndertakingRequest(
        requestDetail = CreateUndertakingRequestDetail(
          undertaking.name,
          undertaking.industrySector,
          BusinessEntityDetail(idValue = lead.businessEntityIdentifier.toString),
          LocalDate.now
        )
      )
    )
  }
  implicit val writes: Writes[CreateUndertakingApiRequest] = Json.writes
}

final case class CreateUndertakingRequest(
  requestCommon: RequestCommon = RequestCommon("CreateNewUndertaking"),
  requestDetail: CreateUndertakingRequestDetail
)
object CreateUndertakingRequest {
  implicit val writes: Writes[CreateUndertakingRequest] = Json.writes
}

final case class CreateUndertakingRequestDetail(
  undertakingName: UndertakingName,
  industrySector: Sector,
  businessEntity: BusinessEntityDetail,
  undertakingStartDate: LocalDate
)
object CreateUndertakingRequestDetail {
  implicit val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  implicit val writes: Writes[CreateUndertakingRequestDetail] = Json.writes
}

final case class BusinessEntityDetail(idType: String = "EORI", idValue: String)
object BusinessEntityDetail {
  implicit val writes: Writes[BusinessEntityDetail] = Json.writes
}
