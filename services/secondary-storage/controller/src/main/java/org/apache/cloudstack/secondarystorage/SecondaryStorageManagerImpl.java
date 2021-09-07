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
package org.apache.cloudstack.secondarystorage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreManager;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageSetupAnswer;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.manager.Commands;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.configuration.ZoneConfig;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.info.RunningHostInfoAgregator;
import com.cloud.info.RunningHostInfoAgregator.ZoneHostInfo;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.secondary.SecStorageVmAlertEventArgs;
import com.cloud.storage.secondary.SecondaryStorageListener;
import com.cloud.storage.secondary.SecondaryStorageVmAllocator;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.events.SubscriptionMgr;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.SystemVmLoadScanHandler;
import com.cloud.vm.SystemVmLoadScanner;
import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.commons.lang3.BooleanUtils;

/**
* Class to manage secondary storages. <br><br>
* Possible secondary storage VM state transition cases:<br>
*    - Creating -> Destroyed<br>
*    - Creating -> Stopped -> Starting -> Running<br>
*    - HA -> Stopped -> Starting -> Running<br>
*    - Migrating -> Running    (if previous state is Running before it enters into Migrating state<br>
*    - Migrating -> Stopped    (if previous state is not Running before it enters into Migrating state)<br>
*    - Running -> HA           (if agent lost connection)<br>
*    - Stopped -> Destroyed<br><br>
*
* <b>Creating</b> state indicates of record creating and IP address allocation are ready, it is a transient state which will soon be switching towards <b>Running</b> if everything goes well.<br><br>
* <b>Stopped</b> state indicates the readiness of being able to start (has storage and IP resources allocated).<br><br>
* <b>Starting</b> state can only be entered from <b>Stopped</b> states.<br><br><br>
*
* <b>Starting</b>, <b>HA</b>, <b>Migrating</b>, <b>Creating</b> and <b>Running</b> states are all counted as <b>Open</b> for available capacity calculation  because sooner or later, it will be driven into <b>Running</b> state.
*/
public class SecondaryStorageManagerImpl extends ManagerBase implements SecondaryStorageVmManager, VirtualMachineGuru, SystemVmLoadScanHandler<Long>,
        ResourceStateAdapter, Configurable {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL_IN_MILLISECONDS = 30000;
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC_IN_SECONDS = 180;
    private static final int STARTUP_DELAY_IN_MILLISECONDS = 60000;

    private int _mgmtPort = 8250;

    private List<SecondaryStorageVmAllocator> _ssVmAllocators;

    @Inject
    protected SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    protected StorageNetworkManager _sNwMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private StoragePoolHostDao _storagePoolHostDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    protected NetworkOrchestrationService _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected SnapshotDao _snapshotDao;
    private SecondaryStorageListener _listener;

    private ServiceOfferingVO _serviceOffering;

    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    private ServiceOfferingDao _offeringDao;
    @Inject
    private AccountService _accountMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    UserVmDetailsDao _vmDetailsDao;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected IPAddressDao _ipAddressDao = null;
    @Inject
    protected RulesManager _rulesMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject
    UploadDao _uploadDao;

    @Inject
    KeystoreManager _keystoreMgr;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    ImageStoreDao _imageStoreDao;
    @Inject
    TemplateDataStoreDao _tmplStoreDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    private IndirectAgentLB indirectAgentLB;

    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL_IN_MILLISECONDS;
    private int _secStorageVmMtuSize;

    private String _instance;
    private boolean _useSSlCopy;
    private String _httpProxy;
    private String _allowedInternalSites;
    protected long _nodeId = ManagementServerNode.getManagementServerId();

    private SystemVmLoadScanner<Long> _loadScanner;
    private Map<Long, ZoneHostInfo> _zoneHostInfoMap;

    private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());

    static final ConfigKey<String> NTPServerConfig = new ConfigKey<String>(String.class, "ntp.server.list", "Advanced", null,
            "Comma separated list of NTP servers to configure in Secondary storage VM", true, ConfigKey.Scope.Global, null);

    static final ConfigKey<Integer> MaxNumberOfSsvmsForMigration = new ConfigKey<Integer>("Advanced", Integer.class, "max.ssvm.count", "5",
            "Number of additional SSVMs to handle migration of data objects concurrently", true, ConfigKey.Scope.Global);

    public SecondaryStorageManagerImpl() {
    }

    @Override
    public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId) {
        try {
            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
            _itMgr.advanceStart(secStorageVm.getUuid(), null, null);
            return _secStorageVmDao.findById(secStorageVm.getId());
        } catch (ConcurrentOperationException | InsufficientCapacityException | OperationTimedoutException | ResourceUnavailableException e) {
            s_logger.warn(String.format("Unable to start secondary storage VM [%s] due to [%s].", secStorageVmId, e.getMessage()), e);
            return null;
        }
    }

    SecondaryStorageVmVO getSSVMfromHost(HostVO ssAHost) {
        if (ssAHost.getType() == Host.Type.SecondaryStorageVM) {
            return _secStorageVmDao.findByInstanceName(ssAHost.getName());
        }
        return null;
    }

    @Override
    public boolean generateSetupCommand(Long ssHostId) {
        HostVO cssHost = _hostDao.findById(ssHostId);
        Long zoneId = cssHost.getDataCenterId();
        if (cssHost.getType() == Host.Type.SecondaryStorageVM) {
            String hostName = cssHost.getName();

            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(hostName);
            if (secStorageVm == null) {
                s_logger.warn(String.format("Secondary storage VM [%s] does not exist.", hostName));
                return false;
            }

            List<DataStore> ssStores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));
            for (DataStore ssStore : ssStores) {
                if (!(ssStore.getTO() instanceof NfsTO)) {
                    continue;
                }
                String secUrl = ssStore.getUri();
                SecStorageSetupCommand setupCmd = null;
                if (!_useSSlCopy) {
                    setupCmd = new SecStorageSetupCommand(ssStore.getTO(), secUrl, null);
                } else {
                    KeystoreManager.Certificates certs = _keystoreMgr.getCertificates(ConsoleProxyManager.CERTIFICATE_NAME);
                    setupCmd = new SecStorageSetupCommand(ssStore.getTO(), secUrl, certs);
                }

                String nfsVersion = imageStoreDetailsUtil.getNfsVersion(ssStore.getId());
                setupCmd.setNfsVersion(nfsVersion);

                String postUploadKey = _configDao.getValue(Config.SSVMPSK.key());
                setupCmd.setPostUploadKey(postUploadKey);

                Answer answer = _agentMgr.easySend(ssHostId, setupCmd);
                if (answer != null && answer.getResult()) {
                    SecStorageSetupAnswer an = (SecStorageSetupAnswer)answer;
                    if (an.get_dir() != null) {
                        ImageStoreVO svo = _imageStoreDao.findById(ssStore.getId());
                        svo.setParent(an.get_dir());
                        _imageStoreDao.update(ssStore.getId(), svo);
                    }

                    s_logger.debug(String.format("Successfully programmed secondary storage [%s] in secondary storage VM [%s].", ssStore.getName(), secStorageVm.getInstanceName()));
                } else {
                    s_logger.debug(String.format("Unable to program secondary storage [%s] in secondary storage VM [%s] due to [%s].", ssStore.getName(), secStorageVm.getInstanceName(), answer == null ? "null answer" : answer.getDetails()));
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean generateVMSetupCommand(Long ssAHostId) {
        HostVO ssAHost = _hostDao.findById(ssAHostId);
        if (ssAHost.getType() != Host.Type.SecondaryStorageVM) {
            return false;
        }
        String ssvmName = ssAHost.getName();
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(ssvmName);
        if (secStorageVm == null) {
            s_logger.warn(String.format("Secondary storage VM [%s] does not exist.", ssvmName));
            return false;
        }

        SecStorageVMSetupCommand setupCmd = new SecStorageVMSetupCommand();
        if (_allowedInternalSites != null) {
            List<String> allowedCidrs = new ArrayList<>();
            String[] cidrs = _allowedInternalSites.split(",");
            for (String cidr : cidrs) {
                if (NetUtils.isValidIp4Cidr(cidr) || NetUtils.isValidIp4(cidr) || !cidr.startsWith("0.0.0.0")) {
                    allowedCidrs.add(cidr);
                }
            }
            setupCmd.setAllowedInternalSites(allowedCidrs.toArray(new String[allowedCidrs.size()]));
        }
        String copyPasswd = _configDao.getValue("secstorage.copy.password");
        setupCmd.setCopyPassword(copyPasswd);
        setupCmd.setCopyUserName(TemplateConstants.DEFAULT_HTTP_AUTH_USER);

        Answer answer = _agentMgr.easySend(ssAHostId, setupCmd);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Successfully set HTTP auth into secondary storage VM [%s].", ssvmName));
            }
            return true;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Failed to set HTTP auth into secondary storage VM [%s] due to [%s].", ssvmName, answer == null ? "answer null" : answer.getDetails()));
            }
            return false;
        }
    }

    @Override
    public Pair<HostVO, SecondaryStorageVmVO> assignSecStorageVm(long zoneId, Command cmd) {
        return null;
    }

    @Override
    public boolean generateFirewallConfiguration(Long ssAHostId) {
        if (ssAHostId == null) {
            return true;
        }
        HostVO ssAHost = _hostDao.findById(ssAHostId);
        String hostName = ssAHost.getName();

        SecondaryStorageVmVO thisSecStorageVm = _secStorageVmDao.findByInstanceName(hostName);

        if (thisSecStorageVm == null) {
            s_logger.warn(String.format("Secondary storage VM [%s] does not exist.", hostName));
            return false;
        }

        String copyPort = _useSSlCopy ? "443" : Integer.toString(TemplateConstants.DEFAULT_TMPLT_COPY_PORT);
        SecStorageFirewallCfgCommand thiscpc = new SecStorageFirewallCfgCommand(true);
        thiscpc.addPortConfig(thisSecStorageVm.getPublicIpAddress(), copyPort, true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);

        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        sc.and(sc.entity().getStatus(), Op.IN, Status.Up, Status.Connecting);
        List<HostVO> ssvms = sc.list();
        for (HostVO ssvm : ssvms) {
            if (ssvm.getId() == ssAHostId) {
                continue;
            }

            hostName = ssvm.getName();
            Answer answer = _agentMgr.easySend(ssvm.getId(), thiscpc);
            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Successfully created firewall rules into secondary storage VM [%s].", hostName));
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Failed to create firewall rules into secondary storage VM [%s].", hostName));
                }
                return false;
            }
        }

        SecStorageFirewallCfgCommand allSSVMIpList = new SecStorageFirewallCfgCommand(false);
        for (HostVO ssvm : ssvms) {
            if (ssvm.getId() == ssAHostId) {
                continue;
            }
            allSSVMIpList.addPortConfig(ssvm.getPublicIpAddress(), copyPort, true, TemplateConstants.DEFAULT_TMPLT_COPY_INTF);
        }

        hostName = thisSecStorageVm.getHostName();

        Answer answer = _agentMgr.easySend(ssAHostId, allSSVMIpList);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Successfully created firewall rules into secondary storage VM [%s].", hostName));
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Failed to create firewall rules into secondary storage VM [%s] due to [%s].", hostName, answer == null ? "answer null" : answer.getDetails()));
            }
            return false;
        }

        return true;

    }

    protected boolean isSecondaryStorageVmRequired(long dcId) {
        DataCenterVO dc = _dcDao.findById(dcId);
        _dcDao.loadDetails(dc);
        String ssvmReq = dc.getDetail(ZoneConfig.EnableSecStorageVm.key());
        if (ssvmReq != null) {
            return Boolean.parseBoolean(ssvmReq);
        }
        return true;
    }

    public SecondaryStorageVmVO startNew(long dataCenterId, SecondaryStorageVm.Role role) {

        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Secondary storage VM not required in zone [%s] account to zone config.", dataCenterId));
            }
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Assign secondary storage VM from a newly started instance for request from data center [%s].", dataCenterId));
        }

        Map<String, Object> context = createSecStorageVmInstance(dataCenterId, role);

        long secStorageVmId = (Long)context.get("secStorageVmId");
        if (secStorageVmId == 0) {
            s_logger.debug(String.format("Creating secondary storage VM instance failed on data center [%s].", dataCenterId));
            return null;
        }

        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);

        if (secStorageVm != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATED, dataCenterId, secStorageVmId, secStorageVm, null));
            return secStorageVm;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Unable to allocate secondary storage VM [%s] due to it was not found on database.", secStorageVmId));
            }
            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE, dataCenterId, secStorageVmId, null, "Unable to allocate storage"));
        }
        return null;
    }

    /**
     * Get the default network for the secondary storage VM, based on the zone it is in. Delegates to
     * either {@link #getDefaultNetworkForZone(DataCenter)} or {@link #getDefaultNetworkForAdvancedSGZone(DataCenter)},
     * depending on the zone network type and whether or not security groups are enabled in the zone.
     * @param dc - The zone (DataCenter) of the secondary storage VM.
     * @return The default network for use with the secondary storage VM.
     */
    protected NetworkVO getDefaultNetworkForCreation(DataCenter dc) {
        if (dc.getNetworkType() == NetworkType.Advanced) {
            return getDefaultNetworkForAdvancedZone(dc);
        } else {
            return getDefaultNetworkForBasicZone(dc);
        }
    }

    /**
     * Get default network for a secondary storage VM starting up in an advanced zone. If the zone
     * is security group-enabled, the first network found that supports SG services is returned.
     * If the zone is not SG-enabled, the Public network is returned.
     * @param dc - The zone.
     * @return The selected default network.
     * @throws CloudRuntimeException - If the zone is not a valid choice or a network couldn't be found.
     */
    protected NetworkVO getDefaultNetworkForAdvancedZone(DataCenter dc) {
        if (dc.getNetworkType() != NetworkType.Advanced) {
            throw new CloudRuntimeException(String.format("%s is not advanced.", dc.toString()));
        }

        if (dc.isSecurityGroupEnabled()) {
            List<NetworkVO> networks = _networkDao.listByZoneSecurityGroup(dc.getId());
            if (CollectionUtils.isEmpty(networks)) {
                throw new CloudRuntimeException(String.format("Can not found security enabled network in SG %s.", dc.toString()));
            }

            return networks.get(0);
        }
        else {
            TrafficType defaultTrafficType = TrafficType.Public;
            List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dc.getId(), defaultTrafficType);

            int networksSize = defaultNetworks.size();
            if (networksSize != 1) {
                throw new CloudRuntimeException(String.format("Found [%s] networks of type [%s] when expect to find 1.", networksSize, defaultTrafficType));
            }

            return defaultNetworks.get(0);
        }
    }

    /**
     * Get default network for secondary storage VM for starting up in a basic zone. Basic zones select
     * the Guest network whether or not the zone is SG-enabled.
     * @param dc - The zone.
     * @return The default network according to the zone's network selection rules.
     * @throws CloudRuntimeException - If the zone is not a valid choice or a network couldn't be found.
     */
    protected NetworkVO getDefaultNetworkForBasicZone(DataCenter dc) {
        if (dc.getNetworkType() != NetworkType.Basic) {
            throw new CloudRuntimeException(String.format("%s is not basic.", dc.toString()));
        }

        TrafficType defaultTrafficType = TrafficType.Guest;
        List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dc.getId(), defaultTrafficType);

        int networksSize = defaultNetworks.size();
        if (networksSize != 1) {
            throw new CloudRuntimeException(String.format("Found [%s] networks of type [%s] when expect to find 1.", networksSize, defaultTrafficType));
        }

        return defaultNetworks.get(0);
    }

    protected Map<String, Object> createSecStorageVmInstance(long dataCenterId, SecondaryStorageVm.Role role) {
        DataStore secStore = _dataStoreMgr.getImageStoreWithFreeCapacity(dataCenterId);
        if (secStore == null) {
            String msg = String.format("No secondary storage available in zone %s, cannot create secondary storage VM.", dataCenterId);
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        long id = _secStorageVmDao.getNextInSequence(Long.class, "id");
        String name = VirtualMachineName.getSystemVmName(id, _instance, "s").intern();
        Account systemAcct = _accountMgr.getSystemAccount();

        DataCenterDeployment plan = new DataCenterDeployment(dataCenterId);
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        NetworkVO defaultNetwork = getDefaultNetworkForCreation(dc);

        List<? extends NetworkOffering> offerings = null;
        if (_sNwMgr.isStorageIpRangeAvailable(dataCenterId)) {
            offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork, NetworkOffering.SystemManagementNetwork, NetworkOffering.SystemStorageNetwork);
        } else {
            offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork, NetworkOffering.SystemManagementNetwork);
        }
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);
        try {
            networks.put(_networkMgr.setupNetwork(systemAcct, _networkOfferingDao.findById(defaultNetwork.getNetworkOfferingId()), plan, null, null, false).get(0),
                    new ArrayList<>(Arrays.asList(defaultNic)));
            for (NetworkOffering offering : offerings) {
                networks.put(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false).get(0), new ArrayList<>());
            }
        } catch (ConcurrentOperationException e) {
            s_logger.error(String.format("Unable to setup networks on %s due [%s].", dc.toString(), e.getMessage()), e);
            return new HashMap<>();
        }

        HypervisorType availableHypervisor = _resourceMgr.getAvailableHypervisor(dataCenterId);
        VMTemplateVO template = _templateDao.findSystemVMReadyTemplate(dataCenterId, availableHypervisor);
        if (template == null) {
            throw new CloudRuntimeException(String.format("Unable to find the system templates or it was not downloaded in %s.", dc.toString()));
        }

        ServiceOfferingVO serviceOffering = _serviceOffering;
        if (serviceOffering == null) {
            serviceOffering = _offeringDao.findDefaultSystemOffering(ServiceOffering.ssvmDefaultOffUniqueName, ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dataCenterId));
        }
        SecondaryStorageVmVO secStorageVm =
            new SecondaryStorageVmVO(id, serviceOffering.getId(), name, template.getId(), template.getHypervisorType(), template.getGuestOSId(), dataCenterId,
                systemAcct.getDomainId(), systemAcct.getId(), _accountMgr.getSystemUser().getId(), role, serviceOffering.isOfferHA());
        secStorageVm.setDynamicallyScalable(template.isDynamicallyScalable());
        secStorageVm = _secStorageVmDao.persist(secStorageVm);
        try {
            _itMgr.allocate(name, template, serviceOffering, networks, plan, null);
            secStorageVm = _secStorageVmDao.findById(secStorageVm.getId());
        } catch (InsufficientCapacityException e) {
            String errorMessage = String.format("Unable to allocate secondary storage VM [%s] due to [%s].", name, e.getMessage());
            s_logger.warn(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("secStorageVmId", secStorageVm.getId());
        return context;
    }

    private SecondaryStorageVmAllocator getCurrentAllocator() {
        if (_ssVmAllocators.size() > 0) {
            return _ssVmAllocators.get(0);
        }

        return null;
    }

    protected String connect(String ipAddress, int port) {
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromRunningPool(long dataCenterId, SecondaryStorageVm.Role role) {
        s_logger.debug(String.format("Assign secondary storage VM from running pool for request from zone [%s].", dataCenterId));

        SecondaryStorageVmAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<SecondaryStorageVmVO> runningList = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Running);
        if (CollectionUtils.isNotEmpty(runningList)) {
            s_logger.debug(String.format("Running secondary storage VM pool size [%s].", runningList.size()));
            for (SecondaryStorageVmVO secStorageVm : runningList) {
                s_logger.debug(String.format("Running secondary storage %s.", secStorageVm.toString()));
            }

            Map<Long, Integer> loadInfo = new HashMap<>();

            return allocator.allocSecondaryStorageVm(runningList, loadInfo, dataCenterId);
        } else {
            s_logger.debug(String.format("There is no running secondary storage VM right now in the zone [%s].", dataCenterId));
        }
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromStoppedPool(long dataCenterId, SecondaryStorageVm.Role role) {
        List<SecondaryStorageVmVO> secondaryStorageVms = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Starting, State.Stopped, State.Migrating);
        if (CollectionUtils.isNotEmpty(secondaryStorageVms)) {
            return secondaryStorageVms.get(0);
        }

        return null;
    }

    public void allocCapacity(long dataCenterId, SecondaryStorageVm.Role role) {
        s_logger.debug(String.format("Allocate secondary storage VM standby capacity for zone [%s].", dataCenterId));

        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Secondary storage VM not required in zone [%s] according to zone config.", dataCenterId));
            }
            return;
        }
        SecondaryStorageVmVO secStorageVm = null;
        String errorString = null;
        try {
            boolean secStorageVmFromStoppedPool = false;
            secStorageVm = assignSecStorageVmFromStoppedPool(dataCenterId, role);
            if (secStorageVm == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("No stopped secondary storage VM is available, need to allocate a new secondary storage VM.");
                }

                if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC_IN_SECONDS)) {
                    try {
                        secStorageVm = startNew(dataCenterId, role);
                    } finally {
                        _allocLock.unlock();
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to acquire synchronization lock for secondary storage VM allocation, wait for next scan.");
                    }
                    return;
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info(String.format("Found a stopped secondary storage %s, starting it.", secStorageVm.toString()));
                }
                secStorageVmFromStoppedPool = true;
            }

            if (secStorageVm != null) {
                long secStorageVmId = secStorageVm.getId();
                GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVmId));
                try {
                    if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC_IN_SECONDS)) {
                        try {
                            secStorageVm = startSecStorageVm(secStorageVmId);
                        } finally {
                            secStorageVmLock.unlock();
                        }
                    } else {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info(String.format("Unable to acquire synchronization lock for starting secondary storage %s.", secStorageVm.toString()));
                        }
                        return;
                    }
                } finally {
                    secStorageVmLock.releaseRef();
                }

                if (secStorageVm == null) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info(String.format("Unable to start secondary storage VM [%s] for standby capacity, it will be recycled and will start a new one.", secStorageVmId));
                    }

                    if (secStorageVmFromStoppedPool) {
                        destroySecStorageVm(secStorageVmId);
                    }
                } else {
                    SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                            new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_UP, dataCenterId, secStorageVmId, secStorageVm, null));
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info(String.format("Secondary storage %s was started.", secStorageVm.toString()));
                    }
                }
            }
        } catch (Exception e) {
            errorString = String.format("Unable to allocate capacity on zone [%s] due to [%s].", dataCenterId, errorString);
            throw e;
        } finally {
            if(secStorageVm == null || secStorageVm.getState() != State.Running)
                SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                        new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE, dataCenterId, 0l, null, errorString));
        }
    }

    public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
        List <HostVO> hosts = _hostDao.listByDataCenterId(dataCenterId);
        if (CollectionUtils.isEmpty(hosts)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Zone " + dataCenterId + " has no host available which is enabled and in Up state");
            }
            return false;
        }
        ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
        if (zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK) != 0) {
            VMTemplateVO template = _templateDao.findSystemVMReadyTemplate(dataCenterId, HypervisorType.Any);
            if (template == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("System VM template is not ready at zone [%s], wait until it is ready to launch secondary storage VM.", dataCenterId));
                }
                return false;
            }

            List<DataStore> stores = _dataStoreMgr.getImageStoresByScopeExcludingReadOnly(new ZoneScope(dataCenterId));
            if (CollectionUtils.isEmpty(stores)) {
                s_logger.debug(String.format("No image store added in zone [%s], wait until it is ready to launch secondary storage VM.", dataCenterId));
                return false;
            }

            if (!template.isDirectDownload() && templateMgr.getImageStore(dataCenterId, template.getId()) == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("No secondary storage available in zone [%s], wait until it is ready to launch secondary storage VM.", dataCenterId));
                }
                return false;
            }

            boolean useLocalStorage = BooleanUtils.toBoolean(ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dataCenterId));
            List<Pair<Long, Integer>> storagePoolHostInfos = _storagePoolHostDao.getDatacenterStoragePoolHostInfo(dataCenterId, !useLocalStorage);
            if (CollectionUtils.isNotEmpty(storagePoolHostInfos) && storagePoolHostInfos.get(0).second() > 0) {
                return true;
            } else {
                if (s_logger.isDebugEnabled()) {
                    String configKey = ConfigurationManagerImpl.SystemVMUseLocalStorage.key();
                    s_logger.debug(String.format("Primary storage is not ready, wait until it is ready to launch secondary storage VM. {\"dataCenterId\": %s, \"%s\": \"%s\"}. "
                        + "If you want to use local storage to start secondary storage VM, you need to set the configuration [%s] to \"true\".", dataCenterId, configKey, useLocalStorage, configKey));
                }
            }

        }
        return false;
    }

    private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
        Date cutTime = DateUtil.currentGMTTime();
        List<RunningHostCountInfo> runningHostCountInfos = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - ClusterManager.HeartbeatThreshold.value()));

        RunningHostInfoAgregator aggregator = new RunningHostInfoAgregator();
        if (CollectionUtils.isNotEmpty(runningHostCountInfos)) {
            for (RunningHostCountInfo countInfo : runningHostCountInfos) {
                aggregator.aggregate(countInfo);
            }
        }

        return aggregator.getZoneHostInfoMap();
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start secondary storage vm manager");
        }

        return true;
    }

    @Override
    public boolean stop() {
        _loadScanner.stop();
        _allocLock.releaseRef();
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Start configuring secondary storage vm manager : " + name);
        }

        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        _secStorageVmMtuSize = NumbersUtil.parseInt(configs.get("secstorage.vm.mtu.size"), DEFAULT_SS_VM_MTUSIZE);
        boolean _useServiceVM = BooleanUtils.toBoolean(_configDao.getValue("secondary.storage.vm"));
        _useSSlCopy = BooleanUtils.toBoolean(_configDao.getValue("secstorage.encrypt.copy"));

        String ssvmUrlDomain = _configDao.getValue("secstorage.ssl.cert.domain");
        if(_useSSlCopy && org.apache.commons.lang3.StringUtils.isEmpty(ssvmUrlDomain)){
            s_logger.warn("Empty secondary storage url domain, explicitly disabling SSL");
            _useSSlCopy = false;
        }

        _allowedInternalSites = _configDao.getValue("secstorage.allowed.internal.sites");

        String value = configs.get("secstorage.capacityscan.interval");
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL_IN_MILLISECONDS);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        Map<String, String> agentMgrConfigs = _configDao.getConfiguration("AgentManager", params);

        value = agentMgrConfigs.get("port");
        _mgmtPort = NumbersUtil.parseInt(value, 8250);

        _listener = new SecondaryStorageListener(this);
        _agentMgr.registerForHostEvents(_listener, true, false, true);

        _itMgr.registerGuru(VirtualMachine.Type.SecondaryStorageVm, this);

        String configKey = Config.SecondaryStorageServiceOffering.key();
        String ssvmSrvcOffIdStr = configs.get(configKey);
        if (ssvmSrvcOffIdStr != null) {
            _serviceOffering = _offeringDao.findByUuid(ssvmSrvcOffIdStr);
            if (_serviceOffering == null) {
                try {
                    s_logger.debug(String.format("Unable to find a service offering by the UUID for secondary storage VM with the value [%s] set in the configuration [%s]. Trying to find by the ID.", ssvmSrvcOffIdStr, configKey));
                    _serviceOffering = _offeringDao.findById(Long.parseLong(ssvmSrvcOffIdStr));

                    if (_serviceOffering == null) {
                        s_logger.info(String.format("Unable to find a service offering by the UUID or ID for secondary storage VM with the value [%s] set in the configuration [%s]", ssvmSrvcOffIdStr, configKey));
                    }
                } catch (NumberFormatException ex) {
                    s_logger.warn(String.format("Unable to find a service offering by the ID for secondary storage VM with the value [%s] set in the configuration [%s]. The value is not a valid integer number. Error: [%s].", ssvmSrvcOffIdStr, configKey, ex.getMessage()), ex);
                }
            }
        }

        if (_serviceOffering == null || !_serviceOffering.isSystemUse()) {
            int ramSize = NumbersUtil.parseInt(_configDao.getValue("ssvm.ram.size"), DEFAULT_SS_VM_RAMSIZE);
            int cpuFreq = NumbersUtil.parseInt(_configDao.getValue("ssvm.cpu.mhz"), DEFAULT_SS_VM_CPUMHZ);
            List<ServiceOfferingVO> offerings = _offeringDao.createSystemServiceOfferings("System Offering For Secondary Storage VM",
                    ServiceOffering.ssvmDefaultOffUniqueName, 1, ramSize, cpuFreq, null, null, false, null,
                    Storage.ProvisioningType.THIN, true, null, true, VirtualMachine.Type.SecondaryStorageVm, true);

            if (offerings == null || offerings.size() < 2) {
                String msg = "Unable to set a service offering for secondary storage VM. Verify if it was removed.";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        }

        if (_useServiceVM) {
            _loadScanner = new SystemVmLoadScanner<>(this);
            _loadScanner.initScan(STARTUP_DELAY_IN_MILLISECONDS, _capacityScanInterval);
        }

        _httpProxy = configs.get(Config.SecStorageProxy.key());
        if (_httpProxy != null) {
            boolean valid = true;
            String errMsg = null;
            try {
                URI uri = new URI(_httpProxy);
                String uriScheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(uriScheme)) {
                    errMsg = String.format("[%s] is not supported, it only supports HTTP proxy", uriScheme);
                    valid = false;
                } else if (uri.getHost() == null) {
                    errMsg = "host can not be null";
                    valid = false;
                } else if (uri.getPort() == -1) {
                    _httpProxy = _httpProxy + ":3128";
                }
            } catch (URISyntaxException e) {
                errMsg = e.toString();
                valid = false;
                s_logger.error(String.format("Unable to configure HTTP proxy [%s] on secondary storage VM manager [%s] due to [%s].", _httpProxy, name, errMsg), e);
            } finally {
                if (!valid) {
                    String message = String.format("Unable to configure HTTP proxy [%s] on secondary storage VM manager [%s] due to [%s].", _httpProxy, name, errMsg);
                    s_logger.warn(message);
                    throw new ConfigurationException(message);
                }
            }
        }

        s_logger.info(String.format("Secondary storage VM manager [%s] was configured.", name));

        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean stopSecStorageVm(long secStorageVmId) {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        if (secStorageVm == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Unable to stop secondary storage VM [%s] due to it no longer exists.", secStorageVmId));
            }
            return false;
        }
        try {
            if (secStorageVm.getHostId() != null) {
                GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVm.getId()));
                try {
                    if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC_IN_SECONDS)) {
                        try {
                            _itMgr.stop(secStorageVm.getUuid());
                            return true;
                        } finally {
                            secStorageVmLock.unlock();
                        }
                    } else {
                        s_logger.debug(String.format("Unable to acquire secondary storage VM [%s] lock.", secStorageVm.toString()));
                        return false;
                    }
                } finally {
                    secStorageVmLock.releaseRef();
                }
            }

            return true;
        } catch (ResourceUnavailableException e) {
            s_logger.error(String.format("Unable to stop secondary storage VM [%s] due to [%s].", secStorageVm.getHostName(), e.toString()), e);
            return false;
        }
    }

    @Override
    public boolean rebootSecStorageVm(long secStorageVmId) {
        final SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);

        if (secStorageVm == null || secStorageVm.getState() == State.Destroyed) {
            return false;
        }

        if (secStorageVm.getState() == State.Running && secStorageVm.getHostId() != null) {
            final RebootCommand cmd = new RebootCommand(secStorageVm.getInstanceName(), _itMgr.getExecuteInSequence(secStorageVm.getHypervisorType()));
            final Answer answer = _agentMgr.easySend(secStorageVm.getHostId(), cmd);

            String secondaryStorageVmName = secStorageVm.getHostName();

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Successfully reboot secondary storage VM [%s].", secondaryStorageVmName));
                }

                SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_REBOOTED, secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null));

                return true;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Unable to reboot secondary storage VM [%s] due to [%s].", secondaryStorageVmName, answer == null ? "answer null" : answer.getDetails()));
                }
                return false;
            }
        } else {
            return startSecStorageVm(secStorageVmId) != null;
        }
    }

    @Override
    public boolean destroySecStorageVm(long vmId) {
        SecondaryStorageVmVO ssvm = _secStorageVmDao.findById(vmId);

        try {
            _itMgr.expunge(ssvm.getUuid());
            _secStorageVmDao.remove(ssvm.getId());
            HostVO host = _hostDao.findByTypeNameAndZoneId(ssvm.getDataCenterId(), ssvm.getHostName(), Host.Type.SecondaryStorageVM);
            if (host != null) {
                s_logger.debug(String.format("Removing host entry for secondary storage VM [%s].", vmId));
                _hostDao.remove(host.getId());

                _tmplStoreDao.expireDnldUrlsForZone(host.getDataCenterId());
                _volumeStoreDao.expireDnldUrlsForZone(host.getDataCenterId());
                return true;
            }
            return false;
        } catch (ResourceUnavailableException e) {
            s_logger.error(String.format("Unable to expunge secondary storage [%s] due to [%s].", ssvm.toString(), e.getMessage()), e);
            return false;
        }
    }

    @Override
    public void onAgentConnect(Long dcId, StartupCommand cmd) {
    }

    private String getAllocLockName() {
        return "secStorageVm.alloc";
    }

    private String getSecStorageVmLockName(long id) {
        return "secStorageVm." + id;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {
        SecondaryStorageVmVO vm = _secStorageVmDao.findById(profile.getId());
        Map<String, String> details = _vmDetailsDao.listDetailsKeyPairs(vm.getId());
        vm.setDetails(details);

        DataStore secStore = _dataStoreMgr.getImageStoreWithFreeCapacity(dest.getDataCenter().getId());
        if (secStore == null) {
            s_logger.warn(String.format("Unable to finalize virtual machine profile [%s] as it has no secondary storage available to satisfy storage needs for zone [%s].", profile.toString(), dest.getDataCenter().getUuid()));
            return false;
        }

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=secstorage");
        buf.append(" host=").append(StringUtils.toCSVList(indirectAgentLB.getManagementServerList(dest.getHost().getId(), dest.getDataCenter().getId(), null)));
        buf.append(" port=").append(_mgmtPort);
        buf.append(" name=").append(profile.getVirtualMachine().getHostName());

        buf.append(" zone=").append(dest.getDataCenter().getId());
        buf.append(" pod=").append(dest.getPod().getId());

        buf.append(" guid=").append(profile.getVirtualMachine().getHostName());

        buf.append(" workers=").append(_configDao.getValue("workers"));

        if (_configDao.isPremium()) {
            s_logger.debug("VMWare hypervisor was configured, informing secondary storage VM to load the PremiumSecondaryStorageResource.");
            buf.append(" resource=com.cloud.storage.resource.PremiumSecondaryStorageResource");
        } else {
            buf.append(" resource=org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource");
        }
        buf.append(" instance=SecStorage");
        buf.append(" sslcopy=").append(Boolean.toString(_useSSlCopy));
        buf.append(" role=").append(vm.getRole().toString());
        buf.append(" mtu=").append(_secStorageVmMtuSize);

        boolean externalDhcp = false;
        String externalDhcpStr = _configDao.getValue("direct.attach.network.externalIpAllocator.enabled");
        if (externalDhcpStr != null && externalDhcpStr.equalsIgnoreCase("true")) {
            externalDhcp = true;
        }

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }

        if (NTPServerConfig.value() != null) {
            buf.append(" ntpserverlist=").append(NTPServerConfig.value().replaceAll("\\s+",""));
        }

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            if (nic.getIPv4Address() == null) {
                buf.append(" eth").append(deviceId).append("mask=").append("0.0.0.0");
                buf.append(" eth").append(deviceId).append("ip=").append("0.0.0.0");
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
                buf.append(" private.network.device=").append("eth").append(deviceId);
            } else if (nic.getTrafficType() == TrafficType.Public) {
                buf.append(" public.network.device=").append("eth").append(deviceId);
            } else if (nic.getTrafficType() == TrafficType.Storage) {
                buf.append(" storageip=").append(nic.getIPv4Address());
                buf.append(" storagenetmask=").append(nic.getIPv4Netmask());
                buf.append(" storagegateway=").append(nic.getIPv4Gateway());
            }
        }

        if (externalDhcp) {
            buf.append(" bootproto=dhcp");
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
        String nfsVersion = imageStoreDetailsUtil != null ? imageStoreDetailsUtil.getNfsVersion(secStore.getId()) : null;
        buf.append(" nfsVersion=").append(nfsVersion);

        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Boot args for machine profile [%s]: [%s].", profile.toString(), bootArgs));
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {

        finalizeCommandsOnStart(cmds, profile);

        SecondaryStorageVmVO secVm = _secStorageVmDao.findById(profile.getId());
        DataCenter dc = dest.getDataCenter();
        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if ((nic.getTrafficType() == TrafficType.Public && dc.getNetworkType() == NetworkType.Advanced) ||
                (nic.getTrafficType() == TrafficType.Guest && (dc.getNetworkType() == NetworkType.Basic || dc.isSecurityGroupEnabled()))) {
                secVm.setPublicIpAddress(nic.getIPv4Address());
                secVm.setPublicNetmask(nic.getIPv4Netmask());
                secVm.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Management) {
                secVm.setPrivateIpAddress(nic.getIPv4Address());
                secVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _secStorageVmDao.update(secVm.getId(), secVm);
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile) {

        NicProfile managementNic = null;
        NicProfile controlNic = null;
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == TrafficType.Management) {
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control && nic.getIPv4Address() != null) {
                controlNic = nic;
            }
        }

        if (controlNic == null) {
            if (managementNic == null) {
                s_logger.warn(String.format("Management network does not exist for the secondary storage %s.", profile. toString()));
                return false;
            }
            controlNic = managementNic;
        }

        controlNic = verifySshAccessOnManagementNicForSystemVm(profile, controlNic, managementNic);

        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIPv4Address(), 3922);
        cmds.addCommand("checkSsh", check);

        return true;
    }

    protected NicProfile verifySshAccessOnManagementNicForSystemVm(VirtualMachineProfile profile, NicProfile controlNic, NicProfile managementNic) {
        if (profile.getHypervisorType() == HypervisorType.Hyperv) {
            return managementNic;
        }

        return controlNic;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn(String.format("Unable to connect via SSH to the VM [%s] due to [%s] ", profile.toString(), answer.getDetails()));
            return false;
        }

        try {
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
            IPAddressVO ipaddr = _ipAddressDao.findByAssociatedVmId(profile.getVirtualMachine().getId());
            if (ipaddr != null && ipaddr.getSystem()) {
                SecondaryStorageVmVO secVm = _secStorageVmDao.findById(profile.getId());
                secVm.setPublicIpAddress(ipaddr.getAddress().addr());
                _secStorageVmDao.update(secVm.getId(), secVm);
            }
        } catch (InsufficientAddressCapacityException ex) {
            s_logger.error(String.format("Failed to get system IP and enable static NAT for the VM [%s] due to [%s].", profile.toString(), ex.getMessage()), ex);
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            CallContext ctx = CallContext.current();
            try {
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCallingAccount(), ctx.getCallingUserId(), true);
            } catch (ResourceUnavailableException ex) {
                s_logger.error(String.format("Failed to disable static NAT and release system IP [%s] as a part of VM [%s] stop due to [%s].", ip, profile.toString(), ex.getMessage()), ex);
            }
        }
    }

    @Override
    public void finalizeExpunge(VirtualMachine vm) {
        SecondaryStorageVmVO ssvm = _secStorageVmDao.findByUuid(vm.getUuid());

        ssvm.setPublicIpAddress(null);
        ssvm.setPublicMacAddress(null);
        ssvm.setPublicNetmask(null);
        _secStorageVmDao.update(ssvm.getId(), ssvm);
    }

    @Override
    public String getScanHandlerName() {
        return "secstorage";
    }

    @Override
    public boolean canScan() {
        return true;
    }

    @Override
    public void onScanStart() {
        _zoneHostInfoMap = getZoneHostInfo();
    }

    @Override
    public Long[] getScannablePools() {
        List<DataCenterVO> zones = _dcDao.listEnabledZones();

        Long[] dcIdList = new Long[zones.size()];
        int i = 0;
        for (DataCenterVO dc : zones) {
            dcIdList[i++] = dc.getId();
        }

        return dcIdList;
    }

    @Override
    public boolean isPoolReadyForScan(Long dataCenterId) {
        if (!isZoneReady(_zoneHostInfoMap, dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Zone [%s] is not ready to launch secondary storage VM.", dataCenterId));
            }
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Zone [%s] is ready to launch secondary storage VM.", dataCenterId));
        }
        return true;
    }

    @Override
    public Pair<AfterScanAction, Object> scanPool(Long dataCenterId) {
        List<SecondaryStorageVmVO> ssVms =
            _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Running, State.Migrating, State.Starting,
                State.Stopped, State.Stopping);
        int vmSize = (ssVms == null) ? 0 : ssVms.size();
        List<DataStore> ssStores = _dataStoreMgr.getImageStoresByScopeExcludingReadOnly(new ZoneScope(dataCenterId));
        int storeSize = (ssStores == null) ? 0 : ssStores.size();
        if (storeSize > vmSize) {
                s_logger.info(String.format("No secondary storage VM found in zone [%s], starting a new one.", dataCenterId));
            return new Pair<>(AfterScanAction.expand, SecondaryStorageVm.Role.templateProcessor);
        }

        return new Pair<>(AfterScanAction.nop, SecondaryStorageVm.Role.templateProcessor);
    }

    @Override
    public void expandPool(Long dataCenterId, Object actionArgs) {
        allocCapacity(dataCenterId, (SecondaryStorageVm.Role)actionArgs);
    }

    @Override
    public void shrinkPool(Long pool, Object actionArgs) {
    }

    @Override
    public void onScanEnd() {
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        StartupCommand firstCmd = cmd[0];
        if (!(firstCmd instanceof StartupSecondaryStorageCommand)) {
            return null;
        }

        host.setType(com.cloud.host.Host.Type.SecondaryStorageVM);
        return host;
    }

    /** Used to be called when add secondary storage on UI through DummySecondaryStorageResource to update that host entry for Secondary Storage.<br><br>
     *  Now since we move secondary storage from host table, this method, in this class, is not needed to be invoked anymore.
     */
    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        return null;
    }

    /** Since secondary storage is moved out of host table, this class should not handle delete secondary storage anymore.
     */
    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return null;
    }

    @Override
    public List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        if (dcId != null) {
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
        }
        sc.and(sc.entity().getState(), Op.IN, Status.Up, Status.Connecting);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        return sc.list();
    }

    @Override
    public HostVO pickSsvmHost(HostVO ssHost) {
        if (ssHost.getType() == Host.Type.LocalSecondaryStorage) {
            return ssHost;
        } else if (ssHost.getType() == Host.Type.SecondaryStorage) {
            Long dcId = ssHost.getDataCenterId();
            List<HostVO> ssAHosts = listUpAndConnectingSecondaryStorageVmHost(dcId);
            if (ssAHosts == null || ssAHosts.isEmpty()) {
                return null;
            }
            Collections.shuffle(ssAHosts);
            return ssAHosts.get(0);
        }
        return null;
    }

    @Override
    public void prepareStop(VirtualMachineProfile profile) {

    }

    @Override
    public void finalizeUnmanage(VirtualMachine vm) {
    }

    public List<SecondaryStorageVmAllocator> getSecondaryStorageVmAllocators() {
        return _ssVmAllocators;
    }

    @Inject
    public void setSecondaryStorageVmAllocators(List<SecondaryStorageVmAllocator> ssVmAllocators) {
        _ssVmAllocators = ssVmAllocators;
    }

    @Override
    public String getConfigComponentName() {
        return SecondaryStorageManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {NTPServerConfig, MaxNumberOfSsvmsForMigration};
    }

}
