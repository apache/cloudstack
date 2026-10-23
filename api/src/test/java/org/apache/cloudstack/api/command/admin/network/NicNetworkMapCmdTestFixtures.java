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
package org.apache.cloudstack.api.command.admin.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.utils.db.EntityManager;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine;

/**
 * Shared mock-entity setup for the nic-network-map admin commands (AssociateNetworkToNicCmd,
 * DisassociateNetworkFromNicCmd, ChangeNicPrimaryNetworkCmd), which all resolve their owning
 * account the same way: look up the Nic by id, then the VirtualMachine by the nic's instance id.
 */
final class NicNetworkMapCmdTestFixtures {

    private NicNetworkMapCmdTestFixtures() {
    }

    static Nic mockNic(EntityManager entityMgr, long nicId, long instanceId) {
        Nic nic = mock(Nic.class);
        when(nic.getInstanceId()).thenReturn(instanceId);
        when(entityMgr.findById(Nic.class, nicId)).thenReturn(nic);
        return nic;
    }

    static void mockMissingNic(EntityManager entityMgr, long nicId) {
        when(entityMgr.findById(Nic.class, nicId)).thenReturn(null);
    }

    static VirtualMachine mockVm(EntityManager entityMgr, long vmId, long accountId) {
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getAccountId()).thenReturn(accountId);
        when(entityMgr.findById(VirtualMachine.class, vmId)).thenReturn(vm);
        return vm;
    }

    static void mockMissingVm(EntityManager entityMgr, long vmId) {
        when(entityMgr.findById(VirtualMachine.class, vmId)).thenReturn(null);
    }
}
