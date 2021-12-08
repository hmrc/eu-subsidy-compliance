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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends AuthorisedFunctions with Results with BaseController {

  type AuthAction[A] = Request[A] => String => Future[Result]

  def authorised(action: AuthAction[AnyContent])(implicit ec: ExecutionContext): Action[AnyContent] =
    Action.async { implicit request =>
    authCommon(action)
  }

  def authorisedWithJson(json: BodyParser[JsValue])(action: AuthAction[JsValue])(
    implicit executionContext: ExecutionContext): Action[JsValue] = Action.async(json) { implicit request =>
    authCommon(action)
  }

  def authCommon[A](action: AuthAction[A])(
    implicit request: Request[A], executionContext: ExecutionContext): Future[Result]
}

@Singleton
class AuthImpl @Inject() (val authConnector: AuthConnector, val controllerComponents: ControllerComponents)
  extends Auth {

  val authProvider: AuthProviders = AuthProviders(GovernmentGateway)
  val retrievals: Retrieval[Enrolments] = Retrievals.allEnrolments

  def authCommon[A](action: AuthAction[A])(implicit request: Request[A], executionContext: ExecutionContext):
    Future[Result] = {
      request.headers.get("Authorization") match {
        case Some(_) =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
          authorised(Enrolment("HMRC-ESC-ORG")).retrieve(retrievals) {
            case enrolments =>
              enrolments.getEnrolment("HMRC-ESC-ORG")
                .map(x => x.getIdentifier("EORINumber"))
                .map(y => y.map(z => z.value)) match {
                case Some(Some(eori)) => action(request)(eori)
                case _ => ??? // TODO: What happens in this scenario?
              }
            case _ => Future.failed(throw InternalError())
          }.recoverWith {
            case _: NoActiveSession =>
              Future.successful(Unauthorized("No active session"))
            case _: InsufficientEnrolments => Future.successful(Unauthorized("Insufficient Enrolments"))
          }
        case _ => Future.successful(Forbidden("Authorization header missing"))
      }
  }
}