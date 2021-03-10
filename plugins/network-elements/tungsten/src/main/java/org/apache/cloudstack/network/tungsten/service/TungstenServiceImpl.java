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

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.HostPodVO;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.ManagerBase;
import net.juniper.tungsten.api.types.FloatingIp;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
    private IPAddressDao _ipAddressDao;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private TungstenProviderDao _tungstenProviderDao;
    @Inject
    private TungstenFabricUtils _tungstenFabricUtils;

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";

    @Override
    public boolean start() {
        synchronizeTungstenData();
        subscribeTungstenEvent();
        return super.start();
    }

    private void synchronizeTungstenData() {
        List<TungstenProviderVO> tungstenProviderList = _tungstenProviderDao.findAll();
        for (TungstenProviderVO tungstenProviderVO : tungstenProviderList) {
            long zoneId = tungstenProviderVO.getZoneId();
            Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
                Networks.TrafficType.Public);
            List<IPAddressVO> ipAddressList = _ipAddressDao.listByDcId(zoneId);
            for (IpAddress ipAddressVO : ipAddressList) {
                if (!ipAddressVO.isSourceNat() && ipAddressVO.getState() == IpAddress.State.Allocated) {
                    createTungstenFloatingIp(zoneId, ipAddressVO);
                }
            }
            deleteTungstenListFloatingIp(zoneId, publicNetwork);
        }

        syncTungstenDbWithCloudstackProjectsAndDomains();
    }

    private boolean deleteTungstenListFloatingIp(long zoneId, Network publicNetwork) {
        boolean result = true;
        GetTungstenFloatingIpsCommand getTungstenFloatingIpsCommand = new GetTungstenFloatingIpsCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer getFloatingIpsTungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(
            getTungstenFloatingIpsCommand, zoneId);
        List<FloatingIp> floatingIpList = (List<FloatingIp>) getFloatingIpsTungstenAnswer.getApiObjectBaseList();
        for (FloatingIp floatingIp : floatingIpList) {
            IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(zoneId, floatingIp.getAddress());
            if (!ipAddressVO.isSourceNat() && ipAddressVO.getState() == IpAddress.State.Free) {
                DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(
                    publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
                TungstenAnswer deleteFloatingIpTungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    deleteTungstenFloatingIpCommand, zoneId);
                result = result && deleteFloatingIpTungstenAnswer.getResult();
            }
        }
        return result;
    }

    private void subscribeTungstenEvent() {
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

        _messageBus.subscribe(ConfigurationManager.MESSAGE_CREATE_POD_IP_RANGE_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(final String senderAddress, final String subject, final Object args) {
                try {
                    final HostPodVO pod = (HostPodVO) args;
                    createManagementNetwork(pod);
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
                    deleteManagementNetwork(pod);
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

        _messageBus.subscribe(TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                try {
                    syncTungstenDbWithCloudstackProjectsAndDomains();
                } catch (final Exception e) {
                    s_logger.error(e.getMessage());
                }
            }
        });
    }

    private boolean createTungstenFloatingIp(long zoneId, IpAddress ipAddress) {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
//        String projectUuid = getProject(ipAddress.getAccountId());
        Network network = _networkDao.findById(ipAddress.getNetworkId());
        String projectFqn = getTungstenProjectFqn(network);
        CreateTungstenFloatingIpCommand createTungstenFloatingIpPoolCommand = new CreateTungstenFloatingIpCommand(
                projectFqn, publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId),
            TungstenUtils.getFloatingIpName(ipAddress.getId()), ipAddress.getAddress().addr());
        TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(createTungstenFloatingIpPoolCommand,
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

        if(network == null){
            return TungstenApi.TUNGSTEN_DEFAULT_DOMAIN + ":" + TungstenApi.TUNGSTEN_DEFAULT_PROJECT;
        }

        String networkProjectUuid = getProject(network.getAccountId());
        Project project = _projectDao.findByUuid(networkProjectUuid);
        Domain domain = _domainDao.findById(network.getDomainId());

        StringBuilder sb = new StringBuilder();
        if(domain != null && domain.getName() != null && domain.getId() != Domain.ROOT_DOMAIN) {
            sb.append(domain.getName());
        } else {
            sb.append(TungstenApi.TUNGSTEN_DEFAULT_DOMAIN);
        }

        sb.append(":");

        if(project != null && project.getName() != null) {
            sb.append(project.getName());
        } else {
            sb.append(TungstenApi.TUNGSTEN_DEFAULT_PROJECT);
        }
        return sb.toString();
    }

    @Override
    public boolean createManagementNetwork(HostPodVO pod) {
        TungstenProvider tungstenProvider = _tungstenProviderDao.findByZoneId(pod.getDataCenterId());
        if (tungstenProvider != null) {
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
                CreateTungstenNetworkCommand createTungstenNetworkCommand = new CreateTungstenNetworkCommand(
                    managementNetwork.getUuid(), TungstenUtils.getManagementNetworkName(pod.getDataCenterId()), null,
                    false, false, pod.getCidrAddress(), pod.getCidrSize(), pod.getGateway(), true, null, startIp, endIp,
                    true, true);
                TungstenAnswer createTungstenNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    createTungstenNetworkCommand, pod.getDataCenterId());

                if (!createTungstenNetworkAnswer.getResult()) {
                    return false;
                }

                // consider policy to protect fabric network
                List<TungstenRule> tungstenRuleList = new ArrayList<>();
                tungstenRuleList.add(new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION,
                    TungstenUtils.ANY_PROTO, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ALL_IP4_PREFIX, 0,
                    -1, -1));

                // create default public network policy rule
                CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand =
                    new CreateTungstenNetworkPolicyCommand(
                    TungstenUtils.getVirtualNetworkPolicyName(managementNetwork.getId()), null, tungstenRuleList);
                TungstenAnswer createTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    createTungstenNetworkPolicyCommand, pod.getDataCenterId());
                if (!createTungstenNetworkPolicyAnswer.getResult()) {
                    return false;
                }

                // apply management network policy
                ApplyTungstenNetworkPolicyCommand applyTungstenManagementNetworkPolicyCommand =
                    new ApplyTungstenNetworkPolicyCommand(
                    null, TungstenUtils.getVirtualNetworkPolicyName(managementNetwork.getId()),
                    managementNetwork.getUuid(), false);
                TungstenAnswer applyNetworkManagementPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    applyTungstenManagementNetworkPolicyCommand, pod.getDataCenterId());
                if (!applyNetworkManagementPolicyAnswer.getResult()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean deleteManagementNetwork(HostPodVO pod) {
        TungstenProvider tungstenProvider = _tungstenProviderDao.findByZoneId(pod.getDataCenterId());
        if (tungstenProvider != null) {
            Network managementNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(pod.getDataCenterId(),
                Networks.TrafficType.Management);

            DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getVirtualNetworkPolicyName(managementNetwork.getId()), null,
                managementNetwork.getUuid());
            TungstenAnswer deleteTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                deleteTungstenNetworkPolicyCommand, pod.getDataCenterId());
            if (!deleteTungstenNetworkPolicyAnswer.getResult()) {
                return false;
            }

            DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
                managementNetwork.getUuid());
            TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand,
                pod.getDataCenterId());
            if (!tungstenAnswer.getResult()) {
                return false;
            }
        }
        return true;
    }

    public boolean createTungstenDomain(DomainVO domain) {
        if(domain != null && domain.getId() != Domain.ROOT_DOMAIN) {
            List<TungstenProviderVO> tungstenProviders = _tungstenProviderDao.findAll();
            for (TungstenProviderVO tungstenProvider : tungstenProviders) {
                CreateTungstenDomainCommand createTungstenDomainCommand =
                        new CreateTungstenDomainCommand(domain.getName(), domain.getUuid());
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
            List<TungstenProviderVO> tungstenProviders = _tungstenProviderDao.findAll();
            for (TungstenProviderVO tungstenProvider : tungstenProviders) {
                DeleteTungstenDomainCommand deleteTungstenDomainCommand =
                        new DeleteTungstenDomainCommand(domain.getUuid());
                TungstenAnswer tungstenAnswer = _tungstenFabricUtils.sendTungstenCommand(deleteTungstenDomainCommand,
                        tungstenProvider.getZoneId());
                if (!tungstenAnswer.getResult()) {
                    return false;
                }
            }
        return true;
    }

    public boolean createTungstenProject(Project project) {
        List<TungstenProviderVO> tungstenProviders = _tungstenProviderDao.findAll();
        String domainName;
        String domainUuid;
        Domain domain = _domainDao.findById(project.getDomainId());
        //Check if the domain is the root domain
        //if the domain is root domain we will use the default domain from tungsten by setting
        //both domainName and domainUuid to null
        if(domain != null && domain.getId() != Domain.ROOT_DOMAIN){
            domainName = domain.getName();
            domainUuid = domain.getUuid();
        } else {
            domainName = null;
            domainUuid = null;
        }
        if(domain != null) {
            for (TungstenProviderVO tungstenProvider : tungstenProviders) {
                CreateTungstenProjectCommand createTungstenProjectCommand =
                        new CreateTungstenProjectCommand(project.getName(), project.getUuid(), domainUuid, domainName);
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
        List<TungstenProviderVO> tungstenProviders = _tungstenProviderDao.findAll();
        for (TungstenProviderVO tungstenProvider : tungstenProviders) {
            DeleteTungstenProjectCommand deleteTungstenProjectCommand =
                    new DeleteTungstenProjectCommand(project.getUuid());
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

        if (cloudstackDomains != null ) {
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
}
