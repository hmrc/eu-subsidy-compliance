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

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.types.UndertakingRef

import java.time.LocalDate

sealed trait Update
case class NilSubmissionDate(d: LocalDate) extends Update
case class UndertakingSubsidyAmendment(updates: List[NonHmrcSubsidy]) extends Update

case class SubsidyUpdate(
  undertakingIdentifier: UndertakingRef,
  update: Update
)

object SubsidyUpdate {
  implicit val updateFormat: Format[SubsidyUpdate] = new Format[SubsidyUpdate] {
    override def writes(o: SubsidyUpdate): JsValue =
      o.update match {
        case NilSubmissionDate(d) =>
          Json.obj(
            "undertakingIdentifier" -> o.undertakingIdentifier,
            "nilSubmissionDate" -> JsString(d.toString)
          )
        case UndertakingSubsidyAmendment(updates) =>
          Json.obj(
            "undertakingIdentifier" -> o.undertakingIdentifier,
            "undertakingSubsidyAmendment" -> updates
          )
      }

    override def reads(json: JsValue): JsResult[SubsidyUpdate] = {
      val update: Update =
        if ((json \ "nilSubmissionDate").isDefined) {
          NilSubmissionDate((json \ "nilSubmissionDate").as[LocalDate])
        } else
          UndertakingSubsidyAmendment(
            (json \ "undertakingSubsidyAmendment").as[List[NonHmrcSubsidy]]
          )
      val id = (json \ "undertakingIdentifier").as[UndertakingRef]
      JsSuccess(SubsidyUpdate(id, update))
    }
  }
}
