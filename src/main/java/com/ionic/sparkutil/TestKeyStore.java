package com.ionic.sparkutil;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.core.codec.Transcoder;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import com.ionic.sdk.error.ServerError;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Map.Entry;
import java.util.Base64;

/*
 * TestKeyStore is a map based key store which is safe for concurrent access.
 * Internals are exposed as public attributes for convenience in evaluating state in tests.
 */
public class TestKeyStore implements KeyStore, Serializable {

  public String keyspace;
  private ReentrantReadWriteLock keyCreateModifyLock;
  public HashMap<String, SAgentKey> keys; // SCreateKeysResponseKey
  public HashMap<String, Set<String>> externalIdToKeyId;
  private int currentKeyNum = 0;

  // Defaults
  private static final String ionicExternalIdAttributeName = "ionic-external-id";
  private static final int keyIdLength = 7;

  // Base constructor
  public TestKeyStore(String keyspace) throws IonicException {
    this.keyspace = keyspace;

    // Initialize keystore
    this.keys = new HashMap<String, SAgentKey>();
    this.externalIdToKeyId = new HashMap<String, Set<String>>();
    this.keyCreateModifyLock = new ReentrantReadWriteLock();
  }

  private String base64UrlSafeEncode(byte[] bts) {
    return Transcoder.base64().encode(bts).replace("+", "-").replace("/", "_");
  }

  // Pack an integer into n bytes
  private static byte[] intToBytes(final int data, final int nbytes) {
    byte[] bts = new byte[nbytes];
    for (int i = 0; i < nbytes; i++) {
      int shift = 8 * (nbytes - i - 1);
      bts[i] = (byte) ((data >> shift) & 0xff);
    }
    return bts;
  }

  public String getKeySpace() {
    return this.keyspace;
  }

  /* Generate the next key id.
  /* Not threadsafe. Expects to be called in `addKey`, which is guarded by a lock.
  */
  private String generateNextKeyId() {
    this.currentKeyNum++;
    final byte[] keyNum = this.intToBytes(this.currentKeyNum, keyIdLength);
    return (this.keyspace + this.base64UrlSafeEncode(keyNum)).substring(0, 4 + keyIdLength);
  }

  /* Add a single key to the store.
  /* Also updates the external id index.
  /* Thread safe.
  */
  public CreateKeysResponse.Key addKey(CreateKeysResponse.Key key) {
    this.keyCreateModifyLock.writeLock().lock();

    // Optionally set key id
    if (key.getId() == "") {
      key.setId(this.generateNextKeyId());
    }

    this.keys.put(key.getId(), new SAgentKey(key));

    // Update mapping of external ids to keys
    // TODO: Should mutable attribute setting of external id work?
    if (key.getAttributesMap().containsKey(ionicExternalIdAttributeName)) {
      for (String externalId : key.getAttributesMap().get(ionicExternalIdAttributeName)) {
        if (this.externalIdToKeyId.get(externalId) == null) {
          this.externalIdToKeyId.put(externalId, new HashSet());
        }
        this.externalIdToKeyId.get(externalId).add(key.getId());
      }
    }

    this.keyCreateModifyLock.writeLock().unlock();

    // Return possibly modified key
    return key;
  }

  public CreateKeysResponse.Key getKeyById(String keyId) {
    this.keyCreateModifyLock.readLock().lock();
    SAgentKey key = this.keys.get(keyId);
    this.keyCreateModifyLock.readLock().unlock();
    if (key == null) {
      return null;
    }
    CreateKeysResponse.Key ccrk = new CreateKeysResponse.Key();
    key.copyAttrs(ccrk);
    return ccrk;
  }

  public Set<String> getKeyIdsForExternalId(String externalId) {
    this.keyCreateModifyLock.readLock().lock();
    Set<String> keyIds = this.externalIdToKeyId.getOrDefault(externalId, new HashSet<String>());
    this.keyCreateModifyLock.readLock().unlock();
    return keyIds;
  }

  public int updateKey(UpdateKeysRequest.Key key) {
    this.keyCreateModifyLock.writeLock().lock();

    SAgentKey skey = this.keys.get(key.getId());
    if (skey == null) {
      // FIXME: Is this the correct error code for a missing key on modify?
      return ServerError.KEY_INVALID_RESOURCE_NAME;
    }

    for (Entry<String, List<String>> attr : skey.getAttributesMap().entrySet()) {
      if (key.getMutableAttributesMap().get(attr.getKey()) != null) {
        // Attempting to set a mutable attribute with same value as a fixed attribute
        return ServerError.KEY_INVALID_CATTR_MATTR;
      }
    }

    skey.setMutableAttributesMap(key.getMutableAttributesMap());
    skey.setMutableAttributesSigBase64FromServer(key.getMutableAttributesSigBase64FromServer());

    // We ignore changes to the key bytes and origin fields
    if (key.getAttributesSigBase64FromServer() != skey.getAttributesSigBase64FromServer()) {
      return ServerError.KEY_MODIFY_FIXED_ATTRIBUTE;
    }
    if (!key.getObligationsMap().equals(skey.getObligationsMap())) {
      return ServerError.KEY_MODIFY_FIXED_ATTRIBUTE;
    }
    if (!key.getAttributesMap().equals(skey.getAttributesMap())) {
      return ServerError.KEY_MODIFY_FIXED_ATTRIBUTE;
    }

    // I'm not sure if this is needed
    this.keys.put(skey.getId(), skey);

    // FIXME: Put in a try...finally
    this.keyCreateModifyLock.writeLock().unlock();

    return ServerError.SERVER_OK;
  }
}
