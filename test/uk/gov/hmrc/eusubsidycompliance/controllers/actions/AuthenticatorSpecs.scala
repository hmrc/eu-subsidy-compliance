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

package uk.gov.hmrc.eusubsidycompliance.controllers.actions

import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class AuthenticatorSpecs extends AnyWordSpec with Matchers with AuthTestSupport
  with DefaultAwaitTimeout with EitherValues with ScalaFutures {

  private val mcc = stubMessagesControllerComponents()
  private val request = FakeRequest()
  private val authenticator = new AuthImpl(mockAuthConnector, mcc)
  private val requestWithAuthHeader = request.withHeaders(("Authorization", "XXXX"))

  // Simple case class to validate request body deserialization.
  private case class Foo(bar: String)
  private implicit val fooFormat = Json.format[Foo]

  private val requestWithAuthHeaderAndJsonBody =
    requestWithAuthHeader.withJsonBody(Json.toJson(Foo("Bar")))
      .withHeaders("Content-type" -> "application/json")
      .withMethod(POST)

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private val actionResponse = "Hello world"

  private def handleRequestWithNoBody = authenticator.authorised { _ => _ => Future.successful(Ok(actionResponse)) }

  private def handleRequestWithJsonBody = authenticator.
    authorisedWithJson(mcc.parsers.json) { _ => _ => Future.successful(Ok(actionResponse)) }

  private def newEnrolment(identifierName: String, identifierValue: String) =
    Enrolment("HMRC-ESC-ORG").withIdentifier(identifierName, identifierValue)

  "Authentication" should {

    "return Forbidden when there is no Authorization header" in {
      status(handleRequestWithNoBody(request)) shouldBe Status.FORBIDDEN
    }

    "return 200 Ok for a valid request without a JSON body" in {
      withAuthorizedUser(Enrolments(Set(newEnrolment("EORINumber", "GB123123123123"))))
      status(handleRequestWithNoBody(requestWithAuthHeader)) shouldBe Status.OK
    }

    "return 200 Ok for a valid request with a JSON body" in {
      withAuthorizedUser(Enrolments(Set(newEnrolment("EORINumber", "GB123123123123"))))

      val app = new GuiceApplicationBuilder()
        .configure(
          "metrics.jvm" -> false,
          "microservice.metrics.graphite.enabled" -> false,
        )
        .overrides(
          bind[AuthConnector].to(mockAuthConnector))
        .build()

      running(app) {
        import app.materializer
        val result = call(handleRequestWithJsonBody, req = requestWithAuthHeaderAndJsonBody)
        status(result) shouldBe 200
        contentAsString(result) shouldBe actionResponse
      }
    }

    "throw illegal state error exception " in {
      withAuthorizedUser()
      assertThrows[IllegalStateException](
        await(handleRequestWithNoBody(requestWithAuthHeader))
      )
    }

    "throw illegal state exception when identifier missing" in {
      withAuthorizedUser(Enrolments(Set(newEnrolment("XXX", "XXX"))))
      assertThrows[IllegalStateException](
        await(handleRequestWithNoBody(requestWithAuthHeader))
      )
    }

    "return 401 UNAUTHORIZED when there are insufficient enrolments" in {
      withUnauthorizedUser(InsufficientEnrolments("User not authorised"))
      status(handleRequestWithNoBody(requestWithAuthHeader)) shouldBe Status.UNAUTHORIZED
    }

    "return 401 UNAUTHORIZED when there is no session record" in {
      withUnauthorizedUser(SessionRecordNotFound("User not authorised"))
      status(handleRequestWithNoBody(requestWithAuthHeader)) shouldBe Status.UNAUTHORIZED
    }

  }
}