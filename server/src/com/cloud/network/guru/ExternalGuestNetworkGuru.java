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
package com.cloud.network.guru;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesCidrsVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class ExternalGuestNetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(ExternalGuestNetworkGuru.class);
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    PortForwardingRulesDao _pfRulesDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    FirewallRulesDao _fwRulesDao;
    @Inject
    FirewallRulesCidrsDao _fwRulesCidrDao;

    public ExternalGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {IsolationMethod.GRE, IsolationMethod.L3, IsolationMethod.VLAN};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source
        // nat service
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated &&
            isMyIsolationMethod(physicalNetwork) && !offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        if (_networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Network.Service.Connectivity)) {
            return null;
        }

        NetworkVO config = (NetworkVO)super.design(offering, plan, userSpecified, owner);
        if (config == null) {
            return null;
        } else if (_networkModel.networkIsConfiguredForExternalNetworking(plan.getDataCenterId(), config.getId())) {
            /* In order to revert userSpecified network setup */
            config.setState(State.Allocated);
        }

        return config;
    }

    @Override
    public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        assert (config.getState() == State.Implementing) : "Why are we implementing " + config;

        if (_networkModel.areServicesSupportedInNetwork(config.getId(), Network.Service.Connectivity)) {
            return null;
        }

        if (!_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            return super.implement(config, offering, dest, context);
        }

        DataCenter zone = dest.getDataCenter();
        NetworkVO implemented =
            new NetworkVO(config.getTrafficType(), config.getMode(), config.getBroadcastDomainType(), config.getNetworkOfferingId(), State.Allocated,
                config.getDataCenterId(), config.getPhysicalNetworkId(), offering.getRedundantRouter());

        // Get a vlan tag
        int vlanTag;
        if (config.getBroadcastUri() == null) {
            String vnet =
                _dcDao.allocateVnet(zone.getId(), config.getPhysicalNetworkId(), config.getAccountId(), context.getReservationId(),
                    UseSystemGuestVlans.valueIn(config.getAccountId()));

            try {
                // when supporting more types of networks this need to become
//              int vlantag = Integer.parseInt(BroadcastDomainType.getValue(vnet));
                vlanTag = Integer.parseInt(vnet);
            } catch (NumberFormatException e) {
                throw new CloudRuntimeException("Obtained an invalid guest vlan tag. Exception: " + e.getMessage());
            }

            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlanTag));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), config.getAccountId(), EventVO.LEVEL_INFO,
                EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assigned Zone Vlan: " + vnet + " Network Id: " + config.getId(), 0);
        } else {
            vlanTag = Integer.parseInt(BroadcastDomainType.getValue(config.getBroadcastUri()));
            implemented.setBroadcastUri(config.getBroadcastUri());
        }

        // Determine the new gateway and CIDR
        String[] oldCidr = config.getCidr().split("/");
        String oldCidrAddress = oldCidr[0];
        int cidrSize = Integer.parseInt(oldCidr[1]);
        long newCidrAddress = (NetUtils.ip2Long(oldCidrAddress));
        // if the implementing network is for vpc, no need to generate newcidr, use the cidr that came from super cidr
        if (config.getVpcId() != null) {
            implemented.setGateway(config.getGateway());
            implemented.setCidr(config.getCidr());
            implemented.setState(State.Implemented);
        } else {
            // Determine the offset from the lowest vlan tag
            int offset = getVlanOffset(config.getPhysicalNetworkId(), vlanTag);
            cidrSize = getGloballyConfiguredCidrSize();
            // If the offset has more bits than there is room for, return null
            long bitsInOffset = 32 - Integer.numberOfLeadingZeros(offset);
            if (bitsInOffset > (cidrSize - 8)) {
                throw new CloudRuntimeException("The offset " + offset + " needs " + bitsInOffset + " bits, but only have " + (cidrSize - 8) + " bits to work with.");
            }
            newCidrAddress = (NetUtils.ip2Long(oldCidrAddress) & 0xff000000) | (offset << (32 - cidrSize));
            implemented.setGateway(NetUtils.long2Ip(newCidrAddress + 1));
            implemented.setCidr(NetUtils.long2Ip(newCidrAddress) + "/" + cidrSize);
            implemented.setState(State.Implemented);
        }

        // Mask the Ipv4 address of all nics that use this network with the new guest VLAN offset
        List<NicVO> nicsInNetwork = _nicDao.listByNetworkId(config.getId());
        for (NicVO nic : nicsInNetwork) {
            if (nic.getIPv4Address() != null) {
                long ipMask = getIpMask(nic.getIPv4Address(), cidrSize);
                nic.setIPv4Address(NetUtils.long2Ip(newCidrAddress | ipMask));
                _nicDao.persist(nic);
            }
        }

        // Mask the destination address of all port forwarding rules in this network with the new guest VLAN offset
        List<PortForwardingRuleVO> pfRulesInNetwork = _pfRulesDao.listByNetwork(config.getId());
        for (PortForwardingRuleVO pfRule : pfRulesInNetwork) {
            if (pfRule.getDestinationIpAddress() != null) {
                long ipMask = getIpMask(pfRule.getDestinationIpAddress().addr(), cidrSize);
                String maskedDestinationIpAddress = NetUtils.long2Ip(newCidrAddress | ipMask);
                pfRule.setDestinationIpAddress(new Ip(maskedDestinationIpAddress));
                _pfRulesDao.update(pfRule.getId(), pfRule);
            }
        }
        // Mask the destination address of all static nat rules in this network with the new guest VLAN offset
        // Here the private ip of the nic get updated. When secondary ip are present the gc will not triggered
        List<IPAddressVO> ipAddrsOfNw = _ipAddressDao.listStaticNatPublicIps(config.getId());
        for (IPAddressVO ip : ipAddrsOfNw) {
            if (ip.getVmIp() != null) {
                long ipMask = getIpMask(ip.getVmIp(), cidrSize);
                String maskedVmIp = NetUtils.long2Ip(newCidrAddress | ipMask);
                ip.setVmIp(maskedVmIp);
                _ipAddressDao.update(ip.getId(), ip);
            }
        }

        //Egress rules cidr is subset of guest nework cidr, we need to change
        List <FirewallRuleVO> fwEgressRules = _fwRulesDao.listByNetworkPurposeTrafficType(config.getId(), FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Egress);

        for (FirewallRuleVO rule: fwEgressRules) {
            //get the cidr list for this rule
            List<FirewallRulesCidrsVO>  fwRuleCidrsVo = _fwRulesCidrDao.listByFirewallRuleId(rule.getId());

            for (FirewallRulesCidrsVO ruleCidrvo: fwRuleCidrsVo) {
                String cidr = ruleCidrvo.getCidr();
                String cidrAddr =  cidr.split("/")[0];
                String size = cidr.split("/")[1];

                long ipMask = getIpMask(cidrAddr, cidrSize);
                String newIp = NetUtils.long2Ip(newCidrAddress | ipMask);
                String updatedCidr = newIp+"/"+size;

                ruleCidrvo.setSourceCidrList(updatedCidr);
                _fwRulesCidrDao.update(ruleCidrvo.getId(), ruleCidrvo);
            }

        }


        return implemented;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException {

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId()) && nic != null && nic.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }

        NicProfile profile = super.allocate(config, nic, vm);

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            profile.setReservationStrategy(ReservationStrategy.Start);
            /* We won't clear IP address, because router may set gateway as it IP, and it would be updated properly later */
            //profile.setIp4Address(null);
            profile.setIPv4Gateway(null);
            profile.setIPv4Netmask(null);
        }

        return profile;
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        super.deallocate(config, nic, vm);

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setIPv4Address(null);
            nic.setIPv4Gateway(null);
            nic.setIPv4Netmask(null);
            nic.setBroadcastUri(null);
            nic.setIsolationUri(null);
        }
    }

    @Override
    public void reserve(NicProfile nic, Network config, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        assert (nic.getReservationStrategy() == ReservationStrategy.Start) : "What can I do for nics that are not allocated at start? ";

        DataCenter dc = _dcDao.findById(config.getDataCenterId());

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setBroadcastUri(config.getBroadcastUri());
            nic.setIsolationUri(config.getBroadcastUri());
            nic.setIPv4Dns1(dc.getDns1());
            nic.setIPv4Dns2(dc.getDns2());
            nic.setIPv4Netmask(NetUtils.cidr2Netmask(config.getCidr()));
            long cidrAddress = NetUtils.ip2Long(config.getCidr().split("/")[0]);
            int cidrSize = getGloballyConfiguredCidrSize();
            nic.setIPv4Gateway(config.getGateway());

            if (nic.getIPv4Address() == null) {
                String guestIp = _ipAddrMgr.acquireGuestIpAddress(config, null);
                if (guestIp == null) {
                    throw new InsufficientVirtualNetworkCapacityException("Unable to acquire guest IP address for network " + config, DataCenter.class, dc.getId());
                }

                nic.setIPv4Address(guestIp);
            } else {
                long ipMask = NetUtils.ip2Long(nic.getIPv4Address()) & ~(0xffffffffffffffffl << (32 - cidrSize));
                nic.setIPv4Address(NetUtils.long2Ip(cidrAddress | ipMask));
            }
        } else {
            super.reserve(nic, config, vm, dest, context);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {

        NetworkVO network = _networkDao.findById(nic.getNetworkId());

        if (network != null && _networkModel.networkIsConfiguredForExternalNetworking(network.getDataCenterId(), network.getId())) {
            return true;
        } else {
            return super.release(nic, vm, reservationId);
        }
    }

    private long getIpMask(String ipAddress, long cidrSize) {
        return NetUtils.ip2Long(ipAddress) & ~(0xffffffffffffffffl << (32 - cidrSize));
    }

}
