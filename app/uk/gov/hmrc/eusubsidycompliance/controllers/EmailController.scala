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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.models.{ApproveEmailAsVerifiedByEoriRequest, ApproveEmailByVerificationIdRequest, ConnectorError, EmailCache, StartEmailVerificationRequest, VerifiedEmailResponse}
import uk.gov.hmrc.eusubsidycompliance.persistence.{EoriEmailRepository, InitialEmailCache}
import uk.gov.hmrc.eusubsidycompliance.util.UuidProvider
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailController @Inject() (
  cc: ControllerComponents,
  authenticator: Authenticator,
  eoriEmailRepository: EoriEmailRepository,
  uuidProvider: UuidProvider
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with TracedLogging {

  def startVerification: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[StartEmailVerificationRequest] { startEmailVerificationRequest: StartEmailVerificationRequest =>
      val initialEmailCache = InitialEmailCache.apply(
        eori = startEmailVerificationRequest.eori,
        verificationId = uuidProvider.getRandom.toString,
        email = startEmailVerificationRequest.emailAddress,
        verified = false
      )

      logger.info(s"startVerification for email eori ${startEmailVerificationRequest.eori}")

      val eventualErrorOrEmailCache = eoriEmailRepository.addEmailInitialisation(initialEmailCache)

      eventualErrorOrEmailCache.map {
        case Left(error) =>
          logger.error(s"Failed adding startVerification for email eori ${startEmailVerificationRequest.eori}", error)
          InternalServerError
        case Right(emailCache) =>
          logger.info(
            s"successfully added startVerification for email eori ${startEmailVerificationRequest.eori}"
          )
          Created(Json.toJson(VerifiedEmailResponse.fromEmailCache(emailCache)))
      }
    }
  }

  private def remapGetEmailVerificationResponse(
    eventualMaybeEmailCache: Future[Option[EmailCache]],
    nonMapping: => Status
  ): Future[Result] = {
    eventualMaybeEmailCache.map { maybeEmailCache: Option[EmailCache] =>
      maybeEmailCache match {
        case Some(value: EmailCache) =>
          Ok(Json.toJson(VerifiedEmailResponse.fromEmailCache(value)))

        case None =>
          nonMapping
      }
    }
  }

  def approveEmailByEori: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[ApproveEmailAsVerifiedByEoriRequest] {
      approveEmailAsVerifiedByEoriRequest: ApproveEmailAsVerifiedByEoriRequest =>
        // eoriEmailRepository.update()
        ???
    }
  }

  def approveEmailByVerificationId: Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[ApproveEmailByVerificationIdRequest] {
        approveEmailByVerificationIdRequest: ApproveEmailByVerificationIdRequest =>
          ???
      }
  }

  def getEmailVerification(eori: EORI): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    remapGetEmailVerificationResponse(eoriEmailRepository.get(eori), NotFound)

  }

}
