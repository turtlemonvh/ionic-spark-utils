package com.ionic.sparkutil;

import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import java.util.Set;

public interface KeyStore {
  public CreateKeysResponse.Key addKey(CreateKeysResponse.Key ccrk, boolean shouldGenerateKeyId);

  public CreateKeysResponse.Key getKeyById(String keyId);

  public Set<String> getKeyIdsForExternalId(String externalId);

  public int updateKey(UpdateKeysRequest.Key key);
}
