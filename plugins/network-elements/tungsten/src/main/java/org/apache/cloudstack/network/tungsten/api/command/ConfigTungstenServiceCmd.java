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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
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

@APICommand(name = "configTungstenService", description = "config tungsten service", responseObject =
    SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigTungstenServiceCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ConfigTungstenServiceCmd.class.getName());
    private static final String s_name = "configtungstenserviceresponse";
    public final static String DefaultTungstenNetworkOffering = "DefaultTungstenNetworkOffering";

    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Inject
    NetworkServiceMapDao _networkServiceMapDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType =
        PhysicalNetworkResponse.class, required = true, description = "the ID of physical network")
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
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                NetworkOfferingVO networkOfferingVO = _networkOfferingDao.findByUniqueName(
                    DefaultTungstenNetworkOffering);
                if (networkOfferingVO == null) {
                    networkOfferingVO = new NetworkOfferingVO(DefaultTungstenNetworkOffering,
                        "Default offering for Tungsten Network", Networks.TrafficType.Guest, false, false, null, null,
                        true, NetworkOffering.Availability.Optional, null, Network.GuestType.Isolated, true, false,
                        false, false, false, false);
                    networkOfferingVO.setForTungsten(true);
                    networkOfferingVO.setState(NetworkOffering.State.Enabled);
                    _networkOfferingDao.persist(networkOfferingVO);

                    Map<Network.Service, Network.Provider> tungstenServiceProvider = new HashMap<>();
                    tungstenServiceProvider.put(Network.Service.Dhcp, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Dns, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.UserData, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.SourceNat, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.StaticNat, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Connectivity, Network.Provider.Tungsten);
                    tungstenServiceProvider.put(Network.Service.Firewall, Network.Provider.Tungsten);

                    for (Network.Service service : tungstenServiceProvider.keySet()) {
                        NetworkOfferingServiceMapVO networkOfferingServiceMapVO = new NetworkOfferingServiceMapVO(
                            networkOfferingVO.getId(), service, tungstenServiceProvider.get(service));
                        _networkOfferingServiceMapDao.persist(networkOfferingServiceMapVO);
                    }
                }

                PhysicalNetworkServiceProviderVO physicalNetworkServiceProvider =
                    _physicalNetworkServiceProviderDao.findByServiceProvider(
                    physicalNetworkId, Network.Provider.Tungsten.getName());
                physicalNetworkServiceProvider.setState(PhysicalNetworkServiceProvider.State.Enabled);
                _physicalNetworkServiceProviderDao.persist(physicalNetworkServiceProvider);

                List<NetworkVO> publicNetworkVOList = _networkDao.listByZoneAndTrafficType(zoneId,
                    Networks.TrafficType.Public);
                NetworkVO publicNetwork = publicNetworkVOList.get(0);
                NetworkServiceMapVO networkServiceMapVO = new NetworkServiceMapVO(publicNetwork.getId(),
                    Network.Service.Connectivity, Network.Provider.Tungsten);
                _networkServiceMapDao.persist(networkServiceMapVO);
            }
        });

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("config tungsten service successfully");

        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
