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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.cloud.network.router.deployment.RouterDeploymentDefinition;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;


@Component
// This will not be a public service anymore, but a helper for the only public service
@Local(value = {VpcNetworkHelperImpl.class})
public class VpcNetworkHelperImpl extends NetworkHelperImpl implements VpcNetworkHelper {

    private static final Logger s_logger = Logger.getLogger(VpcNetworkHelperImpl.class);

    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private PrivateIpDao _privateIpDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    protected VpcManager _vpcMgr;


    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcNetworkHelper#createGuestNicProfileForVpcRouter(com.cloud.network.Network)
     */
    @Override
    public NicProfile createGuestNicProfileForVpcRouter(final Network guestNetwork) {
        NicProfile guestNic = new NicProfile();
        guestNic.setIp4Address(guestNetwork.getGateway());
        guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
        guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
        guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
        guestNic.setMode(guestNetwork.getMode());
        String gatewayCidr = guestNetwork.getCidr();
        guestNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));

        return guestNic;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.router.VpcNetworkHelper#createVpcRouterNetworks(org.cloud.network.router.deployment.VpcRouterDeploymentDefinition)
     */
    @Override
    public LinkedHashMap<Network, List<? extends NicProfile>> createRouterNetworks(
            final RouterDeploymentDefinition vpcRouterDeploymentDefinition)
                    throws ConcurrentOperationException, InsufficientAddressCapacityException {

        final TreeSet<String> publicVlans = new TreeSet<String>();
        publicVlans.add(vpcRouterDeploymentDefinition.getSourceNatIP().getVlanTag());

        //1) allocate nic for control and source nat public ip
        final LinkedHashMap<Network, List<? extends NicProfile>> networks =
                super.createRouterNetworks(vpcRouterDeploymentDefinition);


        final Long vpcId = vpcRouterDeploymentDefinition.getVpc().getId();
        //2) allocate nic for private gateways if needed
        final List<PrivateGateway> privateGateways = this._vpcMgr.getVpcPrivateGateways(vpcId);
        if (privateGateways != null && !privateGateways.isEmpty()) {
            for (PrivateGateway privateGateway : privateGateways) {
                NicProfile privateNic = this.createPrivateNicProfileForGateway(privateGateway);
                Network privateNetwork = _networkModel.getNetwork(privateGateway.getNetworkId());
                networks.put(privateNetwork, new ArrayList<NicProfile>(Arrays.asList(privateNic)));
            }
        }

        //3) allocate nic for guest gateway if needed
        List<? extends Network> guestNetworks = this._vpcMgr.getVpcNetworks(vpcId);
        for (Network guestNetwork : guestNetworks) {
            if (_networkModel.isPrivateGateway(guestNetwork.getId())) {
                continue;
            }
            if (guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup) {
                NicProfile guestNic = createGuestNicProfileForVpcRouter(guestNetwork);
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
            }
        }

        //4) allocate nic for additional public network(s)
        final List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(vpcId, false);
        final List<NicProfile> publicNics = new ArrayList<NicProfile>();
        Network publicNetwork = null;
        for (IPAddressVO ip : ips) {
            PublicIp publicIp = PublicIp.createFromAddrAndVlan(ip, this._vlanDao.findById(ip.getVlanId()));
            if ((ip.getState() == IpAddress.State.Allocated || ip.getState() == IpAddress.State.Allocating) && this._vpcMgr.isIpAllocatedToVpc(ip) &&
                    !publicVlans.contains(publicIp.getVlanTag())) {
                s_logger.debug("Allocating nic for router in vlan " + publicIp.getVlanTag());
                NicProfile publicNic = new NicProfile();
                publicNic.setDefaultNic(false);
                publicNic.setIp4Address(publicIp.getAddress().addr());
                publicNic.setGateway(publicIp.getGateway());
                publicNic.setNetmask(publicIp.getNetmask());
                publicNic.setMacAddress(publicIp.getMacAddress());
                publicNic.setBroadcastType(BroadcastDomainType.Vlan);
                publicNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(publicIp.getVlanTag()));
                publicNic.setIsolationUri(IsolationType.Vlan.toUri(publicIp.getVlanTag()));
                NetworkOffering publicOffering = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemPublicNetwork).get(0);
                if (publicNetwork == null) {
                    List<? extends Network> publicNetworks = _networkMgr.setupNetwork(VirtualNwStatus.account,
                            publicOffering, vpcRouterDeploymentDefinition.getPlan(), null, null, false);
                    publicNetwork = publicNetworks.get(0);
                }
                publicNics.add(publicNic);
                publicVlans.add(publicIp.getVlanTag());
            }
        }
        if (publicNetwork != null) {
            if (networks.get(publicNetwork) != null) {
                List<NicProfile> publicNicProfiles = (List<NicProfile>)networks.get(publicNetwork);
                publicNicProfiles.addAll(publicNics);
                networks.put(publicNetwork, publicNicProfiles);
            } else {
                networks.put(publicNetwork, publicNics);
            }
        }

        return networks;
    }
}