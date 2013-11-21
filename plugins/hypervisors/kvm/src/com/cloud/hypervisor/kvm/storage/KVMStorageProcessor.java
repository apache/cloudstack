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
package com.cloud.hypervisor.kvm.storage;

import static com.cloud.utils.S3Utils.putFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;

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
import org.apache.cloudstack.storage.command.IntroduceObjectCmd;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef.diskProtocol;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.resource.StorageProcessor;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.S3Utils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class KVMStorageProcessor implements StorageProcessor {
    private static final Logger s_logger = Logger.getLogger(KVMStorageProcessor.class);
    private KVMStoragePoolManager storagePoolMgr;
    private LibvirtComputingResource resource;
    private StorageLayer storageLayer;
    private String _createTmplPath;
    private String _manageSnapshotPath;
    private int _cmdsTimeout;

    public KVMStorageProcessor(KVMStoragePoolManager storagePoolMgr, LibvirtComputingResource resource) {
        this.storagePoolMgr = storagePoolMgr;
        this.resource = resource;
    }

    protected String getDefaultStorageScriptsDir() {
        return "scripts/storage/qcow2";
    }

    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        storageLayer = new JavaStorageLayer();
        storageLayer.configure("StorageLayer", params);

        String storageScriptsDir = (String)params.get("storage.scripts.dir");
        if (storageScriptsDir == null) {
            storageScriptsDir = getDefaultStorageScriptsDir();
        }

        _createTmplPath = Script.findScript(storageScriptsDir, "createtmplt.sh");
        if (_createTmplPath == null) {
            throw new ConfigurationException("Unable to find the createtmplt.sh");
        }

        _manageSnapshotPath = Script.findScript(storageScriptsDir, "managesnapshot.sh");
        if (_manageSnapshotPath == null) {
            throw new ConfigurationException("Unable to find the managesnapshot.sh");
        }

        String value = (String)params.get("cmds.timeout");
        _cmdsTimeout = NumbersUtil.parseInt(value, 7200) * 1000;
        return true;
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataStoreTO imageStore = template.getDataStore();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO nfsImageStore = (NfsTO)imageStore;
        String tmplturl = nfsImageStore.getUrl() + File.separator + template.getPath();
        int index = tmplturl.lastIndexOf("/");
        String mountpoint = tmplturl.substring(0, index);
        String tmpltname = null;
        if (index < tmplturl.length() - 1) {
            tmpltname = tmplturl.substring(index + 1);
        }

        KVMPhysicalDisk tmplVol = null;
        KVMStoragePool secondaryPool = null;
        try {
            secondaryPool = storagePoolMgr.getStoragePoolByURI(mountpoint);

            /* Get template vol */
            if (tmpltname == null) {
                secondaryPool.refresh();
                List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    return new PrimaryStorageDownloadAnswer("Failed to get volumes from pool: " + secondaryPool.getUuid());
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        tmplVol = disk;
                        break;
                    }
                }
                if (tmplVol == null) {
                    return new PrimaryStorageDownloadAnswer("Failed to get template from pool: " + secondaryPool.getUuid());
                }
            } else {
                tmplVol = secondaryPool.getPhysicalDisk(tmpltname);
            }

            /* Copy volume to primary storage */
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk primaryVol = null;
            if (destData instanceof VolumeObjectTO) {
                VolumeObjectTO volume = (VolumeObjectTO)destData;
                primaryVol = storagePoolMgr.copyPhysicalDisk(tmplVol, volume.getUuid(), primaryPool, cmd.getWaitInMillSeconds());
            } else if (destData instanceof TemplateObjectTO) {
                TemplateObjectTO destTempl = (TemplateObjectTO)destData;
                primaryVol = storagePoolMgr.copyPhysicalDisk(tmplVol, destTempl.getUuid(), primaryPool, cmd.getWaitInMillSeconds());
            } else {
                primaryVol = storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, cmd.getWaitInMillSeconds());
            }

            DataTO data = null;
            /**
             * Force the ImageFormat for RBD templates to RAW
             *
             */
            if (destData.getObjectType() == DataObjectType.TEMPLATE) {
                TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(primaryVol.getName());
                if (primaryPool.getType() == StoragePoolType.RBD) {
                    newTemplate.setFormat(ImageFormat.RAW);
                } else {
                    newTemplate.setFormat(ImageFormat.QCOW2);
                }
                data = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO volumeObjectTO = new VolumeObjectTO();
                volumeObjectTO.setPath(primaryVol.getName());
                if (primaryVol.getFormat() == PhysicalDiskFormat.RAW)
                    volumeObjectTO.setFormat(ImageFormat.RAW);
                else if (primaryVol.getFormat() == PhysicalDiskFormat.QCOW2) {
                    volumeObjectTO.setFormat(ImageFormat.QCOW2);
                }
                data = volumeObjectTO;
            }
            return new CopyCmdAnswer(data);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate. copies template direct to root disk
    private KVMPhysicalDisk templateToPrimaryDownload(String templateUrl, KVMStoragePool primaryPool, String volUuid, int timeout) {
        int index = templateUrl.lastIndexOf("/");
        String mountpoint = templateUrl.substring(0, index);
        String templateName = null;
        if (index < templateUrl.length() - 1) {
            templateName = templateUrl.substring(index + 1);
        }

        KVMPhysicalDisk templateVol = null;
        KVMStoragePool secondaryPool = null;
        try {
            secondaryPool = storagePoolMgr.getStoragePoolByURI(mountpoint);
            /* Get template vol */
            if (templateName == null) {
                secondaryPool.refresh();
                List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    s_logger.error("Failed to get volumes from pool: " + secondaryPool.getUuid());
                    return null;
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        templateVol = disk;
                        break;
                    }
                }
                if (templateVol == null) {
                    s_logger.error("Failed to get template from pool: " + secondaryPool.getUuid());
                    return null;
                }
            } else {
                templateVol = secondaryPool.getPhysicalDisk(templateName);
            }

            /* Copy volume to primary storage */

            KVMPhysicalDisk primaryVol = storagePoolMgr.copyPhysicalDisk(templateVol, volUuid, primaryPool, timeout);
            return primaryVol;
        } catch (CloudRuntimeException e) {
            s_logger.error("Failed to download template to primary storage", e);
            return null;
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataStoreTO imageStore = template.getDataStore();
        VolumeObjectTO volume = (VolumeObjectTO)destData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();
        KVMPhysicalDisk BaseVol = null;
        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;

        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            String templatePath = template.getPath();

            if (primaryPool.getType() == StoragePoolType.CLVM) {
                templatePath = ((NfsTO)imageStore).getUrl() + File.separator + templatePath;
                vol = templateToPrimaryDownload(templatePath, primaryPool, volume.getUuid(), cmd.getWaitInMillSeconds());
            } else {
                if (templatePath.contains("/mnt")) {
                    //upgrade issue, if the path contains path, need to extract the volume uuid from path
                    templatePath = templatePath.substring(templatePath.lastIndexOf(File.separator) + 1);
                }
                BaseVol = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), templatePath);
                vol = storagePoolMgr.createDiskFromTemplate(BaseVol, volume.getUuid(), BaseVol.getPool(), cmd.getWaitInMillSeconds());
            }
            if (vol == null) {
                return new CopyCmdAnswer(" Can't create storage volume on storage pool");
            }

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vol.getName());
            newVol.setSize(volume.getSize());

            if (vol.getFormat() == PhysicalDiskFormat.RAW) {
                newVol.setFormat(ImageFormat.RAW);
            } else if (vol.getFormat() == PhysicalDiskFormat.QCOW2) {
                newVol.setFormat(ImageFormat.QCOW2);
            }

            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        DataStoreTO srcStore = srcData.getDataStore();
        DataStoreTO destStore = destData.getDataStore();
        VolumeObjectTO srcVol = (VolumeObjectTO)srcData;
        ImageFormat srcFormat = srcVol.getFormat();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destStore;
        if (!(srcStore instanceof NfsTO)) {
            return new CopyCmdAnswer("can only handle nfs storage");
        }
        NfsTO nfsStore = (NfsTO)srcStore;
        String srcVolumePath = srcData.getPath();
        String secondaryStorageUrl = nfsStore.getUrl();
        KVMStoragePool secondaryStoragePool = null;
        KVMStoragePool primaryPool = null;
        try {
            try {
                primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            } catch (CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primaryPool =
                        storagePoolMgr.createStoragePool(primaryStore.getUuid(), primaryStore.getHost(), primaryStore.getPort(), primaryStore.getPath(), null,
                            primaryStore.getPoolType());
                } else {
                    return new CopyCmdAnswer(e.getMessage());
                }
            }

            String volumeName = UUID.randomUUID().toString();

            int index = srcVolumePath.lastIndexOf(File.separator);
            String volumeDir = srcVolumePath.substring(0, index);
            String srcVolumeName = srcVolumePath.substring(index + 1);
            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + File.separator + volumeDir);
            if (!srcVolumeName.endsWith(".qcow2") && srcFormat == ImageFormat.QCOW2) {
                srcVolumeName = srcVolumeName + ".qcow2";
            }
            KVMPhysicalDisk volume = secondaryStoragePool.getPhysicalDisk(srcVolumeName);
            volume.setFormat(PhysicalDiskFormat.valueOf(srcFormat.toString()));
            KVMPhysicalDisk newDisk = storagePoolMgr.copyPhysicalDisk(volume, volumeName, primaryPool, cmd.getWaitInMillSeconds());
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setFormat(ImageFormat.valueOf(newDisk.getFormat().toString().toUpperCase()));
            newVol.setPath(volumeName);
            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        VolumeObjectTO srcVol = (VolumeObjectTO)srcData;
        VolumeObjectTO destVol = (VolumeObjectTO)destData;
        ImageFormat srcFormat = srcVol.getFormat();
        ImageFormat destFormat = destVol.getFormat();
        DataStoreTO srcStore = srcData.getDataStore();
        DataStoreTO destStore = destData.getDataStore();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcStore;
        if (!(destStore instanceof NfsTO)) {
            return new CopyCmdAnswer("can only handle nfs storage");
        }
        NfsTO nfsStore = (NfsTO)destStore;
        String srcVolumePath = srcData.getPath();
        String destVolumePath = destData.getPath();
        String secondaryStorageUrl = nfsStore.getUrl();
        KVMStoragePool secondaryStoragePool = null;

        try {
            String volumeName = UUID.randomUUID().toString();

            String destVolumeName = volumeName + "." + destFormat.getFileExtension();
            KVMPhysicalDisk volume = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), srcVolumePath);
            volume.setFormat(PhysicalDiskFormat.valueOf(srcFormat.toString()));

            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
            secondaryStoragePool.createFolder(destVolumePath);
            storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + File.separator + destVolumePath);
            storagePoolMgr.copyPhysicalDisk(volume, destVolumeName, secondaryStoragePool, cmd.getWaitInMillSeconds());
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(destVolumePath + File.separator + destVolumeName);
            newVol.setFormat(destFormat);
            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        int wait = cmd.getWaitInMillSeconds();
        TemplateObjectTO template = (TemplateObjectTO)destData;
        DataStoreTO imageStore = template.getDataStore();
        VolumeObjectTO volume = (VolumeObjectTO)srcData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO)imageStore;

        KVMStoragePool secondaryStorage = null;
        KVMStoragePool primary = null;
        try {
            String templateFolder = template.getPath();

            secondaryStorage = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl());

            primary = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk disk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volume.getPath());
            String tmpltPath = secondaryStorage.getLocalPath() + File.separator + templateFolder;
            this.storageLayer.mkdirs(tmpltPath);
            String templateName = UUID.randomUUID().toString();

            if (primary.getType() != StoragePoolType.RBD) {
                Script command = new Script(_createTmplPath, wait, s_logger);
                command.add("-f", disk.getPath());
                command.add("-t", tmpltPath);
                command.add("-n", templateName + ".qcow2");

                String result = command.execute();

                if (result != null) {
                    s_logger.debug("failed to create template: " + result);
                    return new CopyCmdAnswer(result);
                }
            } else {
                s_logger.debug("Converting RBD disk " + disk.getPath() + " into template " + templateName);

                QemuImgFile srcFile =
                    new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(), primary.getSourcePort(), primary.getAuthUserName(),
                        primary.getAuthSecret(), disk.getPath()));
                srcFile.setFormat(PhysicalDiskFormat.RAW);

                QemuImgFile destFile = new QemuImgFile(tmpltPath + "/" + templateName + ".qcow2");
                destFile.setFormat(PhysicalDiskFormat.QCOW2);

                QemuImg q = new QemuImg(cmd.getWaitInMillSeconds());
                try {
                    q.convert(srcFile, destFile);
                } catch (QemuImgException e) {
                    s_logger.error("Failed to create new template while converting " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " +
                        e.getMessage());
                }

                File templateProp = new File(tmpltPath + "/template.properties");
                if (!templateProp.exists()) {
                    templateProp.createNewFile();
                }

                String templateContent = "filename=" + templateName + ".qcow2" + System.getProperty("line.separator");

                DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
                Date date = new Date();
                templateContent += "snapshot.name=" + dateFormat.format(date) + System.getProperty("line.separator");

                FileOutputStream templFo = new FileOutputStream(templateProp);
                templFo.write(templateContent.getBytes());
                templFo.flush();
                templFo.close();
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, this.storageLayer);
            Processor qcow2Processor = new QCOW2Processor();

            qcow2Processor.configure("QCOW2 Processor", params);

            FormatInfo info = qcow2Processor.process(tmpltPath, null, templateName);

            TemplateLocation loc = new TemplateLocation(this.storageLayer, tmpltPath);
            loc.create(1, true, templateName);
            loc.addFormat(info);
            loc.save();

            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(templateFolder + File.separator + templateName + ".qcow2");
            newTemplate.setSize(info.virtualSize);
            newTemplate.setPhysicalSize(info.size);
            newTemplate.setFormat(ImageFormat.QCOW2);
            newTemplate.setName(templateName);
            return new CopyCmdAnswer(newTemplate);
        } catch (Exception e) {
            s_logger.debug("Failed to create template from volume: " + e.toString());
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStorage != null) {
                secondaryStorage.delete();
            }
        }
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected String copyToS3(File srcFile, S3TO destStore, String destPath) {
        final String bucket = destStore.getBucketName();

        String key = destPath + S3Utils.SEPARATOR + srcFile.getName();
        putFile(destStore, srcFile, bucket, key);
        return key;
    }

    protected Answer copyToObjectStore(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataStoreTO imageStore = destData.getDataStore();
        NfsTO srcStore = (NfsTO)srcData.getDataStore();
        String srcPath = srcData.getPath();
        int index = srcPath.lastIndexOf(File.separator);
        String srcSnapshotDir = srcPath.substring(0, index);
        String srcFileName = srcPath.substring(index + 1);
        KVMStoragePool srcStorePool = null;
        File srcFile = null;
        try {
            srcStorePool = storagePoolMgr.getStoragePoolByURI(srcStore.getUrl() + File.separator + srcSnapshotDir);
            if (srcStorePool == null) {
                return new CopyCmdAnswer("Can't get store:" + srcStore.getUrl());
            }
            srcFile = new File(srcStorePool.getLocalPath() + File.separator + srcFileName);
            if (!srcFile.exists()) {
                return new CopyCmdAnswer("Can't find src file: " + srcPath);
            }
            String destPath = null;
            if (imageStore instanceof S3TO) {
                destPath = copyToS3(srcFile, (S3TO)imageStore, destData.getPath());
            } else {
                return new CopyCmdAnswer("Unsupported protocol");
            }
            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(destPath);
            return new CopyCmdAnswer(newSnapshot);
        } finally {
            try {
                if (srcFile != null) {
                    srcFile.delete();
                }
                if (srcStorePool != null) {
                    srcStorePool.delete();
                }
            } catch (Exception e) {
                s_logger.debug("Failed to clean up:", e);
            }
        }
    }

    protected Answer backupSnapshotForObjectStore(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataStoreTO imageStore = destData.getDataStore();
        DataTO cacheData = cmd.getCacheTO();
        if (cacheData == null) {
            return new CopyCmdAnswer("Failed to copy to object store without cache store");
        }
        DataStoreTO cacheStore = cacheData.getDataStore();
        ((SnapshotObjectTO)destData).setDataStore(cacheStore);
        CopyCmdAnswer answer = (CopyCmdAnswer)backupSnapshot(cmd);
        if (!answer.getResult()) {
            return answer;
        }
        SnapshotObjectTO snapshotOnCacheStore = (SnapshotObjectTO)answer.getNewData();
        snapshotOnCacheStore.setDataStore(cacheStore);
        ((SnapshotObjectTO)destData).setDataStore(imageStore);
        CopyCommand newCpyCmd = new CopyCommand(snapshotOnCacheStore, destData, cmd.getWaitInMillSeconds(), cmd.executeInSequence());
        return copyToObjectStore(newCpyCmd);
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)snapshot.getDataStore();
        SnapshotObjectTO destSnapshot = (SnapshotObjectTO)destData;
        DataStoreTO imageStore = destData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return backupSnapshotForObjectStore(cmd);
        }
        NfsTO nfsImageStore = (NfsTO)imageStore;

        String secondaryStoragePoolUrl = nfsImageStore.getUrl();
        // NOTE: snapshot name is encoded in snapshot path
        int index = snapshot.getPath().lastIndexOf("/");

        String snapshotName = snapshot.getPath().substring(index + 1);
        String volumePath = snapshot.getVolume().getPath();
        String snapshotDestPath = null;
        String snapshotRelPath = null;
        String vmName = snapshot.getVmName();
        KVMStoragePool secondaryStoragePool = null;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);

            String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = destSnapshot.getPath();

            snapshotDestPath = ssPmountPath + File.separator + snapshotRelPath;
            KVMPhysicalDisk snapshotDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volumePath);
            KVMStoragePool primaryPool = snapshotDisk.getPool();

            /**
             * RBD snapshots can't be copied using qemu-img, so we have to use
             * the Java bindings for librbd here.
             *
             * These bindings will read the snapshot and write the contents to
             * the secondary storage directly
             *
             * It will stop doing so if the amount of time spend is longer then
             * cmds.timeout
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                try {
                    Rados r = new Rados(primaryPool.getAuthUserName());
                    r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                    r.confSet("key", primaryPool.getAuthSecret());
                    r.connect();
                    s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                    IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                    Rbd rbd = new Rbd(io);
                    RbdImage image = rbd.open(snapshotDisk.getName(), snapshotName);

                    long startTime = System.currentTimeMillis() / 1000;

                    File snapDir = new File(snapshotDestPath);
                    s_logger.debug("Attempting to create " + snapDir.getAbsolutePath() + " recursively");
                    FileUtils.forceMkdir(snapDir);

                    File snapFile = new File(snapshotDestPath + "/" + snapshotName);
                    s_logger.debug("Backing up RBD snapshot to " + snapFile.getAbsolutePath());
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(snapFile));
                    int chunkSize = 4194304;
                    long offset = 0;
                    while (true) {
                        byte[] buf = new byte[chunkSize];

                        int bytes = image.read(offset, buf, chunkSize);
                        if (bytes <= 0) {
                            break;
                        }
                        bos.write(buf, 0, bytes);
                        offset += bytes;
                    }
                    s_logger.debug("Completed backing up RBD snapshot " + snapshotName + " to  " + snapFile.getAbsolutePath() + ". Bytes written: " + offset);
                    bos.close();

                    s_logger.debug("Attempting to remove snapshot RBD " + snapshotName + " from image " + snapshotDisk.getName());
                    image.snapRemove(snapshotName);

                    r.ioCtxDestroy(io);
                } catch (RadosException e) {
                    s_logger.error("A RADOS operation failed. The error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                } catch (RbdException e) {
                    s_logger.error("A RBD operation on " + snapshotDisk.getName() + " failed. The error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                } catch (FileNotFoundException e) {
                    s_logger.error("Failed to open " + snapshotDestPath + ". The error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                } catch (IOException e) {
                    s_logger.debug("An I/O error occured during a snapshot operation on " + snapshotDestPath);
                    return new CopyCmdAnswer(e.toString());
                }
            } else {
                Script command = new Script(_manageSnapshotPath, cmd.getWaitInMillSeconds(), s_logger);
                command.add("-b", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                command.add("-p", snapshotDestPath);
                command.add("-t", snapshotName);
                String result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snaptshot: " + result);
                    return new CopyCmdAnswer(result);
                }
            }

            /* Delete the snapshot on primary */

            DomainInfo.DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = this.resource.getDomain(conn, vmName);
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryStorage = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && !primaryStorage.isExternalSnapshot()) {
                DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                snap.delete(0);

                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = this.resource.getDomain(conn, vmName);
                state = vm.getInfo().state;
                if (state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                if (primaryPool.getType() != StoragePoolType.RBD) {
                    Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                    command.add("-d", snapshotDisk.getPath());
                    command.add("-n", snapshotName);
                    String result = command.execute();
                    if (result != null) {
                        s_logger.debug("Failed to backup snapshot: " + result);
                        return new CopyCmdAnswer("Failed to backup snapshot: " + result);
                    }
                }
            }

            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotRelPath + File.separator + snapshotName);
            return new CopyCmdAnswer(newSnapshot);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to backup snapshot: " + e.toString());
            return new CopyCmdAnswer(e.toString());
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to backup snapshot: " + e.toString());
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                secondaryStoragePool.delete();
            }
        }
    }

    protected synchronized String attachOrDetachISO(Connect conn, String vmName, String isoPath, boolean isAttach) throws LibvirtException, URISyntaxException,
        InternalErrorException {
        String isoXml = null;
        if (isoPath != null && isAttach) {
            int index = isoPath.lastIndexOf("/");
            String path = isoPath.substring(0, index);
            String name = isoPath.substring(index + 1);
            KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(path);
            KVMPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            isoPath = isoVol.getPath();

            DiskDef iso = new DiskDef();
            iso.defISODisk(isoPath);
            isoXml = iso.toString();
        } else {
            DiskDef iso = new DiskDef();
            iso.defISODisk(null);
            isoXml = iso.toString();
        }

        List<DiskDef> disks = this.resource.getDisks(conn, vmName);
        String result = attachOrDetachDevice(conn, true, vmName, isoXml);
        if (result == null && !isAttach) {
            for (DiskDef disk : disks) {
                if (disk.getDeviceType() == DiskDef.deviceType.CDROM) {
                    this.resource.cleanupDisk(disk);
                }
            }

        }
        return result;
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        NfsTO nfsStore = (NfsTO)store;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            attachOrDetachISO(conn, cmd.getVmName(), nfsStore.getUrl() + File.separator + isoTO.getPath(), true);
        } catch (LibvirtException e) {
            return new Answer(cmd, false, e.toString());
        } catch (URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        } catch (InternalErrorException e) {
            return new Answer(cmd, false, e.toString());
        }

        return new Answer(cmd);
    }

    @Override
    public Answer dettachIso(DettachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        NfsTO nfsStore = (NfsTO)store;
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            attachOrDetachISO(conn, cmd.getVmName(), nfsStore.getUrl() + File.separator + isoTO.getPath(), false);
        } catch (LibvirtException e) {
            return new Answer(cmd, false, e.toString());
        } catch (URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        } catch (InternalErrorException e) {
            return new Answer(cmd, false, e.toString());
        }

        return new Answer(cmd);
    }

    protected synchronized String attachOrDetachDevice(Connect conn, boolean attach, String vmName, String xml) throws LibvirtException, InternalErrorException {
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);

            if (attach) {
                s_logger.debug("Attaching device: " + xml);
                dm.attachDevice(xml);
            } else {
                s_logger.debug("Detaching device: " + xml);
                dm.detachDevice(xml);
            }
        } catch (LibvirtException e) {
            if (attach) {
                s_logger.warn("Failed to attach device to " + vmName + ": " + e.getMessage());
            } else {
                s_logger.warn("Failed to detach device from " + vmName + ": " + e.getMessage());
            }
            throw e;
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }

        return null;
    }

    protected synchronized String attachOrDetachDisk(Connect conn, boolean attach, String vmName, KVMPhysicalDisk attachingDisk, int devId) throws LibvirtException,
        InternalErrorException {
        List<DiskDef> disks = null;
        Domain dm = null;
        DiskDef diskdef = null;
        KVMStoragePool attachingPool = attachingDisk.getPool();
        try {
            if (!attach) {
                dm = conn.domainLookupByName(vmName);
                LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                String xml = dm.getXMLDesc(0);
                parser.parseDomainXML(xml);
                disks = parser.getDisks();

                for (DiskDef disk : disks) {
                    String file = disk.getDiskPath();
                    if (file != null && file.equalsIgnoreCase(attachingDisk.getPath())) {
                        diskdef = disk;
                        break;
                    }
                }
                if (diskdef == null) {
                    throw new InternalErrorException("disk: " + attachingDisk.getPath() + " is not attached before");
                }
            } else {
                diskdef = new DiskDef();
                if (attachingPool.getType() == StoragePoolType.RBD) {
                    diskdef.defNetworkBasedDisk(attachingDisk.getPath(), attachingPool.getSourceHost(), attachingPool.getSourcePort(), attachingPool.getAuthUserName(),
                        attachingPool.getUuid(), devId, DiskDef.diskBus.VIRTIO, diskProtocol.RBD);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                    diskdef.defFileBasedDisk(attachingDisk.getPath(), devId, DiskDef.diskBus.VIRTIO, DiskDef.diskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    diskdef.defBlockBasedDisk(attachingDisk.getPath(), devId, DiskDef.diskBus.VIRTIO);
                }
            }

            String xml = diskdef.toString();
            return attachOrDetachDevice(conn, attach, vmName, xml);
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    @Override
    public Answer attachVolume(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            storagePoolMgr.connectPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath(), disk.getDetails());

            KVMPhysicalDisk phyDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            attachOrDetachDisk(conn, true, vmName, phyDisk, disk.getDiskSeq().intValue());

            return new AttachAnswer(disk);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to " + e.toString());
            return new AttachAnswer(e.toString());
        } catch (InternalErrorException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to " + e.toString());
            return new AttachAnswer(e.toString());
        }
    }

    @Override
    public Answer dettachVolume(DettachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            KVMPhysicalDisk phyDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            attachOrDetachDisk(conn, false, vmName, phyDisk, disk.getDiskSeq().intValue());

            storagePoolMgr.disconnectPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            return new DettachAnswer(disk);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to " + e.toString());
            return new DettachAnswer(e.toString());
        } catch (InternalErrorException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to " + e.toString());
            return new DettachAnswer(e.toString());
        }
    }

    @Override
    public Answer createVolume(CreateObjectCommand cmd) {
        VolumeObjectTO volume = (VolumeObjectTO)cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();

        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;
        long disksize;
        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            disksize = volume.getSize();

            vol = primaryPool.createPhysicalDisk(volume.getUuid(), disksize);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vol.getName());
            newVol.setSize(volume.getSize());

            /**
             * Volumes on RBD are always in RAW format
             * Hardcode this to RAW since there is no other way right now
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                newVol.setFormat(ImageFormat.RAW);
            }

            return new CreateObjectAnswer(newVol);
        } catch (Exception e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
    }

    protected static MessageFormat SnapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
        + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO)cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)snapshotTO.getDataStore();
        VolumeObjectTO volume = snapshotTO.getVolume();
        String snapshotName = UUID.randomUUID().toString();
        String vmName = volume.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            DomainInfo.DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = this.resource.getDomain(conn, vmName);
                    state = vm.getInfo().state;
                } catch (LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk disk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volume.getPath());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && !primaryPool.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] {snapshotName, vmUuid};
                String snapshot = SnapshotXML.format(args);
                s_logger.debug(snapshot);

                vm.snapshotCreateXML(snapshot);
                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = this.resource.getDomain(conn, vmName);
                state = vm.getInfo().state;
                if (state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                /**
                 * For RBD we can't use libvirt to do our snapshotting or any Bash scripts.
                 * libvirt also wants to store the memory contents of the Virtual Machine,
                 * but that's not possible with RBD since there is no way to store the memory
                 * contents in RBD.
                 *
                 * So we rely on the Java bindings for RBD to create our snapshot
                 *
                 * This snapshot might not be 100% consistent due to writes still being in the
                 * memory of the Virtual Machine, but if the VM runs a kernel which supports
                 * barriers properly (>2.6.32) this won't be any different then pulling the power
                 * cord out of a running machine.
                 */
                if (primaryPool.getType() == StoragePoolType.RBD) {
                    try {
                        Rados r = new Rados(primaryPool.getAuthUserName());
                        r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                        r.confSet("key", primaryPool.getAuthSecret());
                        r.connect();
                        s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                        IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                        Rbd rbd = new Rbd(io);
                        RbdImage image = rbd.open(disk.getName());

                        s_logger.debug("Attempting to create RBD snapshot " + disk.getName() + "@" + snapshotName);
                        image.snapCreate(snapshotName);

                        rbd.close(image);
                        r.ioCtxDestroy(io);
                    } catch (Exception e) {
                        s_logger.error("A RBD snapshot operation on " + disk.getName() + " failed. The error was: " + e.getMessage());
                    }
                } else {
                    /* VM is not running, create a snapshot by ourself */
                    final Script command = new Script(_manageSnapshotPath, this._cmdsTimeout, s_logger);
                    command.add("-c", disk.getPath());
                    command.add("-n", snapshotName);
                    String result = command.execute();
                    if (result != null) {
                        s_logger.debug("Failed to manage snapshot: " + result);
                        return new CreateObjectAnswer("Failed to manage snapshot: " + result);
                    }
                }
            }

            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            // NOTE: sort of hack, we'd better just put snapshtoName
            newSnapshot.setPath(disk.getPath() + File.separator + snapshotName);
            return new CreateObjectAnswer(newSnapshot);
        } catch (LibvirtException e) {
            s_logger.debug("Failed to manage snapshot: " + e.toString());
            return new CreateObjectAnswer("Failed to manage snapshot: " + e.toString());
        }
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        VolumeObjectTO vol = (VolumeObjectTO)cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        try {
            KVMStoragePool pool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            try {
                pool.getPhysicalDisk(vol.getPath());
            } catch (Exception e) {
                s_logger.debug("can't find volume: " + vol.getPath() + ", return true");
                return new Answer(null);
            }
            pool.deletePhysicalDisk(vol.getPath());
            return new Answer(null);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to delete volume: " + e.toString());
            return new Answer(null, false, e.toString());
        }
    }

    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        try {
            DataTO srcData = cmd.getSrcTO();
            SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
            DataTO destData = cmd.getDestTO();
            PrimaryDataStoreTO pool = (PrimaryDataStoreTO)destData.getDataStore();
            DataStoreTO imageStore = srcData.getDataStore();
            VolumeObjectTO volume = snapshot.getVolume();

            if (!(imageStore instanceof NfsTO)) {
                return new CopyCmdAnswer("unsupported protocol");
            }

            NfsTO nfsImageStore = (NfsTO)imageStore;

            String snapshotFullPath = snapshot.getPath();
            int index = snapshotFullPath.lastIndexOf("/");
            String snapshotPath = snapshotFullPath.substring(0, index);
            String snapshotName = snapshotFullPath.substring(index + 1);
            KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl() + File.separator + snapshotPath);
            KVMPhysicalDisk snapshotDisk = secondaryPool.getPhysicalDisk(snapshotName);

            if (volume.getFormat() == ImageFormat.RAW) {
                snapshotDisk.setFormat(PhysicalDiskFormat.RAW);
            } else if (volume.getFormat() == ImageFormat.QCOW2) {
                snapshotDisk.setFormat(PhysicalDiskFormat.QCOW2);
            }

            String primaryUuid = pool.getUuid();
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getPoolType(), primaryUuid);
            String volUuid = UUID.randomUUID().toString();
            KVMPhysicalDisk disk = storagePoolMgr.copyPhysicalDisk(snapshotDisk, volUuid, primaryPool, cmd.getWaitInMillSeconds());
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(disk.getName());
            newVol.setSize(disk.getVirtualSize());

            /**
             * We have to force the format of RBD volumes to RAW
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                newVol.setFormat(ImageFormat.RAW);
            }

            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }
}
