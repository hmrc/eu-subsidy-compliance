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

package uk.gov.hmrc.eusubsidycompliance.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.models.Undertaking
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.undertakingFormat
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class UndertakingController @Inject()(
  cc: ControllerComponents,
  eis: EisConnector
) extends BackendController(cc) { // TODO authentication

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieve(eori: String): Action[AnyContent] = Action.async { implicit request =>
    eis.retrieveUndertaking(
      EORI(eori)
    ).map { undertaking =>
      Ok(Json.toJson(undertaking))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val uF = Json.format[Undertaking]
    withJsonBody[Undertaking] { undertaking: Undertaking =>
      eis.createUndertaking(undertaking).map{ ref =>
        Ok(Json.toJson(ref))
      }
    }
  }
}
