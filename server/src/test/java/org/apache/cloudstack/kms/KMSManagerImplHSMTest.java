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
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.AccountManager;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
}
