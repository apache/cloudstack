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
package com.cloud.network.vpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.network.element.StaticNatServiceProvider;
import org.apache.cloudstack.api.command.user.vpc.ListStaticRoutesCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.api.command.user.vpc.ListPrivateGatewaysCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.VpcOffering.State;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.dao.DomainRouterDao;


@Component
@Local(value = { VpcManager.class, VpcService.class })
public class VpcManagerImpl extends ManagerBase implements VpcManager{
    private static final Logger s_logger = Logger.getLogger(VpcManagerImpl.class);
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffSvcMapDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _ntwkDao;
    @Inject
    NetworkManager _ntwkMgr;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    NetworkService _ntwkSvc;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOffServiceDao ;
    @Inject
    VpcOfferingServiceMapDao _vpcOffServiceDao;
    @Inject
    PhysicalNetworkDao _pNtwkDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    VlanDao _vlanDao = null;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    VpcServiceMapDao _vpcSrvcDao;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("VpcChecker"));
    private List<VpcProvider> vpcElements = null;
    private final List<Service> nonSupportedServices = Arrays.asList(Service.SecurityGroup, Service.Firewall);
 
    int _cleanupInterval;
    int _maxNetworks;
    SearchBuilder<IPAddressVO> IpAddressSearch;

    @Override
    @DB
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        //configure default vpc offering
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        if (_vpcOffDao.findByUniqueName(VpcOffering.defaultVPCOfferingName) == null) {
            s_logger.debug("Creating default VPC offering " + VpcOffering.defaultVPCOfferingName);
            
            Map<Service, Set<Provider>> svcProviderMap = new HashMap<Service, Set<Provider>>();
            Set<Provider> defaultProviders = new HashSet<Provider>();
            defaultProviders.add(Provider.VPCVirtualRouter);
            for (Service svc : getSupportedServices()) {
                if (svc == Service.Lb) {
                    Set<Provider> lbProviders = new HashSet<Provider>();
                    lbProviders.add(Provider.VPCVirtualRouter);
                    svcProviderMap.put(svc, lbProviders);
                } else {
                    svcProviderMap.put(svc, defaultProviders);
                }
            }
            createVpcOffering(VpcOffering.defaultVPCOfferingName, VpcOffering.defaultVPCOfferingName, svcProviderMap, 
                    true, State.Enabled);
        }
                
        txn.commit();
        
        Map<String, String> configs = _configDao.getConfiguration(params);
        String value = configs.get(Config.VpcCleanupInterval.key());
        _cleanupInterval = NumbersUtil.parseInt(value, 60 * 60); // 1 hour

        String maxNtwks = configs.get(Config.VpcMaxNetworks.key());
        _maxNetworks = NumbersUtil.parseInt(maxNtwks, 3); // max=3 is default
        
        
        IpAddressSearch = _ipAddressDao.createSearchBuilder();
        IpAddressSearch.and("accountId", IpAddressSearch.entity().getAllocatedToAccountId(), Op.EQ);
        IpAddressSearch.and("dataCenterId", IpAddressSearch.entity().getDataCenterId(), Op.EQ);
        IpAddressSearch.and("vpcId", IpAddressSearch.entity().getVpcId(), Op.EQ);
        IpAddressSearch.and("associatedWithNetworkId", IpAddressSearch.entity().getAssociatedWithNetworkId(), Op.EQ);
        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch.join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);
        IpAddressSearch.done();
        
        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new VpcCleanupTask(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<? extends Network> getVpcNetworks(long vpcId) {
        return _ntwkDao.listByVpc(vpcId);
    }

    @Override
    public VpcOffering getVpcOffering(long vpcOffId) {
        return _vpcOffDao.findById(vpcOffId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_CREATE, eventDescription = "creating vpc offering", create=true)
    public VpcOffering createVpcOffering(String name, String displayText, List<String> supportedServices, Map<String, List<String>> serviceProviders) {
        Map<Network.Service, Set<Network.Provider>> svcProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();
        defaultProviders.add(Provider.VPCVirtualRouter);

        boolean sourceNatSvc = false;
        boolean firewallSvs = false;
        // populate the services first
        for (String serviceName : supportedServices) {
            // validate if the service is supported
            Service service = Network.Service.getService(serviceName);
            if (service == null || nonSupportedServices.contains(service)) {
                throw new InvalidParameterValueException("Service " + serviceName + " is not supported in VPC");
            }

            svcProviderMap.put(service, defaultProviders);
            if (service == Service.NetworkACL) {
                firewallSvs = true;
            }
            
            if (service == Service.SourceNat) {
                sourceNatSvc = true;
            }
        }
        
        if (!sourceNatSvc) {
            s_logger.debug("Automatically adding source nat service to the list of VPC services");
            svcProviderMap.put(Service.SourceNat, defaultProviders);
        }
        
        if (!firewallSvs) {
            s_logger.debug("Automatically adding network ACL service to the list of VPC services");
            svcProviderMap.put(Service.NetworkACL, defaultProviders);
        }
        
        svcProviderMap.put(Service.Gateway, defaultProviders);

        if (serviceProviders != null) {
            for (String serviceStr : serviceProviders.keySet()) {
                Network.Service service = Network.Service.getService(serviceStr);
                if (svcProviderMap.containsKey(service)) {
                    Set<Provider> providers = new HashSet<Provider>();
                    // don't allow to specify more than 1 provider per service
                    if (serviceProviders.get(serviceStr) != null && serviceProviders.get(serviceStr).size() > 1) {
                        throw new InvalidParameterValueException("In the current release only one provider can be " +
                                "specified for the service");
                    }
                    for (String prvNameStr : serviceProviders.get(serviceStr)) {
                        // check if provider is supported
                        Network.Provider provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }

                        providers.add(provider);
                    }
                    svcProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceStr + " is not enabled for the network " +
                            "offering, can't add a provider to it");
                }
            }
        }

        VpcOffering offering = createVpcOffering(name, displayText, svcProviderMap, false, null);
        UserContext.current().setEventDetails(" Id: " + offering.getId() + " Name: " + name);
        
        return offering;
    }

    
    @DB
    protected VpcOffering createVpcOffering(String name, String displayText, Map<Network.Service, 
            Set<Network.Provider>> svcProviderMap, boolean isDefault, State state) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // create vpc offering object
        VpcOfferingVO offering  = new VpcOfferingVO(name, displayText, isDefault, null);
        
        if (state != null) {
            offering.setState(state);
        }
        s_logger.debug("Adding vpc offering " + offering);
        offering = _vpcOffDao.persist(offering);
        // populate services and providers
        if (svcProviderMap != null) {
            for (Network.Service service : svcProviderMap.keySet()) {
                Set<Provider> providers = svcProviderMap.get(service);
                if (providers != null && !providers.isEmpty()) {
                    for (Network.Provider provider : providers) {
                        VpcOfferingServiceMapVO offService = new VpcOfferingServiceMapVO(offering.getId(), service, provider);
                        _vpcOffSvcMapDao.persist(offService);
                        s_logger.trace("Added service for the vpc offering: " + offService + " with provider " + provider.getName());
                    }
                } else {
                    throw new InvalidParameterValueException("Provider is missing for the VPC offering service " + service.getName());
                }
            }
        }
        txn.commit();

        return offering;
    }
    
    @Override
    public Vpc getVpc(long vpcId) {
        return _vpcDao.findById(vpcId);
    }
    
    @Override
    public Vpc getActiveVpc(long vpcId) {
        return _vpcDao.getActiveVpcById(vpcId);
    }

    @Override
    public Map<Service, Set<Provider>> getVpcOffSvcProvidersMap(long vpcOffId) {
        Map<Service, Set<Provider>> serviceProviderMap = new HashMap<Service, Set<Provider>>();
        List<VpcOfferingServiceMapVO> map = _vpcOffSvcMapDao.listByVpcOffId(vpcOffId);

        for (VpcOfferingServiceMapVO instance : map) {
            String service = instance.getService();
            Set<Provider> providers;
            providers = serviceProviderMap.get(service);
            if (providers == null) {
                providers = new HashSet<Provider>();
            }
            providers.add(Provider.getProvider(instance.getProvider()));
            serviceProviderMap.put(Service.getService(service), providers);
        }

        return serviceProviderMap;
    }
    
    
    @Override
    public List<? extends VpcOffering> listVpcOfferings(Long id, String name, String displayText, List<String> supportedServicesStr,
            Boolean isDefault, String keyword, String state, Long startIndex, Long pageSizeVal) {
        Filter searchFilter = new Filter(VpcOfferingVO.class, "created", false, startIndex, pageSizeVal);
        SearchCriteria<VpcOfferingVO> sc = _vpcOffDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria<VpcOfferingVO> ssc = _vpcOffDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (displayText != null) {
            sc.addAnd("displayText", SearchCriteria.Op.LIKE, "%" + displayText + "%");
        }

        if (isDefault != null) {
            sc.addAnd("isDefault", SearchCriteria.Op.EQ, isDefault);
        }

        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }


        List<VpcOfferingVO> offerings = _vpcOffDao.search(sc, searchFilter);

        // filter by supported services
        boolean listBySupportedServices = (supportedServicesStr != null && !supportedServicesStr.isEmpty() && !offerings.isEmpty());
        
        if (listBySupportedServices) {
            List<VpcOfferingVO> supportedOfferings = new ArrayList<VpcOfferingVO>();
            Service[] supportedServices = null;

            if (listBySupportedServices) {
                supportedServices = new Service[supportedServicesStr.size()];
                int i = 0;
                for (String supportedServiceStr : supportedServicesStr) {
                    Service service = Service.getService(supportedServiceStr);
                    if (service == null) {
                        throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                    } else {
                        supportedServices[i] = service;
                    }
                    i++;
                }
            }

            for (VpcOfferingVO offering : offerings) {
                if (areServicesSupportedByVpcOffering(offering.getId(), supportedServices)) {
                    supportedOfferings.add(offering);
                }
            }

            return supportedOfferings;
        } else {
            return offerings;
        }
    }


    protected boolean areServicesSupportedByVpcOffering(long vpcOffId, Service... services) {
        return (_vpcOffSvcMapDao.areServicesSupportedByNetworkOffering(vpcOffId, services));
    }
    
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_DELETE, eventDescription = "deleting vpc offering")
    public boolean deleteVpcOffering(long offId) {
        UserContext.current().setEventDetails(" Id: " + offId);

        // Verify vpc offering id
        VpcOfferingVO offering = _vpcOffDao.findById(offId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find vpc offering " + offId);
        } 

        // Don't allow to delete default vpc offerings
        if (offering.isDefault() == true) {
            throw new InvalidParameterValueException("Default network offering can't be deleted");
        }

        // don't allow to delete vpc offering if it's in use by existing vpcs (the offering can be disabled though)
        int vpcCount = _vpcDao.getVpcCountByOfferingId(offId);
        if (vpcCount > 0) {
            throw new InvalidParameterValueException("Can't delete vpc offering " + offId + " as its used by " + vpcCount + " vpcs. " +
                    "To make the network offering unavaiable, disable it");
        }

        if (_vpcOffDao.remove(offId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_UPDATE, eventDescription = "updating vpc offering")
    public VpcOffering updateVpcOffering(long vpcOffId, String vpcOfferingName, String displayText, String state) {
        UserContext.current().setEventDetails(" Id: " + vpcOffId);

        // Verify input parameters
        VpcOfferingVO offeringToUpdate = _vpcOffDao.findById(vpcOffId);
        if (offeringToUpdate == null) {
            throw new InvalidParameterValueException("Unable to find vpc offering " + vpcOffId);
        }

        VpcOfferingVO offering = _vpcOffDao.createForUpdate(vpcOffId);

        if (vpcOfferingName != null) {
            offering.setName(vpcOfferingName);
        }

        if (displayText != null) {
            offering.setDisplayText(displayText);
        }

        if (state != null) {
            boolean validState = false;
            for (VpcOffering.State st : VpcOffering.State.values()) {
                if (st.name().equalsIgnoreCase(state)) {
                    validState = true;
                    offering.setState(st);
                }
            }
            if (!validState) {
                throw new InvalidParameterValueException("Incorrect state value: " + state);
            }
        }

        if (_vpcOffDao.update(vpcOffId, offering)) {
            s_logger.debug("Updated VPC offeirng id=" + vpcOffId);
            return _vpcOffDao.findById(vpcOffId);
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_CREATE, eventDescription = "creating vpc", create=true)
    public Vpc createVpc(long zoneId, long vpcOffId, long vpcOwnerId, String vpcName, String displayText, String cidr, 
            String networkDomain) throws ResourceAllocationException {
        Account caller = UserContext.current().getCaller();
        Account owner = _accountMgr.getAccount(vpcOwnerId);
        
        //Verify that caller can perform actions in behalf of vpc owner
        _accountMgr.checkAccess(caller, null, false, owner);
        
        //check resource limit
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.vpc);

        // Validate vpc offering
        VpcOfferingVO vpcOff = _vpcOffDao.findById(vpcOffId);
        if (vpcOff == null || vpcOff.getState() != State.Enabled) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find vpc offering in " + State.Enabled +
                    " state by specified id");
            ex.addProxyObject("vpc_offerings", vpcOffId, "vpcOfferingId");
            throw ex;
        }
       
        //Validate zone
        DataCenter zone = _configMgr.getZone(zoneId);
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            // See DataCenterVO.java
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation since specified Zone is currently disabled");
            ex.addProxyObject("data_center", zone.getId(), "zoneId");
            throw ex;
        }
        
        if (networkDomain == null) {
            // 1) Get networkDomain from the corresponding account
            networkDomain = _ntwkModel.getAccountNetworkDomain(owner.getId(), zoneId);


            // 2) If null, generate networkDomain using domain suffix from the global config variables
            if (networkDomain == null) {
                networkDomain = "cs" + Long.toHexString(owner.getId()) + _ntwkModel.getDefaultNetworkDomain();
            }
        }
        
        return createVpc(zoneId, vpcOffId, owner, vpcName, displayText, cidr, networkDomain);
    }
    
    @Override
    public boolean vpcProviderEnabledInZone(long zoneId, String provider)
    {
        //the provider has to be enabled at least in one network in the zone
        for (PhysicalNetwork pNtwk : _pNtwkDao.listByZone(zoneId)) {
            if (_ntwkModel.isProviderEnabledInPhysicalNetwork(pNtwk.getId(), provider)) {
                return true;
            }
        }
        
        return false;
    }

    
    @DB
    protected Vpc createVpc(long zoneId, long vpcOffId, Account vpcOwner, String vpcName, String displayText, String cidr, 
            String networkDomain) {
        
        //Validate CIDR
        if (!NetUtils.isValidCIDR(cidr)) {
            throw new InvalidParameterValueException("Invalid CIDR specified " + cidr);
        }
        
        //cidr has to be RFC 1918 complient
        if (!NetUtils.validateGuestCidr(cidr)) {
            throw new InvalidParameterValueException("Guest Cidr " + cidr + " is not RFC1918 compliant");
        }

        // validate network domain
        if (!NetUtils.verifyDomainName(networkDomain)) {
            throw new InvalidParameterValueException(
                    "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain " +
                    "label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', " +
                    "the digits '0' through '9', "
                            + "and the hyphen ('-'); can't start or end with \"-\"");
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        VpcVO vpc = new VpcVO (zoneId, vpcName, displayText, vpcOwner.getId(), vpcOwner.getDomainId(), vpcOffId, cidr, 
                networkDomain);
        vpc = _vpcDao.persist(vpc, finalizeServicesAndProvidersForVpc(zoneId, vpcOffId));
        _resourceLimitMgr.incrementResourceCount(vpcOwner.getId(), ResourceType.vpc);
        txn.commit();

        s_logger.debug("Created VPC " + vpc);

        return vpc; 
    }

    private Map<String, String> finalizeServicesAndProvidersForVpc(long zoneId, long offeringId) {
        Map<String, String> svcProviders = new HashMap<String, String>();
        Map<String, List<String>> providerSvcs = new HashMap<String, List<String>>();
        List<VpcOfferingServiceMapVO> servicesMap = _vpcOffSvcMapDao.listByVpcOffId(offeringId);

        for (VpcOfferingServiceMapVO serviceMap : servicesMap) {
            if (svcProviders.containsKey(serviceMap.getService())) {
                // FIXME - right now we pick up the first provider from the list, need to add more logic based on
                // provider load, etc
                continue;
            }

            String service = serviceMap.getService();
            String provider = serviceMap.getProvider();

            if (provider == null) {
                // Default to VPCVirtualRouter
                provider = Provider.VPCVirtualRouter.getName();
            }


            if (!vpcProviderEnabledInZone(zoneId, provider)) {
                throw new InvalidParameterValueException("Provider " + provider +
                        " should be enabled in at least one physical network of the zone specified");
            }

            svcProviders.put(service, provider);
            List<String> l = providerSvcs.get(provider);
            if (l == null) {
                providerSvcs.put(provider, l = new ArrayList<String>());
            }
            l.add(service);
        }

        return svcProviders;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_DELETE, eventDescription = "deleting VPC")
    public boolean deleteVpc(long vpcId) throws ConcurrentOperationException, ResourceUnavailableException {
        UserContext.current().setEventDetails(" Id: " + vpcId);
        UserContext ctx = UserContext.current();

        // Verify vpc id
        Vpc vpc = getVpc(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("unable to find VPC id=" + vpcId);
        }
        
        //verify permissions
        _accountMgr.checkAccess(ctx.getCaller(), null, false, vpc);
        
        return destroyVpc(vpc, ctx.getCaller(), ctx.getCallerUserId());
    }

    @Override
    @DB
    public boolean destroyVpc(Vpc vpc, Account caller, Long callerUserId) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Destroying vpc " + vpc);
        
        //don't allow to delete vpc if it's in use by existing networks
        int networksCount = _ntwkDao.getNetworkCountByVpcId(vpc.getId());
        if (networksCount > 0) {
            throw new InvalidParameterValueException("Can't delete VPC " + vpc + " as its used by " + networksCount + " networks");
        }

        //mark VPC as inactive
        if (vpc.getState() != Vpc.State.Inactive) {
            s_logger.debug("Updating VPC " + vpc + " with state " + Vpc.State.Inactive + " as a part of vpc delete");
            VpcVO vpcVO = _vpcDao.findById(vpc.getId());
            vpcVO.setState(Vpc.State.Inactive);
            
            Transaction txn = Transaction.currentTxn();
            txn.start();
            _vpcDao.update(vpc.getId(), vpcVO);
            
            //decrement resource count
            _resourceLimitMgr.decrementResourceCount(vpc.getAccountId(), ResourceType.vpc);
            txn.commit();
        }
        
        //shutdown VPC
        if (!shutdownVpc(vpc.getId())) {
            s_logger.warn("Failed to shutdown vpc " + vpc + " as a part of vpc destroy process");
            return false;
        }
        
        //cleanup vpc resources
        if (!cleanupVpcResources(vpc.getId(), caller, callerUserId)) {
            s_logger.warn("Failed to cleanup resources for vpc " + vpc);
            return false;
        }

        //update the instance with removed flag only when the cleanup is executed successfully
        if (_vpcDao.remove(vpc.getId())) {
            s_logger.debug("Vpc " + vpc + " is destroyed succesfully");
            return true;
        } else {
            s_logger.warn("Vpc " + vpc + " failed to destroy");
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_UPDATE, eventDescription = "updating vpc")
    public Vpc updateVpc(long vpcId, String vpcName, String displayText) {
        UserContext.current().setEventDetails(" Id: " + vpcId);
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        VpcVO vpcToUpdate = _vpcDao.findById(vpcId);
        if (vpcToUpdate == null) {
            throw new InvalidParameterValueException("Unable to find vpc offering " + vpcId);
        }
        
        _accountMgr.checkAccess(caller, null, false, vpcToUpdate);

        VpcVO vpc = _vpcDao.createForUpdate(vpcId);

        if (vpcName != null) {
            vpc.setName(vpcName);
        }

        if (displayText != null) {
            vpc.setDisplayText(displayText);
        }

        if (_vpcDao.update(vpcId, vpc)) {
            s_logger.debug("Updated VPC id=" + vpcId);
            return _vpcDao.findById(vpcId);
        } else {
            return null;
        }
    }


    @Override
    public List<? extends Vpc> listVpcs(Long id, String vpcName, String displayText, List<String> supportedServicesStr, 
            String cidr, Long vpcOffId, String state, String accountName, Long domainId, String keyword,
            Long startIndex, Long pageSizeVal, Long zoneId, Boolean isRecursive, Boolean listAll, Boolean restartRequired, Map<String, String> tags, Long projectId) {
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, 
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject,
                listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VpcVO.class, "created", false, startIndex, pageSizeVal);

        SearchBuilder<VpcVO> sb = _vpcDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("displayText", sb.entity().getDisplayText(), SearchCriteria.Op.LIKE);
        sb.and("vpcOfferingId", sb.entity().getVpcOfferingId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("restartRequired", sb.entity().isRestartRequired(), SearchCriteria.Op.EQ);
        sb.and("cidr", sb.entity().getCidr(), SearchCriteria.Op.EQ);
        
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
        
        // now set the SC criteria...
        SearchCriteria<VpcVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);  

        if (keyword != null) {
            SearchCriteria<VpcVO> ssc = _vpcDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (vpcName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + vpcName + "%");
        }

        if (displayText != null) {
            sc.addAnd("displayText", SearchCriteria.Op.LIKE, "%" + displayText + "%");
        }
        
        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.Vpc.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }   
       }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        
        if (vpcOffId != null) {
            sc.addAnd("vpcOfferingId", SearchCriteria.Op.EQ, vpcOffId);
        }
        
        if (zoneId != null) {
            sc.addAnd("zoneId", SearchCriteria.Op.EQ, zoneId);
        }
        
        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }
        
        if (cidr != null) {
            sc.addAnd("cidr", SearchCriteria.Op.EQ, cidr);
        }
        
        if (restartRequired != null) {
            sc.addAnd("restartRequired", SearchCriteria.Op.EQ, restartRequired);
        }

        List<VpcVO> vpcs = _vpcDao.search(sc, searchFilter);

        // filter by supported services
        boolean listBySupportedServices = (supportedServicesStr != null && !supportedServicesStr.isEmpty() && !vpcs.isEmpty());
        
        if (listBySupportedServices) {
            List<VpcVO> supportedVpcs = new ArrayList<VpcVO>();
            Service[] supportedServices = null;

            if (listBySupportedServices) {
                supportedServices = new Service[supportedServicesStr.size()];
                int i = 0;
                for (String supportedServiceStr : supportedServicesStr) {
                    Service service = Service.getService(supportedServiceStr);
                    if (service == null) {
                        throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                    } else {
                        supportedServices[i] = service;
                    }
                    i++;
                }
            }

            for (VpcVO vpc : vpcs) {
                if (areServicesSupportedByVpcOffering(vpc.getVpcOfferingId(), supportedServices)) {
                    supportedVpcs.add(vpc);
                }
            }

            return supportedVpcs;
        } else {
            return vpcs;
        }
    }

    
    protected List<Service> getSupportedServices() {
        List<Service> services = new ArrayList<Service>();
        services.add(Network.Service.Dhcp);
        services.add(Network.Service.Dns);
        services.add(Network.Service.UserData);
        services.add(Network.Service.NetworkACL);
        services.add(Network.Service.PortForwarding);
        services.add(Network.Service.Lb);
        services.add(Network.Service.SourceNat);
        services.add(Network.Service.StaticNat);
        services.add(Network.Service.Gateway);
        services.add(Network.Service.Vpn);
        return services;
    }
    
    @Override
    public boolean startVpc(long vpcId, boolean destroyOnFailure) throws ConcurrentOperationException, ResourceUnavailableException, 
    InsufficientCapacityException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        User callerUser = _accountMgr.getActiveUser(ctx.getCallerUserId());
        
        //check if vpc exists
        Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject("vpc", vpcId, "VPC");
            throw ex;
        }
        
        //permission check
        _accountMgr.checkAccess(caller, null, false, vpc);
        
        DataCenter dc = _configMgr.getZone(vpc.getZoneId());
     
        DeployDestination dest = new DeployDestination(dc, null, null, null);
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, 
                _accountMgr.getAccount(vpc.getAccountId()));
        
        boolean result = true;
        try {
            if (!startVpc(vpc, dest, context)) {
                s_logger.warn("Failed to start vpc " + vpc);
                result = false;
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to start vpc " + vpc + " due to ", ex);
            result = false;
        } finally {
            //do cleanup
            if (!result && destroyOnFailure) {
                s_logger.debug("Destroying vpc " + vpc + " that failed to start");
                if (destroyVpc(vpc, caller, callerUser.getId())) {
                    s_logger.warn("Successfully destroyed vpc " + vpc + " that failed to start");
                } else {
                    s_logger.warn("Failed to destroy vpc " + vpc + " that failed to start");
                }
            }
        }
        return result;
    }

    protected boolean startVpc(Vpc vpc, DeployDestination dest, ReservationContext context) 
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        //deploy provider
        boolean success = true;
        List<Provider> providersToImplement = getVpcProviders(vpc.getId());
        for (VpcProvider element: getVpcElements()){
            if(providersToImplement.contains(element.getProvider())){
                if (element.implementVpc(vpc, dest, context)) {
                    s_logger.debug("Vpc " + vpc + " has started succesfully");
                } else {
                    s_logger.warn("Vpc " + vpc + " failed to start");
                    success = false;
                }
            }
        }
        return success;
    }
    
    @Override
    public boolean shutdownVpc(long vpcId) throws ConcurrentOperationException, ResourceUnavailableException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();
        
        //check if vpc exists
        Vpc vpc = getVpc(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Unable to find vpc by id " + vpcId);
        }
        
        //permission check
        _accountMgr.checkAccess(caller, null, false, vpc);

        //shutdown provider
        s_logger.debug("Shutting down vpc " + vpc);
        //TODO - shutdown all vpc resources here (ACLs, gateways, etc)

        boolean success = true;
        List<Provider> providersToImplement = getVpcProviders(vpc.getId());
        ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(ctx.getCallerUserId()), caller);
        for (VpcProvider element: getVpcElements()){
            if(providersToImplement.contains(element.getProvider())){
                if (element.shutdownVpc(vpc, context)) {
                    s_logger.debug("Vpc " + vpc + " has been shutdown succesfully");
                } else {
                    s_logger.warn("Vpc " + vpc + " failed to shutdown");
                    success = false;
                }
            }
        }

        return success;
    }
    
    @Override
    @DB
    public void validateNtkwOffForVpc(long ntwkOffId, String cidr, String networkDomain, 
            Account networkOwner, Vpc vpc, Long networkId, String gateway) {
        
        NetworkOffering guestNtwkOff = _configMgr.getNetworkOffering(ntwkOffId);
        
        if (guestNtwkOff == null) {
            throw new InvalidParameterValueException("Can't find network offering by id specified");
        }

        if (networkId == null) {
            //1) Validate attributes that has to be passed in when create new guest network
            validateNewVpcGuestNetwork(cidr, gateway, networkOwner, vpc, networkDomain); 
        }

        //2) validate network offering attributes
        List<Service> svcs = _ntwkModel.listNetworkOfferingServices(guestNtwkOff.getId());
        validateNtwkOffForVpc(guestNtwkOff, svcs);

        //3) Check services/providers against VPC providers
        List<NetworkOfferingServiceMapVO> networkProviders = _ntwkOffServiceDao.listByNetworkOfferingId(guestNtwkOff.getId());
        
        for (NetworkOfferingServiceMapVO nSvcVO : networkProviders) {
            String pr = nSvcVO.getProvider();
            String service = nSvcVO.getService();
            if (_vpcOffServiceDao.findByServiceProviderAndOfferingId(service, pr, vpc.getVpcOfferingId()) == null) {
                throw new InvalidParameterValueException("Service/provider combination " + service + "/" + 
                        pr + " is not supported by VPC " + vpc);
            }
        }

        //4) Only one network in the VPC can support LB
        if (_ntwkModel.areServicesSupportedByNetworkOffering(guestNtwkOff.getId(), Service.Lb)) {
            List<? extends Network> networks = getVpcNetworks(vpc.getId());
            for (Network network : networks) {
                if (networkId != null && network.getId() == networkId.longValue()) {
                    //skip my own network
                    continue;
                } else {
                    if (_ntwkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb)) {
                        throw new InvalidParameterValueException("LB service is already supported " +
                        		"by network " + network + " in VPC " + vpc);
                    }
                }
            }
        }
    }

    @Override
    public void validateNtwkOffForVpc(NetworkOffering guestNtwkOff, List<Service> supportedSvcs) {
        //1) in current release, only vpc provider is supported by Vpc offering
        List<Provider> providers = _ntwkModel.getNtwkOffDistinctProviders(guestNtwkOff.getId());
        for (Provider provider : providers) {
            if (provider != Provider.VPCVirtualRouter) {
                throw new InvalidParameterValueException("Only provider of type " + Provider.VPCVirtualRouter.getName() 
                        + " is supported for network offering that can be used in VPC");
            }
        }
        
        //2) Only Isolated networks with Source nat service enabled can be added to vpc
        if (!(guestNtwkOff.getGuestType() == GuestType.Isolated 
                && supportedSvcs.contains(Service.SourceNat))) {

            throw new InvalidParameterValueException("Only network offerings of type " + GuestType.Isolated + " with service "
                    + Service.SourceNat.getName() +
                    " are valid for vpc ");
        }

        //3) No redundant router support
        if (guestNtwkOff.getRedundantRouter()) {
            throw new InvalidParameterValueException("No redunant router support when network belnogs to VPC");
        }

        //4) Conserve mode should be off
        if (guestNtwkOff.isConserveMode()) {
            throw new InvalidParameterValueException("Only networks with conserve mode Off can belong to VPC");
        }
    }

    @DB
    protected void validateNewVpcGuestNetwork(String cidr, String gateway, Account networkOwner, Vpc vpc, String networkDomain) {
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        Vpc locked = _vpcDao.acquireInLockTable(vpc.getId());
        if (locked == null) {
            throw new CloudRuntimeException("Unable to acquire lock on " + vpc);
        }
        
        try {
            //check number of active networks in vpc
            if (_ntwkDao.countVpcNetworks(vpc.getId()) >= _maxNetworks) {
                throw new CloudRuntimeException("Number of networks per VPC can't extend " 
                        + _maxNetworks + "; increase it using global config " + Config.VpcMaxNetworks);
            }
            
            
            //1) CIDR is required
            if (cidr == null) {
                throw new InvalidParameterValueException("Gateway/netmask are required when create network for VPC");
            }
            
            //2) Network cidr should be within vpcCidr
            if (!NetUtils.isNetworkAWithinNetworkB(cidr, vpc.getCidr())) {
                throw new InvalidParameterValueException("Network cidr " + cidr + " is not within vpc " + vpc + " cidr");
            }
            
            //3) Network cidr shouldn't cross the cidr of other vpc network cidrs
            List<? extends Network> ntwks = _ntwkDao.listByVpc(vpc.getId());
            for (Network ntwk : ntwks) {
                assert (cidr != null) : "Why the network cidr is null when it belongs to vpc?";
                
                if (NetUtils.isNetworkAWithinNetworkB(ntwk.getCidr(), cidr) 
                        || NetUtils.isNetworkAWithinNetworkB(cidr, ntwk.getCidr())) {
                    throw new InvalidParameterValueException("Network cidr " + cidr + " crosses other network cidr " + ntwk + 
                            " belonging to the same vpc " + vpc);
                }
            }
            
            //4) vpc and network should belong to the same owner
            if (vpc.getAccountId() != networkOwner.getId()) {
                throw new InvalidParameterValueException("Vpc " + vpc + " owner is different from the network owner "
                        + networkOwner);
            }
            
            //5) network domain should be the same as VPC's
            if (!networkDomain.equalsIgnoreCase(vpc.getNetworkDomain())) {
                throw new InvalidParameterValueException("Network domain of the new network should match network" +
                		" domain of vpc " + vpc);
            }
            
            //6) gateway should never be equal to the cidr subnet
            if (NetUtils.getCidrSubNet(cidr).equalsIgnoreCase(gateway)) {
                throw new InvalidParameterValueException("Invalid gateway specified. It should never be equal to the cidr subnet value");
            }

            txn.commit();
        } finally {
            s_logger.debug("Releasing lock for " + locked);
            _vpcDao.releaseFromLockTable(locked.getId());
        }
    }


    protected List<VpcProvider> getVpcElements() {
        if (vpcElements == null) {
            vpcElements = new ArrayList<VpcProvider>();
            vpcElements.add((VpcProvider)_ntwkModel.getElementImplementingProvider(Provider.VPCVirtualRouter.getName()));
        }

        if (vpcElements == null) {
            throw new CloudRuntimeException("Failed to initialize vpc elements");
        }

        return vpcElements;
    }
    
    @Override
    public List<? extends Vpc> getVpcsForAccount(long accountId) {
        return _vpcDao.listByAccountId(accountId);
    }
    
    public boolean cleanupVpcResources(long vpcId, Account caller, long callerUserId) 
            throws ResourceUnavailableException, ConcurrentOperationException {
        s_logger.debug("Cleaning up resources for vpc id=" + vpcId);
        boolean success = true;

        //1) Remove VPN connections and VPN gateway
        s_logger.debug("Cleaning up existed site to site VPN connections");
        _s2sVpnMgr.cleanupVpnConnectionByVpc(vpcId);
        s_logger.debug("Cleaning up existed site to site VPN gateways");
        _s2sVpnMgr.cleanupVpnGatewayByVpc(vpcId);
        
        //2) release all ip addresses
        List<IPAddressVO> ipsToRelease = _ipAddressDao.listByAssociatedVpc(vpcId, null);
        s_logger.debug("Releasing ips for vpc id=" + vpcId + " as a part of vpc cleanup");
        for (IPAddressVO ipToRelease : ipsToRelease) {
            success = success && _ntwkMgr.disassociatePublicIpAddress(ipToRelease.getId(), callerUserId, caller);
            if (!success) {
                s_logger.warn("Failed to cleanup ip " + ipToRelease + " as a part of vpc id=" + vpcId + " cleanup");
            }
        } 
        
        if (success) {
            s_logger.debug("Released ip addresses for vpc id=" + vpcId + " as a part of cleanup vpc process");
        } else {
            s_logger.warn("Failed to release ip addresses for vpc id=" + vpcId + " as a part of cleanup vpc process");
            //although it failed, proceed to the next cleanup step as it doesn't depend on the public ip release
        }

        //3) Delete all static route rules
        if (!revokeStaticRoutesForVpc(vpcId, caller)) {
            s_logger.warn("Failed to revoke static routes for vpc " + vpcId + " as a part of cleanup vpc process");
            return false;
        }

        //4) Delete private gateway
        VpcGateway gateway = getPrivateGatewayForVpc(vpcId);
        if (gateway != null) {
            s_logger.debug("Deleting private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
            if (!deleteVpcPrivateGateway(gateway.getId())) {
                success = false;
                s_logger.debug("Failed to delete private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
            } else {
                s_logger.debug("Deleted private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
            }
        }
        
        return success;
    }


    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_RESTART, eventDescription = "restarting vpc")
    public boolean restartVpc(long vpcId) throws ConcurrentOperationException, ResourceUnavailableException, 
                                        InsufficientCapacityException {
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject("vpc", vpcId, "VPC");
            throw ex;
        }
        
        _accountMgr.checkAccess(caller, null, false, vpc);
        
        s_logger.debug("Restarting VPC " + vpc);
        boolean restartRequired = false;
        try {
            s_logger.debug("Shutting down VPC " + vpc + " as a part of VPC restart process");
            if (!shutdownVpc(vpcId)) {
                s_logger.warn("Failed to shutdown vpc as a part of VPC " + vpc + " restart process");
                restartRequired = true;
                return false;
            }
            
            s_logger.debug("Starting VPC " + vpc + " as a part of VPC restart process");
            if (!startVpc(vpcId, false)) {
                s_logger.warn("Failed to start vpc as a part of VPC " + vpc + " restart process");
                restartRequired = true;
                return false;
            }
            s_logger.debug("VPC " + vpc + " was restarted successfully");
            return true;
        } finally {
            s_logger.debug("Updating VPC " + vpc + " with restartRequired=" + restartRequired);
            VpcVO vo = _vpcDao.findById(vpcId);
            vo.setRestartRequired(restartRequired);
            _vpcDao.update(vpc.getId(), vo);
        }  
    }
    
    @Override
    public List<DomainRouterVO> getVpcRouters(long vpcId) {
        return _routerDao.listByVpcId(vpcId);
    }

    @Override
    public PrivateGateway getVpcPrivateGateway(long id) {
        VpcGateway gateway = _vpcGatewayDao.findById(id);

        if (gateway == null || gateway.getType() != VpcGateway.Type.Private) {
            return null;
        }
        return getPrivateGatewayProfile(gateway);
    }
    
    @Override
    public VpcGateway getVpcGateway(long id) {
        return _vpcGatewayDao.findById(id);
    }

    protected PrivateGateway getPrivateGatewayProfile(VpcGateway gateway) {
        Network network = _ntwkModel.getNetwork(gateway.getNetworkId());
        return new PrivateGatewayProfile(gateway, network.getPhysicalNetworkId());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PRIVATE_GATEWAY_CREATE, eventDescription = "creating vpc private gateway", create=true)
    public PrivateGateway createVpcPrivateGateway(long vpcId, Long physicalNetworkId, String vlan, String ipAddress, 
            String gateway, String netmask, long gatewayOwnerId) throws ResourceAllocationException, 
            ConcurrentOperationException, InsufficientCapacityException {
        
        //Validate parameters
        Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject("vpc", vpcId, "VPC");
            throw ex;
        }

        //Validate physical network
        if (physicalNetworkId == null) {
            List<? extends PhysicalNetwork> pNtwks = _ntwkModel.getPhysicalNtwksSupportingTrafficType(vpc.getZoneId(), TrafficType.Guest);
            if (pNtwks.isEmpty() || pNtwks.size() != 1) {
                throw new InvalidParameterValueException("Physical network can't be determined; pass physical network id");
            }
            physicalNetworkId = pNtwks.get(0).getId();
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        s_logger.debug("Creating Private gateway for VPC " + vpc);
        //1) create private network
        String networkName = "vpc-" + vpc.getName() + "-privateNetwork";
        Network privateNtwk = _ntwkSvc.createPrivateNetwork(networkName, networkName, physicalNetworkId, 
                vlan, ipAddress, null, gateway, netmask, gatewayOwnerId, vpcId);
        
        //2) create gateway entry
        VpcGatewayVO gatewayVO = new VpcGatewayVO(ipAddress, VpcGateway.Type.Private, vpcId, privateNtwk.getDataCenterId(),
                privateNtwk.getId(), vlan, gateway, netmask, vpc.getAccountId(), vpc.getDomainId());
        _vpcGatewayDao.persist(gatewayVO);
        
        s_logger.debug("Created vpc gateway entry " + gatewayVO);
        
        txn.commit();
        
        return getVpcPrivateGateway(gatewayVO.getId());     
    }


    @Override
    public PrivateGateway applyVpcPrivateGateway(long gatewayId, boolean destroyOnFailure) throws ConcurrentOperationException, ResourceUnavailableException {
        VpcGatewayVO vo = _vpcGatewayDao.findById(gatewayId);

        boolean success = true;
        try {
            PrivateGateway gateway = getVpcPrivateGateway(gatewayId);
            for (VpcProvider provider: getVpcElements()){
                if(!provider.createPrivateGateway(gateway)){
                    success = false;
                }
            }
            if (success) {
                s_logger.debug("Private gateway " + gateway + " was applied succesfully on the backend");
                if (vo.getState() != VpcGateway.State.Ready) {
                    vo.setState(VpcGateway.State.Ready);
                    _vpcGatewayDao.update(vo.getId(), vo);
                    s_logger.debug("Marke gateway " + gateway + " with state " + VpcGateway.State.Ready);
                }
                return getVpcPrivateGateway(gatewayId);
            } else {
                s_logger.warn("Private gateway " + gateway + " failed to apply on the backend");
                return null;
            }
        } finally {
            //do cleanup
            if (!success) {
                if (destroyOnFailure) {
                    s_logger.debug("Destroying private gateway " + vo + " that failed to start");
                    if (deleteVpcPrivateGateway(gatewayId)) {
                        s_logger.warn("Successfully destroyed vpc " + vo + " that failed to start");
                    } else {
                        s_logger.warn("Failed to destroy vpc " + vo + " that failed to start");
                    }
                }    
            }
        }      
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PRIVATE_GATEWAY_DELETE, eventDescription = "deleting private gateway")
    @DB
    public boolean deleteVpcPrivateGateway(long gatewayId) throws ConcurrentOperationException, ResourceUnavailableException {
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        VpcGatewayVO gatewayVO = _vpcGatewayDao.acquireInLockTable(gatewayId);
        if (gatewayVO == null || gatewayVO.getType() != VpcGateway.Type.Private) {
            throw new ConcurrentOperationException("Unable to lock gateway " + gatewayId);
        }

        try { 
            //don't allow to remove gateway when there are static routes associated with it
            long routeCount = _staticRouteDao.countRoutesByGateway(gatewayVO.getId());
            if (routeCount > 0) {
                throw new CloudRuntimeException("Can't delete private gateway " + gatewayVO + " as it has " + routeCount +
                        " static routes applied. Remove the routes first");
            }
            
            gatewayVO.setState(VpcGateway.State.Deleting);
            _vpcGatewayDao.update(gatewayVO.getId(), gatewayVO);
            s_logger.debug("Marked gateway " + gatewayVO + " with state " + VpcGateway.State.Deleting);
            
            txn.commit();

            //1) delete the gateway on the backend
            PrivateGateway gateway = getVpcPrivateGateway(gatewayId);
            for (VpcProvider provider: getVpcElements()){
                if (provider.deletePrivateGateway(gateway)) {
                    s_logger.debug("Private gateway " + gateway + " was applied succesfully on the backend");
                } else {
                    s_logger.warn("Private gateway " + gateway + " failed to apply on the backend");
                    return false;
                }
            }
            
            //2) Delete private gateway from the DB
            return deletePrivateGatewayFromTheDB(gateway);
            
        } finally {
            if (gatewayVO != null) {
                _vpcGatewayDao.releaseFromLockTable(gatewayId);
            }
        } 
    }
    
    @DB
    protected boolean deletePrivateGatewayFromTheDB(PrivateGateway gateway) {
        //check if there are ips allocted in the network
        long networkId = gateway.getNetworkId();
        
        boolean deleteNetwork = true;
        List<PrivateIpVO> privateIps = _privateIpDao.listByNetworkId(networkId);
        if (privateIps.size() > 1 || !privateIps.get(0).getIpAddress().equalsIgnoreCase(gateway.getIp4Address())) {
            s_logger.debug("Not removing network id=" + gateway.getNetworkId() + " as it has private ip addresses for other gateways");
            deleteNetwork = false;
        }
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        PrivateIpVO ip = _privateIpDao.findByIpAndVpcId(gateway.getVpcId(), gateway.getIp4Address());
        if (ip != null) {
            _privateIpDao.remove(ip.getId());
            s_logger.debug("Deleted private ip " + ip);
        }
        
        if (deleteNetwork) {
            User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
            Account owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
            ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
            _ntwkMgr.destroyNetwork(networkId, context);
            s_logger.debug("Deleted private network id=" + networkId);
        }
        
        _vpcGatewayDao.remove(gateway.getId());
        s_logger.debug("Deleted private gateway " + gateway);
        
        txn.commit();
        return true;
    }

    @Override
    public Pair<List<PrivateGateway>, Integer> listPrivateGateway(ListPrivateGatewaysCmd cmd) {
        String ipAddress = cmd.getIpAddress();
        String vlan = cmd.getVlan();
        Long vpcId = cmd.getVpcId();
        Long id = cmd.getId();
        Boolean isRecursive = cmd.isRecursive();
        Boolean listAll = cmd.listAll();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String state = cmd.getState();
        Long projectId = cmd.getProjectId();

        Filter searchFilter = new Filter(VpcGatewayVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, 
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject,
                listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        SearchBuilder<VpcGatewayVO> sb = _vpcGatewayDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        
        if (vlan != null) {
            SearchBuilder<NetworkVO> ntwkSearch = _ntwkDao.createSearchBuilder();
            ntwkSearch.and("vlan", ntwkSearch.entity().getBroadcastUri(), SearchCriteria.Op.EQ);
            sb.join("networkSearch", ntwkSearch, sb.entity().getNetworkId(), ntwkSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<VpcGatewayVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);  
        
        if (id != null) {
            sc.addAnd("id", Op.EQ, id);
        }
        
        if (ipAddress != null) {
            sc.addAnd("ip4Address", Op.EQ, ipAddress);
        }
        
        if (state != null) {
            sc.addAnd("state", Op.EQ, state);
        }
        
        if (vpcId != null) {
            sc.addAnd("vpcId", Op.EQ, vpcId);
        }
        
        if (vlan != null) {
            sc.setJoinParameters("networkSearch", "vlan", BroadcastDomainType.Vlan.toUri(vlan));
        }
       
        Pair<List<VpcGatewayVO>, Integer> vos = _vpcGatewayDao.searchAndCount(sc, searchFilter);
        List<PrivateGateway> privateGtws = new ArrayList<PrivateGateway>(vos.first().size());
        for (VpcGateway vo : vos.first()) {
            privateGtws.add(getPrivateGatewayProfile(vo));
        }
        
        return new Pair<List<PrivateGateway>, Integer>(privateGtws, vos.second());
    }
    
    @Override
    public StaticRoute getStaticRoute(long routeId) {
        return _staticRouteDao.findById(routeId);
    }

    @Override
    public boolean applyStaticRoutes(long vpcId) throws ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        List<? extends StaticRoute> routes = _staticRouteDao.listByVpcId(vpcId);
        return applyStaticRoutes(routes, caller, true);
    }

    protected boolean applyStaticRoutes(List<? extends StaticRoute> routes, Account caller, boolean updateRoutesInDB) throws ResourceUnavailableException {
        boolean success = true;
        List<StaticRouteProfile> staticRouteProfiles = new ArrayList<StaticRouteProfile>(routes.size());
        Map<Long, VpcGateway> gatewayMap = new HashMap<Long, VpcGateway>();
        for (StaticRoute route : routes) {
            VpcGateway gateway = gatewayMap.get(route.getVpcGatewayId());
            if (gateway == null) {
                gateway = _vpcGatewayDao.findById(route.getVpcGatewayId());
                gatewayMap.put(gateway.getId(), gateway);
            }
            staticRouteProfiles.add(new StaticRouteProfile(route, gateway));
        }
        if (!applyStaticRoutes(staticRouteProfiles)) {
            s_logger.warn("Routes are not completely applied");
            return false;
        } else {
            if (updateRoutesInDB) {
                for (StaticRoute route : routes) {
                    if (route.getState() == StaticRoute.State.Revoke) {
                        _staticRouteDao.remove(route.getId());
                        s_logger.debug("Removed route " + route + " from the DB");
                    } else if (route.getState() == StaticRoute.State.Add) {
                        StaticRouteVO ruleVO = _staticRouteDao.findById(route.getId());
                        ruleVO.setState(StaticRoute.State.Active);
                        _staticRouteDao.update(ruleVO.getId(), ruleVO);
                        s_logger.debug("Marked route " + route + " with state " + StaticRoute.State.Active);
                    }
                }
            }            
        }

        return success;
    }   
    
    protected boolean applyStaticRoutes(List<StaticRouteProfile> routes) throws ResourceUnavailableException{
        if (routes.isEmpty()) {
            s_logger.debug("No static routes to apply");
            return true;
        }
        Vpc vpc = getVpc(routes.get(0).getVpcId());
        
        s_logger.debug("Applying static routes for vpc " + vpc);
        String staticNatProvider = _vpcSrvcDao.getProviderForServiceInVpc(vpc.getId(), Service.StaticNat);

        for (VpcProvider provider: getVpcElements()){
            if (!(provider instanceof StaticNatServiceProvider && provider.getName().equalsIgnoreCase(staticNatProvider))) {
                continue;
            }

            if (provider.applyStaticRoutes(vpc, routes)) {
                s_logger.debug("Applied static routes for vpc " + vpc);
            } else {
                s_logger.warn("Failed to apply static routes for vpc " + vpc);
                return false;
            }
        }
        
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_STATIC_ROUTE_DELETE, eventDescription = "deleting static route")
    public boolean revokeStaticRoute(long routeId) throws ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        
        StaticRouteVO route = _staticRouteDao.findById(routeId);
        if (route == null) {
            throw new InvalidParameterValueException("Unable to find static route by id");
        }
        
        _accountMgr.checkAccess(caller, null, false, route);

        markStaticRouteForRevoke(route, caller);

        return applyStaticRoutes(route.getVpcId());
    }
    
    @DB
    protected boolean revokeStaticRoutesForVpc(long vpcId, Account caller) throws ResourceUnavailableException {
        //get all static routes for the vpc
        List<StaticRouteVO> routes = _staticRouteDao.listByVpcId(vpcId);
        s_logger.debug("Found " + routes.size() + " to revoke for the vpc " + vpcId);
        if (!routes.isEmpty()) {
            //mark all of them as revoke
            Transaction txn = Transaction.currentTxn();
            txn.start();
            for (StaticRouteVO route : routes) {
                markStaticRouteForRevoke(route, caller);
            }
            txn.commit();
            return applyStaticRoutes(vpcId);
        }
        
        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_STATIC_ROUTE_CREATE, eventDescription = "creating static route", create=true)
    public StaticRoute createStaticRoute(long gatewayId, String cidr) throws NetworkRuleConflictException {
        Account caller = UserContext.current().getCaller();
        
        //parameters validation
        VpcGateway gateway = _vpcGatewayDao.findById(gatewayId);
        if (gateway == null) {
            throw new InvalidParameterValueException("Invalid gateway id is given");
        }
        
        if (gateway.getState() != VpcGateway.State.Ready) {
            throw new InvalidParameterValueException("Gateway is not in the " + VpcGateway.State.Ready + " state: " + gateway.getState());
        }
        
        Vpc vpc = getActiveVpc(gateway.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Can't add static route to VPC that is being deleted");
        }
        _accountMgr.checkAccess(caller, null, false, vpc);
        
        if (!NetUtils.isValidCIDR(cidr)){
            throw new InvalidParameterValueException("Invalid format for cidr " + cidr);
        }
        
        //validate the cidr
        //1) CIDR should be outside of VPC cidr for guest networks
        if (NetUtils.isNetworksOverlap(vpc.getCidr(), cidr)) {
            throw new InvalidParameterValueException("CIDR should be outside of VPC cidr " + vpc.getCidr());
        }
        
        //2) CIDR should be outside of link-local cidr
        if (NetUtils.isNetworksOverlap(vpc.getCidr(), NetUtils.getLinkLocalCIDR())) {
            throw new InvalidParameterValueException("CIDR should be outside of link local cidr " + NetUtils.getLinkLocalCIDR());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        StaticRouteVO newRoute = new StaticRouteVO(gateway.getId(), cidr, vpc.getId(), vpc.getAccountId(), vpc.getDomainId());
        s_logger.debug("Adding static route " + newRoute);
        newRoute = _staticRouteDao.persist(newRoute);
        
        detectRoutesConflict(newRoute);

        if (!_staticRouteDao.setStateToAdd(newRoute)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRoute);
        }
        UserContext.current().setEventDetails("Static route Id: " + newRoute.getId());
        
        txn.commit();

        return newRoute;
    }

    @Override
    public Pair<List<? extends StaticRoute>, Integer> listStaticRoutes(ListStaticRoutesCmd cmd) {
        Long id = cmd.getId();
        Long gatewayId = cmd.getGatewayId();
        Long vpcId = cmd.getVpcId();
        Long domainId = cmd.getDomainId();
        Boolean isRecursive = cmd.isRecursive();
        Boolean listAll = cmd.listAll();
        String accountName = cmd.getAccountName();
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();
        Map<String, String> tags = cmd.getTags();
        Long projectId = cmd.getProjectId();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, 
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject,
                listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(StaticRouteVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchBuilder<StaticRouteVO> sb = _staticRouteDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("vpcGatewayId", sb.entity().getVpcGatewayId(), SearchCriteria.Op.EQ);
        
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
        
        SearchCriteria<StaticRouteVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);  
        
        if (id != null) {
            sc.addAnd("id", Op.EQ, id);
        }
        
        if (vpcId != null) {
            sc.addAnd("vpcId", Op.EQ, vpcId);
        }
        
        if (gatewayId != null) {
            sc.addAnd("vpcGatewayId", Op.EQ, gatewayId);
        }
        
        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.StaticRoute.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }   
        }
        
        Pair<List<StaticRouteVO>, Integer> result = _staticRouteDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends StaticRoute>, Integer>(result.first(), result.second());
    }
    
    protected void detectRoutesConflict(StaticRoute newRoute) throws NetworkRuleConflictException {
        List<? extends StaticRoute> routes = _staticRouteDao.listByGatewayIdAndNotRevoked(newRoute.getVpcGatewayId());
        assert (routes.size() >= 1) : "For static routes, we now always first persist the route and then check for " +
                "network conflicts so we should at least have one rule at this point.";
        
        for (StaticRoute route : routes) {
            if (route.getId() == newRoute.getId()) {
                continue; // Skips my own route.
            }
            
            if (NetUtils.isNetworksOverlap(route.getCidr(), newRoute.getCidr())) {
                throw new NetworkRuleConflictException("New static route cidr conflicts with existing route " + route);
            }
        }
    }
    
    protected void markStaticRouteForRevoke(StaticRouteVO route, Account caller) {
        s_logger.debug("Revoking static route " + route);
        if (caller != null) {
            _accountMgr.checkAccess(caller, null, false, route);
        }

        if (route.getState() == StaticRoute.State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a static route that is still in stage state so just removing it: " + route);
            }
            _staticRouteDao.remove(route.getId());
        } else if (route.getState() == StaticRoute.State.Add || route.getState() == StaticRoute.State.Active) {
            route.setState(StaticRoute.State.Revoke);
            _staticRouteDao.update(route.getId(), route);
            s_logger.debug("Marked static route " + route + " with state " + StaticRoute.State.Revoke);
        }
    }
    
    protected class VpcCleanupTask implements Runnable {
        @Override
        public void run() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("VpcCleanup");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }

                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }

                Transaction txn = null;
                try {
                    txn = Transaction.open(Transaction.CLOUD_DB);

                    // Cleanup inactive VPCs
                    List<VpcVO> inactiveVpcs = _vpcDao.listInactiveVpcs();
                    s_logger.info("Found " + inactiveVpcs.size() + " removed VPCs to cleanup");
                    for (VpcVO vpc : inactiveVpcs) {
                        s_logger.debug("Cleaning up " + vpc);
                        destroyVpc(vpc, _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM); 
                    }
                } catch (Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    if (txn != null) {
                        txn.close();
                    }
                    lock.unlock();
                }
            } catch (Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }
    
    @Override
    public VpcGateway getPrivateGatewayForVpc(long vpcId) {
        return _vpcGatewayDao.getPrivateGatewayForVpc(vpcId);
    }

    
    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "associating Ip", async = true)
    public IpAddress associateIPToVpc(long ipId, long vpcId) throws ResourceAllocationException, ResourceUnavailableException, 
    InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        Account owner = null;

        IpAddress ipToAssoc = _ntwkModel.getIp(ipId);
        if (ipToAssoc != null) {
            _accountMgr.checkAccess(caller, null, true, ipToAssoc);
            owner = _accountMgr.getAccount(ipToAssoc.getAllocatedToAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        Vpc vpc = getVpc(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid VPC id provided");
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, owner, vpc);

        boolean isSourceNat = false;
        if (getExistingSourceNatInVpc(owner.getId(), vpcId) == null) {
            isSourceNat = true;
        }

        s_logger.debug("Associating ip " + ipToAssoc + " to vpc " + vpc);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        IPAddressVO ip = _ipAddressDao.findById(ipId);
        //update ip address with networkId
        ip.setVpcId(vpcId);
        ip.setSourceNat(isSourceNat);
        _ipAddressDao.update(ipId, ip);

        //mark ip as allocated
        _ntwkMgr.markPublicIpAsAllocated(ip);
        txn.commit();

        s_logger.debug("Successfully assigned ip " + ipToAssoc + " to vpc " + vpc);

        return _ipAddressDao.findById(ipId);
    }
    
    
    @Override
    public void unassignIPFromVpcNetwork(long ipId, long networkId) {
        IPAddressVO ip = _ipAddressDao.findById(ipId);
        if (ipUsedInVpc(ip)) {
            return;
        }

        if (ip == null || ip.getVpcId() == null) {
            return;
        }

        s_logger.debug("Releasing VPC ip address " + ip + " from vpc network id=" + networkId);

        long  vpcId = ip.getVpcId();
        boolean success = false;
        try {
            //unassign ip from the VPC router
            success = _ntwkMgr.applyIpAssociations(_ntwkModel.getNetwork(networkId), true);
        } catch (ResourceUnavailableException ex) {
            throw new CloudRuntimeException("Failed to apply ip associations for network id=" + networkId + 
                    " as a part of unassigning ip " + ipId + " from vpc", ex);
        }

        if (success) {
            ip.setAssociatedWithNetworkId(null);
            _ipAddressDao.update(ipId, ip);
            s_logger.debug("IP address " + ip + " is no longer associated with the network inside vpc id=" + vpcId);
        } else {
            throw new CloudRuntimeException("Failed to apply ip associations for network id=" + networkId + 
                    " as a part of unassigning ip " + ipId + " from vpc");
        }
        s_logger.debug("Successfully released VPC ip address " + ip + " back to VPC pool ");
    }
    
    @Override
    public boolean ipUsedInVpc(IpAddress ip) {
        return (ip != null && ip.getVpcId() != null && 
                (ip.isOneToOneNat() || !_firewallDao.listByIp(ip.getId()).isEmpty()));
    }
    
    @DB
    @Override
    public Network createVpcGuestNetwork(long ntwkOffId, String name, String displayText, String gateway, 
            String cidr, String vlanId, String networkDomain, Account owner, Long domainId,
            PhysicalNetwork pNtwk, long zoneId, ACLType aclType, Boolean subdomainAccess, long vpcId, Account caller) 
                    throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException {

        Vpc vpc = getActiveVpc(vpcId);

        if (vpc == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC ");
            ex.addProxyObject("vpc", vpcId, "VPC");
            throw ex;
        }
        _accountMgr.checkAccess(caller, null, false, vpc);
        
        if (networkDomain == null) {
            networkDomain = vpc.getNetworkDomain();
        }
        
        if (vpc.getZoneId() != zoneId) {
            throw new InvalidParameterValueException("New network doesn't belong to vpc zone");
        }
        
        //1) Validate if network can be created for VPC
        validateNtkwOffForVpc(ntwkOffId, cidr, networkDomain, owner, vpc, null, gateway);

        //2) Create network
        Network guestNetwork = _ntwkMgr.createGuestNetwork(ntwkOffId, name, displayText, gateway, cidr, vlanId, 
                networkDomain, owner, domainId, pNtwk, zoneId, aclType, subdomainAccess, vpcId, null, null);

        return guestNetwork;
    }
    
    
    protected IPAddressVO getExistingSourceNatInVpc(long ownerId, long vpcId) {

        List<IPAddressVO> addrs = listPublicIpsAssignedToVpc(ownerId, true, vpcId);
        
        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            return null;
        } else {
            // Account already has ip addresses
            for (IPAddressVO addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = addr;
                    return sourceNatIp;
                }
            }

            assert (sourceNatIp != null) : "How do we get a bunch of ip addresses but none of them are source nat? " +
            "account=" + ownerId + "; vpcId=" + vpcId;
        } 

        return sourceNatIp;
    }
    
    protected List<IPAddressVO> listPublicIpsAssignedToVpc(long accountId, Boolean sourceNat, long vpcId) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("vpcId", vpcId);

        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }
    
    
    @Override
    public PublicIp assignSourceNatIpAddressToVpc(Account owner, Vpc vpc) throws InsufficientAddressCapacityException, ConcurrentOperationException {
        long dcId = vpc.getZoneId();

        IPAddressVO sourceNatIp = getExistingSourceNatInVpc(owner.getId(), vpc.getId());

        PublicIp ipToReturn = null;

        if (sourceNatIp != null) {
            ipToReturn = PublicIp.createFromAddrAndVlan(sourceNatIp, _vlanDao.findById(sourceNatIp.getVlanId()));
        } else {
            ipToReturn = _ntwkMgr.assignDedicateIpAddress(owner, null, vpc.getId(), dcId, true);
        }

        return ipToReturn;
    }


    @Override
    public Network updateVpcGuestNetwork(long networkId, String name, String displayText, Account callerAccount, 
            User callerUser, String domainSuffix, Long ntwkOffId, Boolean changeCidr, String guestVmCidr) {
        NetworkVO network = _ntwkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Couldn't find network by id");
        }
        //perform below validation if the network is vpc network
        if (network.getVpcId() != null && ntwkOffId != null) {
            Vpc vpc = getVpc(network.getVpcId());
            validateNtkwOffForVpc(ntwkOffId, null, null, null, vpc, networkId, null);
        }
        
        return _ntwkSvc.updateGuestNetwork(networkId, name, displayText, callerAccount, callerUser, domainSuffix,
                ntwkOffId, changeCidr, guestVmCidr);
    }

    @Override
    public List<HypervisorType> getSupportedVpcHypervisors() {
        List<HypervisorType> hTypes = new ArrayList<HypervisorType>();
        hTypes.add(HypervisorType.XenServer);
        hTypes.add(HypervisorType.VMware);
        hTypes.add(HypervisorType.KVM);
        return hTypes;
    }

    private List<Provider> getVpcProviders(long vpcId) {
        List<String> providerNames = _vpcSrvcDao.getDistinctProviders(vpcId);
        Map<String, Provider> providers = new HashMap<String, Provider>();
        for (String providerName : providerNames) {
            if(!providers.containsKey(providerName)){
                providers.put(providerName, Network.Provider.getProvider(providerName));
            }
        }

        return new ArrayList<Provider>(providers.values());
    }
}
