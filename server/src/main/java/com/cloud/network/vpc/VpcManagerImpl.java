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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.vpc.CreatePrivateGatewayByAdminCmd;
import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.UpdateVPCOfferingCmd;
import org.apache.cloudstack.api.command.user.vpc.CreatePrivateGatewayCmd;
import org.apache.cloudstack.api.command.user.vpc.CreateVPCCmd;
import org.apache.cloudstack.api.command.user.vpc.ListPrivateGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpc.ListStaticRoutesCmd;
import org.apache.cloudstack.api.command.user.vpc.ListVPCOfferingsCmd;
import org.apache.cloudstack.api.command.user.vpc.RestartVPCCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.query.QueryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.VpcOfferingJoinDao;
import com.cloud.api.query.vo.VpcOfferingJoinVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
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
import com.cloud.network.IpAddressManager;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
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
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.vpc.VpcOffering.State;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingDetailsDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public class VpcManagerImpl extends ManagerBase implements VpcManager, VpcProvisioningService, VpcService {
    private static final Logger s_logger = Logger.getLogger(VpcManagerImpl.class);

    public static final String SERVICE = "service";
    public static final String CAPABILITYTYPE = "capabilitytype";
    public static final String CAPABILITYVALUE = "capabilityvalue";
    public static final String TRUE_VALUE = "true";
    public static final String FALSE_VALUE = "false";

    @Inject
    EntityManager _entityMgr;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcOfferingJoinDao vpcOfferingJoinDao;
    @Inject
    VpcOfferingDetailsDao vpcOfferingDetailsDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffSvcMapDao;
    @Inject
    VpcDao vpcDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _ntwkDao;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    NetworkOrchestrationService _ntwkMgr;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    NetworkService _ntwkSvc;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOffServiceDao;
    @Inject
    VpcOfferingServiceMapDao _vpcOffServiceDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    VlanDao _vlanDao = null;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    VpcServiceMapDao _vpcSrvcDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    NetworkACLDao _networkAclDao;
    @Inject
    NetworkACLManager _networkAclMgr;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    VpcVirtualNetworkApplianceManager _routerService;
    @Inject
    DomainRouterDao routerDao;
    @Inject
    DomainDao domainDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NicDao nicDao;
    @Inject
    AlertManager alertManager;
    @Inject
    CommandSetupHelper commandSetupHelper;
    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper networkHelper;

    @Inject
    private VpcPrivateGatewayTransactionCallable vpcTxCallable;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("VpcChecker"));
    private List<VpcProvider> vpcElements = null;
    private final List<Service> nonSupportedServices = Arrays.asList(Service.SecurityGroup, Service.Firewall);
    private final List<Provider> supportedProviders = Arrays.asList(Provider.VPCVirtualRouter, Provider.NiciraNvp, Provider.InternalLbVm, Provider.Netscaler,
            Provider.JuniperContrailVpcRouter, Provider.Ovs, Provider.BigSwitchBcf, Provider.ConfigDrive);

    int _cleanupInterval;
    int _maxNetworks;
    SearchBuilder<IPAddressVO> IpAddressSearch;

    protected final List<HypervisorType> hTypes = new ArrayList<HypervisorType>();

    @PostConstruct
    protected void setupSupportedVpcHypervisorsList() {
        hTypes.add(HypervisorType.XenServer);
        hTypes.add(HypervisorType.VMware);
        hTypes.add(HypervisorType.KVM);
        hTypes.add(HypervisorType.Simulator);
        hTypes.add(HypervisorType.LXC);
        hTypes.add(HypervisorType.Hyperv);
        hTypes.add(HypervisorType.Ovm3);
    }

    private void checkVpcDns(VpcOffering vpcOffering, String ip4Dns1, String ip4Dns2, String ip6Dns1, String ip6Dns2) {
        if (ObjectUtils.anyNotNull(ip4Dns1, ip4Dns2, ip6Dns1, ip6Dns2) && !areServicesSupportedByVpcOffering(vpcOffering.getId(), Service.Dns)) {
            throw new InvalidParameterValueException("DNS can not be specified for VPCs with offering that do not support DNS service");
        }
        if (!_vpcOffDao.isIpv6Supported(vpcOffering.getId()) && !org.apache.commons.lang3.StringUtils.isAllBlank(ip6Dns1, ip6Dns2)) {
            throw new InvalidParameterValueException("IPv6 DNS can be specified for IPv6 enabled VPC");
        }
        _ntwkModel.verifyIp4DnsPair(ip4Dns1, ip4Dns2);
        _ntwkModel.verifyIp6DnsPair(ip6Dns1, ip6Dns2);
    }

    @Override
    @DB
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        // configure default vpc offering
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {

                if (_vpcOffDao.findByUniqueName(VpcOffering.defaultVPCOfferingName) == null) {
                    s_logger.debug("Creating default VPC offering " + VpcOffering.defaultVPCOfferingName);

                    final Map<Service, Set<Provider>> svcProviderMap = new HashMap<Service, Set<Provider>>();
                    final Set<Provider> defaultProviders = new HashSet<Provider>();
                    defaultProviders.add(Provider.VPCVirtualRouter);
                    for (final Service svc : getSupportedServices()) {
                        if (svc == Service.Lb) {
                            final Set<Provider> lbProviders = new HashSet<Provider>();
                            lbProviders.add(Provider.VPCVirtualRouter);
                            lbProviders.add(Provider.InternalLbVm);
                            svcProviderMap.put(svc, lbProviders);
                        } else {
                            svcProviderMap.put(svc, defaultProviders);
                        }
                    }
                    createVpcOffering(VpcOffering.defaultVPCOfferingName, VpcOffering.defaultVPCOfferingName, svcProviderMap, true, State.Enabled, null, false, false, false);
                }

                // configure default vpc offering with Netscaler as LB Provider
                if (_vpcOffDao.findByUniqueName(VpcOffering.defaultVPCNSOfferingName) == null) {
                    s_logger.debug("Creating default VPC offering with Netscaler as LB Provider" + VpcOffering.defaultVPCNSOfferingName);
                    final Map<Service, Set<Provider>> svcProviderMap = new HashMap<Service, Set<Provider>>();
                    final Set<Provider> defaultProviders = new HashSet<Provider>();
                    defaultProviders.add(Provider.VPCVirtualRouter);
                    for (final Service svc : getSupportedServices()) {
                        if (svc == Service.Lb) {
                            final Set<Provider> lbProviders = new HashSet<Provider>();
                            lbProviders.add(Provider.Netscaler);
                            lbProviders.add(Provider.InternalLbVm);
                            svcProviderMap.put(svc, lbProviders);
                        } else {
                            svcProviderMap.put(svc, defaultProviders);
                        }
                    }
                    createVpcOffering(VpcOffering.defaultVPCNSOfferingName, VpcOffering.defaultVPCNSOfferingName, svcProviderMap, false, State.Enabled, null, false, false, false);

                }

                if (_vpcOffDao.findByUniqueName(VpcOffering.redundantVPCOfferingName) == null) {
                    s_logger.debug("Creating Redundant VPC offering " + VpcOffering.redundantVPCOfferingName);

                    final Map<Service, Set<Provider>> svcProviderMap = new HashMap<Service, Set<Provider>>();
                    final Set<Provider> defaultProviders = new HashSet<Provider>();
                    defaultProviders.add(Provider.VPCVirtualRouter);
                    for (final Service svc : getSupportedServices()) {
                        if (svc == Service.Lb) {
                            final Set<Provider> lbProviders = new HashSet<Provider>();
                            lbProviders.add(Provider.VPCVirtualRouter);
                            lbProviders.add(Provider.InternalLbVm);
                            svcProviderMap.put(svc, lbProviders);
                        } else {
                            svcProviderMap.put(svc, defaultProviders);
                        }
                    }
                    createVpcOffering(VpcOffering.redundantVPCOfferingName, VpcOffering.redundantVPCOfferingName, svcProviderMap, true, State.Enabled, null, false, false, true);
                }
            }
        });

        final Map<String, String> configs = _configDao.getConfiguration(params);
        final String value = configs.get(Config.VpcCleanupInterval.key());
        _cleanupInterval = NumbersUtil.parseInt(value, 60 * 60); // 1 hour

        final String maxNtwks = configs.get(Config.VpcMaxNetworks.key());
        _maxNetworks = NumbersUtil.parseInt(maxNtwks, 3); // max=3 is default

        IpAddressSearch = _ipAddressDao.createSearchBuilder();
        IpAddressSearch.and("accountId", IpAddressSearch.entity().getAllocatedToAccountId(), Op.EQ);
        IpAddressSearch.and("dataCenterId", IpAddressSearch.entity().getDataCenterId(), Op.EQ);
        IpAddressSearch.and("vpcId", IpAddressSearch.entity().getVpcId(), Op.EQ);
        IpAddressSearch.and("associatedWithNetworkId", IpAddressSearch.entity().getAssociatedWithNetworkId(), Op.EQ);
        final SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch
                .join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);
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
    public List<? extends Network> getVpcNetworks(final long vpcId) {
        return _ntwkDao.listByVpc(vpcId);
    }

    @Override
    public VpcOffering getVpcOffering(final long vpcOffId) {
        return _vpcOffDao.findById(vpcOffId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_CREATE, eventDescription = "creating vpc offering", create = true)
    public VpcOffering createVpcOffering(CreateVPCOfferingCmd cmd) {
        final String vpcOfferingName = cmd.getVpcOfferingName();
        final String displayText = cmd.getDisplayText();
        final List<String> supportedServices = cmd.getSupportedServices();
        final Map<String, List<String>> serviceProviderList = cmd.getServiceProviders();
        final Map serviceCapabilityList = cmd.getServiceCapabilityList();
        final NetUtils.InternetProtocol internetProtocol = NetUtils.InternetProtocol.fromValue(cmd.getInternetProtocol());
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        final boolean enable = cmd.getEnable();
        // check if valid domain
        if (CollectionUtils.isNotEmpty(cmd.getDomainIds())) {
            for (final Long domainId: cmd.getDomainIds()) {
                if (domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(cmd.getZoneIds())) {
            for (Long zoneId : cmd.getZoneIds()) {
                if (_dcDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        if (serviceOfferingId != null) {
            _ntwkSvc.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(serviceOfferingId);
        }

        return createVpcOffering(vpcOfferingName, displayText, supportedServices,
                serviceProviderList, serviceCapabilityList, internetProtocol, serviceOfferingId,
                domainIds, zoneIds, (enable ? State.Enabled : State.Disabled));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_CREATE, eventDescription = "creating vpc offering", create = true)
    public VpcOffering createVpcOffering(final String name, final String displayText, final List<String> supportedServices, final Map<String, List<String>> serviceProviders,
                                         final Map serviceCapabilityList, final NetUtils.InternetProtocol internetProtocol, final Long serviceOfferingId, List<Long> domainIds, List<Long> zoneIds, State state) {

        if (!Ipv6Service.Ipv6OfferingCreationEnabled.value() && !(internetProtocol == null || NetUtils.InternetProtocol.IPv4.equals(internetProtocol))) {
            throw new InvalidParameterValueException(String.format("Configuration %s needs to be enabled for creating IPv6 supported VPC offering", Ipv6Service.Ipv6OfferingCreationEnabled.key()));
        }

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);

        final Map<Network.Service, Set<Network.Provider>> svcProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        final Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();
        defaultProviders.add(Provider.VPCVirtualRouter);
        // Just here for 4.1, replaced by commit 836ce6c1 in newer versions
        final Set<Network.Provider> sdnProviders = new HashSet<Network.Provider>();
        sdnProviders.add(Provider.NiciraNvp);
        sdnProviders.add(Provider.JuniperContrailVpcRouter);

        boolean sourceNatSvc = false;
        boolean firewallSvs = false;
        // populate the services first
        for (final String serviceName : supportedServices) {
            // validate if the service is supported
            final Service service = Network.Service.getService(serviceName);
            if (service == null || nonSupportedServices.contains(service)) {
                throw new InvalidParameterValueException("Service " + serviceName + " is not supported in VPC");
            }

            if (service == Service.Connectivity) {
                s_logger.debug("Applying Connectivity workaround, setting provider to NiciraNvp");
                svcProviderMap.put(service, sdnProviders);
            } else {
                svcProviderMap.put(service, defaultProviders);
            }
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

        if (serviceProviders != null) {
            for (final Entry<String, List<String>> serviceEntry : serviceProviders.entrySet()) {
                final Network.Service service = Network.Service.getService(serviceEntry.getKey());
                if (svcProviderMap.containsKey(service)) {
                    final Set<Provider> providers = new HashSet<Provider>();
                    for (final String prvNameStr : serviceEntry.getValue()) {
                        // check if provider is supported
                        final Network.Provider provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }

                        providers.add(provider);
                    }
                    svcProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceEntry.getKey() + " is not enabled for the network " + "offering, can't add a provider to it");
                }
            }
        }

        // add gateway provider (if sourceNat provider is enabled)
        final Set<Provider> sourceNatServiceProviders = svcProviderMap.get(Service.SourceNat);
        if (CollectionUtils.isNotEmpty(sourceNatServiceProviders)) {
            svcProviderMap.put(Service.Gateway, sourceNatServiceProviders);
        }

        validateConnectivtyServiceCapabilities(svcProviderMap.get(Service.Connectivity), serviceCapabilityList);

        final boolean supportsDistributedRouter = isVpcOfferingSupportsDistributedRouter(serviceCapabilityList);
        final boolean offersRegionLevelVPC = isVpcOfferingForRegionLevelVpc(serviceCapabilityList);
        final boolean redundantRouter = isVpcOfferingRedundantRouter(serviceCapabilityList);
        final VpcOfferingVO offering = createVpcOffering(name, displayText, svcProviderMap, false, state, serviceOfferingId, supportsDistributedRouter, offersRegionLevelVPC,
                redundantRouter);

        if (offering != null) {
            List<VpcOfferingDetailsVO> detailsVO = new ArrayList<>();
            for (Long domainId : filteredDomainIds) {
                detailsVO.add(new VpcOfferingDetailsVO(offering.getId(), ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
            }
            if (CollectionUtils.isNotEmpty(zoneIds)) {
                for (Long zoneId : zoneIds) {
                    detailsVO.add(new VpcOfferingDetailsVO(offering.getId(), ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }
            if (internetProtocol != null) {
                detailsVO.add(new VpcOfferingDetailsVO(offering.getId(), ApiConstants.INTERNET_PROTOCOL, String.valueOf(internetProtocol), true));
            }
            if (!detailsVO.isEmpty()) {
                vpcOfferingDetailsDao.saveDetails(detailsVO);
            }
        }
        CallContext.current().setEventDetails(" Id: " + offering.getId() + " Name: " + name);
        CallContext.current().putContextParameter(VpcOffering.class, offering.getUuid());

        return offering;
    }

    @DB
    protected VpcOfferingVO createVpcOffering(final String name, final String displayText, final Map<Network.Service, Set<Network.Provider>> svcProviderMap,
                                              final boolean isDefault, final State state, final Long serviceOfferingId, final boolean supportsDistributedRouter, final boolean offersRegionLevelVPC,
                                              final boolean redundantRouter) {

        return Transaction.execute(new TransactionCallback<VpcOfferingVO>() {
            @Override
            public VpcOfferingVO doInTransaction(final TransactionStatus status) {
                // create vpc offering object
                VpcOfferingVO offering = new VpcOfferingVO(name, displayText, isDefault, serviceOfferingId, supportsDistributedRouter, offersRegionLevelVPC, redundantRouter);

                if (state != null) {
                    offering.setState(state);
                }
                s_logger.debug("Adding vpc offering " + offering);
                offering = _vpcOffDao.persist(offering);
                // populate services and providers
                if (svcProviderMap != null) {
                    for (final Network.Service service : svcProviderMap.keySet()) {
                        final Set<Provider> providers = svcProviderMap.get(service);
                        if (providers != null && !providers.isEmpty()) {
                            for (final Network.Provider provider : providers) {
                                final VpcOfferingServiceMapVO offService = new VpcOfferingServiceMapVO(offering.getId(), service, provider);
                                _vpcOffSvcMapDao.persist(offService);
                                s_logger.trace("Added service for the vpc offering: " + offService + " with provider " + provider.getName());
                            }
                        } else {
                            throw new InvalidParameterValueException("Provider is missing for the VPC offering service " + service.getName());
                        }
                    }
                }

                return offering;
            }
        });
    }

    protected void checkCapabilityPerServiceProvider(final Set<Provider> providers, final Capability capability, final Service service) {
        // TODO Shouldn't it fail it there are no providers?
        if (providers != null) {
            for (final Provider provider : providers) {
                final NetworkElement element = _ntwkModel.getElementImplementingProvider(provider.getName());
                final Map<Service, Map<Capability, String>> capabilities = element.getCapabilities();
                if (capabilities != null && !capabilities.isEmpty()) {
                    final Map<Capability, String> connectivityCapabilities = capabilities.get(service);
                    if (connectivityCapabilities == null || connectivityCapabilities != null && !connectivityCapabilities.keySet().contains(capability)) {
                        throw new InvalidParameterValueException(String.format("Provider %s does not support %s  capability.", provider.getName(), capability.getName()));
                    }
                }
            }
        }
    }

    private void validateConnectivtyServiceCapabilities(final Set<Provider> providers, final Map serviceCapabilitystList) {
        if (serviceCapabilitystList != null && !serviceCapabilitystList.isEmpty()) {
            final Collection serviceCapabilityCollection = serviceCapabilitystList.values();
            final Iterator iter = serviceCapabilityCollection.iterator();

            while (iter.hasNext()) {
                final HashMap<String, String> svcCapabilityMap = (HashMap<String, String>) iter.next();
                Capability capability = null;
                final String svc = svcCapabilityMap.get(SERVICE);
                final String capabilityName = svcCapabilityMap.get(CAPABILITYTYPE);
                final String capabilityValue = svcCapabilityMap.get(CAPABILITYVALUE);
                if (capabilityName != null) {
                    capability = Capability.getCapability(capabilityName);
                }

                if (capability == null || capabilityValue == null) {
                    throw new InvalidParameterValueException("Invalid capability:" + capabilityName + " capability value:" + capabilityValue);
                }
                final Service usedService = Service.getService(svc);

                checkCapabilityPerServiceProvider(providers, capability, usedService);

                if (!capabilityValue.equalsIgnoreCase(TRUE_VALUE) && !capabilityValue.equalsIgnoreCase(FALSE_VALUE)) {
                    throw new InvalidParameterValueException("Invalid Capability value:" + capabilityValue + " specified.");
                }
            }
        }
    }

    private boolean findCapabilityForService(final Map serviceCapabilitystList, final Capability capability, final Service service) {
        boolean foundCapability = false;
        if (serviceCapabilitystList != null && !serviceCapabilitystList.isEmpty()) {
            final Iterator iter = serviceCapabilitystList.values().iterator();
            while (iter.hasNext()) {
                final HashMap<String, String> currentCapabilityMap = (HashMap<String, String>) iter.next();
                final String currentCapabilityService = currentCapabilityMap.get(SERVICE);
                final String currentCapabilityName = currentCapabilityMap.get(CAPABILITYTYPE);
                final String currentCapabilityValue = currentCapabilityMap.get(CAPABILITYVALUE);

                if (currentCapabilityName == null || currentCapabilityService == null || currentCapabilityValue == null) {
                    throw new InvalidParameterValueException(String.format("Invalid capability with name %s, value %s and service %s", currentCapabilityName,
                            currentCapabilityValue, currentCapabilityService));
                }

                if (currentCapabilityName.equalsIgnoreCase(capability.getName())) {
                    foundCapability = currentCapabilityValue.equalsIgnoreCase(TRUE_VALUE);

                    if (!currentCapabilityService.equalsIgnoreCase(service.getName())) {
                        throw new InvalidParameterValueException(String.format("Invalid Service: %s specified. Capability %s can be specified only for service %s",
                                currentCapabilityService, service.getName(), currentCapabilityName));
                    }

                    break;
                }
            }
        }
        return foundCapability;
    }

    private boolean isVpcOfferingForRegionLevelVpc(final Map serviceCapabilitystList) {
        return findCapabilityForService(serviceCapabilitystList, Capability.RegionLevelVpc, Service.Connectivity);
    }

    private boolean isVpcOfferingSupportsDistributedRouter(final Map serviceCapabilitystList) {
        return findCapabilityForService(serviceCapabilitystList, Capability.DistributedRouter, Service.Connectivity);
    }

    private boolean isVpcOfferingRedundantRouter(final Map serviceCapabilitystList) {
        return findCapabilityForService(serviceCapabilitystList, Capability.RedundantRouter, Service.SourceNat);
    }

    @Override
    public Vpc getActiveVpc(final long vpcId) {
        return vpcDao.getActiveVpcById(vpcId);
    }

    @Override
    public Map<Service, Set<Provider>> getVpcOffSvcProvidersMap(final long vpcOffId) {
        final Map<Service, Set<Provider>> serviceProviderMap = new HashMap<Service, Set<Provider>>();
        final List<VpcOfferingServiceMapVO> map = _vpcOffSvcMapDao.listByVpcOffId(vpcOffId);

        for (final VpcOfferingServiceMapVO instance : map) {
            final Service service = Service.getService(instance.getService());
            Set<Provider> providers;
            providers = serviceProviderMap.get(service);
            if (providers == null) {
                providers = new HashSet<Provider>();
            }
            providers.add(Provider.getProvider(instance.getProvider()));
            serviceProviderMap.put(service, providers);
        }

        return serviceProviderMap;
    }

    private void verifyDomainId(Long domainId, Account caller) {
        if (domainId == null) {
            return;
        }
        Domain domain = _entityMgr.findById(Domain.class, domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
        }
        if (!domainDao.isChildDomain(caller.getDomainId(), domainId)) {
            throw new InvalidParameterValueException(String.format("Unable to list VPC offerings for domain: %s as caller does not have access for it", domain.getUuid()));
        }
    }

    @Override
    public Pair<List<? extends VpcOffering>, Integer> listVpcOfferings(ListVPCOfferingsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        final Long id = cmd.getId();
        final String name = cmd.getVpcOffName();
        final String displayText = cmd.getDisplayText();
        final List<String> supportedServicesStr = cmd.getSupportedServices();
        final Boolean isDefault = cmd.getIsDefault();
        final String keyword = cmd.getKeyword();
        final String state = cmd.getState();
        final Long startIndex = cmd.getStartIndex();
        final Long pageSizeVal = cmd.getPageSizeVal();
        final Long domainId = cmd.getDomainId();
        final Long zoneId = cmd.getZoneId();
        final Filter searchFilter = new Filter(VpcOfferingJoinVO.class, "sortKey", QueryService.SortKeyAscending.value(), null, null);
        searchFilter.addOrderBy(VpcOfferingJoinVO.class, "id", true);
        final SearchCriteria<VpcOfferingJoinVO> sc = vpcOfferingJoinDao.createSearchCriteria();

        verifyDomainId(domainId, caller);

        if (keyword != null) {
            final SearchCriteria<VpcOfferingJoinVO> ssc = vpcOfferingJoinDao.createSearchCriteria();
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

        if (zoneId != null) {
            SearchBuilder<VpcOfferingJoinVO> sb = vpcOfferingJoinDao.createSearchBuilder();
            sb.and("zoneId", sb.entity().getZoneId(), Op.FIND_IN_SET);
            sb.or("zId", sb.entity().getZoneId(), Op.NULL);
            sb.done();
            SearchCriteria<VpcOfferingJoinVO> zoneSC = sb.create();
            zoneSC.setParameters("zoneId", String.valueOf(zoneId));
            sc.addAnd("zoneId", SearchCriteria.Op.SC, zoneSC);
        }

        final List<VpcOfferingJoinVO> offerings = vpcOfferingJoinDao.search(sc, searchFilter);

        // Remove offerings that are not associated with caller's domain or domainId passed
        if ((!Account.Type.ADMIN.equals(caller.getType()) || domainId != null) && CollectionUtils.isNotEmpty(offerings)) {
            ListIterator<VpcOfferingJoinVO> it = offerings.listIterator();
            while (it.hasNext()) {
                VpcOfferingJoinVO offering = it.next();
                if (org.apache.commons.lang3.StringUtils.isEmpty(offering.getDomainId())) {
                    continue;
                }
                if (!domainDao.domainIdListContainsAccessibleDomain(offering.getDomainId(), caller, domainId)) {
                    it.remove();
                }
            }
        }
        // filter by supported services
        final boolean listBySupportedServices = supportedServicesStr != null && !supportedServicesStr.isEmpty() && !offerings.isEmpty();

        if (listBySupportedServices) {
            final List<VpcOfferingJoinVO> supportedOfferings = new ArrayList<>();
            Service[] supportedServices = null;

            if (listBySupportedServices) {
                supportedServices = new Service[supportedServicesStr.size()];
                int i = 0;
                for (final String supportedServiceStr : supportedServicesStr) {
                    final Service service = Service.getService(supportedServiceStr);
                    if (service == null) {
                        throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                    } else {
                        supportedServices[i] = service;
                    }
                    i++;
                }
            }

            for (final VpcOfferingJoinVO offering : offerings) {
                if (areServicesSupportedByVpcOffering(offering.getId(), supportedServices)) {
                    supportedOfferings.add(offering);
                }
            }

            final List<? extends VpcOffering> wPagination = StringUtils.applyPagination(supportedOfferings, startIndex, pageSizeVal);
            if (wPagination != null) {
                return new Pair<>(wPagination, supportedOfferings.size());
            }
            return new Pair<List<? extends VpcOffering>, Integer>(supportedOfferings, supportedOfferings.size());
        } else {
            final List<? extends VpcOffering> wPagination = StringUtils.applyPagination(offerings, startIndex, pageSizeVal);
            if (wPagination != null) {
                return new Pair<>(wPagination, offerings.size());
            }
            return new Pair<List<? extends VpcOffering>, Integer>(offerings, offerings.size());
        }
    }

    protected boolean areServicesSupportedByVpcOffering(final long vpcOffId, final Service... services) {
        return _vpcOffSvcMapDao.areServicesSupportedByVpcOffering(vpcOffId, services);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_DELETE, eventDescription = "deleting vpc offering")
    public boolean deleteVpcOffering(final long offId) {
        CallContext.current().setEventDetails(" Id: " + offId);

        // Verify vpc offering id
        final VpcOfferingVO offering = _vpcOffDao.findById(offId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find vpc offering " + offId);
        }

        // Don't allow to delete default vpc offerings
        if (offering.isDefault() == true) {
            throw new InvalidParameterValueException("Default network offering can't be deleted");
        }

        // don't allow to delete vpc offering if it's in use by existing vpcs
        // (the offering can be disabled though)
        final int vpcCount = vpcDao.getVpcCountByOfferingId(offId);
        if (vpcCount > 0) {
            throw new InvalidParameterValueException("Can't delete vpc offering " + offId + " as its used by " + vpcCount + " vpcs. "
                    + "To make the network offering unavailable, disable it");
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
        return updateVpcOfferingInternal(vpcOffId, vpcOfferingName, displayText, state, null, null, null);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_OFFERING_UPDATE, eventDescription = "updating vpc offering")
    public VpcOffering updateVpcOffering(final UpdateVPCOfferingCmd cmd) {
        final Long offeringId = cmd.getId();
        final String vpcOfferingName = cmd.getVpcOfferingName();
        final String displayText = cmd.getDisplayText();
        final String state = cmd.getState();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        final Integer sortKey = cmd.getSortKey();

        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_dcDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        return updateVpcOfferingInternal(offeringId, vpcOfferingName, displayText, state, sortKey, domainIds, zoneIds);
    }

    private VpcOffering updateVpcOfferingInternal(long vpcOffId, String vpcOfferingName, String displayText, String state, Integer sortKey, final List<Long> domainIds, final List<Long> zoneIds) {
        CallContext.current().setEventDetails(" Id: " + vpcOffId);

        // Verify input parameters
        final VpcOfferingVO offeringToUpdate = _vpcOffDao.findById(vpcOffId);
        if (offeringToUpdate == null) {
            throw new InvalidParameterValueException("Unable to find vpc offering " + vpcOffId);
        }

        List<Long> existingDomainIds = vpcOfferingDetailsDao.findDomainIds(vpcOffId);
        Collections.sort(existingDomainIds);

        List<Long> existingZoneIds = vpcOfferingDetailsDao.findZoneIds(vpcOffId);
        Collections.sort(existingZoneIds);


        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);
        Collections.sort(filteredDomainIds);

        List<Long> filteredZoneIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            filteredZoneIds.addAll(zoneIds);
        }
        Collections.sort(filteredZoneIds);

        final boolean updateNeeded = vpcOfferingName != null || displayText != null || state != null || sortKey != null;

        final VpcOfferingVO offering = _vpcOffDao.createForUpdate(vpcOffId);

        if (updateNeeded) {
            if (vpcOfferingName != null) {
                offering.setName(vpcOfferingName);
            }
            if (displayText != null) {
                offering.setDisplayText(displayText);
            }
            if (state != null) {
                boolean validState = false;
                for (final VpcOffering.State st : VpcOffering.State.values()) {
                    if (st.name().equalsIgnoreCase(state)) {
                        validState = true;
                        offering.setState(st);
                    }
                }
                if (!validState) {
                    throw new InvalidParameterValueException("Incorrect state value: " + state);
                }
            }
            if (sortKey != null) {
                offering.setSortKey(sortKey);
            }

            if (!_vpcOffDao.update(vpcOffId, offering)) {
                return  null;
            }
        }
        List<VpcOfferingDetailsVO> detailsVO = new ArrayList<>();
        if(!filteredDomainIds.equals(existingDomainIds) || !filteredZoneIds.equals(existingZoneIds)) {
            SearchBuilder<VpcOfferingDetailsVO> sb = vpcOfferingDetailsDao.createSearchBuilder();
            sb.and("offeringId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.and("detailName", sb.entity().getName(), SearchCriteria.Op.EQ);
            sb.done();
            SearchCriteria<VpcOfferingDetailsVO> sc = sb.create();
            sc.setParameters("offeringId", String.valueOf(vpcOffId));
            if(!filteredDomainIds.equals(existingDomainIds)) {
                sc.setParameters("detailName", ApiConstants.DOMAIN_ID);
                vpcOfferingDetailsDao.remove(sc);
                for (Long domainId : filteredDomainIds) {
                    detailsVO.add(new VpcOfferingDetailsVO(vpcOffId, ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
                }
            }
            if(!filteredZoneIds.equals(existingZoneIds)) {
                sc.setParameters("detailName", ApiConstants.ZONE_ID);
                vpcOfferingDetailsDao.remove(sc);
                for (Long zoneId : filteredZoneIds) {
                    detailsVO.add(new VpcOfferingDetailsVO(vpcOffId, ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }
        }
        if (!detailsVO.isEmpty()) {
            for (VpcOfferingDetailsVO detailVO : detailsVO) {
                vpcOfferingDetailsDao.persist(detailVO);
            }
        }
        s_logger.debug("Updated VPC offeirng id=" + vpcOffId);
        return _vpcOffDao.findById(vpcOffId);
    }

    @Override
    public List<Long> getVpcOfferingDomains(Long vpcOfferingId) {
        final VpcOffering offeringHandle = _entityMgr.findById(VpcOffering.class, vpcOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find VPC offering " + vpcOfferingId);
        }
        return vpcOfferingDetailsDao.findDomainIds(vpcOfferingId);
    }

    @Override
    public List<Long> getVpcOfferingZones(Long vpcOfferingId) {
        final VpcOffering offeringHandle = _entityMgr.findById(VpcOffering.class, vpcOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find VPC offering " + vpcOfferingId);
        }
        return vpcOfferingDetailsDao.findZoneIds(vpcOfferingId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_CREATE, eventDescription = "creating vpc", create = true)
    public Vpc createVpc(final long zoneId, final long vpcOffId, final long vpcOwnerId, final String vpcName, final String displayText, final String cidr, String networkDomain,
            final String ip4Dns1, final String ip4Dns2, final String ip6Dns1, final String ip6Dns2, final Boolean displayVpc, Integer publicMtu) throws ResourceAllocationException {
        final Account caller = CallContext.current().getCallingAccount();
        final Account owner = _accountMgr.getAccount(vpcOwnerId);

        // Verify that caller can perform actions in behalf of vpc owner
        _accountMgr.checkAccess(caller, null, false, owner);

        // check resource limit
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.vpc);

        // Validate zone
        final DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id specified");
        }

        // Validate vpc offering
        final VpcOfferingVO vpcOff = _vpcOffDao.findById(vpcOffId);
        _accountMgr.checkAccess(owner, vpcOff, zone);
        if (vpcOff == null || vpcOff.getState() != State.Enabled) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find vpc offering in " + State.Enabled + " state by specified id");
            if (vpcOff == null) {
                ex.addProxyObject(String.valueOf(vpcOffId), "vpcOfferingId");
            } else {
                ex.addProxyObject(vpcOff.getUuid(), "vpcOfferingId");
            }
            throw ex;
        }

        final boolean isRegionLevelVpcOff = vpcOff.isOffersRegionLevelVPC();
        if (isRegionLevelVpcOff && networkDomain == null) {
            throw new InvalidParameterValueException("Network domain must be specified for region level VPC");
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            // See DataCenterVO.java
            final PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation since specified Zone is currently disabled");
            ex.addProxyObject(zone.getUuid(), "zoneId");
            throw ex;
        }

        if (networkDomain == null) {
            // 1) Get networkDomain from the corresponding account
            networkDomain = _ntwkModel.getAccountNetworkDomain(owner.getId(), zoneId);

            // 2) If null, generate networkDomain using domain suffix from the
            // global config variables
            if (networkDomain == null) {
                networkDomain = "cs" + Long.toHexString(owner.getId()) + NetworkOrchestrationService.GuestDomainSuffix.valueIn(zoneId);
            }
        }

        if (publicMtu > NetworkService.VRPublicInterfaceMtu.valueIn(zoneId)) {
            String subject = "Incorrect MTU configured on network for public interfaces of the VPC VR";
            String message = String.format("Configured MTU for network VR's public interfaces exceeds the upper limit " +
                            "enforced by zone level setting: %s. VR's public interfaces can be configured with a maximum MTU of %s", NetworkService.VRPublicInterfaceMtu.key(),
                    NetworkService.VRPublicInterfaceMtu.valueIn(zoneId));
            s_logger.warn(message);
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_VR_PUBLIC_IFACE_MTU, zoneId, null, subject, message);
            publicMtu = NetworkService.VRPublicInterfaceMtu.valueIn(zoneId);
        } else if (publicMtu < NetworkService.MINIMUM_MTU) {
            String subject = "Incorrect MTU configured on network for public interfaces of the VPC VR";
            String message = String.format("Configured MTU for network VR's public interfaces is lesser than the supported minim MTU of %s", NetworkService.MINIMUM_MTU);
            s_logger.warn(message);
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_VR_PUBLIC_IFACE_MTU, zoneId, null, subject, message);
            publicMtu = NetworkService.MINIMUM_MTU;
        }

        checkVpcDns(vpcOff, ip4Dns1, ip4Dns2, ip6Dns1, ip6Dns2);

        final boolean useDistributedRouter = vpcOff.isSupportsDistributedRouter();
        final VpcVO vpc = new VpcVO(zoneId, vpcName, displayText, owner.getId(), owner.getDomainId(), vpcOffId, cidr, networkDomain, useDistributedRouter, isRegionLevelVpcOff,
                vpcOff.isRedundantRouter(), ip4Dns1, ip4Dns2, ip6Dns1, ip6Dns2);
            vpc.setPublicMtu(publicMtu);

        return createVpc(displayVpc, vpc);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_CREATE, eventDescription = "creating vpc", create = true)
    public Vpc createVpc(CreateVPCCmd cmd) throws ResourceAllocationException {
        return createVpc(cmd.getZoneId(), cmd.getVpcOffering(), cmd.getEntityOwnerId(), cmd.getVpcName(), cmd.getDisplayText(),
            cmd.getCidr(), cmd.getNetworkDomain(), cmd.getIp4Dns1(), cmd.getIp4Dns2(), cmd.getIp6Dns1(),
            cmd.getIp6Dns2(), cmd.isDisplay(), cmd.getPublicMtu());
    }

    @DB
    protected Vpc createVpc(final Boolean displayVpc, final VpcVO vpc) {
        final String cidr = vpc.getCidr();
        // Validate CIDR
        if (!NetUtils.isValidIp4Cidr(cidr)) {
            throw new InvalidParameterValueException("Invalid CIDR specified " + cidr);
        }

        // cidr has to be RFC 1918 complient
        if (!NetUtils.validateGuestCidr(cidr)) {
            throw new InvalidParameterValueException("Guest Cidr " + cidr + " is not RFC1918 compliant");
        }

        // validate network domain
        if (!NetUtils.verifyDomainName(vpc.getNetworkDomain())) {
            throw new InvalidParameterValueException("Invalid network domain. Total length shouldn't exceed 190 chars. Each domain "
                    + "label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', " + "the digits '0' through '9', "
                    + "and the hyphen ('-'); can't start or end with \"-\"");
        }

        return Transaction.execute(new TransactionCallback<VpcVO>() {
            @Override
            public VpcVO doInTransaction(final TransactionStatus status) {
                if (displayVpc != null) {
                    vpc.setDisplay(displayVpc);
                }

                final VpcVO persistedVpc = vpcDao.persist(vpc, finalizeServicesAndProvidersForVpc(vpc.getZoneId(), vpc.getVpcOfferingId()));
                _resourceLimitMgr.incrementResourceCount(vpc.getAccountId(), ResourceType.vpc);
                s_logger.debug("Created VPC " + persistedVpc);
                CallContext.current().putContextParameter(Vpc.class, persistedVpc.getUuid());
                return persistedVpc;
            }
        });
    }

    private Map<String, List<String>> finalizeServicesAndProvidersForVpc(final long zoneId, final long offeringId) {
        final Map<String, List<String>> svcProviders = new HashMap<>();
        final List<VpcOfferingServiceMapVO> servicesMap = _vpcOffSvcMapDao.listByVpcOffId(offeringId);

        for (final VpcOfferingServiceMapVO serviceMap : servicesMap) {
            final String service = serviceMap.getService();
            String provider = serviceMap.getProvider();

            if (provider == null) {
                // Default to VPCVirtualRouter
                provider = Provider.VPCVirtualRouter.getName();
            }

            if (!_ntwkModel.isProviderEnabledInZone(zoneId, provider)) {
                throw new InvalidParameterValueException("Provider " + provider + " should be enabled in at least one physical network of the zone specified");
            }

            List<String> providers = null;
            if (svcProviders.get(service) == null) {
                providers = new ArrayList<String>();
            } else {
                providers = svcProviders.get(service);
            }
            providers.add(provider);
            svcProviders.put(service, providers);
        }

        return svcProviders;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_DELETE, eventDescription = "deleting VPC")
    public boolean deleteVpc(final long vpcId) throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext.current().setEventDetails(" Id: " + vpcId);
        final CallContext ctx = CallContext.current();

        // Verify vpc id
        final Vpc vpc = vpcDao.findById(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("unable to find VPC id=" + vpcId);
        }

        // verify permissions
        _accountMgr.checkAccess(ctx.getCallingAccount(), null, false, vpc);
        _resourceTagDao.removeByIdAndType(vpcId, ResourceObjectType.Vpc);

        return destroyVpc(vpc, ctx.getCallingAccount(), ctx.getCallingUserId());
    }

    @Override
    @DB
    public boolean destroyVpc(final Vpc vpc, final Account caller, final Long callerUserId) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Destroying vpc " + vpc);

        // don't allow to delete vpc if it's in use by existing non system
        // networks (system networks are networks of a private gateway of the
        // VPC,
        // and they will get removed as a part of VPC cleanup
        final int networksCount = _ntwkDao.getNonSystemNetworkCountByVpcId(vpc.getId());
        if (networksCount > 0) {
            throw new InvalidParameterValueException("Can't delete VPC " + vpc + " as its used by " + networksCount + " networks");
        }

        // mark VPC as inactive
        if (vpc.getState() != Vpc.State.Inactive) {
            s_logger.debug("Updating VPC " + vpc + " with state " + Vpc.State.Inactive + " as a part of vpc delete");
            final VpcVO vpcVO = vpcDao.findById(vpc.getId());
            vpcVO.setState(Vpc.State.Inactive);

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    vpcDao.update(vpc.getId(), vpcVO);

                    // decrement resource count
                    _resourceLimitMgr.decrementResourceCount(vpc.getAccountId(), ResourceType.vpc);
                }
            });
        }

        // shutdown VPC
        if (!shutdownVpc(vpc.getId())) {
            s_logger.warn("Failed to shutdown vpc " + vpc + " as a part of vpc destroy process");
            return false;
        }

        // cleanup vpc resources
        if (!cleanupVpcResources(vpc.getId(), caller, callerUserId)) {
            s_logger.warn("Failed to cleanup resources for vpc " + vpc);
            return false;
        }

        // update the instance with removed flag only when the cleanup is
        // executed successfully
        if (vpcDao.remove(vpc.getId())) {
            s_logger.debug("Vpc " + vpc + " is destroyed successfully");
            return true;
        } else {
            s_logger.warn("Vpc " + vpc + " failed to destroy");
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_UPDATE, eventDescription = "updating vpc")
    public Vpc updateVpc(final long vpcId, final String vpcName, final String displayText, final String customId, final Boolean displayVpc, Integer mtu) {
        CallContext.current().setEventDetails(" Id: " + vpcId);
        final Account caller = CallContext.current().getCallingAccount();

        // Verify input parameters
        final VpcVO vpcToUpdate = vpcDao.findById(vpcId);
        if (vpcToUpdate == null) {
            throw new InvalidParameterValueException("Unable to find vpc by id " + vpcId);
        }

        _accountMgr.checkAccess(caller, null, false, vpcToUpdate);

        final VpcVO vpc = vpcDao.createForUpdate(vpcId);

        if (vpcName != null) {
            vpc.setName(vpcName);
        }

        if (displayText != null) {
            vpc.setDisplayText(displayText);
        }

        if (customId != null) {
            vpc.setUuid(customId);
        }

        if (displayVpc != null) {
            vpc.setDisplay(displayVpc);
        }

        mtu = validateMtu(vpcToUpdate, mtu);
        if (mtu != null) {
            updateMtuOfVpcNetwork(vpcToUpdate, vpc, mtu);
        }

        if (vpcDao.update(vpcId, vpc)) {
            s_logger.debug("Updated VPC id=" + vpcId);
            return vpcDao.findById(vpcId);
        } else {
            return null;
        }
    }

    protected Integer validateMtu(VpcVO vpcToUpdate, Integer mtu) {
        Long zoneId = vpcToUpdate.getZoneId();
        if (mtu == null || NetworkService.AllowUsersToSpecifyVRMtu.valueIn(zoneId)) {
            return null;
        }
        if (mtu > NetworkService.VRPublicInterfaceMtu.valueIn(zoneId)) {
            String subject = "Incorrect MTU configured on network for public interfaces of the VPC VR";
            String message = String.format("Configured MTU for network VR's public interfaces exceeds the upper limit " +
                            "enforced by zone level setting: %s. VR's public interfaces can be configured with a maximum MTU of %s", NetworkService.VRPublicInterfaceMtu.key(),
                    NetworkService.VRPublicInterfaceMtu.valueIn(zoneId));
            s_logger.warn(message);
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_VR_PUBLIC_IFACE_MTU, zoneId, null, subject, message);
            mtu = NetworkService.VRPublicInterfaceMtu.valueIn(zoneId);
        } else if (mtu < NetworkService.MINIMUM_MTU) {
            String subject = "Incorrect MTU configured on network for public interfaces of the VPC VR";
            String message = String.format("Configured MTU for network VR's public interfaces is lesser than the minimum MTU of %s", NetworkService.MINIMUM_MTU );
            s_logger.warn(message);
            alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_VR_PUBLIC_IFACE_MTU, zoneId, null, subject, message);
            mtu = NetworkService.MINIMUM_MTU;
        }
        if (Objects.equals(mtu, vpcToUpdate.getPublicMtu())) {
            s_logger.info(String.format("Desired MTU of %s already configured on the VPC public interfaces", mtu));
            mtu = null;
        }
        return mtu;
    }

    protected void updateMtuOfVpcNetwork(VpcVO vpcToUpdate, VpcVO vpc, Integer mtu) {
        List<IPAddressVO> ipAddresses = _ipAddressDao.listByAssociatedVpc(vpcToUpdate.getId(), null);
        long vpcId = vpcToUpdate.getId();
        Set<IpAddressTO> ips = new HashSet<>(ipAddresses.size());
        for (IPAddressVO ip : ipAddresses) {
            VlanVO vlan = _vlanDao.findById(ip.getVlanId());
            String vlanNetmask = vlan.getVlanNetmask();
            IpAddressTO to = new IpAddressTO(ip.getAddress().addr(), mtu, vlanNetmask);
            ips.add(to);
        }

        if (!ips.isEmpty()) {
            boolean success = updateMtuOnVpcVr(vpcId, ips);
            if (success) {
                updateVpcMtu(ips, mtu);
                vpc.setPublicMtu(mtu);
                List<NetworkVO> vpcTierNetworks = _ntwkDao.listByVpc(vpcId);
                for (NetworkVO network : vpcTierNetworks) {
                    network.setPublicMtu(mtu);
                    _ntwkDao.update(network.getId(), network);
                }
                s_logger.info("Successfully update MTU of VPC network");
            } else {
                throw new CloudRuntimeException("Failed to update MTU on the network");
            }
        }
    }

    private void updateVpcMtu(Set<IpAddressTO> ips, Integer publicMtu) {
        for (IpAddressTO ipAddress : ips) {
            NicVO nicVO = nicDao.findByIpAddressAndVmType(ipAddress.getPublicIp(), VirtualMachine.Type.DomainRouter);
            if (nicVO != null) {
                nicVO.setMtu(publicMtu);
                nicDao.update(nicVO.getId(), nicVO);
            }
        }
    }

    protected boolean updateMtuOnVpcVr(Long vpcId, Set<IpAddressTO> ips) {
        boolean success = false;
        List<DomainRouterVO> routers = routerDao.listByVpcId(vpcId);
        for (DomainRouterVO router : routers) {
            Commands cmds = new Commands(Command.OnError.Stop);
            commandSetupHelper.setupUpdateNetworkCommands(router, ips, cmds);
            try {
                networkHelper.sendCommandsToRouter(router, cmds);
                final Answer updateNetworkAnswer = cmds.getAnswer("updateNetwork");
                if (!(updateNetworkAnswer != null && updateNetworkAnswer.getResult())) {
                    s_logger.warn("Unable to update guest network on router " + router);
                    throw new CloudRuntimeException("Failed to update guest network with new MTU");
                }
                success = true;
            } catch (ResourceUnavailableException e) {
                s_logger.error(String.format("Failed to update network MTU for router %s due to %s", router, e.getMessage()));
            }
        }
        return success;
    }

    @Override
    public Pair<List<? extends Vpc>, Integer> listVpcs(final Long id, final String vpcName, final String displayText, final List<String> supportedServicesStr, final String cidr,
                                                       final Long vpcOffId, final String state, final String accountName, Long domainId, final String keyword, final Long startIndex, final Long pageSizeVal,
                                                       final Long zoneId, Boolean isRecursive, final Boolean listAll, final Boolean restartRequired, final Map<String, String> tags, final Long projectId,
                                                       final Boolean display) {
        final Account caller = CallContext.current().getCallingAccount();
        final List<Long> permittedAccounts = new ArrayList<Long>();
        final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive,
                null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        final Filter searchFilter = new Filter(VpcVO.class, "created", false, null, null);

        final SearchBuilder<VpcVO> sb = vpcDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("displayText", sb.entity().getDisplayText(), SearchCriteria.Op.LIKE);
        sb.and("vpcOfferingId", sb.entity().getVpcOfferingId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("restartRequired", sb.entity().isRestartRequired(), SearchCriteria.Op.EQ);
        sb.and("cidr", sb.entity().getCidr(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            final SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        // now set the SC criteria...
        final SearchCriteria<VpcVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (keyword != null) {
            final SearchCriteria<VpcVO> ssc = vpcDao.createSearchCriteria();
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
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.Vpc.toString());
            for (final Map.Entry<String, String> entry : tags.entrySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), entry.getKey());
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), entry.getValue());
                count++;
            }
        }

        if (display != null) {
            sc.setParameters("display", display);
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

        final List<VpcVO> vpcs = vpcDao.search(sc, searchFilter);

        // filter by supported services
        final boolean listBySupportedServices = supportedServicesStr != null && !supportedServicesStr.isEmpty() && !vpcs.isEmpty();

        if (listBySupportedServices) {
            final List<VpcVO> supportedVpcs = new ArrayList<VpcVO>();
            Service[] supportedServices = null;

            if (listBySupportedServices) {
                supportedServices = new Service[supportedServicesStr.size()];
                int i = 0;
                for (final String supportedServiceStr : supportedServicesStr) {
                    final Service service = Service.getService(supportedServiceStr);
                    if (service == null) {
                        throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                    } else {
                        supportedServices[i] = service;
                    }
                    i++;
                }
            }

            for (final VpcVO vpc : vpcs) {
                if (areServicesSupportedByVpcOffering(vpc.getVpcOfferingId(), supportedServices)) {
                    supportedVpcs.add(vpc);
                }
            }

            final List<? extends Vpc> wPagination = StringUtils.applyPagination(supportedVpcs, startIndex, pageSizeVal);
            if (wPagination != null) {
                final Pair<List<? extends Vpc>, Integer> listWPagination = new Pair<List<? extends Vpc>, Integer>(wPagination, supportedVpcs.size());
                return listWPagination;
            }
            return new Pair<List<? extends Vpc>, Integer>(supportedVpcs, supportedVpcs.size());
        } else {
            final List<? extends Vpc> wPagination = StringUtils.applyPagination(vpcs, startIndex, pageSizeVal);
            if (wPagination != null) {
                final Pair<List<? extends Vpc>, Integer> listWPagination = new Pair<List<? extends Vpc>, Integer>(wPagination, vpcs.size());
                return listWPagination;
            }
            return new Pair<List<? extends Vpc>, Integer>(vpcs, vpcs.size());
        }
    }

    protected List<Service> getSupportedServices() {
        final List<Service> services = new ArrayList<Service>();
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
    public boolean startVpc(final long vpcId, final boolean destroyOnFailure) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();
        final User callerUser = _accountMgr.getActiveUser(ctx.getCallingUserId());

        // check if vpc exists
        final Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject(String.valueOf(vpcId), "VPC");
            throw ex;
        }

        // permission check
        _accountMgr.checkAccess(caller, null, false, vpc);

        final DataCenter dc = _entityMgr.findById(DataCenter.class, vpc.getZoneId());

        final DeployDestination dest = new DeployDestination(dc, null, null, null);
        final ReservationContext context = new ReservationContextImpl(null, null, callerUser, _accountMgr.getAccount(vpc.getAccountId()));

        boolean result = true;
        try {
            if (!startVpc(vpc, dest, context)) {
                s_logger.warn("Failed to start vpc " + vpc);
                result = false;
            }
        } catch (final Exception ex) {
            s_logger.warn("Failed to start vpc " + vpc + " due to ", ex);
            result = false;
        } finally {
            // do cleanup
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

    protected boolean startVpc(final Vpc vpc, final DeployDestination dest, final ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        // deploy provider
        boolean success = true;
        final List<Provider> providersToImplement = getVpcProviders(vpc.getId());
        for (final VpcProvider element : getVpcElements()) {
            if (providersToImplement.contains(element.getProvider())) {
                if (element.implementVpc(vpc, dest, context)) {
                    s_logger.debug("Vpc " + vpc + " has started successfully");
                } else {
                    s_logger.warn("Vpc " + vpc + " failed to start");
                    success = false;
                }
            }
        }
        return success;
    }

    @Override
    public boolean shutdownVpc(final long vpcId) throws ConcurrentOperationException, ResourceUnavailableException {
        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        // check if vpc exists
        final Vpc vpc = vpcDao.findById(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Unable to find vpc by id " + vpcId);
        }

        // permission check
        _accountMgr.checkAccess(caller, null, false, vpc);

        // shutdown provider
        s_logger.debug("Shutting down vpc " + vpc);
        // TODO - shutdown all vpc resources here (ACLs, gateways, etc)

        boolean success = true;
        final List<Provider> providersToImplement = getVpcProviders(vpc.getId());
        final ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(ctx.getCallingUserId()), caller);
        for (final VpcProvider element : getVpcElements()) {
            if (providersToImplement.contains(element.getProvider())) {
                if (element.shutdownVpc(vpc, context)) {
                    s_logger.debug("Vpc " + vpc + " has been shutdown successfully");
                } else {
                    s_logger.warn("Vpc " + vpc + " failed to shutdown");
                    success = false;
                }
            }
        }

        return success;
    }

    @DB
    @Override
    public void validateNtwkOffForNtwkInVpc(final Long networkId, final long newNtwkOffId, final String newCidr, final String newNetworkDomain, final Vpc vpc,
                                            final String gateway, final Account networkOwner, final Long aclId) {

        final NetworkOffering guestNtwkOff = _entityMgr.findById(NetworkOffering.class, newNtwkOffId);

        if (guestNtwkOff == null) {
            throw new InvalidParameterValueException("Can't find network offering by id specified");
        }

        if (networkId == null) {
            // 1) Validate attributes that has to be passed in when create new
            // guest network
            validateNewVpcGuestNetwork(newCidr, gateway, networkOwner, vpc, newNetworkDomain);
        }

        // 2) validate network offering attributes
        final List<Service> svcs = _ntwkModel.listNetworkOfferingServices(guestNtwkOff.getId());
        validateNtwkOffForVpc(guestNtwkOff, svcs);

        // 3) Check services/providers against VPC providers
        final List<NetworkOfferingServiceMapVO> networkProviders = _ntwkOffServiceDao.listByNetworkOfferingId(guestNtwkOff.getId());

        for (final NetworkOfferingServiceMapVO nSvcVO : networkProviders) {
            final String pr = nSvcVO.getProvider();
            final String service = nSvcVO.getService();
            if (_vpcOffServiceDao.findByServiceProviderAndOfferingId(service, pr, vpc.getVpcOfferingId()) == null) {
                throw new InvalidParameterValueException("Service/provider combination " + service + "/" + pr + " is not supported by VPC " + vpc);
            }
        }

        // 4) Only one network in the VPC can support public LB inside the VPC.
        // Internal LB can be supported on multiple VPC tiers
        if (_ntwkModel.areServicesSupportedByNetworkOffering(guestNtwkOff.getId(), Service.Lb) && guestNtwkOff.isPublicLb()) {
            final List<? extends Network> networks = getVpcNetworks(vpc.getId());
            for (final Network network : networks) {
                if (networkId != null && network.getId() == networkId.longValue()) {
                    // skip my own network
                    continue;
                } else {
                    final NetworkOffering otherOff = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                    // throw only if networks have different offerings with
                    // public lb support
                    if (_ntwkModel.areServicesSupportedInNetwork(network.getId(), Service.Lb) && otherOff.isPublicLb() && guestNtwkOff.getId() != otherOff.getId()) {
                        throw new InvalidParameterValueException("Public LB service is already supported " + "by network " + network + " in VPC " + vpc);
                    }
                }
            }
        }

        // 5) When aclId is provided, verify that ACLProvider is supported by
        // network offering
        if (aclId != null && !_ntwkModel.areServicesSupportedByNetworkOffering(guestNtwkOff.getId(), Service.NetworkACL)) {
            throw new InvalidParameterValueException("Cannot apply NetworkACL. Network Offering does not support NetworkACL service");
        }

    }

    @Override
    public void validateNtwkOffForVpc(final NetworkOffering guestNtwkOff, final List<Service> supportedSvcs) {
        // 1) in current release, only vpc provider is supported by Vpc offering
        final List<Provider> providers = _ntwkModel.getNtwkOffDistinctProviders(guestNtwkOff.getId());
        for (final Provider provider : providers) {
            if (!supportedProviders.contains(provider)) {
                throw new InvalidParameterValueException("Provider of type " + provider.getName() + " is not supported for network offerings that can be used in VPC");
            }
        }

        // 2) Only Isolated networks with Source nat service enabled can be
        // added to vpc
        if (!(guestNtwkOff.getGuestType() == GuestType.Isolated && supportedSvcs.contains(Service.SourceNat))) {

            throw new InvalidParameterValueException("Only network offerings of type " + GuestType.Isolated + " with service " + Service.SourceNat.getName()
                    + " are valid for vpc ");
        }

        // 3) No redundant router support
        /*
         * TODO This should have never been hardcoded like this in the first
         * place if (guestNtwkOff.getRedundantRouter()) { throw new
         * InvalidParameterValueException
         * ("No redunant router support when network belnogs to VPC"); }
         */

        // 4) Conserve mode should be off
        if (guestNtwkOff.isConserveMode()) {
            throw new InvalidParameterValueException("Only networks with conserve mode Off can belong to VPC");
        }

        // 5) If Netscaler is LB provider make sure it is in dedicated mode
        if (providers.contains(Provider.Netscaler) && !guestNtwkOff.isDedicatedLB()) {
            throw new InvalidParameterValueException("Netscaler only with Dedicated LB can belong to VPC");
        }
        return;
    }

    @DB
    protected void validateNewVpcGuestNetwork(final String cidr, final String gateway, final Account networkOwner, final Vpc vpc, final String networkDomain) {

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                final Vpc locked = vpcDao.acquireInLockTable(vpc.getId());
                if (locked == null) {
                    throw new CloudRuntimeException("Unable to acquire lock on " + vpc);
                }

                try {
                    // check number of active networks in vpc
                    if (_ntwkDao.countVpcNetworks(vpc.getId()) >= _maxNetworks) {
                        s_logger.warn(String.format("Failed to create a new VPC Guest Network because the number of networks per VPC has reached its maximum capacity of [%s]. Increase it by modifying global config [%s].", _maxNetworks, Config.VpcMaxNetworks));
                        throw new CloudRuntimeException(String.format("Number of networks per VPC cannot surpass [%s].", _maxNetworks));
                    }

                    // 1) CIDR is required
                    if (cidr == null) {
                        throw new InvalidParameterValueException("Gateway/netmask are required when create network for VPC");
                    }

                    // 2) Network cidr should be within vpcCidr
                    if (!NetUtils.isNetworkAWithinNetworkB(cidr, vpc.getCidr())) {
                        throw new InvalidParameterValueException("Network cidr " + cidr + " is not within vpc " + vpc + " cidr");
                    }

                    // 3) Network cidr shouldn't cross the cidr of other vpc
                    // network cidrs
                    final List<? extends Network> ntwks = _ntwkDao.listByVpc(vpc.getId());
                    for (final Network ntwk : ntwks) {
                        assert cidr != null : "Why the network cidr is null when it belongs to vpc?";

                        if (NetUtils.isNetworkAWithinNetworkB(ntwk.getCidr(), cidr) || NetUtils.isNetworkAWithinNetworkB(cidr, ntwk.getCidr())) {
                            throw new InvalidParameterValueException("Network cidr " + cidr + " crosses other network cidr " + ntwk + " belonging to the same vpc " + vpc);
                        }
                    }

                    // 4) vpc and network should belong to the same owner
                    if (vpc.getAccountId() != networkOwner.getId()) {
                        throw new InvalidParameterValueException("Vpc " + vpc + " owner is different from the network owner " + networkOwner);
                    }

                    // 5) network domain should be the same as VPC's
                    if (!networkDomain.equalsIgnoreCase(vpc.getNetworkDomain())) {
                        throw new InvalidParameterValueException("Network domain of the new network should match network" + " domain of vpc " + vpc);
                    }

                    // 6) gateway should never be equal to the cidr subnet
                    if (NetUtils.getCidrSubNet(cidr).equalsIgnoreCase(gateway)) {
                        throw new InvalidParameterValueException("Invalid gateway specified. It should never be equal to the cidr subnet value");
                    }
                } finally {
                    s_logger.debug("Releasing lock for " + locked);
                    vpcDao.releaseFromLockTable(locked.getId());
                }
            }
        });
    }

    public List<VpcProvider> getVpcElements() {
        if (vpcElements == null) {
            vpcElements = new ArrayList<VpcProvider>();
            vpcElements.add((VpcProvider) _ntwkModel.getElementImplementingProvider(Provider.VPCVirtualRouter.getName()));
            vpcElements.add((VpcProvider) _ntwkModel.getElementImplementingProvider(Provider.JuniperContrailVpcRouter.getName()));
        }

        if (vpcElements == null) {
            throw new CloudRuntimeException("Failed to initialize vpc elements");
        }

        return vpcElements;
    }

    @Override
    public List<? extends Vpc> getVpcsForAccount(final long accountId) {
        final List<Vpc> vpcs = new ArrayList<Vpc>();
        vpcs.addAll(vpcDao.listByAccountId(accountId));
        return vpcs;
    }

    public boolean cleanupVpcResources(final long vpcId, final Account caller, final long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        s_logger.debug("Cleaning up resources for vpc id=" + vpcId);
        boolean success = true;

        // 1) Remove VPN connections and VPN gateway
        s_logger.debug("Cleaning up existed site to site VPN connections");
        _s2sVpnMgr.cleanupVpnConnectionByVpc(vpcId);
        s_logger.debug("Cleaning up existed site to site VPN gateways");
        _s2sVpnMgr.cleanupVpnGatewayByVpc(vpcId);

        // 2) release all ip addresses
        final List<IPAddressVO> ipsToRelease = _ipAddressDao.listByAssociatedVpc(vpcId, null);
        s_logger.debug("Releasing ips for vpc id=" + vpcId + " as a part of vpc cleanup");
        for (final IPAddressVO ipToRelease : ipsToRelease) {
            if (ipToRelease.isPortable()) {
                // portable IP address are associated with owner, until
                // explicitly requested to be disassociated.
                // so as part of VPC clean up just break IP association with VPC
                ipToRelease.setVpcId(null);
                ipToRelease.setAssociatedWithNetworkId(null);
                _ipAddressDao.update(ipToRelease.getId(), ipToRelease);
                s_logger.debug("Portable IP address " + ipToRelease + " is no longer associated with any VPC");
            } else {
                success = success && _ipAddrMgr.disassociatePublicIpAddress(ipToRelease.getId(), callerUserId, caller);
                if (!success) {
                    s_logger.warn("Failed to cleanup ip " + ipToRelease + " as a part of vpc id=" + vpcId + " cleanup");
                }
            }
        }

        if (success) {
            s_logger.debug("Released ip addresses for vpc id=" + vpcId + " as a part of cleanup vpc process");
        } else {
            s_logger.warn("Failed to release ip addresses for vpc id=" + vpcId + " as a part of cleanup vpc process");
            // although it failed, proceed to the next cleanup step as it
            // doesn't depend on the public ip release
        }

        // 3) Delete all static route rules
        if (!revokeStaticRoutesForVpc(vpcId, caller)) {
            s_logger.warn("Failed to revoke static routes for vpc " + vpcId + " as a part of cleanup vpc process");
            return false;
        }

        // 4) Delete private gateways
        final List<PrivateGateway> gateways = getVpcPrivateGateways(vpcId);
        if (gateways != null) {
            for (final PrivateGateway gateway : gateways) {
                if (gateway != null) {
                    s_logger.debug("Deleting private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
                    if (!deleteVpcPrivateGateway(gateway.getId())) {
                        success = false;
                        s_logger.debug("Failed to delete private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
                    } else {
                        s_logger.debug("Deleted private gateway " + gateway + " as a part of vpc " + vpcId + " resources cleanup");
                    }
                }
            }
        }

        //5) Delete ACLs
        final SearchBuilder<NetworkACLVO> searchBuilder = _networkAclDao.createSearchBuilder();

        searchBuilder.and("vpcId", searchBuilder.entity().getVpcId(), Op.IN);
        final SearchCriteria<NetworkACLVO> searchCriteria = searchBuilder.create();
        searchCriteria.setParameters("vpcId", vpcId, 0);

        final Filter filter = new Filter(NetworkACLVO.class, "id", false, null, null);
        final Pair<List<NetworkACLVO>, Integer> aclsCountPair =  _networkAclDao.searchAndCount(searchCriteria, filter);

        final List<NetworkACLVO> acls = aclsCountPair.first();
        for (final NetworkACLVO networkAcl : acls) {
            if (networkAcl.getId() != NetworkACL.DEFAULT_ALLOW && networkAcl.getId() != NetworkACL.DEFAULT_DENY) {
                _networkAclMgr.deleteNetworkACL(networkAcl);
            }
        }

        VpcVO vpc = vpcDao.findById(vpcId);
        annotationDao.removeByEntityType(AnnotationService.EntityType.VPC.name(), vpc.getUuid());
        return success;
    }


    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_RESTART, eventDescription = "restarting vpc")
    public boolean restartVpc(final RestartVPCCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        final long vpcId = cmd.getId();
        final boolean cleanUp = cmd.getCleanup();
        final boolean makeRedundant = cmd.getMakeredundant();
        final boolean livePatch = cmd.getLivePatch();
        final User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
        return restartVpc(vpcId, cleanUp, makeRedundant, livePatch, callerUser);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_RESTART, eventDescription = "restarting vpc")
    public boolean restartVpc(Long vpcId, boolean cleanUp, boolean makeRedundant, boolean livePatch, User user) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject(String.valueOf(vpcId), "VPC");
            throw ex;
        }

        Account callerAccount = _accountMgr.getActiveAccountById(user.getAccountId());
        final ReservationContext context = new ReservationContextImpl(null, null, user, callerAccount);
        _accountMgr.checkAccess(callerAccount, null, false, vpc);

        s_logger.debug("Restarting VPC " + vpc);
        boolean restartRequired = false;
        try {
            boolean forceCleanup = cleanUp;
            if (!vpc.isRedundant() && makeRedundant) {
                final VpcOfferingVO redundantOffering = _vpcOffDao.findByUniqueName(VpcOffering.redundantVPCOfferingName);

                final VpcVO entity = vpcDao.findById(vpcId);
                entity.setRedundant(true);
                entity.setVpcOfferingId(redundantOffering.getId());

                // Change the VPC in order to get it updated after the end of
                // the restart procedure.
                if (vpcDao.update(vpc.getId(), entity)) {
                    vpc = entity;
                }

                // If the offering and redundant column are changing, force the
                // clean up.
                forceCleanup = true;
            }

            if (forceCleanup) {
                if (!rollingRestartVpc(vpc, context)) {
                    s_logger.warn("Failed to execute a rolling restart as a part of VPC " + vpc + " restart process");
                    restartRequired = true;
                    return false;
                }
                return true;
            }

            if (cleanUp) {
                livePatch = false;
            }

            restartVPCNetworks(vpcId, callerAccount, user, cleanUp, livePatch);

            s_logger.debug("Starting VPC " + vpc + " as a part of VPC restart process without cleanup");
            if (!startVpc(vpcId, false)) {
                s_logger.warn("Failed to start vpc as a part of VPC " + vpc + " restart process");
                restartRequired = true;
                return false;
            }
            s_logger.debug("VPC " + vpc + " was restarted successfully");
            return true;
        } finally {
            s_logger.debug("Updating VPC " + vpc + " with restartRequired=" + restartRequired);
            final VpcVO vo = vpcDao.findById(vpcId);
            vo.setRestartRequired(restartRequired);
            vpcDao.update(vpc.getId(), vo);
        }
    }

    private void restartVPCNetworks(long vpcId, Account callerAccount, User callerUser, boolean cleanUp, boolean livePatch) throws InsufficientCapacityException, ResourceUnavailableException {
        List<? extends Network> networks = _ntwkModel.listNetworksByVpc(vpcId);
        for (Network network: networks) {
            if (network.isRestartRequired() || livePatch) {
                _ntwkMgr.restartNetwork(network.getId(), callerAccount, callerUser, cleanUp, livePatch);
            }
        }
    }

    @Override
    public List<PrivateGateway> getVpcPrivateGateways(final long vpcId) {
        final List<VpcGatewayVO> gateways = _vpcGatewayDao.listByVpcIdAndType(vpcId, VpcGateway.Type.Private);

        if (gateways != null) {
            final List<PrivateGateway> pvtGateway = new ArrayList<PrivateGateway>();
            for (final VpcGatewayVO gateway : gateways) {
                pvtGateway.add(getPrivateGatewayProfile(gateway));
            }
            return pvtGateway;
        } else {
            return null;
        }
    }

    @Override
    public PrivateGateway getVpcPrivateGateway(final long id) {
        final VpcGateway gateway = _vpcGatewayDao.findById(id);

        if (gateway == null || gateway.getType() != VpcGateway.Type.Private) {
            return null;
        }
        return getPrivateGatewayProfile(gateway);
    }

    protected PrivateGateway getPrivateGatewayProfile(final VpcGateway gateway) {
        final Network network = _ntwkModel.getNetwork(gateway.getNetworkId());
        return new PrivateGatewayProfile(gateway, network.getPhysicalNetworkId());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PRIVATE_GATEWAY_CREATE, eventDescription = "creating VPC private gateway", create = true)
    public PrivateGateway createVpcPrivateGateway(CreatePrivateGatewayCmd command) throws ResourceAllocationException,
            ConcurrentOperationException, InsufficientCapacityException {
        long vpcId = command.getVpcId();
        String ipAddress = command.getIpAddress();
        String gateway = command.getGateway();
        String netmask = command.getNetmask();
        long gatewayOwnerId = command.getEntityOwnerId();
        Long networkOfferingId = command.getNetworkOfferingId();
        Boolean isSourceNat = command.getIsSourceNat();
        Long aclId = command.getAclId();
        Long associatedNetworkId = command.getAssociatedNetworkId();

        if (command instanceof CreatePrivateGatewayByAdminCmd) {
            Long physicalNetworkId = ((CreatePrivateGatewayByAdminCmd)command).getPhysicalNetworkId();
            String broadcastUri = ((CreatePrivateGatewayByAdminCmd)command).getBroadcastUri();
            Boolean bypassVlanOverlapCheck = ((CreatePrivateGatewayByAdminCmd)command).getBypassVlanOverlapCheck();
            return createVpcPrivateGateway(vpcId, physicalNetworkId, broadcastUri, ipAddress, gateway, netmask, gatewayOwnerId, networkOfferingId, isSourceNat, aclId, bypassVlanOverlapCheck, associatedNetworkId);
        }
        return createVpcPrivateGateway(vpcId, null, null, ipAddress, gateway, netmask, gatewayOwnerId, networkOfferingId, isSourceNat, aclId, false, associatedNetworkId);
    }

    private PrivateGateway createVpcPrivateGateway(final long vpcId, Long physicalNetworkId, final String broadcastUri, final String ipAddress, final String gateway,
                                                   final String netmask, final long gatewayOwnerId, final Long networkOfferingIdPassed, final Boolean isSourceNat, final Long aclId, final Boolean bypassVlanOverlapCheck, final Long associatedNetworkId) throws ResourceAllocationException,
            ConcurrentOperationException, InsufficientCapacityException {

        // Validate parameters
        final Vpc vpc = getActiveVpc(vpcId);
        if (vpc == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC by id specified");
            ex.addProxyObject(String.valueOf(vpcId), "VPC");
            throw ex;
        }

        NetworkOfferingVO ntwkOff = getVpcPrivateGatewayNetworkOffering(networkOfferingIdPassed, broadcastUri);
        final Long networkOfferingId = ntwkOff.getId();

        validateVpcPrivateGatewayAssociateNetworkId(ntwkOff, broadcastUri, associatedNetworkId, bypassVlanOverlapCheck);

        final Long dcId = vpc.getZoneId();
        physicalNetworkId = validateVpcPrivateGatewayPhysicalNetworkId(dcId, physicalNetworkId, associatedNetworkId, ntwkOff);
        PhysicalNetwork physNet = _entityMgr.findById(PhysicalNetwork.class, physicalNetworkId);;

        final Long physicalNetworkIdFinal = physicalNetworkId;
        final PhysicalNetwork physNetFinal = physNet;
        VpcGatewayVO gatewayVO = null;
        try {
            s_logger.debug("Creating Private gateway for VPC " + vpc);
            // 1) create private network unless it is existing and
            // lswitch'd
            Network privateNtwk = null;
            if (broadcastUri != null
                    && BroadcastDomainType.getSchemeValue(BroadcastDomainType.fromString(broadcastUri)) == BroadcastDomainType.Lswitch) {
                final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
                privateNtwk = _ntwkDao.getPrivateNetwork(broadcastUri, cidr, gatewayOwnerId, dcId, networkOfferingId, vpcId);
                // if the dcid is different we get no network so next we
                // try to create it
            }
            if (privateNtwk == null) {
                s_logger.info("creating new network for vpc " + vpc + " using broadcast uri: " + broadcastUri + " and associated network id: " + associatedNetworkId);
                final String networkName = "vpc-" + vpc.getName() + "-privateNetwork";
                privateNtwk = _ntwkSvc.createPrivateNetwork(networkName, networkName, physicalNetworkIdFinal, broadcastUri, ipAddress, null, gateway, netmask,
                        gatewayOwnerId, vpcId, isSourceNat, networkOfferingId, bypassVlanOverlapCheck, associatedNetworkId);
            } else { // create the nic/ip as createPrivateNetwork
                // doesn''t do that work for us now
                s_logger.info("found and using existing network for vpc " + vpc + ": " + broadcastUri);
                final DataCenterVO dc = _dcDao.lockRow(physNetFinal.getDataCenterId(), true);

                // add entry to private_ip_address table
                PrivateIpVO privateIp = _privateIpDao.findByIpAndSourceNetworkId(privateNtwk.getId(), ipAddress);
                if (privateIp != null) {
                    throw new InvalidParameterValueException("Private ip address " + ipAddress + " already used for private gateway" + " in zone "
                            + _entityMgr.findById(DataCenter.class, dcId).getName());
                }

                final Long mac = dc.getMacAddress();
                final Long nextMac = mac + 1;
                dc.setMacAddress(nextMac);

                s_logger.info("creating private ip address for vpc (" + ipAddress + ", " + privateNtwk.getId() + ", " + nextMac + ", " + vpcId + ", " + isSourceNat + ")");
                privateIp = new PrivateIpVO(ipAddress, privateNtwk.getId(), nextMac, vpcId, isSourceNat);
                _privateIpDao.persist(privateIp);

                _dcDao.update(dc.getId(), dc);
            }

            long networkAclId = NetworkACL.DEFAULT_DENY;
            if (aclId != null) {
                final NetworkACLVO aclVO = _networkAclDao.findById(aclId);
                if (aclVO == null) {
                    throw new InvalidParameterValueException("Invalid network acl id passed ");
                }
                if (aclVO.getVpcId() != vpcId && !(aclId == NetworkACL.DEFAULT_DENY || aclId == NetworkACL.DEFAULT_ALLOW)) {
                    throw new InvalidParameterValueException("Private gateway and network acl are not in the same vpc");
                }

                networkAclId = aclId;
            }

            { // experimental block, this is a hack
                // set vpc id in network to null
                // might be needed for all types of broadcast domains
                // the ugly hack is that vpc gateway nets are created as
                // guest network
                // while they are not.
                // A more permanent solution would be to define a type of
                // 'gatewaynetwork'
                // so that handling code is not mixed between the two
                final NetworkVO gatewaynet = _ntwkDao.findById(privateNtwk.getId());
                gatewaynet.setVpcId(null);
                _ntwkDao.persist(gatewaynet);
            }

            // 2) create gateway entry
            gatewayVO = new VpcGatewayVO(ipAddress, VpcGateway.Type.Private, vpcId, privateNtwk.getDataCenterId(), privateNtwk.getId(), privateNtwk.getBroadcastUri().toString(),
                    gateway, netmask, vpc.getAccountId(), vpc.getDomainId(), isSourceNat, networkAclId);
            _vpcGatewayDao.persist(gatewayVO);

            s_logger.debug("Created vpc gateway entry " + gatewayVO);
        } catch (final Exception e) {
            ExceptionUtil.rethrowRuntime(e);
            ExceptionUtil.rethrow(e, InsufficientCapacityException.class);
            ExceptionUtil.rethrow(e, ResourceAllocationException.class);
            throw new IllegalStateException(e);
        }

        CallContext.current().setEventDetails("Private Gateway Id: " + gatewayVO.getId());
        return getVpcPrivateGateway(gatewayVO.getId());
    }

    private void validateVpcPrivateGatewayAssociateNetworkId(NetworkOfferingVO ntwkOff, String broadcastUri, Long associatedNetworkId, Boolean bypassVlanOverlapCheck) {
        // Validate vlanId and associatedNetworkId
        if (broadcastUri == null && associatedNetworkId == null) {
            throw new InvalidParameterValueException("One of vlanId and associatedNetworkId must be specified");
        }
        if (broadcastUri != null && associatedNetworkId != null) {
            throw new InvalidParameterValueException("vlanId and associatedNetworkId are mutually exclusive");
        }
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId()) && (ntwkOff.isSpecifyVlan() || broadcastUri != null || bypassVlanOverlapCheck)) {
            throw new InvalidParameterValueException("Only ROOT admin is allowed to specify vlanId or bypass vlan overlap check");
        }
        if (ntwkOff.isSpecifyVlan() && broadcastUri == null) {
            throw new InvalidParameterValueException("vlanId must be specified for this network offering");
        }
        if (! ntwkOff.isSpecifyVlan() && associatedNetworkId == null) {
            throw new InvalidParameterValueException("associatedNetworkId must be specified for this network offering");
        }
    }

    private NetworkOfferingVO getVpcPrivateGatewayNetworkOffering(Long networkOfferingIdPassed, String broadcastUri) {
        // Validate network offering
        NetworkOfferingVO ntwkOff = null;
        if (networkOfferingIdPassed != null) {
            ntwkOff = _networkOfferingDao.findById(networkOfferingIdPassed);
            if (ntwkOff == null) {
                throw new InvalidParameterValueException("Unable to find network offering by id specified");
            }
            if (! TrafficType.Guest.equals(ntwkOff.getTrafficType())) {
                throw new InvalidParameterValueException("The network offering cannot be used to create Guest network");
            }
            if (! GuestType.Isolated.equals(ntwkOff.getGuestType())) {
                throw new InvalidParameterValueException("The network offering cannot be used to create Isolated network");
            }
        } else if (broadcastUri != null) {
            ntwkOff = _networkOfferingDao.findByUniqueName(NetworkOffering.SystemPrivateGatewayNetworkOffering);
        } else {
            ntwkOff = _networkOfferingDao.findByUniqueName(NetworkOffering.SystemPrivateGatewayNetworkOfferingWithoutVlan);
        }
        return ntwkOff;
    }

    private Long validateVpcPrivateGatewayPhysicalNetworkId(Long dcId, Long physicalNetworkId, Long associatedNetworkId, NetworkOfferingVO ntwkOff) {
        // Validate physical network
        if (associatedNetworkId != null) {
            Network associatedNetwork = _entityMgr.findById(Network.class, associatedNetworkId);
            if (associatedNetwork == null) {
                throw new InvalidParameterValueException("Unable to find network by ID " + associatedNetworkId);
            }
            if (physicalNetworkId != null && !physicalNetworkId.equals(associatedNetwork.getPhysicalNetworkId())) {
                throw new InvalidParameterValueException("The network can only be created on the same physical network as the associated network");
            } else if (physicalNetworkId == null) {
                physicalNetworkId = associatedNetwork.getPhysicalNetworkId();
            }
        }
        if (physicalNetworkId == null) {
            // Determine the physical network by network offering tags
            physicalNetworkId = _ntwkSvc.findPhysicalNetworkId(dcId, ntwkOff.getTags(), ntwkOff.getTrafficType());
        }
        if (physicalNetworkId == null) {
            final List<? extends PhysicalNetwork> pNtwks = _ntwkModel.getPhysicalNtwksSupportingTrafficType(dcId, TrafficType.Guest);
            if (pNtwks.isEmpty() || pNtwks.size() != 1) {
                throw new InvalidParameterValueException("Physical network can't be determined; pass physical network id");
            }
            physicalNetworkId = pNtwks.get(0).getId();
        }
        return physicalNetworkId;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PRIVATE_GATEWAY_CREATE, eventDescription = "Applying VPC private gateway", async = true)
    public PrivateGateway applyVpcPrivateGateway(final long gatewayId, final boolean destroyOnFailure) throws ConcurrentOperationException, ResourceUnavailableException {
        final VpcGatewayVO vo = _vpcGatewayDao.findById(gatewayId);

        boolean success = true;
        try {
            final List<Provider> providersToImplement = getVpcProviders(vo.getVpcId());

            final PrivateGateway gateway = getVpcPrivateGateway(gatewayId);
            for (final VpcProvider provider : getVpcElements()) {
                if (providersToImplement.contains(provider.getProvider())) {
                    if (!provider.createPrivateGateway(gateway)) {
                        success = false;
                    }
                }
            }
            if (success) {
                s_logger.debug("Private gateway " + gateway + " was applied successfully on the backend");
                if (vo.getState() != VpcGateway.State.Ready) {
                    vo.setState(VpcGateway.State.Ready);
                    _vpcGatewayDao.update(vo.getId(), vo);
                    s_logger.debug("Marke gateway " + gateway + " with state " + VpcGateway.State.Ready);
                }
                CallContext.current().setEventDetails("Private Gateway Id: " + gatewayId);
                return getVpcPrivateGateway(gatewayId);
            } else {
                s_logger.warn("Private gateway " + gateway + " failed to apply on the backend");
                return null;
            }
        } finally {
            // do cleanup
            if (!success) {
                if (destroyOnFailure) {
                    s_logger.debug("Destroying private gateway " + vo + " that failed to start");
                    // calling deleting from db because on createprivategateway
                    // fail, destroyPrivateGateway is already called
                    if (deletePrivateGatewayFromTheDB(getVpcPrivateGateway(gatewayId))) {
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
    public boolean deleteVpcPrivateGateway(final long gatewayId) throws ConcurrentOperationException, ResourceUnavailableException {
        final VpcGatewayVO gatewayToBeDeleted = _vpcGatewayDao.findById(gatewayId);
        if (gatewayToBeDeleted == null) {
            s_logger.debug("VPC gateway is already deleted for id=" + gatewayId);
            return true;
        }

        final VpcGatewayVO gatewayVO = _vpcGatewayDao.acquireInLockTable(gatewayId);
        if (gatewayVO == null || gatewayVO.getType() != VpcGateway.Type.Private) {
            throw new ConcurrentOperationException("Unable to lock gateway " + gatewayId);
        }

        final Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            _accountMgr.checkAccess(caller, null, false, gatewayVO);
            final NetworkVO networkVO = _ntwkDao.findById(gatewayVO.getNetworkId());
            if (networkVO != null) {
                _accountMgr.checkAccess(caller, null, false, networkVO);
                if (_networkOfferingDao.findById(networkVO.getNetworkOfferingId()).isSpecifyVlan()) {
                    throw new InvalidParameterValueException("Unable to delete private gateway with specified vlan by non-ROOT accounts");
                }
            }
        }
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    // don't allow to remove gateway when there are static
                    // routes associated with it
                    final long routeCount = _staticRouteDao.countRoutesByGateway(gatewayVO.getId());
                    if (routeCount > 0) {
                        throw new CloudRuntimeException("Can't delete private gateway " + gatewayVO + " as it has " + routeCount
                                + " static routes applied. Remove the routes first");
                    }

                    gatewayVO.setState(VpcGateway.State.Deleting);
                    _vpcGatewayDao.update(gatewayVO.getId(), gatewayVO);
                    s_logger.debug("Marked gateway " + gatewayVO + " with state " + VpcGateway.State.Deleting);
                }
            });

            // 1) delete the gateway on the backend
            final List<Provider> providersToImplement = getVpcProviders(gatewayVO.getVpcId());
            final PrivateGateway gateway = getVpcPrivateGateway(gatewayId);
            for (final VpcProvider provider : getVpcElements()) {
                if (providersToImplement.contains(provider.getProvider())) {
                    if (provider.deletePrivateGateway(gateway)) {
                        s_logger.debug("Private gateway " + gateway + " was applied successfully on the backend");
                    } else {
                        s_logger.warn("Private gateway " + gateway + " failed to apply on the backend");
                        gatewayVO.setState(VpcGateway.State.Ready);
                        _vpcGatewayDao.update(gatewayVO.getId(), gatewayVO);
                        s_logger.debug("Marked gateway " + gatewayVO + " with state " + VpcGateway.State.Ready);

                        return false;
                    }
                }
            }

            // 2) Clean up any remaining routes
            cleanUpRoutesByGatewayId(gatewayId);

            // 3) Delete private gateway from the DB
            return deletePrivateGatewayFromTheDB(gateway);

        } finally {
            if (gatewayVO != null) {
                _vpcGatewayDao.releaseFromLockTable(gatewayId);
            }
        }
    }

    private void cleanUpRoutesByGatewayId(long gatewayId){
        List<StaticRouteVO> routes = _staticRouteDao.listByGatewayId(gatewayId);
        for (StaticRouteVO route: routes){
            _staticRouteDao.remove(route.getId());
        }
    }

    @DB
    protected boolean deletePrivateGatewayFromTheDB(final PrivateGateway gateway) {
        // check if there are ips allocted in the network
        final long networkId = gateway.getNetworkId();

        vpcTxCallable.setGateway(gateway);

        final ExecutorService txExecutor = Executors.newSingleThreadExecutor();
        final Future<Boolean> futureResult = txExecutor.submit(vpcTxCallable);

        boolean deleteNetworkFinal;
        try {
            deleteNetworkFinal = futureResult.get();
            if (deleteNetworkFinal) {
                final User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
                final Account owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
                final ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
                _ntwkMgr.destroyNetwork(networkId, context, false);
                s_logger.debug("Deleted private network id=" + networkId);
            }
        } catch (final InterruptedException e) {
            s_logger.error("deletePrivateGatewayFromTheDB failed to delete network id " + networkId + "due to => ", e);
        } catch (final ExecutionException e) {
            s_logger.error("deletePrivateGatewayFromTheDB failed to delete network id " + networkId + "due to => ", e);
        }

        return true;
    }

    @Override
    public Pair<List<PrivateGateway>, Integer> listPrivateGateway(final ListPrivateGatewaysCmd cmd) {
        final String ipAddress = cmd.getIpAddress();
        final String vlan = cmd.getVlan();
        final Long vpcId = cmd.getVpcId();
        final Long id = cmd.getId();
        Boolean isRecursive = cmd.isRecursive();
        final Boolean listAll = cmd.listAll();
        Long domainId = cmd.getDomainId();
        final String accountName = cmd.getAccountName();
        final Account caller = CallContext.current().getCallingAccount();
        final List<Long> permittedAccounts = new ArrayList<Long>();
        final String state = cmd.getState();
        final Long projectId = cmd.getProjectId();

        final Filter searchFilter = new Filter(VpcGatewayVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive,
                null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        final SearchBuilder<VpcGatewayVO> sb = _vpcGatewayDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        if (vlan != null) {
            final SearchBuilder<NetworkVO> ntwkSearch = _ntwkDao.createSearchBuilder();
            ntwkSearch.and("vlan", ntwkSearch.entity().getBroadcastUri(), SearchCriteria.Op.EQ);
            sb.join("networkSearch", ntwkSearch, sb.entity().getNetworkId(), ntwkSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        final SearchCriteria<VpcGatewayVO> sc = sb.create();
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

        final Pair<List<VpcGatewayVO>, Integer> vos = _vpcGatewayDao.searchAndCount(sc, searchFilter);
        final List<PrivateGateway> privateGtws = new ArrayList<PrivateGateway>(vos.first().size());
        for (final VpcGateway vo : vos.first()) {
            privateGtws.add(getPrivateGatewayProfile(vo));
        }

        return new Pair<List<PrivateGateway>, Integer>(privateGtws, vos.second());
    }

    @Override
    public StaticRoute getStaticRoute(final long routeId) {
        return _staticRouteDao.findById(routeId);
    }

    @Override
    public boolean applyStaticRoutesForVpc(final long vpcId) throws ResourceUnavailableException {
        final Account caller = CallContext.current().getCallingAccount();
        final List<? extends StaticRoute> routes = _staticRouteDao.listByVpcId(vpcId);
        return applyStaticRoutes(routes, caller, true);
    }

    protected boolean applyStaticRoutes(final List<? extends StaticRoute> routes, final Account caller, final boolean updateRoutesInDB) throws ResourceUnavailableException {
        final boolean success = true;
        final List<StaticRouteProfile> staticRouteProfiles = new ArrayList<StaticRouteProfile>(routes.size());
        final Map<Long, VpcGateway> gatewayMap = new HashMap<Long, VpcGateway>();
        for (final StaticRoute route : routes) {
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
                for (final StaticRoute route : routes) {
                    if (route.getState() == StaticRoute.State.Revoke) {
                        _staticRouteDao.remove(route.getId());
                        s_logger.debug("Removed route " + route + " from the DB");
                    } else if (route.getState() == StaticRoute.State.Add) {
                        final StaticRouteVO ruleVO = _staticRouteDao.findById(route.getId());
                        ruleVO.setState(StaticRoute.State.Active);
                        _staticRouteDao.update(ruleVO.getId(), ruleVO);
                        s_logger.debug("Marked route " + route + " with state " + StaticRoute.State.Active);
                    }
                }
            }
        }

        return success;
    }

    protected boolean applyStaticRoutes(final List<StaticRouteProfile> routes) throws ResourceUnavailableException {
        if (routes.isEmpty()) {
            s_logger.debug("No static routes to apply");
            return true;
        }
        final Vpc vpc = vpcDao.findById(routes.get(0).getVpcId());

        s_logger.debug("Applying static routes for vpc " + vpc);
        final String staticNatProvider = _vpcSrvcDao.getProviderForServiceInVpc(vpc.getId(), Service.StaticNat);

        for (final VpcProvider provider : getVpcElements()) {
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
    public boolean revokeStaticRoute(final long routeId) throws ResourceUnavailableException {
        final Account caller = CallContext.current().getCallingAccount();

        final StaticRouteVO route = _staticRouteDao.findById(routeId);
        if (route == null) {
            throw new InvalidParameterValueException("Unable to find static route by id");
        }

        _accountMgr.checkAccess(caller, null, false, route);

        markStaticRouteForRevoke(route, caller);

        return applyStaticRoutesForVpc(route.getVpcId());
    }

    @DB
    protected boolean revokeStaticRoutesForVpc(final long vpcId, final Account caller) throws ResourceUnavailableException {
        // get all static routes for the vpc
        final List<StaticRouteVO> routes = _staticRouteDao.listByVpcId(vpcId);
        s_logger.debug("Found " + routes.size() + " to revoke for the vpc " + vpcId);
        if (!routes.isEmpty()) {
            // mark all of them as revoke
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    for (final StaticRouteVO route : routes) {
                        markStaticRouteForRevoke(route, caller);
                    }
                }
            });
            return applyStaticRoutesForVpc(vpcId);
        }

        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_STATIC_ROUTE_CREATE, eventDescription = "creating static route", create = true)
    public StaticRoute createStaticRoute(final long gatewayId, final String cidr) throws NetworkRuleConflictException {
        final Account caller = CallContext.current().getCallingAccount();

        // parameters validation
        final VpcGateway gateway = _vpcGatewayDao.findById(gatewayId);
        if (gateway == null) {
            throw new InvalidParameterValueException("Invalid gateway id is given");
        }

        if (gateway.getState() != VpcGateway.State.Ready) {
            throw new InvalidParameterValueException("Gateway is not in the " + VpcGateway.State.Ready + " state: " + gateway.getState());
        }

        final Vpc vpc = getActiveVpc(gateway.getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Can't add static route to VPC that is being deleted");
        }
        _accountMgr.checkAccess(caller, null, false, vpc);

        if (!NetUtils.isValidIp4Cidr(cidr)) {
            throw new InvalidParameterValueException("Invalid format for cidr " + cidr);
        }

        // validate the cidr
        // 1) CIDR should be outside of VPC cidr for guest networks
        if (NetUtils.isNetworksOverlap(vpc.getCidr(), cidr)) {
            throw new InvalidParameterValueException("CIDR should be outside of VPC cidr " + vpc.getCidr());
        }

        // 2) CIDR should be outside of link-local cidr
        if (NetUtils.isNetworksOverlap(vpc.getCidr(), NetUtils.getLinkLocalCIDR())) {
            throw new InvalidParameterValueException("CIDR should be outside of link local cidr " + NetUtils.getLinkLocalCIDR());
        }

        // 3) Verify against denied routes
        if (isCidrDenylisted(cidr, vpc.getZoneId())) {
            throw new InvalidParameterValueException("The static gateway cidr overlaps with one of the denied routes of the zone the VPC belongs to");
        }

        return Transaction.execute(new TransactionCallbackWithException<StaticRouteVO, NetworkRuleConflictException>() {
            @Override
            public StaticRouteVO doInTransaction(final TransactionStatus status) throws NetworkRuleConflictException {
                StaticRouteVO newRoute = new StaticRouteVO(gateway.getId(), cidr, vpc.getId(), vpc.getAccountId(), vpc.getDomainId());
                s_logger.debug("Adding static route " + newRoute);
                newRoute = _staticRouteDao.persist(newRoute);

                detectRoutesConflict(newRoute);

                if (!_staticRouteDao.setStateToAdd(newRoute)) {
                    throw new CloudRuntimeException("Unable to update the state to add for " + newRoute);
                }
                CallContext.current().setEventDetails("Static route Id: " + newRoute.getId());

                return newRoute;
            }
        });
    }

    protected boolean isCidrDenylisted(final String cidr, final long zoneId) {
        final String routesStr = NetworkOrchestrationService.GuestDomainSuffix.valueIn(zoneId);
        if (routesStr != null && !routesStr.isEmpty()) {
            final String[] cidrDenyList = routesStr.split(",");

            if (cidrDenyList != null && cidrDenyList.length > 0) {
                for (final String denyListedRoute : cidrDenyList) {
                    if (NetUtils.isNetworksOverlap(denyListedRoute, cidr)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public Pair<List<? extends StaticRoute>, Integer> listStaticRoutes(final ListStaticRoutesCmd cmd) {
        final Long id = cmd.getId();
        final Long gatewayId = cmd.getGatewayId();
        final Long vpcId = cmd.getVpcId();
        Long domainId = cmd.getDomainId();
        Boolean isRecursive = cmd.isRecursive();
        final Boolean listAll = cmd.listAll();
        final String accountName = cmd.getAccountName();
        final Account caller = CallContext.current().getCallingAccount();
        final List<Long> permittedAccounts = new ArrayList<Long>();
        final Map<String, String> tags = cmd.getTags();
        final Long projectId = cmd.getProjectId();
        final String state = cmd.getState();

        final Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(domainId, isRecursive,
                null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, projectId, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        final ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        final Filter searchFilter = new Filter(StaticRouteVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        final SearchBuilder<StaticRouteVO> sb = _staticRouteDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("vpcGatewayId", sb.entity().getVpcGatewayId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        if (tags != null && !tags.isEmpty()) {
            final SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        final SearchCriteria<StaticRouteVO> sc = sb.create();
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

        if (state != null) {
            sc.addAnd("state", Op.EQ, state);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.StaticRoute.toString());
            for (final String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        final Pair<List<StaticRouteVO>, Integer> result = _staticRouteDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends StaticRoute>, Integer>(result.first(), result.second());
    }

    protected void detectRoutesConflict(final StaticRoute newRoute) throws NetworkRuleConflictException {
        // Multiple private gateways can exist within Vpc. Check for conflicts
        // for all static routes in Vpc
        // and not just the gateway
        final List<? extends StaticRoute> routes = _staticRouteDao.listByVpcIdAndNotRevoked(newRoute.getVpcId());
        assert routes.size() >= 1 : "For static routes, we now always first persist the route and then check for "
                + "network conflicts so we should at least have one rule at this point.";

        for (final StaticRoute route : routes) {
            if (route.getId() == newRoute.getId()) {
                continue; // Skips my own route.
            }

            if (NetUtils.isNetworksOverlap(route.getCidr(), newRoute.getCidr())) {
                throw new NetworkRuleConflictException("New static route cidr conflicts with existing route " + route);
            }
        }
    }

    protected void markStaticRouteForRevoke(final StaticRouteVO route, final Account caller) {
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

    protected class VpcCleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                final GlobalLock lock = GlobalLock.getInternLock("VpcCleanup");
                if (lock == null) {
                    s_logger.debug("Couldn't get the global lock");
                    return;
                }

                if (!lock.lock(30)) {
                    s_logger.debug("Couldn't lock the db");
                    return;
                }

                try {
                    // Cleanup inactive VPCs
                    final List<VpcVO> inactiveVpcs = vpcDao.listInactiveVpcs();
                    if (inactiveVpcs != null) {
                        s_logger.info("Found " + inactiveVpcs.size() + " removed VPCs to cleanup");
                        for (final VpcVO vpc : inactiveVpcs) {
                            s_logger.debug("Cleaning up " + vpc);
                            destroyVpc(vpc, _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM);
                        }
                    }
                } catch (final Exception e) {
                    s_logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (final Exception e) {
                s_logger.error("Exception ", e);
            }
        }
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "associating Ip", async = true)
    public IpAddress associateIPToVpc(final long ipId, final long vpcId) throws ResourceAllocationException, ResourceUnavailableException, InsufficientAddressCapacityException,
            ConcurrentOperationException {
        final Account caller = CallContext.current().getCallingAccount();
        Account owner = null;

        final IpAddress ipToAssoc = _ntwkModel.getIp(ipId);
        if (ipToAssoc != null) {
            _accountMgr.checkAccess(caller, null, true, ipToAssoc);
            owner = _accountMgr.getAccount(ipToAssoc.getAllocatedToAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        final Vpc vpc = vpcDao.findById(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid VPC id provided");
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, owner, vpc);

        s_logger.debug("Associating ip " + ipToAssoc + " to vpc " + vpc);

        final boolean isSourceNatFinal = isSrcNatIpRequired(vpc.getVpcOfferingId()) && getExistingSourceNatInVpc(owner.getId(), vpcId) == null;
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                final IPAddressVO ip = _ipAddressDao.findById(ipId);
                // update ip address with networkId
                ip.setVpcId(vpcId);
                ip.setSourceNat(isSourceNatFinal);

                _ipAddressDao.update(ipId, ip);

                // mark ip as allocated
                _ipAddrMgr.markPublicIpAsAllocated(ip);
            }
        });

        s_logger.debug("Successfully assigned ip " + ipToAssoc + " to vpc " + vpc);
        CallContext.current().putContextParameter(IpAddress.class, ipToAssoc.getUuid());
        return _ipAddressDao.findById(ipId);
    }

    @Override
    public void unassignIPFromVpcNetwork(final long ipId, final long networkId) {
        final IPAddressVO ip = _ipAddressDao.findById(ipId);
        if (isIpAllocatedToVpc(ip)) {
            return;
        }

        if (ip == null || ip.getVpcId() == null) {
            return;
        }

        s_logger.debug("Releasing VPC ip address " + ip + " from vpc network id=" + networkId);

        final long vpcId = ip.getVpcId();
        boolean success = false;
        try {
            // unassign ip from the VPC router
            success = _ipAddrMgr.applyIpAssociations(_ntwkModel.getNetwork(networkId), true);
        } catch (final ResourceUnavailableException ex) {
            throw new CloudRuntimeException("Failed to apply ip associations for network id=" + networkId + " as a part of unassigning ip " + ipId + " from vpc", ex);
        }

        if (success) {
            ip.setAssociatedWithNetworkId(null);
            _ipAddressDao.update(ipId, ip);
            s_logger.debug("IP address " + ip + " is no longer associated with the network inside vpc id=" + vpcId);
        } else {
            throw new CloudRuntimeException("Failed to apply ip associations for network id=" + networkId + " as a part of unassigning ip " + ipId + " from vpc");
        }
        s_logger.debug("Successfully released VPC ip address " + ip + " back to VPC pool ");
    }

    @Override
    public boolean isIpAllocatedToVpc(final IpAddress ip) {
        return ip != null && ip.getVpcId() != null && (ip.isOneToOneNat() || !_firewallDao.listByIp(ip.getId()).isEmpty());
    }

    @DB
    @Override
    public Network createVpcGuestNetwork(final long ntwkOffId, final String name, final String displayText, final String gateway, final String cidr, final String vlanId,
            String networkDomain, final Account owner, final Long domainId, final PhysicalNetwork pNtwk, final long zoneId, final ACLType aclType, final Boolean subdomainAccess,
            final long vpcId, final Long aclId, final Account caller, final Boolean isDisplayNetworkEnabled, String externalId, String ip6Gateway, String ip6Cidr, final String ip4Dns1, final String ip4Dns2, final String ip6Dns1, final String ip6Dns2, Pair<Integer, Integer> vrIfaceMTUs) throws ConcurrentOperationException, InsufficientCapacityException,
            ResourceAllocationException {

        final Vpc vpc = getActiveVpc(vpcId);

        if (vpc == null) {
            final InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find Enabled VPC ");
            ex.addProxyObject(String.valueOf(vpcId), "VPC");
            throw ex;
        }
        _accountMgr.checkAccess(caller, null, false, vpc);

        if (networkDomain == null) {
            networkDomain = vpc.getNetworkDomain();
        }

        if (!vpc.isRegionLevelVpc() && vpc.getZoneId() != zoneId) {
            throw new InvalidParameterValueException("New network doesn't belong to vpc zone");
        }

        // 1) Validate if network can be created for VPC
        validateNtwkOffForNtwkInVpc(null, ntwkOffId, cidr, networkDomain, vpc, gateway, owner, aclId);

        // 2) Create network
        final Network guestNetwork = _ntwkMgr.createGuestNetwork(ntwkOffId, name, displayText, gateway, cidr, vlanId, false, networkDomain, owner, domainId, pNtwk, zoneId, aclType,
                subdomainAccess, vpcId, ip6Gateway, ip6Cidr, isDisplayNetworkEnabled, null, null, externalId, null, null, ip4Dns1, ip4Dns2, ip6Dns1, ip6Dns2, vrIfaceMTUs);

        if (guestNetwork != null) {
            guestNetwork.setNetworkACLId(aclId);
            _ntwkDao.update(guestNetwork.getId(), (NetworkVO) guestNetwork);
        }
        return guestNetwork;
    }

    protected IPAddressVO getExistingSourceNatInVpc(final long ownerId, final long vpcId) {

        final List<IPAddressVO> addrs = listPublicIpsAssignedToVpc(ownerId, true, vpcId);

        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            return null;
        } else {
            // Account already has ip addresses
            for (final IPAddressVO addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = addr;
                    return sourceNatIp;
                }
            }

            assert sourceNatIp != null : "How do we get a bunch of ip addresses but none of them are source nat? " + "account=" + ownerId + "; vpcId=" + vpcId;
        }

        return sourceNatIp;
    }

    protected List<IPAddressVO> listPublicIpsAssignedToVpc(final long accountId, final Boolean sourceNat, final long vpcId) {
        final SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("vpcId", vpcId);

        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public PublicIp assignSourceNatIpAddressToVpc(final Account owner, final Vpc vpc) throws InsufficientAddressCapacityException, ConcurrentOperationException {
        final long dcId = vpc.getZoneId();

        final IPAddressVO sourceNatIp = getExistingSourceNatInVpc(owner.getId(), vpc.getId());

        PublicIp ipToReturn = null;

        if (sourceNatIp != null) {
            ipToReturn = PublicIp.createFromAddrAndVlan(sourceNatIp, _vlanDao.findById(sourceNatIp.getVlanId()));
        } else {
            ipToReturn = _ipAddrMgr.assignDedicateIpAddress(owner, null, vpc.getId(), dcId, true);
        }

        return ipToReturn;
    }

    @Override
    public List<HypervisorType> getSupportedVpcHypervisors() {
        return Collections.unmodifiableList(hTypes);
    }

    private List<Provider> getVpcProviders(final long vpcId) {
        final List<String> providerNames = _vpcSrvcDao.getDistinctProviders(vpcId);
        final Map<String, Provider> providers = new HashMap<String, Provider>();
        for (final String providerName : providerNames) {
            if (!providers.containsKey(providerName)) {
                providers.put(providerName, Network.Provider.getProvider(providerName));
            }
        }

        return new ArrayList<Provider>(providers.values());
    }

    @Inject
    public void setVpcElements(final List<VpcProvider> vpcElements) {
        this.vpcElements = vpcElements;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_STATIC_ROUTE_CREATE, eventDescription = "Applying static route", async = true)
    public boolean applyStaticRoute(final long routeId) throws ResourceUnavailableException {
        final StaticRoute route = _staticRouteDao.findById(routeId);
        return applyStaticRoutesForVpc(route.getVpcId());
    }

    @Override
    public boolean isSrcNatIpRequired(long vpcOfferingId) {
        final Map<Network.Service, Set<Network.Provider>> vpcOffSvcProvidersMap = getVpcOffSvcProvidersMap(vpcOfferingId);
        return vpcOffSvcProvidersMap.get(Network.Service.SourceNat).contains(Network.Provider.VPCVirtualRouter);
    }

    /**
     * rollingRestartVpc performs restart of routers of a VPC by first
     * deploying a new VR and then destroying old VRs in rolling fashion. For
     * non-redundant VPC, it will re-program the new router as final step
     * otherwise deploys a backup router for the VPC.
     * @param vpc vpc to be restarted
     * @param context reservation context
     * @return returns true when the rolling restart succeeds
     * @throws ResourceUnavailableException
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     */
    private boolean rollingRestartVpc(final Vpc vpc, final ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!NetworkOrchestrationService.RollingRestartEnabled.value()) {
            if (shutdownVpc(vpc.getId())) {
                return startVpc(vpc.getId(), false);
            }
            s_logger.warn("Failed to shutdown vpc as a part of VPC " + vpc + " restart process");
            return false;
        }
        s_logger.debug("Performing rolling restart of routers of VPC " + vpc);
        _ntwkMgr.destroyExpendableRouters(routerDao.listByVpcId(vpc.getId()), context);

        final DeployDestination dest = new DeployDestination(_dcDao.findById(vpc.getZoneId()), null, null, null);
        final List<DomainRouterVO> oldRouters = routerDao.listByVpcId(vpc.getId());

        // Create a new router
        if (oldRouters.size() > 0) {
            vpc.setRollingRestart(true);
        }
        startVpc(vpc, dest, context);
        if (oldRouters.size() > 0) {
            vpc.setRollingRestart(false);
        }

        // For redundant vpc wait for 3*advert_int+skew_seconds for VRRP to kick in
        if (vpc.isRedundant() || (oldRouters.size() == 1 && oldRouters.get(0).getIsRedundantRouter())) {
            try {
                Thread.sleep(NetworkOrchestrationService.RVRHandoverTime);
            } catch (final InterruptedException ignored) {
            }
        }

        // Destroy old routers
        for (final DomainRouterVO oldRouter : oldRouters) {
            _routerService.stopRouter(oldRouter.getId(), true);
            _routerService.destroyRouter(oldRouter.getId(), context.getAccount(), context.getCaller().getId());
        }

        // Re-program VPC VR or add a new backup router for redundant VPC
        if (!startVpc(vpc, dest, context)) {
            s_logger.debug("Failed to re-program VPC router or deploy a new backup router for VPC" + vpc);
            return false;
        }

        return _ntwkMgr.areRoutersRunning(routerDao.listByVpcId(vpc.getId()));
    }

    private List<Long> filterChildSubDomains(final List<Long> domainIds) {
        List<Long> filteredDomainIds = new ArrayList<>();
        if (domainIds != null) {
            filteredDomainIds.addAll(domainIds);
        }
        if (filteredDomainIds.size() > 1) {
            for (int i = filteredDomainIds.size() - 1; i >= 1; i--) {
                long first = filteredDomainIds.get(i);
                for (int j = i - 1; j >= 0; j--) {
                    long second = filteredDomainIds.get(j);
                    if (domainDao.isChildDomain(filteredDomainIds.get(i), filteredDomainIds.get(j))) {
                        filteredDomainIds.remove(j);
                        i--;
                    }
                    if (domainDao.isChildDomain(filteredDomainIds.get(j), filteredDomainIds.get(i))) {
                        filteredDomainIds.remove(i);
                        break;
                    }
                }
            }
        }
        return filteredDomainIds;
    }
}
