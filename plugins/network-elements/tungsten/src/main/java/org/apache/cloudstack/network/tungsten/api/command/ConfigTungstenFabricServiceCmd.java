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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@APICommand(name = ConfigTungstenFabricServiceCmd.APINAME, description = "config Tungsten-Fabric service",
    responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigTungstenFabricServiceCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ConfigTungstenFabricServiceCmd.class.getName());
    public static final String APINAME = "configTungstenFabricService";
    public static final String NETWORKOFFERING = "DefaultTungstenFarbicNetworkOffering";

    @Inject
    NetworkModel networkModel;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    NetworkServiceMapDao networkServiceMapDao;
    @Inject
    PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "the ID of physical network")
    private Long physicalNetworkId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(final Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        DataCenter dataCenter = _entityMgr.findById(DataCenter.class, zoneId);
        if (dataCenter.isSecurityGroupEnabled()) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Management);
                    NetworkServiceMapVO managementNetworkServiceMapVO = new NetworkServiceMapVO(managementNetwork.getId(),
                        Network.Service.Connectivity, Network.Provider.Tungsten);
                    if (!networkServiceMapDao.canProviderSupportServiceInNetwork(managementNetwork.getId(),
                            Network.Service.Connectivity, Network.Provider.Tungsten)) {
                        networkServiceMapDao.persist(managementNetworkServiceMapVO);
                    }

                    List<NetworkOfferingVO> systemNetworkOffering = networkOfferingDao.listSystemNetworkOfferings();
                    for (NetworkOfferingVO networkOffering : systemNetworkOffering) {
                        if (networkOffering.getTrafficType() == Networks.TrafficType.Management){
                            NetworkOfferingServiceMapVO publicNetworkOfferingServiceMapVO =
                                new NetworkOfferingServiceMapVO(
                                    networkOffering.getId(), Network.Service.Connectivity, Network.Provider.Tungsten);
                            networkOfferingServiceMapDao.persist(publicNetworkOfferingServiceMapVO);
                        }
                    }
                }
            });
        } else {
            persistDefaultSystemNetwork();
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("configured Tungsten-Fabric service successfully");

        setResponseObject(response);
    }

    private void persistDefaultSystemNetwork() {
        Transaction.execute(new TransactionCallbackNoReturn() {

            private void persistNetworkServiceMapAvoidingDuplicates(Network network,
                                                                    NetworkServiceMapVO mapVO) {
                if (mapVO == null) {
                    s_logger.error("Expected a network-service-provider mapping entity to be persisted");
                    return;
                }
                Network.Service service = Network.Service.getService(mapVO.getService());
                Network.Provider provider = Network.Provider.getProvider(mapVO.getProvider());
                if (service == null || provider == null) {
                    s_logger.error(String.format("Could not obtain the service or the provider " +
                            "from the network-service-provider map with ID = %s", mapVO.getId()));
                    return;
                }
                if (networkServiceMapDao.canProviderSupportServiceInNetwork(network.getId(), service, provider)) {
                    s_logger.debug(String.format("A mapping between the network, service and provider (%s, %s, %s) " +
                                    "already exists, skipping duplicated entry",
                            network.getId(), service.getName(), provider.getName()));
                    return;

                }
                networkServiceMapDao.persist(mapVO);
            }

            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                NetworkOfferingVO networkOfferingVO = networkOfferingDao.findByUniqueName(NETWORKOFFERING);
                if (networkOfferingVO == null) {
                    networkOfferingVO = new NetworkOfferingVO(NETWORKOFFERING,
                            "Default offering for Tungsten-Fabric Network", Networks.TrafficType.Guest, false, false,
                            null, null, true, NetworkOffering.Availability.Optional, null, Network.GuestType.Isolated,
                            true, false, false, false, true, false);
                    networkOfferingVO.setForTungsten(true);
                    networkOfferingVO.setState(NetworkOffering.State.Enabled);
                    networkOfferingDao.persist(networkOfferingVO);

                    Map<Network.Service, Network.Provider> tungstenServiceProvider = new HashMap<>();
                    tungstenServiceProvider.put(Network.Service.Dhcp, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Dns, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.SourceNat, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.StaticNat, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Connectivity, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Firewall, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Lb, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.PortForwarding, Network.Provider.Tungsten);

                    for (Map.Entry<Network.Service, Network.Provider> providerEntry : tungstenServiceProvider.entrySet()) {
                        NetworkOfferingServiceMapVO networkOfferingServiceMapVO = new NetworkOfferingServiceMapVO(
                                networkOfferingVO.getId(), providerEntry.getKey(), providerEntry.getValue());
                        networkOfferingServiceMapDao.persist(networkOfferingServiceMapVO);
                    }
                }

                PhysicalNetworkServiceProviderVO physicalNetworkServiceProvider = physicalNetworkServiceProviderDao.findByServiceProvider(
                        physicalNetworkId, Network.Provider.Tungsten.getName());
                physicalNetworkServiceProvider.setState(PhysicalNetworkServiceProvider.State.Enabled);
                physicalNetworkServiceProviderDao.persist(physicalNetworkServiceProvider);

                Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
                NetworkServiceMapVO publicNetworkServiceMapVO = new NetworkServiceMapVO(publicNetwork.getId(),
                        Network.Service.Connectivity, Network.Provider.Tungsten);
                persistNetworkServiceMapAvoidingDuplicates(publicNetwork, publicNetworkServiceMapVO);

                Network managementNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Management);
                NetworkServiceMapVO managementNetworkServiceMapVO = new NetworkServiceMapVO(managementNetwork.getId(),
                        Network.Service.Connectivity, Network.Provider.Tungsten);
                persistNetworkServiceMapAvoidingDuplicates(managementNetwork, managementNetworkServiceMapVO);

                List<NetworkOfferingVO> systemNetworkOffering = networkOfferingDao.listSystemNetworkOfferings();
                for (NetworkOfferingVO networkOffering : systemNetworkOffering) {
                    if (networkOffering.getTrafficType() == Networks.TrafficType.Public
                            || networkOffering.getTrafficType() == Networks.TrafficType.Management){
                        NetworkOfferingServiceMapVO publicNetworkOfferingServiceMapVO =
                                new NetworkOfferingServiceMapVO(
                                        networkOffering.getId(), Network.Service.Connectivity, Network.Provider.Tungsten);
                        networkOfferingServiceMapDao.persist(publicNetworkOfferingServiceMapVO);
                    }
                }
            }
        });
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
