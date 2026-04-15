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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.dao.VolumeDao;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests covering key lifecycle operations in KMSManagerImpl.
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplKeyLifecycleTest {

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
    private VolumeDao volumeDao;

    @Mock
    private KMSProvider kmsProvider;

    @Before
    public void setUp() {
        doReturn(5).when(kmsManager).getOperationTimeoutSec();
        doReturn(0).when(kmsManager).getRetryCount();
        doReturn(0).when(kmsManager).getRetryDelayMs();
    }

    @Test(expected = KMSException.class)
    public void testUnwrapKey_ThrowsWhenWrappedKeyNotFound() throws KMSException {
        when(kmsWrappedKeyDao.findById(1L)).thenReturn(null);
        kmsManager.unwrapKey(1L);
    }

    @Test(expected = KMSException.class)
    public void testUnwrapKey_ThrowsWhenKmsKeyNotFound() throws KMSException {
        KMSWrappedKeyVO wrappedVO = mock(KMSWrappedKeyVO.class);
        when(wrappedVO.getKmsKeyId()).thenReturn(10L);
        when(kmsWrappedKeyDao.findById(1L)).thenReturn(wrappedVO);
        when(kmsKeyDao.findById(10L)).thenReturn(null);

        kmsManager.unwrapKey(1L);
    }

    @Test
    public void testUnwrapKey_SucceedsWithHintVersion() {
        Long wrappedKeyId = 1L;
        Long kmsKeyId = 10L;
        Long versionId = 5L;

        KMSWrappedKeyVO wrappedVO = mock(KMSWrappedKeyVO.class);
        when(wrappedVO.getKmsKeyId()).thenReturn(kmsKeyId);
        when(wrappedVO.getKekVersionId()).thenReturn(versionId);
        when(wrappedVO.getWrappedBlob()).thenReturn(new byte[]{0, 1});
        when(kmsWrappedKeyDao.findById(wrappedKeyId)).thenReturn(wrappedVO);

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(kmsKey.getAlgorithm()).thenReturn("AES/GCM/NoPadding");
        when(kmsKeyDao.findById(kmsKeyId)).thenReturn(kmsKey);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getStatus()).thenReturn(KMSKekVersionVO.Status.Active);
        when(version.getHsmProfileId()).thenReturn(20L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.findById(versionId)).thenReturn(version);

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(20L)).thenReturn(profile);

        when(kmsProvider.unwrapKey(any(WrappedKey.class), anyLong())).thenReturn(new byte[]{1, 2, 3});
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");

        byte[] result = kmsManager.unwrapKey(wrappedKeyId);

        assertNotNull(result);
        verify(kmsKekVersionDao, never()).getVersionsForDecryption(kmsKeyId);
    }

    @Test(expected = KMSException.class)
    public void testUnwrapKey_FallsBackToAllVersionsWhenNoHint() {
        KMSWrappedKeyVO wrappedVO = mock(KMSWrappedKeyVO.class);
        when(wrappedVO.getKmsKeyId()).thenReturn(10L);
        when(wrappedVO.getKekVersionId()).thenReturn(null);
        when(kmsWrappedKeyDao.findById(1L)).thenReturn(wrappedVO);
        when(kmsKeyDao.findById(10L)).thenReturn(mock(KMSKeyVO.class));

        kmsManager.unwrapKey(1L);
    }

    @Test(expected = KMSException.class)
    public void testUnwrapKey_ThrowsWhenAllVersionsFail() {
        KMSWrappedKeyVO wrappedVO = mock(KMSWrappedKeyVO.class);
        when(wrappedVO.getKmsKeyId()).thenReturn(10L);
        when(wrappedVO.getKekVersionId()).thenReturn(null);
        when(kmsWrappedKeyDao.findById(1L)).thenReturn(wrappedVO);
        when(kmsKeyDao.findById(10L)).thenReturn(mock(KMSKeyVO.class));

        kmsManager.unwrapKey(1L);
    }

    @Test(expected = KMSException.class)
    public void testGenerateVolumeKeyWithKek_ThrowsWhenKeyNull() throws KMSException {
        kmsManager.generateVolumeKeyWithKek(null, 1L);
    }

    @Test(expected = KMSException.class)
    public void testGenerateVolumeKeyWithKek_ThrowsWhenKeyDisabled() throws KMSException {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(false);
        kmsManager.generateVolumeKeyWithKek(key, 1L);
    }

    @Test(expected = KMSException.class)
    public void testGenerateVolumeKeyWithKek_ThrowsWhenWrongPurpose() throws KMSException {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.TLS_CERT);
        kmsManager.generateVolumeKeyWithKek(key, 1L);
    }

    @Test(expected = KMSException.class)
    public void testGenerateVolumeKeyWithKek_ThrowsWhenNoActiveKekVersion() throws KMSException {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(key.getId()).thenReturn(1L);
        when(kmsKekVersionDao.getActiveVersion(1L)).thenReturn(null);

        kmsManager.generateVolumeKeyWithKek(key, 1L);
    }

    @Test(expected = KMSException.class)
    public void testGenerateVolumeKeyWithKek_ThrowsWhenHsmProfileDisabled() throws KMSException {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(key.getId()).thenReturn(1L);

        KMSKekVersionVO activeVersion = mock(KMSKekVersionVO.class);
        when(activeVersion.getHsmProfileId()).thenReturn(10L);
        when(kmsKekVersionDao.getActiveVersion(1L)).thenReturn(activeVersion);

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isEnabled()).thenReturn(false);
        when(hsmProfileDao.findById(10L)).thenReturn(profile);

        kmsManager.generateVolumeKeyWithKek(key, 1L);
    }

    @Test
    public void testGenerateVolumeKeyWithKek_HappyPath() {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(key.getId()).thenReturn(1L);

        KMSKekVersionVO activeVersion = mock(KMSKekVersionVO.class);
        when(activeVersion.getHsmProfileId()).thenReturn(10L);
        when(activeVersion.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.getActiveVersion(1L)).thenReturn(activeVersion);

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isEnabled()).thenReturn(true);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(10L)).thenReturn(profile);

        WrappedKey wrappedKeyResult = mock(WrappedKey.class);
        when(wrappedKeyResult.getWrappedKeyMaterial()).thenReturn(new byte[]{1, 2, 3});
        when(wrappedKeyResult.getKekId()).thenReturn("kek-label");
        when(wrappedKeyResult.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(wrappedKeyResult.getAlgorithm()).thenReturn("AES/GCM/NoPadding");
        when(wrappedKeyResult.getProviderName()).thenReturn("database");
        when(kmsProvider.generateAndWrapDek(any(KeyPurpose.class), anyString(), anyInt(), anyLong()))
                .thenReturn(wrappedKeyResult);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");

        KMSWrappedKeyVO persisted = mock(KMSWrappedKeyVO.class);
        when(persisted.getUuid()).thenReturn("wrapped-uuid");
        when(kmsWrappedKeyDao.persist(any(KMSWrappedKeyVO.class))).thenReturn(persisted);

        WrappedKey result = kmsManager.generateVolumeKeyWithKek(key, 1L);

        assertNotNull(result);
        verify(kmsProvider).generateAndWrapDek(any(KeyPurpose.class), anyString(), anyInt(), anyLong());
        verify(kmsWrappedKeyDao).persist(any(KMSWrappedKeyVO.class));
    }

    @Test
    public void testUpdateUserKMSKey_UpdatesName() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(key.getName()).thenReturn("old-name");

        kmsManager.updateUserKMSKey(key, "new-name", null, null);

        verify(key).setName("new-name");
        verify(kmsKeyDao).update(1L, key);
    }

    @Test
    public void testUpdateUserKMSKey_NoUpdateWhenNothingChanges() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getName()).thenReturn("same-name");

        kmsManager.updateUserKMSKey(key, "same-name", null, null);

        verify(kmsKeyDao, never()).update(anyLong(), any(KMSKeyVO.class));
    }

    @Test
    public void testUpdateUserKMSKey_UpdatesDescription() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(key.getDescription()).thenReturn("old");

        kmsManager.updateUserKMSKey(key, null, "new-desc", null);

        verify(key).setDescription("new-desc");
        verify(kmsKeyDao).update(1L, key);
    }

    @Test
    public void testUpdateUserKMSKey_TogglesEnabled() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(key.isEnabled()).thenReturn(true);

        kmsManager.updateUserKMSKey(key, null, null, false);

        verify(key).setEnabled(false);
        verify(kmsKeyDao).update(1L, key);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteUserKMSKey_ThrowsWhenWrappedKeysExist() throws KMSException {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(kmsWrappedKeyDao.countByKmsKeyId(1L)).thenReturn(3L);

        kmsManager.deleteUserKMSKey(key);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteUserKMSKey_ThrowsWhenVolumesExist() throws KMSException {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(kmsWrappedKeyDao.countByKmsKeyId(1L)).thenReturn(0L);
        when(volumeDao.existsWithKmsKey(1L)).thenReturn(true);

        kmsManager.deleteUserKMSKey(key);
    }

    @Test
    public void testDeleteUserKMSKey_DeletesKekFromProviderAndRemovesKey() throws KMSException {
        Long keyId = 1L;
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(keyId);
        when(kmsWrappedKeyDao.countByKmsKeyId(keyId)).thenReturn(0L);
        when(volumeDao.existsWithKmsKey(keyId)).thenReturn(false);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getHsmProfileId()).thenReturn(10L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.listByKmsKeyId(keyId)).thenReturn(List.of(version));

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(10L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            kmsManager.deleteUserKMSKey(key);

            verify(kmsProvider).deleteKek("kek-label");
            verify(kmsKeyDao).remove(keyId);
        }
    }

    @Test
    public void testDeleteUserKMSKey_ContinuesWhenKekDeletionFails() throws KMSException {
        Long keyId = 1L;
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(keyId);
        when(kmsWrappedKeyDao.countByKmsKeyId(keyId)).thenReturn(0L);
        when(volumeDao.existsWithKmsKey(keyId)).thenReturn(false);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getHsmProfileId()).thenReturn(10L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.listByKmsKeyId(keyId)).thenReturn(List.of(version));

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(10L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
        doThrow(KMSException.kekOperationFailed("provider error")).when(kmsProvider).deleteKek(anyString());

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            kmsManager.deleteUserKMSKey(key);

            verify(kmsKeyDao).remove(keyId);
        }
    }

    @Test
    public void testDeleteKMSKeysByAccountId_ReturnsFalseWhenAccountIdIsNull() {
        assertFalse(kmsManager.deleteKMSKeysByAccountId(null));
    }

    @Test
    public void testDeleteKMSKeysByAccountId_ReturnsTrueWhenNoKeys() {
        when(kmsKeyDao.listByAccount(1L, null, null)).thenReturn(List.of());

        assertTrue(kmsManager.deleteKMSKeysByAccountId(1L));
    }

    @Test
    public void testDeleteKMSKeysByAccountId_DeletesAllKeysAndKeks() {
        Long accountId = 1L;

        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(10L);
        when(kmsKeyDao.listByAccount(accountId, null, null)).thenReturn(List.of(key));
        when(kmsKeyDao.remove(10L)).thenReturn(true);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getHsmProfileId()).thenReturn(20L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.listByKmsKeyId(10L)).thenReturn(List.of(version));

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(20L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            boolean result = kmsManager.deleteKMSKeysByAccountId(accountId);

            assertTrue(result);
            verify(kmsProvider).deleteKek("kek-label");
            verify(kmsKeyDao).remove(10L);
        }
    }

    @Test
    public void testDeleteKMSKeysByAccountId_ToleratesKekProviderFailure() {
        Long accountId = 1L;

        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(10L);
        when(kmsKeyDao.listByAccount(accountId, null, null)).thenReturn(List.of(key));
        when(kmsKeyDao.remove(10L)).thenReturn(true);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getHsmProfileId()).thenReturn(20L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.listByKmsKeyId(10L)).thenReturn(List.of(version));

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(20L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
        doThrow(new RuntimeException("provider unavailable")).when(kmsProvider).deleteKek(anyString());

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            boolean result = kmsManager.deleteKMSKeysByAccountId(accountId);

            assertTrue(result);
            verify(kmsKeyDao).remove(10L);
        }
    }
}
