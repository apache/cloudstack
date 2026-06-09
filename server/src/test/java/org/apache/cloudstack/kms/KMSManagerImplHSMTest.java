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

import com.cloud.api.ApiResponseHelper;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.api.command.user.kms.hsm.DeleteHSMProfileCmd;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HSM-related business logic in KMSManagerImpl
 * Tests sensitive key detection, profile resolution hierarchy, and provider matching
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplHSMTest {

    private final Long testAccountId = 100L;

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    @Mock
    private HSMProfileDao hsmProfileDao;

    @Mock
    private HSMProfileDetailsDao hsmProfileDetailsDao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private DataCenterDao dataCenterDao;

    @Mock
    private DomainDao domainDao;

    @Mock
    private AccountDao accountDao;

    @Mock
    private KMSKeyDao kmsKeyDao;

    @Mock
    private KMSKekVersionDao kmsKekVersionDao;

    @Mock
    private KMSWrappedKeyDao kmsWrappedKeyDao;

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

        try (MockedStatic<ApiResponseHelper> mockedApiResponseHelper = Mockito.mockStatic(ApiResponseHelper.class)) {
            HSMProfileResponse response = kmsManager.createHSMProfileResponse(profile);

            assertNotNull("Response should not be null", response);
            verify(hsmProfileDetailsDao).listByProfileId(profileId);
        }
    }

    /**
     * Test: the seeded default (system-owned) database profile cannot be deleted.
     */
    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteHSMProfile_RejectsSystemOwnedDatabaseProfile() {
        Long profileId = 10L;
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(profile.getAccountId()).thenReturn(Account.ACCOUNT_ID_SYSTEM);
        when(profile.getIsPublic()).thenReturn(true);
        when(hsmProfileDao.findById(profileId)).thenReturn(profile);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(accountManager.isRootAdmin(2L)).thenReturn(true);

        DeleteHSMProfileCmd cmd = mock(DeleteHSMProfileCmd.class);
        when(cmd.getId()).thenReturn(profileId);

        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            CallContext callContext = mock(CallContext.class);
            mockedCallContext.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingAccount()).thenReturn(caller);

            kmsManager.deleteHSMProfile(cmd);
        }
    }

    /**
     * Test: an admin-created (non-system) database profile with no keys can be deleted.
     */
    @Test
    public void testDeleteHSMProfile_AllowsAdminOwnedDatabaseProfile() {
        Long profileId = 10L;
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getId()).thenReturn(profileId);
        when(profile.getProtocol()).thenReturn("database");
        when(profile.getAccountId()).thenReturn(200L);
        when(profile.getIsPublic()).thenReturn(false);
        when(hsmProfileDao.findById(profileId)).thenReturn(profile);

        when(kmsKeyDao.countByHsmProfileId(profileId)).thenReturn(0L);
        when(kmsKekVersionDao.listByHsmProfileId(profileId)).thenReturn(Collections.emptyList());

        KMSProvider kmsProvider = mock(KMSProvider.class);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
        when(hsmProfileDao.remove(profileId)).thenReturn(true);

        Account caller = mock(Account.class);

        DeleteHSMProfileCmd cmd = mock(DeleteHSMProfileCmd.class);
        when(cmd.getId()).thenReturn(profileId);

        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            CallContext callContext = mock(CallContext.class);
            mockedCallContext.when(CallContext::current).thenReturn(callContext);
            when(callContext.getCallingAccount()).thenReturn(caller);

            boolean result = kmsManager.deleteHSMProfile(cmd);

            assertTrue("Admin-owned database profile should be deletable", result);
            verify(kmsProvider).invalidateProfileCache(profileId);
            verify(hsmProfileDao).remove(profileId);
        }
    }

    private HSMProfileVO domainScopedProfile(long domainId) {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getIsPublic()).thenReturn(false);
        when(profile.getAccountId()).thenReturn(-1L);
        when(profile.getDomainId()).thenReturn(domainId);
        return profile;
    }

    /**
     * Test: a non-admin caller in the same domain may use a domain-scoped profile.
     */
    @Test
    public void testCheckHSMProfileAccess_DomainScoped_AllowsUseBySameDomain() {
        HSMProfileVO profile = domainScopedProfile(5L);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(caller.getDomainId()).thenReturn(5L);
        when(accountManager.isRootAdmin(2L)).thenReturn(false);

        kmsManager.checkHSMProfileAccess(caller, profile, false);

        verify(accountManager, Mockito.never()).checkAccess(Mockito.any(Account.class), Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }

    /**
     * Test: a non-admin caller in a different domain is denied use of a domain-scoped profile.
     */
    @Test(expected = PermissionDeniedException.class)
    public void testCheckHSMProfileAccess_DomainScoped_DeniesUseByOtherDomain() {
        HSMProfileVO profile = domainScopedProfile(5L);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(caller.getDomainId()).thenReturn(7L);
        when(accountManager.isRootAdmin(2L)).thenReturn(false);

        kmsManager.checkHSMProfileAccess(caller, profile, false);
    }

    /**
     * Test: a root admin may use a domain-scoped profile from any domain.
     */
    @Test
    public void testCheckHSMProfileAccess_DomainScoped_AllowsRootAdminFromAnyDomain() {
        HSMProfileVO profile = domainScopedProfile(5L);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(accountManager.isRootAdmin(2L)).thenReturn(true);

        kmsManager.checkHSMProfileAccess(caller, profile, false);
    }

    /**
     * Test: a non-admin caller in the same domain may NOT modify a domain-scoped profile.
     */
    @Test(expected = PermissionDeniedException.class)
    public void testCheckHSMProfileAccess_DomainScoped_DeniesModifyByNonAdmin() {
        HSMProfileVO profile = domainScopedProfile(5L);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(accountManager.isRootAdmin(2L)).thenReturn(false);

        kmsManager.checkHSMProfileAccess(caller, profile, true);
    }

    /**
     * Test: a root admin may modify a domain-scoped profile.
     */
    @Test
    public void testCheckHSMProfileAccess_DomainScoped_AllowsModifyByRootAdmin() {
        HSMProfileVO profile = domainScopedProfile(5L);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(2L);
        when(accountManager.isRootAdmin(2L)).thenReturn(true);

        kmsManager.checkHSMProfileAccess(caller, profile, true);
    }

    /**
     * Test: createHSMProfileResponse for a domain-scoped (account-less) profile populates the
     * domain fields directly and does not route through populateOwner (which would NPE on a null owner).
     */
    @Test
    public void testCreateHSMProfileResponse_DomainScoped_PopulatesDomainFields() {
        Long profileId = 11L;
        Long domainId = 5L;

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getId()).thenReturn(profileId);
        when(profile.getUuid()).thenReturn("profile-uuid");
        when(profile.getName()).thenReturn("domain-profile");
        when(profile.getProtocol()).thenReturn("PKCS11");
        when(profile.getVendorName()).thenReturn("TestVendor");
        when(profile.isEnabled()).thenReturn(true);
        when(profile.getCreated()).thenReturn(new java.util.Date());
        when(profile.getAccountId()).thenReturn(-1L);
        when(profile.getDomainId()).thenReturn(domainId);

        DomainVO domain = mock(DomainVO.class);
        when(domain.getUuid()).thenReturn("domain-uuid");
        when(domain.getName()).thenReturn("D");
        when(domain.getPath()).thenReturn("/D/");
        when(domainDao.findById(domainId)).thenReturn(domain);

        when(hsmProfileDetailsDao.listByProfileId(profileId)).thenReturn(Collections.emptyList());

        try (MockedStatic<ApiResponseHelper> mockedApiResponseHelper = Mockito.mockStatic(ApiResponseHelper.class)) {
            HSMProfileResponse response = kmsManager.createHSMProfileResponse(profile);

            assertNotNull("Response should not be null", response);
            mockedApiResponseHelper.verify(() -> ApiResponseHelper.populateOwner(Mockito.any(), Mockito.any()), Mockito.never());
        }
    }
}
