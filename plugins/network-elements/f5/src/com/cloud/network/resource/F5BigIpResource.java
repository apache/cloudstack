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

import iControl.CommonEnabledState;
import iControl.CommonIPPortDefinition;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonVirtualServerDefinition;
import iControl.Interfaces;
import iControl.LocalLBLBMethod;
import iControl.LocalLBNodeAddressBindingStub;
import iControl.LocalLBPersistenceMode;
import iControl.LocalLBPoolBindingStub;
import iControl.LocalLBProfileContextType;
import iControl.LocalLBProfilePersistenceBindingStub;
import iControl.LocalLBProfileULong;
import iControl.LocalLBVirtualServerBindingStub;
import iControl.LocalLBVirtualServerVirtualServerPersistence;
import iControl.LocalLBVirtualServerVirtualServerProfile;
import iControl.LocalLBVirtualServerVirtualServerResource;
import iControl.LocalLBVirtualServerVirtualServerStatisticEntry;
import iControl.LocalLBVirtualServerVirtualServerStatistics;
import iControl.LocalLBVirtualServerVirtualServerType;
import iControl.NetworkingMemberTagType;
import iControl.NetworkingMemberType;
import iControl.NetworkingRouteDomainBindingStub;
import iControl.NetworkingSelfIPBindingStub;
import iControl.NetworkingVLANBindingStub;
import iControl.NetworkingVLANMemberEntry;
import iControl.SystemConfigSyncBindingStub;
import iControl.SystemConfigSyncSaveMode;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.agent.api.to.LoadBalancerTO.StickinessPolicyTO;
import com.cloud.host.Host;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

public class F5BigIpResource implements ServerResource {

    private enum LbAlgorithm {
        RoundRobin(null, LocalLBLBMethod.LB_METHOD_ROUND_ROBIN), LeastConn(null, LocalLBLBMethod.LB_METHOD_LEAST_CONNECTION_MEMBER);

        String persistenceProfileName;
        LocalLBLBMethod method;

        LbAlgorithm(String persistenceProfileName, LocalLBLBMethod method) {
            this.persistenceProfileName = persistenceProfileName;
            this.method = method;
        }

        public String getPersistenceProfileName() {
            return persistenceProfileName;
        }

        public LocalLBLBMethod getMethod() {
            return method;
        }
    }

    private enum LbProtocol {
        tcp, udp;
    }

    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _publicInterface;
    private String _privateInterface;
    private Integer _numRetries;
    private String _guid;

    private Interfaces _interfaces;
    private LocalLBVirtualServerBindingStub _virtualServerApi;
    private LocalLBPoolBindingStub _loadbalancerApi;
    private LocalLBNodeAddressBindingStub _nodeApi;
    private NetworkingVLANBindingStub _vlanApi;
    private NetworkingSelfIPBindingStub _selfIpApi;
    private NetworkingRouteDomainBindingStub _routeDomainApi;
    private SystemConfigSyncBindingStub _configSyncApi;
    private LocalLBProfilePersistenceBindingStub _persistenceProfileApi;
    private String _objectNamePathSep = "-";
    private String _routeDomainIdentifier = "%";

    private static final Logger s_logger = Logger.getLogger(F5BigIpResource.class);

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
                throw new ConfigurationException("Unable to find public interface");
            }

            _privateInterface = (String)params.get("privateinterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 1);

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            login();

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
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
        cmd.setVersion(F5BigIpResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Host.Type getType() {
        return Host.Type.ExternalLoadBalancer;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.ExternalLoadBalancer, id);
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
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    private Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd, numRetries);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            return execute((LoadBalancerConfigCommand)cmd, numRetries);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
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
            s_logger.error("Failed to log in to F5 device at " + _ip + " due to " + e.getMessage());
        }
        return false;
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private synchronized Answer execute(IpAssocCommand cmd, int numRetries) {
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        try {
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {
                // is it saver to use Long.valueOf(BroadcastDomain.getValue(ip.getBroadcastUri())) ???
                // i.o.w. can this contain vlan:// then change !!!
                long guestVlanTag = Long.valueOf(ip.getBroadcastUri());
                // It's a hack, using isOneToOneNat field for indicate if it's inline or not
                boolean inline = ip.isOneToOneNat();
                String vlanSelfIp = inline ? tagAddressWithRouteDomain(ip.getVlanGateway(), guestVlanTag) : ip.getVlanGateway();
                String vlanNetmask = ip.getVlanNetmask();

                // Delete any existing guest VLAN with this tag, self IP, and netmask
                deleteGuestVlan(guestVlanTag, vlanSelfIp, vlanNetmask, inline);

                if (ip.isAdd()) {
                    // Add a new guest VLAN
                    addGuestVlan(guestVlanTag, vlanSelfIp, vlanNetmask, inline);
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
            long guestVlanTag = Long.parseLong(cmd.getAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG));
            LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();
            for (LoadBalancerTO loadBalancer : loadBalancers) {
                boolean inline = loadBalancer.isInline();
                LbProtocol lbProtocol;
                try {
                    if (loadBalancer.getProtocol() == null) {
                        lbProtocol = LbProtocol.tcp;
                    } else {
                        lbProtocol = LbProtocol.valueOf(loadBalancer.getProtocol());
                    }
                } catch (IllegalArgumentException e) {
                    throw new ExecutionException("Got invalid protocol: " + loadBalancer.getProtocol());
                }

                LbAlgorithm lbAlgorithm;
                if (loadBalancer.getAlgorithm().equals("roundrobin")) {
                    lbAlgorithm = LbAlgorithm.RoundRobin;
                } else if (loadBalancer.getAlgorithm().equals("leastconn")) {
                    lbAlgorithm = LbAlgorithm.LeastConn;
                } else {
                    throw new ExecutionException("Got invalid algorithm: " + loadBalancer.getAlgorithm());
                }

                String srcIp = inline ? tagAddressWithRouteDomain(loadBalancer.getSrcIp(), guestVlanTag) : loadBalancer.getSrcIp();
                int srcPort = loadBalancer.getSrcPort();
                String virtualServerName = genVirtualServerName(lbProtocol, srcIp, srcPort);

                boolean destinationsToAdd = false;
                for (DestinationTO destination : loadBalancer.getDestinations()) {
                    if (!destination.isRevoked()) {
                        destinationsToAdd = true;
                        break;
                    }
                }

                // Delete the virtual server with this protocol, source IP, and source port, along with its default pool and all pool members
                deleteVirtualServerAndDefaultPool(virtualServerName);
                if (!loadBalancer.isRevoked() && destinationsToAdd) {
                    // Add the pool
                    addPool(virtualServerName, lbAlgorithm);

                    // Add pool members
                    List<String> activePoolMembers = new ArrayList<String>();
                    for (DestinationTO destination : loadBalancer.getDestinations()) {
                        if (!destination.isRevoked()) {
                            String destIp = inline ? tagAddressWithRouteDomain(destination.getDestIp(), guestVlanTag) : destination.getDestIp();
                            addPoolMember(virtualServerName, destIp, destination.getDestPort());
                            activePoolMembers.add(destIp + "-" + destination.getDestPort());
                        }
                    }

                    // Add the virtual server
                    addVirtualServer(virtualServerName, lbProtocol, srcIp, srcPort, loadBalancer.getStickinessPolicies());
                }
            }

            saveConfiguration();
            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to " + e);

            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, e);
            }

        }
    }

    private synchronized ExternalNetworkResourceUsageAnswer execute(ExternalNetworkResourceUsageCommand cmd) {
        try {
            return getIpBytesSentAndReceived(cmd);
        } catch (ExecutionException e) {
            return new ExternalNetworkResourceUsageAnswer(cmd, e);
        }
    }

    private void saveConfiguration() throws ExecutionException {
        try {
            _configSyncApi.save_configuration("", SystemConfigSyncSaveMode.SAVE_BASE_LEVEL_CONFIG);
            _configSyncApi.save_configuration("", SystemConfigSyncSaveMode.SAVE_HIGH_LEVEL_CONFIG);
            s_logger.debug("Successfully saved F5 BigIp configuration.");
        } catch (RemoteException e) {
            s_logger.error("Failed to save F5 BigIp configuration due to: " + e);
            throw new ExecutionException(e.getMessage());
        }
    }

    private void addGuestVlan(long vlanTag, String vlanSelfIp, String vlanNetmask, boolean inline) throws ExecutionException {
        try {
            String vlanName = genVlanName(vlanTag);
            List<String> allVlans = getStrippedVlans();
            if (!allVlans.contains(vlanName)) {
                String[] vlanNames = genStringArray(vlanName);
                long[] vlanTags = genLongArray(vlanTag);
                CommonEnabledState[] commonEnabledState = {CommonEnabledState.STATE_DISABLED};

                // Create the interface name
                NetworkingVLANMemberEntry[][] vlanMemberEntries = {{new NetworkingVLANMemberEntry()}};
                vlanMemberEntries[0][0].setMember_type(NetworkingMemberType.MEMBER_INTERFACE);
                vlanMemberEntries[0][0].setTag_state(NetworkingMemberTagType.MEMBER_TAGGED);
                vlanMemberEntries[0][0].setMember_name(_privateInterface);

                s_logger.debug("Creating a guest VLAN with tag " + vlanTag);
                _vlanApi.create(vlanNames, vlanTags, vlanMemberEntries, commonEnabledState, new long[] {10L}, new String[] {"00:00:00:00:00:00"});
                s_logger.debug("vlanName " + vlanName);
                s_logger.debug("getStrippedVlans " + getStrippedVlans());

                if (!getStrippedVlans().contains(vlanName)) {
                    throw new ExecutionException("Failed to create vlan with tag " + vlanTag);
                }
            }

            if (inline) {
                List<Long> allRouteDomains = getRouteDomains();
                if (!allRouteDomains.contains(vlanTag)) {
                    long[] routeDomainIds = genLongArray(vlanTag);
                    String[][] vlanNames = new String[][] {genStringArray(genVlanName(vlanTag))};

                    s_logger.debug("Creating route domain " + vlanTag);
                    _routeDomainApi.create(routeDomainIds, vlanNames);

                    if (!getRouteDomains().contains(vlanTag)) {
                        throw new ExecutionException("Failed to create route domain " + vlanTag);
                    }
                }
            }

            List<String> allSelfIps = getSelfIps();
            if (!allSelfIps.contains(vlanSelfIp)) {
                String[] selfIpsToCreate = genStringArray(vlanSelfIp);
                String[] vlans = genStringArray(vlanName);
                String[] netmasks = genStringArray(vlanNetmask);
                long[] unitIds = genLongArray(0L);
                CommonEnabledState[] enabledStates = new CommonEnabledState[] {CommonEnabledState.STATE_DISABLED};

                s_logger.debug("Creating self IP " + vlanSelfIp);
                _selfIpApi.create(selfIpsToCreate, vlans, netmasks, unitIds, enabledStates);

                if (!getSelfIps().contains(vlanSelfIp)) {
                    throw new ExecutionException("Failed to create self IP " + vlanSelfIp);
                }
            }
        } catch (RemoteException e) {
            s_logger.error(e);
            throw new ExecutionException(e.getMessage());
        }

    }

    private void deleteGuestVlan(long vlanTag, String vlanSelfIp, String vlanNetmask, boolean inline) throws ExecutionException {
        try {
            // Delete all virtual servers and pools that use this guest VLAN
            deleteVirtualServersInGuestVlan(vlanSelfIp, vlanNetmask);

            List<String> allSelfIps = getSelfIps();
            if (allSelfIps.contains(vlanSelfIp)) {
                s_logger.debug("Deleting self IP " + vlanSelfIp);
                _selfIpApi.delete_self_ip(genStringArray(vlanSelfIp));

                if (getSelfIps().contains(vlanSelfIp)) {
                    throw new ExecutionException("Failed to delete self IP " + vlanSelfIp);
                }
            }

            if (inline) {
                List<Long> allRouteDomains = getRouteDomains();
                if (allRouteDomains.contains(vlanTag)) {
                    s_logger.debug("Deleting route domain " + vlanTag);
                    _routeDomainApi.delete_route_domain(genLongArray(vlanTag));

                    if (getRouteDomains().contains(vlanTag)) {
                        throw new ExecutionException("Failed to delete route domain " + vlanTag);
                    }
                }
            }

            String vlanName = genVlanName(vlanTag);
            List<String> allVlans = getStrippedVlans();
            if (allVlans.contains(vlanName)) {
                _vlanApi.delete_vlan(genStringArray(vlanName));

                if (getVlans().contains(vlanName)) {
                    throw new ExecutionException("Failed to delete VLAN with tag: " + vlanTag);
                }
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void deleteVirtualServersInGuestVlan(String vlanSelfIp, String vlanNetmask) throws ExecutionException {
        vlanSelfIp = stripRouteDomainFromAddress(vlanSelfIp);
        List<String> virtualServersToDelete = new ArrayList<String>();

        List<String> allVirtualServers = getStrippedVirtualServers();
        for (String virtualServerName : allVirtualServers) {
            // Check if the virtual server's default pool has members in this guest VLAN
            List<String> poolMembers = getMembers(virtualServerName);
            for (String poolMemberName : poolMembers) {
                String poolMemberIp = stripRouteDomainFromAddress(getIpAndPort(poolMemberName)[0]);
                if (NetUtils.sameSubnet(vlanSelfIp, poolMemberIp, vlanNetmask)) {
                    virtualServersToDelete.add(virtualServerName);
                    break;
                }
            }
        }

        for (String virtualServerName : virtualServersToDelete) {
            s_logger.debug("Found a virtual server (" + virtualServerName + ") for guest network with self IP " + vlanSelfIp +
                " that is active when the guest network is being destroyed.");
            deleteVirtualServerAndDefaultPool(virtualServerName);
        }
    }

    private String genVlanName(long vlanTag) {
        return "vlan-" + String.valueOf(vlanTag);
    }

    private List<Long> getRouteDomains() throws ExecutionException {
        try {
            List<Long> routeDomains = new ArrayList<Long>();
            long[] routeDomainsArray = _routeDomainApi.get_list();

            for (long routeDomainName : routeDomainsArray) {
                routeDomains.add(routeDomainName);
            }

            return routeDomains;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private List<String> getSelfIps() throws ExecutionException {
        try {
            List<String> selfIps = new ArrayList<String>();
            String[] selfIpsArray = _selfIpApi.get_list();

            for (String selfIp : selfIpsArray) {
                selfIps.add(selfIp);
            }

            return selfIps;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    //This was working with Big IP 10.x
    //getVlans retuns vlans with user partition information
    //ex: if vlanname is vlan-100 then the get_list() will return /Common/vlan-100
    private List<String> getVlans() throws ExecutionException {
        try {
            List<String> vlans = new ArrayList<String>();
            String[] vlansArray = _vlanApi.get_list();

            for (String vlan : vlansArray) {
                vlans.add(vlan);
            }

            return vlans;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    //getVlans retuns vlan names without user partition information
    //ex: if vlanname is vlan-100 then the get_list() will return /Common/vlan-100
    // This method will strip the partition information and only returns a list with vlan name (vlan-100)
    private List<String> getStrippedVlans() throws ExecutionException {
        try {
            List<String> vlans = new ArrayList<String>();
            String[] vlansArray = _vlanApi.get_list();

            for (String vlan : vlansArray) {
                if(vlan.contains("/")){
                    vlans.add(vlan.substring(vlan.lastIndexOf("/") + 1));
                }else{
                    vlans.add(vlan);
                }
            }

            return vlans;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }
    // Login

    private void login() throws ExecutionException {
        try {
            _interfaces = new Interfaces();

            if (!_interfaces.initialize(_ip, _username, _password)) {
                throw new ExecutionException("Failed to log in to BigIp appliance");
            }

            // iControl.Interfaces.initialize always return true so make a call to force connect to F5 to validate credentials
            _interfaces.getSystemSystemInfo().get_system_information();

            _virtualServerApi = _interfaces.getLocalLBVirtualServer();
            _loadbalancerApi = _interfaces.getLocalLBPool();
            _nodeApi = _interfaces.getLocalLBNodeAddress();
            _vlanApi = _interfaces.getNetworkingVLAN();
            _selfIpApi = _interfaces.getNetworkingSelfIP();
            _routeDomainApi = _interfaces.getNetworkingRouteDomain();
            _configSyncApi = _interfaces.getSystemConfigSync();
            _persistenceProfileApi = _interfaces.getLocalLBProfilePersistence();
        } catch (Exception e) {
            throw new ExecutionException("Failed to log in to BigIp appliance due to " + e.getMessage());
        }
    }

    // Virtual server methods

    private void addVirtualServer(String virtualServerName, LbProtocol protocol, String srcIp, int srcPort, StickinessPolicyTO[] stickyPolicies)
        throws ExecutionException {
        try {
            if (!virtualServerExists(virtualServerName)) {
                s_logger.debug("Adding virtual server " + virtualServerName);
                _virtualServerApi.create(genVirtualServerDefinition(virtualServerName, protocol, srcIp, srcPort), new String[] {"255.255.255.255"},
                    genVirtualServerResource(virtualServerName), genVirtualServerProfile(protocol));
                _virtualServerApi.set_snat_automap(genStringArray(virtualServerName));
                if (!virtualServerExists(virtualServerName)) {
                    throw new ExecutionException("Failed to add virtual server " + virtualServerName);
                }
            }

            if ((stickyPolicies != null) && (stickyPolicies.length > 0) && (stickyPolicies[0] != null)) {
                StickinessPolicyTO stickinessPolicy = stickyPolicies[0];
                if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName())) {

                    String[] profileNames = genStringArray("Cookie-profile-" + virtualServerName);
                    if (!persistenceProfileExists(profileNames[0])) {
                        LocalLBPersistenceMode[] lbPersistenceMode = new iControl.LocalLBPersistenceMode[1];
                        lbPersistenceMode[0] = iControl.LocalLBPersistenceMode.PERSISTENCE_MODE_COOKIE;
                        _persistenceProfileApi.create(profileNames, lbPersistenceMode);
                        _virtualServerApi.add_persistence_profile(genStringArray(virtualServerName), genPersistenceProfile(profileNames[0]));
                    }

                    List<Pair<String, String>> paramsList = stickinessPolicy.getParams();
                    for (Pair<String, String> param : paramsList) {
                        if ("holdtime".equalsIgnoreCase(param.first())) {
                            long timeout = 180; //F5 default
                            if (param.second() != null) {
                                timeout = Long.parseLong(param.second());
                            }
                            LocalLBProfileULong[] cookieTimeout = new LocalLBProfileULong[1];
                            cookieTimeout[0] = new LocalLBProfileULong();
                            cookieTimeout[0].setValue(timeout);
                            _persistenceProfileApi.set_cookie_expiration(profileNames, cookieTimeout);
                        }
                    }
                }
            } else {
                _virtualServerApi.remove_all_persistence_profiles(genStringArray(virtualServerName));
            }

        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void deleteVirtualServerAndDefaultPool(String virtualServerName) throws ExecutionException {
        try {
            if (virtualServerExists(virtualServerName)) {
                // Delete the default pool's members
                List<String> poolMembers = getMembers(virtualServerName);
                for (String poolMember : poolMembers) {
                    String[] destIpAndPort = getIpAndPort(poolMember);
                    deletePoolMember(virtualServerName, destIpAndPort[0], Integer.valueOf(destIpAndPort[1]));
                }

                // Delete the virtual server
                s_logger.debug("Deleting virtual server " + virtualServerName);
                _virtualServerApi.delete_virtual_server(genStringArray(virtualServerName));

                if (getStrippedVirtualServers().contains(virtualServerName)) {
                    throw new ExecutionException("Failed to delete virtual server " + virtualServerName);
                }

                // Delete the default pool
                deletePool(virtualServerName);
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private String genVirtualServerName(LbProtocol protocol, String srcIp, long srcPort) {
        srcIp = stripRouteDomainFromAddress(srcIp);
        return genObjectName("vs", protocol, srcIp, srcPort);
    }

    private boolean virtualServerExists(String virtualServerName) throws ExecutionException {
        return getStrippedVirtualServers().contains(virtualServerName);
    }

    //This was working with Big IP 10.x
    //getVirtualServers retuns VirtualServers with user partition information
    //ex: if VirtualServers is vs-tcp-10.147.44.8-22 then the get_list() will return /Common/vs-tcp-10.147.44.8-22
    private List<String> getVirtualServers() throws ExecutionException {
        try {
            List<String> virtualServers = new ArrayList<String>();
            String[] virtualServersArray = _virtualServerApi.get_list();

            for (String virtualServer : virtualServersArray) {
                virtualServers.add(virtualServer);
            }

            return virtualServers;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

/*    getStrippedVirtualServers retuns VirtualServers without user partition information
    ex: if VirtualServers is vs-tcp-10.147.44.8-22 then the get_list() will return /Common/vs-tcp-10.147.44.8-22
    This method will strip the partition information and only returns a list with VirtualServers (vs-tcp-10.147.44.8-22)*/
    private List<String> getStrippedVirtualServers() throws ExecutionException {
        try {
            List<String> virtualServers = new ArrayList<String>();
            String[] virtualServersArray = _virtualServerApi.get_list();

            for (String virtualServer : virtualServersArray) {
                if(virtualServer.contains("/")){
                    virtualServers.add(virtualServer.substring(virtualServer.lastIndexOf("/") + 1));
                }else{
                    virtualServers.add(virtualServer);
                }
            }

            return virtualServers;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private boolean persistenceProfileExists(String profileName) throws ExecutionException {
        try {
            String[] persistenceProfileArray = _persistenceProfileApi.get_list();
            if (persistenceProfileArray == null) {
                return false;
            }
            for (String profile : persistenceProfileArray) {
                if (profile.equalsIgnoreCase(profileName)) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private iControl.CommonVirtualServerDefinition[] genVirtualServerDefinition(String name, LbProtocol protocol, String srcIp, long srcPort) {
        CommonVirtualServerDefinition vsDefs[] = {new CommonVirtualServerDefinition()};
        vsDefs[0].setName(name);
        vsDefs[0].setAddress(srcIp);
        vsDefs[0].setPort(srcPort);

        if (protocol.equals(LbProtocol.tcp)) {
            vsDefs[0].setProtocol(iControl.CommonProtocolType.PROTOCOL_TCP);
        } else if (protocol.equals(LbProtocol.udp)) {
            vsDefs[0].setProtocol(iControl.CommonProtocolType.PROTOCOL_UDP);
        }

        return vsDefs;
    }

    private iControl.LocalLBVirtualServerVirtualServerResource[] genVirtualServerResource(String poolName) {
        LocalLBVirtualServerVirtualServerResource vsRes[] = {new LocalLBVirtualServerVirtualServerResource()};
        vsRes[0].setType(LocalLBVirtualServerVirtualServerType.RESOURCE_TYPE_POOL);
        vsRes[0].setDefault_pool_name(poolName);
        return vsRes;
    }

    private LocalLBVirtualServerVirtualServerProfile[][] genVirtualServerProfile(LbProtocol protocol) {
        LocalLBVirtualServerVirtualServerProfile vsProfs[][] = {{new LocalLBVirtualServerVirtualServerProfile()}};
        vsProfs[0][0].setProfile_context(LocalLBProfileContextType.PROFILE_CONTEXT_TYPE_ALL);

        if (protocol.equals(LbProtocol.tcp)) {
            vsProfs[0][0].setProfile_name("http");
        } else if (protocol.equals(LbProtocol.udp)) {
            vsProfs[0][0].setProfile_name("udp");
        }

        return vsProfs;
    }

    private LocalLBVirtualServerVirtualServerPersistence[][] genPersistenceProfile(String persistenceProfileName) {
        LocalLBVirtualServerVirtualServerPersistence[][] persistenceProfs = {{new LocalLBVirtualServerVirtualServerPersistence()}};
        persistenceProfs[0][0].setDefault_profile(true);
        persistenceProfs[0][0].setProfile_name(persistenceProfileName);
        return persistenceProfs;
    }

    // Load balancing pool methods

    private void addPool(String virtualServerName, LbAlgorithm algorithm) throws ExecutionException {
        try {
            if (!poolExists(virtualServerName)) {
                if (algorithm.getPersistenceProfileName() != null) {
                    algorithm = LbAlgorithm.RoundRobin;
                }

                s_logger.debug("Adding pool for virtual server " + virtualServerName + " with algorithm " + algorithm);
                _loadbalancerApi.create(genStringArray(virtualServerName), genLbMethod(algorithm), genEmptyMembersArray());

                if (!poolExists(virtualServerName)) {
                    throw new ExecutionException("Failed to create new pool for virtual server " + virtualServerName);
                }
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void deletePool(String virtualServerName) throws ExecutionException {
        try {
            if (poolExists(virtualServerName) && getMembers(virtualServerName).size() == 0) {
                s_logger.debug("Deleting pool for virtual server " + virtualServerName);
                _loadbalancerApi.delete_pool(genStringArray(virtualServerName));

                if (poolExists(virtualServerName)) {
                    throw new ExecutionException("Failed to delete pool for virtual server " + virtualServerName);
                }
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void addPoolMember(String virtualServerName, String destIp, int destPort) throws ExecutionException {
        try {
            String memberIdentifier = destIp + "-" + destPort;

            if (poolExists(virtualServerName) && !memberExists(virtualServerName, memberIdentifier)) {
                s_logger.debug("Adding member " + memberIdentifier + " into pool for virtual server " + virtualServerName);
                _loadbalancerApi.add_member(genStringArray(virtualServerName), genMembers(destIp, destPort));

                if (!memberExists(virtualServerName, memberIdentifier)) {
                    throw new ExecutionException("Failed to add new member " + memberIdentifier + " into pool for virtual server " + virtualServerName);
                }
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private void deleteInactivePoolMembers(String virtualServerName, List<String> activePoolMembers) throws ExecutionException {
        List<String> allPoolMembers = getMembers(virtualServerName);

        for (String member : allPoolMembers) {
            if (!activePoolMembers.contains(member)) {
                String[] ipAndPort = member.split("-");
                deletePoolMember(virtualServerName, ipAndPort[0], Integer.valueOf(ipAndPort[1]));
            }
        }
    }

    private void deletePoolMember(String virtualServerName, String destIp, int destPort) throws ExecutionException {
        try {
            String memberIdentifier = destIp + "-" + destPort;
            List<String> lbPools = getAllStrippedLbPools();

            if (lbPools.contains(virtualServerName) && memberExists(virtualServerName, memberIdentifier)) {
                s_logger.debug("Deleting member " + memberIdentifier + " from pool for virtual server " + virtualServerName);
                _loadbalancerApi.remove_member(genStringArray(virtualServerName), genMembers(destIp, destPort));

                if (memberExists(virtualServerName, memberIdentifier)) {
                    throw new ExecutionException("Failed to delete member " + memberIdentifier + " from pool for virtual server " + virtualServerName);
                }

                if (nodeExists(destIp)) {
                    boolean nodeNeeded = false;
                    done: for (String poolToCheck : lbPools) {
                        for (String memberInPool : getMembers(poolToCheck)) {
                            if (getIpAndPort(memberInPool)[0].equals(destIp)) {
                                nodeNeeded = true;
                                break done;
                            }
                        }
                    }

                    if (!nodeNeeded) {
                        s_logger.debug("Deleting node " + destIp);
                        _nodeApi.delete_node_address(genStringArray(destIp));

                        if (nodeExists(destIp)) {
                            throw new ExecutionException("Failed to delete node " + destIp);
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private boolean poolExists(String poolName) throws ExecutionException {
        return getAllStrippedLbPools().contains(poolName);
    }

    private boolean memberExists(String poolName, String memberIdentifier) throws ExecutionException {
        return getMembers(poolName).contains(memberIdentifier);
    }

    private boolean nodeExists(String destIp) throws RemoteException {
        return getNodes().contains(destIp);
    }

    private String[] getIpAndPort(String memberIdentifier) {
        return memberIdentifier.split("-");
    }

    //This was working with Big IP 10.x
    //getAllLbPools retuns LbPools with user partition information
    //ex: if LbPools is vs-tcp-10.147.44.8-22 then the get_list() will return /Common/vs-tcp-10.147.44.8-22
    public List<String> getAllLbPools() throws ExecutionException {
        try {
            List<String> lbPools = new ArrayList<String>();
            String[] pools = _loadbalancerApi.get_list();

            for (String pool : pools) {
                lbPools.add(pool);
            }

            return lbPools;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    //Big IP 11.x
    //getAllLbPools retuns LbPools without user partition information
    //ex: if LbPools is vs-tcp-10.147.44.8-22 then the get_list() will return /Common/vs-tcp-10.147.44.8-22
    //This method will strip the partition information and only returns a list with LbPools (vs-tcp-10.147.44.8-22)
    public List<String> getAllStrippedLbPools() throws ExecutionException {
        try {
            List<String> lbPools = new ArrayList<String>();
            String[] pools = _loadbalancerApi.get_list();

            for (String pool : pools) {
                 if(pool.contains("/")){
                    lbPools.add(pool.substring(pool.lastIndexOf("/") + 1));
                }else{
                    lbPools.add(pool);
                }
             }
                return lbPools;
            } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private List<String> getMembers(String virtualServerName) throws ExecutionException {
        try {
            List<String> members = new ArrayList<String>();
            String[] virtualServerNames = genStringArray(virtualServerName);
            CommonIPPortDefinition[] membersArray = _loadbalancerApi.get_member(virtualServerNames)[0];

            for (CommonIPPortDefinition member : membersArray) {
                members.add(member.getAddress() + "-" + member.getPort());
            }

            return members;
        } catch (RemoteException e) {
            throw new ExecutionException(e.getMessage());
        }
    }

    private List<String> getNodes() throws RemoteException {
        List<String> nodes = new ArrayList<String>();
        String[] nodesArray = _nodeApi.get_list();

        for (String node : nodesArray) {
            nodes.add(node);
        }

        return nodes;
    }

    private iControl.CommonIPPortDefinition[][] genMembers(String destIp, long destPort) {
        iControl.CommonIPPortDefinition[] membersInnerArray = new iControl.CommonIPPortDefinition[1];
        membersInnerArray[0] = new iControl.CommonIPPortDefinition(destIp, destPort);
        return new iControl.CommonIPPortDefinition[][] {membersInnerArray};
    }

    private iControl.CommonIPPortDefinition[][] genEmptyMembersArray() {
        iControl.CommonIPPortDefinition[] membersInnerArray = new iControl.CommonIPPortDefinition[0];
        return new iControl.CommonIPPortDefinition[][] {membersInnerArray};
    }

    private LocalLBLBMethod[] genLbMethod(LbAlgorithm algorithm) {
        if (algorithm.getMethod() != null) {
            return new LocalLBLBMethod[] {algorithm.getMethod()};
        } else {
            return new LocalLBLBMethod[] {LbAlgorithm.RoundRobin.getMethod()};
        }
    }

    // Stats methods

    private ExternalNetworkResourceUsageAnswer getIpBytesSentAndReceived(ExternalNetworkResourceUsageCommand cmd) throws ExecutionException {
        ExternalNetworkResourceUsageAnswer answer = new ExternalNetworkResourceUsageAnswer(cmd);

        try {

            LocalLBVirtualServerVirtualServerStatistics stats = _virtualServerApi.get_all_statistics();
            for (LocalLBVirtualServerVirtualServerStatisticEntry entry : stats.getStatistics()) {
                String virtualServerIp = entry.getVirtual_server().getAddress();

                virtualServerIp = stripRouteDomainFromAddress(virtualServerIp);

                long[] bytesSentAndReceived = answer.ipBytes.get(virtualServerIp);

                if (bytesSentAndReceived == null) {
                    bytesSentAndReceived = new long[] {0, 0};
                }

                for (CommonStatistic stat : entry.getStatistics()) {
                    int index;
                    if (stat.getType().equals(CommonStatisticType.STATISTIC_CLIENT_SIDE_BYTES_OUT)) {
                        // Add to the outgoing bytes
                        index = 0;
                    } else if (stat.getType().equals(CommonStatisticType.STATISTIC_CLIENT_SIDE_BYTES_IN)) {
                        // Add to the incoming bytes
                        index = 1;
                    } else {
                        continue;
                    }

                    long high = stat.getValue().getHigh();
                    long low = stat.getValue().getLow();
                    long full = getFullUsage(high, low);

                    bytesSentAndReceived[index] += full;
                }

                if (bytesSentAndReceived[0] >= 0 && bytesSentAndReceived[1] >= 0) {
                    answer.ipBytes.put(virtualServerIp, bytesSentAndReceived);
                }
            }
        } catch (Exception e) {
            s_logger.error(e);
            throw new ExecutionException(e.getMessage());
        }

        return answer;
    }

    private long getFullUsage(long high, long low) {
        Double full;
        Double rollOver = new Double(0x7fffffff);
        rollOver = new Double(rollOver.doubleValue() + 1.0);

        if (high >= 0) {
            // shift left 32 bits and mask off new bits to 0's
            full = new Double((high << 32 & 0xffff0000));
        } else {
            // mask off sign bits + shift left by 32 bits then add the sign bit back
            full = new Double(((high & 0x7fffffff) << 32) + (0x80000000 << 32));
        }

        if (low >= 0) {
            // add low to full and we're good
            full = new Double(full.doubleValue() + low);
        } else {
            // add full to low after masking off sign bits and adding 1 to the masked off low order value
            full = new Double(full.doubleValue() + ((low & 0x7fffffff)) + rollOver.doubleValue());
        }

        return full.longValue();
    }

    // Misc methods

    private String tagAddressWithRouteDomain(String address, long vlanTag) {
        return address + _routeDomainIdentifier + vlanTag;
    }

    private String stripRouteDomainFromAddress(String address) {
        int i = address.indexOf(_routeDomainIdentifier);

        if (i > 0) {
            address = address.substring(0, i);
        }

        return address;
    }

    private String genObjectName(Object... args) {
        String objectName = "";

        for (int i = 0; i < args.length; i++) {
            objectName += args[i];
            if (i != args.length - 1) {
                objectName += _objectNamePathSep;
            }
        }

        return objectName;
    }

    private long[] genLongArray(long l) {
        return new long[] {l};
    }

    private static String[] genStringArray(String s) {
        return new String[] {s};
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
