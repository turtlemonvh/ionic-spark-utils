// give the user a nice default project!

import Dependencies._
import Versions._

lazy val root = (project in file(".")).

  settings(
    inThisBuild(List(
      organization := "io.github.turtlemonvh",
      scalaVersion := scalaVer
    )),
    name := "ionicsparkutils",
    version := sparkUtilVer,

    // FIXME: "bootstrap class path not set in conjunction with -source 8"
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a", "+q"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    parallelExecution in Test := false,
    fork := true,

    coverageHighlighting := true,

    libraryDependencies ++= sparkDeps ++ coreDeps ++ testDeps,

    // uses compile classpath for the run task, including "provided" jar (cf http://stackoverflow.com/a/21803413/3827)
    run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated,

    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    pomIncludeRepository := { x => false },

    resolvers ++= repos,

    pomIncludeRepository := { x => false },

    // publish settings
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )
