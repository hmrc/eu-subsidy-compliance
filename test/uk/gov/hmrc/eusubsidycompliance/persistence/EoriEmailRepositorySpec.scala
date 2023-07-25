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

package uk.gov.hmrc.eusubsidycompliance.persistence

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.scalamock.scalatest.MockFactory
import org.scalatest.{EitherValues, OptionValues}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.models.{EmailCache, UpdateEmailCache}
import uk.gov.hmrc.eusubsidycompliance.shared.BaseSpec
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.security.SecureRandom
import java.time._
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Base64

class EoriEmailRepositorySpec
    extends BaseSpec
    with MockFactory
    with DefaultPlayMongoRepositorySupport[EmailCache]
    with OptionValues
    with EitherValues {

  private val timeProvider: TimeProvider = mock[TimeProvider]

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }

  private val configuration = Configuration("crypto.key" -> aesKey)

  private implicit val crypto: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  implicit class RepositoryOps[A](collection: MongoCollection[A]) {
    def getEncodedEmail(eori: EORI): Option[String] = {
      val maybeBsonDocument = collection
        .find[BsonDocument](Filters.equal("_id", eori))
        .headOption()
        .futureValue

      maybeBsonDocument.map { bsonDocument =>
        val json = Json.parse(bsonDocument.toJson)

        (json \ "email").as[String]
      }
    }
  }

  protected override val repository: EoriEmailRepository = new EoriEmailRepository(
    mongoComponent = mongoComponent,
    timeProvider = timeProvider
  )

  "EoriEmailRepositorySpec" must {
    "setEmailInitialisation" should {
      val initialEmailCache =
        InitialEmailCache(EORI("GB123456783306"), "emailValueSet", "verificationIdSet", verified = false)

      val createdInstant = Instant.parse("2023-06-22T11:44:14.681Z")

      val expectedOriginalEntry = EmailCache.createUnverifiedInitialEmailCache(initialEmailCache, createdInstant)

      "save the detail, with verification flag as false" in {
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val errorOrEmailCache = repository.setEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(WriteSuccess)

        val insertedValue = find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption
        insertedValue mustBe Some(expectedOriginalEntry)

        // OptionValues and EitherValues don't give the best failure messages (not very concise)
        // A bit too success happy. Breaks diff ability in Intellij as well
        val maybeEncodedEmail = repository.collection.getEncodedEmail(initialEmailCache.eori)
        maybeEncodedEmail must not be Some(true)
        maybeEncodedEmail.map(_.length) mustBe Some(68)
      }

      "replace an existing validated email with an unvalidate email" in {
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val updatedInstant = createdInstant.plus(30, ChronoUnit.DAYS)
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        //Control so we can verify only one updates
        val otherCacheEntry =
          InitialEmailCache(EORI("GB923456783306"), "emailValueSet", "verificationIdSet", verified = false)

        repository.setEmailInitialisation(initialEmailCache).futureValue mustBe Right(WriteSuccess)
        repository.setEmailInitialisation(otherCacheEntry).futureValue mustBe Right(WriteSuccess)

        val updatedEmailCache = initialEmailCache.copy(email = "anotherEmail")

        val errorOrEmailCache =
          repository.setEmailInitialisation(updatedEmailCache).futureValue

        errorOrEmailCache mustBe Right(UpdateSuccess)

        withClue("updated value is retrievable unencrypted") {
          val updatedValue = find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption
          updatedValue mustBe Some(EmailCache.createUnverifiedInitialEmailCache(updatedEmailCache, updatedInstant))
        }

        withClue("record has been updated in an encrypted fashion") {
          // OptionValues and EitherValues don't give the best failure messages (not very concise)
          // A bit too success happy. Breaks diff ability in Intellij as well
          val maybeEncodedEmail = repository.collection.getEncodedEmail(updatedEmailCache.eori)
          maybeEncodedEmail must not be Some(updatedEmailCache.email)
          maybeEncodedEmail.map(_.length) mustBe Some(64)
        }

        withClue("other value in store is not affected in the update") {
          val updatedValue = find(Filters.equal("_id", otherCacheEntry.eori)).futureValue.headOption
          updatedValue mustBe Some(EmailCache.createUnverifiedInitialEmailCache(otherCacheEntry, createdInstant))
        }
      }
    }

    "markEmailAsVerifiedByEori" should {
      "succeed if eori is found" in {
        val eori = EORI("GB223456783307")
        val initialEmailCache =
          InitialEmailCache(
            eori = eori,
            verificationId = "verificationIdSet",
            email = "emailValueSet",
            verified = false
          )

        val createdInstant = Instant.parse("2023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val emailCacheBeforeVerification =
          EmailCache.createUnverifiedInitialEmailCache(initialEmailCache, createdInstant)

        val errorOrEmailCache = repository.setEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(WriteSuccess)

        val updatedInstant = Instant.parse("3023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        val errorOrMaybeUpdatedEmailCache = repository.markEmailAsVerifiedByEori(eori).futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(Some(WriteSuccess))

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

        val expectedEmailCachePostValidation =
          emailCacheBeforeVerification.copy(verified = true, lastUpdated = updatedInstant)
        updatedMongoDbValue mustBe Some(expectedEmailCachePostValidation)
      }

      "return None when EORI is not found" in {
        val eori = EORI("GB223456783307")

        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(Instant.now())

        val errorOrMaybeUpdatedEmailCache = repository.markEmailAsVerifiedByEori(eori).futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(None)

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", eori)).futureValue.headOption

        updatedMongoDbValue mustBe None
      }

    }

    "markEmailAsVerifiedByVerificationId" should {
      "succeed if eori and verification id is found" in {
        val eori = EORI("GB223456783307")
        val verificationId = "verificationIdSet"
        val initialEmailCache =
          InitialEmailCache(eori = eori, verificationId = verificationId, email = "emailValueSet", verified = false)

        val createdInstant = Instant.parse("2023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val errorOrEmailCache = repository.setEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(WriteSuccess)

        val updatedInstant = Instant.parse("3023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        val errorOrMaybeUpdatedEmailCache =
          repository.markEmailAsVerifiedByVerificationId(eori, verificationId).futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(Some(WriteSuccess))

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

        val expectedEmailCachePostValidation =
          EmailCache
            .createUnverifiedInitialEmailCache(initialEmailCache, createdInstant)
            .copy(verified = true, lastUpdated = updatedInstant)

        updatedMongoDbValue mustBe Some(expectedEmailCachePostValidation)
      }

      "update nothing if the verificationId is not correct" in {
        val eori = EORI("GB223456783307")
        val verificationId = "verificationIdSet"
        val initialEmailCache =
          InitialEmailCache(eori = eori, verificationId = verificationId, email = "emailValueSet", verified = false)

        val createdInstant = Instant.parse("2023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val errorOrEmailCache = repository.setEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(WriteSuccess)

        val updatedInstant = Instant.parse("3023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        val errorOrMaybeUpdatedEmailCache =
          repository.markEmailAsVerifiedByVerificationId(eori, "anotherVerificationId").futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(None)

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

        val emailCacheBeforeVerification =
          EmailCache.createUnverifiedInitialEmailCache(initialEmailCache, createdInstant)
        updatedMongoDbValue mustBe Some(emailCacheBeforeVerification)
      }

      "update nothing if the EORI is not correct but the verification id is" in {
        val eori = EORI("GB223456783307")
        val verificationId = "verificationIdSet"
        val initialEmailCache =
          InitialEmailCache(eori = eori, verificationId = verificationId, email = "emailValueSet", verified = false)

        val createdInstant = Instant.parse("2023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(createdInstant)

        val errorOrEmailCache = repository.setEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(WriteSuccess)

        val updatedInstant = Instant.parse("3023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        val errorOrMaybeUpdatedEmailCache =
          repository.markEmailAsVerifiedByVerificationId(EORI("GB323456783307"), verificationId).futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(None)

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

        val emailCacheBeforeVerification =
          EmailCache.createUnverifiedInitialEmailCache(initialEmailCache, createdInstant)

        updatedMongoDbValue mustBe Some(emailCacheBeforeVerification)
      }

      "return None when the record does not exist" in {
        val eori = EORI("GB223456783307")

        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(Instant.now())

        val errorOrMaybeUpdatedEmailCache =
          repository.markEmailAsVerifiedByVerificationId(eori, "verificationIdSet").futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(None)

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", eori)).futureValue.headOption

        updatedMongoDbValue mustBe None
      }
    }

    "getEmailVerification" must {
      "return None when it does not exist" in {
        val errorOrMaybeEmailCache: Either[EoriEmailRepositoryError, Option[EmailCache]] =
          repository.getEmailVerification(EORI("GB123456783309")).futureValue

        errorOrMaybeEmailCache mustBe Right(None)
      }

      "return None when the EORI is the wrong value" in {
        val initialInstant = Instant.parse("3023-06-22T11:44:14.681Z")

        //Called on insert
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(initialInstant)

        val eori = EORI("GB123456783309")
        val data = EmailCache(
          eori = eori,
          email = "emailValue",
          verificationId = "verificationId",
          verified = false,
          created = initialInstant,
          lastUpdated = initialInstant
        )

        repository
          .setEmailInitialisation(InitialEmailCache(data.eori, data.verificationId, data.email, data.verified))
          .futureValue

        val maybeEmailCache = repository.getEmailVerification(EORI("GB223456783309")).futureValue

        maybeEmailCache mustBe Right(None)
      }

      "when there is matching cached value for an EORI" in {
        val initialInstant = Instant.parse("3023-06-22T11:44:14.681Z")

        //Called on insert
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(initialInstant)

        val eori = EORI("GB123456783309")
        val data = EmailCache(
          eori = eori,
          email = "emailValue",
          verificationId = "verificationId",
          verified = false,
          created = initialInstant,
          lastUpdated = initialInstant
        )

        repository
          .setEmailInitialisation(InitialEmailCache(data.eori, data.verificationId, data.email, data.verified))
          .futureValue

        val maybeEmailCache = repository.getEmailVerification(EORI("GB123456783309")).futureValue
        maybeEmailCache mustBe Right(Some(data))
      }
    }
  }
}
