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

//
//package uk.gov.hmrc.eusubsidycompliance.shared
//
//import org.mongodb.scala.MongoCollection
//import org.mongodb.scala.model.{FindOneAndUpdateOptions, Updates}
//import play.api.libs.json.{JsBoolean, JsValue}
//import uk.gov.hmrc.eusubsidycompliance.persistence.EoriEmailRepository
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.cache.CacheItem
//import uk.gov.hmrc.mongo.test.MongoSupport
//
//import java.time.{Instant, LocalDaeTime}
//import scala.concurrent.{ExecutionContext, Future}
//
//object CachedEmailVerification {
//  import TestJson.ops._
//
//  def fromMongoCache(cacheItem: CacheItem): CachedEmailVerification = {
//    val fields = cacheItem.data.fields.map { case (key, value) =>
//      s"data.$key" -> value
//    }
//    println(fields)
//
//    CachedEmailVerification(
//      cacheItem.id,
//      fields.find(_._1 == EoriEmailRepository.keys.email).map(_._2.asHumanString),
//      fields.find(_._1 == EoriEmailRepository.keys.verificationId).map(_._2.asHumanString),
//      fields.find(_._1 == EoriEmailRepository.keys.verified).map { case (_, value) =>
//        booleanFromJson(value)
//      },
//      cacheItem.createdAt,
//      cacheItem.modifiedAt
//    )
//  }
//
//  private def booleanFromJson(jsValue: JsValue): Boolean =
//    jsValue match {
//      case boolean: JsBoolean => boolean.value
//      case _ => false
//    }
//
//}
//
//case class CachedEmailVerification(
//  id: String,
//  maybeEmail: Option[String],
//  maybeVerificationId: Option[String],
//  maybeVerified: Option[Boolean],
//  createdAt: Instant,
//  lastUpdated: Instant
//)
//
//class TestMongo(implicit val executionContext: ExecutionContext) extends MongoSupport {
//  lazy val component: MongoComponent = mongoComponent
//
//  override def dropDatabase(): Unit = super.dropDatabase()
//
//  /**
//    * CacheItem(
//    * id = eoriNumber,
//    * data = JsObject(
//    * underlying = Map(
//    * // .asJson from   import uk.gov.hmrc.eusubsidycompliance.shared.TestJson.ops._
//    * "email" -> email.asJson,
//    * "verificationId" -> "32123".asJson,
//    * "verified" -> false.asJson
//    * )
//    * ),
//    * createdAt = expectedUtcInstant,
//    * modifiedAt = expectedUtcInstant
//    * )
//    */
//
//  def getCachedEmailItems(collection: MongoCollection[CacheItem]): Future[List[CachedEmailVerification]] =
//    collection
//      .find()
//      .toFuture()
//      .map(a => a.map(b => CachedEmailVerification.fromMongoCache(b)).toList)
//
//  def addCachedEmailItem(
//    collection: MongoCollection[CacheItem],
//    id: String,
//    email: String,
//    verificationId: String,
//    verified: Boolean,
//    updated: LocalDateTime,
//    created: LocalDateTime
//  ): Future[CacheItem] = {
//    import EoriEmailRepository._
//
//    collection
//      .findOneAndUpdate(
//        filter = filter.byId(id),
//        update = Updates.combine(
//          update.email(email),
//          update.verificationId(verificationId),
//          update.verified(verified),
//          update.lastUpdated(updated),
//          update.createdAt(created)
//        ),
//        options = FindOneAndUpdateOptions().upsert(true)
//      )
//      .toFuture()
//  }
//}
