package com.ionic.sparkutil;

import org.junit.Test;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.getkey.GetKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;

import java.util.ArrayList;
import java.util.List;

public class TestAgentTest {
  @Test
  public void testCreateKey() throws IonicException {
    TestAgent testAgent = new TestAgent();
    testAgent.createKey();
    assert (testAgent.keys.size() == 1);
  }

  @Test
  public void testCreateFetchKey() throws IonicException {
    TestAgent a = new TestAgent();
    CreateKeysResponse ccr = a.createKey();
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();
    // https://dev.ionic.com/sdk_docs/ionic_platform_sdk/java/version_2.6.0/com/ionic/sdk/agent/request/getkey/GetKeysResponse.html
    GetKeysResponse resp = a.getKey(createdKey.getId());
    GetKeysResponse.Key fetchedKey = resp.getKey(createdKey.getId());
    assert (fetchedKey.getId() == createdKey.getId());
    assert (a.keys.size() == 1);
  }

  // public CreateKeysResponse createKey(KeyAttributesMap attributes) throws IonicException
  @Test
  public void testCreateFetchExternal() throws IonicException {
    KeyAttributesMap attrs = new KeyAttributesMap();
    ArrayList<String> externalIds = new ArrayList<String>();
    externalIds.add("my-test-a");
    attrs.put("ionic-external-id", externalIds);
    TestAgent a = new TestAgent();
    CreateKeysResponse ccr = a.createKey(attrs);
    CreateKeysResponse.Key createdKey = ccr.getFirstKey();

    GetKeysRequest request = new GetKeysRequest();
    request.addExternalId("my-test-a");
    GetKeysResponse resp = a.getKeys(request);
    GetKeysResponse.Key fetchedKey = resp.getKey(createdKey.getId());
    assert (fetchedKey.getId() == createdKey.getId());
    assert (a.keys.size() == 1);
  }
}
