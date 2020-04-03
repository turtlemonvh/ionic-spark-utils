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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

/*
 * TestAgent is a mock based on KeyServices (https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/key/KeyServices.html).
 *
 */
public class TestAgent implements KeyServices {
  public String keyspace;
  public DeviceProfile profile;
  public String origin;
  public HashMap<String, CreateKeysResponse.Key> keys;
  public HashMap<String, Set<String>> externalIdToKeyId;
  private final CryptoRng cryptoRng;

  // Defaults
  private static final String ionicExternalIdAttributeName = "ionic-external-id";
  private static final String defaultKeyspace = "ABCD";
  private static final String defaultProfileName = "default";
  private static final String defaultHostname = "ionic.com";
  private static final int keyIdLength = 7;

  // Base constructor
  public TestAgent(String keyspace, String deviceId, String origin) {
    if (deviceId == null) {
      final String uuid = UUID.randomUUID().toString();
      deviceId = Value.join(Value.DOT, keyspace, uuid.substring(0, 1), uuid);
    }
    this.keyspace = keyspace;
    this.origin = origin;
    this.cryptoRng = new CryptoRng();
    // Initialize profile
    final String profileName = defaultProfileName;
    final long creationTimestamp = (System.currentTimeMillis() / DateTime.ONE_SECOND_MILLIS);
    final byte[] aesCdIdcKey = this.genRandomBytes(AesCipher.KEY_BYTES);
    final byte[] aesCdEiKey = this.genRandomBytes(AesCipher.KEY_BYTES);
    this.profile =
        new DeviceProfile(
            profileName, creationTimestamp, deviceId, this.origin, aesCdIdcKey, aesCdEiKey);
    // Initialize keystore
    this.keys = new HashMap<String, CreateKeysResponse.Key>();
    this.externalIdToKeyId = new HashMap<String, Set<String>>();
  }

  public TestAgent() {
    this(defaultKeyspace, null, defaultHostname);
  }

  private static byte[] genRandomBytes(int n) {
    final byte[] key = new byte[n];
    try {
      // https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html
      SecureRandom.getInstanceStrong().nextBytes(key);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException("Unexpected error");
    }
    return key;
  }

  private String base64UrlSafeEncode(byte[] bts) {
    return Transcoder.base64().encode(bts).replace("+", "-").replace("/", "_");
  }

  private String generateKeyId() throws IonicException {
    final byte[] randomId = cryptoRng.rand(new byte[keyIdLength]);
    return (this.keyspace + this.base64UrlSafeEncode(randomId)).substring(0, keyIdLength);
  }

  private void addKeyToStore(CreateKeysResponse.Key ccrk) {
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
    addKeyToResponse(new CreateKeysRequest.Key("1", 1, attributes, mutableAttributes), ccr);
    return ccr;
  }

  /*
   * Returns a https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.Key.html
   */
  private void addKeyToResponse(CreateKeysRequest.Key key, CreateKeysResponse ccr)
      throws IonicException {
    for (int ikey = 0; ikey < key.getQuantity(); ikey++) {
      String keyId = this.generateKeyId();
      KeyObligationsMap obligations = new KeyObligationsMap();
      CreateKeysResponse.Key ccrk =
          new CreateKeysResponse.Key(
              key.getRefId(),
              keyId,
              this.genRandomBytes(AesCipher.KEY_BYTES),
              this.getActiveProfile().getDeviceId(),
              key.getAttributesMap(),
              key.getMutableAttributesMap(),
              obligations,
              this.origin);
      ccr.add(ccrk);
      this.addKeyToStore(ccrk);
    }
  }

  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    CreateKeysResponse ccr = new CreateKeysResponse();
    for (CreateKeysRequest.Key key : request.getKeys()) {
      this.addKeyToResponse(key, ccr);
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

  // Update response with key from this query
  private void addKeyToResponse(
      final String keyId, MetadataMap metadata, GetKeysResponse response) {
    if (!this.keys.containsKey(keyId)) {
      // Add an error and query result but not a key
      // Calls to getKey will raise an exception?
      // https://dev.ionic.com/sdk/errors
      int clientError = SdkError.ISCRYPTO_OK;
      int serverError = ServerError.PROCESSING_ERROR; // Better: 40030
      String errorMessage = "Unknown key";
      response.add(new GetKeysResponse.IonicError(keyId, clientError, serverError, errorMessage));
      response.add(new GetKeysResponse.QueryResult(keyId, serverError, errorMessage));
    }
    CreateKeysResponse.Key key = this.keys.get(keyId);

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
    // FIXME: Update this map of external id to key id
    List<String> mappedIdList = new ArrayList<String>();
    response.add(new GetKeysResponse.QueryResult(keyId, mappedIdList));
  }

  @Override
  public GetKeysResponse getKey(String keyId, MetadataMap metadata) {
    GetKeysResponse response = new GetKeysResponse();
    addKeyToResponse(keyId, metadata, response);
    return response;
  }

  @Override
  public GetKeysResponse getKeys(GetKeysRequest request) {
    GetKeysResponse response = new GetKeysResponse();
    for (String keyId : request.getKeyIds()) {
      addKeyToResponse(keyId, request.getMetadata(), response);
    }
    for (String externalId : request.getExternalIds()) {
      for (String keyId : this.externalIdToKeyId.get(externalId)) {
        addKeyToResponse(keyId, request.getMetadata(), response);
      }
      // FIXME: Update the `mappedIdList` query stuff
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
