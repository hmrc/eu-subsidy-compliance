/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{await, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatorSpecs extends AnyWordSpec with Matchers with AuthTestSupport
  with DefaultAwaitTimeout with EitherValues  {

  private val mcc           = stubMessagesControllerComponents()
  private val request       = FakeRequest()
  private val authenticator = new AuthImpl(mockAuthConnector, mcc)
  implicit val ec = ExecutionContext.global

  def result = authenticator.authorised { implicit request => _ =>
    Future.successful(Ok("Hello world"))
  }

  def newEnrolment(key: String, identifierName: String, identifierValue: String): Enrolment =
    Enrolment(key).withIdentifier(identifierName, identifierValue)

  "Authetication" should {
    "should return Forbidden when theres no Authorization header" in {
      status(result(request)) shouldBe Status.FORBIDDEN
    }

    "return 200 Ok" in {
      val newRequest = request.withHeaders(("Authorization", "XXXX"))
      withAuthorizedUser(Enrolments(Set(newEnrolment("HMRC-ESC-ORG", "EORINumber", "GB123123123123"))))
      status(result(newRequest)) shouldBe Status.OK
    }

    "throw illegal state error exception" in {
      val newRequest = request.withHeaders(("Authorization", "XXXX"))
      withAuthorizedUser()

      assertThrows[IllegalStateException](
        await(result(newRequest))
      )
    }

    "throw illegal state exception when identifier missing" in {
      val newRequest = request.withHeaders(("Authorization", "XXXX"))
      withAuthorizedUser(Enrolments(Set(newEnrolment("HMRC-ESC-ORG", "XXX", "XXX"))))

      assertThrows[IllegalStateException](
        await(result(newRequest))
      )
    }

    "return 401 UNAUTHORIZED when there is insufficient enrolments" in {
      val newRequest = request.withHeaders(("Authorization", "XXXX"))
      withUnauthorizedUser(InsufficientEnrolments("User not authorised"))
      status(result(newRequest)) shouldBe Status.UNAUTHORIZED
    }

    "return 401 UNAUTHORIZED when there is no session record" in {
      val newRequest = request.withHeaders(("Authorization", "XXXX"))
      withUnauthorizedUser(SessionRecordNotFound("User not authorised"))
      status(result(newRequest)) shouldBe Status.UNAUTHORIZED
    }
  }
}