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

import java.util.List;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.vm.VirtualMachine.State;

public class AdvancedVpnRules extends BasicVpnRules {

    private static final Logger s_logger = Logger.getLogger(AdvancedVpnRules.class);

    private final RemoteAccessVpn _remoteAccessVpn;

    public AdvancedVpnRules(final RemoteAccessVpn remoteAccessVpn, final List<? extends VpnUser> users) {
        super(null, users);
        _remoteAccessVpn = remoteAccessVpn;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        VpcDao vpcDao = visitor.getVirtualNetworkApplianceFactory().getVpcDao();
        Vpc vpc = vpcDao.findById(_remoteAccessVpn.getVpcId());

        if (_router.getState() != State.Running) {
            s_logger.warn("Failed to add/remove Remote Access VPN users: router not in running state");
            throw new ResourceUnavailableException("Failed to add/remove Remote Access VPN users: router not in running state: " + router.getState(), DataCenter.class,
                    vpc.getZoneId());
        }

        return visitor.visit(this);
    }
}
