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

public class DeleteVMSnapshotAnswer extends Answer {
    private List<VolumeObjectTO> volumeTOs;

    public DeleteVMSnapshotAnswer() {
    }

    public DeleteVMSnapshotAnswer(DeleteVMSnapshotCommand cmd, boolean result, String message) {
        super(cmd, result, message);
    }

    public DeleteVMSnapshotAnswer(DeleteVMSnapshotCommand cmd, List<VolumeObjectTO> volumeTOs) {
        super(cmd, true, "");
        this.volumeTOs = volumeTOs;
    }

    public List<VolumeObjectTO> getVolumeTOs() {
        return volumeTOs;
    }

    public void setVolumeTOs(List<VolumeObjectTO> volumeTOs) {
        this.volumeTOs = volumeTOs;
    }

}
