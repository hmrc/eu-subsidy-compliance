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

import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.models.{ApproveEmailAsVerifiedByEoriRequest, ApproveEmailByVerificationIdRequest, ConnectorError, EmailCache, StartEmailVerificationRequest, VerifiedEmailResponse}
import uk.gov.hmrc.eusubsidycompliance.persistence.{EoriEmailRepository, EoriEmailRepositoryError, InitialEmailCache, WriteSuccess}
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

      val eventualErrorOrEmailCache = eoriEmailRepository.setEmailInitialisation(initialEmailCache)

      eventualErrorOrEmailCache.map {
        case Left(error) =>
          val errorMessage = s"Failed starting verification for email EORI ${startEmailVerificationRequest.eori}"
          logger.error(errorMessage, error)
          InternalServerError(JsString(errorMessage))
        case Right(_) =>
          val successMessage =
            s"successfully started verification for email EORI ${startEmailVerificationRequest.eori}"
          logger.info(
            successMessage
          )
          Created(Json.toJson(successMessage))
      }
    }
  }

  def approveEmailByEori: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[ApproveEmailAsVerifiedByEoriRequest] {
      approveEmailAsVerifiedByEoriRequest: ApproveEmailAsVerifiedByEoriRequest =>
        // eoriEmailRepository.update()
        eoriEmailRepository.markEmailAsVerifiedByEori(approveEmailAsVerifiedByEoriRequest.eoriToVerify).map {
          case Left(error: EoriEmailRepositoryError) =>
            val errorMessage = s"Failed verifying email eori ${approveEmailAsVerifiedByEoriRequest.eoriToVerify}"
            logger.error(errorMessage, error)
            InternalServerError(
              Json.toJson(
                s"There was an error approving the email for EORI ${approveEmailAsVerifiedByEoriRequest.eoriToVerify}"
              )
            )
          case Right(maybeWriteSuccess) =>
            maybeWriteSuccess
              .map { _: WriteSuccess.type =>
                val message = s"EORI ${approveEmailAsVerifiedByEoriRequest.eoriToVerify} has been marked as verified"
                logger.info(message)
                Ok(JsString(message))
              }
              .getOrElse {
                val message =
                  s"EORI ${approveEmailAsVerifiedByEoriRequest.eoriToVerify} could not be found for verification"
                logger.warn(message)
                NotFound(JsString(message))
              }
        }
    }
  }

  def approveEmailByVerificationId: Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[ApproveEmailByVerificationIdRequest] {
        approveEmailByVerificationIdRequest: ApproveEmailByVerificationIdRequest =>
          val eori = approveEmailByVerificationIdRequest.eori
          val verificationId = approveEmailByVerificationIdRequest.verificationId
          eoriEmailRepository
            .markEmailAsVerifiedByVerificationId(eori, verificationId)
            .map {
              case Left(error) => ???
              case Right(maybeWriteSuccess) =>
                maybeWriteSuccess
                  .map { (__ : WriteSuccess.type) =>
                    val message = s"EORI $eori with verification id $verificationId has been marked as verified"
                    logger.info(message)
                    Ok(JsString(message))
                  }
                  .getOrElse(???)

            }
      }
  }

  def getEmailVerification(eori: EORI): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    ///eoriEmailRepository.getEmailVerification(eori), NotFound)
  ???

  }

}
