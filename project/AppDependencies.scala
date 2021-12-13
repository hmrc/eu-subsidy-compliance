import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.13.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.53.0",
    "org.typelevel"           %% "cats-core"                  % "2.6.1",
    "com.chuusai"             %% "shapeless"                  % "2.3.7"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.13.0"            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.53.0"            % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it",
    "org.mockito"             % "mockito-core"                % "3.9.0"             % Test,
    "org.scalatestplus"      %% "scalatestplus-mockito"       % "1.0.0-M2"          % Test
  )
}
