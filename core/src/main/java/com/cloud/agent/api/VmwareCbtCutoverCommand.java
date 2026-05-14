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

import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;

public class VmwareCbtCutoverCommand extends Command {

    private String migrationUuid;
    private RemoteInstanceTO sourceInstance;
    private List<VmwareCbtDiskTO> disks;
    private int finalCycleNumber;
    private boolean runVirtV2vFinalization;
    private String vddkLibDir;
    private String vddkTransports;
    private String vddkThumbprint;

    public VmwareCbtCutoverCommand() {
    }

    public VmwareCbtCutoverCommand(String migrationUuid, RemoteInstanceTO sourceInstance, List<VmwareCbtDiskTO> disks,
                                   int finalCycleNumber, boolean runVirtV2vFinalization) {
        this.migrationUuid = migrationUuid;
        this.sourceInstance = sourceInstance;
        this.disks = disks;
        this.finalCycleNumber = finalCycleNumber;
        this.runVirtV2vFinalization = runVirtV2vFinalization;
    }

    public String getMigrationUuid() {
        return migrationUuid;
    }

    public RemoteInstanceTO getSourceInstance() {
        return sourceInstance;
    }

    public List<VmwareCbtDiskTO> getDisks() {
        return disks;
    }

    public int getFinalCycleNumber() {
        return finalCycleNumber;
    }

    public boolean getRunVirtV2vFinalization() {
        return runVirtV2vFinalization;
    }

    public String getVddkLibDir() {
        return vddkLibDir;
    }

    public void setVddkLibDir(String vddkLibDir) {
        this.vddkLibDir = vddkLibDir;
    }

    public String getVddkTransports() {
        return vddkTransports;
    }

    public void setVddkTransports(String vddkTransports) {
        this.vddkTransports = vddkTransports;
    }

    public String getVddkThumbprint() {
        return vddkThumbprint;
    }

    public void setVddkThumbprint(String vddkThumbprint) {
        this.vddkThumbprint = vddkThumbprint;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
