package com.ionic.sparkutil;

/*

https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/key/KeyServices.html
*/
import com.ionic.sdk.key.KeyServices;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.key.KeyObligationsMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.error.SdkError;
import com.ionic.sdk.error.ServerError;
import com.ionic.sdk.core.date.DateTime;
import com.ionic.sdk.core.value.Value;
import com.ionic.sdk.core.rng.CryptoRng;
import com.ionic.sdk.core.codec.Transcoder;
import com.ionic.sdk.cipher.aes.AesCipher;

import com.ionic.sparkutil.TestKeyStore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

/*
 * TestAgent is a mock based on KeyServices (https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/key/KeyServices.html).
 * Internals are exposed as public attributes for convenience in evaluating state in tests.
 */
public class TestAgent implements KeyServices {
  public DeviceProfile profile;
  public String origin;
  public TestKeyStore keystore;
  private final CryptoRng cryptoRng;

  // Defaults
  private static final String defaultKeyspace = "ABCD";
  private static final String defaultProfileName = "default";
  private static final String defaultHostname = "ionic.com";

  // Base constructor
  public TestAgent(TestKeyStore keystore, String deviceId, String origin) throws IonicException {
    // Handle changing nulls to defaults
    if (keystore == null) {
      keystore = new TestKeyStore(defaultKeyspace);
    }
    this.keystore = keystore;
    if (origin == null) {
      origin = defaultHostname;
    }
    this.origin = origin;
    if (deviceId == null) {
      final String uuid = UUID.randomUUID().toString();
      deviceId = Value.join(Value.DOT, this.keystore.keyspace, uuid.substring(0, 1), uuid);
    }

    this.cryptoRng = new CryptoRng();
    // Initialize profile
    final String profileName = defaultProfileName;
    final long creationTimestamp = (System.currentTimeMillis() / DateTime.ONE_SECOND_MILLIS);
    final byte[] aesCdIdcKey = cryptoRng.rand(new byte[AesCipher.KEY_BYTES]);
    final byte[] aesCdEiKey = cryptoRng.rand(new byte[AesCipher.KEY_BYTES]);
    this.profile =
        new DeviceProfile(
            profileName, creationTimestamp, deviceId, this.origin, aesCdIdcKey, aesCdEiKey);
  }

  public TestAgent() throws IonicException {
    this(new TestKeyStore(defaultKeyspace), null, null);
  }

  public TestAgent(TestKeyStore keystore) throws IonicException {
    this(keystore, null, null);
  }

  @Override
  public CreateKeysResponse createKey() throws IonicException {
    return this.createKey(new KeyAttributesMap(), new KeyAttributesMap(), new MetadataMap());
  }

  @Override
  public CreateKeysResponse createKey(KeyAttributesMap attributes) throws IonicException {
    return this.createKey(attributes, new KeyAttributesMap(), new MetadataMap());
  }

  @Override
  public CreateKeysResponse createKey(
      KeyAttributesMap attributes, KeyAttributesMap mutableAttributes) throws IonicException {
    return this.createKey(attributes, mutableAttributes, new MetadataMap());
  }

  @Override
  public CreateKeysResponse createKey(KeyAttributesMap attributes, MetadataMap metadata)
      throws IonicException {
    return this.createKey(attributes, new KeyAttributesMap(), metadata);
  }

  @Override
  public CreateKeysResponse createKey(MetadataMap metadata) throws IonicException {
    return this.createKey(new KeyAttributesMap(), new KeyAttributesMap(), metadata);
  }

  @Override
  public CreateKeysResponse createKey(
      KeyAttributesMap attributes, KeyAttributesMap mutableAttributes, MetadataMap metadata)
      throws IonicException {
    // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.html
    CreateKeysResponse ccr = new CreateKeysResponse();
    addKeyToCreateKeyResponse(
        new CreateKeysRequest.Key("1", 1, attributes, mutableAttributes), ccr);
    return ccr;
  }

  /*
   * Returns a https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.Key.html
   */
  private void addKeyToCreateKeyResponse(CreateKeysRequest.Key key, CreateKeysResponse ccr)
      throws IonicException {
    for (int ikey = 0; ikey < key.getQuantity(); ikey++) {
      KeyObligationsMap obligations = new KeyObligationsMap();
      CreateKeysResponse.Key ccrk =
          new CreateKeysResponse.Key(
              key.getRefId(),
              "1", // Will be set for us in `addKey`
              cryptoRng.rand(new byte[AesCipher.KEY_BYTES]),
              this.getActiveProfile().getDeviceId(),
              key.getAttributesMap(),
              key.getMutableAttributesMap(),
              obligations,
              this.origin);
      ccrk = this.keystore.addKey(ccrk);
      ccr.add(ccrk);
    }
  }

  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    CreateKeysResponse ccr = new CreateKeysResponse();
    for (CreateKeysRequest.Key key : request.getKeys()) {
      this.addKeyToCreateKeyResponse(key, ccr);
    }
    return ccr;
  }

  @Override
  public DeviceProfile getActiveProfile() {
    return this.profile;
  }

  @Override
  public boolean hasActiveProfile() {
    return true;
  }

  @Override
  public GetKeysResponse getKey(String keyId) {
    return this.getKey(keyId, new MetadataMap());
  }

  /*
  Add the response to an external id query to the response object.
  */
  private void addExternalIdKeyToGetKeyResponse(
      final String externalId, MetadataMap metadata, GetKeysResponse response) {
    // Accumulate list of keys
    ArrayList<CreateKeysResponse.Key> keys = new ArrayList<CreateKeysResponse.Key>();
    Set<String> keyIds = this.keystore.getKeyIdsForExternalId(externalId);
    for (String keyId : keyIds) {
      CreateKeysResponse.Key key = this.keystore.getKeyById(keyId);
      if (keyId != null && key == null) {
        // The key id is defined, but we can't find it
        this.addMissingKeyErrorToResponse(keyId, response);
      } else {
        this.addSingleKeyToResponse(key, response);
      }
    }

    // Add a single query response for each external id query.
    // No QueryResponse is returned unless there is an external id component to the query.
    // The list may be empty.
    List<String> mappedIdList = new ArrayList<String>(keyIds);
    response.add(new GetKeysResponse.QueryResult(externalId, mappedIdList));
  }

  private void addMissingKeyErrorToResponse(String keyId, GetKeysResponse response) {
    // Add an error and query result but not a key
    // Calls to getKey will raise an exception?
    // https://dev.ionic.com/sdk/errors
    int serverError = ServerError.PROCESSING_ERROR; // Better: 40030
    String errorMessage = "Unknown key";
    response.add(
        new GetKeysResponse.IonicError(keyId, SdkError.ISCRYPTO_OK, serverError, errorMessage));
  }

  /*
   * Add a single key grabbed from the key store.
   */
  private void addSingleKeyToResponse(CreateKeysResponse.Key key, GetKeysResponse response) {
    response.add(
        new GetKeysResponse.Key(
            key.getId(),
            key.getKey(),
            key.getDeviceId(),
            key.getAttributesMap(),
            key.getMutableAttributesMap(),
            key.getObligationsMap(),
            key.getOrigin(),
            key.getAttributesSigBase64FromServer(),
            key.getMutableAttributesSigBase64FromServer()));
  }

  // Update response with key from this query
  // No QueryResult objects are added for individual key ids
  private void addKeyToGetKeyResponse(
      final String keyId, MetadataMap metadata, GetKeysResponse response) {
    CreateKeysResponse.Key key = this.keystore.getKeyById(keyId);
    if (key == null) {
      this.addMissingKeyErrorToResponse(keyId, response);
    } else {
      this.addSingleKeyToResponse(key, response);
    }
  }

  @Override
  public GetKeysResponse getKey(String keyId, MetadataMap metadata) {
    GetKeysResponse response = new GetKeysResponse();
    addKeyToGetKeyResponse(keyId, metadata, response);
    return response;
  }

  /*
  Handle a query which may be composed of both external ids and key ids by adding
  QueryResults, Errors, and Keys to a GetKeysResponse object.
  */
  @Override
  public GetKeysResponse getKeys(GetKeysRequest request) {
    GetKeysResponse response = new GetKeysResponse();
    for (String keyId : request.getKeyIds()) {
      addKeyToGetKeyResponse(keyId, request.getMetadata(), response);
    }
    for (String externalId : request.getExternalIds()) {
      addExternalIdKeyToGetKeyResponse(externalId, request.getMetadata(), response);
    }
    return response;
  }

  @Override
  public UpdateKeysResponse updateKeys(final UpdateKeysRequest request) throws IonicException {
    throw new IonicException(SdkError.ISAGENT_NOTIMPLEMENTED);
  }

  @Override
  public UpdateKeysResponse updateKey(final UpdateKeysRequest.Key key, final MetadataMap metadata)
      throws IonicException {
    throw new IonicException(SdkError.ISAGENT_NOTIMPLEMENTED);
  }
}
