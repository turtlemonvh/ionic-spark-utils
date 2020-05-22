package io.github.turtlemonvh.ionicsparkutils;

/*
A cache-like layer on top of a key services implementation. Currently set up for testing rather than performance.

TO DO:

- Configurable cache parameters
  - Use https://github.com/ben-manes/caffeine as the default
- Option to create a new cache layer, copying the state of the old cache, with some changes to settings
  - possibly regeneration by changing cache key function
- Option to cache error responses (for both external id and keyid lookups)
- Option to skip cache in each request (e.g. GetKeys(GetKeysRequest req, Boolean skipCache);) or globally.
- Option to disable each type of cache
  - disabling keycache means every create or fetch results in a call to the wrapped KeyServices
  - disabling keycreateCache means that creates always result in a call to the wrapped KeyServices
  - disabling externalIdCache means that queries by external id always result in a call to the wrapped KeyServices

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
import com.ionic.sdk.agent.key.AgentKey;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The key services cache is a generic caching wrapper that can be placed around any object
 * implementing the {@link KeyServices} interface.
 *
 * It provides caching on 3 levels.
 *
 * Key fetches by id. The key corresponding to a given key id is cached.
 * Key fetches by external id. The mapping from externalid to keyids is cached.
 * Key creates. Creates can be translated into fetch requests via a customizable cache key resolution function implementing {@link KeyCreateCacheResolver}.
 *
 * By combining these 3 types of caches, KeyServicesCache is able to serve key fetches, fetches by external id, and key creates from cache.
 * Key updates are always passed through to the wrapped object, but the responses are cached.
 *
 * Currently the individual caches are not configurable for TTL or max size. They cache forever and are unbounded.
 * As such, they are a reasonable solution for short lived operations (created in Spark transform functions), but
 * are not great for longer lived services. This will be addressed in future versions of this package.
 *
 *
 */
public class KeyServicesCache extends KeyServicesWrapper implements KeyServices, Serializable {
  public KeyServices wrapped;
  private boolean immutable;

  final Logger logger = LoggerFactory.getLogger(KeyServicesCache.class);

  /*
   * Interface for defining a custom approach for translating key creates into key fetches.
   * Return null to skip cache.
   */
  public interface KeyCreateCacheResolver {
    String toKey(AgentKey key);

    String toKey(CreateKeysRequest.Key key);
  }

  /*
   * A default implementation of KeyCreateCacheResolver which segments keys by attributes.
   * Keys with the same values of attributes and mutable attributes will share the same slot in the cache.
   */
  public class AttributesKeyCreateCacheResolver implements KeyCreateCacheResolver, Serializable {
    public String toKey(AgentKey key) {
      return Integer.toString(key.getAttributesMap().hashCode())
          + Integer.toString(key.getMutableAttributesMap().hashCode());
    }

    public String toKey(CreateKeysRequest.Key key) {
      return Integer.toString(key.getAttributesMap().hashCode())
          + Integer.toString(key.getMutableAttributesMap().hashCode());
    }
  }

  /*
   * An implementation of KeyCreateCacheResolver which returns null, so no key creates will be translated into fetches.
   */
  public class NullKeyCreateCacheResolver implements KeyCreateCacheResolver, Serializable {
    public String toKey(AgentKey key) {
      return null;
    }

    public String toKey(CreateKeysRequest.Key key) {
      return null;
    }
  }

  private KeyCreateCacheResolver keyCacheResolver;

  // FIXME: These should be swappable interface definitions (e.g caffeine) instead of
  // something that caches forever
  // Note that TTLs on each caches don't have to be synchronized. Items can expire at different
  // times
  // and everything will behave fine.

  // Cache of keyid to a serializable agentkey
  private HashMap<String, SAgentKey> keycache;
  // Map of request hash to key ids. We cache just the key id, not the full response.
  private HashMap<String, Set<String>> keycreateCache;
  // Map of external id to key ids. We cache just the key id, not the full response.
  private HashMap<String, List<String>> externalIdCache;

  // Most calls will be delegated down to the wrapped KeyServices object
  public KeyServicesCache(KeyServices wrapped) throws IonicException {
    super(wrapped);
    this.wrapped = wrapped;
    this.immutable = false;
    this.keycreateCache = new HashMap<String, Set<String>>();
    this.keycache = new HashMap<String, SAgentKey>();
    this.externalIdCache = new HashMap<String, List<String>>();
    this.keyCacheResolver = new AttributesKeyCreateCacheResolver();
  }

  // Constructor with a customized function for looking up key creates in cache
  public KeyServicesCache(KeyServices wrapped, KeyCreateCacheResolver keyCacheResolver)
      throws IonicException {
    this(wrapped);
    this.keyCacheResolver = keyCacheResolver;
  }

  // A composite class to allow us to return multiple items in response to a cache lookup
  private class KeyCreateCacheResponse implements Serializable {
    public List<String> matchedIds;
    public CreateKeysRequest.Key queryForRemainingKeys;

    KeyCreateCacheResponse(List<String> matchedIds, CreateKeysRequest.Key queryForRemainingKeys) {
      this.matchedIds = matchedIds;
      this.queryForRemainingKeys = queryForRemainingKeys;
    }
  }

  // Make this data structure immutable.
  // Any attempts to reach the underlying wrapper will result in an exception.
  public synchronized void makeImmutable() {
    this.immutable = true;
  }

  // Cache for mapping key creates into key fetches.
  // Note that no special handling is needed for external id because
  // keys with the same external id will share the same cache key.
  private synchronized void setInKeyCreateCache(AgentKey key) {
    String cachekey = this.keyCacheResolver.toKey(key);
    if (cachekey == null) return;
    if (this.keycreateCache.get(cachekey) == null) {
      this.keycreateCache.put(cachekey, new HashSet());
    }
    this.keycreateCache.get(cachekey).add(key.getId());
  }

  // Add a key to both the key create cache and the key cache
  private synchronized void addToCache(AgentKey key) {
    // Add to key create cache
    this.setInKeyCreateCache(key);
    // Add the key itself
    this.keycache.put(key.getId(), new SAgentKey(key));
  }

  private synchronized void setInExtIdCache(String externalId, List keyIds) {
    this.externalIdCache.put(externalId, keyIds);
  }

  // Get a key id from cache. Returns a random choice if multiple values match.
  // FIXME: Should allow getting some from cache and getting some upstream.
  private KeyCreateCacheResponse getFromKeyCreateCache(CreateKeysRequest.Key req)
      throws IonicException {
    String cachekey = this.keyCacheResolver.toKey(req);
    Set<String> allMatchingIds = this.keycreateCache.get(cachekey);
    Integer nFromCache = req.getQuantity();

    List<String> matchedIds = new ArrayList();
    CreateKeysRequest.Key queryForRemainingKeys =
        new CreateKeysRequest.Key(
            req.getRefId(),
            req.getQuantity(),
            req.getAttributesMap(),
            req.getMutableAttributesMap());

    // We need to create all of these
    if (allMatchingIds == null || cachekey == null) {
      // No matching item exists in the cache, or cache should be skipped
      logger.info("In getFromKeyCreateCache: No matching item exists in the cache");
      return new KeyCreateCacheResponse(matchedIds, queryForRemainingKeys);
    }

    // Split request; some are creates, some gets
    if (allMatchingIds.size() < req.getQuantity()) {
      // Query for what we don't have in cache
      queryForRemainingKeys.setQuantity(req.getQuantity() - allMatchingIds.size());
      // We'll get this many as get requests (via the cache)
      nFromCache = allMatchingIds.size();
    } else {
      // We can completely satisfy this request via GET requests
      queryForRemainingKeys.setQuantity(0);
    }

    // Pull random items from the match list
    List<String> allMatchingIdsList = new ArrayList<>(allMatchingIds);
    Collections.shuffle(allMatchingIdsList);
    for (String keyId : allMatchingIdsList) {
      if (matchedIds.size() == nFromCache) {
        break;
      }
      matchedIds.add(keyId);
    }

    logger.info(
        "In getFromKeyCreateCache: Translated "
            + matchedIds.size()
            + " creates to gets via cache and "
            + queryForRemainingKeys.getQuantity()
            + " require an additional create query.");

    return new KeyCreateCacheResponse(matchedIds, queryForRemainingKeys);
  }

  /*
   * Attempts to fulfill a createKeys request by via the following strategy:
   *
   * 1) Translating to key fetches, which may themselves be served from cache
   * 2) Passing the key create request (or a portion of the key create request) upstream
   *
   * Requests may be partly filled from the key cache, partly from upstream fetches, and partly from creates.
   * Because of this, calls to `createKeys` may in some situations result in multiple http requests (one fetch and one create).
   * Any newly fetched ot newly created keys are added to the cache.
   *
   * Any errors are aggregated onto the response object.
   */
  @Override
  public CreateKeysResponse createKeys(CreateKeysRequest request) throws IonicException {
    // A map of refId:keyId for merging fetched keys back into create response
    HashMap<String, Set<String>> refIdToKeyIds = new HashMap();

    // Translate into fetches by id
    GetKeysRequest upstreamFetchReq = new GetKeysRequest();

    // Create request for anything that cannot be translated into a fetch
    CreateKeysRequest upstreamCreateReq = new CreateKeysRequest();

    // A CreateKeysRequest.Key is like AgentKey with extra properties: refId, quantity
    for (CreateKeysRequest.Key key : request.getKeys()) {
      String refId = key.getRefId();
      refIdToKeyIds.put(refId, new HashSet());

      // We always translate into a fetch by id but returned keys will always have the same
      // external id as the request as long as key attributes are part of the cache key.
      KeyCreateCacheResponse ckcr = this.getFromKeyCreateCache(key);
      for (String keyId : ckcr.matchedIds) {
        upstreamFetchReq.add(keyId);
        refIdToKeyIds.get(refId).add(keyId);
      }
      if (ckcr.queryForRemainingKeys.getQuantity() > 0) {
        upstreamCreateReq.add(ckcr.queryForRemainingKeys);
      }
    }

    // Maybe issue fetch and create requests
    // Issue create if we are asking for at least 1 key
    CreateKeysResponse upstreamCreateResp = new CreateKeysResponse();
    if (upstreamCreateReq.getKeys().size() != 0) {
      if (this.immutable) {
        throw new IonicException(
            SdkError.ISAGENT_NOTALLOWED,
            "Attempted to call createKeys on wrapped object when KeyServicesCache is immutable");
      }
      upstreamCreateResp = this.wrapped.createKeys(upstreamCreateReq);
    }
    // Issue fetch if we are asking for at least 1 key
    // Note that getKeys may read from cache
    GetKeysResponse upstreamGetResp = new GetKeysResponse();
    if (upstreamFetchReq.getKeyIds().size() != 0) {
      upstreamGetResp = this.getKeys(upstreamFetchReq);
    }

    // An empty response object into which we will load all responses
    CreateKeysResponse response = new CreateKeysResponse();
    Integer serverErrorCode = SdkError.ISAGENT_OK;
    String serverErrorMessage = "";

    // Add keys from upstream get request.
    // Assemble using map of refId to keyId.
    Integer nReturnedViaGetQuery = 0;
    Integer nReturnedViaCreateQuery = 0;
    for (Map.Entry<String, Set<String>> entry : refIdToKeyIds.entrySet()) {
      String refId = entry.getKey();

      for (String keyId : entry.getValue()) {
        GetKeysResponse.Key fetchResponseKey = upstreamGetResp.getKey(keyId);
        CreateKeysResponse.Key createResponseKey = new CreateKeysResponse.Key();

        if (fetchResponseKey == null) {
          // Just add to the list of messages. Last error response wins.
          serverErrorCode = SdkError.ISAGENT_UNKNOWN;
          serverErrorMessage +=
              "After translating create to fetch, unexpected null value for key with keyId="
                  + keyId
                  + ", refId="
                  + refId
                  + "."
                  + "\n";
          continue;
        }

        // Copy all attributes
        SAgentKey.copyAttrs(fetchResponseKey, createResponseKey);
        createResponseKey.setRefId(refId);
        createResponseKey.setDeviceId(this.getActiveProfile().getDeviceId());

        // Add to the response
        response.add(createResponseKey);
        nReturnedViaGetQuery++;
      }
    }

    // Add keys returned via upstream create request.
    for (CreateKeysResponse.Key key : upstreamCreateResp.getKeys()) {
      response.add(key);
      nReturnedViaCreateQuery++;
      // We purposefully DO NOT add to external id cache here
      this.addToCache(key);
    }

    // Set error details, merging context from create and fetch requests
    if (upstreamCreateResp.getKeys().size() > 0) {
      // These fields only make sense in the context of a single request, so they are only set when
      // a create request is sent
      // TODO: Maybe handle JsonPayload and ServerErrorDataJson?
      response.setConversationId(upstreamCreateResp.getConversationId());
      response.setHttpResponseCode(upstreamCreateResp.getHttpResponseCode());
      // Merge in response information
      // TODO: Validate this is reasonable behavior
      serverErrorCode = Math.max(serverErrorCode, upstreamCreateResp.getServerErrorCode());
      serverErrorMessage += upstreamCreateResp.getServerErrorMessage() + "\n";
    }
    response.setServerErrorCode(serverErrorCode);
    response.setServerErrorMessage(serverErrorMessage);

    logger.info(
        "In createKeys: Returning "
            + nReturnedViaGetQuery
            + " via get query "
            + nReturnedViaCreateQuery
            + " via create query.");

    return response;
  }

  // GetKeys can pass through some fraction of the request, so some keys can come from cache and
  // others are served via a request. Only non-error responses are cached.
  public GetKeysResponse getKeys(GetKeysRequest request) throws IonicException {
    logger.info(
        "Calling getKeys: requesting "
            + request.getKeyIds().size()
            + " key ids and "
            + request.getExternalIds().size()
            + " external ids.");

    // The upstream contains any queries we couldn't serve from the cache
    GetKeysRequest upstreamRequest = new GetKeysRequest();

    // Start forming response early
    // Note: must base off the structure of the original "request"
    GetKeysResponse response = new GetKeysResponse();

    // Compile a list of all the keys we need to fetch.
    // Start with the explicit list of key ids in the request.
    // We'll expand by adding key ids from the cached external id mappings.
    List<String> keyIdsToFetch = new ArrayList<>(request.getKeyIds());

    // Queries by external id
    // These may result in an entire external id query (if no cache entry for external id) or
    // a query for some fraction of the keys if the external id maps to key ids not in cache.
    for (String externalId : request.getExternalIds()) {
      List<String> keyIds = this.externalIdCache.get(externalId);
      if (keyIds == null) {
        // No cached mapping; handle in the upstream
        upstreamRequest.addExternalId(externalId);
      } else {
        // We do have a cached association
        // Add the mapping to the response
        response.add(new GetKeysResponse.QueryResult(externalId, keyIds));

        // Add the mapped keys to the response
        for (String keyId : keyIds) {
          keyIdsToFetch.add(keyId);
        }
      }
    }

    // Queries by key id and any key ids referenced by external ids (from cache)
    for (String keyId : keyIdsToFetch) {
      SAgentKey key = this.keycache.get(keyId);
      if (key == null) {
        // Add it for the upstream
        upstreamRequest.add(keyId);
        logger.info("Key " + keyId + " not found in cache. Querying upstream.");
      } else {
        // Add from cache
        GetKeysResponse.Key respK = new GetKeysResponse.Key();
        key.copyAttrs(respK);
        respK.setDeviceId(this.getActiveProfile().getDeviceId());
        response.add(respK);
      }
    }

    // Only issue the request if we have something to ask for
    GetKeysResponse upstreamResponse = new GetKeysResponse();
    if (!(upstreamRequest.getKeyIds().size() == 0
        && upstreamRequest.getExternalIds().size() == 0)) {
      if (this.immutable) {
        throw new IonicException(
            SdkError.ISAGENT_NOTALLOWED,
            "Attempted to call getKeys on wrapped object when KeyServicesCache is immutable");
      }
      upstreamResponse = this.wrapped.getKeys(upstreamRequest);
    }

    // Anything from the server wins, so we add this second
    // This allows us to update any key that may have been returned via external id queries
    for (GetKeysResponse.Key key : upstreamResponse.getKeys()) {
      this.addToCache(key);
      response.add(key);
    }
    for (GetKeysResponse.QueryResult qr : upstreamResponse.getQueryResults()) {
      // TODO: only set if there was no limit on the number of results returned from the
      // external id query
      if (qr.getMappedIds().size() != 0) {
        // Don't cache items with no matches or errrors
        this.setInExtIdCache(qr.getKeyId(), qr.getMappedIds());
      }
      response.add(qr);
    }
    for (GetKeysResponse.IonicError err : upstreamResponse.getErrors()) {
      response.add(err);
    }

    return response;
  }

  // Update is a pass through, but the response is cached
  @Override
  public UpdateKeysResponse updateKeys(final UpdateKeysRequest request) throws IonicException {
    if (this.immutable) {
      throw new IonicException(
          SdkError.ISAGENT_NOTALLOWED,
          "Attempted to call updateKeys on wrapped object when KeyServicesCache is immutable");
    }
    UpdateKeysResponse resp = this.wrapped.updateKeys(request);
    // Add resp to cache
    for (UpdateKeysResponse.Key key : resp.getKeys()) {
      this.addToCache(key);
    }
    return resp;
  }
}
