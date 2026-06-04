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

package uk.gov.hmrc.eusubsidycompliance.models.types

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliance.models.types

import scala.util.matching.Regex

trait ValidatedType[BaseType] {

  trait Tag

  private lazy val className: String = this.getClass.getSimpleName

  def validateAndTransform(in: BaseType): Option[BaseType]

  def apply(in: BaseType): BaseType =
    of(in).getOrElse {
      throw new IllegalArgumentException(
        s""""$in" is not a valid ${className.init}"""
      )
    }

  def of(in: BaseType): Option[BaseType] =
    validateAndTransform(in) map { x =>
      x
    }
}

class RegexValidatedString(
  val regex: String,
  transform: String => String = identity
) extends ValidatedType[String] {

  private val regexCompiled: Regex = regex.r

  def validateAndTransform(in: String): Option[String] =
    transform(in).some.filter(regexCompiled.findFirstIn(_).isDefined)
}

trait SimpleJson {

  private def validatedStringFormat(
    A: ValidatedType[String],
    name: String
  ): Format[String] = new Format[String] {

    override def reads(
      json: JsValue
    ): JsResult[String] = json match {
      case JsString(value) =>
        A.validateAndTransform(value) match {
          case Some(v) => JsSuccess(A(v))
          case None => JsError(s"Expected a valid $name, got $value instead")
        }
      case xs: JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $xs instead""")))
    }

    override def writes(
      o: String
    ): JsValue = JsString(o)
  }

  private def validatedBigDecimalFormat(
    A: ValidatedType[BigDecimal],
    name: String
  ): Format[BigDecimal] = new Format[BigDecimal] {
    override def reads(json: JsValue): JsResult[BigDecimal] =
      json match {
        case JsNumber(value) =>
          A.validateAndTransform(value) match {
            case Some(v) => JsSuccess(A(v))
            case None => JsError(s"Expected a valid $name, got $value instead.")
          }
        case xs: JsValue =>
          JsError(
            JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $xs instead"""))
          )
      }

    override def writes(o: BigDecimal): JsValue = JsNumber(BigDecimal(o.toString))
  }

  implicit val sectorLimitFormat: Format[BigDecimal] =
    validatedBigDecimalFormat(IndustrySectorLimit, "IndustrySectorLimit")

  implicit val positiveSubsidyAmountFormat: Format[BigDecimal] =
    validatedBigDecimalFormat(PositiveSubsidyAmount, "PositiveSubsidyAmount")

  implicit val subsidyAmountFormat: Format[BigDecimal] =
    validatedBigDecimalFormat(SubsidyAmount, "SubsidyAmount")

  implicit val eisParamValueFormat: Format[String] =
    validatedStringFormat(EisParamValue, "paramValue")

  implicit val eisStatusStringFormat: Format[String] =
    validatedStringFormat(EisStatusString, "eisStatusString")

  implicit val undertakingRefFormat: Format[String] =
    validatedStringFormat(UndertakingRef, "undertakingReference")

  implicit val undertakingNameFormat: Format[String] =
    validatedStringFormat(UndertakingName, "undertakingName")

  implicit val emailAddressFormat: Format[String] =
    validatedStringFormat(EmailAddress, "emailAddress")

  implicit val eoriFormat: Format[String] =
    validatedStringFormat(EORI, "eori")

  implicit val subsidyRefFormat: Format[String] =
    validatedStringFormat(SubsidyRef, "subsidyRef")

  implicit val amendmentTypeFormat: Format[String] =
    validatedStringFormat(EisSubsidyAmendmentType, "amendmentType")

  implicit val traderRefFormat: Format[String] =
    validatedStringFormat(TraderRef, "traderRef")

  implicit val declarationIDFormat: Format[String] =
    validatedStringFormat(DeclarationID, "declarationId")

  implicit val taxTypeFormat: Format[String] =
    validatedStringFormat(TaxType, "taxType")

}
