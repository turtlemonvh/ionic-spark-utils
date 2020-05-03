package com.ionic.sparkutil;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.key.KeyBase;
import com.ionic.sdk.agent.key.KeyMetadata;
import com.ionic.sdk.agent.key.AgentKey;
import com.ionic.sdk.agent.key.KeyObligationsMap;

import java.io.Serializable;

/*
 * Added until `CreateKeysResponse.Key implements Serializable` so we can serialize our key store implementation
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.Key.html
 *
 * This doesn't work because of serialization and inheritance in Java:
 * https://www.geeksforgeeks.org/object-serialization-inheritance-java/
 * https://stackoverflow.com/questions/14042319/how-serialization-works-when-only-subclass-implements-serializable
 * Instance variables inherited from the non-serializable superclass are not serialized.
 *
 * We need to implement our own version of this which stored data directly on serializable objects.
 *
 *
 * Note that key attributes map is serializable:
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/key/KeyAttributesMap.html
 *
 * AgentKey is not serializable
 * https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/key/AgentKey.html
 */
public class SAgentKey implements Serializable, KeyBase, KeyMetadata {
  private String id;
  private byte[] key;
  private String origin;
  private KeyAttributesMap attrsmap;
  private KeyAttributesMap mattrsmap;
  private KeyAttributesMap smattrsmap;
  private KeyObligationsMap obligationsmap;
  // From server
  private String attrssig;
  private String mattrssig;

  public SAgentKey() {
    super();
  }

  public SAgentKey(AgentKey src) {
    super();
    // Copy
    this.setId(src.getId());
    this.setKey(src.getKey());
    this.setOrigin(src.getOrigin());
    this.setAttributesMap(src.getAttributesMap());
    this.setAttributesSigBase64FromServer(src.getAttributesSigBase64FromServer());
    this.setMutableAttributesMap(src.getMutableAttributesMap());
    this.setMutableAttributesMapFromServer(src.getMutableAttributesMapFromServer());
    this.setMutableAttributesSigBase64FromServer(src.getMutableAttributesSigBase64FromServer());
    this.setObligationsMap(src.getObligationsMap());
  }

  public void copyAttrs(AgentKey dest) {
    SAgentKey src = this;
    // Copy
    dest.setId(src.getId());
    dest.setKey(src.getKey());
    dest.setOrigin(src.getOrigin());
    dest.setAttributesMap(src.getAttributesMap());
    dest.setAttributesSigBase64FromServer(src.getAttributesSigBase64FromServer());
    dest.setMutableAttributesMap(src.getMutableAttributesMap());
    dest.setMutableAttributesMapFromServer(src.getMutableAttributesMapFromServer());
    dest.setMutableAttributesSigBase64FromServer(src.getMutableAttributesSigBase64FromServer());
    dest.setObligationsMap(src.getObligationsMap());
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public byte[] getKey() {
    return this.key;
  }

  public void setKey(byte[] key) {
    this.key = key;
  }

  public String getOrigin() {
    return this.origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public KeyAttributesMap getAttributesMap() {
    return this.attrsmap;
  }

  public void setMutableAttributesMap(KeyAttributesMap keyAttributes) {
    this.mattrsmap = keyAttributes;
  }

  public KeyAttributesMap getMutableAttributesMap() {
    return this.mattrsmap;
  }

  public void setMutableAttributesMapFromServer(KeyAttributesMap keyAttributes) {
    this.smattrsmap = keyAttributes;
  }

  public KeyAttributesMap getMutableAttributesMapFromServer() {
    return this.smattrsmap;
  }

  public void setAttributesMap(KeyAttributesMap keyAttributes) {
    this.attrsmap = keyAttributes;
  }

  public String getAttributesSigBase64FromServer() {
    return this.attrssig;
  }

  public void setAttributesSigBase64FromServer(String attributesSigBase64FromServer) {
    this.attrssig = attributesSigBase64FromServer;
  }

  public String getMutableAttributesSigBase64FromServer() {
    return this.mattrssig;
  }

  public void setMutableAttributesSigBase64FromServer(String attributesSigBase64FromServer) {
    this.mattrssig = attributesSigBase64FromServer;
  }

  public KeyObligationsMap getObligationsMap() {
    return this.obligationsmap;
  }

  public void setObligationsMap(KeyObligationsMap obligations) {
    this.obligationsmap = obligations;
  }
}
