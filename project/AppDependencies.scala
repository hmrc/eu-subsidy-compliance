import sbt._

object AppDependencies {

  val bootStrapVersion = "8.3.0"
  val hmrcMongoVersion = "1.7.0"


  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "1.9.0",
    "org.typelevel"     %% "cats-core"                    % "2.9.0",
    "com.chuusai"       %% "shapeless"                    % "2.3.10"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"        % bootStrapVersion % "test, it",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % "test, it",
    "org.scalamock"     %% "scalamock"               % "5.2.0"          % Test
  )
}
