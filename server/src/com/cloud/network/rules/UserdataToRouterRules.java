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

package com.cloud.network.rules;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;

import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

public class UserdataToRouterRules extends RuleApplier {

    private final NicProfile nic;
    private final VirtualMachineProfile profile;

    public UserdataToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        super(network);

        this.nic = nic;
        this.profile = profile;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        this.router = router;

        // for basic zone, send vm data/password information only to the router in the same pod
        final Commands cmds = new Commands(Command.OnError.Stop);
        //final NicVO nicVo = _nicDao.findById(nic.getId());

        //final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
        //final String zoneName = _dcDao.findById(router.getDataCenterId()).getName();

        //        cmds.addCommand(
        //                "vmdata",
        //                generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName, nic.getIp4Address(), vm.getHostName(), vm.getInstanceName(),
        //                        vm.getId(), vm.getUuid(), null, nic.getNetworkId()));

        return visitor.visit(this);
    }

    public NicProfile getNic() {
        return nic;
    }

    public VirtualMachineProfile getProfile() {
        return profile;
    }
}