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
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, HmrcSubsidy, NonHmrcSubsidy, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException
import uk.gov.hmrc.eusubsidycompliance.models.types.{DeclarationID, EORI, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, TaxType, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.testutil.WiremockSupport
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class EisConnectorSpec extends AnyWordSpecLike with Matchers with WiremockSupport with ScalaFutures
  with IntegrationPatience {

  private val RetrieveUndertakingPath = "/scp/retrieveundertaking/v1"
  private val CreateUndertakingPath = "/scp/createundertaking/v1"
  private val RetrieveSubsidyPath = "/scp/getundertakingtransactions/v1"

  // TODO - move these into a separate Fixtures object
  private val eori = EORI("GB123456789012")
  private val now = Instant.now()

  private val underTakingReference = UndertakingRef("SomeReference")
  private val name = UndertakingName("SomeName")
  private val sector = Sector.other
  private val industrySectorLimit = IndustrySectorLimit(BigDecimal(200000.00))
  private val date = now.atZone(ZoneId.of("Europe/London")).toLocalDate
  private val amount = SubsidyAmount(BigDecimal(123.45))

  private val anUnderTaking = Undertaking(
    Some(underTakingReference),
    name,
    sector,
    Some(industrySectorLimit),
    Some(date),
    List(BusinessEntity(eori, leadEORI = true, None))
  )

  private val subsidyRef = SubsidyRef("ABC12345")
  private val declarationId = DeclarationID("12345")
  private val traderRef = TraderRef("SomeTraderReference")
  private val taxType = TaxType("1")
  private val publicAuthority = "SomePublicAuthority"

  private val anHmrcSubsidy = HmrcSubsidy(
    declarationID = declarationId,
    issueDate = Some(date),
    acceptanceDate = date,
    declarantEORI = eori,
    consigneeEORI = eori,
    taxType = Some(taxType),
    amount = Some(amount),
    tradersOwnRefUCR = Some(traderRef)
  )

  private val aNonHmrcSubsidy = NonHmrcSubsidy(
    subsidyUsageTransactionID = Some(subsidyRef),
    allocationDate = date,
    submissionDate = date,
    publicAuthority = Some(publicAuthority),
    traderReference = Some(traderRef),
    nonHMRCSubsidyAmtEUR = amount,
    businessEntityIdentifier = Some(eori),
    amendmentType = None,
  )

  private val anUnderTakingSubsidies = UndertakingSubsidies(
    undertakingIdentifier = underTakingReference,
    nonHMRCSubsidyTotalEUR = amount,
    nonHMRCSubsidyTotalGBP = amount,
    hmrcSubsidyTotalEUR = amount,
    hmrcSubsidyTotalGBP = amount,
    nonHMRCSubsidyUsage = List(aNonHmrcSubsidy),
    hmrcSubsidyUsage = List(anHmrcSubsidy)
  )

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  "EisConnector" when {

    "retrieveUnderTaking is called" should {

      "return an UnderTaking for a successful request" in {
        givenEisReturns(200, RetrieveUndertakingPath,
          s"""{
             | "retrieveUndertakingResponse": {
             |   "responseCommon": {
             |     "status": "OK",
             |     "processingDate": "$now"
             |   },
             |   "responseDetail": {
             |      "undertakingReference": "$underTakingReference",
             |      "undertakingName": "$name",
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
          underTest.retrieveUndertaking(EORI("GB123456789012")).futureValue mustBe anUnderTaking
        }

      }

      "return a 404 if a NOT_OK response is received with the not found error code" in {
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
             |     "processingDate": "$now"
             |   }
             | }
             |}""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.retrieveUndertaking(EORI("GB123456789012")).failed.futureValue mustBe a[EisBadResponseException]
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
             |     "undertakingReference": "$underTakingReference"
             |   }
             |  }
             |}
             |""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.createUndertaking(anUnderTaking).futureValue mustBe underTakingReference
        }
      }
    }

    "retrieveSubsidies" should {

      "return an UndertakingSubsidies instance for a valid request" in {
        givenEisReturns(200, RetrieveSubsidyPath,
          s"""{
            | "getUndertakingTransactionResponse": {
            |   "responseCommon": {
            |     "status": "OK"
            |   },
            |   "responseDetail": {
            |     "undertakingIdentifier": "$underTakingReference",
            |     "nonHMRCSubsidyTotalEUR": "$amount",
            |     "nonHMRCSubsidyTotalGBP": "$amount",
            |     "hmrcSubsidyTotalEUR": "$amount",
            |     "hmrcSubsidyTotalGBP": "$amount",
            |     "nonHMRCSubsidyUsage": [ {
            |       "subsidyUsageTransactionID": "$subsidyRef",
            |       "allocationDate": "$date",
            |       "submissionDate": "$date",
            |       "publicAuthority": "$publicAuthority",
            |       "traderReference": "$traderRef",
            |       "nonHMRCSubsidyAmtEUR": $amount,
            |       "businessEntityIdentifier": "$eori"
            |     } ],
            |     "hmrcSubsidyUsage": [ {
            |       "declarationID": "$declarationId",
            |       "issueDate": "$date",
            |       "acceptanceDate": "$date",
            |       "declarantEORI": "$eori",
            |       "consigneeEORI": "$eori",
            |       "taxType": "$taxType",
            |       "amount": $amount,
            |       "tradersOwnRefUCR": "$traderRef"
            |     } ]
            |   }
            | }
            |}
            |""".stripMargin
        )

        testWithRunningApp { underTest =>
          underTest.retrieveSubsidies(underTakingReference).futureValue mustBe anUnderTakingSubsidies
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

  private def testWithRunningApp(f: EisConnector => Unit): Unit = {
    val app = configuredApplication
    running(app) {
     f(app.injector.instanceOf[EisConnector])
    }
  }

}
