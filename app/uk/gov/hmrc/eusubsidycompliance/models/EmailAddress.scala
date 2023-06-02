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

import play.api.Configuration
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.crypto.json.JsonEncryption

import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

final case class EmailAddress(value: String) extends AnyVal

object EmailAddress {
  implicit val format: Format[EmailAddress] = Json.valueFormat[EmailAddress]

  /*
  // ESC-841 - Added below piece of code
  implicit val format: Format[EmailAddress] =
    (__ \ "email").format[SensitiveString](EmailAddress.apply(_), unlift(EmailAddress.unapply))

  implicit val crypto = {
    val aesKey = {
      val aesKey = new Array[Byte](32)
      new SecureRandom().nextBytes(aesKey)
      Base64.getEncoder.encodeToString(aesKey)
    }
    val config = Configuration("crypto.key" -> aesKey)
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", config.underlying)
  }

  val sensitiveStringFormat = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
   */
}

case class Undeliverable(eventId: String)

object Undeliverable {
  implicit val format: OFormat[Undeliverable] = Json.format[Undeliverable]
}

case class EmailAddressResponse(
  address: EmailAddress,
  timestamp: Option[LocalDateTime],
  undeliverable: Option[Undeliverable]
)

object EmailAddressResponse {
  implicit val format: OFormat[EmailAddressResponse] = Json.format[EmailAddressResponse]
}
