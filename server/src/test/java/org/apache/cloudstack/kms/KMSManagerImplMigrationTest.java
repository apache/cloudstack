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
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.GlobalLock;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
import org.apache.cloudstack.secret.PassphraseVO;
import org.apache.cloudstack.secret.dao.PassphraseDao;
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

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for migrating legacy volume passphrases to KMS, and for repairing the keys 4.23 migrated.
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplMigrationTest {

    private static final Long PASSPHRASE_ID = 7L;
    private static final Long WRAPPED_KEY_ID = 5L;
    private static final Long KMS_KEY_ID = 10L;
    private static final Long HSM_PROFILE_ID = 20L;
    private static final Long KEK_VERSION_ID = 30L;
    private static final String KEK_LABEL = "kek-label";

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
    private PassphraseDao passphraseDao;

    @Mock
    private KMSProvider kmsProvider;

    private MockedStatic<ActionEventUtils> actionEventUtils;

    @Before
    public void setUp() {
        actionEventUtils = Mockito.mockStatic(ActionEventUtils.class);
    }

    @After
    public void tearDown() {
        actionEventUtils.close();
    }

    @Test
    public void testMigrateVolumeToKmsKey_WrapsRawPassphraseAndRemovesLegacyRow() {
        byte[] passphrase = legacyPassphrase();
        VolumeVO volume = mockLegacyVolume(passphrase);
        AtomicReference<byte[]> wrappedPlainKey = mockWrap();
        when(kmsWrappedKeyDao.persist(any(KMSWrappedKeyVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(volumeDao.listIncludingRemovedByPassphraseId(PASSPHRASE_ID)).thenReturn(List.of());

        assertTrue(kmsManager.migrateVolumeToKmsKey(kmsProvider, volume, mockKmsKey(), mockKekVersion()));

        // The raw bytes, so that the base64 text handed to qemu stays the same passphrase
        assertArrayEquals(passphrase, wrappedPlainKey.get());
        ArgumentCaptor<KMSWrappedKeyVO> wrappedKeyCaptor = ArgumentCaptor.forClass(KMSWrappedKeyVO.class);
        verify(kmsWrappedKeyDao).persist(wrappedKeyCaptor.capture());
        assertEquals("AES/GCM/NoPadding", wrappedKeyCaptor.getValue().getWrapAlgorithm());
        verify(volume).setPassphraseId(null);
        // The volume key isn't left behind in the passphrase table
        verify(passphraseDao).remove(PASSPHRASE_ID);
    }

    @Test
    public void testMigrateVolumeToKmsKey_KeepsLegacyRowUsedByAnotherVolume() {
        VolumeVO volume = mockLegacyVolume(legacyPassphrase());
        mockWrap();
        when(kmsWrappedKeyDao.persist(any(KMSWrappedKeyVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(volumeDao.listIncludingRemovedByPassphraseId(PASSPHRASE_ID)).thenReturn(List.of(mock(VolumeVO.class)));

        assertTrue(kmsManager.migrateVolumeToKmsKey(kmsProvider, volume, mockKmsKey(), mockKekVersion()));

        verify(passphraseDao, never()).remove(anyLong());
    }

    @Test
    public void testRepairKeyFormatBatch_RewrapsKeyMigratedBy423() {
        byte[] passphrase = legacyPassphrase();
        KMSWrappedKeyVO wrappedKey = flaggedWrappedKey();
        mockActiveVersionInDatabaseProfile();
        // 4.23 wrapped the base64 text of the passphrase
        doReturn(Base64.getEncoder().encode(passphrase)).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);
        KMSKeyVO kmsKey = mock(KMSKeyVO.class);
        when(kmsKey.getId()).thenReturn(KMS_KEY_ID);
        when(kmsKey.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(kmsKeyDao.findById(KMS_KEY_ID)).thenReturn(kmsKey);
        AtomicReference<byte[]> wrappedPlainKey = mockWrap();
        VolumeVO volume = mockVolumeCreated(new Date(0));
        when(volumeDao.findByKmsWrappedKeyId(WRAPPED_KEY_ID)).thenReturn(List.of(volume));

        assertFalse(runRepairKeyFormatBatch());

        assertArrayEquals(passphrase, wrappedPlainKey.get());
        assertArrayEquals(new byte[]{1, 2, 3}, wrappedKey.getWrappedBlob());
        assertEquals("AES/GCM/NoPadding", wrappedKey.getWrapAlgorithm());
        assertFalse(wrappedKey.isFormatCheckPending());
        verify(kmsWrappedKeyDao).update(WRAPPED_KEY_ID, wrappedKey);
    }

    @Test
    public void testRepairKeyFormatBatch_OnlyClearsFlagOfNativeKey() {
        KMSWrappedKeyVO wrappedKey = flaggedWrappedKey();
        doReturn(new byte[32]).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);

        assertFalse(runRepairKeyFormatBatch());

        assertArrayEquals(new byte[]{9}, wrappedKey.getWrappedBlob());
        assertFalse(wrappedKey.isFormatCheckPending());
        verify(kmsWrappedKeyDao).update(WRAPPED_KEY_ID, wrappedKey);
        verify(kmsProvider, never()).wrapKey(any(byte[].class), any(KeyPurpose.class), any(), any());
    }

    @Test
    public void testRepairKeyFormatBatch_KeepsFlagWhenKeyCantBeUnwrapped() {
        KMSWrappedKeyVO wrappedKey = flaggedWrappedKey();
        doThrow(KMSException.wrapUnwrapFailed("HSM unreachable")).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);

        assertFalse(runRepairKeyFormatBatch());

        // Retried in a later run
        assertTrue(wrappedKey.isFormatCheckPending());
        verify(kmsWrappedKeyDao, never()).update(anyLong(), any(KMSWrappedKeyVO.class));
    }

    @Test
    public void testRepairKeyFormatBatch_SkipsKeyUsedByVolumeCreatedAfterMigration() {
        KMSWrappedKeyVO wrappedKey = flaggedWrappedKey();
        doReturn(Base64.getEncoder().encode(legacyPassphrase())).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);
        // e.g. the copy made by a storage migration on 4.23, encrypted with the passphrase 4.23 used
        VolumeVO volume = mockVolumeCreated(new Date(wrappedKey.getCreated().getTime() + 60_000));
        when(volumeDao.findByKmsWrappedKeyId(WRAPPED_KEY_ID)).thenReturn(List.of(volume));

        assertFalse(runRepairKeyFormatBatch());

        assertArrayEquals(new byte[]{9}, wrappedKey.getWrappedBlob());
        assertFalse(wrappedKey.isFormatCheckPending());
        verify(kmsProvider, never()).wrapKey(any(byte[].class), any(KeyPurpose.class), any(), any());
    }

    @Test
    public void testRepairKeyFormatBatch_ClearsFlagOfKeyThatIsGone() {
        KMSWrappedKeyVO wrappedKey = flaggedWrappedKey();
        doThrow(KMSException.kekNotFound("KMS key not found for wrapped key: 5")).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);

        assertFalse(runRepairKeyFormatBatch());

        // Nothing left to repair, so the repair doesn't keep retrying it
        assertFalse(wrappedKey.isFormatCheckPending());
        verify(kmsWrappedKeyDao).update(WRAPPED_KEY_ID, wrappedKey);
    }

    @Test
    public void testRepairKeyFormatBatch_StopsAtFirstFailure() {
        KMSWrappedKeyVO failingKey = new KMSWrappedKeyVO(KMS_KEY_ID, KEK_VERSION_ID, 1L, new byte[]{9});
        failingKey.setId(WRAPPED_KEY_ID);
        failingKey.setFormatCheckPending(true);
        KMSWrappedKeyVO nextKey = new KMSWrappedKeyVO(KMS_KEY_ID, KEK_VERSION_ID, 1L, new byte[]{9});
        nextKey.setId(6L);
        nextKey.setFormatCheckPending(true);
        when(kmsWrappedKeyDao.listFormatCheckPending(eq(0L), anyInt())).thenReturn(List.of(failingKey, nextKey));
        doThrow(KMSException.wrapUnwrapFailed("HSM unreachable")).when(kmsManager).unwrapKey(WRAPPED_KEY_ID);

        assertFalse(runRepairKeyFormatBatch());

        // The lock it shares with the rewrap worker isn't held through more failing unwraps
        verify(kmsManager, never()).unwrapKey(6L);
    }

    @Test
    public void testRepairKeyFormatBatch_DoneWhenNoKeyIsFlagged() {
        when(kmsWrappedKeyDao.listFormatCheckPending(eq(0L), anyInt())).thenReturn(List.of());

        assertTrue(runRepairKeyFormatBatch());
    }

    @Test
    public void testDecodeLegacyMigratedKey() {
        byte[] passphrase = legacyPassphrase();
        assertArrayEquals(passphrase, KMSManagerImpl.decodeLegacyMigratedKey(Base64.getEncoder().encode(passphrase)));
        // A DEK created by KMS
        assertNull(KMSManagerImpl.decodeLegacyMigratedKey(new byte[32]));
        // 64 bytes that aren't base64 text
        assertNull(KMSManagerImpl.decodeLegacyMigratedKey(new byte[64]));
    }

    private boolean runRepairKeyFormatBatch() {
        try (MockedStatic<GlobalLock> globalLock = Mockito.mockStatic(GlobalLock.class)) {
            GlobalLock lock = mock(GlobalLock.class);
            globalLock.when(() -> GlobalLock.getInternLock("kms.rewrap.worker")).thenReturn(lock);
            when(lock.lock(5)).thenReturn(true);
            return kmsManager.repairKeyFormatBatch();
        }
    }

    private static byte[] legacyPassphrase() {
        byte[] passphrase = new byte[48];
        for (int i = 0; i < passphrase.length; i++) {
            passphrase[i] = (byte) (i * 7);
        }
        return passphrase;
    }

    /**
     * A legacy encrypted volume whose passphrase row holds the base64 text of the given passphrase.
     */
    private VolumeVO mockLegacyVolume(byte[] passphrase) {
        PassphraseVO passphraseVO = mock(PassphraseVO.class);
        when(passphraseVO.getPassphraseString()).thenReturn(Base64.getEncoder().encodeToString(passphrase));
        when(passphraseDao.findById(PASSPHRASE_ID)).thenReturn(passphraseVO);

        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getAccountId()).thenReturn(2L);
        when(volume.getPassphraseId()).thenReturn(PASSPHRASE_ID);
        return volume;
    }

    private VolumeVO mockVolumeCreated(Date created) {
        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getCreated()).thenReturn(created);
        return volume;
    }

    private KMSKey mockKmsKey() {
        KMSKey kmsKey = mock(KMSKey.class);
        when(kmsKey.getAccountId()).thenReturn(2L);
        when(kmsKey.getId()).thenReturn(KMS_KEY_ID);
        return kmsKey;
    }

    private KMSKekVersionVO mockKekVersion() {
        KMSKekVersionVO version = mock(KMSKekVersionVO.class);
        Mockito.lenient().when(version.getId()).thenReturn(KEK_VERSION_ID);
        Mockito.lenient().when(version.getStatus()).thenReturn(KMSKekVersionVO.Status.Active);
        Mockito.lenient().when(version.getKekLabel()).thenReturn(KEK_LABEL);
        Mockito.lenient().when(version.getHsmProfileId()).thenReturn(HSM_PROFILE_ID);
        return version;
    }

    private void mockActiveVersionInDatabaseProfile() {
        KMSKekVersionVO version = mockKekVersion();
        when(kmsKekVersionDao.getActiveVersion(KMS_KEY_ID)).thenReturn(version);
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.getProtocol()).thenReturn("database");
        when(hsmProfileDao.findById(HSM_PROFILE_ID)).thenReturn(profile);
        doReturn(kmsProvider).when(kmsManager).getKMSProvider("database");
    }

    /**
     * Wrapped key 5, existing at the upgrade from 4.23 and not checked yet.
     */
    private KMSWrappedKeyVO flaggedWrappedKey() {
        KMSWrappedKeyVO wrappedKey = new KMSWrappedKeyVO(KMS_KEY_ID, KEK_VERSION_ID, 1L, new byte[]{9});
        wrappedKey.setId(WRAPPED_KEY_ID);
        wrappedKey.setWrapAlgorithm("AES/CBC/PKCS5Padding");
        wrappedKey.setFormatCheckPending(true);
        when(kmsWrappedKeyDao.listFormatCheckPending(eq(0L), anyInt())).thenReturn(List.of(wrappedKey));
        return wrappedKey;
    }

    /**
     * Makes the provider wrap with AES/GCM/NoPadding, recording the plain key it got.
     */
    private AtomicReference<byte[]> mockWrap() {
        AtomicReference<byte[]> plainKey = new AtomicReference<>();
        WrappedKey wrappedKey = mock(WrappedKey.class);
        when(wrappedKey.getWrappedKeyMaterial()).thenReturn(new byte[]{1, 2, 3});
        when(wrappedKey.getAlgorithm()).thenReturn("AES/GCM/NoPadding");
        when(kmsProvider.wrapKey(any(byte[].class), eq(KeyPurpose.VOLUME_ENCRYPTION), eq(KEK_LABEL), eq(HSM_PROFILE_ID)))
                .thenAnswer(invocation -> {
                    plainKey.set(((byte[]) invocation.getArgument(0)).clone());
                    return wrappedKey;
                });
        return plainKey;
    }
}
