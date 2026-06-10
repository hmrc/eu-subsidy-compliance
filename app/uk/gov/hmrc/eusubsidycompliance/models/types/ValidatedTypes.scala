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

import scala.util.matching.Regex

trait ValidatedType[BaseType] {

  opaque type Type <: BaseType = BaseType

  private lazy val className: String = this.getClass.getSimpleName

  def validateAndTransform(in: BaseType): Option[BaseType]

  def apply(in: BaseType): Type =
    of(in).getOrElse {
      throw new IllegalArgumentException(
        s""""$in" is not a valid ${className.init}"""
      )
    }

  def of(in: BaseType): Option[BaseType] =
    validateAndTransform(in)
}

abstract class RegexValidatedString(
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
  ): Format[A.Type] = new Format[A.Type] {

    override def reads(
      json: JsValue
    ): JsResult[A.Type] = json match {
      case JsString(value) =>
        A.validateAndTransform(value) match {
          case Some(v) => JsSuccess(A(v))
          case None => JsError(s"Expected a valid $name, got $value instead")
        }
      case xs: JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $xs instead""")))
    }

    override def writes(
      o: A.Type
    ): JsValue = JsString(o)
  }

  private def validatedBigDecimalFormat(
    A: ValidatedType[BigDecimal],
    name: String
  ): Format[A.Type] = new Format[A.Type] {
    override def reads(json: JsValue): JsResult[A.Type] =
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

    override def writes(o: A.Type): JsValue = JsNumber(BigDecimal(o.toString))
  }

  implicit val sectorLimitFormat: Format[IndustrySectorLimit] =
    validatedBigDecimalFormat(IndustrySectorLimit, "IndustrySectorLimit")

  implicit val positiveSubsidyAmountFormat: Format[PositiveSubsidyAmount] =
    validatedBigDecimalFormat(PositiveSubsidyAmount, "PositiveSubsidyAmount")

  implicit val subsidyAmountFormat: Format[SubsidyAmount] =
    validatedBigDecimalFormat(SubsidyAmount, "SubsidyAmount")

  implicit val eisParamValueFormat: Format[EisParamValue] =
    validatedStringFormat(EisParamValue, "paramValue")

  implicit val eisStatusStringFormat: Format[EisStatusString] =
    validatedStringFormat(EisStatusString, "eisStatusString")

  implicit val undertakingRefFormat: Format[UndertakingRef] =
    validatedStringFormat(UndertakingRef, "undertakingReference")

  implicit val undertakingNameFormat: Format[UndertakingName] =
    validatedStringFormat(UndertakingName, "undertakingName")

  implicit val emailAddressFormat: Format[EmailAddress] =
    validatedStringFormat(EmailAddress, "emailAddress")

  implicit val eoriFormat: Format[EORI] =
    validatedStringFormat(EORI, "eori")

  implicit val subsidyRefFormat: Format[SubsidyRef] =
    validatedStringFormat(SubsidyRef, "subsidyRef")

  implicit val amendmentTypeFormat: Format[EisSubsidyAmendmentType] =
    validatedStringFormat(EisSubsidyAmendmentType, "amendmentType")

  implicit val traderRefFormat: Format[TraderRef] =
    validatedStringFormat(TraderRef, "traderRef")

  implicit val declarationIDFormat: Format[DeclarationID] =
    validatedStringFormat(DeclarationID, "declarationId")

  implicit val taxTypeFormat: Format[TaxType] =
    validatedStringFormat(TaxType, "taxType")

}
