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
package com.cloud.network.element;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.cloudstack.region.gslb.GslbServiceProvider;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetScalerImplementNetworkCommand;
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.agent.api.routing.HealthCheckLBConfigAnswer;
import com.cloud.agent.api.routing.HealthCheckLBConfigCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ConfigureNetscalerLoadBalancerCmd;
import com.cloud.api.commands.DeleteNetscalerLoadBalancerCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancerNetworksCmd;
import com.cloud.api.commands.ListNetscalerLoadBalancersCmd;
import com.cloud.api.commands.RegisterNetscalerControlCenterCmd;
import com.cloud.api.commands.RegisterServicePackageCmd;
import com.cloud.api.response.NetScalerServicePackageResponse;
import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.ExternalLoadBalancerDeviceManager;
import com.cloud.network.ExternalLoadBalancerDeviceManagerImpl;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.network.NetScalerPodVO;
import com.cloud.network.NetScalerServicePackageVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.as.AutoScaleCounter;
import com.cloud.network.as.AutoScaleCounter.AutoScaleCounterType;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.network.dao.NetScalerControlCenterDao;
import com.cloud.network.dao.NetScalerPodDao;
import com.cloud.network.dao.NetScalerServicePackageDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.resource.NetScalerControlCenterResource;
import com.cloud.network.resource.NetscalerResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

public class NetscalerElement extends ExternalLoadBalancerDeviceManagerImpl implements LoadBalancingServiceProvider, NetscalerLoadBalancerElementService,
        ExternalLoadBalancerDeviceManager, IpDeployer, StaticNatServiceProvider, GslbServiceProvider {

    private static final Logger s_logger = Logger.getLogger(NetscalerElement.class);
    public static final AutoScaleCounterType AutoScaleCounterSnmp = new AutoScaleCounterType("snmp");
    public static final AutoScaleCounterType AutoScaleCounterNetscaler = new AutoScaleCounterType("netscaler");

    @Inject
    NetworkModel _networkManager;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    ExternalLoadBalancerDeviceDao _lbDeviceDao;
    @Inject
    NetworkExternalLoadBalancerDao _networkLBDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    HostDetailsDao _detailsDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    NetScalerPodDao _netscalerPodDao;
    @Inject
    DataCenterIpAddressDao _privateIpAddressDao;
    @Inject
    ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject
    NetScalerServicePackageDao _netscalerServicePackageDao;
    @Inject
    NetScalerControlCenterDao _netscalerControlCenterDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    HostDetailsDao _hostDetailDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    NetworkOrchestrationService _networkService;

    private boolean canHandle(Network config, Service service) {
        DataCenter zone = _dcDao.findById(config.getDataCenterId());
        // Create a NCC Resource on Demand for the zone.

        boolean handleInAdvanceZone =
            (zone.getNetworkType() == NetworkType.Advanced && (config.getGuestType() == Network.GuestType.Isolated || config.getGuestType() == Network.GuestType.Shared) && config.getTrafficType() == TrafficType.Guest);
        boolean handleInBasicZone =
            (zone.getNetworkType() == NetworkType.Basic && config.getGuestType() == Network.GuestType.Shared && config.getTrafficType() == TrafficType.Guest);

        if (!(handleInAdvanceZone || handleInBasicZone)) {
            s_logger.trace("Not handling network with Type  " + config.getGuestType() + " and traffic type " + config.getTrafficType() + " in zone of type " +
                zone.getNetworkType());
            return false;
        }

        return (_networkManager.isProviderForNetwork(getProvider(), config.getId()) && _ntwkSrvcDao.canProviderSupportServiceInNetwork(config.getId(), service,
            Network.Provider.Netscaler));
    }

    private boolean isBasicZoneNetwok(Network config) {
        DataCenter zone = _dcDao.findById(config.getDataCenterId());
        return (zone.getNetworkType() == NetworkType.Basic && config.getGuestType() == Network.GuestType.Shared && config.getTrafficType() == TrafficType.Guest);
    }

    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException,
        ConcurrentOperationException, InsufficientNetworkCapacityException {

        if (!canHandle(guestConfig, Service.Lb)) {
            return false;
        }

        if (_ntwkSrvcDao.canProviderSupportServiceInNetwork(guestConfig.getId(), Service.StaticNat, Network.Provider.Netscaler) && !isBasicZoneNetwok(guestConfig)) {
            s_logger.error("NetScaler provider can not be Static Nat service provider for the network " + guestConfig.getGuestType() + " and traffic type " +
                guestConfig.getTrafficType());
            return false;
        }

        try {
            if(offering.getServicePackage() == null) {
                return manageGuestNetworkWithExternalLoadBalancer(true, guestConfig);
            } else {
                // if the network offering has service package implement it with Netscaler Control Center
                manageGuestNetworkWithNetscalerControlCenter(true, guestConfig, offering);
                return true;
            }
        } catch (InsufficientCapacityException capacityException) {
            throw new ResourceUnavailableException("There are no NetScaler load balancer devices with the free capacity for implementing this network", DataCenter.class,
                guestConfig.getDataCenterId());
        } catch (ConfigurationException e) {
            throw new ResourceUnavailableException("There are no NetScaler load balancer devices with the free capacity for implementing this network : " + e.getMessage(), DataCenter.class,
                    guestConfig.getDataCenterId());
        }
    }


    public HostVO getNetScalerControlCenterForNetwork(Network guestConfig) {
        long zoneId = guestConfig.getDataCenterId();
        return _hostDao.findByTypeNameAndZoneId(zoneId, "NetscalerControlCenter", Type.NetScalerControlCenter);
    }

    public HostVO allocateNCCResourceForNetwork(Network guestConfig) throws ConfigurationException {

        //TODO get the NCCVO details and creat the server resource
        List<NetScalerControlCenterVO> ncc =  _netscalerControlCenterDao.listAll();
        HostVO hostVO = null;
        if(ncc.size() > 0) {
            NetScalerControlCenterVO nccVO = ncc.get(0);
            String ipAddress = nccVO.getNccip();
            Map hostDetails = new HashMap<String, String>();
            String hostName =  "NetscalerControlCenter";//getExternalLoadBalancerResourceGuid("NetscalerControlCenter");
            hostDetails.put("name", hostName);
            hostDetails.put("guid", UUID.randomUUID().toString());
            hostDetails.put("zoneId", guestConfig.getDataCenterId());
            hostDetails.put("ip", ipAddress);
            hostDetails.put("username", nccVO.getUsername());
            hostDetails.put("password", nccVO.getPassword());
            hostDetails.put("deviceName", "netscaler control center");
            ServerResource resource = new NetScalerControlCenterResource();
            resource.configure(hostName, hostDetails);
            final Host host = _resourceMgr.addHost(1, resource, Host.Type.NetScalerControlCenter, hostDetails);
            hostVO = _hostDao.findById(host.getId());
        }
        //resource.configure(hostName, hostDetails);
        return hostVO;
    }

    public boolean manageGuestNetworkWithNetscalerControlCenter(boolean add, Network guestConfig, NetworkOffering offering) throws ResourceUnavailableException, InsufficientCapacityException, ConfigurationException {

        if (guestConfig.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("External load balancer can only be used for guest networks.");
            return false;
        }

        long zoneId = guestConfig.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO netscalerControlCenter = null;

        if (add) {
            HostVO lbDeviceVO = null;
            // on restart network, device could have been allocated already, skip allocation if a device is assigned
            lbDeviceVO = getNetScalerControlCenterForNetwork(guestConfig);
            if (lbDeviceVO == null) {
                // allocate a load balancer device for the network
                lbDeviceVO = allocateNCCResourceForNetwork(guestConfig);
                if (lbDeviceVO == null) {
                    String msg = "failed to allocate Netscaler ControlCenter Resource for the zone in the network " + guestConfig.getId();
                    s_logger.error(msg);
                    throw new InsufficientNetworkCapacityException(msg, DataCenter.class, guestConfig.getDataCenterId());
                }
            }
            netscalerControlCenter = _hostDao.findById(lbDeviceVO.getId());
            s_logger.debug("Allocated Netscaler Control Center device:" + lbDeviceVO.getId() + " for the network: " + guestConfig.getId());
        } else {
            // find the load balancer device allocated for the network
            ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(guestConfig);
            if (lbDeviceVO == null) {
                s_logger.warn("Network shutdwon requested on external load balancer element, which did not implement the network."
                    + " Either network implement failed half way through or already network shutdown is completed. So just returning.");
                return true;
            }

            netscalerControlCenter = _hostDao.findById(lbDeviceVO.getHostId());
            assert (netscalerControlCenter != null) : "There is no device assigned to this network how did shutdown network ended up here??";
        }
        JSONObject networkDetails = new JSONObject();
        JSONObject networkPayload = new JSONObject();
        /*

        Implement network details:
        url: <ip>/cs/cca/networks

        json payload:
        { “network”: {"id":<val>, "vlan":<val>, "cidr":<val>, "gateway":<val>, "servicepackage_id":<val>, "zone_id":<val>, "region_id":<val>, "name":<val> }}
        */
        String selfIp = null;
        try {
            networkDetails.put("id", guestConfig.getId());
            networkDetails.put("vlan", guestConfig.getBroadcastUri());
            networkDetails.put("cidr", guestConfig.getCidr());
            networkDetails.put("gateway", guestConfig.getGateway());
            networkDetails.put("servicepackage_id", offering.getServicePackage());
            networkDetails.put("zone_id", zoneId);
            networkDetails.put("account_id", guestConfig.getAccountId());
            selfIp = _ipAddrMgr.acquireGuestIpAddress(guestConfig, null);
            if (selfIp == null) {
                String msg = "failed to acquire guest IP address so not implementing the network on the NetscalerControlCenter";
                s_logger.error(msg);
                throw new InsufficientNetworkCapacityException(msg, Network.class, guestConfig.getId());
            }
            networkDetails.put("snip", selfIp);
            //TODO region is hardcoded make it dynamic
            networkDetails.put("region_id", 1);
            networkDetails.put("name", guestConfig.getName());

            networkPayload.put("network", networkDetails);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        NetScalerImplementNetworkCommand cmd = new NetScalerImplementNetworkCommand(zoneId, netscalerControlCenter.getId(), networkPayload.toString());
         if(add) {
             Answer answer = _agentMgr.easySend(netscalerControlCenter.getId(), cmd);
             //TODO After getting the answer check with the job id and do poll on the job and then save the selfip or acquired guest ip to the Nics table
             if(answer != null ) {
                 if (add) {
                     // Insert a new NIC for this guest network to reserve the self IP
                     _networkService.savePlaceholderNic(guestConfig, selfIp, null, null);
                 }
             }
         }
        // Send a command to the external load balancer to implement or shutdown the guest network
/*        long guestVlanTag = Long.parseLong(BroadcastDomainType.getValue(guestConfig.getBroadcastUri()));
        String selfIp = null;
        String guestVlanNetmask = NetUtils.cidr2Netmask(guestConfig.getCidr());
        Integer networkRate = _networkModel.getNetworkRate(guestConfig.getId(), null);

        if (add) {
            // on restart network, network could have already been implemented. If already implemented then return
            Nic selfipNic = getPlaceholderNic(guestConfig);
            if (selfipNic != null) {
                return true;
            }

            // Acquire a self-ip address from the guest network IP address range
            selfIp = _ipAddrMgr.acquireGuestIpAddress(guestConfig, null);
            if (selfIp == null) {
                String msg = "failed to acquire guest IP address so not implementing the network on the external load balancer ";
                s_logger.error(msg);
                throw new InsufficientNetworkCapacityException(msg, Network.class, guestConfig.getId());
            }
        } else {
            // get the self-ip used by the load balancer
            Nic selfipNic = getPlaceholderNic(guestConfig);
            if (selfipNic == null) {
                s_logger.warn("Network shutdwon requested on external load balancer element, which did not implement the network."
                    + " Either network implement failed half way through or already network shutdown is completed. So just returning.");
                return true;
            }
            selfIp = selfipNic.getIp4Address();
        }
*/
        // It's a hack, using isOneToOneNat field for indicate if it's inline or not
/*        boolean inline = _networkMgr.isNetworkInlineMode(guestConfig);
        IpAddressTO ip =
            new IpAddressTO(guestConfig.getAccountId(), null, add, false, true, String.valueOf(guestVlanTag), selfIp, guestVlanNetmask, null, networkRate, inline);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        Answer answer = _agentMgr.easySend(netscalerControlCenter.getId(), cmd);
*/
/*        if (answer == null || !answer.getResult()) {
            String action = add ? "implement" : "shutdown";
            String answerDetails = (answer != null) ? answer.getDetails() : null;
            answerDetails = (answerDetails != null) ? " due to " + answerDetails : "";
            String msg = "External load balancer was unable to " + action + " the guest network on the external load balancer in zone " + zone.getName() + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, Network.class, guestConfig.getId());
        }

        if (add) {
            // Insert a new NIC for this guest network to reserve the self IP
            _networkMgr.savePlaceholderNic(guestConfig, selfIp, null, null);
        } else {
            // release the self-ip obtained from guest network
            Nic selfipNic = getPlaceholderNic(guestConfig);
            _nicDao.remove(selfipNic.getId());

            // release the load balancer allocated for the network
            boolean releasedLB = freeLoadBalancerForNetwork(guestConfig);
            if (!releasedLB) {
                String msg = "Failed to release the external load balancer used for the network: " + guestConfig.getId();
                s_logger.error(msg);
            }
        }

        if (s_logger.isDebugEnabled()) {
            Account account = _accountDao.findByIdIncludingRemoved(guestConfig.getAccountId());
            String action = add ? "implemented" : "shut down";
            s_logger.debug("External load balancer has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() +
                ") with VLAN tag " + guestVlanTag);
        }*/

        return true;
    }

    @Override
    public boolean prepare(Network config, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean release(Network config, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) {
        return true;
    }

    @Override
    public boolean shutdown(Network guestConfig, ReservationContext context, boolean cleanup) throws ResourceUnavailableException, ConcurrentOperationException {
        if (!canHandle(guestConfig, Service.Lb)) {
            return false;
        }

        try {
            return manageGuestNetworkWithExternalLoadBalancer(false, guestConfig);
        } catch (InsufficientCapacityException capacityException) {
            // TODO: handle out of capacity exception gracefully in case of multple providers available
            return false;
        }
    }

    @Override
    public boolean destroy(Network config, ReservationContext context) {
        return true;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        if (canHandle(network, Service.Lb)) {
            String algo = rule.getAlgorithm();
            return (algo.equals("roundrobin") || algo.equals("leastconn") || algo.equals("source"));
        }
        return true;
    }

    @Override
    public boolean applyLBRules(Network config, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config, Service.Lb)) {
            return false;
        }

        if (!canHandleLbRules(rules)) {
            return false;
        }

        if (isBasicZoneNetwok(config)) {
            return applyElasticLoadBalancerRules(config, rules);
        } else {
            return applyLoadBalancerRules(config, rules);
        }
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();

        // Specifies that the RoundRobin and Leastconn algorithms are supported for load balancing rules
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin, leastconn, source");

        // specifies that Netscaler network element can provided both shared and isolation modes
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated, shared");

        // Specifies that load balancing rules can be made for either TCP or UDP traffic
        lbCapabilities.put(Capability.SupportedProtocols, "tcp,udp");

        // Specifies that this element can measure network usage on a per public IP basis
        lbCapabilities.put(Capability.TrafficStatistics, "per public ip");

        // Specifies that load balancing rules can only be made with public IPs that aren't source NAT IPs
        lbCapabilities.put(Capability.LoadBalancingSupportedIps, "additional");

        // Supports only Public load balancing
        lbCapabilities.put(Capability.LbSchemes, LoadBalancerContainer.Scheme.Public.toString());

        // Specifies that load balancing rules can support autoscaling and the list of counters it supports
        AutoScaleCounter counter;
        List<AutoScaleCounter> counterList = new ArrayList<AutoScaleCounter>();
        counter = new AutoScaleCounter(AutoScaleCounterSnmp);
        counterList.add(counter);
        counter.addParam("snmpcommunity", true, "the community string that has to be used to do a SNMP GET on the AutoScaled Vm", false);
        counter.addParam("snmpport", false, "the port at which SNMP agent is running on the AutoScaled Vm", false);

        counter = new AutoScaleCounter(AutoScaleCounterNetscaler);
        counterList.add(counter);

        Gson gson = new Gson();
        String autoScaleCounterList = gson.toJson(counterList);
        lbCapabilities.put(Capability.AutoScaleCounters, autoScaleCounterList);

        LbStickinessMethod method;
        List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>();
        method = new LbStickinessMethod(StickinessMethodType.LBCookieBased, "This is cookie based sticky method, can be used only for http");
        methodList.add(method);
        method.addParam("holdtime", false, "time period in minutes for which persistence is in effect.", false);

        method = new LbStickinessMethod(StickinessMethodType.AppCookieBased, "This is app session based sticky method, can be used only for http");
        methodList.add(method);
        method.addParam("name", true, "cookie name passed in http header by apllication to the client", false);

        method = new LbStickinessMethod(StickinessMethodType.SourceBased, "This is source based sticky method, can be used for any type of protocol.");
        methodList.add(method);
        method.addParam("holdtime", false, "time period for which persistence is in effect.", false);

        String stickyMethodList = gson.toJson(methodList);
        lbCapabilities.put(Capability.SupportedStickinessMethods, stickyMethodList);

        lbCapabilities.put(Capability.ElasticLb, "true");
        //Setting HealthCheck Capability to True for Netscaler element
        lbCapabilities.put(Capability.HealthCheckPolicy, "true");
        capabilities.put(Service.Lb, lbCapabilities);

        Map<Capability, String> staticNatCapabilities = new HashMap<Capability, String>();
        staticNatCapabilities.put(Capability.ElasticIp, "true");
        capabilities.put(Service.StaticNat, staticNatCapabilities);

        // Supports SSL offloading
        lbCapabilities.put(Capability.SslTermination, "true");

        // TODO - Murali, please put correct capabilities here
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        return capabilities;
    }

    @Override
    public ExternalLoadBalancerDeviceVO addNetscalerLoadBalancer(AddNetscalerLoadBalancerCmd cmd) {
        String deviceName = cmd.getDeviceType();

        if (!isNetscalerDevice(deviceName)) {
            throw new InvalidParameterValueException("Invalid Netscaler device type");
        }

        URI uri;
        try {
            uri = new URI(cmd.getUrl());
        } catch (Exception e) {
            String msg = "Error parsing the url parameter specified in addNetscalerLoadBalancer command due to " + e.getMessage();
            s_logger.debug(msg);
            throw new InvalidParameterValueException(msg);
        }
        Map<String, String> configParams = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), false, configParams);
        boolean dedicatedUse =
            (configParams.get(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED) != null) ? Boolean.parseBoolean(configParams.get(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED))
                : false;

        if (dedicatedUse && !deviceName.equals(NetworkDevice.NetscalerVPXLoadBalancer.getName())) {
            String msg = "Only Netscaler VPX load balancers can be specified for dedicated use";
            s_logger.debug(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (cmd.isGslbProvider()) {

            if (!deviceName.equals(NetworkDevice.NetscalerVPXLoadBalancer.getName()) && !deviceName.equals(NetworkDevice.NetscalerMPXLoadBalancer.getName())) {
                String msg = "Only Netscaler VPX or MPX load balancers can be specified as GSLB service provider";
                s_logger.debug(msg);
                throw new InvalidParameterValueException(msg);
            }

            if (cmd.getSitePublicIp() == null || cmd.getSitePrivateIp() == null) {
                String msg = "Public and Privae IP needs to provided for NetScaler that will be GSLB provider";
                s_logger.debug(msg);
                throw new InvalidParameterValueException(msg);
            }

            if (dedicatedUse) {
                throw new InvalidParameterValueException("NetScaler provisioned to be GSLB service provider can only be configured for shared usage.");
            }

        }

        if (cmd.isExclusiveGslbProvider() && !cmd.isGslbProvider()) {
            throw new InvalidParameterValueException("NetScaler can be provisioned to be exclusive GSLB service provider" +
                " only if its being configured as GSLB service provider also.");
        }

        ExternalLoadBalancerDeviceVO lbDeviceVO =
            addExternalLoadBalancer(cmd.getPhysicalNetworkId(), cmd.getUrl(), cmd.getUsername(), cmd.getPassword(), deviceName, new NetscalerResource(),
                cmd.isGslbProvider(), cmd.isExclusiveGslbProvider(), cmd.getSitePublicIp(), cmd.getSitePrivateIp());

        return lbDeviceVO;
    }

    @Override
    public boolean deleteNetscalerLoadBalancer(DeleteNetscalerLoadBalancerCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();

        ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        if ((lbDeviceVo == null) || !isNetscalerDevice(lbDeviceVo.getDeviceName())) {
            throw new InvalidParameterValueException("No netscaler device found with ID: " + lbDeviceId);
        }

        return deleteExternalLoadBalancer(lbDeviceVo.getHostId());
    }

    @Override
    public ExternalLoadBalancerDeviceVO configureNetscalerLoadBalancer(ConfigureNetscalerLoadBalancerCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        Boolean dedicatedUse = cmd.getLoadBalancerDedicated();
        Long capacity = cmd.getLoadBalancerCapacity();
        List<Long> podIds = cmd.getPodIds();
        try {
            return configureNetscalerLoadBalancer(lbDeviceId, capacity, dedicatedUse, podIds);
        } catch (Exception e) {
            throw new CloudRuntimeException("failed to configure netscaler device due to " + e.getMessage());
        }
    }

    @DB
    private ExternalLoadBalancerDeviceVO configureNetscalerLoadBalancer(final long lbDeviceId, Long capacity, Boolean dedicatedUse, List<Long> newPodsConfig) {
        final ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        final Map<String, String> lbDetails = _detailsDao.findDetails(lbDeviceVo.getHostId());

        if ((lbDeviceVo == null) || !isNetscalerDevice(lbDeviceVo.getDeviceName())) {
            throw new InvalidParameterValueException("No netscaler device found with ID: " + lbDeviceId);
        }

        List<Long> currentPodsConfig = new ArrayList<Long>();
        List<NetScalerPodVO> currentPodVOs = _netscalerPodDao.listByNetScalerDeviceId(lbDeviceVo.getId());
        if (currentPodVOs != null && currentPodVOs.size() > 0) {
            for (NetScalerPodVO nsPodVo : currentPodVOs) {
                currentPodsConfig.add(nsPodVo.getPodId());
            }
        }

        final List<Long> podsToAssociate = new ArrayList<Long>();
        if (newPodsConfig != null && newPodsConfig.size() > 0) {
            for (Long podId : newPodsConfig) {
                HostPodVO pod = _podDao.findById(podId);
                if (pod == null) {
                    throw new InvalidParameterValueException("Can't find pod by id " + podId);
                }
            }

            for (Long podId : newPodsConfig) {
                if (!currentPodsConfig.contains(podId)) {
                    podsToAssociate.add(podId);
                }
            }
        }

        final List<Long> podsToDeassociate = new ArrayList<Long>();
        for (Long podId : currentPodsConfig) {
            if (!newPodsConfig.contains(podId)) {
                podsToDeassociate.add(podId);
            }
        }

        String deviceName = lbDeviceVo.getDeviceName();
        if (dedicatedUse != null || capacity != null) {
            if (NetworkDevice.NetscalerSDXLoadBalancer.getName().equalsIgnoreCase(deviceName) ||
                NetworkDevice.NetscalerMPXLoadBalancer.getName().equalsIgnoreCase(deviceName)) {
                if (dedicatedUse != null && dedicatedUse == true) {
                    throw new InvalidParameterValueException("Netscaler MPX and SDX device should be shared and can not be dedicated to a single account.");
                }
            }

            // check if any networks are using this netscaler device
            List<NetworkExternalLoadBalancerVO> networks = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
            if ((networks != null) && !networks.isEmpty()) {
                if (capacity != null && capacity < networks.size()) {
                    throw new CloudRuntimeException("There are more number of networks already using this netscaler device than configured capacity");
                }

                if (dedicatedUse != null && dedicatedUse == true) {
                    throw new CloudRuntimeException("There are networks already using this netscaler device to make device dedicated");
                }
            }
        }

        if (!NetworkDevice.NetscalerSDXLoadBalancer.getName().equalsIgnoreCase(deviceName)) {
            if (capacity != null) {
                lbDeviceVo.setCapacity(capacity);
            }
        } else {
            // FIXME how to interpret configured capacity of the SDX device
        }

        if (dedicatedUse != null) {
            lbDeviceVo.setIsDedicatedDevice(dedicatedUse);
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                _lbDeviceDao.update(lbDeviceId, lbDeviceVo);

                for (Long podId : podsToAssociate) {
                    NetScalerPodVO nsPodVo = new NetScalerPodVO(lbDeviceId, podId);
                    _netscalerPodDao.persist(nsPodVo);
                }

                for (Long podId : podsToDeassociate) {
                    NetScalerPodVO nsPodVo = _netscalerPodDao.findByPodId(podId);
                    _netscalerPodDao.remove(nsPodVo.getId());
                }

                // FIXME get the row lock to avoid race condition
                _detailsDao.persist(lbDeviceVo.getHostId(), lbDetails);

            }
        });
        HostVO host = _hostDao.findById(lbDeviceVo.getHostId());

        _agentMgr.reconnect(host.getId());
        return lbDeviceVo;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddNetscalerLoadBalancerCmd.class);
        cmdList.add(ConfigureNetscalerLoadBalancerCmd.class);
        cmdList.add(DeleteNetscalerLoadBalancerCmd.class);
        cmdList.add(ListNetscalerLoadBalancerNetworksCmd.class);
        cmdList.add(ListNetscalerLoadBalancersCmd.class);
        cmdList.add(RegisterServicePackageCmd.class);
        cmdList.add(RegisterNetscalerControlCenterCmd.class);

        return cmdList;
    }

    @Override
    public List<? extends Network> listNetworks(ListNetscalerLoadBalancerNetworksCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        List<NetworkVO> networks = new ArrayList<NetworkVO>();

        ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        if (lbDeviceVo == null || !isNetscalerDevice(lbDeviceVo.getDeviceName())) {
            throw new InvalidParameterValueException("Could not find Netscaler load balancer device with ID " + lbDeviceId);
        }

        List<NetworkExternalLoadBalancerVO> networkLbMaps = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
        if (networkLbMaps != null && !networkLbMaps.isEmpty()) {
            for (NetworkExternalLoadBalancerVO networkLbMap : networkLbMaps) {
                NetworkVO network = _networkDao.findById(networkLbMap.getNetworkId());
                networks.add(network);
            }
        }

        return networks;
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listNetscalerLoadBalancers(ListNetscalerLoadBalancersCmd cmd) {
        Long physcialNetworkId = cmd.getPhysicalNetworkId();
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        PhysicalNetworkVO pNetwork = null;
        List<ExternalLoadBalancerDeviceVO> lbDevices = new ArrayList<ExternalLoadBalancerDeviceVO>();

        if (physcialNetworkId == null && lbDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or load balancer device Id must be specified");
        }

        if (lbDeviceId != null) {
            ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
            if (lbDeviceVo == null || !isNetscalerDevice(lbDeviceVo.getDeviceName())) {
                throw new InvalidParameterValueException("Could not find Netscaler load balancer device with ID: " + lbDeviceId);
            }
            lbDevices.add(lbDeviceVo);
            return lbDevices;
        }

        if (physcialNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physcialNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physcialNetworkId);
            }
            lbDevices = _lbDeviceDao.listByPhysicalNetworkAndProvider(physcialNetworkId, Provider.Netscaler.getName());
            return lbDevices;
        }

        return null;
    }

    @Override
    public NetscalerLoadBalancerResponse createNetscalerLoadBalancerResponse(ExternalLoadBalancerDeviceVO lbDeviceVO) {
        NetscalerLoadBalancerResponse response = new NetscalerLoadBalancerResponse();
        Host lbHost = _hostDao.findById(lbDeviceVO.getHostId());
        Map<String, String> lbDetails = _detailsDao.findDetails(lbDeviceVO.getHostId());

        response.setId(lbDeviceVO.getUuid());
        response.setIpAddress(lbHost.getPrivateIpAddress());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(lbDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setPublicInterface(lbDetails.get("publicInterface"));
        response.setPrivateInterface(lbDetails.get("privateInterface"));
        response.setDeviceName(lbDeviceVO.getDeviceName());
        if (lbDeviceVO.getCapacity() == 0) {
            long defaultLbCapacity = NumbersUtil.parseLong(_configDao.getValue(Config.DefaultExternalLoadBalancerCapacity.key()), 50);
            response.setDeviceCapacity(defaultLbCapacity);
        } else {
            response.setDeviceCapacity(lbDeviceVO.getCapacity());
        }
        response.setDedicatedLoadBalancer(lbDeviceVO.getIsDedicatedDevice());
        response.setProvider(lbDeviceVO.getProviderName());
        response.setDeviceState(lbDeviceVO.getState().name());
        response.setObjectName("netscalerloadbalancer");

        response.setGslbProvider(lbDeviceVO.getGslbProvider());
        response.setExclusiveGslbProvider(lbDeviceVO.getExclusiveGslbProvider());
        response.setGslbSitePublicIp(lbDeviceVO.getGslbSitePublicIP());
        response.setGslbSitePrivateIp(lbDeviceVO.getGslbSitePrivateIP());

        List<Long> associatedPods = new ArrayList<Long>();
        List<NetScalerPodVO> currentPodVOs = _netscalerPodDao.listByNetScalerDeviceId(lbDeviceVO.getId());
        if (currentPodVOs != null && currentPodVOs.size() > 0) {
            for (NetScalerPodVO nsPodVo : currentPodVOs) {
                associatedPods.add(nsPodVo.getPodId());
            }
        }
        response.setAssociatedPods(associatedPods);
        return response;
    }

    @Override
    public Provider getProvider() {
        return Provider.Netscaler;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        List<ExternalLoadBalancerDeviceVO> lbDevices = _lbDeviceDao.listByPhysicalNetworkAndProvider(provider.getPhysicalNetworkId(), Provider.Netscaler.getName());

        // true if at-least one Netscaler device is added in to physical network and is in configured (in enabled state)
        // state
        if (lbDevices != null && !lbDevices.isEmpty()) {
            for (ExternalLoadBalancerDeviceVO lbDevice : lbDevices) {
                if (lbDevice.getState() == LBDeviceState.Enabled) {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        // TODO reset the configuration on all of the netscaler devices in this physical network
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    private boolean isNetscalerDevice(String deviceName) {
        if ((deviceName == null) ||
            ((!deviceName.equalsIgnoreCase(NetworkDevice.NetscalerMPXLoadBalancer.getName())) &&
                (!deviceName.equalsIgnoreCase(NetworkDevice.NetscalerSDXLoadBalancer.getName())) && (!deviceName.equalsIgnoreCase(NetworkDevice.NetscalerVPXLoadBalancer.getName())))) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        Set<Service> netscalerServices = new HashSet<Service>();
        netscalerServices.add(Service.Lb);
        netscalerServices.add(Service.StaticNat);

        // NetScaler can only act as Lb and Static Nat service provider
        if (services != null && !services.isEmpty() && !netscalerServices.containsAll(services)) {
            s_logger.warn("NetScaler network element can only support LB and Static NAT services and service combination " + services + " is not supported.");

            StringBuffer buff = new StringBuffer();
            for (Service service : services) {
                buff.append(service.getName());
                buff.append(" ");
            }
            s_logger.warn("NetScaler network element can only support LB and Static NAT services and service combination " + buff.toString() + " is not supported.");
            s_logger.warn("NetScaler network element can only support LB and Static NAT services and service combination " + services + " is not supported.");
            return false;
        }

        return true;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> service) throws ResourceUnavailableException {
        // return true, as IP will be associated as part of LB rule configuration
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {

        if (_networkMgr.isNetworkInlineMode(network)) {
            return getIpDeployerForInlineMode(network);
        }

        return this;
    }

    public boolean applyElasticLoadBalancerRules(Network network, List<LoadBalancingRule> loadBalancingRules) throws ResourceUnavailableException {

        if (loadBalancingRules == null || loadBalancingRules.isEmpty()) {
            return true;
        }

        String errMsg = null;
        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);
        if (lbDeviceVO == null) {
            try {
                lbDeviceVO = allocateLoadBalancerForNetwork(network);
            } catch (Exception e) {
                errMsg = "Could not allocate a NetSclaer load balancer for configuring elastic load balancer rules due to " + e.getMessage();
                s_logger.error(errMsg);
                throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
            }
        }

        if (!isNetscalerDevice(lbDeviceVO.getDeviceName())) {
            errMsg = "There are no NetScaler load balancer assigned for this network. So NetScaler element can not be handle elastic load balancer rules.";
            s_logger.error(errMsg);
            throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
        }

        List<LoadBalancerTO> loadBalancersToApply = new ArrayList<LoadBalancerTO>();
        for (int i = 0; i < loadBalancingRules.size(); i++) {
            LoadBalancingRule rule = loadBalancingRules.get(i);
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String lbUuid = rule.getUuid();
            String srcIp = rule.getSourceIp().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();

            if ((destinations != null && !destinations.isEmpty()) || rule.isAutoScaleConfig()) {
                LoadBalancerTO loadBalancer =
                    new LoadBalancerTO(lbUuid, srcIp, srcPort, protocol, algorithm, revoked, false, false, destinations, rule.getStickinessPolicies(),
                        rule.getHealthCheckPolicies(), rule.getLbSslCert(), rule.getLbProtocol());
                if (rule.isAutoScaleConfig()) {
                    loadBalancer.setAutoScaleVmGroup(rule.getAutoScaleVmGroup());
                }
                loadBalancersToApply.add(loadBalancer);
            }
        }

        if (loadBalancersToApply.size() > 0) {
            int numLoadBalancersForCommand = loadBalancersToApply.size();
            LoadBalancerTO[] loadBalancersForCommand = loadBalancersToApply.toArray(new LoadBalancerTO[numLoadBalancersForCommand]);
            LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(loadBalancersForCommand, null);

            HostVO externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                String details = (answer != null) ? answer.getDetails() : "details unavailable";
                String msg =
                    "Unable to apply elastic load balancer rules to the external load balancer appliance in zone " + network.getDataCenterId() + " due to: " + details +
                        ".";
                s_logger.error(msg);
                throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
            }
        }

        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {

        if (!canHandle(config, Service.StaticNat)) {
            return false;
        }

        boolean multiNetScalerDeployment = Boolean.valueOf(_configDao.getValue(Config.EIPWithMultipleNetScalersEnabled.key()));

        try {
            if (!multiNetScalerDeployment) {
                String errMsg;
                ExternalLoadBalancerDeviceVO lbDevice = getExternalLoadBalancerForNetwork(config);
                if (lbDevice == null) {
                    try {
                        lbDevice = allocateLoadBalancerForNetwork(config);
                    } catch (Exception e) {
                        errMsg = "Could not allocate a NetSclaer load balancer for configuring static NAT rules due to" + e.getMessage();
                        s_logger.error(errMsg);
                        throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
                    }
                }

                if (!isNetscalerDevice(lbDevice.getDeviceName())) {
                    errMsg = "There are no NetScaler load balancer assigned for this network. So NetScaler element will not be handling the static nat rules.";
                    s_logger.error(errMsg);
                    throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
                }
                SetStaticNatRulesAnswer answer = null;
                List<StaticNatRuleTO> rulesTO = null;
                if (rules != null) {
                    rulesTO = new ArrayList<StaticNatRuleTO>();
                    for (StaticNat rule : rules) {
                        IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                        StaticNatRuleTO ruleTO =
                            new StaticNatRuleTO(0, sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(), false);
                        rulesTO.add(ruleTO);
                    }
                }

                SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, null);
                answer = (SetStaticNatRulesAnswer)_agentMgr.send(lbDevice.getHostId(), cmd);
                if (answer == null) {
                    return false;
                } else {
                    return answer.getResult();
                }
            } else {
                if (rules != null) {
                    for (StaticNat rule : rules) {
                        // validate if EIP rule can be configured.
                        ExternalLoadBalancerDeviceVO lbDevice = getNetScalerForEIP(rule);
                        if (lbDevice == null) {
                            String errMsg = "There is no NetScaler device configured to perform EIP to guest IP address: " + rule.getDestIpAddress();
                            s_logger.error(errMsg);
                            throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
                        }

                        List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
                        IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                        StaticNatRuleTO ruleTO =
                            new StaticNatRuleTO(0, sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(), false);
                        rulesTO.add(ruleTO);
                        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, null);

                        // send commands to configure INAT rule on the NetScaler device
                        SetStaticNatRulesAnswer answer = (SetStaticNatRulesAnswer)_agentMgr.send(lbDevice.getHostId(), cmd);
                        if (answer == null) {
                            String errMsg = "Failed to configure INAT rule on NetScaler device " + lbDevice.getHostId();
                            s_logger.error(errMsg);
                            throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
                        }
                    }
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            s_logger.error("Failed to configure StaticNat rule due to " + e.getMessage());
            return false;
        }
    }

    // returns configured NetScaler device that is associated with the pod that owns guest IP
    private ExternalLoadBalancerDeviceVO getNetScalerForEIP(StaticNat rule) {
        String guestIP = rule.getDestIpAddress();
        List<DataCenterIpAddressVO> dcGuestIps = _privateIpAddressDao.listAll();
        if (dcGuestIps != null) {
            for (DataCenterIpAddressVO dcGuestIp : dcGuestIps) {
                if (dcGuestIp.getIpAddress().equalsIgnoreCase(guestIP)) {
                    long podId = dcGuestIp.getPodId();
                    NetScalerPodVO nsPodVO = _netscalerPodDao.findByPodId(podId);
                    if (nsPodVO != null) {
                        ExternalLoadBalancerDeviceVO lbDeviceVO = _lbDeviceDao.findById(nsPodVO.getNetscalerDeviceId());
                        return lbDeviceVO;
                    }
                }
            }
        }
        return null;
    }

    public List<LoadBalancerTO> getElasticLBRulesHealthCheck(Network network, List<LoadBalancingRule> loadBalancingRules) throws ResourceUnavailableException {

        HealthCheckLBConfigAnswer answer = null;

        if (loadBalancingRules == null || loadBalancingRules.isEmpty()) {
            return null;
        }

        String errMsg = null;
        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);

        if (lbDeviceVO == null) {
            s_logger.warn("There is no external load balancer device assigned to this network either network is not implement are already shutdown so just returning");
            return null;
        }

        if (!isNetscalerDevice(lbDeviceVO.getDeviceName())) {
            errMsg = "There are no NetScaler load balancer assigned for this network. So NetScaler element can not be handle elastic load balancer rules.";
            s_logger.error(errMsg);
            throw new ResourceUnavailableException(errMsg, this.getClass(), 0);
        }

        List<LoadBalancerTO> loadBalancersToApply = new ArrayList<LoadBalancerTO>();
        for (int i = 0; i < loadBalancingRules.size(); i++) {
            LoadBalancingRule rule = loadBalancingRules.get(i);
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String lbUuid = rule.getUuid();
            String srcIp = rule.getSourceIp().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();

            if ((destinations != null && !destinations.isEmpty()) || rule.isAutoScaleConfig()) {
                LoadBalancerTO loadBalancer =
                    new LoadBalancerTO(lbUuid, srcIp, srcPort, protocol, algorithm, revoked, false, false, destinations, null, rule.getHealthCheckPolicies(),
                        rule.getLbSslCert(), rule.getLbProtocol());
                loadBalancersToApply.add(loadBalancer);
            }
        }

        if (loadBalancersToApply.size() > 0) {
            int numLoadBalancersForCommand = loadBalancersToApply.size();
            LoadBalancerTO[] loadBalancersForCommand = loadBalancersToApply.toArray(new LoadBalancerTO[numLoadBalancersForCommand]);
            HealthCheckLBConfigCommand cmd = new HealthCheckLBConfigCommand(loadBalancersForCommand);
            HostVO externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            answer = (HealthCheckLBConfigAnswer)_agentMgr.easySend(externalLoadBalancer.getId(), cmd);
            return answer.getLoadBalancers();
        }
        return null;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules) {

        if (canHandle(network, Service.Lb) && canHandleLbRules(lbrules)) {
            try {

                if (isBasicZoneNetwok(network)) {
                    return getElasticLBRulesHealthCheck(network, lbrules);
                } else {
                    return getLBHealthChecks(network, lbrules);
                }
            } catch (ResourceUnavailableException e) {
                s_logger.error("Error in getting the LB Rules from NetScaler " + e);
            }
        } else {
            s_logger.error("Network cannot handle to LB service ");
        }
        return null;
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return true;
    }

    @Override
    public List<LoadBalancerTO> getLBHealthChecks(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        return super.getLBHealthChecks(network, rules);
    }

    @Override
    public boolean applyGlobalLoadBalancerRule(long zoneId, long physicalNetworkId, GlobalLoadBalancerConfigCommand gslbConfigCmd) throws ResourceUnavailableException {

        long zoneGslbProviderHosId = 0;

        // find the NetScaler device configured as gslb service provider in the zone
        ExternalLoadBalancerDeviceVO nsGslbProvider = findGslbProvider(zoneId, physicalNetworkId);
        if (nsGslbProvider == null) {
            String msg = "Unable to find a NetScaler configured as gslb service provider in zone " + zoneId;
            s_logger.debug(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        // get the host Id corresponding to NetScaler acting as GSLB service provider in the zone
        zoneGslbProviderHosId = nsGslbProvider.getHostId();

        // send gslb configuration to NetScaler device
        Answer answer = _agentMgr.easySend(zoneGslbProviderHosId, gslbConfigCmd);
        if (answer == null || !answer.getResult()) {
            String msg = "Unable to apply global load balancer rule to the gslb service provider in zone " + zoneId;
            s_logger.debug(msg);
            throw new ResourceUnavailableException(msg, DataCenter.class, zoneId);
        }

        return true;
    }

    private ExternalLoadBalancerDeviceVO findGslbProvider(long zoneId, long physicalNetworkId) {
        List<PhysicalNetworkVO> pNtwks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, TrafficType.Guest);

        if (pNtwks == null || pNtwks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to get physical network: " + physicalNetworkId + " in zone id = " + zoneId);
        } else {
            for (PhysicalNetwork physicalNetwork : pNtwks) {
                if (physicalNetwork.getId() == physicalNetworkId) {
                    PhysicalNetworkVO physNetwork = pNtwks.get(0);
                    ExternalLoadBalancerDeviceVO nsGslbProvider =
                        _externalLoadBalancerDeviceDao.findGslbServiceProvider(physNetwork.getId(), Provider.Netscaler.getName());
                    return nsGslbProvider;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isServiceEnabledInZone(long zoneId, long physicalNetworkId) {

        ExternalLoadBalancerDeviceVO nsGslbProvider = findGslbProvider(zoneId, physicalNetworkId);
        //return true if a NetScaler device is configured in the zone
        return (nsGslbProvider != null);
    }

    @Override
    public String getZoneGslbProviderPublicIp(long zoneId, long physicalNetworkId) {
        ExternalLoadBalancerDeviceVO nsGslbProvider = findGslbProvider(zoneId, physicalNetworkId);
        if (nsGslbProvider != null) {
            return nsGslbProvider.getGslbSitePublicIP();
        }
        return null;
    }

    @Override
    public String getZoneGslbProviderPrivateIp(long zoneId, long physicalNetworkId) {
        ExternalLoadBalancerDeviceVO nsGslbProvider = findGslbProvider(zoneId, physicalNetworkId);
        if (nsGslbProvider != null) {
            return nsGslbProvider.getGslbSitePrivateIP();
        }
        return null;
    }

    private boolean canHandleLbRules(List<LoadBalancingRule> rules) {
        Map<Capability, String> lbCaps = this.getCapabilities().get(Service.Lb);
        if (!lbCaps.isEmpty()) {
            String schemeCaps = lbCaps.get(Capability.LbSchemes);
            if (schemeCaps != null) {
                for (LoadBalancingRule rule : rules) {
                    if (!schemeCaps.contains(rule.getScheme().toString())) {
                        s_logger.debug("Scheme " + rules.get(0).getScheme() + " is not supported by the provider " + this.getName());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public NetScalerServicePackageResponse registerNetscalerServicePackage(RegisterServicePackageCmd cmd) {
        NetScalerServicePackageVO servicePackage = new NetScalerServicePackageVO(cmd);
        NetScalerServicePackageResponse response = null;
        _netscalerServicePackageDao.persist(servicePackage);
        response = new NetScalerServicePackageResponse(servicePackage);
        return response;
    }

    @Override
    public NetScalerServicePackageResponse deleteNetscalerServicePackage(RegisterServicePackageCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetScalerServicePackageResponse listNetscalerServicePackage(RegisterServicePackageCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetScalerServicePackageResponse createNetscalerServicePackageResponse(NetScalerServicePackageVO servicePackageVO) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @DB
    public NetScalerControlCenterVO registerNetscalerControlCenter(RegisterNetscalerControlCenterCmd cmd) {
        // make a connection and then check it.
        final RegisterNetscalerControlCenterCmd cmdinfo = cmd;
        String ipAddress = cmd.getIpaddress();
        Map hostDetails = new HashMap<String, String>();
        String hostName =  "NetscalerControlCenter";//getExternalLoadBalancerResourceGuid("NetscalerControlCenter");
        hostDetails.put("name", hostName);
        hostDetails.put("guid", UUID.randomUUID().toString());
        //TODO change this zoneid to dynamic value
        hostDetails.put("zoneId", "1");
        hostDetails.put("ip", ipAddress);
        hostDetails.put("username", cmd.getUsername());
        hostDetails.put("password", cmd.getPassword());
        hostDetails.put("deviceName", "netscaler control center");

        try {
            ServerResource resource = new NetScalerControlCenterResource();
            resource.configure(hostName, hostDetails);

            final Host host = _resourceMgr.addHost(1, resource, Host.Type.NetScalerControlCenter, hostDetails);
            if (host != null) {

                return Transaction.execute(new TransactionCallback<NetScalerControlCenterVO>() {
                    @Override
                    public NetScalerControlCenterVO doInTransaction(TransactionStatus status) {
                NetScalerControlCenterVO nccVO = new NetScalerControlCenterVO(host.getId(), cmdinfo.getUsername(), cmdinfo.getPassword(),
                        cmdinfo.getIpaddress(), cmdinfo.getNumretries());
                        _netscalerControlCenterDao.persist(nccVO);
                        DetailVO hostDetail = new DetailVO(host.getId(), ApiConstants.NETSCALER_CONTROLCENTER_ID , String.valueOf(nccVO.getId()));
                        _hostDetailDao.persist(hostDetail);
                        return nccVO;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add load balancer device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }
}
