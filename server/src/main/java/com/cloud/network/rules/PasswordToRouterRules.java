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

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

public class PasswordToRouterRules extends RuleApplier {

    private final NicProfile nic;
    private final VirtualMachineProfile profile;

    private NicVO nicVo;

    public PasswordToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        super(network);

        this.nic = nic;
        this.profile = profile;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        UserVmDao userVmDao = visitor.getVirtualNetworkApplianceFactory().getUserVmDao();
        userVmDao.loadDetails((UserVmVO) profile.getVirtualMachine());
        // for basic zone, send vm data/password information only to the router in the same pod
        NicDao nicDao = visitor.getVirtualNetworkApplianceFactory().getNicDao();
        nicVo = nicDao.findById(nic.getId());

        return visitor.visit(this);
    }

    public VirtualMachineProfile getProfile() {
        return profile;
    }

    public NicVO getNicVo() {
        return nicVo;
    }
}
