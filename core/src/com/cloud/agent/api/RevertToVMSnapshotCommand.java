//
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
//

package com.cloud.agent.api;

import java.util.List;

import org.apache.cloudstack.storage.to.VolumeObjectTO;

public class RevertToVMSnapshotCommand extends VMSnapshotBaseCommand {

    public RevertToVMSnapshotCommand(String vmName, String vmUuid, VMSnapshotTO snapshot, List<VolumeObjectTO> volumeTOs, String guestOSType) {
        super(vmName, snapshot, volumeTOs, guestOSType);
        this.vmUuid = vmUuid;
    }

    public RevertToVMSnapshotCommand(String vmName, String vmUuid, VMSnapshotTO snapshot, List<VolumeObjectTO> volumeTOs, String guestOSType, boolean reloadVm) {
        this(vmName, vmUuid, snapshot, volumeTOs, guestOSType);
        setReloadVm(reloadVm);
    }

    private boolean reloadVm = false;
    private String vmUuid;

    public boolean isReloadVm() {
        return reloadVm;
    }

    public void setReloadVm(boolean reloadVm) {
        this.reloadVm = reloadVm;
    }

    public String getVmUuid() {
        return vmUuid;
    }
}
