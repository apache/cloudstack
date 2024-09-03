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

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.vm.VirtualMachine;

import java.util.List;

public class RestoreBackupCommand extends Command  {
    private String vmName;
    private String backupPath;
    private String backupRepoType;
    private String backupRepoAddress;
    private List<String> volumePaths;
    private String diskType;
    private Boolean vmExists;
    private String restoreVolumeUUID;
    private VirtualMachine.State vmState;

    protected RestoreBackupCommand() {
        super();
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getBackupRepoType() {
        return backupRepoType;
    }

    public void setBackupRepoType(String backupRepoType) {
        this.backupRepoType = backupRepoType;
    }

    public String getBackupRepoAddress() {
        return backupRepoAddress;
    }

    public void setBackupRepoAddress(String backupRepoAddress) {
        this.backupRepoAddress = backupRepoAddress;
    }

    public List<String> getVolumePaths() {
        return volumePaths;
    }

    public void setVolumePaths(List<String> volumePaths) {
        this.volumePaths = volumePaths;
    }

    public Boolean isVmExists() {
        return vmExists;
    }

    public void setVmExists(Boolean vmExists) {
        this.vmExists = vmExists;
    }

    public String getDiskType() {
        return diskType;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }

    public String getMountOptions() {
        return mountOptions;
    }

    public void setMountOptions(String mountOptions) {
        this.mountOptions = mountOptions;
    }

    public String getRestoreVolumeUUID() {
        return restoreVolumeUUID;
    }

    public void setRestoreVolumeUUID(String restoreVolumeUUID) {
        this.restoreVolumeUUID = restoreVolumeUUID;
    }

    public VirtualMachine.State getVmState() {
        return vmState;
    }

    public void setVmState(VirtualMachine.State vmState) {
        this.vmState = vmState;
    }

    @LogLevel(LogLevel.Log4jLevel.Off)
    private String mountOptions;
    @Override

    public boolean executeInSequence() {
        return true;
    }
}
