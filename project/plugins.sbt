lazy val scalariformVer = "1.8.2"
lazy val scalastyleVer = "1.0.0"
lazy val scoverageVer = "1.5.1"
lazy val javaFormatterVer = "0.5.1";

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Spark Package Main Repo" at "https://dl.bintray.com/spark-packages/maven"

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % scalariformVer)
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % scalastyleVer)
addSbtPlugin("org.scoverage" % "sbt-scoverage" % scoverageVer)
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % javaFormatterVer)