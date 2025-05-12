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

import org.apache.cloudstack.storage.to.VolumeObjectTO;

import java.util.List;

public class RecreateCheckpointsCommand extends Command {

    private List<VolumeObjectTO> volumes;

    private String vmName;

    public RecreateCheckpointsCommand(List<VolumeObjectTO> volumes, String vmName) {
        this.volumes = volumes;
        this.vmName = vmName;
    }

    public List<VolumeObjectTO> getDisks() {
        return volumes;
    }

    public String getVmName() {
        return vmName;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
