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

import cats.data.EitherT
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisAmendmentType, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.{GetUndertakingBalanceApiResponse, GetUndertakingBalanceRequest}
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class UndertakingController @Inject() (
  cc: ControllerComponents,
  authenticator: Authenticator,
  eisConnector: EisConnector,
  timeProvider: TimeProvider
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def retrieve(eori: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    EitherT(eisConnector.retrieveUndertaking(EORI(eori)))
      .map(u => Ok(Json.toJson(u)))
      .recover {
        case ConnectorError(NOT_FOUND, _) =>
          logger.error(s"Undertaking not found for EORI $eori")
          NotFound
        case ConnectorError(NOT_ACCEPTABLE, error: String) =>
          logger.error(s"Undertaking NOT_ACCEPTABLE for EORI $eori - $error")
          NotAcceptable
      }
      .getOrElse(InternalServerError)
  }

  def create: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingCreate] { undertakingCreate: UndertakingCreate =>
      for {
        ref <- eisConnector.createUndertaking(undertakingCreate)
        _ <- eisConnector.upsertSubsidyUsage(SubsidyUpdate(ref, NilSubmissionDate(timeProvider.today)))
      } yield {
        logger.info(s"successfully created undertaking ref $ref")
        Ok(Json.toJson(ref))
      }
    }
  }

  def updateUndertaking(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertakingRetrieve: UndertakingRetrieve =>
      eisConnector.updateUndertaking(undertakingRetrieve, EisAmendmentType.A).map { ref =>
        logger.info(s"successfully updateUndertaking undertaking ${undertakingRetrieve.loggableString}")
        Ok(Json.toJson(ref))
      }
    }
  }

  def disableUndertaking: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertaking: UndertakingRetrieve =>
      eisConnector.updateUndertaking(undertaking, EisAmendmentType.D).map { ref =>
        logger.info(s"successfully disabled undertaking ${undertaking.reference}")
        Ok(Json.toJson(ref))
      }
    }
  }

  def addMember(undertakingRef: String): Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
        for {
          amendmentType <- getAmendmentTypeForBusinessEntity(businessEntity)
          ref = UndertakingRef(undertakingRef)
          _ <- eisConnector.addMember(ref, businessEntity, amendmentType)
        } yield {
          logger.info(s"successfully addMember undertaking $undertakingRef BusinessEntity:$businessEntity")
          Ok(Json.toJson(ref))
        }

      }
  }

  private def getAmendmentTypeForBusinessEntity(
    be: BusinessEntity
  )(implicit r: Request[JsValue]): Future[types.AmendmentType.Value] =
    eisConnector.retrieveUndertaking(EORI(be.businessEntityIdentifier)) map {
      case Left(_) => AmendmentType.add
      case Right(_) => AmendmentType.amend
    }

  def deleteMember(undertakingRef: String): Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
        eisConnector.deleteMember(UndertakingRef(undertakingRef), businessEntity).map { _ =>
          logger.info(s"successfully deleteMember undertaking $undertakingRef BusinessEntity:$businessEntity")
          Ok(Json.toJson(UndertakingRef(undertakingRef)))
        }
      }
  }

  def updateSubsidy(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyUpdate] { update: SubsidyUpdate =>
      eisConnector.upsertSubsidyUsage(update).map { _ =>
        logger.info(s"successfully updateSubsidy SubsidyUpdate $update")
        Ok(Json.toJson(update.undertakingIdentifier))
      }
    }
  }

  def retrieveSubsidies(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyRetrieve] { subsidyRetrieve: SubsidyRetrieve =>
      eisConnector.retrieveSubsidies(subsidyRetrieve.undertakingIdentifier, subsidyRetrieve.inDateRange).map { e =>
        logger.info(s"successfully retrieveSubsidies SubsidyRetrieve $subsidyRetrieve")
        Ok(Json.toJson(e))
      }
    }
  }

  def getUndertakingBalance(eori: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    eisConnector.getUndertakingBalance(GetUndertakingBalanceRequest(eori = Some(EORI(eori)))).map {
      case Some(GetUndertakingBalanceApiResponse(Some(undertakingBalanceResponse), None)) =>
        Ok(Json.toJson(undertakingBalanceResponse))
      case _ =>
        logger.warn(s"undertaking with eori: $eori not found.")
        NotFound
    }
  }

}
