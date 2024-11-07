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

import com.cloud.agent.api.Answer;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetrisProviderVO;
import com.cloud.network.netris.NetrisProvider;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.google.common.annotations.VisibleForTesting;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNetrisProviderCmd;
import org.apache.cloudstack.api.command.DeleteNetrisProviderCmd;
import org.apache.cloudstack.api.command.ListNetrisProvidersCmd;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.resource.NetrisResource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Collectors;

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
    private IPAddressDao ipAddressDao;
    @Inject
    private VlanDao vlanDao;

    @Override
    public NetrisProvider addProvider(AddNetrisProviderCmd cmd) {
        final Long zoneId = cmd.getZoneId();
        final String name = cmd.getName();
        final String hostname = cmd.getHostname();
        final String port = cmd.getPort();
        final String username = cmd.getUsername();
        final String password = cmd.getPassword();
        final String tenantName = cmd.getTenantName();
        final String siteName = cmd.getSiteName();

        Map<String, String> params = new HashMap<>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneId.toString());
        params.put("name", name);
        params.put("hostname", hostname);
        params.put("port", port);
        params.put("username", username);
        params.put("password", password);
        params.put("siteName", siteName);
        params.put("tenantName", tenantName);

        Map<String, Object> hostdetails = new HashMap<>(params);
        NetrisProvider netrisProvider;

        NetrisResource netrisResource = new NetrisResource();
        try {
            netrisResource.configure(hostname, hostdetails);
            final Host host = resourceManager.addHost(zoneId, netrisResource, netrisResource.getType(), params);
            if (host != null) {
                netrisProvider = Transaction.execute((TransactionCallback<NetrisProviderVO>) status -> {
                    NetrisProviderVO netrisProviderVO = new NetrisProviderVO.Builder()
                            .setZoneId(zoneId)
                            .setHostId(host.getId())
                            .setName(name)
                            .setPort(port)
                            .setHostname(hostname)
                            .setUsername(username)
                            .setPassword(password)
                            .setSiteName(siteName)
                            .setTenantName(tenantName)
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
            createNetrisPublicIpRangesOnNetrisProvider(zoneId, netrisResource);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return  netrisProvider;
    }

    /**
     * Calculate the minimum CIDR subnet containing the IP range (using the library: <a href="https://github.com/seancfoley/IPAddress">IPAddress</a>)
     * From: <a href="https://github.com/seancfoley/IPAddress/wiki/Code-Examples-3:-Subnetting-and-Other-Subnet-Operations#from-start-and-end-address-get-single-cidr-block-covering-both">Example</a>
     * @param ipRange format: startIP-endIP
     * @return the minimum CIDR containing the IP range
     */
    protected String calculateSubnetCidrFromIpRange(String ipRange) {
        if (StringUtils.isBlank(ipRange) || !ipRange.contains("-")) {
            return null;
        }
        String[] rangeArray = ipRange.split("-");
        String startIp = rangeArray[0];
        String endIp = rangeArray[1];
        IPAddress startIpAddress = new IPAddressString(startIp).getAddress();
        IPAddress endIpAddress = new IPAddressString(endIp).getAddress();
        return startIpAddress.coverWithPrefixBlock(endIpAddress).toPrefixLengthString();
    }

    /**
     * Prepare the Netris Public Range to be used by CloudStack after the zone is created and the Netris provider is added
     */
    public SetupNetrisPublicRangeCommand createSetupPublicRangeCommand(long zoneId, String gateway, String netmask, String ipRange) {
        String superCidr = NetUtils.getCidrFromGatewayAndNetmask(gateway, netmask);
        String subnetNatCidr = calculateSubnetCidrFromIpRange(ipRange);
        return new SetupNetrisPublicRangeCommand(zoneId, superCidr, subnetNatCidr);
    }

    protected void createNetrisPublicIpRangesOnNetrisProvider(long zoneId, NetrisResource netrisResource) {
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        physicalNetworks = physicalNetworks.stream().filter(x -> x.getIsolationMethods().contains(Network.Provider.Netris.getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(physicalNetworks)) {
            return;
        }
        for (PhysicalNetworkVO physicalNetwork : physicalNetworks) {
            List<IPAddressVO> publicIps = ipAddressDao.listByPhysicalNetworkId(physicalNetwork.getId());
            List<Long> vlanDbIds = publicIps.stream()
                    .filter(x -> !x.isForSystemVms())
                    .map(IPAddressVO::getVlanId)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(vlanDbIds)) {
                String msg = "Cannot find a public IP range VLAN range for the Netris Public traffic";
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            if (vlanDbIds.size() > 1) {
                logger.warn("Expected one Netris Public IP range but found {}. Using the first one to create the Netris IPAM allocation and NAT subnet", vlanDbIds.size());
            }
            VlanVO vlanRecord = vlanDao.findById(vlanDbIds.get(0));
            if (vlanRecord == null) {
                logger.error("Cannot set up the Netris Public IP range as it cannot find the public range on database");
                return;
            }
            String gateway = vlanRecord.getVlanGateway();
            String netmask = vlanRecord.getVlanNetmask();
            String ipRange = vlanRecord.getIpRange();
            SetupNetrisPublicRangeCommand cmd = createSetupPublicRangeCommand(zoneId, gateway, netmask, ipRange);
            Answer answer = netrisResource.executeRequest(cmd);
            boolean result = answer != null && answer.getResult();
            if (!result) {
                throw new CloudRuntimeException("Netris Public IP Range setup failed, please check the logs");
            }
        }
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
        response.setHostname(provider.getHostname());
        response.setPort(provider.getPort());
        response.setZoneId(zone.getUuid());
        response.setZoneName(zone.getName());
        response.setSiteName(provider.getSiteName());
        response.setTenantName(provider.getTenantName());
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
