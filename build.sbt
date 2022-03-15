import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import scoverage.ScoverageKeys

val appName = "eu-subsidy-compliance"

val silencerVersion = "1.7.8"

PlayKeys.playDefaultPort := 9094

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.15",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // Use the silencer plugin to suppress warnings
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*models.*;.*config.*;.*TimeProvider;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*testonly.*"
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  javaOptions ++= Seq(
    // Uncomment this to use a separate conf for tests
    // "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)
