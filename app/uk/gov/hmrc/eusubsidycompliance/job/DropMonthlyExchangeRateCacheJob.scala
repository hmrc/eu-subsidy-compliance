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

package uk.gov.hmrc.eusubsidycompliance.job

import cats.implicits.toFunctorOps
import play.api.Logging
import uk.gov.hmrc.eusubsidycompliance.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@Singleton
class DropMonthlyExchangeRateCacheJob @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig)(implicit
  executionContext: ExecutionContext
) extends Logging {
  if (appConfig.dropMonthlyExchangeRateCacheJobEnabled) {
    logger.warn("=== DropMonthlyExchangeRateCacheJob starting ===")
    mongoComponent.database.getCollection("exchangeRateMonthlyCache").drop().toFuture().void.onComplete {
      case Success(_) => logger.warn("=== DropMonthlyExchangeRateCacheJob successfully completed ===")
      case Failure(t) => logger.error("=== DropMonthlyExchangeRateCacheJob failed ===", t)
    }
  } else {
    logger.warn("=== DropMonthlyExchangeRateCacheJob is disabled. Consider removing it if not needed anymore ===")
  }
}
