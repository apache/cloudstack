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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.network.topology.NetworkTopology;
import org.apache.cloudstack.network.topology.NetworkTopologyContext;
import org.apache.log4j.Logger;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupOvsCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;

public class OvsElement extends AdapterBase implements NetworkElement,
OvsElementService, ConnectivityProvider, ResourceStateAdapter,
PortForwardingServiceProvider, LoadBalancingServiceProvider, NetworkMigrationResponder,
StaticNatServiceProvider, IpDeployer {
    @Inject
    OvsTunnelManager _ovsTunnelMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;

    @Inject
    NetworkTopologyContext _networkTopologyContext;

    private static final Logger s_logger = Logger.getLogger(OvsElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.Ovs;
    }

    protected boolean canHandle(final Network network, final Service service) {
        s_logger.debug("Checking if OvsElement can handle service "
                + service.getName() + " on network " + network.getDisplayText());
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vswitch) {
            return false;
        }

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("OvsElement is not a provider for network "
                    + network.getDisplayText());
            return false;
        }

        if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(),
                service, Network.Provider.Ovs)) {
            s_logger.debug("OvsElement can't provide the " + service.getName()
                    + " service on network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }

    @Override
    public boolean implement(final Network network, final NetworkOffering offering,
            final DeployDestination dest, final ReservationContext context)
                    throws ConcurrentOperationException, ResourceUnavailableException,
                    InsufficientCapacityException {
        s_logger.debug("entering OvsElement implement function for network "
                + network.getDisplayText() + " (state " + network.getState()
                + ")");

        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean prepare(final Network network, final NicProfile nic,
            final VirtualMachineProfile vm,
            final DeployDestination dest, final ReservationContext context)
                    throws ConcurrentOperationException, ResourceUnavailableException,
                    InsufficientCapacityException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
            return false;
        }

        if (nic.getTrafficType() != Networks.TrafficType.Guest) {
            return false;
        }

        if (vm.getType() != VirtualMachine.Type.User && vm.getType() != VirtualMachine.Type.DomainRouter) {
            return false;
        }

        // prepare the tunnel network on the host, in order for VM to get launched
        _ovsTunnelMgr.checkAndPrepareHostForTunnelNetwork(network, dest.getHost());

        return true;
    }

    @Override
    public boolean release(final Network network, final NicProfile nic,
            final VirtualMachineProfile vm,
            final ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }
        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
            return false;
        }

        if (nic.getTrafficType() != Networks.TrafficType.Guest) {
            return false;
        }

        final HostVO host = _hostDao.findById(vm.getVirtualMachine().getHostId());
        _ovsTunnelMgr.checkAndRemoveHostFromTunnelNetwork(network, host);
        return true;
    }

    @Override
    public boolean shutdown(final Network network, final ReservationContext context,
            final boolean cleanup) throws ConcurrentOperationException,
            ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean destroy(final Network network, final ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isReady(final PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(
            final PhysicalNetworkServiceProvider provider, final ReservationContext context)
                    throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(final Set<Service> services) {
        if (!services.contains(Service.Connectivity)) {
            s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
            return false;
        }

        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        final Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // L2 Support : SDN provisioning
        final Map<Capability, String> connectivityCapabilities = new HashMap<Capability, String>();
        connectivityCapabilities.put(Capability.DistributedRouter, null);
        connectivityCapabilities.put(Capability.StretchedL2Subnet, null);
        connectivityCapabilities.put(Capability.RegionLevelVpc, null);
        capabilities.put(Service.Connectivity, connectivityCapabilities);


        // L3 Support : Port Forwarding
        capabilities.put(Service.PortForwarding, null);

        // L3 support : StaticNat
        capabilities.put(Service.StaticNat, null);

        // L3 support : Load Balancer
        // Set capabilities for LB service
        final Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp");
        lbCapabilities.put(Capability.SupportedStickinessMethods, getHAProxyStickinessCapability());
        lbCapabilities.put(Capability.LbSchemes, LoadBalancerContainer.Scheme.Public.toString());

        capabilities.put(Service.Lb, lbCapabilities);

        return capabilities;
    }

    public static String getHAProxyStickinessCapability() {
        LbStickinessMethod method;
        final List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>(1);

        method = new LbStickinessMethod(StickinessMethodType.LBCookieBased, "This is loadbalancer cookie based stickiness method.");
        method.addParam("cookie-name", false, "Cookie name passed in http header by the LB to the client.", false);
        method.addParam("mode", false,
                "Valid values: insert, rewrite, prefix. Default value: insert.  In the insert mode cookie will be created" +
                        " by the LB. In other modes, cookie will be created by the server and LB modifies it.", false);
        method.addParam(
                "nocache",
                false,
                "This option is recommended in conjunction with the insert mode when there is a cache between the client" +
                        " and HAProxy, as it ensures that a cacheable response will be tagged non-cacheable if  a cookie needs " +
                        "to be inserted. This is important because if all persistence cookies are added on a cacheable home page" +
                        " for instance, then all customers will then fetch the page from an outer cache and will all share the " +
                        "same persistence cookie, leading to one server receiving much more traffic than others. See also the " +
                        "insert and postonly options. ",
                        true);
        method.addParam(
                "indirect",
                false,
                "When this option is specified in insert mode, cookies will only be added when the server was not reached" +
                        " after a direct access, which means that only when a server is elected after applying a load-balancing algorithm," +
                        " or after a redispatch, then the cookie  will be inserted. If the client has all the required information" +
                        " to connect to the same server next time, no further cookie will be inserted. In all cases, when the " +
                        "indirect option is used in insert mode, the cookie is always removed from the requests transmitted to " +
                        "the server. The persistence mechanism then becomes totally transparent from the application point of view.",
                        true);
        method.addParam(
                "postonly",
                false,
                "This option ensures that cookie insertion will only be performed on responses to POST requests. It is an" +
                        " alternative to the nocache option, because POST responses are not cacheable, so this ensures that the " +
                        "persistence cookie will never get cached.Since most sites do not need any sort of persistence before the" +
                        " first POST which generally is a login request, this is a very efficient method to optimize caching " +
                        "without risking to find a persistence cookie in the cache. See also the insert and nocache options.",
                        true);
        method.addParam(
                "domain",
                false,
                "This option allows to specify the domain at which a cookie is inserted. It requires exactly one parameter:" +
                        " a valid domain name. If the domain begins with a dot, the browser is allowed to use it for any host " +
                        "ending with that name. It is also possible to specify several domain names by invoking this option multiple" +
                        " times. Some browsers might have small limits on the number of domains, so be careful when doing that. " +
                        "For the record, sending 10 domains to MSIE 6 or Firefox 2 works as expected.",
                        false);
        methodList.add(method);

        method = new LbStickinessMethod(StickinessMethodType.AppCookieBased,
                "This is App session based sticky method. Define session stickiness on an existing application cookie. " +
                "It can be used only for a specific http traffic");
        method.addParam("cookie-name", false, "This is the name of the cookie used by the application and which LB will " +
                "have to learn for each new session. Default value: Auto geneared based on ip", false);
        method.addParam("length", false, "This is the max number of characters that will be memorized and checked in " +
                "each cookie value. Default value:52", false);
        method.addParam(
                "holdtime",
                false,
                "This is the time after which the cookie will be removed from memory if unused. The value should be in " +
                        "the format Example : 20s or 30m  or 4h or 5d . only seconds(s), minutes(m) hours(h) and days(d) are valid," +
                        " cannot use th combinations like 20h30m. Default value:3h ",
                        false);
        method.addParam(
                "request-learn",
                false,
                "If this option is specified, then haproxy will be able to learn the cookie found in the request in case the server does not specify any in response. This is typically what happens with PHPSESSID cookies, or when haproxy's session expires before the application's session and the correct server is selected. It is recommended to specify this option to improve reliability",
                true);
        method.addParam(
                "prefix",
                false,
                "When this option is specified, haproxy will match on the cookie prefix (or URL parameter prefix). "
                        +
                        "The appsession value is the data following this prefix. Example : appsession ASPSESSIONID len 64 timeout 3h prefix  This will match the cookie ASPSESSIONIDXXXX=XXXXX, the appsession value will be XXXX=XXXXX.",
                        true);
        method.addParam(
                "mode",
                false,
                "This option allows to change the URL parser mode. 2 modes are currently supported : - path-parameters " +
                        ": The parser looks for the appsession in the path parameters part (each parameter is separated by a semi-colon), " +
                        "which is convenient for JSESSIONID for example.This is the default mode if the option is not set. - query-string :" +
                        " In this mode, the parser will look for the appsession in the query string.",
                        false);
        methodList.add(method);

        method = new LbStickinessMethod(StickinessMethodType.SourceBased, "This is source based Stickiness method, " +
                "it can be used for any type of protocol.");
        method.addParam("tablesize", false, "Size of table to store source ip addresses. example: tablesize=200k or 300m" +
                " or 400g. Default value:200k", false);
        method.addParam("expire", false, "Entry in source ip table will expire after expire duration. units can be s,m,h,d ." +
                " example: expire=30m 20s 50h 4d. Default value:3h", false);
        methodList.add(method);

        final Gson gson = new Gson();
        final String capability = gson.toJson(methodList);
        return capability;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        return cmdList;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(final HostVO host,
            final StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(final HostVO host,
            final StartupCommand[] startup, final ServerResource resource,
            final Map<String, String> details, final List<String> hostTags) {
        if (!(startup[0] instanceof StartupOvsCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(final HostVO host, final boolean isForced,
            final boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public IpDeployer getIpDeployer(final Network network) {
        return this;
    }

    @Override
    public boolean applyIps(final Network network,
            final List<? extends PublicIpAddress> ipAddress, final Set<Service> services)
                    throws ResourceUnavailableException {
        boolean canHandle = true;
        for (final Service service : services) {
            // check if Ovs can handle services except SourceNat & Firewall
            if (!canHandle(network, service) && service != Service.SourceNat && service != Service.Firewall) {
                canHandle = false;
                break;
            }
        }
        boolean result = true;
        if (canHandle) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(
                    network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router element doesn't need to associate ip addresses on the backend; virtual "
                        + "router doesn't exist in the network "
                        + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.associatePublicIP(network, ipAddress, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public boolean applyStaticNats(final Network network, final List<? extends StaticNat> rules)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.StaticNat)) {
            return false;
        }
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(
                network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Ovs element doesn't need to apply static nat on the backend; virtual "
                    + "router doesn't exist in the network " + network.getId());
            return true;
        }

        final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
        final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);
        boolean result = true;
        for (final DomainRouterVO domainRouterVO : routers) {
            result = result && networkTopology.applyStaticNats(network, rules, domainRouterVO);
        }
        return result;
    }

    @Override
    public boolean applyPFRules(final Network network, final List<PortForwardingRule> rules)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(
                network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Ovs element doesn't need to apply firewall rules on the backend; virtual "
                    + "router doesn't exist in the network " + network.getId());
            return true;
        }

        boolean result = true;
        final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
        final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);
        for (final DomainRouterVO domainRouterVO : routers) {
            result = result && networkTopology.applyFirewallRules(network, rules, domainRouterVO);
        }
        return result;
    }

    @Override
    public boolean applyLBRules(final Network network, final List<LoadBalancingRule> rules)
            throws ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.Lb)) {
            if (!canHandleLbRules(rules)) {
                return false;
            }

            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(
                    network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply load balancing rules on the backend; virtual "
                        + "router doesn't exist in the network "
                        + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyLoadBalancingRules(network, rules, domainRouterVO);
                if (!result) {
                    s_logger.debug("Failed to apply load balancing rules in network " + network.getId());
                }
            }
        }
        return result;
    }

    @Override
    public boolean validateLBRule(final Network network, final LoadBalancingRule rule) {
        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        rules.add(rule);
        if (canHandle(network, Service.Lb) && canHandleLbRules(rules)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(
                    network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                return true;
            }
            return validateHAProxyLBRule(rule);
        }
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(final Network network,
            final List<LoadBalancingRule> lbrules) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return true;
    }

    private boolean canHandleLbRules(final List<LoadBalancingRule> rules) {
        final Map<Capability, String> lbCaps = getCapabilities().get(Service.Lb);
        if (!lbCaps.isEmpty()) {
            final String schemeCaps = lbCaps.get(Capability.LbSchemes);
            if (schemeCaps != null) {
                for (final LoadBalancingRule rule : rules) {
                    if (!schemeCaps.contains(rule.getScheme().toString())) {
                        s_logger.debug("Scheme " + rules.get(0).getScheme()
                                + " is not supported by the provider "
                                + getName());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean validateHAProxyLBRule(final LoadBalancingRule rule) {
        final String timeEndChar = "dhms";

        for (final LbStickinessPolicy stickinessPolicy : rule.getStickinessPolicies()) {
            final List<Pair<String, String>> paramsList = stickinessPolicy
                    .getParams();

            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(
                    stickinessPolicy.getMethodName())) {

            } else if (StickinessMethodType.SourceBased.getName()
                    .equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String tablesize = "200k"; // optional
                String expire = "30m"; // optional

                /* overwrite default values with the stick parameters */
                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("tablesize".equalsIgnoreCase(key)) {
                        tablesize = value;
                    }
                    if ("expire".equalsIgnoreCase(key)) {
                        expire = value;
                    }
                }
                if (expire != null
                        && !containsOnlyNumbers(expire, timeEndChar)) {
                    throw new InvalidParameterValueException(
                            "Failed LB in validation rule id: " + rule.getId()
                            + " Cause: expire is not in timeformat: "
                            + expire);
                }
                if (tablesize != null
                        && !containsOnlyNumbers(tablesize, "kmg")) {
                    throw new InvalidParameterValueException(
                            "Failed LB in validation rule id: "
                                    + rule.getId()
                                    + " Cause: tablesize is not in size format: "
                                    + tablesize);

                }
            } else if (StickinessMethodType.AppCookieBased.getName()
                    .equalsIgnoreCase(stickinessPolicy.getMethodName())) {
                String length = null; // optional
                String holdTime = null; // optional

                for (final Pair<String, String> paramKV : paramsList) {
                    final String key = paramKV.first();
                    final String value = paramKV.second();
                    if ("length".equalsIgnoreCase(key)) {
                        length = value;
                    }
                    if ("holdtime".equalsIgnoreCase(key)) {
                        holdTime = value;
                    }
                }

                if (length != null && !containsOnlyNumbers(length, null)) {
                    throw new InvalidParameterValueException(
                            "Failed LB in validation rule id: " + rule.getId()
                            + " Cause: length is not a number: "
                            + length);
                }
                if (holdTime != null
                        && !containsOnlyNumbers(holdTime, timeEndChar) && !containsOnlyNumbers(
                                holdTime, null)) {
                    throw new InvalidParameterValueException(
                            "Failed LB in validation rule id: " + rule.getId()
                            + " Cause: holdtime is not in timeformat: "
                            + holdTime);
                }
            }
        }
        return true;
    }

    /*
     * This function detects numbers like 12 ,32h ,42m .. etc,. 1) plain number
     * like 12 2) time or tablesize like 12h, 34m, 45k, 54m , here last
     * character is non-digit but from known characters .
     */
    private static boolean containsOnlyNumbers(final String str, final String endChar) {
        if (str == null) {
            return false;
        }

        String number = str;
        if (endChar != null) {
            boolean matchedEndChar = false;
            if (str.length() < 2)
            {
                return false; // at least one numeric and one char. example:
            }
            // 3h
            final char strEnd = str.toCharArray()[str.length() - 1];
            for (final char c : endChar.toCharArray()) {
                if (strEnd == c) {
                    number = str.substring(0, str.length() - 1);
                    matchedEndChar = true;
                    break;
                }
            }
            if (!matchedEndChar) {
                return false;
            }
        }
        try {
            Integer.parseInt(number);
        } catch (final NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean prepareMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context) {
        if (!canHandle(network, Service.Connectivity)) {
            return false;
        }

        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
            return false;
        }

        if (nic.getTrafficType() != Networks.TrafficType.Guest) {
            return false;
        }

        if (vm.getType() != VirtualMachine.Type.User && vm.getType() != VirtualMachine.Type.DomainRouter) {
            return false;
        }

        // prepare the tunnel network on the host, in order for VM to get launched
        _ovsTunnelMgr.checkAndPrepareHostForTunnelNetwork(network, dest.getHost());

        return true;
    }

    @Override
    public void rollbackMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final ReservationContext src, final ReservationContext dst) {
        return;
    }

    @Override
    public void commitMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final ReservationContext src, final ReservationContext dst) {
        return;
    }
}
