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
package com.cloud.capacity;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.cache.LazyCache;
import org.apache.cloudstack.utils.cache.SingleCache;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceListener;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ServerResource;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class CapacityManagerImpl extends ManagerBase implements CapacityManager, StateListener<State, VirtualMachine.Event, VirtualMachine>, Listener, ResourceListener,
        Configurable {
    @Inject
    CapacityDao _capacityDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ServiceOfferingDao _offeringsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VMTemplatePoolDao _templatePoolDao;
    @Inject
    AgentManager _agentManager;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    protected VMSnapshotDao _vmSnapshotDao;
    @Inject
    protected UserVmDao _userVMDao;
    @Inject
    protected VMInstanceDetailsDao _vmInstanceDetailsDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    DataStoreProviderManager _dataStoreProviderMgr;

    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    private int _vmCapacityReleaseInterval;
    long _extraBytesPerVolume = 0;

    @Inject
    MessageBus _messageBus;

    private LazyCache<Long, Pair<String, String>> clusterValuesCache;
    private SingleCache<Map<Long, ServiceOfferingVO>> serviceOfferingsCache;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _vmCapacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);

        VirtualMachine.State.getStateMachine().registerListener(this);
        _agentManager.registerForHostEvents(new StorageCapacityListener(_capacityDao, _storageMgr), true, false, false);
        _agentManager.registerForHostEvents(new ComputeCapacityListener(_capacityDao, this), true, false, false);

        return true;
    }

    @Override
    public boolean start() {
        _resourceMgr.registerResourceEvent(ResourceListener.EVENT_PREPARE_MAINTENANCE_AFTER, this);
        _resourceMgr.registerResourceEvent(ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER, this);
        clusterValuesCache = new LazyCache<>(128, 60, this::getClusterValues);
        serviceOfferingsCache = new SingleCache<>(60, this::getServiceOfferingsMap);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @DB
    @Override
    public boolean releaseVmCapacity(VirtualMachine vm, final boolean moveFromReserved, final boolean moveToReservered, final Long hostId) {
        if (hostId == null) {
            return true;
        }
        HostVO host = _hostDao.findById(hostId);
        return releaseVmCapacity(vm, moveFromReserved, moveToReservered, host);
    }

    @DB
    public boolean releaseVmCapacity(VirtualMachine vm, final boolean moveFromReserved, final boolean moveToReservered, final Host host) {
        if (host == null) {
            return true;
        }

        final ServiceOfferingVO svo = _offeringsDao.findById(vm.getId(), vm.getServiceOfferingId());
        CapacityVO capacityCpu = _capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_CPU);
        CapacityVO capacityMemory = _capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_MEMORY);
        CapacityVO capacityCpuCore = _capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_CPU_CORE);
        Long clusterId = host.getClusterId();
        if (capacityCpu == null || capacityMemory == null || svo == null || capacityCpuCore == null) {
            return false;
        }

        try {
            final Long clusterIdFinal = clusterId;
            final long capacityCpuId = capacityCpu.getId();
            final long capacityMemoryId = capacityMemory.getId();
            final long capacityCpuCoreId = capacityCpuCore.getId();

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    CapacityVO capacityCpu = _capacityDao.lockRow(capacityCpuId, true);
                    CapacityVO capacityMemory = _capacityDao.lockRow(capacityMemoryId, true);
                    CapacityVO capacityCpuCore = _capacityDao.lockRow(capacityCpuCoreId, true);

                    long usedCpu = capacityCpu.getUsedCapacity();
                    long usedMem = capacityMemory.getUsedCapacity();
                    long usedCpuCore = capacityCpuCore.getUsedCapacity();
                    long reservedCpu = capacityCpu.getReservedCapacity();
                    long reservedMem = capacityMemory.getReservedCapacity();
                    long reservedCpuCore = capacityCpuCore.getReservedCapacity();
                    long actualTotalCpu = capacityCpu.getTotalCapacity();
                    float cpuOvercommitRatio = Float.parseFloat(_clusterDetailsDao.findDetail(clusterIdFinal, VmDetailConstants.CPU_OVER_COMMIT_RATIO).getValue());
                    float memoryOvercommitRatio = Float.parseFloat(_clusterDetailsDao.findDetail(clusterIdFinal, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO).getValue());
                    int vmCPU = svo.getCpu() * svo.getSpeed();
                    int vmCPUCore = svo.getCpu();
                    long vmMem = svo.getRamSize() * 1024L * 1024L;
                    long actualTotalMem = capacityMemory.getTotalCapacity();
                    long totalMem = (long)(actualTotalMem * memoryOvercommitRatio);
                    long totalCpu = (long)(actualTotalCpu * cpuOvercommitRatio);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Hosts's actual total CPU: " + actualTotalCpu + " and CPU after applying overprovisioning: " + totalCpu);
                        logger.debug("Hosts's actual total RAM: " + toHumanReadableSize(actualTotalMem) + " and RAM after applying overprovisioning: " + toHumanReadableSize(totalMem));
                    }

                    if (!moveFromReserved) {
                        /* move resource from used */
                        if (usedCpu >= vmCPU) {
                            capacityCpu.setUsedCapacity(usedCpu - vmCPU);
                        }
                        if (usedMem >= vmMem) {
                            capacityMemory.setUsedCapacity(usedMem - vmMem);
                        }
                        if (usedCpuCore >= vmCPUCore) {
                            capacityCpuCore.setUsedCapacity(usedCpuCore - vmCPUCore);
                        }

                        if (moveToReservered) {
                            if (reservedCpu + vmCPU <= totalCpu) {
                                capacityCpu.setReservedCapacity(reservedCpu + vmCPU);
                            }
                            if (reservedMem + vmMem <= totalMem) {
                                capacityMemory.setReservedCapacity(reservedMem + vmMem);
                            }
                            capacityCpuCore.setReservedCapacity(reservedCpuCore + vmCPUCore);
                        }
                    } else {
                        if (reservedCpu >= vmCPU) {
                            capacityCpu.setReservedCapacity(reservedCpu - vmCPU);
                        }
                        if (reservedMem >= vmMem) {
                            capacityMemory.setReservedCapacity(reservedMem - vmMem);
                        }
                        if (reservedCpuCore >= vmCPUCore) {
                            capacityCpuCore.setReservedCapacity(reservedCpuCore - vmCPUCore);
                        }
                    }

                    logger.debug("release cpu from host: {}, old used: {}, " +
                            "reserved: {}, actual total: {}, total with overprovisioning: {}; " +
                            "new used: {},reserved:{}; movedfromreserved: {},moveToReservered: {}", host, usedCpu, reservedCpu, actualTotalCpu, totalCpu, capacityCpu.getUsedCapacity(), capacityCpu.getReservedCapacity(), moveFromReserved, moveToReservered);

                    logger.debug("release mem from host: {}, old used: {}, " +
                            "reserved: {}, total: {}; new used: {}, reserved: {}; " +
                            "movedfromreserved: {}, moveToReservered: {}", host, toHumanReadableSize(usedMem), toHumanReadableSize(reservedMem), toHumanReadableSize(totalMem), toHumanReadableSize(capacityMemory.getUsedCapacity()), toHumanReadableSize(capacityMemory.getReservedCapacity()), moveFromReserved, moveToReservered);

                    _capacityDao.update(capacityCpu.getId(), capacityCpu);
                    _capacityDao.update(capacityMemory.getId(), capacityMemory);
                    _capacityDao.update(capacityCpuCore.getId(), capacityCpuCore);
                }
            });

            return true;
        } catch (Exception e) {
            logger.debug("Failed to transit vm's state, due to " + e.getMessage());
            return false;
        }
    }

    @DB
    @Override
    public void allocateVmCapacity(VirtualMachine vm, final boolean fromLastHost) {

        final long hostId = vm.getHostId();
        final HostVO host = _hostDao.findById(hostId);
        final long clusterId = host.getClusterId();
        final float cpuOvercommitRatio = Float.parseFloat(_clusterDetailsDao.findDetail(clusterId, VmDetailConstants.CPU_OVER_COMMIT_RATIO).getValue());
        final float memoryOvercommitRatio = Float.parseFloat(_clusterDetailsDao.findDetail(clusterId, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO).getValue());

        final ServiceOfferingVO svo = _offeringsDao.findById(vm.getId(), vm.getServiceOfferingId());

        CapacityVO capacityCpu = _capacityDao.findByHostIdType(hostId, Capacity.CAPACITY_TYPE_CPU);
        CapacityVO capacityMem = _capacityDao.findByHostIdType(hostId, Capacity.CAPACITY_TYPE_MEMORY);
        CapacityVO capacityCpuCore = _capacityDao.findByHostIdType(hostId, Capacity.CAPACITY_TYPE_CPU_CORE);

        if (capacityCpu == null || capacityMem == null || svo == null || capacityCpuCore == null) {
            return;
        }

        final int cpu = svo.getCpu() * svo.getSpeed();
        final int cpucore = svo.getCpu();
        final int cpuspeed = svo.getSpeed();
        final long ram = svo.getRamSize() * 1024L * 1024L;

        try {
            final long capacityCpuId = capacityCpu.getId();
            final long capacityMemId = capacityMem.getId();
            final long capacityCpuCoreId = capacityCpuCore.getId();

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    CapacityVO capacityCpu = _capacityDao.lockRow(capacityCpuId, true);
                    CapacityVO capacityMem = _capacityDao.lockRow(capacityMemId, true);
                    CapacityVO capacityCpuCore = _capacityDao.lockRow(capacityCpuCoreId, true);

                    long usedCpu = capacityCpu.getUsedCapacity();
                    long usedMem = capacityMem.getUsedCapacity();
                    long usedCpuCore = capacityCpuCore.getUsedCapacity();
                    long reservedCpu = capacityCpu.getReservedCapacity();
                    long reservedMem = capacityMem.getReservedCapacity();
                    long reservedCpuCore = capacityCpuCore.getReservedCapacity();
                    long actualTotalCpu = capacityCpu.getTotalCapacity();
                    long actualTotalMem = capacityMem.getTotalCapacity();
                    long totalCpu = (long)(actualTotalCpu * cpuOvercommitRatio);
                    long totalMem = (long)(actualTotalMem * memoryOvercommitRatio);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Hosts's actual total CPU: " + actualTotalCpu + " and CPU after applying overprovisioning: " + totalCpu);
                    }

                    long freeCpu = totalCpu - (reservedCpu + usedCpu);
                    long freeMem = totalMem - (reservedMem + usedMem);

                    if (logger.isDebugEnabled()) {
                        logger.debug("We are allocating VM, increasing the used capacity of this host:{}", host);
                        logger.debug("Current Used CPU: {} , Free CPU:{} ,Requested CPU: {}", usedCpu, freeCpu, cpu);
                        logger.debug("Current Used RAM: {} , Free RAM:{} ,Requested RAM: {}", toHumanReadableSize(usedMem), toHumanReadableSize(freeMem), toHumanReadableSize(ram));
                    }
                    capacityCpu.setUsedCapacity(usedCpu + cpu);
                    capacityMem.setUsedCapacity(usedMem + ram);
                    capacityCpuCore.setUsedCapacity(usedCpuCore + cpucore);

                    if (fromLastHost) {
                        /* alloc from reserved */
                        if (logger.isDebugEnabled()) {
                            logger.debug("We are allocating VM to the last host again, so adjusting the reserved capacity if it is not less than required");
                            logger.debug("Reserved CPU: " + reservedCpu + " , Requested CPU: " + cpu);
                            logger.debug("Reserved RAM: " + toHumanReadableSize(reservedMem) + " , Requested RAM: " + toHumanReadableSize(ram));
                        }
                        if (reservedCpu >= cpu && reservedMem >= ram) {
                            capacityCpu.setReservedCapacity(reservedCpu - cpu);
                            capacityMem.setReservedCapacity(reservedMem - ram);
                            capacityCpuCore.setReservedCapacity(reservedCpuCore - cpucore);
                        }
                    } else {
                        /* alloc from free resource */
                        if (!((reservedCpu + usedCpu + cpu <= totalCpu) && (reservedMem + usedMem + ram <= totalMem))) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Host doesn't seem to have enough free capacity, but increasing the used capacity anyways, " +
                                    "since the VM is already starting on this host ");
                            }
                        }
                    }

                    logger.debug(String.format("CPU STATS after allocation: for host: %s, " +
                                    "old used: %d, old reserved: %d, actual total: %d, " +
                                    "total with overprovisioning: %d; new used: %d, reserved: %d; " +
                                    "requested cpu: %d, alloc_from_last: %s",
                            host, usedCpu, reservedCpu, actualTotalCpu, totalCpu,
                            capacityCpu.getUsedCapacity(), capacityCpu.getReservedCapacity(), cpu, fromLastHost));

                    logger.debug("RAM STATS after allocation: for host: {}, " +
                            "old used: {}, old reserved: {}, total: {}; new used: {}, reserved: {}; " +
                            "requested mem: {}, alloc_from_last: {}",
                            host, toHumanReadableSize(usedMem), toHumanReadableSize(reservedMem),
                            toHumanReadableSize(totalMem), toHumanReadableSize(capacityMem.getUsedCapacity()),
                            toHumanReadableSize(capacityMem.getReservedCapacity()), toHumanReadableSize(ram), fromLastHost);

                    long cluster_id = host.getClusterId();
                    ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id, VmDetailConstants.CPU_OVER_COMMIT_RATIO);
                    ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);
                    Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                    Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());

                    boolean hostHasCpuCapability, hostHasCapacity = false;
                    hostHasCpuCapability = checkIfHostHasCpuCapability(host, cpucore, cpuspeed);

                    if (hostHasCpuCapability) {
                        // first check from reserved capacity
                        hostHasCapacity = checkIfHostHasCapacity(host, cpu, ram, true, cpuOvercommitRatio, memoryOvercommitRatio, true);

                        // if not reserved, check the free capacity
                        if (!hostHasCapacity)
                            hostHasCapacity = checkIfHostHasCapacity(host, cpu, ram, false, cpuOvercommitRatio, memoryOvercommitRatio, true);
                    }

                    if (!hostHasCapacity || !hostHasCpuCapability) {
                        throw new CloudRuntimeException("Host does not have enough capacity for vm " + vm);
                    }

                    _capacityDao.update(capacityCpu.getId(), capacityCpu);
                    _capacityDao.update(capacityMem.getId(), capacityMem);
                    _capacityDao.update(capacityCpuCore.getId(), capacityCpuCore);
                }
            });
        } catch (Exception e) {
            logger.error("Exception allocating VM capacity", e);
            if (e instanceof CloudRuntimeException) {
                throw e;
            }
            return;
        }
    }

    @Override
    public boolean checkIfHostHasCpuCapability(Host host, Integer cpuNum, Integer cpuSpeed) {
        // Check host can support the Cpu Number and Speed.
        boolean isCpuNumGood = host.getCpus().intValue() >= cpuNum;
        boolean isCpuSpeedGood = host.getSpeed().intValue() >= cpuSpeed;
        boolean hasCpuCapability = isCpuNumGood && isCpuSpeedGood;

        logger.debug("{} {} cpu capability (cpu: {}, speed: {} ) to support requested CPU: {} and requested speed: {}",
                host, hasCpuCapability ? "has" : "doesn't have" ,host.getCpus(), host.getSpeed(), cpuNum, cpuSpeed);

        return hasCpuCapability;
    }

    @Override
    public boolean checkIfHostHasCapacity(Host host, Integer cpu, long ram, boolean checkFromReservedCapacity, float cpuOvercommitRatio, float memoryOvercommitRatio,
        boolean considerReservedCapacity) {
        boolean hasCapacity = false;

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Checking if host: %s has enough capacity for requested CPU: %d and requested RAM: %s , cpuOverprovisioningFactor: %s", host, cpu, toHumanReadableSize(ram), cpuOvercommitRatio));
        }

        CapacityVO capacityCpu = _capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_CPU);
        CapacityVO capacityMem = _capacityDao.findByHostIdType(host.getId(), Capacity.CAPACITY_TYPE_MEMORY);

        if (capacityCpu == null || capacityMem == null) {
            if (capacityCpu == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot checkIfHostHasCapacity, Capacity entry for CPU not found in Db, for host: {}", host);
                }
            }
            if (capacityMem == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot checkIfHostHasCapacity, Capacity entry for RAM not found in Db, for host: {}", host);
                }
            }

            return false;
        }

        long usedCpu = capacityCpu.getUsedCapacity();
        long usedMem = capacityMem.getUsedCapacity();
        long reservedCpu = capacityCpu.getReservedCapacity();
        long reservedMem = capacityMem.getReservedCapacity();
        long actualTotalCpu = capacityCpu.getTotalCapacity();
        long actualTotalMem = capacityMem.getTotalCapacity();
        long totalCpu = (long)(actualTotalCpu * cpuOvercommitRatio);
        long totalMem = (long)(actualTotalMem * memoryOvercommitRatio);
        if (logger.isDebugEnabled()) {
            logger.debug("Hosts's actual total CPU: " + actualTotalCpu + " and CPU after applying overprovisioning: " + totalCpu);
        }

        String failureReason = "";
        if (checkFromReservedCapacity) {
            long freeCpu = reservedCpu;
            long freeMem = reservedMem;

            if (logger.isDebugEnabled()) {
                logger.debug("We need to allocate to the last host again, so checking if there is enough reserved capacity");
                logger.debug("Reserved CPU: " + freeCpu + " , Requested CPU: " + cpu);
                logger.debug("Reserved RAM: " + toHumanReadableSize(freeMem) + " , Requested RAM: " + toHumanReadableSize(ram));
            }
            /* alloc from reserved */
            if (reservedCpu >= cpu) {
                if (reservedMem >= ram) {
                    hasCapacity = true;
                } else {
                    failureReason = "Host does not have enough reserved RAM available";
                }
            } else {
                failureReason = "Host does not have enough reserved CPU available";
            }
        } else {

            long reservedCpuValueToUse = reservedCpu;
            long reservedMemValueToUse = reservedMem;

            if (!considerReservedCapacity) {
                if (logger.isDebugEnabled()) {
                    logger.debug("considerReservedCapacity is" + considerReservedCapacity + " , not considering reserved capacity for calculating free capacity");
                }
                reservedCpuValueToUse = 0;
                reservedMemValueToUse = 0;
            }
            long freeCpu = totalCpu - (reservedCpuValueToUse + usedCpu);
            long freeMem = totalMem - (reservedMemValueToUse + usedMem);

            if (logger.isDebugEnabled()) {
                logger.debug("Free CPU: " + freeCpu + " , Requested CPU: " + cpu);
                logger.debug("Free RAM: " + toHumanReadableSize(freeMem) + " , Requested RAM: " + toHumanReadableSize(ram));
            }
            /* alloc from free resource */
            if ((reservedCpuValueToUse + usedCpu + cpu <= totalCpu)) {
                if ((reservedMemValueToUse + usedMem + ram <= totalMem)) {
                    hasCapacity = true;
                } else {
                    failureReason = "Host does not have enough RAM available";
                }
            } else {
                failureReason = "Host does not have enough CPU available";
            }
        }

        if (hasCapacity) {
            if (logger.isDebugEnabled()) {
                logger.debug("Host has enough CPU and RAM available");
            }

            logger.debug("STATS: Can alloc CPU from host: {}, used: {}, reserved: {}, actual total: {}, total with overprovisioning: {}; requested cpu: {}, alloc_from_last_host?: {}, considerReservedCapacity?: {}", host, usedCpu, reservedCpu, actualTotalCpu, totalCpu, cpu, checkFromReservedCapacity, considerReservedCapacity);

            logger.debug("STATS: Can alloc MEM from host: {}, used: {}, reserved: {}, total: {}; requested mem: {}, alloc_from_last_host?: {}, considerReservedCapacity?: {}", host, toHumanReadableSize(usedMem), toHumanReadableSize(reservedMem), toHumanReadableSize(totalMem), toHumanReadableSize(ram), checkFromReservedCapacity, considerReservedCapacity);
        } else {

            if (checkFromReservedCapacity) {
                logger.debug("STATS: Failed to alloc resource from host: {} reservedCpu: {}, requested cpu: {}, reservedMem: {}, requested mem: {}", host, reservedCpu, cpu, toHumanReadableSize(reservedMem), toHumanReadableSize(ram));
            } else {
                logger.debug("STATS: Failed to alloc resource from host: {}, reservedCpu: {}, used cpu: {}, requested cpu: {}, actual total cpu: {}, total cpu with overprovisioning: {}, reservedMem: {}, used Mem: {}, requested mem: {}, total Mem: {}, considerReservedCapacity?: {}", host, reservedCpu, usedCpu, cpu, actualTotalCpu, totalCpu, toHumanReadableSize(reservedMem), toHumanReadableSize(usedMem), toHumanReadableSize(ram), toHumanReadableSize(totalMem), considerReservedCapacity);
            }

            if (logger.isDebugEnabled()) {
                logger.debug(failureReason + ", cannot allocate to this host.");
            }
        }

        return hasCapacity;

    }

    @Override
    public long getUsedBytes(StoragePoolVO pool) {
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

        if (storeDriver instanceof PrimaryDataStoreDriver) {
            PrimaryDataStoreDriver primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;

            return primaryStoreDriver.getUsedBytes(pool);
        }

        throw new CloudRuntimeException("Storage driver in CapacityManagerImpl.getUsedBytes(StoragePoolVO) is not a PrimaryDataStoreDriver.");
    }

    @Override
    public long getUsedIops(StoragePoolVO pool) {
        DataStoreProvider storeProvider = _dataStoreProviderMgr.getDataStoreProvider(pool.getStorageProviderName());
        DataStoreDriver storeDriver = storeProvider.getDataStoreDriver();

        if (storeDriver instanceof PrimaryDataStoreDriver) {
            PrimaryDataStoreDriver primaryStoreDriver = (PrimaryDataStoreDriver)storeDriver;

            return primaryStoreDriver.getUsedIops(pool);
        }

        throw new CloudRuntimeException("Storage driver in CapacityManagerImpl.getUsedIops(StoragePoolVO) is not a PrimaryDataStoreDriver.");
    }

    @Override
    public long getAllocatedPoolCapacity(StoragePoolVO pool, VMTemplateVO templateForVmCreation) {
        long totalAllocatedSize = 0;

        // if the storage pool is managed, the used bytes can be larger than the sum of the sizes of all of the non-destroyed volumes
        // in this case, call getUsedBytes(StoragePoolVO)
        if (pool.isManaged()) {
            totalAllocatedSize = getUsedBytes(pool);

            if (templateForVmCreation != null) {
                VMTemplateStoragePoolVO templatePoolVO = _templatePoolDao.findByPoolTemplate(pool.getId(), templateForVmCreation.getId(), null);
                if (templatePoolVO == null) {
                    // template is not installed in the pool, consider the template size for allocation
                    long templateForVmCreationSize = templateForVmCreation.getSize() != null ? templateForVmCreation.getSize() : 0;
                    totalAllocatedSize += templateForVmCreationSize;
                }
            }

            return totalAllocatedSize;
        } else {
            // Get size for all the non-destroyed volumes.
            Pair<Long, Long> sizes = _volumeDao.getNonDestroyedCountAndTotalByPool(pool.getId());

            totalAllocatedSize = sizes.second() + sizes.first() * _extraBytesPerVolume;
        }

        // Get size for VM Snapshots.
        totalAllocatedSize += _volumeDao.getVMSnapshotSizeByPool(pool.getId());

        boolean tmpInstalled = false;
        // Iterate through all templates on this storage pool.
        List<VMTemplateStoragePoolVO> templatePoolVOs = _templatePoolDao.listByPoolId(pool.getId());

        for (VMTemplateStoragePoolVO templatePoolVO : templatePoolVOs) {
            if ((templateForVmCreation != null) && !tmpInstalled && (templatePoolVO.getTemplateId() == templateForVmCreation.getId())) {
                tmpInstalled = true;
            }

            long templateSize = templatePoolVO.getTemplateSize();

            totalAllocatedSize += templateSize + _extraBytesPerVolume;
        }

        if ((templateForVmCreation != null) && !tmpInstalled) {
            long templateForVmCreationSize = templateForVmCreation.getSize() != null ? templateForVmCreation.getSize() : 0;

            totalAllocatedSize += templateForVmCreationSize + _extraBytesPerVolume;
        }

        return totalAllocatedSize;
    }

    protected Pair<String, String> getClusterValues(long clusterId) {
        Map<String, String> map = _clusterDetailsDao.findDetails(clusterId,
                List.of(VmDetailConstants.CPU_OVER_COMMIT_RATIO, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO));
        return new Pair<>(map.get(VmDetailConstants.CPU_OVER_COMMIT_RATIO),
                map.get(VmDetailConstants.MEMORY_OVER_COMMIT_RATIO));
    }


    protected Map<Long, ServiceOfferingVO> getServiceOfferingsMap() {
        List<ServiceOfferingVO> serviceOfferings = _offeringsDao.listAllIncludingRemoved();
        if (CollectionUtils.isEmpty(serviceOfferings)) {
            return new HashMap<>();
        }
        return serviceOfferings.stream()
                .collect(Collectors.toMap(
                        ServiceOfferingVO::getId,
                        offering -> offering
                ));
    }

    protected ServiceOfferingVO getServiceOffering(long id) {
        Map <Long, ServiceOfferingVO> map = serviceOfferingsCache.get();
        if (map.containsKey(id)) {
            return map.get(id);
        }
        ServiceOfferingVO serviceOfferingVO = _offeringsDao.findByIdIncludingRemoved(id);
        if (serviceOfferingVO != null) {
            serviceOfferingsCache.invalidate();
        }
        return serviceOfferingVO;
    }

    protected Map<String, String> getVmDetailsForCapacityCalculation(long vmId) {
        return _vmInstanceDetailsDao.listDetailsKeyPairs(vmId,
                List.of(VmDetailConstants.CPU_OVER_COMMIT_RATIO,
                        VmDetailConstants.MEMORY_OVER_COMMIT_RATIO,
                        UsageEventVO.DynamicParameters.memory.name(),
                        UsageEventVO.DynamicParameters.cpuNumber.name(),
                        UsageEventVO.DynamicParameters.cpuSpeed.name()));
    }

    @DB
    @Override
    public void updateCapacityForHost(final Host host) {
        long usedCpuCore = 0;
        long reservedCpuCore = 0;
        long usedCpu = 0;
        long usedMemory = 0;
        long reservedMemory = 0;
        long reservedCpu = 0;
        final CapacityState capacityState = (host.getResourceState() == ResourceState.Enabled) ? CapacityState.Enabled : CapacityState.Disabled;

        List<VMInstanceVO> vms = _vmDao.listIdServiceOfferingForUpVmsByHostId(host.getId());
        logger.debug("Found {} VMs on {}", vms.size(), host);

        final List<VMInstanceVO> vosMigrating = _vmDao.listIdServiceOfferingForVmsMigratingFromHost(host.getId());
        logger.debug("Found {} VMs are Migrating from {}", vosMigrating.size(), host);
        vms.addAll(vosMigrating);

        Pair<String, String> clusterValues =
                clusterValuesCache.get(host.getClusterId());
        Float clusterCpuOvercommitRatio = Float.parseFloat(clusterValues.first());
        Float clusterRamOvercommitRatio = Float.parseFloat(clusterValues.second());
        for (VMInstanceVO vm : vms) {
            Float cpuOvercommitRatio = 1.0f;
            Float ramOvercommitRatio = 1.0f;
            Map<String, String> vmDetails = getVmDetailsForCapacityCalculation(vm.getId());
            String vmDetailCpu = vmDetails.get(VmDetailConstants.CPU_OVER_COMMIT_RATIO);
            String vmDetailRam = vmDetails.get(VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);
            // if vmDetailCpu or vmDetailRam is not null it means it is running in a overcommitted cluster.
            cpuOvercommitRatio = (vmDetailCpu != null) ? Float.parseFloat(vmDetailCpu) : clusterCpuOvercommitRatio;
            ramOvercommitRatio = (vmDetailRam != null) ? Float.parseFloat(vmDetailRam) : clusterRamOvercommitRatio;
            ServiceOffering so = getServiceOffering(vm.getServiceOfferingId());
            if (so == null) {
                so = _offeringsDao.findByIdIncludingRemoved(vm.getServiceOfferingId());
            }
            if (so.isDynamic()) {
                usedMemory +=
                    ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.memory.name())) * 1024L * 1024L) / ramOvercommitRatio) *
                        clusterRamOvercommitRatio;
                if(vmDetails.containsKey(UsageEventVO.DynamicParameters.cpuSpeed.name())) {
                    usedCpu +=
                            ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name())) * Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuSpeed.name()))) / cpuOvercommitRatio) *
                                    clusterCpuOvercommitRatio;
                } else {
                    usedCpu +=
                            ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name())) * so.getSpeed()) / cpuOvercommitRatio) *
                                    clusterCpuOvercommitRatio;
                }
                usedCpuCore += Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name()));
            } else {
                usedMemory += ((so.getRamSize() * 1024L * 1024L) / ramOvercommitRatio) * clusterRamOvercommitRatio;
                usedCpu += ((so.getCpu() * so.getSpeed()) / cpuOvercommitRatio) * clusterCpuOvercommitRatio;
                usedCpuCore += so.getCpu();
            }
        }

        List<VMInstanceVO> vmsByLastHostId = _vmDao.listByLastHostId(host.getId());
        logger.debug("Found {} VM, not running on {}", vmsByLastHostId.size(), host);

        for (VMInstanceVO vm : vmsByLastHostId) {
            Float cpuOvercommitRatio = 1.0f;
            Float ramOvercommitRatio = 1.0f;
            long lastModificationTime = Optional.ofNullable(vm.getUpdateTime()).orElse(vm.getCreated()).getTime();
            long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - lastModificationTime) / 1000;
            if (secondsSinceLastUpdate < _vmCapacityReleaseInterval) {
                Map<String, String> vmDetails = getVmDetailsForCapacityCalculation(vm.getId());
                String vmDetailCpu = vmDetails.get(VmDetailConstants.CPU_OVER_COMMIT_RATIO);
                String vmDetailRam = vmDetails.get(VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);
                if (vmDetailCpu != null) {
                    //if vmDetail_cpu is not null it means it is running in a overcommited cluster.
                    cpuOvercommitRatio = Float.parseFloat(vmDetailCpu);
                }
                if (vmDetailRam != null) {
                    ramOvercommitRatio = Float.parseFloat(vmDetailRam);
                }
                ServiceOffering so = getServiceOffering(vm.getServiceOfferingId());
                if (so == null) {
                    so = _offeringsDao.findByIdIncludingRemoved(vm.getServiceOfferingId());
                }
                if (so.isDynamic()) {
                    reservedMemory +=
                        ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.memory.name())) * 1024L * 1024L) / ramOvercommitRatio) *
                            clusterRamOvercommitRatio;
                    if(vmDetails.containsKey(UsageEventVO.DynamicParameters.cpuSpeed.name())) {
                        reservedCpu +=
                                ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name())) * Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuSpeed.name()))) / cpuOvercommitRatio) *
                                        clusterCpuOvercommitRatio;
                    } else {
                        reservedCpu +=
                                ((Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name())) * so.getSpeed()) / cpuOvercommitRatio) *
                                        clusterCpuOvercommitRatio;
                    }
                    reservedCpuCore += Integer.parseInt(vmDetails.get(UsageEventVO.DynamicParameters.cpuNumber.name()));
                } else {
                    reservedMemory += ((so.getRamSize() * 1024L * 1024L) / ramOvercommitRatio) * clusterRamOvercommitRatio;
                    reservedCpu += (so.getCpu() * so.getSpeed() / cpuOvercommitRatio) * clusterCpuOvercommitRatio;
                    reservedCpuCore += so.getCpu();
                }
            } else {
                // signal if not done already, that the VM has been stopped for skip.counting.hours,
                // hence capacity will not be reserved anymore.
                VMInstanceDetailVO messageSentFlag = _vmInstanceDetailsDao.findDetail(vm.getId(), VmDetailConstants.MESSAGE_RESERVED_CAPACITY_FREED_FLAG);
                if (messageSentFlag == null || !Boolean.valueOf(messageSentFlag.getValue())) {
                    _messageBus.publish(_name, "VM_ReservedCapacity_Free", PublishScope.LOCAL, vm);

                    if (vm.getType() == VirtualMachine.Type.User) {
                        UserVmVO userVM = _userVMDao.findById(vm.getId());
                        _userVMDao.loadDetails(userVM);
                        userVM.setDetail(VmDetailConstants.MESSAGE_RESERVED_CAPACITY_FREED_FLAG, "true");
                        _userVMDao.saveDetails(userVM);
                    }
                }
            }
        }

        List<CapacityVO> capacities = _capacityDao.listByHostIdTypes(host.getId(), List.of(Capacity.CAPACITY_TYPE_CPU,
                Capacity.CAPACITY_TYPE_MEMORY,
                CapacityVO.CAPACITY_TYPE_CPU_CORE));
        CapacityVO cpuCap = null;
        CapacityVO memCap = null;
        CapacityVO cpuCoreCap = null;
        for (CapacityVO c : capacities) {
            if (c.getCapacityType() == Capacity.CAPACITY_TYPE_CPU) {
                cpuCap = c;
            } else if (c.getCapacityType() == Capacity.CAPACITY_TYPE_MEMORY) {
                memCap = c;
            } else if (c.getCapacityType() == Capacity.CAPACITY_TYPE_CPU_CORE) {
                cpuCoreCap = c;
            }
            if (ObjectUtils.allNotNull(cpuCap, memCap, cpuCoreCap)) {
                break;
            }
        }

        if (cpuCoreCap != null) {
            long hostTotalCpuCore = host.getCpus().longValue();

            if (cpuCoreCap.getTotalCapacity() != hostTotalCpuCore) {
                logger.debug("Calibrate total cpu for host: {} old total CPU:{} new total CPU:{}", host, cpuCoreCap.getTotalCapacity(), hostTotalCpuCore);
                cpuCoreCap.setTotalCapacity(hostTotalCpuCore);

            }

            if (cpuCoreCap.getUsedCapacity() == usedCpuCore && cpuCoreCap.getReservedCapacity() == reservedCpuCore) {
                logger.debug("No need to calibrate cpu capacity, host:{} usedCpuCore: {} reservedCpuCore: {}", host, cpuCoreCap.getUsedCapacity(), cpuCoreCap.getReservedCapacity());
            } else {
                if (cpuCoreCap.getReservedCapacity() != reservedCpuCore) {
                    logger.debug("Calibrate reserved cpu core for host: {} old reservedCpuCore: {} new reservedCpuCore: {}", host, cpuCoreCap.getReservedCapacity(), reservedCpuCore);
                    cpuCoreCap.setReservedCapacity(reservedCpuCore);
                }
                if (cpuCoreCap.getUsedCapacity() != usedCpuCore) {
                    logger.debug("Calibrate used cpu core for host: {} old usedCpuCore: {} new usedCpuCore: {}", host, cpuCoreCap.getUsedCapacity(), usedCpuCore);
                    cpuCoreCap.setUsedCapacity(usedCpuCore);
                }
            }
            try {
                _capacityDao.update(cpuCoreCap.getId(), cpuCoreCap);
            } catch (Exception e) {
                logger.error("Caught exception while updating cpucore capacity for the host {}", host, e);
            }
        } else {
            final long usedCpuCoreFinal = usedCpuCore;
            final long reservedCpuCoreFinal = reservedCpuCore;
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    CapacityVO capacity = new CapacityVO(host.getId(), host.getDataCenterId(), host.getPodId(), host.getClusterId(), usedCpuCoreFinal, host.getCpus().longValue(),
                            CapacityVO.CAPACITY_TYPE_CPU_CORE);
                    capacity.setReservedCapacity(reservedCpuCoreFinal);
                    capacity.setCapacityState(capacityState);
                    _capacityDao.persist(capacity);
                }
            });
        }

        if (cpuCap != null && memCap != null) {
            if (host.getTotalMemory() != null) {
                memCap.setTotalCapacity(host.getTotalMemory());
            }
            long hostTotalCpu = host.getCpus().longValue() * host.getSpeed().longValue();

            if (cpuCap.getTotalCapacity() != hostTotalCpu) {
                logger.debug("Calibrate total cpu for host: {} old total CPU:{} new total CPU:{}", host, cpuCap.getTotalCapacity(), hostTotalCpu);
                cpuCap.setTotalCapacity(hostTotalCpu);

            }
            // Set the capacity state as per the host allocation state.
            if(capacityState != cpuCap.getCapacityState()){
                logger.debug("Calibrate cpu capacity state for host: {} old capacity state:{} new capacity state:{}", host, cpuCap.getTotalCapacity(), hostTotalCpu);
                cpuCap.setCapacityState(capacityState);
            }
            memCap.setCapacityState(capacityState);

            if (cpuCap.getUsedCapacity() == usedCpu && cpuCap.getReservedCapacity() == reservedCpu) {
                logger.debug("No need to calibrate cpu capacity, host:{} usedCpu: {} reservedCpu: {}", host, cpuCap.getUsedCapacity(), cpuCap.getReservedCapacity());
            } else {
                if (cpuCap.getReservedCapacity() != reservedCpu) {
                    logger.debug("Calibrate reserved cpu for host: {} old reservedCpu:{} new reservedCpu:{}", host, cpuCap.getReservedCapacity(), reservedCpu);
                    cpuCap.setReservedCapacity(reservedCpu);
                }
                if (cpuCap.getUsedCapacity() != usedCpu) {
                    logger.debug("Calibrate used cpu for host: {} old usedCpu:{} new usedCpu:{}", host, cpuCap.getUsedCapacity(), usedCpu);
                    cpuCap.setUsedCapacity(usedCpu);
                }
            }

            if (memCap.getTotalCapacity() != host.getTotalMemory()) {
                logger.debug("Calibrate total memory for host: {} old total memory:{} new total memory:{}", host, toHumanReadableSize(memCap.getTotalCapacity()), toHumanReadableSize(host.getTotalMemory()));
                memCap.setTotalCapacity(host.getTotalMemory());

            }
            // Set the capacity state as per the host allocation state.
            if(capacityState != memCap.getCapacityState()){
                logger.debug("Calibrate memory capacity state for host: {} old capacity state:{} new capacity state:{}", host, memCap.getTotalCapacity(), hostTotalCpu);
                memCap.setCapacityState(capacityState);
            }

            if (memCap.getUsedCapacity() == usedMemory && memCap.getReservedCapacity() == reservedMemory) {
                logger.debug("No need to calibrate memory capacity, host:{} usedMem: {} reservedMem: {}", host, toHumanReadableSize(memCap.getUsedCapacity()), toHumanReadableSize(memCap.getReservedCapacity()));
            } else {
                if (memCap.getReservedCapacity() != reservedMemory) {
                    logger.debug("Calibrate reserved memory for host: {} old reservedMem:{} new reservedMem:{}", host, memCap.getReservedCapacity(), reservedMemory);
                    memCap.setReservedCapacity(reservedMemory);
                }
                if (memCap.getUsedCapacity() != usedMemory) {
                    /*
                     * Didn't calibrate for used memory, because VMs can be in
                     * state(starting/migrating) that I don't know on which host
                     * they are allocated
                     */
                    logger.debug("Calibrate used memory for host: {} old usedMem: {} new usedMem: {}", host, toHumanReadableSize(memCap.getUsedCapacity()), toHumanReadableSize(usedMemory));
                    memCap.setUsedCapacity(usedMemory);
                }
            }

            try {
                _capacityDao.update(cpuCap.getId(), cpuCap);
                _capacityDao.update(memCap.getId(), memCap);
            } catch (Exception e) {
                logger.error("Caught exception while updating cpu/memory capacity for the host {}", host, e);
            }
        } else {
            final long usedMemoryFinal = usedMemory;
            final long reservedMemoryFinal = reservedMemory;
            final long usedCpuFinal = usedCpu;
            final long reservedCpuFinal = reservedCpu;
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    CapacityVO capacity =
                        new CapacityVO(host.getId(), host.getDataCenterId(), host.getPodId(), host.getClusterId(), usedMemoryFinal, host.getTotalMemory(),
                            Capacity.CAPACITY_TYPE_MEMORY);
                    capacity.setReservedCapacity(reservedMemoryFinal);
                    capacity.setCapacityState(capacityState);
                    _capacityDao.persist(capacity);

                    capacity =
                        new CapacityVO(host.getId(), host.getDataCenterId(), host.getPodId(), host.getClusterId(), usedCpuFinal, host.getCpus().longValue() *
                            host.getSpeed().longValue(), Capacity.CAPACITY_TYPE_CPU);
                    capacity.setReservedCapacity(reservedCpuFinal);
                    capacity.setCapacityState(capacityState);
                    _capacityDao.persist(capacity);
                }
            });

        }

    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vm, boolean transitionStatus, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vm, boolean status, Object opaque) {
      if (!status) {
        return false;
      }
      @SuppressWarnings("unchecked")
      Pair<Long, Long> hosts = (Pair<Long, Long>)opaque;
      Long oldHostId = hosts.first();

      State oldState = transition.getCurrentState();
      State newState = transition.getToState();
      Event event = transition.getEvent();
      Host lastHost = _hostDao.findById(vm.getLastHostId());
      Host oldHost = _hostDao.findById(oldHostId);
      Host newHost = _hostDao.findById(vm.getHostId());
      logger.debug(String.format("%s state transited from [%s] to [%s] with event [%s]. VM's original host: %s, new host: %s, host before state transition: %s", vm, oldState,
                newState, event, lastHost, newHost, oldHost));

      if (oldState == State.Starting) {
        if (newState != State.Running) {
          releaseVmCapacity(vm, false, false, oldHost);
        }
      } else if (oldState == State.Running) {
        if (event == Event.AgentReportStopped) {
          releaseVmCapacity(vm, false, true, oldHost);
        } else if (event == Event.AgentReportMigrated) {
          releaseVmCapacity(vm, false, false, oldHost);
        }
      } else if (oldState == State.Migrating) {
        if (event == Event.AgentReportStopped) {
                /* Release capacity from original host */
          releaseVmCapacity(vm, false, false, lastHost);
          releaseVmCapacity(vm, false, false, oldHost);
        } else if (event == Event.OperationFailed) {
                /* Release from dest host */
          releaseVmCapacity(vm, false, false, oldHost);
        } else if (event == Event.OperationSucceeded) {
          releaseVmCapacity(vm, false, false, lastHost);
        }
      } else if (oldState == State.Stopping) {
        if (event == Event.OperationSucceeded) {
          releaseVmCapacity(vm, false, true, oldHost);
        } else if (event == Event.AgentReportStopped) {
          releaseVmCapacity(vm, false, false, oldHost);
        } else if (event == Event.AgentReportMigrated) {
          releaseVmCapacity(vm, false, false, oldHost);
        }
      } else if (oldState == State.Stopped) {
        if (event == Event.DestroyRequested || event == Event.ExpungeOperation) {
          releaseVmCapacity(vm, true, false, lastHost);
        } else if (event == Event.AgentReportMigrated) {
          releaseVmCapacity(vm, false, false, oldHost);
        }
      }

      if ((newState == State.Starting || newState == State.Migrating || event == Event.AgentReportMigrated) && vm.getHostId() != null) {
        boolean fromLastHost = false;
        if (vm.getHostId().equals(vm.getLastHostId())) {
          logger.debug("VM starting again on the last host it was stopped on");
          fromLastHost = true;
        }
        allocateVmCapacity(vm, fromLastHost);
      }

      if (newState == State.Stopped && event != Event.RestoringFailed && event != Event.RestoringSuccess && vm.getType() == VirtualMachine.Type.User) {
          UserVmVO userVM = _userVMDao.findById(vm.getId());
          _userVMDao.loadDetails(userVM);
          // free the message sent flag if it exists
          userVM.setDetail(VmDetailConstants.MESSAGE_RESERVED_CAPACITY_FREED_FLAG, "false");
          _userVMDao.saveDetails(userVM);
      }

      return true;
    }

  // TODO: Get rid of this case once we've determined that the capacity listeners above have all the changes
    // create capacity entries if none exist for this server
    private void createCapacityEntry(StartupCommand startup, HostVO server) {
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, server.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, server.getDataCenterId());
        capacitySC.addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());

        if (startup instanceof StartupRoutingCommand) {
            SearchCriteria<CapacityVO> capacityCPU = _capacityDao.createSearchCriteria();
            capacityCPU.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, server.getId());
            capacityCPU.addAnd("dataCenterId", SearchCriteria.Op.EQ, server.getDataCenterId());
            capacityCPU.addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
            capacityCPU.addAnd("capacityType", SearchCriteria.Op.EQ, Capacity.CAPACITY_TYPE_CPU);
            List<CapacityVO> capacityVOCpus = _capacityDao.search(capacitySC, null);
            Float cpuovercommitratio = Float.parseFloat(_clusterDetailsDao.findDetail(server.getClusterId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO).getValue());
            Float memoryOvercommitRatio = Float.parseFloat(_clusterDetailsDao.findDetail(server.getClusterId(), VmDetailConstants.MEMORY_OVER_COMMIT_RATIO).getValue());

            if (capacityVOCpus != null && !capacityVOCpus.isEmpty()) {
                CapacityVO CapacityVOCpu = capacityVOCpus.get(0);
                long newTotalCpu = (long)(server.getCpus().longValue() * server.getSpeed().longValue() * cpuovercommitratio);
                if ((CapacityVOCpu.getTotalCapacity() <= newTotalCpu) || ((CapacityVOCpu.getUsedCapacity() + CapacityVOCpu.getReservedCapacity()) <= newTotalCpu)) {
                    CapacityVOCpu.setTotalCapacity(newTotalCpu);
                } else if ((CapacityVOCpu.getUsedCapacity() + CapacityVOCpu.getReservedCapacity() > newTotalCpu) && (CapacityVOCpu.getUsedCapacity() < newTotalCpu)) {
                    CapacityVOCpu.setReservedCapacity(0);
                    CapacityVOCpu.setTotalCapacity(newTotalCpu);
                } else {
                    logger.debug("What? new cpu is :" + newTotalCpu + ", old one is " + CapacityVOCpu.getUsedCapacity() + "," + CapacityVOCpu.getReservedCapacity() +
                        "," + CapacityVOCpu.getTotalCapacity());
                }
                _capacityDao.update(CapacityVOCpu.getId(), CapacityVOCpu);
            } else {
                CapacityVO capacity =
                    new CapacityVO(server.getId(), server.getDataCenterId(), server.getPodId(), server.getClusterId(), 0L, server.getCpus().longValue() *
                        server.getSpeed().longValue(), Capacity.CAPACITY_TYPE_CPU);
                _capacityDao.persist(capacity);
            }

            SearchCriteria<CapacityVO> capacityMem = _capacityDao.createSearchCriteria();
            capacityMem.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, server.getId());
            capacityMem.addAnd("dataCenterId", SearchCriteria.Op.EQ, server.getDataCenterId());
            capacityMem.addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
            capacityMem.addAnd("capacityType", SearchCriteria.Op.EQ, Capacity.CAPACITY_TYPE_MEMORY);
            List<CapacityVO> capacityVOMems = _capacityDao.search(capacityMem, null);

            if (capacityVOMems != null && !capacityVOMems.isEmpty()) {
                CapacityVO CapacityVOMem = capacityVOMems.get(0);
                long newTotalMem = (long)((server.getTotalMemory()) * memoryOvercommitRatio);
                if (CapacityVOMem.getTotalCapacity() <= newTotalMem || (CapacityVOMem.getUsedCapacity() + CapacityVOMem.getReservedCapacity() <= newTotalMem)) {
                    CapacityVOMem.setTotalCapacity(newTotalMem);
                } else if (CapacityVOMem.getUsedCapacity() + CapacityVOMem.getReservedCapacity() > newTotalMem && CapacityVOMem.getUsedCapacity() < newTotalMem) {
                    CapacityVOMem.setReservedCapacity(0);
                    CapacityVOMem.setTotalCapacity(newTotalMem);
                } else {
                    logger.debug("What? new mem is :" + newTotalMem + ", old one is " + CapacityVOMem.getUsedCapacity() + "," + CapacityVOMem.getReservedCapacity() +
                        "," + CapacityVOMem.getTotalCapacity());
                }
                _capacityDao.update(CapacityVOMem.getId(), CapacityVOMem);
            } else {
                CapacityVO capacity =
                    new CapacityVO(server.getId(), server.getDataCenterId(), server.getPodId(), server.getClusterId(), 0L, server.getTotalMemory(),
                        Capacity.CAPACITY_TYPE_MEMORY);
                _capacityDao.persist(capacity);
            }
        }

    }

    @Override
    public float getClusterOverProvisioningFactor(Long clusterId, short capacityType) {

        String capacityOverProvisioningName = "";
        if (capacityType == Capacity.CAPACITY_TYPE_CPU) {
            capacityOverProvisioningName = VmDetailConstants.CPU_OVER_COMMIT_RATIO;
        } else if (capacityType == Capacity.CAPACITY_TYPE_MEMORY) {
            capacityOverProvisioningName = VmDetailConstants.MEMORY_OVER_COMMIT_RATIO;
        } else {
            throw new CloudRuntimeException("Invalid capacityType - " + capacityType);
        }

        ClusterDetailsVO clusterDetailCpu = _clusterDetailsDao.findDetail(clusterId, capacityOverProvisioningName);
        Float clusterOverProvisioningRatio = Float.parseFloat(clusterDetailCpu.getValue());
        return clusterOverProvisioningRatio;

    }

    @Override
    public boolean checkIfClusterCrossesThreshold(Long clusterId, Integer cpuRequested, long ramRequested) {
        Float clusterCpuOverProvisioning = getClusterOverProvisioningFactor(clusterId, Capacity.CAPACITY_TYPE_CPU);
        Float clusterMemoryOverProvisioning = getClusterOverProvisioningFactor(clusterId, Capacity.CAPACITY_TYPE_MEMORY);
        Float clusterCpuCapacityDisableThreshold = DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.valueIn(clusterId);
        Float clusterMemoryCapacityDisableThreshold = DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.valueIn(clusterId);

        float cpuConsumption = _capacityDao.findClusterConsumption(clusterId, Capacity.CAPACITY_TYPE_CPU, cpuRequested);
        if (cpuConsumption / clusterCpuOverProvisioning > clusterCpuCapacityDisableThreshold) {
            logger.debug("Cluster: {} cpu consumption {} crosses disable threshold {}", _clusterDao.findById(clusterId), cpuConsumption / clusterCpuOverProvisioning, clusterCpuCapacityDisableThreshold);
            return true;
        }

        float memoryConsumption = _capacityDao.findClusterConsumption(clusterId, Capacity.CAPACITY_TYPE_MEMORY, ramRequested);
        if (memoryConsumption / clusterMemoryOverProvisioning > clusterMemoryCapacityDisableThreshold) {
            logger.debug("Cluster: {} memory consumption {} crosses disable threshold {}", _clusterDao.findById(clusterId), memoryConsumption / clusterMemoryOverProvisioning, clusterMemoryCapacityDisableThreshold);
            return true;
        }

        return false;

    }

    @Override
    public Pair<Boolean, Boolean> checkIfHostHasCpuCapabilityAndCapacity(Host host, ServiceOffering offering, boolean considerReservedCapacity) {
        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;
        Pair<String, String> clusterDetails = getClusterValues(host.getClusterId());
        Float cpuOvercommitRatio = Float.parseFloat(clusterDetails.first());
        Float memoryOvercommitRatio = Float.parseFloat(clusterDetails.second());

        boolean hostHasCpuCapability = checkIfHostHasCpuCapability(host, offering.getCpu(), offering.getSpeed());
        boolean hostHasCapacity = checkIfHostHasCapacity(host, cpu_requested, ram_requested, false, cpuOvercommitRatio, memoryOvercommitRatio,
                considerReservedCapacity);

        return new Pair<>(hostHasCpuCapability, hostHasCapacity);
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void processCancelMaintenaceEventAfter(Long hostId) {
        updateCapacityForHost(_hostDao.findById(hostId));
    }

    @Override
    public void processCancelMaintenaceEventBefore(Long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processDeletHostEventAfter(Host host) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processDeleteHostEventBefore(Host host) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processDiscoverEventAfter(Map<? extends ServerResource, Map<String, String>> resources) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processDiscoverEventBefore(Long dcid, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processPrepareMaintenaceEventAfter(Long hostId) {
        _capacityDao.removeBy(Capacity.CAPACITY_TYPE_MEMORY, null, null, null, hostId);
        _capacityDao.removeBy(Capacity.CAPACITY_TYPE_CPU, null, null, null, hostId);
        _capacityDao.removeBy(Capacity.CAPACITY_TYPE_CPU_CORE, null, null, null, hostId);
    }

    @Override
    public void processPrepareMaintenaceEventBefore(Long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean checkIfHostReachMaxGuestLimit(Host host) {
        HypervisorType hypervisorType = host.getHypervisorType();
        if (hypervisorType.equals(HypervisorType.KVM)) {
            logger.debug(String.format("Host {id: %s, name: %s, uuid: %s} is %s hypervisor type, no max guest limit check needed", host.getId(), host.getName(), host.getUuid(), hypervisorType));
            return false;
        }
        Long vmCount = _vmDao.countActiveByHostId(host.getId());
        String hypervisorVersion = host.getHypervisorVersion();
        Long maxGuestLimit = _hypervisorCapabilitiesDao.getMaxGuestsLimit(hypervisorType, hypervisorVersion);
        if (vmCount >= maxGuestLimit) {
            logger.info(String.format("Host {id: %s, name: %s, uuid: %s} already reached max Running VMs(count includes system VMs), limit: %d, running VM count: %s",
                    host.getId(), host.getName(), host.getUuid(), maxGuestLimit, vmCount));
            return true;
        }
        return false;
    }

    @Override
    public String getConfigComponentName() {
        return CapacityManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {CpuOverprovisioningFactor, MemOverprovisioningFactor, StorageCapacityDisableThreshold, StorageOverprovisioningFactor,
                StorageAllocatedCapacityDisableThreshold, StorageOperationsExcludeCluster, ImageStoreNFSVersion, SecondaryStorageCapacityThreshold,
                StorageAllocatedCapacityDisableThresholdForVolumeSize, CapacityCalculateWorkers };
    }
}
