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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliance.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliance.models.types._

import java.time.LocalDate

case class UndertakingCreate(
  name: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: Option[IndustrySectorLimit],
  lastSubsidyUsageUpdt: Option[LocalDate],
  undertakingBusinessEntity: List[BusinessEntity]
) {

  //name is user entered so could break GPDR
  lazy val loggableString: String =
    s"""UndertakingCreate:
       | nameLength:${name.length},
       | industrySector:$industrySector,
       | industrySectorLimit:$industrySectorLimit,
       | lastSubsidyUsageUpdt:$lastSubsidyUsageUpdt,
       | undertakingBusinessEntity:$undertakingBusinessEntity""".stripMargin
}

object UndertakingCreate {
  implicit val undertakingFormat: OFormat[UndertakingCreate] = Json.format[UndertakingCreate]
}

case class UndertakingRetrieve(
  reference: Option[UndertakingRef],
  name: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: Option[IndustrySectorLimit],
  lastSubsidyUsageUpdt: Option[LocalDate],
  undertakingBusinessEntity: List[BusinessEntity]
) {
  lazy val loggableString: String =
    s"""UndertakingRetrieve:
       | reference:$reference,
       | nameLength:${name.length},
       | industrySector:$industrySector,
       | industrySectorLimit:$industrySectorLimit,
       | lastSubsidyUsageUpdt:$lastSubsidyUsageUpdt,
       | undertakingBusinessEntity:$undertakingBusinessEntity""".stripMargin
}

object UndertakingRetrieve {
  implicit val undertakingFormat: OFormat[UndertakingRetrieve] = Json.format[UndertakingRetrieve]
}
