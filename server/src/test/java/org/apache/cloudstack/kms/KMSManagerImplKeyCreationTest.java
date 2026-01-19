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

package org.apache.cloudstack.kms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.ArrayList;

import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for KMS key creation logic in KMSManagerImpl
 * Tests key creation with explicit and auto-resolved HSM profiles
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplKeyCreationTest {

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    @Mock
    private KMSKeyDao kmsKeyDao;

    @Mock
    private KMSKekVersionDao kmsKekVersionDao;

    @Mock
    private HSMProfileDao hsmProfileDao;

    @Mock
    private KMSProvider kmsProvider;

    private Long testAccountId = 100L;
    private Long testDomainId = 1L;
    private Long testZoneId = 1L;
    private String testProviderName = "pkcs11";

    @Before
    public void setUp() {
        // Setup provider
        when(kmsProvider.getProviderName()).thenReturn(testProviderName);
    }

    /**
     * Test: createUserKMSKey uses explicit HSM profile when provided
     */
    @Test
    public void testCreateUserKMSKey_WithExplicitProfile() throws Exception {
        // Setup: Explicit profile name provided
        String hsmProfileName = "user-hsm-profile";
        Long hsmProfileId = 10L;

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getId()).thenReturn(hsmProfileId);
        when(profile.getAccountId()).thenReturn(testAccountId);
        when(hsmProfileDao.findByName(hsmProfileName)).thenReturn(profile);

        // Mock provider KEK creation
        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(hsmProfileId)))
            .thenReturn("test-kek-label");

        // Mock DAO persist operations
        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        // Mock getKMSProviderForZone to return our mock provider
        doReturn(kmsProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        KMSKey result = kmsManager.createUserKMSKey(testAccountId, testDomainId,
            testZoneId, "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, hsmProfileName);

        // Verify explicit profile was used
        assertNotNull(result);
        verify(hsmProfileDao).findByName(hsmProfileName);
        verify(kmsProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(hsmProfileId));

        // Verify KMSKeyVO was created with correct profile ID
        ArgumentCaptor<KMSKeyVO> keyCaptor = ArgumentCaptor.forClass(KMSKeyVO.class);
        verify(kmsKeyDao).persist(keyCaptor.capture());
        KMSKeyVO createdKey = keyCaptor.getValue();
        assertEquals(hsmProfileId, createdKey.getHsmProfileId());
    }

    /**
     * Test: createUserKMSKey auto-resolves profile when not provided
     */
    @Test
    public void testCreateUserKMSKey_AutoResolvesProfile() throws Exception {
        // Setup: No explicit profile name, should auto-resolve
        Long autoResolvedProfileId = 20L;

        // Mock profile resolution hierarchy - user has a profile
        HSMProfileVO userProfile = mock(HSMProfileVO.class);
        when(userProfile.getId()).thenReturn(autoResolvedProfileId);
        when(userProfile.isEnabled()).thenReturn(true);
        when(userProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(Arrays.asList(userProfile));

        // Mock provider KEK creation
        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(autoResolvedProfileId)))
            .thenReturn("test-kek-label");

        // Mock DAO persist operations
        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        // Mock getKMSProviderForZone
        doReturn(kmsProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        KMSKey result = kmsManager.createUserKMSKey(testAccountId, testDomainId,
            testZoneId, "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, null);

        // Verify profile was auto-resolved
        assertNotNull(result);
        verify(hsmProfileDao).listByAccountId(testAccountId);
        verify(kmsProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(autoResolvedProfileId));

        // Verify KMSKeyVO was created with auto-resolved profile ID
        ArgumentCaptor<KMSKeyVO> keyCaptor = ArgumentCaptor.forClass(KMSKeyVO.class);
        verify(kmsKeyDao).persist(keyCaptor.capture());
        KMSKeyVO createdKey = keyCaptor.getValue();
        assertEquals(autoResolvedProfileId, createdKey.getHsmProfileId());
    }

    /**
     * Test: createUserKMSKey throws exception when explicit profile not found
     */
    @Test(expected = KMSException.class)
    public void testCreateUserKMSKey_ThrowsExceptionWhenProfileNotFound() throws KMSException {
        // Setup: Profile name provided but doesn't exist
        String invalidProfileName = "non-existent-profile";
        when(hsmProfileDao.findByName(invalidProfileName)).thenReturn(null);

        doReturn(kmsProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        kmsManager.createUserKMSKey(testAccountId, testDomainId, testZoneId,
            "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, invalidProfileName);
    }

    /**
     * Test: createUserKMSKey auto-resolves to zone admin profile when no user profile
     */
    @Test
    public void testCreateUserKMSKey_AutoResolvesToZoneAdmin() throws Exception {
        // Setup: No user profile, but zone admin profile exists
        Long zoneAdminProfileId = 30L;

        HSMProfileVO zoneProfile = mock(HSMProfileVO.class);
        when(zoneProfile.getId()).thenReturn(zoneAdminProfileId);
        when(zoneProfile.isEnabled()).thenReturn(true);
        when(zoneProfile.getProtocol()).thenReturn(testProviderName);

        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles(testZoneId)).thenReturn(Arrays.asList(zoneProfile));

        // Mock provider KEK creation
        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(zoneAdminProfileId)))
            .thenReturn("test-kek-label");

        // Mock DAO persist operations
        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        KMSKey result = kmsManager.createUserKMSKey(testAccountId, testDomainId,
            testZoneId, "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, null);

        // Verify zone admin profile was used
        assertNotNull(result);
        verify(hsmProfileDao).listByAccountId(testAccountId);
        verify(hsmProfileDao).listAdminProfiles(testZoneId);
        verify(kmsProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(zoneAdminProfileId));

        // Verify KMSKeyVO was created with zone admin profile ID
        ArgumentCaptor<KMSKeyVO> keyCaptor = ArgumentCaptor.forClass(KMSKeyVO.class);
        verify(kmsKeyDao).persist(keyCaptor.capture());
        assertEquals(zoneAdminProfileId, keyCaptor.getValue().getHsmProfileId());
    }

    /**
     * Test: createUserKMSKey creates KEK version with correct profile ID
     */
    @Test
    public void testCreateUserKMSKey_CreatesKekVersionWithProfileId() throws Exception {
        // Setup
        Long hsmProfileId = 40L;

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getId()).thenReturn(hsmProfileId);
        when(profile.isEnabled()).thenReturn(true);
        when(profile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(Arrays.asList(profile));

        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(hsmProfileId)))
            .thenReturn("test-kek-label");

        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        kmsManager.createUserKMSKey(testAccountId, testDomainId, testZoneId,
            "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, null);

        // Verify KEK version was created with correct profile ID
        ArgumentCaptor<KMSKekVersionVO> versionCaptor = ArgumentCaptor.forClass(KMSKekVersionVO.class);
        verify(kmsKekVersionDao).persist(versionCaptor.capture());
        KMSKekVersionVO createdVersion = versionCaptor.getValue();
        assertEquals(hsmProfileId, createdVersion.getHsmProfileId());
        assertEquals(Integer.valueOf(1), Integer.valueOf(createdVersion.getVersionNumber()));
        assertEquals("test-kek-label", createdVersion.getKekLabel());
    }

    /**
     * Test: createUserKMSKey returns null profile ID for database provider
     */
    @Test
    public void testCreateUserKMSKey_NullProfileIdForDatabaseProvider() throws Exception {
        // Setup: Database provider doesn't use profiles
        KMSProvider databaseProvider = mock(KMSProvider.class);
        when(databaseProvider.getProviderName()).thenReturn("database");
        when(databaseProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(null)))
            .thenReturn("test-kek-label");

        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        doReturn(databaseProvider).when(kmsManager).getKMSProviderForZone(testZoneId);
        doReturn(true).when(kmsManager).isKmsEnabled(testZoneId);

        kmsManager.createUserKMSKey(testAccountId, testDomainId, testZoneId,
            "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, null);

        // Verify KEK was created with null profile ID
        verify(databaseProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(null));

        // Verify KMSKeyVO has null profile ID
        ArgumentCaptor<KMSKeyVO> keyCaptor = ArgumentCaptor.forClass(KMSKeyVO.class);
        verify(kmsKeyDao).persist(keyCaptor.capture());
        assertEquals(null, keyCaptor.getValue().getHsmProfileId());
    }
}
