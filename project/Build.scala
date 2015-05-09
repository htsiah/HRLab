import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName = "hrlab"
  val appVersion = "1.0"
    
  val appDependencies = Seq(
      play.PlayImport.cache,
      "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
      "com.typesafe.play" %% "play-mailer" % "2.4.0-RC1"
  )
  
  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    scalaVersion := "2.11.1",
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= appDependencies
  )

}