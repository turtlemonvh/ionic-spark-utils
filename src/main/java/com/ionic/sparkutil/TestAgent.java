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
import com.ionic.sdk.agent.service.IDC;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.error.SdkError;
import com.ionic.sdk.error.ServerError;
import com.ionic.sdk.core.date.DateTime;
import com.ionic.sdk.core.value.Value;
import com.ionic.sdk.core.codec.Transcoder;
import com.ionic.sdk.cipher.aes.AesCipher;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TestAgent is a mock based on KeyServices (https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/key/KeyServices.html).
 * Internals are exposed as public attributes for convenience in evaluating state in tests.
 */
public class TestAgent implements KeyServicesMinimal, Serializable {
  public SDeviceProfile profile;
  public KeyStore keystore;
  private final SecureRandom rng;

  final Logger logger = LoggerFactory.getLogger(TestAgent.class);

  // Defaults
  private static final String defaultKeyspace = "ABCD";
  private static final String defaultProfileName = "default";
  private static final String defaultHostname = "ionic.com";

  // Error states
  // Set these to cause api calls to return various error codes
  // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/error/package-summary.html
  public int serverErrorState = 0;
  public int clientErrorState = 0;

  // Base constructor
  public TestAgent(KeyStore keystore, String deviceId, String origin) throws IonicException {
    // Handle changing nulls to defaults
    if (keystore == null) {
      keystore = new TestKeyStore(defaultKeyspace);
    }
    this.keystore = keystore;
    if (origin == null) {
      origin = defaultHostname;
    }
    if (deviceId == null) {
      final String uuid = UUID.randomUUID().toString();
      deviceId = Value.join(Value.DOT, this.keystore.getKeySpace(), uuid.substring(0, 1), uuid);
    }

    this.rng = new SecureRandom();
    // Initialize profile
    final String profileName = defaultProfileName;
    final long creationTimestamp = (System.currentTimeMillis() / DateTime.ONE_SECOND_MILLIS);
    final byte[] aesCdIdcKey = this.randBytes(AesCipher.KEY_BYTES);
    final byte[] aesCdEiKey = this.randBytes(AesCipher.KEY_BYTES);

    this.profile =
        new SDeviceProfile(
            profileName, creationTimestamp, deviceId, origin, aesCdIdcKey, aesCdEiKey);
  }

  public TestAgent() throws IonicException {
    this(new TestKeyStore(defaultKeyspace), null, null);
  }

  public TestAgent(KeyStore keystore) throws IonicException {
    this(keystore, null, null);
  }

  /*
   * We don't use com.ionic.sdk.core.rng.CryptoRng because it is not serializable
   */
  private byte[] randBytes(int nbytes) {
    byte bts[] = new byte[nbytes];
    this.rng.nextBytes(bts);
    return bts;
  }

  /*
   * Modifies a CreateKeysResponse object to add the created key. Also adds to the key store.
   */
  private void addKeyToCreateKeyResponse(CreateKeysRequest.Key key, CreateKeysResponse ccr)
      throws IonicException {

    // Handle injected faults
    if (this.clientErrorState != 0) {
      throw new IonicException(this.clientErrorState, "Error creating keys");
    } else if (this.serverErrorState != 0) {
      ccr.setServerErrorCode(this.serverErrorState);
      ccr.setServerErrorMessage("Error creating keys");
      return;
    }

    for (int ikey = 0; ikey < key.getQuantity(); ikey++) {
      KeyObligationsMap obligations = new KeyObligationsMap();
      CreateKeysResponse.Key ccrk =
          new CreateKeysResponse.Key(
              key.getRefId(),
              "", // Will be set for us in `addKey`
              this.randBytes(AesCipher.KEY_BYTES),
              this.getActiveProfile().getDeviceId(),
              key.getAttributesMap(),
              key.getMutableAttributesMap(),
              obligations,
              this.profile.getServer());
      ccrk = this.keystore.addKey(ccrk);
      ccr.add(ccrk);
    }
  }

  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    logger.info("Calling createKeys: requesting " + request.getKeys().size() + " keys.");
    CreateKeysResponse ccr = new CreateKeysResponse();
    for (CreateKeysRequest.Key key : request.getKeys()) {
      this.addKeyToCreateKeyResponse(key, ccr);
    }
    return ccr;
  }

  @Override
  public DeviceProfile getActiveProfile() {
    return this.profile.toDeviceProfile();
  }

  @Override
  public boolean hasActiveProfile() {
    return true;
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
      if (this.clientErrorState != 0) {
        response.add(
            new GetKeysResponse.IonicError(keyId, this.clientErrorState, 0, "Error fetching key"));
      } else if (this.serverErrorState != 0) {
        response.add(
            new GetKeysResponse.IonicError(keyId, 0, this.serverErrorState, "Error fetching key"));
      } else if (keyId != null && key == null) {
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
            this.getActiveProfile().getDeviceId(),
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
    if (this.clientErrorState != 0) {
      response.add(
          new GetKeysResponse.IonicError(keyId, this.clientErrorState, 0, "Error fetching key"));
    } else if (this.serverErrorState != 0) {
      response.add(
          new GetKeysResponse.IonicError(keyId, 0, this.serverErrorState, "Error fetching key"));
    } else if (key == null) {
      this.addMissingKeyErrorToResponse(keyId, response);
    } else {
      this.addSingleKeyToResponse(key, response);
    }
  }

  /*
  Handle a query which may be composed of both external ids and key ids by adding
  QueryResults, Errors, and Keys to a GetKeysResponse object.
  */
  @Override
  public GetKeysResponse getKeys(GetKeysRequest request) {
    logger.info(
        "Calling getKeys: requesting "
            + request.getKeyIds().size()
            + " key ids and "
            + request.getExternalIds().size()
            + " external ids.");
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
    logger.info("Calling updateKeys: updating " + request.getKeys().size() + " keys.");
    UpdateKeysResponse resp = new UpdateKeysResponse();
    for (UpdateKeysRequest.Key key : request.getKeys()) {
      // For each, either add key or error
      int respCode = this.keystore.updateKey(key);
      if (this.clientErrorState != 0) {
        resp.add(
            new UpdateKeysResponse.IonicError(
                key.getId(), this.clientErrorState, 0, "Error updating key"));
      } else if (this.serverErrorState != 0) {
        resp.add(
            new UpdateKeysResponse.IonicError(
                key.getId(), 0, this.serverErrorState, "Error updating key"));
      } else if (respCode != ServerError.SERVER_OK) {
        resp.add(new UpdateKeysResponse.IonicError(key.getId(), 0, respCode, "Error updating key"));
      } else {
        resp.add(
            new UpdateKeysResponse.Key(key, this.profile.getDeviceId(), this.profile.getServer()));
      }
    }
    return resp;
  }
}
