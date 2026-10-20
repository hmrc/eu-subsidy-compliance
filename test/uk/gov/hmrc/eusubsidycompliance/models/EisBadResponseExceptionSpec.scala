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

package uk.gov.hmrc.eusubsidycompliance.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException

class EisBadResponseExceptionSpec extends AnyWordSpec with Matchers {

  "redactEori" should {

    "mask the last 6 digits of an EORI" in {
      val input =
        "EORI GB362060135000 already associated with another Undertaking NI050052470"

      val result = EisBadResponseException.redactEori(input)

      result shouldBe
        "EORI GB362060****** already associated with another Undertaking NI050052470"
    }

    "leave messages without an EORI unchanged" in {
      val input = "EIS returned an unexpected error"

      EisBadResponseException.redactEori(input) shouldBe input
    }

    "redact multiple EORIs in the same message" in {
      val input = "EORI GB362060135000 conflicts with GB123456789012"

      EisBadResponseException.redactEori(input) shouldBe
        "EORI GB362060****** conflicts with GB123456******"
    }
  }
}
