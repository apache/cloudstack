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

import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.NicProfile;

public class DhcpPvlanRules extends RuleApplier {

    private final boolean _isAddPvlan;
    private final NicProfile _nic;

    private PvlanSetupCommand _setupCommand;

    public DhcpPvlanRules(final boolean isAddPvlan, final NicProfile nic) {
        super(null);

        _isAddPvlan = isAddPvlan;
        _nic = nic;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        String op = "add";
        if (!_isAddPvlan) {
            op = "delete";
        }

        NetworkDao networkDao = visitor.getVirtualNetworkApplianceFactory().getNetworkDao();
        final Network network = networkDao.findById(_nic.getNetworkId());

        NetworkModel networkModel = visitor.getVirtualNetworkApplianceFactory().getNetworkModel();
        final String networkTag = networkModel.getNetworkTag(_router.getHypervisorType(), network);

        _setupCommand = PvlanSetupCommand.createDhcpSetup(op, _nic.getBroadCastUri(), networkTag, _router.getInstanceName(), _nic.getMacAddress(), _nic.getIPv4Address());

        return visitor.visit(this);
    }

    public PvlanSetupCommand getSetupCommand() {
        return _setupCommand;
    }
}
