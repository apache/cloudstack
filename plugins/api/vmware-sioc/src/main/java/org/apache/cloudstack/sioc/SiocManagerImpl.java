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
package org.apache.cloudstack.sioc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.util.LoginInfo;
import org.apache.cloudstack.util.vmware.VMwareUtil;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMapVO;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfoBuilder;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.SharesInfo;
import com.vmware.vim25.SharesLevel;
import com.vmware.vim25.StorageIOAllocationInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;

@Component
public class SiocManagerImpl implements SiocManager {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final int LOCK_TIME_IN_SECONDS = 3;
    private static final int ONE_GB_IN_BYTES = 1000000000;
    private static final int LOWEST_SHARES_PER_VIRTUAL_DISK = 2000; // We want this to be greater than 1,000, which is the VMware default value.
    private static final int HIGHEST_SHARES_PER_VIRTUAL_DISK = 4000; // VMware limit
    private static final int LOWEST_LIMIT_IOPS_PER_VIRTUAL_DISK = 16; // VMware limit
    private static final int HIGHEST_LIMIT_IOPS_PER_VIRTUAL_DISK = 2147483647; // VMware limit

    @Inject private DataCenterDao zoneDao;
    @Inject private DiskOfferingDao diskOfferingDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private VMInstanceDao vmInstanceDao;
    @Inject private VmwareDatacenterDao vmwareDcDao;
    @Inject private VmwareDatacenterZoneMapDao vmwareDcZoneMapDao;
    @Inject private VolumeDao volumeDao;

    @Override
    public void updateSiocInfo(long zoneId, long storagePoolId, int sharesPerGB, int limitIopsPerGB, int iopsNotifyThreshold) throws Exception {
        logger.info("'SiocManagerImpl.updateSiocInfo(long, long, int, int, int)' method invoked");

        DataCenterVO zone = zoneDao.findById(zoneId);

        if (zone == null) {
            throw new Exception("Error: No zone could be located for the following zone ID: " + zoneId + ".");
        }

        StoragePoolVO storagePool = storagePoolDao.findById(storagePoolId);

        if (storagePool == null) {
            throw new Exception("Error: No storage pool could be located for the following pool ID: " + storagePoolId + ".");
        }

        if (storagePool.getDataCenterId() != zoneId) {
            throw new Exception("Error: Storage pool '" + storagePool.getName() + "' is not in zone ID " + zoneId + ".");
        }

        if (!storagePool.getPoolType().equals(StoragePoolType.VMFS)) {
            throw new Exception("Error: Storage pool '" + storagePool.getName() + "' does not represent a VMFS datastore.");
        }

        String lockName = zone.getUuid() + "-" + storagePool.getUuid();
        GlobalLock lock = GlobalLock.getInternLock(lockName);

        if (!lock.lock(LOCK_TIME_IN_SECONDS)) {
            throw new Exception("Busy: The system is already processing this request.");
        }

        VMwareUtil.VMwareConnection connection = null;

        try {
            connection = VMwareUtil.getVMwareConnection(getLoginInfo(zoneId));

            Map<String, ManagedObjectReference> nameToVm = VMwareUtil.getVms(connection);

            List<ManagedObjectReference> allTasks = new ArrayList<>();

            int limitIopsTotal = 0;

            List<VolumeVO> volumes = volumeDao.findByPoolId(storagePoolId, null);

            if (volumes != null && volumes.size() > 0) {
                Set<Long> instanceIds = new HashSet<>();

                for (VolumeVO volume : volumes) {
                    Long instanceId = volume.getInstanceId();

                    if (instanceId != null) {
                        instanceIds.add(instanceId);
                    }
                }

                for (Long instanceId : instanceIds) {
                    ResultWrapper resultWrapper = updateSiocInfo(connection, nameToVm, instanceId, storagePool, sharesPerGB, limitIopsPerGB);

                    limitIopsTotal += resultWrapper.getLimitIopsTotal();

                    allTasks.addAll(resultWrapper.getTasks());
                }
            }
            /*
            Set<String> vmNames = nameToVm.keySet();

            for (String vmName : vmNames) {
                // If the VM's name doesn't start with "i-", then it should be a worker VM (which is not stored in the CloudStack datastore).
                if (!vmName.startsWith("i-")) {
                    ResultWrapper resultWrapper = updateSiocInfoForWorkerVM(connection, nameToVm.get(vmName),
                            getDatastoreName(storagePool.getPath()), limitIopsPerGB);

                    limitIopsTotal += resultWrapper.getLimitIopsTotal();

                    allTasks.addAll(resultWrapper.getTasks());
                }
            }
            */
            for (ManagedObjectReference task : allTasks) {
                VMwareUtil.waitForTask(connection, task);
            }

            if (limitIopsTotal > iopsNotifyThreshold) {
                throw new Exception("Warning: Total number of IOPS: " + limitIopsTotal + "; IOPS notify threshold: " + iopsNotifyThreshold);
            }
        }
        finally {
            VMwareUtil.closeVMwareConnection(connection);

            lock.unlock();
            lock.releaseRef();
        }
    }

    private ResultWrapper updateSiocInfo(VMwareUtil.VMwareConnection connection, Map<String, ManagedObjectReference> nameToVm, Long instanceId,
                                         StoragePoolVO storagePool, int sharesPerGB, int limitIopsPerGB) throws Exception {
        int limitIopsTotal = 0;
        List<ManagedObjectReference> tasks = new ArrayList<>();

        VMInstanceVO vmInstance = vmInstanceDao.findById(instanceId);

        if (vmInstance == null) {
            String errMsg = "Error: The VM with ID " + instanceId + " could not be located.";

            throw new Exception(errMsg);
        }

        String vmName = vmInstance.getInstanceName();

        ManagedObjectReference morVm = nameToVm.get(vmName);

        if (morVm == null) {
            String errMsg = "Error: The VM with ID " + instanceId + " could not be located (ManagedObjectReference).";

            throw new Exception(errMsg);
        }

        VirtualMachineConfigInfo vmci = (VirtualMachineConfigInfo)VMwareUtil.getEntityProps(connection, morVm,
                new String[] { "config" }).get("config");
        List<VirtualDevice> devices = vmci.getHardware().getDevice();

        for (VirtualDevice device : devices) {
            if (device instanceof VirtualDisk) {
                VirtualDisk disk = (VirtualDisk)device;

                VolumeVO volumeVO = getVolumeFromVirtualDisk(vmInstance, storagePool.getId(), devices, disk);

                if (volumeVO != null) {
                    boolean diskUpdated = false;

                    StorageIOAllocationInfo sioai = disk.getStorageIOAllocation();

                    SharesInfo sharesInfo = sioai.getShares();

                    int currentShares = sharesInfo.getShares();
                    int newShares = getNewSharesBasedOnVolumeSize(volumeVO, sharesPerGB);

                    if (currentShares != newShares) {
                        sharesInfo.setLevel(SharesLevel.CUSTOM);
                        sharesInfo.setShares(newShares);

                        diskUpdated = true;
                    }

                    long currentLimitIops = sioai.getLimit() !=  null ? sioai.getLimit() : Long.MIN_VALUE;
                    long newLimitIops = getNewLimitIopsBasedOnVolumeSize(volumeVO, limitIopsPerGB);

                    limitIopsTotal += newLimitIops;

                    if (currentLimitIops != newLimitIops) {
                        sioai.setLimit(newLimitIops);

                        diskUpdated = true;
                    }

                    if (diskUpdated) {
                        VirtualDeviceConfigSpec vdcs = new VirtualDeviceConfigSpec();

                        vdcs.setDevice(disk);
                        vdcs.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

                        VirtualMachineConfigSpec vmcs = new VirtualMachineConfigSpec();

                        vmcs.getDeviceChange().add(vdcs);

                        try {
                            ManagedObjectReference task = VMwareUtil.reconfigureVm(connection, morVm, vmcs);

                            tasks.add(task);

                            logger.info(getInfoMsg(volumeVO, newShares, newLimitIops));
                        } catch (Exception ex) {
                            throw new Exception("Error: " + ex.getMessage());
                        }
                    }
                }
            }
        }

        return new ResultWrapper(limitIopsTotal, tasks);
    }

    private String getDatastoreName(String path) throws Exception {
        String searchString = "/";

        int lastIndexOf = path.lastIndexOf(searchString);

        if (lastIndexOf == -1) {
            throw new Exception("Error: Invalid datastore path");
        }

        return path.substring(lastIndexOf + searchString.length());
    }

    private ResultWrapper updateSiocInfoForWorkerVM(VMwareUtil.VMwareConnection connection, ManagedObjectReference morVm, String datastoreName,
                                                    int limitIopsPerGB) throws Exception {
        int limitIopsTotal = 0;
        List<ManagedObjectReference> tasks = new ArrayList<>();

        VirtualMachineConfigInfo vmci = (VirtualMachineConfigInfo)VMwareUtil.getEntityProps(connection, morVm,
                new String[] { "config" }).get("config");
        List<VirtualDevice> devices = vmci.getHardware().getDevice();

        for (VirtualDevice device : devices) {
            if (device instanceof VirtualDisk) {
                VirtualDisk disk = (VirtualDisk)device;

                if (disk.getBacking() instanceof VirtualDeviceFileBackingInfo) {
                    VirtualDeviceFileBackingInfo backingInfo = (VirtualDeviceFileBackingInfo)disk.getBacking();

                    if (backingInfo.getFileName().contains(datastoreName)) {
                        boolean diskUpdated = false;

                        StorageIOAllocationInfo sioai = disk.getStorageIOAllocation();

                        long currentLimitIops = sioai.getLimit() !=  null ? sioai.getLimit() : Long.MIN_VALUE;
                        long newLimitIops = getNewLimitIopsBasedOnVolumeSize(disk.getCapacityInBytes(), limitIopsPerGB);

                        limitIopsTotal += newLimitIops;

                        if (currentLimitIops != newLimitIops) {
                            sioai.setLimit(newLimitIops);

                            diskUpdated = true;
                        }

                        if (diskUpdated) {
                            VirtualDeviceConfigSpec vdcs = new VirtualDeviceConfigSpec();

                            vdcs.setDevice(disk);
                            vdcs.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

                            VirtualMachineConfigSpec vmcs = new VirtualMachineConfigSpec();

                            vmcs.getDeviceChange().add(vdcs);

                            try {
                                ManagedObjectReference task = VMwareUtil.reconfigureVm(connection, morVm, vmcs);

                                tasks.add(task);

                                logger.info(getInfoMsgForWorkerVm(newLimitIops));
                            } catch (Exception ex) {
                                throw new Exception("Error: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return new ResultWrapper(limitIopsTotal, tasks);
    }

    private String getInfoMsg(Volume volume, Integer newShares, Long newLimitIops) {
        String msgPrefix = "VMware SIOC: Volume = " + volume.getName();

        String msgNewShares = newShares != null ? "; New Shares = " + newShares : "";

        String msgNewLimitIops = newLimitIops != null ? "; New Limit IOPS = " + newLimitIops : "";

        return msgPrefix + msgNewShares + msgNewLimitIops;
    }

    private String getInfoMsgForWorkerVm(Long newLimitIops) {
        return "VMware SIOC: Worker VM's Limit IOPS set to " + newLimitIops;
    }

    private VolumeVO getVolumeFromVirtualDisk(VMInstanceVO vmInstance, long storagePoolId, List<VirtualDevice> allDevices,
            VirtualDisk disk) throws Exception {
        List<VolumeVO> volumes = volumeDao.findByInstance(vmInstance.getId());

        if (volumes == null || volumes.size() == 0) {
            String errMsg = "Error: The VMware virtual disk '" + disk + "' could not be mapped to a CloudStack volume. " +
                    "There were no volumes for the VM with the following ID: " + vmInstance.getId() + ".";

            throw new Exception(errMsg);
        }

        VirtualMachineDiskInfoBuilder diskInfoBuilder = VMwareUtil.getDiskInfoBuilder(allDevices);

        for (VolumeVO volume : volumes) {
            Long poolId = volume.getPoolId();

            if (poolId != null && poolId == storagePoolId) {
                StoragePoolVO storagePool = storagePoolDao.findById(poolId);
                String path = storagePool.getPath();
                String charToSearchFor = "/";
                int index = path.lastIndexOf(charToSearchFor) + charToSearchFor.length();
                String datastoreName = path.substring(index);
                VirtualMachineDiskInfo diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(volume.getPath(), datastoreName);

                if (diskInfo != null) {
                    String deviceBusName = VMwareUtil.getDeviceBusName(allDevices, disk);

                    if (deviceBusName.equals(diskInfo.getDiskDeviceBusName())) {
                        return volume;
                    }
                }
            }
        }

        return null;
    }

    private int getNewSharesBasedOnVolumeSize(VolumeVO volumeVO, int sharesPerGB) {
        long volumeSizeInBytes = getVolumeSizeInBytes(volumeVO);

        double sizeInGB = volumeSizeInBytes / (double)ONE_GB_IN_BYTES;

        int shares = LOWEST_SHARES_PER_VIRTUAL_DISK + ((int)(sharesPerGB * sizeInGB));

        return getAdjustedShares(shares);
    }

    private int getAdjustedShares(int shares) {
        shares = Math.max(shares, LOWEST_SHARES_PER_VIRTUAL_DISK);
        shares = Math.min(shares, HIGHEST_SHARES_PER_VIRTUAL_DISK);

        return shares;
    }

    private long getNewLimitIopsBasedOnVolumeSize(VolumeVO volumeVO, int limitIopsPerGB) {
        long volumeSizeInBytes = getVolumeSizeInBytes(volumeVO);

        return getNewLimitIopsBasedOnVolumeSize(volumeSizeInBytes, limitIopsPerGB);
    }

    private long getNewLimitIopsBasedOnVolumeSize(Long volumeSizeInBytes, int limitIopsPerGB) {
        if (volumeSizeInBytes == null) {
            volumeSizeInBytes = (long)ONE_GB_IN_BYTES;
        }

        double sizeInGB = volumeSizeInBytes / (double)ONE_GB_IN_BYTES;

        long limitIops = (long)(limitIopsPerGB * sizeInGB);

        return getAdjustedLimitIops(limitIops);
    }

    private long getAdjustedLimitIops(long limitIops) {
        limitIops = Math.max(limitIops, LOWEST_LIMIT_IOPS_PER_VIRTUAL_DISK);
        limitIops = Math.min(limitIops, HIGHEST_LIMIT_IOPS_PER_VIRTUAL_DISK);

        return limitIops;
    }

    private long getVolumeSizeInBytes(VolumeVO volumeVO) {
        return volumeVO.getSize() != null && volumeVO.getSize() > ONE_GB_IN_BYTES ? volumeVO.getSize() : ONE_GB_IN_BYTES;
    }

    private LoginInfo getLoginInfo(long zoneId) {
        VmwareDatacenterZoneMapVO vmwareDcZoneMap = vmwareDcZoneMapDao.findByZoneId(zoneId);
        Long associatedVmwareDcId = vmwareDcZoneMap.getVmwareDcId();
        VmwareDatacenterVO associatedVmwareDc = vmwareDcDao.findById(associatedVmwareDcId);

        String host = associatedVmwareDc.getVcenterHost();
        String username = associatedVmwareDc.getUser();
        String password = associatedVmwareDc.getPassword();

        return new LoginInfo(host, username, password);
    }
}

class ResultWrapper {
    private int limitIopsTotal;
    private List<ManagedObjectReference> tasks;

    ResultWrapper(int limitIopsTotal, List<ManagedObjectReference> tasks) {
        this.limitIopsTotal = limitIopsTotal;
        this.tasks = tasks;
    }

    int getLimitIopsTotal() {
        return limitIopsTotal;
    }

    List<ManagedObjectReference> getTasks() {
        return tasks;
    }
}
