import sbt._
import ExclusionRules._
import Versions._

object Dependencies {
  lazy val parentPath = new File("..").getCanonicalPath
  lazy val mavenRepo = s"$parentPath/.m2/repository/"

  lazy val sparkDeps = Seq(
    "org.apache.hadoop" % "hadoop-common" % hadoopVer % "provided",
    "org.apache.spark" %% "spark-core" % sparkVer % "provided" excludeAll (excludeNettyIo, excludeQQ),
    "org.apache.spark" %% "spark-streaming" % sparkVer % "provided" excludeAll (excludeNettyIo, excludeQQ),
    "org.apache.spark" %% "spark-sql" % sparkVer % "provided" excludeAll (excludeNettyIo, excludeQQ)
  )

  lazy val coreDeps = Seq(
      "com.ionic" % "ionic-sdk" % "2.6.0"
  )

  /**
   * Add `% "test"` after the dependency to mark it as test-only
   *
   * http://www.scalatest.org/user_guide/using_scalatest_with_sbt
   * https://www.scala-sbt.org/1.x/docs/Library-Dependencies.html
   *
   * Hive is needed for DataFrameSuiteBase: https://github.com/holdenk/spark-testing-base/wiki/DataFrameSuiteBase
   */
  lazy val testDeps = Seq(
    "com.holdenkarau" %% "spark-testing-base" % sparkTestingBaseVer % "test",
    // https://mvnrepository.com/artifact/org.apache.spark/spark-hive
    "org.apache.spark" %% "spark-hive" % sparkVer % "test",
    // https://mvnrepository.com/artifact/org.scalatest/scalatest
    "org.scalatest" %% "scalatest" % scalaTestVer % "test",
    "org.scalacheck" %% "scalacheck" % scalaCheckVer % "test",
    "junit" % "junit" % junitVer % "test",
    "com.novocode" % "junit-interface" % junitInterfaceVer % "test"
  )

  val repos = Seq(
    "Apache public" at "https://repository.apache.org/content/groups/public/",
    "Local Maven Repository" at s"file:///$mavenRepo",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Spark Packages Repository" at "https://dl.bintray.com/spark-packages/maven/",
    "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Second Typesafe repo" at "http://repo.typesafe.com/typesafe/maven-releases/",
    Resolver.sonatypeRepo("public")
  )
}
