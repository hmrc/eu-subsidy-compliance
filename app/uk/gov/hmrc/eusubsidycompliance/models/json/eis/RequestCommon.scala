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

package uk.gov.hmrc.eusubsidycompliance.models.json.eis

import java.util.UUID
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.receiptDate
import uk.gov.hmrc.eusubsidycompliance.models.types.AcknowledgementRef

final case class RequestCommon(
  originatingSystem: String = "MDTP",
  receiptDate: String = receiptDate,
  acknowledgementReference: AcknowledgementRef = AcknowledgementRef(UUID.randomUUID().toString.replace("-", "")),
  messageTypes: MessageTypes,
  requestParameters: List[RequestParameters] = List(RequestParameters())
)

case object RequestCommon {

  implicit val writes: Writes[RequestCommon] = Json.writes
  def apply(message: String): RequestCommon = RequestCommon(messageTypes = MessageTypes(message))

}

case class MessageTypes(messageType: String)
object MessageTypes {
  implicit val writes: Writes[MessageTypes] = Json.writes
}

case class RequestParameters(paramName: String = "REGIME", paramValue: String = "ES")
object RequestParameters {
  implicit val writes: Writes[RequestParameters] = Json.writes
}
