package com.ionic.sparkutil;

import org.junit.Test;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysRequest;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysResponse;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.error.ServerError;
import com.ionic.sdk.error.SdkError;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class TestAgentTest {
  @Test
  public void testCreateKey() throws IonicException {
    TestAgent testAgent = new TestAgent();
    testAgent.createKey();
    assertTrue(testAgent.keystore.keys.size() == 1);
  }

  @Test
  public void testCreateFetchKey() throws IonicException {
    TestAgent a = new TestAgent();
    CreateKeysResponse ccr = a.createKey();
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    // We got back a key
    GetKeysResponse resp = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey = resp.getKey(createdKey.getId());
    assertTrue(fetchedKey.getId() == createdKey.getId());

    // No query result for non-external id queries
    GetKeysResponse.QueryResult qr = resp.getQueryResult(createdKey.getId());
    assertTrue(qr == null);

    // No errors for successful queries
    GetKeysResponse.IonicError err = resp.getError(createdKey.getId());
    assertTrue(err == null);

    // Keystore state looks reasonable
    assertTrue(a.keystore.keys.size() == 1);
  }

  @Test
  public void testCreateFetchExternalId() throws IonicException {
    TestAgent a = new TestAgent();

    KeyAttributesMap attrs = new KeyAttributesMap();
    ArrayList<String> externalIds = new ArrayList<String>();
    String externalId = "my-test-a";
    externalIds.add(externalId);
    attrs.put("ionic-external-id", externalIds);
    CreateKeysResponse ccr = a.createKey(attrs);
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    // We got back a key
    GetKeysRequest request = new GetKeysRequest();
    request.addExternalId(externalId);
    GetKeysResponse resp = a.getKeys(request);
    GetKeysResponse.Key fetchedKey = resp.getKey(createdKey.getId());
    assertTrue(fetchedKey.getId() == createdKey.getId());

    // We expect a query result for external id queries
    GetKeysResponse.QueryResult qr = resp.getQueryResult(externalId);
    assertFalse(qr == null);

    // No errors for successful queries
    GetKeysResponse.IonicError err = resp.getError(createdKey.getId());
    assertTrue(err == null);

    // Keystore state looks reasonable
    assertTrue(a.keystore.keys.size() == 1);
  }

  @Test
  public void testFetchKeyDNE() throws IonicException {
    TestAgent a = new TestAgent();
    GetKeysResponse resp = a.getKey("abc");
    GetKeysResponse.Key fetchedKey = resp.getKey("abc");

    // Grabbing a key with no hits returns a null object
    assertTrue(fetchedKey == null);

    // Should get an error
    List<GetKeysResponse.IonicError> errors = resp.getErrors();
    assertTrue(errors.size() == 1);

    GetKeysResponse.IonicError err = resp.getError("abc");
    assertFalse(err == null);
    assertTrue(err.getKeyId() == "abc");
    assertTrue(err.getServerError() == ServerError.PROCESSING_ERROR);
  }

  @Test
  public void testFetchExternalIdDNE() throws IonicException {
    TestAgent a = new TestAgent();

    GetKeysRequest request = new GetKeysRequest();
    String externalId = "my-test-a";
    request.addExternalId(externalId);
    GetKeysResponse resp = a.getKeys(request);

    // Query result with no members
    GetKeysResponse.QueryResult qr = resp.getQueryResult(externalId);
    assertFalse(qr == null);
    assertTrue(qr.getMappedIds().size() == 0);

    // No keys
    assertTrue(resp.getKeys().size() == 0);

    // No errors
    assertTrue(qr.getErrorCode() == 0);
    GetKeysResponse.IonicError err = resp.getError(externalId);
    assertTrue(err == null);
  }

  @Test
  public void testUpdateKey() throws IonicException {
    TestAgent a = new TestAgent();
    CreateKeysResponse ccr = a.createKey();
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    // We got back a key
    GetKeysResponse resp1 = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey1 = resp1.getKey(createdKey.getId());
    assertTrue(fetchedKey1.getId() == createdKey.getId());
    List<String> attrs_a = fetchedKey1.getMutableAttributesMap().get("a");
    assertTrue(attrs_a == null);

    // Update
    UpdateKeysRequest.Key updatedKey = new UpdateKeysRequest.Key(fetchedKey1);
    KeyAttributesMap attrs = new KeyAttributesMap();
    ArrayList<String> attrVals = new ArrayList<String>();
    attrVals.add("1");
    attrs.put("a", attrVals);
    updatedKey.setMutableAttributesMap(attrs);
    UpdateKeysResponse resp = a.updateKey(updatedKey, new MetadataMap());

    assertTrue(resp.getErrors().size() == 0);
    assertTrue(resp.getKeys().size() == 1);

    // Grab updated key
    GetKeysResponse resp2 = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey2 = resp2.getKey(createdKey.getId());
    assertTrue(fetchedKey2.getId() == createdKey.getId());

    List<String> attrs_b = fetchedKey2.getMutableAttributesMap().get("a");
    assertTrue(attrs_b.size() == 1);
    assertTrue(attrs_b.get(0) == "1");
  }

  @Test
  public void testMultipleAgentSharedKeyStore() throws IonicException {
    TestKeyStore ks = new TestKeyStore("ABCD");
    TestAgent agentA = new TestAgent(ks);
    TestAgent agentB = new TestAgent(ks);

    // Create key with agent A
    CreateKeysResponse ccr = agentA.createKey();
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    // Get key with agent B
    GetKeysResponse resp = agentB.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey = resp.getKey(createdKey.getId());

    // Ensure the ids match
    assertTrue(fetchedKey.getId() == createdKey.getId());
  }

  @Test
  public void testFaultInjectionOnCreate() throws IonicException {
    TestAgent a = new TestAgent();

    // Set client error
    // Raises exception
    a.clientErrorState = SdkError.ISAGENT_ERROR;
    IonicException thrownException = null;
    try {
      a.createKey();
    } catch (IonicException e) {
      thrownException = e;
    }
    assertTrue(thrownException.getReturnCode() == SdkError.ISAGENT_ERROR);
    a.clientErrorState = 0;

    // Set server side error
    // No exception, just set on the response object
    a.serverErrorState = ServerError.INTERNAL_ERROR;
    CreateKeysResponse ccr = a.createKey();
    assertTrue(ccr.getServerErrorCode() == ServerError.INTERNAL_ERROR);

    // Attempting to access key in the result object results in a new exception being thrown
    try {
      CreateKeysResponse.Key createdKey = ccr.getFirstKey();
    } catch (IonicException e) {
      thrownException = e;
    }
    assertTrue(thrownException.getReturnCode() == SdkError.ISAGENT_KEY_DENIED);
  }

  @Test
  public void testFaultInjectionOnFetch() throws IonicException {
    TestAgent a = new TestAgent();
    CreateKeysResponse ccr = a.createKey();
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    // Set client error
    a.clientErrorState = SdkError.ISAGENT_ERROR;

    // We got back a key, but its null (when accessed via map)
    GetKeysResponse resp1 = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey1 = resp1.getKey(createdKey.getId());
    assertTrue(fetchedKey1 == null);

    // Access via "getFirstKey", we get an exception
    IonicException thrownException = null;
    try {
      fetchedKey1 = resp1.getFirstKey();
    } catch (IonicException e) {
      thrownException = e;
    }
    assertTrue(thrownException.getReturnCode() == SdkError.ISAGENT_KEY_DENIED);

    a.clientErrorState = 0;

    // Set server error
    a.serverErrorState = ServerError.INTERNAL_ERROR;

    // We got back a key, but its null (when accessed via map)
    GetKeysResponse resp2 = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey2 = resp2.getKey(createdKey.getId());
    assertTrue(fetchedKey2 == null);

    // Access via "getFirstKey", we get an exception
    try {
      fetchedKey2 = resp2.getFirstKey();
    } catch (IonicException e) {
      thrownException = e;
    }
    assertTrue(thrownException.getReturnCode() == SdkError.ISAGENT_KEY_DENIED);
  }
}
