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
package org.apache.cloudstack.engine.vm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.config.ConfigRepo;
import org.apache.cloudstack.config.ConfigValue;
import org.apache.cloudstack.engine.cloud.entity.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.config.Configs;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageOrchestrator;
import org.apache.cloudstack.network.NetworkOrchestrator;
import org.apache.cloudstack.vm.jobs.VmWorkJobDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.cluster.ManagementServerNode;
import com.cloud.dao.EntityManager;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfileImpl;

/**
 * VirtualMachineOrchestrator orchestrates virtual machine operations.
 *
 */
public class VirtualMachineOrchestrator extends ManagerBase {
    private final static Logger s_logger = Logger.getLogger(VirtualMachineOrchestrator.class);
    @Inject
    EntityManager _entityMgr;
    @Inject
    ConfigRepo _configRepo;
    @Inject
    NetworkOrchestrator _networkOrchestrator;
    @Inject
    StorageOrchestrator _storageOrchestrator;
    @Inject
    AgentManager _agentMgr;
    @Inject
    VmWorkJobDao _workJobDao;
    @Inject
    VolumeManager _volMgr;
    
    protected ConfigValue<Integer> _retry;
    protected ConfigValue<Integer> _cancelWait;
    protected ConfigValue<Long> _cleanupWait;
    protected ConfigValue<Long> _cleanupInterval;
    protected ConfigValue<Long> _opWaitInterval;
    protected ConfigValue<Integer> _lockStateRetry;
    protected ConfigValue<Integer> _operationTimeout;
    protected ConfigValue<Boolean> _forceStop;

    // FIXME:  Hopefully these can be temporary for now
    @Inject
    UserDao _userDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    ServiceOfferingDao _offeringDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    NetworkManager _networkMgr;
    // FIXME: Hopefully we can remove the above stuff.

    long _nodeId;

    ScheduledExecutorService _executor = null;

    public VirtualMachineEntity create(
            String id,
            String owner,
            String templateId,
            String hostName,
            String displayName,
            Hypervisor.HypervisorType hypervisor,
            int cpu,
            int speed,
            long memory,
            Long diskSize,
            List<String> computeTags,
            List<String> rootDiskTags,
            Map<String, NicProfile> networkNicMap,
            DeploymentPlan plan) throws InsufficientCapacityException {

        List<Pair<NetworkVO, NicProfile>> networkIpMap = new ArrayList<Pair<NetworkVO, NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _entityMgr.findByUuid(NetworkVO.class, uuid);
            if (network != null) {
                networkIpMap.add(new Pair<NetworkVO, NicProfile>(network, networkNicMap.get(uuid)));
            }
        }

        //load vm instance and offerings and call virtualMachineManagerImpl
        VMEntityVO vm = _entityMgr.findByUuid(VMEntityVO.class, id);

        // If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
        // Else, a disk offering is optional, and if present will be used to create the data disk

        Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
        List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();

        ServiceOfferingVO offering = _entityMgr.findById(ServiceOfferingVO.class, vm.getServiceOfferingId());
        rootDiskOffering.first(offering);

        if (vm.getDiskOfferingId() != null) {
            DiskOfferingVO diskOffering = _entityMgr.findById(DiskOfferingVO.class, vm.getDiskOfferingId());
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + vm.getDiskOfferingId());
            }
            Long size = null;
            if (diskOffering.getDiskSize() == 0) {
                size = diskSize;
                if (size == null) {
                    throw new InvalidParameterValueException(
                            "Disk offering " + diskOffering
                                    + " requires size parameter.");
                }
            }
            dataDiskOfferings.add(new Pair<DiskOfferingVO, Long>(diskOffering, size));
        }

        VMTemplateVO template = _entityMgr.findByUuid(VMTemplateVO.class, templateId);

        vm = createDbEntities(vm, plan, template, offering, rootDiskOffering, dataDiskOfferings, networkIpMap, hypervisor, owner);
        if (vm == null) {
            return null;
        }

        return new VirtualMachineEntityImpl2(vm);
    }

    public VirtualMachineEntity createFromScratch(
            String id,
            String owner,
            String isoId,
            String hostName,
            String displayName,
            Hypervisor.HypervisorType hypervisorType,
            String os,
            int cpu,
            int speed,
            long memory,
            Long diskSize,
            List<String> computeTags,
            List<String> rootDiskTags,
            Map<String, NicProfile> networkNicMap,
            DeploymentPlan plan) throws InsufficientCapacityException {

        VMEntityVO vm = _entityMgr.findByUuid(VMEntityVO.class, id);

        Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
        ServiceOfferingVO offering = _entityMgr.findById(ServiceOfferingVO.class, vm.getServiceOfferingId());
        rootDiskOffering.first(offering);

        List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();
        Long diskOfferingId = vm.getDiskOfferingId();
        DiskOfferingVO diskOffering = _entityMgr.findById(DiskOfferingVO.class, diskOfferingId);
        if (diskOffering == null) {
            throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
        }
        Long size = null;
        if (diskOffering.getDiskSize() == 0) {
            size = diskSize;
            if (size == null) {
                throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
            }
        }
        rootDiskOffering.first(diskOffering);
        rootDiskOffering.second(size);

        List<Pair<NetworkVO, NicProfile>> networkIpMap = new ArrayList<Pair<NetworkVO, NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _entityMgr.findByUuid(NetworkVO.class, uuid);
            if (network != null) {
                networkIpMap.add(new Pair<NetworkVO, NicProfile>(network, networkNicMap.get(uuid)));
            }
        }

        VMTemplateVO template = _entityMgr.findByUuid(VMTemplateVO.class, isoId);

        vm = createDbEntities(vm, plan, template, offering, rootDiskOffering, dataDiskOfferings, networkIpMap, hypervisorType, owner);
        if (vm == null) {
            return null;
        }

        return new VirtualMachineEntityImpl2(vm);
    }

    @DB
    protected VMEntityVO createDbEntities(VMEntityVO vm1, DeploymentPlan plan, VMTemplateVO template, ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, List<Pair<NetworkVO, NicProfile>> networks,
            Hypervisor.HypervisorType hyperType, String ownerRef) throws InsufficientCapacityException {
        assert (plan.getClusterId() == null && plan.getPoolId() == null) : "We currently don't support cluster and pool preset yet";

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm1);
        }
        
        AccountVO owner = _entityMgr.findById(AccountVO.class, new Long(ownerRef));

        VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, vm1.getId());
        VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);

        Transaction txn = Transaction.currentTxn();
        txn.start();

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Allocating nics for " + vm);
            }

            _networkMgr.allocate(vmProfile, networks);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation while trying to allocate resources for the VM", e);
        }

        if (dataDiskOfferings == null) {
            dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(0);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocaing disks for " + vm);
        }

        if (template.getFormat() == ImageFormat.ISO) {
            _volMgr.allocateRawVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner);
        } else if (template.getFormat() == ImageFormat.BAREMETAL) {
            // Do nothing
        } else {
            _volMgr.allocateTemplatedVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner);
        }

        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            _volMgr.allocateRawVolume(Type.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
        }

        vm1.setDataCenterId(plan.getDataCenterId());
        if (plan.getPodId() != null) {
            vm1.setPodId(plan.getPodId());
        }

        vm1 = _entityMgr.persist(vm1);
        txn.commit();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }

        return vm1;
    }

    public VirtualMachineEntity get(String uuid) {
        VMEntityVO vo = _entityMgr.findByUuid(VMEntityVO.class, uuid);
        return new VirtualMachineEntityImpl2(vo);
    }

    public VirtualMachineEntity get(long id) {
        VMEntityVO vo = _entityMgr.findById(VMEntityVO.class, id);
        return new VirtualMachineEntityImpl2(vo);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        _retry = _configRepo.get(Configs.StartRetry);
        
        _cancelWait = _configRepo.get(Configs.VmOpCancelInterval);
        _cleanupWait = _configRepo.get(Configs.VmOpCleanupWait);
        _cleanupInterval = _configRepo.get(Configs.VmOpCleanupInterval).setMultiplier(1000);
        _opWaitInterval = _configRepo.get(Configs.VmOpWaitInterval).setMultiplier(1000);
        _lockStateRetry = _configRepo.get(Configs.VmOpLockStateRetry);
        _operationTimeout = _configRepo.get(Configs.Wait).setMultiplier(2);
        _forceStop = _configRepo.get(Configs.VmDestroyForcestop);

        _nodeId = ManagementServerNode.getManagementServerId();

        _agentMgr.registerForHostEvents(new AgentListener(), true, true, true);

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));

//        ReservationContextImpl.setComponents(_userDao, _domainDao, _accountDao);
        VirtualMachineProfileImpl.setComponents(_offeringDao, _templateDao, _accountDao);
        VirtualMachineEntityImpl2.init(_entityMgr, this, _networkOrchestrator, _storageOrchestrator);

        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new CleanupTask(), _cleanupInterval.value(), _cleanupInterval.value(), TimeUnit.SECONDS);
        return super.start();
    }
    
    @Override
    public boolean stop() {
        _executor.shutdownNow();
        try {
            _executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            s_logger.warn("Interrupted while waiting for executor shutdown");
        }
        if (!_executor.isShutdown()) {
            s_logger.warn("Not all background tasks are shutdown.");
        }
        if (!_executor.isTerminated()) {
            s_logger.warn("Not all background tasks are terminated.");
        }
        return super.stop();
    }

    protected class CleanupTask implements Runnable {

        @Override
        public void run() {
            s_logger.info("VM Operation Thread Running");

            try {
                Date cutDate = DateUtil.currentGMTTime();
                cutDate = new Date(cutDate.getTime() - 60000);
                _workJobDao.expungeCompletedWorkJobs(cutDate);
            } catch (Throwable e) {
                s_logger.error("Unexpected exception", e);
            }
        }
    }

    private class AgentListener implements Listener {

        @Override
        public boolean processAnswers(long agentId, long seq, Answer[] answers) {
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
        public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
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
    }
}
