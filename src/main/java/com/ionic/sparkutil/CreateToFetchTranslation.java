package com.ionic.sparkutil;

/*

A cache-like layer on top of a key services implementation. Currently set up for testing rather than performance.

- get keys always works the same
- create keys translates into gets using a cache
- update works until things are made immutable

To use

- create a key services object
- wrap using this wrapper
- create some keys
- call .makeImmutable on this wrapper
- subsequent key creates will be translated into key fetches

TODO

Add a variant of this that provides a more general caching layer
- allow the user to implement `requestToCacheKey`, which decides when to use the cache
    - probably via a callable class
- behavior
    - modifies always write through the cache
        - populate after modification
        - still always results in a hit on the underlying KeyServices implementation
    - reads always read through the cache
        - may hit if the key is in the cache
        - we always look for a specific key id
    - external id queries use the cache just like regular fetches
    - creates may use the cache or may write through
        - populate after create
        - cache hits here here is how we get key re-use (rather than on `get` queries)
- control all behavior based on cache keys and ttls
    - can add a fluent api which is a series of modifiers to the cache which controls the specificity of the cache keys
    - e.g. is the method (get/create) included in the cache?
    - for creates, which key are we going to resolve to use?
        - may be the most recent key under a given cache entry
            - could just hold one key under each cache slot, and evict older when a new one comes in
            - so this can't include key id
        - allow eviction based on number of uses
        - cache should usually be based on shared attributes
            - as long as you are creating another key with the same attrs, use existing
            - optionally require the same metadata too

*/

import com.ionic.sdk.key.KeyServices;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysResponse;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.error.SdkError;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class CreateToFetchTranslation extends KeyServicesWrapper
    implements KeyServices, Serializable {
  public KeyServices wrapped;
  private boolean immutable;

  // Map of request hash to key id
  // We cache just the key id, not the full response
  private HashMap<String, Set<String>> keycreateCache;

  // Most calls will be delegated down to the wrapped KeyServices object
  public CreateToFetchTranslation(KeyServices wrapped) throws IonicException {
    super(wrapped);
    this.wrapped = wrapped;
    this.immutable = false;
    this.keycreateCache = new HashMap<String, Set<String>>();
  }

  public synchronized void makeImmutable() {
    this.immutable = true;
  }

  // Function to map the request into a cache key
  // The cache key is simply a combination of the hash codes for the attributes and the mutable
  // attributes.
  private String requestToCacheKey(CreateKeysRequest.Key req) {
    return Integer.toString(req.getAttributesMap().hashCode())
        + Integer.toString(req.getMutableAttributesMap().hashCode());
  }

  // Save a single key id in the map
  // No special handling needed for external id because keys with the same external id will share
  // the same cache key
  private synchronized void setInCache(CreateKeysRequest.Key req, CreateKeysResponse.Key resp) {
    String cachekey = this.requestToCacheKey(req);
    if (this.keycreateCache.get(cachekey) == null) {
      this.keycreateCache.put(cachekey, new HashSet());
    }
    this.keycreateCache.get(cachekey).add(resp.getId());
  }

  // Get a key id or external id from the map
  // Returns a random selection if multiple values match
  private Set<String> getFromCache(CreateKeysRequest.Key req) throws IonicException {
    String cachekey = this.requestToCacheKey(req);
    Set<String> allMatchingIds = this.keycreateCache.get(cachekey);
    if (allMatchingIds == null) {
      throw new IonicException(SdkError.ISAGENT_UNKNOWN, "No matching item exists in the cache");
    }

    Random rand = new Random();
    Set<String> matchedIds = new HashSet();
    for (Integer nid = 0; nid < req.getQuantity(); nid++) {
      // Pick a random value from the set of matches
      Integer choice_idx = rand.nextInt(allMatchingIds.size());
      Integer current_idx = 0;
      for (String id : allMatchingIds) {
        if (current_idx == choice_idx) {
          matchedIds.add(id);
        }
        current_idx++;
      }
    }

    return matchedIds;
  }

  /*
   * Fetch keys either by passing request through to underlying key services implementation or by translating
   * created into fetches (when set to immutable).
   * Will raise an exception if immutable and we try to create a key with no match.
   */
  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    if (this.immutable) {
      // Build a map of refId:keyId
      HashMap<String, Set<String>> refIdToKeyIds = new HashMap();

      // Translate into fetches by id
      GetKeysRequest fetchReq = new GetKeysRequest();
      for (CreateKeysRequest.Key key : request.getKeys()) {
        String refId = key.getRefId();
        refIdToKeyIds.put(refId, new HashSet());

        // We always translate into a fetch by id, but returned keys will always have the same
        // external id as the request because their attributes are part of the cache key, and the
        // external id is an attribute.
        for (String keyid : this.getFromCache(key)) {
          fetchReq.add(keyid);
          refIdToKeyIds.get(refId).add(keyid);
        }
      }

      GetKeysResponse gkr = this.getKeys(fetchReq);
      CreateKeysResponse ckr = new CreateKeysResponse();

      // Assemble based on ref ids
      for (Map.Entry<String, Set<String>> entry : refIdToKeyIds.entrySet()) {
        String refId = entry.getKey();

        for (String keyId : entry.getValue()) {
          GetKeysResponse.Key src = gkr.getKey(keyId);
          CreateKeysResponse.Key dest = new CreateKeysResponse.Key();
          dest.setRefId(refId);
          dest.setDeviceId(this.getActiveProfile().getDeviceId());
          dest.setOrigin(this.getActiveProfile().getServer());

          if (src == null) {
            throw new IonicException(
                SdkError.ISAGENT_UNKNOWN,
                "Unexpected null value for key with keyId=" + keyId + ", refId=" + refId + ".");
          }

          // Copy all attributes
          // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/createkey/CreateKeysResponse.Key.html
          // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/getkey/GetKeysResponse.Key.html
          dest.setId(src.getId());
          dest.setKey(src.getKey());
          dest.setAttributesMap(src.getAttributesMap());
          dest.setAttributesSigBase64FromServer(src.getAttributesSigBase64FromServer());
          dest.setMutableAttributesMap(src.getMutableAttributesMap());
          dest.setMutableAttributesMapFromServer(src.getMutableAttributesMapFromServer());
          dest.setMutableAttributesSigBase64FromServer(
              src.getMutableAttributesSigBase64FromServer());
          dest.setObligationsMap(src.getObligationsMap());

          // Add to the response
          ckr.add(dest);
        }
      }

      return ckr;
    } else {
      CreateKeysResponse ckr = this.wrapped.createKeys(request);

      // Add ids of created keys to the cache
      for (CreateKeysRequest.Key req : request.getKeys()) {
        String refId = req.getRefId();
        // Add all response keys with the same ref id under the same cache key
        for (CreateKeysResponse.Key resp : ckr.getKeys()) {
          if (resp.getRefId() == refId) {
            this.setInCache(req, resp);
          }
        }
      }

      return ckr;
    }
  }

  @Override
  public UpdateKeysResponse updateKeys(final UpdateKeysRequest request) throws IonicException {
    if (!this.immutable) {
      return this.wrapped.updateKeys(request);
    } else {
      throw new IonicException(
          SdkError.ISAGENT_UNKNOWN, "Cannot update keys once wrapper has been made immutable");
    }
  }
}
