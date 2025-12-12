import sbt._

object AppDependencies {

  val bootStrapVersion = "10.4.0"
  val hmrcMongoVersion = "2.11.0"


  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "4.3.0",
    "org.typelevel"     %% "cats-core"                    % "2.13.0",
    "com.chuusai"       %% "shapeless"                    % "2.3.13"
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootStrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.scalamock"     %% "scalamock"               % "7.5.2"          % Test
  )
}
