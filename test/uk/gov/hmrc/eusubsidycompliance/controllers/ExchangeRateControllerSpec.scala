/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliance.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.models.MonthlyExchangeRate
import uk.gov.hmrc.eusubsidycompliance.services.ExchangeRateService
import uk.gov.hmrc.eusubsidycompliance.test.FakeAuthenticator
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class ExchangeRateControllerSpec extends PlaySpec with MockFactory with ScalaFutures with IntegrationPatience {

  private val mockExchangeRateService = mock[ExchangeRateService]

  private val exchangeRate = MonthlyExchangeRate(
    "EUR",
    "GBP",
    BigDecimal(0.891),
    LocalDate.of(2023, 1, 1),
    LocalDate.of(2023, 1, 31),
    LocalDate.now()
  )

  "ExchangeRateController" when {

    "retrieveExchangeRate is called" should {

      "return a successful response with exchange rate when found" in {
        val app = configuredAppInstance

        givenRetrieveCachedMonthlyExchangeRate(Future.successful(Some(exchangeRate)))

        running(app) {
          val request = FakeRequest(GET, routes.ExchangeRateController.retrieveExchangeRate("2023-01-31").url)
          val result = route(app, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(exchangeRate)
        }
      }

      "return a HTTP 404 if exchange rate not found" in {
        val app = configuredAppInstance

        givenRetrieveCachedMonthlyExchangeRate(Future.successful(None))

        running(app) {
          val request = FakeRequest(GET, routes.ExchangeRateController.retrieveExchangeRate("2023-01-31").url)
          val result = route(app, request).value

          status(result) mustBe NOT_FOUND
        }
      }

      "throw an exception if the service call fails" in {
        val app = configuredAppInstance

        givenRetrieveCachedMonthlyExchangeRate(Future.failed(new RuntimeException("Service failed")))

        running(app) {
          val request = FakeRequest(GET, routes.ExchangeRateController.retrieveExchangeRate("2023-01-31").url)

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }
    }
  }

  private def configuredAppInstance = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "microservice.metrics.graphite.enabled" -> false
    )
    .overrides(
      bind[ExchangeRateService].to(mockExchangeRateService),
      bind[Authenticator].to(new FakeAuthenticator)
    )
    .build()

  private def givenRetrieveCachedMonthlyExchangeRate(res: Future[Option[MonthlyExchangeRate]]): Unit =
    (mockExchangeRateService
      .retrieveCachedMonthlyExchangeRate(_: LocalDate)(_: HeaderCarrier))
      .expects(*, *)
      .returning(res)
}
