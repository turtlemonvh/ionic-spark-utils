package com.ionic.sparkutil;

import com.ionic.sdk.device.profile.DeviceProfile;

import java.io.Serializable;

/*
 * Added until `DeviceProfile implements Serializable`
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/device/profile/DeviceProfile.html
 */
public class SDeviceProfile implements Serializable {
  private String name;
  private String deviceId;
  private String server;
  private long creationTimestamp;
  private byte[] aesCdIdcKey;
  private byte[] aesCdEiKey;

  public SDeviceProfile(
      java.lang.String name,
      long creationTimestamp,
      java.lang.String deviceId,
      java.lang.String server,
      byte[] aesCdIdcKey,
      byte[] aesCdEiKey) {
    this.name = name;
    this.creationTimestamp = creationTimestamp;
    this.deviceId = deviceId;
    this.server = server;
    this.aesCdIdcKey = aesCdIdcKey;
    this.aesCdEiKey = aesCdEiKey;
  }

  public DeviceProfile toDeviceProfile() {
    return new DeviceProfile(
        this.name,
        this.creationTimestamp,
        this.deviceId,
        this.server,
        this.aesCdIdcKey,
        this.aesCdEiKey);
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDeviceId() {
    return this.deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getServer() {
    return this.server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public long getCreationTimestampSecs() {
    return this.creationTimestamp;
  }

  public void setCreationTimestampSecs(long creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public byte[] getAesCdIdcProfileKey() {
    return this.aesCdIdcKey;
  }

  public void setAesCdIdcProfileKey(byte[] keyBytes) {
    this.aesCdIdcKey = keyBytes;
  }

  public byte[] getAesCdEiProfileKey() {
    return this.aesCdEiKey;
  }

  public void setAesCdEiProfileKey(byte[] keyBytes) {
    this.aesCdEiKey = keyBytes;
  }

  public Boolean isLoaded() {
    return ((this.aesCdEiKey.length != 0) && (this.aesCdIdcKey.length != 0));
  }
}
