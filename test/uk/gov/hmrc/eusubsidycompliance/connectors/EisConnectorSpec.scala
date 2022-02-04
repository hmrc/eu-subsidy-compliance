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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.testutil.WiremockSupport
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class EisConnectorSpec extends AnyWordSpecLike with Matchers with WiremockSupport with ScalaFutures
  with IntegrationPatience {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "EisConnector" when {

    "retrieveUnderTaking is called" should {

      "throw an EisBadResponseException if the response has status NOT_OK" in {
        val app = configuredApplication

        val now = Instant.now()

        server.stubFor(
          post(urlEqualTo("/scp/retrieveundertaking/v1"))
            .willReturn(ok(
              s"""{
                 | "retrieveUndertakingResponse": {
                 |   "responseCommon": {
                 |     "status": "NOT_OK",
                 |     "processingDate": "$now"
                 |   }
                 | }
                 |}""".stripMargin
            ))
        )

        running(app) {
          val underTest = app.injector.instanceOf[EisConnector]
          underTest.retrieveUndertaking(EORI("GB123456789012")).failed.futureValue mustBe a[EisBadResponseException]
        }
      }

    }

  }

  private def configuredApplication: Application =
    new GuiceApplicationBuilder().configure(
      "microservice.services.eis.protocol" -> "http",
      "microservice.services.eis.host" -> "localhost",
      "microservice.services.eis.port" -> server.port(),
  ).build()

}
