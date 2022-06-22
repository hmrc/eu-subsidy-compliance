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

package uk.gov.hmrc.eusubsidycompliance.connectors

import play.api.libs.json.{JsSuccess, JsValue, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * A simple connector to fetch GBP to EUR spot rates from the europa ECB API.
 *
 * @param client
 * @param servicesConfig
 */
@Singleton
class EuropaConnector @Inject() (
  val client: HttpClient,
  val servicesConfig: ServicesConfig
) {

  private lazy val europaBasePath = servicesConfig.baseUrl("europa")

  // Daily spot rate for GBP to EUR - see https://sdw-wsrest.ecb.europa.eu/help/ for API docs.
  private val ResourcePath = "service/data/EXR/D.GBP.EUR.SP00.A"

  def retrieveExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EuropaResponse] =
    client.GET[EuropaResponse](
      url = s"$europaBasePath/$ResourcePath",
      headers = Seq("Accept" -> "application/vnd.sdmx.data+json;version=1.0.0-wd"),
      queryParams = Seq(
        "startPeriod" -> date.toString,
        "endPeriod" -> date.toString,
        "detail" -> "dataonly",
        "lastNObservations" -> "1" // We just need the latest rate observation
      )
    )

}

// TODO - move these
object EuropaResponse {

  implicit val europaResponseReads: Reads[EuropaResponse] = (json: JsValue) => {
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
    //                        ]
    //                    }
    //                }
    //            }
    //        }
    //    ],
    //
    // We only ever expect a single rate to be returned in the format shown above.
    val rate = ((json \ "dataSets") (0) \ "series" \ "0:0:0:0:0" \ "observations" \ "0") (0).as[BigDecimal]

    JsSuccess(EuropaResponse("GBP", "EUR", rate))
  }

}

case class EuropaResponse(from: String, to: String, rate: BigDecimal)
