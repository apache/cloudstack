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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
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
import com.cloud.user.AccountManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.ReservationContext;
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
    @Inject protected AccountManager _accountMgr;

    @Inject(adapter=StoragePoolAllocator.class)
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    @Inject(adapter=HostAllocator.class)
    protected Adapters<HostAllocator> _hostAllocators;
    protected String _allocationAlgorithm = "random";


    @Override
    public DeployDestination plan(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoid)
    throws InsufficientServerCapacityException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        DataCenter dc = _dcDao.findById(vm.getDataCenterIdToDeployIn());

        //check if datacenter is in avoid set
        if(avoid.shouldAvoid(dc)){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DataCenter id = '"+ dc.getId() +"' provided is in avoid set, DeploymentPlanner cannot allocate the VM, returning.");
            }
            return null;
        }
        
        ServiceOffering offering = vmProfile.getServiceOffering();
        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;

        String opFactor = _configDao.getValue(Config.CPUOverprovisioningFactor.key());
        float cpuOverprovisioningFactor = NumbersUtil.parseFloat(opFactor, 1);


        if (s_logger.isDebugEnabled()) {
            s_logger.debug("DeploymentPlanner allocation algorithm: "+_allocationAlgorithm);

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
            DataCenterDeployment lastPlan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), hostIdSpecified, plan.getPoolId(), null, plan.getReservationContext());

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
            }else if(avoid.shouldAvoid(host)){
                s_logger.debug("The last host of this VM is in avoid set");
            }else{
                if (host.getStatus() == Status.Up && host.getResourceState() == ResourceState.Enabled) {
                    if(_capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, true, cpuOverprovisioningFactor, true)){
                        s_logger.debug("The last host of this VM is UP and has enough capacity");
                        s_logger.debug("Now checking for suitable pools under zone: "+host.getDataCenterId() +", pod: "+ host.getPodId()+", cluster: "+ host.getClusterId());
                        //search for storage under the zone, pod, cluster of the last host.
                        DataCenterDeployment lastPlan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), plan.getPoolId(), null);
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
                return checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc);
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
                DeployDestination dest = scanClustersForDestinationInZoneOrPod(podIdSpecified, false, vmProfile, plan, avoid);
                if(dest == null){
                    avoid.addPod(plan.getPodId());
                }
                return dest;
            } else {
                s_logger.debug("The specified Pod cannot be found, returning.");
                avoid.addPod(plan.getPodId());
                return null;
            }
        }else{
            s_logger.debug("Searching all possible resources under this Zone: "+ plan.getDataCenterId());
            
            boolean applyAllocationAtPods = Boolean.parseBoolean(_configDao.getValue(Config.ApplyAllocationAlgorithmToPods.key()));
            if(applyAllocationAtPods){
                //start scan at all pods under this zone.
                return scanPodsForDestination(vmProfile, plan, avoid);
            }else{
                //start scan at clusters under this zone.
                return scanClustersForDestinationInZoneOrPod(plan.getDataCenterId(), true, vmProfile, plan, avoid);
            }
        }

    }
    
    private DeployDestination scanPodsForDestination(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid){
        
        ServiceOffering offering = vmProfile.getServiceOffering();
        int requiredCpu = offering.getCpu() * offering.getSpeed();
        long requiredRam = offering.getRamSize() * 1024L * 1024L;
        String opFactor = _configDao.getValue(Config.CPUOverprovisioningFactor.key());
        float cpuOverprovisioningFactor = NumbersUtil.parseFloat(opFactor, 1);
        
        //list pods under this zone by cpu and ram capacity
        List<Long> prioritizedPodIds = new ArrayList<Long>();
        Pair<List<Long>, Map<Long, Double>> podCapacityInfo = listPodsByCapacity(plan.getDataCenterId(), requiredCpu, requiredRam, cpuOverprovisioningFactor); 
        List<Long> podsWithCapacity = podCapacityInfo.first();

        if(!podsWithCapacity.isEmpty()){
            if(avoid.getPodsToAvoid() != null){
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Removing from the podId list these pods from avoid set: "+ avoid.getPodsToAvoid());
                }
                podsWithCapacity.removeAll(avoid.getPodsToAvoid());
            }
            if(!isRootAdmin(plan.getReservationContext())){
                List<Long> disabledPods = listDisabledPods(plan.getDataCenterId());
                if(!disabledPods.isEmpty()){
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Removing from the podId list these pods that are disabled: "+ disabledPods);
                    }
                    podsWithCapacity.removeAll(disabledPods);
                }
           }
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No pods found having a host with enough capacity, returning.");
            }
            return null;
        }
        
        if(!podsWithCapacity.isEmpty()){
            
            prioritizedPodIds = reorderPods(podCapacityInfo, vmProfile, plan);

            //loop over pods
            for(Long podId : prioritizedPodIds){
                s_logger.debug("Checking resources under Pod: "+podId);
                DeployDestination dest = scanClustersForDestinationInZoneOrPod(podId, false, vmProfile, plan, avoid);
                if(dest != null){
                    return dest;
                }
                avoid.addPod(podId);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No Pods found for destination, returning.");
            }
            return null;
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No Pods found after removing disabled pods and pods in avoid list, returning.");
            }
            return null;
        }
    }
    
    private DeployDestination scanClustersForDestinationInZoneOrPod(long id, boolean isZone, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid){
        
        VirtualMachine vm = vmProfile.getVirtualMachine();
        ServiceOffering offering = vmProfile.getServiceOffering();
        DataCenter dc = _dcDao.findById(vm.getDataCenterIdToDeployIn());
        int requiredCpu = offering.getCpu() * offering.getSpeed();
        long requiredRam = offering.getRamSize() * 1024L * 1024L;
        String opFactor = _configDao.getValue(Config.CPUOverprovisioningFactor.key());
        float cpuOverprovisioningFactor = NumbersUtil.parseFloat(opFactor, 1);
        
        //list clusters under this zone by cpu and ram capacity
        Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo = listClustersByCapacity(id, requiredCpu, requiredRam, avoid, isZone, cpuOverprovisioningFactor);
        List<Long> prioritizedClusterIds = clusterCapacityInfo.first();
        if(!prioritizedClusterIds.isEmpty()){
            if(avoid.getClustersToAvoid() != null){
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Removing from the clusterId list these clusters from avoid set: "+ avoid.getClustersToAvoid());
                }
                prioritizedClusterIds.removeAll(avoid.getClustersToAvoid());
            }
            
            if(!isRootAdmin(plan.getReservationContext())){
                List<Long> disabledClusters = new ArrayList<Long>();
                if(isZone){
                    disabledClusters = listDisabledClusters(plan.getDataCenterId(), null);
                }else{
                    disabledClusters = listDisabledClusters(plan.getDataCenterId(), id);
                }
                if(!disabledClusters.isEmpty()){
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Removing from the clusterId list these clusters that are disabled/clusters under disabled pods: "+ disabledClusters);
                    }
                    prioritizedClusterIds.removeAll(disabledClusters);
                }
            }
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No clusters found having a host with enough capacity, returning.");
            }
            return null;
        }
        if(!prioritizedClusterIds.isEmpty()){
            List<Long> clusterList = reorderClusters(id, isZone, clusterCapacityInfo, vmProfile, plan);
            return checkClustersforDestination(clusterList, vmProfile, plan, avoid, dc);
        }else{
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No clusters found after removing disabled clusters and clusters in avoid list, returning.");
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
    protected List<Long> reorderClusters(long id, boolean isZone, Pair<List<Long>, Map<Long, Double>> clusterCapacityInfo, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan){
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
    protected List<Long> reorderPods(Pair<List<Long>, Map<Long, Double>> podCapacityInfo, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan){
        List<Long> podIdsByCapacity = podCapacityInfo.first();
        return podIdsByCapacity;
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
    
    private List<Long> listDisabledPods(long zoneId){
        List<Long> disabledPods = _podDao.listDisabledPods(zoneId);
        return disabledPods;
    }    
    
    private Map<Short,Float> getCapacityThresholdMap(){
    	// Lets build this real time so that the admin wont have to restart MS if he changes these values
    	Map<Short,Float> disableThresholdMap = new HashMap<Short, Float>();
    	
    	String cpuDisableThresholdString = _configDao.getValue(Config.CPUCapacityDisableThreshold.key());
        float cpuDisableThreshold = NumbersUtil.parseFloat(cpuDisableThresholdString, 0.85F);
        disableThresholdMap.put(Capacity.CAPACITY_TYPE_CPU, cpuDisableThreshold);
        
        String memoryDisableThresholdString = _configDao.getValue(Config.MemoryCapacityDisableThreshold.key());
        float memoryDisableThreshold = NumbersUtil.parseFloat(memoryDisableThresholdString, 0.85F);
        disableThresholdMap.put(Capacity.CAPACITY_TYPE_MEMORY, memoryDisableThreshold);
        
    	return disableThresholdMap;
    }

    private List<Short> getCapacitiesForCheckingThreshold(){
    	List<Short> capacityList = new ArrayList<Short>();    	
    	capacityList.add(Capacity.CAPACITY_TYPE_CPU);
    	capacityList.add(Capacity.CAPACITY_TYPE_MEMORY);    	
    	return capacityList;
    }
    
    private void removeClustersCrossingThreshold(List<Long> clusterList, ExcludeList avoid, VirtualMachineProfile<? extends VirtualMachine> vmProfile){
    	        
    	Map<Short,Float> capacityThresholdMap = getCapacityThresholdMap();
    	List<Short> capacityList = getCapacitiesForCheckingThreshold();
    	List<Long> clustersCrossingThreshold = new ArrayList<Long>();
    	
        ServiceOffering offering = vmProfile.getServiceOffering();
        int cpu_requested = offering.getCpu() * offering.getSpeed();
        long ram_requested = offering.getRamSize() * 1024L * 1024L;
    	
    	// Iterate over the cluster List and check for each cluster whether it breaks disable threshold for any of the capacity types
    	for (Long clusterId : clusterList){
    		for(short capacity : capacityList){
    			
    			List<SummedCapacity> summedCapacityList = _capacityDao.findCapacityBy(new Integer(capacity), null, null, clusterId);    			
    	    	if (summedCapacityList != null && summedCapacityList.size() != 0  && summedCapacityList.get(0).getTotalCapacity() != 0){
    	    		
    	    		double used = (double)(summedCapacityList.get(0).getUsedCapacity() + summedCapacityList.get(0).getReservedCapacity());
    	    		double total = summedCapacityList.get(0).getTotalCapacity();
    	    		
    	    		if (capacity == Capacity.CAPACITY_TYPE_CPU){
    	    			total = total * ApiDBUtils.getCpuOverprovisioningFactor();
    	    			used = used + cpu_requested;
    	    		}else{
    	    			used = used + ram_requested;
    	    		}
    	    		
    	    		double usedPercentage = used/total;
    	    		if ( usedPercentage > capacityThresholdMap.get(capacity)){    	    			
    	    			avoid.addCluster(clusterId);
    	    			clustersCrossingThreshold.add(clusterId);
						s_logger.debug("Cannot allocate cluster " + clusterId + " for vm creation since its allocated percentage: " +usedPercentage + 
								" will cross the disable capacity threshold: " + capacityThresholdMap.get(capacity) + " for capacity Type : " + capacity + ", skipping this cluster");    	    			
    	    			break;
    	    		}    	    				
    	    	}    	    	
        	}	    			
    	}
    	
    	clusterList.removeAll(clustersCrossingThreshold);	    	
    	
    }

    private DeployDestination checkClustersforDestination(List<Long> clusterList, VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoid, DataCenter dc){

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List to consider: " + clusterList);
        }

        removeClustersCrossingThreshold(clusterList, avoid, vmProfile);
        
        for(Long clusterId : clusterList){
            Cluster clusterVO = _clusterDao.findById(clusterId);

            if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
                s_logger.debug("Cluster: "+clusterId + " has HyperVisorType that does not match the VM, skipping this cluster");
                avoid.addCluster(clusterVO.getId());
                continue;
            }
            
            s_logger.debug("Checking resources in Cluster: "+clusterId + " under Pod: "+clusterVO.getPodId());
            //search for resources(hosts and storage) under this zone, pod, cluster.
            DataCenterDeployment potentialPlan = new DataCenterDeployment(plan.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), null, plan.getPoolId(), null, plan.getReservationContext());

            //find suitable hosts under this cluster, need as many hosts as we get.
            List<Host> suitableHosts = findSuitableHosts(vmProfile, potentialPlan, avoid, HostAllocator.RETURN_UPTO_ALL);
            //if found suitable hosts in this cluster, find suitable storage pools for each volume of the VM
            if(suitableHosts != null && !suitableHosts.isEmpty()){
                if (vmProfile.getHypervisorType() == HypervisorType.BareMetal) {
                    Pod pod = _podDao.findById(clusterVO.getPodId());
                    DeployDestination dest =  new DeployDestination(dc, pod, clusterVO, suitableHosts.get(0));
                    return dest;
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


    protected Pair<List<Long>, Map<Long, Double>> listClustersByCapacity(long id, int requiredCpu, long requiredRam, ExcludeList avoid, boolean isZone, float cpuOverprovisioningFactor){
        //look at the aggregate available cpu and ram per cluster
        //although an aggregate value may be false indicator that a cluster can host a vm, it will at the least eliminate those clusters which definitely cannot

        //we need clusters having enough cpu AND RAM to host this particular VM and order them by aggregate cluster capacity
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Listing clusters in order of aggregate capacity, that have (atleast one host with) enough CPU and RAM capacity under this "+(isZone ? "Zone: " : "Pod: " )+id);
        }
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = CapacityVO.CAPACITY_TYPE_CPU;
        if("RAM".equalsIgnoreCase(capacityTypeToOrder)){
            capacityType = CapacityVO.CAPACITY_TYPE_MEMORY;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("CPUOverprovisioningFactor considered: " + cpuOverprovisioningFactor);
        }
        List<Long> clusterIdswithEnoughCapacity = _capacityDao.listClustersInZoneOrPodByHostCapacities(id, requiredCpu, requiredRam, capacityType, isZone, cpuOverprovisioningFactor);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List having enough CPU and RAM capacity: " + clusterIdswithEnoughCapacity);
        }
        Pair<List<Long>, Map<Long, Double>> result = _capacityDao.orderClustersByAggregateCapacity(id, capacityType, isZone, cpuOverprovisioningFactor);
        List<Long> clusterIdsOrderedByAggregateCapacity = result.first();
        //only keep the clusters that have enough capacity to host this VM
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List in order of aggregate capacity: " + clusterIdsOrderedByAggregateCapacity);
        }
        clusterIdsOrderedByAggregateCapacity.retainAll(clusterIdswithEnoughCapacity);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("ClusterId List having enough CPU and RAM capacity & in order of aggregate capacity: " + clusterIdsOrderedByAggregateCapacity);
        }
        
        return result;

    }
    
    protected Pair<List<Long>, Map<Long, Double>> listPodsByCapacity(long zoneId, int requiredCpu, long requiredRam, float cpuOverprovisioningFactor){
        //look at the aggregate available cpu and ram per pod
        //although an aggregate value may be false indicator that a pod can host a vm, it will at the least eliminate those pods which definitely cannot

        //we need pods having enough cpu AND RAM to host this particular VM and order them by aggregate pod capacity
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Listing pods in order of aggregate capacity, that have (atleast one host with) enough CPU and RAM capacity under this Zone: "+zoneId);
        }
        String capacityTypeToOrder = _configDao.getValue(Config.HostCapacityTypeToOrderClusters.key());
        short capacityType = CapacityVO.CAPACITY_TYPE_CPU;
        if("RAM".equalsIgnoreCase(capacityTypeToOrder)){
            capacityType = CapacityVO.CAPACITY_TYPE_MEMORY;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("CPUOverprovisioningFactor considered: " + cpuOverprovisioningFactor);
        }
        List<Long> podIdswithEnoughCapacity = _capacityDao.listPodsByHostCapacities(zoneId, requiredCpu, requiredRam, capacityType, cpuOverprovisioningFactor);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("PodId List having enough CPU and RAM capacity: " + podIdswithEnoughCapacity);
        }
        Pair<List<Long>, Map<Long, Double>> result = _capacityDao.orderPodsByAggregateCapacity(zoneId, capacityType, cpuOverprovisioningFactor);
        List<Long> podIdsOrderedByAggregateCapacity = result.first();
        //only keep the clusters that have enough capacity to host this VM
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("PodId List in order of aggregate capacity: " + podIdsOrderedByAggregateCapacity);
        }
        podIdsOrderedByAggregateCapacity.retainAll(podIdswithEnoughCapacity);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("PodId List having enough CPU and RAM capacity & in order of aggregate capacity: " + podIdsOrderedByAggregateCapacity);
        }
        
        return result;

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
            if(!isRootAdmin(plan.getReservationContext())){
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
                final List<StoragePool> suitablePools = allocator.allocateToPool(diskProfile, vmProfile, plan, avoid, returnUpTo);
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


    private boolean isRootAdmin(ReservationContext reservationContext) {
        if(reservationContext != null){
            if(reservationContext.getAccount() != null){
                return _accountMgr.isRootAdmin(reservationContext.getAccount().getType());
            }else{
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean check(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan,
            DeployDestination dest, ExcludeList exclude) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canHandle(VirtualMachineProfile<? extends VirtualMachine> vm, DeploymentPlan plan, ExcludeList avoid) {
        if(vm.getHypervisorType() != HypervisorType.BareMetal){
            //check the allocation strategy
            if (_allocationAlgorithm != null && (_allocationAlgorithm.equals(AllocationAlgorithm.random.toString()) || _allocationAlgorithm.equals(AllocationAlgorithm.firstfit.toString()))) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _allocationAlgorithm = _configDao.getValue(Config.VmAllocationAlgorithm.key());
        return true;
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
