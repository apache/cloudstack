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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.nuage.vsp.acs.client.api.NuageVspPluginClientLoader;
import net.nuage.vsp.acs.client.api.model.VspApiDefaults;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspDomainCleanUp;
import net.nuage.vsp.acs.client.api.model.VspDomainTemplate;
import net.nuage.vsp.acs.client.api.model.VspHost;
import net.nuage.vsp.acs.client.common.NuageVspApiVersion;
import net.nuage.vsp.acs.client.common.NuageVspConstants;
import net.nuage.vsp.acs.client.exception.NuageVspException;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingNuageVspCommand;
import com.cloud.agent.api.manager.CleanUpDomainCommand;
import com.cloud.agent.api.manager.EntityExistsCommand;
import com.cloud.agent.api.manager.GetApiDefaultsAnswer;
import com.cloud.agent.api.manager.GetApiDefaultsCommand;
import com.cloud.agent.api.manager.ListVspDomainTemplatesAnswer;
import com.cloud.agent.api.manager.ListVspDomainTemplatesCommand;
import com.cloud.agent.api.manager.SupportedApiVersionCommand;
import com.cloud.agent.api.manager.UpdateNuageVspDeviceCommand;
import com.cloud.agent.api.sync.SyncDomainCommand;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdAnswer;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.AssociateNuageVspDomainTemplateCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.DisableNuageUnderlayVlanIpRangeCmd;
import com.cloud.api.commands.EnableNuageUnderlayVlanIpRangeCmd;
import com.cloud.api.commands.ListNuageUnderlayVlanIpRangesCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.commands.ListNuageVspDomainTemplatesCmd;
import com.cloud.api.commands.ListNuageVspGlobalDomainTemplateCmd;
import com.cloud.api.commands.UpdateNuageVspDeviceCmd;
import com.cloud.api.response.NuageVlanIpRangeResponse;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.api.response.NuageVspDomainTemplateResponse;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanDetailsVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.network.resource.NuageVspResourceConfiguration;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.user.DomainManager;
import com.cloud.util.NuageVspEntityBuilder;
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

import static com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand.SyncType;

public class NuageVspManagerImpl extends ManagerBase implements NuageVspManager, Configurable, StateListener<Status, Status.Event, Host> {

    private static final Logger s_logger = Logger.getLogger(NuageVspManagerImpl.class);

    public static final Multimap<Network.Service, Network.Provider> DEFAULT_NUAGE_VSP_VPC_SERVICE_MAP;
    public static final Multimap<Network.Service, Network.Provider> SUPPORTED_NUAGE_VSP_VPC_SERVICE_MAP;
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
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffSvcMapDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    private VpcDetailsDao _vpcDetailsDao;
    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    DataCenterDao _dataCenterDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    private DomainDao _domainDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Inject
    NuageVspEntityBuilder _nuageVspEntityBuilder;
    @Inject
    VlanDao _vlanDao;
    @Inject
    VlanDetailsDao _vlanDetailsDao;
    @Inject
    ResponseGenerator _responseGenerator;
    @Inject
    MessageBus _messageBus;

    static {
        Set<Network.Provider> nuageVspProviders = ImmutableSet.of(Network.Provider.NuageVsp);
        Set<Network.Provider> vrProviders = ImmutableSet.of(Network.Provider.VPCVirtualRouter);
        Set<Network.Provider> defaultLbProviders = ImmutableSet.of(Network.Provider.InternalLbVm);
        Set<Network.Provider> supportedLbProviders = ImmutableSet.of(Network.Provider.InternalLbVm);
        Set<Network.Provider> supportedUserDataProviders = ImmutableSet.of(Network.Provider.VPCVirtualRouter, Network.Provider.ConfigDrive);

        DEFAULT_NUAGE_VSP_VPC_SERVICE_MAP = ImmutableMultimap.<Network.Service, Network.Provider>builder()
                .putAll(Network.Service.Connectivity, nuageVspProviders)
                .putAll(Network.Service.Gateway, nuageVspProviders)
                .putAll(Network.Service.Dhcp, nuageVspProviders)
                .putAll(Network.Service.StaticNat, nuageVspProviders)
                .putAll(Network.Service.SourceNat, nuageVspProviders)
                .putAll(Network.Service.NetworkACL, nuageVspProviders)
                .putAll(Network.Service.UserData, vrProviders)
                .putAll(Network.Service.Lb, defaultLbProviders)
                .putAll(Network.Service.Dns, vrProviders)
                .build();

        Multimap<Network.Service, Network.Provider> builder = HashMultimap.create(DEFAULT_NUAGE_VSP_VPC_SERVICE_MAP);
        builder.putAll(Network.Service.UserData, supportedUserDataProviders);
        builder.putAll(Network.Service.Lb, supportedLbProviders);

        SUPPORTED_NUAGE_VSP_VPC_SERVICE_MAP = ImmutableMultimap.copyOf(builder);
    }

    private Listener _nuageVspResourceListener;

    @Override
    public List<Class<?>> getCommands() {
        return Lists.<Class<?>>newArrayList(
                AddNuageVspDeviceCmd.class,
                DeleteNuageVspDeviceCmd.class,
                UpdateNuageVspDeviceCmd.class,
                ListNuageVspDevicesCmd.class,

                DisableNuageUnderlayVlanIpRangeCmd.class,
                EnableNuageUnderlayVlanIpRangeCmd.class,
                ListNuageUnderlayVlanIpRangesCmd.class,

                ListNuageVspDomainTemplatesCmd.class,
                ListNuageVspGlobalDomainTemplateCmd.class,
                AssociateNuageVspDomainTemplateCmd.class
        );
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

        // While the default VSD port is 8443, clustering via HAProxy will go over port 443 (CLOUD-58)
        int port = cmd.getPort() > 0 ? cmd.getPort() : 443;

        try {
            String apiVersion = null;

            String cmsUserPasswordBase64 = NuageVspUtil.encodePassword(cmd.getPassword());

            NuageVspResourceConfiguration resourceConfiguration = new NuageVspResourceConfiguration()
                    .guid(UUID.randomUUID().toString())
                    .zoneId(String.valueOf(physicalNetwork.getDataCenterId()))
                    .hostName(cmd.getHostName())
                    .cmsUser(cmd.getUserName())
                    .cmsUserPassword(cmsUserPasswordBase64)
                    .port(String.valueOf(port))
                    .apiVersion(NuageVspApiVersion.CURRENT.toString())
                    .retryCount(NuageVspConstants.DEFAULT_API_RETRY_COUNT.toString())
                    .retryInterval(NuageVspConstants.DEFAULT_API_RETRY_INTERVAL.toString())
                    .apiRelativePath("/nuage");

            VspHost vspHost = resourceConfiguration.buildVspHost();
            NuageVspPluginClientLoader clientLoader = NuageVspPluginClientLoader.getClientLoader(vspHost);
            VspApiDefaults apiDefaults = clientLoader.getNuageVspManagerClient().getApiDefaults();


            if (StringUtils.isNotBlank(cmd.getApiVersion())){
                apiVersion = cmd.getApiVersion();
                if (!clientLoader.getNuageVspManagerClient().isSupportedApiVersion(apiVersion)){
                    throw new CloudRuntimeException("Unsupported API version : " + cmd.getApiVersion());
                }
            } else {
                List<NuageVspApiVersion> supportedVsdVersions = clientLoader.getNuageVspManagerClient().getSupportedVersionList();
                supportedVsdVersions.retainAll(Arrays.asList(NuageVspApiVersion.SUPPORTED_VERSIONS));

                if(supportedVsdVersions.isEmpty()) {
                    throw new CloudRuntimeException("No supported API version found!");
                }

                supportedVsdVersions.sort(Comparator.reverseOrder());
                apiVersion = supportedVsdVersions.get(0).toString();
            }


            String retryCount = String.valueOf(MoreObjects.firstNonNull(cmd.getApiRetryCount(), apiDefaults.getRetryCount()));
            String retryInterval = String.valueOf(MoreObjects.firstNonNull(cmd.getApiRetryInterval(), apiDefaults.getRetryInterval()));

            resourceConfiguration
                    .apiVersion(apiVersion)
                    .apiRelativePath("/nuage/api/" + apiVersion)
                    .retryCount(retryCount)
                    .retryInterval(retryInterval);
            Map<String, String> hostDetails = resourceConfiguration.build();
            resource.configure("Nuage VSD - " + cmd.getHostName(), Maps.<String, Object>newHashMap(hostDetails));


            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, hostDetails);
            if (host == null) {
                throw new CloudRuntimeException("Failed to add Nuage Vsp Device due to internal error.");
            }

            NuageVspDeviceVO nuageVspDevice = new NuageVspDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
            _nuageVspDao.persist(nuageVspDevice);

            DetailVO detail = new DetailVO(host.getId(), "nuagevspdeviceid", String.valueOf(nuageVspDevice.getId()));
            _hostDetailsDao.persist(detail);

            NuageVspDeviceVO matchingNuageVspDevice = findMatchingNuageVspDevice(nuageVspDevice);
            String cmsId;
            if (matchingNuageVspDevice != null) {
                cmsId = findNuageVspCmsIdForDeviceOrHost(matchingNuageVspDevice.getId(), matchingNuageVspDevice.getHostId());
            } else {
                SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.REGISTER, null);
                SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
                if (answer != null && answer.getSuccess()) {
                    cmsId = answer.getNuageVspCmsId();
                } else {
                    throw new CloudRuntimeException("Failed to register CMS ID");
                }
            }

            host = findNuageVspHost(nuageVspDevice.getHostId());
            registerNewNuageVspDevice(host.getId(), cmsId);

            resourceConfiguration.nuageVspCmsId(cmsId);
            resource.configure(cmd.getHostName(), Maps.<String, Object>newHashMap(resourceConfiguration.build()));

            if (matchingNuageVspDevice == null) {
                auditDomainsOnVsp((HostVO) host, true);
            }

            return nuageVspDevice;
        } catch (ConfigurationException e) {
            s_logger.error("Failed to configure Nuage VSD resource " + cmd.getHostName(), e);
            throw new CloudRuntimeException("Failed to configure Nuage VSD resource " + cmd.getHostName(), e);
        } catch (NuageVspException ee) {
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

        boolean resourceConfigurationChanged = false;
        NuageVspResourceConfiguration resourceConfiguration = NuageVspResourceConfiguration.fromConfiguration(nuageVspHost.getDetails());
        if (!Strings.isNullOrEmpty(command.getHostName()) &&
                !command.getHostName().equals(resourceConfiguration.hostName())) {
            resourceConfiguration.hostName(command.getHostName());
            resourceConfigurationChanged = true;
        }

        if (!Strings.isNullOrEmpty(command.getUserName()) &&
                !command.getUserName().equals(resourceConfiguration.cmsUser())) {
            resourceConfiguration.cmsUser(command.getUserName());
            resourceConfigurationChanged = true;
        }

        if (!Strings.isNullOrEmpty(command.getPassword())) {
            String encodedNewPassword = NuageVspUtil.encodePassword(command.getPassword());
            if (!encodedNewPassword.equals(resourceConfiguration.cmsUserPassword())) {
                resourceConfiguration.cmsUserPassword(encodedNewPassword);
                resourceConfigurationChanged = true;
            }
        }

        if (command.getPort() != null &&
                command.getPort() != Integer.parseInt(resourceConfiguration.port())) {
            resourceConfiguration.port(String.valueOf(command.getPort()));
            resourceConfigurationChanged = true;
        }

        String apiVersion = MoreObjects.firstNonNull(command.getApiVersion(), resourceConfiguration.apiVersion());
        NuageVspApiVersion apiVersionObj = NuageVspApiVersion.fromString(apiVersion);
        NuageVspApiVersion apiVersionCurrent = null;
        try {
            apiVersionCurrent = resourceConfiguration.getApiVersion();
        } catch (ConfigurationException e){
            throw new CloudRuntimeException("Current version is not configured correctly");
        }


        if(command.getApiVersion() != null){
            if(apiVersionObj.compareTo(apiVersionCurrent) < 0) {
                throw new CloudRuntimeException("Downgrading is not supported");
            }

            GetApiDefaultsCommand apiDefaultsCmd = new GetApiDefaultsCommand();
            GetApiDefaultsAnswer apiDefaultsAnswer = (GetApiDefaultsAnswer) _agentMgr.easySend(nuageVspHost.getId(), apiDefaultsCmd);

            SupportedApiVersionCommand supportedApiVersionCmd = new SupportedApiVersionCommand(apiVersion);
            Answer supportedApiVersionAnswer = _agentMgr.easySend(nuageVspHost.getId(), supportedApiVersionCmd);

            if (!supportedApiVersionAnswer.getResult()) {
                throw new CloudRuntimeException("Incorrect API version: Nuage plugin only supports " + apiDefaultsAnswer.getApiDefaults().getVersion());
            }

            String apiRelativePath = "/nuage/api/" + apiVersion;
            if (!apiRelativePath.equals(resourceConfiguration.apiRelativePath())) {
                resourceConfiguration.apiVersion(apiVersion);
                resourceConfiguration.apiRelativePath(apiRelativePath);
                resourceConfigurationChanged = true;
            }

        }

        if (command.getApiRetryCount() != null && resourceConfiguration.retryCount() != null) {
            final int retryCount = Integer.parseInt(resourceConfiguration.retryCount());
            if (command.getApiRetryCount() != retryCount) {
                resourceConfiguration.retryCount(String.valueOf(command.getApiRetryCount()));
                resourceConfigurationChanged = true;
            }
        }

        if (command.getApiRetryInterval() != null && resourceConfiguration.retryInterval() != null) {
            final int apiRetryInterval = Integer.parseInt(resourceConfiguration.retryInterval());
            if (command.getApiRetryInterval() != apiRetryInterval) {
                resourceConfiguration.retryInterval(String.valueOf(command.getApiRetryInterval()));
                resourceConfigurationChanged = true;
            }
        }

        if (!resourceConfigurationChanged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No change in the NuageVsp device parameters. None of the NuageVsp device parameters are modified");
            }
            return nuageVspDevice;
        }

        Map<String, String> config = resourceConfiguration.build();
        try {
            resource.validate(config);

            UpdateNuageVspDeviceCommand cmd = new UpdateNuageVspDeviceCommand(resourceConfiguration);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("UpdateNuageVspDeviceCommand failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    throw new CloudRuntimeException(answer.getDetails());
                }
            }

            _hostDetailsDao.persist(nuageVspDevice.getHostId(), config);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Failed to update Nuage VSP device " + nuageVspDevice.getId() + " with parameters " + resourceConfiguration, e);
        }
        return nuageVspDevice;
    }

    @Override
    public NuageVspDeviceResponse createNuageVspDeviceResponse(NuageVspDeviceVO nuageVspDeviceVO) {
        HostVO nuageVspHost = _hostDao.findById(nuageVspDeviceVO.getHostId());
        _hostDao.loadDetails(nuageVspHost);

        NuageVspResourceConfiguration resourceConfiguration = NuageVspResourceConfiguration.fromConfiguration(nuageVspHost.getDetails());
        NuageVspDeviceResponse response = new NuageVspDeviceResponse();
        response.setDeviceName(nuageVspDeviceVO.getDeviceName());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(nuageVspDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setId(nuageVspDeviceVO.getUuid());
        response.setProviderName(nuageVspDeviceVO.getProviderName());
        response.setHostName(resourceConfiguration.hostName());
        response.setPort(Integer.parseInt(resourceConfiguration.port()));
        String apiRelativePath = resourceConfiguration.apiRelativePath();
        response.setApiVersion(apiRelativePath.substring(apiRelativePath.lastIndexOf('/') + 1));
        response.setApiRetryCount(Integer.parseInt(resourceConfiguration.retryCount()));
        response.setApiRetryInterval(Long.parseLong(resourceConfiguration.retryInterval()));
        response.setCmsId(resourceConfiguration.nuageVspCmsId());
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

        NuageVspDeviceVO matchingNuageVspDevice = findMatchingNuageVspDevice(nuageVspDevice);

        String nuageVspCmsId = findNuageVspCmsIdForDeviceOrHost(nuageVspDevice.getId(), nuageVspDevice.getHostId());
        if (matchingNuageVspDevice == null) {
            HostVO host = findNuageVspHost(nuageVspDevice.getHostId());
            if (!auditDomainsOnVsp(host, false)) {
                return false;
            }

            SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.UNREGISTER, nuageVspCmsId);
            SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
            if (answer == null || !answer.getSuccess()) {
                return false;
            }
        }

        removeLegacyNuageVspDeviceCmsId(nuageVspDevice.getId());

        HostVO nuageHost = _hostDao.findById(nuageVspDevice.getHostId());
        Long hostId = nuageHost.getId();

        nuageHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, nuageHost);
        _resourceMgr.deleteHost(hostId, false, false);

        _nuageVspDao.remove(nuageDeviceId);
        return true;
    }

    private NuageVspDeviceVO findMatchingNuageVspDevice(NuageVspDeviceVO nuageVspDevice) {
        DetailVO nuageVspDeviceHost =  _hostDetailsDao.findDetail(nuageVspDevice.getHostId(), "hostname");
        String nuageVspDeviceHostName = (nuageVspDeviceHost != null) ? nuageVspDeviceHost.getValue(): null;

        List<NuageVspDeviceVO> otherNuageVspDevices = _nuageVspDao.listAll();
        for (NuageVspDeviceVO otherNuageVspDevice : otherNuageVspDevices) {
            if (otherNuageVspDevice.getId() == nuageVspDevice.getId()) continue;

            DetailVO otherNuageVspDeviceHostName = _hostDetailsDao.findDetail(otherNuageVspDevice.getHostId(), "hostname");
            if (otherNuageVspDeviceHostName != null && nuageVspDeviceHostName.equals(otherNuageVspDeviceHostName.getValue())) {
                return otherNuageVspDevice;
            }
        }
        return null;
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

    private void registerNewNuageVspDevice(long hostId, String cmsId) {
        DetailVO detail = new DetailVO(hostId, "nuagevspcmsid", cmsId);
        _hostDetailsDao.persist(detail);
    }

    @Deprecated
    private void removeLegacyNuageVspDeviceCmsId(long deviceId) {
        ConfigurationVO cmsIdConfig = _configDao.findByName(CMSID_CONFIG_KEY);
        if (cmsIdConfig != null) {
            if (!cmsIdConfig.getValue().contains(";") && cmsIdConfig.getValue().startsWith(deviceId + ":")) {
                _configDao.update(CMSID_CONFIG_KEY, "Advanced", "");
            } else {
                String newValue = cmsIdConfig.getValue().replace(String.format("(^|;)%d:[0-9a-f\\-]+;?", deviceId), ";");
                _configDao.update(CMSID_CONFIG_KEY, "Advanced", newValue);
            }
        }
    }

    public boolean executeSyncCmsId(NuageVspDeviceVO nuageVspDevice, SyncType syncType) {
        NuageVspDeviceVO matchingNuageVspDevice = findMatchingNuageVspDevice(nuageVspDevice);
        if (syncType == SyncType.REGISTER && matchingNuageVspDevice != null) {
            String cmsId = findNuageVspCmsIdForDeviceOrHost(matchingNuageVspDevice.getId(), matchingNuageVspDevice.getHostId());
            registerNewNuageVspDevice(nuageVspDevice.getHostId(), cmsId);
            return true;
        }

        String cmsId = findNuageVspCmsIdForDeviceOrHost(nuageVspDevice.getId(), nuageVspDevice.getHostId());

        SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(syncType, cmsId);
        SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
        if (answer != null) {
            if (answer.getSuccess()) {
                if (syncType == SyncType.REGISTER || answer.getSyncType() == SyncType.REGISTER) {
                    registerNewNuageVspDevice(nuageVspDevice.getHostId(), answer.getNuageVspCmsId());
                } else if (syncType == SyncType.UNREGISTER) {
                    removeLegacyNuageVspDeviceCmsId(nuageVspDevice.getId());
                }
            } else if (syncType == SyncType.AUDIT || syncType == SyncType.AUDIT_ONLY) {
                s_logger.fatal("Nuage VSP Device with ID " + nuageVspDevice.getId() + " is configured with an unknown CMS ID!");
            }
        }

        return answer != null && answer.getSuccess();
    }

    void auditHost(long hostId) {
        Host host = _hostDao.findById(hostId);
        auditHost((HostVO)host);
    }

    void auditHost(HostVO host) {
        if (host == null) return;

        _hostDao.loadDetails(host);

        boolean validateDomains = true;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByHost(host.getId());
        if (!CollectionUtils.isEmpty(nuageVspDevices)) {
            for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                String nuageVspCmsId = findNuageVspCmsIdForDeviceOrHost(nuageVspDevice.getId(), nuageVspDevice.getHostId());
                SyncNuageVspCmsIdCommand syncCmd = new SyncNuageVspCmsIdCommand(SyncType.AUDIT, nuageVspCmsId);
                SyncNuageVspCmsIdAnswer answer = (SyncNuageVspCmsIdAnswer) _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);

                if (answer != null && !answer.getSuccess()) {
                    s_logger.error("Nuage VSP Device with ID " + nuageVspDevice.getId() + " is configured with an unknown CMS ID!");
                    validateDomains = false;
                } else if (answer != null && answer.getSyncType() == SyncType.REGISTER) {
                    registerNewNuageVspDevice(nuageVspDevice.getHostId(), answer.getNuageVspCmsId());
                }
            }
        }

        if (validateDomains) {
            auditDomainsOnVsp(host, true);
        }
    }

    private boolean auditDomainsOnVsp(HostVO host, boolean add) {
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByHost(host.getId());
        if (CollectionUtils.isEmpty(nuageVspDevices)) {
            return true;
        }

        final SyncDomainCommand.Type action = add ? SyncDomainCommand.Type.ADD : SyncDomainCommand.Type.REMOVE;

        _hostDao.loadDetails(host);
        List<DomainVO> allDomains = _domainDao.listAll();
        for (DomainVO domain : allDomains) {
            if (action == SyncDomainCommand.Type.REMOVE) {
                VspDomainCleanUp vspDomainCleanUp = _nuageVspEntityBuilder.buildVspDomainCleanUp(domain);
                CleanUpDomainCommand cmd = new CleanUpDomainCommand(vspDomainCleanUp);
                Answer answer = _agentMgr.easySend(host.getId(), cmd);
                if (!answer.getResult()) {
                    return false;
                }
            }

            VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(domain);
            SyncDomainCommand cmd = new SyncDomainCommand(vspDomain, action);
            Answer answer = _agentMgr.easySend(host.getId(), cmd);
            if (!answer.getResult()) {
                return false;
            }
        }
        return true;
    }

    private String findNuageVspCmsIdForDeviceOrHost(long deviceId, long hostId) {
        String cmsId = findNuageVspCmsIdForHostDevice(hostId);
        if(cmsId == null) {
            cmsId = findNuageVspCmsIdForDevice(deviceId);

            if (cmsId != null) {
                // Upgrade
                registerNewNuageVspDevice(hostId, cmsId);
                removeLegacyNuageVspDeviceCmsId(deviceId);
            }
        }

        return cmsId;
    }

    private String findNuageVspCmsIdForHostDevice(long hostId) {
        final DetailVO cmsIdDetailVO = _hostDetailsDao.findDetail(hostId, "nuagevspcmsid");
        if (cmsIdDetailVO != null) {
            return cmsIdDetailVO.getValue();
        }
        return null;
    }

    @Deprecated
    private String findNuageVspCmsIdForDevice(long deviceId) {
        ConfigurationVO cmsIdConfig = _configDao.findByName(CMSID_CONFIG_KEY);
        if(cmsIdConfig != null) {
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
        }
        return null;
    }

    public List<String> getDnsDetails(long dataCenterId) {
        Boolean configureDns = Boolean.valueOf(_configDao.getValue(NuageVspManager.NuageVspConfigDns.key()));
        if (!configureDns) {
            return Lists.newArrayList();
        }

        Boolean configureExternalDns = Boolean.valueOf(_configDao.getValue(NuageVspManager.NuageVspDnsExternal.key()));
        DataCenterVO dc = _dataCenterDao.findById(dataCenterId);
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
    public List<NuageVspDomainTemplateResponse> listNuageVspDomainTemplates(ListNuageVspDomainTemplatesCmd cmd){
        long domainId;

        if (cmd.getDomainId() != null) {
            domainId = cmd.getDomainId();
        } else {
            domainId = CallContext.current().getCallingAccount().getDomainId();
        }

        return listNuageVspDomainTemplates(domainId, cmd.getKeyword(), cmd.getZoneId(), cmd.getPhysicalNetworkId());
    }

    @Override
    public List<NuageVspDomainTemplateResponse> listNuageVspDomainTemplates(long domainId, String keyword,  Long zoneId, Long passedPhysicalNetworkId) {
        Optional<Long> physicalNetworkId;
        Domain domain = _domainDao.findById(domainId);
        VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(domain);

        if (passedPhysicalNetworkId != null) {
            physicalNetworkId = Optional.of(passedPhysicalNetworkId);
        } else if (zoneId != null) {
            physicalNetworkId = Optional.ofNullable(getPhysicalNetworkBasedOnZone(zoneId));
        } else {
            throw new InvalidParameterValueException("No zoneid or physicalnetworkid specified.");
        }

        if (!physicalNetworkId.isPresent()) {
            return new LinkedList<>();
        }

        Long hostId = getNuageVspHostId(physicalNetworkId.get());
        if (hostId == null) {
            return new LinkedList<>();
        }

        ListVspDomainTemplatesCommand agentCmd = new ListVspDomainTemplatesCommand(vspDomain, keyword);
        ListVspDomainTemplatesAnswer answer = (ListVspDomainTemplatesAnswer) _agentMgr.easySend(hostId, agentCmd);
        List<VspDomainTemplate> domainTemplates = answer.getDomainTemplates();

        return domainTemplates.stream()
                       .map(NuageVspManagerImpl::createDomainTemplateResponse)
                       .collect(Collectors.toList());
    }

    private static NuageVspDomainTemplateResponse createDomainTemplateResponse(VspDomainTemplate dt) {
        return new NuageVspDomainTemplateResponse(dt.getName(), dt.getDescription());
    }

    /**
     * Returns the PhysicalNetworkId based on a zoneId
     * @param zoneId != null, the zone id for which we need to retrieve the PhysicalNetworkId
     * @return the physical network id if it's found otherwise null
     */
    private Long getPhysicalNetworkBasedOnZone(Long zoneId){

        Long physicalNetworkId = null;
        List<PhysicalNetworkVO> physicalNetworkVOs = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Guest);
        for (PhysicalNetworkVO physicalNetwok : physicalNetworkVOs) {
            if (physicalNetwok.getIsolationMethods().contains(NUAGE_VSP_ISOLATION)) {
                physicalNetworkId = physicalNetwok.getId();
                break;
            }
        }
        return physicalNetworkId;
    }

    @Override
    public boolean associateNuageVspDomainTemplate(AssociateNuageVspDomainTemplateCmd cmd){
        VpcVO vpc = _vpcDao.findById(cmd.getVpcId());
        Long physicalNetworkId;
        if (cmd.getPhysicalNetworkId() != null) {
            physicalNetworkId = cmd.getPhysicalNetworkId();
        } else if (cmd.getZoneId() != null) {
            physicalNetworkId = getPhysicalNetworkBasedOnZone(cmd.getZoneId());
        } else {
            throw new InvalidParameterValueException("No zoneid or physicalnetworkid specified.");
        }

        EntityExistsCommand entityCmd = new EntityExistsCommand(VpcVO.class, vpc.getUuid());
        boolean exists = entityExist(entityCmd, physicalNetworkId);
        if (exists) {
            throw new CloudRuntimeException("Failed to associate domain template, VPC is already pushed to the Nuage VSP device.");
        }

        if (!checkIfDomainTemplateExist(vpc.getDomainId(), cmd.getDomainTemplate(), cmd.getZoneId(), cmd.getPhysicalNetworkId())) {
            throw new InvalidParameterValueException("Could not find a Domain Template with name: " + cmd.getDomainTemplate());
        }
        setPreConfiguredDomainTemplateName(cmd.getVpcId(), cmd.getDomainTemplate());
        return true;
    }

    @Override
    public boolean checkIfDomainTemplateExist(Long domainId, String domainTemplate, Long zoneId, Long physicalNetworkId){
        List<NuageVspDomainTemplateResponse> domainTemplateList = listNuageVspDomainTemplates(domainId, domainTemplate, zoneId, physicalNetworkId);
        if (domainTemplateList != null) {
            for (NuageVspDomainTemplateResponse val : domainTemplateList) {
                if (val.getName().equals(domainTemplate)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean entityExist(EntityExistsCommand cmd, Long physicalNetworkId){
        Long hostId = getNuageVspHostId(physicalNetworkId);
        if (hostId == null) {
            throw new CloudRuntimeException("There is no Nuage VSP device configured on physical network " + physicalNetworkId);
        }

        Answer answer = _agentMgr.easySend(hostId, cmd);
        if (answer != null) {
            return answer.getResult();
        }
        throw new CloudRuntimeException("No answer received from the client");
    }

    /**
     * Sets the preconfigured domain template of a vpc to the given value.
     * @param vpcId
     * @param domainTemplateName
     */
    private void setPreConfiguredDomainTemplateName(long vpcId, String domainTemplateName) {
        //remove the previous nuageDomainTemplate if it is present.
        if (_vpcDetailsDao.findDetail(vpcId, NuageVspManager.nuageDomainTemplateDetailName) != null) {
           _vpcDetailsDao.removeDetail(vpcId, NuageVspManager.nuageDomainTemplateDetailName);
        }
        VpcDetailVO vpcDetail = new VpcDetailVO(vpcId, NuageVspManager.nuageDomainTemplateDetailName, domainTemplateName, false);
        _vpcDetailsDao.persist(vpcDetail);
    }

    @Override
    public void setPreConfiguredDomainTemplateName(Network network, String domainTemplateName) {

        if (network.getVpcId() != null) {
            setPreConfiguredDomainTemplateName(network.getVpcId(), domainTemplateName);
        } else {
            NetworkDetailVO networkDetail = new NetworkDetailVO(network.getId(), NuageVspManager.nuageDomainTemplateDetailName, domainTemplateName, false);
            _networkDetailsDao.persist(networkDetail);
        }
    }

    @Override
    public String getPreConfiguredDomainTemplateName(Network network) {

        if (network.getVpcId() != null) {
            VpcDetailVO domainTemplateNetworkDetail = _vpcDetailsDao.findDetail(network.getVpcId(), NuageVspManager.nuageDomainTemplateDetailName);
            if (domainTemplateNetworkDetail != null) {
               return domainTemplateNetworkDetail.getValue();
            }

            return NuageVspVpcDomainTemplateName.value();
        } else {
            NetworkDetailVO domainTemplateNetworkDetail = _networkDetailsDao.findDetail(network.getId(), NuageVspManager.nuageDomainTemplateDetailName);
            if (domainTemplateNetworkDetail != null) {
                return domainTemplateNetworkDetail.getValue();
            }

            if (network.getGuestType() == Network.GuestType.Shared) {
                return NuageVspSharedNetworkDomainTemplateName.value();
            }

            return NuageVspIsolatedNetworkDomainTemplateName.value();
        }
    }

    @Override
    public HostVO getNuageVspHost(long physicalNetworkId) {
        HostVO nuageVspHost;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (CollectionUtils.isEmpty(nuageVspDevices)) {
            // Perhaps another physical network is passed from within the same zone, find the VSP physical network in that case
            PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
            List<PhysicalNetworkVO> physicalNetworksInZone = _physicalNetworkDao.listByZone(physicalNetwork.getDataCenterId());
            for (PhysicalNetworkVO physicalNetworkInZone : physicalNetworksInZone) {
                if (physicalNetworkInZone.getIsolationMethods().contains(NUAGE_VSP_ISOLATION)) {
                    nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkInZone.getId());
                    break;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(nuageVspDevices)) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(config.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        } else {
            throw new CloudRuntimeException("There is no Nuage VSP device configured on physical network " + physicalNetworkId);
        }
        return nuageVspHost;
    }

    @Override
    public boolean updateNuageUnderlayVlanIpRange(long vlanIpRangeId, boolean enabled) {
        VlanVO staticNatVlan = _vlanDao.findById(vlanIpRangeId);
        HostVO nuageVspHost = getNuageVspHost(staticNatVlan.getPhysicalNetworkId());
        EntityExistsCommand<Vlan> cmd = new EntityExistsCommand<Vlan>(Vlan.class, staticNatVlan.getUuid());
        Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
        if (answer != null && !answer.getResult()) {
            _vlanDetailsDao.addDetail(staticNatVlan.getId(), NuageVspManager.nuageUnderlayVlanIpRangeDetailKey, String.valueOf(enabled), false);
            return true;
        }

        return false;
    }

    @Override
    public List<NuageVlanIpRangeResponse> filterNuageVlanIpRanges(List<? extends Vlan> vlanIpRanges, Boolean underlay) {
        List<NuageVlanIpRangeResponse> nuageVlanIpRanges = Lists.newArrayList();
        for (Vlan vlanIpRange : vlanIpRanges) {
            NuageVlanIpRangeResponse nuageVlanIpRange = (NuageVlanIpRangeResponse) _responseGenerator.createVlanIpRangeResponse(NuageVlanIpRangeResponse.class, vlanIpRange);

            VlanDetailsVO nuageUnderlayDetail = _vlanDetailsDao.findDetail(vlanIpRange.getId(), NuageVspManager.nuageUnderlayVlanIpRangeDetailKey);
            boolean underlayEnabled = nuageUnderlayDetail != null && nuageUnderlayDetail.getValue().equalsIgnoreCase(String.valueOf(true));
            nuageVlanIpRange.setUnderlay(underlayEnabled);
            if (underlay == null || underlayEnabled == underlay) {
                nuageVlanIpRanges.add(nuageVlanIpRange);
            }
            nuageVlanIpRange.setObjectName("nuagevlaniprange");
        }
        return nuageVlanIpRanges;
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
        _messageBus.subscribe(DomainManager.MESSAGE_ADD_DOMAIN_EVENT, (senderAddress, subject, args) -> {
            Long domainId = (Long) args;
            Domain domain = _domainDao.findById(domainId);

            try {
                _domainDao.acquireInLockTable(domain.getId());

                List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
                for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                    VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(domain);
                    SyncDomainCommand cmd = new SyncDomainCommand(vspDomain, SyncDomainCommand.Type.ADD);
                    _agentMgr.easySend(nuageVspDevice.getHostId(), cmd);
                }
            } finally {
                _domainDao.releaseFromLockTable(domain.getId());
            }
        });

        // Clean up corresponding resources in VSP when deleting a CS Domain
        _messageBus.subscribe(DomainManager.MESSAGE_PRE_REMOVE_DOMAIN_EVENT, (senderAddress, subject, args) -> {
            DomainVO domain = (DomainVO) args;
            List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
            for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                VspDomainCleanUp vspDomainCleanUp = _nuageVspEntityBuilder.buildVspDomainCleanUp(domain);
                CleanUpDomainCommand cmd = new CleanUpDomainCommand(vspDomainCleanUp);
                _agentMgr.easySend(nuageVspDevice.getHostId(), cmd);
            }
        });

        // Delete corresponding enterprise and profile in VSP when deleting a CS Domain
        _messageBus.subscribe(DomainManager.MESSAGE_REMOVE_DOMAIN_EVENT, (senderAddress, subject, args) -> {
            DomainVO domain = (DomainVO) args;
            List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listAll();
            for (NuageVspDeviceVO nuageVspDevice : nuageVspDevices) {
                VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(domain);
                SyncDomainCommand syncCmd = new SyncDomainCommand(vspDomain, SyncDomainCommand.Type.REMOVE);
                _agentMgr.easySend(nuageVspDevice.getHostId(), syncCmd);
            }
        });
    }

    private class NuageVspResourceListener extends AbstractListener {
        @Override
        public boolean processCommands(long agentId, long seq, Command[] commands) {
            if (commands != null && commands.length == 1) {
                Command command = commands[0];
                if (command instanceof PingNuageVspCommand) {
                    PingNuageVspCommand pingNuageVspCommand = (PingNuageVspCommand)command;
                    if (pingNuageVspCommand.shouldAudit()) {
                        auditHost(pingNuageVspCommand.getHostId());
                    }
                }
            }
            return true;
        }
    }

    @DB
    private void initNuageVspResourceListeners() {
        _agentMgr.registerForHostEvents(new NuageVspResourceListener(), false, true, false);
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
                                    Networks.TrafficType.Guest, false, false, null, null, true, NetworkOffering.Availability.Optional, null, Network.GuestType.Shared, true, true, false, false, false,
                                    false);

                    defaultNuageVspSharedSGNetworkOffering.setState(NetworkOffering.State.Enabled);
                    defaultNuageVspSharedSGNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultNuageVspSharedSGNetworkOffering);

                    Map<Network.Service, Network.Provider> defaultNuageVspSharedSGNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
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

    private Long getNuageVspHostId(long physicalNetworkId) {
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices != null && (!nuageVspDevices.isEmpty())) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            return config.getHostId();
        }

        return null;
    }

    @DB
    private void initNuageVspVpcOffering() {
        //configure default Nuage VSP vpc offering
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                VpcOffering offering = _vpcOffDao.findByUniqueName(nuageVPCOfferingName);
                if (offering == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Creating default Nuage VPC offering " + nuageVPCOfferingName);
                    }

                    createVpcOffering(nuageVPCOfferingName, nuageVPCOfferingDisplayText, DEFAULT_NUAGE_VSP_VPC_SERVICE_MAP, true, VpcOffering.State.Enabled, null);
                } else {
                    updateVpcOffering(offering, DEFAULT_NUAGE_VSP_VPC_SERVICE_MAP);
                }
            }
        });
    }

    @DB
    protected VpcOffering createVpcOffering(final String name, final String displayText, final Multimap<Network.Service, Network.Provider> svcProviderMap, final boolean isDefault,
                                            final VpcOffering.State state, final Long serviceOfferingId) {
        return Transaction.execute((TransactionCallback<VpcOffering>)status -> createVpcOfferingInTransaction(name, displayText, svcProviderMap, isDefault, state, serviceOfferingId));
    }

    private VpcOffering createVpcOfferingInTransaction(String name, String displayText, Multimap<Network.Service, Network.Provider> svcProviderMap, boolean isDefault,
            VpcOffering.State state, Long serviceOfferingId) {
        // create vpc offering object
        VpcOfferingVO offering = new VpcOfferingVO(name, displayText, isDefault, serviceOfferingId, false, false);

        if (state != null) {
            offering.setState(state);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Adding vpc offering %s", offering));
        }
        offering = _vpcOffDao.persist(offering);
        // populate services and providers
        if (svcProviderMap != null) {
            for (Map.Entry<Network.Service, Network.Provider> entry : svcProviderMap.entries()) {
                Network.Service service = entry.getKey();
                Network.Provider provider = entry.getValue();

                VpcOfferingServiceMapVO offService = new VpcOfferingServiceMapVO(offering.getId(), service, provider);
                _vpcOffSvcMapDao.persist(offService);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(String.format("Added service for the vpc offering: %s with provider %s", offService, provider.getName()));
                }
            }
        }

        return offering;
    }

    @DB
    protected void updateVpcOffering(final VpcOffering offering, final Multimap<Network.Service, Network.Provider> svcProviderMap) {
        Transaction.execute((TransactionCallback<VpcOffering>)status -> updateVpcOfferingInTransaction(offering, svcProviderMap));
    }

    @Nonnull
    private VpcOffering updateVpcOfferingInTransaction(VpcOffering offering, Multimap<Network.Service, Network.Provider> svcProviderMap) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Updating vpc offering %s", offering));
        }

        List<VpcOfferingServiceMapVO> currentVpcOfferingServices = _vpcOffSvcMapDao.listByVpcOffId(offering.getId());
        Multimap<Network.Service, Network.Provider> currentSvcProviderMap = HashMultimap.create();
        for (VpcOfferingServiceMapVO vpcOfferingService : currentVpcOfferingServices) {
            Network.Service service = Network.Service.getService(vpcOfferingService.getService());
            Network.Provider provider = Network.Provider.getProvider(vpcOfferingService.getProvider());
            currentSvcProviderMap.put(service, provider);
        }

        for (Map.Entry<Network.Service, Network.Provider> entry : svcProviderMap.entries()) {
            Network.Service service = entry.getKey();
            Network.Provider provider = entry.getValue();

            if (!currentSvcProviderMap.containsEntry(service, provider)) {
                VpcOfferingServiceMapVO offService = new VpcOfferingServiceMapVO(offering.getId(), service, provider);
                _vpcOffSvcMapDao.persist(offService);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Added service for the vpc offering: %s", offService));
                }
            }

        }

        return offering;
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
