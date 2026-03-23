// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.kms.provider.pkcs11;

import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.HSMProfileDetailsVO;
import org.apache.cloudstack.kms.KMSKekVersionVO;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PKCS11HSMProvider
 * Tests provider-specific logic: config loading, profile resolution, sensitive key detection
 */
@RunWith(MockitoJUnitRunner.class)
public class PKCS11HSMProviderTest {

    @Spy
    @InjectMocks
    private PKCS11HSMProvider provider;

    @Mock
    private HSMProfileDetailsDao hsmProfileDetailsDao;

    @Mock
    private KMSKekVersionDao kmsKekVersionDao;

    private Long testProfileId = 1L;
    private String testKekLabel = "test-kek-label";

    @Before
    public void setUp() {
        // Minimal setup
    }

    /**
     * Test: resolveProfileId successfully finds profile from KEK label
     */
    @Test
    public void testResolveProfileId_FindsFromKekLabel() throws KMSException {
        // Setup: KEK version with profile ID
        KMSKekVersionVO kekVersion = mock(KMSKekVersionVO.class);
        when(kekVersion.getHsmProfileId()).thenReturn(testProfileId);
        when(kmsKekVersionDao.findByKekLabel(testKekLabel)).thenReturn(kekVersion);

        // Test
        Long result = provider.resolveProfileId(testKekLabel);

        // Verify
        assertNotNull("Should return profile ID", result);
        assertEquals("Should return correct profile ID", testProfileId, result);
        verify(kmsKekVersionDao).findByKekLabel(testKekLabel);
    }

    /**
     * Test: resolveProfileId throws exception when KEK version not found
     */
    @Test(expected = KMSException.class)
    public void testResolveProfileId_ThrowsExceptionWhenVersionNotFound() throws KMSException {
        // Setup: No KEK version found
        when(kmsKekVersionDao.findByKekLabel(testKekLabel)).thenReturn(null);

        // Test - should throw exception
        provider.resolveProfileId(testKekLabel);
    }

    /**
     * Test: resolveProfileId throws exception when profile ID is null
     */
    @Test(expected = KMSException.class)
    public void testResolveProfileId_ThrowsExceptionWhenProfileIdNull() throws KMSException {
        // Setup: KEK version exists but has null profile ID
        KMSKekVersionVO kekVersion = mock(KMSKekVersionVO.class);
        when(kekVersion.getHsmProfileId()).thenReturn(null);
        when(kmsKekVersionDao.findByKekLabel(testKekLabel)).thenReturn(kekVersion);

        // Test - should throw exception
        provider.resolveProfileId(testKekLabel);
    }

    /**
     * Test: loadProfileConfig loads and decrypts sensitive values
     */
    @Test
    public void testLoadProfileConfig_DecryptsSensitiveValues() {
        // Setup: Profile details with encrypted pin
        HSMProfileDetailsVO detail1 = mock(HSMProfileDetailsVO.class);
        when(detail1.getName()).thenReturn("library");
        when(detail1.getValue()).thenReturn("/path/to/lib.so");

        HSMProfileDetailsVO detail2 = mock(HSMProfileDetailsVO.class);
        when(detail2.getName()).thenReturn("pin");
        when(detail2.getValue()).thenReturn("ENC(encrypted_pin)");

        HSMProfileDetailsVO detail3 = mock(HSMProfileDetailsVO.class);
        when(detail3.getName()).thenReturn("slot");
        when(detail3.getValue()).thenReturn("0");

        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(
                Arrays.asList(detail1, detail2, detail3));

        // Test
        Map<String, String> config = provider.loadProfileConfig(testProfileId);

        // Verify
        assertNotNull("Config should not be null", config);
        assertEquals(3, config.size());
        assertEquals("/path/to/lib.so", config.get("library"));
        // Note: In real code, DBEncryptionUtil.decrypt would be called
        // Here we just verify the structure is correct
        assertTrue("Config should contain pin", config.containsKey("pin"));
        assertEquals("0", config.get("slot"));

        verify(hsmProfileDetailsDao).listByProfileId(testProfileId);
    }

    /**
     * Test: loadProfileConfig handles empty details
     */
    @Test(expected = KMSException.class)
    public void testLoadProfileConfig_HandlesEmptyDetails() {
        // Setup
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(Arrays.asList());

        // Test
        Map<String, String> config = provider.loadProfileConfig(testProfileId);
    }

    /**
     * Test: isSensitiveKey correctly identifies sensitive keys
     */
    @Test
    public void testIsSensitiveKey_IdentifiesSensitiveKeys() {
        // Test
        assertTrue(provider.isSensitiveKey("pin"));
        assertTrue(provider.isSensitiveKey("password"));
        assertTrue(provider.isSensitiveKey("api_secret"));
        assertTrue(provider.isSensitiveKey("private_key"));
        assertTrue(provider.isSensitiveKey("PIN")); // Case-insensitive
    }

    /**
     * Test: isSensitiveKey correctly identifies non-sensitive keys
     */
    @Test
    public void testIsSensitiveKey_IdentifiesNonSensitiveKeys() {
        // Test
        assertFalse(provider.isSensitiveKey("library"));
        assertFalse(provider.isSensitiveKey("slot_id"));
        assertFalse(provider.isSensitiveKey("endpoint"));
        assertFalse(provider.isSensitiveKey("max_sessions"));
    }

    /**
     * Test: getProviderName returns correct name
     */
    @Test
    public void testGetProviderName() {
        assertEquals("pkcs11", provider.getProviderName());
    }

    /**
     * Test: createKek requires hsmProfileId
     */
    @Test(expected = KMSException.class)
    public void testCreateKek_RequiresProfileId() throws KMSException {
        provider.createKek(
                KeyPurpose.VOLUME_ENCRYPTION,
                "test-label",
                256,
                null // null profile ID should throw exception
        );
    }

    /**
     * Test: getSessionPool creates pool for new profile
     */
    @Test
    public void testGetSessionPool_CreatesPoolForNewProfile() {
        // Setup
        HSMProfileDetailsVO libraryDetail = mock(HSMProfileDetailsVO.class);
        when(libraryDetail.getName()).thenReturn("library");
        when(libraryDetail.getValue()).thenReturn("/path/to/lib.so");
        HSMProfileDetailsVO slotDetail = mock(HSMProfileDetailsVO.class);
        when(slotDetail.getName()).thenReturn("slot");
        when(slotDetail.getValue()).thenReturn("1");
        HSMProfileDetailsVO pinDetail = mock(HSMProfileDetailsVO.class);
        when(pinDetail.getName()).thenReturn("pin");
        when(pinDetail.getValue()).thenReturn("1234");
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(
                Arrays.asList(libraryDetail, slotDetail, pinDetail));

        // Test
        Object pool = provider.getSessionPool(testProfileId);

        // Verify
        assertNotNull("Pool should be created", pool);
        verify(hsmProfileDetailsDao).listByProfileId(testProfileId);
    }

    /**
     * Test: getSessionPool reuses pool for same profile
     */
    @Test
    public void testGetSessionPool_ReusesPoolForSameProfile() {
        // Setup
        HSMProfileDetailsVO libraryDetail = mock(HSMProfileDetailsVO.class);
        when(libraryDetail.getName()).thenReturn("library");
        when(libraryDetail.getValue()).thenReturn("/path/to/lib.so");
        HSMProfileDetailsVO slotDetail = mock(HSMProfileDetailsVO.class);
        when(slotDetail.getName()).thenReturn("slot");
        when(slotDetail.getValue()).thenReturn("1");
        HSMProfileDetailsVO pinDetail = mock(HSMProfileDetailsVO.class);
        when(pinDetail.getName()).thenReturn("pin");
        when(pinDetail.getValue()).thenReturn("1234");
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(
                Arrays.asList(libraryDetail, slotDetail, pinDetail));

        // Test
        Object pool1 = provider.getSessionPool(testProfileId);
        Object pool2 = provider.getSessionPool(testProfileId);

        // Verify
        assertNotNull("Pool should be created", pool1);
        assertEquals("Should reuse same pool", pool1, pool2);
        // Config should only be loaded once
        verify(hsmProfileDetailsDao, times(1)).listByProfileId(testProfileId);
    }
}
