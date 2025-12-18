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
package com.cloud.agent.manager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.GetGPUStatsAnswer;
import com.cloud.agent.api.GetGPUStatsCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.api.commands.SimulatorAddSecondaryAgent;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.AgentResourceBase;
import com.cloud.resource.AgentRoutingResource;
import com.cloud.resource.AgentStorageResource;
import com.cloud.resource.Discoverer;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.SimulatorDiscoverer;
import com.cloud.resource.SimulatorSecondaryDiscoverer;
import com.cloud.simulator.MockGpuDevice;
import com.cloud.simulator.MockGpuDeviceVO;
import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockHostVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.dao.MockGpuDeviceDao;
import com.cloud.simulator.dao.MockHostDao;
import com.cloud.simulator.dao.MockVMDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.ca.SetupCertificateAnswer;
import org.apache.cloudstack.ca.SetupCertificateCommand;
import org.apache.cloudstack.ca.SetupKeyStoreCommand;
import org.apache.cloudstack.ca.SetupKeystoreAnswer;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.diagnostics.DiagnosticsAnswer;
import org.apache.cloudstack.diagnostics.DiagnosticsCommand;
import org.apache.cloudstack.gpu.GpuDevice;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

@Component
public class MockAgentManagerImpl extends ManagerBase implements MockAgentManager {
    @Inject
    DataCenterDao dcDao;
    @Inject
    HostPodDao _podDao = null;
    @Inject
    MockHostDao _mockHostDao = null;
    @Inject
    MockVMDao _mockVmDao = null;
    @Inject
    MockGpuDeviceDao _mockGpuDeviceDao = null;
    @Inject
    SimulatorManager _simulatorMgr = null;
    @Inject
    AgentManager _agentMgr = null;
    @Inject
    MockStorageManager _storageMgr = null;
    @Inject
    ResourceManager _resourceMgr;
    @Inject private AccountManager _accountMgr;

    SimulatorDiscoverer discoverer;
    SimulatorSecondaryDiscoverer ssDiscoverer;
    @Inject
    HostDao hostDao;

    List<Discoverer> discoverers;

    private SecureRandom random;
    private final Map<String, AgentResourceBase> _resources = new ConcurrentHashMap<String, AgentResourceBase>();
    private ThreadPoolExecutor _executor;

    private Pair<String, Long> getPodCidr(long podId, long dcId) {
        try {
            DataCenterVO zone = dcDao.findById(dcId);
            if (DataCenter.Type.Edge.equals(zone.getType())) {
                String subnet = String.format("172.%d.%d.0", random.nextInt(15) + 16, random.nextInt(6) + 1);
                logger.info(String.format("Pod belongs to an edge zone hence CIDR cannot be found, returning %s/24", subnet));
                return new Pair<>(subnet, 24L);
            }
            HashMap<Long, List<Object>> podMap = _podDao.getCurrentPodCidrSubnets(dcId, 0);
            List<Object> cidrPair = podMap.get(podId);
            String cidrAddress = (String)cidrPair.get(0);
            Long cidrSize = (Long)cidrPair.get(1);
            return new Pair<String, Long>(cidrAddress, cidrSize);
        } catch (PatternSyntaxException e) {
            logger.error("Exception while splitting pod cidr");
            return null;
        } catch (IndexOutOfBoundsException e) {
            logger.error("Invalid pod cidr. Please check");
            return null;
        }
    }

    private String getIpAddress(long instanceId, long dcId, long podId) {
        Pair<String, Long> cidr = this.getPodCidr(podId, dcId);
        return NetUtils.long2Ip(NetUtils.ip2Long(cidr.first()) + instanceId);
    }

    private String getMacAddress(long dcId, long podId, long clusterId, int instanceId) {
        return NetUtils.long2Mac((dcId << 40 + podId << 32 + clusterId << 24 + instanceId));
    }

    public synchronized int getNextAgentId(long cidrSize) {
        return random.nextInt((int)cidrSize);
    }

    @Override
    @DB
    public Map<AgentResourceBase, Map<String, String>> createServerResources(Map<String, Object> params) {

        Map<String, String> args = new HashMap<String, String>();
        Map<AgentResourceBase, Map<String, String>> newResources = new HashMap<AgentResourceBase, Map<String, String>>();
        AgentResourceBase agentResource;
        long cpuCore = Long.parseLong((String)params.get("cpucore"));
        long cpuSpeed = Long.parseLong((String)params.get("cpuspeed"));
        long memory = Long.parseLong((String)params.get("memory"));
        long localStorageSize = Long.parseLong((String)params.get("localstorage"));
        String arch = (String)params.get("arch");
        synchronized (this) {
            long dataCenterId = Long.parseLong((String)params.get("zone"));
            long podId = Long.parseLong((String)params.get("pod"));
            long clusterId = Long.parseLong((String)params.get("cluster"));
            long cidrSize = getPodCidr(podId, dataCenterId).second();

            int agentId = getNextAgentId(cidrSize);
            String ipAddress = getIpAddress(agentId, dataCenterId, podId);
            String macAddress = getMacAddress(dataCenterId, podId, clusterId, agentId);
            MockHostVO mockHost = new MockHostVO();
            mockHost.setDataCenterId(dataCenterId);
            mockHost.setPodId(podId);
            mockHost.setClusterId(clusterId);
            mockHost.setCapabilities("hvm");
            mockHost.setCpuCount(cpuCore);
            mockHost.setCpuSpeed(cpuSpeed);
            mockHost.setMemorySize(memory);
            mockHost.setArch(arch);
            String guid = UUID.randomUUID().toString();
            mockHost.setGuid(guid);
            mockHost.setName("SimulatedAgent." + guid);
            mockHost.setPrivateIpAddress(ipAddress);
            mockHost.setPublicIpAddress(ipAddress);
            mockHost.setStorageIpAddress(ipAddress);
            mockHost.setPrivateMacAddress(macAddress);
            mockHost.setPublicMacAddress(macAddress);
            mockHost.setStorageMacAddress(macAddress);
            mockHost.setVersion(this.getClass().getPackage().getImplementationVersion());
            mockHost.setResource("com.cloud.agent.AgentRoutingResource");

            // Create different types of GPU devices to simulate KVM-like behavior
            // 1. Physical GPUs (passthrough only)
            // 2. Physical GPUs that support VF (Virtual Functions)
            // 3. Physical GPUs that support MDEV (Mediated Devices)

            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                mockHost = _mockHostDao.persist(mockHost);

                // Create physical GPU devices
                createPhysicalGpuDevices(mockHost.getId());

                txn.commit();
            } catch (Exception ex) {
                txn.rollback();
                logger.error("Error while configuring mock agent " + ex.getMessage());
                throw new CloudRuntimeException("Error configuring agent", ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }

            _storageMgr.getLocalStorage(guid, localStorageSize);

            agentResource = new AgentRoutingResource();
            if (agentResource != null) {
                try {
                    params.put("guid", mockHost.getGuid());
                    agentResource.start();
                    agentResource.configure(mockHost.getName(), params);

                    newResources.put(agentResource, args);
                } catch (ConfigurationException e) {
                    logger.error("error while configuring server resource" + e.getMessage());
                }
            }
        }
        return newResources;
    }

    /**
     * Creates different types of GPU devices to simulate KVM-like behavior
     *
     * @param hostId The host ID to associate the GPU devices with
     */
    private void createPhysicalGpuDevices(long hostId) {
        // 1. Create passthrough-only GPU devices across different NUMA nodes and PCI roots
        createPassthroughGpu(hostId, "00:01.0", "1234", "5678", "Simulator",
                "Graphics Card Basic", true, 0, "pci0000:00", 8192L, 4096L, 2160L, 1L);
        createPassthroughGpu(hostId, "00:02.0", "1234", "5678", "Simulator",
                "Graphics Card Basic", true, 0, "pci0000:00", 8192L, 4096L, 2160L, 1L);
        // Additional passthrough GPUs on different PCI roots
        createPassthroughGpu(hostId, "00:03.0", "1234", "5678", "Simulator",
                "Graphics Card Basic", true, 1, "pci0000:17", 8192L, 4096L, 2160L, 1L);
        createPassthroughGpu(hostId, "00:04.0", "1234", "5678", "Simulator",
                "Graphics Card Basic", true, 1, "pci0000:5d", 8192L, 4096L, 2160L, 1L);

        // 3. Create GPUs with MDEV support across different NUMA nodes
        MockGpuDeviceVO mdevParentDevice1 = createPassthroughGpu(hostId, "00:05.0", "1234", "89ab", "Simulator",
                "Graphics Card Pro", false, 0, "pci0000:00", 16384L, 4096L, 2160L, 1L);
        createMdevDevices(hostId, mdevParentDevice1);

        MockGpuDeviceVO mdevParentDevice2 = createPassthroughGpu(hostId, "00:06.0", "1234", "89ab", "Simulator",
                "Graphics Card Pro", false, 0, "pci0000:00", 16384L, 4096L, 2160L, 1L);
        createMdevDevices(hostId, mdevParentDevice2);

        MockGpuDeviceVO mdevParentDevice3 = createPassthroughGpu(hostId, "00:07.0", "1234", "89ab", "Simulator",
                "Graphics Card Pro", false, 1, "pci0000:17", 16384L, 4096L, 2160L, 1L);
        createMdevDevices(hostId, mdevParentDevice3);

        MockGpuDeviceVO mdevParentDevice4 = createPassthroughGpu(hostId, "00:08.0", "1234", "89ab", "Simulator",
                "Graphics Card Pro", false, 1, "pci0000:3a", 16384L, 4096L, 2160L, 1L);
        createMdevDevices(hostId, mdevParentDevice4);
    }

    /**
     * Creates a basic passthrough-only GPU device
     *
     * @return
     */
    private MockGpuDeviceVO createPassthroughGpu(long hostId, String busAddress, String vendorId, String deviceId,
                                      String vendorName, String deviceName, boolean passthroughEnabled, Integer numaNode, String pciRoot, Long videoRam, Long resolutionX, Long resolutionY, Long maxHeads) {
        MockGpuDeviceVO device = new MockGpuDeviceVO();
        device.setBusAddress(busAddress);
        device.setVendorId(vendorId);
        device.setDeviceId(deviceId);
        device.setVendorName(vendorName);
        device.setDeviceName(deviceName);
        device.setHostId(hostId);
        device.setState(MockGpuDevice.State.Available);
        device.setDeviceType(GpuDevice.DeviceType.PCI);
        device.setProfileName("passthrough");
        device.setPassthroughEnabled(passthroughEnabled);
        device.setNumaNode(numaNode);
        device.setPciRoot(pciRoot);
        device.setVideoRam(videoRam);
        device.setMaxResolutionX(resolutionX);
        device.setMaxResolutionY(resolutionY);
        device.setMaxHeads(maxHeads);
        return _mockGpuDeviceDao.persist(device);
    }

    /**
     * Creates MDEV devices with different profiles
     */
    private void createMdevDevices(long hostId, MockGpuDeviceVO parentDevice) {
        // Create MDEV devices with different profiles (similar to NVIDIA/Intel naming)
        String[] mdevProfiles = { "sim-8q", "sim-4q", "sim-2q", "sim-2q"};
        String[] mdevUuids = {UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                              UUID.randomUUID().toString()};
        Long[] videoRam = {8192L, 4096L, 2048L, 2048L};
        Long[] resoultionX = {4096L, 1920L, 1920L, 1920L};
        Long[] resoultionY = {2160L, 1080L, 1080L, 1080L};
        Long[] maxVgpuPerGpu = {2L, 4L, 8L, 8L};
        Long[] maxHeads = {4L, 2L, 1L, 1L};

        for (int i = 0; i < mdevProfiles.length; i++) {
            MockGpuDeviceVO mdevDevice = new MockGpuDeviceVO(mdevUuids[i], parentDevice.getVendorId(), parentDevice.getDeviceId(), parentDevice.getVendorName(), parentDevice.getDeviceName(), hostId );
            mdevDevice.setState(MockGpuDevice.State.Available);
            mdevDevice.setDeviceType(GpuDevice.DeviceType.MDEV);
            mdevDevice.setParentDeviceId(parentDevice.getId());
            mdevDevice.setProfileName(mdevProfiles[i]);
            mdevDevice.setPassthroughEnabled(true); // MDEV devices don't use passthrough
            mdevDevice.setMaxVgpuPerPgpu(maxVgpuPerGpu[i]);
            mdevDevice.setVideoRam(videoRam[i]);
            mdevDevice.setMaxResolutionX(resoultionX[i]);
            mdevDevice.setMaxResolutionY(resoultionY[i]);
            mdevDevice.setMaxHeads(maxHeads[i]);
            // MDEV devices inherit NUMA node and PCI root from parent
            mdevDevice.setNumaNode(parentDevice.getNumaNode());
            mdevDevice.setPciRoot(parentDevice.getPciRoot());
            _mockGpuDeviceDao.persist(mdevDevice);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            _executor = new ThreadPoolExecutor(1, 5, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("Simulator-Agent-Mgr"));
        } catch (NoSuchAlgorithmException e) {
            logger.debug("Failed to initialize random:" + e.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean handleSystemVMStart(long vmId, String privateIpAddress, String privateMacAddress, String privateNetMask, long dcId, long podId, String name,
        String vmType, String url) {
        _executor.execute(new SystemVMHandler(vmId, privateIpAddress, privateMacAddress, privateNetMask, dcId, podId, name, vmType, _simulatorMgr, url));
        return true;
    }

    @Override
    public boolean handleSystemVMStop(long vmId) {
        _executor.execute(new SystemVMHandler(vmId));
        return true;
    }

    private class SystemVMHandler implements Runnable {
        private final long vmId;
        private String privateIpAddress;
        private String privateMacAddress;
        private String privateNetMask;
        private long dcId;
        private long podId;
        private String guid;
        private String name;
        private String vmType;
        private SimulatorManager mgr;
        private final String mode;
        private String url;

        public SystemVMHandler(long vmId, String privateIpAddress, String privateMacAddress, String privateNetMask, long dcId, long podId, String name, String vmType,
                SimulatorManager mgr, String url) {
            this.vmId = vmId;
            this.privateIpAddress = privateIpAddress;
            this.privateMacAddress = privateMacAddress;
            this.privateNetMask = privateNetMask;
            this.dcId = dcId;
            this.guid = "SystemVM-" + UUID.randomUUID().toString();
            this.name = name;
            this.vmType = vmType;
            this.mgr = mgr;
            this.mode = "Start";
            this.url = url;
            this.podId = podId;
        }

        public SystemVMHandler(long vmId) {
            this.vmId = vmId;
            this.mode = "Stop";
        }

        private void handleSystemVMStop() {
            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                if (this.mode.equalsIgnoreCase("Stop")) {
                    txn.start();
                    MockHost host = _mockHostDao.findByVmId(this.vmId);
                    if (host != null) {
                        String guid = host.getGuid();
                        if (guid != null) {
                            AgentResourceBase res = _resources.get(guid);
                            if (res != null) {
                                res.stop();
                                _resources.remove(guid);
                            }
                        }
                    }
                    txn.commit();
                    return;
                }
            } catch (Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("Unable to get host " + guid + " due to " + ex.getMessage(), ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }

            //stop ssvm agent
            HostVO host = hostDao.findByGuid(this.guid);
            if (host != null) {
                try {
                    _resourceMgr.deleteHost(host.getId(), true, true);
                } catch (Exception e) {
                    logger.debug("Failed to delete host: ", e);
                }
            }
        }

        @Override
        @DB
        public void run() {
            CallContext.register(_accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
            if (this.mode.equalsIgnoreCase("Stop")) {
                handleSystemVMStop();
                CallContext.unregister();
                return;
            }

            String resource = null;
            if (vmType.equalsIgnoreCase("secstorage")) {
                resource = "com.cloud.agent.AgentStorageResource";
            }
            MockHostVO mockHost = new MockHostVO();
            mockHost.setDataCenterId(this.dcId);
            mockHost.setPodId(this.podId);
            mockHost.setCpuCount(DEFAULT_HOST_CPU_CORES);
            mockHost.setCpuSpeed(DEFAULT_HOST_SPEED_MHZ);
            mockHost.setMemorySize(DEFAULT_HOST_MEM_SIZE);
            mockHost.setGuid(this.guid);
            mockHost.setName(name);
            mockHost.setPrivateIpAddress(this.privateIpAddress);
            mockHost.setPublicIpAddress(this.privateIpAddress);
            mockHost.setStorageIpAddress(this.privateIpAddress);
            mockHost.setPrivateMacAddress(this.privateMacAddress);
            mockHost.setPublicMacAddress(this.privateMacAddress);
            mockHost.setStorageMacAddress(this.privateMacAddress);
            mockHost.setVersion(this.getClass().getPackage().getImplementationVersion());
            mockHost.setResource(resource);
            mockHost.setVmId(vmId);
            TransactionLegacy simtxn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                simtxn.start();
                mockHost = _mockHostDao.persist(mockHost);
                simtxn.commit();
            } catch (Exception ex) {
                simtxn.rollback();
                throw new CloudRuntimeException("Unable to persist host " + mockHost.getGuid() + " due to " + ex.getMessage(), ex);
            } finally {
                simtxn.close();
                simtxn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                simtxn.close();
            }

            if (vmType.equalsIgnoreCase("secstorage")) {
                AgentStorageResource storageResource = new AgentStorageResource();
                try {
                    Map<String, Object> params = new HashMap<String, Object>();
                    Map<String, String> details = new HashMap<String, String>();
                    params.put("guid", this.guid);
                    details.put("guid", this.guid);
                    storageResource.configure("secondaryStorage", params);
                    storageResource.start();
                    _resources.put(this.guid, storageResource);
                    ssDiscoverer.setResource(storageResource);
                    SimulatorAddSecondaryAgent cmd = new SimulatorAddSecondaryAgent("sim://" + this.guid, this.dcId);
                    try {
                        _resourceMgr.discoverHosts(cmd);
                    } catch (DiscoveryException e) {
                        logger.debug("Failed to discover host: " + e.toString());
                        CallContext.unregister();
                        return;
                    }
                } catch (ConfigurationException e) {
                    logger.debug("Failed to load secondary storage resource: " + e.toString());
                    CallContext.unregister();
                    return;
                }
            }
        }
    }

    @Override
    public MockHost getHost(String guid) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockHost _host = _mockHostDao.findByGuid(guid);
            txn.commit();
            if (_host != null) {
                return _host;
            } else {
                logger.error("Host with guid " + guid + " was not found");
                return null;
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to get host " + guid + " due to " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public GetHostStatsAnswer getHostStatistic(GetHostStatsCommand cmd) {
        String hostGuid = cmd.getHostGuid();
        MockHost host = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            host = _mockHostDao.findByGuid(hostGuid);
            txn.commit();
            if (host == null) {
                return null;
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to get host " + hostGuid + " due to " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        TransactionLegacy vmtxn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            vmtxn.start();
            List<MockVMVO> vms = _mockVmDao.findByHostId(host.getId());
            vmtxn.commit();
            double usedMem = 0.0;
            double usedCpu = 0.0;
            for (MockVMVO vm : vms) {
                usedMem += vm.getMemory();
                usedCpu += vm.getCpu();
            }

            HostStatsEntry hostStats = new HostStatsEntry();
            hostStats.setTotalMemoryKBs(host.getMemorySize());
            hostStats.setFreeMemoryKBs(host.getMemorySize() - usedMem);
            hostStats.setNetworkReadKBs(32768);
            hostStats.setNetworkWriteKBs(16384);
            hostStats.setCpuUtilization(usedCpu / (host.getCpuCount() * host.getCpuSpeed()));
            hostStats.setEntityType("simulator-host");
            hostStats.setHostId(cmd.getHostId());
            return new GetHostStatsAnswer(cmd, hostStats);
        } catch (Exception ex) {
            vmtxn.rollback();
            throw new CloudRuntimeException("Unable to get Vms on host " + host.getGuid() + " due to " + ex.getMessage(), ex);
        } finally {
            vmtxn.close();
            vmtxn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            vmtxn.close();
        }
    }

    @Override
    public Answer checkHealth(CheckHealthCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer getGpuStats(GetGPUStatsCommand cmd, long hostId) {
        return new GetGPUStatsAnswer(cmd, getGPUDevices(hostId));
    }

    @Override
    public List<VgpuTypesInfo> getGPUDevices(long hostId) {
        List<VgpuTypesInfo> gpuDevices = new ArrayList<>();
        List<MockGpuDeviceVO> mockGpuDevices;
        // List all mock GPU devices from database
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            mockGpuDevices = _mockGpuDeviceDao.listByHostId(hostId);

            if (mockGpuDevices != null && !mockGpuDevices.isEmpty()) {
                logger.debug("Found {} mock GPU devices in the database", mockGpuDevices.size());

                for (MockGpuDeviceVO mockGpuDevice : mockGpuDevices) {
                    String busAddress = mockGpuDevice.getBusAddress();
                    String vendorId = mockGpuDevice.getVendorId();
                    String deviceId = mockGpuDevice.getDeviceId();
                    String vendorName = mockGpuDevice.getVendorName();
                    String deviceName = mockGpuDevice.getDeviceName();
                    String modelName = mockGpuDevice.getProfileName();
                    boolean isPassthrough = mockGpuDevice.isPassthroughEnabled();
                    Long maxVgpuPerGpu = mockGpuDevice.getMaxVgpuPerPgpu();
                    Integer numaNode = mockGpuDevice.getNumaNode();
                    String pciRoot = mockGpuDevice.getPciRoot();

                    VgpuTypesInfo vgpuTypesInfo = new VgpuTypesInfo(mockGpuDevice.getDeviceType(),
                            vendorName + " " + deviceName, modelName, busAddress, vendorId, vendorName,
                            deviceId, deviceName, numaNode.toString(), pciRoot);
                    vgpuTypesInfo.setPassthroughEnabled(isPassthrough);
                    vgpuTypesInfo.setMaxVgpuPerGpu(maxVgpuPerGpu);
                    vgpuTypesInfo.setVideoRam(mockGpuDevice.getVideoRam());
                    vgpuTypesInfo.setMaxResolutionX(mockGpuDevice.getMaxResolutionX());
                    vgpuTypesInfo.setMaxResolutionY(mockGpuDevice.getMaxResolutionY());
                    vgpuTypesInfo.setMaxHeads(mockGpuDevice.getMaxHeads());

                    if (mockGpuDevice.getVmId() != null) {
                        MockVMVO mockVm = _mockVmDao.findById(mockGpuDevice.getVmId());
                        vgpuTypesInfo.setVmName(mockVm.getName());
                    }

                    if (mockGpuDevice.getParentDeviceId() != null) {
                        MockGpuDeviceVO parentDevice = _mockGpuDeviceDao.findById(mockGpuDevice.getParentDeviceId());
                        if (parentDevice != null) {
                            vgpuTypesInfo.setParentBusAddress(parentDevice.getBusAddress());
                        }
                    }
                    gpuDevices.add(vgpuTypesInfo);
                }
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to get GPU devices on hostId " + hostId + " due to " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return gpuDevices;
    }

    @Override
    public Answer pingTest(PingTestCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setupKeyStore(SetupKeyStoreCommand cmd) {
        return new SetupKeystoreAnswer(
                "-----BEGIN CERTIFICATE REQUEST-----\n" +
                "MIIBHjCByQIBADBkMQswCQYDVQQGEwJJTjELMAkGA1UECAwCSFIxETAPBgNVBAcM\n" +
                "CEd1cnVncmFtMQ8wDQYDVQQKDAZBcGFjaGUxEzARBgNVBAsMCkNsb3VkU3RhY2sx\n" +
                "DzANBgNVBAMMBnYtMS1WTTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQD46KFWKYrJ\n" +
                "F43Y1oqWUfrl4mj4Qm05Bgsi6nuigZv7ufiAKK0nO4iJKdRa2hFMUvBi2/bU3IyY\n" +
                "Nvg7cdJsn4K9AgMBAAGgADANBgkqhkiG9w0BAQUFAANBAIta9glu/ZSjA/ncyXix\n" +
                "yDOyAKmXXxsRIsdrEuIzakUuJS7C8IG0FjUbDyIaiwWQa5x+Lt4oMqCmpNqRzaGP\n" +
                "fOo=\n" + "-----END CERTIFICATE REQUEST-----");
    }

    @Override
    public Answer setupCertificate(SetupCertificateCommand cmd) {
        return new SetupCertificateAnswer(true);
    }


    @Override
    public boolean start() {
        for (Discoverer discoverer : discoverers) {
            if (discoverer instanceof SimulatorSecondaryDiscoverer) {
                this.ssDiscoverer = (SimulatorSecondaryDiscoverer)discoverer;
            }
            if (discoverer instanceof SimulatorDiscoverer) {
                this.discoverer = (SimulatorDiscoverer)discoverer;
            }
        }

        if (this.ssDiscoverer == null) {
            throw new IllegalStateException("Failed to find SimulatorSecondaryDiscoverer");
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public MaintainAnswer maintain(com.cloud.agent.api.MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    @Override
    public Answer checkNetworkCommand(CheckNetworkCommand cmd) {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking if network name setup is done on the resource");
        }
        return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");
    }

    @Override
    public Answer runDiagnostics(final DiagnosticsCommand cmd) {
        final String vmInstance = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final String[] args = cmd.getSrciptArguments().split(" ");
        final String mockAnswer = String.format("%s %s executed in %s &&  && 0", args[0].toUpperCase(), args[1], vmInstance);
        return new DiagnosticsAnswer(cmd, true, mockAnswer);
    }

    public List<Discoverer> getDiscoverers() {
        return discoverers;
    }

    @Inject
    public void setDiscoverers(List<Discoverer> discoverers) {
        this.discoverers = discoverers;
    }
}
