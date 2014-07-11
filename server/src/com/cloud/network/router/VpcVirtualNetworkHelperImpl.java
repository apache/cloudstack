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
package com.cloud.network.router;


import java.net.URI;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.server.ConfigurationServer;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;


@Component
// This will not be a public service anymore, but a helper for the only public service
@Local(value = {VpcVirtualNetworkHelperImpl.class})
public class VpcVirtualNetworkHelperImpl {

    String _name;
    @Inject
    NetworkService _ntwkService;
    @Inject
    NetworkACLManager _networkACLMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    EntityManager _entityMgr;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    FirewallRulesDao _rulesDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NicDao _nicDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;

    protected NetworkGeneralHelper nwHelper = new NetworkGeneralHelper();


    //@Override
    public List<DomainRouterVO> getVpcRouters(long vpcId) {
        return _routerDao.listByVpcId(vpcId);
    }

    //@Override
    @DB
    public NicProfile createPrivateNicProfileForGateway(VpcGateway privateGateway) {
        Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
        PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(privateNetwork.getDataCenterId(), privateNetwork.getId(), privateGateway.getIp4Address());
        Nic privateNic = _nicDao.findByIp4AddressAndNetworkId(ipVO.getIpAddress(), privateNetwork.getId());

        NicProfile privateNicProfile = new NicProfile();

        if (privateNic != null) {
            VirtualMachine vm = _vmDao.findById(privateNic.getInstanceId());
            privateNicProfile =
                new NicProfile(privateNic, privateNetwork, privateNic.getBroadcastUri(), privateNic.getIsolationUri(), _networkModel.getNetworkRate(
                    privateNetwork.getId(), vm.getId()), _networkModel.isSecurityGroupSupportedInNetwork(privateNetwork), _networkModel.getNetworkTag(
                    vm.getHypervisorType(), privateNetwork));
        } else {
            String netmask = NetUtils.getCidrNetmask(privateNetwork.getCidr());
            PrivateIpAddress ip =
                new PrivateIpAddress(ipVO, privateNetwork.getBroadcastUri().toString(), privateNetwork.getGateway(), netmask,
                    NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress())));

            URI netUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
            privateNicProfile.setIp4Address(ip.getIpAddress());
            privateNicProfile.setGateway(ip.getGateway());
            privateNicProfile.setNetmask(ip.getNetmask());
            privateNicProfile.setIsolationUri(netUri);
            privateNicProfile.setBroadcastUri(netUri);
            // can we solve this in setBroadcastUri()???
            // or more plugable construct is desirable
            privateNicProfile.setBroadcastType(BroadcastDomainType.getSchemeValue(netUri));
            privateNicProfile.setFormat(AddressFormat.Ip4);
            privateNicProfile.setReservationId(String.valueOf(ip.getBroadcastUri()));
            privateNicProfile.setMacAddress(ip.getMacAddress());
        }

        return privateNicProfile;
    }

}
