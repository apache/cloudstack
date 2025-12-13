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

package com.cloud.vm.snapshot;

import java.util.List;

import com.cloud.utils.fsm.NoTransitionException;
import org.apache.cloudstack.api.command.user.vmsnapshot.ListVMSnapshotCmd;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;

public interface VMSnapshotService {

    Pair<List<? extends VMSnapshot>, Integer> listVMSnapshots(ListVMSnapshotCmd cmd);

    VMSnapshot getVMSnapshotById(Long id);

    VMSnapshot createVMSnapshot(Long vmId, Long vmSnapshotId, Boolean quiescevm);

    VMSnapshot allocVMSnapshot(Long vmId, String vsDisplayName, String vsDescription, Boolean snapshotMemory) throws ResourceAllocationException;

    boolean deleteVMSnapshot(Long vmSnapshotId);

    UserVm revertToSnapshot(Long vmSnapshotId) throws InsufficientServerCapacityException, InsufficientCapacityException, ResourceUnavailableException,
        ConcurrentOperationException;

    VirtualMachine getVMBySnapshotId(Long id);

    /**
     * Delete vm snapshots only from database. Introduced as a Vmware optimization in which vm snapshots are deleted when
     * the vm gets deleted on hypervisor (no need to delete each vm snapshot before deleting vm, just mark them as deleted on DB)
     * @param id vm id
     */
    boolean deleteVMSnapshotsFromDB(Long vmId, boolean unmanage);

    void updateOperationFailed(VMSnapshot vmSnapshot) throws NoTransitionException;
}
