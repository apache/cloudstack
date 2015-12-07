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

import org.apache.cloudstack.api.command.admin.router.ConfigureOvsElementCmd;
import org.apache.cloudstack.api.command.admin.router.ConfigureVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.CreateVirtualRouterElementCmd;
import org.apache.cloudstack.api.command.admin.router.ListOvsElementsCmd;
import org.apache.cloudstack.api.command.admin.router.ListVirtualRouterElementsCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.topology.NetworkTopology;
import org.apache.cloudstack.network.topology.NetworkTopologyContext;
import org.apache.log4j.Logger;
import org.cloud.network.router.deployment.RouterDeploymentDefinition;
import org.cloud.network.router.deployment.RouterDeploymentDefinitionBuilder;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.OvsProvider;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.VpnUser;
import com.cloud.network.as.AutoScaleCounter;
import com.cloud.network.as.AutoScaleCounter.AutoScaleCounterType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.OvsProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;

public class VirtualRouterElement extends AdapterBase implements VirtualRouterElementService, DhcpServiceProvider, UserDataServiceProvider, SourceNatServiceProvider,
StaticNatServiceProvider, FirewallServiceProvider, LoadBalancingServiceProvider, PortForwardingServiceProvider, RemoteAccessVPNServiceProvider, IpDeployer,
NetworkMigrationResponder, AggregatedCommandExecutor {
    private static final Logger s_logger = Logger.getLogger(VirtualRouterElement.class);
    public static final AutoScaleCounterType AutoScaleCounterCpu = new AutoScaleCounterType("cpu");
    public static final AutoScaleCounterType AutoScaleCounterMemory = new AutoScaleCounterType("memory");
    protected static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkDao _networksDao;
    @Inject
    NetworkModel _networkMdl;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NetworkOfferingDao _networkOfferingDao;

    @Inject
    VpcVirtualNetworkApplianceManager _routerMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    UserVmManager _userVmMgr;

    @Inject
    UserVmDao _userVmDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    OvsProviderDao _ovsProviderDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    DataCenterDao _dcDao;

    @Inject
    NetworkTopologyContext networkTopologyContext;

    @Inject
    protected RouterDeploymentDefinitionBuilder routerDeploymentDefinitionBuilder;

    protected boolean canHandle(final Network network, final Service service) {
        final Long physicalNetworkId = _networkMdl.getPhysicalNetworkId(network);
        if (physicalNetworkId == null) {
            return false;
        }

        if (network.getVpcId() != null) {
            return false;
        }

        if (!_networkMdl.isProviderEnabledInPhysicalNetwork(physicalNetworkId, Network.Provider.VirtualRouter.getName())) {
            return false;
        }

        if (service == null) {
            if (!_networkMdl.isProviderForNetwork(getProvider(), network.getId())) {
                s_logger.trace("Element " + getProvider().getName() + " is not a provider for the network " + network);
                return false;
            }
        } else {
            if (!_networkMdl.isProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                s_logger.trace("Element " + getProvider().getName() + " doesn't support service " + service.getName() + " in the network " + network);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean implement(final Network network, final NetworkOffering offering, final DeployDestination dest, final ReservationContext context)
            throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {

        if (offering.isSystemOnly()) {
            return false;
        }

        final Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);

        final RouterDeploymentDefinition routerDeploymentDefinition =
                routerDeploymentDefinitionBuilder.create()
                .setGuestNetwork(network)
                .setDeployDestination(dest)
                .setAccountOwner(_accountMgr.getAccount(network.getAccountId()))
                .setParams(params)
                .build();

        final List<DomainRouterVO> routers = routerDeploymentDefinition.deployVirtualRouter();

        int routerCounts = 1;
        if (offering.getRedundantRouter()) {
            routerCounts = 2;
        }
        if (routers == null || routers.size() < routerCounts) {
            throw new ResourceUnavailableException("Can't find all necessary running routers!", DataCenter.class, network.getDataCenterId());
        }

        return true;
    }

    @Override
    public boolean prepare(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (vm.getType() != VirtualMachine.Type.User || vm.getHypervisorType() == HypervisorType.BareMetal) {
            return false;
        }

        if (!canHandle(network, null)) {
            return false;
        }

        final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (offering.isSystemOnly()) {
            return false;
        }
        if (!_networkMdl.isProviderEnabledInPhysicalNetwork(_networkMdl.getPhysicalNetworkId(network), getProvider().getName())) {
            return false;
        }

        final RouterDeploymentDefinition routerDeploymentDefinition =
                routerDeploymentDefinitionBuilder.create()
                .setGuestNetwork(network)
                .setDeployDestination(dest)
                .setAccountOwner(_accountMgr.getAccount(network.getAccountId()))
                .setParams(vm.getParameters())
                .build();

        final List<DomainRouterVO> routers = routerDeploymentDefinition.deployVirtualRouter();

        if (routers == null || routers.size() == 0) {
            throw new ResourceUnavailableException("Can't find at least one running router!", DataCenter.class, network.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean applyFWRules(final Network network, final List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.Firewall)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply firewall rules on the backend; virtual " + "router doesn't exist in the network " + network.getId());
                return true;
            }

            if (rules != null && rules.size() == 1) {
                // for VR no need to add default egress rule to ALLOW traffic
                //The default allow rule is added from the router defalut iptables rules iptables-router
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System
                        && _networkMdl.getNetworkEgressDefaultPolicy(network.getId())) {
                    return true;
                }
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyFirewallRules(network, rules, domainRouterVO);
            }
        }
        return result;
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
            if (str.length() < 2) {
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

    public static boolean validateHAProxyLBRule(final LoadBalancingRule rule) {
        final String timeEndChar = "dhms";

        if (rule.getSourcePortStart() == NetUtils.HAPROXY_STATS_PORT) {
            s_logger.debug("Can't create LB on port 8081, haproxy is listening for  LB stats on this port");
            return false;
        }

        for (final LbStickinessPolicy stickinessPolicy : rule.getStickinessPolicies()) {
            final List<Pair<String, String>> paramsList = stickinessPolicy.getParams();

            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {

            } else if (StickinessMethodType.SourceBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
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
                if (expire != null && !containsOnlyNumbers(expire, timeEndChar)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: expire is not in timeformat: " + expire);
                }
                if (tablesize != null && !containsOnlyNumbers(tablesize, "kmg")) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: tablesize is not in size format: " + tablesize);

                }
            } else if (StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {
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
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: length is not a number: " + length);
                }
                if (holdTime != null && !containsOnlyNumbers(holdTime, timeEndChar) && !containsOnlyNumbers(holdTime, null)) {
                    throw new InvalidParameterValueException("Failed LB in validation rule id: " + rule.getId() + " Cause: holdtime is not in timeformat: " + holdTime);
                }
            }
        }
        return true;
    }

    @Override
    public boolean validateLBRule(final Network network, final LoadBalancingRule rule) {
        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        rules.add(rule);
        if (canHandle(network, Service.Lb) && canHandleLbRules(rules)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                return true;
            }
            return validateHAProxyLBRule(rule);
        }
        return true;
    }

    @Override
    public boolean applyLBRules(final Network network, final List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.Lb)) {
            if (!canHandleLbRules(rules)) {
                return false;
            }

            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply lb rules on the backend; virtual " + "router doesn't exist in the network " + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyLoadBalancingRules(network, rules, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public String[] applyVpnUsers(final RemoteAccessVpn vpn, final List<? extends VpnUser> users) throws ResourceUnavailableException {
        if (vpn.getNetworkId() == null) {
            return null;
        }

        final Network network = _networksDao.findById(vpn.getNetworkId());
        if (canHandle(network, Service.Vpn)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply vpn users on the backend; virtual router" + " doesn't exist in the network " + network.getId());
                return null;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            return networkTopology.applyVpnUsers(network, users, routers);
        } else {
            s_logger.debug("Element " + getName() + " doesn't handle applyVpnUsers command");
            return null;
        }
    }

    @Override
    public boolean startVpn(final RemoteAccessVpn vpn) throws ResourceUnavailableException {
        if (vpn.getNetworkId() == null) {
            return false;
        }

        final Network network = _networksDao.findById(vpn.getNetworkId());
        if (canHandle(network, Service.Vpn)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need stop vpn on the backend; virtual router doesn't" + " exist in the network " + network.getId());
                return true;
            }
            return _routerMgr.startRemoteAccessVpn(network, vpn, routers);
        } else {
            s_logger.debug("Element " + getName() + " doesn't handle createVpn command");
            return false;
        }
    }

    @Override
    public boolean stopVpn(final RemoteAccessVpn vpn) throws ResourceUnavailableException {
        if (vpn.getNetworkId() == null) {
            return false;
        }

        final Network network = _networksDao.findById(vpn.getNetworkId());
        if (canHandle(network, Service.Vpn)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need stop vpn on the backend; virtual router doesn't " + "exist in the network " + network.getId());
                return true;
            }
            return _routerMgr.deleteRemoteAccessVpn(network, vpn, routers);
        } else {
            s_logger.debug("Element " + getName() + " doesn't handle removeVpn command");
            return false;
        }
    }

    @Override
    public boolean applyIps(final Network network, final List<? extends PublicIpAddress> ipAddress, final Set<Service> services) throws ResourceUnavailableException {
        boolean canHandle = true;
        for (final Service service : services) {
            if (!canHandle(network, service)) {
                canHandle = false;
                break;
            }
        }
        boolean result = true;
        if (canHandle) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to associate ip addresses on the backend; virtual " + "router doesn't exist in the network " + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.associatePublicIP(network, ipAddress, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public Provider getProvider() {
        return Provider.VirtualRouter;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    public static String getHAProxyStickinessCapability() {
        LbStickinessMethod method;
        final List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>(1);

        method = new LbStickinessMethod(StickinessMethodType.LBCookieBased, "This is loadbalancer cookie based stickiness method.");
        method.addParam("cookie-name", false, "Cookie name passed in http header by the LB to the client.", false);
        method.addParam("mode", false, "Valid values: insert, rewrite, prefix. Default value: insert.  In the insert mode cookie will be created"
                + " by the LB. In other modes, cookie will be created by the server and LB modifies it.", false);
        method.addParam("nocache", false, "This option is recommended in conjunction with the insert mode when there is a cache between the client"
                + " and HAProxy, as it ensures that a cacheable response will be tagged non-cacheable if  a cookie needs "
                + "to be inserted. This is important because if all persistence cookies are added on a cacheable home page"
                + " for instance, then all customers will then fetch the page from an outer cache and will all share the "
                + "same persistence cookie, leading to one server receiving much more traffic than others. See also the " + "insert and postonly options. ", true);
        method.addParam("indirect", false, "When this option is specified in insert mode, cookies will only be added when the server was not reached"
                + " after a direct access, which means that only when a server is elected after applying a load-balancing algorithm,"
                + " or after a redispatch, then the cookie  will be inserted. If the client has all the required information"
                + " to connect to the same server next time, no further cookie will be inserted. In all cases, when the "
                + "indirect option is used in insert mode, the cookie is always removed from the requests transmitted to "
                + "the server. The persistence mechanism then becomes totally transparent from the application point of view.", true);
        method.addParam("postonly", false, "This option ensures that cookie insertion will only be performed on responses to POST requests. It is an"
                + " alternative to the nocache option, because POST responses are not cacheable, so this ensures that the "
                + "persistence cookie will never get cached.Since most sites do not need any sort of persistence before the"
                + " first POST which generally is a login request, this is a very efficient method to optimize caching "
                + "without risking to find a persistence cookie in the cache. See also the insert and nocache options.", true);
        method.addParam("domain", false, "This option allows to specify the domain at which a cookie is inserted. It requires exactly one parameter:"
                + " a valid domain name. If the domain begins with a dot, the browser is allowed to use it for any host "
                + "ending with that name. It is also possible to specify several domain names by invoking this option multiple"
                + " times. Some browsers might have small limits on the number of domains, so be careful when doing that. "
                + "For the record, sending 10 domains to MSIE 6 or Firefox 2 works as expected.", false);
        methodList.add(method);

        method = new LbStickinessMethod(StickinessMethodType.AppCookieBased,
                "This is App session based sticky method. Define session stickiness on an existing application cookie. " + "It can be used only for a specific http traffic");
        method.addParam("cookie-name", false, "This is the name of the cookie used by the application and which LB will "
                + "have to learn for each new session. Default value: Auto geneared based on ip", false);
        method.addParam("length", false, "This is the max number of characters that will be memorized and checked in " + "each cookie value. Default value:52", false);
        method.addParam("holdtime", false, "This is the time after which the cookie will be removed from memory if unused. The value should be in "
                + "the format Example : 20s or 30m  or 4h or 5d . only seconds(s), minutes(m) hours(h) and days(d) are valid,"
                + " cannot use th combinations like 20h30m. Default value:3h ", false);
        method.addParam(
                "request-learn",
                false,
                "If this option is specified, then haproxy will be able to learn the cookie found in the request in case the server does not specify any in response. This is typically what happens with PHPSESSID cookies, or when haproxy's session expires before the application's session and the correct server is selected. It is recommended to specify this option to improve reliability",
                true);
        method.addParam(
                "prefix",
                false,
                "When this option is specified, haproxy will match on the cookie prefix (or URL parameter prefix). "
                        + "The appsession value is the data following this prefix. Example : appsession ASPSESSIONID len 64 timeout 3h prefix  This will match the cookie ASPSESSIONIDXXXX=XXXXX, the appsession value will be XXXX=XXXXX.",
                        true);
        method.addParam("mode", false, "This option allows to change the URL parser mode. 2 modes are currently supported : - path-parameters "
                + ": The parser looks for the appsession in the path parameters part (each parameter is separated by a semi-colon), "
                + "which is convenient for JSESSIONID for example.This is the default mode if the option is not set. - query-string :"
                + " In this mode, the parser will look for the appsession in the query string.", false);
        methodList.add(method);

        method = new LbStickinessMethod(StickinessMethodType.SourceBased, "This is source based Stickiness method, " + "it can be used for any type of protocol.");
        method.addParam("tablesize", false, "Size of table to store source ip addresses. example: tablesize=200k or 300m" + " or 400g. Default value:200k", false);
        method.addParam("expire", false, "Entry in source ip table will expire after expire duration. units can be s,m,h,d ."
                + " example: expire=30m 20s 50h 4d. Default value:3h", false);
        methodList.add(method);

        final Gson gson = new Gson();
        final String capability = gson.toJson(methodList);
        return capability;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        final Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for LB service
        final Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Capability.SupportedProtocols, "tcp, udp, tcp-proxy");
        lbCapabilities.put(Capability.SupportedStickinessMethods, getHAProxyStickinessCapability());
        lbCapabilities.put(Capability.LbSchemes, LoadBalancerContainer.Scheme.Public.toString());

        // specifies that LB rules can support autoscaling and the list of
        // counters it supports
        AutoScaleCounter counter;
        final List<AutoScaleCounter> counterList = new ArrayList<AutoScaleCounter>();
        counter = new AutoScaleCounter(AutoScaleCounterCpu);
        counterList.add(counter);
        counter = new AutoScaleCounter(AutoScaleCounterMemory);
        counterList.add(counter);
        final Gson gson = new Gson();
        final String autoScaleCounterList = gson.toJson(counterList);
        lbCapabilities.put(Capability.AutoScaleCounters, autoScaleCounterList);
        capabilities.put(Service.Lb, lbCapabilities);

        // Set capabilities for Firewall service
        final Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp, all");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress, egress");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);

        // Set capabilities for vpn
        final Map<Capability, String> vpnCapabilities = new HashMap<Capability, String>();
        vpnCapabilities.put(Capability.SupportedVpnProtocols, "pptp,l2tp,ipsec");
        vpnCapabilities.put(Capability.VpnTypes, "removeaccessvpn");
        capabilities.put(Service.Vpn, vpnCapabilities);

        final Map<Capability, String> dnsCapabilities = new HashMap<Capability, String>();
        dnsCapabilities.put(Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Service.Dns, dnsCapabilities);

        capabilities.put(Service.UserData, null);

        final Map<Capability, String> dhcpCapabilities = new HashMap<Capability, String>();
        dhcpCapabilities.put(Capability.DhcpAccrossMultipleSubnets, "true");
        capabilities.put(Service.Dhcp, dhcpCapabilities);

        capabilities.put(Service.Gateway, null);

        final Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        sourceNatCapabilities.put(Capability.RedundantRouter, "true");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        capabilities.put(Service.StaticNat, null);
        capabilities.put(Service.PortForwarding, null);

        return capabilities;
    }

    @Override
    public boolean applyStaticNats(final Network network, final List<? extends StaticNat> rules) throws ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.StaticNat)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply static nat on the backend; virtual " + "router doesn't exist in the network " + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyStaticNats(network, rules, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public boolean shutdown(final Network network, final ReservationContext context, final boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            return true;
        }
        boolean stopResult = true;
        boolean destroyResult = true;
        for (final DomainRouterVO router : routers) {
            stopResult = stopResult && _routerMgr.stop(router, false, context.getCaller(), context.getAccount()) != null;
            if (!stopResult) {
                s_logger.warn("Failed to stop virtual router element " + router + ", but would try to process clean up anyway.");
            }
            if (cleanup) {
                destroyResult = destroyResult && _routerMgr.destroyRouter(router.getId(), context.getAccount(), context.getCaller().getId()) != null;
                if (!destroyResult) {
                    s_logger.warn("Failed to clean up virtual router element " + router);
                }
            }
        }
        return stopResult & destroyResult;
    }

    @Override
    public boolean destroy(final Network config, final ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(config.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            return true;
        }
        boolean result = true;
        // NOTE that we need to pass caller account to destroyRouter, otherwise
        // it will fail permission check there. Context passed in from
        // deleteNetwork is the network account,
        // not caller account
        final Account callerAccount = _accountMgr.getAccount(context.getCaller().getAccountId());
        for (final DomainRouterVO router : routers) {
            result = result && _routerMgr.destroyRouter(router.getId(), callerAccount, context.getCaller().getId()) != null;
        }
        return result;
    }

    @Override
    public boolean savePassword(final Network network, final NicProfile nic, final VirtualMachineProfile vm) throws ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return false;
        }
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Can't find virtual router element in network " + network.getId());
            return true;
        }

        final VirtualMachineProfile uservm = vm;

        final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
        final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

        // If any router is running then send save password command otherwise
        // save the password in DB
        for (final VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                return networkTopology.savePasswordToRouter(network, nic, uservm, router);
            }
        }
        final String password = (String) uservm.getParameter(VirtualMachineProfile.Param.VmPassword);
        final String password_encrypted = DBEncryptionUtil.encrypt(password);
        final UserVmVO userVmVO = _userVmDao.findById(vm.getId());

        _userVmDao.loadDetails(userVmVO);
        userVmVO.setDetail("password", password_encrypted);
        _userVmDao.saveDetails(userVmVO);

        userVmVO.setUpdateParameters(true);
        _userVmDao.update(userVmVO.getId(), userVmVO);

        return true;
    }

    @Override
    public boolean saveSSHKey(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final String sshPublicKey) throws ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return false;
        }
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Can't find virtual router element in network " + network.getId());
            return true;
        }

        final VirtualMachineProfile uservm = vm;

        final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
        final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

        boolean result = true;
        for (final DomainRouterVO domainRouterVO : routers) {
            result = result && networkTopology.saveSSHPublicKeyToRouter(network, nic, uservm, domainRouterVO, sshPublicKey);
        }
        return result;
    }

    @Override
    public boolean saveUserData(final Network network, final NicProfile nic, final VirtualMachineProfile vm) throws ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return false;
        }
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
        if (routers == null || routers.isEmpty()) {
            s_logger.debug("Can't find virtual router element in network " + network.getId());
            return true;
        }

        final VirtualMachineProfile uservm = vm;

        final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
        final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

        boolean result = true;
        for (final DomainRouterVO domainRouterVO : routers) {
            result = result && networkTopology.saveUserDataToRouter(network, nic, uservm, domainRouterVO);
        }
        return result;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateVirtualRouterElementCmd.class);
        cmdList.add(ConfigureVirtualRouterElementCmd.class);
        cmdList.add(ListVirtualRouterElementsCmd.class);
        return cmdList;
    }

    @Override
    public VirtualRouterProvider configure(final ConfigureVirtualRouterElementCmd cmd) {
        final VirtualRouterProviderVO element = _vrProviderDao.findById(cmd.getId());
        if (element == null || !(element.getType() == Type.VirtualRouter || element.getType() == Type.VPCVirtualRouter)) {
            s_logger.debug("Can't find Virtual Router element with network service provider id " + cmd.getId());
            return null;
        }

        element.setEnabled(cmd.getEnabled());
        _vrProviderDao.persist(element);

        return element;
    }

    @Override
    public OvsProvider configure(final ConfigureOvsElementCmd cmd) {
        final OvsProviderVO element = _ovsProviderDao.findById(cmd.getId());
        if (element == null) {
            s_logger.debug("Can't find Ovs element with network service provider id " + cmd.getId());
            return null;
        }

        element.setEnabled(cmd.getEnabled());
        _ovsProviderDao.persist(element);

        return element;
    }

    @Override
    public VirtualRouterProvider addElement(final Long nspId, final Type providerType) {
        if (!(providerType == Type.VirtualRouter || providerType == Type.VPCVirtualRouter)) {
            throw new InvalidParameterValueException("Element " + getName() + " supports only providerTypes: " + Type.VirtualRouter.toString() + " and " + Type.VPCVirtualRouter);
        }
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(nspId, providerType);
        if (element != null) {
            s_logger.debug("There is already a virtual router element with service provider id " + nspId);
            return null;
        }
        element = new VirtualRouterProviderVO(nspId, providerType);
        _vrProviderDao.persist(element);
        return element;
    }

    @Override
    public boolean applyPFRules(final Network network, final List<PortForwardingRule> rules) throws ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.PortForwarding)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.isEmpty()) {
                s_logger.debug("Virtual router elemnt doesn't need to apply firewall rules on the backend; virtual " + "router doesn't exist in the network " + network.getId());
                return true;
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyFirewallRules(network, rules, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public boolean isReady(final PhysicalNetworkServiceProvider provider) {
        final VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(provider.getId(), getVirtualRouterProvider());
        if (element == null) {
            return false;
        }
        return element.isEnabled();
    }

    @Override
    public boolean shutdownProviderInstances(final PhysicalNetworkServiceProvider provider, final ReservationContext context) throws ConcurrentOperationException,
    ResourceUnavailableException {
        final VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(provider.getId(), getVirtualRouterProvider());
        if (element == null) {
            return true;
        }
        // Find domain routers
        final long elementId = element.getId();
        final List<DomainRouterVO> routers = _routerDao.listByElementId(elementId);
        boolean result = true;
        for (final DomainRouterVO router : routers) {
            result = result && _routerMgr.destroyRouter(router.getId(), context.getAccount(), context.getCaller().getId()) != null;
        }
        _vrProviderDao.remove(elementId);

        return result;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    public Long getIdByNspId(final Long nspId) {
        final VirtualRouterProviderVO vr = _vrProviderDao.findByNspIdAndType(nspId, Type.VirtualRouter);
        return vr.getId();
    }

    @Override
    public VirtualRouterProvider getCreatedElement(final long id) {
        final VirtualRouterProvider provider = _vrProviderDao.findById(id);
        if (!(provider.getType() == Type.VirtualRouter || provider.getType() == Type.VPCVirtualRouter)) {
            throw new InvalidParameterValueException("Unable to find provider by id");
        }
        return provider;
    }

    @Override
    public boolean release(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final ReservationContext context) throws ConcurrentOperationException,
    ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDhcpSupportForSubnet(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final DeployDestination dest,
            final ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(network, Service.Dhcp)) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }

            final VirtualMachineProfile uservm = vm;

            final List<DomainRouterVO> routers = getRouters(network, dest);

            if (routers == null || routers.size() == 0) {
                throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            return networkTopology.configDhcpForSubnet(network, nic, uservm, dest, routers);
        }
        return false;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(final Network network) throws ResourceUnavailableException {
        if (canHandle(network, Service.Dhcp)) {
            final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            if (routers == null || routers.size() == 0) {
                throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
            }
            try {
                return _routerMgr.removeDhcpSupportForSubnet(network, routers);
            } catch (final ResourceUnavailableException e) {
                s_logger.debug("Router resource unavailable ");
            }
        }
        return false;
    }

    @Override
    public boolean addDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.Dhcp)) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }

            final VirtualMachineProfile uservm = vm;
            final List<DomainRouterVO> routers = getRouters(network, dest);

            if (routers == null || routers.size() == 0) {
                throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyDhcpEntry(network, nic, uservm, dest, domainRouterVO);
            }
        }
        return result;
    }

    @Override
    public boolean addPasswordAndUserdata(final Network network, final NicProfile nic, final VirtualMachineProfile vm, final DeployDestination dest,
            final ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        boolean result = true;
        if (canHandle(network, Service.UserData)) {
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }

            if (network.getIp6Gateway() != null) {
                s_logger.info("Skip password and userdata service setup for IPv6 VM");
                return true;
            }

            final VirtualMachineProfile uservm = vm;

            final List<DomainRouterVO> routers = getRouters(network, dest);

            if (routers == null || routers.size() == 0) {
                throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
            }

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            for (final DomainRouterVO domainRouterVO : routers) {
                result = result && networkTopology.applyUserData(network, nic, uservm, dest, domainRouterVO);
            }
        }
        return result;
    }

    protected List<DomainRouterVO> getRouters(final Network network, final DeployDestination dest) {
        boolean publicNetwork = false;
        if (_networkMdl.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, getProvider())) {
            publicNetwork = true;
        }
        final boolean isPodBased = (dest.getDataCenter().getNetworkType() == NetworkType.Basic || _networkMdl.isSecurityGroupSupportedInNetwork(network))
                && network.getTrafficType() == TrafficType.Guest;

        List<DomainRouterVO> routers;

        if (publicNetwork) {
            routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
        } else {
            if (isPodBased && dest.getPod() != null) {
                final Long podId = dest.getPod().getId();
                routers = _routerDao.listByNetworkAndPodAndRole(network.getId(), podId, Role.VIRTUAL_ROUTER);
            } else {
                // With pod == null, it's network restart case, we would add all
                // router to it
                // Ignore DnsBasicZoneUpdate() parameter here
                routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            }
        }

        // for Basic zone, add all Running routers - we have to send
        // Dhcp/vmData/password info to them when
        // network.dns.basiczone.updates is set to "all"
        // With pod == null, it's network restart case, we already add all
        // routers to it
        if (isPodBased && dest.getPod() != null && _routerMgr.getDnsBasicZoneUpdate().equalsIgnoreCase("all")) {
            final Long podId = dest.getPod().getId();
            final List<DomainRouterVO> allRunningRoutersOutsideThePod = _routerDao.findByNetworkOutsideThePod(network.getId(), podId, State.Running, Role.VIRTUAL_ROUTER);
            routers.addAll(allRunningRoutersOutsideThePod);
        }
        return routers;
    }

    @Override
    public List<? extends VirtualRouterProvider> searchForVirtualRouterElement(final ListVirtualRouterElementsCmd cmd) {
        final Long id = cmd.getId();
        final Long nspId = cmd.getNspId();
        final Boolean enabled = cmd.getEnabled();

        final QueryBuilder<VirtualRouterProviderVO> sc = QueryBuilder.create(VirtualRouterProviderVO.class);
        if (id != null) {
            sc.and(sc.entity().getId(), Op.EQ, id);
        }
        if (nspId != null) {
            sc.and(sc.entity().getNspId(), Op.EQ, nspId);
        }
        if (enabled != null) {
            sc.and(sc.entity().isEnabled(), Op.EQ, enabled);
        }

        // return only VR and VPC VR
        sc.and(sc.entity().getType(), Op.IN, VirtualRouterProvider.Type.VPCVirtualRouter, VirtualRouterProvider.Type.VirtualRouter);

        return sc.list();
    }

    @Override
    public List<? extends OvsProvider> searchForOvsElement(final ListOvsElementsCmd cmd) {
        final Long id = cmd.getId();
        final Long nspId = cmd.getNspId();
        final Boolean enabled = cmd.getEnabled();
        final QueryBuilder<OvsProviderVO> sc = QueryBuilder.create(OvsProviderVO.class);

        if (id != null) {
            sc.and(sc.entity().getId(), Op.EQ, id);
        }
        if (nspId != null) {
            sc.and(sc.entity().getNspId(), Op.EQ, nspId);
        }
        if (enabled != null) {
            sc.and(sc.entity().isEnabled(), Op.EQ, enabled);
        }

        return sc.list();
    }

    @Override
    public boolean verifyServicesCombination(final Set<Service> services) {
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(final Network network) {
        return this;
    }

    protected VirtualRouterProvider.Type getVirtualRouterProvider() {
        return VirtualRouterProvider.Type.VirtualRouter;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(final Network network, final List<LoadBalancingRule> lbrules) {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean canHandleLbRules(final List<LoadBalancingRule> rules) {
        final Map<Capability, String> lbCaps = getCapabilities().get(Service.Lb);
        if (!lbCaps.isEmpty()) {
            final String schemeCaps = lbCaps.get(Capability.LbSchemes);
            if (schemeCaps != null) {
                for (final LoadBalancingRule rule : rules) {
                    if (!schemeCaps.contains(rule.getScheme().toString())) {
                        s_logger.debug("Scheme " + rules.get(0).getScheme() + " is not supported by the provider " + getName());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean prepareMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context) {
        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Pvlan) {
            return true;
        }
        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
            assert vm instanceof DomainRouterVO;
            final DomainRouterVO router = (DomainRouterVO) vm.getVirtualMachine();

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            try {
                networkTopology.setupDhcpForPvlan(false, router, router.getHostId(), nic);
            } catch (final ResourceUnavailableException e) {
                s_logger.warn("Timed Out", e);
            }
        } else if (vm.getType() == VirtualMachine.Type.User) {
            assert vm instanceof UserVmVO;
            final UserVmVO userVm = (UserVmVO) vm.getVirtualMachine();
            _userVmMgr.setupVmForPvlan(false, userVm.getHostId(), nic);
        }
        return true;
    }

    @Override
    public void rollbackMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final ReservationContext src, final ReservationContext dst) {
        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Pvlan) {
            return;
        }
        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
            assert vm instanceof DomainRouterVO;
            final DomainRouterVO router = (DomainRouterVO) vm.getVirtualMachine();

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            try {
                networkTopology.setupDhcpForPvlan(true, router, router.getHostId(), nic);
            } catch (final ResourceUnavailableException e) {
                s_logger.warn("Timed Out", e);
            }
        } else if (vm.getType() == VirtualMachine.Type.User) {
            assert vm instanceof UserVmVO;
            final UserVmVO userVm = (UserVmVO) vm.getVirtualMachine();
            _userVmMgr.setupVmForPvlan(true, userVm.getHostId(), nic);
        }
    }

    @Override
    public void commitMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final ReservationContext src, final ReservationContext dst) {
        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Pvlan) {
            return;
        }
        if (vm.getType() == VirtualMachine.Type.DomainRouter) {
            assert vm instanceof DomainRouterVO;
            final DomainRouterVO router = (DomainRouterVO) vm.getVirtualMachine();

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());
            final NetworkTopology networkTopology = networkTopologyContext.retrieveNetworkTopology(dcVO);

            try {
                networkTopology.setupDhcpForPvlan(true, router, router.getHostId(), nic);
            } catch (final ResourceUnavailableException e) {
                s_logger.warn("Timed Out", e);
            }
        } else if (vm.getType() == VirtualMachine.Type.User) {
            assert vm instanceof UserVmVO;
            final UserVmVO userVm = (UserVmVO) vm.getVirtualMachine();
            _userVmMgr.setupVmForPvlan(true, userVm.getHostId(), nic);
        }
    }

    @Override
    public boolean prepareAggregatedExecution(final Network network, final DeployDestination dest) throws ResourceUnavailableException {
        final List<DomainRouterVO> routers = getRouters(network, dest);

        if (routers == null || routers.size() == 0) {
            throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
        }

        return _routerMgr.prepareAggregatedExecution(network, routers);
    }

    @Override
    public boolean completeAggregatedExecution(final Network network, final DeployDestination dest) throws ResourceUnavailableException {
        final List<DomainRouterVO> routers = getRouters(network, dest);

        if (routers == null || routers.size() == 0) {
            throw new ResourceUnavailableException("Can't find at least one router!", DataCenter.class, network.getDataCenterId());
        }

        return _routerMgr.completeAggregatedExecution(network, routers);
    }

    @Override
    public boolean cleanupAggregatedExecution(final Network network, final DeployDestination dest) throws ResourceUnavailableException {
        // The VR code already cleansup in the Finish routine using finally,
        // lets not waste another command
        return true;
    }
}
