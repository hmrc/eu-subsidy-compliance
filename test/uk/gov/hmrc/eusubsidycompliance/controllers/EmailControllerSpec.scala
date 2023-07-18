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

import org.mockito.{ArgumentMatchers, Mockito}
import play.api.http.{MimeTypes, Status}
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.models.{EmailCache, StartEmailVerificationRequest}
import uk.gov.hmrc.eusubsidycompliance.persistence.{EoriEmailRepository, EoriEmailRepositoryError, InitialEmailCache}
import uk.gov.hmrc.eusubsidycompliance.shared.PlayBaseSpec
import uk.gov.hmrc.eusubsidycompliance.test.FakeAuthenticator
import uk.gov.hmrc.eusubsidycompliance.util.UuidProvider

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class EmailControllerSpec
    extends PlayBaseSpec
    with org.mockito.MockitoSugar
    with org.mockito.scalatest.ResetMocksAfterEachTest {

  // Using this for the implicit timout etc.
  import play.api.test.Helpers._

  // Have to use Mockito as the inheritance in EoriEmailRepository blows up scalamock :(
  // Mocking has to be done around the fact that Mockito things NullPointer exceptions are friendly so loose
  // expectation and tight verification to get around the headaches it causes on failure.
  private lazy val eoriEmailRepositoryMock: EoriEmailRepository = mock[EoriEmailRepository]
  private lazy val eoriUuidProviderMock: UuidProvider = mock[UuidProvider]

  "EmailController" when {
    "startVerification is called" should {
      val eori = EORI("GB123443211231")
      val emailAddress = "test@test.com"
      val uuid = UUID.randomUUID
      val initialEmailCache =
        InitialEmailCache(eori = eori, verificationId = uuid.toString, email = emailAddress, verified = false)

      "process successfully" in {
        eoriUuidProviderMock.randomReturns(uuid)

        val successResponse = Right(
          EmailCache(
            eori = eori,
            email = emailAddress,
            verificationId = uuid.toString,
            verified = false,
            created = Instant.EPOCH,
            lastUpdated = Instant.EPOCH
          )
        )

        eoriEmailRepositoryMock.expectAddEmailInitialisation(returningErrorOrEmailCache = successResponse)

        val startEmailVerificationRequest = StartEmailVerificationRequest(eori = eori, emailAddress = emailAddress)

        val app = configuredAppInstance

        Helpers.running(app) {
          val startVerificationUrl = routes.EmailController.startVerification.url
          val startVerificationRequest =
            createPostRequest(startVerificationUrl, Json.toJson(startEmailVerificationRequest))

          FakeRequest(POST, startVerificationUrl).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)

          val result = route(app, startVerificationRequest).value
          Helpers.status(result) mustBe Status.CREATED

          eoriEmailRepositoryMock.verifyAddEmailInitialisation(initialEmailCache)
        }
      }

      "fail on error" in {
        eoriUuidProviderMock.randomReturns(uuid)
        val failure = Left(EoriEmailRepositoryError("I had a problem"))

        eoriEmailRepositoryMock.expectAddEmailInitialisation(returningErrorOrEmailCache = failure)

        val startEmailVerificationRequest = StartEmailVerificationRequest(eori = eori, emailAddress = emailAddress)

        val app = configuredAppInstance

        Helpers.running(app) {
          val startVerificationUrl = routes.EmailController.startVerification.url
          val startVerificationRequest =
            createPostRequest(startVerificationUrl, Json.toJson(startEmailVerificationRequest))

          FakeRequest(POST, startVerificationUrl).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)

          val result = route(app, startVerificationRequest).value
          Helpers.status(result) mustBe Status.INTERNAL_SERVER_ERROR

          eoriEmailRepositoryMock.verifyAddEmailInitialisation(initialEmailCache)
        }

      }
    }

    "approveEmailByEori" should {
      "successfully an EORI when found" in {}
    }

  }

  private implicit class EoriEmailRepositoryMock(eoriEmailRepository: EoriEmailRepository) {
    def expectAddEmailInitialisation(returningErrorOrEmailCache: Either[EoriEmailRepositoryError, EmailCache]): Unit =
      Mockito
        .when(eoriEmailRepository.addEmailInitialisation(ArgumentMatchers.any()))
        .thenReturn(Future.successful(returningErrorOrEmailCache))

    def verifyAddEmailInitialisation(initialEmailCache: InitialEmailCache): Unit =
      Mockito.verify(eoriEmailRepository).addEmailInitialisation(initialEmailCache)
  }

  private implicit class UuidProviderMock(uuidProvider: UuidProvider) {
    def randomReturns(uuid: UUID): Unit = {
      Mockito.when(uuidProvider.getRandom).thenReturn(uuid)
    }
  }

  private def createPostRequest(url: String, body: JsValue) = FakeRequest(POST, url)
    .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
    .withJsonBody(body)

  private def configuredAppInstance = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "microservice.metrics.graphite.enabled" -> false
    )
    .overrides(
      inject.bind[EoriEmailRepository].to(eoriEmailRepositoryMock),
      inject.bind[Authenticator].to(new FakeAuthenticator),
      inject.bind[UuidProvider].to(eoriUuidProviderMock)
    )
    .build()

}