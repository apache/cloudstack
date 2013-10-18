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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.api.StartupRoutingCommand.VmState;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesAnswer;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.network.rules.FirewallRule;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.VirtualMachineName;
import com.google.gson.Gson;

/**
 * Implementation of dummy resource to be returned from discoverer.
 **/

public class HypervDirectConnectResource extends ServerResourceBase implements
        ServerResource {
    public static final int DEFAULT_AGENT_PORT = 8250;
    private static final Logger s_logger = Logger
            .getLogger(HypervDirectConnectResource.class.getName());

    private static final Gson s_gson = GsonHelper.getGson();
    private String _zoneId;
    private String _podId;
    private String _clusterId;
    private String _guid;
    private String _agentIp;
    private int _port = DEFAULT_AGENT_PORT;
    protected final long _ops_timeout = 900000;  // 15 minutes time out to time

    protected final int _retry = 24;
    protected final int _sleep = 10000;
    protected final int DEFAULT_DOMR_SSHPORT = 3922;

    private String _clusterGuid;

    // Used by initialize to assert object configured before
    // initialize called.
    private boolean _configureCalled = false;

    private String _username;
    private String _password;

    @Override
    public final Type getType() {
        return Type.Routing;
    }

    @Override
    public final StartupCommand[] initialize() {
        // assert
        if (!_configureCalled) {
            String errMsg =
                    this.getClass().getName()
                            + " requires configure() be called before"
                            + " initialize()";
            s_logger.error(errMsg);
        }

        // Create default StartupRoutingCommand, then customise
        StartupRoutingCommand defaultStartRoutCmd =
                new StartupRoutingCommand(0, 0, 0, 0, null,
                        Hypervisor.HypervisorType.Hyperv,
                        RouterPrivateIpStrategy.HostLocal,
                        new HashMap<String, VmState>());

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

        s_logger.debug("Generated StartupRoutingCommand for _agentIp \""
                + _agentIp + "\"");

        // TODO: does version need to be hard coded.
        defaultStartRoutCmd.setVersion("4.2.0");

        // Specifics of the host's resource capacity and network configuration
        // comes from the host itself. CloudStack sanity checks network
        // configuration
        // and uses capacity info for resource allocation.
        Command[] startCmds =
                requestStartupCommand(new Command[] {defaultStartRoutCmd});

        // TODO: may throw, is this okay?
        StartupRoutingCommand startCmd = (StartupRoutingCommand) startCmds[0];

        // Assert that host identity is consistent with existing values.
        if (startCmd == null) {
            String errMsg =
                    String.format("Host %s (IP %s)"
                            + "did not return a StartupRoutingCommand",
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getDataCenter().equals(
                defaultStartRoutCmd.getDataCenter())) {
            String errMsg =
                    String.format(
                            "Host %s (IP %s) changed zone/data center.  Was "
                                    + defaultStartRoutCmd.getDataCenter()
                                    + " NOW its " + startCmd.getDataCenter(),
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPod().equals(defaultStartRoutCmd.getPod())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed pod.  Was "
                            + defaultStartRoutCmd.getPod() + " NOW its "
                            + startCmd.getPod(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getCluster().equals(defaultStartRoutCmd.getCluster())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed cluster.  Was "
                            + defaultStartRoutCmd.getCluster() + " NOW its "
                            + startCmd.getCluster(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getGuid().equals(defaultStartRoutCmd.getGuid())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed guid.  Was "
                            + defaultStartRoutCmd.getGuid() + " NOW its "
                            + startCmd.getGuid(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPrivateIpAddress().equals(
                defaultStartRoutCmd.getPrivateIpAddress())) {
            String errMsg =
                    String.format("Host %s (IP %s) IP address.  Was "
                            + defaultStartRoutCmd.getPrivateIpAddress()
                            + " NOW its " + startCmd.getPrivateIpAddress(),
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getName().equals(defaultStartRoutCmd.getName())) {
            String errMsg =
                    String.format(
                            "Host %s (IP %s) name.  Was " + startCmd.getName()
                                    + " NOW its "
                                    + defaultStartRoutCmd.getName(), _name,
                            _agentIp);
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
            storePoolCmd = (StartupStorageCommand) startCmds[1];
            // TODO: is this assertion required?
            if (storePoolCmd == null) {
                String frmtStr =
                        "Host %s (IP %s) sent incorrect Command, "
                                + "second parameter should be a "
                                + "StartupStorageCommand";
                String errMsg = String.format(frmtStr, _name, _agentIp);
                s_logger.error(errMsg);
                // TODO: valid to return null, or should we throw?
                return null;
            }
            s_logger.info("Host " + _name + " (IP " + _agentIp
                    + ") already configured with a storeage pool, details "
                    + s_gson.toJson(startCmds[1]));
        } else {
            s_logger.info("Host " + _name + " (IP " + _agentIp
                    + ") already configured with a storeage pool, details ");
        }
        return new StartupCommand[] {startCmd, storePoolCmd};
    }

    @Override
    public final PingCommand getCurrentStatus(final long id) {
        PingCommand pingCmd = new PingRoutingCommand(getType(), id, null);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping host " + _name + " (IP " + _agentIp + ")");
        }

        Answer pingAns = this.executeRequest(pingCmd);

        if (pingAns == null || !pingAns.getResult()) {
            s_logger.info("Cannot ping host " + _name + " (IP " + _agentIp
                    + "), pingAns (blank means null) is:" + pingAns);
            return null;
        }
        return pingCmd;
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
                    new URI("http", null, _agentIp, _port,
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
        s_logger.debug("requestStartupCommand received response "
                + s_gson.toJson(result));
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
                    new URI("http", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            // TODO add proper logging
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }

        if (clazz == CheckSshCommand.class) {
            answer = execute((CheckSshCommand) cmd);
        } else if (clazz == GetDomRVersionCmd.class) {
            answer = execute((GetDomRVersionCmd)cmd);
        } else if (cmd instanceof NetworkUsageCommand) {
           answer = execute((NetworkUsageCommand) cmd);
        } else if (clazz == IpAssocCommand.class) {
           answer = execute((IpAssocCommand) cmd);
        } else if (clazz == DnsMasqConfigCommand.class) {
            return execute((DnsMasqConfigCommand) cmd);
        } else if (clazz == CreateIpAliasCommand.class) {
            return execute((CreateIpAliasCommand) cmd);
        } else if (clazz == DhcpEntryCommand.class) {
            answer = execute((DhcpEntryCommand) cmd);
        } else if (clazz == VmDataCommand.class) {
            answer = execute((VmDataCommand) cmd);
        } else if (clazz == SavePasswordCommand.class) {
            answer = execute((SavePasswordCommand) cmd);
        } else  if (clazz == SetFirewallRulesCommand.class) {
            answer = execute((SetFirewallRulesCommand)cmd);
        } 

        else {

        // Else send the cmd to hyperv agent.

        String ansStr = postHttpRequest(s_gson.toJson(cmd), agentUri);

        if (ansStr == null) {
            // return null;
           return Answer.createUnsupportedCommandAnswer(cmd);
        }
        // Only Answer instances are returned by remote agents.
        // E.g. see Response.getAnswers()
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("executeRequest received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
            return result[0];
        }
        }
        return answer;
    }

    protected Answer execute(SavePasswordCommand cmd) {
        if (s_logger.isInfoEnabled()) {

            s_logger.info("Executing resource SavePasswordCommand. vmName: " + cmd.getVmName() + ", vmIp: " + cmd.getVmIpAddress() + ", password: "
                    + StringUtils.getMaskedPasswordForDisplay(cmd.getPassword()));
        }

        String controlIp = getRouterSshControlIp(cmd);
        final String password = cmd.getPassword();
        final String vmIpAddress = cmd.getVmIpAddress();

        // Run save_password_to_domr.sh
        String args = " -v " + vmIpAddress;

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domain router " + controlIp + ", /root/savepassword.sh " + args + " -p " + StringUtils.getMaskedPasswordForDisplay(cmd.getPassword()));
        }

        args += " -p " + password;


        try {

            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/root/savepassword.sh " + args);

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
        if (trafficType == FirewallRule.TrafficType.Egress){
            args+= " -E ";
            if (egressDefault.equals("true")) {
                args+= " -P 1 ";
            } else if (egressDefault.equals("System")) {
                args+= " -P 2 ";
            } else {
                args+= " -P 0 ";
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

            if (trafficType == FirewallRule.TrafficType.Egress){
                result = SshHelper.sshExecute(controlIp,
                        DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(),
                        null, "/root/firewallRule_egress.sh " + args);
            } else {
                result = SshHelper.sshExecute(controlIp,
                        DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(),
                        null, "/root/firewall_rule.sh " + args);
            }

            if (s_logger.isDebugEnabled()) {
                if (trafficType == FirewallRule.TrafficType.Egress){
                    s_logger.debug("Executing script on domain router " + controlIp
                            + ": /root/firewallRule_egress.sh " + args);
                } else {
                    s_logger.debug("Executing script on domain router " + controlIp
                            + ": /root/firewall_rule.sh " + args);
                }
            }


            if (!result.first()) {
                s_logger.error("SetFirewallRulesCommand failure on setting one rule. args: "
                        + args);
                //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
                for (int i=0; i < results.length; i++) {
                    results[i] = "Failed";
                }

                return new SetFirewallRulesAnswer(cmd, false, results);
            }
        } catch (Throwable e) {
            s_logger.error("SetFirewallRulesCommand(args: " + args
                    + ") failed on setting one rule due to "
                     ,e);
            //FIXME - in the future we have to process each rule separately; now we temporarily set every rule to be false if single rule fails
            for (int i=0; i < results.length; i++) {
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

        String routerPrivateIpAddress = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String controlIp = getRouterSshControlIp(cmd);

        String vmIpAddress = cmd.getVmIpAddress();
        List<String[]> vmData = cmd.getVmData();
        String[] vmDataArgs = new String[vmData.size() * 2 + 4];
        vmDataArgs[0] = "routerIP";
        vmDataArgs[1] = routerPrivateIpAddress;
        vmDataArgs[2] = "vmIP";
        vmDataArgs[3] = vmIpAddress;
        int i = 4;
        for (String[] vmDataEntry : vmData) {
            String folder = vmDataEntry[0];
            String file = vmDataEntry[1];
            String contents = (vmDataEntry[2] != null) ? vmDataEntry[2] : "none";

            vmDataArgs[i] = folder + "," + file;
            vmDataArgs[i + 1] = contents;
            i += 2;
        }

        String content = encodeDataArgs(vmDataArgs);
        String tmpFileName = UUID.randomUUID().toString();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run vm_data command on domain router " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", data: " + content);
        }

        try {
            SshHelper.scpTo(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/tmp", content.getBytes(), tmpFileName, null);

            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                        "/root/userdata.py " + tmpFileName);

                if (!result.first()) {
                    s_logger.error("vm_data command on domain router " + controlIp + " failed. messge: " + result.second());
                    return new Answer(cmd, false, "VmDataCommand failed due to " + result.second());
                }
            } finally {

                SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "rm /tmp/" + tmpFileName);
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("vm_data command on domain router " + controlIp + " completed");
            }

        } catch (Throwable e) {
            String msg = "VmDataCommand failed due to " + e;
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }
        return new Answer(cmd);
    }


    private String encodeDataArgs(String[] dataArgs) {
        StringBuilder sb = new StringBuilder();

        for (String arg : dataArgs) {
            sb.append(arg);
            sb.append("\n");
        }

        return sb.toString();
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
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                    "/root/edithosts.sh " + args);

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
        String args="";
        for (IpAliasTO ipaliasto : ipAliasTOs) {
            args = args + ipaliasto.getAlias_count()+":"+ipaliasto.getRouterip()+":"+ipaliasto.getNetmask()+"-";
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domR " + cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP) + ", /root/createIpAlias " + args);
        }

        try {
            String controlIp = getRouterSshControlIp(cmd);
            Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                    "/root/createIpAlias.sh " + args);

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

        assert(controlIp != null);

        List<DhcpTO> dhcpTos = cmd.getIps();
        String args ="";
        for(DhcpTO dhcpTo : dhcpTos) {
            args = args + dhcpTo.getRouterIp()+":"+dhcpTo.getGateway()+":"+dhcpTo.getNetmask()+":"+dhcpTo.getStartIpOfSubnet()+"-";
        }
        //File keyFile = mgr.getSystemVMKeyFile();

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
        }catch (Throwable e) {
            String msg = "Dnsmasqconfig command failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
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
                assignPublicIpAddress(routerName, controlIp, ip.getPublicIp(), ip.isAdd(), ip.isFirstIP(), ip.isSourceNat(), ip.getBroadcastUri(), ip.getVlanGateway(), ip.getVlanNetmask(),ip.getVifMacAddress());
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

    protected void assignPublicIpAddress(final String vmName, final String privateIpAddress, final String publicIpAddress, final boolean add, final boolean firstIP,
            final boolean sourceNat, final String vlanId, final String vlanGateway, final String vlanNetmask, final String vifMacAddress) throws Exception {

        //String publicNeworkName = HypervisorHostHelper.getPublicNetworkNamePrefix(vlanId);
        //Pair<Integer, VirtualDevice> publicNicInfo = vmMo.getNicDeviceIndex(publicNeworkName);

        if (s_logger.isDebugEnabled()) {
            //s_logger.debug("Find public NIC index, public network name: " + publicNeworkName + ", index: " + publicNicInfo.first());
        }

        boolean addVif = false;
        boolean removeVif = false;
        if (add ) { // && publicNicInfo.first().intValue() == -1) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Plug new NIC to associate" + privateIpAddress + " to " + publicIpAddress);
            }

            addVif = true;
        } else if (!add && firstIP) {
            removeVif = true;

            if (s_logger.isDebugEnabled()) {
                //s_logger.debug("Unplug NIC " + publicNicInfo.first());
            }
        }

/*        if (addVif) {
            plugPublicNic(vmMo, vlanId, vifMacAddress);
            publicNicInfo = vmMo.getNicDeviceIndex(publicNeworkName);
            if (publicNicInfo.first().intValue() >= 0) {
                networkUsage(privateIpAddress, "addVif", "eth" + publicNicInfo.first());
            }
        }
*/
/*        if (publicNicInfo.first().intValue() < 0) {
            String msg = "Failed to find DomR VIF to associate/disassociate IP with.";
            s_logger.error(msg);
            throw new InternalErrorException(msg);
        }
*/
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
        args += "eth" +"2";  // currently hardcoding to eth 2 (which is default public ipd)//publicNicInfo.first();

        args += " -g ";
        args += vlanGateway;

        if (addVif) {
            args += " -n ";
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on domain router " + privateIpAddress + ", /opt/cloud/bin/ipassoc.sh " + args);
        }

        Pair<Boolean, String> result = SshHelper.sshExecute(privateIpAddress, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/ipassoc.sh " + args);

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
            result = SshHelper.sshExecute(controlIp, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                    "/opt/cloud/bin/get_template_version.sh ");

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

        if(routerGuestIp != null && zoneNetworkType != null && NetworkType.valueOf(zoneNetworkType) == NetworkType.Basic) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("In Basic zone mode, use router's guest IP for SSH control. guest IP : " + routerGuestIp);

            return routerGuestIp;
        }

        if(s_logger.isDebugEnabled())
            s_logger.debug("Use router's private IP for SSH control. IP : " + routerIp);
        return routerIp;
    }

    protected Answer execute(NetworkUsageCommand cmd) {
/*        if ( cmd.isForVpc() ) {
            return VPCNetworkUsage(cmd);
        }
*/        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource NetworkUsageCommand "+ s_gson.toJson(cmd));
        }
        if(cmd.getOption()!=null && cmd.getOption().equals("create") ){
            String result = networkUsage(cmd.getPrivateIP(), "create", null);
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
                    stats[0] += (new Long(splitResult[i++])).longValue();
                    stats[1] += (new Long(splitResult[i++])).longValue();
                }
            } catch (Throwable e) {
                s_logger.warn("Unable to parse return from script return of network usage command: " + e.toString(), e);
            }
        }
        return stats;
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

            Pair<Boolean, String> result = SshHelper.sshExecute(privateIpAddress, DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "/opt/cloud/bin/netusage.sh " + args);

            if (!result.first()) {
                return null;
            }

            return result.second();
        } catch (Throwable e) {
            s_logger.error("Unable to execute NetworkUsage command on DomR (" + privateIpAddress + "), domR may not be ready yet. failure due to "
                    + e);
        }

        return null;
    }

    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        URL url = this.getClass().getClassLoader().getResource("vms/systemvm.iso");
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }

        if(isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/systemvm.iso");
        }

        assert(isoFile != null);
        if(!isoFile.exists()) {
            s_logger.error("Unable to locate systemvm.iso in your setup at " + isoFile.toString());
        }
        return isoFile;
    }

    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader().getResource("scripts/vm/systemvm/id_rsa.cloud");
        File keyFile = null;
        if ( url != null ){
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File("/usr/share/cloudstack-common/scripts/vm/systemvm/id_rsa.cloud");
        }
        assert(keyFile != null);
        if(!keyFile.exists()) {
            s_logger.error("Unable to locate id_rsa.cloud in your setup at " + keyFile.toString());
        }
        return keyFile;
    }


    public static String postHttpRequest(final String jsonCmd,
            final URI agentUri) {
        // Using Apache's HttpClient for HTTP POST
        // Java-only approach discussed at on StackOverflow concludes with
        // comment to use Apache HttpClient
        // http://stackoverflow.com/a/2793153/939250, but final comment is to
        // use Apache.
        s_logger.debug("POST request to" + agentUri.toString()
                + " with contents" + jsonCmd);

        // Create request
        HttpClient httpClient = new DefaultHttpClient();
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
                    + " cmd data:" + jsonCmd);
            HttpResponse response = httpClient.execute(request);

            // Unsupported commands will not route.
            if (response.getStatusLine().getStatusCode()
                == HttpStatus.SC_NOT_FOUND) {
                String errMsg =
                        "Failed to send : HTTP error code : "
                                + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                String unsupportMsg =
                        "Unsupported command "
                                + agentUri.getPath()
                                + ".  Are you sure you got the right type of"
                                + " server?";
                Answer ans = new UnsupportedAnswer(null, unsupportMsg);
                s_logger.error(ans);
                result = s_gson.toJson(new Answer[] {ans});
            } else if (response.getStatusLine().getStatusCode()
                != HttpStatus.SC_OK) {
                String errMsg =
                        "Failed send to " + agentUri.toString()
                                + " : HTTP error code : "
                                + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                return null;
            } else {
                result = EntityUtils.toString(response.getEntity());
                s_logger.debug("POST response is" + result);
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
    public final boolean configure(final String name,
            final Map<String, Object> params) throws ConfigurationException {
        /* todo: update, make consistent with the xen server equivalent. */
        _guid = (String) params.get("guid");
        _zoneId = (String) params.get("zone");
        _podId = (String) params.get("pod");
        _clusterId = (String) params.get("cluster");
        _agentIp = (String) params.get("ipaddress"); // was agentIp
        _name = name;

        _clusterGuid = (String) params.get("cluster.guid");
        _username = (String) params.get("url");
        _password = (String) params.get("password");
        _username = (String) params.get("username");

        _configureCalled = true;
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
        while (System.currentTimeMillis() - startTick <= _ops_timeout || --retry > 0) {
            SocketChannel sch = null;
            try {
                s_logger.info("Trying to connect to " + ipAddress);
                sch = SocketChannel.open();
                sch.configureBlocking(true);
                sch.socket().setSoTimeout(5000);

                InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (IOException e) {
                s_logger.info("Could not connect to " + ipAddress + " due to " + e.toString());
                if (e instanceof ConnectException) {
                    // if connection is refused because of VM is being started,
                    // we give it more sleep time
                    // to avoid running out of retry quota too quickly
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                    }
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (IOException e) {
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }

        s_logger.info("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

}
