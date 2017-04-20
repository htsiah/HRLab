name := "hrsifu"

version := "1.9"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
	play.PlayImport.cache,
	"org.reactivemongo" %% "play2-reactivemongo" % "0.11.5.play24",
	"com.typesafe.play" %% "play-mailer" % "3.0.1",
	"com.github.tototoshi" %% "scala-csv" % "1.3.4"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
