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

package org.apache.cloudstack.veeam;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.cache.SingleCache;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.veeam.utils.DataUtil;
import org.apache.commons.lang3.StringUtils;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.user.AccountService;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.net.NetUtils;

public class VeeamControlServiceImpl extends ManagerBase implements VeeamControlService {

    @Inject
    ManagementServerHostDao managementServerHostDao;

    @Inject
    AccountService accountService;

    private List<RouteHandler> routeHandlers;
    private VeeamControlServer veeamControlServer;
    private SingleCache<List<String>> allowedClientCidrsCache;

    protected List<String> getAllowedClientCidrsInternal() {
        String allowedClientCidrsStr = AllowedClientCidrs.value();
        if (StringUtils.isBlank(allowedClientCidrsStr)) {
            return Collections.emptyList();
        }
        List<String> allowedClientCidrs = List.of(allowedClientCidrsStr.split(","));
        // Sanitize and remove any incorrect CIDR entries
        allowedClientCidrs = allowedClientCidrs.stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(cidr -> {
                    boolean valid = NetUtils.isValidIp4Cidr(cidr);
                    if (!valid) {
                        logger.warn("Invalid CIDR entry '{}' in allowed client CIDRs, ignoring", cidr);
                    }
                    return valid;
                }).collect(Collectors.toList());
        return allowedClientCidrs;
    }

    public List<RouteHandler> getRouteHandlers() {
        return routeHandlers;
    }

    public void setRouteHandlers(final List<RouteHandler> routeHandlers) {
        this.routeHandlers = routeHandlers;
    }

    @Override
    public long getCurrentManagementServerHostId() {
        ManagementServerHostVO hostVO =
                managementServerHostDao.findByMsid(ManagementServerNode.getManagementServerId());
        return hostVO.getId();
    }

    @Override
    public List<String> getAllowedClientCidrs() {
        return allowedClientCidrsCache.get();
    }

    @Override
    public String getInstanceId() {
        return accountService.getSystemAccount().getUuid();
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        return DataUtil.constantTimeEquals(Username.value(), username) &&
                DataUtil.constantTimeEquals(Password.value(), password);
    }

    @Override
    public String getHmacSecret() {
        String base = getInstanceId() + ":" + Port.value() + ":" + Username.value() + Password.value();
        return UuidUtils.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Override
    public boolean start() {
        allowedClientCidrsCache = new SingleCache<>(30, this::getAllowedClientCidrsInternal);
        veeamControlServer = new VeeamControlServer(getRouteHandlers(), this);
        try {
            veeamControlServer.startIfEnabled();
        } catch (Exception e) {
            logger.error("Failed to start Veeam Control API server, continuing without it", e);
        }
        return true;
    }

    @Override
    public boolean stop() {
        try {
            veeamControlServer.stop();
        } catch (Exception e) {
            logger.error("Failed to stop Veeam Control API server cleanly", e);
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of();
    }

    @Override
    public String getConfigComponentName() {
        return VeeamControlService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                Enabled,
                BindAddress,
                Port,
                ContextPath,
                Username,
                Password,
                ServiceAccountId,
                InstanceRestoreAssignOwner,
                AllowedClientCidrs,
                DeveloperLogs
        };
    }
}
