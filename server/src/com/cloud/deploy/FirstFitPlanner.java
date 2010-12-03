package com.cloud.deploy;

import java.util.Collections;
import java.util.List;

import javax.ejb.Local;

import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
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
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
@Local(value=DeploymentPlanner.class)
public class FirstFitPlanner extends PlannerBase implements DeploymentPlanner {
	@Inject private HostDao _hostDao;
	@Inject private CapacityDao _capacityDao;
	@Inject private DataCenterDao _dcDao;
	@Inject private HostPodDao _podDao;
	@Inject private ClusterDao _clusterDao;
	
	@Override
	public DeployDestination plan(VirtualMachineProfile vmProfile,
			DeploymentPlan plan, ExcludeList avoid)
			throws InsufficientServerCapacityException {
		VirtualMachine vm = vmProfile.getVirtualMachine();
		ServiceOffering offering = vmProfile.getServiceOffering();
		DataCenter dc = _dcDao.findById(vm.getDataCenterId());
		int cpu_requested = offering.getCpu() * offering.getSpeed();
		int ram_requested = offering.getRamSize();
		
		if (vm.getLastHostId() != null) {
			HostVO host = _hostDao.findById(vm.getLastHostId());
			
			if (host.getStatus() == Status.Up) {
				boolean canDepployToLastHost = deployToHost(vm.getLastHostId(), cpu_requested, ram_requested, true);
				if (canDepployToLastHost) {
					Pod pod = _podDao.findById(vm.getPodId());
					Cluster cluster = _clusterDao.findById(host.getClusterId());
					return new DeployDestination(dc, pod, cluster, host);
				}
			}
		}
		
		/*Go through all the pods/clusters under zone*/
		List<HostPodVO> pods = _podDao.listByDataCenterId(plan.getDataCenterId());
		//Collections.shuffle(pods);
		
		for (HostPodVO hostPod : pods) {
			List<ClusterVO> clusters = _clusterDao.listByPodId(hostPod.getId());
			//Collections.shuffle(clusters);
			
			for (ClusterVO clusterVO : clusters) {
				
				if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
					continue;
				}
				
				List<HostVO> hosts = _hostDao.listByCluster(clusterVO.getId());
				//Collections.shuffle(hosts);
				
				
				for (HostVO hostVO : hosts) {
					if (hostVO.getStatus() != Status.Up) {
						continue;
					}
					
					if (avoid.shouldAvoid(hostVO)) {
						continue;
					}
					
					boolean canDeployToHost = deployToHost(hostVO.getId(), cpu_requested, ram_requested, false);
					if (canDeployToHost) {
						Pod pod = _podDao.findById(hostPod.getId());
						Cluster cluster = _clusterDao.findById(clusterVO.getId());
						Host host = _hostDao.findById(hostVO.getId());
						return new DeployDestination(dc, pod, cluster, host);
					}
				}
			}
		}
		
		return null;
	}

	
	@Override
	public boolean check(VirtualMachineProfile vm, DeploymentPlan plan,
			DeployDestination dest, ExcludeList exclude) {
		// TODO Auto-generated method stub
		return false;
	}
	  
    @DB
	protected boolean deployToHost(Long hostId, Integer cpu, long ram, boolean fromLastHost) {
		
		CapacityVO capacityCpu = _capacityDao.findByHostIdType(hostId, CapacityVO.CAPACITY_TYPE_CPU);
		CapacityVO capacityMem = _capacityDao.findByHostIdType(hostId, CapacityVO.CAPACITY_TYPE_MEMORY);
				
		Transaction txn = Transaction.currentTxn();
       
        try {
        	txn.start();
        	capacityCpu = _capacityDao.lockRow(capacityCpu.getId(), true);
        	capacityMem = _capacityDao.lockRow(capacityMem.getId(), true);
        	
        	long usedCpu = capacityCpu.getUsedCapacity();
    		long usedMem = capacityMem.getUsedCapacity();
    		long reservedCpu = capacityCpu.getReservedCapacity();
    		long reservedMem = capacityMem.getReservedCapacity();
    		long totalCpu = capacityCpu.getTotalCapacity();
    		long totalMem = capacityMem.getTotalCapacity();
    		
    		boolean success = false;
        	if (fromLastHost) {
        		/*alloc from reserved*/
        		if (reservedCpu >= cpu && reservedMem >= ram) {
        			capacityCpu.setReservedCapacity(reservedCpu - cpu);
        			capacityMem.setReservedCapacity(reservedMem - ram);        
        			capacityCpu.setUsedCapacity(usedCpu + cpu);
        			capacityMem.setUsedCapacity(usedMem + ram);
        			success = true;
        		}		
        	} else {
        		/*alloc from free resource*/
        		if ((reservedCpu + usedCpu + cpu <= totalCpu) && (reservedMem + usedMem + ram <= totalMem)) {
        			capacityCpu.setUsedCapacity(usedCpu + cpu);
        			capacityMem.setUsedCapacity(usedMem + ram);
        			success = true;
        		}
        	}
        	
        	if (success) {
        		_capacityDao.update(capacityCpu.getId(), capacityCpu);
        		_capacityDao.update(capacityMem.getId(), capacityMem);
        	}
        	
        	txn.commit();
        	return success;
        } catch (Exception e) {
        	txn.rollback();
        	return false;
        }        		
	}
    
}
