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

package uk.gov.hmrc.eusubsidycompliance.models

import play.api.libs.json.{JsSuccess, JsValue, Json, Reads, Writes}

case class ExchangeRate(from: String, to: String, rate: BigDecimal)

object ExchangeRate {

  // Convenience constructor for the default case
  def apply(rate: BigDecimal): ExchangeRate = ExchangeRate("EUR", "GBP", rate)

  implicit val europaResponseReads: Reads[ExchangeRate] = (json: JsValue) => {
    // Extract the rate from the JSON response which has the following format.
    //
    //     "dataSets": [
    //        {
    //            "action": "Replace",
    //            "validFrom": "2022-06-22T12:32:42.940+02:00",
    //            "series": {
    //                "0:0:0:0:0": {
    //                    "observations": {
    //                        "0": [
    //                            0.84135
    //                        ],
    //                        "1": [
    //                            0.90132
    //                        ]
    //                    }
    //                }
    //            }
    //        }
    //    ],
    //
    // Note - we request the last 2 rate observations for the month and return the penultimate value for that month
    //        in order to align with the Europa exchange rate converter (see https://ec.europa.eu/info/funding-tenders/procedures-guidelines-tenders/information-contractors-and-beneficiaries/exchange-rate-inforeuro_en)
    val rate = ((json \ "dataSets") (0) \ "series" \ "0:0:0:0:0" \ "observations" \ "0") (0).as[BigDecimal]

    JsSuccess(ExchangeRate(rate))
  }

  implicit val exchangeRateWrites: Writes[ExchangeRate] = Json.writes[ExchangeRate]

}

