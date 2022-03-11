import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {
  val bootStrapVersion = "5.20.0"
  val hmrcMongoVersion = "0.60.0"
  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootStrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
    "org.typelevel" %% "cats-core" % "2.7.0",
    "com.chuusai" %% "shapeless" % "2.3.8"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootStrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % "test, it",
    // TODO - remove mockito once all tests transitioned to scalamock
    "org.mockito" % "mockito-core" % "3.9.0" % Test,
    "org.scalatestplus" %% "scalatestplus-mockito" % "1.0.0-M2" % Test,
    // Need to use a slightly older version for compatibility with the play jackson deps using 2.10.5
    "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % Test,
    "org.scalamock" %% "scalamock" % "5.2.0" % Test
  )
}
