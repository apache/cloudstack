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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ApiRouteHandler;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Vm;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.vm.UserVmVO;

public class BackupVOToBackupConverter {

    public static Backup toBackup(final BackupVO backupVO, final Function<Long, UserVmVO> vmResolver,
              final Function<Long, HostJoinVO> hostResolver, final Function<BackupVO, List<Disk>> disksResolver) {
        Backup backup = new Backup();
        final String basePath = VeeamControlService.ContextPath.value();
        backup.setHref(basePath + VmsRouteHandler.BASE_ROUTE + "/backups/" + backupVO.getUuid());
        backup.setId(backupVO.getUuid());
        backup.setName(backupVO.getName());
        backup.setDescription(backupVO.getDescription());
        backup.setCreationDate(backupVO.getDate().getTime());
        backup.setPhase(mapStatusToPhase(backupVO.getStatus()));
        if (backupVO.getFromCheckpointId() != null) {
            backup.setFromCheckpointId(backupVO.getFromCheckpointId());
        }
        if (backupVO.getToCheckpointId() != null) {
            backup.setToCheckpointId(backupVO.getToCheckpointId());
        }
        if (vmResolver != null) {
            final UserVmVO vmVO = vmResolver.apply(backupVO.getVmId());
            if (vmVO != null) {
                backup.setVm(Vm.of(basePath + VmsRouteHandler.BASE_ROUTE + "/" + vmVO.getUuid(), vmVO.getUuid()));
            }
        }
        if (backupVO.getHostId() != null && hostResolver != null) {
            final HostJoinVO hostVO = hostResolver.apply(backupVO.getHostId());
            if (hostVO != null) {
                backup.setHost(Host.of(basePath + ApiRouteHandler.BASE_ROUTE + "/" + hostVO.getUuid(), hostVO.getUuid()));
            }
        }
        if (disksResolver != null) {
            List<Disk> disks = disksResolver.apply(backupVO);
            backup.setDisks(NamedList.of("disks", disks));
        }
        return backup;
    }

    public static List<Backup> toBackupList(final List<BackupVO> backupVOs, final Function<Long, UserVmVO> vmResolver,
                final Function<Long, HostJoinVO> hostResolver) {
        return backupVOs
                .stream()
                .map(backupVO -> toBackup(backupVO, vmResolver, hostResolver, null))
                .collect(Collectors.toList());
    }

    private static String mapStatusToPhase(final BackupVO.Status status) {
        switch (status) {
            case Allocated:
            case Queued:
                return "initializing";
            case BackingUp:
                return "starting";
            case ReadyForImageTransfer:
                return "ready";
            case FinalizingImageTransfer:
                return "finalizing";
            case Restoring:
            case BackedUp:
                return "succeeded";
        }
        return "failed";
    }
}
