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

import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

trait DesHelpers {

  def http: HttpClient
  def servicesConfig: ServicesConfig
  private def headers(eisTokenKey: String) = Seq(
    HeaderNames.CONTENT_TYPE -> ContentTypes.JSON,
    HeaderNames.ACCEPT -> ContentTypes.JSON,
    HeaderNames.DATE -> LocalDateTime
      .now()
      .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("UTC"))),
    "Environment" -> servicesConfig.getConfString("eis.environment", ""),
    "Authorization" -> s"Bearer ${servicesConfig.getConfString(eisTokenKey, "")}"
  )

  def desGet[O](url: String, eisTokenKey: String)(implicit
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    http.GET[O](url, Seq.empty, headers(eisTokenKey))(rds, addHeaders, ec)

  def desPost[I, O](url: String, body: I, eisTokenKey: String)(implicit
    wts: Writes[I],
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    http.POST[I, O](url, body, headers(eisTokenKey))(wts, rds, addHeaders, ec)

  def addHeaders(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = None)

}
