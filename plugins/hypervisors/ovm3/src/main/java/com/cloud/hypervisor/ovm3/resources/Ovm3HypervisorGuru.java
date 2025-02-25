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

package com.cloud.hypervisor.ovm3.resources;

import javax.inject.Inject;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;

public class Ovm3HypervisorGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    private GuestOSDao guestOsDao;

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Ovm3;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(vm.getBootLoaderType());

        GuestOSVO guestOS = guestOsDao.findById(vm.getVirtualMachine()
                .getGuestOSId());
        to.setOs(guestOS.getDisplayName());

        return to;
    }

    @Override
    public boolean trackVmHostChange() {
        return true;
    }

    @Override
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        logger.debug("getCommandHostDelegation: " + cmd.getClass());
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(true);
        }
        return new Pair<Boolean, Long>(Boolean.FALSE, Long.valueOf(hostId));
    }
}
