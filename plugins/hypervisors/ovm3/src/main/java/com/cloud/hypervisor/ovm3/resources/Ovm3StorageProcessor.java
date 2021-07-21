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

package com.cloud.hypervisor.ovm3.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CheckDataStoreStoragePolicyComplainceCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.command.ResignatureAnswer;
import org.apache.cloudstack.storage.command.ResignatureCommand;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SyncVolumePathCommand;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.FileProperties;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3StoragePool;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.vm.DiskProfile;

/**
 * Storage related bits
 */
public class Ovm3StorageProcessor implements StorageProcessor {
    private final Logger LOGGER = Logger.getLogger(Ovm3StorageProcessor.class);
    private Connection c;
    private OvmObject ovmObject = new OvmObject();
    private Ovm3StoragePool pool;
    private Ovm3Configuration config;

    public Ovm3StorageProcessor(Connection conn, Ovm3Configuration ovm3config,
            Ovm3StoragePool ovm3pool) {
        c = conn;
        config = ovm3config;
        pool = ovm3pool;
    }

    public final Answer execute(final CopyCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        DataTO srcData = cmd.getSrcTO();
        DataStoreTO srcStore = srcData.getDataStore();
        DataTO destData = cmd.getDestTO();
        DataStoreTO destStore = destData.getDataStore();
        String msg = "Not implemented yet";
        try {
            /* target and source are NFS and TEMPLATE */
            if ((srcStore instanceof NfsTO)
                    && (srcData.getObjectType() == DataObjectType.TEMPLATE)
                    && (destData.getObjectType() == DataObjectType.TEMPLATE)) {
                return copyTemplateToPrimaryStorage(cmd);
                /* we assume the cache for templates is local */
            } else if ((srcData.getObjectType() == DataObjectType.TEMPLATE)
                    && (destData.getObjectType() == DataObjectType.VOLUME)) {
                if (srcStore.getUrl().equals(destStore.getUrl())) {
                    return cloneVolumeFromBaseTemplate(cmd);
                } else {
                    msg = "Primary to Primary doesn't match";
                    LOGGER.debug(msg);
                }
            } else if ((srcData.getObjectType() == DataObjectType.SNAPSHOT)
                    && (destData.getObjectType() == DataObjectType.SNAPSHOT)) {
                return backupSnapshot(cmd);
            } else if ((srcData.getObjectType() == DataObjectType.SNAPSHOT)
                    && (destData.getObjectType() == DataObjectType.TEMPLATE)) {
                return createTemplateFromSnapshot(cmd);
            } else if ((srcData.getObjectType() == DataObjectType.SNAPSHOT)
                    && (destData.getObjectType() == DataObjectType.VOLUME)) {
                return createVolumeFromSnapshot(cmd);
            } else {
                msg = "Unable to do stuff for " + srcStore.getClass() + ":"
                        + srcData.getObjectType() + " to "
                        + destStore.getClass() + ":" + destData.getObjectType();
                LOGGER.debug(msg);
            }
        } catch (Exception e) {
            msg = "Catch Exception " + e.getClass().getName()
                    + " for template due to " + e.toString();
            LOGGER.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        LOGGER.warn(msg + " " + cmd.getClass());
        return new CopyCmdAnswer(msg);
    }

    public Answer execute(DeleteCommand cmd) {
        DataTO data = cmd.getData();
        String msg;
        LOGGER.debug("Deleting object: " + data.getObjectType());
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return deleteVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            return deleteSnapshot(cmd);
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            msg = "Template deletion is not implemented yet.";
            LOGGER.info(msg);
        } else {
            msg = data.getObjectType() + " deletion is not implemented yet.";
            LOGGER.info(msg);
        }
        return new Answer(cmd, false, msg);
    }

    public CreateAnswer execute(CreateCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        StorageFilerTO primaryStorage = cmd.getPool();
        DiskProfile disk = cmd.getDiskCharacteristics();
        /* disk should have a uuid */
        // should also be replaced with getVirtualDiskPath ?
        String fileName = UUID.randomUUID().toString() + ".raw";
        String dst = primaryStorage.getPath() + "/"
                + primaryStorage.getUuid() + "/" + fileName;
        try {
            StoragePlugin store = new StoragePlugin(c);
            if (cmd.getTemplateUrl() != null) {
                LOGGER.debug("CreateCommand " + cmd.getTemplateUrl() + " "
                        + dst);
                Linux host = new Linux(c);
                host.copyFile(cmd.getTemplateUrl(), dst);
            } else {
                /* this is a dup with the createVolume ? */
                LOGGER.debug("CreateCommand " + dst);
                store.storagePluginCreate(primaryStorage.getUuid(),
                        primaryStorage.getHost(), dst, disk.getSize(), false);
            }
            FileProperties fp = store.storagePluginGetFileInfo(
                    primaryStorage.getUuid(), primaryStorage.getHost(), dst);
            VolumeTO volume = new VolumeTO(cmd.getVolumeId(), disk.getType(),
                    primaryStorage.getType(), primaryStorage.getUuid(),
                    primaryStorage.getPath(), fileName, fp.getName(),
                    fp.getSize(), null);
            return new CreateAnswer(cmd, volume);
        } catch (Exception e) {
            LOGGER.debug("CreateCommand failed", e);
            return new CreateAnswer(cmd, e.getMessage());
        }
    }

    /**
     * src is Nfs and Template from secondary storage to primary
     */
    @Override
    public CopyCmdAnswer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        LOGGER.debug("execute copyTemplateToPrimaryStorage: "+ cmd.getClass());
        DataTO srcData = cmd.getSrcTO();
        DataStoreTO srcStore = srcData.getDataStore();
        DataTO destData = cmd.getDestTO();
        NfsTO srcImageStore = (NfsTO) srcStore;
        TemplateObjectTO destTemplate = (TemplateObjectTO) destData;
        try {
            String secPoolUuid = pool.setupSecondaryStorage(srcImageStore.getUrl());
            String primaryPoolUuid = destData.getDataStore().getUuid();
            String destPath = config.getAgentOvmRepoPath() + "/"
                    + ovmObject.deDash(primaryPoolUuid) + "/"
                    + config.getTemplateDir();
            String sourcePath = config.getAgentSecStoragePath()
                    + "/" + secPoolUuid;
            Linux host = new Linux(c);
            String destUuid = destTemplate.getUuid();
            /*
             * Would love to add dynamic formats (tolower), to also support
             * VHD and QCOW2, although Ovm3.2 does not have tapdisk2 anymore
             * so we can forget about that.
             */
            /* TODO: add checksumming */
            String srcFile = sourcePath + "/"
                    + srcData.getPath();
            if (srcData.getPath().endsWith("/")) {
                srcFile = sourcePath + "/" + srcData.getPath()
                        + "/" + destUuid + ".raw";
            }
            String destFile = destPath + "/" + destUuid + ".raw";
            LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                    + srcFile + " to " + destData.getObjectType() + ","
                    + destFile);
            host.copyFile(srcFile, destFile);
            TemplateObjectTO newVol = new TemplateObjectTO();
            newVol.setUuid(destUuid);
            // was destfile
            newVol.setPath(destUuid);
            newVol.setFormat(ImageFormat.RAW);
            return new CopyCmdAnswer(newVol);
        } catch (Ovm3ResourceException e) {
            String msg = "Error while copying template to primary storage: " + e.getMessage();
            LOGGER.info(msg);
            return new CopyCmdAnswer(msg);
        }
    }
    /**
     * Only copies in case of dest is NfsTO, xenserver also unmounts secstorage
     */
    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        LOGGER.debug("execute copyVolumeFromPrimaryToSecondary: "+ cmd.getClass());
        return new Answer(cmd);
    }
    /**
     * dest is VolumeObject, src is a template
     */
    @Override
    public CopyCmdAnswer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        LOGGER.debug("execute cloneVolumeFromBaseTemplate: "+ cmd.getClass());
        try {
            // src
            DataTO srcData = cmd.getSrcTO();
            TemplateObjectTO src = (TemplateObjectTO) srcData;
            String srcFile = getVirtualDiskPath(src.getUuid(), src.getDataStore().getUuid());
            srcFile = srcFile.replace(config.getVirtualDiskDir(), config.getTemplateDir());

            DataTO destData = cmd.getDestTO();
            VolumeObjectTO dest = (VolumeObjectTO) destData;
            String destFile = getVirtualDiskPath(dest.getUuid(), dest.getDataStore().getUuid());
            Linux host = new Linux(c);
            LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                    + srcFile + " to " + destData.getObjectType() + ","
                    + destFile);
            host.copyFile(srcFile, destFile);
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setUuid(dest.getUuid());
            // was destfile
            newVol.setPath(dest.getUuid());
            newVol.setFormat(ImageFormat.RAW);
            return new CopyCmdAnswer(newVol);
        } catch (Ovm3ResourceException e) {
            String msg = "Error cloneVolumeFromBaseTemplate: " + e.getMessage();
            LOGGER.info(msg);
            return new CopyCmdAnswer(msg);
        }
    }
    /**
     * createprivatetemplate, also needs template.properties
     */
    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        LOGGER.debug("execute createTemplateFromVolume: "+ cmd.getClass());
        return new Answer(cmd);
    }
    /**
     * Volume to Volume from NfsTO
     */
    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        LOGGER.debug("execute copyVolumeFromImageCacheToPrimary: "+ cmd.getClass());
        return new Answer(cmd);
    }
    /**
     * Copies from secondary to secondary
     */
    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        LOGGER.debug("execute createTemplateFromSnapshot: "+ cmd.getClass());
        try {
            // src.getPath contains the uuid of the snapshot.
            DataTO srcData = cmd.getSrcTO();
            SnapshotObjectTO srcSnap = (SnapshotObjectTO) srcData;
            String secPoolUuid = pool.setupSecondaryStorage(srcData.getDataStore().getUrl());
            String srcFile = config.getAgentSecStoragePath()
                    + "/" + secPoolUuid + "/"
                    + srcSnap.getPath();
            // dest
            DataTO destData = cmd.getDestTO();
            TemplateObjectTO destTemplate = (TemplateObjectTO) destData;
            String secPoolUuidTemplate = pool.setupSecondaryStorage(destData.getDataStore().getUrl());
            String destDir = config.getAgentSecStoragePath()
                    + "/" + secPoolUuidTemplate + "/"
                    + destTemplate.getPath();
            String destFile = destDir + "/"
                    + destTemplate.getUuid() + ".raw";
            CloudstackPlugin csp = new CloudstackPlugin(c);
            csp.ovsMkdirs(destDir);

            Linux host = new Linux(c);
            host.copyFile(srcFile, destFile);
            TemplateObjectTO newVol = new TemplateObjectTO();
            newVol.setUuid(destTemplate.getUuid());
            newVol.setPath(destTemplate.getUuid());
            newVol.setFormat(ImageFormat.RAW);
            return new CopyCmdAnswer(newVol);
        } catch (Ovm3ResourceException e) {
            String msg = "Error backupSnapshot: " + e.getMessage();
            LOGGER.info(msg);
            return new CopyCmdAnswer(msg);
        }
    }

    /**
     * use the cache, or the normal nfs, also delete the leftovers for us
     * also contains object store storage in xenserver.
     */
    @Override
    public CopyCmdAnswer backupSnapshot(CopyCommand cmd) {
        LOGGER.debug("execute backupSnapshot: "+ cmd.getClass());
        try {
            DataTO srcData = cmd.getSrcTO();
            DataTO destData = cmd.getDestTO();
            SnapshotObjectTO src = (SnapshotObjectTO) srcData;
            SnapshotObjectTO dest = (SnapshotObjectTO) destData;

            // src.getPath contains the uuid of the snapshot.
            String srcFile = getVirtualDiskPath(src.getPath(), src.getDataStore().getUuid());

            // destination
            String storeUrl = dest.getDataStore().getUrl();
            String secPoolUuid = pool.setupSecondaryStorage(storeUrl);
            String destDir = config.getAgentSecStoragePath()
                    + "/" + secPoolUuid + "/"
                    + dest.getPath();
            String destFile =  destDir + "/" + src.getPath();
            destFile = destFile.concat(".raw");
            // copy
            Linux host = new Linux(c);
            CloudstackPlugin csp = new CloudstackPlugin(c);
            csp.ovsMkdirs(destDir);
            LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                    + srcFile + " to " + destData.getObjectType() + ","
                    + destFile);
            host.copyFile(srcFile, destFile);
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginDestroy(secPoolUuid, srcFile);

            SnapshotObjectTO newSnap = new SnapshotObjectTO();
            // newSnap.setPath(destFile);
            // damnit frickin crap, no reference whatsoever... could use parent ?
            newSnap.setPath(dest.getPath() + "/" + src.getPath() + ".raw");
            newSnap.setParentSnapshotPath(null);
            return new CopyCmdAnswer(newSnap);
        } catch (Ovm3ResourceException e) {
            String msg = "Error backupSnapshot: " + e.getMessage();
            LOGGER.info(msg);
            return new CopyCmdAnswer(msg);
        }
    }

    public Answer execute(CreateObjectCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        DataTO data = cmd.getData();
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return createVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            return createSnapshot(cmd);
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            LOGGER.debug("Template object creation not supported.");
        }
        return new CreateObjectAnswer(data.getObjectType()
                + " object creation not supported");
    }
    /**
     * Attach an iso
     */
    @Override
    public AttachAnswer attachIso(AttachCommand cmd) {
        LOGGER.debug("execute attachIso: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, true);
    }
    /**
     * Detach an iso
     */
    @Override
    public AttachAnswer dettachIso(DettachCommand cmd) {
        LOGGER.debug("execute dettachIso: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, false);
    }

    /**
     * Iso specific path return.
     * @param disk
     * @return
     * @throws Ovm3ResourceException
     */
    private String getIsoPath(DiskTO disk) throws Ovm3ResourceException {
        TemplateObjectTO isoTO = (TemplateObjectTO) disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        NfsTO nfsStore = (NfsTO) store;
        String secPoolUuid = pool.setupSecondaryStorage(nfsStore.getUrl());
        return config.getAgentSecStoragePath() + "/"
                + secPoolUuid + "/" + isoTO.getPath();
    }

    /**
     * Returns the disk path
     * @param diskUuid
     * @return
     * @throws Ovm3ResourceException
     */
    public String getVirtualDiskPath(String diskUuid, String storeUuid) throws Ovm3ResourceException {
        String d = config.getAgentOvmRepoPath() +
                "/" +
                ovmObject.deDash(storeUuid) +
                "/" +
                config.getVirtualDiskDir() +
                "/" +
                diskUuid;
        if (!d.endsWith(".raw")) {
            d = d.concat(".raw");
        }
        return d;
    }
    public String getVirtualDiskPath(DiskTO disk, String storeUuid) throws Ovm3ResourceException {
        return getVirtualDiskPath(disk.getPath(), storeUuid);
    }

    /**
     * Generic disk attach/detach.
     * @param cmd
     * @param vmName
     * @param disk
     * @param isAttach
     * @return
     */
    private AttachAnswer attachDetach(Command cmd, String vmName, DiskTO disk,
            boolean isAttach) {
        Xen xen = new Xen(c);
        String doThis = (isAttach) ? "Attach" : "Dettach";
        LOGGER.debug(doThis + " volume type " + disk.getType() + "  " + vmName);
        String msg = "";
        String path = "";
        try {
            Xen.Vm vm = xen.getVmConfig(vmName);
            /* check running */
            if (vm == null) {
                msg = doThis + " can't find VM " + vmName;
                LOGGER.debug(msg);
                return new AttachAnswer(msg);
            }
            if (disk.getType() == Volume.Type.ISO) {
                path = getIsoPath(disk);
            } else if (disk.getType() == Volume.Type.DATADISK) {
                path = getVirtualDiskPath(disk, vm.getPrimaryPoolUuid());
            }
            if ("".equals(path)) {
                msg = doThis + " can't do anything with an empty path.";
                LOGGER.debug(msg);
                return new AttachAnswer(msg);
            }
            if (isAttach) {
                if (disk.getType() == Volume.Type.ISO) {
                    vm.addIso(path);
                } else {
                    vm.addDataDisk(path);
                }
            } else {
                if (!vm.removeDisk(path)) {
                    msg = doThis + " failed for " + vmName + disk.getType()
                            + "  was not attached " + path;
                    LOGGER.debug(msg);
                    return new AttachAnswer(msg);
                }
            }
            xen.configureVm(ovmObject.deDash(vm.getPrimaryPoolUuid()),
                    vm.getVmUuid());
            return new AttachAnswer(disk);
        } catch (Ovm3ResourceException e) {
            msg = doThis + " failed for " + vmName + " " + e.getMessage();
            LOGGER.warn(msg, e);
            return new AttachAnswer(msg);
        }
    }
    /**
     * Attach a volume
     */
    @Override
    public AttachAnswer attachVolume(AttachCommand cmd) {
        LOGGER.debug("execute attachVolume: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, true);
    }
    /**
     * Detach a volume
     */
    @Override
    public AttachAnswer dettachVolume(DettachCommand cmd) {
        LOGGER.debug("execute dettachVolume: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, false);
    }

    /**
     * Creates a volume, just a normal empty volume.
     */
    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
        LOGGER.debug("execute createVolume: "+ cmd.getClass());
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO) data;
        try {
            /*
             * public Boolean storagePluginCreate(String uuid, String ssuuid,
             * String host, String file, Integer size)
             */
            String poolUuid = data.getDataStore().getUuid();
            String storeUrl = data.getDataStore().getUrl();
            URI uri = new URI(storeUrl);
            String host = uri.getHost();
            String file = getVirtualDiskPath(volume.getUuid(), poolUuid);
            Long size = volume.getSize();
            StoragePlugin sp = new StoragePlugin(c);
            FileProperties fp = sp.storagePluginCreate(poolUuid, host, file,
                    size, false);
            if (!fp.getName().equals(file)) {
                return new CreateObjectAnswer("Filename mismatch: "
                        + fp.getName() + " != " + file);
            }
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(volume.getName());
            newVol.setSize(fp.getSize());
            newVol.setPath(volume.getUuid());
            return new CreateObjectAnswer(newVol);
        } catch (Ovm3ResourceException | URISyntaxException e) {
            LOGGER.info("Volume creation failed: " + e.toString(), e);
            return new CreateObjectAnswer(e.toString());
        }
    }

    /**
     * Creates a snapshot from a volume, but only if the VM is stopped.
     * This due qemu not being able to snap raw volumes.
     *
     * if stopped yes, if running ... no, unless we have ocfs2 when
     * using raw partitions (file:) if using tap:aio we cloud...
     * The "ancient" way:
     * We do however follow the "two stage" approach, of "snap"
     * on primary first, with the create object... and then
     * backup the snapshot with the copycmd....
     * (should transfer to createSnapshot, backupSnapshot)
     */
    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        LOGGER.debug("execute createSnapshot: "+ cmd.getClass());
        DataTO data = cmd.getData();
        Xen xen = new Xen(c);
        SnapshotObjectTO snap = (SnapshotObjectTO) data;
        VolumeObjectTO vol = snap.getVolume();
        try {
            Xen.Vm vm = xen.getVmConfig(snap.getVmName());
            if (vm != null) {
                return new CreateObjectAnswer(
                        "Snapshot object creation not supported for running VMs."
                                + snap.getVmName());
            }
            Linux host = new Linux(c);
            String uuid = host.newUuid();
            /* for root volumes this works... */
            String src = vol.getPath() + "/" + vol.getUuid()
                    + ".raw";
            String dest = vol.getPath() + "/" + uuid + ".raw";
            /* seems that sometimes the path is already contains a file
             * in case, we just replace it.... (Seems to happen if not ROOT)
             */
            if (vol.getPath().contains(vol.getUuid())) {
                src = getVirtualDiskPath(vol.getUuid(),data.getDataStore().getUuid());
                dest = src.replace(vol.getUuid(), uuid);
            }
            LOGGER.debug("Snapshot " + src + " to " + dest);
            host.copyFile(src, dest);
            SnapshotObjectTO nsnap = new SnapshotObjectTO();
            // nsnap.setPath(dest);
            // move to something that looks the same as xenserver.
            nsnap.setPath(uuid);
            return new CreateObjectAnswer(nsnap);
        } catch (Ovm3ResourceException e) {
            return new CreateObjectAnswer(
                    "Snapshot object creation failed. " + e.getMessage());
        }
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        LOGGER.debug("execute deleteVolume: "+ cmd.getClass());
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO) data;
        try {
            String poolUuid = data.getDataStore().getUuid();
            String uuid = volume.getUuid();
            String path = getVirtualDiskPath(uuid, poolUuid);
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginDestroy(poolUuid, path);
            LOGGER.debug("Volume deletion success: " + path);
        } catch (Ovm3ResourceException e) {
            LOGGER.info("Volume deletion failed: " + e.toString(), e);
            return new CreateObjectAnswer(e.toString());
        }
        return new Answer(cmd);
    }

    /*
     * CopyVolumeCommand gets the storage_pool should use that for
     * bumper bowling.
     */
    public CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        String volumePath = cmd.getVolumePath();
        /* is a repository */
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        int wait = cmd.getWait();
        if (wait == 0) {
            wait = 7200;
        }

        try {
            Linux host = new Linux(c);

            /* to secondary storage */
            if (cmd.toSecondaryStorage()) {
                LOGGER.debug("Copy to  secondary storage " + volumePath
                        + " to " + secondaryStorageURL);
                host.copyFile(volumePath, secondaryStorageURL);
                /* from secondary storage */
            } else {
                LOGGER.debug("Copy from secondary storage "
                        + secondaryStorageURL + " to " + volumePath);
                host.copyFile(secondaryStorageURL, volumePath);
            }
            /* check the truth of this */
            return new CopyVolumeAnswer(cmd, true, null, null, null);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Copy volume failed", e);
            return new CopyVolumeAnswer(cmd, false, e.getMessage(), null, null);
        }
    }

    /* Destroy a volume (image) */
    public Answer execute(DestroyCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        VolumeTO vol = cmd.getVolume();
        String vmName = cmd.getVmName();
        try {
            StoragePlugin store = new StoragePlugin(c);
            store.storagePluginDestroy(vol.getPoolUuid(), vol.getPath());
            return new Answer(cmd, true, "Success");
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Destroy volume " + vol.getName() + " failed for "
                    + vmName + " ", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /* check if a VM is running should be added */
    public CreatePrivateTemplateAnswer execute(
            final CreatePrivateTemplateFromVolumeCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        String volumePath = cmd.getVolumePath();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        int wait = cmd.getWait();
        if (wait == 0) {
            /* Defaut timeout 2 hours */
            wait = 7200;
        }

        try {
            /* missing uuid */
            String installPath = config.getAgentOvmRepoPath() + "/"
                    + config.getTemplateDir() + "/"
                    + accountId + "/" + templateId;
            Linux host = new Linux(c);
            host.copyFile(volumePath, installPath);
            return new CreatePrivateTemplateAnswer(cmd, true, installPath);
        } catch (Exception e) {
            LOGGER.debug("Create template failed", e);
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        }
    }

    /**
     * SnapshotObjectTO secondary to VolumeObjectTO primary in xenserver,
     */
    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        LOGGER.debug("execute createVolumeFromSnapshot: "+ cmd.getClass());
        try {
            DataTO srcData = cmd.getSrcTO();
            DataStoreTO srcStore = srcData.getDataStore();
            NfsTO srcImageStore = (NfsTO) srcStore;

            // source, should contain snap dir/filename
            SnapshotObjectTO srcSnap = (SnapshotObjectTO) srcData;
            String secPoolUuid = pool.setupSecondaryStorage(srcImageStore.getUrl());
            String srcFile = config.getAgentSecStoragePath()
                    + "/" + secPoolUuid + "/"
                    + srcSnap.getPath();

            // dest
            DataTO destData = cmd.getDestTO();
            VolumeObjectTO destVol = (VolumeObjectTO) destData;
            String primaryPoolUuid = destData.getDataStore().getUuid();
            String destFile = getVirtualDiskPath(destVol.getUuid(), ovmObject.deDash(primaryPoolUuid));

            Linux host = new Linux(c);
            host.copyFile(srcFile, destFile);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setUuid(destVol.getUuid());
            // newVol.setPath(destFile);
            newVol.setPath(destVol.getUuid());
            newVol.setFormat(ImageFormat.RAW);
            return new CopyCmdAnswer(newVol);
            /* we assume the cache for templates is local */
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Failed to createVolumeFromSnapshot: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    /**
     * Is not used in normal operation, the SSVM takes care of this.
     */
    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        LOGGER.debug("execute deleteSnapshot: "+ cmd.getClass());
        DataTO data = cmd.getData();
        SnapshotObjectTO snap = (SnapshotObjectTO) data;
        String storeUrl = data.getDataStore().getUrl();
        String snapUuid = snap.getPath();
        try {
            // snapshots/accountid/volumeid
            String secPoolUuid = pool.setupSecondaryStorage(storeUrl);
            String filePath = config.getAgentSecStoragePath()
                    + "/" + secPoolUuid + "/"
                    + snapUuid + ".raw";
            StoragePlugin sp = new StoragePlugin(c);
            sp.storagePluginDestroy(secPoolUuid, filePath);
            LOGGER.debug("Snapshot deletion success: " + filePath);
            return new Answer(cmd, true, "Deleted Snapshot " + filePath);
        } catch (Ovm3ResourceException e) {
            LOGGER.info("Snapshot deletion failed: " + e.toString(), e);
            return new CreateObjectAnswer(e.toString());
        }
    }
    /**
     * SR scan in xenserver
     */
    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        LOGGER.debug("execute introduceObject: "+ cmd.getClass());
        return new Answer(cmd, false, "not implemented yet");
    }
    /**
     * used as unmount for VDIs in xenserver
     */
    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        LOGGER.debug("execute forgetObject: "+ cmd.getClass());
        return new Answer(cmd, false, "not implemented yet");
    }

    /**
     * make sure both mounts are there, snapshot source image
     * copy snap to dest image, remove source snap and unmount
     * iSCSI?
     */
    @Override
    public SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand cmd) {
        LOGGER.info("'SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand)' not currently used for Ovm3StorageProcessor");

        return new SnapshotAndCopyAnswer("Not implemented");
    }

    @Override
    public ResignatureAnswer resignature(final ResignatureCommand cmd) {
        LOGGER.info("'ResignatureAnswer resignature(ResignatureCommand)' not currently used for Ovm3StorageProcessor");

        return new ResignatureAnswer("Not implemented");
    }

    @Override
    public Answer handleDownloadTemplateToPrimaryStorage(DirectDownloadCommand cmd) {
        return null;
    }

    @Override
    public Answer checkDataStoreStoragePolicyCompliance(CheckDataStoreStoragePolicyComplainceCommand cmd) {
        LOGGER.info("'CheckDataStoreStoragePolicyComplainceCommand' not applicable used for Ovm3StorageProcessor");
        return new Answer(cmd,false,"Not applicable used for Ovm3StorageProcessor");
    }

    @Override
    public Answer syncVolumePath(SyncVolumePathCommand cmd) {
        LOGGER.info("SyncVolumePathCommand not currently applicable for Ovm3StorageProcessor");
        return new Answer(cmd, false, "Not currently applicable for Ovm3StorageProcessor");
    }

    @Override
    public Answer copyVolumeFromPrimaryToPrimary(CopyCommand cmd) {
        return null;
    }

    /**
     * Attach disks
     * @param cmd
     * @return
     */
    public Answer execute(AttachCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, true);
    }

    /**
     * Detach disks, calls a middle man which calls attachDetach for volumes.
     * @param cmd
     * @return
     */
    public Answer execute(DettachCommand cmd) {
        LOGGER.debug("execute: "+ cmd.getClass());
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, false);
    }
}
