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

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.nsx.NsxService;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.CreateNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxPortForwardRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.DeleteNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.cloudstack.utils.NsxHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

public class NsxServiceImpl implements NsxService, Configurable {
    @Inject
    NsxControllerUtils nsxControllerUtils;
    @Inject
    VpcDao vpcDao;

    protected Logger logger = LogManager.getLogger(getClass());

    public boolean createVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId, vpcId, vpcName, true, sourceNatEnabled);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxTier1GatewayCommand, zoneId);
        return result.getResult();
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

        logger.debug(String.format("Updating the source NAT IP for NSX VPC %s to IP: %s", vpc.getName(), address.getAddress().addr()));
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(domainId, accountId, zoneId, vpcId, true);
        String sourceNatRuleId = NsxControllerUtils.getNsxNatRuleId(domainId, accountId, zoneId, vpcId, true);
        CreateOrUpdateNsxTier1NatRuleCommand cmd = NsxHelper.createOrUpdateNsxNatRuleCommand(domainId, accountId, zoneId, tier1GatewayName, "SNAT", address.getAddress().addr(), sourceNatRuleId);
        NsxAnswer answer = nsxControllerUtils.sendNsxCommand(cmd, zoneId);
        if (!answer.getResult()) {
            logger.error(String.format("Could not update the source NAT IP address for VPC %s: %s", vpc.getName(), answer.getDetails()));
            return false;
        }
        return true;
    }

    public boolean createNetwork(Long zoneId, long accountId, long domainId, Long networkId, String networkName) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId, networkId, networkName, false, false);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName) {
        DeleteNsxTier1GatewayCommand deleteNsxTier1GatewayCommand =
                new DeleteNsxTier1GatewayCommand(domainId, accountId, zoneId, vpcId, vpcName, true);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteNetwork(long zoneId, long accountId, long domainId, NetworkVO network) {
        String vpcName = null;
        if (Objects.nonNull(network.getVpcId())) {
            VpcVO vpc = vpcDao.findById(network.getVpcId());
            vpcName = Objects.nonNull(vpc) ? vpc.getName() : null;
        }
        DeleteNsxSegmentCommand deleteNsxSegmentCommand = new DeleteNsxSegmentCommand(domainId, accountId, zoneId,
                network.getVpcId(), vpcName, network.getId(), network.getName());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxSegmentCommand, network.getDataCenterId());
        if (!result.getResult()) {
            String msg = String.format("Could not remove the NSX segment for network %s: %s", network.getName(), result.getDetails());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        if (Objects.isNull(network.getVpcId())) {
            DeleteNsxTier1GatewayCommand deleteNsxTier1GatewayCommand = new DeleteNsxTier1GatewayCommand(domainId, accountId, zoneId, network.getId(), network.getName(), false);
            result = nsxControllerUtils.sendNsxCommand(deleteNsxTier1GatewayCommand, zoneId);
        }
        return result.getResult();
    }

    public boolean createStaticNatRule(long zoneId, long domainId, long accountId, Long networkResourceId, String networkResourceName,
                                       boolean isVpcResource, long vmId, String publicIp, String vmIp) {
        CreateNsxStaticNatCommand createNsxStaticNatCommand = new CreateNsxStaticNatCommand(domainId, accountId, zoneId,
                networkResourceId, networkResourceName, isVpcResource, vmId, publicIp, vmIp);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxStaticNatCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteStaticNatRule(long zoneId, long domainId, long accountId, Long networkResourceId, String networkResourceName,
                                       boolean isVpcResource) {
        DeleteNsxNatRuleCommand deleteNsxStaticNatCommand = new DeleteNsxNatRuleCommand(domainId, accountId, zoneId,
                networkResourceId, networkResourceName, isVpcResource, null, null, null, null);
        deleteNsxStaticNatCommand.setService(Network.Service.StaticNat);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxStaticNatCommand, zoneId);
        return result.getResult();
    }

    public NsxAnswer createPortForwardRule(NsxNetworkRule netRule) {
        // TODO: if port doesn't exist in default list of services, create a service entry
        CreateNsxPortForwardRuleCommand createPortForwardCmd = new CreateNsxPortForwardRuleCommand(netRule.getDomainId(),
                netRule.getAccountId(), netRule.getZoneId(), netRule.getNetworkResourceId(),
                netRule.getNetworkResourceName(), netRule.isVpcResource(), netRule.getVmId(), netRule.getRuleId(),
                netRule.getPublicIp(), netRule.getVmIp(), netRule.getPublicPort(), netRule.getPrivatePort(), netRule.getProtocol());
        return nsxControllerUtils.sendNsxCommand(createPortForwardCmd, netRule.getZoneId());
    }

    public boolean deletePortForwardRule(NsxNetworkRule netRule) {
        DeleteNsxNatRuleCommand deleteCmd = new DeleteNsxNatRuleCommand(netRule.getDomainId(),
                netRule.getAccountId(), netRule.getZoneId(), netRule.getNetworkResourceId(),
                netRule.getNetworkResourceName(), netRule.isVpcResource(),  netRule.getVmId(), netRule.getRuleId(), netRule.getPrivatePort(), netRule.getProtocol());
        deleteCmd.setService(Network.Service.PortForwarding);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteCmd, netRule.getZoneId());
        return result.getResult();
    }

    public boolean createLbRule(NsxNetworkRule netRule) {
        CreateNsxLoadBalancerRuleCommand command = new CreateNsxLoadBalancerRuleCommand(netRule.getDomainId(),
                netRule.getAccountId(), netRule.getZoneId(), netRule.getNetworkResourceId(),
                netRule.getNetworkResourceName(), netRule.isVpcResource(),  netRule.getMemberList(), netRule.getRuleId(),
                netRule.getPublicPort(), netRule.getPrivatePort(), netRule.getAlgorithm(), netRule.getProtocol());
        command.setPublicIp(netRule.getPublicIp());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(command, netRule.getZoneId());
        return result.getResult();
    }

    public boolean deleteLbRule(NsxNetworkRule netRule) {
        DeleteNsxLoadBalancerRuleCommand command = new DeleteNsxLoadBalancerRuleCommand(netRule.getDomainId(),
                netRule.getAccountId(), netRule.getZoneId(), netRule.getNetworkResourceId(),
                netRule.getNetworkResourceName(), netRule.isVpcResource(),  netRule.getMemberList(), netRule.getRuleId(),
                netRule.getVmId());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(command, netRule.getZoneId());
        return result.getResult();
    }

    public boolean addFirewallRules(Network network, List<NsxNetworkRule> netRules) {
        CreateNsxDistributedFirewallRulesCommand command = new CreateNsxDistributedFirewallRulesCommand(network.getDomainId(),
                network.getAccountId(), network.getDataCenterId(), network.getVpcId(), network.getId(), netRules);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(command, network.getDataCenterId());
        return result.getResult();
    }

    public boolean deleteFirewallRules(Network network, List<NsxNetworkRule> netRules) {
        DeleteNsxDistributedFirewallRulesCommand command = new DeleteNsxDistributedFirewallRulesCommand(network.getDomainId(),
                network.getAccountId(), network.getDataCenterId(), network.getVpcId(), network.getId(), netRules);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(command, network.getDataCenterId());
        return result.getResult();
    }

    @Override
    public String getConfigComponentName() {
        return NsxApiClient.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            NSX_API_FAILURE_RETRIES, NSX_API_FAILURE_INTERVAL
        };
    }
}
