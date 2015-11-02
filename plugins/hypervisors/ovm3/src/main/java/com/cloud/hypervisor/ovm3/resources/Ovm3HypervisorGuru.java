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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = HypervisorGuru.class)
public class Ovm3HypervisorGuru extends HypervisorGuruBase implements HypervisorGuru {
    private final Logger LOGGER = Logger.getLogger(Ovm3HypervisorGuru.class);
    @Inject
    GuestOSDao guestOsDao;
    @Inject
    EndPointSelector endPointSelector;
    @Inject
    HostDao hostDao;

    protected Ovm3HypervisorGuru() {
        super();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Ovm3;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(vm.getBootLoaderType());

        // Determine the VM's OS description
        GuestOSVO guestOS = guestOsDao.findById(vm.getVirtualMachine()
                .getGuestOSId());
        to.setOs(guestOS.getDisplayName());

        return to;
    }

    @Override
    public boolean trackVmHostChange() {
        return true;
    }

    /* I dislike the notion of having to place this here, and not being able to just override
     *
     * (non-Javadoc)
     * @see com.cloud.hypervisor.HypervisorGuruBase#getCommandHostDelegation(long, com.cloud.agent.api.Command)
     */
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        LOGGER.debug("getCommandHostDelegation: " + cmd.getClass());
        performSideEffectsForDelegationOnCommand(hostId, cmd);
        return new Pair<Boolean, Long>(Boolean.FALSE, Long.valueOf(hostId));
    }

    /**
     * @param hostId
     * @param cmd
     */
    void performSideEffectsForDelegationOnCommand(long hostId, Command cmd) {
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(true);
        }
        if (cmd instanceof CopyCommand) {
            CopyCommand cpyCommand = (CopyCommand)cmd;
            DataTO srcData = cpyCommand.getSrcTO();
            DataTO destData = cpyCommand.getDestTO();

            if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.TEMPLATE) {
                LOGGER.debug("Snapshot to Template: " + cmd);
                DataStoreTO srcStore = srcData.getDataStore();
                DataStoreTO destStore = destData.getDataStore();
                if (srcStore instanceof NfsTO && destStore instanceof NfsTO) {
                    HostVO host = hostDao.findById(hostId);
                    EndPoint ep = endPointSelector.selectHypervisorHost(new ZoneScope(host.getDataCenterId()));
                    host = hostDao.findById(ep.getId());
                    hostDao.loadDetails(host);
                }
            }
        }
    }
}
