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

package uk.gov.hmrc.eusubsidycompliance.persistence

import uk.gov.hmrc.eusubsidycompliance.models.MonthlyExchangeRate
import uk.gov.hmrc.eusubsidycompliance.util.IntegrationBaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class ExchangeRateCacheSpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[MonthlyExchangeRate] {

  override protected val repository = new MonthlyExchangeRateCache(mongoComponent)

  private val today = LocalDate.now()
  private val exchangeRate1 =
    MonthlyExchangeRate("GBP", "EUR", BigDecimal(0.867), LocalDate.of(2023, 9, 1), LocalDate.of(2023, 9, 30), today)
  private val exchangeRate2 =
    MonthlyExchangeRate("GBP", "EUR", BigDecimal(0.807), LocalDate.of(2023, 8, 1), LocalDate.of(2023, 8, 31), today)
  private val exchangeRateSeq = Seq(exchangeRate1, exchangeRate2)

  "ExchangeRateCache" should {

    "return None when the cache is empty" in {
      repository.getMonthlyExchangeRate(LocalDate.of(2023, 9, 30)).futureValue shouldBe None
    }

    "return None when there is no matching item in the cache" in {
      repository.put(exchangeRateSeq).futureValue shouldBe ()
      repository.getMonthlyExchangeRate(LocalDate.of(2058, 9, 1)).futureValue shouldBe None
    }

    "clear the repository when drop is called" in {
      repository.put(exchangeRateSeq).futureValue
      repository.deleteAll().futureValue
      repository.getMonthlyExchangeRate(exchangeRate1.dateEnd).futureValue shouldBe None
    }

    "return the item when present in the cache" in {
      repository.put(exchangeRateSeq).futureValue shouldBe ()
      repository.getMonthlyExchangeRate(exchangeRate1.dateEnd).futureValue shouldBe Some(exchangeRate1)
    }

  }

}
