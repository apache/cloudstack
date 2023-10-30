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

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxPortForwardRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Objects;

public class NsxServiceImpl implements NsxService {
    @Inject
    NsxControllerUtils nsxControllerUtils;
    @Inject
    VpcDao vpcDao;
    @Inject
    NetworkDao networkDao;

    private static final Logger LOGGER = Logger.getLogger(NsxServiceImpl.class);

    public boolean createVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId, vpcId, vpcName, true);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }

    public boolean createNetwork(Long zoneId, long accountId, long domainId, Long networkId, String networkName) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId, networkId, networkName, false);
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
            String msg = String.format("Could not remove the NSX segment for network %s", network.getName());
            LOGGER.error(msg);
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

    public boolean createPortForwardRule(NsxNetworkRule netRule) {
        // TODO: if port doesn't exist in default list of services, create a service entry
        CreateNsxPortForwardRuleCommand createPortForwardCmd = new CreateNsxPortForwardRuleCommand(netRule.getDomainId(),
                netRule.getAccountId(), netRule.getZoneId(), netRule.getNetworkResourceId(),
                netRule.getNetworkResourceName(), netRule.isVpcResource(), netRule.getVmId(), netRule.getRuleId(),
                netRule.getPublicIp(), netRule.getVmIp(), netRule.getPublicPort(), netRule.getPrivatePort(), netRule.getProtocol());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createPortForwardCmd, netRule.getZoneId());
        return result.getResult();
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
                netRule.getPublicPort(), netRule.getAlgorithm(), netRule.getProtocol());
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
}
