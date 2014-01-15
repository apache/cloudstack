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
package com.cloud.hypervisor;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.*;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

@Local(value=HypervisorGuru.class)
public class XenServerGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject GuestOSDao _guestOsDao;
    @Inject
    EndPointSelector endPointSelector;
    @Inject
    HostDao hostDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    VolumeDataFactory _volFactory;

    protected XenServerGuru() {
        super();
    }
    
    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.XenServer;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        BootloaderType bt = BootloaderType.PyGrub;
        if (vm.getBootLoaderType() == BootloaderType.CD) {
        	 bt = vm.getBootLoaderType();
        }
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(bt);
        
        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        
        return to;
    }
    
	@Override
    public boolean trackVmHostChange() {
        return true;
    }

    @Override
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        List<Command> commands = new ArrayList<Command>();

        List<VolumeVO> volumes = _volumeDao.findByInstance(vm.getId());

        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                if (volume.getVolumeType() == Volume.Type.DATADISK) {
                    StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());

                    // storagePool should be null if we are expunging a volume that was never
                    // attached to a VM that was started (the "trick" for storagePool to be null
                    // is that none of the VMs this volume may have been attached to were ever started,
                    // so the volume was never assigned to a storage pool)
                    if (storagePool != null && storagePool.isManaged()) {
                        DataTO volTO = _volFactory.getVolume(volume.getId()).getTO();
                        DiskTO disk = new DiskTO(volTO, volume.getDeviceId(), volume.getPath(), volume.getVolumeType());

                        DettachCommand cmd = new DettachCommand(disk, vm.getInstanceName());

                        cmd.setManaged(true);

                        cmd.setStorageHost(storagePool.getHostAddress());
                        cmd.setStoragePort(storagePool.getPort());

                        cmd.set_iScsiName(volume.get_iScsiName());

                        commands.add(cmd);
                    }
                }
            }
        }

        return commands;
    }

    @Override
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        if (cmd instanceof CopyCommand) {
            CopyCommand cpyCommand = (CopyCommand)cmd;
            DataTO srcData = cpyCommand.getSrcTO();
            DataTO destData = cpyCommand.getDestTO();

            if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.TEMPLATE) {
                DataStoreTO srcStore = srcData.getDataStore();
                DataStoreTO destStore = destData.getDataStore();
                if (srcStore instanceof NfsTO && destStore instanceof NfsTO) {
                    HostVO host = hostDao.findById(hostId);
                    EndPoint ep = endPointSelector.selectHypervisorHost(new ZoneScope(host.getDataCenterId()));
                    host = hostDao.findById(ep.getId());
                    hostDao.loadDetails(host);
                    boolean snapshotHotFix = Boolean.parseBoolean(host.getDetail(HostInfo.XS620_SNAPSHOT_HOTFIX));
                    if (snapshotHotFix) {
                        return new Pair<Boolean, Long>(Boolean.TRUE, new Long(ep.getId()));
                    }
                }
            }
        }
        return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
    }
}
