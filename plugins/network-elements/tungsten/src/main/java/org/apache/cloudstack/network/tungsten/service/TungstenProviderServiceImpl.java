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
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.Lists;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.network.tungsten.api.command.ConfigTungstenServiceCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenManagementNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenPublicNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.GetLoadBalancerSslCertificateCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.api.command.SynchronizeTungstenDataCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.resource.TungstenResource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@Component
public class TungstenProviderServiceImpl extends ManagerBase implements TungstenProviderService {

    private static final Logger s_logger = Logger.getLogger(TungstenProviderServiceImpl.class);

    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ProjectDao _projectDao;
    @Inject
    MessageBus _messageBus;

    @Override
    public List<Class<?>> getCommands() {
        return Lists.<Class<?>>newArrayList(CreateTungstenProviderCmd.class, ConfigTungstenServiceCmd.class,
            CreateTungstenPublicNetworkCmd.class, ListTungstenProvidersCmd.class,
            CreateTungstenManagementNetworkCmd.class, GetLoadBalancerSslCertificateCmd.class,
            SynchronizeTungstenDataCmd.class);
    }

    @Override
    public TungstenProvider addProvider(CreateTungstenProviderCmd cmd) {
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

        DataCenterVO zone = _zoneDao.findById(zoneId);
        String zoneName;
        if (zone != null) {
            zoneName = zone.getName();
        } else {
            zoneName = String.valueOf(zoneId);
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneId.toString());
        params.put("name", "TungstenDevice - " + cmd.getName());
        params.put("hostname", cmd.getHostname());
        params.put("port", cmd.getPort());
        params.put("gateway", cmd.getGateway());
        params.put("vrouterPort", vrouterPort);
        params.put("introspectPort", introspectPort);
        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            tungstenResource.configure(cmd.getHostname(), hostdetails);
            final Host host = _resourceMgr.addHost(zoneId, tungstenResource, Host.Type.L2Networking, params);
            if (host != null) {
                tungstenProvider = Transaction.execute(new TransactionCallback<TungstenProviderVO>() {
                    @Override
                    public TungstenProviderVO doInTransaction(TransactionStatus status) {
                        TungstenProviderVO tungstenProviderVO = new TungstenProviderVO(zoneId, name, host.getId(), port,
                            hostname, gateway, vrouterPort, introspectPort);
                        _tungstenProviderDao.persist(tungstenProviderVO);

                        DetailVO detail = new DetailVO(host.getId(), "tungstendeviceid",
                            String.valueOf(tungstenProviderVO.getId()));
                        _hostDetailsDao.persist(detail);

                        return tungstenProviderVO;
                    }
                });
                _messageBus.publish(_name, TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT,
                    PublishScope.LOCAL, tungstenProvider);
            } else {
                throw new CloudRuntimeException("Failed to add Tungsten provider due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return tungstenProvider;
    }

    @Override
    public TungstenProviderResponse getTungstenProvider(long zoneId) {
        TungstenProviderVO tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
        if (tungstenProvider != null)
            return createTungstenProviderResponse(tungstenProvider);
        else
            return null;
    }

    public TungstenProviderResponse createTungstenProviderResponse(TungstenProviderVO tungstenProviderVO) {
        TungstenProviderResponse tungstenProviderResponse = new TungstenProviderResponse();
        tungstenProviderResponse.setHostname(tungstenProviderVO.getHostname());
        tungstenProviderResponse.setName(tungstenProviderVO.getProviderName());
        tungstenProviderResponse.setPort(tungstenProviderVO.getPort());
        tungstenProviderResponse.setUuid(tungstenProviderVO.getUuid());
        tungstenProviderResponse.setGateway(tungstenProviderVO.getGateway());
        tungstenProviderResponse.setIntrospectPort(tungstenProviderVO.getIntrospectPort());
        tungstenProviderResponse.setVrouterPort(tungstenProviderVO.getVrouterPort());
        tungstenProviderResponse.setObjectName("tungstenProvider");
        return tungstenProviderResponse;
    }


}
