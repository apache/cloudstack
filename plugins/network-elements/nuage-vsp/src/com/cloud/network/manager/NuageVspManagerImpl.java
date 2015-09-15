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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.IssueNuageVspResourceRequestCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.NuageVspResource;
import com.cloud.network.sync.NuageVspSync;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = {NuageVspManager.class})
public class NuageVspManagerImpl extends ManagerBase implements NuageVspManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(NuageVspManagerImpl.class);

    private static final int ONE_MINUTE_MULTIPLIER = 60 * 1000;

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
    ConfigurationDao _configDao;
    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffSvcMapDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    NuageVspDao nuageVspDao;
    @Inject
    NuageVspSync nuageVspSync;

    private ScheduledExecutorService scheduler;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddNuageVspDeviceCmd.class);
        cmdList.add(DeleteNuageVspDeviceCmd.class);
        cmdList.add(ListNuageVspDevicesCmd.class);
        cmdList.add(IssueNuageVspResourceRequestCmd.class);

        return cmdList;
    }

    @Override
    public NuageVspDeviceVO addNuageVspDevice(AddNuageVspDeviceCmd cmd) {
        ServerResource resource = new NuageVspResource();
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
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not added in the physical network: " + physicalNetworkId
                    + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        if (_nuageVspDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A NuageVsp device is already configured on this physical network");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Nuage VSD - " + cmd.getHostName());
        params.put("hostname", cmd.getHostName());
        params.put("cmsuser", cmd.getUserName());
        String cmsUserPasswordBase64 = org.apache.commons.codec.binary.StringUtils.newStringUtf8(Base64.encodeBase64(cmd.getPassword().getBytes(Charset.forName("UTF-8"))));
        params.put("cmsuserpass", cmsUserPasswordBase64);
        int port = cmd.getPort();
        if (0 == port) {
            port = 443;
        }
        params.put("port", String.valueOf(port));
        params.put("apirelativepath", "/nuage/api/" + cmd.getApiVersion());
        params.put("retrycount", String.valueOf(cmd.getApiRetryCount()));
        params.put("retryinterval", String.valueOf(cmd.getApiRetryInterval()));

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            resource.configure(cmd.getHostName(), hostdetails);

            final Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.L2Networking, params);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<NuageVspDeviceVO>() {
                    @Override
                    public NuageVspDeviceVO doInTransaction(TransactionStatus status) {
                        NuageVspDeviceVO nuageVspDevice = new NuageVspDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                        _nuageVspDao.persist(nuageVspDevice);

                        DetailVO detail = new DetailVO(host.getId(), "nuagevspdeviceid", String.valueOf(nuageVspDevice.getId()));
                        _hostDetailsDao.persist(detail);

                        return nuageVspDevice;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add Nuage Vsp Device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
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
            // Find the NuageVsp on this physical network
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

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            initNuageScheduledTasks();
        } catch (Exception ce) {
            s_logger.warn("Failed to load NuageVsp configuration properties. Check if the NuageVsp properties are configured correctly");
        }
        return true;
    }

    private void initNuageScheduledTasks() {
        Integer numOfSyncThreads = Integer.valueOf(_configDao.getValue(NuageVspManager.NuageVspSyncWorkers.key()));
        Integer syncUpIntervalInMinutes = Integer.valueOf(_configDao.getValue(NuageVspManager.NuageVspSyncInterval.key()));

        if (numOfSyncThreads != null && syncUpIntervalInMinutes != null) {
            ThreadFactory threadFactory = new ThreadFactory() {
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "Nuage Vsp sync task");
                    if (thread.isDaemon())
                        thread.setDaemon(false);
                    if (thread.getPriority() != Thread.NORM_PRIORITY)
                        thread.setPriority(Thread.NORM_PRIORITY);
                    return thread;
                }
            };
            scheduler = Executors.newScheduledThreadPool(numOfSyncThreads, threadFactory);
            scheduler.scheduleWithFixedDelay(new NuageVspSyncTask("FLOATING_IP"), ONE_MINUTE_MULTIPLIER * 15, ONE_MINUTE_MULTIPLIER * syncUpIntervalInMinutes,
                    TimeUnit.MILLISECONDS);
            scheduler.scheduleWithFixedDelay(new NuageVspSyncTask("ENTERPRISE_NTWK_MACRO"), ONE_MINUTE_MULTIPLIER * 15, ONE_MINUTE_MULTIPLIER * syncUpIntervalInMinutes,
                    TimeUnit.MILLISECONDS);
            scheduler
                    .scheduleWithFixedDelay(new NuageVspSyncTask("ENTERPRISE"), ONE_MINUTE_MULTIPLIER * 15, ONE_MINUTE_MULTIPLIER * syncUpIntervalInMinutes, TimeUnit.MILLISECONDS);
        } else {
            s_logger.warn("NuageVsp configuration for syncWorkers=" + numOfSyncThreads + " syncInterval=" + syncUpIntervalInMinutes
                    + " could not be read properly. So, check if the properties are configured properly in global properties");
        }
    }

    public class NuageVspSyncTask implements Runnable {

        private String nuageVspEntity;

        public NuageVspSyncTask(String nuageVspEntity) {
            this.nuageVspEntity = nuageVspEntity;
        }

        public String getNuageVspEntity() {
            return nuageVspEntity;
        }

        @Override
        public void run() {
            nuageVspSync.syncWithNuageVsp(nuageVspEntity);
        }
    }

    @Override
    public String getConfigComponentName() {
        return NuageVspManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {NuageVspSyncWorkers, NuageVspSyncInterval};
    }
}
