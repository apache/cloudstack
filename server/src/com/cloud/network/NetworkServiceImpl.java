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
package com.cloud.network;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.bouncycastle.util.IPAddress;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.*;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddress.State;
import com.cloud.vm.Nic;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork.BroadcastDomainRange;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.*;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.*;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.AnnotationHelper;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.*;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.*;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import java.util.*;

/**
 * NetworkServiceImpl implements NetworkService.
 */
@Component
@Local(value = { NetworkService.class })
public class NetworkServiceImpl extends ManagerBase implements  NetworkService {
    private static final Logger s_logger = Logger.getLogger(NetworkServiceImpl.class);

    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    VlanDao _vlanDao = null;
    @Inject
    IPAddressDao _ipAddressDao = null;
    @Inject
    AccountDao _accountDao = null;
    @Inject
    DomainDao _domainDao = null;
    @Inject
    UserDao _userDao = null;
    @Inject
    EventDao _eventDao = null;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmDao _userVmDao = null;

    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    NetworkDao _networksDao = null;
    @Inject
    NicDao _nicDao = null;
    @Inject
    RulesManager _rulesMgr;

    @Inject
    UsageEventDao _usageEventDao;
    
    @Inject List<NetworkGuru> _networkGurus;
  
    @Inject
    NetworkDomainDao _networkDomainDao;
    @Inject
    VMInstanceDao _vmDao;

    @Inject
    FirewallRulesDao _firewallDao;
  
    @Inject
    ResourceLimitService _resourceLimitMgr;
   
    @Inject
    DomainManager _domainMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    
    @Inject
    PhysicalNetworkTrafficTypeDao _pNTrafficTypeDao;

    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    StorageNetworkManager _stnwMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    NetworkModel _networkModel;

    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;

    @Inject
    PortForwardingRulesDao _portForwardingDao;
    @Inject
    HostDao _hostDao;
    @Inject
    HostPodDao _hostPodDao;

    int _cidrLimit;
    boolean _allowSubdomainNetworkAccess;

    private Map<String, String> _configs;

    /* Get a list of IPs, classify them by service */
    protected Map<PublicIp, Set<Service>> getIpToServices(List<PublicIp> publicIps, boolean rulesRevoked, boolean includingFirewall) {
        Map<PublicIp, Set<Service>> ipToServices = new HashMap<PublicIp, Set<Service>>();

        if (publicIps != null && !publicIps.isEmpty()) {
            Set<Long> networkSNAT = new HashSet<Long>();
            for (PublicIp ip : publicIps) {
                Set<Service> services = ipToServices.get(ip);
                if (services == null) {
                    services = new HashSet<Service>();
                }
                if (ip.isSourceNat()) {
                    if (!networkSNAT.contains(ip.getAssociatedWithNetworkId())) {
                        services.add(Service.SourceNat);
                        networkSNAT.add(ip.getAssociatedWithNetworkId());
                    } else {
                        CloudRuntimeException ex = new CloudRuntimeException("Multiple generic soure NAT IPs provided for network");
                        // see the IPAddressVO.java class.
                        ex.addProxyObject("user_ip_address", ip.getAssociatedWithNetworkId(), "networkId");
                        throw ex;
                    }
                }
                ipToServices.put(ip, services);

                // if IP in allocating state then it will not have any rules attached so skip IPAssoc to network service
                // provider
                if (ip.getState() == State.Allocating) {
                    continue;
                }

                // check if any active rules are applied on the public IP
                Set<Purpose> purposes = getPublicIpPurposeInRules(ip, false, includingFirewall);
                // Firewall rules didn't cover static NAT
                if (ip.isOneToOneNat() && ip.getAssociatedWithVmId() != null) {
                    if (purposes == null) {
                        purposes = new HashSet<Purpose>();
                    }
                    purposes.add(Purpose.StaticNat);
                }
                if (purposes == null || purposes.isEmpty()) {
                    // since no active rules are there check if any rules are applied on the public IP but are in
// revoking state
                    
                    purposes = getPublicIpPurposeInRules(ip, true, includingFirewall);
                    if (ip.isOneToOneNat()) {
                        if (purposes == null) {
                            purposes = new HashSet<Purpose>();
                        }
                        purposes.add(Purpose.StaticNat);
                    }
                    if (purposes == null || purposes.isEmpty()) {
                        // IP is not being used for any purpose so skip IPAssoc to network service provider
                        continue;
                    } else {
                        if (rulesRevoked) {
                            // no active rules/revoked rules are associated with this public IP, so remove the
// association with the provider
                            ip.setState(State.Releasing);
                        } else {
                            if (ip.getState() == State.Releasing) {
                                // rules are not revoked yet, so don't let the network service provider revoke the IP
// association
                                // mark IP is allocated so that IP association will not be removed from the provider
                                ip.setState(State.Allocated);
                            }
                        }
                    }
                }
                if (purposes.contains(Purpose.StaticNat)) {
                    services.add(Service.StaticNat);
                }
                if (purposes.contains(Purpose.LoadBalancing)) {
                    services.add(Service.Lb);
                }
                if (purposes.contains(Purpose.PortForwarding)) {
                    services.add(Service.PortForwarding);
                }
                if (purposes.contains(Purpose.Vpn)) {
                    services.add(Service.Vpn);
                }
                if (purposes.contains(Purpose.Firewall)) {
                    services.add(Service.Firewall);
                }
                if (services.isEmpty()) {
                    continue;
                }
                ipToServices.put(ip, services);
            }
        }
        return ipToServices;
    }

    protected boolean canIpUsedForNonConserveService(PublicIp ip, Service service) {
        // If it's non-conserve mode, then the new ip should not be used by any other services
        List<PublicIp> ipList = new ArrayList<PublicIp>();
        ipList.add(ip);
        Map<PublicIp, Set<Service>> ipToServices = getIpToServices(ipList, false, false);
        Set<Service> services = ipToServices.get(ip);
        // Not used currently, safe
        if (services == null || services.isEmpty()) {
            return true;
        }
        // Since it's non-conserve mode, only one service should used for IP
        if (services.size() != 1) {
            throw new InvalidParameterException("There are multiple services used ip " + ip.getAddress() + ".");
        }
        if (service != null && !((Service) services.toArray()[0] == service || service.equals(Service.Firewall))) {
            throw new InvalidParameterException("The IP " + ip.getAddress() + " is already used as " + ((Service) services.toArray()[0]).getName() + " rather than " + service.getName());
        }
        return true;
    }

    protected boolean canIpsUsedForNonConserve(List<PublicIp> publicIps) {
        boolean result = true;
        for (PublicIp ip : publicIps) {
            result = canIpUsedForNonConserveService(ip, null);
            if (!result) {
                break;
            }
        }
        return result;
    }

    private boolean canIpsUseOffering(List<PublicIp> publicIps, long offeringId) {
        Map<PublicIp, Set<Service>> ipToServices = getIpToServices(publicIps, false, true);
        Map<Service, Set<Provider>> serviceToProviders = _networkModel.getNetworkOfferingServiceProvidersMap(offeringId);
        NetworkOfferingVO offering = _networkOfferingDao.findById(offeringId);
        //For inline mode checking, using firewall provider for LB instead, because public ip would apply on firewall provider
        if (offering.isInline()) {
            Provider firewallProvider = null;
            if (serviceToProviders.containsKey(Service.Firewall)) {
                firewallProvider = (Provider)serviceToProviders.get(Service.Firewall).toArray()[0];
            }
            Set<Provider> p = new HashSet<Provider>();
            p.add(firewallProvider);
            serviceToProviders.remove(Service.Lb);
            serviceToProviders.put(Service.Lb, p);
        }
        for (PublicIp ip : ipToServices.keySet()) {
            Set<Service> services = ipToServices.get(ip);
            Provider provider = null;
            for (Service service : services) {
                Set<Provider> curProviders = serviceToProviders.get(service);
                if (curProviders == null || curProviders.isEmpty()) {
                    continue;
                }
                Provider curProvider = (Provider) curProviders.toArray()[0];
                if (provider == null) {
                    provider = curProvider;
                    continue;
                }
                // We don't support multiple providers for one service now
                if (!provider.equals(curProvider)) {
                    throw new InvalidParameterException("There would be multiple providers for IP " + ip.getAddress() + " with the new network offering!");
                }
            }
        }
        return true;
    }
    

    

    private Set<Purpose> getPublicIpPurposeInRules(PublicIp ip, boolean includeRevoked, boolean includingFirewall) {
        Set<Purpose> result = new HashSet<Purpose>();
        List<FirewallRuleVO> rules = null;
        if (includeRevoked) {
            rules = _firewallDao.listByIp(ip.getId());
        } else {
            rules = _firewallDao.listByIpAndNotRevoked(ip.getId());
        }

        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (FirewallRuleVO rule : rules) {
            if (rule.getPurpose() != Purpose.Firewall || includingFirewall) {
                result.add(rule.getPurpose());
            }
        }

        return result;
    }

    @Override
    public List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listByZoneAndGuestType(owner.getId(), zoneId, Network.GuestType.Isolated, false);
    }
    
    @Override
    public List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listSourceNATEnabledNetworks(owner.getId(), zoneId, Network.GuestType.Isolated);
    }

    
    

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "allocating Ip", create = true)
    public IpAddress allocateIP(Account ipOwner, boolean isSystem, long zoneId) 
            throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        // check permissions
        _accountMgr.checkAccess(caller, null, false, ipOwner);
        long callerUserId = UserContext.current().getCallerUserId();
        DataCenter zone = _configMgr.getZone(zoneId);
        
        return _networkMgr.allocateIp(ipOwner, isSystem, caller, callerUserId, zone);
    }

    @Override
    @DB
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _configs = _configDao.getConfiguration("Network", params);

        _cidrLimit = NumbersUtil.parseInt(_configs.get(Config.NetworkGuestCidrLimit.key()), 22);

        _allowSubdomainNetworkAccess = Boolean.valueOf(_configs.get(Config.SubDomainNetworkAccess.key()));

        s_logger.info("Network Service is configured.");

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

    protected NetworkServiceImpl() {
    }


    public String allocateSecondaryGuestIP (Account ipOwner, long zoneId, Long nicId, Long networkId, String requestedIp) throws InsufficientAddressCapacityException {

        Long accountId = null;
        Long domainId = null;
        Long vmId = null;
        String ipaddr = null;

        if (networkId == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        Network network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }
        accountId = network.getAccountId();
        domainId = network.getDomainId();

        // verify permissions
        _accountMgr.checkAccess(ipOwner, null, true, network);

        //check whether the nic belongs to user vm.
        NicVO nicVO = _nicDao.findById(nicId);
        if (nicVO == null) {
            throw new InvalidParameterValueException("There is no nic for the " + nicId);
        }

        if (nicVO.getVmType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("The nic is not belongs to user vm");
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        Long id = nicVO.getInstanceId();

        DataCenter zone = _configMgr.getZone(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Invalid zone Id is given");
        }

        s_logger.debug("Calling the ip allocation ...");
        if (dc.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Isolated) {
            try {
                ipaddr = _networkMgr.allocateGuestIP(ipOwner, false,  zoneId, networkId, requestedIp);
            } catch (InsufficientAddressCapacityException e) {
                throw new InvalidParameterValueException("Allocating guest ip for nic failed");
            }
        } else if (dc.getNetworkType() == NetworkType.Basic) {
            Account caller = UserContext.current().getCaller();
            long callerUserId = UserContext.current().getCallerUserId();
            _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
            //handle the basic networks here
            VirtualMachine vm = _userVmDao.findById(nicVO.getInstanceId());
            if (vm == null) {
                throw new InvalidParameterValueException("There is no vm with the nic");
            }
            VMInstanceVO vmi = (VMInstanceVO)vm;
            Long podId = vmi.getPodIdToDeployIn();
            if (podId == null) {
                throw new InvalidParameterValueException("vm pod id is null");
            }
            Pod pod = _hostPodDao.findById(podId);
            if (pod == null) {
                throw new InvalidParameterValueException("vm pod is null");
            }

            try {
                ipaddr = _networkMgr.allocatePublicIpForGuestNic(networkId, dc, pod, caller, requestedIp);
                if (ipaddr == null) {
                    throw new InvalidParameterValueException("Allocating ip to guest nic " + nicId + " failed");
                }
            } catch (InsufficientAddressCapacityException e) {
                s_logger.error("Allocating ip to guest nic " + nicId + " failed");
                return null;
            }
        } else if (isSharedNetworkOfferingWithServices(network.getNetworkOfferingId()) && zone.getNetworkType() == NetworkType.Advanced) {
            // if shared network in the advanced zone, then check the caller against the network for 'AccessType.UseNetwork'
            Account caller = UserContext.current().getCaller();
            long callerUserId = UserContext.current().getCallerUserId();
            _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
            }
        } else {
            s_logger.error("AddIpToVMNic is not supported in this network...");
            return null;
        }

        if (ipaddr != null) {
            // we got the ip addr so up the nics table and secodary ip
            Transaction txn = Transaction.currentTxn();
            txn.start();

            boolean nicSecondaryIpSet = nicVO.getSecondaryIp();
            if (!nicSecondaryIpSet) {
                nicVO.setSecondaryIp(true);
                // commit when previously set ??
                s_logger.debug("Setting nics table ...");
                _nicDao.update(nicId, nicVO);
            }

            s_logger.debug("Setting nic_secondary_ip table ...");
            vmId = nicVO.getInstanceId();
            NicSecondaryIpVO secondaryIpVO = new NicSecondaryIpVO(nicId, ipaddr, vmId, accountId, domainId, networkId);
            _nicSecondaryIpDao.persist(secondaryIpVO);
            txn.commit();
        }
        return ipaddr;
    }

    @DB
    public boolean releaseSecondaryIpFromNic (long ipAddressId) {
        Account caller = UserContext.current().getCaller();
        boolean success = false;

        // Verify input parameters
        NicSecondaryIpVO secIpVO= _nicSecondaryIpDao.findById(ipAddressId);
        if (secIpVO == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id");
        }

        Network network = _networksDao.findById(secIpVO.getNetworkId());

        // verify permissions
        _accountMgr.checkAccess(caller, null, true, network);

        Long nicId = secIpVO.getNicId();
        s_logger.debug("ip id = " + ipAddressId + " nic id = " + nicId);
        //check is this the last secondary ip for NIC
        List<NicSecondaryIpVO> ipList = _nicSecondaryIpDao.listByNicId(nicId);
        boolean lastIp = false;
        if (ipList.size() == 1) {
            // this is the last secondary ip to nic
            lastIp = true;
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (dc == null) {
            throw new InvalidParameterValueException("Invalid zone Id is given");
        }

        s_logger.debug("Calling the ip allocation ...");
        if (dc.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Isolated) {
            //check PF or static NAT is configured on this ip address
            String secondaryIp = secIpVO.getIp4Address();
            List<PortForwardingRuleVO> pfRuleList = _portForwardingDao.listByDestIpAddr(secondaryIp);
            if (pfRuleList.size() != 0) {
                s_logger.debug("VM nic IP " + secondaryIp + " is associated with the port forwarding rule");
                throw new InvalidParameterValueException("Can't remove the secondary ip " + secondaryIp + " is associate with the port forwarding rule");
            }
            //check if the secondary ip associated with any static nat rule
            IPAddressVO publicIpVO = _ipAddressDao.findByVmIp(secondaryIp);
            if (publicIpVO != null) {
                s_logger.debug("VM nic IP " + secondaryIp + " is associated with the static NAT rule public IP address id " + publicIpVO.getId());
                throw new InvalidParameterValueException("Can' remove the ip " + secondaryIp + "is associate with static NAT rule public IP address id " + publicIpVO.getId());
            }
        } else if (dc.getNetworkType() == NetworkType.Basic) {
            IPAddressVO ip = _ipAddressDao.findByIpAndNetworkId(secIpVO.getNetworkId(), secIpVO.getIp4Address());
            if (ip != null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                _networkMgr.markIpAsUnavailable(ip.getId());
                _ipAddressDao.unassignIpAddress(ip.getId());
                txn.commit();
            }
        } else if (isSharedNetworkOfferingWithServices(network.getNetworkOfferingId()) && dc.getNetworkType() == NetworkType.Advanced) {
            throw new InvalidParameterValueException("Not supported for this network now");
        }

        success = removeNicSecondaryIP(secIpVO, lastIp);
        return success;
    }

    boolean removeNicSecondaryIP(NicSecondaryIpVO ipVO, boolean lastIp) {
        Transaction txn = Transaction.currentTxn();
        long nicId = ipVO.getNicId();
        NicVO nic = _nicDao.findById(nicId);

        txn.start();

        if (lastIp) {
            nic.setSecondaryIp(false);
            s_logger.debug("Setting nics secondary ip to false ...");
            _nicDao.update(nicId, nic);
        }

        s_logger.debug("Revoving nic secondary ip entry ...");
        _nicSecondaryIpDao.remove(ipVO.getId());
        txn.commit();
        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_RELEASE, eventDescription = "disassociating Ip", async = true)
    public boolean releaseIpAddress(long ipAddressId) throws InsufficientAddressCapacityException {
        Long userId = UserContext.current().getCallerUserId();
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        IPAddressVO ipVO = _ipAddressDao.findById(ipAddressId);
        if (ipVO == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id");
        }

        if (ipVO.getAllocatedTime() == null) {
            s_logger.debug("Ip Address id= " + ipAddressId + " is not allocated, so do nothing.");
            return true;
        }

        // verify permissions
        if (ipVO.getAllocatedToAccountId() != null) {
            _accountMgr.checkAccess(caller, null, true, ipVO);
        }

        if (ipVO.isSourceNat()) {
            throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
        }

        VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
        if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
            throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
        }

        // Check for account wide pool. It will have an entry for account_vlan_map.
        if (_accountVlanMapDao.findAccountVlanMap(ipVO.getAllocatedToAccountId(), ipVO.getVlanId()) != null) {            
            //see IPaddressVO.java
            InvalidParameterValueException ex = new InvalidParameterValueException("Sepcified IP address uuid belongs to" +
                    " Account wide IP pool and cannot be disassociated");
            ex.addProxyObject("user_ip_address", ipAddressId, "ipAddressId");
            throw ex;
        }

        // don't allow releasing system ip address
        if (ipVO.getSystem()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Can't release system IP address with specified id");
            ex.addProxyObject(ipVO, ipVO.getId(), "systemIpAddrId");
            throw ex;
        }

        boolean success = _networkMgr.disassociatePublicIpAddress(ipAddressId, userId, caller);

        if (success) {
            Long networkId = ipVO.getAssociatedWithNetworkId();
            if (networkId != null) {
                Network guestNetwork = getNetwork(networkId);
                NetworkOffering offering = _configMgr.getNetworkOffering(guestNetwork.getNetworkOfferingId());
                Long vmId = ipVO.getAssociatedWithVmId();
                if (offering.getElasticIp() && vmId != null) {
                    _rulesMgr.getSystemIpAndEnableStaticNatForVm(_userVmDao.findById(vmId), true);
                    return true;
                }
            }
        } else {
            s_logger.warn("Failed to release public ip address id=" + ipAddressId);
        }
        return success;
    }


    @Override
    @DB
    public Network getNetwork(long id) {
        return _networksDao.findById(id);
    }
   

    private void checkSharedNetworkCidrOverlap(Long zoneId, long physicalNetworkId, String cidr) {
        if (zoneId == null || cidr == null) {
            return;
        }

        DataCenter zone = _dcDao.findById(zoneId);
        List<NetworkVO> networks = _networksDao.listByZone(zoneId);
        Map<Long, String> networkToCidr = new HashMap<Long, String>();

        // check for CIDR overlap with all possible CIDR for isolated guest networks
        // in the zone when using external networking
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork.getVnet() != null) {
            String vlanRange[] = pNetwork.getVnet().split("-");
            int lowestVlanTag = Integer.valueOf(vlanRange[0]);
            int highestVlanTag = Integer.valueOf(vlanRange[1]);
            for (int vlan=lowestVlanTag; vlan <= highestVlanTag; ++vlan) {
                int offset = vlan - lowestVlanTag;
                String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
                int cidrSize = 8 + Integer.parseInt(globalVlanBits);
                String guestNetworkCidr = zone.getGuestNetworkCidr();
                String[] cidrTuple = guestNetworkCidr.split("\\/");
                long newCidrAddress = (NetUtils.ip2Long(cidrTuple[0]) & 0xff000000) | (offset << (32 - cidrSize));
                if (NetUtils.isNetworksOverlap(NetUtils.long2Ip(newCidrAddress), cidr)) {
                    throw new InvalidParameterValueException("Specified CIDR for shared network conflict with CIDR that is reserved for zone vlan " + vlan);
                }
            }
        }

        // check for CIDR overlap with all CIDR's of the shared networks in the zone
        for (NetworkVO network : networks) {
            if (network.getGuestType() == GuestType.Isolated) {
                continue;
            }
            if (network.getCidr() != null) {
                networkToCidr.put(network.getId(), network.getCidr());
            }
        }
        if (networkToCidr != null && !networkToCidr.isEmpty()) {
            for (long networkId : networkToCidr.keySet()) {
                String ntwkCidr = networkToCidr.get(networkId);
                if (NetUtils.isNetworksOverlap(ntwkCidr, cidr)) {
                    throw new InvalidParameterValueException("Specified CIDR for shared network conflict with CIDR of a shared network in the zone.");
                }
            }
        }
    }
    
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_CREATE, eventDescription = "creating network")
    public Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        Long networkOfferingId = cmd.getNetworkOfferingId();
        String gateway = cmd.getGateway();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String netmask = cmd.getNetmask();
        String networkDomain = cmd.getNetworkDomain();
        String vlanId = cmd.getVlan();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Account caller = UserContext.current().getCaller();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long zoneId = cmd.getZoneId();
        String aclTypeStr = cmd.getAclType();
        Long domainId = cmd.getDomainId();
        boolean isDomainSpecific = false;
        Boolean subdomainAccess = cmd.getSubdomainAccess();
        Long vpcId = cmd.getVpcId();
        String startIPv6 = cmd.getStartIpv6();
        String endIPv6 = cmd.getEndIpv6();
        String ip6Gateway = cmd.getIp6Gateway();
        String ip6Cidr = cmd.getIp6Cidr();

        // Validate network offering
        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        if (ntwkOff == null || ntwkOff.isSystemOnly()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering by specified id");
            if (ntwkOff != null) {
                ex.addProxyObject(ntwkOff, networkOfferingId, "networkOfferingId");                
                // Get the VO object's table name.
                String tablename = AnnotationHelper.getTableName(ntwkOff);
                if (tablename != null) {
                    ex.addProxyObject(tablename, networkOfferingId, "networkOfferingId");
                } else {
                    s_logger.info("\nCould not retrieve table name (annotation) from " + tablename + " VO proxy object\n");
                }
                throw ex;
            }
            throw ex;
        }
        // validate physical network and zone
        // Check if physical network exists
        PhysicalNetwork pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find a physical network having the specified physical network id");
            }
        }

        if (zoneId == null) {
            zoneId = pNtwk.getDataCenterId();
        }

        DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Specified zone id was not found");
        }
        
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            // See DataCenterVO.java
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation since specified Zone is currently disabled");
            ex.addProxyObject(zone, zoneId, "zoneId");
            throw ex;            
        }

        // Only domain and account ACL types are supported in Acton.
        ACLType aclType = null;
        if (aclTypeStr != null) {
            if (aclTypeStr.equalsIgnoreCase(ACLType.Account.toString())) {
                aclType = ACLType.Account;
            } else if (aclTypeStr.equalsIgnoreCase(ACLType.Domain.toString())) {
                aclType = ACLType.Domain;
            } else {
                throw new InvalidParameterValueException("Incorrect aclType specified. Check the API documentation for supported types");
            }
            // In 3.0 all Shared networks should have aclType == Domain, all Isolated networks aclType==Account
            if (ntwkOff.getGuestType() == GuestType.Isolated) {
                if (aclType != ACLType.Account) {
                    throw new InvalidParameterValueException("AclType should be " + ACLType.Account + " for network of type " + Network.GuestType.Isolated);
                }
            } else if (ntwkOff.getGuestType() == GuestType.Shared) {
                if (!(aclType == ACLType.Domain || aclType == ACLType.Account)) {
                    throw new InvalidParameterValueException("AclType should be " + ACLType.Domain + " or " + 
                ACLType.Account + " for network of type " + Network.GuestType.Shared);
                }
            }
        } else {
            if (ntwkOff.getGuestType() == GuestType.Isolated) {
                aclType = ACLType.Account;
            } else if (ntwkOff.getGuestType() == GuestType.Shared) {
                aclType = ACLType.Domain;
            }
        }

        // Only Admin can create Shared networks
        if (ntwkOff.getGuestType() == GuestType.Shared && !_accountMgr.isAdmin(caller.getType())) {
            throw new InvalidParameterValueException("Only Admins can create network with guest type " + GuestType.Shared);
        }

        // Check if the network is domain specific
        if (aclType == ACLType.Domain) {
            // only Admin can create domain with aclType=Domain
            if (!_accountMgr.isAdmin(caller.getType())) {
                throw new PermissionDeniedException("Only admin can create networks with aclType=Domain");
            }

            // only shared networks can be Domain specific
            if (ntwkOff.getGuestType() != GuestType.Shared) {
                throw new InvalidParameterValueException("Only " + GuestType.Shared + " networks can have aclType=" + ACLType.Domain);
            }

            if (domainId != null) {
                if (ntwkOff.getTrafficType() != TrafficType.Guest || ntwkOff.getGuestType() != Network.GuestType.Shared) {
                    throw new InvalidParameterValueException("Domain level networks are supported just for traffic type "
                + TrafficType.Guest + " and guest type " + Network.GuestType.Shared);
                }

                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {                    
                    throw new InvalidParameterValueException("Unable to find domain by specified id");
                }
                _accountMgr.checkAccess(caller, domain);
            }
            isDomainSpecific = true;

        } else if (subdomainAccess != null) {
            throw new InvalidParameterValueException("Parameter subDomainAccess can be specified only with aclType=Domain");
        }
        Account owner = null;
        if ((cmd.getAccountName() != null && domainId != null) || cmd.getProjectId() != null) {
            owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), domainId, cmd.getProjectId());
        } else {
            owner = caller;
        }

        UserContext.current().setAccountId(owner.getAccountId());

        boolean ipv4 = true, ipv6 = false;
        if (startIP != null) {
        	ipv4 = true;
        }
        if (startIPv6 != null) {
        	ipv6 = true;
        }
        
        if (gateway != null) {
        	try {
        		// getByName on a literal representation will only check validity of the address
        		// http://docs.oracle.com/javase/6/docs/api/java/net/InetAddress.html#getByName(java.lang.String)
        		InetAddress gatewayAddress = InetAddress.getByName(gateway);
        		if (gatewayAddress instanceof Inet6Address) {
        			ipv6 = true;
        		} else {
        			ipv4 = true;
        		}
        	}
        	catch (UnknownHostException e) {
        		s_logger.error("Unable to convert gateway IP to a InetAddress", e);
        		throw new InvalidParameterValueException("Gateway parameter is invalid");
        	}
        }
        
        
        String cidr = null;
        if (ipv4) {
        	// if end ip is not specified, default it to startIp
        	if (startIP != null) {
        		if (!NetUtils.isValidIp(startIP)) {
        			throw new InvalidParameterValueException("Invalid format for the startIp parameter");
        		}
        		if (endIP == null) {
        			endIP = startIP;
        		} else if (!NetUtils.isValidIp(endIP)) {
        			throw new InvalidParameterValueException("Invalid format for the endIp parameter");
        		}
        	}

        	if (startIP != null && endIP != null) {
        		if (!(gateway != null && netmask != null)) {
        			throw new InvalidParameterValueException("gateway and netmask should be defined when startIP/endIP are passed in");
        		}
        	}

        	if (gateway != null && netmask != null) {
        		if (!NetUtils.isValidIp(gateway)) {
        			throw new InvalidParameterValueException("Invalid gateway");
        		}
        		if (!NetUtils.isValidNetmask(netmask)) {
        			throw new InvalidParameterValueException("Invalid netmask");
        		}

        		cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        	}

        }
        
        if (ipv6) {
        	if (endIPv6 == null) {
        		endIPv6 = startIPv6;
        	}
        	_networkModel.checkIp6Parameters(startIPv6, endIPv6, ip6Gateway, ip6Cidr);
        	
        	if (zone.getNetworkType() != NetworkType.Advanced || ntwkOff.getGuestType() != Network.GuestType.Shared) {
        		throw new InvalidParameterValueException("Can only support create IPv6 network with advance shared network!");
        	}
        }
        
        // Regular user can create Guest Isolated Source Nat enabled network only
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL
                && (ntwkOff.getTrafficType() != TrafficType.Guest || ntwkOff.getGuestType() != Network.GuestType.Isolated
                        && areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat))) {
            throw new InvalidParameterValueException("Regular user can create a network only from the network" +
                    " offering having traffic type " + TrafficType.Guest + " and network type "
                    + Network.GuestType.Isolated + " with a service " + Service.SourceNat.getName() + " enabled");
        }

        // Don't allow to specify vlan if the caller is a regular user
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL && (ntwkOff.getSpecifyVlan() || vlanId != null)) {
            throw new InvalidParameterValueException("Regular user is not allowed to specify vlanId");
        }
        
        if (ipv4) {
        	// For non-root admins check cidr limit - if it's allowed by global config value
        	if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && cidr != null) {

        		String[] cidrPair = cidr.split("\\/");
        		int cidrSize = Integer.valueOf(cidrPair[1]);

        		if (cidrSize < _cidrLimit) {
        			throw new InvalidParameterValueException("Cidr size can't be less than " + _cidrLimit);
        		}
        	}
        }

        Collection<String> ntwkProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(ntwkOff, physicalNetworkId).values();
        if (ipv6 && providersConfiguredForExternalNetworking(ntwkProviders)) {
        	throw new InvalidParameterValueException("Cannot support IPv6 on network offering with external devices!");
        }
        
        if (cidr != null && providersConfiguredForExternalNetworking(ntwkProviders)) {
            if (ntwkOff.getGuestType() == GuestType.Shared && (zone.getNetworkType() == NetworkType.Advanced) &&
                    isSharedNetworkOfferingWithServices(networkOfferingId)) {
                // validate if CIDR specified overlaps with any of the CIDR's allocated for isolated networks and shared networks in the zone
                checkSharedNetworkCidrOverlap(zoneId, pNtwk.getId(), cidr);
            } else {
                throw new InvalidParameterValueException("Cannot specify CIDR when using network offering with external devices!");
            }
        }

        // Vlan is created in 2 cases - works in Advance zone only:
        // 1) GuestType is Shared
        // 2) GuestType is Isolated, but SourceNat service is disabled
        boolean createVlan = (startIP != null && endIP != null && zone.getNetworkType() == NetworkType.Advanced
                && ((ntwkOff.getGuestType() == Network.GuestType.Shared)
                || (ntwkOff.getGuestType() == GuestType.Isolated && 
                !areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat))));
        
        if (!createVlan) {
        	// Only support advance shared network in IPv6, which means createVlan is a must
        	if (ipv6) {
        		createVlan = true;
        	}
        }

        // Can add vlan range only to the network which allows it
        if (createVlan && !ntwkOff.getSpecifyIpRanges()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Network offering with specified id doesn't support adding multiple ip ranges");
            ex.addProxyObject(ntwkOff, ntwkOff.getId(), "networkOfferingId");
            String tablename = AnnotationHelper.getTableName(ntwkOff);
            if (tablename != null) {
                ex.addProxyObject(tablename, ntwkOff.getId(), "networkOfferingId");
            } else {
                s_logger.info("\nCould not retrieve table name (annotation) from " + tablename + " VO proxy object\n");
            }
            throw ex;   
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        Long sharedDomainId = null;
        if (isDomainSpecific) {
            if (domainId != null) {
                sharedDomainId = domainId;
            } else {
                sharedDomainId = _domainMgr.getDomain(Domain.ROOT_DOMAIN).getId();
                subdomainAccess = true;
            }
        }

        // default owner to system if network has aclType=Domain
        if (aclType == ACLType.Domain) {
            owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
        }

        //Create guest network
        Network network = null;
        if (vpcId != null) {
            if (!_configMgr.isOfferingForVpc(ntwkOff)){
                throw new InvalidParameterValueException("Network offering can't be used for VPC networks");
            }
            network = _vpcMgr.createVpcGuestNetwork(networkOfferingId, name, displayText, gateway, cidr, vlanId, 
                    networkDomain, owner, sharedDomainId, pNtwk, zoneId, aclType, subdomainAccess, vpcId, caller);
        } else {
            if (_configMgr.isOfferingForVpc(ntwkOff)){
                throw new InvalidParameterValueException("Network offering can be used for VPC networks only");
            }
            network = _networkMgr.createGuestNetwork(networkOfferingId, name, displayText, gateway, cidr, vlanId, 
            		networkDomain, owner, sharedDomainId, pNtwk, zoneId, aclType, subdomainAccess, vpcId, ip6Gateway, ip6Cidr);
        }  

        if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN && createVlan) {
            // Create vlan ip range
            _configMgr.createVlanAndPublicIpRange(pNtwk.getDataCenterId(), network.getId(), physicalNetworkId,
                    false, null, startIP, endIP, gateway, netmask, vlanId, null, startIPv6, endIPv6, ip6Gateway, ip6Cidr);
        }

        txn.commit();

        // if the network offering has persistent set to true, implement the network
        if ( ntwkOff.getIsPersistent() ) {
            try {
                if ( network.getState() == Network.State.Setup ) {
                    s_logger.debug("Network id=" + network.getId() + " is already provisioned");
                    return network;
                }
                DeployDestination dest = new DeployDestination(zone, null, null, null);
                UserVO callerUser = _userDao.findById(UserContext.current().getCallerUserId());
                Journal journal = new Journal.LogJournal("Implementing " + network, s_logger);
                ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, caller);
                s_logger.debug("Implementing network " + network + " as a part of network provision for persistent network");
                Pair<NetworkGuru, NetworkVO> implementedNetwork = _networkMgr.implementNetwork(network.getId(), dest, context);
                if (implementedNetwork.first() == null) {
                    s_logger.warn("Failed to provision the network " + network);
                }
                network = implementedNetwork.second();
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to implement persistent guest network " + network + "due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement persistent guest network");
                e.addProxyObject(network, network.getId(), "networkId");
                throw e;
            }
        }
        return network;
    }

    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) {
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        Long zoneId = cmd.getZoneId();
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String guestIpType = cmd.getGuestIpType();
        String trafficType = cmd.getTrafficType();
        Boolean isSystem = cmd.getIsSystem();
        String aclType = cmd.getAclType();
        Long projectId = cmd.getProjectId();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String path = null;
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        List<String> supportedServicesStr = cmd.getSupportedServices();
        Boolean restartRequired = cmd.getRestartRequired();
        boolean listAll = cmd.listAll();
        boolean isRecursive = cmd.isRecursive();
        Boolean specifyIpRanges = cmd.getSpecifyIpRanges();
        Long vpcId = cmd.getVpcId();
        Boolean canUseForDeploy = cmd.canUseForDeploy();
        Map<String, String> tags = cmd.getTags();
        Boolean forVpc = cmd.getForVpc();

        // 1) default is system to false if not specified
        // 2) reset parameter to false if it's specified by the regular user
        if ((isSystem == null || caller.getType() == Account.ACCOUNT_TYPE_NORMAL) && id == null) {
            isSystem = false;
        }

        // Account/domainId parameters and isSystem are mutually exclusive
        if (isSystem != null && isSystem && (accountName != null || domainId != null)) {
            throw new InvalidParameterValueException("System network belongs to system, account and domainId parameters can't be specified");
        }

        if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain == null) {
                // see DomainVO.java
                throw new InvalidParameterValueException("Specified domain id doesn't exist in the system");
            }

            _accountMgr.checkAccess(caller, domain);
            if (accountName != null) {
                Account owner = _accountMgr.getActiveAccountByName(accountName, domainId);
                if (owner == null) {
                    // see DomainVO.java
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in specified domain");
                }

                _accountMgr.checkAccess(caller, null, true, owner);
                permittedAccounts.add(owner.getId());
            }
        }

        if (!_accountMgr.isAdmin(caller.getType()) || (!listAll && (projectId != null && projectId.longValue() != -1 && domainId == null))) {
            permittedAccounts.add(caller.getId());
            domainId = caller.getDomainId();
        }

        // set project information
        boolean skipProjectNetworks = true;
        if (projectId != null) {
            if (projectId.longValue() == -1) {
                if (!_accountMgr.isAdmin(caller.getType())) {
                    permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
                }
            } else {
                permittedAccounts.clear();
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {                    
                    throw new InvalidParameterValueException("Unable to find project by specified id");
                }
                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    // getProject() returns type ProjectVO.
                    InvalidParameterValueException ex = new InvalidParameterValueException("Account " + caller + " cannot access specified project id");
                    ex.addProxyObject(project, projectId, "projectId");                    
                    throw ex;
                }
                permittedAccounts.add(project.getProjectAccountId());
            }
            skipProjectNetworks = false;
        }

        if (domainId != null) {
            path = _domainDao.findById(domainId).getPath();
        } else {
        path = _domainDao.findById(caller.getDomainId()).getPath();
        } 
        
        if (listAll && domainId == null) {
            isRecursive = true;
        }

        Filter searchFilter = new Filter(NetworkVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<NetworkVO> sb = _networksDao.createSearchBuilder();
        
        if (forVpc != null) {
            if (forVpc) {
                sb.and("vpc", sb.entity().getVpcId(), Op.NNULL);
            } else {
                sb.and("vpc", sb.entity().getVpcId(), Op.NULL);
            }
        }

        // Don't display networks created of system network offerings
        SearchBuilder<NetworkOfferingVO> networkOfferingSearch = _networkOfferingDao.createSearchBuilder();
        networkOfferingSearch.and("systemOnly", networkOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        if (isSystem != null && isSystem) {
            networkOfferingSearch.and("trafficType", networkOfferingSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        }
        sb.join("networkOfferingSearch", networkOfferingSearch, sb.entity().getNetworkOfferingId(), networkOfferingSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        SearchBuilder<DataCenterVO> zoneSearch = _dcDao.createSearchBuilder();
        zoneSearch.and("networkType", zoneSearch.entity().getNetworkType(), SearchCriteria.Op.EQ);
        sb.join("zoneSearch", zoneSearch, sb.entity().getDataCenterId(), zoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        sb.and("removed", sb.entity().getRemoved(), Op.NULL);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count=0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        if (permittedAccounts.isEmpty()) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }


            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
        accountSearch.and("typeNEQ", accountSearch.entity().getType(), SearchCriteria.Op.NEQ);
        accountSearch.and("typeEQ", accountSearch.entity().getType(), SearchCriteria.Op.EQ);
        
        
            sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        List<NetworkVO> networksToReturn = new ArrayList<NetworkVO>();

        if (isSystem == null || !isSystem) {
            if (!permittedAccounts.isEmpty()) {
                //get account level networks
                networksToReturn.addAll(listAccountSpecificNetworks(
                        buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, 
                                physicalNetworkId, aclType, skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags), searchFilter,
                        permittedAccounts));
                //get domain level networks
                if (domainId != null) {
                    networksToReturn
                    .addAll(listDomainLevelNetworks(
                            buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType,
                                    physicalNetworkId, aclType, true, restartRequired, specifyIpRanges, vpcId, tags), searchFilter,
                                    domainId, false));
                }
            } else {
                //add account specific networks
                networksToReturn.addAll(listAccountSpecificNetworksByDomainPath(
                        buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, 
                                physicalNetworkId, aclType, skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags), searchFilter, path,
                        isRecursive));
                //add domain specific networks of domain + parent domains
                networksToReturn.addAll(listDomainSpecificNetworksByDomainPath(
                        buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, 
                                physicalNetworkId, aclType, skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags), searchFilter, path,
                                isRecursive));
                //add networks of subdomains
                if (domainId == null) {
                    networksToReturn
                    .addAll(listDomainLevelNetworks(
                            buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType,
                                    physicalNetworkId, aclType, true, restartRequired, specifyIpRanges, vpcId, tags), searchFilter,
                                    caller.getDomainId(), true));
                }
            }
        } else {
            networksToReturn = _networksDao.search(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId,
                    guestIpType, trafficType, physicalNetworkId, null, skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags),
                    searchFilter);
        }

        if (supportedServicesStr != null && !supportedServicesStr.isEmpty() && !networksToReturn.isEmpty()) {
            List<NetworkVO> supportedNetworks = new ArrayList<NetworkVO>();
            Service[] suppportedServices = new Service[supportedServicesStr.size()];
            int i = 0;
            for (String supportedServiceStr : supportedServicesStr) {
                Service service = Service.getService(supportedServiceStr);
                if (service == null) {
                    throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                } else {
                    suppportedServices[i] = service;
                }
                i++;
            }

            for (NetworkVO network : networksToReturn) {
                if (areServicesSupportedInNetwork(network.getId(), suppportedServices)) {
                    supportedNetworks.add(network);
                }
            }

            networksToReturn=supportedNetworks;
        }
        
        if (canUseForDeploy != null) {
            List<NetworkVO> networksForDeploy = new ArrayList<NetworkVO>();
            for (NetworkVO network : networksToReturn) {
                if (_networkModel.canUseForDeploy(network) == canUseForDeploy) {
                    networksForDeploy.add(network);
                }
            }
            
            networksToReturn=networksForDeploy;
        }
        
            return networksToReturn;
        }

    

    private SearchCriteria<NetworkVO> buildNetworkSearchCriteria(SearchBuilder<NetworkVO> sb, String keyword, Long id, 
            Boolean isSystem, Long zoneId, String guestIpType, String trafficType, Long physicalNetworkId,
            String aclType, boolean skipProjectNetworks, Boolean restartRequired, Boolean specifyIpRanges, Long vpcId, Map<String, String> tags) {

        SearchCriteria<NetworkVO> sc = sb.create();

        if (isSystem != null) {
            sc.setJoinParameters("networkOfferingSearch", "systemOnly", isSystem);
        }

        if (keyword != null) {
            SearchCriteria<NetworkVO> ssc = _networksDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (guestIpType != null) {
            sc.addAnd("guestType", SearchCriteria.Op.EQ, guestIpType);
        }

        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }

        if (aclType != null) {
            sc.addAnd("aclType", SearchCriteria.Op.EQ, aclType.toString());
        }

        if (physicalNetworkId != null) {
            sc.addAnd("physicalNetworkId", SearchCriteria.Op.EQ, physicalNetworkId);
        }

        if (skipProjectNetworks) {
            sc.setJoinParameters("accountSearch", "typeNEQ", Account.ACCOUNT_TYPE_PROJECT);
        } else {
            sc.setJoinParameters("accountSearch", "typeEQ", Account.ACCOUNT_TYPE_PROJECT);
        }

        if (restartRequired != null) {
            sc.addAnd("restartRequired", SearchCriteria.Op.EQ, restartRequired);
        }

        if (specifyIpRanges != null) {
            sc.addAnd("specifyIpRanges", SearchCriteria.Op.EQ, specifyIpRanges);
        }
        
        if (vpcId != null) {
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
        }
        
        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.Network.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        return sc;
    }

    private List<NetworkVO> listDomainLevelNetworks(SearchCriteria<NetworkVO> sc, Filter searchFilter, long domainId, boolean parentDomainsOnly) {
        List<Long> networkIds = new ArrayList<Long>();
        Set<Long> allowedDomains = _domainMgr.getDomainParentIds(domainId);
        List<NetworkDomainVO> maps = _networkDomainDao.listDomainNetworkMapByDomain(allowedDomains.toArray());

        for (NetworkDomainVO map : maps) {
            if (map.getDomainId() == domainId && parentDomainsOnly) {
                continue;
            }
            boolean subdomainAccess = (map.isSubdomainAccess() != null) ? map.isSubdomainAccess() : getAllowSubdomainAccessGlobal();
            if (map.getDomainId() == domainId || subdomainAccess) {
                networkIds.add(map.getNetworkId());
            }
        }

        if (!networkIds.isEmpty()) {
            SearchCriteria<NetworkVO> domainSC = _networksDao.createSearchCriteria();
            domainSC.addAnd("id", SearchCriteria.Op.IN, networkIds.toArray());
            domainSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Domain.toString());

            sc.addAnd("id", SearchCriteria.Op.SC, domainSC);
            return _networksDao.search(sc, searchFilter);
        } else {
            return new ArrayList<NetworkVO>();
        }
    }

    private List<NetworkVO> listAccountSpecificNetworks(SearchCriteria<NetworkVO> sc, Filter searchFilter, List<Long> permittedAccounts) {
        SearchCriteria<NetworkVO> accountSC = _networksDao.createSearchCriteria();
        if (!permittedAccounts.isEmpty()) {
            accountSC.addAnd("accountId", SearchCriteria.Op.IN, permittedAccounts.toArray());
        }

        accountSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Account.toString());

        sc.addAnd("id", SearchCriteria.Op.SC, accountSC);
        return _networksDao.search(sc, searchFilter);
    }

    private List<NetworkVO> listAccountSpecificNetworksByDomainPath(SearchCriteria<NetworkVO> sc, Filter searchFilter, String path, boolean isRecursive) {
        SearchCriteria<NetworkVO> accountSC = _networksDao.createSearchCriteria();
        accountSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Account.toString());

        if (path != null) {
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", path + "%");
            } else {
                sc.setJoinParameters("domainSearch", "path", path);
            }
        }

        sc.addAnd("id", SearchCriteria.Op.SC, accountSC);
        return _networksDao.search(sc, searchFilter);
    }

    private List<NetworkVO> listDomainSpecificNetworksByDomainPath(SearchCriteria<NetworkVO> sc, Filter searchFilter,
            String path, boolean isRecursive) {

        Set<Long> allowedDomains = new HashSet<Long>();
        if (path != null) {
            if (isRecursive) {
                allowedDomains = _domainMgr.getDomainChildrenIds(path);
            } else {
                Domain domain = _domainDao.findDomainByPath(path);
                allowedDomains.add(domain.getId());
            }
        }

        List<Long> networkIds = new ArrayList<Long>();
        
        List<NetworkDomainVO> maps = _networkDomainDao.listDomainNetworkMapByDomain(allowedDomains.toArray());

        for (NetworkDomainVO map : maps) {
            networkIds.add(map.getNetworkId());
        }

        if (!networkIds.isEmpty()) {
            SearchCriteria<NetworkVO> domainSC = _networksDao.createSearchCriteria();
            domainSC.addAnd("id", SearchCriteria.Op.IN, networkIds.toArray());
            domainSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Domain.toString());

            sc.addAnd("id", SearchCriteria.Op.SC, domainSC);
        return _networksDao.search(sc, searchFilter);
        } else {
            return new ArrayList<NetworkVO>();
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_DELETE, eventDescription = "deleting network", async = true)
    public boolean deleteNetwork(long networkId) {

        Account caller = UserContext.current().getCaller();

        // Verify network id
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            // see NetworkVO.java
            
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find network with specified id");
            ex.addProxyObject(network, networkId, "networkId");            
            throw ex;
        }

        // don't allow to delete system network
        if (isNetworkSystem(network)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Network with specified id is system and can't be removed");
            ex.addProxyObject(network, network.getId(), "networkId");            
            throw ex;
        }

        Account owner = _accountMgr.getAccount(network.getAccountId());

        // Perform permission check
        _accountMgr.checkAccess(caller, null, true, network);

        User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);

        return _networkMgr.destroyNetwork(networkId, context);
    }

    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_RESTART, eventDescription = "restarting network", async = true)
    public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // This method restarts all network elements belonging to the network and re-applies all the rules
        Long networkId = cmd.getNetworkId();

        User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        Account callerAccount = _accountMgr.getActiveAccountById(callerUser.getAccountId());

        // Check if network exists
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {            
            InvalidParameterValueException ex = new InvalidParameterValueException("Network with specified id doesn't exist");
            ex.addProxyObject("networks", networkId, "networkId");
            throw ex;
        }

        // Don't allow to restart network if it's not in Implemented/Setup state
        if (!(network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup)) {
            throw new InvalidParameterValueException("Network is not in the right state to be restarted. Correct states are: " + Network.State.Implemented + ", " + Network.State.Setup);
        }
        
        if (network.getBroadcastDomainType() == BroadcastDomainType.Lswitch ) {
        	/** 
        	 * Unable to restart these networks now.
        	 * TODO Restarting a SDN based network requires updating the nics and the configuration
        	 * in the controller. This requires a non-trivial rewrite of the restart procedure.
        	 */
        	throw new InvalidParameterException("Unable to restart a running SDN network.");
        }

        _accountMgr.checkAccess(callerAccount, null, true, network);

        boolean success = _networkMgr.restartNetwork(networkId, callerAccount, callerUser, cleanup);

        if (success) {
            s_logger.debug("Network id=" + networkId + " is restarted successfully.");
        } else {
            s_logger.warn("Network id=" + networkId + " failed to restart.");
        }

        return success;
    }

    @Override
    public int getActiveNicsInNetwork(long networkId) {
        return _networksDao.getActiveNicsIn(networkId);
    }

    

   

    
    protected Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {

        if (!areServicesSupportedByNetworkOffering(offering.getId(), service)) {
            // TBD: We should be sending networkOfferingId and not the offering object itself.  
            throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the network offering " + offering);
        }

        Map<Capability, String> serviceCapabilities = new HashMap<Capability, String>();

        // get the Provider for this Service for this offering
        List<String> providers = _ntwkOfferingSrvcDao.listProvidersForServiceForNetworkOffering(offering.getId(), service);
        if (providers.isEmpty()) {
            // TBD: We should be sending networkOfferingId and not the offering object itself.
            throw new InvalidParameterValueException("Service " + service.getName() + " is not supported by the network offering " + offering);
        }

        // FIXME - in post 3.0 we are going to support multiple providers for the same service per network offering, so
        // we have to calculate capabilities for all of them
        String provider = providers.get(0);

        // FIXME we return the capabilities of the first provider of the service - what if we have multiple providers
        // for same Service?
        NetworkElement element = _networkModel.getElementImplementingProvider(provider);
        if (element != null) {
            Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
            ;

            if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                // TBD: We should be sending providerId and not the offering object itself.
                throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() + " implementing Provider=" + provider);
            }
            serviceCapabilities = elementCapabilities.get(service);
        }

        return serviceCapabilities;
    }
    
    
    @Override
    public IpAddress getIp(long ipAddressId) {
        return _ipAddressDao.findById(ipAddressId);
    }

    
    protected boolean providersConfiguredForExternalNetworking(Collection<String> providers) {
        for(String providerStr : providers){
            Provider provider = Network.Provider.getProvider(providerStr);
            if(provider.isExternal()){
                return true;
            }
        }
        return false;
    }

    protected boolean isSharedNetworkOfferingWithServices(long networkOfferingId) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if ( (networkOffering.getGuestType()  == Network.GuestType.Shared) && (
                areServicesSupportedByNetworkOffering(networkOfferingId, Service.SourceNat) ||
                areServicesSupportedByNetworkOffering(networkOfferingId, Service.StaticNat) ||
                areServicesSupportedByNetworkOffering(networkOfferingId, Service.Firewall) ||
                areServicesSupportedByNetworkOffering(networkOfferingId, Service.PortForwarding) ||
                areServicesSupportedByNetworkOffering(networkOfferingId, Service.Lb))) {
            return true;
        }
        return false;
    }

    
    protected boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        return (_ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(networkOfferingId, services));
    }

    
    protected boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        return (_ntwkSrvcDao.areServicesSupportedInNetwork(networkId, services));
    }

 
        
   

    

    private boolean checkForNonStoppedVmInNetwork(long networkId) {
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId, VirtualMachine.State.Starting, 
                VirtualMachine.State.Running, VirtualMachine.State.Migrating, VirtualMachine.State.Stopping);
        return vms.isEmpty();
    }
    
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_UPDATE, eventDescription = "updating network", async = true)
    public Network updateGuestNetwork(long networkId, String name, String displayText, Account callerAccount, 
            User callerUser, String domainSuffix, Long networkOfferingId, Boolean changeCidr, String guestVmCidr) {
        boolean restartNetwork = false;

        // verify input parameters
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            // see NetworkVO.java
            InvalidParameterValueException ex = new InvalidParameterValueException("Specified network id doesn't exist in the system");
            ex.addProxyObject("networks", networkId, "networkId");
            throw ex;
        }

        // don't allow to update network in Destroy state
        if (network.getState() == Network.State.Destroy) {
            throw new InvalidParameterValueException("Don't allow to update network in state " + Network.State.Destroy);
        }

        // Don't allow to update system network
        NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (offering.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system networks");
        }

        // allow to upgrade only Guest networks
        if (network.getTrafficType() != Networks.TrafficType.Guest) {
            throw new InvalidParameterValueException("Can't allow networks which traffic type is not " + TrafficType.Guest);
        }
        
        _accountMgr.checkAccess(callerAccount, null, true, network);

        if (name != null) {
            network.setName(name);
        }

        if (displayText != null) {
            network.setDisplayText(displayText);
        }

        // network offering and domain suffix can be updated for Isolated networks only in 3.0
        if ((networkOfferingId != null || domainSuffix != null) && network.getGuestType() != GuestType.Isolated) {
            throw new InvalidParameterValueException("NetworkOffering and domain suffix upgrade can be perfomed for Isolated networks only");
        }

        boolean networkOfferingChanged = false;

        long oldNetworkOfferingId = network.getNetworkOfferingId();
        NetworkOffering oldNtwkOff = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if (networkOfferingId != null) {
            if (networkOffering == null || networkOffering.isSystemOnly()) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering with specified id");
                ex.addProxyObject(networkOffering, networkOfferingId, "networkOfferingId");                
                throw ex;
            }
            
            // network offering should be in Enabled state
            if (networkOffering.getState() != NetworkOffering.State.Enabled) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Network offering with specified id is not in " + NetworkOffering.State.Enabled + " state, can't upgrade to it");
                ex.addProxyObject(networkOffering, networkOfferingId, "networkOfferingId");                
                throw ex;
            }
            //can't update from vpc to non-vpc network offering
            boolean forVpcNew = _configMgr.isOfferingForVpc(networkOffering);
            boolean vorVpcOriginal = _configMgr.isOfferingForVpc(_configMgr.getNetworkOffering(oldNetworkOfferingId));
            if (forVpcNew != vorVpcOriginal) {
                String errMsg = forVpcNew ? "a vpc offering " : "not a vpc offering";
                throw new InvalidParameterValueException("Can't update as the new offering is " + errMsg);
            }

            if (networkOfferingId != oldNetworkOfferingId) {
                Collection<String> newProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(networkOffering, network.getPhysicalNetworkId()).values();
                Collection<String> oldProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(oldNtwkOff, network.getPhysicalNetworkId()).values();
                
                if (providersConfiguredForExternalNetworking(newProviders) != providersConfiguredForExternalNetworking(oldProviders)
                        && !changeCidr) {
                    throw new InvalidParameterValueException("Updating network failed since guest CIDR needs to be changed!");
                }
                if (changeCidr) {
                    if (!checkForNonStoppedVmInNetwork(network.getId())) {
                        InvalidParameterValueException ex = new InvalidParameterValueException("All user vm of network of specified id should be stopped before changing CIDR!");
                        ex.addProxyObject(network, networkId, "networkId");                       
                        throw ex;
                    }
                }
                // check if the network is upgradable
                if (!canUpgrade(network, oldNetworkOfferingId, networkOfferingId)) {
                    throw new InvalidParameterValueException("Can't upgrade from network offering " + oldNetworkOfferingId + " to " + networkOfferingId + "; check logs for more information");
                }
                restartNetwork = true;
                networkOfferingChanged = true;
            }
        }

        Map<String, String> newSvcProviders = new HashMap<String, String>();
        if (networkOfferingChanged) {
            newSvcProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(_configMgr.getNetworkOffering(networkOfferingId), network.getPhysicalNetworkId());
        }

        // don't allow to modify network domain if the service is not supported
        if (domainSuffix != null) {
            // validate network domain
            if (!NetUtils.verifyDomainName(domainSuffix)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }

            long offeringId = oldNetworkOfferingId;
            if (networkOfferingId != null) {
                offeringId = networkOfferingId;
            }

            Map<Network.Capability, String> dnsCapabilities = getNetworkOfferingServiceCapabilities(_configMgr.getNetworkOffering(offeringId), Service.Dns);
            String isUpdateDnsSupported = dnsCapabilities.get(Capability.AllowDnsSuffixModification);
            if (isUpdateDnsSupported == null || !Boolean.valueOf(isUpdateDnsSupported)) {
                // TBD: use uuid instead of networkOfferingId. May need to hardcode tablename in call to addProxyObject().
                throw new InvalidParameterValueException("Domain name change is not supported by the network offering id=" + networkOfferingId);
            }

            network.setNetworkDomain(domainSuffix);
            // have to restart the network
            restartNetwork = true;
        }

        //IP reservation checks
        // allow reservation only to Isolated Guest networks
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        String networkCidr = network.getNetworkCidr();

        if (guestVmCidr!= null ) {
            if(dc.getNetworkType() == NetworkType.Basic) {
                throw new InvalidParameterValueException("Guest VM CIDR can't be specified for zone with " + NetworkType.Basic  + " networking");
            }
            if (network.getGuestType() != GuestType.Isolated) {
                throw new InvalidParameterValueException("Can only allow IP Reservation in networks with guest type " + GuestType.Isolated);
            }
            if (networkOfferingChanged == true) {
                throw new InvalidParameterValueException("Cannot specify this nework offering change and guestVmCidr at same time. Specify only one.");
            }
            if (!(network.getState() == Network.State.Implemented)) {
                throw new InvalidParameterValueException ("The network must be in " +  Network.State.Implemented + " state. IP Reservation cannot be applied in " + network.getState() + " state");
            }
            if (!NetUtils.isValidCIDR(guestVmCidr)) {
                throw new InvalidParameterValueException ("Invalid format of Guest VM CIDR.");
            }
            if (!NetUtils.validateGuestCidr(guestVmCidr)) {
                throw new InvalidParameterValueException ("Invalid format of Guest VM CIDR. Make sure it is RFC1918 compliant. ");
            }

            // If networkCidr is null it implies that there was no prior IP reservation, so the network cidr is network.getCidr()
            // But in case networkCidr is a non null value (IP reservation already exists), it implies network cidr is networkCidr
            if (networkCidr != null && ! NetUtils.isNetworkAWithinNetworkB(guestVmCidr, networkCidr)) {
                    throw new InvalidParameterValueException ("Invalid value of Guest VM CIDR. For IP Reservation, Guest VM CIDR  should be a subset of network CIDR : " + networkCidr);
            } else {
                if (! NetUtils.isNetworkAWithinNetworkB(guestVmCidr, network.getCidr())) {
                     throw new InvalidParameterValueException ("Invalid value of Guest VM CIDR. For IP Reservation, Guest VM CIDR  should be a subset of network CIDR :  " + network.getCidr());
                }
            }

            // This check makes sure there are no active IPs existing outside the guestVmCidr in the network
                String[] guestVmCidrPair = guestVmCidr.split("\\/");
                Long size = Long.valueOf(guestVmCidrPair[1]);
                List<NicVO> nicsPresent = _nicDao.listByNetworkId(networkId);

                String cidrIpRange[] = NetUtils.getIpRangeFromCidr(guestVmCidrPair[0], size);
                s_logger.info("The start IP of the specified guest vm cidr is: " +  cidrIpRange[0] +" and end IP is: " +  cidrIpRange[1]);
                long startIp = NetUtils.ip2Long(cidrIpRange[0]);
                long endIp = NetUtils.ip2Long(cidrIpRange[1]);
                long range =  endIp - startIp + 1;
                s_logger.info("The specified guest vm cidr has " +  range + " IPs");

                for (NicVO nic : nicsPresent) {
                    long nicIp = NetUtils.ip2Long(nic.getIp4Address());
                    //check if nic IP is outside the guest vm cidr
                    if (nicIp < startIp || nicIp > endIp) {
                        if(!(nic.getState() == Nic.State.Deallocating)) {
                            throw new InvalidParameterValueException("Active IPs like " + nic.getIp4Address() + " exist outside the Guest VM CIDR. Cannot apply reservation ");
                            }
                        }
                    }

                // When reservation is applied for the first time, network_cidr will be null
                // Populate it with the actual network cidr
                if (network.getNetworkCidr() == null) {
                    network.setNetworkCidr(network.getCidr());
                }

                // Condition for IP Reservation reset : guestVmCidr and network CIDR are same
                if (network.getNetworkCidr().equals(guestVmCidr)) {
                    s_logger.warn("Guest VM CIDR and Network CIDR both are same, reservation will reset.");
                    network.setNetworkCidr(null);
                }
                // Finally update "cidr" with the guestVmCidr
                // which becomes the effective address space for CloudStack guest VMs
                network.setCidr(guestVmCidr);
                _networksDao.update(networkId, network);
                s_logger.info("IP Reservation has been applied. The new CIDR for Guests Vms is " + guestVmCidr);
            }

        ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);
        // 1) Shutdown all the elements and cleanup all the rules. Don't allow to shutdown network in intermediate
        // states - Shutdown and Implementing
        boolean validStateToShutdown = (network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup || network.getState() == Network.State.Allocated);
        if (restartNetwork) {
            if (validStateToShutdown) {
                if (!changeCidr) {
                    s_logger.debug("Shutting down elements and resources for network id=" + networkId + " as a part of network update");

                    if (!_networkMgr.shutdownNetworkElementsAndResources(context, true, network)) {
                        s_logger.warn("Failed to shutdown the network elements and resources as a part of network restart: " + network);
                        CloudRuntimeException ex = new CloudRuntimeException("Failed to shutdown the network elements and resources as a part of update to network of specified id");
                        ex.addProxyObject(network, networkId, "networkId");                       
                        throw ex;
                    }
                } else {
                    // We need to shutdown the network, since we want to re-implement the network.
                    s_logger.debug("Shutting down network id=" + networkId + " as a part of network update");

                    //check if network has reservation
                    if(NetUtils.isNetworkAWithinNetworkB(network.getCidr(), network.getNetworkCidr())) {
                        s_logger.warn ("Existing IP reservation will become ineffective for the network with id =  " + networkId + " You need to reapply reservation after network reimplementation.");
                        //set cidr to the newtork cidr
                        network.setCidr(network.getNetworkCidr());
                        //set networkCidr to null to bring network back to no IP reservation state
                        network.setNetworkCidr(null);
                    }

                    if (!_networkMgr.shutdownNetwork(network.getId(), context, true)) {
                        s_logger.warn("Failed to shutdown the network as a part of update to network with specified id");
                        CloudRuntimeException ex = new CloudRuntimeException("Failed to shutdown the network as a part of update of specified network id");
                        ex.addProxyObject(network, networkId, "networkId");                        
                        throw ex;
                    }
                }
            } else {
                CloudRuntimeException ex = new CloudRuntimeException("Failed to shutdown the network elements and resources as a part of update to network with specified id; network is in wrong state: " + network.getState());
                ex.addProxyObject(network, networkId, "networkId");                
                throw ex;
            }
        }

        // 2) Only after all the elements and rules are shutdown properly, update the network VO
        // get updated network
        Network.State networkState = _networksDao.findById(networkId).getState();
        boolean validStateToImplement = (networkState == Network.State.Implemented || networkState == Network.State.Setup || networkState == Network.State.Allocated);
        if (restartNetwork && !validStateToImplement) {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to implement the network elements and resources as a part of update to network with specified id; network is in wrong state: " + networkState);
            ex.addProxyObject(network, networkId, "networkId");
            throw ex;
        }

        if (networkOfferingId != null) {
            if (networkOfferingChanged) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                network.setNetworkOfferingId(networkOfferingId);
                _networksDao.update(networkId, network, newSvcProviders);
                // get all nics using this network
                // log remove usage events for old offering
                // log assign usage events for new offering
                List<NicVO> nics = _nicDao.listByNetworkId(networkId);
                for (NicVO nic : nics) {
                    long vmId = nic.getInstanceId();
                    VMInstanceVO vm = _vmDao.findById(vmId);
                    if (vm == null) {
                        s_logger.error("Vm for nic " + nic.getId() + " not found with Vm Id:" + vmId);
                        continue;
                    }
                    long isDefault = (nic.isDefaultNic()) ? 1 : 0;
                    UsageEventUtils.saveUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), null, oldNetworkOfferingId, null, 0L);
                    UsageEventUtils.saveUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getHostName(), networkOfferingId, null, isDefault);
                }
                txn.commit();
            }   else {
                network.setNetworkOfferingId(networkOfferingId);
                _networksDao.update(networkId, network, _networkMgr.finalizeServicesAndProvidersForNetwork(_configMgr.getNetworkOffering(networkOfferingId), network.getPhysicalNetworkId()));
            }
        }   else {
            _networksDao.update(networkId, network);
        }

        // 3) Implement the elements and rules again
        if (restartNetwork) {
            if (network.getState() != Network.State.Allocated) {
                DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);
                s_logger.debug("Implementing the network " + network + " elements and resources as a part of network update");
                try {
                    if (!changeCidr) {
                        _networkMgr.implementNetworkElementsAndResources(dest, context, network, _networkOfferingDao.findById(network.getNetworkOfferingId()));
                    } else {
                        _networkMgr.implementNetwork(network.getId(), dest, context);
                    }
                } catch (Exception ex) {
                    s_logger.warn("Failed to implement network " + network + " elements and resources as a part of network update due to ", ex);
                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id) elements and resources as a part of network update");
                    e.addProxyObject(network, networkId, "networkId");                    
                    throw e;
                }
            }
        }

        // 4) if network has been upgraded from a non persistent ntwk offering to a persistent ntwk offering,
        // implement the network if its not already
        if ( networkOfferingChanged && !oldNtwkOff.getIsPersistent() && networkOffering.getIsPersistent()) {
            if( network.getState() == Network.State.Allocated) {
                try {
                    DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);
                    _networkMgr.implementNetwork(network.getId(), dest, context);
                } catch (Exception ex) {
                    s_logger.warn("Failed to implement network " + network + " elements and resources as a part o" +
                            "f network update due to ", ex);
                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified" +
                            " id) elements and resources as a part of network update");
                    e.addProxyObject(network, networkId, "networkId");
                    throw e;
                }
            }
        }

        return getNetwork(network.getId());
    }




    protected Set<Long> getAvailableIps(Network network, String requestedIp) {
        String[] cidr = network.getCidr().split("/");
        List<String> ips = _nicDao.listIpAddressInNetwork(network.getId());
        Set<Long> allPossibleIps = NetUtils.getAllIpsFromCidr(cidr[0], Integer.parseInt(cidr[1]));
        Set<Long> usedIps = new TreeSet<Long>(); 
        
        for (String ip : ips) {
            if (requestedIp != null && requestedIp.equals(ip)) {
                s_logger.warn("Requested ip address " + requestedIp + " is already in use in network" + network);
                return null;
            }

            usedIps.add(NetUtils.ip2Long(ip));
        }
        if (usedIps.size() != 0) {
            allPossibleIps.removeAll(usedIps);
        }
        return allPossibleIps;
    }


  
    protected boolean canUpgrade(Network network, long oldNetworkOfferingId, long newNetworkOfferingId) {
        NetworkOffering oldNetworkOffering = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
        NetworkOffering newNetworkOffering = _networkOfferingDao.findById(newNetworkOfferingId);

        // can upgrade only Isolated networks
        if (oldNetworkOffering.getGuestType() != GuestType.Isolated) {
            throw new InvalidParameterValueException("NetworkOfferingId can be upgraded only for the network of type " + GuestType.Isolated);
        }

        // security group service should be the same
        if (areServicesSupportedByNetworkOffering(oldNetworkOfferingId, Service.SecurityGroup) != areServicesSupportedByNetworkOffering(newNetworkOfferingId, Service.SecurityGroup)) {
            s_logger.debug("Offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different securityGroupProperty, can't upgrade");
            return false;
        }

        // Type of the network should be the same
        if (oldNetworkOffering.getGuestType() != newNetworkOffering.getGuestType()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " are of different types, can't upgrade");
            return false;
        }

        // tags should be the same
        if (newNetworkOffering.getTags() != null) {
            if (oldNetworkOffering.getTags() == null) {
                s_logger.debug("New network offering id=" + newNetworkOfferingId + " has tags and old network offering id=" + oldNetworkOfferingId + " doesn't, can't upgrade");
                return false;
            }
            if (!oldNetworkOffering.getTags().equalsIgnoreCase(newNetworkOffering.getTags())) {
                s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different tags, can't upgrade");
                return false;
            }
        }

        // Traffic types should be the same
        if (oldNetworkOffering.getTrafficType() != newNetworkOffering.getTrafficType()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different traffic types, can't upgrade");
            return false;
        }

        // specify vlan should be the same
        if (oldNetworkOffering.getSpecifyVlan() != newNetworkOffering.getSpecifyVlan()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different values for specifyVlan, can't upgrade");
            return false;
        }

        // specify ipRanges should be the same
        if (oldNetworkOffering.getSpecifyIpRanges() != newNetworkOffering.getSpecifyIpRanges()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different values for specifyIpRangess, can't upgrade");
            return false;
        }

        // Check all ips
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }
        if (oldNetworkOffering.isConserveMode() && !newNetworkOffering.isConserveMode()) {
            if (!canIpsUsedForNonConserve(publicIps)) {
                return false;
            }
        }

        return canIpsUseOffering(publicIps, newNetworkOfferingId);
    }

    

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_CREATE, eventDescription = "Creating Physical Network", create = true)
    public PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> 
    isolationMethods, String broadcastDomainRangeStr, Long domainId, List<String> tags, String name) {

        // Check if zone exists
        if (zoneId == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        if (Grouping.AllocationState.Enabled == zone.getAllocationState()) {
            // TBD: Send uuid instead of zoneId; may have to hardcode tablename in call to addProxyObject().
            throw new PermissionDeniedException("Cannot create PhysicalNetwork since the Zone is currently enabled, zone Id: " + zoneId);
        }

        NetworkType zoneType = zone.getNetworkType();

        if (zoneType == NetworkType.Basic) {
            if (!_physicalNetworkDao.listByZone(zoneId).isEmpty()) {
                // TBD: Send uuid instead of zoneId; may have to hardcode tablename in call to addProxyObject().
                throw new CloudRuntimeException("Cannot add the physical network to basic zone id: " + zoneId + ", there is a physical network already existing in this basic Zone");
            }
        }
        if (tags != null && tags.size() > 1) {
            throw new InvalidParameterException("Only one tag can be specified for a physical network at this time");
        }

        if (isolationMethods != null && isolationMethods.size() > 1) {
            throw new InvalidParameterException("Only one isolationMethod can be specified for a physical network at this time");
        }

        int vnetStart = 0;
        int vnetEnd = 0;
        if (vnetRange != null) {
            // Verify zone type
            if (zoneType == NetworkType.Basic
                    || (zoneType == NetworkType.Advanced && zone.isSecurityGroupEnabled())) {
                throw new InvalidParameterValueException("Can't add vnet range to the physical network in the zone that supports " + zoneType + " network, Security Group enabled: " + zone.isSecurityGroupEnabled());
            }

            String[] tokens = vnetRange.split("-");
            try {
                vnetStart = Integer.parseInt(tokens[0]);
                if (tokens.length == 1) {
                    vnetEnd = vnetStart;
                } else {
                    vnetEnd = Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("Please specify valid integers for the vlan range.");
            }
            
            if ((vnetStart > vnetEnd) || (vnetStart < 0) || (vnetEnd > 4096)) {
                s_logger.warn("Invalid vnet range: start range:" + vnetStart + " end range:" + vnetEnd);
                throw new InvalidParameterValueException("Vnet range should be between 0-4096 and start range should be lesser than or equal to end range");
            }
        }

        BroadcastDomainRange broadcastDomainRange = null;
        if (broadcastDomainRangeStr != null && !broadcastDomainRangeStr.isEmpty()) {
            try {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.valueOf(broadcastDomainRangeStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve broadcastDomainRange '" + broadcastDomainRangeStr + "' to a supported value {Pod or Zone}");
            }

            // in Acton release you can specify only Zone broadcastdomain type in Advance zone, and Pod in Basic
            if (zoneType == NetworkType.Basic && broadcastDomainRange != null && broadcastDomainRange != BroadcastDomainRange.POD) {
                throw new InvalidParameterValueException("Basic zone can have broadcast domain type of value " + BroadcastDomainRange.POD + " only");
            } else if (zoneType == NetworkType.Advanced && broadcastDomainRange != null && broadcastDomainRange != BroadcastDomainRange.ZONE) {
                throw new InvalidParameterValueException("Advance zone can have broadcast domain type of value " + BroadcastDomainRange.ZONE + " only");
            }
        }

        if (broadcastDomainRange == null) {
            if (zoneType == NetworkType.Basic) {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.POD;
            } else {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.ZONE;
            }
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            // Create the new physical network in the database
            long id = _physicalNetworkDao.getNextInSequence(Long.class, "id");
            PhysicalNetworkVO pNetwork = new PhysicalNetworkVO(id, zoneId, vnetRange, networkSpeed, domainId, broadcastDomainRange, name);
            pNetwork.setTags(tags);
            pNetwork.setIsolationMethods(isolationMethods);

            pNetwork = _physicalNetworkDao.persist(pNetwork);

            // Add vnet entries for the new zone if zone type is Advanced
            if (vnetRange != null) {
                _dcDao.addVnet(zone.getId(), pNetwork.getId(), vnetStart, vnetEnd);
            }

            // add VirtualRouter as the default network service provider
            addDefaultVirtualRouterToPhysicalNetwork(pNetwork.getId());

            // add security group provider to the physical network
            addDefaultSecurityGroupProviderToPhysicalNetwork(pNetwork.getId());
            
            // add VPCVirtualRouter as the defualt network service provider
            addDefaultVpcVirtualRouterToPhysicalNetwork(pNetwork.getId());

            txn.commit();
            return pNetwork;
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to create a physical network");
        }
    }

    @Override
    public Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name) {
        Filter searchFilter = new Filter(PhysicalNetworkVO.class, "id", Boolean.TRUE, startIndex, pageSize);
        SearchCriteria<PhysicalNetworkVO> sc = _physicalNetworkDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        Pair<List<PhysicalNetworkVO>, Integer> result =  _physicalNetworkDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends PhysicalNetwork>, Integer>(result.first(), result.second());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_UPDATE, eventDescription = "updating physical network", async = true)
    public PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(id);
        if (network == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Physical Network with specified id doesn't exist in the system");
            ex.addProxyObject(network, id, "physicalNetworkId");            
            throw ex;
        }

        // if zone is of Basic type, don't allow to add vnet range
        DataCenter zone = _dcDao.findById(network.getDataCenterId());
        if (zone == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Zone with id=" + network.getDataCenterId() + " doesn't exist in the system");
            ex.addProxyObject(zone, network.getDataCenterId(), "dataCenterId");            
            throw ex;
        }
        if (newVnetRangeString != null) {
            if (zone.getNetworkType() == NetworkType.Basic
                    || (zone.getNetworkType() == NetworkType.Advanced && zone.isSecurityGroupEnabled())) {
                throw new InvalidParameterValueException("Can't add vnet range to the physical network in the zone that supports " + zone.getNetworkType() + " network, Security Group enabled: "
                        + zone.isSecurityGroupEnabled());
            }
        }

        if (tags != null && tags.size() > 1) {
            throw new InvalidParameterException("Unable to support more than one tag on network yet");
        }

        PhysicalNetwork.State networkState = null;
        if (state != null && !state.isEmpty()) {
            try {
                networkState = PhysicalNetwork.State.valueOf(state);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve state '" + state + "' to a supported value {Enabled or Disabled}");
            }
        }

        if (state != null) {
            network.setState(networkState);
        }

        if (tags != null) {
            network.setTags(tags);
        }

        if (networkSpeed != null) {
            network.setSpeed(networkSpeed);
        }

        // Vnet range can be extended only
        boolean replaceVnet = false;
        ArrayList<Pair<Integer, Integer>> vnetsToAdd = new ArrayList<Pair<Integer, Integer>>(2);

        if (newVnetRangeString != null) {
            Integer newStartVnet = 0;
            Integer newEndVnet = 0;
            String[] newVnetRange = newVnetRangeString.split("-");
            int maxVnet = 4096;
            // for GRE phynets allow up to 32bits
            // TODO: Not happy about this test.  
            // What about guru-like objects for physical networs?
            s_logger.debug("ISOLATION METHODS:" + network.getIsolationMethods());
            // Java does not have unsigned types...
            if (network.getIsolationMethods().contains("GRE")) { 
                maxVnet = (int)(Math.pow(2, 32)-1);
            }
            String rangeMessage = " between 0 and " + maxVnet; 
            if (newVnetRange.length < 2) {
                throw new InvalidParameterValueException("Please provide valid vnet range" + rangeMessage);
            }

            if (newVnetRange[0] == null || newVnetRange[1] == null) {
                throw new InvalidParameterValueException("Please provide valid vnet range" + rangeMessage);
            }

            try {
                newStartVnet = Integer.parseInt(newVnetRange[0]);
                newEndVnet = Integer.parseInt(newVnetRange[1]);
            } catch (NumberFormatException e) {
                s_logger.warn("Unable to parse vnet range:", e);
                throw new InvalidParameterValueException("Please provide valid vnet range" + rangeMessage);
            }
            if (newStartVnet < 0 || newEndVnet > maxVnet) {
                throw new InvalidParameterValueException("Vnet range has to be" + rangeMessage);
            }

            if (newStartVnet > newEndVnet) {
                throw new InvalidParameterValueException("Vnet range has to be" + rangeMessage + " and start range should be lesser than or equal to stop range");
            }
            
            if (physicalNetworkHasAllocatedVnets(network.getDataCenterId(), network.getId())) {
                String[] existingRange = network.getVnet().split("-");
                int existingStartVnet = Integer.parseInt(existingRange[0]);
                int existingEndVnet = Integer.parseInt(existingRange[1]);

                // check if vnet is being extended
                if (newStartVnet.intValue() > existingStartVnet || newEndVnet.intValue() < existingEndVnet) {
                    throw new InvalidParameterValueException("Can't shrink existing vnet range as it the range has vnets allocated. Only extending existing vnet is supported");
                }

                if (newStartVnet < existingStartVnet) {
                    vnetsToAdd.add(new Pair<Integer, Integer>(newStartVnet, existingStartVnet - 1));
                }

                if (newEndVnet > existingEndVnet) {
                    vnetsToAdd.add(new Pair<Integer, Integer>(existingEndVnet + 1, newEndVnet));
                }

            } else {
                vnetsToAdd.add(new Pair<Integer, Integer>(newStartVnet, newEndVnet));
                replaceVnet = true;
            }
        }

        if (newVnetRangeString != null) {
            network.setVnet(newVnetRangeString);
        }

        _physicalNetworkDao.update(id, network);

        if (replaceVnet) {
            s_logger.debug("Deleting existing vnet range for the physicalNetwork id= " + id + " and zone id=" + network.getDataCenterId() + " as a part of updatePhysicalNetwork call");
            _dcDao.deleteVnet(network.getId());
        }

        for (Pair<Integer, Integer> vnetToAdd : vnetsToAdd) {
            s_logger.debug("Adding vnet range " + vnetToAdd.first() + "-" + vnetToAdd.second() + " for the physicalNetwork id= " + id + " and zone id=" + network.getDataCenterId()
                    + " as a part of updatePhysicalNetwork call");
            _dcDao.addVnet(network.getDataCenterId(), network.getId(), vnetToAdd.first(), vnetToAdd.second());
        }

        return network;
    }

    private boolean physicalNetworkHasAllocatedVnets(long zoneId, long physicalNetworkId) {
        return !_dcDao.listAllocatedVnets(physicalNetworkId).isEmpty();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_DELETE, eventDescription = "deleting physical network", async = true)
    @DB
    public boolean deletePhysicalNetwork(Long physicalNetworkId) {

        // verify input parameters
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Physical Network with specified id doesn't exist in the system");
            ex.addProxyObject(pNetwork, physicalNetworkId, "physicalNetworkId");            
            throw ex;
        }

        checkIfPhysicalNetworkIsDeletable(physicalNetworkId);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // delete vlans for this zone
        List<VlanVO> vlans = _vlanDao.listVlansByPhysicalNetworkId(physicalNetworkId);
        for (VlanVO vlan : vlans) {
            _vlanDao.remove(vlan.getId());
        }

        // Delete networks
        List<NetworkVO> networks = _networksDao.listByPhysicalNetwork(physicalNetworkId);
        if (networks != null && !networks.isEmpty()) {
            for (NetworkVO network : networks) {
                _networksDao.remove(network.getId());
            }
        }

        // delete vnets
        _dcDao.deleteVnet(physicalNetworkId);

        // delete service providers
        List<PhysicalNetworkServiceProviderVO> providers = _pNSPDao.listBy(physicalNetworkId);
        
        for(PhysicalNetworkServiceProviderVO provider : providers){
            try {
                deleteNetworkServiceProvider(provider.getId());
            }catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to complete destroy of the physical network provider: " + provider.getProviderName() + ", id: "+ provider.getId(), e);
                return false;
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Unable to complete destroy of the physical network provider: " + provider.getProviderName() + ", id: "+ provider.getId(), e);
                return false;
            }
        }

        // delete traffic types
        _pNTrafficTypeDao.deleteTrafficTypes(physicalNetworkId);

        boolean success = _physicalNetworkDao.remove(physicalNetworkId);
        
        txn.commit();

        return success;
    }

    @DB
    protected void checkIfPhysicalNetworkIsDeletable(Long physicalNetworkId) {
        List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        List<String> vnet = new ArrayList<String>();
        vnet.add(0, "op_dc_vnet_alloc");
        vnet.add(1, "physical_network_id");
        vnet.add(2, "there are allocated vnets for this physical network");
        tablesToCheck.add(vnet);

        List<String> networks = new ArrayList<String>();
        networks.add(0, "networks");
        networks.add(1, "physical_network_id");
        networks.add(2, "there are networks associated to this physical network");
        tablesToCheck.add(networks);

        /*
         * List<String> privateIP = new ArrayList<String>();
         * privateIP.add(0, "op_dc_ip_address_alloc");
         * privateIP.add(1, "data_center_id");
         * privateIP.add(2, "there are private IP addresses allocated for this zone");
         * tablesToCheck.add(privateIP);
         */

        List<String> publicIP = new ArrayList<String>();
        publicIP.add(0, "user_ip_address");
        publicIP.add(1, "physical_network_id");
        publicIP.add(2, "there are public IP addresses allocated for this physical network");
        tablesToCheck.add(publicIP);

        for (List<String> table : tablesToCheck) {
            String tableName = table.get(0);
            String column = table.get(1);
            String errorMsg = table.get(2);

            String dbName = "cloud";

            String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";

            if (tableName.equals("networks")) {
                selectSql += " AND removed is NULL";
            }

            if (tableName.equals("op_dc_vnet_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            if (tableName.equals("user_ip_address")) {
                selectSql += " AND state!='Free'";
            }

            if (tableName.equals("op_dc_ip_address_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, physicalNetworkId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The Physical Network is not deletable because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if physical network is deletable. Please contact Cloud Support.");
            }
        }

    }

    @Override
    public List<? extends Service> listNetworkServices(String providerName) {

        Provider provider = null;
        if (providerName != null) {
            provider = Network.Provider.getProvider(providerName);
            if (provider == null) {
                throw new InvalidParameterValueException("Invalid Network Service Provider=" + providerName);
            }
        }

        if (provider != null) {
            NetworkElement element = _networkModel.getElementImplementingProvider(providerName);
            if (element == null) {
                throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + providerName + "'");
            }
            return new ArrayList<Service>(element.getCapabilities().keySet());
        } else {
            return Service.listAllServices();
        }
    }

    
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_CREATE, eventDescription = "Creating Physical Network ServiceProvider", create = true)
    public PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId, List<String> enabledServices) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Physical Network with specified id doesn't exist in the system");
            ex.addProxyObject(network, physicalNetworkId, "physicalNetworkId");
            throw ex;
        }

        // verify input parameters
        if (destinationPhysicalNetworkId != null) {
            PhysicalNetworkVO destNetwork = _physicalNetworkDao.findById(destinationPhysicalNetworkId);
            if (destNetwork == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Destination Physical Network with specified id doesn't exist in the system");
                ex.addProxyObject(destNetwork, destinationPhysicalNetworkId, "destinationPhysicalNetworkId");                
                throw ex;
            }
        }

        if (providerName != null) {
            Provider provider = Network.Provider.getProvider(providerName);
            if (provider == null) {
                throw new InvalidParameterValueException("Invalid Network Service Provider=" + providerName);
            }
        }

        if (_pNSPDao.findByServiceProvider(physicalNetworkId, providerName) != null) {
            // TBD: send uuid instead of physicalNetworkId.
            throw new CloudRuntimeException("The '" + providerName + "' provider already exists on physical network : " + physicalNetworkId);
        }

        // check if services can be turned off
        NetworkElement element = _networkModel.getElementImplementingProvider(providerName);
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + providerName + "'");
        }
        List<Service> services = new ArrayList<Service>();

        if (enabledServices != null) {
            if (!element.canEnableIndividualServices()) {
                if (enabledServices.size() != element.getCapabilities().keySet().size()) {
                    throw new InvalidParameterValueException("Cannot enable subset of Services, Please specify the complete list of Services for this Service Provider '" + providerName + "'");
                }
            }

            // validate Services
            boolean addGatewayService = false;
            for (String serviceName : enabledServices) {
                Network.Service service = Network.Service.getService(serviceName);
                if (service == null || service == Service.Gateway) {
                    throw new InvalidParameterValueException("Invalid Network Service specified=" + serviceName);
                } else if (service == Service.SourceNat) {
                    addGatewayService = true;
                }

                // check if the service is provided by this Provider
                if (!element.getCapabilities().containsKey(service)) {
                    throw new InvalidParameterValueException(providerName + " Provider cannot provide this Service specified=" + serviceName);
                }
                services.add(service);
            }

            if (addGatewayService) {
                services.add(Service.Gateway);
            }
        } else {
            // enable all the default services supported by this element.
            services = new ArrayList<Service>(element.getCapabilities().keySet());
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            // Create the new physical network in the database
            PhysicalNetworkServiceProviderVO nsp = new PhysicalNetworkServiceProviderVO(physicalNetworkId, providerName);
            // set enabled services
            nsp.setEnabledServices(services);

            if (destinationPhysicalNetworkId != null) {
                nsp.setDestinationPhysicalNetworkId(destinationPhysicalNetworkId);
            }
            nsp = _pNSPDao.persist(nsp);

            txn.commit();
            return nsp;
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to add a provider to physical network");
        }

    }

    @Override
    public Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId,
            String name, String state, Long startIndex, Long pageSize) {

        Filter searchFilter = new Filter(PhysicalNetworkServiceProviderVO.class, "id", false, startIndex, pageSize);
        SearchBuilder<PhysicalNetworkServiceProviderVO> sb = _pNSPDao.createSearchBuilder();
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = sb.create();

        if (physicalNetworkId != null) {
            sc.addAnd("physicalNetworkId", Op.EQ, physicalNetworkId);
        }

        if (name != null) {
            sc.addAnd("providerName", Op.EQ, name);
        }

        if (state != null) {
            sc.addAnd("state", Op.EQ, state);
        }

        Pair<List<PhysicalNetworkServiceProviderVO>, Integer> result =  _pNSPDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends PhysicalNetworkServiceProvider>, Integer>(result.first(), result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_UPDATE, eventDescription = "Updating physical network ServiceProvider", async = true)
    public PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String stateStr, List<String> enabledServices) {

        PhysicalNetworkServiceProviderVO provider = _pNSPDao.findById(id);
        if (provider == null) {
            throw new InvalidParameterValueException("Network Service Provider id=" + id + "doesn't exist in the system");
        }

        NetworkElement element = _networkModel.getElementImplementingProvider(provider.getProviderName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getProviderName() + "'");
        }

        PhysicalNetworkServiceProvider.State state = null;
        if (stateStr != null && !stateStr.isEmpty()) {
            try {
                state = PhysicalNetworkServiceProvider.State.valueOf(stateStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve state '" + stateStr + "' to a supported value {Enabled or Disabled}");
            }
        }

        boolean update = false;

        if (state != null) {
            if (state == PhysicalNetworkServiceProvider.State.Shutdown) {
                throw new InvalidParameterValueException("Updating the provider state to 'Shutdown' is not supported");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("updating state of the service provider id=" + id + " on physical network: " + provider.getPhysicalNetworkId() + " to state: " + stateStr);
            }
            switch (state) {
            case Enabled:
                if (element != null && element.isReady(provider)) {
                    provider.setState(PhysicalNetworkServiceProvider.State.Enabled);
                    update = true;
                } else {
                    throw new CloudRuntimeException("Provider is not ready, cannot Enable the provider, please configure the provider first");
                }
                break;
            case Disabled:
                // do we need to do anything for the provider instances before disabling?
                provider.setState(PhysicalNetworkServiceProvider.State.Disabled);
                update = true;
                break;
            }
        }

        if (enabledServices != null) {
            // check if services can be turned of
            if (!element.canEnableIndividualServices()) {
                throw new InvalidParameterValueException("Cannot update set of Services for this Service Provider '" + provider.getProviderName() + "'");
            }

            // validate Services
            List<Service> services = new ArrayList<Service>();
            for (String serviceName : enabledServices) {
                Network.Service service = Network.Service.getService(serviceName);
                if (service == null) {
                    throw new InvalidParameterValueException("Invalid Network Service specified=" + serviceName);
                }
                services.add(service);
            }
            // set enabled services
            provider.setEnabledServices(services);
            update = true;
        }

        if (update) {
            _pNSPDao.update(id, provider);
        }
        return provider;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_DELETE, eventDescription = "Deleting physical network ServiceProvider", async = true)
    public boolean deleteNetworkServiceProvider(Long id) throws ConcurrentOperationException, ResourceUnavailableException {
        PhysicalNetworkServiceProviderVO provider = _pNSPDao.findById(id);

        if (provider == null) {
            throw new InvalidParameterValueException("Network Service Provider id=" + id + "doesn't exist in the system");
        }

        // check if there are networks using this provider
        List<NetworkVO> networks = _networksDao.listByPhysicalNetworkAndProvider(provider.getPhysicalNetworkId(), provider.getProviderName());
        if (networks != null && !networks.isEmpty()) {
            throw new CloudRuntimeException("Provider is not deletable because there are active networks using this provider, please upgrade these networks to new network offerings");
        }

        User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        Account callerAccount = _accountMgr.getActiveAccountById(callerUser.getAccountId());
        // shutdown the provider instances
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Shutting down the service provider id=" + id + " on physical network: " + provider.getPhysicalNetworkId());
        }
        NetworkElement element = _networkModel.getElementImplementingProvider(provider.getProviderName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getProviderName() + "'");
        }

        if (element != null && element.shutdownProviderInstances(provider, context)) {
            provider.setState(PhysicalNetworkServiceProvider.State.Shutdown);
        }

        return _pNSPDao.remove(id);
    }

    @Override
    public PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId) {
        return _physicalNetworkDao.findById(physicalNetworkId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_CREATE, eventDescription = "Creating Physical Network", async = true)
    public PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId) {
        return getPhysicalNetwork(physicalNetworkId);
    }

    @Override
    public PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId) {
        return _pNSPDao.findById(providerId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_CREATE, eventDescription = "Creating Physical Network ServiceProvider", async = true)
    public PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId) {
        return getPhysicalNetworkServiceProvider(providerId);
    }

    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        List<PhysicalNetworkVO> pNtwks = new ArrayList<PhysicalNetworkVO>();
        if (trafficType != null) {
            pNtwks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, trafficType);
        } else {
            pNtwks = _physicalNetworkDao.listByZone(zoneId);
        }
        
        if (pNtwks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find physical network in zone id=" + zoneId);
        }

        if (pNtwks.size() > 1) {
            if (tag == null) {
                throw new InvalidParameterValueException("More than one physical networks exist in zone id=" + zoneId + " and no tags are specified in order to make a choice");
            }

            Long pNtwkId = null;
            for (PhysicalNetwork pNtwk : pNtwks) {
                if (pNtwk.getTags().contains(tag)) {
                    s_logger.debug("Found physical network id=" + pNtwk.getId() + " based on requested tags " + tag);
                    pNtwkId = pNtwk.getId();
                    break;
                }
            }
            if (pNtwkId == null) {
                throw new InvalidParameterValueException("Unable to find physical network which match the tags " + tag);
            }
            return pNtwkId;
        } else {
            return pNtwks.get(0).getId();
        }
    }

   
   
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_CREATE, eventDescription = "Creating Physical Network TrafficType", create = true)
    public PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficTypeStr, String xenLabel, String kvmLabel, String vmwareLabel, String simulatorLabel, String vlan) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            throw new InvalidParameterValueException("Physical Network id=" + physicalNetworkId + "doesn't exist in the system");
        }

        Networks.TrafficType trafficType = null;
        if (trafficTypeStr != null && !trafficTypeStr.isEmpty()) {
            try {
                trafficType = Networks.TrafficType.valueOf(trafficTypeStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve trafficType '" + trafficTypeStr + "' to a supported value");
            }
        }

        if (_pNTrafficTypeDao.isTrafficTypeSupported(physicalNetworkId, trafficType)) {
            throw new CloudRuntimeException("This physical network already supports the traffic type: " + trafficType);
        }
        // For Storage, Control, Management, Public check if the zone has any other physical network with this
        // traffictype already present
        // If yes, we cant add these traffics to one more physical network in the zone.

        if (TrafficType.isSystemNetwork(trafficType) || TrafficType.Public.equals(trafficType) || TrafficType.Storage.equals(trafficType)) {
            if (!_physicalNetworkDao.listByZoneAndTrafficType(network.getDataCenterId(), trafficType).isEmpty()) {
                throw new CloudRuntimeException("Fail to add the traffic type to physical network because Zone already has a physical network with this traffic type: " + trafficType);
            }
        }

        if (TrafficType.Storage.equals(trafficType)) {
            List<SecondaryStorageVmVO> ssvms = _stnwMgr.getSSVMWithNoStorageNetwork(network.getDataCenterId());
            if (!ssvms.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                        "Cannot add "
                                + trafficType
                                + " traffic type as there are below secondary storage vm still running. Please stop them all and add Storage traffic type again, then destory them all to allow CloudStack recreate them with storage network(If you have added storage network ip range)");
                sb.append("SSVMs:");
                for (SecondaryStorageVmVO ssvm : ssvms) {
                    sb.append(ssvm.getInstanceName()).append(":").append(ssvm.getState());
                }
                throw new CloudRuntimeException(sb.toString());
            }
        }

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            // Create the new traffic type in the database
            if (xenLabel == null) {
                xenLabel = getDefaultXenNetworkLabel(trafficType);
            }
            PhysicalNetworkTrafficTypeVO pNetworktrafficType = new PhysicalNetworkTrafficTypeVO(physicalNetworkId, trafficType, xenLabel, kvmLabel, vmwareLabel, simulatorLabel, vlan);
            pNetworktrafficType = _pNTrafficTypeDao.persist(pNetworktrafficType);

            txn.commit();
            return pNetworktrafficType;
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to add a traffic type to physical network");
        }

    }

    private String getDefaultXenNetworkLabel(TrafficType trafficType) {
        String xenLabel = null;
        switch (trafficType) {
        case Public:
            xenLabel = _configDao.getValue(Config.XenPublicNetwork.key());
            break;
        case Guest:
            xenLabel = _configDao.getValue(Config.XenGuestNetwork.key());
            break;
        case Storage:
            xenLabel = _configDao.getValue(Config.XenStorageNetwork1.key());
            break;
        case Management:
            xenLabel = _configDao.getValue(Config.XenPrivateNetwork.key());
            break;
        case Control:
            xenLabel = "cloud_link_local_network";
            break;
        }
        return xenLabel;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_CREATE, eventDescription = "Creating Physical Network TrafficType", async = true)
    public PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id) {
        return _pNTrafficTypeDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_UPDATE, eventDescription = "Updating physical network TrafficType", async = true)
    public PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel) {

        PhysicalNetworkTrafficTypeVO trafficType = _pNTrafficTypeDao.findById(id);

        if (trafficType == null) {
            throw new InvalidParameterValueException("Traffic Type with id=" + id + "doesn't exist in the system");
        }

        if (xenLabel != null) {
            if("".equals(xenLabel)){
                xenLabel = null;
            }
            trafficType.setXenNetworkLabel(xenLabel);
        }
        if (kvmLabel != null) {
            if("".equals(kvmLabel)){
                kvmLabel = null;
            }
            trafficType.setKvmNetworkLabel(kvmLabel);
        }
        if (vmwareLabel != null) {
            if("".equals(vmwareLabel)){
                vmwareLabel = null;
            }
            trafficType.setVmwareNetworkLabel(vmwareLabel);
        }
        _pNTrafficTypeDao.update(id, trafficType);

        return trafficType;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_DELETE, eventDescription = "Deleting physical network TrafficType", async = true)
    public boolean deletePhysicalNetworkTrafficType(Long id) {
        PhysicalNetworkTrafficTypeVO trafficType = _pNTrafficTypeDao.findById(id);

        if (trafficType == null) {
            throw new InvalidParameterValueException("Traffic Type with id=" + id + "doesn't exist in the system");
        }

        // check if there are any networks associated to this physical network with this traffic type
        if (TrafficType.Guest.equals(trafficType.getTrafficType())) {
            if (!_networksDao.listByPhysicalNetworkTrafficType(trafficType.getPhysicalNetworkId(), trafficType.getTrafficType()).isEmpty()) {
                throw new CloudRuntimeException("The Traffic Type is not deletable because there are existing networks with this traffic type:" + trafficType.getTrafficType());
            }
        } else if (TrafficType.Storage.equals(trafficType.getTrafficType())) {
            PhysicalNetworkVO pn = _physicalNetworkDao.findById(trafficType.getPhysicalNetworkId());
            if (_stnwMgr.isAnyStorageIpInUseInZone(pn.getDataCenterId())) {
                throw new CloudRuntimeException("The Traffic Type is not deletable because there are still some storage network ip addresses in use:" + trafficType.getTrafficType());
            }
        }
        return _pNTrafficTypeDao.remove(id);
    }

    @Override
    public Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId) {
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Physical Network with specified id doesn't exist in the system");
            ex.addProxyObject(network, physicalNetworkId, "physicalNetworkId");
            throw ex;
        }

        Pair<List<PhysicalNetworkTrafficTypeVO>, Integer> result = _pNTrafficTypeDao.listAndCountBy(physicalNetworkId);
        return new Pair<List<? extends PhysicalNetworkTrafficType>, Integer>(result.first(), result.second());
    }

   



    @Override //TODO: duplicated in NetworkModel
    public NetworkVO getExclusiveGuestNetwork(long zoneId) {
        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, zoneId, GuestType.Shared, TrafficType.Guest);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared + " in zone " + zoneId);
        }

        if (networks.size() > 1) {
            throw new InvalidParameterValueException("Found more than 1 network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared + " in zone " + zoneId);

        }

        return networks.get(0);
    }

    protected PhysicalNetworkServiceProvider addDefaultVirtualRouterToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.VirtualRouter.getName(), null, null);
        // add instance of the provider
        NetworkElement networkElement = _networkModel.getElementImplementingProvider(Network.Provider.VirtualRouter.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the VirtualRouter Provider");
        }
        
        VirtualRouterElement element = (VirtualRouterElement)networkElement;
        element.addElement(nsp.getId(), VirtualRouterProviderType.VirtualRouter);

        return nsp;
    }
    
    protected PhysicalNetworkServiceProvider addDefaultVpcVirtualRouterToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, 
                Network.Provider.VPCVirtualRouter.getName(), null, null);
 
        NetworkElement networkElement =  _networkModel.getElementImplementingProvider(Network.Provider.VPCVirtualRouter.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the VPCVirtualRouter Provider");
        }
        
        VpcVirtualRouterElement element = (VpcVirtualRouterElement)networkElement;
        element.addElement(nsp.getId(), VirtualRouterProviderType.VPCVirtualRouter);

        return nsp;
    }

    protected PhysicalNetworkServiceProvider addDefaultSecurityGroupProviderToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, 
                Network.Provider.SecurityGroupProvider.getName(), null, null);

        return nsp;
    }

    
    protected boolean isNetworkSystem(Network network) {
        NetworkOffering no = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (no.isSystemOnly()) {
            return true;
        } else {
            return false;
        }
    }

    
    
    private boolean getAllowSubdomainAccessGlobal() {
        return _allowSubdomainNetworkAccess;
    }

  

    @Override
    public List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd) {
        String type = cmd.getTrafficType();
        List<Pair<TrafficType, String>> results = new ArrayList<Pair<TrafficType, String>>();
        if (type != null) {
            for (NetworkGuru guru : _networkGurus) {
                if (guru.isMyTrafficType(TrafficType.getTrafficType(type))) {
                    results.add(new Pair<TrafficType, String>(TrafficType.getTrafficType(type), guru.getName()));
                    break;
                }
            }
        } else {
            for (NetworkGuru guru : _networkGurus) {
                TrafficType[] allTypes = guru.getSupportedTrafficType();
                for (TrafficType t : allTypes) {
                    results.add(new Pair<TrafficType, String>(t, guru.getName()));
                }
            }
        }

        return results;
    }

    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "associating Ip", async = true)
    public IpAddress associateIPToNetwork(long ipId, long networkId) throws InsufficientAddressCapacityException,
    ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException {

        Network network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        if (network.getVpcId() != null) {
            throw new InvalidParameterValueException("Can't assign ip to the network directly when network belongs" +
                    " to VPC.Specify vpcId to associate ip address to VPC");
        }
        return _networkMgr.associateIPToGuestNetwork(ipId, networkId, true);
        
    }
    

    @Override @DB
    public Network createPrivateNetwork(String networkName, String displayText, long physicalNetworkId, 
            String vlan, String startIp, String endIp, String gateway, String netmask, long networkOwnerId, Long vpcId) 
                    throws ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {
        
        Account owner = _accountMgr.getAccount(networkOwnerId);
        
        // Get system network offeirng
        NetworkOfferingVO ntwkOff = findSystemNetworkOffering(NetworkOffering.SystemPrivateGatewayNetworkOffering);
        
        // Validate physical network
        PhysicalNetwork pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNtwk == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a physical network" +
                    " having the given id");
            ex.addProxyObject("physical_network", physicalNetworkId, "physicalNetworkId");
            throw ex;
        }
        
        // VALIDATE IP INFO
        // if end ip is not specified, default it to startIp
        if (!NetUtils.isValidIp(startIp)) {
            throw new InvalidParameterValueException("Invalid format for the startIp parameter");
        }
        if (endIp == null) {
            endIp = startIp;
        } else if (!NetUtils.isValidIp(endIp)) {
            throw new InvalidParameterValueException("Invalid format for the endIp parameter");
        }

        String cidr = null;
        if (!NetUtils.isValidIp(gateway)) {
            throw new InvalidParameterValueException("Invalid gateway");
        }
        if (!NetUtils.isValidNetmask(netmask)) {
            throw new InvalidParameterValueException("Invalid netmask");
        }

        cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        //lock datacenter as we need to get mac address seq from there
        DataCenterVO dc = _dcDao.lockRow(pNtwk.getDataCenterId(), true);
        
        //check if we need to create guest network
        Network privateNetwork = _networksDao.getPrivateNetwork(BroadcastDomainType.Vlan.toUri(vlan).toString(), cidr,
                networkOwnerId, pNtwk.getDataCenterId());
        if (privateNetwork == null) {
            //create Guest network
            privateNetwork = _networkMgr.createGuestNetwork(ntwkOff.getId(), networkName, displayText, gateway, cidr, vlan, 
                    null, owner, null, pNtwk, pNtwk.getDataCenterId(), ACLType.Account, null, null, null, null);
            s_logger.debug("Created private network " + privateNetwork);
        } else {
            s_logger.debug("Private network already exists: " + privateNetwork);
        }
        
        //add entry to private_ip_address table
        PrivateIpVO privateIp = _privateIpDao.findByIpAndSourceNetworkId(privateNetwork.getId(), startIp);
        if (privateIp != null) {
            throw new InvalidParameterValueException("Private ip address " + startIp + " already used for private gateway" +
                    " in zone " + _configMgr.getZone(pNtwk.getDataCenterId()).getName());
        }
        
        Long mac = dc.getMacAddress();
        Long nextMac = mac + 1;
        dc.setMacAddress(nextMac);

        privateIp = new PrivateIpVO(startIp, privateNetwork.getId(), nextMac, vpcId);
        _privateIpDao.persist(privateIp);
        
        _dcDao.update(dc.getId(), dc);
        
        txn.commit();
        s_logger.debug("Private network " + privateNetwork + " is created");

        return privateNetwork;
    }

    
    private NetworkOfferingVO findSystemNetworkOffering(String offeringName) {
        List<NetworkOfferingVO> allOfferings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offer: allOfferings){
            if (offer.getName().equals(offeringName)) {
                return offer;
            }
        }
        return null;
    }

    @Override
    public Network getNetwork(String networkUuid) {
       return _networksDao.findByUuid(networkUuid);
    }

    @Override
    public List<? extends Nic> listNics(ListNicsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long nicId = cmd.getNicId();
        Long vmId = cmd.getVmId();

        UserVmVO  userVm = _userVmDao.findById(vmId);

        if (userVm == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Virtual mahine id does not exist");
                ex.addProxyObject(userVm, vmId, "vmId");
                throw ex;
            }
        _accountMgr.checkAccess(caller, null, true, userVm);
        return _networkMgr.listVmNics(vmId, nicId);
    }
}
