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

import uk.gov.hmrc.eusubsidycompliance.models.ExchangeRate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * A simple connector to fetch GBP to EUR spot rates from the europa ECB API.
 */
@Singleton
class EuropaConnector @Inject() (
  val client: ProxiedHttpClient,
  val servicesConfig: ServicesConfig
) {

  private lazy val europaBasePath = servicesConfig.baseUrl("europa")

  private val yearMonthFormatter = DateTimeFormatter.ofPattern("u-MM")

  // Daily spot rate for GBP to EUR - see https://sdw-wsrest.ecb.europa.eu/help/ for API docs.
  private val ResourcePath = "service/data/EXR/D.GBP.EUR.SP00.A"

  // We request the last two rates for the month of the specified date. The penultimate rate will be the rate that
  // applies which corresponds to the first of the two rates returned.
  // The europa response parsing takes care of selecting this rate. See ExchangeRate companion object.
  def retrieveApplicableExchangeRate(date: LocalDate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ExchangeRate] =
    client.GET[ExchangeRate](
      url = s"$europaBasePath/$ResourcePath",
      headers = Seq("Accept" -> "application/vnd.sdmx.data+json;version=1.0.0-wd"),
      queryParams = Seq(
        "startPeriod" -> yearMonthFormatter.format(date),
        "endPeriod" -> yearMonthFormatter.format(date),
        "detail" -> "dataonly",
        "lastNObservations" -> "2"
      )
    )

}