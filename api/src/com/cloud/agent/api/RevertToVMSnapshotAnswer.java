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

package com.cloud.agent.api;

import java.util.List;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.vm.VirtualMachine;

public class RevertToVMSnapshotAnswer extends Answer {

    private List<VolumeTO> volumeTOs;
    private VirtualMachine.State vmState;

    public RevertToVMSnapshotAnswer(RevertToVMSnapshotCommand cmd, boolean result,
            String message) {
        super(cmd, result, message);
    }

    public RevertToVMSnapshotAnswer() {
        super();
    }

    public RevertToVMSnapshotAnswer(RevertToVMSnapshotCommand cmd,
            List<VolumeTO> volumeTOs,
            VirtualMachine.State vmState) {
        super(cmd, true, "");
        this.volumeTOs = volumeTOs;
        this.vmState = vmState;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }

    public List<VolumeTO> getVolumeTOs() {
        return volumeTOs;
    }

    public void setVolumeTOs(List<VolumeTO> volumeTOs) {
        this.volumeTOs = volumeTOs;
    }

    public void setVmState(VirtualMachine.State vmState) {
        this.vmState = vmState;
    }

}
