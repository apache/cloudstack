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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;
import org.apache.cloudstack.resourcedetail.dao.GuestOsDetailsDao;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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

public class XenServerGuru extends HypervisorGuruBase implements HypervisorGuru, Configurable {

    private Logger logger = Logger.getLogger(getClass());

    @Inject
    private GuestOSDao guestOsDao;
    @Inject
    private GuestOSHypervisorDao guestOsHypervisorDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private VolumeDataFactory volFactory;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private GuestOsDetailsDao guestOsDetailsDao;

    private static final ConfigKey<Integer> MaxNumberOfVCPUSPerVM = new ConfigKey<Integer>("Advanced", Integer.class, "xen.vm.vcpu.max", "16",
            "Maximum number of VCPUs that VM can get in XenServer.", true, ConfigKey.Scope.Cluster);

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
        UserVmVO userVmVO = userVmDao.findById(vm.getId());
        if (userVmVO != null) {
            HostVO host = hostDao.findById(userVmVO.getHostId());
            if (host != null) {
                List<HostVO> clusterHosts = hostDao.listByClusterAndHypervisorType(host.getClusterId(), host.getHypervisorType());
                HostVO hostWithMinSocket = clusterHosts.stream().min(Comparator.comparing(HostVO::getCpuSockets)).orElse(null);
                Integer vCpus = MaxNumberOfVCPUSPerVM.valueIn(host.getClusterId());
                if (hostWithMinSocket != null && hostWithMinSocket.getCpuSockets() != null &&
                        hostWithMinSocket.getCpuSockets() < vCpus) {
                    vCpus = hostWithMinSocket.getCpuSockets();
                }
                to.setVcpuMaxLimit(vCpus);
            }
        }

        to.setBootloader(bt);

        // Determine the VM's OS description
        GuestOSVO guestOS = guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());

        Map<String, String> guestOsDetails = guestOsDetailsDao.listDetailsKeyPairs(vm.getVirtualMachine().getGuestOSId());

        to.setGuestOsDetails(guestOsDetails);

        to.setOs(guestOS.getDisplayName());
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), getHypervisorType().toString(), host.getHypervisorVersion());
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
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        List<Command> commands = new ArrayList<Command>();

        List<VolumeVO> volumes = volumeDao.findByInstance(vm.getId());

        // it's OK in this case to send a detach command to the host for a root volume as this
        // will simply lead to the SR that supports the root volume being removed
        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                StoragePoolVO storagePool = storagePoolDao.findById(volume.getPoolId());

                // storagePool should be null if we are expunging a volume that was never
                // attached to a VM that was started (the "trick" for storagePool to be null
                // is that none of the VMs this volume may have been attached to were ever started,
                // so the volume was never assigned to a storage pool)
                if (storagePool != null && storagePool.isManaged()) {
                    DataTO volTO = volFactory.getVolume(volume.getId()).getTO();
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
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(true);
        }
        boolean isCopyCommand = cmd instanceof CopyCommand;
        Pair<Boolean, Long> defaultHostToExecuteCommands = super.getCommandHostDelegation(hostId, cmd);
        if (!isCopyCommand) {
            logger.debug("We are returning the default host to execute commands because the command is not of Copy type.");
            return defaultHostToExecuteCommands;
        }
        CopyCommand copyCommand = (CopyCommand)cmd;
        DataTO srcData = copyCommand.getSrcTO();
        DataTO destData = copyCommand.getDestTO();

        boolean isSourceDataHypervisorXenServer = srcData.getHypervisorType() == HypervisorType.XenServer;
        if (!isSourceDataHypervisorXenServer) {
            logger.debug("We are returning the default host to execute commands because the target hypervisor of the source data is not XenServer.");
            return defaultHostToExecuteCommands;
        }
        DataStoreTO srcStore = srcData.getDataStore();
        DataStoreTO destStore = destData.getDataStore();
        boolean isSourceAndDestinationNfsObjects = srcStore instanceof NfsTO && destStore instanceof NfsTO;
        if (!isSourceAndDestinationNfsObjects) {
            logger.debug("We are returning the default host to execute commands because the source and destination objects are not NFS type.");
            return defaultHostToExecuteCommands;
        }
        boolean isSourceObjectSnapshotTypeAndDestinationObjectTemplateType = srcData.getObjectType() == DataObjectType.SNAPSHOT
                && destData.getObjectType() == DataObjectType.TEMPLATE;
        if (!isSourceObjectSnapshotTypeAndDestinationObjectTemplateType) {
            logger.debug("We are returning the default host to execute commands because the source and destination objects are not snapshot and template respectively.");
            return defaultHostToExecuteCommands;
        }
        HostVO defaultHostToExecuteCommand = hostDao.findById(hostId);

        HostVO hostCandidateToExecutedCommand = hostDao.findHostInZoneToExecuteCommand(defaultHostToExecuteCommand.getDataCenterId(), srcData.getHypervisorType());
        hostDao.loadDetails(hostCandidateToExecutedCommand);
        String hypervisorVersion = hostCandidateToExecutedCommand.getHypervisorVersion();
        if (StringUtils.isBlank(hypervisorVersion)) {
            logger.debug("We are returning the default host to execute commands because the hypervisor version is blank.");
            return defaultHostToExecuteCommands;
        }
        boolean isXenServer610 = StringUtils.equals(hypervisorVersion, "6.1.0");
        if (isXenServer610) {
            logger.debug("We are returning the default host to execute commands because the hypervisor version is 6.1.0.");
            return defaultHostToExecuteCommands;
        }
        String snapshotHotFixVersion = hostCandidateToExecutedCommand.getDetail(XenserverConfigs.XS620HotFix);
        boolean isXenServer620 = StringUtils.equals(hypervisorVersion, "6.2.0");
        if (isXenServer620 && !StringUtils.equalsIgnoreCase(XenserverConfigs.XSHotFix62ESP1004, snapshotHotFixVersion)) {
            logger.debug(String.format("We are returning the default host to execute commands because the hypervisor version is not 6.2.0 with hotfix ESP1004 [hypervisorVersion=%s, hotfixVersion=%s]", hypervisorVersion, snapshotHotFixVersion));
            return defaultHostToExecuteCommands;
        }
        logger.debug(String.format("We are changing the hostId to executed command from %d to %d.", hostId, hostCandidateToExecutedCommand.getId()));
        return new Pair<Boolean, Long>(Boolean.TRUE, new Long(hostCandidateToExecutedCommand.getId()));
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
