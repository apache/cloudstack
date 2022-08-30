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

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.consoleproxy.ConsoleAccessManager;
import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.security.keys.KeysManager;
import org.apache.cloudstack.framework.security.keystore.KeystoreManager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.server.ManagementServer;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class AgentBasedConsoleProxyManager extends ManagerBase implements ConsoleProxyManager {
    private static final Logger s_logger = Logger.getLogger(AgentBasedConsoleProxyManager.class);

    @Inject
    protected HostDao _hostDao;
    @Inject
    protected UserVmDao _userVmDao;
    protected String _consoleProxyUrlDomain;
    @Inject
    private VMInstanceDao _instanceDao;
    private ConsoleProxyListener _listener;
    protected int _consoleProxyUrlPort = ConsoleProxyManager.DEFAULT_PROXY_URL_PORT;
    protected int _consoleProxyPort = ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT;
    protected boolean _sslEnabled = false;
    @Inject
    AgentManager _agentMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    protected ConsoleProxyDao _cpDao;
    @Inject
    protected KeystoreManager _ksMgr;
    @Inject
    protected ConsoleAccessManager consoleAccessManager;

    @Inject
    ConfigurationDao _configDao;
    @Inject
    ManagementServer _ms;
    @Inject
    KeysManager _keysMgr;

    public class AgentBasedAgentHook extends AgentHookBase {

        public AgentBasedAgentHook(VMInstanceDao instanceDao, HostDao hostDao, ConfigurationDao cfgDao, KeystoreManager ksMgr,
                                   AgentManager agentMgr, KeysManager keysMgr, ConsoleAccessManager consoleAccessManager) {
            super(instanceDao, hostDao, cfgDao, ksMgr, agentMgr, keysMgr, consoleAccessManager);
        }

        @Override
        protected HostVO findConsoleProxyHost(StartupProxyCommand cmd) {
            return _hostDao.findByGuid(cmd.getGuid());
        }

    }

    public int getVncPort(VMInstanceVO vm) {
        if (vm.getHostId() == null) {
            return -1;
        }
        GetVncPortAnswer answer = (GetVncPortAnswer)_agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getHostName()));
        return (answer == null || !answer.getResult()) ? -1 : answer.getPort();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring AgentBasedConsoleProxyManager");
        }

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);
        String value = configs.get("consoleproxy.url.port");
        if (value != null) {
            _consoleProxyUrlPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_URL_PORT);
        }

        value = configs.get("consoleproxy.port");
        if (value != null) {
            _consoleProxyPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_VNC_PORT);
        }

        value = configs.get("consoleproxy.sslEnabled");
        if (value != null && value.equalsIgnoreCase("true")) {
            _sslEnabled = true;
        }

        _consoleProxyUrlDomain = configs.get("consoleproxy.url.domain");

        _listener = new ConsoleProxyListener(new AgentBasedAgentHook(_instanceDao, _hostDao, _configDao, _ksMgr,
                _agentMgr, _keysMgr, consoleAccessManager));
        _agentMgr.registerForHostEvents(_listener, true, true, false);

        if (s_logger.isInfoEnabled()) {
            s_logger.info("AgentBasedConsoleProxyManager has been configured. SSL enabled: " + _sslEnabled);
        }
        return true;
    }

    HostVO findHost(VMInstanceVO vm) {
        return _hostDao.findById(vm.getHostId());
    }

    @Override
    public ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId) {
        UserVmVO userVm = _userVmDao.findById(userVmId);
        if (userVm == null) {
            s_logger.warn("User VM " + userVmId + " no longer exists, return a null proxy for user vm:" + userVmId);
            return null;
        }

        HostVO host = findHost(userVm);
        if (host != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Assign embedded console proxy running at " + host.getName() + " to user vm " + userVmId + " with public IP " + host.getPublicIpAddress());
            }

            // only private IP, public IP, host id have meaningful values, rest
            // of all are place-holder values
            String publicIp = host.getPublicIpAddress();
            if (publicIp == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host " + host.getName() + "/" + host.getPrivateIpAddress() +
                        " does not have public interface, we will return its private IP for cosole proxy.");
                }
                publicIp = host.getPrivateIpAddress();
            }

            int urlPort = _consoleProxyUrlPort;

            if (host.getProxyPort() != null && host.getProxyPort().intValue() > 0) {
                urlPort = host.getProxyPort().intValue();
            }

            return new ConsoleProxyInfo(_sslEnabled, publicIp, _consoleProxyPort, urlPort, _consoleProxyUrlDomain);
        } else {
            s_logger.warn("Host that VM is running is no longer available, console access to VM " + userVmId + " will be temporarily unavailable.");
        }
        return null;
    }

    @Override
    public ConsoleProxyVO startProxy(long proxyVmId, boolean ignoreRestartSetting) {
        return null;
    }

    @Override
    public boolean destroyProxy(long proxyVmId) {
        return false;
    }

    @Override
    public int getVncPort() {
        return _consoleProxyPort;
    }

    @Override
    public boolean rebootProxy(long proxyVmId) {
        return false;
    }

    @Override
    public boolean stopProxy(long proxyVmId) {
        return false;
    }

    @Override
    public void setManagementState(ConsoleProxyManagementState state) {
    }

    @Override
    public ConsoleProxyManagementState getManagementState() {
        return null;
    }

    @Override
    public void resumeLastManagementState() {
    }

    @Override
    public String getName() {
        return _name;
    }
}
