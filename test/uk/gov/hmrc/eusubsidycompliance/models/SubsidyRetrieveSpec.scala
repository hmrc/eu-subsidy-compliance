package uk.gov.hmrc.eusubsidycompliance.models

import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.eusubsidycompliance.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliance.shared.BaseSpec

class SubsidyRetrieveSpec extends BaseSpec {

  val startEmailVerificationRequest: SubsidyRetrieve =
    SubsidyRetrieve(UndertakingRef("NI050036956"), None)

  private val jsonText =
    """
      |{
      |  "undertakingIdentifier": "NI050036956",
      |  "getNonHMRCUsageTransaction": true,
      |  "getHMRCUsageTransaction": true
      |}
      |""".stripMargin

  "StartEmailVerificationRequest" should {
    "decode to json" in {
      val actualJsonObject: JsValue =
        SubsidyRetrieve.writes.writes(startEmailVerificationRequest)

      println(actualJsonObject.toString())

      val expectedJsonObject: JsValue = Json.parse(jsonText)

      actualJsonObject mustBe expectedJsonObject
    }

    "decode from json" in {
      val actual = SubsidyRetrieve.reads.reads(Json.parse(jsonText))
      actual mustBe JsSuccess(startEmailVerificationRequest)
    }
  }

}
