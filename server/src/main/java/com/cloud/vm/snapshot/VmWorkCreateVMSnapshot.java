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

import com.cloud.vm.VmWork;

public class VmWorkCreateVMSnapshot extends VmWork {
    private static final long serialVersionUID = 124386202146049838L;

    private Long vmSnapshotId;
    private boolean quiesceVm;

    public VmWorkCreateVMSnapshot(long userId, long accountId, long vmId, String handlerName, Long vmSnapshotId, boolean quiesceVm) {
        super(userId, accountId, vmId, handlerName);

        this.vmSnapshotId = vmSnapshotId;
        this.quiesceVm = quiesceVm;
    }

    public Long getVmSnapshotId() {
        return vmSnapshotId;
    }

    public boolean isQuiesceVm() {
        return quiesceVm;
    }
}
