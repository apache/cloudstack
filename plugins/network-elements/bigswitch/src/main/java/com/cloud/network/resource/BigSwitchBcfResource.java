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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateBcfRouterCommand;
import com.cloud.agent.api.BcfAnswer;
import com.cloud.agent.api.CacheBcfTopologyCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateBcfRouterCommand;
import com.cloud.agent.api.CreateBcfRouterInterfaceCommand;
import com.cloud.agent.api.CreateBcfSegmentCommand;
import com.cloud.agent.api.CreateBcfAttachmentCommand;
import com.cloud.agent.api.CreateBcfStaticNatCommand;
import com.cloud.agent.api.DeleteBcfSegmentCommand;
import com.cloud.agent.api.DeleteBcfAttachmentCommand;
import com.cloud.agent.api.DeleteBcfStaticNatCommand;
import com.cloud.agent.api.GetControllerDataAnswer;
import com.cloud.agent.api.GetControllerDataCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupBigSwitchBcfCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.SyncBcfTopologyCommand;
import com.cloud.agent.api.UpdateBcfAttachmentCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.bigswitch.AclData;
import com.cloud.network.bigswitch.BigSwitchBcfApi;
import com.cloud.network.bigswitch.BigSwitchBcfApiException;
import com.cloud.network.bigswitch.Capabilities;
import com.cloud.network.bigswitch.ControlClusterStatus;
import com.cloud.network.bigswitch.ControllerData;
import com.cloud.network.bigswitch.FloatingIpData;
import com.cloud.network.bigswitch.NetworkData;
import com.cloud.network.bigswitch.AttachmentData;
import com.cloud.network.bigswitch.RouterData;
import com.cloud.network.bigswitch.RouterInterfaceData;
import com.cloud.network.bigswitch.TopologyData;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;

public class BigSwitchBcfResource extends ManagerBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(BigSwitchBcfResource.class);

    private String _name;
    private String _guid;
    private String _zoneId;
    private int _numRetries;

    private BigSwitchBcfApi _bigswitchBcfApi;
    private TopologyData _latestTopology = null;
    private boolean initTopologySyncDone = false;

    protected BigSwitchBcfApi createBigSwitchBcfApi() {
        return new BigSwitchBcfApi();
    }

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

        _numRetries = 2;

        String hostname = (String)params.get("hostname");
        if (hostname == null) {
            throw new ConfigurationException("Missing host name from params: " + params);
        }

        String username = (String) params.get("username");
        if (username == null) {
            throw new ConfigurationException("Missing user name from params: " + params);
        }

        String password = (String) params.get("password");
        if (password == null) {
            throw new ConfigurationException("Missing password from params: " + params);
        }

        Boolean nat = Boolean.parseBoolean((String) params.get("nat"));
        if (nat == null) {
            throw new ConfigurationException("Missing password from params: " + params);
        }

        _bigswitchBcfApi = createBigSwitchBcfApi();
        _bigswitchBcfApi.setControllerAddress(hostname);
        _bigswitchBcfApi.setControllerUsername(username);
        _bigswitchBcfApi.setControllerPassword(password);
        _bigswitchBcfApi.setControllerNat(nat);
        _bigswitchBcfApi.setZoneId(_zoneId);

        return true;
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
    public Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupBigSwitchBcfCommand sc = new StartupBigSwitchBcfCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if(!initTopologySyncDone && _latestTopology != null){
            initTopologySyncDone = true;
            if(_bigswitchBcfApi.isNatEnabled()){
                try{
                    executeRequest(new SyncBcfTopologyCommand(true, true), _numRetries);
                } catch(Exception e){
                    s_logger.error("BigSwitch BCF sync error", e);
                }
            } else {
                try{
                    executeRequest(new SyncBcfTopologyCommand(true, false), _numRetries);
                } catch (Exception e){
                    s_logger.error("BigSwitch BCF sync error", e);
                }
            }
        }
        try {
            ControlClusterStatus ccs = _bigswitchBcfApi.getControlClusterStatus();
            if (!ccs.getStatus()) {
                s_logger.error("ControlCluster state is not ready: " + ccs.getStatus());
                return null;
            }
            if (ccs.isTopologySyncRequested()) {
                if(_latestTopology != null) {
                    if(_bigswitchBcfApi.isNatEnabled()){
                        executeRequest(new SyncBcfTopologyCommand(true, true), _numRetries);
                    } else {
                        executeRequest(new SyncBcfTopologyCommand(true, false), _numRetries);
                    }
                } else {
                    s_logger.debug("topology sync needed but no topology history");
                }
            }
        } catch (BigSwitchBcfApiException e) {
            s_logger.error("getControlClusterStatus failed", e);
            return null;
        }
        try {
            Capabilities cap = _bigswitchBcfApi.getCapabilities();

            // TODO: update controller status display, enable/disable service accordingly
            if (cap.isTopologySyncRequested()) {
                if(_latestTopology != null) {
                    if(_bigswitchBcfApi.isNatEnabled()){
                        executeRequest(new SyncBcfTopologyCommand(true, true), _numRetries);
                    } else {
                        executeRequest(new SyncBcfTopologyCommand(true, false), _numRetries);
                    }
                }
            }

        } catch (BigSwitchBcfApiException e) {
            s_logger.error("getCapabilities failed", e);
        }
        return new PingCommand(Host.Type.L2Networking, id);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    public Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        } else if (cmd instanceof CreateBcfSegmentCommand) {
            _latestTopology = ((CreateBcfSegmentCommand) cmd).getTopology();
            return executeRequest((CreateBcfSegmentCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteBcfSegmentCommand) {
            _latestTopology = ((DeleteBcfSegmentCommand) cmd).getTopology();
            return executeRequest((DeleteBcfSegmentCommand)cmd, numRetries);
        } else if (cmd instanceof CreateBcfAttachmentCommand) {
            _latestTopology = ((CreateBcfAttachmentCommand) cmd).getTopology();
            return executeRequest((CreateBcfAttachmentCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteBcfAttachmentCommand) {
            _latestTopology = ((DeleteBcfAttachmentCommand) cmd).getTopology();
            return executeRequest((DeleteBcfAttachmentCommand)cmd, numRetries);
        } else if (cmd instanceof UpdateBcfAttachmentCommand) {
            _latestTopology = ((UpdateBcfAttachmentCommand) cmd).getTopology();
            return executeRequest((UpdateBcfAttachmentCommand)cmd, numRetries);
        } else if (cmd instanceof CreateBcfRouterCommand) {
            _latestTopology = ((CreateBcfRouterCommand) cmd).getTopology();
            return executeRequest((CreateBcfRouterCommand)cmd, numRetries);
        } else if (cmd instanceof CreateBcfRouterInterfaceCommand) {
            _latestTopology = ((CreateBcfRouterInterfaceCommand) cmd).getTopology();
            return executeRequest((CreateBcfRouterInterfaceCommand)cmd, numRetries);
        } else if (cmd instanceof CreateBcfStaticNatCommand) {
            _latestTopology = ((CreateBcfStaticNatCommand) cmd).getTopology();
            return executeRequest((CreateBcfStaticNatCommand)cmd, numRetries);
        } else if (cmd instanceof DeleteBcfStaticNatCommand) {
            _latestTopology = ((DeleteBcfStaticNatCommand) cmd).getTopology();
            return executeRequest((DeleteBcfStaticNatCommand)cmd, numRetries);
        } else if (cmd instanceof UpdateBcfRouterCommand) {
            _latestTopology = ((UpdateBcfRouterCommand) cmd).getTopology();
            return executeRequest((UpdateBcfRouterCommand)cmd, numRetries);
        } else if (cmd instanceof SyncBcfTopologyCommand) {
            return executeRequest((SyncBcfTopologyCommand)cmd, numRetries);
        } else if (cmd instanceof CacheBcfTopologyCommand) {
            return executeRequest((CacheBcfTopologyCommand)cmd, numRetries);
        } else if (cmd instanceof GetControllerDataCommand) {
            return executeRequest((GetControllerDataCommand)cmd, numRetries);
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

    public void setTopology(TopologyData topology){
        _latestTopology = topology;
    }

    public TopologyData getTopology(){
        return _latestTopology;
    }

    private Answer executeRequest(CreateBcfSegmentCommand cmd, int numRetries) {
        NetworkData network = new NetworkData();
        network.getNetwork().setTenantId(cmd.getTenantId());
        network.getNetwork().setTenantName(cmd.getTenantName());
        network.getNetwork().setId(cmd.getNetworkId());
        network.getNetwork().setName(truncate("segment-cloudstack-" + cmd.getNetworkName(), 64));
        network.getNetwork().setVlan(cmd.getVlan());

        try {
            String hash =_bigswitchBcfApi.createNetwork(network);
            return new BcfAnswer(cmd, true, "Segment " + network.getNetwork().getId() + " created", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "Segment " + network.getNetwork().getId() + " created; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }

    }

    private Answer executeRequest(DeleteBcfSegmentCommand cmd, int numRetries) {
        try {
            String hash = _bigswitchBcfApi.deleteNetwork(cmd.getTenantUuid(), cmd.getNetworkUuid());
            return new BcfAnswer(cmd, true, "Segment " + cmd.getNetworkUuid() + " deleted", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "Segment " + cmd.getNetworkUuid() + " deleted; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(CreateBcfAttachmentCommand cmd, int numRetries) {
        AttachmentData attachment = new AttachmentData();
        attachment.getAttachment().setId(cmd.getNicId());
        attachment.getAttachment().setHostId(cmd.getPortId());
        attachment.getAttachment().setTenantName(cmd.getTenantName());
        attachment.getAttachment().setVlan(cmd.getVlan());
        attachment.getAttachment().addIpv4(cmd.getIpv4());
        attachment.getAttachment().setMac(cmd.getMac());

        try {
            String hash = _bigswitchBcfApi.createAttachment(cmd.getTenantId(), cmd.getNetworkId(), attachment);
            return new BcfAnswer(cmd, true, "network attachment " + cmd.getPortId() + " created", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "network attachment " + cmd.getPortId() + " created; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(DeleteBcfAttachmentCommand cmd, int numRetries) {
        String nicName = cmd.getAttachmentId();
        try {
            String hash = _bigswitchBcfApi.deleteAttachment(cmd.getTenantId(), cmd.getNetworkId(), nicName);
            return new BcfAnswer(cmd, true, "network attachment " + nicName + " deleted", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "network attachment " + nicName + " deleted; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(UpdateBcfAttachmentCommand cmd, int numRetries) {
        AttachmentData attachment = new AttachmentData();
        attachment.getAttachment().setId(cmd.getAttachmentId());
        attachment.getAttachment().setTenantName(cmd.getTenantId());

        try {
            String hash = _bigswitchBcfApi.modifyAttachment(cmd.getTenantId(), cmd.getNetworkId(), attachment);
            return new BcfAnswer(cmd, true, "Network attachment  " + cmd.getAttachmentId() + " updated", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "Network attachment  " + cmd.getAttachmentId() + " updated; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(CreateBcfStaticNatCommand cmd, int numRetries) {
        FloatingIpData fip = new FloatingIpData();
        fip.setTenantId(cmd.getTenantId());
        fip.setNetworkId(cmd.getNetworkId());
        fip.setFixedIp(cmd.getPrivateIp());
        fip.setFloatingIpAndId(cmd.getPublicIp());
        fip.setMac(cmd.getMac());

        try {
            String hash = _bigswitchBcfApi.createFloatingIp(cmd.getTenantId(), fip);
            return new BcfAnswer(cmd, true, "floating ip " + cmd.getPublicIp() + ":" +
                    cmd.getPrivateIp() + " created", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "floating ip " + cmd.getPublicIp() + ":" +
                        cmd.getPrivateIp() + " created; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(DeleteBcfStaticNatCommand cmd, int numRetries) {
        try {
            String hash = _bigswitchBcfApi.deleteFloatingIp(cmd.getTenantId(), cmd.getFloatingIpId());
            return new BcfAnswer(cmd, true, "floating ip " + cmd.getPublicIp() + " deleted", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "floating ip " + cmd.getPublicIp() + " deleted; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(CreateBcfRouterCommand cmd, int numRetries) {
        RouterData router = new RouterData(cmd.getTenantId());
        try {
            String hash;
            hash = _bigswitchBcfApi.createRouter(cmd.getTenantId(), router);

            return new BcfAnswer(cmd, true, "router " + cmd.getTenantId() +
                    " created.", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, " created; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(CreateBcfRouterInterfaceCommand cmd, int numRetries) {
        RouterInterfaceData routerInterface = new RouterInterfaceData(cmd.getTenantId(),
                cmd.getGateway(), cmd.getCidr(), cmd.getNetworkId(), cmd.getNetworkName());
        try {
            String hash;
            hash = _bigswitchBcfApi.createRouterInterface(cmd.getTenantId(),
                    cmd.getTenantId(), routerInterface);

            return new BcfAnswer(cmd, true, "router " + cmd.getTenantId() +
                    " created.", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, " created; topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        }
    }

    private Answer executeRequest(UpdateBcfRouterCommand cmd, int numRetries){
        RouterData routerData = new RouterData(cmd.getTenantId());

        List<AclData> acls = new ArrayList<AclData>();
        acls.addAll(cmd.getAcls());
        routerData.getRouter().getAcls().addAll(acls);

        routerData.getRouter().addExternalGateway(cmd.getPublicIp());

        try {
            String hash = _bigswitchBcfApi.modifyRouter(cmd.getTenantId(), routerData);
            return new BcfAnswer(cmd, true, "tenant " + cmd.getTenantId() + " router updated", hash);
        } catch (BigSwitchBcfApiException e) {
            if (e.is_topologySyncRequested()) {
                cmd.setTopologySyncRequested(true);
                return new BcfAnswer(cmd, true, "tenant " + cmd.getTenantId() + " router updated but topology sync required.");
            } else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new BcfAnswer(cmd, e);
                }
            }
        } catch (IllegalArgumentException e1){
            return new BcfAnswer(cmd, false, "Illegal argument in BCF router update");
        }
    }

    private Answer executeRequest(SyncBcfTopologyCommand cmd, int numRetries) {
        try {
            TopologyData topo = _latestTopology;
            if (!cmd.isNetworkIncluded()) {
                topo.clearNetworks();
            }
            if(!cmd.isRouterIncluded()) {
                topo.clearRouters();
            }
            String hash = _bigswitchBcfApi.syncTopology(topo);
            if(!initTopologySyncDone){
                initTopologySyncDone=true;
            }
            return new BcfAnswer(cmd, true, "BCF topology synced", hash);
        } catch (BigSwitchBcfApiException e) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new BcfAnswer(cmd, e);
            }
        } catch (IllegalArgumentException e1){
            return new BcfAnswer(cmd, false, "Illegal argument in BCF topology sync");
        }
    }

    private Answer executeRequest(CacheBcfTopologyCommand cmd, int numRetries) {
        _latestTopology = cmd.getTopology();
        return new Answer(cmd, true, "BCF topology cached");
    }

    private Answer executeRequest(GetControllerDataCommand cmd, int numRetries) {
        ControllerData controller = _bigswitchBcfApi.getControllerData();
        return new GetControllerDataAnswer(cmd,
                controller.getIpAddress(),
                controller.isPrimary());
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private Answer retry(Command cmd, int numRetries) {
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);
        return executeRequest(cmd, numRetries);
    }

    private String truncate(String string, int length) {
        if (string.length() <= length) {
            return string;
        } else {
            return string.substring(0, length);
        }
    }
}
