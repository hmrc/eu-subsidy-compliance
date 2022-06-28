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
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures.exchangeRate
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
  private val yearMonth = "2022-01"

  private val requestUrl =
    s"/service/data/EXR/D.GBP.EUR.SP00.A?startPeriod=$yearMonth&endPeriod=$yearMonth&detail=dataonly&lastNObservations=2"

  private val validResponse =
    """{
      |    "header": {
      |        "id": "dfd59cc9-9f2b-4bb1-905d-42b22f1780c6",
      |        "test": false,
      |        "prepared": "2022-06-27T17:18:59.359+02:00",
      |        "sender": {
      |            "id": "ECB"
      |        }
      |    },
      |    "dataSets": [
      |        {
      |            "action": "Replace",
      |            "validFrom": "2022-06-27T17:18:59.359+02:00",
      |            "series": {
      |                "0:0:0:0:0": {
      |                    "observations": {
      |                        "0": [
      |                            0.8
      |                        ],
      |                        "1": [
      |                            0.90088
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
      |                            "id": "2020-05-28",
      |                            "name": "2020-05-28",
      |                            "start": "2020-05-28T00:00:00.000+02:00",
      |                            "end": "2020-05-28T23:59:59.999+02:00"
      |                        },
      |                        {
      |                            "id": "2020-05-29",
      |                            "name": "2020-05-29",
      |                            "start": "2020-05-29T00:00:00.000+02:00",
      |                            "end": "2020-05-29T23:59:59.999+02:00"
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
          result.futureValue mustBe exchangeRate
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
      ).build()


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
