import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "hrsifu"
  val appVersion = "1.3.1"
  
  val appDependencies = Seq(
      play.PlayImport.cache,
      "org.reactivemongo" %% "play2-reactivemongo" % "0.11.4.play24",
      "com.typesafe.play" %% "play-mailer" % "3.0.1"
  )
  
  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= appDependencies
  )

}