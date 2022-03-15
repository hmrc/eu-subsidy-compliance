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
import uk.gov.hmrc.eusubsidycompliance.models.types.{EORI, EisAmendmentType, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, Undertaking, UndertakingBusinessEntityUpdate}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

package object digital {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  implicit val undertakingWrites = new Writes[Undertaking] {
    override def writes(o: Undertaking): JsValue = {
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
  }

  implicit val undertakingReads: Reads[Undertaking] =
    readResponseFor[Undertaking]("retrieveUndertakingResponse") { json =>
      val responseDetail: JsLookupResult =
        json \ "retrieveUndertakingResponse" \ "responseDetail"
      val undertakingRef: Option[String] = (responseDetail \ "undertakingReference").asOpt[String]
      val undertakingName: UndertakingName = (responseDetail \ "undertakingName").as[UndertakingName]
      val industrySector: Sector = (responseDetail \ "industrySector").as[Sector]
      val industrySectorLimit: IndustrySectorLimit =
        (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit]
      // TODO - review fold here - should probably be a map - review code for other examples of this
      val lastSubsidyUsageUpdt: Option[LocalDate] = (responseDetail \ "lastSubsidyUsageUpdt")
        .asOpt[String]
        .fold(Option.empty[LocalDate])(lastSubsidyUsageUpdt =>
          LocalDate.parse(lastSubsidyUsageUpdt, eis.oddEisDateFormat).some
        )
      val undertakingBusinessEntity: List[BusinessEntity] =
        (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
      JsSuccess(
        Undertaking(
          undertakingRef.map(UndertakingRef(_)),
          undertakingName,
          industrySector,
          industrySectorLimit.some,
          lastSubsidyUsageUpdt,
          undertakingBusinessEntity
        )
      )
    }

  // provides json for EIS retrieveUndertaking call
  implicit val retrieveUndertakingEORIWrites: Writes[EORI] = new Writes[EORI] {

    override def writes(o: EORI): JsValue = Json.obj(
      "retrieveUndertakingRequest" -> Json.obj(
        "requestCommon" -> RequestCommon("RetrieveUndertaking"),
        "requestDetail" -> Json.obj(
          "idType" -> "EORI",
          "idValue" -> o.toString
        )
      )
    )
  }

  // provides json for EIS Amend Undertaking Member Data (business entities) call
  implicit val amendUndertakingMemberDataWrites: Writes[UndertakingBusinessEntityUpdate] =
    new Writes[UndertakingBusinessEntityUpdate] {
      override def writes(o: UndertakingBusinessEntityUpdate): JsValue = Json.obj(
        "undertakingIdentifier" -> JsString(o.undertakingIdentifier),
        "undertakingComplete" -> JsBoolean(true),
        "memberAmendments" -> o.businessEntityUpdates
      )
    }

  // provides json for EIS updateUndertaking call
  def updateUndertakingWrites(
    amendmentType: EisAmendmentType = EisAmendmentType.A
  ): Writes[Undertaking] = {
    val amendUndertakingWrites: Writes[Undertaking] = new Writes[Undertaking] {
      override def writes(o: Undertaking): JsValue =
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
    }
    amendUndertakingWrites
  }

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

  // TODO - probably better expressed as an enum.
  private val OK = "OK"
  private val NOT_OK = "NOT_OK"

  private def readResponseFor[A](responseName: String)(extractValue: JsValue => JsSuccess[A]) = new Reads[A] {
    override def reads(json: JsValue): JsResult[A] = {
      val responseCommon: JsLookupResult = json \ responseName \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case OK => extractValue(json)
        case NOT_OK =>
          val processingDate = (responseCommon \ "processingDate").as[ZonedDateTime]
          val statusText = (responseCommon \ "statusText").asOpt[String]
          val returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]]
          // TODO - this should probably be handled in the connector.
          throw new EisBadResponseException(NOT_OK, processingDate, statusText, returnParameters)
        case _ => JsError("unable to parse status from response")
      }
    }
  }

}
