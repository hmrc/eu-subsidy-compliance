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

package uk.gov.hmrc.eusubsidycompliance.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Format, Json}
import play.api.mvc.{ControllerComponents, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Auth
import uk.gov.hmrc.eusubsidycompliance.models.types.AmendmentType.AmendmentType
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures._
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class UndertakingControllerSpec extends PlaySpec with MockFactory with ScalaFutures with IntegrationPatience {

  // FakeAuthenticator that allows every request.
  private class FakeAuth extends Auth {
    override def authCommon[A](
      action: AuthAction[A]
    )(implicit request: Request[A], executionContext: ExecutionContext): Future[Result] = action(request)(eori)
    override protected def controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
    // This isn't used in this implementation so can be left as unimplemented.
    override def authConnector: AuthConnector = ???
  }

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

          status(result) mustBe Status.OK
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
      implicit val format: Format[Undertaking] = Json.format[Undertaking]

      "Happy path" in {
        val app = configuredAppInstance

        givenUpdateUndertaking(Future.successful(undertakingReference))
        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.updateUndertaking().url)
            .withJsonBody(Json.toJson(undertaking))
            .withHeaders("Content-type" -> "application/json")
          val result = route(app, request).value

          status(result) mustBe Status.OK
        }
      }
    }

    "deleteMember is called" should {

      "Happy path" in {
        val app = configuredAppInstance

        givenDeleteMember(Future.successful((): Unit))
        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.deleteMember(undertakingReference).url)
            .withJsonBody(Json.toJson(businessEntity))
            .withHeaders("Content-type" -> "application/json")
          val result = route(app, request).value

          status(result) mustBe Status.OK
        }
      }
    }

    "addMember is called" should {

      "Happy path" in {
        val app = configuredAppInstance

        givenRetrieveRetrieveUndertaking(Right(undertaking))
        givenAddMember(Future.successful((): Unit))

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.addMember(undertakingReference).url)
            .withJsonBody(Json.toJson(businessEntity))
            .withHeaders("Content-type" -> "application/json")
          val result = route(app, request).value

          status(result) mustBe Status.OK
        }
      }
    }

    "updateSubsidy is called" should {

      "Happy path" in {
        val app = configuredAppInstance

        givenUpdateSubsidy(Future.successful((): Unit))

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.updateSubsidy().url)
            .withJsonBody(Json.toJson(SubsidyUpdate(undertakingReference, NilSubmissionDate(date))))
            .withHeaders("Content-type" -> "application/json")
          val result = route(app, request).value

          status(result) mustBe Status.OK
        }
      }
    }

    "create undertaking is called" should {
      implicit val format: Format[Undertaking] = Json.format[Undertaking]

      "return a valid response for a successful to create undertaking" in {

        givenCreateUndertakingReturns(Future.successful(undertakingReference))
        givenUpdateSubsidy(Future.successful((): Unit))
        returningFixedDate(date)

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.create().url)
            .withJsonBody(Json.toJson(undertaking))
            .withHeaders("Content-type" -> "application/json")

          val result = route(app, request).value

          status(result) mustBe Status.OK
          contentAsJson(result) mustBe Json.toJson(undertakingReference)
        }
      }

      "throw an exception if the call to EIS to create undertaking fails" in {
        givenCreateUndertakingReturns(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.create().url)
            .withJsonBody(Json.toJson(undertaking))
            .withHeaders("Content-type" -> "application/json")

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }

      "throw an exception if the call to EIS fails to add default subsidy usage" in {
        givenCreateUndertakingReturns(Future.successful(undertakingReference))
        returningFixedDate(date)
        givenUpdateSubsidy(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.create().url)
            .withJsonBody(Json.toJson(undertaking))
            .withHeaders("Content-type" -> "application/json")

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }
    }

    "retrieve subsidies is called" should {

      "return a valid response for a successful request with no date range" in {

        givenRetrieveSubsidiesReturns(Future.successful(undertakingSubsidies))

        val app = configuredAppInstance

        running(app) {

          val request = FakeRequest(POST, routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, None)))
            .withHeaders("Content-type" -> "application/json")

          val result = route(app, request).value

          status(result) mustBe Status.OK
          contentAsJson(result) mustBe Json.toJson(undertakingSubsidies)
        }
      }

      "return a valid response for a successful request with a date range" in {

        givenRetrieveSubsidiesReturns(Future.successful(undertakingSubsidies))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, Some((date, date.plusDays(7))))))
            .withHeaders("Content-type" -> "application/json")

          val result = route(app, request).value

          status(result) mustBe Status.OK
          contentAsJson(result) mustBe Json.toJson(undertakingSubsidies)
        }
      }

      "throw an exception if the call to EIS fails" in {
        givenRetrieveSubsidiesReturns(Future.failed(new RuntimeException("Something failed")))

        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson(SubsidyRetrieve(undertakingReference, Some((date, date.plusDays(7))))))
            .withHeaders("Content-type" -> "application/json")

          route(app, request).value.failed.futureValue mustBe a[RuntimeException]
        }
      }

      "return a HTTP 400 if the request body is invalid" in {
        val app = configuredAppInstance

        running(app) {
          val request = FakeRequest(POST, routes.UndertakingController.retrieveSubsidies().url)
            .withBody("This is not valid JSON")
            .withHeaders("Content-type" -> "application/json")

          status(route(app, request).value) mustBe Status.BAD_REQUEST
        }
      }

    }

  }

  private def configuredAppInstance = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "microservice.metrics.graphite.enabled" -> false
    )
    .overrides(
      bind[EisConnector].to(mockEisConnector),
      bind[TimeProvider].to(mockTimeProvider),
      bind[Auth].to(new FakeAuth)
    )
    .build()

  private def givenRetrieveSubsidiesReturns(res: Future[UndertakingSubsidies]): Unit =
    (mockEisConnector
      .retrieveSubsidies(_: UndertakingRef, _: Option[(LocalDate, LocalDate)])(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertakingReference, *, *, *)
      .returning(res)

  private def givenCreateUndertakingReturns(res: Future[UndertakingRef]): Unit =
    (mockEisConnector
      .createUndertaking(_: Undertaking)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertaking, *, *)
      .returning(res)

  private def givenUpdateSubsidy(res: Future[Unit]): Unit =
    (mockEisConnector
      .upsertSubsidyUsage(_: SubsidyUpdate)(_: HeaderCarrier, _: ExecutionContext))
      .expects(SubsidyUpdate(undertakingReference, NilSubmissionDate(date)), *, *)
      .returning(res)

  private def returningFixedDate(fixedDate: LocalDate): Unit =
    (mockTimeProvider.today _).expects().returning(fixedDate)

  private def givenRetrieveRetrieveUndertaking(res: Either[ConnectorError, Undertaking]): Unit =
    (mockEisConnector
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori, *, *)
      .returning(Future.successful(res))

  private def givenUpdateUndertaking(res: Future[UndertakingRef]): Unit =
    (mockEisConnector
      .updateUndertaking(_: Undertaking)(_: HeaderCarrier, _: ExecutionContext))
      .expects(undertaking, *, *)
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
