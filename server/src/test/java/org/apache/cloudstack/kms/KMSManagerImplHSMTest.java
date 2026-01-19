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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Unit tests for HSM-related business logic in KMSManagerImpl
 * Tests sensitive key detection, profile resolution hierarchy, and provider matching
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplHSMTest {

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    @Mock
    private HSMProfileDao hsmProfileDao;

    @Mock
    private HSMProfileDetailsDao hsmProfileDetailsDao;

    @Mock
    private AccountManager accountManager;

    private Long testAccountId = 100L;
    private Long testZoneId = 1L;
    private String testProviderName = "pkcs11";

    /**
     * Test: isSensitiveKey correctly identifies "pin" as sensitive
     */
    @Test
    public void testIsSensitiveKey_DetectsPin() {
        boolean result = kmsManager.isSensitiveKey("pin");
        assertTrue("'pin' should be detected as sensitive", result);
    }

    /**
     * Test: isSensitiveKey correctly identifies "password" as sensitive
     */
    @Test
    public void testIsSensitiveKey_DetectsPassword() {
        boolean result = kmsManager.isSensitiveKey("password");
        assertTrue("'password' should be detected as sensitive", result);
    }

    /**
     * Test: isSensitiveKey correctly identifies keys containing "secret" as sensitive
     */
    @Test
    public void testIsSensitiveKey_DetectsSecret() {
        boolean result = kmsManager.isSensitiveKey("api_secret");
        assertTrue("'api_secret' should be detected as sensitive", result);
    }

    /**
     * Test: isSensitiveKey correctly identifies "private_key" as sensitive
     */
    @Test
    public void testIsSensitiveKey_DetectsPrivateKey() {
        boolean result = kmsManager.isSensitiveKey("private_key");
        assertTrue("'private_key' should be detected as sensitive", result);
    }

    /**
     * Test: isSensitiveKey correctly identifies non-sensitive keys
     */
    @Test
    public void testIsSensitiveKey_DoesNotDetectNonSensitive() {
        boolean result = kmsManager.isSensitiveKey("library_path");
        assertFalse("'library_path' should not be detected as sensitive", result);
    }

    /**
     * Test: isSensitiveKey is case-insensitive
     */
    @Test
    public void testIsSensitiveKey_CaseInsensitive() {
        boolean resultUpper = kmsManager.isSensitiveKey("PIN");
        boolean resultMixed = kmsManager.isSensitiveKey("Password");

        assertTrue("'PIN' (uppercase) should be detected as sensitive", resultUpper);
        assertTrue("'Password' (mixed case) should be detected as sensitive", resultMixed);
    }

    /**
     * Test: resolveHSMProfile selects user profile when available
     */
    @Test
    public void testResolveHSMProfile_SelectsUserProfile() {
        // Setup: User has a profile
        HSMProfileVO userProfile = mock(HSMProfileVO.class);
        when(userProfile.getId()).thenReturn(1L);
        when(userProfile.isEnabled()).thenReturn(true);
        when(userProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(Arrays.asList(userProfile));

        Long result = kmsManager.resolveHSMProfile(testAccountId, testZoneId, testProviderName);

        assertNotNull("Should return user profile ID", result);
        assertEquals("Should select user profile", userProfile.getId(), result.longValue());
        verify(hsmProfileDao).listByAccountId(testAccountId);
    }

    /**
     * Test: resolveHSMProfile falls back to zone admin profile when no user profile
     */
    @Test
    public void testResolveHSMProfile_FallbackToZoneAdmin() {
        // Setup: No user profile, but zone admin profile exists
        HSMProfileVO zoneProfile = mock(HSMProfileVO.class);
        when(zoneProfile.getId()).thenReturn(2L);
        when(zoneProfile.isEnabled()).thenReturn(true);
        when(zoneProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles(testZoneId)).thenReturn(Arrays.asList(zoneProfile));

        Long result = kmsManager.resolveHSMProfile(testAccountId, testZoneId, testProviderName);

        assertNotNull("Should return zone admin profile ID", result);
        assertEquals("Should select zone admin profile", zoneProfile.getId(), result.longValue());
        verify(hsmProfileDao).listByAccountId(testAccountId);
        verify(hsmProfileDao).listAdminProfiles(testZoneId);
    }

    /**
     * Test: resolveHSMProfile falls back to global admin profile when no user or zone profile
     */
    @Test
    public void testResolveHSMProfile_FallbackToGlobal() {
        // Setup: No user or zone profile, but global admin profile exists
        HSMProfileVO globalProfile = mock(HSMProfileVO.class);
        when(globalProfile.getId()).thenReturn(3L);
        when(globalProfile.isEnabled()).thenReturn(true);
        when(globalProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles(testZoneId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles()).thenReturn(Arrays.asList(globalProfile));

        Long result = kmsManager.resolveHSMProfile(testAccountId, testZoneId, testProviderName);

        assertNotNull("Should return global admin profile ID", result);
        assertEquals("Should select global admin profile", globalProfile.getId(), result.longValue());
        verify(hsmProfileDao).listByAccountId(testAccountId);
        verify(hsmProfileDao).listAdminProfiles(testZoneId);
        verify(hsmProfileDao).listAdminProfiles();
    }

    /**
     * Test: resolveHSMProfile throws exception when no profile found
     */
    @Test(expected = CloudRuntimeException.class)
    public void testResolveHSMProfile_ThrowsExceptionWhenNoneFound() {
        // Setup: No profiles at any level
        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles(testZoneId)).thenReturn(new ArrayList<>());
        when(hsmProfileDao.listAdminProfiles()).thenReturn(new ArrayList<>());

        kmsManager.resolveHSMProfile(testAccountId, testZoneId, testProviderName);
    }

    /**
     * Test: resolveHSMProfile skips disabled profiles
     */
    @Test
    public void testResolveHSMProfile_SkipsDisabledProfiles() {
        // Setup: User has disabled profile, zone has enabled profile
        HSMProfileVO disabledProfile = mock(HSMProfileVO.class);
        when(disabledProfile.isEnabled()).thenReturn(false);

        HSMProfileVO enabledZoneProfile = mock(HSMProfileVO.class);
        when(enabledZoneProfile.getId()).thenReturn(5L);
        when(enabledZoneProfile.isEnabled()).thenReturn(true);
        when(enabledZoneProfile.getProtocol()).thenReturn(testProviderName);

        when(hsmProfileDao.listByAccountId(testAccountId)).thenReturn(Arrays.asList(disabledProfile));
        when(hsmProfileDao.listAdminProfiles(testZoneId)).thenReturn(Arrays.asList(enabledZoneProfile));

        Long result = kmsManager.resolveHSMProfile(testAccountId, testZoneId, testProviderName);

        assertNotNull("Should return zone profile ID (skip disabled)", result);
        assertEquals("Should select zone profile (not disabled user profile)", enabledZoneProfile.getId(), result.longValue());
    }

    /**
     * Test: resolveHSMProfile returns null for database provider
     */
    @Test
    public void testResolveHSMProfile_ReturnsNullForDatabaseProvider() {
        Long result = kmsManager.resolveHSMProfile(testAccountId, testZoneId, "database");

        assertNull("Should return null for database provider", result);
        verify(hsmProfileDao, never()).listByAccountId(anyLong());
    }

    /**
     * Test: isProviderMatch correctly matches PKCS11 protocol
     */
    @Test
    public void testIsProviderMatch_MatchesPKCS11() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("PKCS11");

        boolean result = kmsManager.isProviderMatch(profile, "pkcs11");

        assertTrue("Should match PKCS11 (case-insensitive)", result);
    }

    /**
     * Test: isProviderMatch is case-insensitive
     */
    @Test
    public void testIsProviderMatch_MatchesDifferentCases() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("pkcs11");

        boolean resultUpper = kmsManager.isProviderMatch(profile, "PKCS11");
        boolean resultMixed = kmsManager.isProviderMatch(profile, "Pkcs11");

        assertTrue("Should match PKCS11 (uppercase)", resultUpper);
        assertTrue("Should match Pkcs11 (mixed case)", resultMixed);
    }

    /**
     * Test: createHSMProfileResponse populates details correctly
     */
    @Test
    public void testCreateHSMProfileResponse_PopulatesDetails() {
        Long profileId = 10L;

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getId()).thenReturn(profileId);
        when(profile.getUuid()).thenReturn("profile-uuid");
        when(profile.getName()).thenReturn("test-profile");
        when(profile.getProtocol()).thenReturn("PKCS11");
        when(profile.getAccountId()).thenReturn(testAccountId);
        when(profile.getVendorName()).thenReturn("TestVendor");
        when(profile.isEnabled()).thenReturn(true);
        when(profile.getCreated()).thenReturn(new java.util.Date());

        HSMProfileDetailsVO detail1 = mock(HSMProfileDetailsVO.class);
        when(detail1.getName()).thenReturn("library_path");
        when(detail1.getValue()).thenReturn("/path/to/lib.so");

        HSMProfileDetailsVO detail2 = mock(HSMProfileDetailsVO.class);
        when(detail2.getName()).thenReturn("pin");
        when(detail2.getValue()).thenReturn("ENC(encrypted_value)");

        when(hsmProfileDetailsDao.listByProfileId(profileId)).thenReturn(Arrays.asList(detail1, detail2));

        com.cloud.user.Account mockAccount = mock(com.cloud.user.Account.class);
        when(mockAccount.getUuid()).thenReturn("account-uuid");
        when(mockAccount.getAccountName()).thenReturn("testaccount");
        when(accountManager.getAccount(testAccountId)).thenReturn(mockAccount);

        HSMProfileResponse response = kmsManager.createHSMProfileResponse(profile);

        assertNotNull("Response should not be null", response);
        verify(accountManager).getAccount(testAccountId);
        verify(hsmProfileDetailsDao).listByProfileId(profileId);
    }
}
