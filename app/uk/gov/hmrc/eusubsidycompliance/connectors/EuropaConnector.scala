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

package uk.gov.hmrc.eusubsidycompliance.connectors

import play.api.http.HeaderNames.ACCEPT
import uk.gov.hmrc.eusubsidycompliance.models.MonthlyExchangeRate
import uk.gov.hmrc.eusubsidycompliance.connectors.EuropaConnector.reads
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.api.libs.functional.syntax._

@Singleton
class EuropaConnector @Inject() (
  val client: ProxiedHttpClient
) {
  def retrieveMonthlyExchangeRates(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MonthlyExchangeRate]] =
    client.GET[Seq[MonthlyExchangeRate]](
      url = "https://ec.europa.eu/budg/inforeuro/api/public/currencies/gbp",
      headers = Seq(ACCEPT -> "application/vnd.sdmx.data+json;version=1.0.0-wd")
    )
}

object EuropaConnector {
  private val datePatternStr = "dd/MM/yyyy"
  private implicit val localDateFormat: Reads[LocalDate] = Reads.localDateReads(datePatternStr)
  implicit val reads: Reads[MonthlyExchangeRate] = (
    (__ \ "currencyIso").read[String] and
      (__ \ "refCurrencyIso").read[String] and
      (__ \ "amount").read[BigDecimal] and
      (__ \ "dateStart").read[LocalDate] and
      (__ \ "dateEnd").read[LocalDate] and
      Reads.pure(LocalDate.now())
  )(MonthlyExchangeRate.apply _)
}
