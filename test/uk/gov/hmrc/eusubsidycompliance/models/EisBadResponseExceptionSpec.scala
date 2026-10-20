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
