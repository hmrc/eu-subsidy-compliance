

# Undertaking/Subsidy request

On EIS to get subsidies we call 
```
scp/getundertakingtransactions/v1
```

To update subsidies we call

```
scp/amendundertakingsubsidyusage/v1
```


We have 2 types of subsidy. 

## HmrcSubsidy
We do not register this type, only retrieve. Where does this come from as it keeps track of currency related
information?

This type has both 

* hmrcSubsidyAmtGBP
* hmrcSubsidyAmtEUR

Optional fields. This type possibly fires on the fly conversions, this possibly causes the failure for GB123456123456 internally 
in EIS.

**ERRORCODE,202. Error while fetching the Currency conversion values**

```
uk.gov.hmrc.eusubsidycompliance.models.json.digital.EisBadResponseException: 2023-07-26T12:31:05Z NOT_OK 
List(Params(ERRORCODE,202), Params(ERRORTEXT,Error while fetching the Currency conversion values)) 
at uk.gov.hmrc.eusubsidycompliance.models.json.digital.package$.uk$gov$hmrc$eusubsidycompliance$models$json$digital$package$$$anonfun$readResponseFor$1(package.scala:88)
at uk.gov.hmrc.eusubsidycompliance.models.json.digital.package$$anonfun$readResponseFor$2.reads(package.scala:79) 
at play.api.libs.json.JsValue.validate(JsValue.scala:17)
at play.api.libs.json.JsValue.validate$(JsValue.scala:16) at play.api.libs.json.JsObject.validate(JsValue.scala:126) 
at uk.gov.hmrc.http.HttpReadsJson.$anonfun$readJsResult$1(HttpReadsInstances.scala:107) at uk.gov.hmrc.http.HttpReads.$anonfun$map$1(HttpReads.scala:52) 
at uk.gov.hmrc.http.HttpReads$$anon$3.read(HttpReads.scala:57) at uk.gov.hmrc.http.HttpReads$$anon$3.read(HttpReads.scala:57) 
at uk.gov.hmrc.http.HttpReads$$anon$3.read(HttpReads.scala:57) at uk.gov.hmrc.http.HttpReads$$anon$3.read(HttpReads.scala:57) 
at uk.gov.hmrc.http.HttpPost.$anonfun$POST$4(HttpPost.scala:55) at scala.concurrent.impl.Promise$Transformation.run(Promise.scala:467) 
at akka.dispatch.BatchingExecutor$AbstractBatch.processBatch(BatchingExecutor.scala:63) 
at akka.dispatch.BatchingExecutor$BlockableBatch.$anonfun$run$1(BatchingExecutor.scala:100) 
at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18) 
at scala.concurrent.BlockContext$.withBlockContext(BlockContext.scala:94) 
at akka.dispatch.BatchingExecutor$BlockableBatch.run(BatchingExecutor.scala:100) 
at akka.dispatch.TaskInvocation.run(AbstractDispatcher.scala:49) 
at uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService.$anonfun$execute$1(MDCPropagatingExecutorService.scala:53) 
at akka.dispatch.ForkJoinExecutorConfigurator$AkkaForkJoinTask.exec(ForkJoinExecutorConfigurator.scala:48) 
at java.base/java.util.concurrent.ForkJoinTask.doExec(Unknown Source) 
at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(Unknown Source) 
at java.base/java.util.concurrent.ForkJoinPool.scan(Unknown Source) 
at java.base/java.util.concurrent.ForkJoinPool.runWorker(Unknown Source) 
at java.base/java.util.concurrent.ForkJoinWorkerThread.run(Unknown Source)
```
How do these records get created?

## NonHmrcSubsidy

Has values in EUR only. This is the value we actually send to the subsidy create in the service, hence the confusion why things 
blow up on conversion.


