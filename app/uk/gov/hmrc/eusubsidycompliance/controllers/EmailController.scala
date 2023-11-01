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

import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliance.connectors.EmailConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliance.models.EmailRequest
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailController @Inject() (
  cc: ControllerComponents,
  emailConnector: EmailConnector,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val undertakingAdminDeadlineReminder = configuration.get[String]("email.undertakingAdminDeadlineReminder")
  private val undertakingAdminDeadlineExpired = configuration.get[String]("email.undertakingAdminDeadlineExpired")
  def sendNudgeEmail(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[EmailRequest] {
      case deadlineReminderRequest @ EmailRequest(_, _, "1", _) =>
        sendEmail(undertakingAdminDeadlineReminder, deadlineReminderRequest)
      case deadlineExpiredRequest @ EmailRequest(_, _, "2", _) =>
        sendEmail(undertakingAdminDeadlineExpired, deadlineExpiredRequest)
      case _ => Future.successful(InternalServerError.apply("Unsupported message type"))
    }
  }

  private def sendEmail(messageType: String, originalRequest: EmailRequest)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    emailConnector
      .sendEmail(originalRequest.copy(messageType = messageType))
      .map(response =>
        if (response.status == OK) NoContent
        else
          InternalServerError.apply(
            "The request failed due to unavailability of downstream services or an unexpected error."
          )
      )

}
