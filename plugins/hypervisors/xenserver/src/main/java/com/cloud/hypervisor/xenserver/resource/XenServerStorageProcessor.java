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
package com.cloud.hypervisor.xenserver.resource;

import static com.cloud.utils.ReflectUtil.flattenProperties;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachAnswer;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectAnswer;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.command.ResignatureAnswer;
import org.apache.cloudstack.storage.command.ResignatureCommand;
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase.SRType;
import com.cloud.hypervisor.xenserver.resource.wrapper.xenbase.XenServerUtilitiesHelper;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.S3.ClientOptions;
import com.google.common.annotations.VisibleForTesting;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

public class XenServerStorageProcessor implements StorageProcessor {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageProcessor.class);
    protected CitrixResourceBase hypervisorResource;
    protected String BaseMountPointOnHost = "/var/run/cloud_mount";

    protected final static String BASE_MOUNT_POINT_ON_REMOTE = "/var/cloud_mount/";

    public XenServerStorageProcessor(final CitrixResourceBase resource) {
        hypervisorResource = resource;
    }

    // if the source SR needs to be attached to, do so
    // take a snapshot of the source VDI (on the source SR)
    // create an iSCSI SR based on the new back-end volume
    // copy the snapshot to the new SR
    // delete the snapshot
    // detach the new SR
    // if we needed to perform an attach to the source SR, detach from it
    @Override
    public SnapshotAndCopyAnswer snapshotAndCopy(final SnapshotAndCopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();

        try {
            SR sourceSr = null;

            final Map<String, String> sourceDetails = cmd.getSourceDetails();

            if (sourceDetails != null && sourceDetails.keySet().size() > 0) {
                final String iScsiName = sourceDetails.get(DiskTO.IQN);
                final String storageHost = sourceDetails.get(DiskTO.STORAGE_HOST);
                final String chapInitiatorUsername = sourceDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
                final String chapInitiatorSecret = sourceDetails.get(DiskTO.CHAP_INITIATOR_SECRET);

                sourceSr = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, false);
            }

            final VDI vdiToSnapshot = VDI.getByUuid(conn, cmd.getUuidOfSourceVdi());

            final VDI vdiSnapshot = vdiToSnapshot.snapshot(conn, new HashMap<String, String>());

            final Map<String, String> destDetails = cmd.getDestDetails();

            final String iScsiName = destDetails.get(DiskTO.IQN);
            final String storageHost = destDetails.get(DiskTO.STORAGE_HOST);
            final String chapInitiatorUsername = destDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
            final String chapInitiatorSecret = destDetails.get(DiskTO.CHAP_INITIATOR_SECRET);

            final SR newSr = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, false);

            final VDI vdiCopy = vdiSnapshot.copy(conn, newSr);

            final String vdiUuid = vdiCopy.getUuid(conn);

            vdiSnapshot.destroy(conn);

            if (sourceSr != null) {
                hypervisorResource.removeSR(conn, sourceSr);
            }

            hypervisorResource.removeSR(conn, newSr);

            final SnapshotAndCopyAnswer snapshotAndCopyAnswer = new SnapshotAndCopyAnswer();

            snapshotAndCopyAnswer.setPath(vdiUuid);

            return snapshotAndCopyAnswer;
        }
        catch (final Exception ex) {
            s_logger.warn("Failed to take and copy snapshot: " + ex.toString(), ex);

            return new SnapshotAndCopyAnswer(ex.getMessage());
        }
    }

    @Override
    public ResignatureAnswer resignature(final ResignatureCommand cmd) {
        SR newSr = null;

        final Connection conn = hypervisorResource.getConnection();

        try {
            final Map<String, String> details = cmd.getDetails();

            final String iScsiName = details.get(DiskTO.IQN);
            final String storageHost = details.get(DiskTO.STORAGE_HOST);
            final String chapInitiatorUsername = details.get(DiskTO.CHAP_INITIATOR_USERNAME);
            final String chapInitiatorSecret = details.get(DiskTO.CHAP_INITIATOR_SECRET);

            newSr = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, true, false);

            Set<VDI> vdis = newSr.getVDIs(conn);

            if (vdis.size() != 1) {
                throw new RuntimeException("There were " + vdis.size() + " VDIs in the SR.");
            }

            VDI vdi = vdis.iterator().next();

            final ResignatureAnswer resignatureAnswer = new ResignatureAnswer();

            resignatureAnswer.setSize(vdi.getVirtualSize(conn));
            resignatureAnswer.setPath(vdi.getUuid(conn));
            resignatureAnswer.setFormat(ImageFormat.VHD);

            return resignatureAnswer;
        }
        catch (final Exception ex) {
            s_logger.warn("Failed to resignature: " + ex.toString(), ex);

            return new ResignatureAnswer(ex.getMessage());
        }
        finally {
            if (newSr != null) {
                hypervisorResource.removeSR(conn, newSr);
            }
        }
    }

    @Override
    public Answer handleDownloadTemplateToPrimaryStorage(DirectDownloadCommand cmd) {
        //Not implemented for Xen
        return null;
    }

    @Override
    public AttachAnswer attachIso(final AttachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final DataTO data = disk.getData();
        final DataStoreTO store = data.getDataStore();

        String isoURL = null;
        if (store == null) {
            final TemplateObjectTO iso = (TemplateObjectTO) disk.getData();
            isoURL = iso.getName();
        } else {
            if (!(store instanceof NfsTO)) {
                s_logger.debug("Can't attach a iso which is not created on nfs: ");
                return new AttachAnswer("Can't attach a iso which is not created on nfs: ");
            }
            final NfsTO nfsStore = (NfsTO) store;
            isoURL = nfsStore.getUrl() + nfsStore.getPathSeparator() + data.getPath();
        }

        final String vmName = cmd.getVmName();
        try {
            final Connection conn = hypervisorResource.getConnection();

            VBD isoVBD = null;

            // Find the VM
            final VM vm = hypervisorResource.getVM(conn, vmName);
            // Find the ISO VDI
            final VDI isoVDI = hypervisorResource.getIsoVDIByURL(conn, vmName, isoURL);

            // Find the VM's CD-ROM VBD
            final Set<VBD> vbds = vm.getVBDs(conn);
            for (final VBD vbd : vbds) {
                final String userDevice = vbd.getUserdevice(conn);
                final Types.VbdType type = vbd.getType(conn);

                if (userDevice.equals("3") && type == Types.VbdType.CD) {
                    isoVBD = vbd;
                    break;
                }
            }

            if (isoVBD == null) {
                throw new CloudRuntimeException("Unable to find CD-ROM VBD for VM: " + vmName);
            } else {
                // If an ISO is already inserted, eject it
                if (!isoVBD.getEmpty(conn)) {
                    isoVBD.eject(conn);
                }

                // Insert the new ISO
                isoVBD.insert(conn, isoVDI);
            }

            return new AttachAnswer(disk);

        } catch (final XenAPIException e) {
            s_logger.warn("Failed to attach iso" + ": " + e.toString(), e);
            return new AttachAnswer(e.toString());
        } catch (final Exception e) {
            s_logger.warn("Failed to attach iso" + ": " + e.toString(), e);
            return new AttachAnswer(e.toString());
        }
    }

    @Override
    public AttachAnswer attachVolume(final AttachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final DataTO data = disk.getData();
        try {
            final String vmName = cmd.getVmName();
            final String vdiNameLabel = vmName + "-DATA";

            final Connection conn = hypervisorResource.getConnection();
            VM vm = null;

            boolean vmNotRunning = true;

            try {
                vm = hypervisorResource.getVM(conn, vmName);

                final VM.Record vmr = vm.getRecord(conn);

                vmNotRunning = vmr.powerState != VmPowerState.RUNNING;
            } catch (final CloudRuntimeException ex) {
            }

            final Map<String, String> details = disk.getDetails();
            final boolean isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));

            // if the VM is not running and we're not dealing with managed storage, just return success (nothing to do here)
            // this should probably never actually happen
            if (vmNotRunning && !isManaged) {
                return new AttachAnswer(disk);
            }

            VDI vdi;

            if (isManaged) {
                vdi = hypervisorResource.prepareManagedStorage(conn, details, data.getPath(), vdiNameLabel);

                if (vmNotRunning) {
                    final DiskTO newDisk = new DiskTO(disk.getData(), disk.getDiskSeq(), vdi.getUuid(conn), disk.getType());

                    return new AttachAnswer(newDisk);
                }
            } else {
                vdi = hypervisorResource.mount(conn, null, null, data.getPath());
            }

            hypervisorResource.destroyUnattachedVBD(conn, vm);

            final VBD.Record vbdr = new VBD.Record();

            vbdr.VM = vm;
            vbdr.VDI = vdi;
            vbdr.bootable = false;
            vbdr.userdevice = "autodetect";

            final Long deviceId = disk.getDiskSeq();

            if (deviceId != null && !hypervisorResource.isDeviceUsed(conn, vm, deviceId)) {
                vbdr.userdevice = deviceId.toString();
            }

            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            vbdr.unpluggable = true;

            final VBD vbd = VBD.create(conn, vbdr);

            // Attach the VBD to the VM
            try {
                vbd.plug(conn);
            } catch (final Exception e) {
                vbd.destroy(conn);
                throw e;
            }

            // Update the VDI's label to include the VM name
            vdi.setNameLabel(conn, vdiNameLabel);

            final DiskTO newDisk = new DiskTO(disk.getData(), Long.parseLong(vbd.getUserdevice(conn)), vdi.getUuid(conn), disk.getType());

            return new AttachAnswer(newDisk);
        } catch (final Exception e) {
            final String msg = "Failed to attach volume for uuid: " + data.getPath() + " due to "  + e.toString();

            s_logger.warn(msg, e);

            return new AttachAnswer(msg);
        }
    }

    @Override
    public Answer dettachIso(final DettachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final DataTO data = disk.getData();
        final DataStoreTO store = data.getDataStore();

        String isoURL = null;
        if (store == null) {
            final TemplateObjectTO iso = (TemplateObjectTO) disk.getData();
            isoURL = iso.getName();
        } else {
            if (!(store instanceof NfsTO)) {
                s_logger.debug("Can't detach a iso which is not created on nfs: ");
                return new AttachAnswer("Can't detach a iso which is not created on nfs: ");
            }
            final NfsTO nfsStore = (NfsTO) store;
            isoURL = nfsStore.getUrl() + nfsStore.getPathSeparator() + data.getPath();
        }

        try {
            final Connection conn = hypervisorResource.getConnection();
            // Find the VM
            final VM vm = hypervisorResource.getVM(conn, cmd.getVmName());
            final String vmUUID = vm.getUuid(conn);

            // Find the ISO VDI
            final VDI isoVDI = hypervisorResource.getIsoVDIByURL(conn, cmd.getVmName(), isoURL);

            final SR sr = isoVDI.getSR(conn);

            // Look up all VBDs for this VDI
            final Set<VBD> vbds = isoVDI.getVBDs(conn);

            // Iterate through VBDs, and if the VBD belongs the VM, eject
            // the ISO from it
            for (final VBD vbd : vbds) {
                final VM vbdVM = vbd.getVM(conn);
                final String vbdVmUUID = vbdVM.getUuid(conn);

                if (vbdVmUUID.equals(vmUUID)) {
                    // If an ISO is already inserted, eject it
                    if (!vbd.getEmpty(conn)) {
                        vbd.eject(conn);
                    }
                    break;
                }
            }

            if (!XenServerUtilitiesHelper.isXenServerToolsSR(sr.getNameLabel(conn))) {
                hypervisorResource.removeSR(conn, sr);
            }

            return new DettachAnswer(disk);
        } catch (final XenAPIException e) {
            final String msg = "Failed to detach volume" + " for uuid: " + data.getPath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new DettachAnswer(msg);
        } catch (final Exception e) {
            final String msg = "Failed to detach volume" + " for uuid: " + data.getPath() + "  due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new DettachAnswer(msg);
        }
    }

    @Override
    public Answer dettachVolume(final DettachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final DataTO data = disk.getData();

        try {
            final Connection conn = hypervisorResource.getConnection();

            final String vmName = cmd.getVmName();
            VM vm = null;

            boolean vmNotRunning = true;

            try {
                vm = hypervisorResource.getVM(conn, vmName);

                final VM.Record vmr = vm.getRecord(conn);

                vmNotRunning = vmr.powerState != VmPowerState.RUNNING;
            } catch (final CloudRuntimeException ex) {
            }

            // if the VM is not running and we're not dealing with managed storage, just return success (nothing to do here)
            // this should probably never actually happen
            if (vmNotRunning && !cmd.isManaged()) {
                return new DettachAnswer(disk);
            }

            if (!vmNotRunning) {
                final VDI vdi = hypervisorResource.mount(conn, null, null, data.getPath());

                // Look up all VBDs for this VDI
                final Set<VBD> vbds = vdi.getVBDs(conn);

                // Detach each VBD from its VM, and then destroy it
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdr = vbd.getRecord(conn);

                    if (vbdr.currentlyAttached) {
                        vbd.unplug(conn);
                    }

                    vbd.destroy(conn);
                }

                hypervisorResource.umount(conn, vdi);
            }

            if (cmd.isManaged()) {
                hypervisorResource.handleSrAndVdiDetach(cmd.get_iScsiName(), conn);
            }

            return new DettachAnswer(disk);
        } catch (final Exception e) {
            s_logger.warn("Failed dettach volume: " + data.getPath());
            return new DettachAnswer("Failed dettach volume: " + data.getPath() + ", due to " + e.toString());
        }
    }

    protected VDI createVdi(final Connection conn, final String vdiName, final SR sr, final long size) throws BadServerResponse, XenAPIException, XmlRpcException {
        final VDI.Record vdir = new VDI.Record();
        vdir.nameLabel = vdiName;
        vdir.SR = sr;
        vdir.type = Types.VdiType.USER;

        vdir.virtualSize = size;
        final VDI vdi = VDI.create(conn, vdir);
        return vdi;
    }

    protected void deleteVDI(final Connection conn, final VDI vdi) throws BadServerResponse, XenAPIException, XmlRpcException {
        vdi.destroy(conn);
    }

    @Override
    public Answer createSnapshot(final CreateObjectCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final SnapshotObjectTO snapshotTO = (SnapshotObjectTO) cmd.getData();
        final long snapshotId = snapshotTO.getId();
        final String snapshotName = snapshotTO.getName();
        String details = "create snapshot operation Failed for snapshotId: " + snapshotId;
        String snapshotUUID = null;

        try {
            final String volumeUUID = snapshotTO.getVolume().getPath();
            final VDI volume = VDI.getByUuid(conn, volumeUUID);

            final VDI snapshot = volume.snapshot(conn, new HashMap<String, String>());

            if (snapshotName != null) {
                snapshot.setNameLabel(conn, snapshotName);
            }

            snapshotUUID = snapshot.getUuid(conn);
            final String preSnapshotUUID = snapshotTO.getParentSnapshotPath();
            //check if it is a empty snapshot
            if (preSnapshotUUID != null) {
                final SR sr = volume.getSR(conn);
                final String srUUID = sr.getUuid(conn);
                final String type = sr.getType(conn);
                final Boolean isISCSI = IsISCSI(type);
                final String snapshotParentUUID = getVhdParent(conn, srUUID, snapshotUUID, isISCSI);

                try {
                    final String preSnapshotParentUUID = getVhdParent(conn, srUUID, preSnapshotUUID, isISCSI);
                    if (snapshotParentUUID != null && snapshotParentUUID.equals(preSnapshotParentUUID)) {
                        // this is empty snapshot, remove it
                        snapshot.destroy(conn);
                        snapshotUUID = preSnapshotUUID;
                    }
                } catch (final Exception e) {
                    s_logger.debug("Failed to get parent snapshot", e);
                }
            }
            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotUUID);
            return new CreateObjectAnswer(newSnapshot);
        } catch (final XenAPIException e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        } catch (final Exception e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        }

        return new CreateObjectAnswer(details);
    }

    @Override
    public Answer deleteVolume(final DeleteCommand cmd) {
        final DataTO volume = cmd.getData();
        final Connection conn = hypervisorResource.getConnection();
        String errorMsg = null;
        try {
            final VDI vdi = VDI.getByUuid(conn, volume.getPath());
            for(VDI svdi : vdi.getSnapshots(conn)) {
                deleteVDI(conn, svdi);
            }
            deleteVDI(conn, vdi);
            return new Answer(null);
        } catch (final BadServerResponse e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (final XenAPIException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (final XmlRpcException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        }
        return new Answer(null, false, errorMsg);
    }

    protected boolean IsISCSI(final String type) {
        return SRType.LVMOHBA.equals(type) || SRType.LVMOISCSI.equals(type) || SRType.LVM.equals(type);
    }

    private String copy_vhd_from_secondarystorage(final Connection conn, final String mountpoint, final String sruuid, final int wait) {
        final String nameLabel = "cloud-" + UUID.randomUUID().toString();
        final String results =
                hypervisorResource.callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage", wait, "mountpoint", mountpoint, "sruuid", sruuid, "namelabel",
                        nameLabel);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_from_secondarystorage return null";
        } else {
            final String[] tmp = results.split("#");
            final String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        final String source = mountpoint.substring(mountpoint.lastIndexOf('/') + 1);
        if (hypervisorResource.killCopyProcess(conn, source)) {
            destroyVDIbyNameLabel(conn, nameLabel);
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    private void destroyVDIbyNameLabel(final Connection conn, final String nameLabel) {
        try {
            final Set<VDI> vdis = VDI.getByNameLabel(conn, nameLabel);
            if (vdis.size() != 1) {
                s_logger.warn("destoryVDIbyNameLabel failed due to there are " + vdis.size() + " VDIs with name " + nameLabel);
                return;
            }
            for (final VDI vdi : vdis) {
                try {
                    vdi.destroy(conn);
                } catch (final Exception e) {
                }
            }
        } catch (final Exception e) {
        }
    }

    protected VDI getVDIbyUuid(final Connection conn, final String uuid) {
        try {
            return VDI.getByUuid(conn, uuid);
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " :VDI getByUuid for uuid: " + uuid + " failed due to " + e.toString();
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected String getVhdParent(final Connection conn, final String primaryStorageSRUuid, final String snapshotUuid, final Boolean isISCSI) {
        final String parentUuid =
                hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid, "snapshotUuid", snapshotUuid,
                        "isISCSI", isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(final CopyCommand cmd) {
        final DataTO srcDataTo = cmd.getSrcTO();
        final DataTO destDataTo = cmd.getDestTO();
        final int wait = cmd.getWait();
        final DataStoreTO srcDataStoreTo = srcDataTo.getDataStore();
        final Connection conn = hypervisorResource.getConnection();
        SR sr = null;
        boolean removeSrAfterCopy = false;

        try {
            if (srcDataStoreTo instanceof NfsTO && srcDataTo.getObjectType() == DataObjectType.TEMPLATE) {
                final NfsTO srcImageStore = (NfsTO) srcDataStoreTo;
                final TemplateObjectTO srcTemplateObjectTo = (TemplateObjectTO) srcDataTo;
                final String storeUrl = srcImageStore.getUrl();
                final URI uri = new URI(storeUrl);
                final String tmplPath = uri.getHost() + ":" + uri.getPath() + "/" + srcDataTo.getPath();
                final DataStoreTO destDataStoreTo = destDataTo.getDataStore();

                boolean managed = false;
                String storageHost = null;
                String managedStoragePoolName = null;
                String managedStoragePoolRootVolumeName = null;
                String managedStoragePoolRootVolumeSize = null;
                String chapInitiatorUsername = null;
                String chapInitiatorSecret = null;

                if (destDataStoreTo instanceof PrimaryDataStoreTO) {
                    final PrimaryDataStoreTO destPrimaryDataStoreTo = (PrimaryDataStoreTO)destDataStoreTo;

                    final Map<String, String> details = destPrimaryDataStoreTo.getDetails();

                    if (details != null) {
                        managed = Boolean.parseBoolean(details.get(PrimaryDataStoreTO.MANAGED));

                        if (managed) {
                            storageHost = details.get(PrimaryDataStoreTO.STORAGE_HOST);
                            managedStoragePoolName = details.get(PrimaryDataStoreTO.MANAGED_STORE_TARGET);
                            managedStoragePoolRootVolumeName = details.get(PrimaryDataStoreTO.MANAGED_STORE_TARGET_ROOT_VOLUME);
                            managedStoragePoolRootVolumeSize = details.get(PrimaryDataStoreTO.VOLUME_SIZE);
                            chapInitiatorUsername = details.get(PrimaryDataStoreTO.CHAP_INITIATOR_USERNAME);
                            chapInitiatorSecret = details.get(PrimaryDataStoreTO.CHAP_INITIATOR_SECRET);
                            removeSrAfterCopy = Boolean.parseBoolean(details.get(PrimaryDataStoreTO.REMOVE_AFTER_COPY));
                        }
                    }
                }

                if (managed) {
                    final Map<String, String> details = new HashMap<String, String>();

                    details.put(DiskTO.STORAGE_HOST, storageHost);
                    details.put(DiskTO.IQN, managedStoragePoolName);
                    details.put(DiskTO.VOLUME_SIZE, managedStoragePoolRootVolumeSize);
                    details.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInitiatorUsername);
                    details.put(DiskTO.CHAP_INITIATOR_SECRET, chapInitiatorSecret);

                    sr = hypervisorResource.prepareManagedSr(conn, details);
                } else {
                    final String srName = destDataStoreTo.getUuid();
                    final Set<SR> srs = SR.getByNameLabel(conn, srName);

                    if (srs.size() != 1) {
                        final String msg = "There are " + srs.size() + " SRs with same name: " + srName;

                        s_logger.warn(msg);

                        return new CopyCmdAnswer(msg);
                    } else {
                        sr = srs.iterator().next();
                    }
                }

                final String srUuid = sr.getUuid(conn);
                final String tmplUuid = copy_vhd_from_secondarystorage(conn, tmplPath, srUuid, wait);
                final VDI tmplVdi = getVDIbyUuid(conn, tmplUuid);

                final String uuidToReturn;
                final Long physicalSize = tmplVdi.getPhysicalUtilisation(conn);

                if (managed) {
                    uuidToReturn = tmplUuid;

                    tmplVdi.setNameLabel(conn, managedStoragePoolRootVolumeName);
                } else {
                    final VDI snapshotVdi = tmplVdi.snapshot(conn, new HashMap<String, String>());

                    uuidToReturn = snapshotVdi.getUuid(conn);

                    snapshotVdi.setNameLabel(conn, "Template " + srcTemplateObjectTo.getName());

                    tmplVdi.destroy(conn);
                }

                sr.scan(conn);

                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {
                }

                final TemplateObjectTO newVol = new TemplateObjectTO();

                newVol.setUuid(uuidToReturn);
                newVol.setPath(uuidToReturn);

                if (physicalSize != null) {
                    newVol.setSize(physicalSize);
                }

                newVol.setFormat(ImageFormat.VHD);

                return new CopyCmdAnswer(newVol);
            }
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " for template + " + " due to " + e.toString();

            s_logger.warn(msg, e);

            return new CopyCmdAnswer(msg);
        }
        finally {
            if (removeSrAfterCopy && sr != null) {
                hypervisorResource.removeSR(conn, sr);
            }
        }

        return new CopyCmdAnswer("not implemented yet");
    }

    @Override
    public Answer createVolume(final CreateObjectCommand cmd) {
        final DataTO data = cmd.getData();
        final VolumeObjectTO volume = (VolumeObjectTO) data;

        try {
            final Connection conn = hypervisorResource.getConnection();
            final SR poolSr = hypervisorResource.getStorageRepository(conn, data.getDataStore().getUuid());
            VDI.Record vdir = new VDI.Record();
            vdir.nameLabel = volume.getName();
            vdir.SR = poolSr;
            vdir.type = Types.VdiType.USER;

            vdir.virtualSize = volume.getSize();
            VDI vdi;

            vdi = VDI.create(conn, vdir);
            vdir = vdi.getRecord(conn);
            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(vdir.nameLabel);
            newVol.setSize(vdir.virtualSize);
            newVol.setPath(vdir.uuid);

            return new CreateObjectAnswer(newVol);
        } catch (final Exception e) {
            s_logger.debug("create volume failed: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final VolumeObjectTO volume = (VolumeObjectTO) destData;
        VDI vdi = null;
        try {
            VDI tmpltvdi = null;

            tmpltvdi = getVDIbyUuid(conn, srcData.getPath());
            vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
            Long virtualSize  = vdi.getVirtualSize(conn);
            if (volume.getSize() > virtualSize) {
                s_logger.debug("Overriding provided template's size with new size " + volume.getSize() + " for volume: " + volume.getName());
                vdi.resize(conn, volume.getSize());
            } else {
                s_logger.debug("Using templates disk size of " + virtualSize + " for volume: " + volume.getName() + " since size passed was " + volume.getSize());
            }
            vdi.setNameLabel(conn, volume.getName());

            VDI.Record vdir;
            vdir = vdi.getRecord(conn);
            s_logger.debug("Succesfully created VDI: Uuid = " + vdir.uuid);

            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(vdir.nameLabel);
            newVol.setSize(vdir.virtualSize);
            newVol.setPath(vdir.uuid);

            return new CopyCmdAnswer(newVol);
        } catch (final Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + destData + "; Disk: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWait();
        final VolumeObjectTO srcVolume = (VolumeObjectTO) srcData;
        final VolumeObjectTO destVolume = (VolumeObjectTO) destData;
        final DataStoreTO srcStore = srcVolume.getDataStore();

        if (srcStore instanceof NfsTO) {
            final NfsTO nfsStore = (NfsTO) srcStore;
            try {
                final SR primaryStoragePool = hypervisorResource.getStorageRepository(conn, destVolume.getDataStore().getUuid());
                final String srUuid = primaryStoragePool.getUuid(conn);
                final URI uri = new URI(nfsStore.getUrl());
                final String volumePath = uri.getHost() + ":" + uri.getPath() + nfsStore.getPathSeparator() + srcVolume.getPath();
                final String uuid = copy_vhd_from_secondarystorage(conn, volumePath, srUuid, wait);
                final VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(uuid);
                newVol.setSize(srcVolume.getSize());

                return new CopyCmdAnswer(newVol);
            } catch (final Exception e) {
                final String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
                s_logger.warn(msg, e);
                return new CopyCmdAnswer(e.toString());
            }
        }

        s_logger.debug("unsupported protocol");
        return new CopyCmdAnswer("unsupported protocol");
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final VolumeObjectTO srcVolume = (VolumeObjectTO) cmd.getSrcTO();
        final VolumeObjectTO destVolume = (VolumeObjectTO) cmd.getDestTO();
        final int wait = cmd.getWait();
        final DataStoreTO destStore = destVolume.getDataStore();

        if (destStore instanceof NfsTO) {
            SR secondaryStorage = null;
            try {
                final NfsTO nfsStore = (NfsTO) destStore;
                final URI uri = new URI(nfsStore.getUrl());
                // Create the volume folder
                if (!hypervisorResource.createSecondaryStorageFolder(conn, uri.getHost() + ":" + uri.getPath(), destVolume.getPath())) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }

                // Create a SR for the volume UUID folder
                secondaryStorage = hypervisorResource.createNfsSRbyURI(conn, new URI(nfsStore.getUrl() + nfsStore.getPathSeparator() + destVolume.getPath()), false);
                // Look up the volume on the source primary storage pool
                final VDI srcVdi = getVDIbyUuid(conn, srcVolume.getPath());
                // Copy the volume to secondary storage
                final VDI destVdi = hypervisorResource.cloudVDIcopy(conn, srcVdi, secondaryStorage, wait);
                final String destVolumeUUID = destVdi.getUuid(conn);

                final VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(destVolume.getPath() + nfsStore.getPathSeparator() + destVolumeUUID + ".vhd");
                newVol.setSize(srcVolume.getSize());
                return new CopyCmdAnswer(newVol);
            } catch (final Exception e) {
                s_logger.debug("Failed to copy volume to secondary: " + e.toString());
                return new CopyCmdAnswer("Failed to copy volume to secondary: " + e.toString());
            } finally {
                hypervisorResource.removeSR(conn, secondaryStorage);
            }
        }
        return new CopyCmdAnswer("unsupported protocol");
    }

    private boolean swiftUpload(final Connection conn, final SwiftTO swift, final String container, final String ldir, final String lfilename, final Boolean isISCSI,
            final int wait) {

        List<String> params = getSwiftParams(swift, container, ldir, lfilename, isISCSI);

        try {
            String result = hypervisorResource.callHostPluginAsync(conn, "swiftxenserver", "swift", wait, params.toArray(new String[params.size()]));
            return BooleanUtils.toBoolean(result);
        } catch (final Exception e) {
            s_logger.warn("swift upload failed due to " + e.toString(), e);
        }
        return false;
    }

    @VisibleForTesting
    List<String> getSwiftParams(SwiftTO swift, String container, String ldir, String lfilename, Boolean isISCSI) {
        // ORDER IS IMPORTANT
        List<String> params = new ArrayList<>();

        //operation
        params.add("op");
        params.add("upload");

        //auth
        params.add("url");
        params.add(swift.getUrl());
        params.add("account");
        params.add(swift.getAccount());
        params.add("username");
        params.add(swift.getUserName());
        params.add("key");
        params.add(swift.getKey());

        // object info
        params.add("container");
        params.add(container);
        params.add("ldir");
        params.add(ldir);
        params.add("lfilename");
        params.add(lfilename);
        params.add("isISCSI");
        params.add(isISCSI.toString());

        if (swift.getStoragePolicy() != null) {
            params.add("storagepolicy");
            params.add(swift.getStoragePolicy());
        }

        return params;
    }

    protected String deleteSnapshotBackup(final Connection conn, final String localMountPoint, final String path, final String secondaryStorageMountPath, final String backupUUID) {

        // If anybody modifies the formatting below again, I'll skin them
        final String result =
                hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "deleteSnapshotBackup", "backupUUID", backupUUID, "path", path, "secondaryStorageMountPath",
                        secondaryStorageMountPath, "localMountPoint", localMountPoint);

        return result;
    }

    protected String swiftBackupSnapshot(final Connection conn, final SwiftTO swift, final String srUuid, final String snapshotUuid, final String container, final Boolean isISCSI,
            final int wait) {
        String lfilename;
        String ldir;
        if (isISCSI) {
            ldir = "/dev/VG_XenStorage-" + srUuid;
            lfilename = "VHD-" + snapshotUuid;
        } else {
            ldir = "/var/run/sr-mount/" + srUuid;
            lfilename = snapshotUuid + ".vhd";
        }
        swiftUpload(conn, swift, container, ldir, lfilename, isISCSI, wait);
        return lfilename;
    }

    protected String backupSnapshotToS3(final Connection connection, final S3TO s3, final String srUuid, final String folder, final String snapshotUuid,
            final Boolean iSCSIFlag, final int wait) {

        final String filename = iSCSIFlag ? "VHD-" + snapshotUuid : snapshotUuid + ".vhd";
        final String dir = (iSCSIFlag ? "/dev/VG_XenStorage-" : "/var/run/sr-mount/") + srUuid;
        final String key = folder + "/" + filename; // String.format("/snapshots/%1$s", snapshotUuid);

        try {

            final List<String> parameters = newArrayList(flattenProperties(s3, ClientOptions.class));
            // https workaround for Introspector bug that does not
            // recognize Boolean accessor methods ...

            parameters.addAll(Arrays.asList("operation", "put", "filename", dir + "/" + filename, "iSCSIFlag", iSCSIFlag.toString(), "bucket", s3.getBucketName(), "key",
                    key, "https", s3.isHttps() != null ? s3.isHttps().toString() : "null", "maxSingleUploadSizeInBytes", String.valueOf(s3.getMaxSingleUploadSizeInBytes())));
            final String result = hypervisorResource.callHostPluginAsync(connection, "s3xenserver", "s3", wait, parameters.toArray(new String[parameters.size()]));

            if (result != null && result.equals("true")) {
                return key;
            }
            return null;

        } catch (final Exception e) {
            s_logger.error(String.format("S3 upload failed of snapshot %1$s due to %2$s.", snapshotUuid, e.toString()), e);
        }

        return null;

    }

    private Long getSnapshotSize(final Connection conn, final String primaryStorageSRUuid, final String snapshotUuid, final Boolean isISCSI, final int wait) {
        final String physicalSize = hypervisorResource.callHostPluginAsync(conn, "vmopsSnapshot", "getSnapshotSize", wait,
                "primaryStorageSRUuid", primaryStorageSRUuid, "snapshotUuid", snapshotUuid, "isISCSI", isISCSI.toString());
        if (physicalSize == null || physicalSize.isEmpty()) {
            return (long) 0;
        } else {
            return Long.parseLong(physicalSize);
        }
    }

    private String backupSnapshot(final Connection conn, final String primaryStorageSRUuid, final String localMountPoint, final String path, final String secondaryStorageMountPath,
            final String snapshotUuid, String prevBackupUuid, final Boolean isISCSI, final int wait) {
        String backupSnapshotUuid = null;

        if (prevBackupUuid == null) {
            prevBackupUuid = "";
        }

        // Each argument is put in a separate line for readability.
        // Using more lines does not harm the environment.
        final String backupUuid = UUID.randomUUID().toString();
        final String results =
                hypervisorResource.callHostPluginAsync(conn, "vmopsSnapshot", "backupSnapshot", wait, "primaryStorageSRUuid", primaryStorageSRUuid, "path", path,
                        "secondaryStorageMountPath", secondaryStorageMountPath, "snapshotUuid", snapshotUuid, "prevBackupUuid", prevBackupUuid, "backupUuid", backupUuid,
                        "isISCSI", isISCSI.toString(), "localMountPoint", localMountPoint);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg =
                    "Could not copy backupUuid: " + backupSnapshotUuid + " from primary storage " + primaryStorageSRUuid + " to secondary storage " +
                            secondaryStorageMountPath + " due to null";
        } else {

            final String[] tmp = results.split("#");
            final String status = tmp[0];
            backupSnapshotUuid = tmp[1];
            // status == "1" if and only if backupSnapshotUuid != null
            // So we don't rely on status value but return backupSnapshotUuid as an
            // indicator of success.
            if (status != null && status.equalsIgnoreCase("1") && backupSnapshotUuid != null) {
                s_logger.debug("Successfully copied backupUuid: " + backupSnapshotUuid + " to secondary storage");
                return results;
            } else {
                errMsg =
                        "Could not copy backupUuid: " + backupSnapshotUuid + " from primary storage " + primaryStorageSRUuid + " to secondary storage " +
                                secondaryStorageMountPath + " due to " + tmp[1];
            }
        }
        final String source = backupUuid + ".vhd";
        hypervisorResource.killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    protected boolean destroySnapshotOnPrimaryStorageExceptThis(final Connection conn, final String volumeUuid, final String avoidSnapshotUuid) {
        try {
            final VDI volume = getVDIbyUuid(conn, volumeUuid);
            if (volume == null) {
                throw new InternalErrorException("Could not destroy snapshot on volume " + volumeUuid + " due to can not find it");
            }
            // To avoid deleting snapshots which are still waiting in queue to get backed up.
            VDI avoidSnapshot = getVDIbyUuid(conn, avoidSnapshotUuid);
            if (avoidSnapshot == null) {
                throw new InternalErrorException("Could not find current snapshot " + avoidSnapshotUuid);
            }
            final Set<VDI> snapshots = volume.getSnapshots(conn);
            for (final VDI snapshot : snapshots) {
                try {
                    if (!snapshot.getUuid(conn).equals(avoidSnapshotUuid) && snapshot.getSnapshotTime(conn).before(avoidSnapshot.getSnapshotTime(conn)) && snapshot.getVBDs(conn).isEmpty()) {
                        snapshot.destroy(conn);
                    }
                } catch (final Exception e) {
                    final String msg = "Destroying snapshot: " + snapshot + " on primary storage failed due to " + e.toString();
                    s_logger.warn(msg, e);
                }
            }
            s_logger.debug("Successfully destroyed snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid);
            return true;
        } catch (final XenAPIException e) {
            final String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (final Exception e) {
            final String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot " + avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }

        return false;
    }

    protected boolean destroySnapshotOnPrimaryStorage(final Connection conn, final String lastSnapshotUuid) {
        try {
            final VDI snapshot = getVDIbyUuid(conn, lastSnapshotUuid);
            if (snapshot == null) {
                // since this is just used to cleanup leftover bad snapshots, no need to throw exception
                s_logger.warn("Could not destroy snapshot " + lastSnapshotUuid + " due to can not find it");
                return false;
            }
            snapshot.destroy(conn);
            return true;
        } catch (final XenAPIException e) {
            final String msg = "Destroying snapshot: " + lastSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (final Exception e) {
            final String msg = "Destroying snapshot: " + lastSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }
        return false;
    }

    @Override
    public Answer backupSnapshot(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final DataTO cacheData = cmd.getCacheTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWait();
        final String primaryStorageNameLabel = srcData.getDataStore().getUuid();
        String secondaryStorageUrl = null;
        NfsTO cacheStore = null;
        String destPath = null;
        if (cacheData != null) {
            cacheStore = (NfsTO) cacheData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = cacheData.getPath();
        } else {
            cacheStore = (NfsTO) destData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = destData.getPath();
        }

        final SnapshotObjectTO snapshotTO = (SnapshotObjectTO) srcData;
        final SnapshotObjectTO snapshotOnImage = (SnapshotObjectTO) destData;
        final String snapshotUuid = snapshotTO.getPath();
        final String volumeUuid = snapshotTO.getVolume().getPath();

        final String prevBackupUuid = snapshotOnImage.getParentSnapshotPath();
        final String prevSnapshotUuid = snapshotTO.getParentSnapshotPath();

        // By default assume failure
        String details = null;
        String snapshotBackupUuid = null;
        Long physicalSize = null;
        final Map<String, String> options = cmd.getOptions();
        boolean fullbackup = Boolean.parseBoolean(options.get("fullSnapshot"));
        boolean result = false;
        try {
            final SR primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " +
                        primaryStorageNameLabel);
            }
            final String psUuid = primaryStorageSR.getUuid(conn);
            final Boolean isISCSI = IsISCSI(primaryStorageSR.getType(conn));

            final VDI snapshotVdi = getVDIbyUuid(conn, snapshotUuid);
            String snapshotPaUuid = null;

            if (prevSnapshotUuid != null && !fullbackup) {
                try {
                    snapshotPaUuid = getVhdParent(conn, psUuid, snapshotUuid, isISCSI);
                    if (snapshotPaUuid != null) {
                        final String snashotPaPaPaUuid = getVhdParent(conn, psUuid, snapshotPaUuid, isISCSI);
                        final String prevSnashotPaUuid = getVhdParent(conn, psUuid, prevSnapshotUuid, isISCSI);
                        if (snashotPaPaPaUuid != null && prevSnashotPaUuid != null && prevSnashotPaUuid.equals(snashotPaPaPaUuid)) {
                            fullbackup = false;
                        } else {
                            fullbackup = true;
                        }
                    }
                } catch (final Exception e) {
                    s_logger.debug("Failed to get parent snapshots, take full snapshot", e);
                    fullbackup = true;
                }
            }

            final URI uri = new URI(secondaryStorageUrl);
            final String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            final DataStoreTO destStore = destData.getDataStore();
            final String folder = destPath;
            String finalPath = null;

            final String localMountPoint = BaseMountPointOnHost + File.separator + UUID.nameUUIDFromBytes(secondaryStorageUrl.getBytes()).toString();
            if (fullbackup) {
                // the first snapshot is always a full snapshot

                if (!hypervisorResource.createSecondaryStorageFolder(conn, secondaryStorageMountPath, folder)) {
                    details = " Filed to create folder " + folder + " in secondary storage";
                    s_logger.warn(details);
                    return new CopyCmdAnswer(details);
                }
                final String snapshotMountpoint = secondaryStorageUrl + "/" + folder;
                SR snapshotSr = null;
                try {
                    snapshotSr = hypervisorResource.createNfsSRbyURI(conn, new URI(snapshotMountpoint), false);
                    final VDI backedVdi = hypervisorResource.cloudVDIcopy(conn, snapshotVdi, snapshotSr, wait);
                    snapshotBackupUuid = backedVdi.getUuid(conn);
                    final String primarySRuuid = snapshotSr.getUuid(conn);
                    physicalSize = getSnapshotSize(conn, primarySRuuid, snapshotBackupUuid, isISCSI, wait);

                    if (destStore instanceof SwiftTO) {
                        try {
                            final String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                            final String destSnapshotName = swiftBackupSnapshot(conn, (SwiftTO) destStore, snapshotSr.getUuid(conn), snapshotBackupUuid, container, false, wait);
                            final String swiftPath = container + File.separator + destSnapshotName;
                            finalPath = swiftPath;
                        } finally {
                            try {
                                deleteSnapshotBackup(conn, localMountPoint, folder, secondaryStorageMountPath, snapshotBackupUuid);
                            } catch (final Exception e) {
                                s_logger.debug("Failed to delete snapshot on cache storages", e);
                            }
                        }

                    } else if (destStore instanceof S3TO) {
                        try {
                            finalPath = backupSnapshotToS3(conn, (S3TO) destStore, snapshotSr.getUuid(conn), folder, snapshotBackupUuid, isISCSI, wait);
                            if (finalPath == null) {
                                throw new CloudRuntimeException("S3 upload of snapshots " + snapshotBackupUuid + " failed");
                            }
                        } finally {
                            try {
                                deleteSnapshotBackup(conn, localMountPoint, folder, secondaryStorageMountPath, snapshotBackupUuid);
                            } catch (final Exception e) {
                                s_logger.debug("Failed to delete snapshot on cache storages", e);
                            }
                        }
                        // finalPath = folder + File.separator + snapshotBackupUuid;
                    } else {
                        finalPath = folder + cacheStore.getPathSeparator() + snapshotBackupUuid;
                    }

                } finally {
                    if (snapshotSr != null) {
                        hypervisorResource.removeSR(conn, snapshotSr);
                    }
                }
            } else {
                final String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                if (destStore instanceof SwiftTO) {
                    final String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                    snapshotBackupUuid =
                            swiftBackupSnapshot(conn, (SwiftTO) destStore, primaryStorageSRUuid, snapshotPaUuid, "S-" + snapshotTO.getVolume().getVolumeId().toString(),
                                    isISCSI, wait);
                    finalPath = container + File.separator + snapshotBackupUuid;
                } else if (destStore instanceof S3TO) {
                    finalPath = backupSnapshotToS3(conn, (S3TO) destStore, primaryStorageSRUuid, folder, snapshotPaUuid, isISCSI, wait);
                    if (finalPath == null) {
                        throw new CloudRuntimeException("S3 upload of snapshots " + snapshotPaUuid + " failed");
                    }
                } else {
                    final String results =
                            backupSnapshot(conn, primaryStorageSRUuid, localMountPoint, folder, secondaryStorageMountPath, snapshotUuid, prevBackupUuid, isISCSI, wait);

                    final String[] tmp = results.split("#");
                    snapshotBackupUuid = tmp[1];
                    physicalSize = Long.parseLong(tmp[2]);
                    finalPath = folder + cacheStore.getPathSeparator() + snapshotBackupUuid;
                }
            }
            // delete primary snapshots with only the last one left
            destroySnapshotOnPrimaryStorageExceptThis(conn, volumeUuid, snapshotUuid);

            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(finalPath);
            newSnapshot.setPhysicalSize(physicalSize);
            if (fullbackup) {
                newSnapshot.setParentSnapshotPath(null);
            } else {
                newSnapshot.setParentSnapshotPath(prevBackupUuid);
            }
            result = true;
            return new CopyCmdAnswer(newSnapshot);
        } catch (final XenAPIException e) {
            details = "BackupSnapshot Failed due to " + e.toString();
            s_logger.warn(details, e);
        } catch (final Exception e) {
            details = "BackupSnapshot Failed due to " + e.getMessage();
            s_logger.warn(details, e);
        } finally {
            if (!result) {
                // remove last bad primary snapshot when exception happens
                try {
                    destroySnapshotOnPrimaryStorage(conn, snapshotUuid);
                } catch (final Exception e) {
                    s_logger.debug("clean up snapshot failed", e);
                }
            }
        }

        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer createTemplateFromVolume(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final VolumeObjectTO volume = (VolumeObjectTO) cmd.getSrcTO();
        final TemplateObjectTO template = (TemplateObjectTO) cmd.getDestTO();
        final NfsTO destStore = (NfsTO) cmd.getDestTO().getDataStore();
        final int wait = cmd.getWait();

        final String secondaryStoragePoolURL = destStore.getUrl();
        final String volumeUUID = volume.getPath();

        final String userSpecifiedName = template.getName();

        String details = null;
        SR tmpltSR = null;
        boolean result = false;
        String secondaryStorageMountPath = null;
        String installPath = null;
        try {
            final URI uri = new URI(secondaryStoragePoolURL);
            secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            installPath = template.getPath();
            if (!hypervisorResource.createSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath)) {
                details = " Filed to create folder " + installPath + " in secondary storage";
                s_logger.warn(details);
                return new CopyCmdAnswer(details);
            }

            final VDI vol = getVDIbyUuid(conn, volumeUUID);
            // create template SR
            final URI tmpltURI = new URI(secondaryStoragePoolURL + "/" + installPath);
            tmpltSR = hypervisorResource.createNfsSRbyURI(conn, tmpltURI, false);

            // copy volume to template SR
            final VDI tmpltVDI = hypervisorResource.cloudVDIcopy(conn, vol, tmpltSR, wait);
            // scan makes XenServer pick up VDI physicalSize
            tmpltSR.scan(conn);
            if (userSpecifiedName != null) {
                tmpltVDI.setNameLabel(conn, userSpecifiedName);
            }

            final String tmpltUUID = tmpltVDI.getUuid(conn);
            final String tmpltFilename = tmpltUUID + ".vhd";
            final long virtualSize = tmpltVDI.getVirtualSize(conn);
            final long physicalSize = tmpltVDI.getPhysicalUtilisation(conn);
            // create the template.properties file
            final String templatePath = secondaryStorageMountPath + "/" + installPath;
            result =
                    hypervisorResource.postCreatePrivateTemplate(conn, templatePath, tmpltFilename, tmpltUUID, userSpecifiedName, null, physicalSize, virtualSize,
                            template.getId());
            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir: " + tmpltURI);
            }
            installPath = installPath + "/" + tmpltFilename;
            hypervisorResource.removeSR(conn, tmpltSR);
            tmpltSR = null;
            final TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(installPath);
            newTemplate.setFormat(ImageFormat.VHD);
            newTemplate.setSize(virtualSize);
            newTemplate.setPhysicalSize(physicalSize);
            newTemplate.setName(tmpltUUID);
            final CopyCmdAnswer answer = new CopyCmdAnswer(newTemplate);
            return answer;
        } catch (final Exception e) {
            if (tmpltSR != null) {
                hypervisorResource.removeSR(conn, tmpltSR);
            }
            if (secondaryStorageMountPath != null) {
                hypervisorResource.deleteSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath);
            }
            details = "Creating template from volume " + volumeUUID + " failed due to " + e.toString();
            s_logger.error(details, e);
        }
        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer createTemplateFromSnapshot(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();

        final SnapshotObjectTO snapshotObjTO = (SnapshotObjectTO)cmd.getSrcTO();
        final TemplateObjectTO templateObjTO = (TemplateObjectTO)cmd.getDestTO();

        if (!(snapshotObjTO.getDataStore() instanceof PrimaryDataStoreTO) || !(templateObjTO.getDataStore() instanceof NfsTO)) {
            return null;
        }

        final String userSpecifiedTemplateName = templateObjTO.getName();

        NfsTO destStore = null;
        URI destUri = null;

        try {
            destStore = (NfsTO)templateObjTO.getDataStore();

            destUri = new URI(destStore.getUrl());
        } catch (final Exception ex) {
            s_logger.debug("Invalid URI", ex);

            return new CopyCmdAnswer("Invalid URI: " + ex.toString());
        }

        SR srcSr = null;
        SR destSr = null;

        final String destDir = templateObjTO.getPath();
        VDI destVdi = null;

        boolean result = false;

        try {
            final Map<String, String> srcDetails = cmd.getOptions();

            final String iScsiName = srcDetails.get(DiskTO.IQN);
            final String storageHost = srcDetails.get(DiskTO.STORAGE_HOST);
            final String chapInitiatorUsername = srcDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
            final String chapInitiatorSecret = srcDetails.get(DiskTO.CHAP_INITIATOR_SECRET);

            srcSr = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, true);

            final String destNfsPath = destUri.getHost() + ":" + destUri.getPath();

            if (!hypervisorResource.createSecondaryStorageFolder(conn, destNfsPath, destDir)) {
                final String details = " Failed to create folder " + destDir + " in secondary storage";

                s_logger.warn(details);

                return new CopyCmdAnswer(details);
            }

            final URI templateUri = new URI(destStore.getUrl() + "/" + destDir);

            destSr = hypervisorResource.createNfsSRbyURI(conn, templateUri, false);

            // there should only be one VDI in this SR
            final VDI srcVdi = srcSr.getVDIs(conn).iterator().next();

            destVdi = srcVdi.copy(conn, destSr);

            // scan makes XenServer pick up VDI physicalSize
            destSr.scan(conn);

            if (userSpecifiedTemplateName != null) {
                destVdi.setNameLabel(conn, userSpecifiedTemplateName);
            }

            final String templateUuid = destVdi.getUuid(conn);
            final String templateFilename = templateUuid + ".vhd";
            final long virtualSize = destVdi.getVirtualSize(conn);
            final long physicalSize = destVdi.getPhysicalUtilisation(conn);

            // create the template.properties file
            String templatePath = destNfsPath + "/" + destDir;

            templatePath = templatePath.replaceAll("//", "/");

            result = hypervisorResource.postCreatePrivateTemplate(conn, templatePath, templateFilename, templateUuid, userSpecifiedTemplateName, null,
                    physicalSize, virtualSize, templateObjTO.getId());

            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir: " + templateUri);
            }

            final TemplateObjectTO newTemplate = new TemplateObjectTO();

            newTemplate.setPath(destDir + "/" + templateFilename);
            newTemplate.setFormat(Storage.ImageFormat.VHD);
            newTemplate.setHypervisorType(HypervisorType.XenServer);
            newTemplate.setSize(virtualSize);
            newTemplate.setPhysicalSize(physicalSize);
            newTemplate.setName(templateUuid);

            result = true;

            return new CopyCmdAnswer(newTemplate);
        } catch (final Exception ex) {
            s_logger.error("Failed to create a template from a snapshot", ex);

            return new CopyCmdAnswer("Failed to create a template from a snapshot: " + ex.toString());
        } finally {
            if (!result) {
                if (destVdi != null) {
                    try {
                        destVdi.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.debug("Cleaned up leftover VDI on destination storage due to failure: ", e);
                    }
                }
            }

            if (srcSr != null) {
                hypervisorResource.removeSR(conn, srcSr);
            }

            if (destSr != null) {
                hypervisorResource.removeSR(conn, destSr);
            }
        }
    }

    private boolean isManaged(Map<String, String> options) {
        if (options == null) {
            return false;
        }

        String iqn = options.get(DiskTO.IQN);

        if (iqn == null || iqn.trim().length() == 0) {
            return false;
        }

        String storageHost = options.get(DiskTO.STORAGE_HOST);

        if (storageHost == null || storageHost.trim().length() == 0) {
            return false;
        }

        return true;
    }

    boolean isCreateManagedVolumeFromManagedSnapshot(Map<String, String> volumeOptions, Map<String, String> snapshotOptions) {
        return isManaged(volumeOptions) && isManaged(snapshotOptions);
    }

    boolean isCreateNonManagedVolumeFromManagedSnapshot(Map<String, String> volumeOptions, Map<String, String> snapshotOptions) {
        return !isManaged(volumeOptions) && isManaged(snapshotOptions);
    }

    @Override
    public Answer createVolumeFromSnapshot(final CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();

        DataTO srcData = cmd.getSrcTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataStoreTO imageStore = srcData.getDataStore();
        DataTO destData = cmd.getDestTO();

        if (isCreateManagedVolumeFromManagedSnapshot(cmd.getOptions2(), cmd.getOptions())) {
            return createManagedVolumeFromManagedSnapshot(cmd);
        }

        if (isCreateNonManagedVolumeFromManagedSnapshot(cmd.getOptions2(), cmd.getOptions())) {
            return createNonManagedVolumeFromManagedSnapshot(cmd);
        }

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO nfsImageStore = (NfsTO)imageStore;
        String primaryStorageNameLabel = destData.getDataStore().getUuid();
        String secondaryStorageUrl = nfsImageStore.getUrl();

        int wait = cmd.getWait();
        boolean result = false;

        // Generic error message.
        String details;
        String volumeUUID;

        if (secondaryStorageUrl == null) {
            details = "The URL passed in 'null'.";

            return new CopyCmdAnswer(details);
        }

        try {
            SR primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);

            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not create volume from snapshot because the primary storage SR could not be " +
                        "created from the name label: " + primaryStorageNameLabel);
            }

            // Get the absolute path of the snapshot on the secondary storage.
            String snapshotInstallPath = snapshot.getPath();
            int index = snapshotInstallPath.lastIndexOf(nfsImageStore.getPathSeparator());
            String snapshotName = snapshotInstallPath.substring(index + 1);

            if (!snapshotName.startsWith("VHD-") && !snapshotName.endsWith(".vhd")) {
                snapshotInstallPath = snapshotInstallPath + ".vhd";
            }

            URI snapshotURI = new URI(secondaryStorageUrl + nfsImageStore.getPathSeparator() + snapshotInstallPath);
            String snapshotPath = snapshotURI.getHost() + ":" + snapshotURI.getPath();
            String srUuid = primaryStorageSR.getUuid(conn);

            volumeUUID = copy_vhd_from_secondarystorage(conn, snapshotPath, srUuid, wait);
            result = true;

            VDI volume = VDI.getByUuid(conn, volumeUUID);
            VDI.Record vdir = volume.getRecord(conn);
            VolumeObjectTO newVol = new VolumeObjectTO();

            newVol.setPath(volumeUUID);
            newVol.setSize(vdir.virtualSize);

            return new CopyCmdAnswer(newVol);
        } catch (final XenAPIException e) {
            details = "Exception due to " + e.toString();

            s_logger.warn(details, e);
        } catch (final Exception e) {
            details = "Exception due to " + e.getMessage();

            s_logger.warn(details, e);
        }

        if (!result) {
            // Is this logged at a higher level?
            s_logger.error(details);
        }

        // In all cases return something.
        return new CopyCmdAnswer(details);
    }

    Answer createManagedVolumeFromManagedSnapshot(final CopyCommand cmd) {
        try {
            final Connection conn = hypervisorResource.getConnection();

            final Map<String, String> srcOptions = cmd.getOptions();

            final String src_iScsiName = srcOptions.get(DiskTO.IQN);
            final String srcStorageHost = srcOptions.get(DiskTO.STORAGE_HOST);
            final String srcChapInitiatorUsername = srcOptions.get(DiskTO.CHAP_INITIATOR_USERNAME);
            final String srcChapInitiatorSecret = srcOptions.get(DiskTO.CHAP_INITIATOR_SECRET);

            final SR srcSr = hypervisorResource.getIscsiSR(conn, src_iScsiName, srcStorageHost, src_iScsiName, srcChapInitiatorUsername, srcChapInitiatorSecret, false);

            final Map<String, String> destOptions = cmd.getOptions2();

            final String dest_iScsiName = destOptions.get(DiskTO.IQN);
            final String destStorageHost = destOptions.get(DiskTO.STORAGE_HOST);
            final String destChapInitiatorUsername = destOptions.get(DiskTO.CHAP_INITIATOR_USERNAME);
            final String destChapInitiatorSecret = destOptions.get(DiskTO.CHAP_INITIATOR_SECRET);

            final SR destSr = hypervisorResource.getIscsiSR(conn, dest_iScsiName, destStorageHost, dest_iScsiName, destChapInitiatorUsername, destChapInitiatorSecret, false);

            // there should only be one VDI in this SR
            final VDI srcVdi = srcSr.getVDIs(conn).iterator().next();

            final VDI vdiCopy = srcVdi.copy(conn, destSr);

            final VolumeObjectTO newVol = new VolumeObjectTO();

            newVol.setSize(vdiCopy.getVirtualSize(conn));
            newVol.setPath(vdiCopy.getUuid(conn));
            newVol.setFormat(ImageFormat.VHD);

            hypervisorResource.removeSR(conn, srcSr);
            hypervisorResource.removeSR(conn, destSr);

            return new CopyCmdAnswer(newVol);
        }
        catch (final Exception ex) {
            s_logger.warn("Failed to copy snapshot to volume: " + ex.toString(), ex);

            return new CopyCmdAnswer(ex.getMessage());
        }
    }

    Answer createNonManagedVolumeFromManagedSnapshot(final CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        SR srcSr = null;

        try {
            Map<String, String> srcOptions = cmd.getOptions();

            String src_iScsiName = srcOptions.get(DiskTO.IQN);
            String srcStorageHost = srcOptions.get(DiskTO.STORAGE_HOST);
            String srcChapInitiatorUsername = srcOptions.get(DiskTO.CHAP_INITIATOR_USERNAME);
            String srcChapInitiatorSecret = srcOptions.get(DiskTO.CHAP_INITIATOR_SECRET);

            srcSr = hypervisorResource.getIscsiSR(conn, src_iScsiName, srcStorageHost, src_iScsiName,
                    srcChapInitiatorUsername, srcChapInitiatorSecret, false);

            // there should only be one VDI in this SR
            VDI srcVdi = srcSr.getVDIs(conn).iterator().next();

            DataTO destData = cmd.getDestTO();
            String primaryStorageNameLabel = destData.getDataStore().getUuid();

            SR destSr = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);

            VDI vdiCopy = srcVdi.copy(conn, destSr);

            VolumeObjectTO newVol = new VolumeObjectTO();

            newVol.setSize(vdiCopy.getVirtualSize(conn));
            newVol.setPath(vdiCopy.getUuid(conn));
            newVol.setFormat(ImageFormat.VHD);

            return new CopyCmdAnswer(newVol);
        }
        catch (Exception ex) {
            s_logger.warn("Failed to copy snapshot to volume: " + ex.toString(), ex);

            return new CopyCmdAnswer(ex.getMessage());
        }
        finally {
            if (srcSr != null) {
                hypervisorResource.removeSR(conn, srcSr);
            }
        }
    }

    @Override
    public Answer deleteSnapshot(final DeleteCommand cmd) {
        final SnapshotObjectTO snapshot = (SnapshotObjectTO) cmd.getData();
        final DataStoreTO store = snapshot.getDataStore();
        if (store.getRole() == DataStoreRole.Primary) {
            final Connection conn = hypervisorResource.getConnection();
            final VDI snapshotVdi = getVDIbyUuid(conn, snapshot.getPath());
            if (snapshotVdi == null) {
                return new Answer(null);
            }
            String errMsg = null;
            try {
                deleteVDI(conn, snapshotVdi);
            } catch (final BadServerResponse e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            } catch (final XenAPIException e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            } catch (final XmlRpcException e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            }
            return new Answer(cmd, false, errMsg);
        }
        return new Answer(cmd, false, "unsupported storage type");
    }

    @Override
    public Answer introduceObject(final IntroduceObjectCmd cmd) {
        try {
            final Connection conn = hypervisorResource.getConnection();
            final DataStoreTO store = cmd.getDataTO().getDataStore();
            final SR poolSr = hypervisorResource.getStorageRepository(conn, store.getUuid());
            poolSr.scan(conn);
            return new IntroduceObjectAnswer(cmd.getDataTO());
        } catch (final Exception e) {
            s_logger.debug("Failed to introduce object", e);
            return new Answer(cmd, false, e.toString());
        }
    }

    @Override
    public Answer forgetObject(final ForgetObjectCmd cmd) {
        try {
            final Connection conn = hypervisorResource.getConnection();
            final DataTO data = cmd.getDataTO();
            final VDI vdi = VDI.getByUuid(conn, data.getPath());
            vdi.forget(conn);
            return new IntroduceObjectAnswer(cmd.getDataTO());
        } catch (final Exception e) {
            s_logger.debug("Failed to forget object", e);
            return new Answer(cmd, false, e.toString());
        }
    }
}
