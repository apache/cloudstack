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

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import com.citrix.netscaler.nitro.exception.nitro_exception;
import com.citrix.netscaler.nitro.resource.base.base_response;
import com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction;
import com.citrix.netscaler.nitro.resource.config.autoscale.autoscalepolicy;
import com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleprofile;
import com.citrix.netscaler.nitro.resource.config.basic.server_service_binding;
import com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding;
import com.citrix.netscaler.nitro.resource.config.basic.servicegroup;
import com.citrix.netscaler.nitro.resource.config.basic.servicegroup_lbmonitor_binding;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbservice;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbservice_lbmonitor_binding;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbsite;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbsite_gslbservice_binding;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbvserver;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbvserver_domain_binding;
import com.citrix.netscaler.nitro.resource.config.gslb.gslbvserver_gslbservice_binding;
import com.citrix.netscaler.nitro.resource.config.lb.lbmetrictable;
import com.citrix.netscaler.nitro.resource.config.lb.lbmetrictable_metric_binding;
import com.citrix.netscaler.nitro.resource.config.lb.lbmonitor;
import com.citrix.netscaler.nitro.resource.config.lb.lbmonitor_metric_binding;
import com.citrix.netscaler.nitro.resource.config.lb.lbvserver;
import com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding;
import com.citrix.netscaler.nitro.resource.config.lb.lbvserver_servicegroup_binding;
import com.citrix.netscaler.nitro.resource.config.network.Interface;
import com.citrix.netscaler.nitro.resource.config.network.inat;
import com.citrix.netscaler.nitro.resource.config.network.rnat;
import com.citrix.netscaler.nitro.resource.config.network.vlan;
import com.citrix.netscaler.nitro.resource.config.network.vlan_interface_binding;
import com.citrix.netscaler.nitro.resource.config.network.vlan_nsip_binding;
import com.citrix.netscaler.nitro.resource.config.ns.nsconfig;
import com.citrix.netscaler.nitro.resource.config.ns.nshardware;
import com.citrix.netscaler.nitro.resource.config.ns.nsip;
import com.citrix.netscaler.nitro.resource.config.ns.nstimer;
import com.citrix.netscaler.nitro.resource.config.ns.nstimer_autoscalepolicy_binding;
import com.citrix.netscaler.nitro.resource.config.ssl.sslcertkey;
import com.citrix.netscaler.nitro.resource.config.ssl.sslcertkey_sslvserver_binding;
import com.citrix.netscaler.nitro.resource.config.ssl.sslcertlink;
import com.citrix.netscaler.nitro.resource.config.ssl.sslvserver_sslcertkey_binding;
import com.citrix.netscaler.nitro.resource.stat.lb.lbvserver_stats;
import com.citrix.netscaler.nitro.service.nitro_service;
import com.citrix.netscaler.nitro.util.filtervalue;
import com.citrix.sdx.nitro.resource.config.mps.mps;
import com.citrix.sdx.nitro.resource.config.ns.ns;
import com.citrix.sdx.nitro.resource.config.xen.xen_nsvpx_image;
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
import com.cloud.agent.api.routing.CreateLoadBalancerApplianceCommand;
import com.cloud.agent.api.routing.DestroyLoadBalancerApplianceCommand;
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigAnswer;
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.agent.api.routing.HealthCheckLBConfigAnswer;
import com.cloud.agent.api.routing.HealthCheckLBConfigCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SiteLoadBalancerConfig;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScalePolicyTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmGroupTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmProfileTO;
import com.cloud.agent.api.to.LoadBalancerTO.ConditionTO;
import com.cloud.agent.api.to.LoadBalancerTO.CounterTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.agent.api.to.LoadBalancerTO.HealthCheckPolicyTO;
import com.cloud.agent.api.to.LoadBalancerTO.StickinessPolicyTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.security.CertificateHelper;
import com.cloud.utils.ssh.SshHelper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

class NitroError {
    static final int NS_RESOURCE_EXISTS = 273;
    static final int NS_RESOURCE_NOT_EXISTS = 258;
    static final int NS_NO_SERIVCE = 344;
    static final int NS_OPERATION_NOT_PERMITTED = 257;
    static final int NS_INTERFACE_ALREADY_BOUND_TO_VLAN = 2080;
    static final int NS_GSLB_DOMAIN_ALREADY_BOUND = 1842;
}

public class NetscalerResource implements ServerResource {

    public final static int DEFAULT_SNMP_PORT = 161;
    // deployment configuration
    private String _name;
    private String _zoneId;
    private String _physicalNetworkId;
    private String _ip;
    private String _username;
    private String _password;
    private String _publicInterface;
    private String _privateInterface;
    private Integer _numRetries;
    private String _guid;
    private boolean _isSdx;
    private boolean _cloudManaged;
    private String _deviceName;
    private String _publicIP;
    private String _publicIPNetmask;
    private String _publicIPVlan;

    private static final Logger s_logger = Logger.getLogger(NetscalerResource.class);
    protected Gson _gson;
    private final String _objectNamePathSep = "-";

    // interface to interact with VPX and MPX devices
    com.citrix.netscaler.nitro.service.nitro_service _netscalerService;

    // interface to interact with service VM of the SDX appliance
    com.citrix.sdx.nitro.service.nitro_service _netscalerSdxService;

    base_response apiCallResult;

    public NetscalerResource() {
        _gson = GsonHelper.getGsonLogger();
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String)params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name in the configuration parameters");
            }

            _zoneId = (String)params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone Id  in the configuration parameters");
            }

            _physicalNetworkId = (String)params.get("physicalNetworkId");
            if (_physicalNetworkId == null) {
                throw new ConfigurationException("Unable to find physical network id in the configuration parameters");
            }

            _ip = (String)params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP address in the configuration parameters");
            }

            _username = (String)params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username in the configuration parameters");
            }

            _password = (String)params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password in the configuration parameters");
            }

            _publicInterface = (String)params.get("publicinterface");
            if (_publicInterface == null) {
                throw new ConfigurationException("Unable to find public interface in the configuration parameters");
            }

            _privateInterface = (String)params.get("privateinterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface in the configuration parameters");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 2);

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid in the configuration parameters");
            }

            _deviceName = (String)params.get("deviceName");
            if (_deviceName == null) {
                throw new ConfigurationException("Unable to find the device name in the configuration parameters");
            }

            _isSdx = _deviceName.equalsIgnoreCase("NetscalerSDXLoadBalancer");

            if ((String)params.get("cloudmanaged") != null) {
                _cloudManaged = Boolean.parseBoolean((String)params.get("cloudmanaged"));
            }

            // validate device configuration parameters
            login();
            validateDeviceType(_deviceName);
            validateInterfaces(_publicInterface, _privateInterface);

            //enable load balancing feature
            enableLoadBalancingFeature();
            SSL.enableSslFeature(_netscalerService, _isSdx);

            //if the the device is cloud stack provisioned then make it part of the public network
            if (_cloudManaged) {
                _publicIP = (String)params.get("publicip");
                _publicIPNetmask = (String)params.get("publicipnetmask");
                _publicIPVlan = (String)params.get("publicipvlan");
                if ("untagged".equalsIgnoreCase(_publicIPVlan)) {
                    // if public network is un-tagged just add subnet IP
                    addSubnetIP(_publicIP, _publicIPNetmask);
                } else {
                    // if public network is tagged then add vlan and bind subnet IP to the vlan
                    addGuestVlanAndSubnet(Long.parseLong(_publicIPVlan), _publicIP, _publicIPNetmask, false);
                }
            }

            return true;
        } catch (final Exception e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    private void logout() throws ExecutionException {
        try {
            if (!_isSdx) {
                if (_netscalerService != null) {
                    _netscalerService.logout();
                }
            } else {
                if (_netscalerSdxService != null) {
                    _netscalerSdxService.logout();
                }
            }
        } catch (final Exception e) {
            // Ignore logout exceptions
        }
    }

    private void login() throws ExecutionException {
        try {
            // If a previous session was open, log it out.
            logout();
            if (!_isSdx) {
                _netscalerService = new nitro_service(_ip, "https");
                _netscalerService.set_credential(_username, _password);
                _netscalerService.set_certvalidation(false);
                _netscalerService.set_hostnameverification(false);
                apiCallResult = _netscalerService.login();
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to log in to Netscaler device at " + _ip + " due to error " + apiCallResult.errorcode + " and message " +
                            apiCallResult.message);
                }
            } else {
                _netscalerSdxService = new com.citrix.sdx.nitro.service.nitro_service(_ip, "https");
                _netscalerSdxService.set_credential(_username, _password);
                final com.citrix.sdx.nitro.resource.base.login login = _netscalerSdxService.login();
                if (login == null) {
                    throw new ExecutionException("Failed to log in to Netscaler SDX device at " + _ip + " due to error " + apiCallResult.errorcode + " and message " +
                            apiCallResult.message);
                }
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        }
    }

    private void enableLoadBalancingFeature() throws ExecutionException {
        if (_isSdx) {
            return;
        }
        try {
            final String[] features = _netscalerService.get_enabled_features();
            if (features != null) {
                for (final String feature : features) {
                    if (feature.equalsIgnoreCase("LB")) {
                        return;
                    }
                }
            }

            // enable load balancing on the device
            final String[] feature = new String[1];
            feature[0] = "LB";
            apiCallResult = _netscalerService.enable_features(feature);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Enabling load balancing feature on the device failed.");
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Enabling load balancing feature on the device failed  due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Enabling load balancing feature on the device failed due to " + e.getMessage());
        }
    }

    private void validateInterfaces(final String publicInterface, final String privateInterface) throws ExecutionException {
        try {
            if (!_isSdx && !_cloudManaged) {
                final Interface publicIf = Interface.get(_netscalerService, publicInterface);
                final Interface privateIf = Interface.get(_netscalerService, privateInterface);
                if (publicIf != null || privateIf != null) {
                    return;
                } else {
                    throw new ExecutionException("Invalid interface name specified for public/private interfaces.");
                }
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                throw new ExecutionException("Invalid interface name specified for public and private interfaces.");
            } else {
                throw new ExecutionException("Failed to verify public interface and private intefaces are valid due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify public interface and private intefaces are valid due to " + e.getMessage());
        }
    }

    private void validateDeviceType(final String deviceType) throws ExecutionException {
        try {
            if (!_isSdx && !_cloudManaged) {
                final nshardware nsHw = com.citrix.netscaler.nitro.resource.config.ns.nshardware.get(_netscalerService);
                if (nsHw == null) {
                    throw new ExecutionException("Failed to get the hardware description of the Netscaler device at " + _ip);
                } else {
                    if (_deviceName.equalsIgnoreCase("NetscalerMPXLoadBalancer") && nsHw.get_hwdescription().contains("MPX") ||
                            _deviceName.equalsIgnoreCase("NetscalerVPXLoadBalancer") && nsHw.get_hwdescription().contains("NetScaler Virtual Appliance")) {
                        return;
                    }
                    throw new ExecutionException("Netscalar device type specified does not match with the actuall device type.");
                }
            } else if (_isSdx) {
                final mps serviceVM = mps.get(_netscalerSdxService);
                if (serviceVM != null) {
                    if (serviceVM.get_platform().contains("SDX") || serviceVM.get_product().contains("SDX")) {
                        return;
                    } else {
                        throw new ExecutionException("Netscalar device type specified does not match with the actuall device type.");
                    }
                } else {
                    throw new ExecutionException("Failed to get the hardware details of the Netscaler device at " + _ip);
                }
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify device type specified when matching with actuall device type due to " + e.getMessage());
        }
    }

    @Override
    public StartupCommand[] initialize() {
        final StartupExternalLoadBalancerCommand cmd = new StartupExternalLoadBalancerCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion(NetscalerResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Answer executeRequest(final Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    private Answer executeRequest(final Command cmd, final int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd, numRetries);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            return execute((LoadBalancerConfigCommand)cmd, numRetries);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd, numRetries);
        } else if (cmd instanceof CreateLoadBalancerApplianceCommand) {
            return execute((CreateLoadBalancerApplianceCommand)cmd, numRetries);
        } else if (cmd instanceof DestroyLoadBalancerApplianceCommand) {
            return execute((DestroyLoadBalancerApplianceCommand)cmd, numRetries);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand)cmd, numRetries);
        } else if (cmd instanceof GlobalLoadBalancerConfigCommand) {
            return execute((GlobalLoadBalancerConfigCommand)cmd, numRetries);
        } else if (cmd instanceof HealthCheckLBConfigCommand) {
            return execute((HealthCheckLBConfigCommand)cmd, numRetries);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(final ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    protected Answer execute(final MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private synchronized Answer execute(final IpAssocCommand cmd, final int numRetries) {
        if (_isSdx) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }

        final String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        try {
            final IpAddressTO[] ips = cmd.getIpAddresses();
            for (final IpAddressTO ip : ips) {
                final long guestVlanTag = Long.parseLong(ip.getBroadcastUri());
                final String vlanSelfIp = ip.getVlanGateway();
                final String vlanNetmask = ip.getVlanNetmask();

                if (ip.isAdd()) {
                    // Add a new guest VLAN and its subnet and bind it to private interface
                    addGuestVlanAndSubnet(guestVlanTag, vlanSelfIp, vlanNetmask, true);
                } else {
                    // Check and delete guest VLAN with this tag, self IP, and netmask
                    deleteGuestVlan(guestVlanTag, vlanSelfIp, vlanNetmask);
                }

                saveConfiguration();
                results[i++] = ip.getPublicIp() + " - success";
                final String action = ip.isAdd() ? "associate" : "remove";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Netscaler load balancer " + _ip + " successfully executed IPAssocCommand to " + action + " IP " + ip);
                }
            }
        } catch (final ExecutionException e) {
            s_logger.error("Netscaler loadbalancer " + _ip + " failed to execute IPAssocCommand due to " + e.getMessage());
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }

        return new IpAssocAnswer(cmd, results);
    }

    private Answer execute(final HealthCheckLBConfigCommand cmd, final int numRetries) {

        final List<LoadBalancerTO> hcLB = new ArrayList<LoadBalancerTO>();
        try {

            if (_isSdx) {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            final LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();

            if (loadBalancers == null) {
                return new HealthCheckLBConfigAnswer(hcLB);
            }

            for (final LoadBalancerTO loadBalancer : loadBalancers) {
                final HealthCheckPolicyTO[] healthCheckPolicies = loadBalancer.getHealthCheckPolicies();
                if (healthCheckPolicies != null && healthCheckPolicies.length > 0 && healthCheckPolicies[0] != null) {
                    final String nsVirtualServerName = generateNSVirtualServerName(loadBalancer.getSrcIp(), loadBalancer.getSrcPort());

                    final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings =
                            com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(_netscalerService, nsVirtualServerName);

                    if (serviceBindings != null) {
                        for (final DestinationTO destination : loadBalancer.getDestinations()) {
                            final String nsServiceName = generateNSServiceName(destination.getDestIp(), destination.getDestPort());
                            for (final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                                if (nsServiceName.equalsIgnoreCase(binding.get_servicename())) {
                                    destination.setMonitorState(binding.get_curstate());
                                    break;
                                }
                            }
                        }
                        hcLB.add(loadBalancer);
                    }
                }
            }

        } catch (final ExecutionException e) {
            s_logger.error("Failed to execute HealthCheckLBConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new HealthCheckLBConfigAnswer(hcLB);
            }
        } catch (final Exception e) {
            s_logger.error("Failed to execute HealthCheckLBConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new HealthCheckLBConfigAnswer(hcLB);
            }
        }
        return new HealthCheckLBConfigAnswer(hcLB);
    }

    private synchronized Answer execute(final LoadBalancerConfigCommand cmd, final int numRetries) {
        try {
            if (_isSdx) {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            final LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();
            if (loadBalancers == null) {
                return new Answer(cmd);
            }

            for (final LoadBalancerTO loadBalancer : loadBalancers) {
                final String srcIp = loadBalancer.getSrcIp();
                final int srcPort = loadBalancer.getSrcPort();
                final String lbProtocol = getNetScalerProtocol(loadBalancer);
                final String lbAlgorithm = loadBalancer.getAlgorithm();
                final String nsVirtualServerName = generateNSVirtualServerName(srcIp, srcPort);
                final String nsMonitorName = generateNSMonitorName(srcIp, srcPort);
                final LbSslCert sslCert = loadBalancer.getSslCert();

                if (loadBalancer.isAutoScaleVmGroupTO()) {
                    applyAutoScaleConfig(loadBalancer);
                    // Continue to process all the rules.
                    continue;
                }
                boolean hasMonitor = false;
                boolean deleteMonitor = false;
                boolean destinationsToAdd = false;
                boolean deleteCert = false;
                for (final DestinationTO destination : loadBalancer.getDestinations()) {
                    if (!destination.isRevoked()) {
                        destinationsToAdd = true;
                        break;
                    }
                }

                if (!loadBalancer.isRevoked() && destinationsToAdd) {

                    // create a load balancing virtual server
                    addLBVirtualServer(nsVirtualServerName, srcIp, srcPort, lbAlgorithm, lbProtocol, loadBalancer.getStickinessPolicies(), null);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Created load balancing virtual server " + nsVirtualServerName + " on the Netscaler device");
                    }

                    // create a new monitor
                    final HealthCheckPolicyTO[] healthCheckPolicies = loadBalancer.getHealthCheckPolicies();
                    if (healthCheckPolicies != null && healthCheckPolicies.length > 0 && healthCheckPolicies[0] != null) {

                        for (final HealthCheckPolicyTO healthCheckPolicyTO : healthCheckPolicies) {
                            if (!healthCheckPolicyTO.isRevoked()) {
                                addLBMonitor(nsMonitorName, lbProtocol, healthCheckPolicyTO);
                                hasMonitor = true;
                            } else {
                                deleteMonitor = true;
                                hasMonitor = false;
                            }
                        }

                    }

                    for (final DestinationTO destination : loadBalancer.getDestinations()) {

                        final String nsServerName = generateNSServerName(destination.getDestIp());
                        final String nsServiceName = generateNSServiceName(destination.getDestIp(), destination.getDestPort());
                        if (!destination.isRevoked()) {
                            // add a new destination to deployed load balancing rule

                            // add a new server
                            if (!nsServerExists(nsServerName)) {
                                final com.citrix.netscaler.nitro.resource.config.basic.server nsServer = new com.citrix.netscaler.nitro.resource.config.basic.server();
                                nsServer.set_name(nsServerName);
                                nsServer.set_ipaddress(destination.getDestIp());
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.add(_netscalerService, nsServer);
                                if (apiCallResult.errorcode != 0 && apiCallResult.errorcode != NitroError.NS_RESOURCE_EXISTS) {
                                    throw new ExecutionException("Failed to add server " + destination.getDestIp() + " due to" + apiCallResult.message);
                                }
                            }

                            // create a new service using the server added
                            if (!nsServiceExists(nsServiceName)) {
                                final com.citrix.netscaler.nitro.resource.config.basic.service newService = new com.citrix.netscaler.nitro.resource.config.basic.service();
                                newService.set_name(nsServiceName);
                                newService.set_port(destination.getDestPort());
                                newService.set_servername(nsServerName);
                                newService.set_state("ENABLED");
                                if(lbProtocol.equalsIgnoreCase(NetUtils.SSL_PROTO)) {
                                    newService.set_servicetype(NetUtils.HTTP_PROTO);
                                } else {
                                    newService.set_servicetype(lbProtocol);
                                }

                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.add(_netscalerService, newService);
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to create service " + nsServiceName + " using server " + nsServerName + " due to" +
                                            apiCallResult.message);
                                }
                            }

                            //bind service to load balancing virtual server
                            if (!nsServiceBindingExists(nsVirtualServerName, nsServiceName)) {
                                final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding svcBinding =
                                        new com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding();
                                svcBinding.set_name(nsVirtualServerName);
                                svcBinding.set_servicename(nsServiceName);
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.add(_netscalerService, svcBinding);

                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to bind service: " + nsServiceName + " to the lb virtual server: " + nsVirtualServerName +
                                            " on Netscaler device");
                                }
                            }

                            // After binding the service to the LB Vserver
                            // successfully, bind the created monitor to the
                            // service.
                            if (hasMonitor) {
                                if (!isServiceBoundToMonitor(nsServiceName, nsMonitorName)) {
                                    bindServiceToMonitor(nsServiceName, nsMonitorName);
                                }
                            } else {
                                // check if any monitor created by CS is already
                                // existing, if yes, unbind it from services and
                                // delete it.
                                if (nsMonitorExist(nsMonitorName)) {
                                    // unbind the service from the monitor and
                                    // delete the monitor
                                    unBindServiceToMonitor(nsServiceName, nsMonitorName);
                                    deleteMonitor = true;
                                }

                            }

                            if (sslCert != null && lbProtocol.equalsIgnoreCase(NetUtils.SSL_PROTO)) {
                                if (sslCert.isRevoked()) {
                                    deleteCert = true;
                                } else {

                                    // If there is a chain, that should go first to the NS

                                    String previousCertKeyName = null;

                                    if (sslCert.getChain() != null) {
                                        final List<Certificate> chainList = CertificateHelper.parseChain(sslCert.getChain());
                                        // go from ROOT to intermediate CAs
                                        for (final Certificate intermediateCert : Lists.reverse(chainList)) {

                                            final String fingerPrint = CertificateHelper.generateFingerPrint(intermediateCert);
                                            final String intermediateCertKeyName = generateSslCertKeyName(fingerPrint);
                                            final String intermediateCertFileName = intermediateCertKeyName + ".pem";

                                            if (!SSL.isSslCertKeyPresent(_netscalerService, intermediateCertKeyName)) {
                                                final PemObject pemObject = new PemObject(intermediateCert.getType(), intermediateCert.getEncoded());
                                                final StringWriter textWriter = new StringWriter();
                                                try (final PemWriter pemWriter = new PemWriter(textWriter);) {
                                                    pemWriter.writeObject(pemObject);
                                                    pemWriter.flush();
                                                } catch (final IOException e) {
                                                    if (s_logger.isDebugEnabled())
                                                    {
                                                        s_logger.debug("couldn't write PEM to a string", e);
                                                    } // else just close the certDataStream
                                                }

                                                SSL.uploadCert(_ip, _username, _password, intermediateCertFileName, textWriter.toString().getBytes());
                                                SSL.createSslCertKey(_netscalerService, intermediateCertFileName, null, intermediateCertKeyName, null);
                                            }

                                            if (previousCertKeyName != null && !SSL.certLinkExists(_netscalerService, intermediateCertKeyName, previousCertKeyName)) {
                                                SSL.linkCerts(_netscalerService, intermediateCertKeyName, previousCertKeyName);
                                            }

                                            previousCertKeyName = intermediateCertKeyName;
                                        }
                                    }

                                    final String certFilename = generateSslCertName(sslCert.getFingerprint()) + ".pem"; //netscaler uses ".pem" format for "bundle" files
                                    final String keyFilename = generateSslKeyName(sslCert.getFingerprint()) + ".pem"; //netscaler uses ".pem" format for "bundle" files
                                    final String certKeyName = generateSslCertKeyName(sslCert.getFingerprint());

                                    try (final ByteArrayOutputStream certDataStream = new ByteArrayOutputStream();) {
                                        certDataStream.write(sslCert.getCert().getBytes());

                                        if (!SSL.isSslCertKeyPresent(_netscalerService, certKeyName)) {

                                            SSL.uploadCert(_ip, _username, _password, certFilename, certDataStream.toByteArray());
                                            SSL.uploadKey(_ip, _username, _password, keyFilename, sslCert.getKey().getBytes());
                                            SSL.createSslCertKey(_netscalerService, certFilename, keyFilename, certKeyName, sslCert.getPassword());
                                        }
                                    } catch (final IOException e) {
                                        if (s_logger.isDebugEnabled())
                                        {
                                            s_logger.debug("couldn't open buffer for certificate", e);
                                        } // else just close the certDataStream
                                    }

                                    if (previousCertKeyName != null && !SSL.certLinkExists(_netscalerService, certKeyName, previousCertKeyName)) {
                                        SSL.linkCerts(_netscalerService, certKeyName, previousCertKeyName);
                                    }

                                    SSL.bindCertKeyToVserver(_netscalerService, certKeyName, nsVirtualServerName);
                                }

                            }

                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Successfully added LB destination: " + destination.getDestIp() + ":" + destination.getDestPort() + " to load balancer " +
                                        srcIp + ":" + srcPort);
                            }

                        } else {
                            // remove a destination from the deployed load balancing rule
                            final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings =
                                    com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(_netscalerService, nsVirtualServerName);
                            if (serviceBindings != null) {
                                for (final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                                    if (nsServiceName.equalsIgnoreCase(binding.get_servicename())) {
                                        // delete the binding
                                        apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.delete(_netscalerService, binding);
                                        if (apiCallResult.errorcode != 0) {
                                            throw new ExecutionException("Failed to delete the binding between the virtual server: " + nsVirtualServerName +
                                                    " and service:" + nsServiceName + " due to" + apiCallResult.message);
                                        }

                                        // check if service is bound to any other virtual server
                                        if (!isServiceBoundToVirtualServer(nsServiceName)) {
                                            // no lb virtual servers are bound to this service so delete it
                                            apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.delete(_netscalerService, nsServiceName);
                                            if (apiCallResult.errorcode != 0) {
                                                throw new ExecutionException("Failed to delete service: " + nsServiceName + " due to " + apiCallResult.message);
                                            }
                                        }

                                        // delete the server if there is no associated services
                                        final server_service_binding[] services = server_service_binding.get(_netscalerService, nsServerName);
                                        if (services == null || services.length == 0) {
                                            apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(_netscalerService, nsServerName);
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
                    final lbvserver lbserver = getVirtualServerIfExisits(nsVirtualServerName);
                    if (lbserver != null) {
                        //unbind the all services associated with this virtual server
                        final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings =
                                com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(_netscalerService, nsVirtualServerName);

                        if (serviceBindings != null) {
                            for (final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                                final String serviceName = binding.get_servicename();
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.delete(_netscalerService, binding);
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to unbind service from the lb virtual server: " + nsVirtualServerName + " due to " +
                                            apiCallResult.message);
                                }

                                final com.citrix.netscaler.nitro.resource.config.basic.service svc =
                                        com.citrix.netscaler.nitro.resource.config.basic.service.get(_netscalerService, serviceName);
                                final String nsServerName = svc.get_servername();

                                // check if service is bound to any other virtual server
                                if (!isServiceBoundToVirtualServer(serviceName)) {
                                    // no lb virtual servers are bound to this service so delete it
                                    apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.delete(_netscalerService, serviceName);
                                    if (apiCallResult.errorcode != 0) {
                                        throw new ExecutionException("Failed to delete service: " + serviceName + " due to " + apiCallResult.message);
                                    }
                                }

                                //delete the server if no more services attached
                                final server_service_binding[] services = server_service_binding.get(_netscalerService, nsServerName);
                                if (services == null || services.length == 0) {
                                    apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(_netscalerService, nsServerName);
                                    if (apiCallResult.errorcode != 0) {
                                        throw new ExecutionException("Failed to remove server:" + nsServerName + " due to " + apiCallResult.message);
                                    }
                                }
                            }
                        }
                        removeLBVirtualServer(nsVirtualServerName);
                        deleteMonitor = true;
                        deleteCert = true;
                    }
                }
                if (deleteMonitor) {
                    removeLBMonitor(nsMonitorName);
                }
                if (sslCert != null && deleteCert) {

                    final String certFilename = generateSslCertName(sslCert.getFingerprint()) + ".pem"; //netscaler uses ".pem" format for "bundle" files
                    final String keyFilename = generateSslKeyName(sslCert.getFingerprint()) + ".pem"; //netscaler uses ".pem" format for "bundle" files
                    final String certKeyName = generateSslCertKeyName(sslCert.getFingerprint());

                    // unbind before deleting
                    if (nsVirtualServerExists(nsVirtualServerName) &&
                            SSL.isSslCertKeyPresent(_netscalerService, certKeyName) &&
                            SSL.isBoundToVserver(_netscalerService, certKeyName, nsVirtualServerName)) {
                        SSL.unbindCertKeyFromVserver(_netscalerService, certKeyName, nsVirtualServerName);
                    }

                    if (SSL.isSslCertKeyPresent(_netscalerService, certKeyName)) {

                        SSL.deleteSslCertKey(_netscalerService, certKeyName);
                        SSL.deleteCertFile(_ip, _username, _password, certFilename);
                        SSL.deleteKeyFile(_ip, _username, _password, keyFilename);
                    }

                    /*
                     * Check and delete intermediate certs:
                     * we can delete an intermediate cert if no other
                     * cert references it as the athority
                     */

                    if (sslCert.getChain() != null) {
                        final List<Certificate> chainList = CertificateHelper.parseChain(sslCert.getChain());
                        //go from intermediate CAs to ROOT
                        for (final Certificate intermediateCert : chainList) {

                            final String fingerPrint = CertificateHelper.generateFingerPrint(intermediateCert);
                            final String intermediateCertKeyName = generateSslCertKeyName(fingerPrint);
                            final String intermediateCertFileName = intermediateCertKeyName + ".pem";

                            if (SSL.isSslCertKeyPresent(_netscalerService, intermediateCertKeyName) &&
                                    !SSL.isCaforCerts(_netscalerService, intermediateCertKeyName)) {
                                SSL.deleteSslCertKey(_netscalerService, intermediateCertKeyName);
                                SSL.deleteCertFile(_ip, _username, _password, intermediateCertFileName);
                            } else {
                                break;// if this cert has another certificate as a child then stop at this point because we need the whole chain
                            }

                        }
                    }
                }

            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Successfully executed resource LoadBalancerConfigCommand: " + _gson.toJson(cmd));
            }

            saveConfiguration();
            return new Answer(cmd);
        } catch (final ExecutionException e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, e);
            }
        } catch (final Exception e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    private synchronized Answer execute(final CreateLoadBalancerApplianceCommand cmd, final int numRetries) {

        if (!_isSdx) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }

        try {
            final String vpxName = "Cloud-VPX-" + cmd.getLoadBalancerIP();
            final String username = "admin";
            final String password = "admin";

            final ns ns_obj = new ns();
            ns_obj.set_name(vpxName);
            ns_obj.set_ip_address(cmd.getLoadBalancerIP());
            ns_obj.set_netmask(cmd.getNetmask());
            ns_obj.set_gateway(cmd.getGateway());
            ns_obj.set_username(username);
            ns_obj.set_password(password);

            // configure VPX instances with defaults
            ns_obj.set_license("Standard");
            ns_obj.set_vm_memory_total(new Double(2048));
            ns_obj.set_throughput(new Double(1000));
            ns_obj.set_pps(new Double(1000000));
            ns_obj.set_number_of_ssl_cores(0);
            ns_obj.set_profile_name("ns_nsroot_profile");

            // use the first VPX image of the available VPX images on the SDX to create an instance of VPX
            // TODO: should enable the option to choose the template while adding the SDX device in to CloudStack
            final xen_nsvpx_image[] vpxImages = xen_nsvpx_image.get(_netscalerSdxService);
            if (!(vpxImages != null && vpxImages.length >= 1)) {
                new Answer(cmd, new ExecutionException("Failed to create VPX instance on the netscaler SDX device " + _ip +
                        " as there are no VPX images on SDX to use for creating VPX."));
            }
            final String imageName = vpxImages[0].get_file_name();
            ns_obj.set_image_name(imageName);

            String publicIf = _publicInterface;
            String privateIf = _privateInterface;

            // enable only the interfaces that will be used by VPX
            enableVPXInterfaces(_publicInterface, _privateInterface, ns_obj);

            // create new VPX instance
            ns newVpx = ns.add(_netscalerSdxService, ns_obj);

            if (newVpx == null) {
                return new Answer(cmd, new ExecutionException("Failed to create VPX instance on the netscaler SDX device " + _ip));
            }

            // wait for VPX instance to start-up
            long startTick = System.currentTimeMillis();
            final long startWaitMilliSeconds = 600000;
            while (!newVpx.get_instance_state().equalsIgnoreCase("up") && System.currentTimeMillis() - startTick < startWaitMilliSeconds) {
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException e) {
                    s_logger.debug("[ignored] interrupted while waiting for netscaler to be 'up'.");
                }
                final ns refreshNsObj = new ns();
                refreshNsObj.set_id(newVpx.get_id());
                newVpx = ns.get(_netscalerSdxService, refreshNsObj);
            }

            // if vpx instance never came up then error out
            if (!newVpx.get_instance_state().equalsIgnoreCase("up")) {
                return new Answer(cmd, new ExecutionException("Failed to start VPX instance " + vpxName + " created on the netscaler SDX device " + _ip));
            }

            // wait till NS service in side VPX is actually ready
            startTick = System.currentTimeMillis();
            boolean nsServiceUp = false;
            final long nsServiceWaitMilliSeconds = 60000;
            while (System.currentTimeMillis() - startTick < nsServiceWaitMilliSeconds) {
                try {
                    final nitro_service netscalerService = new nitro_service(cmd.getLoadBalancerIP(), "https");
                    netscalerService.set_certvalidation(false);
                    netscalerService.set_hostnameverification(false);
                    netscalerService.set_credential(username, password);
                    apiCallResult = netscalerService.login();
                    if (apiCallResult.errorcode == 0) {
                        nsServiceUp = true;
                        break;
                    }
                } catch (final Exception e) {
                    Thread.sleep(10000);
                    continue;
                }
            }

            if (!nsServiceUp) {
                return new Answer(cmd, new ExecutionException("Failed to create VPX instance " + vpxName + " on the netscaler SDX device " + _ip));
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Successfully provisioned VPX instance " + vpxName + " on the Netscaler SDX device " + _ip);
            }

            // physical interfaces on the SDX range from 10/1 to 10/8 & 1/1 to 1/8 of which two different port or same port can be used for public and private interfaces
            // However the VPX instances created will have interface range start from 10/1 but will only have as many interfaces enabled while creating the VPX instance
            // So due to this, we need to map public & private interface on SDX to correct public & private interface of VPX

            final int publicIfnum = Integer.parseInt(_publicInterface.substring(_publicInterface.lastIndexOf("/") + 1));
            final int privateIfnum = Integer.parseInt(_privateInterface.substring(_privateInterface.lastIndexOf("/") + 1));

            if (_publicInterface.startsWith("10/") && _privateInterface.startsWith("10/")) {
                if (publicIfnum == privateIfnum) {
                    publicIf = "10/1";
                    privateIf = "10/1";
                } else if (publicIfnum > privateIfnum) {
                    privateIf = "10/1";
                    publicIf = "10/2";
                } else {
                    publicIf = "10/1";
                    privateIf = "10/2";
                }
            } else if (_publicInterface.startsWith("1/") && _privateInterface.startsWith("1/")) {
                if (publicIfnum == privateIfnum) {
                    publicIf = "1/1";
                    privateIf = "1/1";
                } else if (publicIfnum > privateIfnum) {
                    privateIf = "1/1";
                    publicIf = "1/2";
                } else {
                    publicIf = "1/1";
                    privateIf = "1/2";
                }
            } else if (_publicInterface.startsWith("1/") && _privateInterface.startsWith("10/")) {
                publicIf = "1/1";
                privateIf = "10/1";
            } else if (_publicInterface.startsWith("10/") && _privateInterface.startsWith("1/")) {
                publicIf = "10/1";
                privateIf = "1/1";
            }

            return new CreateLoadBalancerApplianceAnswer(cmd, true, "provisioned VPX instance", "NetscalerVPXLoadBalancer", "Netscaler", new NetscalerResource(),
                    publicIf, privateIf, _username, _password);
        } catch (final Exception e) {
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            }
            return new CreateLoadBalancerApplianceAnswer(cmd, false, "failed to provisioned VPX instance due to " + e.getMessage(), null, null, null, null, null, null,
                    null);
        }
    }

    private Answer execute(final GlobalLoadBalancerConfigCommand gslbCmd, final int numRetries) {

        final String lbMethod = gslbCmd.getLoadBalancerMethod();
        final String persistenceType = gslbCmd.getPersistenceType();
        final String serviceType = gslbCmd.getServiceType();
        final boolean forRevoke = gslbCmd.isForRevoke();
        final long gslbId = gslbCmd.getGslbId();
        final List<SiteLoadBalancerConfig> sites = gslbCmd.getSiteDetails();

        final String domainName = gslbCmd.getDomainName();
        final String vserverName = GSLB.generateVirtualServerName(domainName);

        try {

            if (!forRevoke) { //check if the global load balancer rule is being added

                // Add a GSLB virtual server
                GSLB.createVirtualServer(_netscalerService, vserverName, lbMethod, persistenceType, gslbId, serviceType);

                if (sites != null) { // check if there are any sites that are participating in global load balancing
                    for (final SiteLoadBalancerConfig site : sites) {

                        final String sitePrivateIP = site.getGslbProviderPrivateIp();
                        final String sitePublicIP = site.getGslbProviderPublicIp();
                        final String servicePublicIp = site.getServicePublicIp();
                        final String servicePublicPort = site.getServicePort();
                        final String siteName = GSLB.generateUniqueSiteName(sitePrivateIP, sitePublicIP, site.getDataCenterId());

                        // Add/Delete GSLB local and remote sites that are part of GSLB virtual server
                        if (!site.forRevoke()) {
                            final String siteType = site.isLocal() ? "LOCAL" : "REMOTE";
                            if (GSLB.getSiteObject(_netscalerService, siteName) != null) {
                                GSLB.updateSite(_netscalerService, siteType, siteName, site.getGslbProviderPrivateIp(), site.getGslbProviderPublicIp());
                            } else {
                                GSLB.createSite(_netscalerService, siteName, siteType, site.getGslbProviderPrivateIp(), site.getGslbProviderPublicIp());
                            }
                        }

                        // Add/Delete GSLB service corresponding the service running on each site
                        final String serviceName = GSLB.generateUniqueServiceName(siteName, servicePublicIp, servicePublicPort);
                        final String monitorName = GSLB.generateGslbServiceMonitorName(servicePublicIp);
                        if (!site.forRevoke()) {
                            // create a 'gslbservice' object
                            GSLB.createService(_netscalerService, serviceName, site.getServiceType(), servicePublicIp, servicePublicPort, siteName);

                            // Bind 'gslbservice' service object to GSLB virtual server
                            GSLB.createVserverServiceBinding(_netscalerService, serviceName, vserverName, site.getWeight());

                            // create a monitor for the service running on the site
                            GSLB.createGslbServiceMonitor(_netscalerService, servicePublicIp, serviceName);

                            // bind the monitor to the GSLB service
                            GSLB.createGslbServiceGslbMonitorBinding(_netscalerService, monitorName, serviceName);

                        } else {

                            // delete GSLB service and GSLB monitor binding
                            GSLB.deleteGslbServiceGslbMonitorBinding(_netscalerService, monitorName, serviceName);

                            // Unbind GSLB service with GSLB virtual server
                            GSLB.deleteVserverServiceBinding(_netscalerService, serviceName, vserverName);

                            GSLB.getServiceObject(_netscalerService, serviceName);
                            GSLB.deleteService(_netscalerService, serviceName);

                            // delete the GSLB service monitor
                            GSLB.deleteGslbServiceMonitor(_netscalerService, monitorName);
                        }

                        if (site.forRevoke()) { // delete the site if its for revoke
                            GSLB.deleteSite(_netscalerService, siteName);
                        }
                    }
                }

                // Bind GSLB vserver to domain
                GSLB.createVserverDomainBinding(_netscalerService, vserverName, domainName);

            } else {  // global load balancer rule is being deleted, so clean up all objects created

                // remove binding between virtual server and the domain name
                GSLB.deleteVserverDomainBinding(_netscalerService, vserverName, domainName);

                if (sites != null) {
                    for (final SiteLoadBalancerConfig site : sites) {

                        final String sitePrivateIP = site.getGslbProviderPrivateIp();
                        final String sitePublicIP = site.getGslbProviderPublicIp();
                        final String servicePublicIp = site.getServicePublicIp();
                        final String servicePublicPort = site.getServicePort();
                        final String siteName = GSLB.generateUniqueSiteName(sitePrivateIP, sitePublicIP, site.getDataCenterId());
                        final String serviceName = GSLB.generateUniqueServiceName(siteName, servicePublicIp, servicePublicPort);
                        final String monitorName = GSLB.generateGslbServiceMonitorName(servicePublicIp);

                        // delete GSLB service and GSLB monitor binding
                        GSLB.deleteGslbServiceGslbMonitorBinding(_netscalerService, monitorName, serviceName);

                        // remove binding between virtual server and services
                        GSLB.deleteVserverServiceBinding(_netscalerService, serviceName, vserverName);

                        // delete service object
                        GSLB.deleteService(_netscalerService, serviceName);

                        // delete GSLB site object
                        GSLB.deleteSite(_netscalerService, siteName);

                        // delete the GSLB service monitor
                        GSLB.deleteGslbServiceMonitor(_netscalerService, monitorName);
                    }
                }

                // delete GSLB virtual server
                GSLB.deleteVirtualServer(_netscalerService, vserverName);
            }

            saveConfiguration();

        } catch (final Exception e) {
            final String errMsg = "Failed to apply GSLB configuration due to " + e.getMessage();
            if (shouldRetry(numRetries)) {
                return retry(gslbCmd, numRetries);
            }
            return new GlobalLoadBalancerConfigAnswer(false, errMsg);
        }

        return new GlobalLoadBalancerConfigAnswer(true, "Successfully applied GSLB configuration.");
    }

    /*
     * convenience class to create/update/delete/get the GSLB specific NetScaler objects
     *     - gslbsite
     *     - gslbvserver
     *     - gslbservice
     *     - vserver-service binding
     *     - vserver-domain bindings
     */
    private static class GSLB {

        // create a 'gslbsite' object representing a site
        private static void createSite(final nitro_service client, final String siteName, final String siteType, final String siteIP, final String sitePublicIP) throws ExecutionException {
            try {
                gslbsite site;
                site = getSiteObject(client, siteName);

                boolean isUpdateSite = false;
                if (site == null) {
                    site = new gslbsite();
                } else {
                    isUpdateSite = true;
                }

                assert "LOCAL".equalsIgnoreCase(siteType) || "REMOTE".equalsIgnoreCase(siteType);
                site.set_sitetype(siteType);
                site.set_sitename(siteName);
                site.set_siteipaddress(siteIP);
                site.set_publicip(sitePublicIP);
                site.set_metricexchange("ENABLED");
                site.set_nwmetricexchange("ENABLED");
                site.set_sessionexchange("ENABLED");
                if (isUpdateSite) {
                    gslbsite.update(client, site);
                } else {
                    gslbsite.add(client, site);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully created GSLB site: " + siteName);
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to create GSLB site: " + siteName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // delete 'gslbsite' object representing a site
        private static void deleteSite(final nitro_service client, final String siteName) throws ExecutionException {
            try {
                final gslbsite site = getSiteObject(client, siteName);
                if (site != null) {
                    final gslbsite_gslbservice_binding[] serviceBindings = gslbsite_gslbservice_binding.get(client, siteName);
                    if (serviceBindings != null && serviceBindings.length > 0) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("There are services associated with GSLB site: " + siteName + " so ignoring site deletion");
                        }
                    }
                    gslbsite.delete(client, siteName);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully deleted GSLB site: " + siteName);
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.warn("Ignoring delete request for non existing  GSLB site: " + siteName);
                    }
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to delete GSLB site: " + siteName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // update 'gslbsite' object representing a site
        private static void updateSite(final nitro_service client, final String siteType, final String siteName, final String siteIP, final String sitePublicIP) throws ExecutionException {
            try {
                gslbsite site;
                site = getSiteObject(client, siteName);
                if (site == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.warn("Ignoring update request for non existing  GSLB site: " + siteName);
                    }
                    return;
                }
                assert "LOCAL".equalsIgnoreCase(siteType) || "REMOTE".equalsIgnoreCase(siteType);
                site.set_sitetype(siteType);
                site.set_sitename(siteName);
                site.set_siteipaddress(siteIP);
                site.set_publicip(sitePublicIP);
                site.set_metricexchange("ENABLED");
                site.set_nwmetricexchange("ENABLED");
                site.set_sessionexchange("ENABLED");
                gslbsite.update(client, site);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully updated GSLB site: " + siteName);
                }

            } catch (final Exception e) {
                final String errMsg = "Failed to update GSLB site: " + siteName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // create a 'gslbvserver' object representing a globally load balanced service
        private static void
        createVirtualServer(final nitro_service client, final String vserverName, final String lbMethod, final String persistenceType, final long persistenceId, final String serviceType)
                throws ExecutionException {
            try {
                gslbvserver vserver;
                vserver = getVserverObject(client, vserverName);

                boolean isUpdateSite = false;
                if (vserver == null) {
                    vserver = new gslbvserver();
                } else {
                    isUpdateSite = true;
                }

                vserver.set_name(vserverName);
                if ("RoundRobin".equalsIgnoreCase(lbMethod)) {
                    vserver.set_lbmethod("ROUNDROBIN");
                } else if ("LeastConn".equalsIgnoreCase(lbMethod)) {
                    vserver.set_lbmethod("LEASTCONNECTION");
                } else if ("Proximity".equalsIgnoreCase(lbMethod)) {
                    vserver.set_lbmethod("RTT");
                } else {
                    throw new ExecutionException("Unsupported LB method");
                }
                vserver.set_persistencetype(persistenceType);
                if ("SOURCEIP".equalsIgnoreCase(persistenceType)) {
                    vserver.set_persistenceid(persistenceId);
                }
                vserver.set_servicetype(serviceType);
                vserver.set_state("ENABLED");
                vserver.set_cookietimeout(null);
                vserver.set_domainname(null);
                if (isUpdateSite) {
                    // both netmask and LB method can not be specified while update so set to null
                    vserver.set_netmask(null);
                    vserver.set_v6netmasklen(null);
                    gslbvserver.update(client, vserver);
                } else {
                    gslbvserver.add(client, vserver);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully added GSLB virtual server: " + vserverName);
                }

            } catch (final Exception e) {
                final String errMsg = "Failed to add GSLB virtual server: " + vserverName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // delete 'gslbvserver' object representing a globally load balanced service
        private static void deleteVirtualServer(final nitro_service client, final String vserverName) throws ExecutionException {
            try {
                final gslbvserver vserver = getVserverObject(client, vserverName);
                if (vserver != null) {
                    gslbvserver.delete(client, vserver);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully deleted GSLB virtual server: " + vserverName);
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.warn("Ignoring delete request for non existing  GSLB virtual server: " + vserverName);
                    }
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to delete GSLB virtual server: " + vserverName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // create, delete, update, get the GSLB services
        private static void createService(final nitro_service client, final String serviceName, final String serviceType, final String serviceIp, final String servicePort, final String siteName)
                throws ExecutionException {
            try {
                gslbservice service;
                service = getServiceObject(client, serviceName);
                final String gslbServerName = generateGslbServerName(serviceIp);

                if (!gslbServerExists(client, gslbServerName)) {
                    base_response apiCallResult;
                    final com.citrix.netscaler.nitro.resource.config.basic.server nsServer = new com.citrix.netscaler.nitro.resource.config.basic.server();
                    nsServer.set_name(gslbServerName);
                    nsServer.set_ipaddress(serviceIp);
                    apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.add(client, nsServer);
                    if (apiCallResult.errorcode != 0 && apiCallResult.errorcode != NitroError.NS_RESOURCE_EXISTS) {
                        throw new ExecutionException("Failed to add server " + gslbServerName + " due to" + apiCallResult.message);
                    }
                }

                boolean isUpdateSite = false;
                if (service == null) {
                    service = new gslbservice();
                } else {
                    isUpdateSite = true;
                }

                service.set_sitename(siteName);
                service.set_servername(gslbServerName);
                final int port = Integer.parseInt(servicePort);
                service.set_port(port);
                service.set_servicename(serviceName);
                service.set_servicetype(serviceType);
                if (isUpdateSite) {
                    service.set_viewip(null);
                    service.set_viewname(null);
                    gslbservice.update(client, service);
                } else {
                    gslbservice.add(client, service);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully created service: " + serviceName + " at site: " + siteName);
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to created service: " + serviceName + " at site: " + siteName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        private static void deleteService(final nitro_service client, final String serviceName) throws ExecutionException {
            try {
                final gslbservice service = getServiceObject(client, serviceName);
                if (service != null) {
                    gslbservice.delete(client, serviceName);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully deleted service: " + serviceName);
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.warn("Ignoring delete request for non existing  service: " + serviceName);
                    }
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to delete service: " + serviceName + " due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        private static void createVserverServiceBinding(final nitro_service client, final String serviceName, final String vserverName, final long weight) throws ExecutionException {
            String errMsg;
            try {
                assert weight >= 1 && weight <= 100;
                final gslbvserver_gslbservice_binding binding = new gslbvserver_gslbservice_binding();
                binding.set_name(vserverName);
                binding.set_servicename(serviceName);
                binding.set_weight(weight);
                gslbvserver_gslbservice_binding.add(client, binding);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully created service: " + serviceName + " and virtual server: " + vserverName + " binding");
                }
            } catch (final nitro_exception ne) {
                if (ne.getErrorCode() == 273) {
                    return;
                }
                errMsg = "Failed to create service: " + serviceName + " and virtual server: " + vserverName + " binding due to " + ne.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            } catch (final Exception e) {
                errMsg = "Failed to create service: " + serviceName + " and virtual server: " + vserverName + " binding due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        private static void deleteVserverServiceBinding(final nitro_service client, final String serviceName, final String vserverName) throws ExecutionException {
            try {
                final gslbvserver_gslbservice_binding[] bindings = gslbvserver_gslbservice_binding.get(client, vserverName);
                if (bindings != null) {
                    for (final gslbvserver_gslbservice_binding binding : bindings) {
                        if (binding.get_servicename().equalsIgnoreCase(serviceName) && binding.get_name().equals(vserverName)) {
                            gslbvserver_gslbservice_binding.delete(client, binding);
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Successfully deleted service: " + serviceName + " and virtual server: " + vserverName + " binding");
                            }
                            break;
                        }
                    }
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to create service: " + serviceName + " and virtual server: " + vserverName + " binding due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        // create, delete GSLB virtual server and domain bindings
        private static void createVserverDomainBinding(final nitro_service client, final String vserverName, final String domainName) throws ExecutionException {
            String errMsg;
            try {
                final gslbvserver_domain_binding binding = new gslbvserver_domain_binding();
                binding.set_domainname(domainName);
                binding.set_name(vserverName);
                gslbvserver_domain_binding.add(client, binding);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully added virtual server: " + vserverName + " domain name: " + domainName + " binding");
                }
                return;
            } catch (final nitro_exception e) {
                if (e.getErrorCode() == NitroError.NS_GSLB_DOMAIN_ALREADY_BOUND) {
                    return;
                }
                errMsg = e.getMessage();
            } catch (final Exception e) {
                errMsg = e.getMessage();
            }
            errMsg = "Failed to create virtual server: " + vserverName + " domain name: " + domainName + " binding" + errMsg;
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(errMsg);
            }
            throw new ExecutionException(errMsg);
        }

        private static void deleteVserverDomainBinding(final nitro_service client, final String vserverName, final String domainName) throws ExecutionException {
            try {
                final gslbvserver_domain_binding[] bindings = gslbvserver_domain_binding.get(client, vserverName);
                if (bindings != null) {
                    for (final gslbvserver_domain_binding binding : bindings) {
                        if (binding.get_domainname().equalsIgnoreCase(domainName)) {
                            gslbvserver_domain_binding.delete(client, binding);
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Successfully deleted virtual server: " + vserverName + " and " + " domain: " + domainName + " binding");
                            }
                            break;
                        }
                    }
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to delete virtual server: " + vserverName + " and domain " + domainName + " binding due to " + e.getMessage();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        private static void createGslbServiceMonitor(final nitro_service nsService, final String servicePublicIp, final String serviceName) throws ExecutionException {
            try {
                final lbmonitor newmonitor = new lbmonitor();
                final String monitorName = generateGslbServiceMonitorName(servicePublicIp);
                newmonitor.set_type("TCP");
                newmonitor.set_servicename(serviceName);
                newmonitor.set_monitorname(monitorName);
                newmonitor.set_state("ENABLED");
                lbmonitor.add(nsService, newmonitor);
            } catch (final nitro_exception ne) {
                if (ne.getErrorCode() == NitroError.NS_RESOURCE_EXISTS) {
                    return;
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to create GSLB monitor for service public ip" + servicePublicIp;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(errMsg);
                }
                throw new ExecutionException(errMsg);
            }
        }

        private static void deleteGslbServiceMonitor(final nitro_service nsService, final String monitorName) throws ExecutionException {
            try {
                final lbmonitor serviceMonitor = lbmonitor.get(nsService, monitorName);
                if (serviceMonitor != null) {
                    lbmonitor.delete(nsService, serviceMonitor);
                }
            } catch (final nitro_exception ne) {
                if (ne.getErrorCode() != NitroError.NS_RESOURCE_NOT_EXISTS) {
                    final String errMsg = "Failed to delete monitor " + monitorName + " for GSLB service due to " + ne.getMessage();
                    s_logger.debug(errMsg);
                    throw new com.cloud.utils.exception.ExecutionException(errMsg);
                }
            } catch (final Exception e) {
                final String errMsg = "Failed to delete monitor " + monitorName + " for GSLB service due to " + e.getMessage();
                s_logger.debug(errMsg);
                throw new com.cloud.utils.exception.ExecutionException(errMsg);
            }
        }

        private static void createGslbServiceGslbMonitorBinding(final nitro_service nsService, final String monitorName, final String serviceName) {
            try {
                final gslbservice_lbmonitor_binding monitorBinding = new gslbservice_lbmonitor_binding();
                monitorBinding.set_monitor_name(monitorName);
                monitorBinding.set_servicename(serviceName);
                gslbservice_lbmonitor_binding.add(nsService, monitorBinding);
            } catch (final Exception e) {
                // TODO: Nitro API version 10.* is not compatible for NetScalers 9.*, so may fail
                // against NetScaler version lesser than 10 hence ignore the exception
                s_logger.warn("Failed to bind monitor to GSLB service due to " + e.getMessage());
            }
        }

        private static void deleteGslbServiceGslbMonitorBinding(final nitro_service nsService, final String monitorName, final String serviceName) {
            try {
                final gslbservice_lbmonitor_binding[] monitorBindings = gslbservice_lbmonitor_binding.get(nsService, serviceName);
                if (monitorBindings != null && monitorBindings.length > 0) {
                    for (final gslbservice_lbmonitor_binding binding : monitorBindings) {
                        if (binding.get_monitor_name().equalsIgnoreCase(monitorName)) {
                            s_logger.info("Found a binding between monitor " + binding.get_monitor_name() + " and " + binding.get_servicename());
                            gslbservice_lbmonitor_binding.delete(nsService, binding);
                        }
                    }
                }
            } catch (final Exception e) {
                s_logger.debug("Failed to delete GSLB monitor " + monitorName + " and GSLB service " + serviceName + " binding due to " + e.getMessage() +
                        " but moving on ..., will be cleaned up as part of GSLB " + " service delete any way..");
            }
        }

        // get 'gslbsite' object corresponding to a site name
        private static gslbsite getSiteObject(final nitro_service client, final String siteName) {
            try {
                final gslbsite site = gslbsite.get(client, siteName);
                if (site != null) {
                    return site;
                }
            } catch (final Exception e) {
                s_logger.info("[ignored]"
                        + "error getting site: " + e.getLocalizedMessage());
            }
            return null;
        }

        private static gslbvserver getVserverObject(final nitro_service client, final String vserverName) {
            try {
                final gslbvserver vserver = gslbvserver.get(client, vserverName);
                return vserver;
            } catch (final Exception e) {
                return null;
            }
        }

        private static gslbservice getServiceObject(final nitro_service client, final String serviceName) {
            try {
                final gslbservice service = gslbservice.get(client, serviceName);
                return service;
            } catch (final Exception e) {
                return null;
            }
        }

        private static String generateUniqueSiteName(final String sitePrivateIp, final String sitePublicIP, final long dataCenterId) {
            return "cloudsite" + String.valueOf(dataCenterId);
        }

        private static String generateVirtualServerName(final String domainName) {
            return "cloud-gslb-vserver-" + domainName;
        }

        private static String generateUniqueServiceName(final String siteName, final String publicIp, final String publicPort) {
            return "cloud-gslb-service-" + siteName + "-" + publicIp + "-" + publicPort;
        }

        private static String generateGslbServiceMonitorName(final String publicIp) {
            return "cloud-monitor-" + publicIp;
        }

        private static boolean gslbServerExists(final nitro_service client, final String serverName) throws ExecutionException {
            try {
                if (com.citrix.netscaler.nitro.resource.config.basic.server.get(client, serverName) != null) {
                    return true;
                } else {
                    return false;
                }
            } catch (final nitro_exception e) {
                if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                    return false;
                } else {
                    throw new ExecutionException("Failed to verify Server " + serverName + " exists on the NetScaler device due to " + e.getMessage());
                }
            } catch (final Exception e) {
                throw new ExecutionException("Failed to verify Server " + serverName + " exists on the NetScaler device due to " + e.getMessage());
            }
        }

        private static String generateGslbServerName(final String serverIP) {
            return genGslbObjectName("Cloud-Server-", serverIP);
        }

        private static String genGslbObjectName(final Object... args) {
            final StringBuffer buff = new StringBuffer();
            for (int i = 0; i < args.length; i++) {
                buff.append(args[i]);
                if (i != args.length - 1) {
                    buff.append("-");
                }
            }
            return buff.toString();
        }
    }

    /* SSL Termination */
    private static class SSL {

        private static final String SSL_CERT_PATH = "/nsconfig/ssl/";
        private static final int SSH_PORT = 22;

        private static boolean isSslCertKeyPresent(final nitro_service ns, final String certKeyName) throws ExecutionException {

            final String filter = "certkey:" + certKeyName;

            try {
                if (sslcertkey.count_filtered(ns, filter) > 0) {
                    return true;
                }
            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to get certkey " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to get certkey " + e.getMessage());
            }

            return false;
        }

        private static void deleteSslCertKey(final nitro_service ns, final String certKeyName) throws ExecutionException {
            try {

                final sslcertkey certkey = new sslcertkey();
                certkey.set_certkey(certKeyName);
                sslcertkey.delete(ns, certkey);

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to delete certkey " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to delete certkey " + e.getMessage());
            }

        }

        private static void deleteCertFile(final String nsIp, final String username, final String password, final String certFilename) throws Exception {
            SshHelper.sshExecute(nsIp, SSH_PORT, username, null, password, "shell rm " + SSL_CERT_PATH + certFilename);
        }

        private static void deleteKeyFile(final String nsIp, final String username, final String password, final String keyFilename) throws Exception {
            SshHelper.sshExecute(nsIp, SSH_PORT, username, null, password, "shell rm " + SSL_CERT_PATH + keyFilename);
        }

        private static void createSslCertKey(final nitro_service ns, final String certFilename, final String keyFilename, final String certKeyName, final String password) throws ExecutionException {
            s_logger.debug("Adding cert to netscaler");
            try {
                final sslcertkey certkey = new sslcertkey();
                certkey.set_certkey(certKeyName);
                certkey.set_cert(SSL_CERT_PATH + certFilename);

                if (keyFilename != null) {
                    certkey.set_key(SSL_CERT_PATH + keyFilename);
                }

                if (password != null) {
                    certkey.set_passplain(password);
                }

                certkey.perform_operation(ns);

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to add certkey binding " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to add certkey binding " + e.getMessage());
            }

        }

        private static void bindCertKeyToVserver(final nitro_service ns, final String certKeyName, final String vserver) throws ExecutionException {
            s_logger.debug("Adding cert to netscaler");

            try {
                final sslvserver_sslcertkey_binding cert_binding = new sslvserver_sslcertkey_binding();
                cert_binding.set_certkeyname(certKeyName);
                cert_binding.set_vservername(vserver);
                cert_binding.perform_operation(ns);
            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to bind certkey to vserver due to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to bind certkey to vserver due to " + e.getMessage());
            }
        }

        private static void unbindCertKeyFromVserver(final nitro_service ns, final String certKeyName, final String vserver) throws ExecutionException {
            try {

                final sslvserver_sslcertkey_binding cert_binding = new sslvserver_sslcertkey_binding();
                cert_binding.set_certkeyname(certKeyName);
                cert_binding.set_vservername(vserver);
                sslvserver_sslcertkey_binding.delete(ns, cert_binding);

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to unbind certkey to vserver due to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to unbind certkey to vserver due to " + e.getMessage());
            }

        }

        private static void uploadCert(final String nsIp, final String user, final String password, final String certFilename, final byte[] certData) throws ExecutionException {
            try {
                SshHelper.scpTo(nsIp, SSH_PORT, user, null, password, SSL_CERT_PATH, certData, certFilename, null);
            } catch (final Exception e) {
                throw new ExecutionException("Failed to copy private key to device " + e.getMessage());
            }
        }

        private static void uploadKey(final String nsIp, final String user, final String password, final String keyFilename, final byte[] keyData) throws ExecutionException {
            try {
                SshHelper.scpTo(nsIp, SSH_PORT, user, null, password, SSL_CERT_PATH, keyData, keyFilename, null);
            } catch (final Exception e) {
                throw new ExecutionException("Failed to copy private key to device " + e.getMessage());
            }
        }

        private static void enableSslFeature(final nitro_service ns, final boolean isSdx) throws ExecutionException {
            if (isSdx) {
                return;
            }
            try {
                final base_response result = ns.enable_features(new String[] {"SSL"});
                if (result.errorcode != 0) {
                    throw new ExecutionException("Unable to enable SSL on LB");
                }
            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to enable ssl feature on load balancer due to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to enable ssl feature on load balancer due to " + e.getMessage());
            }
        }

        public static boolean certLinkExists(final nitro_service ns, final String userCertName, final String caCertName) throws ExecutionException {
            try {
                // check if there is a link from userCertName to caCertName

                final sslcertkey userCert = sslcertkey.get(ns, userCertName);
                final String nsCaCert = userCert.get_linkcertkeyname();

                if (nsCaCert != null && nsCaCert.equals(caCertName)) {
                    return true;
                }

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer due to " + e.getMessage());
            }
            return false;
        }

        public static void linkCerts(final nitro_service ns, final String userCertName, final String caCertName) throws ExecutionException {
            try {

                // the assumption is that that both userCertName and caCertName are present on NS

                final sslcertkey caCert = sslcertkey.get(ns, caCertName);
                final sslcertkey userCert = sslcertkey.get(ns, userCertName);

                final sslcertkey linkResource = new sslcertkey();

                // link user cert to CA cert
                linkResource.set_certkey(userCert.get_certkey());
                linkResource.set_linkcertkeyname(caCert.get_certkey());
                sslcertkey.link(ns, linkResource);

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer due to " + e.getMessage());
            }

        }

        public static boolean isCaforCerts(final nitro_service ns, final String caCertName) throws ExecutionException {
            // check if this certificate  serves as a CA for other certificates
            try {
                final sslcertlink[] childLinks = sslcertlink.get_filtered(ns, "linkcertkeyname:" + caCertName);
                if (childLinks != null && childLinks.length > 0) {
                    return true;
                }

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer due to " + e.getMessage());
            }
            return false;

        }

        public static boolean isBoundToVserver(final nitro_service ns, final String certKeyName, final String nsVirtualServerName) throws ExecutionException {
            try {

                final sslcertkey_sslvserver_binding[] cert_vs_binding = sslcertkey_sslvserver_binding.get_filtered(ns, certKeyName, "vservername:" + nsVirtualServerName);
                if (cert_vs_binding != null && cert_vs_binding.length > 0) {
                    return true;
                }

            } catch (final nitro_exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer to " + e.getMessage());
            } catch (final Exception e) {
                throw new ExecutionException("Failed to check cert link on load balancer due to " + e.getMessage());
            }
            return false;

        }
    }

    private void enableVPXInterfaces(final String publicIf, final String privateIf, final ns nsObj) {
        // enable VPX to use 10 gigabit Ethernet interfaces if public/private interface
        // on SDX is a 10Gig interface
        if (publicIf.equals("10/1") || privateIf.equals("10/1")) {
            nsObj.set_if_10_1(new Boolean(true));
        }

        if (publicIf.equals("10/2") || privateIf.equals("10/2")) {
            nsObj.set_if_10_2(new Boolean(true));
        }

        if (publicIf.equals("10/3") || privateIf.equals("10/3")) {
            nsObj.set_if_10_3(new Boolean(true));
        }

        if (publicIf.equals("10/4") || privateIf.equals("10/4")) {
            nsObj.set_if_10_4(new Boolean(true));
        }

        if (publicIf.equals("10/5") || privateIf.equals("10/5")) {
            nsObj.set_if_10_5(new Boolean(true));
        }

        if (publicIf.equals("10/6") || privateIf.equals("10/6")) {
            nsObj.set_if_10_6(new Boolean(true));
        }

        if (publicIf.equals("10/7") || privateIf.equals("10/7")) {
            nsObj.set_if_10_7(new Boolean(true));
        }

        if (publicIf.equals("10/8") || privateIf.equals("10/8")) {
            nsObj.set_if_10_8(new Boolean(true));
        }

        // enable VPX to use 1 gigabit Ethernet interfaces if public/private interface
        // on SDX is a 1Gig interface
        if (publicIf.equals("1/1") || privateIf.equals("1/1")) {
            nsObj.set_if_1_1(new Boolean(true));
        }

        if (publicIf.equals("1/2") || privateIf.equals("1/2")) {
            nsObj.set_if_1_2(new Boolean(true));
        }

        if (publicIf.equals("1/3") || privateIf.equals("1/3")) {
            nsObj.set_if_1_3(new Boolean(true));
        }

        if (publicIf.equals("1/4") || privateIf.equals("1/4")) {
            nsObj.set_if_1_4(new Boolean(true));
        }

        if (publicIf.equals("1/5") || privateIf.equals("1/5")) {
            nsObj.set_if_1_5(new Boolean(true));
        }

        if (publicIf.equals("1/6") || privateIf.equals("1/6")) {
            nsObj.set_if_1_6(new Boolean(true));
        }

        if (publicIf.equals("1/7") || privateIf.equals("1/7")) {
            nsObj.set_if_1_7(new Boolean(true));
        }

        if (publicIf.equals("1/8") || privateIf.equals("1/8")) {
            nsObj.set_if_1_8(new Boolean(true));
        }
    }

    private synchronized Answer execute(final DestroyLoadBalancerApplianceCommand cmd, final int numRetries) {
        final String vpxName = "Cloud-VPX-" + cmd.getLoadBalancerIP();
        if (!_isSdx) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }

        try {
            ns vpxToDelete = null;
            final ns[] vpxInstances = ns.get(_netscalerSdxService);
            for (final ns vpx : vpxInstances) {
                if (vpx.get_name().equals(vpxName)) {
                    vpxToDelete = vpx;
                    break;
                }
            }

            if (vpxToDelete == null) {
                final String msg = "There is no VPX instance " + vpxName + " on the Netscaler SDX device " + _ip + " to delete";
                s_logger.warn(msg);
                return new DestroyLoadBalancerApplianceAnswer(cmd, true, msg);
            }

            // destroy the VPX instance
            final ns nsDelObj = new ns();
            nsDelObj.set_id(vpxToDelete.get_id());
            vpxToDelete = ns.delete(_netscalerSdxService, nsDelObj);
            final String msg = "Deleted VPX instance " + vpxName + " on Netscaler SDX " + _ip + " successfully.";
            s_logger.info(msg);
            return new DestroyLoadBalancerApplianceAnswer(cmd, true, msg);
        } catch (final Exception e) {
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            }
            return new DestroyLoadBalancerApplianceAnswer(cmd, false, "Failed to delete VPX instance " + vpxName + " on Netscaler SDX " + _ip);
        }
    }

    private synchronized Answer execute(final SetStaticNatRulesCommand cmd, final int numRetries) {

        if (_isSdx) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }

        final String[] results = new String[cmd.getRules().length];
        int i = 0;
        boolean endResult = true;

        try {
            for (final StaticNatRuleTO rule : cmd.getRules()) {
                final String srcIp = rule.getSrcIp();
                final String dstIP = rule.getDstIp();
                final String iNatRuleName = generateInatRuleName(srcIp, dstIP);
                final String rNatRuleName = generateRnatRuleName(srcIp, dstIP);
                inat iNatRule = null;
                rnat rnatRule = null;

                if (!rule.revoked()) {
                    try {
                        iNatRule = inat.get(_netscalerService, iNatRuleName);
                    } catch (final nitro_exception e) {
                        if (e.getErrorCode() != NitroError.NS_RESOURCE_NOT_EXISTS) {
                            throw e;
                        }
                    }

                    if (iNatRule == null) {
                        iNatRule = new inat();
                        iNatRule.set_name(iNatRuleName);
                        iNatRule.set_publicip(srcIp);
                        iNatRule.set_privateip(dstIP);
                        iNatRule.set_usnip("OFF");
                        iNatRule.set_usip("ON");
                        try {
                            apiCallResult = inat.add(_netscalerService, iNatRule);
                        } catch (final nitro_exception e) {
                            if (e.getErrorCode() != NitroError.NS_RESOURCE_EXISTS) {
                                throw e;
                            }
                        }
                        s_logger.debug("Created Inat rule on the Netscaler device " + _ip + " to enable static NAT from " + srcIp + " to " + dstIP);
                    }
                    try {
                        final rnat[] rnatRules = rnat.get(_netscalerService);
                        if (rnatRules != null) {
                            for (final rnat rantrule : rnatRules) {
                                if (rantrule.get_network().equalsIgnoreCase(rNatRuleName)) {
                                    rnatRule = rantrule;
                                    break;
                                }
                            }
                        }
                    } catch (final nitro_exception e) {
                        throw e;
                    }

                    if (rnatRule == null) {
                        rnatRule = new rnat();
                        rnatRule.set_natip(srcIp);
                        rnatRule.set_network(dstIP);
                        rnatRule.set_netmask("255.255.255.255");
                        try {
                            apiCallResult = rnat.update(_netscalerService, rnatRule);
                        } catch (final nitro_exception e) {
                            if (e.getErrorCode() != NitroError.NS_RESOURCE_EXISTS) {
                                throw e;
                            }
                        }
                        s_logger.debug("Created Rnat rule on the Netscaler device " + _ip + " to enable revese static NAT from " + dstIP + " to " + srcIp);
                    }
                } else {
                    try {
                        inat.delete(_netscalerService, iNatRuleName);
                        final rnat[] rnatRules = rnat.get(_netscalerService);
                        if (rnatRules != null) {
                            for (final rnat rantrule : rnatRules) {
                                if (rantrule.get_network().equalsIgnoreCase(dstIP)) {
                                    rnatRule = rantrule;
                                    rnat.clear(_netscalerService, rnatRule);
                                    break;
                                }
                            }
                        }
                    } catch (final nitro_exception e) {
                        if (e.getErrorCode() != NitroError.NS_RESOURCE_NOT_EXISTS) {
                            throw e;
                        }
                    }
                    s_logger.debug("Deleted Inat rule on the Netscaler device " + _ip + " to remove static NAT from " + srcIp + " to " + dstIP);
                }

                saveConfiguration();
                results[i++] = "Static nat rule from " + srcIp + " to " + dstIP + " successfully " + (rule.revoked() ? " revoked." : " created.");
            }
        } catch (final Exception e) {
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            }
            results[i++] = "Configuring static nat rule failed due to " + e.getMessage();
            endResult = false;
            return new SetStaticNatRulesAnswer(cmd, results, endResult);
        }

        return new SetStaticNatRulesAnswer(cmd, results, endResult);
    }

    private synchronized Answer execute(final ExternalNetworkResourceUsageCommand cmd, final int numRetries) {
        try {
            if (!_isSdx) {
                return getPublicIpBytesSentAndReceived(cmd);
            } else {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final ExecutionException e) {
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new ExternalNetworkResourceUsageAnswer(cmd, e);
            }
        }
    }

    private void addSubnetIP(final String snip, final String netmask) throws ExecutionException {
        try {
            final nsip selfIp = new nsip();
            selfIp.set_ipaddress(snip);
            selfIp.set_netmask(netmask);
            selfIp.set_type("SNIP");
            apiCallResult = nsip.add(_netscalerService, selfIp);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Failed to add SNIP object on the Netscaler device due to " + apiCallResult.message);
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to add SNIP object on the Netscaler device due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to add SNIP object on the Netscaler device due to " + e.getMessage());
        }
    }

    private void addGuestVlanAndSubnet(final long vlanTag, final String vlanSelfIp, final String vlanNetmask, final boolean guestVlan) throws ExecutionException {
        try {
            // add vlan object for guest VLAN
            if (!nsVlanExists(vlanTag)) {
                try {
                    final vlan vlanObj = new vlan();
                    vlanObj.set_id(vlanTag);
                    apiCallResult = vlan.add(_netscalerService, vlanObj);
                    if (apiCallResult.errorcode != 0) {
                        throw new ExecutionException("Failed to add new vlan with tag:" + vlanTag + "on the NetScaler device due to " + apiCallResult.message);
                    }
                } catch (final nitro_exception e) {
                    throw new ExecutionException("Failed to add new vlan with tag:" + vlanTag + "on the NetScaler device due to " + e.getMessage());
                }
            }

            // add subnet IP object for this guest network
            if (!nsSnipExists(vlanSelfIp)) {
                try {
                    final nsip selfIp = new nsip();
                    selfIp.set_ipaddress(vlanSelfIp);
                    selfIp.set_netmask(vlanNetmask);
                    selfIp.set_type("SNIP");
                    apiCallResult = nsip.add(_netscalerService, selfIp);
                    if (apiCallResult.errorcode != 0) {
                        throw new ExecutionException("Failed to add SNIP object for the guest network on the Netscaler device due to " + apiCallResult.message);
                    }
                } catch (final nitro_exception e) {
                    throw new ExecutionException("Failed to add SNIP object for the guest network on the Netscaler device due to " + e.getMessage());
                }
            }

            // bind the vlan object to subnet IP object
            if (!nsVlanNsipBindingExists(vlanTag, vlanSelfIp)) {
                try {
                    final vlan_nsip_binding ipVlanBinding = new vlan_nsip_binding();
                    ipVlanBinding.set_id(vlanTag);
                    ipVlanBinding.set_ipaddress(vlanSelfIp);
                    ipVlanBinding.set_netmask(vlanNetmask);
                    apiCallResult = vlan_nsip_binding.add(_netscalerService, ipVlanBinding);
                    if (apiCallResult.errorcode != 0) {
                        throw new ExecutionException("Failed to bind VLAN with tag:" + vlanTag + " to the subnet due to " + apiCallResult.message);
                    }
                } catch (final nitro_exception e) {
                    throw new ExecutionException("Failed to bind VLAN with tage:" + vlanTag + " to the subnet due to " + e.getMessage());
                }
            }

            // bind vlan object to the private interface
            try {
                final vlan_interface_binding vlanBinding = new vlan_interface_binding();
                if (guestVlan) {
                    vlanBinding.set_ifnum(_privateInterface);
                } else {
                    vlanBinding.set_ifnum(_publicInterface);
                }
                vlanBinding.set_tagged(true);
                vlanBinding.set_id(vlanTag);
                apiCallResult = vlan_interface_binding.add(_netscalerService, vlanBinding);
                if (apiCallResult.errorcode != 0) {
                    final String vlanInterface = guestVlan ? _privateInterface : _publicInterface;
                    throw new ExecutionException("Failed to bind vlan with tag:" + vlanTag + " with the interface " + vlanInterface + " due to " + apiCallResult.message);
                }
            } catch (final nitro_exception e) {
                if (!(e.getErrorCode() == NitroError.NS_INTERFACE_ALREADY_BOUND_TO_VLAN)) {
                    throw new ExecutionException("Failed to bind VLAN " + vlanTag + " with interface on the Netscaler device due to " + e.getMessage());
                }
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to implement guest network on the Netscaler device due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to implement guest network on the Netscaler device due to " + e.getMessage());
        }
    }

    private void deleteGuestVlan(final long vlanTag, final String vlanSelfIp, final String vlanNetmask) throws ExecutionException {
        try {

            // Delete all servers and associated services from this guest VLAN
            deleteServersInGuestVlan(vlanTag, vlanSelfIp, vlanNetmask);

            // unbind vlan to the private interface
            try {
                final vlan_interface_binding vlanIfBinding = new vlan_interface_binding();
                vlanIfBinding.set_id(vlanTag);
                vlanIfBinding.set_ifnum(_privateInterface);
                vlanIfBinding.set_tagged(true);
                apiCallResult = vlan_interface_binding.delete(_netscalerService, vlanIfBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to unbind vlan:" + vlanTag + " with the private interface due to " + apiCallResult.message);
                }
            } catch (final nitro_exception e) {
                // if Vlan to interface binding does not exist then ignore the exception and proceed
                if (!(e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS)) {
                    throw new ExecutionException("Failed to unbind vlan from the interface while shutdown of guest network on the Netscaler device due to " +
                            e.getMessage());
                }
            }

            // unbind the vlan to subnet
            try {
                final vlan_nsip_binding vlanSnipBinding = new vlan_nsip_binding();
                vlanSnipBinding.set_netmask(vlanNetmask);
                vlanSnipBinding.set_ipaddress(vlanSelfIp);
                vlanSnipBinding.set_id(vlanTag);
                apiCallResult = vlan_nsip_binding.delete(_netscalerService, vlanSnipBinding);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to unbind vlan:" + vlanTag + " with the subnet due to " + apiCallResult.message);
                }
            } catch (final nitro_exception e) {
                // if Vlan to subnet binding does not exist then ignore the exception and proceed
                if (!(e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS)) {
                    throw new ExecutionException("Failed to unbind vlan:" + vlanTag + " with the subnet due to " + e.getMessage());
                }
            }

            // remove subnet IP
            try {
                final nsip tmpVlanSelfIp = new nsip();
                tmpVlanSelfIp.set_ipaddress(vlanSelfIp);

                final nsip subnetIp = nsip.get(_netscalerService, tmpVlanSelfIp);
                apiCallResult = nsip.delete(_netscalerService, subnetIp);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to remove subnet ip:" + vlanSelfIp + " from the NetScaler device due to" + apiCallResult.message);
                }
            } catch (final nitro_exception e) {
                // if subnet SNIP does not exist then ignore the exception and proceed
                if (!(e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS)) {
                    throw new ExecutionException("Failed to remove subnet ip:" + vlanSelfIp + " from the NetScaler device due to" + e.getMessage());
                }
            }

            // remove the vlan from the NetScaler device
            if (nsVlanExists(vlanTag)) {
                // remove vlan
                apiCallResult = com.citrix.netscaler.nitro.resource.config.network.vlan.delete(_netscalerService, vlanTag);
                if (apiCallResult.errorcode != 0) {
                    throw new ExecutionException("Failed to remove vlan with tag:" + vlanTag + "due to" + apiCallResult.message);
                }
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to delete guest vlan network on the Netscaler device due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to delete guest vlan network on the Netscaler device due to " + e.getMessage());
        }
    }

    private boolean nsVlanExists(final long vlanTag) throws ExecutionException {
        try {
            if (vlan.get(_netscalerService, new Long(vlanTag)) != null) {
                return true;
            } else {
                return false;
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify  VLAN exists on the NetScaler device due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify  VLAN exists on the NetScaler device due to " + e.getMessage());
        }
    }

    private boolean nsSnipExists(final String subnetIp) throws ExecutionException {
        try {
            final nsip tmpSubnetIp = new nsip();
            tmpSubnetIp.set_ipaddress(subnetIp);

            final nsip snip = nsip.get(_netscalerService, tmpSubnetIp);
            return snip != null;
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify if SNIP exists on the NetScaler device due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify if SNIP exists on the NetScaler device due to " + e.getMessage());
        }
    }

    private boolean nsServerExists(final String serverName) throws ExecutionException {
        try {
            if (com.citrix.netscaler.nitro.resource.config.basic.server.get(_netscalerService, serverName) != null) {
                return true;
            } else {
                return false;
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify Server " + serverName + " exists on the NetScaler device due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify Server " + serverName + " exists on the NetScaler device due to " + e.getMessage());
        }
    }

    private boolean nsVirtualServerExists(final String vserverName) throws ExecutionException {
        try {
            if (com.citrix.netscaler.nitro.resource.config.lb.lbvserver.get(_netscalerService, vserverName) != null) {
                return true;
            } else {
                return false;
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify VServer " + vserverName + " exists on the NetScaler device due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify VServer " + vserverName + " exists on the NetScaler device due to " + e.getMessage());
        }
    }

    private boolean nsVlanNsipBindingExists(final long vlanTag, final String vlanSelfIp) throws ExecutionException {
        try {
            final vlan_nsip_binding[] vlanNsipBindings = vlan_nsip_binding.get(_netscalerService, vlanTag);
            if (vlanNsipBindings != null && vlanNsipBindings[0] != null && vlanNsipBindings[0].get_ipaddress().equalsIgnoreCase(vlanSelfIp)) {
                return true;
            } else {
                return false;
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify Vlan " + vlanTag + " to SNIP " + vlanSelfIp + " binding exists due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify Vlan " + vlanTag + " to SNIP " + vlanSelfIp + " binding exists due to " + e.getMessage());
        }
    }

    private lbvserver getVirtualServerIfExisits(final String lbVServerName) throws ExecutionException {
        try {
            return lbvserver.get(_netscalerService, lbVServerName);
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return null;
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private lbmonitor getMonitorIfExisits(final String lbMonitorName) throws ExecutionException {
        try {
            return lbmonitor.get(_netscalerService, lbMonitorName);
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return null;
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private boolean isServiceBoundToVirtualServer(final String serviceName) throws ExecutionException {
        try {
            final lbvserver[] lbservers = lbvserver.get(_netscalerService);
            for (final lbvserver vserver : lbservers) {
                final filtervalue[] filter = new filtervalue[1];
                filter[0] = new filtervalue("servicename", serviceName);
                final lbvserver_service_binding[] result = lbvserver_service_binding.get_filtered(_netscalerService, vserver.get_name(), filter);
                if (result != null && result.length > 0) {
                    return true;
                }
            }
            return false;
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify service " + serviceName + " is bound to any virtual server due to " + e.getMessage());
        }
    }

    private boolean isServiceBoundToMonitor(final String nsServiceName, final String nsMonitorName) throws ExecutionException {

        final filtervalue[] filter = new filtervalue[1];
        filter[0] = new filtervalue("monitor_name", nsMonitorName);
        service_lbmonitor_binding[] result;
        try {
            result = service_lbmonitor_binding.get_filtered(_netscalerService, nsServiceName, filter);
            if (result != null && result.length > 0) {
                return true;
            }

        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify service " + nsServiceName + " is bound to any monitor due to " + e.getMessage());
        }
        return false;
    }

    private boolean nsMonitorExist(final String nsMonitorname) throws ExecutionException {
        if (getMonitorIfExisits(nsMonitorname) != null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean nsServiceExists(final String serviceName) throws ExecutionException {
        try {
            if (com.citrix.netscaler.nitro.resource.config.basic.service.get(_netscalerService, serviceName) != null) {
                return true;
            } else {
                return false;
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_NO_SERIVCE) {
                return false;
            } else {
                throw new ExecutionException("Failed to verify service " + serviceName + " exists due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify service " + serviceName + " exists due to " + e.getMessage());
        }
    }

    private boolean nsServiceBindingExists(final String lbVirtualServer, final String serviceName) throws ExecutionException {
        try {
            final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding[] serviceBindings =
                    com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding.get(_netscalerService, lbVirtualServer);
            if (serviceBindings != null) {
                for (final com.citrix.netscaler.nitro.resource.config.lb.lbvserver_service_binding binding : serviceBindings) {
                    if (serviceName.equalsIgnoreCase(binding.get_servicename())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to verify lb vserver " + lbVirtualServer + "and service " + serviceName + " binding exists due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify lb vserver " + lbVirtualServer + "and service " + serviceName + " binding exists due to " + e.getMessage());
        }
    }

    private boolean isServiceGroupBoundToVirtualServer(final String nsVirtualServerName, final String serviceGroupName) throws ExecutionException {

        new lbvserver_servicegroup_binding();

        try {
            final lbvserver_servicegroup_binding[] result =
                    lbvserver_servicegroup_binding.get_filtered(_netscalerService, nsVirtualServerName, "servicegroupname:" + serviceGroupName);
            if (result != null && result.length > 0) {
                return true;
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to verify lb vserver " + nsVirtualServerName + "and servicegrop " + serviceGroupName + " binding exists due to " +
                    e.getMessage());
        }
        return false;

    }

    private boolean nsServiceGroupExists(final String lbVServerName) throws ExecutionException {
        try {
            return servicegroup.get(_netscalerService, lbVServerName) != null;
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return false; // service group does not exist
            } else {
                throw new ExecutionException(e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void deleteServersInGuestVlan(final long vlanTag, final String vlanSelfIp, final String vlanNetmask) throws ExecutionException {
        try {
            final com.citrix.netscaler.nitro.resource.config.basic.server[] serverList = com.citrix.netscaler.nitro.resource.config.basic.server.get(_netscalerService);

            if (serverList == null) {
                return;
            }

            // remove the server and services associated with guest vlan
            for (final com.citrix.netscaler.nitro.resource.config.basic.server server : serverList) {
                // check if server belong to same subnet as one associated with vlan
                if (NetUtils.sameSubnet(vlanSelfIp, server.get_ipaddress(), vlanNetmask)) {
                    // first remove services associated with this server
                    final com.citrix.netscaler.nitro.resource.config.basic.service serveicesList[] =
                            com.citrix.netscaler.nitro.resource.config.basic.service.get(_netscalerService);
                    if (serveicesList != null) {
                        for (final com.citrix.netscaler.nitro.resource.config.basic.service svc : serveicesList) {
                            if (svc.get_servername().equals(server.get_ipaddress())) {
                                apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.service.delete(_netscalerService, svc.get_name());
                                if (apiCallResult.errorcode != 0) {
                                    throw new ExecutionException("Failed to remove service:" + svc.get_name());
                                }
                            }
                        }
                    }
                    // remove the server
                    // don't delete server which has no ip address (these servers are created by NS for autoscale
                    if (server.get_ipaddress() != null) {
                        apiCallResult = com.citrix.netscaler.nitro.resource.config.basic.server.delete(_netscalerService, server.get_name());
                        if (apiCallResult.errorcode != 0) {
                            throw new ExecutionException("Failed to remove server:" + server.get_name());
                        }
                    }
                }
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to delete server and services in the guest vlan:" + vlanTag + " on the Netscaler device due to: " + e.getMessage());
        }
    }

    private String getNetScalerProtocol(final LoadBalancerTO loadBalancer) throws ExecutionException {
        final int port = loadBalancer.getSrcPort();
        String lbProtocol = loadBalancer.getLbProtocol();
        final StickinessPolicyTO[] stickyPolicies = loadBalancer.getStickinessPolicies();
        String nsProtocol = "TCP";

        if (lbProtocol == null) {
            lbProtocol = loadBalancer.getProtocol();
        }

        if (stickyPolicies != null && stickyPolicies.length > 0 && stickyPolicies[0] != null) {
            final StickinessPolicyTO stickinessPolicy = stickyPolicies[0];
            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName()) ||
                    StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                nsProtocol = "HTTP";
                return nsProtocol;
            }
        }

        if (lbProtocol.equalsIgnoreCase(NetUtils.SSL_PROTO) || lbProtocol.equalsIgnoreCase(NetUtils.HTTP_PROTO)) {
            return lbProtocol.toUpperCase();
        }

        if (port == NetUtils.HTTP_PORT) {
            nsProtocol = "HTTP";
        } else if (NetUtils.TCP_PROTO.equalsIgnoreCase(lbProtocol)) {
            nsProtocol = "TCP";
        } else if (NetUtils.UDP_PROTO.equalsIgnoreCase(lbProtocol)) {
            nsProtocol = "UDP";
        }

        return nsProtocol;
    }

    private void addLBVirtualServer(final String virtualServerName, final String publicIp, final int publicPort, final String lbAlgorithm, final String protocol, final StickinessPolicyTO[] stickyPolicies,
            final AutoScaleVmGroupTO vmGroupTO) throws ExecutionException {
        try {
            String lbMethod;
            if ("roundrobin".equalsIgnoreCase(lbAlgorithm)) {
                lbMethod = "ROUNDROBIN";
            } else if ("leastconn".equalsIgnoreCase(lbAlgorithm)) {
                lbMethod = "LEASTCONNECTION";
            } else if ("source".equalsIgnoreCase(lbAlgorithm)) {
                lbMethod = "SOURCEIPHASH";
            } else {
                throw new ExecutionException("Got invalid load balancing algorithm: " + lbAlgorithm + " in the load balancing rule");
            }

            boolean vserverExisis = false;
            lbvserver vserver = getVirtualServerIfExisits(virtualServerName);
            if (vserver != null) {
                if (!vserver.get_servicetype().equalsIgnoreCase(protocol)) {
                    throw new ExecutionException("Can not update virtual server:" + virtualServerName + " as current protocol:" + vserver.get_servicetype() +
                            " of virtual server is different from the " + " intended protocol:" + protocol);
                }
                vserverExisis = true;
            }
            // Use new vserver always for configuration
            vserver = new lbvserver();
            vserver.set_name(virtualServerName);
            vserver.set_ipv46(publicIp);
            vserver.set_port(publicPort);
            vserver.set_servicetype(protocol);
            vserver.set_lbmethod(lbMethod);

            // netmask can only be set for source IP load balancer algorithm
            if (!lbMethod.equalsIgnoreCase("SOURCEIPHASH")) {
                vserver.set_netmask(null);
                vserver.set_v6netmasklen(null);
            }

            if (stickyPolicies != null && stickyPolicies.length > 0 && stickyPolicies[0] != null) {
                long timeout = 2;// netscaler default 2 min
                String cookieName = null;
                final StickinessPolicyTO stickinessPolicy = stickyPolicies[0];

                // get the session persistence parameters
                final List<Pair<String, String>> paramsList = stickinessPolicy.getParams();
                for (final Pair<String, String> param : paramsList) {
                    if ("holdtime".equalsIgnoreCase(param.first())) {
                        timeout = Long.parseLong(param.second());
                    } else if ("name".equalsIgnoreCase(param.first())) {
                        cookieName = param.second();
                    }
                }

                // configure virtual server based on the persistence method
                if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                    vserver.set_persistencetype("COOKIEINSERT");
                } else if (StickinessMethodType.SourceBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                    vserver.set_persistencetype("SOURCEIP");
                } else if (StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                    vserver.set_persistencetype("RULE");
                    vserver.set_rule("HTTP.REQ.HEADER(\"COOKIE\").VALUE(0).typecast_nvlist_t('=',';').value(\"" + cookieName + "\")");
                    vserver.set_resrule("HTTP.RES.HEADER(\"SET-COOKIE\").VALUE(0).typecast_nvlist_t('=',';').value(\"" + cookieName + "\")");
                } else {
                    throw new ExecutionException("Got invalid session persistence method: " + stickinessPolicy.getMethodName() + " in the load balancing rule");
                }

                // set session persistence timeout
                vserver.set_timeout(timeout);
            } else {
                // delete the LB stickyness policy
                vserver.set_persistencetype("NONE");
            }

            if (vserverExisis) {
                apiCallResult = lbvserver.update(_netscalerService, vserver);
            } else {
                apiCallResult = lbvserver.add(_netscalerService, vserver);
            }
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Failed to create new load balancing virtual server:" + virtualServerName + " due to " + apiCallResult.message);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Created load balancing virtual server " + virtualServerName + " on the Netscaler device");
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to create new virtual server:" + virtualServerName + " due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to create new virtual server:" + virtualServerName + " due to " + e.getMessage());
        }
    }

    private void removeLBVirtualServer(final String virtualServerName) throws ExecutionException {
        try {
            final lbvserver vserver = lbvserver.get(_netscalerService, virtualServerName);
            if (vserver == null) {
                return;
            }
            apiCallResult = lbvserver.delete(_netscalerService, vserver);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Failed to delete virtual server:" + virtualServerName + " due to " + apiCallResult.message);
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return;
            } else {
                throw new ExecutionException("Failed remove virtual server:" + virtualServerName + " due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to remove virtual server:" + virtualServerName + " due to " + e.getMessage());
        }
    }

    // Monitor related methods
    private void addLBMonitor(final String nsMonitorName, final String lbProtocol, final HealthCheckPolicyTO hcp) throws ExecutionException {
        try {
            // check if the monitor exists
            boolean csMonitorExisis = false;
            final lbmonitor csMonitor = getMonitorIfExisits(nsMonitorName);
            if (csMonitor != null) {
                if (!csMonitor.get_type().equalsIgnoreCase(lbProtocol)) {
                    throw new ExecutionException("Can not update monitor :" + nsMonitorName + " as current protocol:" + csMonitor.get_type() +
                            " of monitor is different from the " + " intended protocol:" + lbProtocol);
                }
                csMonitorExisis = true;
            }
            if (!csMonitorExisis) {
                final lbmonitor csMon = new lbmonitor();
                csMon.set_monitorname(nsMonitorName);
                csMon.set_type(lbProtocol);
                if (lbProtocol.equalsIgnoreCase("HTTP")) {
                    csMon.set_httprequest(hcp.getpingPath());
                    s_logger.trace("LB Protocol is HTTP,  Applying  ping path on HealthCheck Policy");
                } else {
                    s_logger.debug("LB Protocol is not HTTP, Skipping to apply  ping path on HealthCheck Policy");
                }

                csMon.set_interval(hcp.getHealthcheckInterval());
                csMon.set_retries(Math.max(hcp.getHealthcheckThresshold(), hcp.getUnhealthThresshold()) + 1);
                csMon.set_resptimeout(hcp.getResponseTime());
                csMon.set_failureretries(hcp.getUnhealthThresshold());
                csMon.set_successretries(hcp.getHealthcheckThresshold());
                s_logger.debug("Monitor properites going to get created :interval :: " + csMon.get_interval() + "respTimeOUt:: " + csMon.get_resptimeout() +
                        "failure retires(unhealththresshold) :: " + csMon.get_failureretries() + "successtries(healththresshold) ::" + csMon.get_successretries());
                lbmonitor.add(_netscalerService, csMon);
            } else {
                s_logger.debug("Monitor :" + nsMonitorName + " is already existing. Skipping to delete and create it");
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to create new monitor :" + nsMonitorName + " due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to create new monitor :" + nsMonitorName + " due to " + e.getMessage());
        }
    }

    private void bindServiceToMonitor(final String nsServiceName, final String nsMonitorName) throws ExecutionException {

        try {
            com.citrix.netscaler.nitro.resource.config.basic.service serviceObject = new com.citrix.netscaler.nitro.resource.config.basic.service();
            serviceObject = com.citrix.netscaler.nitro.resource.config.basic.service.get(_netscalerService, nsServiceName);
            if (serviceObject != null) {
                final com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding serviceMonitor =
                        new com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding();
                serviceMonitor.set_monitor_name(nsMonitorName);
                serviceMonitor.set_name(nsServiceName);
                serviceMonitor.set_monstate("ENABLED");
                s_logger.debug("Trying to bind  the monitor :" + nsMonitorName + " to the service :" + nsServiceName);
                com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding.add(_netscalerService, serviceMonitor);
                s_logger.debug("Successfully binded the monitor :" + nsMonitorName + " to the service :" + nsServiceName);
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to create new monitor :" + nsMonitorName + " due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to create new monitor :" + nsMonitorName + " due to " + e.getMessage());
        }
    }

    private void unBindServiceToMonitor(final String nsServiceName, final String nsMonitorName) throws ExecutionException {

        try {
            com.citrix.netscaler.nitro.resource.config.basic.service serviceObject = new com.citrix.netscaler.nitro.resource.config.basic.service();
            serviceObject = com.citrix.netscaler.nitro.resource.config.basic.service.get(_netscalerService, nsServiceName);

            if (serviceObject != null) {
                final com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding serviceMonitor =
                        new com.citrix.netscaler.nitro.resource.config.basic.service_lbmonitor_binding();
                serviceMonitor.set_monitor_name(nsMonitorName);
                serviceMonitor.set_name(nsServiceName);
                s_logger.debug("Trying to unbind  the monitor :" + nsMonitorName + " from the service :" + nsServiceName);
                service_lbmonitor_binding.delete(_netscalerService, serviceMonitor);
                s_logger.debug("Successfully unbinded the monitor :" + nsMonitorName + " from the service :" + nsServiceName);
            }

        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return;
            } else {
                throw new ExecutionException("Failed to unbind monitor :" + nsMonitorName + "from the service :" + nsServiceName + "due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to unbind monitor :" + nsMonitorName + "from the service :" + nsServiceName + "due to " + e.getMessage());
        }

    }

    private void removeLBMonitor(final String nsMonitorName) throws ExecutionException {

        try {
            if (nsMonitorExist(nsMonitorName)) {
                final lbmonitor monitorObj = lbmonitor.get(_netscalerService, nsMonitorName);
                monitorObj.set_respcode(null);
                lbmonitor.delete(_netscalerService, monitorObj);
                s_logger.info("Successfully deleted monitor : " + nsMonitorName);
            }
        } catch (final nitro_exception e) {
            if (e.getErrorCode() == NitroError.NS_RESOURCE_NOT_EXISTS) {
                return;
            } else {
                throw new ExecutionException("Failed to delete monitor :" + nsMonitorName + " due to " + e.getMessage());
            }
        } catch (final Exception e) {
            throw new ExecutionException("Failed to delete monitor :" + nsMonitorName + " due to " + e.getMessage());
        }

    }

    public synchronized void applyAutoScaleConfig(final LoadBalancerTO loadBalancer) throws Exception, ExecutionException {

        final AutoScaleVmGroupTO vmGroupTO = loadBalancer.getAutoScaleVmGroupTO();
        if (!isAutoScaleSupportedInNetScaler()) {
            throw new ExecutionException("AutoScale not supported in this version of NetScaler");
        }
        if (loadBalancer.isRevoked() || vmGroupTO.getState().equals("revoke")) {
            removeAutoScaleConfig(loadBalancer);
        } else {
            createAutoScaleConfig(loadBalancer);
        }
        // AutoScale APIs are successful executed, now save the configuration.
        saveConfiguration();
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Successfully executed resource AutoScaleConfig");
        }
    }

    private synchronized boolean createAutoScaleConfig(final LoadBalancerTO loadBalancerTO) throws ExecutionException, Exception {

        final String srcIp = loadBalancerTO.getSrcIp();
        final int srcPort = loadBalancerTO.getSrcPort();
        final String lbProtocol = getNetScalerProtocol(loadBalancerTO);
        final String lbAlgorithm = loadBalancerTO.getAlgorithm();
        generateAutoScaleVmGroupIdentifier(loadBalancerTO);
        final String nsVirtualServerName = generateNSVirtualServerName(srcIp, srcPort);
        final AutoScaleVmGroupTO vmGroupTO = loadBalancerTO.getAutoScaleVmGroupTO();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created load balancing virtual server " + nsVirtualServerName + " on the Netscaler device");
        }
        addLBVirtualServer(nsVirtualServerName, srcIp, srcPort, lbAlgorithm, lbProtocol, loadBalancerTO.getStickinessPolicies(), vmGroupTO);

        final String serviceGroupName = generateAutoScaleServiceGroupName(loadBalancerTO);
        if (!nsServiceGroupExists(serviceGroupName)) {
            // add servicegroup lb_autoscaleGroup -autoscale POLICY -memberPort 80
            final int memberPort = vmGroupTO.getMemberPort();
            try {
                final servicegroup serviceGroup = new servicegroup();
                serviceGroup.set_servicegroupname(serviceGroupName);
                serviceGroup.set_servicetype(lbProtocol);
                serviceGroup.set_autoscale("POLICY");
                serviceGroup.set_memberport(memberPort);
                servicegroup.add(_netscalerService, serviceGroup);
            } catch (final Exception e) {
                throw e;
            }
        }

        if (!isServiceGroupBoundToVirtualServer(nsVirtualServerName, serviceGroupName)) {
            // Bind autoscale service group
            // bind lb vserver lb lb_autoscaleGroup
            final lbvserver_servicegroup_binding vserver_servicegroup_binding = new lbvserver_servicegroup_binding();

            try {
                vserver_servicegroup_binding.set_name(nsVirtualServerName);
                vserver_servicegroup_binding.set_servicegroupname(serviceGroupName);
                lbvserver_servicegroup_binding.add(_netscalerService, vserver_servicegroup_binding);
            } catch (final Exception e) {
                throw e;
            }
        }

        // Create the autoscale config
        if (!loadBalancerTO.getAutoScaleVmGroupTO().getState().equals("disabled")) {
            // on restart of network, there might be vmgrps in disabled state, no need to create autoscale config for them
            enableAutoScaleConfig(loadBalancerTO, false);
        } else if (loadBalancerTO.getAutoScaleVmGroupTO().getState().equals("disabled")) {
            disableAutoScaleConfig(loadBalancerTO, false);
        }

        return true;
    }

    private synchronized boolean removeAutoScaleConfig(final LoadBalancerTO loadBalancerTO) throws Exception, ExecutionException {
        final String srcIp = loadBalancerTO.getSrcIp();
        final int srcPort = loadBalancerTO.getSrcPort();
        generateAutoScaleVmGroupIdentifier(loadBalancerTO);

        final String nsVirtualServerName = generateNSVirtualServerName(srcIp, srcPort);
        final String serviceGroupName = generateAutoScaleServiceGroupName(loadBalancerTO);

        if (loadBalancerTO.getAutoScaleVmGroupTO().getCurrentState().equals("enabled")) {
            disableAutoScaleConfig(loadBalancerTO, false);
        }

        if (isServiceGroupBoundToVirtualServer(nsVirtualServerName, serviceGroupName)) {
            // UnBind autoscale service group
            // unbind lb vserver lb lb_autoscaleGroup
            final lbvserver_servicegroup_binding vserver_servicegroup_binding = new lbvserver_servicegroup_binding();
            try {
                vserver_servicegroup_binding.set_name(nsVirtualServerName);
                vserver_servicegroup_binding.set_servicegroupname(serviceGroupName);
                lbvserver_servicegroup_binding.delete(_netscalerService, vserver_servicegroup_binding);
            } catch (final Exception e) {
                throw e;
            }
        }

        if (nsServiceGroupExists(serviceGroupName)) {
            // Remove autoscale service group
            final com.citrix.netscaler.nitro.resource.config.basic.servicegroup serviceGroup = new com.citrix.netscaler.nitro.resource.config.basic.servicegroup();
            try {
                serviceGroup.set_servicegroupname(serviceGroupName);
                servicegroup.delete(_netscalerService, serviceGroup);
            } catch (final Exception e) {
                throw e;
            }
        }

        removeLBVirtualServer(nsVirtualServerName);

        return true;
    }

    private synchronized boolean enableAutoScaleConfig(final LoadBalancerTO loadBalancerTO, final boolean isCleanUp) throws Exception {
        final String vmGroupIdentifier = generateAutoScaleVmGroupIdentifier(loadBalancerTO);
        final String srcIp = loadBalancerTO.getSrcIp();
        final int srcPort = loadBalancerTO.getSrcPort();

        final String nsVirtualServerName = generateNSVirtualServerName(srcIp, srcPort);
        final String serviceGroupName = generateAutoScaleServiceGroupName(loadBalancerTO);
        final String profileName = generateAutoScaleProfileName(vmGroupIdentifier);
        final String timerName = generateAutoScaleTimerName(vmGroupIdentifier);
        final String scaleDownActionName = generateAutoScaleScaleDownActionName(vmGroupIdentifier);
        final String scaleUpActionName = generateAutoScaleScaleUpActionName(vmGroupIdentifier);
        final String mtName = generateSnmpMetricTableName(vmGroupIdentifier);
        final String monitorName = generateSnmpMonitorName(vmGroupIdentifier);
        final AutoScaleVmGroupTO vmGroupTO = loadBalancerTO.getAutoScaleVmGroupTO();
        final AutoScaleVmProfileTO profileTO = vmGroupTO.getProfile();
        final List<AutoScalePolicyTO> policies = vmGroupTO.getPolicies();
        final int interval = vmGroupTO.getInterval();
        profileTO.getCounterParamList();
        String snmpCommunity = null;
        int snmpPort = DEFAULT_SNMP_PORT;
        long cur_prirotiy = 1;

        // get the session persistence parameters
        final List<Pair<String, String>> paramsList = profileTO.getCounterParamList();
        for (final Pair<String, String> param : paramsList) {
            if ("snmpcommunity".equalsIgnoreCase(param.first())) {
                snmpCommunity = param.second();
            } else if ("snmpport".equalsIgnoreCase(param.first())) {
                snmpPort = Integer.parseInt(param.second());
            }
        }

        try {
            // Set min and max autoscale members;
            // add lb vserver lb  http 10.102.31.100 80 -minAutoscaleMinMembers 3 -maxAutoscaleMembers 10
            final int minAutoScaleMembers = vmGroupTO.getMinMembers();
            final int maxAutoScaleMembers = vmGroupTO.getMaxMembers();
            final lbvserver vserver = new lbvserver();
            try {
                vserver.set_name(nsVirtualServerName);
                vserver.set_minautoscalemembers(minAutoScaleMembers);
                vserver.set_maxautoscalemembers(maxAutoScaleMembers);
                lbvserver.update(_netscalerService, vserver);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            /* AutoScale Config */
            // Add AutoScale Profile
            // add autoscale profile lb_asprofile CLOUDSTACK -url -http:// 10.102.31.34:8080/client/api- -apiKey abcdef
            // -sharedSecret xyzabc
            final String apiKey = profileTO.getAutoScaleUserApiKey();
            final String secretKey = profileTO.getAutoScaleUserSecretKey();
            final String url = profileTO.getCloudStackApiUrl();

            final autoscaleprofile autoscaleProfile = new autoscaleprofile();
            try {
                autoscaleProfile.set_name(profileName);
                autoscaleProfile.set_type("CLOUDSTACK");
                autoscaleProfile.set_apikey(apiKey);
                autoscaleProfile.set_sharedsecret(secretKey);
                autoscaleProfile.set_url(url);
                autoscaleprofile.add(_netscalerService, autoscaleProfile);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            // Add Timer
            final nstimer timer = new nstimer();
            try {
                timer.set_name(timerName);
                timer.set_interval(interval);
                nstimer.add(_netscalerService, timer);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            // AutoScale Actions
            Integer scaleUpQuietTime = null;
            Integer scaleDownQuietTime = null;
            for (final AutoScalePolicyTO autoScalePolicyTO : policies) {
                if (scaleUpQuietTime == null) {
                    if (isScaleUpPolicy(autoScalePolicyTO)) {
                        scaleUpQuietTime = autoScalePolicyTO.getQuietTime();
                        if (scaleDownQuietTime != null) {
                            break;
                        }
                    }
                }
                if (scaleDownQuietTime == null) {
                    if (isScaleDownPolicy(autoScalePolicyTO)) {
                        scaleDownQuietTime = autoScalePolicyTO.getQuietTime();
                        if (scaleUpQuietTime != null) {
                            break;
                        }
                    }
                }
            }

            // Add AutoScale ScaleUp action
            // add autoscale action lb_scaleUpAction provision -vserver lb -profilename lb_asprofile -params
            // -lbruleid=1234&command=deployvm&zoneid=10&templateid=5&serviceofferingid=3- -quiettime 300
            final com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction scaleUpAction =
                    new com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction();
            try {
                scaleUpAction.set_name(scaleUpActionName);
                scaleUpAction.set_type("SCALE_UP"); // TODO: will this be called provision?
                scaleUpAction.set_vserver(nsVirtualServerName); // Actions Vserver, the one that is autoscaled, with CS
                // now both are same. Not exposed in API.
                scaleUpAction.set_profilename(profileName);
                if(scaleUpQuietTime != null) {
                    scaleUpAction.set_quiettime(scaleUpQuietTime);
                }
                final String scaleUpParameters =
                        "command=deployVirtualMachine" + "&" + ApiConstants.ZONE_ID + "=" + profileTO.getZoneId() + "&" + ApiConstants.SERVICE_OFFERING_ID + "=" +
                                profileTO.getServiceOfferingId() + "&" + ApiConstants.TEMPLATE_ID + "=" + profileTO.getTemplateId() + "&" + ApiConstants.DISPLAY_NAME + "=" +
                                profileTO.getVmName() + "&" + (profileTO.getNetworkId() == null ? "" : ApiConstants.NETWORK_IDS + "=" + profileTO.getNetworkId() + "&") +
                                (profileTO.getOtherDeployParams() == null ? "" : profileTO.getOtherDeployParams() + "&") + "lbruleid=" + loadBalancerTO.getUuid();
                scaleUpAction.set_parameters(scaleUpParameters);
                autoscaleaction.add(_netscalerService, scaleUpAction);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            final com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction scaleDownAction =
                    new com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction();
            final Integer destroyVmGracePeriod = profileTO.getDestroyVmGraceperiod();
            try {
                scaleDownAction.set_name(scaleDownActionName);
                scaleDownAction.set_type("SCALE_DOWN"); // TODO: will this be called de-provision?
                scaleDownAction.set_vserver(nsVirtualServerName); // TODO: no global option as of now through Nitro.
                // Testing cannot be done.
                scaleDownAction.set_profilename(profileName);
                scaleDownAction.set_quiettime(scaleDownQuietTime);
                final String scaleDownParameters = "command=destroyVirtualMachine" + "&" + "lbruleid=" + loadBalancerTO.getUuid();
                scaleDownAction.set_parameters(scaleDownParameters);
                scaleDownAction.set_vmdestroygraceperiod(destroyVmGracePeriod);
                autoscaleaction.add(_netscalerService, scaleDownAction);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            /* Create min member policy */
            final String minMemberPolicyName = generateAutoScaleMinPolicyName(vmGroupIdentifier);
            final String minMemberPolicyExp =
                    "SYS.VSERVER(\"" + nsVirtualServerName + "\").ACTIVESERVICES.LT(SYS.VSERVER(\"" + nsVirtualServerName + "\").MINAUTOSCALEMEMBERS)";
            addAutoScalePolicy(timerName, minMemberPolicyName, cur_prirotiy++, minMemberPolicyExp, scaleUpActionName, interval, interval, isCleanUp);

            /* Create max member policy */
            final String maxMemberPolicyName = generateAutoScaleMaxPolicyName(vmGroupIdentifier);
            final String maxMemberPolicyExp =
                    "SYS.VSERVER(\"" + nsVirtualServerName + "\").ACTIVESERVICES.GT(SYS.VSERVER(\"" + nsVirtualServerName + "\").MAXAUTOSCALEMEMBERS)";
            addAutoScalePolicy(timerName, maxMemberPolicyName, cur_prirotiy++, maxMemberPolicyExp, scaleDownActionName, interval, interval, isCleanUp);

            /* Create Counters */
            final HashMap<String, Integer> snmpMetrics = new HashMap<String, Integer>();
            for (final AutoScalePolicyTO autoScalePolicyTO : policies) {
                final List<ConditionTO> conditions = autoScalePolicyTO.getConditions();
                String policyExpression = "";
                int snmpCounterNumber = 0;
                for (final ConditionTO conditionTO : conditions) {
                    final CounterTO counterTO = conditionTO.getCounter();
                    String counterName = counterTO.getName();
                    final String operator = conditionTO.getRelationalOperator();
                    final long threshold = conditionTO.getThreshold();

                    final StringBuilder conditionExpression = new StringBuilder();
                    try(Formatter formatter = new Formatter(conditionExpression, Locale.US);) {

                        if (counterTO.getSource().equals("snmp")) {
                            counterName = generateSnmpMetricName(counterName);
                            if (snmpMetrics.size() == 0) {
                                // Create Metric Table
                                //add lb metricTable lb_metric_table
                                final lbmetrictable metricTable = new lbmetrictable();
                                try {
                                    metricTable.set_metrictable(mtName);
                                    lbmetrictable.add(_netscalerService, metricTable);
                                } catch (final Exception e) {
                                    // Ignore Exception on cleanup
                                    if (!isCleanUp) {
                                        throw e;
                                    }
                                }

                                // Create Monitor
                                // add lb monitor lb_metric_table_mon LOAD -destPort 161 -snmpCommunity public -metricTable
                                // lb_metric_table -interval <policy_interval == 80% >
                                final lbmonitor monitor = new lbmonitor();
                                try {
                                    monitor.set_monitorname(monitorName);
                                    monitor.set_type("LOAD");
                                    monitor.set_destport(snmpPort);
                                    monitor.set_snmpcommunity(snmpCommunity);
                                    monitor.set_metrictable(mtName);
                                    monitor.set_interval((int)(interval * 0.8));
                                    lbmonitor.add(_netscalerService, monitor);
                                } catch (final Exception e) {
                                    // Ignore Exception on cleanup
                                    if (!isCleanUp) {
                                        throw e;
                                    }
                                }

                                // Bind monitor to servicegroup.
                                // bind lb monitor lb_metric_table_mon lb_autoscaleGroup -passive
                                final servicegroup_lbmonitor_binding servicegroup_monitor_binding = new servicegroup_lbmonitor_binding();
                                try {
                                    servicegroup_monitor_binding.set_servicegroupname(serviceGroupName);
                                    servicegroup_monitor_binding.set_monitor_name(monitorName);

                                    // Use the monitor for autoscaling purpose only.
                                    // Don't mark service members down when metric breaches threshold
                                    servicegroup_monitor_binding.set_passive(true);

                                    servicegroup_lbmonitor_binding.add(_netscalerService, servicegroup_monitor_binding);
                                } catch (final Exception e) {
                                    // Ignore Exception on cleanup
                                    if (!isCleanUp) {
                                        throw e;
                                    }
                                }
                            }

                            final boolean newMetric = !snmpMetrics.containsKey(counterName);
                            if (newMetric) {
                                snmpMetrics.put(counterName, snmpCounterNumber++);
                            }

                            if (newMetric) {
                                // bind lb metricTable lb_metric_table mem 1.3.6.1.4.1.2021.11.9.0
                                final String counterOid = counterTO.getValue();
                                final lbmetrictable_metric_binding metrictable_metric_binding = new lbmetrictable_metric_binding();
                                try {
                                    metrictable_metric_binding.set_metrictable(mtName);
                                    metrictable_metric_binding.set_metric(counterName);
                                    metrictable_metric_binding.set_Snmpoid(counterOid);
                                    lbmetrictable_metric_binding.add(_netscalerService, metrictable_metric_binding);
                                } catch (final Exception e) {
                                    // Ignore Exception on cleanup
                                    if (!isCleanUp) {
                                        throw e;
                                    }
                                }

                                // bind lb monitor lb_metric_table_mon -metric cpu -metricThreshold 1
                                final lbmonitor_metric_binding monitor_metric_binding = new lbmonitor_metric_binding();

                                try {
                                    monitor_metric_binding.set_monitorname(monitorName);
                                    monitor_metric_binding.set_metric(counterName);
                                    /*
                                     * Setting it to max to make sure traffic is not affected due to 'LOAD' monitoring.
                                     * For Ex. if CPU is tracked and CPU is greater than 80, it is still < than Integer.MAX_VALUE
                                     * so traffic will continue to flow.
                                     */
                                    monitor_metric_binding.set_metricthreshold(Integer.MAX_VALUE);
                                    lbmonitor_metric_binding.add(_netscalerService, monitor_metric_binding);
                                } catch (final Exception e) {
                                    // Ignore Exception on cleanup
                                    if (!isCleanUp) {
                                        throw e;
                                    }
                                }
                            }
                            // SYS.VSERVER("abcd").SNMP_TABLE(0).AVERAGE_VALUE.GT(80)
                            final int counterIndex = snmpMetrics.get(counterName); // TODO: temporary fix. later on counter name
                            // will be added as a param to SNMP_TABLE.
                            formatter.format("SYS.VSERVER(\"%s\").SNMP_TABLE(%d).AVERAGE_VALUE.%s(%d)", nsVirtualServerName, counterIndex, operator, threshold);
                        } else if (counterTO.getSource().equals("netscaler")) {
                            //SYS.VSERVER("abcd").RESPTIME.GT(10)
                            formatter.format("SYS.VSERVER(\"%s\").%s.%s(%d)", nsVirtualServerName, counterTO.getValue(), operator, threshold);
                        }
                    } finally {
                        // closing formatter
                    }
                    if (policyExpression.length() != 0) {
                        policyExpression += " && ";
                    }
                    policyExpression += conditionExpression;
                }
                policyExpression = "(" + policyExpression + ")";

                final String policyId = Long.toString(autoScalePolicyTO.getId());
                final String policyName = generateAutoScalePolicyName(vmGroupIdentifier, policyId);
                String action = null;
                if (isScaleUpPolicy(autoScalePolicyTO)) {
                    action = scaleUpActionName;
                    final String scaleUpCondition =
                            "SYS.VSERVER(\"" + nsVirtualServerName + "\").ACTIVESERVICES.LT(SYS.VSERVER(\"" + nsVirtualServerName + "\").MAXAUTOSCALEMEMBERS)";
                    policyExpression = scaleUpCondition + " && " + policyExpression;
                } else {
                    action = scaleDownActionName;
                    final String scaleDownCondition =
                            "SYS.VSERVER(\"" + nsVirtualServerName + "\").ACTIVESERVICES.GT(SYS.VSERVER(\"" + nsVirtualServerName + "\").MINAUTOSCALEMEMBERS)";
                    policyExpression = scaleDownCondition + " && " + policyExpression;
                }

                addAutoScalePolicy(timerName, policyName, cur_prirotiy++, policyExpression, action, autoScalePolicyTO.getDuration(), interval, isCleanUp);

            }
        } catch (final Exception ex) {
            if (!isCleanUp) {
                // Normal course, exception has occurred
                disableAutoScaleConfig(loadBalancerTO, true);
                throw ex;

            } else {
                // Programming error. Exception should never be thrown afterall.
                throw ex;
            }
        }

        return true;
    }

    private synchronized boolean disableAutoScaleConfig(final LoadBalancerTO loadBalancerTO, final boolean isCleanUp) throws Exception {

        final String vmGroupIdentifier = generateAutoScaleVmGroupIdentifier(loadBalancerTO);

        final String profileName = generateAutoScaleProfileName(vmGroupIdentifier);
        final String timerName = generateAutoScaleTimerName(vmGroupIdentifier);
        final String scaleDownActionName = generateAutoScaleScaleDownActionName(vmGroupIdentifier);
        final String scaleUpActionName = generateAutoScaleScaleUpActionName(vmGroupIdentifier);
        final String mtName = generateSnmpMetricTableName(vmGroupIdentifier);
        final String monitorName = generateSnmpMonitorName(vmGroupIdentifier);
        final String serviceGroupName = generateAutoScaleServiceGroupName(loadBalancerTO);
        final AutoScaleVmGroupTO vmGroupTO = loadBalancerTO.getAutoScaleVmGroupTO();
        final List<AutoScalePolicyTO> policies = vmGroupTO.getPolicies();
        final String minMemberPolicyName = generateAutoScaleMinPolicyName(vmGroupIdentifier);
        final String maxMemberPolicyName = generateAutoScaleMaxPolicyName(vmGroupIdentifier);

        try {

            /* Delete min/max member policies */

            removeAutoScalePolicy(timerName, minMemberPolicyName, isCleanUp);

            removeAutoScalePolicy(timerName, maxMemberPolicyName, isCleanUp);

            boolean isSnmp = false;
            /* Create Counters */
            for (final AutoScalePolicyTO autoScalePolicyTO : policies) {
                final List<ConditionTO> conditions = autoScalePolicyTO.getConditions();
                for (final ConditionTO conditionTO : conditions) {
                    final CounterTO counterTO = conditionTO.getCounter();
                    if (counterTO.getSource().equals("snmp")) {
                        isSnmp = true;
                        break;
                    }
                }
                final String policyId = Long.toString(autoScalePolicyTO.getId());
                final String policyName = generateAutoScalePolicyName(vmGroupIdentifier, policyId);

                // Removing Timer policy
                removeAutoScalePolicy(timerName, policyName, isCleanUp);
            }

            /* Delete AutoScale Config */
            // Delete AutoScale ScaleDown action
            final com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction scaleDownAction =
                    new com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction();
            try {
                scaleDownAction.set_name(scaleDownActionName);
                autoscaleaction.delete(_netscalerService, scaleDownAction);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            // Delete AutoScale ScaleUp action
            final com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction scaleUpAction =
                    new com.citrix.netscaler.nitro.resource.config.autoscale.autoscaleaction();
            try {
                scaleUpAction.set_name(scaleUpActionName);
                autoscaleaction.delete(_netscalerService, scaleUpAction);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            // Delete Timer
            final nstimer timer = new nstimer();
            try {
                timer.set_name(timerName);
                nstimer.delete(_netscalerService, timer);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            // Delete AutoScale Profile
            final autoscaleprofile autoscaleProfile = new autoscaleprofile();
            try {
                autoscaleProfile.set_name(profileName);
                autoscaleprofile.delete(_netscalerService, autoscaleProfile);
            } catch (final Exception e) {
                // Ignore Exception on cleanup
                if (!isCleanUp) {
                    throw e;
                }
            }

            if (isSnmp) {
                final servicegroup_lbmonitor_binding servicegroup_monitor_binding = new servicegroup_lbmonitor_binding();
                try {
                    servicegroup_monitor_binding.set_monitor_name(monitorName);
                    servicegroup_monitor_binding.set_servicegroupname(serviceGroupName);
                    servicegroup_lbmonitor_binding.delete(_netscalerService, servicegroup_monitor_binding);
                } catch (final Exception e) {
                    // Ignore Exception on cleanup
                    if (!isCleanUp) {
                        throw e;
                    }
                }

                // Delete Monitor
                // rm lb monitor lb_metric_table_mon
                final com.citrix.netscaler.nitro.resource.config.lb.lbmonitor monitor = new com.citrix.netscaler.nitro.resource.config.lb.lbmonitor();
                try {
                    monitor.set_monitorname(monitorName);
                    monitor.set_type("LOAD");
                    lbmonitor.delete(_netscalerService, monitor);
                } catch (final Exception e) {
                    // Ignore Exception on cleanup
                    if (!isCleanUp) {
                        throw e;
                    }
                }

                // Delete Metric Table
                final com.citrix.netscaler.nitro.resource.config.lb.lbmetrictable metricTable = new com.citrix.netscaler.nitro.resource.config.lb.lbmetrictable();
                try {
                    metricTable.set_metrictable(mtName);
                    lbmetrictable.delete(_netscalerService, metricTable);
                } catch (final Exception e) {
                    // Ignore Exception on cleanup
                    if (!isCleanUp) {
                        throw e;
                    }
                }
            }
        } catch (final Exception ex) {
            if (!isCleanUp) {
                // Normal course, exception has occurred
                enableAutoScaleConfig(loadBalancerTO, true);
                throw ex;
            } else {
                // Programming error
                throw ex;
            }
        }

        return true;
    }

    private synchronized void addAutoScalePolicy(final String timerName, final String policyName, final long priority, final String policyExpression, final String action, final int duration, final int interval,
            final boolean isCleanUp) throws Exception {
        // Adding a autoscale policy
        // add timer policy lb_policy_scaleUp_cpu_mem -rule - (SYS.CUR_VSERVER.METRIC_TABLE(cpu).AVG_VAL.GT(80)-
        // -action lb_scaleUpAction
        final autoscalepolicy timerPolicy = new autoscalepolicy();
        try {
            timerPolicy.set_name(policyName);
            timerPolicy.set_action(action);
            timerPolicy.set_rule(policyExpression);
            autoscalepolicy.add(_netscalerService, timerPolicy);
        } catch (final Exception e) {
            // Ignore Exception on cleanup
            if (!isCleanUp) {
                throw e;
            }
        }

        // bind timer policy
        // For now it is bound globally.
        // bind timer trigger lb_astimer -policyName lb_policy_scaleUp -vserver lb -priority 1 -samplesize 5
        // TODO: later bind to lbvserver. bind timer trigger lb_astimer -policyName lb_policy_scaleUp -vserver lb
        // -priority 1 -samplesize 5
        // -thresholdsize 5
        final nstimer_autoscalepolicy_binding timer_policy_binding = new nstimer_autoscalepolicy_binding();
        final int sampleSize = duration / interval;
        try {
            timer_policy_binding.set_name(timerName);
            timer_policy_binding.set_policyname(policyName);
            timer_policy_binding.set_samplesize(sampleSize);
            timer_policy_binding.set_threshold(sampleSize); // We are not exposing this parameter as of now.
            // i.e. n(m) is not exposed to CS user. So thresholdSize == sampleSize
            timer_policy_binding.set_priority(priority);
            nstimer_autoscalepolicy_binding.add(_netscalerService, timer_policy_binding);
        } catch (final Exception e) {
            // Ignore Exception on cleanup
            if (!isCleanUp) {
                throw e;
            }
        }
    }

    private void removeAutoScalePolicy(final String timerName, final String policyName, final boolean isCleanUp) throws Exception {
        // unbind timer policy
        // unbbind timer trigger lb_astimer -policyName lb_policy_scaleUp
        final nstimer_autoscalepolicy_binding timer_policy_binding = new nstimer_autoscalepolicy_binding();
        try {
            timer_policy_binding.set_name(timerName);
            timer_policy_binding.set_policyname(policyName);
            nstimer_autoscalepolicy_binding.delete(_netscalerService, timer_policy_binding);
        } catch (final Exception e) {
            // Ignore Exception on cleanup
            if (!isCleanUp) {
                throw e;
            }
        }

        // Removing Timer policy
        // rm timer policy lb_policy_scaleUp_cpu_mem
        final autoscalepolicy timerPolicy = new autoscalepolicy();
        try {
            timerPolicy.set_name(policyName);
            autoscalepolicy.delete(_netscalerService, timerPolicy);
        } catch (final Exception e) {
            // Ignore Exception on cleanup
            if (!isCleanUp) {
                throw e;
            }
        }

    }

    private boolean isAutoScaleSupportedInNetScaler() throws ExecutionException {
        new autoscaleprofile();
        try {
            autoscaleprofile.get(_netscalerService);
        } catch (final Exception ex) {
            // Looks like autoscale is not supported in this netscaler.
            // TODO: Config team has introduce a new command to check
            // the list of entities supported in a NetScaler. Can use that
            // once it is present in AutoScale branch.
            s_logger.warn("AutoScale is not supported in NetScaler");
            return false;
        }
        return true;
    }

    private boolean isScaleUpPolicy(final AutoScalePolicyTO autoScalePolicyTO) {
        return autoScalePolicyTO.getAction().equals("scaleup");
    }

    private boolean isScaleDownPolicy(final AutoScalePolicyTO autoScalePolicyTO) {
        return autoScalePolicyTO.getAction().equals("scaledown");
    }

    private void saveConfiguration() throws ExecutionException {
        try {
            apiCallResult = nsconfig.save(_netscalerService);
            if (apiCallResult.errorcode != 0) {
                throw new ExecutionException("Error occurred while saving configuration changes to Netscaler device due to " + apiCallResult.message);
            }
        } catch (final nitro_exception e) {
            throw new ExecutionException("Failed to save configuration changes to Netscaler device due to " + e.getMessage());
        } catch (final Exception e) {
            throw new ExecutionException("Failed to save configuration changes to Netscaler device due to " + e.getMessage());
        }
    }

    private ExternalNetworkResourceUsageAnswer getPublicIpBytesSentAndReceived(final ExternalNetworkResourceUsageCommand cmd) throws ExecutionException {
        final ExternalNetworkResourceUsageAnswer answer = new ExternalNetworkResourceUsageAnswer(cmd);

        try {
            final lbvserver_stats[] stats = lbvserver_stats.get(_netscalerService);

            if (stats == null || stats.length == 0) {
                return answer;
            }

            for (final lbvserver_stats stat_entry : stats) {
                final String lbvserverName = stat_entry.get_name();
                final lbvserver vserver = lbvserver.get(_netscalerService, lbvserverName);
                if (vserver != null) {
                    final String lbVirtualServerIp = vserver.get_ipv46();

                    long[] bytesSentAndReceived = answer.ipBytes.get(lbVirtualServerIp);
                    if (bytesSentAndReceived == null) {
                        bytesSentAndReceived = new long[] {0, 0};
                    }
                    bytesSentAndReceived[0] += stat_entry.get_totalrequestbytes();
                    bytesSentAndReceived[1] += stat_entry.get_totalresponsebytes();

                    if (bytesSentAndReceived[0] >= 0 && bytesSentAndReceived[1] >= 0) {
                        answer.ipBytes.put(lbVirtualServerIp, bytesSentAndReceived);
                    }
                }
            }
        } catch (final Exception e) {
            s_logger.error("Failed to get bytes sent and recived statistics due to " + e);
            throw new ExecutionException(e.getMessage());
        }

        return answer;
    }

    private Answer retry(final Command cmd, final int numRetries) {
        final int numRetriesRemaining = numRetries - 1;
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetriesRemaining);
        return executeRequest(cmd, numRetriesRemaining);
    }

    private boolean shouldRetry(final int numRetries) {
        try {
            if (numRetries > 0) {
                login();
                return true;
            }
        } catch (final Exception e) {
            s_logger.error("Failed to log in to Netscaler device at " + _ip + " due to " + e.getMessage());
        }
        return false;
    }

    private String generateInatRuleName(final String srcIp, final String dstIP) {
        return genObjectName("Cloud-Inat", srcIp);
    }

    private String generateRnatRuleName(final String srcIp, final String dstIP) {
        return genObjectName("Cloud-Rnat", srcIp);
    }

    private String generateNSVirtualServerName(final String srcIp, final long srcPort) {
        return genObjectName("Cloud-VirtualServer", srcIp, srcPort);
    }

    private String generateNSMonitorName(final String srcIp, final long srcPort) {
        // maximum length supported by NS is 31
        return genObjectName("Cloud-Hc", srcIp, srcPort);
    }

    private String generateNSServerName(final String serverIP) {
        return genObjectName("Cloud-Server-", serverIP);
    }

    private String generateNSServiceName(final String ip, final long port) {
        return genObjectName("Cloud-Service", ip, port);
    }

    private String generateAutoScaleVmGroupIdentifier(final LoadBalancerTO lbTO) {
        return lbTO.getSrcIp() + "-" + lbTO.getSrcPort();
    }

    private String generateAutoScaleServiceGroupName(final LoadBalancerTO lbTO) {
        /*
         *  ServiceGroup name in NetScaler wont support long names. Providing special name.
         *  Need for introducing uuid because every vmgroup creation should be distinguished.
         *  Ex. (1) create a vm group, delete a vmgroup, create a vmgroup on same lb ip and port
         *  This will reuse all vms from the original vm group in step (1)
         */

        return "Cloud" + lbTO.getAutoScaleVmGroupTO().getUuid().replace("-", "");
    }

    private String generateAutoScaleTimerName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-Timer", vmGroupIdentifier);
    }

    private String generateAutoScaleProfileName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-Profile", vmGroupIdentifier);
    }

    private String generateAutoScaleScaleUpActionName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-ScaleUpAction", vmGroupIdentifier);
    }

    private String generateAutoScaleScaleDownActionName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-ScaleDownAction", vmGroupIdentifier);
    }

    private String generateAutoScalePolicyName(final String vmGroupIdentifier, final String poilcyId) {
        return genObjectName("Cloud-AutoScale-Policy", vmGroupIdentifier, poilcyId);
    }

    private String generateAutoScaleMinPolicyName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-Policy-Min", vmGroupIdentifier);
    }

    private String generateAutoScaleMaxPolicyName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-AutoScale-Policy-Max", vmGroupIdentifier);
    }

    private String generateSnmpMetricTableName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-MTbl", vmGroupIdentifier);
    }

    private String generateSnmpMonitorName(final String vmGroupIdentifier) {
        return genObjectName("Cloud-Mon", vmGroupIdentifier);
    }

    private String generateSnmpMetricName(final String counterName) {
        return counterName.replace(' ', '_');
    }

    private String generateSslCertName(final String fingerPrint) {
        // maximum length supported by NS is 31
        // the first 20 characters of the SHA-1 checksum are the unique id
        final String uniqueId = fingerPrint.replace(":", "").substring(0, 20);

        return genObjectName("Cloud-Cert", uniqueId);
    }

    private String generateSslKeyName(final String fingerPrint) {
        final String uniqueId = fingerPrint.replace(":", "").substring(0, 20);
        return genObjectName("Cloud-Key", uniqueId);
    }

    private String generateSslCertKeyName(final String fingerPrint) {
        final String uniqueId = fingerPrint.replace(":", "").substring(0, 20);
        return genObjectName("Cloud-Cert", uniqueId);
    }

    private String genObjectName(final Object... args) {
        final StringBuffer buff = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            buff.append(args[i]);
            if (i != args.length - 1) {
                buff.append(_objectNamePathSep);
            }
        }
        return buff.toString();
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.ExternalLoadBalancer, id);
    }

    @Override
    public Type getType() {
        return Host.Type.ExternalLoadBalancer;
    }

    @Override
    public void setAgentControl(final IAgentControl agentControl) {
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

    @Override
    public void setName(final String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
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
    public void setRunLevel(final int level) {
        // TODO Auto-generated method stub

    }
}
