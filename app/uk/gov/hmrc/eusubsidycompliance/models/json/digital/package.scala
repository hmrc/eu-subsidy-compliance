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

package uk.gov.hmrc.eusubsidycompliance.models.json

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.{Params, RequestCommon}
import uk.gov.hmrc.eusubsidycompliance.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliance.models.types.{EORI, EisAmendmentType, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, Undertaking, UndertakingBusinessEntityUpdate}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

package object digital {

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private val OK = "OK"
  private val NOT_OK = "NOT_OK"

  implicit val undertakingWrites: Writes[Undertaking] = (o: Undertaking) => {
    val lead: BusinessEntity =
      o.undertakingBusinessEntity match {
        case h :: Nil => h
        case _ =>
          throw new IllegalStateException(s"unable to create undertaking with missing or multiple business entities")
      }

    Json.obj(
      "createUndertakingRequest" -> Json.obj(
        "requestCommon" -> RequestCommon("CreateNewUndertaking"),
        "requestDetail" -> Json.obj(
          "undertakingName" -> o.name,
          "industrySector" -> o.industrySector,
          "businessEntity" ->
            Json.obj(
              "idType" -> "EORI",
              "idValue" -> JsString(lead.businessEntityIdentifier)
            ),
          "undertakingStartDate" -> dateFormatter.format(LocalDate.now)
        )
      )
    )
  }

  implicit val undertakingReads: Reads[Undertaking] =
    readResponseFor[Undertaking]("retrieveUndertakingResponse") { json =>
      val responseDetail = json \ "retrieveUndertakingResponse" \ "responseDetail"
      JsSuccess(
        Undertaking(
          reference = (responseDetail \ "undertakingReference").asOpt[String].map(UndertakingRef(_)),
          name = (responseDetail \ "undertakingName").as[UndertakingName],
          industrySector = (responseDetail \ "industrySector").as[Sector],
          industrySectorLimit = (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit].some,
          lastSubsidyUsageUpdt = (responseDetail \ "lastSubsidyUsageUpdt")
            .asOpt[String]
            .map(ds => LocalDate.parse(ds, eis.oddEisDateFormat)),
          undertakingBusinessEntity = (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
        )
      )
    }

  implicit val retrieveUndertakingEORIWrites: Writes[EORI] = (o: EORI) =>
    Json.obj(
      "retrieveUndertakingRequest" -> Json.obj(
        "requestCommon" -> RequestCommon("RetrieveUndertaking"),
        "requestDetail" -> Json.obj(
          "idType" -> "EORI",
          "idValue" -> s"$o" // Explicitly stringify the EORI here to prevent recursive call to this Writes instance.
        )
      )
    )

  implicit val amendUndertakingMemberDataWrites: Writes[UndertakingBusinessEntityUpdate] =
    (o: UndertakingBusinessEntityUpdate) =>
      Json.obj(
        "undertakingIdentifier" -> JsString(o.undertakingIdentifier),
        "undertakingComplete" -> JsBoolean(true),
        "memberAmendments" -> o.businessEntityUpdates
      )

  def updateUndertakingWrites(amendmentType: EisAmendmentType = EisAmendmentType.A): Writes[Undertaking] =
    (o: Undertaking) =>
      Json.obj(
        "updateUndertakingRequest" -> Json.obj(
          "requestCommon" -> RequestCommon("UpdateUndertaking"),
          "requestDetail" -> Json.obj(
            "amendmentType" -> amendmentType,
            "undertakingId" -> o.reference,
            "undertakingName" -> o.name,
            "industrySector" -> o.industrySector,
            "disablementStartDate" -> dateFormatter.format(LocalDate.now)
          )
        )
      )

  implicit val undertakingCreateResponseReads: Reads[UndertakingRef] =
    readResponseFor[UndertakingRef]("createUndertakingResponse") { json =>
      val ref = (json \ "createUndertakingResponse" \ "responseDetail" \ "undertakingReference").as[String]
      JsSuccess(UndertakingRef(ref))
    }

  implicit val undertakingUpdateResponseReads: Reads[UndertakingRef] =
    readResponseFor[UndertakingRef]("updateUndertakingResponse") { json =>
      val ref = (json \ "updateUndertakingResponse" \ "responseDetail" \ "undertakingReference").as[String]
      JsSuccess(UndertakingRef(ref))
    }

  implicit val amendUndertakingMemberDataResponseReads: Reads[Unit] =
    readResponseFor[Unit]("amendUndertakingMemberDataResponse")(_ => JsSuccess(Unit))

  implicit val amendSubsidyResponseReads: Reads[Unit] =
    readResponseFor[Unit]("amendUndertakingSubsidyUsageResponse")(_ => JsSuccess(Unit))

  private def readResponseFor[A](responseName: String)(extractValue: JsValue => JsSuccess[A]) = new Reads[A] {
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
