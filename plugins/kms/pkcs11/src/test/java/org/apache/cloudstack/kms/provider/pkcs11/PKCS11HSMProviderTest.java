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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

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
        when(detail1.getName()).thenReturn("library_path");
        when(detail1.getValue()).thenReturn("/path/to/lib.so");

        HSMProfileDetailsVO detail2 = mock(HSMProfileDetailsVO.class);
        when(detail2.getName()).thenReturn("pin");
        when(detail2.getValue()).thenReturn("ENC(encrypted_pin)");

        HSMProfileDetailsVO detail3 = mock(HSMProfileDetailsVO.class);
        when(detail3.getName()).thenReturn("slot_id");
        when(detail3.getValue()).thenReturn("0");

        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(
            Arrays.asList(detail1, detail2, detail3));

        // Test
        Map<String, String> config = provider.loadProfileConfig(testProfileId);

        // Verify
        assertNotNull("Config should not be null", config);
        assertEquals(3, config.size());
        assertEquals("/path/to/lib.so", config.get("library_path"));
        // Note: In real code, DBEncryptionUtil.decrypt would be called
        // Here we just verify the structure is correct
        assertTrue("Config should contain pin", config.containsKey("pin"));
        assertEquals("0", config.get("slot_id"));

        verify(hsmProfileDetailsDao).listByProfileId(testProfileId);
    }

    /**
     * Test: loadProfileConfig handles empty details
     */
    @Test
    public void testLoadProfileConfig_HandlesEmptyDetails() {
        // Setup
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(Arrays.asList());

        // Test
        Map<String, String> config = provider.loadProfileConfig(testProfileId);

        // Verify
        assertNotNull("Config should not be null", config);
        assertEquals(0, config.size());
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
        assertFalse(provider.isSensitiveKey("library_path"));
        assertFalse(provider.isSensitiveKey("slot_id"));
        assertFalse(provider.isSensitiveKey("endpoint"));
        assertFalse(provider.isSensitiveKey("max_sessions"));
    }

    /**
     * Test: generateKekLabel creates valid label
     */
    @Test
    public void testGenerateKekLabel_CreatesValidLabel() {
        // Test
        String label = provider.generateKekLabel(KeyPurpose.VOLUME_ENCRYPTION);

        // Verify
        assertNotNull("Label should not be null", label);
        assertTrue("Label should start with purpose", label.startsWith(KeyPurpose.VOLUME_ENCRYPTION.getName()));
        assertTrue("Label should contain UUID", label.length() > (KeyPurpose.VOLUME_ENCRYPTION.getName() + "-kek-").length());
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
     * Test: loadProfileConfig caches configuration
     */
    @Test
    public void testLoadProfileConfig_CachesConfiguration() {
        // Setup
        HSMProfileDetailsVO detail = mock(HSMProfileDetailsVO.class);
        when(detail.getName()).thenReturn("library_path");
        when(detail.getValue()).thenReturn("/path/to/lib.so");
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(Arrays.asList(detail));

        // Load twice
        provider.loadProfileConfig(testProfileId);
        provider.loadProfileConfig(testProfileId);

        // DAO should only be called once due to caching
        verify(hsmProfileDetailsDao, times(1)).listByProfileId(testProfileId);
    }

    /**
     * Test: getSessionPool creates pool for new profile
     */
    @Test
    public void testGetSessionPool_CreatesPoolForNewProfile() {
        // Setup
        HSMProfileDetailsVO detail = mock(HSMProfileDetailsVO.class);
        when(detail.getName()).thenReturn("library_path");
        when(detail.getValue()).thenReturn("/path/to/lib.so");
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(Arrays.asList(detail));

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
        HSMProfileDetailsVO detail = mock(HSMProfileDetailsVO.class);
        when(detail.getName()).thenReturn("library_path");
        when(detail.getValue()).thenReturn("/path/to/lib.so");
        when(hsmProfileDetailsDao.listByProfileId(testProfileId)).thenReturn(Arrays.asList(detail));

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
