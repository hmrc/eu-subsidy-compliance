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

import org.slf4j.LoggerFactory
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EuropaConnector @Inject() (
  val client: HttpClient,
  val servicesConfig: ServicesConfig
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // TODO - add this to config
//  private lazy val europaBasePath = servicesConfig.baseUrl("europa")

  // TODO - this should return an Either
  def retrieveExchangeRate(
    // TODO - introduce currency code type - or remove these params for now?
    from: String,
    to: String,
    date: LocalDate
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EuropaResponse] = {
    val url = "https://sdw-wsrest.ecb.europa.eu/service/data/EXR/D.GBP.EUR.SP00.A"
    val result = client.GET[EuropaResponse](
      url = url,
      headers = Seq("Accept" -> "application/vnd.sdmx.data+json;version=1.0.0-wd"),
      queryParams = Seq(
        // TODO - use date parameter
        "startPeriod" -> "2022-01-03",
        "endPeriod" -> "2022-01-03",
        "detail" -> "dataonly",
        "lastNObservations" -> "1"
      )
    )
    result.map(r => "Got result: $r")
    result
  }

}

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

    JsSuccess(EuropaResponse("EUR", "GBP", rate))
  }

}

case class EuropaResponse(from: String, to: String, rate: BigDecimal)
