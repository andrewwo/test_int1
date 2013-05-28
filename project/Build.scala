import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "WorkdayDataManager"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
	"org.apache.poi" % "poi" % "3.9",
	"org.apache.poi" % "poi-scratchpad" % "3.9",
	"org.apache.poi" % "poi-ooxml" % "3.9",
	"com.typesafe.slick" %% "slick" % "1.0.0",
	"com.typesafe" %% "play-plugins-mailer" % "2.1.0",
	"org.mindrot" % "jbcrypt" % "0.3m"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    scalacOptions += "-feature"
  )

}
