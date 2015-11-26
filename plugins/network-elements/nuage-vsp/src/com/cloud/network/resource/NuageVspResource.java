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

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingNuageVspCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupVspCommand;
import com.cloud.agent.api.VspResourceAnswer;
import com.cloud.agent.api.VspResourceCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ImplementVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.manager.GetClientDefaultsAnswer;
import com.cloud.agent.api.manager.GetClientDefaultsCommand;
import com.cloud.agent.api.manager.SupportedApiVersionCommand;
import com.cloud.agent.api.sync.SyncDomainAnswer;
import com.cloud.agent.api.sync.SyncDomainCommand;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdAnswer;
import com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand;
import com.cloud.agent.api.sync.SyncVspCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.util.NuageVspUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;
import net.nuage.vsp.acs.NuageVspPluginClientLoader;
import net.nuage.vsp.acs.client.NuageVspApiClient;
import net.nuage.vsp.acs.client.NuageVspElementClient;
import net.nuage.vsp.acs.client.NuageVspGuruClient;
import net.nuage.vsp.acs.client.NuageVspManagerClient;
import net.nuage.vsp.acs.client.NuageVspSyncClient;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.cloud.agent.api.sync.SyncNuageVspCmsIdCommand.SyncType;

public class NuageVspResource extends ManagerBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(NuageVspResource.class);

    private static final String NAME = "name";
    private static final String GUID = "guid";
    private static final String ZONE_ID = "zoneid";
    private static final String HOST_NAME = "hostname";
    private static final String CMS_USER = "cmsuser";
    private static final String CMS_USER_PASSWORD = "cmsuserpass";
    private static final String PORT = "port";
    private static final String API_VERSION = "apiversion";
    private static final String API_RELATIVE_PATH = "apirelativepath";
    private static final String RETRY_COUNT = "retrycount";
    private static final String RETRY_INTERVAL = "retryinterval";
    private static final String NUAGE_VSP_CMS_ID = "nuagevspcmsid";

    private String _name;
    private String _guid;
    private String _zoneId;
    private String[] _cmsUserInfo;
    private String _hostName;
    private String _relativePath;
    private int _numRetries;
    private int _retryInterval;
    private String _nuageVspCmsId;
    private boolean _shouldAudit = true;

    protected NuageVspApiClient _nuageVspApiClient;
    protected NuageVspGuruClient _nuageVspGuruClient;
    protected NuageVspElementClient _nuageVspElementClient;
    protected NuageVspSyncClient _nuageVspSyncClient;
    protected NuageVspManagerClient _nuageVspManagerClient;
    protected boolean _isNuageVspClientLoaded;

    private static final String CMS_USER_ENTEPRISE_NAME = "CSP";
    private static final String NUAGE_VSP_PLUGIN_ERROR_MESSAGE = "Nuage Vsp plugin client is not installed";

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _name = (String)params.get(NAME);
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        _guid = (String)params.get(GUID);
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _zoneId = (String)params.get(ZONE_ID);
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        _hostName = (String)params.get(HOST_NAME);
        if (Strings.isNullOrEmpty(_hostName)) {
            throw new ConfigurationException("Unable to find hostname");
        }

        String cmsUser = (String)params.get(CMS_USER);
        if (Strings.isNullOrEmpty(cmsUser)) {
            throw new ConfigurationException("Unable to find CMS username");
        }

        String cmsUserPassBase64 = (String)params.get(CMS_USER_PASSWORD);
        if (Strings.isNullOrEmpty(cmsUserPassBase64)) {
            throw new ConfigurationException("Unable to find CMS password");
        }

        String port = (String)params.get(PORT);
        if (Strings.isNullOrEmpty(port)) {
            throw new ConfigurationException("Unable to find port");
        }

        String apiVersion = (String)params.get(API_VERSION);
        if (Strings.isNullOrEmpty(apiVersion)) {
            throw new ConfigurationException("Unable to find API version");
        } else if (!Pattern.matches("v\\d+_\\d+", apiVersion)) {
            throw new ConfigurationException("Incorrect API version");
        }

        String apiRelativePath = (String)params.get(API_RELATIVE_PATH);
        if (Strings.isNullOrEmpty(apiRelativePath) || !apiRelativePath.contains(apiVersion)) {
            throw new ConfigurationException("Unable to find API version in relative path");
        }

        String retryCount = (String)params.get(RETRY_COUNT);
        if (!Strings.isNullOrEmpty(retryCount)) {
            try {
                _numRetries = Integer.parseInt(retryCount);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Number of retries has to be between 1 and 10");
            }
            if ((_numRetries < 1) || (_numRetries > 10)) {
                throw new ConfigurationException("Number of retries has to be between 1 and 10");
            }
        } else {
            throw new ConfigurationException("Unable to find number of retries");
        }

        String retryInterval = (String)params.get(RETRY_INTERVAL);
        if (!Strings.isNullOrEmpty(retryInterval)) {
            try {
                _retryInterval = Integer.parseInt(retryInterval);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Retry interval has to be between 0 and 10000 ms");
            }
            if ((_retryInterval < 0) || (_retryInterval > 10000)) {
                throw new ConfigurationException("Retry interval has to be between 0 and 10000 ms");
            }
        } else {
            throw new ConfigurationException("Unable to find retry interval");
        }

        _relativePath = new StringBuffer().append("https://").append(_hostName).append(":").append(port).append(apiRelativePath).toString();

        String cmsUserPass = NuageVspUtil.decodePassword(cmsUserPassBase64);
        _cmsUserInfo = new String[] {CMS_USER_ENTEPRISE_NAME, cmsUser, cmsUserPass};

        _nuageVspCmsId = (String)params.get(NUAGE_VSP_CMS_ID);

        loadNuageClient();

        try {
            login();
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failed to login to Nuage VSD on " + name + " as user " + cmsUser, e);
            throw new CloudRuntimeException("Failed to login to Nuage VSD on " + name + " as user " + cmsUser, e);
        }

        return true;
    }

    protected void login() throws ConfigurationException, ExecutionException {
        isNuageVspApiLoaded();
        _nuageVspApiClient.login();
    }

    protected <A extends NuageVspApiClient, B extends NuageVspElementClient, C extends NuageVspSyncClient, D extends NuageVspGuruClient> void loadNuageClient() {

        try {
            NuageVspPluginClientLoader clientLoader = NuageVspPluginClientLoader.getClientLoader(_relativePath, _cmsUserInfo, _numRetries, _retryInterval, _nuageVspCmsId);
            _nuageVspApiClient = clientLoader.getNuageVspApiClient();
            _nuageVspSyncClient = clientLoader.getNuageVspSyncClient();
            _nuageVspGuruClient = clientLoader.getNuageVspGuruClient();
            _nuageVspElementClient = clientLoader.getNuageVspElementClient();
            _nuageVspManagerClient = clientLoader.getNuageVspManagerClient();
            _isNuageVspClientLoaded = true;
        } catch (ConfigurationException e) {
            _isNuageVspClientLoaded = false;
            String errorMessage = "Nuage Vsp Plugin client is not yet installed. Please install NuageVsp plugin client to use NuageVsp plugin in Cloudstack. ";
            s_logger.error(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        }

    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
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
        if (_relativePath == null || _relativePath.isEmpty()) {
            s_logger.error("Refusing to ping Nuage VSD because the resource configuration is missing the relative path information");
            _shouldAudit = true;
            return null;
        }
        if (_cmsUserInfo == null || _cmsUserInfo.length < 2) {
            s_logger.error("Refusing to ping Nuage VSD because the resource configuration is missing the CMS user information");
            _shouldAudit = true;
            return null;
        }
        try {
            login();
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failed to ping to Nuage VSD on " + _name + " as user " + _cmsUserInfo[1], e);
            _shouldAudit = true;
            return null;
        }
        PingNuageVspCommand pingNuageVspCommand = new PingNuageVspCommand(Host.Type.L2Networking, id, _shouldAudit);
        _shouldAudit = false;
        return pingNuageVspCommand;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        } else if (cmd instanceof VspResourceCommand) {
            return executeRequest((VspResourceCommand)cmd);
        }
        //Guru commands
        else if (cmd instanceof ImplementNetworkVspCommand) {
            return executeRequest((ImplementNetworkVspCommand)cmd);
        } else if (cmd instanceof ReserveVmInterfaceVspCommand) {
            return executeRequest((ReserveVmInterfaceVspCommand)cmd);
        } else if (cmd instanceof DeallocateVmVspCommand) {
            return executeRequest((DeallocateVmVspCommand)cmd);
        } else if (cmd instanceof TrashNetworkVspCommand) {
            return executeRequest((TrashNetworkVspCommand)cmd);
        }
        //Element commands
        else if (cmd instanceof ImplementVspCommand) {
            return executeRequest((ImplementVspCommand)cmd);
        } else if (cmd instanceof ApplyAclRuleVspCommand) {
            return executeRequest((ApplyAclRuleVspCommand)cmd);
        } else if (cmd instanceof ApplyStaticNatVspCommand) {
            return executeRequest((ApplyStaticNatVspCommand)cmd);
        } else if (cmd instanceof ShutDownVpcVspCommand) {
            return executeRequest((ShutDownVpcVspCommand)cmd);
        }
        //Sync Commands
        else if (cmd instanceof SyncVspCommand) {
            return executeRequest((SyncVspCommand)cmd);
        } else if (cmd instanceof SyncNuageVspCmsIdCommand) {
            return executeRequest((SyncNuageVspCmsIdCommand)cmd);
        } else if (cmd instanceof SyncDomainCommand) {
            return executeRequest((SyncDomainCommand)cmd);
        }
        //Other commands
        else if (cmd instanceof GetClientDefaultsCommand) {
            return executeRequest((GetClientDefaultsCommand)cmd);
        } else if (cmd instanceof SupportedApiVersionCommand) {
            return executeRequest((SupportedApiVersionCommand)cmd);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Received unsupported command " + cmd.toString());
        }
        return Answer.createUnsupportedCommandAnswer(cmd);
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

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer executeRequest(VspResourceCommand cmd) {
        try {
            isNuageVspApiLoaded();
            if (cmd.getRequestType().equalsIgnoreCase("GETALL") || cmd.getRequestType().equalsIgnoreCase("GET") || cmd.getRequestType().equalsIgnoreCase("GETRELATED")) {
                String resourceInfo = _nuageVspApiClient.executeRestApi(cmd.getRequestType(), cmd.getResource(), cmd.getResourceId(), cmd.getChildResource(),
                        cmd.getEntityDetails(), cmd.getResourceFilter(), cmd.getProxyUserUuid(), cmd.getProxyUserDomainuuid());
                return new VspResourceAnswer(cmd, resourceInfo, "Executed Issue Resource command");
            }
            return new VspResourceAnswer(cmd, false, cmd.getRequestType() + " is not yet supported");
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new VspResourceAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ImplementNetworkVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.implement(cmd.getNetworkDomainName(), cmd.getNetworkDomainPath(), cmd.getNetworkDomainUuid(), cmd.getNetworkAccountName(),
                    cmd.getNetworkAccountUuid(), cmd.getNetworkName(), cmd.getNetworkCidr(), cmd.getNetworkGateway(), cmd.getNetworkAclId(), cmd.getDnsServers(),
                    cmd.getGatewaySystemIds(), cmd.isL3Network(), cmd.isVpc(), cmd.isSharedNetwork(), cmd.getNetworkUuid(), cmd.getVpcName(), cmd.getVpcUuid(),
                    cmd.isDefaultEgressPolicy(), cmd.getIpAddressRange(), cmd.getDomainTemplateName());
            return new Answer(cmd, true, "Created network mapping to " + cmd.getNetworkName() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(ReserveVmInterfaceVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.reserve(cmd.getNicUuid(), cmd.getNicMacAddress(), cmd.getNetworkUuid(), cmd.isL3Network(),
                    cmd.isSharedNetwork(), cmd.getVpcUuid(), cmd.getNetworkDomainUuid(), cmd.getNetworksAccountUuid(), cmd.isDomainRouter(), cmd.getDomainRouterIp(),
                    cmd.getVmInstanceName(), cmd.getVmUuid(), cmd.useStaticIp(), cmd.getStaticIp(), cmd.getStaticNatIpUuid(), cmd.getStaticNatIpAddress(), cmd.isStaticNatIpAllocated(),
                    cmd.isOneToOneNat(), cmd.getStaticNatVlanUuid(), cmd.getStaticNatVlanGateway(), cmd.getStaticNatVlanNetmask());
            return new Answer(cmd, true, "Created NIC that maps to nicUuid" + cmd.getNicUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(DeallocateVmVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.deallocate(cmd.getNetworkUuid(), cmd.getNicFromDdUuid(), cmd.getNicMacAddress(), cmd.getNicIp4Address(), cmd.isL3Network(), cmd.isSharedNetwork(),
                    cmd.getVpcUuid(), cmd.getNetworksDomainUuid(), cmd.getVmInstanceName(), cmd.getVmUuid(), cmd.isExpungingState());
            return new Answer(cmd, true, "Deallocated VM " + cmd.getVmInstanceName() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(TrashNetworkVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.trash(cmd.getDomainUuid(), cmd.getNetworkUuid(), cmd.isL3Network(), cmd.isSharedNetwork(), cmd.getVpcUuid(), cmd.getDomainTemplateName());
            return new Answer(cmd, true, "Deleted network mapping to " + cmd.getNetworkUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(ApplyStaticNatVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.applyStaticNats(cmd.getNetworkDomainUuid(), cmd.getNetworkUuid(), cmd.getVpcOrSubnetUuid(), cmd.isL3Network(),
                    cmd.isVpc(), cmd.getStaticNatDetails());
            return new Answer(cmd, true, "Applied Static NAT to network mapping " + cmd.getVpcOrSubnetUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(ImplementVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            boolean success = _nuageVspElementClient.implement(cmd.getNetworkId(), cmd.getNetworkDomainUuid(), cmd.getNetworkUuid(), cmd.getNetworkName(), cmd.getVpcOrSubnetUuid(), cmd.isL2Network(),
                    cmd.isL3Network(), cmd.isVpc(), cmd.isShared(), cmd.getDomainTemplateName(), cmd.isFirewallServiceSupported(), cmd.getDnsServers(), cmd.getIngressFirewallRules(),
                    cmd.getEgressFirewallRules(), cmd.getAcsFipUuid(), cmd.isEgressDefaultPolicy());
            return new Answer(cmd, success, "Implemented network " + cmd.getNetworkUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(ApplyAclRuleVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.applyAclRules(cmd.isNetworkAcl(), cmd.getNetworkUuid(), cmd.getNetworkDomainUuid(), cmd.getVpcOrSubnetUuid(), cmd.getNetworkName(),
                    cmd.isL2Network(), cmd.getAclRules(), cmd.getNetworkId(), cmd.isEgressDefaultPolicy(), cmd.getAcsIngressAcl(), cmd.isNetworkReset(), cmd.getDomainTemplateName());
            return new Answer(cmd, true, "Applied ACL Rule to network mapping " + cmd.getVpcOrSubnetUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(ShutDownVpcVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.shutdownVpc(cmd.getDomainUuid(), cmd.getVpcUuid(), cmd.getDomainTemplateName());
            return new Answer(cmd, true, "Shutdown VPC " + cmd.getVpcUuid() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(SyncVspCommand cmd) {
        try {
            isNuageVspSyncLoaded();
            _nuageVspSyncClient.syncWithNuageVsp(cmd.getNuageVspEntity());
            return new Answer(cmd, true, "Synced " + cmd.getNuageVspEntity() + " on Nuage VSD " + _hostName);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    private Answer executeRequest(SyncNuageVspCmsIdCommand cmd) {
        try {
            isNuageVspManagerLoaded();
            if (cmd.getSyncType() == SyncType.AUDIT || cmd.getSyncType() == SyncType.AUDIT_ONLY) {
                Pair<Boolean, String> answer = _nuageVspManagerClient.auditNuageVspCmsId(cmd.getNuageVspCmsId(), cmd.getSyncType() == SyncType.AUDIT_ONLY);
                return new SyncNuageVspCmsIdAnswer(answer.getLeft(), answer.getRight(), cmd.getSyncType());
            } else if (cmd.getSyncType() == SyncType.REGISTER) {
                String registeredNuageVspCmsId = _nuageVspManagerClient.registerNuageVspCmsId();
                return new SyncNuageVspCmsIdAnswer(StringUtils.isNotBlank(registeredNuageVspCmsId), registeredNuageVspCmsId, cmd.getSyncType());
            } else {
                boolean success = _nuageVspManagerClient.unregisterNuageVspCmsId(cmd.getNuageVspCmsId());
                return new SyncNuageVspCmsIdAnswer(success, cmd.getNuageVspCmsId(), cmd.getSyncType());
            }
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new SyncNuageVspCmsIdAnswer(false, null, cmd.getSyncType());
        }
    }

    private Answer executeRequest(SyncDomainCommand cmd) {
        try {
            isNuageVspManagerLoaded();
            boolean success = _nuageVspManagerClient.syncDomainWithNuageVsp(cmd.getDomainUuid(), cmd.getDomainName(), cmd.getDomainPath(), cmd.isToAdd(), cmd.isToRemove());
            return new SyncDomainAnswer(success);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new SyncDomainAnswer(false);
        }
    }

    private Answer executeRequest(GetClientDefaultsCommand cmd) {
        try {
            isNuageVspManagerLoaded();
            Map<String, Object> clientDefaults = _nuageVspManagerClient.getClientDefaults();
            return new GetClientDefaultsAnswer(cmd, clientDefaults);
        } catch (ExecutionException | ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new GetClientDefaultsAnswer(cmd, e);
        }
    }

    private Answer executeRequest(SupportedApiVersionCommand cmd) {
        try {
            isNuageVspManagerLoaded();
            boolean supported = _nuageVspManagerClient.isSupportedApiVersion(cmd.getApiVersion());
            return new Answer(cmd, supported, "Check if API version " + cmd.getApiVersion() + " is supported");
        } catch (ConfigurationException e) {
            s_logger.error("Failure during " + cmd + " on Nuage VSD " + _hostName, e);
            return new Answer(cmd, e);
        }
    }

    protected void isNuageVspApiLoaded() throws ConfigurationException {
        if (!_isNuageVspClientLoaded || _nuageVspApiClient == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspGuruLoaded() throws ConfigurationException {
        if (!_isNuageVspClientLoaded || _nuageVspGuruClient == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspElementLoaded() throws ConfigurationException {
        if (!_isNuageVspClientLoaded || _nuageVspElementClient == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspSyncLoaded() throws ConfigurationException {
        if (!_isNuageVspClientLoaded || _nuageVspSyncClient == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspManagerLoaded() throws ConfigurationException {
        if (!_isNuageVspClientLoaded || _nuageVspManagerClient == null) {
            throw new ConfigurationException(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    public static class Configuration {
        private String _name;
        private String _guid;
        private String _zoneId;
        private String _hostName;
        private String _cmsUser;
        private String _cmsUserPassword;
        private String _port;
        private String _apiVersion;
        private String _apiRelativePath;
        private String _retryCount;
        private String _retryInterval;
        private String _nuageVspCmsId;

        public String name() {
            return this._name;
        }

        public Configuration name(String name) {
            this._name = name;
            return this;
        }

        public String guid() {
            return this._guid;
        }

        public Configuration guid(String guid) {
            this._guid = guid;
            return this;
        }

        public String zoneId() {
            return this._zoneId;
        }

        public Configuration zoneId(String zoneId) {
            this._zoneId = zoneId;
            return this;
        }

        public String hostName() {
            return this._hostName;
        }

        public Configuration hostName(String hostName) {
            this._hostName = hostName;
            return this;
        }

        public String cmsUser() {
            return this._cmsUser;
        }

        public Configuration cmsUser(String cmsUser) {
            this._cmsUser = cmsUser;
            return this;
        }

        public String cmsUserPassword() {
            return this._cmsUserPassword;
        }

        public Configuration cmsUserPassword(String cmsUserPassword) {
            this._cmsUserPassword = cmsUserPassword;
            return this;
        }

        public String port() {
            return this._port;
        }

        public Configuration port(String port) {
            this._port = port;
            return this;
        }

        public String apiVersion() {
            return this._apiVersion;
        }

        public Configuration apiVersion(String apiVersion) {
            this._apiVersion = apiVersion;
            return this;
        }

        public String apiRelativePath() {
            return this._apiRelativePath;
        }

        public Configuration apiRelativePath(String apiRelativePath) {
            this._apiRelativePath = apiRelativePath;
            return this;
        }

        public String retryCount() {
            return this._retryCount;
        }

        public Configuration retryCount(String retryCount) {
            this._retryCount = retryCount;
            return this;
        }

        public String retryInterval() {
            return this._retryInterval;
        }

        public Configuration retryInterval(String retryInterval) {
            this._retryInterval = retryInterval;
            return this;
        }

        public String nuageVspCmsId() {
            return this._nuageVspCmsId;
        }

        public Configuration nuageVspCmsId(String nuageVspCmsId) {
            this._nuageVspCmsId = nuageVspCmsId;
            return this;
        }

        public Map<String, String> build() {
            return new HashMap<String, String>() {{
                if (_name != null) put(NAME, _name);
                if (_guid != null) put(GUID, _guid);
                if (_zoneId != null) put(ZONE_ID, _zoneId);
                if (_hostName != null) put(HOST_NAME, _hostName);
                if (_cmsUser != null) put(CMS_USER, _cmsUser);
                if (_cmsUserPassword != null) put(CMS_USER_PASSWORD, _cmsUserPassword);
                if (_port != null) put(PORT, _port);
                if (_apiVersion != null) put(API_VERSION, _apiVersion);
                if (_apiRelativePath != null) put(API_RELATIVE_PATH, _apiRelativePath);
                if (_retryCount != null) put(RETRY_COUNT, _retryCount);
                if (_retryInterval != null) put(RETRY_INTERVAL, _retryInterval);
                if (_nuageVspCmsId != null) put(NUAGE_VSP_CMS_ID, _nuageVspCmsId);
            }};
        }

        public static Configuration fromConfiguration(Map<String, String> configuration) {
            return new Configuration()
                    .name(configuration.get(NAME))
                    .guid(configuration.get(GUID))
                    .zoneId(configuration.get(ZONE_ID))
                    .hostName(configuration.get(HOST_NAME))
                    .cmsUser(configuration.get(CMS_USER))
                    .cmsUserPassword(configuration.get(CMS_USER_PASSWORD))
                    .port(configuration.get(PORT))
                    .apiVersion(configuration.get(API_VERSION))
                    .apiRelativePath(configuration.get(API_RELATIVE_PATH))
                    .retryCount(configuration.get(RETRY_COUNT))
                    .retryInterval(configuration.get(RETRY_INTERVAL))
                    .nuageVspCmsId(configuration.get(NUAGE_VSP_CMS_ID));
        }
    }
}
