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
import uk.gov.hmrc.eusubsidycompliance.models.Undertaking
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.RequestCommon
import uk.gov.hmrc.eusubsidycompliance.models.types.{EisAmendmentType, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class UpdateUndertakingApiRequest(updateUndertakingRequest: UpdateUndertakingRequest)

object UpdateUndertakingApiRequest {
  implicit val writes: Writes[UpdateUndertakingApiRequest] = Json.writes

  def apply(undertaking: Undertaking, amendmentType: EisAmendmentType): UpdateUndertakingApiRequest =
    UpdateUndertakingApiRequest(
      UpdateUndertakingRequest(
        requestDetail = UpdateUndertakingRequestDetail(
          amendmentType,
          undertakingId = undertaking.reference,
          undertakingName = undertaking.name,
          industrySector = undertaking.industrySector
        )
      )
    )
}

final case class UpdateUndertakingRequest(
  requestCommon: RequestCommon = RequestCommon("UpdateUndertaking"),
  requestDetail: UpdateUndertakingRequestDetail
)
object UpdateUndertakingRequest {
  implicit val writes: Writes[UpdateUndertakingRequest] = Json.writes
}

final case class UpdateUndertakingRequestDetail(
  amendmentType: EisAmendmentType,
  undertakingId: Option[UndertakingRef],
  undertakingName: UndertakingName,
  industrySector: Sector,
  disablementStartDate: LocalDate = LocalDate.now
)

object UpdateUndertakingRequestDetail {
  implicit val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  implicit val writes: Writes[UpdateUndertakingRequestDetail] = Json.writes
}
