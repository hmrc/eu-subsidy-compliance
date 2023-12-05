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

import org.mockito.ArgumentMatchers.{any, eq => param}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.eusubsidycompliance.connectors.EmailConnector
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.ContentTypes.JSON
import play.api.inject
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.Status.{NO_CONTENT, OK}
import play.test.Helpers.POST
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, EmailRequest}
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures.eori
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import org.mockito.ArgumentMatchers
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.eusubsidycompliance.models.types.{EORI, EmailAddress, UndertakingRef}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class EmailControllerSpec extends AnyWordSpec with OptionValues with Matchers with MockitoSugar with ScalaFutures {

  private val mockEmailConnector = mock[EmailConnector]
  val undertakingAdminDeadlineReminder = "undertaking_admin_deadline_reminder"
  val undertakingAdminDeadlineExpired = "undertaking_admin_deadline_expired"
  private val authStubBehaviour: StubBehaviour = mock[StubBehaviour]
  when(authStubBehaviour.stubAuth(any, ArgumentMatchers.eq(Retrieval.EmptyRetrieval)))
    .thenReturn(Future.unit)
  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val cc: ControllerComponents = stubControllerComponents()
  private def configuredAppInstance = new GuiceApplicationBuilder()
    .overrides(
      bind[EmailConnector].to(mockEmailConnector),
      inject.bind[BackendAuthComponents].toInstance(BackendAuthComponentsStub(authStubBehaviour))
    )
    .build()
  val validDeadlineReminderEmailRequest: EmailRequest =
    EmailRequest(UndertakingRef("ABC12345"), EORI("GB000000000012"), "1", EmailAddress("jdoe@example.com"))
  val validDeadlineExpiredEmailRequest: EmailRequest =
    EmailRequest(UndertakingRef("ABC12345"), EORI("GB000000000012"), "2", EmailAddress("jdoe@example.com"))
  val invalidEmailRequest: EmailRequest =
    EmailRequest(UndertakingRef("ABC12345"), EORI("GB000000000012"), "3", EmailAddress("jdoe@example.com"))
  val invalidJson: BusinessEntity = BusinessEntity(eori, leadEORI = false)

  "Email Controller" when {
    "handling request to send nudge email" when {
      "the request has come from an authorised source" should {
        "return an ACCEPTED status when an appropriate deadline reminder request is sent" in {
          val app = configuredAppInstance
          running(app) {
            when(
              mockEmailConnector.sendEmail(
                param(validDeadlineReminderEmailRequest.copy(messageType = undertakingAdminDeadlineReminder))
              )(
                any()
              )
            ).thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))
            val fakeRequest = FakeRequest(
              POST,
              routes.EmailController
                .sendNudgeEmail()
                .url
            )
              .withJsonBody(Json.toJson(validDeadlineReminderEmailRequest))
              .withHeaders("Authorization" -> "token", CONTENT_TYPE -> JSON)
            route(app, fakeRequest).value.futureValue.header.status shouldBe NO_CONTENT
          }
        }
        "return an ACCEPTED status when an appropriate deadline expired request is sent" in {
          val app = configuredAppInstance
          running(app) {
            when(
              mockEmailConnector.sendEmail(
                param(validDeadlineExpiredEmailRequest.copy(messageType = undertakingAdminDeadlineExpired))
              )(
                any()
              )
            ).thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))
            val fakeRequest = FakeRequest(
              POST,
              routes.EmailController
                .sendNudgeEmail()
                .url
            )
              .withJsonBody(Json.toJson(validDeadlineExpiredEmailRequest))
              .withHeaders("Authorization" -> "token", CONTENT_TYPE -> JSON)

            route(app, fakeRequest).value.futureValue.header.status shouldBe NO_CONTENT
          }
        }
        "return an InternalServerError when provided unsupported message type" in {
          val app = configuredAppInstance
          running(app) {
            val fakeRequest = FakeRequest(
              POST,
              routes.EmailController
                .sendNudgeEmail()
                .url
            )
              .withJsonBody(Json.toJson(invalidEmailRequest))
              .withHeaders("Authorization" -> "token", CONTENT_TYPE -> JSON)

            route(app, fakeRequest).value.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
        "return a BadRequest when providing invalid json" in {
          val app = configuredAppInstance
          running(app) {
            val fakeRequest = FakeRequest(
              POST,
              routes.EmailController
                .sendNudgeEmail()
                .url
            )
              .withJsonBody(Json.toJson(invalidJson))
              .withHeaders("Authorization" -> "token", CONTENT_TYPE -> JSON)

            route(app, fakeRequest).value.futureValue.header.status shouldBe BAD_REQUEST
          }
        }
        "return an InternalServerError if the connector replies with a 404" in {
          val app = configuredAppInstance
          running(app) {
            when(
              mockEmailConnector.sendEmail(
                param(validDeadlineExpiredEmailRequest.copy(messageType = undertakingAdminDeadlineExpired))
              )(
                any()
              )
            ).thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
            val fakeRequest = FakeRequest(
              POST,
              routes.EmailController
                .sendNudgeEmail()
                .url
            )
              .withJsonBody(Json.toJson(validDeadlineExpiredEmailRequest))
              .withHeaders("Authorization" -> "token", CONTENT_TYPE -> JSON)

            route(app, fakeRequest).value.futureValue.header.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}
