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
package com.cloud.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.fsm.StateMachine2;

import org.apache.log4j.Logger;
import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMReservationDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanner.PlannerResourceUsage;
import com.cloud.deploy.dao.PlannerHostReservationDao;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.gpu.GPU;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = {DeploymentPlanningManager.class})
public class DeploymentPlanningManagerImpl extends ManagerBase implements DeploymentPlanningManager, Manager, Listener,
StateListener<State, VirtualMachine.Event, VirtualMachine> {

    private static final Logger s_logger = Logger.getLogger(DeploymentPlanningManagerImpl.class);
    @Inject
    AgentManager _agentMgr;
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Inject
    AffinityGroupService _affinityGroupService;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    PlannerHostReservationDao _plannerHostReserveDao;
    private int _vmCapacityReleaseInterval;
    @Inject
    MessageBus _messageBus;
    private Timer _timer = null;
    private long _hostReservationReleasePeriod = 60L * 60L * 1000L; // one hour by default
    @Inject
    protected VMReservationDao _reservationDao;

    private static final long INITIAL_RESERVATION_RELEASE_CHECKER_DELAY = 30L * 1000L; // thirty seconds expressed in milliseconds
    protected long _nodeId = -1;

    protected List<StoragePoolAllocator> _storagePoolAllocators;

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    public void setStoragePoolAllocators(List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    protected List<HostAllocator> _hostAllocators;

    public List<HostAllocator> getHostAllocators() {
        return _hostAllocators;
    }

    public void setHostAllocators(List<HostAllocator> hostAllocators) {
        _hostAllocators = hostAllocators;
    }

    @Inject
    protected HostDao _hostDao;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected DedicatedResourceDao _dedicatedDao;
    @Inject
    protected GuestOSDao _guestOSDao = null;
    @Inject
    protected GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected StoragePoolHostDao _poolHostDao;

    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected StorageManager _storageMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    protected ClusterDetailsDao _clusterDetailsDao;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected ServiceOfferingDetailsDao _serviceOfferingDetailsDao;

    protected List<DeploymentPlanner> _planners;

    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }

    public void setPlanners(List<DeploymentPlanner> planners) {
        _planners = planners;
    }

    protected List<AffinityGroupProcessor> _affinityProcessors;

    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }

    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        _affinityProcessors = affinityProcessors;
    }

    @Override
    public DeployDestination planDeployment(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoids, DeploymentPlanner planner)
            throws InsufficientServerCapacityException, AffinityConflictException {

        // call affinitygroup chain
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());
        DataCenter dc = _dcDao.findById(vm.getDataCenterId());

        if (vmGroupCount > 0) {
            for (AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, avoids);
            }
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            checkForNonDedicatedResources(vmProfile, dc, avoids);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deploy avoids pods: " + avoids.getPodsToAvoid() + ", clusters: " + avoids.getClustersToAvoid() + ", hosts: " + avoids.getHostsToAvoid());
        }

        // call planners
        //DataCenter dc = _dcDao.findById(vm.getDataCenterId());
        // check if datacenter is in avoid set
        if (avoids.shouldAvoid(dc)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DataCenter id = '" + dc.getId() + "' provided is in avoid set, DeploymentPlanner cannot allocate the VM, returning.");
            }
            return null;
        }

        ServiceOffering offering = vmProfile.getServiceOffering();
        if(planner == null){
            String plannerName = offering.getDeploymentPlanner();
            if (plannerName == null) {
                if (vm.getHypervisorType() == HypervisorType.BareMetal) {
                    plannerName = "BareMetalPlanner";
                } else {
                    plannerName = _configDao.getValue(Config.VmDeploymentPlanner.key());
                }
            }
            planner = getDeploymentPlannerByName(plannerName);
        }

        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("DeploymentPlanner allocation algorithm: " + planner);

            s_logger.debug("Trying to allocate a host and storage pools from dc:" + plan.getDataCenterId() + ", pod:" + plan.getPodId() + ",cluster:" +
                    plan.getClusterId() + ", requested cpu: " + cpu_requested + ", requested ram: " + ram_requested);

            s_logger.debug("Is ROOT volume READY (pool already allocated)?: " + (plan.getPoolId() != null ? "Yes" : "No"));
        }

        String haVmTag = (String)vmProfile.getParameter(VirtualMachineProfile.Param.HaTag);

        if (plan.getHostId() != null && haVmTag == null) {
            Long hostIdSpecified = plan.getHostId();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DeploymentPlan has host_id specified, choosing this host and making no checks on this host: " + hostIdSpecified);
            }
            HostVO host = _hostDao.findById(hostIdSpecified);
            if (host == null) {
                s_logger.debug("The specified host cannot be found");
            } else if (avoids.shouldAvoid(host)) {
                s_logger.debug("The specified host is in avoid set");
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Looking for suitable pools for this host under zone: " + host.getDataCenterId() + ", pod: " + host.getPodId() + ", cluster: " +
                            host.getClusterId());
                }

                Pod pod = _podDao.findById(host.getPodId());
                Cluster cluster = _clusterDao.findById(host.getClusterId());

                if (vm.getHypervisorType() == HypervisorType.BareMetal) {
                    DeployDestination dest = new DeployDestination(dc, pod, cluster, host, new HashMap<Volume, StoragePool>());
                    s_logger.debug("Returning Deployment Destination: " + dest);
                    return dest;
                }

                // search for storage under the zone, pod, cluster of the host.
                DataCenterDeployment lastPlan =
                        new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), hostIdSpecified, plan.getPoolId(), null,
                                plan.getReservationContext());

                Pair<Map<Volume, List<StoragePool>>, List<Volume>> result = findSuitablePoolsForVolumes(vmProfile, lastPlan, avoids, HostAllocator.RETURN_UPTO_ALL);
                Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
                List<Volume> readyAndReusedVolumes = result.second();

                // choose the potential pool for this VM for this host
                if (!suitableVolumeStoragePools.isEmpty()) {
                    List<Host> suitableHosts = new ArrayList<Host>();
                    suitableHosts.add(host);
                    Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(
                            suitableHosts, suitableVolumeStoragePools, avoids,
                            getPlannerUsage(planner, vmProfile, plan, avoids), readyAndReusedVolumes);
                    if (potentialResources != null) {
                        pod = _podDao.findById(host.getPodId());
                        cluster = _clusterDao.findById(host.getClusterId());
                        Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                        // remove the reused vol<->pool from destination, since
                        // we don't have to prepare this volume.
                        for (Volume vol : readyAndReusedVolumes) {
                            storageVolMap.remove(vol);
                        }
                        DeployDestination dest = new DeployDestination(dc, pod, cluster, host, storageVolMap);
                        s_logger.debug("Returning Deployment Destination: " + dest);
                        return dest;
                    }
                }
            }
            s_logger.debug("Cannot deploy to specified host, returning.");
            return null;
        }

        if (vm.getLastHostId() != null && haVmTag == null) {
            s_logger.debug("This VM has last host_id specified, trying to choose the same host: " + vm.getLastHostId());

            HostVO host = _hostDao.findById(vm.getLastHostId());
            ServiceOfferingDetailsVO offeringDetails = null;
            if (host == null) {
                s_logger.debug("The last host of this VM cannot be found");
            } else if (avoids.shouldAvoid(host)) {
                s_logger.debug("The last host of this VM is in avoid set");
            } else if (plan.getClusterId() != null && host.getClusterId() != null
                    && !plan.getClusterId().equals(host.getClusterId())) {
                s_logger.debug("The last host of this VM cannot be picked as the plan specifies different clusterId: "
                        + plan.getClusterId());
            } else if (_capacityMgr.checkIfHostReachMaxGuestLimit(host)) {
                s_logger.debug("The last Host, hostId: " + host.getId() +
                        " already has max Running VMs(count includes system VMs), skipping this and trying other available hosts");
            } else if ((offeringDetails  = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString())) != null) {
                ServiceOfferingDetailsVO groupName = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.pciDevice.toString());
                if(!_resourceMgr.isGPUDeviceAvailable(host.getId(), groupName.getValue(), offeringDetails.getValue())){
                    s_logger.debug("The last host of this VM does not have required GPU devices available");
                }
            } else {
                if (host.getStatus() == Status.Up && host.getResourceState() == ResourceState.Enabled) {
                    boolean hostTagsMatch = true;
                    if(offering.getHostTag() != null){
                        _hostDao.loadHostTags(host);
                        if (!(host.getHostTags() != null && host.getHostTags().contains(offering.getHostTag()))) {
                            hostTagsMatch = false;
                        }
                    }
                    if (hostTagsMatch) {
                        long cluster_id = host.getClusterId();
                        ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id,
                                "cpuOvercommitRatio");
                        ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id,
                                "memoryOvercommitRatio");
                        Float cpuOvercommitRatio = Float.parseFloat(cluster_detail_cpu.getValue());
                        Float memoryOvercommitRatio = Float.parseFloat(cluster_detail_ram.getValue());

                        boolean hostHasCpuCapability, hostHasCapacity = false;
                        hostHasCpuCapability = _capacityMgr.checkIfHostHasCpuCapability(host.getId(), offering.getCpu(), offering.getSpeed());

                        if (hostHasCpuCapability) {
                            // first check from reserved capacity
                            hostHasCapacity = _capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, true, cpuOvercommitRatio, memoryOvercommitRatio, true);

                            // if not reserved, check the free capacity
                            if (!hostHasCapacity)
                                hostHasCapacity = _capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, false, cpuOvercommitRatio, memoryOvercommitRatio, true);
                        }

                        if (hostHasCapacity
                                && hostHasCpuCapability) {
                            s_logger.debug("The last host of this VM is UP and has enough capacity");
                            s_logger.debug("Now checking for suitable pools under zone: " + host.getDataCenterId()
                                    + ", pod: " + host.getPodId() + ", cluster: " + host.getClusterId());

                            Pod pod = _podDao.findById(host.getPodId());
                            Cluster cluster = _clusterDao.findById(host.getClusterId());
                            if (vm.getHypervisorType() == HypervisorType.BareMetal) {
                                DeployDestination dest = new DeployDestination(dc, pod, cluster, host, new HashMap<Volume, StoragePool>());
                                s_logger.debug("Returning Deployment Destination: " + dest);
                                return dest;
                            }

                            // search for storage under the zone, pod, cluster
                            // of
                            // the last host.
                            DataCenterDeployment lastPlan = new DataCenterDeployment(host.getDataCenterId(),
                                    host.getPodId(), host.getClusterId(), host.getId(), plan.getPoolId(), null);
                            Pair<Map<Volume, List<StoragePool>>, List<Volume>> result = findSuitablePoolsForVolumes(
                                    vmProfile, lastPlan, avoids, HostAllocator.RETURN_UPTO_ALL);
                            Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
                            List<Volume> readyAndReusedVolumes = result.second();

                            // choose the potential pool for this VM for this
                            // host
                            if (!suitableVolumeStoragePools.isEmpty()) {
                                List<Host> suitableHosts = new ArrayList<Host>();
                                suitableHosts.add(host);
                                Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(
                                        suitableHosts, suitableVolumeStoragePools, avoids,
                                        getPlannerUsage(planner, vmProfile, plan, avoids), readyAndReusedVolumes);
                                if (potentialResources != null) {
                                    Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                                    // remove the reused vol<->pool from
                                    // destination, since we don't have to
                                    // prepare
                                    // this volume.
                                    for (Volume vol : readyAndReusedVolumes) {
                                        storageVolMap.remove(vol);
                                    }
                                    DeployDestination dest = new DeployDestination(dc, pod, cluster, host,
                                            storageVolMap);
                                    s_logger.debug("Returning Deployment Destination: " + dest);
                                    return dest;
                                }
                            }
                        } else {
                            s_logger.debug("The last host of this VM does not have enough capacity");
                        }
                    } else {
                        s_logger.debug("Service Offering host tag does not match the last host of this VM");
                    }
                } else {
                    s_logger.debug("The last host of this VM is not UP or is not enabled, host status is: " + host.getStatus().name() + ", host resource state is: " +
                            host.getResourceState());
                }
            }
            s_logger.debug("Cannot choose the last host to deploy this VM ");
        }

        DeployDestination dest = null;
        List<Long> clusterList = null;

        if (planner != null && planner.canHandle(vmProfile, plan, avoids)) {
            while (true) {
                if (planner instanceof DeploymentClusterPlanner) {

                    ExcludeList plannerAvoidInput =
                            new ExcludeList(avoids.getDataCentersToAvoid(), avoids.getPodsToAvoid(), avoids.getClustersToAvoid(), avoids.getHostsToAvoid(),
                                    avoids.getPoolsToAvoid());

                    clusterList = ((DeploymentClusterPlanner)planner).orderClusters(vmProfile, plan, avoids);

                    if (clusterList != null && !clusterList.isEmpty()) {
                        // planner refactoring. call allocators to list hosts
                        ExcludeList plannerAvoidOutput =
                                new ExcludeList(avoids.getDataCentersToAvoid(), avoids.getPodsToAvoid(), avoids.getClustersToAvoid(), avoids.getHostsToAvoid(),
                                        avoids.getPoolsToAvoid());

                        resetAvoidSet(plannerAvoidOutput, plannerAvoidInput);

                        dest =
                                checkClustersforDestination(clusterList, vmProfile, plan, avoids, dc, getPlannerUsage(planner, vmProfile, plan, avoids), plannerAvoidOutput);
                        if (dest != null) {
                            return dest;
                        }
                        // reset the avoid input to the planners
                        resetAvoidSet(avoids, plannerAvoidOutput);

                    } else {
                        return null;
                    }
                } else {
                    dest = planner.plan(vmProfile, plan, avoids);
                    if (dest != null) {
                        long hostId = dest.getHost().getId();
                        avoids.addHost(dest.getHost().getId());

                        if (checkIfHostFitsPlannerUsage(hostId, DeploymentPlanner.PlannerResourceUsage.Shared)) {
                            // found destination
                            return dest;
                        } else {
                            // find another host - seems some concurrent
                            // deployment picked it up for dedicated access
                            continue;
                        }
                    } else {
                        return null;
                    }
                }
            }
        }

        return dest;
    }

    @Override
    public DeploymentPlanner getDeploymentPlannerByName(String plannerName) {
        if (plannerName != null) {
            for (DeploymentPlanner plannerInList : _planners) {
                if (plannerName.equalsIgnoreCase(plannerInList.getName())) {
                    return plannerInList;
                }
            }
        }

        return null;
    }

    private void checkForNonDedicatedResources(VirtualMachineProfile vmProfile, DataCenter dc, ExcludeList avoids) {
        boolean isExplicit = false;
        VirtualMachine vm = vmProfile.getVirtualMachine();

        // check if zone is dedicated. if yes check if vm owner has acess to it.
        DedicatedResourceVO dedicatedZone = _dedicatedDao.findByZoneId(dc.getId());
        if (dedicatedZone != null && !_accountMgr.isRootAdmin(vmProfile.getOwner().getId())) {
            long accountDomainId = vmProfile.getOwner().getDomainId();
            long accountId = vmProfile.getOwner().getAccountId();

            // If a zone is dedicated to an account then all hosts in this zone
            // will be explicitly dedicated to
            // that account. So there won't be any shared hosts in the zone, the
            // only way to deploy vms from that
            // account will be to use explicit dedication affinity group.
            if (dedicatedZone.getAccountId() != null) {
                if (dedicatedZone.getAccountId().equals(accountId)) {
                    return;
                } else {
                    throw new CloudRuntimeException("Failed to deploy VM, Zone " + dc.getName() + " not available for the user account " + vmProfile.getOwner());
                }
            }

            // if zone is dedicated to a domain. Check owner's access to the
            // domain level dedication group
            if (!_affinityGroupService.isAffinityGroupAvailableInDomain(dedicatedZone.getAffinityGroupId(), accountDomainId)) {
                throw new CloudRuntimeException("Failed to deploy VM, Zone " + dc.getName() + " not available for the user domain " + vmProfile.getOwner());
            }

        }

        // check affinity group of type Explicit dedication exists. If No put
        // dedicated pod/cluster/host in avoid list
        List<AffinityGroupVMMapVO> vmGroupMappings = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), "ExplicitDedication");

        if (vmGroupMappings != null && !vmGroupMappings.isEmpty()) {
            isExplicit = true;
        }

        if (!isExplicit) {
            //add explicitly dedicated resources in avoidList

            List<Long> allPodsInDc = _podDao.listAllPods(dc.getId());
            List<Long> allDedicatedPods = _dedicatedDao.listAllPods();
            allPodsInDc.retainAll(allDedicatedPods);
            avoids.addPodList(allPodsInDc);

            List<Long> allClustersInDc = _clusterDao.listAllCusters(dc.getId());
            List<Long> allDedicatedClusters = _dedicatedDao.listAllClusters();
            allClustersInDc.retainAll(allDedicatedClusters);
            avoids.addClusterList(allClustersInDc);

            List<Long> allHostsInDc = _hostDao.listAllHosts(dc.getId());
            List<Long> allDedicatedHosts = _dedicatedDao.listAllHosts();
            allHostsInDc.retainAll(allDedicatedHosts);
            avoids.addHostList(allHostsInDc);
        }
    }

    private void resetAvoidSet(ExcludeList avoidSet, ExcludeList removeSet) {
        if (avoidSet.getDataCentersToAvoid() != null && removeSet.getDataCentersToAvoid() != null) {
            avoidSet.getDataCentersToAvoid().removeAll(removeSet.getDataCentersToAvoid());
        }
        if (avoidSet.getPodsToAvoid() != null && removeSet.getPodsToAvoid() != null) {
            avoidSet.getPodsToAvoid().removeAll(removeSet.getPodsToAvoid());
        }
        if (avoidSet.getClustersToAvoid() != null && removeSet.getClustersToAvoid() != null) {
            avoidSet.getClustersToAvoid().removeAll(removeSet.getClustersToAvoid());
        }
        if (avoidSet.getHostsToAvoid() != null && removeSet.getHostsToAvoid() != null) {
            avoidSet.getHostsToAvoid().removeAll(removeSet.getHostsToAvoid());
        }
        if (avoidSet.getPoolsToAvoid() != null && removeSet.getPoolsToAvoid() != null) {
            avoidSet.getPoolsToAvoid().removeAll(removeSet.getPoolsToAvoid());
        }
    }

    private PlannerResourceUsage getPlannerUsage(DeploymentPlanner planner, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoids)
            throws InsufficientServerCapacityException {
        if (planner != null && planner instanceof DeploymentClusterPlanner) {
            return ((DeploymentClusterPlanner)planner).getResourceUsage(vmProfile, plan, avoids);
        } else {
            return DeploymentPlanner.PlannerResourceUsage.Shared;
        }

    }

    @DB
    private boolean checkIfHostFitsPlannerUsage(final long hostId, final PlannerResourceUsage resourceUsageRequired) {
        // TODO Auto-generated method stub
        // check if this host has been picked up by some other planner
        // exclusively
        // if planner can work with shared host, check if this host has
        // been marked as 'shared'
        // else if planner needs dedicated host,

        PlannerHostReservationVO reservationEntry = _plannerHostReserveDao.findByHostId(hostId);
        if (reservationEntry != null) {
            final long id = reservationEntry.getId();
            PlannerResourceUsage hostResourceType = reservationEntry.getResourceUsage();

            if (hostResourceType != null) {
                if (hostResourceType == resourceUsageRequired) {
                    return true;
                } else {
                    s_logger.debug("Cannot use this host for usage: " + resourceUsageRequired + ", since this host has been reserved for planner usage : " +
                            hostResourceType);
                    return false;
                }
            } else {
                final PlannerResourceUsage hostResourceTypeFinal = hostResourceType;
                // reserve the host for required resourceType
                // let us lock the reservation entry before updating.
                return Transaction.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        final PlannerHostReservationVO lockedEntry = _plannerHostReserveDao.lockRow(id, true);
                        if (lockedEntry == null) {
                            s_logger.error("Unable to lock the host entry for reservation, host: " + hostId);
                            return false;
                        }
                        // check before updating
                        if (lockedEntry.getResourceUsage() == null) {
                            lockedEntry.setResourceUsage(resourceUsageRequired);
                            _plannerHostReserveDao.persist(lockedEntry);
                            return true;
                        } else {
                            // someone updated it earlier. check if we can still use it
                            if (lockedEntry.getResourceUsage() == resourceUsageRequired) {
                                return true;
                            } else {
                                s_logger.debug("Cannot use this host for usage: " + resourceUsageRequired + ", since this host has been reserved for planner usage : " +
                                        hostResourceTypeFinal);
                                return false;
                            }
                        }
                    }
                });

            }

        }

        return false;
    }

    @DB
    public boolean checkHostReservationRelease(final Long hostId) {

        if (hostId != null) {
            PlannerHostReservationVO reservationEntry = _plannerHostReserveDao.findByHostId(hostId);
            if (reservationEntry != null && reservationEntry.getResourceUsage() != null) {

                // check if any VMs are starting or running on this host
                List<VMInstanceVO> vms = _vmInstanceDao.listUpByHostId(hostId);
                if (vms.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cannot release reservation, Found " + vms.size() + " VMs Running on host " + hostId);
                    }
                    return false;
                }

                List<VMInstanceVO> vmsByLastHostId = _vmInstanceDao.listByLastHostId(hostId);
                if (vmsByLastHostId.size() > 0) {
                    // check if any VMs are within skip.counting.hours, if yes
                    // we
                    // cannot release the host
                    for (VMInstanceVO stoppedVM : vmsByLastHostId) {
                        long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - stoppedVM.getUpdateTime().getTime()) / 1000;
                        if (secondsSinceLastUpdate < _vmCapacityReleaseInterval) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Cannot release reservation, Found VM: " + stoppedVM + " Stopped but reserved on host " + hostId);
                            }
                            return false;
                        }
                    }
                }

                // check if any VMs are stopping on or migrating to this host
                List<VMInstanceVO> vmsStoppingMigratingByHostId = _vmInstanceDao.findByHostInStates(hostId, State.Stopping, State.Migrating, State.Starting);
                if (vmsStoppingMigratingByHostId.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cannot release reservation, Found " + vmsStoppingMigratingByHostId.size() + " VMs stopping/migrating/starting on host " + hostId);
                    }
                    return false;
                }

                // check if any VMs are in starting state with no hostId set yet
                // -
                // just ignore host release to avoid race condition
                List<VMInstanceVO> vmsStartingNoHost = _vmInstanceDao.listStartingWithNoHostId();

                if (vmsStartingNoHost.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cannot release reservation, Found " + vms.size() + " VMs starting as of now and no hostId yet stored");
                    }
                    return false;
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host has no VMs associated, releasing the planner reservation for host " + hostId);
                }

                final long id = reservationEntry.getId();

                return Transaction.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        final PlannerHostReservationVO lockedEntry = _plannerHostReserveDao.lockRow(id, true);
                        if (lockedEntry == null) {
                            s_logger.error("Unable to lock the host entry for reservation, host: " + hostId);
                            return false;
                        }
                        // check before updating
                        if (lockedEntry.getResourceUsage() != null) {
                            lockedEntry.setResourceUsage(null);
                            _plannerHostReserveDao.persist(lockedEntry);
                            return true;
                        }

                        return false;
                    }
                });
            }

        }
        return false;
    }

    class HostReservationReleaseChecker extends ManagedContextTimerTask {
        @Override
        protected void runInContext() {
            try {
                s_logger.debug("Checking if any host reservation can be released ... ");
                checkHostReservations();
                s_logger.debug("Done running HostReservationReleaseChecker ... ");
            } catch (Throwable t) {
                s_logger.error("Exception in HostReservationReleaseChecker", t);
            }
        }
    }

    private void checkHostReservations() {
        List<PlannerHostReservationVO> reservedHosts = _plannerHostReserveDao.listAllReservedHosts();

        for (PlannerHostReservationVO hostReservation : reservedHosts) {
            HostVO host = _hostDao.findById(hostReservation.getHostId());
            if (host != null && host.getManagementServerId() != null && host.getManagementServerId() == _nodeId) {
                checkHostReservationRelease(hostReservation.getHostId());
            }
        }

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
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        PlannerHostReservationVO reservationEntry = _plannerHostReserveDao.findByHostId(host.getId());
        if (reservationEntry == null) {
            // record the host in this table
            PlannerHostReservationVO newHost = new PlannerHostReservationVO(host.getId(), host.getDataCenterId(), host.getPodId(), host.getClusterId());
            _plannerHostReserveDao.persist(newHost);
        }

    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        // TODO Auto-generated method stub
        return false;
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
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _agentMgr.registerForHostEvents(this, true, false, true);
        VirtualMachine.State.getStateMachine().registerListener(this);
        _messageBus.subscribe("VM_ReservedCapacity_Free", new MessageSubscriber() {
            @Override
            public void onPublishMessage(String senderAddress, String subject, Object obj) {
                VMInstanceVO vm = ((VMInstanceVO)obj);
                s_logger.debug("MessageBus message: host reserved capacity released for VM: " + vm.getLastHostId() +
                        ", checking if host reservation can be released for host:" + vm.getLastHostId());
                Long hostId = vm.getLastHostId();
                checkHostReservationRelease(hostId);
            }
        });

        _vmCapacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);

        String hostReservationReleasePeriod = _configDao.getValue(Config.HostReservationReleasePeriod.key());
        if (hostReservationReleasePeriod != null) {
            _hostReservationReleasePeriod = Long.parseLong(hostReservationReleasePeriod);
            if (_hostReservationReleasePeriod <= 0)
                _hostReservationReleasePeriod = Long.parseLong(Config.HostReservationReleasePeriod.getDefaultValue());
        }

        _timer = new Timer("HostReservationReleaseChecker");

        _nodeId = ManagementServerNode.getManagementServerId();

        return super.configure(name, params);
    }

    @Override
    public boolean start() {
        _timer.schedule(new HostReservationReleaseChecker(), INITIAL_RESERVATION_RELEASE_CHECKER_DELAY, _hostReservationReleasePeriod);
        cleanupVMReservations();
        return true;
    }

    @Override
    public boolean stop() {
        _timer.cancel();
        return true;
    }

    @Override
    public void cleanupVMReservations() {
        List<VMReservationVO> reservations = _reservationDao.listAll();

        for (VMReservationVO reserv : reservations) {
            VMInstanceVO vm = _vmInstanceDao.findById(reserv.getVmId());
            if (vm != null) {
                if (vm.getState() == State.Starting || (vm.getState() == State.Stopped && vm.getLastHostId() == null)) {
                    continue;
                } else {
                    // delete reservation
                    _reservationDao.remove(reserv.getId());
                }
            } else {
                // delete reservation
                _reservationDao.remove(reserv.getId());
            }
        }
    }

    // /refactoring planner methods
    private DeployDestination checkClustersforDestination(List<Long> clusterList, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, DataCenter dc,
            DeploymentPlanner.PlannerResourceUsage resourceUsageRequired, ExcludeList plannerAvoidOutput) {

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List to consider: " + clusterList);
        }

        for (Long clusterId : clusterList) {
            ClusterVO clusterVO = _clusterDao.findById(clusterId);

            if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
                s_logger.debug("Cluster: " + clusterId + " has HyperVisorType that does not match the VM, skipping this cluster");
                avoid.addCluster(clusterVO.getId());
                continue;
            }

            s_logger.debug("Checking resources in Cluster: " + clusterId + " under Pod: " + clusterVO.getPodId());
            // search for resources(hosts and storage) under this zone, pod,
            // cluster.
            DataCenterDeployment potentialPlan =
                    new DataCenterDeployment(plan.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), null, plan.getPoolId(), null, plan.getReservationContext());

            // find suitable hosts under this cluster, need as many hosts as we
            // get.
            List<Host> suitableHosts = findSuitableHosts(vmProfile, potentialPlan, avoid, HostAllocator.RETURN_UPTO_ALL);
            // if found suitable hosts in this cluster, find suitable storage
            // pools for each volume of the VM
            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                if (vmProfile.getHypervisorType() == HypervisorType.BareMetal) {
                    Pod pod = _podDao.findById(clusterVO.getPodId());
                    DeployDestination dest = new DeployDestination(dc, pod, clusterVO, suitableHosts.get(0));
                    return dest;
                }

                Pair<Map<Volume, List<StoragePool>>, List<Volume>> result =
                        findSuitablePoolsForVolumes(vmProfile, potentialPlan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
                Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
                List<Volume> readyAndReusedVolumes = result.second();

                // choose the potential host and pool for the VM
                if (!suitableVolumeStoragePools.isEmpty()) {
                    Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(
                            suitableHosts, suitableVolumeStoragePools, avoid, resourceUsageRequired,
                            readyAndReusedVolumes);

                    if (potentialResources != null) {
                        Pod pod = _podDao.findById(clusterVO.getPodId());
                        Host host = _hostDao.findById(potentialResources.first().getId());
                        Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                        // remove the reused vol<->pool from destination, since
                        // we don't have to prepare this volume.
                        for (Volume vol : readyAndReusedVolumes) {
                            storageVolMap.remove(vol);
                        }
                        DeployDestination dest = new DeployDestination(dc, pod, clusterVO, host, storageVolMap);
                        s_logger.debug("Returning Deployment Destination: " + dest);
                        return dest;
                    }
                } else {
                    s_logger.debug("No suitable storagePools found under this Cluster: " + clusterId);
                }
            } else {
                s_logger.debug("No suitable hosts found under this Cluster: " + clusterId);
            }

            if (canAvoidCluster(clusterVO, avoid, plannerAvoidOutput, vmProfile)) {
                avoid.addCluster(clusterVO.getId());
            }
        }
        s_logger.debug("Could not find suitable Deployment Destination for this VM under any clusters, returning. ");
        return null;
    }

    private boolean canAvoidCluster(Cluster clusterVO, ExcludeList avoids, ExcludeList plannerAvoidOutput, VirtualMachineProfile vmProfile) {

        ExcludeList allocatorAvoidOutput =
                new ExcludeList(avoids.getDataCentersToAvoid(), avoids.getPodsToAvoid(), avoids.getClustersToAvoid(), avoids.getHostsToAvoid(), avoids.getPoolsToAvoid());

        // remove any hosts/pools that the planners might have added
        // to get the list of hosts/pools that Allocators flagged as 'avoid'

        resetAvoidSet(allocatorAvoidOutput, plannerAvoidOutput);

        // if all hosts or all pools in the cluster are in avoid set after this
        // pass, then put the cluster in avoid set.
        boolean avoidAllHosts = true;
        boolean avoidAllPools = true;
        boolean avoidAllLocalPools = true;
        boolean avoidAllSharedPools = true;

        List<HostVO> allhostsInCluster =
                _hostDao.listAllUpAndEnabledNonHAHosts(Host.Type.Routing, clusterVO.getId(), clusterVO.getPodId(), clusterVO.getDataCenterId(), null);
        for (HostVO host : allhostsInCluster) {
            if (!allocatorAvoidOutput.shouldAvoid(host)) {
                // there's some host in the cluster that is not yet in avoid set
                avoidAllHosts = false;
                break;
            }
        }

        // all hosts in avoid set, avoid the cluster. Otherwise check the pools
        if (avoidAllHosts) {
            return true;
        }

        // Cluster can be put in avoid set in following scenarios:
        // 1. If storage allocators haven't put any pools in avoid set means either no pools in cluster
        // or pools not suitable for the allocators to handle or there is no
        // linkage of any suitable host to any of the pools in cluster
        // 2. If all 'shared' or 'local' pools are in avoid set
        if  (allocatorAvoidOutput.getPoolsToAvoid() != null && !allocatorAvoidOutput.getPoolsToAvoid().isEmpty()) {

            Pair<Boolean, Boolean> storageRequirements = findVMStorageRequirements(vmProfile);
            boolean vmRequiresSharedStorage = storageRequirements.first();
            boolean vmRequiresLocalStorege = storageRequirements.second();

            if (vmRequiresSharedStorage) {
                // check shared pools
                List<StoragePoolVO> allPoolsInCluster = _storagePoolDao.findPoolsByTags(clusterVO.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), null);
                for (StoragePoolVO pool : allPoolsInCluster) {
                    if (!allocatorAvoidOutput.shouldAvoid(pool)) {
                        // there's some pool in the cluster that is not yet in avoid set
                        avoidAllSharedPools = false;
                        break;
                    }
                }
            }

            if (vmRequiresLocalStorege) {
                // check local pools
                List<StoragePoolVO> allLocalPoolsInCluster =
                        _storagePoolDao.findLocalStoragePoolsByTags(clusterVO.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), null);
                for (StoragePoolVO pool : allLocalPoolsInCluster) {
                    if (!allocatorAvoidOutput.shouldAvoid(pool)) {
                        // there's some pool in the cluster that is not yet
                        // in avoid set
                        avoidAllLocalPools = false;
                        break;
                    }
                }
            }

            if (vmRequiresSharedStorage && vmRequiresLocalStorege) {
                avoidAllPools = (avoidAllLocalPools || avoidAllSharedPools) ? true : false;
            } else if (vmRequiresSharedStorage) {
                avoidAllPools = avoidAllSharedPools;
            } else if (vmRequiresLocalStorege) {
                avoidAllPools = avoidAllLocalPools;
            }
        }

        if (avoidAllHosts || avoidAllPools) {
            return true;
        }
        return false;
    }

    private Pair<Boolean, Boolean> findVMStorageRequirements(VirtualMachineProfile vmProfile) {

        boolean requiresShared = false, requiresLocal = false;

        List<VolumeVO> volumesTobeCreated = _volsDao.findUsableVolumesForInstance(vmProfile.getId());

        // for each volume find whether shared or local pool is required
        for (VolumeVO toBeCreated : volumesTobeCreated) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(toBeCreated.getDiskOfferingId());

            if (diskOffering != null) {
                if (diskOffering.getUseLocalStorage()) {
                    requiresLocal = true;
                } else {
                    requiresShared = true;
                }
            }
        }

        return new Pair<Boolean, Boolean>(requiresShared, requiresLocal);
    }

    protected Pair<Host, Map<Volume, StoragePool>> findPotentialDeploymentResources(List<Host> suitableHosts, Map<Volume, List<StoragePool>> suitableVolumeStoragePools,
            ExcludeList avoid, DeploymentPlanner.PlannerResourceUsage resourceUsageRequired, List<Volume> readyAndReusedVolumes) {
        s_logger.debug("Trying to find a potenial host and associated storage pools from the suitable host/pool lists for this VM");

        boolean hostCanAccessPool = false;
        boolean haveEnoughSpace = false;

        if (readyAndReusedVolumes == null) {
            readyAndReusedVolumes = new ArrayList<Volume>();
        }
        Map<Volume, StoragePool> storage = new HashMap<Volume, StoragePool>();
        TreeSet<Volume> volumesOrderBySizeDesc = new TreeSet<Volume>(new Comparator<Volume>() {
            @Override
            public int compare(Volume v1, Volume v2) {
                if (v1.getSize() < v2.getSize())
                    return 1;
                else
                    return -1;
            }
        });
        volumesOrderBySizeDesc.addAll(suitableVolumeStoragePools.keySet());
        boolean multipleVolume = volumesOrderBySizeDesc.size() > 1;
        for (Host potentialHost : suitableHosts) {
            Map<StoragePool, List<Volume>> volumeAllocationMap = new HashMap<StoragePool, List<Volume>>();
            for (Volume vol : volumesOrderBySizeDesc) {
                haveEnoughSpace = false;
                s_logger.debug("Checking if host: " + potentialHost.getId() + " can access any suitable storage pool for volume: " + vol.getVolumeType());
                List<StoragePool> volumePoolList = suitableVolumeStoragePools.get(vol);
                hostCanAccessPool = false;
                for (StoragePool potentialSPool : volumePoolList) {
                    if (hostCanAccessSPool(potentialHost, potentialSPool)) {
                        hostCanAccessPool = true;
                        if (multipleVolume && !readyAndReusedVolumes.contains(vol)) {
                            List<Volume> requestVolumes = null;
                            if (volumeAllocationMap.containsKey(potentialSPool))
                                requestVolumes = volumeAllocationMap.get(potentialSPool);
                            else
                                requestVolumes = new ArrayList<Volume>();
                            requestVolumes.add(vol);

                            if (!_storageMgr.storagePoolHasEnoughSpace(requestVolumes, potentialSPool))
                                continue;
                            volumeAllocationMap.put(potentialSPool, requestVolumes);
                        }
                        storage.put(vol, potentialSPool);
                        haveEnoughSpace = true;
                        break;
                    }
                }
                if (!hostCanAccessPool) {
                    break;
                }
                if (!haveEnoughSpace) {
                    s_logger.warn("insufficient capacity to allocate all volumes");
                    break;
                }
            }
            if (hostCanAccessPool && haveEnoughSpace && checkIfHostFitsPlannerUsage(potentialHost.getId(), resourceUsageRequired)) {
                s_logger.debug("Found a potential host " + "id: " + potentialHost.getId() + " name: " + potentialHost.getName() +
                        " and associated storage pools for this VM");
                return new Pair<Host, Map<Volume, StoragePool>>(potentialHost, storage);
            } else {
                avoid.addHost(potentialHost.getId());
            }
        }
        s_logger.debug("Could not find a potential host that has associated storage pools from the suitable host/pool lists for this VM");
        return null;
    }

    protected boolean hostCanAccessSPool(Host host, StoragePool pool) {
        boolean hostCanAccessSPool = false;

        StoragePoolHostVO hostPoolLinkage = _poolHostDao.findByPoolHost(pool.getId(), host.getId());
        if (hostPoolLinkage != null) {
            hostCanAccessSPool = true;
        }

        s_logger.debug("Host: " + host.getId() + (hostCanAccessSPool ? " can" : " cannot") + " access pool: " + pool.getId());
        return hostCanAccessSPool;
    }

    protected List<Host> findSuitableHosts(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
        List<Host> suitableHosts = new ArrayList<Host>();
        for (HostAllocator allocator : _hostAllocators) {
            suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, avoid, returnUpTo);
            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                break;
            }
        }

        if (suitableHosts.isEmpty()) {
            s_logger.debug("No suitable hosts found");
        }
        return suitableHosts;
    }

    protected Pair<Map<Volume, List<StoragePool>>, List<Volume>> findSuitablePoolsForVolumes(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid,
            int returnUpTo) {
        List<VolumeVO> volumesTobeCreated = _volsDao.findUsableVolumesForInstance(vmProfile.getId());
        Map<Volume, List<StoragePool>> suitableVolumeStoragePools = new HashMap<Volume, List<StoragePool>>();
        List<Volume> readyAndReusedVolumes = new ArrayList<Volume>();

        // There should be atleast the ROOT volume of the VM in usable state
        if (volumesTobeCreated.isEmpty()) {
            throw new CloudRuntimeException("Unable to create deployment, no usable volumes found for the VM");
        }

        // don't allow to start vm that doesn't have a root volume
        if (_volsDao.findByInstanceAndType(vmProfile.getId(), Volume.Type.ROOT).isEmpty()) {
            throw new CloudRuntimeException("Unable to prepare volumes for vm as ROOT volume is missing");
        }

        // for each volume find list of suitable storage pools by calling the
        // allocators
        Set<Long> originalAvoidPoolSet = avoid.getPoolsToAvoid();
        if (originalAvoidPoolSet == null) {
            originalAvoidPoolSet = new HashSet<Long>();
        }
        Set<Long> poolsToAvoidOutput = new HashSet<Long>(originalAvoidPoolSet);

        for (VolumeVO toBeCreated : volumesTobeCreated) {
            s_logger.debug("Checking suitable pools for volume (Id, Type): (" + toBeCreated.getId() + "," + toBeCreated.getVolumeType().name() + ")");

            // If the plan specifies a poolId, it means that this VM's ROOT
            // volume is ready and the pool should be reused.
            // In this case, also check if rest of the volumes are ready and can
            // be reused.
            if (plan.getPoolId() != null || (toBeCreated.getVolumeType() == Volume.Type.DATADISK && toBeCreated.getPoolId() != null && toBeCreated.getState() == Volume.State.Ready)) {
                s_logger.debug("Volume has pool already allocated, checking if pool can be reused, poolId: " + toBeCreated.getPoolId());
                List<StoragePool> suitablePools = new ArrayList<StoragePool>();
                StoragePool pool = null;
                if (toBeCreated.getPoolId() != null) {
                    pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(toBeCreated.getPoolId());
                } else {
                    pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(plan.getPoolId());
                }

                if (!pool.isInMaintenance()) {
                    if (!avoid.shouldAvoid(pool)) {
                        long exstPoolDcId = pool.getDataCenterId();
                        long exstPoolPodId = pool.getPodId() != null ? pool.getPodId() : -1;
                        long exstPoolClusterId = pool.getClusterId() != null ? pool.getClusterId() : -1;
                        boolean canReusePool = false;
                        if (plan.getDataCenterId() == exstPoolDcId && plan.getPodId() == exstPoolPodId && plan.getClusterId() == exstPoolClusterId) {
                            canReusePool = true;
                        } else if (plan.getDataCenterId() == exstPoolDcId) {
                            DataStore dataStore = dataStoreMgr.getPrimaryDataStore(pool.getId());
                            if (dataStore != null && dataStore.getScope() != null && dataStore.getScope().getScopeType() == ScopeType.ZONE) {
                                canReusePool = true;
                            }
                        } else {
                            s_logger.debug("Pool of the volume does not fit the specified plan, need to reallocate a pool for this volume");
                            canReusePool = false;
                        }

                        if (canReusePool) {
                            s_logger.debug("Planner need not allocate a pool for this volume since its READY");
                            suitablePools.add(pool);
                            suitableVolumeStoragePools.put(toBeCreated, suitablePools);
                            if (!(toBeCreated.getState() == Volume.State.Allocated || toBeCreated.getState() == Volume.State.Creating)) {
                                readyAndReusedVolumes.add(toBeCreated);
                            }
                            continue;
                        }
                    } else {
                        s_logger.debug("Pool of the volume is in avoid set, need to reallocate a pool for this volume");
                    }
                } else {
                    s_logger.debug("Pool of the volume is in maintenance, need to reallocate a pool for this volume");
                }
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("We need to allocate new storagepool for this volume");
            }
            if (!isRootAdmin(vmProfile)) {
                if (!isEnabledForAllocation(plan.getDataCenterId(), plan.getPodId(), plan.getClusterId())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cannot allocate new storagepool for this volume in this cluster, allocation state is disabled");
                        s_logger.debug("Cannot deploy to this specified plan, allocation state is disabled, returning.");
                    }
                    // Cannot find suitable storage pools under this cluster for
                    // this volume since allocation_state is disabled.
                    // - remove any suitable pools found for other volumes.
                    // All volumes should get suitable pools under this cluster;
                    // else we cant use this cluster.
                    suitableVolumeStoragePools.clear();
                    break;
                }
            }

            s_logger.debug("Calling StoragePoolAllocators to find suitable pools");

            DiskOfferingVO diskOffering = _diskOfferingDao.findById(toBeCreated.getDiskOfferingId());

            if (vmProfile.getTemplate().getFormat() == Storage.ImageFormat.ISO && vmProfile.getServiceOffering().getTagsArray().length != 0) {
                diskOffering.setTagsArray(Arrays.asList(vmProfile.getServiceOffering().getTagsArray()));
            }

            DiskProfile diskProfile = new DiskProfile(toBeCreated, diskOffering, vmProfile.getHypervisorType());
            boolean useLocalStorage = false;
            if (vmProfile.getType() != VirtualMachine.Type.User) {
                DataCenterVO zone = _dcDao.findById(plan.getDataCenterId());
                assert (zone != null) : "Invalid zone in deployment plan";
                Boolean useLocalStorageForSystemVM = ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(zone.getId());
                if (useLocalStorageForSystemVM != null) {
                    useLocalStorage = useLocalStorageForSystemVM.booleanValue();
                    s_logger.debug("System VMs will use " + (useLocalStorage ? "local" : "shared") + " storage for zone id=" + plan.getDataCenterId());
                }
            } else {
                useLocalStorage = diskOffering.getUseLocalStorage();

                // TODO: this is a hacking fix for the problem of deploy
                // ISO-based VM on local storage
                // when deploying VM based on ISO, we have a service offering
                // and an additional disk offering, use-local storage flag is
                // actually
                // saved in service offering, override the flag from service
                // offering when it is a ROOT disk
                if (!useLocalStorage && vmProfile.getServiceOffering().getUseLocalStorage()) {
                    if (toBeCreated.getVolumeType() == Volume.Type.ROOT) {
                        useLocalStorage = true;
                    }
                }
            }
            diskProfile.setUseLocalStorage(useLocalStorage);

            boolean foundPotentialPools = false;
            for (StoragePoolAllocator allocator : _storagePoolAllocators) {
                final List<StoragePool> suitablePools = allocator.allocateToPool(diskProfile, vmProfile, plan, avoid, returnUpTo);
                if (suitablePools != null && !suitablePools.isEmpty()) {
                    suitableVolumeStoragePools.put(toBeCreated, suitablePools);
                    foundPotentialPools = true;
                    break;
                }
            }

            if (avoid.getPoolsToAvoid() != null) {
                poolsToAvoidOutput.addAll(avoid.getPoolsToAvoid());
                avoid.getPoolsToAvoid().retainAll(originalAvoidPoolSet);
            }

            if (!foundPotentialPools) {
                s_logger.debug("No suitable pools found for volume: " + toBeCreated + " under cluster: " + plan.getClusterId());
                // No suitable storage pools found under this cluster for this
                // volume. - remove any suitable pools found for other volumes.
                // All volumes should get suitable pools under this cluster;
                // else we cant use this cluster.
                suitableVolumeStoragePools.clear();
                break;
            }
        }

        HashSet<Long> toRemove = new HashSet<Long>();
        for (List<StoragePool> lsp : suitableVolumeStoragePools.values()) {
            for (StoragePool sp : lsp) {
                toRemove.add(sp.getId());
            }
        }
        poolsToAvoidOutput.removeAll(toRemove);

        if (avoid.getPoolsToAvoid() != null) {
            avoid.getPoolsToAvoid().addAll(poolsToAvoidOutput);
        }

        if (suitableVolumeStoragePools.isEmpty()) {
            s_logger.debug("No suitable pools found");
        }

        return new Pair<Map<Volume, List<StoragePool>>, List<Volume>>(suitableVolumeStoragePools, readyAndReusedVolumes);
    }

    private boolean isEnabledForAllocation(long zoneId, Long podId, Long clusterId) {
        // Check if the zone exists in the system
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone != null && Grouping.AllocationState.Disabled == zone.getAllocationState()) {
            s_logger.info("Zone is currently disabled, cannot allocate to this zone: " + zoneId);
            return false;
        }

        Pod pod = _podDao.findById(podId);
        if (pod != null && Grouping.AllocationState.Disabled == pod.getAllocationState()) {
            s_logger.info("Pod is currently disabled, cannot allocate to this pod: " + podId);
            return false;
        }

        Cluster cluster = _clusterDao.findById(clusterId);
        if (cluster != null && Grouping.AllocationState.Disabled == cluster.getAllocationState()) {
            s_logger.info("Cluster is currently disabled, cannot allocate to this cluster: " + clusterId);
            return false;
        }

        return true;
    }

    private boolean isRootAdmin(VirtualMachineProfile vmProfile) {
        if (vmProfile != null) {
            if (vmProfile.getOwner() != null) {
                return _accountMgr.isRootAdmin(vmProfile.getOwner().getId());
            } else {
                return false;
            }
        }
        return false;
    }

    @DB
    @Override
    public String finalizeReservation(final DeployDestination plannedDestination, final VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoids, final DeploymentPlanner planner)
            throws InsufficientServerCapacityException, AffinityConflictException {

        final VirtualMachine vm = vmProfile.getVirtualMachine();
        final long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        return Transaction.execute(new TransactionCallback<String>() {
            @Override
            public String doInTransaction(TransactionStatus status) {
                boolean saveReservation = true;

                if (vmGroupCount > 0) {
                    List<Long> groupIds = _affinityGroupVMMapDao.listAffinityGroupIdsByVmId(vm.getId());
                    SearchCriteria<AffinityGroupVO> criteria = _affinityGroupDao.createSearchCriteria();
                    criteria.addAnd("id", SearchCriteria.Op.IN, groupIds.toArray(new Object[groupIds.size()]));
                    _affinityGroupDao.lockRows(criteria, null, true);

                    for (AffinityGroupProcessor processor : _affinityProcessors) {
                        if (!processor.check(vmProfile, plannedDestination)) {
                            saveReservation = false;
                            break;
                        }
                    }
                }

                if (saveReservation) {
                    VMReservationVO vmReservation =
                            new VMReservationVO(vm.getId(), plannedDestination.getDataCenter().getId(), plannedDestination.getPod().getId(), plannedDestination.getCluster()
                                    .getId(), plannedDestination.getHost().getId());
                    if (planner != null) {
                        vmReservation.setDeploymentPlanner(planner.getName());
                    }
                    Map<Long, Long> volumeReservationMap = new HashMap<Long, Long>();

                    if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                        for (Volume vo : plannedDestination.getStorageForDisks().keySet()) {
                            volumeReservationMap.put(vo.getId(), plannedDestination.getStorageForDisks().get(vo).getId());
                        }
                        vmReservation.setVolumeReservation(volumeReservationMap);
                    }
                    _reservationDao.persist(vmReservation);
                    return vmReservation.getUuid();
                }

                return null;
            }
        });
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vo, boolean status, Object opaque) {
      if (!status) {
        return false;
      }
      State oldState = transition.getCurrentState();
      State newState = transition.getToState();
      if ((oldState == State.Starting) && (newState != State.Starting)) {
        // cleanup all VM reservation entries
        SearchCriteria<VMReservationVO> sc = _reservationDao.createSearchCriteria();
        sc.addAnd("vmId", SearchCriteria.Op.EQ, vo.getId());
        _reservationDao.expunge(sc);
      }
      return true;
    }
}
