/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.resources;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.ForgetObjectCmd;
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
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
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.FileProperties;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.vm.DiskProfile;

/**
 * Storage related bits
 */
public class Ovm3StorageProcessor implements StorageProcessor {
    private final Logger LOGGER = Logger
            .getLogger(Ovm3StorageProcessor.class);
    private Connection c;
    private OvmObject ovmObject = new OvmObject();
    private Ovm3StoragePool pool;
    private Ovm3Configuration config;
    public Ovm3StorageProcessor(Connection conn, Ovm3Configuration ovm3config, Ovm3StoragePool ovm3pool) {
        c = conn;
        config = ovm3config;
        pool = ovm3pool;
    }

    /*
     * TODO: This should move to StorageSubSystemCommand type as defined in the KVM plugin
     */
    public final Answer execute(final CopyCommand cmd) {
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
                NfsTO srcImageStore = (NfsTO) srcStore;
                TemplateObjectTO srcTemplate = (TemplateObjectTO) srcData;
                String storeUrl = srcImageStore.getUrl();
                String secPoolUuid = pool.setupSecondaryStorage(storeUrl);
                String primaryPoolUuid = destData.getDataStore().getUuid();
                String destPath = config.getAgentOvmRepoPath() + "/"
                        + ovmObject.deDash(primaryPoolUuid) + "/" + "Templates";
                String sourcePath = config.getAgentSecStoragePath() + "/" + secPoolUuid;

                Linux host = new Linux(c);
                String destUuid = srcTemplate.getUuid();
                /*
                 * Would love to add dynamic formats (tolower), to also support
                 * VHD and QCOW2, although Ovm3.2 does not have tapdisk2 anymore
                 * so we can forget about that.
                 */
                /* TODO: add checksumming */
                String srcFile = sourcePath + "/" + srcData.getPath();
                if (srcData.getPath().endsWith("/")) {
                    srcFile = sourcePath + "/" + srcData.getPath() + "/"
                            + destUuid + ".raw";
                }
                String destFile = destPath + "/" + destUuid + ImageFormat.RAW;
                LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                        + srcFile + " to " + destData.getObjectType() + ","
                        + destFile);
                host.copyFile(srcFile, destFile);

                TemplateObjectTO newVol = new TemplateObjectTO();
                newVol.setUuid(destUuid);
                newVol.setPath(destPath);
                newVol.setFormat(ImageFormat.RAW);
                return new CopyCmdAnswer(newVol);
                /* we assume the cache for templates is local */
            } else if ((srcData.getObjectType() == DataObjectType.TEMPLATE)
                    && (destData.getObjectType() == DataObjectType.VOLUME)) {
                if (srcStore.getUrl().equals(destStore.getUrl())) {
                    TemplateObjectTO srcTemplate = (TemplateObjectTO) srcData;
                    VolumeObjectTO dstVolume = (VolumeObjectTO) destData;

                    String srcFile = srcTemplate.getPath() + "/"
                            + srcTemplate.getUuid() + ".raw";
                    String vDisksPath = srcTemplate.getPath().replace(
                            "Templates", "VirtualDisks");
                    String destFile = vDisksPath + "/" + dstVolume.getUuid()
                            + ".raw";

                    Linux host = new Linux(c);
                    LOGGER.debug("CopyFrom: " + srcData.getObjectType() + ","
                            + srcFile + " to " + destData.getObjectType() + ","
                            + destFile);
                    host.copyFile(srcFile, destFile);
                    VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setUuid(dstVolume.getUuid());
                    newVol.setPath(vDisksPath);
                    newVol.setFormat(ImageFormat.RAW);
                    return new CopyCmdAnswer(newVol);
                } else {
                    msg = "Primary to Primary doesn't match";
                    LOGGER.debug(msg);
                }
            } else {
                msg = "Unable to do stuff for " + srcStore.getClass()
                        + ":" + srcData.getObjectType() + " to "
                        + destStore.getClass() + ":" + destData.getObjectType();
                LOGGER.debug(msg);
            }
        } catch (Exception e) {
            msg = "Catch Exception " + e.getClass().getName()
                    + " for template due to " + e.toString();
            LOGGER.warn(msg, e);
            return new CopyCmdAnswer(msg);
        }
        return new CopyCmdAnswer(msg);
    }

    public Answer execute(DeleteCommand cmd) {
        DataTO data = cmd.getData();
        String msg;
        LOGGER.debug("Deleting object: " + data.getObjectType());
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return deleteVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            msg = "Snapshot deletion is not implemented yet.";
            LOGGER.info(msg);
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            msg = "Template deletion is not implemented yet.";
            LOGGER.info(msg);
        } else {
            msg = data.getObjectType()
                    + " deletion is not implemented yet.";
            LOGGER.info(msg);
        }
        return new Answer(cmd, false, msg);
    }
    /* TODO: Create a Disk from a template needs cleaning */
    public CreateAnswer execute(CreateCommand cmd) {
        StorageFilerTO primaryStorage = cmd.getPool();
        DiskProfile disk = cmd.getDiskCharacteristics();
        /* disk should have a uuid */
        String fileName = UUID.randomUUID().toString() + ".raw";
        String dst = primaryStorage.getPath() + "/" + primaryStorage.getUuid()
                + "/" + fileName;
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
                        primaryStorage.getHost(), dst, disk.getSize());
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

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        /*
         * To change body of implemented methods use File | Settings | File
         * Templates.
         */
        return null;
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        return new Answer(cmd);
    }

    public Answer execute(CreateObjectCommand cmd) {
        DataTO data = cmd.getData();
        Xen xen = new Xen(c);
        if (data.getObjectType() == DataObjectType.VOLUME) {
            return createVolume(cmd);
        } else if (data.getObjectType() == DataObjectType.SNAPSHOT) {
            /*
             * if stopped yes, if running ... no, unless we have ocfs2 when
             * using raw partitions (file:) if using tap:aio we cloud...
             */
            SnapshotObjectTO snap = (SnapshotObjectTO) data;
            VolumeObjectTO vol = snap.getVolume();
            try {
                Xen.Vm vm = xen.getVmConfig(snap.getVmName());
                if (vm != null) {
                    return new CreateObjectAnswer("Snapshot object creation not supported for running VMs." + snap.getVmName());
                }
                Linux host = new Linux(c);
                String uuid = host.newUuid();
                String path = vol.getPath() + "/" + vol.getUuid() + ".raw";
                String dest = vol.getPath() + "/" + uuid + ".raw";
                host.copyFile(path,  dest);
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setUuid(uuid);
                newVol.setName(vol.getName());
                newVol.setSize(vol.getSize());
                newVol.setPath(dest);
                snap.setVolume(newVol);
                return new CreateObjectAnswer(snap);
            } catch (Ovm3ResourceException e) {
                return new CreateObjectAnswer("Snapshot object creation failed. " + e.getMessage());
            }
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            LOGGER.debug("Template object creation not supported.");
        }
        return new CreateObjectAnswer(data.getObjectType()
                + " object creation not supported");
    }

    @Override
    public AttachAnswer attachIso(AttachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, true);
    }

    @Override
    public AttachAnswer dettachIso(DettachCommand cmd) {
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
        String isoPath = config.getAgentSecStoragePath() + File.separator + secPoolUuid
                + File.separator + isoTO.getPath();
        return isoPath;
    }
    /**
     * Returns the disk path
     * @param disk
     * @param uuid
     * @return
     * @throws Ovm3ResourceException
     */
    private String getDiskPath(DiskTO disk) throws Ovm3ResourceException {
        return disk.getPath();
    }

    /**
     * Generic disk attach/detach.
     * @param cmd
     * @param vmName
     * @param disk
     * @param isAttach
     * @return
     */
    public AttachAnswer attachDetach(Command cmd, String vmName, DiskTO disk, boolean isAttach) {
        Xen xen = new Xen(c);
        String doThis = (isAttach) ? "Attach" : "Dettach";
        LOGGER.debug(doThis + " volume type " + disk.getType()
                + "  " + vmName);
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
                path = getDiskPath(disk);
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
                    msg = doThis + " failed for " + vmName
                            + disk.getType() + "  was not attached "
                            + path;
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

    @Override
    public AttachAnswer attachVolume(AttachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, true);
    }

    @Override
    public AttachAnswer dettachVolume(DettachCommand cmd) {
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, false);
    }

    /**
     * Creates a volume, just a normal empty volume.
     */
    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
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
            String file = config.getAgentOvmRepoPath() + "/" + ovmObject.deDash(poolUuid)
                    + "/VirtualDisks/" + volume.getUuid() + ".raw";
            Long size = volume.getSize();
            StoragePlugin sp = new StoragePlugin(c);
            FileProperties fp = sp.storagePluginCreate(poolUuid, host, file,
                    size);
            if (!fp.getName().equals(file)) {
                return new CreateObjectAnswer("Filename mismatch: " + fp.getName() + " != " + file);
            }
            // sp.storagePluginGetFileInfo(file);
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setName(volume.getName());
            newVol.setSize(fp.getSize());
            newVol.setPath(file);
            return new CreateObjectAnswer(newVol);
        } catch (Ovm3ResourceException | URISyntaxException e) {
            LOGGER.info("Volume creation failed: " + e.toString(), e);
            return new CreateObjectAnswer(e.toString());
        }
    }

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        /* storagePluginDestroy(String ssuuid, String file) */
        DataTO data = cmd.getData();
        VolumeObjectTO volume = (VolumeObjectTO) data;
        try {
            String poolUuid = data.getDataStore().getUuid();
            /* needs the file attached too please... */
            String path = volume.getPath();
            if (!path.contains(volume.getUuid())) {
                path = volume.getPath() + "/" + volume.getUuid() + ".raw";
            }
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
        String volumePath = cmd.getVolumePath();
        /* is a repository */
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        int wait = cmd.getWait();
        if (wait == 0) {
            wait = 7200;
        }

        /* TODO: we need to figure out what sec and prim really is for checks and balances */
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
            String installPath = config.getAgentOvmRepoPath() + "/Templates/" + accountId
                    + "/" + templateId;
            Linux host = new Linux(c);
            /* check if VM is running or thrown an error, or pause it :P */
            host.copyFile(volumePath, installPath);
            /* TODO: look at the original */
            return new CreatePrivateTemplateAnswer(cmd, true, installPath);
        } catch (Exception e) {
            LOGGER.debug("Create template failed", e);
            return new CreatePrivateTemplateAnswer(cmd, false, e.getMessage());
        }
    }

    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    @Override
    public Answer snapshotAndCopy(SnapshotAndCopyCommand cmd) {
        return new Answer(cmd, false, "not implemented yet");
    }

    /**
     * Attach disks
     * @param cmd
     * @return
     */
    public Answer execute(AttachCommand cmd) {
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
        String vmName = cmd.getVmName();
        DiskTO disk = cmd.getDisk();
        return attachDetach(cmd, vmName, disk, false);
    }
}
