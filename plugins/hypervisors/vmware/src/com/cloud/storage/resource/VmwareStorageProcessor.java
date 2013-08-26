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
package com.cloud.storage.resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualSCSISharing;

import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.hypervisor.vmware.manager.VmwareHostService;
import com.cloud.hypervisor.vmware.manager.VmwareStorageMount;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.NetworkDetails;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.resource.VmwareResource;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Volume;
import com.cloud.storage.template.VmdkProcessor;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine.State;

public class VmwareStorageProcessor implements StorageProcessor {
    private static final Logger s_logger = Logger.getLogger(VmwareStorageProcessor.class);
    private VmwareHostService hostService;
    private boolean _fullCloneFlag;
    private VmwareStorageMount mountService;
    private VmwareResource resource;
    private Integer _timeout;
    protected Integer _shutdown_waitMs;
    private final Gson _gson;
    private final StorageLayer _storage = new JavaStorageLayer();
    private final PremiumSecondaryStorageResource storageResource;

    public VmwareStorageProcessor(VmwareHostService hostService, boolean fullCloneFlag, VmwareStorageMount mountService,
            Integer timeout,
            VmwareResource resource,
            Integer shutdownWaitMs,
            PremiumSecondaryStorageResource storageResource) {
        this.hostService = hostService;
        this._fullCloneFlag = fullCloneFlag;
        this.mountService = mountService;
        this._timeout = timeout;
        this.resource = resource;
        this._shutdown_waitMs = shutdownWaitMs;
        _gson = GsonHelper.getGsonLogger();
        this.storageResource = storageResource;
    }

    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert(_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if(files != null) {
            for(String fileName : files) {
                if(fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
    }
    private void copyTemplateFromSecondaryToPrimary(VmwareHypervisorHost hyperHost, DatastoreMO datastoreMo, String secondaryStorageUrl,
            String templatePathAtSecondaryStorage, String templateName, String templateUuid) throws Exception {

        s_logger.info("Executing copyTemplateFromSecondaryToPrimary. secondaryStorage: "
                + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage
                + ", templateName: " + templateName);

        String secondaryMountPoint = mountService.getMountPoint(secondaryStorageUrl);
        s_logger.info("Secondary storage mount point: " + secondaryMountPoint);

        String srcOVAFileName = secondaryMountPoint + "/" +  templatePathAtSecondaryStorage +
                templateName + "." + ImageFormat.OVA.getFileExtension();

        String srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {
            Script command = new Script("tar", 0, s_logger);
            command.add("--no-same-owner");
            command.add("-xf", srcOVAFileName);
            command.setWorkDir(secondaryMountPoint + "/" +  templatePathAtSecondaryStorage);
            s_logger.info("Executing command: " + command.toString());
            String result = command.execute();
            if(result != null) {
                String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        String vmName = templateUuid;
        hyperHost.importVmFromOVF(srcFileName, vmName, datastoreMo, "thin");

        VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
        if(vmMo == null) {
            String msg = "Failed to import OVA template. secondaryStorage: "
                    + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage
                    + ", templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if(vmMo.createSnapshot("cloud.template.base", "Base snapshot", false, false)) {
            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, templateUuid);
            vmMo.markAsTemplate();
        } else {
            vmMo.destroy();
            String msg = "Unable to create base snapshot for template, templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataStoreTO srcStore = srcData.getDataStore();
        if (!(srcStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO)srcStore;
        DataTO destData = cmd.getDestTO();
        DataStoreTO destStore = destData.getDataStore();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destStore;
        String secondaryStorageUrl = nfsImageStore.getUrl();
        assert (secondaryStorageUrl != null);

        String templateUrl = secondaryStorageUrl + File.separator + srcData.getPath();

        String templateName = null;
        String mountPoint = null;
        if (templateUrl.endsWith(".ova")) {
            int index = templateUrl.lastIndexOf("/");
            mountPoint = templateUrl.substring(0, index);
            mountPoint = mountPoint.substring(secondaryStorageUrl.length() + 1);
            if (!mountPoint.endsWith("/")) {
                mountPoint = mountPoint + "/";
            }

            templateName = templateUrl.substring(index + 1).replace("." + ImageFormat.OVA.getFileExtension(), "");

            if (templateName == null || templateName.isEmpty()) {
                templateName = template.getName();
            }
        } else {
            mountPoint = templateUrl.substring(secondaryStorageUrl.length() + 1);
            if (!mountPoint.endsWith("/")) {
                mountPoint = mountPoint + "/";
            }
            templateName = template.getName();
        }

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            String templateUuidName = UUID.nameUUIDFromBytes((templateName + "@" + primaryStore.getUuid() + "-" + hyperHost.getMor().getValue()).getBytes()).toString();
            // truncate template name to 32 chars to ensure they work well with vSphere API's.
            templateUuidName = templateUuidName.replace("-", "");

            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
            VirtualMachineMO templateMo = VmwareHelper.pickOneVmOnRunningHost(dcMo.findVmByNameAndLabel(templateUuidName), true);

            if (templateMo == null) {
                if(s_logger.isInfoEnabled()) {
                    s_logger.info("Template " + templateName + " is not setup yet, setup template from secondary storage with uuid name: " + templateUuidName);
                }
                ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());
                assert (morDs != null);
                DatastoreMO primaryStorageDatastoreMo = new DatastoreMO(context, morDs);

                copyTemplateFromSecondaryToPrimary(hyperHost,
                        primaryStorageDatastoreMo, secondaryStorageUrl,
                        mountPoint, templateName, templateUuidName);
            } else {
                s_logger.info("Template " + templateName + " has already been setup, skip the template setup process in primary storage");
            }

            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(templateUuidName);
            return new CopyCmdAnswer(newTemplate);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            String msg = "Unable to copy template to primary storage due to exception:" + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new CopyCmdAnswer(msg);
        }
    }
    private boolean createVMLinkedClone(VirtualMachineMO vmTemplate, DatacenterMO dcMo, DatastoreMO dsMo,
            String vmdkName, ManagedObjectReference morDatastore, ManagedObjectReference morPool) throws Exception {

        ManagedObjectReference morBaseSnapshot = vmTemplate.getSnapshotMor("cloud.template.base");
        if (morBaseSnapshot == null) {
            String msg = "Unable to find template base snapshot, invalid template";
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if(dsMo.folderExists(String.format("[%s]", dsMo.getName()), vmdkName)) {
            dsMo.deleteFile(String.format("[%s] %s/", dsMo.getName(), vmdkName), dcMo.getMor(), false);
        }

        s_logger.info("creating linked clone from template");
        if (!vmTemplate.createLinkedClone(vmdkName, morBaseSnapshot, dcMo.getVmFolder(), morPool, morDatastore)) {
            String msg = "Unable to clone from the template";
            s_logger.error(msg);
            throw new Exception(msg);
        }

        // we can't rely on un-offical API (VirtualMachineMO.moveAllVmDiskFiles() any more, use hard-coded disk names that we know
        // to move files
        s_logger.info("Move volume out of volume-wrapper VM ");
        dsMo.moveDatastoreFile(String.format("[%s] %s/%s.vmdk", dsMo.getName(), vmdkName, vmdkName),
                dcMo.getMor(), dsMo.getMor(),
                String.format("[%s] %s.vmdk", dsMo.getName(), vmdkName), dcMo.getMor(), true);

        dsMo.moveDatastoreFile(String.format("[%s] %s/%s-delta.vmdk", dsMo.getName(), vmdkName, vmdkName),
                dcMo.getMor(), dsMo.getMor(),
                String.format("[%s] %s-delta.vmdk", dsMo.getName(), vmdkName), dcMo.getMor(), true);

        return true;
    }

    private boolean createVMFullClone(VirtualMachineMO vmTemplate, DatacenterMO dcMo, DatastoreMO dsMo,
            String vmdkName, ManagedObjectReference morDatastore, ManagedObjectReference morPool) throws Exception {

        if(dsMo.folderExists(String.format("[%s]", dsMo.getName()), vmdkName)) {
            dsMo.deleteFile(String.format("[%s] %s/", dsMo.getName(), vmdkName), dcMo.getMor(), false);
        }

        s_logger.info("creating full clone from template");
        if (!vmTemplate.createFullClone(vmdkName, dcMo.getVmFolder(), morPool, morDatastore)) {
            String msg = "Unable to create full clone from the template";
            s_logger.error(msg);
            throw new Exception(msg);
        }

        // we can't rely on un-offical API (VirtualMachineMO.moveAllVmDiskFiles() any more, use hard-coded disk names that we know
        // to move files
        s_logger.info("Move volume out of volume-wrapper VM ");
        dsMo.moveDatastoreFile(String.format("[%s] %s/%s.vmdk", dsMo.getName(), vmdkName, vmdkName),
                dcMo.getMor(), dsMo.getMor(),
                String.format("[%s] %s.vmdk", dsMo.getName(), vmdkName), dcMo.getMor(), true);

        dsMo.moveDatastoreFile(String.format("[%s] %s/%s-flat.vmdk", dsMo.getName(), vmdkName, vmdkName),
                dcMo.getMor(), dsMo.getMor(),
                String.format("[%s] %s-flat.vmdk", dsMo.getName(), vmdkName), dcMo.getMor(), true);

        return true;
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataTO destData = cmd.getDestTO();
        VolumeObjectTO volume = (VolumeObjectTO)destData;
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();
        PrimaryDataStoreTO srcStore = (PrimaryDataStoreTO)template.getDataStore();


        try {
            VmwareContext context = this.hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(context, null);
            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
            VirtualMachineMO vmMo = null;
            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());
            if (morDatastore == null) {
                throw new Exception("Unable to find datastore in vSphere");
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDatastore);


            // attach volume id to make the name unique
            String vmdkName = volume.getName() + "-" + volume.getId();
            if (srcStore == null) {
                // create a root volume for blank VM
                String dummyVmName = this.hostService.getWorkerName(context, cmd, 0);

                try {
                    vmMo = prepareVolumeHostDummyVm(hyperHost, dsMo, dummyVmName);
                    if (vmMo == null) {
                        throw new Exception("Unable to create a dummy VM for volume creation");
                    }

                    String volumeDatastorePath = String.format("[%s] %s.vmdk", dsMo.getName(), vmdkName);
                    synchronized (this) {
                        s_logger.info("Delete file if exists in datastore to clear the way for creating the volume. file: " + volumeDatastorePath);
                        VmwareHelper.deleteVolumeVmdkFiles(dsMo, vmdkName, dcMo);
                        vmMo.createDisk(volumeDatastorePath, (int) (volume.getSize() / (1024L * 1024L)), morDatastore, -1);
                        vmMo.detachDisk(volumeDatastorePath, false);
                    }

                    VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setPath(vmdkName);
                    newVol.setSize(volume.getSize());
                    return new CopyCmdAnswer(newVol);
                } finally {
                    vmMo.detachAllDisks();

                    s_logger.info("Destroy dummy VM after volume creation");
                    vmMo.destroy();
                }
            } else {
                String templatePath = template.getPath();
                VirtualMachineMO vmTemplate = VmwareHelper.pickOneVmOnRunningHost(dcMo.findVmByNameAndLabel(templatePath), true);
                if (vmTemplate == null) {
                    s_logger.warn("Template host in vSphere is not in connected state, request template reload");
                    return new CopyCmdAnswer("Template host in vSphere is not in connected state, request template reload");
                }

                ManagedObjectReference morPool = hyperHost.getHyperHostOwnerResourcePool();
                ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
                //createVMLinkedClone(vmTemplate, dcMo, dsMo, vmdkName, morDatastore, morPool);
                if (!_fullCloneFlag) {
                    createVMLinkedClone(vmTemplate, dcMo, dsMo, vmdkName, morDatastore, morPool);
                } else {
                    createVMFullClone(vmTemplate, dcMo, dsMo, vmdkName, morDatastore, morPool);
                }

                vmMo = new ClusterMO(context, morCluster).findVmOnHyperHost(vmdkName);
                assert (vmMo != null);

                s_logger.info("detach disks from volume-wrapper VM " + vmdkName);
                vmMo.detachAllDisks();

                s_logger.info("destroy volume-wrapper VM " + vmdkName);
                vmMo.destroy();

                String srcFile = String.format("[%s] %s/", dsMo.getName(), vmdkName);
                dsMo.deleteFile(srcFile, dcMo.getMor(), true);
                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(vmdkName);
                newVol.setSize(volume.getSize());
                return new CopyCmdAnswer(newVol);
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                this.hostService.invalidateServiceContext(null);
            }

            String msg = "clone volume from base image failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    private Pair<String, String> copyVolumeFromSecStorage(VmwareHypervisorHost hyperHost, String srcVolumePath,
            DatastoreMO dsMo, String secStorageUrl) throws Exception {

        String volumeFolder = null;
        String volumeName = null;
        String sufix = ".ova";
        int index = srcVolumePath.lastIndexOf(File.separator);
        if (srcVolumePath.endsWith(sufix)) {
            volumeFolder = srcVolumePath.substring(0, index);
            volumeName = srcVolumePath.substring(index + 1).replace(sufix, "");
        } else {
            volumeFolder = srcVolumePath;
            volumeName = srcVolumePath.substring(index + 1);
        }

        String newVolume    = UUID.randomUUID().toString().replaceAll("-", "");
        restoreVolumeFromSecStorage(hyperHost, dsMo, newVolume, secStorageUrl, volumeFolder, volumeName);

        return new Pair<String, String>(volumeFolder, newVolume);
    }

    private String deleteVolumeDirOnSecondaryStorage(String volumeDir, String secStorageUrl) throws Exception {
        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String volumeMountRoot = secondaryMountPoint + File.separator + volumeDir;

        return deleteDir(volumeMountRoot);
    }

    private String deleteDir(String dir) {
        synchronized(dir.intern()) {
            Script command = new Script(false, "rm", _timeout, s_logger);
            command.add("-rf");
            command.add(dir);
            return command.execute();
        }
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(CopyCommand cmd) {
        VolumeObjectTO srcVolume = (VolumeObjectTO)cmd.getSrcTO();
        VolumeObjectTO destVolume = (VolumeObjectTO)cmd.getDestTO();
        VmwareContext context = hostService.getServiceContext(cmd);
        try {

            NfsTO srcStore = (NfsTO)srcVolume.getDataStore();
            PrimaryDataStoreTO destStore = (PrimaryDataStoreTO)destVolume.getDataStore();

            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            String uuid = destStore.getUuid();

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, uuid);
            if (morDatastore == null) {
                morDatastore = hyperHost.mountDatastore(
                        false,
                        destStore.getHost(), 0, destStore.getPath(),
                        destStore.getUuid().replace("-", ""));

                if (morDatastore == null) {
                    throw new Exception("Unable to mount storage pool on host. storeUrl: " + destStore.getHost() + ":/" + destStore.getPath());
                }
            }

            Pair<String, String>  result = copyVolumeFromSecStorage(
                    hyperHost, srcVolume.getPath(),
                    new DatastoreMO(context, morDatastore),
                    srcStore.getUrl());
            deleteVolumeDirOnSecondaryStorage(result.first(), srcStore.getUrl());
            VolumeObjectTO newVolume = new VolumeObjectTO();
            newVolume.setPath(result.second());
            return new CopyCmdAnswer(newVolume);
        } catch (Throwable t) {
            if (t instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            String msg = "Unable to execute CopyVolumeCommand due to exception";
            s_logger.error(msg, t);
            return new CopyCmdAnswer("copy volume secondary to primary failed due to exception: " + VmwareHelper.getExceptionMessage(t));
        }

    }

    private String getVolumePathInDatastore(DatastoreMO dsMo, String volumeFileName) throws Exception {
        String datastoreVolumePath = dsMo.searchFileInSubFolders(volumeFileName, true);
        assert (datastoreVolumePath != null) : "Virtual disk file missing from datastore.";
        return datastoreVolumePath;
    }

    private Pair<String, String> copyVolumeToSecStorage(VmwareHostService hostService, VmwareHypervisorHost hyperHost, CopyCommand cmd,
            String vmName, String poolId, String volumePath, String destVolumePath,
            String secStorageUrl, String workerVmName) throws Exception {
        VirtualMachineMO workerVm=null;
        VirtualMachineMO vmMo=null;
        String exportName = UUID.randomUUID().toString();

        try {
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolId);

            if (morDs == null) {
                String msg = "Unable to find volumes's storage pool for copy volume operation";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                // create a dummy worker vm for attaching the volume
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
                //restrict VM name to 32 chars, (else snapshot descriptor file name will be truncated to 32 chars of vm name)
                VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();
                vmConfig.setName(workerVmName);
                vmConfig.setMemoryMB((long) 4);
                vmConfig.setNumCPUs(1);
                vmConfig.setGuestId(VirtualMachineGuestOsIdentifier.OTHER_GUEST.value());
                VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
                fileInfo.setVmPathName(String.format("[%s]", dsMo.getName()));
                vmConfig.setFiles(fileInfo);

                // Scsi controller
                VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
                scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
                scsiController.setBusNumber(0);
                scsiController.setKey(1);
                VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
                scsiControllerSpec.setDevice(scsiController);
                scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
                vmConfig.getDeviceChange().add(scsiControllerSpec);

                hyperHost.createVm(vmConfig);
                workerVm = hyperHost.findVmOnHyperHost(workerVmName);
                if (workerVm == null) {
                    String msg = "Unable to create worker VM to execute CopyVolumeCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                //attach volume to worker VM
                String datastoreVolumePath = getVolumePathInDatastore(dsMo, volumePath + ".vmdk");
                workerVm.attachDisk(new String[] { datastoreVolumePath }, morDs);
                vmMo = workerVm;
            }

            vmMo.createSnapshot(exportName, "Temporary snapshot for copy-volume command", false, false);

            exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl, destVolumePath, exportName,
                    hostService.getWorkerName(hyperHost.getContext(), cmd, 1));
            return new Pair<String, String>(destVolumePath, exportName);

        } finally {
            vmMo.removeSnapshot(exportName, false);
            if (workerVm != null) {
                //detach volume and destroy worker vm
                workerVm.detachAllDisks();
                workerVm.destroy();
            }
        }
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(CopyCommand cmd) {
        VolumeObjectTO srcVolume = (VolumeObjectTO)cmd.getSrcTO();
        VolumeObjectTO destVolume = (VolumeObjectTO)cmd.getDestTO();
        String vmName = srcVolume.getVmName();

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            PrimaryDataStoreTO primaryStorage = (PrimaryDataStoreTO)srcVolume.getDataStore();
            NfsTO destStore = (NfsTO)destVolume.getDataStore();
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            Pair<String, String> result;

            result = copyVolumeToSecStorage(hostService,
                    hyperHost, cmd, vmName, primaryStorage.getUuid(), srcVolume.getPath(),destVolume.getPath(),
                    destStore.getUrl(),
                    hostService.getWorkerName(context, cmd, 0));
            VolumeObjectTO newVolume = new VolumeObjectTO();
            newVolume.setPath(result.first() + File.separator + result.second());
            return new CopyCmdAnswer(newVolume);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            String msg = "Unable to execute CopyVolumeCommand due to exception";
            s_logger.error(msg, e);
            return new CopyCmdAnswer("copy volume from primary to secondary failed due to exception: " + VmwareHelper.getExceptionMessage(e));
        }
    }

    private void postCreatePrivateTemplate(String installFullPath, long templateId,
            String templateName, long size, long virtualSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/template.properties")));
            out.write("filename=" + templateName + ".ova");
            out.newLine();
            out.write("description=");
            out.newLine();
            out.write("checksum=");
            out.newLine();
            out.write("hvm=false");
            out.newLine();
            out.write("size=" + size);
            out.newLine();
            out.write("ova=true");
            out.newLine();
            out.write("id=" + templateId);
            out.newLine();
            out.write("public=false");
            out.newLine();
            out.write("ova.filename=" + templateName + ".ova");
            out.newLine();
            out.write("uniquename=" + templateName);
            out.newLine();
            out.write("ova.virtualsize=" + virtualSize);
            out.newLine();
            out.write("virtualsize=" + virtualSize);
            out.newLine();
            out.write("ova.size=" + size);
            out.newLine();
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }

    private Ternary<String, Long, Long> createTemplateFromVolume(VirtualMachineMO vmMo, String installPath, long templateId, String templateUniqueName,
            String secStorageUrl, String volumePath, String workerVmName) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        synchronized(installPath.intern()) {
            Script command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);

            String result = command.execute();
            if(result != null) {
                String msg = "unable to prepare template directory: "
                        + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        VirtualMachineMO clonedVm = null;
        try {
            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath, false);
            if(volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            if(!vmMo.createSnapshot(templateUniqueName, "Temporary snapshot for template creation", false, false)) {
                String msg = "Unable to take snapshot for creating template from volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            // 4 MB is the minimum requirement for VM memory in VMware
            vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(),
                    VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
            if(clonedVm == null) {
                String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            clonedVm.exportVm(secondaryMountPoint + "/" + installPath, templateUniqueName, true, false);

            long physicalSize = new File(installFullPath + "/" + templateUniqueName + ".ova").length();
            VmdkProcessor processor = new VmdkProcessor();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("VMDK Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, templateUniqueName);

            postCreatePrivateTemplate(installFullPath, templateId, templateUniqueName, physicalSize, virtualSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);

        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }

            vmMo.removeSnapshot(templateUniqueName, false);
        }
    }

    @Override
    public Answer createTemplateFromVolume(CopyCommand cmd) {
        VolumeObjectTO volume = (VolumeObjectTO)cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)cmd.getDestTO();
        DataStoreTO imageStore = template.getDataStore();

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }
        NfsTO nfsImageStore = (NfsTO)imageStore;
        String secondaryStoragePoolURL = nfsImageStore.getUrl();
        String volumePath = volume.getPath();

        String details = null;

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(volume.getVmName());
            if (vmMo == null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find the owner VM for CreatePrivateTemplateFromVolumeCommand on host " + hyperHost.getHyperHostName() + ", try within datacenter");
                }
                vmMo = hyperHost.findVmOnPeerHyperHost(volume.getVmName());

                if (vmMo == null) {
                    // This means either the volume is on a zone wide storage pool or VM is deleted by external entity.
                    // Look for the VM in the datacenter.
                    ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                    DatacenterMO dcMo = new DatacenterMO(context, dcMor);
                    vmMo = dcMo.findVm(volume.getVmName());
                }

                if(vmMo == null) {
                    String msg = "Unable to find the owner VM for volume operation. vm: " + volume.getVmName();
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            Ternary<String, Long, Long> result = createTemplateFromVolume(vmMo,
                    template.getPath(), template.getId(), template.getName(),
                    secondaryStoragePoolURL, volumePath,
                    hostService.getWorkerName(context, cmd, 0));

            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(result.first());
            newTemplate.setFormat(ImageFormat.OVA);
            newTemplate.setSize(result.third());
            newTemplate.setPhysicalSize(result.second());
            return new CopyCmdAnswer(newTemplate);

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);

            details = "create template from volume exception: " + VmwareHelper.getExceptionMessage(e);
            return new CopyCmdAnswer(details);
        }
    }

    private void writeMetaOvaForTemplate(String installFullPath, String ovfFilename, String vmdkFilename,
            String templateName, long diskSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/" + templateName +".ova.meta")));
            out.write("ova.filename=" + templateName + ".ova");
            out.newLine();
            out.write("version=1.0");
            out.newLine();
            out.write("ovf=" + ovfFilename);
            out.newLine();
            out.write("numDisks=1");
            out.newLine();
            out.write("disk1.name=" + vmdkFilename);
            out.newLine();
            out.write("disk1.size=" + diskSize);
            out.newLine();
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }

    private Ternary<String, Long, Long> createTemplateFromSnapshot(String installPath, String templateUniqueName,
            String secStorageUrl, String snapshotPath, Long templateId) throws Exception {
        //Snapshot path is decoded in this form: /snapshots/account/volumeId/uuid/uuid
        String backupSSUuid;
        String snapshotFolder;
        if (snapshotPath.endsWith(".ova")) {
            int index = snapshotPath.lastIndexOf(File.separator);
            backupSSUuid = snapshotPath.substring(index + 1).replace(".ova", "");
            snapshotFolder = snapshotPath.substring(0, index);
        } else {
            String[] tokens = snapshotPath.split(File.separator);
            backupSSUuid = tokens[tokens.length - 1];
            snapshotFolder = StringUtils.join(tokens, File.separator, 0, tokens.length -1);
        }

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        String installFullOVAName = installFullPath + "/" + templateUniqueName + ".ova";  //Note: volss for tmpl
        String snapshotRoot = secondaryMountPoint + "/" + snapshotFolder;
        String snapshotFullOVAName = snapshotRoot + "/" + backupSSUuid + ".ova";
        String snapshotFullOvfName = snapshotRoot + "/" + backupSSUuid + ".ovf";
        String result;
        Script command;
        String templateVMDKName = "";
        String snapshotFullVMDKName = snapshotRoot + "/" + backupSSUuid + "/";

        synchronized(installPath.intern()) {
            command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);

            result = command.execute();
            if(result != null) {
                String msg = "unable to prepare template directory: "
                        + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        try {
            if(new File(snapshotFullOVAName).exists()) {
                command = new Script(false, "cp", _timeout, s_logger);
                command.add(snapshotFullOVAName);
                command.add(installFullOVAName);
                result = command.execute();
                if(result != null) {
                    String msg = "unable to copy snapshot " + snapshotFullOVAName + " to " + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                // untar OVA file at template directory
                command = new Script("tar", 0, s_logger);
                command.add("--no-same-owner");
                command.add("-xf", installFullOVAName);
                command.setWorkDir(installFullPath);
                s_logger.info("Executing command: " + command.toString());
                result = command.execute();
                if(result != null) {
                    String msg = "unable to untar snapshot " + snapshotFullOVAName + " to "
                            + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

            } else {  // there is no ova file, only ovf originally;
                if(new File(snapshotFullOvfName).exists()) {
                    command = new Script(false, "cp", _timeout, s_logger);
                    command.add(snapshotFullOvfName);
                    //command.add(installFullOvfName);
                    command.add(installFullPath);
                    result = command.execute();
                    if(result != null) {
                        String msg = "unable to copy snapshot " + snapshotFullOvfName + " to " + installFullPath;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }

                    s_logger.info("vmdkfile parent dir: " + snapshotRoot);
                    File snapshotdir = new File(snapshotRoot);
                    File[] ssfiles = snapshotdir.listFiles();
                    if (ssfiles == null) {
                        String msg = "unable to find snapshot vmdk files in " + snapshotRoot;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }
                    // List<String> filenames = new ArrayList<String>();
                    for (int i = 0; i < ssfiles.length; i++) {
                        String vmdkfile = ssfiles[i].getName();
                        s_logger.info("vmdk file name: " + vmdkfile);
                        if(vmdkfile.toLowerCase().startsWith(backupSSUuid) && vmdkfile.toLowerCase().endsWith(".vmdk")) {
                            snapshotFullVMDKName = snapshotRoot + File.separator + vmdkfile;
                            templateVMDKName += vmdkfile;
                            break;
                        }
                    }
                    if (snapshotFullVMDKName != null) {
                        command = new Script(false, "cp", _timeout, s_logger);
                        command.add(snapshotFullVMDKName);
                        command.add(installFullPath);
                        result = command.execute();
                        s_logger.info("Copy VMDK file: " + snapshotFullVMDKName);
                        if(result != null) {
                            String msg = "unable to copy snapshot vmdk file " + snapshotFullVMDKName + " to " + installFullPath;
                            s_logger.error(msg);
                            throw new Exception(msg);
                        }
                    }
                } else {
                    String msg = "unable to find any snapshot ova/ovf files" + snapshotFullOVAName + " to " + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            long physicalSize = new File(installFullPath + "/" + templateVMDKName).length();
            VmdkProcessor processor = new VmdkProcessor();
            // long physicalSize = new File(installFullPath + "/" + templateUniqueName + ".ova").length();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("VMDK Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, templateUniqueName);

            postCreatePrivateTemplate(installFullPath, templateId, templateUniqueName, physicalSize, virtualSize);
            writeMetaOvaForTemplate(installFullPath, backupSSUuid + ".ovf", templateVMDKName, templateUniqueName, physicalSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);
        } catch(Exception e) {
            // TODO, clean up left over files
            throw e;
        }
    }

    @Override
    public Answer createTemplateFromSnapshot(CopyCommand cmd) {
        SnapshotObjectTO snapshot = (SnapshotObjectTO)cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)cmd.getDestTO();
        DataStoreTO imageStore = template.getDataStore();
        String details;
        String uniqeName = UUID.randomUUID().toString();

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            if (!(imageStore instanceof  NfsTO)) {
                return new CopyCmdAnswer("Only support create template from snapshot, when the dest store is nfs");
            }

            NfsTO nfsSvr = (NfsTO)imageStore;
            Ternary<String, Long, Long> result = createTemplateFromSnapshot(template.getPath(),
                    uniqeName,
                    nfsSvr.getUrl(), snapshot.getPath(),
                    template.getId()
                    );

            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(result.first());
            newTemplate.setPhysicalSize(result.second());
            newTemplate.setSize(result.third());
            newTemplate.setFormat(ImageFormat.OVA);
            newTemplate.setName(uniqeName);
            return new CopyCmdAnswer(newTemplate);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);

            details = "create template from snapshot exception: " + VmwareHelper.getExceptionMessage(e);
            return new CopyCmdAnswer(details);
        }
    }

    private void exportVolumeToSecondaryStroage(VirtualMachineMO vmMo, String volumePath,
            String secStorageUrl, String secStorageDir, String exportName,
            String workerVmName) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String exportPath =  secondaryMountPoint + "/" + secStorageDir + "/" + exportName;

        synchronized(exportPath.intern()) {
            if(!new File(exportPath).exists()) {
                Script command = new Script(false, "mkdir", _timeout, s_logger);
                command.add("-p");
                command.add(exportPath);
                if(command.execute() != null) {
                    throw new Exception("unable to prepare snapshot backup directory");
                }
            }
        }

        VirtualMachineMO clonedVm = null;
        try {

            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath, false);
            if(volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            // 4 MB is the minimum requirement for VM memory in VMware
            vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(),
                    VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
            if(clonedVm == null) {
                String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            clonedVm.exportVm(exportPath, exportName, false, false);
        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }


    private String backupSnapshotToSecondaryStorage(VirtualMachineMO vmMo, String installPath,
            String volumePath, String snapshotUuid, String secStorageUrl,
            String prevSnapshotUuid, String prevBackupUuid, String workerVmName) throws Exception {

        String backupUuid = UUID.randomUUID().toString();
        exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl,
                installPath, backupUuid, workerVmName);
        return backupUuid + "/" + backupUuid;
    }
    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        SnapshotObjectTO srcSnapshot = (SnapshotObjectTO)cmd.getSrcTO();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcSnapshot.getDataStore();
        SnapshotObjectTO destSnapshot = (SnapshotObjectTO)cmd.getDestTO();
        DataStoreTO destStore = destSnapshot.getDataStore();
        if (!(destStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO destNfsStore = (NfsTO)destStore;


        String secondaryStorageUrl = destNfsStore.getUrl();
        String snapshotUuid = srcSnapshot.getPath();
        String prevSnapshotUuid = srcSnapshot.getParentSnapshotPath();
        String prevBackupUuid = destSnapshot.getParentSnapshotPath();
        VirtualMachineMO workerVm=null;
        String workerVMName = null;
        String volumePath = srcSnapshot.getVolume().getPath();
        ManagedObjectReference morDs = null;
        DatastoreMO dsMo=null;

        // By default assume failure
        String details = null;
        boolean success = false;
        String snapshotBackupUuid = null;

        VmwareContext context = hostService.getServiceContext(cmd);
        VirtualMachineMO vmMo = null;
        String vmName = srcSnapshot.getVmName();
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());

            try {
                vmMo = hyperHost.findVmOnHyperHost(vmName);
                if (vmMo == null) {
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find owner VM for BackupSnapshotCommand on host " + hyperHost.getHyperHostName() + ", will try within datacenter");
                    }

                    vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
                    if(vmMo == null) {
                        dsMo = new DatastoreMO(hyperHost.getContext(), morDs);

                        workerVMName = hostService.getWorkerName(context, cmd, 0);

                        // attach a volume to dummay wrapper VM for taking snapshot and exporting the VM for backup
                        if (!hyperHost.createBlankVm(workerVMName, 1, 512, 0, false, 4, 0, VirtualMachineGuestOsIdentifier.OTHER_GUEST.value(), morDs, false)) {
                            String msg = "Unable to create worker VM to execute BackupSnapshotCommand";
                            s_logger.error(msg);
                            throw new Exception(msg);
                        }
                        vmMo = hyperHost.findVmOnHyperHost(workerVMName);
                        if (vmMo == null) {
                            throw new Exception("Failed to find the newly create or relocated VM. vmName: " + workerVMName);
                        }
                        workerVm = vmMo;

                        // attach volume to worker VM
                        String datastoreVolumePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumePath);
                        vmMo.attachDisk(new String[] { datastoreVolumePath }, morDs);
                    }
                }

                if (!vmMo.createSnapshot(snapshotUuid, "Snapshot taken for " + srcSnapshot.getName(), false, false)) {
                    throw new Exception("Failed to take snapshot " + srcSnapshot.getName() + " on vm: " + vmName);
                }

                snapshotBackupUuid = backupSnapshotToSecondaryStorage(vmMo, destSnapshot.getPath(), srcSnapshot.getVolume().getPath(), snapshotUuid, secondaryStorageUrl, prevSnapshotUuid, prevBackupUuid,
                        hostService.getWorkerName(context, cmd, 1));

                success = (snapshotBackupUuid != null);

                if (!success) {
                    details = "Failed to backUp the snapshot with uuid: " + snapshotUuid + " to secondary storage.";
                    return new CopyCmdAnswer(details);
                } else {
                    details = "Successfully backedUp the snapshot with Uuid: " + snapshotUuid + " to secondary storage.";
                    SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
                    newSnapshot.setPath(destSnapshot.getPath() + "/" + snapshotBackupUuid);
                    return new CopyCmdAnswer(newSnapshot);
                }
            } finally {
                if(vmMo != null){
                    ManagedObjectReference snapshotMor = vmMo.getSnapshotMor(snapshotUuid);
                    if (snapshotMor != null){
                        vmMo.removeSnapshot(snapshotUuid, false);
                    }
                }

                try {
                    if (workerVm != null) {
                        // detach volume and destroy worker vm
                        workerVm.detachAllDisks();
                        workerVm.destroy();
                    }
                } catch (Throwable e) {
                    s_logger.warn("Failed to destroy worker VM: " + workerVMName);
                }
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);

            details = "backup snapshot exception: " + VmwareHelper.getExceptionMessage(e);
            return new CopyCmdAnswer(details);
        }
    }

    @Override
    public Answer attachIso(AttachCommand cmd) {
        return this.attachIso(cmd.getDisk(), true, cmd.getVmName());
    }

    @Override
    public Answer attachVolume(AttachCommand cmd) {
        return this.attachVolume(cmd, cmd.getDisk(), true, cmd.isManaged(), cmd.getVmName(), cmd.get_iScsiName(),
                cmd.getStorageHost(), cmd.getStoragePort(), cmd.getChapInitiatorUsername(), cmd.getChapInitiatorPassword(),
                cmd.getChapTargetUsername(), cmd.getChapTargetPassword());
    }

    private Answer attachVolume(Command cmd, DiskTO disk, boolean isAttach, boolean isManaged, String vmName, String iScsiName, String storageHost, int storagePort) {
        return attachVolume(cmd, disk, isAttach, isManaged, vmName, iScsiName, storageHost, storagePort, null, null, null, null);
    }

    private Answer attachVolume(Command cmd, DiskTO disk, boolean isAttach, boolean isManaged, String vmName,
            String iScsiName, String storageHost, int storagePort, String initiatorUsername, String initiatorPassword,
            String targetUsername, String targetPassword) {

        VolumeObjectTO volumeTO = (VolumeObjectTO)disk.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volumeTO.getDataStore();
        try {
            VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(this.hostService.getServiceContext(null), null);
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                String msg = "Unable to find the VM to execute AttachVolumeCommand, vmName: " + vmName;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            ManagedObjectReference morDs = null;

            if (isAttach && isManaged) {
                morDs = this.hostService.handleDatastoreAndVmdkAttach(cmd, iScsiName, storageHost, storagePort,
                        initiatorUsername, initiatorPassword, targetUsername, targetPassword);
            }
            else {
                morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, isManaged ? VmwareResource.getDatastoreName(iScsiName) : primaryStore.getUuid());
            }

            if (morDs == null) {
                String msg = "Unable to find the mounted datastore to execute AttachVolumeCommand, vmName: " + vmName;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO dsMo = new DatastoreMO(this.hostService.getServiceContext(null), morDs);
            String datastoreVolumePath = String.format("[%s] %s.vmdk", dsMo.getName(), isManaged ? dsMo.getName() : volumeTO.getPath());

            disk.setVdiUuid(datastoreVolumePath);

            AttachAnswer answer = new AttachAnswer(disk);
            if (isAttach) {
                vmMo.attachDisk(new String[] { datastoreVolumePath }, morDs);
            } else {
                vmMo.removeAllSnapshots();
                vmMo.detachDisk(datastoreVolumePath, false);

                if (isManaged) {
                    this.hostService.handleDatastoreAndVmdkDetach(iScsiName, storageHost, storagePort);
                }
            }

            return answer;
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                this.hostService.invalidateServiceContext(null);
            }

            String msg = "AttachVolumeCommand failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new AttachAnswer(msg);
        }
    }

    private static String getSecondaryDatastoreUUID(String storeUrl) {
        return UUID.nameUUIDFromBytes(storeUrl.getBytes()).toString();
    }

    public synchronized ManagedObjectReference prepareSecondaryDatastoreOnHost(String storeUrl) throws Exception {
        String storeName = getSecondaryDatastoreUUID(storeUrl);
        URI uri = new URI(storeUrl);

        VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(this.hostService.getServiceContext(null), null);
        ManagedObjectReference morDatastore = hyperHost.mountDatastore(false, uri.getHost(), 0, uri.getPath(), storeName.replace("-", ""));

        if (morDatastore == null) {
            throw new Exception("Unable to mount secondary storage on host. storeUrl: " + storeUrl);
        }

        return morDatastore;
    }
    private Answer attachIso(DiskTO disk, boolean isAttach, String vmName) {


        try {
            VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(this.hostService.getServiceContext(null), null);
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                String msg = "Unable to find VM in vSphere to execute AttachIsoCommand, vmName: " + vmName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
            TemplateObjectTO iso = (TemplateObjectTO)disk.getData();
            NfsTO nfsImageStore = (NfsTO)iso.getDataStore();
            String storeUrl = null;
            if (nfsImageStore != null) {
                storeUrl = nfsImageStore.getUrl();
            }
            if (storeUrl == null) {
                if (!iso.getName().equalsIgnoreCase("vmware-tools.iso")) {
                    String msg = "ISO store root url is not found in AttachIsoCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                } else {
                    if (isAttach) {
                        vmMo.mountToolsInstaller();
                    } else {
                        try{
                            vmMo.unmountToolsInstaller();
                        }catch(Throwable e){
                            vmMo.detachIso(null);
                        }
                    }

                    return new AttachAnswer(disk);
                }
            }

            ManagedObjectReference morSecondaryDs = prepareSecondaryDatastoreOnHost(storeUrl);
            String isoPath = nfsImageStore.getUrl() + File.separator + iso.getPath();
            if (!isoPath.startsWith(storeUrl)) {
                assert (false);
                String msg = "ISO path does not start with the secondary storage root";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            int isoNameStartPos = isoPath.lastIndexOf('/');
            String isoFileName = isoPath.substring(isoNameStartPos + 1);
            String isoStorePathFromRoot = isoPath.substring(storeUrl.length(), isoNameStartPos);

            // TODO, check if iso is already attached, or if there is a previous
            // attachment
            DatastoreMO secondaryDsMo = new DatastoreMO(this.hostService.getServiceContext(null), morSecondaryDs);
            String storeName = secondaryDsMo.getName();
            String isoDatastorePath = String.format("[%s] %s/%s", storeName, isoStorePathFromRoot, isoFileName);

            if (isAttach) {
                vmMo.attachIso(isoDatastorePath, morSecondaryDs, true, false);
            } else {
                vmMo.detachIso(isoDatastorePath);
            }

            return new AttachAnswer(disk);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                this.hostService.invalidateServiceContext(null);
            }

            if(isAttach) {
                String msg = "AttachIsoCommand(attach) failed due to " + VmwareHelper.getExceptionMessage(e);
                s_logger.error(msg, e);
                return new AttachAnswer(msg);
            } else {
                String msg = "AttachIsoCommand(detach) failed due to " + VmwareHelper.getExceptionMessage(e);
                s_logger.warn(msg, e);
                return new AttachAnswer(msg);
            }
        }
    }
    @Override
    public Answer dettachIso(DettachCommand cmd) {
        return this.attachIso(cmd.getDisk(), false, cmd.getVmName());
    }

    @Override
    public Answer dettachVolume(DettachCommand cmd) {
        return this.attachVolume(cmd, cmd.getDisk(), false, cmd.isManaged(), cmd.getVmName(), cmd.get_iScsiName(), cmd.getStorageHost(), cmd.getStoragePort());
    }

    protected VirtualMachineMO prepareVolumeHostDummyVm(VmwareHypervisorHost hyperHost, DatastoreMO dsMo, String vmName) throws Exception {
        assert (hyperHost != null);

        VirtualMachineMO vmMo = null;
        VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();
        vmConfig.setName(vmName);
        vmConfig.setMemoryMB((long) 4); // vmware request minimum of 4 MB
        vmConfig.setNumCPUs(1);
        vmConfig.setGuestId(VirtualMachineGuestOsIdentifier.OTHER_GUEST.value());
        VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
        fileInfo.setVmPathName(String.format("[%s]", dsMo.getName()));
        vmConfig.setFiles(fileInfo);

        // Scsi controller
        VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
        scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
        scsiController.setBusNumber(0);
        scsiController.setKey(1);
        VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
        scsiControllerSpec.setDevice(scsiController);
        scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        vmConfig.getDeviceChange().add(scsiControllerSpec );
        hyperHost.createVm(vmConfig);
        vmMo = hyperHost.findVmOnHyperHost(vmName);
        return vmMo;
    }
    @Override
    public Answer createVolume(CreateObjectCommand cmd) {

        VolumeObjectTO volume = (VolumeObjectTO)cmd.getData();
        PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)volume.getDataStore();

        try {
            VmwareContext context = this.hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(context, null);
            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());
            if (morDatastore == null) {
                throw new Exception("Unable to find datastore in vSphere");
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDatastore);
            // create data volume
            VirtualMachineMO vmMo = null;
            String volumeUuid = UUID.randomUUID().toString().replace("-", "");
            String volumeDatastorePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumeUuid);
            String dummyVmName = this.hostService.getWorkerName(context, cmd, 0);
            try {
                vmMo = prepareVolumeHostDummyVm(hyperHost, dsMo, dummyVmName);
                if (vmMo == null) {
                    throw new Exception("Unable to create a dummy VM for volume creation");
                }

                synchronized (this) {
                    // s_logger.info("Delete file if exists in datastore to clear the way for creating the volume. file: " + volumeDatastorePath);
                    VmwareHelper.deleteVolumeVmdkFiles(dsMo, volumeUuid.toString(), dcMo);

                    vmMo.createDisk(volumeDatastorePath, (int) (volume.getSize() / (1024L * 1024L)), morDatastore, vmMo.getScsiDeviceControllerKey());
                    vmMo.detachDisk(volumeDatastorePath, false);
                }

                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(volumeUuid);
                newVol.setSize(volume.getSize());
                return new CreateObjectAnswer(newVol);
            } finally {
                s_logger.info("Destroy dummy VM after volume creation");
                vmMo.detachAllDisks();
                vmMo.destroy();
            }

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                this.hostService.invalidateServiceContext(null);
            }

            String msg = "create volume failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new CreateObjectAnswer(e.toString());
        }
    }

    @Override
    public Answer createSnapshot(CreateObjectCommand cmd) {
        // snapshot operation (create or destroy) is handled inside BackupSnapshotCommand(), we just fake
        // a success return here
        String snapshotUUID = UUID.randomUUID().toString();
        SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
        newSnapshot.setPath(snapshotUUID);
        return new CreateObjectAnswer(newSnapshot);
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource DestroyCommand: " + _gson.toJson(cmd));
        }

        /*
         * DestroyCommand content example
         *
         * {"volume": {"id":5,"name":"Volume1", "mountPoint":"/export/home/kelven/vmware-test/primary",
         * "path":"6bb8762f-c34c-453c-8e03-26cc246ceec4", "size":0,"type":"DATADISK","resourceType":
         * "STORAGE_POOL","storagePoolType":"NetworkFilesystem", "poolId":0,"deviceId":0 } }
         *
         * {"volume": {"id":1, "name":"i-2-1-KY-ROOT", "mountPoint":"/export/home/kelven/vmware-test/primary",
         * "path":"i-2-1-KY-ROOT","size":0,"type":"ROOT", "resourceType":"STORAGE_POOL", "storagePoolType":"NetworkFilesystem",
         * "poolId":0,"deviceId":0 } }
         */

        try {
            VmwareContext context = this.hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = this.hostService.getHyperHost(context, null);
            VolumeObjectTO vol = (VolumeObjectTO)cmd.getData();
            PrimaryDataStoreTO store = (PrimaryDataStoreTO)vol.getDataStore();

            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, store.getUuid());
            if (morDs == null) {
                String msg = "Unable to find datastore based on volume mount point " + store.getPath();
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDs);

            ManagedObjectReference morDc = hyperHost.getHyperHostDatacenter();
            ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
            ClusterMO clusterMo = new ClusterMO(context, morCluster);

            if (vol.getVolumeType() == Volume.Type.ROOT) {
                String vmName = vol.getVmName();
                if (vmName != null) {
                    VirtualMachineMO vmMo = clusterMo.findVmOnHyperHost(vmName);
                    if (vmMo != null) {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Destroy root volume and VM itself. vmName " + vmName);
                        }

                        HostMO hostMo = vmMo.getRunningHost();
                        List<NetworkDetails> networks = vmMo.getNetworksWithDetails();

                        // tear down all devices first before we destroy the VM to avoid accidently delete disk backing files
                        if (this.resource.getVmState(vmMo) != State.Stopped) {
                            vmMo.safePowerOff(_shutdown_waitMs);
                        }
                        vmMo.tearDownDevices(new Class<?>[] { VirtualDisk.class, VirtualEthernetCard.class });
                        vmMo.destroy();

                        for (NetworkDetails netDetails : networks) {
                            if (netDetails.getGCTag() != null && netDetails.getGCTag().equalsIgnoreCase("true")) {
                                if (netDetails.getVMMorsOnNetwork() == null || netDetails.getVMMorsOnNetwork().length == 1) {
                                    this.resource.cleanupNetwork(hostMo, netDetails);
                                }
                            }
                        }
                    }

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Destroy volume by original name: " + vol.getPath() + ".vmdk");
                    }
                    dsMo.deleteFile(vol.getPath() + ".vmdk", morDc, true);

                    // root volume may be created via linked-clone, delete the delta disk as well
                    if (_fullCloneFlag) {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Destroy volume by derived name: " + vol.getPath() + "-flat.vmdk");
                        }
                        dsMo.deleteFile(vol.getPath() + "-flat.vmdk", morDc, true);
                    } else {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Destroy volume by derived name: " + vol.getPath() + "-delta.vmdk");
                        }
                        dsMo.deleteFile(vol.getPath() + "-delta.vmdk", morDc, true);
                    }
                    return new Answer(cmd, true, "Success");
                }

                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Destroy root volume directly from datastore");
                }
            } else {
                // evitTemplate will be converted into DestroyCommand, test if we are running in this case
                VirtualMachineMO vmMo = clusterMo.findVmOnHyperHost(vol.getPath());
                if (vmMo != null) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Destroy template volume " + vol.getPath());
                    }

                    vmMo.destroy();
                    return new Answer(cmd, true, "Success");
                }
            }

            String chainInfo = vol.getChainInfo();
            if (chainInfo != null && !chainInfo.isEmpty()) {
                s_logger.info("Destroy volume by chain info: " + chainInfo);
                String[] diskChain = _gson.fromJson(chainInfo, String[].class);

                if (diskChain != null && diskChain.length > 0) {
                    for (String backingName : diskChain) {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Delete volume backing file: " + backingName);
                        }
                        dsMo.deleteFile(backingName, morDc, true);
                    }
                } else {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Empty disk chain info, fall back to try to delete by original backing file name");
                    }
                    dsMo.deleteFile(vol.getPath() + ".vmdk", morDc, true);

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Destroy volume by derived name: " + vol.getPath() + "-flat.vmdk");
                    }
                    dsMo.deleteFile(vol.getPath() + "-flat.vmdk", morDc, true);
                }
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Destroy volume by original name: " + vol.getPath() + ".vmdk");
                }
                dsMo.deleteFile(vol.getPath() + ".vmdk", morDc, true);

                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Destroy volume by derived name: " + vol.getPath() + "-flat.vmdk");
                }
                dsMo.deleteFile(vol.getPath() + "-flat.vmdk", morDc, true);
            }

            return new Answer(cmd, true, "Success");
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                this.hostService.invalidateServiceContext(null);
            }

            String msg = "delete volume failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    private Long restoreVolumeFromSecStorage(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName,
            String secStorageUrl, String secStorageDir, String backupName) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String srcOVAFileName = null;
        String srcOVFFileName = null;

        srcOVAFileName = secondaryMountPoint + "/" +  secStorageDir + "/"
                + backupName + "." + ImageFormat.OVA.getFileExtension();
        srcOVFFileName = secondaryMountPoint + "/" +  secStorageDir + "/"
                + backupName + ".ovf";

        String snapshotDir = "";
        if (backupName.contains("/")){
            snapshotDir = backupName.split("/")[0];
        }

        File ovafile = new File(srcOVAFileName);

        File ovfFile = new File(srcOVFFileName);
        // String srcFileName = getOVFFilePath(srcOVAFileName);
        if (!ovfFile.exists()) {
            srcOVFFileName = getOVFFilePath(srcOVAFileName);
            if(srcOVFFileName == null && ovafile.exists() ) {  // volss: ova file exists; o/w can't do tar
                Script command = new Script("tar", 0, s_logger);
                command.add("--no-same-owner");
                command.add("-xf", srcOVAFileName);
                command.setWorkDir(secondaryMountPoint + "/" +  secStorageDir + "/" + snapshotDir);
                s_logger.info("Executing command: " + command.toString());
                String result = command.execute();
                if(result != null) {
                    String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
                srcOVFFileName = getOVFFilePath(srcOVAFileName);
            } else if (srcOVFFileName == null){
                String msg = "Unable to find snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        if(srcOVFFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        VirtualMachineMO clonedVm = null;
        try {
            hyperHost.importVmFromOVF(srcOVFFileName, newVolumeName, primaryDsMo, "thin");
            clonedVm = hyperHost.findVmOnHyperHost(newVolumeName);
            if(clonedVm == null) {
                throw new Exception("Unable to create container VM for volume creation");
            }

            clonedVm.moveAllVmDiskFiles(primaryDsMo, "", false);
            clonedVm.detachAllDisks();
            return _storage.getSize(srcOVFFileName);
        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }

    @Override
    public Answer createVolumeFromSnapshot(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        DataTO destData = cmd.getDestTO();
        PrimaryDataStoreTO pool = (PrimaryDataStoreTO)destData.getDataStore();
        DataStoreTO imageStore = srcData.getDataStore();


        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        NfsTO nfsImageStore = (NfsTO)imageStore;
        String primaryStorageNameLabel = pool.getUuid();

        String secondaryStorageUrl = nfsImageStore.getUrl();
        String backedUpSnapshotUuid = snapshot.getPath();
        int index = backedUpSnapshotUuid.lastIndexOf(File.separator);
        String backupPath = backedUpSnapshotUuid.substring(0, index);
        backedUpSnapshotUuid = backedUpSnapshotUuid.substring(index + 1);
        String details = null;
        String newVolumeName = UUID.randomUUID().toString().replaceAll("-", "");

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            ManagedObjectReference morPrimaryDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost,
                    primaryStorageNameLabel);
            if (morPrimaryDs == null) {
                String msg = "Unable to find datastore: " + primaryStorageNameLabel;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO primaryDsMo = new DatastoreMO(hyperHost.getContext(), morPrimaryDs);
            restoreVolumeFromSecStorage(hyperHost, primaryDsMo,
                    newVolumeName, secondaryStorageUrl, backupPath, backedUpSnapshotUuid);

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(newVolumeName);
            return new CopyCmdAnswer(newVol);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);
            details = "create volume from snapshot exception: " + VmwareHelper.getExceptionMessage(e);
        }
        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer deleteSnapshot(DeleteCommand cmd) {
        SnapshotObjectTO snapshot = (SnapshotObjectTO)cmd.getData();
        DataStoreTO store = snapshot.getDataStore();
        if (store.getRole() == DataStoreRole.Primary) {
            return new Answer(cmd);
        } else {
            return new Answer(cmd, false, "unsupported command");
        }
    }
}
