//
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
//

package com.cloud.network.resource;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import com.google.common.base.Strings;

import net.nuage.vsp.acs.client.api.NuageVspAclClient;
import net.nuage.vsp.acs.client.api.NuageVspApiClient;
import net.nuage.vsp.acs.client.api.NuageVspElementClient;
import net.nuage.vsp.acs.client.api.NuageVspGuruClient;
import net.nuage.vsp.acs.client.api.NuageVspManagerClient;
import net.nuage.vsp.acs.client.api.NuageVspPluginClientLoader;
import net.nuage.vsp.acs.client.api.model.VspHost;
import net.nuage.vsp.acs.client.common.RequestType;
import net.nuage.vsp.acs.client.common.model.NuageVspEntity;
import net.nuage.vsp.acs.client.exception.NuageVspException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingNuageVspCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupVspCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;

public class NuageVspResource extends ManagerBase implements ServerResource, VspStatisticsMBean {
    private static final Logger s_logger = Logger.getLogger(NuageVspResource.class);

    private String _guid;
    private String _zoneId;
    private String _hostName;
    private boolean _shouldAudit = true;

    private VspHost _vspHost;

    private static final String NUAGE_VSP_PLUGIN_ERROR_MESSAGE = "Nuage Vsp plugin client is not installed";
    protected NuageVspPluginClientLoader _clientLoader;

    public VspHost validate(Map<String, ?> params) throws ConfigurationException {
        return validate(NuageVspResourceConfiguration.fromConfiguration(params));
    }

    public VspHost validate(NuageVspResourceConfiguration configuration) throws ConfigurationException {
        configuration.validate();

        VspHost newVspHost = configuration.buildVspHost();


        if (!newVspHost.getApiVersion().isSupported()) {
            s_logger.warn(String.format("[UPGRADE] API version %s of Nuage Vsp Device %s should be updated.", newVspHost.getApiVersion(), configuration.hostName()));
        }

        _guid = configuration.guid();
        _zoneId = configuration.zoneId();
        _hostName = configuration.hostName();
        _name = configuration.name();

        try {
            final NuageVspPluginClientLoader clientLoader = getClientLoader(newVspHost);
            clientLoader.getNuageVspApiClient().login();

            _vspHost = newVspHost;
            _clientLoader = clientLoader;
        } catch (NuageVspException e) {
            s_logger.error(e.getMessage(), e);
            throw new CloudRuntimeException(e.getMessage(), e);
        }

        return _vspHost;
    }

    @Override
    public long getVSDStatistics() {
        return _clientLoader.getNuageVspStatistics().getVsdCount();
    }


    @Override
    public long getVsdStatisticsByEntityType(String entity) {
        try {
            NuageVspEntity nuageVspEntity = NuageVspEntity.valueOf(entity);
            return _clientLoader.getNuageVspStatistics().getVsdCount(nuageVspEntity);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    @Override
    public long getVsdStatisticsByRequestType(String requestType) {
        try {
            RequestType requestTypeValue = RequestType.valueOf(requestType);
            return _clientLoader.getNuageVspStatistics().getVsdCount(requestTypeValue);
        } catch (IllegalArgumentException e) {
            return -1;
        }

    }

    @Override
    public long getVsdStatisticsByEntityAndRequestType(String entity, String requestType) {
        try {
            RequestType requestTypeValue = RequestType.valueOf(requestType);
            NuageVspEntity nuageVspEntity = NuageVspEntity.valueOf(entity);
            return _clientLoader.getNuageVspStatistics().getVsdCount(nuageVspEntity, requestTypeValue);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    private static Map<String, AtomicLong> convertHashMap(Map<RequestType, AtomicLong> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue)
                );
    }

    @Override
    public Map<String, Map<String, AtomicLong>> getVsdStatisticsReport() {
       return _clientLoader.getNuageVspStatistics()
                .getVsdCountReport()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> convertHashMap(e.getValue())
                ));
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if(super.configure(name, params)) {
            validate(params);
        }
        return true;
    }

    protected void login() throws ConfigurationException, NuageVspException {
        getNuageVspApiClient().login();
    }

    protected NuageVspPluginClientLoader getClientLoader(VspHost vspHost) {
        return NuageVspPluginClientLoader.getClientLoader(vspHost);
    }

    @Override
    public boolean start() {
        try {
            JmxUtil.registerMBean("NuageVspResource", _name, new VspStatisticsMBeanImpl(this));
        } catch (Exception e) {
            s_logger.warn("Unable to initialize statistics Mbean", e);
        }

        return true;

    }

    @Override
    public boolean stop() {

        try {
            JmxUtil.unregisterMBean("NuageVspResource", _name);
        } catch (Exception e) {
            s_logger.warn("Unable to stop NuageVspResource", e);
        }

        return true;
    }

    @Override
    public Host.Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupVspCommand sc = new StartupVspCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(NuageVspResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if (Strings.isNullOrEmpty(_vspHost.getRestRelativePath())) {
            s_logger.error("Refusing to ping Nuage VSD because the resource configuration is missing the relative path information");
            _shouldAudit = true;
            return null;
        }

        if (Strings.isNullOrEmpty(_vspHost.getCmsUserLogin()) || Strings.isNullOrEmpty(_vspHost.getCmsUserPassword())) {
            s_logger.error("Refusing to ping Nuage VSD because the resource configuration is missing the CMS user information");
            _shouldAudit = true;
            return null;
        }

        try {
            login();
        } catch (NuageVspException | ConfigurationException e) {
            s_logger.error("Failed to ping to Nuage VSD on " + _name + " as user " +_vspHost.getCmsUserLogin(), e);
            _shouldAudit = true;
            return null;
        }
        PingNuageVspCommand pingNuageVspCommand = new PingNuageVspCommand(Host.Type.L2Networking, id, _shouldAudit);
        _shouldAudit = false;
        return pingNuageVspCommand;
    }

    public boolean getStatus() {
        try {
            login();
            return true;
        } catch (NuageVspException | ConfigurationException e) {
            s_logger.error("Failed to ping to Nuage VSD on " + _name + " as user " +_vspHost.getCmsUserLogin(), e);
            return false;
        }
    }

    @Override
    public Answer executeRequest(final Command cmd) {
        final NuageVspRequestWrapper wrapper = NuageVspRequestWrapper.getInstance();
        try {
            return wrapper.execute(cmd, this);
        } catch (final Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Received unsupported command " + cmd.toString(), e);
            }
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
    }

    protected void assertNuageVspClientsLoaded() throws ConfigurationException {
        if (_clientLoader == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    public NuageVspApiClient getNuageVspApiClient() throws ConfigurationException {
        assertNuageVspClientsLoaded();
        return _clientLoader.getNuageVspApiClient();

    }

    public NuageVspGuruClient getNuageVspGuruClient() throws ConfigurationException {
        assertNuageVspClientsLoaded();
        return _clientLoader.getNuageVspGuruClient();
    }

    public NuageVspAclClient getNuageVspAclClient() throws ConfigurationException {
        assertNuageVspClientsLoaded();
        return _clientLoader.getNuageVspAclClient();
    }

    public NuageVspElementClient getNuageVspElementClient() throws ConfigurationException {
        assertNuageVspClientsLoaded();
        return _clientLoader.getNuageVspElementClient();
    }

    public NuageVspManagerClient getNuageVspManagerClient() throws ConfigurationException {
        assertNuageVspClientsLoaded();
        return _clientLoader.getNuageVspManagerClient();
    }

}
