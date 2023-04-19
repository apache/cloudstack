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
package com.cloud.vm;

import static com.cloud.configuration.ConfigurationManagerImpl.VM_USERDATA_MAX_LENGTH;
import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.DeployVMCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMSSHKeyCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMUserDataCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.api.command.user.vm.SecurityGroupAction;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateDefaultNicForVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVmNicIpCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.api.command.user.vmgroup.CreateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.DeleteVMGroupCmd;
import org.apache.cloudstack.api.command.user.volume.ChangeOfferingForVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.snapshot.SnapshotHelper;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetVmDiskStatsAnswer;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.agent.api.GetVmNetworkStatsAnswer;
import com.cloud.agent.api.GetVmNetworkStatsCommand;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.RestoreVMSnapshotAnswer;
import com.cloud.agent.api.RestoreVMSnapshotCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmNetworkStatsEntry;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.deploy.PlannerHostReservationVO;
import com.cloud.deploy.dao.PlannerHostReservationDao;
import com.cloud.deployasis.UserVmDeployAsIsDetailVO;
import com.cloud.deployasis.dao.TemplateDeployAsIsDetailsDao;
import com.cloud.deployasis.dao.UserVmDeployAsIsDetailsDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.gpu.GPU;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resourcelimit.CheckedReservation;
import com.cloud.server.ManagementService;
import com.cloud.server.ResourceTag;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserData;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserDataDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.crypt.RSAHelper;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.UUIDManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicExtraDhcpOptionDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VmStatsDao;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class UserVmManagerImpl extends ManagerBase implements UserVmManager, VirtualMachineGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(UserVmManagerImpl.class);

    /**
     * The number of seconds to wait before timing out when trying to acquire a global lock.
     */
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3;

    private static final long GiB_TO_BYTES = 1024 * 1024 * 1024;

    @Inject
    private EntityManager _entityMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private VMTemplateZoneDao _templateZoneDao;
    @Inject
    private TemplateDataStoreDao _templateStoreDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private UserVmDao _vmDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private FirewallRulesDao _rulesDao;
    @Inject
    private LoadBalancerVMMapDao _loadBalancerVMMapDao;
    @Inject
    private PortForwardingRulesDao _portForwardingDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private GuestOSDao _guestOSDao;
    @Inject
    private HighAvailabilityManager _haMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private AccountService _accountService;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private SecurityGroupManager _securityGroupMgr;
    @Inject
    private NetworkOfferingDao _networkOfferingDao;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private InstanceGroupVMMapDao _groupVMMapDao;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private RulesManager _rulesMgr;
    @Inject
    private LoadBalancingRulesManager _lbMgr;
    @Inject
    private SSHKeyPairDao _sshKeyPairDao;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;
    @Inject
    private HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    private SecurityGroupDao _securityGroupDao;
    @Inject
    private CapacityManager _capacityMgr;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private FirewallManager _firewallMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    private PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    private VpcManager _vpcMgr;
    @Inject
    private TemplateManager _templateMgr;
    @Inject
    private GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    private UsageEventDao _usageEventDao;
    @Inject
    private VmDiskStatisticsDao _vmDiskStatsDao;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private VMSnapshotManager _vmSnapshotMgr;
    @Inject
    private AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Inject
    private AffinityGroupDao _affinityGroupDao;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    @Inject
    private AffinityGroupService _affinityGroupService;
    @Inject
    private PlannerHostReservationDao _plannerHostReservationDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private UserStatisticsDao _userStatsDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private VolumeService _volService;
    @Inject
    private VolumeDataFactory volFactory;
    @Inject
    private UUIDManager _uuidMgr;
    @Inject
    private DeploymentPlanningManager _planningMgr;
    @Inject
    private VolumeApiService _volumeService;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private VpcVirtualNetworkApplianceManager _virtualNetAppliance;
    @Inject
    private DomainRouterDao _routerDao;
    @Inject
    private VMNetworkMapDao _vmNetworkMapDao;
    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    private NicExtraDhcpOptionDao _nicExtraDhcpOptionDao;
    @Inject
    private TemplateApiService _tmplService;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DpdkHelper dpdkHelper;
    @Inject
    private ResourceTagDao resourceTagDao;
    @Inject
    private TemplateDeployAsIsDetailsDao templateDeployAsIsDetailsDao;
    @Inject
    private UserVmDeployAsIsDetailsDao userVmDeployAsIsDetailsDao;
    @Inject
    private DataStoreProviderManager _dataStoreProviderMgr;
    @Inject
    private StorageManager storageManager;
    @Inject
    private ServiceOfferingJoinDao serviceOfferingJoinDao;
    @Inject
    private BackupDao backupDao;
    @Inject
    private BackupManager backupManager;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private VmStatsDao vmStatsDao;
    @Inject
    private MessageBus messageBus;
    @Inject
    protected CommandSetupHelper commandSetupHelper;
    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper nwHelper;
    @Inject
    ReservationDao reservationDao;
    @Inject
    ResourceLimitService resourceLimitService;

    @Inject
    private StatsCollector statsCollector;
    @Inject
    private UserDataDao userDataDao;

    @Inject
    protected SnapshotHelper snapshotHelper;

    @Inject
    private AutoScaleManager autoScaleManager;

    private ScheduledExecutorService _executor = null;
    private ScheduledExecutorService _vmIpFetchExecutor = null;
    private int _expungeInterval;
    private int _expungeDelay;
    private boolean _dailyOrHourly = false;
    private int capacityReleaseInterval;
    private ExecutorService _vmIpFetchThreadExecutor;


    private String _instance;
    private boolean _instanceNameFlag;
    private int _scaleRetry;
    private Map<Long, VmAndCountDetails> vmIdCountMap = new ConcurrentHashMap<>();

    protected static long ROOT_DEVICE_ID = 0;

    private static final int MAX_HTTP_GET_LENGTH = 2 * MAX_USER_DATA_LENGTH_BYTES;
    private static final int NUM_OF_2K_BLOCKS = 512;
    private static final int MAX_HTTP_POST_LENGTH = NUM_OF_2K_BLOCKS * MAX_USER_DATA_LENGTH_BYTES;

    @Inject
    private OrchestrationService _orchSrvc;

    @Inject
    private VolumeOrchestrationService volumeMgr;

    @Inject
    private ManagementService _mgr;

    private static final ConfigKey<Integer> VmIpFetchWaitInterval = new ConfigKey<Integer>("Advanced", Integer.class, "externaldhcp.vmip.retrieval.interval", "180",
            "Wait Interval (in seconds) for shared network vm dhcp ip addr fetch for next iteration ", true);

    private static final ConfigKey<Integer> VmIpFetchTrialMax = new ConfigKey<Integer>("Advanced", Integer.class, "externaldhcp.vmip.max.retry", "10",
            "The max number of retrieval times for shared entwork vm dhcp ip fetch, in case of failures", true);

    private static final ConfigKey<Integer> VmIpFetchThreadPoolMax = new ConfigKey<Integer>("Advanced", Integer.class, "externaldhcp.vmipFetch.threadPool.max", "10",
            "number of threads for fetching vms ip address", true);

    private static final ConfigKey<Integer> VmIpFetchTaskWorkers = new ConfigKey<Integer>("Advanced", Integer.class, "externaldhcp.vmipfetchtask.workers", "10",
            "number of worker threads for vm ip fetch task ", true);

    private static final ConfigKey<Boolean> AllowDeployVmIfGivenHostFails = new ConfigKey<Boolean>("Advanced", Boolean.class, "allow.deploy.vm.if.deploy.on.given.host.fails", "false",
            "allow vm to deploy on different host if vm fails to deploy on the given host ", true);

    private static final ConfigKey<Boolean> EnableAdditionalVmConfig = new ConfigKey<>("Advanced", Boolean.class,
            "enable.additional.vm.configuration", "false", "allow additional arbitrary configuration to vm", true, ConfigKey.Scope.Account);

    private static final ConfigKey<String> KvmAdditionalConfigAllowList = new ConfigKey<>(String.class,
    "allow.additional.vm.configuration.list.kvm", "Advanced", "", "Comma separated list of allowed additional configuration options.", true, ConfigKey.Scope.Global, null, null, EnableAdditionalVmConfig.key(), null, null, ConfigKey.Kind.CSV, null);

    private static final ConfigKey<String> XenServerAdditionalConfigAllowList = new ConfigKey<>(String.class,
    "allow.additional.vm.configuration.list.xenserver", "Advanced", "", "Comma separated list of allowed additional configuration options", true, ConfigKey.Scope.Global, null, null, EnableAdditionalVmConfig.key(), null, null, ConfigKey.Kind.CSV, null);

    private static final ConfigKey<String> VmwareAdditionalConfigAllowList = new ConfigKey<>(String.class,
    "allow.additional.vm.configuration.list.vmware", "Advanced", "", "Comma separated list of allowed additional configuration options.", true, ConfigKey.Scope.Global, null, null, EnableAdditionalVmConfig.key(), null, null, ConfigKey.Kind.CSV, null);

    private static final ConfigKey<Boolean> VmDestroyForcestop = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.destroy.forcestop", "false",
            "On destroy, force-stop takes this value ", true);

    public static final List<HypervisorType> VM_STORAGE_MIGRATION_SUPPORTING_HYPERVISORS = new ArrayList<>(Arrays.asList(
            HypervisorType.KVM,
            HypervisorType.VMware,
            HypervisorType.XenServer,
            HypervisorType.Simulator
    ));

    private static final List<HypervisorType> HYPERVISORS_THAT_CAN_DO_STORAGE_MIGRATION_ON_NON_USER_VMS = Arrays.asList(HypervisorType.KVM, HypervisorType.VMware);

    @Override
    public UserVmVO getVirtualMachine(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public List<? extends UserVm> getVirtualMachines(long hostId) {
        return _vmDao.listByHostId(hostId);
    }

    private void resourceLimitCheck(Account owner, Boolean displayVm, Long cpu, Long memory) throws ResourceAllocationException {
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.user_vm, displayVm);
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.cpu, displayVm, cpu);
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.memory, displayVm, memory);
    }

    protected void resourceCountIncrement(long accountId, Boolean displayVm, Long cpu, Long memory) {
        if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.user_vm, displayVm);
            _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.cpu, displayVm, cpu);
            _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.memory, displayVm, memory);
        }
    }

    protected void resourceCountDecrement(long accountId, Boolean displayVm, Long cpu, Long memory) {
        if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.user_vm, displayVm);
            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.cpu, displayVm, cpu);
            _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.memory, displayVm, memory);
        }
    }

    public class VmAndCountDetails {
        long vmId;
        int  retrievalCount = VmIpFetchTrialMax.value();


        public VmAndCountDetails() {
        }

        public VmAndCountDetails (long vmId, int retrievalCount) {
            this.vmId = vmId;
            this.retrievalCount = retrievalCount;
        }

        public VmAndCountDetails (long vmId) {
            this.vmId = vmId;
        }

        public int getRetrievalCount() {
            return retrievalCount;
        }

        public void setRetrievalCount(int retrievalCount) {
            this.retrievalCount = retrievalCount;
        }

        public long getVmId() {
            return vmId;
        }

        public void setVmId(long vmId) {
            this.vmId = vmId;
        }

        public void decrementCount() {
            this.retrievalCount--;

        }
    }

    private class VmIpAddrFetchThread extends ManagedContextRunnable {


        long nicId;
        long vmId;
        String vmName;
        boolean isWindows;
        Long hostId;
        String networkCidr;

        public VmIpAddrFetchThread(long vmId, long nicId, String instanceName, boolean windows, Long hostId, String networkCidr) {
            this.vmId = vmId;
            this.nicId = nicId;
            this.vmName = instanceName;
            this.isWindows = windows;
            this.hostId = hostId;
            this.networkCidr = networkCidr;
        }

        @Override
        protected void runInContext() {
            GetVmIpAddressCommand cmd = new GetVmIpAddressCommand(vmName, networkCidr, isWindows);
            boolean decrementCount = true;

            try {
                s_logger.debug("Trying for vm "+ vmId +" nic Id "+nicId +" ip retrieval ...");
                Answer answer = _agentMgr.send(hostId, cmd);
                NicVO nic = _nicDao.findById(nicId);
                if (answer.getResult()) {
                    String vmIp = answer.getDetails();

                    if (NetUtils.isValidIp4(vmIp)) {
                        // set this vm ip addr in vm nic.
                        if (nic != null) {
                            nic.setIPv4Address(vmIp);
                            _nicDao.update(nicId, nic);
                            s_logger.debug("Vm "+ vmId +" IP "+vmIp +" got retrieved successfully");
                            vmIdCountMap.remove(nicId);
                            decrementCount = false;
                            ActionEventUtils.onActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
                                    Domain.ROOT_DOMAIN, EventTypes.EVENT_NETWORK_EXTERNAL_DHCP_VM_IPFETCH,
                                    "VM " + vmId + " nic id " + nicId + " ip address " + vmIp + " got fetched successfully", vmId, ApiCommandResourceType.VirtualMachine.toString());
                        }
                    }
                } else {
                    //previously vm has ip and nic table has ip address. After vm restart or stop/start
                    //if vm doesnot get the ip then set the ip in nic table to null
                    if (nic.getIPv4Address() != null) {
                        nic.setIPv4Address(null);
                        _nicDao.update(nicId, nic);
                    }
                    if (answer.getDetails() != null) {
                        s_logger.debug("Failed to get vm ip for Vm "+ vmId + answer.getDetails());
                    }
                }
            } catch (OperationTimedoutException e) {
                s_logger.warn("Timed Out", e);
            } catch (AgentUnavailableException e) {
                s_logger.warn("Agent Unavailable ", e);
            } finally {
                if (decrementCount) {
                    VmAndCountDetails vmAndCount = vmIdCountMap.get(nicId);
                    vmAndCount.decrementCount();
                    s_logger.debug("Ip is not retrieved for VM " + vmId +" nic "+nicId + " ... decremented count to "+vmAndCount.getRetrievalCount());
                    vmIdCountMap.put(nicId, vmAndCount);
                }
            }
        }
    }

    private void addVmUefiBootOptionsToParams(Map<VirtualMachineProfile.Param, Object> params, String bootType, String bootMode) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("Adding boot options (%s, %s, %s) into the param map for VM start as UEFI detail(%s=%s) found for the VM",
                    VirtualMachineProfile.Param.UefiFlag.getName(),
                    VirtualMachineProfile.Param.BootType.getName(),
                    VirtualMachineProfile.Param.BootMode.getName(),
                    bootType,
                    bootMode));
        }
        params.put(VirtualMachineProfile.Param.UefiFlag, "Yes");
        params.put(VirtualMachineProfile.Param.BootType, bootType);
        params.put(VirtualMachineProfile.Param.BootMode, bootMode);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETPASSWORD, eventDescription = "resetting Vm password", async = true)
    public UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        Account caller = CallContext.current().getCallingAccount();
        Long vmId = cmd.getId();
        UserVmVO userVm = _vmDao.findById(cmd.getId());

        // Do parameters input validation
        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + cmd.getId());
        }

        _vmDao.loadDetails(userVm);

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm.getTemplateId());
        if (template == null || !template.isEnablePassword()) {
            throw new InvalidParameterValueException("Fail to reset password for the virtual machine, the template is not password enabled");
        }

        if (userVm.getState() == State.Error || userVm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm with id " + vmId + " is not in the right state");
        }

        if (userVm.getState() != State.Stopped) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm " + userVm + " should be stopped to do password reset");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);

        boolean result = resetVMPasswordInternal(vmId, password);

        if (result) {
            userVm.setPassword(password);
        } else {
            throw new CloudRuntimeException("Failed to reset password for the virtual machine ");
        }

        return userVm;
    }

    private boolean resetVMPasswordInternal(Long vmId, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        Long userId = CallContext.current().getCallingUserId();
        VMInstanceVO vmInstance = _vmDao.findById(vmId);

        if (password == null || password.equals("")) {
            return false;
        }

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vmInstance.getTemplateId());
        if (template.isEnablePassword()) {
            Nic defaultNic = _networkModel.getDefaultNic(vmId);
            if (defaultNic == null) {
                s_logger.error("Unable to reset password for vm " + vmInstance + " as the instance doesn't have default nic");
                return false;
            }

            Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
            NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null, _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork),
                    _networkModel.getNetworkTag(template.getHypervisorType(), defaultNetwork));
            VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vmInstance);
            vmProfile.setParameter(VirtualMachineProfile.Param.VmPassword, password);

            UserDataServiceProvider element = _networkMgr.getPasswordResetProvider(defaultNetwork);
            if (element == null) {
                throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for password reset");
            }

            boolean result = element.savePassword(defaultNetwork, defaultNicProfile, vmProfile);

            // Need to reboot the virtual machine so that the password gets
            // redownloaded from the DomR, and reset on the VM
            if (!result) {
                s_logger.debug("Failed to reset password for the virtual machine; no need to reboot the vm");
                return false;
            } else {
                final UserVmVO userVm = _vmDao.findById(vmId);
                _vmDao.loadDetails(userVm);
                // update the password in vm_details table too
                // Check if an SSH key pair was selected for the instance and if so
                // use it to encrypt & save the vm password
                encryptAndStorePassword(userVm, password);

                if (vmInstance.getState() == State.Stopped) {
                    s_logger.debug("Vm " + vmInstance + " is stopped, not rebooting it as a part of password reset");
                    return true;
                }

                if (rebootVirtualMachine(userId, vmId, false, false) == null) {
                    s_logger.warn("Failed to reboot the vm " + vmInstance);
                    return false;
                } else {
                    s_logger.debug("Vm " + vmInstance + " is rebooted successfully as a part of password reset");
                    return true;
                }
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Reset password called for a vm that is not using a password enabled template");
            }
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETUSERDATA, eventDescription = "resetting VM userdata", async = true)
    public UserVm resetVMUserData(ResetVMUserDataCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException {

        Account caller = CallContext.current().getCallingAccount();
        Long vmId = cmd.getId();
        UserVmVO userVm = _vmDao.findById(cmd.getId());

        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine by id" + cmd.getId());
        }
        _accountMgr.checkAccess(caller, null, true, userVm);

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm.getTemplateId());

        // Do parameters input validation

        if (userVm.getState() != State.Stopped) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException(String.format("VM %s should be stopped to do UserData reset", userVm));
        }

        String userData = cmd.getUserData();
        Long userDataId = cmd.getUserdataId();
        String userDataDetails = null;
        if (MapUtils.isNotEmpty(cmd.getUserdataDetails())) {
            userDataDetails = cmd.getUserdataDetails().toString();
        }
        userData = finalizeUserData(userData, userDataId, template);
        userData = validateUserData(userData, cmd.getHttpMethod());

        userVm.setUserDataId(userDataId);
        userVm.setUserData(userData);
        userVm.setUserDataDetails(userDataDetails);
        _vmDao.update(userVm.getId(), userVm);

        updateUserData(userVm);

        UserVmVO vm = _vmDao.findById(vmId);
        _vmDao.loadDetails(vm);
        return vm;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETSSHKEY, eventDescription = "resetting Vm SSHKey", async = true)
    public UserVm resetVMSSHKey(ResetVMSSHKeyCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        Long vmId = cmd.getId();
        UserVmVO userVm = _vmDao.findById(cmd.getId());

        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine by id" + cmd.getId());
        }

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm.getTemplateId());

        // Do parameters input validation

        if (userVm.getState() == State.Error || userVm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm with specified id is not in the right state");
        }
        if (userVm.getState() != State.Stopped) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm " + userVm + " should be stopped to do SSH Key reset");
        }

        if (cmd.getNames() == null || cmd.getNames().isEmpty()) {
            throw new InvalidParameterValueException("'keypair' or 'keyparis' must be specified");
        }

        String keypairnames = "";
        String sshPublicKeys = "";
        List<SSHKeyPairVO> pairs = new ArrayList<>();

        pairs = _sshKeyPairDao.findByNames(owner.getAccountId(), owner.getDomainId(), cmd.getNames());
        if (pairs == null || pairs.size() != cmd.getNames().size()) {
            throw new InvalidParameterValueException("Not all specified keyparis exist");
        }
        sshPublicKeys = pairs.stream().map(p -> p.getPublicKey()).collect(Collectors.joining("\n"));
        keypairnames = String.join(",", cmd.getNames());

        _accountMgr.checkAccess(caller, null, true, userVm);

        boolean result = resetVMSSHKeyInternal(vmId, sshPublicKeys, keypairnames);

        UserVmVO vm = _vmDao.findById(vmId);
        _vmDao.loadDetails(vm);
        if (!result) {
            throw new CloudRuntimeException("Failed to reset SSH Key for the virtual machine ");
        }

        removeEncryptedPasswordFromUserVmVoDetails(vmId);

        _vmDao.loadDetails(userVm);
        return userVm;
    }

    protected void removeEncryptedPasswordFromUserVmVoDetails(long vmId) {
        userVmDetailsDao.removeDetail(vmId, VmDetailConstants.ENCRYPTED_PASSWORD);
    }

    private boolean resetVMSSHKeyInternal(Long vmId, String sshPublicKeys, String keypairnames) throws ResourceUnavailableException, InsufficientCapacityException {
        Long userId = CallContext.current().getCallingUserId();
        VMInstanceVO vmInstance = _vmDao.findById(vmId);

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vmInstance.getTemplateId());
        Nic defaultNic = _networkModel.getDefaultNic(vmId);
        if (defaultNic == null) {
            s_logger.error("Unable to reset SSH Key for vm " + vmInstance + " as the instance doesn't have default nic");
            return false;
        }

        Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
        NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null, _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork),
                _networkModel.getNetworkTag(template.getHypervisorType(), defaultNetwork));

        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vmInstance);

        UserDataServiceProvider element = _networkMgr.getSSHKeyResetProvider(defaultNetwork);
        if (element == null) {
            throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for SSH Key reset");
        }
        boolean result = element.saveSSHKey(defaultNetwork, defaultNicProfile, vmProfile, sshPublicKeys);
        // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
        if (!result) {
            s_logger.debug("Failed to reset SSH Key for the virtual machine; no need to reboot the vm");
            return false;
        } else {
            final UserVmVO userVm = _vmDao.findById(vmId);
            _vmDao.loadDetails(userVm);
            userVm.setDetail(VmDetailConstants.SSH_PUBLIC_KEY, sshPublicKeys);
            userVm.setDetail(VmDetailConstants.SSH_KEY_PAIR_NAMES, keypairnames);
            _vmDao.saveDetails(userVm);

            if (vmInstance.getState() == State.Stopped) {
                s_logger.debug("Vm " + vmInstance + " is stopped, not rebooting it as a part of SSH Key reset");
                return true;
            }
            if (rebootVirtualMachine(userId, vmId, false, false) == null) {
                s_logger.warn("Failed to reboot the vm " + vmInstance);
                return false;
            } else {
                s_logger.debug("Vm " + vmInstance + " is rebooted successfully as a part of SSH Key reset");
                return true;
            }
        }
    }

    @Override
    public boolean stopVirtualMachine(long userId, long vmId) {
        boolean status = false;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping vm=" + vmId);
        }
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is either removed or deleted.");
            }
            return true;
        }

        _userDao.findById(userId);
        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            status = vmEntity.stop(Long.toString(userId));
        } catch (ResourceUnavailableException e) {
            s_logger.debug("Unable to stop due to ", e);
            status = false;
        } catch (CloudException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        }
        return status;
    }

    private UserVm rebootVirtualMachine(long userId, long vmId, boolean enterSetup, boolean forced) throws InsufficientCapacityException, ResourceUnavailableException {
        UserVmVO vm = _vmDao.findById(vmId);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("reboot %s with enterSetup set to %s", vm.getInstanceName(), Boolean.toString(enterSetup)));
        }

        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            s_logger.warn("Vm id=" + vmId + " doesn't exist");
            return null;
        }

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            collectVmDiskAndNetworkStatistics(vm, State.Running);

            if (forced) {
                Host vmOnHost = _hostDao.findById(vm.getHostId());
                if (vmOnHost == null || vmOnHost.getResourceState() != ResourceState.Enabled || vmOnHost.getStatus() != Status.Up ) {
                    throw new CloudRuntimeException("Unable to force reboot the VM as the host: " + vm.getHostId() + " is not in the right state");
                }
                return forceRebootVirtualMachine(vmId, vm.getHostId(), enterSetup);
            }

            DataCenterVO dc = _dcDao.findById(vm.getDataCenterId());
            try {
                if (dc.getNetworkType() == DataCenter.NetworkType.Advanced) {
                    //List all networks of vm
                    List<Long> vmNetworks = _vmNetworkMapDao.getNetworks(vmId);
                    List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
                    //List the stopped routers
                    for(long vmNetworkId : vmNetworks) {
                        List<DomainRouterVO> router = _routerDao.listStopped(vmNetworkId);
                        routers.addAll(router);
                    }
                    //A vm may not have many nics attached and even fewer routers might be stopped (only in exceptional cases)
                    //Safe to start the stopped router serially, this is consistent with the way how multiple networks are added to vm during deploy
                    //and routers are started serially ,may revisit to make this process parallel
                    for(DomainRouterVO routerToStart : routers) {
                        s_logger.warn("Trying to start router " + routerToStart.getInstanceName() + " as part of vm: " + vm.getInstanceName() + " reboot");
                        _virtualNetAppliance.startRouter(routerToStart.getId(),true);
                    }
                }
            } catch (ConcurrentOperationException e) {
                throw new CloudRuntimeException("Concurrent operations on starting router. " + e);
            } catch (Exception ex){
                throw new CloudRuntimeException("Router start failed due to" + ex);
            } finally {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info(String.format("Rebooting vm %s%s.", vm.getInstanceName(), enterSetup? " entering hardware setup menu" : " as is"));
                }
                Map<VirtualMachineProfile.Param,Object> params = null;
                if (enterSetup) {
                    params = new HashMap();
                    params.put(VirtualMachineProfile.Param.BootIntoSetup, Boolean.TRUE);
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace(String.format("Adding %s to paramlist", VirtualMachineProfile.Param.BootIntoSetup));
                    }
                }
                _itMgr.reboot(vm.getUuid(), params);
            }
            return _vmDao.findById(vmId);
        } else {
            s_logger.error("Vm id=" + vmId + " is not in Running state, failed to reboot");
            return null;
        }
    }

    private UserVm forceRebootVirtualMachine(long vmId, long hostId, boolean enterSetup) {
        try {
            if (stopVirtualMachine(vmId, false) != null) {
                Map<VirtualMachineProfile.Param,Object> params = null;
                if (enterSetup) {
                    params = new HashMap();
                    params.put(VirtualMachineProfile.Param.BootIntoSetup, Boolean.TRUE);
                }
                return startVirtualMachine(vmId, null, null, hostId, params, null, false).first();
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to reboot the VM: " + vmId, e);
        } catch (CloudException e) {
            throw new CloudRuntimeException("Unable to reboot the VM: " + vmId, e);
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPGRADE, eventDescription = "upgrading Vm")
    /*
     * TODO: cleanup eventually - Refactored API call
     */
    // This method will be deprecated as we use ScaleVMCmd for both stopped VMs and running VMs
    public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) throws ResourceAllocationException {
        Long vmId = cmd.getId();
        Long svcOffId = cmd.getServiceOfferingId();
        Account caller = CallContext.current().getCallingAccount();

        // Verify input parameters
        //UserVmVO vmInstance = _vmDao.findById(vmId);
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        } else if (!(vmInstance.getState().equals(State.Stopped))) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " " + " in state " + vmInstance.getState()
            + "; make sure the virtual machine is stopped");
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        upgradeStoppedVirtualMachine(vmId, svcOffId, cmd.getDetails());

        // Generate usage event for VM upgrade
        UserVmVO userVm = _vmDao.findById(vmId);
        generateUsageEvent( userVm, userVm.isDisplayVm(), EventTypes.EVENT_VM_UPGRADE);

        return userVm;
    }

    private void validateOfferingMaxResource(ServiceOfferingVO offering) {
        Integer maxCPUCores = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.value() == 0 ? Integer.MAX_VALUE: ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.value();
        if (offering.getCpu() > maxCPUCores) {
            throw new InvalidParameterValueException("Invalid cpu cores value, please choose another service offering with cpu cores between 1 and " + maxCPUCores);
        }
        Integer maxRAMSize = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.value() == 0 ? Integer.MAX_VALUE: ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.value();
        if (offering.getRamSize() > maxRAMSize) {
            throw new InvalidParameterValueException("Invalid memory value, please choose another service offering with memory between 32 and " + maxRAMSize + " MB");
        }
    }

    @Override
    public void validateCustomParameters(ServiceOfferingVO serviceOffering, Map<String, String> customParameters) {
        //TODO need to validate custom cpu, and memory against min/max CPU/Memory ranges from service_offering_details table
        if (customParameters.size() != 0) {
            Map<String, String> offeringDetails = serviceOfferingDetailsDao.listDetailsKeyPairs(serviceOffering.getId());
            if (serviceOffering.getCpu() == null) {
                int minCPU = NumbersUtil.parseInt(offeringDetails.get(ApiConstants.MIN_CPU_NUMBER), 1);
                int maxCPU = NumbersUtil.parseInt(offeringDetails.get(ApiConstants.MAX_CPU_NUMBER), Integer.MAX_VALUE);
                int cpuNumber = NumbersUtil.parseInt(customParameters.get(UsageEventVO.DynamicParameters.cpuNumber.name()), -1);
                Integer maxCPUCores = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.value() == 0 ? Integer.MAX_VALUE: ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.value();
                if (cpuNumber < minCPU || cpuNumber > maxCPU || cpuNumber > maxCPUCores) {
                    throw new InvalidParameterValueException(String.format("Invalid cpu cores value, specify a value between %d and %d", minCPU, Math.min(maxCPUCores, maxCPU)));
                }
            } else if (customParameters.containsKey(UsageEventVO.DynamicParameters.cpuNumber.name())) {
                throw new InvalidParameterValueException("The cpu cores of this offering id:" + serviceOffering.getUuid()
                + " is not customizable. This is predefined in the template.");
            }

            if (serviceOffering.getSpeed() == null) {
                String cpuSpeed = customParameters.get(UsageEventVO.DynamicParameters.cpuSpeed.name());
                if ((cpuSpeed == null) || (NumbersUtil.parseInt(cpuSpeed, -1) <= 0)) {
                    throw new InvalidParameterValueException("Invalid cpu speed value, specify a value between 1 and " + Integer.MAX_VALUE);
                }
            } else if (!serviceOffering.isCustomCpuSpeedSupported() && customParameters.containsKey(UsageEventVO.DynamicParameters.cpuSpeed.name())) {
                throw new InvalidParameterValueException("The cpu speed of this offering id:" + serviceOffering.getUuid()
                + " is not customizable. This is predefined in the template.");
            }

            if (serviceOffering.getRamSize() == null) {
                int minMemory = NumbersUtil.parseInt(offeringDetails.get(ApiConstants.MIN_MEMORY), 32);
                int maxMemory = NumbersUtil.parseInt(offeringDetails.get(ApiConstants.MAX_MEMORY), Integer.MAX_VALUE);
                int memory = NumbersUtil.parseInt(customParameters.get(UsageEventVO.DynamicParameters.memory.name()), -1);
                Integer maxRAMSize = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.value() == 0 ? Integer.MAX_VALUE: ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.value();
                if (memory < minMemory || memory > maxMemory || memory > maxRAMSize) {
                    throw new InvalidParameterValueException(String.format("Invalid memory value, specify a value between %d and %d", minMemory, Math.min(maxRAMSize, maxMemory)));
                }
            } else if (customParameters.containsKey(UsageEventVO.DynamicParameters.memory.name())) {
                throw new InvalidParameterValueException("The memory of this offering id:" + serviceOffering.getUuid() + " is not customizable. This is predefined in the template.");
            }
        } else {
            throw new InvalidParameterValueException("Need to specify custom parameter values cpu, cpu speed and memory when using custom offering");
        }
    }

    private UserVm upgradeStoppedVirtualMachine(Long vmId, Long svcOffId, Map<String, String> customParameters) throws ResourceAllocationException {

        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        // Check resource limits for CPU and Memory.
        ServiceOfferingVO newServiceOffering = serviceOfferingDao.findById(svcOffId);
        if (newServiceOffering.getState() == ServiceOffering.State.Inactive) {
            throw new InvalidParameterValueException(String.format("Unable to upgrade virtual machine %s with an inactive service offering %s", vmInstance.getUuid(), newServiceOffering.getUuid()));
        }
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            validateCustomParameters(newServiceOffering, customParameters);
            newServiceOffering = serviceOfferingDao.getComputeOffering(newServiceOffering, customParameters);
        } else {
            validateOfferingMaxResource(newServiceOffering);
        }
        ServiceOfferingVO currentServiceOffering = serviceOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());

        validateDiskOfferingChecks(currentServiceOffering, newServiceOffering);

        int newCpu = newServiceOffering.getCpu();
        int newMemory = newServiceOffering.getRamSize();
        int currentCpu = currentServiceOffering.getCpu();
        int currentMemory = currentServiceOffering.getRamSize();

        Account owner = _accountMgr.getActiveAccountById(vmInstance.getAccountId());
        if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            if (newCpu > currentCpu) {
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.cpu, newCpu - currentCpu);
            }
            if (newMemory > currentMemory) {
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.memory, newMemory - currentMemory);
            }
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, newServiceOffering);

        // Check if the new service offering can be applied to vm instance
        _accountMgr.checkAccess(owner, newServiceOffering, _dcDao.findById(vmInstance.getDataCenterId()));

        // resize and migrate the root volume if required
        DiskOfferingVO newDiskOffering = _diskOfferingDao.findById(newServiceOffering.getDiskOfferingId());
        changeDiskOfferingForRootVolume(vmId, newDiskOffering, customParameters, vmInstance.getDataCenterId());

        _itMgr.upgradeVmDb(vmId, newServiceOffering, currentServiceOffering);

        // Increment or decrement CPU and Memory count accordingly.
        if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            if (newCpu > currentCpu) {
                _resourceLimitMgr.incrementResourceCount(owner.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
            } else if (currentCpu > newCpu) {
                _resourceLimitMgr.decrementResourceCount(owner.getAccountId(), ResourceType.cpu, new Long(currentCpu - newCpu));
            }
            if (newMemory > currentMemory) {
                _resourceLimitMgr.incrementResourceCount(owner.getAccountId(), ResourceType.memory, new Long(newMemory - currentMemory));
            } else if (currentMemory > newMemory) {
                _resourceLimitMgr.decrementResourceCount(owner.getAccountId(), ResourceType.memory, new Long(currentMemory - newMemory));
            }
        }

        return _vmDao.findById(vmInstance.getId());

    }

    /**
     * Prepares the Resize Volume Command and verifies if the disk offering from the new service offering can be resized.
     * <br>
     * If the Service Offering was configured with a root disk size (size > 0) then it can only resize to an offering with a larger disk
     * or to an offering with a root size of zero, which is the default behavior.
     */
    protected ResizeVolumeCmd prepareResizeVolumeCmd(VolumeVO rootVolume, DiskOfferingVO currentRootDiskOffering, DiskOfferingVO newRootDiskOffering) {
        if (rootVolume == null) {
            throw new InvalidParameterValueException("Could not find Root volume for the VM while preparing the Resize Volume Command.");
        }
        if (currentRootDiskOffering == null) {
            throw new InvalidParameterValueException("Could not find Disk Offering matching the provided current Root Offering ID.");
        }
        if (newRootDiskOffering == null) {
            throw new InvalidParameterValueException("Could not find Disk Offering matching the provided Offering ID for resizing Root volume.");
        }

        ResizeVolumeCmd resizeVolumeCmd = new ResizeVolumeCmd(rootVolume.getId(), newRootDiskOffering.getMinIops(), newRootDiskOffering.getMaxIops());

        long newNewOfferingRootSizeInBytes = newRootDiskOffering.getDiskSize();
        long newNewOfferingRootSizeInGiB = newNewOfferingRootSizeInBytes / GiB_TO_BYTES;
        long currentRootDiskOfferingGiB = currentRootDiskOffering.getDiskSize() / GiB_TO_BYTES;
        if (newNewOfferingRootSizeInBytes > currentRootDiskOffering.getDiskSize()) {
            resizeVolumeCmd = new ResizeVolumeCmd(rootVolume.getId(), newRootDiskOffering.getMinIops(), newRootDiskOffering.getMaxIops(), newRootDiskOffering.getId());
            s_logger.debug(String.format("Preparing command to resize VM Root disk from %d GB to %d GB; current offering: %s, new offering: %s.", currentRootDiskOfferingGiB,
                    newNewOfferingRootSizeInGiB, currentRootDiskOffering.getName(), newRootDiskOffering.getName()));
        } else if (newNewOfferingRootSizeInBytes > 0l && newNewOfferingRootSizeInBytes < currentRootDiskOffering.getDiskSize()) {
            throw new InvalidParameterValueException(String.format(
                    "Failed to resize Root volume. The new Service Offering [id: %d, name: %s] has a smaller disk size [%d GB] than the current disk [%d GB].",
                    newRootDiskOffering.getId(), newRootDiskOffering.getName(), newNewOfferingRootSizeInGiB, currentRootDiskOfferingGiB));
        }
        return resizeVolumeCmd;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_CREATE, eventDescription = "Creating Nic", async = true)
    public UserVm addNicToVirtualMachine(AddNicToVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long networkId = cmd.getNetworkId();
        String ipAddress = cmd.getIpAddress();
        String macAddress = cmd.getMacAddress();
        Account caller = CallContext.current().getCallingAccount();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("NIC cannot be added to VM with VM Snapshots");
        }

        NetworkVO network = _networkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("unable to find a network with id " + networkId);
        }

        Account vmOwner = _accountMgr.getAccount(vmInstance.getAccountId());
        _networkModel.checkNetworkPermissions(vmOwner, network);

        checkIfNetExistsForVM(vmInstance, network);

        macAddress = validateOrReplaceMacAddress(macAddress, network.getId());

        if(_nicDao.findByNetworkIdAndMacAddress(networkId, macAddress) != null) {
            throw new CloudRuntimeException("A NIC with this MAC address exists for network: " + network.getUuid());
        }

        NicProfile profile = new NicProfile(ipAddress, null, macAddress);
        if (ipAddress != null) {
            if (!(NetUtils.isValidIp4(ipAddress) || NetUtils.isValidIp6(ipAddress))) {
                throw new InvalidParameterValueException("Invalid format for IP address parameter: " + ipAddress);
            }
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new CloudRuntimeException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't add a new NIC to a VM on a Basic Network");
        }

        //ensure network belongs in zone
        if (network.getDataCenterId() != vmInstance.getDataCenterId()) {
            throw new CloudRuntimeException(vmInstance + " is in zone:" + vmInstance.getDataCenterId() + " but " + network + " is in zone:" + network.getDataCenterId());
        }

        if(_networkModel.getNicInNetwork(vmInstance.getId(),network.getId()) != null){
            s_logger.debug("VM " + vmInstance.getHostName() + " already in network " + network.getName() + " going to add another NIC");
        } else {
            //* get all vms hostNames in the network
            List<String> hostNames = _vmInstanceDao.listDistinctHostNames(network.getId());
            //* verify that there are no duplicates
            if (hostNames.contains(vmInstance.getHostName())) {
                throw new CloudRuntimeException("Network " + network.getName() + " already has a vm with host name: " + vmInstance.getHostName());
            }
        }

        NicProfile guestNic = null;
        boolean cleanUp = true;

        try {
            guestNic = _itMgr.addVmToNetwork(vmInstance, network, profile);
            saveExtraDhcpOptions(guestNic.getId(), cmd.getDhcpOptionsMap());
            _networkMgr.configureExtraDhcpOptions(network, guestNic.getId(), cmd.getDhcpOptionsMap());
            cleanUp = false;
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to add NIC to " + vmInstance + ": " + e);
        } catch (InsufficientCapacityException e) {
            throw new CloudRuntimeException("Insufficient capacity when adding NIC to " + vmInstance + ": " + e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operations on adding NIC to " + vmInstance + ": " + e);
        } finally {
            if(cleanUp) {
                try {
                    _itMgr.removeVmFromNetwork(vmInstance, network, null);
                } catch (ResourceUnavailableException e) {
                    throw new CloudRuntimeException("Error while cleaning up NIC " + e);
                }
            }
        }
        CallContext.current().putContextParameter(Nic.class, guestNic.getUuid());
        s_logger.debug(String.format("Successful addition of %s from %s through %s", network, vmInstance, guestNic));
        return _vmDao.findById(vmInstance.getId());
    }

    /**
     * duplicated in {@see VirtualMachineManagerImpl} for a {@see VMInstanceVO}
     */
    private void checkIfNetExistsForVM(VirtualMachine virtualMachine, Network network) {
        List<NicVO> allNics = _nicDao.listByVmId(virtualMachine.getId());
        for (NicVO nic : allNics) {
            if (nic.getNetworkId() == network.getId()) {
                throw new CloudRuntimeException("A NIC already exists for VM:" + virtualMachine.getInstanceName() + " in network: " + network.getUuid());
            }
        }
    }

    /**
     * If the given MAC address is invalid it replaces the given MAC with the next available MAC address
     */
    protected String validateOrReplaceMacAddress(String macAddress, long networkId) {
        if (!NetUtils.isValidMac(macAddress)) {
            try {
                macAddress = _networkModel.getNextAvailableMacAddressInNetwork(networkId);
            } catch (InsufficientAddressCapacityException e) {
                throw new CloudRuntimeException(String.format("A MAC address cannot be generated for this NIC in the network [id=%s] ", networkId));
            }
        }
        return macAddress;
    }

    private void saveExtraDhcpOptions(long nicId, Map<Integer, String> dhcpOptions) {
        List<NicExtraDhcpOptionVO> nicExtraDhcpOptionVOList = dhcpOptions
                .entrySet()
                .stream()
                .map(entry -> new NicExtraDhcpOptionVO(nicId, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        _nicExtraDhcpOptionDao.saveExtraDhcpOptions(nicExtraDhcpOptionVOList);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_DELETE, eventDescription = "Removing Nic", async = true)
    public UserVm removeNicFromVirtualMachine(RemoveNicFromVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long nicId = cmd.getNicId();
        Account caller = CallContext.current().getCallingAccount();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("NIC cannot be removed from VM with VM Snapshots");
        }

        NicVO nic = _nicDao.findById(nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Unable to find a nic with id " + nicId);
        }

        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find a network with id " + nic.getNetworkId());
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new InvalidParameterValueException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't remove a NIC from a VM on a Basic Network");
        }

        // check to see if nic is attached to VM
        if (nic.getInstanceId() != vmId) {
            throw new InvalidParameterValueException(nic + " is not a nic on " + vmInstance);
        }

        // don't delete default NIC on a user VM
        if (nic.isDefaultNic() && vmInstance.getType() == VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Unable to remove nic from " + vmInstance + " in " + network + ", nic is default.");
        }

        // if specified nic is associated with PF/LB/Static NAT
        if (_rulesMgr.listAssociatedRulesForGuestNic(nic).size() > 0) {
            throw new InvalidParameterValueException("Unable to remove nic from " + vmInstance + " in " + network + ", nic has associated Port forwarding or Load balancer or Static NAT rules.");
        }

        boolean nicremoved = false;
        try {
            nicremoved = _itMgr.removeNicFromVm(vmInstance, nic);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to remove " + network + " from " + vmInstance + ": " + e);

        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operations on removing " + network + " from " + vmInstance + ": " + e);
        }

        if (!nicremoved) {
            throw new CloudRuntimeException("Unable to remove " + network + " from " + vmInstance);
        }

        s_logger.debug("Successful removal of " + network + " from " + vmInstance);
        return _vmDao.findById(vmInstance.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_UPDATE, eventDescription = "Creating Nic", async = true)
    public UserVm updateDefaultNicForVirtualMachine(UpdateDefaultNicForVMCmd cmd) throws InvalidParameterValueException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long nicId = cmd.getNicId();
        Account caller = CallContext.current().getCallingAccount();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("NIC cannot be updated for VM with VM Snapshots");
        }

        NicVO nic = _nicDao.findById(nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("unable to find a nic with id " + nicId);
        }
        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("unable to find a network with id " + nic.getNetworkId());
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new CloudRuntimeException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't change default NIC on a Basic Network");
        }

        // no need to check permissions for network, we'll enumerate the ones they already have access to
        Network existingdefaultnet = _networkModel.getDefaultNetworkForVm(vmId);

        //check to see if nic is attached to VM
        if (nic.getInstanceId() != vmId) {
            throw new InvalidParameterValueException(nic + " is not a nic on  " + vmInstance);
        }
        // if current default equals chosen new default, Throw an exception
        if (nic.isDefaultNic()) {
            throw new CloudRuntimeException("refusing to set default nic because chosen nic is already the default");
        }

        //make sure the VM is Running or Stopped
        if ((vmInstance.getState() != State.Running) && (vmInstance.getState() != State.Stopped)) {
            throw new CloudRuntimeException("refusing to set default " + vmInstance + " is not Running or Stopped");
        }

        NicProfile existing = null;
        List<NicProfile> nicProfiles = _networkMgr.getNicProfiles(vmInstance);
        for (NicProfile nicProfile : nicProfiles) {
            if (nicProfile.isDefaultNic() && existingdefaultnet != null && nicProfile.getNetworkId() == existingdefaultnet.getId()) {
                existing = nicProfile;
            }
        }

        if (existing == null) {
            s_logger.warn("Failed to update default nic, no nic profile found for existing default network");
            throw new CloudRuntimeException("Failed to find a nic profile for the existing default network. This is bad and probably means some sort of configuration corruption");
        }

        Network oldDefaultNetwork = null;
        oldDefaultNetwork = _networkModel.getDefaultNetworkForVm(vmId);
        String oldNicIdString = Long.toString(_networkModel.getDefaultNic(vmId).getId());
        long oldNetworkOfferingId = -1L;

        if (oldDefaultNetwork != null) {
            oldNetworkOfferingId = oldDefaultNetwork.getNetworkOfferingId();
        }
        NicVO existingVO = _nicDao.findById(existing.id);
        Integer chosenID = nic.getDeviceId();
        Integer existingID = existing.getDeviceId();

        Network newdefault = null;
        if (_itMgr.updateDefaultNicForVM(vmInstance, nic, existingVO)) {
            newdefault = _networkModel.getDefaultNetworkForVm(vmId);
        }

        if (newdefault == null) {
            nic.setDefaultNic(false);
            nic.setDeviceId(chosenID);
            existingVO.setDefaultNic(true);
            existingVO.setDeviceId(existingID);

            nic = _nicDao.persist(nic);
            _nicDao.persist(existingVO);

            newdefault = _networkModel.getDefaultNetworkForVm(vmId);
            if (newdefault.getId() == existingdefaultnet.getId()) {
                throw new CloudRuntimeException("Setting a default nic failed, and we had no default nic, but we were able to set it back to the original");
            }
            throw new CloudRuntimeException("Failed to change default nic to " + nic + " and now we have no default");
        } else if (newdefault.getId() == nic.getNetworkId()) {
            s_logger.debug("successfully set default network to " + network + " for " + vmInstance);
            String nicIdString = Long.toString(nic.getId());
            long newNetworkOfferingId = network.getNetworkOfferingId();
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vmInstance.getAccountId(), vmInstance.getDataCenterId(), vmInstance.getId(),
                    oldNicIdString, oldNetworkOfferingId, null, 1L, VirtualMachine.class.getName(), vmInstance.getUuid(), vmInstance.isDisplay());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vmInstance.getAccountId(), vmInstance.getDataCenterId(), vmInstance.getId(), nicIdString,
                    newNetworkOfferingId, null, 1L, VirtualMachine.class.getName(), vmInstance.getUuid(), vmInstance.isDisplay());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vmInstance.getAccountId(), vmInstance.getDataCenterId(), vmInstance.getId(), nicIdString,
                    newNetworkOfferingId, null, 0L, VirtualMachine.class.getName(), vmInstance.getUuid(), vmInstance.isDisplay());
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vmInstance.getAccountId(), vmInstance.getDataCenterId(), vmInstance.getId(),
                    oldNicIdString, oldNetworkOfferingId, null, 0L, VirtualMachine.class.getName(), vmInstance.getUuid(), vmInstance.isDisplay());

            if (vmInstance.getState() == State.Running) {
                try {
                    VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vmInstance);
                    User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
                    ReservationContext context = new ReservationContextImpl(null, null, callerUser, caller);
                    DeployDestination dest = new DeployDestination(dc, null, null, null);
                    _networkMgr.prepare(vmProfile, dest, context);
                } catch (final Exception e) {
                    s_logger.info("Got exception: ", e);
                }
            }

            return _vmDao.findById(vmInstance.getId());
        }

        throw new CloudRuntimeException("something strange happened, new default network(" + newdefault.getId() + ") is not null, and is not equal to the network("
                + nic.getNetworkId() + ") of the chosen nic");
    }

    @Override
    public UserVm updateNicIpForVirtualMachine(UpdateVmNicIpCmd cmd) {
        Long nicId = cmd.getNicId();
        String ipaddr = cmd.getIpaddress();
        Account caller = CallContext.current().getCallingAccount();

        //check whether the nic belongs to user vm.
        NicVO nicVO = _nicDao.findById(nicId);
        if (nicVO == null) {
            throw new InvalidParameterValueException("There is no nic for the " + nicId);
        }

        if (nicVO.getVmType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("The nic is not belongs to user vm");
        }

        UserVm vm = _vmDao.findById(nicVO.getInstanceId());
        if (vm == null) {
            throw new InvalidParameterValueException("There is no vm with the nic");
        }

        Network network = _networkDao.findById(nicVO.getNetworkId());
        if (network == null) {
            throw new InvalidParameterValueException("There is no network with the nic");
        }
        // Don't allow to update vm nic ip if network is not in Implemented/Setup/Allocated state
        if (!(network.getState() == Network.State.Allocated || network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup)) {
            throw new InvalidParameterValueException("Network is not in the right state to update vm nic ip. Correct states are: " + Network.State.Allocated + ", " + Network.State.Implemented + ", "
                    + Network.State.Setup);
        }

        NetworkOfferingVO offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (offering == null) {
            throw new InvalidParameterValueException("There is no network offering with the network");
        }
        if (!_networkModel.listNetworkOfferingServices(offering.getId()).isEmpty() && vm.getState() != State.Stopped) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "VM is not Stopped, unable to update the vm nic having the specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        // verify permissions
        _accountMgr.checkAccess(caller, null, true, vm);
        Account ipOwner = _accountDao.findByIdIncludingRemoved(vm.getAccountId());

        // verify ip address
        s_logger.debug("Calling the ip allocation ...");
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (dc == null) {
            throw new InvalidParameterValueException("There is no dc with the nic");
        }
        if (dc.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Isolated) {
            try {
                ipaddr = _ipAddrMgr.allocateGuestIP(network, ipaddr);
            } catch (InsufficientAddressCapacityException e) {
                throw new InvalidParameterValueException("Allocating ip to guest nic " + nicVO.getUuid() + " failed, for insufficient address capacity");
            }
            if (ipaddr == null) {
                throw new InvalidParameterValueException("Allocating ip to guest nic " + nicVO.getUuid() + " failed, please choose another ip");
            }

            if (nicVO.getIPv4Address() != null) {
                updatePublicIpDnatVmIp(vm.getId(), network.getId(), nicVO.getIPv4Address(), ipaddr);
                updateLoadBalancerRulesVmIp(vm.getId(), network.getId(), nicVO.getIPv4Address(), ipaddr);
                updatePortForwardingRulesVmIp(vm.getId(), network.getId(), nicVO.getIPv4Address(), ipaddr);
            }

        } else if (dc.getNetworkType() == NetworkType.Basic || network.getGuestType()  == Network.GuestType.Shared) {
            //handle the basic networks here
            //for basic zone, need to provide the podId to ensure proper ip alloation
            Long podId = null;
            if (dc.getNetworkType() == NetworkType.Basic) {
                podId = vm.getPodIdToDeployIn();
                if (podId == null) {
                    throw new InvalidParameterValueException("vm pod id is null in Basic zone; can't decide the range for ip allocation");
                }
            }

            try {
                ipaddr = _ipAddrMgr.allocatePublicIpForGuestNic(network, podId, ipOwner, ipaddr);
                if (ipaddr == null) {
                    throw new InvalidParameterValueException("Allocating ip to guest nic " + nicVO.getUuid() + " failed, please choose another ip");
                }

                final IPAddressVO newIp = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), ipaddr);
                final Vlan vlan = _vlanDao.findById(newIp.getVlanId());
                nicVO.setIPv4Gateway(vlan.getVlanGateway());
                nicVO.setIPv4Netmask(vlan.getVlanNetmask());

                final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nicVO.getNetworkId(), nicVO.getIPv4Address());
                if (ip != null) {
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            _ipAddrMgr.markIpAsUnavailable(ip.getId());
                            _ipAddressDao.unassignIpAddress(ip.getId());
                        }
                    });
                }
            } catch (InsufficientAddressCapacityException e) {
                s_logger.error("Allocating ip to guest nic " + nicVO.getUuid() + " failed, for insufficient address capacity");
                return null;
            }
        } else {
            throw new InvalidParameterValueException("UpdateVmNicIpCmd is not supported in L2 network");
        }

        s_logger.debug("Updating IPv4 address of NIC " + nicVO + " to " + ipaddr + "/" + nicVO.getIPv4Netmask() + " with gateway " + nicVO.getIPv4Gateway());
        nicVO.setIPv4Address(ipaddr);
        _nicDao.persist(nicVO);

        return vm;
    }

    private void updatePublicIpDnatVmIp(long vmId, long networkId, String oldIp, String newIp) {
        if (!_networkModel.areServicesSupportedInNetwork(networkId, Service.StaticNat)) {
            return;
        }
        List<IPAddressVO> publicIps = _ipAddressDao.listByAssociatedVmId(vmId);
        for (IPAddressVO publicIp : publicIps) {
            if (oldIp.equals(publicIp.getVmIp()) && publicIp.getAssociatedWithNetworkId() == networkId) {
                publicIp.setVmIp(newIp);
                _ipAddressDao.persist(publicIp);
            }
        }
    }

    private void updateLoadBalancerRulesVmIp(long vmId, long networkId, String oldIp, String newIp) {
        if (!_networkModel.areServicesSupportedInNetwork(networkId, Service.Lb)) {
            return;
        }
        List<LoadBalancerVMMapVO> loadBalancerVMMaps = _loadBalancerVMMapDao.listByInstanceId(vmId);
        for (LoadBalancerVMMapVO map : loadBalancerVMMaps) {
            long lbId = map.getLoadBalancerId();
            FirewallRuleVO rule = _rulesDao.findById(lbId);
            if (oldIp.equals(map.getInstanceIp()) && networkId == rule.getNetworkId()) {
                map.setInstanceIp(newIp);
                _loadBalancerVMMapDao.persist(map);
            }
        }
    }

    private void updatePortForwardingRulesVmIp(long vmId, long networkId, String oldIp, String newIp) {
        if (!_networkModel.areServicesSupportedInNetwork(networkId, Service.PortForwarding)) {
            return;
        }
        List<PortForwardingRuleVO> firewallRules = _portForwardingDao.listByVm(vmId);
        for (PortForwardingRuleVO firewallRule : firewallRules) {
            FirewallRuleVO rule = _rulesDao.findById(firewallRule.getId());
            if (oldIp.equals(firewallRule.getDestinationIpAddress().toString()) && networkId == rule.getNetworkId()) {
                firewallRule.setDestinationIpAddress(new Ip(newIp));
                _portForwardingDao.persist(firewallRule);
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPGRADE, eventDescription = "Upgrading VM", async = true)
    public UserVm upgradeVirtualMachine(ScaleVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
    VirtualMachineMigrationException {

        Long vmId = cmd.getId();
        Long newServiceOfferingId = cmd.getServiceOfferingId();
        VirtualMachine vm = (VirtualMachine) this._entityMgr.findById(VirtualMachine.class, vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find VM's UUID");
        }
        CallContext.current().setEventDetails("Vm Id: " + vm.getUuid());

        boolean result = upgradeVirtualMachine(vmId, newServiceOfferingId, cmd.getDetails());
        if (result) {
            UserVmVO vmInstance = _vmDao.findById(vmId);
            if (vmInstance.getState().equals(State.Stopped)) {
                // Generate usage event for VM upgrade
                generateUsageEvent(vmInstance, vmInstance.isDisplayVm(), EventTypes.EVENT_VM_UPGRADE);
            }
            return vmInstance;
        } else {
            throw new CloudRuntimeException("Failed to scale the VM");
        }
    }

    @Override
    public boolean upgradeVirtualMachine(Long vmId, Long newServiceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
    ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {

        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vmInstance);
        if (vmInstance == null) {
            s_logger.error(String.format("VM instance with id [%s] is null, it is not possible to upgrade a null VM.", vmId));
            return false;
        }

        if (State.Stopped.equals(vmInstance.getState())) {
            upgradeStoppedVirtualMachine(vmId, newServiceOfferingId, customParameters);
            return true;
        }

        if (State.Running.equals(vmInstance.getState())) {
            ServiceOfferingVO newServiceOfferingVO = serviceOfferingDao.findById(newServiceOfferingId);
            HostVO instanceHost = _hostDao.findById(vmInstance.getHostId());
            _hostDao.loadHostTags(instanceHost);

            if (!instanceHost.checkHostServiceOfferingTags(newServiceOfferingVO)) {
                s_logger.error(String.format("Cannot upgrade VM [%s] as the new service offering [%s] does not have the required host tags %s.", vmInstance, newServiceOfferingVO,
                        instanceHost.getHostTags()));
                return false;
            }
        }
        return upgradeRunningVirtualMachine(vmId, newServiceOfferingId, customParameters);
    }

    private boolean upgradeRunningVirtualMachine(Long vmId, Long newServiceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
    ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {

        Account caller = CallContext.current().getCallingAccount();
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);

        Set<HypervisorType> supportedHypervisorTypes = new HashSet<>();
        supportedHypervisorTypes.add(HypervisorType.XenServer);
        supportedHypervisorTypes.add(HypervisorType.VMware);
        supportedHypervisorTypes.add(HypervisorType.Simulator);
        supportedHypervisorTypes.add(HypervisorType.KVM);

        HypervisorType vmHypervisorType = vmInstance.getHypervisorType();

        if (!supportedHypervisorTypes.contains(vmHypervisorType)) {
            String message = String.format("Scaling the VM dynamically is not supported for VMs running on Hypervisor [%s].", vmInstance.getHypervisorType());
            s_logger.info(message);
            throw new InvalidParameterValueException(message);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        //Check if its a scale "up"
        ServiceOfferingVO newServiceOffering = serviceOfferingDao.findById(newServiceOfferingId);
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            validateCustomParameters(newServiceOffering, customParameters);
            newServiceOffering = serviceOfferingDao.getComputeOffering(newServiceOffering, customParameters);
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, newServiceOffering);

        ServiceOfferingVO currentServiceOffering = serviceOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
        if (newServiceOffering.isDynamicScalingEnabled() != currentServiceOffering.isDynamicScalingEnabled()) {
            throw new InvalidParameterValueException("Unable to Scale VM: since dynamic scaling enabled flag is not same for new service offering and old service offering");
        }

        validateDiskOfferingChecks(currentServiceOffering, newServiceOffering);

        int newCpu = newServiceOffering.getCpu();
        int newMemory = newServiceOffering.getRamSize();
        int newSpeed = newServiceOffering.getSpeed();
        int currentCpu = currentServiceOffering.getCpu();
        int currentMemory = currentServiceOffering.getRamSize();
        int currentSpeed = currentServiceOffering.getSpeed();
        int memoryDiff = newMemory - currentMemory;
        int cpuDiff = newCpu * newSpeed - currentCpu * currentSpeed;

        // Don't allow to scale when (Any of the new values less than current values) OR (All current and new values are same)
        if ((newSpeed < currentSpeed || newMemory < currentMemory || newCpu < currentCpu) || (newSpeed == currentSpeed && newMemory == currentMemory && newCpu == currentCpu)) {
            String message = String.format("While the VM is running, only scalling up it is supported. New service offering {\"memory\": %s, \"speed\": %s, \"cpu\": %s} should"
              + " have at least one value (ram, speed or cpu) greater than the current values {\"memory\": %s, \"speed\": %s, \"cpu\": %s}.", newMemory, newSpeed, newCpu,
              currentMemory, currentSpeed, currentCpu);

            throw new InvalidParameterValueException(message);
        }

        if (vmHypervisorType.equals(HypervisorType.KVM) && !currentServiceOffering.isDynamic()) {
            String message = String.format("Unable to live scale VM on KVM when current service offering is a \"Fixed Offering\". KVM needs the tag \"maxMemory\" to live scale and it is only configured when VM is deployed with a custom service offering and \"Dynamic Scalable\" is enabled.");
            s_logger.info(message);
            throw new InvalidParameterValueException(message);
        }

        serviceOfferingDao.loadDetails(currentServiceOffering);
        serviceOfferingDao.loadDetails(newServiceOffering);

        Map<String, String> currentDetails = currentServiceOffering.getDetails();
        Map<String, String> newDetails = newServiceOffering.getDetails();
        String currentVgpuType = currentDetails.get("vgpuType");
        String newVgpuType = newDetails.get("vgpuType");

        if (currentVgpuType != null && (newVgpuType == null || !newVgpuType.equalsIgnoreCase(currentVgpuType))) {
            throw new InvalidParameterValueException(String.format("Dynamic scaling of vGPU type is not supported. VM has vGPU Type: [%s].", currentVgpuType));
        }

        // Check resource limits
        if (newCpu > currentCpu) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.cpu, newCpu - currentCpu);
        }

        if (newMemory > currentMemory) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.memory, memoryDiff);
        }

        // Dynamically upgrade the running vms
        boolean success = false;
        if (vmInstance.getState().equals(State.Running)) {
            int retry = _scaleRetry;
            ExcludeList excludes = new ExcludeList();

            // Check zone wide flag
            boolean enableDynamicallyScaleVm = EnableDynamicallyScaleVm.valueIn(vmInstance.getDataCenterId());
            if (!enableDynamicallyScaleVm) {
                throw new PermissionDeniedException("Dynamically scaling virtual machines is disabled for this zone, please contact your admin.");
            }

            // Check vm flag
            if (!vmInstance.isDynamicallyScalable()) {
                throw new CloudRuntimeException(String.format("Unable to scale %s as it does not have tools to support dynamic scaling.", vmInstance.toString()));
            }

            // Check disable threshold for cluster is not crossed
            HostVO host = _hostDao.findById(vmInstance.getHostId());
            if (_capacityMgr.checkIfClusterCrossesThreshold(host.getClusterId(), cpuDiff, memoryDiff)) {
                throw new CloudRuntimeException(String.format("Unable to scale %s due to insufficient resources.", vmInstance.toString()));
            }

            while (retry-- != 0) { // It's != so that it can match -1.
                try {
                    boolean existingHostHasCapacity = false;

                    // Increment CPU and Memory count accordingly.
                    if (newCpu > currentCpu) {
                        _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
                    }

                    if (memoryDiff > 0) {
                        _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(memoryDiff));
                    }

                    // #1 Check existing host has capacity
                    if (!excludes.shouldAvoid(ApiDBUtils.findHostById(vmInstance.getHostId()))) {
                        existingHostHasCapacity = _capacityMgr.checkIfHostHasCpuCapability(vmInstance.getHostId(), newCpu, newSpeed)
                                && _capacityMgr.checkIfHostHasCapacity(vmInstance.getHostId(), cpuDiff, ByteScaleUtils.mebibytesToBytes(memoryDiff), false,
                                        _capacityMgr.getClusterOverProvisioningFactor(host.getClusterId(), Capacity.CAPACITY_TYPE_CPU),
                                        _capacityMgr.getClusterOverProvisioningFactor(host.getClusterId(), Capacity.CAPACITY_TYPE_MEMORY), false);
                        excludes.addHost(vmInstance.getHostId());
                    }

                    // #2 migrate the vm if host doesn't have capacity or is in avoid set
                    if (!existingHostHasCapacity) {
                        _itMgr.findHostAndMigrate(vmInstance.getUuid(), newServiceOfferingId, customParameters, excludes);
                    }

                    // #3 resize or migrate the root volume if required
                    DiskOfferingVO newDiskOffering = _diskOfferingDao.findById(newServiceOffering.getDiskOfferingId());
                    changeDiskOfferingForRootVolume(vmId, newDiskOffering, customParameters, vmInstance.getDataCenterId());

                    // #4 scale the vm now
                    vmInstance = _vmInstanceDao.findById(vmId);
                    _itMgr.reConfigureVm(vmInstance.getUuid(), currentServiceOffering, newServiceOffering, customParameters, existingHostHasCapacity);
                    success = true;
                    return success;
                } catch (InsufficientCapacityException | ResourceUnavailableException | ConcurrentOperationException e) {
                    s_logger.error(String.format("Unable to scale %s due to [%s].", vmInstance.toString(), e.getMessage()), e);
                } finally {
                    if (!success) {
                        // Decrement CPU and Memory count accordingly.
                        if (newCpu > currentCpu) {
                            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
                        }

                        if (memoryDiff > 0) {
                            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(memoryDiff));
                        }
                    }
                }
            }
        }
        return success;
    }

    protected void validateDiskOfferingChecks(ServiceOfferingVO currentServiceOffering, ServiceOfferingVO newServiceOffering) {
        if (currentServiceOffering.getDiskOfferingStrictness() != newServiceOffering.getDiskOfferingStrictness()) {
            throw new InvalidParameterValueException("Unable to Scale VM, since disk offering strictness flag is not same for new service offering and old service offering");
        }

        if (currentServiceOffering.getDiskOfferingStrictness() && currentServiceOffering.getDiskOfferingId() != newServiceOffering.getDiskOfferingId()) {
            throw new InvalidParameterValueException("Unable to Scale VM, since disk offering id associated with the old service offering is not same for new service offering");
        }

        DiskOfferingVO currentRootDiskOffering = _diskOfferingDao.findByIdIncludingRemoved(currentServiceOffering.getDiskOfferingId());
        DiskOfferingVO newRootDiskOffering = _diskOfferingDao.findById(newServiceOffering.getDiskOfferingId());

        if (currentRootDiskOffering.getEncrypt() != newRootDiskOffering.getEncrypt()) {
            throw new InvalidParameterValueException("Cannot change volume encryption type via service offering change");
        }
    }

    private void changeDiskOfferingForRootVolume(Long vmId, DiskOfferingVO newDiskOffering, Map<String, String> customParameters, Long zoneId) throws ResourceAllocationException {

        if (!AllowDiskOfferingChangeDuringScaleVm.valueIn(zoneId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Changing the disk offering of the root volume during the compute offering change operation is disabled. Please check the setting [%s].", AllowDiskOfferingChangeDuringScaleVm.key()));
            }
            return;
        }

        List<VolumeVO> vols = _volsDao.findReadyAndAllocatedRootVolumesByInstance(vmId);

        for (final VolumeVO rootVolumeOfVm : vols) {
            DiskOfferingVO currentRootDiskOffering = _diskOfferingDao.findById(rootVolumeOfVm.getDiskOfferingId());
            Long rootDiskSize= null;
            Long rootDiskSizeBytes = null;
            if (customParameters.containsKey(ApiConstants.ROOT_DISK_SIZE)) {
                rootDiskSize = Long.parseLong(customParameters.get(ApiConstants.ROOT_DISK_SIZE));
                rootDiskSizeBytes = rootDiskSize << 30;
            }
            if (currentRootDiskOffering.getId() == newDiskOffering.getId() &&
                    (!newDiskOffering.isCustomized() || (newDiskOffering.isCustomized() && Objects.equals(rootVolumeOfVm.getSize(), rootDiskSizeBytes)))) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Volume %s is already having disk offering %s", rootVolumeOfVm, newDiskOffering.getUuid()));
                }
                continue;
            }
            HypervisorType hypervisorType = _volsDao.getHypervisorType(rootVolumeOfVm.getId());
            if (HypervisorType.Simulator != hypervisorType) {
                Long minIopsInNewDiskOffering = null;
                Long maxIopsInNewDiskOffering = null;
                boolean autoMigrate = false;
                boolean shrinkOk = false;
                if (customParameters.containsKey(ApiConstants.MIN_IOPS)) {
                    minIopsInNewDiskOffering = Long.parseLong(customParameters.get(ApiConstants.MIN_IOPS));
                }
                if (customParameters.containsKey(ApiConstants.MAX_IOPS)) {
                    minIopsInNewDiskOffering = Long.parseLong(customParameters.get(ApiConstants.MAX_IOPS));
                }
                if (customParameters.containsKey(ApiConstants.AUTO_MIGRATE)) {
                    autoMigrate = Boolean.parseBoolean(customParameters.get(ApiConstants.AUTO_MIGRATE));
                }
                if (customParameters.containsKey(ApiConstants.SHRINK_OK)) {
                    shrinkOk = Boolean.parseBoolean(customParameters.get(ApiConstants.SHRINK_OK));
                }
                ChangeOfferingForVolumeCmd changeOfferingForVolumeCmd = new ChangeOfferingForVolumeCmd(rootVolumeOfVm.getId(), newDiskOffering.getId(), minIopsInNewDiskOffering, maxIopsInNewDiskOffering, autoMigrate, shrinkOk);
                if (rootDiskSize != null) {
                    changeOfferingForVolumeCmd.setSize(rootDiskSize);
                }
                Volume result = _volumeService.changeDiskOfferingForVolume(changeOfferingForVolumeCmd);
                if (result == null) {
                    throw new CloudRuntimeException("Failed to change disk offering of the root volume");
                }
            } else if (newDiskOffering.getDiskSize() > 0 && currentRootDiskOffering.getDiskSize() != newDiskOffering.getDiskSize()) {
                throw new InvalidParameterValueException("Hypervisor " + hypervisorType + " does not support volume resize");
            }
        }
    }

    @Override
    public HashMap<String, VolumeStatsEntry> getVolumeStatistics(long clusterId, String poolUuid, StoragePoolType poolType,  int timeout) {
        List<HostVO> neighbors = _resourceMgr.listHostsInClusterByStatus(clusterId, Status.Up);
        StoragePoolVO storagePool = _storagePoolDao.findPoolByUUID(poolUuid);
        HashMap<String, VolumeStatsEntry> volumeStatsByUuid = new HashMap<>();

        for (HostVO neighbor : neighbors) {

            // - zone wide storage for specific hypervisortypes
            if ((ScopeType.ZONE.equals(storagePool.getScope()) && storagePool.getHypervisor() != neighbor.getHypervisorType())) {
                // skip this neighbour if their hypervisor type is not the same as that of the store
                continue;
            }

            List<String> volumeLocators = getVolumesByHost(neighbor, storagePool);
            if (!CollectionUtils.isEmpty(volumeLocators)) {

                GetVolumeStatsCommand cmd = new GetVolumeStatsCommand(poolType, poolUuid, volumeLocators);
                Answer answer = null;

                DataStoreProvider storeProvider = _dataStoreProviderMgr
                        .getDataStoreProvider(storagePool.getStorageProviderName());
                DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

                if (storeDriver instanceof PrimaryDataStoreDriver && ((PrimaryDataStoreDriver) storeDriver).canProvideVolumeStats()) {
                    // Get volume stats from the pool directly instead of sending cmd to host
                    answer = storageManager.getVolumeStats(storagePool, cmd);
                } else {
                    if (timeout > 0) {
                        cmd.setWait(timeout/1000);
                    }

                    answer = _agentMgr.easySend(neighbor.getId(), cmd);
                }

                if (answer != null && answer instanceof GetVolumeStatsAnswer){
                    GetVolumeStatsAnswer volstats = (GetVolumeStatsAnswer)answer;
                    if (volstats.getVolumeStats() != null) {
                        volumeStatsByUuid.putAll(volstats.getVolumeStats());
                    }
                }
            }
        }
        return volumeStatsByUuid.size() > 0 ? volumeStatsByUuid : null;
    }

    private List<String> getVolumesByHost(HostVO host, StoragePool pool){
        List<VMInstanceVO> vmsPerHost = _vmInstanceDao.listByHostId(host.getId());
        return vmsPerHost.stream()
                .flatMap(vm -> _volsDao.findByInstanceIdAndPoolId(vm.getId(),pool.getId()).stream().map(vol ->
                vol.getState() == Volume.State.Ready ? (vol.getFormat() == ImageFormat.OVA ? vol.getChainInfo() : vol.getPath()) : null).filter(Objects::nonNull))
                .collect(Collectors.toList());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VM_RECOVER, eventDescription = "Recovering VM")
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException, CloudRuntimeException {

        final Long vmId = cmd.getId();
        Account caller = CallContext.current().getCallingAccount();
        final Long userId = caller.getAccountId();

        // Verify input parameters
        final UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        // When trying to expunge, permission is denied when the caller is not an admin and the AllowUserExpungeRecoverVm is false for the caller.
        if (!_accountMgr.isAdmin(userId) && !AllowUserExpungeRecoverVm.valueIn(userId)) {
            throw new PermissionDeniedException("Recovering a vm can only be done by an Admin. Or when the allow.user.expunge.recover.vm key is set.");
        }

        if (vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is removed: " + vmId);
            }
            throw new InvalidParameterValueException("Unable to find vm by id " + vmId);
        }

        if (vm.getState() != State.Destroyed) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm is not in the right state: " + vmId);
            }
            throw new InvalidParameterValueException("Vm with id " + vmId + " is not in the right state");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Recovering vm " + vmId);
        }

        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<ResourceAllocationException>() {
            @Override public void doInTransactionWithoutResult(TransactionStatus status) throws ResourceAllocationException {

                Account account = _accountDao.lockRow(vm.getAccountId(), true);

                // if the account is deleted, throw error
                if (account.getRemoved() != null) {
                    throw new CloudRuntimeException("Unable to recover VM as the account is deleted");
                }

                // Get serviceOffering for Virtual Machine
                ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

                // First check that the maximum number of UserVMs, CPU and Memory limit for the given
                // accountId will not be exceeded
                if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
                    resourceLimitCheck(account, vm.isDisplayVm(), new Long(serviceOffering.getCpu()), new Long(serviceOffering.getRamSize()));
                }

                _haMgr.cancelDestroy(vm, vm.getHostId());

                try {
                    if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.RecoveryRequested, null)) {
                        s_logger.debug("Unable to recover the vm because it is not in the correct state: " + vmId);
                        throw new InvalidParameterValueException("Unable to recover the vm because it is not in the correct state: " + vmId);
                    }
                } catch (NoTransitionException e) {
                    throw new InvalidParameterValueException("Unable to recover the vm because it is not in the correct state: " + vmId);
                }

                // Recover the VM's disks
                List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
                for (VolumeVO volume : volumes) {
                    if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                        recoverRootVolume(volume, vmId);
                        break;
                    }
                }

                //Update Resource Count for the given account
                resourceCountIncrement(account.getId(), vm.isDisplayVm(), new Long(serviceOffering.getCpu()), new Long(serviceOffering.getRamSize()));
            }
        });

        return _vmDao.findById(vmId);
    }

    protected void recoverRootVolume(VolumeVO volume, Long vmId) {
        if (Volume.State.Destroy.equals(volume.getState())) {
            _volumeService.recoverVolume(volume.getId());
            _volsDao.attachVolume(volume.getId(), vmId, ROOT_DEVICE_ID);
        } else {
            _volumeService.publishVolumeCreationUsageEvent(volume);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        capacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);

        String time = configs.get("expunge.interval");
        _expungeInterval = NumbersUtil.parseInt(time, 86400);
        time = configs.get("expunge.delay");
        _expungeDelay = NumbersUtil.parseInt(time, _expungeInterval);

        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UserVm-Scavenger"));

        String vmIpWorkers = configs.get(VmIpFetchTaskWorkers.value());
        int vmipwrks = NumbersUtil.parseInt(vmIpWorkers, 10);

        _vmIpFetchExecutor =   Executors.newScheduledThreadPool(vmipwrks, new NamedThreadFactory("UserVm-ipfetch"));

        String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        int _usageAggregationRange  = NumbersUtil.parseInt(aggregationRange, 1440);
        int HOURLY_TIME = 60;
        final int DAILY_TIME = 60 * 24;
        if (_usageAggregationRange == DAILY_TIME) {
            _dailyOrHourly = true;
        } else if (_usageAggregationRange == HOURLY_TIME) {
            _dailyOrHourly = true;
        } else {
            _dailyOrHourly = false;
        }

        _itMgr.registerGuru(VirtualMachine.Type.User, this);

        VirtualMachine.State.getStateMachine().registerListener(new UserVmStateListener(_usageEventDao, _networkDao, _nicDao, serviceOfferingDao, _vmDao, this, _configDao));

        String value = _configDao.getValue(Config.SetVmInternalNameUsingDisplayName.key());
        _instanceNameFlag = (value == null) ? false : Boolean.parseBoolean(value);

        _scaleRetry = NumbersUtil.parseInt(configs.get(Config.ScaleRetry.key()), 2);

        _vmIpFetchThreadExecutor = Executors.newFixedThreadPool(VmIpFetchThreadPoolMax.value(), new NamedThreadFactory("vmIpFetchThread"));

        s_logger.info("User VM Manager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new ExpungeTask(), _expungeInterval, _expungeInterval, TimeUnit.SECONDS);
        _vmIpFetchExecutor.scheduleWithFixedDelay(new VmIpFetchTask(), VmIpFetchWaitInterval.value(), VmIpFetchWaitInterval.value(), TimeUnit.SECONDS);
        loadVmDetailsInMapForExternalDhcpIp();
        return true;
    }

    private void loadVmDetailsInMapForExternalDhcpIp() {

        List<NetworkVO> networks = _networkDao.listByGuestType(Network.GuestType.Shared);

        for (NetworkVO network: networks) {
            if(_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                List<NicVO> nics = _nicDao.listByNetworkId(network.getId());

                for (NicVO nic : nics) {
                    if (nic.getIPv4Address() == null) {
                        long nicId = nic.getId();
                        long vmId = nic.getInstanceId();
                        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);

                        // only load running vms. For stopped vms get loaded on starting
                        if (vmInstance != null && vmInstance.getState() == State.Running) {
                            VmAndCountDetails vmAndCount = new VmAndCountDetails(vmId, VmIpFetchTrialMax.value());
                            vmIdCountMap.put(nicId, vmAndCount);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean stop() {
        _executor.shutdown();
        _vmIpFetchExecutor.shutdown();
        return true;
    }

    public String getRandomPrivateTemplateName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean expunge(UserVmVO vm) {
        vm = _vmDao.acquireInLockTable(vm.getId());
        if (vm == null) {
            return false;
        }
        try {

            if (vm.getBackupOfferingId() != null) {
                List<Backup> backupsForVm = backupDao.listByVmId(vm.getDataCenterId(), vm.getId());
                if (CollectionUtils.isEmpty(backupsForVm)) {
                    backupManager.removeVMFromBackupOffering(vm.getId(), true);
                } else {
                    throw new CloudRuntimeException(String.format("This VM [uuid: %s, name: %s] has a "
                            + "Backup Offering [id: %s, external id: %s] with %s backups. Please, remove the backup offering "
                            + "before proceeding to VM exclusion!", vm.getUuid(), vm.getInstanceName(), vm.getBackupOfferingId(),
                            vm.getBackupExternalId(), backupsForVm.size()));
                }
            }

            autoScaleManager.removeVmFromVmGroup(vm.getId());

            releaseNetworkResourcesOnExpunge(vm.getId());

            List<VolumeVO> rootVol = _volsDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);
            // expunge the vm
            _itMgr.advanceExpunge(vm.getUuid());

            // Only if vm is not expunged already, cleanup it's resources
            if (vm.getRemoved() == null) {
                // Cleanup vm resources - all the PF/LB/StaticNat rules
                // associated with vm
                s_logger.debug("Starting cleaning up vm " + vm + " resources...");
                if (cleanupVmResources(vm.getId())) {
                    s_logger.debug("Successfully cleaned up vm " + vm + " resources as a part of expunge process");
                } else {
                    s_logger.warn("Failed to cleanup resources as a part of vm " + vm + " expunge");
                    return false;
                }

                if (vm.getUserDataId() != null) {
                    vm.setUserDataId(null);
                    _vmDao.update(vm.getId(), vm);
                }

                _vmDao.remove(vm.getId());
            }

            return true;

        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge  " + vm, e);
            return false;
        } catch (OperationTimedoutException e) {
            s_logger.warn("Operation time out on expunging " + vm, e);
            return false;
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Concurrent operations on expunging " + vm, e);
            return false;
        } finally {
            _vmDao.releaseFromLockTable(vm.getId());
        }
    }

    /**
     * Release network resources, it was done on vm stop previously.
     * @param id vm id
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    private void releaseNetworkResourcesOnExpunge(long id) throws ConcurrentOperationException, ResourceUnavailableException {
        final VMInstanceVO vmInstance = _vmDao.findById(id);
        if (vmInstance != null){
            final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vmInstance);
            _networkMgr.release(profile, false);
        }
        else {
            s_logger.error("Couldn't find vm with id = " + id + ", unable to release network resources");
        }
    }

    private boolean cleanupVmResources(long vmId) {
        boolean success = true;
        // Remove vm from security groups
        _securityGroupMgr.removeInstanceFromGroups(vmId);

        // Remove vm from instance group
        removeInstanceFromInstanceGroup(vmId);

        // cleanup firewall rules
        if (_firewallMgr.revokeFirewallRulesForVm(vmId)) {
            s_logger.debug("Firewall rules are removed successfully as a part of vm id=" + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove firewall rules as a part of vm id=" + vmId + " expunge");
        }

        // cleanup port forwarding rules
        if (_rulesMgr.revokePortForwardingRulesForVm(vmId)) {
            s_logger.debug("Port forwarding rules are removed successfully as a part of vm id=" + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove port forwarding rules as a part of vm id=" + vmId + " expunge");
        }

        // cleanup load balancer rules
        if (_lbMgr.removeVmFromLoadBalancers(vmId)) {
            s_logger.debug("Removed vm id=" + vmId + " from all load balancers as a part of expunge process");
        } else {
            success = false;
            s_logger.warn("Fail to remove vm id=" + vmId + " from load balancers as a part of expunge process");
        }

        // If vm is assigned to static nat, disable static nat for the ip
        // address and disassociate ip if elasticIP is enabled
        List<IPAddressVO> ips = _ipAddressDao.findAllByAssociatedVmId(vmId);

        for (IPAddressVO ip : ips) {
            try {
                if (_rulesMgr.disableStaticNat(ip.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM, true)) {
                    s_logger.debug("Disabled 1-1 nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge");
                } else {
                    s_logger.warn("Failed to disable static nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge");
                    success = false;
                }
            } catch (ResourceUnavailableException e) {
                success = false;
                s_logger.warn("Failed to disable static nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge because resource is unavailable", e);
            }
        }

        return success;
    }

    @Override
    public void deletePrivateTemplateRecord(Long templateId) {
        if (templateId != null) {
            _templateDao.remove(templateId);
        }
    }

    // used for vm transitioning to error state
    private void updateVmStateForFailedVmCreation(Long vmId, Long hostId) {

        UserVmVO vm = _vmDao.findById(vmId);

        if (vm != null) {
            if (vm.getState().equals(State.Stopped)) {
                s_logger.debug("Destroying vm " + vm + " as it failed to create on Host with Id:" + hostId);
                try {
                    _itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationFailedToError, null);
                } catch (NoTransitionException e1) {
                    s_logger.warn(e1.getMessage());
                }
                // destroy associated volumes for vm in error state
                // get all volumes in non destroyed state
                List<VolumeVO> volumesForThisVm = _volsDao.findUsableVolumesForInstance(vm.getId());
                for (VolumeVO volume : volumesForThisVm) {
                    if (volume.getState() != Volume.State.Destroy) {
                        volumeMgr.destroyVolume(volume);
                    }
                }
                String msg = "Failed to deploy Vm with Id: " + vmId + ", on Host with Id: " + hostId;
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);

                // Get serviceOffering for Virtual Machine
                ServiceOfferingVO offering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

                // Update Resource Count for the given account
                resourceCountDecrement(vm.getAccountId(), vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));
            }
        }
    }



    private class VmIpFetchTask extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("vmIpFetch");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {

                        for (Entry<Long, VmAndCountDetails> entry:   vmIdCountMap.entrySet()) {
                            long nicId = entry.getKey();
                            VmAndCountDetails vmIdAndCount = entry.getValue();
                            long vmId = vmIdAndCount.getVmId();

                            if (vmIdAndCount.getRetrievalCount() <= 0) {
                                vmIdCountMap.remove(nicId);
                                s_logger.debug("Vm " + vmId +" nic "+nicId + " count is zero .. removing vm nic from map ");

                                ActionEventUtils.onActionEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM,
                                        Domain.ROOT_DOMAIN, EventTypes.EVENT_NETWORK_EXTERNAL_DHCP_VM_IPFETCH,
                                        "VM " + vmId + " nic id "+ nicId + " ip addr fetch failed ", vmId, ApiCommandResourceType.VirtualMachine.toString());

                                continue;
                            }


                            UserVm userVm = _vmDao.findById(vmId);
                            VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
                            NicVO nicVo = _nicDao.findById(nicId);
                            NetworkVO network = _networkDao.findById(nicVo.getNetworkId());

                            VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(userVm);
                            VirtualMachine vm = vmProfile.getVirtualMachine();
                            boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

                            _vmIpFetchThreadExecutor.execute(new VmIpAddrFetchThread(vmId, nicId, vmInstance.getInstanceName(),
                                    isWindows, vm.getHostId(), network.getCidr()));

                        }
                    } catch (Exception e) {
                        s_logger.error("Caught the Exception in VmIpFetchTask", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }

        }
    }


    private class ExpungeTask extends ManagedContextRunnable {
        public ExpungeTask() {
        }

        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("UserVMExpunge");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {
                        List<UserVmVO> vms = _vmDao.findDestroyedVms(new Date(System.currentTimeMillis() - ((long)_expungeDelay << 10)));
                        if (s_logger.isInfoEnabled()) {
                            if (vms.size() == 0) {
                                s_logger.trace("Found " + vms.size() + " vms to expunge.");
                            } else {
                                s_logger.info("Found " + vms.size() + " vms to expunge.");
                            }
                        }
                        for (UserVmVO vm : vms) {
                            try {
                                expungeVm(vm.getId());
                            } catch (Exception e) {
                                s_logger.warn("Unable to expunge " + vm, e);
                            }
                        }
                    } catch (Exception e) {
                        s_logger.error("Caught the following Exception", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    private void verifyVmLimits(UserVmVO vmInstance, Map<String, String> details) {
        Account owner = _accountDao.findById(vmInstance.getAccountId());
        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vmInstance + " does not exist: " + vmInstance.getAccountId());
        }

        long newCpu = NumberUtils.toLong(details.get(VmDetailConstants.CPU_NUMBER));
        long newMemory = NumberUtils.toLong(details.get(VmDetailConstants.MEMORY));
        ServiceOfferingVO currentServiceOffering = serviceOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
        ServiceOfferingVO svcOffering = serviceOfferingDao.findById(vmInstance.getServiceOfferingId());
        boolean isDynamic = currentServiceOffering.isDynamic();
        if (isDynamic) {
            Map<String, String> customParameters = new HashMap<>();
            customParameters.put(VmDetailConstants.CPU_NUMBER, String.valueOf(newCpu));
            customParameters.put(VmDetailConstants.MEMORY, String.valueOf(newMemory));
            if (svcOffering.isCustomCpuSpeedSupported()) {
                customParameters.put(VmDetailConstants.CPU_SPEED, details.get(VmDetailConstants.CPU_SPEED));
            }
            validateCustomParameters(svcOffering, customParameters);
        }
        if (VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            return;
        }
        long currentCpu = currentServiceOffering.getCpu();
        long currentMemory = currentServiceOffering.getRamSize();

        try {
            if (newCpu > currentCpu) {
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.cpu, newCpu - currentCpu);
            }
            if (newMemory > currentMemory) {
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.memory, newMemory - currentMemory);
            }
        } catch (ResourceAllocationException e) {
            s_logger.error(String.format("Failed to updated VM due to: %s", e.getLocalizedMessage()));
            throw new InvalidParameterValueException(e.getLocalizedMessage());
        }

        if (newCpu > currentCpu) {
            _resourceLimitMgr.incrementResourceCount(owner.getAccountId(), ResourceType.cpu, newCpu - currentCpu);
        } else if (newCpu > 0 && currentCpu > newCpu){
            _resourceLimitMgr.decrementResourceCount(owner.getAccountId(), ResourceType.cpu, currentCpu - newCpu);
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.incrementResourceCount(owner.getAccountId(), ResourceType.memory, newMemory - currentMemory);
        } else if (newMemory > 0 && currentMemory > newMemory){
            _resourceLimitMgr.decrementResourceCount(owner.getAccountId(), ResourceType.memory, currentMemory - newMemory);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPDATE, eventDescription = "updating Vm")
    public UserVm updateVirtualMachine(UpdateVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException {
        validateInputsAndPermissionForUpdateVirtualMachineCommand(cmd);

        String displayName = cmd.getDisplayName();
        String group = cmd.getGroup();
        Boolean ha = cmd.getHaEnable();
        Boolean isDisplayVm = cmd.getDisplayVm();
        Long id = cmd.getId();
        Long osTypeId = cmd.getOsTypeId();
        Boolean isDynamicallyScalable = cmd.isDynamicallyScalable();
        String hostName = cmd.getHostName();
        Map<String,String> details = cmd.getDetails();
        List<Long> securityGroupIdList = getSecurityGroupIdList(cmd);
        boolean cleanupDetails = cmd.isCleanupDetails();
        String extraConfig = cmd.getExtraConfig();

        UserVmVO vmInstance = _vmDao.findById(cmd.getId());
        VMTemplateVO template = _templateDao.findById(vmInstance.getTemplateId());
        if (MapUtils.isNotEmpty(details) || cmd.isCleanupDetails()) {
            if (template != null && template.isDeployAsIs()) {
                throw new CloudRuntimeException("Detail settings are read from OVA, it cannot be changed by API call.");
            }
        }
        String userData = cmd.getUserData();
        Long userDataId = cmd.getUserdataId();
        String userDataDetails = null;
        if (MapUtils.isNotEmpty(cmd.getUserdataDetails())) {
            userDataDetails = cmd.getUserdataDetails().toString();
        }
        userData = finalizeUserData(userData, userDataId, template);

        long accountId = vmInstance.getAccountId();

        if (isDisplayVm != null && isDisplayVm != vmInstance.isDisplay()) {
            updateDisplayVmFlag(isDisplayVm, id, vmInstance);
        }
        final Account caller = CallContext.current().getCallingAccount();
        final List<String> userDenyListedSettings = Stream.of(QueryService.UserVMDeniedDetails.value().split(","))
                .map(item -> (item).trim())
                .collect(Collectors.toList());
        final List<String> userReadOnlySettings = Stream.of(QueryService.UserVMReadOnlyDetails.value().split(","))
                .map(item -> (item).trim())
                .collect(Collectors.toList());
        if (cleanupDetails){
            if (caller != null && caller.getType() == Account.Type.ADMIN) {
                userVmDetailsDao.removeDetails(id);
            } else {
                for (final UserVmDetailVO detail : userVmDetailsDao.listDetails(id)) {
                    if (detail != null && !userDenyListedSettings.contains(detail.getName())
                            && !userReadOnlySettings.contains(detail.getName())) {
                        userVmDetailsDao.removeDetail(id, detail.getName());
                    }
                }
            }
        } else {
            if (MapUtils.isNotEmpty(details)) {
                if (details.containsKey("extraconfig")) {
                    throw new InvalidParameterValueException("'extraconfig' should not be included in details as key");
                }

                if (caller != null && caller.getType() != Account.Type.ADMIN) {
                    // Ensure denied or read-only detail is not passed by non-root-admin user
                    for (final String detailName : details.keySet()) {
                        if (userDenyListedSettings.contains(detailName)) {
                            throw new InvalidParameterValueException("You're not allowed to add or edit the restricted setting: " + detailName);
                        }
                        if (userReadOnlySettings.contains(detailName)) {
                            throw new InvalidParameterValueException("You're not allowed to add or edit the read-only setting: " + detailName);
                        }
                    }
                    // Add any hidden/denied or read-only detail
                    for (final UserVmDetailVO detail : userVmDetailsDao.listDetails(id)) {
                        if (userDenyListedSettings.contains(detail.getName()) || userReadOnlySettings.contains(detail.getName())) {
                            details.put(detail.getName(), detail.getValue());
                        }
                    }
                }

                verifyVmLimits(vmInstance, details);
                vmInstance.setDetails(details);
                _vmDao.saveDetails(vmInstance);
            }
            if (StringUtils.isNotBlank(extraConfig)) {
                if (EnableAdditionalVmConfig.valueIn(accountId)) {
                    s_logger.info("Adding extra configuration to user vm: " + vmInstance.getUuid());
                    addExtraConfig(vmInstance, extraConfig);
                } else {
                    throw new InvalidParameterValueException("attempted setting extraconfig but enable.additional.vm.configuration is disabled");
                }
            }
        }
        return updateVirtualMachine(id, displayName, group, ha, isDisplayVm, osTypeId, userData, userDataId, userDataDetails, isDynamicallyScalable,
                cmd.getHttpMethod(), cmd.getCustomId(), hostName, cmd.getInstanceName(), securityGroupIdList, cmd.getDhcpOptionsMap());
    }

    protected void updateDisplayVmFlag(Boolean isDisplayVm, Long id, UserVmVO vmInstance) {
        vmInstance.setDisplayVm(isDisplayVm);

        // Resource limit changes
        ServiceOffering offering = serviceOfferingDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
        if (isDisplayVm) {
            resourceCountIncrement(vmInstance.getAccountId(), true, Long.valueOf(offering.getCpu()), Long.valueOf(offering.getRamSize()));
        } else {
            resourceCountDecrement(vmInstance.getAccountId(), true, Long.valueOf(offering.getCpu()), Long.valueOf(offering.getRamSize()));
        }

        // Usage
        saveUsageEvent(vmInstance);

        // take care of the root volume as well.
        List<VolumeVO> rootVols = _volsDao.findByInstanceAndType(id, Volume.Type.ROOT);
        if (!rootVols.isEmpty()) {
            _volumeService.updateDisplay(rootVols.get(0), isDisplayVm);
        }

        // take care of the data volumes as well.
        List<VolumeVO> dataVols = _volsDao.findByInstanceAndType(id, Volume.Type.DATADISK);
        for (Volume dataVol : dataVols) {
            _volumeService.updateDisplay(dataVol, isDisplayVm);
        }
    }

    protected void validateInputsAndPermissionForUpdateVirtualMachineCommand(UpdateVMCmd cmd) {
        UserVmVO vmInstance = _vmDao.findById(cmd.getId());
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find virtual machine with id: " + cmd.getId());
        }
        validateGuestOsIdForUpdateVirtualMachineCommand(cmd);
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, vmInstance);
    }

    protected void validateGuestOsIdForUpdateVirtualMachineCommand(UpdateVMCmd cmd) {
        Long osTypeId = cmd.getOsTypeId();
        if (osTypeId != null) {
            GuestOSVO guestOS = _guestOSDao.findById(osTypeId);
            if (guestOS == null) {
                throw new InvalidParameterValueException("Please specify a valid guest OS ID.");
            }
        }
    }

    private void saveUsageEvent(UserVmVO vm) {

        // If vm not destroyed
        if( vm.getState() != State.Destroyed && vm.getState() != State.Expunging && vm.getState() != State.Error){

            if(vm.isDisplayVm()){
                //1. Allocated VM Usage Event
                generateUsageEvent(vm, true, EventTypes.EVENT_VM_CREATE);

                if(vm.getState() == State.Running || vm.getState() == State.Stopping){
                    //2. Running VM Usage Event
                    generateUsageEvent(vm, true, EventTypes.EVENT_VM_START);

                    // 3. Network offering usage
                    generateNetworkUsageForVm(vm, true, EventTypes.EVENT_NETWORK_OFFERING_ASSIGN);
                }

            }else {
                //1. Allocated VM Usage Event
                generateUsageEvent(vm, true, EventTypes.EVENT_VM_DESTROY);

                if(vm.getState() == State.Running || vm.getState() == State.Stopping){
                    //2. Running VM Usage Event
                    generateUsageEvent(vm, true, EventTypes.EVENT_VM_STOP);

                    // 3. Network offering usage
                    generateNetworkUsageForVm(vm, true, EventTypes.EVENT_NETWORK_OFFERING_REMOVE);
                }
            }
        }

    }

    private void generateNetworkUsageForVm(VirtualMachine vm, boolean isDisplay, String eventType){

        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            long isDefault = (nic.isDefaultNic()) ? 1 : 0;
            UsageEventUtils.publishUsageEvent(eventType, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    Long.toString(nic.getId()), network.getNetworkOfferingId(), null, isDefault, vm.getClass().getName(), vm.getUuid(), isDisplay);
        }

    }

    @Override
    public UserVm updateVirtualMachine(long id, String displayName, String group, Boolean ha, Boolean isDisplayVmEnabled, Long osTypeId, String userData,
                                       Long userDataId, String userDataDetails, Boolean isDynamicallyScalable, HTTPMethod httpMethod, String customId, String hostName, String instanceName, List<Long> securityGroupIdList, Map<String, Map<Integer, String>> extraDhcpOptionsMap)
                    throws ResourceUnavailableException, InsufficientCapacityException {
        UserVmVO vm = _vmDao.findById(id);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find virtual machine with id " + id);
        }

        if(instanceName != null){
            VMInstanceVO vmInstance = _vmInstanceDao.findVMByInstanceName(instanceName);
            if(vmInstance != null && vmInstance.getId() != id){
                throw new CloudRuntimeException("Instance name : " + instanceName + " is not unique");
            }
        }

        if (vm.getState() == State.Error || vm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + id);
            throw new InvalidParameterValueException("Vm with id " + id + " is not in the right state");
        }

        if (displayName == null) {
            displayName = vm.getDisplayName();
        }

        if (ha == null) {
            ha = vm.isHaEnabled();
        }

        ServiceOffering offering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
        if (!offering.isOfferHA() && ha) {
            throw new InvalidParameterValueException("Can't enable ha for the vm as it's created from the Service offering having HA disabled");
        }

        if (isDisplayVmEnabled == null) {
            isDisplayVmEnabled = vm.isDisplayVm();
        }

        boolean updateUserdata = false;
        if (userData != null) {
            // check and replace newlines
            userData = userData.replace("\\n", "");
            userData = validateUserData(userData, httpMethod);
            // update userData on domain router.
            updateUserdata = true;
        } else {
            userData = vm.getUserData();
        }

        if (userDataId == null) {
            userDataId = vm.getUserDataId();
        }

        if (userDataDetails == null) {
            userDataDetails = vm.getUserDataDetails();
        }

        if (osTypeId == null) {
            osTypeId = vm.getGuestOSId();
        }

        if (group != null) {
            addInstanceToGroup(id, group);
        }

        if (isDynamicallyScalable == null) {
            isDynamicallyScalable = vm.isDynamicallyScalable();
        } else {
            if (isDynamicallyScalable == true) {
                VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());
                if (!template.isDynamicallyScalable()) {
                    throw new InvalidParameterValueException("Dynamic Scaling cannot be enabled for the VM since its template does not have dynamic scaling enabled");
                }
                if (!offering.isDynamicScalingEnabled()) {
                    throw new InvalidParameterValueException("Dynamic Scaling cannot be enabled for the VM since its service offering does not have dynamic scaling enabled");
                }
                if (!UserVmManager.EnableDynamicallyScaleVm.valueIn(vm.getDataCenterId())) {
                    s_logger.debug(String.format("Dynamic Scaling cannot be enabled for the VM %s since the global setting enable.dynamic.scale.vm is set to false", vm.getUuid()));
                    throw new InvalidParameterValueException("Dynamic Scaling cannot be enabled for the VM since corresponding global setting is set to false");
                }
            }
        }

        boolean isVMware = (vm.getHypervisorType() == HypervisorType.VMware);

        if (securityGroupIdList != null && isVMware) {
            throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
        } else {
            // Get default guest network in Basic zone
            Network defaultNetwork = null;
            try {
                DataCenterVO zone = _dcDao.findById(vm.getDataCenterId());

                if (zone.getNetworkType() == NetworkType.Basic) {
                    // Get default guest network in Basic zone
                    defaultNetwork = _networkModel.getExclusiveGuestNetwork(zone.getId());
                } else if (zone.isSecurityGroupEnabled()) {
                    NicVO defaultNic = _nicDao.findDefaultNicForVM(vm.getId());
                    if (defaultNic != null) {
                        defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
                    }
                }
            } catch (InvalidParameterValueException e) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug(e.getMessage(),e);
                }
                defaultNetwork = _networkModel.getDefaultNetworkForVm(id);
            }

            if (securityGroupIdList != null && _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkModel.canAddDefaultSecurityGroup()) {
                if (vm.getState() == State.Stopped) {
                    // Remove instance from security groups
                    _securityGroupMgr.removeInstanceFromGroups(id);
                    // Add instance in provided groups
                    _securityGroupMgr.addInstanceToGroups(id, securityGroupIdList);
                } else {
                    throw new InvalidParameterValueException("Virtual machine must be stopped prior to update security groups ");
                }
            }
        }
        List<? extends Nic> nics = _nicDao.listByVmId(vm.getId());
        if (hostName != null) {
            // Check is hostName is RFC compliant
            checkNameForRFCCompliance(hostName);

            if (vm.getHostName().equals(hostName)) {
                s_logger.debug("Vm " + vm + " is already set with the hostName specified: " + hostName);
                hostName = null;
            }

            // Verify that vm's hostName is unique

            List<NetworkVO> vmNtwks = new ArrayList<NetworkVO>(nics.size());
            for (Nic nic : nics) {
                vmNtwks.add(_networkDao.findById(nic.getNetworkId()));
            }
            checkIfHostNameUniqueInNtwkDomain(hostName, vmNtwks);
        }

        List<NetworkVO> networks = nics.stream()
                .map(nic -> _networkDao.findById(nic.getNetworkId()))
                .collect(Collectors.toList());

        verifyExtraDhcpOptionsNetwork(extraDhcpOptionsMap, networks);
        for (Nic nic : nics) {
            _networkMgr.saveExtraDhcpOptions(networks.stream()
                    .filter(network -> network.getId() == nic.getNetworkId())
                    .findFirst()
                    .get()
                    .getUuid(), nic.getId(), extraDhcpOptionsMap);
        }

        _vmDao.updateVM(id, displayName, ha, osTypeId, userData, userDataId, userDataDetails, isDisplayVmEnabled, isDynamicallyScalable, customId, hostName, instanceName);

        if (updateUserdata) {
            updateUserData(vm);
        }

        if (State.Running == vm.getState()) {
            updateDns(vm, hostName);
        }

        return _vmDao.findById(id);
    }

    protected void updateUserData(UserVm vm) throws ResourceUnavailableException, InsufficientCapacityException {
        boolean result = updateUserDataInternal(vm);
        if (result) {
            s_logger.debug(String.format("User data successfully updated for vm id:  %s", vm.getId()));
        } else {
            throw new CloudRuntimeException("Failed to reset userdata for the virtual machine ");
        }
    }

    private void updateDns(UserVmVO vm, String hostName) throws ResourceUnavailableException, InsufficientCapacityException {
        if (!StringUtils.isEmpty(hostName)) {
            vm.setHostName(hostName);
            try {
                List<NicVO> nicVOs = _nicDao.listByVmId(vm.getId());
                for (NicVO nic : nicVOs) {
                    List<DomainRouterVO> routers = _routerDao.findByNetwork(nic.getNetworkId());
                    for (DomainRouterVO router : routers) {
                        if (router.getState() != State.Running) {
                            s_logger.warn(String.format("Unable to update DNS for VM %s, as virtual router: %s is not in the right state: %s ", vm, router.getName(), router.getState()));
                            continue;
                        }
                        Commands commands = new Commands(Command.OnError.Stop);
                        commandSetupHelper.createDhcpEntryCommand(router, vm, nic, false, commands);
                        if (!nwHelper.sendCommandsToRouter(router, commands)) {
                            throw new CloudRuntimeException(String.format("Unable to send commands to virtual router: %s", router.getHostId()));
                        }
                        Answer answer = commands.getAnswer("dhcp");
                        if (answer == null || !answer.getResult()) {
                            throw new CloudRuntimeException("Failed to update hostname");
                        }
                        updateUserData(vm);
                    }
                }
            } catch (CloudRuntimeException e) {
                throw new CloudRuntimeException(String.format("Failed to update hostname of VM %s to %s", vm.getInstanceName(), vm.getHostName()));
            }
        }
    }

    private boolean updateUserDataInternal(UserVm vm) throws ResourceUnavailableException, InsufficientCapacityException {
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());

        List<? extends Nic> nics = _nicDao.listByVmId(vm.getId());
        if (nics == null || nics.isEmpty()) {
            s_logger.error("unable to find any nics for vm " + vm.getUuid());
            return false;
        }

        boolean userDataApplied = false;
        for (Nic nic : nics) {
            userDataApplied |= applyUserData(template.getHypervisorType(), vm, nic);
        }
        return userDataApplied;
    }

    protected boolean applyUserData(HypervisorType hyperVisorType, UserVm vm, Nic nic) throws ResourceUnavailableException, InsufficientCapacityException {
        Network network = _networkDao.findById(nic.getNetworkId());
        NicProfile nicProfile = new NicProfile(nic, network, null, null, null, _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(
                hyperVisorType, network));
        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vm);

        if (_networkModel.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Service.UserData)) {
            UserDataServiceProvider element = _networkModel.getUserDataUpdateProvider(network);
            if (element == null) {
                throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for UserData update");
            }
            boolean result = element.saveUserData(network, nicProfile, vmProfile);
            if (!result) {
                s_logger.error("Failed to update userdata for vm " + vm + " and nic " + nic);
            } else {
                return true;
            }
        } else {
            s_logger.debug("Not applying userdata for nic id=" + nic.getId() + " in vm id=" + vmProfile.getId() + " because it is not supported in network id=" + network.getId());
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_START, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(StartVMCmd cmd) throws ExecutionException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Map<VirtualMachineProfile.Param, Object> additonalParams = new HashMap<>();
        if (cmd.getBootIntoSetup() != null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("Adding %s into the param map", VirtualMachineProfile.Param.BootIntoSetup.getName()));
            }
            additonalParams.put(VirtualMachineProfile.Param.BootIntoSetup, cmd.getBootIntoSetup());
        }
        UserVmDetailVO uefiDetail = userVmDetailsDao.findDetail(cmd.getId(), ApiConstants.BootType.UEFI.toString());
        if (uefiDetail != null) {
            addVmUefiBootOptionsToParams(additonalParams, uefiDetail.getName(), uefiDetail.getValue());
        }
        if (cmd.getConsiderLastHost() != null) {
            additonalParams.put(VirtualMachineProfile.Param.ConsiderLastHost, cmd.getConsiderLastHost().toString());
        }

        return startVirtualMachine(cmd.getId(), cmd.getPodId(), cmd.getClusterId(), cmd.getHostId(), additonalParams, cmd.getDeploymentPlanner()).first();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_REBOOT, eventDescription = "rebooting Vm", async = true)
    public UserVm rebootVirtualMachine(RebootVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();
        Long vmId = cmd.getId();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }

        if (vmInstance.getState() != State.Running) {
            throw new InvalidParameterValueException(String.format("The VM %s (%s) is not running, unable to reboot it",
                    vmInstance.getUuid(), vmInstance.getDisplayNameOrHostName()));
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        checkIfHostOfVMIsInPrepareForMaintenanceState(vmInstance.getHostId(), vmId, "Reboot");

        // If the VM is Volatile in nature, on reboot discard the VM's root disk and create a new root disk for it: by calling restoreVM
        long serviceOfferingId = vmInstance.getServiceOfferingId();
        ServiceOfferingVO offering = serviceOfferingDao.findById(vmInstance.getId(), serviceOfferingId);
        if (offering != null && offering.getRemoved() == null) {
            if (offering.isVolatileVm()) {
                return restoreVMInternal(caller, vmInstance, null);
            }
        } else {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId + " corresponding to the vm");
        }

        Boolean enterSetup = cmd.getBootIntoSetup();
        if (enterSetup != null && enterSetup && !HypervisorType.VMware.equals(vmInstance.getHypervisorType())) {
            throw new InvalidParameterValueException("Booting into a hardware setup menu is not implemented on " + vmInstance.getHypervisorType());
        }

        UserVm userVm = rebootVirtualMachine(CallContext.current().getCallingUserId(), vmId, enterSetup == null ? false : cmd.getBootIntoSetup(), cmd.isForced());
        if (userVm != null ) {
            // update the vmIdCountMap if the vm is in advanced shared network with out services
            final List<NicVO> nics = _nicDao.listByVmId(vmId);
            for (NicVO nic : nics) {
                Network network = _networkModel.getNetwork(nic.getNetworkId());
                if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                    s_logger.debug("Adding vm " +vmId +" nic id "+ nic.getId() +" into vmIdCountMap as part of vm " +
                            "reboot for vm ip fetch ");
                    vmIdCountMap.put(nic.getId(), new VmAndCountDetails(nic.getInstanceId(), VmIpFetchTrialMax.value()));
                }
            }
            return  userVm;
        }
        return  null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_DESTROY, eventDescription = "destroying Vm", async = true)
    public UserVm destroyVm(DestroyVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        CallContext ctx = CallContext.current();
        long vmId = cmd.getId();
        boolean expunge = cmd.getExpunge();

        // When trying to expunge, permission is denied when the caller is not an admin and the AllowUserExpungeRecoverVm is false for the caller.
        if (expunge && !_accountMgr.isAdmin(ctx.getCallingAccount().getId()) && !AllowUserExpungeRecoverVm.valueIn(cmd.getEntityOwnerId())) {
            throw new PermissionDeniedException("Parameter " + ApiConstants.EXPUNGE + " can be passed by Admin only. Or when the allow.user.expunge.recover.vm key is set.");
        }
        // check if VM exists
        UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null || vm.getRemoved() != null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        if (Arrays.asList(State.Destroyed, State.Expunging).contains(vm.getState()) && !expunge) {
            s_logger.debug("Vm id=" + vmId + " is already destroyed");
            return vm;
        }

        // check if vm belongs to AutoScale vm group in Disabled state
        autoScaleManager.checkIfVmActionAllowed(vmId);

        // check if there are active volume snapshots tasks
        s_logger.debug("Checking if there are any ongoing snapshots on the ROOT volumes associated with VM with ID " + vmId);
        if (checkStatusOfVolumeSnapshots(vmId, Volume.Type.ROOT)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on ROOT volume, vm destroy is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volume of type ROOT, for the vm with id " + vmId);

        List<VolumeVO> volumesToBeDeleted = getVolumesFromIds(cmd);

        checkForUnattachedVolumes(vmId, volumesToBeDeleted);
        validateVolumes(volumesToBeDeleted);

        final ControlledEntity[] volumesToDelete = volumesToBeDeleted.toArray(new ControlledEntity[0]);
        _accountMgr.checkAccess(ctx.getCallingAccount(), null, true, volumesToDelete);

        stopVirtualMachine(vmId, VmDestroyForcestop.value());

        // Detach all data disks from VM
        List<VolumeVO> dataVols = _volsDao.findByInstanceAndType(vmId, Volume.Type.DATADISK);
        detachVolumesFromVm(dataVols);

        UserVm destroyedVm = destroyVm(vmId, expunge);
        if (expunge && !expunge(vm)) {
            throw new CloudRuntimeException("Failed to expunge vm " + destroyedVm);
        }

        autoScaleManager.removeVmFromVmGroup(vmId);

        deleteVolumesFromVm(volumesToBeDeleted, expunge);

        if (getDestroyRootVolumeOnVmDestruction(vm.getDomainId())) {
            VolumeVO rootVolume = _volsDao.getInstanceRootVolume(vm.getId());
            if (rootVolume != null) {
                _volService.destroyVolume(rootVolume.getId());
            } else {
                s_logger.warn(String.format("Tried to destroy ROOT volume for VM [%s], but couldn't retrieve it.", vm.getUuid()));
            }
        }

        return destroyedVm;
    }

    private List<VolumeVO> getVolumesFromIds(DestroyVMCmd cmd) {
        List<VolumeVO> volumes = new ArrayList<>();
        if (cmd.getVolumeIds() != null) {
            for (Long volId : cmd.getVolumeIds()) {
                VolumeVO vol = _volsDao.findById(volId);

                if (vol == null) {
                    throw new InvalidParameterValueException("Unable to find volume with ID: " + volId);
                }
                volumes.add(vol);
            }
        }
        return volumes;
    }

    @Override
    @DB
    public InstanceGroupVO createVmGroup(CreateVMGroupCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String groupName = cmd.getGroupName();
        Long projectId = cmd.getProjectId();

        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);
        long accountId = owner.getId();

        // Check if name is already in use by this account
        boolean isNameInUse = _vmGroupDao.isNameInUse(accountId, groupName);

        if (isNameInUse) {
            throw new InvalidParameterValueException("Unable to create vm group, a group with name " + groupName + " already exists for account " + accountId);
        }

        return createVmGroup(groupName, accountId);
    }

    @DB
    private InstanceGroupVO createVmGroup(String groupName, long accountId) {
        Account account = null;
        try {
            account = _accountDao.acquireInLockTable(accountId); // to ensure
            // duplicate
            // vm group
            // names are
            // not
            // created.
            if (account == null) {
                s_logger.warn("Failed to acquire lock on account");
                return null;
            }
            InstanceGroupVO group = _vmGroupDao.findByAccountAndName(accountId, groupName);
            if (group == null) {
                group = new InstanceGroupVO(groupName, accountId);
                group = _vmGroupDao.persist(group);
            }
            return group;
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(accountId);
            }
        }
    }

    @Override
    public boolean deleteVmGroup(DeleteVMGroupCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long groupId = cmd.getId();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId);
        if ((group == null) || (group.getRemoved() != null)) {
            throw new InvalidParameterValueException("unable to find a vm group with id " + groupId);
        }

        _accountMgr.checkAccess(caller, null, true, group);

        return deleteVmGroup(groupId);
    }

    @Override
    public boolean deleteVmGroup(long groupId) {
        InstanceGroupVO group = _vmGroupDao.findById(groupId);
        annotationDao.removeByEntityType(AnnotationService.EntityType.INSTANCE_GROUP.name(), group.getUuid());
        // delete all the mappings from group_vm_map table
        List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByGroupId(groupId);
        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
            SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
            sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
            _groupVMMapDao.expunge(sc);
        }

        if (_vmGroupDao.remove(groupId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @DB
    public boolean addInstanceToGroup(final long userVmId, String groupName) {
        UserVmVO vm = _vmDao.findById(userVmId);

        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(vm.getAccountId(), groupName);
        // Create vm group if the group doesn't exist for this account
        if (group == null) {
            group = createVmGroup(groupName, vm.getAccountId());
        }

        if (group != null) {
            UserVm userVm = _vmDao.acquireInLockTable(userVmId);
            if (userVm == null) {
                s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
            }
            try {
                final InstanceGroupVO groupFinal = group;
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        // don't let the group be deleted when we are assigning vm to
                        // it.
                        InstanceGroupVO ngrpLock = _vmGroupDao.lockRow(groupFinal.getId(), false);
                        if (ngrpLock == null) {
                            s_logger.warn("Failed to acquire lock on vm group id=" + groupFinal.getId() + " name=" + groupFinal.getName());
                            throw new CloudRuntimeException("Failed to acquire lock on vm group id=" + groupFinal.getId() + " name=" + groupFinal.getName());
                        }

                        // Currently don't allow to assign a vm to more than one group
                        if (_groupVMMapDao.listByInstanceId(userVmId) != null) {
                            // Delete all mappings from group_vm_map table
                            List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(userVmId);
                            for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                                SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
                                sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
                                _groupVMMapDao.expunge(sc);
                            }
                        }
                        InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(groupFinal.getId(), userVmId);
                        _groupVMMapDao.persist(groupVmMapVO);

                    }
                });

                return true;
            } finally {
                if (userVm != null) {
                    _vmDao.releaseFromLockTable(userVmId);
                }
            }
        }
        return false;
    }

    @Override
    public InstanceGroupVO getGroupForVm(long vmId) {
        // TODO - in future releases vm can be assigned to multiple groups; but
        // currently return just one group per vm
        try {
            List<InstanceGroupVMMapVO> groupsToVmMap = _groupVMMapDao.listByInstanceId(vmId);

            if (groupsToVmMap != null && groupsToVmMap.size() != 0) {
                InstanceGroupVO group = _vmGroupDao.findById(groupsToVmMap.get(0).getGroupId());
                return group;
            } else {
                return null;
            }
        } catch (Exception e) {
            s_logger.warn("Error trying to get group for a vm: ", e);
            return null;
        }
    }

    @Override
    public void removeInstanceFromInstanceGroup(long vmId) {
        try {
            List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(vmId);
            for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
                sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
                _groupVMMapDao.expunge(sc);
            }
        } catch (Exception e) {
            s_logger.warn("Error trying to remove vm from group: ", e);
        }
    }

    private boolean validPassword(String password) {
        if (password == null || password.length() == 0) {
            return false;
        }
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    public UserVm createBasicSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> securityGroupIdList,
                                                         Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, HTTPMethod httpmethod,
                                                         String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard, List<Long> affinityGroupIdList,
                                                         Map<String, String> customParametes, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
                                                         Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap, Map<String, String> userVmOVFProperties, boolean dynamicScalingEnabled, Long overrideDiskOfferingId) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
    StorageUnavailableException, ResourceAllocationException {

        Account caller = CallContext.current().getCallingAccount();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // Verify that owner can use the service offering
        _accountMgr.checkAccess(owner, serviceOffering, zone);
        _accountMgr.checkAccess(owner, _diskOfferingDao.findById(diskOfferingId), zone);

        // Get default guest network in Basic zone
        Network defaultNetwork = _networkModel.getExclusiveGuestNetwork(zone.getId());

        if (defaultNetwork == null) {
            throw new InvalidParameterValueException("Unable to find a default network to start a vm");
        } else {
            networkList.add(_networkDao.findById(defaultNetwork.getId()));
        }

        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        if (securityGroupIdList != null && isVmWare) {
            throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
        } else if (!isVmWare && _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkModel.canAddDefaultSecurityGroup()) {
            //add the default securityGroup only if no security group is specified
            if (securityGroupIdList == null || securityGroupIdList.isEmpty()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }
                SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(owner.getId());
                if (defaultGroup != null) {
                    securityGroupIdList.add(defaultGroup.getId());
                } else {
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account " + owner + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            owner.getDomainId(), owner.getId(), owner.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod,
                userData, userDataId, userDataDetails, sshKeyPairs, hypervisor, caller, requestedIps, defaultIps, displayVm, keyboard, affinityGroupIdList, customParametes, customId, dhcpOptionMap,
                dataDiskTemplateToDiskOfferingMap, userVmOVFProperties, dynamicScalingEnabled, null, overrideDiskOfferingId);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
                                                            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor,
                                                            HTTPMethod httpmethod, String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard,
                                                            List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
                                                            Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap, Map<String, String> userVmOVFProperties, boolean dynamicScalingEnabled, Long overrideDiskOfferingId, String vmType) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

        Account caller = CallContext.current().getCallingAccount();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();
        boolean isSecurityGroupEnabledNetworkUsed = false;
        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // Verify that owner can use the service offering
        _accountMgr.checkAccess(owner, serviceOffering, zone);
        _accountMgr.checkAccess(owner, _diskOfferingDao.findById(diskOfferingId), zone);

        // If no network is specified, find system security group enabled network
        if (networkIdList == null || networkIdList.isEmpty()) {
            Network networkWithSecurityGroup = _networkModel.getNetworkWithSGWithFreeIPs(zone.getId());
            if (networkWithSecurityGroup == null) {
                throw new InvalidParameterValueException("No network with security enabled is found in zone id=" + zone.getUuid());
            }

            networkList.add(_networkDao.findById(networkWithSecurityGroup.getId()));
            isSecurityGroupEnabledNetworkUsed = true;

        } else if (securityGroupIdList != null && !securityGroupIdList.isEmpty()) {
            if (isVmWare) {
                throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
            }
            // Only one network can be specified, and it should be security group enabled
            if (networkIdList.size() > 1 && template.getHypervisorType() != HypervisorType.KVM && hypervisor != HypervisorType.KVM) {
                throw new InvalidParameterValueException("Only support one network per VM if security group enabled");
            }

            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);

                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkId);
                }

                if (!_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                    throw new InvalidParameterValueException("Network is not security group enabled: " + network.getId());
                }

                _accountMgr.checkAccess(owner, AccessType.UseEntry, false, network);

                networkList.add(network);
            }
            isSecurityGroupEnabledNetworkUsed = true;

        } else {
            // Verify that all the networks are Shared/Guest; can't create combination of SG enabled and disabled networks
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);

                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
                }

                boolean isSecurityGroupEnabled = _networkModel.isSecurityGroupSupportedInNetwork(network);
                if (isSecurityGroupEnabled) {
                    isSecurityGroupEnabledNetworkUsed = true;
                }

                if (!(network.getTrafficType() == TrafficType.Guest && network.getGuestType() == Network.GuestType.Shared)) {
                    throw new InvalidParameterValueException("Can specify only Shared Guest networks when" + " deploy vm in Advance Security Group enabled zone");
                }

                _accountMgr.checkAccess(owner, AccessType.UseEntry, false, network);

                networkList.add(network);
            }
        }

        // if network is security group enabled, and no security group is specified, then add the default security group automatically
        if (isSecurityGroupEnabledNetworkUsed && !isVmWare && _networkModel.canAddDefaultSecurityGroup()) {

            //add the default securityGroup only if no security group is specified
            if (securityGroupIdList == null || securityGroupIdList.isEmpty()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }

                SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(owner.getId());
                if (defaultGroup != null) {
                    securityGroupIdList.add(defaultGroup.getId());
                } else {
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account " + owner + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            owner.getDomainId(), owner.getId(), owner.getAccountName());
                    messageBus.publish(_name, SecurityGroupService.MESSAGE_CREATE_TUNGSTEN_SECURITY_GROUP_EVENT,
                        PublishScope.LOCAL, defaultGroup);
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod,
                userData, userDataId, userDataDetails, sshKeyPairs, hypervisor, caller, requestedIps, defaultIps, displayVm, keyboard, affinityGroupIdList, customParameters, customId, dhcpOptionMap, dataDiskTemplateToDiskOfferingMap,
                userVmOVFProperties, dynamicScalingEnabled, vmType, overrideDiskOfferingId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner,
                                               String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, HTTPMethod httpmethod, String userData,
                                               Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayvm, String keyboard, List<Long> affinityGroupIdList,
                                               Map<String, String> customParametrs, String customId, Map<String, Map<Integer, String>> dhcpOptionsMap, Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
                                               Map<String, String> userVmOVFPropertiesMap, boolean dynamicScalingEnabled, String vmType, Long overrideDiskOfferingId) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
    StorageUnavailableException, ResourceAllocationException {

        Account caller = CallContext.current().getCallingAccount();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // Verify that owner can use the service offering
        _accountMgr.checkAccess(owner, serviceOffering, zone);
        _accountMgr.checkAccess(owner, _diskOfferingDao.findById(diskOfferingId), zone);

        List<HypervisorType> vpcSupportedHTypes = _vpcMgr.getSupportedVpcHypervisors();
        if (networkIdList == null || networkIdList.isEmpty()) {
            NetworkVO defaultNetwork = getDefaultNetwork(zone, owner, false);
            if (defaultNetwork != null) {
                networkList.add(defaultNetwork);
            }
        } else {
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);
                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
                }
                if (network.getVpcId() != null) {
                    // Only ISOs, XenServer, KVM, and VmWare template types are
                    // supported for vpc networks
                    if (template.getFormat() != ImageFormat.ISO && !vpcSupportedHTypes.contains(template.getHypervisorType())) {
                        throw new InvalidParameterValueException("Can't create vm from template with hypervisor " + template.getHypervisorType() + " in vpc network " + network);
                    } else if (template.getFormat() == ImageFormat.ISO && !vpcSupportedHTypes.contains(hypervisor)) {
                        // Only XenServer, KVM, and VMware hypervisors are supported
                        // for vpc networks
                        throw new InvalidParameterValueException("Can't create vm of hypervisor type " + hypervisor + " in vpc network");

                    }
                }

                _networkModel.checkNetworkPermissions(owner, network);

                // don't allow to use system networks
                NetworkOffering networkOffering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                if (networkOffering.isSystemOnly()) {
                    throw new InvalidParameterValueException("Network id=" + networkId + " is system only and can't be used for vm deployment");
                }
                networkList.add(network);
            }
        }
        verifyExtraDhcpOptionsNetwork(dhcpOptionsMap, networkList);
        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, null, group, httpmethod, userData,
                userDataId, userDataDetails, sshKeyPairs, hypervisor, caller, requestedIps, defaultIps, displayvm, keyboard, affinityGroupIdList, customParametrs, customId, dhcpOptionsMap,
                dataDiskTemplateToDiskOfferingMap, userVmOVFPropertiesMap, dynamicScalingEnabled, vmType, overrideDiskOfferingId);
    }

    private NetworkVO getNetworkToAddToNetworkList(VirtualMachineTemplate template, Account owner, HypervisorType hypervisor,
            List<HypervisorType> vpcSupportedHTypes, Long networkId) {
        NetworkVO network = _networkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find network by id " + networkId);
        }
        if (network.getVpcId() != null) {
            // Only ISOs, XenServer, KVM, and VmWare template types are
            // supported for vpc networks
            if (template.getFormat() != ImageFormat.ISO && !vpcSupportedHTypes.contains(template.getHypervisorType())) {
                throw new InvalidParameterValueException("Can't create vm from template with hypervisor " + template.getHypervisorType() + " in vpc network " + network);
            } else if (template.getFormat() == ImageFormat.ISO && !vpcSupportedHTypes.contains(hypervisor)) {
                // Only XenServer, KVM, and VMware hypervisors are supported
                // for vpc networks
                throw new InvalidParameterValueException("Can't create vm of hypervisor type " + hypervisor + " in vpc network");
            }
        }

        _networkModel.checkNetworkPermissions(owner, network);

        // don't allow to use system networks
        NetworkOffering networkOffering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
        if (networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Network id=" + networkId + " is system only and can't be used for vm deployment");
        }
        return network;
    }

    private NetworkVO getDefaultNetwork(DataCenter zone, Account owner, boolean selectAny) throws InsufficientCapacityException, ResourceAllocationException {
        NetworkVO defaultNetwork = null;

        // if no network is passed in
        // Check if default virtual network offering has
        // Availability=Required. If it's true, search for corresponding
        // network
        // * if network is found, use it. If more than 1 virtual network is
        // found, throw an error
        // * if network is not found, create a new one and use it

        List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
        if (requiredOfferings.size() < 1) {
            throw new InvalidParameterValueException("Unable to find network offering with availability=" + Availability.Required
                    + " to automatically create the network as a part of vm creation");
        }

        if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
            // get Virtual networks
            List<? extends Network> virtualNetworks = _networkModel.listNetworksForAccount(owner.getId(), zone.getId(), Network.GuestType.Isolated);
            if (virtualNetworks == null) {
                throw new InvalidParameterValueException("No (virtual) networks are found for account " + owner);
            }
            if (virtualNetworks.isEmpty()) {
                defaultNetwork = createDefaultNetworkForAccount(zone, owner, requiredOfferings);
            } else if (virtualNetworks.size() > 1 && !selectAny) {
                throw new InvalidParameterValueException("More than 1 default Isolated networks are found for account " + owner + "; please specify networkIds");
            } else {
                defaultNetwork = _networkDao.findById(virtualNetworks.get(0).getId());
            }
        } else {
            throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled);
        }

        return defaultNetwork;
    }

    private NetworkVO createDefaultNetworkForAccount(DataCenter zone, Account owner, List<NetworkOfferingVO> requiredOfferings)
            throws InsufficientCapacityException, ResourceAllocationException {
        NetworkVO defaultNetwork = null;
        long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
        // Validate physical network
        PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                    + requiredOfferings.get(0).getTags());
        }
        s_logger.debug("Creating network for account " + owner + " from the network offering id=" + requiredOfferings.get(0).getId() + " as a part of deployVM process");
        Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName() + "-network",
                null, null, null, false, null, owner, null, physicalNetwork, zone.getId(), ACLType.Account, null, null, null, null, true, null, null,
                null, null, null, null, null, null, null, null);
        if (newNetwork != null) {
            defaultNetwork = _networkDao.findById(newNetwork.getId());
        }
        return defaultNetwork;
    }

    private void verifyExtraDhcpOptionsNetwork(Map<String, Map<Integer, String>> dhcpOptionsMap, List<NetworkVO> networkList) throws InvalidParameterValueException {
        if (dhcpOptionsMap != null) {
            for (String networkUuid : dhcpOptionsMap.keySet()) {
                boolean networkFound = false;
                for (NetworkVO network : networkList) {
                    if (network.getUuid().equals(networkUuid)) {
                        networkFound = true;
                        break;
                    }
                }

                if (!networkFound) {
                    throw new InvalidParameterValueException("VM does not has a nic in the Network (" + networkUuid + ") that is specified in the extra dhcp options.");
                }
            }
        }
    }

    public void checkNameForRFCCompliance(String name) {
        if (!NetUtils.verifyDomainNameLabel(name, true)) {
            throw new InvalidParameterValueException("Invalid name. Vm name can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }
    }

    @DB
    private UserVm createVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate tmplt, String hostName, String displayName, Account owner,
                                        Long diskOfferingId, Long diskSize, List<NetworkVO> networkList, List<Long> securityGroupIdList, String group, HTTPMethod httpmethod, String userData,
                                        Long userDataId, String userDataDetails, List<String> sshKeyPairs, HypervisorType hypervisor, Account caller, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean isDisplayVm, String keyboard,
                                        List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
                                        Map<Long, DiskOffering> datadiskTemplateToDiskOfferringMap,
                                        Map<String, String> userVmOVFPropertiesMap, boolean dynamicScalingEnabled, String vmType, Long overrideDiskOfferingId) throws InsufficientCapacityException, ResourceUnavailableException,
    ConcurrentOperationException, StorageUnavailableException, ResourceAllocationException {

        _accountMgr.checkAccess(caller, null, true, owner);

        if (owner.getState() == Account.State.DISABLED) {
            throw new PermissionDeniedException("The owner of vm to deploy is disabled: " + owner);
        }
        VMTemplateVO template = _templateDao.findById(tmplt.getId());
        if (template != null) {
            _templateDao.loadDetails(template);
        }

        HypervisorType hypervisorType = null;
        if (template.getHypervisorType() == null || template.getHypervisorType() == HypervisorType.None) {
            if (hypervisor == null || hypervisor == HypervisorType.None) {
                throw new InvalidParameterValueException("hypervisor parameter is needed to deploy VM or the hypervisor parameter value passed is invalid");
            }
            hypervisorType = hypervisor;
        } else {
            if (hypervisor != null && hypervisor != HypervisorType.None && hypervisor != template.getHypervisorType()) {
                throw new InvalidParameterValueException("Hypervisor passed to the deployVm call, is different from the hypervisor type of the template");
            }
            hypervisorType = template.getHypervisorType();
        }

        long accountId = owner.getId();

        assert !(requestedIps != null && (defaultIps.getIp4Address() != null || defaultIps.getIp6Address() != null)) : "requestedIp list and defaultNetworkIp should never be specified together";

        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException(
                    "Cannot perform this operation, Zone is currently disabled: "
                            + zone.getId());
        }

        // check if zone is dedicated
        DedicatedResourceVO dedicatedZone = _dedicatedDao.findByZoneId(zone.getId());
        if (dedicatedZone != null) {
            DomainVO domain = _domainDao.findById(dedicatedZone.getDomainId());
            if (domain == null) {
                throw new CloudRuntimeException("Unable to find the domain " + zone.getDomainId() + " for the zone: " + zone);
            }
            // check that caller can operate with domain
            _configMgr.checkZoneAccess(caller, zone);
            // check that vm owner can create vm in the domain
            _configMgr.checkZoneAccess(owner, zone);
        }

        ServiceOfferingVO offering = serviceOfferingDao.findById(serviceOffering.getId());

        if (offering.isDynamic()) {
            offering.setDynamicFlag(true);
            validateCustomParameters(offering, customParameters);
            offering = serviceOfferingDao.getComputeOffering(offering, customParameters);
        } else {
            validateOfferingMaxResource(offering);
        }
        // check if account/domain is with in resource limits to create a new vm
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();

        Long rootDiskOfferingId = offering.getDiskOfferingId();
        if (isIso) {
            if (diskOfferingId == null) {
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(rootDiskOfferingId);
                if (diskOffering.isComputeOnly()) {
                    throw new InvalidParameterValueException("Installing from ISO requires a disk offering to be specified for the root disk.");
                }
            } else {
                rootDiskOfferingId = diskOfferingId;
                diskOfferingId = null;
            }
            if (!customParameters.containsKey(VmDetailConstants.ROOT_DISK_SIZE)) {
                customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(diskSize));
            }
        }
        if (!offering.getDiskOfferingStrictness() && overrideDiskOfferingId != null) {
            rootDiskOfferingId = overrideDiskOfferingId;
        }

        DiskOfferingVO rootdiskOffering = _diskOfferingDao.findById(rootDiskOfferingId);
        long volumesSize = configureCustomRootDiskSize(customParameters, template, hypervisorType, rootdiskOffering);

        if (rootdiskOffering.getEncrypt() && hypervisorType != HypervisorType.KVM) {
            throw new InvalidParameterValueException("Root volume encryption is not supported for hypervisor type " + hypervisorType);
        }

        if (!isIso && diskOfferingId != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            volumesSize += verifyAndGetDiskSize(diskOffering, diskSize);
        }
        UserVm vm = getCheckedUserVmResource(zone, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod, userData, userDataId, userDataDetails, sshKeyPairs, caller, requestedIps, defaultIps, isDisplayVm, keyboard, affinityGroupIdList, customParameters, customId, dhcpOptionMap, datadiskTemplateToDiskOfferringMap, userVmOVFPropertiesMap, dynamicScalingEnabled, vmType, template, hypervisorType, accountId, offering, isIso, rootDiskOfferingId, volumesSize);

        _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);

        if (affinityGroupIdList != null && !affinityGroupIdList.isEmpty()) {
            _affinityGroupVMMapDao.updateMap(vm.getId(), affinityGroupIdList);
        }

        CallContext.current().putContextParameter(VirtualMachine.class, vm.getUuid());
        return vm;
    }
    private UserVm getCheckedUserVmResource(DataCenter zone, String hostName, String displayName, Account owner, Long diskOfferingId, Long diskSize, List<NetworkVO> networkList, List<Long> securityGroupIdList, String group, HTTPMethod httpmethod, String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Account caller, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean isDisplayVm, String keyboard, List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap, Map<Long, DiskOffering> datadiskTemplateToDiskOfferringMap, Map<String, String> userVmOVFPropertiesMap, boolean dynamicScalingEnabled, String vmType, VMTemplateVO template, HypervisorType hypervisorType, long accountId, ServiceOfferingVO offering, boolean isIso, Long rootDiskOfferingId, long volumesSize) throws ResourceAllocationException, StorageUnavailableException, InsufficientCapacityException {
        if (!VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            try (CheckedReservation vmReservation = new CheckedReservation(owner, ResourceType.user_vm, 1l, reservationDao, resourceLimitService);
                 CheckedReservation cpuReservation = new CheckedReservation(owner, ResourceType.cpu, Long.valueOf(offering.getCpu()), reservationDao, resourceLimitService);
                 CheckedReservation memReservation = new CheckedReservation(owner, ResourceType.memory, Long.valueOf(offering.getRamSize()), reservationDao, resourceLimitService);
            ) {
                return getUncheckedUserVmResource(zone, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod, userData, userDataId, userDataDetails, sshKeyPairs, caller, requestedIps, defaultIps, isDisplayVm, keyboard, affinityGroupIdList, customParameters, customId, dhcpOptionMap, datadiskTemplateToDiskOfferringMap, userVmOVFPropertiesMap, dynamicScalingEnabled, vmType, template, hypervisorType, accountId, offering, isIso, rootDiskOfferingId, volumesSize);
            } catch (ResourceAllocationException | CloudRuntimeException  e) {
                throw e;
            } catch (Exception e) {
                s_logger.error("error during resource reservation and allocation", e);
                throw new CloudRuntimeException(e);
            }

        } else {
            return getUncheckedUserVmResource(zone, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod, userData, userDataId, userDataDetails, sshKeyPairs, caller, requestedIps, defaultIps, isDisplayVm, keyboard, affinityGroupIdList, customParameters, customId, dhcpOptionMap, datadiskTemplateToDiskOfferringMap, userVmOVFPropertiesMap, dynamicScalingEnabled, vmType, template, hypervisorType, accountId, offering, isIso, rootDiskOfferingId, volumesSize);
        }
    }

    private UserVm getUncheckedUserVmResource(DataCenter zone, String hostName, String displayName, Account owner, Long diskOfferingId, Long diskSize, List<NetworkVO> networkList, List<Long> securityGroupIdList, String group, HTTPMethod httpmethod, String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Account caller, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean isDisplayVm, String keyboard, List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap, Map<Long, DiskOffering> datadiskTemplateToDiskOfferringMap, Map<String, String> userVmOVFPropertiesMap, boolean dynamicScalingEnabled, String vmType, VMTemplateVO template, HypervisorType hypervisorType, long accountId, ServiceOfferingVO offering, boolean isIso, Long rootDiskOfferingId, long volumesSize) throws ResourceAllocationException, StorageUnavailableException, InsufficientCapacityException {
        try (CheckedReservation volumeReservation = new CheckedReservation(owner, ResourceType.volume, (isIso || diskOfferingId == null ? 1l : 2), reservationDao, resourceLimitService);
             CheckedReservation primaryStorageReservation = new CheckedReservation(owner, ResourceType.primary_storage, volumesSize, reservationDao, resourceLimitService)) {

            // verify security group ids
            if (securityGroupIdList != null) {
                for (Long securityGroupId : securityGroupIdList) {
                    SecurityGroup sg = _securityGroupDao.findById(securityGroupId);
                    if (sg == null) {
                        throw new InvalidParameterValueException("Unable to find security group by id " + securityGroupId);
                    } else {
                        // verify permissions
                        _accountMgr.checkAccess(caller, null, true, owner, sg);
                    }
                }
            }

            if (datadiskTemplateToDiskOfferringMap != null && !datadiskTemplateToDiskOfferringMap.isEmpty()) {
                for (Entry<Long, DiskOffering> datadiskTemplateToDiskOffering : datadiskTemplateToDiskOfferringMap.entrySet()) {
                    VMTemplateVO dataDiskTemplate = _templateDao.findById(datadiskTemplateToDiskOffering.getKey());
                    DiskOffering dataDiskOffering = datadiskTemplateToDiskOffering.getValue();

                    if (dataDiskTemplate == null
                            || (!dataDiskTemplate.getTemplateType().equals(TemplateType.DATADISK)) && (dataDiskTemplate.getState().equals(VirtualMachineTemplate.State.Active))) {
                        throw new InvalidParameterValueException("Invalid template id specified for Datadisk template" + datadiskTemplateToDiskOffering.getKey());
                    }
                    long dataDiskTemplateId = datadiskTemplateToDiskOffering.getKey();
                    if (!dataDiskTemplate.getParentTemplateId().equals(template.getId())) {
                        throw new InvalidParameterValueException("Invalid Datadisk template. Specified Datadisk template" + dataDiskTemplateId
                                + " doesn't belong to template " + template.getId());
                    }
                    if (dataDiskOffering == null) {
                        throw new InvalidParameterValueException("Invalid disk offering id " + datadiskTemplateToDiskOffering.getValue().getId() +
                                " specified for datadisk template " + dataDiskTemplateId);
                    }
                    if (dataDiskOffering.isCustomized()) {
                        throw new InvalidParameterValueException("Invalid disk offering id " + dataDiskOffering.getId() + " specified for datadisk template " +
                                dataDiskTemplateId + ". Custom Disk offerings are not supported for Datadisk templates");
                    }
                    if (dataDiskOffering.getDiskSize() < dataDiskTemplate.getSize()) {
                        throw new InvalidParameterValueException("Invalid disk offering id " + dataDiskOffering.getId() + " specified for datadisk template " +
                                dataDiskTemplateId + ". Disk offering size should be greater than or equal to the template size");
                    }
                    _templateDao.loadDetails(dataDiskTemplate);
                    _resourceLimitMgr.checkResourceLimit(owner, ResourceType.volume, 1);
                    _resourceLimitMgr.checkResourceLimit(owner, ResourceType.primary_storage, dataDiskOffering.getDiskSize());
                }
            }

            // check that the affinity groups exist
            if (affinityGroupIdList != null) {
                for (Long affinityGroupId : affinityGroupIdList) {
                    AffinityGroupVO ag = _affinityGroupDao.findById(affinityGroupId);
                    if (ag == null) {
                        throw new InvalidParameterValueException("Unable to find affinity group " + ag);
                    } else if (!_affinityGroupService.isAffinityGroupProcessorAvailable(ag.getType())) {
                        throw new InvalidParameterValueException("Affinity group type is not supported for group: " + ag + " ,type: " + ag.getType()
                                + " , Please try again after removing the affinity group");
                    } else {
                        // verify permissions
                        if (ag.getAclType() == ACLType.Domain) {
                            _accountMgr.checkAccess(caller, null, false, owner, ag);
                            // Root admin has access to both VM and AG by default,
                            // but
                            // make sure the owner of these entities is same
                            if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getId())) {
                                if (!_affinityGroupService.isAffinityGroupAvailableInDomain(ag.getId(), owner.getDomainId())) {
                                    throw new PermissionDeniedException("Affinity Group " + ag + " does not belong to the VM's domain");
                                }
                            }
                        } else {
                            _accountMgr.checkAccess(caller, null, true, owner, ag);
                            // Root admin has access to both VM and AG by default,
                            // but
                            // make sure the owner of these entities is same
                            if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getId())) {
                                if (ag.getAccountId() != owner.getAccountId()) {
                                    throw new PermissionDeniedException("Affinity Group " + ag + " does not belong to the VM's account");
                                }
                            }
                        }
                    }
                }
            }

            if (hypervisorType != HypervisorType.BareMetal) {
                // check if we have available pools for vm deployment
                long availablePools = _storagePoolDao.countPoolsByStatus(StoragePoolStatus.Up);
                if (availablePools < 1) {
                    throw new StorageUnavailableException("There are no available pools in the UP state for vm deployment", -1);
                }
            }

            if (template.getTemplateType().equals(TemplateType.SYSTEM) && !CKS_NODE.equals(vmType)) {
                throw new InvalidParameterValueException("Unable to use system template " + template.getId() + " to deploy a user vm");
            }
            List<VMTemplateZoneVO> listZoneTemplate = _templateZoneDao.listByZoneTemplate(zone.getId(), template.getId());
            if (listZoneTemplate == null || listZoneTemplate.isEmpty()) {
                throw new InvalidParameterValueException("The template " + template.getId() + " is not available for use");
            }

            if (isIso && !template.isBootable()) {
                throw new InvalidParameterValueException("Installing from ISO requires an ISO that is bootable: " + template.getId());
            }

            // Check templates permissions
            _accountMgr.checkAccess(owner, AccessType.UseEntry, false, template);

            // check if the user data is correct
            userData = validateUserData(userData, httpmethod);

            // Find an SSH public key corresponding to the key pair name, if one is
            // given
            String sshPublicKeys = "";
            String keypairnames = "";
            if (!sshKeyPairs.isEmpty()) {
                List<SSHKeyPairVO> pairs = _sshKeyPairDao.findByNames(owner.getAccountId(), owner.getDomainId(), sshKeyPairs);
                if (pairs == null || pairs.size() != sshKeyPairs.size()) {
                    throw new InvalidParameterValueException("Not all specified keyparis exist");
                }

                sshPublicKeys = pairs.stream().map(p -> p.getPublicKey()).collect(Collectors.joining("\n"));
                keypairnames = String.join(",", sshKeyPairs);
            }

            LinkedHashMap<String, List<NicProfile>> networkNicMap = new LinkedHashMap<>();

            short defaultNetworkNumber = 0;
            boolean securityGroupEnabled = false;
            int networkIndex = 0;
            for (NetworkVO network : networkList) {
                if ((network.getDataCenterId() != zone.getId())) {
                    if (!network.isStrechedL2Network()) {
                        throw new InvalidParameterValueException("Network id=" + network.getId() +
                                " doesn't belong to zone " + zone.getId());
                    }

                    NetworkOffering ntwkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
                    Long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), ntwkOffering.getTags(), ntwkOffering.getTrafficType());

                    String provider = _ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Service.Connectivity);
                    if (!_networkModel.isProviderEnabledInPhysicalNetwork(physicalNetworkId, provider)) {
                        throw new InvalidParameterValueException("Network in which is VM getting deployed could not be" +
                                " streched to the zone, as we could not find a valid physical network");
                    }
                }

                _accountMgr.checkAccess(owner, AccessType.UseEntry, false, network);

                IpAddresses requestedIpPair = null;
                if (requestedIps != null && !requestedIps.isEmpty()) {
                    requestedIpPair = requestedIps.get(network.getId());
                }

                if (requestedIpPair == null) {
                    requestedIpPair = new IpAddresses(null, null);
                } else {
                    _networkModel.checkRequestedIpAddresses(network.getId(), requestedIpPair);
                }

                NicProfile profile = new NicProfile(requestedIpPair.getIp4Address(), requestedIpPair.getIp6Address(), requestedIpPair.getMacAddress());
                profile.setOrderIndex(networkIndex);
                if (defaultNetworkNumber == 0) {
                    defaultNetworkNumber++;
                    // if user requested specific ip for default network, add it
                    if (defaultIps.getIp4Address() != null || defaultIps.getIp6Address() != null) {
                        _networkModel.checkRequestedIpAddresses(network.getId(), defaultIps);
                        profile = new NicProfile(defaultIps.getIp4Address(), defaultIps.getIp6Address());
                    } else if (defaultIps.getMacAddress() != null) {
                        profile = new NicProfile(null, null, defaultIps.getMacAddress());
                    }

                    profile.setDefaultNic(true);
                    if (!_networkModel.areServicesSupportedInNetwork(network.getId(), new Service[]{Service.UserData})) {
                        if ((userData != null) && (!userData.isEmpty())) {
                            throw new InvalidParameterValueException(String.format("Unable to deploy VM as UserData is provided while deploying the VM, but there is no support for %s service in the default network %s/%s.", Service.UserData.getName(), network.getName(), network.getUuid()));
                        }

                        if ((sshPublicKeys != null) && (!sshPublicKeys.isEmpty())) {
                            throw new InvalidParameterValueException(String.format("Unable to deploy VM as SSH keypair is provided while deploying the VM, but there is no support for %s service in the default network %s/%s", Service.UserData.getName(), network.getName(), network.getUuid()));
                        }

                        if (template.isEnablePassword()) {
                            throw new InvalidParameterValueException(String.format("Unable to deploy VM as template %s is password enabled, but there is no support for %s service in the default network %s/%s", template.getId(), Service.UserData.getName(), network.getName(), network.getUuid()));
                        }
                    }
                }

                if (_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                    securityGroupEnabled = true;
                }
                List<NicProfile> profiles = networkNicMap.get(network.getUuid());
                if (CollectionUtils.isEmpty(profiles)) {
                    profiles = new ArrayList<>();
                }
                profiles.add(profile);
                networkNicMap.put(network.getUuid(), profiles);
                networkIndex++;
            }

            if (securityGroupIdList != null && !securityGroupIdList.isEmpty() && !securityGroupEnabled) {
                throw new InvalidParameterValueException("Unable to deploy vm with security groups as SecurityGroup service is not enabled for the vm's network");
            }

            // Verify network information - network default network has to be set;
            // and vm can't have more than one default network
            // This is a part of business logic because default network is required
            // by Agent Manager in order to configure default
            // gateway for the vm
            if (defaultNetworkNumber == 0) {
                throw new InvalidParameterValueException("At least 1 default network has to be specified for the vm");
            } else if (defaultNetworkNumber > 1) {
                throw new InvalidParameterValueException("Only 1 default network per vm is supported");
            }

            long id = _vmDao.getNextInSequence(Long.class, "id");

            if (hostName != null) {
                // Check is hostName is RFC compliant
                checkNameForRFCCompliance(hostName);
            }

            String instanceName = null;
            String instanceSuffix = _instance;
            String uuidName = _uuidMgr.generateUuid(UserVm.class, customId);
            if (_instanceNameFlag && HypervisorType.VMware.equals(hypervisorType)) {
                if (StringUtils.isNotEmpty(hostName)) {
                    instanceSuffix = hostName;
                }
                if (hostName == null) {
                    if (displayName != null) {
                        hostName = displayName;
                    } else {
                        hostName = generateHostName(uuidName);
                    }
                }
                // If global config vm.instancename.flag is set to true, then CS will set guest VM's name as it appears on the hypervisor, to its hostname.
                // In case of VMware since VM name must be unique within a DC, check if VM with the same hostname already exists in the zone.
                VMInstanceVO vmByHostName = _vmInstanceDao.findVMByHostNameInZone(hostName, zone.getId());
                if (vmByHostName != null && vmByHostName.getState() != State.Expunging) {
                    throw new InvalidParameterValueException("There already exists a VM by the name: " + hostName + ".");
                }
            } else {
                if (hostName == null) {
                    //Generate name using uuid and instance.name global config
                    hostName = generateHostName(uuidName);
                }
            }

            if (hostName != null) {
                // Check is hostName is RFC compliant
                checkNameForRFCCompliance(hostName);
            }
            instanceName = VirtualMachineName.getVmName(id, owner.getId(), instanceSuffix);
            if (_instanceNameFlag && HypervisorType.VMware.equals(hypervisorType) && !instanceSuffix.equals(_instance)) {
                customParameters.put(VmDetailConstants.NAME_ON_HYPERVISOR, instanceName);
            }

            // Check if VM with instanceName already exists.
            VMInstanceVO vmObj = _vmInstanceDao.findVMByInstanceName(instanceName);
            if (vmObj != null && vmObj.getState() != State.Expunging) {
                throw new InvalidParameterValueException("There already exists a VM by the display name supplied");
            }

            checkIfHostNameUniqueInNtwkDomain(hostName, networkList);

            long userId = CallContext.current().getCallingUserId();
            if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
                List<UserVO> userVOs = _userDao.listByAccount(owner.getAccountId());
                if (!userVOs.isEmpty()) {
                    userId = userVOs.get(0).getId();
                }
            }

            dynamicScalingEnabled = dynamicScalingEnabled && checkIfDynamicScalingCanBeEnabled(null, offering, template, zone.getId());

            UserVmVO vm = commitUserVm(zone, template, hostName, displayName, owner, diskOfferingId, diskSize, userData, userDataId, userDataDetails, caller, isDisplayVm, keyboard, accountId, userId, offering,
                    isIso, sshPublicKeys, networkNicMap, id, instanceName, uuidName, hypervisorType, customParameters, dhcpOptionMap,
                    datadiskTemplateToDiskOfferringMap, userVmOVFPropertiesMap, dynamicScalingEnabled, vmType, rootDiskOfferingId, keypairnames);

            assignInstanceToGroup(group, id);
            return vm;
        } catch (ResourceAllocationException | CloudRuntimeException  e) {
            throw e;
        } catch (Exception e) {
            s_logger.error("error during resource reservation and allocation", e);
            throw new CloudRuntimeException(e);
        }
    }

    private void assignInstanceToGroup(String group, long id) {
        // Assign instance to the group
        try {
            if (group != null) {
                boolean addToGroup = addInstanceToGroup(Long.valueOf(id), group);
                if (!addToGroup) {
                    throw new CloudRuntimeException("Unable to assign Vm to the group " + group);
                }
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unable to assign Vm to the group " + group);
        }
    }

    private long verifyAndGetDiskSize(DiskOfferingVO diskOffering, Long diskSize) {
        long size = 0l;
        if (diskOffering == null) {
            throw new InvalidParameterValueException("Specified disk offering cannot be found");
        }
        if (diskOffering.isCustomized() && !diskOffering.isComputeOnly()) {
            if (diskSize == null) {
                throw new InvalidParameterValueException("This disk offering requires a custom size specified");
            }
            _volumeService.validateCustomDiskOfferingSizeRange(diskSize);
            size = diskSize * GiB_TO_BYTES;
        } else {
            size = diskOffering.getDiskSize();
        }
        _volumeService.validateVolumeSizeInBytes(size);
        return size;
    }

    @Override
    public boolean checkIfDynamicScalingCanBeEnabled(VirtualMachine vm, ServiceOffering offering, VirtualMachineTemplate template, Long zoneId) {
        boolean canEnableDynamicScaling = (vm != null ? vm.isDynamicallyScalable() : true) && offering.isDynamicScalingEnabled() && template.isDynamicallyScalable() && UserVmManager.EnableDynamicallyScaleVm.valueIn(zoneId);
        if (!canEnableDynamicScaling) {
            s_logger.info("VM cannot be configured to be dynamically scalable if any of the service offering's dynamic scaling property, template's dynamic scaling property or global setting is false");
        }

        return canEnableDynamicScaling;
    }

    /**
     * Configures the Root disk size via User`s custom parameters.
     * If the Service Offering has the Root Disk size field configured then the User`s root disk custom parameter is overwritten by the service offering.
     */
    protected long configureCustomRootDiskSize(Map<String, String> customParameters, VMTemplateVO template, HypervisorType hypervisorType, DiskOfferingVO rootDiskOffering) {
        verifyIfHypervisorSupportsRootdiskSizeOverride(hypervisorType);
        long rootDiskSizeInBytes = verifyAndGetDiskSize(rootDiskOffering, NumbersUtil.parseLong(customParameters.get(VmDetailConstants.ROOT_DISK_SIZE), -1));
        if (rootDiskSizeInBytes > 0) { //if the size at DiskOffering is not zero then the Service Offering had it configured, it holds priority over the User custom size
            _volumeService.validateVolumeSizeInBytes(rootDiskSizeInBytes);
            long rootDiskSizeInGiB = rootDiskSizeInBytes / GiB_TO_BYTES;
            customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(rootDiskSizeInGiB));
            return rootDiskSizeInBytes;
        }

        if (customParameters.containsKey(VmDetailConstants.ROOT_DISK_SIZE)) {
            Long rootDiskSize = NumbersUtil.parseLong(customParameters.get(VmDetailConstants.ROOT_DISK_SIZE), -1);
            if (rootDiskSize <= 0) {
                throw new InvalidParameterValueException("Root disk size should be a positive number.");
            }
            rootDiskSize *= GiB_TO_BYTES;
            _volumeService.validateVolumeSizeInBytes(rootDiskSize);
            return rootDiskSize;
        } else {
            // For baremetal, size can be 0 (zero)
            Long templateSize = _templateDao.findById(template.getId()).getSize();
            if (templateSize != null) {
                return templateSize;
            }
        }
        return 0;
    }

    /**
     * Only KVM, XenServer and VMware supports rootdisksize override
     * @throws InvalidParameterValueException if the hypervisor does not support rootdisksize override
     */
    protected void verifyIfHypervisorSupportsRootdiskSizeOverride(HypervisorType hypervisorType) {
        if (!(hypervisorType == HypervisorType.KVM || hypervisorType == HypervisorType.XenServer || hypervisorType == HypervisorType.VMware || hypervisorType == HypervisorType.Simulator)) {
            throw new InvalidParameterValueException("Hypervisor " + hypervisorType + " does not support rootdisksize override");
        }
    }

    private void checkIfHostNameUniqueInNtwkDomain(String hostName, List<? extends Network> networkList) {
        // Check that hostName is unique in the network domain
        Map<String, List<Long>> ntwkDomains = new HashMap<String, List<Long>>();
        for (Network network : networkList) {
            String ntwkDomain = network.getNetworkDomain();
            if (!ntwkDomains.containsKey(ntwkDomain)) {
                List<Long> ntwkIds = new ArrayList<Long>();
                ntwkIds.add(network.getId());
                ntwkDomains.put(ntwkDomain, ntwkIds);
            } else {
                List<Long> ntwkIds = ntwkDomains.get(ntwkDomain);
                ntwkIds.add(network.getId());
                ntwkDomains.put(ntwkDomain, ntwkIds);
            }
        }

        for (Entry<String, List<Long>> ntwkDomain : ntwkDomains.entrySet()) {
            for (Long ntwkId : ntwkDomain.getValue()) {
                // * get all vms hostNames in the network
                List<String> hostNames = _vmInstanceDao.listDistinctHostNames(ntwkId);
                // * verify that there are no duplicates
                if (hostNames.contains(hostName)) {
                    throw new InvalidParameterValueException("The vm with hostName " + hostName + " already exists in the network domain: " + ntwkDomain.getKey() + "; network="
                            + ((_networkModel.getNetwork(ntwkId) != null) ? _networkModel.getNetwork(ntwkId).getName() : "<unknown>"));
                }
            }
        }
    }

    private String generateHostName(String uuidName) {
        return _instance + "-" + uuidName;
    }

    private UserVmVO commitUserVm(final boolean isImport, final DataCenter zone, final Host host, final Host lastHost, final VirtualMachineTemplate template, final String hostName, final String displayName, final Account owner,
                                  final Long diskOfferingId, final Long diskSize, final String userData, Long userDataId, String userDataDetails, final Boolean isDisplayVm, final String keyboard,
                                  final long accountId, final long userId, final ServiceOffering offering, final boolean isIso, final String sshPublicKeys, final LinkedHashMap<String, List<NicProfile>> networkNicMap,
                                  final long id, final String instanceName, final String uuidName, final HypervisorType hypervisorType, final Map<String, String> customParameters,
                                  final Map<String, Map<Integer, String>> extraDhcpOptionMap, final Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
                                  final Map<String, String> userVmOVFPropertiesMap, final VirtualMachine.PowerState powerState, final boolean dynamicScalingEnabled, String vmType, final Long rootDiskOfferingId, String sshkeypairs) throws InsufficientCapacityException {
        return Transaction.execute(new TransactionCallbackWithException<UserVmVO, InsufficientCapacityException>() {
            @Override
            public UserVmVO doInTransaction(TransactionStatus status) throws InsufficientCapacityException {
                UserVmVO vm = new UserVmVO(id, instanceName, displayName, template.getId(), hypervisorType, template.getGuestOSId(), offering.isOfferHA(),
                        offering.getLimitCpuUse(), owner.getDomainId(), owner.getId(), userId, offering.getId(), userData, userDataId, userDataDetails, hostName);
                vm.setUuid(uuidName);
                vm.setDynamicallyScalable(dynamicScalingEnabled);

                Map<String, String> details = template.getDetails();
                if (details != null && !details.isEmpty()) {
                    vm.details.putAll(details);
                }

                if (StringUtils.isNotBlank(sshPublicKeys)) {
                    vm.setDetail(VmDetailConstants.SSH_PUBLIC_KEY, sshPublicKeys);
                }

                if (StringUtils.isNotBlank(sshkeypairs)) {
                    vm.setDetail(VmDetailConstants.SSH_KEY_PAIR_NAMES, sshkeypairs);
                }

                if (keyboard != null && !keyboard.isEmpty()) {
                    vm.setDetail(VmDetailConstants.KEYBOARD, keyboard);
                }

                if (!isImport && isIso) {
                    vm.setIsoId(template.getId());
                }

                long guestOSId = template.getGuestOSId();
                GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
                long guestOSCategoryId = guestOS.getCategoryId();
                GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
                if (hypervisorType.equals(HypervisorType.VMware)) {
                    updateVMDiskController(vm, customParameters, guestOS);
                }

                Long rootDiskSize = null;
                // custom root disk size, resizes base template to larger size
                if (customParameters.containsKey(VmDetailConstants.ROOT_DISK_SIZE)) {
                    // already verified for positive number
                    rootDiskSize = Long.parseLong(customParameters.get(VmDetailConstants.ROOT_DISK_SIZE));

                    VMTemplateVO templateVO = _templateDao.findById(template.getId());
                    if (templateVO == null) {
                        throw new InvalidParameterValueException("Unable to look up template by id " + template.getId());
                    }

                    validateRootDiskResize(hypervisorType, rootDiskSize, templateVO, vm, customParameters);
                }

                if (isDisplayVm != null) {
                    vm.setDisplayVm(isDisplayVm);
                } else {
                    vm.setDisplayVm(true);
                }

                if (isImport) {
                    vm.setDataCenterId(zone.getId());
                    vm.setHostId(host.getId());
                    if (lastHost != null) {
                        vm.setLastHostId(lastHost.getId());
                    }
                    vm.setPowerState(powerState);
                    if (powerState == VirtualMachine.PowerState.PowerOn) {
                        vm.setState(State.Running);
                    }
                }

                vm.setUserVmType(vmType);
                _vmDao.persist(vm);
                for (String key : customParameters.keySet()) {
                    // BIOS was explicitly passed as the boot type, so honour it
                    if (key.equalsIgnoreCase(ApiConstants.BootType.BIOS.toString())) {
                        vm.details.remove(ApiConstants.BootType.UEFI.toString());
                        continue;
                    }

                    // Deploy as is, Don't care about the boot type or template settings
                    if (key.equalsIgnoreCase(ApiConstants.BootType.UEFI.toString()) && template.isDeployAsIs()) {
                        vm.details.remove(ApiConstants.BootType.UEFI.toString());
                        continue;
                    }

                    if (!hypervisorType.equals(HypervisorType.KVM)) {
                        if (key.equalsIgnoreCase(VmDetailConstants.IOTHREADS)) {
                            vm.details.remove(VmDetailConstants.IOTHREADS);
                            continue;
                        }
                        if (key.equalsIgnoreCase(VmDetailConstants.IO_POLICY)) {
                            vm.details.remove(VmDetailConstants.IO_POLICY);
                            continue;
                        }
                    }

                    if (key.equalsIgnoreCase(VmDetailConstants.CPU_NUMBER) ||
                            key.equalsIgnoreCase(VmDetailConstants.CPU_SPEED) ||
                            key.equalsIgnoreCase(VmDetailConstants.MEMORY)) {
                        // handle double byte strings.
                        vm.setDetail(key, Integer.toString(Integer.parseInt(customParameters.get(key))));
                    } else {
                        vm.setDetail(key, customParameters.get(key));
                    }
                }
                vm.setDetail(VmDetailConstants.DEPLOY_VM, "true");

                persistVMDeployAsIsProperties(vm, userVmOVFPropertiesMap);

                List<String> hiddenDetails = new ArrayList<>();
                if (customParameters.containsKey(VmDetailConstants.NAME_ON_HYPERVISOR)) {
                    hiddenDetails.add(VmDetailConstants.NAME_ON_HYPERVISOR);
                }
                _vmDao.saveDetails(vm, hiddenDetails);
                if (!isImport) {
                    s_logger.debug("Allocating in the DB for vm");
                    DataCenterDeployment plan = new DataCenterDeployment(zone.getId());

                    List<String> computeTags = new ArrayList<String>();
                    computeTags.add(offering.getHostTag());

                    List<String> rootDiskTags = new ArrayList<String>();
                    DiskOfferingVO rootDiskOfferingVO = _diskOfferingDao.findById(rootDiskOfferingId);
                    rootDiskTags.add(rootDiskOfferingVO.getTags());

                    if (isIso) {
                        _orchSrvc.createVirtualMachineFromScratch(vm.getUuid(), Long.toString(owner.getAccountId()), vm.getIsoId().toString(), hostName, displayName,
                                hypervisorType.name(), guestOSCategory.getName(), offering.getCpu(), offering.getSpeed(), offering.getRamSize(), diskSize, computeTags, rootDiskTags,
                                networkNicMap, plan, extraDhcpOptionMap, rootDiskOfferingId);
                    } else {
                        _orchSrvc.createVirtualMachine(vm.getUuid(), Long.toString(owner.getAccountId()), Long.toString(template.getId()), hostName, displayName, hypervisorType.name(),
                                offering.getCpu(), offering.getSpeed(), offering.getRamSize(), diskSize, computeTags, rootDiskTags, networkNicMap, plan, rootDiskSize, extraDhcpOptionMap,
                                dataDiskTemplateToDiskOfferingMap, diskOfferingId, rootDiskOfferingId);
                    }

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully allocated DB entry for " + vm);
                    }
                }
                CallContext.current().setEventDetails("Vm Id: " + vm.getUuid());

                if (!isImport) {
                    if (!offering.isDynamic()) {
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(), vm.getHostName(), offering.getId(), template.getId(),
                                hypervisorType.toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
                    } else {
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(), vm.getHostName(), offering.getId(), template.getId(),
                                hypervisorType.toString(), VirtualMachine.class.getName(), vm.getUuid(), customParameters, vm.isDisplayVm());
                    }

                    //Update Resource Count for the given account
                    resourceCountIncrement(accountId, isDisplayVm, new Long(offering.getCpu()), new Long(offering.getRamSize()));
                }
                return vm;
            }
        });
    }

    private void updateVMDiskController(UserVmVO vm, Map<String, String> customParameters, GuestOSVO guestOS) {
        // If hypervisor is vSphere and OS is OS X, set special settings.
        if (guestOS.getDisplayName().toLowerCase().contains("apple mac os")) {
            vm.setDetail(VmDetailConstants.SMC_PRESENT, "TRUE");
            vm.setDetail(VmDetailConstants.ROOT_DISK_CONTROLLER, "scsi");
            vm.setDetail(VmDetailConstants.DATA_DISK_CONTROLLER, "scsi");
            vm.setDetail(VmDetailConstants.FIRMWARE, "efi");
            s_logger.info("guestOS is OSX : overwrite root disk controller to scsi, use smc and efi");
        } else {
            String rootDiskControllerSetting = customParameters.get(VmDetailConstants.ROOT_DISK_CONTROLLER);
            String dataDiskControllerSetting = customParameters.get(VmDetailConstants.DATA_DISK_CONTROLLER);
            if (StringUtils.isNotEmpty(rootDiskControllerSetting)) {
                vm.setDetail(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDiskControllerSetting);
            }

            if (StringUtils.isNotEmpty(dataDiskControllerSetting)) {
                vm.setDetail(VmDetailConstants.DATA_DISK_CONTROLLER, dataDiskControllerSetting);
            }

            String controllerSetting = StringUtils.defaultIfEmpty(_configDao.getValue(Config.VmwareRootDiskControllerType.key()),
                    Config.VmwareRootDiskControllerType.getDefaultValue());

            // Don't override if VM already has root/data disk controller detail
            if (vm.getDetail(VmDetailConstants.ROOT_DISK_CONTROLLER) == null) {
                vm.setDetail(VmDetailConstants.ROOT_DISK_CONTROLLER, controllerSetting);
            }
            if (vm.getDetail(VmDetailConstants.DATA_DISK_CONTROLLER) == null) {
                if (controllerSetting.equalsIgnoreCase("scsi")) {
                    vm.setDetail(VmDetailConstants.DATA_DISK_CONTROLLER, "scsi");
                } else {
                    vm.setDetail(VmDetailConstants.DATA_DISK_CONTROLLER, "osdefault");
                }
            }
        }
    }

    /**
     * take the properties and set them on the vm.
     * consider should we be complete, and make sure all default values are copied as well if known?
     * I.E. iterate over the template details as well to copy any that are not defined yet.
     */
    private void persistVMDeployAsIsProperties(UserVmVO vm, Map<String, String> userVmOVFPropertiesMap) {
        if (MapUtils.isNotEmpty(userVmOVFPropertiesMap)) {
            for (String key : userVmOVFPropertiesMap.keySet()) {
                String detailKey = key;
                String value = userVmOVFPropertiesMap.get(key);

                // Sanitize boolean values to expected format and encrypt passwords
                if (StringUtils.isNotBlank(value)) {
                    if (value.equalsIgnoreCase("True")) {
                        value = "True";
                    } else if (value.equalsIgnoreCase("False")) {
                        value = "False";
                    } else {
                        OVFPropertyTO propertyTO = templateDeployAsIsDetailsDao.findPropertyByTemplateAndKey(vm.getTemplateId(), key);
                        if (propertyTO != null && propertyTO.isPassword()) {
                            value = DBEncryptionUtil.encrypt(value);
                        }
                    }
                } else if (value == null) {
                    value = "";
                }
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(String.format("setting property '%s' as '%s' with value '%s'", key, detailKey, value));
                }
                UserVmDeployAsIsDetailVO detail = new UserVmDeployAsIsDetailVO(vm.getId(), detailKey, value);
                userVmDeployAsIsDetailsDao.persist(detail);
            }
        }
    }

    private UserVmVO commitUserVm(final DataCenter zone, final VirtualMachineTemplate template, final String hostName, final String displayName, final Account owner,
                                  final Long diskOfferingId, final Long diskSize, final String userData, Long userDataId, String userDataDetails, final Account caller, final Boolean isDisplayVm, final String keyboard,
                                  final long accountId, final long userId, final ServiceOfferingVO offering, final boolean isIso, final String sshPublicKeys, final LinkedHashMap<String, List<NicProfile>> networkNicMap,
                                  final long id, final String instanceName, final String uuidName, final HypervisorType hypervisorType, final Map<String, String> customParameters, final Map<String,
            Map<Integer, String>> extraDhcpOptionMap, final Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
                                  Map<String, String> userVmOVFPropertiesMap, final boolean dynamicScalingEnabled, String vmType, final Long rootDiskOfferingId, String sshkeypairs) throws InsufficientCapacityException {
        return commitUserVm(false, zone, null, null, template, hostName, displayName, owner,
                diskOfferingId, diskSize, userData, userDataId, userDataDetails, isDisplayVm, keyboard,
                accountId, userId, offering, isIso, sshPublicKeys, networkNicMap,
                id, instanceName, uuidName, hypervisorType, customParameters,
                extraDhcpOptionMap, dataDiskTemplateToDiskOfferingMap,
                userVmOVFPropertiesMap, null, dynamicScalingEnabled, vmType, rootDiskOfferingId, sshkeypairs);
    }

    public void validateRootDiskResize(final HypervisorType hypervisorType, Long rootDiskSize, VMTemplateVO templateVO, UserVmVO vm, final Map<String, String> customParameters) throws InvalidParameterValueException
    {
        // rootdisksize must be larger than template.
        boolean isIso = ImageFormat.ISO == templateVO.getFormat();
        if ((rootDiskSize << 30) < templateVO.getSize()) {
            String error = String.format("Unsupported: rootdisksize override (%s GB) is smaller than template size %s", rootDiskSize, toHumanReadableSize(templateVO.getSize()));
            s_logger.error(error);
            throw new InvalidParameterValueException(error);
        } else if ((rootDiskSize << 30) > templateVO.getSize()) {
            if (hypervisorType == HypervisorType.VMware && (vm.getDetails() == null || vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER) == null)) {
                s_logger.warn("If Root disk controller parameter is not overridden, then Root disk resize may fail because current Root disk controller value is NULL.");
            } else if (hypervisorType == HypervisorType.VMware && vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER).toLowerCase().contains("ide") && !isIso) {
                String error = String.format("Found unsupported root disk controller [%s].", vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER));
                s_logger.error(error);
                throw new InvalidParameterValueException(error);
            } else {
                s_logger.debug("Rootdisksize override validation successful. Template root disk size " + toHumanReadableSize(templateVO.getSize()) + " Root disk size specified " + rootDiskSize + " GB");
            }
        } else {
            s_logger.debug("Root disk size specified is " + toHumanReadableSize(rootDiskSize << 30) + " and Template root disk size is " + toHumanReadableSize(templateVO.getSize()) + ". Both are equal so no need to override");
            customParameters.remove(VmDetailConstants.ROOT_DISK_SIZE);
        }
    }


    @Override
    public void generateUsageEvent(VirtualMachine vm, boolean isDisplay, String eventType){
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
        if (!serviceOffering.isDynamic()) {
            UsageEventUtils.publishUsageEvent(eventType, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getHostName(), serviceOffering.getId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                    VirtualMachine.class.getName(), vm.getUuid(), isDisplay);
        }
        else {
            Map<String, String> customParameters = new HashMap<String, String>();
            customParameters.put(UsageEventVO.DynamicParameters.cpuNumber.name(), serviceOffering.getCpu().toString());
            customParameters.put(UsageEventVO.DynamicParameters.cpuSpeed.name(), serviceOffering.getSpeed().toString());
            customParameters.put(UsageEventVO.DynamicParameters.memory.name(), serviceOffering.getRamSize().toString());
            UsageEventUtils.publishUsageEvent(eventType, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getHostName(), serviceOffering.getId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                    VirtualMachine.class.getName(), vm.getUuid(), customParameters, isDisplay);
        }
    }

    @Override
    public void collectVmNetworkStatistics (final UserVm userVm) {
        if (!userVm.getHypervisorType().equals(HypervisorType.KVM)) {
            return;
        }
        s_logger.debug("Collect vm network statistics from host before stopping Vm");
        long hostId = userVm.getHostId();
        List<String> vmNames = new ArrayList<String>();
        vmNames.add(userVm.getInstanceName());
        final HostVO host = _hostDao.findById(hostId);

        GetVmNetworkStatsAnswer networkStatsAnswer = null;
        try {
            networkStatsAnswer = (GetVmNetworkStatsAnswer) _agentMgr.easySend(hostId, new GetVmNetworkStatsCommand(vmNames, host.getGuid(), host.getName()));
        } catch (Exception e) {
            s_logger.warn("Error while collecting network stats for vm: " + userVm.getHostName() + " from host: " + host.getName(), e);
            return;
        }
        if (networkStatsAnswer != null) {
            if (!networkStatsAnswer.getResult()) {
                s_logger.warn("Error while collecting network stats vm: " + userVm.getHostName() + " from host: " + host.getName() + "; details: " + networkStatsAnswer.getDetails());
                return;
            }
            try {
                final GetVmNetworkStatsAnswer networkStatsAnswerFinal = networkStatsAnswer;
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        HashMap<String, List<VmNetworkStatsEntry>> vmNetworkStatsByName = networkStatsAnswerFinal.getVmNetworkStatsMap();
                        if (vmNetworkStatsByName == null) {
                            return;
                        }
                        List<VmNetworkStatsEntry> vmNetworkStats = vmNetworkStatsByName.get(userVm.getInstanceName());
                        if (vmNetworkStats == null) {
                            return;
                        }

                        for (VmNetworkStatsEntry vmNetworkStat:vmNetworkStats) {
                            SearchCriteria<NicVO> sc_nic = _nicDao.createSearchCriteria();
                            sc_nic.addAnd("macAddress", SearchCriteria.Op.EQ, vmNetworkStat.getMacAddress());
                            NicVO nic = _nicDao.search(sc_nic, null).get(0);
                            List<VlanVO> vlan = _vlanDao.listVlansByNetworkId(nic.getNetworkId());
                            if (vlan == null || vlan.size() == 0 || vlan.get(0).getVlanType() != VlanType.DirectAttached)
                            {
                                break; // only get network statistics for DirectAttached network (shared networks in Basic zone and Advanced zone with/without SG)
                            }
                            UserStatisticsVO previousvmNetworkStats = _userStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), nic.getNetworkId(), nic.getIPv4Address(), userVm.getId(), "UserVm");
                            if (previousvmNetworkStats == null) {
                                previousvmNetworkStats = new UserStatisticsVO(userVm.getAccountId(), userVm.getDataCenterId(),nic.getIPv4Address(), userVm.getId(), "UserVm", nic.getNetworkId());
                                _userStatsDao.persist(previousvmNetworkStats);
                            }
                            UserStatisticsVO vmNetworkStat_lock = _userStatsDao.lock(userVm.getAccountId(), userVm.getDataCenterId(), nic.getNetworkId(), nic.getIPv4Address(), userVm.getId(), "UserVm");

                            if ((vmNetworkStat.getBytesSent() == 0) && (vmNetworkStat.getBytesReceived() == 0)) {
                                s_logger.debug("bytes sent and received are all 0. Not updating user_statistics");
                                continue;
                            }

                            if (vmNetworkStat_lock == null) {
                                s_logger.warn("unable to find vm network stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId()+ " and nicId:" + nic.getId());
                                continue;
                            }

                            if (previousvmNetworkStats != null
                                    && ((previousvmNetworkStats.getCurrentBytesSent() != vmNetworkStat_lock.getCurrentBytesSent())
                                            || (previousvmNetworkStats.getCurrentBytesReceived() != vmNetworkStat_lock.getCurrentBytesReceived()))) {
                                s_logger.debug("vm network stats changed from the time GetNmNetworkStatsCommand was sent. " +
                                        "Ignoring current answer. Host: " + host.getName()  + " . VM: " + vmNetworkStat.getVmName() +
                                        " Sent(Bytes): " + toHumanReadableSize(vmNetworkStat.getBytesSent()) + " Received(Bytes): " + toHumanReadableSize(vmNetworkStat.getBytesReceived()));
                                continue;
                            }

                            if (vmNetworkStat_lock.getCurrentBytesSent() > vmNetworkStat.getBytesSent()) {
                                if (s_logger.isDebugEnabled()) {
                                   s_logger.debug("Sent # of bytes that's less than the last one.  " +
                                            "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmNetworkStat.getVmName() +
                                            " Reported: " + toHumanReadableSize(vmNetworkStat.getBytesSent()) + " Stored: " + toHumanReadableSize(vmNetworkStat_lock.getCurrentBytesSent()));
                                }
                                vmNetworkStat_lock.setNetBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                            }
                            vmNetworkStat_lock.setCurrentBytesSent(vmNetworkStat.getBytesSent());

                            if (vmNetworkStat_lock.getCurrentBytesReceived() > vmNetworkStat.getBytesReceived()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  " +
                                            "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmNetworkStat.getVmName() +
                                            " Reported: " + toHumanReadableSize(vmNetworkStat.getBytesReceived()) + " Stored: " + toHumanReadableSize(vmNetworkStat_lock.getCurrentBytesReceived()));
                                }
                                vmNetworkStat_lock.setNetBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                            }
                            vmNetworkStat_lock.setCurrentBytesReceived(vmNetworkStat.getBytesReceived());

                            if (! _dailyOrHourly) {
                                //update agg bytes
                                vmNetworkStat_lock.setAggBytesReceived(vmNetworkStat_lock.getNetBytesReceived() + vmNetworkStat_lock.getCurrentBytesReceived());
                                vmNetworkStat_lock.setAggBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                            }

                            _userStatsDao.update(vmNetworkStat_lock.getId(), vmNetworkStat_lock);
                        }
                    }
                });
            } catch (Exception e) {
                s_logger.warn("Unable to update vm network statistics for vm: " + userVm.getId() + " from host: " + hostId, e);
            }
        }
    }

    protected String validateUserData(String userData, HTTPMethod httpmethod) {
        byte[] decodedUserData = null;
        if (userData != null) {

            if (userData.contains("%")) {
                try {
                    userData = URLDecoder.decode(userData, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new InvalidParameterValueException("Url decoding of userdata failed.");
                }
            }

            if (!Base64.isBase64(userData)) {
                throw new InvalidParameterValueException("User data is not base64 encoded");
            }
            // If GET, use 4K. If POST, support up to 1M.
            if (httpmethod.equals(HTTPMethod.GET)) {
                if (userData.length() >= MAX_HTTP_GET_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for an http GET request");
                }
                if (userData.length() > VM_USERDATA_MAX_LENGTH.value()) {
                    throw new InvalidParameterValueException("User data has exceeded configurable max length : " + VM_USERDATA_MAX_LENGTH.value());
                }
                decodedUserData = Base64.decodeBase64(userData.getBytes());
                if (decodedUserData.length > MAX_HTTP_GET_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for GET request");
                }
            } else if (httpmethod.equals(HTTPMethod.POST)) {
                if (userData.length() >= MAX_HTTP_POST_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for an http POST request");
                }
                if (userData.length() > VM_USERDATA_MAX_LENGTH.value()) {
                    throw new InvalidParameterValueException("User data has exceeded configurable max length : " + VM_USERDATA_MAX_LENGTH.value());
                }
                decodedUserData = Base64.decodeBase64(userData.getBytes());
                if (decodedUserData.length > MAX_HTTP_POST_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for POST request");
                }
            }

            if (decodedUserData == null || decodedUserData.length < 1) {
                throw new InvalidParameterValueException("User data is too short");
            }
            // Re-encode so that the '=' paddings are added if necessary since 'isBase64' does not require it, but python does on the VR.
            return Base64.encodeBase64String(decodedUserData);
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(DeployVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        long vmId = cmd.getEntityId();
        Long podId = null;
        Long clusterId = null;
        Long hostId = cmd.getHostId();
        Map<VirtualMachineProfile.Param, Object> additionalParams =  new HashMap<>();
        Map<Long, DiskOffering> diskOfferingMap = cmd.getDataDiskTemplateToDiskOfferingMap();
        Map<String, String> details = cmd.getDetails();
        if (cmd instanceof DeployVMCmdByAdmin) {
            DeployVMCmdByAdmin adminCmd = (DeployVMCmdByAdmin)cmd;
            podId = adminCmd.getPodId();
            clusterId = adminCmd.getClusterId();
        }
        UserVmDetailVO uefiDetail = userVmDetailsDao.findDetail(cmd.getEntityId(), ApiConstants.BootType.UEFI.toString());
        if (uefiDetail != null) {
            addVmUefiBootOptionsToParams(additionalParams, uefiDetail.getName(), uefiDetail.getValue());
        }
        if (cmd.getBootIntoSetup() != null) {
            additionalParams.put(VirtualMachineProfile.Param.BootIntoSetup, cmd.getBootIntoSetup());
        }
        return startVirtualMachine(vmId, podId, clusterId, hostId, diskOfferingMap, additionalParams, cmd.getDeploymentPlanner());
    }

    private UserVm startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId, Map<Long, DiskOffering> diskOfferingMap
            , Map<VirtualMachineProfile.Param, Object> additonalParams, String deploymentPlannerToUse)
            throws ResourceUnavailableException,
            InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        UserVmVO vm = _vmDao.findById(vmId);
        Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> vmParamPair = null;

        try {
            vmParamPair = startVirtualMachine(vmId, podId, clusterId, hostId, additonalParams, deploymentPlannerToUse);
            vm = vmParamPair.first();

            // At this point VM should be in "Running" state
            UserVmVO tmpVm = _vmDao.findById(vm.getId());
            if (!tmpVm.getState().equals(State.Running)) {
                // Some other thread changed state of VM, possibly vmsync
                s_logger.error("VM " + tmpVm + " unexpectedly went to " + tmpVm.getState() + " state");
                throw new ConcurrentOperationException("Failed to deploy VM "+vm);
            }

            try {
                if (!diskOfferingMap.isEmpty()) {
                    List<VolumeVO> vols = _volsDao.findByInstance(tmpVm.getId());
                    for (VolumeVO vol : vols) {
                        if (vol.getVolumeType() == Volume.Type.DATADISK) {
                            DiskOffering doff =  _entityMgr.findById(DiskOffering.class, vol.getDiskOfferingId());
                            _volService.resizeVolumeOnHypervisor(vol.getId(), doff.getDiskSize(), tmpVm.getHostId(), vm.getInstanceName());
                        }
                    }
                }
            }
            catch (Exception e) {
                s_logger.fatal("Unable to resize the data disk for vm " + vm.getDisplayName() + " due to " + e.getMessage(), e);
            }

        } finally {
            updateVmStateForFailedVmCreation(vm.getId(), hostId);
        }

        // Check that the password was passed in and is valid
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());
        if (template.isEnablePassword()) {
            // this value is not being sent to the backend; need only for api
            // display purposes
            vm.setPassword((String)vmParamPair.second().get(VirtualMachineProfile.Param.VmPassword));
        }

        return vm;
    }

    private void addUserVMCmdlineArgs(Long vmId, VirtualMachineProfile profile, DeployDestination dest, StringBuilder buf) {
        UserVmVO k8sVM = _vmDao.findById(vmId);
        buf.append(" template=domP");
        buf.append(" name=").append(profile.getHostName());
        buf.append(" type=").append(k8sVM.getUserVmType());
        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if (nic.getIPv4Address() == null) {
                buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
                buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
            } else {
                buf.append(" eth").append(deviceId).append("ip=").append(nic.getIPv4Address());
                buf.append(" eth").append(deviceId).append("mask=").append(nic.getIPv4Netmask());
            }

            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getIPv4Gateway());
            }

            if (nic.getTrafficType() == TrafficType.Management) {
                String mgmt_cidr = _configDao.getValue(Config.ManagementNetwork.key());
                if (NetUtils.isValidIp4Cidr(mgmt_cidr)) {
                    buf.append(" mgmtcidr=").append(mgmt_cidr);
                }
                buf.append(" localgw=").append(dest.getPod().getGateway());
            }
        }
        DataCenterVO dc = _dcDao.findById(profile.getVirtualMachine().getDataCenterId());
        buf.append(" internaldns1=").append(dc.getInternalDns1());
        if (dc.getInternalDns2() != null) {
            buf.append(" internaldns2=").append(dc.getInternalDns2());
        }
        buf.append(" dns1=").append(dc.getDns1());
        if (dc.getDns2() != null) {
            buf.append(" dns2=").append(dc.getDns2());
        }
        s_logger.info("cmdline details: "+ buf.toString());
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {
        UserVmVO vm = _vmDao.findById(profile.getId());
        Map<String, String> details = userVmDetailsDao.listDetailsKeyPairs(vm.getId());
        vm.setDetails(details);
        StringBuilder buf = profile.getBootArgsBuilder();
        if (CKS_NODE.equals(vm.getUserVmType())) {
            addUserVMCmdlineArgs(vm.getId(), profile, dest, buf);
        }
        // add userdata info into vm profile
        Nic defaultNic = _networkModel.getDefaultNic(vm.getId());
        if(defaultNic != null) {
            Network network = _networkModel.getNetwork(defaultNic.getNetworkId());
            if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                final String serviceOffering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
                boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
                String destHostname = VirtualMachineManager.getHypervisorHostname(dest.getHost() != null ? dest.getHost().getName() : "");
                List<String[]> vmData = _networkModel.generateVmData(vm.getUserData(), vm.getUserDataDetails(), serviceOffering, vm.getDataCenterId(), vm.getInstanceName(), vm.getHostName(), vm.getId(),
                        vm.getUuid(), defaultNic.getIPv4Address(), vm.getDetail(VmDetailConstants.SSH_PUBLIC_KEY), (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword), isWindows, destHostname);
                String vmName = vm.getInstanceName();
                String configDriveIsoRootFolder = "/tmp";
                String isoFile = configDriveIsoRootFolder + "/" + vmName + "/configDrive/" + vmName + ".iso";
                profile.setVmData(vmData);
                profile.setConfigDriveLabel(VirtualMachineManager.VmConfigDriveLabel.value());
                profile.setConfigDriveIsoRootFolder(configDriveIsoRootFolder);
                profile.setConfigDriveIsoFile(isoFile);
            }
        }

        _templateMgr.prepareIsoForVmProfile(profile, dest);
        return true;
    }

    @Override
    public boolean setupVmForPvlan(boolean add, Long hostId, NicProfile nic) {
        if (!nic.getBroadCastUri().getScheme().equals("pvlan")) {
            return false;
        }
        String op = "add";
        if (!add) {
            // "delete" would remove all the rules(if using ovs) related to this vm
            op = "delete";
        }
        Network network = _networkDao.findById(nic.getNetworkId());
        Host host = _hostDao.findById(hostId);
        String networkTag = _networkModel.getNetworkTag(host.getHypervisorType(), network);
        PvlanSetupCommand cmd = PvlanSetupCommand.createVmSetup(op, nic.getBroadCastUri(), networkTag, nic.getMacAddress());
        Answer answer = null;
        try {
            answer = _agentMgr.send(hostId, cmd);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            return false;
        } catch (AgentUnavailableException e) {
            s_logger.warn("Agent Unavailable ", e);
            return false;
        }

        boolean result = true;
        if (answer == null || !answer.getResult()) {
            result = false;
        }
        return result;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {
        UserVmVO userVm = _vmDao.findById(profile.getId());
        List<NicVO> nics = _nicDao.listByVmId(userVm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest || network.getTrafficType() == TrafficType.Public) {
                userVm.setPrivateIpAddress(nic.getIPv4Address());
                userVm.setPrivateMacAddress(nic.getMacAddress());
                _vmDao.update(userVm.getId(), userVm);
            }
        }

        List<VolumeVO> volumes = _volsDao.findByInstance(userVm.getId());
        VmDiskStatisticsVO diskstats = null;
        for (VolumeVO volume : volumes) {
            diskstats = _vmDiskStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), volume.getId());
            if (diskstats == null) {
                diskstats = new VmDiskStatisticsVO(userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), volume.getId());
                _vmDiskStatsDao.persist(diskstats);
            }
        }

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile) {
        UserVmVO vm = _vmDao.findById(profile.getId());
        List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vm.getId());
        RestoreVMSnapshotCommand command = _vmSnapshotMgr.createRestoreCommand(vm, vmSnapshots);
        if (command != null) {
            cmds.addCommand("restoreVMSnapshot", command);
        }
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        UserVmVO vm = _vmDao.findById(profile.getId());

        Answer[] answersToCmds = cmds.getAnswers();
        if (answersToCmds == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Returning from finalizeStart() since there are no answers to read");
            }
            return true;
        }
        Answer startAnswer = cmds.getAnswer(StartAnswer.class);
        String returnedIp = null;
        String originalIp = null;
        if (startAnswer != null) {
            StartAnswer startAns = (StartAnswer)startAnswer;
            VirtualMachineTO vmTO = startAns.getVirtualMachine();
            for (NicTO nicTO : vmTO.getNics()) {
                if (nicTO.getType() == TrafficType.Guest) {
                    returnedIp = nicTO.getIp();
                }
            }
        }

        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        NicVO guestNic = null;
        NetworkVO guestNetwork = null;
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            long isDefault = (nic.isDefaultNic()) ? 1 : 0;
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), Long.toString(nic.getId()),
                    network.getNetworkOfferingId(), null, isDefault, VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplay());
            if (network.getTrafficType() == TrafficType.Guest) {
                originalIp = nic.getIPv4Address();
                guestNic = nic;
                guestNetwork = network;
                // In vmware, we will be effecting pvlan settings in portgroups in StartCommand.
                if (profile.getHypervisorType() != HypervisorType.VMware) {
                    if (nic.getBroadcastUri().getScheme().equals("pvlan")) {
                        NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 0, false, "pvlan-nic");
                        if (!setupVmForPvlan(true, hostId, nicProfile)) {
                            return false;
                        }
                    }
                }
            }
        }
        boolean ipChanged = false;
        if (originalIp != null && !originalIp.equalsIgnoreCase(returnedIp)) {
            if (returnedIp != null && guestNic != null) {
                guestNic.setIPv4Address(returnedIp);
                ipChanged = true;
            }
        }
        if (returnedIp != null && !returnedIp.equalsIgnoreCase(originalIp)) {
            if (guestNic != null) {
                guestNic.setIPv4Address(returnedIp);
                ipChanged = true;
            }
        }
        if (ipChanged) {
            _dcDao.findById(vm.getDataCenterId());
            UserVmVO userVm = _vmDao.findById(profile.getId());
            // dc.getDhcpProvider().equalsIgnoreCase(Provider.ExternalDhcpServer.getName())
            if (_ntwkSrvcDao.canProviderSupportServiceInNetwork(guestNetwork.getId(), Service.Dhcp, Provider.ExternalDhcpServer)) {
                _nicDao.update(guestNic.getId(), guestNic);
                userVm.setPrivateIpAddress(guestNic.getIPv4Address());
                _vmDao.update(userVm.getId(), userVm);

                s_logger.info("Detected that ip changed in the answer, updated nic in the db with new ip " + returnedIp);
            }
        }

        // get system ip and create static nat rule for the vm
        try {
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
        } catch (Exception ex) {
            s_logger.warn("Failed to get system ip and enable static nat for the vm " + profile.getVirtualMachine() + " due to exception ", ex);
            return false;
        }

        Answer answer = cmds.getAnswer("restoreVMSnapshot");
        if (answer != null && answer instanceof RestoreVMSnapshotAnswer) {
            RestoreVMSnapshotAnswer restoreVMSnapshotAnswer = (RestoreVMSnapshotAnswer) answer;
            if (restoreVMSnapshotAnswer == null || !restoreVMSnapshotAnswer.getResult()) {
                s_logger.warn("Unable to restore the vm snapshot from image file to the VM: " + restoreVMSnapshotAnswer.getDetails());
            }
        }

        final VirtualMachineProfile vmProfile = profile;
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                final UserVmVO vm = _vmDao.findById(vmProfile.getId());
                final List<NicVO> nics = _nicDao.listByVmId(vm.getId());
                for (NicVO nic : nics) {
                    Network network = _networkModel.getNetwork(nic.getNetworkId());
                    if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                        vmIdCountMap.put(nic.getId(), new VmAndCountDetails(nic.getInstanceId(), VmIpFetchTrialMax.value()));
                    }
                }
            }
        });

        return true;
    }

    @Override
    public void finalizeExpunge(VirtualMachine vm) {
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_STOP, eventDescription = "stopping Vm", async = true)
    public UserVm stopVirtualMachine(long vmId, boolean forced) throws ConcurrentOperationException {
        // Input validation
        Account caller = CallContext.current().getCallingAccount();
        Long userId = CallContext.current().getCallingUserId();

        // if account is removed, return error
        if (caller != null && caller.getRemoved() != null) {
            throw new PermissionDeniedException("The account " + caller.getUuid() + " is removed");
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        // check if vm belongs to AutoScale vm group in Disabled state
        autoScaleManager.checkIfVmActionAllowed(vmId);

        boolean status = false;
        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());

            if(forced) {
                status = vmEntity.stopForced(Long.toString(userId));
            } else {
                status = vmEntity.stop(Long.toString(userId));
            }
            if (status) {
                return _vmDao.findById(vmId);
            } else {
                return null;
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        } catch (CloudException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        }
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
        VirtualMachine vm = profile.getVirtualMachine();
        // release elastic IP here
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            CallContext ctx = CallContext.current();
            try {
                long networkId = ip.getAssociatedWithNetworkId();
                Network guestNetwork = _networkDao.findById(networkId);
                NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, guestNetwork.getNetworkOfferingId());
                assert (offering.isAssociatePublicIP() == true) : "User VM should not have system owned public IP associated with it when offering configured not to associate public IP.";
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCallingAccount(), ctx.getCallingUserId(), true);
            } catch (Exception ex) {
                s_logger.warn("Failed to disable static nat and release system ip " + ip + " as a part of vm " + profile.getVirtualMachine() + " stop due to exception ", ex);
            }
        }

        final List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (final NicVO nic : nics) {
            final NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if (network != null && network.getTrafficType() == TrafficType.Guest) {
                if (nic.getBroadcastUri() != null && nic.getBroadcastUri().getScheme().equals("pvlan")) {
                    NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 0, false, "pvlan-nic");
                    setupVmForPvlan(false, vm.getHostId(), nicProfile);
                }
            }
        }
    }

    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        return startVirtualMachine(vmId, null, null, hostId, additionalParams, deploymentPlannerToUse);
    }

    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId,
            Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        return startVirtualMachine(vmId, podId, clusterId, hostId, additionalParams, deploymentPlannerToUse, true);
    }

    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId,
            Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse, boolean isExplicitHost)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        // Input validation
        final Account callerAccount = CallContext.current().getCallingAccount();
        UserVO callerUser = _userDao.findById(CallContext.current().getCallingUserId());

        // if account is removed, return error
        if (callerAccount != null && callerAccount.getRemoved() != null) {
            throw new InvalidParameterValueException("The account " + callerAccount.getId() + " is removed");
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        if (vm.getState() == State.Running) {
            throw new InvalidParameterValueException(String.format("The virtual machine %s (%s) is already running",
                    vm.getUuid(), vm.getDisplayNameOrHostName()));
        }

        _accountMgr.checkAccess(callerAccount, null, true, vm);

        Account owner = _accountDao.findById(vm.getAccountId());

        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.DISABLED) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
        }
        if (VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            // check if account/domain is with in resource limits to start a new vm
            ServiceOfferingVO offering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
            resourceLimitCheck(owner, vm.isDisplayVm(), Long.valueOf(offering.getCpu()), Long.valueOf(offering.getRamSize()));
        }
        // check if vm is security group enabled
        if (_securityGroupMgr.isVmSecurityGroupEnabled(vmId) && _securityGroupMgr.getSecurityGroupsForVm(vmId).isEmpty()
                && !_securityGroupMgr.isVmMappedToDefaultSecurityGroup(vmId) && _networkModel.canAddDefaultSecurityGroup()) {
            // if vm is not mapped to security group, create a mapping
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Vm " + vm + " is security group enabled, but not mapped to default security group; creating the mapping automatically");
            }

            SecurityGroup defaultSecurityGroup = _securityGroupMgr.getDefaultSecurityGroup(vm.getAccountId());
            if (defaultSecurityGroup != null) {
                List<Long> groupList = new ArrayList<Long>();
                groupList.add(defaultSecurityGroup.getId());
                _securityGroupMgr.addInstanceToGroups(vmId, groupList);
            }
        }
        // Choose deployment planner
        // Host takes 1st preference, Cluster takes 2nd preference and Pod takes 3rd
        // Default behaviour is invoked when host, cluster or pod are not specified
        boolean isRootAdmin = _accountService.isRootAdmin(callerAccount.getId());
        Pod destinationPod = getDestinationPod(podId, isRootAdmin);
        Cluster destinationCluster = getDestinationCluster(clusterId, isRootAdmin);
        Host destinationHost = getDestinationHost(hostId, isRootAdmin, isExplicitHost);
        DataCenterDeployment plan = null;
        boolean deployOnGivenHost = false;
        if (destinationHost != null) {
            s_logger.debug("Destination Host to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            final ServiceOfferingVO offering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
            Pair<Boolean, Boolean> cpuCapabilityAndCapacity = _capacityMgr.checkIfHostHasCpuCapabilityAndCapacity(destinationHost, offering, false);
            if (!cpuCapabilityAndCapacity.first() || !cpuCapabilityAndCapacity.second()) {
                String errorMsg = "Cannot deploy the VM to specified host " + hostId + "; host has cpu capability? " + cpuCapabilityAndCapacity.first() + ", host has capacity? " + cpuCapabilityAndCapacity.second();
                s_logger.info(errorMsg);
                if (!AllowDeployVmIfGivenHostFails.value()) {
                    throw new InvalidParameterValueException(errorMsg);
                };
            } else {
                plan = new DataCenterDeployment(vm.getDataCenterId(), destinationHost.getPodId(), destinationHost.getClusterId(), destinationHost.getId(), null, null);
                if (!AllowDeployVmIfGivenHostFails.value()) {
                    deployOnGivenHost = true;
                }
            }
        } else if (destinationCluster != null) {
            s_logger.debug("Destination Cluster to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            plan = new DataCenterDeployment(vm.getDataCenterId(), destinationCluster.getPodId(), destinationCluster.getId(), null, null, null);
            if (!AllowDeployVmIfGivenHostFails.value()) {
                deployOnGivenHost = true;
            }
        } else if (destinationPod != null) {
            s_logger.debug("Destination Pod to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            plan = new DataCenterDeployment(vm.getDataCenterId(), destinationPod.getId(), null, null, null, null);
            if (!AllowDeployVmIfGivenHostFails.value()) {
                deployOnGivenHost = true;
            }
        }

        // Set parameters
        Map<VirtualMachineProfile.Param, Object> params = null;
        VMTemplateVO template = null;
        if (vm.isUpdateParameters()) {
            _vmDao.loadDetails(vm);
            // Check that the password was passed in and is valid
            template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());

            String password = "saved_password";
            if (template.isEnablePassword()) {
                if (vm.getDetail("password") != null) {
                    password = DBEncryptionUtil.decrypt(vm.getDetail("password"));
                } else {
                    password = _mgr.generateRandomPassword();
                    vm.setPassword(password);
                }
            }

            if (!validPassword(password)) {
                throw new InvalidParameterValueException("A valid password for this virtual machine was not provided.");
            }

            // Check if an SSH key pair was selected for the instance and if so
            // use it to encrypt & save the vm password
            encryptAndStorePassword(vm, password);

            params = createParameterInParameterMap(params, additionalParams, VirtualMachineProfile.Param.VmPassword, password);
        }

        if(null != additionalParams && additionalParams.containsKey(VirtualMachineProfile.Param.BootIntoSetup)) {
            if (! HypervisorType.VMware.equals(vm.getHypervisorType())) {
                throw new InvalidParameterValueException(ApiConstants.BOOT_INTO_SETUP + " makes no sense for " + vm.getHypervisorType());
            }
            Object paramValue = additionalParams.get(VirtualMachineProfile.Param.BootIntoSetup);
            if (s_logger.isTraceEnabled()) {
                    s_logger.trace("It was specified whether to enter setup mode: " + paramValue.toString());
            }
            params = createParameterInParameterMap(params, additionalParams, VirtualMachineProfile.Param.BootIntoSetup, paramValue);
        }

        VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());

        DeploymentPlanner planner = null;
        if (deploymentPlannerToUse != null) {
            // if set to null, the deployment planner would be later figured out either from global config var, or from
            // the service offering
            planner = _planningMgr.getDeploymentPlannerByName(deploymentPlannerToUse);
            if (planner == null) {
                throw new InvalidParameterValueException("Can't find a planner by name " + deploymentPlannerToUse);
            }
        }
        vmEntity.setParamsToEntity(additionalParams);

        String reservationId = vmEntity.reserve(planner, plan, new ExcludeList(), Long.toString(callerUser.getId()));
        vmEntity.deploy(reservationId, Long.toString(callerUser.getId()), params, deployOnGivenHost);

        Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> vmParamPair = new Pair(vm, params);
        if (vm != null && vm.isUpdateParameters()) {
            // this value is not being sent to the backend; need only for api
            // display purposes
            if (template.isEnablePassword()) {
                if (vm.getDetail(VmDetailConstants.PASSWORD) != null) {
                    userVmDetailsDao.removeDetail(vm.getId(), VmDetailConstants.PASSWORD);
                }
                vm.setUpdateParameters(false);
                _vmDao.update(vm.getId(), vm);
            }
        }

        return vmParamPair;
    }

    private Map<VirtualMachineProfile.Param, Object> createParameterInParameterMap(Map<VirtualMachineProfile.Param, Object> params, Map<VirtualMachineProfile.Param, Object> parameterMap, VirtualMachineProfile.Param parameter,
            Object parameterValue) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("createParameterInParameterMap(%s, %s)", parameter, parameterValue));
        }
        if (params == null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("creating new Parameter map");
            }
            params = new HashMap<>();
            if (parameterMap != null) {
                params.putAll(parameterMap);
            }
        }
        params.put(parameter, parameterValue);
        return params;
    }

    private Pod getDestinationPod(Long podId, boolean isRootAdmin) {
        Pod destinationPod = null;
        if (podId != null) {
            if (!isRootAdmin) {
                throw new PermissionDeniedException(
                        "Parameter " + ApiConstants.POD_ID + " can only be specified by a Root Admin, permission denied");
            }
            destinationPod = _podDao.findById(podId);
            if (destinationPod == null) {
                throw new InvalidParameterValueException("Unable to find the pod to deploy the VM, pod id=" + podId);
            }
        }
        return destinationPod;
    }

    private Cluster getDestinationCluster(Long clusterId, boolean isRootAdmin) {
        Cluster destinationCluster = null;
        if (clusterId != null) {
            if (!isRootAdmin) {
                throw new PermissionDeniedException(
                        "Parameter " + ApiConstants.CLUSTER_ID + " can only be specified by a Root Admin, permission denied");
            }
            destinationCluster = _clusterDao.findById(clusterId);
            if (destinationCluster == null) {
                throw new InvalidParameterValueException("Unable to find the cluster to deploy the VM, cluster id=" + clusterId);
            }
        }
        return destinationCluster;
    }

    private Host getDestinationHost(Long hostId, boolean isRootAdmin, boolean isExplicitHost) {
        Host destinationHost = null;
        if (hostId != null) {
            if (isExplicitHost && !isRootAdmin) {
                throw new PermissionDeniedException(
                        "Parameter " + ApiConstants.HOST_ID + " can only be specified by a Root Admin, permission denied");
            }
            destinationHost = _hostDao.findById(hostId);
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to deploy the VM, host id=" + hostId);
            } else if (destinationHost.getResourceState() != ResourceState.Enabled || destinationHost.getStatus() != Status.Up ) {
                throw new InvalidParameterValueException("Unable to deploy the VM as the host: " + destinationHost.getName() + " is not in the right state");
            }
        }
        return destinationHost;
    }

    @Override
    public UserVm destroyVm(long vmId, boolean expunge) throws ResourceUnavailableException, ConcurrentOperationException {
        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a virtual machine with specified vmId");
            throw ex;
        }

        if (vm.getState() == State.Destroyed || vm.getState() == State.Expunging) {
            s_logger.trace("Vm id=" + vmId + " is already destroyed");
            return vm;
        }

        vmStatsDao.removeAllByVmId(vmId);

        boolean status;
        State vmState = vm.getState();

        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            status = vmEntity.destroy(expunge);
        } catch (CloudException e) {
            CloudRuntimeException ex = new CloudRuntimeException("Unable to destroy with specified vmId", e);
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        if (status) {
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
            for (VolumeVO volume : volumes) {
                if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                            Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
                }
            }

            if (vmState != State.Error) {
                // Get serviceOffering for Virtual Machine
                ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

                //Update Resource Count for the given account
                resourceCountDecrement(vm.getAccountId(), vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));
            }
            return _vmDao.findById(vmId);
        } else {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to destroy vm with specified vmId");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

    }

    @Override
    public void collectVmDiskStatistics(final UserVm userVm) {
        // Only supported for KVM and VMware
        if (!(userVm.getHypervisorType().equals(HypervisorType.KVM) || userVm.getHypervisorType().equals(HypervisorType.VMware))) {
            return;
        }
        s_logger.debug("Collect vm disk statistics from host before stopping VM");
        if (userVm.getHostId() == null) {
            s_logger.error("Unable to collect vm disk statistics for VM as the host is null, skipping VM disk statistics collection");
            return;
        }
        long hostId = userVm.getHostId();
        List<String> vmNames = new ArrayList<String>();
        vmNames.add(userVm.getInstanceName());
        final HostVO host = _hostDao.findById(hostId);

        GetVmDiskStatsAnswer diskStatsAnswer = null;
        try {
            diskStatsAnswer = (GetVmDiskStatsAnswer)_agentMgr.easySend(hostId, new GetVmDiskStatsCommand(vmNames, host.getGuid(), host.getName()));
        } catch (Exception e) {
            s_logger.warn("Error while collecting disk stats for vm: " + userVm.getInstanceName() + " from host: " + host.getName(), e);
            return;
        }
        if (diskStatsAnswer != null) {
            if (!diskStatsAnswer.getResult()) {
                s_logger.warn("Error while collecting disk stats vm: " + userVm.getInstanceName() + " from host: " + host.getName() + "; details: " + diskStatsAnswer.getDetails());
                return;
            }
            try {
                final GetVmDiskStatsAnswer diskStatsAnswerFinal = diskStatsAnswer;
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        HashMap<String, List<VmDiskStatsEntry>> vmDiskStatsByName = diskStatsAnswerFinal.getVmDiskStatsMap();
                        if (vmDiskStatsByName == null) {
                            return;
                        }
                        List<VmDiskStatsEntry> vmDiskStats = vmDiskStatsByName.get(userVm.getInstanceName());
                        if (vmDiskStats == null) {
                            return;
                        }

                        for (VmDiskStatsEntry vmDiskStat : vmDiskStats) {
                            SearchCriteria<VolumeVO> sc_volume = _volsDao.createSearchCriteria();
                            sc_volume.addAnd("path", SearchCriteria.Op.EQ, vmDiskStat.getPath());
                            List<VolumeVO> volumes = _volsDao.search(sc_volume, null);
                            if ((volumes == null) || (volumes.size() == 0)) {
                                break;
                            }
                            VolumeVO volume = volumes.get(0);
                            VmDiskStatisticsVO previousVmDiskStats = _vmDiskStatsDao.findBy(userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), volume.getId());
                            VmDiskStatisticsVO vmDiskStat_lock = _vmDiskStatsDao.lock(userVm.getAccountId(), userVm.getDataCenterId(), userVm.getId(), volume.getId());

                            if ((vmDiskStat.getIORead() == 0) && (vmDiskStat.getIOWrite() == 0) && (vmDiskStat.getBytesRead() == 0) && (vmDiskStat.getBytesWrite() == 0)) {
                                s_logger.debug("Read/Write of IO and Bytes are both 0. Not updating vm_disk_statistics");
                                continue;
                            }

                            if (vmDiskStat_lock == null) {
                                s_logger.warn("unable to find vm disk stats from host for account: " + userVm.getAccountId() + " with vmId: " + userVm.getId() + " and volumeId:"
                                        + volume.getId());
                                continue;
                            }

                            if (previousVmDiskStats != null
                                    && ((previousVmDiskStats.getCurrentIORead() != vmDiskStat_lock.getCurrentIORead()) || ((previousVmDiskStats.getCurrentIOWrite() != vmDiskStat_lock
                                    .getCurrentIOWrite())
                                            || (previousVmDiskStats.getCurrentBytesRead() != vmDiskStat_lock.getCurrentBytesRead()) || (previousVmDiskStats
                                                    .getCurrentBytesWrite() != vmDiskStat_lock.getCurrentBytesWrite())))) {
                                s_logger.debug("vm disk stats changed from the time GetVmDiskStatsCommand was sent. " + "Ignoring current answer. Host: " + host.getName()
                                + " . VM: " + vmDiskStat.getVmName() + " IO Read: " + vmDiskStat.getIORead() + " IO Write: " + vmDiskStat.getIOWrite() + " Bytes Read: "
                                + vmDiskStat.getBytesRead() + " Bytes Write: " + vmDiskStat.getBytesWrite());
                                continue;
                            }

                            if (vmDiskStat_lock.getCurrentIORead() > vmDiskStat.getIORead()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Read # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " + host.getName()
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIORead() + " Stored: " + vmDiskStat_lock.getCurrentIORead());
                                }
                                vmDiskStat_lock.setNetIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                            }
                            vmDiskStat_lock.setCurrentIORead(vmDiskStat.getIORead());
                            if (vmDiskStat_lock.getCurrentIOWrite() > vmDiskStat.getIOWrite()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Write # of IO that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " + host.getName()
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getIOWrite() + " Stored: " + vmDiskStat_lock.getCurrentIOWrite());
                                }
                                vmDiskStat_lock.setNetIOWrite(vmDiskStat_lock.getNetIOWrite() + vmDiskStat_lock.getCurrentIOWrite());
                            }
                            vmDiskStat_lock.setCurrentIOWrite(vmDiskStat.getIOWrite());
                            if (vmDiskStat_lock.getCurrentBytesRead() > vmDiskStat.getBytesRead()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Read # of Bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " + host.getName()
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + toHumanReadableSize(vmDiskStat.getBytesRead()) + " Stored: " + toHumanReadableSize(vmDiskStat_lock.getCurrentBytesRead()));
                                }
                                vmDiskStat_lock.setNetBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                            }
                            vmDiskStat_lock.setCurrentBytesRead(vmDiskStat.getBytesRead());
                            if (vmDiskStat_lock.getCurrentBytesWrite() > vmDiskStat.getBytesWrite()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Write # of Bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " + host.getName()
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + toHumanReadableSize(vmDiskStat.getBytesWrite()) + " Stored: "
                                    + toHumanReadableSize(vmDiskStat_lock.getCurrentBytesWrite()));
                                }
                                vmDiskStat_lock.setNetBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                            }
                            vmDiskStat_lock.setCurrentBytesWrite(vmDiskStat.getBytesWrite());

                            if (!_dailyOrHourly) {
                                //update agg bytes
                                vmDiskStat_lock.setAggIORead(vmDiskStat_lock.getNetIORead() + vmDiskStat_lock.getCurrentIORead());
                                vmDiskStat_lock.setAggIOWrite(vmDiskStat_lock.getNetIOWrite() + vmDiskStat_lock.getCurrentIOWrite());
                                vmDiskStat_lock.setAggBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                                vmDiskStat_lock.setAggBytesWrite(vmDiskStat_lock.getNetBytesWrite() + vmDiskStat_lock.getCurrentBytesWrite());
                            }

                            _vmDiskStatsDao.update(vmDiskStat_lock.getId(), vmDiskStat_lock);
                        }
                    }
                });
            } catch (Exception e) {
                s_logger.warn(String.format("Unable to update VM disk statistics for %s from %s", userVm.getInstanceName(), host), e);
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_EXPUNGE, eventDescription = "expunging Vm", async = true)
    public UserVm expungeVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
        Account caller = CallContext.current().getCallingAccount();
        Long callerId = caller.getId();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a virtual machine with specified vmId");
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

        if (vm.getRemoved() != null) {
            s_logger.trace("Vm id=" + vmId + " is already expunged");
            return vm;
        }

        if (!(vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getState() == State.Error)) {
            CloudRuntimeException ex = new CloudRuntimeException("Please destroy vm with specified vmId before expunge");
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

        // When trying to expunge, permission is denied when the caller is not an admin and the AllowUserExpungeRecoverVm is false for the caller.
        if (!_accountMgr.isAdmin(callerId) && !AllowUserExpungeRecoverVm.valueIn(callerId)) {
            throw new PermissionDeniedException("Expunging a vm can only be done by an Admin. Or when the allow.user.expunge.recover.vm key is set.");
        }

        // check if vm belongs to AutoScale vm group in Disabled state
        autoScaleManager.checkIfVmActionAllowed(vmId);

        _vmSnapshotMgr.deleteVMSnapshotsFromDB(vmId, false);

        boolean status;

        status = expunge(vm);
        if (status) {
            return _vmDao.findByIdIncludingRemoved(vmId);
        } else {
            CloudRuntimeException ex = new CloudRuntimeException("Failed to expunge vm with specified vmId");
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

    }

    @Override
    public HypervisorType getHypervisorTypeOfUserVM(long vmId) {
        UserVmVO userVm = _vmDao.findById(vmId);
        if (userVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a virtual machine with specified id");
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

        return userVm.getHypervisorType();
    }

    protected String finalizeUserData(String userData, Long userDataId, VirtualMachineTemplate template) {
        if (StringUtils.isEmpty(userData) && userDataId == null && (template == null || template.getUserDataId() == null)) {
            return null;
        }

        if (userDataId != null && StringUtils.isNotEmpty(userData)) {
            throw new InvalidParameterValueException("Both userdata and userdata ID inputs are not allowed, please provide only one");
        }
        if (template != null && template.getUserDataId() != null) {
            switch (template.getUserDataOverridePolicy()) {
                case DENYOVERRIDE:
                    if (StringUtils.isNotEmpty(userData) || userDataId != null) {
                        String msg = String.format("UserData input is not allowed here since template %s is configured to deny any userdata", template.getName());
                        throw new CloudRuntimeException(msg);
                    }
                case ALLOWOVERRIDE:
                    if (userDataId != null) {
                        UserData apiUserDataVO = userDataDao.findById(userDataId);
                        return apiUserDataVO.getUserData();
                    } else if (StringUtils.isNotEmpty(userData)) {
                        return userData;
                    } else {
                        UserData templateUserDataVO = userDataDao.findById(template.getUserDataId());
                        if (templateUserDataVO == null) {
                            String msg = String.format("UserData linked to the template %s is not found", template.getName());
                            throw new CloudRuntimeException(msg);
                        }
                        return templateUserDataVO.getUserData();
                    }
                case APPEND:
                    UserData templateUserDataVO = userDataDao.findById(template.getUserDataId());
                    if (templateUserDataVO == null) {
                        String msg = String.format("UserData linked to the template %s is not found", template.getName());
                        throw new CloudRuntimeException(msg);
                    }
                    if (userDataId != null) {
                        UserData apiUserDataVO = userDataDao.findById(userDataId);
                        return doConcateUserDatas(templateUserDataVO.getUserData(), apiUserDataVO.getUserData());
                    } else if (StringUtils.isNotEmpty(userData)) {
                        return doConcateUserDatas(templateUserDataVO.getUserData(), userData);
                    } else {
                        return templateUserDataVO.getUserData();
                    }
                default:
                    String msg = String.format("This userdataPolicy %s is not supported for use with this feature", template.getUserDataOverridePolicy().toString());
                    throw new CloudRuntimeException(msg);            }
        } else {
            if (userDataId != null) {
                UserData apiUserDataVO = userDataDao.findById(userDataId);
                return apiUserDataVO.getUserData();
            } else if (StringUtils.isNotEmpty(userData)) {
                return userData;
            }
        }
        return null;
    }

    private String doConcateUserDatas(String userdata1, String userdata2) {
        byte[] userdata1Bytes = Base64.decodeBase64(userdata1.getBytes());
        byte[] userdata2Bytes = Base64.decodeBase64(userdata2.getBytes());
        byte[] finalUserDataBytes = new byte[userdata1Bytes.length + userdata2Bytes.length];
        System.arraycopy(userdata1Bytes, 0, finalUserDataBytes, 0, userdata1Bytes.length);
        System.arraycopy(userdata2Bytes, 0, finalUserDataBytes, userdata1Bytes.length, userdata2Bytes.length);

        return Base64.encodeBase64String(finalUserDataBytes);
    }

    @Override
    public UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException,
    StorageUnavailableException, ResourceAllocationException {
        //Verify that all objects exist before passing them to the service
        Account owner = _accountService.getActiveAccountById(cmd.getEntityOwnerId());

        verifyDetails(cmd.getDetails());

        Long zoneId = cmd.getZoneId();

        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
        }

        Long serviceOfferingId = cmd.getServiceOfferingId();
        Long overrideDiskOfferingId = cmd.getOverrideDiskOfferingId();

        ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
        }

        if (ServiceOffering.State.Inactive.equals(serviceOffering.getState())) {
            throw new InvalidParameterValueException(String.format("Service offering is inactive: [%s].", serviceOffering.getUuid()));
        }

        if (serviceOffering.getDiskOfferingStrictness() && overrideDiskOfferingId != null) {
            throw new InvalidParameterValueException(String.format("Cannot override disk offering id %d since provided service offering is strictly mapped to its disk offering", overrideDiskOfferingId));
        }

        if (!serviceOffering.isDynamic()) {
            for(String detail: cmd.getDetails().keySet()) {
                if(detail.equalsIgnoreCase(VmDetailConstants.CPU_NUMBER) || detail.equalsIgnoreCase(VmDetailConstants.CPU_SPEED) || detail.equalsIgnoreCase(VmDetailConstants.MEMORY)) {
                    throw new InvalidParameterValueException("cpuNumber or cpuSpeed or memory should not be specified for static service offering");
                }
            }
        }

        Long templateId = cmd.getTemplateId();

        boolean dynamicScalingEnabled = cmd.isDynamicScalingEnabled();

        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Unable to use template " + templateId);
        }

        ServiceOfferingJoinVO svcOffering = serviceOfferingJoinDao.findById(serviceOfferingId);

        if (template.isDeployAsIs()) {
            if (svcOffering != null && svcOffering.getRootDiskSize() != null && svcOffering.getRootDiskSize() > 0) {
                throw new InvalidParameterValueException("Failed to deploy Virtual Machine as a service offering with root disk size specified cannot be used with a deploy as-is template");
            }

            if (cmd.getDetails().get("rootdisksize") != null) {
                throw new InvalidParameterValueException("Overriding root disk size isn't supported for VMs deployed from deploy as-is templates");
            }

            // Bootmode and boottype are not supported on VMWare dpeloy-as-is templates (since 4.15)
            if ((cmd.getBootMode() != null || cmd.getBootType() != null)) {
                throw new InvalidParameterValueException("Boot type and boot mode are not supported on VMware for templates registered as deploy-as-is, as we honour what is defined in the template.");
            }
        }

        Long diskOfferingId = cmd.getDiskOfferingId();
        DiskOffering diskOffering = null;
        if (diskOfferingId != null) {
            diskOffering = _entityMgr.findById(DiskOffering.class, diskOfferingId);
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
            }
            if (diskOffering.isComputeOnly()) {
                throw new InvalidParameterValueException(String.format("The disk offering id %d provided is directly mapped to a service offering, please provide an individual disk offering", diskOfferingId));
            }
        }

        if (!zone.isLocalStorageEnabled()) {
            DiskOffering diskOfferingMappedInServiceOffering = _entityMgr.findById(DiskOffering.class, serviceOffering.getDiskOfferingId());
            if (diskOfferingMappedInServiceOffering.isUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOfferingMappedInServiceOffering.getName() + " mapped in service offering uses it");
            }
            if (diskOffering != null && diskOffering.isUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " uses it");
            }
        }

        List<Long> networkIds = cmd.getNetworkIds();
        LinkedHashMap<Integer, Long> userVmNetworkMap = getVmOvfNetworkMapping(zone, owner, template, cmd.getVmNetworkMap());
        if (MapUtils.isNotEmpty(userVmNetworkMap)) {
            networkIds = new ArrayList<>(userVmNetworkMap.values());
        }

        String userData = cmd.getUserData();
        Long userDataId = cmd.getUserdataId();
        String userDataDetails = null;
        if (MapUtils.isNotEmpty(cmd.getUserdataDetails())) {
            userDataDetails = cmd.getUserdataDetails().toString();
        }
        userData = finalizeUserData(userData, userDataId, template);

        Account caller = CallContext.current().getCallingAccount();
        Long callerId = caller.getId();

        boolean isRootAdmin = _accountService.isRootAdmin(callerId);

        Long hostId = cmd.getHostId();
        getDestinationHost(hostId, isRootAdmin, true);

        String ipAddress = cmd.getIpAddress();
        String ip6Address = cmd.getIp6Address();
        String macAddress = cmd.getMacAddress();
        String name = cmd.getName();
        String displayName = cmd.getDisplayName();
        UserVm vm = null;
        IpAddresses addrs = new IpAddresses(ipAddress, ip6Address, macAddress);
        Long size = cmd.getSize();
        String group = cmd.getGroup();
        List<String> sshKeyPairNames = cmd.getSSHKeyPairNames();
        Boolean displayVm = cmd.isDisplayVm();
        String keyboard = cmd.getKeyboard();
        Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap = cmd.getDataDiskTemplateToDiskOfferingMap();
        Map<String, String> userVmOVFProperties = cmd.getVmProperties();
        if (zone.getNetworkType() == NetworkType.Basic) {
            if (networkIds != null) {
                throw new InvalidParameterValueException("Can't specify network Ids in Basic zone");
            } else {
                vm = createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, getSecurityGroupIdList(cmd), owner, name, displayName, diskOfferingId,
                        size , group , cmd.getHypervisor(), cmd.getHttpMethod(), userData, userDataId, userDataDetails, sshKeyPairNames, cmd.getIpToNetworkMap(), addrs, displayVm , keyboard , cmd.getAffinityGroupIdList(),
                        cmd.getDetails(), cmd.getCustomId(), cmd.getDhcpOptionsMap(),
                        dataDiskTemplateToDiskOfferingMap, userVmOVFProperties, dynamicScalingEnabled, overrideDiskOfferingId);
            }
        } else {
            if (zone.isSecurityGroupEnabled())  {
                vm = createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, networkIds, getSecurityGroupIdList(cmd), owner, name,
                        displayName, diskOfferingId, size, group, cmd.getHypervisor(), cmd.getHttpMethod(), userData, userDataId, userDataDetails, sshKeyPairNames, cmd.getIpToNetworkMap(), addrs, displayVm, keyboard,
                        cmd.getAffinityGroupIdList(), cmd.getDetails(), cmd.getCustomId(), cmd.getDhcpOptionsMap(),
                        dataDiskTemplateToDiskOfferingMap, userVmOVFProperties, dynamicScalingEnabled, overrideDiskOfferingId, null);

            } else {
                if (cmd.getSecurityGroupIdList() != null && !cmd.getSecurityGroupIdList().isEmpty()) {
                    throw new InvalidParameterValueException("Can't create vm with security groups; security group feature is not enabled per zone");
                }
                vm = createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, name, displayName, diskOfferingId, size, group,
                        cmd.getHypervisor(), cmd.getHttpMethod(), userData, userDataId, userDataDetails, sshKeyPairNames, cmd.getIpToNetworkMap(), addrs, displayVm, keyboard, cmd.getAffinityGroupIdList(), cmd.getDetails(),
                        cmd.getCustomId(), cmd.getDhcpOptionsMap(), dataDiskTemplateToDiskOfferingMap, userVmOVFProperties, dynamicScalingEnabled, null, overrideDiskOfferingId);
            }
        }

        // check if this templateId has a child ISO
        List<VMTemplateVO> child_templates = _templateDao.listByParentTemplatetId(templateId);
        for (VMTemplateVO tmpl: child_templates){
            if (tmpl.getFormat() == Storage.ImageFormat.ISO){
                s_logger.info("MDOV trying to attach disk to the VM " + tmpl.getId() + " vmid=" + vm.getId());
                _tmplService.attachIso(tmpl.getId(), vm.getId(), true);
            }
        }

        // Add extraConfig to user_vm_details table
        String extraConfig = cmd.getExtraConfig();
        if (StringUtils.isNotBlank(extraConfig)) {
            if (EnableAdditionalVmConfig.valueIn(callerId)) {
                s_logger.info("Adding extra configuration to user vm: " + vm.getUuid());
                addExtraConfig(vm, extraConfig);
            } else {
                throw new InvalidParameterValueException("attempted setting extraconfig but enable.additional.vm.configuration is disabled");
            }
        }

        if (cmd.getCopyImageTags()) {
            VMTemplateVO templateOrIso = _templateDao.findById(templateId);
            if (templateOrIso != null) {
                final ResourceTag.ResourceObjectType templateType = (templateOrIso.getFormat() == ImageFormat.ISO) ? ResourceTag.ResourceObjectType.ISO : ResourceTag.ResourceObjectType.Template;
                final List<? extends ResourceTag> resourceTags = resourceTagDao.listBy(templateId, templateType);
                for (ResourceTag resourceTag : resourceTags) {
                    final ResourceTagVO copyTag = new ResourceTagVO(resourceTag.getKey(), resourceTag.getValue(), resourceTag.getAccountId(), resourceTag.getDomainId(), vm.getId(), ResourceTag.ResourceObjectType.UserVm, resourceTag.getCustomer(), vm.getUuid());
                    resourceTagDao.persist(copyTag);
                }
            }
        }
        return vm;
    }

    /**
     * Persist extra configuration data in the user_vm_details table as key/value pair
     * @param decodedUrl String consisting of the extra config data to appended onto the vmx file for VMware instances
     */
    protected void persistExtraConfigVmware(String decodedUrl, UserVm vm) {
        boolean isValidConfig = isValidKeyValuePair(decodedUrl);
        if (isValidConfig) {
            String[] extraConfigs = decodedUrl.split("\\r?\\n");
            for (String cfg : extraConfigs) {
                // Validate cfg against unsupported operations set by admin here
                String[] allowedKeyList = VmwareAdditionalConfigAllowList.value().split(",");
                boolean validXenOrVmwareConfiguration = isValidXenOrVmwareConfiguration(cfg, allowedKeyList);
                String[] paramArray = cfg.split("=");
                if (validXenOrVmwareConfiguration && paramArray.length == 2) {
                    userVmDetailsDao.addDetail(vm.getId(), paramArray[0].trim(), paramArray[1].trim(), true);
                } else {
                    throw new CloudRuntimeException("Extra config " + cfg + " is not on the list of allowed keys for VMware hypervisor hosts.");
                }
            }
        } else {
            throw new CloudRuntimeException("The passed extra config string " + decodedUrl + "contains an invalid key/value pair pattern");
        }
    }

    /**
     * Used to persist extra configuration settings in user_vm_details table for the XenServer hypervisor
     * persists config as key/value pair e.g key = extraconfig-1 , value="PV-bootloader=pygrub" and so on to extraconfig-N where
     * N denotes the number of extra configuration settings passed by user
     *
     * @param decodedUrl A string containing extra configuration settings as key/value pairs seprated by newline escape character
     *                   e.x PV-bootloader=pygrub\nPV-args=console\nHV-Boot-policy=""
     */
    protected void persistExtraConfigXenServer(String decodedUrl, UserVm vm) {
        boolean isValidConfig = isValidKeyValuePair(decodedUrl);
        if (isValidConfig) {
            String[] extraConfigs = decodedUrl.split("\\r?\\n");
            int i = 1;
            String extraConfigKey = ApiConstants.EXTRA_CONFIG + "-";
            for (String cfg : extraConfigs) {
                // Validate cfg against unsupported operations set by admin here
                String[] allowedKeyList = XenServerAdditionalConfigAllowList.value().split(",");
                boolean validXenOrVmwareConfiguration = isValidXenOrVmwareConfiguration(cfg, allowedKeyList);
                if (validXenOrVmwareConfiguration) {
                    userVmDetailsDao.addDetail(vm.getId(), extraConfigKey + String.valueOf(i), cfg, true);
                    i++;
                } else {
                    throw new CloudRuntimeException("Extra config " + cfg + " is not on the list of allowed keys for XenServer hypervisor hosts.");
                }
            }
        } else {
            String msg = String.format("The passed extra config string '%s' contains an invalid key/value pair pattern", decodedUrl);
            throw new CloudRuntimeException(msg);
        }
    }

    /**
     * Used to valid extraconfig keylvalue pair for Vmware and XenServer
     * Example of tested valid config for VMware as taken from VM instance vmx file
     * <p>
     * nvp.vm-uuid=34b3d5ea-1c25-4bb0-9250-8dc3388bfa9b
     * migrate.hostLog=i-2-67-VM-5130f8ab.hlog
     * ethernet0.address=02:00:5f:51:00:41
     * </p>
     * <p>
     * Examples of tested valid configs for XenServer
     * <p>
     * is-a-template=true\nHVM-boot-policy=\nPV-bootloader=pygrub\nPV-args=hvc0
     * </p>
     *
     * Allow the following character set {', ", -, ., =, a-z, 0-9, empty space, \n}
     *
     * @param decodedUrl String conprising of extra config key/value pairs for XenServer and Vmware
     * @return True if extraconfig is valid key/value pair
     */
    protected boolean isValidKeyValuePair(String decodedUrl) {
        // Valid pairs should look like "key-1=value1, param:key-2=value2, my.config.v0=False"
        Pattern pattern = Pattern.compile("^(?:[\\w-\\s\\.:]*=[\\w-\\s\\.'\":]*(?:\\s+|$))+$");
        Matcher matcher = pattern.matcher(decodedUrl);
        return matcher.matches();
    }

    /**
     * Validates key/value pair strings passed as extra configuration for XenServer and Vmware
     * @param cfg configuration key-value pair
     * @param allowedKeyList list of allowed configuration keys for XenServer and VMware
     * @return
     */
    protected boolean isValidXenOrVmwareConfiguration(String cfg, String[] allowedKeyList) {
        // This should be of minimum length 1
        // Value is ignored in case it is empty
        String[] cfgKeyValuePair = cfg.split("=");
        if (cfgKeyValuePair.length >= 1) {
            for (String allowedKey : allowedKeyList) {
                if (cfgKeyValuePair[0].equalsIgnoreCase(allowedKey.trim())) {
                    return true;
                }
            }
        } else {
            String msg = String.format("An incorrect configuration %s has been passed", cfg);
            throw new CloudRuntimeException(msg);
        }
        return false;
    }

    /**
     * Persist extra configuration data on KVM
     * persisted in the user_vm_details DB as extraconfig-1, and so on depending on the number of configurations
     * For KVM, extra config is passed as XML
     * @param decodedUrl string containing xml configuration to be persisted into user_vm_details table
     * @param vm
     */
    protected void persistExtraConfigKvm(String decodedUrl, UserVm vm) {
        // validate config against denied cfg commands
        validateKvmExtraConfig(decodedUrl);
        String[] extraConfigs = decodedUrl.split("\n\n");
        for (String cfg : extraConfigs) {
            int i = 1;
            String[] cfgParts = cfg.split("\n");
            String extraConfigKey = ApiConstants.EXTRA_CONFIG;
            String extraConfigValue;
            if (cfgParts[0].matches("\\S+:$")) {
                extraConfigKey += "-" + cfgParts[0].substring(0, cfgParts[0].length() - 1);
                extraConfigValue = cfg.replace(cfgParts[0] + "\n", "");
            } else {
                extraConfigKey += "-" + String.valueOf(i);
                extraConfigValue = cfg;
            }
            userVmDetailsDao.addDetail(vm.getId(), extraConfigKey, extraConfigValue, true);
            i++;
        }
    }

    /**
     * This method is called by the persistExtraConfigKvm
     * Validates passed extra configuration data for KVM and validates against deny-list of unwanted commands
     * controlled by Root admin
     * @param decodedUrl string containing xml configuration to be validated
     */
    protected void validateKvmExtraConfig(String decodedUrl) {
        String[] allowedConfigOptionList = KvmAdditionalConfigAllowList.value().split(",");
        // Skip allowed keys validation validation for DPDK
        if (!decodedUrl.contains(":")) {
            try {
                DocumentBuilder builder = ParserUtils.getSaferDocumentBuilderFactory().newDocumentBuilder();
                InputSource src = new InputSource();
                src.setCharacterStream(new StringReader(String.format("<config>\n%s\n</config>", decodedUrl)));
                Document doc = builder.parse(src);
                doc.getDocumentElement().normalize();
                NodeList nodeList=doc.getElementsByTagName("*");
                for (int i = 1; i < nodeList.getLength(); i++) { // First element is config so skip it
                    Element element = (Element)nodeList.item(i);
                    boolean isValidConfig = false;
                    String currentConfig = element.getNodeName().trim();
                    for (String tag : allowedConfigOptionList) {
                        if (currentConfig.equals(tag.trim())) {
                            isValidConfig = true;
                        }
                    }
                    if (!isValidConfig) {
                        throw new CloudRuntimeException(String.format("Extra config %s is not on the list of allowed keys for KVM hypervisor hosts", currentConfig));
                    }
                }
            } catch (ParserConfigurationException | IOException | SAXException e) {
                throw new CloudRuntimeException("Failed to parse additional XML configuration: " + e.getMessage());
            }
        }
    }

    /**
     * Adds extra config data to guest VM instances
     * @param extraConfig Extra Configuration settings to be added in UserVm instances for KVM, XenServer and VMware
     */
    protected void addExtraConfig(UserVm vm, String extraConfig) {
        String decodedUrl = decodeExtraConfig(extraConfig);
        HypervisorType hypervisorType = vm.getHypervisorType();

        switch (hypervisorType) {
            case XenServer:
                persistExtraConfigXenServer(decodedUrl, vm);
                break;
            case KVM:
                persistExtraConfigKvm(decodedUrl, vm);
                break;
            case VMware:
                persistExtraConfigVmware(decodedUrl, vm);
                break;
            default:
                String msg = String.format("This hypervisor %s is not supported for use with this feature", hypervisorType.toString());
                throw new CloudRuntimeException(msg);
        }
    }

    /**
     * Decodes an URL encoded string passed as extra configuration for guest VMs
     * @param encodeString URL encoded string
     * @return String result of decoded URL
     */
    protected String decodeExtraConfig(String encodeString) {
        String decodedUrl;
        try {
            decodedUrl = URLDecoder.decode(encodeString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Failed to provided decode URL string: " + e.getMessage());
        }
        return decodedUrl;
    }

    protected List<Long> getSecurityGroupIdList(SecurityGroupAction cmd) {
        if (cmd.getSecurityGroupNameList() != null && cmd.getSecurityGroupIdList() != null) {
            throw new InvalidParameterValueException("securitygroupids parameter is mutually exclusive with securitygroupnames parameter");
        }

        //transform group names to ids here
        if (cmd.getSecurityGroupNameList() != null) {
            List<Long> securityGroupIds = new ArrayList<Long>();
            for (String groupName : cmd.getSecurityGroupNameList()) {
                SecurityGroup sg = _securityGroupMgr.getSecurityGroup(groupName, cmd.getEntityOwnerId());
                if (sg == null) {
                    throw new InvalidParameterValueException("Unable to find group by name " + groupName);
                } else {
                    securityGroupIds.add(sg.getId());
                }
            }
            return securityGroupIds;
        } else {
            return cmd.getSecurityGroupIdList();
        }
    }

    // this is an opportunity to verify that parameters that came in via the Details Map are OK
    // for example, minIops and maxIops should either both be specified or neither be specified and,
    // if specified, minIops should be <= maxIops
    private void verifyDetails(Map<String,String> details) {
        if (details != null) {
            String minIops = details.get("minIops");
            String maxIops = details.get("maxIops");

            verifyMinAndMaxIops(minIops, maxIops);

            minIops = details.get("minIopsDo");
            maxIops = details.get("maxIopsDo");

            verifyMinAndMaxIops(minIops, maxIops);

            if (details.containsKey("extraconfig")) {
                throw new InvalidParameterValueException("'extraconfig' should not be included in details as key");
            }
        }
    }

    private void verifyMinAndMaxIops(String minIops, String maxIops) {
        if ((minIops != null && maxIops == null) || (minIops == null && maxIops != null)) {
            throw new InvalidParameterValueException("Either 'Min IOPS' and 'Max IOPS' must both be specified or neither be specified.");
        }

        long lMinIops;

        try {
            if (minIops != null) {
                lMinIops = Long.parseLong(minIops);
            }
            else {
                lMinIops = 0;
            }
        }
        catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("'Min IOPS' must be a whole number.");
        }

        long lMaxIops;

        try {
            if (maxIops != null) {
                lMaxIops = Long.parseLong(maxIops);
            }
            else {
                lMaxIops = 0;
            }
        }
        catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("'Max IOPS' must be a whole number.");
        }

        if (lMinIops > lMaxIops) {
            throw new InvalidParameterValueException("'Min IOPS' must be less than or equal to 'Max IOPS'.");
        }
    }

    @Override
    public UserVm getUserVm(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public VirtualMachine getVm(long vmId) {
        return _vmInstanceDao.findById(vmId);
    }

    private VMInstanceVO preVmStorageMigrationCheck(Long vmId) {
        // access check - only root admin can migrate VM
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + vmId);
        }

        if (vm.getState() != State.Stopped) {
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Stopped, unable to migrate the vm having the specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        HypervisorType hypervisorType = vm.getHypervisorType();
        if (vm.getType() != VirtualMachine.Type.User && !HYPERVISORS_THAT_CAN_DO_STORAGE_MIGRATION_ON_NON_USER_VMS.contains(hypervisorType)) {
            throw new InvalidParameterValueException(String.format("Unable to migrate storage of non-user VMs for hypervisor [%s]. Operation only supported for the following"
                    + " hypervisors: [%s].", hypervisorType, HYPERVISORS_THAT_CAN_DO_STORAGE_MIGRATION_ON_NON_USER_VMS));
        }

        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        if (vols.size() > 1) {
            // OffLineVmwareMigration: data disks are not permitted, here!
            if (vols.size() > 1 &&
                    // OffLineVmwareMigration: allow multiple disks for vmware
                    !HypervisorType.VMware.equals(hypervisorType)) {
                throw new InvalidParameterValueException("Data disks attached to the vm, can not migrate. Need to detach data disks first");
            }
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("VM's disk cannot be migrated, please remove all the VM Snapshots for this VM");
        }

        return vm;
    }

    private VirtualMachine findMigratedVm(long vmId, VirtualMachine.Type vmType) {
        if (VirtualMachine.Type.User.equals(vmType)) {
            return _vmDao.findById(vmId);
        }
        return _vmInstanceDao.findById(vmId);
    }

    @Override
    public VirtualMachine vmStorageMigration(Long vmId, StoragePool destPool) {
        VMInstanceVO vm = preVmStorageMigrationCheck(vmId);
        Map<Long, Long> volumeToPoolIds = new HashMap<>();
        checkDestinationHypervisorType(destPool, vm);
        List<VolumeVO> volumes = _volsDao.findByInstance(vm.getId());
        StoragePoolVO destinationPoolVo = _storagePoolDao.findById(destPool.getId());
        Long destPoolPodId = ScopeType.CLUSTER.equals(destinationPoolVo.getScope()) || ScopeType.HOST.equals(destinationPoolVo.getScope()) ?
                destinationPoolVo.getPodId() : null;
        for (VolumeVO volume : volumes) {
            if (!VirtualMachine.Type.User.equals(vm.getType())) {
                // Migrate within same pod as source storage and same cluster for all disks only. Hypervisor check already done
                StoragePoolVO pool = _storagePoolDao.findById(volume.getPoolId());
                if (destPoolPodId != null &&
                        (ScopeType.CLUSTER.equals(pool.getScope()) || ScopeType.HOST.equals(pool.getScope())) &&
                        !destPoolPodId.equals(pool.getPodId())) {
                    throw new InvalidParameterValueException("Storage migration of non-user VMs cannot be done between storage pools of different pods");
                }
            }
            volumeToPoolIds.put(volume.getId(), destPool.getId());
        }
        _itMgr.storageMigration(vm.getUuid(), volumeToPoolIds);
        return findMigratedVm(vm.getId(), vm.getType());
    }

    @Override
    public VirtualMachine vmStorageMigration(Long vmId, Map<String, String> volumeToPool) {
        VMInstanceVO vm = preVmStorageMigrationCheck(vmId);
        Map<Long, Long> volumeToPoolIds = new HashMap<>();
        Long poolClusterId = null;
        for (Map.Entry<String, String> entry : volumeToPool.entrySet()) {
            Volume volume = _volsDao.findByUuid(entry.getKey());
            StoragePoolVO pool = _storagePoolDao.findPoolByUUID(entry.getValue());
            if (poolClusterId != null &&
                    (ScopeType.CLUSTER.equals(pool.getScope()) || ScopeType.HOST.equals(pool.getScope())) &&
                    !poolClusterId.equals(pool.getClusterId())) {
                throw new InvalidParameterValueException("VM's disk cannot be migrated, input destination storage pools belong to different clusters");
            }
            if (pool.getClusterId() != null) {
                poolClusterId = pool.getClusterId();
            }
            checkDestinationHypervisorType(pool, vm);
            volumeToPoolIds.put(volume.getId(), pool.getId());
        }
        _itMgr.storageMigration(vm.getUuid(), volumeToPoolIds);
        return findMigratedVm(vm.getId(), vm.getType());
    }

    private void checkDestinationHypervisorType(StoragePool destPool, VMInstanceVO vm) {
        HypervisorType destHypervisorType = destPool.getHypervisor();
        if (destHypervisorType == null) {
            destHypervisorType = _clusterDao.findById(
                    destPool.getClusterId()).getHypervisorType();
        }

        if (vm.getHypervisorType() != destHypervisorType && destHypervisorType != HypervisorType.Any) {
            throw new InvalidParameterValueException("hypervisor is not compatible: dest: " + destHypervisorType.toString() + ", vm: " + vm.getHypervisorType().toString());
        }

    }

    public boolean isVMUsingLocalStorage(VMInstanceVO vm) {
        boolean usesLocalStorage = false;

        List<VolumeVO> volumes = _volsDao.findByInstance(vm.getId());
        for (VolumeVO vol : volumes) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
            if (diskOffering.isUseLocalStorage()) {
                usesLocalStorage = true;
                break;
            }
            StoragePoolVO storagePool = _storagePoolDao.findById(vol.getPoolId());
            if (storagePool.isLocal()) {
                usesLocalStorage = true;
                break;
            }
        }
        return usesLocalStorage;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MIGRATE, eventDescription = "migrating VM", async = true)
    public VirtualMachine migrateVirtualMachine(Long vmId, Host destinationHost) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
    VirtualMachineMigrationException {
        // access check - only root admin can migrate VM
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + vmId);
        }
        // business logic
        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Running, unable to migrate the vm with specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        checkIfHostOfVMIsInPrepareForMaintenanceState(vm.getHostId(), vmId, "Migrate");

        if(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()) != null) {
            throw new InvalidParameterValueException("Live Migration of GPU enabled VM is not supported");
        }

        if (!isOnSupportedHypevisorForMigration(vm)) {
            s_logger.error(vm + " is not XenServer/VMware/KVM/Ovm/Hyperv, cannot migrate this VM from hypervisor type " + vm.getHypervisorType());
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        if (vm.getType().equals(VirtualMachine.Type.User) && vm.getHypervisorType().equals(HypervisorType.LXC)) {
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for User VM migration, we support XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        if (isVMUsingLocalStorage(vm)) {
            s_logger.error(vm + " is using Local Storage, cannot migrate this VM.");
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }

        // check if migrating to same host
        long srcHostId = vm.getHostId();
        Host srcHost = _resourceMgr.getHost(srcHostId);
        if (srcHost == null) {
            throw new InvalidParameterValueException("Cannot migrate VM, host with id: " + srcHostId + " for VM not found");
        }

        DeployDestination dest = null;
        if (destinationHost == null) {
            dest = chooseVmMigrationDestination(vm, srcHost);
        } else {
            dest = checkVmMigrationDestination(vm, srcHost, destinationHost);
        }

        // If no suitable destination found then throw exception
        if (dest == null) {
            throw new CloudRuntimeException("Unable to find suitable destination to migrate VM " + vm.getInstanceName());
        }

        collectVmDiskAndNetworkStatistics(vmId, State.Running);
        _itMgr.migrate(vm.getUuid(), srcHostId, dest);
        return findMigratedVm(vm.getId(), vm.getType());
    }

    private DeployDestination chooseVmMigrationDestination(VMInstanceVO vm, Host srcHost) {
        vm.setLastHostId(null); // Last host does not have higher priority in vm migration
        final ServiceOfferingVO offering = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm, null, offering, null, null);
        final Long srcHostId = srcHost.getId();
        final Host host = _hostDao.findById(srcHostId);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(srcHostId);
        final DataCenterDeployment plan = _itMgr.getMigrationDeployment(vm, host, null, excludes);
        try {
            return _planningMgr.planDeployment(profile, plan, excludes, null);
        } catch (final AffinityConflictException e2) {
            s_logger.warn("Unable to create deployment, affinity rules associated to the VM conflict", e2);
            throw new CloudRuntimeException("Unable to create deployment, affinity rules associated to the VM conflict");
        } catch (final InsufficientServerCapacityException e3) {
            throw new CloudRuntimeException("Unable to find a server to migrate the vm to");
        }
    }

    private DeployDestination checkVmMigrationDestination(VMInstanceVO vm, Host srcHost, Host destinationHost) throws VirtualMachineMigrationException {
        if (destinationHost == null) {
            return null;
        }
        if (destinationHost.getId() == srcHost.getId()) {
            throw new InvalidParameterValueException("Cannot migrate VM, VM is already present on this host, please specify valid destination host to migrate the VM");
        }

        // check if host is UP
        if (destinationHost.getState() != com.cloud.host.Status.Up || destinationHost.getResourceState() != ResourceState.Enabled) {
            throw new InvalidParameterValueException("Cannot migrate VM, destination host is not in correct state, has status: " + destinationHost.getState() + ", state: "
                    + destinationHost.getResourceState());
        }

        if (vm.getType() != VirtualMachine.Type.User) {
            // for System VMs check that the destination host is within the same pod
            if (srcHost.getPodId() != null && !srcHost.getPodId().equals(destinationHost.getPodId())) {
                throw new InvalidParameterValueException("Cannot migrate the VM, destination host is not in the same pod as current host of the VM");
            }
        }

        if (dpdkHelper.isVMDpdkEnabled(vm.getId()) && !dpdkHelper.isHostDpdkEnabled(destinationHost.getId())) {
            throw new CloudRuntimeException("Cannot migrate VM, VM is DPDK enabled VM but destination host is not DPDK enabled");
        }

        checkHostsDedication(vm, srcHost.getId(), destinationHost.getId());

        // call to core process
        DataCenterVO dcVO = _dcDao.findById(destinationHost.getDataCenterId());
        HostPodVO pod = _podDao.findById(destinationHost.getPodId());
        Cluster cluster = _clusterDao.findById(destinationHost.getClusterId());
        DeployDestination dest = new DeployDestination(dcVO, pod, cluster, destinationHost);

        // check max guest vm limit for the destinationHost
        HostVO destinationHostVO = _hostDao.findById(destinationHost.getId());
        if (_capacityMgr.checkIfHostReachMaxGuestLimit(destinationHostVO)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Host name: " + destinationHost.getName() + ", hostId: " + destinationHost.getId()
                + " already has max Running VMs(count includes system VMs), cannot migrate to this host");
            }
            throw new VirtualMachineMigrationException("Destination host, hostId: " + destinationHost.getId()
            + " already has max Running VMs(count includes system VMs), cannot migrate to this host");
        }
        //check if there are any ongoing volume snapshots on the volumes associated with the VM.
        Long vmId = vm.getId();
        s_logger.debug("Checking if there are any ongoing snapshots volumes associated with VM with ID " + vmId);
        if (checkStatusOfVolumeSnapshots(vmId, null)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on volume(s) attached to this VM, VM Migration is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volumes associated with the vm with id " + vmId);

        return dest;
    }

    private boolean isOnSupportedHypevisorForMigration(VMInstanceVO vm) {
        return (vm.getHypervisorType().equals(HypervisorType.XenServer) ||
                vm.getHypervisorType().equals(HypervisorType.VMware) ||
                vm.getHypervisorType().equals(HypervisorType.KVM) ||
                vm.getHypervisorType().equals(HypervisorType.Ovm) ||
                vm.getHypervisorType().equals(HypervisorType.Hyperv) ||
                vm.getHypervisorType().equals(HypervisorType.LXC) ||
                vm.getHypervisorType().equals(HypervisorType.Simulator) ||
                vm.getHypervisorType().equals(HypervisorType.Ovm3));
    }

    private boolean checkIfHostIsDedicated(HostVO host) {
        long hostId = host.getId();
        DedicatedResourceVO dedicatedHost = _dedicatedDao.findByHostId(hostId);
        DedicatedResourceVO dedicatedClusterOfHost = _dedicatedDao.findByClusterId(host.getClusterId());
        DedicatedResourceVO dedicatedPodOfHost = _dedicatedDao.findByPodId(host.getPodId());
        if (dedicatedHost != null || dedicatedClusterOfHost != null || dedicatedPodOfHost != null) {
            return true;
        } else {
            return false;
        }
    }

    private void checkIfHostOfVMIsInPrepareForMaintenanceState(Long hostId, Long vmId, String operation) {
        HostVO host = _hostDao.findById(hostId);
        if (host.getResourceState() != ResourceState.PrepareForMaintenance) {
            return;
        }

        s_logger.debug("Host is in PrepareForMaintenance state - " + operation + " VM operation on the VM id: " + vmId + " is not allowed");
        throw new InvalidParameterValueException(operation + " VM operation on the VM id: " + vmId + " is not allowed as host is preparing for maintenance mode");
    }

    private Long accountOfDedicatedHost(HostVO host) {
        long hostId = host.getId();
        DedicatedResourceVO dedicatedHost = _dedicatedDao.findByHostId(hostId);
        DedicatedResourceVO dedicatedClusterOfHost = _dedicatedDao.findByClusterId(host.getClusterId());
        DedicatedResourceVO dedicatedPodOfHost = _dedicatedDao.findByPodId(host.getPodId());
        if (dedicatedHost != null) {
            return dedicatedHost.getAccountId();
        }
        if (dedicatedClusterOfHost != null) {
            return dedicatedClusterOfHost.getAccountId();
        }
        if (dedicatedPodOfHost != null) {
            return dedicatedPodOfHost.getAccountId();
        }
        return null;
    }

    private Long domainOfDedicatedHost(HostVO host) {
        long hostId = host.getId();
        DedicatedResourceVO dedicatedHost = _dedicatedDao.findByHostId(hostId);
        DedicatedResourceVO dedicatedClusterOfHost = _dedicatedDao.findByClusterId(host.getClusterId());
        DedicatedResourceVO dedicatedPodOfHost = _dedicatedDao.findByPodId(host.getPodId());
        if (dedicatedHost != null) {
            return dedicatedHost.getDomainId();
        }
        if (dedicatedClusterOfHost != null) {
            return dedicatedClusterOfHost.getDomainId();
        }
        if (dedicatedPodOfHost != null) {
            return dedicatedPodOfHost.getDomainId();
        }
        return null;
    }

    public void checkHostsDedication(VMInstanceVO vm, long srcHostId, long destHostId) {
        HostVO srcHost = _hostDao.findById(srcHostId);
        HostVO destHost = _hostDao.findById(destHostId);
        boolean srcExplDedicated = checkIfHostIsDedicated(srcHost);
        boolean destExplDedicated = checkIfHostIsDedicated(destHost);
        //if srcHost is explicitly dedicated and destination Host is not
        if (srcExplDedicated && !destExplDedicated) {
            //raise an alert
            String msg = "VM is being migrated from a explicitly dedicated host " + srcHost.getName() + " to non-dedicated host " + destHost.getName();
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
            s_logger.warn(msg);
        }
        //if srcHost is non dedicated but destination Host is explicitly dedicated
        if (!srcExplDedicated && destExplDedicated) {
            //raise an alert
            String msg = "VM is being migrated from a non dedicated host " + srcHost.getName() + " to a explicitly dedicated host " + destHost.getName();
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
            s_logger.warn(msg);
        }

        //if hosts are dedicated to different account/domains, raise an alert
        if (srcExplDedicated && destExplDedicated) {
            if (!((accountOfDedicatedHost(srcHost) == null) || (accountOfDedicatedHost(srcHost).equals(accountOfDedicatedHost(destHost))))) {
                String msg = "VM is being migrated from host " + srcHost.getName() + " explicitly dedicated to account " + accountOfDedicatedHost(srcHost) + " to host "
                        + destHost.getName() + " explicitly dedicated to account " + accountOfDedicatedHost(destHost);
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
                s_logger.warn(msg);
            }
            if (!((domainOfDedicatedHost(srcHost) == null) || (domainOfDedicatedHost(srcHost).equals(domainOfDedicatedHost(destHost))))) {
                String msg = "VM is being migrated from host " + srcHost.getName() + " explicitly dedicated to domain " + domainOfDedicatedHost(srcHost) + " to host "
                        + destHost.getName() + " explicitly dedicated to domain " + domainOfDedicatedHost(destHost);
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
                s_logger.warn(msg);
            }
        }

        // Checks for implicitly dedicated hosts
        ServiceOfferingVO deployPlanner = serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
        if (deployPlanner.getDeploymentPlanner() != null && deployPlanner.getDeploymentPlanner().equals("ImplicitDedicationPlanner")) {
            //VM is deployed using implicit planner
            long accountOfVm = vm.getAccountId();
            String msg = "VM of account " + accountOfVm + " with implicit deployment planner being migrated to host " + destHost.getName();
            //Get all vms on destination host
            boolean emptyDestination = false;
            List<VMInstanceVO> vmsOnDest = getVmsOnHost(destHostId);
            if (vmsOnDest == null || vmsOnDest.isEmpty()) {
                emptyDestination = true;
            }

            if (!emptyDestination) {
                //Check if vm is deployed using strict implicit planner
                if (!isServiceOfferingUsingPlannerInPreferredMode(vm.getServiceOfferingId())) {
                    //Check if all vms on destination host are created using strict implicit mode
                    if (!checkIfAllVmsCreatedInStrictMode(accountOfVm, vmsOnDest)) {
                        msg = "VM of account " + accountOfVm + " with strict implicit deployment planner being migrated to host " + destHost.getName()
                        + " not having all vms strict implicitly dedicated to account " + accountOfVm;
                    }
                } else {
                    //If vm is deployed using preferred implicit planner, check if all vms on destination host must be
                    //using implicit planner and must belong to same account
                    for (VMInstanceVO vmsDest : vmsOnDest) {
                        ServiceOfferingVO destPlanner = serviceOfferingDao.findById(vm.getId(), vmsDest.getServiceOfferingId());
                        if (!((destPlanner.getDeploymentPlanner() != null && destPlanner.getDeploymentPlanner().equals("ImplicitDedicationPlanner")) && vmsDest.getAccountId() == accountOfVm)) {
                            msg = "VM of account " + accountOfVm + " with preffered implicit deployment planner being migrated to host " + destHost.getName()
                            + " not having all vms implicitly dedicated to account " + accountOfVm;
                        }
                    }
                }
            }
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
            s_logger.warn(msg);

        } else {
            //VM is not deployed using implicit planner, check if it migrated between dedicated hosts
            List<PlannerHostReservationVO> reservedHosts = _plannerHostReservationDao.listAllDedicatedHosts();
            boolean srcImplDedicated = false;
            boolean destImplDedicated = false;
            String msg = null;
            for (PlannerHostReservationVO reservedHost : reservedHosts) {
                if (reservedHost.getHostId() == srcHostId) {
                    srcImplDedicated = true;
                }
                if (reservedHost.getHostId() == destHostId) {
                    destImplDedicated = true;
                }
            }
            if (srcImplDedicated) {
                if (destImplDedicated) {
                    msg = "VM is being migrated from implicitly dedicated host " + srcHost.getName() + " to another implicitly dedicated host " + destHost.getName();
                } else {
                    msg = "VM is being migrated from implicitly dedicated host " + srcHost.getName() + " to shared host " + destHost.getName();
                }
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
                s_logger.warn(msg);
            } else {
                if (destImplDedicated) {
                    msg = "VM is being migrated from shared host " + srcHost.getName() + " to implicitly dedicated host " + destHost.getName();
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);
                    s_logger.warn(msg);
                }
            }
        }
    }

    private List<VMInstanceVO> getVmsOnHost(long hostId) {
        List<VMInstanceVO> vms =  _vmInstanceDao.listUpByHostId(hostId);
        List<VMInstanceVO> vmsByLastHostId = _vmInstanceDao.listByLastHostId(hostId);
        if (vmsByLastHostId.size() > 0) {
            // check if any VMs are within skip.counting.hours, if yes we have to consider the host.
            for (VMInstanceVO stoppedVM : vmsByLastHostId) {
                long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - stoppedVM.getUpdateTime().getTime()) / 1000;
                if (secondsSinceLastUpdate < capacityReleaseInterval) {
                    vms.add(stoppedVM);
                }
            }
        }

        return vms;
    }

    private boolean isServiceOfferingUsingPlannerInPreferredMode(long serviceOfferingId) {
        boolean preferred = false;
        Map<String, String> details = serviceOfferingDetailsDao.listDetailsKeyPairs(serviceOfferingId);
        if (details != null && !details.isEmpty()) {
            String preferredAttribute = details.get("ImplicitDedicationMode");
            if (preferredAttribute != null && preferredAttribute.equals("Preferred")) {
                preferred = true;
            }
        }
        return preferred;
    }

    private boolean checkIfAllVmsCreatedInStrictMode(Long accountId, List<VMInstanceVO> allVmsOnHost) {
        boolean createdByImplicitStrict = true;
        if (allVmsOnHost.isEmpty()) {
            return false;
        }
        for (VMInstanceVO vm : allVmsOnHost) {
            if (!isImplicitPlannerUsedByOffering(vm.getServiceOfferingId()) || vm.getAccountId() != accountId) {
                s_logger.info("Host " + vm.getHostId() + " found to be running a vm created by a planner other" + " than implicit, or running vms of other account");
                createdByImplicitStrict = false;
                break;
            } else if (isServiceOfferingUsingPlannerInPreferredMode(vm.getServiceOfferingId()) || vm.getAccountId() != accountId) {
                s_logger.info("Host " + vm.getHostId() + " found to be running a vm created by an implicit planner" + " in preferred mode, or running vms of other account");
                createdByImplicitStrict = false;
                break;
            }
        }
        return createdByImplicitStrict;
    }

    private boolean isImplicitPlannerUsedByOffering(long offeringId) {
        boolean implicitPlannerUsed = false;
        ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(offeringId);
        if (offering == null) {
            s_logger.error("Couldn't retrieve the offering by the given id : " + offeringId);
        } else {
            String plannerName = offering.getDeploymentPlanner();
            if (plannerName != null) {
                if (plannerName.equals("ImplicitDedicationPlanner")) {
                    implicitPlannerUsed = true;
                }
            }
        }

        return implicitPlannerUsed;
    }

    private boolean isVmVolumesOnZoneWideStore(VMInstanceVO vm) {
        final List<VolumeVO> volumes = _volsDao.findCreatedByInstance(vm.getId());
        if (CollectionUtils.isEmpty(volumes)) {
            return false;
        }
        for (Volume volume : volumes) {
            if (volume == null || volume.getPoolId() == null) {
                return false;
            }
            StoragePoolVO pool = _storagePoolDao.findById(volume.getPoolId());
            if (pool == null || !ScopeType.ZONE.equals(pool.getScope())) {
                return false;
            }
        }
        return true;
    }

    private Pair<Host, Host> getHostsForMigrateVmWithStorage(VMInstanceVO vm, Host destinationHost) throws VirtualMachineMigrationException {
        long srcHostId = vm.getHostId();
        Host srcHost = _resourceMgr.getHost(srcHostId);

        if (srcHost == null) {
            throw new InvalidParameterValueException("Cannot migrate VM, host with ID: " + srcHostId + " for VM not found");
        }

        // Check if source and destination hosts are valid and migrating to same host
        if (destinationHost.getId() == srcHostId) {
            throw new InvalidParameterValueException(String.format("Cannot migrate VM as it is already present on host %s (ID: %s), please specify valid destination host to migrate the VM",
                    destinationHost.getName(), destinationHost.getUuid()));
        }

        String srcHostVersion = srcHost.getHypervisorVersion();
        String destHostVersion = destinationHost.getHypervisorVersion();

        // Check if the source and destination hosts are of the same type and support storage motion.
        if (!srcHost.getHypervisorType().equals(destinationHost.getHypervisorType())) {
            throw new CloudRuntimeException("The source and destination hosts are not of the same type and version. Source hypervisor type and version: " +
                    srcHost.getHypervisorType().toString() + " " + srcHostVersion + ", Destination hypervisor type and version: " +
                    destinationHost.getHypervisorType().toString() + " " + destHostVersion);
        }

        if (!VirtualMachine.Type.User.equals(vm.getType())) {
            // for System VMs check that the destination host is within the same pod
            if (srcHost.getPodId() != null && !srcHost.getPodId().equals(destinationHost.getPodId())) {
                throw new InvalidParameterValueException("Cannot migrate the VM, destination host is not in the same pod as current host of the VM");
            }
        }

        if (HypervisorType.KVM.equals(srcHost.getHypervisorType())) {
            if (srcHostVersion == null) {
                srcHostVersion = "";
            }

            if (destHostVersion == null) {
                destHostVersion = "";
            }
        }

        if (!_hypervisorCapabilitiesDao.isStorageMotionSupported(srcHost.getHypervisorType(), srcHostVersion)) {
            throw new CloudRuntimeException(String.format("Migration with storage isn't supported for source host %s (ID: %s) on hypervisor %s with version %s", srcHost.getName(), srcHost.getUuid(), srcHost.getHypervisorType(), srcHost.getHypervisorVersion()));
        }

        if (srcHostVersion == null || !srcHostVersion.equals(destHostVersion)) {
            if (!_hypervisorCapabilitiesDao.isStorageMotionSupported(destinationHost.getHypervisorType(), destHostVersion)) {
                throw new CloudRuntimeException(String.format("Migration with storage isn't supported for target host %s (ID: %s) on hypervisor %s with version %s", destinationHost.getName(), destinationHost.getUuid(), destinationHost.getHypervisorType(), destinationHost.getHypervisorVersion()));
            }
        }

        // Check if destination host is up.
        if (destinationHost.getState() != com.cloud.host.Status.Up || destinationHost.getResourceState() != ResourceState.Enabled) {
            throw new CloudRuntimeException(String.format("Cannot migrate VM, destination host %s (ID: %s) is not in correct state, has status: %s, state: %s",
                    destinationHost.getName(), destinationHost.getUuid(), destinationHost.getState(), destinationHost.getResourceState()));
        }

        // Check max guest vm limit for the destinationHost.
        if (_capacityMgr.checkIfHostReachMaxGuestLimit(destinationHost)) {
            throw new VirtualMachineMigrationException(String.format("Cannot migrate VM as destination host %s (ID: %s) already has max running vms (count includes system VMs)",
                    destinationHost.getName(), destinationHost.getUuid()));
        }

        return new Pair<>(srcHost, destinationHost);
    }

    private List<VolumeVO> getVmVolumesForMigrateVmWithStorage(VMInstanceVO vm) {
        List<VolumeVO> vmVolumes = _volsDao.findUsableVolumesForInstance(vm.getId());
        for (VolumeVO volume : vmVolumes) {
            if (volume.getState() != Volume.State.Ready) {
                throw new CloudRuntimeException(String.format("Volume %s (ID: %s) of the VM is not in Ready state. Cannot migrate the VM %s (ID: %s) with its volumes", volume.getName(), volume.getUuid(), vm.getInstanceName(), vm.getUuid()));
            }
        }
        return vmVolumes;
    }

    private Map<Long, Long> getVolumePoolMappingForMigrateVmWithStorage(VMInstanceVO vm, Map<String, String> volumeToPool) {
        Map<Long, Long> volToPoolObjectMap = new HashMap<Long, Long>();

        List<VolumeVO> vmVolumes = getVmVolumesForMigrateVmWithStorage(vm);

        if (MapUtils.isNotEmpty(volumeToPool)) {
            // Check if all the volumes and pools passed as parameters are valid.
            for (Map.Entry<String, String> entry : volumeToPool.entrySet()) {
                VolumeVO volume = _volsDao.findByUuid(entry.getKey());
                StoragePoolVO pool = _storagePoolDao.findByUuid(entry.getValue());
                if (volume == null) {
                    throw new InvalidParameterValueException("There is no volume present with the given id " + entry.getKey());
                } else if (pool == null) {
                    throw new InvalidParameterValueException("There is no storage pool present with the given id " + entry.getValue());
                } else if (pool.isInMaintenance()) {
                    throw new InvalidParameterValueException("Cannot migrate volume " + volume + "to the destination storage pool " + pool.getName() +
                            " as the storage pool is in maintenance mode.");
                } else {
                    // Verify the volume given belongs to the vm.
                    if (!vmVolumes.contains(volume)) {
                        throw new InvalidParameterValueException(String.format("Volume " + volume + " doesn't belong to the VM %s (ID: %s) that has to be migrated", vm.getInstanceName(), vm.getUuid()));
                    }
                    volToPoolObjectMap.put(volume.getId(), pool.getId());
                }
                HypervisorType hypervisorType = _volsDao.getHypervisorType(volume.getId());

                try {
                    snapshotHelper.checkKvmVolumeSnapshotsOnlyInPrimaryStorage(volume, hypervisorType);
                } catch (CloudRuntimeException ex) {
                    throw new CloudRuntimeException(String.format("Unable to migrate %s to the destination storage pool [%s] due to [%s]", volume,
                            new ToStringBuilder(pool, ToStringStyle.JSON_STYLE).append("uuid", pool.getUuid()).append("name", pool.getName()).toString(), ex.getMessage()), ex);
                }

                if (hypervisorType.equals(HypervisorType.VMware)) {
                    try {
                        DiskOffering diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
                        DiskProfile diskProfile = new DiskProfile(volume, diskOffering, _volsDao.getHypervisorType(volume.getId()));
                        Pair<Volume, DiskProfile> volumeDiskProfilePair = new Pair<>(volume, diskProfile);
                        boolean isStoragePoolStoragepolicyCompliance = storageManager.isStoragePoolCompliantWithStoragePolicy(Arrays.asList(volumeDiskProfilePair), pool);
                        if (!isStoragePoolStoragepolicyCompliance) {
                            throw new CloudRuntimeException(String.format("Storage pool %s is not storage policy compliance with the volume %s", pool.getUuid(), volume.getUuid()));
                        }
                    } catch (StorageUnavailableException e) {
                        throw new CloudRuntimeException(String.format("Could not verify storage policy compliance against storage pool %s due to exception %s", pool.getUuid(), e.getMessage()));
                    }
                }
            }
        }
        return volToPoolObjectMap;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MIGRATE, eventDescription = "migrating VM", async = true)
    public VirtualMachine migrateVirtualMachineWithVolume(Long vmId, Host destinationHost, Map<String, String> volumeToPool) throws ResourceUnavailableException,
    ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {
        // Access check - only root administrator can migrate VM.
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by ID " + vmId);
        }

        // OfflineVmwareMigration: this would be it ;) if multiple paths exist: unify
        if (vm.getState() != State.Running) {
            // OfflineVmwareMigration: and not vmware
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            CloudRuntimeException ex = new CloudRuntimeException(String.format("Unable to migrate the VM %s (ID: %s) as it is not in Running state", vm.getInstanceName(), vm.getUuid()));
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        if(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()) != null) {
            throw new InvalidParameterValueException("Live Migration of GPU enabled VM is not supported");
        }

        if (!VM_STORAGE_MIGRATION_SUPPORTING_HYPERVISORS.contains(vm.getHypervisorType())) {
            throw new InvalidParameterValueException(String.format("Unsupported hypervisor: %s for VM migration, we support XenServer/VMware/KVM only", vm.getHypervisorType()));
        }

        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("VM with VM Snapshots cannot be migrated with storage, please remove all VM snapshots");
        }

        Pair<Host, Host> sourceDestinationHosts = getHostsForMigrateVmWithStorage(vm, destinationHost);
        Host srcHost = sourceDestinationHosts.first();

        if (!isVMUsingLocalStorage(vm) && MapUtils.isEmpty(volumeToPool)
                && (destinationHost.getClusterId().equals(srcHost.getClusterId()) || isVmVolumesOnZoneWideStore(vm))){
            // If volumes do not have to be migrated
            // call migrateVirtualMachine for non-user VMs else throw exception
            if (!VirtualMachine.Type.User.equals(vm.getType())) {
                return migrateVirtualMachine(vmId, destinationHost);
            }
            throw new InvalidParameterValueException(String.format("Migration of the VM: %s (ID: %s) from host %s (ID: %s) to destination host  %s (ID: %s) doesn't involve migrating the volumes",
                    vm.getInstanceName(), vm.getUuid(), srcHost.getName(), srcHost.getUuid(), destinationHost.getName(), destinationHost.getUuid()));
        }

        Map<Long, Long> volToPoolObjectMap = getVolumePoolMappingForMigrateVmWithStorage(vm, volumeToPool);

        checkHostsDedication(vm, srcHost.getId(), destinationHost.getId());

        _itMgr.migrateWithStorage(vm.getUuid(), srcHost.getId(), destinationHost.getId(), volToPoolObjectMap);
        return findMigratedVm(vm.getId(), vm.getType());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MOVE, eventDescription = "move VM to another user", async = false)
    public UserVm moveVMToUser(final AssignVMCmd cmd) throws ResourceAllocationException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // VERIFICATIONS and VALIDATIONS

        // VV 1: verify the two users
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getId())
                && !_accountMgr.isDomainAdmin(caller.getId())) { // only
            // root
            // admin
            // can
            // assign
            // VMs
            throw new InvalidParameterValueException("Only domain admins are allowed to assign VMs and not " + caller.getType());
        }

        // get and check the valid VM
        final UserVmVO vm = _vmDao.findById(cmd.getVmId());
        if (vm == null) {
            throw new InvalidParameterValueException("There is no vm by that id " + cmd.getVmId());
        } else if (vm.getState() == State.Running) { // VV 3: check if vm is
            // running
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is Running, unable to move the vm " + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is Running, unable to move the vm with specified vmId");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        final Account oldAccount = _accountService.getActiveAccountById(vm.getAccountId());
        if (oldAccount == null) {
            throw new InvalidParameterValueException("Invalid account for VM " + vm.getAccountId() + " in domain.");
        }
        final Account newAccount = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        if (newAccount == null) {
            throw new InvalidParameterValueException("Invalid accountid=" + cmd.getAccountName() + " in domain " + cmd.getDomainId());
        }

        if (newAccount.getState() == Account.State.DISABLED) {
            throw new InvalidParameterValueException("The new account owner " + cmd.getAccountName() + " is disabled.");
        }

        if (cmd.getProjectId() != null && cmd.getDomainId() == null) {
            throw new InvalidParameterValueException("Please provide a valid domain ID; cannot assign VM to a project if domain ID is NULL.");
        }

        //check caller has access to both the old and new account
        _accountMgr.checkAccess(caller, null, true, oldAccount);
        _accountMgr.checkAccess(caller, null, true, newAccount);

        // make sure the accounts are not same
        if (oldAccount.getAccountId() == newAccount.getAccountId()) {
            throw new InvalidParameterValueException("The new account is the same as the old account. Account id =" + oldAccount.getAccountId());
        }

        // don't allow to move the vm if there are existing PF/LB/Static Nat
        // rules, or vm is assigned to static Nat ip
        List<PortForwardingRuleVO> pfrules = _portForwardingDao.listByVm(cmd.getVmId());
        if (pfrules != null && pfrules.size() > 0) {
            throw new InvalidParameterValueException("Remove the Port forwarding rules for this VM before assigning to another user.");
        }
        List<FirewallRuleVO> snrules = _rulesDao.listStaticNatByVmId(vm.getId());
        if (snrules != null && snrules.size() > 0) {
            throw new InvalidParameterValueException("Remove the StaticNat rules for this VM before assigning to another user.");
        }
        List<LoadBalancerVMMapVO> maps = _loadBalancerVMMapDao.listByInstanceId(vm.getId());
        if (maps != null && maps.size() > 0) {
            throw new InvalidParameterValueException("Remove the load balancing rules for this VM before assigning to another user.");
        }
        // check for one on one nat
        List<IPAddressVO> ips = _ipAddressDao.findAllByAssociatedVmId(cmd.getVmId());
        for (IPAddressVO ip : ips) {
            if (ip.isOneToOneNat()) {
                throw new InvalidParameterValueException("Remove the one to one nat rule for this VM for ip " + ip.toString());
            }
        }

        final List<VolumeVO> volumes = _volsDao.findByInstance(cmd.getVmId());

        for (VolumeVO volume : volumes) {
            List<SnapshotVO> snapshots = _snapshotDao.listByStatusNotIn(volume.getId(), Snapshot.State.Destroyed,Snapshot.State.Error);
            if (snapshots != null && snapshots.size() > 0) {
                throw new InvalidParameterValueException(
                        "Snapshots exists for volume: "+ volume.getName()+ ", Detach volume or remove snapshots for volume before assigning VM to another user.");
            }
        }

        DataCenterVO zone = _dcDao.findById(vm.getDataCenterId());

        // Get serviceOffering and Volumes for Virtual Machine
        final ServiceOfferingVO offering = serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

        //Remove vm from instance group
        removeInstanceFromInstanceGroup(cmd.getVmId());

        // VV 2: check if account/domain is with in resource limits to create a new vm
        if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            resourceLimitCheck(newAccount, vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));
        }

        // VV 3: check if volumes and primary storage space are with in resource limits
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.volume, _volsDao.findByInstance(cmd.getVmId()).size());
        Long totalVolumesSize = (long)0;
        for (VolumeVO volume : volumes) {
            totalVolumesSize += volume.getSize();
        }
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.primary_storage, totalVolumesSize);

        // VV 4: Check if new owner can use the vm template
        VirtualMachineTemplate template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());
        if (template == null) {
            throw new InvalidParameterValueException(String.format("Template for VM: %s cannot be found", vm.getUuid()));
        }
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountMgr.getAccount(template.getAccountId());
            _accountMgr.checkAccess(newAccount, null, true, templateOwner);
        }

        // VV 5: check the new account can create vm in the domain
        DomainVO domain = _domainDao.findById(cmd.getDomainId());
        _accountMgr.checkAccess(newAccount, domain);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                //generate destroy vm event for usage
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(),
                        vm.getId(), vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(),
                        vm.getHypervisorType().toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
                // update resource counts for old account
                resourceCountDecrement(oldAccount.getAccountId(), vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));

                // OWNERSHIP STEP 1: update the vm owner
                vm.setAccountId(newAccount.getAccountId());
                vm.setDomainId(cmd.getDomainId());
                _vmDao.persist(vm);

                // OS 2: update volume
                for (VolumeVO volume : volumes) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                            Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
                    _resourceLimitMgr.decrementResourceCount(oldAccount.getAccountId(), ResourceType.volume);
                    _resourceLimitMgr.decrementResourceCount(oldAccount.getAccountId(), ResourceType.primary_storage, new Long(volume.getSize()));
                    volume.setAccountId(newAccount.getAccountId());
                    volume.setDomainId(newAccount.getDomainId());
                    _volsDao.persist(volume);
                    _resourceLimitMgr.incrementResourceCount(newAccount.getAccountId(), ResourceType.volume);
                    _resourceLimitMgr.incrementResourceCount(newAccount.getAccountId(), ResourceType.primary_storage, new Long(volume.getSize()));
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                            volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize(), Volume.class.getName(),
                            volume.getUuid(), volume.isDisplayVolume());
                }

                //update resource count of new account
                if (! VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
                    resourceCountIncrement(newAccount.getAccountId(), vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));
                }

                //generate usage events to account for this change
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                        vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                        VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
            }
        });

        VirtualMachine vmoi = _itMgr.findById(vm.getId());
        VirtualMachineProfileImpl vmOldProfile = new VirtualMachineProfileImpl(vmoi);

        // OS 3: update the network
        List<Long> networkIdList = cmd.getNetworkIds();
        List<Long> securityGroupIdList = cmd.getSecurityGroupIdList();

        if (zone.getNetworkType() == NetworkType.Basic) {
            if (networkIdList != null && !networkIdList.isEmpty()) {
                throw new InvalidParameterValueException("Can't move vm with network Ids; this is a basic zone VM");
            }
            // cleanup the old security groups
            _securityGroupMgr.removeInstanceFromGroups(cmd.getVmId());
            // cleanup the network for the oldOwner
            _networkMgr.cleanupNics(vmOldProfile);
            _networkMgr.removeNics(vmOldProfile);
            // security groups will be recreated for the new account, when the
            // VM is started
            List<NetworkVO> networkList = new ArrayList<NetworkVO>();

            // Get default guest network in Basic zone
            Network defaultNetwork = _networkModel.getExclusiveGuestNetwork(zone.getId());

            if (defaultNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a default network to start a vm");
            } else {
                networkList.add(_networkDao.findById(defaultNetwork.getId()));
            }

            boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware);

            if (securityGroupIdList != null && isVmWare) {
                throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
            } else if (!isVmWare && _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkModel.canAddDefaultSecurityGroup()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }
                SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(newAccount.getId());
                if (defaultGroup != null) {
                    // check if security group id list already contains Default
                    // security group, and if not - add it
                    boolean defaultGroupPresent = false;
                    for (Long securityGroupId : securityGroupIdList) {
                        if (securityGroupId.longValue() == defaultGroup.getId()) {
                            defaultGroupPresent = true;
                            break;
                        }
                    }

                    if (!defaultGroupPresent) {
                        securityGroupIdList.add(defaultGroup.getId());
                    }

                } else {
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account " + newAccount + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            newAccount.getDomainId(), newAccount.getId(), newAccount.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }

            LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();
            NicProfile profile = new NicProfile();
            profile.setDefaultNic(true);
            networks.put(networkList.get(0), new ArrayList<NicProfile>(Arrays.asList(profile)));

            VirtualMachine vmi = _itMgr.findById(vm.getId());
            VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmi);
            _networkMgr.allocate(vmProfile, networks, null);

            _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);

            int securityIdList = securityGroupIdList != null ? securityGroupIdList.size() : 0;
            s_logger.debug("AssignVM: Basic zone, adding security groups no " + securityIdList + " to " + vm.getInstanceName());
        } else {
            Set<NetworkVO> applicableNetworks = new LinkedHashSet<>();
            Map<Long, String> requestedIPv4ForNics = new HashMap<>();
            Map<Long, String> requestedIPv6ForNics = new HashMap<>();
            if (zone.isSecurityGroupEnabled())  { // advanced zone with security groups
                // cleanup the old security groups
                _securityGroupMgr.removeInstanceFromGroups(cmd.getVmId());
                // if networkIdList is null and the first network of vm is shared network, then keep it if possible
                if (networkIdList == null || networkIdList.isEmpty()) {
                    NicVO defaultNicOld = _nicDao.findDefaultNicForVM(vm.getId());
                    if (defaultNicOld != null) {
                        NetworkVO defaultNetworkOld = _networkDao.findById(defaultNicOld.getNetworkId());
                        if (canAccountUseNetwork(newAccount, defaultNetworkOld)) {
                            applicableNetworks.add(defaultNetworkOld);
                            requestedIPv4ForNics.put(defaultNetworkOld.getId(), defaultNicOld.getIPv4Address());
                            requestedIPv6ForNics.put(defaultNetworkOld.getId(), defaultNicOld.getIPv6Address());
                            s_logger.debug("AssignVM: use old shared network " + defaultNetworkOld.getName() + " with old ip " + defaultNicOld.getIPv4Address() + " on default nic of vm:" + vm.getInstanceName());
                        }
                    }
                }

                if (networkIdList != null && !networkIdList.isEmpty()) {
                    // add any additional networks
                    for (Long networkId : networkIdList) {
                        NetworkVO network = _networkDao.findById(networkId);
                        if (network == null) {
                            InvalidParameterValueException ex = new InvalidParameterValueException(
                                    "Unable to find specified network id");
                            ex.addProxyObject(networkId.toString(), "networkId");
                            throw ex;
                        }

                        _networkModel.checkNetworkPermissions(newAccount, network);

                        // don't allow to use system networks
                        NetworkOffering networkOffering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                        if (networkOffering.isSystemOnly()) {
                            InvalidParameterValueException ex = new InvalidParameterValueException(
                                    "Specified Network id is system only and can't be used for vm deployment");
                            ex.addProxyObject(network.getUuid(), "networkId");
                            throw ex;
                        }

                        if (network.getGuestType() == Network.GuestType.Shared && network.getAclType() == ACLType.Domain) {
                            NicVO nicOld = _nicDao.findByNtwkIdAndInstanceId(network.getId(), vm.getId());
                            if (nicOld != null) {
                                requestedIPv4ForNics.put(network.getId(), nicOld.getIPv4Address());
                                requestedIPv6ForNics.put(network.getId(), nicOld.getIPv6Address());
                                s_logger.debug("AssignVM: use old shared network " + network.getName() + " with old ip " + nicOld.getIPv4Address() + " on nic of vm:" + vm.getInstanceName());
                            }
                        }
                        s_logger.debug("AssignVM: Added network " + network.getName() + " to vm " + vm.getId());
                        applicableNetworks.add(network);
                    }
                }

                // cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.removeNics(vmOldProfile);

                // add the new nics
                LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();
                int toggle = 0;
                NetworkVO defaultNetwork = null;
                for (NetworkVO appNet : applicableNetworks) {
                    NicProfile defaultNic = new NicProfile();
                    if (toggle == 0) {
                        defaultNic.setDefaultNic(true);
                        defaultNetwork = appNet;
                        toggle++;
                    }

                    defaultNic.setRequestedIPv4(requestedIPv4ForNics.get(appNet.getId()));
                    defaultNic.setRequestedIPv6(requestedIPv6ForNics.get(appNet.getId()));
                    networks.put(appNet, new ArrayList<NicProfile>(Arrays.asList(defaultNic)));

                }

                boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware);
                if (securityGroupIdList != null && isVmWare) {
                    throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
                } else if (!isVmWare && (defaultNetwork == null || _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork)) && _networkModel.canAddDefaultSecurityGroup()) {
                    if (securityGroupIdList == null) {
                        securityGroupIdList = new ArrayList<Long>();
                    }
                    SecurityGroup defaultGroup = _securityGroupMgr
                            .getDefaultSecurityGroup(newAccount.getId());
                    if (defaultGroup != null) {
                        // check if security group id list already contains Default
                        // security group, and if not - add it
                        boolean defaultGroupPresent = false;
                        for (Long securityGroupId : securityGroupIdList) {
                            if (securityGroupId.longValue() == defaultGroup.getId()) {
                                defaultGroupPresent = true;
                                break;
                            }
                        }

                        if (!defaultGroupPresent) {
                            securityGroupIdList.add(defaultGroup.getId());
                        }

                    } else {
                        // create default security group for the account
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Couldn't find default security group for the account "
                                    + newAccount + " so creating a new one");
                        }
                        defaultGroup = _securityGroupMgr.createSecurityGroup(
                                SecurityGroupManager.DEFAULT_GROUP_NAME,
                                SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                                newAccount.getDomainId(), newAccount.getId(),
                                newAccount.getAccountName());
                        securityGroupIdList.add(defaultGroup.getId());
                    }
                }

                VirtualMachine vmi = _itMgr.findById(vm.getId());
                VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmi);

                if (applicableNetworks.isEmpty()) {
                    throw new InvalidParameterValueException("No network is specified, please specify one when you move the vm. For now, please add a network to VM on NICs tab.");
                } else {
                    _networkMgr.allocate(vmProfile, networks, null);
                }

                _securityGroupMgr.addInstanceToGroups(vm.getId(),
                        securityGroupIdList);
                s_logger.debug("AssignVM: Advanced zone, adding security groups no "
                        + securityGroupIdList.size() + " to "
                        + vm.getInstanceName());

            } else {
                if (securityGroupIdList != null && !securityGroupIdList.isEmpty()) {
                    throw new InvalidParameterValueException("Can't move vm with security groups; security group feature is not enabled in this zone");
                }
                // if networkIdList is null and the first network of vm is shared network, then keep it if possible
                if (networkIdList == null || networkIdList.isEmpty()) {
                    NicVO defaultNicOld = _nicDao.findDefaultNicForVM(vm.getId());
                    if (defaultNicOld != null) {
                        NetworkVO defaultNetworkOld = _networkDao.findById(defaultNicOld.getNetworkId());
                        if (canAccountUseNetwork(newAccount, defaultNetworkOld)) {
                            applicableNetworks.add(defaultNetworkOld);
                            requestedIPv4ForNics.put(defaultNetworkOld.getId(), defaultNicOld.getIPv4Address());
                            requestedIPv6ForNics.put(defaultNetworkOld.getId(), defaultNicOld.getIPv6Address());
                            s_logger.debug("AssignVM: use old shared network " + defaultNetworkOld.getName() + " with old ip " + defaultNicOld.getIPv4Address() + " on default nic of vm:" + vm.getInstanceName());
                        }
                    }
                }

                if (networkIdList != null && !networkIdList.isEmpty()) {
                    // add any additional networks
                    for (Long networkId : networkIdList) {
                        NetworkVO network = _networkDao.findById(networkId);
                        if (network == null) {
                            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find specified network id");
                            ex.addProxyObject(networkId.toString(), "networkId");
                            throw ex;
                        }

                        _networkModel.checkNetworkPermissions(newAccount, network);

                        // don't allow to use system networks
                        NetworkOffering networkOffering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
                        if (networkOffering.isSystemOnly()) {
                            InvalidParameterValueException ex = new InvalidParameterValueException("Specified Network id is system only and can't be used for vm deployment");
                            ex.addProxyObject(network.getUuid(), "networkId");
                            throw ex;
                        }

                        if (network.getGuestType() == Network.GuestType.Shared && network.getAclType() == ACLType.Domain) {
                            NicVO nicOld = _nicDao.findByNtwkIdAndInstanceId(network.getId(), vm.getId());
                            if (nicOld != null) {
                                requestedIPv4ForNics.put(network.getId(), nicOld.getIPv4Address());
                                requestedIPv6ForNics.put(network.getId(), nicOld.getIPv6Address());
                                s_logger.debug("AssignVM: use old shared network " + network.getName() + " with old ip " + nicOld.getIPv4Address() + " on nic of vm:" + vm.getInstanceName());
                            }
                        }
                        s_logger.debug("AssignVM: Added network " + network.getName() + " to vm " + vm.getId());
                        applicableNetworks.add(network);
                    }
                } else if (applicableNetworks.isEmpty()) {
                    NetworkVO defaultNetwork = null;
                    List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
                    if (requiredOfferings.size() < 1) {
                        throw new InvalidParameterValueException("Unable to find network offering with availability=" + Availability.Required
                                + " to automatically create the network as a part of vm creation");
                    }
                    if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                        // get Virtual networks
                        List<? extends Network> virtualNetworks = _networkModel.listNetworksForAccount(newAccount.getId(), zone.getId(), Network.GuestType.Isolated);
                        if (virtualNetworks.isEmpty()) {
                            long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0)
                                    .getTrafficType());
                            // Validate physical network
                            PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
                            if (physicalNetwork == null) {
                                throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                                        + requiredOfferings.get(0).getTags());
                            }
                            s_logger.debug("Creating network for account " + newAccount + " from the network offering id=" + requiredOfferings.get(0).getId()
                                    + " as a part of deployVM process");
                            Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), newAccount.getAccountName() + "-network",
                                    newAccount.getAccountName() + "-network", null, null, null, false, null, newAccount,
                                    null, physicalNetwork, zone.getId(), ACLType.Account, null, null,
                                    null, null, true, null, null, null, null, null, null, null, null, null, null);
                            // if the network offering has persistent set to true, implement the network
                            if (requiredOfferings.get(0).isPersistent()) {
                                DeployDestination dest = new DeployDestination(zone, null, null, null);
                                UserVO callerUser = _userDao.findById(CallContext.current().getCallingUserId());
                                Journal journal = new Journal.LogJournal("Implementing " + newNetwork, s_logger);
                                ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, caller);
                                s_logger.debug("Implementing the network for account" + newNetwork + " as a part of" + " network provision for persistent networks");
                                try {
                                    Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = _networkMgr.implementNetwork(newNetwork.getId(), dest, context);
                                    if (implementedNetwork == null || implementedNetwork.first() == null) {
                                        s_logger.warn("Failed to implement the network " + newNetwork);
                                    }
                                    newNetwork = implementedNetwork.second();
                                } catch (Exception ex) {
                                    s_logger.warn("Failed to implement network " + newNetwork + " elements and"
                                            + " resources as a part of network provision for persistent network due to ", ex);
                                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network"
                                            + " (with specified id) elements and resources as a part of network provision");
                                    e.addProxyObject(newNetwork.getUuid(), "networkId");
                                    throw e;
                                }
                            }
                            defaultNetwork = _networkDao.findById(newNetwork.getId());
                        } else if (virtualNetworks.size() > 1) {
                            throw new InvalidParameterValueException("More than 1 default Isolated networks are found " + "for account " + newAccount
                                    + "; please specify networkIds");
                        } else {
                            defaultNetwork = _networkDao.findById(virtualNetworks.get(0).getId());
                        }
                    } else {
                        throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled);
                    }

                    applicableNetworks.add(defaultNetwork);
                }

                // cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.removeNics(vmOldProfile);

                // add the new nics
                LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();
                int toggle = 0;
                for (NetworkVO appNet : applicableNetworks) {
                    NicProfile defaultNic = new NicProfile();
                    if (toggle == 0) {
                        defaultNic.setDefaultNic(true);
                        toggle++;
                    }
                    defaultNic.setRequestedIPv4(requestedIPv4ForNics.get(appNet.getId()));
                    defaultNic.setRequestedIPv6(requestedIPv6ForNics.get(appNet.getId()));
                    networks.put(appNet, new ArrayList<NicProfile>(Arrays.asList(defaultNic)));
                }
                VirtualMachine vmi = _itMgr.findById(vm.getId());
                VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmi);
                _networkMgr.allocate(vmProfile, networks, null);
                s_logger.debug("AssignVM: Advance virtual, adding networks no " + networks.size() + " to " + vm.getInstanceName());
            } // END IF NON SEC GRP ENABLED
        } // END IF ADVANCED
        s_logger.info("AssignVM: vm " + vm.getInstanceName() + " now belongs to account " + newAccount.getAccountName());
        return vm;
    }

    private boolean canAccountUseNetwork(Account newAccount, Network network) {
        if (network != null && network.getAclType() == ACLType.Domain
                && (network.getGuestType() == Network.GuestType.Shared
                || network.getGuestType() == Network.GuestType.L2)) {
            try {
                _networkModel.checkNetworkPermissions(newAccount, network);
                return true;
            } catch (PermissionDeniedException e) {
                s_logger.debug(String.format("AssignVM: %s network %s can not be used by new account %s", network.getGuestType(), network.getName(), newAccount.getAccountName()));
                return false;
            }
        }
        return false;
    }

    @Override
    public UserVm restoreVM(RestoreVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException {
        // Input validation
        Account caller = CallContext.current().getCallingAccount();

        long vmId = cmd.getVmId();
        Long newTemplateId = cmd.getTemplateId();

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Cannot find VM with ID " + vmId);
            ex.addProxyObject(String.valueOf(vmId), "vmId");
            throw ex;
        }

        _accountMgr.checkAccess(caller, null, true, vm);

        //check if there are any active snapshots on volumes associated with the VM
        s_logger.debug("Checking if there are any ongoing snapshots on the ROOT volumes associated with VM with ID " + vmId);
        if (checkStatusOfVolumeSnapshots(vmId, Volume.Type.ROOT)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on ROOT volume, Re-install VM is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volume of type ROOT, for the vm with id " + vmId);
        return restoreVMInternal(caller, vm, newTemplateId);
    }

    public UserVm restoreVMInternal(Account caller, UserVmVO vm, Long newTemplateId) throws InsufficientCapacityException, ResourceUnavailableException {
        return _itMgr.restoreVirtualMachine(vm.getId(), newTemplateId);
    }

    private VMTemplateVO getRestoreVirtualMachineTemplate(Account caller, Long newTemplateId, List<VolumeVO> rootVols, UserVmVO vm) {
        VMTemplateVO template = null;
        if (CollectionUtils.isNotEmpty(rootVols)) {
            VolumeVO root = rootVols.get(0);
            Long templateId = root.getTemplateId();
            boolean isISO = false;
            if (templateId == null) {
                // Assuming that for a vm deployed using ISO, template ID is set to NULL
                isISO = true;
                templateId = vm.getIsoId();
            }
            //newTemplateId can be either template or ISO id. In the following snippet based on the vm deployment (from template or ISO) it is handled accordingly
            if (newTemplateId != null) {
                template = _templateDao.findById(newTemplateId);
                _accountMgr.checkAccess(caller, null, true, template);
                if (isISO) {
                    if (!template.getFormat().equals(ImageFormat.ISO)) {
                        throw new InvalidParameterValueException("VM has been created using an ISO therefore it can not be re-installed with a template");
                    }
                } else {
                    if (template.getFormat().equals(ImageFormat.ISO)) {
                        throw new InvalidParameterValueException("Invalid template id provided to restore the VM ");
                    }
                }
            } else {
                if (isISO && templateId == null) {
                    throw new CloudRuntimeException("Cannot restore the VM since there is no ISO attached to VM");
                }
                template = _templateDao.findById(templateId);
                if (template == null) {
                    InvalidParameterValueException ex = new InvalidParameterValueException("Cannot find template/ISO for specified volumeid and vmId");
                    ex.addProxyObject(vm.getUuid(), "vmId");
                    ex.addProxyObject(root.getUuid(), "volumeId");
                    throw ex;
                }
            }
        }

        return template;
    }

    @Override
    public UserVm restoreVirtualMachine(final Account caller, final long vmId, final Long newTemplateId) throws InsufficientCapacityException, ResourceUnavailableException {
        Long userId = caller.getId();
        _userDao.findById(userId);
        UserVmVO vm = _vmDao.findById(vmId);
        Account owner = _accountDao.findById(vm.getAccountId());
        boolean needRestart = false;

        // Input validation
        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.DISABLED) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
        }

        if (vm.getState() != VirtualMachine.State.Running && vm.getState() != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException("Vm " + vm.getUuid() + " currently in " + vm.getState() + " state, restore vm can only execute when VM in Running or Stopped");
        }

        if (vm.getState() == VirtualMachine.State.Running) {
            needRestart = true;
        }

        VMTemplateVO currentTemplate = _templateDao.findById(vm.getTemplateId());
        List<VolumeVO> rootVols = _volsDao.findByInstanceAndType(vmId, Volume.Type.ROOT);
        if (rootVols.isEmpty()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Can not find root volume for VM " + vm.getUuid());
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }
        if (rootVols.size() > 1 && currentTemplate != null && !currentTemplate.isDeployAsIs()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("There are " + rootVols.size() + " root volumes for VM " + vm.getUuid());
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        // If target VM has associated VM snapshots then don't allow restore of VM
        List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (vmSnapshots.size() > 0) {
            throw new InvalidParameterValueException("Unable to restore VM, please remove VM snapshots before restoring VM");
        }

        VMTemplateVO template = getRestoreVirtualMachineTemplate(caller, newTemplateId, rootVols, vm);
        checkRestoreVmFromTemplate(vm, template);

        if (needRestart) {
            try {
                _itMgr.stop(vm.getUuid());
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Stop vm " + vm.getUuid() + " failed", e);
                CloudRuntimeException ex = new CloudRuntimeException("Stop vm failed for specified vmId");
                ex.addProxyObject(vm.getUuid(), "vmId");
                throw ex;
            }
        }

        List<Volume> newVols = new ArrayList<>();
        for (VolumeVO root : rootVols) {
            if ( !Volume.State.Allocated.equals(root.getState()) || newTemplateId != null ){
                Long templateId = root.getTemplateId();
                boolean isISO = false;
                if (templateId == null) {
                    // Assuming that for a vm deployed using ISO, template ID is set to NULL
                    isISO = true;
                    templateId = vm.getIsoId();
                }

                /* If new template/ISO is provided allocate a new volume from new template/ISO otherwise allocate new volume from original template/ISO */
                Volume newVol = null;
                if (newTemplateId != null) {
                    if (isISO) {
                        newVol = volumeMgr.allocateDuplicateVolume(root, null);
                        vm.setIsoId(newTemplateId);
                        vm.setGuestOSId(template.getGuestOSId());
                        vm.setTemplateId(newTemplateId);
                    } else {
                        newVol = volumeMgr.allocateDuplicateVolume(root, newTemplateId);
                        vm.setGuestOSId(template.getGuestOSId());
                        vm.setTemplateId(newTemplateId);
                    }
                    // check and update VM if it can be dynamically scalable with the new template
                    updateVMDynamicallyScalabilityUsingTemplate(vm, newTemplateId);
                } else {
                    newVol = volumeMgr.allocateDuplicateVolume(root, null);
                }
                newVols.add(newVol);

                if (userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.ROOT_DISK_SIZE) == null && !newVol.getSize().equals(template.getSize())) {
                    VolumeVO resizedVolume = (VolumeVO) newVol;
                    if (template.getSize() != null) {
                        resizedVolume.setSize(template.getSize());
                        _volsDao.update(resizedVolume.getId(), resizedVolume);
                    }
                }

                // 1. Save usage event and update resource count for user vm volumes
                _resourceLimitMgr.incrementResourceCount(newVol.getAccountId(), ResourceType.volume, newVol.isDisplay());
                _resourceLimitMgr.incrementResourceCount(newVol.getAccountId(), ResourceType.primary_storage, newVol.isDisplay(), new Long(newVol.getSize()));
                // 2. Create Usage event for the newly created volume
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, newVol.getAccountId(), newVol.getDataCenterId(), newVol.getId(), newVol.getName(), newVol.getDiskOfferingId(), template.getId(), newVol.getSize());
                _usageEventDao.persist(usageEvent);

                handleManagedStorage(vm, root);

                _volsDao.attachVolume(newVol.getId(), vmId, newVol.getDeviceId());

                // Detach, destroy and create the usage event for the old root volume.
                _volsDao.detachVolume(root.getId());
                volumeMgr.destroyVolume(root);

                // For VMware hypervisor since the old root volume is replaced by the new root volume, force expunge old root volume if it has been created in storage
                if (vm.getHypervisorType() == HypervisorType.VMware) {
                    VolumeInfo volumeInStorage = volFactory.getVolume(root.getId());
                    if (volumeInStorage != null) {
                        s_logger.info("Expunging volume " + root.getId() + " from primary data store");
                        AsyncCallFuture<VolumeApiResult> future = _volService.expungeVolumeAsync(volFactory.getVolume(root.getId()));
                        try {
                            future.get();
                        } catch (Exception e) {
                            s_logger.debug("Failed to expunge volume:" + root.getId(), e);
                        }
                    }
                }
            }
        }

        Map<VirtualMachineProfile.Param, Object> params = null;
        String password = null;

        if (template.isEnablePassword()) {
            password = _mgr.generateRandomPassword();
            boolean result = resetVMPasswordInternal(vmId, password);
            if (!result) {
                throw new CloudRuntimeException("VM reset is completed but failed to reset password for the virtual machine ");
            }
            vm.setPassword(password);
        }
        if (needRestart) {
            try {
                if (vm.getDetail(VmDetailConstants.PASSWORD) != null) {
                    params = new HashMap<>();
                    params.put(VirtualMachineProfile.Param.VmPassword, password);
                }
                _itMgr.start(vm.getUuid(), params);
                vm = _vmDao.findById(vmId);
                if (template.isEnablePassword()) {
                    // this value is not being sent to the backend; need only for api
                    // display purposes
                    vm.setPassword(password);
                    if (vm.isUpdateParameters()) {
                        vm.setUpdateParameters(false);
                        _vmDao.loadDetails(vm);
                        if (vm.getDetail(VmDetailConstants.PASSWORD) != null) {
                            userVmDetailsDao.removeDetail(vm.getId(), VmDetailConstants.PASSWORD);
                        }
                        _vmDao.update(vm.getId(), vm);
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Unable to start VM " + vm.getUuid(), e);
                CloudRuntimeException ex = new CloudRuntimeException("Unable to start VM with specified id" + e.getMessage());
                ex.addProxyObject(vm.getUuid(), "vmId");
                throw ex;
            }
        }

        s_logger.debug("Restore VM " + vmId + " done successfully");
        return vm;

    }

    private void updateVMDynamicallyScalabilityUsingTemplate(UserVmVO vm, Long newTemplateId) {
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(vm.getServiceOfferingId());
        VMTemplateVO newTemplate = _templateDao.findById(newTemplateId);
        boolean dynamicScalingEnabled = checkIfDynamicScalingCanBeEnabled(vm, serviceOffering, newTemplate, vm.getDataCenterId());
        vm.setDynamicallyScalable(dynamicScalingEnabled);
        _vmDao.update(vm.getId(), vm);
    }

    /**
     * Perform basic checkings to make sure restore is possible. If not, #InvalidParameterValueException is thrown.
     *
     * @param vm vm
     * @param template template
     * @throws InvalidParameterValueException if restore is not possible
     */
    private void checkRestoreVmFromTemplate(UserVmVO vm, VMTemplateVO template) {
        TemplateDataStoreVO tmplStore;
        if (!template.isDirectDownload()) {
            tmplStore = _templateStoreDao.findByTemplateZoneReady(template.getId(), vm.getDataCenterId());
            if (tmplStore == null) {
                throw new InvalidParameterValueException("Cannot restore the vm as the template " + template.getUuid() + " isn't available in the zone");
            }
        } else {
            tmplStore = _templateStoreDao.findByTemplate(template.getId(), DataStoreRole.Image);
            if (tmplStore == null || (tmplStore != null && !tmplStore.getDownloadState().equals(VMTemplateStorageResourceAssoc.Status.BYPASSED))) {
                throw new InvalidParameterValueException("Cannot restore the vm as the bypassed template " + template.getUuid() + " isn't available in the zone");
            }
        }
    }

    private void handleManagedStorage(UserVmVO vm, VolumeVO root) {
        if (Volume.State.Allocated.equals(root.getState())) {
            return;
        }

        StoragePoolVO storagePool = _storagePoolDao.findById(root.getPoolId());

        if (storagePool != null && storagePool.isManaged()) {
            Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();

            if (hostId != null) {
                VolumeInfo volumeInfo = volFactory.getVolume(root.getId());
                Host host = _hostDao.findById(hostId);

                final Command cmd;

                if (host.getHypervisorType() == HypervisorType.XenServer) {
                    DiskTO disk = new DiskTO(volumeInfo.getTO(), root.getDeviceId(), root.getPath(), root.getVolumeType());

                    // it's OK in this case to send a detach command to the host for a root volume as this
                    // will simply lead to the SR that supports the root volume being removed
                    cmd = new DettachCommand(disk, vm.getInstanceName());

                    DettachCommand detachCommand = (DettachCommand)cmd;

                    detachCommand.setManaged(true);

                    detachCommand.setStorageHost(storagePool.getHostAddress());
                    detachCommand.setStoragePort(storagePool.getPort());

                    detachCommand.set_iScsiName(root.get_iScsiName());
                }
                else if (host.getHypervisorType() == HypervisorType.VMware) {
                    PrimaryDataStore primaryDataStore = (PrimaryDataStore)volumeInfo.getDataStore();
                    Map<String, String> details = primaryDataStore.getDetails();

                    if (details == null) {
                        details = new HashMap<>();

                        primaryDataStore.setDetails(details);
                    }

                    details.put(DiskTO.MANAGED, Boolean.TRUE.toString());

                    cmd = new DeleteCommand(volumeInfo.getTO());
                }
                else if (host.getHypervisorType() == HypervisorType.KVM) {
                    cmd = null;
                }
                else {
                    throw new CloudRuntimeException("This hypervisor type is not supported on managed storage for this command.");
                }

                if (cmd != null) {
                    Commands cmds = new Commands(Command.OnError.Stop);

                    cmds.addCommand(cmd);

                    try {
                        _agentMgr.send(hostId, cmds);
                    } catch (Exception ex) {
                        throw new CloudRuntimeException(ex.getMessage());
                    }

                    if (!cmds.isSuccessful()) {
                        for (Answer answer : cmds.getAnswers()) {
                            if (!answer.getResult()) {
                                s_logger.warn("Failed to reset vm due to: " + answer.getDetails());

                                throw new CloudRuntimeException("Unable to reset " + vm + " due to " + answer.getDetails());
                            }
                        }
                    }
                }

                // root.getPoolId() should be null if the VM we are detaching the disk from has never been started before
                DataStore dataStore = root.getPoolId() != null ? _dataStoreMgr.getDataStore(root.getPoolId(), DataStoreRole.Primary) : null;

                volumeMgr.revokeAccess(volFactory.getVolume(root.getId()), host, dataStore);

                if (dataStore != null) {
                    handleTargetsForVMware(host.getId(), storagePool.getHostAddress(), storagePool.getPort(), root.get_iScsiName());
                }
            }
        }
    }

    private void handleTargetsForVMware(long hostId, String storageAddress, int storagePort, String iScsiName) {
        HostVO host = _hostDao.findById(hostId);

        if (host.getHypervisorType() == HypervisorType.VMware) {
            ModifyTargetsCommand cmd = new ModifyTargetsCommand();

            List<Map<String, String>> targets = new ArrayList<>();

            Map<String, String> target = new HashMap<>();

            target.put(ModifyTargetsCommand.STORAGE_HOST, storageAddress);
            target.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePort));
            target.put(ModifyTargetsCommand.IQN, iScsiName);

            targets.add(target);

            cmd.setTargets(targets);
            cmd.setApplyToAllHostsInCluster(true);
            cmd.setAdd(false);
            cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);

            sendModifyTargetsCommand(cmd, hostId);
        }
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            String msg = "Unable to get an answer to the modify targets command";

            s_logger.warn(msg);
        }
        else if (!answer.getResult()) {
            String msg = "Unable to modify target on the following host: " + hostId;

            s_logger.warn(msg);
        }
    }

    @Override
    public void prepareStop(VirtualMachineProfile profile) {
        collectVmDiskAndNetworkStatistics(profile.getId(), State.Stopping);
    }

    @Override
    public void finalizeUnmanage(VirtualMachine vm) {
    }

    private void encryptAndStorePassword(UserVmVO vm, String password) {
        String sshPublicKeys = vm.getDetail(VmDetailConstants.SSH_PUBLIC_KEY);
        if (sshPublicKeys != null && !sshPublicKeys.equals("") && password != null && !password.equals("saved_password")) {
            if (!sshPublicKeys.startsWith("ssh-rsa")) {
                s_logger.warn("Only RSA public keys can be used to encrypt a vm password.");
                return;
            }
            String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(sshPublicKeys, password);
            if (encryptedPasswd == null) {
                throw new CloudRuntimeException("Error encrypting password");
            }

            vm.setDetail(VmDetailConstants.ENCRYPTED_PASSWORD, encryptedPasswd);
            _vmDao.saveDetails(vm);
        }
    }

    @Override
    public void persistDeviceBusInfo(UserVmVO vm, String rootDiskController) {
        String existingVmRootDiskController = vm.getDetail(VmDetailConstants.ROOT_DISK_CONTROLLER);
        if (StringUtils.isEmpty(existingVmRootDiskController) && StringUtils.isNotEmpty(rootDiskController)) {
            vm.setDetail(VmDetailConstants.ROOT_DISK_CONTROLLER, rootDiskController);
            _vmDao.saveDetails(vm);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Persisted device bus information rootDiskController=" + rootDiskController + " for vm: " + vm.getDisplayName());
            }
        }
    }

    @Override
    public String getConfigComponentName() {
        return UserVmManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {EnableDynamicallyScaleVm, AllowDiskOfferingChangeDuringScaleVm, AllowUserExpungeRecoverVm, VmIpFetchWaitInterval, VmIpFetchTrialMax,
                VmIpFetchThreadPoolMax, VmIpFetchTaskWorkers, AllowDeployVmIfGivenHostFails, EnableAdditionalVmConfig, DisplayVMOVFProperties,
                KvmAdditionalConfigAllowList, XenServerAdditionalConfigAllowList, VmwareAdditionalConfigAllowList, DestroyRootVolumeOnVmDestruction};
    }

    @Override
    public String getVmUserData(long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, vm);
        return vm.getUserData();
    }

    @Override
    public boolean isDisplayResourceEnabled(Long vmId) {
        UserVm vm = _vmDao.findById(vmId);
        if (vm != null) {
            return vm.isDisplayVm();
        }

        return true;
    }

    private boolean checkStatusOfVolumeSnapshots(long vmId, Volume.Type type) {
        List<VolumeVO> listVolumes = null;
        if (type == Volume.Type.ROOT) {
            listVolumes = _volsDao.findByInstanceAndType(vmId, type);
        } else if (type == Volume.Type.DATADISK) {
            listVolumes = _volsDao.findByInstanceAndType(vmId, type);
        } else {
            listVolumes = _volsDao.findByInstance(vmId);
        }
        s_logger.debug("Found "+listVolumes.size()+" no. of volumes of type "+type+" for vm with VM ID "+vmId);
        for (VolumeVO volume : listVolumes) {
            Long volumeId = volume.getId();
            s_logger.debug("Checking status of snapshots for Volume with Volume Id: "+volumeId);
            List<SnapshotVO> ongoingSnapshots = _snapshotDao.listByStatus(volumeId, Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
            int ongoingSnapshotsCount = ongoingSnapshots.size();
            s_logger.debug("The count of ongoing Snapshots for VM with ID "+vmId+" and disk type "+type+" is "+ongoingSnapshotsCount);
            if (ongoingSnapshotsCount > 0) {
                s_logger.debug("Found "+ongoingSnapshotsCount+" no. of snapshots, on volume of type "+type+", which snapshots are not yet backed up");
                return true;
            }
        }
        return false;
    }

    private void checkForUnattachedVolumes(long vmId, List<VolumeVO> volumes) {

        StringBuilder sb = new StringBuilder();

        for (VolumeVO volume : volumes) {
            if (volume.getInstanceId() == null || vmId != volume.getInstanceId() || volume.getVolumeType() != Volume.Type.DATADISK) {
                sb.append(volume.toString() + "; ");
            }
        }

        if (!StringUtils.isEmpty(sb.toString())) {
            throw new InvalidParameterValueException("The following supplied volumes are not DATADISK attached to the VM: " + sb.toString());
        }
    }

    private void validateVolumes(List<VolumeVO> volumes) {

        for (VolumeVO volume : volumes) {
            if (!(volume.getVolumeType() == Volume.Type.ROOT || volume.getVolumeType() == Volume.Type.DATADISK)) {
                throw new InvalidParameterValueException("Please specify volume of type " + Volume.Type.DATADISK.toString() + " or " + Volume.Type.ROOT.toString());
            }
        }
    }

    private void detachVolumesFromVm(List<VolumeVO> volumes) {

        for (VolumeVO volume : volumes) {
            // Create new context and inject correct event resource type, id and details,
            // otherwise VOLUME.DETACH event will be associated with VirtualMachine and contain VM id and other information.
            CallContext volumeContext = CallContext.register(CallContext.current(), ApiCommandResourceType.Volume);
            volumeContext.setEventDetails("Volume Id: " + this._uuidMgr.getUuid(Volume.class, volume.getId()) + " Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, volume.getInstanceId()));
            volumeContext.setEventResourceType(ApiCommandResourceType.Volume);
            volumeContext.setEventResourceId(volume.getId());

            Volume detachResult = null;
            try {
                detachResult = _volumeService.detachVolumeViaDestroyVM(volume.getInstanceId(), volume.getId());
            } finally {
                // Remove volumeContext and pop vmContext back
                CallContext.unregister();
            }

            if (detachResult == null) {
                s_logger.error("DestroyVM remove volume - failed to detach and delete volume " + volume.getInstanceId() + " from instance " + volume.getId());
            }
        }
    }

    private void deleteVolumesFromVm(List<VolumeVO> volumes, boolean expunge) {

        for (VolumeVO volume : volumes) {

            Volume result = _volumeService.destroyVolume(volume.getId(), CallContext.current().getCallingAccount(), expunge, false);

            if (result == null) {
                s_logger.error("DestroyVM remove volume - failed to delete volume " + volume.getInstanceId() + " from instance " + volume.getId());
            }
        }
    }

    @Override
    public UserVm importVM(final DataCenter zone, final Host host, final VirtualMachineTemplate template, final String instanceName, final String displayName,
                           final Account owner, final String userData, final Account caller, final Boolean isDisplayVm, final String keyboard,
                           final long accountId, final long userId, final ServiceOffering serviceOffering, final String sshPublicKeys,
                           final String hostName, final HypervisorType hypervisorType, final Map<String, String> customParameters, final VirtualMachine.PowerState powerState) throws InsufficientCapacityException {
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to import virtual machine with invalid zone");
        }
        if (host == null) {
            throw new InvalidParameterValueException("Unable to import virtual machine with invalid host");
        }

        final long id = _vmDao.getNextInSequence(Long.class, "id");

        if (hostName != null) {
            // Check is hostName is RFC compliant
            checkNameForRFCCompliance(hostName);
        }

        final String uuidName = _uuidMgr.generateUuid(UserVm.class, null);
        final Host lastHost = powerState != VirtualMachine.PowerState.PowerOn ? host : null;
        final Boolean dynamicScalingEnabled = checkIfDynamicScalingCanBeEnabled(null, serviceOffering, template, zone.getId());
        return commitUserVm(true, zone, host, lastHost, template, hostName, displayName, owner,
                null, null, userData, null, null, isDisplayVm, keyboard,
                accountId, userId, serviceOffering, template.getFormat().equals(ImageFormat.ISO), sshPublicKeys, null,
                id, instanceName, uuidName, hypervisorType, customParameters,
                null, null, null, powerState, dynamicScalingEnabled, null, serviceOffering.getDiskOfferingId(), null);
    }

    @Override
    public boolean unmanageUserVM(Long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find a VM with ID = " + vmId);
        }

        vm = _vmDao.acquireInLockTable(vm.getId());
        boolean result;
        try {
            if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
                s_logger.debug("VM ID = " + vmId + " is not running or stopped, cannot be unmanaged");
                return false;
            }

            if (vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
                throw new UnsupportedServiceException("Unmanaging a VM is currently allowed for VMware VMs only");
            }

            List<VolumeVO> volumes = _volsDao.findByInstance(vm.getId());
            checkUnmanagingVMOngoingVolumeSnapshots(vm);
            checkUnmanagingVMVolumes(vm, volumes);

            result = _itMgr.unmanage(vm.getUuid());
            if (result) {
                cleanupUnmanageVMResources(vm.getId());
                unmanageVMFromDB(vm.getId());
                publishUnmanageVMUsageEvents(vm, volumes);
            } else {
                throw new CloudRuntimeException("Error while unmanaging VM: " + vm.getUuid());
            }
        } catch (Exception e) {
            s_logger.error("Could not unmanage VM " + vm.getUuid(), e);
            throw new CloudRuntimeException(e);
        } finally {
            _vmDao.releaseFromLockTable(vm.getId());
        }

        return true;
    }

    /*
        Generate usage events related to unmanaging a VM
     */
    private void publishUnmanageVMUsageEvents(UserVmVO vm, List<VolumeVO> volumes) {
        postProcessingUnmanageVMVolumes(volumes, vm);
        postProcessingUnmanageVM(vm);
    }

    /*
        Cleanup the VM from resources and groups
     */
    private void cleanupUnmanageVMResources(long vmId) {
        cleanupVmResources(vmId);
        removeVMFromAffinityGroups(vmId);
    }

    private void unmanageVMFromDB(long vmId) {
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        userVmDetailsDao.removeDetails(vmId);
        vm.setState(State.Expunging);
        vm.setRemoved(new Date());
        _vmInstanceDao.update(vm.getId(), vm);
    }

    /*
        Remove VM from affinity groups after unmanaging
     */
    private void removeVMFromAffinityGroups(long vmId) {
        List<AffinityGroupVMMapVO> affinityGroups = _affinityGroupVMMapDao.listByInstanceId(vmId);
        if (affinityGroups.size() > 0) {
            s_logger.debug("Cleaning up VM from affinity groups after unmanaging");
            for (AffinityGroupVMMapVO map : affinityGroups) {
                _affinityGroupVMMapDao.expunge(map.getId());
            }
        }
    }

    /*
        Decrement VM resources and generate usage events after unmanaging VM
     */
    private void postProcessingUnmanageVM(UserVmVO vm) {
        ServiceOfferingVO offering = serviceOfferingDao.findById(vm.getServiceOfferingId());
        Long cpu = offering.getCpu() != null ? new Long(offering.getCpu()) : 0L;
        Long ram = offering.getRamSize() != null ? new Long(offering.getRamSize()) : 0L;
        // First generate a VM stop event if the VM was not stopped already
        if (vm.getState() != State.Stopped) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_STOP, vm.getAccountId(), vm.getDataCenterId(),
                    vm.getId(), vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(),
                    vm.getHypervisorType().toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
            resourceCountDecrement(vm.getAccountId(), vm.isDisplayVm(), cpu, ram);
        }

        // VM destroy usage event
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(),
                vm.getId(), vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(),
                vm.getHypervisorType().toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
        resourceCountDecrement(vm.getAccountId(), vm.isDisplayVm(), cpu, ram);
    }

    /*
        Decrement resources for volumes and generate usage event for ROOT volume after unmanaging VM.
        Usage events for DATA disks are published by the transition listener: @see VolumeStateListener#postStateTransitionEvent
     */
    private void postProcessingUnmanageVMVolumes(List<VolumeVO> volumes, UserVmVO vm) {
        for (VolumeVO volume : volumes) {
            if (volume.getVolumeType() == Volume.Type.ROOT) {
                //
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                        Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
            }
            _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.volume);
            _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, new Long(volume.getSize()));
        }
    }

    private void checkUnmanagingVMOngoingVolumeSnapshots(UserVmVO vm) {
        s_logger.debug("Checking if there are any ongoing snapshots on the ROOT volumes associated with VM with ID " + vm.getId());
        if (checkStatusOfVolumeSnapshots(vm.getId(), Volume.Type.ROOT)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on ROOT volume, vm unmanage is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volume of type ROOT, for the vm with id " + vm.getId());
    }

    private void checkUnmanagingVMVolumes(UserVmVO vm, List<VolumeVO> volumes) {
        for (VolumeVO volume : volumes) {
            if (volume.getInstanceId() == null || !volume.getInstanceId().equals(vm.getId())) {
                throw new CloudRuntimeException("Invalid state for volume with ID " + volume.getId() + " of VM " +
                        vm.getId() +": it is not attached to VM");
            } else if (volume.getVolumeType() != Volume.Type.ROOT && volume.getVolumeType() != Volume.Type.DATADISK) {
                throw new CloudRuntimeException("Invalid type for volume with ID " + volume.getId() +
                        ": ROOT or DATADISK expected but got " + volume.getVolumeType());
            }
        }
    }

    private LinkedHashMap<Integer, Long> getVmOvfNetworkMapping(DataCenter zone, Account owner, VirtualMachineTemplate template, Map<Integer, Long> vmNetworkMapping) throws InsufficientCapacityException, ResourceAllocationException {
        LinkedHashMap<Integer, Long> mapping = new LinkedHashMap<>();
        if (ImageFormat.OVA.equals(template.getFormat())) {
            List<OVFNetworkTO> OVFNetworkTOList =
                    templateDeployAsIsDetailsDao.listNetworkRequirementsByTemplateId(template.getId());
            if (CollectionUtils.isNotEmpty(OVFNetworkTOList)) {
                Network lastMappedNetwork = null;
                for (OVFNetworkTO OVFNetworkTO : OVFNetworkTOList) {
                    Long networkId = vmNetworkMapping.get(OVFNetworkTO.getInstanceID());
                    if (networkId == null && lastMappedNetwork == null) {
                        lastMappedNetwork = getNetworkForOvfNetworkMapping(zone, owner);
                    }
                    if (networkId == null) {
                        networkId = lastMappedNetwork.getId();
                    }
                    mapping.put(OVFNetworkTO.getInstanceID(), networkId);
                }
            }
        }
        return mapping;
    }

    private Network getNetworkForOvfNetworkMapping(DataCenter zone, Account owner) throws InsufficientCapacityException, ResourceAllocationException {
        Network network = null;
        if (zone.isSecurityGroupEnabled()) {
            network = _networkModel.getNetworkWithSGWithFreeIPs(zone.getId());
            if (network == null) {
                throw new InvalidParameterValueException("No network with security enabled is found in zone ID: " + zone.getUuid());
            }
        } else {
            network = getDefaultNetwork(zone, owner, true);
            if (network == null) {
                throw new InvalidParameterValueException(String.format("Default network not found for zone ID: %s and account ID: %s", zone.getUuid(), owner.getUuid()));
            }
        }
        return network;
    }

    private void collectVmDiskAndNetworkStatistics(Long vmId, State expectedState) {
        UserVmVO uservm = _vmDao.findById(vmId);
        if (uservm != null) {
            collectVmDiskAndNetworkStatistics(uservm, expectedState);
        } else {
            s_logger.info(String.format("Skip collecting vm %s disk and network statistics as it is not user vm", uservm));
        }
    }

    private void collectVmDiskAndNetworkStatistics(UserVm vm, State expectedState) {
        if (expectedState == null || expectedState == vm.getState()) {
            collectVmDiskStatistics(vm);
            collectVmNetworkStatistics(vm);
        } else {
            s_logger.warn(String.format("Skip collecting vm %s disk and network statistics as the expected vm state is %s but actual state is %s", vm, expectedState, vm.getState()));
        }
    }

    public Boolean getDestroyRootVolumeOnVmDestruction(Long domainId){
        return DestroyRootVolumeOnVmDestruction.valueIn(domainId);
    }
}
