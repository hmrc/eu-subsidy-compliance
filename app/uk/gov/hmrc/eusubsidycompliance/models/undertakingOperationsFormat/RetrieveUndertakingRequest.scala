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

package uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.RequestCommon
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI

final case class RetrieveUndertakingAPIRequest(retrieveUndertakingRequest: RetrieveUndertakingRequest)

object RetrieveUndertakingAPIRequest {
  implicit val writes: Writes[RetrieveUndertakingAPIRequest] = Json.writes

  def apply(eori: EORI): RetrieveUndertakingAPIRequest = RetrieveUndertakingAPIRequest(
    RetrieveUndertakingRequest(requestDetail = RequestDetail(idValue = eori))
  )
}

final case class RetrieveUndertakingRequest(
  requestCommon: RequestCommon = RequestCommon("RetrieveUndertaking"),
  requestDetail: RequestDetail
)
object RetrieveUndertakingRequest {
  implicit val writes: Writes[RetrieveUndertakingRequest] = Json.writes
}

final case class RequestDetail(
  idType: String = "EORI",
  idValue: EORI
)

object RequestDetail {
  implicit val writes: Writes[RequestDetail] = Json.writes
}
