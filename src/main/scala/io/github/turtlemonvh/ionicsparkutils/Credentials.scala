package io.github.turtlemonvh.ionicsparkutils

import scala.collection.JavaConverters._

import com.ionic.sdk.agent.Agent
import com.ionic.sdk.device.profile.persistor.DeviceProfiles

/**
 * Helper methods for working with [[com.ionic.sdk.device.profile.persistor.DeviceProfiles DeviceProfiles]].
 */
object Credentials {

  /**
   * Name of env var from which the device profile can be loaded.
   */
  val deviceProfileEnvVar = "IONIC_PROFILE"

  /**
   * Returns a string containing a JSON serialized [[com.ionic.sdk.device.profile.persistor.DeviceProfiles DeviceProfile]], loaded from an environment variable.
   */
  def profileFromEnvVar(): String = {
    val envVars = System.getenv().asScala
    envVars.get(deviceProfileEnvVar).get
  }

  /**
   * Return an [[com.ionic.sdk.agent.Agent Agent]] object from a JSON string.
   *
   * @param profileJson A JSON string containing a device profile
   */
  def getAgent(profileJson: String): Agent = {
    val deviceProfiles = new DeviceProfiles(profileJson)
    new Agent(deviceProfiles)
  }

}