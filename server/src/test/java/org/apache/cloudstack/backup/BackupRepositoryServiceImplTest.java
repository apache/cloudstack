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

package org.apache.cloudstack.backup;

import com.cloud.user.AccountManager;
import org.apache.cloudstack.api.command.user.backup.repository.AddBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.DeleteBackupRepositoryCmd;
import org.apache.cloudstack.api.command.user.backup.repository.ListBackupRepositoriesCmd;
import org.apache.cloudstack.api.command.user.backup.repository.UpdateBackupRepositoryCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupRepositoryDao;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupRepositoryServiceImplTest {

    @InjectMocks
    private BackupRepositoryServiceImpl backupRepositoryService;

    @Mock
    private BackupRepositoryDao repositoryDao;

    @Mock
    private BackupOfferingDao backupOfferingDao;

    @Mock
    private BackupDao backupDao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private AddBackupRepositoryCmd addCmd;

    @Mock
    private UpdateBackupRepositoryCmd updateCmd;

    @Mock
    private DeleteBackupRepositoryCmd deleteCmd;

    @Mock
    private ListBackupRepositoriesCmd listCmd;

    @Mock
    private BackupRepositoryVO repositoryVO;

    @Mock
    private BackupOfferingVO offeringVO;

    @Mock
    private BackupVO backupVO;

    @Mock
    private CallContext callContext;

    private Long zoneId = 2L;
    private Long backupOfferingId = 3L;

    @Test
    public void testUpdateBackupRepository() {
        when(updateCmd.getId()).thenReturn(1L);
        when(updateCmd.getName()).thenReturn("updated-repo");
        when(updateCmd.getAddress()).thenReturn("192.168.1.200:/backup");
        when(updateCmd.getMountOptions()).thenReturn("rw,noexec");
        when(updateCmd.crossZoneInstanceCreationEnabled()).thenReturn(false);

        when(repositoryDao.findById(1L)).thenReturn(repositoryVO);
        when(repositoryDao.createForUpdate(1L)).thenReturn(repositoryVO);
        when(repositoryDao.update(eq(1L), any(BackupRepositoryVO.class))).thenReturn(true);

        try (MockedStatic<CallContext> callContextMock = mockStatic(CallContext.class)) {
            callContextMock.when(CallContext::current).thenReturn(callContext);

            BackupRepository result = backupRepositoryService.updateBackupRepository(updateCmd);

            Assert.assertEquals(repositoryVO, result);
            verify(repositoryDao, Mockito.times(2)).findById(1L);
            verify(repositoryDao).createForUpdate(1L);
            verify(repositoryDao).update(eq(1L), any(BackupRepositoryVO.class));
            verify(callContext).setEventDetails(anyString());
        }
    }

    @Test
    public void testUpdateBackupRepositoryWithNullRepository() {
        when(updateCmd.getId()).thenReturn(1L);
        when(updateCmd.getName()).thenReturn("updated-repo");

        when(repositoryDao.findById(1L)).thenReturn(null);

        BackupRepository result = backupRepositoryService.updateBackupRepository(updateCmd);

        Assert.assertNull(result);
        verify(repositoryDao).findById(1L);
        verify(repositoryDao, never()).createForUpdate(anyLong());
        verify(repositoryDao, never()).update(anyLong(), any(BackupRepositoryVO.class));
    }

    @Test
    public void testUpdateBackupRepositoryWithUpdateFailure() {
        when(updateCmd.getId()).thenReturn(1L);
        when(updateCmd.getName()).thenReturn("updated-repo");

        when(repositoryDao.findById(1L)).thenReturn(repositoryVO);
        when(repositoryDao.createForUpdate(1L)).thenReturn(repositoryVO);
        when(repositoryDao.update(eq(1L), any(BackupRepositoryVO.class))).thenReturn(false);

        try (MockedStatic<CallContext> callContextMock = mockStatic(CallContext.class)) {
            callContextMock.when(CallContext::current).thenReturn(callContext);

            BackupRepository result = backupRepositoryService.updateBackupRepository(updateCmd);

            Assert.assertNull(result);
            verify(repositoryDao, Mockito.times(1)).findById(1L);
            verify(repositoryDao).createForUpdate(1L);
            verify(repositoryDao).update(eq(1L), any(BackupRepositoryVO.class));
        }
    }

    @Test
    public void testDeleteBackupRepository() {
        when(deleteCmd.getId()).thenReturn(1L);

        when(repositoryDao.findById(1L)).thenReturn(repositoryVO);
        when(repositoryVO.getUuid()).thenReturn("repo-uuid");
        when(repositoryVO.getZoneId()).thenReturn(zoneId);
        when(repositoryVO.getId()).thenReturn(1L);

        when(backupOfferingDao.findByExternalId("repo-uuid", zoneId)).thenReturn(offeringVO);
        when(offeringVO.getId()).thenReturn(backupOfferingId);

        when(backupDao.listByOfferingId(backupOfferingId)).thenReturn(new ArrayList<>());

        when(repositoryDao.remove(1L)).thenReturn(true);

        boolean result = backupRepositoryService.deleteBackupRepository(deleteCmd);

        Assert.assertTrue(result);
        verify(repositoryDao).findById(1L);
        verify(backupOfferingDao).findByExternalId("repo-uuid", zoneId);
        verify(backupDao).listByOfferingId(backupOfferingId);
        verify(repositoryDao).remove(1L);
    }

    @Test
    public void testDeleteBackupRepositoryWithNullRepository() {
        when(deleteCmd.getId()).thenReturn(1L);

        when(repositoryDao.findById(1L)).thenReturn(null);

        boolean result = backupRepositoryService.deleteBackupRepository(deleteCmd);

        Assert.assertFalse(result);
        verify(repositoryDao).findById(1L);
        verify(backupOfferingDao, never()).findByExternalId(anyString(), anyLong());
        verify(backupDao, never()).listByOfferingId(anyLong());
        verify(repositoryDao, never()).remove(anyLong());
    }

    @Test
    public void testDeleteBackupRepositoryWithExistingBackups() {
        when(deleteCmd.getId()).thenReturn(1L);

        when(repositoryDao.findById(1L)).thenReturn(repositoryVO);
        when(repositoryVO.getUuid()).thenReturn("repo-uuid");
        when(repositoryVO.getZoneId()).thenReturn(zoneId);

        when(backupOfferingDao.findByExternalId("repo-uuid", zoneId)).thenReturn(offeringVO);
        when(offeringVO.getId()).thenReturn(backupOfferingId);

        List<Backup> backups = Arrays.asList(backupVO);
        when(backupDao.listByOfferingId(backupOfferingId)).thenReturn(backups);

        try {
            backupRepositoryService.deleteBackupRepository(deleteCmd);
            Assert.fail("Expected CloudRuntimeException");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to delete backup repository as there are backups present on it"));
        }

        verify(repositoryDao).findById(1L);
        verify(backupOfferingDao).findByExternalId("repo-uuid", zoneId);
        verify(backupDao).listByOfferingId(backupOfferingId);
        verify(repositoryDao, never()).remove(anyLong());
    }

    @Test
    public void testDeleteBackupRepositoryWithNullOffering() {
        when(deleteCmd.getId()).thenReturn(1L);

        when(repositoryDao.findById(1L)).thenReturn(repositoryVO);
        when(repositoryVO.getUuid()).thenReturn("repo-uuid");
        when(repositoryVO.getId()).thenReturn(1L);
        when(repositoryVO.getZoneId()).thenReturn(zoneId);

        when(repositoryDao.remove(1L)).thenReturn(true);

        boolean result = backupRepositoryService.deleteBackupRepository(deleteCmd);

        Assert.assertTrue(result);
        verify(repositoryDao).findById(1L);
        verify(backupOfferingDao).findByExternalId("repo-uuid", zoneId);
        verify(backupDao, never()).listByOfferingId(anyLong());
        verify(repositoryDao).remove(1L);
    }
}
