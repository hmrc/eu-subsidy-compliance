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

package uk.gov.hmrc.eusubsidycompliance.models

import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI

case class VerifiedEmailResponse(eori: EORI, email: String, verificationId: String, verified: Boolean)

object VerifiedEmailResponse {
  implicit val verifiedEmailFormat: OFormat[VerifiedEmailResponse] = Json.format[VerifiedEmailResponse]

  implicit class VerifiedEmailOps(verifiedEmail: VerifiedEmailResponse) {
    val asJson: JsObject = verifiedEmailFormat.writes(verifiedEmail)
  }

  def fromEmailCache(emailCache: EmailCache): VerifiedEmailResponse =
    VerifiedEmailResponse(emailCache.eori, emailCache.email, emailCache.verificationId, emailCache.verified)

}