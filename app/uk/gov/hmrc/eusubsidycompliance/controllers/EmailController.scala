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

import play.api.{Configuration, Logging}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliance.connectors.EmailConnector
import uk.gov.hmrc.eusubsidycompliance.models.{EmailParameters, EmailRequest, OriginalEmailRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  emailConnector: EmailConnector,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {
  private val undertakingAdminDeadlineReminder = configuration.get[String]("email.undertakingAdminDeadlineReminder")
  private val undertakingAdminDeadlineExpired = configuration.get[String]("email.undertakingAdminDeadlineExpired")
  val permission = Predicate.Permission(
    Resource(ResourceType("eu-subsidy-compliance"), ResourceLocation("email-notification")),
    IAAction("ADMIN")
  )
  def sendNudgeEmail(): Action[JsValue] = auth.authorizedAction(permission).async(parse.json) { implicit request =>
    withJsonBody[OriginalEmailRequest] {
      case deadlineReminderRequest @ OriginalEmailRequest(_, _, "1", _) =>
        sendEmail(undertakingAdminDeadlineReminder, deadlineReminderRequest)
      case deadlineExpiredRequest @ OriginalEmailRequest(_, _, "2", _) =>
        sendEmail(undertakingAdminDeadlineExpired, deadlineExpiredRequest)
      case _ => Future.successful(InternalServerError.apply("Unsupported message type"))
    }
  }

  private def sendEmail(messageType: String, originalRequest: OriginalEmailRequest)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    emailConnector
      //TODO Replace hardcoded date with deadline received from updated request
      .sendEmail(EmailRequest(List(originalRequest.emailAddress), messageType, EmailParameters("10 December 2023")))
      .map(response =>
        if (response.status == ACCEPTED) NoContent
        else {
          logger
            .error(s"Did not receive accepted from email service - instead got ${response.status} and ${response.body}")
          InternalServerError.apply(
            "The request failed due to unavailability of downstream services or an unexpected error."
          )
        }
      )

}
