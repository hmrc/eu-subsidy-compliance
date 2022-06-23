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

package uk.gov.hmrc.eusubsidycompliance.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.eusubsidycompliance.connectors.EuropaConnector
import uk.gov.hmrc.eusubsidycompliance.models.ExchangeRate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ExchangeRateServiceSpec extends AnyWordSpecLike
  with Matchers
  with MockFactory
  with ScalaFutures
  with IntegrationPatience {

  private val mockConnector = mock[EuropaConnector]

  private implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val underTest = new ExchangeRateService(mockConnector)

  private val exchangeRate = ExchangeRate("GBP", "EUR", BigDecimal(0.80))

  "ExchangeRateService" when {

    "getExchangeRate is called" should {

      "return a successful response for a valid date" in {
        givenEuropaConnectorReturns(LocalDate.of(2022, 3, 30))(Future(exchangeRate))
        underTest.getExchangeRate(LocalDate.of(2022, 4, 22)).futureValue shouldBe exchangeRate
      }

      "correctly compute the exchange rate date across a year boundary" in {
        givenEuropaConnectorReturns(LocalDate.of(2021, 12, 30))(Future(exchangeRate))
        underTest.getExchangeRate(LocalDate.of(2022, 1, 22)).futureValue shouldBe exchangeRate
      }

    }

  }

  private def givenEuropaConnectorReturns(date: LocalDate)(res: Future[ExchangeRate]): Unit =
    (mockConnector
      .retrieveExchangeRate(_: LocalDate)(_: HeaderCarrier, _: ExecutionContext))
      .expects(date, *, *)
      .returning(res)

}
