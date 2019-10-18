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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMEntityVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMEntityDao;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMReservationDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.org.Cluster;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VMEntityManagerImpl implements VMEntityManager {

    private static final Logger s_logger = Logger.getLogger(VMEntityManagerImpl.class);

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

    protected List<DeploymentPlanner> _planners;

    @Inject
    protected VolumeDao _volsDao;

    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    DataStoreManager dataStoreMgr;

    @Inject
    DeploymentPlanningManager _dpMgr;

    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Inject
    DeploymentPlanningManager _planningMgr;

    @Override
    public VMEntityVO loadVirtualMachine(String vmId) {
        // TODO Auto-generated method stub
        return _vmEntityDao.findByUuid(vmId);
    }

    @Override
    public void saveVirtualMachine(VMEntityVO entity) {
        _vmEntityDao.persist(entity);

    }

    protected boolean areAffinityGroupsAssociated(VirtualMachineProfile vmProfile) {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            return true;
        }
        return false;
    }

    @Override
    public String reserveVirtualMachine(VMEntityVO vmEntityVO, DeploymentPlanner plannerToUse, DeploymentPlan planToDeploy, ExcludeList exclude)
        throws InsufficientCapacityException, ResourceUnavailableException {

        //call planner and get the deployDestination.
        //load vm instance and offerings and call virtualMachineManagerImpl
        //FIXME: profile should work on VirtualMachineEntity
        VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());
        VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm);
        vmProfile.setServiceOffering(_serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()));
        if (vmEntityVO.getDetails() != null) {
            Map<String, String> details = vmEntityVO.getDetails();
            if (details.containsKey(VirtualMachineProfile.Param.BootType.getName())) {
                vmProfile.getParameters().put(VirtualMachineProfile.Param.BootType, details.get(VirtualMachineProfile.Param.BootType.getName()));
            }
        }
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodIdToDeployIn(), null, null, null, null);
        if (planToDeploy != null && planToDeploy.getDataCenterId() != 0) {
            plan =
                new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(),
                    planToDeploy.getPoolId(), planToDeploy.getPhysicalNetworkId());
        }

        boolean planChangedByReadyVolume = false;
        List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vm.getId());
        if (!vols.isEmpty()) {
            VolumeVO vol = vols.get(0);
            StoragePool pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(vol.getPoolId());

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
                            throw new ResourceUnavailableException(
                                "Root volume is ready in different cluster, Deployment plan provided cannot be satisfied, unable to create a deployment for " + vm,
                                Cluster.class, clusterIdSpecified);
                        }
                    }
                    plan =
                        new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(),
                            vol.getPoolId(), null, null);
                } else {
                    plan = new DataCenterDeployment(rootVolDcId, rootVolPodId, rootVolClusterId, null, vol.getPoolId(), null, null);
                    planChangedByReadyVolume = true;
                }
            }

        }

        while (true) {
            DeployDestination dest = null;
            try {
                dest = _dpMgr.planDeployment(vmProfile, plan, exclude, plannerToUse);
            } catch (AffinityConflictException e) {
                throw new CloudRuntimeException("Unable to create deployment, affinity rules associated to the VM conflict");
            }

            if (dest != null) {
                String reservationId = _dpMgr.finalizeReservation(dest, vmProfile, plan, exclude, plannerToUse);
                if (reservationId != null) {
                    return reservationId;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Cannot finalize the VM reservation for this destination found, retrying");
                    }
                    exclude.addHost(dest.getHost().getId());
                    continue;
                }
            } else if (planChangedByReadyVolume) {
                // we could not reserve in the Volume's cluster - let the deploy
                // call retry it.
                return UUID.randomUUID().toString();
            } else {
                throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId(),
                    areAffinityGroupsAssociated(vmProfile));
            }
        }
    }

    @Override
    public void deployVirtualMachine(String reservationId, VMEntityVO vmEntityVO, String caller, Map<VirtualMachineProfile.Param, Object> params, boolean deployOnGivenHost)
        throws InsufficientCapacityException, ResourceUnavailableException {
        //grab the VM Id and destination using the reservationId.

        VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());

        VMReservationVO vmReservation = _reservationDao.findByReservationId(reservationId);
        if (vmReservation != null) {

            DataCenterDeployment reservedPlan =
                new DataCenterDeployment(vm.getDataCenterId(), vmReservation.getPodId(), vmReservation.getClusterId(), vmReservation.getHostId(), null, null);
            try {
                _itMgr.start(vm.getUuid(), params, reservedPlan, _planningMgr.getDeploymentPlannerByName(vmReservation.getDeploymentPlanner()));
            } catch (Exception ex) {
                // Retry the deployment without using the reservation plan
                // Retry is only done if host id is not passed in deploy virtual machine api. Otherwise
                // the instance may be started on another host instead of the intended one.
                if (!deployOnGivenHost) {
                    DataCenterDeployment plan = new DataCenterDeployment(0, null, null, null, null, null);

                    if (reservedPlan.getAvoids() != null) {
                        plan.setAvoids(reservedPlan.getAvoids());
                    }

                    _itMgr.start(vm.getUuid(), params, plan, null);
                }
            }
        } else {
            // no reservation found. Let VirtualMachineManager retry
            _itMgr.start(vm.getUuid(), params, null, null);
        }

    }

    @Override
    public boolean stopvirtualmachine(VMEntityVO vmEntityVO, String caller) throws ResourceUnavailableException {
        _itMgr.stop(vmEntityVO.getUuid());
        return true;
    }

    @Override
    public boolean stopvirtualmachineforced(VMEntityVO vmEntityVO, String caller) throws ResourceUnavailableException {
        _itMgr.stopForced(vmEntityVO.getUuid());
        return true;
    }

    @Override
    public boolean destroyVirtualMachine(VMEntityVO vmEntityVO, String caller, boolean expunge) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        VMInstanceVO vm = _vmDao.findByUuid(vmEntityVO.getUuid());
        _itMgr.destroy(vm.getUuid(), expunge);
        return true;
    }

    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }

    @Inject
    public void setPlanners(List<DeploymentPlanner> planners) {
        this._planners = planners;
    }

}
