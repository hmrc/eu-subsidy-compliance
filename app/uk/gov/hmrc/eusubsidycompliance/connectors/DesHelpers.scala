/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE, DATE}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

trait DesHelpers {

  def http: HttpClientV2

  def servicesConfig: ServicesConfig

  def desPost[I, O](url: String, body: I, eisTokenKey: String)(implicit
    wts: Writes[I],
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    http
      .post(url"$url")
      .setHeader(headers(eisTokenKey): _*)
      .withBody(Json.toJson(body))
      .execute[O](implicitly[HttpReads[O]], implicitly[ExecutionContext])

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = None)

  private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("UTC"))

  def headers(eisTokenKey: String) = Seq(
    CONTENT_TYPE -> JSON,
    ACCEPT -> JSON,
    DATE -> LocalDateTime.now().format(dateTimeFormatter),
    "Environment" -> servicesConfig.getConfString("eis.environment", ""),
    "Authorization" -> s"Bearer ${servicesConfig.getConfString(eisTokenKey, "")}"
  )
}
