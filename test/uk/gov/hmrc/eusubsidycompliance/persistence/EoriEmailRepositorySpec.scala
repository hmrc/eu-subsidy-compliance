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

  "addEmailInitialisation" must {
    "save the detail, with verification flag as false" in {
      val initialEmailCache =
        InitialEmailCache(EORI("GB123456783306"), "emailValueSet", "verificationIdSet", verified = false)

      val instant = Instant.parse("2023-06-22T11:44:14.681Z")
      (() => timeProvider.nowAsInstant)
        .expects()
        .returning(instant)

      val errorOrEmailCache = repository.addEmailInitialisation(initialEmailCache).futureValue
      val insertedValue = find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

      val expected =
        EmailCache(
          eori = initialEmailCache.eori,
          email = initialEmailCache.email,
          verificationId = initialEmailCache.verificationId,
          verified = initialEmailCache.verified,
          created = instant,
          lastUpdated = instant
        )

      errorOrEmailCache mustBe Right(expected)
      insertedValue mustBe Some(expected)

      // OptionValues and EitherValues don't give the best failure messages (not very concise)
      // A bit too success happy. Breaks diff ability in Intellij as well
      val maybeEncodedEmail = repository.collection.getEncodedEmail(initialEmailCache.eori)
      maybeEncodedEmail must not be Some(
        initialEmailCache.email
      )

      maybeEncodedEmail.map(_.length) mustBe Some(68)
    }

    "markEoriAsVerified" must {
      "succeed if eori is found" in {
        val eori = EORI("GB223456783307")
        val initialEmailCache =
          InitialEmailCache(eori, "emailValueSet", "verificationIdSet", verified = false)

        val instant = Instant.parse("2023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(instant)

        val emailCacheBeforeVerification = EmailCache(
          eori = initialEmailCache.eori,
          email = initialEmailCache.email,
          verificationId = initialEmailCache.verificationId,
          verified = false,
          created = instant,
          lastUpdated = instant
        )

        //Usually I wouldn't call a prod method for setup in a test for another prod method as unwittingly tests can actually
        //change in unknown ways and end up with successes for different reasons but the whole mongo/encryption thing
        //is very opinionated.
        val errorOrEmailCache = repository.addEmailInitialisation(initialEmailCache).futureValue
        errorOrEmailCache mustBe Right(emailCacheBeforeVerification)

        val updatedInstant = Instant.parse("3023-06-22T11:44:14.681Z")
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updatedInstant)

        val errorOrMaybeUpdatedEmailCache = repository.markEoriAsVerified(eori).futureValue
        val expectedEmailCachePostValidation =
          emailCacheBeforeVerification.copy(verified = true, lastUpdated = updatedInstant)
        errorOrMaybeUpdatedEmailCache mustBe Right(Some(expectedEmailCachePostValidation))

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", initialEmailCache.eori)).futureValue.headOption

        updatedMongoDbValue mustBe Some(expectedEmailCachePostValidation)
      }

      "return None when EORI is not found" in {
        val eori = EORI("GB223456783307")

        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(Instant.now())

        val errorOrMaybeUpdatedEmailCache = repository.markEoriAsVerified(eori).futureValue

        errorOrMaybeUpdatedEmailCache mustBe Right(None)

        val updatedMongoDbValue: Option[EmailCache] =
          find(Filters.equal("_id", eori)).futureValue.headOption

        updatedMongoDbValue mustBe None
      }

    }

    "update" must {
      "when there is a no cached value for an EORI" in {
        val missingCacheReference =
          UpdateEmailCache(EORI("GB123456783309"), "email", "", verified = true)

        val updateResult = repository.update(missingCacheReference).futureValue

        updateResult mustBe Left(NotFound)
        updateResult.left.value mustBe NotFound
      }

      "when there is a matching cached value for an EORI" in {
        val initialInstant = Instant.parse("3023-06-22T11:44:14.681Z")

        // 4 versus 3 at the beginning is easy to compare
        val updateInstant = Instant.parse("4023-06-22T11:44:14.681Z")

        //Called on insert
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(initialInstant)

        //Called on update
        (() => timeProvider.nowAsInstant)
          .expects()
          .returning(updateInstant)

        val eori = EORI("GB123456783309")
        val verificationIdUpdated = "verificationIdUpdated"
        val initialEmailValue = "testEmailValue1"
        val cachedValue = EmailCache(
          eori = eori,
          email = initialEmailValue,
          verificationId = verificationIdUpdated,
          verified = false,
          created = initialInstant,
          lastUpdated = initialInstant
        )

        val eventualMaybeResult =
          repository.addEmailInitialisation(
            InitialEmailCache(
              eori = eori,
              verificationId = "verificationIdInitial",
              email = initialEmailValue,
              verified = false
            )
          )

        eventualMaybeResult.futureValue

        val updatedValue = cachedValue.copy(
          email = "testEmailValue2",
          verified = true
        )
        val updateResult = repository
          .update(
            UpdateEmailCache(eori = cachedValue.eori, updatedValue.email, verificationIdUpdated, verified = true)
          )
          .futureValue

        // Could use EitherValues but failure message is not the most clear, normal assert Right() is clearer
        updateResult mustBe Right(true)

        val insertedValue = find(Filters.equal("_id", cachedValue.eori)).futureValue.headOption

        insertedValue mustBe Some(
          EmailCache(
            eori = cachedValue.eori,
            email = updatedValue.email,
            verificationId = verificationIdUpdated,
            verified = true,
            created = initialInstant,
            lastUpdated = updateInstant
          )
        )

        //   val maybeDbEmailCache = find(Filters.equal("_id", cachedValue.eori)).futureValue.headOption

        val maybeEncodedEmail = repository.collection.getEncodedEmail(cachedValue.eori)
        maybeEncodedEmail.iterator.toList must contain noneOf (cachedValue.email, updatedValue.email)

        maybeEncodedEmail.map(_.length) mustBe Some(68)
      }
    }
  }

  "get" must {
    "when there is no cached value for an EORI" in {
      val noData: Option[EmailCache] = repository.get(EORI("GB123456783309")).futureValue

      noData mustBe None
    }

    "when there is matching cached value for an EORI" in {
      val initialInstant = Instant.parse("3023-06-22T11:44:14.681Z")

      //Called on insert
      (() => timeProvider.nowAsInstant)
        .expects()
        .returning(initialInstant)

      val data =
        EmailCache(
          eori = EORI("GB123456783309"),
          email = "emailValue",
          verificationId = "verificationId",
          verified = false,
          created = initialInstant,
          lastUpdated = initialInstant
        )

      repository
        .addEmailInitialisation(InitialEmailCache(data.eori, data.verificationId, data.email, data.verified))
        .futureValue

      val maybeEmailCache = repository.get(EORI("GB123456783309")).futureValue
      maybeEmailCache mustBe Some(data)
    }
  }

}
