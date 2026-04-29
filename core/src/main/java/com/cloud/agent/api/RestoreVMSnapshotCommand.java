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
import java.util.Map;

import org.apache.cloudstack.storage.to.VolumeObjectTO;

public class RestoreVMSnapshotCommand extends VMSnapshotBaseCommand {

    List<VMSnapshotTO> snapshots;
    Map<Long, VMSnapshotTO> snapshotAndParents;

    public RestoreVMSnapshotCommand(String vmName, VMSnapshotTO snapshot, List<VolumeObjectTO> volumeTOs, String guestOSType) {
        super(vmName, snapshot, volumeTOs, guestOSType);
    }

    public List<VMSnapshotTO> getSnapshots() {
            return snapshots;
    }

    public void setSnapshots(List<VMSnapshotTO> snapshots) {
        this.snapshots = snapshots;
    }

    public Map<Long, VMSnapshotTO> getSnapshotAndParents() {
        return snapshotAndParents;
    }

    public void setSnapshotAndParents(Map<Long, VMSnapshotTO> snapshotAndParents) {
        this.snapshotAndParents = snapshotAndParents;
    }

}
