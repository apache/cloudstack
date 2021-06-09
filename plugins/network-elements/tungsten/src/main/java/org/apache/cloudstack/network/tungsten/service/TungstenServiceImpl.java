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
import com.cloud.vm.dao.UserVmDao;
import net.juniper.tungsten.api.types.FloatingIp;
import net.sf.cglib.proxy.Enhancer;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenObjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerSslCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerStatsCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenFloatingIP;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TungstenServiceImpl extends ManagerBase implements TungstenService {
    private static final Logger s_logger = Logger.getLogger(TungstenServiceImpl.class);
    @Inject
    private MessageBus _messageBus;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private LoadBalancerCertMapDao _lbCertMapDao;
    @Inject
    private FirewallRulesDao _fwRulesDao;
    @Inject
    private TungstenGuestNetworkIpAddressDao _tungstenGuestNetworkIpAddressDao;
    @Inject
    private TungstenProviderDao _tungstenProviderDao;
    @Inject
    private TungstenFabricUtils _tungstenFabricUtils;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    private SecurityGroupDao _securityGroupDao;
    @Inject
    private UserVmDao _userVmDao;

    @Override
    public boolean start() {
        subscribeTungstenEvent();
        return true;
    }

    @Override
    public boolean synchronizeTungstenData(Long tungstenProviderId) {
        boolean result = true;
        TungstenProviderVO tungstenProviderVO = _tungstenProviderDao.findById(tungstenProviderId);
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
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        List<IPAddressVO> ipAddressList = _ipAddressDao.listByDcIdAndAssociatedNetwork(zoneId);
        TungstenCommand getTungstenFloatingIpsCommand = new GetTungstenFloatingIpsCommand(publicNetwork.getUuid(),
            TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(getTungstenFloatingIpsCommand, zoneId);
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
        _messageBus.subscribe(IpAddressManager.MESSAGE_ASSIGN_IPADDR_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final IpAddress ipAddress = (IpAddress) args;
                    long zoneId = ipAddress.getDataCenterId();
                    TungstenProvider tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
                    if (!ipAddress.isSourceNat() && tungstenProvider != null) {
                        createTungstenFloatingIp(zoneId, ipAddress);
                    }
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        _messageBus.subscribe(IpAddressManager.MESSAGE_RELEASE_IPADDR_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final IpAddress ipAddress = (IpAddress) args;
                    if (!ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                        long zoneId = ipAddress.getDataCenterId();
                        TungstenProvider tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
                        if (tungstenProvider != null) {
                            deleteTungstenFloatingIp(zoneId, ipAddress);
                        }
                    }
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        _messageBus.subscribe(TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final Network network = (Network) args;
                    List<IPAddressVO> ipAddressVOList = _ipAddressDao.listByAccount(Account.ACCOUNT_ID_SYSTEM);
                    for (IPAddressVO ipAddressVO : ipAddressVOList) {
                        ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand =
                            new ApplyTungstenNetworkPolicyCommand(
                            null, TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), network.getUuid(),
                            true);
                        _tungstenFabricUtils.sendTungstenCommand(applyTungstenNetworkPolicyCommand,
                            network.getDataCenterId());
                    }
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });

        _messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_VLAN_IP_RANGE_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_VLAN_IP_RANGE_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_POD_IP_RANGE_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(ConfigurationManager.MESSAGE_DELETE_POD_IP_RANGE_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(DomainManager.MESSAGE_CREATE_TUNGSTEN_DOMAIN_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(DomainManager.MESSAGE_DELETE_TUNGSTEN_DOMAIN_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(ProjectManager.MESSAGE_CREATE_TUNGSTEN_PROJECT_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(ProjectManager.MESSAGE_DELETE_TUNGSTEN_PROJECT_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT,
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

        _messageBus.subscribe(SecurityGroupService.MESSAGE_CREATE_TUNGSTEN_SECURITY_GROUP_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(SecurityGroupService.MESSAGE_DELETE_TUNGSTEN_SECURITY_GROUP_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(SecurityGroupService.MESSAGE_ADD_SECURITY_GROUP_RULE_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(SecurityGroupService.MESSAGE_ADD_VM_TO_SECURITY_GROUPS_EVENT, new MessageSubscriber() {
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

        _messageBus.subscribe(SecurityGroupService.MESSAGE_REMOVE_SECURITY_GROUP_RULE_EVENT, new MessageSubscriber() {
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
        List<TungstenProviderVO> tungstenProviders = _tungstenProviderDao.findAll();
        if (tungstenProviders != null) {
            return tungstenProviders;
        } else {
            return new ArrayList<>();
        }
    }

    private boolean createTungstenFloatingIp(long zoneId, IpAddress ipAddress) {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        Network network = _networkDao.findById(ipAddress.getNetworkId());
        String projectFqn = getTungstenProjectFqn(network);
        CreateTungstenFloatingIpCommand createTungstenFloatingIpCommand = new CreateTungstenFloatingIpCommand(
            projectFqn, publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId),
            TungstenUtils.getFloatingIpName(ipAddress.getId()), ipAddress.getAddress().addr());
        TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(createTungstenFloatingIpCommand,
            zoneId);
        return tungstenAnswer.getResult();
    }

    private void deleteTungstenFloatingIp(long zoneId, IpAddress ipAddress) {
        List<NetworkVO> publicNetworkVOList = _networkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        NetworkVO publicNetwork = publicNetworkVOList.get(0);
        DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpPoolCommand = new DeleteTungstenFloatingIpCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId),
            TungstenUtils.getFloatingIpName(ipAddress.getId()));
        _tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpPoolCommand, zoneId);
    }

    private void deleteTungstenFloatingIp(long zoneId, FloatingIp floatingIp) {
        TungstenCommand tungstenCommand = new DeleteTungstenObjectCommand(floatingIp);
        _tungstenFabricUtils.sendTungstenCommand(tungstenCommand, zoneId);
    }

    @Override
    public String getProject(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            ProjectVO projectVO = _projectDao.findByProjectAccountId(account.getId());
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
        Project project = _projectDao.findByUuid(projectUuid);
        Domain domain = _domainDao.findById(domainUuid);

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
    public boolean createManagementNetwork(long zoneId) {
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
            Networks.TrafficType.Management);

        CreateTungstenNetworkCommand createTungstenNetworkCommand = new CreateTungstenNetworkCommand(
            managementNetwork.getUuid(), TungstenUtils.getManagementNetworkName(zoneId),
            TungstenUtils.getManagementNetworkName(zoneId), null, false, false, null, 0, null, true, null, null, null,
            true, true, null);
        TungstenAnswer createTungstenNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenNetworkCommand, zoneId);

        return createTungstenNetworkAnswer.getResult();
    }

    @Override
    public boolean addManagementNetworkSubnet(HostPodVO pod) {
        final String[] podIpRanges = pod.getDescription().split(",");
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
            Networks.TrafficType.Management);

        // tungsten system don't support delete a part of allocation pool in subnet
        // we only create first pod ip range in a pod (the same with public network)
        // in UI : don't permit to add more than 1 pod ip range if tungsten zone
        // consider to permit add more pod ip range if it is not overlap subnet
        if (podIpRanges.length == 1) {
            final String[] ipRange = podIpRanges[0].split("-");
            String startIp = ipRange[0];
            String endIp = ipRange[1];

            AddTungstenNetworkSubnetCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(
                managementNetwork.getUuid(), pod.getCidrAddress(), pod.getCidrSize(), null, true, "0.0.0.0", startIp,
                endIp, true, pod.getUuid());
            TungstenAnswer createTungstenNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
                addTungstenNetworkSubnetCommand, pod.getDataCenterId());

            if (!createTungstenNetworkAnswer.getResult()) {
                return false;
            }

            List<TungstenRule> tungstenRuleList = new ArrayList<>();
            tungstenRuleList.add(new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION,
                TungstenUtils.ANY_PROTO, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ALL_IP4_PREFIX, 0, -1,
                -1));

            // create default management network policy rule
            CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand =
                new CreateTungstenNetworkPolicyCommand(
                TungstenUtils.getVirtualNetworkPolicyName(pod.getId()), null, tungstenRuleList);
            TungstenAnswer createTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                createTungstenNetworkPolicyCommand, pod.getDataCenterId());
            if (!createTungstenNetworkPolicyAnswer.getResult()) {
                return false;
            }

            // apply management network policy
            ApplyTungstenNetworkPolicyCommand applyTungstenManagementNetworkPolicyCommand =
                new ApplyTungstenNetworkPolicyCommand(
                null, TungstenUtils.getVirtualNetworkPolicyName(pod.getId()), managementNetwork.getUuid(), false);
            TungstenAnswer applyNetworkManagementPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                applyTungstenManagementNetworkPolicyCommand, pod.getDataCenterId());
            return applyNetworkManagementPolicyAnswer.getResult();
        }

        return false;
    }

    @Override
    public boolean deleteManagementNetwork(long zoneId) {
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
            Networks.TrafficType.Management);

        DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
            managementNetwork.getUuid());
        TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, zoneId);
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean removeManagementNetworkSubnet(HostPodVO pod) {
        Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
            Networks.TrafficType.Management);

        DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getVirtualNetworkPolicyName(pod.getId()), null, managementNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenNetworkPolicyCommand, pod.getDataCenterId());
        if (!deleteTungstenNetworkPolicyAnswer.getResult()) {
            return false;
        }

        RemoveTungstenNetworkSubnetCommand removeTungstenNetworkSubnetCommand = new RemoveTungstenNetworkSubnetCommand(
            managementNetwork.getUuid(), pod.getUuid());

        TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(removeTungstenNetworkSubnetCommand,
            pod.getDataCenterId());
        return tungstenAnswer.getResult();
    }

    @Override
    public boolean createPublicNetwork(long zoneId) {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public network
        CreateTungstenNetworkCommand createTungstenPublicNetworkCommand = new CreateTungstenNetworkCommand(
            publicNetwork.getUuid(), TungstenUtils.getPublicNetworkName(zoneId),
            TungstenUtils.getPublicNetworkName(zoneId), null, true, false, null, 0, null, true, "0.0.0.0", null, null,
            false, false, null);
        TungstenAnswer createPublicNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenPublicNetworkCommand, zoneId);
        if (!createPublicNetworkAnswer.getResult()) {
            return false;
        }

        // change default tungsten security group
        // change default forwarding mode

        // consider policy to protect fabric network
        List<TungstenRule> fabricRuleList = new ArrayList<>();
        fabricRuleList.add(
            new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY_PROTO,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1));

        GetTungstenFabricNetworkCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
        TungstenAnswer getTungstenFabricNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            getTungstenFabricNetworkCommand, zoneId);
        if (!getTungstenFabricNetworkAnswer.getResult()) {
            return false;
        }

        // create default fabric network policy rule
        CreateTungstenNetworkPolicyCommand createFabricNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
            TungstenUtils.getFabricNetworkPolicyName(), null, fabricRuleList);
        TungstenAnswer createfabricNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createFabricNetworkPolicyCommand, zoneId);
        if (!createfabricNetworkPolicyAnswer.getResult()) {
            return false;
        }

        // apply fabric network policy
        ApplyTungstenNetworkPolicyCommand applyTungstenFabricNetworkPolicyCommand =
            new ApplyTungstenNetworkPolicyCommand(
            null, TungstenUtils.getFabricNetworkPolicyName(),
            getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid(), false);
        TungstenAnswer applyNetworkFabricNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            applyTungstenFabricNetworkPolicyCommand, zoneId);
        if (!applyNetworkFabricNetworkPolicyAnswer.getResult()) {
            return false;
        }

        // create floating ip pool
        CreateTungstenFloatingIpPoolCommand createTungstenFloatingIpPoolCommand =
            new CreateTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer createFloatingIpPoolAnswer = _tungstenFabricUtils.sendTungstenCommand(
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
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public ip address
        String[] ipAddress = pubVlanVO.getIpRange().split("-");
        String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
            pubVlanVO.getVlanNetmask());
        Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);

        AddTungstenNetworkSubnetCommand addTungstenNetworkSubnetCommand = new AddTungstenNetworkSubnetCommand(
            publicNetwork.getUuid(), publicPair.first(), publicPair.second(), pubVlanVO.getVlanGateway(), true, null,
            ipAddress[0], ipAddress[1], false, pubVlanVO.getUuid());
        TungstenAnswer addTungstenNetworkSubnetAnswer = _tungstenFabricUtils.sendTungstenCommand(
            addTungstenNetworkSubnetCommand, zoneId);
        if (!addTungstenNetworkSubnetAnswer.getResult()) {
            return false;
        }

        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(
            new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY_PROTO,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, publicPair.first(), publicPair.second(), -1, -1));

        // create default public network policy rule
        CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
            TungstenUtils.getDefaultPublicNetworkPolicyName(pubVlanVO.getId()), null, tungstenRuleList);
        TungstenAnswer createTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenNetworkPolicyCommand, zoneId);
        if (!createTungstenNetworkPolicyAnswer.getResult()) {
            return false;
        }

        // apply default network policy
        ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand = new ApplyTungstenNetworkPolicyCommand(
            null, TungstenUtils.getDefaultPublicNetworkPolicyName(pubVlanVO.getId()), publicNetwork.getUuid(), false);
        TungstenAnswer applyNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            applyTungstenNetworkPolicyCommand, zoneId);
        if (!applyNetworkPolicyAnswer.getResult()) {
            return false;
        }


        return true;
    }

    @Override
    public boolean deletePublicNetwork(long zoneId) {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // delete floating ip pool
        DeleteTungstenFloatingIpPoolCommand deleteTungstenFloatingIpPoolCommand =
            new DeleteTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        _tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpPoolCommand, zoneId);

        // get Tungsten-Fabric network and remove default network policy
        GetTungstenFabricNetworkCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
        TungstenAnswer getTungstenFabricNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            getTungstenFabricNetworkCommand, zoneId);
        if (getTungstenFabricNetworkAnswer.getResult()) {
            DeleteTungstenNetworkPolicyCommand deleteFabricNetworkPolicyCommand =
                new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getFabricNetworkPolicyName(), null,
                getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid());
            _tungstenFabricUtils.sendTungstenCommand(deleteFabricNetworkPolicyCommand, zoneId);
        }

        // delete public network
        DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
            publicNetwork.getUuid());
        _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, zoneId);

        return true;
    }

    @Override
    public boolean removePublicNetworkSubnet(VlanVO vlanVO) {
        long zoneId = vlanVO.getDataCenterId();
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        RemoveTungstenNetworkSubnetCommand removeTungstenNetworkSubnetCommand = new RemoveTungstenNetworkSubnetCommand(
            publicNetwork.getUuid(), vlanVO.getUuid());
        TungstenAnswer removeTungstenNetworkSubnetAnswer = _tungstenFabricUtils.sendTungstenCommand(
            removeTungstenNetworkSubnetCommand, zoneId);
        if (!removeTungstenNetworkSubnetAnswer.getResult()) {
            return false;
        }

        // clear default public network policy
        DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
            TungstenUtils.getDefaultPublicNetworkPolicyName(vlanVO.getId()), null, publicNetwork.getUuid());
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            deleteTungstenNetworkPolicyCommand, zoneId);
        return deleteTungstenNetworkPolicyAnswer.getResult();
    }

    public boolean createTungstenDomain(DomainVO domain) {
        if (domain != null && domain.getId() != Domain.ROOT_DOMAIN) {
            for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
                CreateTungstenDomainCommand createTungstenDomainCommand = new CreateTungstenDomainCommand(
                    domain.getName(), domain.getUuid());
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(createTungstenDomainCommand,
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
            DeleteTungstenDomainCommand deleteTungstenDomainCommand = new DeleteTungstenDomainCommand(domain.getUuid());
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenDomainCommand,
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
        Domain domain = _domainDao.findById(project.getDomainId());
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
                CreateTungstenProjectCommand createTungstenProjectCommand = new CreateTungstenProjectCommand(
                    project.getName(), project.getUuid(), domainUuid, domainName);
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(createTungstenProjectCommand,
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
            DeleteTungstenProjectCommand deleteTungstenProjectCommand = new DeleteTungstenProjectCommand(
                project.getUuid());
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenProjectCommand,
                tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public void syncTungstenDbWithCloudstackProjectsAndDomains() {
        List<DomainVO> cloudstackDomains = _domainDao.listAll();
        List<ProjectVO> cloudstackProjects = _projectDao.listAll();

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
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
            loadBalancingRule.getSourceIp().addr());
        List<HostVO> hostList = _hostDao.listAllHostsByZoneAndHypervisorType(network.getDataCenterId(),
            Hypervisor.HypervisorType.KVM);

        GetTungstenLoadBalancerCommand getTungstenLoadBalancerCommand = new GetTungstenLoadBalancerCommand(
            getTungstenProjectFqn(network), TungstenUtils.getLoadBalancerName(ipAddressVO.getId()));
        TungstenAnswer getTungstenLoadBalancerAnswer = _tungstenFabricUtils.sendTungstenCommand(
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
        UpdateLoadBalancerServiceInstanceCommand updateLoadBalancerServiceInstanceCommand =
            new UpdateLoadBalancerServiceInstanceCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
            TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
        TungstenAnswer updateLoadBalancerServiceInstanceAnswer = _tungstenFabricUtils.sendTungstenCommand(
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
        String lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        if (!lbStatsVisibility.equals("disabled")) {
            String lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
            String lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
            String lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
            UpdateTungstenLoadbalancerStatsCommand updateTungstenLoadbalancerStatsCommand =
                new UpdateTungstenLoadbalancerStatsCommand(
                getTungstenLoadBalancerAnswer.getApiObjectBase().getUuid(), lbStatsPort, lbStatsUri, lbStatsAuth);
            for (HostVO host : hostList) {
                Answer answer = _agentMgr.easySend(host.getId(), updateTungstenLoadbalancerStatsCommand);
                if (answer == null || !answer.getResult()) {
                    return false;
                }
            }
        }

        // update haproxy ssl
        List<FirewallRuleVO> firewallRulesDaoVOList = _fwRulesDao.listByIpAndPurposeAndNotRevoked(ipAddressVO.getId(),
            FirewallRule.Purpose.LoadBalancing);
        for (FirewallRuleVO firewallRuleVO : firewallRulesDaoVOList) {
            LoadBalancerCertMapVO loadBalancerCertMapVO = _lbCertMapDao.findByLbRuleId(firewallRuleVO.getId());
            if (loadBalancerCertMapVO != null) {
                SslCertVO certVO = _entityMgr.findById(SslCertVO.class, loadBalancerCertMapVO.getCertId());
                if (certVO == null) {
                    return false;
                }

                TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO =
                    _tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                    network.getId(), ipAddressVO.getAddress().addr());

                UpdateTungstenLoadbalancerSslCommand updateTungstenLoadbalancerSslCommand =
                    new UpdateTungstenLoadbalancerSslCommand(
                    getTungstenLoadBalancerAnswer.getApiObjectBase().getUuid(), certVO.getName(),
                    certVO.getCertificate(), certVO.getKey(),
                    tungstenGuestNetworkIpAddressVO.getGuestIpAddress().addr(), String.valueOf(NetUtils.HTTPS_PORT));
                for (HostVO host : hostList) {
                    Answer answer = _agentMgr.easySend(host.getId(), updateTungstenLoadbalancerSslCommand);
                    if (answer == null || !answer.getResult()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean createTungstenSecurityGroup(SecurityGroup securityGroup) {
        Project project = _projectDao.findByProjectAccountId(securityGroup.getAccountId());
        String projectFqn;
        if (project != null) {
            projectFqn = buildProjectFqnName(securityGroup.getDomainId(), project.getUuid());
        } else {
            projectFqn = buildProjectFqnName(securityGroup.getDomainId(), null);
        }
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            CreateTungstenSecurityGroupCommand createTungstenSecurityGroupCommand =
                    new CreateTungstenSecurityGroupCommand(securityGroup.getUuid(), securityGroup.getName(),
                            securityGroup.getDescription(), projectFqn);
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(createTungstenSecurityGroupCommand,
                    tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteTungstenSecurityGroup(SecurityGroup securityGroup) {
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            DeleteTungstenSecurityGroupCommand deleteTungstenSecurityGroupCommand =
                    new DeleteTungstenSecurityGroupCommand(securityGroup.getUuid());
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenSecurityGroupCommand,
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
                SecurityGroup securityGroup = _securityGroupDao.findById(securityRule.getSecurityGroupId());
                Pair<String, Integer> pair = NetUtils.getCidr(securityRule.getAllowedSourceIpCidr());
                AddTungstenSecurityGroupRuleCommand addTungstenSecurityGroupRuleCommand =
                        new AddTungstenSecurityGroupRuleCommand(securityGroup.getUuid(),
                                securityRule.getUuid(), securityRule.getType(),
                                securityRule.getStartPort(), securityRule.getEndPort(),
                                securityRule.getAllowedSourceIpCidr(), pair.first(),
                                pair.second(), securityRule.getProtocol());
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(addTungstenSecurityGroupRuleCommand,
                        tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean removeTungstenSecurityGroupRule(SecurityRule securityRule) {
        SecurityGroup securityGroup = _securityGroupDao.findById(securityRule.getSecurityGroupId());
        if (securityGroup == null)
            return false;
        for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
            RemoveTungstenSecurityGroupRuleCommand removeTungstenSecurityGroupRuleCommand = new RemoveTungstenSecurityGroupRuleCommand(securityGroup.getUuid(),
                    securityRule.getUuid(), securityRule.getType());
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(removeTungstenSecurityGroupRuleCommand,
                    tungstenProvider.getZoneId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean addTungstenVmToSecurityGroup(Pair<Long, List<SecurityGroupVO>> pair) {
        long vmId = pair.first();
        List<String> securityGroupUuidList = new ArrayList<>();
        UserVm userVm = _userVmDao.findById(vmId);
        for (SecurityGroupVO securityGroup : pair.second()) {
            securityGroupUuidList.add(securityGroup.getUuid());
        }

        //check if this security group exists in tungsten
        //if not create the security group
        checkTungstenSecurityGroups(pair.second());

        if (userVm != null && !securityGroupUuidList.isEmpty()) {
            for (TungstenProviderVO tungstenProvider : getTungstenProviders()) {
                AddTungstenVmToSecurityGroupCommand addTungstenVmToSecurityGroupCommand =
                        new AddTungstenVmToSecurityGroupCommand(userVm.getUuid(), securityGroupUuidList);
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(addTungstenVmToSecurityGroupCommand,
                        tungstenProvider.getZoneId());
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
                GetTungstenSecurityGroupCommand getTungstenSecurityGroupCommand =
                        new GetTungstenSecurityGroupCommand(securityGroup.getUuid());
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(getTungstenSecurityGroupCommand,
                        tungstenProvider.getZoneId());
                if (tungstenAnswer.getApiObjectBase() == null) {
                    createTungstenSecurityGroup(securityGroup);
                }
            }
        }
    }

}
