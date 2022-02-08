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
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Auth
import uk.gov.hmrc.eusubsidycompliance.models.SubsidyRetrieve
import uk.gov.hmrc.eusubsidycompliance.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures.{eori, undertakingReference, undertakingSubsidies}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class UndertakingControllerSpec extends PlaySpec with MockFactory with Results {

  private class FakeAuth extends Auth {
    override def authCommon[A](
      action: AuthAction[A]
    )(implicit request: Request[A], executionContext: ExecutionContext): Future[Result] = action(request)(eori)
    override def authConnector: AuthConnector = ???
    override protected def controllerComponents: ControllerComponents = ???
  }

  "UnderTakingController" when {

    "retrieve subsidies is called" should {

      "return a valid response for a successful request" in {

        val mockEisConnector = mock[EisConnector]

        (mockEisConnector.retrieveSubsidies(_: UndertakingRef, _: Option[(LocalDate, LocalDate)])(_: HeaderCarrier, _: ExecutionContext))
          .expects(undertakingReference, *, *, *)
          .returning(Future.successful(undertakingSubsidies))

        val app = new GuiceApplicationBuilder()
          .configure(
            "metrics.jvm" -> false,
            "microservice.metrics.graphite.enabled" -> false,
          )
          .overrides(
            bind[EisConnector].to(mockEisConnector),
            bind[Auth].to(new FakeAuth)
          )
          .build()

        running(app) {

          val request = FakeRequest(POST, routes.UndertakingController.retrieveSubsidies().url)
            .withJsonBody(Json.toJson( SubsidyRetrieve(undertakingReference, None)))
            .withHeaders("Content-type" -> "application/json")

          val result = route(app, request).value
          status(result) mustBe 200

        }
      }

    }

  }

}
