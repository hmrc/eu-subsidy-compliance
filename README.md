
# eu-subsidy-compliance

This is a placeholder README.md for a new repository

This microservice serves the following purposes:
- Create a new Undertaking
- Retrieve an existing undertaking
- Update an undertaking
- Add member to an Undertaking.
- Delete member from an undertaking
- Upsert undertaking subsidy
- retrieve existing undertaking subsidies.

This service depends on the following services
- [auth]("https://github.com/hmrc/auth")
- [eu-subsidy-stub]("https://github.com/hmrc/eu-subsidy-compliance-stub")

## Persistence

This service uses `mongodb` to persist user answers about Undertaking, Business Entity and Subsidies.

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) using the
[Play framework](http://playframework.com/), so needs at least a [JRE] to run.

JRE/JDK 1.8 is recommended.

The service also depends on `mongodb`

## Running the service

All dependant services can run via
```
sm --start ESC_ALL
```
By default, this service runs on port `9094`. To bring up only this service , use
```
sbt run
```

## Testing the service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to
provide test coverage reports.

Use the following command to run the tests with coverage and generate a report.

```
sbt clean coverage test it:test coverageReport
```

## Endpoints

##### Headers

The following request headers must be provided for accessing all the end points

* `Authorization` set to valid `Bearer` token string
* `Content-type` set to `application/json` for normal usage

### `GET /undertaking/:eori`

Retrieve an undertaking based on a EORI. A `200` (OK) response is returned if a correct Undertaking response json 
is retrieved.

Example response body:
```json
  {
  "reference": "ABCDE",
  "name": "Test Undertaking Ltd",
  "industrySector": "0",
  "industrySectorLimit": 511.5,
  "lastSubsidyUsageUpdt": "2021-08-03",
  "undertakingBusinessEntity": [
    {
      "businessEntityIdentifier": "GB123456789092",
      "leadEORI": true
    }
  ]
}
```

### `POST /undertaking`

Create an undertaking based on an undertaking request. In addition to this ,it also creates the subsidy usage for that 
newly created undertaking with NilSubmissionDate. A `200` (OK) response is returned if both undertaking and subsidy usage
data has been created. The example request body shows a sample for the create undertaking request

Example request body:
```json
 {
  "name": "Test Undertaking Ltd",
  "industrySector": "0",
  "industrySectorLimit": 511.5,
  "lastSubsidyUsageUpdt": "2021-08-03",
  "undertakingBusinessEntity": [
    {
      "businessEntityIdentifier": "GB123456789092",
      "leadEORI": true
    }
  ]
}
```

Example response body:
```json
 "lxrndsymibjpcsg"
```

### `POST   /undertaking/update`

Update an undertaking for it's name/sector based on an update request. This api call don't update the Business Entity 
member of that Undertaking. A `200` (OK) response is returned when an undertaking is successfully updated. 
The example request body shows a sample for update undertaking request

Example request body:
```json
 {
  "reference": "zmmnodopnhxbl",
  "name": "Test new Undertaking Ltd",
  "industrySector": "1",
  "industrySectorLimit": 511.5,
  "lastSubsidyUsageUpdt": "2021-08-03",
  "undertakingBusinessEntity": [
    {
      "businessEntityIdentifier": "GB123456789020",
      "leadEORI": true
    }
  ]
}
```

Example response body:
```json
 "zmmnodopnhxbl"
```

### `POST       /undertaking/member/:undertakingRef `

Create or update a Business Entity member to an undertaking based on undertakingRef. 
A `200` (OK) response is returned when a business entity member is successfully inserted or updated for an undertaking.
The example request body shows a sample for add member request

Example request body:
```json
    {
      "businessEntityIdentifier": "GB123456789030",
      "leadEORI": false
    }
```

Example response body:
```json
 "zmmnodopnhxbl"
```

### `POST       /undertaking/member/remove/:undertakingRef`

Removes a Business Entity member from an undertaking based on undertakingRef.
A `200` (OK) response is returned when a business entity member is successfully removed from an undertaking.
The example request body shows a sample for remove member request

Example request body:
```json
    {
      "businessEntityIdentifier": "GB123456789030",
      "leadEORI": false
    }
```

Example response body:
```json
 "zmmnodopnhxbl"
```

### `POST       /subsidy/update`
Update subsidy information for an undertaking based on reference in the request body.
A `200` (OK) response is returned when a subsidy is successfully updated.
The example request body shows a sample for remove member request

Example request body:
```json
    {
      "undertakingIdentifier": "zmmnodopnhxbl",
      "undertakingSubsidyAmendment": [
        {
          "subsidyUsageTransactionId": "ah2gPyR",
          "allocationDate": "2021-07-03",
          "submissionDate": "2021-02-11",
          "publicAuthority": "Direct NI",
          "traderReference": "o85hqwL",
          "nonHMRCSubsidyAmtEUR": 54378631168,
          "businessEntityIdentifier": "XI360259082657",
          "amendmentType": "3"
        },
        {
          "subsidyUsageTransactionId": "xIx3",
          "allocationDate": "2021-04-14",
          "submissionDate": "2020-12-24",
          "publicAuthority": "Direct NI",
          "traderReference": "6M",
          "nonHMRCSubsidyAmtEUR": 37080547328,
          "businessEntityIdentifier": "XI938993317490",
          "amendmentType": "2"
        }
      ]
    }
```

Example response body:
```json
 "zmmnodopnhxbl"
```

### `POST             /subsidy/retrieve`
Retrieve subsidy information for an undertaking based on the request body.
A `200` (OK) response is returned when a subsidy information is successfully retrieved.
The example request body shows a sample for remove member request

Example request body:

```json
{
  "undertakingIdentifier": "zmmnodopnhxbl",
  "dateFromNonHMRCSubsidyUsage": "2021-07-03",
  "dateToNonHMRCSubsidyUsage": "2021-04-14"
}
```

Example response body:
```json
 {
  "undertakingIdentifier": "zmmnodopnhxbl",
  "getNonHMRCUsageTransaction": true,
  "getHMRCUsageTransaction": false,
  "dateFromNonHMRCSubsidyUsage": "2021-07-03",
  "dateToNonHMRCSubsidyUsage": "2021-04-14",
  "dateFromHMRCSubsidyUsage": "2021-07-19",
  "dateToHMRCSubsidyUsage": "2021-07-19"
}
```
## Monitoring

The following grafana and kibana dashboards are availble for this service
* [grafana](https://grafana.tools.production.tax.service.gov.uk/d/RwwxDLSnz/eu-subsidy-compliance-frontend)
* [kibana](https://kibana.tools.production.tax.service.gov.uk/app/kibana#/dashboard/eu-subsidy-compliance-frontend)

## Runbook

The runbook for this service can be found
[here](https://confluence.tools.tax.service.gov.uk/display/SC/Runbook+-+Subsidy+Compliance).
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").