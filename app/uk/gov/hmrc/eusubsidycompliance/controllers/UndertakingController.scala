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

import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Auth
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliance.models.{BusinessEntity, NilSubmissionDate, SubsidyRetrieve, SubsidyUpdate, Undertaking}
import uk.gov.hmrc.eusubsidycompliance.util.TimeProvider
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton()
class UndertakingController @Inject()(
  cc: ControllerComponents,
  authenticator: Auth,
  eis: EisConnector,
  timeProvider: TimeProvider
) extends BackendController(cc) {

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieve(eori: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    eis.retrieveUndertaking(
      EORI(eori)
    ).map { undertaking =>
      implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
      Ok(Json.toJson(undertaking))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = Json.format[Undertaking]
    withJsonBody[Undertaking] { undertaking: Undertaking =>
      for {
        ref <- eis.createUndertaking(undertaking)
        _ <- eis.updateSubsidy(SubsidyUpdate(ref, NilSubmissionDate(timeProvider.today)))
      } yield Ok(Json.toJson(ref))
    }
  }

  def updateUndertaking: Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = Json.format[Undertaking]
    withJsonBody[Undertaking] { undertaking: Undertaking =>
      eis.updateUndertaking(undertaking).map(ref => Ok(Json.toJson(ref)))
    }

  }

  def addMember(undertakingRef: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = Json.format[BusinessEntity]
    //TODO make sure the undertaking is correct
    withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
      val a = eis.retrieveUndertaking(
          EORI(businessEntity.businessEntityIdentifier)
        )
        .map (_ => AmendmentType.amend)
        .recover {
          case _ =>  AmendmentType.add
        }
      a.flatMap { amendType =>
        eis.addMember(UndertakingRef(undertakingRef), businessEntity, amendType).map { _ =>
          Ok(Json.toJson(UndertakingRef(undertakingRef)))
        }
      }
    }
  }

  def deleteMember(undertakingRef: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = Json.format[BusinessEntity]
    withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
      eis.deleteMember(UndertakingRef(undertakingRef), businessEntity).map{ _ =>
        Ok(Json.toJson(UndertakingRef(undertakingRef)))
      }
    }
  }


  def updateSubsidy(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = SubsidyUpdate.updateFormat
    withJsonBody[SubsidyUpdate] { update: SubsidyUpdate =>
      eis.updateSubsidy(update).map{ _ =>
        Ok(Json.toJson(update.undertakingIdentifier)) // TODO check error handling
      }
    }
  }

  def retrieveSubsidies(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyRetrieve] { retrieve: SubsidyRetrieve =>
      eis.retrieveSubsidies(retrieve.undertakingIdentifier, retrieve.inDateRange).map{ e =>
        Ok(Json.toJson(e)) // TODO check error handling
      }
    }
  }

}
