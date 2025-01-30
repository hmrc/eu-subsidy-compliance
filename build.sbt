import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "eu-subsidy-compliance"

PlayKeys.playDefaultPort := 9094

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*models.*;.*connectors.EuropaConnector;.*connectors.ProxiedHttpClient;.*config.*;.*TimeProvider;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*testonly.*;.*job.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  javaOptions ++= Seq(
    // Uncomment this to use a separate conf for tests
    // "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)

//Check both integration and normal scopes so formatAndTest can be applied when needed more easily.
Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

addCommandAlias("precommit", ";scalafmt;test:scalafmt;it/Test/scalafmt;coverage;test;it/test;coverageReport")



