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

/**
  * Taken from https://github.com/pbyrne84/scala-case-class-prettification which is mine, on maven etc.
  * Helps remove some of the headaches when diffing failed case class comparisons etc.
  *  1st rule of testing from an engineer perspective, what are the tests like when they fail, does it take minimal
  *  effort to work out the cause? If not that is going to be someone else headache, leaving people headaches is not nice.
  *  (I will leave some headaches I am sure)
  *
  *  Example CacheItem will diff like this
  *
  *  CacheItem(
  *   id = "GB123456789010",
  *   data = JsObject(
  *     underlying = Map(verified -> true)
  *   ),
  *   createdAt = 2023-06-02T11:23:06.410Z,
  *   modifiedAt = 2023-06-02T11:23:06.410Z
  * )
  */
object Prettifiers {
  val caseClassPrettifier: CaseClassPrettifier = new CaseClassPrettifier

  implicit val prettifier: Prettifier = Prettifier.apply {
    case a: AnyRef if CaseClassPrettifier.shouldBeUsedInTestMatching(a) =>
      caseClassPrettifier.prettify(a)

    case a => Prettifier.default(a)
  }
}
