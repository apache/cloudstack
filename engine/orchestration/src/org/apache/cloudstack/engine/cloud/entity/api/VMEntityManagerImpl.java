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
package org.apache.cloudstack.engine.cloud.entity.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMEntityDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMReservationDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.org.Cluster;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VMEntityManagerImpl implements VMEntityManager {

    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected VMTemplateDao _templateDao = null;
    
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    
    @Inject
    protected DiskOfferingDao _diskOfferingDao = null;
    
    @Inject
    protected NetworkDao _networkDao;
    
    @Inject
    protected AccountDao _accountDao = null;

    @Inject
    protected UserDao _userDao = null;

    @Inject 
    protected VMEntityDao _vmEntityDao;
    
    @Inject 
    protected VMReservationDao _reservationDao;
    
    @Inject
    protected VirtualMachineManager _itMgr;
    
    @Inject
    protected List<DeploymentPlanner> _planners;
    
    @Inject
    protected VolumeDao _volsDao;
    
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    DataStoreManager dataStoreMgr;
    
	@Override
	public VMEntityVO loadVirtualMachine(String vmId) {
		// TODO Auto-generated method stub
		return _vmEntityDao.findByUuid(vmId);
	}

	@Override
	public void saveVirtualMachine(VMEntityVO entity) {
	    _vmEntityDao.persist(entity);
		
	}

    @Override
    public String reserveVirtualMachine(VMEntityVO vmEntityVO, String plannerToUse, DeploymentPlan planToDeploy, ExcludeList exclude) 
            throws InsufficientCapacityException, ResourceUnavailableException {

        //call planner and get the deployDestination.
        //load vm instance and offerings and call virtualMachineManagerImpl
        //FIXME: profile should work on VirtualMachineEntity
        VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());
        VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodIdToDeployIn(), null, null, null, null);
        if(planToDeploy != null && planToDeploy.getDataCenterId() != 0){
            plan = new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(), planToDeploy.getPoolId(), planToDeploy.getPhysicalNetworkId());
        }
        
        List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vm.getId());
        if(!vols.isEmpty()){
            VolumeVO vol = vols.get(0);
            StoragePool pool = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(vol.getPoolId());
            
            if (!pool.isInMaintenance()) {
                long rootVolDcId = pool.getDataCenterId();
                Long rootVolPodId = pool.getPodId();
                Long rootVolClusterId = pool.getClusterId();
                if (planToDeploy != null && planToDeploy.getDataCenterId() != 0) {
                    Long clusterIdSpecified = planToDeploy.getClusterId();
                    if (clusterIdSpecified != null && rootVolClusterId != null) {
                        if (rootVolClusterId.longValue() != clusterIdSpecified.longValue()) {
                            // cannot satisfy the plan passed in to the
                            // planner
                            throw new ResourceUnavailableException("Root volume is ready in different cluster, Deployment plan provided cannot be satisfied, unable to create a deployment for "
                                    + vm, Cluster.class, clusterIdSpecified);
                        }
                    }
                    plan = new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(), vol.getPoolId(), null, null);
                }else{
                    plan = new DataCenterDeployment(rootVolDcId, rootVolPodId, rootVolClusterId, null, vol.getPoolId(), null, null);

                }
            }
        
        }
        
        DeploymentPlanner planner = ComponentContext.getComponent(plannerToUse);
        DeployDestination dest = null;
        
        if (planner.canHandle(vmProfile, plan, exclude)) {
            dest = planner.plan(vmProfile, plan, exclude);
        } 

        if (dest != null) {
           //save destination with VMEntityVO
            VMReservationVO vmReservation = new VMReservationVO(vm.getId(), dest.getDataCenter().getId(), dest.getPod().getId(), dest.getCluster().getId(), dest.getHost().getId());
            Map<Long,Long> volumeReservationMap = new HashMap<Long,Long>();
            
            if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                for(Volume vo : dest.getStorageForDisks().keySet()){
                    volumeReservationMap.put(vo.getId(), dest.getStorageForDisks().get(vo).getId());
                }
                vmReservation.setVolumeReservation(volumeReservationMap);
            }

            vmEntityVO.setVmReservation(vmReservation);
            _vmEntityDao.persist(vmEntityVO);
            
            return vmReservation.getUuid();
        }else{
            throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId());
        }
        
    }

    @Override
    public void deployVirtualMachine(String reservationId, String caller, Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException{
        //grab the VM Id and destination using the reservationId.
        
        VMReservationVO vmReservation = _reservationDao.findByReservationId(reservationId);
        long vmId = vmReservation.getVmId();
        
        VMInstanceVO vm = _vmDao.findById(vmId);
        //Pass it down
        Long poolId = null;
        Map<Long,Long> storage = vmReservation.getVolumeReservation();
        if(storage != null){
            List<Long> volIdList = new ArrayList<Long>(storage.keySet());
            if(volIdList !=null && !volIdList.isEmpty()){
                poolId = storage.get(volIdList.get(0));
            }
        }
        
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vmReservation.getPodId(), vmReservation.getClusterId(),
                vmReservation.getHostId(), poolId , null);
        
        VMInstanceVO vmDeployed = _itMgr.start(vm, params, _userDao.findById(new Long(caller)), _accountDao.findById(vm.getAccountId()), plan);
        
    }

    @Override
    public boolean stopvirtualmachine(VMEntityVO vmEntityVO, String caller) throws ResourceUnavailableException {

        VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());
        return _itMgr.stop(vm, _userDao.findById(new Long(caller)), _accountDao.findById(vm.getAccountId()));

    }

    @Override
    public boolean destroyVirtualMachine(VMEntityVO vmEntityVO, String caller) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException{

         VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());
         return _itMgr.destroy(vm, _userDao.findById(new Long(caller)), _accountDao.findById(vm.getAccountId()));

            
    }

}
