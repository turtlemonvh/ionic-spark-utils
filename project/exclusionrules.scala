import sbt._

object ExclusionRules {
  val excludeNettyIo = ExclusionRule(organization = "io.netty")
  val excludeQQ = ExclusionRule(organization = "org.scalamacros")
}
