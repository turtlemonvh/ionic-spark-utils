package com.ionic.sparkutil;

import com.ionic.sdk.device.profile.DeviceProfile;

import java.io.Serializable;

/*
 * Added until `DeviceProfile implements Serializable`
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/device/profile/DeviceProfile.html
 */
public class SDeviceProfile extends DeviceProfile implements Serializable {
  public SDeviceProfile(
      java.lang.String name,
      long creationTimestamp,
      java.lang.String deviceId,
      java.lang.String server,
      byte[] aesCdIdcKey,
      byte[] aesCdEiKey) {
    super(name, creationTimestamp, deviceId, server, aesCdIdcKey, aesCdEiKey);
  }
}
