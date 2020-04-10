package com.ionic.sparkutil;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.core.rng.CryptoRng;
import com.ionic.sdk.core.codec.Transcoder;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * TestKeyStore is a map based key store which is safe for concurrent access.
 * Internals are exposed as public attributes for convenience in evaluating state in tests.
 */
public class TestKeyStore {

  public String keyspace;
  private ReentrantReadWriteLock keyCreateModifyLock;
  public HashMap<String, CreateKeysResponse.Key> keys;
  public HashMap<String, Set<String>> externalIdToKeyId;
  private final CryptoRng cryptoRng;
  private int currentKeyNum = 0;

  // Defaults
  private static final String ionicExternalIdAttributeName = "ionic-external-id";
  private static final int keyIdLength = 7;

  // Base constructor
  public TestKeyStore(String keyspace) throws IonicException {
    this.keyspace = keyspace;
    this.cryptoRng = new CryptoRng();

    // Initialize keystore
    this.keys = new HashMap<String, CreateKeysResponse.Key>();
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

  // * Generate the next key id.
  /* No locking around this method.
  /* Expects to be called in addKeyToStore which is guarded by a lock.
  */
  private String generateNextKeyId() throws IonicException {
    this.currentKeyNum++;
    final byte[] keyNum = this.intToBytes(this.currentKeyNum, keyIdLength);
    return (this.keyspace + this.base64UrlSafeEncode(keyNum)).substring(0, 4 + keyIdLength);
  }

  /* Add a single key to store, assigning a new id to the key if the key id is null.
  /* Also updates the external id index.
  /* Guarded by a lock to prevent concurrent modification of the key store.
  */
  public CreateKeysResponse.Key addKey(CreateKeysResponse.Key ccrk) throws IonicException {
    this.keyCreateModifyLock.writeLock().lock();

    // Overwrite the key id
    ccrk.setId(this.generateNextKeyId());

    // Add to keystore
    this.keys.put(ccrk.getId(), ccrk);

    // Update mapping of external ids to keys
    // TODO: Should mutable attribute setting of external id work?
    if (ccrk.getAttributesMap().containsKey(ionicExternalIdAttributeName)) {
      for (String externalId : ccrk.getAttributesMap().get(ionicExternalIdAttributeName)) {
        if (this.externalIdToKeyId.get(externalId) == null) {
          this.externalIdToKeyId.put(externalId, new HashSet());
        }
        this.externalIdToKeyId.get(externalId).add(ccrk.getId());
      }
    }

    this.keyCreateModifyLock.writeLock().unlock();

    // Return possibly modified key
    return ccrk;
  }

  public CreateKeysResponse.Key getKeyById(String keyId) {
    this.keyCreateModifyLock.readLock().lock();
    CreateKeysResponse.Key key = this.keys.get(keyId);
    this.keyCreateModifyLock.readLock().unlock();
    return key;
  }

  public Set<String> getKeyIdsForExternalId(String externalId) {
    this.keyCreateModifyLock.readLock().lock();
    Set<String> keyIds = this.externalIdToKeyId.getOrDefault(externalId, new HashSet<String>());
    this.keyCreateModifyLock.readLock().unlock();
    return keyIds;
  }
}
