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

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.cloudstack.network.NetworkPermissionVO;
import org.apache.cloudstack.network.dao.NetworkPermissionDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkAccountDao;
import com.cloud.network.dao.NetworkAccountVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.IpDeployingRequester;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.VMInstanceDao;

public class NetworkModelImpl extends ManagerBase implements NetworkModel, Configurable {
    static final Logger s_logger = Logger.getLogger(NetworkModelImpl.class);
    public static final String UNABLE_TO_USE_NETWORK = "Unable to use network with id= %s, permission denied";
    @Inject
    EntityManager _entityMgr;
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
    AccountManager _accountMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    NetworkDao _networksDao = null;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    NicDao _nicDao = null;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    ProjectDao projectDao;
    @Inject
    NetworkPermissionDao _networkPermissionDao;

    private List<NetworkElement> networkElements;

    public List<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    public void setNetworkElements(List<NetworkElement> networkElements) {
        this.networkElements = networkElements;
    }

    @Inject
    NetworkAccountDao _networkAccountDao;
    @Inject
    NetworkDomainDao _networkDomainDao;
    @Inject
    VMInstanceDao _vmDao;

    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    DomainManager _domainMgr;

    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _pNTrafficTypeDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    ApplicationLoadBalancerRuleDao _appLbRuleDao;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    NetworkOfferingDetailsDao _ntwkOffDetailsDao;
    @Inject
    private NetworkService _networkService;
    @Inject
    TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;

    private final HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);

    SearchBuilder<IPAddressVO> IpAddressSearch;
    SearchBuilder<NicVO> NicForTrafficTypeSearch;

    private boolean _allowSubdomainNetworkAccess;

    private Map<String, String> _configs;

    protected boolean _executeInSequenceNtwkElmtCmd;

    HashMap<Long, Long> _lastNetworkIdsToFree = new HashMap<Long, Long>();

    static HashMap<Service, List<Provider>> s_serviceToImplementedProvidersMap = new HashMap<Service, List<Provider>>();
    static HashMap<String, String> s_providerToNetworkElementMap = new HashMap<String, String>();

    /**
     *
     */
    public NetworkModelImpl() {
        super();
    }

    @Override
    public NetworkElement getElementImplementingProvider(String providerName) {
        String elementName = s_providerToNetworkElementMap.get(providerName);
        NetworkElement element = AdapterBase.getAdapterByName(networkElements, elementName);
        return element;
    }

    @Override
    public List<Service> getElementServices(Provider provider) {
        NetworkElement element = getElementImplementingProvider(provider.getName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getName() + "'");
        }
        return new ArrayList<Service>(element.getCapabilities().keySet());
    }

    @Override
    public boolean canElementEnableIndividualServices(Provider provider) {
        NetworkElement element = getElementImplementingProvider(provider.getName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getName() + "'");
        }
        return element.canEnableIndividualServices();
    }

    Set<Purpose> getPublicIpPurposeInRules(PublicIpAddress ip, boolean includeRevoked, boolean includingFirewall) {
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
    public Map<PublicIpAddress, Set<Service>> getIpToServices(List<? extends PublicIpAddress> publicIps, boolean postApplyRules, boolean includingFirewall) {
        Map<PublicIpAddress, Set<Service>> ipToServices = new HashMap<PublicIpAddress, Set<Service>>();

        if (publicIps != null && !publicIps.isEmpty()) {
            Set<Long> networkSNAT = new HashSet<Long>();
            for (PublicIpAddress ip : publicIps) {
                Set<Service> services = ipToServices.get(ip);
                if (services == null) {
                    services = new HashSet<Service>();
                }
                if (ip.isSourceNat()) {
                    if (!networkSNAT.contains(ip.getAssociatedWithNetworkId())) {
                        services.add(Service.SourceNat);
                        networkSNAT.add(ip.getAssociatedWithNetworkId());
                    } else {
                        CloudRuntimeException ex = new CloudRuntimeException("Multiple generic source NAT IPs provided for network");
                        // see the IPAddressVO.java class.
                        IPAddressVO ipAddr = ApiDBUtils.findIpAddressById(ip.getAssociatedWithNetworkId());
                        String ipAddrUuid = ip.getAssociatedWithNetworkId().toString();
                        if (ipAddr != null) {
                            ipAddrUuid = ipAddr.getUuid();
                        }
                        ex.addProxyObject(ipAddrUuid, "networkId");
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
                        if (postApplyRules) {
                            // no active rules/revoked rules are associated with this public IP, so remove the
                            // association with the provider
                            if (ip.isSourceNat()) {
                                s_logger.debug("Not releasing ip " + ip.getAddress().addr() + " as it is in use for SourceNat");
                            } else {
                                ip.setState(State.Releasing);
                            }
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

    public boolean canIpUsedForNonConserveService(PublicIp ip, Service service) {
        // If it's non-conserve mode, then the new ip should not be used by any other services
        List<PublicIpAddress> ipList = new ArrayList<PublicIpAddress>();
        ipList.add(ip);
        Map<PublicIpAddress, Set<Service>> ipToServices = getIpToServices(ipList, false, false);
        Set<Service> services = ipToServices.get(ip);
        // Not used currently, safe
        if (services == null || services.isEmpty()) {
            return true;
        }
        // Since it's non-conserve mode, only one service should used for IP
        if (services.size() != 1) {
            throw new InvalidParameterException("There are multiple services used ip " + ip.getAddress() + ".");
        }
        if (service != null && !((Service)services.toArray()[0] == service || service.equals(Service.Firewall))) {
            throw new InvalidParameterException("The IP " + ip.getAddress() + " is already used as " + ((Service)services.toArray()[0]).getName() + " rather than " +
                service.getName());
        }
        return true;
    }

    Map<Service, Set<Provider>> getServiceProvidersMap(long networkId) {
        Map<Service, Set<Provider>> map = new HashMap<Service, Set<Provider>>();
        List<NetworkServiceMapVO> nsms = _ntwkSrvcDao.getServicesInNetwork(networkId);
        for (NetworkServiceMapVO nsm : nsms) {
            Set<Provider> providers = map.get(Service.getService(nsm.getService()));
            if (providers == null) {
                providers = new HashSet<Provider>();
            }
            providers.add(Provider.getProvider(nsm.getProvider()));
            map.put(Service.getService(nsm.getService()), providers);
        }
        return map;
    }

    public boolean canIpUsedForService(PublicIp publicIp, Service service, Long networkId) {
        List<PublicIpAddress> ipList = new ArrayList<PublicIpAddress>();
        ipList.add(publicIp);
        Map<PublicIpAddress, Set<Service>> ipToServices = getIpToServices(ipList, false, true);
        Set<Service> services = ipToServices.get(publicIp);
        if (services == null || services.isEmpty()) {
            return true;
        }

        if (networkId == null) {
            networkId = publicIp.getAssociatedWithNetworkId();
        }

        // We only support one provider for one service now
        Map<Service, Set<Provider>> serviceToProviders = getServiceProvidersMap(networkId);
        // Since IP already has service to bind with, the oldProvider can't be null
        Set<Provider> newProviders = serviceToProviders.get(service);
        if (newProviders == null || newProviders.isEmpty()) {
            throw new InvalidParameterException("There is no new provider for IP " + publicIp.getAddress() + " of service " + service.getName() + "!");
        }
        Provider newProvider = (Provider)newProviders.toArray()[0];
        Set<Provider> oldProviders = serviceToProviders.get(services.toArray()[0]);
        Provider oldProvider = (Provider)oldProviders.toArray()[0];
        Network network = _networksDao.findById(networkId);
        NetworkElement oldElement = getElementImplementingProvider(oldProvider.getName());
        NetworkElement newElement = getElementImplementingProvider(newProvider.getName());
        if (oldElement instanceof IpDeployingRequester && newElement instanceof IpDeployingRequester) {
            IpDeployer oldIpDeployer = ((IpDeployingRequester)oldElement).getIpDeployer(network);
            IpDeployer newIpDeployer = ((IpDeployingRequester)newElement).getIpDeployer(network);
            // FIXME: I ignored this check
        } else {
            throw new InvalidParameterException("Ip cannot be applied for new provider!");
        }
        return true;
    }

    Map<Provider, Set<Service>> getProviderServicesMap(long networkId) {
        Map<Provider, Set<Service>> map = new HashMap<Provider, Set<Service>>();
        List<NetworkServiceMapVO> nsms = _ntwkSrvcDao.getServicesInNetwork(networkId);
        for (NetworkServiceMapVO nsm : nsms) {
            Set<Service> services = map.get(Provider.getProvider(nsm.getProvider()));
            if (services == null) {
                services = new HashSet<Service>();
            }
            services.add(Service.getService(nsm.getService()));
            map.put(Provider.getProvider(nsm.getProvider()), services);
        }
        return map;
    }

    @Override
    public Map<Provider, ArrayList<PublicIpAddress>> getProviderToIpList(Network network, Map<PublicIpAddress, Set<Service>> ipToServices) {
        NetworkOffering offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (!offering.isConserveMode()) {
            for (PublicIpAddress ip : ipToServices.keySet()) {
                Set<Service> services = new HashSet<Service>();
                services.addAll(ipToServices.get(ip));
                if (services != null && services.contains(Service.Firewall)) {
                    services.remove(Service.Firewall);
                }
                if (services != null && services.size() > 1) {
                    throw new CloudRuntimeException("Ip " + ip.getAddress() + " is used by multiple services!");
                }
            }
        }
        Map<Service, Set<PublicIpAddress>> serviceToIps = new HashMap<Service, Set<PublicIpAddress>>();
        for (PublicIpAddress ip : ipToServices.keySet()) {
            for (Service service : ipToServices.get(ip)) {
                Set<PublicIpAddress> ips = serviceToIps.get(service);
                if (ips == null) {
                    ips = new HashSet<PublicIpAddress>();
                }
                ips.add(ip);
                serviceToIps.put(service, ips);
            }
        }
        // TODO Check different provider for same IP
        Map<Provider, Set<Service>> providerToServices = getProviderServicesMap(network.getId());
        Map<Provider, ArrayList<PublicIpAddress>> providerToIpList = new HashMap<Provider, ArrayList<PublicIpAddress>>();
        for (Provider provider : providerToServices.keySet()) {
            if (!(getElementImplementingProvider(provider.getName()) instanceof IpDeployingRequester)) {
                continue;
            }
            Set<Service> services = providerToServices.get(provider);
            ArrayList<PublicIpAddress> ipList = new ArrayList<PublicIpAddress>();
            Set<PublicIpAddress> ipSet = new HashSet<PublicIpAddress>();
            for (Service service : services) {
                Set<PublicIpAddress> serviceIps = serviceToIps.get(service);
                if (serviceIps == null || serviceIps.isEmpty()) {
                    continue;
                }
                ipSet.addAll(serviceIps);
            }
            Set<PublicIpAddress> sourceNatIps = serviceToIps.get(Service.SourceNat);
            if (sourceNatIps != null && !sourceNatIps.isEmpty()) {
                ipList.addAll(0, sourceNatIps);
                ipSet.removeAll(sourceNatIps);
            }
            ipList.addAll(ipSet);
            providerToIpList.put(provider, ipList);
        }
        return providerToIpList;
    }

    @Override
    public List<IPAddressVO> listPublicIpsAssignedToGuestNtwk(long accountId, long associatedNetworkId, Boolean sourceNat) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("associatedWithNetworkId", associatedNetworkId);
        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public List<IPAddressVO> listPublicIpsAssignedToGuestNtwk(long associatedNetworkId, Boolean sourceNat) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("associatedWithNetworkId", associatedNetworkId);

        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public List<IPAddressVO> listPublicIpsAssignedToAccount(long accountId, long dcId, Boolean sourceNat) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("dataCenterId", dcId);

        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public List<? extends Nic> getNics(long vmId) {
        return _nicDao.listByVmId(vmId);
    }

    @Override
    public String getNextAvailableMacAddressInNetwork(long networkId) throws InsufficientAddressCapacityException {
        NetworkVO network = _networksDao.findById(networkId);
        String mac = _networksDao.getNextAvailableMacAddress(networkId, MACIdentifier.value());
        if (mac == null) {
            throw new InsufficientAddressCapacityException("Unable to create another mac address", Network.class, networkId);
        }
        return mac;
    }

    @Override
    @DB
    public Network getNetwork(long id) {
        return _networksDao.findById(id);
    }

    @Override
    public boolean canUseForDeploy(Network network) {
        if (network.getTrafficType() != TrafficType.Guest) {
            return false;
        }
        if (network.getGuestType() == GuestType.L2 || listNetworkOfferingServices(network.getNetworkOfferingId()).isEmpty()) {
            return true; // do not check free IPs if there is no service in the network
        }
        boolean hasFreeIps = true;
        if (network.getGuestType() == GuestType.Shared) {
            if (network.getGateway() != null) {
                hasFreeIps = _ipAddressDao.countFreeIPsInNetwork(network.getId()) > 0;
            }
            if (!hasFreeIps) {
                return false;
            }
        } else {
            if (network.getCidr() == null) {
                s_logger.debug("Network - " + network.getId() +  " has NULL CIDR.");
                return false;
            }
            hasFreeIps = (getAvailableIps(network, null)).size() > 0;
        }

        return hasFreeIps;
    }

    @Override
    public boolean areThereIPv6AddressAvailableInNetwork(long networkId) {
        Network network = _networksDao.findById(networkId);
        if (network == null) {
            return false;
        }
        if (network.getIp6Gateway() == null) {
            return false;
        }
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
        for (Vlan vlan : vlans) {
            if (isIP6AddressAvailableInVlan(vlan.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isIP6AddressAvailableInVlan(long vlanId) {
        VlanVO vlan = _vlanDao.findById(vlanId);
        if (vlan.getIp6Range() == null) {
            return false;
        }
        long existedCount = _ipv6Dao.countExistedIpsInVlan(vlanId);
        BigInteger existedInt = BigInteger.valueOf(existedCount);
        BigInteger rangeInt = NetUtils.countIp6InRange(vlan.getIp6Range());
        return (existedInt.compareTo(rangeInt) < 0);
    }

    @Override
    public Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId) {

        Map<Service, Map<Capability, String>> networkCapabilities = new HashMap<Service, Map<Capability, String>>();

        // list all services of this networkOffering
        List<NetworkServiceMapVO> servicesMap = _ntwkSrvcDao.getServicesInNetwork(networkId);
        for (NetworkServiceMapVO instance : servicesMap) {
            Service service = Service.getService(instance.getService());
            NetworkElement element = getElementImplementingProvider(instance.getProvider());
            if (element != null) {
                Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
                if (elementCapabilities != null) {
                    networkCapabilities.put(service, elementCapabilities.get(service));
                }
            }
        }

        return networkCapabilities;
    }

    @Override
    public Map<Capability, String> getNetworkServiceCapabilities(long networkId, Service service) {

        if (!areServicesSupportedInNetwork(networkId, service)) {
            // TBD: networkId to uuid. No VO object being passed. So we will need to call
            // addProxyObject with hardcoded tablename. Or we should probably look up the correct dao proxy object.
            throw new UnsupportedServiceException("Service " + service.getName() + " is not supported in the network id=" + networkId);
        }

        Map<Capability, String> serviceCapabilities = new HashMap<Capability, String>();

        // get the Provider for this Service for this offering
        String provider = _ntwkSrvcDao.getProviderForServiceInNetwork(networkId, service);

        NetworkElement element = getElementImplementingProvider(provider);
        if (element != null) {
            Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
            ;

            if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() +
                    " implementing Provider=" + provider);
            }
            serviceCapabilities = elementCapabilities.get(service);
        }

        return serviceCapabilities;
    }

    @Override
    public Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {

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
        NetworkElement element = getElementImplementingProvider(provider);
        if (element != null) {
            Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
            ;

            if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                // TBD: We should be sending providerId and not the offering object itself.
                throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() +
                    " implementing Provider=" + provider);
            }
            serviceCapabilities = elementCapabilities.get(service);
        }

        return serviceCapabilities;
    }

    @Override
    public NetworkVO getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering : offerings) {
            if (offering.getTrafficType() == trafficType) {
                networkOfferingId = offering.getId();
                break;
            }
        }

        if (networkOfferingId == null) {
            throw new InvalidParameterValueException("Unable to find system network offering with traffic type " + trafficType);
        }

        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, networkOfferingId, zoneId);
        if (networks == null || networks.isEmpty()) {
            // TBD: send uuid instead of zoneId. Hardcode tablename in call to addProxyObject().
            throw new InvalidParameterValueException("Unable to find network with traffic type " + trafficType + " in zone " + zoneId);
        }
        return networks.get(0);
    }

    @Override
    public NetworkVO getNetworkWithSGWithFreeIPs(Long zoneId) {
        List<NetworkVO> networks = _networksDao.listByZoneSecurityGroup(zoneId);
        if (networks == null || networks.isEmpty()) {
            return null;
        }
        NetworkVO ret_network = null;
        for (NetworkVO nw : networks) {
            List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(nw.getId());
            for (VlanVO vlan : vlans) {
                if (_ipAddressDao.countFreeIpsInVlan(vlan.getId()) > 0) {
                    ret_network = nw;
                    break;
                }
            }
            if (ret_network != null) {
                break;
            }
        }
        if (ret_network == null) {
            s_logger.debug("Can not find network with security group enabled with free IPs");
        }
        return ret_network;
    }

    @Override
    public NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId) {
        List<NetworkVO> networks = _networksDao.listByZoneSecurityGroup(zoneId);
        if (networks == null || networks.isEmpty()) {
            return null;
        }

        if (networks.size() > 1) {
            s_logger.debug("There are multiple network with security group enabled? select one of them...");
        }
        return networks.get(0);
    }

    @Override
    public PublicIpAddress getPublicIpAddress(long ipAddressId) {
        IPAddressVO addr = _ipAddressDao.findById(ipAddressId);
        if (addr == null) {
            return null;
        }

        return PublicIp.createFromAddrAndVlan(addr, _vlanDao.findById(addr.getVlanId()));
    }

    @Override
    public List<VlanVO> listPodVlans(long podId) {
        List<VlanVO> vlans = _vlanDao.listVlansForPodByType(podId, VlanType.DirectAttached);
        return vlans;
    }

    @Override
    public List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem) {
        List<NetworkVO> networks = new ArrayList<NetworkVO>();

        List<NicVO> nics = _nicDao.listByVmId(vmId);
        if (nics != null) {
            for (Nic nic : nics) {
                NetworkVO network = _networksDao.findByIdIncludingRemoved(nic.getNetworkId());

                if (isNetworkSystem(network) == isSystem) {
                    networks.add(network);
                }
            }
        }

        return networks;
    }

    @Override
    public Nic getNicInNetwork(long vmId, long networkId) {
        return _nicDao.findByNtwkIdAndInstanceId(networkId, vmId);
    }

    @Override
    public String getIpInNetwork(long vmId, long networkId) {
        Nic guestNic = getNicInNetwork(vmId, networkId);
        assert (guestNic != null && guestNic.getIPv4Address() != null) : "Vm doesn't belong to network associated with " + "ipAddress or ip4 address is null";
        return guestNic.getIPv4Address();
    }

    @Override
    public String getIpInNetworkIncludingRemoved(long vmId, long networkId) {
        Nic guestNic = getNicInNetworkIncludingRemoved(vmId, networkId);
        assert (guestNic != null && guestNic.getIPv4Address() != null) : "Vm doesn't belong to network associated with " + "ipAddress or ip4 address is null";
        return guestNic.getIPv4Address();
    }

    @Override
    public List<NicVO> getNicsForTraffic(long vmId, TrafficType type) {
        SearchCriteria<NicVO> sc = NicForTrafficTypeSearch.create();
        sc.setParameters("instance", vmId);
        sc.setJoinParameters("network", "traffictype", type);

        return _nicDao.search(sc, null);
    }

    @Override
    public IpAddress getIp(long ipAddressId) {
        return _ipAddressDao.findById(ipAddressId);
    }

    @Override
    public Network getDefaultNetworkForVm(long vmId) {
        Nic defaultNic = getDefaultNic(vmId);
        if (defaultNic == null) {
            return null;
        } else {
            return _networksDao.findById(defaultNic.getNetworkId());
        }
    }

    @Override
    public Nic getDefaultNic(long vmId) {
        List<NicVO> nics = _nicDao.listByVmId(vmId);
        Nic defaultNic = null;
        if (nics != null) {
            for (Nic nic : nics) {
                if (nic.isDefaultNic()) {
                    defaultNic = nic;
                    break;
                }
            }
        } else {
            s_logger.debug("Unable to find default network for the vm; vm doesn't have any nics");
            return null;
        }

        if (defaultNic == null) {
            s_logger.debug("Unable to find default network for the vm; vm doesn't have default nic");
        }

        return defaultNic;

    }

    @Override
    public UserDataServiceProvider getUserDataUpdateProvider(Network network) {
        String userDataProvider = _ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Service.UserData);

        if (userDataProvider == null) {
            s_logger.debug("Network " + network + " doesn't support service " + Service.UserData.getName());
            return null;
        }

        return (UserDataServiceProvider)getElementImplementingProvider(userDataProvider);
    }

    @Override
    public  boolean isSharedNetworkWithoutServices (long networkId) {

        Network network = _networksDao.findById(networkId);

        if (network != null && network.getGuestType() != GuestType.Shared) {
            return false;
        }

        List<Service> services = listNetworkOfferingServices(network.getNetworkOfferingId());

        if (services == null || services.isEmpty()) {
            return true;
        }

        return false;
    }


    @Override
    public boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        return (_ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(networkOfferingId, services));
    }

    @Override
    public boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        return (_ntwkSrvcDao.areServicesSupportedInNetwork(networkId, services));
    }

    @Override
    public String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId) {

        List<NetworkVO> virtualNetworks = _networksDao.listByZoneAndGuestType(accountId, dataCenterId, GuestType.Isolated, false);

        if (virtualNetworks.isEmpty()) {
            s_logger.trace("Unable to find default Virtual network account id=" + accountId);
            return null;
        }

        NetworkVO virtualNetwork = virtualNetworks.get(0);

        NicVO networkElementNic = _nicDao.findByNetworkIdAndType(virtualNetwork.getId(), Type.DomainRouter);

        if (networkElementNic != null) {
            return networkElementNic.getIPv4Address();
        } else {
            s_logger.warn("Unable to set find network element for the network id=" + virtualNetwork.getId());
            return null;
        }
    }

    @Override
    public List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, GuestType type) {
        List<NetworkVO> accountNetworks = new ArrayList<NetworkVO>();
        List<NetworkVO> zoneNetworks = _networksDao.listByZone(zoneId);

        for (NetworkVO network : zoneNetworks) {
            if (!isNetworkSystem(network)) {
                if (network.getGuestType() == GuestType.Shared || !_networksDao.listBy(accountId, network.getId()).isEmpty()) {
                    if (type == null || type == network.getGuestType()) {
                        accountNetworks.add(network);
                    }
                }
            }
        }
        return accountNetworks;
    }

    @Override
    public List<NetworkVO> listAllNetworksInAllZonesByType(GuestType type) {
        List<NetworkVO> networks = new ArrayList<NetworkVO>();
        for (NetworkVO network : _networksDao.listAll()) {
            if (!isNetworkSystem(network)) {
                networks.add(network);
            }
        }
        return networks;
    }

    @Override
    public Long getDedicatedNetworkDomain(long networkId) {
        NetworkDomainVO networkMaps = _networkDomainDao.getDomainNetworkMapByNetworkId(networkId);
        if (networkMaps != null) {
            return networkMaps.getDomainId();
        } else {
            return null;
        }
    }

    @Override
    public Integer getNetworkRate(long networkId, Long vmId) {
        VMInstanceVO vm = null;
        if (vmId != null) {
            vm = _vmDao.findById(vmId);
        }
        final Network network = getNetwork(networkId);
        final NetworkOffering ntwkOff = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());

        // For user VM: For default nic use network rate from the service/compute offering,
        //              or on NULL from vm.network.throttling.rate global setting
        // For router: Get network rate for guest and public networks from the guest network offering
        //              or on NULL from network.throttling.rate
        // For others: Use network rate from their network offering,
        //              or on NULL from network.throttling.rate setting at zone > global level
        // http://docs.cloudstack.apache.org/en/latest/adminguide/service_offerings.html#network-throttling
        if (vm != null) {
            switch (vm.getType()) {
                case User:
                    final Nic nic = _nicDao.findByNtwkIdAndInstanceId(networkId, vmId);
                    if (nic != null && nic.isDefaultNic()) {
                        return _configMgr.getServiceOfferingNetworkRate(vm.getServiceOfferingId(), network.getDataCenterId());
                    }
                    break;
                case DomainRouter:
                    if (TrafficType.Guest.equals(network.getTrafficType())) {
                        final Nic routerNic = _nicDao.findByNtwkIdAndInstanceId(networkId, vmId);
                        if (routerNic != null) {
                            return _configMgr.getNetworkOfferingNetworkRate(network.getNetworkOfferingId(), network.getDataCenterId());
                        }
                    } else if (TrafficType.Public.equals(network.getTrafficType())) {
                        List<NicVO> routerNics = _nicDao.listByVmId(vmId);
                        for (final Nic routerNic : routerNics) {
                            final NetworkVO nw = _networksDao.findById(routerNic.getNetworkId());
                            if (TrafficType.Guest.equals(nw.getTrafficType())) {
                                return _configMgr.getNetworkOfferingNetworkRate(nw.getNetworkOfferingId(), network.getDataCenterId());
                            }
                        }
                    }
                    break;
                case ConsoleProxy:
                case SecondaryStorageVm:
                    return -1;
            }
        }
        if (ntwkOff != null) {
            return _configMgr.getNetworkOfferingNetworkRate(ntwkOff.getId(), network.getDataCenterId());
        }
        final Integer networkRate = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(network.getDataCenterId());
        if (networkRate != null && networkRate > 0) {
            return networkRate;
        }
        return -1;
    }

    @Override
    public String getAccountNetworkDomain(long accountId, long zoneId) {
        String networkDomain = _accountDao.findById(accountId).getNetworkDomain();

        if (networkDomain == null) {
            // get domain level network domain
            return getDomainNetworkDomain(_accountDao.findById(accountId).getDomainId(), zoneId);
        }

        return networkDomain;
    }

    @Override
    public String getStartIpAddress(long networkId) {
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
        if (vlans.isEmpty()) {
            return null;
        }

        String startIP = vlans.get(0).getIpRange().split("-")[0];

        for (VlanVO vlan : vlans) {
            String startIP1 = vlan.getIpRange().split("-")[0];
            long startIPLong = NetUtils.ip2Long(startIP);
            long startIPLong1 = NetUtils.ip2Long(startIP1);

            if (startIPLong1 < startIPLong) {
                startIP = startIP1;
            }
        }

        return startIP;
    }

    @Override
    public Long getPodIdForVlan(long vlanDbId) {
        PodVlanMapVO podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(vlanDbId);
        if (podVlanMaps == null) {
            return null;
        } else {
            return podVlanMaps.getPodId();
        }
    }

    @Override
    public Map<Service, Set<Provider>> getNetworkOfferingServiceProvidersMap(long networkOfferingId) {
        Map<Service, Set<Provider>> serviceProviderMap = new HashMap<Service, Set<Provider>>();
        List<NetworkOfferingServiceMapVO> map = _ntwkOfferingSrvcDao.listByNetworkOfferingId(networkOfferingId);

        for (NetworkOfferingServiceMapVO instance : map) {
            String service = instance.getService();
            Set<Provider> providers;
            providers = serviceProviderMap.get(Service.getService(service));
            if (providers == null) {
                providers = new HashSet<Provider>();
            }
            providers.add(Provider.getProvider(instance.getProvider()));
            serviceProviderMap.put(Service.getService(service), providers);
        }

        return serviceProviderMap;
    }

    @Override
    public boolean isProviderSupportServiceInNetwork(long networkId, Service service, Provider provider) {
        return _ntwkSrvcDao.canProviderSupportServiceInNetwork(networkId, service, provider);
    }

    @Override
    public List<? extends Provider> listSupportedNetworkServiceProviders(String serviceName) {
        Network.Service service = null;
        if (serviceName != null) {
            service = Network.Service.getService(serviceName);
            if (service == null) {
                throw new InvalidParameterValueException("Invalid Network Service=" + serviceName);
            }
        }

        Set<Provider> supportedProviders = new HashSet<Provider>();

        if (service != null) {
            List<Provider> providers = s_serviceToImplementedProvidersMap.get(service);
            if (providers != null && !providers.isEmpty()) {
                supportedProviders.addAll(providers);
            }
        } else {
            for (List<Provider> pList : s_serviceToImplementedProvidersMap.values()) {
                supportedProviders.addAll(pList);
            }
        }

        return new ArrayList<Provider>(supportedProviders);
    }

    @Override
    public Provider getDefaultUniqueProviderForService(String serviceName) {
        List<? extends Provider> providers = listSupportedNetworkServiceProviders(serviceName);
        if (providers.isEmpty()) {
            throw new CloudRuntimeException("No providers supporting service " + serviceName + " found in cloudStack");
        }
        if (providers.size() > 1) {
            throw new CloudRuntimeException("More than 1 provider supporting service " + serviceName + " found in cloudStack");
        }

        return providers.get(0);
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
            return getPhysicalNetworkId(zoneId, pNtwks, tag);
        } else {
            return pNtwks.get(0).getId();
        }
    }

    private Long getPhysicalNetworkId(long zoneId, List<PhysicalNetworkVO> pNtwks, String tag) {
        Long pNtwkId = null;
        for (PhysicalNetwork pNtwk : pNtwks) {
            if (tag == null && pNtwk.getTags().isEmpty()) {
                s_logger.debug("Found physical network id=" + pNtwk.getId() + " with null tag");
                if (pNtwkId != null) {
                    throw new CloudRuntimeException("There is more than 1 physical network with empty tag in the zone id=" + zoneId);
                }
                pNtwkId = pNtwk.getId();
            } else if (tag != null && pNtwk.getTags().contains(tag)) {
                s_logger.debug("Found physical network id=" + pNtwk.getId() + " based on requested tags " + tag);
                pNtwkId = pNtwk.getId();
                break;
            }
        }
        if (pNtwkId == null) {
            throw new InvalidParameterValueException("Unable to find physical network which match the tags " + tag);
        }
        return pNtwkId;
    }

    @Override
    public List<Long> listNetworkOfferingsForUpgrade(long networkId) {
        List<Long> offeringsToReturn = new ArrayList<Long>();
        NetworkOffering originalOffering = _entityMgr.findById(NetworkOffering.class, getNetwork(networkId).getNetworkOfferingId());

        boolean securityGroupSupportedByOriginalOff = areServicesSupportedByNetworkOffering(originalOffering.getId(), Service.SecurityGroup);

        // security group supported property should be the same

        List<Long> offerings = _networkOfferingDao.getOfferingIdsToUpgradeFrom(originalOffering);

        for (Long offeringId : offerings) {
            if (areServicesSupportedByNetworkOffering(offeringId, Service.SecurityGroup) == securityGroupSupportedByOriginalOff) {
                offeringsToReturn.add(offeringId);
            }
        }

        return offeringsToReturn;
    }

    @Override
    public boolean isSecurityGroupSupportedInNetwork(Network network) {
        if (network.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Security group can be enabled for Guest networks only; and network " + network + " has a diff traffic type");
            return false;
        }

        Long physicalNetworkId = network.getPhysicalNetworkId();

        // physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = findPhysicalNetworkId(network.getDataCenterId(), null, null);
        }

        return isServiceEnabledInNetwork(physicalNetworkId, network.getId(), Service.SecurityGroup);
    }

    @Override
    public PhysicalNetwork getDefaultPhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {

        List<PhysicalNetworkVO> networkList = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, trafficType);
        DataCenter dc = ApiDBUtils.findZoneById(zoneId);
        String dcUuid = String.valueOf(zoneId);
        if (dc != null) {
            dcUuid = dc.getUuid();
        }

        if (networkList.isEmpty()) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("Unable to find the default physical network with traffic=" + trafficType + " in the specified zone id");
            ex.addProxyObject(dcUuid, "zoneId");
            throw ex;
        }

        if (networkList.size() > 1) {
            InvalidParameterValueException ex =
                new InvalidParameterValueException("More than one physical networks exist in zone id=" + zoneId + " with traffic type=" + trafficType);
            ex.addProxyObject(dcUuid, "zoneId");
            throw ex;
        }

        return networkList.get(0);
    }

    @Override
    public String getDefaultManagementTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        try {
            PhysicalNetwork mgmtPhyNetwork = getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Management);
            PhysicalNetworkTrafficTypeVO mgmtTraffic = _pNTrafficTypeDao.findBy(mgmtPhyNetwork.getId(), TrafficType.Management);
            if (mgmtTraffic != null) {
                String label = null;
                switch (hypervisorType) {
                    case XenServer:
                        label = mgmtTraffic.getXenNetworkLabel();
                        break;
                    case KVM:
                        label = mgmtTraffic.getKvmNetworkLabel();
                        break;
                    case VMware:
                        label = mgmtTraffic.getVmwareNetworkLabel();
                        break;
                    case Hyperv:
                        label = mgmtTraffic.getHypervNetworkLabel();
                        break;
                    case Ovm3:
                        label = mgmtTraffic.getOvm3NetworkLabel();
                        break;
                }
                return label;
            }
        } catch (Exception ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to retrive the default label for management traffic:" + "zone: " + zoneId + " hypervisor: " + hypervisorType + " due to:" +
                    ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getDefaultStorageTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        try {
            PhysicalNetwork storagePhyNetwork = getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Storage);
            PhysicalNetworkTrafficTypeVO storageTraffic = _pNTrafficTypeDao.findBy(storagePhyNetwork.getId(), TrafficType.Storage);
            if (storageTraffic != null) {
                String label = null;
                switch (hypervisorType) {
                    case XenServer:
                        label = storageTraffic.getXenNetworkLabel();
                        break;
                    case KVM:
                        label = storageTraffic.getKvmNetworkLabel();
                        break;
                    case VMware:
                        label = storageTraffic.getVmwareNetworkLabel();
                        break;
                    case Hyperv:
                        label = storageTraffic.getHypervNetworkLabel();
                        break;
                    case Ovm3:
                        label = storageTraffic.getOvm3NetworkLabel();
                        break;
                }
                return label;
            }
        } catch (Exception ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to retrive the default label for storage traffic:" + "zone: " + zoneId + " hypervisor: " + hypervisorType + " due to:" +
                    ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public List<PhysicalNetworkSetupInfo> getPhysicalNetworkInfo(long dcId, HypervisorType hypervisorType) {
        List<PhysicalNetworkSetupInfo> networkInfoList = new ArrayList<PhysicalNetworkSetupInfo>();
        List<PhysicalNetworkVO> physicalNtwkList = _physicalNetworkDao.listByZone(dcId);
        for (PhysicalNetworkVO pNtwk : physicalNtwkList) {
            String publicName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Public, hypervisorType);
            String privateName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Management, hypervisorType);
            String guestName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Guest, hypervisorType);
            String storageName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Storage, hypervisorType);
            // String controlName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Control, hypervisorType);
            PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();
            info.setPhysicalNetworkId(pNtwk.getId());
            info.setGuestNetworkName(guestName);
            info.setPrivateNetworkName(privateName);
            info.setPublicNetworkName(publicName);
            info.setStorageNetworkName(storageName);
            PhysicalNetworkTrafficTypeVO mgmtTraffic = _pNTrafficTypeDao.findBy(pNtwk.getId(), TrafficType.Management);
            if (mgmtTraffic != null) {
                String vlan = mgmtTraffic.getVlan();
                info.setMgmtVlan(vlan);
            }
            networkInfoList.add(info);
        }
        return networkInfoList;
    }

    @Override
    public boolean isProviderEnabledInPhysicalNetwork(long physicalNetowrkId, String providerName) {
        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _pNSPDao.findByServiceProvider(physicalNetowrkId, providerName);
        if (ntwkSvcProvider == null) {
            s_logger.warn("Unable to find provider " + providerName + " in physical network id=" + physicalNetowrkId);
            return false;
        }
        return isProviderEnabled(ntwkSvcProvider);
    }

    @Override
    public boolean isProviderEnabledInZone(long zoneId, String provider) {
        //the provider has to be enabled at least in one network in the zone
        for (PhysicalNetwork pNtwk : _physicalNetworkDao.listByZone(zoneId)) {
            if (isProviderEnabledInPhysicalNetwork(pNtwk.getId(), provider)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getNetworkTag(HypervisorType hType, Network network) {
        // no network tag for control traffic type
        TrafficType effectiveTrafficType = network.getTrafficType();
        if (hType == HypervisorType.VMware && effectiveTrafficType == TrafficType.Control)
            effectiveTrafficType = TrafficType.Management;

        if (effectiveTrafficType == TrafficType.Control) {
            return null;
        }

        Long physicalNetworkId = null;
        if (effectiveTrafficType != TrafficType.Guest) {
            physicalNetworkId = getNonGuestNetworkPhysicalNetworkId(network, effectiveTrafficType);
        } else {
            NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
            physicalNetworkId = network.getPhysicalNetworkId();
            if (physicalNetworkId == null) {
                physicalNetworkId = findPhysicalNetworkId(network.getDataCenterId(), offering.getTags(), offering.getTrafficType());
            }
        }

        if (physicalNetworkId == null) {
            assert (false) : "Can't get the physical network";
            s_logger.warn("Can't get the physical network");
            return null;
        }

        return _pNTrafficTypeDao.getNetworkTag(physicalNetworkId, effectiveTrafficType, hType);
    }

    @Override
    public NetworkVO getExclusiveGuestNetwork(long zoneId) {
        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, zoneId, GuestType.Shared, TrafficType.Guest);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared + " in zone " +
                zoneId);
        }

        if (networks.size() > 1) {
            throw new InvalidParameterValueException("Found more than 1 network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared +
                " in zone " + zoneId);

        }

        return networks.get(0);
    }

    @Override
    public boolean isNetworkSystem(Network network) {
        NetworkOffering no = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (no.isSystemOnly()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Long getPhysicalNetworkId(Network network) {
        if (network.getTrafficType() != TrafficType.Guest) {
            return getNonGuestNetworkPhysicalNetworkId(network);
        }

        Long physicalNetworkId = network.getPhysicalNetworkId();
        NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
        if (physicalNetworkId == null) {
            physicalNetworkId = findPhysicalNetworkId(network.getDataCenterId(), offering.getTags(), offering.getTrafficType());
        }
        return physicalNetworkId;
    }

    @Override
    public boolean getAllowSubdomainAccessGlobal() {
        return _allowSubdomainNetworkAccess;
    }

    @Override
    public boolean isProviderForNetwork(Provider provider, long networkId) {
        if (_ntwkSrvcDao.isProviderForNetwork(networkId, provider) != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isProviderForNetworkOffering(Provider provider, long networkOfferingId) {
        if (_ntwkOfferingSrvcDao.isProviderForNetworkOffering(networkOfferingId, provider)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void canProviderSupportServices(Map<Provider, Set<Service>> providersMap) {
        for (Provider provider : providersMap.keySet()) {
            // check if services can be turned off
            NetworkElement element = getElementImplementingProvider(provider.getName());
            if (element == null) {
                throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getName() + "'");
            }

            Set<Service> enabledServices = new HashSet<Service>();
            enabledServices.addAll(providersMap.get(provider));

            if (enabledServices != null && !enabledServices.isEmpty()) {
                if (!element.canEnableIndividualServices()) {
                    Set<Service> requiredServices = new HashSet<Service>();
                    requiredServices.addAll(element.getCapabilities().keySet());

                    if (requiredServices.contains(Network.Service.Gateway)) {
                        requiredServices.remove(Network.Service.Gateway);
                    }

                    if (requiredServices.contains(Network.Service.Firewall)) {
                        requiredServices.remove(Network.Service.Firewall);
                    }

                    if (enabledServices.contains(Network.Service.Firewall)) {
                        enabledServices.remove(Network.Service.Firewall);
                    }

                    // exclude gateway service
                    if (enabledServices.size() != requiredServices.size()) {
                        StringBuilder servicesSet = new StringBuilder();

                        for (Service requiredService : requiredServices) {
                            // skip gateway service as we don't allow setting it via API
                            if (requiredService == Service.Gateway) {
                                continue;
                            }
                            servicesSet.append(requiredService.getName() + ", ");
                        }
                        servicesSet.delete(servicesSet.toString().length() - 2, servicesSet.toString().length());

                        throw new InvalidParameterValueException("Cannot enable subset of Services, Please specify the complete list of Services: " +
                            servicesSet.toString() + "  for Service Provider " + provider.getName());
                    }
                }
                List<String> serviceList = new ArrayList<String>();
                for (Service service : enabledServices) {
                    // check if the service is provided by this Provider
                    if (!element.getCapabilities().containsKey(service)) {
                        throw new UnsupportedServiceException(provider.getName() + " Provider cannot provide service " + service.getName());
                    }
                    serviceList.add(service.getName());
                }
                if (!element.verifyServicesCombination(enabledServices)) {
                    throw new UnsupportedServiceException("Provider " + provider.getName() + " doesn't support services combination: " + serviceList);
                }
            }
        }
    }

    @Override
    public boolean canAddDefaultSecurityGroup() {
        String defaultAdding = _configDao.getValue(Config.SecurityGroupDefaultAdding.key());
        return (defaultAdding != null && defaultAdding.equalsIgnoreCase("true"));
    }

    @Override
    public List<Service> listNetworkOfferingServices(long networkOfferingId) {
        List<Service> services = new ArrayList<Service>();
        List<String> servicesStr = _ntwkOfferingSrvcDao.listServicesForNetworkOffering(networkOfferingId);
        for (String serviceStr : servicesStr) {
            services.add(Service.getService(serviceStr));
        }

        return services;
    }

    @Override
    public boolean areServicesEnabledInZone(long zoneId, NetworkOffering offering, List<Service> services) {
        long physicalNtwkId = findPhysicalNetworkId(zoneId, offering.getTags(), offering.getTrafficType());
        boolean result = true;
        List<String> checkedProvider = new ArrayList<String>();
        for (Service service : services) {
            // get all the providers, and check if each provider is enabled
            List<String> providerNames = _ntwkOfferingSrvcDao.listProvidersForServiceForNetworkOffering(offering.getId(), service);
            for (String providerName : providerNames) {
                if (!checkedProvider.contains(providerName)) {
                    result = result && isProviderEnabledInPhysicalNetwork(physicalNtwkId, providerName);
                }
                checkedProvider.add(providerName);
            }
        }

        return result;
    }

    @Override
    public boolean checkIpForService(IpAddress userIp, Service service, Long networkId) {
        if (networkId == null) {
            networkId = userIp.getAssociatedWithNetworkId();
        }

        NetworkVO network = _networksDao.findById(networkId);
        NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (offering.getGuestType() != GuestType.Isolated) {
            return true;
        }
        IPAddressVO ipVO = _ipAddressDao.findById(userIp.getId());
        PublicIp publicIp = PublicIp.createFromAddrAndVlan(ipVO, _vlanDao.findById(userIp.getVlanId()));
        if (!canIpUsedForService(publicIp, service, networkId)) {
            return false;
        }
        if (!offering.isConserveMode()) {
            return canIpUsedForNonConserveService(publicIp, service);
        }
        return true;
    }

    @Override
    public boolean providerSupportsCapability(Set<Provider> providers, Service service, Capability cap) {
        for (Provider provider : providers) {
            NetworkElement element = getElementImplementingProvider(provider.getName());
            if (element != null) {
                Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
                if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() +
                            " implementing Provider=" + provider.getName());
                }
                Map<Capability, String> serviceCapabilities = elementCapabilities.get(service);
                if (serviceCapabilities == null || serviceCapabilities.isEmpty()) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " doesn't have capabilites for element=" + element.getName() +
                            " implementing Provider=" + provider.getName());
                }

                if (serviceCapabilities.containsKey(cap)) {
                    return true;
                }
            } else {
                throw new UnsupportedServiceException("Unable to find network element for provider " + provider.getName());
            }
        }
        return false;
    }

    @Override
    public void checkCapabilityForProvider(Set<Provider> providers, Service service, Capability cap, String capValue) {
        for (Provider provider : providers) {
            NetworkElement element = getElementImplementingProvider(provider.getName());
            if (element != null) {
                Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
                if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() +
                        " implementing Provider=" + provider.getName());
                }
                Map<Capability, String> serviceCapabilities = elementCapabilities.get(service);
                if (serviceCapabilities == null || serviceCapabilities.isEmpty()) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " doesn't have capabilities for element=" + element.getName() +
                        " implementing Provider=" + provider.getName());
                }

                String value = serviceCapabilities.get(cap);
                if (value == null || value.isEmpty()) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " doesn't have capability " + cap.getName() + " for element=" +
                        element.getName() + " implementing Provider=" + provider.getName());
                }

                if (!value.toLowerCase().contains(capValue.toLowerCase())) {
                    throw new UnsupportedServiceException("Service " + service.getName() + " doesn't support value " + capValue + " for capability " + cap.getName() +
                        " for element=" + element.getName() + " implementing Provider=" + provider.getName());
                }
            } else {
                throw new UnsupportedServiceException("Unable to find network element for provider " + provider.getName());
            }
        }
    }

    @Override
    public final void checkNetworkPermissions(Account caller, Network network) {
        if (_accountMgr.isRootAdmin(caller.getAccountId()) && Boolean.TRUE.equals(AdminIsAllowedToDeployAnywhere.value())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("root admin is permitted to do stuff on every network");
            }
        } else {
            if (network == null) {
                throw new CloudRuntimeException("cannot check permissions on (Network) <null>");
            }
            s_logger.info(String.format("Checking permission for account %s (%s) on network %s (%s)", caller.getAccountName(), caller.getUuid(), network.getName(), network.getUuid()));
            if (network.getGuestType() != GuestType.Shared || network.getAclType() == ACLType.Account) {
                checkAccountNetworkPermissions(caller, network);

            } else {
                checkDomainNetworkPermissions(caller, network);
            }
        }
    }

    private void checkAccountNetworkPermissions(Account caller, Network network) {
        AccountVO networkOwner = _accountDao.findById(network.getAccountId());
        if (networkOwner == null)
            throw new PermissionDeniedException("Unable to use network with id= " + ((NetworkVO) network).getUuid() +
                ", network does not have an owner");
        if (!Account.Type.PROJECT.equals(caller.getType()) && Account.Type.PROJECT.equals(networkOwner.getType())) {
            checkProjectNetworkPermissions(caller, networkOwner, network);
        } else {
            List<NetworkVO> networkMap = _networksDao.listBy(caller.getId(), network.getId());
            NetworkPermissionVO networkPermission = _networkPermissionDao.findByNetworkAndAccount(network.getId(), caller.getId());
            if (CollectionUtils.isEmpty(networkMap) && networkPermission == null) {
                throw new PermissionDeniedException(String.format(UNABLE_TO_USE_NETWORK, ((NetworkVO) network).getUuid()));
            }
        }
    }

    private void checkDomainNetworkPermissions(Account caller, Network network) {
        if (!isNetworkAvailableInDomain(network.getId(), caller.getDomainId())) {
            DomainVO callerDomain = _domainDao.findById(caller.getDomainId());
            if (callerDomain == null) {
                throw new CloudRuntimeException("cannot check permission on account " + caller.getAccountName() + " whose domain does not exist");
            }
            throw new PermissionDeniedException("Shared network id=" + ((NetworkVO) network).getUuid() + " is not available in domain id=" +
                    callerDomain.getUuid());
        }
    }

    private void checkProjectNetworkPermissions(Account owner, Account networkOwner, Network network){
        User user = CallContext.current().getCallingUser();
        Project project = projectDao.findByProjectAccountId(networkOwner.getId());
        if (project == null) {
            throw new CloudRuntimeException("Unable to find project to which the network belongs to");
        }
        ProjectAccount projectAccountUser = _projectAccountDao.findByProjectIdUserId(project.getId(), user.getAccountId(), user.getId());
        if (projectAccountUser != null) {
            if (!_projectAccountDao.canUserAccessProjectAccount(user.getAccountId(), user.getId(), networkOwner.getId())) {
                throw new PermissionDeniedException(String.format(UNABLE_TO_USE_NETWORK, ((NetworkVO)network).getUuid()));
            }
        } else {
            if (!_projectAccountDao.canAccessProjectAccount(owner.getAccountId(), networkOwner.getId())) {
                throw new PermissionDeniedException(String.format(UNABLE_TO_USE_NETWORK, ((NetworkVO) network).getUuid()));
            }
        }
    }

    @Override
    public void checkNetworkOperatePermissions(Account owner, Network network) {
        if (network == null) {
            throw new CloudRuntimeException("cannot check permissions on (Network) <null>");
        }
        if (owner.getType() == Account.Type.ADMIN) {
            return;
        }
        if (network.getGuestType() == GuestType.Shared) {
            checkSharedNetworkOperatePermissions(owner, network);
        } else {
            checkNonSharedNetworkOperatePermissions(owner, network);
        }
    }

    @Override
    public void checkRouterPermissions(Account owner, VirtualRouter router) {
        Account account = _accountMgr.getAccount(router.getAccountId());
        try {
            _accountMgr.checkAccess(owner, null, true, account);
            return;
        } catch (PermissionDeniedException ex) {
            s_logger.info("Account " + owner + " do not have permission on router owner " + account);
        }
        List<NicVO> routerNics = _nicDao.listByVmId(router.getId());
        for (final Nic routerNic : routerNics) {
            final NetworkVO network = _networksDao.findById(routerNic.getNetworkId());
            if (TrafficType.Guest.equals(network.getTrafficType())) {
                checkNetworkOperatePermissions(owner, network);
            }
        }
    }

    private void checkNonSharedNetworkOperatePermissions(Account owner, Network network) {
        // check on isolated/L2 networks
        Account networkOwner = _accountDao.findByIdIncludingRemoved(network.getAccountId());
        if (owner.getType() == Account.Type.DOMAIN_ADMIN) {
            if (!_domainDao.isChildDomain(owner.getDomainId(), networkOwner.getDomainId())) {
                throw new PermissionDeniedException(String.format("network %s cannot be operated by domain admin %s", network, owner));
            }
        } else if (owner.getType() == Account.Type.NORMAL) {
            if (owner.getType() != Account.Type.PROJECT && networkOwner.getType() == Account.Type.PROJECT) {
                checkProjectNetworkPermissions(owner, networkOwner, network);
            } else if (networkOwner.getAccountId() != owner.getAccountId()) {
                throw new PermissionDeniedException(String.format("network %s cannot be operated by normal user %s", network, owner));
            }
        } else {
            throw new PermissionDeniedException(String.format("network %s cannot be operated by this account %s", network, owner));
        }
    }

    private void checkSharedNetworkOperatePermissions(Account owner, Network network) {
        NetworkOffering networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if (networkOffering.isSpecifyVlan() && owner.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Shared network %s with specifyvlan=true can only be operated by root admin", network));
        }
        if (owner.getType() == Account.Type.DOMAIN_ADMIN) {
            if (network.getAclType() == ACLType.Domain) {
                // Allow domain admins to operate shared network for their domain.
                Long networkDomainId = getDomainIdForSharedNetwork(network);
                if (!_domainDao.isChildDomain(owner.getDomainId(), networkDomainId)) {
                    throw new PermissionDeniedException(String.format("Shared network %s belongs to another domain cannot be operated by domain admin %s", network, owner));
                }
            } else if (network.getAclType() == ACLType.Account) {
                // Allow domain admins to operate shared network for an account in their domain.
                Long networkAccountId = getAccountIdForSharedNetwork(network);
                if (!_domainDao.isChildDomain(owner.getDomainId(), _accountDao.findByIdIncludingRemoved(networkAccountId).getDomainId())) {
                    throw new PermissionDeniedException(String.format("Shared network %s belongs to an account in another domain cannot be operated by domain admin %s", network, owner));
                }
            }
        } else if (owner.getType() == Account.Type.NORMAL) {
            // Allow normal users to operate shared network for themselves.
            if (network.getAclType() == ACLType.Account) {
                // Allow domain admin to operate shared network for an account in its domain.
                Long networkAccountId = getAccountIdForSharedNetwork(network);
                Account networkOwner = _accountDao.findByIdIncludingRemoved(networkAccountId);
                if (owner.getType() != Account.Type.PROJECT && networkOwner.getType() == Account.Type.PROJECT) {
                    checkProjectNetworkPermissions(owner, networkOwner, network);
                } else if (networkOwner.getAccountId() != owner.getAccountId()) {
                    throw new PermissionDeniedException(String.format("Shared network %s belongs to another account cannot be operated by normal user %s", network, owner));
                }
            } else {
                throw new PermissionDeniedException(String.format("Shared network %s belongs to domain cannot be operated by normal user %s", network, owner));
            }
        } else if (owner.getType() != Account.Type.ADMIN) {
            throw new PermissionDeniedException(String.format("Shared network %s cannot be operated by account %s with type = %d", network, owner, owner.getType()));
        }
    }

    private Long getAccountIdForSharedNetwork(Network network) {
        NetworkAccountVO networkAccountMap = _networkAccountDao.getAccountNetworkMapByNetworkId(network.getId());
        if (networkAccountMap == null) {
            throw new CloudRuntimeException(String.format("Cannot find account info for Shared network %s with aclType=Account", network));
        }
        return networkAccountMap.getAccountId();
    }

    private Long getDomainIdForSharedNetwork(Network network) {
        NetworkDomainVO networkDomainMap = _networkDomainDao.getDomainNetworkMapByNetworkId(network.getId());
        if (networkDomainMap == null) {
            throw new CloudRuntimeException(String.format("Cannot find domain info for Shared network %s with aclType=Domain", network));
        }
        return networkDomainMap.getDomainId();
    }

    @Override
    public String getDefaultPublicTrafficLabel(long dcId, HypervisorType hypervisorType) {
        try {
            PhysicalNetwork publicPhyNetwork = getOnePhysicalNetworkByZoneAndTrafficType(dcId, TrafficType.Public);
            PhysicalNetworkTrafficTypeVO publicTraffic = _pNTrafficTypeDao.findBy(publicPhyNetwork.getId(), TrafficType.Public);
            if (publicTraffic != null) {
                String label = null;
                switch (hypervisorType) {
                    case XenServer:
                        label = publicTraffic.getXenNetworkLabel();
                        break;
                    case KVM:
                        label = publicTraffic.getKvmNetworkLabel();
                        break;
                    case VMware:
                        label = publicTraffic.getVmwareNetworkLabel();
                        break;
                    case Hyperv:
                        label = publicTraffic.getHypervNetworkLabel();
                        break;
                    case Ovm3:
                        label = publicTraffic.getOvm3NetworkLabel();
                        break;
                }
                return label;
            }
        } catch (Exception ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to retrieve the default label for public traffic." + "zone: " + dcId + " hypervisor: " + hypervisorType + " due to: " +
                    ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public String getDefaultGuestTrafficLabel(long dcId, HypervisorType hypervisorType) {
        try {
            PhysicalNetwork guestPhyNetwork = getOnePhysicalNetworkByZoneAndTrafficType(dcId, TrafficType.Guest);
            PhysicalNetworkTrafficTypeVO guestTraffic = _pNTrafficTypeDao.findBy(guestPhyNetwork.getId(), TrafficType.Guest);
            if (guestTraffic != null) {
                String label = null;
                switch (hypervisorType) {
                    case XenServer:
                        label = guestTraffic.getXenNetworkLabel();
                        break;
                    case KVM:
                        label = guestTraffic.getKvmNetworkLabel();
                        break;
                    case VMware:
                        label = guestTraffic.getVmwareNetworkLabel();
                        break;
                    case Hyperv:
                        label = guestTraffic.getHypervNetworkLabel();
                        break;
                    case Ovm3:
                        label = guestTraffic.getOvm3NetworkLabel();
                        break;
                }
                return label;
            }
        } catch (Exception ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Failed to retrive the default label for guest traffic:" + "zone: " + dcId + " hypervisor: " + hypervisorType + " due to:" +
                    ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public List<? extends Network> listNetworksByVpc(long vpcId) {
        return _networksDao.listByVpc(vpcId);
    }

    @Override
    public List<Provider> getNtwkOffDistinctProviders(long ntkwOffId) {
        List<String> providerNames = _ntwkOfferingSrvcDao.getDistinctProviders(ntkwOffId);
        List<Provider> providers = new ArrayList<Provider>();
        for (String providerName : providerNames) {
            providers.add(Network.Provider.getProvider(providerName));
        }

        return providers;
    }

    @Override
    public boolean isVmPartOfNetwork(long vmId, long ntwkId) {
        if (_nicDao.findNonReleasedByInstanceIdAndNetworkId(ntwkId, vmId) != null) {
            return true;
        }
        return false;
    }

    @Override
    public List<? extends PhysicalNetwork> getPhysicalNtwksSupportingTrafficType(long zoneId, TrafficType trafficType) {

        List<? extends PhysicalNetwork> pNtwks = _physicalNetworkDao.listByZone(zoneId);

        Iterator<? extends PhysicalNetwork> it = pNtwks.iterator();
        while (it.hasNext()) {
            PhysicalNetwork pNtwk = it.next();
            if (!_pNTrafficTypeDao.isTrafficTypeSupported(pNtwk.getId(), trafficType)) {
                it.remove();
            }
        }
        return pNtwks;
    }

    @Override
    public boolean isPrivateGateway(long ntwkId) {
        final VpcGatewayVO gateway = _vpcGatewayDao.getVpcGatewayByNetworkId(ntwkId);
        if (gateway == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames) {
        List<NetworkOfferingVO> offerings = new ArrayList<NetworkOfferingVO>(offeringNames.length);
        for (String offeringName : offeringNames) {
            NetworkOfferingVO network = _systemNetworks.get(offeringName);
            if (network == null) {
                throw new CloudRuntimeException("Unable to find system network profile for " + offeringName);
            }
            offerings.add(network);
        }
        return offerings;
    }

    @Override
    public boolean isNetworkAvailableInDomain(long networkId, long domainId) {
        Long networkDomainId = null;
        Network network = getNetwork(networkId);
        if (network.getGuestType() != GuestType.Shared) {
            s_logger.trace("Network id=" + networkId + " is not shared");
            return false;
        }

        NetworkDomainVO networkDomainMap = _networkDomainDao.getDomainNetworkMapByNetworkId(networkId);
        if (networkDomainMap == null) {
            s_logger.trace("Network id=" + networkId + " is shared, but not domain specific");
            return true;
        } else {
            networkDomainId = networkDomainMap.getDomainId();
        }

        if (domainId == networkDomainId.longValue()) {
            return true;
        }

        if (networkDomainMap.subdomainAccess) {
            Set<Long> parentDomains = _domainMgr.getDomainParentIds(domainId);

            if (parentDomains.contains(networkDomainId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Long> getAvailableIps(Network network, String requestedIp) {
        if (network.getCidr() == null) {
            return Collections.emptySet();
        }
        String[] cidr = network.getCidr().split("/");
        List<String> ips = getUsedIpsInNetwork(network);
        Set<Long> usedIps = new TreeSet<Long>();

        for (String ip : ips) {
            if (requestedIp != null && requestedIp.equals(ip)) {
                s_logger.warn("Requested ip address " + requestedIp + " is already in use in network" + network);
                return null;
            }

            usedIps.add(NetUtils.ip2Long(ip));
        }

        Set<Long> allPossibleIps = NetUtils.getAllIpsFromCidr(cidr[0], Integer.parseInt(cidr[1]), usedIps, -1);

        String gateway = network.getGateway();
        if ((gateway != null) && (allPossibleIps.contains(NetUtils.ip2Long(gateway))))
            allPossibleIps.remove(NetUtils.ip2Long(gateway));

        return allPossibleIps;
    }

    @Override
    public List<String> getUsedIpsInNetwork(Network network) {
        //Get all ips used by vms nics
        List<String> ips = _nicDao.listIpAddressInNetwork(network.getId());
        //Get all secondary ips for nics
        List<String> secondaryIps = _nicSecondaryIpDao.listSecondaryIpAddressInNetwork(network.getId());
        ips.addAll(secondaryIps);
        //Get ips used by load balancers
        List<String> lbIps = _appLbRuleDao.listLbIpsBySourceIpNetworkId(network.getId());
        ips.addAll(lbIps);
        //Get ips used by tungsten
        List<String> tfIps = tungstenGuestNetworkIpAddressDao.listGuestIpAddressByNetworkId(network.getId());
        ips.addAll(tfIps);
        return ips;
    }

    @Override
    public String getDomainNetworkDomain(long domainId, long zoneId) {
        String networkDomain = null;
        Long searchDomainId = domainId;
        while (searchDomainId != null) {
            DomainVO domain = _domainDao.findById(searchDomainId);
            if (domain.getNetworkDomain() != null) {
                networkDomain = domain.getNetworkDomain();
                break;
            }
            searchDomainId = domain.getParent();
        }
        if (networkDomain == null) {
            return getZoneNetworkDomain(zoneId);
        }
        return networkDomain;
    }

    boolean isProviderEnabled(PhysicalNetworkServiceProvider provider) {
        if (provider == null || provider.getState() != PhysicalNetworkServiceProvider.State.Enabled) { // TODO: check
            // for other states: Shutdown?
            return false;
        }
        return true;
    }

    boolean isServiceEnabledInNetwork(long physicalNetworkId, long networkId, Service service) {
        // check if the service is supported in the network
        if (!areServicesSupportedInNetwork(networkId, service)) {
            s_logger.debug("Service " + service.getName() + " is not supported in the network id=" + networkId);
            return false;
        }

        // get provider for the service and check if all of them are supported
        String provider = _ntwkSrvcDao.getProviderForServiceInNetwork(networkId, service);
        if (!isProviderEnabledInPhysicalNetwork(physicalNetworkId, provider)) {
            s_logger.debug("Provider " + provider + " is not enabled in physical network id=" + physicalNetworkId);
            return false;
        }

        return true;
    }

    @Override
    public Nic getNicInNetworkIncludingRemoved(long vmId, long networkId) {
        return _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(networkId, vmId);
    }

    String getZoneNetworkDomain(long zoneId) {
        return _dcDao.findById(zoneId).getDomain();
    }

    PhysicalNetwork getOnePhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        List<PhysicalNetworkVO> networkList = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, trafficType);

        if (networkList.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find the default physical network with traffic=" + trafficType + " in zone id=" + zoneId + ". ");
        }

        if (networkList.size() > 1) {
            s_logger.info("More than one physical networks exist in zone id=" + zoneId + " with traffic type=" + trafficType + ". ");
        }

        return networkList.get(0);
    }

    protected Long getNonGuestNetworkPhysicalNetworkId(Network network, TrafficType trafficType) {
        // VMware control network is management network
        // we need to retrieve traffic label information through physical network
        Long physicalNetworkId = network.getPhysicalNetworkId();

        if (physicalNetworkId == null) {
            List<PhysicalNetworkVO> pNtwks = _physicalNetworkDao.listByZone(network.getDataCenterId());
            if (pNtwks.size() == 1) {
                physicalNetworkId = pNtwks.get(0).getId();
            } else {
                // locate physicalNetwork with supported traffic type
                // We can make this assumptions based on the fact that Public/Management/Control traffic types are
                // supported only in one physical network in the zone in 3.0
                for (PhysicalNetworkVO pNtwk : pNtwks) {
                    if (_pNTrafficTypeDao.isTrafficTypeSupported(pNtwk.getId(), trafficType)) {
                        physicalNetworkId = pNtwk.getId();
                        break;
                    }
                }
            }
        }
        return physicalNetworkId;
    }

    protected Long getNonGuestNetworkPhysicalNetworkId(Network network) {
        // no physical network for control traffic type

        // have to remove this sanity check as VMware control network is management network
        // we need to retrieve traffic label information through physical network
        /*
                if (network.getTrafficType() == TrafficType.Control) {
                    return null;
                }
        */
        Long physicalNetworkId = network.getPhysicalNetworkId();

        if (physicalNetworkId == null) {
            List<PhysicalNetworkVO> pNtwks = _physicalNetworkDao.listByZone(network.getDataCenterId());
            if (pNtwks.size() == 1) {
                physicalNetworkId = pNtwks.get(0).getId();
            } else {
                // locate physicalNetwork with supported traffic type
                // We can make this assumptions based on the fact that Public/Management/Control traffic types are
                // supported only in one physical network in the zone in 3.0
                for (PhysicalNetworkVO pNtwk : pNtwks) {
                    if (_pNTrafficTypeDao.isTrafficTypeSupported(pNtwk.getId(), network.getTrafficType())) {
                        physicalNetworkId = pNtwk.getId();
                        break;
                    }
                }
            }
        }
        return physicalNetworkId;
    }

    @Override
    public NicProfile getNicProfile(VirtualMachine vm, long networkId, String broadcastUri) {
        NicVO nic = null;
        if (broadcastUri != null) {
            nic = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(networkId, vm.getId(), broadcastUri);
        } else {
            nic = _nicDao.findByNtwkIdAndInstanceId(networkId, vm.getId());
        }
        if (nic == null) {
            return null;
        }
        NetworkVO network = _networksDao.findById(networkId);
        Integer networkRate = getNetworkRate(network.getId(), vm.getId());

//        NetworkGuru guru = _networkGurus.get(network.getGuruName());
        NicProfile profile =
            new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), networkRate, isSecurityGroupSupportedInNetwork(network), getNetworkTag(
                vm.getHypervisorType(), network));
        if (network.getTrafficType() == TrafficType.Public && network.getPublicMtu() != null) {
            profile.setMtu(network.getPublicMtu());
        }
        if (network.getTrafficType() == TrafficType.Guest && network.getPrivateMtu() != null) {
            profile.setMtu(network.getPrivateMtu());
        }
//        guru.updateNicProfile(profile, network);
        return profile;
    }

    @Override
    public boolean networkIsConfiguredForExternalNetworking(long zoneId, long networkId) {
        List<Provider> networkProviders = getNetworkProviders(networkId);
        for (Provider provider : networkProviders) {
            if (provider.isExternal()) {
                return true;
            }
        }
        return false;
    }

    private List<Provider> getNetworkProviders(long networkId) {
        List<String> providerNames = _ntwkSrvcDao.getDistinctProviders(networkId);
        Map<String, Provider> providers = new HashMap<String, Provider>();
        for (String providerName : providerNames) {
            if (!providers.containsKey(providerName)) {
                providers.put(providerName, Network.Provider.getProvider(providerName));
            }
        }

        return new ArrayList<Provider>(providers.values());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _configs = _configDao.getConfiguration("Network", params);
        _allowSubdomainNetworkAccess = Boolean.valueOf(_configs.get(Config.SubDomainNetworkAccess.key()));
        _executeInSequenceNtwkElmtCmd = Boolean.valueOf(_configs.get(Config.ExecuteInSequenceNetworkElementCommands.key()));

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemPublicNetwork, TrafficType.Public, true);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        _systemNetworks.put(NetworkOffering.SystemPublicNetwork, publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemManagementNetwork, TrafficType.Management, false);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        _systemNetworks.put(NetworkOffering.SystemManagementNetwork, managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemControlNetwork, TrafficType.Control, false);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        _systemNetworks.put(NetworkOffering.SystemControlNetwork, controlNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemStorageNetwork, TrafficType.Storage, true);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        _systemNetworks.put(NetworkOffering.SystemStorageNetwork, storageNetworkOffering);
        NetworkOfferingVO privateGatewayNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemPrivateGatewayNetworkOffering, GuestType.Isolated, true);
        privateGatewayNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(privateGatewayNetworkOffering);
        _systemNetworks.put(NetworkOffering.SystemPrivateGatewayNetworkOffering, privateGatewayNetworkOffering);
        NetworkOfferingVO privateGatewayNetworkOfferingWithoutVlan = new NetworkOfferingVO(NetworkOffering.SystemPrivateGatewayNetworkOfferingWithoutVlan, GuestType.Isolated, false);
        privateGatewayNetworkOfferingWithoutVlan = _networkOfferingDao.persistDefaultNetworkOffering(privateGatewayNetworkOfferingWithoutVlan);
        _systemNetworks.put(NetworkOffering.SystemPrivateGatewayNetworkOfferingWithoutVlan, privateGatewayNetworkOfferingWithoutVlan);

        IpAddressSearch = _ipAddressDao.createSearchBuilder();
        IpAddressSearch.and("accountId", IpAddressSearch.entity().getAllocatedToAccountId(), Op.EQ);
        IpAddressSearch.and("dataCenterId", IpAddressSearch.entity().getDataCenterId(), Op.EQ);
        IpAddressSearch.and("vpcId", IpAddressSearch.entity().getVpcId(), Op.EQ);
        IpAddressSearch.and("associatedWithNetworkId", IpAddressSearch.entity().getAssociatedWithNetworkId(), Op.EQ);
        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch.join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(),
            JoinBuilder.JoinType.INNER);
        IpAddressSearch.done();

        NicForTrafficTypeSearch = _nicDao.createSearchBuilder();
        SearchBuilder<NetworkVO> networkSearch = _networksDao.createSearchBuilder();
        NicForTrafficTypeSearch.join("network", networkSearch, networkSearch.entity().getId(), NicForTrafficTypeSearch.entity().getNetworkId(), JoinType.INNER);
        NicForTrafficTypeSearch.and("instance", NicForTrafficTypeSearch.entity().getInstanceId(), Op.EQ);
        networkSearch.and("traffictype", networkSearch.entity().getTrafficType(), Op.EQ);
        NicForTrafficTypeSearch.done();

        s_logger.info("Network Model is configured.");

        return true;
    }

    @Override
    public boolean start() {
        // populate s_serviceToImplementedProvidersMap & s_providerToNetworkElementMap with current _networkElements
        // Need to do this in start() since _networkElements are not completely configured until then.
        for (NetworkElement element : networkElements) {
            Map<Service, Map<Capability, String>> capabilities = element.getCapabilities();
            Provider implementedProvider = element.getProvider();
            if (implementedProvider != null) {
                if (s_providerToNetworkElementMap.containsKey(implementedProvider.getName())) {
                    s_logger.error("Cannot start NetworkModel: Provider <-> NetworkElement must be a one-to-one map, " + "multiple NetworkElements found for Provider: " +
                        implementedProvider.getName());
                    continue;
                }
                s_logger.info("Add provider <-> element map entry. " + implementedProvider.getName() + "-" + element.getName() + "-" + element.getClass().getSimpleName());
                s_providerToNetworkElementMap.put(implementedProvider.getName(), element.getName());
            }
            if (capabilities != null && implementedProvider != null) {
                for (Service service : capabilities.keySet()) {
                    if (s_serviceToImplementedProvidersMap.containsKey(service)) {
                        List<Provider> providers = s_serviceToImplementedProvidersMap.get(service);
                        providers.add(implementedProvider);
                    } else {
                        List<Provider> providers = new ArrayList<Provider>();
                        providers.add(implementedProvider);
                        s_serviceToImplementedProvidersMap.put(service, providers);
                    }
                }
            }
        }

        //After network elements are configured correctly, verify ConfigDrive entries on enabled zones
        verifyDisabledConfigDriveEntriesOnEnabledZones();

        s_logger.info("Started Network Model");
        return true;
    }

    /**
     * Verifies ConfigDrive entries on a zone and adds disabled ConfigDrive provider if missing.
     */
    protected void addDisabledConfigDriveEntriesOnZone(DataCenterVO zone) {
        if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
            List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZoneAndTrafficType(
                    zone.getId(), TrafficType.Guest);
            for (PhysicalNetworkVO physicalNetworkVO : physicalNetworks) {
                PhysicalNetworkServiceProviderVO provider = _pNSPDao.findByServiceProvider(
                        physicalNetworkVO.getId(), Provider.ConfigDrive.getName());
                if (provider == null) {
                    _networkService.addProviderToPhysicalNetwork(
                            physicalNetworkVO.getId(), Provider.ConfigDrive.getName(), null, null);
                }
            }
        }
    }

    /**
     * Verifies ConfigDrive entries on enabled zones, adds disabled ConfigDrive provider if missing.
     */
    protected void verifyDisabledConfigDriveEntriesOnEnabledZones() {
        List<DataCenterVO> zones = _dcDao.listEnabledZones();
        if (CollectionUtils.isNotEmpty(zones)) {
            for (DataCenterVO zone : zones) {
                addDisabledConfigDriveEntriesOnZone(zone);
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public PublicIpAddress getSourceNatIpAddressForGuestNetwork(Account owner, Network guestNetwork) {
        List<? extends IpAddress> addrs = listPublicIpsAssignedToGuestNtwk(owner.getId(), guestNetwork.getId(), true);

        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            return null;
        } else {
            for (IpAddress addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = _ipAddressDao.findById(addr.getId());
                    return PublicIp.createFromAddrAndVlan(sourceNatIp, _vlanDao.findById(sourceNatIp.getVlanId()));
                }
            }
        }

        return null;
    }

    @Override
    public boolean isNetworkInlineMode(Network network) {
        NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        return offering.isInline();
    }

    @Override
    public void checkIp6Parameters(String startIPv6, String endIPv6, String ip6Gateway, String ip6Cidr) throws InvalidParameterValueException {

        if (org.apache.commons.lang3.StringUtils.isAnyBlank(ip6Gateway, ip6Cidr)) {
            throw new InvalidParameterValueException("ip6Gateway and ip6Cidr should be defined for an IPv6 network work properly");
        }

        if (!NetUtils.isValidIp6(ip6Gateway)) {
            throw new InvalidParameterValueException("Invalid ip6Gateway");
        }
        if (!NetUtils.isValidIp6Cidr(ip6Cidr)) {
            throw new InvalidParameterValueException("Invalid ip6cidr");
        }

        if (!NetUtils.isIp6InNetwork(ip6Gateway, ip6Cidr)) {
            throw new InvalidParameterValueException("ip6Gateway is not in ip6cidr indicated network!");
        }

        if (org.apache.commons.lang3.StringUtils.isNotBlank(startIPv6)) {
            if (!NetUtils.isValidIp6(startIPv6)) {
                throw new InvalidParameterValueException("Invalid format for the startIPv6 parameter");
            }
            if (!NetUtils.isIp6InNetwork(startIPv6, ip6Cidr)) {
                throw new InvalidParameterValueException("startIPv6 is not in ip6cidr indicated network!");
            }
        }

        if (org.apache.commons.lang3.StringUtils.isNotBlank(endIPv6)) {
            if (!NetUtils.isValidIp6(endIPv6)) {
                throw new InvalidParameterValueException("Invalid format for the endIPv6 parameter");
            }
            if (!NetUtils.isIp6InNetwork(endIPv6, ip6Cidr)) {
                throw new InvalidParameterValueException("endIPv6 is not in ip6cidr indicated network!");
            }
        }

        int cidrSize = NetUtils.getIp6CidrSize(ip6Cidr);
        // we only support cidr == 64
        if (cidrSize != 64) {
            throw new InvalidParameterValueException("The cidr size of IPv6 network must be 64 bits!");
        }
    }

    @Override
    public void checkRequestedIpAddresses(long networkId, IpAddresses ips) throws InvalidParameterValueException {
        String ip4 = ips.getIp4Address();
        String ip6 = ips.getIp6Address();
        String mac = ips.getMacAddress();
        if (ip4 != null) {
            if (!NetUtils.isValidIp4(ip4)) {
                throw new InvalidParameterValueException("Invalid specified IPv4 address " + ip4);
            }
            //Other checks for ipv4 are done in assignPublicIpAddress()
        }
        if (ip6 != null) {
            if (!NetUtils.isValidIp6(ip6)) {
                throw new InvalidParameterValueException("Invalid specified IPv6 address " + ip6);
            }
            if (_ipv6Dao.findByNetworkIdAndIp(networkId, ip6) != null) {
                throw new InvalidParameterValueException("The requested IP is already taken!");
            }
            List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
            if (vlans == null) {
                throw new CloudRuntimeException("Cannot find related vlan attached to network " + networkId);
            }
            Vlan ipVlan = null;
            for (Vlan vlan : vlans) {
                if (NetUtils.isIp6InRange(ip6, vlan.getIp6Range())) {
                    ipVlan = vlan;
                    break;
                }
            }
            if (ipVlan == null) {
                throw new InvalidParameterValueException("Requested IPv6 is not in the predefined range!");
            }
        }
        if (mac != null) {
            if(!NetUtils.isValidMac(mac)) {
                throw new InvalidParameterValueException("Invalid specified MAC address " + mac);
            }
            if (_nicDao.findByNetworkIdAndMacAddress(networkId, mac) != null) {
                throw new InvalidParameterValueException("The requested Mac address is already taken! " + mac);
            }

        }
    }

    @Override
    public String getStartIpv6Address(long networkId) {
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
        if (vlans == null) {
            return null;
        }
        String startIpv6 = null;
        // Get the start ip of first create vlan(not the lowest, because if you add a lower vlan, lowest vlan would change)
        for (Vlan vlan : vlans) {
            if (vlan.getIp6Range() != null) {
                startIpv6 = vlan.getIp6Range().split("-")[0];
                break;
            }
        }
        return startIpv6;
    }

    @Override
    public NicVO getPlaceholderNicForRouter(Network network, Long podId) {
        List<NicVO> nics = _nicDao.listPlaceholderNicsByNetworkIdAndVmType(network.getId(), VirtualMachine.Type.DomainRouter);
        List<? extends Vlan> vlans = new ArrayList<VlanVO>();
        if (podId != null) {
            vlans = _vlanDao.listVlansForPod(podId);
        }
        for (NicVO nic : nics) {
            if (nic.getReserver() == null && (nic.getIPv4Address() != null || nic.getIPv6Address() != null)) {
                if (podId == null) {
                    return nic;
                } else {
                    IpAddress ip = null;
                    UserIpv6AddressVO ipv6 = null;

                    if (nic.getIPv4Address() != null) {
                        ip = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), nic.getIPv4Address());
                    } else {
                        ipv6 = _ipv6Dao.findByNetworkIdAndIp(network.getId(), nic.getIPv6Address());
                    }

                    if (vlans.isEmpty()) {
                        return nic;
                    }
                    //return nic only when its ip address belong to the pod range (for the Basic zone case)
                    for (Vlan vlan : vlans) {
                        if (ip != null && ip.getVlanId() == vlan.getId()) {
                            return nic;
                        } else if (ipv6 != null && ipv6.getVlanId() == vlan.getId()) {
                            return nic;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IpAddress getPublicIpAddress(String ipAddress, long zoneId) {
        List<? extends Network> networks = _networksDao.listByZoneAndTrafficType(zoneId, TrafficType.Public);
        if (networks.isEmpty() || networks.size() > 1) {
            throw new CloudRuntimeException("Can't find public network in the zone specified");
        }

        return _ipAddressDao.findByIpAndSourceNetworkId(networks.get(0).getId(), ipAddress);
    }

    @Override
    public Map<Detail, String> getNtwkOffDetails(long offId) {
        return _ntwkOffDetailsDao.getNtwkOffDetails(offId);
    }

    @Override
    public Networks.IsolationType[] listNetworkIsolationMethods() {
        return Networks.IsolationType.values();
    }

    @Override
    public boolean getExecuteInSeqNtwkElmtCmd() {
        return _executeInSequenceNtwkElmtCmd;
    }

    @Override
    public boolean isNetworkReadyForGc(long networkId) {
        Network network = getNetwork(networkId);
        List<Long> networkIds = _networksDao.findNetworksToGarbageCollect();
        List<String> secondaryIps = _nicSecondaryIpDao.listSecondaryIpAddressInNetwork(networkId);
        if (!networkIds.contains(networkId)) {
            return false;
        }

        // add an exception for networks that use external networking devices and has secondary guest IP's allocated.
        // On network GC, when network goes through implement phase a new vlan is allocated, based on the acquired VLAN
        // id cidr of the network is decided in case of external networking case. While NIC uses reservation strategy 'Start'
        // which ensures that new primary ip is allocated for the NiC from the new CIDR. Secondary IP's have hardcoded IP's in
        // network rules. So prevent network GC.
        if (secondaryIps != null && !secondaryIps.isEmpty() && networkIsConfiguredForExternalNetworking(network.getDataCenterId(), networkId)) {
            return false;
        }

        //if the network has vms in Starting state (nics for those might not be allocated yet as Starting state also used when vm is being Created)
        //don't GC
        if (_nicDao.countNicsForStartingVms(networkId) > 0) {
            s_logger.debug("Network id=" + networkId + " is not ready for GC as it has vms that are Starting at the moment");
            return false;
        }

        return true;
    }

    @Override
    public boolean getNetworkEgressDefaultPolicy(Long networkId) {
        NetworkVO network = _networksDao.findById(networkId);

        if (network != null) {
            NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
            return offering.isEgressDefaultPolicy();
        } else {
            InvalidParameterValueException ex = new InvalidParameterValueException("network with network id does not exist");
            throw ex;
        }
    }

    @Override
    public List<String[]> generateVmData(String userData, String userDataDetails, String serviceOffering, long datacenterId,
                                         String vmName, String vmHostName, long vmId, String vmUuid,
                                         String guestIpAddress, String publicKey, String password, Boolean isWindows, String hostname) {

        DataCenterVO dcVo = _dcDao.findById(datacenterId);
        final String zoneName = dcVo.getName();

        IPAddressVO publicIp = _ipAddressDao.findByAssociatedVmId(vmId);
        VirtualMachine vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Cannot generate VM instance data, no VM exists by ID: %d", vmId));
        }

        final List<String[]> vmData = new ArrayList<String[]>();

        if (userData != null) {
            vmData.add(new String[]{USERDATA_DIR, USERDATA_FILE, userData});
        }
        vmData.add(new String[]{METATDATA_DIR, SERVICE_OFFERING_FILE, StringUtils.unicodeEscape(serviceOffering)});
        vmData.add(new String[]{METATDATA_DIR, AVAILABILITY_ZONE_FILE, StringUtils.unicodeEscape(zoneName)});
        vmData.add(new String[]{METATDATA_DIR, LOCAL_HOSTNAME_FILE, StringUtils.unicodeEscape(vmHostName)});
        vmData.add(new String[]{METATDATA_DIR, LOCAL_IPV4_FILE, guestIpAddress});

        addUserDataDetailsToCommand(vmData, userDataDetails);

        String publicIpAddress = guestIpAddress;
        String publicHostName = StringUtils.unicodeEscape(vmHostName);

        if (dcVo.getNetworkType() != DataCenter.NetworkType.Basic) {
            if (publicIp != null) {
                publicIpAddress = publicIp.getAddress().addr();
                publicHostName = publicIp.getAddress().addr();
            } else {
                publicHostName = null;
            }
        }
        vmData.add(new String[]{METATDATA_DIR, PUBLIC_IPV4_FILE, publicIpAddress});
        vmData.add(new String[]{METATDATA_DIR, PUBLIC_HOSTNAME_FILE, publicHostName});

        if (vmUuid == null) {
            vmData.add(new String[]{METATDATA_DIR, INSTANCE_ID_FILE, vmName});
            vmData.add(new String[]{METATDATA_DIR, VM_ID_FILE, String.valueOf(vmId)});
        } else {
            vmData.add(new String[]{METATDATA_DIR, INSTANCE_ID_FILE, vmUuid});
            vmData.add(new String[]{METATDATA_DIR, VM_ID_FILE, vmUuid});
        }

        vmData.add(new String[]{METATDATA_DIR, PUBLIC_KEYS_FILE, publicKey});

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        } else {
            cloudIdentifier = "CloudStack-{" + cloudIdentifier + "}";
        }
        vmData.add(new String[]{METATDATA_DIR, CLOUD_IDENTIFIER_FILE, cloudIdentifier});

        if (password != null && !password.isEmpty() && !password.equals("saved_password")) {

            // Here we are calculating MD5 checksum to reduce the over head of calculating MD5 checksum
            // in windows VM in password reset script.

            if (isWindows) {
                MessageDigest md5 = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    s_logger.error("Unexpected exception " + e.getMessage(), e);
                    throw new CloudRuntimeException("Unable to get MD5 MessageDigest", e);
                }
                md5.reset();
                md5.update(password.getBytes(StringUtils.getPreferredCharset()));
                byte[] digest = md5.digest();
                BigInteger bigInt = new BigInteger(1, digest);
                String hashtext = bigInt.toString(16);

                vmData.add(new String[]{PASSWORD_DIR, PASSWORD_CHECKSUM_FILE, hashtext});
            }

            vmData.add(new String[]{PASSWORD_DIR, PASSWORD_FILE, password});
        }
        vmData.add(new String[]{METATDATA_DIR, HYPERVISOR_HOST_NAME_FILE, hostname});

        Domain domain = _domainDao.findById(vm.getDomainId());
        if (domain != null && VirtualMachineManager.AllowExposeDomainInMetadata.valueIn(domain.getId())) {
            s_logger.debug("Adding domain info to cloud metadata");
            vmData.add(new String[]{METATDATA_DIR, CLOUD_DOMAIN_FILE, domain.getName()});
            vmData.add(new String[]{METATDATA_DIR, CLOUD_DOMAIN_ID_FILE, domain.getUuid()});
        }

        return vmData;
    }

    protected void addUserDataDetailsToCommand(List<String[]> vmData, String userDataDetails) {
        if(userDataDetails != null && !userDataDetails.isEmpty()) {
            userDataDetails = userDataDetails.substring(1, userDataDetails.length()-1);
            String[] keyValuePairs = userDataDetails.split(",");
            for(String pair : keyValuePairs)
            {
                final Pair<String, String> keyValue = StringUtils.getKeyValuePairWithSeparator(pair, "=");
                vmData.add(new String[]{METATDATA_DIR, keyValue.first(), StringUtils.unicodeEscape(keyValue.second())});
            }
        }
    }

    @Override
    public String getConfigComponentName() {
        return NetworkModel.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {MACIdentifier, AdminIsAllowedToDeployAnywhere};
    }

    @Override
    public String getValidNetworkCidr(Network guestNetwork) {
        String networkCidr = guestNetwork.getNetworkCidr();
        return networkCidr == null ? guestNetwork.getCidr() : networkCidr;
    }

    @Override
    public Pair<String, String> getNetworkIp4Dns(final Network network, final DataCenter zone) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(network.getDns1())) {
            return new Pair<>(network.getDns1(), network.getDns2());
        }
        return new Pair<>(zone.getDns1(), zone.getDns2());
    }

    @Override
    public Pair<String, String> getNetworkIp6Dns(final Network network, final DataCenter zone) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(network.getIp6Dns1())) {
            return new Pair<>(network.getIp6Dns1(), network.getIp6Dns2());
        }
        return new Pair<>(zone.getIp6Dns1(), zone.getIp6Dns2());
    }

    @Override
    public void verifyIp4DnsPair(String ip4Dns1, String ip4Dns2) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(ip4Dns1) && org.apache.commons.lang3.StringUtils.isNotEmpty(ip4Dns2)) {
            throw new InvalidParameterValueException("Second IPv4 DNS can be specified only with the first IPv4 DNS");
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(ip4Dns1) && !NetUtils.isValidIp4(ip4Dns1)) {
            throw new InvalidParameterValueException("Invalid IPv4 for DNS1");
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(ip4Dns2) && !NetUtils.isValidIp4(ip4Dns2)) {
            throw new InvalidParameterValueException("Invalid IPv4 for DNS2");
        }
    }

    @Override
    public void verifyIp6DnsPair(String ip6Dns1, String ip6Dns2) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(ip6Dns1) && org.apache.commons.lang3.StringUtils.isNotEmpty(ip6Dns2)) {
            throw new InvalidParameterValueException("Second IPv6 DNS can be specified only with the first IPv6 DNS");
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(ip6Dns1) && !NetUtils.isValidIp6(ip6Dns1)) {
            throw new InvalidParameterValueException("Invalid IPv6 for IPv6 DNS1");
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(ip6Dns2) && !NetUtils.isValidIp6(ip6Dns2)) {
            throw new InvalidParameterValueException("Invalid IPv6 for IPv6 DNS2");
        }
    }
}
