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
package org.apache.cloudstack.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.dc.VlanDetailsVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetrisProviderVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.Vpc;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.netris.model.NatPostBody;
import org.apache.cloudstack.agent.api.AddOrUpdateNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisNatCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.NetrisAnswer;
import org.apache.cloudstack.agent.api.NetrisCommand;
import org.apache.cloudstack.agent.api.ReleaseNatIpCommand;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.resource.NetrisResourceObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class NetrisServiceImpl implements NetrisService, Configurable {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private NetrisProviderDao netrisProviderDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private PhysicalNetworkDao physicalNetworkDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private VlanDao vlanDao;
    @Inject
    private VlanDetailsDao vlanDetailsDao;

    @Override
    public String getConfigComponentName() {
        return NetrisService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    private NetrisProviderVO getZoneNetrisProvider(long zoneId) {
        NetrisProviderVO netrisProviderVO = netrisProviderDao.findByZoneId(zoneId);
        if (netrisProviderVO == null) {
            logger.error("No Netris controller was found!");
            throw new InvalidParameterValueException("Failed to find a Netris controller");
        }
        return netrisProviderVO;
    }

    private NetrisAnswer sendNetrisCommand(NetrisCommand cmd, long zoneId) {
        NetrisProviderVO netrisProviderVO = getZoneNetrisProvider(zoneId);
        Answer answer = agentMgr.easySend(netrisProviderVO.getHostId(), cmd);
        if (answer == null || !answer.getResult()) {
            logger.error("Netris API Command failed");
            throw new InvalidParameterValueException("Failed API call to Netris controller");
        }
        return (NetrisAnswer) answer;
    }

    /**
     * Calculate the minimum CIDR subnet containing the IP range (using the library: <a href="https://github.com/seancfoley/IPAddress">IPAddress</a>)
     * From: <a href="https://github.com/seancfoley/IPAddress/wiki/Code-Examples-3:-Subnetting-and-Other-Subnet-Operations#from-start-and-end-address-get-single-cidr-block-covering-both">Example</a>
     * @param ipRange format: startIP-endIP
     * @return the minimum CIDR containing the IP range
     */
    protected String calculateSubnetCidrFromIpRange(String ipRange) {
        if (StringUtils.isBlank(ipRange) || !ipRange.contains("-")) {
            return null;
        }
        String[] rangeArray = ipRange.split("-");
        String startIp = rangeArray[0];
        String endIp = rangeArray[1];
        IPAddress startIpAddress = new IPAddressString(startIp).getAddress();
        IPAddress endIpAddress = new IPAddressString(endIp).getAddress();
        return startIpAddress.coverWithPrefixBlock(endIpAddress).toPrefixLengthString();
    }

    /**
     * Prepare the Netris Public Range to be used by CloudStack after the zone is created and the Netris provider is added
     */
    public SetupNetrisPublicRangeCommand createSetupPublicRangeCommand(long zoneId, String gateway, String netmask, String ipRange) {
        String superCidr = NetUtils.getCidrFromGatewayAndNetmask(gateway, netmask);
        String subnetNatCidr = calculateSubnetCidrFromIpRange(ipRange);
        return new SetupNetrisPublicRangeCommand(zoneId, superCidr, subnetNatCidr);
    }

    @Override
    public boolean createIPAMAllocationsForZoneLevelPublicRanges(long zoneId) {
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        physicalNetworks = physicalNetworks.stream().filter(x -> x.getIsolationMethods().contains(Network.Provider.Netris.getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(physicalNetworks)) {
            return false;
        }
        for (PhysicalNetworkVO physicalNetwork : physicalNetworks) {
            List<IPAddressVO> publicIps = ipAddressDao.listByPhysicalNetworkId(physicalNetwork.getId());
            List<Long> vlanDbIds = publicIps.stream()
                    .filter(x -> !x.isForSystemVms())
                    .map(IPAddressVO::getVlanId)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(vlanDbIds)) {
                String msg = "Cannot find a public IP range VLAN range for the Netris Public traffic";
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            for (Long vlanDbId : vlanDbIds) {
                VlanVO vlanRecord = vlanDao.findById(vlanDbId);
                if (vlanRecord == null) {
                    logger.error("Cannot set up the Netris Public IP range as it cannot find the public range on database");
                    return false;
                }
                VlanDetailsVO vlanDetail = vlanDetailsDao.findDetail(vlanDbId, ApiConstants.NETRIS_DETAIL_KEY);
                if (vlanDetail == null) {
                    logger.debug("Skipping the Public IP range {} creation on Netris as it does not belong to the Netris Public IP Pool", vlanRecord.getIpRange());
                    continue;
                }
                String gateway = vlanRecord.getVlanGateway();
                String netmask = vlanRecord.getVlanNetmask();
                String ipRange = vlanRecord.getIpRange();
                SetupNetrisPublicRangeCommand cmd = createSetupPublicRangeCommand(zoneId, gateway, netmask, ipRange);
                NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
                if (!answer.getResult()) {
                    throw new CloudRuntimeException("Netris Public IP Range setup failed, please check the logs");
                }
            }
        }
        return true;
    }

    @Override
    public boolean createVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled, String cidr, boolean isVpc) {
        CreateNetrisVpcCommand cmd = new CreateNetrisVpcCommand(zoneId, accountId, domainId, vpcName, cidr, vpcId, isVpc);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteVpcResource(long zoneId, long accountId, long domainId, Vpc vpc) {
        DeleteNetrisVpcCommand cmd = new DeleteNetrisVpcCommand(zoneId, accountId, domainId, vpc.getName(), vpc.getCidr(), vpc.getId(), true);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean createVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr) {
        NetworkVO network = networkDao.findById(networkId);
        String vxlanId = Networks.BroadcastDomainType.getValue(network.getBroadcastUri());
        CreateNetrisVnetCommand cmd = new CreateNetrisVnetCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, cidr, network.getGateway(), !Objects.isNull(vpcId));
        cmd.setVxlanId(Integer.parseInt(vxlanId));
        NetrisProviderVO netrisProvider = netrisProviderDao.findByZoneId(zoneId);
        cmd.setNetrisTag(netrisProvider.getNetrisTag());
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteVnetResource(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr) {
        DeleteNetrisVnetCommand cmd = new DeleteNetrisVnetCommand(zoneId, accountId, domainId, networkName, networkId, vpcName, vpcId, cidr, Objects.nonNull(vpcName));
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean createSnatRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName, long networkId, boolean isForVpc, String vpcCidr, String snatIP) {
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, isForVpc, vpcCidr);
        cmd.setNatIp(snatIP);
        cmd.setNatRuleType("SNAT");
        String suffix;
        if (isForVpc) {
            suffix = String.valueOf(vpcId); // D1-A1-Z1-V25-SNAT
        } else {
            suffix = String.valueOf(networkId); // D1-A1-Z1-N25-SNAT
        }
        cmd.setProtocol(NatPostBody.ProtocolEnum.ALL.getValue());
        cmd.setState(NatPostBody.StateEnum.ENABLED.getValue());
        String snatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.SNAT, suffix);
        cmd.setNatRuleName(snatRuleName);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean createPortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, long vpcId, String networkName,
                                            Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule) {
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId,
                networkName, networkId, isForVpc, vpcCidr);
        cmd.setProtocol(networkRule.getProtocol().toLowerCase(Locale.ROOT));
        cmd.setDestinationAddress(networkRule.getPublicIp());
        cmd.setDestinationPort(networkRule.getPublicPort());
        cmd.setSourceAddress(networkRule.getVmIp());
        cmd.setSourcePort(networkRule.getPrivatePort());
        cmd.setState(NatPostBody.StateEnum.ENABLED.getValue());
        String ruleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.DNAT,
                String.valueOf(vpcId), String.format("R%s", networkRule.getRuleId()));
        cmd.setNatRuleName(ruleName);
        cmd.setNatRuleType("DNAT");

        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deletePortForwardingRule(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, boolean isForVpc, String vpcCidr, SDNProviderNetworkRule networkRule) {
        DeleteNetrisNatRuleCommand cmd = new DeleteNetrisNatRuleCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, isForVpc);
        String ruleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.DNAT,
                String.valueOf(vpcId), String.format("R%s", networkRule.getRuleId()));
        cmd.setNatRuleType("DNAT");
        cmd.setNatRuleName(ruleName);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address) {
        if (vpc == null || address == null) {
            return false;
        }
        long accountId = vpc.getAccountId();
        long domainId = vpc.getDomainId();
        long zoneId = vpc.getZoneId();
        long vpcId = vpc.getId();
        String vpcName = vpc.getName();

        logger.debug("Updating the source NAT IP for Netris VPC {} to IP: {}", vpc.getName(), address.getAddress().addr());

        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, null, null, true, address.getAddress().addr());
        cmd.setNatRuleType("SNAT");
        String snatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.SNAT, String.valueOf(vpcId));
        cmd.setNatRuleName(snatRuleName);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        if (!answer.getResult()) {
            logger.error("Could not update the source NAT IP address for VPC {}: {}", vpc.getName(), answer.getDetails());
            return false;
        }
        return answer.getResult();
    }

    @Override
    public boolean createStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String vpcCidr, String staticNatIp, String vmIp) {
        String vpcName = null;
        String networkName = null;
        Long vpcId = null;
        Long networkId = null;
        if (isForVpc) {
            vpcName = networkResourceName;
            vpcId = networkResourceId;
        } else {
            networkName = networkResourceName;
            networkId = networkResourceId;
        }
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, isForVpc, vpcCidr);
        cmd.setNatRuleType("STATICNAT");
        cmd.setNatIp(staticNatIp);
        cmd.setVmIp(vmIp);
        String suffix = getResourceSuffix(vpcId, networkId, isForVpc);
        String dnatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.STATICNAT, suffix);
        cmd.setNatRuleName(dnatRuleName);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteStaticNatRule(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String staticNatIp) {
        String vpcName = null;
        String networkName = null;
        Long vpcId = null;
        Long networkId = null;
        if (isForVpc) {
            vpcName = networkResourceName;
            vpcId = networkResourceId;
        } else {
            networkName = networkResourceName;
            networkId = networkResourceId;
        }
        DeleteNetrisNatRuleCommand cmd = new DeleteNetrisNatRuleCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, isForVpc);
        String suffix = getResourceSuffix(vpcId, networkId, isForVpc);
        String dnatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.STATICNAT, suffix);
        cmd.setNatRuleName(dnatRuleName);
        cmd.setNatRuleType("STATICNAT");
        cmd.setNatIp(staticNatIp);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean addOrUpdateStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId, boolean updateRoute) {
        AddOrUpdateNetrisStaticRouteCommand cmd = new AddOrUpdateNetrisStaticRouteCommand(zoneId, accountId, domainId, networkResourceName, networkResourceId, isForVpc, prefix, nextHop, routeId, updateRoute);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteStaticRoute(long zoneId, long accountId, long domainId, String networkResourceName, Long networkResourceId, boolean isForVpc, String prefix, String nextHop, Long routeId) {
        DeleteNetrisStaticRouteCommand cmd = new DeleteNetrisStaticRouteCommand(zoneId, accountId, domainId, networkResourceName, networkResourceId, isForVpc, prefix, nextHop, routeId);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean releaseNatIp(long zoneId, String publicIp) {
        ReleaseNatIpCommand cmd = new ReleaseNatIpCommand(zoneId, null, null, null, null, false, publicIp);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    private String getResourceSuffix(Long vpcId, Long networkId, boolean isForVpc) {
        String suffix;
        if (isForVpc) {
            suffix = String.valueOf(vpcId); // D1-A1-Z1-V25-STATICNAT or D1-A1-Z1-V25-SNAT
        } else {
            suffix = String.valueOf(networkId); // D1-A1-Z1-N25-STATICNAT or D1-A1-Z1-N25-SNAT
        }
        return suffix;
    }
}
