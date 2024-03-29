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

package uk.gov.hmrc.eusubsidycompliance.controllers.actions

import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}

import scala.concurrent.Future

trait AuthTestSupport extends MockitoSugar {

  lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def withAuthorizedUser(enrolments: Enrolments = Enrolments(Set.empty)): Unit =
    when(mockAuthConnector.authorise(any(), argEq(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(enrolments))

  def withUnauthorizedUser(error: Throwable): Unit =
    when(mockAuthConnector.authorise(any(), any())(any(), any())).thenReturn(Future.failed(error))
}
