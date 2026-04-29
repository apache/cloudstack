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
package com.cloud.simulator;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.backup.Backup;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.NicDao;

public class SimulatorGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    GuestOSDao _guestOsDao;

    @Inject
    VMInstanceDao instanceDao;

    @Inject
    VolumeDao volumeDao;

    @Inject
    NicDao nicDao;

    protected SimulatorGuru() {
        super();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Simulator;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());

        return to;
    }

    @Override
    public VirtualMachine importVirtualMachineFromBackup(long zoneId, long domainId, long accountId, long userId,
                                                  String vmInternalName, Backup backup) {
        VMInstanceVO vm = instanceDao.findVMByInstanceNameIncludingRemoved(vmInternalName);
        if (vm.getRemoved() != null) {
            vm.setState(VirtualMachine.State.Stopped);
            vm.setPowerState(VirtualMachine.PowerState.PowerOff);
            instanceDao.update(vm.getId(), vm);
            instanceDao.unremove(vm.getId());
        }
        for (final VolumeVO volume : volumeDao.findIncludingRemovedByInstanceAndType(vm.getId(), null)) {
            volume.setState(Volume.State.Ready);
            volume.setAttached(new Date());
            volumeDao.update(volume.getId(), volume);
            volumeDao.unremove(volume.getId());
        }
        return vm;
    }

    @Override
    public boolean attachRestoredVolumeToVirtualMachine(long zoneId, String location, Backup.VolumeInfo volumeInfo, VirtualMachine vm, long poolId, Backup backup) {

        VMInstanceVO targetVM = instanceDao.findVMByInstanceNameIncludingRemoved(vm.getName());
        List<VolumeVO> vmVolumes = volumeDao.findByInstance(targetVM.getId());
        VolumeVO restoredVolume = volumeDao.findByUuid(location);
        if (restoredVolume != null) {
            try {
                volumeDao.attachVolume(restoredVolume.getId(), vm.getId(), getNextAvailableDeviceId(vmVolumes));
                restoredVolume.setState(Volume.State.Ready);
                volumeDao.update(restoredVolume.getId(), restoredVolume);
                return true;
            } catch (Exception e) {
                restoredVolume.setDisplay(false);
                restoredVolume.setDisplayVolume(false);
                restoredVolume.setState(Volume.State.Destroy);
                volumeDao.update(restoredVolume.getId(), restoredVolume);
                throw new RuntimeException("Unable to attach volume " + restoredVolume.getName() + " to VM" + vm.getName() + " due to : " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public boolean trackVmHostChange() {
        return false;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        return null;
    }

    private long getNextAvailableDeviceId(List<VolumeVO> vmVolumes) {
        if (vmVolumes == null || vmVolumes.isEmpty()) {
            return 0;
        }
        long maxDeviceId = vmVolumes.stream() .mapToLong(VolumeVO::getDeviceId) .max() .orElse(-1);
        return maxDeviceId + 1;
    }
}
