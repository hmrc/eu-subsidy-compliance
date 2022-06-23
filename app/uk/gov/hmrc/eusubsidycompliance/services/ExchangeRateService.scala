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

import uk.gov.hmrc.eusubsidycompliance.connectors.EuropaConnector
import uk.gov.hmrc.eusubsidycompliance.models.ExchangeRate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExchangeRateService @Inject() (
  europaConnector: EuropaConnector
)(implicit ec: ExecutionContext) {

  def getExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier): Future[ExchangeRate] =
    europaConnector.retrieveExchangeRate(dateForExchangeRate(date))

  // The exchange rate for a given month is the value of the rate on the penultimate day of the preceding month.
  // For example the rate to apply for the whole of April 2022 will be the published rate for 30th March 2022.
  private def dateForExchangeRate(date: LocalDate): LocalDate = {
    val previousMonth = date.minusMonths(1)
    val penultimateDay = previousMonth.lengthOfMonth() - 1

    previousMonth.withDayOfMonth(penultimateDay)
  }

}
