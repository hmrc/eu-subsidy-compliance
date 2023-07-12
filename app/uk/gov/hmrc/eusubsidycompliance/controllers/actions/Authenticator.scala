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

package uk.gov.hmrc.eusubsidycompliance.controllers.actions

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Authenticator @Inject() (
  val authConnector: AuthConnector,
  val controllerComponents: ControllerComponents
) extends AuthorisedFunctions
    with Results
    with BaseController {

  type AuthAction[A] = Request[A] => String => Future[Result]

  private val EnrollmentKey = "HMRC-ESC-ORG"
  private val EnrolmentIdentifier = "EORINumber"

  def authorised(action: AuthAction[AnyContent])(implicit ec: ExecutionContext): Action[AnyContent] =
    Action.async { implicit request =>
      authCommon(action)
    }

  protected def authCommon[A](
    action: AuthAction[A]
  )(implicit request: Request[A], executionContext: ExecutionContext): Future[Result] =
    request.headers
      .get(AUTHORIZATION)
      .fold(Future.successful(Forbidden("Authorization header missing")))(_ => checkEnrolment(action))

  private def checkEnrolment[A](
    action: AuthAction[A]
  )(implicit request: Request[A], ec: ExecutionContext): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val retrievals: Retrieval[Enrolments] = Retrievals.allEnrolments

    authorised(Enrolment(EnrollmentKey))
      .retrieve(retrievals) {
        case enrolments: Enrolments =>
          enrolments
            .getEnrolment(EnrollmentKey)
            .flatMap(_.getIdentifier(EnrolmentIdentifier))
            .map(_.value)
            .fold(throw new IllegalStateException("EORI missing from enrolment")) { eori =>
              action(request)(EORI(eori))
            }
        case _ => Future.failed(throw InternalError())
      }
      .recover {
        case _: NoActiveSession => Unauthorized("No active session")
        case _: InsufficientEnrolments => Unauthorized("Insufficient Enrolments")
      }
  }

  def authorisedWithJson(json: BodyParser[JsValue])(
    action: AuthAction[JsValue]
  )(implicit executionContext: ExecutionContext): Action[JsValue] = Action.async(json) { implicit request =>
    authCommon(action)
  }
}
