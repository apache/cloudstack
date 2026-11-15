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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.junit.Test;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.vm.UserVmVO;

public class BackupVOToBackupConverterTest {

    @Test
    public void testToBackup_MapsCoreFieldsAndResolvedRefs() {
        final BackupVO backupVO = mock(BackupVO.class);
        when(backupVO.getUuid()).thenReturn("bkp-1");
        when(backupVO.getName()).thenReturn("backup-1");
        when(backupVO.getDescription()).thenReturn("desc-1");
        when(backupVO.getDate()).thenReturn(new Date(1000L));
        when(backupVO.getStatus()).thenReturn(Backup.Status.ReadyForImageTransfer);
        when(backupVO.getFromCheckpointId()).thenReturn("cp-1");
        when(backupVO.getToCheckpointId()).thenReturn("cp-2");
        when(backupVO.getVmId()).thenReturn(101L);
        when(backupVO.getHostId()).thenReturn(201L);

        final UserVmVO vmVO = mock(UserVmVO.class);
        when(vmVO.getUuid()).thenReturn("vm-1");
        final HostJoinVO hostVO = mock(HostJoinVO.class);
        when(hostVO.getUuid()).thenReturn("host-1");

        final org.apache.cloudstack.veeam.api.dto.Backup backup = BackupVOToBackupConverter.toBackup(
                backupVO,
                id -> vmVO,
                id -> hostVO,
                vo -> List.of(new Disk())
        );

        assertEquals("bkp-1", backup.getId());
        assertEquals("backup-1", backup.getName());
        assertEquals("desc-1", backup.getDescription());
        assertEquals(Long.valueOf(1000L), backup.getCreationDate());
        assertEquals("ready", backup.getPhase());
        assertEquals("cp-1", backup.getFromCheckpointId());
        assertEquals("cp-2", backup.getToCheckpointId());
        assertEquals("vm-1", backup.getVm().getId());
        assertEquals("host-1", backup.getHost().getId());
        assertNotNull(backup.getDisks());
    }

    @Test
    public void testToBackup_PhaseMappingForDifferentStatuses() {
        final BackupVO queued = mock(BackupVO.class);
        when(queued.getUuid()).thenReturn("b1");
        when(queued.getDate()).thenReturn(new Date(1L));
        when(queued.getStatus()).thenReturn(Backup.Status.Queued);
        when(queued.getVmId()).thenReturn(1L);

        final BackupVO finalizing = mock(BackupVO.class);
        when(finalizing.getUuid()).thenReturn("b2");
        when(finalizing.getDate()).thenReturn(new Date(2L));
        when(finalizing.getStatus()).thenReturn(Backup.Status.FinalizingImageTransfer);
        when(finalizing.getVmId()).thenReturn(2L);

        final BackupVO failed = mock(BackupVO.class);
        when(failed.getUuid()).thenReturn("b3");
        when(failed.getDate()).thenReturn(new Date(3L));
        when(failed.getStatus()).thenReturn(Backup.Status.Failed);
        when(failed.getVmId()).thenReturn(3L);

        assertEquals("initializing", BackupVOToBackupConverter.toBackup(queued, null, null, null).getPhase());
        assertEquals("finalizing", BackupVOToBackupConverter.toBackup(finalizing, null, null, null).getPhase());
        assertEquals("failed", BackupVOToBackupConverter.toBackup(failed, null, null, null).getPhase());
    }
}
