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

package com.cloud.network.manager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingNuageVspCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.manager.GetClientDefaultsAnswer;
import com.cloud.agent.api.manager.GetClientDefaultsCommand;
import com.cloud.agent.api.manager.SupportedApiVersionCommand;
import com.cloud.agent.api.sync.SyncDomainAnswer;
import com.cloud.agent.api.sync.SyncDomainCommand;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdAnswer;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.IssueNuageVspResourceRequestCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.commands.UpdateNuageVspDeviceCmd;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.network.sync.NuageVspSync;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.util.NuageVspUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.nuage.vsp.acs.NuageVspPluginClientLoader;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand.SyncType;

public class NuageVspManagerImpl extends ManagerBase implements NuageVspManager, Configurable, StateListener<Status, Status.Event, Host> {

    private static final Logger s_logger = Logger.getLogger(NuageVspManagerImpl.class);

    private static final int ONE_MINUTE_MULTIPLIER = 60 * 1000;

    private static final Set<Network.Provider> NUAGE_VSP_PROVIDERS;
    private static final Map<Network.Service, Set<Network.Provider>> NUAGE_VSP_VPC_SERVICE_MAP;
    private static final ConfigKey[] NUAGE_VSP_CONFIG_KEYS = new ConfigKey<?>[] { NuageVspConfigDns, NuageVspDnsExternal, NuageVspConfigGateway,
            NuageVspSharedNetworkDomainTemplateName, NuageVspVpcDomainTemplateName, NuageVspIsolatedNetworkDomainTemplateName };

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffSvcMapDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcManager _vpcManager;
    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    NuageVspSync _nuageVspSync;
    @Inject
    DataCenterDao _dataCenterDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    AccountManager _accountMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    VpcServiceMapDao _vpcSrvcDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    private DomainDao _domainDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;

    private ScheduledExecutorService scheduler;

    @Inject
    MessageBus _messageBus;

    static {
        NUAGE_VSP_PROVIDERS = ImmutableSet.of(Network.Provider.NuageVsp);
        NUAGE_VSP_VPC_SERVICE_MAP = ImmutableMap.<Network.Service, Set<Network.Provider>>builder()
                .put(Network.Service.Connectivity, NUAGE_VSP_PROVIDERS)
                .put(Network.Service.Dhcp, NUAGE_VSP_PROVIDERS)
                .put(Network.Service.StaticNat, NUAGE_VSP_PROVIDERS)
                .put(Network.Service.SourceNat, NUAGE_VSP_PROVIDERS)
                .put(Network.Service.NetworkACL, NUAGE_VSP_PROVIDERS)
                .build();
    }

    @Override
    public List<Class<?>> getCommands() {
        return Lists.<Class<?>>newArrayList(AddNuageVspDeviceCmd.class, DeleteNuageVspDeviceCmd.class, ListNuageVspDevicesCmd.class,
                IssueNuageVspResourceRequestCmd.class, UpdateNuageVspDeviceCmd.class);
    }

    @Override
    public NuageVspDeviceVO addNuageVspDevice(AddNuageVspDeviceCmd cmd) {
        final NuageVspResource resource = new NuageVspResource();
        final String deviceName = Network.Provider.NuageVsp.getName();
        ExternalNetworkDeviceManager.NetworkDevice networkDevice = ExternalNetworkDeviceManager.NetworkDevice.getNetworkDevice(deviceName);
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(),
                networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: "
                    + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        if (_nuageVspDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A NuageVsp device is already configured on this physical network");
        }

        try {
            NuageVspPluginClientLoader clientLoader = NuageVspPluginClientLoader.getClientLoader(null, null, 1, 1, null);

            Map<String, Object> clientDefaults = clientLoader.getNuageVspManagerClient().getClientDefaults();
            String apiVersion = MoreObjects.firstNonNull(cmd.getApiVersion(), (String) clientDefaults.get("CURRENT_API_VERSION"));
            if (!clientLoader.getNuageVspManagerClient().isSupportedApiVersion(apiVersion)) {
                throw new CloudRuntimeException("Unsupported API version : " + apiVersion);
            }

            int port = cmd.getPort();
            if (0 == port) {
                port = 8443;
            }
            String cmsUserPasswordBase64 = NuageVspUtil.encodePassword(cmd.getPassword());
            String retryCount = String.valueOf(MoreObjects.firstNonNull(cmd.getApiRetryCount(), clientDefaults.get("DEFAULT_API_RETRY_COUNT")));
            String retryInterval = String.valueOf(MoreObjects.firstNonNull(cmd.getApiRetryInterval(), clientDefaults.get("DEFAULT_API_RETRY_INTERVAL")));
            NuageVspResource.Configuration resourceConfiguration = new NuageVspResource.Configuration()
                    .name("Nuage VSD - " + cmd.getHostName())
                    .guid(UUID.randomUUID().toString())
                    .zoneId(String.valueOf(physicalNetwork.getDataCenterId()))
                    .hostName(cmd.getHostName())
                    .cmsUser(cmd.getUserName())
                    .cmsUserPassword(cmsUserPasswordBase64)
                    .port(String.valueOf(port))
                    .apiVersion(apiVersion)
                    .apiRelativePath("/nuage/api/" + apiVersion)
                    .retryCount(retryCount)
                    .retryInterval(retryInterval);

            Map<String, String> hostDetails = resourceConfiguration.build();
            resource.configure(cmd.getHostName(), Maps.<String, Object>newHashMap(hostDetails));
            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, hostDetails);
            if (host == null) {
                throw new CloudRuntimeException("Failed to add Nuage Vsp Device due to internal error.");
            }

            NuageVspDeviceVO nuageVspDevice = new NuageVspDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
            _nuageVspDao.persist(nuageVspDevice);

            DetailVO detail = new DetailVO(host.getId(), "nuagevspdeviceid", String.valueOf(nuageVspDevice.getId()));
            _hostDetailsDao.persist(detail);

            ConfigurationVO cmsIdConfig = _configDao.findByName("nuagevsp.cms.id");
            host = findNuageVspHost(nuageVspDevice.getHostId());
            SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.REGISTER, null);
            SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
            if (answer != null && answer.getSuccess()) {
                registerNewNuageVspDevice(cmsIdConfig, nuageVspDevice.getId() + ":" + answer.getNuageVspCmsId());

                detail = new DetailVO(host.getId(), "nuagevspcmsid", answer.getNuageVspCmsId());
                _hostDetailsDao.persist(detail);

                resourceConfiguration.nuageVspCmsId(answer.getNuageVspCmsId());
                resource.configure(cmd.getHostName(), Maps.<String, Object>newHashMap(resourceConfiguration.build()));

                auditDomainsOnVsp((HostVO) host, true, false);
            } else {
                throw new CloudRuntimeException("Failed to register CMS ID");
            }
            return nuageVspDevice;
        } catch (ConfigurationException e) {
            s_logger.error("Failed to configure Nuage VSD resource " + cmd.getHostName(), e);
            throw new CloudRuntimeException("Failed to configure Nuage VSD resource " + cmd.getHostName(), e);
        } catch (ExecutionException ee) {
            s_logger.error("Failed to add Nuage VSP device " + cmd.getHostName(), ee);
            throw new CloudRuntimeException("Failed to add Nuage VSP device " + cmd.getHostName(), ee);
        }
    }

    @Override
    public NuageVspDeviceVO updateNuageVspDevice(UpdateNuageVspDeviceCmd command) {
        NuageVspResource resource = new NuageVspResource();
        final String deviceName = Network.Provider.NuageVsp.getName();
        ExternalNetworkDeviceManager.NetworkDevice networkDevice = ExternalNetworkDeviceManager.NetworkDevice.getNetworkDevice(deviceName);
        final Long physicalNetworkId = command.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(),
                networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: "
                    + physicalNetworkId + "to add this device");
        }
        if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        HostVO nuageVspHost = null;
        NuageVspDeviceVO nuageVspDevice = null;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices == null || nuageVspDevices.isEmpty()) {
            throw new CloudRuntimeException("Nuage VSD is not configured on physical network " + physicalNetworkId);
        } else {
            nuageVspDevice = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(nuageVspDevice.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        }

        boolean updateRequired = false;
        NuageVspResource.Configuration resourceConfiguration = NuageVspResource.Configuration.fromConfiguration(nuageVspHost.getDetails());
        if (!Strings.isNullOrEmpty(command.getHostName()) &&
                !command.getHostName().equals(resourceConfiguration.hostName())) {
            resourceConfiguration.name("Nuage VSD - " + command.getHostName());
            resourceConfiguration.hostName(command.getHostName());
            updateRequired = true;
        }

        if (!Strings.isNullOrEmpty(command.getUserName()) &&
                !command.getUserName().equals(resourceConfiguration.cmsUser())) {
            resourceConfiguration.cmsUser(command.getUserName());
            updateRequired = true;
        }

        if (!Strings.isNullOrEmpty(command.getPassword())) {
            String encodedNewPassword = NuageVspUtil.encodePassword(command.getPassword());
            if (!encodedNewPassword.equals(resourceConfiguration.cmsUserPassword())) {
                resourceConfiguration.cmsUserPassword(encodedNewPassword);
                updateRequired = true;
            }
        }

        if (command.getPort() != null &&
                command.getPort() != Integer.parseInt(resourceConfiguration.port())) {
            resourceConfiguration.port(String.valueOf(command.getPort()));
            updateRequired = true;
        }

        GetClientDefaultsCommand getClientDefaultsCmd = new GetClientDefaultsCommand();
        GetClientDefaultsAnswer getClientDefaultsAnswer = (GetClientDefaultsAnswer) _agentMgr.easySend(nuageVspHost.getId(), getClientDefaultsCmd);
        String apiVersion = MoreObjects.firstNonNull(command.getApiVersion(), resourceConfiguration.apiVersion());
        SupportedApiVersionCommand supportedApiVersionCmd = new SupportedApiVersionCommand(apiVersion);
        Answer supportedApiVersionAnswer = _agentMgr.easySend(nuageVspHost.getId(), supportedApiVersionCmd);
        if (!supportedApiVersionAnswer.getResult()) {
            throw new CloudRuntimeException("Incorrect API version: Nuage plugin only supports " + getClientDefaultsAnswer.getCurrentApiVersion());
        }

        String apiRelativePath = "/nuage/api/" + apiVersion;
        if (!apiRelativePath.equals(resourceConfiguration.apiRelativePath())) {
            resourceConfiguration.apiVersion(apiVersion);
            resourceConfiguration.apiRelativePath(apiRelativePath);
            updateRequired = true;
        }

        if (command.getApiRetryCount() != null && resourceConfiguration.retryCount() != null) {
            final int retryCount = Integer.parseInt(resourceConfiguration.retryCount());
            if (command.getApiRetryCount() != retryCount) {
                resourceConfiguration.retryCount(String.valueOf(command.getApiRetryCount()));
                updateRequired = true;
            }
        }

        if (command.getApiRetryInterval() != null && resourceConfiguration.retryInterval() != null) {
            final int apiRetryInterval = Integer.parseInt(resourceConfiguration.retryInterval());
            if (command.getApiRetryInterval() != apiRetryInterval) {
                resourceConfiguration.retryInterval(String.valueOf(command.getApiRetryInterval()));
                updateRequired = true;
            }
        }

        if (!updateRequired) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No change in the NuageVsp device parameters. None of the NuageVsp device parameters are modified");
            }
            return nuageVspDevice;
        }

        Map<String, String> config = resourceConfiguration.build();
        String updateParameters = "{" + Joiner.on(", ").withKeyValueSeparator(": ").join(config) + "}";
        Map<String, Object> hostDetails = Maps.<String, Object>newHashMap(config);
        try {
            resource.configure(resourceConfiguration.hostName(), hostDetails);
            _hostDetailsDao.persist(nuageVspDevice.getHostId(), config);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Failed to update Nuage VSP device " + nuageVspDevice.getId() + " with parameters " + updateParameters, e);
        }
        return nuageVspDevice;
    }

    @Override
    public NuageVspDeviceResponse createNuageVspDeviceResponse(NuageVspDeviceVO nuageVspDeviceVO) {
        HostVO nuageVspHost = _hostDao.findById(nuageVspDeviceVO.getHostId());
        _hostDao.loadDetails(nuageVspHost);

        NuageVspDeviceResponse response = new NuageVspDeviceResponse();
        response.setDeviceName(nuageVspDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(nuageVspDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(nuageVspDeviceVO.getUuid());
        response.setProviderName(nuageVspDeviceVO.getProviderName());
        response.setHostName(nuageVspHost.getDetail("hostname"));
        response.setPort(Integer.parseInt(nuageVspHost.getDetail("port")));
        String apiRelativePath = nuageVspHost.getDetail("apirelativepath");
        response.setApiVersion(apiRelativePath.substring(apiRelativePath.lastIndexOf('/') + 1));
        response.setApiRetryCount(Integer.parseInt(nuageVspHost.getDetail("retrycount")));
        response.setApiRetryInterval(Long.parseLong(nuageVspHost.getDetail("retryinterval")));
        response.setObjectName("nuagevspdevice");
        return response;
    }

    @Override
    public boolean deleteNuageVspDevice(DeleteNuageVspDeviceCmd cmd) {
        Long nuageDeviceId = cmd.getNuageVspDeviceId();
        NuageVspDeviceVO nuageVspDevice = _nuageVspDao.findById(nuageDeviceId);
        if (nuageVspDevice == null) {
            throw new InvalidParameterValueException("Could not find a Nuage Vsp device with id " + nuageDeviceId);
        }

        // Find the physical network we work for
        Long physicalNetworkId = nuageVspDevice.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            // Lets see if there are networks that use us
            // Find the nuage networks on this physical network
            List<NetworkVO> networkList = _networkDao.listByPhysicalNetwork(physicalNetworkId);

            // Networks with broadcast type lswitch are ours
            for (NetworkVO network : networkList) {
                if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Vsp) {
                    if ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy)) {
                        throw new CloudRuntimeException("This Nuage Vsp device can not be deleted as there are one or more logical networks provisioned by Cloudstack.");
                    }
                }
            }
        }

        ConfigurationVO cmsIdConfig = _configDao.findByName("nuagevsp.cms.id");
        HostVO host = findNuageVspHost(nuageVspDevice.getHostId());
        if (!auditDomainsOnVsp(host, false, true)) {
            return false;
        }

        String nuageVspCmsId = findNuageVspCmsIdForDevice(nuageVspDevice.getId(), cmsIdConfig);
        SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.UNREGISTER, nuageVspCmsId);
        SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
        if (answer != null && answer.getSuccess()) {
            String currentValue = cmsIdConfig.getValue();
            String newValue = currentValue.replace(nuageVspDevice.getId() + ":" + answer.getNuageVspCmsId(), "");
            if (!Strings.isNullOrEmpty(newValue) && newValue.startsWith(";")) {
                newValue = newValue.substring(1);
            }
            _configDao.update("nuagevsp.cms.id", newValue);
        } else {
            return false;
        }

        HostVO nuageHost = _hostDao.findById(nuageVspDevice.getHostId());
        Long hostId = nuageHost.getId();

        nuageHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, nuageHost);
        _resourceMgr.deleteHost(hostId, false, false);

        _nuageVspDao.remove(nuageDeviceId);
        return true;
    }

    @Override
    public List<NuageVspDeviceVO> listNuageVspDevices(ListNuageVspDevicesCmd cmd) {
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long nuageVspDeviceId = cmd.getNuageVspDeviceId();
        List<NuageVspDeviceVO> responseList = new ArrayList<NuageVspDeviceVO>();

        if (physicalNetworkId == null && nuageVspDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or Nuage device Id must be specified");
        }

        if (nuageVspDeviceId != null) {
            NuageVspDeviceVO nuageVspDevice = _nuageVspDao.findById(nuageVspDeviceId);
            if (nuageVspDevice == null) {
                throw new InvalidParameterValueException("Could not find Nuage Vsp device with id: " + nuageVspDeviceId);
            }
            responseList.add(nuageVspDevice);
        } else {
            PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
            }
            responseList = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        }

        return responseList;
    }

    private void registerNewNuageVspDevice(ConfigurationVO currentConfig, String registeredNuageVspDevice) {
        if (currentConfig == null) {
            ConfigKey<String> configKey = new ConfigKey<String>("Advanced", String.class, "nuagevsp.cms.id", registeredNuageVspDevice,
                    "<ACS Nuage VSP Device ID>:<Allocated VSD CMS ID> - Do not edit", false);
            ConfigurationVO configuration = new ConfigurationVO("management-server", configKey);
            _configDao.persist(configuration);
        } else {
            String newValue;
            String currentValue = currentConfig.getValue();
            if (!Strings.isNullOrEmpty(currentValue)) {
                newValue = currentValue + ";" + registeredNuageVspDevice;
            } else {
                newValue = registeredNuageVspDevice;
            }
            _configDao.update("nuagevsp.cms.id", newValue);
        }
    }

    private void auditHost(HostVO host) {
        _hostDao.loadDetails(host);

        boolean validateDomains = true;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByHost(host.getId());
        if (!CollectionUtils.isEmpty(nuageVspDevices)) {
            for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                ConfigurationVO cmsIdConfig = _configDao.findByName("nuagevsp.cms.id");
                String nuageVspCmsId = findNuageVspCmsIdForDevice(nuageVspDevice.getId(), cmsIdConfig);
                SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.AUDIT, nuageVspCmsId);
                SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);

                if (answer != null && !answer.getSuccess()) {
                    s_logger.error("Nuage VSP Device with ID " + nuageVspDevice.getId() + " is configured with an unknown CMS ID!");
                    validateDomains = false;
                } else if (answer != null && answer.getSyncType() == SyncType.REGISTER) {
                    registerNewNuageVspDevice(cmsIdConfig, nuageVspDevice.getId() + ":" + answer.getNuageVspCmsId());
                }
            }
        }

        if (validateDomains) {
            auditDomainsOnVsp(host, true, false);
        }
    }

    private boolean auditDomainsOnVsp(HostVO host, boolean add, boolean remove) {
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByHost(host.getId());
        if (CollectionUtils.isEmpty(nuageVspDevices)) {
            return true;
        }

        _hostDao.loadDetails(host);
        List<DomainVO> allDomains = _domainDao.listAll();
        for (DomainVO domain : allDomains) {
            SyncDomainCommand cmd = new SyncDomainCommand(domain.getUuid(), domain.getName(), domain.getPath(), add, remove);
            SyncDomainAnswer answer = (SyncDomainAnswer) _agentMgr.easySend(host.getId(), cmd);
            return answer.getSuccess();
        }
        return true;
    }

    private String findNuageVspCmsIdForDevice(long deviceId, ConfigurationVO cmsIdConfig) {
        String configValue = cmsIdConfig.getValue();
        if (!Strings.isNullOrEmpty(configValue)) {
            String[] configuredNuageVspDevices = configValue.split(";");
            for (String configuredNuageVspDevice : configuredNuageVspDevices) {
                if (configuredNuageVspDevice.startsWith(deviceId + ":")) {
                    String[] split = configuredNuageVspDevice.split(":");
                    if (split.length != 2 || (split.length > 1 && Strings.isNullOrEmpty(split[1]))) {
                        throw new IllegalArgumentException("The configured CMS ID for Nuage VSP device " + deviceId + " is in an incorrect format");
                    }
                    return split[1];
                }
            }
        }
        return null;
    }

    public List<String> getDnsDetails(Network network) {
        Boolean configureDns = Boolean.valueOf(_configDao.getValue(NuageVspManager.NuageVspConfigDns.key()));
        if (!configureDns) {
            return Lists.newArrayList();
        }

        Boolean configureExternalDns = Boolean.valueOf(_configDao.getValue(NuageVspManager.NuageVspDnsExternal.key()));
        DataCenterVO dc = _dataCenterDao.findById(network.getDataCenterId());
        List<String> dnsServers = new ArrayList<String>();
        if (configureExternalDns) {
            if (!Strings.isNullOrEmpty(dc.getDns1())) {
                dnsServers.add(dc.getDns1());
            }
            if (!Strings.isNullOrEmpty(dc.getDns2())) {
                dnsServers.add(dc.getDns2());
            }
        } else {
            if (!Strings.isNullOrEmpty(dc.getInternalDns1())) {
                dnsServers.add(dc.getInternalDns1());
            }
            if (!Strings.isNullOrEmpty(dc.getInternalDns2())) {
                dnsServers.add(dc.getInternalDns2());
            }
        }
        return dnsServers;
    }

    public List<String> getGatewaySystemIds() {
        String gatewaySystemIds = String.valueOf(_configDao.getValue(NuageVspManager.NuageVspConfigGateway.key()));
        if (!Strings.isNullOrEmpty(gatewaySystemIds)) {
            return Lists.newArrayList(gatewaySystemIds.split(","));
        }
        return Lists.newArrayList();
    }

    @Override
    public boolean preStateTransitionEvent(Status oldState, Status.Event event, Status newState, Host host, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<Status, Status.Event> transition, Host vo, boolean status, Object opaque) {
        // Whenever a Nuage VSP Host comes up, check if all CS domains are present and check if the CMS ID is valid
        if (transition.getToState() == Status.Up && vo instanceof HostVO) {
            auditHost((HostVO) vo);
        }
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        initMessageBusListeners();
        initNuageVspResourceListeners();
        initNuageNetworkOffering();
        initNuageVspVpcOffering();
        Status.getStateMachine().registerListener(this);
        return true;
    }

    @DB
    private void initMessageBusListeners() {
        // Create corresponding enterprise and profile in VSP when creating a CS Domain
        _messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                Long domainId = (Long) args;
                Domain domain = _domainDao.findById(domainId);

                try {
                    _domainDao.acquireInLockTable(domain.getId());

                    List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
                    for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                        HostVO host = findNuageVspHost(nuageVspDevice.getHostId());
                        SyncDomainCommand cmd = new SyncDomainCommand(domain.getUuid(), domain.getName(), domain.getPath(), true, false);
                        _agentMgr.easySend(host.getId(), cmd);
                    }
                } finally {
                    _domainDao.releaseFromLockTable(domain.getId());
                }
            }
        });

        // Delete corresponding enterprise and profile in VSP when deleting a CS Domain
        _messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object args) {
                DomainVO domain = (DomainVO) args;
                List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
                for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                    HostVO host = findNuageVspHost(nuageVspDevice.getHostId());
                    SyncDomainCommand cmd = new SyncDomainCommand(domain.getUuid(), domain.getName(), domain.getPath(), false, true);
                    _agentMgr.easySend(host.getId(), cmd);
                }
            }
        });
    }

    @DB
    private void initNuageVspResourceListeners() {
        _agentMgr.registerForHostEvents(new Listener() {
            @Override
            public boolean processAnswers(long agentId, long seq, Answer[] answers) {
                return true;
            }

            @Override
            public boolean processCommands(long agentId, long seq, Command[] commands) {
                if (commands != null && commands.length == 1) {
                    Command command = commands[0];
                    if (command instanceof PingNuageVspCommand) {
                        PingNuageVspCommand pingNuageVspCommand = (PingNuageVspCommand) command;
                        if (pingNuageVspCommand.shouldAudit()) {
                            Host host = _hostDao.findById(pingNuageVspCommand.getHostId());
                            auditHost((HostVO) host);
                        }
                    }
                }
                return true;
            }

            @Override
            public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
                return null;
            }

            @Override
            public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {

            }

            @Override
            public boolean processDisconnect(long agentId, Status state) {
                return true;
            }

            @Override
            public boolean isRecurring() {
                return false;
            }

            @Override
            public int getTimeout() {
                return 0;
            }

            @Override
            public boolean processTimeout(long agentId, long seq) {
                return true;
            }
        }, false, true, false);
    }

    @DB
    private void initNuageNetworkOffering() {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                NetworkOffering sharedNetworkOfferingWithSG = _networkOfferingDao.findByUniqueName(nuageVspSharedNetworkOfferingWithSGServiceName);
                if (sharedNetworkOfferingWithSG == null) {
                    NetworkOfferingVO defaultNuageVspSharedSGNetworkOffering =
                            new NetworkOfferingVO(nuageVspSharedNetworkOfferingWithSGServiceName, "Offering for NuageVsp Shared Security group enabled networks",
                                    Networks.TrafficType.Guest, false, false, null, null, true, NetworkOffering.Availability.Optional, null, Network.GuestType.Shared, true, true, false, false, false);

                    defaultNuageVspSharedSGNetworkOffering.setState(NetworkOffering.State.Enabled);
                    defaultNuageVspSharedSGNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultNuageVspSharedSGNetworkOffering);

                    Map<Network.Service, Network.Provider> defaultNuageVspSharedSGNetworkOfferingProviders = new HashMap<>();
                    defaultNuageVspSharedSGNetworkOfferingProviders.put(Network.Service.Dhcp, Network.Provider.NuageVsp);
                    defaultNuageVspSharedSGNetworkOfferingProviders.put(Network.Service.SecurityGroup, Network.Provider.NuageVsp);
                    defaultNuageVspSharedSGNetworkOfferingProviders.put(Network.Service.Connectivity, Network.Provider.NuageVsp);

                    for (Network.Service service : defaultNuageVspSharedSGNetworkOfferingProviders.keySet()) {
                        NetworkOfferingServiceMapVO offService =
                                new NetworkOfferingServiceMapVO(defaultNuageVspSharedSGNetworkOffering.getId(), service, defaultNuageVspSharedSGNetworkOfferingProviders.get(service));
                        _networkOfferingServiceMapDao.persist(offService);
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Added service for the NuageVsp network offering: " + offService);
                        }
                    }
                }
            }
        });
    }

    @DB
    private void initNuageVspVpcOffering() {
        //configure default Nuage VSP vpc offering
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                if (_vpcOffDao.findByUniqueName(nuageVPCOfferingName) == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Creating default Nuage VPC offering " + nuageVPCOfferingName);
                    }

                    Map<Network.Service, Set<Network.Provider>> svcProviderMap = Maps.newHashMap(NUAGE_VSP_VPC_SERVICE_MAP);
                    Set<Network.Provider> userDataProviders = Collections.singleton(Network.Provider.VPCVirtualRouter);
                    svcProviderMap.put(Network.Service.UserData, userDataProviders);

                    createVpcOffering(nuageVPCOfferingName, nuageVPCOfferingDisplayText, svcProviderMap, true, VpcOffering.State.Enabled, null);
                }
            }
        });
    }

    @DB
    protected VpcOffering createVpcOffering(final String name, final String displayText, final Map<Network.Service, Set<Network.Provider>> svcProviderMap, final boolean isDefault,
                                            final VpcOffering.State state, final Long serviceOfferingId) {
        return Transaction.execute(new TransactionCallback<VpcOffering>() {
            @Override
            public VpcOffering doInTransaction(TransactionStatus status) {
                // create vpc offering object
                VpcOfferingVO offering = new VpcOfferingVO(name, displayText, isDefault, serviceOfferingId, false, false);

                if (state != null) {
                    offering.setState(state);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Adding vpc offering " + offering);
                }
                offering = _vpcOffDao.persist(offering);
                // populate services and providers
                if (svcProviderMap != null) {
                    for (Network.Service service : svcProviderMap.keySet()) {
                        Set<Network.Provider> providers = svcProviderMap.get(service);
                        if (providers != null && !providers.isEmpty()) {
                            for (Network.Provider provider : providers) {
                                VpcOfferingServiceMapVO offService = new VpcOfferingServiceMapVO(offering.getId(), service, provider);
                                _vpcOffSvcMapDao.persist(offService);
                                if (s_logger.isTraceEnabled()) {
                                    s_logger.trace("Added service for the vpc offering: " + offService + " with provider " + provider.getName());
                                }
                            }
                        } else {
                            throw new InvalidParameterValueException("Provider is missing for the VPC offering service " + service.getName());
                        }
                    }
                }
                return offering;
            }
        });
    }

    private HostVO findNuageVspHost(long hostId) {
        HostVO host = _hostDao.findById(hostId);
        _hostDao.loadDetails(host);
        return host;
    }

    @Override
    public String getConfigComponentName() {
        return NuageVspManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return Arrays.copyOf(NUAGE_VSP_CONFIG_KEYS, NUAGE_VSP_CONFIG_KEYS.length);
    }
}
