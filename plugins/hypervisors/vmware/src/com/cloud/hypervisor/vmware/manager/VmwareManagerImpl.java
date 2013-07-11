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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.ManagedObjectReference;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.vmware.LegacyZoneVO;
import com.cloud.hypervisor.vmware.VmwareCleanupMaid;
import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterService;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
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
import com.cloud.hypervisor.vmware.mo.TaskMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VmwareHostType;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.FileUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.DomainRouterVO;

import org.apache.cloudstack.api.command.admin.zone.AddVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.ListVmwareDcsCmd;
import org.apache.cloudstack.api.command.admin.zone.RemoveVmwareDcCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;


@Local(value = {VmwareManager.class, VmwareDatacenterService.class})
public class VmwareManagerImpl extends ManagerBase implements VmwareManager, VmwareStorageMount, Listener, VmwareDatacenterService {
    private static final Logger s_logger = Logger.getLogger(VmwareManagerImpl.class);

    private static final int STARTUP_DELAY = 60000; 				// 60 seconds
    private static final long DEFAULT_HOST_SCAN_INTERVAL = 600000; 	// every 10 minutes

    private long _hostScanInterval = DEFAULT_HOST_SCAN_INTERVAL;
    int _timeout;

    private String _instance;

    @Inject AgentManager _agentMgr;
    @Inject
    protected NetworkModel _netMgr;
    @Inject HostDao _hostDao;
    @Inject ClusterDao _clusterDao;
    @Inject ClusterDetailsDao _clusterDetailsDao;
    @Inject CommandExecLogDao _cmdExecLogDao;
    @Inject ClusterManager _clusterMgr;
    @Inject SecondaryStorageVmManager _ssvmMgr;
    @Inject DataStoreManager _dataStoreMgr;
    @Inject CiscoNexusVSMDeviceDao _nexusDao;
    @Inject ClusterVSMMapDao _vsmMapDao;
    @Inject ConfigurationDao _configDao;
    @Inject ConfigurationServer _configServer;
    @Inject HypervisorCapabilitiesDao _hvCapabilitiesDao;
    @Inject DataCenterDao _dcDao;
    @Inject VmwareDatacenterDao _vmwareDcDao;
    @Inject VmwareDatacenterZoneMapDao _vmwareDcZoneMapDao;
    @Inject LegacyZoneDao _legacyZoneDao;

    String _mountParent;
    StorageLayer _storage;
    String _privateNetworkVSwitchName = "vSwitch0";

    int _portsPerDvPortGroup = 256;
    boolean _nexusVSwitchActive;
    boolean _fullCloneFlag;
    String _serviceConsoleName;
    String _managemetPortGroupName;
    String _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();
    String _recycleHungWorker = "false";
    int _additionalPortRangeStart;
    int _additionalPortRangeSize;
    int _routerExtraPublicNics = 2;

    String _reserveCpu = "false";

    String _reserveMem = "false";

    String _rootDiskController = DiskControllerType.ide.toString();

    Map<String, String> _storageMounts = new HashMap<String, String>();

    Random _rand = new Random(System.currentTimeMillis());
    Gson _gson;

    VmwareStorageManager _storageMgr;
    GlobalLock _exclusiveOpLock = GlobalLock.getInternLock("vmware.exclusive.op");

    private final ScheduledExecutorService _hostScanScheduler = Executors.newScheduledThreadPool(
            1, new NamedThreadFactory("Vmware-Host-Scan"));

    public VmwareManagerImpl() {
        _gson = GsonHelper.getGsonLogger();
        _storageMgr = new VmwareStorageManagerImpl(this);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        s_logger.info("Configure VmwareManagerImpl, manager name: " + name);

        if(!_configDao.isPremium()) {
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
        _portsPerDvPortGroup = NumbersUtil.parseInt(_configDao.getValue(Config.VmwarePortsPerDVPortGroup.key()), _portsPerDvPortGroup);

        _serviceConsoleName = _configDao.getValue(Config.VmwareServiceConsole.key());
        if(_serviceConsoleName == null) {
            _serviceConsoleName = "Service Console";
        }

        _managemetPortGroupName = _configDao.getValue(Config.VmwareManagementPortGroup.key());
        if(_managemetPortGroupName == null) {
            _managemetPortGroupName = "Management Network";
        }

        _defaultSystemVmNicAdapterType = _configDao.getValue(Config.VmwareSystemVmNicDeviceType.key());
        if(_defaultSystemVmNicAdapterType == null)
            _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();

        _additionalPortRangeStart = NumbersUtil.parseInt(_configDao.getValue(Config.VmwareAdditionalVncPortRangeStart.key()), 59000);
        if(_additionalPortRangeStart > 65535) {
            s_logger.warn("Invalid port range start port (" + _additionalPortRangeStart + ") for additional VNC port allocation, reset it to default start port 59000");
            _additionalPortRangeStart = 59000;
        }

        _additionalPortRangeSize = NumbersUtil.parseInt(_configDao.getValue(Config.VmwareAdditionalVncPortRangeSize.key()), 1000);
        if(_additionalPortRangeSize < 0 || _additionalPortRangeStart + _additionalPortRangeSize > 65535) {
            s_logger.warn("Invalid port range size (" + _additionalPortRangeSize + " for range starts at " + _additionalPortRangeStart);
            _additionalPortRangeSize = Math.min(1000, 65535 - _additionalPortRangeStart);
        }

        _routerExtraPublicNics = NumbersUtil.parseInt(_configDao.getValue(Config.RouterExtraPublicNics.key()), 2);

        _reserveCpu = _configDao.getValue(Config.VmwareReserveCpu.key());
        if(_reserveCpu == null || _reserveCpu.isEmpty())
            _reserveCpu = "false";
        _reserveMem = _configDao.getValue(Config.VmwareReserveMem.key());
        if(_reserveMem == null || _reserveMem.isEmpty())
            _reserveMem = "false";

        _recycleHungWorker = _configDao.getValue(Config.VmwareRecycleHungWorker.key());
        if(_recycleHungWorker == null || _recycleHungWorker.isEmpty())
            _recycleHungWorker = "false";

        _rootDiskController = _configDao.getValue(Config.VmwareRootDiskControllerType.key());
        if(_rootDiskController == null || _rootDiskController.isEmpty())
            _rootDiskController = DiskControllerType.ide.toString();

        s_logger.info("Additional VNC port allocation range is settled at " + _additionalPortRangeStart + " to " + (_additionalPortRangeStart + _additionalPortRangeSize));

        value = _configDao.getValue("vmware.host.scan.interval");
        _hostScanInterval = NumbersUtil.parseLong(value, DEFAULT_HOST_SCAN_INTERVAL);
        s_logger.info("VmwareManagerImpl config - vmware.host.scan.interval: " + _hostScanInterval);

        ((VmwareStorageManagerImpl)_storageMgr).configure(params);

        _agentMgr.registerForHostEvents(this, true, true, true);

        s_logger.info("VmwareManagerImpl has been successfully configured");
        return true;
    }

    @Override
    public boolean start() {
        _hostScanScheduler.scheduleAtFixedRate(getHostScanTask(),
                STARTUP_DELAY, _hostScanInterval, TimeUnit.MILLISECONDS);

        startupCleanup(_mountParent);
        return true;
    }

    @Override
    public boolean stop() {
        _hostScanScheduler.shutdownNow();
        try {
            _hostScanScheduler.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

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
        if(firewallMo != null) {
            if(hostMo.getHostType() == VmwareHostType.ESX) {
                firewallMo.enableRuleset("vncServer");
                firewallMo.refreshFirewall();
            }
        }

        // prepare at least one network on the vswitch to enable OVF importing
        String vSwitchName = privateTrafficLabel;
        String vlanId = null;
        String vlanToken;
        String[] tokens = privateTrafficLabel.split(",");
        if(tokens.length >= 2) {
            vSwitchName = tokens[0].trim();
            vlanToken = tokens[1].trim();
            if (!vlanToken.isEmpty()) {
                vlanId = vlanToken;
            }
        }
        s_logger.info("Preparing network on host " + hostMo.getContext().toString() + " for " + privateTrafficLabel);
        //The management network is probably always going to be a physical network with vlans, so assume BroadcastDomainType VLAN
        HypervisorHostHelper.prepareNetwork(vSwitchName, "cloud.private", hostMo, vlanId, null, null, 180000, false, BroadcastDomainType.Vlan, null);
    }

    @Override
    public List<ManagedObjectReference> addHostToPodCluster(VmwareContext serviceContext, long dcId, Long podId, Long clusterId,
            String hostInventoryPath) throws Exception {
        ManagedObjectReference mor = null;
        if (serviceContext != null)
            mor = serviceContext.getHostMorByPath(hostInventoryPath);
        String privateTrafficLabel = null;
        privateTrafficLabel = serviceContext.getStockObject("privateTrafficLabel");
        if (privateTrafficLabel == null) {
            privateTrafficLabel = _privateNetworkVSwitchName;
        }

        if(mor != null) {
            List<ManagedObjectReference> returnedHostList = new ArrayList<ManagedObjectReference>();

            if(mor.getType().equals("ComputeResource")) {
                List<ManagedObjectReference> hosts = (List<ManagedObjectReference>)serviceContext.getVimClient().getDynamicProperty(mor, "host");
                assert(hosts != null && hosts.size() > 0);

                // For ESX host, we need to enable host firewall to allow VNC access
                HostMO hostMo = new HostMO(serviceContext, hosts.get(0));

                prepareHost(hostMo, privateTrafficLabel);
                returnedHostList.add(hosts.get(0));
                return returnedHostList;
            } else if(mor.getType().equals("ClusterComputeResource")) {
                List<ManagedObjectReference> hosts = (List<ManagedObjectReference>)serviceContext.getVimClient().getDynamicProperty(mor, "host");
                assert(hosts != null);

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

                for(ManagedObjectReference morHost: hosts) {
                    // For ESX host, we need to enable host firewall to allow VNC access
                    HostMO hostMo = new HostMO(serviceContext, morHost);
                    prepareHost(hostMo, privateTrafficLabel);
                    returnedHostList.add(morHost);
                }
                return returnedHostList;
            } else if(mor.getType().equals("HostSystem")) {
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

    @Deprecated
    private ManagedObjectReference addHostToVCenterCluster(VmwareContext serviceContext, ManagedObjectReference morCluster,
            String host, String userName, String password) throws Exception {

        VmwareClient vclient = serviceContext.getVimClient();
        ManagedObjectReference morHost = vclient.getDecendentMoRef(morCluster, "HostSystem", host);
        if(morHost == null) {
            HostConnectSpec hostSpec = new HostConnectSpec();
            hostSpec.setUserName(userName);
            hostSpec.setPassword(password);
            hostSpec.setHostName(host);
            hostSpec.setForce(true);		// forcely take over the host

            ManagedObjectReference morTask = serviceContext.getService().addHostTask(morCluster, hostSpec, true, null, null);
            boolean taskResult = vclient.waitForTask(morTask);
            if(!taskResult) {
                s_logger.error("Unable to add host " + host + " to vSphere cluster due to " + TaskMO.getTaskFailureInfo(serviceContext, morTask));
                throw new CloudRuntimeException("Unable to add host " + host + " to vSphere cluster due to " + taskResult);
            }
            serviceContext.waitForTaskProgressDone(morTask);

            // init morHost after it has been created
            morHost = vclient.getDecendentMoRef(morCluster, "HostSystem", host);
            if(morHost == null) {
                throw new CloudRuntimeException("Successfully added host into vSphere but unable to find it later on?!. Please make sure you are either using IP address or full qualified domain name for host");
            }
        }

        // For ESX host, we need to enable host firewall to allow VNC access
        HostMO hostMo = new HostMO(serviceContext, morHost);
        HostFirewallSystemMO firewallMo = hostMo.getHostFirewallSystemMO();
        if(firewallMo != null) {
            firewallMo.enableRuleset("vncServer");
            firewallMo.refreshFirewall();
        }
        return morHost;
    }

    @Override
    public String getSecondaryStorageStoreUrl(long dcId) {

        DataStore secStore = _dataStoreMgr.getImageStore(dcId);
        if(secStore != null)
            return secStore.getUri();

        return null;
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
        if(hostMo.getHostType() == VmwareHostType.ESXi)
            return  _managemetPortGroupName;
        return _serviceConsoleName;
    }

    @Override
    public void setupResourceStartupParams(Map<String, Object> params) {
        params.put("vmware.create.full.clone", _fullCloneFlag);
        params.put("service.console.name", _serviceConsoleName);
        params.put("management.portgroup.name", _managemetPortGroupName);
        params.put("vmware.reserve.cpu", _reserveCpu);
        params.put("vmware.reserve.mem", _reserveMem);
        params.put("vmware.root.disk.controller", _rootDiskController);
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
    public void prepareSecondaryStorageStore(String storageUrl) {
        String mountPoint = getMountPoint(storageUrl);

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            if(lock.lock(3600)) {
                try {
                    File patchFolder = new File(mountPoint + "/systemvm");
                    if(!patchFolder.exists()) {
                        if(!patchFolder.mkdirs()) {
                            String msg = "Unable to create systemvm folder on secondary storage. location: " + patchFolder.toString();
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    }

                    File srcIso = getSystemVMPatchIsoFile();
                    File destIso = new File(mountPoint + "/systemvm/" + getSystemVMIsoFileNameOnDatastore());
                    if(!destIso.exists()) {
                        s_logger.info("Inject SSH key pairs before copying systemvm.iso into secondary storage");
                        _configServer.updateKeyPairs();

                        s_logger.info("Copy System VM patch ISO file to secondary storage. source ISO: " + srcIso.getAbsolutePath() +
                        	", destination: " + destIso.getAbsolutePath());
                        try {
                            FileUtil.copyfile(srcIso, destIso);
                        } catch(IOException e) {
                            s_logger.error("Unexpected exception ", e);

                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: " + srcIso.toString() + ", dest location: " + destIso;
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        if(s_logger.isTraceEnabled())
                            s_logger.trace("SystemVM ISO file " + destIso.getPath() + " already exists");
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

        if(isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/systemvm.iso");
        }

        assert(isoFile != null);
        if(!isoFile.exists()) {
        	s_logger.error("Unable to locate systemvm.iso in your setup at " + isoFile.toString());
        }
        return isoFile;
    }

    @Override
    public File getSystemVMKeyFile() {
        URL url = this.getClass().getClassLoader().getResource("scripts/vm/systemvm/id_rsa.cloud");
        File keyFile = null;
        if ( url != null ){
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            keyFile = new File("/usr/share/cloudstack-common/scripts/vm/systemvm/id_rsa.cloud");
        }
        assert(keyFile != null);
        if(!keyFile.exists()) {
        	s_logger.error("Unable to locate id_rsa.cloud in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    private Runnable getHostScanTask() {
        return new Runnable() {
            @Override
            public void run() {
                // TODO scan vSphere for newly added hosts.
                // we are going to both support adding host from CloudStack UI and
                // adding host via vSphere server
                //
                // will implement host scanning later
            }
        };
    }

    @Override
    public String getMountPoint(String storageUrl) {
        String mountPoint = null;
        synchronized(_storageMounts) {
            mountPoint = _storageMounts.get(storageUrl);
            if(mountPoint != null) {
                return mountPoint;
            }

            URI uri;
            try {
                uri = new URI(storageUrl);
            } catch (URISyntaxException e) {
                s_logger.error("Invalid storage URL format ", e);
                throw new CloudRuntimeException("Unable to create mount point due to invalid storage URL format " + storageUrl);
            }
            mountPoint = mount(uri.getHost() + ":" + uri.getPath(), _mountParent);
            if(mountPoint == null) {
                s_logger.error("Unable to create mount point for " + storageUrl);
                return "/mnt/sec"; // throw new CloudRuntimeException("Unable to create mount point for " + storageUrl);
            }

            _storageMounts.put(storageUrl, mountPoint);
            return mountPoint;
        }
    }

    private String setupMountPoint(String parent) {
        String mountPoint = null;
        long mshostId = _clusterMgr.getManagementNodeId();
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

        long mshostId = _clusterMgr.getManagementNodeId();

        // cleanup left-over NFS mounts from previous session
        String[] mounts = _storage.listFiles(parent + File.separator + String.valueOf(mshostId) + ".*");
        if(mounts != null && mounts.length > 0) {
            for(String mountPoint : mounts) {
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

        for(String mountPoint : _storageMounts.values()) {
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

    protected String mount(String path, String parent) {
        String mountPoint = setupMountPoint(parent);
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }

        Script script = null;
        String result = null;
        Script command = new Script(true, "mount", _timeout, s_logger);
        command.add("-t", "nfs");
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
        script.add("777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
            return null;
        }
        return mountPoint;
    }

    @DB
    private void updateClusterNativeHAState(Host host, StartupCommand cmd) {
        ClusterVO cluster = _clusterDao.findById(host.getClusterId());
        if(cluster.getClusterType() == ClusterType.ExternalManaged) {
            if(cmd instanceof StartupRoutingCommand) {
                StartupRoutingCommand hostStartupCmd = (StartupRoutingCommand)cmd;
                Map<String, String> details = hostStartupCmd.getHostDetails();

                if(details.get("NativeHA") != null && details.get("NativeHA").equalsIgnoreCase("true")) {
                    _clusterDetailsDao.persist(host.getClusterId(), "NativeHA", "true");
                } else {
                    _clusterDetailsDao.persist(host.getClusterId(), "NativeHA", "false");
                }
            }
        }
    }

    @Override @DB
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        if(answers != null) {
            for(Answer answer : answers) {
                String execIdStr = answer.getContextParam("execid");
                if(execIdStr != null) {
                    long execId = 0;
                    try {
                        execId = Long.parseLong(execIdStr);
                    } catch(NumberFormatException e) {
                        assert(false);
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
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) {
        if(cmd instanceof StartupCommand) {
            if(host.getHypervisorType() == HypervisorType.VMware) {
                updateClusterNativeHAState(host, cmd);
            } else {
                return;
            }
        }
    }

    protected final int DEFAULT_DOMR_SSHPORT = 3922;

    protected boolean shutdownRouterVM(DomainRouterVO router) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Try to shutdown router VM " + router.getInstanceName() + " directly.");
        }

        Pair<Boolean, String> result;
        try {
            result = SshHelper.sshExecute(router.getPrivateIpAddress(), DEFAULT_DOMR_SSHPORT, "root", getSystemVMKeyFile(), null,
                    "poweroff -f");

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
        }
        else {
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
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddVmwareDcCmd.class);
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
            throw new InvalidParameterValueException("Missing or invalid parameter name. " +
                    "Please provide valid VMware vCenter server's IP address or fully qualified domain name.");
        }

        if (zoneId == null) {
            throw new InvalidParameterValueException("Missing or invalid parameter name. " +
                    "Please provide valid zone id.");
        }

        // Zone validation
        validateZone(zoneId);

        VmwareDatacenterZoneMapVO vmwareDcZoneMap = _vmwareDcZoneMapDao.findByZoneId(zoneId);
        // Check if zone is associated with VMware DC
        if (vmwareDcZoneMap != null) {
            // Check if the associated VMware DC matches the one specified in API params
            // This check would yield success as the association exists between same entities (zone and VMware DC)
            // This scenario would result in if the API addVmwareDc is called more than once with same parameters.
            Long associatedVmwareDcId = vmwareDcZoneMap.getVmwareDcId();
            VmwareDatacenterVO associatedVmwareDc = _vmwareDcDao.findById(associatedVmwareDcId);
            if (associatedVmwareDc.getVcenterHost().equalsIgnoreCase(vCenterHost) &&
                    associatedVmwareDc.getVmwareDatacenterName().equalsIgnoreCase(vmwareDcName)) {
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
        vmwareDc = _vmwareDcDao.getVmwareDatacenterByGuid(vmwareDcName + "@" + vCenterHost);
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
            Transaction txn = Transaction.currentTxn();
            try {
                txn.start();
                vmwareDc = _vmwareDcDao.persist(vmwareDc);
                txn.commit();
            } catch (Exception e) {
                txn.rollback();
                s_logger.error("Failed to persist VMware datacenter details to database. Exception: " + e.getMessage());
                throw new CloudRuntimeException(e.getMessage());
            }

            // Map zone with vmware datacenter
            vmwareDcZoneMap = new VmwareDatacenterZoneMapVO(zoneId, vmwareDc.getId());

            txn = Transaction.currentTxn();
            try {
                txn.start();
                vmwareDcZoneMap = _vmwareDcZoneMapDao.persist(vmwareDcZoneMap);
                txn.commit();
            } catch (Exception e) {
                txn.rollback();
                s_logger.error("Failed to associate VMware datacenter with zone " + zoneId + ". Exception: " + e.getMessage());
                // Removing VMware datacenter from vmware_data_center table because association with zone failed.
                _vmwareDcDao.remove(vmwareDcZoneMap.getId());
                throw new CloudRuntimeException(e.getMessage());
            }

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
            if (context != null)
                context.close();
            context = null;
        }
        return vmwareDc;
    }


    @Override
    public boolean removeVmwareDatacenter(RemoveVmwareDcCmd cmd) throws ResourceInUseException {
        Long zoneId = cmd.getZoneId();
        // Validate zone
        validateZone(zoneId);
        // Zone validation to check if the zone already has resources.
        // Association of VMware DC to zone is not allowed if zone already has resources added.
        validateZoneWithResources(zoneId, "remove VMware datacenter to zone");

        // Get DC associated with this zone
        VmwareDatacenterZoneMapVO vmwareDcZoneMap;
        VmwareDatacenterVO vmwareDatacenter;
        String vmwareDcName;
        long vmwareDcId;
        String vCenterHost;
        String userName;
        String password;
        DatacenterMO dcMo = null;
        Transaction txn;

        vmwareDcZoneMap = _vmwareDcZoneMapDao.findByZoneId(zoneId);
        // Check if zone is associated with VMware DC
        if (vmwareDcZoneMap == null) {
            throw new CloudRuntimeException("Zone " + zoneId + " is not associated with any VMware datacenter.");
        }

        vmwareDcId = vmwareDcZoneMap.getVmwareDcId();
        vmwareDatacenter = _vmwareDcDao.findById(vmwareDcId);
        vmwareDcName = vmwareDatacenter.getVmwareDatacenterName();
        vCenterHost = vmwareDatacenter.getVcenterHost();
        userName = vmwareDatacenter.getUser();
        password = vmwareDatacenter.getPassword();
        txn = Transaction.currentTxn();
        try {
            txn.start();
            // Remove the VMware datacenter entry in table vmware_data_center
            _vmwareDcDao.remove(vmwareDcId);
            // Remove the map entry in table vmware_data_center_zone_map
            _vmwareDcZoneMapDao.remove(vmwareDcZoneMap.getId());
            txn.commit();
        } catch (Exception e) {
            s_logger.info("Caught exception when trying to delete VMware datacenter record." + e.getMessage());
            throw new CloudRuntimeException("Failed to delete VMware datacenter.");
        }

        // Construct context
        VmwareContext context = null;
        try {
            context = VmwareContextFactory.create(vCenterHost, userName, password);

            // Check if DC exists on vCenter
            try {
                dcMo = new DatacenterMO(context, vmwareDcName);
            } catch(Throwable t) {
                String msg = "Unable to find DC " + vmwareDcName + " in vCenter " + vCenterHost;
                s_logger.error(msg);
                throw new DiscoveryException(msg);
            }

            assert (dcMo != null);

            // Reset custom field property cloud.zone over this DC
            dcMo.setCustomFieldValue(CustomFieldConstants.CLOUD_ZONE, "false");
            s_logger.info("Sucessfully reset custom field property cloud.zone over DC " + vmwareDcName);
        } catch (Exception e) {
            String msg = "Unable to reset custom field property cloud.zone over DC " + vmwareDcName
                    + " due to : " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        } finally {
            if (context != null)
                context.close();
            context = null;
        }
        return true;
    }

    private void validateZone(Long zoneId) throws InvalidParameterValueException {
        // Check if zone with specified id exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Can't find zone by the id specified.");
            throw ex;
        }
    }

    private void validateZoneWithResources(Long zoneId, String errStr) throws ResourceInUseException {
        // Check if zone has resources? - For now look for clusters
        List<ClusterVO> clusters = _clusterDao.listByZoneId(zoneId);
        if (clusters != null && clusters.size() > 0) {
            // Look for VMware hypervisor.
            for (ClusterVO cluster : clusters) {
                if (cluster.getHypervisorType().equals(HypervisorType.VMware)) {
                    throw new ResourceInUseException("Zone has one or more clusters."
                            + " Can't " + errStr + " which already has clusters.");
                }
            }
        }
    }

    @Override
    public boolean isLegacyZone(long dcId) {
        boolean isLegacyZone = false;
        LegacyZoneVO legacyZoneVo = _legacyZoneDao.findById(dcId);
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
        validateZone(zoneId);

        // Check if zone is associated with VMware DC
        vmwareDcZoneMap = _vmwareDcZoneMapDao.findByZoneId(zoneId);
        if (vmwareDcZoneMap == null) {
            return null;
        }
        // Retrieve details of VMware DC associated with zone.
        vmwareDcId = vmwareDcZoneMap.getVmwareDcId();
        vmwareDatacenter = _vmwareDcDao.findById(vmwareDcId);
        vmwareDcList.add(vmwareDatacenter);

        // Currently a zone can have only 1 VMware DC associated with.
        // Returning list of VmwareDatacenterVO objects, in-line with future requirements, if any, like participation of multiple VMware DCs in a zone.
        return vmwareDcList;
    }
}
