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
package com.cloud.hypervisor.xen.resource;


import static com.cloud.utils.ReflectUtil.flattenProperties;
import static com.google.common.collect.Lists.newArrayList;


import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreAnswer;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
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
import org.apache.cloudstack.storage.datastore.protocol.DataStoreProtocol;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase.SRType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.utils.S3Utils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.DecodedDataObject;
import com.cloud.utils.storage.encoding.DecodedDataStore;
import com.cloud.utils.storage.encoding.Decoder;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;

public class XenServerStorageProcessor implements StorageProcessor {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageProcessor.class);
    protected CitrixResourceBase hypervisorResource;
    private String BaseMountPointOnHost = "/var/run/cloud_mount";

    public XenServerStorageProcessor(CitrixResourceBase resource) {
        this.hypervisorResource = resource;
    }

    @Override
    public AttachAnswer attachIso(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        DataTO data = disk.getData();
        DataStoreTO store = data.getDataStore();

        String isoURL = null;
        if (store == null) {
            TemplateObjectTO iso = (TemplateObjectTO)disk.getData();
            isoURL = iso.getName();
        } else {
            if (!(store instanceof NfsTO)) {
                s_logger.debug("Can't attach a iso which is not created on nfs: ");
                return new AttachAnswer("Can't attach a iso which is not created on nfs: ");
            }
            NfsTO nfsStore = (NfsTO)store;
            isoURL = nfsStore.getUrl() + File.separator + data.getPath();
        }

        String vmName = cmd.getVmName();
        try {
            Connection conn = this.hypervisorResource.getConnection();

            VBD isoVBD = null;

            // Find the VM
            VM vm = this.hypervisorResource.getVM(conn, vmName);
            // Find the ISO VDI
            VDI isoVDI = this.hypervisorResource.getIsoVDIByURL(conn, vmName, isoURL);

            // Find the VM's CD-ROM VBD
            Set<VBD> vbds = vm.getVBDs(conn);
            for (VBD vbd : vbds) {
                String userDevice = vbd.getUserdevice(conn);
                Types.VbdType type = vbd.getType(conn);

                if (userDevice.equals("3") && type == Types.VbdType.CD) {
                    isoVBD = vbd;
                    break;
                }
            }

            if (isoVBD == null) {
                throw new CloudRuntimeException("Unable to find CD-ROM VBD for VM: " + vmName);
            } else {
                // If an ISO is already inserted, eject it
                if (isoVBD.getEmpty(conn) == false) {
                    isoVBD.eject(conn);
                }

                // Insert the new ISO
                isoVBD.insert(conn, isoVDI);
            }

            return new AttachAnswer(disk);

        } catch (XenAPIException e) {
            s_logger.warn("Failed to attach iso" + ": " + e.toString(), e);
            return new AttachAnswer(e.toString());
        } catch (Exception e) {
            s_logger.warn("Failed to attach iso" + ": " + e.toString(), e);
            return new AttachAnswer(e.toString());
        }
    }

    @Override
    public AttachAnswer attachVolume(AttachCommand cmd) {
        String vmName = cmd.getVmName();
        String vdiNameLabel = vmName + "-DATA";
        DiskTO disk = cmd.getDisk();
        DataTO data = disk.getData();

        try {
            Connection conn = this.hypervisorResource.getConnection();

            VDI vdi = null;

            Map<String, String> details = cmd.getDisk().getDetails();
            boolean isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));

            if (isManaged) {
                String iScsiName = details.get(DiskTO.IQN);
                String storageHost = details.get(DiskTO.STORAGE_HOST);
                String chapInitiatorUsername = disk.getDetails().get(DiskTO.CHAP_INITIATOR_USERNAME);
                String chapInitiatorSecret = disk.getDetails().get(DiskTO.CHAP_INITIATOR_SECRET);
                Long volumeSize = Long.parseLong(details.get(DiskTO.VOLUME_SIZE));

                SR sr = this.hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName,
                            chapInitiatorUsername, chapInitiatorSecret, true);

                vdi = this.hypervisorResource.getVDIbyUuid(conn, data.getPath(), false);

                if (vdi == null) {
                    vdi = this.hypervisorResource.createVdi(sr, vdiNameLabel, volumeSize);
                }
            }
            else {
                vdi = this.hypervisorResource.mount(conn, null, null, data.getPath());
            }

            // Look up the VM
            VM vm = this.hypervisorResource.getVM(conn, vmName);
            /* For HVM guest, if no pv driver installed, no attach/detach */
            boolean isHVM;
            if (vm.getPVBootloader(conn).equalsIgnoreCase("")) {
                isHVM = true;
            } else {
                isHVM = false;
            }
            VMGuestMetrics vgm = vm.getGuestMetrics(conn);
            boolean pvDrvInstalled = false;
            if (!this.hypervisorResource.isRefNull(vgm) && vgm.getPVDriversUpToDate(conn)) {
                pvDrvInstalled = true;
            }
            if (isHVM && !pvDrvInstalled) {
                s_logger.warn(": You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected");
                return new AttachAnswer("You attempted an operation that requires PV drivers to be installed on the VM. Please install them by inserting xen-pv-drv.iso.");
            }

            // Figure out the disk number to attach the VM to
            String diskNumber = null;
            Long deviceId = disk.getDiskSeq();
            if( deviceId != null ) {
                if( deviceId.longValue() == 3 ) {
                    String msg = "Device 3 is reserved for CD-ROM, choose other device";
                    return new AttachAnswer(msg);
                }
                if(this.hypervisorResource.isDeviceUsed(conn, vm, deviceId)) {
                    String msg = "Device " + deviceId + " is used in VM " + vmName;
                    return new AttachAnswer(msg);
                }
                diskNumber = deviceId.toString();
            } else {
                diskNumber = this.hypervisorResource.getUnusedDeviceNum(conn, vm);
            }
            // Create a new VBD
            VBD.Record vbdr = new VBD.Record();
            vbdr.VM = vm;
            vbdr.VDI = vdi;
            vbdr.bootable = false;
            vbdr.userdevice = diskNumber;
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            vbdr.unpluggable = true;
            VBD vbd = VBD.create(conn, vbdr);

            // Attach the VBD to the VM
            vbd.plug(conn);

            // Update the VDI's label to include the VM name
            vdi.setNameLabel(conn, vdiNameLabel);
            DiskTO newDisk = new DiskTO(disk.getData(), Long.parseLong(diskNumber), vdi.getUuid(conn), disk.getType());
            return new AttachAnswer(newDisk);

        } catch (XenAPIException e) {
            String msg = "Failed to attach volume" + " for uuid: " + data.getPath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new AttachAnswer(msg);
        } catch (Exception e) {
            String msg = "Failed to attach volume" + " for uuid: " + data.getPath() + "  due to "  + e.getMessage();
            s_logger.warn(msg, e);
            return new AttachAnswer(msg);
        }
    }

    @Override
    public Answer dettachIso(DettachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        DataTO data = disk.getData();
        DataStoreTO store = data.getDataStore();

        String isoURL = null;
        if (store == null) {
            TemplateObjectTO iso = (TemplateObjectTO)disk.getData();
            isoURL = iso.getName();
        } else {
            if (!(store instanceof NfsTO)) {
                s_logger.debug("Can't attach a iso which is not created on nfs: ");
                return new AttachAnswer("Can't attach a iso which is not created on nfs: ");
            }
            NfsTO nfsStore = (NfsTO)store;
            isoURL = nfsStore.getUrl() + File.separator + data.getPath();
        }

        try {
            Connection conn = this.hypervisorResource.getConnection();
            // Find the VM
            VM vm = this.hypervisorResource.getVM(conn, cmd.getVmName());
            String vmUUID = vm.getUuid(conn);

            // Find the ISO VDI
            VDI isoVDI = this.hypervisorResource.getIsoVDIByURL(conn, cmd.getVmName(), isoURL);

            SR sr = isoVDI.getSR(conn);

            // Look up all VBDs for this VDI
            Set<VBD> vbds = isoVDI.getVBDs(conn);

            // Iterate through VBDs, and if the VBD belongs the VM, eject
            // the ISO from it
            for (VBD vbd : vbds) {
                VM vbdVM = vbd.getVM(conn);
                String vbdVmUUID = vbdVM.getUuid(conn);

                if (vbdVmUUID.equals(vmUUID)) {
                    // If an ISO is already inserted, eject it
                    if (!vbd.getEmpty(conn)) {
                        vbd.eject(conn);
                    }
                    break;
                }
            }

            if (!sr.getNameLabel(conn).startsWith("XenServer Tools")) {
                this.hypervisorResource.removeSR(conn, sr);
            }

            return new DettachAnswer(disk);
        } catch (XenAPIException e) {
            String msg = "Failed to dettach volume" + " for uuid: " + data.getPath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new DettachAnswer(msg);
        } catch (Exception e) {
            String msg = "Failed to dettach volume" + " for uuid: " + data.getPath() + "  due to "  + e.getMessage();
            s_logger.warn(msg, e);
            return new DettachAnswer(msg);
        }
    }


    @Override
    public Answer dettachVolume(DettachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        DataTO data = disk.getData();
        try {
            Connection conn = this.hypervisorResource.getConnection();
            // Look up the VDI
            VDI vdi = this.hypervisorResource.mount(conn, null, null, data.getPath());
            // Look up the VM
            VM vm = this.hypervisorResource.getVM(conn, vmName);
            /* For HVM guest, if no pv driver installed, no attach/detach */
            boolean isHVM;
            if (vm.getPVBootloader(conn).equalsIgnoreCase("")) {
                isHVM = true;
            } else {
                isHVM = false;
            }
            VMGuestMetrics vgm = vm.getGuestMetrics(conn);
            boolean pvDrvInstalled = false;
            if (!this.hypervisorResource.isRefNull(vgm) && vgm.getPVDriversUpToDate(conn)) {
                pvDrvInstalled = true;
            }
            if (isHVM && !pvDrvInstalled) {
                s_logger.warn(": You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected");
                return new DettachAnswer("You attempted an operation that requires PV drivers to be installed on the VM. Please install them by inserting xen-pv-drv.iso.");
            }


            // Look up all VBDs for this VDI
            Set<VBD> vbds = vdi.getVBDs(conn);

            // Detach each VBD from its VM, and then destroy it
            for (VBD vbd : vbds) {
                VBD.Record vbdr = vbd.getRecord(conn);

                if (vbdr.currentlyAttached) {
                    vbd.unplug(conn);
                }

                vbd.destroy(conn);
            }

            // Update the VDI's label to be "detached"
            vdi.setNameLabel(conn, "detached");

            this.hypervisorResource.umount(conn, vdi);

            if (cmd.isManaged()) {
                this.hypervisorResource.handleSrAndVdiDetach(cmd.get_iScsiName());
            }

            return new DettachAnswer(disk);
        } catch(Exception e) {
            s_logger.warn("Failed dettach volume: " + data.getPath());
            return new DettachAnswer("Failed dettach volume: " + data.getPath() + ", due to " + e.toString());
        }
    }


    protected SR getSRByNameLabel(Connection conn, String nameLabel) throws BadServerResponse, XenAPIException, XmlRpcException {
        Set<SR> srs = SR.getByNameLabel(conn, nameLabel);
        if (srs.size() != 1) {
            throw new CloudRuntimeException("storage uuid: " + nameLabel + " is not unique");
        }
        SR poolsr = srs.iterator().next();
        return poolsr;
    }

    protected VDI createVdi(Connection conn, String vdiName, SR sr, long size) throws BadServerResponse, XenAPIException, XmlRpcException {
        VDI.Record vdir = new VDI.Record();
        vdir.nameLabel = vdiName;
        vdir.SR = sr;
        vdir.type = Types.VdiType.USER;

        vdir.virtualSize = size;
        VDI vdi = VDI.create(conn, vdir);
        return vdi;
    }

    protected void deleteVDI(Connection conn, VDI vdi) throws BadServerResponse, XenAPIException, XmlRpcException {
        vdi.destroy(conn);
    }

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)cmd.getData();
        long snapshotId = snapshotTO.getId();
        String snapshotName = snapshotTO.getName();
        String details = "create snapshot operation Failed for snapshotId: " + snapshotId;
        String snapshotUUID = null;

        try {
            String volumeUUID = snapshotTO.getVolume().getPath();
            VDI volume = VDI.getByUuid(conn, volumeUUID);

            VDI snapshot = volume.snapshot(conn, new HashMap<String, String>());

            if (snapshotName != null) {
                snapshot.setNameLabel(conn, snapshotName);
            }

            snapshotUUID = snapshot.getUuid(conn);
            String preSnapshotUUID = snapshotTO.getParentSnapshotPath();
            //check if it is a empty snapshot
            if( preSnapshotUUID != null) {
                SR sr = volume.getSR(conn);
                String srUUID = sr.getUuid(conn);
                String type = sr.getType(conn);
                Boolean isISCSI = IsISCSI(type);
                String snapshotParentUUID = getVhdParent(conn, srUUID, snapshotUUID, isISCSI);

                String preSnapshotParentUUID = getVhdParent(conn, srUUID, preSnapshotUUID, isISCSI);
                if( snapshotParentUUID != null && snapshotParentUUID.equals(preSnapshotParentUUID)) {
                    // this is empty snapshot, remove it
                    snapshot.destroy(conn);
                    snapshotUUID = preSnapshotUUID;
                }
            }
            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotUUID);
            return new CreateObjectAnswer(newSnapshot);
        } catch (XenAPIException e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        }

        return new CreateObjectAnswer(details);
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        DataTO volume = cmd.getData();
        Connection conn = hypervisorResource.getConnection();
        String errorMsg = null;
        try {
            VDI vdi = VDI.getByUuid(conn, volume.getPath());
            deleteVDI(conn, vdi);
            return new Answer(null);
        } catch (BadServerResponse e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (XenAPIException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        } catch (XmlRpcException e) {
            s_logger.debug("Failed to delete volume", e);
            errorMsg = e.toString();
        }
        return new Answer(null, false, errorMsg);
    }


    protected SR getNfsSR(Connection conn, StorageFilerTO pool) {
        Map<String, String> deviceConfig = new HashMap<String, String>();
        try {
            String server = pool.getHost();
            String serverpath = pool.getPath();
            serverpath = serverpath.replace("//", "/");
            Set<SR> srs = SR.getAll(conn);
            for (SR sr : srs) {
                if (!SRType.NFS.equals(sr.getType(conn))) {
                    continue;
                }

                Set<PBD> pbds = sr.getPBDs(conn);
                if (pbds.isEmpty()) {
                    continue;
                }

                PBD pbd = pbds.iterator().next();

                Map<String, String> dc = pbd.getDeviceConfig(conn);

                if (dc == null) {
                    continue;
                }

                if (dc.get("server") == null) {
                    continue;
                }

                if (dc.get("serverpath") == null) {
                    continue;
                }

                if (server.equals(dc.get("server")) && serverpath.equals(dc.get("serverpath"))) {
                    throw new CloudRuntimeException("There is a SR using the same configuration server:" + dc.get("server") + ", serverpath:"
                            + dc.get("serverpath") + " for pool " + pool.getUuid() + "on host:" + hypervisorResource.getHost().uuid);
                }

            }
            deviceConfig.put("server", server);
            deviceConfig.put("serverpath", serverpath);
            Host host = Host.getByUuid(conn, hypervisorResource.getHost().uuid);
            SR sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), Long.toString(pool.getId()), SRType.NFS.toString(), "user", true,
                    new HashMap<String, String>());
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pool.toString(), e);
        } catch (XmlRpcException e) {
            throw new CloudRuntimeException("Unable to create NFS SR " + pool.toString(), e);
        }
    }

    protected SR getIscsiSR(Connection conn, StorageFilerTO pool) {
        synchronized (pool.getUuid().intern()) {
            Map<String, String> deviceConfig = new HashMap<String, String>();
            try {
                String target = pool.getHost();
                String path = pool.getPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                String tmp[] = path.split("/");
                if (tmp.length != 3) {
                    String msg = "Wrong iscsi path " + pool.getPath() + " it should be /targetIQN/LUN";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                String targetiqn = tmp[1].trim();
                String lunid = tmp[2].trim();
                String scsiid = "";

                Set<SR> srs = SR.getByNameLabel(conn, pool.getUuid());
                for (SR sr : srs) {
                    if (!SRType.LVMOISCSI.equals(sr.getType(conn))) {
                        continue;
                    }
                    Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty()) {
                        continue;
                    }
                    PBD pbd = pbds.iterator().next();
                    Map<String, String> dc = pbd.getDeviceConfig(conn);
                    if (dc == null) {
                        continue;
                    }
                    if (dc.get("target") == null) {
                        continue;
                    }
                    if (dc.get("targetIQN") == null) {
                        continue;
                    }
                    if (dc.get("lunid") == null) {
                        continue;
                    }
                    if (target.equals(dc.get("target")) && targetiqn.equals(dc.get("targetIQN")) && lunid.equals(dc.get("lunid"))) {
                        throw new CloudRuntimeException("There is a SR using the same configuration target:" + dc.get("target") +  ",  targetIQN:"
                                + dc.get("targetIQN")  + ", lunid:" + dc.get("lunid") + " for pool " + pool.getUuid() + "on host:" + this.hypervisorResource.getHost().uuid);
                    }
                }
                deviceConfig.put("target", target);
                deviceConfig.put("targetIQN", targetiqn);

                Host host = Host.getByUuid(conn, this.hypervisorResource.getHost().uuid);
                Map<String, String> smConfig = new HashMap<String, String>();
                String type = SRType.LVMOISCSI.toString();
                String poolId = Long.toString(pool.getId());
                SR sr = null;
                try {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } catch (XenAPIException e) {
                    String errmsg = e.toString();
                    if (errmsg.contains("SR_BACKEND_FAILURE_107")) {
                        String lun[] = errmsg.split("<LUN>");
                        boolean found = false;
                        for (int i = 1; i < lun.length; i++) {
                            int blunindex = lun[i].indexOf("<LUNid>") + 7;
                            int elunindex = lun[i].indexOf("</LUNid>");
                            String ilun = lun[i].substring(blunindex, elunindex);
                            ilun = ilun.trim();
                            if (ilun.equals(lunid)) {
                                int bscsiindex = lun[i].indexOf("<SCSIid>") + 8;
                                int escsiindex = lun[i].indexOf("</SCSIid>");
                                scsiid = lun[i].substring(bscsiindex, escsiindex);
                                scsiid = scsiid.trim();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String msg = "can not find LUN " + lunid + " in " + errmsg;
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                        s_logger.warn(msg, e);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
                deviceConfig.put("SCSIid", scsiid);

                String result = SR.probe(conn, host, deviceConfig, type , smConfig);
                String pooluuid = null;
                if( result.indexOf("<UUID>") != -1) {
                    pooluuid = result.substring(result.indexOf("<UUID>") + 6, result.indexOf("</UUID>")).trim();
                }
                if( pooluuid == null || pooluuid.length() != 36) {
                    sr = SR.create(conn, host, deviceConfig, new Long(0), pool.getUuid(), poolId, type, "user", true,
                            smConfig);
                } else {
                    sr = SR.introduce(conn, pooluuid, pool.getUuid(), poolId,
                            type, "user", true, smConfig);
                    Pool.Record pRec = XenServerConnectionPool.getPoolRecord(conn);
                    PBD.Record rec = new PBD.Record();
                    rec.deviceConfig = deviceConfig;
                    rec.host = pRec.master;
                    rec.SR = sr;
                    PBD pbd = PBD.create(conn, rec);
                    pbd.plug(conn);
                }
                sr.scan(conn);
                return sr;
            } catch (XenAPIException e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.toString();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            } catch (Exception e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig + " due to  " + e.getMessage();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            }
        }
    }
    protected Answer execute(CreateStoragePoolCommand cmd) {
        Connection conn = this.hypervisorResource.getConnection();
        StorageFilerTO pool = cmd.getPool();
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                getNfsSR(conn, pool);
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                getIscsiSR(conn, pool);
            } else if (pool.getType() == StoragePoolType.PreSetup) {
            } else {
                return new Answer(cmd, false, "The pool type: " + pool.getType().name() + " is not supported.");
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + ", create StoragePool failed due to " + e.toString() + " on host:" + this.hypervisorResource.getHost().uuid + " pool: " + pool.getHost() + pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    protected Answer directDownloadHttpTemplate(CopyCommand cmd, DecodedDataObject srcObj, DecodedDataObject destObj) {
        Connection conn = hypervisorResource.getConnection();
        SR poolsr = null;
        VDI vdi = null;
        boolean result = false;
        try {
            if (destObj.getPath() == null) {
                //need to create volume at first

            }
            vdi = VDI.getByUuid(conn, destObj.getPath());
            if (vdi == null) {
                throw new CloudRuntimeException("can't find volume: " + destObj.getPath());
            }
            String destStoreUuid = destObj.getStore().getUuid();
            Set<SR> srs = SR.getByNameLabel(conn, destStoreUuid);
            if (srs.size() != 1) {
                throw new CloudRuntimeException("storage uuid: " + destStoreUuid + " is not unique");
            }
            poolsr = srs.iterator().next();
            VDI.Record vdir = vdi.getRecord(conn);
            String vdiLocation = vdir.location;
            String pbdLocation = null;
            if (destObj.getStore().getScheme().equalsIgnoreCase(DataStoreProtocol.NFS.toString())) {
                pbdLocation = "/run/sr-mount/" + poolsr.getUuid(conn);
            } else {
                Set<PBD> pbds = poolsr.getPBDs(conn);
                if (pbds.size() != 1) {
                    throw new CloudRuntimeException("Don't how to handle multiple pbds:" + pbds.size() + " for sr: " + poolsr.getUuid(conn));
                }
                PBD pbd = pbds.iterator().next();
                Map<String, String> deviceCfg = pbd.getDeviceConfig(conn);
                pbdLocation = deviceCfg.get("location");
            }
            if (pbdLocation == null) {
                throw new CloudRuntimeException("Can't get pbd location");
            }

            String vdiPath = pbdLocation + "/" + vdiLocation + ".vhd";
            //download a url into vdipath
            //downloadHttpToLocalFile(vdiPath, template.getPath());
            hypervisorResource.callHostPlugin(conn, "storagePlugin", "downloadTemplateFromUrl", "destPath", vdiPath, "srcUrl", srcObj.getPath());
            result = true;
            //return new CopyCmdAnswer(cmd, vdi.getUuid(conn));
        } catch (BadServerResponse e) {
            s_logger.debug("Failed to download template", e);
        } catch (XenAPIException e) {
            s_logger.debug("Failed to download template", e);
        } catch (XmlRpcException e) {
            s_logger.debug("Failed to download template", e);
        } catch (Exception e) {
            s_logger.debug("Failed to download template", e);
        } finally {
            if (!result && vdi != null) {
                try {
                    vdi.destroy(conn);
                } catch (BadServerResponse e) {
                    s_logger.debug("Failed to cleanup newly created vdi");
                } catch (XenAPIException e) {
                    s_logger.debug("Failed to cleanup newly created vdi");
                } catch (XmlRpcException e) {
                    s_logger.debug("Failed to cleanup newly created vdi");
                }
            }
        }
        return new Answer(cmd, false, "Failed to download template");
    }

    protected Answer execute(AttachPrimaryDataStoreCmd cmd) {
        String dataStoreUri = cmd.getDataStore();
        Connection conn = hypervisorResource.getConnection();
        try {
            DecodedDataObject obj = Decoder.decode(dataStoreUri);

            DecodedDataStore store = obj.getStore();

            SR sr = hypervisorResource.getStorageRepository(conn, store.getUuid());
            hypervisorResource.setupHeartbeatSr(conn, sr, false);
            long capacity = sr.getPhysicalSize(conn);
            long available = capacity - sr.getPhysicalUtilisation(conn);
            if (capacity == -1) {
                String msg = "Pool capacity is -1! pool: ";
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            }
            AttachPrimaryDataStoreAnswer answer = new AttachPrimaryDataStoreAnswer(cmd);
            answer.setCapacity(capacity);
            answer.setUuid(sr.getUuid(conn));
            answer.setAvailable(available);
            return answer;
        } catch (XenAPIException e) {
            String msg = "AttachPrimaryDataStoreCmd add XenAPIException:" + e.toString();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            String msg = "AttachPrimaryDataStoreCmd failed:" + e.getMessage();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    private boolean IsISCSI(String type) {
        return SRType.LVMOHBA.equals(type) || SRType.LVMOISCSI.equals(type) || SRType.LVM.equals(type) ;
    }

    private String copy_vhd_from_secondarystorage(Connection conn, String mountpoint, String sruuid, int wait) {
        String nameLabel = "cloud-" + UUID.randomUUID().toString();
        String results = hypervisorResource.callHostPluginAsync(conn, "vmopspremium", "copy_vhd_from_secondarystorage",
                wait, "mountpoint", mountpoint, "sruuid", sruuid, "namelabel", nameLabel);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "copy_vhd_from_secondarystorage return null";
        } else {
            String[] tmp = results.split("#");
            String status = tmp[0];
            if (status.equals("0")) {
                return tmp[1];
            } else {
                errMsg = tmp[1];
            }
        }
        String source = mountpoint.substring(mountpoint.lastIndexOf('/') + 1);
        if( hypervisorResource.killCopyProcess(conn, source) ) {
            destroyVDIbyNameLabel(conn, nameLabel);
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    private void destroyVDIbyNameLabel(Connection conn, String nameLabel) {
        try {
            Set<VDI> vdis = VDI.getByNameLabel(conn, nameLabel);
            if ( vdis.size() != 1 ) {
                s_logger.warn("destoryVDIbyNameLabel failed due to there are " + vdis.size() + " VDIs with name " + nameLabel);
                return;
            }
            for (VDI vdi : vdis) {
                try {
                    vdi.destroy(conn);
                } catch (Exception e) {
                }
            }
        } catch (Exception e){
        }
    }

    protected VDI getVDIbyUuid(Connection conn, String uuid) {
        try {
            return VDI.getByUuid(conn, uuid);
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " :VDI getByUuid for uuid: " + uuid + " failed due to " + e.toString();
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected String getVhdParent(Connection conn, String primaryStorageSRUuid, String snapshotUuid, Boolean isISCSI) {
        String parentUuid = hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid,
                "snapshotUuid", snapshotUuid, "isISCSI", isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        int wait = cmd.getWait();
        DataStoreTO srcStore = srcData.getDataStore();
        try {
            if ((srcStore instanceof NfsTO) && (srcData.getObjectType() == DataObjectType.TEMPLATE)) {
                NfsTO srcImageStore = (NfsTO)srcStore;
                TemplateObjectTO srcTemplate = (TemplateObjectTO)srcData;
                String storeUrl = srcImageStore.getUrl();

                URI uri = new URI(storeUrl);
                String tmplpath = uri.getHost() + ":" + uri.getPath() + "/" + srcData.getPath();
                String poolName = destData.getDataStore().getUuid();
                Connection conn = hypervisorResource.getConnection();

                SR poolsr = null;
                Set<SR> srs = SR.getByNameLabel(conn, poolName);
                if (srs.size() != 1) {
                    String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                    s_logger.warn(msg);
                    return new CopyCmdAnswer(msg);
                } else {
                    poolsr = srs.iterator().next();
                }
                String pUuid = poolsr.getUuid(conn);
                boolean isISCSI = IsISCSI(poolsr.getType(conn));
                String uuid = copy_vhd_from_secondarystorage(conn, tmplpath, pUuid, wait);
                VDI tmpl = getVDIbyUuid(conn, uuid);
                VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
                String snapshotUuid = snapshotvdi.getUuid(conn);
                snapshotvdi.setNameLabel(conn, "Template " + srcTemplate.getName());
                String parentuuid = getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
                VDI parent = getVDIbyUuid(conn, parentuuid);
                Long phySize = parent.getPhysicalUtilisation(conn);
                tmpl.destroy(conn);
                poolsr.scan(conn);
                try{
                    Thread.sleep(5000);
                } catch (Exception e) {
                }

                TemplateObjectTO newVol = new TemplateObjectTO();
                newVol.setUuid(snapshotvdi.getUuid(conn));
                newVol.setPath(newVol.getUuid());
                newVol.setFormat(ImageFormat.VHD);
                return new CopyCmdAnswer(newVol);
            }
        }catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " for template + " + " due to " + e.toString();
            s_logger.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        return new CopyCmdAnswer("not implemented yet");
    }

    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO)data;

        try {
            Connection conn = hypervisorResource.getConnection();
            SR poolSr = hypervisorResource.getStorageRepository(conn, data.getDataStore().getUuid());
            VDI.Record vdir = new VDI.Record();
            vdir.nameLabel = volume.getName();
            vdir.SR = poolSr;
            vdir.type = Types.VdiType.USER;

            vdir.virtualSize = volume.getSize();
            VDI vdi;

            vdi = VDI.create(conn, vdir);
            vdir = vdi.getRecord(conn);
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(vdir.nameLabel);
            newVol.setSize(vdir.virtualSize);
            newVol.setPath(vdir.uuid);

            return new CreateObjectAnswer(newVol);
        } catch (Exception e) {
            s_logger.debug("create volume failed: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        VolumeObjectTO volume = (VolumeObjectTO)destData;
        VDI vdi = null;
        try {
            VDI tmpltvdi = null;

            tmpltvdi = getVDIbyUuid(conn, srcData.getPath());
            vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
            vdi.setNameLabel(conn, volume.getName());


            VDI.Record vdir;
            vdir = vdi.getRecord(conn);
            s_logger.debug("Succesfully created VDI: Uuid = " + vdir.uuid);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(vdir.nameLabel);
            newVol.setSize(vdir.virtualSize);
            newVol.setPath(vdir.uuid);

            return new CopyCmdAnswer(newVol);
        } catch (Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + destData + "; Disk: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        int wait = cmd.getWait();
        VolumeObjectTO srcVolume = (VolumeObjectTO)srcData;
        VolumeObjectTO destVolume = (VolumeObjectTO)destData;
        DataStoreTO srcStore = srcVolume.getDataStore();

        if (srcStore instanceof NfsTO) {
            NfsTO nfsStore = (NfsTO)srcStore;
            try {
                SR primaryStoragePool = hypervisorResource.getStorageRepository(conn, destVolume.getDataStore().getUuid());
                String srUuid = primaryStoragePool.getUuid(conn);
                URI uri = new URI(nfsStore.getUrl());
                String volumePath = uri.getHost() + ":" + uri.getPath() + File.separator + srcVolume.getPath();
                String uuid = copy_vhd_from_secondarystorage(conn, volumePath, srUuid, wait );
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(uuid);
                newVol.setSize(srcVolume.getSize());

                return new CopyCmdAnswer(newVol);
            } catch (Exception e) {
                String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
                s_logger.warn(msg, e);
                return new CopyCmdAnswer(e.toString());
            }
        }

        s_logger.debug("unsupported protocol");
        return new CopyCmdAnswer("unsupported protocol");
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        VolumeObjectTO srcVolume = (VolumeObjectTO)cmd.getSrcTO();
        VolumeObjectTO destVolume = (VolumeObjectTO)cmd.getDestTO();
        int wait = cmd.getWait();
        DataStoreTO destStore = destVolume.getDataStore();

        if (destStore instanceof NfsTO) {
            SR secondaryStorage = null;
            try {
                NfsTO nfsStore = (NfsTO)destStore;
                URI uri = new URI(nfsStore.getUrl());
                // Create the volume folder
                if (!hypervisorResource.createSecondaryStorageFolder(conn, uri.getHost() + ":" + uri.getPath(), destVolume.getPath())) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }

                // Create a SR for the volume UUID folder
                secondaryStorage = hypervisorResource.createNfsSRbyURI(conn, new URI(nfsStore.getUrl() + File.separator + destVolume.getPath()), false);
                // Look up the volume on the source primary storage pool
                VDI srcVdi = getVDIbyUuid(conn, srcVolume.getPath());
                // Copy the volume to secondary storage
                VDI destVdi = hypervisorResource.cloudVDIcopy(conn, srcVdi, secondaryStorage, wait);
                String destVolumeUUID = destVdi.getUuid(conn);

                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(destVolume.getPath() + File.separator + destVolumeUUID + ".vhd");
                newVol.setSize(srcVolume.getSize());
                return new CopyCmdAnswer(newVol);
            } catch (Exception e) {
                s_logger.debug("Failed to copy volume to secondary: " + e.toString());
                return new CopyCmdAnswer("Failed to copy volume to secondary: " + e.toString());
            } finally {
                hypervisorResource.removeSR(conn, secondaryStorage);
            }
        }
        return new CopyCmdAnswer("unsupported protocol");
    }

    boolean swiftUpload(Connection conn, SwiftTO swift, String container, String ldir, String lfilename, Boolean isISCSI, int wait) {
        String result = null;
        try {
            result = hypervisorResource.callHostPluginAsync(conn, "swiftxen", "swift", wait,
                    "op", "upload", "url", swift.getUrl(), "account", swift.getAccount(),
                    "username", swift.getUserName(), "key", swift.getKey(), "container", container,
                    "ldir", ldir, "lfilename", lfilename, "isISCSI", isISCSI.toString());
            if( result != null && result.equals("true")) {
                return true;
            }
        } catch (Exception e) {
            s_logger.warn("swift upload failed due to " + e.toString(), e);
        }
        return false;
    }

    protected String deleteSnapshotBackup(Connection conn, String localMountPoint, String path, String secondaryStorageMountPath, String backupUUID) {

        // If anybody modifies the formatting below again, I'll skin them
        String result = hypervisorResource.callHostPlugin(conn, "vmopsSnapshot", "deleteSnapshotBackup", "backupUUID", backupUUID, "path", path, "secondaryStorageMountPath", secondaryStorageMountPath, "localMountPoint", localMountPoint);

        return result;
    }

    public String swiftBackupSnapshot(Connection conn, SwiftTO swift, String srUuid, String snapshotUuid, String container, Boolean isISCSI, int wait)  {
        String lfilename;
        String ldir;
        if ( isISCSI ) {
            ldir = "/dev/VG_XenStorage-" + srUuid;
            lfilename = "VHD-" + snapshotUuid;
        } else {
            ldir = "/var/run/sr-mount/" + srUuid;
            lfilename = snapshotUuid + ".vhd";
        }
        swiftUpload(conn, swift, container, ldir, lfilename, isISCSI, wait);
        return lfilename;
    }

    private String backupSnapshotToS3(final Connection connection, final S3TO s3, final String srUuid, final String folder, final String snapshotUuid,
            final Boolean iSCSIFlag, final int wait) {

        final String filename = iSCSIFlag ? "VHD-" + snapshotUuid
                : snapshotUuid + ".vhd";
        final String dir = (iSCSIFlag ? "/dev/VG_XenStorage-"
                : "/var/run/sr-mount/") + srUuid;
        final String key = folder + "/" + filename; // String.format("/snapshots/%1$s", snapshotUuid);

        try {

            final List<String> parameters = newArrayList(flattenProperties(s3,
                    S3Utils.ClientOptions.class));
            // https workaround for Introspector bug that does not
            // recognize Boolean accessor methods ...

            parameters.addAll(Arrays.asList("operation", "put", "filename",
                    dir + "/" + filename, "iSCSIFlag",
                    iSCSIFlag.toString(), "bucket", s3.getBucketName(),
                    "key", key, "https", s3.isHttps() != null ? s3.isHttps().toString()
                            : "null", "maxSingleUploadSizeInBytes", String.valueOf(s3.getMaxSingleUploadSizeInBytes())));
            final String result = hypervisorResource.callHostPluginAsync(connection, "s3xen",
                    "s3", wait,
                    parameters.toArray(new String[parameters.size()]));

            if (result != null && result.equals("true")) {
                return key;
            }
            return null;

        } catch (Exception e) {
            s_logger.error(String.format(
                    "S3 upload failed of snapshot %1$s due to %2$s.",
                    snapshotUuid, e.toString()), e);
        }

        return null;

    }

    protected String backupSnapshot(Connection conn, String primaryStorageSRUuid, String localMountPoint, String path, String secondaryStorageMountPath, String snapshotUuid, String prevBackupUuid, Boolean isISCSI, int wait) {
        String backupSnapshotUuid = null;

        if (prevBackupUuid == null) {
            prevBackupUuid = "";
        }

        // Each argument is put in a separate line for readability.
        // Using more lines does not harm the environment.
        String backupUuid = UUID.randomUUID().toString();
        String results = hypervisorResource.callHostPluginAsync(conn, "vmopsSnapshot", "backupSnapshot", wait,
                "primaryStorageSRUuid", primaryStorageSRUuid, "path", path, "secondaryStorageMountPath", secondaryStorageMountPath,
                "snapshotUuid", snapshotUuid, "prevBackupUuid", prevBackupUuid, "backupUuid", backupUuid, "isISCSI", isISCSI.toString(), "localMountPoint", localMountPoint);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "Could not copy backupUuid: " + backupSnapshotUuid
                    + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                    + secondaryStorageMountPath + " due to null";
        } else {

            String[] tmp = results.split("#");
            String status = tmp[0];
            backupSnapshotUuid = tmp[1];
            // status == "1" if and only if backupSnapshotUuid != null
            // So we don't rely on status value but return backupSnapshotUuid as an
            // indicator of success.
            if (status != null && status.equalsIgnoreCase("1") && backupSnapshotUuid != null) {
                s_logger.debug("Successfully copied backupUuid: " + backupSnapshotUuid
                        + " to secondary storage");
                return backupSnapshotUuid;
            } else {
                errMsg = "Could not copy backupUuid: " + backupSnapshotUuid
                        + " from primary storage " + primaryStorageSRUuid + " to secondary storage "
                        + secondaryStorageMountPath + " due to " + tmp[1];
            }
        }
        String source = backupUuid + ".vhd";
        hypervisorResource.killCopyProcess(conn, source);
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

    private boolean destroySnapshotOnPrimaryStorageExceptThis(Connection conn, String volumeUuid, String avoidSnapshotUuid){
        try {
            VDI volume = getVDIbyUuid(conn, volumeUuid);
            if (volume == null) {
                throw new InternalErrorException("Could not destroy snapshot on volume " + volumeUuid + " due to can not find it");
            }
            Set<VDI> snapshots = volume.getSnapshots(conn);
            for( VDI snapshot : snapshots ) {
                try {
                    if(! snapshot.getUuid(conn).equals(avoidSnapshotUuid)) {
                        snapshot.destroy(conn);
                    }
                } catch (Exception e) {
                    String msg = "Destroying snapshot: " + snapshot+ " on primary storage failed due to " + e.toString();
                    s_logger.warn(msg, e);
                }
            }
            s_logger.debug("Successfully destroyed snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid );
            return true;
        } catch (XenAPIException e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(msg, e);
        } catch (Exception e) {
            String msg = "Destroying snapshot on volume: " + volumeUuid + " execept this current snapshot "+ avoidSnapshotUuid + " failed due to " + e.toString();
            s_logger.warn(msg, e);
        }

        return false;
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        Connection conn = hypervisorResource.getConnection();
        DataTO srcData = cmd.getSrcTO();
        DataTO cacheData = cmd.getCacheTO();
        DataTO destData = cmd.getDestTO();
        int wait = cmd.getWait();
        String primaryStorageNameLabel = srcData.getDataStore().getUuid();
        String secondaryStorageUrl = null;
        NfsTO cacheStore = null;
        String destPath = null;
        if (cacheData != null) {
            cacheStore = (NfsTO)cacheData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = cacheData.getPath();
        } else {
            cacheStore = (NfsTO)destData.getDataStore();
            secondaryStorageUrl = cacheStore.getUrl();
            destPath = destData.getPath();
        }

        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)srcData;
        SnapshotObjectTO snapshotOnImage = (SnapshotObjectTO)destData;
        String snapshotUuid = snapshotTO.getPath();

        String prevBackupUuid = snapshotOnImage.getParentSnapshotPath();
        String prevSnapshotUuid = snapshotTO.getParentSnapshotPath();

        // By default assume failure
        String details = null;
        String snapshotBackupUuid = null;
        boolean fullbackup = true;
        try {
            SR primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            String psUuid = primaryStorageSR.getUuid(conn);
            Boolean isISCSI = IsISCSI(primaryStorageSR.getType(conn));

            VDI snapshotVdi = getVDIbyUuid(conn, snapshotUuid);
            String snapshotPaUuid = null;
            if ( prevBackupUuid != null ) {
                try {
                    snapshotPaUuid = getVhdParent(conn, psUuid, snapshotUuid, isISCSI);
                    if( snapshotPaUuid != null ) {
                        String snashotPaPaPaUuid = getVhdParent(conn, psUuid, snapshotPaUuid, isISCSI);
                        String prevSnashotPaUuid = getVhdParent(conn, psUuid, prevSnapshotUuid, isISCSI);
                        if (snashotPaPaPaUuid != null && prevSnashotPaUuid!= null && prevSnashotPaUuid.equals(snashotPaPaPaUuid)) {
                            fullbackup = false;
                        }
                    }
                } catch (Exception e) {
                }
            }

            URI uri = new URI(secondaryStorageUrl);
            String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            DataStoreTO destStore = destData.getDataStore();
            String folder = destPath;
            String finalPath = null;

            String localMountPoint =  BaseMountPointOnHost + File.separator + UUID.nameUUIDFromBytes(secondaryStorageUrl.getBytes()).toString();
            if (fullbackup) {
                // the first snapshot is always a full snapshot

                if( !hypervisorResource.createSecondaryStorageFolder(conn, secondaryStorageMountPath, folder)) {
                    details = " Filed to create folder " + folder + " in secondary storage";
                    s_logger.warn(details);
                    return new CopyCmdAnswer(details);
                }
                String snapshotMountpoint = secondaryStorageUrl + "/" + folder;
                SR snapshotSr = null;
                try {
                    snapshotSr = hypervisorResource.createNfsSRbyURI(conn, new URI(snapshotMountpoint), false);
                    VDI backedVdi = hypervisorResource.cloudVDIcopy(conn, snapshotVdi, snapshotSr, wait);
                    snapshotBackupUuid = backedVdi.getUuid(conn);

                    if( destStore instanceof SwiftTO) {
                        try {
                            String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                            String destSnapshotName = swiftBackupSnapshot(conn, (SwiftTO)destStore, snapshotSr.getUuid(conn), snapshotBackupUuid, container, false, wait);
                            String swiftPath = container + File.separator + destSnapshotName;
                            finalPath = swiftPath;
                        } finally {
                            try {
                                deleteSnapshotBackup(conn, localMountPoint, folder, secondaryStorageMountPath, snapshotBackupUuid);
                            } catch (Exception e) {
                                s_logger.debug("Failed to delete snapshot on cache storages" ,e);
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
                            } catch (Exception e) {
                                s_logger.debug("Failed to delete snapshot on cache storages" ,e);
                            }
                        }
                        // finalPath = folder + File.separator + snapshotBackupUuid;
                    } else {
                        finalPath = folder + File.separator + snapshotBackupUuid;
                    }

                } finally {
                    if( snapshotSr != null) {
                        hypervisorResource.removeSR(conn, snapshotSr);
                    }
                }
            } else {
                String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                if( destStore instanceof SwiftTO ) {
                    String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                    snapshotBackupUuid = swiftBackupSnapshot(conn, (SwiftTO)destStore, primaryStorageSRUuid, snapshotPaUuid, "S-" + snapshotTO.getVolume().getVolumeId().toString(), isISCSI, wait);
                    finalPath = container + File.separator + snapshotBackupUuid;
                } else if (destStore instanceof S3TO ) {
                    finalPath = backupSnapshotToS3(conn, (S3TO) destStore, primaryStorageSRUuid, folder, snapshotPaUuid, isISCSI, wait);
                    if (finalPath == null) {
                        throw new CloudRuntimeException("S3 upload of snapshots " + snapshotPaUuid + " failed");
                    }
                } else {
                    snapshotBackupUuid = backupSnapshot(conn, primaryStorageSRUuid, localMountPoint, folder,
                            secondaryStorageMountPath, snapshotUuid, prevBackupUuid, isISCSI, wait);

                    finalPath = folder + File.separator + snapshotBackupUuid;
                }
            }
            String volumeUuid = snapshotTO.getVolume().getPath();
            destroySnapshotOnPrimaryStorageExceptThis(conn, volumeUuid, snapshotUuid);

            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(finalPath);
            if (fullbackup) {
                newSnapshot.setParentSnapshotPath(null);
            } else {
                newSnapshot.setParentSnapshotPath(prevBackupUuid);
            }
            return new CopyCmdAnswer(newSnapshot);
        } catch (XenAPIException e) {
            details = "BackupSnapshot Failed due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details = "BackupSnapshot Failed due to " + e.getMessage();
            s_logger.warn(details, e);
        }

        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        Connection conn = this.hypervisorResource.getConnection();
        VolumeObjectTO volume = (VolumeObjectTO)cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)cmd.getDestTO();
        NfsTO destStore = (NfsTO)cmd.getDestTO().getDataStore();
        int wait = cmd.getWait();

        String secondaryStoragePoolURL = destStore.getUrl();
        String volumeUUID = volume.getPath();

        String userSpecifiedName = template.getName();


        String details = null;
        SR tmpltSR = null;
        boolean result = false;
        String secondaryStorageMountPath = null;
        String installPath = null;
        try {
            URI uri = new URI(secondaryStoragePoolURL);
            secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            installPath = template.getPath();
            if( !this.hypervisorResource.createSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath)) {
                details = " Filed to create folder " + installPath + " in secondary storage";
                s_logger.warn(details);
                return new CopyCmdAnswer(details);
            }

            VDI vol = getVDIbyUuid(conn, volumeUUID);
            // create template SR
            URI tmpltURI = new URI(secondaryStoragePoolURL + "/" + installPath);
            tmpltSR = this.hypervisorResource.createNfsSRbyURI(conn, tmpltURI, false);

            // copy volume to template SR
            VDI tmpltVDI = this.hypervisorResource.cloudVDIcopy(conn, vol, tmpltSR, wait);
            // scan makes XenServer pick up VDI physicalSize
            tmpltSR.scan(conn);
            if (userSpecifiedName != null) {
                tmpltVDI.setNameLabel(conn, userSpecifiedName);
            }

            String tmpltUUID = tmpltVDI.getUuid(conn);
            String tmpltFilename = tmpltUUID + ".vhd";
            long virtualSize = tmpltVDI.getVirtualSize(conn);
            long physicalSize = tmpltVDI.getPhysicalUtilisation(conn);
            // create the template.properties file
            String templatePath = secondaryStorageMountPath + "/" + installPath;
            result = this.hypervisorResource.postCreatePrivateTemplate(conn, templatePath, tmpltFilename, tmpltUUID, userSpecifiedName, null, physicalSize, virtualSize, template.getId());
            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir: " + tmpltURI);
            }
            installPath = installPath + "/" + tmpltFilename;
            this.hypervisorResource.removeSR(conn, tmpltSR);
            tmpltSR = null;
            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(installPath);
            newTemplate.setFormat(ImageFormat.VHD);
            newTemplate.setSize(virtualSize);
            newTemplate.setPhysicalSize(physicalSize);
            newTemplate.setName(tmpltUUID);
            CopyCmdAnswer answer = new CopyCmdAnswer(newTemplate);
            return answer;
        } catch (Exception e) {
            if (tmpltSR != null) {
                this.hypervisorResource.removeSR(conn, tmpltSR);
            }
            if ( secondaryStorageMountPath != null) {
                this.hypervisorResource.deleteSecondaryStorageFolder(conn, secondaryStorageMountPath, installPath);
            }
            details = "Creating template from volume " + volumeUUID + " failed due to " + e.toString();
            s_logger.error(details, e);
        }
        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        Connection conn = this.hypervisorResource.getConnection();
        DataTO srcData = cmd.getSrcTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataTO destData = cmd.getDestTO();
        DataStoreTO imageStore = srcData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO nfsImageStore = (NfsTO)imageStore;
        String primaryStorageNameLabel = destData.getDataStore().getUuid();
        String secondaryStorageUrl = nfsImageStore.getUrl();
        int wait = cmd.getWait();
        boolean result = false;
        // Generic error message.
        String details = null;
        String volumeUUID = null;

        if (secondaryStorageUrl == null) {
            details += " because the URL passed: " + secondaryStorageUrl + " is invalid.";
            return new CopyCmdAnswer(details);
        }
        try {
            SR primaryStorageSR = this.hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not create volume from snapshot because the primary Storage SR could not be created from the name label: "
                        + primaryStorageNameLabel);
            }
            // Get the absolute path of the snapshot on the secondary storage.
            String snapshotInstallPath = snapshot.getPath();
            int index = snapshotInstallPath.lastIndexOf(File.separator);
            String snapshotName = snapshotInstallPath.substring(index + 1);

            if (!snapshotName.startsWith("VHD-") && !snapshotName.endsWith(".vhd")) {
                snapshotInstallPath = snapshotInstallPath + ".vhd";
            }
            URI snapshotURI = new URI(secondaryStorageUrl + File.separator + snapshotInstallPath);
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
        } catch (XenAPIException e) {
            details += " due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += " due to " + e.getMessage();
            s_logger.warn(details, e);
        }
        if (!result) {
            // Is this logged at a higher level?
            s_logger.error(details);
        }

        // In all cases return something.
        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        SnapshotObjectTO snapshot = (SnapshotObjectTO)cmd.getData();
        DataStoreTO store = snapshot.getDataStore();
        if (store.getRole() == DataStoreRole.Primary) {
            Connection conn = this.hypervisorResource.getConnection();
            VDI snapshotVdi = getVDIbyUuid(conn, snapshot.getPath());
            if (snapshotVdi == null) {
                return new Answer(null);
            }
            String errMsg = null;
            try {
                this.deleteVDI(conn, snapshotVdi);
            } catch (BadServerResponse e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            } catch (XenAPIException e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            } catch (XmlRpcException e) {
                s_logger.debug("delete snapshot failed:" + e.toString());
                errMsg = e.toString();
            }
            return new Answer(cmd, false, errMsg);
        }
        return new Answer(cmd, false, "unsupported storage type");
    }

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        try {
            Connection conn = hypervisorResource.getConnection();
            DataStoreTO store = cmd.getDataTO().getDataStore();
            SR poolSr = hypervisorResource.getStorageRepository(conn, store.getUuid());
            poolSr.scan(conn);
            return new IntroduceObjectAnswer(cmd.getDataTO());
        } catch (Exception e) {
            s_logger.debug("Failed to introduce object", e);
            return new Answer(cmd, false, e.toString());
        }
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        try {
            Connection conn = hypervisorResource.getConnection();
            DataTO data = cmd.getDataTO();
            VDI vdi = VDI.getByUuid(conn, data.getPath());
            vdi.forget(conn);
            return new IntroduceObjectAnswer(cmd.getDataTO());
        } catch (Exception e) {
            s_logger.debug("Failed to introduce object", e);
            return new Answer(cmd, false, e.toString());
        }
    }
}
