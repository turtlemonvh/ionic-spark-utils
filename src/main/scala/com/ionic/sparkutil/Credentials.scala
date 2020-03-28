package com.ionic.sparkutil

import scala.collection.JavaConverters._

/*
Load credential


https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/key/KeyServices.html
- for testing
*/

import com.ionic.sdk.agent.Agent
import com.ionic.sdk.device.profile.persistor.DeviceProfiles

// https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/Agent.html#Agent-com.ionic.sdk.device.profile.persistor.DeviceProfiles-

object Credentials {

  val deviceProfileEnvVar = "IONIC_PROFILE"

  /**
   * Load device profile from an environment variable.
   */
  def profileFromEnvVar(): String = {
    val envVars = System.getenv().asScala
    envVars.get(deviceProfileEnvVar).get
  }

  /**
   * Gets an agent object from a JSON string.
   */
  def getAgent(profileJson: String): Agent = {
    // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/device/profile/persistor/DeviceProfiles.html
    val deviceProfiles = new DeviceProfiles(profileJson)
    new Agent(deviceProfiles)
  }

}