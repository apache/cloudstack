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

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.utils.Pair;

/**
 * used to tell the agent to migrate a vm to a different primary storage pool.
 * It is for now only implemented on Vmware and is supposed to work irrespective of whether the VM is started or not.
 *
 */
public class MigrateVmToPoolCommand extends Command {
    private String vmName;
    private boolean executeInSequence = false;
    private List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerAsList;
    private String hostGuidInTargetCluster;

    protected MigrateVmToPoolCommand() {
    }

    /**
     *
     * @param vmName the name of the VM to migrate
     * @param volumeToFilerTo the volume to primary storage pool map to migrate the VM to
     * @param hostGuidInTargetCluster GUID of host in target cluster when migrating across clusters
     * @param executeInSequence
     */
    public MigrateVmToPoolCommand(String vmName, List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerTo,
                                  String hostGuidInTargetCluster, boolean executeInSequence) {
        this.vmName = vmName;
        this.hostGuidInTargetCluster = hostGuidInTargetCluster;
        this.volumeToFilerAsList = volumeToFilerTo;
        this.executeInSequence = executeInSequence;
    }

    public String getVmName() {
        return vmName;
    }

    public List<Pair<VolumeTO, StorageFilerTO>> getVolumeToFilerAsList() {
        return volumeToFilerAsList;
    }

    public String getHostGuidInTargetCluster() {
        return hostGuidInTargetCluster;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

}
