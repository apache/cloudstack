/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceState;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.allocator.StoragePoolAllocator;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=DeploymentPlanner.class)
public class FirstFitPlanner extends PlannerBase implements DeploymentPlanner {
    private static final Logger s_logger = Logger.getLogger(FirstFitPlanner.class);
    @Inject protected HostDao _hostDao;
    @Inject protected DataCenterDao _dcDao;
    @Inject protected HostPodDao _podDao;
    @Inject protected ClusterDao _clusterDao;
    @Inject protected HostDetailsDao _hostDetailsDao = null;
    @Inject protected GuestOSDao _guestOSDao = null;
    @Inject protected GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject protected DiskOfferingDao _diskOfferingDao;
    @Inject protected StoragePoolHostDao _poolHostDao;
    @Inject protected UserVmDao _vmDao;
    @Inject protected VMInstanceDao _vmInstanceDao;
    @Inject protected VolumeDao _volsDao;
    @Inject protected CapacityManager _capacityMgr;
    @Inject protected ConfigurationDao _configDao;
    @Inject protected StoragePoolDao _storagePoolDao;
    @Inject protected CapacityDao _capacityDao;

    @Inject(adapter=StoragePoolAllocator.class)
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    @Inject(adapter=HostAllocator.class)
    protected Adapters<HostAllocator> _hostAllocators;


    @Override
    public DeployDestination plan(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoid)
    throws InsufficientServerCapacityException {
        String _allocationAlgorithm = _configDao.getValue(Config.VmAllocationAlgorithm.key());
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();
        DataCenter dc = _dcDao.findById(vm.getDataCenterIdToDeployIn());
        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;

        String opFactor = _configDao.getValue(Config.CPUOverprovisioningFactor.key());
        float cpuOverprovisioningFactor = NumbersUtil.parseFloat(opFactor, 1);


        if (s_logger.isDebugEnabled()) {
            s_logger.debug("In FirstFitPlanner:: plan");

            s_logger.debug("Trying to allocate a host and storage pools from dc:" + plan.getDataCenterId() + ", pod:" + plan.getPodId() + ",cluster:" + plan.getClusterId() +
                    ", requested cpu: " + cpu_requested + ", requested ram: " + ram_requested);

            s_logger.debug("Is ROOT volume READY (pool already allocated)?: " + (plan.getPoolId()!=null ? "Yes": "No"));
        }

        if(plan.getHostId() != null){
            Long hostIdSpecified = plan.getHostId();
            if (s_logger.isDebugEnabled()){
                s_logger.debug("DeploymentPlan has host_id specified, making no checks on this host, looks like admin test: "+hostIdSpecified);
            }
            HostVO host = _hostDao.findById(hostIdSpecified);
            if (s_logger.isDebugEnabled()) {
                if(host == null){
                    s_logger.debug("The specified host cannot be found");
                }else{
                    s_logger.debug("Looking for suitable pools for this host under zone: "+host.getDataCenterId() +", pod: "+ host.getPodId()+", cluster: "+ host.getClusterId());
                }
            }

            //search for storage under the zone, pod, cluster of the host.
            DataCenterDeployment lastPlan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), hostIdSpecified, plan.getPoolId());

            Pair<Map<Volume, List<StoragePool>>, List<Volume>> result = findSuitablePoolsForVolumes(vmProfile, lastPlan, avoid, HostAllocator.RETURN_UPTO_ALL);
            Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
            List<Volume> readyAndReusedVolumes = result.second();

            //choose the potential pool for this VM for this host
            if(!suitableVolumeStoragePools.isEmpty()){
                List<Host> suitableHosts = new ArrayList<Host>();
                suitableHosts.add(host);

                Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(suitableHosts, suitableVolumeStoragePools);
                if(potentialResources != null){
                    Pod pod = _podDao.findById(host.getPodId());
                    Cluster cluster = _clusterDao.findById(host.getClusterId());
                    Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                    // remove the reused vol<->pool from destination, since we don't have to prepare this volume.
                    for(Volume vol : readyAndReusedVolumes){
                        storageVolMap.remove(vol);
                    }
                    DeployDestination dest =  new DeployDestination(dc, pod, cluster, host, storageVolMap);
                    s_logger.debug("Returning Deployment Destination: "+ dest);
                    return dest;
                }
            }
            s_logger.debug("Cannnot deploy to specified host, returning.");
            return null;
        }

        if (vm.getLastHostId() != null) {
            s_logger.debug("This VM has last host_id specified, trying to choose the same host: " +vm.getLastHostId());

            HostVO host = _hostDao.findById(vm.getLastHostId());
            if(host == null){
                s_logger.debug("The last host of this VM cannot be found");
            }else{
                if (host.getStatus() == Status.Up && host.getResourceState() == ResourceState.Enabled) {
                    if(_capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, true, cpuOverprovisioningFactor, true)){
                        s_logger.debug("The last host of this VM is UP and has enough capacity");
                        s_logger.debug("Now checking for suitable pools under zone: "+host.getDataCenterId() +", pod: "+ host.getPodId()+", cluster: "+ host.getClusterId());
                        //search for storage under the zone, pod, cluster of the last host.
                        DataCenterDeployment lastPlan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), plan.getPoolId());
                        Pair<Map<Volume, List<StoragePool>>, List<Volume>> result = findSuitablePoolsForVolumes(vmProfile, lastPlan, avoid, HostAllocator.RETURN_UPTO_ALL);
                        Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
                        List<Volume> readyAndReusedVolumes = result.second();
                        //choose the potential pool for this VM for this host
                        if(!suitableVolumeStoragePools.isEmpty()){
                            List<Host> suitableHosts = new ArrayList<Host>();
                            suitableHosts.add(host);

                            Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(suitableHosts, suitableVolumeStoragePools);
                            if(potentialResources != null){
                                Pod pod = _podDao.findById(host.getPodId());
                                Cluster cluster = _clusterDao.findById(host.getClusterId());
                                Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                                // remove the reused vol<->pool from destination, since we don't have to prepare this volume.
                                for(Volume vol : readyAndReusedVolumes){
                                    storageVolMap.remove(vol);
                                }
                                DeployDestination dest =  new DeployDestination(dc, pod, cluster, host, storageVolMap);
                                s_logger.debug("Returning Deployment Destination: "+ dest);
                                return dest;
                            }
                        }
                    }else{
                        s_logger.debug("The last host of this VM does not have enough capacity");
                    }
                }else{
                    s_logger.debug("The last host of this VM is not UP or is not enabled, host status is: "+host.getStatus().name() + ", host resource state is: "+host.getResourceState());
                }
            }

            s_logger.debug("Cannot choose the last host to deploy this VM ");
        }

        List<Long> clusterList = new ArrayList<Long>();
        if (plan.getClusterId() != null) {
            Long clusterIdSpecified = plan.getClusterId();
            s_logger.debug("Searching resources only under specified Cluster: "+ clusterIdSpecified);
            ClusterVO cluster = _clusterDao.findById(plan.getClusterId());
            if (cluster != null ){
                clusterList.add(clusterIdSpecified);
                return checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc, _allocationAlgorithm);
            }else{
                s_logger.debug("The specified cluster cannot be found, returning.");
                avoid.addCluster(plan.getClusterId());
                return null;
            }
        }else if (plan.getPodId() != null) {
            //consider clusters under this pod only
            Long podIdSpecified = plan.getPodId();
            s_logger.debug("Searching resources only under specified Pod: "+ podIdSpecified);

            HostPodVO pod = _podDao.findById(podIdSpecified);
            if (pod != null) {
                //list clusters under this pod by cpu and ram capacity
                clusterList = listClustersByCapacity(podIdSpecified, cpu_requested, ram_requested, avoid, false, cpuOverprovisioningFactor);
                if(!clusterList.isEmpty()){
                    if(avoid.getClustersToAvoid() != null){
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Removing from the clusterId list these clusters from avoid set: "+ avoid.getClustersToAvoid());
                        }
                        clusterList.removeAll(avoid.getClustersToAvoid());
                    }

                    List<Long> disabledClusters = listDisabledClusters(plan.getDataCenterId(), podIdSpecified);
                    if(!disabledClusters.isEmpty()){
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Removing from the clusterId list these clusters that are disabled: "+ disabledClusters);
                        }
                        clusterList.removeAll(disabledClusters);
                    }

                    DeployDestination dest = checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc, _allocationAlgorithm);
                    if(dest == null){
                        avoid.addPod(plan.getPodId());
                    }
                    return dest;
                }else{
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("No clusters found under this pod, having a host with enough capacity, returning.");
                    }
                    avoid.addPod(plan.getPodId());
                    return null;
                }
            } else {
                s_logger.debug("The specified Pod cannot be found, returning.");
                avoid.addPod(plan.getPodId());
                return null;
            }
        }else{
            //consider all clusters under this zone.
            s_logger.debug("Searching all possible resources under this Zone: "+ plan.getDataCenterId());
            //list clusters under this zone by cpu and ram capacity
            List<Long> prioritizedClusterIds = listClustersByCapacity(plan.getDataCenterId(), cpu_requested, ram_requested, avoid, true, cpuOverprovisioningFactor);
            if(!prioritizedClusterIds.isEmpty()){
                if(avoid.getClustersToAvoid() != null){
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Removing from the clusterId list these clusters from avoid set: "+ avoid.getClustersToAvoid());
                    }
                    prioritizedClusterIds.removeAll(avoid.getClustersToAvoid());
                }
                List<Long> disabledClusters = listDisabledClusters(plan.getDataCenterId(), null);
                if(!disabledClusters.isEmpty()){
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Removing from the clusterId list these clusters that are disabled/clusters under disabled pods: "+ disabledClusters);
                    }
                    prioritizedClusterIds.removeAll(disabledClusters);
                }
            }else{
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No clusters found having a host with enough capacity, returning.");
                }
                return null;
            }
            if(!prioritizedClusterIds.isEmpty()){
                boolean applyUserConcentrationPodHeuristic = Boolean.parseBoolean(_configDao.getValue(Config.UseUserConcentratedPodAllocation.key()));
                if(applyUserConcentrationPodHeuristic && vmProfile.getOwner() != null){
                    //user has VMs in certain pods. - prioritize those pods first
                    //UserConcentratedPod strategy
                    long accountId = vmProfile.getOwner().getAccountId();
                    List<Long> podIds = listPodsByUserConcentration(plan.getDataCenterId(), accountId);
                    if(!podIds.isEmpty()){
                        if(avoid.getPodsToAvoid() != null){
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Removing from the pod list these pods from avoid set: "+ avoid.getPodsToAvoid());
                            }
                            podIds.removeAll(avoid.getPodsToAvoid());
                        }
                        clusterList = reorderClustersByPods(prioritizedClusterIds, podIds);
                    }else{
                        clusterList = prioritizedClusterIds;
                    }
                }else{
                    clusterList = prioritizedClusterIds;
                }
                return checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc, _allocationAlgorithm);
            }else{
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No clusters found after removing disabled clusters and clusters in avoid list, returning.");
                }
                return null;
            }
        }

    }

    private List<Long> listDisabledClusters(long zoneId, Long podId){
        List<Long> disabledClusters = _clusterDao.listDisabledClusters(zoneId, podId);
        if(podId == null){
            //list all disabled clusters under this zone + clusters under any disabled pod of this zone
            List<Long> clustersWithDisabledPods = _clusterDao.listClustersWithDisabledPods(zoneId);
            disabledClusters.addAll(clustersWithDisabledPods);
        }
        return disabledClusters;
    }



    private DeployDestination checkClustersforDestination(List<Long> clusterList, VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoid, DataCenter dc, String _allocationAlgorithm){

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List to consider: " + clusterList);
        }

        for(Long clusterId : clusterList){
            Cluster clusterVO = _clusterDao.findById(clusterId);

            if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
                s_logger.debug("Cluster: "+clusterId + " has HyperVisorType that does not match the VM, skipping this cluster");
                avoid.addCluster(clusterVO.getId());
                continue;
            }

            s_logger.debug("Checking resources in Cluster: "+clusterId + " under Pod: "+clusterVO.getPodId());
            //search for resources(hosts and storage) under this zone, pod, cluster.
            DataCenterDeployment potentialPlan = new DataCenterDeployment(plan.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), null, plan.getPoolId());

            //find suitable hosts under this cluster, need as many hosts as we get.
            List<Host> suitableHosts = findSuitableHosts(vmProfile, potentialPlan, avoid, HostAllocator.RETURN_UPTO_ALL);
            //if found suitable hosts in this cluster, find suitable storage pools for each volume of the VM
            if(suitableHosts != null && !suitableHosts.isEmpty()){
                if (vmProfile.getHypervisorType() == HypervisorType.BareMetal) {
                    Pod pod = _podDao.findById(clusterVO.getPodId());
                    DeployDestination dest =  new DeployDestination(dc, pod, clusterVO, suitableHosts.get(0));
                    return dest;
                }

                if (_allocationAlgorithm != null && _allocationAlgorithm.equalsIgnoreCase("random")) {
                    Collections.shuffle(suitableHosts);
                }
                Pair<Map<Volume, List<StoragePool>>, List<Volume>> result = findSuitablePoolsForVolumes(vmProfile, potentialPlan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
                Map<Volume, List<StoragePool>> suitableVolumeStoragePools = result.first();
                List<Volume> readyAndReusedVolumes = result.second();

                //choose the potential host and pool for the VM
                if(!suitableVolumeStoragePools.isEmpty()){
                    Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(suitableHosts, suitableVolumeStoragePools);

                    if(potentialResources != null){
                        Pod pod = _podDao.findById(clusterVO.getPodId());
                        Host host = _hostDao.findById(potentialResources.first().getId());
                        Map<Volume, StoragePool> storageVolMap = potentialResources.second();
                        // remove the reused vol<->pool from destination, since we don't have to prepare this volume.
                        for(Volume vol : readyAndReusedVolumes){
                            storageVolMap.remove(vol);
                        }
                        DeployDestination dest =  new DeployDestination(dc, pod, clusterVO, host, storageVolMap );
                        s_logger.debug("Returning Deployment Destination: "+ dest);
                        return dest;
                    }
                }else{
                    s_logger.debug("No suitable storagePools found under this Cluster: "+clusterId);
                }
            }else{
                s_logger.debug("No suitable hosts found under this Cluster: "+clusterId);
            }
            avoid.addCluster(clusterVO.getId());
        }
        s_logger.debug("Could not find suitable Deployment Destination for this VM under any clusters, returning. ");
        return null;
    }

    private List<Long> reorderClustersByPods(List<Long> clusterIds, List<Long> podIds) {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Reordering cluster list as per pods ordered by user concentration");
        }

        Map<Long, List<Long>> podClusterMap = _clusterDao.getPodClusterIdMap(clusterIds);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Pod To cluster Map is: "+podClusterMap );
        }

        List<Long> reorderedClusters = new ArrayList<Long>();
        for (Long pod : podIds){
            if(podClusterMap.containsKey(pod)){
                List<Long> clustersOfThisPod = podClusterMap.get(pod);
                if(clustersOfThisPod != null){
                    for(Long clusterId : clusterIds){
                        if(clustersOfThisPod.contains(clusterId)){
                            reorderedClusters.add(clusterId);
                        }
                    }
                    clusterIds.removeAll(clustersOfThisPod);
                }
            }
        }
        reorderedClusters.addAll(clusterIds);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Reordered cluster list: " + reorderedClusters);
        }
        return reorderedClusters;
    }

    protected List<Long> listPodsByUserConcentration(long zoneId, long accountId){

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Applying UserConcentratedPod heuristic for account: "+ accountId);
        }

        List<Long> prioritizedPods = _vmDao.listPodIdsHavingVmsforAccount(zoneId, accountId);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pods to be considered, after applying UserConcentratedPod heuristic: "+ prioritizedPods);
        }

        return prioritizedPods;
    }

    protected List<Long> listClustersByCapacity(long id, int requiredCpu, long requiredRam, ExcludeList avoid, boolean isZone, float cpuOverprovisioningFactor){
        //look at the aggregate available cpu and ram per cluster
        //although an aggregate value may be false indicator that a cluster can host a vm, it will at the least eliminate those clusters which definitely cannot

        //we need clusters having enough cpu AND RAM
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Listing clusters that have enough aggregate CPU and RAM capacity under this "+(isZone ? "Zone: " : "Pod: " )+id);
        }
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = CapacityVO.CAPACITY_TYPE_CPU;
        if("RAM".equalsIgnoreCase(capacityTypeToOrder)){
            capacityType = CapacityVO.CAPACITY_TYPE_MEMORY;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("CPUOverprovisioningFactor considered: " + cpuOverprovisioningFactor);
        }
        List<Long> clusterIdswithEnoughCapacity = _capacityDao.orderClustersInZoneOrPodByHostCapacities(id, requiredCpu, requiredRam, capacityType, isZone, cpuOverprovisioningFactor);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List having enough aggregate capacity: " + clusterIdswithEnoughCapacity);
        }
        return clusterIdswithEnoughCapacity;

    }

    protected Pair<Host, Map<Volume, StoragePool>> findPotentialDeploymentResources(List<Host> suitableHosts, Map<Volume, List<StoragePool>> suitableVolumeStoragePools){
        s_logger.debug("Trying to find a potenial host and associated storage pools from the suitable host/pool lists for this VM");

        boolean hostCanAccessPool = false;
        Map<Volume, StoragePool> storage = new HashMap<Volume, StoragePool>();
        for(Host potentialHost : suitableHosts){
            for(Volume vol : suitableVolumeStoragePools.keySet()){
                s_logger.debug("Checking if host: "+potentialHost.getId() +" can access any suitable storage pool for volume: "+ vol.getVolumeType());
                List<StoragePool> volumePoolList = suitableVolumeStoragePools.get(vol);
                hostCanAccessPool = false;
                for(StoragePool potentialSPool : volumePoolList){
                    if(hostCanAccessSPool(potentialHost, potentialSPool)){
                        storage.put(vol, potentialSPool);
                        hostCanAccessPool = true;
                        break;
                    }
                }
                if(!hostCanAccessPool){
                    break;
                }
            }
            if(hostCanAccessPool){
                s_logger.debug("Found a potential host " + "id: "+potentialHost.getId() + " name: " +potentialHost.getName()+ " and associated storage pools for this VM");
                return new Pair<Host, Map<Volume, StoragePool>>(potentialHost, storage);
            }
        }
        s_logger.debug("Could not find a potential host that has associated storage pools from the suitable host/pool lists for this VM");
        return null;
    }

    protected boolean hostCanAccessSPool(Host host, StoragePool pool){
        boolean hostCanAccessSPool = false;

        StoragePoolHostVO hostPoolLinkage = _poolHostDao.findByPoolHost(pool.getId(), host.getId());
        if(hostPoolLinkage != null){
            hostCanAccessSPool = true;
        }

        s_logger.debug("Host: "+ host.getId() + (hostCanAccessSPool ?" can" : " cannot") + " access pool: "+ pool.getId());
        return hostCanAccessSPool;
    }

    protected List<Host> findSuitableHosts(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo){
        List<Host> suitableHosts = new ArrayList<Host>();
        Enumeration<HostAllocator> enHost = _hostAllocators.enumeration();
        s_logger.debug("Calling HostAllocators to find suitable hosts");

        while (enHost.hasMoreElements()) {
            final HostAllocator allocator = enHost.nextElement();
            suitableHosts = allocator.allocateTo(vmProfile, plan, Host.Type.Routing, avoid, returnUpTo);
            if (suitableHosts != null && !suitableHosts.isEmpty()) {
                break;
            }
        }

        if(suitableHosts.isEmpty()){
            s_logger.debug("No suitable hosts found");
        }
        return suitableHosts;
    }

    protected Pair<Map<Volume, List<StoragePool>>, List<Volume>> findSuitablePoolsForVolumes(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo){
        List<VolumeVO> volumesTobeCreated = _volsDao.findUsableVolumesForInstance(vmProfile.getId());
        Map<Volume, List<StoragePool>> suitableVolumeStoragePools = new HashMap<Volume, List<StoragePool>>();
        List<Volume> readyAndReusedVolumes = new ArrayList<Volume>();

        //for each volume find list of suitable storage pools by calling the allocators
        for (VolumeVO toBeCreated : volumesTobeCreated) {
            s_logger.debug("Checking suitable pools for volume (Id, Type): ("+toBeCreated.getId() +"," +toBeCreated.getVolumeType().name() + ")");

            //If the plan specifies a poolId, it means that this VM's ROOT volume is ready and the pool should be reused.
            //In this case, also check if rest of the volumes are ready and can be reused.
            if(plan.getPoolId() != null){
                if (toBeCreated.getState() == Volume.State.Ready && toBeCreated.getPoolId() != null) {
                    s_logger.debug("Volume is in READY state and has pool already allocated, checking if pool can be reused, poolId: "+toBeCreated.getPoolId());
                    List<StoragePool> suitablePools = new ArrayList<StoragePool>();
                    StoragePoolVO pool = _storagePoolDao.findById(toBeCreated.getPoolId());
                    if(!pool.isInMaintenance()){
                        if(!avoid.shouldAvoid(pool)){
                            long exstPoolDcId = pool.getDataCenterId();

                            long exstPoolPodId = pool.getPodId() != null ? pool.getPodId() : -1;
                            long exstPoolClusterId = pool.getClusterId() != null ? pool.getClusterId() : -1;
                            if(plan.getDataCenterId() == exstPoolDcId && plan.getPodId() == exstPoolPodId && plan.getClusterId() == exstPoolClusterId){
                                s_logger.debug("Planner need not allocate a pool for this volume since its READY");
                                suitablePools.add(pool);
                                suitableVolumeStoragePools.put(toBeCreated, suitablePools);
                                readyAndReusedVolumes.add(toBeCreated);
                                continue;
                            }else{
                                s_logger.debug("Pool of the volume does not fit the specified plan, need to reallocate a pool for this volume");
                            }
                        }else{
                            s_logger.debug("Pool of the volume is in avoid set, need to reallocate a pool for this volume");
                        }
                    }else{
                        s_logger.debug("Pool of the volume is in maintenance, need to reallocate a pool for this volume");
                    }
                }
            }

            if(s_logger.isDebugEnabled()){
                s_logger.debug("We need to allocate new storagepool for this volume");
            }
            if(!isEnabledForAllocation(plan.getDataCenterId(), plan.getPodId(), plan.getClusterId())){
                if(s_logger.isDebugEnabled()){
                    s_logger.debug("Cannot allocate new storagepool for this volume in this cluster, allocation state is disabled");                    
                    s_logger.debug("Cannot deploy to this specified plan, allocation state is disabled, returning.");
                }
                //Cannot find suitable storage pools under this cluster for this volume since allocation_state is disabled. 
                //- remove any suitable pools found for other volumes.
                //All volumes should get suitable pools under this cluster; else we cant use this cluster.
                suitableVolumeStoragePools.clear();                
                break;
            }

            s_logger.debug("Calling StoragePoolAllocators to find suitable pools");

            DiskOfferingVO diskOffering = _diskOfferingDao.findById(toBeCreated.getDiskOfferingId());
            DiskProfile diskProfile = new DiskProfile(toBeCreated, diskOffering, vmProfile.getHypervisorType());

            boolean useLocalStorage = false;
            if (vmProfile.getType() != VirtualMachine.Type.User) {
                String ssvmUseLocalStorage = _configDao.getValue(Config.SystemVMUseLocalStorage.key());
                if (ssvmUseLocalStorage.equalsIgnoreCase("true")) {
                    useLocalStorage = true;
                }
            } else {
                useLocalStorage = diskOffering.getUseLocalStorage();

                // TODO: this is a hacking fix for the problem of deploy ISO-based VM on local storage
                // when deploying VM based on ISO, we have a service offering and an additional disk offering, use-local storage flag is actually
                // saved in service offering, overrde the flag from service offering when it is a ROOT disk
                if(!useLocalStorage && vmProfile.getServiceOffering().getUseLocalStorage()) {
                	if(toBeCreated.getVolumeType() == Volume.Type.ROOT)
                		useLocalStorage = true;
                }
            }
            diskProfile.setUseLocalStorage(useLocalStorage);


            boolean foundPotentialPools = false;

            Enumeration<StoragePoolAllocator> enPool = _storagePoolAllocators.enumeration();
            while (enPool.hasMoreElements()) {
                final StoragePoolAllocator allocator = enPool.nextElement();
                final List<StoragePool> suitablePools = allocator.allocateToPool(diskProfile, vmProfile.getTemplate(), plan, avoid, returnUpTo);
                if (suitablePools != null && !suitablePools.isEmpty()) {
                    suitableVolumeStoragePools.put(toBeCreated, suitablePools);
                    foundPotentialPools = true;
                    break;
                }
            }

            if(!foundPotentialPools){
                s_logger.debug("No suitable pools found for volume: "+toBeCreated +" under cluster: "+plan.getClusterId());
                //No suitable storage pools found under this cluster for this volume. - remove any suitable pools found for other volumes.
                //All volumes should get suitable pools under this cluster; else we cant use this cluster.
                suitableVolumeStoragePools.clear();
                break;
            }
        }

        if(suitableVolumeStoragePools.isEmpty()){
            s_logger.debug("No suitable pools found");
        }

        return new Pair<Map<Volume, List<StoragePool>>, List<Volume>>(suitableVolumeStoragePools, readyAndReusedVolumes);
    }


    @Override
    public boolean check(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan,
            DeployDestination dest, ExcludeList exclude) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid) {
        return vm.getHypervisorType() != HypervisorType.BareMetal;
    }

    private boolean isEnabledForAllocation(long zoneId, Long podId, Long clusterId){
        // Check if the zone exists in the system
        DataCenterVO zone = _dcDao.findById(zoneId);
        if(zone != null && Grouping.AllocationState.Disabled == zone.getAllocationState()){
            s_logger.info("Zone is currently disabled, cannot allocate to this zone: "+ zoneId);
            return false;
        }

        Pod pod = _podDao.findById(podId);
        if(pod != null && Grouping.AllocationState.Disabled == pod.getAllocationState()){
            s_logger.info("Pod is currently disabled, cannot allocate to this pod: "+ podId);
            return false;
        }

        Cluster cluster = _clusterDao.findById(clusterId);
        if(cluster != null && Grouping.AllocationState.Disabled == cluster.getAllocationState()){
            s_logger.info("Cluster is currently disabled, cannot allocate to this cluster: "+ clusterId);
            return false;
        }

        return true;
    }
}
