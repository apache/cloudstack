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

import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KMS key rotation logic in KMSManagerImpl
 * Tests key rotation within same HSM and cross-HSM migration
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplKeyRotationTest {

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    @Mock
    private KMSKeyDao kmsKeyDao;

    @Mock
    private KMSKekVersionDao kmsKekVersionDao;

    @Mock
    private KMSWrappedKeyDao kmsWrappedKeyDao;

    @Mock
    private HSMProfileDao hsmProfileDao;

    @Mock
    private KMSProvider kmsProvider;

    private final Long testZoneId = 1L;
    private final String testProviderName = "pkcs11";

    @Before
    public void setUp() {
    }

    /**
     * Test: rotateKek creates new KEK version in same HSM
     */
    @Test
    public void testRotateKek_SameHSM() throws Exception {
        // Setup: Rotating within same HSM
        Long oldProfileId = 10L;
        Long kmsKeyId = 1L;
        String oldKekLabel = "old-kek-label";
        String newKekLabel = "new-kek-label";

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getId()).thenReturn(kmsKeyId);
        when(kmsKey.getHsmProfileId()).thenReturn(oldProfileId);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        HSMProfileVO hsmProfile = mock(HSMProfileVO.class);
        when(hsmProfile.getId()).thenReturn(oldProfileId);
        when(hsmProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(oldProfileId)).thenReturn(hsmProfile);

        // Old version should be marked as Previous
        KMSKekVersionVO oldVersion = mock(KMSKekVersionVO.class);
        when(oldVersion.getVersionNumber()).thenReturn(1);
        when(oldVersion.getId()).thenReturn(10L);
        when(kmsKekVersionDao.getActiveVersion(kmsKeyId)).thenReturn(oldVersion);
        when(kmsKekVersionDao.listByKmsKeyId(kmsKeyId)).thenReturn(List.of(oldVersion));

        // Provider creates new KEK
        when(kmsProvider.createKek(any(KeyPurpose.class), eq(newKekLabel), anyInt(), eq(oldProfileId)))
                .thenReturn("new-kek-id");

        KMSKekVersionVO newVersion = mock(KMSKekVersionVO.class);
        when(newVersion.getVersionNumber()).thenReturn(2);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(newVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        String result = kmsManager.rotateKek(kmsKey, oldKekLabel, newKekLabel, 256, null);

        // Verify new KEK was created in same HSM
        assertNotNull(result);
        verify(kmsProvider).createKek(any(KeyPurpose.class), eq(newKekLabel), eq(256), eq(oldProfileId));

        // Verify old version marked as Previous
        verify(oldVersion).setStatus(KMSKekVersionVO.Status.Previous);
        verify(kmsKekVersionDao).update(eq(10L), eq(oldVersion));

        // Verify new version created
        ArgumentCaptor<KMSKekVersionVO> versionCaptor = ArgumentCaptor.forClass(KMSKekVersionVO.class);
        verify(kmsKekVersionDao).persist(versionCaptor.capture());
        KMSKekVersionVO createdVersion = versionCaptor.getValue();
        assertEquals(Integer.valueOf(2), createdVersion.getVersionNumber());
        assertEquals(oldProfileId, createdVersion.getHsmProfileId());
    }

    /**
     * Test: rotateKek migrates key to different HSM
     */
    @Test
    public void testRotateKek_CrossHSMMigration() throws Exception {
        // Setup: Rotating to different HSM
        Long oldProfileId = 10L;
        Long newProfileId = 20L;
        Long kmsKeyId = 1L;
        String oldKekLabel = "old-kek-label";
        String newKekLabel = "new-kek-label";

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getId()).thenReturn(kmsKeyId);
        when(kmsKey.getHsmProfileId()).thenReturn(oldProfileId);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        HSMProfileVO hsmProfile = mock(HSMProfileVO.class);
        when(hsmProfile.getId()).thenReturn(newProfileId);
        when(hsmProfile.getProtocol()).thenReturn(testProviderName);

        KMSKekVersionVO oldVersion = mock(KMSKekVersionVO.class);
        when(oldVersion.getVersionNumber()).thenReturn(1);
        when(oldVersion.getId()).thenReturn(10L);
        when(kmsKekVersionDao.getActiveVersion(kmsKeyId)).thenReturn(oldVersion);
        when(kmsKekVersionDao.listByKmsKeyId(kmsKeyId)).thenReturn(List.of(oldVersion));

        // Provider creates new KEK in different HSM
        when(kmsProvider.createKek(any(KeyPurpose.class), eq(newKekLabel), anyInt(), eq(newProfileId)))
                .thenReturn("new-kek-id");

        KMSKekVersionVO newVersion = mock(KMSKekVersionVO.class);
        when(newVersion.getVersionNumber()).thenReturn(2);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(newVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        String result = kmsManager.rotateKek(kmsKey, oldKekLabel, newKekLabel, 256, hsmProfile);

        // Verify new KEK was created in new HSM
        assertNotNull(result);
        verify(kmsProvider).createKek(any(KeyPurpose.class), eq(newKekLabel), eq(256), eq(newProfileId));

        // Verify new version created with new profile ID
        ArgumentCaptor<KMSKekVersionVO> versionCaptor = ArgumentCaptor.forClass(KMSKekVersionVO.class);
        verify(kmsKekVersionDao).persist(versionCaptor.capture());
        KMSKekVersionVO createdVersion = versionCaptor.getValue();
        assertEquals(newProfileId, createdVersion.getHsmProfileId());

        // Verify KMS key updated with new profile ID
        verify(kmsKey).setHsmProfileId(newProfileId);
        verify(kmsKeyDao).update(kmsKeyId, kmsKey);
    }

    /**
     * Test: rewrapSingleKey unwraps with old KEK and wraps with new KEK
     */
    @Test
    public void testRewrapSingleKey_UnwrapAndRewrap() throws Exception {
        // Setup
        Long wrappedKeyId = 100L;
        Long oldVersionId = 1L;
        Long newVersionId = 2L;
        Long oldProfileId = 10L;
        Long newProfileId = 20L;

        KMSWrappedKeyVO wrappedKeyVO = mock(KMSWrappedKeyVO.class);
        when(wrappedKeyVO.getId()).thenReturn(wrappedKeyId);

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        KMSKekVersionVO oldVersion = mock(KMSKekVersionVO.class);

        KMSKekVersionVO newVersion = mock(KMSKekVersionVO.class);
        when(newVersion.getId()).thenReturn(newVersionId);
        when(newVersion.getKekLabel()).thenReturn("new-kek-label");
        when(newVersion.getHsmProfileId()).thenReturn(newProfileId);

        // Mock unwrap and wrap operations
        byte[] plainDek = "plain-dek-bytes".getBytes();
        doReturn(plainDek).when(kmsManager).unwrapKey(wrappedKeyId);

        WrappedKey newWrappedKey = mock(WrappedKey.class);
        when(newWrappedKey.getWrappedKeyMaterial()).thenReturn("new-wrapped-blob".getBytes());
        when(kmsProvider.wrapKey(plainDek, KeyPurpose.VOLUME_ENCRYPTION, "new-kek-label", newProfileId))
                .thenReturn(newWrappedKey);

        kmsManager.rewrapSingleKey(wrappedKeyVO, kmsKey, newVersion, kmsProvider);

        // Verify unwrap was called
        verify(kmsManager).unwrapKey(wrappedKeyId);

        // Verify wrap was called with new profile
        verify(kmsProvider).wrapKey(plainDek, KeyPurpose.VOLUME_ENCRYPTION, "new-kek-label", newProfileId);

        // Verify wrapped key was updated
        verify(wrappedKeyVO).setKekVersionId(newVersionId);
        verify(wrappedKeyVO).setWrappedBlob("new-wrapped-blob".getBytes());
        verify(kmsWrappedKeyDao).update(wrappedKeyId, wrappedKeyVO);
    }

    /**
     * Test: rotateKek generates new label when not provided
     */
    @Test
    public void testRotateKek_GeneratesLabel() throws Exception {
        // Setup
        Long oldProfileId = 10L;
        Long kmsKeyId = 1L;
        String oldKekLabel = "old-kek-label";

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getId()).thenReturn(kmsKeyId);
        when(kmsKey.getHsmProfileId()).thenReturn(oldProfileId);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        HSMProfileVO hsmProfile = mock(HSMProfileVO.class);
        when(hsmProfile.getId()).thenReturn(oldProfileId);
        when(hsmProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(oldProfileId)).thenReturn(hsmProfile);

        KMSKekVersionVO oldVersion = mock(KMSKekVersionVO.class);
        when(oldVersion.getVersionNumber()).thenReturn(1);
        when(oldVersion.getId()).thenReturn(10L);
        when(kmsKekVersionDao.getActiveVersion(kmsKeyId)).thenReturn(oldVersion);
        when(kmsKekVersionDao.listByKmsKeyId(kmsKeyId)).thenReturn(List.of(oldVersion));

        // Provider creates new KEK - will accept any label
        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(oldProfileId)))
                .thenReturn("new-kek-id");

        KMSKekVersionVO newVersion = mock(KMSKekVersionVO.class);
        when(newVersion.getVersionNumber()).thenReturn(2);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(newVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        kmsManager.rotateKek(kmsKey, oldKekLabel, null, 256, null);

        // Verify a label was generated and passed to createKek
        ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);
        verify(kmsProvider).createKek(any(KeyPurpose.class), labelCaptor.capture(), eq(256), eq(oldProfileId));
        String generatedLabel = labelCaptor.getValue();
        assertNotNull("Label should be generated", generatedLabel);
    }

    /**
     * Test: rotateKek throws exception when old KEK not found (provider rejects the rotation)
     */
    @Test(expected = KMSException.class)
    public void testRotateKek_ThrowsExceptionWhenOldKekNotFound() throws KMSException {
        Long oldProfileId = 10L;
        Long kmsKeyId = 1L;

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getHsmProfileId()).thenReturn(oldProfileId);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        HSMProfileVO hsmProfile = mock(HSMProfileVO.class);
        when(hsmProfile.getId()).thenReturn(oldProfileId);
        when(hsmProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(oldProfileId)).thenReturn(hsmProfile);

        // Provider throws because the old KEK label doesn't exist in the HSM
        when(kmsProvider.createKek(any(KeyPurpose.class), eq("new-label"), eq(256), eq(oldProfileId)))
                .thenThrow(KMSException.kekNotFound("Old KEK not found: non-existent-label"));

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        kmsManager.rotateKek(kmsKey, "non-existent-label", "new-label", 256, null);
    }

    /**
     * Test: rotateKek uses current profile when target profile is null
     */
    @Test
    public void testRotateKek_UsesCurrentProfileWhenTargetNull() throws Exception {
        // Setup
        Long currentProfileId = 10L;
        Long kmsKeyId = 1L;
        String oldKekLabel = "old-kek-label";

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getId()).thenReturn(kmsKeyId);
        when(kmsKey.getHsmProfileId()).thenReturn(currentProfileId);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);

        HSMProfileVO hsmProfile = mock(HSMProfileVO.class);
        when(hsmProfile.getId()).thenReturn(currentProfileId);
        when(hsmProfile.getProtocol()).thenReturn(testProviderName);
        when(hsmProfileDao.findById(currentProfileId)).thenReturn(hsmProfile);

        KMSKekVersionVO oldVersion = mock(KMSKekVersionVO.class);
        when(oldVersion.getVersionNumber()).thenReturn(1);
        when(oldVersion.getId()).thenReturn(10L);
        when(kmsKekVersionDao.getActiveVersion(kmsKeyId)).thenReturn(oldVersion);
        when(kmsKekVersionDao.listByKmsKeyId(kmsKeyId)).thenReturn(List.of(oldVersion));

        when(kmsProvider.createKek(any(KeyPurpose.class), anyString(), anyInt(), eq(currentProfileId)))
                .thenReturn("new-kek-id");

        KMSKekVersionVO newVersion = mock(KMSKekVersionVO.class);
        when(newVersion.getVersionNumber()).thenReturn(2);
        when(kmsKekVersionDao.persist(any(KMSKekVersionVO.class))).thenReturn(newVersion);

        doReturn(kmsProvider).when(kmsManager).getKMSProvider(testProviderName);

        kmsManager.rotateKek(kmsKey, oldKekLabel, "new-label", 256, null);

        // Verify current profile was used (not a different one)
        verify(kmsProvider).createKek(any(KeyPurpose.class), anyString(), eq(256), eq(currentProfileId));

        // Verify KMS key was not updated (same profile)
        verify(kmsKey, never()).setHsmProfileId(currentProfileId);
        verify(kmsKeyDao, never()).update(kmsKeyId, kmsKey);
    }
}
