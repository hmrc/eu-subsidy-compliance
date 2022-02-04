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
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.{EORI, IndustrySectorLimit, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.testutil.WiremockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class EisConnectorSpec extends AnyWordSpecLike with Matchers with WiremockSupport with ScalaFutures
  with IntegrationPatience {

  private val RetrieveUndertakingPath = "/scp/retrieveundertaking/v1"
  private val eori = EORI("GB123456789012")
  private val now = Instant.now()

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  "EisConnector" when {

    "retrieveUnderTaking is called" should {

      "return an UnderTaking for a successful request" in {
        val app = configuredApplication

        val reference = UndertakingRef("SomeReference")
        val name = UndertakingName("SomeName")
        val sector = Sector.other
        val industrySectorLimit = IndustrySectorLimit(BigDecimal(200000.00))
        val lastSubUsageDate = now.atZone(ZoneId.of("Europe/London")).toLocalDate

        val expectedUndertaking = Undertaking(
          Some(reference),
          name,
          sector,
          Some(industrySectorLimit),
          Some(lastSubUsageDate),
          List(BusinessEntity(eori, leadEORI = true, None))
        )

        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "OK",
             |     "processingDate": "$now"
             |   },
             |   "responseDetail": {
             |      "undertakingReference": "$reference",
             |      "undertakingName": "$name",
             |      "industrySector": "$sector",
             |      "industrySectorLimit": $industrySectorLimit,
             |      "lastSubsidyUsageUpdt": "$lastSubUsageDate",
             |      "undertakingBusinessEntity": [{
             |        "businessEntityIdentifier": "$eori",
             |        "leadEORI": true
             |      }]
             |   }
             | }
             |}""".stripMargin
        )

        running(app) {
          val underTest = app.injector.instanceOf[EisConnector]
          underTest.retrieveUndertaking(EORI("GB123456789012")).futureValue mustBe expectedUndertaking
        }

      }

      "return a 404 if a NOT_OK response is received with the not found error code" in {
        val app = configuredApplication

        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "NOT_OK",
             |     "processingDate": "$now",
             |     "returnParameters": [{
             |       "paramName": "ERRORCODE",
             |       "paramValue": "107"
             |     }]
             |   }
             | }
             |}""".stripMargin
        )

        running(app) {
          val underTest = app.injector.instanceOf[EisConnector]
          underTest.retrieveUndertaking(EORI("GB123456789012")).failed.futureValue mustBe a[UpstreamErrorResponse]
        }
      }

      "throw an EisBadResponseException if the response has status NOT_OK" in {
        val app = configuredApplication

        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "NOT_OK",
             |     "processingDate": "$now"
             |   }
             | }
             |}""".stripMargin
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

  private def givenEisReturns(status: Int, url: String, body: String) =
    server.stubFor(
      post(urlEqualTo(url))
        .willReturn(aResponse()
          .withStatus(status)
          .withBody(body)
      ))

}
