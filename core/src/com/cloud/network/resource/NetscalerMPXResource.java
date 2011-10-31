/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.network.resource;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

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
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.google.gson.Gson;

import com.citrix.netscaler.nitro.service.nitro_service;
import com.citrix.netscaler.nitro.resource.base.base_response;
import com.citrix.netscaler.nitro.exception.nitro_exception;
import com.citrix.netscaler.nitro.resource.config.ns.nsconfig;
import com.citrix.netscaler.nitro.resource.config.lb.lbvserver;
import com.citrix.netscaler.nitro.resource.config.basic.service;
import com.citrix.netscaler.nitro.resource.config.network.*;
import com.citrix.netscaler.nitro.resource.config.ns.*;
import com.citrix.netscaler.nitro.resource.config.basic.server_service_binding;
import com.citrix.netscaler.nitro.resource.stat.lb.lbvserver_stats;

import org.apache.axis.types.*;
import org.apache.log4j.Logger;

class NitroError {
    static final int NS_RESOURCE_EXISTS = 273;
    static final int NS_RESOURCE_NOT_EXISTS=258;
    static final int NS_NO_SERIVCE = 344;
}

public class NetscalerMPXResource implements ServerResource {

    // deployment configuration
    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _publicInterface;
    private String _privateInterface;
    private Integer _numRetries; 
    private String _guid;
    private boolean _inline;

    private static final Logger s_logger = Logger.getLogger(NetscalerMPXResource.class);
    protected Gson _gson;
    private String _objectNamePathSep = "-";

    nitro_service nsService ;
    Long timeout = new Long(100000);
    base_response apiCallResult;

    public NetscalerMPXResource () {
        _gson = GsonHelper.getGsonLogger();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String) params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            _zoneId = (String) params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _ip = (String) params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _username = (String) params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username");
            }

            _password = (String) params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password");
            }

            _publicInterface = (String) params.get("publicInterface");
            if (_publicInterface == null) {
                throw new ConfigurationException("Unable to find public interface");
            }
            
            _privateInterface = (String) params.get("privateInterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface");
            }
            
            _numRetries = NumbersUtil.parseInt((String) params.get("numRetries"), 1);

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }
            
            _inline = Boolean.parseBoolean((String) params.get("inline"));

            login();          
            enableNetScalerLoadBalancing();
                       
            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    private void login() throws ExecutionException {
        try {
            nsService = new nitro_service(_ip, "https");
            apiCallResult = nsService.login(_username, _password, timeout);
            if (apiCallResult.errorcode != 0) {
            	throw new ExecutionException ("Failed to log in to Netscaler device at " + _ip + " due to " + apiCallResult.message);
            }
        } catch (nitro_exception e) {
        	throw new ExecutionException("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        } catch (Exception e) {
        	throw new ExecutionException("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        }
    }

    private void enableNetScalerLoadBalancing() throws ExecutionException {
        try {
            String[] feature = new String[1];
            feature[0] = "LB";
            nsService.enable_features(feature);
        } catch (nitro_exception e) {
        	throw new ExecutionException("Enabling netscaler load balancing feature failed due to " + e.getMessage());
        } catch (Exception e) {
        	throw new ExecutionException("Enabling netscaler load balancing feature failed due to " + e.getMessage());
        }
    }

    @Override
    public StartupCommand[] initialize() {
        StartupExternalLoadBalancerCommand cmd = new StartupExternalLoadBalancerCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion("");
        cmd.setGuid(_guid);
        return new StartupCommand[]{cmd};
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }    
    
    private Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand) cmd, numRetries);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            return execute((LoadBalancerConfigCommand) cmd, numRetries);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    protected Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd, "Put host in maintaince");
    }

    private synchronized Answer execute(IpAssocCommand cmd, int numRetries) {
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        try {        
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {
                long guestVlanTag = Long.valueOf(ip.getVlanId());
                String vlanSelfIp = ip.getVlanGateway();
                String vlanNetmask = ip.getVlanNetmask();
                
                // Check and delete any existing guest VLAN with this tag, self IP, and netmask
                deleteGuestVlan(guestVlanTag, vlanSelfIp, vlanNetmask);
                
                if (ip.isAdd()) {
                    // Add a new guest VLAN and its subnet and bind it to private interface
                    addGuestVlanAndSubnet(guestVlanTag, vlanSelfIp, vlanNetmask);
                }
                
                saveConfiguration();
                results[i++] = ip.getPublicIp() + " - success";
            }
        } catch (ExecutionException e) {
            s_logger.error("Failed to execute IPAssocCommand due to " + e);                    
            
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }        
        
        return new IpAssocAnswer(cmd, results);
    }
    
    private synchronized Answer execute(LoadBalancerConfigCommand cmd, int numRetries) {
        try {
            LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();
            
            for (LoadBalancerTO loadBalancer : loadBalancers) {
                String srcIp = loadBalancer.getSrcIp();
                int srcPort = loadBalancer.getSrcPort();
                String lbProtocol = loadBalancer.getProtocol();
                String lbAlgorithm = loadBalancer.getAlgorithm();
                String nsVirtualServerName  = generateNSVirtualServerName(srcIp, srcPort, lbProtocol);
                
                boolean destinationsToAdd = false;
                for (DestinationTO destination : loadBalancer.getDestinations()) {
                    if (!destination.isRevoked()) {
                        destinationsToAdd = true;
                        break;
                    }
                }

                if (!loadBalancer.isRevoked() && destinationsToAdd) {

                    // create a load balancing virtual server
                    addLBVirtualServer(nsVirtualServerName, srcIp, srcPort, lbAlgorithm, lbProtocol);

                    for (DestinationTO destination : loadBalancer.getDestinations()) {
                        
                        String nsServerName = generateNSServerName(destination.getDestIp());
                        String nsServiceName = generateNSServiceName(destination.getDestIp(), destination.getDestPort());
                        
                        if (!destination.isRevoked()) {
                            // add a new destination to deployed load balancing rule

                            // add a new server
                            if (!nsServerExists(nsServerName)) {
                                com.citrix.netscaler.nitro.resource.config.basic.server nsServer = new com.citrix.netscaler.nitro.resource.config.basic.server();
                                nsServer.set_name(nsServerName);
                                nsServer.set_ipaddress(destination.getDestIp());
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.add(nsService, nsServer);
                                if ((apiCallResult.errorcode != 0) && (apiCallResult.errorcode != NitroError.NS_RESOURCE_EXISTS)) {
                                    throw new ExecutionException("Failed to add server " + destination.getDestIp() + " due to" + apiCallResult.message);
                                }
                            }

                            // create a new service using the server added
                            if (!nsServiceExists(nsServiceName)) {
                                com.citrix.netscaler.nitro.resource.config.basic.service newService = new com.citrix.netscaler.nitro.resource.config.basic.service();
                                newService.set_name(nsServiceName);
                                newService.set_port(destination.getDestPort());
                                newService.set_servername(nsServerName);
                                newService.set_state("ENABLED");
                                newService.set_servicetype(lbProtocol);
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.add(nsService, newService);
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to create service " + nsServiceName + " using server " + nsServerName + " due to" + apiCallResult.message);
                                }
                            }

                            //bind service to load balancing virtual server    
                            if (!nsServiceBindingExists(nsVirtualServerName, nsServiceName)) {
                                com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding svcBinding = new com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding();
                                svcBinding.set_name(nsVirtualServerName);
                                svcBinding.set_servicename(nsServiceName);
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.add(nsService, svcBinding);
                                
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to bind service: " + nsServiceName + " to the lb virtual server: " + nsVirtualServerName + " on Netscaler device");
                                }
                            }
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Successfully added LB destination: " + destination.getDestIp() + ":" + destination.getDestPort() + " to load balancer " + srcIp + ":" + srcPort);
                            }
                        } else {
                            // remove a destination from the deployed load balancing rule
                            com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(nsService, nsVirtualServerName);
                            if (serviceBindings != null) {
                                for (com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                                    if (nsServiceName.equalsIgnoreCase(binding.get_servicename())) {
                                        // delete the binding
                                        apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.delete(nsService, binding);
                                        if (apiCallResult.errorcode != 0) {
                                            throw new ExecutionException("Failed to delete the binding between the virtual server: " + nsVirtualServerName + " and service:" + nsServiceName + " due to" + apiCallResult.message);
                                        }
    
                                        // delete the service
                                        apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.delete(nsService, nsServiceName);
                                        if (apiCallResult.errorcode != 0) {
                                            throw new ExecutionException("Failed to delete service: " + nsServiceName + " due to " + apiCallResult.message);
                                        }
    
                                        // delete the server if there is no associated services
                                        server_service_binding[] services = server_service_binding.get(nsService, nsServerName);
                                        if ((services == null) || (services.length == 0)) {
                                            apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(nsService, nsServerName);
                                            if (apiCallResult.errorcode != 0) {
                                                throw new ExecutionException("Failed to remove server:" + nsServerName + " due to " + apiCallResult.message);
                                            }
                                        }
                                    }
                                }
                            }
                         }
                    }
                } else {
                    // delete the implemented load balancing rule and its destinations 
                    lbvserver lbserver = getVirtualServerIfExisits(nsVirtualServerName);
                    if (lbserver == null) {
                        throw new ExecutionException("Failed to find virtual server with name:" + nsVirtualServerName);
                    }
                    //unbind the all services associated with this virtual server
                    com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(nsService, nsVirtualServerName);
                    
                    if (serviceBindings != null) {
                        for (com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                            String serviceName = binding.get_servicename();
                            apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.delete(nsService, binding);
                            if (apiCallResult.errorcode != 0) {
                                throw new ExecutionException("Failed to unbind service from the lb virtual server: " + nsVirtualServerName + " due to " + apiCallResult.message);
                            }
    
                            com.citrix.netscaler.nitro.resource.config.basic.service svc = com.citrix.netscaler.nitro.resource.config.basic.service.get(nsService, serviceName);
                            String nsServerName = svc.get_servername();
                            
                            // delete the service
                            com.citrix.netscaler.nitro.resource.config.basic.service.delete(nsService, serviceName);
                            
                            //delete the server if no more services attached
                            server_service_binding[] services = server_service_binding.get(nsService, nsServerName);
                            if ((services == null) || (services.length == 0)) {
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(nsService, nsServerName);
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to remove server:" + nsServerName + " due to " + apiCallResult.message);
                                }
                            }
                        }
                    }
                    removeLBVirtualServer(nsVirtualServerName);    
                }
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Successfully executed resource LoadBalancerConfigCommand: " + _gson.toJson(cmd));
            }

            saveConfiguration();
            return new Answer(cmd);        
        } catch (ExecutionException e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to " + e.getMessage());
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, e);
            }
        }  catch (Exception e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to " + e.getMessage());
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, e);
            }
        } 
    }

    private synchronized ExternalNetworkResourceUsageAnswer execute(ExternalNetworkResourceUsageCommand cmd) {
        try {
            return getPublicIpBytesSentAndReceived(cmd);
        } catch (ExecutionException e) {
            return new ExternalNetworkResourceUsageAnswer(cmd, e);
        }
    }
    
    private void addGuestVlanAndSubnet(long vlanTag, String vlanSelfIp, String vlanNetmask) throws ExecutionException {
        org.apache.axis.types.UnsignedInt result;

        try {
            String vlanName = generateVlanName(vlanTag);    
            if (!nsVlanExists(vlanTag)) {
                // add new vlan
                vlan vlanObj = new vlan();
                vlanObj.set_id(vlanTag);
                apiCallResult = vlan.add(nsService, vlanObj);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to add new vlan with tag:" + vlanTag + "due to" + apiCallResult.message);
                }
            
                // add self-ip and subnet to the Netscaler
                nsip selfIp = new nsip();
                selfIp.set_ipaddress(vlanSelfIp);
                selfIp.set_netmask(vlanNetmask);
                selfIp.set_type("SNIP");
                apiCallResult = nsip.add(nsService, selfIp);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to add new self-ip due to "+ apiCallResult.message);
                }

                //bind the vlan to guest subnet
                vlan_nsip_binding ipVlanBinding = new vlan_nsip_binding();
                ipVlanBinding.set_id(vlanTag);
                ipVlanBinding.set_ipaddress(vlanSelfIp);
                ipVlanBinding.set_netmask(vlanNetmask);
                apiCallResult = vlan_nsip_binding.add(nsService, ipVlanBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to bind vlan with tag:" + vlanTag + " to the subnet due to" + apiCallResult.message);
                }

                // bind vlan to the private interface
                vlan_interface_binding vlanBinding = new vlan_interface_binding();
                vlanBinding.set_ifnum(_privateInterface);
                vlanBinding.set_tagged(true);
                vlanBinding.set_id(vlanTag);
                apiCallResult = vlan_interface_binding.add(nsService, vlanBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to bind vlan with tag:" + vlanTag + " with the interface " + _privateInterface + " due to " + apiCallResult.message);
                }
            } else {
                throw new ExecutionException("Failed to configure Netscaler device for vlan with tag " + vlanTag + " as vlan already exisits");
            }
        }  catch (nitro_exception e) {
            throw new ExecutionException("Failed to implement guest network on the Netscaler device");
        }  catch (Exception e) {
            throw new ExecutionException("Failed to implement guest network on the Netscaler device");            
        }
    }

    private void deleteGuestVlan(long vlanTag, String vlanSelfIp, String vlanNetmask) throws ExecutionException {
        org.apache.axis.types.UnsignedInt result;

        try {
            if (nsVlanExists(vlanTag)) {

                // Delete all servers and associated services from this guest VLAN
                deleteServersInGuestVlan(vlanTag, vlanSelfIp, vlanNetmask);

                // unbind vlan to the private interface
                vlan_interface_binding vlanIfBinding = new vlan_interface_binding();
                vlanIfBinding.set_id(vlanTag);
                vlanIfBinding.set_ifnum(_privateInterface);
                vlanIfBinding.set_tagged(true);
                apiCallResult = vlan_interface_binding.delete(nsService, vlanIfBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to unbind vlan:" + vlanTag + " with the private interface due to " + apiCallResult.message);
                }

                //unbind the vlan to subnet
                vlan_nsip_binding vlanSnipBinding = new vlan_nsip_binding();
                vlanSnipBinding.set_netmask(vlanNetmask);
                vlanSnipBinding.set_ipaddress(vlanSelfIp);
                vlanSnipBinding.set_id(vlanTag);
                apiCallResult = vlan_nsip_binding.delete(nsService, vlanSnipBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to unbind vlan:" + vlanTag + " with the subnet due to " + apiCallResult.message);
                }

                // remove subnet IP
                nsip subnetIp = nsip.get(nsService, vlanSelfIp);
                apiCallResult = nsip.delete(nsService, subnetIp);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to remove subnet ip:" + vlanTag + " to the subnet due to" + apiCallResult.message);
                }

                // remove vlan
                apiCallResult = com.citrix.netscaler.nitro.resource.config.network.vlan.delete(nsService, vlanTag);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to remove vlan with tag:" + vlanTag + "due to" + apiCallResult.message);
                }
            } 
        }  catch (nitro_exception e) {
            throw new ExecutionException("Failed to delete guest vlan network on the Netscaler device");
        }  catch (Exception e) {
            s_logger.error(e);
            throw new ExecutionException(e.getMessage());
        }
    }

    private boolean nsVlanExists(long vlanTag) {
        try {
            if (vlan.get(nsService, new Long(vlanTag)) != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean nsServerExists(String serverName) throws ExecutionException {
        try {
            if (com.citrix.netscaler.nitro.resource.config.basic.server.get(nsService, serverName) != null) {
                return true;
            } else {
                return false;
            }
        } catch (nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private lbvserver getVirtualServerIfExisits(String lbVServerName ) throws ExecutionException {
        try {
            return lbvserver.get(nsService, lbVServerName);
        } catch (nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return null;
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private boolean nsServiceExists(String serviceName) throws ExecutionException {
        try {
            if (com.citrix.netscaler.nitro.resource.config.basic.service.get(nsService, serviceName) != null) {
                return true;
            } else {
                return false;
            }
        } catch (nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_NO_SERIVCE) {
                return false;
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }
    
    private boolean nsServiceBindingExists(String lbVirtualServer, String serviceName) throws ExecutionException {
        try {
            com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(nsService, lbVirtualServer);
            if (serviceBindings != null) {
                for (com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                    if (serviceName.equalsIgnoreCase(binding.get_servicename())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (nitro_exception e) {
            throw new ExecutionException(e.getMessage());
        } catch (Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }
    
    private void deleteServersInGuestVlan(long vlanTag, String vlanSelfIp, String vlanNetmask) throws ExecutionException {
        try {
            com.citrix.netscaler.nitro.resource.config.basic.server[] serverList = com.citrix.netscaler.nitro.resource.config.basic.server.get(nsService);

            if (serverList == null) {
                return;
            }

            // remove the server and services associated with guest vlan
            for (com.citrix.netscaler.nitro.resource.config.basic.server server : serverList) {
                // check if server belong to same subnet as one associated with vlan
                if (NetUtils.sameSubnet(vlanSelfIp, server.get_ipaddress(), vlanNetmask)) {
                    // first remove services associated with this server
                    com.citrix.netscaler.nitro.resource.config.basic.service serveicesList[] = com.citrix.netscaler.nitro.resource.config.basic.service.get(nsService);
                    if (serveicesList != null) {
                        for (com.citrix.netscaler.nitro.resource.config.basic.service svc : serveicesList) {
                            if (svc.get_servername().equals(server.get_ipaddress())) {
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.delete(nsService, svc.get_name());
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to remove service:" + svc.get_name());
                                }
                            }
                        }
                    }
                    // remove the server
                    apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(nsService, server.get_name());
                    if (apiCallResult.errorcode != 0) {
                        throw new ExecutionException("Failed to remove server:" + server.get_name()+ " due to " + apiCallResult.message);
                    }
                }
            }
        } catch (Exception e) {
            throw new ExecutionException("Failed to delete server and services in the guest vlan:" + vlanTag + " on the Netscaler device due to: "+ e.getMessage());
        }
    }

    private void addLBVirtualServer(String virtualServerName, String srcIp, int srcPort, String lbMethod, String lbProtocol) throws ExecutionException {
        try {

            if (lbProtocol == null) {
                lbProtocol = "TCP";
            } else if (lbProtocol.equals(NetUtils.TCP_PROTO)){
                lbProtocol = "TCP";
            } else if (lbProtocol.equals(NetUtils.UDP_PROTO)) {
                lbProtocol = "UDP";
            } else {
                throw new ExecutionException("Got invalid protocol: " + lbProtocol);
            }

            if (lbMethod.equals("roundrobin")) {
                lbMethod = "ROUNDROBIN";
            } else if (lbMethod.equals("leastconn")) {
                lbMethod = "LEASTCONNECTION";
            } else {
                throw new ExecutionException("Got invalid load balancing algorithm: " + lbMethod);
            }

            boolean vserverExisis = false;
            lbvserver vserver = getVirtualServerIfExisits(virtualServerName);
            if (vserver == null) {
                vserver = new lbvserver();
            } else {
            	vserverExisis = true;
            }
            vserver.set_name(virtualServerName);
            vserver.set_ipv46(srcIp);
            vserver.set_port(srcPort);
            vserver.set_servicetype(lbProtocol);
            vserver.set_lbmethod(lbMethod);

            if (vserverExisis) {
                apiCallResult = lbvserver.update(nsService,vserver);
            } else {
                apiCallResult = lbvserver.add(nsService,vserver);
            }
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Failed to create new virtual server:" + virtualServerName+ " due to " + apiCallResult.message);
            }            
        } catch (nitro_exception e) {
            if (e.getErrorCode() != NitroError.NS_RESOURCE_EXISTS) {
                throw new ExecutionException("Failed to create new virtual server:" + virtualServerName + " due to " + e.getMessage());    
            }
        } catch (Exception e) {
            throw new ExecutionException("Failed to create new virtual server:" + virtualServerName + " due to " + e.getMessage());
        }
    }
    
    private void removeLBVirtualServer (String virtualServerName) throws ExecutionException {
        try {
            lbvserver vserver = lbvserver.get(nsService, virtualServerName);
            if (vserver == null) {
                throw new ExecutionException("Failed to find virtual server with name:" + virtualServerName);
            }
            apiCallResult = lbvserver.delete(nsService, vserver);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Failed to remove virtual server:" + virtualServerName + " due to " + apiCallResult.message);
            }
        } catch (nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return;
            } else {
                throw new ExecutionException("Failed remove virtual server:" + virtualServerName +" due to " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ExecutionException("Failed to remove virtual server:" + virtualServerName +" due to " + e.getMessage());
        }
    }
    
    private void saveConfiguration() throws ExecutionException {
        try {
            apiCallResult = nsconfig.save(nsService);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Error occured while saving configuration changes to Netscaler device due to " + apiCallResult.message);
            }
        } catch (nitro_exception e) {
            throw new ExecutionException("Failed to save configuration changes to Netscaler device due to " + e.getMessage());
        } catch (Exception e) {
            throw new ExecutionException("Failed to save configuration changes to Netscaler device due to " + e.getMessage());
        }
    }

    private ExternalNetworkResourceUsageAnswer getPublicIpBytesSentAndReceived(ExternalNetworkResourceUsageCommand cmd) throws ExecutionException {
        ExternalNetworkResourceUsageAnswer answer = new ExternalNetworkResourceUsageAnswer(cmd);
        
        try {
        	
        	lbvserver_stats[] stats = lbvserver_stats.get(nsService);

			for (lbvserver_stats stat_entry : stats) {
				String lbvserverName = stat_entry.get_name();
		        lbvserver vserver = lbvserver.get(nsService, lbvserverName);
		        String lbVirtualServerIp = vserver.get_ipv46();

		        long[] bytesSentAndReceived = answer.ipBytes.get(lbVirtualServerIp);
				if (bytesSentAndReceived == null) {
				    bytesSentAndReceived = new long[]{0, 0};
				}
				bytesSentAndReceived[0] += stat_entry.get_totalrequestbytes();
				bytesSentAndReceived[1] += stat_entry.get_totalresponsebytes();

				if (bytesSentAndReceived[0] >= 0 && bytesSentAndReceived[1] >= 0) {
					answer.ipBytes.put(lbVirtualServerIp, bytesSentAndReceived);			
				}
			}
        } catch (Exception e) {
            s_logger.error(e);
            throw new ExecutionException(e.getMessage());
        }
        
        return answer;
    }

    private Answer retry(Command cmd, int numRetries) {                
        int numRetriesRemaining = numRetries - 1;
        s_logger.error("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetriesRemaining);
        return executeRequest(cmd, numRetriesRemaining);    
    }

    private boolean shouldRetry(int numRetries) {
    	try {
    		if (numRetries > 0) {
    			login();
    			return true;
    		}
        } catch (Exception e) {
        	s_logger.error("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        }
        return false;
    }
    
    private String generateVlanName(long vlanTag) {
        return genObjectName("cloud-vlan",  String.valueOf(vlanTag));
    }

    private String generateNSVirtualServerName(String srcIp, long srcPort, String protocol) {
        return genObjectName("cloud-VirtualServer", protocol, srcIp, srcPort);
    }
    
    private String generateNSServerName(String serverIP) {
        return genObjectName("cloud-server",  serverIP);
    }

    private String generateNSServiceName(String ip, long port) {
        return genObjectName("cloud-Service", ip, port);
    }
    
    private String genObjectName(Object... args) {
        String objectName = "";
        for (int i = 0; i < args.length; i++) {
            objectName += args[i];
            if (i != args.length -1) {
                objectName += _objectNamePathSep;
            }
        }
        return objectName;
    }
    
    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(Host.Type.ExternalLoadBalancer, id);
    }

    @Override
    public Type getType() {
        return Host.Type.ExternalLoadBalancer;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
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
    public void disconnected() {
        return;
    }
}
