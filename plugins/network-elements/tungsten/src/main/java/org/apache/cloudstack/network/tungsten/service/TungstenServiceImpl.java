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
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.PolicyEntriesType;
import net.juniper.tungsten.api.types.PolicyRuleType;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.sf.cglib.proxy.Enhancer;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
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
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenObjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNicCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerSslCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerStatsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenVrouterConfigCommand;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricAddressGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricApplicationPolicySetResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricFirewallRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricNicResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricPolicyResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricRuleResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricServiceGroupResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagTypeResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricVmResponse;
import org.apache.cloudstack.network.tungsten.model.TungstenFloatingIP;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class TungstenServiceImpl extends ManagerBase implements TungstenService {
    private static final Logger s_logger = Logger.getLogger(TungstenServiceImpl.class);
    @Inject
    private MessageBus messageBus;
    @Inject
    private ProjectDao projectDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private EntityManager entityMgr;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    private DomainDao domainDao;
    @Inject
    private LoadBalancerCertMapDao lbCertMapDao;
    @Inject
    private FirewallRulesDao fwRulesDao;
    @Inject
    private TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;
    @Inject
    private TungstenProviderDao tungstenProviderDao;
    @Inject
    private TungstenFabricUtils tungstenFabricUtils;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private HostDao hostDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    private SecurityGroupDao securityGroupDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private NicDao nicDao;

    @Override
    public boolean start() {
        subscribeTungstenEvent();
        return true;
    }

    @Override
    public boolean synchronizeTungstenData(Long tungstenProviderId) {
        boolean result = true;
        TungstenProviderVO tungstenProviderVO = tungstenProviderDao.findById(tungstenProviderId);
        if (tungstenProviderVO == null) {
            return false;
        }

        long zoneId = tungstenProviderVO.getZoneId();
        if (!syncTungstenFloatingIp(zoneId)) {
            result = false;
        }

        syncTungstenDbWithCloudstackProjectsAndDomains();

        return result;
    }

    private boolean syncTungstenFloatingIp(long zoneId) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        List<IPAddressVO> ipAddressList = ipAddressDao.listByDcIdAndAssociatedNetwork(zoneId);
        TungstenCommand getTungstenFloatingIpsCommand = new GetTungstenFloatingIpsCommand(publicNetwork.getUuid(),
            TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(getTungstenFloatingIpsCommand, zoneId);
        List<FloatingIp> floatingIpList = (List<FloatingIp>) tungstenAnswer.getApiObjectBaseList();
        boolean result = getDifferent(ipAddressList, floatingIpList, TungstenFloatingIP.class);
        if (result) {
            for (IPAddressVO ipAddressVO : ipAddressList) {
                createTungstenFloatingIp(zoneId, ipAddressVO);
            }

            for (FloatingIp floatingIp : floatingIpList) {
                deleteTungstenFloatingIp(zoneId, floatingIp);
            }
        }
        return result;
    }

    private Class getClass(Object o) {
        return Enhancer.isEnhanced(o.getClass()) ? o.getClass().getSuperclass() : o.getClass();
    }

    private boolean getDifferent(List list1, List list2, Class cls) {
        try {
            List tmp1 = new ArrayList();
            List tmp2 = new ArrayList();

            for (Object o : list1) {
                Constructor constructor = cls.getConstructor(getClass(o));
                tmp1.add(constructor.newInstance(o));
            }

            for (Object o : list2) {
                Constructor constructor = cls.getConstructor(getClass(o));
                tmp2.add(constructor.newInstance(o));
            }

            List removeList1 = new ArrayList();
            List removeList2 = new ArrayList();

            List list = (List) CollectionUtils.intersection(tmp1, tmp2);
            for (Object o : list) {
                for (Object i : list1) {
                    if (o.equals(i)) {
                        removeList1.add(i);
                    }
                }
                for (Object i : list2) {
                    if (o.equals(i)) {
                        removeList2.add(i);
                    }
                }
            }

            list1.removeAll(removeList1);
            list2.removeAll(removeList2);

            return true;
        } catch (Exception e) {
            s_logger.error("Can not get different");
            return false;
        }
    }

    @Override
    public void subscribeTungstenEvent() {
        messageBus.subscribe(IpAddressManager.MESSAGE_ASSIGN_IPADDR_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
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
            }
        });

        messageBus.subscribe(IpAddressManager.MESSAGE_RELEASE_IPADDR_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
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
            }
        });

        messageBus.subscribe(TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final Network network = (Network) args;
                    List<IPAddressVO> ipAddressVOList = ipAddressDao.listByAccount(Account.ACCOUNT_ID_SYSTEM);
                    for (IPAddressVO ipAddressVO : ipAddressVOList) {
                        ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand =
                            new ApplyTungstenNetworkPolicyCommand(
                            null, TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), network.getUuid(), 1,
                            0);
                        tungstenFabricUtils.sendTungstenCommand(applyTungstenNetworkPolicyCommand,
                            network.getDataCenterId());
                    }
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_VLAN_IP_RANGE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final VlanVO vlanVO = (VlanVO) args;
                    addPublicNetworkSubnet(vlanVO);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_VLAN_IP_RANGE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final VlanVO vlanVO = (VlanVO) args;
                    removePublicNetworkSubnet(vlanVO);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_POD_IP_RANGE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final HostPodVO pod = (HostPodVO) args;
                    addManagementNetworkSubnet(pod);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_POD_IP_RANGE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final HostPodVO pod = (HostPodVO) args;
                    removeManagementNetworkSubnet(pod);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_CREATE_TUNGSTEN_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final DomainVO domain = (DomainVO) args;
                    createTungstenDomain(domain);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(DomainManager.MESSAGE_DELETE_TUNGSTEN_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final DomainVO domain = (DomainVO) args;
                    deleteTungstenDomain(domain);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ProjectManager.MESSAGE_CREATE_TUNGSTEN_PROJECT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Project project = (Project) args;
                    createTungstenProject(project);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(ProjectManager.MESSAGE_DELETE_TUNGSTEN_PROJECT_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Project project = (Project) args;
                    deleteTungstenProject(project);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT,
            new MessageSubscriber() {
                @Override
                public void onPublishMessage(String senderAddress, String subject, Object args) {
                    try {
                        syncTungstenDbWithCloudstackProjectsAndDomains();
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                }
            });

        messageBus.subscribe(SecurityGroupService.MESSAGE_CREATE_TUNGSTEN_SECURITY_GROUP_EVENT,
            new MessageSubscriber() {
                @Override
                public void onPublishMessage(String senderAddress, String subject, Object args) {
                    try {
                        final SecurityGroup securityGroup = (SecurityGroup) args;
                        createTungstenSecurityGroup(securityGroup);
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                }
            });

        messageBus.subscribe(SecurityGroupService.MESSAGE_DELETE_TUNGSTEN_SECURITY_GROUP_EVENT,
            new MessageSubscriber() {
                @Override
                public void onPublishMessage(String senderAddress, String subject, Object args) {
                    try {
                        final SecurityGroup securityGroup = (SecurityGroup) args;
                        deleteTungstenSecurityGroup(securityGroup);
                    } catch (final Exception e) {
                        s_logger.error(e.getMessage());
                    }
                }
            });

        messageBus.subscribe(SecurityGroupService.MESSAGE_ADD_SECURITY_GROUP_RULE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final List<SecurityRule> securityRules = (List<SecurityRule>) args;
                    addTungstenSecurityGroupRule(securityRules);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(SecurityGroupService.MESSAGE_ADD_VM_TO_SECURITY_GROUPS_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final Pair<Long, List<SecurityGroupVO>> pair = (Pair<Long, List<SecurityGroupVO>>) args;
                    addTungstenVmToSecurityGroup(pair);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        messageBus.subscribe(SecurityGroupService.MESSAGE_REMOVE_SECURITY_GROUP_RULE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    final SecurityRule securityRule = (SecurityRule) args;
                    removeTungstenSecurityGroupRule(securityRule);
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });
    }

    public List<TungstenProviderVO> getTungstenProviders() {
        List<TungstenProviderVO> tungstenProviders = tungstenProviderDao.findAll();
        if (tungstenProviders != null) {
            return tungstenProviders;
        } else {
            return new ArrayList<>();
        }
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
        List<NetworkVO> publicNetworkVOList = networkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        NetworkVO publicNetwork = publicNetworkVOList.get(0);
        TungstenCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(publicNetwork.getUuid(),
            TungstenUtils.getFloatingIpPoolName(zoneId), TungstenUtils.getFloatingIpName(ipAddress.getId()));
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpCommand,
            zoneId);
        return tungstenAnswer.getResult();
    }

    private boolean deleteTungstenFloatingIp(long zoneId, FloatingIp floatingIp) {
        TungstenCommand tungstenCommand = new DeleteTungstenObjectCommand(floatingIp);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    private String getProject(long accountId) {
        Account account = accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
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
        TungstenAnswer createTungstenPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenPolicyCommand, zoneId);
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
            TungstenAnswer createTungstenNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
                addTungstenNetworkSubnetCommand, pod.getDataCenterId());

            if (!createTungstenNetworkAnswer.getResult()) {
                return false;
            }
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
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean removeManagementNetworkSubnet(HostPodVO pod) {
        Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
            Networks.TrafficType.Management);

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
            null, 0, null, true, "0.0.0.0", null, null, false, false, null);
        TungstenAnswer createPublicNetworkAnswer = tungstenFabricUtils.sendTungstenCommand(
            createTungstenPublicNetworkCommand, zoneId);
        if (!createPublicNetworkAnswer.getResult()) {
            return false;
        }

        // change default tungsten security group
        // change default forwarding mode
        TungstenCommand updateTungstenGlobalVrouterConfigCommand = new UpdateTungstenVrouterConfigCommand(
            TungstenUtils.getDefaultForwardingMode());
        TungstenAnswer updateTungstenGlobalVrouterConfigAnswer = tungstenFabricUtils.sendTungstenCommand(
            updateTungstenGlobalVrouterConfigCommand, zoneId);
        if (!updateTungstenGlobalVrouterConfigAnswer.getResult()) {
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
        networkDetailsDao.persist(networkDetailVO);

        return true;
    }

    @Override
    public boolean addPublicNetworkSubnet(Vlan pubVlanVO) {
        long zoneId = pubVlanVO.getDataCenterId();
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public ip address
        String[] ipAddress = pubVlanVO.getIpRange().split("-");
        String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
            pubVlanVO.getVlanNetmask());
        Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);

        TungstenCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(publicNetwork.getUuid(),
            publicPair.first(), publicPair.second(), pubVlanVO.getVlanGateway(), true, null, ipAddress[0], ipAddress[1],
            false, pubVlanVO.getUuid());
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

        return true;
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

    public boolean deleteTungstenDomain(DomainVO domain) {
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

    public boolean createTungstenProject(Project project) {
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
            }
        }
        return true;
    }

    public boolean deleteTungstenProject(Project project) {
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

    public void syncTungstenDbWithCloudstackProjectsAndDomains() {
        List<DomainVO> cloudstackDomains = domainDao.listAll();
        List<ProjectVO> cloudstackProjects = projectDao.listAll();

        if (cloudstackDomains != null) {
            for (DomainVO domain : cloudstackDomains) {
                createTungstenDomain(domain);
            }
        }

        if (cloudstackProjects != null) {
            for (ProjectVO project : cloudstackProjects) {
                createTungstenProject(project);
            }
        }
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

        }

        // update haproxy stats
        String lbStatsVisibility = configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        if (!lbStatsVisibility.equals("disabled")) {
            String lbStatsUri = configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
            String lbStatsAuth = configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
            String lbStatsPort = configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
            UpdateTungstenLoadbalancerStatsCommand updateTungstenLoadbalancerStatsCommand =
                new UpdateTungstenLoadbalancerStatsCommand(
                getTungstenLoadBalancerAnswer.getApiObjectBase().getUuid(), lbStatsPort, lbStatsUri, lbStatsAuth);
            for (HostVO host : hostList) {
                agentMgr.easySend(host.getId(), updateTungstenLoadbalancerStatsCommand);
            }
        }

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
                    new UpdateTungstenLoadbalancerSslCommand(
                    getTungstenLoadBalancerAnswer.getApiObjectBase().getUuid(), certVO.getName(),
                    certVO.getCertificate(), certVO.getKey(),
                    tungstenGuestNetworkIpAddressVO.getGuestIpAddress().addr(), String.valueOf(NetUtils.HTTPS_PORT));
                for (HostVO host : hostList) {
                    agentMgr.easySend(host.getId(), updateTungstenLoadbalancerSslCommand);
                }
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
            TungstenCommand createTungstenSecurityGroupCommand = new CreateTungstenSecurityGroupCommand(
                securityGroup.getUuid(), securityGroup.getName(), securityGroup.getDescription(), projectFqn);
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(createTungstenSecurityGroupCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
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
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            for (SecurityRule securityRule : securityRules) {
                SecurityGroup securityGroup = securityGroupDao.findById(securityRule.getSecurityGroupId());
                Pair<String, Integer> pair = NetUtils.getCidr(securityRule.getAllowedSourceIpCidr());
                TungstenCommand addTungstenSecurityGroupRuleCommand = new AddTungstenSecurityGroupRuleCommand(
                    securityGroup.getUuid(), securityRule.getUuid(), securityRule.getType(),
                    securityRule.getStartPort(), securityRule.getEndPort(), securityRule.getAllowedSourceIpCidr(),
                    pair.first(), pair.second(), securityRule.getProtocol());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                    addTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean removeTungstenSecurityGroupRule(SecurityRule securityRule) {
        SecurityGroup securityGroup = securityGroupDao.findById(securityRule.getSecurityGroupId());
        if (securityGroup == null)
            return false;
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            TungstenCommand removeTungstenSecurityGroupRuleCommand = new RemoveTungstenSecurityGroupRuleCommand(
                securityGroup.getUuid(), securityRule.getUuid(), securityRule.getType());
            TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                removeTungstenSecurityGroupRuleCommand, tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean addTungstenVmToSecurityGroup(Pair<Long, List<SecurityGroupVO>> pair) {
        long vmId = pair.first();
        List<String> securityGroupUuidList = new ArrayList<>();
        UserVm userVm = userVmDao.findById(vmId);
        for (SecurityGroupVO securityGroup : pair.second()) {
            securityGroupUuidList.add(securityGroup.getUuid());
        }

        //check if this security group exists in tungsten
        //if not create the security group
        checkTungstenSecurityGroups(pair.second());

        if (userVm != null && !securityGroupUuidList.isEmpty()) {
            for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
                TungstenCommand addTungstenVmToSecurityGroupCommand = new AddTungstenVmToSecurityGroupCommand(
                    userVm.getUuid(), securityGroupUuidList);
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                    addTungstenVmToSecurityGroupCommand, tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void checkTungstenSecurityGroups(List<SecurityGroupVO> securityGroups) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            for (SecurityGroupVO securityGroup : securityGroups) {
                TungstenCommand getTungstenSecurityGroupCommand = new GetTungstenSecurityGroupCommand(
                    securityGroup.getUuid());
                TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(
                    getTungstenSecurityGroupCommand, tungstenProvider.getZoneId());
                if (tungstenAnswer.getApiObjectBase() == null) {
                    createTungstenSecurityGroup(securityGroup);
                }
            }
        }
    }

    @Override
    public TungstenFabricPolicyResponse createTungstenPolicy(final long zoneId, final String name) {
        TungstenCommand tungstenCommand = new CreateTungstenPolicyCommand(name, getTungstenProjectFqn(null));
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            NetworkPolicy networkPolicy = (NetworkPolicy) tungstenAnswer.getApiObjectBase();
            TungstenFabricPolicyResponse tungstenPolicyResponse = new TungstenFabricPolicyResponse(networkPolicy);
            return tungstenPolicyResponse;
        }
        return null;
    }

    @Override
    public TungstenFabricRuleResponse addTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String action, final String direction, final String protocol, final String srcNetwork,
        final String srcIpPrefix, final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort,
        final String destNetwork, final String destIpPrefix, final int destIpPrefixLen, final int destStartPort,
        final int destEndPort) {
        TungstenCommand addTungstenPolicyRuleCommand = new AddTungstenPolicyRuleCommand(policyUuid, action, direction,
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
                    .equals(((AddTungstenPolicyRuleCommand) addTungstenPolicyRuleCommand).getUuid())) {
                    tungstenRuleResponse = new TungstenFabricRuleResponse(networkPolicy.getUuid(), policyRuleType);
                }
            }

            return tungstenRuleResponse;
        }

        return null;
    }

    @Override
    public List<TungstenFabricPolicyResponse> listTungstenPolicy(final long zoneId, final Long networkId,
        final Long addressId, final String policyUuid) {
        String projectFqn = getTungstenProjectFqn(null);
        String networkUuid = getNetworkUuid(networkId);
        String policyName = addressId != null ? TungstenUtils.getPublicNetworkPolicyName(addressId) : null;
        TungstenCommand tungstenCommand = new ListTungstenPolicyCommand(projectFqn, networkUuid, policyName,
            policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricPolicyResponse> tungstenPolicyResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<NetworkPolicy> networkPolicyList = (List<NetworkPolicy>) tungstenAnswer.getApiObjectBaseList();
            for (NetworkPolicy networkPolicy : networkPolicyList) {
                tungstenPolicyResponseList.add(new TungstenFabricPolicyResponse(networkPolicy));
            }
        }
        return tungstenPolicyResponseList;
    }

    @Override
    public List<TungstenFabricNetworkResponse> listTungstenNetwork(final long zoneId, final String networkUuid) {
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenNetworkCommand(projectFqn, networkUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricNetworkResponse> tungstenNetworkResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<VirtualNetwork> networkList = (List<VirtualNetwork>) tungstenAnswer.getApiObjectBaseList();
            for (VirtualNetwork network : networkList) {
                tungstenNetworkResponseList.add(new TungstenFabricNetworkResponse(network));
            }
        }
        return tungstenNetworkResponseList;
    }

    @Override
    public List<TungstenFabricNicResponse> listTungstenNic(final long zoneId, final String nicUuid) {
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenNicCommand(projectFqn, nicUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricNicResponse> tungstenNicResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<VirtualMachineInterface> nicList =
                (List<VirtualMachineInterface>) tungstenAnswer.getApiObjectBaseList();
            for (VirtualMachineInterface nic : nicList) {
                tungstenNicResponseList.add(new TungstenFabricNicResponse(nic));
            }
        }
        return tungstenNicResponseList;
    }

    @Override
    public List<TungstenFabricVmResponse> listTungstenVm(final long zoneId, final String vmUuid) {
        String projectFqn = getTungstenProjectFqn(null);
        TungstenCommand tungstenCommand = new ListTungstenVmCommand(projectFqn, vmUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricVmResponse> tungstenVmResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<VirtualMachine> vmList = (List<VirtualMachine>) tungstenAnswer.getApiObjectBaseList();
            for (VirtualMachine vm : vmList) {
                tungstenVmResponseList.add(new TungstenFabricVmResponse(vm));
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
    public List<TungstenFabricRuleResponse> listTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid) {
        TungstenCommand tungstenCommand = new ListTungstenPolicyRuleCommand(policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricRuleResponse> tungstenRuleResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            NetworkPolicy networkPolicy = (NetworkPolicy) tungstenAnswer.getApiObjectBase();
            PolicyEntriesType policyEntriesType = networkPolicy.getEntries();
            if (policyEntriesType != null) {
                List<PolicyRuleType> policyRuleTypeList = policyEntriesType.getPolicyRule();
                if (policyRuleTypeList != null) {
                    for (PolicyRuleType policyRuleType : policyRuleTypeList) {
                        if (ruleUuid == null) {
                            tungstenRuleResponseList.add(
                                new TungstenFabricRuleResponse(networkPolicy.getUuid(), policyRuleType));
                        } else {
                            if (policyRuleType.getRuleUuid().equals(ruleUuid)) {
                                tungstenRuleResponseList.add(
                                    new TungstenFabricRuleResponse(networkPolicy.getUuid(), policyRuleType));
                            }
                        }
                    }
                }
            }
        }
        return tungstenRuleResponseList;
    }

    @Override
    public TungstenFabricPolicyResponse removeTungstenPolicyRule(final long zoneId, final String policyUuid,
        final String ruleUuid) {
        TungstenCommand tungstenCommand = new RemoveTungstenPolicyRuleCommand(policyUuid, ruleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((NetworkPolicy) tungstenAnswer.getApiObjectBase());
        }
        return null;
    }

    @Override
    public TungstenFabricTagResponse createTungstenTag(final long zoneId, final String tagType, final String tagValue) {
        TungstenCommand tungstenCommand = new CreateTungstenTagCommand(tagType, tagValue);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            Tag tag = (Tag) tungstenAnswer.getApiObjectBase();
            TungstenFabricTagResponse tungstenTagResponse = new TungstenFabricTagResponse(tag);
            return tungstenTagResponse;
        }
        return null;
    }

    @Override
    public TungstenFabricTagTypeResponse createTungstenTagType(final long zoneId, final String name) {
        TungstenCommand tungstenCommand = new CreateTungstenTagTypeCommand(name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            TagType tagtype = (TagType) tungstenAnswer.getApiObjectBase();
            TungstenFabricTagTypeResponse tungstenTagTypeResponse = new TungstenFabricTagTypeResponse(tagtype);
            return tungstenTagTypeResponse;
        }
        return null;
    }

    @Override
    public List<TungstenFabricTagResponse> listTungstenTags(final long zoneId, final String networkUuid,
        final String vmUuid, final String nicUuid, final String policyUuid, final String tagUuid) {
        TungstenCommand tungstenCommand = new ListTungstenTagCommand(networkUuid, vmUuid, nicUuid, policyUuid, tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricTagResponse> tungstenTagResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<Tag> tagList = (List<Tag>) tungstenAnswer.getApiObjectBaseList();
            for (Tag tag : tagList) {
                tungstenTagResponseList.add(new TungstenFabricTagResponse(tag));
            }
        }
        return tungstenTagResponseList;
    }

    @Override
    public List<TungstenFabricTagTypeResponse> listTungstenTagTypes(final long zoneId, final String tagTypeUuid) {
        TungstenCommand tungstenCommand = new ListTungstenTagTypeCommand(tagTypeUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricTagTypeResponse> tungstenTagTypeResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<TagType> tagTypeList = (List<TagType>) tungstenAnswer.getApiObjectBaseList();
            for (TagType tagType : tagTypeList) {
                tungstenTagTypeResponseList.add(new TungstenFabricTagTypeResponse(tagType));
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
        TungstenCommand tungstenCommand = new ApplyTungstenNetworkPolicyCommand(networkUuid, policyUuid, majorSequence,
            minorSequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((NetworkPolicy) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricTagResponse applyTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String tagUuid) {
        TungstenCommand tungstenCommand = new ApplyTungstenTagCommand(networkUuids, vmUuids, nicUuids, policyUuid,
            tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricTagResponse((Tag) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricPolicyResponse removeTungstenPolicy(final long zoneId, final String networkUuid,
        final String policyUuid) {
        TungstenCommand tungstenCommand = new RemoveTungstenPolicyCommand(networkUuid, policyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricPolicyResponse((NetworkPolicy) tungstenAnswer.getApiObjectBase());
        }
        return null;
    }

    @Override
    public TungstenFabricTagResponse removeTungstenTag(final long zoneId, final List<String> networkUuids,
        final List<String> vmUuids, final List<String> nicUuids, final String policyUuid, final String tagUuid) {
        TungstenCommand tungstenCommand = new RemoveTungstenTagCommand(networkUuids, vmUuids, nicUuids, policyUuid,
            tagUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricTagResponse((Tag) tungstenAnswer.getApiObjectBase());
        }
        return null;
    }

    @Override
    public TungstenFabricAddressGroupResponse createTungstenAddressGroup(final long zoneId, final String name,
        final String ipPrefix, final int ipPrefixLen) {
        TungstenCommand tungstenCommand = new CreateTungstenAddressGroupCommand(name, ipPrefix, ipPrefixLen);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricAddressGroupResponse((AddressGroup) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricServiceGroupResponse createTungstenServiceGroup(final long zoneId, final String name,
        final String protocol, final int startPort, final int endPort) {
        TungstenCommand tungstenCommand = new CreateTungstenServiceGroupCommand(name, protocol, startPort, endPort);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricServiceGroupResponse((ServiceGroup) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallRuleResponse createTungstenFirewallRule(final long zoneId, final String name,
        final String action, final String serviceGroupUuid, final String srcTagUuid, final String srcAddressGroupUuid,
        final String direction, final String destTagUuid, final String destAddressGroupUuid, final String tagTypeUuid) {
        TungstenCommand tungstenCommand = new CreateTungstenFirewallRuleCommand(name, action, serviceGroupUuid,
            srcTagUuid, srcAddressGroupUuid, direction, destTagUuid, destAddressGroupUuid, tagTypeUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallRuleResponse(
                (net.juniper.tungsten.api.types.FirewallRule) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallPolicyResponse createTungstenFirewallPolicy(final long zoneId, final String name) {
        TungstenCommand tungstenCommand = new CreateTungstenFirewallPolicyCommand(name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallPolicyResponse((FirewallPolicy) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricApplicationPolicySetResponse createTungstenApplicationPolicySet(final long zoneId,
        final String name) {
        TungstenCommand tungstenCommand = new CreateTungstenApplicationPolicySetCommand(name);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricApplicationPolicySetResponse(
                (ApplicationPolicySet) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricApplicationPolicySetResponse addTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid, final String tagUuid,
        final int sequence) {
        TungstenCommand tungstenCommand = new AddTungstenFirewallPolicyCommand(applicationPolicySetUuid,
            firewallPolicyUuid, tagUuid, sequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricApplicationPolicySetResponse(
                (ApplicationPolicySet) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallPolicyResponse addTungstenFirewallRule(final long zoneId,
        final String firewallPolicyUuid, final String firewallRuleUuid, final int sequence) {
        TungstenCommand tungstenCommand = new AddTungstenFirewallRuleCommand(firewallPolicyUuid, firewallRuleUuid,
            sequence);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallPolicyResponse((FirewallPolicy) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public List<TungstenFabricApplicationPolicySetResponse> listTungstenApplicationPolicySet(final long zoneId,
        final String applicationPolicySetUuid) {
        TungstenCommand tungstenCommand = new ListTungstenApplicationPolicySetCommand(applicationPolicySetUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricApplicationPolicySetResponse> tungstenApplicationPolicySetResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ApplicationPolicySet> applicationPolicySetList =
                (List<ApplicationPolicySet>) tungstenAnswer.getApiObjectBaseList();
            for (ApplicationPolicySet applicationPolicySet : applicationPolicySetList) {
                tungstenApplicationPolicySetResponseList.add(
                    new TungstenFabricApplicationPolicySetResponse(applicationPolicySet));
            }
        }
        return tungstenApplicationPolicySetResponseList;
    }

    @Override
    public List<TungstenFabricFirewallPolicyResponse> listTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid) {
        TungstenCommand tungstenCommand = new ListTungstenFirewallPolicyCommand(applicationPolicySetUuid,
            firewallPolicyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricFirewallPolicyResponse> tungstenFirewallPolicyResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<FirewallPolicy> firewallPolicyList = (List<FirewallPolicy>) tungstenAnswer.getApiObjectBaseList();
            for (FirewallPolicy firewallPolicy : firewallPolicyList) {
                tungstenFirewallPolicyResponseList.add(new TungstenFabricFirewallPolicyResponse(firewallPolicy));
            }
        }
        return tungstenFirewallPolicyResponseList;
    }

    @Override
    public List<TungstenFabricFirewallRuleResponse> listTungstenFirewallRule(final long zoneId,
        final String firewallPolicyUuid, final String firewallRuleUuid) {
        TungstenCommand tungstenCommand = new ListTungstenFirewallRuleCommand(firewallPolicyUuid, firewallRuleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricFirewallRuleResponse> tungstenFirewallRuleResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<net.juniper.tungsten.api.types.FirewallRule> firewallRuleList =
                (List<net.juniper.tungsten.api.types.FirewallRule>) tungstenAnswer
                .getApiObjectBaseList();
            for (net.juniper.tungsten.api.types.FirewallRule firewallRule : firewallRuleList) {
                tungstenFirewallRuleResponseList.add(new TungstenFabricFirewallRuleResponse(firewallRule));
            }
        }
        return tungstenFirewallRuleResponseList;
    }

    @Override
    public List<TungstenFabricServiceGroupResponse> listTungstenServiceGroup(final long zoneId,
        final String serviceGroupUuid) {
        TungstenCommand tungstenCommand = new ListTungstenServiceGroupCommand(serviceGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricServiceGroupResponse> tungstenServiceGroupResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<ServiceGroup> serviceGroupList = (List<ServiceGroup>) tungstenAnswer.getApiObjectBaseList();
            for (ServiceGroup serviceGroup : serviceGroupList) {
                tungstenServiceGroupResponseList.add(new TungstenFabricServiceGroupResponse(serviceGroup));
            }
        }
        return tungstenServiceGroupResponseList;
    }

    @Override
    public List<TungstenFabricAddressGroupResponse> listTungstenAddressGroup(final long zoneId,
        final String addressGroupUuid) {
        TungstenCommand tungstenCommand = new ListTungstenAddressGroupCommand(addressGroupUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        List<TungstenFabricAddressGroupResponse> tungstenAddressGroupResponseList = new ArrayList<>();
        if (tungstenAnswer.getResult()) {
            List<AddressGroup> addressGroupList = (List<AddressGroup>) tungstenAnswer.getApiObjectBaseList();
            for (AddressGroup addressGroup : addressGroupList) {
                tungstenAddressGroupResponseList.add(new TungstenFabricAddressGroupResponse(addressGroup));
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
    public TungstenFabricApplicationPolicySetResponse removeTungstenFirewallPolicy(final long zoneId,
        final String applicationPolicySetUuid, final String firewallPolicyUuid) {
        TungstenCommand tungstenCommand = new RemoveTungstenFirewallPolicyCommand(applicationPolicySetUuid,
            firewallPolicyUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricApplicationPolicySetResponse(
                (ApplicationPolicySet) tungstenAnswer.getApiObjectBase());
        }

        return null;
    }

    @Override
    public TungstenFabricFirewallPolicyResponse removeTungstenFirewallRule(final long zoneId,
        final String firewallPolicyUuid, final String firewallRuleUuid) {
        TungstenCommand tungstenCommand = new RemoveTungstenFirewallRuleCommand(firewallPolicyUuid, firewallRuleUuid);
        TungstenAnswer tungstenAnswer = tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
        if (tungstenAnswer.getResult()) {
            return new TungstenFabricFirewallPolicyResponse((FirewallPolicy) tungstenAnswer.getApiObjectBase());
        }

        return null;
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

    private String getNicUuid(Long nicId) {
        if (nicId == null) {
            return null;
        }

        Nic nic = nicDao.findById(nicId);

        if (nic != null) {
            return nic.getUuid();
        }

        return null;
    }
}
