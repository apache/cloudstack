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
package com.cloud.consoleproxy;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.resource.ResourceManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@Local(value={ConsoleProxyManager.class})
public class StaticConsoleProxyManager extends AgentBasedConsoleProxyManager implements ConsoleProxyManager {
    String _ip = null;
    @Inject ConsoleProxyDao _proxyDao;
    @Inject ResourceManager _resourceMgr;
    @Inject ConfigurationDao _configDao;

    @Override
    protected HostVO findHost(VMInstanceVO vm) {

        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByType(Type.ConsoleProxy, vm.getDataCenterId());

        return hosts.isEmpty() ? null : hosts.get(0);
    }

    @Override
    public ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId) {
        return new ConsoleProxyInfo(false, _ip, _consoleProxyPort, _consoleProxyUrlPort, _consoleProxyUrlDomain);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> dbParams = _configDao.getConfiguration("ManagementServer", params);

        _ip = dbParams.get("public.ip");
        if (_ip == null) {
            _ip = "127.0.0.1";
        }

        return true;
    }
}
