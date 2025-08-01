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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetrisProviderVO;
import com.cloud.network.netris.NetrisProvider;
import com.cloud.network.netris.NetrisService;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.annotations.VisibleForTesting;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNetrisProviderCmd;
import org.apache.cloudstack.api.command.DeleteNetrisProviderCmd;
import org.apache.cloudstack.api.command.ListNetrisProvidersCmd;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.resource.NetrisResource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class NetrisProviderServiceImpl implements NetrisProviderService {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    ResourceManager resourceManager;
    @Inject
    NetrisProviderDao netrisProviderDao;
    @Inject
    HostDetailsDao hostDetailsDao;
    @Inject
    PhysicalNetworkDao physicalNetworkDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    private NetrisService netrisService;

    @Override
    public NetrisProvider addProvider(AddNetrisProviderCmd cmd) {
        final Long zoneId = cmd.getZoneId();
        final String name = cmd.getName();
        final String url = cmd.getUrl();
        final String username = cmd.getUsername();
        final String password = cmd.getPassword();
        final String tenantName = cmd.getTenantName();
        final String siteName = cmd.getSiteName();
        final String netrisTag = cmd.getNetrisTag();

        Map<String, String> params = new HashMap<>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneId.toString());
        params.put("name", name);
        params.put("url", url);
        params.put("username", username);
        params.put("password", password);
        params.put("siteName", siteName);
        params.put("tenantName", tenantName);
        params.put("netrisTag", netrisTag);

        Map<String, Object> hostdetails = new HashMap<>(params);
        NetrisProvider netrisProvider;

        NetrisResource netrisResource = new NetrisResource();
        try {
            netrisResource.configure(url, hostdetails);
            final Host host = resourceManager.addHost(zoneId, netrisResource, netrisResource.getType(), params);
            if (host != null) {
                netrisProvider = Transaction.execute((TransactionCallback<NetrisProviderVO>) status -> {
                    NetrisProviderVO netrisProviderVO = new NetrisProviderVO.Builder()
                            .setZoneId(zoneId)
                            .setHostId(host.getId())
                            .setName(name)
                            .setUrl(url)
                            .setUsername(username)
                            .setPassword(password)
                            .setSiteName(siteName)
                            .setTenantName(tenantName)
                            .setNetrisTag(netrisTag)
                            .build();

                    netrisProviderDao.persist(netrisProviderVO);

                    DetailVO detail = new DetailVO(host.getId(), "netriscontrollerid",
                            String.valueOf(netrisProviderVO.getId()));
                    hostDetailsDao.persist(detail);

                    return netrisProviderVO;
                });
            } else {
                throw new CloudRuntimeException("Failed to add Netris controller due to internal error.");
            }
            netrisService.createIPAMAllocationsForZoneLevelPublicRanges(zoneId);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return  netrisProvider;
    }

    @Override
    public List<BaseResponse> listNetrisProviders(Long zoneId) {
        List<BaseResponse> netrisControllersResponseList = new ArrayList<>();
        if (zoneId != null) {
            NetrisProviderVO netrisProviderVO = netrisProviderDao.findByZoneId(zoneId);
            if (Objects.nonNull(netrisProviderVO)) {
                netrisControllersResponseList.add(createNetrisProviderResponse(netrisProviderVO));
            }
        } else {
            List<NetrisProviderVO> netrisProviderVOList = netrisProviderDao.listAll();
            for (NetrisProviderVO nsxProviderVO : netrisProviderVOList) {
                netrisControllersResponseList.add(createNetrisProviderResponse(nsxProviderVO));
            }
        }

        return netrisControllersResponseList;
    }

    @Override
    public boolean deleteNetrisProvider(Long providerId) {
        NetrisProviderVO netrisProvider = netrisProviderDao.findById(providerId);
        if (Objects.isNull(netrisProvider)) {
            throw new InvalidParameterValueException(String.format("Failed to find Netris provider with id: %s", providerId));
        }
        Long zoneId = netrisProvider.getZoneId();
        // Find the physical network we work for
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZone(zoneId);
        for (PhysicalNetworkVO physicalNetwork : physicalNetworks) {
            List<NetworkVO> networkList = networkDao.listByPhysicalNetwork(physicalNetwork.getId());
            if (!CollectionUtils.isEmpty(networkList)) {
                validateNetworkState(networkList);
            }
        }
        netrisProviderDao.remove(providerId);
        return true;
    }

    @Override
    public NetrisProviderResponse createNetrisProviderResponse(NetrisProvider provider) {
        DataCenterVO zone  = dataCenterDao.findById(provider.getZoneId());
        if (Objects.isNull(zone)) {
            throw new CloudRuntimeException(String.format("Failed to find zone with id %s", provider.getZoneId()));
        }

        NetrisProviderResponse response = new NetrisProviderResponse();
        response.setName(provider.getName());
        response.setUuid(provider.getUuid());
        response.setHostname(provider.getUrl());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());
        response.setSiteName(provider.getSiteName());
        response.setTenantName(provider.getTenantName());
        response.setNetrisTag(provider.getNetrisTag());
        response.setObjectName("netrisProvider");
        return response;
    }

    @VisibleForTesting
    void validateNetworkState(List<NetworkVO> networkList) {
        for (NetworkVO network : networkList) {
            if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Netris &&
                    ((network.getState() != Network.State.Shutdown) && (network.getState() != Network.State.Destroy))) {
                throw new CloudRuntimeException("This Netris provider cannot be deleted as there are one or more logical networks provisioned by CloudStack on it.");
            }
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        if (Boolean.TRUE.equals(NetworkOrchestrationService.NETRIS_ENABLED.value())) {
            cmdList.add(AddNetrisProviderCmd.class);
            cmdList.add(ListNetrisProvidersCmd.class);
            cmdList.add(DeleteNetrisProviderCmd.class);
        }
        return cmdList;
    }
}
