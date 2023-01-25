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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.security.TungstenSecurityGroupRule;
import com.cloud.network.security.TungstenSecurityGroupRuleVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupRuleDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.dao.TungstenSecurityGroupRuleDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.PolicyEntriesType;
import net.juniper.tungsten.api.types.PolicyRuleType;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkGatewayToLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDefaultProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetworkDnsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenConnectedNetworkFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNicCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkGatewayFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenVmFromSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenDefaultSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerSslCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerStatsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenVrouterConfigCommand;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricAddressGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricApplicationPolicySetResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricLBHealthMonitorResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricLogicalRouterResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNicResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricServiceGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagTypeResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricVmResponse;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorDao;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
import org.apache.cloudstack.network.tungsten.model.TungstenLogicalRouter;
import org.apache.cloudstack.network.tungsten.model.TungstenModel;
import org.apache.cloudstack.network.tungsten.model.TungstenNetworkPolicy;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.cloudstack.network.tungsten.model.TungstenTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class TungstenServiceImpl extends ManagerBase implements TungstenService, Configurable {
    private static final Logger s_logger = Logger.getLogger(TungstenServiceImpl.class);

    private static final String NETWORK = "network";

    @Inject
    protected MessageBus messageBus;
    @Inject
    protected ProjectDao projectDao;
    @Inject
    protected AccountDao accountDao;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected ConfigurationDao configDao;
    @Inject
    protected AccountManager accountMgr;
    @Inject
    protected IPAddressDao ipAddressDao;
    @Inject
    protected EntityManager entityMgr;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected DomainDao domainDao;
    @Inject
    protected LoadBalancerCertMapDao lbCertMapDao;
    @Inject
    protected FirewallRulesDao fwRulesDao;
    @Inject
    protected TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;
    @Inject
    protected TungstenProviderDao tungstenProviderDao;
    @Inject
    protected TungstenFabricUtils tungstenFabricUtils;
    @Inject
    protected AgentManager agentMgr;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected NetworkDetailsDao networkDetailsDao;
    @Inject
    protected SecurityGroupDao securityGroupDao;
    @Inject
    protected NicDao nicDao;
    @Inject
    protected TungstenSecurityGroupRuleDao tungstenSecurityGroupRuleDao;
    @Inject
    protected SecurityGroupVMMapDao securityGroupVMMapDao;
    @Inject
    protected SecurityGroupRuleDao securityGroupRuleDao;
    @Inject
    protected SecurityGroupManager securityGroupManager;
    @Inject
    protected NicSecondaryIpDao nicSecIpDao;
    @Inject
    protected DataCenterIpAddressDao dataCenterIpAddressDao;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected IpAddressManager ipAddressManager;
    @Inject
    protected TungstenFabricLBHealthMonitorDao tungstenFabricLBHealthMonitorDao;
    @Inject
    protected LoadBalancingRulesService loadBalancingRulesService;
    @Inject
    LoadBalancerDao loadBalancerDao;

    @Override
    public boolean start() {
        subscribeTungstenEvent();
        return true;
    }

    @Override
    public boolean synchronizeTungstenData(Long tungstenProviderId) {
        TungstenProviderVO tungstenProviderVO = tungstenProviderDao.findById(tungstenProviderId);
        return tungstenProviderVO != null && syncTungstenDbWithCloudstackProjectsAndDomains();
    }

    @Override
    public void subscribeTungstenEvent() {
        subscribeIpAddressEvent();
        subscribeNetworkPolicyEvent();
        subscribeVlanEvent();
        subscribePopEvent();
        subscribeDomainEvent();
        subscribeProjectEvent();
        subscribeSecurityGroupEvent();
        subscribeSecondaryNicEvent();
        subscribeSynchonizeEvent();
    }

    private void subscribeSynchonizeEvent() {
        messageBus.subscribe(TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT,
                (senderAddress, subject, args) -> {
                    try {
                        syncTungstenDbWithCloudstackProjectsAndDomains();
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                });
    }

    private void subscribeIpAddressEvent() {
        messageBus.subscribe(IpAddressManager.MESSAGE_ASSIGN_IPADDR_EVENT, (senderAddress, subject, args) -> {
            try {
                final IpAddress ipAddress = (IpAddress) args;
                long zoneId = ipAddress.getDataCenterId();
                TungstenProvider tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);
                if (!ipAddress.isSourceNat() && tungstenProvider != null) {
                    createTungstenFloatingIp(zoneId, ipAddress);
                }
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(IpAddressManager.MESSAGE_RELEASE_IPADDR_EVENT, (senderAddress, subject, args) -> {
            try {
                final IpAddress ipAddress = (IpAddress) args;
                if (!ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                    long zoneId = ipAddress.getDataCenterId();
                    TungstenProvider tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);
                    if (tungstenProvider != null) {
                        deleteTungstenFloatingIp(zoneId, ipAddress);
                    }
                }
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeNetworkPolicyEvent() {
        messageBus.subscribe(TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT, (senderAddress, subject, args) -> {
            try {
                final Network network = (Network) args;
                List<IPAddressVO> ipAddressVOList = ipAddressDao.listByAccount(Account.ACCOUNT_ID_SYSTEM);
                for (IPAddressVO ipAddressVO : ipAddressVOList) {
                    ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand =
                            new ApplyTungstenNetworkPolicyCommand(
                                    null, TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()),
                                    network.getUuid(), 1,
                                    0);
                    tungstenFabricUtils.sendTungstenCommand(applyTungstenNetworkPolicyCommand,
                            network.getDataCenterId());
                }
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeVlanEvent() {
        messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_VLAN_IP_RANGE_EVENT,
                (senderAddress, subject, args) -> {
            try {
                final VlanVO vlanVO = (VlanVO) args;
                addPublicNetworkSubnet(vlanVO);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_VLAN_IP_RANGE_EVENT,
         (senderAddress, subject, args) -> {
            try {
                final VlanVO vlanVO = (VlanVO) args;
                removePublicNetworkSubnet(vlanVO);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribePopEvent() {
        messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_POD_IP_RANGE_EVENT, (senderAddress, subject, args) -> {
            try {
                final HostPodVO pod = (HostPodVO) args;
                addManagementNetworkSubnet(pod);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_POD_IP_RANGE_EVENT, (senderAddress, subject, args) -> {
            try {
                final HostPodVO pod = (HostPodVO) args;
                removeManagementNetworkSubnet(pod);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeDomainEvent() {
        messageBus.subscribe(DomainManager.MESSAGE_CREATE_TUNGSTEN_DOMAIN_EVENT, (senderAddress, subject, args) -> {
            try {
                final DomainVO domain = (DomainVO) args;
                createTungstenDomain(domain);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_DELETE_TUNGSTEN_DOMAIN_EVENT, (senderAddress, subject, args) -> {
            try {
                final DomainVO domain = (DomainVO) args;
                deleteTungstenDomain(domain);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeProjectEvent() {
        messageBus.subscribe(ProjectManager.MESSAGE_CREATE_TUNGSTEN_PROJECT_EVENT, (senderAddress, subject, args) -> {
            try {
                final Project project = (Project) args;
                createTungstenProject(project);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(ProjectManager.MESSAGE_DELETE_TUNGSTEN_PROJECT_EVENT, (senderAddress, subject, args) -> {
            try {
                final Project project = (Project) args;
                deleteTungstenProject(project);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeSecurityGroupEvent() {
        messageBus.subscribe(SecurityGroupService.MESSAGE_CREATE_TUNGSTEN_SECURITY_GROUP_EVENT,
                (senderAddress, subject, args) -> {
                    try {
                        final SecurityGroup securityGroup = (SecurityGroup) args;
                        createTungstenSecurityGroup(securityGroup);
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                });

        messageBus.subscribe(SecurityGroupService.MESSAGE_DELETE_TUNGSTEN_SECURITY_GROUP_EVENT,
                (senderAddress, subject, args) -> {
                    try {
                        final SecurityGroup securityGroup = (SecurityGroup) args;
                        deleteTungstenSecurityGroup(securityGroup);
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                });

        messageBus.subscribe(SecurityGroupService.MESSAGE_ADD_SECURITY_GROUP_RULE_EVENT, (senderAddress, subject,
         args) -> {
            try {
                final List<SecurityRule> securityRules = (List<SecurityRule>) args;
                addTungstenSecurityGroupRule(securityRules);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(SecurityGroupService.MESSAGE_REMOVE_SECURITY_GROUP_RULE_EVENT, (senderAddress, subject,
         args) -> {
            try {
                final SecurityRule securityRule = (SecurityRule) args;
                removeTungstenSecurityGroupRule(securityRule);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    private void subscribeSecondaryNicEvent() {
        messageBus.subscribe(NetworkService.MESSAGE_ASSIGN_NIC_SECONDARY_IP_EVENT, (senderAddress, subject, args) -> {
            try {
                final long id = (long) args;
                addTungstenNicSecondaryIpAddress(id);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });

        messageBus.subscribe(NetworkService.MESSAGE_RELEASE_NIC_SECONDARY_IP_EVENT, (senderAddress, subject, args) -> {
            try {
                final NicSecondaryIpVO nicSecondaryIpVO = (NicSecondaryIpVO) args;
                removeTungstenNicSecondaryIpAddress(nicSecondaryIpVO);
            } catch (final Exception e) {
                s_logger.error(e.getMessage());
            }
        });
    }

    @Override
    public List<TungstenProviderVO> getTungstenProviders() {
        List<TungstenProviderVO> tungstenProviders = tungstenProviderDao.findAll();
        return Objects.requireNonNullElseGet(tungstenProviders, ArrayList::new);
    }

    private boolean createTungstenFloatingIp(long zoneId, IpAddress ipAddress) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        Network network = networkDao.findById(ipAddress.getNetworkId());
        String projectFqn = getTungstenProjectFqn(network);
        TungstenCommand createTungstenFloatingIpCommand = new CreateTungstenFloatingIpCommand(projectFqn,
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId),
            TungstenUtils.getFloatingIpName(ipAddress.getId()), ipAddress.getAddress().addr());
        TungstenAnswer createTungstenFloatingIpAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenFloatingIpCommand, zoneId);
        return createTungstenFloatingIpAnswer.getResult();
    }

    private boolean deleteTungstenFloatingIp(long zoneId, IpAddress ipAddress) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        TungstenCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(publicNetwork.getUuid(),
            TungstenUtils.getFloatingIpPoolName(zoneId), TungstenUtils.getFloatingIpName(ipAddress.getId()));
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpCommand,
            zoneId);
        return tungstenAnswer.getResult();
    }

    private String getProject(long accountId) {
        Account account = accountDao.findById(accountId);
        if (account.getType() == Account.Type.PROJECT) {
            ProjectVO projectVO = projectDao.findByProjectAccountId(account.getId());
            if (projectVO != null) {
                return projectVO.getUuid();
            }
        }
        return null;
    }

    @Override
    public String getTungstenProjectFqn(Network network) {

        if (network == null) {
            return TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;
        }

        String networkProjectUuid = getProject(network.getAccountId());
        return buildProjectFqnName(network.getDomainId(), networkProjectUuid);
    }

    public String buildProjectFqnName(long domainUuid, String projectUuid) {
        Project project = projectDao.findByUuid(projectUuid);
        Domain domain = domainDao.findById(domainUuid);

        StringBuilder sb = new StringBuilder();
        if (domain != null && domain.getName() != null && domain.getId() != Domain.ROOT_DOMAIN) {
            sb.append(domain.getName());
        } else {
            sb.append(TungstenApi.TUNGSTEN_DEFAULT_DOMAIN);
        }

        sb.append(":");

        if (project != null && project.getName() != null) {
            sb.append(project.getName());
        } else {
            sb.append(TungstenApi.TUNGSTEN_DEFAULT_PROJECT);
        }
        return sb.toString();
    }

    @Override
    public boolean addTungstenDefaultNetworkPolicy(long zoneId, String projectFqn, String policyName,
        String networkUuid, List<TungstenRule> ruleList, int majorSequence, int minorSequence) {

        // create network policy
        TungstenCommand createTungstenPolicyCommand = new CreateTungstenNetworkPolicyCommand(policyName, projectFqn,
            ruleList);
        TungstenAnswer createTungstenPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenPolicyCommand,
            zoneId);
        if (!createTungstenPolicyAnswer.getResult()) {
            return false;
        }

        // apply network policy
        TungstenCommand applyTungstenNetworkPolicyCommand = new ApplyTungstenNetworkPolicyCommand(networkUuid,
            createTungstenPolicyAnswer.getApiObjectBase().getUuid(), majorSequence, minorSequence);
        TungstenAnswer applyTungstenPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            applyTungstenNetworkPolicyCommand, zoneId);
        return applyTungstenPolicyAnswer.getResult();
    }

    @Override
    public boolean createManagementNetwork(long zoneId) {
        Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
            Networks.TrafficType.Management);

        TungstenCommand createTungstenNetworkCommand = new CreateTungstenNetworkCommand(managementNetwork.getUuid(),
            TungstenUtils.getManagementNetworkName(zoneId), TungstenUtils.getManagementNetworkName(zoneId), null, false,
            false, null, 0, null, true, null, null, null, true, true, null);
        TungstenAnswer createTungstenNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenNetworkCommand, zoneId);

        if (!createTungstenNetworkAnswer.getResult()) {
            return false;
        }

        // change default tungsten security group
        TungstenCommand updateTungstenDefaultSecurityGroupCommand = new UpdateTungstenDefaultSecurityGroupCommand(null);
        TungstenAnswer updateTungstenDefaultSecurityGroupAnswer = tungstenFabricUtils.sendTungstenCommand(updateTungstenDefaultSecurityGroupCommand, zoneId);

        if (!updateTungstenDefaultSecurityGroupAnswer.getResult()) {
            return false;
        }

        // change default forwarding mode
        TungstenCommand updateTungstenGlobalVrouterConfigCommand = new UpdateTungstenVrouterConfigCommand(
            TungstenUtils.DEFAULT_FORWARDING_MODE);
        TungstenAnswer updateTungstenGlobalVrouterConfigAnswer = tungstenFabricUtils.sendTungstenCommand(updateTungstenGlobalVrouterConfigCommand, zoneId);

        if (!updateTungstenGlobalVrouterConfigAnswer.getResult()) {
            return false;
        }

        VirtualNetwork managementVirtualNetwork = (VirtualNetwork) createTungstenNetworkAnswer.getApiObjectBase();
        List<TungstenRule> tungstenManagementRuleList = new ArrayList<>();
        tungstenManagementRuleList.add(
            new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY,
                StringUtils.join(managementVirtualNetwork.getQualifiedName(), ":"), null, 0, -1, -1,
                TungstenUtils.FABRIC_NETWORK_FQN, null, 0, -1, -1));

        boolean addDefaultManagementPolicy = addTungstenDefaultNetworkPolicy(zoneId, null,
            TungstenUtils.getManagementPolicyName(zoneId), managementVirtualNetwork.getUuid(),
            tungstenManagementRuleList, 1, 0);
        if (!addDefaultManagementPolicy) {
            return false;
        }

        TungstenCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
        TungstenAnswer getTungstenFabricNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            getTungstenFabricNetworkCommand, zoneId);
        if (!getTungstenFabricNetworkAnswer.getResult()) {
            return false;
        }

        List<TungstenRule> tungstenFabricRuleList = new ArrayList<>();
        tungstenFabricRuleList.add(
            new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY,
                TungstenUtils.FABRIC_NETWORK_FQN, null, 0, -1, -1,
                StringUtils.join(managementVirtualNetwork.getQualifiedName(), ":"), null, 0, -1, -1));

        String fabricNetworkUuid = getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid();
        return addTungstenDefaultNetworkPolicy(zoneId, null, TungstenUtils.getFabricPolicyName(zoneId),
            fabricNetworkUuid, tungstenFabricRuleList, 1, 0);
    }

    @Override
    public boolean addManagementNetworkSubnet(HostPodVO pod) {
        final String[] podIpRanges = pod.getDescription().split(",");
        Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
            Networks.TrafficType.Management);

        // tungsten system don't support delete a part of allocation pool in subnet
        // we only create first pod ip range in a pod (the same with public network)
        // in UI : don't permit to add more than 1 pod ip range if tungsten zone
        // consider to permit add more pod ip range if it is not overlap subnet
        if (podIpRanges.length == 1) {
            final String[] ipRange = podIpRanges[0].split("-");
            String startIp = ipRange[0];
            String endIp = ipRange[1];

            TungstenCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(
                managementNetwork.getUuid(), pod.getCidrAddress(), pod.getCidrSize(), null, true, "0.0.0.0", startIp,
                endIp, true, pod.getUuid());
            TungstenAnswer addTungstenNetworkSubnetAnswer = tungstenFabricUtils.sendTungstenCommand(
                addTungstenNetworkSubnetCommand, pod.getDataCenterId());

            if (!addTungstenNetworkSubnetAnswer.getResult()) {
                return false;
            }

            return allocateDnsIpAddress(managementNetwork, pod, pod.getUuid());
        }

        return true;
    }

    @Override
    public boolean deleteManagementNetwork(long zoneId) {
        Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
            Networks.TrafficType.Management);

        TungstenCommand deleteTungstenManagementPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getManagementPolicyName(zoneId), null, managementNetwork.getUuid());
        TungstenAnswer deleteTungstenManagementPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenManagementPolicyCommand, zoneId);
        if (!deleteTungstenManagementPolicyAnswer.getResult()) {
            return false;
        }

        TungstenCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
        TungstenAnswer getTungstenFabricNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            getTungstenFabricNetworkCommand, zoneId);
        if (!getTungstenFabricNetworkAnswer.getResult()) {
            return false;
        }

        TungstenCommand deleteTungstenFabricPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getFabricPolicyName(zoneId), null,
            getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid());
        TungstenAnswer deleteTungstenFabricPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenFabricPolicyCommand, zoneId);
        if (!deleteTungstenFabricPolicyAnswer.getResult()) {
            return false;
        }

        TungstenCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(managementNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, zoneId);
        return deleteTungstenNetworkAnswer.getResult();
    }

    @Override
    public boolean removeManagementNetworkSubnet(HostPodVO pod) {
        Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
            Networks.TrafficType.Management);

        deallocateDnsIpAddress(managementNetwork, pod, pod.getUuid());

        TungstenCommand removeTungstenNetworkSubnetCommand = new RemoveTungstenNetworkSubnetCommand(
            managementNetwork.getUuid(), pod.getUuid());

        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(removeTungstenNetworkSubnetCommand,
            pod.getDataCenterId());
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean createPublicNetwork(long zoneId) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public network
        TungstenCommand createTungstenPublicNetworkCommand = new CreateTungstenNetworkCommand(publicNetwork.getUuid(),
            TungstenUtils.getPublicNetworkName(zoneId), TungstenUtils.getPublicNetworkName(zoneId), null, true, false,
            null, 0, null, true, null, null, null, false, false, null);
        TungstenAnswer createPublicNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenPublicNetworkCommand, zoneId);
        if (!createPublicNetworkAnswer.getResult()) {
            return false;
        }

        // add default public network policy
        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(
            new TungstenRule(TungstenUtils.DENY_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY,
                TungstenUtils.ANY, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ANY,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1));

        addTungstenDefaultNetworkPolicy(zoneId, null, TungstenUtils.getDefaultPublicNetworkPolicyName(zoneId),
            publicNetwork.getUuid(), tungstenRuleList, 100, 0);

        // create floating ip pool
        TungstenCommand createTungstenFloatingIpPoolCommand = new CreateTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer createFloatingIpPoolAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenFloatingIpPoolCommand, zoneId);
        if (!createFloatingIpPoolAnswer.getResult()) {
            return false;
        }

        NetworkDetailVO networkDetailVO = new NetworkDetailVO(publicNetwork.getId(), "vrf",
            TungstenUtils.getVrfNetworkName(createPublicNetworkAnswer.getApiObjectBase().getQualifiedName()), false);
        NetworkDetailVO persisted = networkDetailsDao.persist(networkDetailVO);

        return persisted != null;
    }

    @Override
    public boolean addPublicNetworkSubnet(VlanVO pubVlanVO) {
        long zoneId = pubVlanVO.getDataCenterId();
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public ip address
        String[] ipAddress = pubVlanVO.getIpRange().split("-");
        String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
            pubVlanVO.getVlanNetmask());
        Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);

        TungstenCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(publicNetwork.getUuid(),
            publicPair.first(), publicPair.second(), pubVlanVO.getVlanGateway(), true, "0.0.0.0", ipAddress[0],
            ipAddress[1], false, pubVlanVO.getUuid());
        TungstenAnswer addTungstenNetworkSubnetAnswer = tungstenFabricUtils.sendTungstenCommand(
            addTungstenNetworkSubnetCommand, zoneId);
        if (!addTungstenNetworkSubnetAnswer.getResult()) {
            return false;
        }

        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(
            new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY,
                TungstenUtils.ANY, publicPair.first(), publicPair.second(), -1, -1, TungstenUtils.ANY,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1));

        addTungstenDefaultNetworkPolicy(zoneId, null, TungstenUtils.getDefaultPublicSubnetPolicyName(pubVlanVO.getId()),
            publicNetwork.getUuid(), tungstenRuleList, 99, 0);

        return allocateDnsIpAddress(publicNetwork, null, pubVlanVO.getUuid());
    }

    @Override
    public boolean deletePublicNetwork(long zoneId) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        TungstenCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getDefaultPublicNetworkPolicyName(zoneId), null, publicNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenNetworkPolicyCommand, zoneId);
        if (!deleteTungstenNetworkPolicyAnswer.getResult()) {
            return false;
        }

        // delete floating ip pool
        TungstenCommand deleteTungstenFloatingIpPoolCommand = new DeleteTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer deleteTungstenFloatingIpPoolAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenFloatingIpPoolCommand, zoneId);
        if (!deleteTungstenFloatingIpPoolAnswer.getResult()) {
            return false;
        }

        // delete public network
        TungstenCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(publicNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenNetworkCommand, zoneId);
        return deleteTungstenNetworkAnswer.getResult();
    }

    @Override
    public boolean removePublicNetworkSubnet(VlanVO vlanVO) {
        long zoneId = vlanVO.getDataCenterId();
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        deallocateDnsIpAddress(publicNetwork, null, vlanVO.getUuid());

        TungstenCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getDefaultPublicSubnetPolicyName(vlanVO.getId()), null, publicNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenNetworkPolicyCommand, vlanVO.getDataCenterId());
        if (!deleteTungstenNetworkPolicyAnswer.getResult()) {
            return false;
        }

        TungstenCommand removeTungstenNetworkSubnetCommand = new RemoveTungstenNetworkSubnetCommand(
            publicNetwork.getUuid(), vlanVO.getUuid());
        TungstenAnswer removeTungstenNetworkSubnetAnswer = tungstenFabricUtils.sendTungstenCommand(
            removeTungstenNetworkSubnetCommand, zoneId);
        return removeTungstenNetworkSubnetAnswer.getResult();
    }

    @Override
    public boolean allocateDnsIpAddress(Network network, Pod pod, String subnetName) {
        long zoneId = network.getDataCenterId();
        TungstenCommand getTungstenNetworkDnsCommand = new GetTungstenNetworkDnsCommand(network.getUuid(), subnetName);
        TungstenAnswer getTungstenNetworkDnsAnswer = tungstenFabricUtils.sendTungstenCommand(
                getTungstenNetworkDnsCommand, zoneId);
        if (getTungstenNetworkDnsAnswer.getResult()) {
            return markIpAddress(pod, network, zoneId, getTungstenNetworkDnsAnswer.getDetails());
        }
        return true;
    }

    private boolean markIpAddress(Pod pod, Network network, long zoneId, String ip) {
        String cidr = network.getCidr();
        if (NetUtils.isIpWithInCidrRange(ip, cidr)) {
            if ((network.getTrafficType() == Networks.TrafficType.Public || (
                    network.getTrafficType() == Networks.TrafficType.Guest
                            && network.getGuestType() == Network.GuestType.Shared) &&
                    ipAddressDao.findByIpAndDcId(zoneId, ip) != null)) {
                return ipAddressDao.mark(zoneId, new Ip(ip));
            }

            if (network.getTrafficType() == Networks.TrafficType.Management && pod != null) {
                long podId = pod.getId();
                if (!dataCenterIpAddressDao.listByPodIdDcIdIpAddress(podId, zoneId, ip).isEmpty()) {
                    return dataCenterIpAddressDao.mark(zoneId, podId, ip);
                }
            }

            if (network.getTrafficType() == Networks.TrafficType.Guest
                    && network.getGuestType() == Network.GuestType.Isolated) {
                TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO =
                        new TungstenGuestNetworkIpAddressVO(network.getId(), new Ip(ip));
                return tungstenGuestNetworkIpAddressDao.persist(tungstenGuestNetworkIpAddressVO) != null;
            }
        }

        return true;
    }

    @Override
    public void deallocateDnsIpAddress(Network network, Pod pod, String subnetName) {
        long zoneId = network.getDataCenterId();
        TungstenCommand getTungstenNetworkDnsCommand = new GetTungstenNetworkDnsCommand(network.getUuid(), subnetName);
        TungstenAnswer getTungstenNetworkDnsAnswer = tungstenFabricUtils.sendTungstenCommand(
            getTungstenNetworkDnsCommand, zoneId);
        if (getTungstenNetworkDnsAnswer.getResult()) {
            releaseIpAddress(pod, network, zoneId, getTungstenNetworkDnsAnswer.getDetails());
        }
    }

    private void releaseIpAddress(Pod pod, Network network, long zoneId, String ip) {
        if (network.getTrafficType() == Networks.TrafficType.Public || (
                network.getTrafficType() == Networks.TrafficType.Guest
                        && network.getGuestType() == Network.GuestType.Shared)) {
            IpAddress ipAddress = ipAddressDao.findByIpAndDcId(zoneId, ip);
            if (ipAddress != null) {
                ipAddressDao.unassignIpAddress(ipAddress.getId());
            }
        }

        if (network.getTrafficType() == Networks.TrafficType.Management && pod != null) {
            long podId = pod.getId();
            List<DataCenterIpAddressVO> dataCenterIpAddressVOList = dataCenterIpAddressDao.listByPodIdDcIdIpAddress(
                    podId, zoneId, ip);
            for (DataCenterIpAddressVO dataCenterIpAddressVO : dataCenterIpAddressVOList) {
                dataCenterIpAddressDao.releasePodIpAddress(dataCenterIpAddressVO.getId());
            }
        }

        if (network.getTrafficType() == Networks.TrafficType.Guest
                && network.getGuestType() == Network.GuestType.Isolated) {
            TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO =
                    tungstenGuestNetworkIpAddressDao.findByNetworkAndGuestIpAddress(network.getId(), ip);
            if (tungstenGuestNetworkIpAddressVO != null) {
                tungstenGuestNetworkIpAddressDao.expunge(tungstenGuestNetworkIpAddressVO.getId());
            }
        }
    }

    private boolean createTungstenDomain(DomainVO domain) {
        if (domain != null && domain.getId() != Domain.ROOT_DOMAIN) {
            List<TungstenProviderVO> tungstenProviders = tungstenProviderDao.findAll();
            for (TungstenProviderVO tungstenProvider : tungstenProviders) {
                TungstenCommand createTungstenDomainCommand = new CreateTungstenDomainCommand(domain.getName(),
                    domain.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenDomainCommand,
                    tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean deleteTungstenDomain(DomainVO domain) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            TungstenCommand deleteTungstenDomainCommand = new DeleteTungstenDomainCommand(domain.getUuid());
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenDomainCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    private boolean createTungstenProject(Project project) {
        String domainName;
        String domainUuid;
        Domain domain = domainDao.findById(project.getDomainId());
        //Check if the domain is the root domain
        //if the domain is root domain we will use the default domain from tungsten by setting
        //both domainName and domainUuid to null
        if (domain != null && domain.getId() != Domain.ROOT_DOMAIN) {
            domainName = domain.getName();
            domainUuid = domain.getUuid();
        } else {
            domainName = null;
            domainUuid = null;
        }
        if (domain != null) {
            for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
                TungstenCommand createTungstenProjectCommand = new CreateTungstenProjectCommand(project.getName(),
                    project.getUuid(), domainUuid, domainName);
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenProjectCommand,
                    tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }

                TungstenCommand updateTungstenDefaultSecurityGroupCommand =
                    new UpdateTungstenDefaultSecurityGroupCommand(
                    buildProjectFqnName(domain.getId(), project.getUuid()));
                TungstenAnswer updateTungstenDefaultSecurityGroupAnswer = tungstenFabricUtils.sendTungstenCommand(
                    updateTungstenDefaultSecurityGroupCommand, tungstenProvider.getZoneId());
                if (!updateTungstenDefaultSecurityGroupAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean deleteTungstenProject(Project project) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            TungstenCommand deleteTungstenProjectCommand = new DeleteTungstenProjectCommand(project.getUuid());
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenProjectCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean syncTungstenDbWithCloudstackProjectsAndDomains() {
        List<DomainVO> cloudstackDomains = domainDao.listAll();
        List<ProjectVO> cloudstackProjects = projectDao.listAll();

        if (cloudstackDomains != null) {
            for (DomainVO domain : cloudstackDomains) {
                if (!createTungstenDomain(domain)) {
                    return false;
                }
            }
        }

        if (cloudstackProjects != null) {
            for (ProjectVO project : cloudstackProjects) {
                if (!createTungstenProject(project)) {
                    return false;
                }
            }
        }

        return createTungstenDefaultProject();
    }

    private boolean createTungstenDefaultProject() {

        List<TungstenProviderVO> tungstenProviders = tungstenProviderDao.findAll();
        for (TungstenProviderVO tungstenProvider : tungstenProviders) {
            TungstenCommand tungstenCommand = new CreateTungstenDefaultProjectCommand();
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }

        return true;
    }

    @Override
    // remove this when tungsten support cloudstack ssl and stats
    public boolean updateLoadBalancer(Network network, LoadBalancingRule loadBalancingRule) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
            loadBalancingRule.getSourceIp().addr());
        List<HostVO> hostList = hostDao.listAllHostsByZoneAndHypervisorType(network.getDataCenterId(),
            Hypervisor.HypervisorType.KVM);

        TungstenCommand getTungstenLoadBalancerCommand = new GetTungstenLoadBalancerCommand(
                getTungstenProjectFqn(network), TungstenUtils.getLoadBalancerName(ipAddressVO.getId()));
        TungstenAnswer getTungstenLoadBalancerAnswer = tungstenFabricUtils.sendTungstenCommand(
                getTungstenLoadBalancerCommand, network.getDataCenterId());
        if (!getTungstenLoadBalancerAnswer.getResult()) {
            return false;
        }

        // wait for service instance update
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // remove fat flow protocol
        TungstenCommand updateLoadBalancerServiceInstanceCommand = new UpdateLoadBalancerServiceInstanceCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
            TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
        TungstenAnswer updateLoadBalancerServiceInstanceAnswer = tungstenFabricUtils.sendTungstenCommand(
            updateLoadBalancerServiceInstanceCommand, network.getDataCenterId());
        if (!updateLoadBalancerServiceInstanceAnswer.getResult()) {
            return false;
        }

        // wait for service instance update
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String lbUuid = getTungstenLoadBalancerAnswer.getApiObjectBase().getUuid();

        return updateHaproxyStats(hostList, lbUuid) && updateHaproxySsl(hostList, network, ipAddressVO, lbUuid);
    }

    private boolean updateHaproxyStats(List<HostVO> hostList, String lbUuid) {
        // update haproxy stats
        String lbStatsVisibility = configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        if (!lbStatsVisibility.equals("disabled")) {
            String lbStatsUri = configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
            String lbStatsAuth = configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
            String lbStatsPort = configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
            UpdateTungstenLoadbalancerStatsCommand updateTungstenLoadbalancerStatsCommand =
                    new UpdateTungstenLoadbalancerStatsCommand(lbUuid, lbStatsPort, lbStatsUri, lbStatsAuth);
            for (HostVO host : hostList) {
                Answer answer = agentMgr.easySend(host.getId(), updateTungstenLoadbalancerStatsCommand);
                if (answer == null || !answer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean updateHaproxySsl(List<HostVO> hostList, Network network, IPAddressVO ipAddressVO, String lbUuid) {
        // update haproxy ssl
        List<FirewallRuleVO> firewallRulesDaoVOList = fwRulesDao.listByIpAndPurposeAndNotRevoked(ipAddressVO.getId(),
                FirewallRule.Purpose.LoadBalancing);
        for (FirewallRuleVO firewallRuleVO : firewallRulesDaoVOList) {
            LoadBalancerCertMapVO loadBalancerCertMapVO = lbCertMapDao.findByLbRuleId(firewallRuleVO.getId());
            if (loadBalancerCertMapVO != null) {
                SslCertVO certVO = entityMgr.findById(SslCertVO.class, loadBalancerCertMapVO.getCertId());
                if (certVO == null) {
                    return false;
                }

                TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO =
                        tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                                network.getId(), ipAddressVO.getAddress().addr());

                UpdateTungstenLoadbalancerSslCommand updateTungstenLoadbalancerSslCommand =
                        new UpdateTungstenLoadbalancerSslCommand(lbUuid, certVO.getName(),
                                certVO.getCertificate(), certVO.getKey(),
                                tungstenGuestNetworkIpAddressVO.getGuestIpAddress().addr(), String.valueOf(NetUtils.HTTPS_PORT));
                for (HostVO host : hostList) {
                    Answer answer = agentMgr.easySend(host.getId(), updateTungstenLoadbalancerSslCommand);
                    if (answer == null || !answer.getResult()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    // add this when tungsten support cloudstack ssl
    public boolean updateLoadBalancerSsl(Network network, LoadBalancingRule loadBalancingRule) {
        LoadBalancingRule.LbSslCert lbSslCert = loadBalancingRule.getLbSslCert();
        if (lbSslCert != null) {
            String httpsProtocol = "TERMINATED_HTTPS";
            int listenerPort = NetUtils.HTTPS_PORT;

            User callerUser = accountMgr.getActiveUser(CallContext.current().getCallingUserId());
            String apiKey = callerUser.getApiKey();
            String secretKey = callerUser.getSecretKey();
            if (apiKey != null && secretKey != null) {
                String url;
                try {
                    String data = "apiKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name()).replace("\\+", "%20") + "&command"
                        + "=getLoadBalancerSslCertificate" + "&id=" + URLEncoder.encode(
                        loadBalancingRule.getUuid(), StandardCharsets.UTF_8.name()).replace("\\+", "%20") + "&response=json";
                    String signature = EncryptionUtil.generateSignature(data.toLowerCase(), secretKey);
                    url = data + "&signature=" + URLEncoder.encode(signature, StandardCharsets.UTF_8.name()).replace("\\+", "%2B");
                } catch (UnsupportedEncodingException e) {
                    return false;
                }

                TungstenCommand updateTungstenLoadBalancerListenerCommand = new UpdateTungstenLoadBalancerListenerCommand(
                    getTungstenProjectFqn(network), TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()),
                    httpsProtocol, listenerPort, url);
                TungstenAnswer updateTungstenLoadBalancerListenerAnswer = tungstenFabricUtils.sendTungstenCommand(
                    updateTungstenLoadBalancerListenerCommand, network.getDataCenterId());
                return updateTungstenLoadBalancerListenerAnswer.getResult();
            } else {
                s_logger.error("Tungsten-Fabric ssl require user api key");
            }
        }
        return true;
    }

    public boolean createTungstenSecurityGroup(SecurityGroup securityGroup) {
        Project project = projectDao.findByProjectAccountId(securityGroup.getAccountId());
        String projectFqn;
        if (project != null) {
            projectFqn = buildProjectFqnName(securityGroup.getDomainId(), project.getUuid());
        } else {
            projectFqn = buildProjectFqnName(securityGroup.getDomainId(), null);
        }

        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            List<TungstenSecurityGroupRuleVO> tungstenSecurityGroupRuleVOList = createDefaultTungstenSecurityGroupRule(
                    tungstenProvider, securityGroup, projectFqn);

            TungstenCommand createTungstenSecurityGroupCommand = new CreateTungstenSecurityGroupCommand(
                securityGroup.getUuid(),
                TungstenUtils.getSecurityGroupName(securityGroup.getName(), securityGroup.getAccountId()),
                securityGroup.getDescription(), projectFqn);
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenSecurityGroupCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }

            for (TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO : tungstenSecurityGroupRuleVOList) {
                TungstenCommand addTungstenSecurityGroupRuleCommand = new AddTungstenSecurityGroupRuleCommand(
                    securityGroup.getUuid(), tungstenSecurityGroupRuleVO.getUuid(),
                    tungstenSecurityGroupRuleVO.getRuleType(), NetUtils.PORT_RANGE_MIN, NetUtils.PORT_RANGE_MAX,
                    tungstenSecurityGroupRuleVO.getRuleTarget(), tungstenSecurityGroupRuleVO.getEtherType(),
                    NetUtils.ANY_PROTO);
                TungstenAnswer addTungstenSecurityGroupRuleAnswer = tungstenFabricUtils.sendTungstenCommand(
                    addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                if (!addTungstenSecurityGroupRuleAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<TungstenSecurityGroupRuleVO> createDefaultTungstenSecurityGroupRule(TungstenProvider tungstenProvider
            , SecurityGroup securityGroup, String projectFqn) {
        // create default Tungsten-Fabric security group rule
        return Transaction.execute(
                (TransactionCallback<List<TungstenSecurityGroupRuleVO>>) status -> {
                    List<TungstenSecurityGroupRuleVO> ruleVOList = new ArrayList<>();
                    TungstenSecurityGroupRuleVO defaultIpv4EgressRule =
                            tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                                    securityGroup.getId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV4);
                    if (defaultIpv4EgressRule == null) {
                        defaultIpv4EgressRule = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                                securityGroup.getId(), TungstenUtils.EGRESS_RULE, NetUtils.ALL_IP4_CIDRS,
                                TungstenUtils.IPV4, true);
                        tungstenSecurityGroupRuleDao.persist(defaultIpv4EgressRule);
                        ruleVOList.add(defaultIpv4EgressRule);
                    }

                    TungstenSecurityGroupRuleVO defaultIpv6EgressRule =
                            tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                                    securityGroup.getId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV6);
                    if (defaultIpv6EgressRule == null) {
                        defaultIpv6EgressRule = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                                securityGroup.getId(), TungstenUtils.EGRESS_RULE, NetUtils.ALL_IP6_CIDRS,
                                TungstenUtils.IPV6, true);
                        tungstenSecurityGroupRuleDao.persist(defaultIpv6EgressRule);
                        ruleVOList.add(defaultIpv6EgressRule);
                    }

                    TungstenSecurityGroupRuleVO defaultIpv4IngressRule =
                            tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                                    securityGroup.getId(), TungstenUtils.INGRESS_RULE, TungstenUtils.IPV4);
                    if (defaultIpv4IngressRule == null) {
                        defaultIpv4IngressRule = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                                securityGroup.getId(), TungstenUtils.INGRESS_RULE,
                                projectFqn + ":" + TungstenUtils.LOCAL, TungstenUtils.IPV4, true);
                        tungstenSecurityGroupRuleDao.persist(defaultIpv4IngressRule);
                        ruleVOList.add(defaultIpv4IngressRule);
                    }

                    TungstenSecurityGroupRuleVO defaultIpv6IngressRule =
                            tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                                    securityGroup.getId(), TungstenUtils.INGRESS_RULE, TungstenUtils.IPV6);
                    if (defaultIpv6IngressRule == null) {
                        defaultIpv6IngressRule = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                                securityGroup.getId(), TungstenUtils.INGRESS_RULE,
                                projectFqn + ":" + TungstenUtils.LOCAL, TungstenUtils.IPV6, true);
                        tungstenSecurityGroupRuleDao.persist(defaultIpv6IngressRule);
                        ruleVOList.add(defaultIpv6IngressRule);
                    }

                    return ruleVOList;
                });
    }

    public boolean deleteTungstenSecurityGroup(SecurityGroup securityGroup) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            TungstenCommand deleteTungstenSecurityGroupCommand = new DeleteTungstenSecurityGroupCommand(
                securityGroup.getUuid());
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenSecurityGroupCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean addTungstenSecurityGroupRule(List<SecurityRule> securityRules) {
        List<SecurityGroupVO> securityGroupVOList = new ArrayList<>();
        for (SecurityRule securityRule : securityRules) {
            SecurityGroupVO securityGroupVO = securityGroupDao.findById(securityRule.getSecurityGroupId());
            securityGroupVOList.add(securityGroupVO);
            if (securityRule.getAllowedNetworkId() != null) {
                SecurityGroupVO allowedSecurityGroupVO = securityGroupDao.findById(securityRule.getAllowedNetworkId());
                securityGroupVOList.add(allowedSecurityGroupVO);
            }
        }

        checkTungstenSecurityGroups(securityGroupVOList);
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            for (SecurityRule securityRule : securityRules) {
                checkSecurityRule(securityRule);

                SecurityGroup securityGroup = securityGroupDao.findById(securityRule.getSecurityGroupId());
                removeEgressRule(securityRule, tungstenProvider, securityGroup);

                int tungstenEndPort = securityRule.getEndPort();
                if (securityRule.getProtocol().equals(NetUtils.ALL_PROTO) && securityRule.getEndPort() == NetUtils.PORT_RANGE_MIN) {
                    tungstenEndPort = NetUtils.PORT_RANGE_MAX;
                }

                if (securityRule.getAllowedNetworkId() != null) {
                    return addTungstenRuleWithNetwork(securityRule, tungstenProvider, securityGroup, tungstenEndPort);
                }

                addTungstenRuleWithVM(securityRule, tungstenProvider, securityGroup, tungstenEndPort);
            }
        }

        return true;
    }

    private void checkSecurityRule(SecurityRule securityRule) {
        if (StringUtils.isNumeric(securityRule.getProtocol())) {
            throw new CloudRuntimeException("Tungsten-Fabric don't support number protocol");
        }
    }

    private void removeEgressRule(SecurityRule securityRule, TungstenProvider tungstenProvider, SecurityGroup securityGroup) {
        if (securityRule.getRuleType() == SecurityRule.SecurityRuleType.EgressRule) {
            TungstenSecurityGroupRule egressIpv4Rule = tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                    securityRule.getSecurityGroupId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV4);

            if (egressIpv4Rule != null) {
                TungstenCommand tungstenCommand = new RemoveTungstenSecurityGroupRuleCommand(
                        securityGroup.getUuid(), egressIpv4Rule.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand,
                        tungstenProvider.getZoneId());
                if (tungstenAnswer.getResult()) {
                    tungstenSecurityGroupRuleDao.expunge(egressIpv4Rule.getId());
                }
            }

            TungstenSecurityGroupRule egressIpv6Rule = tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                    securityRule.getSecurityGroupId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV6);

            if (egressIpv6Rule != null) {
                TungstenCommand tungstenCommand = new RemoveTungstenSecurityGroupRuleCommand(
                        securityGroup.getUuid(), egressIpv6Rule.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand,
                        tungstenProvider.getZoneId());
                if (tungstenAnswer.getResult()) {
                    tungstenSecurityGroupRuleDao.expunge(egressIpv6Rule.getId());
                }
            }
        }
    }

    private boolean addTungstenRuleWithNetwork(SecurityRule securityRule, TungstenProvider tungstenProvider, SecurityGroup securityGroup, int tungstenEndPort) {
        List<Long> vmIdList = securityGroupVMMapDao.listVmIdsBySecurityGroup(securityRule.getAllowedNetworkId());
        for (long vmid : vmIdList) {
            Nic nic = nicDao.findDefaultNicForVM(vmid);
            List<String> ipAddressList = getListIpAddressFromNic(nic);
            for (String ipAddress : ipAddressList) {
                String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
                TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = new TungstenSecurityGroupRuleVO(
                        tungstenProvider.getZoneId(), securityRule.getSecurityGroupId(), securityRule.getType(),
                        cidr, TungstenUtils.getEthertTypeFromCidr(cidr), false);
                TungstenSecurityGroupRuleVO persisted = tungstenSecurityGroupRuleDao.persist(
                        tungstenSecurityGroupRuleVO);
                if (persisted != null) {
                    TungstenCommand addTungstenSecurityGroupRuleCommand =
                            new AddTungstenSecurityGroupRuleCommand(
                                    securityGroup.getUuid(), tungstenSecurityGroupRuleVO.getUuid(),
                                    securityRule.getType(), securityRule.getStartPort(), tungstenEndPort, cidr,
                                    TungstenUtils.getEthertTypeFromCidr(cidr), securityRule.getProtocol());
                    TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                            addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                    if (!tungstenAnswer.getResult()) {
                        throw new CloudRuntimeException("Can not add Tungsten Fabric Security Group Rule to network");
                    }
                } else {
                    throw new CloudRuntimeException("Can not add Security Group Rule to network");
                }
            }
        }
        return true;
    }

    private void addTungstenRuleWithVM(SecurityRule securityRule, TungstenProvider tungstenProvider, SecurityGroup securityGroup, int tungstenEndPort) {
        TungstenCommand addTungstenSecurityGroupRuleCommand = new AddTungstenSecurityGroupRuleCommand(
                securityGroup.getUuid(), securityRule.getUuid(), securityRule.getType(),
                securityRule.getStartPort(), tungstenEndPort, securityRule.getAllowedSourceIpCidr(),
                TungstenUtils.getEthertTypeFromCidr(securityRule.getAllowedSourceIpCidr()),
                securityRule.getProtocol());
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
        if (!tungstenAnswer.getResult()) {
            throw new CloudRuntimeException("Can not add Tungsten Fabric Security Rule to VM");
        }
    }

    public boolean removeTungstenSecurityGroupRule(SecurityRule securityRule) {
        SecurityGroup securityGroup = securityGroupDao.findById(securityRule.getSecurityGroupId());
        if (securityGroup == null)
            return false;
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            if (securityRule.getRuleType() == SecurityRule.SecurityRuleType.EgressRule) {
                List<SecurityGroupRuleVO> securityGroupRuleVOList = securityGroupRuleDao.listBySecurityGroupId(
                        securityRule.getSecurityGroupId(), SecurityRule.SecurityRuleType.EgressRule);
                if (securityGroupRuleVOList.isEmpty()) {
                    addTungstenEgressRule(securityGroup, tungstenProvider);
                }
            }

            if (securityRule.getAllowedNetworkId() != null) {
                return removeTungstenRuleWithNetwork(securityRule, securityGroup, tungstenProvider);
            } else {
                TungstenCommand removeTungstenSecurityGroupRuleCommand = new RemoveTungstenSecurityGroupRuleCommand(
                    securityGroup.getUuid(), securityRule.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                    removeTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addTungstenEgressRule(SecurityGroup securityGroup, TungstenProvider tungstenProvider) {
        TungstenSecurityGroupRuleVO tungstenIpv4SecurityGroupRuleVO =
                tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                        securityGroup.getId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV4);

        if (tungstenIpv4SecurityGroupRuleVO == null) {
            tungstenIpv4SecurityGroupRuleVO = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                    securityGroup.getId(), TungstenUtils.EGRESS_RULE, NetUtils.ALL_IP4_CIDRS,
                    TungstenUtils.IPV4, true);
            TungstenSecurityGroupRuleVO persisted = tungstenSecurityGroupRuleDao.persist(
                    tungstenIpv4SecurityGroupRuleVO);
            if (persisted != null) {
                TungstenCommand addTungstenSecurityGroupRuleCommand =
                        new AddTungstenSecurityGroupRuleCommand(
                                securityGroup.getUuid(), persisted.getUuid(), TungstenUtils.EGRESS_RULE,
                                NetUtils.PORT_RANGE_MIN, NetUtils.PORT_RANGE_MAX, NetUtils.ALL_IP4_CIDRS,
                                TungstenUtils.IPV4, NetUtils.ANY_PROTO);
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                        addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        TungstenSecurityGroupRuleVO tungstenIpv6SecurityGroupRuleVO =
                tungstenSecurityGroupRuleDao.findDefaultSecurityRule(
                        securityGroup.getId(), TungstenUtils.EGRESS_RULE, TungstenUtils.IPV6);

        if (tungstenIpv6SecurityGroupRuleVO == null) {
            tungstenIpv6SecurityGroupRuleVO = new TungstenSecurityGroupRuleVO(tungstenProvider.getZoneId(),
                    securityGroup.getId(), TungstenUtils.EGRESS_RULE, NetUtils.ALL_IP6_CIDRS,
                    TungstenUtils.IPV6, true);
            TungstenSecurityGroupRuleVO persisted = tungstenSecurityGroupRuleDao.persist(
                    tungstenIpv6SecurityGroupRuleVO);
            if (persisted != null) {
                TungstenCommand addTungstenSecurityGroupRuleCommand =
                        new AddTungstenSecurityGroupRuleCommand(
                                securityGroup.getUuid(), persisted.getUuid(), TungstenUtils.EGRESS_RULE,
                                NetUtils.PORT_RANGE_MIN, NetUtils.PORT_RANGE_MAX, NetUtils.ALL_IP6_CIDRS,
                                TungstenUtils.IPV6, NetUtils.ANY_PROTO);
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                        addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                return tungstenAnswer.getResult();
            } else {
                return false;
            }
        }

        return true;
    }

    private boolean removeTungstenRuleWithNetwork(SecurityRule securityRule, SecurityGroup securityGroup, TungstenProvider tungstenProvider) {
        List<Long> vmIdList = securityGroupVMMapDao.listVmIdsBySecurityGroup(
                securityRule.getAllowedNetworkId());
        for (long vmId : vmIdList) {
            Nic nic = nicDao.findDefaultNicForVM(vmId);
            List<String> ipAddressList = getListIpAddressFromNic(nic);
            for (String ipAddress : ipAddressList) {
                String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
                TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO =
                        tungstenSecurityGroupRuleDao.findBySecurityGroupAndRuleTypeAndRuleTarget(
                                securityGroup.getId(), securityRule.getType(), cidr);
                TungstenCommand removeTungstenSecurityGroupRule = new RemoveTungstenSecurityGroupRuleCommand(
                        securityGroup.getUuid(), tungstenSecurityGroupRuleVO.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                        removeTungstenSecurityGroupRule, tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }

                if (!tungstenSecurityGroupRuleDao.expunge(tungstenSecurityGroupRuleVO.getId())) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean addTungstenNicSecondaryIpAddress(long id) {
        NicSecondaryIp nicSecondaryIp = entityMgr.findById(NicSecondaryIp.class, id);
        Network network = entityMgr.findById(Network.class, nicSecondaryIp.getNetworkId());
        DataCenter dataCenter = entityMgr.findById(DataCenter.class, network.getDataCenterId());
        Nic nic = entityMgr.findById(Nic.class, nicSecondaryIp.getNicId());

        String ipAddress =
            nicSecondaryIp.getIp4Address() != null ? nicSecondaryIp.getIp4Address() : nicSecondaryIp.getIp6Address();
        String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
        String etherType = TungstenUtils.getEthertTypeFromCidr(cidr);

        TungstenCommand addTungstenSecondaryIpAddressCommand = new AddTungstenSecondaryIpAddressCommand(
            network.getUuid(), nic.getUuid(), TungstenUtils.getSecondaryInstanceIpName(nicSecondaryIp.getId()),
            ipAddress);
        TungstenAnswer addTungstenSecondaryIpAddressAnswer = tungstenFabricUtils.sendTungstenCommand(
            addTungstenSecondaryIpAddressCommand, network.getDataCenterId());
        if (!addTungstenSecondaryIpAddressAnswer.getResult()) {
            return false;
        }

        if (dataCenter.isSecurityGroupEnabled() && network.getGuestType() == Network.GuestType.Shared) {
            List<SecurityGroupVO> securityGroupVOList = securityGroupManager.getSecurityGroupsForVm(
                nicSecondaryIp.getVmId());

            for (TungstenProvider tungstenProvider : getTungstenProviders()) {
                for (SecurityGroupVO securityGroupVO : securityGroupVOList) {
                    List<SecurityGroupRuleVO> securityGroupRuleVOList =
                        securityGroupRuleDao.listByAllowedSecurityGroupId(securityGroupVO.getId());
                    addSecondaryNicRule(securityGroupRuleVOList, securityGroupVO, tungstenProvider, dataCenter.getId(), cidr, etherType);
                }
            }
        }

        return true;
    }

    private void addSecondaryNicRule(List<SecurityGroupRuleVO> securityGroupRuleVOList, SecurityGroupVO securityGroupVO
            , TungstenProvider tungstenProvider, long zoneId, String cidr, String etherType) {
        for (SecurityGroupRuleVO securityGroupRuleVO : securityGroupRuleVOList) {
            TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO =
                    tungstenSecurityGroupRuleDao.findBySecurityGroupAndRuleTypeAndRuleTarget(
                            securityGroupVO.getId(), securityGroupRuleVO.getType(), cidr);
            if (tungstenSecurityGroupRuleVO == null) {
                tungstenSecurityGroupRuleVO = new TungstenSecurityGroupRuleVO(zoneId,
                        securityGroupVO.getId(), securityGroupRuleVO.getType(), cidr, etherType, false);
                TungstenSecurityGroupRuleVO persisted = tungstenSecurityGroupRuleDao.persist(
                        tungstenSecurityGroupRuleVO);
                if (persisted != null) {
                    int tungstenEndPort = securityGroupRuleVO.getEndPort();
                    if (securityGroupRuleVO.getProtocol().equals(NetUtils.ALL_PROTO)) {
                        tungstenEndPort = NetUtils.PORT_RANGE_MAX;
                    }

                    TungstenCommand addTungstenSecurityGroupRuleCommand =
                            new AddTungstenSecurityGroupRuleCommand(
                                    securityGroupVO.getUuid(), tungstenSecurityGroupRuleVO.getUuid(),
                                    securityGroupRuleVO.getType(), securityGroupRuleVO.getStartPort(), tungstenEndPort,
                                    cidr, etherType, securityGroupRuleVO.getProtocol());
                    TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                            addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                    if (!tungstenAnswer.getResult()) {
                        throw new CloudRuntimeException("Can not add tungsten security group rule");
                    }
                } else {
                    throw new CloudRuntimeException("Can not persist tungsten security group rule");
                }
            }
        }
    }

    public boolean removeTungstenNicSecondaryIpAddress(NicSecondaryIpVO nicSecondaryIpVO) {
        Network network = entityMgr.findById(Network.class, nicSecondaryIpVO.getNetworkId());
        DataCenter dataCenter = entityMgr.findById(DataCenter.class, network.getDataCenterId());

        TungstenCommand removeTungstenSecondaryIpAddressCommand = new RemoveTungstenSecondaryIpAddressCommand(
            TungstenUtils.getSecondaryInstanceIpName(nicSecondaryIpVO.getId()));
        TungstenAnswer removeTungstenSecondaryIpAddressAnswer = tungstenFabricUtils.sendTungstenCommand(
            removeTungstenSecondaryIpAddressCommand, network.getDataCenterId());
        if (!removeTungstenSecondaryIpAddressAnswer.getResult()) {
            return false;
        }

        if (dataCenter.isSecurityGroupEnabled() && network.getGuestType() == Network.GuestType.Shared) {
            String ipAddress = nicSecondaryIpVO.getIp4Address() != null ? nicSecondaryIpVO.getIp4Address() :
                nicSecondaryIpVO.getIp6Address();
            String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
            List<TungstenSecurityGroupRuleVO> tungstenSecurityGroupRuleVOList =
                tungstenSecurityGroupRuleDao.listByRuleTarget(
                cidr);
            for (TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO : tungstenSecurityGroupRuleVOList) {
                SecurityGroup securityGroup = securityGroupDao.findById(
                    tungstenSecurityGroupRuleVO.getSecurityGroupId());
                TungstenCommand tungstenCommand = new RemoveTungstenSecurityGroupRuleCommand(securityGroup.getUuid(),
                    tungstenSecurityGroupRuleVO.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand,
                    tungstenSecurityGroupRuleVO.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }

                if (!tungstenSecurityGroupRuleDao.expunge(tungstenSecurityGroupRuleVO.getId())) {
                    return false;
                }
            }
        }

        return true;
    }

    private void checkTungstenSecurityGroups(List<SecurityGroupVO> securityGroups) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            for (SecurityGroupVO securityGroup : securityGroups) {
                TungstenCommand getTungstenSecurityGroupCommand = new GetTungstenSecurityGroupCommand(
                    securityGroup.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(getTungstenSecurityGroupCommand,
                    tungstenProvider.getZoneId());
                if (tungstenAnswer.getApiObjectBase() == null) {
                    createTungstenSecurityGroup(securityGroup);
                }
            }
        }
    }

    @Override
    public TungstenFabricPolicyResponse createTungstenPolicy(final long zoneId, final String name) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenPolicyCommand(name, getTungstenProjectFqn(null));
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            TungstenModel tungstenModel = tungstenAnswer.getTungstenModel();
            return new TungstenFabricPolicyResponse((TungstenNetworkPolicy) tungstenModel, dataCenter);
        }
        return null;
    }

    @Override
    public TungstenFabricRuleResponse addTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String action, final String direction, final String protocol, final String srcNetwork,
        final String srcIpPrefix, final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort,
        final String destNetwork, final String destIpPrefix, final int destIpPrefixLen, final int destStartPort,
        final int destEndPort) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        AddTungstenPolicyRuleCommand addTungstenPolicyRuleCommand = new AddTungstenPolicyRuleCommand(policyUuid, action, direction,
            protocol, srcNetwork, srcIpPrefix, srcIpPrefixLen, srcStartPort, srcEndPort, destNetwork, destIpPrefix,
            destIpPrefixLen, destStartPort, destEndPort);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(addTungstenPolicyRuleCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            NetworkPolicy networkPolicy = (NetworkPolicy) tungstenAnswer.getApiObjectBase();
            PolicyEntriesType policyEntriesType = networkPolicy.getEntries();

            if (policyEntriesType == null) {
                return null;
            }

            List<PolicyRuleType> policyRuleTypeList = policyEntriesType.getPolicyRule();

            if (policyRuleTypeList == null) {
                return null;
            }

            TungstenFabricRuleResponse tungstenRuleResponse = null;
            for (PolicyRuleType policyRuleType : policyRuleTypeList) {
                if (policyRuleType.getRuleUuid()
                    .equals(addTungstenPolicyRuleCommand.getUuid())) {
                    tungstenRuleResponse = new TungstenFabricRuleResponse(networkPolicy.getUuid(), policyRuleType, dataCenter);
                }
            }

            return tungstenRuleResponse;
        }

        return null;
    }

    @Override
    public List<BaseResponse> listTungstenPolicy(final long zoneId, final Long networkId,
        final Long addressId, final String policyUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        String projectFqn = getTungstenProjectFqn(null);
        String networkUuid = getNetworkUuid(networkId);
        String policyName = addressId != null ? TungstenUtils.getPublicNetworkPolicyName(addressId) : null;
        TungstenCommand tungstenCommand = new ListTungstenPolicyCommand(projectFqn, networkUuid, policyName,
            policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenPolicyResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<TungstenModel> tungstenModelList = tungstenAnswer.getTungstenModelList();
            for (TungstenModel tungstenModel : tungstenModelList) {
                tungstenPolicyResponseList.add(new TungstenFabricPolicyResponse((TungstenNetworkPolicy) tungstenModel, dataCenter));
            }
        }
        return tungstenPolicyResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenNetwork(final long zoneId, final String networkUuid, final boolean listAll) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenNetworkCommand(projectFqn, networkUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenNetworkResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<String> nameList = new ArrayList<>();
            nameList.add(TungstenUtils.GUEST_NETWORK_NAME);
            if (listAll) {
                nameList.add(TungstenUtils.PUBLIC_NETWORK_NAME + zoneId);
            }
            List<ApiObjectBase> networkList = filterByName(tungstenAnswer.getApiObjectBaseList(), nameList);
            for (ApiObjectBase network : networkList) {
                tungstenNetworkResponseList.add(new TungstenFabricNetworkResponse((VirtualNetwork) network, dataCenter));
            }
        }
        return tungstenNetworkResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenNic(final long zoneId, final String nicUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenNicCommand(projectFqn, nicUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenNicResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> nicList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase nic : nicList) {
                tungstenNicResponseList.add(new TungstenFabricNicResponse((VirtualMachineInterface) nic, dataCenter));
            }
        }
        return tungstenNicResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenVm(final long zoneId, final String vmUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenVmCommand(projectFqn, vmUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenVmResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> vmList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase vm : vmList) {
                tungstenVmResponseList.add(new TungstenFabricVmResponse((VirtualMachine) vm, dataCenter));
            }
        }
        return tungstenVmResponseList;
    }

    @Override
    public boolean deleteTungstenPolicy(final long zoneId, final String policyUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenPolicyCommand(policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public List<BaseResponse> listTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid) {
        TungstenCommand tungstenCommand = new ListTungstenPolicyRuleCommand(policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenRuleResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            NetworkPolicy networkPolicy = (NetworkPolicy) tungstenAnswer.getApiObjectBase();
            tungstenRuleResponseList = getListTungstenPolicyRuleReponse(networkPolicy, zoneId, ruleUuid);
        }
        return tungstenRuleResponseList;
    }

    private List<BaseResponse> getListTungstenPolicyRuleReponse(NetworkPolicy networkPolicy, long zoneId, String ruleUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        List<BaseResponse> tungstenRuleResponseList = new ArrayList<>();
        PolicyEntriesType policyEntriesType = networkPolicy.getEntries();
        if (policyEntriesType != null) {
            List<PolicyRuleType> policyRuleTypeList = policyEntriesType.getPolicyRule();
            if (policyRuleTypeList != null) {
                for (PolicyRuleType policyRuleType : policyRuleTypeList) {
                    if (ruleUuid == null || policyRuleType.getRuleUuid().equals(ruleUuid)) {
                        tungstenRuleResponseList.add(
                                new TungstenFabricRuleResponse(networkPolicy.getUuid(), policyRuleType, dataCenter));
                    }
                }
            }
        }
        return tungstenRuleResponseList;
    }

    @Override
    public TungstenFabricPolicyResponse removeTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new RemoveTungstenPolicyRuleCommand(policyUuid, ruleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((TungstenNetworkPolicy) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public TungstenFabricTagResponse createTungstenTag(final long zoneId, final String tagType, final String tagValue) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenTagCommand(tagType, tagValue);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            TungstenTag tungstenTag = (TungstenTag) tungstenAnswer.getTungstenModel();
            return new TungstenFabricTagResponse(tungstenTag, dataCenter);
        }
        return null;
    }

    @Override
    public TungstenFabricTagTypeResponse createTungstenTagType(final long zoneId, final String name) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenTagTypeCommand(name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            TagType tagtype = (TagType) tungstenAnswer.getApiObjectBase();
            return new TungstenFabricTagTypeResponse(tagtype, dataCenter);
        }
        return null;
    }

    @Override
    public List<BaseResponse> listTungstenTags(final long zoneId, final String networkUuid,
        final String vmUuid, final String nicUuid, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenTagCommand(networkUuid, vmUuid, nicUuid, policyUuid, applicationPolicySetUuid, tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenTagResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<TungstenModel> tungstenModelList = tungstenAnswer.getTungstenModelList();
            for (TungstenModel tungstenModel : tungstenModelList) {
                tungstenTagResponseList.add(new TungstenFabricTagResponse((TungstenTag) tungstenModel, dataCenter));
            }
        }
        return tungstenTagResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenTagTypes(final long zoneId, final String tagTypeUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenTagTypeCommand(tagTypeUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenTagTypeResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> tagTypeList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase tagType : tagTypeList) {
                tungstenTagTypeResponseList.add(new TungstenFabricTagTypeResponse((TagType) tagType, dataCenter));
            }
        }
        return tungstenTagTypeResponseList;
    }

    @Override
    public boolean deleteTungstenTag(final long zoneId, final String tagUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenTagCommand(tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean deleteTungstenTagType(final long zoneId, final String tagTypeUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenTagTypeCommand(tagTypeUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public TungstenFabricPolicyResponse applyTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid, final int majorSequence, final int minorSequence) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ApplyTungstenNetworkPolicyCommand(networkUuid, policyUuid, majorSequence,
            minorSequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((TungstenNetworkPolicy) tungstenAnswer.getTungstenModel(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricTagResponse applyTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ApplyTungstenTagCommand(networkUuids, vmUuids, nicUuids, policyUuid, applicationPolicySetUuid,
            tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricTagResponse((TungstenTag) tungstenAnswer.getTungstenModel(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricPolicyResponse removeTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new RemoveTungstenPolicyCommand(networkUuid, policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((TungstenNetworkPolicy) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public TungstenFabricTagResponse removeTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new RemoveTungstenTagCommand(networkUuids, vmUuids, nicUuids, policyUuid,
            applicationPolicySetUuid, tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricTagResponse((TungstenTag) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public TungstenFabricAddressGroupResponse createTungstenAddressGroup(final long zoneId, final String name,
        final String ipPrefix, final int ipPrefixLen) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenAddressGroupCommand(name, ipPrefix, ipPrefixLen);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricAddressGroupResponse((AddressGroup) tungstenAnswer.getApiObjectBase(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricServiceGroupResponse createTungstenServiceGroup(final long zoneId, final String name,
        final String protocol, final int startPort, final int endPort) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenServiceGroupCommand(name, protocol, startPort, endPort);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricServiceGroupResponse((ServiceGroup) tungstenAnswer.getApiObjectBase(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallRuleResponse createTungstenFirewallRule(final long zoneId, final String firewallPolicyUuid, final String name,
        final String action, final String serviceGroupUuid, final String srcTagUuid, final String srcAddressGroupUuid, final String srcNetworkUuid,
        final String direction, final String destTagUuid, final String destAddressGroupUuid, final String destNetworkUuid, final String tagTypeUuid, final int sequence) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenFirewallRuleCommand(firewallPolicyUuid, name, action, serviceGroupUuid,
            srcTagUuid, srcAddressGroupUuid, srcNetworkUuid, direction, destTagUuid, destAddressGroupUuid, destNetworkUuid, tagTypeUuid, sequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallRuleResponse(
                (net.juniper.tungsten.api.types.FirewallRule) tungstenAnswer.getApiObjectBase(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallPolicyResponse createTungstenFirewallPolicy(final long zoneId, final  String applicationPolicySetUuid, final String name, final int sequence) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenFirewallPolicyCommand(name, applicationPolicySetUuid, sequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallPolicyResponse((FirewallPolicy) tungstenAnswer.getApiObjectBase(), dataCenter);
        }

        return null;
    }

    @Override
    public TungstenFabricApplicationPolicySetResponse createTungstenApplicationPolicySet(final long zoneId,
        final String name) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenApplicationPolicySetCommand(name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricApplicationPolicySetResponse(
                (ApplicationPolicySet) tungstenAnswer.getApiObjectBase(), dataCenter);
        }

        return null;
    }

    @Override
    public List<BaseResponse> listTungstenApplicationPolicySet(final long zoneId,
        final String applicationPolicySetUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenApplicationPolicySetCommand(applicationPolicySetUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenApplicationPolicySetResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> applicationPolicySetList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase applicationPolicySet : applicationPolicySetList) {
                tungstenApplicationPolicySetResponseList.add(
                    new TungstenFabricApplicationPolicySetResponse((ApplicationPolicySet) applicationPolicySet, dataCenter));
            }
        }
        return tungstenApplicationPolicySetResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenFirewallPolicyCommand(applicationPolicySetUuid,
            firewallPolicyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenFirewallPolicyResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> firewallPolicyList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase firewallPolicy : firewallPolicyList) {
                tungstenFirewallPolicyResponseList.add(new TungstenFabricFirewallPolicyResponse((FirewallPolicy) firewallPolicy, dataCenter));
            }
        }
        return tungstenFirewallPolicyResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenFirewallRule(final long zoneId,
        final String firewallPolicyUuid, final String firewallRuleUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenFirewallRuleCommand(firewallPolicyUuid, firewallRuleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenFirewallRuleResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> firewallRuleList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase firewallRule : firewallRuleList) {
                tungstenFirewallRuleResponseList.add(new TungstenFabricFirewallRuleResponse((net.juniper.tungsten.api.types.FirewallRule) firewallRule, dataCenter));
            }
        }
        return tungstenFirewallRuleResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenServiceGroup(final long zoneId,
        final String serviceGroupUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenServiceGroupCommand(serviceGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenServiceGroupResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> serviceGroupList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase serviceGroup : serviceGroupList) {
                tungstenServiceGroupResponseList.add(new TungstenFabricServiceGroupResponse((ServiceGroup) serviceGroup, dataCenter));
            }
        }
        return tungstenServiceGroupResponseList;
    }

    @Override
    public List<BaseResponse> listTungstenAddressGroup(final long zoneId,
        final String addressGroupUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenAddressGroupCommand(addressGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenAddressGroupResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApiObjectBase> addressGroupList = tungstenAnswer.getApiObjectBaseList();
            for (ApiObjectBase addressGroup : addressGroupList) {
                tungstenAddressGroupResponseList.add(new TungstenFabricAddressGroupResponse((AddressGroup) addressGroup, dataCenter));
            }
        }
        return tungstenAddressGroupResponseList;
    }

    @Override
    public boolean deleteTungstenApplicationPolicySet(final long zoneId, final String applicationPolicySetUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenApplicationPolicySetCommand(applicationPolicySetUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean deleteTungstenFirewallPolicy(final long zoneId, final String firewallPolicyUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenFirewallPolicyCommand(firewallPolicyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean deleteTungstenFirewallRule(final long zoneId, final String firewallRuleUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenFirewallRuleCommand(firewallRuleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean deleteTungstenServiceGroup(final long zoneId, final String serviceGroupUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenServiceGroupCommand(serviceGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean deleteTungstenAddressGroup(final long zoneId, final String addressGroupUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenAddressGroupCommand(addressGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean createSharedNetwork(final Network network, final Vlan vlan) {
        String tungstenProjectFqn = getTungstenProjectFqn(network);
        TungstenCommand createTungstenSharedNetworkCommand = new CreateTungstenNetworkCommand(network.getUuid(),
            TungstenUtils.getSharedNetworkName(network.getId()), network.getName(), tungstenProjectFqn, true, true,
            null, 0, null, network.getMode().equals(Networks.Mode.Dhcp), null, null, null, false, false, null);
        TungstenAnswer createSharedNetwork = tungstenFabricUtils.sendTungstenCommand(createTungstenSharedNetworkCommand,
            network.getDataCenterId());

        if (!createSharedNetwork.getResult()) {
            return false;
        }

        if (vlan.getVlanGateway() != null) {
            Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());
            String[] ipAddress = vlan.getIpRange().split("-");

            TungstenCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(network.getUuid(),
                pair.first(), pair.second(), network.getGateway(), true, null, ipAddress[0], ipAddress[1], false,
                TungstenUtils.getIPV4SubnetName(network.getId()));

            TungstenAnswer addIpV4NetworkSubnetAnswer = tungstenFabricUtils.sendTungstenCommand(
                addTungstenNetworkSubnetCommand, network.getDataCenterId());
            if (!addIpV4NetworkSubnetAnswer.getResult()) {
                return false;
            }

            if (!setupVrouter(network, createSharedNetwork.getApiObjectBase().getQualifiedName())) {
                return false;
            }

            if (!allocateDnsIpAddress(network, null, TungstenUtils.getIPV4SubnetName(network.getId()))) {
                return false;
            }
        }

        if (vlan.getIp6Gateway() != null) {
            Pair<String, Integer> pair = NetUtils.getCidr(vlan.getIp6Cidr());
            String[] ipAddress = vlan.getIp6Range().split("-");
            TungstenCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(network.getUuid(),
                pair.first(), pair.second(), vlan.getIp6Gateway(), false, null, ipAddress[0], ipAddress[1], false,
                TungstenUtils.getIPV6SubnetName(network.getId()));

            TungstenAnswer addIpV6NetworkSubnetAnswer = tungstenFabricUtils.sendTungstenCommand(
                addTungstenNetworkSubnetCommand, network.getDataCenterId());
            return addIpV6NetworkSubnetAnswer.getResult();
        }

        return true;
    }

    private boolean setupVrouter(Network network, List<String> qualifiedName) {
        NetworkDetailVO networkDetailVO = networkDetailsDao.findDetail(network.getId(), "vrf");
        if (networkDetailVO == null) {
            networkDetailVO = new NetworkDetailVO(network.getId(), "vrf",
                    TungstenUtils.getVrfNetworkName(qualifiedName), false);
            NetworkDetailVO persistNetworkDetail = networkDetailsDao.persist(networkDetailVO);
            if (persistNetworkDetail != null) {
                TungstenProviderVO tungstenProvider = tungstenProviderDao.findByZoneId(network.getDataCenterId());
                if (tungstenProvider != null) {
                    Host host = hostDao.findByPublicIp(tungstenProvider.getGateway());
                    if (host != null) {
                        Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("create",
                                TungstenUtils.getSgVgwName(network.getId()), network.getCidr(), NetUtils.ALL_IP4_CIDRS,
                                persistNetworkDetail.getValue());
                        Answer answer = agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
                        return answer != null && answer.getResult();
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean addTungstenVmSecurityGroup(VMInstanceVO vm) {
        DataCenter dataCenter = dataCenterDao.findById(vm.getDataCenterId());
        if (!dataCenter.isSecurityGroupEnabled()) {
            return true;
        }

        // if security group is not exist, create it
        List<SecurityGroupVO> securityGroupVOList = securityGroupManager.getSecurityGroupsForVm(vm.getId());
        checkTungstenSecurityGroups(securityGroupVOList);

        // if vm is in tungsten zone, add security group to vmi
        Nic nic = nicDao.findDefaultNicForVM(vm.getId());
        if (nic != null && nic.getBroadcastUri().equals(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"))) {
            List<String> securityGroupUuidList = new ArrayList<>();
            for (SecurityGroupVO securityGroupVO : securityGroupVOList) {
                securityGroupUuidList.add(securityGroupVO.getUuid());
            }

            TungstenCommand addTungstenVmToSecurityGroupCommand = new AddTungstenVmToSecurityGroupCommand(nic.getUuid(),
                securityGroupUuidList);
            TungstenAnswer addTungstenVmToSecurityGroupAnswer = tungstenFabricUtils.sendTungstenCommand(addTungstenVmToSecurityGroupCommand,
                vm.getDataCenterId());
            if (!addTungstenVmToSecurityGroupAnswer.getResult()) {
                return false;
            }

            return addVmIp(securityGroupVOList, nic);

        }

        return true;
    }

    private boolean addVmIp(List<SecurityGroupVO> securityGroupVOList, Nic nic) {
        // add vm to tungsten security group rule
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            for (SecurityGroupVO securityGroupVO : securityGroupVOList) {
                List<SecurityGroupRuleVO> securityGroupRuleVOList = securityGroupRuleDao.listByAllowedSecurityGroupId(
                        securityGroupVO.getId());
                for (SecurityGroupRuleVO securityGroupRuleVO : securityGroupRuleVOList) {
                    addSecurityGroupRule(securityGroupRuleVO, nic, tungstenProvider, securityGroupVO);
                }
            }
        }

        return true;
    }

    private void addSecurityGroupRule(SecurityGroupRuleVO securityGroupRuleVO, Nic nic,
                                      TungstenProviderVO tungstenProvider, SecurityGroupVO securityGroupVO) {
        List<String> ipAddressList = getListIpAddressFromNic(nic);
        int tungstenEndPort = securityGroupRuleVO.getEndPort();
        if (securityGroupRuleVO.getProtocol().equals(NetUtils.ALL_PROTO)) {
            tungstenEndPort = NetUtils.PORT_RANGE_MAX;
        }

        for (String ipAddress : ipAddressList) {
            String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
            String etherType = TungstenUtils.getEthertTypeFromCidr(cidr);
            TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = new TungstenSecurityGroupRuleVO(
                    tungstenProvider.getZoneId(), securityGroupRuleVO.getSecurityGroupId(),
                    securityGroupRuleVO.getType(), cidr, etherType, false);
            TungstenSecurityGroupRuleVO persisted = tungstenSecurityGroupRuleDao.persist(
                    tungstenSecurityGroupRuleVO);
            if (persisted != null) {
                TungstenCommand addTungstenSecurityGroupRuleCommand = new AddTungstenSecurityGroupRuleCommand(
                        securityGroupVO.getUuid(), persisted.getUuid(), securityGroupRuleVO.getType(),
                        securityGroupRuleVO.getStartPort(), tungstenEndPort, cidr, etherType,
                        securityGroupRuleVO.getProtocol());
                TungstenAnswer addTungstenSecurityGroupRuleAnswer =
                 tungstenFabricUtils.sendTungstenCommand(addTungstenSecurityGroupRuleCommand,
                        tungstenProvider.getZoneId());
                if (!addTungstenSecurityGroupRuleAnswer.getResult()) {
                    throw new CloudRuntimeException("Can not add Tungsten Fabric Security Group Rule");
                }
            }
        }
    }

    @Override
    public boolean removeTungstenVmSecurityGroup(VMInstanceVO vm) {
        DataCenter dataCenter = dataCenterDao.findById(vm.getDataCenterId());
        TungstenProvider tungstenProvider = tungstenProviderDao.findByZoneId(dataCenter.getId());
        if (!dataCenter.isSecurityGroupEnabled() || tungstenProvider == null) {
            return true;
        }

        List<SecurityGroupVO> securityGroupVOList = securityGroupManager.getSecurityGroupsForVm(vm.getId());

        // remove vmi security group
        Nic nic = nicDao.findDefaultNicForVM(vm.getId());
        if (nic != null) {
            List<String> securityGroupUuidList = new ArrayList<>();
            for (SecurityGroupVO securityGroupVO : securityGroupVOList) {
                securityGroupUuidList.add(securityGroupVO.getUuid());
            }

            TungstenCommand removeTungstenVmFromSecurityGroupCommand = new RemoveTungstenVmFromSecurityGroupCommand(nic.getUuid(),
                securityGroupUuidList);
            TungstenAnswer removeTungstenVmFromSecurityGroupAnswer = tungstenFabricUtils.sendTungstenCommand(removeTungstenVmFromSecurityGroupCommand,
                vm.getDataCenterId());
            if (!removeTungstenVmFromSecurityGroupAnswer.getResult()) {
                return false;
            }

            return removeVmIp(nic);
        }

        return true;
    }

    private boolean removeVmIp(Nic nic) {
        // remove vm security group rule
        List<String> ipAddressList = getListIpAddressFromNic(nic);
        for (String ipAddress : ipAddressList) {
            String cidr = TungstenUtils.getSingleIpAddressCidr(ipAddress);
            List<TungstenSecurityGroupRuleVO> tungstenSecurityGroupRuleVOList =
                    tungstenSecurityGroupRuleDao.listByRuleTarget(
                            cidr);
            for (TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO : tungstenSecurityGroupRuleVOList) {
                SecurityGroup securityGroup = securityGroupDao.findById(
                        tungstenSecurityGroupRuleVO.getSecurityGroupId());
                TungstenCommand removeTungstenSecurityGroupRuleCommand = new RemoveTungstenSecurityGroupRuleCommand(securityGroup.getUuid(),
                        tungstenSecurityGroupRuleVO.getUuid());
                TungstenAnswer removeTungstenSecurityGroupRuleAnswer = tungstenFabricUtils.sendTungstenCommand(removeTungstenSecurityGroupRuleCommand,
                        tungstenSecurityGroupRuleVO.getZoneId());
                if (!removeTungstenSecurityGroupRuleAnswer.getResult()) {
                    return false;
                }

                if (!tungstenSecurityGroupRuleDao.expunge(tungstenSecurityGroupRuleVO.getId())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public BaseResponse createRoutingLogicalRouter(final long zoneId, final String projectFqn, final String name) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new CreateTungstenRoutingLogicalRouterCommand(projectFqn, name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricLogicalRouterResponse((TungstenLogicalRouter) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public BaseResponse addNetworkGatewayToLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        Network network = networkDao.findByUuid(networkUuid);
        String ipAddress = ipAddressManager.acquireLastGuestIpAddress(network);
        TungstenCommand tungstenCommand = new AddTungstenNetworkGatewayToLogicalRouterCommand(networkUuid, logicalRouterUuid,
            ipAddress);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            tungstenGuestNetworkIpAddressDao.persist(
                new TungstenGuestNetworkIpAddressVO(network.getId(), new Ip(ipAddress), logicalRouterUuid));
            return new TungstenFabricLogicalRouterResponse((TungstenLogicalRouter) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public List<BaseResponse> listRoutingLogicalRouter(final long zoneId, final String networkUuid, final String logicalRouterUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new ListTungstenRoutingLogicalRouterCommand(networkUuid, logicalRouterUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<BaseResponse> tungstenLogicalRouterResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<TungstenModel> tungstenModelList = tungstenAnswer.getTungstenModelList();
            for (TungstenModel tungstenModel : tungstenModelList) {
                tungstenLogicalRouterResponseList.add(
                    new TungstenFabricLogicalRouterResponse((TungstenLogicalRouter) tungstenModel, dataCenter));
            }
        }
        return tungstenLogicalRouterResponseList;
    }

    @Override
    public BaseResponse removeNetworkGatewayFromLogicalRouter(final long zoneId, final String networkUuid,
        final String logicalRouterUuid) {
        DataCenter dataCenter = dataCenterDao.findById(zoneId);
        TungstenCommand tungstenCommand = new RemoveTungstenNetworkGatewayFromLogicalRouterCommand(networkUuid,
            logicalRouterUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            Network network = networkDao.findByUuid(networkUuid);
            TungstenGuestNetworkIpAddressVO gatewayIp = tungstenGuestNetworkIpAddressDao.findByNetworkAndLogicalRouter(
                network.getId(), logicalRouterUuid);
            if (gatewayIp != null) {
                tungstenGuestNetworkIpAddressDao.expunge(gatewayIp.getId());
            }
            return new TungstenFabricLogicalRouterResponse((TungstenLogicalRouter) tungstenAnswer.getTungstenModel(), dataCenter);
        }
        return null;
    }

    @Override
    public boolean deleteLogicalRouter(final long zoneId, final String logicalRouterUuid) {
        TungstenCommand tungstenCommand = new DeleteTungstenRoutingLogicalRouterCommand(logicalRouterUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public List<String> listConnectedNetworkFromLogicalRouter(final long zoneId, final String logicalRouterUuid) {
        List<String> networkList = new ArrayList<>();
        TungstenCommand tungstenCommand = new ListTungstenConnectedNetworkFromLogicalRouterCommand(logicalRouterUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            for(ApiObjectBase apiObjectBase : tungstenAnswer.getApiObjectBaseList()) {
                networkList.add(apiObjectBase.getUuid());
            }
        }
        return networkList;
    }

    @Override
    public TungstenFabricLBHealthMonitorVO updateTungstenFabricLBHealthMonitor(final long lbId, final String type,
        final int retry, final int timeout, final int interval, final String httpMethod, final String expectedCode,
        final String urlPath) {
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO = tungstenFabricLBHealthMonitorDao.findByLbId(lbId);
        if (tungstenFabricLBHealthMonitorVO == null) {
            tungstenFabricLBHealthMonitorVO = new TungstenFabricLBHealthMonitorVO(lbId, type, retry, timeout, interval, httpMethod, expectedCode, urlPath);
        } else {
            tungstenFabricLBHealthMonitorVO.setType(type);
            tungstenFabricLBHealthMonitorVO.setRetry(retry);
            tungstenFabricLBHealthMonitorVO.setTimeout(timeout);
            tungstenFabricLBHealthMonitorVO.setInterval(interval);
            tungstenFabricLBHealthMonitorVO.setHttpMethod(httpMethod);
            tungstenFabricLBHealthMonitorVO.setExpectedCode(expectedCode);
            tungstenFabricLBHealthMonitorVO.setUrlPath(urlPath);
        }
        return tungstenFabricLBHealthMonitorDao.persist(tungstenFabricLBHealthMonitorVO);
    }

    @Override
    public boolean applyLBHealthMonitor(final long lbId) {
        boolean success = true;
        LoadBalancerVO loadBalancer = loadBalancerDao.findById(lbId);
        if (loadBalancer == null) {
            throw new InvalidParameterException("Invalid Load balancer Id:" + lbId);
        }

        if (loadBalancer.getState() == FirewallRule.State.Active) {
            loadBalancer.setState(FirewallRule.State.Add);
            loadBalancerDao.persist(loadBalancer);
        }

        try {
            loadBalancingRulesService.applyLoadBalancerConfig(lbId);
        } catch (ResourceUnavailableException e) {
            success = false;
        }
        return success;
    }

    @Override
    public List<BaseResponse> listTungstenFabricLBHealthMonitor(final long lbId) {
        List<BaseResponse> responseList = new ArrayList<>();
        LoadBalancer loadBalancer = loadBalancerDao.findById(lbId);
        Network network = networkDao.findById(loadBalancer.getNetworkId());
        DataCenter dataCenter = dataCenterDao.findById(network.getDataCenterId());
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO = tungstenFabricLBHealthMonitorDao.findByLbId(lbId);
        if (tungstenFabricLBHealthMonitorVO != null) {
            responseList.add(new TungstenFabricLBHealthMonitorResponse(tungstenFabricLBHealthMonitorVO, dataCenter));
        }
        return responseList;
    }

    private List<String> getListIpAddressFromNic(Nic nic) {
        List<String> ipAddressList = new ArrayList<>();
        if (nic.getIPv4Address() != null) {
            ipAddressList.add(nic.getIPv4Address());
        }

        if (nic.getIPv6Address() != null) {
            ipAddressList.add(nic.getIPv6Address());
        }

        if (nic.getSecondaryIp()) {
            ipAddressList.addAll(nicSecIpDao.getSecondaryIpAddressesForNic(nic.getId()));
        }

        return ipAddressList;
    }

    private String getNetworkUuid(Long networkId) {
        if (networkId == null)
            return null;

        Network network = networkDao.findById(networkId);

        if (network != null) {
            return network.getUuid();
        }

        return null;
    }

    private List<ApiObjectBase> filterByName(List<? extends ApiObjectBase> apiObjectBaseList, List<String> nameList) {
        List<ApiObjectBase> resultList = new ArrayList<>();
        for(String name : nameList) {
            for(ApiObjectBase apiObjectBase : apiObjectBaseList) {
                if (apiObjectBase.getName().startsWith(name)) {
                    resultList.add(apiObjectBase);
                }
            }
        }
        return resultList;
    }

    @Override
    public String getConfigComponentName() {
        return TungstenServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
            TUNGSTEN_ENABLED
        };
    }
}
