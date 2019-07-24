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
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
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
import com.cloud.storage.UploadVO;
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

//
// Possible secondary storage vm state transition cases
//        Creating -> Destroyed
//        Creating -> Stopped --> Starting -> Running
//        HA -> Stopped -> Starting -> Running
//        Migrating -> Running    (if previous state is Running before it enters into Migrating state
//        Migrating -> Stopped    (if previous state is not Running before it enters into Migrating state)
//        Running -> HA            (if agent lost connection)
//        Stopped -> Destroyed
//
//        Creating state indicates of record creating and IP address allocation are ready, it is a transient
//         state which will soon be switching towards Running if everything goes well.
//        Stopped state indicates the readiness of being able to start (has storage and IP resources allocated)
//        Starting state can only be entered from Stopped states
//
// Starting, HA, Migrating, Creating and Running state are all counted as "Open" for available capacity calculation
// because sooner or later, it will be driven into Running state
//
public class SecondaryStorageManagerImpl extends ManagerBase implements SecondaryStorageVmManager, VirtualMachineGuru, SystemVmLoadScanHandler<Long>,
        ResourceStateAdapter, Configurable {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageManagerImpl.class);

    private static final int DEFAULT_CAPACITY_SCAN_INTERVAL = 30000; // 30
    // seconds
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC = 180; // 3
    // minutes

    private static final int STARTUP_DELAY = 60000; // 60 seconds

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

    private long _capacityScanInterval = DEFAULT_CAPACITY_SCAN_INTERVAL;
    private int _secStorageVmMtuSize;

    private String _instance;
    private boolean _useSSlCopy;
    private String _httpProxy;
    private String _allowedInternalSites;
    protected long _nodeId = ManagementServerNode.getManagementServerId();

    private SystemVmLoadScanner<Long> _loadScanner;
    private Map<Long, ZoneHostInfo> _zoneHostInfoMap; // map <zone id, info about running host in zone>

    private final GlobalLock _allocLock = GlobalLock.getInternLock(getAllocLockName());

    static final ConfigKey<String> NTPServerConfig = new ConfigKey<String>(String.class, "ntp.server.list", "Advanced", null,
            "Comma separated list of NTP servers to configure in Secondary storage VM", false, ConfigKey.Scope.Global, null);

    public SecondaryStorageManagerImpl() {
    }

    @Override
    public SecondaryStorageVmVO startSecStorageVm(long secStorageVmId) {
        try {
            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
            _itMgr.advanceStart(secStorageVm.getUuid(), null, null);
            return _secStorageVmDao.findById(secStorageVm.getId());
        } catch (StorageUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (InsufficientCapacityException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
            return null;
        } catch (Exception e) {
            s_logger.warn("Exception while trying to start secondary storage vm", e);
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

            SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(cssHost.getName());
            if (secStorageVm == null) {
                s_logger.warn("secondary storage VM " + cssHost.getName() + " doesn't exist");
                return false;
            }

            List<DataStore> ssStores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));
            for (DataStore ssStore : ssStores) {
                if (!(ssStore.getTO() instanceof NfsTO)) {
                    continue; // only do this for Nfs
                }
                String secUrl = ssStore.getUri();
                SecStorageSetupCommand setupCmd = null;
                if (!_useSSlCopy) {
                    setupCmd = new SecStorageSetupCommand(ssStore.getTO(), secUrl, null);
                } else {
                    KeystoreManager.Certificates certs = _keystoreMgr.getCertificates(ConsoleProxyManager.CERTIFICATE_NAME);
                    setupCmd = new SecStorageSetupCommand(ssStore.getTO(), secUrl, certs);
                }

                Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(ssStore.getId());
                setupCmd.setNfsVersion(nfsVersion);

                //template/volume file upload key
                String postUploadKey = _configDao.getValue(Config.SSVMPSK.key());
                setupCmd.setPostUploadKey(postUploadKey);

                Answer answer = _agentMgr.easySend(ssHostId, setupCmd);
                if (answer != null && answer.getResult()) {
                    SecStorageSetupAnswer an = (SecStorageSetupAnswer)answer;
                    if (an.get_dir() != null) {
                        // update the parent path in image_store table for this image store
                        ImageStoreVO svo = _imageStoreDao.findById(ssStore.getId());
                        svo.setParent(an.get_dir());
                        _imageStoreDao.update(ssStore.getId(), svo);
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + ssStore.getName() + " in secondary storage VM " + secStorageVm.getInstanceName());
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + ssStore.getName() + " in secondary storage VM " + secStorageVm.getInstanceName());
                    }
                    return false;
                }
            }
        }
        /* After removing SecondaryStorage entries from host table, control should never come here!!
        else if( cssHost.getType() == Host.Type.SecondaryStorage ) {
            List<SecondaryStorageVmVO> alreadyRunning = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, zoneId, State.Running);
            String secUrl = cssHost.getStorageUrl();
            SecStorageSetupCommand setupCmd = new SecStorageSetupCommand(secUrl, null);
            for ( SecondaryStorageVmVO ssVm : alreadyRunning ) {
                HostVO host = _resourceMgr.findHostByName(ssVm.getInstanceName());
                Answer answer = _agentMgr.easySend(host.getId(), setupCmd);
                if (answer != null && answer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + host.getName() + " in secondary storage VM " + ssVm.getInstanceName());
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully programmed secondary storage " + host.getName() + " in secondary storage VM " + ssVm.getInstanceName());
                    }
                    return false;
                }
            }
        }
         */
        return true;
    }

    @Override
    public boolean generateVMSetupCommand(Long ssAHostId) {
        HostVO ssAHost = _hostDao.findById(ssAHostId);
        if (ssAHost.getType() != Host.Type.SecondaryStorageVM) {
            return false;
        }
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findByInstanceName(ssAHost.getName());
        if (secStorageVm == null) {
            s_logger.warn("secondary storage VM " + ssAHost.getName() + " doesn't exist");
            return false;
        }

        SecStorageVMSetupCommand setupCmd = new SecStorageVMSetupCommand();
        if (_allowedInternalSites != null) {
            List<String> allowedCidrs = new ArrayList<String>();
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
                s_logger.debug("Successfully programmed http auth into " + secStorageVm.getHostName());
            }
            return true;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program http auth into secondary storage vm : " + secStorageVm.getHostName());
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
        SecondaryStorageVmVO thisSecStorageVm = _secStorageVmDao.findByInstanceName(ssAHost.getName());

        if (thisSecStorageVm == null) {
            s_logger.warn("secondary storage VM " + ssAHost.getName() + " doesn't exist");
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
            Answer answer = _agentMgr.easySend(ssvm.getId(), thiscpc);
            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully programmed firewall rules into SSVM " + ssvm.getName());
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("failed to program firewall rules into secondary storage vm : " + ssvm.getName());
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

        Answer answer = _agentMgr.easySend(ssAHostId, allSSVMIpList);
        if (answer != null && answer.getResult()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully programmed firewall rules into " + thisSecStorageVm.getHostName());
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("failed to program firewall rules into secondary storage vm : " + thisSecStorageVm.getHostName());
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
                s_logger.debug("Secondary storage vm not required in zone " + dataCenterId + " acc. to zone config");
            }
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Assign secondary storage vm from a newly started instance for request from data center : " + dataCenterId);
        }

        Map<String, Object> context = createSecStorageVmInstance(dataCenterId, role);

        long secStorageVmId = (Long)context.get("secStorageVmId");
        if (secStorageVmId == 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Creating secondary storage vm instance failed, data center id : " + dataCenterId);
            }

            return null;
        }

        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        // SecondaryStorageVmVO secStorageVm =
        // allocSecStorageVmStorage(dataCenterId, secStorageVmId);
        if (secStorageVm != null) {
            SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATED, dataCenterId, secStorageVmId, secStorageVm, null));
            return secStorageVm;
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to allocate secondary storage vm storage, remove the secondary storage vm record from DB, secondary storage vm id: " +
                    secStorageVmId);
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
            throw new CloudRuntimeException("Zone " + dc + " is not advanced.");
        }

        if (dc.isSecurityGroupEnabled()) {
            List<NetworkVO> networks = _networkDao.listByZoneSecurityGroup(dc.getId());
            if (CollectionUtils.isEmpty(networks)) {
                throw new CloudRuntimeException("Can not found security enabled network in SG Zone " + dc);
            }

            return networks.get(0);
        }
        else {
            TrafficType defaultTrafficType = TrafficType.Public;
            List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dc.getId(), defaultTrafficType);
            // api should never allow this situation to happen
            if (defaultNetworks.size() != 1) {
                throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type " + defaultTrafficType + " when expect to find 1");
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
            throw new CloudRuntimeException("Zone " + dc + "is not basic.");
        }

        TrafficType defaultTrafficType = TrafficType.Guest;
        List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dc.getId(), defaultTrafficType);
        // api should never allow this situation to happen
        if (defaultNetworks.size() != 1) {
            throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type " + defaultTrafficType + " when expect to find 1");
        }

        return defaultNetworks.get(0);
    }

    protected Map<String, Object> createSecStorageVmInstance(long dataCenterId, SecondaryStorageVm.Role role) {
        DataStore secStore = _dataStoreMgr.getImageStoreWithFreeCapacity(dataCenterId);
        if (secStore == null) {
            String msg = "No secondary storage available in zone " + dataCenterId + ", cannot create secondary storage vm";
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
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(offerings.size() + 1);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(2);
        try {
            networks.put(_networkMgr.setupNetwork(systemAcct, _networkOfferingDao.findById(defaultNetwork.getNetworkOfferingId()), plan, null, null, false).get(0),
                    new ArrayList<NicProfile>(Arrays.asList(defaultNic)));
            for (NetworkOffering offering : offerings) {
                networks.put(_networkMgr.setupNetwork(systemAcct, offering, plan, null, null, false).get(0), new ArrayList<NicProfile>());
            }
        } catch (ConcurrentOperationException e) {
            s_logger.info("Unable to setup due to concurrent operation. " + e);
            return new HashMap<String, Object>();
        }

        VMTemplateVO template = null;
        HypervisorType availableHypervisor = _resourceMgr.getAvailableHypervisor(dataCenterId);
        template = _templateDao.findSystemVMReadyTemplate(dataCenterId, availableHypervisor);
        if (template == null) {
            throw new CloudRuntimeException("Not able to find the System templates or not downloaded in zone " + dataCenterId);
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
            s_logger.warn("InsufficientCapacity", e);
            throw new CloudRuntimeException("Insufficient capacity exception", e);
        }

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("secStorageVmId", secStorageVm.getId());
        return context;
    }

    private SecondaryStorageVmAllocator getCurrentAllocator() {

        // for now, only one adapter is supported
        if (_ssVmAllocators.size() > 0) {
            return _ssVmAllocators.get(0);
        }

        return null;
    }

    protected String connect(String ipAddress, int port) {
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromRunningPool(long dataCenterId, SecondaryStorageVm.Role role) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Assign  secondary storage vm from running pool for request from data center : " + dataCenterId);
        }

        SecondaryStorageVmAllocator allocator = getCurrentAllocator();
        assert (allocator != null);
        List<SecondaryStorageVmVO> runningList = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Running);
        if (runningList != null && runningList.size() > 0) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Running secondary storage vm pool size : " + runningList.size());
                for (SecondaryStorageVmVO secStorageVm : runningList) {
                    s_logger.trace("Running secStorageVm instance : " + secStorageVm.getHostName());
                }
            }

            Map<Long, Integer> loadInfo = new HashMap<Long, Integer>();

            return allocator.allocSecondaryStorageVm(runningList, loadInfo, dataCenterId);
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Empty running secStorageVm pool for now in data center : " + dataCenterId);
            }
        }
        return null;
    }

    public SecondaryStorageVmVO assignSecStorageVmFromStoppedPool(long dataCenterId, SecondaryStorageVm.Role role) {
        List<SecondaryStorageVmVO> l = _secStorageVmDao.getSecStorageVmListInStates(role, dataCenterId, State.Starting, State.Stopped, State.Migrating);
        if (l != null && l.size() > 0) {
            return l.get(0);
        }

        return null;
    }

    private void allocCapacity(long dataCenterId, SecondaryStorageVm.Role role) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Allocate secondary storage vm standby capacity for data center : " + dataCenterId);
        }

        if (!isSecondaryStorageVmRequired(dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Secondary storage vm not required in zone " + dataCenterId + " according to zone config");
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
                    s_logger.info("No stopped secondary storage vm is available, need to allocate a new secondary storage vm");
                }

                if (_allocLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                    try {
                        secStorageVm = startNew(dataCenterId, role);
                        for (UploadVO upload : _uploadDao.listAll()) {
                            _uploadDao.expunge(upload.getId());
                        }
                    } finally {
                        _allocLock.unlock();
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to acquire synchronization lock for secondary storage vm allocation, wait for next scan");
                    }
                    return;
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Found a stopped secondary storage vm, starting it. Vm id : " + secStorageVm.getId());
                }
                secStorageVmFromStoppedPool = true;
            }

            if (secStorageVm != null) {
                long secStorageVmId = secStorageVm.getId();
                GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVmId));
                try {
                    if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                        try {
                            secStorageVm = startSecStorageVm(secStorageVmId);
                        } finally {
                            secStorageVmLock.unlock();
                        }
                    } else {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Unable to acquire synchronization lock for starting secondary storage vm id : " + secStorageVm.getId());
                        }
                        return;
                    }
                } finally {
                    secStorageVmLock.releaseRef();
                }

                if (secStorageVm == null) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Unable to start secondary storage vm for standby capacity, vm id : " + secStorageVmId + ", will recycle it and start a new one");
                    }

                    if (secStorageVmFromStoppedPool) {
                        destroySecStorageVm(secStorageVmId);
                    }
                } else {
                    SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                            new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_UP, dataCenterId, secStorageVmId, secStorageVm, null));
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Secondary storage vm " + secStorageVm.getHostName() + " is started");
                    }
                }
            }
        } catch (Exception e) {
            errorString = e.getMessage();
            throw e;
        } finally {
            // TODO - For now put all the alerts as creation failure. Distinguish between creation vs start failure in future.
            // Also add failure reason since startvm masks some of them.
            if(secStorageVm == null || secStorageVm.getState() != State.Running)
                SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                        new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_CREATE_FAILURE, dataCenterId, 0l, null, errorString));
        }
    }

    public boolean isZoneReady(Map<Long, ZoneHostInfo> zoneHostInfoMap, long dataCenterId) {
        ZoneHostInfo zoneHostInfo = zoneHostInfoMap.get(dataCenterId);
        if (zoneHostInfo != null && (zoneHostInfo.getFlags() & RunningHostInfoAgregator.ZoneHostInfo.ROUTING_HOST_MASK) != 0) {
            VMTemplateVO template = _templateDao.findSystemVMReadyTemplate(dataCenterId, HypervisorType.Any);
            if (template == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("System vm template is not ready at data center " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
                }
                return false;
            }

            List<DataStore> stores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(dataCenterId));
            if (stores.size() < 1) {
                s_logger.debug("No image store added  in zone " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
                return false;
            }

            DataStore store = templateMgr.getImageStore(dataCenterId, template.getId());
            if (store == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No secondary storage available in zone " + dataCenterId + ", wait until it is ready to launch secondary storage vm");
                }
                return false;
            }

            boolean useLocalStorage = false;
            Boolean useLocal = ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dataCenterId);
            if (useLocal != null) {
                useLocalStorage = useLocal.booleanValue();
            }
            List<Pair<Long, Integer>> l = _storagePoolHostDao.getDatacenterStoragePoolHostInfo(dataCenterId, !useLocalStorage);
            if (l != null && l.size() > 0 && l.get(0).second().intValue() > 0) {
                return true;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Primary storage is not ready, wait until it is ready to launch secondary storage vm. dcId: " + dataCenterId +
                        ", " + ConfigurationManagerImpl.SystemVMUseLocalStorage.key() + ": " + useLocalStorage + ". " +
                        "If you want to use local storage to start SSVM, need to set " + ConfigurationManagerImpl.SystemVMUseLocalStorage.key() + " to true");
                }
            }

        }
        return false;
    }

    private synchronized Map<Long, ZoneHostInfo> getZoneHostInfo() {
        Date cutTime = DateUtil.currentGMTTime();
        List<RunningHostCountInfo> l = _hostDao.getRunningHostCounts(new Date(cutTime.getTime() - ClusterManager.HeartbeatThreshold.value()));

        RunningHostInfoAgregator aggregator = new RunningHostInfoAgregator();
        if (l.size() > 0) {
            for (RunningHostCountInfo countInfo : l) {
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
        String useServiceVM = _configDao.getValue("secondary.storage.vm");
        boolean _useServiceVM = false;
        if ("true".equalsIgnoreCase(useServiceVM)) {
            _useServiceVM = true;
        }

        String sslcopy = _configDao.getValue("secstorage.encrypt.copy");
        if ("true".equalsIgnoreCase(sslcopy)) {
            _useSSlCopy = true;
        }

        //default to HTTP in case of missing domain
        String ssvmUrlDomain = _configDao.getValue("secstorage.ssl.cert.domain");
        if(_useSSlCopy && (ssvmUrlDomain == null || ssvmUrlDomain.isEmpty())){
            s_logger.warn("Empty secondary storage url domain, explicitly disabling SSL");
            _useSSlCopy = false;
        }

        _allowedInternalSites = _configDao.getValue("secstorage.allowed.internal.sites");

        String value = configs.get("secstorage.capacityscan.interval");
        _capacityScanInterval = NumbersUtil.parseLong(value, DEFAULT_CAPACITY_SCAN_INTERVAL);

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

        //check if there is a default service offering configured
        String ssvmSrvcOffIdStr = configs.get(Config.SecondaryStorageServiceOffering.key());
        if (ssvmSrvcOffIdStr != null) {
            _serviceOffering = _offeringDao.findByUuid(ssvmSrvcOffIdStr);
            if (_serviceOffering == null) {
                try {
                    _serviceOffering = _offeringDao.findById(Long.parseLong(ssvmSrvcOffIdStr));
                } catch (NumberFormatException ex) {
                    s_logger.debug("The system service offering specified by global config is not id, but uuid=" + ssvmSrvcOffIdStr + " for secondary storage vm");
                }
            }
            if (_serviceOffering == null) {
                s_logger.warn("Can't find system service offering specified by global config, uuid=" + ssvmSrvcOffIdStr + " for secondary storage vm");
            }
        }

        if (_serviceOffering == null || !_serviceOffering.isSystemUse()) {
            int ramSize = NumbersUtil.parseInt(_configDao.getValue("ssvm.ram.size"), DEFAULT_SS_VM_RAMSIZE);
            int cpuFreq = NumbersUtil.parseInt(_configDao.getValue("ssvm.cpu.mhz"), DEFAULT_SS_VM_CPUMHZ);
            List<ServiceOfferingVO> offerings = _offeringDao.createSystemServiceOfferings("System Offering For Secondary Storage VM",
                    ServiceOffering.ssvmDefaultOffUniqueName, 1, ramSize, cpuFreq, null, null, false, null,
                    Storage.ProvisioningType.THIN, true, null, true, VirtualMachine.Type.SecondaryStorageVm, true);
            // this can sometimes happen, if DB is manually or programmatically manipulated
            if (offerings == null || offerings.size() < 2) {
                String msg = "Data integrity problem : System Offering For Secondary Storage VM has been removed?";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        }

        if (_useServiceVM) {
            _loadScanner = new SystemVmLoadScanner<Long>(this);
            _loadScanner.initScan(STARTUP_DELAY, _capacityScanInterval);
        }

        _httpProxy = configs.get(Config.SecStorageProxy.key());
        if (_httpProxy != null) {
            boolean valid = true;
            String errMsg = null;
            try {
                URI uri = new URI(_httpProxy);
                if (!"http".equalsIgnoreCase(uri.getScheme())) {
                    errMsg = "Only support http proxy";
                    valid = false;
                } else if (uri.getHost() == null) {
                    errMsg = "host can not be null";
                    valid = false;
                } else if (uri.getPort() == -1) {
                    _httpProxy = _httpProxy + ":3128";
                }
            } catch (URISyntaxException e) {
                errMsg = e.toString();
            } finally {
                if (!valid) {
                    s_logger.debug("ssvm http proxy " + _httpProxy + " is invalid: " + errMsg);
                    throw new ConfigurationException("ssvm http proxy " + _httpProxy + "is invalid: " + errMsg);
                }
            }
        }
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Secondary storage vm Manager is configured.");
        }
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean stopSecStorageVm(long secStorageVmId) {
        SecondaryStorageVmVO secStorageVm = _secStorageVmDao.findById(secStorageVmId);
        if (secStorageVm == null) {
            String msg = "Stopping secondary storage vm failed: secondary storage vm " + secStorageVmId + " no longer exists";
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msg);
            }
            return false;
        }
        try {
            if (secStorageVm.getHostId() != null) {
                GlobalLock secStorageVmLock = GlobalLock.getInternLock(getSecStorageVmLockName(secStorageVm.getId()));
                try {
                    if (secStorageVmLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_SYNC)) {
                        try {
                            _itMgr.stop(secStorageVm.getUuid());
                            return true;
                        } finally {
                            secStorageVmLock.unlock();
                        }
                    } else {
                        String msg = "Unable to acquire secondary storage vm lock : " + secStorageVm.toString();
                        s_logger.debug(msg);
                        return false;
                    }
                } finally {
                    secStorageVmLock.releaseRef();
                }
            }

            // vm was already stopped, return true
            return true;
        } catch (ResourceUnavailableException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopping secondary storage vm " + secStorageVm.getHostName() + " faled : exception " + e.toString());
            }
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

            if (answer != null && answer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully reboot secondary storage vm " + secStorageVm.getHostName());
                }

                SubscriptionMgr.getInstance().notifySubscribers(ALERT_SUBJECT, this,
                    new SecStorageVmAlertEventArgs(SecStorageVmAlertEventArgs.SSVM_REBOOTED, secStorageVm.getDataCenterId(), secStorageVm.getId(), secStorageVm, null));

                return true;
            } else {
                String msg = "Rebooting Secondary Storage VM failed - " + secStorageVm.getHostName();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
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
                s_logger.debug("Removing host entry for ssvm id=" + vmId);
                _hostDao.remove(host.getId());
                //Expire the download urls in the entire zone for templates and volumes.
                _tmplStoreDao.expireDnldUrlsForZone(host.getDataCenterId());
                _volumeStoreDao.expireDnldUrlsForZone(host.getDataCenterId());
                return true;
            }
            return false;
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge " + ssvm, e);
            return false;
        }
    }

    @Override
    public void onAgentConnect(Long dcId, StartupCommand cmd) {
    }

    private String getAllocLockName() {
        // to improve security, it may be better to return a unique mashed
        // name(for example MD5 hashed)
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
            s_logger.error(String.format("Unable to finalize virtual machine profile as no secondary storage available to satisfy storage needs for zone: %s", dest.getDataCenter().getUuid()));
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
            s_logger.debug("VmWare hypervisor configured, telling the ssvm to load the PremiumSecondaryStorageResource");
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

        /* External DHCP mode */
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
        Integer nfsVersion = imageStoreDetailsUtil != null ? imageStoreDetailsUtil.getNfsVersion(secStore.getId()) : null;
        buf.append(" nfsVersion=").append(nfsVersion);

        String bootArgs = buf.toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + bootArgs);
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
                s_logger.error("Management network doesn't exist for the secondaryStorageVm " + profile.getVirtualMachine());
                return false;
            }
            controlNic = managementNic;
        }

        // verify ssh access on management nic for system vm running on HyperV
        if(profile.getHypervisorType() == HypervisorType.Hyperv) {
            controlNic = managementNic;
        }

        CheckSshCommand check = new CheckSshCommand(profile.getInstanceName(), controlNic.getIPv4Address(), 3922);
        cmds.addCommand("checkSsh", check);

        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }

        try {
            //get system ip and create static nat rule for the vm in case of basic networking with EIP/ELB
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
            IPAddressVO ipaddr = _ipAddressDao.findByAssociatedVmId(profile.getVirtualMachine().getId());
            if (ipaddr != null && ipaddr.getSystem()) {
                SecondaryStorageVmVO secVm = _secStorageVmDao.findById(profile.getId());
                // override SSVM guest IP with EIP, so that download url's with be prepared with EIP
                secVm.setPublicIpAddress(ipaddr.getAddress().addr());
                _secStorageVmDao.update(secVm.getId(), secVm);
            }
        } catch (Exception ex) {
            s_logger.warn("Failed to get system ip and enable static nat for the vm " + profile.getVirtualMachine() + " due to exception ", ex);
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
        //release elastic IP here
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            CallContext ctx = CallContext.current();
            try {
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCallingAccount(), ctx.getCallingUserId(), true);
            } catch (Exception ex) {
                s_logger.warn("Failed to disable static nat and release system ip " + ip + " as a part of vm " + profile.getVirtualMachine() + " stop due to exception ",
                    ex);
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
    public boolean isPoolReadyForScan(Long pool) {
        // pool is at zone basis
        long dataCenterId = pool.longValue();

        if (!isZoneReady(_zoneHostInfoMap, dataCenterId)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Zone " + dataCenterId + " is not ready to launch secondary storage VM yet");
            }
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Zone " + dataCenterId + " is ready to launch secondary storage VM");
        }
        return true;
    }

    @Override
    public Pair<AfterScanAction, Object> scanPool(Long pool) {
        long dataCenterId = pool.longValue();

        List<SecondaryStorageVmVO> ssVms =
            _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Running, State.Migrating, State.Starting,
                State.Stopped, State.Stopping);
        int vmSize = (ssVms == null) ? 0 : ssVms.size();
        List<DataStore> ssStores = _dataStoreMgr.getImageStoresByScope(new ZoneScope(dataCenterId));
        int storeSize = (ssStores == null) ? 0 : ssStores.size();
        if (storeSize > vmSize) {
            s_logger.info("No secondary storage vms found in datacenter id=" + dataCenterId + ", starting a new one");
            return new Pair<AfterScanAction, Object>(AfterScanAction.expand, SecondaryStorageVm.Role.templateProcessor);
        }

        return new Pair<AfterScanAction, Object>(AfterScanAction.nop, SecondaryStorageVm.Role.templateProcessor);
    }

    @Override
    public void expandPool(Long pool, Object actionArgs) {
        long dataCenterId = pool.longValue();
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
        /* Called when Secondary Storage VM connected */
        StartupCommand firstCmd = cmd[0];
        if (!(firstCmd instanceof StartupSecondaryStorageCommand)) {
            return null;
        }

        host.setType(com.cloud.host.Host.Type.SecondaryStorageVM);
        return host;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        // Used to be Called when add secondary storage on UI through DummySecondaryStorageResource to update that host entry for Secondary Storage.
        // Now since we move secondary storage from host table, this code is not needed to be invoked anymore.
        /*
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupStorageCommand)) {
            return null;
        }

        com.cloud.host.Host.Type type = null;
        StartupStorageCommand ssCmd = ((StartupStorageCommand) firstCmd);
        if (ssCmd.getHostType() == Host.Type.SecondaryStorageCmdExecutor) {
            type = ssCmd.getHostType();
        } else {
            if (ssCmd.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE) {
                type = Host.Type.SecondaryStorage;
                if (resource != null && resource instanceof DummySecondaryStorageResource) {
                    host.setResource(null);
                }
            } else if (ssCmd.getResourceType() == Storage.StorageResourceType.LOCAL_SECONDARY_STORAGE) {
                type = Host.Type.LocalSecondaryStorage;
            } else {
                type = Host.Type.Storage;
            }

            final Map<String, String> hostDetails = ssCmd.getHostDetails();
            if (hostDetails != null) {
                if (details != null) {
                    details.putAll(hostDetails);
                } else {
                    details = hostDetails;
                }
            }

            host.setDetails(details);
            host.setParent(ssCmd.getParent());
            host.setTotalSize(ssCmd.getTotalSize());
            host.setHypervisorType(HypervisorType.None);
            host.setType(type);
            if (ssCmd.getNfsShare() != null) {
                host.setStorageUrl(ssCmd.getNfsShare());
            }
        }
         */
        return null; // no need to handle this event anymore since secondary storage is not in host table anymore.
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // Since secondary storage is moved out of host table, this class should not handle delete secondary storage anymore.
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
        return new ConfigKey<?>[] {NTPServerConfig};
    }

}
