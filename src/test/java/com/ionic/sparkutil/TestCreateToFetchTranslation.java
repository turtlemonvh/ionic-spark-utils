package com.ionic.sparkutil;

import org.junit.Test;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.key.KeyServices;

import static org.junit.Assert.*;

public class TestCreateToFetchTranslation {
  @Test
  public void testCreateKey() throws IonicException {
    TestAgent a = new TestAgent();
    CreateToFetchTranslation wrapped = new CreateToFetchTranslation(a);

    CreateKeysResponse ccr1 = wrapped.createKey();
    assertEquals(((TestKeyStore) a.keystore).keys.size(), 1);
    String firstId = ccr1.getFirstKey().getId();

    wrapped.makeImmutable();

    // Creates shoul result in a fetch of the same key
    CreateKeysResponse ccr2 = wrapped.createKey();
    assertEquals(((TestKeyStore) a.keystore).keys.size(), 1);
    assertEquals(ccr2.getFirstKey().getId(), firstId);
  }
}
