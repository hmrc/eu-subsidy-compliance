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

import com.fasterxml.jackson.core.JsonParseException
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.eusubsidycompliance.models.ExchangeRate
import uk.gov.hmrc.eusubsidycompliance.test.util.WiremockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class EuropaConnectorSpec extends AnyWordSpecLike
  with Matchers
  with WiremockSupport
  with ScalaFutures
  with IntegrationPatience {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val date = LocalDate.of(2022, 1, 3)

  private val requestUrl =
    s"/service/data/EXR/D.GBP.EUR.SP00.A?startPeriod=$date&endPeriod=$date&detail=dataonly&lastNObservations=1"

  private val validResponse =
    """{
      |    "header": {
      |        "id": "972803cb-94d7-4342-a6ec-a69edf56041d",
      |        "test": false,
      |        "prepared": "2022-06-22T12:32:42.940+02:00",
      |        "sender": {
      |            "id": "ECB"
      |        }
      |    },
      |    "dataSets": [
      |        {
      |            "action": "Replace",
      |            "validFrom": "2022-06-22T12:32:42.940+02:00",
      |            "series": {
      |                "0:0:0:0:0": {
      |                    "observations": {
      |                        "0": [
      |                            0.84135
      |                        ]
      |                    }
      |                }
      |            }
      |        }
      |    ],
      |    "structure": {
      |        "links": [
      |            {
      |                "title": "Exchange Rates",
      |                "rel": "dataflow",
      |                "href": "https://sdw-wsrest.ecb.europa.eu:443/service/dataflow/ECB/EXR/1.0"
      |            }
      |        ],
      |        "name": "Exchange Rates",
      |        "dimensions": {
      |            "series": [
      |                {
      |                    "id": "FREQ",
      |                    "name": "Frequency",
      |                    "values": [
      |                        {
      |                            "id": "D",
      |                            "name": "Daily"
      |                        }
      |                    ]
      |                },
      |                {
      |                    "id": "CURRENCY",
      |                    "name": "Currency",
      |                    "values": [
      |                        {
      |                            "id": "GBP",
      |                            "name": "UK pound sterling"
      |                        }
      |                    ]
      |                },
      |                {
      |                    "id": "CURRENCY_DENOM",
      |                    "name": "Currency denominator",
      |                    "values": [
      |                        {
      |                            "id": "EUR",
      |                            "name": "Euro"
      |                        }
      |                    ]
      |                },
      |                {
      |                    "id": "EXR_TYPE",
      |                    "name": "Exchange rate type",
      |                    "values": [
      |                        {
      |                            "id": "SP00",
      |                            "name": "Spot"
      |                        }
      |                    ]
      |                },
      |                {
      |                    "id": "EXR_SUFFIX",
      |                    "name": "Series variation - EXR context",
      |                    "values": [
      |                        {
      |                            "id": "A",
      |                            "name": "Average"
      |                        }
      |                    ]
      |                }
      |            ],
      |            "observation": [
      |                {
      |                    "id": "TIME_PERIOD",
      |                    "name": "Time period or range",
      |                    "role": "time",
      |                    "values": [
      |                        {
      |                            "id": "2022-01-03",
      |                            "name": "2022-01-03",
      |                            "start": "2022-01-03T00:00:00.000+01:00",
      |                            "end": "2022-01-03T23:59:59.999+01:00"
      |                        }
      |                    ]
      |                }
      |            ]
      |        }
      |    }
      |}""".stripMargin

  "EuropaConnector" when {

    "an exchange rate request is made" should {

      "return a successful response for a valid response from the europa API" in {
        givenEuropaReturns(200, requestUrl, validResponse)

        testWithRunningApp { underTest =>
          val result = underTest.retrieveExchangeRate(date)
          result.futureValue mustBe ExchangeRate("GBP", "EUR", BigDecimal(0.84135))
        }
      }

      "throw a JsonParseException for a response that could not be parsed" in {
        givenEuropaReturns(200, requestUrl, "This is not a valid JSON response")

        testWithRunningApp { underTest =>
          val result = underTest.retrieveExchangeRate(date)
          result.failed.futureValue mustBe a[JsonParseException]
        }
      }

      "throw an UpstreamErrorResponse for an internal server error response " in {
        givenEuropaReturns(500, requestUrl, "Error")

        testWithRunningApp { underTest =>
          val result = underTest.retrieveExchangeRate(date)
          result.failed.futureValue mustBe a[UpstreamErrorResponse]
        }
      }

    }

  }

  private def configuredApplication: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.europa.protocol" -> "http",
        "microservice.services.europa.host" -> "localhost",
        "microservice.services.europa.port" -> server.port()
      )
      .build()

  private def testWithRunningApp(f: EuropaConnector => Unit): Unit = {
    val app = configuredApplication
    running(app) {
      f(app.injector.instanceOf[EuropaConnector])
    }
  }

  private def givenEuropaReturns(status: Int, url: String, responseBody: String): Unit =
    server.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

}
