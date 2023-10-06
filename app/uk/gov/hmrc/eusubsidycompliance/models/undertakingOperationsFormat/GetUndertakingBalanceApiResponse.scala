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

package uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eusubsidycompliance.models.types.{IndustrySectorLimit, SubsidyAmount, UndertakingRef}

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

case class GetUndertakingBalanceApiResponse(getUndertakingBalanceResponse: GetUndertakingBalanceResponse)

object GetUndertakingBalanceApiResponse {
  implicit val format: Format[GetUndertakingBalanceApiResponse] = Json.format[GetUndertakingBalanceApiResponse]
}

case class GetUndertakingBalanceResponse(
  responseCommon: ResponseCommon = ResponseCommon(
    EisStatus.OK,
    EisStatusString("ok"),
    LocalDateTime.now,
    None
  ),
  responseDetail: Option[UndertakingBalanceResponse]
)
object GetUndertakingBalanceResponse {

  implicit val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  implicit val format: Format[GetUndertakingBalanceResponse] = Json.format[GetUndertakingBalanceResponse]
}

case class UndertakingBalance(
  undertakingIdentifier: UndertakingRef,
  nonHMRCSubsidyAllocationEUR: Option[SubsidyAmount] = None,
  hmrcSubsidyAllocationEUR: Option[SubsidyAmount] = None,
  industrySectorLimit: IndustrySectorLimit,
  totalEUR: SubsidyAmount,
  totalGBP: SubsidyAmount,
  conversionRate: SubsidyAmount
) {
  val availableBalanceEUR: SubsidyAmount = SubsidyAmount(industrySectorLimit - totalEUR)
  val availableBalanceGBP: SubsidyAmount = SubsidyAmount(industrySectorLimit - totalGBP)
  val nationalCapBalanceEUR: IndustrySectorLimit = industrySectorLimit

}

case class UndertakingBalanceResponse(
  undertakingIdentifier: UndertakingRef,
  nonHMRCSubsidyAllocationEUR: Option[SubsidyAmount] = None,
  hmrcSubsidyAllocationEUR: Option[SubsidyAmount] = None,
  industrySectorLimit: IndustrySectorLimit,
  availableBalanceEUR: SubsidyAmount,
  availableBalanceGBP: SubsidyAmount,
  conversionRate: SubsidyAmount,
  nationalCapBalanceEUR: IndustrySectorLimit
)

object UndertakingBalanceResponse {
  def apply(ub: UndertakingBalance): UndertakingBalanceResponse =
    UndertakingBalanceResponse(
      undertakingIdentifier = ub.undertakingIdentifier,
      nonHMRCSubsidyAllocationEUR = None,
      hmrcSubsidyAllocationEUR = None,
      industrySectorLimit = ub.industrySectorLimit,
      availableBalanceEUR = ub.availableBalanceEUR,
      availableBalanceGBP = ub.availableBalanceGBP,
      conversionRate = ub.conversionRate,
      nationalCapBalanceEUR = ub.nationalCapBalanceEUR
    )

  implicit val format: Format[UndertakingBalanceResponse] = Json.format[UndertakingBalanceResponse]
}
