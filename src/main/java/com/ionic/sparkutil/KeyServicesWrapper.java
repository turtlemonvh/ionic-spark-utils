package com.ionic.sparkutil;

import com.ionic.sdk.key.KeyServices;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.service.IDC;
import com.ionic.sdk.device.profile.DeviceProfile;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysResponse;
import com.ionic.sdk.error.IonicException;

import java.io.Serializable;

/*
 * A wrapper around another KeyServices implementation, meant for customizing behavior.
 * This allows wrappers to just override a small number of methods to change behavior.
 *
 * Usually the functions to override include
 *
 *      createKeys(CreateKeysRequest request)
 *      getKeys(GetKeysRequest request)
 *      updateKeys(final UpdateKeysRequest request)
 *
 */
public class KeyServicesWrapper implements KeyServices, Serializable {
  public KeyServices wrapped;

  // Most calls will be delegated down to the wrapped KeyServices object
  public KeyServicesWrapper(KeyServices wrapped) throws IonicException {
    this.wrapped = wrapped;
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
    CreateKeysRequest req = new CreateKeysRequest();
    req.add(new CreateKeysRequest.Key(IDC.Payload.REF, 1, attributes, mutableAttributes));
    req.setMetadata(metadata);
    return this.createKeys(req);
  }

  // Calls out to base class
  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    return this.wrapped.createKeys(request);
  }

  // Calls out to base class
  @Override
  public DeviceProfile getActiveProfile() {
    return this.wrapped.getActiveProfile();
  }

  // Calls out to base class
  @Override
  public boolean hasActiveProfile() {
    return this.wrapped.hasActiveProfile();
  }

  @Override
  public GetKeysResponse getKey(String keyId) throws IonicException {
    return this.getKey(keyId, new MetadataMap());
  }

  @Override
  public GetKeysResponse getKey(String keyId, MetadataMap metadata) throws IonicException {
    GetKeysRequest req = new GetKeysRequest();
    req.add(keyId);
    req.setMetadata(metadata);
    return this.getKeys(req);
  }

  // Calls out to base class
  @Override
  public GetKeysResponse getKeys(GetKeysRequest request) throws IonicException {
    return this.wrapped.getKeys(request);
  }

  // Calls out to base class
  @Override
  public UpdateKeysResponse updateKeys(final UpdateKeysRequest request) throws IonicException {
    return this.wrapped.updateKeys(request);
  }

  @Override
  public UpdateKeysResponse updateKey(final UpdateKeysRequest.Key key, final MetadataMap metadata)
      throws IonicException {
    UpdateKeysRequest req = new UpdateKeysRequest();
    req.addKey(key);
    req.setMetadata(metadata);
    return this.updateKeys(req);
  }
}
