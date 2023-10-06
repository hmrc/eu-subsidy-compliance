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

package uk.gov.hmrc.eusubsidycompliance.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisAmendmentType, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.{GetUndertakingBalanceApiResponse, GetUndertakingBalanceRequest, GetUndertakingBalanceResponse}
import uk.gov.hmrc.eusubsidycompliance.test.FakeAuthenticator
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures._
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class UndertakingControllerSpec extends PlaySpec with MockFactory with ScalaFutures with IntegrationPatience {

  private val mockEisConnector = mock[EisConnector]
  private val mockTimeProvider = mock[TimeProvider]

  "UndertakingController" when {

    "retrieve is called" should {

      "return a successful response for a valid request where the undertaking exists" in {

        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Right(undertaking))
        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.retrieve(eori).url)
          val result = route(app, request).value

          status(result) mustBe OK
        }

      }

      "return a HTTP 404 if the connector returns an 404 error" in {
        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Left(ConnectorError(NOT_FOUND, "not found")))
        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.retrieve(eori).url)
          val result = route(app, request).value

          status(result) mustBe NOT_FOUND
        }
      }

      "return a HTTP 406 if the connector returns an 404 error" in {
        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Left(ConnectorError(NOT_ACCEPTABLE, "eori not in EMTP")))
        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.retrieve(eori).url)
          val result = route(app, request).value

          status(result) mustBe NOT_ACCEPTABLE
        }
      }

      "return a HTTP 500 if the connector returns any other error" in {
        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Left(ConnectorError(INTERNAL_SERVER_ERROR, "ruh roh!")))
        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.retrieve(eori).url)
          val result = route(app, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }

      }
    }

    "updateUndertaking is called" should {

      "return a successful response for a valid request" in {
        val app = configuredAppInstance

        givenUpdateUndertaking(Future.successful(undertakingReference), EisAmendmentType.A)

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.updateUndertaking.url)
            .withJsonBody(Json.toJson(undertaking))

          val result = route(app, request).value

          status(result) mustBe OK
        }
      }
    }

    "deleteMember is called" should {

      "return a successful response for a valid request" in {
        val app = configuredAppInstance

        givenDeleteMember(Future.successful((): Unit))
        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.deleteMember(undertakingReference).url)
            .withJsonBody(Json.toJson(businessEntity))

          val result = route(app, request).value

          status(result) mustBe OK
        }
      }
    }

    "addMember is called" should {

      "return a successful response for a valid request" in {
        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Right(undertaking))
        givenAddMember(Future.successful((): Unit))

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.addMember(undertakingReference).url)
            .withJsonBody(Json.toJson(businessEntity))
          val result = route(app, request).value

          status(result) mustBe OK
        }
      }
    }

    "updateSubsidy is called" should {

      "return a successful response for a valid request" in {
        val app = configuredAppInstance

        givenUpdateSubsidy(Future.successful((): Unit))

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.updateSubsidy().url)
            .withJsonBody(Json.toJson(SubsidyUpdate(undertakingReference, NilSubmissionDate(date))))
          val result = route(app, request).value

          status(result) mustBe OK
        }
      }
    }

    "create undertaking is called" should {

      "return a valid response for a successful to create undertaking" in {

        givenCreateUndertakingReturns(Future.successful(undertakingReference))
        givenUpdateSubsidy(Future.successful((): Unit))
        givenTimeProviderReturnsDate(date)

        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.create.url)
            .withJsonBody(Json.toJson(undertaking))

          val result = route(app, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(undertakingReference)
        }
      }

      "throw an exception if the call to EIS to create undertaking fails" in {
        givenCreateUndertakingReturns(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.create.url)
            .withJsonBody(Json.toJson(undertaking))

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }

      "throw an exception if the call to EIS fails to add default subsidy usage" in {
        givenCreateUndertakingReturns(Future.successful(undertakingReference))
        givenTimeProviderReturnsDate(date)
        givenUpdateSubsidy(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.create.url)
            .withJsonBody(Json.toJson(undertaking))

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }
    }

    "retrieve subsidies is called" should {

      "return a valid response for a successful request with no date range" in {

        givenRetrieveSubsidiesReturns(Future.successful(undertakingSubsidies))

        val app = configuredAppInstance

        running(app) {

          val request = fakeJsonPost(routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, None)))

          val result = route(app, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(undertakingSubsidies)
        }
      }

      "return a valid response for a successful request with a date range" in {

        givenRetrieveSubsidiesReturns(Future.successful(undertakingSubsidies))

        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, Some((date, date.plusDays(7))))))

          val result = route(app, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(undertakingSubsidies)
        }
      }

      "throw an exception if the call to EIS fails" in {
        givenRetrieveSubsidiesReturns(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, Some((date, date.plusDays(7))))))

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }

      "return a HTTP 400 if the request body is invalid" in {
        val app = configuredAppInstance

        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.retrieveSubsidies().url)
            .withBody("This is not valid JSON")

          status(route(app, request).value) mustBe BAD_REQUEST
        }
      }

    }

    "disable undertaking is called" should {

      "return OK for a successful request" in {
        val app = configuredAppInstance

        givenUpdateUndertaking(Future.successful(undertakingReference), EisAmendmentType.D)
        running(app) {
          val request = fakeJsonPost(routes.UndertakingController.disableUndertaking().url)
            .withJsonBody(Json.toJson(undertaking))
          val result = route(app, request).value

          status(result) mustBe OK
        }
      }
    }

    "get undertaking balance is called" should {

      "return a valid response for a successful request with no date range" in {

        givenGetUndertakingBalanceReturns(Future.successful(validUndertakingBalanceApiResponse))

        val app = configuredAppInstance

        running(app) {

          val request = FakeRequest(GET, routes.UndertakingController.getUndertakingBalance(eori).url)
            .withHeaders(CONTENT_TYPE -> JSON)
          val result = route(app, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(
            validUndertakingBalanceApiResponse.getUndertakingBalanceResponse
          )
        }
      }

      "return a not found response when anything other than a status OK is received" in {

        givenGetUndertakingBalanceReturns(Future.successful(undertakingBalanceApiErrorResponse))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.getUndertakingBalance(eori).url)
            .withHeaders(CONTENT_TYPE -> JSON)
          val result = route(app, request).value

          status(result) mustBe NOT_FOUND
        }
      }

      "throw an exception if the call to EIS fails" in {
        givenGetUndertakingBalanceReturns(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(GET, routes.UndertakingController.getUndertakingBalance(eori).url)
            .withHeaders(CONTENT_TYPE -> JSON)

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }

    }

  }

  private def fakeJsonPost(url: String) =
    FakeRequest(POST, url)
      .withHeaders(CONTENT_TYPE -> JSON)

  private def configuredAppInstance = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "microservice.metrics.graphite.enabled" -> false
    )
    .overrides(
      bind[EisConnector].to(mockEisConnector),
      bind[TimeProvider].to(mockTimeProvider),
      bind[Authenticator].to(new FakeAuthenticator)
    )
    .build()

  private def givenGetUndertakingBalanceReturns(res: Future[GetUndertakingBalanceApiResponse]): Unit =
    (mockEisConnector
      .getUndertakingBalance(_: GetUndertakingBalanceRequest)(_: HeaderCarrier, _: ExecutionContext))
      .expects(validUndertakingBalanceApiRequest, *, *)
      .returning(res)

  private def givenRetrieveSubsidiesReturns(res: Future[UndertakingSubsidies]): Unit =
    (mockEisConnector
      .retrieveSubsidies(_: UndertakingRef, _: Option[(LocalDate, LocalDate)])(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertakingReference, *, *, *)
      .returning(res)

  private def givenCreateUndertakingReturns(res: Future[UndertakingRef]): Unit =
    (mockEisConnector
      .createUndertaking(_: UndertakingCreate)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertakingCreate, *, *)
      .returning(res)

  private def givenUpdateSubsidy(res: Future[Unit]): Unit =
    (mockEisConnector
      .upsertSubsidyUsage(_: SubsidyUpdate)(_: HeaderCarrier, _: ExecutionContext))
      .expects(SubsidyUpdate(undertakingReference, NilSubmissionDate(date)), *, *)
      .returning(res)

  private def givenTimeProviderReturnsDate(fixedDate: LocalDate): Unit =
    (() => mockTimeProvider.today).expects().returning(fixedDate)

  private def givenRetrieveRetrieveUndertaking(res: Either[ConnectorError, UndertakingRetrieve]): Unit =
    (mockEisConnector
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori, *, *)
      .returning(Future.successful(res))

  private def givenUpdateUndertaking(res: Future[UndertakingRef], eisAmendmentType: EisAmendmentType): Unit =
    (mockEisConnector
      .updateUndertaking(_: UndertakingRetrieve, _: EisAmendmentType)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertaking, eisAmendmentType, *, *)
      .returning(res)

  private def givenDeleteMember(res: Future[Unit]): Unit =
    (mockEisConnector
      .deleteMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertakingReference, businessEntity, *, *)
      .returning(res)

  private def givenAddMember(res: Future[Unit]): Unit =
    (mockEisConnector
      .addMember(_: UndertakingRef, _: BusinessEntity, _: AmendmentType)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertakingReference, businessEntity, AmendmentType.amend, *, *)
      .returning(res)
}
