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

package uk.gov.hmrc.eusubsidycompliance.test

import uk.gov.hmrc.eusubsidycompliance.models.types.{DeclarationID, EORI, EisParamValue, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, TaxType, TraderRef, UndertakingName, UndertakingRef, UndertakingStatus}
import uk.gov.hmrc.eusubsidycompliance.models._
import uk.gov.hmrc.eusubsidycompliance.models.undertakingOperationsFormat.{EisParamName, EisStatus, EisStatusString, GetUndertakingBalanceApiResponse, GetUndertakingBalanceRequest, Params, ResponseCommon, UndertakingBalanceResponse}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

object Fixtures {

  val eori: EORI = EORI.of("GB123456789012").get
  val fixedInstant: Instant = Instant.parse("2022-01-01T12:00:00Z")

  val undertakingReference: UndertakingRef = UndertakingRef.of("SomeUndertakingReference").get
  val undertakingName: UndertakingName = UndertakingName.of("SomeUndertakingName").get
  val sector: types.Sector.Value = Sector.other
  val undertakingStatus: types.UndertakingStatus.Value = UndertakingStatus.active
  val industrySectorLimit: IndustrySectorLimit = IndustrySectorLimit.of(BigDecimal(200000.00)).get
  val date: LocalDate = fixedInstant.atZone(ZoneId.of("Europe/London")).toLocalDate
  val subsidyAmount: SubsidyAmount = SubsidyAmount.of(BigDecimal(123.45)).get

  val undertakingCreate: UndertakingCreate = UndertakingCreate(
    undertakingName,
    sector,
    Some(industrySectorLimit),
    Some(date),
    List(BusinessEntity(eori, leadEORI = true))
  )

  val undertaking: UndertakingRetrieve = UndertakingRetrieve(
    Some(undertakingReference),
    undertakingName,
    sector,
    Some(industrySectorLimit),
    Some(date),
    Some(undertakingStatus),
    List(BusinessEntity(eori, leadEORI = true))
  )

  val businessEntity: BusinessEntity = BusinessEntity(eori, leadEORI = true)

  val subsidyRef: SubsidyRef = SubsidyRef.of("ABC12345").get
  val declarationId: DeclarationID = DeclarationID.of("12345").get
  val traderRef: TraderRef = TraderRef.of("SomeTraderReference").get
  val taxType: TaxType = TaxType.of("1").get
  val publicAuthority = "SomePublicAuthority"

  val hmrcSubsidy: HmrcSubsidy = HmrcSubsidy(
    declarationID = declarationId,
    issueDate = Some(date),
    acceptanceDate = date,
    declarantEORI = eori,
    consigneeEORI = eori,
    taxType = Some(taxType),
    hmrcSubsidyAmtGBP = Some(subsidyAmount),
    hmrcSubsidyAmtEUR = Some(subsidyAmount),
    tradersOwnRefUCR = Some(traderRef)
  )

  val nonHmrcSubsidy: NonHmrcSubsidy = NonHmrcSubsidy(
    subsidyUsageTransactionId = Some(subsidyRef),
    allocationDate = date,
    submissionDate = date,
    publicAuthority = Some(publicAuthority),
    traderReference = Some(traderRef),
    nonHMRCSubsidyAmtEUR = subsidyAmount,
    businessEntityIdentifier = Some(eori),
    amendmentType = None
  )

  val undertakingSubsidies: UndertakingSubsidies = UndertakingSubsidies(
    undertakingIdentifier = undertakingReference,
    nonHMRCSubsidyTotalEUR = subsidyAmount,
    nonHMRCSubsidyTotalGBP = subsidyAmount,
    hmrcSubsidyTotalEUR = subsidyAmount,
    hmrcSubsidyTotalGBP = subsidyAmount,
    nonHMRCSubsidyUsage = List(nonHmrcSubsidy),
    hmrcSubsidyUsage = List(hmrcSubsidy)
  )

  val undertakingBalanceResponse = UndertakingBalanceResponse(
    undertakingIdentifier = undertakingReference,
    nonHMRCSubsidyAllocationEUR = None,
    hmrcSubsidyAllocationEUR = None,
    industrySectorLimit = industrySectorLimit,
    availableBalanceEUR = subsidyAmount,
    availableBalanceGBP = subsidyAmount,
    conversionRate = SubsidyAmount.of(1.2).get,
    nationalCapBalanceEUR = industrySectorLimit
  )

  val validUndertakingBalanceApiRequest = GetUndertakingBalanceRequest(eori = Some(eori))
  val validUndertakingBalanceApiResponse =
    GetUndertakingBalanceApiResponse(getUndertakingBalanceResponse = Some(undertakingBalanceResponse))
  val undertakingBalanceApiErrorResponse = GetUndertakingBalanceApiResponse(
    getUndertakingBalanceResponse = None,
    errorDetail = Some(
      ResponseCommon(
        EisStatus.NOT_OK,
        EisStatusString("String"), // verbatim from spec
        LocalDateTime.now,
        Some(
          List(
            Params(
              EisParamName.ERRORCODE,
              EisParamValue.of("107").get
            ),
            Params(
              EisParamName.ERRORTEXT,
              EisParamValue.of("some not found error").get
            )
          )
        )
      )
    )
  )

}
