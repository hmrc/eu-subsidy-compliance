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

import play.api.libs.json.{Format, Json, OFormat, OWrites, Reads}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.persistence.InitialEmailCache
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

object EmailCache {
  implicit val format: OFormat[EmailCache] = Json.format[EmailCache]

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[EmailCache] = {

    import play.api.libs.functional.syntax._

    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    import play.api.libs.json.__

    //
    val encryptedReads: Reads[EmailCache] =
      (
        (__ \ "_id").read[String] and
          (__ \ "verificationId").read[String] and
          (__ \ "email").read[SensitiveString] and
          (__ \ "verified").read[Boolean] and
          (__ \ "created").read(MongoJavatimeFormats.instantFormat) and
          (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )((eori, verificationId, email, verified, created, lastUpdated) =>
        EmailCache(
          eori = EORI(eori),
          email = email.decryptedValue,
          verificationId = verificationId,
          verified = verified,
          created = created,
          lastUpdated = lastUpdated
        )
      )

    val encryptedWrites: OWrites[EmailCache] = (
      (__ \ "_id").write[String] and
        (__ \ "verificationId").write[String] and
        (__ \ "email").write[SensitiveString] and
        (__ \ "verified").write[Boolean] and
        (__ \ "created").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) { emailCache =>
      (
        emailCache.eori,
        emailCache.verificationId,
        SensitiveString(emailCache.email),
        emailCache.verified,
        emailCache.created,
        emailCache.lastUpdated
      )
    }

    OFormat(encryptedReads, encryptedWrites)
  }

  def createUnverifiedInitialEmailCache(initialEmailCache: InitialEmailCache, createdInstant: Instant): EmailCache =
    EmailCache(
      eori = initialEmailCache.eori,
      email = initialEmailCache.email,
      verificationId = initialEmailCache.verificationId,
      verified = false,
      created = createdInstant,
      lastUpdated = createdInstant
    )
}

final case class EmailCache(
  eori: EORI,
  email: String,
  verificationId: String,
  verified: Boolean,
  created: Instant,
  lastUpdated: Instant
)

final case class UpdateEmailCache(eori: EORI, email: String, verificationId: String, verified: Boolean) {
  def asEmailCache(created: Instant, updated: Instant): EmailCache =
    EmailCache(
      eori = eori,
      email = email,
      verificationId = verificationId,
      verified = verified,
      created = created,
      lastUpdated = updated
    )
}
