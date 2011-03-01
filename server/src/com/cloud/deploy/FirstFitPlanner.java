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
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
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
	@Inject private HostDao _hostDao;
	@Inject private DataCenterDao _dcDao;
	@Inject private HostPodDao _podDao;
	@Inject private ClusterDao _clusterDao;
	@Inject DetailsDao _hostDetailsDao = null;
	@Inject GuestOSDao _guestOSDao = null; 
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject private DiskOfferingDao _diskOfferingDao;
    @Inject private StoragePoolHostDao _poolHostDao;
    @Inject private UserVmDao _vmDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject protected VolumeDao _volsDao;
    @Inject CapacityManager _capacityMgr;
    @Inject ConfigurationDao _configDao;
    @Inject protected StoragePoolDao _storagePoolDao;
    @Inject CapacityDao _capacityDao;
    
    @Inject(adapter=StoragePoolAllocator.class)
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    @Inject(adapter=HostAllocator.class)
    protected Adapters<HostAllocator> _hostAllocators;
 	
    private static int RETURN_UPTO_ALL = -1;
	
	@Override
	public DeployDestination plan(VirtualMachineProfile vmProfile,
			DeploymentPlan plan, ExcludeList avoid)
			throws InsufficientServerCapacityException {
	    String _allocationAlgorithm = _configDao.getValue(Config.VmAllocationAlgorithm.key());
		VirtualMachine vm = vmProfile.getVirtualMachine();
		ServiceOffering offering = vmProfile.getServiceOffering();
		DataCenter dc = _dcDao.findById(vm.getDataCenterId());
		int cpu_requested = offering.getCpu() * offering.getSpeed();
		long ram_requested = offering.getRamSize() * 1024L * 1024L;
		
		s_logger.debug("In FirstFitPlanner:: plan");
		
		s_logger.debug("Trying to allocate a host and storage pools from dc:" + plan.getDataCenterId() + ", pod:" + plan.getPodId() + ",cluster:" + plan.getClusterId() +
				", requested cpu: " + cpu_requested + ", requested ram: " + ram_requested);
		
		s_logger.debug("Is ROOT volume READY (pool already allocated)?: " + (plan.getPoolId()!=null ? "Yes": "No"));
		
		
		if (vm.getLastHostId() != null) {
			s_logger.debug("This VM has last host_id specified, trying to choose the same host: " +vm.getLastHostId());			

			HostVO host = _hostDao.findById(vm.getLastHostId());
			if(host == null){
				s_logger.debug("The last host of this VM cannot be found");
			}else{
				if (host.getStatus() == Status.Up) {
					if(_capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, true)){
						s_logger.debug("The last host of this VM is UP and has enough capacity"); 
						s_logger.debug("Now checking for suitable pools under zone: "+vm.getDataCenterId() +", pod: "+ vm.getPodId()+", cluster: "+ host.getClusterId());
						//search for storage under the zone, pod, cluster of the last host.
						DataCenterDeployment lastPlan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodId(), host.getClusterId(), null);				
						Map<Volume, List<StoragePool>> suitableVolumeStoragePools = findSuitablePoolsForVolumes(vmProfile, lastPlan, avoid, RETURN_UPTO_ALL);
						//choose the potential pool for this VM for this host
						if(!suitableVolumeStoragePools.isEmpty()){
							List<Host> suitableHosts = new ArrayList<Host>();
							suitableHosts.add(host);
							
							Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(suitableHosts, suitableVolumeStoragePools);
							if(potentialResources != null){
								Pod pod = _podDao.findById(vm.getPodId());
								Cluster cluster = _clusterDao.findById(host.getClusterId());
								DeployDestination dest =  new DeployDestination(dc, pod, cluster, host, potentialResources.second());
								s_logger.debug("Returning Deployment Destination: "+ dest);
								return dest;
							}
						}
					}else{
						s_logger.debug("The last host of this VM does not have enough capacity");
					}
				}else{
					s_logger.debug("The last host of this VM is not UP, host status is: "+host.getStatus().name());
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
				avoid.addPod(plan.getClusterId());
				return null;
			}
		}else if (plan.getPodId() != null) {
			//consider clusters under this pod only
			Long podIdSpecified = plan.getPodId();
			s_logger.debug("Searching resources only under specified Pod: "+ podIdSpecified);
			
			HostPodVO pod = _podDao.findById(podIdSpecified);
			if (pod != null) {
				//list clusters under this pod by cpu and ram capacity 
				clusterList = listClustersByCapacity(podIdSpecified, cpu_requested, ram_requested, avoid, false);
				if(!clusterList.isEmpty()){
			    	if(avoid.getClustersToAvoid() != null){
				    	if (s_logger.isDebugEnabled()) {
				    		s_logger.debug("Removing from the clusterId list these clusters from avoid set: "+ avoid.getClustersToAvoid());
						}
			    		clusterList.removeAll(avoid.getClustersToAvoid());
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
			List<Long> prioritizedClusterIds = listClustersByCapacity(plan.getDataCenterId(), cpu_requested, ram_requested, avoid, true);
			if(!prioritizedClusterIds.isEmpty()){
				if(avoid.getClustersToAvoid() != null){
			    	if (s_logger.isDebugEnabled()) {
			    		s_logger.debug("Removing from the clusterId list these clusters from avoid set: "+ avoid.getClustersToAvoid());
					}
					prioritizedClusterIds.removeAll(avoid.getClustersToAvoid());
				}			
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
		    		s_logger.debug("No clusters found having a host with enough capacity, returning.");
				}
	    		return null;
			}
		}

	}
	
	
	private DeployDestination checkClustersforDestination(List<Long> clusterList, VirtualMachineProfile vmProfile,
			DeploymentPlan plan, ExcludeList avoid, DataCenter dc, String _allocationAlgorithm){
		
		for(Long clusterId : clusterList){
			Cluster clusterVO = _clusterDao.findById(clusterId);

			if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
				s_logger.debug("Cluster: "+clusterId + " has HyperVisorType that does not match the VM, skipping this cluster");
				avoid.addCluster(clusterVO.getId());
				continue;
			}
			s_logger.debug("Checking resources in Cluster: "+clusterId + " under Pod: "+clusterVO.getPodId());
			//search for resources(hosts and storage) under this zone, pod, cluster.
			DataCenterDeployment potentialPlan = new DataCenterDeployment(plan.getDataCenterId(), clusterVO.getPodId(), clusterVO.getId(), plan.getPoolId());

			//find suitable hosts under this cluster, need as many hosts as we get.
			List<Host> suitableHosts = findSuitableHosts(vmProfile, potentialPlan, avoid, RETURN_UPTO_ALL);
			//if found suitable hosts in this cluster, find suitable storage pools for each volume of the VM
			if(suitableHosts != null && !suitableHosts.isEmpty()){
				if (_allocationAlgorithm != null && _allocationAlgorithm.equalsIgnoreCase("random")) {
				    Collections.shuffle(suitableHosts);
				}
				Map<Volume, List<StoragePool>> suitableVolumeStoragePools = findSuitablePoolsForVolumes(vmProfile, potentialPlan, avoid, RETURN_UPTO_ALL);
            	
				//choose the potential host and pool for the VM
				if(!suitableVolumeStoragePools.isEmpty()){
					Pair<Host, Map<Volume, StoragePool>> potentialResources = findPotentialDeploymentResources(suitableHosts, suitableVolumeStoragePools);
					
					if(potentialResources != null){
						Pod pod = _podDao.findById(clusterVO.getPodId());
						Host host = _hostDao.findById(potentialResources.first().getId());
						DeployDestination dest =  new DeployDestination(dc, pod, clusterVO, host, potentialResources.second() );
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
		
		if (s_logger.isDebugEnabled()) {
    		s_logger.debug("Pod To cluster Map is: "+podClusterMap );
		}
		
		List<Long> reorderedClusters = new ArrayList<Long>();
		for (Long pod : podIds){
			if(podClusterMap.containsKey(pod)){
				List<Long> clustersOfThisPod = (List<Long>)podClusterMap.get(pod);
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
		
		if (s_logger.isDebugEnabled()) {
    		s_logger.debug("Reordered cluster list: "+reorderedClusters );
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
	
	protected List<Long> listClustersByCapacity(long id, int requiredCpu, long requiredRam, ExcludeList avoid, boolean isZone){
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
    			
		List<Long> clusterIdswithEnoughCapacity = _capacityDao.orderClustersInZoneOrPodByHostCapacities(id, requiredCpu, requiredRam, capacityType, isZone);
    	if (s_logger.isDebugEnabled()) {
    		s_logger.debug("ClusterId List having enough aggregate capacity: "+clusterIdswithEnoughCapacity );
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
				s_logger.debug("Found a potential host and associated storage pools for this VM");
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

	protected List<Host> findSuitableHosts(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo){
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
	
	protected Map<Volume, List<StoragePool>> findSuitablePoolsForVolumes(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo){
		List<VolumeVO> volumesTobeCreated = _volsDao.findUsableVolumesForInstance(vmProfile.getId());
		Map<Volume, List<StoragePool>> suitableVolumeStoragePools = new HashMap<Volume, List<StoragePool>>();
		
		s_logger.debug("Calling StoragePoolAllocators to find suitable pools");
		
		//for each volume find list of suitable storage pools by calling the allocators
		for (VolumeVO toBeCreated : volumesTobeCreated) {
			s_logger.debug("Checking suitable pools for volume (Id, Type): ("+toBeCreated.getId() +"," +toBeCreated.getVolumeType().name() + ")");
			
			//skip the volume if its already in READY state and has pool allocated
			if(plan.getPoolId() != null){
				if (toBeCreated.getPoolId() != null && toBeCreated.getPoolId().longValue() == plan.getPoolId().longValue()) {
					s_logger.debug("Volume is in READY state and has pool already allocated.");
					List<StoragePool> suitablePools = new ArrayList<StoragePool>();
					StoragePoolVO pool = _storagePoolDao.findById(toBeCreated.getPoolId());
					suitablePools.add(pool);
					suitableVolumeStoragePools.put(toBeCreated, suitablePools);
					continue;
	            }
			}
			DiskOfferingVO diskOffering = _diskOfferingDao.findById(toBeCreated.getDiskOfferingId());
			DiskProfile diskProfile = new DiskProfile(toBeCreated, diskOffering, vmProfile.getHypervisorType());
			
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
	        	//No suitable storage pools found under this cluster for this volume.
	        	break;
	        }
		}
		if(suitableVolumeStoragePools.isEmpty()){
			s_logger.debug("No suitable pools found");
		}
		return suitableVolumeStoragePools;
	}	

	
	@Override
	public boolean check(VirtualMachineProfile vm, DeploymentPlan plan,
			DeployDestination dest, ExcludeList exclude) {
		// TODO Auto-generated method stub
		return false;
	}
}
