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
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private SnapshotDao snapshotDao;

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
        mockWrappedKey(5L, "AES/CBC/PKCS5Padding");
        mockDatabaseProfile();
        KMSKekVersionVO version = mockVersion(5L, 1, KMSKekVersionVO.Status.Active);
        when(kmsKekVersionDao.findById(5L)).thenReturn(version);
        when(kmsProvider.unwrapKey(any(WrappedKey.class), anyLong())).thenReturn(new byte[32]);

        byte[] result = kmsManager.unwrapKey(1L);

        assertNotNull(result);
        // The blob is unwrapped with the algorithm recorded with it
        ArgumentCaptor<WrappedKey> wrappedKeyCaptor = ArgumentCaptor.forClass(WrappedKey.class);
        verify(kmsProvider).unwrapKey(wrappedKeyCaptor.capture(), anyLong());
        assertEquals("AES/CBC/PKCS5Padding", wrappedKeyCaptor.getValue().getAlgorithm());
        verify(kmsKekVersionDao, never()).getVersionsForDecryption(10L);
    }

    @Test
    public void testUnwrapKey_NoFallbackForCbcWhenRecordedVersionFails() {
        mockWrappedKey(5L, "AES/CBC/PKCS5Padding");
        mockDatabaseProfile();
        KMSKekVersionVO version = mockVersion(5L, 1, KMSKekVersionVO.Status.Previous);
        when(kmsKekVersionDao.findById(5L)).thenReturn(version);
        when(kmsProvider.unwrapKey(any(WrappedKey.class), anyLong())).thenThrow(KMSException.wrapUnwrapFailed("Failed to unwrap key"));

        assertThrows(KMSException.class, () -> kmsManager.unwrapKey(1L));

        // Another KEK could unwrap a CBC blob to a wrong key without an error
        verify(kmsKekVersionDao, never()).getVersionsForDecryption(anyLong());
        verify(kmsProvider).unwrapKey(any(WrappedKey.class), anyLong());
    }

    @Test
    public void testUnwrapKey_FallsBackToOtherVersionsForAuthenticatedAlgorithm() {
        mockWrappedKey(5L, "AES/GCM/NoPadding");
        mockDatabaseProfile();
        KMSKekVersionVO recordedVersion = mockVersion(5L, 1, KMSKekVersionVO.Status.Previous);
        when(kmsKekVersionDao.findById(5L)).thenReturn(recordedVersion);
        KMSKekVersionVO activeVersion = mockVersion(6L, 2, KMSKekVersionVO.Status.Active);
        when(kmsKekVersionDao.getVersionsForDecryption(10L)).thenReturn(List.of(recordedVersion, activeVersion));
        when(kmsProvider.unwrapKey(any(WrappedKey.class), anyLong()))
                .thenThrow(KMSException.wrapUnwrapFailed("Failed to unwrap key"))
                .thenReturn(new byte[32]);

        byte[] result = kmsManager.unwrapKey(1L);

        assertEquals(32, result.length);
        // The recorded version and then the active one; the recorded version isn't tried twice
        verify(kmsProvider, times(2)).unwrapKey(any(WrappedKey.class), anyLong());
    }

    @Test
    public void testUnwrapKey_ThrowsForCbcWithoutRecordedVersion() {
        mockWrappedKey(null, "AES/CBC/PKCS5Padding");

        KMSException e = assertThrows(KMSException.class, () -> kmsManager.unwrapKey(1L));

        // Nothing could be tried, so the key counts as gone
        assertEquals(KMSException.ErrorType.KEK_NOT_FOUND, e.getErrorType());
        verify(kmsKekVersionDao, never()).getVersionsForDecryption(anyLong());
    }

    @Test
    public void testUnwrapKey_ThrowsKekNotFoundForArchivedVersion() {
        mockWrappedKey(5L, "AES/GCM/NoPadding");
        KMSKekVersionVO version = mockVersion(5L, 1, KMSKekVersionVO.Status.Archived);
        when(kmsKekVersionDao.findById(5L)).thenReturn(version);

        KMSException e = assertThrows(KMSException.class, () -> kmsManager.unwrapKey(1L));

        // Its KEK was deleted, so no other version is tried either
        assertEquals(KMSException.ErrorType.KEK_NOT_FOUND, e.getErrorType());
        verify(kmsKekVersionDao, never()).getVersionsForDecryption(anyLong());
        verify(kmsProvider, never()).unwrapKey(any(WrappedKey.class), anyLong());
    }

    @Test(expected = KMSException.class)
    public void testUnwrapKey_ThrowsWhenAllVersionsFail() {
        mockWrappedKey(null, "AES/GCM/NoPadding");
        when(kmsKekVersionDao.getVersionsForDecryption(10L)).thenReturn(List.of());

        kmsManager.unwrapKey(1L);
    }

    @Test
    public void testDeleteKMSWrappedKey_KeepsKeyNeededBySnapshot() {
        Volume volume = mockVolumeWithWrappedKey();
        SnapshotVO snapshot = mockSnapshot(Snapshot.State.BackedUp);
        when(snapshotDao.searchByVolumes(List.of(1L))).thenReturn(List.of(snapshot));

        assertFalse(kmsManager.deleteKMSWrappedKey(volume));

        verify(kmsWrappedKeyDao, never()).remove(anyLong());
    }

    @Test
    public void testDeleteKMSWrappedKey_RemovesKeyWhenOnlyDestroyedSnapshotsAreLeft() {
        Volume volume = mockVolumeWithWrappedKey();
        SnapshotVO snapshot = mockSnapshot(Snapshot.State.Destroyed);
        when(snapshotDao.searchByVolumes(List.of(1L))).thenReturn(List.of(snapshot));
        when(kmsWrappedKeyDao.remove(50L)).thenReturn(true);

        assertTrue(kmsManager.deleteKMSWrappedKey(volume));

        verify(kmsWrappedKeyDao).remove(50L);
    }

    /**
     * Wrapped key 1 of KMS key 10 (volume encryption), recorded with the given KEK version and algorithm.
     */
    private KMSWrappedKeyVO mockWrappedKey(Long kekVersionId, String wrapAlgorithm) {
        KMSWrappedKeyVO wrappedVO = mock(KMSWrappedKeyVO.class);
        when(wrappedVO.getKmsKeyId()).thenReturn(10L);
        when(wrappedVO.getKekVersionId()).thenReturn(kekVersionId);
        Mockito.lenient().when(wrappedVO.getWrapAlgorithm()).thenReturn(wrapAlgorithm);
        Mockito.lenient().when(wrappedVO.getWrappedBlob()).thenReturn(new byte[]{0, 1});
        when(kmsWrappedKeyDao.findById(1L)).thenReturn(wrappedVO);

        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        Mockito.lenient().when(kmsKey.getId()).thenReturn(10L);
        Mockito.lenient().when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(kmsKeyDao.findById(10L)).thenReturn(kmsKey);
        return wrappedVO;
    }

    /**
     * A KEK version of KMS key 10 in HSM profile 20 (see {@link #mockDatabaseProfile()}).
     */
    private KMSKekVersionVO mockVersion(Long id, int versionNumber, KMSKekVersionVO.Status status) {
        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        Mockito.lenient().when(version.getId()).thenReturn(id);
        Mockito.lenient().when(version.getVersionNumber()).thenReturn(versionNumber);
        Mockito.lenient().when(version.getStatus()).thenReturn(status);
        Mockito.lenient().when(version.getHsmProfileId()).thenReturn(20L);
        Mockito.lenient().when(version.getKekLabel()).thenReturn("kek-label-" + versionNumber);
        return version;
    }

    /**
     * HSM profile 20, an enabled profile of the "database" provider.
     */
    private void mockDatabaseProfile() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(profile.isEnabled()).thenReturn(true);
        when(hsmProfileDao.findById(20L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
    }

    /**
     * Volume 1, the only volume using wrapped key 50.
     */
    private Volume mockVolumeWithWrappedKey() {
        Volume volume = mock(Volume.class);
        when(volume.getKmsWrappedKeyId()).thenReturn(50L);
        KMSWrappedKeyVO wrappedKey = mock(KMSWrappedKeyVO.class);
        when(wrappedKey.getId()).thenReturn(50L);
        when(kmsWrappedKeyDao.findById(50L)).thenReturn(wrappedKey);
        when(volumeDao.findByKmsWrappedKeyId(50L)).thenReturn(List.of());

        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getId()).thenReturn(1L);
        when(volumeDao.listIncludingRemovedByKmsWrappedKeyId(50L)).thenReturn(List.of(volumeVO));
        return volume;
    }

    private SnapshotVO mockSnapshot(Snapshot.State state) {
        SnapshotVO snapshot = mock(SnapshotVO.class);
        when(snapshot.getState()).thenReturn(state);
        return snapshot;
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
    public void testDeleteUserKMSKey_KeepsKeyWhenKekDeletionFails() throws KMSException {
        KMSKeyVO key = mockDeletableKeyWithOneKekVersion();
        doThrow(KMSException.kekOperationFailed("provider error")).when(kmsProvider).deleteKek(anyString());

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            assertThrows(CloudRuntimeException.class, () -> kmsManager.deleteUserKMSKey(key));

            // The KEK is still in the HSM, so keep the key that refers to it
            verify(kmsKeyDao, never()).remove(anyLong());
        }
    }

    @Test
    public void testDeleteUserKMSKey_ContinuesWhenKekIsAlreadyGone() throws KMSException {
        KMSKeyVO key = mockDeletableKeyWithOneKekVersion();
        doThrow(KMSException.kekNotFound("Key with label 'kek-label' not found in HSM")).when(kmsProvider).deleteKek(anyString());

        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            kmsManager.deleteUserKMSKey(key);

            verify(kmsKeyDao).remove(1L);
        }
    }

    /**
     * KMS key 1, not used by any wrapped key or volume, with one KEK version in a "database" HSM profile.
     */
    private KMSKeyVO mockDeletableKeyWithOneKekVersion() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getId()).thenReturn(1L);
        when(kmsWrappedKeyDao.countByKmsKeyId(1L)).thenReturn(0L);
        when(volumeDao.existsWithKmsKey(1L)).thenReturn(false);

        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        when(version.getHsmProfileId()).thenReturn(10L);
        when(version.getKekLabel()).thenReturn("kek-label");
        when(kmsKekVersionDao.listByKmsKeyId(1L)).thenReturn(List.of(version));

        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(10L)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
        return key;
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
