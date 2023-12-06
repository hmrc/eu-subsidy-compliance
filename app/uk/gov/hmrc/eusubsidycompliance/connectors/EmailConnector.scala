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
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliance.models.EmailRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject() (
  client: HttpClient,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  private lazy val baseUrl: String = servicesConfig.baseUrl("email")
  private lazy val sendEmailUrl: String = s"$baseUrl/hmrc/email"

  def sendEmail(request: EmailRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    client.POST[EmailRequest, HttpResponse](sendEmailUrl, request)

}
