/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import scala.concurrent.{ExecutionContext, Future}

class SyntaxSpec extends AnyWordSpec with Matchers with ScalaFutures {
  import FutureSyntax._
  import OptionTSyntax._

  implicit val ec: ExecutionContext = ExecutionContext.global

  "FutureOps" should {
    "convert a value to a successful Future" in {
      "hello".toFuture.futureValue shouldBe "hello"
    }
  }

  "FutureToOptionTOps" should {
    "convert a Future to OptionT" in {
      Future.successful("hello").toContext.value.futureValue shouldBe Some("hello")
    }
  }

  "ValueToOptionTOps" should {
    "convert a value to OptionT" in {
      "hello".toContext.value.futureValue shouldBe Some("hello")
    }
  }

  "OptionToOptionTOps" should {
    "convert a Some to OptionT" in {
      Some("hello").toContext.value.futureValue shouldBe Some("hello")
    }
    "convert a None to OptionT" in {
      None.toContext.value.futureValue shouldBe None
    }
  }

  "FutureOptionToOptionTOps" should {
    "convert a Future[Option] to OptionT" in {
      Future.successful(Some("hello")).toContext.value.futureValue shouldBe Some("hello")
    }
  }
}
