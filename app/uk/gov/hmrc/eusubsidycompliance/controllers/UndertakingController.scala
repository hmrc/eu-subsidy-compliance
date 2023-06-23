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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.eusubsidycompliance.connectors.EisConnector
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliance.models.{types, _}
import uk.gov.hmrc.eusubsidycompliance.models.types.{AmendmentType, EORI, EisAmendmentType, UndertakingRef}
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
    with TracedLogging {

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
    withJsonBody[UndertakingCreate] { undertaking: UndertakingCreate =>
      val eventualResult = for {
        ref <- eisConnector.createUndertaking(undertaking)
        _ <- eisConnector.upsertSubsidyUsage(SubsidyUpdate(ref, NilSubmissionDate(timeProvider.today)))
      } yield Ok(Json.toJson(ref))

      eventualResult.foreach { _ =>
        logger.info(s"successfully created undertaking $undertaking")
      }
      eventualResult.failed.foreach { e =>
        logger.error(s"failed created undertaking $undertaking", e)
      }

      eventualResult
    }
  }

  def updateUndertaking(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertakingRetrieve: UndertakingRetrieve =>
      val eventualResult =
        eisConnector.updateUndertaking(undertakingRetrieve, EisAmendmentType.A).map(ref => Ok(Json.toJson(ref)))

      eventualResult.foreach { _ =>
        logger.info(s"successfully updateUndertaking undertaking $undertakingRetrieve")
      }
      eventualResult.failed.foreach { e =>
        logger.error(s"failed updateUndertaking undertaking $undertakingRetrieve", e)
      }

      eventualResult
    }
  }

  def disableUndertaking: Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[UndertakingRetrieve] { undertaking: UndertakingRetrieve =>
      eisConnector.updateUndertaking(undertaking, EisAmendmentType.D).map(ref => Ok(Json.toJson(ref)))
    }
  }

  def addMember(undertakingRef: String): Action[JsValue] = authenticator.authorisedWithJson(parse.json) {
    implicit request => _ =>
      withJsonBody[BusinessEntity] { businessEntity: BusinessEntity =>
        val eventualResult = for {
          amendmentType <- getAmendmentTypeForBusinessEntity(businessEntity)
          ref = UndertakingRef(undertakingRef)
          _ <- eisConnector.addMember(ref, businessEntity, amendmentType)
        } yield Ok(Json.toJson(ref))

        eventualResult.foreach { _ =>
          logger.info(s"successfully addMember undertaking $undertakingRef BusinessEntity:$businessEntity")
        }
        eventualResult.failed.foreach { e =>
          logger.error(s"failed updateUndertaking undertaking $undertakingRef  BusinessEntity:$businessEntity", e)
        }

        eventualResult
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
        val eventualResult = eisConnector.deleteMember(UndertakingRef(undertakingRef), businessEntity).map { _ =>
          Ok(Json.toJson(UndertakingRef(undertakingRef)))
        }

        eventualResult.foreach { _ =>
          logger.info(s"successfully deleteMember undertaking $undertakingRef BusinessEntity:$businessEntity")
        }
        eventualResult.failed.foreach { e =>
          logger.error(s"failed deleteMember undertaking $undertakingRef  BusinessEntity:$businessEntity", e)
        }

        eventualResult
      }
  }

  def updateSubsidy(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyUpdate] { update: SubsidyUpdate =>
      val eventualResult = eisConnector.upsertSubsidyUsage(update).map { _ =>
        Ok(Json.toJson(update.undertakingIdentifier))
      }

      eventualResult.foreach { _ =>
        logger.info(s"successfully updateSubsidy SubsidyUpdate $update")
      }
      eventualResult.failed.foreach { e =>
        logger.error(s"failed updateSubsidy SubsidyUpdate $update", e)
      }

      eventualResult
    }
  }

  def retrieveSubsidies(): Action[JsValue] = authenticator.authorisedWithJson(parse.json) { implicit request => _ =>
    withJsonBody[SubsidyRetrieve] { retrieve: SubsidyRetrieve =>
      val eventualResult =
        eisConnector.retrieveSubsidies(retrieve.undertakingIdentifier, retrieve.inDateRange).map { e =>
          Ok(Json.toJson(e))
        }

      eventualResult.foreach { _ =>
        logger.info(s"successfully retrieveSubsidies SubsidyRetrieve $retrieve")
      }
      eventualResult.failed.foreach { e =>
        logger.error(s"failed retrieveSubsidies SubsidyRetrieve $retrieve", e)
      }

      eventualResult
    }
  }

}
