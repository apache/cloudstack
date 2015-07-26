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

import static com.cloud.utils.S3Utils.mputFile;
import static com.cloud.utils.S3Utils.putFile;

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
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdImage;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef.DiskProtocol;
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
    private final KVMStoragePoolManager storagePoolMgr;
    private final LibvirtComputingResource resource;
    private StorageLayer storageLayer;
    private String _createTmplPath;
    private String _manageSnapshotPath;
    private int _cmdsTimeout;

    public KVMStorageProcessor(final KVMStoragePoolManager storagePoolMgr, final LibvirtComputingResource resource) {
        this.storagePoolMgr = storagePoolMgr;
        this.resource = resource;
    }

    protected String getDefaultStorageScriptsDir() {
        return "scripts/storage/qcow2";
    }

    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
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

        final String value = (String)params.get("cmds.timeout");
        _cmdsTimeout = NumbersUtil.parseInt(value, 7200) * 1000;
        return true;
    }

    @Override
    public SnapshotAndCopyAnswer snapshotAndCopy(final SnapshotAndCopyCommand cmd) {
        s_logger.info("'SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand)' not currently used for KVMStorageProcessor");

        return new SnapshotAndCopyAnswer();
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final TemplateObjectTO template = (TemplateObjectTO)srcData;
        final DataStoreTO imageStore = template.getDataStore();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        final NfsTO nfsImageStore = (NfsTO)imageStore;
        final String tmplturl = nfsImageStore.getUrl() + File.separator + template.getPath();
        final int index = tmplturl.lastIndexOf("/");
        final String mountpoint = tmplturl.substring(0, index);
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
                final List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    return new PrimaryStorageDownloadAnswer("Failed to get volumes from pool: " + secondaryPool.getUuid());
                }
                for (final KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        tmplVol = disk;
                        break;
                    }
                }
            } else {
                tmplVol = secondaryPool.getPhysicalDisk(tmpltname);
            }

            if (tmplVol == null) {
                return new PrimaryStorageDownloadAnswer("Failed to get template from pool: " + secondaryPool.getUuid());
            }

            /* Copy volume to primary storage */
            s_logger.debug("Copying template to primary storage, template format is " + tmplVol.getFormat() );
            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk primaryVol = null;
            if (destData instanceof VolumeObjectTO) {
                final VolumeObjectTO volume = (VolumeObjectTO)destData;
                // pass along volume's target size if it's bigger than template's size, for storage types that copy template rather than cloning on deploy
                if (volume.getSize() != null && volume.getSize() > tmplVol.getVirtualSize()) {
                    s_logger.debug("Using configured size of " + volume.getSize());
                    tmplVol.setSize(volume.getSize());
                    tmplVol.setVirtualSize(volume.getSize());
                } else {
                    s_logger.debug("Using template's size of " + tmplVol.getVirtualSize());
                }
                primaryVol = storagePoolMgr.copyPhysicalDisk(tmplVol, volume.getUuid(), primaryPool, cmd.getWaitInMillSeconds());
            } else if (destData instanceof TemplateObjectTO) {
                final TemplateObjectTO destTempl = (TemplateObjectTO)destData;
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
                final TemplateObjectTO newTemplate = new TemplateObjectTO();
                newTemplate.setPath(primaryVol.getName());
                newTemplate.setSize(primaryVol.getSize());
                if (primaryPool.getType() == StoragePoolType.RBD) {
                    newTemplate.setFormat(ImageFormat.RAW);
                } else {
                    newTemplate.setFormat(ImageFormat.QCOW2);
                }
                data = newTemplate;
            } else if (destData.getObjectType() == DataObjectType.VOLUME) {
                final VolumeObjectTO volumeObjectTO = new VolumeObjectTO();
                volumeObjectTO.setPath(primaryVol.getName());
                volumeObjectTO.setSize(primaryVol.getSize());
                if (primaryVol.getFormat() == PhysicalDiskFormat.RAW) {
                    volumeObjectTO.setFormat(ImageFormat.RAW);
                } else if (primaryVol.getFormat() == PhysicalDiskFormat.QCOW2) {
                    volumeObjectTO.setFormat(ImageFormat.QCOW2);
                }
                data = volumeObjectTO;
            }
            return new CopyCmdAnswer(data);
        } catch (final CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        } finally {
            try {
                if (secondaryPool != null) {
                    secondaryPool.delete();
                }
            } catch(final Exception e) {
                s_logger.debug("Failed to clean up secondary storage", e);
            }
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate. copies template direct to root disk
    private KVMPhysicalDisk templateToPrimaryDownload(final String templateUrl, final KVMStoragePool primaryPool, final String volUuid, final Long size, final int timeout) {
        final int index = templateUrl.lastIndexOf("/");
        final String mountpoint = templateUrl.substring(0, index);
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
                final List<KVMPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    s_logger.error("Failed to get volumes from pool: " + secondaryPool.getUuid());
                    return null;
                }
                for (final KVMPhysicalDisk disk : disks) {
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

            if (size > templateVol.getSize()) {
                s_logger.debug("Overriding provided template's size with new size " + size);
                templateVol.setSize(size);
                templateVol.setVirtualSize(size);
            } else {
                s_logger.debug("Using templates disk size of " + templateVol.getVirtualSize() + "since size passed was " + size);
            }

            final KVMPhysicalDisk primaryVol = storagePoolMgr.copyPhysicalDisk(templateVol, volUuid, primaryPool, timeout);
            return primaryVol;
        } catch (final CloudRuntimeException e) {
            s_logger.error("Failed to download template to primary storage", e);
            return null;
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final TemplateObjectTO template = (TemplateObjectTO)srcData;
        final DataStoreTO imageStore = template.getDataStore();
        final VolumeObjectTO volume = (VolumeObjectTO)destData;
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();
        KVMPhysicalDisk BaseVol = null;
        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;

        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            String templatePath = template.getPath();

            if (primaryPool.getType() == StoragePoolType.CLVM) {
                templatePath = ((NfsTO)imageStore).getUrl() + File.separator + templatePath;
                vol = templateToPrimaryDownload(templatePath, primaryPool, volume.getUuid(), volume.getSize(), cmd.getWaitInMillSeconds());
            } else {
                if (templatePath.contains("/mnt")) {
                    //upgrade issue, if the path contains path, need to extract the volume uuid from path
                    templatePath = templatePath.substring(templatePath.lastIndexOf(File.separator) + 1);
                }
                BaseVol = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), templatePath);
                vol = storagePoolMgr.createDiskFromTemplate(BaseVol, volume.getUuid(), volume.getProvisioningType(),
                        BaseVol.getPool(), volume.getSize(), cmd.getWaitInMillSeconds());
            }
            if (vol == null) {
                return new CopyCmdAnswer(" Can't create storage volume on storage pool");
            }

            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vol.getName());
            newVol.setSize(volume.getSize());

            if (vol.getFormat() == PhysicalDiskFormat.RAW) {
                newVol.setFormat(ImageFormat.RAW);
            } else if (vol.getFormat() == PhysicalDiskFormat.QCOW2) {
                newVol.setFormat(ImageFormat.QCOW2);
            } else if (vol.getFormat() == PhysicalDiskFormat.DIR) {
                newVol.setFormat(ImageFormat.DIR);
            }

            return new CopyCmdAnswer(newVol);
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to create volume: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final DataStoreTO srcStore = srcData.getDataStore();
        final DataStoreTO destStore = destData.getDataStore();
        final VolumeObjectTO srcVol = (VolumeObjectTO)srcData;
        final ImageFormat srcFormat = srcVol.getFormat();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destStore;
        if (!(srcStore instanceof NfsTO)) {
            return new CopyCmdAnswer("can only handle nfs storage");
        }
        final NfsTO nfsStore = (NfsTO)srcStore;
        final String srcVolumePath = srcData.getPath();
        final String secondaryStorageUrl = nfsStore.getUrl();
        KVMStoragePool secondaryStoragePool = null;
        KVMStoragePool primaryPool = null;
        try {
            try {
                primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            } catch (final CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primaryPool =
                            storagePoolMgr.createStoragePool(primaryStore.getUuid(), primaryStore.getHost(), primaryStore.getPort(), primaryStore.getPath(), null,
                                    primaryStore.getPoolType());
                } else {
                    return new CopyCmdAnswer(e.getMessage());
                }
            }

            final String volumeName = UUID.randomUUID().toString();

            final int index = srcVolumePath.lastIndexOf(File.separator);
            final String volumeDir = srcVolumePath.substring(0, index);
            String srcVolumeName = srcVolumePath.substring(index + 1);
            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + File.separator + volumeDir);
            if (!srcVolumeName.endsWith(".qcow2") && srcFormat == ImageFormat.QCOW2) {
                srcVolumeName = srcVolumeName + ".qcow2";
            }
            final KVMPhysicalDisk volume = secondaryStoragePool.getPhysicalDisk(srcVolumeName);
            volume.setFormat(PhysicalDiskFormat.valueOf(srcFormat.toString()));
            final KVMPhysicalDisk newDisk = storagePoolMgr.copyPhysicalDisk(volume, volumeName, primaryPool, cmd.getWaitInMillSeconds());
            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setFormat(ImageFormat.valueOf(newDisk.getFormat().toString().toUpperCase()));
            newVol.setPath(volumeName);
            return new CopyCmdAnswer(newVol);
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to ccopyVolumeFromImageCacheToPrimary: ", e);
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final VolumeObjectTO srcVol = (VolumeObjectTO)srcData;
        final VolumeObjectTO destVol = (VolumeObjectTO)destData;
        final ImageFormat srcFormat = srcVol.getFormat();
        final ImageFormat destFormat = destVol.getFormat();
        final DataStoreTO srcStore = srcData.getDataStore();
        final DataStoreTO destStore = destData.getDataStore();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcStore;
        if (!(destStore instanceof NfsTO)) {
            return new CopyCmdAnswer("can only handle nfs storage");
        }
        final NfsTO nfsStore = (NfsTO)destStore;
        final String srcVolumePath = srcData.getPath();
        final String destVolumePath = destData.getPath();
        final String secondaryStorageUrl = nfsStore.getUrl();
        KVMStoragePool secondaryStoragePool = null;

        try {
            final String volumeName = UUID.randomUUID().toString();

            final String destVolumeName = volumeName + "." + destFormat.getFileExtension();
            final KVMPhysicalDisk volume = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), srcVolumePath);
            volume.setFormat(PhysicalDiskFormat.valueOf(srcFormat.toString()));

            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
            secondaryStoragePool.createFolder(destVolumePath);
            storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl + File.separator + destVolumePath);
            storagePoolMgr.copyPhysicalDisk(volume, destVolumeName, secondaryStoragePool, cmd.getWaitInMillSeconds());
            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(destVolumePath + File.separator + destVolumeName);
            newVol.setFormat(destFormat);
            return new CopyCmdAnswer(newVol);
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to copyVolumeFromPrimaryToSecondary: ", e);
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
    }

    @Override
    public Answer createTemplateFromVolume(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWaitInMillSeconds();
        final TemplateObjectTO template = (TemplateObjectTO)destData;
        final DataStoreTO imageStore = template.getDataStore();
        final VolumeObjectTO volume = (VolumeObjectTO)srcData;
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        final NfsTO nfsImageStore = (NfsTO)imageStore;

        KVMStoragePool secondaryStorage = null;
        KVMStoragePool primary = null;
        try {
            final String templateFolder = template.getPath();

            secondaryStorage = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl());

            primary = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            final KVMPhysicalDisk disk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volume.getPath());
            final String tmpltPath = secondaryStorage.getLocalPath() + File.separator + templateFolder;
            storageLayer.mkdirs(tmpltPath);
            final String templateName = UUID.randomUUID().toString();

            if (primary.getType() != StoragePoolType.RBD) {
                final Script command = new Script(_createTmplPath, wait, s_logger);
                command.add("-f", disk.getPath());
                command.add("-t", tmpltPath);
                command.add("-n", templateName + ".qcow2");

                final String result = command.execute();

                if (result != null) {
                    s_logger.debug("failed to create template: " + result);
                    return new CopyCmdAnswer(result);
                }
            } else {
                s_logger.debug("Converting RBD disk " + disk.getPath() + " into template " + templateName);

                final QemuImgFile srcFile =
                        new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(), primary.getSourcePort(), primary.getAuthUserName(),
                                primary.getAuthSecret(), disk.getPath()));
                srcFile.setFormat(PhysicalDiskFormat.RAW);

                final QemuImgFile destFile = new QemuImgFile(tmpltPath + "/" + templateName + ".qcow2");
                destFile.setFormat(PhysicalDiskFormat.QCOW2);

                final QemuImg q = new QemuImg(cmd.getWaitInMillSeconds());
                try {
                    q.convert(srcFile, destFile);
                } catch (final QemuImgException e) {
                    final String message = "Failed to create new template while converting " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " +
                            e.getMessage();

                    throw new QemuImgException(message);
                }

                final File templateProp = new File(tmpltPath + "/template.properties");
                if (!templateProp.exists()) {
                    templateProp.createNewFile();
                }

                String templateContent = "filename=" + templateName + ".qcow2" + System.getProperty("line.separator");

                final DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
                final Date date = new Date();
                templateContent += "snapshot.name=" + dateFormat.format(date) + System.getProperty("line.separator");


                try(FileOutputStream templFo = new FileOutputStream(templateProp);){
                    templFo.write(templateContent.getBytes());
                    templFo.flush();
                } catch (final IOException e) {
                    throw e;
                }
            }

            final Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, storageLayer);
            final Processor qcow2Processor = new QCOW2Processor();

            qcow2Processor.configure("QCOW2 Processor", params);

            final FormatInfo info = qcow2Processor.process(tmpltPath, null, templateName);

            final TemplateLocation loc = new TemplateLocation(storageLayer, tmpltPath);
            loc.create(1, true, templateName);
            loc.addFormat(info);
            loc.save();

            final TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(templateFolder + File.separator + templateName + ".qcow2");
            newTemplate.setSize(info.virtualSize);
            newTemplate.setPhysicalSize(info.size);
            newTemplate.setFormat(ImageFormat.QCOW2);
            newTemplate.setName(templateName);
            return new CopyCmdAnswer(newTemplate);

        } catch (final QemuImgException e) {
            s_logger.error(e.getMessage());
            return new CopyCmdAnswer(e.toString());
        } catch (final IOException e) {
            s_logger.debug("Failed to createTemplateFromVolume: ", e);
            return new CopyCmdAnswer(e.toString());
        } catch (final Exception e) {
            s_logger.debug("Failed to createTemplateFromVolume: ", e);
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryStorage != null) {
                secondaryStorage.delete();
            }
        }
    }

    @Override
    public Answer createTemplateFromSnapshot(final CopyCommand cmd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected String copyToS3(final File srcFile, final S3TO destStore, final String destPath) throws InterruptedException {
        final String bucket = destStore.getBucketName();

        final long srcSize = srcFile.length();
        final String key = destPath + S3Utils.SEPARATOR + srcFile.getName();
        if (!destStore.getSingleUpload(srcSize)) {
            mputFile(destStore, srcFile, bucket, key);
        } else {
            putFile(destStore, srcFile, bucket, key);
        }
        return key;
    }

    protected Answer copyToObjectStore(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final DataStoreTO imageStore = destData.getDataStore();
        final NfsTO srcStore = (NfsTO)srcData.getDataStore();
        final String srcPath = srcData.getPath();
        final int index = srcPath.lastIndexOf(File.separator);
        final String srcSnapshotDir = srcPath.substring(0, index);
        final String srcFileName = srcPath.substring(index + 1);
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
            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(destPath);
            return new CopyCmdAnswer(newSnapshot);
        } catch (final Exception e) {
            s_logger.error("failed to upload" + srcPath, e);
            return new CopyCmdAnswer("failed to upload" + srcPath + e.toString());
        } finally {
            try {
                if (srcFile != null) {
                    srcFile.delete();
                }
                if (srcStorePool != null) {
                    srcStorePool.delete();
                }
            } catch (final Exception e) {
                s_logger.debug("Failed to clean up:", e);
            }
        }
    }

    protected Answer backupSnapshotForObjectStore(final CopyCommand cmd) {
        final DataTO destData = cmd.getDestTO();
        final DataStoreTO imageStore = destData.getDataStore();
        final DataTO cacheData = cmd.getCacheTO();
        if (cacheData == null) {
            return new CopyCmdAnswer("Failed to copy to object store without cache store");
        }
        final DataStoreTO cacheStore = cacheData.getDataStore();
        ((SnapshotObjectTO)destData).setDataStore(cacheStore);
        final CopyCmdAnswer answer = (CopyCmdAnswer)backupSnapshot(cmd);
        if (!answer.getResult()) {
            return answer;
        }
        final SnapshotObjectTO snapshotOnCacheStore = (SnapshotObjectTO)answer.getNewData();
        snapshotOnCacheStore.setDataStore(cacheStore);
        ((SnapshotObjectTO)destData).setDataStore(imageStore);
        final CopyCommand newCpyCmd = new CopyCommand(snapshotOnCacheStore, destData, cmd.getWaitInMillSeconds(), cmd.executeInSequence());
        return copyToObjectStore(newCpyCmd);
    }

    @Override
    public Answer backupSnapshot(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)snapshot.getDataStore();
        final SnapshotObjectTO destSnapshot = (SnapshotObjectTO)destData;
        final DataStoreTO imageStore = destData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return backupSnapshotForObjectStore(cmd);
        }
        final NfsTO nfsImageStore = (NfsTO)imageStore;

        final String secondaryStoragePoolUrl = nfsImageStore.getUrl();
        // NOTE: snapshot name is encoded in snapshot path
        final int index = snapshot.getPath().lastIndexOf("/");

        final String snapshotName = snapshot.getPath().substring(index + 1);
        final String volumePath = snapshot.getVolume().getPath();
        String snapshotDestPath = null;
        String snapshotRelPath = null;
        final String vmName = snapshot.getVmName();
        KVMStoragePool secondaryStoragePool = null;
        Connect conn = null;
        KVMPhysicalDisk snapshotDisk = null;
        KVMStoragePool primaryPool = null;
        try {
            conn = LibvirtConnection.getConnectionByVmName(vmName);

            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);

            final String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = destSnapshot.getPath();

            snapshotDestPath = ssPmountPath + File.separator + snapshotRelPath;
            snapshotDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volumePath);
            primaryPool = snapshotDisk.getPool();

            long size = 0;
            /**
             * Since Ceph version Dumpling (0.67.X) librbd / Qemu supports converting RBD
             * snapshots to RAW/QCOW2 files directly.
             *
             * This reduces the amount of time and storage it takes to back up a snapshot dramatically
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                final String rbdSnapshot = snapshotDisk.getPath() +  "@" + snapshotName;
                final String snapshotFile = snapshotDestPath + "/" + snapshotName;
                try {
                    s_logger.debug("Attempting to backup RBD snapshot " + rbdSnapshot);

                    final File snapDir = new File(snapshotDestPath);
                    s_logger.debug("Attempting to create " + snapDir.getAbsolutePath() + " recursively for snapshot storage");
                    FileUtils.forceMkdir(snapDir);

                    final QemuImgFile srcFile =
                            new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primaryPool.getSourceHost(), primaryPool.getSourcePort(), primaryPool.getAuthUserName(),
                                    primaryPool.getAuthSecret(), rbdSnapshot));
                    srcFile.setFormat(PhysicalDiskFormat.RAW);

                    final QemuImgFile destFile = new QemuImgFile(snapshotFile);
                    destFile.setFormat(snapshotDisk.getFormat());

                    s_logger.debug("Backing up RBD snapshot " + rbdSnapshot + " to " + snapshotFile);
                    final QemuImg q = new QemuImg(cmd.getWaitInMillSeconds());
                    q.convert(srcFile, destFile);

                    final File snapFile = new File(snapshotFile);
                    if(snapFile.exists()) {
                        size = snapFile.length();
                    }

                    s_logger.debug("Finished backing up RBD snapshot " + rbdSnapshot + " to " + snapshotFile + " Snapshot size: " + size);
                } catch (final FileNotFoundException e) {
                    s_logger.error("Failed to open " + snapshotDestPath + ". The error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                } catch (final IOException e) {
                    s_logger.error("Failed to create " + snapshotDestPath + ". The error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                }  catch (final QemuImgException e) {
                    s_logger.error("Failed to backup the RBD snapshot from " + rbdSnapshot +
                            " to " + snapshotFile + " the error was: " + e.getMessage());
                    return new CopyCmdAnswer(e.toString());
                }
            } else {
                final Script command = new Script(_manageSnapshotPath, cmd.getWaitInMillSeconds(), s_logger);
                command.add("-b", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                command.add("-p", snapshotDestPath);
                command.add("-t", snapshotName);
                final String result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snaptshot: " + result);
                    return new CopyCmdAnswer(result);
                }
                final File snapFile = new File(snapshotDestPath + "/" + snapshotName);
                if(snapFile.exists()){
                    size = snapFile.length();
                }
            }

            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(snapshotRelPath + File.separator + snapshotName);
            newSnapshot.setPhysicalSize(size);
            return new CopyCmdAnswer(newSnapshot);
        } catch (final LibvirtException e) {
            s_logger.debug("Failed to backup snapshot: ", e);
            return new CopyCmdAnswer(e.toString());
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to backup snapshot: ", e);
            return new CopyCmdAnswer(e.toString());
        } finally {
            try {
                /* Delete the snapshot on primary */
                DomainInfo.DomainState state = null;
                Domain vm = null;
                if (vmName != null) {
                    try {
                        vm = resource.getDomain(conn, vmName);
                        state = vm.getInfo().state;
                    } catch (final LibvirtException e) {
                        s_logger.trace("Ignoring libvirt error.", e);
                    }
                }

                final KVMStoragePool primaryStorage = storagePoolMgr.getStoragePool(primaryStore.getPoolType(),
                        primaryStore.getUuid());
                if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && !primaryStorage.isExternalSnapshot()) {
                    final DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                    snap.delete(0);

                    /*
                     * libvirt on RHEL6 doesn't handle resume event emitted from
                     * qemu
                     */
                    vm = resource.getDomain(conn, vmName);
                    state = vm.getInfo().state;
                    if (state == DomainInfo.DomainState.VIR_DOMAIN_PAUSED) {
                        vm.resume();
                    }
                } else {
                    if (primaryPool.getType() != StoragePoolType.RBD) {
                        final Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                        command.add("-d", snapshotDisk.getPath());
                        command.add("-n", snapshotName);
                        final String result = command.execute();
                        if (result != null) {
                            s_logger.debug("Failed to delete snapshot on primary: " + result);
                            // return new CopyCmdAnswer("Failed to backup snapshot: " + result);
                        }
                    }
                }
            } catch (final Exception ex) {
                s_logger.debug("Failed to delete snapshots on primary", ex);
            }

            try {
                if (secondaryStoragePool != null) {
                    secondaryStoragePool.delete();
                }
            } catch (final Exception ex) {
                s_logger.debug("Failed to delete secondary storage", ex);
            }
        }
    }

    protected synchronized String attachOrDetachISO(final Connect conn, final String vmName, String isoPath, final boolean isAttach) throws LibvirtException, URISyntaxException,
    InternalErrorException {
        String isoXml = null;
        if (isoPath != null && isAttach) {
            final int index = isoPath.lastIndexOf("/");
            final String path = isoPath.substring(0, index);
            final String name = isoPath.substring(index + 1);
            final KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(path);
            final KVMPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            isoPath = isoVol.getPath();

            final DiskDef iso = new DiskDef();
            iso.defISODisk(isoPath);
            isoXml = iso.toString();
        } else {
            final DiskDef iso = new DiskDef();
            iso.defISODisk(null);
            isoXml = iso.toString();
        }

        final List<DiskDef> disks = resource.getDisks(conn, vmName);
        final String result = attachOrDetachDevice(conn, true, vmName, isoXml);
        if (result == null && !isAttach) {
            for (final DiskDef disk : disks) {
                if (disk.getDeviceType() == DiskDef.DeviceType.CDROM) {
                    resource.cleanupDisk(disk);
                }
            }

        }
        return result;
    }

    @Override
    public Answer attachIso(final AttachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        final DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        final NfsTO nfsStore = (NfsTO)store;
        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            attachOrDetachISO(conn, cmd.getVmName(), nfsStore.getUrl() + File.separator + isoTO.getPath(), true);
        } catch (final LibvirtException e) {
            return new Answer(cmd, false, e.toString());
        } catch (final URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        } catch (final InternalErrorException e) {
            return new Answer(cmd, false, e.toString());
        }

        return new Answer(cmd);
    }

    @Override
    public Answer dettachIso(final DettachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final TemplateObjectTO isoTO = (TemplateObjectTO)disk.getData();
        final DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        final NfsTO nfsStore = (NfsTO)store;
        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(cmd.getVmName());
            attachOrDetachISO(conn, cmd.getVmName(), nfsStore.getUrl() + File.separator + isoTO.getPath(), false);
        } catch (final LibvirtException e) {
            return new Answer(cmd, false, e.toString());
        } catch (final URISyntaxException e) {
            return new Answer(cmd, false, e.toString());
        } catch (final InternalErrorException e) {
            return new Answer(cmd, false, e.toString());
        }

        return new Answer(cmd);
    }

    protected synchronized String attachOrDetachDevice(final Connect conn, final boolean attach, final String vmName, final String xml) throws LibvirtException, InternalErrorException {
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
        } catch (final LibvirtException e) {
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
                } catch (final LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }

        return null;
    }

    protected synchronized String attachOrDetachDisk(final Connect conn, final boolean attach, final String vmName, final KVMPhysicalDisk attachingDisk, final int devId) throws LibvirtException,
    InternalErrorException {
        List<DiskDef> disks = null;
        Domain dm = null;
        DiskDef diskdef = null;
        final KVMStoragePool attachingPool = attachingDisk.getPool();
        try {
            if (!attach) {
                dm = conn.domainLookupByName(vmName);
                final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                final String xml = dm.getXMLDesc(0);
                parser.parseDomainXML(xml);
                disks = parser.getDisks();

                if (attachingPool.getType() == StoragePoolType.RBD) {
                    if (resource.getHypervisorType() == Hypervisor.HypervisorType.LXC) {
                        final String device = resource.mapRbdDevice(attachingDisk);
                        if (device != null) {
                            s_logger.debug("RBD device on host is: "+device);
                            attachingDisk.setPath(device);
                        }
                    }
                }

                for (final DiskDef disk : disks) {
                    final String file = disk.getDiskPath();
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
                    if(resource.getHypervisorType() == Hypervisor.HypervisorType.LXC){
                        // For LXC, map image to host and then attach to Vm
                        final String device = resource.mapRbdDevice(attachingDisk);
                        if (device != null) {
                            s_logger.debug("RBD device on host is: "+device);
                            diskdef.defBlockBasedDisk(device, devId, DiskDef.DiskBus.VIRTIO);
                        } else {
                            throw new InternalErrorException("Error while mapping disk "+attachingDisk.getPath()+" on host");
                        }
                    } else {
                        diskdef.defNetworkBasedDisk(attachingDisk.getPath(), attachingPool.getSourceHost(), attachingPool.getSourcePort(), attachingPool.getAuthUserName(),
                                attachingPool.getUuid(), devId, DiskDef.DiskBus.VIRTIO, DiskProtocol.RBD, DiskDef.DiskFmtType.RAW);
                    }
                } else if (attachingPool.getType() == StoragePoolType.Gluster) {
                    final String mountpoint = attachingPool.getLocalPath();
                    final String path = attachingDisk.getPath();
                    final String glusterVolume = attachingPool.getSourceDir().replace("/", "");
                    diskdef.defNetworkBasedDisk(glusterVolume + path.replace(mountpoint, ""), attachingPool.getSourceHost(), attachingPool.getSourcePort(), null,
                            null, devId, DiskDef.DiskBus.VIRTIO, DiskProtocol.GLUSTER, DiskDef.DiskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                    diskdef.defFileBasedDisk(attachingDisk.getPath(), devId, DiskDef.DiskBus.VIRTIO, DiskDef.DiskFmtType.QCOW2);
                } else if (attachingDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    diskdef.defBlockBasedDisk(attachingDisk.getPath(), devId, DiskDef.DiskBus.VIRTIO);
                }
            }

            final String xml = diskdef.toString();
            return attachOrDetachDevice(conn, attach, vmName, xml);
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    @Override
    public Answer attachVolume(final AttachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        final String vmName = cmd.getVmName();
        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            storagePoolMgr.connectPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath(), disk.getDetails());

            final KVMPhysicalDisk phyDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            attachOrDetachDisk(conn, true, vmName, phyDisk, disk.getDiskSeq().intValue());

            return new AttachAnswer(disk);
        } catch (final LibvirtException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to ", e);
            storagePoolMgr.disconnectPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());
            return new AttachAnswer(e.toString());
        } catch (final InternalErrorException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to ", e);
            return new AttachAnswer(e.toString());
        }
    }

    @Override
    public Answer dettachVolume(final DettachCommand cmd) {
        final DiskTO disk = cmd.getDisk();
        final VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        final String vmName = cmd.getVmName();
        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            final KVMPhysicalDisk phyDisk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            attachOrDetachDisk(conn, false, vmName, phyDisk, disk.getDiskSeq().intValue());

            storagePoolMgr.disconnectPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), vol.getPath());

            return new DettachAnswer(disk);
        } catch (final LibvirtException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to ", e);
            return new DettachAnswer(e.toString());
        } catch (final InternalErrorException e) {
            s_logger.debug("Failed to attach volume: " + vol.getPath() + ", due to ", e);
            return new DettachAnswer(e.toString());
        }
    }

    @Override
    public Answer createVolume(final CreateObjectCommand cmd) {
        final VolumeObjectTO volume = (VolumeObjectTO)cmd.getData();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();

        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;
        long disksize;
        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            disksize = volume.getSize();
            PhysicalDiskFormat format;
            if (volume.getFormat() == null) {
                format = primaryPool.getDefaultFormat();
            } else {
                format = PhysicalDiskFormat.valueOf(volume.getFormat().toString().toUpperCase());
            }
            vol = primaryPool.createPhysicalDisk(volume.getUuid(), format,
                    volume.getProvisioningType(), disksize);

            final VolumeObjectTO newVol = new VolumeObjectTO();
            if(vol != null) {
                newVol.setPath(vol.getName());
            }
            newVol.setSize(volume.getSize());
            newVol.setFormat(ImageFormat.valueOf(format.toString().toUpperCase()));

            return new CreateObjectAnswer(newVol);
        } catch (final Exception e) {
            s_logger.debug("Failed to create volume: ", e);
            return new CreateObjectAnswer(e.toString());
        }
    }

    protected static final MessageFormat SnapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
            + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

    @Override
    public Answer createSnapshot(final CreateObjectCommand cmd) {
        final SnapshotObjectTO snapshotTO = (SnapshotObjectTO)cmd.getData();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)snapshotTO.getDataStore();
        final VolumeObjectTO volume = snapshotTO.getVolume();
        final String snapshotName = UUID.randomUUID().toString();
        final String vmName = volume.getVmName();
        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            DomainInfo.DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = resource.getDomain(conn, vmName);
                    state = vm.getInfo().state;
                } catch (final LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            final KVMPhysicalDisk disk = storagePoolMgr.getPhysicalDisk(primaryStore.getPoolType(), primaryStore.getUuid(), volume.getPath());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && !primaryPool.isExternalSnapshot()) {
                final String vmUuid = vm.getUUIDString();
                final Object[] args = new Object[] {snapshotName, vmUuid};
                final String snapshot = SnapshotXML.format(args);

                final long start = System.currentTimeMillis();
                vm.snapshotCreateXML(snapshot);
                final long total = (System.currentTimeMillis() - start)/1000;
                s_logger.debug("snapshot takes " + total + " seconds to finish");

                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = resource.getDomain(conn, vmName);
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
                        final Rados r = new Rados(primaryPool.getAuthUserName());
                        r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                        r.confSet("key", primaryPool.getAuthSecret());
                        r.confSet("client_mount_timeout", "30");
                        r.connect();
                        s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                        final IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                        final Rbd rbd = new Rbd(io);
                        final RbdImage image = rbd.open(disk.getName());

                        s_logger.debug("Attempting to create RBD snapshot " + disk.getName() + "@" + snapshotName);
                        image.snapCreate(snapshotName);

                        rbd.close(image);
                        r.ioCtxDestroy(io);
                    } catch (final Exception e) {
                        s_logger.error("A RBD snapshot operation on " + disk.getName() + " failed. The error was: " + e.getMessage());
                    }
                } else {
                    /* VM is not running, create a snapshot by ourself */
                    final Script command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                    command.add("-c", disk.getPath());
                    command.add("-n", snapshotName);
                    final String result = command.execute();
                    if (result != null) {
                        s_logger.debug("Failed to manage snapshot: " + result);
                        return new CreateObjectAnswer("Failed to manage snapshot: " + result);
                    }
                }
            }

            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            // NOTE: sort of hack, we'd better just put snapshtoName
            newSnapshot.setPath(disk.getPath() + File.separator + snapshotName);
            return new CreateObjectAnswer(newSnapshot);
        } catch (final LibvirtException e) {
            s_logger.debug("Failed to manage snapshot: ", e);
            return new CreateObjectAnswer("Failed to manage snapshot: " + e.toString());
        }
    }

    @Override
    public Answer deleteVolume(final DeleteCommand cmd) {
        final VolumeObjectTO vol = (VolumeObjectTO)cmd.getData();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)vol.getDataStore();
        try {
            final KVMStoragePool pool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            try {
                pool.getPhysicalDisk(vol.getPath());
            } catch (final Exception e) {
                s_logger.debug("can't find volume: " + vol.getPath() + ", return true");
                return new Answer(null);
            }
            pool.deletePhysicalDisk(vol.getPath(), vol.getFormat());
            return new Answer(null);
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to delete volume: ", e);
            return new Answer(null, false, e.toString());
        }
    }

    @Override
    public Answer createVolumeFromSnapshot(final CopyCommand cmd) {
        try {
            final DataTO srcData = cmd.getSrcTO();
            final SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
            final DataTO destData = cmd.getDestTO();
            final PrimaryDataStoreTO pool = (PrimaryDataStoreTO)destData.getDataStore();
            final DataStoreTO imageStore = srcData.getDataStore();
            final VolumeObjectTO volume = snapshot.getVolume();

            if (!(imageStore instanceof NfsTO)) {
                return new CopyCmdAnswer("unsupported protocol");
            }

            final NfsTO nfsImageStore = (NfsTO)imageStore;

            final String snapshotFullPath = snapshot.getPath();
            final int index = snapshotFullPath.lastIndexOf("/");
            final String snapshotPath = snapshotFullPath.substring(0, index);
            final String snapshotName = snapshotFullPath.substring(index + 1);
            final KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl() + File.separator + snapshotPath);
            final KVMPhysicalDisk snapshotDisk = secondaryPool.getPhysicalDisk(snapshotName);

            if (volume.getFormat() == ImageFormat.RAW) {
                snapshotDisk.setFormat(PhysicalDiskFormat.RAW);
            } else if (volume.getFormat() == ImageFormat.QCOW2) {
                snapshotDisk.setFormat(PhysicalDiskFormat.QCOW2);
            }

            final String primaryUuid = pool.getUuid();
            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getPoolType(), primaryUuid);
            final String volUuid = UUID.randomUUID().toString();
            final KVMPhysicalDisk disk = storagePoolMgr.copyPhysicalDisk(snapshotDisk, volUuid, primaryPool, cmd.getWaitInMillSeconds());
            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(disk.getName());
            newVol.setSize(disk.getVirtualSize());
            newVol.setFormat(ImageFormat.valueOf(disk.getFormat().toString().toUpperCase()));

            return new CopyCmdAnswer(newVol);
        } catch (final CloudRuntimeException e) {
            s_logger.debug("Failed to createVolumeFromSnapshot: ", e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer deleteSnapshot(final DeleteCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer introduceObject(final IntroduceObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

    @Override
    public Answer forgetObject(final ForgetObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

}
