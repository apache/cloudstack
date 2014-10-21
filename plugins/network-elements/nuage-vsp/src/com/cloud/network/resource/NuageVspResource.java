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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

import net.nuage.vsp.acs.NuageVspPluginClientLoader;
import net.nuage.vsp.acs.client.NuageVspApiClient;
import net.nuage.vsp.acs.client.NuageVspElementClient;
import net.nuage.vsp.acs.client.NuageVspGuruClient;
import net.nuage.vsp.acs.client.NuageVspSyncClient;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupVspCommand;
import com.cloud.agent.api.VspResourceAnswer;
import com.cloud.agent.api.VspResourceCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspAnswer;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspAnswer;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspAnswer;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspAnswer;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspAnswer;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReleaseVmVspAnswer;
import com.cloud.agent.api.guru.ReleaseVmVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspAnswer;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspAnswer;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.sync.SyncVspAnswer;
import com.cloud.agent.api.sync.SyncVspCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class NuageVspResource extends ManagerBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(NuageVspResource.class);

    private String _name;
    private String _guid;
    private String _zoneId;
    private String[] _cmsUserInfo;
    private String _relativePath;
    private int _numRetries;
    private int _retryInterval;

    protected NuageVspApiClient _nuageVspApiClient;
    protected NuageVspGuruClient _nuageVspGuruClient;
    protected NuageVspElementClient _nuageVspElementClient;
    protected NuageVspSyncClient _nuageVspSyncClient;
    protected boolean _isNuageVspClientLoaded;

    private static final String CMS_USER_ENTEPRISE_NAME = "CSP";
    private static final String NUAGE_VSP_PLUGIN_ERROR_MESSAGE = "Nuage Vsp plugin client is not installed";
    private static final String NUAGE_PLUGIN_CLIENT_JAR_FILE = "/usr/share/nuagevsp/lib/nuage-vsp-acs-client.jar";
    private static final String NUAGE_VSP_API_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspApiClientImpl";
    private static final String NUAGE_VSP_SYNC_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspSyncClientImpl";
    private static final String NUAGE_VSP_ELEMENT_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspElementClientImpl";
    private static final String NUAGE_VSP_GURU_CLIENT_IMPL = "net.nuage.vsp.acs.client.impl.NuageVspGuruClientImpl";

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _name = (String)params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _zoneId = (String)params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        String hostname = (String)params.get("hostname");
        if (hostname == null) {
            throw new ConfigurationException("Unable to find hostname");
        }

        String cmsUser = (String)params.get("cmsuser");
        if (cmsUser == null) {
            throw new ConfigurationException("Unable to find CMS username");
        }

        String cmsUserPassBase64 = (String)params.get("cmsuserpass");
        if (cmsUserPassBase64 == null) {
            throw new ConfigurationException("Unable to find CMS password");
        }

        String port = (String)params.get("port");
        if (port == null) {
            throw new ConfigurationException("Unable to find port");
        }

        String apiRelativePath = (String)params.get("apirelativepath");
        if ((apiRelativePath != null) && (!apiRelativePath.isEmpty())) {
            String apiVersion = apiRelativePath.substring(apiRelativePath.lastIndexOf('/') + 1);
            if (!Pattern.matches("v\\d+_\\d+", apiVersion)) {
                throw new ConfigurationException("Incorrect API version");
            }
        } else {
            throw new ConfigurationException("Unable to find API version");
        }

        String retryCount = (String)params.get("retrycount");
        if ((retryCount != null) && (!retryCount.isEmpty())) {
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

        String retryInterval = (String)params.get("retryinterval");
        if ((retryInterval != null) && (!retryInterval.isEmpty())) {
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

        _relativePath = new StringBuffer().append("https://").append(hostname).append(":").append(port).append(apiRelativePath).toString();

        String cmsUserPass = org.apache.commons.codec.binary.StringUtils.newStringUtf8(Base64.decodeBase64(cmsUserPassBase64));
        _cmsUserInfo = new String[] {CMS_USER_ENTEPRISE_NAME, cmsUser, cmsUserPass};

        try {
            loadNuageClient();
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to login to Nuage VSD on " + name + " as user " + cmsUser, e);
        }

        try {
            login();
        } catch (Exception e) {
            s_logger.error("Failed to login to Nuage VSD on " + name + " as user " + cmsUser + " Exception " + e.getMessage());
            throw new CloudRuntimeException("Failed to login to Nuage VSD on " + name + " as user " + cmsUser, e);
        }

        return true;
    }

    protected void login() throws Exception {
        isNuageVspApiLoaded();
        _nuageVspApiClient.login();
    }

    protected <A extends NuageVspApiClient, B extends NuageVspElementClient, C extends NuageVspSyncClient, D extends NuageVspGuruClient> void loadNuageClient() throws Exception {

        try {
            ClassLoader loader = NuageVspPluginClientLoader.getClassLoader(NUAGE_PLUGIN_CLIENT_JAR_FILE);

            Class<?> nuageVspApiClientClass = Class.forName(NUAGE_VSP_API_CLIENT_IMPL, true, loader);
            Class<?> nuageVspSyncClientClass = Class.forName(NUAGE_VSP_SYNC_CLIENT_IMPL, true, loader);
            Class<?> nuageVspGuruClientClass = Class.forName(NUAGE_VSP_GURU_CLIENT_IMPL, true, loader);
            Class<?> nuageVspElementClientClass = Class.forName(NUAGE_VSP_ELEMENT_CLIENT_IMPL, true, loader);

            //Instantiate the instances
            _nuageVspApiClient = (NuageVspApiClient)nuageVspApiClientClass.newInstance();
            _nuageVspApiClient.setNuageVspHost(_relativePath, _cmsUserInfo, _numRetries, _retryInterval);
            _nuageVspSyncClient = (NuageVspSyncClient)nuageVspSyncClientClass.newInstance();
            _nuageVspSyncClient.setNuageVspApiClient(_nuageVspApiClient);
            _nuageVspGuruClient = (NuageVspGuruClient)nuageVspGuruClientClass.newInstance();
            _nuageVspGuruClient.setNuageVspApiClient(_nuageVspApiClient);
            _nuageVspElementClient = (NuageVspElementClient)nuageVspElementClientClass.newInstance();
            _nuageVspElementClient.setNuageVspApiClient(_nuageVspApiClient);
            _isNuageVspClientLoaded = true;
        } catch (Exception e) {
            _isNuageVspClientLoaded = false;
            String errorMessage = "Nuage Vsp Plugin client is not yet installed. Please install NuageVsp plugin client to use NuageVsp plugin in Cloudstack. ";
            s_logger.warn(errorMessage + e.getMessage());
            throw new Exception(errorMessage);
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
        if ((_relativePath == null) || (_relativePath.isEmpty()) || (_cmsUserInfo == null) || (_cmsUserInfo.length == 0)) {
            s_logger.error("Failed to ping to Nuage VSD");
            return null;
        }
        try {
            login();
        } catch (Exception e) {
            s_logger.error("Failed to ping to Nuage VSD on " + _name + " as user " + _cmsUserInfo[1] + " Exception " + e.getMessage());
            return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
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
        } else if (cmd instanceof ReleaseVmVspCommand) {
            return executeRequest((ReleaseVmVspCommand)cmd);
        } else if (cmd instanceof DeallocateVmVspCommand) {
            return executeRequest((DeallocateVmVspCommand)cmd);
        } else if (cmd instanceof TrashNetworkVspCommand) {
            return executeRequest((TrashNetworkVspCommand)cmd);
        }
        //Element commands
        else if (cmd instanceof ApplyAclRuleVspCommand) {
            return executeRequest((ApplyAclRuleVspCommand)cmd);
        } else if (cmd instanceof ApplyStaticNatVspCommand) {
            return executeRequest((ApplyStaticNatVspCommand)cmd);
        } else if (cmd instanceof ShutDownVpcVspCommand) {
            return executeRequest((ShutDownVpcVspCommand)cmd);
        }
        //Sync Commands
        else if (cmd instanceof SyncVspCommand) {
            return executeRequest((SyncVspCommand)cmd);
        }
        s_logger.debug("Received unsupported command " + cmd.toString());
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
        } catch (Exception e) {
            return new VspResourceAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ImplementNetworkVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.implement(cmd.getNetworkDomainName(), cmd.getNetworkDomainPath(), cmd.getNetworkDomainUuid(), cmd.getNetworkAccountName(),
                    cmd.getNetworkAccountUuid(), cmd.getNetworkName(), cmd.getNetworkCidr(), cmd.getNetworkGateway(), cmd.getNetworkUuid(), cmd.isL3Network(), cmd.getVpcName(),
                    cmd.getVpcUuid(), cmd.isDefaultEgressPolicy(), cmd.getIpAddressRange());
            return new ImplementNetworkVspAnswer(cmd, true, "Created Nuage VSP network mapping to " + cmd.getNetworkName());
        } catch (Exception e) {
            return new ImplementNetworkVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ReserveVmInterfaceVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            List<Map<String, String>> vmInterfaceInfo = _nuageVspGuruClient.reserve(cmd.getNicUuid(), cmd.getNicMacAddress(), cmd.getNetworkUuid(), cmd.isL3Network(),
                    cmd.getVpcUuid(), cmd.getNetworkDomainUuid(), cmd.getNetworksAccountUuid(), cmd.isDomainRouter(), cmd._getDomainRouterIp(), cmd._getVmInstanceName(),
                    cmd._getVmUuid());
            return new ReserveVmInterfaceVspAnswer(cmd, vmInterfaceInfo, "Created NIC in VSP that maps to nicUuid" + cmd.getNicUuid());
        } catch (Exception e) {
            return new ReserveVmInterfaceVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ReleaseVmVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.release(cmd.getNetworkUuid(), cmd.getVmUuid(), cmd.getVmInstanceName());
            return new ReleaseVmVspAnswer(cmd, true, "VM has been deleted from VSP.");
        } catch (Exception e) {
            return new ReleaseVmVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(DeallocateVmVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.deallocate(cmd.getNetworkUuid(), cmd.getNicFrmDdUuid(), cmd.getNicMacAddress(), cmd.getNicIp4Address(), cmd.isL3Network(), cmd.getVpcUuid(),
                    cmd.getNetworksDomainUuid(), cmd.getVmInstanceName(), cmd.getVmUuid());
            return new DeallocateVmVspAnswer(cmd, true, "Deallocated VM from Nuage VSP.");
        } catch (Exception e) {
            return new DeallocateVmVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(TrashNetworkVspCommand cmd) {
        try {
            isNuageVspGuruLoaded();
            _nuageVspGuruClient.trash(cmd.getDomainUuid(), cmd.getNetworkUuid(), cmd.isL3Network(), cmd.getVpcUuid());
            return new TrashNetworkVspAnswer(cmd, true, "Deleted Nuage VSP network mapping to " + cmd.getNetworkUuid());
        } catch (Exception e) {
            return new TrashNetworkVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ApplyStaticNatVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.applyStaticNats(cmd.getNetworkDomainUuid(), cmd.getVpcOrSubnetUuid(), cmd.isL3Network(), cmd.getStaticNatDetails());
            return new ApplyStaticNatVspAnswer(cmd, true, "Applied Static NAT to VSP network mapping to " + cmd.getVpcOrSubnetUuid());
        } catch (Exception e) {
            return new ApplyStaticNatVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ApplyAclRuleVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.applyAclRules(cmd.getNetworkUuid(), cmd.getNetworkDomainUuid(), cmd.getVpcOrSubnetUuid(), cmd.isL3Network(), cmd.getAclRules(), cmd.isVpc(),
                    cmd.getNetworkId());
            return new ApplyAclRuleVspAnswer(cmd, true, "Applied ACL Rule to VSP network mapping to " + cmd.getVpcOrSubnetUuid());
        } catch (Exception e) {
            return new ApplyAclRuleVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(ShutDownVpcVspCommand cmd) {
        try {
            isNuageVspElementLoaded();
            _nuageVspElementClient.shutDownVpc(cmd.getDomainUuid(), cmd.getVpcUuid());
            return new ShutDownVpcVspAnswer(cmd, true, "Shutdown VPC " + cmd.getVpcUuid());
        } catch (Exception e) {
            return new ShutDownVpcVspAnswer(cmd, e);
        }
    }

    private Answer executeRequest(SyncVspCommand cmd) {
        try {
            isNuageVspSyncLoaded();
            _nuageVspSyncClient.syncWithNuageVsp(cmd.getNuageVspEntity());
            return new SyncVspAnswer(cmd, true, "Synced " + cmd.getNuageVspEntity() + " in VSP");
        } catch (Exception e) {
            return new SyncVspAnswer(cmd, e);
        }
    }

    protected void isNuageVspApiLoaded() throws Exception {
        if (!_isNuageVspClientLoaded || _nuageVspApiClient == null) {
            throw new Exception(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspGuruLoaded() throws Exception {
        if (!_isNuageVspClientLoaded || _nuageVspGuruClient == null) {
            throw new Exception(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspElementLoaded() throws Exception {
        if (!_isNuageVspClientLoaded || _nuageVspElementClient == null) {
            throw new Exception(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }

    protected void isNuageVspSyncLoaded() throws Exception {
        if (!_isNuageVspClientLoaded || _nuageVspSyncClient == null) {
            throw new Exception(NUAGE_VSP_PLUGIN_ERROR_MESSAGE);
        }
    }
}
