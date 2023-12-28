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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.eusubsidycompliance.models.types.{EORI, EisSubsidyAmendmentType, SubsidyAmount, SubsidyRef, TraderRef, UndertakingRef}

import java.time.LocalDate

class SubsidyUpdateSpec extends AnyWordSpec with Matchers {

  private val maxLengthTraderRef = "TraderRef-".padTo(400, 'a')
  private val maxValueSubsidyUpdateJson = s"""
       |{
       |  "undertakingIdentifier" : "uIdentifier-xxxxx",
       |  "undertakingSubsidyAmendment" : [ {
       |    "subsidyUsageTransactionId" : "subsidyref",
       |    "allocationDate" : "1970-01-01",
       |    "submissionDate" : "1970-01-02",
       |    "publicAuthority" : "publicAuthority-1",
       |    "traderReference" : "$maxLengthTraderRef",
       |    "nonHMRCSubsidyAmtEUR" : 99999999999.99,
       |    "businessEntityIdentifier" : "GB1234512345123",
       |    "amendmentType" : "1"
       |  } ]
       |}
       |""".stripMargin.trim

  private val maxValueSubsidyUpdate = SubsidyUpdate(
    undertakingIdentifier = UndertakingRef("uIdentifier-".padTo(17, 'x').mkString),
    update = UndertakingSubsidyAmendment(
      updates = List(
        NonHmrcSubsidy(
          subsidyUsageTransactionId = Some(SubsidyRef("subsidyref")),
          allocationDate = LocalDate.EPOCH,
          submissionDate = LocalDate.EPOCH.plusDays(1),
          publicAuthority = Some("publicAuthority-1"),
          traderReference = Some(TraderRef(maxLengthTraderRef)),
          nonHMRCSubsidyAmtEUR = SubsidyAmount(BigDecimal(99999999999.99)),
          businessEntityIdentifier = Some(EORI("GB1234512345123")),
          amendmentType = Some(EisSubsidyAmendmentType("1"))
        )
      )
    )
  )

  "SubsidyUpdate" should {
    "encode to json with a UndertakingSubsidyAmendment" in {
      Json.prettyPrint(SubsidyUpdate.updateFormat.writes(maxValueSubsidyUpdate)) mustBe maxValueSubsidyUpdateJson
    }

    "decode json to a UndertakingSubsidyAmendment" in {
      SubsidyUpdate.updateFormat.reads(Json.parse(maxValueSubsidyUpdateJson)) mustBe JsSuccess(maxValueSubsidyUpdate)
    }
  }
}
