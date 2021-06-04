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
package org.apache.cloudstack.storage.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class VMSnapshotHelperImpl implements VMSnapshotHelper {
    @Inject
    VMSnapshotDao _vmSnapshotDao;
    @Inject
    UserVmDao userVmDao;
    @Inject
    HostDao hostDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    VolumeDataFactory volumeDataFactory;

    StateMachine2<VMSnapshot.State, VMSnapshot.Event, VMSnapshot> _vmSnapshottateMachine;

    public VMSnapshotHelperImpl() {
        _vmSnapshottateMachine = VMSnapshot.State.getStateMachine();
    }

    @Override
    public boolean vmSnapshotStateTransitTo(VMSnapshot vsnp, VMSnapshot.Event event) throws NoTransitionException {
        return _vmSnapshottateMachine.transitTo(vsnp, event, null, _vmSnapshotDao);
    }

    @Override
    public Long pickRunningHost(Long vmId) {
        UserVmVO vm = userVmDao.findById(vmId);
        // use VM's host if VM is running
        if (vm.getState() == VirtualMachine.State.Running)
            return vm.getHostId();

        // check if lastHostId is available
        if (vm.getLastHostId() != null) {
            HostVO lastHost = hostDao.findByIdIncludingRemoved(vm.getLastHostId());
            if (lastHost.getStatus() == com.cloud.host.Status.Up && !lastHost.isInMaintenanceStates())
                return lastHost.getId();
        }

        List<VolumeVO> listVolumes = volumeDao.findByInstance(vmId);
        if (listVolumes == null || listVolumes.size() == 0) {
            throw new InvalidParameterValueException("vmInstance has no volumes");
        }
        VolumeVO volume = listVolumes.get(0);
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            throw new InvalidParameterValueException("pool id is not found");
        }
        StoragePoolVO storagePool = primaryDataStoreDao.findById(poolId);
        if (storagePool == null) {
            throw new InvalidParameterValueException("storage pool is not found");
        }
        List<HostVO> listHost =
            hostDao.listAllUpAndEnabledNonHAHosts(Host.Type.Routing, storagePool.getClusterId(), storagePool.getPodId(), storagePool.getDataCenterId(), null);
        if (listHost == null || listHost.size() == 0) {
            throw new InvalidParameterValueException("no host in up state is found");
        }
        return listHost.get(0).getId();
    }

    @Override
    public List<VolumeObjectTO> getVolumeTOList(Long vmId) {
        List<VolumeObjectTO> volumeTOs = new ArrayList<VolumeObjectTO>();
        List<VolumeVO> volumeVos = volumeDao.findByInstance(vmId);
        VolumeInfo volumeInfo = null;
        for (VolumeVO volume : volumeVos) {
            volumeInfo = volumeDataFactory.getVolume(volume.getId());

            volumeTOs.add((VolumeObjectTO)volumeInfo.getTO());
        }
        return volumeTOs;
    }

    private VMSnapshotTO convert2VMSnapshotTO(VMSnapshotVO vo) {
        return new VMSnapshotTO(vo.getId(), vo.getName(), vo.getType(), vo.getCreated().getTime(), vo.getDescription(), vo.getCurrent(), null, true);
    }

    @Override
    public VMSnapshotTO getSnapshotWithParents(VMSnapshotVO snapshot) {
        Map<Long, VMSnapshotVO> snapshotMap = new HashMap<Long, VMSnapshotVO>();
        List<VMSnapshotVO> allSnapshots = _vmSnapshotDao.findByVm(snapshot.getVmId());
        for (VMSnapshotVO vmSnapshotVO : allSnapshots) {
            snapshotMap.put(vmSnapshotVO.getId(), vmSnapshotVO);
        }

        VMSnapshotTO currentTO = convert2VMSnapshotTO(snapshot);
        VMSnapshotTO result = currentTO;
        VMSnapshotVO current = snapshot;
        while (current.getParent() != null) {
            VMSnapshotVO parent = snapshotMap.get(current.getParent());
            if (parent == null) {
                break;
            }
            currentTO.setParent(convert2VMSnapshotTO(parent));
            current = snapshotMap.get(current.getParent());
            currentTO = currentTO.getParent();
        }
        return result;
    }

    @Override
    public Long getStoragePoolForVM(Long vmId) {
        List<VolumeVO> rootVolumes = volumeDao.findReadyRootVolumesByInstance(vmId);
        if (rootVolumes == null || rootVolumes.isEmpty()) {
            throw new InvalidParameterValueException("Failed to find root volume for the user vm:" + vmId);
        }

        VolumeVO rootVolume = rootVolumes.get(0);
        StoragePoolVO rootVolumePool = primaryDataStoreDao.findById(rootVolume.getPoolId());
        if (rootVolumePool == null) {
            throw new InvalidParameterValueException("Failed to find root volume storage pool for the user vm:" + vmId);
        }

        if (rootVolumePool.isInMaintenance()) {
            throw new InvalidParameterValueException("Storage pool for the user vm:" + vmId + " is in maintenance");
        }

        return rootVolumePool.getId();
    }

    @Override
    public Storage.StoragePoolType getStoragePoolType(Long poolId) {
        StoragePoolVO storagePool = primaryDataStoreDao.findById(poolId);
        if (storagePool == null) {
            throw new InvalidParameterValueException("storage pool is not found");
        }

        return storagePool.getPoolType();
    }
}
