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
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.BaseDto;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Vm;

import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;

public class VmSnapshotVOToSnapshotConverter {
    public static Snapshot toSnapshot(final VMSnapshotVO vmSnapshotVO, String vmUuid) {
        final String basePath = VeeamControlService.ContextPath.value();
        final Snapshot snapshot = new Snapshot();
        snapshot.setId(vmSnapshotVO.getUuid());
        snapshot.setHref(basePath + VmsRouteHandler.BASE_ROUTE + "/" + vmUuid + "/snapshots/" + vmSnapshotVO.getUuid());
        snapshot.setVm(Vm.of(basePath + VmsRouteHandler.BASE_ROUTE + "/" + vmUuid, vmUuid));
        snapshot.setDescription(vmSnapshotVO.getDescription());
        snapshot.setSnapshotType("active");
        snapshot.setDate(vmSnapshotVO.getCreated().getTime());
        snapshot.setPersistMemorystate(String.valueOf(VMSnapshotVO.Type.DiskAndMemory.equals(vmSnapshotVO.getType())));
        snapshot.setSnapshotStatus(VMSnapshot.State.Ready.equals(vmSnapshotVO.getState()) ? "ok" : "locked");
        snapshot.setActions(new Actions(List.of(BaseDto.getActionLink("restore", snapshot.getHref()))));
        return snapshot;
    }

    public static List<Snapshot> toSnapshotList(final List<VMSnapshotVO> vmSnapshotVOList, final String vmUuid) {
        return vmSnapshotVOList.stream()
                .map(v -> toSnapshot(v, vmUuid))
                .collect(Collectors.toList());
    }
}
