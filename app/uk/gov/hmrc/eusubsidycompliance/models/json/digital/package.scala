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

package uk.gov.hmrc.eusubsidycompliance.models.json

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliance.models.types.{IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, UndertakingRetrieve}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZonedDateTime}

package object digital {

  private val clock: Clock = Clock.systemUTC()
  private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
  private val OK = "OK"
  private val NOT_OK = "NOT_OK"
  private val oddEisDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def receiptDate: String = {
    val instant = Instant.now(clock)
    val withoutNanos = instant.minusNanos(instant.getNano)
    formatter.format(withoutNanos)
  }

  implicit val retrieveUndertakingResponseReads: Reads[UndertakingRetrieve] =
    readResponseFor[UndertakingRetrieve]("retrieveUndertakingResponse") { json =>
      val responseDetail = json \ "retrieveUndertakingResponse" \ "responseDetail"

      JsSuccess(
        UndertakingRetrieve(
          reference = (responseDetail \ "undertakingReference").asOpt[String].map(UndertakingRef(_)),
          name = (responseDetail \ "undertakingName").as[UndertakingName],
          industrySector = (responseDetail \ "industrySector").as[Sector],
          industrySectorLimit = (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit].some,
          lastSubsidyUsageUpdt = (responseDetail \ "lastSubsidyUsageUpdt")
            .asOpt[String]
            .map(ds => LocalDate.parse(ds, oddEisDateFormat)),
          undertakingBusinessEntity = (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
        )
      )
    }

  implicit val createUndertakingResponseReads: Reads[UndertakingRef] =
    readResponseFor[UndertakingRef]("createUndertakingResponse") { json =>
      val ref = (json \ "createUndertakingResponse" \ "responseDetail" \ "undertakingReference").as[String]
      JsSuccess(UndertakingRef(ref))
    }

  implicit val updateUndertakingResponseReads: Reads[UndertakingRef] =
    readResponseFor[UndertakingRef]("updateUndertakingResponse") { json =>
      val ref = (json \ "updateUndertakingResponse" \ "responseDetail" \ "undertakingReference").as[String]
      JsSuccess(UndertakingRef(ref))
    }

  implicit val amendUndertakingMemberResponseReads: Reads[Unit] =
    readResponseFor[Unit]("amendUndertakingMemberDataResponse")(_ => JsSuccess(()))

  implicit val amendSubsidyUpdateResponseReads: Reads[Unit] =
    readResponseFor[Unit]("amendUndertakingSubsidyUsageResponse")(_ => JsSuccess((())))

  def readResponseFor[A](responseName: String)(extractValue: JsValue => JsSuccess[A]): Reads[A] = new Reads[A] {
    override def reads(json: JsValue): JsResult[A] = {
      val responseCommon: JsLookupResult = json \ responseName \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case OK => extractValue(json)
        case NOT_OK =>
          throw new EisBadResponseException(
            status = NOT_OK,
            processingDate = (responseCommon \ "processingDate").as[ZonedDateTime],
            statusText = (responseCommon \ "statusText").asOpt[String],
            returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]]
          )
        case _ => JsError("unable to parse status from response")
      }
    }
  }

}
