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

import javax.inject.Inject;

import com.cloud.utils.exception.CloudRuntimeException;
import org.cloud.network.router.deployment.RouterDeploymentDefinition;

import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;


public class NicProfileHelperImpl implements NicProfileHelper {

    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private PrivateIpDao _privateIpDao;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected VpcManager _vpcMgr;
    @Inject
    protected NicDao _nicDao;
    @Inject
    protected IpAddressManager _ipAddrMgr;

    @Override
    @DB
    public NicProfile createPrivateNicProfileForGateway(final VpcGateway privateGateway, final VirtualRouter router) {
        final Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());

        PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(privateNetwork.getDataCenterId(), privateNetwork.getId(), privateGateway.getIp4Address());

        final Long vpcId = privateGateway.getVpcId();
        if (ipVO == null) {
            ipVO = _privateIpDao.findByIpAndVpcId(vpcId, privateGateway.getIp4Address());
            if (ipVO == null) {
                throw new CloudRuntimeException("cannot find IP address " + privateGateway.getIp4Address() + " to reuse for private gateway on vpc (id==" + vpcId + ")");
            }
        }

        Nic privateNic = null;

        if (ipVO != null) {
            privateNic = _nicDao.findByIp4AddressAndNetworkId(ipVO.getIpAddress(), privateNetwork.getId());
        }

        NicProfile privateNicProfile = new NicProfile();

        if (privateNic != null) {
            privateNicProfile =
                    new NicProfile(privateNic, privateNetwork, privateNic.getBroadcastUri(), privateNic.getIsolationUri(), _networkModel.getNetworkRate(
                            privateNetwork.getId(), router.getId()), _networkModel.isSecurityGroupSupportedInNetwork(privateNetwork), _networkModel.getNetworkTag(
                                    router.getHypervisorType(), privateNetwork));
            privateNicProfile.setDeviceId(null);

            if (router.getIsRedundantRouter()) {
              String newMacAddress = NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress(), NetworkModel.MACIdentifier.value()));
              privateNicProfile.setMacAddress(newMacAddress);
            }
        } else {
            final String netmask = NetUtils.getCidrNetmask(privateNetwork.getCidr());
            final PrivateIpAddress ip =
                    new PrivateIpAddress(ipVO, privateNetwork.getBroadcastUri().toString(), privateNetwork.getGateway(), netmask,
                            NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress(), NetworkModel.MACIdentifier.value())));

            final URI netUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
            privateNicProfile.setIPv4Address(ip.getIpAddress());
            privateNicProfile.setIPv4Gateway(ip.getGateway());
            privateNicProfile.setIPv4Netmask(ip.getNetmask());
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

    @Override
    public NicProfile createGuestNicProfileForVpcRouter(final RouterDeploymentDefinition vpcRouterDeploymentDefinition, final Network guestNetwork) {
        final NicProfile guestNic = new NicProfile();

        if (vpcRouterDeploymentDefinition.isRedundant()) {
            guestNic.setIPv4Address(this.acquireGuestIpAddressForVrouterRedundant(guestNetwork));
        } else {
            guestNic.setIPv4Address(guestNetwork.getGateway());
        }

        guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
        guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
        guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
        guestNic.setMode(guestNetwork.getMode());
        final String gatewayCidr = _networkModel.getValidNetworkCidr(guestNetwork);
        guestNic.setIPv4Netmask(NetUtils.getCidrNetmask(gatewayCidr));

        return guestNic;
    }

    public String acquireGuestIpAddressForVrouterRedundant(Network network) {
        return _ipAddrMgr.acquireGuestIpAddressByPlacement(network, null);
    }

}
