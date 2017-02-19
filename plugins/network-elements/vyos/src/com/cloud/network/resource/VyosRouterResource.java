package com.cloud.network.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalFirewallCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.rules.FirewallRule;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshException;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;

public class VyosRouterResource implements ServerResource{

    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private Integer _numRetries;
    private String _publicInterface;
    private String _privateInterface;
    private static final Logger s_logger = Logger.getLogger(VyosRouterResource.class);


    private static com.trilead.ssh2.Connection s_sshClient;

    private static final String s_vyosShellScript = "#!/bin/vbash\nallParams=\"\\\"'$@'\\\"\"\nsource /opt/vyatta/etc/functions/script-template\n\"'$allParams'\"\n";
    private static final String s_vyosScriptPath="/home/vyos/";
    private static final String s_vyosScriptName="testShellScript.sh";

    protected enum VyosRouterMethod {
        SHELL, HTTPSTUB;
    }

    protected enum VyosRouterCommandType {
        READ, WRITE, SAVE;
    }

    private enum VyosRouterPrimative {
        CHECK_IF_EXISTS, ADD, DELETE;
    }

    private enum Protocol {
        TCP("tcp"), UDP("udp"), ICMP("icmp"), ALL("all");

        private final String protocol;

        private Protocol(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public String toString() {
            return protocol;
        }
    }

    private enum GuestNetworkType {
        SOURCE_NAT, INTERFACE_NAT;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand)cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand)cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            return execute((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String)params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            _zoneId = (String)params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _ip = (String)params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _username = (String)params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username");
            }

            _password = (String)params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password");
            }

            _publicInterface = (String)params.get("publicinterface");
            if (_publicInterface == null) {
                throw new ConfigurationException("Unable to find public interface.");
            }

            _privateInterface = (String)params.get("privateinterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface.");
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 0);

            // Open a socket
            initializeVyosIntegration();

            return true;
        } catch (Exception e) {
            try {
                //System.out.println("In VyosRouterResource.configure(). attempting to disconnect the ssh client. ");
                if (s_sshClient != null) {
                    //System.out.println("In VyosRouterResource.configure(). We are connected. Running disconnect. ");
                    s_sshClient.close();
                }
                throw new ConfigurationException("Exception of type: "+e.getClass().getName()+" caught. message: "+e.getMessage());
            }catch (Exception f) {
                throw new ConfigurationException("Exception of type: "+e.getClass().getName()+" caught. message: "+e.getMessage()+" \nThen exception handling threw an exception of type: "+f.getClass().getName()+" caught. message: "+f.getMessage());

            }
        } finally {
            try {
                if (s_sshClient != null) {
                    s_sshClient.close();
                }
            }catch (Exception e) {
                throw new ConfigurationException("Exception of type: "+e.getClass().getName()+" caught. message: "+e.getMessage());
            }

        }

    }

    @Override
    public StartupCommand[] initialize() {
        StartupExternalFirewallCommand cmd = new StartupExternalFirewallCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion(VyosRouterResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Host.Type getType() {
        return Host.Type.ExternalFirewall;
    }

    @Override
    public String getName() {
        return _name;
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
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.ExternalFirewall, id);
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
        return;
    }

    private boolean refreshVyosRouterConnection() throws IOException {
        try {
            s_sshClient = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
        }catch(Exception e){
            s_logger.error(e);
            if (s_sshClient != null) {
                s_sshClient.close();
            }
        }
        return true;
    }

    public static Map<String,String> sshExecuteCmd(String cmd) throws SshException {
        Map<String, String> result = new HashMap<String, String>();
        s_logger.debug("Executing cmd: " + cmd);
        Session sshSession = null;
        try {
            sshSession = s_sshClient.openSession();
            // There is a bug in Trilead library, wait a second before
            // starting a shell and executing commands, from http://spci.st.ewi.tudelft.nl/chiron/xref/nl/tudelft/swerl/util/SSHConnection.html
            Thread.sleep(1000);

            if (sshSession == null) {
                throw new SshException("Cannot open ssh session");
            }

            sshSession.execCommand(cmd);

            InputStream stdout = sshSession.getStdout();
            InputStream stderr = sshSession.getStderr();

            byte[] buffer = new byte[8192];
            StringBuffer sbStdout = new StringBuffer();
            StringBuffer sbStderr = new StringBuffer();

            int currentReadBytes = 0;
            while (true) {
                if (stdout == null || stderr == null) {
                    throw new SshException("stdout or stderr of ssh session is null");
                }
                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions = sshSession.waitForCondition(ChannelCondition.STDOUT_DATA
                                | ChannelCondition.STDERR_DATA | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS,
                                120000);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        String msg = "Timed out in waiting SSH execution result";
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }

                    if ((conditions & ChannelCondition.EXIT_STATUS) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }

                    if ((conditions & ChannelCondition.EOF) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (stdout.available() > 0) {
                    currentReadBytes = stdout.read(buffer);
                    sbStdout.append(new String(buffer, 0, currentReadBytes));
                }

                while (stderr.available() > 0) {
                    currentReadBytes = stderr.read(buffer);
                    sbStderr.append(new String(buffer, 0, currentReadBytes));
                }
            }

            String stdoutString=sbStdout.toString();
            String stderrString=sbStderr.toString();
            result.put("stdout", stdoutString);
            result.put("stderr", stderrString);

            String resultString="";
            if (stdoutString != null && !stdoutString.isEmpty()) {
                resultString=result+" "+stdoutString;
            }
            if (stderrString != null && !stderrString.isEmpty()) {
                resultString=result+" "+stderrString;
            }
            if (!result.isEmpty()) {
                s_logger.debug(cmd + " output:" + resultString);
            }
            // exit status delivery might get delayed
            for(int i = 0 ; i<10 ; i++ ) {
                Integer status = sshSession.getExitStatus();
                if( status != null ) {
                    result.put("exitCode", status.toString());
                    return result;
                }
                Thread.sleep(100);
            }
            result.put("exitCode", "-1");
            return result;
        } catch (Exception e) {
            s_logger.debug("Ssh executed failed", e);
            throw new SshException("Ssh executed failed " + e.getMessage());
        } finally {
            if (sshSession != null)
                sshSession.close();
        }
    }

  //Executes a shell command. Throws error on a nonzero return code. Returns the contents of stdout.
    public String executeVyosRouterCommand(String shellCommand)
            throws IOException, SshException {

         refreshVyosRouterConnection();

         String tmpOutput="";
        try {

            tmpOutput=tmpOutput+"\nexecuting the following command: "+shellCommand;
            Map<String,String> result=sshExecuteCmd(shellCommand);

            if (!result.get("stdout").isEmpty()) {
                tmpOutput=tmpOutput+"\n******\n stdout: \n"+result.get("stdout")+"\n******";
            }
            if (!result.get("stderr").isEmpty()) {
                tmpOutput=tmpOutput+"\n******\n stderr: \n"+result.get("stderr")+"\n******";
            }
            //Validate the result of command execution and throw an exception on error.
            this.validResponse(shellCommand, result.get("stdout"), result.get("stderr"));

            return result.get("stdout");
        } finally {
            if (!shellCommand.contains("echo") && !shellCommand.contains("chmod") && !shellCommand.contains("show"))
            {
                System.out.println(tmpOutput);
            }
        }
    }
    // Write the shell script we will be using to execute vyos commands to disk on the router.
    public void initializeVyosIntegration() throws IOException {
        try{
            refreshVyosRouterConnection();
            executeVyosRouterCommand("echo -e \""+s_vyosShellScript+"\" > ~/"+s_vyosScriptName);
            executeVyosRouterCommand("chmod +x ~/"+s_vyosScriptName);
        }catch (Exception e) {
            throw new IOException(e.getMessage());
        }


    }
 // ENTRY POINTS...

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private ExternalNetworkResourceUsageAnswer execute(ExternalNetworkResourceUsageCommand cmd) {
        return new ExternalNetworkResourceUsageAnswer(cmd);
    }

    /*
     * Guest networks
     */

    private synchronized Answer execute(IpAssocCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(IpAssocCommand cmd, int numRetries) {
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        try {
            IpAddressTO ip;
            if (cmd.getIpAddresses().length != 1) {
                throw new ExecutionException("Received an invalid number of guest IPs to associate.");
            } else {
                ip = cmd.getIpAddresses()[0];
            }

            String sourceNatIpAddress = null;
            GuestNetworkType type = GuestNetworkType.INTERFACE_NAT;

            if (ip.isSourceNat()) {
                type = GuestNetworkType.SOURCE_NAT;

                if (ip.getPublicIp() == null) {
                    throw new ExecutionException("Source NAT IP address must not be null.");
                } else {
                    sourceNatIpAddress = ip.getPublicIp();
                }
            }

            long guestVlanTag = Long.parseLong(cmd.getAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG));
            String guestVlanGateway = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
            String cidr = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);
            String defaultEgressPolicy = cmd.getAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT);
            long cidrSize = NetUtils.cidrToLong(cidr)[1];
            String guestVlanSubnet = NetUtils.getCidrSubNet(guestVlanGateway, cidrSize);

            Long publicVlanTag = null;
            if (ip.getBroadcastUri() != null) {
                String parsedVlanTag = parsePublicVlanTag(ip.getBroadcastUri());
                if (!parsedVlanTag.equals("untagged")) {
                    try {
                        publicVlanTag = Long.parseLong(parsedVlanTag);
                    } catch (Exception e) {
                        throw new ExecutionException("Could not parse public VLAN tag: " + parsedVlanTag);
                    }
                }
            }

            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            if (ip.isAdd()) {
                // Implement the guest network for this VLAN
                implementGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize, defaultEgressPolicy);
            } else {
                // Remove the guest network:
                shutdownGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize, defaultEgressPolicy);
            }

            requestWithCommit(commandList);
            System.out.println("Finished Executing commands via requestWithCommit\n***************************************");
            results[i++] = ip.getPublicIp() + " - success";
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying IPAssocCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }

        return new IpAssocAnswer(cmd, results);
    }

    private void implementGuestNetwork(ArrayList<IVyosRouterCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String publicIp, long privateVlanTag,
            String privateGateway, String privateSubnet, long privateCidrNumber, String defaultEgressPolicy) throws ExecutionException {
            privateSubnet = privateSubnet + "/" + privateCidrNumber;

            managePrivateInterface(cmdList, VyosRouterPrimative.ADD, privateVlanTag, privateGateway + "/" + privateCidrNumber, defaultEgressPolicy);

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
                managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp + "/32", privateVlanTag, defaultEgressPolicy);
                manageSrcNatRule(cmdList, VyosRouterPrimative.ADD, type, publicVlanTag, publicIp + "/32", privateVlanTag, privateGateway + "/" + privateCidrNumber);
            }

            String msg =
                "Implemented guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrNumber;
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + publicIp : "";
            s_logger.debug(msg);
        }

    private void shutdownGuestNetwork(ArrayList<IVyosRouterCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String sourceNatIpAddress, long privateVlanTag,
            String privateGateway, String privateSubnet, long privateCidrSize, String defaultEgressPolicy) throws ExecutionException {
            privateSubnet = privateSubnet + "/" + privateCidrSize;

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
                manageSrcNatRule(cmdList, VyosRouterPrimative.DELETE, type, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, privateGateway + "/" + privateCidrSize);
                managePublicInterface(cmdList, VyosRouterPrimative.DELETE, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, defaultEgressPolicy);
            }

            managePrivateInterface(cmdList, VyosRouterPrimative.DELETE, privateVlanTag, privateGateway + "/" + privateCidrSize, defaultEgressPolicy);

            String msg = "Shut down guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrSize;
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + sourceNatIpAddress : "";
            s_logger.debug(msg);
        }

    /*
     * Firewall rule entry point
     */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {

        //Get the private network information associated with the current rule.
        String guestVlanTag = cmd.getAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG);
        String guestCidr = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);

        FirewallRuleTO[] rules = cmd.getRules();
        try {
            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            for (FirewallRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageFirewallRule(commandList, VyosRouterPrimative.ADD, rule, guestVlanTag, guestCidr);
                } else {
                    manageFirewallRule(commandList, VyosRouterPrimative.DELETE, rule, guestVlanTag, guestCidr);
                }
            }

            requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 ) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetFirewallRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    /*
     * Static NAT rule entry point
     */

    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        StaticNatRuleTO[] rules = cmd.getRules();

        try {
            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            for (StaticNatRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageStcNatRule(commandList, VyosRouterPrimative.ADD, rule);
                } else {
                    manageStcNatRule(commandList, VyosRouterPrimative.DELETE, rule);
                }
            }
            requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 ) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetStaticNatRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    /*
     * Destination NAT (Port Forwarding) entry point
     */
    private synchronized Answer execute(SetPortForwardingRulesCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {
        PortForwardingRuleTO[] rules = cmd.getRules();

        try {
            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            for (PortForwardingRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageDstNatRule(commandList, VyosRouterPrimative.ADD, rule);
                } else {
                    manageDstNatRule(commandList, VyosRouterPrimative.DELETE, rule);
                }
            }

            requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 ) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetPortForwardingRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

 // IMPLEMENTATIONS...

    /*
     * Vyos Firewall Rule-set implementation
     * In Vyos, each interface can have three different sets of firewall rules in (inbound packets), out (outbound packets),
     * and local (packets whose destination is the router itself). To add firewall rules we must first create rule sets to hold them.
     */
    public boolean manageFirewallRulesets(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, String interfaceName, String privateVlanTag, String trafficType, String defaultEgressPolicy)
            throws ExecutionException {
        String firewallRulesetName=privateVlanTag+"_"+trafficType;
        String vlanConfigString="";
        boolean isPrivate=true;
        //Handle firewall for public interfaces
        if (privateVlanTag.isEmpty()) {
            firewallRulesetName=interfaceName+"_"+trafficType;
            isPrivate=false;
        }

        String defaultFirewallAction="drop";
        String firewallRuleAction="accept";

        if (trafficType=="Egress" && defaultEgressPolicy == "allow")
        {
            defaultFirewallAction="accept";
            firewallRuleAction="drop";
        }

        //This is kind of confusing but in Vyos There are 3 firewalls per interface.
        // in : applies to packets originating on a network associated with the interface and entering the router for processing.
        // out : applies to packets that are exiting the router bound for a network associated with the interface.
        // local : applies to packets entering the interface whose end point is the router itself.

        //    This means that all packets traversing the router must pass filtering by two firewalls, on "in" and one "out" .
        //    The firewalls that are applied to each packet will differ based on the interface at which they enter the router
        //    and the interface at which they leave the router.

        String vyosTrafficType=trafficType;
        if ( (trafficType == "Egress" && isPrivate == true) || (trafficType == "Ingress" && isPrivate == false)){
            vyosTrafficType="in";
        } else if (trafficType != "local"){
            vyosTrafficType="out";
        }
        if (isPrivate){
            vlanConfigString=" vif "+privateVlanTag;
        }

        switch (prim) {
            case CHECK_IF_EXISTS:
            // check if one exists already

                ArrayList<String> params = new ArrayList<String>();
                params.add("run show configuration commands");
                String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
                if (response.contains("set firewall name "+firewallRulesetName)) {
                    s_logger.debug("Firewall ruleset exists: " + firewallRulesetName);
                    return true;
                }
                else {
                    s_logger.debug("Firewall ruleset does not exist: " + firewallRulesetName+" response: " + response);
                    return false;
                }
            case ADD:
                if (manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, trafficType, defaultEgressPolicy)) {
                    return true;
                }

                 ArrayList<String> a_params = new ArrayList<String>();
                 a_params.add("set firewall name "+firewallRulesetName+" default-action '"+defaultFirewallAction+"'" );
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 action '"+firewallRuleAction+"' ");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 state established 'enable' ");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 state related 'enable' ");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 description 'defaultFirewallRule' ");

                 //allow port 22 to the router.
                 // TODO This needs to be secured!
                 if (vyosTrafficType == "local") {
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 action '"+firewallRuleAction+"' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 destination port '22' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 protocol 'tcp' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 description 'allowSSHToRouter' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 state new 'enable' ");

                 }

                 a_params.add("set interfaces ethernet "+interfaceName+vlanConfigString+" firewall "+vyosTrafficType+" name "+firewallRulesetName);
                 cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                 return true;

            case DELETE:
                if (!manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, trafficType, defaultEgressPolicy)) {
                    return true;
                }
                //disassociate the firewall rulest from the network interface
                ArrayList<String> d_nic_params = new ArrayList<String>();
                d_nic_params.add("delete interfaces ethernet "+interfaceName+vlanConfigString+" firewall "+vyosTrafficType);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_nic_params));

                //Delete the firewall ruleset
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete firewall name "+firewallRulesetName);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }
    }
    /*
     * Private interface implementation
     */


    public boolean managePrivateInterface(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, long privateVlanTag, String privateGateway, String defaultEgressPolicy)
        throws ExecutionException {
        String interfaceName = _privateInterface; //genPrivateInterfaceName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                //
                ArrayList<String> params = new ArrayList<String>();
                params.add("run show configuration commands");
                String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
                if (response.contains("set interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" address '"+privateGateway+"'")) {
                    s_logger.debug("Private sub-interface exists: " + interfaceName +"."+privateVlanTag );
                    return true;
                }
                else {
                    s_logger.debug("Private sub-interface does not exist: " + interfaceName +"."+ privateVlanTag +" address: "+ privateGateway +" response: " + response);
                    return false;
                }


            case ADD:
                if (managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway, defaultEgressPolicy)) {
                    return true;
                }

                // add vlan and privateGateway ip to private interface
                ArrayList<String> a_sub_params = new ArrayList<String>();
                a_sub_params.add("set interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" address "+privateGateway);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_sub_params));


                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), "Ingress", defaultEgressPolicy);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), "Egress", defaultEgressPolicy);

                return true;

            case DELETE:
                if (!managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway, defaultEgressPolicy)) {
                    return true;
                }

             // Delete ingress and egress firewall rule sets.
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), "Ingress", defaultEgressPolicy);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), "Egress", defaultEgressPolicy);

                // delete vlan and privateGateway ip from private interface
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete interfaces ethernet "+interfaceName+" vif "+privateVlanTag);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));


                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Public Interface implementation
     */

    public boolean managePublicInterface(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, Long publicVlanTag, String publicIp, long privateVlanTag, String defaultEgressPolicy)
        throws ExecutionException {
        String interfaceName=_publicInterface;
        String vlanConfigString="";
        String publicVlanString="";
        if (publicVlanTag != null) {
            publicVlanString=Long.toString(publicVlanTag);
            vlanConfigString=" vif "+publicVlanTag;

        }


        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                ArrayList<String> params = new ArrayList<String>();
                //params.add("run show interfaces ethernet "+interfaceName+vlanString);
                params.add("run show configuration commands");
                String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
                if (response.contains("set interfaces ethernet "+interfaceName+vlanConfigString+" address '"+publicIp+"'")) {
                    s_logger.debug("Public sub-interface exists: " + interfaceName +"."+publicVlanTag );
                    return true;
                }
                else {
                    s_logger.debug("Public sub-interface does not exist: " + interfaceName +"."+ publicVlanTag +" address: "+ publicIp +" response: " + response);
                    return false;
                }


            case ADD:
                if (managePublicInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag, defaultEgressPolicy)) {
                    return true;
                }

                // add IP to the sub-interface
                // add vlan and public ip to public interface
                ArrayList<String> a_sub_params = new ArrayList<String>();
                a_sub_params.add("set interfaces ethernet "+interfaceName+vlanConfigString+" address "+publicIp);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_sub_params));

                // Make sure there are rulesets for the public interface.
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, "Ingress", defaultEgressPolicy);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, "Egress", defaultEgressPolicy);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, "local", defaultEgressPolicy);

                return true;

            case DELETE:
                if (!managePublicInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag, defaultEgressPolicy)) {
                    return true;
                }
                // delete IP from interface...
                // delete vlan and public ip from public interface
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete interfaces ethernet "+interfaceName+vlanConfigString+" address "+publicIp);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }



    /*
     * Source NAT rule implementation
     */



    public boolean manageSrcNatRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, GuestNetworkType type, Long publicVlanTag, String publicIp,
            long privateVlanTag, String privateGateway) throws ExecutionException {

        //For default source nat rules we must make sure that they are the last rules in the source nat section. Vyos processes rules in ascending numerical order.
        //        Default source nat rules should only be applied if no other rule matches the packets being processed. So must have the highest possible vyos rule numbers.
        int sourceRuleNumber=0;
        String cloudstackRuleName= Long.toString(privateVlanTag);


        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", cloudstackRuleName, true);
                if (sourceRuleNumber != 0 ) {
                    s_logger.debug("Source Nat Rule exists: Rule Number: " + sourceRuleNumber);
                    return true;
                }
                else {
                    s_logger.debug("Source Nat Rule does not exist.");
                    return false;
                }

            case ADD:
                if (manageSrcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }

                // Build the parameters needed for vyos
                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", "", true);
                ArrayList<String> a_params = new ArrayList<String>();
                a_params.add("set nat source rule "+sourceRuleNumber+" outbound-interface '"+_publicInterface+"'" );
                a_params.add("set nat source rule "+sourceRuleNumber+" source address '"+privateGateway+"'" );
                a_params.add("set nat source rule "+sourceRuleNumber+" translation address masquerade" );
                a_params.add("set nat source rule "+sourceRuleNumber+" description '"+cloudstackRuleName+"'");
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                return true;

            case DELETE:
                if (!manageSrcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }

                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", cloudstackRuleName, true);

                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete nat source rule " +sourceRuleNumber);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Destination NAT rules (Port Forwarding) implementation
     */

    public boolean manageDstNatRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, PortForwardingRuleTO rule) throws ExecutionException {
        String publicIp = rule.getSrcIp();
        String dstIp= rule.getDstIp();



        String publicInterfaceName=_publicInterface;
        String cloudstackRuleName= Long.toString(rule.getId());
        int destinationRuleNumber=0;

        Long publicVlanTag = null;
        long privateVlanTag = 0;



        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", cloudstackRuleName);
                if (destinationRuleNumber != 0) {
                    s_logger.debug("Destination Nat Rule exists: Rule Number: "+destinationRuleNumber );
                    return true;
                }
                else {
                    s_logger.debug("Destination Nat Rule does not exist.");
                    return false;
                }

            case ADD:
                if (manageDstNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                //get vyos destination nat rule number
                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", "");

                // build source port range string
                String protocol = rule.getProtocol();
                int[] srcPortRange = rule.getSrcPortRange();
                String srcPortRangeString="";
                if (srcPortRange != null) {
                    if (srcPortRange.length == 1 || srcPortRange[0] == srcPortRange[1]) {
                        srcPortRangeString = String.valueOf(srcPortRange[0]);
                    } else {
                        srcPortRangeString = String.valueOf(srcPortRange[0]) + "-" + String.valueOf(srcPortRange[1]);
                    }
                }
                // build destination port range string
                int[] dstPortRange = rule.getDstPortRange();
                String dstPortRangeString="";
                if (dstPortRange != null) {
                    if (dstPortRange.length == 1 || dstPortRange[0] == dstPortRange[1]) {
                        dstPortRangeString = String.valueOf(dstPortRange[0]);
                    } else {
                        dstPortRangeString = String.valueOf(dstPortRange[0]) + "-" + String.valueOf(dstPortRange[1]);
                    }
                }

                // Make sure the public IP referenced in this rule exists.
                if (managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp+"/32", privateVlanTag, null) == false ) {
                    throw new ExecutionException("Could not add the public Ip");
                }
                ArrayList<String> a_params = new ArrayList<String>();
                a_params.add("set nat destination rule "+destinationRuleNumber+" translation port '"+dstPortRangeString+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" inbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" protocol '"+protocol+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" translation address '"+dstIp+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" description '"+cloudstackRuleName+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" destination address '"+publicIp+"'" );

                if (srcPortRangeString != "") {
                    a_params.add("set nat destination rule "+destinationRuleNumber+" destination port '"+srcPortRangeString+"'" );

                }
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                return true;

            case DELETE:
                if (!manageDstNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", cloudstackRuleName);
                // delete the dst nat rule
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete nat destination rule " +destinationRuleNumber);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Static NAT rule implementation
     */


    public boolean manageStcNatRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, StaticNatRuleTO rule) throws ExecutionException {
        String publicIp = rule.getSrcIp();
        String privateIp = rule.getDstIp();
        String cloudstackRuleName= Long.toString(rule.getId());
        int sourceRuleNumber=0;
        int destinationRuleNumber=0;

        String publicInterfaceName=_publicInterface;

        //Dummy values for calling managePublicInterface
        Long publicVlanTag = null;
        long privateVlanTag = 0;

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", cloudstackRuleName);
                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", cloudstackRuleName);
                if (sourceRuleNumber != 0 && destinationRuleNumber != 0) {
                    s_logger.debug("Static Nat Rule exists: Rule Numbers: Source:" + sourceRuleNumber+" Destination: "+destinationRuleNumber );
                    return true;
                }
                else {
                    s_logger.debug("Static Nat Rule does not exist.");
                    return false;
                }

            case ADD:
                if (manageStcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }
                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", "");
                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", "");


                // Make sure the public IP referenced in this rule exists.
                if (managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp+"/32", privateVlanTag, null) == false ) {
                    throw new ExecutionException("Could not add the public Ip");
                }

                // add the static nat rule for the public IP
                ArrayList<String> a_params = new ArrayList<String>();
                a_params.add("set nat source rule "+sourceRuleNumber+" source address '"+privateIp+"'" );
                a_params.add("set nat source rule "+sourceRuleNumber+" outbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat source rule "+sourceRuleNumber+" translation address '"+publicIp+"'" );
                a_params.add("set nat source rule "+sourceRuleNumber+" description '"+cloudstackRuleName+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" inbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" destination address '"+publicIp+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" translation address '"+privateIp+"'" );
                a_params.add("set nat destination rule "+destinationRuleNumber+" description '"+cloudstackRuleName+"'" );

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                return true;

            case DELETE:
                if (!manageStcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                sourceRuleNumber=getVyosRuleNumber("set nat source rule ", cloudstackRuleName);
                destinationRuleNumber=getVyosRuleNumber("set nat destination rule ", cloudstackRuleName);
                // delete the static nat rule
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete nat destination rule " +destinationRuleNumber);
                d_sub_params.add("delete nat source rule " +sourceRuleNumber);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Firewall rule implementation
     */
    private String genFirewallRuleName(long id) { // ingress
        return Long.toString(id);
    }

    private String genFirewallRuleName(long id, String vlan) { // egress
        return Long.toString(id) + "_" + vlan;
    }

    public boolean manageFirewallRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, FirewallRuleTO rule, String guestVlanTag, String guestCidr) throws ExecutionException {
        //This will always interact with two firewall rulesets. One for the interface where packets enter the router and one for the interface where
        //packets exit the router.

        String trafficType="";
        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
            trafficType="Egress";
        } else { //Ingress
            trafficType="Ingress";
        }

        int privateRuleName=0;
        int publicRuleName=0;
        String privateNetworkGroupName="";
        String publicNetworkGroupName="";
        String privateInterfaceFirewallType="";
        String publicInterfaceFirewallType="";

        String cloudstackRuleName="";
        String privateInterfaceNameString=_privateInterface+" vif "+guestVlanTag;
        String publicInterfaceNameString=_publicInterface;;
        String privateFirewallRuleSetName=guestVlanTag+"_"+trafficType;
        String publicFirewallRuleSetName=_publicInterface+"_"+trafficType;


        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
            cloudstackRuleName = genFirewallRuleName(rule.getId(), rule.getSrcVlanTag());
            privateInterfaceFirewallType="in";
            publicInterfaceFirewallType="out";
        } else { //Ingress
            cloudstackRuleName = genFirewallRuleName(rule.getId());
            privateInterfaceFirewallType="out";
            publicInterfaceFirewallType="in";
        }

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                privateRuleName=getVyosRuleNumber(privateFirewallRuleSetName, cloudstackRuleName);
                publicRuleName=getVyosRuleNumber(publicFirewallRuleSetName, cloudstackRuleName);
                if (privateRuleName != 0 && publicRuleName !=0) {
                    s_logger.debug("Firewall Rules exist: Private: " + privateFirewallRuleSetName+"."+privateRuleName+" Public: "+publicFirewallRuleSetName+"."+publicRuleName );
                    return true;
                }
                else {
                    s_logger.debug("Firewall Rules do not exist: Private: " + privateFirewallRuleSetName +" Public: " + publicFirewallRuleSetName);
                    return false;
                }

            case ADD:
                if (manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule, guestVlanTag, guestCidr)) {
                    return true;
                }

                privateRuleName=getVyosRuleNumber(privateFirewallRuleSetName, "");
                publicRuleName=getVyosRuleNumber(publicFirewallRuleSetName, "");

                privateNetworkGroupName=privateFirewallRuleSetName+"-"+privateRuleName;
                publicNetworkGroupName=publicFirewallRuleSetName+"-"+publicRuleName;

                if (privateRuleName == 0 || publicRuleName == 0) {
                    throw new ExecutionException("Could not determine a valid new Vyos rule number for the firewall rule with Cloudstack Rule Id: "+Long.toString(rule.getId()));
                }

                String action = "accept";

                String protocol=rule.getProtocol();
                if (protocol.equals(Protocol.ICMP.toString())) {
                    ArrayList<String> a_params = new ArrayList<String>();
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" action 'accept'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" icmp type '"+rule.getIcmpType()+"'");
                    a_params.add( "set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" icmp code '"+rule.getIcmpCode()+"'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" protocol 'icmp'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" state new 'enable'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" description '"+cloudstackRuleName+"'");

                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" action 'accept'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" icmp type '"+rule.getIcmpType()+"'");
                    a_params.add( "set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" icmp code '"+rule.getIcmpCode()+"'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" protocol 'icmp'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" state new 'enable'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" description '"+cloudstackRuleName+"'");
                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                }
                else if (protocol.equals(Protocol.TCP.toString()) || protocol.equals(Protocol.UDP.toString())) {

                    //Handle the default egress firewall policy
                    if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                        // defaults to 'allow', the deny rules are as follows
                        if (rule.getType() == FirewallRule.FirewallRuleType.System) {
                            if (!rule.isDefaultEgressPolicy()) { // default of deny && system rule, so deny
                                action = "drop";
                            }
                        } else {
                            if (rule.isDefaultEgressPolicy()) { // default is allow && user rule, so deny
                                action = "drop";
                            }
                        }
                    }
                    ArrayList<String> a_params = new ArrayList<String>();
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" action '"+action+"'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" protocol '"+protocol+"'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" state new 'enable'");
                    a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" description '"+cloudstackRuleName+"'");

                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" action '"+action+"'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" protocol '"+protocol+"'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" state new 'enable'");
                    a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" description '"+cloudstackRuleName+"'");

                    // Build source port range. In Vyos these are called destination ports.
                    int[] srcPortRange = rule.getSrcPortRange();
                    String srcPortRangeString="";
                    if (srcPortRange != null) {
                        if (srcPortRange.length == 1 || srcPortRange[0] == srcPortRange[1]) {
                            srcPortRangeString = String.valueOf(srcPortRange[0]);
                        } else {
                            srcPortRangeString = String.valueOf(srcPortRange[0]) + "-" + String.valueOf(srcPortRange[1]);
                        }
                        a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" destination port '"+srcPortRangeString+"'");

                        a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" destination port '"+srcPortRangeString+"'");
                    }

                    // Build allowed cidr range
                    // To support multiple cidrs per firewall rule, a network group will be created for each rule.
                    // group names will be the firewall ruleset name and the rule name separated by a dash.
                    // This is to ensure that the names do not collide.


                    // Create the network-group and associated it with the firewall rule
                    a_params.add("set firewall group network-group '"+privateNetworkGroupName+"'");
                    a_params.add("set firewall group network-group '"+publicNetworkGroupName+"'");


                    List<String> ruleSrcCidrList = rule.getSourceCidrList();

                    if (ruleSrcCidrList.size() > 0) { // a cidr was entered, modify as needed...
                        for (int i = 0; i < ruleSrcCidrList.size(); i++) {
                            String curCidr="";

                            if (ruleSrcCidrList.get(i).trim().equals("0.0.0.0/0")) { // allow any
                                if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                                    curCidr = getPrivateSubnet(rule.getSrcVlanTag());
                                } else {
                                    //Do not associate the network group with the firewall rule since it is set to allow all.
                                    continue;
                                }
                            } else {
                                curCidr = ruleSrcCidrList.get(i).trim();
                            }
                            if (curCidr != "") {

                                //Build network group cidrs
                                a_params.add("set firewall group network-group "+privateNetworkGroupName+" network '"+curCidr+"'");
                                a_params.add("set firewall group network-group "+publicNetworkGroupName+" network '"+curCidr+"'");

                                //Associate the network group with the firewall rule.
                                a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" source group network-group '"+privateNetworkGroupName+"'");
                                a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" source group network-group '"+publicNetworkGroupName+"'");

                            }

                        }
                    } else { // no cidr was entered, so allow ALL according to firewall rule type
                        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                            a_params.add("set firewall group network-group "+privateNetworkGroupName+" network '"+getPrivateSubnet(rule.getSrcVlanTag())+"'");
                            a_params.add("set firewall group network-group "+publicNetworkGroupName+" network '"+getPrivateSubnet(rule.getSrcVlanTag())+"'");

                            //Associate the network group with the firewall rule.
                            a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" source group network-group '"+privateNetworkGroupName+"'");
                            a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" source group network-group '"+publicNetworkGroupName+"'");
                        }

                    }

                    // Build destination address
                    if (rule.getSrcIp() != null){
                        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress){
                            //From my understanding this should be an error state. srcIP should always be null for Egress rules.
                            throw new ExecutionException("Egress Firewall Rule has a non null value in srcIp: "+rule.getSrcIp());
                        }
                        else{
                            //The public Ip cannot be used in the firewall rules since Vyos performs destination nat before applying any firewall rules.
                            //We must use the guest network cidr instead.
                            a_params.add("set firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName+" destination address '"+guestCidr+"'");

                            a_params.add("set firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName+" destination address '"+guestCidr+"'");
                        }
                    }


                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));
                }
                else {
                    throw new ExecutionException("The protocol is not supported: "+protocol);
                    //return false;
                }

                return true;

            case DELETE:
                if (!manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule, guestVlanTag, guestCidr)) {
                    return true;
                }

                privateRuleName=getVyosRuleNumber(privateFirewallRuleSetName, cloudstackRuleName);
                publicRuleName=getVyosRuleNumber(publicFirewallRuleSetName, cloudstackRuleName);
                if (privateRuleName == 0 || publicRuleName == 0) {
                    throw new ExecutionException("Could not find a Vyos rule number associated with the Cloudstack ruleId: "+Long.toString(rule.getId()));
                }
                privateNetworkGroupName=privateFirewallRuleSetName+"-"+privateRuleName;
                publicNetworkGroupName=publicFirewallRuleSetName+"-"+publicRuleName;

                //Temporarily disassociate the firewall ruleset from its interface so we can delete the firewall rule.
                ArrayList<String> d_eth_assoc_params = new ArrayList<String>();
                d_eth_assoc_params.add("delete interface ethernet "+privateInterfaceNameString+" firewall "+privateInterfaceFirewallType);

                d_eth_assoc_params.add("delete interface ethernet "+publicInterfaceNameString+" firewall "+publicInterfaceFirewallType);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_eth_assoc_params));


                // delete the firewall rule
                ArrayList<String> d_params = new ArrayList<String>();
                d_params.add("delete firewall name "+privateFirewallRuleSetName+" rule "+privateRuleName);

                d_params.add("delete firewall name "+publicFirewallRuleSetName+" rule "+publicRuleName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_params));

                // delete the network-group for the firewall rule
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete firewall group network-group '"+privateNetworkGroupName+"'");

                d_sub_params.add("delete firewall group network-group '"+publicNetworkGroupName+"'");
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                //reassociate the firewall ruleset from its interface.
                ArrayList<String> a_eth_assoc_params = new ArrayList<String>();
                a_eth_assoc_params.add("set interface ethernet "+privateInterfaceNameString+" firewall "+privateInterfaceFirewallType+" name "+privateFirewallRuleSetName);

                a_eth_assoc_params.add("set interface ethernet "+publicInterfaceNameString+" firewall "+publicInterfaceFirewallType+" name "+publicFirewallRuleSetName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_eth_assoc_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Helper config functions
     */


    private String extractIpAddress(String ipString){
        String IPADDRESS_PATTERN =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipString);
        if (matcher.find()) {
            return matcher.group();
        } else{
            return null;
        }
    }
    private String getPrivateSubnet(String vlan) throws ExecutionException {
        String interfaceName = _privateInterface;
        // TODO Build the parameters needed for vyos
        //Query Vyos to get the ip for the interface.

        ArrayList<String> params = new ArrayList<String>();
        params.add("ip addr show "+interfaceName+"."+vlan+" | grep inet | grep "+interfaceName+"."+vlan);
        String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
        String cidr= response.split("/")[1].split(" ")[0];
        String privateIp=extractIpAddress(response);
        if (privateIp == null)
            return null;
        else {
            //replace the last octet with 0
            return privateIp.replaceFirst("\\d+$", "0")+"/"+cidr;
        }
    }

    /* Function to make calls to the Vyos Router API. */
    /* All API calls will end up going through this function. */
    protected String request(VyosRouterMethod method, VyosRouterCommandType commandType, ArrayList<String> commands) throws ExecutionException {
        if (method != VyosRouterMethod.SHELL && method != VyosRouterMethod.HTTPSTUB) {
            throw new ExecutionException("Invalid method used to access the Vyos Router API.");
        }

        String responseBody = "";
        String debug_msg = "Vyos Router Request\n";
        String vyosShellScript=s_vyosScriptPath+s_vyosScriptName+" ";

        // SHELL method...
        // commandType is used to determine whether to enter configuration mode.
        //        in Vyos there are two execution modes. Read only and Configure.
        //        changes can only be made in configuration mode.
        //        there are some commands available in both modes (EG show) but
        //        their allowed input parameters and output will differ based on the current
        //        execution mode. DANGEROUS!
        // commands is an ordered list of commands to execute.

        //executeVyosRouterCommand("echo -e \""+s_vyosShellScript+"\" > "+s_vyosScriptPath+s_vyosScriptName);
        //executeVyosRouterCommand("chmod +x "+s_vyosScriptPath+s_vyosScriptName);
        String commandsString="\"";

        if (method == VyosRouterMethod.SHELL) {
            if (commandType == VyosRouterCommandType.READ)
            {
                debug_msg = debug_msg + "Command Type: "+commandType;

                for (String curCommand : commands) {
                    commandsString=commandsString+" eval "+curCommand+";";
                }


            } else if (commandType == VyosRouterCommandType.WRITE) {
                debug_msg = debug_msg + "Command Type: "+commandType;

                //Enter Configuration Mode
                commandsString=commandsString+" eval "+"configure;";
                for (String curCommand : commands) {
                    commandsString=commandsString+" eval "+curCommand+";";
                }
                //Commit Changes
                commandsString=commandsString+" eval "+"commit;";

            } else if (commandType == VyosRouterCommandType.SAVE)
            {
                debug_msg = debug_msg + "Command Type: "+commandType;

                for (String curCommand : commands) {
                    commandsString=commandsString+" eval "+curCommand+";";
                }

            } else {
                throw new ExecutionException("Shell method called with an invalid commandType: "+commandType);
            }

            try {
                //execute the script
                responseBody=responseBody+executeVyosRouterCommand(vyosShellScript+commandsString+"\"");


            } catch (Exception e) {
                throw new ExecutionException(e.getMessage());
            }
            for (String curCommand : commands) {
                debug_msg = debug_msg + "\n"+ curCommand;
            }

        }


        // a STUB method...
        // This cannot be implemented until the Vyos team releases a production version of VyConf
        if (method == VyosRouterMethod.HTTPSTUB) {
            throw new ExecutionException("HTTPSTUB method called but has not been implemented yet. Use SHELL method for all requests");
        }

        debug_msg = debug_msg + "\n" + responseBody.replace("\"", "\\\"") + "\n\n"; // test cases
        s_logger.debug(debug_msg); // this can be commented if we don't want to show each request in the log.
        if (commandType == VyosRouterCommandType.WRITE) {
            System.out.println("Finished call to VyosRouterResource.request(). Debug Messages: "+debug_msg);
        }
        return responseBody;
    }

    private synchronized boolean requestWithCommit(ArrayList<IVyosRouterCommand> commandList) throws ExecutionException {
        // This loops through and executes all the commands in the commandList array.
        //If any commands fail then all commands are rolled back. Else, commands are saved to the vyos boot config
        System.out.println("***************************************\nExecuting "+commandList.size()+" commands via requestWithCommit. numretries: "+_numRetries+" Commands:");
        for (IVyosRouterCommand commands : commandList) {
            int count=1;
            for (String curCommand : commands.getParams() ) {
                System.out.println(count+":"+curCommand);
            }
            count++;
            System.out.println("");
        }

        boolean result = true;
        boolean saving = false;
        if (commandList.size() > 0) {
            try {
                // RUN THE SEQUENCE OF COMMANDS
                for (IVyosRouterCommand command : commandList) {
                    result = (result && command.execute()); // run commands and modify result boolean
                }
                //Save the current config
                saving = true;
                ArrayList<String> save = new ArrayList<String>();
                save.add("save");
                DefaultVyosRouterCommand saveCommand=new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.SAVE, save );
                result = (result && saveCommand.execute());

                return result;
            } catch (Exception e) {
                //Handle revert on configuration failure by reloading the current config.boot (last known good config)
                if (!saving){
                    try {
                        //temporarily disassociate firewalls with all interfaces.
                        Map<String,String> interfacesWithFirewalls=this.getVyosInterfacesWithFirewalls();
                        ArrayList<String> interfaceFirewallRemovals = new ArrayList<String>();
                        if (!interfacesWithFirewalls.isEmpty()) {
                            for (String curInterface : interfacesWithFirewalls.keySet()) {
                                interfaceFirewallRemovals.add("delete interfaces ethernet "+curInterface+" firewall");
                            }
                            DefaultVyosRouterCommand interfaceFirewallRemovalCommand=new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, interfaceFirewallRemovals );
                            result = (result && interfaceFirewallRemovalCommand.execute());
                        }

                        ArrayList<String> revert = new ArrayList<String>();
                        revert.add("load /config/config.boot");
                        DefaultVyosRouterCommand revertCommand=new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, revert );
                        result = (result && revertCommand.execute());

                    }catch (Exception f) {
                        throw new ExecutionException("Shell Execution Failed and the changes could not be reverted. Original Error:"+e.getMessage()+" Rollback error: "+f.getMessage());
                    }
                } else {
                    throw new ExecutionException("Failed to save the new config to disk. "+e.getMessage());
                }

                throw new ExecutionException(e.getMessage());
            }

        } else {
            return true; // nothing to do
        }
    }

    /* A default response handler to validate that the request was successful. */
    public void validResponse(String shellCommand, String stdout, String stderr) throws IOException {
        if (!stderr.isEmpty() || stdout.contains("failed") || stdout.contains("does not exist")) {
            String commandOutput="";
            if (!stdout.isEmpty()) {
                commandOutput="\n STDOUT Output: "+stdout;
            }
            if (!stderr.isEmpty()) {
                commandOutput=commandOutput+"\n STDERR Output: "+stderr;
            }

            throw new IOException("Shell Command Failed COMMAND:\n"+shellCommand+commandOutput);
        }

    }

    /* Validate that the response is not empty. */
    public boolean responseNotEmpty(String response) throws ExecutionException {
        // TODO For now response validation is handled in the request() method. Once requests are using http api calls this will need to be implemented.
        s_logger.debug("Something called the responseNotEmpty() method. This has not been implemented!");
        return false;
    }


    /* Command Interface */
    public interface IVyosRouterCommand {
        public boolean execute() throws ExecutionException;
        public ArrayList<String> getParams();


    }

    /* Command Abstract */
    private abstract class AbstractVyosRouterCommand implements IVyosRouterCommand {
        VyosRouterMethod method;
        VyosRouterCommandType commandType;
        ArrayList<String> params;

        public AbstractVyosRouterCommand(VyosRouterMethod method, VyosRouterCommandType commandType, ArrayList<String> params) {
            this.method = method;
            this.commandType=commandType;
            this.params = params;
        }

        @Override
        public boolean execute() throws ExecutionException {
            request(method, commandType, params);
            return true;
        }

        @Override
        public ArrayList<String> getParams() {
            return this.params;
        }


    }

    /* Implement the default functionality */
    private class DefaultVyosRouterCommand extends AbstractVyosRouterCommand {
        public DefaultVyosRouterCommand(VyosRouterMethod method, VyosRouterCommandType commandType, ArrayList<String> params) {
            super(method, commandType, params);
        }

    }

    /*
     * Misc
     */

    private String parsePublicVlanTag(String uri) {
        return uri.replace("vlan://", "");
    }

    //Some vyos rule numbers should be selected in ascending order and some in descending order. Grab the next available rule number for either case.
    private int getNextAvailableVyosRuleNumber(TreeMap<Integer, Integer> existingRuleNumbers, boolean descendingOrder) throws ExecutionException{
        if (descendingOrder == false) {
            Integer previousRuleNumber = new Integer(0);
            for (Integer curRuleNumber : existingRuleNumbers.keySet()) {
                Integer nextRuleNumber = new Integer(previousRuleNumber.intValue()+1);
                if (curRuleNumber.intValue() > nextRuleNumber.intValue()) {
                    return nextRuleNumber.intValue();
                } else if (curRuleNumber.intValue() == nextRuleNumber.intValue()) {
                    previousRuleNumber = new Integer(curRuleNumber);
                } else {
                    //curRuleNumber is less than next rule number. In a sorted map this should never occur.
                    throw new ExecutionException("Could not determine the next available Vyos rule number.");
                }
            }
            return (previousRuleNumber.intValue()+1);
        } else {
            Integer previousRuleNumber = new Integer(10000);
            for (Integer curRuleNumber : existingRuleNumbers.descendingKeySet()) {
                Integer nextRuleNumber = new Integer(previousRuleNumber.intValue()-1);
                if (curRuleNumber.intValue() < nextRuleNumber.intValue()) {
                    return nextRuleNumber.intValue();
                } else if (curRuleNumber.intValue() == nextRuleNumber.intValue()) {
                    previousRuleNumber = new Integer(curRuleNumber);
                } else {
                    //curRuleNumber is less than next rule number. In a sorted map this should never occur.
                    throw new ExecutionException("Could not determine the next available Vyos rule number.");
                }
            }
            return (previousRuleNumber.intValue()-1);
        }

    }

    //Parses the vyos configuation to return the vyos rule number associated with the current cloudstack rule.

    //If no cloudstackRuleId is passed in then this will pass in the next available vyos rule number
    //commandIdentificationString is a String that is unique enough to identify all lines associated with the
    //        type of Vyos command we are looking for (EG Source Nat, Destination Nat, Firewall...etc)
    private int getVyosRuleNumber(String commandIdentificationString, String cloudstackRuleName) throws ExecutionException {
        return getVyosRuleNumber(commandIdentificationString, cloudstackRuleName, false);
    }
    private int getVyosRuleNumber(String commandIdentificationString, String cloudstackRuleName, boolean descendingOrder) throws ExecutionException {
        try {

            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
            //System.out.println("Response:\n"+response);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            TreeMap<Integer, Integer> existingRuleNumbers = new TreeMap<Integer, Integer>();
            while( (line=bufReader.readLine()) != null ) {
                //return the next highest rule number
                if (cloudstackRuleName == "" || cloudstackRuleName == null){
                    /*
                    System.out.println("No ruleId passed in.  commandIndentificationString: **"+commandIdentificationString+"** curLine: "+line);
                    if (line.contains(commandIdentificationString)) {
                        System.out.println("RulesetName matched");
                    }
                    if (line.contains("rule")) {
                        System.out.println("rule keyword matched");
                    }
                    */
                    if (line.contains(commandIdentificationString) && line.contains("rule") ){
                        //System.out.println("In getVyosRuleNumber. curRuleNumber: "+line.split("rule")[1].trim().split(" ")[0]);
                        int curRule=Integer.parseInt(line.split("rule")[1].trim().split(" ")[0]);
                        existingRuleNumbers.put(curRule, curRule);
                    }
                } else {
                    //System.out.println("ruleId passed in: "+cloudstackRuleName+"  commandIndentificationString: "+commandIdentificationString);
                    if (line.contains(commandIdentificationString) && line.contains("description") && line.contains(cloudstackRuleName)) {
                        String curRuleNumber=line.split("rule")[1].trim().split(" ")[0];
                        //System.out.println("In getVyosRuleNumber. curRuleNumber: "+curRuleNumber);
                        return Integer.parseInt(curRuleNumber);
                    }
                }
            }

            if (cloudstackRuleName == "" || cloudstackRuleName == null){
                return getNextAvailableVyosRuleNumber( existingRuleNumbers, descendingOrder);
            }

            //System.out.println("Finished getVyosRuleNumber without finding a matching or valid rule number. returning: 0");

            return 0;


        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        }

    }

    //To load last known good configuration without error we must temporarily disassociate each interface with its firewall rulests.
    private Map<String,String> getVyosInterfacesWithFirewalls() throws ExecutionException{
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);

            Map<String, String> interfacesWithFirewalls = new HashMap<String, String>();
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            while( (line=bufReader.readLine()) != null ) {
                if (line.trim().contains("set interfaces") && line.trim().contains("firewall") ) {
                    if (line.trim().contains("vif")) {
                        String vlanInterface= line.trim().split(" ")[3]+" vif "+line.trim().split(" ")[5];
                        interfacesWithFirewalls.put(vlanInterface, vlanInterface);
                    } else {
                        interfacesWithFirewalls.put(line.trim().split(" ")[3], line.trim().split(" ")[3]);
                    }
                }
            }
            return interfacesWithFirewalls;
        } catch (Exception e) {
            throw new ExecutionException (e.getMessage());
        }

    }



    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {

    }

    @Override
    public Map<String, Object> getConfigParams() {
        return null;
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {

    }

}
