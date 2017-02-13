package com.cloud.network.resource;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
//import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class VyosRouterResource implements ServerResource{

	private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    //private String _key;
    private Integer _numRetries;
    private Integer _timeoutInSeconds;
    private String _publicZone;
    private String _privateZone;
    private String _publicInterface;
    private String _privateInterface;
    //private String _publicInterfaceType;
    //private String _privateInterfaceType;
    private String _virtualRouter;
    //private String _threatProfile;
    //private String _logProfile;
    //private String _pingManagementProfile;
    private static final Logger s_logger = Logger.getLogger(VyosRouterResource.class);
    
    private static SSHClient s_sshClient;
    private static final String s_vyosShellScript = "#!/bin/bash\nallParams=\"$@\"\nsource /opt/vyatta/etc/functions/script-template\n$allParams\n";
    private static final String s_vyosScriptName="testShellScript.sh";
    
    protected enum VyosRouterMethod {
        SHELL, HTTPSTUB;
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

            _publicZone = (String)params.get("publicnetwork");
            if (_publicZone == null) {
                throw new ConfigurationException("Unable to find public zone");
            }

            _privateZone = (String)params.get("privatenetwork");
            if (_privateZone == null) {
                throw new ConfigurationException("Unable to find private zone");
            }

            _virtualRouter = (String)params.get("pavr");
            if (_virtualRouter == null) {
                throw new ConfigurationException("Unable to find virtual router");
            }

           // _threatProfile = (String)params.get("patp");
           // _logProfile = (String)params.get("palp");

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 1);
            _timeoutInSeconds = NumbersUtil.parseInt((String)params.get("timeout"), 300);

            // Open a socket           
            initializeVyosIntegration();

            /*
            // check that the threat profile exists if one was specified
            if (_threatProfile != null) {
                try {
                    boolean has_profile = getThreatProfile(_threatProfile);
                    if (!has_profile) {
                        throw new ConfigurationException("The specified threat profile group does not exist.");
                    }
                } catch (ExecutionException e) {
                    throw new ConfigurationException(e.getMessage());
                }
            }
			*/
            /*
            // check that the log profile exists if one was specified
            if (_logProfile != null) {
                try {
                    boolean has_profile = getLogProfile(_logProfile);
                    if (!has_profile) {
                        throw new ConfigurationException("The specified log profile does not exist.");
                    }
                } catch (ExecutionException e) {
                    throw new ConfigurationException(e.getMessage());
                }
            }
			*/
            // get public interface type
            /*
            try {
                _publicInterfaceType = getInterfaceType(_publicInterface);
                if (_publicInterfaceType.equals("")) {
                    throw new ConfigurationException("The specified public interface is not configured on the Palo Alto.");
                }
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
            }

            // get private interface type
            try {
                _privateInterfaceType = getInterfaceType(_privateInterface);
                if (_privateInterfaceType.equals("")) {
                    throw new ConfigurationException("The specified private interface is not configured on the Palo Alto.");
                }
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
            }
			*/
            //_pingManagementProfile = "Ping";
         // TODO I do not think this is needed?
            /*
            try {
                ArrayList<IVyosRouterCommand> cmdList = new ArrayList<IVyosRouterCommand>();
             
            	//managePingProfile(cmdList, VyosRouterPrimative.ADD);
                boolean status = requestWithCommit(cmdList);
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
            }
            */

            return true;
        } catch (Exception e) {
        	try {
        		s_sshClient.disconnect();
        		throw new ConfigurationException(e.getMessage());
        	}catch (Exception f) {
        		throw new ConfigurationException(e.getMessage()+" "+f.getMessage());
        	}            
        } finally {
        	try {
        		s_sshClient.disconnect();
        	}catch (Exception e) {
        		throw new ConfigurationException(e.getMessage());
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
		// TODO This will need to be handled once VyosRouter is refactored to be a drop in replacement for VRouter
		return true;
	}

	@Override
	public boolean stop() {
		// TODO This will need to be handled once VyosRouter is refactored to be a drop in replacement for VRouter
		return true;
	}

	@Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.ExternalFirewall, id);
    }
	
	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
		
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
        if (s_sshClient == null || !s_sshClient.isConnected()) {
        	try {
        		s_sshClient = new SSHClient();
        		s_sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        		s_sshClient.loadKnownHosts();
	
        		s_sshClient.connect(_ip);
        		s_sshClient.authPassword(_username, _password);
        	}catch(Exception e){
        		s_logger.error(e);
        		s_sshClient.disconnect();
        		return false;
        	}        	
        }    
        return true;
    }
    
    public void executeVyosRouterCommand(String shellCommand)
    		throws IOException {
    	 if (s_sshClient == null || !s_sshClient.isConnected()) {
    		 refreshVyosRouterConnection();
    	 }
    	 net.schmizz.sshj.connection.channel.direct.Session session = s_sshClient.startSession();
    	try {              
    		
    		net.schmizz.sshj.connection.channel.direct.Session.Command cmd = session.exec(shellCommand);
            System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
            cmd.join(5, TimeUnit.SECONDS);
            System.out.println("\n** exit status: " + cmd.getExitStatus());
        } finally {
        	session.close();
        }
    }
    
    // Write the shell script we will be using to execute vyos commands to disk on the router.
    public void initializeVyosIntegration() throws IOException {
    	
    	if (s_sshClient == null || !s_sshClient.isConnected()) {
   		 refreshVyosRouterConnection();
   		}
    	executeVyosRouterCommand("echo -e \""+s_vyosShellScript+"\" > ~/"+s_vyosScriptName);
    	executeVyosRouterCommand("chmod +x ~/"+s_vyosScriptName);
    	
    	
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
    	//refreshPaloAltoConnection();
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
                implementGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);
            } else {
                // Remove the guest network:
                shutdownGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);
            }

            boolean status = requestWithCommit(commandList);

            results[i++] = ip.getPublicIp() + " - success";
        } catch (ExecutionException e) {
            s_logger.error(e);

            //if (numRetries > 0 && refreshPaloAltoConnection()) {
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
            String privateGateway, String privateSubnet, long privateCidrNumber) throws ExecutionException {
            privateSubnet = privateSubnet + "/" + privateCidrNumber;

            managePrivateInterface(cmdList, VyosRouterPrimative.ADD, privateVlanTag, privateGateway + "/" + privateCidrNumber);

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
                managePublicInterface(cmdList, VyosRouterPrimative.ADD, publicVlanTag, publicIp + "/32", privateVlanTag);
                manageSrcNatRule(cmdList, VyosRouterPrimative.ADD, type, publicVlanTag, publicIp + "/32", privateVlanTag, privateGateway + "/" + privateCidrNumber);
             // TODO I do not think this is needed?
            	//manageNetworkIsolation(cmdList, VyosRouterPrimative.ADD, privateVlanTag, privateSubnet, privateGateway);
            }

            String msg =
                "Implemented guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrNumber;
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + publicIp : "";
            s_logger.debug(msg);
        }
    
    private void shutdownGuestNetwork(ArrayList<IVyosRouterCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String sourceNatIpAddress, long privateVlanTag,
            String privateGateway, String privateSubnet, long privateCidrSize) throws ExecutionException {
            privateSubnet = privateSubnet + "/" + privateCidrSize;

            // remove any orphaned egress rules if they exist...
            //removeOrphanedFirewallRules(cmdList, privateVlanTag);

            if (type.equals(GuestNetworkType.SOURCE_NAT)) {
               // TODO I do not think this is needed?
            	//manageNetworkIsolation(cmdList, VyosRouterPrimative.DELETE, privateVlanTag, privateSubnet, privateGateway);
                manageSrcNatRule(cmdList, VyosRouterPrimative.DELETE, type, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, privateGateway + "/" + privateCidrSize);
                managePublicInterface(cmdList, VyosRouterPrimative.DELETE, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag);
            }

            managePrivateInterface(cmdList, VyosRouterPrimative.DELETE, privateVlanTag, privateGateway + "/" + privateCidrSize);

            String msg = "Shut down guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrSize;
            msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + sourceNatIpAddress : "";
            s_logger.debug(msg);
        }
    
    /*
     * Firewall rule entry point
     */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {     
    	//refreshPaloAltoConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {
        FirewallRuleTO[] rules = cmd.getRules();
        try {
            ArrayList<IVyosRouterCommand> commandList = new ArrayList<IVyosRouterCommand>();

            for (FirewallRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageFirewallRule(commandList, VyosRouterPrimative.ADD, rule);
                } else {
                    manageFirewallRule(commandList, VyosRouterPrimative.DELETE, rule);
                }
            }

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            //if (numRetries > 0 && refreshPaloAltoConnection()) {
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
        //refreshPaloAltoConnection();
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

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

          //if (numRetries > 0 && refreshPaloAltoConnection()) {
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
        //refreshPaloAltoConnection();
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

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

          //if (numRetries > 0 && refreshPaloAltoConnection()) {
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
    public boolean manageFirewallRulesets(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, String interfaceName, String privateVlanTag, String trafficType)
            throws ExecutionException {    
    	String firewallRulesetName=privateVlanTag+"_"+trafficType;
    	String vyosTrafficType="in";
    	if (trafficType == "EGRESS"){
    		vyosTrafficType="out";
    	}
    	// TODO This only works with a deny defaultEgressPolicy
    	switch (prim) {
    		case CHECK_IF_EXISTS:
            // check if one exists already
        	// 
    			Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show firewall name "+firewallRulesetName);                
                String response = request(VyosRouterMethod.SHELL, params);
                if (!response.contains("Invalid firewall instance")) { //(!response.contains("Invalid") && !response.contains("empty") && response != "") {                
                	s_logger.debug("Firewall ruleset exists: " + firewallRulesetName);
                	return true;
                }
                else {
                	s_logger.debug("Firewall ruleset does not exist: " + firewallRulesetName+" response: " + response);
                	return false;
                }
    		case ADD:
    			if (manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, trafficType)) {
                    return true;
                }
    			 Map<String, String> a_params = new HashMap<String, String>();
                 a_params.put("type", "writeMultiple");
                 a_params.put("command1", "set firewall name "+firewallRulesetName+" default-action 'drop'" );
                 a_params.put("command2", "set firewall name "+firewallRulesetName+" rule 1 action 'accept' ");
                 a_params.put("command3", "set firewall name "+firewallRulesetName+" rule 1 state established 'enable' ");
                 a_params.put("command4", "set firewall name "+firewallRulesetName+" rule 1 state related 'enable' ");
                 a_params.put("command5", "set interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" firewall "+vyosTrafficType+" name "+firewallRulesetName);
                 cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));
                 
                 return true;
    			
    		case DELETE:
    			if (!manageFirewallRulesets(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, interfaceName, privateVlanTag, trafficType)) {
                    return true;
                }
    			Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "writeMultiple");
                d_sub_params.put("command1", "delete interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" firewall "+vyosTrafficType);
                d_sub_params.put("command2", "delete firewall name "+firewallRulesetName);
                                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
    			
    		default:
                s_logger.debug("Unrecognized command.");
                return false;
    	
    	}    	
    }
    /*
     * Private interface implementation
     */
    

    public boolean managePrivateInterface(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, long privateVlanTag, String privateGateway)
        throws ExecutionException {
        String interfaceName = _privateInterface; //genPrivateInterfaceName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	// 
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" address");                
                String response = request(VyosRouterMethod.SHELL, params);
                if (response.contains(privateGateway)) { //(!response.contains("Invalid") && !response.contains("empty") && response != "") {                
                	s_logger.debug("Private sub-interface exists: " + interfaceName +"."+privateVlanTag );
                	return true;
                }
                else {
                	s_logger.debug("Private sub-interface does not exist: " + interfaceName +"."+ privateVlanTag +" address: "+ privateGateway +" response: " + response);
                	return false;
                }
                

            case ADD:
                if (managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway)) {
                    return true;
                }

                // add vlan and privateGateway ip to private interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "write");
                a_sub_params.put("command", "set interfaces ethernet "+interfaceName+" vif "+privateVlanTag+" address "+privateGateway+"/24");
                                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_sub_params));            
                
                
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), "INGRESS");
                manageFirewallRulesets(cmdList, VyosRouterPrimative.ADD, interfaceName, Long.toString(privateVlanTag), "EGRESS");

                return true;

            case DELETE:
                if (!managePrivateInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway)) {
                    return true;
                }

             // Delete ingress and egress firewall rule sets.
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), "INGRESS");
                manageFirewallRulesets(cmdList, VyosRouterPrimative.DELETE, interfaceName, Long.toString(privateVlanTag), "EGRESS");
                
                // delete vlan and privateGateway ip from private interface
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "write");
                d_sub_params.put("command", "delete interfaces ethernet "+interfaceName+" vif "+privateVlanTag);
                                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
                
                
                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }
    
    /*
     * Public Interface implementation
     */    

    public boolean managePublicInterface(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, Long publicVlanTag, String publicIp, long privateVlanTag)
        throws ExecutionException {
        String interfaceName=_publicInterface;
        String vlanString="";
        if (publicVlanTag != null) {
        	vlanString=" vif "+publicVlanTag;
        }
        		

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show interfaces ethernet "+interfaceName+vlanString+" address");                
                String response = request(VyosRouterMethod.SHELL, params);
                if (response.contains(publicIp)) { //(!response.contains("Invalid") && !response.contains("empty") && response != "") {                
                	s_logger.debug("Public sub-interface exists: " + interfaceName +"."+publicVlanTag );
                	return true;
                }
                else {
                	s_logger.debug("Public sub-interface does not exist: " + interfaceName +"."+ publicVlanTag +" address: "+ publicIp +" response: " + response);
                	return false;
                }
                

            case ADD:
                if (managePublicInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag)) {
                    return true;
                }

                // add IP to the sub-interface
                // add vlan and public ip to public interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "write");
                a_sub_params.put("command", "set interfaces ethernet "+interfaceName+vlanString+" address "+publicIp+"/24");
                                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_sub_params));            
                

                return true;

            case DELETE:
                if (!managePublicInterface(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag)) {
                    return true;
                }

                // delete IP from sub-interface...
                // delete vlan and public ip from public interface
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "write");
                d_sub_params.put("command", "delete interfaces ethernet "+interfaceName+vlanString);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));

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
    	
        String srcNatName = Long.toString(privateVlanTag);
        String srcNatCreationString="set nat source rule "+srcNatName;

        switch (prim) {

            case CHECK_IF_EXISTS:                
            	// check if one exists already
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show configuration commands");                
                String response = request(VyosRouterMethod.SHELL, params);
                if (response.contains(srcNatCreationString)) { //(!response.contains("Invalid") && !response.contains("empty") && response != "") {                
                	s_logger.debug("Source NAT exists: " + srcNatName );
                	return true;
                }
                else {
                	s_logger.debug("Source NAT does not exist: " + srcNatName+" response: " + response);
                	return false;
                }
                

            case ADD:
                if (manageSrcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }

             // Build the parameters needed for vyos
                
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "writeMultiple");
                a_params.put("command1", srcNatCreationString+" outbound-interface '"+_publicInterface+"'" );
                a_params.put("command2", srcNatCreationString+" source address '"+privateGateway+"/24'" );
                a_params.put("command3", srcNatCreationString+" translation address masquerade" );
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));

                return true;

            case DELETE:
                if (!manageSrcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }
             
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "write");
                d_sub_params.put("command", "delete nat source rule " +srcNatName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));

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
        //String publicIp = rule.getSrcIp();
        String dstIp= rule.getDstIp();
        String dstNatName = Long.toString(rule.getId());

        String publicInterfaceName=_publicInterface;
        String dstNatCreationString="set nat destination rule "+dstNatName;
        

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show configuration commands");                
                String response = request(VyosRouterMethod.SHELL, params);
                if (response.contains(dstNatCreationString)) {                 
                	s_logger.debug("Destination NAT exists: " + dstNatName );
                	return true;
                }
                else {
                	s_logger.debug("Destination NAT does not exist: " + dstNatName+" response: " + response);
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
                  //  manageService(cmdList, VyosRouterPrimative.ADD, protocol, portRange, null);
                    
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
                  //  manageService(cmdList, VyosRouterPrimative.ADD, protocol, portRange, null);
                    
                } 

                // add public IP to the sub-interface
             // TODO DO I NEED THIS????  Build the parameters needed for vyos
                /*
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip");
                a_sub_params.put("element", "<entry name='" + publicIp + "/32'/>");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_sub_params));
                */
                
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "writeMultiple");
                a_params.put("command1", dstNatCreationString+" destination port '"+dstPortRangeString+"'" );
                a_params.put("command2", dstNatCreationString+" inbound-interface '"+publicInterfaceName+"'" );
                a_params.put("command3", dstNatCreationString+" protocol '"+protocol+"'" );
                a_params.put("command4", dstNatCreationString+" translation address '"+dstIp+"'" );
                if (srcPortRangeString != "") {
                	a_params.put("command5", dstNatCreationString+" source port '"+srcPortRangeString+"'" );
                
                }
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));

                return true;

            case DELETE:
                if (!manageDstNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // determine if we need to delete the ip from the interface as well...
             // TODO Do I need to do this? Build the parameters needed for vyos
                /*
                Map<String, String> c_params = new HashMap<String, String>();
                c_params.put("type", "config");
                c_params.put("action", "get");
                c_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[destination/member[text()='" + publicIp + "']]");
                
                String c_response = request(VyosRouterMethod.SHELL, c_params);            
                
             
                String count = "";
                NodeList response_body;
                Document doc = getDocument(c_response);
                XPath xpath = XPathFactory.newInstance().newXPath();
                try {
                    XPathExpression expr = xpath.compile("/response[@status='success']/result");
                    response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    throw new ExecutionException(e.getCause().getMessage());
                }
                if (response_body.getLength() > 0 && response_body.item(0).getAttributes().getLength() > 0) {
                    count = response_body.item(0).getAttributes().getNamedItem("count").getTextContent();
                }
                */

                // delete the dst nat rule
             // TODO Build the parameters needed for vyos
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "write");
                d_sub_params.put("command", "delete nat destination rule " +dstNatName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
                

             // TODO Do I need to do this? Build the parameters needed for vyos
                /*
                if (!count.equals("") && Integer.parseInt(count) == 1) { // this dst nat rule is the last, so remove the ip...
                    // delete IP from sub-interface...
                	
                    
                    Map<String, String> d_sub_params = new HashMap<String, String>();
                    d_sub_params.put("type", "config");
                    d_sub_params.put("action", "delete");
                    d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                        "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip/entry[@name='" + publicIp + "/32']");
                    
                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
                }
				*/
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
        String stcNatName = Long.toString(rule.getId());

        String publicInterfaceName=_publicInterface;
        
        //Static NAT in Vyos is accomplished by creating both source and destination nat rules for the given public and private ip addresses
        String srcNatCreationString="set nat source rule "+stcNatName;
        String dstNatCreationString="set nat destination rule "+stcNatName;
        

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show configuration commands");                
                String response = request(VyosRouterMethod.SHELL, params);
                if (response.contains(dstNatCreationString) && response.contains(srcNatCreationString)) {                 
                	s_logger.debug("Destination NAT exists: " + stcNatName );
                	return true;
                }
                else {
                	s_logger.debug("Destination NAT does not exist: " + stcNatName+" response: " + response);
                	return false;
                }

            case ADD:
                if (manageStcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // add public IP to the sub-interface
             // TODO Do I need to do this? Build the parameters needed for vyos
                /*
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip");
                a_sub_params.put("element", "<entry name='" + publicIp + "/32'/>");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_sub_params));
                 */
                
                // add the static nat rule for the public IP
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "writeMultiple");
                a_params.put("command1", srcNatCreationString+" source address '"+privateIp+"'" );
                a_params.put("command2", srcNatCreationString+" outbound-interface '"+publicInterfaceName+"'" );
                a_params.put("command3", srcNatCreationString+" translation address '"+publicIp+"'" );
                a_params.put("command4", dstNatCreationString+" inbound-interface '"+publicInterfaceName+"'" );
                a_params.put("command5", dstNatCreationString+" destination address '"+publicIp+"'" );             
                a_params.put("command6", dstNatCreationString+" translation address '"+privateIp+"'" );
                
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));


                return true;

            case DELETE:
                if (!manageStcNatRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // delete the static nat rule
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "writeMultiple");
                d_sub_params.put("command", "delete nat destination rule " +stcNatName);
                d_sub_params.put("command", "delete nat source rule " +stcNatName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
                

                // delete IP from sub-interface...
             // TODO Do I need to do this? Build the parameters needed for vyos
                /*
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "config");
                d_sub_params.put("action", "delete");
                d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip/entry[@name='" + publicIp + "/32']");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));
                */
                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Firewall rule implementation
     */
   
    

    public boolean manageFirewallRule(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, FirewallRuleTO rule) throws ExecutionException {
    	//To minimize the chance of overlap prefix the ruleId with a 9. This is necessary since we have to input some general rules that must have 
    	//a numeric value.
        String ruleName="9"+Long.toString(rule.getId());
        String firewallRuleSetName=rule.getSrcVlanTag()+"_"+rule.getTrafficType();
        
        // TODO This only works with a deny defaultEgressPolicy 
        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	Map<String, String> params = new HashMap<String, String>();
            	params.put("type", "read");
            	params.put("command","show firewall name "+firewallRuleSetName+" rule "+ruleName);                
                String response = request(VyosRouterMethod.SHELL, params);
                if (!response.contains("Invalid rule")) {                 
                	s_logger.debug("Firewall Rule exists: " + firewallRuleSetName+"."+ruleName );
                	return true;
                }
                else {
                	s_logger.debug("Firewall Rule does not exist: " + firewallRuleSetName+"."+ruleName+" response: " + response);
                	return false;
                }

            case ADD:
                if (manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }
                
                String protocol=rule.getProtocol();
                if (protocol.equals(Protocol.ICMP.toString())) {                	
                	Map<String, String> a_params = new HashMap<String, String>();
                    a_params.put("type", "writeMultiple");
                    a_params.put("command1", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" action 'accept'");
                    a_params.put("command2", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" icmp type '"+rule.getIcmpType()+"'");
                    a_params.put("command3", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" icmp code '"+rule.getIcmpCode()+"'");
                    a_params.put("command4", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" protocol 'icmp'");
                    a_params.put("command5", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" state new 'enable'");
                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));
                	
                }
                else if (protocol.equals(Protocol.TCP.toString()) || protocol.equals(Protocol.UDP.toString())) {   
                	Map<String, String> a_params = new HashMap<String, String>();
                    a_params.put("type", "writeMultiple");
                    a_params.put("command1", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" action 'accept'");
                    a_params.put("command2", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" protocol '"+protocol+"'");
                    a_params.put("command3", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" state new 'enable'");
                    
                    // Build source port range. In Vyos these are called destination ports.
                	int[] srcPortRange = rule.getSrcPortRange();
                    String srcPortRangeString="";
                    if (srcPortRange != null) {                    
                        if (srcPortRange.length == 1 || srcPortRange[0] == srcPortRange[1]) {
                        	srcPortRangeString = String.valueOf(srcPortRange[0]);
                        } else {
                        	srcPortRangeString = String.valueOf(srcPortRange[0]) + "-" + String.valueOf(srcPortRange[1]);
                        }
                        a_params.put("command4", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" destination port '"+srcPortRangeString+"'");
                    } 
                    
                    // Build allowed cidr range
                    List<String> ruleSrcCidrList = rule.getSourceCidrList();
                    String srcCidr="";
                    if (ruleSrcCidrList.size() == 1) { // a cidr was entered, modify as needed...                    		
                        srcCidr=ruleSrcCidrList.get(0).trim();
                        if (srcCidr.equals("0.0.0.0/0")) { // allow any
                        	srcCidr = getPrivateSubnet(rule.getSrcVlanTag());                        	 
                        }
                        if (srcCidr != "") {
                        	//Handle egress rule
                        	if (rule.getTrafficType() == FirewallRule.TrafficType.Egress){
                        		a_params.put("command5", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" source address '"+srcCidr+"'");
                        	} else if (rule.getTrafficType() == FirewallRule.TrafficType.Ingress){
                        		a_params.put("command5", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" source address '"+srcCidr+"'");
                        	} else {
                        		throw new ExecutionException("Firewall Rule Traffic Type not supported: "+rule.getTrafficType().toString());
                        	}
                        }
                    } else if (ruleSrcCidrList.size() > 1){ // TODO Currently cannot handle firewall rules with multiple cidrs. Use address groups to implement this
                       	throw new ExecutionException("Firewall Rules with multiple cidrs is not supported cidr list:"+ruleSrcCidrList.toString());                    
                    } 
                    
                    // Build destination address
                    if (rule.getSrcIp() != null){
                    	if (rule.getTrafficType() == FirewallRule.TrafficType.Egress){
                    		//From my understanding this should be an error state. srcIP should always be null for Egress rules.
                    		throw new ExecutionException("Egress Firewall Rule has a non null value in srcIp: "+rule.getSrcIp());
                    	}
                    	else{
                    		a_params.put("command6", "set firewall name"+firewallRuleSetName+" rule "+ruleName+" destination address '"+rule.getSrcIp()+"'");
                    	}
                    }
                	
                    
                    cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));
                }
                else {
                	throw new ExecutionException("The protocol is not supported: "+protocol);
                	//return false;
                }

                return true;

            case DELETE:
                if (!manageFirewallRule(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

             // delete the firewall nat rule
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "write");
                d_sub_params.put("command", "delete firewall name"+firewallRuleSetName+" rule "+ruleName);
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }
    
 // TODO I do not think this is needed since I handle deleting firewall rules when I delete an interface.
    /*
 // remove orphaned rules if they exist...
    public void removeOrphanedFirewallRules(ArrayList<IVyosRouterCommand> cmdList, long vlan) throws ExecutionException {
    	 	
        
        	// TODO Build the parameters needed for vyos
            
            Map<String, String> d_params = new HashMap<String, String>();
            d_params.put("type", "config");
            d_params.put("action", "delete");
            d_params.put("xpath",
                "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[contains(@name, 'policy') and contains(@name, '" + Long.toString(vlan) +
                    "')]");
            
            cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_params));
        
    }
    */
    /*
     * Usage
     */

    /*
     * Helper config functions
     */

    // TODO I do not think I need this for VYOS?
    /*
    // ensure guest network isolation
    private String genNetworkIsolationName(long privateVlanTag) {
        return "isolate_" + Long.toString(privateVlanTag);
    }

    public boolean manageNetworkIsolation(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, long privateVlanTag, String privateSubnet, String privateGateway)
        throws ExecutionException {
        String ruleName = genNetworkIsolationName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	// TODO Build the parameters needed for vyos
                
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                
                String response = request(VyosRouterMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Firewall policy exists: " + ruleName + ", " + result);
                return result;

            case ADD:
                if (manageNetworkIsolation(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateSubnet, privateGateway)) {
                    return true;
                }

                String xml = "";
                xml += "<from><member>" + _privateZone + "</member></from>";
                xml += "<to><member>" + _privateZone + "</member></to>";
                xml += "<source><member>" + privateSubnet + "</member></source>";
                xml += "<destination><member>" + privateGateway + "</member></destination>";
                xml += "<application><member>any</member></application>";
                xml += "<service><member>any</member></service>";
                xml += "<action>deny</action>";
                xml += "<negate-source>no</negate-source>";
                xml += "<negate-destination>yes</negate-destination>";

             // TODO Build the parameters needed for vyos
                
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                a_params.put("element", xml);
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));

                return true;

            case DELETE:
                if (!manageNetworkIsolation(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, privateVlanTag, privateSubnet, privateGateway)) {
                    return true;
                }

             // TODO Build the parameters needed for vyos
                
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.POST, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }
    */
    // TODO I think this is not needed for vyos
 // make the interfaces pingable for basic network troubleshooting
    /*
    public boolean managePingProfile(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim) throws ExecutionException {
        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	// TODO Build the parameters needed for vyos
                
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                
                String response = request(VyosRouterMethod.SHELL, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Management profile exists: " + _pingManagementProfile + ", " + result);
                return result;

            case ADD:
                if (managePingProfile(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS)) {
                    return true;
                }

                // add ping profile...
             // TODO Build the parameters needed for vyos
                
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                a_params.put("element", "<ping>yes</ping>");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));

                return true;

            case DELETE:
                if (!managePingProfile(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS)) {
                    return true;
                }

                // delete ping profile...
                Map<String, String> d_params = new HashMap<String, String>();
             // TODO Build the parameters needed for vyos
                
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }
    */
    
 // TODO I think this is not needed for vyos
    /*
    private String genServiceName(String protocol, String dstPorts, String srcPorts) {
        String name;
        if (srcPorts == null) {
            name = "cs_" + protocol.toLowerCase() + "_" + dstPorts.replace(',', '.');
        } else {
            name = "cs_" + protocol.toLowerCase() + "_" + dstPorts.replace(',', '.') + "_" + srcPorts.replace(',', '.');
        }
        return name;
    }

    public boolean manageService(ArrayList<IVyosRouterCommand> cmdList, VyosRouterPrimative prim, String protocol, String dstPorts, String srcPorts)
        throws ExecutionException {
        String serviceName = genServiceName(protocol, dstPorts, srcPorts);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
            	 // TODO Build the parameters needed for vyos
                
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                
                String response = request(VyosRouterMethod.SHELL, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Service exists: " + serviceName + ", " + result);
                return result;

            case ADD:
                if (manageService(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, protocol, dstPorts, srcPorts)) {
                    return true;
                }

                String dstPortXML = "<port>" + dstPorts + "</port>";
                String srcPortXML = "";
                if (srcPorts != null) {
                    srcPortXML = "<source-port>" + srcPorts + "</source-port>";
                }

                // add ping profile...
                // TODO Build the parameters needed for vyos
                
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                a_params.put("element", "<protocol><" + protocol.toLowerCase() + ">" + dstPortXML + srcPortXML + "</" + protocol.toLowerCase() + "></protocol>");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.SHELL, a_params));

                return true;

            case DELETE:
                if (!manageService(cmdList, VyosRouterPrimative.CHECK_IF_EXISTS, protocol, dstPorts, srcPorts)) {
                    return true;
                }

                // delete ping profile...
                // TODO Build the parameters needed for vyos
                
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                
                cmdList.add(new DefaultVyosRouterCommand(VyosRouterMethod.GET, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }
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
        Map<String, String> params = new HashMap<String, String>();
    	params.put("type", "read");
    	params.put("command","ip addr show "+interfaceName+"."+vlan+" | grep inet | grep "+interfaceName+"."+vlan);                
        String response = request(VyosRouterMethod.SHELL, params);
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
    protected String request(VyosRouterMethod method, Map<String, String> params) throws ExecutionException {
        if (method != VyosRouterMethod.SHELL && method != VyosRouterMethod.HTTPSTUB) {
            throw new ExecutionException("Invalid method used to access the Vyos Router API.");
        }

        String responseBody = "";
        String debug_msg = "Vyos Router Request\n";

        // SHELL method...
        // Params has two keys: type and command.
        // Type is used to determine whether to enter configuration mode.
        if (method == VyosRouterMethod.SHELL) {
            
            for (String key : params.keySet()) {
            	debug_msg = debug_msg + key + " --> " + params.get(key);
            }         

            
            
        }

        // a STUB method...
        // TODO This cannot be implemented until the Vyos team releases a production version of VyConf
        if (method == VyosRouterMethod.HTTPSTUB) {
        	throw new ExecutionException("HTTPSTUB method called but has not been implemented yet. Use SHELL method for all requests");
        }
                
        debug_msg = debug_msg + "\n" + responseBody.replace("\"", "\\\"") + "\n\n"; // test cases
        s_logger.debug(debug_msg); // this can be commented if we don't want to show each request in the log.

        return responseBody;
    }
    
    /* Used for requests that require polling to get a result (eg: commit) */
    private String requestWithPolling(VyosRouterMethod method, Map<String, String> params) throws ExecutionException {
    	// TODO Do I need this?
    	 s_logger.debug("Something called the requestWithPolling() method. This has not been implemented!");
    	return "requestWithPolling() method not implemented";
    }
    
    private synchronized boolean requestWithCommit(ArrayList<IVyosRouterCommand> commandList) throws ExecutionException {
    	// TODO Do I need this?   	 
    	s_logger.debug("Something called the requestWithCommit() method. This has not been implemented!"); 
    	return false;   
    }
    
    /* A default response handler to validate that the request was successful. */
    public boolean validResponse(String response) throws ExecutionException {
    	// TODO For now response validation is handled in the request() method. Once requests are using http api calls this will need to be implemented.   	 
    	s_logger.debug("Something called the validResponse() method. This has not been implemented!"); 
    	return false;   
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
    }

    /* Command Abstract */
    private abstract class AbstractVyosRouterCommand implements IVyosRouterCommand {
    	VyosRouterMethod method;
        Map<String, String> params;

        public AbstractVyosRouterCommand() {
        }

        public AbstractVyosRouterCommand(VyosRouterMethod method, Map<String, String> params) {
            this.method = method;
            this.params = params;
        }

        @Override
        public boolean execute() throws ExecutionException {
            String response = request(method, params);
            // TODO Implement validResponse() method once the vyconf https api is implemented.
            //return validResponse(response);
            return true;
        }
    }

    /* Implement the default functionality */
    private class DefaultVyosRouterCommand extends AbstractVyosRouterCommand {
        public DefaultVyosRouterCommand(VyosRouterMethod method, Map<String, String> params) {
            super(method, params);
        }
    }
    
    /*
     * Misc
     */

    private String genIpIdentifier(String ip) {
        return ip.replace('.', '-').replace('/', '-');
    }

    private String parsePublicVlanTag(String uri) {
        return uri.replace("vlan://", "");
    }

    private Protocol getProtocol(String protocolName) throws ExecutionException {
        protocolName = protocolName.toLowerCase();

        try {
            return Protocol.valueOf(protocolName);
        } catch (Exception e) {
            throw new ExecutionException("Invalid protocol: " + protocolName);
        }
    }
    
    
    
    
    
    
	@Override
	public void setName(String name) {
		_name = name;		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}


	
	
	
	

	

}
