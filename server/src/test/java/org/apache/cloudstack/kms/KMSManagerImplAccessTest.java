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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests covering access and permission helpers in KMSManagerImpl.
 */
@RunWith(MockitoJUnitRunner.class)
public class KMSManagerImplAccessTest {

    @Spy
    @InjectMocks
    private KMSManagerImpl kmsManager;

    @Mock
    private KMSKeyDao kmsKeyDao;

    @Mock
    private HSMProfileDao hsmProfileDao;

    @Mock
    private AccountManager accountManager;

    @Test
    public void testHasPermission_ReturnsFalseWhenCallerAccountIdIsNull() {
        assertFalse(kmsManager.hasPermission(null, mock(KMSKey.class)));
    }

    @Test
    public void testHasPermission_ReturnsFalseWhenKeyIsNull() {
        assertFalse(kmsManager.hasPermission(1L, null));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testHasPermission_ThrowsWhenKeyIsDisabled() {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(false);
        kmsManager.hasPermission(1L, key);
    }

    @Test
    public void testHasPermission_ReturnsFalseWhenCallerAccountNotFound() {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(accountManager.getAccount(1L)).thenReturn(null);

        assertFalse(kmsManager.hasPermission(1L, key));
    }

    @Test
    public void testHasPermission_ReturnsFalseWhenPermissionDenied() {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getAccountId()).thenReturn(10L);

        Account caller = mock(Account.class);
        Account owner = mock(Account.class);
        when(accountManager.getAccount(1L)).thenReturn(caller);
        when(accountManager.getAccount(10L)).thenReturn(owner);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountManager).checkAccess(caller, null, true, owner);

        assertFalse(kmsManager.hasPermission(1L, key));
    }

    @Test
    public void testHasPermission_ReturnsTrueWhenAccessGranted() {
        KMSKey key = mock(KMSKey.class);
        when(key.isEnabled()).thenReturn(true);
        when(key.getAccountId()).thenReturn(10L);

        Account caller = mock(Account.class);
        Account owner = mock(Account.class);
        when(accountManager.getAccount(1L)).thenReturn(caller);
        when(accountManager.getAccount(10L)).thenReturn(owner);

        assertTrue(kmsManager.hasPermission(1L, key));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testFindKMSKeyAndCheckAccess_ThrowsWhenKeyNotFound() {
        when(kmsKeyDao.findById(99L)).thenReturn(null);
        kmsManager.findKMSKeyAndCheckAccess(99L, mock(Account.class));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testFindKMSKeyAndCheckAccess_ThrowsWhenPermissionDenied() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        Account caller = mock(Account.class);
        when(kmsKeyDao.findById(1L)).thenReturn(key);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountManager).checkAccess(caller, null, true, key);

        kmsManager.findKMSKeyAndCheckAccess(1L, caller);
    }

    @Test
    public void testFindKMSKeyAndCheckAccess_ReturnsKeyOnSuccess() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        Account caller = mock(Account.class);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        KMSKeyVO result = kmsManager.findKMSKeyAndCheckAccess(1L, caller);

        assertSame(key, result);
    }

    @Test
    public void testCheckKmsKeyForVolumeEncryption_NoOpWhenKeyIdIsNull() {
        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), null, 1L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckKmsKeyForVolumeEncryption_ThrowsWhenKeyNotFound() {
        when(kmsKeyDao.findById(1L)).thenReturn(null);
        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckKmsKeyForVolumeEncryption_ThrowsWhenPermissionDenied() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        Account caller = mock(Account.class);
        when(kmsKeyDao.findById(1L)).thenReturn(key);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountManager).checkAccess(caller, null, true, key);

        kmsManager.checkKmsKeyForVolumeEncryption(caller, 1L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckKmsKeyForVolumeEncryption_ThrowsOnZoneMismatch() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getZoneId()).thenReturn(2L);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, 3L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckKmsKeyForVolumeEncryption_ThrowsWhenKeyDisabled() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getZoneId()).thenReturn(null);
        when(key.isEnabled()).thenReturn(false);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckKmsKeyForVolumeEncryption_ThrowsWhenWrongPurpose() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getZoneId()).thenReturn(null);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.TLS_CERT);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, null);
    }

    @Test
    public void testCheckKmsKeyForVolumeEncryption_PassesForMatchingZone() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getZoneId()).thenReturn(1L);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, 1L);
    }

    @Test
    public void testCheckKmsKeyForVolumeEncryption_PassesWhenKeyHasNoZoneRestriction() {
        KMSKeyVO key = mock(KMSKeyVO.class);
        when(key.getZoneId()).thenReturn(null);
        when(key.isEnabled()).thenReturn(true);
        when(key.getPurpose()).thenReturn(KeyPurpose.VOLUME_ENCRYPTION);
        when(kmsKeyDao.findById(1L)).thenReturn(key);

        kmsManager.checkKmsKeyForVolumeEncryption(mock(Account.class), 1L, 5L);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckHSMProfileAccess_DeniesNonRootModifyOfSystemProfile() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isSystem()).thenReturn(true);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(1L);
        when(accountManager.isRootAdmin(1L)).thenReturn(false);

        kmsManager.checkHSMProfileAccess(caller, profile, true);
    }

    @Test
    public void testCheckHSMProfileAccess_AllowsRootModifyOfSystemProfile() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isSystem()).thenReturn(true);

        Account caller = mock(Account.class);
        when(caller.getId()).thenReturn(1L);
        when(accountManager.isRootAdmin(1L)).thenReturn(true);

        kmsManager.checkHSMProfileAccess(caller, profile, true);
    }

    @Test
    public void testCheckHSMProfileAccess_AllowsReadAccessToSystemProfileForAllUsers() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isSystem()).thenReturn(true);

        kmsManager.checkHSMProfileAccess(mock(Account.class), profile, false);
    }

    @Test
    public void testCheckHSMProfileAccess_DelegatesToAclForOwnedProfile() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isSystem()).thenReturn(false);

        kmsManager.checkHSMProfileAccess(mock(Account.class), profile, true);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckHSMProfileAccess_ThrowsWhenAclDeniesOwnedProfile() {
        HSMProfileVO profile = mock(HSMProfileVO.class);
        when(profile.isSystem()).thenReturn(false);

        Account caller = mock(Account.class);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountManager).checkAccess(caller, null, true, profile);

        kmsManager.checkHSMProfileAccess(caller, profile, true);
    }

    @Test
    public void testParseKeyPurpose_ReturnsNullForNullInput() {
        assertNull(kmsManager.parseKeyPurpose(null));
    }

    @Test
    public void testParseKeyPurpose_ReturnsVolumeEncryptionForValidName() {
        KeyPurpose result = kmsManager.parseKeyPurpose("volume");
        assertNotNull(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testParseKeyPurpose_ThrowsForUnknownPurpose() {
        kmsManager.parseKeyPurpose("not-a-valid-purpose");
    }
}
