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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.gpu.GPU;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = DeploymentPlanner.class)
public class FirstFitPlanner extends PlannerBase implements DeploymentClusterPlanner, Configurable {
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected GuestOSDao _guestOSDao = null;
    @Inject
    protected GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected StoragePoolHostDao _poolHostDao;
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
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
    protected ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    protected HostGpuGroupsDao _hostGpuGroupsDao;
    @Inject
    protected HostTagsDao _hostTagsDao;

    protected String _allocationAlgorithm = "random";
    protected String _globalDeploymentPlanner = "FirstFitPlanner";
    protected String[] _implicitHostTags;

    @Override
    public List<Long> orderClusters(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        DataCenter dc = _dcDao.findById(vm.getDataCenterId());

        //check if datacenter is in avoid set
        if (avoid.shouldAvoid(dc)) {
            if (logger.isDebugEnabled()) {
                logger.debug("DataCenter id = '" + dc.getId() + "' provided is in avoid set, DeploymentPlanner cannot allocate the VM, returning.");
            }
            return null;
        }

        List<Long> clusterList = new ArrayList<Long>();
        if (plan.getClusterId() != null) {
            Long clusterIdSpecified = plan.getClusterId();
            logger.debug("Searching resources only under specified Cluster: " + clusterIdSpecified);
            ClusterVO cluster = _clusterDao.findById(plan.getClusterId());
            if (cluster != null) {
                if (avoid.shouldAvoid(cluster)) {
                    logger.debug("The specified cluster is in avoid set, returning.");
                } else {
                    clusterList.add(clusterIdSpecified);
                    removeClustersCrossingThreshold(clusterList, avoid, vmProfile, plan);
                }
            } else {
                logger.debug("The specified cluster cannot be found, returning.");
                avoid.addCluster(plan.getClusterId());
                return null;
            }
        } else if (plan.getPodId() != null) {
            //consider clusters under this pod only
            Long podIdSpecified = plan.getPodId();
            logger.debug("Searching resources only under specified Pod: " + podIdSpecified);

            HostPodVO pod = _podDao.findById(podIdSpecified);
            if (pod != null) {
                if (avoid.shouldAvoid(pod)) {
                    logger.debug("The specified pod is in avoid set, returning.");
                } else {
                    clusterList = scanClustersForDestinationInZoneOrPod(podIdSpecified, false, vmProfile, plan, avoid);
                    if (clusterList == null) {
                        avoid.addPod(plan.getPodId());
                    }
                }
            } else {
                logger.debug("The specified Pod cannot be found, returning.");
                avoid.addPod(plan.getPodId());
                return null;
            }
        } else {
            logger.debug("Searching all possible resources under this Zone: " + plan.getDataCenterId());

            boolean applyAllocationAtPods = Boolean.parseBoolean(_configDao.getValue(Config.ApplyAllocationAlgorithmToPods.key()));
            if (applyAllocationAtPods) {
                //start scan at all pods under this zone.
                clusterList = scanPodsForDestination(vmProfile, plan, avoid);
            } else {
                //start scan at clusters under this zone.
                clusterList = scanClustersForDestinationInZoneOrPod(plan.getDataCenterId(), true, vmProfile, plan, avoid);
            }
        }

        if (clusterList != null && !clusterList.isEmpty()) {
            ServiceOffering offering = vmProfile.getServiceOffering();
            // In case of non-GPU VMs, protect GPU enabled Hosts and prefer VM deployment on non-GPU Hosts.
            if ((_serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString()) == null) && !(_hostGpuGroupsDao.listHostIds().isEmpty())) {
                int requiredCpu = offering.getCpu() * offering.getSpeed();
                long requiredRam = offering.getRamSize() * 1024L * 1024L;
                reorderClustersBasedOnImplicitTags(clusterList, requiredCpu, requiredRam);
            }
        }
        return clusterList;
    }

    private void reorderClustersBasedOnImplicitTags(List<Long> clusterList, int requiredCpu, long requiredRam) {
            final HashMap<Long, Long> UniqueTagsInClusterMap = new HashMap<Long, Long>();
            Long uniqueTags;
            for (Long clusterId : clusterList) {
                uniqueTags = (long) 0;
                List<Long> hostList = _capacityDao.listHostsWithEnoughCapacity(requiredCpu, requiredRam, clusterId, Host.Type.Routing.toString());
                if (!hostList.isEmpty() && _implicitHostTags.length > 0) {
                    uniqueTags = new Long(_hostTagsDao.getDistinctImplicitHostTags(hostList, _implicitHostTags).size());
                }
                UniqueTagsInClusterMap.put(clusterId, uniqueTags);
            }
            Collections.sort(clusterList, new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    Long t1 = UniqueTagsInClusterMap.get(o1);
                    Long t2 = UniqueTagsInClusterMap.get(o2);
                    return t1.compareTo(t2);
                }
            });
    }

    private List<Long> scanPodsForDestination(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) {

        ServiceOffering offering = vmProfile.getServiceOffering();
        int requiredCpu = offering.getCpu() * offering.getSpeed();
        long requiredRam = offering.getRamSize() * 1024L * 1024L;
        //list pods under this zone by cpu and ram capacity
        List<Long> prioritizedPodIds = new ArrayList<Long>();
        Pair<List<Long>, Map<Long, Double>> podCapacityInfo = listPodsByCapacity(plan.getDataCenterId(), requiredCpu, requiredRam);
        List<Long> podsWithCapacity = podCapacityInfo.first();

        if (!podsWithCapacity.isEmpty()) {
            if (avoid.getPodsToAvoid() != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing from the podId list these pods from avoid set: " + avoid.getPodsToAvoid());
                }
                podsWithCapacity.removeAll(avoid.getPodsToAvoid());
            }
            if (!isRootAdmin(vmProfile)) {
                List<Long> disabledPods = listDisabledPods(plan.getDataCenterId());
                if (!disabledPods.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removing from the podId list these pods that are disabled: " + disabledPods);
                    }
                    podsWithCapacity.removeAll(disabledPods);
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No pods found having a host with enough capacity, returning.");
            }
            return null;
        }

        if (!podsWithCapacity.isEmpty()) {

            prioritizedPodIds = reorderPods(podCapacityInfo, vmProfile, plan);
            if (prioritizedPodIds == null || prioritizedPodIds.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Pods found for destination, returning.");
                }
                return null;
            }

            List<Long> clusterList = new ArrayList<Long>();
            //loop over pods
            for (Long podId : prioritizedPodIds) {
                logger.debug("Checking resources under Pod: " + podId);
                List<Long> clustersUnderPod = scanClustersForDestinationInZoneOrPod(podId, false, vmProfile, plan, avoid);
                if (clustersUnderPod != null) {
                    clusterList.addAll(clustersUnderPod);
                }
            }
            return clusterList;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No Pods found after removing disabled pods and pods in avoid list, returning.");
            }
            return null;
        }
    }

    private Map<Short, Float> getCapacityThresholdMap() {
        // Lets build this real time so that the admin wont have to restart MS
        // if he changes these values
        Map<Short, Float> disableThresholdMap = new HashMap<Short, Float>();

        String cpuDisableThresholdString = ClusterCPUCapacityDisableThreshold.value().toString();
        float cpuDisableThreshold = NumbersUtil.parseFloat(cpuDisableThresholdString, 0.85F);
        disableThresholdMap.put(Capacity.CAPACITY_TYPE_CPU, cpuDisableThreshold);

        String memoryDisableThresholdString = ClusterMemoryCapacityDisableThreshold.value().toString();
        float memoryDisableThreshold = NumbersUtil.parseFloat(memoryDisableThresholdString, 0.85F);
        disableThresholdMap.put(Capacity.CAPACITY_TYPE_MEMORY, memoryDisableThreshold);

        return disableThresholdMap;
    }

    private List<Short> getCapacitiesForCheckingThreshold() {
        List<Short> capacityList = new ArrayList<Short>();
        capacityList.add(Capacity.CAPACITY_TYPE_CPU);
        capacityList.add(Capacity.CAPACITY_TYPE_MEMORY);
        return capacityList;
    }

    /**
     * This method should remove the clusters crossing capacity threshold to avoid further vm allocation on it.
     * @param clusterListForVmAllocation
     * @param avoid
     * @param vmProfile
     * @param plan
     */
    protected void removeClustersCrossingThreshold(List<Long> clusterListForVmAllocation, ExcludeList avoid,
            VirtualMachineProfile vmProfile, DeploymentPlan plan) {

        List<Short> capacityList = getCapacitiesForCheckingThreshold();
        List<Long> clustersCrossingThreshold = new ArrayList<Long>();

        ServiceOffering offering = vmProfile.getServiceOffering();
        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;

        // For each capacity get the cluster list crossing the threshold and
        // remove it from the clusterList that will be used for vm allocation.
        for (short capacity : capacityList) {

            if (clusterListForVmAllocation == null || clusterListForVmAllocation.size() == 0) {
                return;
            }
            if (capacity == Capacity.CAPACITY_TYPE_CPU) {
                clustersCrossingThreshold =
                    _capacityDao.listClustersCrossingThreshold(capacity, plan.getDataCenterId(), ClusterCPUCapacityDisableThreshold.key(), cpu_requested);
            } else if (capacity == Capacity.CAPACITY_TYPE_MEMORY) {
                clustersCrossingThreshold =
                    _capacityDao.listClustersCrossingThreshold(capacity, plan.getDataCenterId(), ClusterMemoryCapacityDisableThreshold.key(), ram_requested);
            }

            if (clustersCrossingThreshold != null && clustersCrossingThreshold.size() != 0) {
                // addToAvoid Set
                avoid.addClusterList(clustersCrossingThreshold);
                // Remove clusters crossing disabled threshold
                clusterListForVmAllocation.removeAll(clustersCrossingThreshold);

                logger.debug("Cannot allocate cluster list " + clustersCrossingThreshold.toString() + " for vm creation since their allocated percentage" +
                        " crosses the disable capacity threshold defined at each cluster/ at global value for capacity Type : " + capacity + ", skipping these clusters");
            }

        }
    }

    private List<Long> scanClustersForDestinationInZoneOrPod(long id, boolean isZone, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) {

        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();
        DataCenter dc = _dcDao.findById(vm.getDataCenterId());
        int requiredCpu = offering.getCpu() * offering.getSpeed();
        long requiredRam = offering.getRamSize() * 1024L * 1024L;

        //list clusters under this zone by cpu and ram capacity
        Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo = listClustersByCapacity(id, requiredCpu, requiredRam, avoid, isZone);
        List<Long> prioritizedClusterIds = clusterCapacityInfo.first();
        if (!prioritizedClusterIds.isEmpty()) {
            if (avoid.getClustersToAvoid() != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing from the clusterId list these clusters from avoid set: " + avoid.getClustersToAvoid());
                }
                prioritizedClusterIds.removeAll(avoid.getClustersToAvoid());
            }

            if (!isRootAdmin(vmProfile)) {
                List<Long> disabledClusters = new ArrayList<Long>();
                if (isZone) {
                    disabledClusters = listDisabledClusters(plan.getDataCenterId(), null);
                } else {
                    disabledClusters = listDisabledClusters(plan.getDataCenterId(), id);
                }
                if (!disabledClusters.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removing from the clusterId list these clusters that are disabled/clusters under disabled pods: " + disabledClusters);
                    }
                    prioritizedClusterIds.removeAll(disabledClusters);
                }
            }

            removeClustersCrossingThreshold(prioritizedClusterIds, avoid, vmProfile, plan);

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No clusters found having a host with enough capacity, returning.");
            }
            return null;
        }
        if (!prioritizedClusterIds.isEmpty()) {
            List<Long> clusterList = reorderClusters(id, isZone, clusterCapacityInfo, vmProfile, plan);
            return clusterList; //return checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No clusters found after removing disabled clusters and clusters in avoid list, returning.");
            }
            return null;
        }
    }

    /**
     * This method should reorder the given list of Cluster Ids by applying any necessary heuristic
     * for this planner
     * For FirstFitPlanner there is no specific heuristic to be applied
     * other than the capacity based ordering which is done by default.
     * @return List<Long> ordered list of Cluster Ids
     */
    protected List<Long> reorderClusters(long id, boolean isZone, Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo, VirtualMachineProfile vmProfile,
        DeploymentPlan plan) {
        List<Long> reordersClusterIds = clusterCapacityInfo.first();
        return reordersClusterIds;
    }

    /**
     * This method should reorder the given list of Pod Ids by applying any necessary heuristic
     * for this planner
     * For FirstFitPlanner there is no specific heuristic to be applied
     * other than the capacity based ordering which is done by default.
     * @return List<Long> ordered list of Pod Ids
     */
    protected List<Long> reorderPods(Pair<List<Long>, Map<Long, Double>> podCapacityInfo, VirtualMachineProfile vmProfile, DeploymentPlan plan) {
        List<Long> podIdsByCapacity = podCapacityInfo.first();
        return podIdsByCapacity;
    }

    private List<Long> listDisabledClusters(long zoneId, Long podId) {
        List<Long> disabledClusters = _clusterDao.listDisabledClusters(zoneId, podId);
        if (podId == null) {
            //list all disabled clusters under this zone + clusters under any disabled pod of this zone
            List<Long> clustersWithDisabledPods = _clusterDao.listClustersWithDisabledPods(zoneId);
            disabledClusters.addAll(clustersWithDisabledPods);
        }
        return disabledClusters;
    }

    private List<Long> listDisabledPods(long zoneId) {
        List<Long> disabledPods = _podDao.listDisabledPods(zoneId);
        return disabledPods;
    }

    protected Pair<List<Long>, Map<Long, Double>> listClustersByCapacity(long id, int requiredCpu, long requiredRam, ExcludeList avoid, boolean isZone) {
        //look at the aggregate available cpu and ram per cluster
        //although an aggregate value may be false indicator that a cluster can host a vm, it will at the least eliminate those clusters which definitely cannot

        //we need clusters having enough cpu AND RAM to host this particular VM and order them by aggregate cluster capacity
        if (logger.isDebugEnabled()) {
            logger.debug("Listing clusters in order of aggregate capacity, that have (atleast one host with) enough CPU and RAM capacity under this " +
                (isZone ? "Zone: " : "Pod: ") + id);
        }
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = Capacity.CAPACITY_TYPE_CPU;
        if ("RAM".equalsIgnoreCase(capacityTypeToOrder)) {
            capacityType = Capacity.CAPACITY_TYPE_MEMORY;
        }

        List<Long> clusterIdswithEnoughCapacity = _capacityDao.listClustersInZoneOrPodByHostCapacities(id, requiredCpu, requiredRam, capacityType, isZone);
        if (logger.isTraceEnabled()) {
            logger.trace("ClusterId List having enough CPU and RAM capacity: " + clusterIdswithEnoughCapacity);
        }
        Pair<List<Long>, Map<Long, Double>> result = _capacityDao.orderClustersByAggregateCapacity(id, capacityType, isZone);
        List<Long> clusterIdsOrderedByAggregateCapacity = result.first();
        //only keep the clusters that have enough capacity to host this VM
        if (logger.isTraceEnabled()) {
            logger.trace("ClusterId List in order of aggregate capacity: " + clusterIdsOrderedByAggregateCapacity);
        }
        clusterIdsOrderedByAggregateCapacity.retainAll(clusterIdswithEnoughCapacity);

        if (logger.isTraceEnabled()) {
            logger.trace("ClusterId List having enough CPU and RAM capacity & in order of aggregate capacity: " + clusterIdsOrderedByAggregateCapacity);
        }

        return result;

    }

    protected Pair<List<Long>, Map<Long, Double>> listPodsByCapacity(long zoneId, int requiredCpu, long requiredRam) {
        //look at the aggregate available cpu and ram per pod
        //although an aggregate value may be false indicator that a pod can host a vm, it will at the least eliminate those pods which definitely cannot

        //we need pods having enough cpu AND RAM to host this particular VM and order them by aggregate pod capacity
        if (logger.isDebugEnabled()) {
            logger.debug("Listing pods in order of aggregate capacity, that have (atleast one host with) enough CPU and RAM capacity under this Zone: " + zoneId);
        }
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = Capacity.CAPACITY_TYPE_CPU;
        if ("RAM".equalsIgnoreCase(capacityTypeToOrder)) {
            capacityType = Capacity.CAPACITY_TYPE_MEMORY;
        }

        List<Long> podIdswithEnoughCapacity = _capacityDao.listPodsByHostCapacities(zoneId, requiredCpu, requiredRam, capacityType);
        if (logger.isTraceEnabled()) {
            logger.trace("PodId List having enough CPU and RAM capacity: " + podIdswithEnoughCapacity);
        }
        Pair<List<Long>, Map<Long, Double>> result = _capacityDao.orderPodsByAggregateCapacity(zoneId, capacityType);
        List<Long> podIdsOrderedByAggregateCapacity = result.first();
        //only keep the clusters that have enough capacity to host this VM
        if (logger.isTraceEnabled()) {
            logger.trace("PodId List in order of aggregate capacity: " + podIdsOrderedByAggregateCapacity);
        }
        podIdsOrderedByAggregateCapacity.retainAll(podIdswithEnoughCapacity);

        if (logger.isTraceEnabled()) {
            logger.trace("PodId List having enough CPU and RAM capacity & in order of aggregate capacity: " + podIdsOrderedByAggregateCapacity);
        }

        return result;

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

    @Override
    public boolean check(VirtualMachineProfile vm, DeploymentPlan plan, DeployDestination dest, ExcludeList exclude) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) {
        // check what the ServiceOffering says. If null, check the global config
        ServiceOffering offering = vm.getServiceOffering();
        if (vm.getHypervisorType() != HypervisorType.BareMetal) {
            if (offering != null && offering.getDeploymentPlanner() != null) {
                if (offering.getDeploymentPlanner().equals(getName())) {
                    return true;
                }
            } else {
                if (_globalDeploymentPlanner != null && _globalDeploymentPlanner.equals(_name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _allocationAlgorithm = _configDao.getValue(Config.VmAllocationAlgorithm.key());
        _globalDeploymentPlanner = _configDao.getValue(Config.VmDeploymentPlanner.key());
        String configValue;
        if ((configValue = _configDao.getValue(Config.ImplicitHostTags.key())) != null) {
            _implicitHostTags = configValue.trim().split("\\s*,\\s*");
        }
        return true;
    }

    @Override
    public DeployDestination plan(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PlannerResourceUsage getResourceUsage(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        return PlannerResourceUsage.Shared;
    }

    @Override
    public String getConfigComponentName() {
        return DeploymentClusterPlanner.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ClusterCPUCapacityDisableThreshold, ClusterMemoryCapacityDisableThreshold};
    }
}
