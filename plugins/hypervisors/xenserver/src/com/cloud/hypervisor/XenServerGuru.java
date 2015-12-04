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
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import org.apache.log4j.Logger;

public class XenServerGuru extends HypervisorGuruBase implements HypervisorGuru, Configurable {
    private final Logger LOGGER = Logger.getLogger(XenServerGuru.class);
    @Inject
    GuestOSDao _guestOsDao;
    @Inject
    GuestOSHypervisorDao _guestOsHypervisorDao;
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
    @Inject
    UserVmDao _userVmDao;

    static final ConfigKey<Integer> MaxNumberOfVCPUSPerVM = new ConfigKey<Integer>("Advanced", Integer.class, "xen.vm.vcpu.max", "16",
            "Maximum number of VCPUs that VM can get in XenServer.", true, ConfigKey.Scope.Cluster);

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
        UserVmVO userVmVO = _userVmDao.findById(vm.getId());
        if (userVmVO != null) {
            HostVO host = hostDao.findById(userVmVO.getHostId());
            if (host != null) {
                to.setVcpuMaxLimit(MaxNumberOfVCPUSPerVM.valueIn(host.getClusterId()));
            }
        }

        to.setBootloader(bt);

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = _guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), getHypervisorType().toString(), host.getHypervisorVersion());
        }
        if (guestOsMapping == null || host == null) {
            to.setPlatformEmulator(null);
        } else {
            to.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }

        return to;
    }

    @Override
    public boolean trackVmHostChange() {
        return true;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        return null;
    }

    @Override
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        List<Command> commands = new ArrayList<Command>();

        List<VolumeVO> volumes = _volumeDao.findByInstance(vm.getId());

        // it's OK in this case to send a detach command to the host for a root volume as this
        // will simply lead to the SR that supports the root volume being removed
        if (volumes != null) {
            for (VolumeVO volume : volumes) {
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

        return commands;
    }

    @Override
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        LOGGER.debug("getCommandHostDelegation: " + cmd.getClass());
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(true);
        }
        if (cmd instanceof CopyCommand) {
            CopyCommand cpyCommand = (CopyCommand)cmd;
            DataTO srcData = cpyCommand.getSrcTO();
            DataTO destData = cpyCommand.getDestTO();

            if (srcData.getHypervisorType() == HypervisorType.XenServer && srcData.getObjectType() == DataObjectType.SNAPSHOT &&
                    destData.getObjectType() == DataObjectType.TEMPLATE) {
                DataStoreTO srcStore = srcData.getDataStore();
                DataStoreTO destStore = destData.getDataStore();
                if (srcStore instanceof NfsTO && destStore instanceof NfsTO) {
                    HostVO host = hostDao.findById(hostId);
                    EndPoint ep = endPointSelector.selectHypervisorHost(new ZoneScope(host.getDataCenterId()));
                    host = hostDao.findById(ep.getId());
                    hostDao.loadDetails(host);
                    String hypervisorVersion = host.getHypervisorVersion();
                    String snapshotHotFixVersion = host.getDetail(XenserverConfigs.XS620HotFix);
                    if (hypervisorVersion != null && !hypervisorVersion.equalsIgnoreCase("6.1.0")) {
                        if (!(hypervisorVersion.equalsIgnoreCase("6.2.0") &&
                                !(snapshotHotFixVersion != null && snapshotHotFixVersion.equalsIgnoreCase(XenserverConfigs.XSHotFix62ESP1004)))) {
                            return new Pair<Boolean, Long>(Boolean.TRUE, new Long(ep.getId()));
                        }
                    }
                }
            }
        }
        return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
    }

    @Override
    public String getConfigComponentName() {
        return XenServerGuru.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {MaxNumberOfVCPUSPerVM};
    }
}
