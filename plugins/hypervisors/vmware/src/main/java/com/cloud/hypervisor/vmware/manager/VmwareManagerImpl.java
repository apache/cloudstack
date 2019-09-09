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
package com.cloud.hypervisor.vmware.manager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.zone.AddVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.ListVmwareDcsCmd;
import org.apache.cloudstack.api.command.admin.zone.RemoveVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateVmwareDcCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobManagerImpl;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;

import com.amazonaws.util.CollectionUtils;
import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.dao.ManagementServerHostPeerDao;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.vmware.LegacyZoneVO;
import com.cloud.hypervisor.vmware.VmwareCleanupMaid;
import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMap;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMapVO;
import com.cloud.hypervisor.vmware.dao.LegacyZoneDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.HostFirewallSystemMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualSwitchType;
import com.cloud.hypervisor.vmware.mo.VmwareHostType;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VmwareTrafficLabel;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.org.Cluster;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.FileUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.UserVmCloneSettingDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.ManagedObjectReference;

public class VmwareManagerImpl extends ManagerBase implements VmwareManager, VmwareStorageMount, Listener, VmwareDatacenterService, Configurable {
    private static final Logger s_logger = Logger.getLogger(VmwareManagerImpl.class);

    private static final long SECONDS_PER_MINUTE = 60;
    private static final int DEFAULT_PORTS_PER_DV_PORT_GROUP_VSPHERE4_x = 256;
    private static final int DEFAULT_PORTS_PER_DV_PORT_GROUP = 8;

    private int _timeout;

    private String _instance;

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private NetworkModel _netMgr;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private HostDetailsDao hostDetailsDao;
    @Inject
    private CommandExecLogDao _cmdExecLogDao;
    @Inject
    private DataStoreManager _dataStoreMgr;
    @Inject
    private CiscoNexusVSMDeviceDao _nexusDao;
    @Inject
    private ClusterVSMMapDao _vsmMapDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ConfigurationServer _configServer;
    @Inject
    private HypervisorCapabilitiesDao _hvCapabilitiesDao;
    @Inject
    private DataCenterDao datacenterDao;
    @Inject
    private VmwareDatacenterDao vmwareDcDao;
    @Inject
    private VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Inject
    private LegacyZoneDao legacyZoneDao;
    @Inject
    private ManagementServerHostPeerDao msHostPeerDao;
    @Inject
    private ClusterManager clusterManager;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    private PrimaryDataStoreDao primaryStorageDao;
    @Inject
    private VMTemplatePoolDao templateDataStoreDao;
    @Inject
    private TemplateJoinDao templateDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private UserVmCloneSettingDao cloneSettingDao;
    @Inject
    private TemplateManager templateManager;

    private String _mountParent;
    private StorageLayer _storage;
    private final String _privateNetworkVSwitchName = "vSwitch0";

    private int _portsPerDvPortGroup = DEFAULT_PORTS_PER_DV_PORT_GROUP;
    private boolean _fullCloneFlag;
    private boolean _instanceNameFlag;
    private String _serviceConsoleName;
    private String _managemetPortGroupName;
    private String _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();
    private String _recycleHungWorker = "false";
    private int _additionalPortRangeStart;
    private int _additionalPortRangeSize;
    private int _routerExtraPublicNics = 2;
    private int _vCenterSessionTimeout = 1200000; // Timeout in milliseconds
    private String _rootDiskController = DiskControllerType.ide.toString();

    private final String _dataDiskController = DiskControllerType.osdefault.toString();

    private final Map<String, String> _storageMounts = new HashMap<>();

    private final Random _rand = new Random(System.currentTimeMillis());

    private static ScheduledExecutorService templateCleanupScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vmware-FullyClonedTemplateCheck"));;

    private final VmwareStorageManager _storageMgr;
    private final GlobalLock _exclusiveOpLock = GlobalLock.getInternLock("vmware.exclusive.op");

    public VmwareManagerImpl() {
        _storageMgr = new VmwareStorageManagerImpl(this);
    }

    @Override
    public String getConfigComponentName() {
        return VmwareManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {s_vmwareNicHotplugWaitTimeout, s_vmwareCleanOldWorderVMs, templateCleanupInterval, s_vmwareSearchExcludeFolder, s_vmwareOVAPackageTimeout};
    }
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        s_logger.info("Configure VmwareManagerImpl, manager name: " + name);

        if (!_configDao.isPremium()) {
            s_logger.error("Vmware component can only run under premium distribution");
            throw new ConfigurationException("Vmware component can only run under premium distribution");
        }

        _instance = _configDao.getValue(Config.InstanceName.key());
        if (_instance == null) {
            _instance = "DEFAULT";
        }
        s_logger.info("VmwareManagerImpl config - instance.name: " + _instance);

        _mountParent = _configDao.getValue(Config.MountParent.key());
        if (_mountParent == null) {
            _mountParent = File.separator + "mnt";
        }

        if (_instance != null) {
            _mountParent = _mountParent + File.separator + _instance;
        }
        s_logger.info("VmwareManagerImpl config - _mountParent: " + _mountParent);

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;

        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            _storage = new JavaStorageLayer();
            _storage.configure("StorageLayer", params);
        }

        value = _configDao.getValue(Config.VmwareCreateFullClone.key());
        if (value == null) {
            _fullCloneFlag = false;
        } else {
            _fullCloneFlag = Boolean.parseBoolean(value);
        }

        value = _configDao.getValue(Config.SetVmInternalNameUsingDisplayName.key());
        if (value == null) {
            _instanceNameFlag = false;
        } else {
            _instanceNameFlag = Boolean.parseBoolean(value);
        }

        _serviceConsoleName = _configDao.getValue(Config.VmwareServiceConsole.key());
        if (_serviceConsoleName == null) {
            _serviceConsoleName = "Service Console";
        }

        _managemetPortGroupName = _configDao.getValue(Config.VmwareManagementPortGroup.key());
        if (_managemetPortGroupName == null) {
            _managemetPortGroupName = "Management Network";
        }

        _defaultSystemVmNicAdapterType = _configDao.getValue(Config.VmwareSystemVmNicDeviceType.key());
        if (_defaultSystemVmNicAdapterType == null) {
            _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();
        }

        _additionalPortRangeStart = NumbersUtil.parseInt(_configDao.getValue(Config.VmwareAdditionalVncPortRangeStart.key()), 59000);
        if (_additionalPortRangeStart > 65535) {
            s_logger.warn("Invalid port range start port (" + _additionalPortRangeStart + ") for additional VNC port allocation, reset it to default start port 59000");
            _additionalPortRangeStart = 59000;
        }

        _additionalPortRangeSize = NumbersUtil.parseInt(_configDao.getValue(Config.VmwareAdditionalVncPortRangeSize.key()), 1000);
        if (_additionalPortRangeSize < 0 || _additionalPortRangeStart + _additionalPortRangeSize > 65535) {
            s_logger.warn("Invalid port range size (" + _additionalPortRangeSize + " for range starts at " + _additionalPortRangeStart);
            _additionalPortRangeSize = Math.min(1000, 65535 - _additionalPortRangeStart);
        }

        _routerExtraPublicNics = NumbersUtil.parseInt(_configDao.getValue(Config.RouterExtraPublicNics.key()), 2);

        _vCenterSessionTimeout = NumbersUtil.parseInt(_configDao.getValue(Config.VmwareVcenterSessionTimeout.key()), 1200) * 1000;
        s_logger.info("VmwareManagerImpl config - vmware.vcenter.session.timeout: " + _vCenterSessionTimeout);

        _recycleHungWorker = _configDao.getValue(Config.VmwareRecycleHungWorker.key());
        if (_recycleHungWorker == null || _recycleHungWorker.isEmpty()) {
            _recycleHungWorker = "false";
        }

        _rootDiskController = _configDao.getValue(Config.VmwareRootDiskControllerType.key());
        if (_rootDiskController == null || _rootDiskController.isEmpty()) {
            _rootDiskController = DiskControllerType.ide.toString();
        }

        s_logger.info("Additional VNC port allocation range is settled at " + _additionalPortRangeStart + " to " + (_additionalPortRangeStart + _additionalPortRangeSize));

        ((VmwareStorageManagerImpl)_storageMgr).configure(params);

        _agentMgr.registerForHostEvents(this, true, true, true);

        s_logger.info("VmwareManagerImpl has been successfully configured");
        return true;
    }

    @Override
    public boolean start() {
// Do not run empty task        _hostScanScheduler.scheduleAtFixedRate(getHostScanTask(), STARTUP_DELAY, _hostScanInterval, TimeUnit.MILLISECONDS);
// but implement it first!

        startTemplateCleanJobSchedule();
        startupCleanup(_mountParent);

        s_logger.info("start done");
        return true;
    }

    @Override
    public boolean stop() {
        s_logger.info("shutting down scheduled tasks");
        templateCleanupScheduler.shutdown();
        shutdownCleanup();
        return true;
    }

    @Override
    public boolean getFullCloneFlag() {
        return _fullCloneFlag;
    }

    @Override
    public String composeWorkerName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String getPrivateVSwitchName(long dcId, HypervisorType hypervisorType) {
        return _netMgr.getDefaultManagementTrafficLabel(dcId, hypervisorType);
    }

    private void prepareHost(HostMO hostMo, String privateTrafficLabel) throws Exception {
        // For ESX host, we need to enable host firewall to allow VNC access
        HostFirewallSystemMO firewallMo = hostMo.getHostFirewallSystemMO();
        if (firewallMo != null) {
            if (hostMo.getHostType() == VmwareHostType.ESX) {
                firewallMo.enableRuleset("vncServer");
                firewallMo.refreshFirewall();
            }
        }

        // prepare at least one network on the vswitch to enable OVF importing
        String vSwitchName;
        String vlanId;
        String vSwitchType;
        VmwareTrafficLabel mgmtTrafficLabelObj = new VmwareTrafficLabel(privateTrafficLabel, TrafficType.Management);
        vSwitchName = mgmtTrafficLabelObj.getVirtualSwitchName();
        vlanId = mgmtTrafficLabelObj.getVlanId();
        vSwitchType = mgmtTrafficLabelObj.getVirtualSwitchType().toString();

        s_logger.info("Preparing network on host " + hostMo.getContext().toString() + " for " + privateTrafficLabel);
        VirtualSwitchType vsType = VirtualSwitchType.getType(vSwitchType);
        //The management network is probably always going to be a physical network with islation type of vlans, so assume BroadcastDomainType VLAN
        if (VirtualSwitchType.StandardVirtualSwitch == vsType) {
            HypervisorHostHelper.prepareNetwork(vSwitchName, "cloud.private", hostMo, vlanId, null, null, 180000, false, BroadcastDomainType.Vlan, null, null);
        }
        else {
            int portsPerDvPortGroup = _portsPerDvPortGroup;
            AboutInfo about = hostMo.getHostAboutInfo();
            if (about != null) {
                String version = about.getApiVersion();
                if (version != null && (version.equals("4.0") || version.equals("4.1")) && _portsPerDvPortGroup < DEFAULT_PORTS_PER_DV_PORT_GROUP_VSPHERE4_x) {
                    portsPerDvPortGroup = DEFAULT_PORTS_PER_DV_PORT_GROUP_VSPHERE4_x;
                }
            }
            HypervisorHostHelper.prepareNetwork(vSwitchName, "cloud.private", hostMo, vlanId, null, null, null, 180000,
                    vsType, portsPerDvPortGroup, null, false, BroadcastDomainType.Vlan, null, null);
        }
    }

    @Override
    public List<ManagedObjectReference> addHostToPodCluster(VmwareContext serviceContext, long dcId, Long podId, Long clusterId, String hostInventoryPath)
            throws Exception {
        if (serviceContext == null) {
            throw new CloudRuntimeException("Invalid serviceContext");
        }
        ManagedObjectReference mor = serviceContext.getHostMorByPath(hostInventoryPath);
        String privateTrafficLabel = null;
        privateTrafficLabel = serviceContext.getStockObject("privateTrafficLabel");
        if (privateTrafficLabel == null) {
            privateTrafficLabel = _privateNetworkVSwitchName;
        }

        if (mor != null) {
            List<ManagedObjectReference> returnedHostList = new ArrayList<ManagedObjectReference>();

            if (mor.getType().equals("ComputeResource")) {
                List<ManagedObjectReference> hosts = serviceContext.getVimClient().getDynamicProperty(mor, "host");
                assert (hosts != null && hosts.size() > 0);

                // For ESX host, we need to enable host firewall to allow VNC access
                HostMO hostMo = new HostMO(serviceContext, hosts.get(0));

                prepareHost(hostMo, privateTrafficLabel);
                returnedHostList.add(hosts.get(0));
                return returnedHostList;
            } else if (mor.getType().equals("ClusterComputeResource")) {
                List<ManagedObjectReference> hosts = serviceContext.getVimClient().getDynamicProperty(mor, "host");
                assert (hosts != null);

                if (hosts.size() > 0) {
                    AboutInfo about = (AboutInfo)(serviceContext.getVimClient().getDynamicProperty(hosts.get(0), "config.product"));
                    String version = about.getApiVersion();
                    int maxHostsPerCluster = _hvCapabilitiesDao.getMaxHostsPerCluster(HypervisorType.VMware, version);
                    if (hosts.size() > maxHostsPerCluster) {
                        String msg = "Failed to add VMware cluster as size is too big, current size: " + hosts.size() + ", max. size: " + maxHostsPerCluster;
                        s_logger.error(msg);
                        throw new DiscoveredWithErrorException(msg);
                    }
                }

                for (ManagedObjectReference morHost : hosts) {
                    // For ESX host, we need to enable host firewall to allow VNC access
                    HostMO hostMo = new HostMO(serviceContext, morHost);
                    prepareHost(hostMo, privateTrafficLabel);
                    returnedHostList.add(morHost);
                }
                return returnedHostList;
            } else if (mor.getType().equals("HostSystem")) {
                // For ESX host, we need to enable host firewall to allow VNC access
                HostMO hostMo = new HostMO(serviceContext, mor);
                prepareHost(hostMo, privateTrafficLabel);
                returnedHostList.add(mor);
                return returnedHostList;
            } else {
                s_logger.error("Unsupport host type " + mor.getType() + ":" + mor.getValue() + " from inventory path: " + hostInventoryPath);
                return null;
            }
        }

        s_logger.error("Unable to find host from inventory path: " + hostInventoryPath);
        return null;
    }

    @Override
    public Pair<String, Long> getSecondaryStorageStoreUrlAndId(long dcId) {

        String secUrl = null;
        Long secId = null;
        DataStore secStore = _dataStoreMgr.getImageStoreWithFreeCapacity(dcId);
        if (secStore != null) {
            secUrl = secStore.getUri();
            secId = secStore.getId();
        }

        if (secUrl == null) {
            // we are using non-NFS image store, then use cache storage instead
            s_logger.info("Secondary storage is not NFS, we need to use staging storage");
            DataStore cacheStore = _dataStoreMgr.getImageCacheStore(dcId);
            if (cacheStore != null) {
                secUrl = cacheStore.getUri();
                secId = cacheStore.getId();
            } else {
                s_logger.warn("No staging storage is found when non-NFS secondary storage is used");
            }
        }

        return new Pair<String, Long>(secUrl, secId);
    }

    @Override
    public List<Pair<String, Long>> getSecondaryStorageStoresUrlAndIdList(long dcId) {
        List<Pair<String, Long>> urlIdList = new ArrayList<>();
        List<DataStore> secStores = _dataStoreMgr.listImageStoresWithFreeCapacity(dcId);
        if (!CollectionUtils.isNullOrEmpty(secStores)) {
            for (DataStore secStore : secStores) {
                if (secStore != null) {
                    urlIdList.add(new Pair<>(secStore.getUri(), secStore.getId()));
                }
            }
        }

        if (urlIdList.isEmpty()) {
            // we are using non-NFS image store, then use cache storage instead
            s_logger.info("Secondary storage is not NFS, we need to use staging storage");
            DataStore cacheStore = _dataStoreMgr.getImageCacheStore(dcId);
            if (cacheStore != null) {
                urlIdList.add(new Pair<>(cacheStore.getUri(), cacheStore.getId()));
            } else {
                s_logger.warn("No staging storage is found when non-NFS secondary storage is used");
            }
        }

        return urlIdList;
    }

    @Override
    public String getServiceConsolePortGroupName() {
        return _serviceConsoleName;
    }

    @Override
    public String getManagementPortGroupName() {
        return _managemetPortGroupName;
    }

    @Override
    public String getManagementPortGroupByHost(HostMO hostMo) throws Exception {
        if (hostMo.getHostType() == VmwareHostType.ESXi) {
            return _managemetPortGroupName;
        }
        return _serviceConsoleName;
    }

    @Override
    public void setupResourceStartupParams(Map<String, Object> params) {
        params.put("vmware.create.full.clone", _fullCloneFlag);
        params.put("vm.instancename.flag", _instanceNameFlag);
        params.put("service.console.name", _serviceConsoleName);
        params.put("management.portgroup.name", _managemetPortGroupName);
        params.put("vmware.root.disk.controller", _rootDiskController);
        params.put("vmware.data.disk.controller", _dataDiskController);
        params.put("vmware.recycle.hung.wokervm", _recycleHungWorker);
        params.put("ports.per.dvportgroup", _portsPerDvPortGroup);
    }

    @Override
    public VmwareStorageManager getStorageManager() {
        return _storageMgr;
    }

    @Override
    public void gcLeftOverVMs(VmwareContext context) {
        VmwareCleanupMaid.gcLeftOverVMs(context);
    }

    @Override
    public boolean needRecycle(String workerTag) {
        if (s_logger.isInfoEnabled())
            s_logger.info("Check to see if a worker VM with tag " + workerTag + " needs to be recycled");

        if (workerTag == null || workerTag.isEmpty()) {
            s_logger.error("Invalid worker VM tag " + workerTag);
            return false;
        }

        String tokens[] = workerTag.split("-");
        if (tokens.length != 3) {
            s_logger.error("Invalid worker VM tag " + workerTag);
            return false;
        }

        long startTick = Long.parseLong(tokens[0]);
        long msid = Long.parseLong(tokens[1]);
        long runid = Long.parseLong(tokens[2]);

        if (msHostPeerDao.countStateSeenInPeers(msid, runid, ManagementServerHost.State.Down) > 0) {
            if (s_logger.isInfoEnabled())
                s_logger.info("Worker VM's owner management server node has been detected down from peer nodes, recycle it");
            return true;
        }

        if (runid != clusterManager.getManagementRunId(msid)) {
            if (s_logger.isInfoEnabled())
                s_logger.info("Worker VM's owner management server has changed runid, recycle it");
            return true;
        }

        // this time-out check was disabled
        // "until we have found out a VMware API that can check if there are pending tasks on the subject VM"
        // but as we expire jobs and those stale worker VMs stay around untill an MS reboot we opt in to have them removed anyway
        Instant start = Instant.ofEpochMilli(startTick);
        Instant end = start.plusSeconds(2 * (AsyncJobManagerImpl.JobExpireMinutes.value() + AsyncJobManagerImpl.JobCancelThresholdMinutes.value()) * SECONDS_PER_MINUTE);
        Instant now = Instant.now();
        if(s_vmwareCleanOldWorderVMs.value() && now.isAfter(end)) {
            if(s_logger.isInfoEnabled()) {
                s_logger.info("Worker VM expired, seconds elapsed: " + Duration.between(start,now).getSeconds());
            }
            return true;
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Worker VM with tag '" + workerTag + "' does not need recycling, yet." +
                    "But in " + Duration.between(now,end).getSeconds() + " seconds, though");
        }
        return false;
    }

    @Override
    public void prepareSecondaryStorageStore(String storageUrl, Long storeId) {
        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(storeId);
        String mountPoint = getMountPoint(storageUrl, nfsVersion);

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            if (lock.lock(3600)) {
                try {
                    File patchFolder = new File(mountPoint + "/systemvm");
                    if (!patchFolder.exists()) {
                        if (!patchFolder.mkdirs()) {
                            String msg = "Unable to create systemvm folder on secondary storage. location: " + patchFolder.toString();
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    }

                    File srcIso = getSystemVMPatchIsoFile();
                    File destIso = new File(mountPoint + "/systemvm/" + getSystemVMIsoFileNameOnDatastore());
                    if (!destIso.exists()) {
                        s_logger.info("Inject SSH key pairs before copying systemvm.iso into secondary storage");
                        _configServer.updateKeyPairs();

                        s_logger.info("Copy System VM patch ISO file to secondary storage. source ISO: " + srcIso.getAbsolutePath() + ", destination: " +
                                destIso.getAbsolutePath());
                        try {
                            FileUtil.copyfile(srcIso, destIso);
                        } catch (IOException e) {
                            s_logger.error("Unexpected exception ", e);

                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: " + srcIso.toString() + ", dest location: " + destIso;
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("SystemVM ISO file " + destIso.getPath() + " already exists");
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    @Override
    public String getSystemVMIsoFileNameOnDatastore() {
        String version = this.getClass().getPackage().getImplementationVersion();
        String fileName = "systemvm-" + version + ".iso";
        return fileName.replace(':', '-');
    }

    @Override
    public String getSystemVMDefaultNicAdapterType() {
        return _defaultSystemVmNicAdapterType;
    }

    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        URL url = this.getClass().getClassLoader().getResource("vms/systemvm.iso");
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }

        if (isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/systemvm.iso");
        }

        assert (isoFile != null);
        if (!isoFile.exists()) {
            s_logger.error("Unable to locate systemvm.iso in your setup at " + isoFile.toString());
        }
        return isoFile;
    }

    @Override
    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader().getResource("scripts/vm/systemvm/id_rsa.cloud");
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File("/usr/share/cloudstack-common/scripts/vm/systemvm/id_rsa.cloud");
        }
        assert (keyFile != null);
        if (!keyFile.exists()) {
            s_logger.error("Unable to locate id_rsa.cloud in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    @Override
    public String getMountPoint(String storageUrl, Integer nfsVersion) {
        String mountPoint = null;
        synchronized (_storageMounts) {
            mountPoint = _storageMounts.get(storageUrl);
            if (mountPoint != null) {
                return mountPoint;
            }

            URI uri;
            try {
                uri = new URI(storageUrl);
            } catch (URISyntaxException e) {
                s_logger.error("Invalid storage URL format ", e);
                throw new CloudRuntimeException("Unable to create mount point due to invalid storage URL format " + storageUrl);
            }

            mountPoint = mount(uri.getHost() + ":" + uri.getPath(), _mountParent, nfsVersion);
            if (mountPoint == null) {
                s_logger.error("Unable to create mount point for " + storageUrl);
                return "/mnt/sec"; // throw new CloudRuntimeException("Unable to create mount point for " + storageUrl);
            }

            _storageMounts.put(storageUrl, mountPoint);
            return mountPoint;
        }
    }

    private String setupMountPoint(String parent) {
        String mountPoint = null;
        long mshostId = ManagementServerNode.getManagementServerId();
        for (int i = 0; i < 10; i++) {
            String mntPt = parent + File.separator + String.valueOf(mshostId) + "." + Integer.toHexString(_rand.nextInt(Integer.MAX_VALUE));
            File file = new File(mntPt);
            if (!file.exists()) {
                if (_storage.mkdir(mntPt)) {
                    mountPoint = mntPt;
                    break;
                }
            }
            s_logger.error("Unable to create mount: " + mntPt);
        }

        return mountPoint;
    }

    private void startupCleanup(String parent) {
        s_logger.info("Cleanup mounted NFS mount points used in previous session");

        long mshostId = ManagementServerNode.getManagementServerId();

        // cleanup left-over NFS mounts from previous session
        List<String> mounts = _storage.listMountPointsByMsHost(parent, mshostId);
        if (mounts != null && !mounts.isEmpty()) {
            for (String mountPoint : mounts) {
                s_logger.info("umount NFS mount from previous session: " + mountPoint);

                String result = null;
                Script command = new Script(true, "umount", _timeout, s_logger);
                command.add(mountPoint);
                result = command.execute();
                if (result != null) {
                    s_logger.warn("Unable to umount " + mountPoint + " due to " + result);
                }
                File file = new File(mountPoint);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    private void shutdownCleanup() {
        s_logger.info("Cleanup mounted NFS mount points used in current session");

        for (String mountPoint : _storageMounts.values()) {
            s_logger.info("umount NFS mount: " + mountPoint);

            String result = null;
            Script command = new Script(true, "umount", _timeout, s_logger);
            command.add(mountPoint);
            result = command.execute();
            if (result != null) {
                s_logger.warn("Unable to umount " + mountPoint + " due to " + result);
            }
            File file = new File(mountPoint);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    protected String mount(String path, String parent, Integer nfsVersion) {
        String mountPoint = setupMountPoint(parent);
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }

        Script script = null;
        String result = null;
        Script command = new Script(true, "mount", _timeout, s_logger);
        command.add("-t", "nfs");
        if (nfsVersion != null){
            command.add("-o", "vers=" + nfsVersion);
        }
        // command.add("-o", "soft,timeo=133,retrans=2147483647,tcp,acdirmax=0,acdirmin=0");
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name"))) {
            command.add("-o", "resvport");
        }
        command.add(path);
        command.add(mountPoint);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to mount " + path + " due to " + result);
            File file = new File(mountPoint);
            if (file.exists()) {
                file.delete();
            }
            return null;
        }

        // Change permissions for the mountpoint
        script = new Script(true, "chmod", _timeout, s_logger);
        script.add("1777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
        }
        return mountPoint;
    }

    @DB
    private void updateClusterNativeHAState(Host host, StartupCommand cmd) {
        ClusterVO cluster = clusterDao.findById(host.getClusterId());
        if (cluster.getClusterType() == ClusterType.ExternalManaged) {
            if (cmd instanceof StartupRoutingCommand) {
                StartupRoutingCommand hostStartupCmd = (StartupRoutingCommand)cmd;
                Map<String, String> details = hostStartupCmd.getHostDetails();

                if (details.get("NativeHA") != null && details.get("NativeHA").equalsIgnoreCase("true")) {
                    clusterDetailsDao.persist(host.getClusterId(), "NativeHA", "true");
                } else {
                    clusterDetailsDao.persist(host.getClusterId(), "NativeHA", "false");
                }
            }
        }
    }

    @Override
    @DB
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        if (answers != null) {
            for (Answer answer : answers) {
                String execIdStr = answer.getContextParam("execid");
                if (execIdStr != null) {
                    long execId = 0;
                    try {
                        execId = Long.parseLong(execIdStr);
                    } catch (NumberFormatException e) {
                        assert (false);
                    }

                    _cmdExecLogDao.expunge(execId);
                }
            }
        }

        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) {
        if (cmd instanceof StartupCommand) {
            if (host.getHypervisorType() == HypervisorType.VMware) {
                updateClusterNativeHAState(host, cmd);
            } else {
                return;
            }
        }
    }

    protected final static int DEFAULT_DOMR_SSHPORT = 3922;

    protected boolean shutdownRouterVM(DomainRouterVO router) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Try to shutdown router VM " + router.getInstanceName() + " directly.");
        }

        Pair<Boolean, String> result;
        try {
            result = SshHelper.sshExecute(router.getPrivateIpAddress(), DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null, "poweroff -f");

            if (!result.first()) {
                s_logger.debug("Unable to shutdown " + router.getInstanceName() + " directly");
                return false;
            }
        } catch (Throwable e) {
            s_logger.warn("Unable to shutdown router " + router.getInstanceName() + " directly.");
            return false;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Shutdown router " + router.getInstanceName() + " successful.");
        }
        return true;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
    public boolean beginExclusiveOperation(int timeOutSeconds) {
        return _exclusiveOpLock.lock(timeOutSeconds);
    }

    @Override
    public void endExclusiveOperation() {
        _exclusiveOpLock.unlock();
    }

    @Override
    public Pair<Integer, Integer> getAddiionalVncPortRange() {
        return new Pair<Integer, Integer>(_additionalPortRangeStart, _additionalPortRangeSize);
    }

    @Override
    public int getRouterExtraPublicNics() {
        return _routerExtraPublicNics;
    }

    @Override
    public Map<String, String> getNexusVSMCredentialsByClusterId(Long clusterId) {
        CiscoNexusVSMDeviceVO nexusVSM = null;
        ClusterVSMMapVO vsmMapVO = null;

        vsmMapVO = _vsmMapDao.findByClusterId(clusterId);
        long vsmId = 0;
        if (vsmMapVO != null) {
            vsmId = vsmMapVO.getVsmId();
            s_logger.info("vsmId is " + vsmId);
            nexusVSM = _nexusDao.findById(vsmId);
            s_logger.info("Fetching nexus vsm credentials from database.");
        } else {
            s_logger.info("Found empty vsmMapVO.");
            return null;
        }

        Map<String, String> nexusVSMCredentials = new HashMap<String, String>();
        if (nexusVSM != null) {
            nexusVSMCredentials.put("vsmip", nexusVSM.getipaddr());
            nexusVSMCredentials.put("vsmusername", nexusVSM.getUserName());
            nexusVSMCredentials.put("vsmpassword", nexusVSM.getPassword());
            s_logger.info("Successfully fetched the credentials of Nexus VSM.");
        }
        return nexusVSMCredentials;
    }

    @Override
    public String getRootDiskController() {
        return _rootDiskController;
    }

    @Override
    public String getDataDiskController() {
        return _dataDiskController;
    }

    @Override
    public int getVcenterSessionTimeout() {
        return _vCenterSessionTimeout;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddVmwareDcCmd.class);
        cmdList.add(UpdateVmwareDcCmd.class);
        cmdList.add(RemoveVmwareDcCmd.class);
        cmdList.add(ListVmwareDcsCmd.class);
        return cmdList;
    }

    @Override
    @DB
    public VmwareDatacenterVO addVmwareDatacenter(AddVmwareDcCmd cmd) throws ResourceInUseException {
        VmwareDatacenterVO vmwareDc = null;
        Long zoneId = cmd.getZoneId();
        String userName = cmd.getUsername();
        String password = cmd.getPassword();
        String vCenterHost = cmd.getVcenter();
        String vmwareDcName = cmd.getName();

        // Validate username, password, VMware DC name and vCenter
        if (userName == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter username.");
        }

        if (password == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter username.");
        }

        if (vmwareDcName == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter name. Please provide valid VMware datacenter name.");
        }

        if (vCenterHost == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter name. "
                    + "Please provide valid VMware vCenter server's IP address or fully qualified domain name.");
        }

        if (zoneId == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter name. " + "Please provide valid zone id.");
        }

        // Zone validation
        validateZone(zoneId);

        VmwareDatacenterZoneMapVO vmwareDcZoneMap = vmwareDatacenterZoneMapDao.findByZoneId(zoneId);
        // Check if zone is associated with VMware DC
        if (vmwareDcZoneMap != null) {
            // Check if the associated VMware DC matches the one specified in API params
            // This check would yield success as the association exists between same entities (zone and VMware DC)
            // This scenario would result in if the API addVmwareDc is called more than once with same parameters.
            Long associatedVmwareDcId = vmwareDcZoneMap.getVmwareDcId();
            VmwareDatacenterVO associatedVmwareDc = vmwareDcDao.findById(associatedVmwareDcId);
            if (associatedVmwareDc.getVcenterHost().equalsIgnoreCase(vCenterHost) && associatedVmwareDc.getVmwareDatacenterName().equalsIgnoreCase(vmwareDcName)) {
                s_logger.info("Ignoring API call addVmwareDc, because VMware DC " + vCenterHost + "/" + vmwareDcName +
                        " is already associated with specified zone with id " + zoneId);
                return associatedVmwareDc;
            } else {
                throw new CloudRuntimeException("Zone " + zoneId + " is already associated with a VMware datacenter. " +
                        "Only 1 VMware DC can be associated with a zone.");
            }
        }
        // Zone validation to check if the zone already has resources.
        // Association of VMware DC to zone is not allowed if zone already has resources added.
        validateZoneWithResources(zoneId, "add VMware datacenter to zone");

        // Check if DC is already part of zone
        // In that case vmware_data_center table should have the DC
        vmwareDc = vmwareDcDao.getVmwareDatacenterByGuid(vmwareDcName + "@" + vCenterHost);
        if (vmwareDc != null) {
            throw new ResourceInUseException("This DC is already part of other CloudStack zone(s). Cannot add this DC to more zones.");
        }

        VmwareContext context = null;
        DatacenterMO dcMo = null;
        String dcCustomFieldValue;
        boolean addDcCustomFieldDef = false;
        boolean dcInUse = false;
        String guid;
        ManagedObjectReference dcMor;
        try {
            context = VmwareContextFactory.create(vCenterHost, userName, password);

            // Check if DC exists on vCenter
            dcMo = new DatacenterMO(context, vmwareDcName);
            dcMor = dcMo.getMor();
            if (dcMor == null) {
                String msg = "Unable to find VMware DC " + vmwareDcName + " in vCenter " + vCenterHost + ". ";
                s_logger.error(msg);
                throw new InvalidParameterValueException(msg);
            }

            // Check if DC is already associated with another cloudstack deployment
            // Get custom field property cloud.zone over this DC
            guid = vmwareDcName + "@" + vCenterHost;

            dcCustomFieldValue = dcMo.getCustomFieldValue(CustomFieldConstants.CLOUD_ZONE);
            if (dcCustomFieldValue == null) {
                addDcCustomFieldDef = true;
            }
            dcInUse = Boolean.parseBoolean(dcCustomFieldValue);
            if (dcInUse) {
                throw new ResourceInUseException("This DC is being managed by other CloudStack deployment. Cannot add this DC to zone.");
            }

            // Add DC to database into vmware_data_center table
            vmwareDc = new VmwareDatacenterVO(guid, vmwareDcName, vCenterHost, userName, password);
            vmwareDc = vmwareDcDao.persist(vmwareDc);

            // Map zone with vmware datacenter
            vmwareDcZoneMap = new VmwareDatacenterZoneMapVO(zoneId, vmwareDc.getId());

            vmwareDcZoneMap = vmwareDatacenterZoneMapDao.persist(vmwareDcZoneMap);

            // Set custom field for this DC
            if (addDcCustomFieldDef) {
                dcMo.ensureCustomFieldDef(CustomFieldConstants.CLOUD_ZONE);
            }
            dcMo.setCustomFieldValue(CustomFieldConstants.CLOUD_ZONE, "true");

        } catch (Throwable e) {
            String msg = "Failed to add VMware DC to zone ";
            if (e instanceof RemoteException) {
                msg = "Encountered remote exception at vCenter. " + VmwareHelper.getExceptionMessage(e);
            } else {
                msg += "due to : " + e.getMessage();
            }
            throw new CloudRuntimeException(msg);
        } finally {
            if (context != null) {
                context.close();
            }
            context = null;
        }
        return vmwareDc;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_EDIT, eventDescription = "updating VMware datacenter")
    public VmwareDatacenter updateVmwareDatacenter(UpdateVmwareDcCmd cmd) {
        final Long zoneId = cmd.getZoneId();
        final String userName = cmd.getUsername();
        final String password = cmd.getPassword();
        final String vCenterHost = cmd.getVcenter();
        final String vmwareDcName = cmd.getName();
        final Boolean isRecursive = cmd.isRecursive();

        final VmwareDatacenterZoneMap vdcMap = vmwareDatacenterZoneMapDao.findByZoneId(zoneId);
        final VmwareDatacenterVO vmwareDc = vmwareDcDao.findById(vdcMap.getVmwareDcId());
        if (vmwareDc == null) {
            throw new CloudRuntimeException("VMWare datacenter does not exist by provided ID");
        }
        final String oldVCenterHost = vmwareDc.getVcenterHost();

        if (!Strings.isNullOrEmpty(userName)) {
            vmwareDc.setUser(userName);
        }
        if (!Strings.isNullOrEmpty(password)) {
            vmwareDc.setPassword(password);
        }
        if (!Strings.isNullOrEmpty(vCenterHost)) {
            vmwareDc.setVcenterHost(vCenterHost);
        }
        if (!Strings.isNullOrEmpty(vmwareDcName)) {
            vmwareDc.setVmwareDatacenterName(vmwareDcName);
        }
        vmwareDc.setGuid(String.format("%s@%s", vmwareDc.getVmwareDatacenterName(), vmwareDc.getVcenterHost()));

        return Transaction.execute(new TransactionCallback<VmwareDatacenter>() {
            @Override
            public VmwareDatacenter doInTransaction(TransactionStatus status) {
                if (vmwareDcDao.update(vmwareDc.getId(), vmwareDc)) {
                    if (isRecursive) {
                        for (final Cluster cluster : clusterDao.listByDcHyType(zoneId, Hypervisor.HypervisorType.VMware.toString())) {
                            final Map<String, String> clusterDetails = clusterDetailsDao.findDetails(cluster.getId());
                            clusterDetails.put("username", vmwareDc.getUser());
                            clusterDetails.put("password", vmwareDc.getPassword());
                            final String clusterUrl = clusterDetails.get("url");
                            if (!oldVCenterHost.equals(vmwareDc.getVcenterHost()) && !Strings.isNullOrEmpty(clusterUrl)) {
                                clusterDetails.put("url", clusterUrl.replace(oldVCenterHost, vmwareDc.getVcenterHost()));
                            }
                            clusterDetailsDao.persist(cluster.getId(), clusterDetails);
                        }
                        for (final Host host : hostDao.listAllHostsByZoneAndHypervisorType(zoneId, HypervisorType.VMware)) {
                            final Map<String, String> hostDetails = hostDetailsDao.findDetails(host.getId());
                            hostDetails.put("username", vmwareDc.getUser());
                            hostDetails.put("password", vmwareDc.getPassword());
                            final String hostGuid = hostDetails.get("guid");
                            if (!Strings.isNullOrEmpty(hostGuid)) {
                                hostDetails.put("guid", hostGuid.replace(oldVCenterHost, vmwareDc.getVcenterHost()));
                            }
                            hostDetailsDao.persist(host.getId(), hostDetails);
                        }
                    }
                    return vmwareDc;
                }
                return null;
            }
        });
    }

    @Override
    public boolean removeVmwareDatacenter(RemoveVmwareDcCmd cmd) throws ResourceInUseException {
        Long zoneId = cmd.getZoneId();
        // Validate Id of zone
        doesZoneExist(zoneId);
        // Zone validation to check if the zone already has resources.
        // Association of VMware DC to zone is not allowed if zone already has resources added.
        validateZoneWithResources(zoneId, "remove VMware datacenter to zone");

        // Get DC associated with this zone
        VmwareDatacenterVO vmwareDatacenter;
        String vmwareDcName;
        String vCenterHost;
        String userName;
        String password;
        DatacenterMO dcMo = null;
        final VmwareDatacenterZoneMapVO vmwareDcZoneMap = vmwareDatacenterZoneMapDao.findByZoneId(zoneId);
        // Check if zone is associated with VMware DC
        if (vmwareDcZoneMap == null) {
            throw new CloudRuntimeException("Zone " + zoneId + " is not associated with any VMware datacenter.");
        }

        final long vmwareDcId = vmwareDcZoneMap.getVmwareDcId();
        vmwareDatacenter = vmwareDcDao.findById(vmwareDcId);
        vmwareDcName = vmwareDatacenter.getVmwareDatacenterName();
        vCenterHost = vmwareDatacenter.getVcenterHost();
        userName = vmwareDatacenter.getUser();
        password = vmwareDatacenter.getPassword();
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // Remove the VMware datacenter entry in table vmware_data_center
                vmwareDcDao.remove(vmwareDcId);
                // Remove the map entry in table vmware_data_center_zone_map
                vmwareDatacenterZoneMapDao.remove(vmwareDcZoneMap.getId());
            }
        });

        // Construct context
        VmwareContext context = null;
        try {
            context = VmwareContextFactory.create(vCenterHost, userName, password);

            // Check if DC exists on vCenter
            try {
                dcMo = new DatacenterMO(context, vmwareDcName);
            } catch (Throwable t) {
                String msg = "Unable to find DC " + vmwareDcName + " in vCenter " + vCenterHost;
                s_logger.error(msg);
                throw new DiscoveryException(msg);
            }

            assert (dcMo != null);

            // Reset custom field property cloud.zone over this DC
            dcMo.setCustomFieldValue(CustomFieldConstants.CLOUD_ZONE, "false");
            s_logger.info("Sucessfully reset custom field property cloud.zone over DC " + vmwareDcName);
        } catch (Exception e) {
            String msg = "Unable to reset custom field property cloud.zone over DC " + vmwareDcName + " due to : " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        } finally {
            if (context != null) {
                context.close();
            }
            context = null;
        }
        return true;
    }

    private void validateZone(Long zoneId) throws InvalidParameterValueException {
        // Check if zone with specified id exists
        doesZoneExist(zoneId);
        // Check if zone is legacy zone
        if (isLegacyZone(zoneId)) {
            throw new InvalidParameterValueException("The specified zone is legacy zone. Adding VMware datacenter to legacy zone is not supported.");
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("The specified zone is not legacy zone.");
            }
        }
    }

    private void validateZoneWithResources(Long zoneId, String errStr) throws ResourceInUseException {
        // Check if zone has resources? - For now look for clusters
        List<ClusterVO> clusters = clusterDao.listByZoneId(zoneId);
        if (clusters != null && clusters.size() > 0) {
            // Look for VMware hypervisor.
            for (ClusterVO cluster : clusters) {
                if (cluster.getHypervisorType().equals(HypervisorType.VMware)) {
                    throw new ResourceInUseException("Zone has one or more clusters." + " Can't " + errStr + " which already has clusters.");
                }
            }
        }
    }

    @Override
    public boolean isLegacyZone(long dcId) {
        boolean isLegacyZone = false;
        LegacyZoneVO legacyZoneVo = legacyZoneDao.findByZoneId(dcId);
        if (legacyZoneVo != null) {
            isLegacyZone = true;
        }
        return isLegacyZone;
    }

    @Override
    public List<? extends VmwareDatacenter> listVmwareDatacenters(ListVmwareDcsCmd cmd) throws CloudRuntimeException, InvalidParameterValueException {
        Long zoneId = cmd.getZoneId();
        List<VmwareDatacenterVO> vmwareDcList = new ArrayList<VmwareDatacenterVO>();
        VmwareDatacenterZoneMapVO vmwareDcZoneMap;
        VmwareDatacenterVO vmwareDatacenter;
        long vmwareDcId;

        // Validate if zone id parameter passed to API is valid
        doesZoneExist(zoneId);

        // Check if zone is associated with VMware DC
        vmwareDcZoneMap = vmwareDatacenterZoneMapDao.findByZoneId(zoneId);
        if (vmwareDcZoneMap == null) {
            return null;
        }
        // Retrieve details of VMware DC associated with zone.
        vmwareDcId = vmwareDcZoneMap.getVmwareDcId();
        vmwareDatacenter = vmwareDcDao.findById(vmwareDcId);
        vmwareDcList.add(vmwareDatacenter);

        // Currently a zone can have only 1 VMware DC associated with.
        // Returning list of VmwareDatacenterVO objects, in-line with future requirements, if any, like participation of multiple VMware DCs in a zone.
        return vmwareDcList;
    }

    private void doesZoneExist(Long zoneId) throws InvalidParameterValueException {
        // Check if zone with specified id exists
        DataCenterVO zone = datacenterDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by the id specified.");
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Zone with id:[" + zoneId + "] exists.");
        }
    }

    @Override
    public boolean hasNexusVSM(Long clusterId) {
        ClusterVSMMapVO vsmMapVo = null;

        vsmMapVo = _vsmMapDao.findByClusterId(clusterId);
        if (vsmMapVo == null) {
            s_logger.info("There is no instance of Nexus 1000v VSM associated with this cluster [Id:" + clusterId + "] yet.");
            return false;
        }
        else {
            s_logger.info("An instance of Nexus 1000v VSM [Id:" + vsmMapVo.getVsmId() + "] associated with this cluster [Id:" + clusterId + "]");
            return true;
        }
    }

    private void startTemplateCleanJobSchedule() {
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("checking to see if we should schedule a job to search for fully cloned templates to clean-up");
        }
        if(StorageManager.StorageCleanupEnabled.value() &&
                StorageManager.TemplateCleanupEnabled.value() &&
                templateCleanupInterval.value() > 0) {
            try {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("scheduling job to search for fully cloned templates to clean-up once per " + templateCleanupInterval.value() + " minutes.");
                }
//                    futureTemplateCleanup =
                Runnable task = getCleanupFullyClonedTemplatesTask();
                templateCleanupScheduler.scheduleAtFixedRate(task,
                        templateCleanupInterval.value(),
                        templateCleanupInterval.value(),
                        TimeUnit.MINUTES);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("scheduled job to search for fully cloned templates to clean-up.");
                }
            } catch (RejectedExecutionException ree) {
                s_logger.error("job to search for fully cloned templates cannot be scheduled");
                s_logger.debug("job to search for fully cloned templates cannot be scheduled;", ree);
            } catch (NullPointerException npe) {
                s_logger.error("job to search for fully cloned templates is invalid");
                s_logger.debug("job to search for fully cloned templates is invalid;", npe);
            } catch (IllegalArgumentException iae) {
                s_logger.error("job to search for fully cloned templates is scheduled at invalid intervals");
                s_logger.debug("job to search for fully cloned templates is scheduled at invalid intervals;", iae);
            } catch (Exception e) {
                s_logger.error("job to search for fully cloned templates failed for unknown reasons");
                s_logger.debug("job to search for fully cloned templates failed for unknown reasons;", e);
            }
        }
    }

    /**
     * This task is to cleanup templates from primary storage that are otherwise not cleaned by the {@link com.cloud.storage.StorageManagerImpl.StorageGarbageCollector}.
     * it is called at regular intervals when storage.template.cleanup.enabled == true
     * It collect all templates that
     * - are deleted from cloudstack
     * - when vmware.create.full.clone == true and the entries for VMs having volumes on the primary storage in db table “user_vm_clone_setting” reads 'full'
     */
    private Runnable getCleanupFullyClonedTemplatesTask() {
        return new CleanupFullyClonedTemplatesTask(primaryStorageDao,
                templateDataStoreDao,
                templateDao,
                vmInstanceDao,
                cloneSettingDao,
                templateManager);
    }
}
