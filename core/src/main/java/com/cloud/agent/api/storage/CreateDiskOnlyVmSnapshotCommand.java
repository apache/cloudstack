/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.agent.api.storage;


import com.cloud.agent.api.VMSnapshotBaseCommand;
import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import java.util.List;

public class CreateDiskOnlyVmSnapshotCommand extends VMSnapshotBaseCommand {

    protected VirtualMachine.State vmState;

    public CreateDiskOnlyVmSnapshotCommand(String vmName, VMSnapshotTO snapshot, List<VolumeObjectTO> volumeTOs, String guestOSType, VirtualMachine.State vmState) {
        super(vmName, snapshot, volumeTOs, guestOSType);
        this.vmState = vmState;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }
}
