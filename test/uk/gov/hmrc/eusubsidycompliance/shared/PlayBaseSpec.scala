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

package uk.gov.hmrc.eusubsidycompliance.shared

import org.scalactic.Prettifier
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

abstract class PlayBaseSpec extends PlaySpec with ScalaFutures with IntegrationPatience with BeforeAndAfter {

  //Don't use this in production. This sets to unlimited. The default one calculates things off core count
  //and is for cpu blocking code. CachedThreadPool are for blocking stuff. This just makes it safe for this context
  //to be used for speed in both cases. In production doing this for everything could be counted as CPU abuse as there
  //can be a much higher amount of concurrent action (concurrent users etc).
  protected implicit val executor: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  //Don't use default config as anything that is not immediate can take a minimum of 400 milli seconds
  //That can really add up in some tests.
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2000, Millis)), interval = scaled(Span(1, Millis)))

  //Intellij may remove it in an optimise imports otherwise
  //This makes case classes etc diff nicely in intellij etc (makes things like kotlins to string)
  implicit val prettifier: Prettifier = Prettifiers.prettifier

  //Just makes autocomplete easier as the add the trait method just makes a mess of the call scope
  //and makes people lean into having to keep opening the traits to refer to them. That approach has a definite
  //limit of pleasantness very very quickly.
  //
  // Organise for ease of memorisation and usage, not how fast you can go in the first couple of stories.
  // I have have had to train myself out of doing the trait thing due to the mess it creates when handed to other people
//  val mongo = new TestMongo()

}
