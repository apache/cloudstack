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
package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.DestinationNatRule;
import com.cloud.network.nicira.L3GatewayAttachment;
import com.cloud.network.nicira.LogicalRouterConfig;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.Match;
import com.cloud.network.nicira.NatRule;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpList;
import com.cloud.network.nicira.NiciraNvpTag;
import com.cloud.network.nicira.PatchAttachment;
import com.cloud.network.nicira.RouterNextHop;
import com.cloud.network.nicira.SingleDefaultRouteImplictRoutingConfig;
import com.cloud.network.nicira.SourceNatRule;
import com.cloud.network.nicira.TransportZoneBinding;
import com.cloud.network.nicira.VifAttachment;
import com.cloud.resource.ServerResource;

public class NiciraNvpResource implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(NiciraNvpResource.class);
    
    private String _name;
    private String _guid;
    private String _zoneId;
    private int _numRetries;
    
    private NiciraNvpApi _niciraNvpApi;
    
    protected NiciraNvpApi createNiciraNvpApi() {
    	return new NiciraNvpApi();
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
    	
        _name = (String) params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }
        
        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }
        
        _zoneId = (String) params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }
        
        _numRetries = 2;

        String ip = (String) params.get("ip");
        if (ip == null) {
            throw new ConfigurationException("Unable to find IP");
        }
        
        String adminuser = (String) params.get("adminuser");
        if (adminuser == null) {
            throw new ConfigurationException("Unable to find admin username");
        }
        
        String adminpass = (String) params.get("adminpass");
        if (adminpass == null) {
            throw new ConfigurationException("Unable to find admin password");
        }               
        
        _niciraNvpApi = createNiciraNvpApi();
        _niciraNvpApi.setControllerAddress(ip);
        _niciraNvpApi.setAdminCredentials(adminuser,adminpass);

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
        // Think up a better name for this Type?
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupNiciraNvpCommand sc = new StartupNiciraNvpCommand();
        sc.setGuid(_guid);
        sc.setName(_name);
        sc.setDataCenter(_zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion(NiciraNvpResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] { sc };
    }

	@Override
	public PingCommand getCurrentStatus(long id) {
        try {
            ControlClusterStatus ccs = _niciraNvpApi.getControlClusterStatus();
            if (!"stable".equals(ccs.getClusterStatus())) {
            	s_logger.error("ControlCluster state is not stable: "
            			+ ccs.getClusterStatus());
            	return null;
            }
        } catch (NiciraNvpApiException e) {
        	s_logger.error("getControlClusterStatus failed", e);
        	return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
	}

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    public Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        }
        else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand)cmd);
        }
        else if (cmd instanceof CreateLogicalSwitchCommand) {
            return executeRequest((CreateLogicalSwitchCommand)cmd, numRetries);
        }
        else if (cmd instanceof DeleteLogicalSwitchCommand) {
            return executeRequest((DeleteLogicalSwitchCommand) cmd, numRetries);
        }
        else if (cmd instanceof CreateLogicalSwitchPortCommand) {
            return executeRequest((CreateLogicalSwitchPortCommand) cmd, numRetries);
        }
        else if (cmd instanceof DeleteLogicalSwitchPortCommand) {
            return executeRequest((DeleteLogicalSwitchPortCommand) cmd, numRetries);
        }
        else if (cmd instanceof UpdateLogicalSwitchPortCommand) {
        	return executeRequest((UpdateLogicalSwitchPortCommand) cmd, numRetries);
        }
        else if (cmd instanceof FindLogicalSwitchPortCommand) {
        	return executeRequest((FindLogicalSwitchPortCommand) cmd, numRetries);
        }
        else if (cmd instanceof CreateLogicalRouterCommand) {
        	return executeRequest((CreateLogicalRouterCommand) cmd, numRetries);
        }
        else if (cmd instanceof DeleteLogicalRouterCommand) {
        	return executeRequest((DeleteLogicalRouterCommand) cmd, numRetries);
        }
        else if (cmd instanceof ConfigureStaticNatRulesOnLogicalRouterCommand) {
        	return executeRequest((ConfigureStaticNatRulesOnLogicalRouterCommand) cmd, numRetries);
        }
        else if (cmd instanceof ConfigurePortForwardingRulesOnLogicalRouterCommand) {
        	return executeRequest((ConfigurePortForwardingRulesOnLogicalRouterCommand) cmd, numRetries);
        }       
        else if (cmd instanceof ConfigurePublicIpsOnLogicalRouterCommand) {
        	return executeRequest((ConfigurePublicIpsOnLogicalRouterCommand) cmd, numRetries);
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
    
    private Answer executeRequest(CreateLogicalSwitchCommand cmd, int numRetries) {
        LogicalSwitch logicalSwitch = new LogicalSwitch();
        logicalSwitch.setDisplay_name(truncate("lswitch-" + cmd.getName(), 40));
        logicalSwitch.setPort_isolation_enabled(false);

        // Set transport binding
        List<TransportZoneBinding> ltzb = new ArrayList<TransportZoneBinding>();
        ltzb.add(new TransportZoneBinding(cmd.getTransportUuid(), cmd.getTransportType()));
        logicalSwitch.setTransport_zones(ltzb);

        // Tags set to scope cs_account and account name
        List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));
        logicalSwitch.setTags(tags);
        
        try {
            logicalSwitch = _niciraNvpApi.createLogicalSwitch(logicalSwitch);
            return new CreateLogicalSwitchAnswer(cmd, true, "Logicalswitch " + logicalSwitch.getUuid() + " created", logicalSwitch.getUuid());
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new CreateLogicalSwitchAnswer(cmd, e);
        	}
        }
        
    }
    
    private Answer executeRequest(DeleteLogicalSwitchCommand cmd, int numRetries) {
        try {
            _niciraNvpApi.deleteLogicalSwitch(cmd.getLogicalSwitchUuid());
            return new DeleteLogicalSwitchAnswer(cmd, true, "Logicalswitch " + cmd.getLogicalSwitchUuid() + " deleted");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new DeleteLogicalSwitchAnswer(cmd, e);
        	}
        }
    }
    
    private Answer executeRequest(CreateLogicalSwitchPortCommand cmd, int numRetries) {
        String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        String attachmentUuid = cmd.getAttachmentUuid();
        
        try {
            // Tags set to scope cs_account and account name
            List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));

            LogicalSwitchPort logicalSwitchPort = new LogicalSwitchPort(attachmentUuid, tags, true);
            LogicalSwitchPort newPort = _niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, logicalSwitchPort);
            try {
            	_niciraNvpApi.modifyLogicalSwitchPortAttachment(cmd.getLogicalSwitchUuid(), newPort.getUuid(), new VifAttachment(attachmentUuid));
            } catch (NiciraNvpApiException ex) {
            	s_logger.warn("modifyLogicalSwitchPort failed after switchport was created, removing switchport");
            	_niciraNvpApi.deleteLogicalSwitchPort(cmd.getLogicalSwitchUuid(), newPort.getUuid());
            	throw (ex); // Rethrow the original exception
            }
            return new CreateLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + newPort.getUuid() + " created", newPort.getUuid());
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new CreateLogicalSwitchPortAnswer(cmd, e);
        	}
        }
        
    }
    
    private Answer executeRequest(DeleteLogicalSwitchPortCommand cmd, int numRetries) {
        try {
            _niciraNvpApi.deleteLogicalSwitchPort(cmd.getLogicalSwitchUuid(), cmd.getLogicalSwitchPortUuid());
            return new DeleteLogicalSwitchPortAnswer(cmd, true, "Logical switch port " + cmd.getLogicalSwitchPortUuid() + " deleted");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new DeleteLogicalSwitchPortAnswer(cmd, e);
        	}
        }
    }

    private Answer executeRequest(UpdateLogicalSwitchPortCommand cmd, int numRetries) {
        String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        String logicalSwitchPortUuid = cmd.getLogicalSwitchPortUuid();
        String attachmentUuid = cmd.getAttachmentUuid();
        
        try {
            // Tags set to scope cs_account and account name
            List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
            tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));

            _niciraNvpApi.modifyLogicalSwitchPortAttachment(logicalSwitchUuid, logicalSwitchPortUuid, new VifAttachment(attachmentUuid));
            return new UpdateLogicalSwitchPortAnswer(cmd, true, "Attachment for  " + logicalSwitchPortUuid + " updated", logicalSwitchPortUuid);
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new UpdateLogicalSwitchPortAnswer(cmd, e);
        	}
        }
    	
    }
    
    private Answer executeRequest(FindLogicalSwitchPortCommand cmd, int numRetries) {
    	String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
        String logicalSwitchPortUuid = cmd.getLogicalSwitchPortUuid();
        
        try {
        	NiciraNvpList<LogicalSwitchPort> ports = _niciraNvpApi.findLogicalSwitchPortsByUuid(logicalSwitchUuid, logicalSwitchPortUuid);
        	if (ports.getResultCount() == 0) {
        		return new FindLogicalSwitchPortAnswer(cmd, false, "Logical switchport " + logicalSwitchPortUuid + " not found", null);
        	}
        	else {
        		return new FindLogicalSwitchPortAnswer(cmd, true, "Logical switchport " + logicalSwitchPortUuid + " found", logicalSwitchPortUuid);
        	}
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new FindLogicalSwitchPortAnswer(cmd, e);
        	}
        }    	
    }
    
    private Answer executeRequest(CreateLogicalRouterCommand cmd, int numRetries) {
    	String routerName = cmd.getName();
    	String gatewayServiceUuid = cmd.getGatewayServiceUuid();
    	String logicalSwitchUuid = cmd.getLogicalSwitchUuid();
    	
        List<NiciraNvpTag> tags = new ArrayList<NiciraNvpTag>();
        tags.add(new NiciraNvpTag("cs_account",cmd.getOwnerName()));
        
        String publicNetworkNextHopIp = cmd.getPublicNextHop();
        String publicNetworkIpAddress = cmd.getPublicIpCidr();
        String internalNetworkAddress = cmd.getInternalIpCidr();
        
        s_logger.debug("Creating a logical router with external ip " 
        		+ publicNetworkIpAddress + " and internal ip " + internalNetworkAddress
        		+ "on gateway service " + gatewayServiceUuid);
        
        try {
        	// Create the Router
        	LogicalRouterConfig lrc = new LogicalRouterConfig();
        	lrc.setDisplayName(truncate(routerName, 40));
        	lrc.setTags(tags);
        	lrc.setRoutingConfig(new SingleDefaultRouteImplictRoutingConfig(
        			new RouterNextHop(publicNetworkNextHopIp)));
        	lrc = _niciraNvpApi.createLogicalRouter(lrc);
        	
        	// store the switchport for rollback
        	LogicalSwitchPort lsp = null;
        	
        	try {
	        	// Create the outside port for the router
	        	LogicalRouterPort lrpo = new LogicalRouterPort();
	        	lrpo.setAdminStatusEnabled(true);
	        	lrpo.setDisplayName(truncate(routerName + "-outside-port", 40));
	        	lrpo.setTags(tags);
	        	List<String> outsideIpAddresses = new ArrayList<String>();
	        	outsideIpAddresses.add(publicNetworkIpAddress);
	        	lrpo.setIpAddresses(outsideIpAddresses);
	        	lrpo = _niciraNvpApi.createLogicalRouterPort(lrc.getUuid(),lrpo);
	        	
	        	// Attach the outside port to the gateway service on the correct VLAN
	        	L3GatewayAttachment attachment = new L3GatewayAttachment(gatewayServiceUuid);
	        	if (cmd.getVlanId() != 0) {
	        		attachment.setVlanId(cmd.getVlanId());
	        	}
	        	_niciraNvpApi.modifyLogicalRouterPortAttachment(lrc.getUuid(), lrpo.getUuid(), attachment);
	        	
	        	// Create the inside port for the router
	        	LogicalRouterPort lrpi = new LogicalRouterPort();
	        	lrpi.setAdminStatusEnabled(true);
	        	lrpi.setDisplayName(truncate(routerName + "-inside-port", 40));
	        	lrpi.setTags(tags);
	        	List<String> insideIpAddresses = new ArrayList<String>();
	        	insideIpAddresses.add(internalNetworkAddress);
	        	lrpi.setIpAddresses(insideIpAddresses);
	        	lrpi = _niciraNvpApi.createLogicalRouterPort(lrc.getUuid(),lrpi);
	        	
	        	// Create the inside port on the lswitch
	            lsp = new LogicalSwitchPort(truncate(routerName + "-inside-port", 40), tags, true);
	            lsp = _niciraNvpApi.createLogicalSwitchPort(logicalSwitchUuid, lsp);
	       	
	        	// Attach the inside router port to the lswitch port with a PatchAttachment
	            _niciraNvpApi.modifyLogicalRouterPortAttachment(lrc.getUuid(), lrpi.getUuid(), 
	            		new PatchAttachment(lsp.getUuid()));
	        	
	        	// Attach the inside lswitch port to the router with a PatchAttachment
	            _niciraNvpApi.modifyLogicalSwitchPortAttachment(logicalSwitchUuid, lsp.getUuid(), 
	            		new PatchAttachment(lrpi.getUuid()));
	            
	            // Setup the source nat rule
	            SourceNatRule snr = new SourceNatRule();
	            snr.setToSourceIpAddressMin(publicNetworkIpAddress.split("/")[0]);
	            snr.setToSourceIpAddressMax(publicNetworkIpAddress.split("/")[0]);
	            Match match = new Match();
	            match.setSourceIpAddresses(internalNetworkAddress);
	            snr.setMatch(match);
	            snr.setOrder(200); 
	            _niciraNvpApi.createLogicalRouterNatRule(lrc.getUuid(), snr);
        	} catch (NiciraNvpApiException e) {
        		// We need to destroy the router if we already created it
        		// this will also take care of any router ports and rules
        		try {
        			_niciraNvpApi.deleteLogicalRouter(lrc.getUuid());
        			if (lsp != null) {
        				_niciraNvpApi.deleteLogicalSwitchPort(logicalSwitchUuid, lsp.getUuid());
        			}
        		} catch (NiciraNvpApiException ex) {}
        		
        		throw e;
        	}
            
            return new CreateLogicalRouterAnswer(cmd, true, "Logical Router created (uuid " + lrc.getUuid() + ")", lrc.getUuid());    	
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new CreateLogicalRouterAnswer(cmd, e);
        	}
        }
    }
    
    private Answer executeRequest(DeleteLogicalRouterCommand cmd, int numRetries) {
    	try {
    		_niciraNvpApi.deleteLogicalRouter(cmd.getLogicalRouterUuid());
    		return new DeleteLogicalRouterAnswer(cmd, true, "Logical Router deleted (uuid " + cmd.getLogicalRouterUuid() + ")");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new DeleteLogicalRouterAnswer(cmd, e);
        	}
        }
    }
    
    private Answer executeRequest(ConfigurePublicIpsOnLogicalRouterCommand cmd, int numRetries) {
    	try {
    		NiciraNvpList<LogicalRouterPort> ports = _niciraNvpApi.findLogicalRouterPortByGatewayServiceUuid(cmd.getLogicalRouterUuid(), cmd.getL3GatewayServiceUuid());
    		if (ports.getResultCount() != 1) {
    			return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, false, "No logical router ports found, unable to set ip addresses");
    		}
    		LogicalRouterPort lrp = ports.getResults().get(0);
    		lrp.setIpAddresses(cmd.getPublicCidrs());
    		_niciraNvpApi.modifyLogicalRouterPort(cmd.getLogicalRouterUuid(), lrp);
    		
    		return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, true, "Configured " + cmd.getPublicCidrs().size() + 
    				" ip addresses on logical router uuid " + cmd.getLogicalRouterUuid());
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new ConfigurePublicIpsOnLogicalRouterAnswer(cmd, e);
        	}
        }
    	
    }
    
    private Answer executeRequest(ConfigureStaticNatRulesOnLogicalRouterCommand cmd, int numRetries) {
    	try {
    		NiciraNvpList<NatRule> existingRules = _niciraNvpApi.findNatRulesByLogicalRouterUuid(cmd.getLogicalRouterUuid());
    		// Rules of the game (also known as assumptions-that-will-make-stuff-break-later-on)
    		// A SourceNat rule with a match other than a /32 cidr is assumed to be the "main" SourceNat rule
    		// Any other SourceNat rule should have a corresponding DestinationNat rule
    		
    		for (StaticNatRuleTO rule : cmd.getRules()) {
    			
    			NatRule[] rulepair = generateStaticNatRulePair(rule.getDstIp(), rule.getSrcIp());
    							
				NatRule incoming = null;
				NatRule outgoing = null;

				for (NatRule storedRule : existingRules.getResults()) {					
    				if (storedRule.equalsIgnoreUuid(rulepair[1])) {
						// The outgoing rule exists
    					outgoing = storedRule;
    					s_logger.debug("Found matching outgoing rule " + outgoing.getUuid());
    					if (incoming != null) {
    						break;
    					}
        			}    					
    				else if (storedRule.equalsIgnoreUuid(rulepair[0])) {
    					// The incoming rule exists
    					incoming = storedRule;
    					s_logger.debug("Found matching incoming rule " + incoming.getUuid());
    					if (outgoing != null) {
    						break;
    					}
    				}
    			}
				if (incoming != null && outgoing != null) {
					if (rule.revoked()) {
						s_logger.debug("Deleting incoming rule " + incoming.getUuid());
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), incoming.getUuid());
						
						s_logger.debug("Deleting outgoing rule " + outgoing.getUuid());
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), outgoing.getUuid());
					}
				}
				else {
					if (rule.revoked()) {
						s_logger.warn("Tried deleting a rule that does not exist, " + 
								rule.getSrcIp() + " -> " + rule.getDstIp());
						break;
					}
					
					rulepair[0] = _niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0]);
					s_logger.debug("Created " + natRuleToString(rulepair[0]));
					
					try {
						rulepair[1] = _niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[1]);
						s_logger.debug("Created " + natRuleToString(rulepair[1]));
					} catch (NiciraNvpApiException ex) {
						s_logger.debug("Failed to create SourceNatRule, rolling back DestinationNatRule");
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0].getUuid());
						throw ex; // Rethrow original exception
					}
					
				}
    		}
    		return new ConfigureStaticNatRulesOnLogicalRouterAnswer(cmd, true, cmd.getRules().size() +" StaticNat rules applied");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new ConfigureStaticNatRulesOnLogicalRouterAnswer(cmd, e);
        	}
        }
    }

    private Answer executeRequest(ConfigurePortForwardingRulesOnLogicalRouterCommand cmd, int numRetries) {
    	try {
    		NiciraNvpList<NatRule> existingRules = _niciraNvpApi.findNatRulesByLogicalRouterUuid(cmd.getLogicalRouterUuid());
    		// Rules of the game (also known as assumptions-that-will-make-stuff-break-later-on)
    		// A SourceNat rule with a match other than a /32 cidr is assumed to be the "main" SourceNat rule
    		// Any other SourceNat rule should have a corresponding DestinationNat rule
    		
    		for (PortForwardingRuleTO rule : cmd.getRules()) {
    			if (rule.isAlreadyAdded() && !rule.revoked()) {
    				// Don't need to do anything
    				continue;
    			}
    			
    			if (rule.getDstPortRange()[0] != rule.getDstPortRange()[1] || 
    			        rule.getSrcPortRange()[0] != rule.getSrcPortRange()[1]    ) {
    				return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, false, "Nicira NVP doesn't support port ranges for port forwarding");
    			}
    			
    			NatRule[] rulepair = generatePortForwardingRulePair(rule.getDstIp(), rule.getDstPortRange(), rule.getSrcIp(), rule.getSrcPortRange(), rule.getProtocol());
				
				NatRule incoming = null;
				NatRule outgoing = null;

				for (NatRule storedRule : existingRules.getResults()) {
    				if (storedRule.equalsIgnoreUuid(rulepair[1])) {
						// The outgoing rule exists
    					outgoing = storedRule;
    					s_logger.debug("Found matching outgoing rule " + outgoing.getUuid());
    					if (incoming != null) {
    						break;
    					}
        			}    					
    				else if (storedRule.equalsIgnoreUuid(rulepair[0])) {
    					// The incoming rule exists
    					incoming = storedRule;
    					s_logger.debug("Found matching incoming rule " + incoming.getUuid());
    					if (outgoing != null) {
    						break;
    					}
    				}
				}
				if (incoming != null && outgoing != null) {
					if (rule.revoked()) {
						s_logger.debug("Deleting incoming rule " + incoming.getUuid());
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), incoming.getUuid());
						
						s_logger.debug("Deleting outgoing rule " + outgoing.getUuid());
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), outgoing.getUuid());
					}
				}
				else {
					if (rule.revoked()) {
						s_logger.warn("Tried deleting a rule that does not exist, " + 
								rule.getSrcIp() + " -> " + rule.getDstIp());
						break;
					}
					
					rulepair[0] = _niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0]);
					s_logger.debug("Created " + natRuleToString(rulepair[0]));
					
					try {
						rulepair[1] = _niciraNvpApi.createLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[1]);
						s_logger.debug("Created " + natRuleToString(rulepair[1]));
					} catch (NiciraNvpApiException ex) {
						s_logger.warn("NiciraNvpApiException during create call, rolling back previous create");
						_niciraNvpApi.deleteLogicalRouterNatRule(cmd.getLogicalRouterUuid(), rulepair[0].getUuid());
						throw ex; // Rethrow the original exception
					}
					
				}
    		}
    		return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, true, cmd.getRules().size() +" PortForwarding rules applied");
        } catch (NiciraNvpApiException e) {
        	if (numRetries > 0) {
        		return retry(cmd, --numRetries);
        	} 
        	else {
        		return new ConfigurePortForwardingRulesOnLogicalRouterAnswer(cmd, e);
        	}
        }
    	
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
    
    private String natRuleToString(NatRule rule) {
    	
		StringBuilder natRuleStr = new StringBuilder();
		natRuleStr.append("Rule ");
		natRuleStr.append(rule.getUuid());
		natRuleStr.append(" (");
		natRuleStr.append(rule.getType());
		natRuleStr.append(") :");
		Match m = rule.getMatch();
		natRuleStr.append("match (");
		natRuleStr.append(m.getProtocol());
		natRuleStr.append(" ");
		natRuleStr.append(m.getSourceIpAddresses());
		natRuleStr.append(" [");
		natRuleStr.append(m.getSourcePort());
		natRuleStr.append(" ] -> ");
		natRuleStr.append(m.getDestinationIpAddresses());
		natRuleStr.append(" [");
		natRuleStr.append(m.getDestinationPort());
		natRuleStr.append(" ]) -->");
		if ("SourceNatRule".equals(rule.getType())) {
			natRuleStr.append(((SourceNatRule)rule).getToSourceIpAddressMin());
			natRuleStr.append("-");
			natRuleStr.append(((SourceNatRule)rule).getToSourceIpAddressMax());
			natRuleStr.append(" [");
			natRuleStr.append(((SourceNatRule)rule).getToSourcePort());
			natRuleStr.append(" ])");
		}
		else {
			natRuleStr.append(((DestinationNatRule)rule).getToDestinationIpAddress());
			natRuleStr.append(" [");
			natRuleStr.append(((DestinationNatRule)rule).getToDestinationPort());
			natRuleStr.append(" ])");
		}
		return natRuleStr.toString();
    }
    
    private String truncate(String string, int length) {
    	if (string.length() <= length) {
    		return string;
    	}
    	else {
    		return string.substring(0, length);
    	}
    }
    
    protected NatRule[] generateStaticNatRulePair(String insideIp, String outsideIp) {
    	NatRule[] rulepair = new NatRule[2];
    	rulepair[0] = new DestinationNatRule();
    	rulepair[0].setType("DestinationNatRule");
    	rulepair[0].setOrder(100);
    	rulepair[1] = new SourceNatRule();
    	rulepair[1].setType("SourceNatRule");
    	rulepair[1].setOrder(100);
    	
		Match m = new Match();
		m.setDestinationIpAddresses(outsideIp);
		rulepair[0].setMatch(m);
		((DestinationNatRule)rulepair[0]).setToDestinationIpAddress(insideIp);

		// create matching snat rule
		m = new Match();
		m.setSourceIpAddresses(insideIp);
		rulepair[1].setMatch(m);
		((SourceNatRule)rulepair[1]).setToSourceIpAddressMin(outsideIp);
		((SourceNatRule)rulepair[1]).setToSourceIpAddressMax(outsideIp);
    	
    	return rulepair;
    	
    }
    
    protected NatRule[] generatePortForwardingRulePair(String insideIp, int[] insidePorts, String outsideIp, int[] outsidePorts, String protocol) {
       	// Start with a basic static nat rule, then add port and protocol details
    	NatRule[] rulepair = generateStaticNatRulePair(insideIp, outsideIp);
    	
    	((DestinationNatRule)rulepair[0]).setToDestinationPort(insidePorts[0]);
    	rulepair[0].getMatch().setDestinationPort(outsidePorts[0]);
    	rulepair[0].setOrder(50);
    	rulepair[0].getMatch().setEthertype("IPv4");
		if ("tcp".equals(protocol)) {
			rulepair[0].getMatch().setProtocol(6);
		}
		else if ("udp".equals(protocol)) {
			rulepair[0].getMatch().setProtocol(17);
		}

    	((SourceNatRule)rulepair[1]).setToSourcePort(outsidePorts[0]);
    	rulepair[1].getMatch().setSourcePort(insidePorts[0]);
    	rulepair[1].setOrder(50);
    	rulepair[1].getMatch().setEthertype("IPv4");
		if ("tcp".equals(protocol)) {
			rulepair[1].getMatch().setProtocol(6);
		}
		else if ("udp".equals(protocol)) {
			rulepair[1].getMatch().setProtocol(17);
		}

		return rulepair;
   	
    }

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
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
