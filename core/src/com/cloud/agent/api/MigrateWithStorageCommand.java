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

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.utils.Pair;

public class MigrateWithStorageCommand extends Command {
    VirtualMachineTO vm;
    Map<VolumeTO, StorageFilerTO> volumeToFiler;
    List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerAsList;
    String tgtHost;

    public MigrateWithStorageCommand(VirtualMachineTO vm, Map<VolumeTO, StorageFilerTO> volumeToFiler) {
        this.vm = vm;
        this.volumeToFiler = volumeToFiler;
        this.volumeToFilerAsList = null;
        this.tgtHost = null;
    }

    public MigrateWithStorageCommand(VirtualMachineTO vm, List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerAsList) {
        this.vm = vm;
        this.volumeToFiler = null;
        this.volumeToFilerAsList = volumeToFilerAsList;
        this.tgtHost = null;
    }

    public MigrateWithStorageCommand(VirtualMachineTO vm, Map<VolumeTO, StorageFilerTO> volumeToFiler, String tgtHost) {
        this.vm = vm;
        this.volumeToFiler = volumeToFiler;
        this.volumeToFilerAsList = null;
        this.tgtHost = tgtHost;
    }

    public MigrateWithStorageCommand(VirtualMachineTO vm, List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerAsList, String tgtHost) {
        this.vm = vm;
        this.volumeToFiler = null;
        this.volumeToFilerAsList = volumeToFilerAsList;
        this.tgtHost = tgtHost;
    }

    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }

    public Map<VolumeTO, StorageFilerTO> getVolumeToFiler() {
        return volumeToFiler;
    }

    public List<Pair<VolumeTO, StorageFilerTO>> getVolumeToFilerAsList() {
        return volumeToFilerAsList;
    }

    public String getTargetHost() {
        return tgtHost;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
