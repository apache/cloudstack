/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.platform.orchestration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntityImpl;
import org.apache.cloudstack.engine.cloud.entity.api.VMEntityManager;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;


@Component
public class CloudOrchestrator implements OrchestrationService {

	@Inject
	private VMEntityManager vmEntityManager;

	@Inject
	private VirtualMachineManager _itMgr;
	
	@Inject
	protected VMTemplateDao _templateDao = null;
	
    @Inject
    protected VMInstanceDao _vmDao;
    
    @Inject
    protected UserVmDao _userVmDao = null;
    
	@Inject
	protected ServiceOfferingDao _serviceOfferingDao;
	
	@Inject
	protected DiskOfferingDao _diskOfferingDao = null;
	
	@Inject
	protected NetworkDao _networkDao;
	
	@Inject
	protected AccountDao _accountDao = null;

	public CloudOrchestrator() {
	}
	
    public VirtualMachineEntity createFromScratch(String uuid, String iso, String os, String hypervisor, String hostName, int cpu, int speed, long memory, List<String> networks, List<String> computeTags,
            Map<String, String> details, String owner) {
        // TODO Auto-generated method stub
        return null;
    }

    public String reserve(String vm, String planner, Long until) throws InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    public String deploy(String reservationId) {
        // TODO Auto-generated method stub
        return null;
    }

    public void joinNetwork(String network1, String network2) {
        // TODO Auto-generated method stub

    }

    public void createNetwork() {
        // TODO Auto-generated method stub

    }

    public void destroyNetwork() {
        // TODO Auto-generated method stub

    }

    @Override
    public VolumeEntity createVolume() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateEntity registerTemplate(String name, URL path, String os, Hypervisor hypervisor) {
        return null;
    }

    @Override
    public void destroyNetwork(String networkUuid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroyVolume(String volumeEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public VirtualMachineEntity createVirtualMachine(
            String id, 
            String owner, 
            String templateId, 
            String hostName, 
            String displayName,
            String hypervisor,
            int cpu, 
            int speed, 
            long memory,
            Long diskSize,
            List<String> computeTags, 
            List<String> rootDiskTags,
            Map<String, NicProfile> networkNicMap, DeploymentPlan plan) throws InsufficientCapacityException {

    	// VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, networks, vmEntityManager);

        List<Pair<NetworkVO, NicProfile>> networkIpMap = new ArrayList<Pair<NetworkVO, NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _networkDao.findByUuid(uuid);
            if(network != null){
                networkIpMap.add(new Pair<NetworkVO, NicProfile>(network, networkNicMap.get(uuid)));
            }
        }

    	VirtualMachineEntityImpl vmEntity = ComponentContext.inject(VirtualMachineEntityImpl.class);
    	vmEntity.init(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, new ArrayList<String>(networkNicMap.keySet()));
        
    	
    	HypervisorType hypervisorType = HypervisorType.valueOf(hypervisor);

    	//load vm instance and offerings and call virtualMachineManagerImpl
    	VMInstanceVO vm = _vmDao.findByUuid(id);
    	
		// If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
		// Else, a disk offering is optional, and if present will be used to create the data disk

    	Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();
 
		ServiceOfferingVO offering = _serviceOfferingDao.findById(vm.getServiceOfferingId());
		rootDiskOffering.first(offering);
		
		if(vm.getDiskOfferingId() != null){
    		DiskOfferingVO diskOffering = _diskOfferingDao.findById(vm.getDiskOfferingId());
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
		
		
		
    	if (_itMgr.allocate(_userVmDao.findById(vm.getId(), true), _templateDao.findById(new Long(templateId)), offering, rootDiskOffering, dataDiskOfferings, networkIpMap, null,	plan, hypervisorType, _accountDao.findById(new Long(owner))) == null) {
			return null;
		}
    	
        return vmEntity;
    }

    @Override
    public VirtualMachineEntity createVirtualMachineFromScratch(String id, String owner, String isoId, String hostName, String displayName, String hypervisor, String os, int cpu, int speed, long memory,Long diskSize,
            List<String> computeTags, List<String> rootDiskTags, Map<String, NicProfile> networkNicMap, DeploymentPlan plan)  throws InsufficientCapacityException {
		
    	// VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, networks, vmEntityManager);
    	VirtualMachineEntityImpl vmEntity = ComponentContext.inject(VirtualMachineEntityImpl.class);
    	vmEntity.init(id, owner, hostName, displayName, cpu, speed, memory, computeTags, rootDiskTags, new ArrayList<String>(networkNicMap.keySet()));

    	//load vm instance and offerings and call virtualMachineManagerImpl
    	VMInstanceVO vm = _vmDao.findByUuid(id);
    	
    	
		Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
		ServiceOfferingVO offering = _serviceOfferingDao.findById(vm.getServiceOfferingId());
		rootDiskOffering.first(offering);
		
		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();
		Long diskOfferingId = vm.getDiskOfferingId();
		if (diskOfferingId == null) {
			throw new InvalidParameterValueException(
					"Installing from ISO requires a disk offering to be specified for the root disk.");
		}
		DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
		if (diskOffering == null) {
			throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
		}
		Long size = null;
		if (diskOffering.getDiskSize() == 0) {
			size = diskSize;
			if (size == null) {
				throw new InvalidParameterValueException("Disk offering "
						+ diskOffering + " requires size parameter.");
			}
		}
		rootDiskOffering.first(diskOffering);
		rootDiskOffering.second(size);
		
        List<Pair<NetworkVO, NicProfile>> networkIpMap = new ArrayList<Pair<NetworkVO, NicProfile>>();
        for (String uuid : networkNicMap.keySet()) {
            NetworkVO network = _networkDao.findByUuid(uuid);
            if(network != null){
                networkIpMap.add(new Pair<NetworkVO, NicProfile>(network, networkNicMap.get(uuid)));
            }
        }
		
		HypervisorType hypervisorType = HypervisorType.valueOf(hypervisor);
		
    	if (_itMgr.allocate(_userVmDao.findById(vm.getId(), true), _templateDao.findById(new Long(isoId)), offering, rootDiskOffering, dataDiskOfferings, networkIpMap, null,	plan, hypervisorType, _accountDao.findById(new Long(owner))) == null) {
			return null;
		}
    	
        return vmEntity;
    }

    @Override
    public NetworkEntity createNetwork(String id, String name, String domainName, String cidr, String gateway) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public VirtualMachineEntity getVirtualMachine(String id) {
		VirtualMachineEntityImpl vmEntity = new VirtualMachineEntityImpl(id, vmEntityManager);
		return vmEntity;
	}

}
