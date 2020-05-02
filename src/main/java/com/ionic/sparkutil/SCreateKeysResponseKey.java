package com.ionic.sparkutil;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;

import java.io.Serializable;

/*
 * Added until `CreateKeysResponse.Key implements Serializable` so we can serialize our key store implementation
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.Key.html
 */
public class SCreateKeysResponseKey extends CreateKeysResponse.Key implements Serializable {
  public SCreateKeysResponseKey() {
    super();
  }

  public static SCreateKeysResponseKey FromCreateKeysResponseKey(CreateKeysResponse.Key src) {
    SCreateKeysResponseKey dest = new SCreateKeysResponseKey();
    // Copy
    dest.setDeviceId(src.getDeviceId());
    dest.setRefId(src.getRefId());
    dest.setAttributesMap(src.getAttributesMap());
    dest.setAttributesSigBase64FromServer(src.getAttributesSigBase64FromServer());
    dest.setId(src.getId());
    dest.setKey(src.getKey());
    dest.setMutableAttributesMap(src.getMutableAttributesMap());
    dest.setMutableAttributesMapFromServer(src.getMutableAttributesMapFromServer());
    dest.setMutableAttributesSigBase64FromServer(src.getMutableAttributesSigBase64FromServer());
    dest.setObligationsMap(src.getObligationsMap());
    dest.setOrigin(src.getOrigin());
    return dest;
  }

  public CreateKeysResponse.Key toCreateKeysResponseKey() {
    SCreateKeysResponseKey src = this;
    CreateKeysResponse.Key dest = new CreateKeysResponse.Key();
    // Copy
    dest.setDeviceId(src.getDeviceId());
    dest.setRefId(src.getRefId());
    dest.setAttributesMap(src.getAttributesMap());
    dest.setAttributesSigBase64FromServer(src.getAttributesSigBase64FromServer());
    dest.setId(src.getId());
    dest.setKey(src.getKey());
    dest.setMutableAttributesMap(src.getMutableAttributesMap());
    dest.setMutableAttributesMapFromServer(src.getMutableAttributesMapFromServer());
    dest.setMutableAttributesSigBase64FromServer(src.getMutableAttributesSigBase64FromServer());
    dest.setObligationsMap(src.getObligationsMap());
    dest.setOrigin(src.getOrigin());
    return dest;
  }
}
