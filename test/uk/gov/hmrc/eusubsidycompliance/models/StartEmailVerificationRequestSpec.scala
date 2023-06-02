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

package uk.gov.hmrc.eusubsidycompliance.models

import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.eusubsidycompliance.models.types.EORI
import uk.gov.hmrc.eusubsidycompliance.shared.BaseSpec

class StartEmailVerificationRequestSpec extends BaseSpec {

  val startEmailVerificationRequest: StartEmailVerificationRequest =
    StartEmailVerificationRequest(eori = EORI("GB123123123123"), emailAddress = "a@a.com")

  private val jsonText =
    """
      |{
      |  "eori" : "GB123123123123",
      |  "emailAddress" : "a@a.com"
      |}
      |""".stripMargin

  "StartEmailVerificationRequest" should {
    "decode to json" in {
      val actualJsonObject: JsValue =
        StartEmailVerificationRequest.startEmailVerificationRequestFormat.writes(startEmailVerificationRequest)

      val expectedJsonObject: JsValue = Json.parse(jsonText)

      actualJsonObject mustBe expectedJsonObject
    }

    "decode from json" in {
      val actual = StartEmailVerificationRequest.startEmailVerificationRequestFormat.reads(Json.parse(jsonText))
      actual mustBe JsSuccess(startEmailVerificationRequest)
    }
  }
}
