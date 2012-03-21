/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.cluster.CheckPointManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManager;
import com.cloud.hypervisor.vmware.manager.VmwareStorageManagerImpl;
import com.cloud.hypervisor.vmware.manager.VmwareStorageMount;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.HostFirewallSystemMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HostVirtualNicType;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.TaskMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VmwareHostType;
import com.cloud.hypervisor.vmware.resource.SshHelper;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.FileUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.DomainRouterVO;
import com.google.gson.Gson;
import com.vmware.apputils.vim25.ServiceUtil;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.ManagedObjectReference;

@Local(value = {VmwareManager.class})
public class VmwareManagerImpl implements VmwareManager, VmwareStorageMount, Listener, Manager {
    private static final Logger s_logger = Logger.getLogger(VmwareManagerImpl.class);

    private static final int STARTUP_DELAY = 60000; 				// 60 seconds
    private static final long DEFAULT_HOST_SCAN_INTERVAL = 600000; 	// every 10 minutes

    private long _hostScanInterval = DEFAULT_HOST_SCAN_INTERVAL;
    int _timeout;

    private String _name;
    private String _instance;

    @Inject AgentManager _agentMgr;
    @Inject HostDao _hostDao;
    @Inject ClusterDao _clusterDao;
    @Inject ClusterDetailsDao _clusterDetailsDao;
    @Inject CommandExecLogDao _cmdExecLogDao;
    @Inject ClusterManager _clusterMgr;
    @Inject CheckPointManager _checkPointMgr;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject SecondaryStorageVmManager _ssvmMgr;
    
    ConfigurationServer _configServer;

    String _mountParent;
    StorageLayer _storage;

    String _privateNetworkVSwitchName;
    String _publicNetworkVSwitchName;
    String _guestNetworkVSwitchName;
    String _serviceConsoleName;
    String _managemetPortGroupName;
    String _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();
    String _recycleHungWorker = "false";
    int _additionalPortRangeStart;
    int _additionalPortRangeSize;
    int _maxHostsPerCluster;
    int _routerExtraPublicNics = 2;
    
    String _cpuOverprovisioningFactor = "1";
    String _reserveCpu = "false";
    
    String _memOverprovisioningFactor = "1";
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

        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        if(!configDao.isPremium()) {
            s_logger.error("Vmware component can only run under premium distribution");
            throw new ConfigurationException("Vmware component can only run under premium distribution");
        }

        _instance = configDao.getValue(Config.InstanceName.key());
        if (_instance == null) {
            _instance = "DEFAULT";
        }
        s_logger.info("VmwareManagerImpl config - instance.name: " + _instance);

        _mountParent = configDao.getValue(Config.MountParent.key());
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
            value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }

            try {
                Class<?> clazz = Class.forName(value);
                _storage = (StorageLayer)ComponentLocator.inject(clazz);
                _storage.configure("StorageLayer", params);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }

        _privateNetworkVSwitchName = configDao.getValue(Config.VmwarePrivateNetworkVSwitch.key());
        if(_privateNetworkVSwitchName == null) {
            _privateNetworkVSwitchName = "vSwitch0";
        }

        _publicNetworkVSwitchName = configDao.getValue(Config.VmwarePublicNetworkVSwitch.key());
        if(_publicNetworkVSwitchName == null) {
            _publicNetworkVSwitchName = "vSwitch0";
        }

        _guestNetworkVSwitchName =  configDao.getValue(Config.VmwareGuestNetworkVSwitch.key());
        if(_guestNetworkVSwitchName == null) {
            _guestNetworkVSwitchName = "vSwitch0";
        }

        _serviceConsoleName = configDao.getValue(Config.VmwareServiceConsole.key());
        if(_serviceConsoleName == null) {
            _serviceConsoleName = "Service Console";
        }
        
        _managemetPortGroupName = configDao.getValue(Config.VmwareManagementPortGroup.key());
        if(_managemetPortGroupName == null) {
        	_managemetPortGroupName = "Management Network";
        }
        
        _defaultSystemVmNicAdapterType = configDao.getValue(Config.VmwareSystemVmNicDeviceType.key());
        if(_defaultSystemVmNicAdapterType == null)
            _defaultSystemVmNicAdapterType = VirtualEthernetCardType.E1000.toString();
        
        _additionalPortRangeStart = NumbersUtil.parseInt(configDao.getValue(Config.VmwareAdditionalVncPortRangeStart.key()), 59000);
        if(_additionalPortRangeStart > 65535) {
        	s_logger.warn("Invalid port range start port (" + _additionalPortRangeStart + ") for additional VNC port allocation, reset it to default start port 59000");
        	_additionalPortRangeStart = 59000;
        }
        
        _additionalPortRangeSize = NumbersUtil.parseInt(configDao.getValue(Config.VmwareAdditionalVncPortRangeSize.key()), 1000);
        if(_additionalPortRangeSize < 0 || _additionalPortRangeStart + _additionalPortRangeSize > 65535) {
        	s_logger.warn("Invalid port range size (" + _additionalPortRangeSize + " for range starts at " + _additionalPortRangeStart);
        	_additionalPortRangeSize = Math.min(1000, 65535 - _additionalPortRangeStart);
        }
        
        _routerExtraPublicNics = NumbersUtil.parseInt(configDao.getValue(Config.RouterExtraPublicNics.key()), 2);
        
        _maxHostsPerCluster = NumbersUtil.parseInt(configDao.getValue(Config.VmwarePerClusterHostMax.key()), VmwareManager.MAX_HOSTS_PER_CLUSTER);
        _cpuOverprovisioningFactor = configDao.getValue(Config.CPUOverprovisioningFactor.key());
        if(_cpuOverprovisioningFactor == null || _cpuOverprovisioningFactor.isEmpty())
        	_cpuOverprovisioningFactor = "1";

        _memOverprovisioningFactor = configDao.getValue(Config.MemOverprovisioningFactor.key());
        if(_memOverprovisioningFactor == null || _memOverprovisioningFactor.isEmpty())
        	_memOverprovisioningFactor = "1";
        
        _reserveCpu = configDao.getValue(Config.VmwareReserveCpu.key());
        if(_reserveCpu == null || _reserveCpu.isEmpty())
        	_reserveCpu = "false";
        _reserveMem = configDao.getValue(Config.VmwareReserveMem.key());
        if(_reserveMem == null || _reserveMem.isEmpty())
        	_reserveMem = "false";
        
        _recycleHungWorker = configDao.getValue(Config.VmwareRecycleHungWorker.key());
        if(_recycleHungWorker == null || _recycleHungWorker.isEmpty())
            _recycleHungWorker = "false";
        
        _rootDiskController = configDao.getValue(Config.VmwareRootDiskControllerType.key());
        if(_rootDiskController == null || _rootDiskController.isEmpty())
        	_rootDiskController = DiskControllerType.ide.toString();
        
    	s_logger.info("Additional VNC port allocation range is settled at " + _additionalPortRangeStart + " to " + (_additionalPortRangeStart + _additionalPortRangeSize));

        value = configDao.getValue("vmware.host.scan.interval");
        _hostScanInterval = NumbersUtil.parseLong(value, DEFAULT_HOST_SCAN_INTERVAL);
        s_logger.info("VmwareManagerImpl config - vmware.host.scan.interval: " + _hostScanInterval);

        ((VmwareStorageManagerImpl)_storageMgr).configure(params);

        if(_configServer == null)
            _configServer = (ConfigurationServer)ComponentLocator.getComponent(ConfigurationServer.Name);
        
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
    public String getName() {
        return _name;
    }

    @Override
    public String composeWorkerName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public List<ManagedObjectReference> addHostToPodCluster(VmwareContext serviceContext, long dcId, Long podId, Long clusterId,
            String hostInventoryPath) throws Exception {
        ManagedObjectReference mor = serviceContext.getHostMorByPath(hostInventoryPath);
        if(mor != null) {
            List<ManagedObjectReference> returnedHostList = new ArrayList<ManagedObjectReference>();

            if(mor.getType().equals("ComputeResource")) {
                ManagedObjectReference[] hosts = (ManagedObjectReference[])serviceContext.getServiceUtil().getDynamicProperty(mor, "host");
                assert(hosts != null);

                // For ESX host, we need to enable host firewall to allow VNC access
                HostMO hostMo = new HostMO(serviceContext, hosts[0]);
                HostFirewallSystemMO firewallMo = hostMo.getHostFirewallSystemMO();
                if(firewallMo != null) {
            		if(hostMo.getHostType() == VmwareHostType.ESX) {
	                    firewallMo.enableRuleset("vncServer");
	                    firewallMo.refreshFirewall();
            		}
                }

                // prepare at least one network on the vswitch to enable OVF importing
                String managementPortGroupName = getManagementPortGroupByHost(hostMo);
                assert(managementPortGroupName != null);
                HostPortGroupSpec spec = hostMo.getPortGroupSpec(managementPortGroupName);
                String vlanId = null;
                if(spec.getVlanId() != 0) {
                    vlanId = String.valueOf(spec.getVlanId());
                }

                HypervisorHostHelper.prepareNetwork(_privateNetworkVSwitchName, "cloud.private", hostMo, vlanId, null, null, 180000, false);
                returnedHostList.add(hosts[0]);
                return returnedHostList;
            } else if(mor.getType().equals("ClusterComputeResource")) {
                ManagedObjectReference[] hosts = (ManagedObjectReference[])serviceContext.getServiceUtil().getDynamicProperty(mor, "host");
                assert(hosts != null);
                
                if(hosts.length > _maxHostsPerCluster) {
                	String msg = "vCenter cluster size is too big (current configured cluster size: " + _maxHostsPerCluster + ")";
                	s_logger.error(msg);
                	throw new DiscoveredWithErrorException(msg);
                }
                
                for(ManagedObjectReference morHost: hosts) {
                    // For ESX host, we need to enable host firewall to allow VNC access
                    HostMO hostMo = new HostMO(serviceContext, morHost);
                    HostFirewallSystemMO firewallMo = hostMo.getHostFirewallSystemMO();
                    if(firewallMo != null) {
                		if(hostMo.getHostType() == VmwareHostType.ESX) {
	                        firewallMo.enableRuleset("vncServer");
	                        firewallMo.refreshFirewall();
                		}
                    }

                    String managementPortGroupName = getManagementPortGroupByHost(hostMo);
                    assert(managementPortGroupName != null);
                    HostPortGroupSpec spec = hostMo.getPortGroupSpec(managementPortGroupName);
                    String vlanId = null;
                    if(spec.getVlanId() != 0) {
                        vlanId = String.valueOf(spec.getVlanId());
                    }

                    // prepare at least one network on the vswitch to enable OVF importing
                    HypervisorHostHelper.prepareNetwork(_privateNetworkVSwitchName, "cloud.private", hostMo, vlanId, null, null, 180000, false);
                    returnedHostList.add(morHost);
                }
                return returnedHostList;
            } else if(mor.getType().equals("HostSystem")) {
                // For ESX host, we need to enable host firewall to allow VNC access
                HostMO hostMo = new HostMO(serviceContext, mor);
                HostFirewallSystemMO firewallMo = hostMo.getHostFirewallSystemMO();
                if(firewallMo != null) {
            		if(hostMo.getHostType() == VmwareHostType.ESX) {
	                    firewallMo.enableRuleset("vncServer");
	                    firewallMo.refreshFirewall();
            		}
                }

                String managementPortGroupName = getManagementPortGroupByHost(hostMo);
                assert(managementPortGroupName != null);
                HostPortGroupSpec spec = hostMo.getPortGroupSpec(managementPortGroupName);
                String vlanId = null;
                if(spec.getVlanId() != 0) {
                    vlanId = String.valueOf(spec.getVlanId());
                }

                // prepare at least one network on the vswitch to enable OVF importing
                HypervisorHostHelper.prepareNetwork(_privateNetworkVSwitchName, "cloud.private", hostMo, vlanId, null, null, 180000, false);
                returnedHostList.add(mor);
                return returnedHostList;
            } else {
                s_logger.error("Unsupport host type " + mor.getType() + ":" + mor.get_value() + " from inventory path: " + hostInventoryPath);
                return null;
            }
        }

        s_logger.error("Unable to find host from inventory path: " + hostInventoryPath);
        return null;
    }

    @Deprecated
    private ManagedObjectReference addHostToVCenterCluster(VmwareContext serviceContext, ManagedObjectReference morCluster,
            String host, String userName, String password) throws Exception {

        ServiceUtil serviceUtil = serviceContext.getServiceUtil();
        ManagedObjectReference morHost = serviceUtil.getDecendentMoRef(morCluster, "HostSystem", host);
        if(morHost == null) {
            HostConnectSpec hostSpec = new HostConnectSpec();
            hostSpec.setUserName(userName);
            hostSpec.setPassword(password);
            hostSpec.setHostName(host);
            hostSpec.setForce(true);		// forcely take over the host

            ManagedObjectReference morTask = serviceContext.getService().addHost_Task(morCluster, hostSpec, true, null, null);
            String taskResult = serviceUtil.waitForTask(morTask);
            if(!taskResult.equals("sucess")) {
                s_logger.error("Unable to add host " + host + " to vSphere cluster due to " + TaskMO.getTaskFailureInfo(serviceContext, morTask));
                throw new CloudRuntimeException("Unable to add host " + host + " to vSphere cluster due to " + taskResult);
            }
            serviceContext.waitForTaskProgressDone(morTask);

            // init morHost after it has been created
            morHost = serviceUtil.getDecendentMoRef(morCluster, "HostSystem", host);
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
    	List<HostVO> secStorageHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(dcId);
    	if(secStorageHosts.size() > 0)
    		return secStorageHosts.get(0).getStorageUrl();
    	
        return null;
    }

	public String getServiceConsolePortGroupName() {
		return _serviceConsoleName;
	}
	
	public String getManagementPortGroupName() {
		return _managemetPortGroupName;
	}
    
    @Override
    public String getManagementPortGroupByHost(HostMO hostMo) throws Exception {
    	if(hostMo.getHostType() == VmwareHostType.ESXi)
    		return  this._managemetPortGroupName;
        return this._serviceConsoleName;
    }
    
    @Override
    public void setupResourceStartupParams(Map<String, Object> params) {
        params.put("private.network.vswitch.name", _privateNetworkVSwitchName);
        params.put("public.network.vswitch.name", _publicNetworkVSwitchName);
        params.put("guest.network.vswitch.name", _guestNetworkVSwitchName);
        params.put("service.console.name", _serviceConsoleName);
        params.put("management.portgroup.name", _managemetPortGroupName);
        params.put("cpu.overprovisioning.factor", _cpuOverprovisioningFactor);
        params.put("vmware.reserve.cpu", _reserveCpu);
        params.put("mem.overprovisioning.factor", _memOverprovisioningFactor);
        params.put("vmware.reserve.mem", _reserveMem);
        params.put("vmware.root.disk.controller", _rootDiskController);
        params.put("vmware.recycle.hung.wokervm", _recycleHungWorker);
    }

    @Override
    public VmwareStorageManager getStorageManager() {
        return _storageMgr;
    }

    
    @Override
	public long pushCleanupCheckpoint(String hostGuid, String vmName) {
        return _checkPointMgr.pushCheckPoint(new VmwareCleanupMaid(hostGuid, vmName));
    }
    
    @Override
	public void popCleanupCheckpoint(long checkpoint) {
    	_checkPointMgr.popCheckPoint(checkpoint);
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
        String version = ComponentLocator.class.getPackage().getImplementationVersion();
        String fileName = "systemvm-" + version + ".iso";
        return fileName.replace(':', '-');
    }
    
    @Override
    public String getSystemVMDefaultNicAdapterType() {
        return this._defaultSystemVmNicAdapterType;
    }
    
    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        URL url = ComponentLocator.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(url.getFile());
        File isoFile = new File(file.getParent() + "/vms/systemvm.iso");
        if (!isoFile.exists()) {
            isoFile = new File("/usr/lib64/cloud/agent/" + "/vms/systemvm.iso");
            if (!isoFile.exists()) {
                isoFile = new File("/usr/lib/cloud/agent/" + "/vms/systemvm.iso");
            }
        }
        return isoFile;
    }

    @Override
    public File getSystemVMKeyFile() {
        URL url = ComponentLocator.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(url.getFile());

        File keyFile = new File(file.getParent(), "/scripts/vm/systemvm/id_rsa.cloud");
        if (!keyFile.exists()) {
            keyFile = new File("/usr/lib64/cloud/agent" + "/scripts/vm/systemvm/id_rsa.cloud");
            if (!keyFile.exists()) {
                keyFile = new File("/usr/lib/cloud/agent" + "/scripts/vm/systemvm/id_rsa.cloud");
            }
        }
        return keyFile;
    }

    private Runnable getHostScanTask() {
        return new Runnable() {
            @Override
            public void run() {
                // TODO scan vSphere for newly added hosts.
                // we are going to both support adding host from Cloud.com UI and
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
                throw new CloudRuntimeException("Unable to create mount point for " + storageUrl);
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
    private void updateClusterNativeHAState(HostVO host, StartupCommand cmd) {
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

                String checkPointIdStr = answer.getContextParam("checkpoint");
                if(checkPointIdStr != null) {
                    _checkPointMgr.popCheckPoint(Long.parseLong(checkPointIdStr));
                }
                
                checkPointIdStr = answer.getContextParam("checkpoint2");
                if(checkPointIdStr != null) {
                    _checkPointMgr.popCheckPoint(Long.parseLong(checkPointIdStr));
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
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) {
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
    public int getMaxHostsPerCluster() {
    	return this._maxHostsPerCluster;
    }
    
    @Override
	public int getRouterExtraPublicNics() {
		return this._routerExtraPublicNics;
	}
}
