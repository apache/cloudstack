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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
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
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetVmDiskStatsAnswer;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.agent.api.GetVmNetworkStatsAnswer;
import com.cloud.agent.api.GetVmNetworkStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.RestoreVMSnapshotAnswer;
import com.cloud.agent.api.RestoreVMSnapshotCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmNetworkStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
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
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.gpu.GPU;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
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
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
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
import com.cloud.server.ManagementService;
import com.cloud.server.ResourceTag;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.TemplateOVFPropertyVO;
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
import com.cloud.storage.dao.TemplateOVFPropertiesDao;
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
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
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
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class UserVmManagerImpl extends ManagerBase implements UserVmManager, VirtualMachineGuru, UserVmService, Configurable {
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
    private ServiceOfferingDao _offeringDao;
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
    private ServiceOfferingDao _serviceOfferingDao;
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
    private TemplateOVFPropertiesDao templateOVFPropertiesDao;

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

    private static final int MAX_HTTP_GET_LENGTH = 2 * MAX_USER_DATA_LENGTH_BYTES;
    private static final int MAX_HTTP_POST_LENGTH = 16 * MAX_USER_DATA_LENGTH_BYTES;

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

    private static final ConfigKey<Boolean> EnableAdditionalVmConfig = new ConfigKey<>("Advanced", Boolean.class, "enable.additional.vm.configuration",
            "false", "allow additional arbitrary configuration to vm", true, ConfigKey.Scope.Account);
    private static final ConfigKey<Boolean> VmDestroyForcestop = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.destroy.forcestop", "false",
            "On destroy, force-stop takes this value ", true);

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

    private void resourceCountIncrement(long accountId, Boolean displayVm, Long cpu, Long memory) {
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.user_vm, displayVm);
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.cpu, displayVm, cpu);
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.memory, displayVm, memory);
    }

    private void resourceCountDecrement(long accountId, Boolean displayVm, Long cpu, Long memory) {
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.user_vm, displayVm);
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.cpu, displayVm, cpu);
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.memory, displayVm, memory);
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
                                    "VM " + vmId + " nic id " + nicId + " ip address " + vmIp + " got fetched successfully");
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

                if (rebootVirtualMachine(userId, vmId) == null) {
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
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETSSHKEY, eventDescription = "resetting Vm SSHKey", async = true)
    public UserVm resetVMSSHKey(ResetVMSSHKeyCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        Long vmId = cmd.getId();

        UserVmVO userVm = _vmDao.findById(cmd.getId());
        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine by id" + cmd.getId());
        }

        _vmDao.loadDetails(userVm);
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

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s == null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' does not exist for account " + owner.getAccountName()
            + " in specified domain id");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);
        String password = null;
        String sshPublicKey = s.getPublicKey();
        if (template != null && template.isEnablePassword()) {
            password = _mgr.generateRandomPassword();
        }

        boolean result = resetVMSSHKeyInternal(vmId, sshPublicKey, password);

        if (!result) {
            throw new CloudRuntimeException("Failed to reset SSH Key for the virtual machine ");
        }
        return userVm;
    }

    private boolean resetVMSSHKeyInternal(Long vmId, String sshPublicKey, String password) throws ResourceUnavailableException, InsufficientCapacityException {
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

        if (template.isEnablePassword()) {
            vmProfile.setParameter(VirtualMachineProfile.Param.VmPassword, password);
        }

        UserDataServiceProvider element = _networkMgr.getSSHKeyResetProvider(defaultNetwork);
        if (element == null) {
            throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for SSH Key reset");
        }
        boolean result = element.saveSSHKey(defaultNetwork, defaultNicProfile, vmProfile, sshPublicKey);

        // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
        if (!result) {
            s_logger.debug("Failed to reset SSH Key for the virtual machine; no need to reboot the vm");
            return false;
        } else {
            final UserVmVO userVm = _vmDao.findById(vmId);
            _vmDao.loadDetails(userVm);
            userVm.setDetail(VmDetailConstants.SSH_PUBLIC_KEY, sshPublicKey);
            if (template.isEnablePassword()) {
                userVm.setPassword(password);
                //update the encrypted password in vm_details table too
                encryptAndStorePassword(userVm, password);
            }
            _vmDao.saveDetails(userVm);

            if (vmInstance.getState() == State.Stopped) {
                s_logger.debug("Vm " + vmInstance + " is stopped, not rebooting it as a part of SSH Key reset");
                return true;
            }
            if (rebootVirtualMachine(userId, vmId) == null) {
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

    private UserVm rebootVirtualMachine(long userId, long vmId) throws InsufficientCapacityException, ResourceUnavailableException {
        UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            s_logger.warn("Vm id=" + vmId + " doesn't exist");
            return null;
        }

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            collectVmDiskStatistics(vm);
            collectVmNetworkStatistics(vm);
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
            }finally {
                s_logger.info("Rebooting vm " + vm.getInstanceName());
                _itMgr.reboot(vm.getUuid(), null);
            }
            return _vmDao.findById(vmId);
        } else {
            s_logger.error("Vm id=" + vmId + " is not in Running state, failed to reboot");
            return null;
        }
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

        // Check resource limits for CPU and Memory.
        Map<String, String> customParameters = cmd.getDetails();
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(svcOffId);
        if (newServiceOffering.getState() == DiskOffering.State.Inactive) {
            throw new InvalidParameterValueException(String.format("Unable to upgrade virtual machine %s with an inactive service offering %s", vmInstance.getUuid(), newServiceOffering.getUuid()));
        }
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            validateCustomParameters(newServiceOffering, cmd.getDetails());
            newServiceOffering = _offeringDao.getcomputeOffering(newServiceOffering, customParameters);
        }
        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());

        int newCpu = newServiceOffering.getCpu();
        int newMemory = newServiceOffering.getRamSize();
        int currentCpu = currentServiceOffering.getCpu();
        int currentMemory = currentServiceOffering.getRamSize();

        if (newCpu > currentCpu) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.cpu, newCpu - currentCpu);
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.memory, newMemory - currentMemory);
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, newServiceOffering);

        _itMgr.upgradeVmDb(vmId, svcOffId);
        if (newServiceOffering.isDynamic()) {
            //save the custom values to the database.
            saveCustomOfferingDetails(vmId, newServiceOffering);
        }
        if (currentServiceOffering.isDynamic() && !newServiceOffering.isDynamic()) {
            removeCustomOfferingDetails(vmId);
        }

        // Increment or decrement CPU and Memory count accordingly.
        if (newCpu > currentCpu) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
        } else if (currentCpu > newCpu) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(currentCpu - newCpu));
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(newMemory - currentMemory));
        } else if (currentMemory > newMemory) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(currentMemory - newMemory));
        }

        // Generate usage event for VM upgrade
        UserVmVO userVm = _vmDao.findById(vmId);
        generateUsageEvent( userVm, userVm.isDisplayVm(), EventTypes.EVENT_VM_UPGRADE);

        return userVm;
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
                if (cpuNumber < minCPU || cpuNumber > maxCPU) {
                    throw new InvalidParameterValueException(String.format("Invalid cpu cores value, specify a value between %d and %d", minCPU, maxCPU));
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
                if (memory < minMemory || memory > maxMemory) {
                    throw new InvalidParameterValueException(String.format("Invalid memory value, specify a value between %d and %d", minMemory, maxMemory));
                }
            } else if (customParameters.containsKey(UsageEventVO.DynamicParameters.memory.name())) {
                throw new InvalidParameterValueException("The memory of this offering id:" + serviceOffering.getUuid() + " is not customizable. This is predefined in the template.");
            }
        } else {
            throw new InvalidParameterValueException("Need to specify custom parameter values cpu, cpu speed and memory when using custom offering");
        }
    }

    private UserVm upgradeStoppedVirtualMachine(Long vmId, Long svcOffId, Map<String, String> customParameters) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        // Verify input parameters
        //UserVmVO vmInstance = _vmDao.findById(vmId);
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Check resource limits for CPU and Memory.
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(svcOffId);
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            validateCustomParameters(newServiceOffering, customParameters);
            newServiceOffering = _offeringDao.getcomputeOffering(newServiceOffering, customParameters);
        }
        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());

        int newCpu = newServiceOffering.getCpu();
        int newMemory = newServiceOffering.getRamSize();
        int currentCpu = currentServiceOffering.getCpu();
        int currentMemory = currentServiceOffering.getRamSize();

        if (newCpu > currentCpu) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.cpu, newCpu - currentCpu);
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.memory, newMemory - currentMemory);
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, newServiceOffering);

        DiskOfferingVO newROOTDiskOffering = _diskOfferingDao.findById(newServiceOffering.getId());

        List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vmInstance.getId());

        for (final VolumeVO rootVolumeOfVm : vols) {
            rootVolumeOfVm.setDiskOfferingId(newROOTDiskOffering.getId());

            _volsDao.update(rootVolumeOfVm.getId(), rootVolumeOfVm);

            ResizeVolumeCmd resizeVolumeCmd = new ResizeVolumeCmd(rootVolumeOfVm.getId(), newROOTDiskOffering.getMinIops(), newROOTDiskOffering.getMaxIops());

            _volumeService.resizeVolume(resizeVolumeCmd);
        }

        // Check if the new service offering can be applied to vm instance
        ServiceOffering newSvcOffering = _offeringDao.findById(svcOffId);
        Account owner = _accountMgr.getActiveAccountById(vmInstance.getAccountId());
        _accountMgr.checkAccess(owner, newSvcOffering, _dcDao.findById(vmInstance.getDataCenterId()));

        _itMgr.upgradeVmDb(vmId, svcOffId);
        if (newServiceOffering.isDynamic()) {
            //save the custom values to the database.
            saveCustomOfferingDetails(vmId, newServiceOffering);
        }
        if (currentServiceOffering.isDynamic() && !newServiceOffering.isDynamic()) {
            removeCustomOfferingDetails(vmId);
        }

        // Increment or decrement CPU and Memory count accordingly.
        if (newCpu > currentCpu) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
        } else if (currentCpu > newCpu) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(currentCpu - newCpu));
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(newMemory - currentMemory));
        } else if (currentMemory > newMemory) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long(currentMemory - newMemory));
        }

        return _vmDao.findById(vmInstance.getId());

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

        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (!(network.getGuestType() == Network.GuestType.Shared && network.getAclType() == ACLType.Domain)
                    && !(network.getAclType() == ACLType.Account && network.getAccountId() == vmInstance.getAccountId())) {
                throw new InvalidParameterValueException("only shared network or isolated network with the same account_id can be added to vmId: " + vmId);
            }
        }

        List<NicVO> allNics = _nicDao.listByVmId(vmInstance.getId());
        for (NicVO nic : allNics) {
            if (nic.getNetworkId() == network.getId()) {
                throw new CloudRuntimeException("A NIC already exists for VM:" + vmInstance.getInstanceName() + " in network: " + network.getUuid());
            }
        }

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

        // Perform account permission check on network
        _accountMgr.checkAccess(caller, AccessType.UseEntry, false, network);

        //ensure network belongs in zone
        if (network.getDataCenterId() != vmInstance.getDataCenterId()) {
            throw new CloudRuntimeException(vmInstance + " is in zone:" + vmInstance.getDataCenterId() + " but " + network + " is in zone:" + network.getDataCenterId());
        }

        // Get all vms hostNames in the network
        List<String> hostNames = _vmInstanceDao.listDistinctHostNames(network.getId());
        // verify that there are no duplicates, listDistictHostNames could return hostNames even if the NIC
        //in the network is removed, so also check if the NIC is present and then throw an exception.
        //This will also check if there are multiple nics of same vm in the network
        if (hostNames.contains(vmInstance.getHostName())) {
            for (String hostName : hostNames) {
                VMInstanceVO vm = _vmInstanceDao.findVMByHostName(hostName);
                if (_networkModel.getNicInNetwork(vm.getId(), network.getId()) != null && vm.getHostName().equals(vmInstance.getHostName())) {
                    throw new CloudRuntimeException(network + " already has a vm with host name: " + vmInstance.getHostName());
                }
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
        s_logger.debug("Successful addition of " + network + " from " + vmInstance);
        return _vmDao.findById(vmInstance.getId());
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

        // Perform account permission check on network
        _accountMgr.checkAccess(caller, AccessType.UseEntry, false, network);

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

        nic.setDefaultNic(true);
        nic.setDeviceId(existingID);
        existingVO.setDefaultNic(false);
        existingVO.setDeviceId(chosenID);

        nic = _nicDao.persist(nic);
        existingVO = _nicDao.persist(existingVO);

        Network newdefault = null;
        newdefault = _networkModel.getDefaultNetworkForVm(vmId);

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

            if (_networkModel.areServicesSupportedInNetwork(network.getId(), Service.StaticNat)) {
                IPAddressVO oldIP = _ipAddressDao.findByAssociatedVmId(vm.getId());
                if (oldIP != null) {
                    oldIP.setVmIp(ipaddr);
                    _ipAddressDao.persist(oldIP);
                }
            }
            // implementing the network elements and resources as a part of vm nic ip update if network has services and it is in Implemented state
            if (!_networkModel.listNetworkOfferingServices(offering.getId()).isEmpty() && network.getState() == Network.State.Implemented) {
                User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
                ReservationContext context = new ReservationContextImpl(null, null, callerUser, caller);
                DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);

                s_logger.debug("Implementing the network " + network + " elements and resources as a part of vm nic ip update");
                try {
                    // implement the network elements and rules again
                    _networkMgr.implementNetworkElementsAndResources(dest, context, network, offering);
                } catch (Exception ex) {
                    s_logger.warn("Failed to implement network " + network + " elements and resources as a part of vm nic ip update due to ", ex);
                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id) elements and resources as a part of vm nic ip update");
                    e.addProxyObject(network.getUuid(), "networkId");
                    // restore to old ip address
                    if (_networkModel.areServicesSupportedInNetwork(network.getId(), Service.StaticNat)) {
                        IPAddressVO oldIP = _ipAddressDao.findByAssociatedVmId(vm.getId());
                        if (oldIP != null) {
                            oldIP.setVmIp(nicVO.getIPv4Address());
                            _ipAddressDao.persist(oldIP);
                        }
                    }
                    throw e;
                }
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

                final IPAddressVO newIp = _ipAddressDao.findByIpAndDcId(dc.getId(), ipaddr);
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
            s_logger.error("UpdateVmNicIpCmd is not supported in this network...");
            return null;
        }

        s_logger.debug("Updating IPv4 address of NIC " + nicVO + " to " + ipaddr + "/" + nicVO.getIPv4Netmask() + " with gateway " + nicVO.getIPv4Gateway());
        nicVO.setIPv4Address(ipaddr);
        _nicDao.persist(nicVO);

        return vm;
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
            if (vmInstance.getState().equals(State.Running)) {
                // Generate usage event for Dynamic scaling of VM
                generateUsageEvent( vmInstance, vmInstance.isDisplayVm(), EventTypes.EVENT_VM_DYNAMIC_SCALE);
            }
            return vmInstance;
        } else {
            throw new CloudRuntimeException("Failed to scale the VM");
        }
    }

    @Override
    public HashMap<Long, List<VmDiskStatsEntry>> getVmDiskStatistics(long hostId, String hostName, List<Long> vmIds) throws CloudRuntimeException {
        HashMap<Long, List<VmDiskStatsEntry>> vmDiskStatsById = new HashMap<Long, List<VmDiskStatsEntry>>();

        if (vmIds.isEmpty()) {
            return vmDiskStatsById;
        }

        List<String> vmNames = new ArrayList<String>();

        for (Long vmId : vmIds) {
            UserVmVO vm = _vmDao.findById(vmId);
            vmNames.add(vm.getInstanceName());
        }

        Answer answer = _agentMgr.easySend(hostId, new GetVmDiskStatsCommand(vmNames, _hostDao.findById(hostId).getGuid(), hostName));
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to obtain VM disk statistics.");
            return null;
        } else {
            HashMap<String, List<VmDiskStatsEntry>> vmDiskStatsByName = ((GetVmDiskStatsAnswer)answer).getVmDiskStatsMap();

            if (vmDiskStatsByName == null) {
                s_logger.warn("Unable to obtain VM disk statistics.");
                return null;
            }

            for (Map.Entry<String, List<VmDiskStatsEntry>> entry: vmDiskStatsByName.entrySet()) {
                vmDiskStatsById.put(vmIds.get(vmNames.indexOf(entry.getKey())), entry.getValue());
            }
        }

        return vmDiskStatsById;
    }

    @Override
    public boolean upgradeVirtualMachine(Long vmId, Long newServiceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
    ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {

        // Verify input parameters
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);

        if (vmInstance != null) {
            if (vmInstance.getState().equals(State.Stopped)) {
                upgradeStoppedVirtualMachine(vmId, newServiceOfferingId, customParameters);
                return true;
            }
            if (vmInstance.getState().equals(State.Running)) {
                return upgradeRunningVirtualMachine(vmId, newServiceOfferingId, customParameters);
            }
        }
        return false;
    }

    private boolean upgradeRunningVirtualMachine(Long vmId, Long newServiceOfferingId, Map<String, String> customParameters) throws ResourceUnavailableException,
    ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {

        Account caller = CallContext.current().getCallingAccount();
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        if (vmInstance.getHypervisorType() != HypervisorType.XenServer && vmInstance.getHypervisorType() != HypervisorType.VMware && vmInstance.getHypervisorType() != HypervisorType.Simulator) {
            s_logger.info("Scaling the VM dynamically is not supported for VMs running on Hypervisor "+vmInstance.getHypervisorType());
            throw new InvalidParameterValueException("Scaling the VM dynamically is not supported for VMs running on Hypervisor "+vmInstance.getHypervisorType());
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        //Check if its a scale "up"
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(newServiceOfferingId);
        if (newServiceOffering.isDynamic()) {
            newServiceOffering.setDynamicFlag(true);
            validateCustomParameters(newServiceOffering, customParameters);
            newServiceOffering = _offeringDao.getcomputeOffering(newServiceOffering, customParameters);
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, newServiceOffering);

        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());
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
            throw new InvalidParameterValueException("Only scaling up the vm is supported, new service offering(speed=" + newSpeed + ",cpu=" + newCpu + ",memory=," + newMemory
                    + ")" + " should have at least one value(cpu/ram) greater than old value and no resource value less than older(speed=" + currentSpeed + ",cpu=" + currentCpu
                    + ",memory=," + currentMemory + ")");
        }

        _offeringDao.loadDetails(currentServiceOffering);
        _offeringDao.loadDetails(newServiceOffering);

        Map<String, String> currentDetails = currentServiceOffering.getDetails();
        Map<String, String> newDetails = newServiceOffering.getDetails();
        String currentVgpuType = currentDetails.get("vgpuType");
        String newVgpuType = newDetails.get("vgpuType");
        if(currentVgpuType != null) {
            if(newVgpuType == null || !newVgpuType.equalsIgnoreCase(currentVgpuType)) {
                throw new InvalidParameterValueException("Dynamic scaling of vGPU type is not supported. VM has vGPU Type: " + currentVgpuType);
            }
        }

        // Check resource limits
        if (newCpu > currentCpu) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.cpu, newCpu - currentCpu);
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.memory, newMemory - currentMemory);
        }

        // Dynamically upgrade the running vms
        boolean success = false;
        if (vmInstance.getState().equals(State.Running)) {
            int retry = _scaleRetry;
            ExcludeList excludes = new ExcludeList();

            // Check zone wide flag
            boolean enableDynamicallyScaleVm = EnableDynamicallyScaleVm.valueIn(vmInstance.getDataCenterId());
            if (!enableDynamicallyScaleVm) {
                throw new PermissionDeniedException("Dynamically scaling virtual machines is disabled for this zone, please contact your admin");
            }

            // Check vm flag
            if (!vmInstance.isDynamicallyScalable()) {
                throw new CloudRuntimeException("Unable to Scale the vm: " + vmInstance.getUuid() + " as vm does not have tools to support dynamic scaling");
            }

            // Check disable threshold for cluster is not crossed
            HostVO host = _hostDao.findById(vmInstance.getHostId());
            if (_capacityMgr.checkIfClusterCrossesThreshold(host.getClusterId(), cpuDiff, memoryDiff)) {
                throw new CloudRuntimeException("Unable to scale vm: " + vmInstance.getUuid() + " due to insufficient resources");
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
                                && _capacityMgr.checkIfHostHasCapacity(vmInstance.getHostId(), cpuDiff, (memoryDiff) * 1024L * 1024L, false,
                                        _capacityMgr.getClusterOverProvisioningFactor(host.getClusterId(), Capacity.CAPACITY_TYPE_CPU),
                                        _capacityMgr.getClusterOverProvisioningFactor(host.getClusterId(), Capacity.CAPACITY_TYPE_MEMORY), false);
                        excludes.addHost(vmInstance.getHostId());
                    }

                    // #2 migrate the vm if host doesn't have capacity or is in avoid set
                    if (!existingHostHasCapacity) {
                        _itMgr.findHostAndMigrate(vmInstance.getUuid(), newServiceOfferingId, excludes);
                    }

                    // #3 scale the vm now
                    _itMgr.upgradeVmDb(vmId, newServiceOfferingId);
                    if (newServiceOffering.isDynamic()) {
                        //save the custom values to the database.
                        saveCustomOfferingDetails(vmId, newServiceOffering);
                    }
                    vmInstance = _vmInstanceDao.findById(vmId);
                    _itMgr.reConfigureVm(vmInstance.getUuid(), currentServiceOffering, existingHostHasCapacity);
                    success = true;
                    if (currentServiceOffering.isDynamic() && !newServiceOffering.isDynamic()) {
                        removeCustomOfferingDetails(vmId);
                    }
                    return success;
                } catch (InsufficientCapacityException e) {
                    s_logger.warn("Received exception while scaling ", e);
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Received exception while scaling ", e);
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("Received exception while scaling ", e);
                } catch (Exception e) {
                    s_logger.warn("Received exception while scaling ", e);
                } finally {
                    if (!success) {
                        _itMgr.upgradeVmDb(vmId, currentServiceOffering.getId()); // rollback

                        // Decrement CPU and Memory count accordingly.
                        if (newCpu > currentCpu) {
                            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long(newCpu - currentCpu));
                        }
                        //restoring old service offering will take care of removing new SO.
                        if(currentServiceOffering.isDynamic()){
                            saveCustomOfferingDetails(vmId, currentServiceOffering);
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

    @Override
    public void saveCustomOfferingDetails(long vmId, ServiceOffering serviceOffering) {
        //save the custom values to the database.
        Map<String, String> details = userVmDetailsDao.listDetailsKeyPairs(vmId);
        details.put(UsageEventVO.DynamicParameters.cpuNumber.name(), serviceOffering.getCpu().toString());
        details.put(UsageEventVO.DynamicParameters.cpuSpeed.name(), serviceOffering.getSpeed().toString());
        details.put(UsageEventVO.DynamicParameters.memory.name(), serviceOffering.getRamSize().toString());
        List<UserVmDetailVO> detailList = new ArrayList<UserVmDetailVO>();
        for (Map.Entry<String, String> entry: details.entrySet()) {
            UserVmDetailVO detailVO = new UserVmDetailVO(vmId, entry.getKey(), entry.getValue(), true);
            detailList.add(detailVO);
        }
        userVmDetailsDao.saveDetails(detailList);
    }

    @Override
    public void removeCustomOfferingDetails(long vmId) {
        Map<String, String> details = userVmDetailsDao.listDetailsKeyPairs(vmId);
        details.remove(UsageEventVO.DynamicParameters.cpuNumber.name());
        details.remove(UsageEventVO.DynamicParameters.cpuSpeed.name());
        details.remove(UsageEventVO.DynamicParameters.memory.name());
        List<UserVmDetailVO> detailList = new ArrayList<UserVmDetailVO>();
        for(Map.Entry<String, String> entry: details.entrySet()) {
            UserVmDetailVO detailVO = new UserVmDetailVO(vmId, entry.getKey(), entry.getValue(), true);
            detailList.add(detailVO);
        }
        userVmDetailsDao.saveDetails(detailList);
    }

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds) throws CloudRuntimeException {
        HashMap<Long, VmStatsEntry> vmStatsById = new HashMap<Long, VmStatsEntry>();

        if (vmIds.isEmpty()) {
            return vmStatsById;
        }

        List<String> vmNames = new ArrayList<String>();

        for (Long vmId : vmIds) {
            UserVmVO vm = _vmDao.findById(vmId);
            vmNames.add(vm.getInstanceName());
        }

        Answer answer = _agentMgr.easySend(hostId, new GetVmStatsCommand(vmNames, _hostDao.findById(hostId).getGuid(), hostName));
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to obtain VM statistics.");
            return null;
        } else {
            HashMap<String, VmStatsEntry> vmStatsByName = ((GetVmStatsAnswer)answer).getVmStatsMap();

            if (vmStatsByName == null) {
                s_logger.warn("Unable to obtain VM statistics.");
                return null;
            }

            for (Map.Entry<String, VmStatsEntry> entry : vmStatsByName.entrySet()) {
                vmStatsById.put(vmIds.get(vmNames.indexOf(entry.getKey())), entry.getValue());
            }
        }

        return vmStatsById;
    }

    @Override
    public HashMap<String, VolumeStatsEntry> getVolumeStatistics(long clusterId, String poolUuid, StoragePoolType poolType, List<String> volumeLocators, int timeout) {
        List<HostVO> neighbors = _resourceMgr.listHostsInClusterByStatus(clusterId, Status.Up);
        StoragePool storagePool = _storagePoolDao.findPoolByUUID(poolUuid);
        for (HostVO neighbor : neighbors) {
            if (storagePool.isManaged()) {

                volumeLocators = getVolumesByHost(neighbor, storagePool);

            }

            GetVolumeStatsCommand cmd = new GetVolumeStatsCommand(poolType, poolUuid, volumeLocators);

            if (timeout > 0) {
                cmd.setWait(timeout/1000);
            }

            Answer answer = _agentMgr.easySend(neighbor.getId(), cmd);

            if (answer instanceof GetVolumeStatsAnswer){
                GetVolumeStatsAnswer volstats = (GetVolumeStatsAnswer)answer;
                return volstats.getVolumeStats();
            }
        }
        return null;
    }

    private List<String> getVolumesByHost(HostVO host, StoragePool pool){
        List<UserVmVO> vmsPerHost = _vmDao.listByHostId(host.getId());
        return vmsPerHost.stream()
                .flatMap(vm -> _volsDao.findByInstanceIdAndPoolId(vm.getId(),pool.getId()).stream().map(vol -> vol.getPath()))
                .collect(Collectors.toList());
    }

    @Override
    @DB
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
                ServiceOfferingVO serviceOffering = _serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

                // First check that the maximum number of UserVMs, CPU and Memory limit for the given
                // accountId will not be exceeded
                resourceLimitCheck(account, vm.isDisplayVm(), new Long(serviceOffering.getCpu()), new Long(serviceOffering.getRamSize()));

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
                        // Create an event
                        Long templateId = volume.getTemplateId();
                        Long diskOfferingId = volume.getDiskOfferingId();
                        Long offeringId = null;
                        if (diskOfferingId != null) {
                            DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);
                            if (offering != null && (offering.getType() == DiskOfferingVO.Type.Disk)) {
                                offeringId = offering.getId();
                            }
                        }
                        UsageEventUtils
                        .publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), offeringId,
                                templateId, volume.getSize(), Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
                    }
                }

                //Update Resource Count for the given account
                resourceCountIncrement(account.getId(), vm.isDisplayVm(), new Long(serviceOffering.getCpu()), new Long(serviceOffering.getRamSize()));
            }
        });

        return _vmDao.findById(vmId);
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

        VirtualMachine.State.getStateMachine().registerListener(new UserVmStateListener(_usageEventDao, _networkDao, _nicDao, _offeringDao, _vmDao, this, _configDao));

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
                        if (vmInstance.getState() == State.Running) {
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
    public boolean expunge(UserVmVO vm, long callerUserId, Account caller) {
        vm = _vmDao.acquireInLockTable(vm.getId());
        if (vm == null) {
            return false;
        }
        try {

            releaseNetworkResourcesOnExpunge(vm.getId());

            List<VolumeVO> rootVol = _volsDao.findByInstanceAndType(vm.getId(), Volume.Type.ROOT);
            // expunge the vm
            _itMgr.advanceExpunge(vm.getUuid());
            // Update Resource count
            if (vm.getAccountId() != Account.ACCOUNT_ID_SYSTEM && !rootVol.isEmpty()) {
                _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.volume);
                _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.primary_storage, new Long(rootVol.get(0).getSize()));
            }

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
                ServiceOfferingVO offering = _serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());

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
                                        "VM " + vmId + " nic id "+ nicId + " ip addr fetch failed ");

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
        String userData = cmd.getUserData();
        Boolean isDynamicallyScalable = cmd.isDynamicallyScalable();
        String hostName = cmd.getHostName();
        Map<String,String> details = cmd.getDetails();
        List<Long> securityGroupIdList = getSecurityGroupIdList(cmd);
        boolean cleanupDetails = cmd.isCleanupDetails();
        String extraConfig = cmd.getExtraConfig();

        UserVmVO vmInstance = _vmDao.findById(cmd.getId());
        long accountId = vmInstance.getAccountId();

        if (isDisplayVm != null && isDisplayVm != vmInstance.isDisplay()) {
            updateDisplayVmFlag(isDisplayVm, id, vmInstance);
        }
        final Account caller = CallContext.current().getCallingAccount();
        final List<String> userBlacklistedSettings = Stream.of(QueryService.UserVMBlacklistedDetails.value().split(","))
                .map(item -> (item).trim())
                .collect(Collectors.toList());
        if (cleanupDetails){
            if (caller != null && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
                userVmDetailsDao.removeDetails(id);
            } else {
                for (final UserVmDetailVO detail : userVmDetailsDao.listDetails(id)) {
                    if (detail != null && !userBlacklistedSettings.contains(detail.getName())) {
                        userVmDetailsDao.removeDetail(id, detail.getName());
                    }
                }
            }
        } else {
            if (MapUtils.isNotEmpty(details)) {
                if (caller != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                    // Ensure blacklisted detail is not passed by non-root-admin user
                    for (final String detailName : details.keySet()) {
                        if (userBlacklistedSettings.contains(detailName)) {
                            throw new InvalidParameterValueException("You're not allowed to add or edit the restricted setting: " + detailName);
                        }
                    }
                    // Add any hidden/blacklisted detail
                    for (final UserVmDetailVO detail : userVmDetailsDao.listDetails(id)) {
                        if (userBlacklistedSettings.contains(detail.getName())) {
                            details.put(detail.getName(), detail.getValue());
                        }
                    }
                }
                for (String detailName : details.keySet()) {
                    if (detailName.startsWith(ApiConstants.OVF_PROPERTIES)) {
                        String ovfPropKey = detailName.replace(ApiConstants.OVF_PROPERTIES + "-", "");
                        TemplateOVFPropertyVO ovfPropertyVO = templateOVFPropertiesDao.findByTemplateAndKey(vmInstance.getTemplateId(), ovfPropKey);
                        if (ovfPropertyVO != null && ovfPropertyVO.isPassword()) {
                            details.put(detailName, DBEncryptionUtil.encrypt(details.get(detailName)));
                        }
                    }
                }
                vmInstance.setDetails(details);
                _vmDao.saveDetails(vmInstance);
            }
            if (StringUtils.isNotBlank(extraConfig) && EnableAdditionalVmConfig.valueIn(accountId)) {
                AccountVO account = _accountDao.findById(accountId);
                addExtraConfig(vmInstance, account, extraConfig);
            }
        }
        return updateVirtualMachine(id, displayName, group, ha, isDisplayVm, osTypeId, userData, isDynamicallyScalable,
                cmd.getHttpMethod(), cmd.getCustomId(), hostName, cmd.getInstanceName(), securityGroupIdList, cmd.getDhcpOptionsMap());
    }

    protected void updateDisplayVmFlag(Boolean isDisplayVm, Long id, UserVmVO vmInstance) {
        vmInstance.setDisplayVm(isDisplayVm);

        // Resource limit changes
        ServiceOffering offering = _serviceOfferingDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());
        _resourceLimitMgr.changeResourceCount(vmInstance.getAccountId(), ResourceType.user_vm, isDisplayVm);
        _resourceLimitMgr.changeResourceCount(vmInstance.getAccountId(), ResourceType.cpu, isDisplayVm, new Long(offering.getCpu()));
        _resourceLimitMgr.changeResourceCount(vmInstance.getAccountId(), ResourceType.memory, isDisplayVm, new Long(offering.getRamSize()));

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
            Boolean isDynamicallyScalable, HTTPMethod httpMethod, String customId, String hostName, String instanceName, List<Long> securityGroupIdList, Map<String, Map<Integer, String>> extraDhcpOptionsMap)
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

        ServiceOffering offering = _serviceOfferingDao.findById(vm.getId(), vm.getServiceOfferingId());
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

        if (isDynamicallyScalable == null) {
            isDynamicallyScalable = vm.isDynamicallyScalable();
        }

        if (osTypeId == null) {
            osTypeId = vm.getGuestOSId();
        }

        if (group != null) {
            addInstanceToGroup(id, group);
        }

        if (isDynamicallyScalable == null) {
            isDynamicallyScalable = vm.isDynamicallyScalable();
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

            if (vm.getHostName().equalsIgnoreCase(hostName)) {
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

        _vmDao.updateVM(id, displayName, ha, osTypeId, userData, isDisplayVmEnabled, isDynamicallyScalable, customId, hostName, instanceName);

        if (updateUserdata) {
            boolean result = updateUserDataInternal(_vmDao.findById(id));
            if (result) {
                s_logger.debug("User data successfully updated for vm id=" + id);
            } else {
                throw new CloudRuntimeException("Failed to reset userdata for the virtual machine ");
            }
        }

        return _vmDao.findById(id);
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
    public UserVm startVirtualMachine(StartVMCmd cmd) throws ExecutionException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return startVirtualMachine(cmd.getId(), cmd.getPodId(), cmd.getClusterId(), cmd.getHostId(), null, cmd.getDeploymentPlanner()).first();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_REBOOT, eventDescription = "rebooting Vm", async = true)
    public UserVm rebootVirtualMachine(RebootVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();
        Long vmId = cmd.getId();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        checkIfHostOfVMIsInPrepareForMaintenanceState(vmInstance.getHostId(), vmId, "Reboot");

        // If the VM is Volatile in nature, on reboot discard the VM's root disk and create a new root disk for it: by calling restoreVM
        long serviceOfferingId = vmInstance.getServiceOfferingId();
        ServiceOfferingVO offering = _serviceOfferingDao.findById(vmInstance.getId(), serviceOfferingId);
        if (offering != null && offering.getRemoved() == null) {
            if (offering.isVolatileVm()) {
                return restoreVMInternal(caller, vmInstance, null);
            }
        } else {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId + " corresponding to the vm");
        }

        UserVm userVm = rebootVirtualMachine(CallContext.current().getCallingUserId(), vmId);
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

        if (vm.getState() == State.Destroyed || vm.getState() == State.Expunging) {
            s_logger.debug("Vm id=" + vmId + " is already destroyed");
            return vm;
        }

        // check if there are active volume snapshots tasks
        s_logger.debug("Checking if there are any ongoing snapshots on the ROOT volumes associated with VM with ID " + vmId);
        if (checkStatusOfVolumeSnapshots(vmId, Volume.Type.ROOT)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on ROOT volume, vm destroy is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volume of type ROOT, for the vm with id " + vmId);

        List<VolumeVO> volumes = getVolumesFromIds(cmd);

        checkForUnattachedVolumes(vmId, volumes);
        validateVolumes(volumes);

        stopVirtualMachine(vmId, VmDestroyForcestop.value());

        detachVolumesFromVm(volumes);

        UserVm destroyedVm = destroyVm(vmId, expunge);
        if (expunge) {
            if (!expunge(vm, ctx.getCallingUserId(), ctx.getCallingAccount())) {
                throw new CloudRuntimeException("Failed to expunge vm " + destroyedVm);
            }
        }

        deleteVolumesFromVm(volumes);

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
            String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard, List<Long> affinityGroupIdList,
            Map<String, String> customParametes, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
            Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap, Map<String, String> userVmOVFProperties) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
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
                userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIps, displayVm, keyboard, affinityGroupIdList, customParametes, customId, dhcpOptionMap,
                dataDiskTemplateToDiskOfferingMap, userVmOVFProperties);

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor,
            HTTPMethod httpmethod, String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard,
            List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
            Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap, Map<String, String> userVmOVFProperties) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

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
            if (networkIdList.size() > 1) {
                throw new InvalidParameterValueException("Only support one network per VM if security group enabled");
            }

            NetworkVO network = _networkDao.findById(networkIdList.get(0));

            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
            }

            if (!_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                throw new InvalidParameterValueException("Network is not security group enabled: " + network.getId());
            }

            networkList.add(network);
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
                    if (networkIdList.size() > 1) {
                        throw new InvalidParameterValueException("Can't create a vm with multiple networks one of" + " which is Security Group enabled");
                    }

                    isSecurityGroupEnabledNetworkUsed = true;
                }

                if (!(network.getTrafficType() == TrafficType.Guest && network.getGuestType() == Network.GuestType.Shared)) {
                    throw new InvalidParameterValueException("Can specify only Shared Guest networks when" + " deploy vm in Advance Security Group enabled zone");
                }

                // Perform account permission check
                if (network.getAclType() == ACLType.Account) {
                    _accountMgr.checkAccess(caller, AccessType.UseEntry, false, network);
                }
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
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, securityGroupIdList, group, httpmethod,
                userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIps, displayVm, keyboard, affinityGroupIdList, customParameters, customId, dhcpOptionMap, dataDiskTemplateToDiskOfferingMap,
                userVmOVFProperties);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner,
            String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, HTTPMethod httpmethod, String userData,
            String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayvm, String keyboard, List<Long> affinityGroupIdList,
            Map<String, String> customParametrs, String customId, Map<String, Map<Integer, String>> dhcpOptionsMap, Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
            Map<String, String> userVmOVFPropertiesMap) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
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
                    long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
                    // Validate physical network
                    PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
                    if (physicalNetwork == null) {
                        throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                                + requiredOfferings.get(0).getTags());
                    }
                    s_logger.debug("Creating network for account " + owner + " from the network offering id=" + requiredOfferings.get(0).getId() + " as a part of deployVM process");
                    Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName() + "-network",
                            null, null, null, false, null, owner, null, physicalNetwork, zone.getId(), ACLType.Account, null, null, null, null, true, null,
                            null);
                    if (newNetwork != null) {
                        defaultNetwork = _networkDao.findById(newNetwork.getId());
                    }
                } else if (virtualNetworks.size() > 1) {
                    throw new InvalidParameterValueException("More than 1 default Isolated networks are found for account " + owner + "; please specify networkIds");
                } else {
                    defaultNetwork = _networkDao.findById(virtualNetworks.get(0).getId());
                }
            } else {
                throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled);
            }

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
                sshKeyPair, hypervisor, caller, requestedIps, defaultIps, displayvm, keyboard, affinityGroupIdList, customParametrs, customId, dhcpOptionsMap,
                dataDiskTemplateToDiskOfferingMap, userVmOVFPropertiesMap);
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
            String sshKeyPair, HypervisorType hypervisor, Account caller, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean isDisplayVm, String keyboard,
            List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
            Map<Long, DiskOffering> datadiskTemplateToDiskOfferringMap,
            Map<String, String> userVmOVFPropertiesMap) throws InsufficientCapacityException, ResourceUnavailableException,
    ConcurrentOperationException, StorageUnavailableException, ResourceAllocationException {

        _accountMgr.checkAccess(caller, null, true, owner);

        if (owner.getState() == Account.State.disabled) {
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

        ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOffering.getId());
        if (offering.isDynamic()) {
            offering.setDynamicFlag(true);
            validateCustomParameters(offering, customParameters);
            offering = _offeringDao.getcomputeOffering(offering, customParameters);
        }
        // check if account/domain is with in resource limits to create a new vm
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();
        long size = 0;
        // custom root disk size, resizes base template to larger size
        if (customParameters.containsKey(VmDetailConstants.ROOT_DISK_SIZE)) {
            // only KVM, XenServer and VMware supports rootdisksize override
            if (!(hypervisorType == HypervisorType.KVM || hypervisorType == HypervisorType.XenServer || hypervisorType == HypervisorType.VMware || hypervisorType == HypervisorType.Simulator)) {
                throw new InvalidParameterValueException("Hypervisor " + hypervisorType + " does not support rootdisksize override");
            }

            Long rootDiskSize = NumbersUtil.parseLong(customParameters.get(VmDetailConstants.ROOT_DISK_SIZE), -1);
            if (rootDiskSize <= 0) {
                throw new InvalidParameterValueException("Root disk size should be a positive number.");
            }
            size = rootDiskSize * GiB_TO_BYTES;
        } else {
            // For baremetal, size can be null
            Long templateSize = _templateDao.findById(template.getId()).getSize();
            if (templateSize != null) {
                size = templateSize;
            }
        }
        if (diskOfferingId != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if (diskOffering != null && diskOffering.isCustomized()) {
                if (diskSize == null) {
                    throw new InvalidParameterValueException("This disk offering requires a custom size specified");
                }
                Long customDiskOfferingMaxSize = VolumeOrchestrationService.CustomDiskOfferingMaxSize.value();
                Long customDiskOfferingMinSize = VolumeOrchestrationService.CustomDiskOfferingMinSize.value();
                if ((diskSize < customDiskOfferingMinSize) || (diskSize > customDiskOfferingMaxSize)) {
                    throw new InvalidParameterValueException("VM Creation failed. Volume size: " + diskSize + "GB is out of allowed range. Max: " + customDiskOfferingMaxSize
                            + " Min:" + customDiskOfferingMinSize);
                }
                size += diskSize * GiB_TO_BYTES;
            }
            size += _diskOfferingDao.findById(diskOfferingId).getDiskSize();
        }
        resourceLimitCheck(owner, isDisplayVm, new Long(offering.getCpu()), new Long(offering.getRamSize()));

        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.volume, (isIso || diskOfferingId == null ? 1 : 2));
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.primary_storage, size);

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

        if (template.getTemplateType().equals(TemplateType.SYSTEM)) {
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
        String sshPublicKey = null;
        if (sshKeyPair != null && !sshKeyPair.equals("")) {
            SSHKeyPair pair = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (pair == null) {
                throw new InvalidParameterValueException("A key pair with name '" + sshKeyPair + "' was not found.");
            }

            sshPublicKey = pair.getPublicKey();
        }

        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();

        LinkedHashMap<String, NicProfile> networkNicMap = new LinkedHashMap<String, NicProfile>();

        short defaultNetworkNumber = 0;
        boolean securityGroupEnabled = false;
        boolean vpcNetwork = false;
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

            //relax the check if the caller is admin account
            if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                if (!(network.getGuestType() == Network.GuestType.Shared && network.getAclType() == ACLType.Domain)
                        && !(network.getAclType() == ACLType.Account && network.getAccountId() == accountId)) {
                    throw new InvalidParameterValueException("only shared network or isolated network with the same account_id can be added to vm");
                }
            }

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
                        throw new InvalidParameterValueException("Unable to deploy VM as UserData is provided while deploying the VM, but there is no support for " + Network.Service.UserData.getName() + " service in the default network " + network.getId());
                    }

                    if ((sshPublicKey != null) && (!sshPublicKey.isEmpty())) {
                        throw new InvalidParameterValueException("Unable to deploy VM as SSH keypair is provided while deploying the VM, but there is no support for " + Network.Service.UserData.getName() + " service in the default network " + network.getId());
                    }

                    if (template.isEnablePassword()) {
                        throw new InvalidParameterValueException("Unable to deploy VM as template " + template.getId() + " is password enabled, but there is no support for " + Network.Service.UserData.getName() + " service in the default network " + network.getId());
                    }
                }
            }

            networks.add(new Pair<NetworkVO, NicProfile>(network, profile));

            if (_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                securityGroupEnabled = true;
            }

            // vm can't be a part of more than 1 VPC network
            if (network.getVpcId() != null) {
                if (vpcNetwork) {
                    throw new InvalidParameterValueException("Vm can't be a part of more than 1 VPC network");
                }
                vpcNetwork = true;
            }

            networkNicMap.put(network.getUuid(), profile);
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
        String uuidName = _uuidMgr.generateUuid(UserVm.class, customId);
        if (_instanceNameFlag && hypervisor.equals(HypervisorType.VMware)) {
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
            if (vmByHostName != null && vmByHostName.getState() != VirtualMachine.State.Expunging) {
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
        instanceName = VirtualMachineName.getVmName(id, owner.getId(), _instance);

        // Check if VM with instanceName already exists.
        VMInstanceVO vmObj = _vmInstanceDao.findVMByInstanceName(instanceName);
        if (vmObj != null && vmObj.getState() != VirtualMachine.State.Expunging) {
            throw new InvalidParameterValueException("There already exists a VM by the display name supplied");
        }

        checkIfHostNameUniqueInNtwkDomain(hostName, networkList);

        long userId = CallContext.current().getCallingUserId();
        if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
            List<UserVO> userVOs = _userDao.listByAccount(owner.getAccountId());
            if (!userVOs.isEmpty()) {
                userId =  userVOs.get(0).getId();
            }
        }

        UserVmVO vm = commitUserVm(zone, template, hostName, displayName, owner, diskOfferingId, diskSize, userData, caller, isDisplayVm, keyboard, accountId, userId, offering,
                isIso, sshPublicKey, networkNicMap, id, instanceName, uuidName, hypervisorType, customParameters, dhcpOptionMap,
                datadiskTemplateToDiskOfferringMap, userVmOVFPropertiesMap);

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

        _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);

        if (affinityGroupIdList != null && !affinityGroupIdList.isEmpty()) {
            _affinityGroupVMMapDao.updateMap(vm.getId(), affinityGroupIdList);
        }

        CallContext.current().putContextParameter(VirtualMachine.class, vm.getUuid());
        return vm;
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
                            + _networkModel.getNetwork(ntwkId));
                }
            }
        }
    }

    private String generateHostName(String uuidName) {
        return _instance + "-" + uuidName;
    }

    private UserVmVO commitUserVm(final DataCenter zone, final VirtualMachineTemplate template, final String hostName, final String displayName, final Account owner,
            final Long diskOfferingId, final Long diskSize, final String userData, final Account caller, final Boolean isDisplayVm, final String keyboard,
            final long accountId, final long userId, final ServiceOfferingVO offering, final boolean isIso, final String sshPublicKey, final LinkedHashMap<String, NicProfile> networkNicMap,
            final long id, final String instanceName, final String uuidName, final HypervisorType hypervisorType, final Map<String, String> customParameters, final Map<String,
            Map<Integer, String>> extraDhcpOptionMap, final Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
            Map<String, String> userVmOVFPropertiesMap) throws InsufficientCapacityException {
        return Transaction.execute(new TransactionCallbackWithException<UserVmVO, InsufficientCapacityException>() {
            @Override
            public UserVmVO doInTransaction(TransactionStatus status) throws InsufficientCapacityException {
                UserVmVO vm = new UserVmVO(id, instanceName, displayName, template.getId(), hypervisorType, template.getGuestOSId(), offering.isOfferHA(),
                        offering.getLimitCpuUse(), owner.getDomainId(), owner.getId(), userId, offering.getId(), userData, hostName, diskOfferingId);
                vm.setUuid(uuidName);
                vm.setDynamicallyScalable(template.isDynamicallyScalable());

                Map<String, String> details = template.getDetails();
                if (details != null && !details.isEmpty()) {
                    vm.details.putAll(details);
                }

                if (sshPublicKey != null) {
                    vm.setDetail(VmDetailConstants.SSH_PUBLIC_KEY, sshPublicKey);
                }

                if (keyboard != null && !keyboard.isEmpty()) {
                    vm.setDetail(VmDetailConstants.KEYBOARD, keyboard);
                }

                if (isIso) {
                    vm.setIsoId(template.getId());
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

                long guestOSId = template.getGuestOSId();
                GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
                long guestOSCategoryId = guestOS.getCategoryId();
                GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);

                // If hypervisor is vSphere and OS is OS X, set special settings.
                if (hypervisorType.equals(HypervisorType.VMware)) {
                    if (guestOS.getDisplayName().toLowerCase().contains("apple mac os")) {
                        vm.setDetail(VmDetailConstants.SMC_PRESENT, "TRUE");
                        vm.setDetail(VmDetailConstants.ROOT_DISK_CONTROLLER, "scsi");
                        vm.setDetail(VmDetailConstants.DATA_DISK_CONTROLLER, "scsi");
                        vm.setDetail(VmDetailConstants.FIRMWARE, "efi");
                        s_logger.info("guestOS is OSX : overwrite root disk controller to scsi, use smc and efi");
                    } else {
                        String controllerSetting = _configDao.getValue("vmware.root.disk.controller");
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

                _vmDao.persist(vm);
                for (String key : customParameters.keySet()) {
                    if( key.equalsIgnoreCase(VmDetailConstants.CPU_NUMBER) ||
                            key.equalsIgnoreCase(VmDetailConstants.CPU_SPEED) ||
                            key.equalsIgnoreCase(VmDetailConstants.MEMORY)) {
                        // handle double byte strings.
                        vm.setDetail(key, Integer.toString(Integer.parseInt(customParameters.get(key))));
                    } else {
                        vm.setDetail(key, customParameters.get(key));
                    }
                }
                vm.setDetail(VmDetailConstants.DEPLOY_VM, "true");

                if (MapUtils.isNotEmpty(userVmOVFPropertiesMap)) {
                    for (String key : userVmOVFPropertiesMap.keySet()) {
                        String detailKey = ApiConstants.OVF_PROPERTIES + "-" + key;
                        String value = userVmOVFPropertiesMap.get(key);

                        // Sanitize boolean values to expected format and encrypt passwords
                        if (StringUtils.isNotBlank(value)) {
                            if (value.equalsIgnoreCase("True")) {
                                value = "True";
                            } else if (value.equalsIgnoreCase("False")) {
                                value = "False";
                            } else {
                                TemplateOVFPropertyVO ovfPropertyVO = templateOVFPropertiesDao.findByTemplateAndKey(vm.getTemplateId(), key);
                                if (ovfPropertyVO.isPassword()) {
                                    value = DBEncryptionUtil.encrypt(value);
                                }
                            }
                        }
                        vm.setDetail(detailKey, value);
                    }
                }

                _vmDao.saveDetails(vm);

                s_logger.debug("Allocating in the DB for vm");
                DataCenterDeployment plan = new DataCenterDeployment(zone.getId());

                List<String> computeTags = new ArrayList<String>();
                computeTags.add(offering.getHostTag());

                List<String> rootDiskTags = new ArrayList<String>();
                rootDiskTags.add(offering.getTags());

                if (isIso) {
                    _orchSrvc.createVirtualMachineFromScratch(vm.getUuid(), Long.toString(owner.getAccountId()), vm.getIsoId().toString(), hostName, displayName,
                            hypervisorType.name(), guestOSCategory.getName(), offering.getCpu(), offering.getSpeed(), offering.getRamSize(), diskSize, computeTags, rootDiskTags,
                            networkNicMap, plan, extraDhcpOptionMap);
                } else {
                    _orchSrvc.createVirtualMachine(vm.getUuid(), Long.toString(owner.getAccountId()), Long.toString(template.getId()), hostName, displayName, hypervisorType.name(),
                            offering.getCpu(), offering.getSpeed(), offering.getRamSize(), diskSize, computeTags, rootDiskTags, networkNicMap, plan, rootDiskSize, extraDhcpOptionMap, dataDiskTemplateToDiskOfferingMap);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully allocated DB entry for " + vm);
                }
                CallContext.current().setEventDetails("Vm Id: " + vm.getUuid());

                if (!offering.isDynamic()) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(), vm.getHostName(), offering.getId(), template.getId(),
                            hypervisorType.toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
                } else {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(), vm.getHostName(), offering.getId(), template.getId(),
                            hypervisorType.toString(), VirtualMachine.class.getName(), vm.getUuid(), customParameters, vm.isDisplayVm());
                }

                //Update Resource Count for the given account
                resourceCountIncrement(accountId, isDisplayVm, new Long(offering.getCpu()), new Long(offering.getRamSize()));
                return vm;
            }
        });
    }

    public void validateRootDiskResize(final HypervisorType hypervisorType, Long rootDiskSize, VMTemplateVO templateVO, UserVmVO vm, final Map<String, String> customParameters) throws InvalidParameterValueException
    {
        // rootdisksize must be larger than template.
        if ((rootDiskSize << 30) < templateVO.getSize()) {
            Long templateVOSizeGB = templateVO.getSize() / GiB_TO_BYTES;
            String error = "Unsupported: rootdisksize override is smaller than template size " + templateVO.getSize() + "B (" + templateVOSizeGB + "GB)";
            s_logger.error(error);
            throw new InvalidParameterValueException(error);
        } else if ((rootDiskSize << 30) > templateVO.getSize()) {
            if (hypervisorType == HypervisorType.VMware && (vm.getDetails() == null || vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER) == null)) {
                s_logger.warn("If Root disk controller parameter is not overridden, then Root disk resize may fail because current Root disk controller value is NULL.");
            } else if (hypervisorType == HypervisorType.VMware && !vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER).toLowerCase().contains("scsi")) {
                String error = "Found unsupported root disk controller: " + vm.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER);
                s_logger.error(error);
                throw new InvalidParameterValueException(error);
            } else {
                s_logger.debug("Rootdisksize override validation successful. Template root disk size " + (templateVO.getSize() / GiB_TO_BYTES) + "GB Root disk size specified " + rootDiskSize + "GB");
            }
        } else {
            s_logger.debug("Root disk size specified is " + (rootDiskSize << 30) + "B and Template root disk size is " + templateVO.getSize() + "B. Both are equal so no need to override");
            customParameters.remove(VmDetailConstants.ROOT_DISK_SIZE);
        }
    }


    @Override
    public void generateUsageEvent(VirtualMachine vm, boolean isDisplay, String eventType){
        ServiceOfferingVO serviceOffering = _offeringDao.findById(vm.getId(), vm.getServiceOfferingId());
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
    public HashMap<Long, List<VmNetworkStatsEntry>> getVmNetworkStatistics(long hostId, String hostName, List<Long> vmIds) {
        HashMap<Long, List<VmNetworkStatsEntry>> vmNetworkStatsById = new HashMap<Long, List<VmNetworkStatsEntry>>();

        if (vmIds.isEmpty()) {
            return vmNetworkStatsById;
        }

        List<String> vmNames = new ArrayList<String>();

        for (Long vmId : vmIds) {
            UserVmVO vm = _vmDao.findById(vmId);
            vmNames.add(vm.getInstanceName());
        }

        Answer answer = _agentMgr.easySend(hostId, new GetVmNetworkStatsCommand(vmNames, _hostDao.findById(hostId).getGuid(), hostName));
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to obtain VM network statistics.");
            return null;
        } else {
            HashMap<String, List<VmNetworkStatsEntry>> vmNetworkStatsByName = ((GetVmNetworkStatsAnswer)answer).getVmNetworkStatsMap();

            if (vmNetworkStatsByName == null) {
                s_logger.warn("Unable to obtain VM network statistics.");
                return null;
            }

            for (String vmName : vmNetworkStatsByName.keySet()) {
                vmNetworkStatsById.put(vmIds.get(vmNames.indexOf(vmName)), vmNetworkStatsByName.get(vmName));
            }
        }

        return vmNetworkStatsById;
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
                                        " Sent(Bytes): " + vmNetworkStat.getBytesSent() + " Received(Bytes): " + vmNetworkStat.getBytesReceived());
                                continue;
                            }

                            if (vmNetworkStat_lock.getCurrentBytesSent() > vmNetworkStat.getBytesSent()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Sent # of bytes that's less than the last one.  " +
                                            "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmNetworkStat.getVmName() +
                                            " Reported: " + vmNetworkStat.getBytesSent() + " Stored: " + vmNetworkStat_lock.getCurrentBytesSent());
                                }
                                vmNetworkStat_lock.setNetBytesSent(vmNetworkStat_lock.getNetBytesSent() + vmNetworkStat_lock.getCurrentBytesSent());
                            }
                            vmNetworkStat_lock.setCurrentBytesSent(vmNetworkStat.getBytesSent());

                            if (vmNetworkStat_lock.getCurrentBytesReceived() > vmNetworkStat.getBytesReceived()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  " +
                                            "Assuming something went wrong and persisting it. Host: " + host.getName() + " . VM: " + vmNetworkStat.getVmName() +
                                            " Reported: " + vmNetworkStat.getBytesReceived() + " Stored: " + vmNetworkStat_lock.getCurrentBytesReceived());
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
            // If GET, use 4K. If POST, support upto 32K.
            if (httpmethod.equals(HTTPMethod.GET)) {
                if (userData.length() >= MAX_HTTP_GET_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for an http GET request");
                }
                decodedUserData = Base64.decodeBase64(userData.getBytes());
                if (decodedUserData.length > MAX_HTTP_GET_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for GET request");
                }
            } else if (httpmethod.equals(HTTPMethod.POST)) {
                if (userData.length() >= MAX_HTTP_POST_LENGTH) {
                    throw new InvalidParameterValueException("User data is too long for an http POST request");
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
    public UserVm startVirtualMachine(DeployVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        long vmId = cmd.getEntityId();
        Long podId = null;
        Long clusterId = null;
        Long hostId = cmd.getHostId();
        Map<Long, DiskOffering> diskOfferingMap = cmd.getDataDiskTemplateToDiskOfferingMap();
        if (cmd instanceof DeployVMCmdByAdmin) {
            DeployVMCmdByAdmin adminCmd = (DeployVMCmdByAdmin)cmd;
            podId = adminCmd.getPodId();
            clusterId = adminCmd.getClusterId();
        }
        return startVirtualMachine(vmId, podId, clusterId, hostId, diskOfferingMap, null, cmd.getDeploymentPlanner());
    }

    private UserVm startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId, Map<Long, DiskOffering> diskOfferingMap, Map<VirtualMachineProfile.Param, Object> additonalParams, String deploymentPlannerToUse)
            throws ResourceUnavailableException,
            InsufficientCapacityException, ConcurrentOperationException {
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

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {
        UserVmVO vm = _vmDao.findById(profile.getId());
        Map<String, String> details = userVmDetailsDao.listDetailsKeyPairs(vm.getId());
        vm.setDetails(details);

        // add userdata info into vm profile
        Nic defaultNic = _networkModel.getDefaultNic(vm.getId());
        if(defaultNic != null) {
            Network network = _networkModel.getNetwork(defaultNic.getNetworkId());
            if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
                boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

                List<String[]> vmData = _networkModel.generateVmData(vm.getUserData(), serviceOffering, vm.getDataCenterId(), vm.getInstanceName(), vm.getHostName(), vm.getId(),
                        vm.getUuid(), defaultNic.getIPv4Address(), vm.getDetail(VmDetailConstants.SSH_PUBLIC_KEY), (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword), isWindows);
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
    public boolean  finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
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

        _userDao.findById(userId);
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
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return startVirtualMachine(vmId, null, null, hostId, additionalParams, deploymentPlannerToUse);
    }

    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(long vmId, Long podId, Long clusterId, Long hostId, Map<VirtualMachineProfile.Param, Object> additionalParams, String deploymentPlannerToUse)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
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

        _accountMgr.checkAccess(callerAccount, null, true, vm);

        Account owner = _accountDao.findById(vm.getAccountId());

        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
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
        Host destinationHost = getDestinationHost(hostId, isRootAdmin);
        DataCenterDeployment plan = null;
        boolean deployOnGivenHost = false;
        if (destinationHost != null) {
            s_logger.debug("Destination Host to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            plan = new DataCenterDeployment(vm.getDataCenterId(), destinationHost.getPodId(), destinationHost.getClusterId(), destinationHost.getId(), null, null);
            if (!AllowDeployVmIfGivenHostFails.value()) {
                deployOnGivenHost = true;
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

            params = new HashMap<VirtualMachineProfile.Param, Object>();
            if (additionalParams != null) {
                params.putAll(additionalParams);
            }
            params.put(VirtualMachineProfile.Param.VmPassword, password);
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

    private Host getDestinationHost(Long hostId, boolean isRootAdmin) {
        Host destinationHost = null;
        if (hostId != null) {
            if (!isRootAdmin) {
                throw new PermissionDeniedException(
                        "Parameter " + ApiConstants.HOST_ID + " can only be specified by a Root Admin, permission denied");
            }
            destinationHost = _hostDao.findById(hostId);
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to deploy the VM, host id=" + hostId);
            }
        }
        return destinationHost;
    }

    @Override
    public UserVm destroyVm(long vmId, boolean expunge) throws ResourceUnavailableException, ConcurrentOperationException {
        // Account caller = CallContext.current().getCallingAccount();
        // Long userId = CallContext.current().getCallingUserId();
        Long userId = 2L;

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

        boolean status;
        State vmState = vm.getState();

        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            status = vmEntity.destroy(Long.toString(userId), expunge);
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
                ServiceOfferingVO offering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

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
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getBytesRead() + " Stored: " + vmDiskStat_lock.getCurrentBytesRead());
                                }
                                vmDiskStat_lock.setNetBytesRead(vmDiskStat_lock.getNetBytesRead() + vmDiskStat_lock.getCurrentBytesRead());
                            }
                            vmDiskStat_lock.setCurrentBytesRead(vmDiskStat.getBytesRead());
                            if (vmDiskStat_lock.getCurrentBytesWrite() > vmDiskStat.getBytesWrite()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Write # of Bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Host: " + host.getName()
                                    + " . VM: " + vmDiskStat.getVmName() + " Reported: " + vmDiskStat.getBytesWrite() + " Stored: "
                                    + vmDiskStat_lock.getCurrentBytesWrite());
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
                s_logger.warn("Unable to update vm disk statistics for vm: " + userVm.getId() + " from host: " + hostId, e);
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_EXPUNGE, eventDescription = "expunging Vm", async = true)
    public UserVm expungeVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
        Account caller = CallContext.current().getCallingAccount();
        Long userId = caller.getId();

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
        if (!_accountMgr.isAdmin(userId) && !AllowUserExpungeRecoverVm.valueIn(userId)) {
            throw new PermissionDeniedException("Expunging a vm can only be done by an Admin. Or when the allow.user.expunge.recover.vm key is set.");
        }

        _vmSnapshotMgr.deleteVMSnapshotsFromDB(vmId);

        boolean status;

        status = expunge(vm, userId, caller);
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

        ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
        }

        Long templateId = cmd.getTemplateId();

        if (!serviceOffering.isDynamic()) {
            for(String detail: cmd.getDetails().keySet()) {
                if(detail.equalsIgnoreCase(VmDetailConstants.CPU_NUMBER) || detail.equalsIgnoreCase(VmDetailConstants.CPU_SPEED) || detail.equalsIgnoreCase(VmDetailConstants.MEMORY)) {
                    throw new InvalidParameterValueException("cpuNumber or cpuSpeed or memory should not be specified for static service offering");
                }
            }
        }

        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Unable to use template " + templateId);
        }

        Long diskOfferingId = cmd.getDiskOfferingId();
        DiskOffering diskOffering = null;
        if (diskOfferingId != null) {
            diskOffering = _entityMgr.findById(DiskOffering.class, diskOfferingId);
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
            }
        }

        if (!zone.isLocalStorageEnabled()) {
            if (serviceOffering.isUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone is not configured to use local storage but service offering " + serviceOffering.getName() + " uses it");
            }
            if (diskOffering != null && diskOffering.isUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " uses it");
            }
        }

        String ipAddress = cmd.getIpAddress();
        String ip6Address = cmd.getIp6Address();
        String macAddress = cmd.getMacAddress();
        String name = cmd.getName();
        String displayName = cmd.getDisplayName();
        UserVm vm = null;
        IpAddresses addrs = new IpAddresses(ipAddress, ip6Address, macAddress);
        Long size = cmd.getSize();
        String group = cmd.getGroup();
        String userData = cmd.getUserData();
        String sshKeyPairName = cmd.getSSHKeyPairName();
        Boolean displayVm = cmd.isDisplayVm();
        String keyboard = cmd.getKeyboard();
        Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap = cmd.getDataDiskTemplateToDiskOfferingMap();
        Map<String, String> userVmOVFProperties = cmd.getVmOVFProperties();
        if (zone.getNetworkType() == NetworkType.Basic) {
            if (cmd.getNetworkIds() != null) {
                throw new InvalidParameterValueException("Can't specify network Ids in Basic zone");
            } else {
                vm = createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, getSecurityGroupIdList(cmd), owner, name, displayName, diskOfferingId,
                        size , group , cmd.getHypervisor(), cmd.getHttpMethod(), userData , sshKeyPairName , cmd.getIpToNetworkMap(), addrs, displayVm , keyboard , cmd.getAffinityGroupIdList(),
                        cmd.getDetails(), cmd.getCustomId(), cmd.getDhcpOptionsMap(),
                        dataDiskTemplateToDiskOfferingMap, userVmOVFProperties);
            }
        } else {
            if (zone.isSecurityGroupEnabled())  {
                vm = createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, cmd.getNetworkIds(), getSecurityGroupIdList(cmd), owner, name,
                        displayName, diskOfferingId, size, group, cmd.getHypervisor(), cmd.getHttpMethod(), userData, sshKeyPairName, cmd.getIpToNetworkMap(), addrs, displayVm, keyboard,
                        cmd.getAffinityGroupIdList(), cmd.getDetails(), cmd.getCustomId(), cmd.getDhcpOptionsMap(),
                        dataDiskTemplateToDiskOfferingMap, userVmOVFProperties);

            } else {
                if (cmd.getSecurityGroupIdList() != null && !cmd.getSecurityGroupIdList().isEmpty()) {
                    throw new InvalidParameterValueException("Can't create vm with security groups; security group feature is not enabled per zone");
                }
                vm = createAdvancedVirtualMachine(zone, serviceOffering, template, cmd.getNetworkIds(), owner, name, displayName, diskOfferingId, size, group,
                        cmd.getHypervisor(), cmd.getHttpMethod(), userData, sshKeyPairName, cmd.getIpToNetworkMap(), addrs, displayVm, keyboard, cmd.getAffinityGroupIdList(), cmd.getDetails(),
                        cmd.getCustomId(), cmd.getDhcpOptionsMap(), dataDiskTemplateToDiskOfferingMap, userVmOVFProperties);
            }
        }
        // check if this templateId has a child ISO
        List<VMTemplateVO> child_templates = _templateDao.listByParentTemplatetId(templateId);
        for (VMTemplateVO tmpl: child_templates){
            if (tmpl.getFormat() == Storage.ImageFormat.ISO){
                s_logger.info("MDOV trying to attach disk to the VM " + tmpl.getId() + " vmid=" + vm.getId());
                _tmplService.attachIso(tmpl.getId(), vm.getId());
            }
        }

        // Add extraConfig to user_vm_details table
        Account caller = CallContext.current().getCallingAccount();
        Long callerId = caller.getId();
        String extraConfig = cmd.getExtraConfig();
        if (StringUtils.isNotBlank(extraConfig) && EnableAdditionalVmConfig.valueIn(callerId) ) {
            addExtraConfig(vm, caller, extraConfig);
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
     * Persist extra configurations as details for VMware VMs
     */
    protected void persistExtraConfigVmware(String decodedUrl, UserVm vm) {
        String[] configDataArr = decodedUrl.split("\\r?\\n");
        for (String config: configDataArr) {
            String[] keyValue = config.split("=");
            try {
                userVmDetailsDao.addDetail(vm.getId(), keyValue[0], keyValue[1], true);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CloudRuntimeException("Issue occurred during parsing of:" + config);
            }
        }
    }

    /**
     * Persist extra configurations as details for hypervisors except Vmware
     */
    protected void persistExtraConfigNonVmware(String decodedUrl, UserVm vm) {
        String[] extraConfigs = decodedUrl.split("\n\n");
        for (String cfg : extraConfigs) {
            int i = 1;
            String[] cfgParts = cfg.split("\n");
            String extraConfigKey = ApiConstants.EXTRA_CONFIG;
            String extraConfigValue;
            if (cfgParts[0].matches("\\S+:$")) {
                extraConfigKey += "-" + cfgParts[0].substring(0,cfgParts[0].length() - 1);
                extraConfigValue = cfg.replace(cfgParts[0] + "\n", "");
            } else {
                extraConfigKey += "-" + String.valueOf(i);
                extraConfigValue = cfg;
            }
            userVmDetailsDao.addDetail(vm.getId(), extraConfigKey, extraConfigValue, true);
            i++;
        }
    }

    protected void addExtraConfig(UserVm vm, Account caller, String extraConfig) {
        String decodedUrl = decodeExtraConfig(extraConfig);
        HypervisorType hypervisorType = vm.getHypervisorType();
        if (hypervisorType == HypervisorType.VMware) {
            persistExtraConfigVmware(decodedUrl, vm);
        } else {
            persistExtraConfigNonVmware(decodedUrl, vm);
        }
    }

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
    public VirtualMachine vmStorageMigration(Long vmId, StoragePool destPool) {
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

        if (vm.getType() != VirtualMachine.Type.User) {
            // OffLineVmwareMigration: *WHY* ?
            throw new InvalidParameterValueException("can only do storage migration on user vm");
        }

        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        if (vols.size() > 1) {
            // OffLineVmwareMigration: data disks are not permitted, here!
            if (vols.size() > 1 &&
                    // OffLineVmwareMigration: allow multiple disks for vmware
                    !HypervisorType.VMware.equals(vm.getHypervisorType())) {
                throw new InvalidParameterValueException("Data disks attached to the vm, can not migrate. Need to detach data disks first");
            }
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("VM's disk cannot be migrated, please remove all the VM Snapshots for this VM");
        }

        checkDestinationHypervisorType(destPool, vm);

        _itMgr.storageMigration(vm.getUuid(), destPool);
        return _vmDao.findById(vm.getId());

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

    private boolean isVMUsingLocalStorage(VMInstanceVO vm) {
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
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is not XenServer/VMware/KVM/Ovm/Hyperv, cannot migrate this VM form hypervisor type " + vm.getHypervisorType());
            }
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        if (vm.getType().equals(VirtualMachine.Type.User) && vm.getHypervisorType().equals(HypervisorType.LXC)) {
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for User VM migration, we support XenServer/VMware/KVM/Ovm/Hyperv/Ovm3 only");
        }

        if (isVMUsingLocalStorage(vm)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is using Local Storage, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }

        // check if migrating to same host
        long srcHostId = vm.getHostId();
        if (destinationHost.getId() == srcHostId) {
            throw new InvalidParameterValueException("Cannot migrate VM, VM is already present on this host, please specify valid destination host to migrate the VM");
        }

        // check if host is UP
        if (destinationHost.getState() != com.cloud.host.Status.Up || destinationHost.getResourceState() != ResourceState.Enabled) {
            throw new InvalidParameterValueException("Cannot migrate VM, destination host is not in correct state, has status: " + destinationHost.getState() + ", state: "
                    + destinationHost.getResourceState());
        }

        if (vm.getType() != VirtualMachine.Type.User) {
            // for System VMs check that the destination host is within the same
            // cluster
            HostVO srcHost = _hostDao.findById(srcHostId);
            if (srcHost != null && srcHost.getClusterId() != null && destinationHost.getClusterId() != null) {
                if (srcHost.getClusterId().longValue() != destinationHost.getClusterId().longValue()) {
                    throw new InvalidParameterValueException("Cannot migrate the VM, destination host is not in the same cluster as current host of the VM");
                }
            }
        }

        if (dpdkHelper.isVMDpdkEnabled(vm.getId()) && !dpdkHelper.isHostDpdkEnabled(destinationHost.getId())) {
            throw new CloudRuntimeException("Cannot migrate VM, VM is DPDK enabled VM but destination host is not DPDK enabled");
        }

        checkHostsDedication(vm, srcHostId, destinationHost.getId());

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
        s_logger.debug("Checking if there are any ongoing snapshots volumes associated with VM with ID " + vmId);
        if (checkStatusOfVolumeSnapshots(vmId, null)) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on volume(s) attached to this VM, VM Migration is not permitted, please try again later.");
        }
        s_logger.debug("Found no ongoing snapshots on volumes associated with the vm with id " + vmId);

        UserVmVO uservm = _vmDao.findById(vmId);
        if (uservm != null) {
            collectVmDiskStatistics(uservm);
            collectVmNetworkStatistics(uservm);
        }
        _itMgr.migrate(vm.getUuid(), srcHostId, dest);
        VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
        if (vmInstance.getType().equals(VirtualMachine.Type.User)) {
            return _vmDao.findById(vmId);
        } else {
            return vmInstance;
        }
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
        ServiceOfferingVO deployPlanner = _offeringDao.findById(vm.getId(), vm.getServiceOfferingId());
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
                        ServiceOfferingVO destPlanner = _offeringDao.findById(vm.getId(), vmsDest.getServiceOfferingId());
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
        ServiceOfferingVO offering = _serviceOfferingDao.findByIdIncludingRemoved(offeringId);
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
            throw new InvalidParameterValueException("Unable to find the vm by id " + vmId);
        }

        // OfflineVmwareMigration: this would be it ;) if multiple paths exist: unify
        if (vm.getState() != State.Running) {
            // OfflineVmwareMigration: and not vmware
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            CloudRuntimeException ex = new CloudRuntimeException("VM is not Running, unable to migrate the vm with" + " specified id");
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }

        if(serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()) != null) {
            throw new InvalidParameterValueException("Live Migration of GPU enabled VM is not supported");
        }

        // OfflineVmwareMigration: this condition is to complicated. (already a method somewhere)
        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware) && !vm.getHypervisorType().equals(HypervisorType.KVM)
                && !vm.getHypervisorType().equals(HypervisorType.Ovm) && !vm.getHypervisorType().equals(HypervisorType.Hyperv)
                && !vm.getHypervisorType().equals(HypervisorType.Simulator)) {
            throw new InvalidParameterValueException("Unsupported hypervisor type for vm migration, we support" + " XenServer/VMware/KVM only");
        }

        long srcHostId = vm.getHostId();
        Host srcHost = _resourceMgr.getHost(srcHostId);

        if(srcHost == null ){
            throw new InvalidParameterValueException("Cannot migrate VM, there is not Host with id: " + srcHostId);
        }

        // Check if src and destination hosts are valid and migrating to same host
        if (destinationHost.getId() == srcHostId) {
            throw new InvalidParameterValueException("Cannot migrate VM, VM is already present on this host, please" + " specify valid destination host to migrate the VM");
        }

        // Check if the source and destination hosts are of the same type and support storage motion.
        if (!srcHost.getHypervisorType().equals(destinationHost.getHypervisorType())) {
            throw new CloudRuntimeException("The source and destination hosts are not of the same type and version. Source hypervisor type and version: " +
                    srcHost.getHypervisorType().toString() + " " + srcHost.getHypervisorVersion() + ", Destination hypervisor type and version: " +
                    destinationHost.getHypervisorType().toString() + " " + destinationHost.getHypervisorVersion());
        }

        String srcHostVersion = srcHost.getHypervisorVersion();
        String destinationHostVersion = destinationHost.getHypervisorVersion();

        if (HypervisorType.KVM.equals(srcHost.getHypervisorType())) {
            if (srcHostVersion == null) {
                srcHostVersion = "";
            }

            if (destinationHostVersion == null) {
                destinationHostVersion = "";
            }
        }

        if (!srcHostVersion.equals(destinationHostVersion)) {
            throw new CloudRuntimeException("The source and destination hosts are not of the same type and version. Source hypervisor type and version: " +
                    srcHost.getHypervisorType().toString() + " " + srcHost.getHypervisorVersion() + ", Destination hypervisor type and version: " +
                    destinationHost.getHypervisorType().toString() + " " + destinationHost.getHypervisorVersion());
        }

        HypervisorCapabilitiesVO capabilities = _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(srcHost.getHypervisorType(), srcHost.getHypervisorVersion());

        if (capabilities == null && HypervisorType.KVM.equals(srcHost.getHypervisorType())) {
            List<HypervisorCapabilitiesVO> lstHypervisorCapabilities = _hypervisorCapabilitiesDao.listAllByHypervisorType(HypervisorType.KVM);

            if (lstHypervisorCapabilities != null) {
                for (HypervisorCapabilitiesVO hypervisorCapabilities : lstHypervisorCapabilities) {
                    if (hypervisorCapabilities.isStorageMotionSupported()) {
                        capabilities = hypervisorCapabilities;

                        break;
                    }
                }
            }
        }

        if (!capabilities.isStorageMotionSupported()) {
            throw new CloudRuntimeException("Migration with storage isn't supported on hypervisor " + srcHost.getHypervisorType() + " of version " + srcHost.getHypervisorVersion());
        }

        // Check if destination host is up.
        if (destinationHost.getState() != com.cloud.host.Status.Up || destinationHost.getResourceState() != ResourceState.Enabled) {
            throw new CloudRuntimeException("Cannot migrate VM, destination host is not in correct state, has " + "status: " + destinationHost.getState() + ", state: "
                    + destinationHost.getResourceState());
        }

        // Check that Vm does not have VM Snapshots
        if (_vmSnapshotDao.findByVm(vmId).size() > 0) {
            throw new InvalidParameterValueException("VM with VM Snapshots cannot be migrated with storage, please remove all VM snapshots");
        }

        List<VolumeVO> vmVolumes = _volsDao.findUsableVolumesForInstance(vm.getId());
        Map<Long, Long> volToPoolObjectMap = new HashMap<Long, Long>();
        if (!isVMUsingLocalStorage(vm) && destinationHost.getClusterId().equals(srcHost.getClusterId())) {
            if (volumeToPool.isEmpty()) {
                // If the destination host is in the same cluster and volumes do not have to be migrated across pools
                // then fail the call. migrateVirtualMachine api should have been used.
                throw new InvalidParameterValueException("Migration of the vm " + vm + "from host " + srcHost + " to destination host " + destinationHost
                        + " doesn't involve migrating the volumes.");
            }
        }

        if (!volumeToPool.isEmpty()) {
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
                        throw new InvalidParameterValueException("There volume " + volume + " doesn't belong to " + "the virtual machine " + vm + " that has to be migrated");
                    }
                    volToPoolObjectMap.put(Long.valueOf(volume.getId()), Long.valueOf(pool.getId()));
                }
            }
        }

        // Check if all the volumes are in the correct state.
        for (VolumeVO volume : vmVolumes) {
            if (volume.getState() != Volume.State.Ready) {
                throw new CloudRuntimeException("Volume " + volume + " of the VM is not in Ready state. Cannot " + "migrate the vm with its volumes.");
            }
        }

        // Check max guest vm limit for the destinationHost.
        HostVO destinationHostVO = _hostDao.findById(destinationHost.getId());
        if (_capacityMgr.checkIfHostReachMaxGuestLimit(destinationHostVO)) {
            throw new VirtualMachineMigrationException("Host name: " + destinationHost.getName() + ", hostId: " + destinationHost.getId()
            + " already has max running vms (count includes system VMs). Cannot" + " migrate to this host");
        }

        checkHostsDedication(vm, srcHostId, destinationHost.getId());

        _itMgr.migrateWithStorage(vm.getUuid(), srcHostId, destinationHost.getId(), volToPoolObjectMap);
        return _vmDao.findById(vm.getId());
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

        if (newAccount.getState() == Account.State.disabled) {
            throw new InvalidParameterValueException("The new account owner " + cmd.getAccountName() + " is disabled.");
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
        final ServiceOfferingVO offering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId());

        //Remove vm from instance group
        removeInstanceFromInstanceGroup(cmd.getVmId());

        // VV 2: check if account/domain is with in resource limits to create a new vm
        resourceLimitCheck(newAccount, vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));

        // VV 3: check if volumes and primary storage space are with in resource limits
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.volume, _volsDao.findByInstance(cmd.getVmId()).size());
        Long totalVolumesSize = (long)0;
        for (VolumeVO volume : volumes) {
            totalVolumesSize += volume.getSize();
        }
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.primary_storage, totalVolumesSize);

        // VV 4: Check if new owner can use the vm template
        VirtualMachineTemplate template = _templateDao.findById(vm.getTemplateId());
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
                resourceCountIncrement(newAccount.getAccountId(), vm.isDisplayVm(), new Long(offering.getCpu()), new Long(offering.getRamSize()));

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
            _networkMgr.expungeNics(vmOldProfile);
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

            s_logger.debug("AssignVM: Basic zone, adding security groups no " + securityGroupIdList.size() + " to " + vm.getInstanceName());
        } else {
            if (zone.isSecurityGroupEnabled())  { // advanced zone with security groups
                // cleanup the old security groups
                _securityGroupMgr.removeInstanceFromGroups(cmd.getVmId());

                Set<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
                String requestedIPv4ForDefaultNic = null;
                String requestedIPv6ForDefaultNic = null;
                // if networkIdList is null and the first network of vm is shared network, then keep it if possible
                if (networkIdList == null || networkIdList.isEmpty()) {
                    NicVO defaultNicOld = _nicDao.findDefaultNicForVM(vm.getId());
                    if (defaultNicOld != null) {
                        NetworkVO defaultNetworkOld = _networkDao.findById(defaultNicOld.getNetworkId());
                        if (defaultNetworkOld != null && defaultNetworkOld.getGuestType() == Network.GuestType.Shared && defaultNetworkOld.getAclType() == ACLType.Domain) {
                            try {
                                _networkModel.checkNetworkPermissions(newAccount, defaultNetworkOld);
                                applicableNetworks.add(defaultNetworkOld);
                                requestedIPv4ForDefaultNic = defaultNicOld.getIPv4Address();
                                requestedIPv6ForDefaultNic = defaultNicOld.getIPv6Address();
                                s_logger.debug("AssignVM: use old shared network " + defaultNetworkOld.getName() + " with old ip " + requestedIPv4ForDefaultNic + " on default nic of vm:" + vm.getInstanceName());
                            } catch (PermissionDeniedException e) {
                                s_logger.debug("AssignVM: the shared network on old default nic can not be applied to new account");
                            }
                        }
                    }
                }
                // cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.expungeNics(vmOldProfile);

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
                        applicableNetworks.add(network);
                    }
                }

                // add the new nics
                LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();
                int toggle = 0;
                NetworkVO defaultNetwork = null;
                for (NetworkVO appNet : applicableNetworks) {
                    NicProfile defaultNic = new NicProfile();
                    if (toggle == 0) {
                        defaultNic.setDefaultNic(true);
                        defaultNic.setRequestedIPv4(requestedIPv4ForDefaultNic);
                        defaultNic.setRequestedIPv6(requestedIPv6ForDefaultNic);
                        defaultNetwork = appNet;
                        toggle++;
                    }
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
                Set<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
                // if networkIdList is null and the first network of vm is shared network, then keep it if possible
                if (networkIdList == null || networkIdList.isEmpty()) {
                    NicVO defaultNicOld = _nicDao.findDefaultNicForVM(vm.getId());
                    if (defaultNicOld != null) {
                        NetworkVO defaultNetworkOld = _networkDao.findById(defaultNicOld.getNetworkId());
                        if (defaultNetworkOld != null && defaultNetworkOld.getGuestType() == Network.GuestType.Shared && defaultNetworkOld.getAclType() == ACLType.Domain) {
                            try {
                                _networkModel.checkNetworkPermissions(newAccount, defaultNetworkOld);
                                applicableNetworks.add(defaultNetworkOld);
                            } catch (PermissionDeniedException e) {
                                s_logger.debug("AssignVM: the shared network on old default nic can not be applied to new account");
                            }
                        }
                    }
                }

                // cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.expungeNics(vmOldProfile);

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
                                    null, null, true, null, null);
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

                // add the new nics
                LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();
                int toggle = 0;
                for (NetworkVO appNet : applicableNetworks) {
                    NicProfile defaultNic = new NicProfile();
                    if (toggle == 0) {
                        defaultNic.setDefaultNic(true);
                        toggle++;
                    }
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

        Long userId = caller.getId();
        Account owner = _accountDao.findById(vm.getAccountId());
        _userDao.findById(userId);
        long vmId = vm.getId();
        boolean needRestart = false;

        // Input validation
        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
        }

        if (vm.getState() != VirtualMachine.State.Running && vm.getState() != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException("Vm " + vm.getUuid() + " currently in " + vm.getState() + " state, restore vm can only execute when VM in Running or Stopped");
        }

        if (vm.getState() == VirtualMachine.State.Running) {
            needRestart = true;
        }

        List<VolumeVO> rootVols = _volsDao.findByInstanceAndType(vmId, Volume.Type.ROOT);
        if (rootVols.isEmpty()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Can not find root volume for VM " + vm.getUuid());
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }
        if (rootVols.size() > 1) {
            InvalidParameterValueException ex = new InvalidParameterValueException("There are " + rootVols.size() + " root volumes for VM " + vm.getUuid());
            ex.addProxyObject(vm.getUuid(), "vmId");
            throw ex;
        }
        VolumeVO root = rootVols.get(0);
        if ( !Volume.State.Allocated.equals(root.getState()) || newTemplateId != null ){
            Long templateId = root.getTemplateId();
            boolean isISO = false;
            if (templateId == null) {
                // Assuming that for a vm deployed using ISO, template ID is set to NULL
                isISO = true;
                templateId = vm.getIsoId();
            }

            // If target VM has associated VM snapshots then don't allow restore of VM
            List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vmId);
            if (vmSnapshots.size() > 0) {
                throw new InvalidParameterValueException("Unable to restore VM, please remove VM snapshots before restoring VM");
            }

            VMTemplateVO template = null;
            //newTemplateId can be either template or ISO id. In the following snippet based on the vm deployment (from template or ISO) it is handled accordingly
            if (newTemplateId != null) {
                template = _templateDao.findById(newTemplateId);
                _accountMgr.checkAccess(caller, null, true, template);
                if (isISO) {
                    if (!template.getFormat().equals(ImageFormat.ISO)) {
                        throw new InvalidParameterValueException("Invalid ISO id provided to restore the VM ");
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

            /* If new template/ISO is provided allocate a new volume from new template/ISO otherwise allocate new volume from original template/ISO */
            Volume newVol = null;
            if (newTemplateId != null) {
                if (isISO) {
                    newVol = volumeMgr.allocateDuplicateVolume(root, null);
                    vm.setIsoId(newTemplateId);
                    vm.setGuestOSId(template.getGuestOSId());
                    vm.setTemplateId(newTemplateId);
                    _vmDao.update(vmId, vm);
                } else {
                    newVol = volumeMgr.allocateDuplicateVolume(root, newTemplateId);
                    vm.setGuestOSId(template.getGuestOSId());
                    vm.setTemplateId(newTemplateId);
                    _vmDao.update(vmId, vm);
                }
            } else {
                newVol = volumeMgr.allocateDuplicateVolume(root, null);
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

            Map<VirtualMachineProfile.Param, Object> params = null;
            String password = null;

            if (template.isEnablePassword()) {
                password = _mgr.generateRandomPassword();
                boolean result = resetVMPasswordInternal(vmId, password);
                if (!result) {
                    throw new CloudRuntimeException("VM reset is completed but failed to reset password for the virtual machine ");
                }
            }

            if (needRestart) {
                try {
                    if (vm.getDetail(VmDetailConstants.PASSWORD) != null) {
                        params = new HashMap<VirtualMachineProfile.Param, Object>();
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
        }

        s_logger.debug("Restore VM " + vmId + " done successfully");
        return vm;

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
        UserVmVO vm = _vmDao.findById(profile.getId());
        if (vm != null && vm.getState() == State.Stopping) {
            collectVmDiskStatistics(vm);
            collectVmNetworkStatistics(vm);
        }
    }

    private void encryptAndStorePassword(UserVmVO vm, String password) {
        String sshPublicKey = vm.getDetail(VmDetailConstants.SSH_PUBLIC_KEY);
        if (sshPublicKey != null && !sshPublicKey.equals("") && password != null && !password.equals("saved_password")) {
            if (!sshPublicKey.startsWith("ssh-rsa")) {
                s_logger.warn("Only RSA public keys can be used to encrypt a vm password.");
                return;
            }
            String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(sshPublicKey, password);
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
        if (StringUtils.isEmpty(existingVmRootDiskController) && !StringUtils.isEmpty(rootDiskController)) {
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
        return new ConfigKey<?>[] {EnableDynamicallyScaleVm, AllowUserExpungeRecoverVm, VmIpFetchWaitInterval, VmIpFetchTrialMax, VmIpFetchThreadPoolMax,
            VmIpFetchTaskWorkers, AllowDeployVmIfGivenHostFails, EnableAdditionalVmConfig, DisplayVMOVFProperties};
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
            if (volume.getInstanceId() == null || vmId != volume.getInstanceId()) {
                sb.append(volume.toString() + "; ");
            }
        }

        if (!StringUtils.isEmpty(sb.toString())) {
            throw new InvalidParameterValueException("The following supplied volumes are not attached to the VM: " + sb.toString());
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

            Volume detachResult = _volumeService.detachVolumeViaDestroyVM(volume.getInstanceId(), volume.getId());

            if (detachResult == null) {
                s_logger.error("DestroyVM remove volume - failed to detach and delete volume " + volume.getInstanceId() + " from instance " + volume.getId());
            }
        }
    }

    private void deleteVolumesFromVm(List<VolumeVO> volumes) {

        for (VolumeVO volume : volumes) {

            boolean deleteResult = _volumeService.deleteVolume(volume.getId(), CallContext.current().getCallingAccount());

            if (!deleteResult) {
                s_logger.error("DestroyVM remove volume - failed to delete volume " + volume.getInstanceId() + " from instance " + volume.getId());
            }
        }
    }
}
