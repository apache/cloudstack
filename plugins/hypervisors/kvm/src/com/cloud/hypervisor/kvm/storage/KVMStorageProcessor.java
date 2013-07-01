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

import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
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

        String storageScriptsDir = (String) params.get("storage.scripts.dir");
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

        String value = (String) params.get("cmds.timeout");
        _cmdsTimeout = NumbersUtil.parseInt(value, 7200) * 1000;
        return true;
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        TemplateObjectTO template = (TemplateObjectTO) srcData;
        DataStoreTO imageStore = template.getDataStore();
        TemplateObjectTO volume = (TemplateObjectTO) destData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO nfsImageStore = (NfsTO) imageStore;
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
                    return new PrimaryStorageDownloadAnswer("Failed to get volumes from pool: "
                            + secondaryPool.getUuid());
                }
                for (KVMPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        tmplVol = disk;
                        break;
                    }
                }
                if (tmplVol == null) {
                    return new PrimaryStorageDownloadAnswer("Failed to get template from pool: "
                            + secondaryPool.getUuid());
                }
            } else {
                tmplVol = secondaryPool.getPhysicalDisk(tmpltname);
            }

            /* Copy volume to primary storage */
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(),
                    primaryStore.getUuid());

            KVMPhysicalDisk primaryVol = storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(),
                    primaryPool);

            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(primaryVol.getName());
            newTemplate.setFormat(ImageFormat.QCOW2);
            return new CopyCmdAnswer(newTemplate);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        } finally {
            if (secondaryPool != null) {
                secondaryPool.delete();
            }
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate
    private KVMPhysicalDisk templateToPrimaryDownload(String templateUrl, KVMStoragePool primaryPool) {
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

            KVMPhysicalDisk primaryVol = storagePoolMgr.copyPhysicalDisk(templateVol, UUID.randomUUID().toString(),
                    primaryPool);
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
        TemplateObjectTO template = (TemplateObjectTO) srcData;
        DataStoreTO imageStore = template.getDataStore();
        VolumeObjectTO volume = (VolumeObjectTO) destData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();
        KVMPhysicalDisk BaseVol = null;
        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;

        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            String templatePath = template.getPath();

            if (primaryPool.getType() == StoragePoolType.CLVM) {
                vol = templateToPrimaryDownload(templatePath, primaryPool);
            } else {
                BaseVol = primaryPool.getPhysicalDisk(templatePath);
                vol = storagePoolMgr.createDiskFromTemplate(BaseVol, UUID.randomUUID().toString(), primaryPool);
            }
            if (vol == null) {
                return new CopyCmdAnswer(" Can't create storage volume on storage pool");
            }

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vol.getName());
            newVol.setSize(vol.getSize());

            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        return null;
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        int wait = cmd.getWait();
        TemplateObjectTO template = (TemplateObjectTO) destData;
        DataStoreTO imageStore = template.getDataStore();
        VolumeObjectTO volume = (VolumeObjectTO) srcData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO) imageStore;

        KVMStoragePool secondaryStorage = null;
        KVMStoragePool primary = null;
        try {
            String templateFolder = template.getPath();

            secondaryStorage = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl());

            try {
                primary = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            } catch (CloudRuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    primary = storagePoolMgr.createStoragePool(primaryStore.getUuid(), primaryStore.getHost(),
                            primaryStore.getPort(), primaryStore.getPath(), null, primaryStore.getPoolType());
                } else {
                    return new CopyCmdAnswer(e.getMessage());
                }
            }

            KVMPhysicalDisk disk = primary.getPhysicalDisk(volume.getPath());
            String tmpltPath = secondaryStorage.getLocalPath() + File.separator + templateFolder;
            this.storageLayer.mkdirs(tmpltPath);
            String templateName = UUID.randomUUID().toString();

            if (primary.getType() != StoragePoolType.RBD) {
                Script command = new Script(_createTmplPath, wait * 1000, s_logger);
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

                QemuImgFile srcFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(primary.getSourceHost(),
                        primary.getSourcePort(), primary.getAuthUserName(), primary.getAuthSecret(), disk.getPath()));
                srcFile.setFormat(PhysicalDiskFormat.RAW);

                QemuImgFile destFile = new QemuImgFile(tmpltPath + "/" + templateName + ".qcow2");
                destFile.setFormat(PhysicalDiskFormat.QCOW2);

                QemuImg q = new QemuImg();
                try {
                    q.convert(srcFile, destFile);
                } catch (QemuImgException e) {
                    s_logger.error("Failed to create new template while converting " + srcFile.getFileName() + " to "
                            + destFile.getFileName() + " the error was: " + e.getMessage());
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
            newTemplate.setFormat(ImageFormat.QCOW2);
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
    public Answer backupSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        DataTO destData = cmd.getDestTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO) srcData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) snapshot.getDataStore();
        SnapshotObjectTO destSnapshot = (SnapshotObjectTO) destData;
        DataStoreTO imageStore = destData.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO) imageStore;

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
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(),
                    primaryStore.getUuid());
            KVMPhysicalDisk snapshotDisk = primaryPool.getPhysicalDisk(volumePath);
            Script command = new Script(_manageSnapshotPath, cmd.getWait() * 1000, s_logger);
            command.add("-b", snapshotDisk.getPath());
            command.add("-n", snapshotName);
            command.add("-p", snapshotDestPath);
            command.add("-t", snapshotName);
            String result = command.execute();
            if (result != null) {
                s_logger.debug("Failed to backup snaptshot: " + result);
                return new CopyCmdAnswer(result);
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

            KVMStoragePool primaryStorage = storagePoolMgr.getStoragePool(primaryStore.getPoolType(),
                    primaryStore.getUuid());
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
                command = new Script(_manageSnapshotPath, _cmdsTimeout, s_logger);
                command.add("-d", snapshotDisk.getPath());
                command.add("-n", snapshotName);
                result = command.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snapshot: " + result);
                    return new CopyCmdAnswer("Failed to backup snapshot: " + result);
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

    protected synchronized String attachOrDetachISO(Connect conn, String vmName, String isoPath, boolean isAttach)
            throws LibvirtException, URISyntaxException, InternalErrorException {
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
                    this.resource.cleanupDisk(conn, disk);
                }
            }

        }
        return result;
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        DiskTO disk = cmd.getDisk();
        TemplateObjectTO isoTO = (TemplateObjectTO) disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        NfsTO nfsStore = (NfsTO) store;
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
        TemplateObjectTO isoTO = (TemplateObjectTO) disk.getData();
        DataStoreTO store = isoTO.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new AttachAnswer("unsupported protocol");
        }
        NfsTO nfsStore = (NfsTO) store;
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

    protected synchronized String attachOrDetachDevice(Connect conn, boolean attach, String vmName, String xml)
            throws LibvirtException, InternalErrorException {
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

    protected synchronized String attachOrDetachDisk(Connect conn, boolean attach, String vmName,
            KVMPhysicalDisk attachingDisk, int devId) throws LibvirtException, InternalErrorException {
        List<DiskDef> disks = null;
        Domain dm = null;
        DiskDef diskdef = null;
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
                if (attachingDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                    diskdef.defFileBasedDisk(attachingDisk.getPath(), devId, DiskDef.diskBus.VIRTIO,
                            DiskDef.diskFmtType.QCOW2);
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
        VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) vol.getDataStore();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            KVMStoragePool primary = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk phyDisk = primary.getPhysicalDisk(vol.getPath());
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
        VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) vol.getDataStore();
        String vmName = cmd.getVmName();
        try {
            Connect conn = LibvirtConnection.getConnectionByVmName(vmName);
            KVMStoragePool primary = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());

            KVMPhysicalDisk phyDisk = primary.getPhysicalDisk(vol.getPath());
            attachOrDetachDisk(conn, false, vmName, phyDisk, disk.getDiskSeq().intValue());

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
        VolumeObjectTO volume = (VolumeObjectTO) cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) volume.getDataStore();

        KVMStoragePool primaryPool = null;
        KVMPhysicalDisk vol = null;
        long disksize;
        try {
            primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(), primaryStore.getUuid());
            disksize = volume.getSize();

            vol = primaryPool.createPhysicalDisk(UUID.randomUUID().toString(), disksize);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vol.getName());

            return new CreateObjectAnswer(newVol);
        } catch (Exception e) {
            s_logger.debug("Failed to create volume: " + e.toString());
            return new CreateObjectAnswer(e.toString());
        }
    }

    protected static MessageFormat SnapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>"
            + "          <domain>" + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        SnapshotObjectTO snapshotTO = (SnapshotObjectTO) cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) snapshotTO.getDataStore();
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

            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(primaryStore.getPoolType(),
                    primaryStore.getUuid());

            if (primaryPool.getType() == StoragePoolType.RBD) {
                s_logger.debug("Snapshots are not supported on RBD volumes");
                return new CreateObjectAnswer("Snapshots are not supported on RBD volumes");
            }

            KVMPhysicalDisk disk = primaryPool.getPhysicalDisk(volume.getPath());
            if (state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING && !primaryPool.isExternalSnapshot()) {
                String vmUuid = vm.getUUIDString();
                Object[] args = new Object[] { snapshotName, vmUuid };
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
        VolumeObjectTO vol = (VolumeObjectTO) cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO) vol.getDataStore();
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
            SnapshotObjectTO snapshot = (SnapshotObjectTO) srcData;
            DataTO destData = cmd.getDestTO();
            PrimaryDataStoreTO pool = (PrimaryDataStoreTO) destData.getDataStore();
            DataStoreTO imageStore = srcData.getDataStore();

            if (!(imageStore instanceof NfsTO)) {
                return new CopyCmdAnswer("unsupported protocol");
            }

            NfsTO nfsImageStore = (NfsTO) imageStore;

            String snapshotPath = snapshot.getPath();
            int index = snapshotPath.lastIndexOf("/");
            snapshotPath = snapshotPath.substring(0, index);
            String snapshotName = snapshotPath.substring(index + 1);
            KVMStoragePool secondaryPool = storagePoolMgr.getStoragePoolByURI(nfsImageStore.getUrl() + File.separator
                    + snapshotPath);
            KVMPhysicalDisk snapshotDisk = secondaryPool.getPhysicalDisk(snapshotName);

            String primaryUuid = pool.getUuid();
            KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getPoolType(), primaryUuid);
            String volUuid = UUID.randomUUID().toString();
            KVMPhysicalDisk disk = storagePoolMgr.copyPhysicalDisk(snapshotDisk, volUuid, primaryPool);
            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(disk.getName());
            newVol.setSize(disk.getVirtualSize());
            return new CopyCmdAnswer(newVol);
        } catch (CloudRuntimeException e) {
            return new CopyCmdAnswer(e.toString());
        }
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        return new Answer(cmd);
    }
}
