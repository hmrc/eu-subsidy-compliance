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

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.{Filters, InsertOneOptions, ReplaceOptions, Updates}
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.eusubsidycompliance.models.{EmailCache, UpdateEmailCache}
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.{CacheIdType, MongoCacheRepository}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

final case class InitialEmailCache(eori: EORI, verificationId: String, email: String, verified: Boolean) {
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

case object NotFound

case class EoriEmailRepositoryError(message: String, maybeCause: Option[Throwable] = None)
    extends RuntimeException(message, maybeCause.orNull)

case object WriteSuccess

@Singleton
class EoriEmailRepository @Inject() (
  mongoComponent: MongoComponent,
  timeProvider: TimeProvider
)(implicit ec: ExecutionContext, crypto: Encrypter with Decrypter)
    extends PlayMongoRepository[
      uk.gov.hmrc.eusubsidycompliance.models.EmailCache
    ]( // TODO: change this to MongoCacheRepository
      mongoComponent = mongoComponent,
      collectionName = "eori-email-store",
      domainFormat = EmailCache.encryptedFormat,
      indexes = Seq.empty,
      optSchema = None,
      replaceIndexes = false,
      extraCodecs = Seq.empty
    ) {

  def addEmailInitialisation(
    initialEmailCache: InitialEmailCache
  ): Future[Either[EoriEmailRepositoryError, WriteSuccess.type]] = {
    val now = timeProvider.nowAsInstant
    collection
      .insertOne(
        initialEmailCache.asEmailCache(now, now),
        options = InsertOneOptions().ensuring(true)
      )
      .toFuture()
      .map { insertOneResult: InsertOneResult =>
        if (insertOneResult.wasAcknowledged()) {
          Right(WriteSuccess)
        } else {
          Left(
            EoriEmailRepositoryError(
              s"Failed addEmailInitialisation for EORI ${initialEmailCache.eori} - the write was not acknowledged"
            )
          )
        }

      }
      .recover { case e: Throwable =>
        Left(
          EoriEmailRepositoryError(
            s"Failed addEmailInitialisation for EORI ${initialEmailCache.eori} - ${e.getMessage}",
            Some(e)
          )
        )
      }
  }

  def markEmailAsVerifiedByEori(eori: EORI): Future[Either[EoriEmailRepositoryError, Option[WriteSuccess.type]]] = {
    val inputDocument = createVerifiedUpdateDocument

    collection
      .updateOne(Filters.eq("_id", eori), Document("$set" -> Document(inputDocument)))
      .toFuture()
      .map { updateResult: UpdateResult =>
        if (updateResult.getModifiedCount > 0) {
          Right(Some(WriteSuccess))
        } else {
          Right(None)
        }
      }

  }

  private def createVerifiedUpdateDocument = {
    s"""
       |{"verified" : true, "lastUpdated" : ISODate("${timeProvider.nowAsInstant.toString}")}
       |""".stripMargin
  }

  def markEmailAsVerifiedByVerificationId(
    eori: EORI,
    verificationId: String
  ): Future[Either[EoriEmailRepositoryError, Option[EmailCache]]] = {
    val inputDocument = createVerifiedUpdateDocument
    collection
      .updateOne(
        Filters.and(
          Filters.equal("_id", eori),
          Filters.equal("verificationId", verificationId)
        ),
        Document("$set" -> Document(inputDocument))
      )
      .toFuture()
      .flatMap { updateResult: UpdateResult =>
        if (updateResult.getMatchedCount > 0) {
          getEmailVerification(eori)
        } else {
          Future.successful(Right(None))
        }
      }
      .recover(error => Left(EoriEmailRepositoryError(s"Failed markEoriAsVerified for EORI:$eori", Some(error))))
  }

  def getEmailVerification(eori: EORI): Future[Either[EoriEmailRepositoryError, Option[EmailCache]]] =
    collection
      .find(
        filter = Filters.equal("_id", eori)
      )
      .toFuture()
      .map { result =>
        Right(result.headOption)
      }
      .recover(error => Left(EoriEmailRepositoryError(s"Failed getting verification for EORI:$eori", Some(error))))

}
