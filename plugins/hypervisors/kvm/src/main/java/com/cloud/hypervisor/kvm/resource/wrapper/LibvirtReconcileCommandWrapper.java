//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.command.ReconcileAnswer;
import org.apache.cloudstack.command.ReconcileCommand;
import org.apache.cloudstack.command.ReconcileCopyAnswer;
import org.apache.cloudstack.command.ReconcileCopyCommand;
import org.apache.cloudstack.command.ReconcileMigrateAnswer;
import org.apache.cloudstack.command.ReconcileMigrateCommand;
import org.apache.cloudstack.command.ReconcileMigrateVolumeAnswer;
import org.apache.cloudstack.command.ReconcileMigrateVolumeCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ResourceWrapper(handles =  ReconcileCommand.class)
public final class LibvirtReconcileCommandWrapper extends CommandWrapper<ReconcileCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final ReconcileCommand command, final LibvirtComputingResource libvirtComputingResource) {

        if (command instanceof ReconcileMigrateCommand) {
            return handle((ReconcileMigrateCommand) command, libvirtComputingResource);
        } else if (command instanceof ReconcileCopyCommand) {
            return handle((ReconcileCopyCommand) command, libvirtComputingResource);
        } else if (command instanceof ReconcileMigrateVolumeCommand) {
            return handle((ReconcileMigrateVolumeCommand) command, libvirtComputingResource);
        }
        return new ReconcileAnswer();
    }

    private ReconcileAnswer handle(final ReconcileMigrateCommand reconcileCommand, final LibvirtComputingResource libvirtComputingResource) {
        String vmName = reconcileCommand.getVmName();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

        ReconcileMigrateAnswer answer;
        try {
            Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            Domain vm = conn.domainLookupByName(vmName);
            DomainState domainState = vm.getInfo().state;
            logger.debug(String.format("Found VM %s with domain state %s", vmName, domainState));
            VirtualMachine.State state = getState(domainState);
            List<String> disks = null;
            if (VirtualMachine.State.Running.equals(state)) {
                disks = getVmDiskPaths(libvirtComputingResource.getDisks(conn, vmName));
            }
            answer = new ReconcileMigrateAnswer(vmName, state);
            answer.setVmDisks(disks);
        } catch (LibvirtException e) {
            logger.debug(String.format("Failed to get state of VM %s, assume it is Stopped", vmName));
            VirtualMachine.State state = VirtualMachine.State.Stopped;
            answer = new ReconcileMigrateAnswer(vmName, state);
        }
        return answer;
    }

    static VirtualMachine.State getState(DomainState domainState) {
        VirtualMachine.State state;
        if (domainState == DomainState.VIR_DOMAIN_RUNNING) {
            state = VirtualMachine.State.Running;
        } else if (Arrays.asList(DomainState.VIR_DOMAIN_SHUTDOWN, DomainState.VIR_DOMAIN_SHUTOFF, DomainState.VIR_DOMAIN_CRASHED).contains(domainState)) {
            state = VirtualMachine.State.Stopped;
        } else if (domainState == DomainState.VIR_DOMAIN_PAUSED) {
            state = VirtualMachine.State.Unknown;
        } else {
            state = VirtualMachine.State.Unknown;
        }
        return state;
    }

    private List<String> getVmDiskPaths(List<LibvirtVMDef.DiskDef> diskDefs) {
        List<String> diskPaths = new ArrayList<String>();
        for (LibvirtVMDef.DiskDef diskDef : diskDefs) {
            if (diskDef.getDiskPath() != null) {
                diskPaths.add(diskDef.getDiskPath());
            }
        }
        return diskPaths;
    }

    private ReconcileAnswer handle(final ReconcileCopyCommand reconcileCommand, final LibvirtComputingResource libvirtComputingResource) {
        DataTO srcData = reconcileCommand.getSrcData();
        DataTO destData = reconcileCommand.getDestData();
        DataStoreTO srcDataStore = srcData.getDataStore();
        DataStoreTO destDataStore = destData.getDataStore();

        // consistent with StorageSubsystemCommandHandlerBase.execute(CopyCommand cmd)
        if (srcData.getObjectType() == DataObjectType.TEMPLATE &&
                (srcData.getDataStore().getRole() == DataStoreRole.Image || srcData.getDataStore().getRole() == DataStoreRole.ImageCache) &&
                destData.getDataStore().getRole() == DataStoreRole.Primary) {
            String reason = "copy template to primary storage";
            return new ReconcileCopyAnswer(true, reason);
        } else if (srcData.getObjectType() == DataObjectType.TEMPLATE && srcDataStore.getRole() == DataStoreRole.Primary &&
                destDataStore.getRole() == DataStoreRole.Primary) {
            String reason = "clone template to a volume";
            return new ReconcileCopyAnswer(true, reason);
        } else if (srcData.getObjectType() == DataObjectType.VOLUME &&
                (srcData.getDataStore().getRole() == DataStoreRole.ImageCache || srcDataStore.getRole() == DataStoreRole.Image)) {
            logger.debug("Reconciling: copy volume from image cache to primary");
            return reconcileCopyVolumeFromImageCacheToPrimary(srcData, destData, reconcileCommand.getOption2(), libvirtComputingResource);
        } else if (srcData.getObjectType() == DataObjectType.VOLUME && srcData.getDataStore().getRole() == DataStoreRole.Primary) {
            if (destData.getObjectType() == DataObjectType.VOLUME) {
                if ((srcData instanceof VolumeObjectTO && ((VolumeObjectTO)srcData).isDirectDownload()) ||
                        destData.getDataStore().getRole() == DataStoreRole.Primary) {
                    logger.debug("Reconciling: copy volume from primary to primary");
                    return reconcileCopyVolumeFromPrimaryToPrimary(srcData, destData, libvirtComputingResource);
                } else {
                    logger.debug("Reconciling: copy volume from primary to secondary");
                    return reconcileCopyVolumeFromPrimaryToSecondary(srcData, destData, reconcileCommand.getOption(), libvirtComputingResource);
                }
            } else if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                String reason = "create volume from template";
                return new ReconcileCopyAnswer(true, reason);
            }
        } else if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.SNAPSHOT &&
                srcData.getDataStore().getRole() == DataStoreRole.Primary) {
            String reason = "backup snapshot from primary";
            return new ReconcileCopyAnswer(true, reason);
        } else if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.VOLUME) {
            String reason = "create volume from snapshot";
            return new ReconcileCopyAnswer(true, reason);
        } else if (srcData.getObjectType() == DataObjectType.SNAPSHOT && destData.getObjectType() == DataObjectType.TEMPLATE) {
            String  reason = "create template from snapshot";
            return new ReconcileCopyAnswer(true, reason);
        }

        return new ReconcileCopyAnswer(true, "not implemented yet");
    }

    private ReconcileCopyAnswer reconcileCopyVolumeFromImageCacheToPrimary(DataTO srcData, DataTO destData, Map<String, String> details, LibvirtComputingResource libvirtComputingResource) {
        // consistent with KVMStorageProcessor.copyVolumeFromImageCacheToPrimary
        final DataStoreTO srcStore = srcData.getDataStore();
        if (!(srcStore instanceof NfsTO)) {
            return new ReconcileCopyAnswer(true, "can only handle nfs storage as source");
        }
        final DataStoreTO destStore = destData.getDataStore();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destStore;
        String path = destData.getPath();
        if (path == null) {
            path = details != null ? details.get(DiskTO.PATH) : null;
        }
        if (path == null) {
            path = details != null ? details.get(DiskTO.IQN) : null;
        }
        if (path == null) {
            return new ReconcileCopyAnswer(true, "path and iqn on destination storage are null");
        }
        try {
            VolumeOnStorageTO volumeOnDestination = libvirtComputingResource.getVolumeOnStorage(primaryStore, path);
            return new ReconcileCopyAnswer(null, volumeOnDestination);
        } catch (final CloudRuntimeException e) {
            logger.debug("Failed to reconcile CopyVolumeFromImageCacheToPrimary: ", e);
            return new ReconcileCopyAnswer(false, false, e.toString());
        }
    }
    private ReconcileCopyAnswer reconcileCopyVolumeFromPrimaryToPrimary(DataTO srcData, DataTO destData, LibvirtComputingResource libvirtComputingResource) {
        // consistent with KVMStorageProcessor.copyVolumeFromPrimaryToPrimary
        final String srcVolumePath = srcData.getPath();
        final String destVolumePath = destData.getPath();
        final DataStoreTO srcStore = srcData.getDataStore();
        final DataStoreTO destStore = destData.getDataStore();
        final PrimaryDataStoreTO srcPrimaryStore = (PrimaryDataStoreTO)srcStore;
        final PrimaryDataStoreTO destPrimaryStore = (PrimaryDataStoreTO)destStore;

        VolumeOnStorageTO volumeOnSource = null;
        VolumeOnStorageTO volumeOnDestination = null;
        try {
            volumeOnSource = libvirtComputingResource.getVolumeOnStorage(srcPrimaryStore, srcVolumePath);
            if (destPrimaryStore.isManaged() || destVolumePath != null) {
                volumeOnDestination = libvirtComputingResource.getVolumeOnStorage(destPrimaryStore, destVolumePath);
            }
            return new ReconcileCopyAnswer(volumeOnSource, volumeOnDestination);
        } catch (final CloudRuntimeException e) {
            logger.debug("Failed to reconcile CopyVolumeFromPrimaryToPrimary: ", e);
            return new ReconcileCopyAnswer(false, false, e.toString());
        }
    }

    private ReconcileCopyAnswer reconcileCopyVolumeFromPrimaryToSecondary(DataTO srcData, DataTO destData, Map<String, String> details, LibvirtComputingResource libvirtComputingResource) {
        // consistent with KVMStorageProcessor.copyVolumeFromPrimaryToSecondary
        final String srcVolumePath = srcData.getPath();
        final DataStoreTO srcStore = srcData.getDataStore();
        final DataStoreTO destStore = destData.getDataStore();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcStore;
        if (!(destStore instanceof NfsTO)) {
            return new ReconcileCopyAnswer(true, "can only handle nfs storage as destination");
        }
        VolumeOnStorageTO volumeOnSource = libvirtComputingResource.getVolumeOnStorage(primaryStore, srcVolumePath);
        return new ReconcileCopyAnswer(volumeOnSource, null);
    }

    private ReconcileAnswer handle(final ReconcileMigrateVolumeCommand reconcileCommand, final LibvirtComputingResource libvirtComputingResource) {
        // consistent with LibvirtMigrateVolumeCommandWrapper.execute
        DataTO srcData = reconcileCommand.getSrcData();
        DataTO destData = reconcileCommand.getDestData();
        PrimaryDataStoreTO srcDataStore = (PrimaryDataStoreTO) srcData.getDataStore();
        PrimaryDataStoreTO destDataStore = (PrimaryDataStoreTO) destData.getDataStore();

        VolumeOnStorageTO volumeOnSource = libvirtComputingResource.getVolumeOnStorage(srcDataStore, srcData.getPath());
        VolumeOnStorageTO volumeOnDestination = libvirtComputingResource.getVolumeOnStorage(destDataStore, destData.getPath());

        ReconcileMigrateVolumeAnswer answer = new ReconcileMigrateVolumeAnswer(volumeOnSource, volumeOnDestination);
        String vmName = reconcileCommand.getVmName();
        if (vmName != null) {
            try {
                LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
                Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
                List<String> disks = getVmDiskPaths(libvirtComputingResource.getDisks(conn, vmName));
                answer.setVmName(vmName);
                answer.setVmDiskPaths(disks);
            } catch (LibvirtException e) {
                logger.error(String.format("Unable to get disks for %s due to %s", vmName, e.getMessage()));
            }
        }

        return answer;
    }
}
