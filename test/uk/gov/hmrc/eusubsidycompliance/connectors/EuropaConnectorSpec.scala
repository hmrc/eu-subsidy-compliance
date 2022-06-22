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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class EuropaConnectorSpec extends AnyWordSpecLike
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  "EuropaConnector" when {

    // TODO - initial placeholder to verify that we can hit the real API and parse the response
    "a request is made" in {

      testWithRunningApp { underTest =>
        val result = underTest.retrieveExchangeRate("GBP", "EUR", LocalDate.of(2022, 4, 29))
        result.futureValue mustBe EuropaResponse("EUR", "GBP", BigDecimal(0.84135))
      }


    }

  }

  private def configuredApplication: Application =
    new GuiceApplicationBuilder()
      .configure(
        // TODO - configure this to point to wiremock
        "microservice.services.eis.protocol" -> "https",
        "microservice.services.eis.host" -> "foo",
        "microservice.services.eis.port" -> "443"
      )
      .build()

  private def testWithRunningApp(f: EuropaConnector => Unit): Unit = {
    val app = configuredApplication
    running(app) {
      f(app.injector.instanceOf[EuropaConnector])
    }
  }

}
