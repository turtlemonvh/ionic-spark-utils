import Dependencies._
import Versions._

lazy val scala212 = "2.12.10"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

lazy val root = (project in file(".")).

  settings(
    inThisBuild(List(
      organization := "io.github.turtlemonvh",
      scalaVersion := scalaVer
    )),
    name := "ionicsparkutils",
    version := sparkUtilVer,

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
    javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a", "+q"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    parallelExecution in Test := false,
    fork := true,

    coverageHighlighting := true,
    crossScalaVersions := supportedScalaVersions,

    // Timothy Van Heest (Sonatype) <timothy.vanheest@gmail.com>
    usePgpKeyHex("E28A79FE457C8B867A9D37D1B1E97D3579E8CA30"),

    // uses compile classpath for the run task, including "provided" jar (cf http://stackoverflow.com/a/21803413/3827)
    run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated,

    libraryDependencies ++= sparkDeps ++ coreDeps ++ testDeps,
    pomIncludeRepository := { x => false },
    resolvers ++= repos,

    // Documentation
    autoAPIMappings := true,

    // publish settings
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )

