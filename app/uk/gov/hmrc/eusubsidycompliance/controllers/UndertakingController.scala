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

import cats.data.EitherT
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Auth
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisAmendmentType, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class UndertakingController @Inject() (
  cc: ControllerComponents,
  authenticator: Auth,
  eis: EisConnector,
  timeProvider: TimeProvider
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def retrieve(eori: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    EitherT(eis.retrieveUndertaking(EORI(eori)))
      .map(u => Ok(Json.toJson(u)))
      .recover {
        case ConnectorError(NOT_FOUND, _) => NotFound
        case ConnectorError(NOT_ACCEPTABLE, _) => NotAcceptable
      }
      .getOrElse(InternalServerError)
  }

  def create: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingCreate] { undertaking: UndertakingCreate =>
      for {
        ref <- eis.createUndertaking(undertaking)
        _ <- eis.upsertSubsidyUsage(SubsidyUpdate(ref, NilSubmissionDate(timeProvider.today)))
      } yield Ok(Json.toJson(ref))
    }
  }

  def updateUndertaking: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertaking: UndertakingRetrieve =>
      eis.updateUndertaking(undertaking, EisAmendmentType.A).map(ref => Ok(Json.toJson(ref)))
    }
  }

  def disableUndertaking: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertaking: UndertakingRetrieve =>
      eis.updateUndertaking(undertaking, EisAmendmentType.D).map(ref => Ok(Json.toJson(ref)))
    }
  }

  def addMember(undertakingRef: String): Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
        for {
          amendmentType <- getAmendmentTypeForBusinessEntity(businessEntity)
          ref = UndertakingRef(undertakingRef)
          _ <- eis.addMember(ref, businessEntity, amendmentType)
        } yield Ok(Json.toJson(ref))
      }
  }

  private def getAmendmentTypeForBusinessEntity(be: BusinessEntity)(implicit r: Request[JsValue]) =
    eis.retrieveUndertaking(EORI(be.businessEntityIdentifier)) map {
      case Left(_) => AmendmentType.add
      case Right(_) => AmendmentType.amend
    }

  def deleteMember(undertakingRef: String): Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
        eis.deleteMember(UndertakingRef(undertakingRef), businessEntity).map { _ =>
          Ok(Json.toJson(UndertakingRef(undertakingRef)))
        }
      }
  }

  def updateSubsidy(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyUpdate] { update: SubsidyUpdate =>
      eis.upsertSubsidyUsage(update).map { _ =>
        Ok(Json.toJson(update.undertakingIdentifier))
      }
    }
  }

  def retrieveSubsidies(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyRetrieve] { retrieve: SubsidyRetrieve =>
      eis.retrieveSubsidies(retrieve.undertakingIdentifier, retrieve.inDateRange).map { e =>
        Ok(Json.toJson(e))
      }
    }
  }

}
