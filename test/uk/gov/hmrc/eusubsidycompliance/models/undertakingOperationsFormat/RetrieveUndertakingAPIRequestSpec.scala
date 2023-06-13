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

package uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat

import org.joda.time.{LocalDate, LocalDateTime}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import uk.gov.hmrc.eusubsidycompliance.models.json.eis.RequestCommon
import uk.gov.hmrc.eusubsidycompliance.models.types.{AcknowledgementRef, EORI}

import java.time.Instant

class RetrieveUndertakingAPIRequestSpec extends AnyWordSpecLike with Matchers {

  "RetrieveUndertakingAPIRequest" should {
    "convert a baseRequest to a valid EIS definition" in {

      //As there is static random stuff and time calls within we are going to have to dance
      //When writing code to be testable controlling time and random is highly important
      //so should allow easier ways to do what I am doing
      //The benefit of these types of tests is that we can confirm the payloads are what
      //we expect easily. Also changes to json are picked up.
      val baseRequest = RetrieveUndertakingAPIRequest(EORI("GB1234453334333"))
      val akrefTest = "abcdefghijklmabcdefghijklmabcdefghijklm" // m is 13 so 39 chars
      val receiptDate = Instant.EPOCH.toString
      val retrieveUndertakingRequest = baseRequest.retrieveUndertakingRequest
      val requestCommon: RequestCommon = retrieveUndertakingRequest.requestCommon
        .copy(acknowledgementReference = AcknowledgementRef(akrefTest), receiptDate = receiptDate)

      val jsValue = RetrieveUndertakingAPIRequest.writes.writes(
        baseRequest.copy(retrieveUndertakingRequest = retrieveUndertakingRequest.copy(requestCommon = requestCommon))
      )

      Json.prettyPrint(jsValue) mustBe Json.prettyPrint(Json.parse(s"""
          |{
          |  "retrieveUndertakingRequest": {
          |    "requestCommon": {
          |      "originatingSystem": "MDTP",
          |      "receiptDate": "$receiptDate",
          |      "acknowledgementReference": "$akrefTest",
          |      "messageTypes": {
          |        "messageType": "RetrieveUndertaking"
          |      },
          |      "requestParameters": [
          |        {
          |          "paramName": "REGIME",
          |          "paramValue": "SC"
          |        }
          |      ]
          |    },
          |    "requestDetail": {
          |      "idType": "EORI",
          |      "idValue": "GB1234453334333"
          |    }
          |  }
          |}
          |""".stripMargin))
    }
  }
}
