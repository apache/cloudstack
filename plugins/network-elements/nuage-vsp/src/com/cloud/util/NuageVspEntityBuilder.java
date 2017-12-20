//
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
//

package com.cloud.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspAddressRange;
import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspDhcpVMOption;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspDomainCleanUp;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;
import net.nuage.vsp.acs.client.common.model.Pair;

public class NuageVspEntityBuilder {
    private static final Logger s_logger = Logger.getLogger(NuageVspEntityBuilder.class);

    @Inject
    NetworkDao _networkDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    VlanDao _vlanDao;
    @Inject
    VlanDetailsDao _vlanDetailsDao;
    @Inject
    ConfigurationDao _configurationDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    NuageVspManager _nuageVspManager;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;


    public VspDomain buildVspDomain(Domain domain) {
        return new VspDomain.Builder()
                .uuid(domain.getUuid())
                .name(domain.getName())
                .path(domain.getPath())
                .build();
    }

    public VspDomainCleanUp buildVspDomainCleanUp(Domain domain) {
        VspDomainCleanUp.Builder vspDomainCleanUpBuilder = new VspDomainCleanUp.Builder().uuid(domain.getUuid());

        Map<String, List<String>> sharedNetworkUuids = Maps.newHashMap();
        List<NetworkVO> allSharedNetworks = _networkDao.listByGuestType(Network.GuestType.Shared);
        for (NetworkVO sharedNetwork : allSharedNetworks) {
            if (_networkModel.isNetworkAvailableInDomain(sharedNetwork.getId(), domain.getId())) {
                NetworkOffering networkOffering = _networkOfferingDao.findById(sharedNetwork.getNetworkOfferingId());
                String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configurationDao, _networkDetailsDao, sharedNetwork, networkOffering);
                if (!sharedNetworkUuids.containsKey(preConfiguredDomainTemplateName)) {
                    sharedNetworkUuids.put(preConfiguredDomainTemplateName, Lists.<String>newArrayList());
                }
                sharedNetworkUuids.get(preConfiguredDomainTemplateName).add(sharedNetwork.getUuid());
            }
        }
        vspDomainCleanUpBuilder.sharedNetworkUuids(sharedNetworkUuids);

        return vspDomainCleanUpBuilder.build();
    }

    public VspNetwork buildVspNetwork(Network network) {
        return buildVspNetwork(network.getDomainId(), network);
    }

    public VspNetwork buildVspNetwork(long domainId, Network network) {
        VspNetwork.Builder vspNetworkBuilder = new VspNetwork.Builder()
                .id(network.getId())
                .uuid(network.getUuid())
                .name(network.getName())
                .cidr(network.getCidr())
                .gateway(network.getGateway());

        DomainVO domain = _domainDao.findById(domainId);
        VspDomain vspDomain = buildVspDomain(domain);
        vspNetworkBuilder.domain(vspDomain);

        AccountVO account = _accountDao.findById(network.getAccountId());
        if (account != null) {
            vspNetworkBuilder.accountUuid(account.getUuid()).accountName(account.getAccountName());
        }

        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        vspNetworkBuilder.egressDefaultPolicy(networkOffering.getEgressDefaultPolicy()).publicAccess(networkOffering.getSupportsPublicAccess());

        if (network.getVpcId() != null) {
            VpcVO vpc = _vpcDao.findById(network.getVpcId());
            vspNetworkBuilder.vpcUuid(vpc.getUuid())
                    .vpcName(vpc.getName())
                    .networkType(VspNetwork.NetworkType.Vpc);
        } else {
            if (networkOffering.getGuestType() == Network.GuestType.Shared) {
                List<VlanVO> vlans = _vlanDao.listVlansByNetworkIdIncludingRemoved(network.getId());
                List<VspAddressRange> vspAddressRanges = Lists.transform(vlans, new Function<VlanVO, VspAddressRange>() {
                    @Nullable
                    @Override
                    public VspAddressRange apply(VlanVO vlanVO) {
                        return new VspAddressRange.Builder().gateway(vlanVO.getVlanGateway()).netmask(vlanVO.getVlanNetmask()).build();
                    }
                });

                vspNetworkBuilder.networkType(VspNetwork.NetworkType.Shared).addressRanges(vspAddressRanges);
            } else if (_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Network.Service.SourceNat)
                    || _networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Network.Service.StaticNat)) {
                vspNetworkBuilder.networkType(VspNetwork.NetworkType.L3);
            } else {
                vspNetworkBuilder.networkType(VspNetwork.NetworkType.L2);
            }
        }

        boolean firewallServiceSupported = _networkModel.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Network.Service.Firewall);
        vspNetworkBuilder.firewallServiceSupported(firewallServiceSupported);

        String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configurationDao, _networkDetailsDao, network, networkOffering);
        vspNetworkBuilder.domainTemplateName(preConfiguredDomainTemplateName);

        if (usesVirtualRouter(networkOffering.getId())) {
            try {
                List<Pair<String, String>> ipAddressRanges =
                        networkOffering.getGuestType() == Network.GuestType.Shared ? getSharedIpAddressRanges(network.getId()) : getIpAddressRanges(network);
                String virtualRouterIp = getVirtualRouterIP(network, ipAddressRanges);
                vspNetworkBuilder.virtualRouterIp(virtualRouterIp);
            } catch (InsufficientVirtualNetworkCapacityException ex) {
                s_logger.error("There is an insufficient network capacity in network " + network.getId(), ex);
                throw new CloudRuntimeException("There is an insufficient network capacity in network " + network.getId(), ex);
            }
        }

        return vspNetworkBuilder.build();
    }

    public boolean usesVirtualRouter(long networkOfferingId) {
        return _networkOfferingServiceMapDao.isProviderForNetworkOffering(networkOfferingId, Network.Provider.VirtualRouter) ||
                _networkOfferingServiceMapDao.isProviderForNetworkOffering(networkOfferingId, Network.Provider.VPCVirtualRouter);
    }

    public VspNetwork updateVspNetworkByPublicIp(VspNetwork vspNetwork, Network network, String publicIp) {
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
        final long ip = NetUtils.ip2Long(publicIp);
        VlanVO matchingVlan = Iterables.find(vlans, new Predicate<VlanVO>() {
            @Override
            public boolean apply(@Nullable VlanVO vlan) {
                Pair<String, String> ipAddressRange = getIpAddressRange(vlan);
                long startIp = NetUtils.ip2Long(ipAddressRange.getLeft());
                long endIp = NetUtils.ip2Long(ipAddressRange.getRight());
                return startIp <= ip && ip <= endIp;
            }
        });

        return new VspNetwork.Builder().fromObject(vspNetwork)
                .gateway(matchingVlan.getVlanGateway())
                .cidr(NetUtils.getCidrFromGatewayAndNetmask(matchingVlan.getVlanGateway(), matchingVlan.getVlanNetmask()))
                .build();
    }

    private List<Pair<String, String>> getSharedIpAddressRanges(long networkId) {
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
        List<Pair<String, String>> ipAddressRanges = Lists.newArrayList();
        for (VlanVO vlan : vlans) {
            Pair<String, String> ipAddressRange = getIpAddressRange(vlan);
            if (ipAddressRange != null) {
                ipAddressRanges.add(ipAddressRange);
            }
        }
        return ipAddressRanges;
    }

    private List<Pair<String, String>> getIpAddressRanges(Network network) {
        List<Pair<String, String>> ipAddressRanges = Lists.newArrayList();
        String subnet = NetUtils.getCidrSubNet(network.getCidr());
        String netmask = NetUtils.getCidrNetmask(network.getCidr());
        long cidrSize = NetUtils.getCidrSize(netmask);
        Set<Long> allIPsInCidr = NetUtils.getAllIpsFromCidr(subnet, cidrSize, new HashSet<Long>());
        if (allIPsInCidr == null || !(allIPsInCidr instanceof TreeSet)) {
            throw new IllegalStateException("The IPs in CIDR for subnet " + subnet + " where null or returned in a non-ordered set.");
        }

        Iterator<Long> ipIterator = allIPsInCidr.iterator();
        long ip =  ipIterator.next();
        long gatewayIp = NetUtils.ip2Long(network.getGateway());
        String lastIp = NetUtils.getIpRangeEndIpFromCidr(subnet, cidrSize);
        if (gatewayIp == ip) {
            ip = ipIterator.next();
            ipAddressRanges.add(Pair.of(NetUtils.long2Ip(ip), lastIp));
        } else if (!network.getGateway().equals(lastIp)) {
            ipAddressRanges.add(Pair.of(NetUtils.long2Ip(ip), NetUtils.long2Ip(gatewayIp - 1)));
            ipAddressRanges.add(Pair.of(NetUtils.long2Ip(gatewayIp + 1), lastIp));
        } else {
            ipAddressRanges.add(Pair.of(NetUtils.long2Ip(ip), NetUtils.long2Ip(gatewayIp - 1)));
        }

        return ipAddressRanges;
    }

    public Pair<String, String> getIpAddressRange(Vlan vlan) {
        boolean isIpv4 = StringUtils.isNotBlank(vlan.getIpRange());
        String[] range = isIpv4 ? vlan.getIpRange().split("-") : vlan.getIp6Range().split("-");
        if (range.length == 2) {
            return Pair.of(range[0], range[1]);
        }
        return null;
    }

    private String getVirtualRouterIP(Network network, List<Pair<String, String>> ipAddressRanges) throws InsufficientVirtualNetworkCapacityException {
        if (network.getBroadcastUri() != null) {
            return network.getBroadcastUri().getPath().substring(1);
        }

        Pair<String, String> lowestIpAddressRange = null;
        long ipCount = 0;
        if (ipAddressRanges.size() == 1) {
            lowestIpAddressRange = Iterables.getOnlyElement(ipAddressRanges);
            ipCount = NetUtils.ip2Long(lowestIpAddressRange.getRight()) - NetUtils.ip2Long(lowestIpAddressRange.getLeft()) + 1;
        } else {
            for (Pair<String, String> ipAddressRange : ipAddressRanges) {
                if (lowestIpAddressRange == null || NetUtils.ip2Long(ipAddressRange.getLeft()) < NetUtils.ip2Long(lowestIpAddressRange.getLeft())) {
                    lowestIpAddressRange = ipAddressRange;
                }
                ipCount += NetUtils.ip2Long(ipAddressRange.getRight()) - NetUtils.ip2Long(ipAddressRange.getLeft()) + 1;
            }
        }

        if (ipCount == 0) {
            throw new InsufficientVirtualNetworkCapacityException("VSP allocates an IP for VirtualRouter." + " But no ip address ranges are specified", Network.class,
                    network.getId());
        } else if (ipCount < 3) {
            throw new InsufficientVirtualNetworkCapacityException("VSP allocates an IP for VirtualRouter." + " So, subnet should have atleast minimum 3 hosts", Network.class,
                    network.getId());
        }

        String virtualRouterIp = lowestIpAddressRange.getLeft();
        long lowestIp = NetUtils.ip2Long(lowestIpAddressRange.getLeft());
        lowestIp = lowestIp + 1;
        lowestIpAddressRange.setLeft(NetUtils.long2Ip(lowestIp));
        return virtualRouterIp;
    }

    public VspVm buildVspVm(VirtualMachine vm, Network network) {
        VspVm.Builder vspVmBuilder = new VspVm.Builder()
                .uuid(vm.getUuid())
                .name(vm.getInstanceName());

        switch (vm.getState()) {
            case Starting:
                vspVmBuilder.state(VspVm.State.Starting); break;
            case Running:
                vspVmBuilder.state(VspVm.State.Running); break;
            case Stopping:
                vspVmBuilder.state(VspVm.State.Stopping); break;
            case Stopped:
                vspVmBuilder.state(VspVm.State.Stopped); break;
            case Destroyed:
                vspVmBuilder.state(VspVm.State.Destroyed); break;
            case Expunging:
                vspVmBuilder.state(VspVm.State.Expunging); break;
            case Migrating:
                vspVmBuilder.state(VspVm.State.Migrating); break;
            case Error:
                vspVmBuilder.state(VspVm.State.Error); break;
            case Shutdowned:
                vspVmBuilder.state(VspVm.State.Shutdowned); break;
            default:
                vspVmBuilder.state(VspVm.State.Unknown);
        }

        boolean isDomainRouter = vm.getType().equals(VirtualMachine.Type.DomainRouter);
        vspVmBuilder.domainRouter(isDomainRouter);

        if (network.getBroadcastUri() != null) {
            String domainRouterIp = network.getBroadcastUri().getPath().substring(1);
            vspVmBuilder.domainRouterIp(domainRouterIp);
        }

        return vspVmBuilder.build();
    }

    public VspNic buildVspNic(String nicUuid, NicProfile nicProfile) {
        return buildVspNic(nicUuid, nicProfile.getMacAddress(), nicProfile.getIPv4Address(), nicProfile.getNetworkId());
    }

    public VspNic buildVspNic(NicVO nic) {
        return buildVspNic(nic.getUuid(), nic.getMacAddress(), nic.getIPv4Address(), nic.getNetworkId());
    }

    private VspNic buildVspNic(String uuid, String macAddress, String ip, long networkId) {
        VspNic.Builder vspNicBuilder = new VspNic.Builder()
                .uuid(uuid)
                .macAddress(macAddress)
                .useStaticIp(true)
                .ip(ip);

        Network network = _networkDao.findById(networkId);
        NetworkOffering networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());

        return vspNicBuilder.build();
    }

    public VspStaticNat buildVspStaticNat(Boolean forRevoke, IPAddressVO staticNatIp, VlanVO staticNatVlan, VspNic vspNic) {
        VspStaticNat.Builder vspStaticNatBuilder = new VspStaticNat.Builder()
                .ipUuid(staticNatIp.getUuid())
                .ipAddress(staticNatIp.getAddress().addr())
                .revoke(forRevoke)
                .oneToOneNat(staticNatIp.isOneToOneNat())
                .state(getEnumValue(staticNatIp.getState(), VspStaticNat.State.class))
                .vlanUuid(staticNatVlan.getUuid())
                .vlanGateway(staticNatVlan.getVlanGateway())
                .vlanNetmask(staticNatVlan.getVlanNetmask())
                .vlanUnderlay(NuageVspUtil.isUnderlayEnabledForVlan(_vlanDetailsDao, staticNatVlan));

        if (vspNic != null) {
            vspStaticNatBuilder.nic(vspNic);
        }

        return vspStaticNatBuilder.build();
    }

    public VspStaticNat buildVspStaticNat(Boolean forRevoke, IPAddressVO staticNatIp, VlanVO staticNatVlan, NicVO nic) {
        VspNic vspNic = (nic != null) ?buildVspNic(nic) : null;
        return buildVspStaticNat(forRevoke, staticNatIp, staticNatVlan, vspNic);
    }

    public VspAclRule buildVspAclRule(FirewallRule firewallRule, Network network) {
        VspAclRule.Builder vspAclRuleBuilder = new VspAclRule.Builder()
                .uuid(firewallRule.getUuid())
                .protocol(firewallRule.getProtocol())
                .startPort(firewallRule.getSourcePortStart())
                .endPort(firewallRule.getSourcePortEnd())
                .sourceCidrList(firewallRule.getSourceCidrList())
                .priority(-1)
                .type(VspAclRule.ACLType.Firewall);

        switch (firewallRule.getState()) {
            case Active:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Active); break;
            case Add:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Add); break;
            case Revoke:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Revoke);
        }

        switch (firewallRule.getTrafficType()) {
            case Ingress:
                vspAclRuleBuilder.trafficType(VspAclRule.ACLTrafficType.Ingress); break;
            case Egress:
                vspAclRuleBuilder.trafficType(VspAclRule.ACLTrafficType.Egress);
        }

        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Egress && networkOffering.getEgressDefaultPolicy()) {
            vspAclRuleBuilder.action(VspAclRule.ACLAction.Deny);
        } else {
            vspAclRuleBuilder.action(VspAclRule.ACLAction.Allow);
        }

        if (firewallRule.getSourceIpAddressId() != null) {
            IPAddressVO ipAddress = _ipAddressDao.findById(firewallRule.getSourceIpAddressId());
            if (ipAddress != null) {
                vspAclRuleBuilder.sourceIpAddress(ipAddress.getVmIp() + "/32");
            }
        }

        return vspAclRuleBuilder.build();
    }

    public VspAclRule buildVspAclRule(NetworkACLItem networkAcl) {
        VspAclRule.Builder vspAclRuleBuilder = new VspAclRule.Builder()
                .uuid(networkAcl.getUuid())
                .protocol(networkAcl.getProtocol())
                .startPort(networkAcl.getSourcePortStart())
                .endPort(networkAcl.getSourcePortEnd())
                .sourceIpAddress(null)
                .sourceCidrList(networkAcl.getSourceCidrList())
                .priority(networkAcl.getNumber())
                .type(VspAclRule.ACLType.NetworkACL);

        switch (networkAcl.getState()) {
            case Active:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Active); break;
            case Add:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Add); break;
            case Revoke:
                vspAclRuleBuilder.state(VspAclRule.ACLState.Revoke);
        }

        switch (networkAcl.getTrafficType()) {
            case Ingress:
                vspAclRuleBuilder.trafficType(VspAclRule.ACLTrafficType.Ingress); break;
            case Egress:
                vspAclRuleBuilder.trafficType(VspAclRule.ACLTrafficType.Egress);
        }

        switch (networkAcl.getAction()) {
            case Allow:
                vspAclRuleBuilder.action(VspAclRule.ACLAction.Allow); break;
            case Deny:
                vspAclRuleBuilder.action(VspAclRule.ACLAction.Deny);
        }

        return vspAclRuleBuilder.build();
    }

    /** Build VspDhcpVMOption to put on the VM interface */
    public VspDhcpVMOption buildVmDhcpOption (NicVO userNic, boolean defaultHasDns, boolean networkHasDns) {
        VMInstanceVO userVm  = _vmInstanceDao.findById(userNic.getInstanceId());
        VspDhcpVMOption.Builder vspDhcpVMOptionBuilder = new VspDhcpVMOption.Builder()
                .nicUuid(userNic.getUuid())
                .defaultHasDns(defaultHasDns)
                .hostname(userVm.getHostName())
                .networkHasDns(networkHasDns)
                .isDefaultInterface(userNic.isDefaultNic())
                .domainRouter(VirtualMachine.Type.DomainRouter.equals(userNic.getVmType()));
        return vspDhcpVMOptionBuilder.build();
    }

    /** Build VspDhcpVMOption to put on the subnet */
    public VspDhcpDomainOption buildNetworkDhcpOption(Network network, NetworkOffering offering) {
        List<String> dnsProvider = _ntwkOfferingSrvcDao.listProvidersForServiceForNetworkOffering(offering.getId(), Network.Service.Dns);
        boolean isVrDnsProvider = dnsProvider.contains("VirtualRouter") || dnsProvider.contains("VpcVirtualRouter");
        VspDhcpDomainOption.Builder vspDhcpDomainBuilder = new VspDhcpDomainOption.Builder()
                .dnsServers(_nuageVspManager.getDnsDetails(network.getDataCenterId()))
                .vrIsDnsProvider(isVrDnsProvider);

        if (isVrDnsProvider) {
            vspDhcpDomainBuilder.networkDomain(network.getVpcId() != null ? _vpcDao.findById(network.getVpcId()).getNetworkDomain() : network.getNetworkDomain());
        }

        return vspDhcpDomainBuilder.build();
    }

    private <E extends Enum<E>> E getEnumValue(Enum cloudstackValue, Class<E> target) {
        try {
            return Enum.valueOf(target, cloudstackValue.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private <E extends Enum<E>> E getEnumValue(Enum cloudstackValue, E defaultValue) {
        try {
            return Enum.valueOf(defaultValue.getDeclaringClass(), cloudstackValue.name());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
