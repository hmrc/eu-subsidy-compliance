/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.services.ExchangeRateService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json

import java.time.LocalDate

class ExchangeRateController @Inject() (
  cc: ControllerComponents,
  authenticator: Authenticator,
  exchangeRateService: ExchangeRateService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {
  def retrieveExchangeRate(date: String): Action[AnyContent] = authenticator.authorised { implicit request => _ =>
    val parsedDate = LocalDate.parse(date)
    exchangeRateService.retrieveCachedMonthlyExchangeRate(parsedDate).map {
      case Some(retrievedExchangeRate) =>
        Ok(Json.toJson(retrievedExchangeRate))
      case x =>
        logger.warn(s"Exchange rate not found for date: $date - instead found $x")
        NotFound
    }
  }
}
