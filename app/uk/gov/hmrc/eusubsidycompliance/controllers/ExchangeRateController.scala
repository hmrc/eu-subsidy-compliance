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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Auth
import uk.gov.hmrc.eusubsidycompliance.services.ExchangeRateService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ExchangeRateController @Inject() (
  cc: ControllerComponents,
  authenticator: Auth,
  service: ExchangeRateService
)(implicit ec: ExecutionContext) extends BackendController(cc) {

  def getExchangeRate(dateString: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    Try(LocalDate.parse(dateString))
      .fold(
        _ => Future.successful(BadRequest("Invalid date string")),
        date => service.getExchangeRate(date).map(r => Ok(Json.toJson(r)))
      )
  }

}
