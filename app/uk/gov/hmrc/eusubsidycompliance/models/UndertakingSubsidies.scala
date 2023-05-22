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

import play.api.libs.json.{Format, JsSuccess, Json, Reads}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.readResponseFor
import uk.gov.hmrc.eusubsidycompliance.models.types.{SubsidyAmount, UndertakingRef}

case class UndertakingSubsidies(
  undertakingIdentifier: UndertakingRef,
  nonHMRCSubsidyTotalEUR: SubsidyAmount,
  nonHMRCSubsidyTotalGBP: SubsidyAmount,
  hmrcSubsidyTotalEUR: SubsidyAmount,
  hmrcSubsidyTotalGBP: SubsidyAmount,
  nonHMRCSubsidyUsage: List[NonHmrcSubsidy],
  hmrcSubsidyUsage: List[HmrcSubsidy]
)

object UndertakingSubsidies {

  implicit val eisRetrieveUndertakingSubsidiesResponseRead: Reads[UndertakingSubsidies] =
    readResponseFor[UndertakingSubsidies]("getUndertakingTransactionResponse") { json =>
      val responseDetail = json \ "getUndertakingTransactionResponse" \ "responseDetail"
      val ref =
        (responseDetail \ "undertakingIdentifier").as[String]
      val nonHmrcTotalEur =
        (responseDetail \ "nonHMRCSubsidyTotalEUR").as[BigDecimal]
      val nonHmrcTotalGbp =
        (responseDetail \ "nonHMRCSubsidyTotalGBP").as[BigDecimal]
      val hmrcTotalEur =
        (responseDetail \ "hmrcSubsidyTotalEUR").as[BigDecimal]
      val hmrcTotalGbp =
        (responseDetail \ "hmrcSubsidyTotalGBP").as[BigDecimal]
      val nonHmrcUsage = (responseDetail \ "nonHMRCSubsidyUsage")
        .asOpt[List[NonHmrcSubsidy]]
      val hmrcUsage = (responseDetail \ "hmrcSubsidyUsage")
        .asOpt[List[HmrcSubsidy]]
      JsSuccess(
        UndertakingSubsidies(
          UndertakingRef(ref),
          SubsidyAmount(nonHmrcTotalEur),
          SubsidyAmount(nonHmrcTotalGbp),
          SubsidyAmount(hmrcTotalEur),
          SubsidyAmount(hmrcTotalGbp),
          nonHmrcUsage.getOrElse(List.empty),
          hmrcUsage.getOrElse(List.empty)
        )
      )
    }
  implicit val format: Format[UndertakingSubsidies] = Json.format[UndertakingSubsidies]
}
