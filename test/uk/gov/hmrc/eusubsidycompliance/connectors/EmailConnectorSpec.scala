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

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.JsString
import uk.gov.hmrc.eusubsidycompliance.models.EmailRequest
import uk.gov.hmrc.eusubsidycompliance.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailConnectorSpec extends AnyWordSpec with BeforeAndAfter with Matchers with MockFactory with HttpSupport {

  protected implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val (protocol, host, port) = ("http", "host", "123")

  private val config = Configuration(
    ConfigFactory.parseString(s"""
         | microservice.services.email-send {
         |    protocol = "$protocol"
         |    host     = "$host"
         |    port     = $port
         |  }
         |""".stripMargin)
  )
  val validEmailRequest: EmailRequest = EmailRequest("ABC12345", "GB000000000012", "1", "jdoe@example.com")

  private val connector = new EmailConnector(mockHttp, new ServicesConfig(config))

  "EmailConnectorSpec" when {
    "handling request to send an email " when {
      "The server returns a response" in {
        val expectedUrl = s"$protocol://$host:$port/hmrc/email"
        mockPost(expectedUrl, Seq.empty, validEmailRequest)(_)
        () => connector.sendEmail(validEmailRequest) shouldBe Future.successful(HttpResponse(200, "{}"))
      }

    }
  }

}
