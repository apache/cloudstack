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
package com.cloud.hypervisor;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface HypervisorGuru extends Adapter {
    ConfigKey<Boolean> VmwareFullClone = new ConfigKey<Boolean>("Advanced", Boolean.class, "vmware.create.full.clone", "true",
            "If set to true, creates guest VMs as full clones on ESX", false);
    HypervisorType getHypervisorType();

    /**
     * Convert from a virtual machine to the
     * virtual machine that the hypervisor expects.
     * @param vm
     * @return
     */
    VirtualMachineTO implement(VirtualMachineProfile vm);

    /**
     * Gives hypervisor guru opportunity to decide if certain commands need to be delegated to another host, for instance, we may have the opportunity to change from a system VM (is considered a host) to a real host to execute commands.
     *
     * @param hostId original hypervisor host
     * @param cmd command that is going to be sent, hypervisor guru usually needs to register various context objects into the command object
     *
     * @return delegated host id if the command will be delegated
     */
    Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd);

    /**
     *  @return true if VM can be migrated independently with CloudStack, and therefore CloudStack needs to track and reflect host change
     *  into CloudStack database, false if CloudStack enforces VM sync logic
     *
     */
    boolean trackVmHostChange();

    /**
     * @param profile
     * @return
     */
    NicTO toNicTO(NicProfile profile);

    /**
     * Give hypervisor guru opportunity to decide if certain command needs to be done after expunge VM from DB
     * @param vm
     * @return a list of Commands
     */
    List<Command> finalizeExpunge(VirtualMachine vm);

    /**
     * Give the hypervisor guru the opportinity to decide if additional clean is
     * required for nics before expunging the VM
     *
     */
    List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics);

    List<Command> finalizeExpungeVolumes(VirtualMachine vm);

    Map<String, String> getClusterSettings(long vmId);

    VirtualMachine importVirtualMachineFromBackup(long zoneId, long domainId, long accountId, long userId,
                                                  String vmInternalName, Backup backup) throws Exception;

    boolean attachRestoredVolumeToVirtualMachine(long zoneId, String location, Backup.VolumeInfo volumeInfo,
                                                 VirtualMachine vm, long poolId, Backup backup) throws Exception;
    /**
     * Will generate commands to migrate a vm to a pool. For now this will only work for stopped VMs on Vmware.
     *
     * @param vm the stopped vm to migrate
     * @param destination the primary storage pool to migrate to
     * @return a list of commands to perform for a successful migration
     */
    List<Command> finalizeMigrate(VirtualMachine vm, Map<Volume, StoragePool> volumeToPool);
}
