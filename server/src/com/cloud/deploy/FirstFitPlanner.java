package com.cloud.deploy;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

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
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=DeploymentPlanner.class)
public class FirstFitPlanner extends PlannerBase implements DeploymentPlanner {
	private static final Logger s_logger = Logger.getLogger(FirstFitPlanner.class);
	@Inject private HostDao _hostDao;
	@Inject private CapacityDao _capacityDao;
	@Inject private DataCenterDao _dcDao;
	@Inject private HostPodDao _podDao;
	@Inject private ClusterDao _clusterDao;
	@Inject DetailsDao _hostDetailsDao = null;
	@Inject GuestOSDao _guestOSDao = null; 
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
	
	@Override
	public DeployDestination plan(VirtualMachineProfile vmProfile,
			DeploymentPlan plan, ExcludeList avoid)
			throws InsufficientServerCapacityException {
		VirtualMachine vm = vmProfile.getVirtualMachine();
		ServiceOffering offering = vmProfile.getServiceOffering();
		DataCenter dc = _dcDao.findById(vm.getDataCenterId());
		int cpu_requested = offering.getCpu() * offering.getSpeed();
		long ram_requested = offering.getRamSize() * 1024L * 1024L;
		
		s_logger.debug("try to allocate a host from dc:" + plan.getDataCenterId() + ", pod:" + plan.getPodId() + ",cluster:" + plan.getClusterId() +
				", requested cpu: " + cpu_requested + ", requested ram: " + ram_requested);
		if (vm.getLastHostId() != null) {
			HostVO host = _hostDao.findById(vm.getLastHostId());
			
			if (host != null && host.getStatus() == Status.Up) {
				boolean canDepployToLastHost = deployToHost(host, cpu_requested, ram_requested, true, avoid);
				if (canDepployToLastHost) {
					Pod pod = _podDao.findById(vm.getPodId());
					Cluster cluster = _clusterDao.findById(host.getClusterId());
					return new DeployDestination(dc, pod, cluster, host);
				}
			}
		}
		
		/*Go through all the pods/clusters under zone*/
		List<HostPodVO> pods = null;
		if (plan.getPodId() != null) {
			HostPodVO pod = _podDao.findById(plan.getPodId());
			if (pod != null && dc.getId() == pod.getDataCenterId()) {
				pods = new ArrayList<HostPodVO>(1);
				pods.add(pod);
			} else {
				s_logger.debug("Can't enforce the pod selector");
				return null;
			}
		} 
		
		if (pods == null)
			pods = _podDao.listByDataCenterId(plan.getDataCenterId());
		
		//Collections.shuffle(pods);
		
		for (HostPodVO hostPod : pods) {
			if (avoid.shouldAvoid(hostPod)) {
				continue;
			}
			
			//Collections.shuffle(clusters);
			List<ClusterVO> clusters = null;
			if (plan.getClusterId() != null) {
				ClusterVO cluster = _clusterDao.findById(plan.getClusterId());
				if (cluster != null && hostPod.getId() == cluster.getPodId()) {
					clusters = new ArrayList<ClusterVO>(1);
					clusters.add(cluster);
				} else {
					s_logger.debug("Can't enforce the cluster selector");
					return null;
				}
			} 
			
			if (clusters == null) {			
				clusters = _clusterDao.listByPodId(hostPod.getId());
			}
			
			for (ClusterVO clusterVO : clusters) {
				if (avoid.shouldAvoid(clusterVO)) {
					continue;
				}
				
				if (clusterVO.getHypervisorType() != vmProfile.getHypervisorType()) {
					avoid.addCluster(clusterVO.getId());
					continue;
				}
				
				List<HostVO> hosts = _hostDao.listBy(Host.Type.Routing, clusterVO.getId(), hostPod.getId(), dc.getId());
				//Collections.shuffle(hosts);
				
				 // We will try to reorder the host lists such that we give priority to hosts that have
		        // the minimums to support a VM's requirements
		        hosts = prioritizeHosts(vmProfile.getTemplate(), hosts);
				
				for (HostVO hostVO : hosts) {																				
					boolean canDeployToHost = deployToHost(hostVO, cpu_requested, ram_requested, false, avoid);
					if (canDeployToHost) {
						Pod pod = _podDao.findById(hostPod.getId());
						Cluster cluster = _clusterDao.findById(clusterVO.getId());
						Host host = _hostDao.findById(hostVO.getId());
						return new DeployDestination(dc, pod, cluster, host);
					}
					avoid.addHost(hostVO.getId());
				}
				avoid.addCluster(clusterVO.getId());
			}
			avoid.addPod(hostPod.getId());
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
	protected boolean deployToHost(HostVO host, Integer cpu, long ram, boolean fromLastHost, ExcludeList avoid) {		
    	if (avoid.shouldAvoid(host)) {
			return false;
		}
    	
		CapacityVO capacityCpu = _capacityDao.findByHostIdType(host.getId(), CapacityVO.CAPACITY_TYPE_CPU);
		CapacityVO capacityMem = _capacityDao.findByHostIdType(host.getId(), CapacityVO.CAPACITY_TYPE_MEMORY);
				
		capacityCpu = _capacityDao.lockRow(capacityCpu.getId(), true);
    	capacityMem = _capacityDao.lockRow(capacityMem.getId(), true);
    	
		Transaction txn = Transaction.currentTxn();
       
        try {
        	txn.start();
        	   	
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
        		s_logger.debug("alloc cpu from host: " + host.getId() + ", old used: " + usedCpu + ", old reserved: " +
        				reservedCpu + ", old total: " + totalCpu + 
        				"; new used:" + capacityCpu.getUsedCapacity() + ", reserved:" + capacityCpu.getReservedCapacity() + ", total: " + capacityCpu.getTotalCapacity() + 
        				"; requested cpu:" + cpu + ",alloc_from_last:" + fromLastHost);
        		
        		s_logger.debug("alloc mem from host: " + host.getId() + ", old used: " + usedMem + ", old reserved: " +
        				reservedMem + ", old total: " + totalMem + "; new used: " + capacityMem.getUsedCapacity() + ", reserved: " +
        				capacityMem.getReservedCapacity() + ", total: " + capacityMem.getTotalCapacity() + "; requested mem: " + ram + ",alloc_from_last:" + fromLastHost);
        		
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
    
    protected List<HostVO> prioritizeHosts(VirtualMachineTemplate template, List<HostVO> hosts) {
    	if (template == null) {
    		return hosts;
    	}
    	
    	// Determine the guest OS category of the template
    	String templateGuestOSCategory = getTemplateGuestOSCategory(template);
    	
    	List<HostVO> prioritizedHosts = new ArrayList<HostVO>();
    	
    	// If a template requires HVM and a host doesn't support HVM, remove it from consideration
    	List<HostVO> hostsToCheck = new ArrayList<HostVO>();
    	if (template.isRequiresHvm()) {
    		for (HostVO host : hosts) {
    			if (hostSupportsHVM(host)) {
    				hostsToCheck.add(host);
    			}
    		}
    	} else {
    		hostsToCheck.addAll(hosts);
    	}
    	
    	// If a host is tagged with the same guest OS category as the template, move it to a high priority list
    	// If a host is tagged with a different guest OS category than the template, move it to a low priority list
    	List<HostVO> highPriorityHosts = new ArrayList<HostVO>();
    	List<HostVO> lowPriorityHosts = new ArrayList<HostVO>();
    	for (HostVO host : hostsToCheck) {
    		String hostGuestOSCategory = getHostGuestOSCategory(host);
    		if (hostGuestOSCategory == null) {
    			continue;
    		} else if (templateGuestOSCategory.equals(hostGuestOSCategory)) {
    			highPriorityHosts.add(host);
    		} else {
    			lowPriorityHosts.add(host);
    		}
    	}
    	
    	hostsToCheck.removeAll(highPriorityHosts);
    	hostsToCheck.removeAll(lowPriorityHosts);
    	
    	// Prioritize the remaining hosts by HVM capability
    	for (HostVO host : hostsToCheck) {
    		if (!template.isRequiresHvm() && !hostSupportsHVM(host)) {
    			// Host and template both do not support hvm, put it as first consideration
    			prioritizedHosts.add(0, host);
    		} else {
    			// Template doesn't require hvm, but the machine supports it, make it last for consideration
    			prioritizedHosts.add(host);
    		}
    	}
    	
    	// Merge the lists
    	prioritizedHosts.addAll(0, highPriorityHosts);
    	prioritizedHosts.addAll(lowPriorityHosts);
    	
    	return prioritizedHosts;
    }
    
    protected boolean hostSupportsHVM(HostVO host) {
    	// Determine host capabilities
		String caps = host.getCapabilities();
		
		if (caps != null) {
            String[] tokens = caps.split(",");
            for (String token : tokens) {
            	if (token.contains("hvm")) {
            	    return true;
            	}
            }
		}
		
		return false;
    }
    
    protected String getHostGuestOSCategory(HostVO host) {
		DetailVO hostDetail = _hostDetailsDao.findDetail(host.getId(), "guest.os.category.id");
		if (hostDetail != null) {
			String guestOSCategoryIdString = hostDetail.getValue();
			long guestOSCategoryId;
			
			try {
				guestOSCategoryId = Long.parseLong(guestOSCategoryIdString);
			} catch (Exception e) {
				return null;
			}
			
			GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
			
			if (guestOSCategory != null) {
				return guestOSCategory.getName();
			} else {
				return null;
			}
		} else {
			return null;
		}
    }
    
    protected String getTemplateGuestOSCategory(VirtualMachineTemplate template) {
    	long guestOSId = template.getGuestOSId();
    	GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    	long guestOSCategoryId = guestOS.getCategoryId();
    	GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
    	return guestOSCategory.getName();
    }
}
