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

package uk.gov.hmrc.eusubsidycompliance.test

import play.api.mvc.{Request, Result}
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.eusubsidycompliance.controllers.actions.Authenticator
import uk.gov.hmrc.eusubsidycompliance.test.Fixtures.eori
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

// Fake authenticator that allows every request.
class FakeAuthenticator
    extends Authenticator(
      authConnector = new FakeAuthConnector(),
      controllerComponents = Helpers.stubControllerComponents()
    ) {
  override def authCommon[A](
    action: AuthAction[A]
  )(implicit request: Request[A], executionContext: ExecutionContext): Future[Result] = action(request)(eori)
}

// This isn't used in this implementation so can be left as unimplemented.
class FakeAuthConnector extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = throw new NotImplementedError(
    "FakeAuthConnector.authorise is not implemented as this should not be called in tests"
  )
}
