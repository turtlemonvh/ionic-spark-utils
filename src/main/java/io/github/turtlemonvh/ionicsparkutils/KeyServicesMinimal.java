package io.github.turtlemonvh.ionicsparkutils;

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

/*
 * A version of the key services interface with default methods.
 * This allows wrappers to just override a small number of methods to change behavior.
 *
 * Usually the functions to override include
 *
 *      CreateKeysResponse      createKeys(CreateKeysRequest request)
 *      GetKeysResponse         getKeys(GetKeysRequest request)
 *      UpdateKeysResponse      updateKeys(final UpdateKeysRequest request)
 *
 */
interface KeyServicesMinimal extends KeyServices {

  default CreateKeysResponse createKey() throws IonicException {
    return this.createKey(new KeyAttributesMap(), new KeyAttributesMap(), new MetadataMap());
  }

  default CreateKeysResponse createKey(KeyAttributesMap attributes) throws IonicException {
    return this.createKey(attributes, new KeyAttributesMap(), new MetadataMap());
  }

  default CreateKeysResponse createKey(
      KeyAttributesMap attributes, KeyAttributesMap mutableAttributes) throws IonicException {
    return this.createKey(attributes, mutableAttributes, new MetadataMap());
  }

  default CreateKeysResponse createKey(KeyAttributesMap attributes, MetadataMap metadata)
      throws IonicException {
    return this.createKey(attributes, new KeyAttributesMap(), metadata);
  }

  default CreateKeysResponse createKey(MetadataMap metadata) throws IonicException {
    return this.createKey(new KeyAttributesMap(), new KeyAttributesMap(), metadata);
  }

  default CreateKeysResponse createKey(
      KeyAttributesMap attributes, KeyAttributesMap mutableAttributes, MetadataMap metadata)
      throws IonicException {
    CreateKeysRequest req = new CreateKeysRequest();
    req.add(new CreateKeysRequest.Key(IDC.Payload.REF, 1, attributes, mutableAttributes));
    req.setMetadata(metadata);
    return this.createKeys(req);
  }

  default GetKeysResponse getKey(String keyId) throws IonicException {
    return this.getKey(keyId, new MetadataMap());
  }

  default GetKeysResponse getKey(String keyId, MetadataMap metadata) throws IonicException {
    GetKeysRequest req = new GetKeysRequest();
    req.add(keyId);
    req.setMetadata(metadata);
    return this.getKeys(req);
  }

  default UpdateKeysResponse updateKey(final UpdateKeysRequest.Key key, final MetadataMap metadata)
      throws IonicException {
    UpdateKeysRequest req = new UpdateKeysRequest();
    req.addKey(key);
    req.setMetadata(metadata);
    return this.updateKeys(req);
  }
}
