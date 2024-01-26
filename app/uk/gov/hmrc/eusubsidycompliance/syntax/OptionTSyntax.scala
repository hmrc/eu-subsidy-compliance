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

package uk.gov.hmrc.eusubsidycompliance.syntax

import cats.data.OptionT
import cats.implicits._
import uk.gov.hmrc.eusubsidycompliance.syntax.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

object OptionTSyntax {

  implicit class FutureToOptionTOps[A](val f: Future[A]) extends AnyVal {
    def toContext(implicit ec: ExecutionContext): OptionT[Future, A] = OptionT[Future, A](f.map(Some(_)))
  }

  implicit class ValueToOptionTOps[A](val a: A) extends AnyVal {
    def toContext: OptionT[Future, A] = OptionT(Option(a).toFuture)
  }

  implicit class OptionToOptionTOps[A](val o: Option[A]) extends AnyVal {
    def toContext(implicit ec: ExecutionContext): OptionT[Future, A] = OptionT.fromOption[Future](o)
  }

  implicit class FutureOptionToOptionTOps[A](val fo: Future[Option[A]]) extends AnyVal {
    def toContext: OptionT[Future, A] = OptionT(fo)
  }

}
