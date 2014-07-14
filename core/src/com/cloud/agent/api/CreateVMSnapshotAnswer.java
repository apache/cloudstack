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

public class CreateVMSnapshotAnswer extends Answer {

    private List<VolumeObjectTO> volumeTOs;
    private VMSnapshotTO vmSnapshotTo;

    public List<VolumeObjectTO> getVolumeTOs() {
        return volumeTOs;
    }

    public void setVolumeTOs(List<VolumeObjectTO> volumeTOs) {
        this.volumeTOs = volumeTOs;
    }

    public VMSnapshotTO getVmSnapshotTo() {
        return vmSnapshotTo;
    }

    public void setVmSnapshotTo(VMSnapshotTO vmSnapshotTo) {
        this.vmSnapshotTo = vmSnapshotTo;
    }

    public CreateVMSnapshotAnswer() {

    }

    public CreateVMSnapshotAnswer(CreateVMSnapshotCommand cmd, boolean success, String result) {
        super(cmd, success, result);
    }

    public CreateVMSnapshotAnswer(CreateVMSnapshotCommand cmd, VMSnapshotTO vmSnapshotTo, List<VolumeObjectTO> volumeTOs) {
        super(cmd, true, "");
        this.vmSnapshotTo = vmSnapshotTo;
        this.volumeTOs = volumeTOs;
    }

}
