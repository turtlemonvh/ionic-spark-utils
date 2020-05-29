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

import java.io.Serializable;

/**
 * A wrapper around another
 * [[https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.7.0/sdk/com/ionic/sdk/key/KeyServices.html
 * KeyServices]] object, meant for customizing behavior. This allows wrappers to just override a
 * small number of methods to change behavior. All methods not overridden pass through to the
 * wrapped KeyServices object.
 *
 * <p>Usually the functions to override include
 *
 * <p>1. `CreateKeysResponse createKeys(CreateKeysRequest request)` 1. `GetKeysResponse
 * getKeys(GetKeysRequest request)` 1. `UpdateKeysResponse updateKeys(final UpdateKeysRequest
 * request)`
 *
 * @see com.ionic.sdk.key.KeyServices
 */
public class KeyServicesWrapper implements KeyServicesMinimal, Serializable {
  public KeyServices wrapped;

  // Most calls will be delegated down to the wrapped KeyServices object
  public KeyServicesWrapper(KeyServices wrapped) throws IonicException {
    this.wrapped = wrapped;
  }

  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    return this.wrapped.createKeys(request);
  }

  public DeviceProfile getActiveProfile() {
    return this.wrapped.getActiveProfile();
  }

  public boolean hasActiveProfile() {
    return this.wrapped.hasActiveProfile();
  }

  public GetKeysResponse getKeys(GetKeysRequest request) throws IonicException {
    return this.wrapped.getKeys(request);
  }

  public UpdateKeysResponse updateKeys(final UpdateKeysRequest request) throws IonicException {
    return this.wrapped.updateKeys(request);
  }
}
