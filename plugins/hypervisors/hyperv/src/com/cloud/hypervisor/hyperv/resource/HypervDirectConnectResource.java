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
package com.cloud.hypervisor.hyperv.resource;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.storage.command.CopyCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetVmConfigAnswer;
import com.cloud.agent.api.GetVmConfigAnswer.NicDetails;
import com.cloud.agent.api.GetVmConfigCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.ModifyVmNicConfigCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesAnswer;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesAnswer;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetSourceNatAnswer;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteAnswer;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.hyperv.manager.HypervManager;
import com.cloud.network.HAProxyConfigurator;
import com.cloud.network.LoadBalancerConfigurator;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.network.rules.FirewallRule;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachineName;


/**
 * Implementation of dummy resource to be returned from discoverer.
 **/
@Local(value = ServerResource.class)
public class HypervDirectConnectResource extends ServerResourceBase implements ServerResource, VirtualRouterDeployer {
    public static final int DEFAULT_AGENT_PORT = 8250;
    public static final String HOST_VM_STATE_REPORT_COMMAND = "org.apache.cloudstack.HostVmStateReportCommand";
    private static final Logger s_logger = Logger.getLogger(HypervDirectConnectResource.class.getName());

    private static final Gson s_gson = GsonHelper.getGson();
    private String _zoneId;
    private String _podId;
    private String _clusterId;
    private String _guid;
    private String _agentIp;
    private final int _port = DEFAULT_AGENT_PORT;
    protected final long _opsTimeout = 900000;  // 15 minutes time out to time

    protected final int _retry = 24;
    protected final int _sleep = 10000;
    protected static final int DEFAULT_DOMR_SSHPORT = 3922;
    private String _clusterGuid;

    // Used by initialize to assert object configured before
    // initialize called.
    private boolean _configureCalled = false;

    private String _username;
    private String _password;

    private static HypervManager s_hypervMgr;
    @Inject
    HypervManager _hypervMgr;
    protected VirtualRoutingResource _vrResource;

    @PostConstruct
    void init() {
        s_hypervMgr = _hypervMgr;
    }

    @Override
    public final Type getType() {
        return Type.Routing;
    }

    @Override
    public final StartupCommand[] initialize() {
        // assert
        if (!_configureCalled) {
            String errMsg = this.getClass().getName() + " requires configure() be called before" + " initialize()";
            s_logger.error(errMsg);
        }

        // Create default StartupRoutingCommand, then customise
        StartupRoutingCommand defaultStartRoutCmd =
            new StartupRoutingCommand(0, 0, 0, 0, null, Hypervisor.HypervisorType.Hyperv, RouterPrivateIpStrategy.HostLocal);

        // Identity within the data centre is decided by CloudStack kernel,
        // and passed via ServerResource.configure()
        defaultStartRoutCmd.setDataCenter(_zoneId);
        defaultStartRoutCmd.setPod(_podId);
        defaultStartRoutCmd.setCluster(_clusterId);
        defaultStartRoutCmd.setGuid(_guid);
        defaultStartRoutCmd.setName(_name);
        defaultStartRoutCmd.setPrivateIpAddress(_agentIp);
        defaultStartRoutCmd.setStorageIpAddress(_agentIp);
        defaultStartRoutCmd.setPool(_clusterGuid);

        s_logger.debug("Generated StartupRoutingCommand for _agentIp \"" + _agentIp + "\"");

        defaultStartRoutCmd.setVersion(this.getClass().getPackage().getImplementationVersion());

        // Specifics of the host's resource capacity and network configuration
        // comes from the host itself. CloudStack sanity checks network
        // configuration
        // and uses capacity info for resource allocation.
        Command[] startCmds = requestStartupCommand(new Command[] {defaultStartRoutCmd});

        // TODO: may throw, is this okay?
        StartupRoutingCommand startCmd = (StartupRoutingCommand)startCmds[0];

        // Assert that host identity is consistent with existing values.
        if (startCmd == null) {
            String errMsg = String.format("Host %s (IP %s)" + "did not return a StartupRoutingCommand", _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getDataCenter().equals(defaultStartRoutCmd.getDataCenter())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed zone/data center.  Was " + defaultStartRoutCmd.getDataCenter() + " NOW its " + startCmd.getDataCenter(), _name,
                            _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPod().equals(defaultStartRoutCmd.getPod())) {
            String errMsg = String.format("Host %s (IP %s) changed pod.  Was " + defaultStartRoutCmd.getPod() + " NOW its " + startCmd.getPod(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getCluster().equals(defaultStartRoutCmd.getCluster())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed cluster.  Was " + defaultStartRoutCmd.getCluster() + " NOW its " + startCmd.getCluster(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getGuid().equals(defaultStartRoutCmd.getGuid())) {
            String errMsg = String.format("Host %s (IP %s) changed guid.  Was " + defaultStartRoutCmd.getGuid() + " NOW its " + startCmd.getGuid(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPrivateIpAddress().equals(defaultStartRoutCmd.getPrivateIpAddress())) {
            String errMsg =
                    String.format("Host %s (IP %s) IP address.  Was " + defaultStartRoutCmd.getPrivateIpAddress() + " NOW its " + startCmd.getPrivateIpAddress(), _name,
                            _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getName().equals(defaultStartRoutCmd.getName())) {
            String errMsg = String.format("Host %s (IP %s) name.  Was " + startCmd.getName() + " NOW its " + defaultStartRoutCmd.getName(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }

        // Host will also supply details of an existing StoragePool if it has
        // been configured with one.
        //
        // NB: if the host was configured
        // with a local storage pool, CloudStack may not be able to use it
        // unless
        // it is has service offerings configured to recognise this storage
        // type.
        StartupStorageCommand storePoolCmd = null;
        if (startCmds.length > 1) {
            storePoolCmd = (StartupStorageCommand)startCmds[1];
            // TODO: is this assertion required?
            if (storePoolCmd == null) {
                String frmtStr = "Host %s (IP %s) sent incorrect Command, " + "second parameter should be a " + "StartupStorageCommand";
                String errMsg = String.format(frmtStr, _name, _agentIp);
                s_logger.error(errMsg);
                // TODO: valid to return null, or should we throw?
                return null;
            }
            s_logger.info("Host " + _name + " (IP " + _agentIp + ") already configured with a storeage pool, details " + s_gson.toJson(startCmds[1]));
        } else {
            s_logger.info("Host " + _name + " (IP " + _agentIp + ") already configured with a storeage pool, details ");
        }
        return new StartupCommand[] {startCmd, storePoolCmd};
    }

    @Override
    public final PingCommand getCurrentStatus(final long id) {
        PingCommand pingCmd = new PingRoutingCommand(getType(), id, getHostVmStateReport());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping host " + _name + " (IP " + _agentIp + ")");
        }

        Answer pingAns = executeRequest(pingCmd);

        if (pingAns == null || !pingAns.getResult()) {
            s_logger.info("Cannot ping host " + _name + " (IP " + _agentIp + "), pingAns (blank means null) is:" + pingAns);
            return null;
        }
        return pingCmd;
    }

    public final ArrayList<Map<String, String>> requestHostVmStateReport() {
        URI agentUri = null;
        try {
            agentUri = new URI("https", null, _agentIp, _port, "/api/HypervResource/" + HOST_VM_STATE_REPORT_COMMAND, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        String incomingCmd = postHttpRequest("{}", agentUri);

        if (incomingCmd == null) {
            return null;
        }
        ArrayList<Map<String, String>> result = null;
        try {
            result = s_gson.fromJson(incomingCmd, new TypeToken<ArrayList<HashMap<String, String>>>() {
            }.getType());
        } catch (Exception ex) {
            String errMsg = "Failed to deserialize Command[] " + incomingCmd;
            s_logger.error(errMsg, ex);
        }
        s_logger.debug("HostVmStateReportCommand received response "
                + s_gson.toJson(result));
        if (result != null) {
            if (!result.isEmpty()) {
                return result;
            } else {
                return new ArrayList<Map<String, String>>();
            }
        }
        return null;
    }

    protected HashMap<String, HostVmStateReportEntry> getHostVmStateReport() {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<String, HostVmStateReportEntry>();
        ArrayList<Map<String, String>> vmList = requestHostVmStateReport();
        if (vmList == null) {
            return null;
        }

        for (Map<String, String> vmMap : vmList) {
            String name = (String)vmMap.keySet().toArray()[0];
            vmStates.put(name, new HostVmStateReportEntry(PowerState.valueOf(vmMap.get(name)), _guid));
        }
        return vmStates;
    }

    // TODO: Is it valid to return NULL, or should we throw on error?
    // Returns StartupCommand with fields revised with values known only to the
    // host
    public final Command[] requestStartupCommand(final Command[] cmd) {
        // Set HTTP POST destination URI
        // Using java.net.URI, see
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/net/URI.html
        URI agentUri = null;
        try {
            String cmdName = StartupCommand.class.getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            // TODO add proper logging
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        String incomingCmd = postHttpRequest(s_gson.toJson(cmd), agentUri);

        if (incomingCmd == null) {
            return null;
        }
        Command[] result = null;
        try {
            result = s_gson.fromJson(incomingCmd, Command[].class);
        } catch (Exception ex) {
            String errMsg = "Failed to deserialize Command[] " + incomingCmd;
            s_logger.error(errMsg, ex);
        }
        s_logger.debug("requestStartupCommand received response " + s_gson.toJson(result));
        if (result.length > 0) {
            return result;
        }
        return null;
    }

    // TODO: Is it valid to return NULL, or should we throw on error?
    @Override
    public final Answer executeRequest(final Command cmd) {
        // Set HTTP POST destination URI
        // Using java.net.URI, see
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/net/URI.html
        URI agentUri = null;
        Class<? extends Command> clazz = cmd.getClass();
        Answer answer = null;
        try {
            String cmdName = cmd.getClass().getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            // TODO add proper logging
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        if (cmd instanceof NetworkElementCommand) {
            return _vrResource.executeRequest((NetworkElementCommand)cmd);
        }if (clazz == CheckSshCommand.class) {
            answer = execute((CheckSshCommand)cmd);
        } else if (clazz == GetDomRVersionCmd.class) {
            answer = execute((GetDomRVersionCmd)cmd);
        } else if (cmd instanceof NetworkUsageCommand) {
            answer = execute((NetworkUsageCommand)cmd);
        } else if (clazz == IpAssocCommand.class) {
            answer = execute((IpAssocCommand)cmd);
        } else if (clazz == DnsMasqConfigCommand.class) {
            return execute((DnsMasqConfigCommand)cmd);
        } else if (clazz == CreateIpAliasCommand.class) {
            return execute((CreateIpAliasCommand)cmd);
        } else if (clazz == DhcpEntryCommand.class) {
            answer = execute((DhcpEntryCommand)cmd);
        } else if (clazz == VmDataCommand.class) {
            answer = execute((VmDataCommand)cmd);
        } else if (clazz == SavePasswordCommand.class) {
            answer = execute((SavePasswordCommand)cmd);
        } else if (clazz == SetFirewallRulesCommand.class) {
            answer = execute((SetFirewallRulesCommand)cmd);
        } else if (clazz == LoadBalancerConfigCommand.class) {
            answer = execute((LoadBalancerConfigCommand)cmd);
        } else if (clazz == DeleteIpAliasCommand.class) {
            return execute((DeleteIpAliasCommand)cmd);
        } else if (clazz == PingTestCommand.class) {
            answer = execute((PingTestCommand)cmd);
        } else if (clazz == SetStaticNatRulesCommand.class) {
            answer = execute((SetStaticNatRulesCommand)cmd);
        } else if (clazz == CheckRouterCommand.class) {
            answer = execute((CheckRouterCommand)cmd);
        } else if (clazz == SetPortForwardingRulesCommand.class) {
            answer = execute((SetPortForwardingRulesCommand)cmd);
        } else if (clazz == SetSourceNatCommand.class) {
            answer = execute((SetSourceNatCommand)cmd);
        } else if (clazz == Site2SiteVpnCfgCommand.class) {
            answer = execute((Site2SiteVpnCfgCommand)cmd);
        } else if (clazz == CheckS2SVpnConnectionsCommand.class) {
            answer = execute((CheckS2SVpnConnectionsCommand) cmd);
        } else if (clazz == RemoteAccessVpnCfgCommand.class) {
            answer = execute((RemoteAccessVpnCfgCommand) cmd);
        } else if (clazz == VpnUsersCfgCommand.class) {
            answer = execute((VpnUsersCfgCommand) cmd);
        } else if (clazz == SetStaticRouteCommand.class) {
            answer = execute((SetStaticRouteCommand) cmd);
        } else if (clazz == SetMonitorServiceCommand.class) {
            answer = execute((SetMonitorServiceCommand) cmd);
        } else if (clazz == PlugNicCommand.class) {
            answer = execute((PlugNicCommand)cmd);
        } else if (clazz == UnPlugNicCommand.class) {
            answer = execute((UnPlugNicCommand)cmd);
        } else if (clazz == CopyCommand.class) {
            answer = execute((CopyCommand)cmd);
        }
        else {
            if (clazz == StartCommand.class) {
                VirtualMachineTO vmSpec = ((StartCommand)cmd).getVirtualMachine();
                if (vmSpec.getType() != VirtualMachine.Type.User) {
                    if (s_hypervMgr != null) {
                        String secondary = s_hypervMgr.prepareSecondaryStorageStore(Long.parseLong(_zoneId));
                        if (secondary != null) {
                            ((StartCommand)cmd).setSecondaryStorage(secondary);
                        }
                    } else {
                        s_logger.error("Hyperv manager isn't available. Couldn't check and copy the systemvm iso.");
                    }
                }
            }

            // Send the cmd to hyperv agent.
            String ansStr = postHttpRequest(s_gson.toJson(cmd), agentUri);
            if (ansStr == null) {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
            // Only Answer instances are returned by remote agents.
            // E.g. see Response.getAnswers()
            Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
            String logResult = cleanPassword(s_gson.toJson(result));
            s_logger.debug("executeRequest received response " + logResult);
            if (result.length > 0) {
                return result[0];
            }
        }
        return answer;
    }

    private Answer execute(CopyCommand cmd) {
        URI agentUri = null;
        try {
            String cmdName = cmd.getClass().getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        cleanPassword(cmd.getSrcTO().getDataStore());
        cleanPassword(cmd.getDestTO().getDataStore());

        // Send the cmd to hyperv agent.
        String ansStr = postHttpRequest(s_gson.toJson(cmd), agentUri);
        if (ansStr == null) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }

        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        String logResult = cleanPassword(s_gson.toJson(result));
        s_logger.debug("executeRequest received response " + logResult);
        if (result.length > 0) {
            return result[0];
        }

        return null;
    }

    private void cleanPassword(DataStoreTO dataStoreTO) {
        if (dataStoreTO instanceof NfsTO) {
            NfsTO nfsTO = (NfsTO)dataStoreTO;
            String url = nfsTO.getUrl();
            if (url.contains("cifs") && url.contains("password")) {
                nfsTO.setUrl(url.substring(0, url.indexOf('?')));
            }
        }
    }

    private PlugNicAnswer execute(PlugNicCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource PlugNicCommand " + s_gson.toJson(cmd));
        }

        try {

            String vmName = cmd.getVmName();
            NicTO nic = cmd.getNic();
            URI broadcastUri = nic.getBroadcastUri();
            if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
                throw new InternalErrorException("Unable to assign a public IP to a VIF on network " + nic.getBroadcastUri());
            }
            String vlanId = BroadcastDomainType.getValue(broadcastUri);
            int publicNicInfo = -1;
            publicNicInfo = getVmFreeNicIndex(vmName);
            if (publicNicInfo > 0) {
                modifyNicVlan(vmName, vlanId, publicNicInfo, true, cmd.getNic().getName());
                return new PlugNicAnswer(cmd, true, "success");
            }
            String msg = " Plug Nic failed for the vm as it has reached max limit of NICs to be added";
            s_logger.warn(msg);
            return new PlugNicAnswer(cmd, false, msg);

        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new PlugNicAnswer(cmd, false, "Unable to execute PlugNicCommand due to " + e.toString());
        }
    }


    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource UnPlugNicCommand " + s_gson.toJson(cmd));
        }

        try {
            String vmName = cmd.getVmName();
            NicTO nic = cmd.getNic();
            URI broadcastUri = nic.getBroadcastUri();
            if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
                throw new InternalErrorException("Unable to unassign a public IP to a VIF on network " + nic.getBroadcastUri());
            }
            String vlanId = BroadcastDomainType.getValue(broadcastUri);
            int publicNicInfo = -1;
            publicNicInfo = getVmNics(vmName, vlanId);
            if (publicNicInfo > 0) {
                modifyNicVlan(vmName, "2", publicNicInfo, false, "");
            }
            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new UnPlugNicAnswer(cmd, false, "Unable to execute unPlugNicCommand due to " + e.toString());
        }
    }

    @Override
    public ExecutionResult executeInVR(String routerIP, String script, String args) {
        return executeInVR(routerIP, script, args, 120);
    }

    @Override
    public ExecutionResult executeInVR(String routerIP, String script, String args, int timeout) {
        Pair<Boolean, String> result;

        //TODO: Password should be masked, cannot output to log directly
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on VR: " + routerIP + ", script: " + script + " with args: " + args);
        }

        try {
            result = SshHelper.sshExecute(routerIP, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/" + script + " " + args,
                    60000, 60000, timeout * 1000);
        } catch (Exception e) {
            String msg = "Command failed due to " + e ;
            s_logger.error(msg);
            result = new Pair<Boolean, String>(false, msg);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(script + " execution result: " + result.first().toString());
        }
        return new ExecutionResult(result.first(), result.second());
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String filePath, String fileName, String content) {
        File keyFile = getSystemVMKeyFile();
        try {
            SshHelper.scpTo(routerIp, 3922, "root", keyFile, null, filePath, content.getBytes(Charset.forName("UTF-8")), fileName, null);
        } catch (Exception e) {
            s_logger.warn("Fail to create file " + filePath + fileName + " in VR " + routerIp, e);
            return new ExecutionResult(false, e.getMessage());
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        //Update IP used to access router
        cmd.setRouterAccessIp(getRouterSshControlIp(cmd));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand)cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand)cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand)cmd);
        } else if (cmd instanceof SetNetworkACLCommand) {
            return prepareNetworkElementCommand((SetNetworkACLCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        try {

            IpAddressTO[] ips = cmd.getIpAddresses();
            String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
            String controlIp = getRouterSshControlIp(cmd);

            for (IpAddressTO ip : ips) {
                /**
                 * TODO support other networks
                 */
                URI broadcastUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
                if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
                    throw new InternalErrorException("Unable to assign a public IP to a VIF on network " + ip.getBroadcastUri());
                }
                String vlanId = BroadcastDomainType.getValue(broadcastUri);
                int publicNicInfo = -1;
                publicNicInfo = getVmNics(routerName, vlanId);

                boolean addVif = false;
                if (ip.isAdd() && publicNicInfo == -1) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Plug new NIC to associate" + controlIp + " to " + ip.getPublicIp());
                    }
                    addVif = true;
                }

                if (addVif) {
                    Pair<Integer, String> nicdevice = findRouterFreeEthDeviceIndex(controlIp);
                    publicNicInfo = nicdevice.first();
                    if (publicNicInfo > 0) {
                        modifyNicVlan(routerName, vlanId, nicdevice.second());
                        // After modifying the vnic on VR, check the VR VNics config in the host and get the device position
                        publicNicInfo = getVmNics(routerName, vlanId);
                        // As a new nic got activated in the VR. add the entry in the NIC's table.
                        networkUsage(controlIp, "addVif", "eth" + publicNicInfo);
                    }
                    else {
                        // we didn't find any eth device available in VR to configure the ip range with new VLAN
                        String msg = "No Nic is available on DomR VIF to associate/disassociate IP with.";
                        s_logger.error(msg);
                        throw new InternalErrorException(msg);
                    }
                    ip.setNicDevId(publicNicInfo);
                    ip.setNewNic(addVif);
                } else {
                    ip.setNicDevId(publicNicInfo);
                }
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString() + " will shortcut rest of IPAssoc commands", e);
            return new ExecutionResult(false, e.toString());
        }
        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(SetupGuestNetworkCommand cmd) {
        NicTO nic = cmd.getNic();
        String domrName =
                cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            URI broadcastUri = nic.getBroadcastUri();
            String vlanId = BroadcastDomainType.getValue(broadcastUri);
            int ethDeviceNum = getVmNics(domrName, vlanId);
            if (ethDeviceNum > 0) {
                nic.setDeviceId(ethDeviceNum);
            } else {
                return new ExecutionResult(false, "Prepare SetupGuestNetwork failed due to unable to find the nic");
            }
        } catch (Exception e) {
            String msg = "Prepare SetupGuestNetwork failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }


    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {
                URI broadcastUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
                if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
                    throw new InternalErrorException("Invalid Broadcast URI " + ip.getBroadcastUri());
                }
                String vlanId = BroadcastDomainType.getValue(broadcastUri);
                int publicNicInfo = -1;
                publicNicInfo = getVmNics(routerName, vlanId);
                if (publicNicInfo < 0) {
                    if (ip.isAdd()) {
                        throw new InternalErrorException("Failed to find DomR VIF to associate/disassociate IP with.");
                    } else {
                        s_logger.debug("VIF to deassociate IP with does not exist, return success");
                        continue;
                    }
                }

                ip.setNicDevId(publicNicInfo);
            }
        } catch (Exception e) {
            s_logger.error("Prepare Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.toString());
        }

        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        IpAddressTO pubIp = cmd.getIpAddress();

        try {
            String broadcastUri = pubIp.getBroadcastUri();
            String vlanId = BroadcastDomainType.getValue(broadcastUri);
            int ethDeviceNum = getVmNics(routerName, vlanId);
            if (ethDeviceNum > 0) {
                pubIp.setNicDevId(ethDeviceNum);
            } else {
                return new ExecutionResult(false, "Prepare Ip SNAT failed due to unable to find the nic");
            }
        } catch (Exception e) {
            String msg = "Prepare Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, e.toString());
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(SetNetworkACLCommand cmd) {
        NicTO nic = cmd.getNic();
        String routerName =
                cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            URI broadcastUri = nic.getBroadcastUri();
            String vlanId = BroadcastDomainType.getValue(broadcastUri);
            int ethDeviceNum = getVmNics(routerName, vlanId);
            if (ethDeviceNum > 0) {
                nic.setDeviceId(ethDeviceNum);
            } else {
                return new ExecutionResult(false, "Prepare SetNetworkACL failed due to unable to find the nic");
            }
        } catch (Exception e) {
            String msg = "Prepare SetNetworkACL failed due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        return new ExecutionResult(true, null);
    }

    protected Answer execute(final RemoteAccessVpnCfgCommand cmd) {
        String controlIp = getRouterSshControlIp(cmd);
        StringBuffer argsBuf = new StringBuffer();
        if (cmd.isCreate()) {
            argsBuf.append(" -r ").append(cmd.getIpRange()).append(" -p ").append(cmd.getPresharedKey()).append(" -s ").append(cmd.getVpnServerIp()).append(" -l ").append(cmd.getLocalIp())
            .append(" -c ");

        } else {
            argsBuf.append(" -d ").append(" -s ").append(cmd.getVpnServerIp());
        }
        argsBuf.append(" -C ").append(cmd.getLocalCidr());
        argsBuf.append(" -i ").append(cmd.getPublicInterface());

        try {

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Executing /opt/cloud/bin/vpn_lt2p.sh ");
            }

            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/vpn_l2tp.sh " + argsBuf.toString());

            if (!result.first()) {
                s_logger.error("RemoteAccessVpnCfg command on domR failed, message: " + result.second());

                return new Answer(cmd, false, "RemoteAccessVpnCfg command failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("RemoteAccessVpnCfg command on domain router " + argsBuf.toString() + " completed");
            }

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn(e.getMessage());
            }

            String msg = "RemoteAccessVpnCfg command failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
    }

    protected Answer execute(final VpnUsersCfgCommand cmd) {

        String controlIp = getRouterSshControlIp(cmd);
        for (VpnUsersCfgCommand.UsernamePassword userpwd : cmd.getUserpwds()) {
            StringBuffer argsBuf = new StringBuffer();
            if (!userpwd.isAdd()) {
                argsBuf.append(" -U ").append(userpwd.getUsername());
            } else {
                argsBuf.append(" -u ").append(userpwd.getUsernamePassword());
            }

            try {

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Executing /opt/cloud/bin/vpn_lt2p.sh ");
                }

                Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/vpn_l2tp.sh " + argsBuf.toString());

                if (!result.first()) {
                    s_logger.error("VpnUserCfg command on domR failed, message: " + result.second());

                    return new Answer(cmd, false, "VpnUserCfg command failed due to " + result.second());
                }
            } catch (Throwable e) {
                if (e instanceof RemoteException) {
                    s_logger.warn(e.getMessage());
                }

                String msg = "VpnUserCfg command failed due to " + e.getMessage();
                s_logger.error(msg, e);
                return new Answer(cmd, false, msg);
            }
        }

        return new Answer(cmd);
    }
    private SetStaticRouteAnswer execute(SetStaticRouteCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource SetStaticRouteCommand: " + s_gson.toJson(cmd));
        }

        boolean endResult = true;

        String controlIp = getRouterSshControlIp(cmd);
        String args = "";
        String[] results = new String[cmd.getStaticRoutes().length];
        int i = 0;

        // Extract and build the arguments for the command to be sent to the VR.
        String[] rules = cmd.generateSRouteRules();
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < rules.length; j++) {
            sb.append(rules[j]).append(',');
        }
        args += " -a " + sb.toString();

        // Send over the command for execution, via ssh, to the VR.
        try {
            Pair<Boolean, String> result =
                    SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/vpc_staticroute.sh " + args);

            if (s_logger.isDebugEnabled())
                s_logger.debug("Executing script on domain router " + controlIp + ": /opt/cloud/bin/vpc_staticroute.sh " + args);

            if (!result.first()) {
                s_logger.error("SetStaticRouteCommand failure on setting one rule. args: " + args);
                results[i++] = "Failed";
                endResult = false;
            } else {
                results[i++] = null;
            }
        } catch (Throwable e) {
            s_logger.error("SetStaticRouteCommand(args: " + args + ") failed on setting one rule due to " + e);
            results[i++] = "Failed";
            endResult = false;
        }
        return new SetStaticRouteAnswer(cmd, endResult, results);

    }

    protected CheckS2SVpnConnectionsAnswer execute(CheckS2SVpnConnectionsCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CheckS2SVpnConnectionsCommand: " + s_gson.toJson(cmd));
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /opt/cloud/bin/checkbatchs2svpn.sh ");
        }

        Pair<Boolean, String> result;
        try {
            String controlIp = getRouterSshControlIp(cmd);
            StringBuilder cmdline = new StringBuilder("/opt/cloud/bin/checkbatchs2svpn.sh ");
            for (String ip : cmd.getVpnIps()) {
                cmdline.append(" ");
                cmdline.append(ip);
            }

            result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, cmdline.toString());

            if (!result.first()) {
                s_logger.error("check site-to-site vpn connections command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " failed, message: " +
                        result.second());

                return new CheckS2SVpnConnectionsAnswer(cmd, false, result.second());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("check site-to-site vpn connections command on domain router " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " completed");
            }
        } catch (Throwable e) {
            String msg = "CheckS2SVpnConnectionsCommand failed due to " + e;
            s_logger.error(msg, e);
            return new CheckS2SVpnConnectionsAnswer(cmd, false, "CheckS2SVpnConneciontsCommand failed");
        }
        return new CheckS2SVpnConnectionsAnswer(cmd, true, result.second());
    }

    protected Answer execute(Site2SiteVpnCfgCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource Site2SiteVpnCfgCommand " + s_gson.toJson(cmd));
        }

        String routerIp = getRouterSshControlIp(cmd);

        String args = "";
        if (cmd.isCreate()) {
            args += " -A";
            args += " -l ";
            args += cmd.getLocalPublicIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -g ";
            args += cmd.getLocalPublicGateway();
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
            args += " -e ";
            args += "\"" + cmd.getEspPolicy() + "\"";
            args += " -i ";
            args += "\"" + cmd.getIkePolicy() + "\"";
            args += " -t ";
            args += Long.toString(cmd.getIkeLifetime());
            args += " -T ";
            args += Long.toString(cmd.getEspLifetime());
            args += " -s ";
            args += "\"" + cmd.getIpsecPsk() + "\"";
            args += " -d ";
            if (cmd.getDpd()) {
                args += "1";
            } else {
                args += "0";
            }
        } else {
            args += " -D";
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
        }

        Pair<Boolean, String> result;
        try {
            result = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/ipsectunnel.sh " + args);

            if (!result.first()) {
                s_logger.error("Setup site2site VPN " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " failed, message: " + result.second());

                return new Answer(cmd, false, "Setup site2site VPN falied due to " + result.second());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("setup site 2 site vpn on router " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " completed");
            }
        } catch (Throwable e) {
            String msg = "Setup site2site VPN falied due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, "Setup site2site VPN failed due to " + e.getMessage());
        }
        return new Answer(cmd, true, result.second());
    }

    protected SetSourceNatAnswer execute(SetSourceNatCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource SetSourceNatCommand " + s_gson.toJson(cmd));
        }

        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = getRouterSshControlIp(cmd);
        IpAddressTO pubIp = cmd.getIpAddress();
        try {
            int ethDeviceNum = findRouterEthDeviceIndex(routerName, routerIp, pubIp.getVifMacAddress());
            String args = "";
            args += " -A ";
            args += " -l ";
            args += pubIp.getPublicIp();

            args += " -c ";
            args += "eth" + ethDeviceNum;

            Pair<Boolean, String> result = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/vpc_snat.sh " + args);

            if (!result.first()) {
                String msg = "SetupGuestNetworkCommand on domain router " + routerIp + " failed. message: " + result.second();
                s_logger.error(msg);

                return new SetSourceNatAnswer(cmd, false, msg);
            }

            return new SetSourceNatAnswer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new SetSourceNatAnswer(cmd, false, msg);
        }
    }

    protected Answer execute(SetPortForwardingRulesCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource SetPortForwardingRulesCommand: " + s_gson.toJson(cmd));
        }

        String controlIp = getRouterSshControlIp(cmd);
        String args = "";
        String[] results = new String[cmd.getRules().length];
        int i = 0;

        boolean endResult = true;
        for (PortForwardingRuleTO rule : cmd.getRules()) {
            args += rule.revoked() ? " -D " : " -A ";
            args += " -P " + rule.getProtocol().toLowerCase();
            args += " -l " + rule.getSrcIp();
            args += " -p " + rule.getStringSrcPortRange();
            args += " -r " + rule.getDstIp();
            args += " -d " + rule.getStringDstPortRange();

            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/firewall.sh " + args);

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Executing script on domain router " + controlIp + ": /root/firewall.sh " + args);

                if (!result.first()) {
                    s_logger.error("SetPortForwardingRulesCommand failure on setting one rule. args: " + args);
                    results[i++] = "Failed";
                    endResult = false;
                } else {
                    results[i++] = null;
                }
            } catch (Throwable e) {
                s_logger.error("SetPortForwardingRulesCommand(args: " + args + ") failed on setting one rule due to " + e.getMessage());
                results[i++] = "Failed";
                endResult = false;
            }
        }

        return new SetPortForwardingRulesAnswer(cmd, results, endResult);
    }

    protected Answer execute(CheckRouterCommand cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource CheckRouterCommand: " + s_gson.toJson(cmd));
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /opt/cloud/bin/checkrouter.sh ");
        }

        Pair<Boolean, String> result;
        try {

            String controlIp = getRouterSshControlIp(cmd);
            result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/checkrouter.sh ");

            if (!result.first()) {
                s_logger.error("check router command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " failed, message: " + result.second());

                return new CheckRouterAnswer(cmd, "CheckRouter failed due to " + result.second());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("check router command on domain router " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " completed");
            }
        } catch (Throwable e) {
            String msg = "CheckRouterCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new CheckRouterAnswer(cmd, msg);
        }
        return new CheckRouterAnswer(cmd, result.second(), true);
    }

    protected Answer execute(SetStaticNatRulesCommand cmd) {

        if (cmd.getVpcId() != null) {
            //return SetVPCStaticNatRules(cmd);
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource SetFirewallRuleCommand: " + s_gson.toJson(cmd));
        }

        String args = null;
        String[] results = new String[cmd.getRules().length];
        int i = 0;
        boolean endResult = true;
        for (StaticNatRuleTO rule : cmd.getRules()) {
            // 1:1 NAT needs instanceip;publicip;domrip;op
            args = rule.revoked() ? " -D " : " -A ";

            args += " -l " + rule.getSrcIp();
            args += " -r " + rule.getDstIp();

            if (rule.getProtocol() != null) {
                args += " -P " + rule.getProtocol().toLowerCase();
            }

            args += " -d " + rule.getStringSrcPortRange();
            args += " -G ";

            try {
                String controlIp = getRouterSshControlIp(cmd);
                Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/firewall.sh " + args);

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Executing script on domain router " + controlIp + ": /root/firewall.sh " + args);

                if (!result.first()) {
                    s_logger.error("SetStaticNatRulesCommand failure on setting one rule. args: " + args);
                    results[i++] = "Failed";
                    endResult = false;
                } else {
                    results[i++] = null;
                }
            } catch (Throwable e) {
                s_logger.error("SetStaticNatRulesCommand (args: " + args + ") failed on setting one rule due to " + e.getMessage());
                results[i++] = "Failed";
                endResult = false;
            }
        }
        return new SetStaticNatRulesAnswer(cmd, results, endResult);
    }

    protected Answer execute(PingTestCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource PingTestCommand: " + s_gson.toJson(cmd));
        }
        String controlIp = cmd.getRouterIp();
        String args = " -c 1 -n -q " + cmd.getPrivateIp();
        try {
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/bin/ping" + args);
            if (result.first())
                return new Answer(cmd);
        } catch (Exception e) {
            s_logger.error("Unable to execute ping command on DomR (" + controlIp + "), domR may not be ready yet. failure due to " + e.getMessage());
        }
        return new Answer(cmd, false, "PingTestCommand failed");
    }

    protected Answer execute(final DeleteIpAliasCommand cmd) {
        cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        List<IpAliasTO> revokedIpAliasTOs = cmd.getDeleteIpAliasTos();
        List<IpAliasTO> activeIpAliasTOs = cmd.getCreateIpAliasTos();
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing deleteIpAlias command: " + s_gson.toJson(cmd));
        }
        StringBuilder args = new StringBuilder();
        for (IpAliasTO ipAliasTO : revokedIpAliasTOs) {
            args.append(ipAliasTO.getAlias_count());
            args.append(":");
            args.append(ipAliasTO.getRouterip());
            args.append(":");
            args.append(ipAliasTO.getNetmask());
            args.append("-");
        }
        args.append("- ");
        for (IpAliasTO ipAliasTO : activeIpAliasTOs) {
            args.append(ipAliasTO.getAlias_count());
            args.append(":");
            args.append(ipAliasTO.getRouterip());
            args.append(":");
            args.append(ipAliasTO.getNetmask());
            args.append("-");
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /root/deleteIpAlias " + args);
        }

        try {
            String controlIp = getRouterSshControlIp(cmd);
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/deleteIpAlias.sh " + args);

            if (!result.first()) {
                s_logger.error("deleteIpAlias command on domr " + controlIp + " failed, message: " + result.second());

                return new Answer(cmd, false, "deleteIpAlias failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("deleteIpAlias command on domain router " + controlIp + " completed");
            }

        } catch (Throwable e) {
            String msg = "deleteIpAlias failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
    }

    protected Answer execute(final LoadBalancerConfigCommand cmd) {

        if (cmd.getVpcId() != null) {
            //return VPCLoadBalancerConfig(cmd);
        }

        File keyFile = getSystemVMKeyFile();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String controlIp = getRouterSshControlIp(cmd);

        assert (controlIp != null);

        LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();
        String[] config = cfgtr.generateConfiguration(cmd);

        String[][] rules = cfgtr.generateFwRules(cmd);
        String tmpCfgFilePath = "/tmp/" + routerIp.replace('.', '_') + ".cfg";
        StringBuilder tmpCfgFileContents = new StringBuilder();
        for (int i = 0; i < config.length; i++) {
            tmpCfgFileContents.append(config[i]);
            tmpCfgFileContents.append("\n");
        }

        try {
            SshHelper.scpTo(controlIp, DEFAULT_DOMR_SSHPORT, "root", keyFile, null, "/tmp/", tmpCfgFileContents.toString().getBytes(Charset.forName("UTF-8")), routerIp.replace('.', '_') +
                    ".cfg", null);

            try {
                String[] addRules = rules[LoadBalancerConfigurator.ADD];
                String[] removeRules = rules[LoadBalancerConfigurator.REMOVE];
                String[] statRules = rules[LoadBalancerConfigurator.STATS];

                String args = "";
                args += "-i " + routerIp;
                args += " -f " + tmpCfgFilePath;

                StringBuilder sb = new StringBuilder();
                if (addRules.length > 0) {
                    for (int i = 0; i < addRules.length; i++) {
                        sb.append(addRules[i]).append(',');
                    }

                    args += " -a " + sb.toString();
                }

                sb = new StringBuilder();
                if (removeRules.length > 0) {
                    for (int i = 0; i < removeRules.length; i++) {
                        sb.append(removeRules[i]).append(',');
                    }

                    args += " -d " + sb.toString();
                }

                sb = new StringBuilder();
                if (statRules.length > 0) {
                    for (int i = 0; i < statRules.length; i++) {
                        sb.append(statRules[i]).append(',');
                    }

                    args += " -s " + sb.toString();
                }

                Pair<Boolean, String> result =
                        SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "scp " + tmpCfgFilePath + " /etc/haproxy/haproxy.cfg.new");

                if (!result.first()) {
                    s_logger.error("Unable to copy haproxy configuration file");
                    return new Answer(cmd, false, "LoadBalancerConfigCommand failed due to uanble to copy haproxy configuration file");
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Run command on domain router " + routerIp + ",  /root/loadbalancer.sh " + args);
                }

                result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/loadbalancer.sh " + args);

                if (!result.first()) {
                    String msg = "LoadBalancerConfigCommand on domain router " + routerIp + " failed. message: " + result.second();
                    s_logger.error(msg);

                    return new Answer(cmd, false, msg);
                }

                if (s_logger.isInfoEnabled()) {
                    s_logger.info("LoadBalancerConfigCommand on domain router " + routerIp + " completed");
                }
            } finally {
                SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "rm " + tmpCfgFilePath);
            }

            return new Answer(cmd);
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString(), e);
            return new Answer(cmd, false, "LoadBalancerConfigCommand failed due to " + e.getMessage());
        }
    }

    protected Answer execute(SavePasswordCommand cmd) {
        if (s_logger.isInfoEnabled()) {

            s_logger.info("Executing resource SavePasswordCommand. vmName: " + cmd.getVmName() + ", vmIp: " + cmd.getVmIpAddress() + ", password: " +
                    StringUtils.getMaskedPasswordForDisplay(cmd.getPassword()));
        }

        String controlIp = getRouterSshControlIp(cmd);
        final String password = cmd.getPassword();
        final String vmIpAddress = cmd.getVmIpAddress();

        // Run save_password_to_domr.sh
        String args = " -v " + vmIpAddress;

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domain router " + controlIp + ", /opt/cloud/bin/savepassword.sh " + args + " -p " +
                    StringUtils.getMaskedPasswordForDisplay(cmd.getPassword()));
        }

        args += " -p " + password;

        try {

            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/savepassword.sh " +
                    args);

            if (!result.first()) {
                s_logger.error("savepassword command on domain router " + controlIp + " failed, message: " + result.second());

                return new Answer(cmd, false, "SavePassword failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("savepassword command on domain router " + controlIp + " completed");
            }

        } catch (Throwable e) {
            String msg = "SavePasswordCommand failed due to " + e;
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }
        return new Answer(cmd);
    }

    protected SetFirewallRulesAnswer execute(SetFirewallRulesCommand cmd) {
        String controlIp = getRouterSshControlIp(cmd);
        String[] results = new String[cmd.getRules().length];
        FirewallRuleTO[] allrules = cmd.getRules();
        FirewallRule.TrafficType trafficType = allrules[0].getTrafficType();
        String egressDefault = cmd.getAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT);

        String[][] rules = cmd.generateFwRules();
        String args = "";
        args += " -F ";
        if (trafficType == FirewallRule.TrafficType.Egress) {
            args += " -E ";
            if (egressDefault.equals("true")) {
                args += " -P 1 ";
            } else if (egressDefault.equals("System")) {
                args += " -P 2 ";
            } else {
                args += " -P 0 ";
            }
        }

        StringBuilder sb = new StringBuilder();
        String[] fwRules = rules[0];
        if (fwRules.length > 0) {
            for (int i = 0; i < fwRules.length; i++) {
                sb.append(fwRules[i]).append(',');
            }
            args += " -a " + sb.toString();
        }

        try {
            Pair<Boolean, String> result = null;

            if (trafficType == FirewallRule.TrafficType.Egress) {
                result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/firewallRule_egress.sh " + args);
            } else {
                result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/firewall_rule.sh " + args);
            }

            if (s_logger.isDebugEnabled()) {
                if (trafficType == FirewallRule.TrafficType.Egress) {
                    s_logger.debug("Executing script on domain router " + controlIp + ": /root/firewallRule_egress.sh " + args);
                } else {
                    s_logger.debug("Executing script on domain router " + controlIp + ": /root/firewall_rule.sh " + args);
                }
            }

            if (!result.first()) {
                s_logger.error("SetFirewallRulesCommand failure on setting one rule. args: " + args);
                //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
                for (int i = 0; i < results.length; i++) {
                    results[i] = "Failed";
                }

                return new SetFirewallRulesAnswer(cmd, false, results);
            }
        } catch (Throwable e) {
            s_logger.error("SetFirewallRulesCommand(args: " + args + ") failed on setting one rule due to ", e);
            //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
            for (int i = 0; i < results.length; i++) {
                results[i] = "Failed";
            }
            return new SetFirewallRulesAnswer(cmd, false, results);
        }

        return new SetFirewallRulesAnswer(cmd, true, results);
    }

    protected Answer execute(VmDataCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource VmDataCommand: " + s_gson.toJson(cmd));
        }
        String controlIp = getRouterSshControlIp(cmd);
        Map<String, List<String[]>> data = new HashMap<String, List<String[]>>();
        data.put(cmd.getVmIpAddress(), cmd.getVmData());

        String json = new Gson().toJson(data);
        s_logger.debug("VM data JSON IS:" + json);

        json = Base64.encodeBase64String(json.getBytes(Charset.forName("UTF-8")));

        String args = "-d " + json;

        try {
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/vmdata.py " + args);
            if (!result.first()) {
                s_logger.error("vm_data command on domain router " + controlIp + " failed. messge: " + result.second());
                return new Answer(cmd, false, "VmDataCommand failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("vm_data command on domain router " + controlIp + " completed");
            }
        } catch (Throwable e) {
            String msg = "VmDataCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }
        return new Answer(cmd);
    }

    protected Answer execute(DhcpEntryCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource DhcpEntryCommand: " + s_gson.toJson(cmd));
        }

        // ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domr "/root/edithosts.sh $mac $ip $vm $dfltrt $ns $staticrt" >/dev/null

        String args = " -m " + cmd.getVmMac();
        if (cmd.getVmIpAddress() != null) {
            args += " -4 " + cmd.getVmIpAddress();
        }
        args += " -h " + cmd.getVmName();

        if (cmd.getDefaultRouter() != null) {
            args += " -d " + cmd.getDefaultRouter();
        }

        if (cmd.getDefaultDns() != null) {
            args += " -n " + cmd.getDefaultDns();
        }

        if (cmd.getStaticRoutes() != null) {
            args += " -s " + cmd.getStaticRoutes();
        }

        if (cmd.getVmIp6Address() != null) {
            args += " -6 " + cmd.getVmIp6Address();
            args += " -u " + cmd.getDuid();
        }

        if (!cmd.isDefault()) {
            args += " -N";
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /root/edithosts.sh " + args);
        }

        try {
            String controlIp = getRouterSshControlIp(cmd);
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/edithosts.sh " + args);

            if (!result.first()) {
                s_logger.error("dhcp_entry command on domR " + controlIp + " failed, message: " + result.second());

                return new Answer(cmd, false, "DhcpEntry failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("dhcp_entry command on domain router " + controlIp + " completed");
            }

        } catch (Throwable e) {
            String msg = "DhcpEntryCommand failed due to " + e;
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
    }

    protected Answer execute(final CreateIpAliasCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing createIpAlias command: " + s_gson.toJson(cmd));
        }
        cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        List<IpAliasTO> ipAliasTOs = cmd.getIpAliasList();
        StringBuilder args = new StringBuilder();
        for (IpAliasTO ipaliasto : ipAliasTOs) {
            args.append(ipaliasto.getAlias_count());
            args.append(":");
            args.append(ipaliasto.getRouterip());
            args.append(":");
            args.append(ipaliasto.getNetmask());
            args.append("-");
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /root/createIpAlias " + args);
        }

        try {
            String controlIp = getRouterSshControlIp(cmd);
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/createIpAlias.sh " + args);

            if (!result.first()) {
                s_logger.error("CreateIpAlias command on domr " + controlIp + " failed, message: " + result.second());

                return new Answer(cmd, false, "createipAlias failed due to " + result.second());
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("createIpAlias command on domain router " + controlIp + " completed");
            }

        } catch (Throwable e) {
            String msg = "createIpAlias failed due to " + e;
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
    }

    protected Answer execute(final DnsMasqConfigCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing dnsmasqConfig command: " + s_gson.toJson(cmd));
        }
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String controlIp = getRouterSshControlIp(cmd);

        assert (controlIp != null);

        List<DhcpTO> dhcpTos = cmd.getIps();
        StringBuilder args = new StringBuilder();
        for (DhcpTO dhcpTo : dhcpTos) {
            args.append(dhcpTo.getRouterIp());
            args.append(":");
            args.append(dhcpTo.getGateway());
            args.append(":");
            args.append(dhcpTo.getNetmask());
            args.append(":");
            args.append(dhcpTo.getStartIpOfSubnet());
            args.append("-");
        }

        try {
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/dnsmasq.sh " + args);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Run command on domain router " + routerIp + ",  /root/dnsmasq.sh");
            }

            if (!result.first()) {
                s_logger.error("Unable update dnsmasq config file");
                return new Answer(cmd, false, "dnsmasq config update failed due to: " + result.second());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("dnsmasq config command on domain router " + routerIp + " completed");
            }
        } catch (Throwable e) {
            String msg = "Dnsmasqconfig command failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
    }

    //
    // find mac address of a specified ethx device
    //    ip address show ethx | grep link/ether | sed -e 's/^[ \t]*//' | cut -d' ' -f2
    // returns
    //      eth0:xx.xx.xx.xx

    //
    // list IP with eth devices
    //  ifconfig ethx |grep -B1 "inet addr" | awk '{ if ( $1 == "inet" ) { print $2 } else if ( $2 == "Link" ) { printf "%s:" ,$1 } }'
    //     | awk -F: '{ print $1 ": " $3 }'
    //
    // returns
    //      eth0:xx.xx.xx.xx
    //
    //

    private int findRouterEthDeviceIndex(String domrName, String routerIp, String mac) throws Exception {

        s_logger.info("findRouterEthDeviceIndex. mac: " + mac);

        // TODO : this is a temporary very inefficient solution, will refactor it later
        Pair<Boolean, String> result = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                "ls /proc/sys/net/ipv4/conf");

        // when we dynamically plug in a new NIC into virtual router, it may take time to show up in guest OS
        // we use a waiting loop here as a workaround to synchronize activities in systems
        long startTick = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTick < 15000) {
            if (result.first()) {
                String[] tokens = result.second().split("\\s+");
                for (String token : tokens) {
                    if (!("all".equalsIgnoreCase(token) || "default".equalsIgnoreCase(token) || "lo".equalsIgnoreCase(token))) {
                        String cmd = String.format("ip address show %s | grep link/ether | sed -e 's/^[ \t]*//' | cut -d' ' -f2", token);

                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Run domr script " + cmd);
                        Pair<Boolean, String> result2 = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                                // TODO need to find the dev index inside router based on IP address
                                cmd);
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("result: " + result2.first() + ", output: " + result2.second());

                        if (result2.first() && result2.second().trim().equalsIgnoreCase(mac.trim()))
                            return Integer.parseInt(token.substring(3));
                    }
                }
            }

            s_logger.warn("can not find intereface associated with mac: " + mac + ", guest OS may still at loading state, retry...");

        }

        return -1;
    }

    private Pair<Integer, String> findRouterFreeEthDeviceIndex(String routerIp) throws Exception {

        s_logger.info("findRouterFreeEthDeviceIndex. mac: ");

        // TODO : this is a temporary very inefficient solution, will refactor it later
        Pair<Boolean, String> result = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                "ip address | grep DOWN| cut -f2 -d :");

        // when we dynamically plug in a new NIC into virtual router, it may take time to show up in guest OS
        // we use a waiting loop here as a workaround to synchronize activities in systems
        long startTick = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTick < 15000) {
            if (result.first() && !result.second().isEmpty()) {
                String[] tokens = result.second().split("\\n");
                for (String token : tokens) {
                    if (!("all".equalsIgnoreCase(token) || "default".equalsIgnoreCase(token) || "lo".equalsIgnoreCase(token))) {
                        //String cmd = String.format("ip address show %s | grep link/ether | sed -e 's/^[ \t]*//' | cut -d' ' -f2", token);
                        //TODO: don't check for eth0,1,2, as they will be empty by default.
                        //String cmd = String.format("ip address show %s ", token);
                        String cmd = String.format("ip address show %s | grep link/ether | sed -e 's/^[ \t]*//' | cut -d' ' -f2", token);
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Run domr script " + cmd);
                        Pair<Boolean, String> result2 = SshHelper.sshExecute(routerIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                                // TODO need to find the dev index inside router based on IP address
                                cmd);
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("result: " + result2.first() + ", output: " + result2.second());

                        if (result2.first() && result2.second().trim().length() > 0)
                            return new Pair<Integer, String>(Integer.parseInt(token.trim().substring(3)), result2.second().trim()) ;
                    }
                }
            }

            //s_logger.warn("can not find intereface associated with mac: , guest OS may still at loading state, retry...");

        }

        return new Pair<Integer, String>(-1, "");
    }

    protected Answer execute(IpAssocCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource IPAssocCommand: " + s_gson.toJson(cmd));
        }

        int i = 0;
        String[] results = new String[cmd.getIpAddresses().length];

        try {

            IpAddressTO[] ips = cmd.getIpAddresses();
            String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
            String controlIp = getRouterSshControlIp(cmd);
            for (IpAddressTO ip : ips) {
                assignPublicIpAddress(routerName, controlIp, ip.getPublicIp(), ip.isAdd(), ip.isFirstIP(), ip.isSourceNat(), ip.getBroadcastUri(), ip.getVlanGateway(),
                        ip.getVlanNetmask(), ip.getVifMacAddress());
                results[i++] = ip.getPublicIp() + " - success";
            }

            for (; i < cmd.getIpAddresses().length; i++) {
                results[i++] = IpAssocAnswer.errorResult;
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString() + " will shortcut rest of IPAssoc commands", e);

            for (; i < cmd.getIpAddresses().length; i++) {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }

        return new IpAssocAnswer(cmd, results);
    }


    protected int getVmFreeNicIndex(String vmName) {
        GetVmConfigCommand vmConfig = new GetVmConfigCommand(vmName);
        URI agentUri = null;
        int nicposition = -1;
        try {
            String cmdName = GetVmConfigCommand.class.getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
        }
        String ansStr = postHttpRequest(s_gson.toJson(vmConfig), agentUri);
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("GetVmConfigCommand response received "
                + s_gson.toJson(result));
        if (result.length > 0) {
            GetVmConfigAnswer ans = ((GetVmConfigAnswer)result[0]);
            List<NicDetails> nics = ans.getNics();
            for (NicDetails nic : nics) {
                if (nic.getState() == false) {
                    nicposition = nics.indexOf(nic);
                    break;
                }
            }
        }
        return nicposition;
    }

    protected int getVmNics(String vmName, String vlanid) {
        GetVmConfigCommand vmConfig = new GetVmConfigCommand(vmName);
        URI agentUri = null;
        int nicposition = -1;
        if(vlanid.equalsIgnoreCase("untagged")) {
            vlanid = "-1";
        }
        try {
            String cmdName = GetVmConfigCommand.class.getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
        }
        String ansStr = postHttpRequest(s_gson.toJson(vmConfig), agentUri);
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("executeRequest received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
            GetVmConfigAnswer ans = ((GetVmConfigAnswer)result[0]);
            List<NicDetails> nics = ans.getNics();
            for (NicDetails nic : nics) {
                nicposition++;
                if (nicposition > 1 && nic.getVlanid().equals(vlanid)) {
                    break;
                }
            }
        }
        return nicposition;
    }

    protected void modifyNicVlan(String vmName, String vlanId, String macAddress) {
        ModifyVmNicConfigCommand modifynic = new ModifyVmNicConfigCommand(vmName, vlanId, macAddress);
        URI agentUri = null;
        try {
            String cmdName = ModifyVmNicConfigCommand.class.getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
        }
        String ansStr = postHttpRequest(s_gson.toJson(modifynic), agentUri);
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("executeRequest received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
        }
    }

    protected void modifyNicVlan(String vmName, String vlanId, int pos, boolean enable, String switchLabelName) {
        ModifyVmNicConfigCommand modifyNic = new ModifyVmNicConfigCommand(vmName, vlanId, pos, enable);
        modifyNic.setSwitchLableName(switchLabelName);
        URI agentUri = null;
        try {
            String cmdName = ModifyVmNicConfigCommand.class.getName();
            agentUri =
                    new URI("https", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
        }
        String ansStr = postHttpRequest(s_gson.toJson(modifyNic), agentUri);
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("executeRequest received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
        }
    }

    protected void assignPublicIpAddress(final String vmName, final String privateIpAddress, final String publicIpAddress, final boolean add, final boolean firstIP,
            final boolean sourceNat, final String broadcastId, final String vlanGateway, final String vlanNetmask, final String vifMacAddress) throws Exception {

        URI broadcastUri = BroadcastDomainType.fromString(broadcastId);
        if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
            throw new InternalErrorException("Unable to assign a public IP to a VIF on network " + broadcastId);
        }
        String vlanId = BroadcastDomainType.getValue(broadcastUri);

        int publicNicInfo = -1;
        publicNicInfo = getVmNics(vmName, vlanId);

        boolean addVif = false;
        if (add && publicNicInfo == -1) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Plug new NIC to associate" + privateIpAddress + " to " + publicIpAddress);
            }
            addVif = true;
        } else if (!add && firstIP) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unplug NIC " + publicNicInfo);
            }
        }

        if (addVif) {
            Pair<Integer, String> nicdevice = findRouterFreeEthDeviceIndex(privateIpAddress);
            publicNicInfo = nicdevice.first();
            if (publicNicInfo > 0) {
                modifyNicVlan(vmName, vlanId, nicdevice.second());
                // After modifying the vnic on VR, check the VR VNics config in the host and get the device position
                publicNicInfo = getVmNics(vmName, vlanId);
                // As a new nic got activated in the VR. add the entry in the NIC's table.
                networkUsage(privateIpAddress, "addVif", "eth" + publicNicInfo);
            }
            else {
                // we didn't find any eth device available in VR to configure the ip range with new VLAN
                String msg = "No Nic is available on DomR VIF to associate/disassociate IP with.";
                s_logger.error(msg);
                throw new InternalErrorException(msg);
            }
        }

        String args = null;

        if (add) {
            args = " -A ";
        } else {
            args = " -D ";
        }

        if (sourceNat) {
            args += " -s ";
        }
        if (firstIP) {
            args += " -f ";
        }
        String cidrSize = Long.toString(NetUtils.getCidrSize(vlanNetmask));
        args += " -l ";
        args += publicIpAddress + "/" + cidrSize;

        args += " -c ";

        args += "eth" + publicNicInfo;
        args += " -g ";
        args += vlanGateway;

        if (addVif) {
            args += " -n ";
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domain router " + privateIpAddress + ", /opt/cloud/bin/ipassoc.sh " + args);
        }

        Pair<Boolean, String> result =
                SshHelper.sshExecute(privateIpAddress, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/ipassoc.sh " + args);

        if (!result.first()) {
            s_logger.error("ipassoc command on domain router " + privateIpAddress + " failed. message: " + result.second());
            throw new Exception("ipassoc failed due to " + result.second());
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("ipassoc command on domain router " + privateIpAddress + " completed");
        }
    }

    protected Answer execute(GetDomRVersionCmd cmd) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Executing resource GetDomRVersionCmd: " + s_gson.toJson(cmd));
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /opt/cloud/bin/get_template_version.sh ");
        }

        Pair<Boolean, String> result;
        try {
            String controlIp = getRouterSshControlIp(cmd);
            result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/get_template_version.sh ");

            if (!result.first()) {
                s_logger.error("GetDomRVersionCmd on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " failed, message: " + result.second());

                return new GetDomRVersionAnswer(cmd, "GetDomRVersionCmd failed due to " + result.second());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("GetDomRVersionCmd on domain router " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + " completed");
            }
        } catch (Throwable e) {
            String msg = "GetDomRVersionCmd failed due to " + e;
            s_logger.error(msg, e);
            return new GetDomRVersionAnswer(cmd, msg);
        }
        String[] lines = result.second().split("&");
        if (lines.length != 2) {
            return new GetDomRVersionAnswer(cmd, result.second());
        }
        return new GetDomRVersionAnswer(cmd, result.second(), lines[0], lines[1]);
    }

    private static String getRouterSshControlIp(NetworkElementCommand cmd) {
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String routerGuestIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        String zoneNetworkType = cmd.getAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE);

        if (routerGuestIp != null && zoneNetworkType != null && NetworkType.valueOf(zoneNetworkType) == NetworkType.Basic) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("In Basic zone mode, use router's guest IP for SSH control. guest IP : " + routerGuestIp);

            return routerGuestIp;
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Use router's private IP for SSH control. IP : " + routerIp);
        return routerIp;
    }

    protected Answer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            //return VPCNetworkUsage(cmd);
        }
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource NetworkUsageCommand " + s_gson.toJson(cmd));
        }
        if (cmd.getOption() != null && cmd.getOption().equals("create")) {
            networkUsage(cmd.getPrivateIP(), "create", null);
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "true", 0L, 0L);
            return answer;
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP());

        NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
        return answer;
    }

    private long[] getNetworkStats(String privateIP) {
        String result = networkUsage(privateIP, "get", null);
        long[] stats = new long[2];
        if (result != null) {
            try {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += Long.parseLong(splitResult[i++]);
                    stats[1] += Long.parseLong(splitResult[i++]);
                }
            } catch (Throwable e) {
                s_logger.warn("Unable to parse return from script return of network usage command: " + e.toString(), e);
            }
        }
        return stats;
    }

    protected Answer execute(SetMonitorServiceCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource SetMonitorServiceCommand: " + s_gson.toJson(cmd));
        }

        String controlIp = getRouterSshControlIp(cmd);
        String config = cmd.getConfiguration();

        String args = "";

        args += " -c " + config;

        try {
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/monitor_service.sh " + args);

            if (!result.first()) {
                String msg=  "monitor_service.sh failed on domain router " + controlIp + " failed " + result.second();
                s_logger.error(msg);
                return new Answer(cmd, false, msg);
            }

            return new Answer(cmd);

        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString(), e);
            return new Answer(cmd, false, "SetMonitorServiceCommand failed due to " + e);
        }
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        try {
            String result = connect(cmd.getName(), privateIp, cmdPort);
            if (result != null) {
                s_logger.error("Can not ping System vm " + vmName + "due to:" + result);
                return new CheckSshAnswer(cmd, "Can not ping System vm " + vmName + "due to:" + result);
            }
        } catch (Exception e) {
            s_logger.error("Can not ping System vm " + vmName + "due to exception");
            return new CheckSshAnswer(cmd, e);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        if (VirtualMachineName.isValidRouterName(vmName)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Execute network usage setup command on " + vmName);
            }
            networkUsage(privateIp, "create", null);
        }

        return new CheckSshAnswer(cmd);
    }

    protected String networkUsage(final String privateIpAddress, final String option, final String ethName) {
        String args = null;
        if (option.equals("get")) {
            args = "-g";
        } else if (option.equals("create")) {
            args = "-c";
        } else if (option.equals("reset")) {
            args = "-r";
        } else if (option.equals("addVif")) {
            args = "-a";
            args += ethName;
        } else if (option.equals("deleteVif")) {
            args = "-d";
            args += ethName;
        }

        try {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Executing /opt/cloud/bin/netusage.sh " + args + " on DomR " + privateIpAddress);
            }

            Pair<Boolean, String> result =
                    SshHelper.sshExecute(privateIpAddress, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/netusage.sh " + args);

            if (!result.first()) {
                return null;
            }

            return result.second();
        } catch (Throwable e) {
            s_logger.error("Unable to execute NetworkUsage command on DomR (" + privateIpAddress + "), domR may not be ready yet. failure due to " + e);
        }

        return null;
    }

    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader().getResource("scripts/vm/systemvm/id_rsa.cloud");
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File("/usr/share/cloudstack-common/scripts/vm/systemvm/id_rsa.cloud");
        }
        assert (keyFile != null);
        if (!keyFile.exists()) {
            s_logger.error("Unable to locate id_rsa.cloud in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    public static String postHttpRequest(final String jsonCmd, final URI agentUri) {
        // Using Apache's HttpClient for HTTP POST
        // Java-only approach discussed at on StackOverflow concludes with
        // comment to use Apache HttpClient
        // http://stackoverflow.com/a/2793153/939250, but final comment is to
        // use Apache.
        String logMessage = StringEscapeUtils.unescapeJava(jsonCmd);
        logMessage = cleanPassword(logMessage);
        s_logger.debug("POST request to " + agentUri.toString()
                + " with contents " + logMessage);

        // Create request
        HttpClient httpClient = null;
        TrustStrategy easyStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                return true;
            }
        };

        try {
            SSLSocketFactory sf = new SSLSocketFactory(easyStrategy, new AllowAllHostnameVerifier());
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("https", DEFAULT_AGENT_PORT, sf));
            ClientConnectionManager ccm = new BasicClientConnectionManager(registry);
            httpClient = new DefaultHttpClient(ccm);
        } catch (KeyManagementException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (UnrecoverableKeyException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (KeyStoreException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        }

        String result = null;

        // TODO: are there timeout settings and worker thread settings to tweak?
        try {
            HttpPost request = new HttpPost(agentUri);

            // JSON encode command
            // Assumes command sits comfortably in a string, i.e. not used for
            // large data transfers
            StringEntity cmdJson = new StringEntity(jsonCmd);
            request.addHeader("content-type", "application/json");
            request.setEntity(cmdJson);
            s_logger.debug("Sending cmd to " + agentUri.toString()
                    + " cmd data:" + logMessage);
            HttpResponse response = httpClient.execute(request);

            // Unsupported commands will not route.
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String errMsg = "Failed to send : HTTP error code : " + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                String unsupportMsg = "Unsupported command " + agentUri.getPath() + ".  Are you sure you got the right type of" + " server?";
                Answer ans = new UnsupportedAnswer(null, unsupportMsg);
                s_logger.error(ans);
                result = s_gson.toJson(new Answer[] {ans});
            } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errMsg = "Failed send to " + agentUri.toString() + " : HTTP error code : " + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                return null;
            } else {
                result = EntityUtils.toString(response.getEntity());
                String logResult = cleanPassword(StringEscapeUtils.unescapeJava(result));
                s_logger.debug("POST response is " + logResult);
            }
        } catch (ClientProtocolException protocolEx) {
            // Problem with HTTP message exchange
            s_logger.error(protocolEx);
        } catch (IOException connEx) {
            // Problem with underlying communications
            s_logger.error(connEx);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return result;
    }

    @Override
    protected final String getDefaultScriptsDir() {
        // TODO Auto-generated method stub
        return null;
    }

    // NB: 'params' can come from one of two places.
    // For a new host, HypervServerDiscoverer.find().
    // For an existing host, DiscovererBase.reloadResource().
    // In the later case, the params Map is populated with predefined keys
    // and custom keys from the database that were passed out by the find()
    // call.
    // the custom keys go by the variable name 'details'.
    // Thus, in find(), you see that 'details' are added to the params Map.
    @Override
    public final boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        /* todo: update, make consistent with the xen server equivalent. */

        if (params != null) {
            _guid = (String)params.get("guid");
            _zoneId = (String)params.get("zone");
            _podId = (String)params.get("pod");
            _clusterId = (String)params.get("cluster");
            _agentIp = (String)params.get("ipaddress"); // was agentIp
            _name = name;

            _clusterGuid = (String)params.get("cluster.guid");
            _username = (String)params.get("url");
            _password = (String)params.get("password");
            _username = (String)params.get("username");
            _configureCalled = true;
        }

        _vrResource = new VirtualRoutingResource(this);
        if (!_vrResource.configure(name, new HashMap<String, Object>())) {
            throw new ConfigurationException("Unable to configure VirtualRoutingResource");
        }
        return true;
    }

    @Override
    public void setName(final String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
        // TODO Auto-generated method stub
    }

    @Override
    public final Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
        // TODO Auto-generated method stub
    }

    protected String connect(final String vmName, final String ipAddress, final int port) {
        long startTick = System.currentTimeMillis();

        // wait until we have at least been waiting for _ops_timeout time or
        // at least have tried _retry times, this is to coordinate with system
        // VM patching/rebooting time that may need
        int retry = _retry;
        while (System.currentTimeMillis() - startTick <= _opsTimeout || --retry > 0) {
            s_logger.info("Trying to connect to " + ipAddress);
            try (SocketChannel sch = SocketChannel.open();) {
                sch.configureBlocking(true);
                sch.socket().setSoTimeout(5000);
                // we need to connect to the control ip address to check the status of the system vm
                InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (IOException e) {
                s_logger.info("Could] not connect to " + ipAddress + " due to " + e.toString());
                if (e instanceof ConnectException) {
                    // if connection is refused because of VM is being started,
                    // we give it more sleep time
                    // to avoid running out of retry quota too quickly
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        s_logger.debug("[ignored] interupted while waiting to retry connecting to vm after exception: "+e.getLocalizedMessage());
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                s_logger.debug("[ignored] interupted while connecting to vm.");
            }
        }

        s_logger.info("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

    public static String cleanPassword(String logString) {
        String cleanLogString = null;
        if (logString != null) {
            cleanLogString = logString;
            String[] temp = logString.split(",");
            int i = 0;
            if (temp != null) {
                while (i < temp.length) {
                    temp[i] = StringUtils.cleanString(temp[i]);
                    i++;
                }
                List<String> stringList = new ArrayList<String>();
                Collections.addAll(stringList, temp);
                cleanLogString = StringUtils.join(stringList, ",");
            }
        }
        return cleanLogString;
    }
}
