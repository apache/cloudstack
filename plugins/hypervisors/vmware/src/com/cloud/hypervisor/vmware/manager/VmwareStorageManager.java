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
package com.cloud.hypervisor.vmware.manager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.storage.*;

public interface VmwareStorageManager {
    Answer execute(VmwareHostService hostService, PrimaryStorageDownloadCommand cmd);
    Answer execute(VmwareHostService hostService, BackupSnapshotCommand cmd);
    Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromVolumeCommand cmd);
    Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromSnapshotCommand cmd);
    Answer execute(VmwareHostService hostService, CopyVolumeCommand cmd);
    Answer execute(VmwareHostService hostService, CreateVolumeFromSnapshotCommand cmd);
    Answer execute(VmwareHostService hostService, CreateVMSnapshotCommand cmd);
    Answer execute(VmwareHostService hostService, DeleteVMSnapshotCommand cmd);
    Answer execute(VmwareHostService hostService, RevertToVMSnapshotCommand cmd);
    boolean execute(VmwareHostService hostService, CreateEntityDownloadURLCommand cmd);
    public void createOva(String path, String name);
}
