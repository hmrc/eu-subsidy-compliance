
# eu-subsidy-compliance

This microservice serves the following purposes:
- Create a new Undertaking
- Retrieve an existing undertaking
- Retrieve existing undertaking subsidies.
- Update an undertaking
- Add member to an Undertaking.
- Delete member from an undertaking
- Upsert undertaking subsidy
- send email requests to send nudge emails
- Retrieve exchange rates from Europa end point


## Requirements

This service is written in [Scala](http://www.scala-lang.org/) using the
[Play framework](http://playframework.com/), so needs at least a JRE to run.

JRE/JDK 11 is recommended.

## Running the service

All dependant services can run via
```
sm2 --start ESC_ALL
```


### Using localhost

To run this microservice locally on the configured port **'9094'**, you can run:

```
sbt run 
```

**NOTE:** Ensure that you are not running the microservice via service manager before starting your service locally (vice versa) or the service will fail to start


### Accessing the service

Access details can be found on
[DDCY Live Services Credentials sheet](https://docs.google.com/spreadsheets/d/1ecLTROmzZtv97jxM-5LgoujinGxmDoAuZauu2tFoAVU/edit?gid=1186990023#gid=1186990023)
for both staging and local url's or check the Tech Overview section in the
[service summary page ](https://confluence.tools.tax.service.gov.uk/display/ELSY/EUSC+Service+Summary)


## Testing the service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to
provide test coverage reports.

Use the following command to run the tests with coverage and generate a report.

```
sbt clean coverage test coverageReport
```

The following command can also be run to format the code base and then execute all tests:

```
sbt precommit
```

## Formatting
This is governed by the **.scalafmt.conf** file.

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
### `GET             /retrieve-exchange-rate/:date`
This endpoint facilitates the retrieval of exchange rates for a specific date. Specifically, it aims to obtain the exchange rate for the last day of the previous month corresponding to the provided date.

Usage Example:

If the input date is 15/08/2023, the endpoint will retrieve the exchange rate for 31/07/2023.

Process Flow:

The endpoint initiates a check in the MonthlyExchangeRate cache for the required data.
If the data is found in the cache, it is retrieved directly.
If the data is not found in the cache, the module fetches it from the europa endpoint.
A 200 (OK) response is returned upon successful retrieval of the correct date's exchange rate.

Example response body:
```json
{
  "currencyIso": "GBP",
  "refCurrencyIso": "EUR",
  "amount": 0.864,
  "dateStart": "2023-07-01",
  "dateEnd": "2023-07-31",
  "createDate": "2024-02-02"
}

```

### `POST             /email-notification`
This endpoint handles requests to send notification emails to the user.

Suspension Timeline:
Nudge Email Request (76 days):

At 76 days without a submitted report, a reminder email is triggered.
Message type '1' is used to represent a deadline reminder request.
Successful requests will return an 'ACCEPTED' status.

Suspension Email (90+ days):

If the user fails to submit a report by the 90-day mark, another email is sent.
Message type '2' is used to represent the expiration of the deadline.
The account is then suspended.
Successful requests will return an 'ACCEPTED' status.

Communication Types:

Message Type '1': Deadline Reminder Request 
A reminder email sent at 76 days.

Message Type '2': Deadline Expired

An email notifying the user of the account suspension due to the 90+ day period without report submission.

Response:
Successful requests will return an 'ACCEPTED' status.
Example request body:

```json
 {
  "undertakingIdentifier": "ABC12345",
  "businessEntityIdentifier": "GB000000000012",
  "messageType": "1",
  "emailAddress": "jdoe@example.com"
}
```

## Populating exchange rate cache in test environments
The exchange rate data is fetched from an external endpoint - https://ec.europa.eu/budg/inforeuro/api/public/currencies/gbp 

When hitting external endpoints proxy access must be configured, This works fine in QA and production where we have this set up but you should not hit anything external in other environments. To resolve this, we manually run a job in Jenkins to populate the exchange rate cache.

The cache has a TTL of 30 days, and the latest exchange rates will also need to be added in if you wish to use the latest dates in testing. To populate the cache, do the following:

1) Visit the following link: https://build.tax.service.gov.uk/job/build-and-deploy/job/Query-Mongo/
2) Click 'Build with Parameters'
3) Select which environment you wish to update the cache for, e.g. staging, and ensure the 'REPLICA_SET_CHOICE' is set to 'Protected'
4) Paste the following into the query, replacing the ***EXCHANGE RATE DATA*** with the table data in the europa endpoint linked above:
```
use eu-subsidy-compliance;
var records = [
EXCHANGE RATE DATA GOES HERE
];
function convertToDate(str) {
  var parts = str.split("/");
  var day = parts[0];
  var month = parts[1];
  var year = parts[2];
  return ISODate(year + "-" + month.padStart(2, "0") + "-" + day.padStart(2, "0"));
}
records.forEach(function (record) {
  record.dateStart = convertToDate(record.dateStart);
  record.dateEnd = convertToDate(record.dateEnd);
  record.createDate = ISODate();
});

db.getCollection("exchangeRateMonthlyCache").deleteMany({});
db.getCollection("exchangeRateMonthlyCache").insertMany(records);
```
5) Click 'Build' - once the job completes the cache will be populated with data in the relevant environment, allowing you to use the journey without hitting any external endpoint.
## Monitoring

The following grafana and kibana dashboards are availble for this service
* [grafana](https://grafana.tools.production.tax.service.gov.uk/d/RwwxDLSnz/eu-subsidy-compliance-frontend)
* [kibana](https://kibana.tools.production.tax.service.gov.uk/app/kibana#/dashboard/eu-subsidy-compliance-frontend)

## Runbook

The runbook for this service can be found
[here](https://confluence.tools.tax.service.gov.uk/display/SC/Runbook+-+Subsidy+Compliance).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").