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
package com.cloud.configuration;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.agent.lb.IndirectAgentLBServiceImpl;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.config.ResetCfgCmd;
import org.apache.cloudstack.api.command.admin.config.UpdateCfgCmd;
import org.apache.cloudstack.api.command.admin.network.CreateGuestNetworkIpv6PrefixCmd;
import org.apache.cloudstack.api.command.admin.network.CreateManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteGuestNetworkIpv6PrefixCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.ListGuestNetworkIpv6PrefixesCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.UpdatePodManagementNetworkIpRangeCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.IsAccountAllowedToCreateOfferingsWithTagsCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.pod.DeletePodCmd;
import org.apache.cloudstack.api.command.admin.pod.UpdatePodCmd;
import org.apache.cloudstack.api.command.admin.region.CreatePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.DeletePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.ListPortableIpRangesCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DeleteVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.UpdateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.zone.CreateZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateZoneCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationGroupVO;
import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.network.lb.LoadBalancerConfigManager;
import org.apache.cloudstack.network.lb.LoadBalancerConfig.SSLConfiguration;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpDao;
import org.apache.cloudstack.region.PortableIpRange;
import org.apache.cloudstack.region.PortableIpRangeDao;
import org.apache.cloudstack.region.PortableIpRangeVO;
import org.apache.cloudstack.region.PortableIpVO;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.RegionVO;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.NetworkOfferingJoinDao;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.DataCenterGuestIpv6PrefixVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.DomainVlanMapVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.dc.dao.DataCenterGuestIpv6PrefixDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.DomainVlanMapDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainDetailVO;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDetailsDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.gpu.GPU;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Ipv6GuestPrefixSubnetNetworkMapVO;
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
import com.cloud.network.UserIpv6AddressVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Ipv6GuestPrefixSubnetNetworkMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingDetailsVO;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.test.IPRangeConfig;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicIpAliasVO;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Enums;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;

public class ConfigurationManagerImpl extends ManagerBase implements ConfigurationManager, ConfigurationService, Configurable {
    public static final Logger s_logger = Logger.getLogger(ConfigurationManagerImpl.class);

    @Inject
    EntityManager _entityMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigurationGroupDao _configGroupDao;
    @Inject
    ConfigurationSubGroupDao _configSubGroupDao;
    @Inject
    ConfigDepot _configDepot;
    @Inject
    HostPodDao _podDao;
    @Inject
    HostDao _hostDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    DomainVlanMapDao _domainVlanMapDao;
    @Inject
    PodVlanMapDao podVlanMapDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkOfferingJoinDao networkOfferingJoinDao;
    @Inject
    NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    IPAddressDao _publicIpAddressDao;
    @Inject
    DataCenterIpAddressDao _privateIpAddressDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkService _networkSvc;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    AlertManager _alertMgr;
    List<SecurityChecker> _secChecker;

    @Inject
    CapacityDao _capacityDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOffServiceMapDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _trafficTypeDao;
    @Inject
    NicDao _nicDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    UserDao _userDao;
    @Inject
    PortableIpRangeDao _portableIpRangeDao;
    @Inject
    RegionDao _regionDao;
    @Inject
    PortableIpDao _portableIpDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    DataCenterDetailsDao _dcDetailsDao;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    AccountDetailsDao _accountDetailsDao;
    @Inject
    DomainDetailsDao _domainDetailsDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    NicIpAliasDao _nicIpAliasDao;
    @Inject
    public ManagementService _mgr;
    @Inject
    DedicatedResourceDao _dedicatedDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    AffinityGroupDao _affinityGroupDao;
    @Inject
    AffinityGroupService _affinityGroupService;
    @Inject
    StorageManager _storageManager;
    @Inject
    ImageStoreDao _imageStoreDao;
    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;
    @Inject
    MessageBus messageBus;
    @Inject
    AgentManager _agentManager;
    @Inject
    IndirectAgentLB _indirectAgentLB;
    @Inject
    private VMTemplateZoneDao templateZoneDao;
    @Inject
    VsphereStoragePolicyDao vsphereStoragePolicyDao;
    @Inject
    HostTagsDao hostTagDao;
    @Inject
    StoragePoolTagsDao storagePoolTagDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    DataCenterGuestIpv6PrefixDao dataCenterGuestIpv6PrefixDao;
    @Inject
    Ipv6GuestPrefixSubnetNetworkMapDao ipv6GuestPrefixSubnetNetworkMapDao;
    @Inject
    Ipv6Service ipv6Service;

    // FIXME - why don't we have interface for DataCenterLinkLocalIpAddressDao?
    @Inject
    protected DataCenterLinkLocalIpAddressDao _linkLocalIpAllocDao;

    private long _defaultPageSize = Long.parseLong(Config.DefaultPageSize.getDefaultValue());
    private static final String DOMAIN_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{1,63}$";
    protected Set<String> configValuesForValidation;
    private Set<String> weightBasedParametersForValidation;
    private Set<String> overprovisioningFactorsForValidation;
    public static final String VM_USERDATA_MAX_LENGTH_STRING = "vm.userdata.max.length";

    public static final ConfigKey<Boolean> SystemVMUseLocalStorage = new ConfigKey<Boolean>(Boolean.class, "system.vm.use.local.storage", "Advanced", "false",
            "Indicates whether to use local storage pools or shared storage pools for system VMs.", false, ConfigKey.Scope.Zone, null);

    public final static ConfigKey<Long> BYTES_MAX_READ_LENGTH= new ConfigKey<Long>(Long.class, "vm.disk.bytes.maximum.read.length", "Advanced", "0",
            "Maximum Bytes read burst duration (seconds). If '0' (zero) then does not check for maximum burst length.", true, ConfigKey.Scope.Global, null);
    public final static ConfigKey<Long> BYTES_MAX_WRITE_LENGTH = new ConfigKey<Long>(Long.class, "vm.disk.bytes.maximum.write.length", "Advanced", "0",
            "Maximum Bytes write burst duration (seconds). If '0' (zero) then does not check for maximum burst length.", true, ConfigKey.Scope.Global, null);
    public final static ConfigKey<Long> IOPS_MAX_READ_LENGTH = new ConfigKey<Long>(Long.class, "vm.disk.iops.maximum.read.length", "Advanced", "0",
            "Maximum IOPS read burst duration (seconds). If '0' (zero) then does not check for maximum burst length.", true, ConfigKey.Scope.Global, null);
    public final static ConfigKey<Long> IOPS_MAX_WRITE_LENGTH = new ConfigKey<Long>(Long.class, "vm.disk.iops.maximum.write.length", "Advanced", "0",
            "Maximum IOPS write burst duration (seconds). If '0' (zero) then does not check for maximum burst length.", true, ConfigKey.Scope.Global, null);
    public static final ConfigKey<Boolean> ADD_HOST_ON_SERVICE_RESTART_KVM = new ConfigKey<Boolean>(Boolean.class, "add.host.on.service.restart.kvm", "Advanced", "true",
            "Indicates whether the host will be added back to cloudstack after restarting agent service on host. If false it won't be added back even after service restart",
            true, ConfigKey.Scope.Global, null);
    public static final ConfigKey<Boolean> SET_HOST_DOWN_TO_MAINTENANCE = new ConfigKey<Boolean>(Boolean.class, "set.host.down.to.maintenance", "Advanced", "false",
                        "Indicates whether the host in down state can be put into maintenance state so thats its not enabled after it comes back.",
                        true, ConfigKey.Scope.Zone, null);
    public static final ConfigKey<Boolean> ENABLE_ACCOUNT_SETTINGS_FOR_DOMAIN = new ConfigKey<Boolean>(Boolean.class, "enable.account.settings.for.domain", "Advanced", "false",
            "Indicates whether to add account settings for domain. If true, account settings will be added to domain settings, all accounts in the domain will inherit the domain setting if account setting is not set.", true, ConfigKey.Scope.Global, null);
    public static final ConfigKey<Boolean> ENABLE_DOMAIN_SETTINGS_FOR_CHILD_DOMAIN = new ConfigKey<Boolean>(Boolean.class, "enable.domain.settings.for.child.domain", "Advanced", "false",
            "Indicates whether the settings of parent domain should be applied for child domain. If true, the child domain will get value from parent domain if its not configured in child domain else global value is taken.",
            true, ConfigKey.Scope.Global, null);

    public static ConfigKey<Integer> VM_SERVICE_OFFERING_MAX_CPU_CORES = new ConfigKey<Integer>("Advanced", Integer.class, "vm.serviceoffering.cpu.cores.max", "0", "Maximum CPU cores "
      + "for vm service offering. If 0 - no limitation", true);

    public static ConfigKey<Integer> VM_SERVICE_OFFERING_MAX_RAM_SIZE = new ConfigKey<Integer>("Advanced", Integer.class, "vm.serviceoffering.ram.size.max", "0", "Maximum RAM size in "
      + "MB for vm service offering. If 0 - no limitation", true);

    public static final ConfigKey<Integer> VM_USERDATA_MAX_LENGTH = new ConfigKey<Integer>("Advanced", Integer.class, VM_USERDATA_MAX_LENGTH_STRING, "32768",
            "Max length of vm userdata after base64 decoding. Default is 32768 and maximum is 1048576", true);
    public static final ConfigKey<Boolean> MIGRATE_VM_ACROSS_CLUSTERS = new ConfigKey<Boolean>(Boolean.class, "migrate.vm.across.clusters", "Advanced", "false",
            "Indicates whether the VM can be migrated to different cluster if no host is found in same cluster",true, ConfigKey.Scope.Zone, null);

    public static final ConfigKey<Boolean> ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS = new ConfigKey<>(Boolean.class, "allow.domain.admins.to.create.tagged.offerings", "Advanced",
            "false", "Allow domain admins to create offerings with tags.", true, ConfigKey.Scope.Account, null);

    private static final String IOPS_READ_RATE = "IOPS Read";
    private static final String IOPS_WRITE_RATE = "IOPS Write";
    private static final String BYTES_READ_RATE = "Bytes Read";
    private static final String BYTES_WRITE_RATE = "Bytes Write";

    private static final String DefaultForSystemVmsForPodIpRange = "0";
    private static final String DefaultVlanForPodIpRange = Vlan.UNTAGGED.toString();

    private static final Set<Provider> VPC_ONLY_PROVIDERS = Sets.newHashSet(Provider.VPCVirtualRouter, Provider.JuniperContrailVpcRouter, Provider.InternalLbVm);

    private static final long GiB_TO_BYTES = 1024 * 1024 * 1024;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        final String defaultPageSizeString = _configDao.getValue(Config.DefaultPageSize.key());
        _defaultPageSize = NumbersUtil.parseLong(defaultPageSizeString, Long.parseLong(Config.DefaultPageSize.getDefaultValue()));

        populateConfigValuesForValidationSet();
        weightBasedParametersForValidation();
        overProvisioningFactorsForValidation();
        initMessageBusListener();
        return true;
    }

    private void populateConfigValuesForValidationSet() {
        configValuesForValidation = new HashSet<String>();
        configValuesForValidation.add("event.purge.interval");
        configValuesForValidation.add("account.cleanup.interval");
        configValuesForValidation.add("alert.wait");
        configValuesForValidation.add("consoleproxy.capacityscan.interval");
        configValuesForValidation.add("expunge.interval");
        configValuesForValidation.add("host.stats.interval");
        configValuesForValidation.add("network.gc.interval");
        configValuesForValidation.add("ping.interval");
        configValuesForValidation.add("snapshot.poll.interval");
        configValuesForValidation.add("storage.stats.interval");
        configValuesForValidation.add("storage.cleanup.interval");
        configValuesForValidation.add("wait");
        configValuesForValidation.add("xenserver.heartbeat.interval");
        configValuesForValidation.add("xenserver.heartbeat.timeout");
        configValuesForValidation.add("ovm3.heartbeat.interval");
        configValuesForValidation.add("ovm3.heartbeat.timeout");
        configValuesForValidation.add("incorrect.login.attempts.allowed");
        configValuesForValidation.add("vm.password.length");
        configValuesForValidation.add("externaldhcp.vmip.retrieval.interval");
        configValuesForValidation.add("externaldhcp.vmip.max.retry");
        configValuesForValidation.add("externaldhcp.vmipFetch.threadPool.max");
        configValuesForValidation.add("remote.access.vpn.psk.length");
        configValuesForValidation.add(StorageManager.STORAGE_POOL_DISK_WAIT.key());
        configValuesForValidation.add(StorageManager.STORAGE_POOL_CLIENT_TIMEOUT.key());
        configValuesForValidation.add(StorageManager.STORAGE_POOL_CLIENT_MAX_CONNECTIONS.key());
        configValuesForValidation.add(VM_USERDATA_MAX_LENGTH_STRING);
    }

    private void weightBasedParametersForValidation() {
        weightBasedParametersForValidation = new HashSet<String>();
        weightBasedParametersForValidation.add(AlertManager.CPUCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.StorageAllocatedCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.StorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.MemoryCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.PublicIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.PrivateIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.SecondaryStorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.VlanCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.DirectNetworkPublicIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.LocalStorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(CapacityManager.StorageAllocatedCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(CapacityManager.StorageCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(Config.AgentLoadThreshold.key());
        weightBasedParametersForValidation.add(Config.VmUserDispersionWeight.key());
        weightBasedParametersForValidation.add(CapacityManager.SecondaryStorageCapacityThreshold.key());

    }

    private void overProvisioningFactorsForValidation() {
        overprovisioningFactorsForValidation = new HashSet<String>();
        overprovisioningFactorsForValidation.add(CapacityManager.MemOverprovisioningFactor.key());
        overprovisioningFactorsForValidation.add(CapacityManager.CpuOverprovisioningFactor.key());
        overprovisioningFactorsForValidation.add(CapacityManager.StorageOverprovisioningFactor.key());
    }

    private void initMessageBusListener() {
        messageBus.subscribe(EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, new MessageSubscriber() {
            @Override
            public void onPublishMessage(String serderAddress, String subject, Object args) {
                String globalSettingUpdated = (String) args;
                if (StringUtils.isEmpty(globalSettingUpdated)) {
                    return;
                }
                if (globalSettingUpdated.equals(ApiServiceConfiguration.ManagementServerAddresses.key()) ||
                        globalSettingUpdated.equals(IndirectAgentLBServiceImpl.IndirectAgentLBAlgorithm.key())) {
                    _indirectAgentLB.propagateMSListToAgents();
                } else if (globalSettingUpdated.equals(Config.RouterAggregationCommandEachTimeout.toString())
                        ||  globalSettingUpdated.equals(Config.MigrateWait.toString())) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(Config.RouterAggregationCommandEachTimeout.toString(), _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));
                    params.put(Config.MigrateWait.toString(), _configDao.getValue(Config.MigrateWait.toString()));
                    _agentManager.propagateChangeToAgents(params);
                }
            }
        });
    }

    @Override
    public boolean start() {

        // TODO : this may not be a good place to do integrity check here, we
        // put it here as we need _alertMgr to be properly
        // configured
        // before we can use it

        // As it is so common for people to forget about configuring
        // management.network.cidr,
        final String mgtCidr = _configDao.getValue(Config.ManagementNetwork.key());
        if (mgtCidr == null || mgtCidr.trim().isEmpty()) {
            final String[] localCidrs = NetUtils.getLocalCidrs();
            if (localCidrs != null && localCidrs.length > 0) {
                s_logger.warn("Management network CIDR is not configured originally. Set it default to " + localCidrs[0]);

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management network CIDR is not configured originally. Set it default to "
                        + localCidrs[0], "");
                _configDao.update(Config.ManagementNetwork.key(), Config.ManagementNetwork.getCategory(), localCidrs[0]);
            } else {
                s_logger.warn("Management network CIDR is not properly configured and we are not able to find a default setting");
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0),
                        "Management network CIDR is not properly configured and we are not able to find a default setting", "");
            }
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @DB
    public String updateConfiguration(final long userId, final String name, final String category, final String value, final String scope, final Long resourceId) {
        final String validationMsg = validateConfigurationValue(name, value, scope);

        if (validationMsg != null) {
            s_logger.error("Invalid configuration option, name: " + name + ", value:" + value);
            throw new InvalidParameterValueException(validationMsg);
        }

        // If scope of the parameter is given then it needs to be updated in the
        // corresponding details table,
        // if scope is mentioned as global or not mentioned then it is normal
        // global parameter updation
        if (scope != null && !scope.isEmpty() && !ConfigKey.Scope.Global.toString().equalsIgnoreCase(scope)) {
            switch (ConfigKey.Scope.valueOf(scope)) {
            case Zone:
                final DataCenterVO zone = _zoneDao.findById(resourceId);
                if (zone == null) {
                    throw new InvalidParameterValueException("unable to find zone by id " + resourceId);
                }
                _dcDetailsDao.addDetail(resourceId, name, value, true);
                break;
            case Cluster:
                final ClusterVO cluster = _clusterDao.findById(resourceId);
                if (cluster == null) {
                    throw new InvalidParameterValueException("unable to find cluster by id " + resourceId);
                }
                String newName = name;
                if (name.equalsIgnoreCase("cpu.overprovisioning.factor")) {
                    newName = "cpuOvercommitRatio";
                }
                if (name.equalsIgnoreCase("mem.overprovisioning.factor")) {
                    newName = "memoryOvercommitRatio";
                }
                ClusterDetailsVO clusterDetailsVO = _clusterDetailsDao.findDetail(resourceId, newName);
                if (clusterDetailsVO == null) {
                    clusterDetailsVO = new ClusterDetailsVO(resourceId, newName, value);
                    _clusterDetailsDao.persist(clusterDetailsVO);
                } else {
                    clusterDetailsVO.setValue(value);
                    _clusterDetailsDao.update(clusterDetailsVO.getId(), clusterDetailsVO);
                }
                break;

            case StoragePool:
                final StoragePoolVO pool = _storagePoolDao.findById(resourceId);
                if (pool == null) {
                    throw new InvalidParameterValueException("unable to find storage pool by id " + resourceId);
                }
                if(name.equals(CapacityManager.StorageOverprovisioningFactor.key())) {
                    if(!pool.getPoolType().supportsOverProvisioning() ) {
                        throw new InvalidParameterValueException("Unable to update storage pool with id " + resourceId + ". Overprovision not supported for " + pool.getPoolType());
                    }
                }

                _storagePoolDetailsDao.addDetail(resourceId, name, value, true);
                if (pool.getPoolType() == Storage.StoragePoolType.DatastoreCluster) {
                    List<StoragePoolVO> childDataStores = _storagePoolDao.listChildStoragePoolsInDatastoreCluster(resourceId);
                    for (StoragePoolVO childDataStore: childDataStores) {
                        _storagePoolDetailsDao.addDetail(childDataStore.getId(), name, value, true);
                    }
                }

                break;

            case Account:
                final AccountVO account = _accountDao.findById(resourceId);
                if (account == null) {
                    throw new InvalidParameterValueException("unable to find account by id " + resourceId);
                }
                AccountDetailVO accountDetailVO = _accountDetailsDao.findDetail(resourceId, name);
                if (accountDetailVO == null) {
                    accountDetailVO = new AccountDetailVO(resourceId, name, value);
                    _accountDetailsDao.persist(accountDetailVO);
                } else {
                    accountDetailVO.setValue(value);
                    _accountDetailsDao.update(accountDetailVO.getId(), accountDetailVO);
                }
                break;

            case ImageStore:
                final ImageStoreVO imgStore = _imageStoreDao.findById(resourceId);
                Preconditions.checkState(imgStore != null);
                _imageStoreDetailsDao.addDetail(resourceId, name, value, true);
                break;

            case Domain:
                final DomainVO domain = _domainDao.findById(resourceId);
                if (domain == null) {
                    throw new InvalidParameterValueException("unable to find domain by id " + resourceId);
                }
                DomainDetailVO domainDetailVO = _domainDetailsDao.findDetail(resourceId, name);
                if (domainDetailVO == null) {
                    domainDetailVO = new DomainDetailVO(resourceId, name, value);
                    _domainDetailsDao.persist(domainDetailVO);
                } else {
                    domainDetailVO.setValue(value);
                    _domainDetailsDao.update(domainDetailVO.getId(), domainDetailVO);
                }
                break;

            default:
                throw new InvalidParameterValueException("Scope provided is invalid");
            }
            return value;
        }

        // Execute all updates in a single transaction
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        if (!_configDao.update(name, category, value)) {
            s_logger.error("Failed to update configuration option, name: " + name + ", value:" + value);
            throw new CloudRuntimeException("Failed to update configuration value. Please contact Cloud Support.");
        }

        PreparedStatement pstmt = null;
        if (Config.XenServerGuestNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "guest.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update guest.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerPrivateNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "private.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update private.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerPublicNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "public.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update public.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerStorageNetwork1.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device1");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update storage.network.device1 in host_details due to exception ", e);
            }
        } else if (Config.XenServerStorageNetwork2.key().equals(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device2");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update storage.network.device2 in host_details due to exception ", e);
            }
        } else if (Config.SecStorageSecureCopyCert.key().equalsIgnoreCase(name)) {
            //FIXME - Ideally there should be a listener model to listen to global config changes and be able to take action gracefully.
            //Expire the download urls
            final String sqlTemplate = "update template_store_ref set download_url_created=?";
            final String sqlVolume = "update volume_store_ref set download_url_created=?";
            try {
                // Change for templates
                pstmt = txn.prepareAutoCloseStatement(sqlTemplate);
                pstmt.setDate(1, new Date(-1l));// Set the time before the epoch time.
                pstmt.executeUpdate();
                // Change for volumes
                pstmt = txn.prepareAutoCloseStatement(sqlVolume);
                pstmt.setDate(1, new Date(-1l));// Set the time before the epoch time.
                pstmt.executeUpdate();
                // Cleanup the download urls
                _storageManager.cleanupDownloadUrls();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to clean up download URLs in template_store_ref or volume_store_ref due to exception ", e);
            }
        }

        txn.commit();
        messageBus.publish(_name, EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, PublishScope.GLOBAL, name);
        return _configDao.getValue(name);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, eventDescription = "updating configuration")
    public Configuration updateConfiguration(final UpdateCfgCmd cmd) throws InvalidParameterValueException {
        final Long userId = CallContext.current().getCallingUserId();
        final String name = cmd.getCfgName();
        String value = cmd.getValue();
        final Long zoneId = cmd.getZoneId();
        final Long clusterId = cmd.getClusterId();
        final Long storagepoolId = cmd.getStoragepoolId();
        final Long imageStoreId = cmd.getImageStoreId();
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        CallContext.current().setEventDetails(" Name: " + name + " New Value: " + (name.toLowerCase().contains("password") ? "*****" : value == null ? "" : value));
        // check if config value exists
        final ConfigurationVO config = _configDao.findByName(name);
        String catergory = null;

        final Account caller = CallContext.current().getCallingAccount();
        if (_accountMgr.isDomainAdmin(caller.getId())) {
            if (accountId == null && domainId == null) {
                domainId = caller.getDomainId();
            }
        } else if (_accountMgr.isNormalUser(caller.getId())) {
            if (accountId == null) {
                accountId = caller.getAccountId();
            }
        }

        // FIX ME - All configuration parameters are not moved from config.java to configKey
        if (config == null) {
            if (_configDepot.get(name) == null) {
                s_logger.warn("Probably the component manager where configuration variable " + name + " is defined needs to implement Configurable interface");
                throw new InvalidParameterValueException("Config parameter with name " + name + " doesn't exist");
            }
            catergory = _configDepot.get(name).category();
        } else {
            catergory = config.getCategory();
        }

        if (value == null) {
            return _configDao.findByName(name);
        }

        String scope = null;
        Long id = null;
        int paramCountCheck = 0;

        if (zoneId != null) {
            scope = ConfigKey.Scope.Zone.toString();
            id = zoneId;
            paramCountCheck++;
        }
        if (clusterId != null) {
            scope = ConfigKey.Scope.Cluster.toString();
            id = clusterId;
            paramCountCheck++;
        }
        if (accountId != null) {
            Account account = _accountMgr.getAccount(accountId);
            _accountMgr.checkAccess(caller, null, false, account);
            scope = ConfigKey.Scope.Account.toString();
            id = accountId;
            paramCountCheck++;
        }
        if (domainId != null) {
            _accountMgr.checkAccess(caller, _domainDao.findById(domainId));
            scope = ConfigKey.Scope.Domain.toString();
            id = domainId;
            paramCountCheck++;
        }
        if (storagepoolId != null) {
            scope = ConfigKey.Scope.StoragePool.toString();
            id = storagepoolId;
            paramCountCheck++;
        }
        if (imageStoreId != null) {
            scope = ConfigKey.Scope.ImageStore.toString();
            id = imageStoreId;
            paramCountCheck++;
        }

        if (paramCountCheck > 1) {
            throw new InvalidParameterValueException("cannot handle multiple IDs, provide only one ID corresponding to the scope");
        }

        value = value.trim();

        if (value.isEmpty() || value.equals("null")) {
            value = (id == null) ? null : "";
        }

        final String updatedValue = updateConfiguration(userId, name, catergory, value, scope, id);
        if (value == null && updatedValue == null || updatedValue.equalsIgnoreCase(value)) {
            return _configDao.findByName(name);
        } else {
            throw new CloudRuntimeException("Unable to update configuration parameter " + name);
        }
    }

    private ParamCountPair getParamCount(Map<String, Long> scopeMap) {
        Long id = null;
        int paramCount = 0;
        String scope = ConfigKey.Scope.Global.toString();

        for (var entry : scopeMap.entrySet()) {
            if (entry.getValue() != null) {
                id = entry.getValue();
                scope = entry.getKey();
                paramCount++;
            }
        }

        return new ParamCountPair(id, paramCount, scope);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, eventDescription = "resetting configuration")
    public Pair<Configuration, String> resetConfiguration(final ResetCfgCmd cmd) throws InvalidParameterValueException {
        final Long userId = CallContext.current().getCallingUserId();
        final String name = cmd.getCfgName();
        final Long zoneId = cmd.getZoneId();
        final Long clusterId = cmd.getClusterId();
        final Long storagepoolId = cmd.getStoragepoolId();
        final Long accountId = cmd.getAccountId();
        final Long domainId = cmd.getDomainId();
        final Long imageStoreId = cmd.getImageStoreId();
        ConfigKey<?> configKey = null;
        Optional optionalValue;
        String defaultValue;
        String category;
        String configScope;
        final ConfigurationVO config = _configDao.findByName(name);
        if (config == null) {
            configKey = _configDepot.get(name);
            if (configKey == null) {
                s_logger.warn("Probably the component manager where configuration variable " + name + " is defined needs to implement Configurable interface");
                throw new InvalidParameterValueException("Config parameter with name " + name + " doesn't exist");
            }
            defaultValue = configKey.defaultValue();
            category = configKey.category();
            configScope = configKey.scope().toString();
        } else {
            defaultValue = config.getDefaultValue();
            category = config.getCategory();
            configScope = config.getScope();
        }

        String scope = "";
        Map<String, Long> scopeMap = new LinkedHashMap<>();

        Long id = null;
        int paramCountCheck = 0;

        scopeMap.put(ConfigKey.Scope.Zone.toString(), zoneId);
        scopeMap.put(ConfigKey.Scope.Cluster.toString(), clusterId);
        scopeMap.put(ConfigKey.Scope.Domain.toString(), domainId);
        scopeMap.put(ConfigKey.Scope.Account.toString(), accountId);
        scopeMap.put(ConfigKey.Scope.StoragePool.toString(), storagepoolId);
        scopeMap.put(ConfigKey.Scope.ImageStore.toString(), imageStoreId);

        ParamCountPair paramCountPair = getParamCount(scopeMap);
        id = paramCountPair.getId();
        paramCountCheck = paramCountPair.getParamCount();
        scope = paramCountPair.getScope();

        if (paramCountCheck > 1) {
            throw new InvalidParameterValueException("cannot handle multiple IDs, provide only one ID corresponding to the scope");
        }

        if (scope != null && !scope.equals(ConfigKey.Scope.Global.toString()) && !configScope.contains(scope)) {
            throw new InvalidParameterValueException("Invalid scope id provided for the parameter " + name);
        }

        String newValue = null;
        switch (ConfigKey.Scope.valueOf(scope)) {
            case Zone:
                final DataCenterVO zone = _zoneDao.findById(id);
                if (zone == null) {
                    throw new InvalidParameterValueException("unable to find zone by id " + id);
                }
                _dcDetailsDao.removeDetail(id, name);
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id): config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            case Cluster:
                final ClusterVO cluster = _clusterDao.findById(id);
                if (cluster == null) {
                    throw new InvalidParameterValueException("unable to find cluster by id " + id);
                }
                ClusterDetailsVO clusterDetailsVO = _clusterDetailsDao.findDetail(id, name);
                newValue = configKey != null ? configKey.value().toString() : config.getValue();
                if (name.equalsIgnoreCase("cpu.overprovisioning.factor") || name.equalsIgnoreCase("mem.overprovisioning.factor")) {
                    _clusterDetailsDao.persist(id, name, newValue);
                } else if (clusterDetailsVO != null) {
                    _clusterDetailsDao.remove(clusterDetailsVO.getId());
                }
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id): config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            case StoragePool:
                final StoragePoolVO pool = _storagePoolDao.findById(id);
                if (pool == null) {
                    throw new InvalidParameterValueException("unable to find storage pool by id " + id);
                }
                _storagePoolDetailsDao.removeDetail(id, name);
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id) : config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            case Domain:
                final DomainVO domain = _domainDao.findById(id);
                if (domain == null) {
                    throw new InvalidParameterValueException("unable to find domain by id " + id);
                }
                DomainDetailVO domainDetailVO = _domainDetailsDao.findDetail(id, name);
                if (domainDetailVO != null) {
                    _domainDetailsDao.remove(domainDetailVO.getId());
                }
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id) : config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            case Account:
                final AccountVO account = _accountDao.findById(id);
                if (account == null) {
                    throw new InvalidParameterValueException("unable to find account by id " + id);
                }
                AccountDetailVO accountDetailVO = _accountDetailsDao.findDetail(id, name);
                if (accountDetailVO != null) {
                    _accountDetailsDao.remove(accountDetailVO.getId());
                }
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id) : config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            case ImageStore:
                final ImageStoreVO imageStoreVO = _imageStoreDao.findById(id);
                if (imageStoreVO == null) {
                    throw new InvalidParameterValueException("unable to find the image store by id " + id);
                }
                ImageStoreDetailVO imageStoreDetailVO = _imageStoreDetailsDao.findDetail(id, name);
                if (imageStoreDetailVO != null) {
                    _imageStoreDetailsDao.remove(imageStoreDetailVO.getId());
                }
                optionalValue = Optional.ofNullable(configKey != null ? configKey.valueIn(id) : config.getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
                break;

            default:
                if (!_configDao.update(name, category, defaultValue)) {
                    s_logger.error("Failed to reset configuration option, name: " + name + ", defaultValue:" + defaultValue);
                    throw new CloudRuntimeException("Failed to reset configuration value. Please contact Cloud Support.");
                }
                optionalValue = Optional.ofNullable(configKey != null ? configKey.value() : _configDao.findByName(name).getValue());
                newValue = optionalValue.isPresent() ? optionalValue.get().toString() : defaultValue;
        }

        CallContext.current().setEventDetails(" Name: " + name + " New Value: " + (name.toLowerCase().contains("password") ? "*****" : defaultValue == null ? "" : defaultValue));
        return new Pair<Configuration, String>(_configDao.findByName(name), newValue);
    }

    private String validateConfigurationValue(final String name, String value, final String scope) {

        final ConfigurationVO cfg = _configDao.findByName(name);
        if (cfg == null) {
            s_logger.error("Missing configuration variable " + name + " in configuration table");
            return "Invalid configuration variable.";
        }

        final String configScope = cfg.getScope();
        if (scope != null) {
            if (!configScope.contains(scope) &&
                    !(ENABLE_ACCOUNT_SETTINGS_FOR_DOMAIN.value() && configScope.contains(ConfigKey.Scope.Account.toString()) &&
                            scope.equals(ConfigKey.Scope.Domain.toString()))) {
                s_logger.error("Invalid scope id provided for the parameter " + name);
                return "Invalid scope id provided for the parameter " + name;
            }
        }
        Class<?> type = null;
        final Config configuration = Config.getConfig(name);
        if (configuration == null) {
            s_logger.warn("Did not find configuration " + name + " in Config.java. Perhaps moved to ConfigDepot");
            final ConfigKey<?> configKey = _configDepot.get(name);
            if(configKey == null) {
                s_logger.warn("Did not find configuration " + name + " in ConfigDepot too.");
                return null;
            }
            type = configKey.type();
        } else {
            type = configuration.getType();
        }
        //no need to validate further if a
        //config can have null value.
        String errMsg = null;
        try {
            if (type.equals(Integer.class)) {
                errMsg = "There was error in trying to parse value: " + value + ". Please enter a valid integer value for parameter " + name;
                Integer.parseInt(value);
            } else if (type.equals(Float.class)) {
                errMsg = "There was error in trying to parse value: " + value + ". Please enter a valid float value for parameter " + name;
                Float.parseFloat(value);
            } else if (type.equals(Long.class)) {
                errMsg = "There was error in trying to parse value: " + value + ". Please enter a valid long value for parameter " + name;
                Long.parseLong(value);
            }
        } catch (final Exception e) {
            // catching generic exception as some throws NullPointerException and some throws NumberFormatExcpeion
            s_logger.error(errMsg);
            return errMsg;
        }

        if (LoadBalancerConfigManager.DefaultLbSSLConfiguration.key().equalsIgnoreCase(name)) {
            if (org.apache.commons.lang3.StringUtils.isBlank(value) || ! SSLConfiguration.validate(value.toLowerCase())) {
                final String msg = "Please enter valid value in " + String.join(",", SSLConfiguration.getValues());
                s_logger.error(msg);
                throw new InvalidParameterValueException(msg);
            }
        }

        if (value == null) {
            if (type.equals(Boolean.class)) {
                return "Please enter either 'true' or 'false'.";
            }
            if (overprovisioningFactorsForValidation.contains(name)) {
                final String msg = "value cannot be null for the parameter " + name;
                s_logger.error(msg);
                return msg;
            }
            return null;
        }

        value = value.trim();
        try {
            if (overprovisioningFactorsForValidation.contains(name) && Float.parseFloat(value) <= 0f) {
                final String msg = name + " should be greater than 0";
                s_logger.error(msg);
                throw new InvalidParameterValueException(msg);
            }
        } catch (final NumberFormatException e) {
            final String msg = "There was an error trying to parse the float value for: " + name;
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (type.equals(Boolean.class)) {
            if (!(value.equals("true") || value.equals("false"))) {
                s_logger.error("Configuration variable " + name + " is expecting true or false instead of " + value);
                return "Please enter either 'true' or 'false'.";
            }
            return null;
        }

        if (type.equals(Integer.class) && NetworkModel.MACIdentifier.key().equalsIgnoreCase(name)) {
            try {
                final int val = Integer.parseInt(value);
                //The value need to be between 0 to 255 because the mac generation needs a value of 8 bit
                //0 value is considered as disable.
                if(val < 0 || val > 255){
                    throw new InvalidParameterValueException(name+" value should be between 0 and 255. 0 value will disable this feature");
                }
            } catch (final NumberFormatException e) {
                s_logger.error("There was an error trying to parse the integer value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the integer value for:" + name);
            }
        }

        if (type.equals(Integer.class) && configValuesForValidation.contains(name)) {
            try {
                final int val = Integer.parseInt(value);
                if (val <= 0) {
                    throw new InvalidParameterValueException("Please enter a positive value for the configuration parameter:" + name);
                }
                if ("vm.password.length".equalsIgnoreCase(name) && val < 6) {
                    throw new InvalidParameterValueException("Please enter a value greater than 5 for the configuration parameter:" + name);
                }
                if ("remote.access.vpn.psk.length".equalsIgnoreCase(name)) {
                    if (val < 8) {
                        throw new InvalidParameterValueException("Please enter a value greater than 7 for the configuration parameter:" + name);
                    }
                    if (val > 256) {
                        throw new InvalidParameterValueException("Please enter a value less than 257 for the configuration parameter:" + name);
                    }
                }
                if (VM_USERDATA_MAX_LENGTH_STRING.equalsIgnoreCase(name)) {
                    if (val > 1048576) {
                        throw new InvalidParameterValueException("Please enter a value less than 1048576 for the configuration parameter:" + name);
                    }
                }
            } catch (final NumberFormatException e) {
                s_logger.error("There was an error trying to parse the integer value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the integer value for:" + name);
            }
        }

        if (type.equals(Float.class)) {
            try {
                final Float val = Float.parseFloat(value);
                if (weightBasedParametersForValidation.contains(name) && (val < 0f || val > 1f)) {
                    throw new InvalidParameterValueException("Please enter a value between 0 and 1 for the configuration parameter: " + name);
                }
            } catch (final NumberFormatException e) {
                s_logger.error("There was an error trying to parse the float value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the float value for:" + name);
            }
        }

        if (configuration == null ) {
            //range validation has to be done per case basis, for now
            //return in case of Configkey parameters
            return null;
        }

        if (configuration.getRange() == null) {
            return null;
        }
        String[] range = configuration.getRange().split(",");

        if (type.equals(String.class)) {
            return validateIfStringValueIsInRange(name, value, range);
        } else if (type.equals(Integer.class)) {
            return validateIfIntValueIsInRange(name, value, range[0]);
        }
        return String.format("Invalid value for configuration [%s].", name);
    }

    /**
     * A valid value should be an integer between min and max (the values from the range).
     */
    protected String validateIfIntValueIsInRange(String name, String value, String range) {
        final String[] options = range.split("-");
        final int min = Integer.parseInt(options[0]);
        final int max = Integer.parseInt(options[1]);
        final int val = Integer.parseInt(value);
        if (val < min || val > max) {
            s_logger.error(String.format("Invalid value for configuration [%s]. Please enter a value in the range [%s].", name, range));
            return String.format("The provided value is not valid for this configuration. Please enter an integer in the range: [%s]", range);
        }
        return null;
    }

    /**
     * Checks if the value for the configuration is valid for any of the ranges selected.
     */
    protected String validateIfStringValueIsInRange(String name, String value, String... range) {
        List<String> message = new ArrayList<String>();
        String errMessage = "";
        for (String rangeOption : range) {
            switch (rangeOption) {
                case "privateip":
                    errMessage = validateRangePrivateIp(name, value);
                    break;
                case "hypervisorList":
                    errMessage = validateRangeHypervisorList(value);
                    break;
                case "instanceName":
                    errMessage = validateRangeInstanceName(value);
                    break;
                case "domainName":
                    errMessage = validateRangeDomainName(value);
                    break;
                default:
                    errMessage = validateRangeOther(name, value, rangeOption);
            }
            if (StringUtils.isEmpty(errMessage)) {
                return null;
            }
            message.add(errMessage);
        }
        if (message.size() == 1) {
            return String.format("The provided value is not %s.", message.get(0));
        }
        return String.format("The provided value is neither %s.", String.join(" NOR ", message));
    }

    /**
     * Checks if the value is a private IP according to {@link NetUtils#isSiteLocalAddress(String)}.
     */
    protected String validateRangePrivateIp(String name, String value) {
        try {
            if (NetUtils.isSiteLocalAddress(value)) {
                return null;
            }
            s_logger.error(String.format("Value [%s] is not a valid private IP range for configuration [%s].", value, name));
        } catch (final NullPointerException e) {
            s_logger.error(String.format("Error while parsing IP address for [%s].", name));
        }
        return "a valid site local IP address";
    }

    /**
     * Valid values are XenServer, KVM, VMware, Hyperv, VirtualBox, Parralels, BareMetal, Simulator, Ovm, Ovm3, LXC.
     * Inputting "Any" will return the hypervisor type Any, other inputs will result in the hypervisor type none.
     * Both of these are invalid values and will return an error message.
     */
    protected String validateRangeHypervisorList(String value) {
        final String[] hypervisors = value.split(",");
        for (final String hypervisor : hypervisors) {
            if (HypervisorType.getType(hypervisor) == HypervisorType.Any || HypervisorType.getType(hypervisor) == HypervisorType.None) {
                return "a valid hypervisor type";
            }
        }
        return null;
    }

    /**
     * Valid values are instance names, the only restriction is that they may not have hyphens, spaces or plus signs.
     */
    protected String validateRangeInstanceName(String value) {
        if (NetUtils.verifyInstanceName(value)) {
            return null;
        }
        return "a valid instance name (instance names cannot contain hyphens, spaces or plus signs)";
    }

    /**
     * Verifies if the value is a valid domain name. If it starts with "*.", these two symbols are ignored and do not count towards the character limit.
     * Max length for FQDN is 253 + 2, code adds xxx-xxx-xxx-xxx to domain name when creating URL.
     */
    protected String validateRangeDomainName(String value) {
        String domainName = value;
        if (value.startsWith("*")) {
            domainName = value.substring(2);
        }
        if (domainName.length() >= 238 || !domainName.matches(DOMAIN_NAME_PATTERN)) {
            return "a valid domain name";
        }
        return null;
    }

    /**
     * In configurations where this type of range is used, a list of possible values is passed as argument in the creation of the configuration,
     * a valid value is any option within this list.
     */
    protected String validateRangeOther(String name, String value, String rangeOption) {
        final String[] options = rangeOption.split(",");
        for (final String option : options) {
            if (option.trim().equalsIgnoreCase(value)) {
                return null;
            }
        }
        s_logger.error(String.format("Invalid value for configuration [%s].", name));
        return String.format("a valid value for this configuration (Options are: [%s])", rangeOption);
    }


    private boolean podHasAllocatedPrivateIPs(final long podId) {
        final HostPodVO pod = _podDao.findById(podId);
        final int count = _privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true);
        return count > 0;
    }

    protected void checkIfPodIsDeletable(final long podId) {
        final HostPodVO pod = _podDao.findById(podId);

        final String errorMsg = "The pod cannot be deleted because ";

        // Check if there are allocated private IP addresses in the pod
        if (_privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true) != 0) {
            throw new CloudRuntimeException(errorMsg + "there are private IP addresses allocated in this pod.");
        }

        // Check if there are any non-removed volumes in the pod.
        if (!_volumeDao.findByPod(podId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are storage volumes in this pod.");
        }

        // Check if there are any non-removed hosts in the pod.
        if (!_hostDao.findByPodId(podId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are servers in this pod.");
        }

        // Check if there are any non-removed vms in the pod.
        if (!_vmInstanceDao.listByPodId(podId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are virtual machines in this pod.");
        }

        // Check if there are any non-removed clusters in the pod.
        if (!_clusterDao.listByPodId(podId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are clusters in this pod.");
        }
    }

    private void checkPodAttributesForNonEdgeZone(final long podId, final String podName, final DataCenter zone, final String gateway,
          final String cidr, final String startIp, final String endIp, final boolean skipGatewayOverlapCheck) {

        String cidrAddress;
        long cidrSize;
        // Get the individual cidrAddress and cidrSize values, if the CIDR is
        // valid. If it's not valid, return an error.
        if (NetUtils.isValidIp4Cidr(cidr)) {
            cidrAddress = getCidrAddress(cidr);
            cidrSize = getCidrSize(cidr);
        } else {
            throw new InvalidParameterValueException("Please enter a valid CIDR for pod: " + podName);
        }

        // Check if the IP range is valid
        checkIpRange(startIp, endIp, cidrAddress, cidrSize);

        // Check if the IP range overlaps with the public ip
        if (StringUtils.isNotEmpty(startIp)) {
            checkOverlapPublicIpRange(zone.getId(), startIp, endIp);
        }

        // Check if the gateway is a valid IP address
        if (!NetUtils.isValidIp4(gateway)) {
            throw new InvalidParameterValueException("The gateway is not a valid IP address.");
        }

        // Check if the gateway is in the CIDR subnet
        if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
        }

        // Don't allow gateway to overlap with start/endIp
        if (!skipGatewayOverlapCheck) {
            if (NetUtils.ipRangesOverlap(startIp, endIp, gateway, gateway)) {
                throw new InvalidParameterValueException("The gateway shouldn't overlap start/end ip addresses");
            }
        }

        final String checkPodCIDRs = _configDao.getValue("check.pod.cidrs");
        if (checkPodCIDRs == null || checkPodCIDRs.trim().isEmpty() || Boolean.parseBoolean(checkPodCIDRs)) {
            checkPodCidrSubnets(zone.getId(), podId, cidr);
        }
    }

    private void checkPodAttributes(final long podId, final String podName, final DataCenter zone, final String gateway, final String cidr, final String startIp, final String endIp, final String allocationStateStr,
            final boolean checkForDuplicates, final boolean skipGatewayOverlapCheck) {
        if (checkForDuplicates) {
            // Check if the pod already exists
            if (validPod(podName, zone.getId())) {
                throw new InvalidParameterValueException("A pod with name: " + podName + " already exists in zone " + zone.getId() + ". Please specify a different pod name. ");
            }
        }

        if (!DataCenter.Type.Edge.equals(zone.getType())) {
            checkPodAttributesForNonEdgeZone(podId, podName, zone, gateway, cidr, startIp, endIp, skipGatewayOverlapCheck);
        }

        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (final IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_POD_DELETE, eventDescription = "deleting pod", async = false)
    public boolean deletePod(final DeletePodCmd cmd) {
        final Long podId = cmd.getId();

        // Make sure the pod exists
        if (!validPod(podId)) {
            throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
        }

        checkIfPodIsDeletable(podId);

        final HostPodVO pod = _podDao.findById(podId);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                // Delete private ip addresses for the pod if there are any
                final List<DataCenterIpAddressVO> privateIps = _privateIpAddressDao.listByPodIdDcId(podId, pod.getDataCenterId());
                if (!privateIps.isEmpty()) {
                    if (!_privateIpAddressDao.deleteIpAddressByPod(podId)) {
                        throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
                    }
                }

                // Delete link local ip addresses for the pod
                final List<DataCenterLinkLocalIpAddressVO> localIps = _linkLocalIpAllocDao.listByPodIdDcId(podId, pod.getDataCenterId());
                if (!localIps.isEmpty()) {
                    if (!_linkLocalIpAllocDao.deleteIpAddressByPod(podId)) {
                        throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
                    }
                }

                // Delete vlans associated with the pod
                final List<? extends Vlan> vlans = _networkModel.listPodVlans(podId);
                if (vlans != null && !vlans.isEmpty()) {
                    for (final Vlan vlan : vlans) {
                        _vlanDao.remove(vlan.getId());
                    }
                }

                // Delete corresponding capacity records
                _capacityDao.removeBy(null, null, podId, null, null);

                // Delete the pod
                if (!_podDao.remove(podId)) {
                    throw new CloudRuntimeException("Failed to delete pod " + podId);
                }

                // remove from dedicated resources
                final DedicatedResourceVO dr = _dedicatedDao.findByPodId(podId);
                if (dr != null) {
                    _dedicatedDao.remove(dr.getId());
                }

                // Remove comments (if any)
                annotationDao.removeByEntityType(AnnotationService.EntityType.POD.name(), pod.getUuid());
            }
        });

        messageBus.publish(_name, MESSAGE_DELETE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, pod);

        return true;
    }

    /**
     * Get vlan number from vlan uri
     * @param vlan
     * @return
     */
    protected String getVlanNumberFromUri(String vlan) {
        URI uri;
        try {
            uri = new URI(vlan);
            String vlanId = BroadcastDomainType.getValue(uri);
            if (vlanId == null || !uri.getScheme().equalsIgnoreCase("vlan")) {
                throw new CloudRuntimeException("Vlan parameter : " + vlan + " is not in valid format");
            }
            return vlanId;
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Invalid vlan parameter: " + vlan + " can't get vlan number from it due to: " + e.getMessage());
        }
    }

    @Override
    @DB
    public Pod createPodIpRange(final CreateManagementNetworkIpRangeCmd cmd) {

        final Account account = CallContext.current().getCallingAccount();

        if(!_accountMgr.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Calling account is not root admin: " + account.getId());
        }

        final long podId = cmd.getPodId();
        final String gateway = cmd.getGateWay();
        final String netmask = cmd.getNetmask();
        final String startIp = cmd.getStartIp();
        String endIp = cmd.getEndIp();
        final boolean forSystemVms = cmd.isForSystemVms();
        String vlan = cmd.getVlan();
        if (StringUtils.isNotEmpty(vlan) && !vlan.startsWith(BroadcastDomainType.Vlan.scheme())) {
            vlan = BroadcastDomainType.Vlan.toUri(vlan).toString();
        }

        String vlanNumberFromUri = getVlanNumberFromUri(vlan);
        final Integer vlanId = vlanNumberFromUri.equals(Vlan.UNTAGGED.toString()) ? null : Integer.parseInt(vlanNumberFromUri);

        final HostPodVO pod = _podDao.findById(podId);

        if(pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by ID: " + podId);
        }

        final long zoneId = pod.getDataCenterId();

        if(!NetUtils.isValidIp4(gateway) && !NetUtils.isValidIp6(gateway)) {
            throw new InvalidParameterValueException("The gateway IP address is invalid.");
        }

        if(!NetUtils.isValidIp4Netmask(netmask)) {
            throw new InvalidParameterValueException("The netmask IP address is invalid.");
        }

        if(endIp == null) {
            endIp = startIp;
        }

        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);

        if(!NetUtils.isValidIp4Cidr(cidr)) {
            throw new InvalidParameterValueException("The CIDR is invalid " + cidr);
        }

        final String cidrAddress = pod.getCidrAddress();
        final long cidrSize = pod.getCidrSize();

        // Because each pod has only one Gateway and Netmask.
        if (!gateway.equals(pod.getGateway())) {
            throw new InvalidParameterValueException("Multiple gateways for the POD: " + pod.getId() + " are not allowed. The Gateway should be same as the existing Gateway " + pod.getGateway());
        }

        if (!netmask.equals(NetUtils.getCidrNetmask(cidrSize))) {
            throw new InvalidParameterValueException("Multiple subnets for the POD: " + pod.getId() + " are not allowed. The Netmask should be same as the existing Netmask " + NetUtils.getCidrNetmask(cidrSize));
        }

        // Check if the IP range is valid.
        checkIpRange(startIp, endIp, cidrAddress, cidrSize);

        // Check if the IP range overlaps with the public ip.
        checkOverlapPublicIpRange(zoneId, startIp, endIp);

        // Check if the gateway is in the CIDR subnet
        if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
        }

        if (NetUtils.ipRangesOverlap(startIp, endIp, gateway, gateway)) {
            throw new InvalidParameterValueException("The gateway shouldn't overlap start/end ip addresses");
        }

        final String[] existingPodIpRanges = pod.getDescription().split(",");

        for(String podIpRange: existingPodIpRanges) {
            final String[] existingPodIpRange = podIpRange.split("-");

            if (existingPodIpRange.length > 1) {
                if (!NetUtils.isValidIp4(existingPodIpRange[0]) || !NetUtils.isValidIp4(existingPodIpRange[1])) {
                    continue;
                }
                // Check if the range overlaps with any existing range.
                if (NetUtils.ipRangesOverlap(startIp, endIp, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new InvalidParameterValueException("The new range overlaps with existing range. Please add a mutually exclusive range.");
                }
            }
        }

        try {
            final String endIpFinal = endIp;

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    String ipRange = pod.getDescription();

                    /*
                     * POD Description is refactored to:
                     * <START_IP>-<END_IP>-<FOR_SYSTEM_VMS>-<VLAN>,<START_IP>-<END_IP>-<FOR_SYSTEM_VMS>-<VLAN>,...
                    */
                    String range = startIp + "-" + endIpFinal + "-" + (forSystemVms ? "1" : "0") + "-" + (vlanId == null ? DefaultVlanForPodIpRange : vlanId);
                    if(ipRange != null && !ipRange.isEmpty())
                        ipRange += ("," + range);
                    else
                        ipRange = (range);

                    pod.setDescription(ipRange);

                    HostPodVO lock = null;
                    try {
                        lock = _podDao.acquireInLockTable(podId);

                        if (lock == null) {
                            String msg = "Unable to acquire lock on table to update the ip range of POD: " + pod.getName() + ", Creation failed.";
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }

                        _podDao.update(podId, pod);
                    } finally {
                        if (lock != null) {
                            _podDao.releaseFromLockTable(podId);
                        }
                    }

                    _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIpFinal, forSystemVms, vlanId);
                }
            });
        } catch (final Exception e) {
            s_logger.error("Unable to create Pod IP range due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to create Pod IP range. Please contact Cloud Support.");
        }

        messageBus.publish(_name, MESSAGE_CREATE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, pod);

        return pod;
    }

    @Override
    @DB
    public void deletePodIpRange(final DeleteManagementNetworkIpRangeCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        final long podId = cmd.getPodId();
        final String startIp = cmd.getStartIp();
        final String endIp = cmd.getEndIp();
        String vlan = cmd.getVlan();
        try {
            vlan = BroadcastDomainType.getValue(vlan);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Incorrect vlan " + vlan);
        }

        final HostPodVO pod = _podDao.findById(podId);

        if(pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id " + podId);
        }

        if (startIp == null || !NetUtils.isValidIp4(startIp)) {
            throw new InvalidParameterValueException("The start address of the IP range is not a valid IP address.");
        }

        if (endIp == null || !NetUtils.isValidIp4(endIp)) {
            throw new InvalidParameterValueException("The end address of the IP range is not a valid IP address.");
        }

        if (NetUtils.ip2Long(startIp) > NetUtils.ip2Long(endIp)) {
            throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
        }

        for(long ipAddr = NetUtils.ip2Long(startIp); ipAddr <= NetUtils.ip2Long(endIp); ipAddr++) {
            if(_privateIpAddressDao.countIpAddressUsage(NetUtils.long2Ip(ipAddr), podId, pod.getDataCenterId(), true) > 0) {
                throw new CloudRuntimeException("Some IPs of the range has been allocated, so it cannot be deleted.");
            }
        }

        final String[] existingPodIpRanges = pod.getDescription().split(",");

        if(existingPodIpRanges.length == 0) {
            throw new InvalidParameterValueException("The IP range cannot be found. As the existing IP range is empty.");
        }

        final String[] newPodIpRanges = new String[existingPodIpRanges.length-1];
        int index = existingPodIpRanges.length-2;
        boolean foundRange = false;

        for(String podIpRange: existingPodIpRanges) {
            final String[] existingPodIpRange = podIpRange.split("-");

            if(existingPodIpRange.length > 1) {
                if (startIp.equals(existingPodIpRange[0]) && endIp.equals(existingPodIpRange[1]) &&
                        (existingPodIpRange.length > 3 ? vlan.equals(existingPodIpRange[3]) : vlan.equals(DefaultVlanForPodIpRange))) {
                    foundRange = true;
                } else if (index >= 0) {
                    newPodIpRanges[index--] = (existingPodIpRange[0] + "-" + existingPodIpRange[1] + "-" +
                            (existingPodIpRange.length > 2 ? existingPodIpRange[2] : DefaultForSystemVmsForPodIpRange) + "-" +
                            (existingPodIpRange.length > 3 ? existingPodIpRange[3] : DefaultVlanForPodIpRange));
                }
            }
        }

        if(!foundRange) {
            throw new InvalidParameterValueException("The input IP range: " + startIp + "-" + endIp + " of pod: " + podId + "is not present. Please input an existing range.");
        }

        final StringBuilder newPodIpRange = new StringBuilder();
        boolean first = true;
        for (String podIpRange : newPodIpRanges) {
            if (first)
                first = false;
            else
                newPodIpRange.append(",");

            newPodIpRange.append(podIpRange);
        }

        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    pod.setDescription(newPodIpRange.toString());

                    HostPodVO lock = null;
                    try {
                        lock = _podDao.acquireInLockTable(podId);

                        if (lock == null) {
                            String msg = "Unable to acquire lock on table to update the ip range of POD: " + pod.getName() + ", Deletion failed.";
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }

                        _podDao.update(podId, pod);
                    } finally {
                        if (lock != null) {
                            _podDao.releaseFromLockTable(podId);
                        }
                    }

                    for(long ipAddr = NetUtils.ip2Long(startIp); ipAddr <= NetUtils.ip2Long(endIp); ipAddr++) {
                        if (!_privateIpAddressDao.deleteIpAddressByPodDc(NetUtils.long2Ip(ipAddr), podId, pod.getDataCenterId())) {
                            throw new CloudRuntimeException("Failed to cleanup private ip address: " + NetUtils.long2Ip(ipAddr) + " of Pod: " + podId + " DC: " + pod.getDataCenterId());
                        }
                    }
                }
            });
        } catch (final Exception e) {
            s_logger.error("Unable to delete Pod " + podId + "IP range due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete Pod " + podId + "IP range. Please contact Cloud Support.");
        }

        messageBus.publish(_name, MESSAGE_DELETE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, pod);
    }

    @Override
    @DB
    public void updatePodIpRange(final UpdatePodManagementNetworkIpRangeCmd cmd) throws ConcurrentOperationException {
        final long podId = cmd.getPodId();
        final HostPodVO pod = _podDao.findById(podId);
        if (pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id: " + podId);
        }

        final String currentStartIP = cmd.getCurrentStartIP();
        final String currentEndIP = cmd.getCurrentEndIP();
        String newStartIP = cmd.getNewStartIP();
        String newEndIP = cmd.getNewEndIP();

        if (newStartIP == null) {
            newStartIP = currentStartIP;
        }

        if (newEndIP == null) {
            newEndIP = currentEndIP;
        }

        if (newStartIP.equals(currentStartIP) && newEndIP.equals(currentEndIP)) {
            throw new InvalidParameterValueException("New starting and ending IP address are the same as current starting and ending IP address");
        }

        final String[] existingPodIpRanges = pod.getDescription().split(",");
        if (existingPodIpRanges.length == 0) {
            throw new InvalidParameterValueException("The IP range cannot be found in the pod: " + podId + " since the existing IP range is empty.");
        }

        verifyIpRangeParameters(currentStartIP,currentEndIP);
        verifyIpRangeParameters(newStartIP,newEndIP);
        checkIpRangeContainsTakenAddresses(pod,currentStartIP,currentEndIP,newStartIP,newEndIP);

        String vlan = verifyPodIpRangeExists(podId,existingPodIpRanges,currentStartIP,currentEndIP,newStartIP,newEndIP);

        List<Long> currentIpRange = listAllIPsWithintheRange(currentStartIP,currentEndIP);
        List<Long> newIpRange = listAllIPsWithintheRange(newStartIP,newEndIP);

        try {
            final String finalNewEndIP = newEndIP;
            final String finalNewStartIP = newStartIP;
            final Integer vlanId = vlan.equals(Vlan.UNTAGGED) ? null : Integer.parseInt(vlan);

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    final long zoneId = pod.getDataCenterId();
                    pod.setDescription(pod.getDescription().replace(currentStartIP + "-",
                            finalNewStartIP + "-").replace(currentEndIP, finalNewEndIP));
                    updatePodIpRangeInDb(zoneId,podId,vlanId,pod,newIpRange,currentIpRange);
                }
            });
        } catch (final Exception e) {
            s_logger.error("Unable to update Pod " + podId + " IP range due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to update Pod " + podId + " IP range. Please contact Cloud Support.");
        }
    }

    private String verifyPodIpRangeExists(long podId,String[] existingPodIpRanges, String currentStartIP,
            String currentEndIP, String newStartIP, String newEndIP) {
        boolean foundRange = false;
        String vlan = null;

        for (String podIpRange: existingPodIpRanges) {
            final String[] existingPodIpRange = podIpRange.split("-");

            if (existingPodIpRange.length > 1) {
                if (!NetUtils.isValidIp4(existingPodIpRange[0]) || !NetUtils.isValidIp4(existingPodIpRange[1])) {
                    continue;
                }
                if (currentStartIP.equals(existingPodIpRange[0]) && currentEndIP.equals(existingPodIpRange[1])) {
                    foundRange = true;
                    vlan = existingPodIpRange[3];
                }
                if (!foundRange && NetUtils.ipRangesOverlap(newStartIP, newEndIP, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new InvalidParameterValueException("The Start and End IP address range: (" + newStartIP + "-" + newEndIP + ") overlap with the pod IP range: " + podIpRange);
                }
            }
        }

        if (!foundRange) {
            throw new InvalidParameterValueException("The input IP range: " + currentStartIP + "-" + currentEndIP + " of pod: " + podId + " is not present. Please input an existing range.");
        }

        return vlan;
    }

    private void updatePodIpRangeInDb (long zoneId, long podId, Integer vlanId, HostPodVO pod, List<Long> newIpRange, List<Long> currentIpRange) {
        HostPodVO lock = null;
        try {
            lock = _podDao.acquireInLockTable(podId);
            if (lock == null) {
                String msg = "Unable to acquire lock on table to update the ip range of POD: " + pod.getName() + ", Update failed.";
                s_logger.warn(msg);
                throw new CloudRuntimeException(msg);
            }
            List<Long> iPaddressesToAdd = new ArrayList(newIpRange);
            iPaddressesToAdd.removeAll(currentIpRange);
            if (iPaddressesToAdd.size() > 0) {
                for (Long startIP : iPaddressesToAdd) {
                    _zoneDao.addPrivateIpAddress(zoneId, podId, NetUtils.long2Ip(startIP), NetUtils.long2Ip(startIP), false, vlanId);
                }
            } else {
                currentIpRange.removeAll(newIpRange);
                if (currentIpRange.size() > 0) {
                    for (Long startIP: currentIpRange) {
                        if (!_privateIpAddressDao.deleteIpAddressByPodDc(NetUtils.long2Ip(startIP),podId,zoneId)) {
                            throw new CloudRuntimeException("Failed to remove private ip address: " + NetUtils.long2Ip(startIP) + " of Pod: " + podId + " DC: " + pod.getDataCenterId());
                        }
                    }
                }
            }
            _podDao.update(podId, pod);
        } catch (final Exception e) {
            s_logger.error("Unable to update Pod " + podId + " IP range due to database error " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to update Pod " + podId + " IP range. Please contact Cloud Support.");
        }  finally {
            if (lock != null) {
                _podDao.releaseFromLockTable(podId);
            }
        }
    }

    private List<Long> listAllIPsWithintheRange(String startIp, String endIP) {
        verifyIpRangeParameters(startIp,endIP);
        long startIpLong = NetUtils.ip2Long(startIp);
        long endIpLong = NetUtils.ip2Long(endIP);

        List<Long> listOfIpsinRange = new ArrayList<>();
        while (startIpLong <= endIpLong) {
            listOfIpsinRange.add(startIpLong);
            startIpLong++;
        }
        return listOfIpsinRange;
    }

    private void verifyIpRangeParameters(String startIP, String endIp) {

        if (StringUtils.isNotEmpty(startIP) && !NetUtils.isValidIp4(startIP)) {
            throw new InvalidParameterValueException("The current start address of the IP range " + startIP + " is not a valid IP address.");
        }

        if (StringUtils.isNotEmpty(endIp) && !NetUtils.isValidIp4(endIp)) {
            throw new InvalidParameterValueException("The current end address of the IP range " + endIp + " is not a valid IP address.");
        }

        if (NetUtils.ip2Long(startIP) > NetUtils.ip2Long(endIp)) {
            throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
        }
    }

    private void checkIpRangeContainsTakenAddresses(final HostPodVO pod,final String currentStartIP,
            final String currentEndIP,final String newStartIp, final String newEndIp) {

        List<Long> newIpRange = listAllIPsWithintheRange(newStartIp,newEndIp);
        List<Long> currentIpRange = listAllIPsWithintheRange(currentStartIP,currentEndIP);
        List<Long> takenIpsList = new ArrayList<>();
        final List<DataCenterIpAddressVO> takenIps = _privateIpAddressDao.listIpAddressUsage(pod.getId(),pod.getDataCenterId(),true);

        for (DataCenterIpAddressVO takenIp : takenIps) {
            takenIpsList.add(NetUtils.ip2Long(takenIp.getIpAddress()));
        }

        takenIpsList.retainAll(currentIpRange);
        if (!newIpRange.containsAll(takenIpsList)) {
            throw new InvalidParameterValueException("The IP range does not contain some IP addresses that have "
                    + "already been taken. Please adjust your IP range to include all IP addresses already taken.");
        }
    }

    @Override
    @DB
    public DataCenterGuestIpv6Prefix createDataCenterGuestIpv6Prefix(final CreateGuestNetworkIpv6PrefixCmd cmd) throws ConcurrentOperationException {
        final long zoneId = cmd.getZoneId();
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id: " + zoneId);
        }
        final String prefix = cmd.getPrefix();
        IPv6Network prefixNet = IPv6Network.fromString(prefix);
        if (prefixNet.getNetmask().asPrefixLength() > Ipv6Service.IPV6_SLAAC_CIDR_NETMASK) {
            throw new InvalidParameterValueException(String.format("IPv6 prefix must be /%d or less", Ipv6Service.IPV6_SLAAC_CIDR_NETMASK));
        }
        List<DataCenterGuestIpv6PrefixVO> existingPrefixes = dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId);
        for (DataCenterGuestIpv6PrefixVO existingPrefix : existingPrefixes) {
            IPv6Network existingPrefixNet = IPv6Network.fromString(existingPrefix.getPrefix());
            if (NetUtils.ipv6NetworksOverlap(existingPrefixNet, prefixNet)) {
                throw new InvalidParameterValueException(String.format("IPv6 prefix %s overlaps with the existing IPv6 prefix %s", prefixNet, existingPrefixNet));
            }
        }
        DataCenterGuestIpv6Prefix dataCenterGuestIpv6Prefix = null;
        try {
            dataCenterGuestIpv6Prefix = Transaction.execute(new TransactionCallback<DataCenterGuestIpv6Prefix>() {
                @Override
                public DataCenterGuestIpv6Prefix doInTransaction(TransactionStatus status) {
                    DataCenterGuestIpv6PrefixVO dataCenterGuestIpv6PrefixVO = new DataCenterGuestIpv6PrefixVO(zoneId, prefix);
                    dataCenterGuestIpv6PrefixDao.persist(dataCenterGuestIpv6PrefixVO);
                    return dataCenterGuestIpv6PrefixVO;
                }
            });
        } catch (final Exception e) {
            s_logger.error(String.format("Unable to add IPv6 prefix for zone: %s due to %s", zone, e.getMessage()), e);
            throw new CloudRuntimeException(String.format("Unable to add IPv6 prefix for zone ID: %s. Please contact Cloud Support.", zone.getUuid()));
        }
        return dataCenterGuestIpv6Prefix;
    }

    @Override
    public List<? extends DataCenterGuestIpv6Prefix> listDataCenterGuestIpv6Prefixes(final ListGuestNetworkIpv6PrefixesCmd cmd) throws ConcurrentOperationException {
        final Long id = cmd.getId();
        final Long zoneId = cmd.getZoneId();
        if (id != null) {
            DataCenterGuestIpv6PrefixVO prefix = dataCenterGuestIpv6PrefixDao.findById(id);
            List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
            if (prefix != null) {
                prefixes.add(prefix);
            }
            return prefixes;
        }
        if (zoneId != null) {
            final DataCenterVO zone = _zoneDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id: " + zoneId);
            }
            return dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId);
        }
        return dataCenterGuestIpv6PrefixDao.listAll();
    }

    @Override
    public boolean deleteDataCenterGuestIpv6Prefix(DeleteGuestNetworkIpv6PrefixCmd cmd) {
        final long prefixId = cmd.getId();
        final DataCenterGuestIpv6PrefixVO prefix = dataCenterGuestIpv6PrefixDao.findById(prefixId);
        if (prefix == null) {
            throw new InvalidParameterValueException("Unable to find guest network IPv6 prefix by id: " + prefixId);
        }
        List<Ipv6GuestPrefixSubnetNetworkMapVO> prefixSubnets = ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefixId);
        if (CollectionUtils.isNotEmpty(prefixSubnets)) {
            List<String> usedSubnets = prefixSubnets.stream().map(Ipv6GuestPrefixSubnetNetworkMapVO::getSubnet).collect(Collectors.toList());
            s_logger.error(String.format("Subnets for guest IPv6 prefix {ID: %s, %s} are in use: %s", prefix.getUuid(), prefix.getPrefix(), String.join(", ", usedSubnets)));
            throw new CloudRuntimeException(String.format("Unable to delete guest network IPv6 prefix ID: %s. Prefix subnets are in use.", prefix.getUuid()));
        }
        ipv6GuestPrefixSubnetNetworkMapDao.deleteByPrefixId(prefixId);
        dataCenterGuestIpv6PrefixDao.remove(prefixId);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_POD_EDIT, eventDescription = "updating pod", async = false)
    public Pod editPod(final UpdatePodCmd cmd) {
        return editPod(cmd.getId(), cmd.getPodName(), null, null, cmd.getGateway(), cmd.getNetmask(), cmd.getAllocationState());
    }

    @Override
    @DB
    public Pod editPod(final long id, String name, String startIp, String endIp, String gateway, String netmask, String allocationStateStr) {

        // verify parameters
        final HostPodVO pod = _podDao.findById(id);

        if (pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id " + id);
        }

        // If the gateway, CIDR, private IP range is being changed, check if the
        // pod has allocated private IP addresses
        if (podHasAllocatedPrivateIPs(id)) {

            if (StringUtils.isNotEmpty(netmask)) {
                final long newCidr = NetUtils.getCidrSize(netmask);
                final long oldCidr = pod.getCidrSize();

                if (newCidr > oldCidr) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                }
            }
        }

        if (gateway == null) {
            gateway = pod.getGateway();
        }

        if (netmask == null) {
            netmask = NetUtils.getCidrNetmask(pod.getCidrSize());
        }

        final String oldPodName = pod.getName();
        if (name == null) {
            name = oldPodName;
        }

        if (allocationStateStr == null) {
            allocationStateStr = pod.getAllocationState().toString();
        }

        // Verify pod's attributes
        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        final boolean checkForDuplicates = !oldPodName.equals(name);
        final DataCenterVO zone = _zoneDao.findById(pod.getDataCenterId());
        checkPodAttributes(id, name, zone, gateway, cidr, startIp, endIp, allocationStateStr, checkForDuplicates, true);

        // Valid check is already done in checkPodAttributes method.
        final String cidrAddress = getCidrAddress(cidr);
        final long cidrSize = getCidrSize(cidr);

        // Check if start IP and end IP of all the ranges lie in the CIDR subnet.
        final String[] existingPodIpRanges = pod.getDescription().split(",");

        for(String podIpRange: existingPodIpRanges) {
            final String[] existingPodIpRange = podIpRange.split("-");

            if (existingPodIpRange.length > 1) {
                if (!NetUtils.isValidIp4(existingPodIpRange[0]) || !NetUtils.isValidIp4(existingPodIpRange[1])) {
                    continue;
                }

                if (!NetUtils.getCidrSubNet(existingPodIpRange[0], cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
                    throw new InvalidParameterValueException("The start address of the some IP range is not in the CIDR subnet.");
                }

                if (!NetUtils.getCidrSubNet(existingPodIpRange[1], cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
                    throw new InvalidParameterValueException("The end address of the some IP range is not in the CIDR subnet.");
                }

                if (NetUtils.ipRangesOverlap(existingPodIpRange[0], existingPodIpRange[1], gateway, gateway)) {
                    throw new InvalidParameterValueException("The gateway shouldn't overlap some start/end ip addresses");
                }
            }
        }

        try {
            final String allocationStateStrFinal = allocationStateStr;
            final String nameFinal = name;
            final String gatewayFinal = gateway;
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    final long zoneId = pod.getDataCenterId();

                    pod.setName(nameFinal);
                    pod.setDataCenterId(zoneId);
                    pod.setGateway(gatewayFinal);
                    pod.setCidrAddress(getCidrAddress(cidr));
                    pod.setCidrSize(getCidrSize(cidr));

                    Grouping.AllocationState allocationState = null;
                    if (allocationStateStrFinal != null && !allocationStateStrFinal.isEmpty()) {
                        allocationState = Grouping.AllocationState.valueOf(allocationStateStrFinal);
                        pod.setAllocationState(allocationState);
                    }

                    _podDao.update(id, pod);
                }
            });

            messageBus.publish(_name, MESSAGE_DELETE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, pod);
            messageBus.publish(_name, MESSAGE_CREATE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, pod);
        } catch (final Exception e) {
            s_logger.error("Unable to edit pod due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to edit pod. Please contact Cloud Support.");
        }

        return pod;
    }

    private void checkPodRangeParametersBasicsForNonEdgeZone(final String startIp, final String endIp, final String gateway, final String netmask) {
        if (!NetUtils.isValidIp4(startIp)) {
            throw new InvalidParameterValueException("The start IP is invalid");
        }
        if (endIp != null && !NetUtils.isValidIp4(endIp)) {
            throw new InvalidParameterValueException("The end IP is invalid");
        }
        if (!NetUtils.isValidIp4(gateway)) {
            throw new InvalidParameterValueException("The gateway is invalid");
        }
        if (!NetUtils.isValidIp4Netmask(netmask)) {
            throw new InvalidParameterValueException("The netmask is invalid");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_POD_CREATE, eventDescription = "creating pod", async = false)
    public Pod createPod(final long zoneId, final String name, final String startIp, final String endIp, final String gateway, final String netmask, String allocationState) {
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        final Account account = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        String cidr = null;
        if (!DataCenter.Type.Edge.equals(zone.getType())) {
            checkPodRangeParametersBasicsForNonEdgeZone(startIp, endIp, gateway, netmask);
            cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        } else {
            if (ObjectUtils.anyNotNull(startIp, endIp, gateway, netmask)) {
                throw new InvalidParameterValueException("IP range parameters can not be specified for a pod in an edge zone");
            }
        }

        final Long userId = CallContext.current().getCallingUserId();

        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Enabled.toString();
        }
        return createPod(userId.longValue(), name, zone, gateway, cidr, startIp, endIp, allocationState, false);
    }

    @Override
    @DB
    public HostPodVO createPod(final long userId, final String podName, final DataCenter zone, final String gateway, final String cidr, String startIp, String endIp, final String allocationStateStr,
            final boolean skipGatewayOverlapCheck) {
        final String cidrAddress = DataCenter.Type.Edge.equals(zone.getType()) ? "" : getCidrAddress(cidr);
        final int cidrSize = DataCenter.Type.Edge.equals(zone.getType()) ? 0 : getCidrSize(cidr);
        if (DataCenter.Type.Edge.equals(zone.getType())) {
            startIp = null;
            endIp = null;
        }

        // endIp is an optional parameter; if not specified - default it to the
        // end ip of the pod's cidr
        if (StringUtils.isNotEmpty(startIp)) {
            if (endIp == null) {
                endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
            }
        }

        // Validate new pod settings
        checkPodAttributes(-1, podName, zone, gateway, cidr, startIp, endIp, allocationStateStr, true, skipGatewayOverlapCheck);

        // Create the new pod in the database
        String ipRange = null;
        if (StringUtils.isNotEmpty(startIp)) {
            ipRange = startIp + "-" + endIp + "-" + DefaultForSystemVmsForPodIpRange + "-" + DefaultVlanForPodIpRange;
        }

        final HostPodVO podFinal = new HostPodVO(podName, zone.getId(), StringUtils.defaultIfEmpty(gateway, "") , cidrAddress, cidrSize, ipRange);

        Grouping.AllocationState allocationState = null;
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            podFinal.setAllocationState(allocationState);
        }

        final String startIpFinal = startIp;
        final String endIpFinal = endIp;
        HostPodVO hostPodVO = Transaction.execute((TransactionCallback<HostPodVO>) status -> {
            final HostPodVO pod = _podDao.persist(podFinal);

            if (StringUtils.isNotEmpty(startIpFinal)) {
                _zoneDao.addPrivateIpAddress(zone.getId(), pod.getId(), startIpFinal, endIpFinal, false, null);
            }

            final String[] linkLocalIpRanges = NetUtils.getLinkLocalIPRange(_configDao.getValue(Config.ControlCidr.key()));
            if (linkLocalIpRanges.length > 1) {
                _zoneDao.addLinkLocalIpAddress(zone.getId(), pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
            }

            CallContext.current().putContextParameter(Pod.class, pod.getUuid());

            return pod;
        });

        messageBus.publish(_name, MESSAGE_CREATE_POD_IP_RANGE_EVENT, PublishScope.LOCAL, hostPodVO);

        return hostPodVO;
    }

    @DB
    protected void checkIfZoneIsDeletable(final long zoneId) {
        final String errorMsg = "The zone cannot be deleted because ";


        // Check if there are any non-removed hosts in the zone.
        if (!_hostDao.listByDataCenterId(zoneId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are servers in this zone.");
        }

        // Check if there are any non-removed pods in the zone.
        if (!_podDao.listByDataCenterId(zoneId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are pods in this zone.");
        }

        // Check if there are allocated private IP addresses in the zone.
        if (_privateIpAddressDao.countIPs(zoneId, true) != 0) {
            throw new CloudRuntimeException(errorMsg + "there are private IP addresses allocated in this zone.");
        }

        // Check if there are allocated public IP addresses in the zone.
        if (_publicIpAddressDao.countIPs(zoneId, true) != 0) {
            throw new CloudRuntimeException(errorMsg + "there are public IP addresses allocated in this zone.");
        }

        // Check if there are any non-removed vms in the zone.
        if (!_vmInstanceDao.listByZoneId(zoneId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are virtual machines in this zone.");
        }

        // Check if there are any non-removed volumes in the zone.
        if (!_volumeDao.findByDc(zoneId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are storage volumes in this zone.");
        }

        // Check if there are any non-removed physical networks in the zone.
        if (!_physicalNetworkDao.listByZone(zoneId).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are physical networks in this zone.");
        }

        //check if there are any secondary stores attached to the zone
        if(!_imageStoreDao.findByZone(new ZoneScope(zoneId), null).isEmpty()) {
            throw new CloudRuntimeException(errorMsg + "there are Secondary storages in this zone");
        }

        // Check if there are any non-removed VMware datacenters in the zone.
        //if (_vmwareDatacenterZoneMapDao.findByZoneId(zoneId) != null) {
        //    throw new CloudRuntimeException(errorMsg + "there are VMware datacenters in this zone.");
        //}
    }

    private void checkZoneParameters(final String zoneName, final String dns1, final String dns2, final String internalDns1, final String internalDns2, final boolean checkForDuplicates, final Long domainId,
            final String allocationStateStr, final String ip6Dns1, final String ip6Dns2) {
        if (checkForDuplicates) {
            // Check if a zone with the specified name already exists
            if (validZone(zoneName)) {
                throw new InvalidParameterValueException("A zone with that name already exists. Please specify a unique zone name.");
            }
        }

        // check if valid domain
        if (domainId != null) {
            final DomainVO domain = _domainDao.findById(domainId);

            if (domain == null) {
                throw new InvalidParameterValueException("Please specify a valid domain id");
            }
        }

        // Check IP validity for DNS addresses
        // Empty strings is a valid input -- hence the length check
        if (dns1 != null && dns1.length() > 0 && !NetUtils.isValidIp4(dns1)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for DNS1");
        }

        if (dns2 != null && dns2.length() > 0 && !NetUtils.isValidIp4(dns2)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for DNS2");
        }

        if (internalDns1 != null && internalDns1.length() > 0 && !NetUtils.isValidIp4(internalDns1)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS1");
        }

        if (internalDns2 != null && internalDns2.length() > 0 && !NetUtils.isValidIp4(internalDns2)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS2");
        }

        if (ip6Dns1 != null && ip6Dns1.length() > 0 && !NetUtils.isValidIp6(ip6Dns1)) {
            throw new InvalidParameterValueException("Please enter a valid IPv6 address for IP6 DNS1");
        }

        if (ip6Dns2 != null && ip6Dns2.length() > 0 && !NetUtils.isValidIp6(ip6Dns2)) {
            throw new InvalidParameterValueException("Please enter a valid IPv6 address for IP6 DNS2");
        }

        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (final IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    private void checkIpRange(final String startIp, final String endIp, final String cidrAddress, final long cidrSize) {
        //Checking not null for start IP as well. Previously we assumed to be not null always.
        //But the check is required for the change in updatePod API.
        if (StringUtils.isNotEmpty(startIp) && !NetUtils.isValidIp4(startIp)) {
            throw new InvalidParameterValueException("The start address of the IP range is not a valid IP address.");
        }

        if (StringUtils.isNotEmpty(endIp) && !NetUtils.isValidIp4(endIp)) {
            throw new InvalidParameterValueException("The end address of the IP range is not a valid IP address.");
        }

        //Not null check is required for the change in updatePod API.
        if (StringUtils.isNotEmpty(startIp) && !NetUtils.getCidrSubNet(startIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The start address of the IP range is not in the CIDR subnet.");
        }

        if (StringUtils.isNotEmpty(endIp) && !NetUtils.getCidrSubNet(endIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The end address of the IP range is not in the CIDR subnet.");
        }

        if (StringUtils.isNotEmpty(endIp) && NetUtils.ip2Long(startIp) > NetUtils.ip2Long(endIp)) {
            throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
        }

    }

    private void checkOverlapPublicIpRange(final Long zoneId, final String startIp, final String endIp) {
        final long privateStartIp = NetUtils.ip2Long(startIp);
        final long privateEndIp = NetUtils.ip2Long(endIp);

        final List<IPAddressVO> existingPublicIPs = _publicIpAddressDao.listByDcId(zoneId);
        for (final IPAddressVO publicIPVO : existingPublicIPs) {
            final long publicIP = NetUtils.ip2Long(publicIPVO.getAddress().addr());
            if (publicIP >= privateStartIp && publicIP <= privateEndIp) {
                throw new InvalidParameterValueException("The Start IP and endIP address range overlap with Public IP :" + publicIPVO.getAddress().addr());
            }
        }
    }

    private void checkOverlapPrivateIpRange(final Long zoneId, final String startIp, final String endIp) {

        final List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zoneId);
        for (final HostPodVO hostPod : podsInZone) {
            final String[] existingPodIpRanges = hostPod.getDescription().split(",");

            for(String podIpRange: existingPodIpRanges) {
                final String[] existingPodIpRange = podIpRange.split("-");

                if (existingPodIpRange.length > 1) {
                    if (!NetUtils.isValidIp4(existingPodIpRange[0]) || !NetUtils.isValidIp4(existingPodIpRange[1])) {
                        continue;
                    }

                    if (NetUtils.ipRangesOverlap(startIp, endIp, existingPodIpRange[0], existingPodIpRange[1])) {
                        throw new InvalidParameterValueException("The Start IP and EndIP address range overlap with private IP :" + existingPodIpRange[0] + ":" + existingPodIpRange[1]);
                    }
                }
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_DELETE, eventDescription = "deleting zone", async = false)
    public boolean deleteZone(final DeleteZoneCmd cmd) {

        final Long zoneId = cmd.getId();
        DataCenterVO zone = _zoneDao.findById(zoneId);

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        checkIfZoneIsDeletable(zoneId);

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                // delete vlans for this zone
                final List<VlanVO> vlans = _vlanDao.listByZone(zoneId);
                for (final VlanVO vlan : vlans) {
                    _vlanDao.remove(vlan.getId());
                }

                final boolean success = _zoneDao.remove(zoneId);

                if (success) {
                    // delete template refs for this zone
                    templateZoneDao.deleteByZoneId(zoneId);
                    // delete all capacity records for the zone
                    _capacityDao.removeBy(null, zoneId, null, null, null);
                    // remove from dedicated resources
                    final DedicatedResourceVO dr = _dedicatedDao.findByZoneId(zoneId);
                    if (dr != null) {
                        _dedicatedDao.remove(dr.getId());
                        // find the group associated and check if there are any more
                        // resources under that group
                        final List<DedicatedResourceVO> resourcesInGroup = _dedicatedDao.listByAffinityGroupId(dr.getAffinityGroupId());
                        if (resourcesInGroup.isEmpty()) {
                            // delete the group
                            _affinityGroupService.deleteAffinityGroup(dr.getAffinityGroupId(), null, null, null, null);
                        }
                    }
                    annotationDao.removeByEntityType(AnnotationService.EntityType.ZONE.name(), zone.getUuid());
                }

                return success;
            }
        });
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_EDIT, eventDescription = "editing zone", async = false)
    public DataCenter editZone(final UpdateZoneCmd cmd) {
        // Parameter validation as from execute() method in V1
        final Long zoneId = cmd.getId();
        String zoneName = cmd.getZoneName();
        String dns1 = cmd.getDns1();
        String dns2 = cmd.getDns2();
        String ip6Dns1 = cmd.getIp6Dns1();
        String ip6Dns2 = cmd.getIp6Dns2();
        String internalDns1 = cmd.getInternalDns1();
        String internalDns2 = cmd.getInternalDns2();
        String guestCidr = cmd.getGuestCidrAddress();
        final List<String> dnsSearchOrder = cmd.getDnsSearchOrder();
        final Boolean isPublic = cmd.isPublic();
        final String allocationStateStr = cmd.getAllocationState();
        final String dhcpProvider = cmd.getDhcpProvider();
        final Map<?, ?> detailsMap = cmd.getDetails();
        final String networkDomain = cmd.getDomain();
        final Boolean localStorageEnabled = cmd.getLocalStorageEnabled();

        final Map<String, String> newDetails = new HashMap<String, String>();
        if (detailsMap != null) {
            final Collection<?> zoneDetailsCollection = detailsMap.values();
            final Iterator<?> iter = zoneDetailsCollection.iterator();
            while (iter.hasNext()) {
                final HashMap<?, ?> detail = (HashMap<?, ?>)iter.next();
                final String key = (String)detail.get("key");
                final String value = (String)detail.get("value");
                if (key == null || value == null) {
                    throw new InvalidParameterValueException(
                            "Invalid Zone Detail specified, fields 'key' and 'value' cannot be null, please specify details in the form:  details[0].key=XXX&details[0].value=YYY");
                }
                newDetails.put(key, value);
            }
        }

        // add the domain prefix list to details if not null
        if (dnsSearchOrder != null) {
            for (final String dom : dnsSearchOrder) {
                if (!NetUtils.verifyDomainName(dom)) {
                    throw new InvalidParameterValueException(
                            "Invalid network domain suffixes. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                    + "and the hyphen ('-'); can't start or end with \"-\"");
                }
            }
            newDetails.put(ZoneConfig.DnsSearchOrder.getName(), StringUtils.join(dnsSearchOrder, ","));
        }

        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }

        if (zoneName == null) {
            zoneName = zone.getName();
        }

        if (guestCidr != null && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        final String oldZoneName = zone.getName();

        if (zoneName == null) {
            zoneName = oldZoneName;
        }

        if (dns1 == null) {
            dns1 = zone.getDns1();
        }

        if (dns2 == null) {
            dns2 = zone.getDns2();
        }

        if (ip6Dns1 == null) {
            ip6Dns1 = zone.getIp6Dns1();
        }

        if (ip6Dns2 == null) {
            ip6Dns2 = zone.getIp6Dns2();
        }

        if (internalDns1 == null) {
            internalDns1 = zone.getInternalDns1();
        }

        if (internalDns2 == null) {
            internalDns2 = zone.getInternalDns2();
        }

        if (guestCidr == null) {
            guestCidr = zone.getGuestNetworkCidr();
        }

        int sortKey = cmd.getSortKey() != null ? cmd.getSortKey() : zone.getSortKey();

        // validate network domain
        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        final boolean checkForDuplicates = !zoneName.equals(oldZoneName);
        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, checkForDuplicates, null, allocationStateStr, ip6Dns1, ip6Dns2);// not allowing updating
        // domain associated with
        // a zone, once created

        zone.setName(zoneName);
        zone.setDns1(dns1);
        zone.setDns2(dns2);
        zone.setIp6Dns1(ip6Dns1);
        zone.setIp6Dns2(ip6Dns2);
        zone.setInternalDns1(internalDns1);
        zone.setInternalDns2(internalDns2);
        zone.setGuestNetworkCidr(guestCidr);
        zone.setSortKey(sortKey);
        if (localStorageEnabled != null) {
            zone.setLocalStorageEnabled(localStorageEnabled.booleanValue());
        }

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                zone.setDomain(null);
            } else {
                zone.setDomain(networkDomain);
            }
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                final Map<String, String> updatedDetails = new HashMap<String, String>();
                _zoneDao.loadDetails(zone);
                if (zone.getDetails() != null) {
                    updatedDetails.putAll(zone.getDetails());
                }
                updatedDetails.putAll(newDetails);
                zone.setDetails(updatedDetails);

                if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
                    final Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);

                    if (allocationState == Grouping.AllocationState.Enabled && !DataCenter.Type.Edge.equals(zone.getType())) {
                        // check if zone has necessary trafficTypes before enabling
                        try {
                            PhysicalNetwork mgmtPhyNetwork;
                            // zone should have a physical network with management
                            // traffiType
                            mgmtPhyNetwork = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Management);
                            if (NetworkType.Advanced == zone.getNetworkType() && !zone.isSecurityGroupEnabled()) {
                                // advanced zone without SG should have a physical
                                // network with public Thpe
                                _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Public);
                            }

                            try {
                                _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Storage);
                            } catch (final InvalidParameterValueException noStorage) {
                                final PhysicalNetworkTrafficTypeVO mgmtTraffic = _trafficTypeDao.findBy(mgmtPhyNetwork.getId(), TrafficType.Management);
                                _networkSvc.addTrafficTypeToPhysicalNetwork(mgmtPhyNetwork.getId(), TrafficType.Storage.toString(), "vlan", mgmtTraffic.getXenNetworkLabel(),
                                        mgmtTraffic.getKvmNetworkLabel(), mgmtTraffic.getVmwareNetworkLabel(), mgmtTraffic.getSimulatorNetworkLabel(), mgmtTraffic.getVlan(),
                                        mgmtTraffic.getHypervNetworkLabel(), mgmtTraffic.getOvm3NetworkLabel());
                                s_logger.info("No storage traffic type was specified by admin, create default storage traffic on physical network " + mgmtPhyNetwork.getId()
                                        + " with same configure of management traffic type");
                            }
                        } catch (final InvalidParameterValueException ex) {
                            throw new InvalidParameterValueException("Cannot enable this Zone since: " + ex.getMessage());
                        }
                    }
                    zone.setAllocationState(allocationState);
                }

                if (dhcpProvider != null) {
                    zone.setDhcpProvider(dhcpProvider);
                }

                // update a private zone to public; not vice versa
                if (isPublic != null && isPublic) {
                    zone.setDomainId(null);
                    zone.setDomain(null);

                    // release the dedication for this zone
                    final DedicatedResourceVO resource = _dedicatedDao.findByZoneId(zoneId);
                    Long resourceId = null;
                    if (resource != null) {
                        resourceId = resource.getId();
                        if (!_dedicatedDao.remove(resourceId)) {
                            throw new CloudRuntimeException("Failed to delete dedicated Zone Resource " + resourceId);
                        }
                        // find the group associated and check if there are any more
                        // resources under that group
                        final List<DedicatedResourceVO> resourcesInGroup = _dedicatedDao.listByAffinityGroupId(resource.getAffinityGroupId());
                        if (resourcesInGroup.isEmpty()) {
                            // delete the group
                            _affinityGroupService.deleteAffinityGroup(resource.getAffinityGroupId(), null, null, null, null);
                        }
                    }
                }

                if (!_zoneDao.update(zoneId, zone)) {
                    throw new CloudRuntimeException("Failed to edit zone. Please contact Cloud Support.");
                }
            }
        });

        return zone;
    }

    @Override
    @DB
    public DataCenterVO createZone(final long userId, final String zoneName, final String dns1, final String dns2, final String internalDns1, final String internalDns2, final String guestCidr, final String domain,
            final Long domainId, final NetworkType zoneType, final String allocationStateStr, final String networkDomain, final boolean isSecurityGroupEnabled, final boolean isLocalStorageEnabled,
            final String ip6Dns1, final String ip6Dns2, final boolean isEdge) {

        // checking the following params outside checkzoneparams method as we do
        // not use these params for updatezone
        // hence the method below is generic to check for common params
        if (guestCidr != null && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        // Validate network domain
        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, true, domainId, allocationStateStr, ip6Dns1, ip6Dns2);

        final byte[] bytes = (zoneName + System.currentTimeMillis()).getBytes();
        final String zoneToken = UUID.nameUUIDFromBytes(bytes).toString();

        // Create the new zone in the database
        final DataCenterVO zoneFinal = new DataCenterVO(zoneName, null, dns1, dns2, internalDns1, internalDns2, guestCidr, domain, domainId, zoneType, zoneToken, networkDomain,
                isSecurityGroupEnabled, isLocalStorageEnabled, ip6Dns1, ip6Dns2);
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            final Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            zoneFinal.setAllocationState(allocationState);
        } else {
            // Zone will be disabled since 3.0. Admin should enable it after
            // physical network and providers setup.
            zoneFinal.setAllocationState(Grouping.AllocationState.Disabled);
        }
        zoneFinal.setType(isEdge ? DataCenter.Type.Edge : DataCenter.Type.Core);

        return Transaction.execute(new TransactionCallback<DataCenterVO>() {
            @Override
            public DataCenterVO doInTransaction(final TransactionStatus status) {
                final DataCenterVO zone = _zoneDao.persist(zoneFinal);
                CallContext.current().putContextParameter(DataCenter.class, zone.getUuid());
                if (domainId != null) {
                    // zone is explicitly dedicated to this domain
                    // create affinity group associated and dedicate the zone.
                    final AffinityGroup group = createDedicatedAffinityGroup(null, domainId, null);
                    final DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(zone.getId(), null, null, null, domainId, null, group.getId());
                    _dedicatedDao.persist(dedicatedResource);
                }

                // Create default system networks
                createDefaultSystemNetworks(zone.getId());

                return zone;
            }
        });
    }

    private AffinityGroup createDedicatedAffinityGroup(String affinityGroupName, final Long domainId, final Long accountId) {
        if (affinityGroupName == null) {
            // default to a groupname with account/domain information
            affinityGroupName = "ZoneDedicatedGrp-domain-" + domainId + (accountId != null ? "-acct-" + accountId : "");
        }

        AffinityGroup group = null;
        String accountName = null;

        if (accountId != null) {
            final AccountVO account = _accountDao.findById(accountId);
            accountName = account.getAccountName();

            group = _affinityGroupDao.findByAccountAndName(accountId, affinityGroupName);
            if (group != null) {
                return group;
            }
        } else {
            // domain level group
            group = _affinityGroupDao.findDomainLevelGroupByName(domainId, affinityGroupName);
            if (group != null) {
                return group;
            }
        }

        group = _affinityGroupService.createAffinityGroup(accountName, null, domainId, affinityGroupName, "ExplicitDedication", "dedicated resources group");

        return group;

    }

    @Override
    public void createDefaultSystemNetworks(final long zoneId) throws ConcurrentOperationException {
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        final String networkDomain = null;
        // Create public, management, control and storage networks as a part of
        // the zone creation
        if (zone != null) {
            final List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();

            for (final NetworkOfferingVO offering : ntwkOff) {
                final DataCenterDeployment plan = new DataCenterDeployment(zone.getId(), null, null, null, null, null);
                final NetworkVO userNetwork = new NetworkVO();

                final Account systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);

                BroadcastDomainType broadcastDomainType = null;
                if (offering.getTrafficType() == TrafficType.Management) {
                    broadcastDomainType = BroadcastDomainType.Native;
                } else if (offering.getTrafficType() == TrafficType.Control) {
                    broadcastDomainType = BroadcastDomainType.LinkLocal;
                } else if (offering.getTrafficType() == TrafficType.Public) {
                    if (zone.getNetworkType() == NetworkType.Advanced && !zone.isSecurityGroupEnabled() || zone.getNetworkType() == NetworkType.Basic) {
                        broadcastDomainType = BroadcastDomainType.Vlan;
                    } else {
                        continue; // so broadcastDomainType remains null! why have None/Undecided/UnKnown?
                    }
                } else if (offering.getTrafficType() == TrafficType.Guest) {
                    continue;
                }

                userNetwork.setBroadcastDomainType(broadcastDomainType);
                userNetwork.setNetworkDomain(networkDomain);
                _networkMgr.setupNetwork(systemAccount, offering, userNetwork, plan, null, null, false, Domain.ROOT_DOMAIN, null, null, null, true);
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_CREATE, eventDescription = "creating zone", async = false)
    public DataCenter createZone(final CreateZoneCmd cmd) {
        // grab parameters from the command
        final Long userId = CallContext.current().getCallingUserId();
        final String zoneName = cmd.getZoneName();
        final String dns1 = cmd.getDns1();
        final String dns2 = cmd.getDns2();
        final String ip6Dns1 = cmd.getIp6Dns1();
        final String ip6Dns2 = cmd.getIp6Dns2();
        final String internalDns1 = cmd.getInternalDns1();
        final String internalDns2 = cmd.getInternalDns2();
        final String guestCidr = cmd.getGuestCidrAddress();
        final Long domainId = cmd.getDomainId();
        final String type = cmd.getNetworkType();
        Boolean isBasic = false;
        String allocationState = cmd.getAllocationState();
        final String networkDomain = cmd.getDomain();
        boolean isSecurityGroupEnabled = cmd.getSecuritygroupenabled();
        final boolean isLocalStorageEnabled = cmd.getLocalStorageEnabled();
        final boolean isEdge = cmd.isEdge();

        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Disabled.toString();
        }

        if (!type.equalsIgnoreCase(NetworkType.Basic.toString()) && !type.equalsIgnoreCase(NetworkType.Advanced.toString())) {
            throw new InvalidParameterValueException("Invalid zone type; only Advanced and Basic values are supported");
        } else if (type.equalsIgnoreCase(NetworkType.Basic.toString())) {
            isBasic = true;
        }

        final NetworkType zoneType = isBasic ? NetworkType.Basic : NetworkType.Advanced;

        // error out when the parameter specified for Basic zone
        if (zoneType == NetworkType.Basic && guestCidr != null) {
            throw new InvalidParameterValueException("guestCidrAddress parameter is not supported for Basic zone");
        }

        if (!NetworkType.Advanced.equals(zoneType) && isEdge) {
            throw new InvalidParameterValueException("Only advanced network type zones can be edge zones");
        }

        DomainVO domainVO = null;

        if (domainId != null) {
            domainVO = _domainDao.findById(domainId);
        }

        if (zoneType == NetworkType.Basic) {
            isSecurityGroupEnabled = true;
        }

        return createZone(userId, zoneName, dns1, dns2, internalDns1, internalDns2, guestCidr, domainVO != null ? domainVO.getName() : null, domainId, zoneType, allocationState,
                networkDomain, isSecurityGroupEnabled, isLocalStorageEnabled, ip6Dns1, ip6Dns2, isEdge);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_CREATE, eventDescription = "creating service offering")
    public ServiceOffering createServiceOffering(final CreateServiceOfferingCmd cmd) {
        final Long userId = CallContext.current().getCallingUserId();
        final Map<String, String> details = cmd.getDetails();
        final String offeringName = cmd.getServiceOfferingName();

        final String name = cmd.getServiceOfferingName();
        if (name == null || name.length() == 0) {
            throw new InvalidParameterValueException("Failed to create service offering: specify the name that has non-zero length");
        }

        final String displayText = cmd.getDisplayText();
        if (displayText == null || displayText.length() == 0) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the display text that has non-zero length");
        }

        final Integer cpuNumber = cmd.getCpuNumber();
        final Integer cpuSpeed = cmd.getCpuSpeed();
        final Integer memory = cmd.getMemory();

        // Optional Custom Parameters
        Integer maxCPU = cmd.getMaxCPUs();
        Integer minCPU = cmd.getMinCPUs();
        Integer maxMemory = cmd.getMaxMemory();
        Integer minMemory = cmd.getMinMemory();

        // Check if service offering is Custom,
        // If Customized, the following conditions must hold
        // 1. cpuNumber, cpuSpeed and memory should be all null
        // 2. minCPU, maxCPU, minMemory and maxMemory should all be null or all specified
        boolean isCustomized = cmd.isCustomized();
        if (isCustomized) {
            // validate specs
            //restricting the createserviceoffering to allow setting all or none of the dynamic parameters to null
            if (cpuNumber != null || memory != null) {
                throw new InvalidParameterValueException("For creating a custom compute offering cpu and memory all should be null");
            }
            // if any of them is null, then all of them shoull be null
            if (maxCPU == null || minCPU == null || maxMemory == null || minMemory == null || cpuSpeed == null) {
                if (maxCPU != null || minCPU != null || maxMemory != null || minMemory != null || cpuSpeed != null) {
                    throw new InvalidParameterValueException("For creating a custom compute offering min/max cpu and min/max memory/cpu speed should all be null or all specified");
                }
            } else {
                if (cpuSpeed.intValue() < 0 || cpuSpeed.longValue() > Integer.MAX_VALUE) {
                    throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the cpu speed value between 1 and " + Integer.MAX_VALUE);
                }
                if ((maxCPU <= 0 || maxCPU.longValue() > Integer.MAX_VALUE) || (minCPU <= 0 || minCPU.longValue() > Integer.MAX_VALUE )  ) {
                    throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the minimum or minimum cpu number value between 1 and " + Integer.MAX_VALUE);
                }
                if (minMemory < 32 || (minMemory.longValue() > Integer.MAX_VALUE) || (maxMemory.longValue() > Integer.MAX_VALUE)) {
                    throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the memory value between 32 and " + Integer.MAX_VALUE + " MB");
                }
                // Persist min/max CPU and Memory parameters in the service_offering_details table
                details.put(ApiConstants.MIN_MEMORY, minMemory.toString());
                details.put(ApiConstants.MAX_MEMORY, maxMemory.toString());
                details.put(ApiConstants.MIN_CPU_NUMBER, minCPU.toString());
                details.put(ApiConstants.MAX_CPU_NUMBER, maxCPU.toString());
            }
        } else {
            Integer maxCPUCores = VM_SERVICE_OFFERING_MAX_CPU_CORES.value() == 0 ? Integer.MAX_VALUE: VM_SERVICE_OFFERING_MAX_CPU_CORES.value();
            Integer maxRAMSize = VM_SERVICE_OFFERING_MAX_RAM_SIZE.value() == 0 ? Integer.MAX_VALUE: VM_SERVICE_OFFERING_MAX_RAM_SIZE.value();
            if (cpuNumber != null && (cpuNumber.intValue() <= 0 || cpuNumber.longValue() > maxCPUCores)) {
                throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the cpu number value between 1 and " + maxCPUCores);
            }
            if (cpuSpeed == null || (cpuSpeed.intValue() < 0 || cpuSpeed.longValue() > Integer.MAX_VALUE)) {
                throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the cpu speed value between 0 and " + Integer.MAX_VALUE);
            }
            if (memory != null && (memory.intValue() < 32 || memory.longValue() > maxRAMSize)) {
                throw new InvalidParameterValueException("Failed to create service offering " + offeringName + ": specify the memory value between 32 and " + maxRAMSize + " MB");
            }
        }

        // check if valid domain
        if (CollectionUtils.isNotEmpty(cmd.getDomainIds())) {
            for (final Long domainId: cmd.getDomainIds()) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(cmd.getZoneIds())) {
            for (Long zoneId : cmd.getZoneIds()) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        // check if cache_mode parameter is valid
        validateCacheMode(cmd.getCacheMode());

        final Boolean offerHA = cmd.isOfferHa();

        boolean localStorageRequired = false;
        final String storageType = cmd.getStorageType();
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                if(offerHA) {
                    throw new InvalidParameterValueException("HA offering with local storage is not supported. ");
                }
                localStorageRequired = true;
            } else if (!storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
            }
        }

        final Boolean limitCpuUse = cmd.isLimitCpuUse();
        final Boolean volatileVm = cmd.isVolatileVm();

        final String vmTypeString = cmd.getSystemVmType();
        VirtualMachine.Type vmType = null;
        boolean allowNetworkRate = false;

        Boolean isCustomizedIops;

        if (cmd.isSystem()) {
            if (vmTypeString == null || VirtualMachine.Type.DomainRouter.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.DomainRouter;
                allowNetworkRate = true;
            } else if (VirtualMachine.Type.ConsoleProxy.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.ConsoleProxy;
            } else if (VirtualMachine.Type.SecondaryStorageVm.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.SecondaryStorageVm;
            } else if (VirtualMachine.Type.InternalLoadBalancerVm.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.InternalLoadBalancerVm;
            } else {
                throw new InvalidParameterValueException("Invalid systemVmType. Supported types are: " + VirtualMachine.Type.DomainRouter + ", " + VirtualMachine.Type.ConsoleProxy
                        + ", " + VirtualMachine.Type.SecondaryStorageVm);
            }

            if (cmd.isCustomizedIops() != null) {
                throw new InvalidParameterValueException("Customized IOPS is not a valid parameter for a system VM.");
            }

            isCustomizedIops = false;

            if (cmd.getHypervisorSnapshotReserve() != null) {
                throw new InvalidParameterValueException("Hypervisor snapshot reserve is not a valid parameter for a system VM.");
            }
        } else {
            allowNetworkRate = true;
            isCustomizedIops = cmd.isCustomizedIops();
        }

        if (cmd.getNetworkRate() != null) {
            if(!allowNetworkRate) {
                throw new InvalidParameterValueException("Network rate can be specified only for non-System offering and system offerings having \"domainrouter\" systemvmtype");
            }
            if(cmd.getNetworkRate().intValue() < 0) {
                throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the network rate value more than 0");
            }
        }

        if (cmd.getDeploymentPlanner() != null) {
            final List<String> planners = _mgr.listDeploymentPlanners();
            if (planners != null && !planners.isEmpty()) {
                if (!planners.contains(cmd.getDeploymentPlanner())) {
                    throw new InvalidParameterValueException("Invalid name for Deployment Planner specified, please use listDeploymentPlanners to get the valid set");
                }
            } else {
                throw new InvalidParameterValueException("No deployment planners found");
            }
        }

        final Long storagePolicyId = cmd.getStoragePolicy();
        if (storagePolicyId != null) {
            if (vsphereStoragePolicyDao.findById(storagePolicyId) == null) {
                throw new InvalidParameterValueException("Please specify a valid vSphere storage policy id");
            }
        }

        final Long diskOfferingId = cmd.getDiskOfferingId();
        if (diskOfferingId != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.isComputeOnly()) {
                throw new InvalidParameterValueException("Please specify a valid disk offering.");
            }
        }

        return createServiceOffering(userId, cmd.isSystem(), vmType, cmd.getServiceOfferingName(), cpuNumber, memory, cpuSpeed, cmd.getDisplayText(),
                cmd.getProvisioningType(), localStorageRequired, offerHA, limitCpuUse, volatileVm, cmd.getTags(), cmd.getDomainIds(), cmd.getZoneIds(), cmd.getHostTag(),
                cmd.getNetworkRate(), cmd.getDeploymentPlanner(), details, cmd.getRootDiskSize(), isCustomizedIops, cmd.getMinIops(), cmd.getMaxIops(),
                cmd.getBytesReadRate(), cmd.getBytesReadRateMax(), cmd.getBytesReadRateMaxLength(),
                cmd.getBytesWriteRate(), cmd.getBytesWriteRateMax(), cmd.getBytesWriteRateMaxLength(),
                cmd.getIopsReadRate(), cmd.getIopsReadRateMax(), cmd.getIopsReadRateMaxLength(),
                cmd.getIopsWriteRate(), cmd.getIopsWriteRateMax(), cmd.getIopsWriteRateMaxLength(),
                cmd.getHypervisorSnapshotReserve(), cmd.getCacheMode(), storagePolicyId, cmd.getDynamicScalingEnabled(), diskOfferingId,
                cmd.getDiskOfferingStrictness(), cmd.isCustomized(), cmd.getEncryptRoot());
    }

    protected ServiceOfferingVO createServiceOffering(final long userId, final boolean isSystem, final VirtualMachine.Type vmType,
            final String name, final Integer cpu, final Integer ramSize, final Integer speed, final String displayText, final String provisioningType, final boolean localStorageRequired,
            final boolean offerHA, final boolean limitResourceUse, final boolean volatileVm, String tags, final List<Long> domainIds, List<Long> zoneIds, final String hostTag,
            final Integer networkRate, final String deploymentPlanner, final Map<String, String> details, Long rootDiskSizeInGiB, final Boolean isCustomizedIops, Long minIops, Long maxIops,
            Long bytesReadRate, Long bytesReadRateMax, Long bytesReadRateMaxLength,
            Long bytesWriteRate, Long bytesWriteRateMax, Long bytesWriteRateMaxLength,
            Long iopsReadRate, Long iopsReadRateMax, Long iopsReadRateMaxLength,
            Long iopsWriteRate, Long iopsWriteRateMax, Long iopsWriteRateMaxLength,
            final Integer hypervisorSnapshotReserve, String cacheMode, final Long storagePolicyID, final boolean dynamicScalingEnabled, final Long diskOfferingId,
            final boolean diskOfferingStrictness, final boolean isCustomized, final boolean encryptRoot) {

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            if (filteredDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to create public service offering by admin: %s because it is domain-admin", user.getUuid()));
            }
            if (!org.apache.commons.lang3.StringUtils.isAllBlank(tags, hostTag) && !ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS.valueIn(account.getAccountId())) {
                throw new InvalidParameterValueException(String.format("User [%s] is unable to create service offerings with storage tags or host tags.", user.getUuid()));
            }
            for (Long domainId : filteredDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException(String.format("Unable to create service offering by another domain-admin: %s for domain: %s", user.getUuid(), _entityMgr.findById(Domain.class, domainId).getUuid()));
                }
            }
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to create service offering by user: %s because it is not root-admin or domain-admin", user.getUuid()));
        }

        final ProvisioningType typedProvisioningType = ProvisioningType.getProvisioningType(provisioningType);

        tags = com.cloud.utils.StringUtils.cleanupTags(tags);

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, null, offerHA,
                limitResourceUse, volatileVm, displayText, isSystem, vmType,
                hostTag, deploymentPlanner, dynamicScalingEnabled, isCustomized);

        List<ServiceOfferingDetailsVO> detailsVO = new ArrayList<ServiceOfferingDetailsVO>();
        if (details != null) {
            // To have correct input, either both gpu card name and VGPU type should be passed or nothing should be passed.
            // Use XOR condition to verify that.
            final boolean entry1 = details.containsKey(GPU.Keys.pciDevice.toString());
            final boolean entry2 = details.containsKey(GPU.Keys.vgpuType.toString());
            if ((entry1 || entry2) && !(entry1 && entry2)) {
                throw new InvalidParameterValueException("Please specify the pciDevice and vgpuType correctly.");
            }
            for (final Entry<String, String> detailEntry : details.entrySet()) {
                String detailEntryValue = detailEntry.getValue();
                if (detailEntry.getKey().equals(GPU.Keys.pciDevice.toString())) {
                    if (detailEntryValue == null) {
                        throw new InvalidParameterValueException("Please specify a GPU Card.");
                    }
                }
                if (detailEntry.getKey().equals(GPU.Keys.vgpuType.toString())) {
                    if (detailEntryValue == null) {
                        throw new InvalidParameterValueException("vGPUType value cannot be null");
                    }
                }
                if (detailEntry.getKey().startsWith(ApiConstants.EXTRA_CONFIG)) {
                    try {
                        detailEntryValue = URLDecoder.decode(detailEntry.getValue(), "UTF-8");
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        s_logger.error("Cannot decode extra configuration value for key: " + detailEntry.getKey() + ", skipping it");
                        continue;
                    }
                }
                if (detailEntry.getKey().equalsIgnoreCase(Volume.BANDWIDTH_LIMIT_IN_MBPS) || detailEntry.getKey().equalsIgnoreCase(Volume.IOPS_LIMIT)) {
                    // Add in disk offering details
                    continue;
                }
                detailsVO.add(new ServiceOfferingDetailsVO(serviceOffering.getId(), detailEntry.getKey(), detailEntryValue, true));
            }
        }

        if (storagePolicyID != null) {
            detailsVO.add(new ServiceOfferingDetailsVO(serviceOffering.getId(), ApiConstants.STORAGE_POLICY, String.valueOf(storagePolicyID), false));
        }

        serviceOffering.setDiskOfferingStrictness(diskOfferingStrictness);

        DiskOfferingVO diskOffering = null;
        if (diskOfferingId == null) {
            diskOffering = createDiskOfferingInternal(userId, isSystem, vmType,
                    name, cpu, ramSize, speed, displayText, typedProvisioningType, localStorageRequired,
                    offerHA, limitResourceUse, volatileVm, tags, domainIds, zoneIds, hostTag,
                    networkRate, deploymentPlanner, details, rootDiskSizeInGiB, isCustomizedIops, minIops, maxIops,
                    bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength,
                    bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength,
                    iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength,
                    iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength,
                    hypervisorSnapshotReserve, cacheMode, storagePolicyID, encryptRoot);
        } else {
            diskOffering = _diskOfferingDao.findById(diskOfferingId);
        }
        if (diskOffering != null) {
            serviceOffering.setDiskOfferingId(diskOffering.getId());
        } else {
            return null;
        }

        if ((serviceOffering = _serviceOfferingDao.persist(serviceOffering)) != null) {
            for (Long domainId : filteredDomainIds) {
                detailsVO.add(new ServiceOfferingDetailsVO(serviceOffering.getId(), ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
            }
            if (CollectionUtils.isNotEmpty(zoneIds)) {
                for (Long zoneId : zoneIds) {
                    detailsVO.add(new ServiceOfferingDetailsVO(serviceOffering.getId(), ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }
            if (CollectionUtils.isNotEmpty(detailsVO)) {
                for (ServiceOfferingDetailsVO detail : detailsVO) {
                    detail.setResourceId(serviceOffering.getId());
                }
                _serviceOfferingDetailsDao.saveDetails(detailsVO);
            }

            CallContext.current().setEventDetails("Service offering id=" + serviceOffering.getId());
            CallContext.current().putContextParameter(ServiceOffering.class, serviceOffering.getId());
            return serviceOffering;
        } else {
            return null;
        }
    }

    private DiskOfferingVO createDiskOfferingInternal(final long userId, final boolean isSystem, final VirtualMachine.Type vmType,
                                                      final String name, final Integer cpu, final Integer ramSize, final Integer speed, final String displayText, final ProvisioningType typedProvisioningType, final boolean localStorageRequired,
                                                      final boolean offerHA, final boolean limitResourceUse, final boolean volatileVm, String tags, final List<Long> domainIds, List<Long> zoneIds, final String hostTag,
                                                      final Integer networkRate, final String deploymentPlanner, final Map<String, String> details, Long rootDiskSizeInGiB, final Boolean isCustomizedIops, Long minIops, Long maxIops,
                                                      Long bytesReadRate, Long bytesReadRateMax, Long bytesReadRateMaxLength,
                                                      Long bytesWriteRate, Long bytesWriteRateMax, Long bytesWriteRateMaxLength,
                                                      Long iopsReadRate, Long iopsReadRateMax, Long iopsReadRateMaxLength,
                                                      Long iopsWriteRate, Long iopsWriteRateMax, Long iopsWriteRateMaxLength,
                                                      final Integer hypervisorSnapshotReserve, String cacheMode, final Long storagePolicyID, boolean encrypt) {

        DiskOfferingVO diskOffering = new DiskOfferingVO(name, displayText, typedProvisioningType, false, tags, false, localStorageRequired, false);

        if (Boolean.TRUE.equals(isCustomizedIops) || isCustomizedIops == null) {
            minIops = null;
            maxIops = null;
        } else {
            if (minIops == null && maxIops == null) {
                minIops = 0L;
                maxIops = 0L;
            } else {
                if (minIops == null || minIops <= 0) {
                    throw new InvalidParameterValueException("The min IOPS must be greater than 0.");
                }

                if (maxIops == null) {
                    maxIops = 0L;
                }

                if (minIops > maxIops) {
                    throw new InvalidParameterValueException("The min IOPS must be less than or equal to the max IOPS.");
                }
            }
        }

        if (rootDiskSizeInGiB != null && rootDiskSizeInGiB <= 0L) {
            throw new InvalidParameterValueException(String.format("The Root disk size is of %s GB but it must be greater than 0.", rootDiskSizeInGiB));
        } else if (rootDiskSizeInGiB != null) {
            long maxVolumeSizeInGb = VolumeOrchestrationService.MaxVolumeSize.value();
            if (rootDiskSizeInGiB > maxVolumeSizeInGb) {
                throw new InvalidParameterValueException(String.format("The maximum size for a disk is %d GB.", maxVolumeSizeInGb));
            }
            long rootDiskSizeInBytes = rootDiskSizeInGiB * GiB_TO_BYTES;
            diskOffering.setDiskSize(rootDiskSizeInBytes);
        }

        diskOffering.setCustomizedIops(isCustomizedIops);
        diskOffering.setMinIops(minIops);
        diskOffering.setMaxIops(maxIops);
        diskOffering.setEncrypt(encrypt);

        setBytesRate(diskOffering, bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength, bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength);
        setIopsRate(diskOffering, iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength, iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength);

        if(cacheMode != null) {
            diskOffering.setCacheMode(DiskOffering.DiskCacheMode.valueOf(cacheMode.toUpperCase()));
        }

        if (hypervisorSnapshotReserve != null && hypervisorSnapshotReserve < 0) {
            throw new InvalidParameterValueException("If provided, Hypervisor Snapshot Reserve must be greater than or equal to 0.");
        }

        diskOffering.setHypervisorSnapshotReserve(hypervisorSnapshotReserve);

        if ((diskOffering = _diskOfferingDao.persist(diskOffering)) != null) {
            if (details != null && !details.isEmpty()) {
                List<DiskOfferingDetailVO> diskDetailsVO = new ArrayList<DiskOfferingDetailVO>();
                // Support disk offering details for below parameters
                if (details.containsKey(Volume.BANDWIDTH_LIMIT_IN_MBPS)) {
                    diskDetailsVO.add(new DiskOfferingDetailVO(diskOffering.getId(), Volume.BANDWIDTH_LIMIT_IN_MBPS, details.get(Volume.BANDWIDTH_LIMIT_IN_MBPS), false));
                }
                if (details.containsKey(Volume.IOPS_LIMIT)) {
                    diskDetailsVO.add(new DiskOfferingDetailVO(diskOffering.getId(), Volume.IOPS_LIMIT, details.get(Volume.IOPS_LIMIT), false));
                }
                if (!diskDetailsVO.isEmpty()) {
                    diskOfferingDetailsDao.saveDetails(diskDetailsVO);
                }
            }
        } else {
            return null;
        }

        return diskOffering;
    }
    private void setIopsRate(DiskOffering offering, Long iopsReadRate, Long iopsReadRateMax, Long iopsReadRateMaxLength, Long iopsWriteRate, Long iopsWriteRateMax, Long iopsWriteRateMaxLength) {
        if (iopsReadRate != null && iopsReadRate > 0) {
            offering.setIopsReadRate(iopsReadRate);
        }
        if (iopsReadRateMax != null && iopsReadRateMax > 0) {
            offering.setIopsReadRateMax(iopsReadRateMax);
        }
        if (iopsReadRateMaxLength != null && iopsReadRateMaxLength > 0) {
            offering.setIopsReadRateMaxLength(iopsReadRateMaxLength);
        }
        if (iopsWriteRate != null && iopsWriteRate > 0) {
            offering.setIopsWriteRate(iopsWriteRate);
        }
        if (iopsWriteRateMax != null && iopsWriteRateMax > 0) {
            offering.setIopsWriteRateMax(iopsWriteRateMax);
        }
        if (iopsWriteRateMaxLength != null && iopsWriteRateMaxLength > 0) {
            offering.setIopsWriteRateMaxLength(iopsWriteRateMaxLength);
        }
    }

    private void setBytesRate(DiskOffering offering, Long bytesReadRate, Long bytesReadRateMax, Long bytesReadRateMaxLength, Long bytesWriteRate, Long bytesWriteRateMax, Long bytesWriteRateMaxLength) {
        if (bytesReadRate != null && bytesReadRate > 0) {
            offering.setBytesReadRate(bytesReadRate);
        }
        if (bytesReadRateMax != null && bytesReadRateMax > 0) {
            offering.setBytesReadRateMax(bytesReadRateMax);
        }
        if (bytesReadRateMaxLength != null && bytesReadRateMaxLength > 0) {
            offering.setBytesReadRateMaxLength(bytesReadRateMaxLength);
        }
        if (bytesWriteRate != null && bytesWriteRate > 0) {
            offering.setBytesWriteRate(bytesWriteRate);
        }
        if (bytesWriteRateMax != null && bytesWriteRateMax > 0) {
            offering.setBytesWriteRateMax(bytesWriteRateMax);
        }
        if (bytesWriteRateMaxLength != null && bytesWriteRateMaxLength > 0) {
            offering.setBytesWriteRateMaxLength(bytesWriteRateMaxLength);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_EDIT, eventDescription = "updating service offering")
    public ServiceOffering updateServiceOffering(final UpdateServiceOfferingCmd cmd) {
        final String displayText = cmd.getDisplayText();
        final Long id = cmd.getId();
        final String name = cmd.getServiceOfferingName();
        final Integer sortKey = cmd.getSortKey();
        Long userId = CallContext.current().getCallingUserId();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        String storageTags = cmd.getStorageTags();
        String hostTags = cmd.getHostTags();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify input parameters
        final ServiceOffering offeringHandle = _entityMgr.findById(ServiceOffering.class, id);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("unable to find service offering " + id);
        }

        List<Long> existingDomainIds = _serviceOfferingDetailsDao.findDomainIds(id);
        Collections.sort(existingDomainIds);

        List<Long> existingZoneIds = _serviceOfferingDetailsDao.findZoneIds(id);
        Collections.sort(existingZoneIds);

        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);
        Collections.sort(filteredDomainIds);

        List<Long> filteredZoneIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            filteredZoneIds.addAll(zoneIds);
        }
        Collections.sort(filteredZoneIds);

        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            if (!filteredZoneIds.equals(existingZoneIds)) { // Domain-admins cannot update zone(s) for offerings
                throw new InvalidParameterValueException(String.format("Unable to update zone(s) for service offering: %s by admin: %s as it is domain-admin", offeringHandle.getUuid(), user.getUuid()));
            }
            if (existingDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to update public service offering: %s by user: %s because it is domain-admin", offeringHandle.getUuid(), user.getUuid()));
            } else {
                if (filteredDomainIds.isEmpty()) {
                    throw new InvalidParameterValueException(String.format("Unable to update service offering: %s to a public offering by user: %s because it is domain-admin", offeringHandle.getUuid(), user.getUuid()));
                }
            }
            if (!org.apache.commons.lang3.StringUtils.isAllBlank(hostTags, storageTags) && !ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS.valueIn(account.getAccountId())) {
                throw new InvalidParameterValueException(String.format("User [%s] is unable to update storage tags or host tags.", user.getUuid()));
            }
            List<Long> nonChildDomains = new ArrayList<>();
            for (Long domainId : existingDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    if (name != null || displayText != null || sortKey != null) { // Domain-admins cannot update name, display text, sort key for offerings with domain which are not child domains for domain-admin
                        throw new InvalidParameterValueException(String.format("Unable to update service offering: %s as it has linked domain(s) which are not child domain for domain-admin: %s", offeringHandle.getUuid(), user.getUuid()));
                    }
                    nonChildDomains.add(domainId);
                }
            }
            for (Long domainId : filteredDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    Domain domain = _entityMgr.findById(Domain.class, domainId);
                    throw new InvalidParameterValueException(String.format("Unable to update service offering: %s by domain-admin: %s with domain: %3$s which is not a child domain", offeringHandle.getUuid(), user.getUuid(), domain.getUuid()));
                }
            }
            filteredDomainIds.addAll(nonChildDomains); // Final list must include domains which were not child domain for domain-admin but specified for this offering prior to update
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to update service offering: %s by id user: %s because it is not root-admin or domain-admin", offeringHandle.getUuid(), user.getUuid()));
        }

        final boolean updateNeeded = name != null || displayText != null || sortKey != null || storageTags != null || hostTags != null;
        final boolean detailsUpdateNeeded = !filteredDomainIds.equals(existingDomainIds) || !filteredZoneIds.equals(existingZoneIds);
        if (!updateNeeded && !detailsUpdateNeeded) {
            return _serviceOfferingDao.findById(id);
        }

        ServiceOfferingVO offering = _serviceOfferingDao.createForUpdate(id);

        if (name != null) {
            offering.setName(name);
        }

        if (displayText != null) {
            offering.setDisplayText(displayText);
        }

        if (sortKey != null) {
            offering.setSortKey(sortKey);
        }

        DiskOfferingVO diskOffering = _diskOfferingDao.findById(offeringHandle.getDiskOfferingId());
        updateOfferingTagsIfIsNotNull(storageTags, diskOffering);
        _diskOfferingDao.update(diskOffering.getId(), diskOffering);

        updateServiceOfferingHostTagsIfNotNull(hostTags, offering);

        if (updateNeeded && !_serviceOfferingDao.update(id, offering)) {
            return null;
        }
        List<ServiceOfferingDetailsVO> detailsVO = new ArrayList<>();
        if(detailsUpdateNeeded) {
            SearchBuilder<ServiceOfferingDetailsVO> sb = _serviceOfferingDetailsDao.createSearchBuilder();
            sb.and("offeringId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.and("detailName", sb.entity().getName(), SearchCriteria.Op.EQ);
            sb.done();
            SearchCriteria<ServiceOfferingDetailsVO> sc = sb.create();
            sc.setParameters("offeringId", String.valueOf(id));
            if(!filteredDomainIds.equals(existingDomainIds)) {
                sc.setParameters("detailName", ApiConstants.DOMAIN_ID);
                _serviceOfferingDetailsDao.remove(sc);
                for (Long domainId : filteredDomainIds) {
                    detailsVO.add(new ServiceOfferingDetailsVO(id, ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
                }
            }
            if(!filteredZoneIds.equals(existingZoneIds)) {
                sc.setParameters("detailName", ApiConstants.ZONE_ID);
                _serviceOfferingDetailsDao.remove(sc);
                for (Long zoneId : filteredZoneIds) {
                    detailsVO.add(new ServiceOfferingDetailsVO(id, ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }
        }
        if (!detailsVO.isEmpty()) {
            for (ServiceOfferingDetailsVO detailVO : detailsVO) {
                _serviceOfferingDetailsDao.persist(detailVO);
            }
        }
        offering = _serviceOfferingDao.findById(id);
        CallContext.current().setEventDetails("Service offering id=" + offering.getId());
        return offering;
    }

    @Override
    public List<Long> getServiceOfferingDomains(Long serviceOfferingId) {
        final ServiceOffering offeringHandle = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find service offering " + serviceOfferingId);
        }
        return _serviceOfferingDetailsDao.findDomainIds(serviceOfferingId);
    }

    @Override
    public List<Long> getServiceOfferingZones(Long serviceOfferingId) {
        final ServiceOffering offeringHandle = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find service offering " + serviceOfferingId);
        }
        return _serviceOfferingDetailsDao.findZoneIds(serviceOfferingId);
    }

    protected DiskOfferingVO createDiskOffering(final Long userId, final List<Long> domainIds, final List<Long> zoneIds, final String name, final String description, final String provisioningType,
                                                final Long numGibibytes, String tags, boolean isCustomized, final boolean localStorageRequired,
                                                final boolean isDisplayOfferingEnabled, final Boolean isCustomizedIops, Long minIops, Long maxIops,
                                                Long bytesReadRate, Long bytesReadRateMax, Long bytesReadRateMaxLength,
                                                Long bytesWriteRate, Long bytesWriteRateMax, Long bytesWriteRateMaxLength,
                                                Long iopsReadRate, Long iopsReadRateMax, Long iopsReadRateMaxLength,
                                                Long iopsWriteRate, Long iopsWriteRateMax, Long iopsWriteRateMaxLength,
                                                final Integer hypervisorSnapshotReserve, String cacheMode, final Map<String, String> details, final Long storagePolicyID,
                                                final boolean diskSizeStrictness, final boolean encrypt) {
        long diskSize = 0;// special case for custom disk offerings
        long maxVolumeSizeInGb = VolumeOrchestrationService.MaxVolumeSize.value();
        if (numGibibytes != null && numGibibytes <= 0) {
            throw new InvalidParameterValueException("Please specify a disk size of at least 1 GB.");
        } else if (numGibibytes != null && numGibibytes > maxVolumeSizeInGb) {
            throw new InvalidParameterValueException(String.format("The maximum size for a disk is %d GB.", maxVolumeSizeInGb));
        }
        final ProvisioningType typedProvisioningType = ProvisioningType.getProvisioningType(provisioningType);

        if (numGibibytes != null) {
            diskSize = numGibibytes * 1024 * 1024 * 1024;
        }

        if (diskSize == 0) {
            isCustomized = true;
        }

        if (Boolean.TRUE.equals(isCustomizedIops) || isCustomizedIops == null) {
            minIops = null;
            maxIops = null;
        } else {
            if (minIops == null && maxIops == null) {
                minIops = 0L;
                maxIops = 0L;
            } else {
                if (minIops == null || minIops <= 0) {
                    throw new InvalidParameterValueException("The min IOPS must be greater than 0.");
                }

                if (maxIops == null) {
                    maxIops = 0L;
                }

                if (minIops > maxIops) {
                    throw new InvalidParameterValueException("The min IOPS must be less than or equal to the max IOPS.");
                }
            }
        }

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            if (filteredDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to create public disk offering by admin: %s because it is domain-admin", user.getUuid()));
            }
            if (StringUtils.isNotBlank(tags) && !ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS.valueIn(account.getAccountId())) {
                throw new InvalidParameterValueException(String.format("User [%s] is unable to create disk offerings with storage tags.", user.getUuid()));
            }
            for (Long domainId : filteredDomainIds) {
                if (domainId == null || !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException(String.format("Unable to create disk offering by another domain-admin: %s for domain: %s", user.getUuid(), _entityMgr.findById(Domain.class, domainId).getUuid()));
                }
            }
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to create disk offering by user: %s because it is not root-admin or domain-admin", user.getUuid()));
        }

        tags = com.cloud.utils.StringUtils.cleanupTags(tags);
        final DiskOfferingVO newDiskOffering = new DiskOfferingVO(name, description, typedProvisioningType, diskSize, tags, isCustomized,
                isCustomizedIops, minIops, maxIops);
        newDiskOffering.setUseLocalStorage(localStorageRequired);
        newDiskOffering.setDisplayOffering(isDisplayOfferingEnabled);

        setBytesRate(newDiskOffering, bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength, bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength);
        setIopsRate(newDiskOffering, iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength, iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength);

        if (cacheMode != null) {
            newDiskOffering.setCacheMode(DiskOffering.DiskCacheMode.valueOf(cacheMode.toUpperCase()));
        }

        if (hypervisorSnapshotReserve != null && hypervisorSnapshotReserve < 0) {
            throw new InvalidParameterValueException("If provided, Hypervisor Snapshot Reserve must be greater than or equal to 0.");
        }

        newDiskOffering.setEncrypt(encrypt);
        newDiskOffering.setHypervisorSnapshotReserve(hypervisorSnapshotReserve);
        newDiskOffering.setDiskSizeStrictness(diskSizeStrictness);

        CallContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
        final DiskOfferingVO offering = _diskOfferingDao.persist(newDiskOffering);
        if (offering != null) {
            List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
            for (Long domainId : filteredDomainIds) {
                detailsVO.add(new DiskOfferingDetailVO(offering.getId(), ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
            }
            if (CollectionUtils.isNotEmpty(zoneIds)) {
                for (Long zoneId : zoneIds) {
                    detailsVO.add(new DiskOfferingDetailVO(offering.getId(), ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }

            if (MapUtils.isNotEmpty(details)) {
                details.forEach((key, value) -> {
                    boolean displayDetail = !StringUtils.equalsAny(key, Volume.BANDWIDTH_LIMIT_IN_MBPS, Volume.IOPS_LIMIT);
                    detailsVO.add(new DiskOfferingDetailVO(offering.getId(), key, value, displayDetail));
                });
            }
            if (storagePolicyID != null) {
                detailsVO.add(new DiskOfferingDetailVO(offering.getId(), ApiConstants.STORAGE_POLICY, String.valueOf(storagePolicyID), false));
            }
            if (!detailsVO.isEmpty()) {
                diskOfferingDetailsDao.saveDetails(detailsVO);
            }
            CallContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
            CallContext.current().putContextParameter(DiskOffering.class, newDiskOffering.getId());
            return offering;
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_CREATE, eventDescription = "creating disk offering")
    public DiskOffering createDiskOffering(final CreateDiskOfferingCmd cmd) {
        final String name = cmd.getOfferingName();
        final String description = cmd.getDisplayText();
        final String provisioningType = cmd.getProvisioningType();
        final Long numGibibytes = cmd.getDiskSize();
        final boolean isDisplayOfferingEnabled = cmd.getDisplayOffering() != null ? cmd.getDisplayOffering() : true;
        final boolean isCustomized = cmd.isCustomized() != null ? cmd.isCustomized() : false; // false
        final String tags = cmd.getTags();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        final Map<String, String> details = cmd.getDetails();
        final Long storagePolicyId = cmd.getStoragePolicy();
        final boolean diskSizeStrictness =  cmd.getDiskSizeStrictness();

        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        if (!isCustomized && numGibibytes == null) {
            throw new InvalidParameterValueException("Disksize is required for a non-customized disk offering");
        }

        if (isCustomized && numGibibytes != null) {
            throw new InvalidParameterValueException("Disksize is not allowed for a customized disk offering");
        }

        // check if cache_mode parameter is valid
        validateCacheMode(cmd.getCacheMode());

        boolean localStorageRequired = false;
        final String storageType = cmd.getStorageType();
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                localStorageRequired = true;
            } else if (!storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
            }
        }

        if (storagePolicyId != null) {
            if (vsphereStoragePolicyDao.findById(storagePolicyId) == null) {
                throw new InvalidParameterValueException("Please specify a valid vSphere storage policy id");
            }
        }

        final Boolean isCustomizedIops = cmd.isCustomizedIops();
        final Long minIops = cmd.getMinIops();
        final Long maxIops = cmd.getMaxIops();
        final Long bytesReadRate = cmd.getBytesReadRate();
        final Long bytesReadRateMax = cmd.getBytesReadRateMax();
        final Long bytesReadRateMaxLength = cmd.getBytesReadRateMaxLength();
        final Long bytesWriteRate = cmd.getBytesWriteRate();
        final Long bytesWriteRateMax = cmd.getBytesWriteRateMax();
        final Long bytesWriteRateMaxLength = cmd.getBytesWriteRateMaxLength();
        final Long iopsReadRate = cmd.getIopsReadRate();
        final Long iopsReadRateMax = cmd.getIopsReadRateMax();
        final Long iopsReadRateMaxLength = cmd.getIopsReadRateMaxLength();
        final Long iopsWriteRate = cmd.getIopsWriteRate();
        final Long iopsWriteRateMax = cmd.getIopsWriteRateMax();
        final Long iopsWriteRateMaxLength = cmd.getIopsWriteRateMaxLength();
        final Integer hypervisorSnapshotReserve = cmd.getHypervisorSnapshotReserve();
        final String cacheMode = cmd.getCacheMode();
        final boolean encrypt = cmd.getEncrypt();

        validateMaxRateEqualsOrGreater(iopsReadRate, iopsReadRateMax, IOPS_READ_RATE);
        validateMaxRateEqualsOrGreater(iopsWriteRate, iopsWriteRateMax, IOPS_WRITE_RATE);
        validateMaxRateEqualsOrGreater(bytesReadRate, bytesReadRateMax, BYTES_READ_RATE);
        validateMaxRateEqualsOrGreater(bytesWriteRate, bytesWriteRateMax, BYTES_WRITE_RATE);

        validateMaximumIopsAndBytesLength(iopsReadRateMaxLength, iopsWriteRateMaxLength, bytesReadRateMaxLength, bytesWriteRateMaxLength);

        final Long userId = CallContext.current().getCallingUserId();
        return createDiskOffering(userId, domainIds, zoneIds, name, description, provisioningType, numGibibytes, tags, isCustomized,
                localStorageRequired, isDisplayOfferingEnabled, isCustomizedIops, minIops,
                maxIops, bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength, bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength,
                iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength, iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength,
                hypervisorSnapshotReserve, cacheMode, details, storagePolicyId, diskSizeStrictness, encrypt);
    }

    /**
     * Validates rate offerings, being flexible about which rate is being validated (e.g. read/write Bytes, read/write IOPS).</br>
     * It throws InvalidParameterValueException if normal rate is greater than maximum rate
     */
    protected void validateMaxRateEqualsOrGreater(Long normalRate, Long maxRate, String rateType) {
        if (normalRate != null && maxRate != null && maxRate < normalRate) {
            throw new InvalidParameterValueException(
                    String.format("%s rate (%d) cannot be greater than %s maximum rate (%d)", rateType, normalRate, rateType, maxRate));
        }
    }

    /**
     *  Throws InvalidParameterValueException if At least one of the VM disk Bytes/IOPS Read/Write length are smaller than the respective disk offering max length.</br>
     *  It will ignore verification in case of default values (zero):
     * <ul>
     *  <li>vm.disk.bytes.maximum.read.length = 0</li>
     *  <li>vm.disk.bytes.maximum.write.length = 0</li>
     *  <li>vm.disk.iops.maximum.read.length = 0</li>
     *  <li>vm.disk.iops.maximum.write.length = 0</li>
     * </ul>
     */
    protected void validateMaximumIopsAndBytesLength(final Long iopsReadRateMaxLength, final Long iopsWriteRateMaxLength, Long bytesReadRateMaxLength, Long bytesWriteRateMaxLength) {
        if (IOPS_MAX_READ_LENGTH.value() != null && IOPS_MAX_READ_LENGTH.value() != 0l) {
            if (iopsReadRateMaxLength != null && iopsReadRateMaxLength > IOPS_MAX_READ_LENGTH.value()) {
                throw new InvalidParameterValueException(String.format("IOPS read max length (%d seconds) cannot be greater than vm.disk.iops.maximum.read.length (%d seconds)",
                        iopsReadRateMaxLength, IOPS_MAX_READ_LENGTH.value()));
            }
        }

        if (IOPS_MAX_WRITE_LENGTH.value() != null && IOPS_MAX_WRITE_LENGTH.value() != 0l) {
            if (iopsWriteRateMaxLength != null && iopsWriteRateMaxLength > IOPS_MAX_WRITE_LENGTH.value()) {
                throw new InvalidParameterValueException(String.format("IOPS write max length (%d seconds) cannot be greater than vm.disk.iops.maximum.write.length (%d seconds)",
                        iopsWriteRateMaxLength, IOPS_MAX_WRITE_LENGTH.value()));
            }
        }

        if (BYTES_MAX_READ_LENGTH.value() != null && BYTES_MAX_READ_LENGTH.value() != 0l) {
            if (bytesReadRateMaxLength != null && bytesReadRateMaxLength > BYTES_MAX_READ_LENGTH.value()) {
                throw new InvalidParameterValueException(String.format("Bytes read max length (%d seconds) cannot be greater than vm.disk.bytes.maximum.read.length (%d seconds)",
                        bytesReadRateMaxLength, BYTES_MAX_READ_LENGTH.value()));
            }
        }

        if (BYTES_MAX_WRITE_LENGTH.value() != null && BYTES_MAX_WRITE_LENGTH.value() != 0l) {
            if (bytesWriteRateMaxLength != null && bytesWriteRateMaxLength > BYTES_MAX_WRITE_LENGTH.value()) {
                throw new InvalidParameterValueException(String.format("Bytes write max length (%d seconds) cannot be greater than vm.disk.bytes.maximum.write.length (%d seconds)",
                        bytesWriteRateMaxLength, BYTES_MAX_WRITE_LENGTH.value()));
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_EDIT, eventDescription = "updating disk offering")
    public DiskOffering updateDiskOffering(final UpdateDiskOfferingCmd cmd) {
        final Long diskOfferingId = cmd.getId();
        final String name = cmd.getDiskOfferingName();
        final String displayText = cmd.getDisplayText();
        final Integer sortKey = cmd.getSortKey();
        final Boolean displayDiskOffering = cmd.getDisplayOffering();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        final String tags = cmd.getTags();

        Long bytesReadRate = cmd.getBytesReadRate();
        Long bytesReadRateMax = cmd.getBytesReadRateMax();
        Long bytesReadRateMaxLength = cmd.getBytesReadRateMaxLength();
        Long bytesWriteRate = cmd.getBytesWriteRate();
        Long bytesWriteRateMax = cmd.getBytesWriteRateMax();
        Long bytesWriteRateMaxLength = cmd.getBytesWriteRateMaxLength();
        Long iopsReadRate = cmd.getIopsReadRate();
        Long iopsReadRateMax = cmd.getIopsReadRateMax();
        Long iopsReadRateMaxLength = cmd.getIopsReadRateMaxLength();
        Long iopsWriteRate = cmd.getIopsWriteRate();
        Long iopsWriteRateMax = cmd.getIopsWriteRateMax();
        Long iopsWriteRateMaxLength = cmd.getIopsWriteRateMaxLength();
        String cacheMode = cmd.getCacheMode();

        // Check if diskOffering exists
        final DiskOffering diskOfferingHandle = _entityMgr.findById(DiskOffering.class, diskOfferingId);
        if (diskOfferingHandle == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        List<Long> existingDomainIds = diskOfferingDetailsDao.findDomainIds(diskOfferingId);
        Collections.sort(existingDomainIds);

        List<Long> existingZoneIds = diskOfferingDetailsDao.findZoneIds(diskOfferingId);
        Collections.sort(existingZoneIds);

        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);
        Collections.sort(filteredDomainIds);

        List<Long> filteredZoneIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            filteredZoneIds.addAll(zoneIds);
        }
        Collections.sort(filteredZoneIds);

        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            if (!filteredZoneIds.equals(existingZoneIds)) { // Domain-admins cannot update zone(s) for offerings
                throw new InvalidParameterValueException(String.format("Unable to update zone(s) for disk offering: %s by admin: %s as it is domain-admin", diskOfferingHandle.getUuid(), user.getUuid()));
            }
            if (existingDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to update public disk offering: %s by user: %s because it is domain-admin", diskOfferingHandle.getUuid(), user.getUuid()));
            } else {
                if (filteredDomainIds.isEmpty()) {
                    throw new InvalidParameterValueException(String.format("Unable to update disk offering: %s to a public offering by user: %s because it is domain-admin", diskOfferingHandle.getUuid(), user.getUuid()));
                }
            }
            if (StringUtils.isNotBlank(tags) && !ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS.valueIn(account.getAccountId())) {
                throw new InvalidParameterValueException(String.format("User [%s] is unable to update disk offering tags.", user.getUuid()));
            }

            List<Long> nonChildDomains = new ArrayList<>();
            for (Long domainId : existingDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    if (name != null || displayText != null || sortKey != null) { // Domain-admins cannot update name, display text, sort key for offerings with domain which are not child domains for domain-admin
                        throw new InvalidParameterValueException(String.format("Unable to update disk offering: %s as it has linked domain(s) which are not child domain for domain-admin: %s", diskOfferingHandle.getUuid(), user.getUuid()));
                    }
                    nonChildDomains.add(domainId);
                }
            }
            for (Long domainId : filteredDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    Domain domain = _entityMgr.findById(Domain.class, domainId);
                    throw new InvalidParameterValueException(String.format("Unable to update disk offering: %s by domain-admin: %s with domain: %3$s which is not a child domain", diskOfferingHandle.getUuid(), user.getUuid(), domain.getUuid()));
                }
            }
            filteredDomainIds.addAll(nonChildDomains); // Final list must include domains which were not child domain for domain-admin but specified for this offering prior to update
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to update disk offering: %s by id user: %s because it is not root-admin or domain-admin", diskOfferingHandle.getUuid(), user.getUuid()));
        }

        boolean updateNeeded = shouldUpdateDiskOffering(name, displayText, sortKey, displayDiskOffering, tags, cacheMode) ||
                shouldUpdateIopsRateParameters(iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength, iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength) ||
                shouldUpdateBytesRateParameters(bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength, bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength);

        final boolean detailsUpdateNeeded = !filteredDomainIds.equals(existingDomainIds) || !filteredZoneIds.equals(existingZoneIds);
        if (!updateNeeded && !detailsUpdateNeeded) {
            return _diskOfferingDao.findById(diskOfferingId);
        }

        final DiskOfferingVO diskOffering = _diskOfferingDao.createForUpdate(diskOfferingId);

        if (name != null) {
            diskOffering.setName(name);
        }

        if (displayText != null) {
            diskOffering.setDisplayText(displayText);
        }

        if (sortKey != null) {
            diskOffering.setSortKey(sortKey);
        }

        if (displayDiskOffering != null) {
            diskOffering.setDisplayOffering(displayDiskOffering);
        }

        updateOfferingTagsIfIsNotNull(tags, diskOffering);

        validateMaxRateEqualsOrGreater(iopsReadRate, iopsReadRateMax, IOPS_READ_RATE);
        validateMaxRateEqualsOrGreater(iopsWriteRate, iopsWriteRateMax, IOPS_WRITE_RATE);
        validateMaxRateEqualsOrGreater(bytesReadRate, bytesReadRateMax, BYTES_READ_RATE);
        validateMaxRateEqualsOrGreater(bytesWriteRate, bytesWriteRateMax, BYTES_WRITE_RATE);
        validateMaximumIopsAndBytesLength(iopsReadRateMaxLength, iopsWriteRateMaxLength, bytesReadRateMaxLength, bytesWriteRateMaxLength);

        setBytesRate(diskOffering, bytesReadRate, bytesReadRateMax, bytesReadRateMaxLength, bytesWriteRate, bytesWriteRateMax, bytesWriteRateMaxLength);
        setIopsRate(diskOffering, iopsReadRate, iopsReadRateMax, iopsReadRateMaxLength, iopsWriteRate, iopsWriteRateMax, iopsWriteRateMaxLength);

        if (cacheMode != null) {
            validateCacheMode(cacheMode);
            diskOffering.setCacheMode(DiskOffering.DiskCacheMode.valueOf(cacheMode.toUpperCase()));
        }

        if (updateNeeded && !_diskOfferingDao.update(diskOfferingId, diskOffering)) {
            return null;
        }
        List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
        if(detailsUpdateNeeded) {
            SearchBuilder<DiskOfferingDetailVO> sb = diskOfferingDetailsDao.createSearchBuilder();
            sb.and("offeringId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.and("detailName", sb.entity().getName(), SearchCriteria.Op.EQ);
            sb.done();
            SearchCriteria<DiskOfferingDetailVO> sc = sb.create();
            sc.setParameters("offeringId", String.valueOf(diskOfferingId));
            if(!filteredDomainIds.equals(existingDomainIds)) {
                sc.setParameters("detailName", ApiConstants.DOMAIN_ID);
                diskOfferingDetailsDao.remove(sc);
                for (Long domainId : filteredDomainIds) {
                    detailsVO.add(new DiskOfferingDetailVO(diskOfferingId, ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
                }
            }
            if(!filteredZoneIds.equals(existingZoneIds)) {
                sc.setParameters("detailName", ApiConstants.ZONE_ID);
                diskOfferingDetailsDao.remove(sc);
                for (Long zoneId : filteredZoneIds) {
                    detailsVO.add(new DiskOfferingDetailVO(diskOfferingId, ApiConstants.ZONE_ID, String.valueOf(zoneId), false));
                }
            }
        }
        if (!detailsVO.isEmpty()) {
            for (DiskOfferingDetailVO detailVO : detailsVO) {
                diskOfferingDetailsDao.persist(detailVO);
            }
        }
        CallContext.current().setEventDetails("Disk offering id=" + diskOffering.getId());
        return _diskOfferingDao.findById(diskOfferingId);
    }

    /**
     * Check the tags parameters to the disk/service offering
     * <ul>
     *     <li>If tags is null, do nothing and return.</li>
     *     <li>If tags is not null, will set tag to the disk/service offering if the pools with active volumes have the new tags.</li>
     *     <li>If tags is an blank string, set null on disk/service offering tag.</li>
     * </ul>
     */
    protected void updateOfferingTagsIfIsNotNull(String tags, DiskOfferingVO diskOffering) {
        if (tags == null) { return; }
        if (StringUtils.isNotBlank(tags)) {
            tags = com.cloud.utils.StringUtils.cleanupTags(tags);
            List<StoragePoolVO> pools = _storagePoolDao.listStoragePoolsWithActiveVolumesByOfferingId(diskOffering.getId());
            if (CollectionUtils.isNotEmpty(pools)) {
                List<String> listOfTags = Arrays.asList(tags.split(","));
                for (StoragePoolVO storagePoolVO : pools) {
                    List<String> tagsOnPool = storagePoolTagDao.getStoragePoolTags(storagePoolVO.getId());
                    if (CollectionUtils.isEmpty(tagsOnPool) || !tagsOnPool.containsAll(listOfTags)) {
                        DiskOfferingVO offeringToRetrieveInfo = _diskOfferingDao.findById(diskOffering.getId());
                        List<VolumeVO> volumes = _volumeDao.findByDiskOfferingId(diskOffering.getId());
                        String listOfVolumesNamesAndUuid = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumes, "name", "uuid");
                        String diskOfferingInfo = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(offeringToRetrieveInfo, "name", "uuid");
                        String poolInfo = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(storagePoolVO, "name", "uuid");
                        throw new InvalidParameterValueException(String.format("There are active volumes using the disk offering %s, and the pool %s doesn't have the new tags. " +
                                "The following volumes are using the mentioned disk offering %s. Please first add the new tags to the mentioned storage pools before adding them" +
                                " to the disk offering.", diskOfferingInfo, poolInfo, listOfVolumesNamesAndUuid));
                    }
                }
            }
            diskOffering.setTags(tags);
        } else {
            diskOffering.setTags(null);
        }
    }

    /**
     * Check the host tags parameters to the service offering
     * <ul>
     *     <li>If host tags is null, do nothing and return.</li>
     *     <li>If host tags is not null, will set host tag to the service offering if the hosts with active VMs have the new tags.</li>
     *     <li>If host tags is an blank string, set null on service offering tag.</li>
     * </ul>
     */
    protected void updateServiceOfferingHostTagsIfNotNull(String hostTags, ServiceOfferingVO offering) {
        if (hostTags == null) {
            return;
        }
        if (StringUtils.isNotBlank(hostTags)) {
            hostTags = com.cloud.utils.StringUtils.cleanupTags(hostTags);
            List<HostVO> hosts = _hostDao.listHostsWithActiveVMs(offering.getId());
            if (CollectionUtils.isNotEmpty(hosts)) {
                List<String> listOfHostTags = Arrays.asList(hostTags.split(","));
                for (HostVO host : hosts) {
                    List<String> tagsOnHost = hostTagDao.getHostTags(host.getId());
                    if (CollectionUtils.isEmpty(tagsOnHost) || !tagsOnHost.containsAll(listOfHostTags)) {
                        throw new InvalidParameterValueException(String.format("There are active VMs using offering [%s], and the hosts [%s] don't have the new tags", offering.getId(), hosts));
                    }
                }
            }
            offering.setHostTag(hostTags);
        } else {
            offering.setHostTag(null);
        }
    }

    /**
     * Check if it needs to update any parameter when updateDiskoffering is called
     * Verify if name or displayText are not blank, tags is not null, sortkey and displayDiskOffering is not null
     */
    protected boolean shouldUpdateDiskOffering(String name, String displayText, Integer sortKey, Boolean displayDiskOffering, String tags, String cacheMode) {
        return !StringUtils.isAllBlank(name, displayText, cacheMode) || tags != null || sortKey != null || displayDiskOffering != null;
    }

    protected boolean shouldUpdateBytesRateParameters(Long bytesReadRate, Long bytesReadRateMax, Long bytesReadRateMaxLength, Long bytesWriteRate, Long bytesWriteRateMax, Long bytesWriteRateMaxLength) {
        return bytesReadRate != null || bytesReadRateMax != null || bytesReadRateMaxLength != null || bytesWriteRate != null ||
                bytesWriteRateMax != null || bytesWriteRateMaxLength != null;
    }

    protected boolean shouldUpdateIopsRateParameters(Long iopsReadRate, Long iopsReadRateMax, Long iopsReadRateMaxLength, Long iopsWriteRate, Long iopsWriteRateMax, Long iopsWriteRateMaxLength) {
        return iopsReadRate != null || iopsReadRateMax != null || iopsReadRateMaxLength != null || iopsWriteRate != null || iopsWriteRateMax != null || iopsWriteRateMaxLength != null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_DELETE, eventDescription = "deleting disk offering")
    public boolean deleteDiskOffering(final DeleteDiskOfferingCmd cmd) {
        final Long diskOfferingId = cmd.getId();

        final DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);

        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            List<Long> existingDomainIds = diskOfferingDetailsDao.findDomainIds(diskOfferingId);
            if (existingDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to delete public disk offering: %s by admin: %s because it is domain-admin", offering.getUuid(), user.getUuid()));
            }
            for (Long domainId : existingDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException(String.format("Unable to delete disk offering: %s as it has linked domain(s) which are not child domain for domain-admin: %s", offering.getUuid(), user.getUuid()));
                }
            }
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to delete disk offering: %s by user: %s because it is not root-admin or domain-admin", offering.getUuid(), user.getUuid()));
        }

        annotationDao.removeByEntityType(AnnotationService.EntityType.DISK_OFFERING.name(), offering.getUuid());
        offering.setState(DiskOffering.State.Inactive);
        if (_diskOfferingDao.update(offering.getId(), offering)) {
            CallContext.current().setEventDetails("Disk offering id=" + diskOfferingId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Long> getDiskOfferingDomains(Long diskOfferingId) {
        final DiskOffering offeringHandle = _entityMgr.findById(DiskOffering.class, diskOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
        }
        return diskOfferingDetailsDao.findDomainIds(diskOfferingId);
    }

    @Override
    public List<Long> getDiskOfferingZones(Long diskOfferingId) {
        final DiskOffering offeringHandle = _entityMgr.findById(DiskOffering.class, diskOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
        }
        return diskOfferingDetailsDao.findZoneIds(diskOfferingId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_DELETE, eventDescription = "deleting service offering")
    public boolean deleteServiceOffering(final DeleteServiceOfferingCmd cmd) {

        final Long offeringId = cmd.getId();
        Long userId = CallContext.current().getCallingUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify service offering id
        final ServiceOfferingVO offering = _serviceOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find service offering " + offeringId);
        }

        // Verify disk offering id mapped to the service offering
        final DiskOfferingVO diskOffering = _diskOfferingDao.findById(offering.getDiskOfferingId());
        if (diskOffering == null) {
            throw new InvalidParameterValueException("unable to find disk offering " + offering.getDiskOfferingId() + " mapped to the service offering " + offeringId);
        }

        if (offering.getDefaultUse()) {
            throw new InvalidParameterValueException("Default service offerings cannot be deleted");
        }

        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.Type.DOMAIN_ADMIN) {
            List<Long> existingDomainIds = _serviceOfferingDetailsDao.findDomainIds(offeringId);
            if (existingDomainIds.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to delete public service offering: %s by admin: %s because it is domain-admin", offering.getUuid(), user.getUuid()));
            }
            for (Long domainId : existingDomainIds) {
                if (!_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new InvalidParameterValueException(String.format("Unable to delete service offering: %s as it has linked domain(s) which are not child domain for domain-admin: %s", offering.getUuid(), user.getUuid()));
                }
            }
        } else if (account.getType() != Account.Type.ADMIN) {
            throw new InvalidParameterValueException(String.format("Unable to delete service offering: %s by user: %s because it is not root-admin or domain-admin", offering.getUuid(), user.getUuid()));
        }

        annotationDao.removeByEntityType(AnnotationService.EntityType.SERVICE_OFFERING.name(), offering.getUuid());
        if (diskOffering.isComputeOnly()) {
            diskOffering.setState(DiskOffering.State.Inactive);
            if (!_diskOfferingDao.update(diskOffering.getId(), diskOffering)) {
                throw new CloudRuntimeException(String.format("Unable to delete disk offering %s mapped to the service offering %s", diskOffering.getUuid(), offering.getUuid()));
            }
        }
        offering.setState(ServiceOffering.State.Inactive);
        if (_serviceOfferingDao.update(offeringId, offering)) {
            CallContext.current().setEventDetails("Service offering id=" + offeringId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_CREATE, eventDescription = "creating vlan ip range", async = false)
    public Vlan createVlanAndPublicIpRange(final CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
    ResourceAllocationException {
        Long zoneId = cmd.getZoneId();
        final Long podId = cmd.getPodId();
        final String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        final String newVlanGateway = cmd.getGateway();
        final String newVlanNetmask = cmd.getNetmask();
        String vlanId = cmd.getVlan();
        // TODO decide if we should be forgiving or demand a valid and complete URI
        if (!(vlanId == null || "".equals(vlanId) || vlanId.startsWith(BroadcastDomainType.Vlan.scheme()))) {
            vlanId = BroadcastDomainType.Vlan.toUri(vlanId).toString();
        }
        final Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        Long networkId = cmd.getNetworkID();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final String accountName = cmd.getAccountName();
        final Long projectId = cmd.getProjectId();
        final Long domainId = cmd.getDomainId();
        final String startIPv6 = cmd.getStartIpv6();
        String endIPv6 = cmd.getEndIpv6();
        final String ip6Gateway = cmd.getIp6Gateway();
        final String ip6Cidr = cmd.getIp6Cidr();
        final Boolean forSystemVms = cmd.isForSystemVms();

        Account vlanOwner = null;

        if (forSystemVms && accountName != null) {
            throw new InvalidParameterValueException("Account name should not be provided when ForSystemVMs is enabled");
        }

        final boolean ipv4 = startIP != null;
        final boolean ipv6 = ip6Cidr != null;

        if (!ipv4 && !ipv6) {
            throw new InvalidParameterValueException("StartIP or IPv6 CIDR is missing in the parameters!");
        }

        if (ipv4) {
            // if end ip is not specified, default it to startIp
            if (endIP == null && startIP != null) {
                endIP = startIP;
            }
        }

        if (ipv6) {
            // if end ip is not specified, default it to startIp
            if (endIPv6 == null && startIPv6 != null) {
                endIPv6 = startIPv6;
            }

            IPv6Network iPv6Network = IPv6Network.fromString(ip6Cidr);
            if (iPv6Network.getNetmask().asPrefixLength() > Ipv6Service.IPV6_SLAAC_CIDR_NETMASK) {
                throw new InvalidParameterValueException(String.format("For IPv6 range, prefix must be /%d or less", Ipv6Service.IPV6_SLAAC_CIDR_NETMASK));
            }
        }

        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("Account and projectId are mutually exclusive");
            }
            final Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }

            vlanOwner = _accountMgr.getAccount(project.getProjectAccountId());
            if (vlanOwner == null) {
                throw new InvalidParameterValueException("Please specify a valid projectId");
            }
        }

        Domain domain = null;
        if (accountName != null && domainId != null) {
            vlanOwner = _accountDao.findActiveAccount(accountName, domainId);
            if (vlanOwner == null) {
                throw new InvalidParameterValueException("Please specify a valid account.");
            } else if (vlanOwner.getId() == Account.ACCOUNT_ID_SYSTEM) {
                // by default vlan is dedicated to system account
                vlanOwner = null;
            }
        } else if (domainId != null) {
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Please specify a valid domain id");
            }
        }

        // Verify that network exists
        Network network = null;
        if (networkId != null) {
            network = _networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkId);
            } else {
                zoneId = network.getDataCenterId();
                physicalNetworkId = network.getPhysicalNetworkId();
            }
        }

        // Verify that zone exists
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // verify that physical network exists
        PhysicalNetworkVO pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find Physical Network with id=" + physicalNetworkId);
            }
            if (zoneId == null) {
                zoneId = pNtwk.getDataCenterId();
            }
        } else {
            if (zoneId == null) {
                throw new InvalidParameterValueException("");
            }
            // deduce physicalNetworkFrom Zone or Network.
            if (network != null && network.getPhysicalNetworkId() != null) {
                physicalNetworkId = network.getPhysicalNetworkId();
            } else {
                if (forVirtualNetwork) {
                    // default physical network with public traffic in the zone
                    physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
                } else {
                    if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
                        // default physical network with guest traffic in the
                        // zone
                        physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Guest).getId();
                    } else if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
                        if (zone.isSecurityGroupEnabled()) {
                            physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Guest).getId();
                        } else {
                            throw new InvalidParameterValueException("Physical Network Id is null, please provide the Network id for Direct vlan creation ");
                        }
                    }
                }
            }
        }

        // Check if zone is enabled
        final Account caller = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        if (zone.isSecurityGroupEnabled() && zone.getNetworkType() != DataCenter.NetworkType.Basic && forVirtualNetwork) {
            throw new InvalidParameterValueException("Can't add virtual ip range into a zone with security group enabled");
        }

        // If networkId is not specified, and vlan is Virtual or Direct
        // Untagged, try to locate default networks
        if (forVirtualNetwork) {
            if (network == null) {
                // find default public network in the zone
                networkId = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
                network = _networkModel.getNetwork(networkId);
            } else if (network.getGuestType() != null || network.getTrafficType() != TrafficType.Public) {
                throw new InvalidParameterValueException("Can't find Public network by id=" + networkId);
            }
        } else {
            if (network == null) {
                if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
                    networkId = _networkModel.getExclusiveGuestNetwork(zoneId).getId();
                    network = _networkModel.getNetwork(networkId);
                } else {
                    network = _networkModel.getNetworkWithSecurityGroupEnabled(zoneId);
                    if (network == null) {
                        throw new InvalidParameterValueException("Nework id is required for Direct vlan creation ");
                    }
                    networkId = network.getId();
                    zoneId = network.getDataCenterId();
                }
            } else if (network.getGuestType() == null ||
                    network.getGuestType() == Network.GuestType.Isolated
                    && _ntwkOffServiceMapDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Service.SourceNat)) {
                throw new InvalidParameterValueException("Can't create direct vlan for network id=" + networkId + " with type: " + network.getGuestType());
            }
        }

        Pair<Boolean, Pair<String, String>> sameSubnet = null;
        // Can add vlan range only to the network which allows it
        if (!network.getSpecifyIpRanges()) {
            throw new InvalidParameterValueException("Network " + network + " doesn't support adding ip ranges");
        }

        if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
            if (network.getTrafficType() == TrafficType.Guest) {
                if (network.getGuestType() != GuestType.Shared) {
                    throw new InvalidParameterValueException("Can execute createVLANIpRanges on shared guest network, but type of this guest network " + network.getId() + " is "
                            + network.getGuestType());
                }

                final List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
                if (vlans != null && vlans.size() > 0) {
                    final VlanVO vlan = vlans.get(0);
                    if (vlanId == null || vlanId.contains(Vlan.UNTAGGED)) {
                        vlanId = vlan.getVlanTag();
                    } else if (!NetUtils.isSameIsolationId(vlan.getVlanTag(), vlanId)) {
                        throw new InvalidParameterValueException("there is already one vlan " + vlan.getVlanTag() + " on network :" + +network.getId()
                                + ", only one vlan is allowed on guest network");
                    }
                }
                sameSubnet = validateIpRange(startIP, endIP, newVlanGateway, newVlanNetmask, vlans, ipv4, ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);

            }

        } else if (network.getTrafficType() == TrafficType.Management) {
            throw new InvalidParameterValueException("Cannot execute createVLANIpRanges on management network");
        } else if (zone.getNetworkType() == NetworkType.Basic) {
            final List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
            sameSubnet = validateIpRange(startIP, endIP, newVlanGateway, newVlanNetmask, vlans, ipv4, ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);
        }

        if (zoneId == null || ipv6 && (ip6Gateway == null || ip6Cidr == null)) {
            throw new InvalidParameterValueException("Gateway, netmask and zoneId have to be passed in for virtual and direct untagged networks");
        }

        if (forVirtualNetwork) {
            if (vlanOwner != null) {

                final long accountIpRange = NetUtils.ip2Long(endIP) - NetUtils.ip2Long(startIP) + 1;

                // check resource limits
                _resourceLimitMgr.checkResourceLimit(vlanOwner, ResourceType.public_ip, accountIpRange);
            }
        }
        // Check if the IP range overlaps with the private ip
        if (ipv4) {
            checkOverlapPrivateIpRange(zoneId, startIP, endIP);
        }

        return commitVlan(zoneId, podId, startIP, endIP, newVlanGateway, newVlanNetmask, vlanId, forVirtualNetwork, forSystemVms, networkId, physicalNetworkId, startIPv6, endIPv6, ip6Gateway,
                ip6Cidr, domain, vlanOwner, network, sameSubnet);
    }

    private Vlan commitVlan(final Long zoneId, final Long podId, final String startIP, final String endIP, final String newVlanGatewayFinal, final String newVlanNetmaskFinal,
            final String vlanId, final Boolean forVirtualNetwork, final Boolean forSystemVms, final Long networkId, final Long physicalNetworkId, final String startIPv6, final String endIPv6,
            final String ip6Gateway, final String ip6Cidr, final Domain domain, final Account vlanOwner, final Network network, final Pair<Boolean, Pair<String, String>> sameSubnet) {
        final GlobalLock commitVlanLock = GlobalLock.getInternLock("CommitVlan");
        commitVlanLock.lock(5);
        s_logger.debug("Acquiring lock for committing vlan");
        try {
            Vlan vlan = Transaction.execute(new TransactionCallback<Vlan>() {
                @Override
                public Vlan doInTransaction(final TransactionStatus status) {
                    String newVlanNetmask = newVlanNetmaskFinal;
                    String newVlanGateway = newVlanGatewayFinal;

                    if ((sameSubnet == null || !sameSubnet.first()) && network.getTrafficType() == TrafficType.Guest && network.getGuestType() == GuestType.Shared
                            && _vlanDao.listVlansByNetworkId(networkId) != null) {
                        final Map<Capability, String> dhcpCapabilities = _networkSvc.getNetworkOfferingServiceCapabilities(_networkOfferingDao.findById(network.getNetworkOfferingId()),
                            Service.Dhcp);
                        final String supportsMultipleSubnets = dhcpCapabilities.get(Capability.DhcpAccrossMultipleSubnets);
                        if (supportsMultipleSubnets == null || !Boolean.valueOf(supportsMultipleSubnets)) {
                            throw new  InvalidParameterValueException("The dhcp service provider for this network does not support dhcp across multiple subnets");
                        }
                        s_logger.info("adding a new subnet to the network " + network.getId());
                    } else if (sameSubnet != null) {
                        // if it is same subnet the user might not send the vlan and the
                        // netmask details. so we are
                        // figuring out while validation and setting them here.
                        newVlanGateway = sameSubnet.second().first();
                        newVlanNetmask = sameSubnet.second().second();
                    }
                    final Vlan vlan = createVlanAndPublicIpRange(zoneId, networkId, physicalNetworkId, forVirtualNetwork, forSystemVms, podId, startIP, endIP, newVlanGateway, newVlanNetmask, vlanId,
                            false, domain, vlanOwner, startIPv6, endIPv6, ip6Gateway, ip6Cidr);
                    // create an entry in the nic_secondary table. This will be the new
                    // gateway that will be configured on the corresponding routervm.
                    return vlan;
                }
            });

            messageBus.publish(_name, MESSAGE_CREATE_VLAN_IP_RANGE_EVENT, PublishScope.LOCAL, vlan);

            return vlan;
        } finally {
            commitVlanLock.unlock();
        }
    }

    public NetUtils.SupersetOrSubset checkIfSubsetOrSuperset(String vlanGateway, String vlanNetmask, String newVlanGateway, String newVlanNetmask, final String newStartIP, final String newEndIP) {
        if (newVlanGateway == null && newVlanNetmask == null) {
            newVlanGateway = vlanGateway;
            newVlanNetmask = vlanNetmask;
            // this means we are trying to add to the existing subnet.
            if (NetUtils.sameSubnet(newStartIP, newVlanGateway, newVlanNetmask)) {
                if (NetUtils.sameSubnet(newEndIP, newVlanGateway, newVlanNetmask)) {
                    return NetUtils.SupersetOrSubset.sameSubnet;
                }
            }
            return NetUtils.SupersetOrSubset.neitherSubetNorSuperset;
        } else if (newVlanGateway == null || newVlanNetmask == null) {
            throw new InvalidParameterValueException(
                    "either both netmask and gateway should be passed or both should me omited.");
        } else {
            if (!NetUtils.sameSubnet(newStartIP, newVlanGateway, newVlanNetmask)) {
                throw new InvalidParameterValueException("The start ip and gateway do not belong to the same subnet");
            }
            if (!NetUtils.sameSubnet(newEndIP, newVlanGateway, newVlanNetmask)) {
                throw new InvalidParameterValueException("The end ip and gateway do not belong to the same subnet");
            }
        }
        final String cidrnew = NetUtils.getCidrFromGatewayAndNetmask(newVlanGateway, newVlanNetmask);
        final String existing_cidr = NetUtils.getCidrFromGatewayAndNetmask(vlanGateway, vlanNetmask);

        return NetUtils.isNetowrkASubsetOrSupersetOfNetworkB(cidrnew, existing_cidr);
    }

    public Pair<Boolean, Pair<String, String>> validateIpRange(final String startIP, final String endIP, final String newVlanGateway, final String newVlanNetmask, final List<VlanVO> vlans, final boolean ipv4,
            final boolean ipv6, String ip6Gateway, String ip6Cidr, final String startIPv6, final String endIPv6, final Network network) {
        String vlanGateway = null;
        String vlanNetmask = null;
        boolean sameSubnet = false;
        if (CollectionUtils.isNotEmpty(vlans)) {
            for (final VlanVO vlan : vlans) {
                vlanGateway = vlan.getVlanGateway();
                vlanNetmask = vlan.getVlanNetmask();
                sameSubnet = hasSameSubnet(ipv4, vlanGateway, vlanNetmask, newVlanGateway, newVlanNetmask, startIP, endIP,
                        ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);
                if (sameSubnet) break;
            }
        } else if(network.getGateway() != null && network.getCidr() != null) {
            vlanGateway = network.getGateway();
            vlanNetmask = NetUtils.getCidrNetmask(network.getCidr());
            sameSubnet = hasSameSubnet(ipv4, vlanGateway, vlanNetmask, newVlanGateway, newVlanNetmask, startIP, endIP,
                    ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);
        }
        if (newVlanGateway == null && newVlanNetmask == null && !sameSubnet) {
            throw new InvalidParameterValueException("The ip range dose not belong to any of the existing subnets, Provide the netmask and gateway if you want to add new subnet");
        }
        Pair<String, String> vlanDetails = null;

        if (sameSubnet) {
            vlanDetails = new Pair<String, String>(vlanGateway, vlanNetmask);
        } else {
            vlanDetails = new Pair<String, String>(newVlanGateway, newVlanNetmask);
        }
        // check if the gatewayip is the part of the ip range being added.
        if (ipv4 && NetUtils.ipRangesOverlap(startIP, endIP, vlanDetails.first(), vlanDetails.first())) {
            throw new InvalidParameterValueException("The gateway ip should not be the part of the ip range being added.");
        }

        return new Pair<Boolean, Pair<String, String>>(sameSubnet, vlanDetails);
    }

    public boolean hasSameSubnet(boolean ipv4, String vlanGateway, String vlanNetmask, String newVlanGateway, String newVlanNetmask, String newStartIp, String newEndIp,
                                  boolean ipv6, String newIp6Gateway, String newIp6Cidr, String newIp6StartIp, String newIp6EndIp, Network network) {
        if (ipv4) {
            // check if subset or super set or neither.
            final NetUtils.SupersetOrSubset val = checkIfSubsetOrSuperset(vlanGateway, vlanNetmask, newVlanGateway, newVlanNetmask, newStartIp, newEndIp);
            if (val == NetUtils.SupersetOrSubset.isSuperset) {
                // this means that new cidr is a superset of the
                // existing subnet.
                throw new InvalidParameterValueException("The subnet you are trying to add is a superset of the existing subnet having gateway " + vlanGateway
                        + " and netmask " + vlanNetmask);
            } else if (val == NetUtils.SupersetOrSubset.neitherSubetNorSuperset) {
                // this implies the user is trying to add a new subnet
                // which is not a superset or subset of this subnet.
            } else if (val == NetUtils.SupersetOrSubset.isSubset) {
                // this means we are trying to add to the same subnet.
                throw new InvalidParameterValueException("The subnet you are trying to add is a subset of the existing subnet having gateway " + vlanGateway
                        + " and netmask " + vlanNetmask);
            } else if (val == NetUtils.SupersetOrSubset.sameSubnet) {
                //check if the gateway provided by the user is same as that of the subnet.
                if (newVlanGateway != null && !newVlanGateway.equals(vlanGateway)) {
                    throw new InvalidParameterValueException("The gateway of the subnet should be unique. The subnet already has a gateway " + vlanGateway);
                }
                return true;
            }
        }
        if (ipv6) {
            if (newIp6Gateway != null && !newIp6Gateway.equals(network.getIp6Gateway())) {
                throw new InvalidParameterValueException("The input gateway " + newIp6Gateway + " is not same as network gateway " + network.getIp6Gateway());
            }
            if (newIp6Cidr != null && !newIp6Cidr.equals(network.getIp6Cidr())) {
                throw new InvalidParameterValueException("The input cidr " + newIp6Cidr + " is not same as network cidr " + network.getIp6Cidr());
            }

            newIp6Gateway = MoreObjects.firstNonNull(newIp6Gateway, network.getIp6Gateway());
            newIp6Cidr = MoreObjects.firstNonNull(newIp6Cidr, network.getIp6Cidr());
            _networkModel.checkIp6Parameters(newIp6StartIp, newIp6EndIp, newIp6Gateway, newIp6Cidr);
            return true;
        }
        return false;
    }

    @Override
    @DB
    public Vlan createVlanAndPublicIpRange(final long zoneId, final long networkId, final long physicalNetworkId, final boolean forVirtualNetwork, final boolean forSystemVms, final Long podId, final String startIP, final String endIP,
            final String vlanGateway, final String vlanNetmask, String vlanId, boolean bypassVlanOverlapCheck, Domain domain, final Account vlanOwner, final String startIPv6, final String endIPv6, final String vlanIp6Gateway, final String vlanIp6Cidr) {
        final Network network = _networkModel.getNetwork(networkId);

        boolean ipv4 = false, ipv6 = false;

        if (startIP != null) {
            ipv4 = true;
        }

        if (vlanIp6Cidr != null) {
            ipv6 = true;
        }

        if (!ipv4 && !ipv6) {
            throw new InvalidParameterValueException("Please specify IPv4 or IPv6 address.");
        }

        // Validate the zone
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        // ACL check
        checkZoneAccess(CallContext.current().getCallingAccount(), zone);

        // Validate the physical network
        if (_physicalNetworkDao.findById(physicalNetworkId) == null) {
            throw new InvalidParameterValueException("Please specify a valid physical network id");
        }

        // Validate the pod
        if (podId != null) {
            final Pod pod = _podDao.findById(podId);
            if (pod == null) {
                throw new InvalidParameterValueException("Please specify a valid pod.");
            }
            if (pod.getDataCenterId() != zoneId) {
                throw new InvalidParameterValueException("Pod id=" + podId + " doesn't belong to zone id=" + zoneId);
            }
            // pod vlans can be created in basic zone only
            if (zone.getNetworkType() != NetworkType.Basic || network.getTrafficType() != TrafficType.Guest) {
                throw new InvalidParameterValueException("Pod id can be specified only for the networks of type " + TrafficType.Guest + " in zone of type " + NetworkType.Basic);
            }
        }


        // 1) if vlan is specified for the guest network range, it should be the
        // same as network's vlan
        // 2) if vlan is missing, default it to the guest network's vlan
        if (network.getTrafficType() == TrafficType.Guest) {
            String networkVlanId = null;
            boolean connectivityWithoutVlan = false;
            if (_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Connectivity)) {
                Map<Capability, String> connectivityCapabilities = _networkModel.getNetworkServiceCapabilities(network.getId(), Service.Connectivity);
                connectivityWithoutVlan = MapUtils.isNotEmpty(connectivityCapabilities) && connectivityCapabilities.containsKey(Capability.NoVlan);
            }

            final URI uri = network.getBroadcastUri();
            if (connectivityWithoutVlan) {
                networkVlanId = network.getBroadcastDomainType().toUri(network.getUuid()).toString();
            } else if (uri != null) {
                // Do not search for the VLAN tag when the network doesn't support VLAN
               if (uri.toString().startsWith("vlan")) {
                    final String[] vlan = uri.toString().split("vlan:\\/\\/");
                    networkVlanId = vlan[1];
                    // For pvlan
                    if (network.getBroadcastDomainType() != BroadcastDomainType.Vlan) {
                        networkVlanId = networkVlanId.split("-")[0];
                    }
               }
            }

            if (vlanId != null && !connectivityWithoutVlan) {
                // if vlan is specified, throw an error if it's not equal to
                // network's vlanId
                if (networkVlanId != null && !NetUtils.isSameIsolationId(networkVlanId, vlanId)) {
                    throw new InvalidParameterValueException("Vlan doesn't match vlan of the network");
                }
            } else {
                vlanId = networkVlanId;
            }
        } else if (network.getTrafficType() == TrafficType.Public && vlanId == null) {
            throw new InvalidParameterValueException("Unable to determine vlan id or untagged vlan for public network");
        }

        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        }

        final VlanType vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;

        if ((domain != null || vlanOwner != null) && zone.getNetworkType() != NetworkType.Advanced) {
            throw new InvalidParameterValueException("Vlan owner can be defined only in the zone of type " + NetworkType.Advanced);
        }

        if (ipv4) {
            // Make sure the gateway is valid
            if (!NetUtils.isValidIp4(vlanGateway)) {
                throw new InvalidParameterValueException("Please specify a valid gateway");
            }

            // Make sure the netmask is valid
            if (!NetUtils.isValidIp4Netmask(vlanNetmask)) {
                throw new InvalidParameterValueException("Please specify a valid netmask");
            }
        }

        if (ipv6) {
            if (!NetUtils.isValidIp6(vlanIp6Gateway)) {
                throw new InvalidParameterValueException("Please specify a valid IPv6 gateway");
            }
            if (!NetUtils.isValidIp6Cidr(vlanIp6Cidr)) {
                throw new InvalidParameterValueException("Please specify a valid IPv6 CIDR");
            }
        }

        boolean isSharedNetworkWithoutSpecifyVlan = _networkMgr.isSharedNetworkWithoutSpecifyVlan(_networkOfferingDao.findById(network.getNetworkOfferingId()));
        if (ipv4) {
            final String newCidr = NetUtils.getCidrFromGatewayAndNetmask(vlanGateway, vlanNetmask);

            //Make sure start and end ips are with in the range of cidr calculated for this gateway and netmask {
            if (!NetUtils.isIpWithInCidrRange(vlanGateway, newCidr) || !NetUtils.isIpWithInCidrRange(startIP, newCidr) || !NetUtils.isIpWithInCidrRange(endIP, newCidr)) {
                throw new InvalidParameterValueException("Please specify a valid IP range or valid netmask or valid gateway");
            }

            // Check if the new VLAN's subnet conflicts with the guest network
            // in
            // the specified zone (guestCidr is null for basic zone)
            // when adding shared network with same cidr of zone guest cidr,
            // if the specified vlan is not present in zone, physical network, allow to create the network as the isolation is based on VLAN.
            final String guestNetworkCidr = zone.getGuestNetworkCidr();
            if (guestNetworkCidr != null && NetUtils.isNetworksOverlap(newCidr, guestNetworkCidr) && _zoneDao.findVnet(zoneId, physicalNetworkId, vlanId).isEmpty() != true) {
                throw new InvalidParameterValueException("The new IP range you have specified has  overlapped with the guest network in zone: " + zone.getName()
                        + "along with existing Vlan also. Please specify a different gateway/netmask");
            }

            // Check if there are any errors with the IP range
            checkPublicIpRangeErrors(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

            checkConflictsWithPortableIpRange(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

            if (!isSharedNetworkWithoutSpecifyVlan) {
                checkZoneVlanIpOverlap(zone, network, newCidr, vlanId, vlanGateway, vlanNetmask, startIP, endIP);
            }
        }

        String ipv6Range = null;
        if (ipv6) {
            ipv6Range = startIPv6;
            if (StringUtils.isNotEmpty(ipv6Range) && StringUtils.isNotEmpty(endIPv6)) {
                ipv6Range += "-" + endIPv6;
            }

            final List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
            for (final VlanVO vlan : vlans) {
                if (vlan.getIp6Gateway() == null) {
                    continue;
                }
                if ((StringUtils.isAllEmpty(ipv6Range, vlan.getIp6Range())) &&
                        NetUtils.ipv6NetworksOverlap(IPv6Network.fromString(vlanIp6Cidr), IPv6Network.fromString(vlan.getIp6Cidr()))) {
                    throw new InvalidParameterValueException(String.format("The IPv6 range with tag: %s already has IPs that overlap with the new range.",
                            vlan.getVlanTag()));
                }
                if (!StringUtils.isAllEmpty(ipv6Range, vlan.getIp6Range())) {
                    String r1 = StringUtils.isEmpty(ipv6Range) ? NetUtils.getIpv6RangeFromCidr(vlanIp6Cidr) : ipv6Range;
                    String r2 = StringUtils.isEmpty(vlan.getIp6Range()) ? NetUtils.getIpv6RangeFromCidr(vlan.getIp6Cidr()) : vlan.getIp6Range();
                    if(NetUtils.isIp6RangeOverlap(r1, r2)) {
                        throw new InvalidParameterValueException(String.format("The IPv6 range with tag: %s already has IPs that overlap with the new range.",
                                vlan.getVlanTag()));
                    }
                }
                if (NetUtils.isSameIsolationId(vlanId, vlan.getVlanTag()) && !vlanIp6Gateway.equals(vlan.getIp6Gateway())) {
                    throw new InvalidParameterValueException(String.format("The IP range with tag: %s has already been added with gateway %s. Please specify a different tag.",
                            vlan.getVlanTag(), vlan.getIp6Gateway()));
                }
            }
        }

        // Check if the vlan is being used
        if (isSharedNetworkWithoutSpecifyVlan) {
            bypassVlanOverlapCheck = true;
        }
        if (!bypassVlanOverlapCheck && _zoneDao.findVnet(zoneId, physicalNetworkId, BroadcastDomainType.getValue(BroadcastDomainType.fromString(vlanId))).size() > 0) {
            throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for dynamic vlan allocation for the guest network in zone "
                    + zone.getName());
        }

        String ipRange = null;

        if (ipv4) {
            ipRange = startIP;
            if (endIP != null) {
                ipRange += "-" + endIP;
            }
        }

        // Everything was fine, so persist the VLAN
        final VlanVO vlan = commitVlanAndIpRange(zoneId, networkId, physicalNetworkId, podId, startIP, endIP, vlanGateway, vlanNetmask, vlanId, domain, vlanOwner, vlanIp6Gateway, vlanIp6Cidr,
                ipv4, zone, vlanType, ipv6Range, ipRange, forSystemVms);

        return vlan;
    }

    private void checkZoneVlanIpOverlap(DataCenterVO zone, Network network, String newCidr, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) {
        // Throw an exception if this subnet overlaps with subnet on other VLAN,
        // if this is ip range extension, gateway, network mask should be same and ip range should not overlap

        final List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
        for (final VlanVO vlan : vlans) {
            final String otherVlanGateway = vlan.getVlanGateway();
            final String otherVlanNetmask = vlan.getVlanNetmask();
            // Continue if it's not IPv4
            if (ObjectUtils.anyNull(otherVlanGateway, otherVlanNetmask, vlan.getNetworkId())) {
                continue;
            }
            final String otherCidr = NetUtils.getCidrFromGatewayAndNetmask(otherVlanGateway, otherVlanNetmask);
            if( !NetUtils.isNetworksOverlap(newCidr,  otherCidr)) {
                continue;
            }
            // from here, subnet overlaps
            if (vlanId.toLowerCase().contains(Vlan.UNTAGGED) || UriUtils.checkVlanUriOverlap(
                    BroadcastDomainType.getValue(BroadcastDomainType.fromString(vlanId)),
                    BroadcastDomainType.getValue(BroadcastDomainType.fromString(vlan.getVlanTag())))) {
                // For untagged VLAN Id and overlapping URIs we need to expand and verify IP ranges
                final String[] otherVlanIpRange = vlan.getIpRange().split("\\-");
                final String otherVlanStartIP = otherVlanIpRange[0];
                String otherVlanEndIP = null;
                if (otherVlanIpRange.length > 1) {
                    otherVlanEndIP = otherVlanIpRange[1];
                }

                // extend IP range
                if (!vlanGateway.equals(otherVlanGateway) || !vlanNetmask.equals(vlan.getVlanNetmask())) {
                    throw new InvalidParameterValueException("The IP range has already been added with gateway "
                            + otherVlanGateway + " ,and netmask " + otherVlanNetmask
                            + ", Please specify the gateway/netmask if you want to extend ip range" );
                }
                if (!NetUtils.is31PrefixCidr(newCidr) && NetUtils.ipRangesOverlap(startIP, endIP, otherVlanStartIP, otherVlanEndIP)) {
                    throw new InvalidParameterValueException("The IP range already has IPs that overlap with the new range." +
                            " Please specify a different start IP/end IP.");
                }
            } else {
                // For tagged or non-overlapping URIs we need to ensure there is no Public traffic type
                boolean overlapped = false;
                if (network.getTrafficType() == TrafficType.Public) {
                    overlapped = true;
                } else {
                    final Long nwId = vlan.getNetworkId();
                    if (nwId != null) {
                        final Network nw = _networkModel.getNetwork(nwId);
                        if (nw != null && nw.getTrafficType() == TrafficType.Public) {
                            overlapped = true;
                        }
                    }

                }
                if (overlapped) {
                    throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag()
                            + " in zone " + zone.getName()
                            + " has overlapped with the subnet. Please specify a different gateway/netmask.");
                }
            }
        }
    }

    private VlanVO commitVlanAndIpRange(final long zoneId, final long networkId, final long physicalNetworkId, final Long podId, final String startIP, final String endIP,
            final String vlanGateway, final String vlanNetmask, final String vlanId, final Domain domain, final Account vlanOwner, final String vlanIp6Gateway, final String vlanIp6Cidr,
            final boolean ipv4, final DataCenterVO zone, final VlanType vlanType, final String ipv6Range, final String ipRange, final boolean forSystemVms) {
        return Transaction.execute(new TransactionCallback<VlanVO>() {
            @Override
            public VlanVO doInTransaction(final TransactionStatus status) {
                VlanVO vlan = new VlanVO(vlanType, vlanId, vlanGateway, vlanNetmask, zone.getId(), ipRange, networkId, physicalNetworkId, vlanIp6Gateway, vlanIp6Cidr, ipv6Range);
                s_logger.debug("Saving vlan range " + vlan);
                vlan = _vlanDao.persist(vlan);

                // IPv6 use a used ip map, is different from ipv4, no need to save
                // public ip range
                if (ipv4) {
                    if (!savePublicIPRange(startIP, endIP, zoneId, vlan.getId(), networkId, physicalNetworkId, forSystemVms)) {
                        throw new CloudRuntimeException("Failed to save IPv4 range. Please contact Cloud Support.");
                    }
                }

                if (vlanOwner != null) {
                    // This VLAN is account-specific, so create an AccountVlanMapVO
                    // entry
                    final AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(vlanOwner.getId(), vlan.getId());
                    _accountVlanMapDao.persist(accountVlanMapVO);

                    // generate usage event for dedication of every ip address in the
                    // range
                    final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlan.getId());
                    for (final IPAddressVO ip : ips) {
                        final boolean usageHidden = _ipAddrMgr.isUsageHidden(ip);
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, vlanOwner.getId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(),
                                ip.isSourceNat(), vlan.getVlanType().toString(), ip.getSystem(), usageHidden, ip.getClass().getName(), ip.getUuid());
                    }
                    // increment resource count for dedicated public ip's
                    _resourceLimitMgr.incrementResourceCount(vlanOwner.getId(), ResourceType.public_ip, new Long(ips.size()));
                } else if (domain != null && !forSystemVms) {
                    // This VLAN is domain-wide, so create a DomainVlanMapVO entry
                    final DomainVlanMapVO domainVlanMapVO = new DomainVlanMapVO(domain.getId(), vlan.getId());
                    _domainVlanMapDao.persist(domainVlanMapVO);
                } else if (podId != null) {
                    // This VLAN is pod-wide, so create a PodVlanMapVO entry
                    final PodVlanMapVO podVlanMapVO = new PodVlanMapVO(podId, vlan.getId());
                    podVlanMapDao.persist(podVlanMapVO);
                }
                return vlan;
            }
        });

    }

    @Override
    public Vlan updateVlanAndPublicIpRange(UpdateVlanIpRangeCmd cmd) throws ConcurrentOperationException,
            ResourceUnavailableException,ResourceAllocationException {

        return  updateVlanAndPublicIpRange(cmd.getId(), cmd.getStartIp(),cmd.getEndIp(), cmd.getGateway(),cmd.getNetmask(),
                cmd.getStartIpv6(), cmd.getEndIpv6(), cmd.getIp6Gateway(), cmd.getIp6Cidr(), cmd.isForSystemVms());
    }

    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_UPDATE, eventDescription = "update vlan ip Range", async
            = false)
    public Vlan updateVlanAndPublicIpRange(final long id, String startIp,
                                           String endIp,
                                           String gateway,
                                           String netmask,
                                           String startIpv6,
                                           String endIpv6,
                                           String ip6Gateway,
                                           String ip6Cidr,
                                           Boolean forSystemVms) throws ConcurrentOperationException {

        VlanVO vlanRange = _vlanDao.findById(id);
        if (vlanRange == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        final boolean ipv4 = vlanRange.getVlanGateway() != null;
        final boolean ipv6 = vlanRange.getIp6Gateway() != null;
        if (!ipv4) {
            if (startIp != null || endIp != null || gateway != null || netmask != null) {
                throw new InvalidParameterValueException("IPv4 is not support in this IP range.");
            }
        }
        if (!ipv6) {
            if (startIpv6 != null || endIpv6 != null || ip6Gateway != null || ip6Cidr != null) {
                throw new InvalidParameterValueException("IPv6 is not support in this IP range.");
            }
        }

        final Boolean isRangeForSystemVM = checkIfVlanRangeIsForSystemVM(id);
        if (forSystemVms != null && isRangeForSystemVM != forSystemVms) {
            if (VlanType.DirectAttached.equals(vlanRange.getVlanType())) {
                throw new InvalidParameterValueException("forSystemVms is not available for this IP range with vlan type: " + VlanType.DirectAttached);
            }
            // Check if range has already been dedicated
            final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(id);
            if (maps != null && !maps.isEmpty()) {
                throw new InvalidParameterValueException("Specified Public IP range has already been dedicated to an account");
            }

            List<DomainVlanMapVO> domainmaps = _domainVlanMapDao.listDomainVlanMapsByVlan(id);
            if (domainmaps != null && !domainmaps.isEmpty()) {
                throw new InvalidParameterValueException("Specified Public IP range has already been dedicated to a domain");
            }
        }
        if (ipv4) {
            updateVlanAndIpv4Range(id, vlanRange, startIp, endIp, gateway, netmask, isRangeForSystemVM, forSystemVms);
        }
        if (ipv6) {
            updateVlanAndIpv6Range(id, vlanRange, startIpv6, endIpv6, ip6Gateway, ip6Cidr, isRangeForSystemVM, forSystemVms);
        }
        return _vlanDao.findById(id);
    }

    private void updateVlanAndIpv4Range(final long id, final VlanVO vlanRange,
                                        String startIp,
                                        String endIp,
                                        String gateway,
                                        String netmask,
                                        final Boolean isRangeForSystemVM,
                                        final Boolean forSystemVms) {
        final List<IPAddressVO> listAllocatedIPs = _publicIpAddressDao.listByVlanIdAndState(id, IpAddress.State.Allocated);

        if (gateway != null && !gateway.equals(vlanRange.getVlanGateway()) && CollectionUtils.isNotEmpty(listAllocatedIPs)) {
            throw new InvalidParameterValueException(String.format("Unable to change gateway to %s because some IPs are in use", gateway));
        }
        if (netmask != null && !netmask.equals(vlanRange.getVlanNetmask()) && CollectionUtils.isNotEmpty(listAllocatedIPs)) {
            throw new InvalidParameterValueException(String.format("Unable to change netmask to %s because some IPs are in use", netmask));
        }

        gateway = MoreObjects.firstNonNull(gateway, vlanRange.getVlanGateway());
        netmask = MoreObjects.firstNonNull(netmask, vlanRange.getVlanNetmask());

        final String[] existingVlanIPRangeArray = vlanRange.getIpRange().split("-");
        final String currentStartIP = existingVlanIPRangeArray[0];
        final String currentEndIP = existingVlanIPRangeArray[1];

        startIp = MoreObjects.firstNonNull(startIp, currentStartIP);
        endIp = MoreObjects.firstNonNull(endIp, currentEndIP);

        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        if (StringUtils.isEmpty(cidr)) {
            throw new InvalidParameterValueException(String.format("Invalid gateway (%s) or netmask (%s)", gateway, netmask));
        }
        final String cidrAddress = getCidrAddress(cidr);
        final long cidrSize = getCidrSize(cidr);

        checkIpRange(startIp, endIp, cidrAddress, cidrSize);

        checkGatewayOverlap(startIp, endIp, gateway);

        checkAllocatedIpsAreWithinVlanRange(listAllocatedIPs, startIp, endIp, forSystemVms);

        try {
            final String newStartIP = startIp;
            final String newEndIP = endIp;

            VlanVO range = _vlanDao.acquireInLockTable(id, 30);
            if (range == null) {
                throw new CloudRuntimeException("Unable to acquire vlan configuration: " + id);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("lock vlan " + id + " is acquired");
            }

            commitUpdateVlanAndIpRange(id, newStartIP, newEndIP, currentStartIP, currentEndIP, gateway, netmask,true, isRangeForSystemVM, forSystemVms);

        } catch (final Exception e) {
            s_logger.error("Unable to edit VlanRange due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to edit VlanRange. Please contact Cloud Support.");
        } finally {
            _vlanDao.releaseFromLockTable(id);
        }
    }

    private void updateVlanAndIpv6Range(final long id, final VlanVO vlanRange,
                                        String startIpv6,
                                        String endIpv6,
                                        String ip6Gateway,
                                        String ip6Cidr,
                                        final Boolean isRangeForSystemVM,
                                        final Boolean forSystemVms) {
        final List<UserIpv6AddressVO> listAllocatedIPs = _ipv6Dao.listByVlanIdAndState(id, IpAddress.State.Allocated);

        if (ip6Gateway != null && !ip6Gateway.equals(vlanRange.getIp6Gateway()) && (CollectionUtils.isNotEmpty(listAllocatedIPs) || CollectionUtils.isNotEmpty(ipv6Service.getAllocatedIpv6FromVlanRange(vlanRange)))) {
            throw new InvalidParameterValueException(String.format("Unable to change ipv6 gateway to %s because some IPs are in use", ip6Gateway));
        }
        if (ip6Cidr != null && !ip6Cidr.equals(vlanRange.getIp6Cidr()) && (CollectionUtils.isNotEmpty(listAllocatedIPs) || CollectionUtils.isNotEmpty(ipv6Service.getAllocatedIpv6FromVlanRange(vlanRange)))) {
            throw new InvalidParameterValueException(String.format("Unable to change ipv6 cidr to %s because some IPs are in use", ip6Cidr));
        }
        ip6Gateway = MoreObjects.firstNonNull(ip6Gateway, vlanRange.getIp6Gateway());
        ip6Cidr = MoreObjects.firstNonNull(ip6Cidr, vlanRange.getIp6Cidr());

        final String[] existingVlanIPRangeArray = StringUtils.isNotEmpty(vlanRange.getIp6Range()) ? vlanRange.getIp6Range().split("-") : null;
        final String currentStartIPv6 = existingVlanIPRangeArray != null ? existingVlanIPRangeArray[0] : null;
        final String currentEndIPv6 = existingVlanIPRangeArray != null ? existingVlanIPRangeArray[1] : null;

        startIpv6 = ObjectUtils.allNull(startIpv6, currentStartIPv6) ? null : MoreObjects.firstNonNull(startIpv6, currentStartIPv6);
        endIpv6 = ObjectUtils.allNull(endIpv6, currentEndIPv6) ? null : MoreObjects.firstNonNull(endIpv6, currentEndIPv6);

        _networkModel.checkIp6Parameters(startIpv6, endIpv6, ip6Gateway, ip6Cidr);

        if (!ObjectUtils.allNull(startIpv6, endIpv6) && ObjectUtils.anyNull(startIpv6, endIpv6)) {
            throw new InvalidParameterValueException(String.format("Invalid IPv6 range %s-%s", startIpv6, endIpv6));
        }
        if (ObjectUtils.allNotNull(startIpv6, endIpv6) && (!startIpv6.equals(currentStartIPv6) || !endIpv6.equals(currentEndIPv6))) {
            checkAllocatedIpv6sAreWithinVlanRange(listAllocatedIPs, startIpv6, endIpv6);
        }

        try {
            VlanVO range = _vlanDao.acquireInLockTable(id, 30);
            if (range == null) {
                throw new CloudRuntimeException("Unable to acquire vlan configuration: " + id);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("lock vlan " + id + " is acquired");
            }

            commitUpdateVlanAndIpRange(id, startIpv6, endIpv6, currentStartIPv6, currentEndIPv6, ip6Gateway, ip6Cidr, false, isRangeForSystemVM,forSystemVms);

        } catch (final Exception e) {
            s_logger.error("Unable to edit VlanRange due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to edit VlanRange. Please contact Cloud Support.");
        } finally {
            _vlanDao.releaseFromLockTable(id);
        }
    }

    private VlanVO commitUpdateVlanAndIpRange(final Long id, final String newStartIP, final String newEndIP, final String currentStartIP, final String currentEndIP,
                                              final String gateway, final String netmask,
                                              final boolean ipv4, final Boolean isRangeForSystemVM, final Boolean forSystemvms) {

        return Transaction.execute(new TransactionCallback<VlanVO>() {
            @Override
            public VlanVO doInTransaction(final TransactionStatus status) {
                VlanVO vlanRange = _vlanDao.findById(id);
                s_logger.debug("Updating vlan range " + vlanRange.getId());
                if (ipv4) {
                    vlanRange.setIpRange(newStartIP + "-" + newEndIP);
                    vlanRange.setVlanGateway(gateway);
                    vlanRange.setVlanNetmask(netmask);
                    _vlanDao.update(vlanRange.getId(), vlanRange);
                    if (!updatePublicIPRange(newStartIP, currentStartIP, newEndIP, currentEndIP, vlanRange.getDataCenterId(), vlanRange.getId(), vlanRange.getNetworkId(), vlanRange.getPhysicalNetworkId(), isRangeForSystemVM, forSystemvms)) {
                        throw new CloudRuntimeException("Failed to update IPv4 range. Please contact Cloud Support.");
                    }
                } else {
                    if (ObjectUtils.allNotNull(newStartIP, newEndIP)) {
                        vlanRange.setIp6Range(newStartIP + "-" + newEndIP);
                    } else {
                        vlanRange.setIp6Range(null);
                    }
                    vlanRange.setIp6Gateway(gateway);
                    vlanRange.setIp6Cidr(netmask);
                    _vlanDao.update(vlanRange.getId(), vlanRange);
                }
                return vlanRange;
            }
        });
    }

    private boolean checkIfVlanRangeIsForSystemVM(final long vlanId) {
        List<IPAddressVO> existingPublicIPs = _publicIpAddressDao.listByVlanId(vlanId);
        if (CollectionUtils.isEmpty(existingPublicIPs)) {
            return false;
        }
        boolean initialIsSystemVmValue = existingPublicIPs.get(0).isForSystemVms();
        for (IPAddressVO existingIPs : existingPublicIPs) {
            if (initialIsSystemVmValue != existingIPs.isForSystemVms()) {
                throw new CloudRuntimeException("Your \"For System VM\" value seems to be inconsistent with the rest of the records. Please contact Cloud Support");
            }
        }
        return initialIsSystemVmValue;
    }

    private void checkAllocatedIpsAreWithinVlanRange
            (List<IPAddressVO> listAllocatedIPs, String startIp, String endIp, Boolean forSystemVms) {
        Collections.sort(listAllocatedIPs, Comparator.comparing(IPAddressVO::getAddress));
        for (IPAddressVO allocatedIP : listAllocatedIPs) {
            if ((StringUtils.isNotEmpty(startIp) && NetUtils.ip2Long(startIp) > NetUtils.ip2Long(allocatedIP.getAddress().addr()))
                    || (StringUtils.isNotEmpty(endIp) && NetUtils.ip2Long(endIp) < NetUtils.ip2Long(allocatedIP.getAddress().addr()))) {
                throw new InvalidParameterValueException(String.format("The start IP address must be less than or equal to %s which is already in use. "
                                + "The end IP address must be greater than or equal to %s which is already in use. "
                                + "There are %d IPs already allocated in this range.",
                        listAllocatedIPs.get(0).getAddress(), listAllocatedIPs.get(listAllocatedIPs.size() - 1).getAddress(), listAllocatedIPs.size()));
            }
            if (forSystemVms != null && allocatedIP.isForSystemVms() != forSystemVms) {
                throw new InvalidParameterValueException(String.format("IP %s is in use, cannot change forSystemVms of the IP range", allocatedIP.getAddress().addr()));
            }
        }
    }

    private void checkAllocatedIpv6sAreWithinVlanRange(List<UserIpv6AddressVO> listAllocatedIPs, String startIpv6, String endIpv6) {
        Collections.sort(listAllocatedIPs, Comparator.comparing(UserIpv6AddressVO::getAddress));
        for (UserIpv6AddressVO allocatedIP : listAllocatedIPs) {
            if ((StringUtils.isNotEmpty(startIpv6)
                    && IPv6Address.fromString(startIpv6).toBigInteger().compareTo(IPv6Address.fromString(allocatedIP.getAddress()).toBigInteger()) > 0)
                    || (StringUtils.isNotEmpty(endIpv6)
                    && IPv6Address.fromString(endIpv6).toBigInteger().compareTo(IPv6Address.fromString(allocatedIP.getAddress()).toBigInteger()) < 0)) {
                throw new InvalidParameterValueException(String.format("The start IPv6 address must be less than or equal to %s which is already in use. "
                                + "The end IPv6 address must be greater than or equal to %s which is already in use. "
                                + "There are %d IPv6 addresses already allocated in this range.",
                        listAllocatedIPs.get(0).getAddress(), listAllocatedIPs.get(listAllocatedIPs.size() - 1).getAddress(), listAllocatedIPs.size()));
            }
        }
    }

    private void checkGatewayOverlap(String startIp, String endIp, String gateway) {
        if (NetUtils.ipRangesOverlap(startIp, endIp, gateway, gateway)) {
            throw new InvalidParameterValueException("The gateway shouldn't overlap the new start/end ip "
                    + "addresses");
        }
    }

    @Override
    @DB
    public boolean deleteVlanAndPublicIpRange(final long userId, final long vlanDbId, final Account caller) {
        VlanVO vlanRange = _vlanDao.findById(vlanDbId);
        if (vlanRange == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        boolean isAccountSpecific = false;
        final List<AccountVlanMapVO> acctVln = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanRange.getId());
        // Check for account wide pool. It will have an entry for
        // account_vlan_map.
        if (acctVln != null && !acctVln.isEmpty()) {
            isAccountSpecific = true;
        }

        boolean isDomainSpecific = false;
        List<DomainVlanMapVO> domainVlan = _domainVlanMapDao.listDomainVlanMapsByVlan(vlanRange.getId());
        // Check for domain wide pool. It will have an entry for domain_vlan_map.
        if (domainVlan != null && !domainVlan.isEmpty()) {
            isDomainSpecific = true;
        }

        // Check if the VLAN has any allocated public IPs
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        if (isAccountSpecific) {
            int resourceCountToBeDecrement = 0;
            try {
                vlanRange = _vlanDao.acquireInLockTable(vlanDbId, 30);
                if (vlanRange == null) {
                    throw new CloudRuntimeException("Unable to acquire vlan configuration: " + vlanDbId);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("lock vlan " + vlanDbId + " is acquired");
                }
                for (final IPAddressVO ip : ips) {
                    boolean success = true;
                    if (ip.isOneToOneNat()) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range is used for static nat purposes. Cleanup the rules first");
                    }

                    if (ip.isSourceNat()) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range is a source nat ip for the network id=" + ip.getSourceNetworkId()
                                + ". IP range with the source nat ip address can be removed either as a part of Network, or account removal");
                    }

                    if (_firewallDao.countRulesByIpId(ip.getId()) > 0) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range has firewall rules applied. Cleanup the rules first");
                    }
                    if (ip.getAllocatedTime() != null) {
                        // This means IP is allocated
                        // release public ip address here
                        success = _ipAddrMgr.disassociatePublicIpAddress(ip.getId(), userId, caller);
                    }
                    if (!success) {
                        s_logger.warn("Some ip addresses failed to be released as a part of vlan " + vlanDbId + " removal");
                    } else {
                        resourceCountToBeDecrement++;
                        final boolean usageHidden = _ipAddrMgr.isUsageHidden(ip);
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_RELEASE, acctVln.get(0).getAccountId(), ip.getDataCenterId(), ip.getId(),
                                ip.getAddress().toString(), ip.isSourceNat(), vlanRange.getVlanType().toString(), ip.getSystem(), usageHidden, ip.getClass().getName(), ip.getUuid());
                    }
                }
            } finally {
                _vlanDao.releaseFromLockTable(vlanDbId);
                if (resourceCountToBeDecrement > 0) {  //Making sure to decrement the count of only success operations above. For any reaason if disassociation fails then this number will vary from original range length.
                    _resourceLimitMgr.decrementResourceCount(acctVln.get(0).getAccountId(), ResourceType.public_ip, new Long(resourceCountToBeDecrement));
                }
            }
        } else {   // !isAccountSpecific
            final NicIpAliasVO ipAlias = _nicIpAliasDao.findByGatewayAndNetworkIdAndState(vlanRange.getVlanGateway(), vlanRange.getNetworkId(), NicIpAlias.State.active);
            //check if the ipalias belongs to the vlan range being deleted.
            if (ipAlias != null && vlanDbId == _publicIpAddressDao.findByIpAndSourceNetworkId(vlanRange.getNetworkId(), ipAlias.getIp4Address()).getVlanId()) {
                throw new InvalidParameterValueException("Cannot delete vlan range " + vlanDbId + " as " + ipAlias.getIp4Address()
                        + "is being used for providing dhcp service in this subnet. Delete all VMs in this subnet and try again");
            }
            final long allocIpCount = _publicIpAddressDao.countIPs(vlanRange.getDataCenterId(), vlanDbId, true);
            if (allocIpCount > 0) {
                throw new InvalidParameterValueException(allocIpCount + "  Ips are in use. Cannot delete this vlan");
            }
        }
        List<String> ipAddresses = ipv6Service.getAllocatedIpv6FromVlanRange(vlanRange);
        if (CollectionUtils.isNotEmpty(ipAddresses)) {
            throw new InvalidParameterValueException(String.format("%d IPv6 addresses are in use. Cannot delete this vlan", ipAddresses.size()));
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                _publicIpAddressDao.deletePublicIPRange(vlanDbId);
                s_logger.debug(String.format("Delete Public IP Range (from user_ip_address, where vlan_db_id=%s)", vlanDbId));

                _vlanDao.remove(vlanDbId);
                s_logger.debug(String.format("Mark vlan as Remove vlan (vlan_db_id=%s)", vlanDbId));

                SearchBuilder<PodVlanMapVO> sb = podVlanMapDao.createSearchBuilder();
                sb.and("vlan_db_id", sb.entity().getVlanDbId(), SearchCriteria.Op.EQ);
                SearchCriteria<PodVlanMapVO> sc = sb.create();
                sc.setParameters("vlan_db_id", vlanDbId);
                podVlanMapDao.remove(sc);
                s_logger.debug(String.format("Delete vlan_db_id=%s in pod_vlan_map", vlanDbId));
            }
        });

        messageBus.publish(_name, MESSAGE_DELETE_VLAN_IP_RANGE_EVENT, PublishScope.LOCAL, vlanRange);

        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_DEDICATE, eventDescription = "dedicating vlan ip range", async = false)
    public Vlan dedicatePublicIpRange(final DedicatePublicIpRangeCmd cmd) throws ResourceAllocationException {
        final Long vlanDbId = cmd.getId();
        final String accountName = cmd.getAccountName();
        final Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();

        // Check if account is valid
        Account vlanOwner = null;
        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("accountName and projectId are mutually exclusive");
            }
            final Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            vlanOwner = _accountMgr.getAccount(project.getProjectAccountId());
            if (vlanOwner == null) {
                throw new InvalidParameterValueException("Please specify a valid projectId");
            }
        }

        Domain domain = null;
        if (accountName != null && domainId != null) {
            vlanOwner = _accountDao.findActiveAccount(accountName, domainId);
            if (vlanOwner == null) {
                throw new InvalidParameterValueException("Unable to find account by name " + accountName);
            } else if (vlanOwner.getId() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Please specify a valid account. Cannot dedicate IP range to system account");
            }
        } else if (domainId != null) {
            domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Please specify a valid domain id");
            }
        }

        // Check if range is valid
        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Unable to find vlan by id " + vlanDbId);
        }

        // Check if range has already been dedicated
        final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        if (maps != null && !maps.isEmpty()) {
            throw new InvalidParameterValueException("Specified Public IP range has already been dedicated");
        }

        List<DomainVlanMapVO> domainmaps = _domainVlanMapDao.listDomainVlanMapsByVlan(vlanDbId);
        if (domainmaps != null && !domainmaps.isEmpty()) {
            throw new InvalidParameterValueException("Specified Public IP range has already been dedicated to a domain");
        }

        // Verify that zone exists and is advanced
        final Long zoneId = vlan.getDataCenterId();
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }
        if (zone.getNetworkType() == NetworkType.Basic) {
            throw new InvalidParameterValueException("Public IP range can be dedicated to an account only in the zone of type " + NetworkType.Advanced);
        }

        // Check Public IP resource limits
        if (vlanOwner != null) {
            final int accountPublicIpRange = _publicIpAddressDao.countIPs(zoneId, vlanDbId, false);
            _resourceLimitMgr.checkResourceLimit(vlanOwner, ResourceType.public_ip, accountPublicIpRange);
        }

        // Check if any of the Public IP addresses is allocated to another
        // account
        boolean forSystemVms = false;
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        for (final IPAddressVO ip : ips) {
            forSystemVms = ip.isForSystemVms();
            final Long allocatedToAccountId = ip.getAllocatedToAccountId();
            if (allocatedToAccountId != null) {
                if (vlanOwner != null && allocatedToAccountId != vlanOwner.getId()) {
                    throw new InvalidParameterValueException(ip.getAddress() + " Public IP address in range is allocated to another account ");
                }
                final Account accountAllocatedTo = _accountMgr.getActiveAccountById(allocatedToAccountId);
                if (vlanOwner == null && domain != null && domain.getId() != accountAllocatedTo.getDomainId()){
                    throw new InvalidParameterValueException(ip.getAddress()
                            + " Public IP address in range is allocated to another domain/account ");
                }
            }
        }

        if (vlanOwner != null) {
            // Create an AccountVlanMapVO entry
            final AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(vlanOwner.getId(), vlan.getId());
            _accountVlanMapDao.persist(accountVlanMapVO);

           // generate usage event for dedication of every ip address in the range
            for (final IPAddressVO ip : ips) {
                final boolean usageHidden = _ipAddrMgr.isUsageHidden(ip);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, vlanOwner.getId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(), ip.isSourceNat(),
                        vlan.getVlanType().toString(), ip.getSystem(), usageHidden, ip.getClass().getName(), ip.getUuid());
            }
        } else if (domain != null && !forSystemVms) {
            // Create an DomainVlanMapVO entry
            DomainVlanMapVO domainVlanMapVO = new DomainVlanMapVO(domain.getId(), vlan.getId());
            _domainVlanMapDao.persist(domainVlanMapVO);
        }

        // increment resource count for dedicated public ip's
        if (vlanOwner != null) {
            _resourceLimitMgr.incrementResourceCount(vlanOwner.getId(), ResourceType.public_ip, new Long(ips.size()));
        }

        return vlan;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_RELEASE, eventDescription = "releasing a public ip range", async = false)
    public boolean releasePublicIpRange(final ReleasePublicIpRangeCmd cmd) {
        final Long vlanDbId = cmd.getId();

        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        return releasePublicIpRange(vlanDbId, CallContext.current().getCallingUserId(), CallContext.current().getCallingAccount());
    }

    @DB
    public boolean releasePublicIpRange(final long vlanDbId, final long userId, final Account caller) {
        VlanVO vlan = _vlanDao.findById(vlanDbId);
        if(vlan == null) {
            // Nothing to do if vlan can't be found
            s_logger.warn(String.format("Skipping the process for releasing public IP range as could not find a VLAN with ID '%s' for Account '%s' and User '%s'."
                    ,vlanDbId, caller, userId));
            return true;
        }

        // Verify range is dedicated
        boolean isAccountSpecific = false;
        final List<AccountVlanMapVO> acctVln = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        // Verify range is dedicated
        if (acctVln != null && !acctVln.isEmpty()) {
            isAccountSpecific = true;
        }

        boolean isDomainSpecific = false;
        final List<DomainVlanMapVO> domainVlan = _domainVlanMapDao.listDomainVlanMapsByVlan(vlanDbId);
        // Check for domain wide pool. It will have an entry for domain_vlan_map.
        if (domainVlan != null && !domainVlan.isEmpty()) {
            isDomainSpecific = true;
        }

        if (!isAccountSpecific && !isDomainSpecific) {
            throw new InvalidParameterValueException("Can't release Public IP range " + vlanDbId
                    + " as it not dedicated to any domain and any account");
        }
        // Check if range has any allocated public IPs
        final long allocIpCount = _publicIpAddressDao.countIPs(vlan.getDataCenterId(), vlanDbId, true);
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        boolean success = true;
        final List<IPAddressVO> ipsInUse = new ArrayList<IPAddressVO>();
        if (allocIpCount > 0) {
            try {
                vlan = _vlanDao.acquireInLockTable(vlanDbId, 30);
                if (vlan == null) {
                    throw new CloudRuntimeException("Unable to acquire vlan configuration: " + vlanDbId);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("lock vlan " + vlanDbId + " is acquired");
                }
                for (final IPAddressVO ip : ips) {
                    // Disassociate allocated IP's that are not in use
                    if (!ip.isOneToOneNat() && !ip.isSourceNat() && !(_firewallDao.countRulesByIpId(ip.getId()) > 0)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Releasing Public IP addresses" + ip + " of vlan " + vlanDbId + " as part of Public IP" + " range release to the system pool");
                        }
                        success = success && _ipAddrMgr.disassociatePublicIpAddress(ip.getId(), userId, caller);
                    } else {
                        ipsInUse.add(ip);
                    }
                }
                if (!success) {
                    s_logger.warn("Some Public IP addresses that were not in use failed to be released as a part of" + " vlan " + vlanDbId + "release to the system pool");
                }
            } finally {
                _vlanDao.releaseFromLockTable(vlanDbId);
            }
        }

        // A Public IP range can only be dedicated to one account at a time
        if (isAccountSpecific && _accountVlanMapDao.remove(acctVln.get(0).getId())) {
            // generate usage events to remove dedication for every ip in the range that has been disassociated
            for (final IPAddressVO ip : ips) {
                if (!ipsInUse.contains(ip)) {
                    final boolean usageHidden = _ipAddrMgr.isUsageHidden(ip);
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_RELEASE, acctVln.get(0).getAccountId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(),
                            ip.isSourceNat(), vlan.getVlanType().toString(), ip.getSystem(), usageHidden, ip.getClass().getName(), ip.getUuid());
                }
            }
            // decrement resource count for dedicated public ip's
            _resourceLimitMgr.decrementResourceCount(acctVln.get(0).getAccountId(), ResourceType.public_ip, new Long(ips.size()));
            success = true;
        } else if (isDomainSpecific && _domainVlanMapDao.remove(domainVlan.get(0).getId())) {
            s_logger.debug("Remove the vlan from domain_vlan_map successfully.");
            success = true;
        } else {
            success = false;
        }

        return success;
    }

    @DB
    protected boolean savePublicIPRange(final String startIP, final String endIP, final long zoneId, final long vlanDbId, final long sourceNetworkid, final long physicalNetworkId, final boolean forSystemVms) {
        final long startIPLong = NetUtils.ip2Long(startIP);
        final long endIPLong = NetUtils.ip2Long(endIP);

        final List<String> problemIps = Transaction.execute(new TransactionCallback<List<String>>() {
            @Override
            public List<String> doInTransaction(final TransactionStatus status) {
                final IPRangeConfig config = new IPRangeConfig();
                return config.savePublicIPRange(TransactionLegacy.currentTxn(), startIPLong, endIPLong, zoneId, vlanDbId, sourceNetworkid, physicalNetworkId, forSystemVms);
            }
        });

        return CollectionUtils.isEmpty(problemIps);
    }

    @DB
    protected boolean updatePublicIPRange(final String newStartIP, final String currentStartIP, final String newEndIP, final String currentEndIP, final long zoneId, final long vlanDbId, final long sourceNetworkid, final long physicalNetworkId, final boolean isRangeForSystemVM, final Boolean forSystemVms) {
        long newStartIPLong = NetUtils.ip2Long(newStartIP);
        long newEndIPLong = NetUtils.ip2Long(newEndIP);
        long currentStartIPLong = NetUtils.ip2Long(currentStartIP);
        long currentEndIPLong = NetUtils.ip2Long(currentEndIP);

        List<Long> currentIPRange = new ArrayList<>();
        List<Long> newIPRange = new ArrayList<>();
        while (newStartIPLong <= newEndIPLong) {
            newIPRange.add(newStartIPLong);
            newStartIPLong++;
        }
        while (currentStartIPLong <= currentEndIPLong) {
            currentIPRange.add(currentStartIPLong);
            currentStartIPLong++;
        }

        final List<String> problemIps = Transaction.execute(new TransactionCallback<List<String>>() {

            @Override
            public List<String> doInTransaction(final TransactionStatus status) {
                final IPRangeConfig config = new IPRangeConfig();
                Vector<String> configResult = new Vector<>();
                List<Long> ipAddressesToAdd = new ArrayList(newIPRange);
                ipAddressesToAdd.removeAll(currentIPRange);
                if (ipAddressesToAdd.size() > 0) {
                    for (Long startIP : ipAddressesToAdd) {
                        configResult.addAll(config.savePublicIPRange(TransactionLegacy.currentTxn(), startIP, startIP, zoneId, vlanDbId, sourceNetworkid, physicalNetworkId, forSystemVms != null ? forSystemVms : isRangeForSystemVM));
                    }
                }
                List<Long> ipAddressesToDelete = new ArrayList(currentIPRange);
                ipAddressesToDelete.removeAll(newIPRange);
                if (ipAddressesToDelete.size() > 0) {
                    for (Long startIP : ipAddressesToDelete) {
                        configResult.addAll(config.deletePublicIPRange(TransactionLegacy.currentTxn(), startIP, startIP, vlanDbId));
                    }
                }
                if (forSystemVms != null && isRangeForSystemVM != forSystemVms) {
                    List<Long> ipAddressesToUpdate = new ArrayList(currentIPRange);
                    ipAddressesToUpdate.removeAll(ipAddressesToDelete);
                    if (ipAddressesToUpdate.size() > 0) {
                        for (Long startIP : ipAddressesToUpdate) {
                            configResult.addAll(config.updatePublicIPRange(TransactionLegacy.currentTxn(), startIP, startIP, vlanDbId, forSystemVms));
                        }
                    }
                }
                return configResult;
            }
        });
        return problemIps != null && problemIps.size() == 0;
    }

    private void checkPublicIpRangeErrors(final long zoneId, final String vlanId, final String vlanGateway, final String vlanNetmask, final String startIP, final String endIP) {
        // Check that the start and end IPs are valid
        if (!NetUtils.isValidIp4(startIP)) {
            throw new InvalidParameterValueException("Please specify a valid start IP");
        }

        if (endIP != null && !NetUtils.isValidIp4(endIP)) {
            throw new InvalidParameterValueException("Please specify a valid end IP");
        }

        if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
            throw new InvalidParameterValueException("Please specify a valid IP range.");
        }

        // Check that the IPs that are being added are compatible with the
        // VLAN's gateway and netmask
        if (vlanNetmask == null) {
            throw new InvalidParameterValueException("Please ensure that your IP range's netmask is specified");
        }

        if (endIP != null && !NetUtils.sameSubnet(startIP, endIP, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the IP range's netmask.");
        }

        if (!NetUtils.sameSubnet(startIP, vlanGateway, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
        }

        if (endIP != null && !NetUtils.sameSubnet(endIP, vlanGateway, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
        }
        // check if the gatewayip is the part of the ip range being added.
        // RFC 3021 - 31-Bit Prefixes on IPv4 Point-to-Point Links
        //     GW              Netmask         Stat IP        End IP
        // 192.168.24.0 - 255.255.255.254 - 192.168.24.0 - 192.168.24.1
        // https://tools.ietf.org/html/rfc3021
        // Added by Wilder Rodrigues
        final String newCidr = NetUtils.getCidrFromGatewayAndNetmask(vlanGateway, vlanNetmask);
        if (!NetUtils.is31PrefixCidr(newCidr)) {
            if (NetUtils.ipRangesOverlap(startIP, endIP, vlanGateway, vlanGateway)) {
                throw new InvalidParameterValueException(
                        "The gateway ip should not be the part of the ip range being added.");
            }
        }
    }

    private void checkConflictsWithPortableIpRange(final long zoneId, final String vlanId, final String vlanGateway, final String vlanNetmask, final String startIP, final String endIP) {
        // check and throw exception if there is portable IP range that overlaps with public ip range being configured
        if (checkOverlapPortableIpRange(_regionDao.getRegionId(), startIP, endIP)) {
            throw new InvalidParameterValueException("Ip range: " + startIP + "-" + endIP + " overlaps with a portable" + " IP range already configured in the region "
                    + _regionDao.getRegionId());
        }

        // verify and throw exception if the VLAN Id is used by any portable IP range
        final List<PortableIpRangeVO> existingPortableIPRanges = _portableIpRangeDao.listByRegionId(_regionDao.getRegionId());
        if (existingPortableIPRanges != null && !existingPortableIPRanges.isEmpty()) {
            for (final PortableIpRangeVO portableIpRange : existingPortableIPRanges) {
                if (NetUtils.isSameIsolationId(portableIpRange.getVlanTag(), vlanId)) {
                    throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for portable ip range in this region");
                }
            }
        }
    }

    private String getCidrAddress(final String cidr) {
        final String[] cidrPair = cidr.split("\\/");
        return cidrPair[0];
    }

    private int getCidrSize(final String cidr) {
        final String[] cidrPair = cidr.split("\\/");
        return Integer.parseInt(cidrPair[1]);
    }

    @Override
    public void checkPodCidrSubnets(final long dcId, final Long podIdToBeSkipped, final String cidr) {
        // For each pod, return an error if any of the following is true:
        // The pod's CIDR subnet conflicts with the CIDR subnet of any other pod

        // Check if the CIDR conflicts with the Guest Network or other pods
        long skipPod = 0;
        if (podIdToBeSkipped != null) {
            skipPod = podIdToBeSkipped;
        }
        final HashMap<Long, List<Object>> currentPodCidrSubnets = _podDao.getCurrentPodCidrSubnets(dcId, skipPod);
        final List<Object> newCidrPair = new ArrayList<Object>();
        newCidrPair.add(0, getCidrAddress(cidr));
        newCidrPair.add(1, (long)getCidrSize(cidr));
        currentPodCidrSubnets.put(new Long(-1), newCidrPair);

        final DataCenterVO dcVo = _zoneDao.findById(dcId);
        final String guestNetworkCidr = dcVo.getGuestNetworkCidr();

        // Guest cidr can be null for Basic zone
        String guestIpNetwork = null;
        Long guestCidrSize = null;
        if (guestNetworkCidr != null) {
            final String[] cidrTuple = guestNetworkCidr.split("\\/");
            guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
            guestCidrSize = Long.parseLong(cidrTuple[1]);
        }

        final String zoneName = getZoneName(dcId);

        // Iterate through all pods in this zone
        for (final Long podId : currentPodCidrSubnets.keySet()) {
            String podName;
            if (podId.longValue() == -1) {
                podName = "newPod";
            } else {
                podName = getPodName(podId.longValue());
            }

            final List<Object> cidrPair = currentPodCidrSubnets.get(podId);
            final String cidrAddress = (String)cidrPair.get(0);
            final long cidrSize = ((Long)cidrPair.get(1)).longValue();

            long cidrSizeToUse = -1;
            if (guestCidrSize == null || cidrSize < guestCidrSize) {
                cidrSizeToUse = cidrSize;
            } else {
                cidrSizeToUse = guestCidrSize;
            }

            String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);

            if (guestNetworkCidr != null) {
                final String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
                // Check that cidrSubnet does not equal guestSubnet
                if (cidrSubnet.equals(guestSubnet)) {
                    if (podName.equals("newPod")) {
                        throw new InvalidParameterValueException(
                                "The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.");
                    } else {
                        throw new InvalidParameterValueException(
                                "Warning: The subnet of pod "
                                        + podName
                                        + " in zone "
                                        + zoneName
                                        + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.");
                    }
                }
            }

            // Iterate through the rest of the pods
            for (final Long otherPodId : currentPodCidrSubnets.keySet()) {
                if (podId.equals(otherPodId)) {
                    continue;
                }

                // Check that cidrSubnet does not equal otherCidrSubnet
                final List<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
                final String otherCidrAddress = (String)otherCidrPair.get(0);
                final long otherCidrSize = ((Long)otherCidrPair.get(1)).longValue();

                if (cidrSize < otherCidrSize) {
                    cidrSizeToUse = cidrSize;
                } else {
                    cidrSizeToUse = otherCidrSize;
                }

                cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
                final String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);

                if (cidrSubnet.equals(otherCidrSubnet)) {
                    final String otherPodName = getPodName(otherPodId.longValue());
                    if (podName.equals("newPod")) {
                        throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName
                                + ". Please specify a different CIDR.");
                    } else {
                        throw new InvalidParameterValueException("Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName
                                + " have conflicting CIDR subnets. Please change the CIDR of one of these pods.");
                    }
                }
            }
        }

    }

    private boolean validPod(final long podId) {
        return _podDao.findById(podId) != null;
    }

    private boolean validPod(final String podName, final long zoneId) {
        return _podDao.findByName(podName, zoneId) != null;
    }

    private String getPodName(final long podId) {
        return _podDao.findById(new Long(podId)).getName();
    }

    private boolean validZone(final String zoneName) {
        return _zoneDao.findByName(zoneName) != null;
    }

    private boolean validZone(final long zoneId) {
        return _zoneDao.findById(zoneId) != null;
    }

    private String getZoneName(final long zoneId) {
        final DataCenterVO zone = _zoneDao.findById(new Long(zoneId));
        if (zone != null) {
            return zone.getName();
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_DELETE, eventDescription = "deleting vlan ip range", async = false)
    public boolean deleteVlanIpRange(final DeleteVlanIpRangeCmd cmd) {
        final Long vlanDbId = cmd.getId();

        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        return deleteVlanAndPublicIpRange(CallContext.current().getCallingUserId(), vlanDbId, CallContext.current().getCallingAccount());
    }

    @Override
    public void checkDiskOfferingAccess(final Account caller, final DiskOffering dof, DataCenter zone) {
        for (final SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, dof, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to disk offering:" + dof.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException(String.format("Access denied to %s for disk offering: %s, zone: %s by %s", caller, dof.getUuid(), zone.getUuid(), checker.getName()));
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to disk offering:" + dof.getId());
    }

    @Override
    public void checkZoneAccess(final Account caller, final DataCenter zone) {
        for (final SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to zone:" + zone.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException("Access denied to " + caller + " by " + checker.getName() + " for zone " + zone.getId());
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to zone:" + zone.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_CREATE, eventDescription = "creating network offering")
    public NetworkOffering createNetworkOffering(final CreateNetworkOfferingCmd cmd) {
        final String name = cmd.getNetworkOfferingName();
        final String displayText = cmd.getDisplayText();
        final NetUtils.InternetProtocol internetProtocol = NetUtils.InternetProtocol.fromValue(cmd.getInternetProtocol());
        final String tags = cmd.getTags();
        final String trafficTypeString = cmd.getTraffictype();
        final boolean specifyVlan = cmd.getSpecifyVlan();
        final boolean conserveMode = cmd.getConserveMode();
        final String availabilityStr = cmd.getAvailability();
        Integer networkRate = cmd.getNetworkRate();
        TrafficType trafficType = null;
        Availability availability = null;
        Network.GuestType guestType = null;
        final boolean specifyIpRanges = cmd.getSpecifyIpRanges();
        final boolean isPersistent = cmd.getIsPersistent();
        final Map<String, String> detailsStr = cmd.getDetails();
        final Boolean egressDefaultPolicy = cmd.getEgressDefaultPolicy();
        Boolean forVpc = cmd.getForVpc();
        Boolean forTungsten = cmd.getForTungsten();
        Integer maxconn = null;
        boolean enableKeepAlive = false;
        String servicePackageuuid = cmd.getServicePackageId();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        final boolean enable = cmd.getEnable();
        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        // if network offering is for tungsten check if every item from serviceProviderList has Tungsten-Fabric provider
        // except ConfigDrive
        if(Boolean.TRUE.equals(forTungsten)){
            for(Map.Entry<String, List<String>> item : cmd.getServiceProviders().entrySet()) {
                if (item.getValue().size() != 1 || !(item.getValue().contains("Tungsten") || item.getValue().contains("ConfigDrive"))) {
                    throw new InvalidParameterValueException("Please specify Tungsten-Fabric provider for the " + item.getKey() + " service provider.");
                }
            }
        }

        // Verify traffic type
        for (final TrafficType tType : TrafficType.values()) {
            if (tType.name().equalsIgnoreCase(trafficTypeString)) {
                trafficType = tType;
                break;
            }
        }
        if (trafficType == null) {
            throw new InvalidParameterValueException("Invalid value for traffictype. Supported traffic types: Public, Management, Control, Guest, Vlan or Storage");
        }

        // Only GUEST traffic type is supported in Acton
        if (trafficType != TrafficType.Guest) {
            throw new InvalidParameterValueException("Only traffic type " + TrafficType.Guest + " is supported in the current release");
        }

        // Verify offering type
        for (final Network.GuestType offType : Network.GuestType.values()) {
            if (offType.name().equalsIgnoreCase(cmd.getGuestIpType())) {
                guestType = offType;
                break;
            }
        }

        if (guestType == null) {
            throw new InvalidParameterValueException("Invalid \"type\" parameter is given; can have Shared and Isolated values");
        }

        if (internetProtocol != null) {
            if (!GuestType.Isolated.equals(guestType)) {
                throw new InvalidParameterValueException(String.format("%s is supported only for %s guest type", ApiConstants.INTERNET_PROTOCOL, GuestType.Isolated));
            }

            if (!Ipv6Service.Ipv6OfferingCreationEnabled.value() && !NetUtils.InternetProtocol.IPv4.equals(internetProtocol)) {
                throw new InvalidParameterValueException(String.format("Configuration %s needs to be enabled for creating IPv6 supported network offering", Ipv6Service.Ipv6OfferingCreationEnabled.key()));
            }
        }

        // Verify availability
        for (final Availability avlb : Availability.values()) {
            if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                availability = avlb;
            }
        }

        if (availability == null) {
            throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional);
        }

        if (networkRate != null && networkRate < 0) {
            networkRate = 0;
        }

        final Long serviceOfferingId = cmd.getServiceOfferingId();

        if (serviceOfferingId != null) {
            _networkSvc.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(serviceOfferingId);
        }

        // configure service provider map
        final Map<Network.Service, Set<Network.Provider>> serviceProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        final Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();

        // populate the services first
        for (final String serviceName : cmd.getSupportedServices()) {
            // validate if the service is supported
            final Service service = Network.Service.getService(serviceName);
            if (service == null || service == Service.Gateway) {
                throw new InvalidParameterValueException("Invalid service " + serviceName);
            }

            if (forVpc == null) {
                if (service == Service.SecurityGroup || service == Service.Firewall) {
                    forVpc = false;
                } else if (service == Service.NetworkACL) {
                    forVpc = true;
                }
            }

            if (service == Service.SecurityGroup) {
                // allow security group service for Shared networks only
                if (guestType != GuestType.Shared) {
                    throw new InvalidParameterValueException("Security group service is supported for network offerings with guest ip type " + GuestType.Shared);
                }
                final Set<Network.Provider> sgProviders = new HashSet<Network.Provider>();
                sgProviders.add(Provider.SecurityGroupProvider);
                serviceProviderMap.put(Network.Service.SecurityGroup, sgProviders);
                continue;
            }

            serviceProviderMap.put(service, defaultProviders);
        }

        // add gateway provider (if sourceNat provider is enabled)
        final Set<Provider> sourceNatServiceProviders = serviceProviderMap.get(Service.SourceNat);
        if (sourceNatServiceProviders != null && !sourceNatServiceProviders.isEmpty()) {
            serviceProviderMap.put(Service.Gateway, sourceNatServiceProviders);
        }

        // populate providers
        final Map<Provider, Set<Service>> providerCombinationToVerify = new HashMap<Provider, Set<Service>>();
        final Map<String, List<String>> svcPrv = cmd.getServiceProviders();
        Provider firewallProvider = null;
        Provider dhcpProvider = null;
        Boolean IsVrUserdataProvider = false;
        if (svcPrv != null) {
            for (final String serviceStr : svcPrv.keySet()) {
                final Network.Service service = Network.Service.getService(serviceStr);
                if (serviceProviderMap.containsKey(service)) {
                    final Set<Provider> providers = new HashSet<Provider>();
                    // Allow to specify more than 1 provider per service only if
                    // the service is LB
                    if (!serviceStr.equalsIgnoreCase(Service.Lb.getName()) && svcPrv.get(serviceStr) != null && svcPrv.get(serviceStr).size() > 1) {
                        throw new InvalidParameterValueException("In the current release only one provider can be " + "specified for the service if the service is not LB");
                    }
                    for (final String prvNameStr : svcPrv.get(serviceStr)) {
                        // check if provider is supported
                        final Network.Provider provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }

                        if (provider == Provider.CiscoVnmc) {
                            firewallProvider = provider;
                        }

                        if (provider == Provider.PaloAlto) {
                            firewallProvider = Provider.PaloAlto;
                        }

                        if ((service == Service.PortForwarding || service == Service.StaticNat) && provider == Provider.VirtualRouter) {
                            firewallProvider = Provider.VirtualRouter;
                        }

                        if (forVpc == null && VPC_ONLY_PROVIDERS.contains(provider)) {
                            forVpc = true;
                        }

                        if (forTungsten == null && Provider.Tungsten.equals(provider)){
                            forTungsten = true;
                        }

                        if (service == Service.Dhcp) {
                            dhcpProvider = provider;
                        }

                        if (service == Service.UserData && provider == Provider.VirtualRouter) {
                            IsVrUserdataProvider = true;
                        }

                        providers.add(provider);

                        Set<Service> serviceSet = null;
                        if (providerCombinationToVerify.get(provider) == null) {
                            serviceSet = new HashSet<Service>();
                        } else {
                            serviceSet = providerCombinationToVerify.get(provider);
                        }
                        serviceSet.add(service);
                        providerCombinationToVerify.put(provider, serviceSet);

                    }
                    serviceProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceStr + " is not enabled for the network " + "offering, can't add a provider to it");
                }
            }
        }

        // dhcp provider and userdata provider should be same because vm will be contacting dhcp server for user data.
        if (dhcpProvider == null && IsVrUserdataProvider) {
            s_logger.debug("User data provider VR can't be selected without VR as dhcp provider. In this case VM fails to contact the DHCP server for userdata");
            throw new InvalidParameterValueException("Without VR as dhcp provider, User data can't selected for VR. Please select VR as DHCP provider ");
        }

        // validate providers combination here
        _networkModel.canProviderSupportServices(providerCombinationToVerify);

        // validate the LB service capabilities specified in the network
        // offering
        final Map<Capability, String> lbServiceCapabilityMap = cmd.getServiceCapabilities(Service.Lb);
        if (!serviceProviderMap.containsKey(Service.Lb) && lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for LB service can be specifed only when LB service is enabled for network offering.");
        }
        validateLoadBalancerServiceCapabilities(lbServiceCapabilityMap);

        if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            maxconn = cmd.getMaxconnections();
            if (maxconn == null) {
                maxconn = Integer.parseInt(_configDao.getValue(Config.NetworkLBHaproxyMaxConn.key()));
            }
        }
        if (cmd.getKeepAliveEnabled() != null && cmd.getKeepAliveEnabled()) {
            enableKeepAlive = true;
        }

        // validate the Source NAT service capabilities specified in the network
        // offering
        final Map<Capability, String> sourceNatServiceCapabilityMap = cmd.getServiceCapabilities(Service.SourceNat);
        if (!serviceProviderMap.containsKey(Service.SourceNat) && sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for source NAT service can be specifed only when source NAT service is enabled for network offering.");
        }
        validateSourceNatServiceCapablities(sourceNatServiceCapabilityMap);

        // validate the Static Nat service capabilities specified in the network
        // offering
        final Map<Capability, String> staticNatServiceCapabilityMap = cmd.getServiceCapabilities(Service.StaticNat);
        if (!serviceProviderMap.containsKey(Service.StaticNat) && sourceNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for static NAT service can be specifed only when static NAT service is enabled for network offering.");
        }
        validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);

        // validate the 'Connectivity' service capabilities specified in the network offering, if 'Connectivity' service
        // is in the supported services of network offering
        final Map<Capability, String> connectivityServiceCapabilityMap = cmd.getServiceCapabilities(Service.Connectivity);
        if (!serviceProviderMap.containsKey(Service.Connectivity) &&
                connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty())  {
            throw new InvalidParameterValueException("Capabilities for 'Connectivity' service can be specified " +
                    "only when Connectivity service is enabled for network offering.");
        }
        validateConnectivityServiceCapablities(guestType, serviceProviderMap.get(Service.Connectivity), connectivityServiceCapabilityMap);

        final Map<Service, Map<Capability, String>> serviceCapabilityMap = new HashMap<Service, Map<Capability, String>>();
        serviceCapabilityMap.put(Service.Lb, lbServiceCapabilityMap);
        serviceCapabilityMap.put(Service.SourceNat, sourceNatServiceCapabilityMap);
        serviceCapabilityMap.put(Service.StaticNat, staticNatServiceCapabilityMap);
        serviceCapabilityMap.put(Service.Connectivity, connectivityServiceCapabilityMap);

        // if Firewall service is missing, add Firewall service/provider
        // combination
        if (firewallProvider != null) {
            s_logger.debug("Adding Firewall service with provider " + firewallProvider.getName());
            final Set<Provider> firewallProviderSet = new HashSet<Provider>();
            firewallProviderSet.add(firewallProvider);
            serviceProviderMap.put(Service.Firewall, firewallProviderSet);
            if (!(firewallProvider.getName().equals(Provider.JuniperSRX.getName()) || firewallProvider.getName().equals(Provider.PaloAlto.getName()) || firewallProvider.getName()
                    .equals(Provider.VirtualRouter.getName())) && egressDefaultPolicy == false) {
                throw new InvalidParameterValueException("Firewall egress with default policy " + egressDefaultPolicy + " is not supported by the provider "
                        + firewallProvider.getName());
            }
        }

        final Map<NetworkOffering.Detail, String> details = new HashMap<NetworkOffering.Detail, String>();
        if (detailsStr != null) {
            for (final String detailStr : detailsStr.keySet()) {
                NetworkOffering.Detail offDetail = null;
                for (final NetworkOffering.Detail supportedDetail : NetworkOffering.Detail.values()) {
                    if (detailStr.equalsIgnoreCase(supportedDetail.toString())) {
                        offDetail = supportedDetail;
                        break;
                    }
                }
                if (offDetail == null) {
                    throw new InvalidParameterValueException("Unsupported detail " + detailStr);
                }
                details.put(offDetail, detailsStr.get(detailStr));
            }
        }

        if (forVpc == null) {
            forVpc = false;
        }

        final NetworkOfferingVO offering = createNetworkOffering(name, displayText, trafficType, tags, specifyVlan, availability, networkRate, serviceProviderMap, false, guestType, false,
                serviceOfferingId, conserveMode, serviceCapabilityMap, specifyIpRanges, isPersistent, details, egressDefaultPolicy, maxconn, enableKeepAlive, forVpc, forTungsten, domainIds, zoneIds, enable, internetProtocol);
        CallContext.current().setEventDetails(" Id: " + offering.getId() + " Name: " + name);
        CallContext.current().putContextParameter(NetworkOffering.class, offering.getId());
        return offering;
    }

    void validateLoadBalancerServiceCapabilities(final Map<Capability, String> lbServiceCapabilityMap) {
        if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            if (lbServiceCapabilityMap.keySet().size() > 4 || !lbServiceCapabilityMap.containsKey(Capability.SupportedLBIsolation)) {
                throw new InvalidParameterValueException(String.format("Only %s capabilities can be specified for LB service",
                        StringUtils.join(Capability.SupportedLBIsolation.getName(), Capability.ElasticLb.getName(),
                                Capability.InlineMode.getName(), Capability.LbSchemes.getName(), Capability.VmAutoScaling.getName())));
            }

            for (final Capability cap : lbServiceCapabilityMap.keySet()) {
                final String value = lbServiceCapabilityMap.get(cap);
                if (cap == Capability.SupportedLBIsolation) {
                    final boolean dedicatedLb = value.contains("dedicated");
                    final boolean sharedLB = value.contains("shared");
                    if (dedicatedLb && sharedLB || !dedicatedLb && !sharedLB) {
                        throw new InvalidParameterValueException("Either dedicated or shared isolation can be specified for " + Capability.SupportedLBIsolation.getName());
                    }
                } else if (cap == Capability.ElasticLb) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.ElasticLb.getName());
                    }
                } else if (cap == Capability.InlineMode) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.InlineMode.getName());
                    }
                } else if (cap == Capability.LbSchemes) {
                    final boolean internalLb = value.contains("internal");
                    final boolean publicLb = value.contains("public");
                    if (!internalLb && !publicLb) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.LbSchemes.getName());
                    }
                } else if (cap == Capability.VmAutoScaling) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.VmAutoScaling.getName());
                    }
                } else {
                    throw new InvalidParameterValueException(String.format("Only %s capabilities can be specified for LB service",
                            StringUtils.join(Capability.SupportedLBIsolation.getName(), Capability.ElasticLb.getName(),
                                    Capability.InlineMode.getName(), Capability.LbSchemes.getName(), Capability.VmAutoScaling.getName())));
                }
            }
        }
    }

    void validateSourceNatServiceCapablities(final Map<Capability, String> sourceNatServiceCapabilityMap) {
        if (sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
            if (sourceNatServiceCapabilityMap.keySet().size() > 2) {
                throw new InvalidParameterValueException("Only " + Capability.SupportedSourceNatTypes.getName() + " and " + Capability.RedundantRouter
                        + " capabilities can be sepcified for source nat service");
            }

            for (final Map.Entry<Capability ,String> srcNatPair : sourceNatServiceCapabilityMap.entrySet()) {
                final Capability capability = srcNatPair.getKey();
                final String value = srcNatPair.getValue();
                if (capability == Capability.SupportedSourceNatTypes) {
                    final boolean perAccount = value.contains("peraccount");
                    final boolean perZone = value.contains("perzone");
                    if (perAccount && perZone || !perAccount && !perZone) {
                        throw new InvalidParameterValueException("Either peraccount or perzone source NAT type can be specified for "
                                + Capability.SupportedSourceNatTypes.getName());
                    }
                } else if (capability == Capability.RedundantRouter) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.RedundantRouter.getName());
                    }
                } else {
                    throw new InvalidParameterValueException("Only " + Capability.SupportedSourceNatTypes.getName() + " and " + Capability.RedundantRouter
                            + " capabilities can be sepcified for source nat service");
                }
            }
        }
    }

    void validateStaticNatServiceCapablities(final Map<Capability, String> staticNatServiceCapabilityMap) {
        if (staticNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
            boolean eipEnabled = false;
            boolean associatePublicIP = true;
            for (final Capability capability : staticNatServiceCapabilityMap.keySet()) {
                final String value = staticNatServiceCapabilityMap.get(capability).toLowerCase();
                if (!(value.contains("true") ^ value.contains("false"))) {
                    throw new InvalidParameterValueException("Unknown specified value (" + value + ") for " + capability);
                }
                if (capability == Capability.ElasticIp) {
                    eipEnabled = value.contains("true");
                } else if (capability == Capability.AssociatePublicIP) {
                    associatePublicIP = value.contains("true");
                } else {
                    throw new InvalidParameterValueException("Only " + Capability.ElasticIp.getName() + " and " + Capability.AssociatePublicIP.getName()
                            + " capability can be sepcified for static nat service");
                }
            }
            if (!eipEnabled && associatePublicIP) {
                throw new InvalidParameterValueException("Capability " + Capability.AssociatePublicIP.getName() + " can only be set when capability "
                        + Capability.ElasticIp.getName() + " is true");
            }
        }
    }

    void validateConnectivityServiceCapablities(final Network.GuestType guestType, final Set<Provider> providers, final Map<Capability, String> connectivityServiceCapabilityMap) {
        if (connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty()) {
            for (final Map.Entry<Capability, String>entry: connectivityServiceCapabilityMap.entrySet()) {
                final Capability capability = entry.getKey();
                if (capability == Capability.StretchedL2Subnet || capability == Capability.PublicAccess) {
                    final String value = entry.getValue().toLowerCase();
                    if (!(value.contains("true") ^ value.contains("false"))) {
                        throw new InvalidParameterValueException("Invalid value (" + value + ") for " + capability +
                                " should be true/false");
                    } else if (capability == Capability.PublicAccess && guestType != GuestType.Shared) {
                        throw new InvalidParameterValueException("Capability " + capability.getName() + " can only be enabled for network offerings " +
                                "with guest type Shared.");
                    }
                } else {
                    throw new InvalidParameterValueException("Capability " + capability.getName() + " can not be "
                            + " specified with connectivity service.");
                }
            }

            // validate connectivity service provider actually supports specified capabilities
            if (providers != null && !providers.isEmpty()) {
                for (Capability capability : connectivityServiceCapabilityMap.keySet()) {
                    _networkModel.providerSupportsCapability(providers, Service.Connectivity, capability);
                }
            }
        }
    }

    @Override
    @DB
    public NetworkOfferingVO createNetworkOffering(final String name, final String displayText, final TrafficType trafficType, String tags, final boolean specifyVlan,
            final Availability availability,
            final Integer networkRate, final Map<Service, Set<Provider>> serviceProviderMap, final boolean isDefault, final GuestType type, final boolean systemOnly,
            final Long serviceOfferingId,
            final boolean conserveMode, final Map<Service, Map<Capability, String>> serviceCapabilityMap, final boolean specifyIpRanges, final boolean isPersistent,
            final Map<Detail, String> details, final boolean egressDefaultPolicy, final Integer maxconn, final boolean enableKeepAlive, Boolean forVpc,
            Boolean forTungsten, final List<Long> domainIds, final List<Long> zoneIds, final boolean enableOffering, final NetUtils.InternetProtocol internetProtocol) {

        String servicePackageUuid;
        String spDescription = null;
        if (details == null) {
            servicePackageUuid = null;
        } else {
            servicePackageUuid = details.get(NetworkOffering.Detail.servicepackageuuid);
            spDescription = details.get(NetworkOffering.Detail.servicepackagedescription);
        }


        final String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        final int multicastRate = multicastRateStr == null ? 10 : Integer.parseInt(multicastRateStr);
        tags = com.cloud.utils.StringUtils.cleanupTags(tags);

        // specifyIpRanges should always be true for Shared networks
        // specifyIpRanges can only be true for Isolated networks with no Source
        // Nat service
        if (specifyIpRanges) {
            if (type == GuestType.Isolated) {
                if (serviceProviderMap.containsKey(Service.SourceNat)) {
                    throw new InvalidParameterValueException("SpecifyIpRanges can only be true for Shared network offerings and Isolated with no SourceNat service");
                }
            }
        } else {
            if (type == GuestType.Shared) {
                throw new InvalidParameterValueException("SpecifyIpRanges should always be true for Shared network offerings");
            }
        }

        // isPersistent should always be false for Shared network Offerings
        if (isPersistent && type == GuestType.Shared) {
            throw new InvalidParameterValueException("isPersistent should be false if network offering's type is " + type);
        }

        // validate availability value
        if (availability == NetworkOffering.Availability.Required) {
            final boolean canOffBeRequired = type == GuestType.Isolated && serviceProviderMap.containsKey(Service.SourceNat);
            if (!canOffBeRequired) {
                throw new InvalidParameterValueException("Availability can be " + NetworkOffering.Availability.Required + " only for networkOfferings of type "
                        + GuestType.Isolated + " and with " + Service.SourceNat.getName() + " enabled");
            }

            // only one network offering in the system can be Required
            final List<NetworkOfferingVO> offerings = _networkOfferingDao.listByAvailability(Availability.Required, false);
            if (!offerings.isEmpty()) {
                throw new InvalidParameterValueException("System already has network offering id=" + offerings.get(0).getId() + " with availability " + Availability.Required);
            }
        }

        boolean dedicatedLb = false;
        boolean elasticLb = false;
        boolean sharedSourceNat = false;
        boolean redundantRouter = false;
        boolean elasticIp = false;
        boolean associatePublicIp = false;
        boolean inline = false;
        boolean publicLb = false;
        boolean internalLb = false;
        boolean strechedL2Subnet = false;
        boolean publicAccess = false;
        boolean vmAutoScaling = false;

        if (serviceCapabilityMap != null && !serviceCapabilityMap.isEmpty()) {
            final Map<Capability, String> lbServiceCapabilityMap = serviceCapabilityMap.get(Service.Lb);

            if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
                final String isolationCapability = lbServiceCapabilityMap.get(Capability.SupportedLBIsolation);
                if (isolationCapability != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.SupportedLBIsolation, isolationCapability);
                    dedicatedLb = isolationCapability.contains("dedicated");
                } else {
                    dedicatedLb = true;
                }

                final String param = lbServiceCapabilityMap.get(Capability.ElasticLb);
                if (param != null) {
                    elasticLb = param.contains("true");
                }

                final String inlineMode = lbServiceCapabilityMap.get(Capability.InlineMode);
                if (inlineMode != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.InlineMode, inlineMode);
                    inline = inlineMode.contains("true");
                } else {
                    inline = false;
                }

                final String publicLbStr = lbServiceCapabilityMap.get(Capability.LbSchemes);
                if (serviceProviderMap.containsKey(Service.Lb)) {
                    if (publicLbStr != null) {
                        _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.LbSchemes, publicLbStr);
                        internalLb = publicLbStr.contains("internal");
                        publicLb = publicLbStr.contains("public");
                    }
                }

                final String vmAutoScalingStr = lbServiceCapabilityMap.get(Capability.VmAutoScaling);
                if (vmAutoScalingStr != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.VmAutoScaling, vmAutoScalingStr);
                    vmAutoScaling = vmAutoScalingStr.contains("true");
                }
            }

            // in the current version of the code, publicLb and specificLb can't
            // both be set to true for the same network offering
            if (publicLb && internalLb) {
                throw new InvalidParameterValueException("Public lb and internal lb can't be enabled at the same time on the offering");
            }

            final Map<Capability, String> sourceNatServiceCapabilityMap = serviceCapabilityMap.get(Service.SourceNat);
            if (sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
                final String sourceNatType = sourceNatServiceCapabilityMap.get(Capability.SupportedSourceNatTypes);
                if (sourceNatType != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.SourceNat), Service.SourceNat, Capability.SupportedSourceNatTypes, sourceNatType);
                    sharedSourceNat = sourceNatType.contains("perzone");
                }

                final String param = sourceNatServiceCapabilityMap.get(Capability.RedundantRouter);
                if (param != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.SourceNat), Service.SourceNat, Capability.RedundantRouter, param);
                    redundantRouter = param.contains("true");
                }
            }

            final Map<Capability, String> staticNatServiceCapabilityMap = serviceCapabilityMap.get(Service.StaticNat);
            if (staticNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
                final String param = staticNatServiceCapabilityMap.get(Capability.ElasticIp);
                if (param != null) {
                    elasticIp = param.contains("true");
                    final String associatePublicIP = staticNatServiceCapabilityMap.get(Capability.AssociatePublicIP);
                    if (associatePublicIP != null) {
                        associatePublicIp = associatePublicIP.contains("true");
                    }
                }
            }

            final Map<Capability, String> connectivityServiceCapabilityMap = serviceCapabilityMap.get(Service.Connectivity);
            if (connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty()) {
                if (connectivityServiceCapabilityMap.containsKey(Capability.StretchedL2Subnet)) {
                    final String value = connectivityServiceCapabilityMap.get(Capability.StretchedL2Subnet);
                    if ("true".equalsIgnoreCase(value)) {
                        strechedL2Subnet = true;
                    }
                }

                if (connectivityServiceCapabilityMap.containsKey(Capability.PublicAccess)) {
                    final String value = connectivityServiceCapabilityMap.get(Capability.PublicAccess);
                    if ("true".equalsIgnoreCase(value)) {
                        publicAccess = true;
                    }
                }
            }
        }

        if (serviceProviderMap != null && serviceProviderMap.containsKey(Service.Lb) && !internalLb && !publicLb) {
            //if not specified, default public lb to true
            publicLb = true;
        }

        final NetworkOfferingVO offeringFinal = new NetworkOfferingVO(name, displayText, trafficType, systemOnly, specifyVlan, networkRate, multicastRate, isDefault, availability,
                tags, type, conserveMode, dedicatedLb, sharedSourceNat, redundantRouter, elasticIp, elasticLb, specifyIpRanges, inline, isPersistent, associatePublicIp, publicLb,
                internalLb, forVpc, egressDefaultPolicy, strechedL2Subnet, publicAccess);

        if (serviceOfferingId != null) {
            offeringFinal.setServiceOfferingId(serviceOfferingId);
        }

        offeringFinal.setForTungsten(Objects.requireNonNullElse(forTungsten, false));

        if (enableOffering) {
            offeringFinal.setState(NetworkOffering.State.Enabled);
        }

        // Set VM AutoScaling capability
        offeringFinal.setSupportsVmAutoScaling(vmAutoScaling);

        //Set Service package id
        offeringFinal.setServicePackage(servicePackageUuid);
        // validate the details
        if (details != null) {
            validateNtwkOffDetails(details, serviceProviderMap);
        }

        boolean vpcOff = false;
        boolean nsOff = false;

        if (serviceProviderMap != null && spDescription != null) {
            for (final Network.Service service : serviceProviderMap.keySet()) {
                final Set<Provider> providers = serviceProviderMap.get(service);
                if (providers != null && !providers.isEmpty()) {
                    for (final Network.Provider provider : providers) {
                        if (provider == Provider.VPCVirtualRouter) {
                            vpcOff = true;
                        }
                        if (provider == Provider.Netscaler) {
                            nsOff = true;
                        }
                    }
                }
            }
            if(vpcOff && nsOff) {
                if(!(spDescription.equalsIgnoreCase("A NetScalerVPX is dedicated per network.") || spDescription.contains("dedicated NetScaler"))) {
                    throw new InvalidParameterValueException("Only NetScaler Service Package with Dedicated Device Mode is Supported in VPC Type Guest Network");
                }
            }
        }

        return Transaction.execute(new TransactionCallback<NetworkOfferingVO>() {
            @Override
            public NetworkOfferingVO doInTransaction(final TransactionStatus status) {
                NetworkOfferingVO offering = offeringFinal;

                // 1) create network offering object
                s_logger.debug("Adding network offering " + offering);
                offering.setConcurrentConnections(maxconn);
                offering.setKeepAliveEnabled(enableKeepAlive);
                offering = _networkOfferingDao.persist(offering, details);
                // 2) populate services and providers
                if (serviceProviderMap != null) {
                    for (final Network.Service service : serviceProviderMap.keySet()) {
                        final Set<Provider> providers = serviceProviderMap.get(service);
                        if (providers != null && !providers.isEmpty()) {
                            boolean vpcOff = false;
                            for (final Network.Provider provider : providers) {
                                if (provider == Provider.VPCVirtualRouter) {
                                    vpcOff = true;
                                }
                                final NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, provider);
                                _ntwkOffServiceMapDao.persist(offService);
                                s_logger.trace("Added service for the network offering: " + offService + " with provider " + provider.getName());
                            }

                            if (vpcOff) {
                                final List<Service> supportedSvcs = new ArrayList<Service>();
                                supportedSvcs.addAll(serviceProviderMap.keySet());
                                _vpcMgr.validateNtwkOffForVpc(offering, supportedSvcs);
                            }
                        } else {
                            final NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, null);
                            _ntwkOffServiceMapDao.persist(offService);
                            s_logger.trace("Added service for the network offering: " + offService + " with null provider");
                        }
                    }
                    if (offering != null) {
                        // Filter child domains when both parent and child domains are present
                        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);
                        List<NetworkOfferingDetailsVO> detailsVO = new ArrayList<>();
                        for (Long domainId : filteredDomainIds) {
                            detailsVO.add(new NetworkOfferingDetailsVO(offering.getId(), Detail.domainid, String.valueOf(domainId), false));
                        }
                        if (CollectionUtils.isNotEmpty(zoneIds)) {
                            for (Long zoneId : zoneIds) {
                                detailsVO.add(new NetworkOfferingDetailsVO(offering.getId(), Detail.zoneid, String.valueOf(zoneId), false));
                            }
                        }
                        if (internetProtocol != null) {
                            detailsVO.add(new NetworkOfferingDetailsVO(offering.getId(), Detail.internetProtocol, String.valueOf(internetProtocol), true));
                        }
                        if (!detailsVO.isEmpty()) {
                            for (NetworkOfferingDetailsVO detail : detailsVO) {
                                networkOfferingDetailsDao.persist(detail);
                            }
                        }
                    }
                }

                return offering;
            }
        });
    }

    protected void validateNtwkOffDetails(final Map<Detail, String> details, final Map<Service, Set<Provider>> serviceProviderMap) {
        for (final Detail detail : details.keySet()) {

            Provider lbProvider = null;
            if (detail == NetworkOffering.Detail.InternalLbProvider || detail == NetworkOffering.Detail.PublicLbProvider) {
                // 1) Vaidate the detail values - have to match the lb provider
                // name
                final String providerStr = details.get(detail);
                if (Network.Provider.getProvider(providerStr) == null) {
                    throw new InvalidParameterValueException("Invalid value " + providerStr + " for the detail " + detail);
                }
                if (serviceProviderMap.get(Service.Lb) != null) {
                    for (final Provider provider : serviceProviderMap.get(Service.Lb)) {
                        if (provider.getName().equalsIgnoreCase(providerStr)) {
                            lbProvider = provider;
                            break;
                        }
                    }
                }

                if (lbProvider == null) {
                    throw new InvalidParameterValueException("Invalid value " + details.get(detail) + " for the detail " + detail
                            + ". The provider is not supported by the network offering");
                }

                // 2) validate if the provider supports the scheme
                final Set<Provider> lbProviders = new HashSet<Provider>();
                lbProviders.add(lbProvider);
                if (detail == NetworkOffering.Detail.InternalLbProvider) {
                    _networkModel.checkCapabilityForProvider(lbProviders, Service.Lb, Capability.LbSchemes, Scheme.Internal.toString());
                } else if (detail == NetworkOffering.Detail.PublicLbProvider) {
                    _networkModel.checkCapabilityForProvider(lbProviders, Service.Lb, Capability.LbSchemes, Scheme.Public.toString());
                }
            }
        }
    }

    @Override
    public Pair<List<? extends NetworkOffering>, Integer> searchForNetworkOfferings(final ListNetworkOfferingsCmd cmd) {
        final Filter searchFilter = new Filter(NetworkOfferingJoinVO.class, "sortKey", QueryService.SortKeyAscending.value(), null, null);
        searchFilter.addOrderBy(NetworkOfferingJoinVO.class, "id", true);
        final Account caller = CallContext.current().getCallingAccount();
        final SearchCriteria<NetworkOfferingJoinVO> sc = networkOfferingJoinDao.createSearchCriteria();

        final Long id = cmd.getId();
        final Object name = cmd.getNetworkOfferingName();
        final Object displayText = cmd.getDisplayText();
        final Object trafficType = cmd.getTrafficType();
        final Object isDefault = cmd.getIsDefault();
        final Object specifyVlan = cmd.getSpecifyVlan();
        final Object availability = cmd.getAvailability();
        final Object state = cmd.getState();
        final Long domainId = cmd.getDomainId();
        final Long zoneId = cmd.getZoneId();
        DataCenter zone = null;
        final Long networkId = cmd.getNetworkId();
        final String guestIpType = cmd.getGuestIpType();
        final List<String> supportedServicesStr = cmd.getSupportedServices();
        final Object specifyIpRanges = cmd.getSpecifyIpRanges();
        final String tags = cmd.getTags();
        final Boolean isTagged = cmd.isTagged();
        final Boolean forVpc = cmd.getForVpc();

        if (domainId != null) {
            Domain domain = _entityMgr.findById(Domain.class, domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
            }
            if (!_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                throw new InvalidParameterValueException(String.format("Unable to list network offerings for domain: %s as caller does not have access for it", domain.getUuid()));
            }
        }

        if (zoneId != null) {
            zone = _entityMgr.findById(DataCenter.class, zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find the zone by id=" + zoneId);
            }
        }

        final Object keyword = cmd.getKeyword();

        if (keyword != null) {
            final SearchCriteria<NetworkOfferingJoinVO> ssc = networkOfferingJoinDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }

        if (guestIpType != null) {
            sc.addAnd("guestType", SearchCriteria.Op.EQ, guestIpType);
        }

        if (displayText != null) {
            sc.addAnd("displayText", SearchCriteria.Op.LIKE, "%" + displayText + "%");
        }

        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }

        if (isDefault != null) {
            sc.addAnd("isDefault", SearchCriteria.Op.EQ, isDefault);
        }

        // only root admin can list network offering with specifyVlan = true
        if (specifyVlan != null) {
            sc.addAnd("specifyVlan", SearchCriteria.Op.EQ, specifyVlan);
        }

        if (availability != null) {
            sc.addAnd("availability", SearchCriteria.Op.EQ, availability);
        }

        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }

        if (specifyIpRanges != null) {
            sc.addAnd("specifyIpRanges", SearchCriteria.Op.EQ, specifyIpRanges);
        }

        if (zone != null) {
            if (zone.getNetworkType() == NetworkType.Basic) {
                // return empty list as we don't allow to create networks in
                // basic zone, and shouldn't display networkOfferings
                return new Pair<List<? extends NetworkOffering>, Integer>(new ArrayList<NetworkOffering>(), 0);
            }
        }

        // Don't return system network offerings to the user
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, false);

        // if networkId is specified, list offerings available for upgrade only
        // (for this network)
        Network network = null;
        if (networkId != null) {
            // check if network exists and the caller can operate with it
            network = _networkModel.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find the network by id=" + networkId);
            }
            // Don't allow to update system network
            final NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
            if (offering.isSystemOnly()) {
                throw new InvalidParameterValueException("Can't update system networks");
            }

            _accountMgr.checkAccess(caller, null, true, network);

            final List<Long> offeringIds = _networkModel.listNetworkOfferingsForUpgrade(networkId);

            if (!offeringIds.isEmpty()) {
                sc.addAnd("id", SearchCriteria.Op.IN, offeringIds.toArray());
            } else {
                return new Pair<List<? extends NetworkOffering>, Integer>(new ArrayList<NetworkOffering>(), 0);
            }
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (tags != null) {
            sc.addAnd("tags", SearchCriteria.Op.EQ, tags);
        }

        if (isTagged != null) {
            if (isTagged) {
                sc.addAnd("tags", SearchCriteria.Op.NNULL);
            } else {
                sc.addAnd("tags", SearchCriteria.Op.NULL);
            }
        }

        if (zoneId != null) {
            SearchBuilder<NetworkOfferingJoinVO> sb = networkOfferingJoinDao.createSearchBuilder();
            sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.FIND_IN_SET);
            sb.or("zId", sb.entity().getZoneId(), SearchCriteria.Op.NULL);
            sb.done();
            SearchCriteria<NetworkOfferingJoinVO> zoneSC = sb.create();
            zoneSC.setParameters("zoneId", String.valueOf(zoneId));
            sc.addAnd("zoneId", SearchCriteria.Op.SC, zoneSC);
        }

        final List<NetworkOfferingJoinVO> offerings = networkOfferingJoinDao.search(sc, searchFilter);
        // Remove offerings that are not associated with caller's domain or domainId passed
        if ((!Account.Type.ADMIN.equals(caller.getType()) || domainId != null) && CollectionUtils.isNotEmpty(offerings)) {
            ListIterator<NetworkOfferingJoinVO> it = offerings.listIterator();
            while (it.hasNext()) {
                NetworkOfferingJoinVO offering = it.next();
                if (StringUtils.isEmpty(offering.getDomainId())) {
                    continue;
                }
                if (!_domainDao.domainIdListContainsAccessibleDomain(offering.getDomainId(), caller, domainId)) {
                    it.remove();
                }
            }
        }
        final Boolean sourceNatSupported = cmd.getSourceNatSupported();
        final List<String> pNtwkTags = new ArrayList<String>();
        boolean checkForTags = false;
        boolean allowNullTag = false;
        if (zone != null) {
            allowNullTag = allowNetworkOfferingWithNullTag(zoneId, pNtwkTags);
            checkForTags = !pNtwkTags.isEmpty() || allowNullTag;
        }

        // filter by supported services
        final boolean listBySupportedServices = supportedServicesStr != null && !supportedServicesStr.isEmpty() && !offerings.isEmpty();
        final boolean checkIfProvidersAreEnabled = zoneId != null;
        final boolean parseOfferings = listBySupportedServices || sourceNatSupported != null || checkIfProvidersAreEnabled || forVpc != null || network != null;

        if (parseOfferings) {
            final List<NetworkOfferingJoinVO> supportedOfferings = new ArrayList<>();
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

            for (final NetworkOfferingJoinVO offering : offerings) {
                boolean addOffering = true;
                List<Service> checkForProviders = new ArrayList<Service>();

                if (checkForTags && ! checkNetworkOfferingTags(pNtwkTags, allowNullTag, offering.getTags())) {
                    continue;
                }

                if (listBySupportedServices) {
                    addOffering = addOffering && _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), supportedServices);
                }

                if (checkIfProvidersAreEnabled) {
                    if (supportedServices != null && supportedServices.length > 0) {
                        checkForProviders = Arrays.asList(supportedServices);
                    } else {
                        checkForProviders = _networkModel.listNetworkOfferingServices(offering.getId());
                    }

                    addOffering = addOffering && _networkModel.areServicesEnabledInZone(zoneId, offering, checkForProviders);
                }

                if (sourceNatSupported != null) {
                    addOffering = addOffering && _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Network.Service.SourceNat) == sourceNatSupported;
                }

                if (forVpc != null) {
                    addOffering = addOffering && offering.isForVpc() == forVpc.booleanValue();
                } else if (network != null) {
                    addOffering = addOffering && offering.isForVpc() == (network.getVpcId() != null);
                }

                if (addOffering) {
                    supportedOfferings.add(offering);
                }

            }

            // Now apply pagination
            final List<NetworkOfferingJoinVO> wPagination = com.cloud.utils.StringUtils.applyPagination(supportedOfferings, cmd.getStartIndex(), cmd.getPageSizeVal());
            if (wPagination != null) {
                final Pair<List<? extends NetworkOffering>, Integer> listWPagination = new Pair<List<? extends NetworkOffering>, Integer>(wPagination, supportedOfferings.size());
                return listWPagination;
            }
            return new Pair<List<? extends NetworkOffering>, Integer>(supportedOfferings, supportedOfferings.size());
        } else {
            final List<NetworkOfferingJoinVO> wPagination = com.cloud.utils.StringUtils.applyPagination(offerings, cmd.getStartIndex(), cmd.getPageSizeVal());
            if (wPagination != null) {
                final Pair<List<? extends NetworkOffering>, Integer> listWPagination = new Pair<>(wPagination, offerings.size());
                return listWPagination;
            }
            return new Pair<List<? extends NetworkOffering>, Integer>(offerings, offerings.size());
        }
    }

    private boolean allowNetworkOfferingWithNullTag(Long zoneId, List<String> allPhysicalNetworkTags) {
        boolean allowNullTag = false;
        final List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, TrafficType.Guest);
        for (final PhysicalNetworkVO physicalNetwork : physicalNetworks) {
            final List<String> physicalNetworkTags = physicalNetwork.getTags();
            if (CollectionUtils.isEmpty(physicalNetworkTags)) {
                if (!allowNullTag) {
                    allowNullTag = true;
                } else {
                    throw new CloudRuntimeException("There are more than 1 physical network with empty tag in the zone id=" + zoneId);
                }
            } else {
                allPhysicalNetworkTags.addAll(physicalNetworkTags);
            }
        }
        return allowNullTag;
    }

    private boolean checkNetworkOfferingTags(List<String> physicalNetworkTags, boolean allowNullTag, String offeringTags) {
      return (offeringTags != null || allowNullTag) && (offeringTags == null || physicalNetworkTags.contains(offeringTags));
    }

    @Override
    public boolean isOfferingForVpc(final NetworkOffering offering) {
        return offering.isForVpc();
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_DELETE, eventDescription = "deleting network offering")
    public boolean deleteNetworkOffering(final DeleteNetworkOfferingCmd cmd) {
        final Long offeringId = cmd.getId();
        CallContext.current().setEventDetails(" Id: " + offeringId);

        // Verify network offering id
        final NetworkOfferingVO offering = _networkOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        } else if (offering.getRemoved() != null || offering.isSystemOnly()) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        }

        // Don't allow to delete default network offerings
        if (offering.isDefault() == true) {
            throw new InvalidParameterValueException("Default network offering can't be deleted");
        }

        // don't allow to delete network offering if it's in use by existing
        // networks (the offering can be disabled
        // though)
        final int networkCount = _networkDao.getNetworkCountByNetworkOffId(offeringId);
        if (networkCount > 0) {
            throw new InvalidParameterValueException("Can't delete network offering " + offeringId + " as its used by " + networkCount + " networks. "
                    + "To make the network offering unavailable, disable it");
        }

        annotationDao.removeByEntityType(AnnotationService.EntityType.NETWORK_OFFERING.name(), offering.getUuid());

        networkOfferingDetailsDao.removeDetails(offeringId);

        if (_networkOfferingDao.remove(offeringId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_EDIT, eventDescription = "updating network offering")
    public NetworkOffering updateNetworkOffering(final UpdateNetworkOfferingCmd cmd) {
        final String displayText = cmd.getDisplayText();
        final Long id = cmd.getId();
        final String name = cmd.getNetworkOfferingName();
        final String availabilityStr = cmd.getAvailability();
        final Integer sortKey = cmd.getSortKey();
        final Integer maxconn = cmd.getMaxconnections();
        Availability availability = null;
        final String state = cmd.getState();
        final String tags = cmd.getTags();
        final List<Long> domainIds = cmd.getDomainIds();
        final List<Long> zoneIds = cmd.getZoneIds();
        CallContext.current().setEventDetails(" Id: " + id);

        // Verify input parameters
        final NetworkOfferingVO offeringToUpdate = _networkOfferingDao.findById(id);
        if (offeringToUpdate == null) {
            throw new InvalidParameterValueException("unable to find network offering " + id);
        }

        List<Long> existingDomainIds = networkOfferingDetailsDao.findDomainIds(id);
        Collections.sort(existingDomainIds);

        List<Long> existingZoneIds = networkOfferingDetailsDao.findZoneIds(id);
        Collections.sort(existingZoneIds);

        // Don't allow to update system network offering
        if (offeringToUpdate.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system network offerings");
        }

        // check if valid domain
        if (CollectionUtils.isNotEmpty(domainIds)) {
            for (final Long domainId: domainIds) {
                if (_domainDao.findById(domainId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid domain id");
                }
            }
        }

        // check if valid zone
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            for (Long zoneId : zoneIds) {
                if (_zoneDao.findById(zoneId) == null)
                    throw new InvalidParameterValueException("Please specify a valid zone id");
            }
        }

        // Filter child domains when both parent and child domains are present
        List<Long> filteredDomainIds = filterChildSubDomains(domainIds);
        Collections.sort(filteredDomainIds);

        List<Long> filteredZoneIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            filteredZoneIds.addAll(zoneIds);
        }
        Collections.sort(filteredZoneIds);

        final NetworkOfferingVO offering = _networkOfferingDao.createForUpdate(id);

        boolean updateNeeded = name != null || displayText != null || sortKey != null ||
                state != null || tags != null || availabilityStr != null || maxconn != null;

        if(updateNeeded) {
            if (name != null) {
                offering.setName(name);
            }

            if (displayText != null) {
                offering.setDisplayText(displayText);
            }

            if (sortKey != null) {
                offering.setSortKey(sortKey);
            }

            if (state != null) {
                boolean validState = false;
                for (final NetworkOffering.State st : NetworkOffering.State.values()) {
                    if (st.name().equalsIgnoreCase(state)) {
                        validState = true;
                        offering.setState(st);
                    }
                }
                if (!validState) {
                    throw new InvalidParameterValueException("Incorrect state value: " + state);
                }
            }

            if (tags != null) {
                List<DataCenterVO> dataCenters = _zoneDao.listAll();
                TrafficType trafficType = offeringToUpdate.getTrafficType();
                String oldTags = offeringToUpdate.getTags();

                for (DataCenterVO dataCenter : dataCenters) {
                    long zoneId = dataCenter.getId();
                    long newPhysicalNetworkId = _networkModel.findPhysicalNetworkId(zoneId, tags, trafficType);
                    if (oldTags != null) {
                        long oldPhysicalNetworkId = _networkModel.findPhysicalNetworkId(zoneId, oldTags, trafficType);
                        if (newPhysicalNetworkId != oldPhysicalNetworkId) {
                            throw new InvalidParameterValueException("New tags: selects different physical network for zone " + zoneId);
                        }
                    }
                }

                offering.setTags(tags);
            }

            // Verify availability
            if (availabilityStr != null) {
                for (final Availability avlb : Availability.values()) {
                    if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                        availability = avlb;
                    }
                }
                if (availability == null) {
                    throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional);
                } else {
                    if (availability == NetworkOffering.Availability.Required) {
                        final boolean canOffBeRequired = offeringToUpdate.getGuestType() == GuestType.Isolated && _networkModel.areServicesSupportedByNetworkOffering(
                                offeringToUpdate.getId(), Service.SourceNat);
                        if (!canOffBeRequired) {
                            throw new InvalidParameterValueException("Availability can be " + NetworkOffering.Availability.Required + " only for networkOfferings of type "
                                    + GuestType.Isolated + " and with " + Service.SourceNat.getName() + " enabled");
                        }

                        // only one network offering in the system can be Required
                        final List<NetworkOfferingVO> offerings = _networkOfferingDao.listByAvailability(Availability.Required, false);
                        if (!offerings.isEmpty() && offerings.get(0).getId() != offeringToUpdate.getId()) {
                            throw new InvalidParameterValueException("System already has network offering id=" + offerings.get(0).getId() + " with availability "
                                    + Availability.Required);
                        }
                    }
                    offering.setAvailability(availability);
                }
            }
            if (_ntwkOffServiceMapDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Lb)) {
                if (maxconn != null) {
                    offering.setConcurrentConnections(maxconn);
                }
            }

            if (!_networkOfferingDao.update(id, offering)) {
                return null;
            }
        }

        List<NetworkOfferingDetailsVO> detailsVO = new ArrayList<>();
        if(!filteredDomainIds.equals(existingDomainIds) || !filteredZoneIds.equals(existingZoneIds)) {
            SearchBuilder<NetworkOfferingDetailsVO> sb = networkOfferingDetailsDao.createSearchBuilder();
            sb.and("offeringId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
            sb.and("detailName", sb.entity().getName(), SearchCriteria.Op.EQ);
            sb.done();
            SearchCriteria<NetworkOfferingDetailsVO> sc = sb.create();
            sc.setParameters("offeringId", String.valueOf(id));
            if(!filteredDomainIds.equals(existingDomainIds)) {
                sc.setParameters("detailName", ApiConstants.DOMAIN_ID);
                networkOfferingDetailsDao.remove(sc);
                for (Long domainId : filteredDomainIds) {
                    detailsVO.add(new NetworkOfferingDetailsVO(id, Detail.domainid, String.valueOf(domainId), false));
                }
            }
            if(!filteredZoneIds.equals(existingZoneIds)) {
                sc.setParameters("detailName", ApiConstants.ZONE_ID);
                networkOfferingDetailsDao.remove(sc);
                for (Long zoneId : filteredZoneIds) {
                    detailsVO.add(new NetworkOfferingDetailsVO(id, Detail.zoneid, String.valueOf(zoneId), false));
                }
            }
        }
        if (!detailsVO.isEmpty()) {
            for (NetworkOfferingDetailsVO detailVO : detailsVO) {
                networkOfferingDetailsDao.persist(detailVO);
            }
        }

        return _networkOfferingDao.findById(id);
    }

    @Override
    public List<Long> getNetworkOfferingDomains(Long networkOfferingId) {
        final NetworkOffering offeringHandle = _entityMgr.findById(NetworkOffering.class, networkOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find network offering " + networkOfferingId);
        }
        return networkOfferingDetailsDao.findDomainIds(networkOfferingId);
    }

    @Override
    public List<Long> getNetworkOfferingZones(Long networkOfferingId) {
        final NetworkOffering offeringHandle = _entityMgr.findById(NetworkOffering.class, networkOfferingId);
        if (offeringHandle == null) {
            throw new InvalidParameterValueException("Unable to find network offering " + networkOfferingId);
        }
        return networkOfferingDetailsDao.findZoneIds(networkOfferingId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_MARK_DEFAULT_ZONE, eventDescription = "Marking account with the " + "default zone", async = true)
    public AccountVO markDefaultZone(final String accountName, final long domainId, final long defaultZoneId) {

        // Check if the account exists
        final Account account = _accountDao.findEnabledAccount(accountName, domainId);
        if (account == null) {
            s_logger.error("Unable to find account by name: " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Account by name: " + accountName + " doesn't exist in domain " + domainId);
        }

        // Don't allow modification of system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        final AccountVO acctForUpdate = _accountDao.findById(account.getId());

        acctForUpdate.setDefaultZoneId(defaultZoneId);

        if (_accountDao.update(account.getId(), acctForUpdate)) {
            CallContext.current().setEventDetails("Default zone id= " + defaultZoneId);
            return _accountDao.findById(account.getId());
        } else {
            return null;
        }
    }

    // Note: This method will be used for entity name validations in the coming
    // releases (place holder for now)
    @SuppressWarnings("unused")
    private void validateEntityName(final String str) {
        final String forbidden = "~!@#$%^&*()+=";
        final char[] searchChars = forbidden.toCharArray();
        if (str == null || str.length() == 0 || searchChars == null || searchChars.length == 0) {
            return;
        }
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length; j++) {
                if (searchChars[j] == ch) {
                    throw new InvalidParameterValueException("Name cannot contain any of the following special characters:" + forbidden);
                }
            }
        }
    }

    @Override
    public Integer getNetworkOfferingNetworkRate(final long networkOfferingId, final Long dataCenterId) {

        // validate network offering information
        final NetworkOffering no = _entityMgr.findById(NetworkOffering.class, networkOfferingId);
        if (no == null) {
            throw new InvalidParameterValueException("Unable to find network offering by id=" + networkOfferingId);
        }

        Integer networkRate;
        if (no.getRateMbps() != null) {
            networkRate = no.getRateMbps();
        } else {
            networkRate = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(dataCenterId);
        }

        // networkRate is unsigned int in networkOfferings table, and can't be
        // set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with
        // all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    public Account getVlanAccount(final long vlanId) {
        final Vlan vlan = _vlanDao.findById(vlanId);

        // if vlan is Virtual Account specific, get vlan information from the
        // accountVlanMap; otherwise get account information
        // from the network
        if (vlan.getVlanType() == VlanType.VirtualNetwork) {
            final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanId);
            if (maps != null && !maps.isEmpty()) {
                return _accountMgr.getAccount(maps.get(0).getAccountId());
            }
        }

        return null;
    }

    @Override
    public Domain getVlanDomain(long vlanId) {
        Vlan vlan = _vlanDao.findById(vlanId);
        Long domainId = null;

        // if vlan is Virtual Domain specific, get vlan information from the
        // accountVlanMap; otherwise get account information
        // from the network
        if (vlan.getVlanType() == VlanType.VirtualNetwork) {
            List<DomainVlanMapVO> maps = _domainVlanMapDao.listDomainVlanMapsByVlan(vlanId);
            if (maps != null && !maps.isEmpty()) {
                return _domainDao.findById(maps.get(0).getDomainId());
            }
        }

        return null;
    }

    @Override
    public List<? extends NetworkOffering> listNetworkOfferings(final TrafficType trafficType, final boolean systemOnly) {
        final Filter searchFilter = new Filter(NetworkOfferingVO.class, "created", false, null, null);
        final SearchCriteria<NetworkOfferingVO> sc = _networkOfferingDao.createSearchCriteria();
        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, systemOnly);

        return _networkOfferingDao.search(sc, searchFilter);
    }

     @Override
     @DB
     public boolean releaseDomainSpecificVirtualRanges(final long domainId) {
        final List<DomainVlanMapVO> maps = _domainVlanMapDao.listDomainVlanMapsByDomain(domainId);
        if (CollectionUtils.isNotEmpty(maps)) {
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        for (DomainVlanMapVO map : maps) {
                            if (!releasePublicIpRange(map.getVlanDbId(), _accountMgr.getSystemUser().getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM))) {
                                throw new CloudRuntimeException("Failed to release domain specific virtual ip ranges for domain id=" + domainId);
                            }
                        }
                    }
                });
            } catch (final CloudRuntimeException e) {
                s_logger.error(e);
                return false;
            }
        } else {
            s_logger.trace("Domain id=" + domainId + " has no domain specific virtual ip ranges, nothing to release");
        }
        return true;
    }

    @Override
    @DB
    public boolean releaseAccountSpecificVirtualRanges(final long accountId) {
        final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(accountId);
        if (maps != null && !maps.isEmpty()) {
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        for (final AccountVlanMapVO map : maps) {
                            if (!releasePublicIpRange(map.getVlanDbId(), _accountMgr.getSystemUser().getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM))) {
                                throw new CloudRuntimeException("Failed to release account specific virtual ip ranges for account id=" + accountId);
                            }
                        }
                    }
                });
            } catch (final CloudRuntimeException e) {
                s_logger.error(e);
                return false;
            }
        } else {
            s_logger.trace("Account id=" + accountId + " has no account specific virtual ip ranges, nothing to release");
        }
        return true;
    }

    @Override
    public AllocationState findClusterAllocationState(final ClusterVO cluster) {

        if (cluster.getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else if (ApiDBUtils.findPodById(cluster.getPodId()).getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else {
            final DataCenterVO zone = ApiDBUtils.findZoneById(cluster.getDataCenterId());
            return zone.getAllocationState();
        }
    }

    @Override
    public AllocationState findPodAllocationState(final HostPodVO pod) {

        if (pod.getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else {
            final DataCenterVO zone = ApiDBUtils.findZoneById(pod.getDataCenterId());
            return zone.getAllocationState();
        }
    }

    @Override
    public Long getDefaultPageSize() {
        return _defaultPageSize;
    }

    @Override
    public Integer getServiceOfferingNetworkRate(final long serviceOfferingId, final Long dataCenterId) {

        // validate network offering information
        final ServiceOffering offering = _serviceOfferingDao.findById(serviceOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find service offering by id=" + serviceOfferingId);
        }

        Integer networkRate;
        if (offering.getRateMbps() != null) {
            networkRate = offering.getRateMbps();
        } else {
            // for domain router service offering, get network rate from
            if (offering.getSystemVmType() != null && offering.getSystemVmType().equalsIgnoreCase(VirtualMachine.Type.DomainRouter.toString())) {
                networkRate = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(dataCenterId);
            } else {
                networkRate = Integer.parseInt(_configDao.getValue(Config.VmNetworkThrottlingRate.key()));
            }
        }

        // networkRate is unsigned int in serviceOffering table, and can't be
        // set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with
        // all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_RANGE_CREATE, eventDescription = "creating portable ip range", async = false)
    public PortableIpRange createPortableIpRange(final CreatePortableIpRangeCmd cmd) throws ConcurrentOperationException {
        final Integer regionId = cmd.getRegionId();
        final String startIP = cmd.getStartIp();
        final String endIP = cmd.getEndIp();
        final String gateway = cmd.getGateway();
        final String netmask = cmd.getNetmask();
        String vlanId = cmd.getVlan();

        final RegionVO region = _regionDao.findById(regionId);
        if (region == null) {
            throw new InvalidParameterValueException("Invalid region ID: " + regionId);
        }

        if (!NetUtils.isValidIp4(startIP) || !NetUtils.isValidIp4(endIP) || !NetUtils.validIpRange(startIP, endIP)) {
            throw new InvalidParameterValueException("Invalid portable ip  range: " + startIP + "-" + endIP);
        }

        if (!NetUtils.sameSubnet(startIP, gateway, netmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as "
                    + "your portable IP range's gateway and as per the IP range's netmask.");
        }

        if (!NetUtils.sameSubnet(endIP, gateway, netmask)) {
            throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as "
                    + "your portable IP range's gateway and as per the IP range's netmask.");
        }

        if (checkOverlapPortableIpRange(regionId, startIP, endIP)) {
            throw new InvalidParameterValueException("Ip  range: " + startIP + "-" + endIP + " overlaps with a portable" + " IP range already configured in the region " + regionId);
        }

        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        } else {
            if (!NetUtils.isValidVlan(vlanId)) {
                throw new InvalidParameterValueException("Invalid vlan id " + vlanId);
            }

            final List<DataCenterVO> zones = _zoneDao.listAllZones();
            if (zones != null && !zones.isEmpty()) {
                for (final DataCenterVO zone : zones) {
                    // check if there is zone vlan with same id
                    if (_vlanDao.findByZoneAndVlanId(zone.getId(), vlanId) != null) {
                        throw new InvalidParameterValueException("Found a VLAN id " + vlanId + " already existing in" + " zone " + zone.getUuid()
                                + " that conflicts with VLAN id of the portable ip range being configured");
                    }
                    //check if there is a public ip range that overlaps with portable ip range being created
                    checkOverlapPublicIpRange(zone.getId(), startIP, endIP);
                }
            }

        }
        final GlobalLock portableIpLock = GlobalLock.getInternLock("PortablePublicIpRange");
        portableIpLock.lock(5);
        try {
            final String vlanIdFinal = vlanId;
            return Transaction.execute(new TransactionCallback<PortableIpRangeVO>() {
                @Override
                public PortableIpRangeVO doInTransaction(final TransactionStatus status) {
                    PortableIpRangeVO portableIpRange = new PortableIpRangeVO(regionId, vlanIdFinal, gateway, netmask, startIP, endIP);
                    portableIpRange = _portableIpRangeDao.persist(portableIpRange);

                    long startIpLong = NetUtils.ip2Long(startIP);
                    final long endIpLong = NetUtils.ip2Long(endIP);
                    while (startIpLong <= endIpLong) {
                        final PortableIpVO portableIP = new PortableIpVO(regionId, portableIpRange.getId(), vlanIdFinal, gateway, netmask, NetUtils.long2Ip(startIpLong));
                        _portableIpDao.persist(portableIP);
                        startIpLong++;
                    }

                    // implicitly enable portable IP service for the region
                    region.setPortableipEnabled(true);
                    _regionDao.update(region.getId(), region);

                    return portableIpRange;
                }
            });
        } finally {
            portableIpLock.unlock();
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_RANGE_DELETE, eventDescription = "deleting portable ip range", async = false)
    public boolean deletePortableIpRange(final DeletePortableIpRangeCmd cmd) {
        final long rangeId = cmd.getId();

        final PortableIpRangeVO portableIpRange = _portableIpRangeDao.findById(rangeId);
        if (portableIpRange == null) {
            throw new InvalidParameterValueException("Please specify a valid portable IP range id.");
        }

        final List<PortableIpVO> fullIpRange = _portableIpDao.listByRangeId(portableIpRange.getId());
        final List<PortableIpVO> freeIpRange = _portableIpDao.listByRangeIdAndState(portableIpRange.getId(), PortableIp.State.Free);

        if (fullIpRange != null && freeIpRange != null) {
            if (fullIpRange.size() == freeIpRange.size()) {
                _portableIpRangeDao.expunge(portableIpRange.getId());
                final List<PortableIpRangeVO> pipranges = _portableIpRangeDao.listAll();
                if (pipranges == null || pipranges.isEmpty()) {
                    final RegionVO region = _regionDao.findById(portableIpRange.getRegionId());
                    region.setPortableipEnabled(false);
                    _regionDao.update(region.getId(), region);
                }
                return true;
            } else {
                throw new InvalidParameterValueException("Can't delete portable IP range as there are IP's assigned.");
            }
        }
        return false;
    }

    @Override
    public List<? extends PortableIpRange> listPortableIpRanges(final ListPortableIpRangesCmd cmd) {
        final Integer regionId = cmd.getRegionIdId();
        final Long rangeId = cmd.getPortableIpRangeId();

        final List<PortableIpRangeVO> ranges = new ArrayList<PortableIpRangeVO>();
        if (regionId != null) {
            final Region region = _regionDao.findById(regionId);
            if (region == null) {
                throw new InvalidParameterValueException("Invalid region ID: " + regionId);
            }
            return _portableIpRangeDao.listByRegionId(regionId);
        }

        if (rangeId != null) {
            final PortableIpRangeVO range = _portableIpRangeDao.findById(rangeId);
            if (range == null) {
                throw new InvalidParameterValueException("Invalid portable IP range ID: " + regionId);
            }
            ranges.add(range);
            return ranges;
        }

        return _portableIpRangeDao.listAll();
    }

    @Override
    public List<? extends PortableIp> listPortableIps(final long id) {

        final PortableIpRangeVO portableIpRange = _portableIpRangeDao.findById(id);
        if (portableIpRange == null) {
            throw new InvalidParameterValueException("Please specify a valid portable IP range id.");
        }

        return _portableIpDao.listByRangeId(portableIpRange.getId());
    }

    private boolean checkOverlapPortableIpRange(final int regionId, final String newStartIpStr, final String newEndIpStr) {
        final long newStartIp = NetUtils.ip2Long(newStartIpStr);
        final long newEndIp = NetUtils.ip2Long(newEndIpStr);

        final List<PortableIpRangeVO> existingPortableIPRanges = _portableIpRangeDao.listByRegionId(regionId);

        if (existingPortableIPRanges == null || existingPortableIPRanges.isEmpty()) {
            return false;
        }

        for (final PortableIpRangeVO portableIpRange : existingPortableIPRanges) {
            final String ipRangeStr = portableIpRange.getIpRange();
            final String[] range = ipRangeStr.split("-");
            final long startip = NetUtils.ip2Long(range[0]);
            final long endIp = NetUtils.ip2Long(range[1]);

            if (newStartIp >= startip && newStartIp <= endIp || newEndIp >= startip && newEndIp <= endIp) {
                return true;
            }

            if (startip >= newStartIp && startip <= newEndIp || endIp >= newStartIp && endIp <= newEndIp) {
                return true;
            }
        }
        return false;
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
                    if (_domainDao.isChildDomain(filteredDomainIds.get(i), filteredDomainIds.get(j))) {
                        filteredDomainIds.remove(j);
                        i--;
                    }
                    if (_domainDao.isChildDomain(filteredDomainIds.get(j), filteredDomainIds.get(i))) {
                        filteredDomainIds.remove(i);
                        break;
                    }
                }
            }
        }
        return filteredDomainIds;
    }

    protected void validateCacheMode(String cacheMode){
        if(cacheMode != null &&
                !Enums.getIfPresent(DiskOffering.DiskCacheMode.class,
                        cacheMode.toUpperCase()).isPresent()) {
            throw new InvalidParameterValueException(String.format("Invalid cache mode (%s). Please specify one of the following " +
                    "valid cache mode parameters: none, writeback or writethrough", cacheMode));
        }
    }

    public List<SecurityChecker> getSecChecker() {
        return _secChecker;
    }


    @Override
    public Boolean isAccountAllowedToCreateOfferingsWithTags(IsAccountAllowedToCreateOfferingsWithTagsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Account targetAccount = _accountMgr.getAccount(cmd.getId());
        _accountMgr.checkAccess(caller, null, true, targetAccount);
        return ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS.valueIn(cmd.getId());
    }

    @Inject
    public void setSecChecker(final List<SecurityChecker> secChecker) {
        _secChecker = secChecker;
    }

    @Override
    public String getConfigComponentName() {
        return ConfigurationManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {SystemVMUseLocalStorage, IOPS_MAX_READ_LENGTH, IOPS_MAX_WRITE_LENGTH,
                BYTES_MAX_READ_LENGTH, BYTES_MAX_WRITE_LENGTH, ADD_HOST_ON_SERVICE_RESTART_KVM, SET_HOST_DOWN_TO_MAINTENANCE, VM_SERVICE_OFFERING_MAX_CPU_CORES,
                VM_SERVICE_OFFERING_MAX_RAM_SIZE, VM_USERDATA_MAX_LENGTH, MIGRATE_VM_ACROSS_CLUSTERS,
                ENABLE_ACCOUNT_SETTINGS_FOR_DOMAIN, ENABLE_DOMAIN_SETTINGS_FOR_CHILD_DOMAIN, ALLOW_DOMAIN_ADMINS_TO_CREATE_TAGGED_OFFERINGS
        };
    }

    @Override
    public String getConfigurationType(final String configName) {
        final ConfigurationVO cfg = _configDao.findByName(configName);
        if (cfg == null) {
            s_logger.warn("Configuration " + configName + " not found");
            return Configuration.ValueType.String.name();
        }

        if (weightBasedParametersForValidation.contains(configName)) {
            return Configuration.ValueType.Range.name();
        }

        Class<?> type = null;
        final Config c = Config.getConfig(configName);
        if (c == null) {
            s_logger.warn("Configuration " + configName + " no found. Perhaps moved to ConfigDepot");
            final ConfigKey<?> configKey = _configDepot.get(configName);
            if (configKey == null) {
                s_logger.warn("Couldn't find configuration " + configName + " in ConfigDepot too.");
                return Configuration.ValueType.String.name();
            }
            type = configKey.type();
        } else {
            type = c.getType();
        }

        return getInputType(type, cfg);
    }

    private String getInputType(Class<?> type, ConfigurationVO cfg) {
        if (type == null) {
            return Configuration.ValueType.String.name();
        }

        if (type == String.class || type == Character.class) {
            if (cfg.getKind() == null) {
                return Configuration.ValueType.String.name();
            }
            return cfg.getKind();
        }
        if (type == Integer.class || type == Long.class || type == Short.class) {
            return Configuration.ValueType.Number.name();
        }
        if (type == Float.class || type == Double.class) {
            return Configuration.ValueType.Decimal.name();
        }
        if (type == Boolean.class) {
            return Configuration.ValueType.Boolean.name();
        }
        return Configuration.ValueType.String.name();
    }

    @Override
    public Pair<String, String> getConfigurationGroupAndSubGroup(final String configName) {
        if (StringUtils.isBlank(configName)) {
            throw new CloudRuntimeException("Empty configuration name provided");
        }

        final ConfigurationVO cfg = _configDao.findByName(configName);
        if (cfg == null) {
            s_logger.warn("Configuration " + configName + " not found");
            throw new InvalidParameterValueException("configuration with name " + configName + " doesn't exist");
        }

        String groupName = "Miscellaneous";
        String subGroupName = "Others";
        ConfigurationSubGroupVO configSubGroup = _configSubGroupDao.findById(cfg.getSubGroupId());
        if (configSubGroup != null) {
            subGroupName = configSubGroup.getName();
        }

        ConfigurationGroupVO configGroup = _configGroupDao.findById(cfg.getGroupId());
        if (configGroup != null) {
            groupName = configGroup.getName();
        }

        return new Pair<String, String>(groupName, subGroupName);
    }

    @Override
    public List<ConfigurationSubGroupVO> getConfigurationSubGroups(final Long groupId) {
        List<ConfigurationSubGroupVO> configSubGroups = _configSubGroupDao.findByGroup(groupId);
        return configSubGroups;
    }

    static class ParamCountPair {
        private Long id;
        private int paramCount;
        private String scope;

        public ParamCountPair(Long id, int paramCount, String scope) {
            this.id = id;
            this.paramCount = paramCount;
            this.scope = scope;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public int getParamCount() {
            return paramCount;
        }

        public void setParamCount(int paramCount) {
            this.paramCount = paramCount;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
