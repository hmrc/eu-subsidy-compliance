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

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI

import java.time.LocalDate

case class BusinessEntityUpdate(
  amendmentType: AmendmentType,
  amendmentEffectiveDate: LocalDate,
  businessEntity: BusinessEntity
)

object BusinessEntityUpdate {

  private implicit val businessEntityReads: Reads[BusinessEntity] = (json: JsValue) =>
    JsSuccess(
      BusinessEntity(
        (json \ "businessEntityIdentifier").as[EORI],
        (json \ "leadEORIIndicator").as[Boolean]
      )
    )

  private implicit val businessEntityWrites: Writes[BusinessEntity] = (
    (JsPath \ "businessEntityIdentifier").write[EORI] and
      (JsPath \ "leadEORIIndicator").write[Boolean]
  )(unlift(BusinessEntity.unapply))

  implicit val businessEntityUpdateReads: Reads[BusinessEntityUpdate] = (json: JsValue) =>
    JsSuccess(
      BusinessEntityUpdate(
        (json \ "amendmentType").as[AmendmentType],
        (json \ "amendmentEffectiveDate").as[LocalDate],
        (json \ "businessEntity").as[BusinessEntity]
      )
    )

  implicit val businessEntityUpdateWrites: Writes[BusinessEntityUpdate] = (o: BusinessEntityUpdate) =>
    Json.obj(
      "amendmentType" -> o.amendmentType,
      "amendmentEffectiveDate" -> o.amendmentEffectiveDate,
      "businessEntity" -> o.businessEntity
    )

}
