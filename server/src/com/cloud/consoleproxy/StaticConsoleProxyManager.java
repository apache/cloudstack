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
import java.util.Random;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.keystore.KeystoreDao;
import com.cloud.keystore.KeystoreManager;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.NumbersUtil;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = {ConsoleProxyManager.class})
public class StaticConsoleProxyManager extends AgentBasedConsoleProxyManager implements ConsoleProxyManager, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(StaticConsoleProxyManager.class);

    @Inject
    ConsoleProxyDao _proxyDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    private VMInstanceDao _instanceDao;
    @Inject
    KeystoreDao _ksDao;
    @Inject
    private KeystoreManager _ksMgr;
    @Inject
    private HostDao _hostDao;
    private final Random _random = new Random(System.currentTimeMillis());
    private String _hashKey;
    private String _ip = null;

    public StaticConsoleProxyManager() {

    }

    @Override
    protected HostVO findHost(VMInstanceVO vm) {

        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByType(Type.ConsoleProxy, vm.getDataCenterId());

        return hosts.isEmpty() ? null : hosts.get(0);
    }

    @Override
    public ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId) {
        return new ConsoleProxyInfo(_sslEnabled, _ip, _consoleProxyPort, _consoleProxyUrlPort, _consoleProxyUrlDomain);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _ip = _configDao.getValue("consoleproxy.static.publicIp");
        if (_ip == null) {
            _ip = "127.0.0.1";
        }

        String value = (String)params.get("consoleproxy.sslEnabled");
        if (value != null && value.equalsIgnoreCase("true")) {
            _sslEnabled = true;
        }
        int defaultPort = 8088;
        if (_sslEnabled)
            defaultPort = 8443;
        _consoleProxyUrlPort = NumbersUtil.parseInt(_configDao.getValue("consoleproxy.static.port"), defaultPort);

        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);

        return true;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        if (!(cmd[0] instanceof StartupProxyCommand)) {
            return null;
        }

        host.setType(com.cloud.host.Host.Type.ConsoleProxy);
        return host;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        return null;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return null;
    }

}
