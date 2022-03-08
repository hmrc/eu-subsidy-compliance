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

import com.fasterxml.jackson.core.JsonParseException
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures._
import uk.gov.hmrc.eusubsidycompliance.test.util.WiremockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class EisConnectorSpec extends AnyWordSpecLike with Matchers with WiremockSupport with ScalaFutures
  with IntegrationPatience {

  private val RetrieveUndertakingPath = "/scp/retrieveundertaking/v1"
  private val CreateUndertakingPath = "/scp/createundertaking/v1"
  private val RetrieveSubsidyPath = "/scp/getundertakingtransactions/v1"

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  "EisConnector" when {

    "retrieveUnderTaking is called" should {

      "return an UnderTaking for a successful request" in {
        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "OK",
             |     "processingDate": "$fixedInstant"
             |   },
             |   "responseDetail": {
             |      "undertakingReference": "$undertakingReference",
             |      "undertakingName": "$undertakingName",
             |      "industrySector": "$sector",
             |      "industrySectorLimit": $industrySectorLimit,
             |      "lastSubsidyUsageUpdt": "$date",
             |      "undertakingBusinessEntity": [{
             |        "businessEntityIdentifier": "$eori",
             |        "leadEORI": true
             |      }]
             |   }
             | }
             |}""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.retrieveUndertaking(EORI("GB123456789012")).futureValue mustBe undertaking
        }

      }

      "return a 404 if a NOT_OK response is received with the not found error code" in {
        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "NOT_OK",
             |     "processingDate": "$fixedInstant",
             |     "returnParameters": [{
             |       "paramName": "ERRORCODE",
             |       "paramValue": "107"
             |     }]
             |   }
             | }
             |}""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.retrieveUndertaking(EORI("GB123456789012")).failed.futureValue mustBe a[UpstreamErrorResponse]
        }
      }

      "throw an EisBadResponseException if the response has status NOT_OK" in {
        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "NOT_OK",
             |     "processingDate": "$fixedInstant"
             |   }
             | }
             |}""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.retrieveUndertaking(EORI("GB123456789012")).failed.futureValue mustBe an[EisBadResponseException]
        }
      }

    }

    "createUndertaking is called" should {

      "return an undertaking reference for a successful create" in {
        givenEisReturns(200, CreateUndertakingPath,
          s"""{
             | "createUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "OK"
             |   },
             |   "responseDetail": {
             |     "undertakingReference": "$undertakingReference"
             |   }
             |  }
             |}
             |""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.createUndertaking(undertaking).futureValue mustBe undertakingReference
        }
      }
    }

    "retrieveSubsidies is called" should {

      val retrieveSubsidiesResponse = s"""{
         | "getUndertakingTransactionResponse": {
         |   "responseCommon": {
         |     "status": "OK"
         |   },
         |   "responseDetail": {
         |     "undertakingIdentifier": "$undertakingReference",
         |     "nonHMRCSubsidyTotalEUR": "$subsidyAmount",
         |     "nonHMRCSubsidyTotalGBP": "$subsidyAmount",
         |     "hmrcSubsidyTotalEUR": "$subsidyAmount",
         |     "hmrcSubsidyTotalGBP": "$subsidyAmount",
         |     "nonHMRCSubsidyUsage": [ {
         |       "subsidyUsageTransactionId": "$subsidyRef",
         |       "allocationDate": "$date",
         |       "submissionDate": "$date",
         |       "publicAuthority": "$publicAuthority",
         |       "traderReference": "$traderRef",
         |       "nonHMRCSubsidyAmtEUR": $subsidyAmount,
         |       "businessEntityIdentifier": "$eori"
         |     } ],
         |     "hmrcSubsidyUsage": [ {
         |       "declarationID": "$declarationId",
         |       "issueDate": "$date",
         |       "acceptanceDate": "$date",
         |       "declarantEORI": "$eori",
         |       "consigneeEORI": "$eori",
         |       "taxType": "$taxType",
         |       "amount": $subsidyAmount,
         |       "tradersOwnRefUCR": "$traderRef"
         |     } ]
         |   }
         | }
         |}
         |""".stripMargin


      "return an UndertakingSubsidies instance for a valid request without specifying a date range" in {

        // When no date is specified the connector falls back to a range of 2000-01-01 to LocalDate.now()
        val requestBody = retrieveSubsidiesRequestWithDates(LocalDate.of(2000, 1, 1), LocalDate.now())

        givenEisReturns(200, RetrieveSubsidyPath, requestBody, retrieveSubsidiesResponse)

        testWithRunningApp { underTest =>
          underTest.retrieveSubsidies(undertakingReference, None).futureValue mustBe undertakingSubsidies
        }
      }

      "return an UndertakingSubsidies instance for a valid request with a date range" in {

        val toDate = date.plusDays(7)

        val requestBody = retrieveSubsidiesRequestWithDates(date, date.plusDays(7))

        givenEisReturns(200, RetrieveSubsidyPath, requestBody, retrieveSubsidiesResponse)

        testWithRunningApp { underTest =>
          underTest.retrieveSubsidies(
            undertakingReference,
            Some((date, toDate))
          ).futureValue mustBe undertakingSubsidies
        }
      }

      "throw an UpstreamErrorResponse exception if EIS returns an Internal Server Error" in {
        givenEisReturns(500, RetrieveSubsidyPath, "Internal Server Error")

        testWithRunningApp { underTest =>
          underTest.retrieveSubsidies(undertakingReference, None).failed.futureValue mustBe a[UpstreamErrorResponse]
        }
      }

      "throw a JsonParseException if EIS returns a response that cannot be parsed" in {
        givenEisReturns(200, RetrieveSubsidyPath, "This is not a valid response")

        testWithRunningApp { underTest =>
          underTest.retrieveSubsidies(undertakingReference, None).failed.futureValue mustBe a[JsonParseException]
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

  private def givenEisReturns(status: Int, url: String, body: String): Unit =
    server.stubFor(
      post(urlEqualTo(url))
        .willReturn(aResponse()
          .withStatus(status)
          .withBody(body)
      ))

  private def givenEisReturns(status: Int, url: String, requestBody: String, responseBody: String): Unit =
    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse()
          .withStatus(status)
          .withBody(responseBody)
        ))

  private def testWithRunningApp(f: EisConnector => Unit): Unit = {
    val app = configuredApplication
    running(app) {
     f(app.injector.instanceOf[EisConnector])
    }
  }

  private def retrieveSubsidiesRequestWithDates(from: LocalDate, to: LocalDate) =
    s"""{
       |  "undertakingIdentifier": "$undertakingReference",
       |  "getNonHMRCUsageTransaction": true,
       |  "getHMRCUsageTransaction": true,
       |  "dateFromNonHMRCSubsidyUsage": "$from",
       |  "dateFromHMRCSubsidyUsage": "$from",
       |  "dateToNonHMRCSubsidyUsage": "$to",
       |  "dateToHMRCSubsidyUsage": "$to"
       |}
       |""".stripMargin

}
