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

import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.NetworkOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class DhcpEntryRules extends RuleApplier {

    private final NicProfile _nic;
    private final VirtualMachineProfile _profile;
    private final DeployDestination _destination;

    private NicVO _nicVo;
    private UserVmVO _userVM;

    public DhcpEntryRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination destination) {
        super(network);

        _nic = nic;
        _profile = profile;
        _destination = destination;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        _userVM = _userVmDao.findById(_profile.getId());
        _userVmDao.loadDetails(_userVM);
        _nicVo = _nicDao.findById(_nic.getId());

        return visitor.visit(this);
    }

    public VirtualMachineProfile getProfile() {
        return _profile;
    }

    public DeployDestination getDestination() {
        return _destination;
    }

    public NicVO getNicVo() {
        return _nicVo;
    }

    public UserVmVO getUserVM() {
        return _userVM;
    }

    public void createDhcpEntryCommand(final VirtualRouter router, final UserVm vm, final NicVO nic, final Commands cmds) {
        final DhcpEntryCommand dhcpCommand =
                new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), vm.getHostName(), nic.getIp6Address(), _networkModel.getExecuteInSeqNtwkElmtCmd());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        final Nic defaultNic = findGatewayIp(vm.getId());
        String gatewayIp = defaultNic.getGateway();
        if (gatewayIp != null && !gatewayIp.equals(nic.getGateway())) {
            gatewayIp = "0.0.0.0";
        }
        dhcpCommand.setDefaultRouter(gatewayIp);
        dhcpCommand.setIp6Gateway(nic.getIp6Gateway());
        String ipaddress = null;
        final NicVO domrDefaultNic = findDefaultDnsIp(vm.getId());
        if (domrDefaultNic != null) {
            ipaddress = domrDefaultNic.getIp4Address();
        }
        dhcpCommand.setDefaultDns(ipaddress);
        dhcpCommand.setDuid(NetUtils.getDuidLL(nic.getMacAddress()));
        dhcpCommand.setDefault(nic.isDefaultNic());

        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(nic.getNetworkId(), router.getId()));
        dhcpCommand.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("dhcp", dhcpCommand);
    }

    private NicVO findGatewayIp(final long userVmId) {
        final NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);
        return defaultNic;
    }

    private NicVO findDefaultDnsIp(final long userVmId) {
        final NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);

        //check if DNS provider is the domR
        if (!_networkModel.isProviderSupportServiceInNetwork(defaultNic.getNetworkId(), Service.Dns, Provider.VirtualRouter)) {
            return null;
        }

        final NetworkOffering offering = _networkOfferingDao.findById(_networkDao.findById(defaultNic.getNetworkId()).getNetworkOfferingId());
        if (offering.getRedundantRouter()) {
            return findGatewayIp(userVmId);
        }

        final DataCenter dc = _dcDao.findById(_networkModel.getNetwork(defaultNic.getNetworkId()).getDataCenterId());
        final boolean isZoneBasic = (dc.getNetworkType() == NetworkType.Basic);

        //find domR's nic in the network
        NicVO domrDefaultNic;
        if (isZoneBasic) {
            domrDefaultNic = _nicDao.findByNetworkIdTypeAndGateway(defaultNic.getNetworkId(), VirtualMachine.Type.DomainRouter, defaultNic.getGateway());
        } else {
            domrDefaultNic = _nicDao.findByNetworkIdAndType(defaultNic.getNetworkId(), VirtualMachine.Type.DomainRouter);
        }
        return domrDefaultNic;
    }
}