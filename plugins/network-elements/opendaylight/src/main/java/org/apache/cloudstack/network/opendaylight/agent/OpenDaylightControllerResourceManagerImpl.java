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

package org.apache.cloudstack.network.opendaylight.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.cloudstack.network.opendaylight.api.commands.AddOpenDaylightControllerCmd;
import org.apache.cloudstack.network.opendaylight.api.commands.DeleteOpenDaylightControllerCmd;
import org.apache.cloudstack.network.opendaylight.api.commands.ListOpenDaylightControllersCmd;
import org.apache.cloudstack.network.opendaylight.api.responses.OpenDaylightControllerResponse;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerMappingDao;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerVO;

import com.cloud.api.ApiDBUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class OpenDaylightControllerResourceManagerImpl implements OpenDaylightControllerResourceManager {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    HostDao hostDao;
    @Inject
    ResourceManager resourceManager;
    @Inject
    PhysicalNetworkDao physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;
    @Inject
    OpenDaylightControllerMappingDao openDaylightControllerMappingDao;
    @Inject
    NetworkDao networkDao;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<Class<?>>();
        commands.add(AddOpenDaylightControllerCmd.class);
        commands.add(DeleteOpenDaylightControllerCmd.class);
        commands.add(ListOpenDaylightControllersCmd.class);
        return commands;
    }

    @Override
    public OpenDaylightControllerVO addController(AddOpenDaylightControllerCmd cmd) {
        ServerResource odlController = new OpenDaylightControllerResource();

        final String deviceName = NetworkDevice.OpenDaylightController.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        if (networkDevice == null) {
            throw new CloudRuntimeException("No network device found for name " + deviceName);
        }
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider = physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(),
                networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: "
                    + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: "
                    + physicalNetworkId + "to add this device");
        }

        final Map<String, String> hostParams = new HashMap<String, String>();
        hostParams.put("guid", UUID.randomUUID().toString());
        hostParams.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        hostParams.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        hostParams.put("name", "ODL Controller - " + hostParams.get("guid"));
        hostParams.put("url", cmd.getUrl());
        hostParams.put("username", cmd.getUsername());
        hostParams.put("password", cmd.getPassword());

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(hostParams);

        try {
            odlController.configure(hostParams.get("name"), hostdetails);
            final Host host = resourceManager.addHost(zoneId, odlController, Host.Type.L2Networking, hostParams);
            if (host != null) {
                return Transaction.execute(new TransactionCallback<OpenDaylightControllerVO>() {
                    @Override
                    public OpenDaylightControllerVO doInTransaction(TransactionStatus status) {
                        OpenDaylightControllerVO controller = new OpenDaylightControllerVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), hostParams
                                .get("name"));
                        openDaylightControllerMappingDao.persist(controller);
                        return controller;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to create host object for ODL Controller");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Failed to add ODL Controller as a resource", e);
        }
    }

    @Override
    public void deleteController(DeleteOpenDaylightControllerCmd cmd) throws InvalidParameterValueException {
        OpenDaylightControllerVO controller = openDaylightControllerMappingDao.findById(cmd.getId());
        if (controller == null) {
            throw new InvalidParameterValueException("No ODL Controller with id " + cmd.getId());
        }

        // Find the physical network we work for
        Long physicalNetworkId = controller.getPhysicalNetworkId();
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork != null) {
            // Lets see if there are networks that use us
            List<NetworkVO> networkList = networkDao.listByPhysicalNetwork(physicalNetworkId);

            if (networkList != null) {
                // Networks with broadcast type lswitch are ours
                for (NetworkVO network : networkList) {
                    if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.OpenDaylight) {
                        if ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy)) {
                            throw new CloudRuntimeException("This Controller can not be deleted as there are one or more logical networks provisioned by cloudstack.");
                        }
                    }
                }
            }
        }

        HostVO host = hostDao.findById(controller.getHostId());
        Long hostId = host.getId();

        host.setResourceState(ResourceState.Maintenance);
        hostDao.update(hostId, host);
        resourceManager.deleteHost(hostId, false, false);

        openDaylightControllerMappingDao.remove(cmd.getId());
    }

    @Override
    public List<OpenDaylightControllerVO> listControllers(ListOpenDaylightControllersCmd cmd) {
        if (cmd.getId() != null) {
            List<OpenDaylightControllerVO> foundControllers = new ArrayList<OpenDaylightControllerVO>();
            OpenDaylightControllerVO controller = openDaylightControllerMappingDao.findById(cmd.getId());
            if (controller != null) {
                foundControllers.add(controller);
            }
            return foundControllers;
        } else if (cmd.getPhysicalNetworkId() != null) {
            return openDaylightControllerMappingDao.listByPhysicalNetwork(cmd.getPhysicalNetworkId());
        }
        return openDaylightControllerMappingDao.listAll();
    }

    @Override
    public OpenDaylightControllerResponse createResponseFromVO(OpenDaylightControllerVO controller) {
        OpenDaylightControllerResponse response = new OpenDaylightControllerResponse();
        HostVO controllerHost = hostDao.findById(controller.getHostId());
        hostDao.loadDetails(controllerHost);

        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(controller.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }

        response.setObjectName("opendaylightcontroller");
        response.setId(controller.getUuid());
        response.setUrl(controllerHost.getDetail("url"));
        response.setName(controllerHost.getDetail("name"));
        response.setUsername(controllerHost.getDetail("username"));

        return response;
    }

}
