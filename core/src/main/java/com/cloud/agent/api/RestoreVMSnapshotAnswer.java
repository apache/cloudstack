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

import com.cloud.vm.VirtualMachine;

public class RestoreVMSnapshotAnswer extends Answer {

    private List<VolumeObjectTO> volumeTOs;
    private VirtualMachine.PowerState vmState;

    public RestoreVMSnapshotAnswer(RestoreVMSnapshotCommand cmd, boolean result, String message) {
        super(cmd, result, message);
    }

    public RestoreVMSnapshotAnswer() {
        super();
    }

    public RestoreVMSnapshotAnswer(RestoreVMSnapshotCommand cmd, List<VolumeObjectTO> volumeTOs, VirtualMachine.PowerState vmState) {
        super(cmd, true, "");
        this.volumeTOs = volumeTOs;
        this.vmState = vmState;
    }

    public VirtualMachine.PowerState getVmState() {
        return vmState;
    }

    public List<VolumeObjectTO> getVolumeTOs() {
        return volumeTOs;
    }

    public void setVolumeTOs(List<VolumeObjectTO> volumeTOs) {
        this.volumeTOs = volumeTOs;
    }

    public void setVmState(VirtualMachine.PowerState vmState) {
        this.vmState = vmState;
    }

}
