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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.Lists;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.network.tungsten.api.command.AddTungstenFabricNetworkGatewayToLogicalRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.AddTungstenFabricPolicyRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.ApplyTungstenFabricPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.ApplyTungstenFabricTagCmd;
import org.apache.cloudstack.network.tungsten.api.command.ConfigTungstenFabricServiceCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricAddressGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricApplicationPolicySetCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricFirewallPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricFirewallRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricLogicalRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricLBHealthMonitorCmd;
import org.apache.cloudstack.network.tungsten.api.command.UpdateTungstenFabricLBHealthMonitorCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricManagementNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricPublicNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricServiceGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricTagCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenFabricTagTypeCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricAddressGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricApplicationPolicySetCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricFirewallPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricFirewallRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricLogicalRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricServiceGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricTagCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenFabricTagTypeCmd;
import org.apache.cloudstack.network.tungsten.api.command.GetLoadBalancerSslCertificateCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricAddressGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricApplictionPolicySetCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricFirewallPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricFirewallRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricLogicalRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricNicCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricPolicyRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricProvidersCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricServiceGroupCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricTagCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricTagTypeCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenFabricVmCmd;
import org.apache.cloudstack.network.tungsten.api.command.RemoveTungstenFabricNetworkGatewayFromLogicalRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.RemoveTungstenFabricPolicyCmd;
import org.apache.cloudstack.network.tungsten.api.command.RemoveTungstenFabricPolicyRuleCmd;
import org.apache.cloudstack.network.tungsten.api.command.RemoveTungstenFabricTagCmd;
import org.apache.cloudstack.network.tungsten.api.command.SynchronizeTungstenFabricDataCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricProviderResponse;
import org.apache.cloudstack.network.tungsten.resource.TungstenResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@Component
public class TungstenProviderServiceImpl extends ManagerBase implements TungstenProviderService {
    @Inject
    TungstenProviderDao tungstenProviderDao;
    @Inject
    DataCenterDao zoneDao;
    @Inject
    ResourceManager resourceMgr;
    @Inject
    HostDetailsDao hostDetailsDao;
    @Inject
    DomainDao domainDao;
    @Inject
    ProjectDao projectDao;
    @Inject
    MessageBus messageBus;

    @Override
    public List<Class<?>> getCommands() {
        return Lists.newArrayList(CreateTungstenFabricProviderCmd.class, ConfigTungstenFabricServiceCmd.class,
            CreateTungstenFabricPublicNetworkCmd.class, ListTungstenFabricProvidersCmd.class,
            CreateTungstenFabricManagementNetworkCmd.class, GetLoadBalancerSslCertificateCmd.class,
            SynchronizeTungstenFabricDataCmd.class, CreateTungstenFabricPolicyCmd.class,
            AddTungstenFabricPolicyRuleCmd.class, ListTungstenFabricPolicyCmd.class,
            ListTungstenFabricPolicyRuleCmd.class, RemoveTungstenFabricPolicyRuleCmd.class,
            DeleteTungstenFabricPolicyCmd.class, CreateTungstenFabricTagCmd.class, CreateTungstenFabricTagTypeCmd.class,
            DeleteTungstenFabricTagCmd.class, DeleteTungstenFabricTagTypeCmd.class, ListTungstenFabricTagCmd.class,
            ListTungstenFabricTagTypeCmd.class, ApplyTungstenFabricPolicyCmd.class, ApplyTungstenFabricTagCmd.class,
            RemoveTungstenFabricPolicyCmd.class, RemoveTungstenFabricTagCmd.class, ListTungstenFabricNetworkCmd.class,
            ListTungstenFabricVmCmd.class, ListTungstenFabricNicCmd.class, UpdateTungstenFabricLBHealthMonitorCmd.class,
            CreateTungstenFabricApplicationPolicySetCmd.class, CreateTungstenFabricFirewallPolicyCmd.class,
            CreateTungstenFabricFirewallRuleCmd.class, CreateTungstenFabricServiceGroupCmd.class,
            CreateTungstenFabricAddressGroupCmd.class, ListTungstenFabricApplictionPolicySetCmd.class,
            ListTungstenFabricFirewallPolicyCmd.class, ListTungstenFabricFirewallRuleCmd.class,
            ListTungstenFabricServiceGroupCmd.class, ListTungstenFabricAddressGroupCmd.class,
            DeleteTungstenFabricApplicationPolicySetCmd.class, DeleteTungstenFabricFirewallPolicyCmd.class,
            DeleteTungstenFabricFirewallRuleCmd.class, DeleteTungstenFabricAddressGroupCmd.class,
            DeleteTungstenFabricServiceGroupCmd.class, CreateTungstenFabricLogicalRouterCmd.class,
            AddTungstenFabricNetworkGatewayToLogicalRouterCmd.class, RemoveTungstenFabricNetworkGatewayFromLogicalRouterCmd.class,
            ListTungstenFabricLogicalRouterCmd.class, DeleteTungstenFabricLogicalRouterCmd.class,
            ListTungstenFabricLBHealthMonitorCmd.class);
    }

    @Override
    public TungstenProvider addProvider(CreateTungstenFabricProviderCmd cmd) {
        TungstenProviderVO tungstenProvider;
        final Long zoneId = cmd.getZoneId();
        final String name = cmd.getName();
        final String hostname = cmd.getHostname();
        final String gateway = cmd.getGateway();
        final String port = cmd.getPort() == null || cmd.getPort().equals(StringUtils.EMPTY) ? "8082" : cmd.getPort();
        final String vrouterPort =
            cmd.getVrouterPort() == null || cmd.getVrouterPort().equals(StringUtils.EMPTY) ? "9091" :
                cmd.getVrouterPort();
        final String introspectPort =
            cmd.getIntrospectPort() == null || cmd.getIntrospectPort().equals(StringUtils.EMPTY) ? "8085" :
                cmd.getIntrospectPort();

        TungstenResource tungstenResource = new TungstenResource();

        Map<String, String> params = new HashMap<>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneId.toString());
        params.put("name", "TungstenDevice - " + cmd.getName());
        params.put("hostname", cmd.getHostname());
        params.put("port", port);
        params.put("gateway", cmd.getGateway());
        params.put("vrouterPort", vrouterPort);
        params.put("introspectPort", introspectPort);
        Map<String, Object> hostdetails = new HashMap<>(params);

        try {
            tungstenResource.configure(cmd.getHostname(), hostdetails);
            final Host host = resourceMgr.addHost(zoneId, tungstenResource, Host.Type.L2Networking, params);
            if (host != null) {
                tungstenProvider = Transaction.execute((TransactionCallback<TungstenProviderVO>) status -> {
                    TungstenProviderVO tungstenProviderVO = new TungstenProviderVO(zoneId, name, host.getId(), port,
                        hostname, gateway, vrouterPort, introspectPort);
                    tungstenProviderDao.persist(tungstenProviderVO);

                    DetailVO detail = new DetailVO(host.getId(), "tungstendeviceid",
                        String.valueOf(tungstenProviderVO.getId()));
                    hostDetailsDao.persist(detail);

                    return tungstenProviderVO;
                });
                messageBus.publish(_name, TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT,
                    PublishScope.LOCAL, tungstenProvider);
            } else {
                throw new CloudRuntimeException("Failed to add Tungsten-Fabric provider due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return tungstenProvider;
    }

    @Override
    public List<BaseResponse> listTungstenProvider(Long zoneId) {
        List<BaseResponse> tungstenProviderResponseList = new ArrayList<>();
        if (zoneId != null) {
            TungstenProviderVO tungstenProviderVO = tungstenProviderDao.findByZoneId(zoneId);
            tungstenProviderResponseList.add(createTungstenProviderResponse(tungstenProviderVO));
        } else {
            List<TungstenProviderVO> tungstenProviderVOList = tungstenProviderDao.listAll();
            for (TungstenProviderVO tungstenProviderVO : tungstenProviderVOList) {
                tungstenProviderResponseList.add(createTungstenProviderResponse(tungstenProviderVO));
            }
        }

        return tungstenProviderResponseList;
    }

    @Override
    public TungstenFabricProviderResponse createTungstenProviderResponse(TungstenProvider tungstenProvider) {
        DataCenterVO zone = zoneDao.findById(tungstenProvider.getZoneId());
        String zoneName = zone.getName();
        TungstenFabricProviderResponse tungstenProviderResponse = new TungstenFabricProviderResponse();
        tungstenProviderResponse.setHostname(tungstenProvider.getHostname());
        tungstenProviderResponse.setName(tungstenProvider.getProviderName());
        tungstenProviderResponse.setPort(tungstenProvider.getPort());
        tungstenProviderResponse.setUuid(tungstenProvider.getUuid());
        tungstenProviderResponse.setGateway(tungstenProvider.getGateway());
        tungstenProviderResponse.setIntrospectPort(tungstenProvider.getIntrospectPort());
        tungstenProviderResponse.setVrouterPort(tungstenProvider.getVrouterPort());
        tungstenProviderResponse.setZoneId(tungstenProvider.getZoneId());
        tungstenProviderResponse.setZoneName(zoneName);
        tungstenProviderResponse.setSecurityGroupsEnabled(zone.isSecurityGroupEnabled());
        tungstenProviderResponse.setObjectName("tungstenProvider");
        return tungstenProviderResponse;
    }


}
