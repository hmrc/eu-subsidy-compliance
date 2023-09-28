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

import cats.implicits.catsSyntaxOptionId
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.types.{EisParamValue, RegexValidatedString}
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.EisParamName.EisParamName
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.EisStatus.EisStatus

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}

object EisStatus extends Enumeration {
  type EisStatus = Value
  val OK, NOT_OK = Value

  implicit val format: Format[EisStatus.Value] = Json.formatEnum(EisStatus)
}

object EisParamName extends Enumeration {
  type EisParamName = Value
  val ERRORCODE, ERRORTEXT = Value

  implicit val format: Format[EisParamName.Value] = Json.formatEnum(EisParamName)
}
case class Params(
  paramName: EisParamName,
  paramValue: EisParamValue
)

case object Params {
  implicit val paramsFormat: OFormat[Params] =
    Json.format[Params]
}

object EisStatusString
    extends RegexValidatedString(
      """.{0,100}"""
    )

case class ResponseCommon(
  status: EisStatus,
  statusText: String,
  processingDate: LocalDateTime,
  returnParameters: Option[List[Params]]
)

object ResponseCommon {
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
  implicit class RichLocalDateTime(in: LocalDateTime) {
    def eisFormat: String =
      formatter.format(in.toInstant(ZoneOffset.UTC).minusNanos(in.getNano))
  }

  implicit val ldtwrites: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    override def writes(o: LocalDateTime): JsValue = JsString(o.eisFormat)
  }
  implicit val writes: Writes[ResponseCommon] = (
    (JsPath \ "status").write[EisStatus] and
      (JsPath \ "statusText").write[String] and
      (JsPath \ "processingDate").write[LocalDateTime] and
      (JsPath \ "returnParameters").writeNullable[List[Params]]
  )(unlift(ResponseCommon.unapply))

  def apply(errorCode: String, errorText: String): ResponseCommon =
    ResponseCommon(
      EisStatus.NOT_OK,
      EisStatusString("String"), // taken verbatim from spec
      LocalDateTime.now,
      List(
        Params(
          EisParamName.ERRORCODE,
          EisParamValue(errorCode)
        ),
        Params(
          EisParamName.ERRORTEXT,
          EisParamValue(errorText)
        )
      ).some
    )

  def apply(receiptDate: String): ResponseCommon =
    ResponseCommon(
      EisStatus.OK,
      EisStatusString("String"), // taken verbatim from spec
      LocalDateTime.parse(receiptDate),
      None
    )

  implicit val format: Format[ResponseCommon] = Json.format[ResponseCommon]

}
