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

import com.cloud.event.ActionEventUtils;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

/**
 * Unit tests for KMS key creation logic in KMSManagerImpl
 * Tests key creation with explicit and auto-resolved HSM profiles
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplKeyCreationTest {

    private final Long testAccountId = 100L;
    private final Long testDomainId = 1L;
    private final Long testZoneId = 1L;
    private final String testProviderName = "pkcs11";
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

    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kms-test");
            t.setDaemon(true);
            return t;
        });
        ReflectionTestUtils.setField(kmsManager, "kmsOperationExecutor", executor);
        doReturn(5).when(kmsManager).getOperationTimeoutSec();
        doReturn(0).when(kmsManager).getRetryCount();
        doReturn(0).when(kmsManager).getRetryDelayMs();
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
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
        when(profile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(hsmProfileId)).thenReturn(profile);

        // Mock provider KEK creation
        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(hsmProfileId)))
                .thenReturn("test-kek-label");

        // Mock DAO persist operations
        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            actionEventUtils.when(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyInt())).thenReturn(2L);
            KMSKey result = kmsManager.createUserKMSKey(testAccountId, testDomainId,
                    testZoneId, "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, hsmProfileId);

            // Verify explicit profile was used
            assertNotNull(result);
            verify(hsmProfileDao).findById(hsmProfileId);
            verify(kmsProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(hsmProfileId));

            // Verify KMSKeyVO was created with correct profile ID
            ArgumentCaptor<KMSKeyVO> keyCaptor = ArgumentCaptor.forClass(KMSKeyVO.class);
            verify(kmsKeyDao).persist(keyCaptor.capture());
            KMSKeyVO createdKey = keyCaptor.getValue();
            assertEquals(hsmProfileId, createdKey.getHsmProfileId());
        }
    }

    /**
     * Test: createUserKMSKey throws exception when explicit profile not found
     */
    @Test(expected = KMSException.class)
    public void testCreateUserKMSKey_ThrowsExceptionWhenProfileNotFound() throws KMSException {
        // Setup: Profile name provided but doesn't exist
        String invalidProfileName = "non-existent-profile";
        long hsmProfileId = 1L;
        when(hsmProfileDao.findById(hsmProfileId)).thenReturn(null);

        kmsManager.createUserKMSKey(testAccountId, testDomainId, testZoneId,
                "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, hsmProfileId);
    }

    /**
     * Test: createUserKMSKey creates KEK version with correct profile ID
     */
    @Test
    public void testCreateUserKMSKey_CreatesKekVersionWithProfileId() throws Exception {
        // Setup
        Long hsmProfileId = 40L;

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(hsmProfileId)).thenReturn(profile);

        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(hsmProfileId)))
                .thenReturn("test-kek-label");

        KMSKeyVO mockKey = mock(KMSKeyVO.class);
        when(mockKey.getId()).thenReturn(1L);
        when(kmsKeyDao.persist(any(KMSKeyVO.class))).thenReturn(mockKey);

        KMSKekVersionVO mockVersion = mock(KMSKekVersionVO.class);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(mockVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            actionEventUtils.when(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyInt())).thenReturn(2L);

            kmsManager.createUserKMSKey(testAccountId, testDomainId, testZoneId,
                    "test-key", "Test key", KeyPurpose.VOLUME_ENCRYPTION, 256, hsmProfileId);

            // Verify KEK version was created with correct profile ID
            ArgumentCaptor<KMSKekVersionVO> versionCaptor = ArgumentCaptor.forClass(KMSKekVersionVO.class);
            verify(kmsKekVersionDao).persist(versionCaptor.capture());
            KMSKekVersionVO createdVersion = versionCaptor.getValue();
            assertEquals(hsmProfileId, createdVersion.getHsmProfileId());
            assertEquals(Integer.valueOf(1), createdVersion.getVersionNumber());
            assertEquals("test-kek-label", createdVersion.getKekLabel());
        }
    }
}
