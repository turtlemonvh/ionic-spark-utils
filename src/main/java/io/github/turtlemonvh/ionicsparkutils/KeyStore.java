package io.github.turtlemonvh.ionicsparkutils;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import java.util.Set;

public interface KeyStore {
  // Return the keyspace associated with this keystore
  public String getKeySpace();

  // Add a single key.
  // A new key id should be generated for the caller if the id of "key" is the empty string.
  public CreateKeysResponse.Key addKey(CreateKeysResponse.Key key);

  // Get the key for a given id
  // Returns null if the key is not found
  public CreateKeysResponse.Key getKeyById(String keyId);

  // Get the key tags of any keys associated with a given external id
  public Set<String> getKeyIdsForExternalId(String externalId);

  // Update a key and return error code
  // Errors can be returned for attempts to modify a key that doesn't exist,
  // attempting to modify fixed attributes, etc.
  public int updateKey(UpdateKeysRequest.Key key);
}
