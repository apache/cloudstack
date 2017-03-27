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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.utils.ssh.SshException;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;

@Component
public class VyosRouterResource implements ServerResource{

    //DAO objects to use to query cloudstack
    @Inject
    FirewallRulesDao _fwRulesDao;
    @Inject
    NetworkDao _networkDao;

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

    protected enum VyosNextIdNumber {
        ASCENDING, DESCENDING
    }

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
                if (s_sshClient != null) {
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
            if (!result.isEmpty() && !cmd.contains("run show configuration commands")) {
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
                //Uncomment this to see the full shell command being sent to the vyos router.
                //s_logger.debug(tmpOutput);
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
            String defaultEgressPolicy = "allow";
            if (cmd.getAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT) == "false" ) {
                defaultEgressPolicy="deny";
            }

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
            //privateSubnet = privateSubnet + "/" + privateCidrNumber;

            managePrivateInterface(cmdList, VyosRouterPrimative.ADD, privateVlanTag, privateGateway + "/" + privateCidrNumber, defaultEgressPolicy, publicIp);

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
                managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp + "/32", privateVlanTag, defaultEgressPolicy);
                manageSrcNatRule(cmdList, VyosRouterPrimative.ADD, type, publicVlanTag, publicIp + "/32", privateVlanTag, privateGateway + "/" + privateCidrNumber);
            }

            String msg =
                "************Implemented guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrNumber+" *************\n";
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + publicIp +" public vlan: "+ publicVlanTag: "";
            s_logger.debug(msg);
            System.out.println(msg);

        }

    private void shutdownGuestNetwork(ArrayList<IVyosRouterCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String sourceNatIpAddress, long privateVlanTag,
            String privateGateway, String privateSubnet, long privateCidrSize, String defaultEgressPolicy) throws ExecutionException {
            privateSubnet = privateSubnet + "/" + privateCidrSize;

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
                manageSrcNatRule(cmdList, VyosRouterPrimative.DELETE, type, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, privateGateway + "/" + privateCidrSize);
                managePublicInterface(cmdList, VyosRouterPrimative.DELETE, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, defaultEgressPolicy);
            }

            managePrivateInterface(cmdList, VyosRouterPrimative.DELETE, privateVlanTag, privateGateway + "/" + privateCidrSize, defaultEgressPolicy, sourceNatIpAddress);

            String msg = "Shut down guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrSize;
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + sourceNatIpAddress +" public vlan: "+ publicVlanTag: "";
            s_logger.debug(msg);
        }

    /*
     * Firewall rule entry point
     */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {

        FirewallRuleTO[] rules = cmd.getRules();
        try {
            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            for (FirewallRuleTO rule : rules) {
                s_logger.debug("\n*************************** Current Firewall Rule: "+rule.getGuestCidr()+" .. "+rule.getSrcIp()+" .. "+rule.getSrcVlanTag()+"   *********************\n");
                if (!rule.revoked()) {
                    manageFirewallRule(commandList, VyosRouterPrimative.ADD, rule);
                } else {
                    manageFirewallRule(commandList, VyosRouterPrimative.DELETE, rule);
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

    //Store the list of public Ips associated with each guest network in the description field of the default ingress rule for the guest network.
    public String buildPublicIpList(String firewallRulesetName, String publicIp) throws ExecutionException {
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            while( (line=bufReader.readLine()) != null ) {
                if (line.contains(firewallRulesetName+" rule 1 description")) {
                    return line.split("'")[1]+","+publicIp;
                }
            }
            return publicIp;

        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        }

    }

    /*
     * Vyos Firewall Rule-set implementation
     * In Vyos, each interface can have three different sets of firewall rules: in (inbound packets), out (outbound packets),
     * and local (packets whose destination is the router itself). To add firewall rules we must first create rule sets to hold them.
     */
    public boolean manageFirewallRulesets(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, String interfaceName, String privateVlanTag, String firewallRulesetName, String defaultEgressPolicy, String publicIp)
            throws ExecutionException {
        //String firewallRulesetName="public_"+trafficType;
        if (!firewallRulesetName.contains("public_") && !firewallRulesetName.contains("private_")) { //rulesetname should always contain public_ or private_ if not, throw exception
            throw new ExecutionException("FirewallRulesetName pass in does not contain public_ or private_ as expected. firewallRulesetName: "+firewallRulesetName);
        }
        String description="defaultFirewallRule";
        String vlanConfigString="";
        boolean isPrivate=true;
        //Handle firewall for public interfaces
        if (firewallRulesetName.contains("public_")) {
            isPrivate=false;
        } else {
            description=buildPublicIpList(firewallRulesetName, publicIp);
        }

        String defaultFirewallAction="drop";

        if (firewallRulesetName.contains("Egress") && defaultEgressPolicy == "allow")
        {
            defaultFirewallAction="accept";
        }

        //This is kind of confusing but in Vyos There are 3 firewalls per interface.
        // in : applies to packets originating on a network associated with the interface and entering the router for processing.
        // out : applies to packets that are exiting the router bound for a network associated with the interface.
        // local : applies to packets entering the interface whose end point is the router itself.

        //    This means that all packets traversing the router must pass filtering by two firewalls, on "in" and one "out or local" .
        //    The firewalls that are applied to each packet will differ based on the interface at which they enter the router
        //    and the interface at which they leave the router.

        String vyosTrafficType="local";
        if ( (firewallRulesetName.contains("Egress") && isPrivate == true) || (firewallRulesetName.contains("Ingress") && isPrivate == false)){
            vyosTrafficType="in";
        } else if (!firewallRulesetName.contains("local") ){
            vyosTrafficType="out";
        }
        if (privateVlanTag != null && !privateVlanTag.isEmpty()){
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
                    s_logger.debug("Firewall ruleset does not exist: " + firewallRulesetName);
                    return false;
                }
            case ADD:
                if (manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, firewallRulesetName, defaultEgressPolicy, publicIp)) {
                    return true;
                }
                s_logger.debug("Adding Firewall ruleset with name: " + firewallRulesetName);
                 ArrayList<String> a_params = new ArrayList<String>();
                 a_params.add("set firewall name "+firewallRulesetName+" default-action '"+defaultFirewallAction+"'" );
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 action 'accept'");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 state established 'enable' ");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 state related 'enable' ");
                 a_params.add("set firewall name "+firewallRulesetName+" rule 1 description '"+description+"' ");

                 //allow port 22 to the router.
                 // TODO This needs to be secured!
                 if (vyosTrafficType == "local") {
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 action 'accept' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 destination port '22' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 protocol 'tcp' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 description 'allowSSHToRouter' ");
                     a_params.add("set firewall name "+firewallRulesetName+" rule 2 state new 'enable' ");

                 }

                 a_params.add("set interfaces ethernet "+interfaceName+vlanConfigString+" firewall "+vyosTrafficType+" name "+firewallRulesetName);
                 cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                 return true;

            case DELETE:
                if (!manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, firewallRulesetName, defaultEgressPolicy, publicIp)) {
                    return true;
                }
                s_logger.debug("Deleting Firewall ruleset with name: " + firewallRulesetName);
                //disassociate the firewall rulest from the network interface
                ArrayList<String> d_nic_params = new ArrayList<String>();
                d_nic_params.add("delete interfaces ethernet "+interfaceName+vlanConfigString+" firewall "+vyosTrafficType);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_nic_params));

                //Delete the firewall ruleset
                ArrayList<String> d_sub_params = new ArrayList<String>();
                d_sub_params.add("delete firewall name "+firewallRulesetName);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, d_sub_params));

                return true;
            default:
                s_logger.debug("Unrecognized command: "+prim);
                return false;

        }
    }
    /*
     * Private interface implementation
     */


    public boolean managePrivateInterface(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, long privateVlanTag, String privateGateway, String defaultEgressPolicy, String publicIp)
        throws ExecutionException {
        String interfaceName = _privateInterface;
        String firewallRulesetPrefix="private_"+Long.toString(privateVlanTag)+"_";
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
                    s_logger.debug("Private sub-interface does not exist: " + interfaceName +"."+ privateVlanTag +" address: "+ privateGateway);
                    return false;
                }
            case ADD:
                if (managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway, defaultEgressPolicy, publicIp)) {
                    return true;
                }

                // add vlan and privateGateway ip to private interface
                ArrayList<String> a_sub_params = new ArrayList<String>();
                a_sub_params.add("set interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" address "+privateGateway);

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_sub_params));


                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), firewallRulesetPrefix+"Ingress", defaultEgressPolicy, publicIp);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), firewallRulesetPrefix+"Egress", defaultEgressPolicy, publicIp);

                return true;
            case DELETE:
                if (!managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway, defaultEgressPolicy, publicIp)) {
                    return true;
                }

             // Delete ingress and egress firewall rule sets.
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), firewallRulesetPrefix+"Ingress", defaultEgressPolicy, publicIp);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), firewallRulesetPrefix+"Egress", defaultEgressPolicy, publicIp);

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
        String firewallRulesetPrefix="public_";
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
                    s_logger.debug("Public sub-interface exists: " + interfaceName +" "+publicVlanString );
                    return true;
                }
                else {
                    s_logger.debug("Public sub-interface does not exist: " + interfaceName +" "+ publicVlanString +" address: "+ publicIp);
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
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, firewallRulesetPrefix+"Ingress", defaultEgressPolicy, publicIp);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, firewallRulesetPrefix+"Egress", defaultEgressPolicy, publicIp);
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, publicVlanString, firewallRulesetPrefix+"local", defaultEgressPolicy, publicIp);

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

        if (publicVlanTag == null) {
            s_logger.debug("\n******************** Calling getPublicVlanTag from manageSrcNatRule*****************************\n");
            String publicVlanTagString=getPublicVlanTag(publicIp);
            if (publicVlanTagString != null) {
                publicVlanTag=Long.valueOf(publicVlanTagString);
            }
        }

        String publicInterfaceName=_publicInterface;
        //Add vlan to the public interface name
        if (publicVlanTag != null ) {
            publicInterfaceName=publicInterfaceName+"."+publicVlanTag.toString();
        }

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

                ArrayList<String> a_params = new ArrayList<String>();
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.DESCENDING+" }} outbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.DESCENDING+" }} source address '"+privateGateway+"'" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.DESCENDING+" }} translation address masquerade" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.DESCENDING+" }} description '"+cloudstackRuleName+"'");
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
        if (rule.getSrcVlanTag() != null) {
            publicVlanTag=Long.valueOf(parsePublicVlanTag(rule.getSrcVlanTag()));
        } else {
            s_logger.debug("\n******************** Calling getPublicVlanTag from manageDstNatRule*****************************\n");
            String publicVlanTagString=getPublicVlanTag(publicIp);
            if (publicVlanTagString != null) {
                publicVlanTag=Long.valueOf(publicVlanTagString);
            }
        }

        //Add vlan to the public interface name
        if (publicVlanTag != null ) {
            publicInterfaceName=publicInterfaceName+"."+publicVlanTag.toString();
        }
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
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} translation port '"+dstPortRangeString+"'" );
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} inbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} protocol '"+protocol+"'" );
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} translation address '"+dstIp+"'" );
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} description '"+cloudstackRuleName+"'" );
                a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} destination address '"+publicIp+"'" );

                if (srcPortRangeString != "") {
                    a_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} destination port '"+srcPortRangeString+"'" );

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


        Long publicVlanTag = null;
        if (rule.getSrcVlanTag() != null) {
            publicVlanTag=Long.valueOf(parsePublicVlanTag(rule.getSrcVlanTag()));
        } else {
            s_logger.debug("\n******************** Calling getPublicVlanTag from manageStcNatRule*****************************\n");
            String publicVlanTagString=getPublicVlanTag(publicIp);
            if (publicVlanTagString != null) {
                publicVlanTag=Long.valueOf(publicVlanTagString);
            }
        }
        long privateVlanTag = 0;

        //Add vlan to the public interface name
        if (publicVlanTag != null ) {
            publicInterfaceName=publicInterfaceName+"."+publicVlanTag.toString();
        }

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

                // Make sure the public IP referenced in this rule exists.
                if (managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp+"/32", privateVlanTag, null) == false ) {
                    throw new ExecutionException("Could not add the public Ip");
                }

                // add the static nat rule for the public IP
                ArrayList<String> a_params = new ArrayList<String>();
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.ASCENDING+" }} source address '"+privateIp+"'" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.ASCENDING+" }} outbound-interface '"+publicInterfaceName+"'" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.ASCENDING+" }} translation address '"+publicIp+"'" );
                a_params.add("set nat source rule {{ "+VyosNextIdNumber.ASCENDING+" }} description '"+cloudstackRuleName+"'" );
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

                ArrayList<String> b_params = new ArrayList<String>();
                b_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} inbound-interface '"+publicInterfaceName+"'" );
                b_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} destination address '"+publicIp+"'" );
                b_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} translation address '"+privateIp+"'" );
                b_params.add("set nat destination rule {{ "+VyosNextIdNumber.ASCENDING+" }} description '"+cloudstackRuleName+"'" );

                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, b_params));

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

                // Make sure the public IP referenced in this rule is removed.
                if (managePublicInterface(cmdList, VyosRouterPrimative.DELETE, publicVlanTag, publicIp+"/32", privateVlanTag, null) == false ) {
                    throw new ExecutionException("Could not add the public Ip");
                }
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

    public boolean setDefaultEgressPolicy(ArrayList<IVyosRouterCommand> cmdList, String privateFirewallRuleSetName, String publicFirewallRuleSetName, String action) throws ExecutionException {
        ArrayList<String> a_params = new ArrayList<String>();
        a_params.add("set firewall name "+privateFirewallRuleSetName+" default-action '"+action+"'" );
        a_params.add("set firewall name "+publicFirewallRuleSetName+" default-action '"+action+"'" );
        cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, a_params));

        return true;
    }

    public boolean manageFirewallRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, FirewallRuleTO rule) throws ExecutionException {
        //This will always interact with two firewall rulesets. One for the interface where packets enter the router and one for the interface where
        //packets exit the router.
        String firewallRuleVars="Firewall Rules Vars:";
        if (rule.getGuestCidr() != null) {
            firewallRuleVars=firewallRuleVars+" GuestCidr: "+rule.getGuestCidr();
        }
        if (rule.getSrcIp() != null) {
            firewallRuleVars=firewallRuleVars+" SrcIp: "+rule.getSrcIp();
        }
        if (rule.getSrcVlanTag() != null) {
            firewallRuleVars=firewallRuleVars+" SrcVlan: "+rule.getSrcVlanTag();
        }

        if (rule.getSourceCidrList() !=null) {
            firewallRuleVars=firewallRuleVars+" SourceCidrList: ";
            for (String curCidr : rule.getSourceCidrList() ) {
                firewallRuleVars=firewallRuleVars+curCidr+" ";
            }
        }
        s_logger.debug("\n*************************** manageFirewallRule called with "+firewallRuleVars+"*************************************\n");

        String guestCidr=rule.getGuestCidr();
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
        String guestVlanTag=rule.getSrcVlanTag();


        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
            cloudstackRuleName = genFirewallRuleName(rule.getId(), rule.getSrcVlanTag());
            privateInterfaceFirewallType="in";
            publicInterfaceFirewallType="out";
        } else { //Ingress
            cloudstackRuleName = genFirewallRuleName(rule.getId());
            guestVlanTag=getGuestVlanTag(rule.getId());
            privateInterfaceFirewallType="out";
            publicInterfaceFirewallType="in";
        }
        String privateFirewallRuleSetName="private_"+guestVlanTag+"_"+trafficType;
        String publicFirewallRuleSetName="public_"+trafficType;

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
                if (manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                //System rules are assigned in descending order. User rules in ascending. This ensures that system rules are applied only if no user rules match.
                VyosNextIdNumber ruleNamePlaceholder=VyosNextIdNumber.ASCENDING;
                if (rule.getType() == FirewallRule.FirewallRuleType.System) {
                    ruleNamePlaceholder=VyosNextIdNumber.DESCENDING;
                }
                privateNetworkGroupName=privateFirewallRuleSetName+"-{{ "+ruleNamePlaceholder+" }}";
                publicNetworkGroupName=publicFirewallRuleSetName+"-{{ "+ruleNamePlaceholder+" }}";

                String action = "accept";
                String protocol=rule.getProtocol();

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
                } else if (rule.getType() == FirewallRule.FirewallRuleType.System) {
                    s_logger.debug("*********************\nSomething Called for the creation of a system INGRESS firewall rule. This is a rule type that is currently not defined in this code:\n "+rule.toString()+"\n***************************");
                }

                if (protocol.equals(Protocol.ICMP.toString())) {
                    ArrayList<String> aParams = new ArrayList<String>();
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} action '"+action+"'");

                    if (rule.getIcmpType().equals(-1) || rule.getIcmpCode().equals(-1)) {
                        //allow all icmp types
                        aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp type-name 'any'");
                    }else {
                        aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp type '"+rule.getIcmpType()+"'");
                        aParams.add( "set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp code '"+rule.getIcmpCode()+"'");
                    }
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} protocol 'icmp'");
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} state new 'enable'");
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} description '"+cloudstackRuleName+"'");

                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} action 'accept'");

                    if (rule.getIcmpType().equals(-1) || rule.getIcmpCode().equals(-1)) {
                        //allow all icmp types
                        aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp type-name 'any'");
                    }else {
                        aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp type '"+rule.getIcmpType()+"'");
                        aParams.add( "set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} icmp code '"+rule.getIcmpCode()+"'");
                    }

                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} protocol 'icmp'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} state new 'enable'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} description '"+cloudstackRuleName+"'");

                 // Create the network-group and associated it with the firewall rule
                    aParams.add("set firewall group network-group '"+privateNetworkGroupName+"'");
                    aParams.add("set firewall group network-group '"+publicNetworkGroupName+"'");

                    buildFirewallRuleCidrList(aParams, rule, privateNetworkGroupName, publicNetworkGroupName, ruleNamePlaceholder, privateFirewallRuleSetName, publicFirewallRuleSetName, guestCidr);
                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, aParams));

                }
                else if (protocol.equalsIgnoreCase(Protocol.TCP.toString()) || protocol.equalsIgnoreCase(Protocol.UDP.toString()) || protocol.equalsIgnoreCase(Protocol.ALL.toString())) {

                    ArrayList<String> aParams = new ArrayList<String>();
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} action '"+action+"'");
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} protocol '"+protocol+"'");
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} state new 'enable'");
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} description '"+cloudstackRuleName+"'");

                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} action '"+action+"'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} protocol '"+protocol+"'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} state new 'enable'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} description '"+cloudstackRuleName+"'");

                    // Build source port range. In Vyos these are called destination ports.
                    int[] srcPortRange = rule.getSrcPortRange();
                    String srcPortRangeString="";
                    if (srcPortRange != null) {
                        if (srcPortRange.length == 1 || srcPortRange[0] == srcPortRange[1]) {
                            srcPortRangeString = String.valueOf(srcPortRange[0]);
                        } else {
                            srcPortRangeString = String.valueOf(srcPortRange[0]) + "-" + String.valueOf(srcPortRange[1]);
                        }
                        aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} destination port '"+srcPortRangeString+"'");

                        aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} destination port '"+srcPortRangeString+"'");
                    }

                    // Build allowed cidr range
                    // To support multiple cidrs per firewall rule, a network group will be created for each rule.
                    // group names will be the firewall ruleset name and the rule name separated by a dash.
                    // This is to ensure that the names do not collide.

                    // Create the network-group and associated it with the firewall rule
                    aParams.add("set firewall group network-group '"+privateNetworkGroupName+"'");
                    aParams.add("set firewall group network-group '"+publicNetworkGroupName+"'");


                    buildFirewallRuleCidrList(aParams, rule, privateNetworkGroupName, publicNetworkGroupName, ruleNamePlaceholder, privateFirewallRuleSetName, publicFirewallRuleSetName, guestCidr);

                    // Build destination address
                    if (rule.getSrcIp() != null){
                        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress){
                            //From my understanding this should be an error state. srcIP should always be null for Egress rules.
                            throw new ExecutionException("Egress Firewall Rule has a non null value in srcIp: "+rule.getSrcIp());
                        }
                        else{
                            //The public Ip cannot be used in the firewall rules since Vyos performs destination nat before applying any firewall rules.
                            //We must use the guest network cidr instead.

                            //If guestCidr is null we have to query Vyos for the private subnet associated with this rule.
                            if (guestCidr == null || guestCidr.isEmpty() ) {
                                    guestCidr=getPrivateSubnet(rule.getId());
                                }
                            }
                            aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} destination address '"+guestCidr+"'");

                            aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} destination address '"+guestCidr+"'");
                        }

                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, VyosRouterCommandType.WRITE, aParams));
                }
                else {
                    throw new ExecutionException("The protocol is not supported: "+protocol+" supported are: "+Protocol.TCP.toString()+" -- "+Protocol.UDP.toString()+" -- "+Protocol.ALL.toString());
                    //return false;
                }

                return true;
            case DELETE:
                if (!manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                privateRuleName=getVyosRuleNumber(privateFirewallRuleSetName, cloudstackRuleName);
                publicRuleName=getVyosRuleNumber(publicFirewallRuleSetName, cloudstackRuleName);
                if (privateRuleName == 0 || publicRuleName == 0) {
                    throw new ExecutionException("Could not find a Vyos rule number associated with the Cloudstack ruleId: "+Long.toString(rule.getId()));
                }
                privateNetworkGroupName=privateFirewallRuleSetName+"-"+privateRuleName;
                publicNetworkGroupName=publicFirewallRuleSetName+"-"+publicRuleName;


                // Derive the proper vyos interface names for the private and public networks.
                Map<String, String> privateInterfaces=getVyosInterfacesWithFirewalls(privateFirewallRuleSetName);
                Map<String, String> publicInterfaces=getVyosInterfacesWithFirewalls(publicFirewallRuleSetName);

                //Temporarily disassociate the firewall ruleset from its interface so we can delete the firewall rule.
                ArrayList<String> d_eth_assoc_params = new ArrayList<String>();
                for (String curInterfaceNameString : privateInterfaces.keySet() ) {
                    d_eth_assoc_params.add("delete interface ethernet "+curInterfaceNameString+" firewall "+privateInterfaceFirewallType);
                }

                for (String curInterfaceNameString : publicInterfaces.keySet() ) {
                    d_eth_assoc_params.add("delete interface ethernet "+curInterfaceNameString+" firewall "+publicInterfaceFirewallType);
                }

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
                for (String curInterfaceNameString : privateInterfaces.keySet() ) {
                    a_eth_assoc_params.add("set interface ethernet "+curInterfaceNameString+" firewall "+privateInterfaceFirewallType+" name "+privateFirewallRuleSetName);
                }

                for (String curInterfaceNameString : publicInterfaces.keySet() ) {
                    a_eth_assoc_params.add("set interface ethernet "+curInterfaceNameString+" firewall "+publicInterfaceFirewallType+" name "+publicFirewallRuleSetName);
                }

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

    // This method queries Vyos and returns the subnet for the private network configured for the router.
    //To do this we parse the current config for the _privateInterface variable and return its associated subnet.
    // TODO This will not work if multiple guest networks use the same vyos router.
    protected String getPrivateSubnet(long firewallRuleId) throws ExecutionException {
        boolean isSameNic=false;
        if (_privateInterface.equals(_publicInterface)) {
            isSameNic=true;
        }
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            ArrayList<String> privateNicAddressLines= new ArrayList<String>();
            String guestVlan="";
            while( (line=bufReader.readLine()) != null ) {
                if (isSameNic) {
                    if (line.contains(_privateInterface) && line.contains("address")) {
                        privateNicAddressLines.add(line);
                    } else if (line.contains(_privateInterface) && line.contains("firewall") && line.contains("private_")) {
                        //get the virtual interface (VLAN) value for the private interface.
                        guestVlan=line.split("vif ")[1].split(" ")[0];
                    }
                }else {
                    if (line.contains(_privateInterface) && line.contains("address")) {
                        String curIp=line.split("'")[1].split("/")[0];
                        String curCidr=line.split("'")[1].split("/")[1];
                        curIp=curIp.replaceFirst("\\d+$", "0")+"/"+curCidr;
                        return curIp;
                    }
                }
            }

            //Use the guestVlan found along with the recorded lines for the private nic to derive the subnet.
            if (isSameNic && !guestVlan.isEmpty()) {
                for (String curLine : privateNicAddressLines) {
                    if (curLine.contains(_privateInterface) && curLine.contains("vif "+guestVlan) && curLine.contains("address")) {
                        String curIp=curLine.split("'")[1].split("/")[0];
                        String curCidr=curLine.split("'")[1].split("/")[1];
                        curIp=curIp.replaceFirst("\\d+$", "0")+"/"+curCidr;
                        return curIp;
                    }
                }
            }
            System.out.println("in getPrivateSubnet. could not determine the Guest Subnet. Private Interface Name: "+_privateInterface+" Public Interface Name: "+_publicInterface);
            s_logger.debug("in getPrivateSubnet. could not determine the Guest Subnet. Private Interface Name: "+_privateInterface+" Public Interface Name: "+_publicInterface);
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        }

    }


/*
    //Given the ID of a firewall rule query cloudstack to find the guest network subnet associated with the rule.
    protected String getPrivateSubnet(long firewallRuleId) throws ExecutionException {
        if (_fwRulesDao == null ) {
            throw new ExecutionException("_fwRulesDao is null.");
        }
        if (_networkDao == null ) {
            throw new ExecutionException("_networkDao is null.");
        }
        FirewallRuleVO fwr = _fwRulesDao.findById(firewallRuleId);
        long nwId = fwr.getNetworkId();
        NetworkVO nw = _networkDao.findById(nwId);
        String cidr=nw.getCidr();
        s_logger.debug("\n******* In getprivatesubnet. Returning subnet of: "+cidr+"*********************\n");
        return cidr;

    }
*/
  //Sometimes cloudstack does not pass a valid cidr for firewall rules. In that case, use the guest network cidr listed in vyos.
    // TODO This will not work if multiple guest networks use the same vyos router.
    private String getPrivateCidrBits(String guestCidr, long firewallRuleId) throws ExecutionException {

        if (guestCidr == null || guestCidr.isEmpty()) {
            guestCidr=this.getPrivateSubnet(firewallRuleId);

        }
        if (guestCidr == null)
            return null;
        String cidr= guestCidr.split("/")[1];
        return cidr;

    }

    //Given the ID of a firewall rule query cloudstack to find the guest network vlan associated with the rule.
    protected String getGuestVlanTag(long firewallRuleId) throws ExecutionException {
        if (_fwRulesDao == null ) {
            throw new ExecutionException("_fwRulesDao is null.");
        }
        if (_networkDao == null ) {
            throw new ExecutionException("_networkDao is null.");
        }
        FirewallRuleVO fwr = _fwRulesDao.findById(firewallRuleId);
        long nwId = fwr.getNetworkId();
        NetworkVO nw = _networkDao.findById(nwId);
        String guestVlanTag = BroadcastDomainType.getValue(nw.getBroadcastUri());
        s_logger.debug("\n******* In getGuestVlanTag. Returning guestVlanTag of: "+guestVlanTag+"*********************\n");
        return guestVlanTag;


    }



    //There appear to be situations where Cloudstack may not pass to us the vlan tag associated with a given public IP range.
    //This information is needed to properly add IPs to Vyos in response to requests from Cloudstack. In that event, we can derive the
    // proper vlan tag for the ip range by querying Vyos.
    //This is predicated by the notion that every IP octet is in a separate vlan so
    //if the new IP is 192.168.99.35, then there will be only one VLAN with addresses matching 192.168.99.*
    private String getPublicVlanTag(String publicIp) throws ExecutionException {
        try {
            //search the vyos config for a vlan with ips matching the first 3 octets of the IP passed in.
            String firstThreeOctets=publicIp.replaceFirst("\\d+$", "");
            System.out.println("in getPublicVlanTag. Searching for firstThreeOctets of "+firstThreeOctets);
            s_logger.debug("\n************************ in getPublicVlanTag. Searching for firstThreeOctets of "+firstThreeOctets+" ****************************\n");

            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            while( (line=bufReader.readLine()) != null ) {
                if (line.contains(firstThreeOctets) && line.contains("vif")) {
                    String curVlan=line.split("vif ")[1].split(" ")[0];
                    return curVlan;
                }
            }
            System.out.println("in getPublicVlanTag. no vlan found for firstThreeOctets of "+firstThreeOctets);
            s_logger.debug("\n************************ in getPublicVlanTag. no vlan found for firstThreeOctets of "+firstThreeOctets+" ****************************\n");
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        }

    }

    //Refactor to remove redundancy in this process since ICMP and TCP/UDP both use this code. This builds our list of cidrs associated with the
    //given firewall rule and creates the vyos commands necessary to implement them.
    private void buildFirewallRuleCidrList(ArrayList<String> aParams, FirewallRuleTO rule, String privateNetworkGroupName, String publicNetworkGroupName, VyosNextIdNumber ruleNamePlaceholder, String privateFirewallRuleSetName, String publicFirewallRuleSetName, String guestCidr) throws ExecutionException {
        List<String> ruleSrcCidrList = rule.getSourceCidrList();

        if (ruleSrcCidrList.size() > 0) { // a cidr was entered, modify as needed...
            for (int i = 0; i < ruleSrcCidrList.size(); i++) {
                String curCidr=ruleSrcCidrList.get(i).trim();
                if (curCidr.equals("0.0.0.0/0")) { // allow any
                    if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                        curCidr=guestCidr;
                        if (curCidr == null || curCidr.isEmpty()) {
                            curCidr = getPrivateSubnet(rule.getId());
                        }
                    } else {
                        //Do not associate the network group with the firewall rule since it is set to allow all.
                        continue;
                    }
                } else if (curCidr.contains("/0")) {
                  //The cidr passed in from cloudstack is /0 but contains nonzero ip octets. use cidr from the guest network in vyos.
                    String defaultCidrBits=getPrivateCidrBits(guestCidr, rule.getId());
                    curCidr = curCidr.split("/")[0]+"/"+defaultCidrBits;
                }
                if (curCidr != null && !curCidr.isEmpty() ) {
                    //Build network group cidrs
                    aParams.add("set firewall group network-group "+privateNetworkGroupName+" network '"+curCidr+"'");
                    aParams.add("set firewall group network-group "+publicNetworkGroupName+" network '"+curCidr+"'");

                    //Associate the network group with the firewall rule.
                    aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} source group network-group '"+privateNetworkGroupName+"'");
                    aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} source group network-group '"+publicNetworkGroupName+"'");

                }

            }
        } else { // no cidr was entered, so allow ALL according to firewall rule type
            if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                String privateSubnet=guestCidr;
                if (privateSubnet == null || privateSubnet.isEmpty()) {
                    privateSubnet = getPrivateSubnet(rule.getId());
                }
                aParams.add("set firewall group network-group "+privateNetworkGroupName+" network '"+privateSubnet+"'");
                aParams.add("set firewall group network-group "+publicNetworkGroupName+" network '"+privateSubnet+"'");

                //Associate the network group with the firewall rule.
                aParams.add("set firewall name "+privateFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} source group network-group '"+privateNetworkGroupName+"'");
                aParams.add("set firewall name "+publicFirewallRuleSetName+" rule {{ "+ruleNamePlaceholder+" }} source group network-group '"+publicNetworkGroupName+"'");
            }

        }

    }

    // Function to make calls to the Vyos Router API.
    // All API calls will end up going through this function.
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

                int nextId=-1;
                boolean isDescending=false;
                for (String curCommand : commands) {
                    //Get Next available ID number for the current command set.
                    //For write operations the unique rule id number must be ascertained at write time to eliminate race conditions.
                    if (nextId == -1) {
                        if (curCommand.contains("{{ "+VyosNextIdNumber.ASCENDING+" }}") ) {
                            nextId=getVyosRuleNumber(curCommand, "", isDescending);
                        } else if (curCommand.contains("{{ "+VyosNextIdNumber.DESCENDING+" }}") ) {
                            isDescending=true;
                            nextId=getVyosRuleNumber(curCommand, "", isDescending);
                        }
                    }
                    if (nextId == 0) {
                        throw new ExecutionException("Could not determine a valid new Vyos rule number for the current Command: "+curCommand);
                    }

                    if (!isDescending && nextId != -1) {
                        curCommand=curCommand.replace("{{ "+VyosNextIdNumber.ASCENDING+" }}", String.valueOf(nextId));
                    } else if (isDescending && nextId != -1) {
                        curCommand=curCommand.replace("{{ "+VyosNextIdNumber.DESCENDING+" }}", String.valueOf(nextId));
                    }
                    //print the current command now that any rule number placeholders have been set to the next available vyos id.
                    //System.out.println(curCommand);
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
        //s_logger.debug(debug_msg); // this can be commented if we don't want to show each request in the log.
        return responseBody;
    }

    private synchronized boolean requestWithCommit(ArrayList<IVyosRouterCommand> commandList) throws ExecutionException {
        // This loops through and executes all the commands in the commandList array.
        //If any commands fail then all commands are rolled back. Else, commands are saved to the vyos boot config
        for (IVyosRouterCommand commands : commandList) {
            int count=1;
            for (String curCommand : commands.getParams() ) {
                s_logger.debug(count+":"+curCommand);
            }
            count++;
            s_logger.debug("\n");
        }

        boolean result = true;
        boolean saving = false;
        if (commandList.size() > 0) {
            try {
                // RUN THE SEQUENCE OF COMMANDS
                //System.out.println("Current List of Commands being sent to Vyos:");
                //int i=1;
                for (IVyosRouterCommand command : commandList) {
                    //System.out.println("Command Set # "+i);
                    //i++;
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
                    throw new ExecutionException("Failed to save the new vyos config to disk. "+e.getMessage());
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
    //  If no cloudstackRuleId is passed in then this will return the next available vyos rule number
    //  commandIdentificationString is a String that is unique enough to identify all lines associated with the
    //      type of Vyos command we are looking for (EG Source Nat, Destination Nat, Firewall...etc)
    protected int getVyosRuleNumber(String commandIdentificationString, String cloudstackRuleName) throws ExecutionException {
        return getVyosRuleNumber(commandIdentificationString, cloudstackRuleName, false);
    }
    protected int getVyosRuleNumber(String commandIdentificationString, String cloudstackRuleName, boolean descendingOrder) throws ExecutionException {
        try {
            //Build Identification String add operations.
            if (commandIdentificationString.contains("{{ "+VyosNextIdNumber.ASCENDING+" }}") || commandIdentificationString.contains("{{ "+VyosNextIdNumber.DESCENDING+" }}") ) {
                commandIdentificationString=commandIdentificationString.split("rule")[0];
            }
            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            TreeMap<Integer, Integer> existingRuleNumbers = new TreeMap<Integer, Integer>();
            while( (line=bufReader.readLine()) != null ) {
                //return the next highest rule number
                if (cloudstackRuleName == null || cloudstackRuleName == ""){
                    if (line.contains(commandIdentificationString) && line.contains("rule") ){
                        int curRule=Integer.parseInt(line.split("rule")[1].trim().split(" ")[0]);
                        existingRuleNumbers.put(curRule, curRule);
                    }
                } else {
                    if (line.contains(commandIdentificationString) && line.contains("description") && cloudstackRuleName.equals(line.split("'")[1].trim())) {
                        String curRuleNumber=line.split("rule")[1].trim().split(" ")[0];
                        return Integer.parseInt(curRuleNumber);
                    }
                }
            }

            if (cloudstackRuleName == null || cloudstackRuleName == ""){
                return getNextAvailableVyosRuleNumber( existingRuleNumbers, descendingOrder);
            }

            return 0;


        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        }

    }

    //To delete firewalls we must first dissassociate the firewall from all interfaces that use it.
    //  If no firewall name is passed in then return all interfaces that have firewalls associated with them.
    private Map<String,String> getVyosInterfacesWithFirewalls() throws ExecutionException {
        return getVyosInterfacesWithFirewalls("");
    }
    private Map<String,String> getVyosInterfacesWithFirewalls(String firewallName) throws ExecutionException {
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add("run show configuration commands");
            String response = request(VyosRouterMethod.SHELL, VyosRouterCommandType.READ, params);

            Map<String, String> interfacesWithFirewalls = new HashMap<String, String>();
            BufferedReader bufReader = new BufferedReader(new StringReader(response));
            String line=null;
            while( (line=bufReader.readLine()) != null ) {
                if (line.trim().contains("set interfaces") && line.trim().contains("firewall") ) {
                    String vyosFirewallName=line.trim().split("'")[1];
                    String vyosInterfaceName=line.trim().split(" ")[3];
                    if (line.trim().contains("vif")) {
                        vyosInterfaceName=line.trim().split(" ")[3]+" vif "+line.trim().split(" ")[5];
                    }
                    if (firewallName.isEmpty() || (!vyosInterfaceName.isEmpty() && firewallName.equals(vyosFirewallName) ) ) {
                        interfacesWithFirewalls.put(vyosInterfaceName, vyosFirewallName);
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
