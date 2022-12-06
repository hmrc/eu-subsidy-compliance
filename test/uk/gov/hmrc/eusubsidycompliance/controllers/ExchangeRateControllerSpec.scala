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

package uk.gov.hmrc.eusubsidycompliance.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.models.ExchangeRate
import uk.gov.hmrc.eusubsidycompliance.services.ExchangeRateService
import uk.gov.hmrc.eusubsidycompliance.test.FakeAuthenticator
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures.exchangeRate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExchangeRateControllerSpec extends PlaySpec with MockFactory with ScalaFutures with IntegrationPatience {

  private val mockExchangeRateService = mock[ExchangeRateService]

  private val dateString = "2022-02-03"

  "ExchangeRateController" when {

    "getExchangeRate is called" should {

      "return a successful response for a valid request" in {

        val app = configuredAppInstance

        givenExchangeRateServiceReturns(LocalDate.of(2022, 2, 3))(Future(exchangeRate))

        running(app) {
          val request =
            FakeRequest(GET, routes.ExchangeRateController.getExchangeRate(dateString).url)

          val result = route(app, request).value

          status(result) mustBe 200
          contentAsJson(result) mustBe Json.toJson(exchangeRate)
        }

      }

      "return a bad request for a request with an invalid date string" in {

        val app = configuredAppInstance

        running(app) {
          val request =
            FakeRequest(GET, routes.ExchangeRateController.getExchangeRate(dateString).url)

          val result = route(app, request).value

          status(result) mustBe 400
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

  def givenExchangeRateServiceReturns(date: LocalDate)(res: Future[ExchangeRate]): Unit =
    (mockExchangeRateService
      .getExchangeRate(_: LocalDate)(_: HeaderCarrier))
      .expects(date, *)
      .returning(res)

}
