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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.HostHostBusAdapter;
import com.vmware.vim25.HostInternetScsiHba;
import com.vmware.vim25.HostInternetScsiHbaAuthenticationProperties;
import com.vmware.vim25.HostInternetScsiHbaStaticTarget;
import com.vmware.vim25.HostInternetScsiTargetTransport;
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.HostScsiTopology;
import com.vmware.vim25.HostScsiTopologyInterface;
import com.vmware.vim25.HostScsiTopologyLun;
import com.vmware.vim25.HostScsiTopologyTarget;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;

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
import org.apache.cloudstack.storage.command.SnapshotAndCopyAnswer;
import org.apache.cloudstack.storage.command.SnapshotAndCopyCommand;
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
import com.cloud.hypervisor.vmware.mo.DatastoreFile;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostDatastoreSystemMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HostStorageSystemMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.NetworkDetails;
import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfo;
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
import com.cloud.storage.template.OVAProcessor;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine.PowerState;

public class VmwareStorageProcessor implements StorageProcessor {
    private static final Logger s_logger = Logger.getLogger(VmwareStorageProcessor.class);
    private static final int DEFAULT_NFS_PORT = 2049;

    private final VmwareHostService hostService;
    private final boolean _fullCloneFlag;
    private final VmwareStorageMount mountService;
    private final VmwareResource resource;
    private final Integer _timeout;
    protected Integer _shutdownWaitMs;
    private final Gson _gson;
    private final StorageLayer _storage = new JavaStorageLayer();

    public VmwareStorageProcessor(VmwareHostService hostService, boolean fullCloneFlag, VmwareStorageMount mountService, Integer timeout, VmwareResource resource,
            Integer shutdownWaitMs, PremiumSecondaryStorageResource storageResource) {
        this.hostService = hostService;
        _fullCloneFlag = fullCloneFlag;
        this.mountService = mountService;
        _timeout = timeout;
        this.resource = resource;
        _shutdownWaitMs = shutdownWaitMs;
        _gson = GsonHelper.getGsonLogger();
    }

    @Override
    public SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand cmd) {
        s_logger.info("'SnapshotAndCopyAnswer snapshotAndCopy(SnapshotAndCopyCommand)' not currently used for VmwareStorageProcessor");

        return new SnapshotAndCopyAnswer();
    }

    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert (_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if (files != null) {
            for (String fileName : files) {
                if (fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
    }

    private VirtualMachineMO copyTemplateFromSecondaryToPrimary(VmwareHypervisorHost hyperHost, DatastoreMO datastoreMo, String secondaryStorageUrl,
            String templatePathAtSecondaryStorage, String templateName, String templateUuid, boolean createSnapshot) throws Exception {

        s_logger.info("Executing copyTemplateFromSecondaryToPrimary. secondaryStorage: " + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " +
                templatePathAtSecondaryStorage + ", templateName: " + templateName);

        String secondaryMountPoint = mountService.getMountPoint(secondaryStorageUrl);
        s_logger.info("Secondary storage mount point: " + secondaryMountPoint);

        String srcOVAFileName =
                VmwareStorageLayoutHelper.getTemplateOnSecStorageFilePath(secondaryMountPoint, templatePathAtSecondaryStorage, templateName,
                        ImageFormat.OVA.getFileExtension());

        String srcFileName = getOVFFilePath(srcOVAFileName);
        if (srcFileName == null) {
            Script command = new Script("tar", 0, s_logger);
            command.add("--no-same-owner");
            command.add("-xf", srcOVAFileName);
            command.setWorkDir(secondaryMountPoint + "/" + templatePathAtSecondaryStorage);
            s_logger.info("Executing command: " + command.toString());
            String result = command.execute();
            if (result != null) {
                String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        srcFileName = getOVFFilePath(srcOVAFileName);
        if (srcFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        String vmName = templateUuid;
        hyperHost.importVmFromOVF(srcFileName, vmName, datastoreMo, "thin");

        VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
        if (vmMo == null) {
            String msg =
                    "Failed to import OVA template. secondaryStorage: " + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage +
                    ", templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if (createSnapshot) {
            if (vmMo.createSnapshot("cloud.template.base", "Base snapshot", false, false)) {
                // the same template may be deployed with multiple copies at per-datastore per-host basis,
                // save the original template name from CloudStack DB as the UUID to associate them.
                vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, templateName);
                vmMo.markAsTemplate();
            } else {
                vmMo.destroy();

                String msg = "Unable to create base snapshot for template, templateName: " + templateName + ", templateUuid: " + templateUuid;

                s_logger.error(msg);

                throw new Exception(msg);
            }
        }

        return vmMo;
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
        DataStoreTO primaryStore = destStore;

        String secondaryStorageUrl = nfsImageStore.getUrl();

        assert (secondaryStorageUrl != null);

        boolean managed = false;
        String storageHost = null;
        int storagePort = Integer.MIN_VALUE;
        String managedStoragePoolName = null;
        String managedStoragePoolRootVolumeName = null;
        String chapInitiatorUsername = null;
        String chapInitiatorSecret = null;
        String chapTargetUsername = null;
        String chapTargetSecret = null;

        if (destStore instanceof PrimaryDataStoreTO) {
            PrimaryDataStoreTO destPrimaryDataStoreTo = (PrimaryDataStoreTO)destStore;

            Map<String, String> details = destPrimaryDataStoreTo.getDetails();

            if (details != null) {
                managed = Boolean.parseBoolean(details.get(PrimaryDataStoreTO.MANAGED));

                if (managed) {
                    storageHost = details.get(PrimaryDataStoreTO.STORAGE_HOST);

                    try {
                        storagePort = Integer.parseInt(details.get(PrimaryDataStoreTO.STORAGE_PORT));
                    }
                    catch (Exception ex) {
                        storagePort = 3260;
                    }

                    managedStoragePoolName = details.get(PrimaryDataStoreTO.MANAGED_STORE_TARGET);
                    managedStoragePoolRootVolumeName = details.get(PrimaryDataStoreTO.MANAGED_STORE_TARGET_ROOT_VOLUME);
                    chapInitiatorUsername = details.get(PrimaryDataStoreTO.CHAP_INITIATOR_USERNAME);
                    chapInitiatorSecret = details.get(PrimaryDataStoreTO.CHAP_INITIATOR_SECRET);
                    chapTargetUsername = details.get(PrimaryDataStoreTO.CHAP_TARGET_USERNAME);
                    chapTargetSecret = details.get(PrimaryDataStoreTO.CHAP_TARGET_SECRET);
                }
            }
        }

        String templateUrl = secondaryStorageUrl + "/" + srcData.getPath();

        Pair<String, String> templateInfo = VmwareStorageLayoutHelper.decodeTemplateRelativePathAndNameFromUrl(secondaryStorageUrl, templateUrl, template.getName());

        VmwareContext context = hostService.getServiceContext(cmd);
        if (context == null) {
            return new CopyCmdAnswer("Failed to create a Vmware context, check the management server logs or the ssvm log for details");
        }

        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            String storageUuid = managed ? managedStoragePoolName : primaryStore.getUuid();
            String templateUuidName = deriveTemplateUuidOnHost(hyperHost, storageUuid, templateInfo.second());
            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
            VirtualMachineMO templateMo = VmwareHelper.pickOneVmOnRunningHost(dcMo.findVmByNameAndLabel(templateUuidName), true);
            DatastoreMO dsMo = null;

            if (templateMo == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Template " + templateInfo.second() + " is not setup yet. Set up template from secondary storage with uuid name: " + templateUuidName);
                }

                final ManagedObjectReference morDs;

                if (managed) {
                    morDs = prepareManagedDatastore(context, hyperHost, null, managedStoragePoolName, storageHost, storagePort,
                                chapInitiatorUsername, chapInitiatorSecret, chapTargetUsername, chapTargetSecret);
                }
                else {
                    morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, storageUuid);
                }

                assert (morDs != null);

                dsMo = new DatastoreMO(context, morDs);

                if (managed) {
                    VirtualMachineMO vmMo = copyTemplateFromSecondaryToPrimary(hyperHost, dsMo, secondaryStorageUrl, templateInfo.first(), templateInfo.second(),
                            managedStoragePoolRootVolumeName, false);

                    vmMo.unregisterVm();

                    String[] vmwareLayoutFilePair = VmwareStorageLayoutHelper.getVmdkFilePairDatastorePath(dsMo, managedStoragePoolRootVolumeName,
                            managedStoragePoolRootVolumeName, VmwareStorageLayoutType.VMWARE, false);
                    String[] legacyCloudStackLayoutFilePair = VmwareStorageLayoutHelper.getVmdkFilePairDatastorePath(dsMo, null,
                            managedStoragePoolRootVolumeName, VmwareStorageLayoutType.CLOUDSTACK_LEGACY, false);

                    dsMo.moveDatastoreFile(vmwareLayoutFilePair[0], dcMo.getMor(), dsMo.getMor(), legacyCloudStackLayoutFilePair[0], dcMo.getMor(), true);
                    dsMo.moveDatastoreFile(vmwareLayoutFilePair[1], dcMo.getMor(), dsMo.getMor(), legacyCloudStackLayoutFilePair[1], dcMo.getMor(), true);

                    String folderToDelete = dsMo.getDatastorePath(managedStoragePoolRootVolumeName, true);
                    dsMo.deleteFolder(folderToDelete, dcMo.getMor());
                }
                else {
                    copyTemplateFromSecondaryToPrimary(hyperHost, dsMo, secondaryStorageUrl, templateInfo.first(), templateInfo.second(),
                            templateUuidName, true);
                }
            } else {
                s_logger.info("Template " + templateInfo.second() + " has already been setup, skip the template setup process in primary storage");
            }

            TemplateObjectTO newTemplate = new TemplateObjectTO();

            if (managed) {
                if(dsMo != null) {
                    String path = dsMo.getDatastorePath(managedStoragePoolRootVolumeName + ".vmdk");
                    newTemplate.setPath(path);
                }
            }
            else {
                newTemplate.setPath(templateUuidName);
            }
            newTemplate.setSize(new Long(0)); // TODO: replace 0 with correct template physical_size.

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

    private boolean createVMLinkedClone(VirtualMachineMO vmTemplate, DatacenterMO dcMo, DatastoreMO dsMo, String vmdkName, ManagedObjectReference morDatastore,
            ManagedObjectReference morPool) throws Exception {
        ManagedObjectReference morBaseSnapshot = vmTemplate.getSnapshotMor("cloud.template.base");

        if (morBaseSnapshot == null) {
            String msg = "Unable to find template base snapshot, invalid template";

            s_logger.error(msg);

            throw new Exception(msg);
        }

        s_logger.info("creating linked clone from template");

        if (!vmTemplate.createLinkedClone(vmdkName, morBaseSnapshot, dcMo.getVmFolder(), morPool, morDatastore)) {
            String msg = "Unable to clone from the template";

            s_logger.error(msg);

            throw new Exception(msg);
        }

        return true;
    }

    private boolean createVMFullClone(VirtualMachineMO vmTemplate, DatacenterMO dcMo, DatastoreMO dsMo, String vmdkName, ManagedObjectReference morDatastore,
            ManagedObjectReference morPool) throws Exception {
        s_logger.info("creating full clone from template");

        if (!vmTemplate.createFullClone(vmdkName, dcMo.getVmFolder(), morPool, morDatastore)) {
            String msg = "Unable to create full clone from the template";

            s_logger.error(msg);

            throw new Exception(msg);
        }

        return true;
    }

    @Override
    public Answer cloneVolumeFromBaseTemplate(CopyCommand cmd) {
        DataTO srcData = cmd.getSrcTO();
        TemplateObjectTO template = (TemplateObjectTO)srcData;
        DataTO destData = cmd.getDestTO();
        VolumeObjectTO volume = (VolumeObjectTO)destData;
        DataStoreTO primaryStore = volume.getDataStore();
        DataStoreTO srcStore = template.getDataStore();

        try {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
            VirtualMachineMO vmMo = null;
            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());
            if (morDatastore == null) {
                throw new Exception("Unable to find datastore in vSphere");
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDatastore);

            String vmdkName = volume.getName();
            String vmdkFileBaseName = null;
            if (srcStore == null) {
                // create a root volume for blank VM (created from ISO)
                String dummyVmName = hostService.getWorkerName(context, cmd, 0);

                try {
                    vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, dummyVmName);
                    if (vmMo == null) {
                        throw new Exception("Unable to create a dummy VM for volume creation");
                    }

                    vmdkFileBaseName = vmMo.getVmdkFileBaseNames().get(0);
                    // we only use the first file in the pair, linked or not will not matter
                    String vmdkFilePair[] = VmwareStorageLayoutHelper.getVmdkFilePairDatastorePath(dsMo, null, vmdkFileBaseName, VmwareStorageLayoutType.CLOUDSTACK_LEGACY, true);
                    String volumeDatastorePath = vmdkFilePair[0];
                    synchronized (this) {
                        s_logger.info("Delete file if exists in datastore to clear the way for creating the volume. file: " + volumeDatastorePath);
                        VmwareStorageLayoutHelper.deleteVolumeVmdkFiles(dsMo, vmdkName, dcMo);
                        vmMo.createDisk(volumeDatastorePath, (int)(volume.getSize() / (1024L * 1024L)), morDatastore, -1);
                        vmMo.detachDisk(volumeDatastorePath, false);
                    }
                } finally {
                    s_logger.info("Destroy dummy VM after volume creation");
                    if (vmMo != null) {
                        s_logger.warn("Unable to destroy a null VM ManagedObjectReference");
                        vmMo.detachAllDisks();
                        vmMo.destroy();
                    }
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
                if (!_fullCloneFlag) {
                    createVMLinkedClone(vmTemplate, dcMo, dsMo, vmdkName, morDatastore, morPool);
                } else {
                    createVMFullClone(vmTemplate, dcMo, dsMo, vmdkName, morDatastore, morPool);
                }

                vmMo = new ClusterMO(context, morCluster).findVmOnHyperHost(vmdkName);
                assert (vmMo != null);

                vmdkFileBaseName = vmMo.getVmdkFileBaseNames().get(0); // TO-DO: Support for base template containing multiple disks
                s_logger.info("Move volume out of volume-wrapper VM ");
                String[] vmwareLayoutFilePair = VmwareStorageLayoutHelper.getVmdkFilePairDatastorePath(dsMo, vmdkName, vmdkFileBaseName, VmwareStorageLayoutType.VMWARE, !_fullCloneFlag);
                String[] legacyCloudStackLayoutFilePair = VmwareStorageLayoutHelper.getVmdkFilePairDatastorePath(dsMo, vmdkName, vmdkFileBaseName, VmwareStorageLayoutType.CLOUDSTACK_LEGACY, !_fullCloneFlag);

                dsMo.moveDatastoreFile(vmwareLayoutFilePair[0], dcMo.getMor(), dsMo.getMor(), legacyCloudStackLayoutFilePair[0], dcMo.getMor(), true);
                dsMo.moveDatastoreFile(vmwareLayoutFilePair[1], dcMo.getMor(), dsMo.getMor(), legacyCloudStackLayoutFilePair[1], dcMo.getMor(), true);

                s_logger.info("detach disks from volume-wrapper VM " + vmdkName);
                vmMo.detachAllDisks();

                s_logger.info("destroy volume-wrapper VM " + vmdkName);
                vmMo.destroy();

                String srcFile = dsMo.getDatastorePath(vmdkName, true);
                dsMo.deleteFile(srcFile, dcMo.getMor(), true);
            }
            // restoreVM - move the new ROOT disk into corresponding VM folder
            VirtualMachineMO restoreVmMo = dcMo.findVm(volume.getVmName());
            if (restoreVmMo != null) {
                String vmNameInVcenter = restoreVmMo.getName(); // VM folder name in datastore will be VM's name in vCenter.
                if (dsMo.folderExists(String.format("[%s]", dsMo.getName()), vmNameInVcenter)) {
                    VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmNameInVcenter, dsMo, vmdkFileBaseName);
                }
            }

            VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(vmdkFileBaseName);
            newVol.setSize(volume.getSize());
            return new CopyCmdAnswer(newVol);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                hostService.invalidateServiceContext(null);
            }

            String msg = "clone volume from base image failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new CopyCmdAnswer(e.toString());
        }
    }

    private Pair<String, String> copyVolumeFromSecStorage(VmwareHypervisorHost hyperHost, String srcVolumePath, DatastoreMO dsMo, String secStorageUrl, long wait) throws Exception {

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

        String newVolume = VmwareHelper.getVCenterSafeUuid();
        restoreVolumeFromSecStorage(hyperHost, dsMo, newVolume, secStorageUrl, volumeFolder, volumeName, wait);

        return new Pair<String, String>(volumeFolder, newVolume);
    }

    private String deleteVolumeDirOnSecondaryStorage(String volumeDir, String secStorageUrl) throws Exception {
        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String volumeMountRoot = secondaryMountPoint + File.separator + volumeDir;

        return deleteDir(volumeMountRoot);
    }

    private String deleteDir(String dir) {
        synchronized (dir.intern()) {
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
            DataStoreTO destStore = destVolume.getDataStore();

            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            String uuid = destStore.getUuid();

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, uuid);
            if (morDatastore == null) {
                URI uri = new URI(destStore.getUrl());

                morDatastore = hyperHost.mountDatastore(false, uri.getHost(), 0, uri.getPath(), destStore.getUuid().replace("-", ""));

                if (morDatastore == null) {
                    throw new Exception("Unable to mount storage pool on host. storeUrl: " + uri.getHost() + ":/" + uri.getPath());
                }
            }

            Pair<String, String> result = copyVolumeFromSecStorage(hyperHost, srcVolume.getPath(), new DatastoreMO(context, morDatastore), srcStore.getUrl(), (long)cmd.getWait() * 1000);
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

    private Pair<String, String> copyVolumeToSecStorage(VmwareHostService hostService, VmwareHypervisorHost hyperHost, CopyCommand cmd, String vmName, String poolId,
            String volumePath, String destVolumePath, String secStorageUrl, String workerVmName) throws Exception {
        VirtualMachineMO workerVm = null;
        VirtualMachineMO vmMo = null;
        String exportName = UUID.randomUUID().toString().replace("-", "");

        try {
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolId);

            if (morDs == null) {
                String msg = "Unable to find volumes's storage pool for copy volume operation";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null || VmwareResource.getVmState(vmMo) == PowerState.PowerOff) {
                // create a dummy worker vm for attaching the volume
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
                workerVm = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, workerVmName);

                if (workerVm == null) {
                    String msg = "Unable to create worker VM to execute CopyVolumeCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                // attach volume to worker VM
                String datastoreVolumePath = getVolumePathInDatastore(dsMo, volumePath + ".vmdk");
                workerVm.attachDisk(new String[] {datastoreVolumePath}, morDs);
                vmMo = workerVm;
            }

            vmMo.createSnapshot(exportName, "Temporary snapshot for copy-volume command", false, false);

            exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl, destVolumePath, exportName, hostService.getWorkerName(hyperHost.getContext(), cmd, 1));
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
            DataStoreTO primaryStorage = srcVolume.getDataStore();
            NfsTO destStore = (NfsTO)destVolume.getDataStore();
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            Pair<String, String> result;

            result =
                    copyVolumeToSecStorage(hostService, hyperHost, cmd, vmName, primaryStorage.getUuid(), srcVolume.getPath(), destVolume.getPath(), destStore.getUrl(),
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

    private void postCreatePrivateTemplate(String installFullPath, long templateId, String templateName, long size, long virtualSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/template.properties"),"UTF-8"));
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
            if (out != null) {
                out.close();
            }
        }
    }

    private Ternary<String, Long, Long> createTemplateFromVolume(VirtualMachineMO vmMo, String installPath, long templateId, String templateUniqueName,
            String secStorageUrl, String volumePath, String workerVmName) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        synchronized (installPath.intern()) {
            Script command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);

            String result = command.execute();
            if (result != null) {
                String msg = "unable to prepare template directory: " + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        VirtualMachineMO clonedVm = null;
        try {
            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath);
            if (volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            if (!vmMo.createSnapshot(templateUniqueName, "Temporary snapshot for template creation", false, false)) {
                String msg = "Unable to take snapshot for creating template from volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            // 4 MB is the minimum requirement for VM memory in VMware
            Pair<VirtualMachineMO, String[]> cloneResult =
                    vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = cloneResult.first();

            clonedVm.exportVm(secondaryMountPoint + "/" + installPath, templateUniqueName, false, false);

            // Get VMDK filename
            String templateVMDKName = "";
            File[] files = new File(installFullPath).listFiles();
            if(files != null) {
                for(File file : files) {
                    String fileName = file.getName();
                    if(fileName.toLowerCase().startsWith(templateUniqueName) && fileName.toLowerCase().endsWith(".vmdk")) {
                        templateVMDKName += fileName;
                        break;
                    }
                }
            }

            long physicalSize = new File(installFullPath + "/" + templateVMDKName).length();
            OVAProcessor processor = new OVAProcessor();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("OVA Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, templateUniqueName);

            postCreatePrivateTemplate(installFullPath, templateId, templateUniqueName, physicalSize, virtualSize);
            writeMetaOvaForTemplate(installFullPath, templateUniqueName + ".ovf", templateVMDKName, templateUniqueName, physicalSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);

        } finally {
            if (clonedVm != null) {
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
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find the owner VM for CreatePrivateTemplateFromVolumeCommand on host " + hyperHost.getHyperHostName() +
                            ", try within datacenter");
                }
                vmMo = hyperHost.findVmOnPeerHyperHost(volume.getVmName());

                if (vmMo == null) {
                    // This means either the volume is on a zone wide storage pool or VM is deleted by external entity.
                    // Look for the VM in the datacenter.
                    ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                    DatacenterMO dcMo = new DatacenterMO(context, dcMor);
                    vmMo = dcMo.findVm(volume.getVmName());
                }

                if (vmMo == null) {
                    String msg = "Unable to find the owner VM for volume operation. vm: " + volume.getVmName();
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            Ternary<String, Long, Long> result =
                    createTemplateFromVolume(vmMo, template.getPath(), template.getId(), template.getName(), secondaryStoragePoolURL, volumePath,
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

    private void writeMetaOvaForTemplate(String installFullPath, String ovfFilename, String vmdkFilename, String templateName, long diskSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/" + templateName + ".ova.meta"),"UTF-8"));
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
            if (out != null) {
                out.close();
            }
        }
    }

    private Ternary<String, Long, Long> createTemplateFromSnapshot(String installPath, String templateUniqueName, String secStorageUrl, String snapshotPath,
            Long templateId, long wait) throws Exception {
        //Snapshot path is decoded in this form: /snapshots/account/volumeId/uuid/uuid
        String backupSSUuid;
        String snapshotFolder;
        if (snapshotPath.endsWith(".ova")) {
            int index = snapshotPath.lastIndexOf(File.separator);
            backupSSUuid = snapshotPath.substring(index + 1).replace(".ova", "");
            snapshotFolder = snapshotPath.substring(0, index);
        } else {
            String[] tokens = snapshotPath.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
            backupSSUuid = tokens[tokens.length - 1];
            snapshotFolder = StringUtils.join(tokens, File.separator, 0, tokens.length - 1);
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

        synchronized (installPath.intern()) {
            command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);

            result = command.execute();
            if (result != null) {
                String msg = "unable to prepare template directory: " + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }

        try {
            if (new File(snapshotFullOVAName).exists()) {
                command = new Script(false, "cp", wait, s_logger);
                command.add(snapshotFullOVAName);
                command.add(installFullOVAName);
                result = command.execute();
                if (result != null) {
                    String msg = "unable to copy snapshot " + snapshotFullOVAName + " to " + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                // untar OVA file at template directory
                command = new Script("tar", wait, s_logger);
                command.add("--no-same-owner");
                command.add("-xf", installFullOVAName);
                command.setWorkDir(installFullPath);
                s_logger.info("Executing command: " + command.toString());
                result = command.execute();
                if (result != null) {
                    String msg = "unable to untar snapshot " + snapshotFullOVAName + " to " + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

            } else {  // there is no ova file, only ovf originally;
                if (new File(snapshotFullOvfName).exists()) {
                    command = new Script(false, "cp", wait, s_logger);
                    command.add(snapshotFullOvfName);
                    //command.add(installFullOvfName);
                    command.add(installFullPath);
                    result = command.execute();
                    if (result != null) {
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
                        if (vmdkfile.toLowerCase().startsWith(backupSSUuid) && vmdkfile.toLowerCase().endsWith(".vmdk")) {
                            snapshotFullVMDKName = snapshotRoot + File.separator + vmdkfile;
                            templateVMDKName += vmdkfile;
                            break;
                        }
                    }
                    if (snapshotFullVMDKName != null) {
                        command = new Script(false, "cp", wait, s_logger);
                        command.add(snapshotFullVMDKName);
                        command.add(installFullPath);
                        result = command.execute();
                        s_logger.info("Copy VMDK file: " + snapshotFullVMDKName);
                        if (result != null) {
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
            OVAProcessor processor = new OVAProcessor();
            // long physicalSize = new File(installFullPath + "/" + templateUniqueName + ".ova").length();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("OVA Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, templateUniqueName);

            postCreatePrivateTemplate(installFullPath, templateId, templateUniqueName, physicalSize, virtualSize);
            writeMetaOvaForTemplate(installFullPath, backupSSUuid + ".ovf", templateVMDKName, templateUniqueName, physicalSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);
        } finally {
            // TODO, clean up left over files
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
            if (!(imageStore instanceof NfsTO)) {
                return new CopyCmdAnswer("Only support create template from snapshot, when the dest store is nfs");
            }

            NfsTO nfsSvr = (NfsTO)imageStore;
            Ternary<String, Long, Long> result = createTemplateFromSnapshot(template.getPath(), uniqeName, nfsSvr.getUrl(), snapshot.getPath(), template.getId(), (long)cmd.getWait() * 1000);

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

    // return Pair<String(divice bus name), String[](disk chain)>
    private Pair<String, String[]> exportVolumeToSecondaryStroage(VirtualMachineMO vmMo, String volumePath, String secStorageUrl, String secStorageDir,
            String exportName, String workerVmName) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String exportPath = secondaryMountPoint + "/" + secStorageDir + "/" + exportName;

        synchronized (exportPath.intern()) {
            if (!new File(exportPath).exists()) {
                Script command = new Script(false, "mkdir", _timeout, s_logger);
                command.add("-p");
                command.add(exportPath);
                if (command.execute() != null) {
                    throw new Exception("unable to prepare snapshot backup directory");
                }
            }
        }

        VirtualMachineMO clonedVm = null;
        try {

            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath);
            if (volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            // 4 MB is the minimum requirement for VM memory in VMware
            Pair<VirtualMachineMO, String[]> cloneResult =
                    vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = cloneResult.first();
            String disks[] = cloneResult.second();

            clonedVm.exportVm(exportPath, exportName, false, false);
            return new Pair<String, String[]>(volumeDeviceInfo.second(), disks);
        } finally {
            if (clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }

    // Ternary<String(backup uuid in secondary storage), String(device bus name), String[](original disk chain in the snapshot)>
    private Ternary<String, String, String[]> backupSnapshotToSecondaryStorage(VirtualMachineMO vmMo, String installPath, String volumePath, String snapshotUuid,
            String secStorageUrl, String prevSnapshotUuid, String prevBackupUuid, String workerVmName) throws Exception {

        String backupUuid = UUID.randomUUID().toString();
        Pair<String, String[]> snapshotInfo = exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl, installPath, backupUuid, workerVmName);
        return new Ternary<String, String, String[]>(backupUuid, snapshotInfo.first(), snapshotInfo.second());
    }

    @Override
    public Answer backupSnapshot(CopyCommand cmd) {
        SnapshotObjectTO srcSnapshot = (SnapshotObjectTO)cmd.getSrcTO();
        DataStoreTO primaryStore = srcSnapshot.getDataStore();
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
        VirtualMachineMO workerVm = null;
        String workerVMName = null;
        String volumePath = srcSnapshot.getVolume().getPath();
        ManagedObjectReference morDs = null;
        DatastoreMO dsMo = null;

        // By default assume failure
        String details = null;
        boolean success = false;
        String snapshotBackupUuid = null;

        boolean hasOwnerVm = false;
        Ternary<String, String, String[]> backupResult = null;

        VmwareContext context = hostService.getServiceContext(cmd);
        VirtualMachineMO vmMo = null;
        String vmName = srcSnapshot.getVmName();
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());

            CopyCmdAnswer answer = null;

            try {
                if(vmName != null) {
                    vmMo = hyperHost.findVmOnHyperHost(vmName);
                    if (vmMo == null) {
                        if(s_logger.isDebugEnabled()) {
                            s_logger.debug("Unable to find owner VM for BackupSnapshotCommand on host " + hyperHost.getHyperHostName() + ", will try within datacenter");
                        }
                        vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
                    }
                }
                if(vmMo == null) {
                    dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
                    workerVMName = hostService.getWorkerName(context, cmd, 0);
                    vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, workerVMName);
                    if (vmMo == null) {
                        throw new Exception("Failed to find the newly create or relocated VM. vmName: " + workerVMName);
                    }
                    workerVm = vmMo;
                    // attach volume to worker VM
                    String datastoreVolumePath = dsMo.getDatastorePath(volumePath + ".vmdk");
                    vmMo.attachDisk(new String[] { datastoreVolumePath }, morDs);
                } else {
                    s_logger.info("Using owner VM " + vmName + " for snapshot operation");
                    hasOwnerVm = true;
                }

                if (!vmMo.createSnapshot(snapshotUuid, "Snapshot taken for " + srcSnapshot.getName(), false, false)) {
                    throw new Exception("Failed to take snapshot " + srcSnapshot.getName() + " on vm: " + vmName);
                }

                backupResult =
                        backupSnapshotToSecondaryStorage(vmMo, destSnapshot.getPath(), srcSnapshot.getVolume().getPath(), snapshotUuid, secondaryStorageUrl,
                                prevSnapshotUuid, prevBackupUuid, hostService.getWorkerName(context, cmd, 1));
                snapshotBackupUuid = backupResult.first();

                success = (snapshotBackupUuid != null);
                if (!success) {
                    details = "Failed to backUp the snapshot with uuid: " + snapshotUuid + " to secondary storage.";
                    answer = new CopyCmdAnswer(details);
                } else {
                    details = "Successfully backedUp the snapshot with Uuid: " + snapshotUuid + " to secondary storage.";

                    // Get snapshot physical size
                    long physicalSize = 0l;
                    String secondaryMountPoint = mountService.getMountPoint(secondaryStorageUrl);
                    String snapshotDir =  destSnapshot.getPath() + "/" + snapshotBackupUuid;
                    File[] files = new File(secondaryMountPoint + "/" + snapshotDir).listFiles();
                    if(files != null) {
                        for(File file : files) {
                            String fileName = file.getName();
                            if(fileName.toLowerCase().startsWith(snapshotBackupUuid) && fileName.toLowerCase().endsWith(".vmdk")) {
                                physicalSize = new File(secondaryMountPoint + "/" + snapshotDir + "/" + fileName).length();
                                break;
                            }
                        }
                    }

                    SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
                    newSnapshot.setPath(snapshotDir + "/" + snapshotBackupUuid);
                    newSnapshot.setPhysicalSize(physicalSize);
                    answer = new CopyCmdAnswer(newSnapshot);
                }
            } finally {
                if (vmMo != null) {
                    ManagedObjectReference snapshotMor = vmMo.getSnapshotMor(snapshotUuid);
                    if (snapshotMor != null) {
                        vmMo.removeSnapshot(snapshotUuid, false);

                        // Snapshot operation may cause disk consolidation in VMware, when this happens
                        // we need to update CloudStack DB
                        //
                        // TODO: this post operation fixup is not atomic and not safe when management server stops
                        // in the middle
                        if (backupResult != null && hasOwnerVm) {
                            s_logger.info("Check if we have disk consolidation after snapshot operation");

                            boolean chainConsolidated = false;
                            for (String vmdkDsFilePath : backupResult.third()) {
                                s_logger.info("Validate disk chain file:" + vmdkDsFilePath);

                                if (vmMo.getDiskDevice(vmdkDsFilePath) == null) {
                                    s_logger.info("" + vmdkDsFilePath + " no longer exists, consolidation detected");
                                    chainConsolidated = true;
                                    break;
                                } else {
                                    s_logger.info("" + vmdkDsFilePath + " is found still in chain");
                                }
                            }

                            if (chainConsolidated) {
                                String topVmdkFilePath = null;
                                try {
                                    topVmdkFilePath = vmMo.getDiskCurrentTopBackingFileInChain(backupResult.second());
                                } catch (Exception e) {
                                    s_logger.error("Unexpected exception", e);
                                }

                                s_logger.info("Disk has been consolidated, top VMDK is now: " + topVmdkFilePath);
                                if (topVmdkFilePath != null) {
                                    DatastoreFile file = new DatastoreFile(topVmdkFilePath);

                                    SnapshotObjectTO snapshotInfo = (SnapshotObjectTO)answer.getNewData();
                                    VolumeObjectTO vol = new VolumeObjectTO();
                                    vol.setUuid(srcSnapshot.getVolume().getUuid());
                                    vol.setPath(file.getFileBaseName());
                                    snapshotInfo.setVolume(vol);
                                } else {
                                    s_logger.error("Disk has been consolidated, but top VMDK is not found ?!");
                                }
                            }
                        }
                    } else {
                        s_logger.error("Can not find the snapshot we just used ?!");
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

            return answer;
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
        Map<String, String> details = cmd.getDisk().getDetails();
        boolean isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
        String iScsiName = details.get(DiskTO.IQN);
        String storageHost = details.get(DiskTO.STORAGE_HOST);
        int storagePort = Integer.parseInt(details.get(DiskTO.STORAGE_PORT));

        return this.attachVolume(cmd, cmd.getDisk(), true, isManaged, cmd.getVmName(), iScsiName, storageHost, storagePort);
    }

    private Answer attachVolume(Command cmd, DiskTO disk, boolean isAttach, boolean isManaged, String vmName, String iScsiName, String storageHost, int storagePort) {
        VolumeObjectTO volumeTO = (VolumeObjectTO)disk.getData();
        DataStoreTO primaryStore = volumeTO.getDataStore();
        try {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                String msg = "Unable to find the VM to execute AttachVolumeCommand, vmName: " + vmName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
            vmName = vmMo.getName();

            ManagedObjectReference morDs = null;
            String diskUuid =  volumeTO.getUuid().replace("-", "");

            if (isAttach && isManaged) {
                Map<String, String> details = disk.getDetails();

                morDs = prepareManagedStorage(context, hyperHost, diskUuid, iScsiName, storageHost, storagePort, null,
                            details.get(DiskTO.CHAP_INITIATOR_USERNAME), details.get(DiskTO.CHAP_INITIATOR_SECRET),
                            details.get(DiskTO.CHAP_TARGET_USERNAME), details.get(DiskTO.CHAP_TARGET_SECRET),
                            volumeTO.getSize(), cmd);
            }
            else {
                if (storagePort == DEFAULT_NFS_PORT) {
                    morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, isManaged ? VmwareResource.getDatastoreName(diskUuid) : primaryStore.getUuid());
                } else {
                    morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, isManaged ? VmwareResource.getDatastoreName(iScsiName) : primaryStore.getUuid());
                }
            }

            if (morDs == null) {
                String msg = "Unable to find the mounted datastore to execute AttachVolumeCommand, vmName: " + vmName;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDs);
            String datastoreVolumePath;

            if (isAttach) {
                if (isManaged) {
                    datastoreVolumePath = dsMo.getDatastorePath(dsMo.getName() + ".vmdk");
                } else {
                    datastoreVolumePath = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dsMo.getOwnerDatacenter().first(), vmName, dsMo, volumeTO.getPath());
                }
            } else {
                if (isManaged) {
                    datastoreVolumePath = dsMo.getDatastorePath(dsMo.getName() + ".vmdk");
                } else {
                    datastoreVolumePath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(dsMo, volumeTO.getPath() + ".vmdk");

                    if (!dsMo.fileExists(datastoreVolumePath)) {
                        datastoreVolumePath = VmwareStorageLayoutHelper.getVmwareDatastorePathFromVmdkFileName(dsMo, vmName, volumeTO.getPath() + ".vmdk");
                    }
                }
            }

            disk.setPath(datastoreVolumePath);

            AttachAnswer answer = new AttachAnswer(disk);

            if (isAttach) {
                vmMo.attachDisk(new String[] { datastoreVolumePath }, morDs);
            } else {
                vmMo.removeAllSnapshots();
                vmMo.detachDisk(datastoreVolumePath, false);

                if (isManaged) {
                    handleDatastoreAndVmdkDetachManaged(diskUuid, iScsiName, storageHost, storagePort);
                } else {
                    VmwareStorageLayoutHelper.syncVolumeToRootFolder(dsMo.getOwnerDatacenter().first(), dsMo, volumeTO.getPath(), vmName);
                }
            }

            return answer;
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                hostService.invalidateServiceContext(null);
            }

            String msg = "";
            if (isAttach)
                msg += "Failed to attach volume: " + e.getMessage();
            else
                msg += "Failed to detach volume: " + e.getMessage();
            s_logger.error(msg, e);
            return new AttachAnswer(msg);
        }
    }

    private static String getSecondaryDatastoreUUID(String storeUrl) {
        String uuid = null;
        try{
            uuid=UUID.nameUUIDFromBytes(storeUrl.getBytes("UTF-8")).toString();
        }catch(UnsupportedEncodingException e){
            s_logger.warn("Failed to create UUID from string " + storeUrl + ". Bad storeUrl or UTF-8 encoding error." );
        }
        return uuid;
    }

    public synchronized ManagedObjectReference prepareSecondaryDatastoreOnHost(String storeUrl) throws Exception {
        String storeName = getSecondaryDatastoreUUID(storeUrl);
        URI uri = new URI(storeUrl);

        VmwareHypervisorHost hyperHost = hostService.getHyperHost(hostService.getServiceContext(null), null);
        ManagedObjectReference morDatastore = hyperHost.mountDatastore(false, uri.getHost(), 0, uri.getPath(), storeName.replace("-", ""));

        if (morDatastore == null) {
            throw new Exception("Unable to mount secondary storage on host. storeUrl: " + storeUrl);
        }

        return morDatastore;
    }

    private Answer attachIso(DiskTO disk, boolean isAttach, String vmName) {
        try {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
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
                            if (!vmMo.unmountToolsInstaller()) {
                                return new AttachAnswer("Failed to unmount vmware-tools installer ISO as the corresponding CDROM device is locked by VM. Please unmount the CDROM device inside the VM and ret-try.");
                            }
                        } catch(Throwable e){
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
            DatastoreMO secondaryDsMo = new DatastoreMO(context, morSecondaryDs);
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
                hostService.invalidateServiceContext(null);
            }

            if (isAttach) {
                String msg = "AttachIsoCommand(attach) failed due to " + VmwareHelper.getExceptionMessage(e);
                msg = msg + " Also check if your guest os is a supported version";
                s_logger.error(msg, e);
                return new AttachAnswer(msg);
            } else {
                String msg = "AttachIsoCommand(detach) failed due to " + VmwareHelper.getExceptionMessage(e);
                msg = msg + " Also check if your guest os is a supported version";
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

    @Override
    public Answer createVolume(CreateObjectCommand cmd) {

        VolumeObjectTO volume = (VolumeObjectTO)cmd.getData();
        DataStoreTO primaryStore = volume.getDataStore();

        try {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStore.getUuid());
            if (morDatastore == null) {
                throw new Exception("Unable to find datastore in vSphere");
            }

            DatastoreMO dsMo = new DatastoreMO(context, morDatastore);
            // create data volume
            VirtualMachineMO vmMo = null;
            String volumeUuid = UUID.randomUUID().toString().replace("-", "");

            String volumeDatastorePath = dsMo.getDatastorePath(volumeUuid + ".vmdk");
            String dummyVmName = hostService.getWorkerName(context, cmd, 0);
            try {
                s_logger.info("Create worker VM " + dummyVmName);
                vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, dummyVmName);
                if (vmMo == null) {
                    throw new Exception("Unable to create a dummy VM for volume creation");
                }

                synchronized (this) {
                    // s_logger.info("Delete file if exists in datastore to clear the way for creating the volume. file: " + volumeDatastorePath);
                    VmwareStorageLayoutHelper.deleteVolumeVmdkFiles(dsMo, volumeUuid.toString(), dcMo);

                    vmMo.createDisk(volumeDatastorePath, (int)(volume.getSize() / (1024L * 1024L)), morDatastore, vmMo.getScsiDeviceControllerKey());
                    vmMo.detachDisk(volumeDatastorePath, false);
                }

                VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(volumeUuid);
                newVol.setSize(volume.getSize());
                return new CreateObjectAnswer(newVol);
            } finally {
                s_logger.info("Destroy dummy VM after volume creation");
                if (vmMo != null) {
                    vmMo.detachAllDisks();
                    vmMo.destroy();
                }
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                hostService.invalidateServiceContext(null);
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

    // format: [datastore_name] file_name.vmdk (the '[' and ']' chars should only be used to denote the datastore)
    private String getManagedDatastoreNameFromPath(String path) {
        int lastIndexOf = path.lastIndexOf("]");

        return path.substring(1, lastIndexOf);
    }

    @Override
    public Answer deleteVolume(DeleteCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource DeleteCommand: " + _gson.toJson(cmd));
        }

        try {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
            VolumeObjectTO vol = (VolumeObjectTO)cmd.getData();
            DataStoreTO store = vol.getDataStore();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO)store;

            Map<String, String> details = primaryDataStoreTO.getDetails();
            boolean isManaged = false;
            String managedDatastoreName = null;

            if (details != null) {
                isManaged = Boolean.parseBoolean(details.get(PrimaryDataStoreTO.MANAGED));

                if (isManaged) {
                    managedDatastoreName = getManagedDatastoreNameFromPath(vol.getPath());
                }
            }

            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost,
                    isManaged ? managedDatastoreName : store.getUuid());

            if (morDs == null) {
                String msg = "Unable to find datastore based on volume mount point " + store.getUuid();
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
                    if (vmMo == null) {
                        // Volume might be on a zone-wide storage pool, look for VM in datacenter
                        DatacenterMO dcMo = new DatacenterMO(context, morDc);
                        vmMo = dcMo.findVm(vmName);
                    }
                    if (vmMo != null) {
                        if (s_logger.isInfoEnabled()) {
                            s_logger.info("Destroy root volume and VM itself. vmName " + vmName);
                        }

                        // Remove all snapshots to consolidate disks for removal
                        vmMo.removeAllSnapshots();

                        VirtualMachineDiskInfo diskInfo = null;
                        if (vol.getChainInfo() != null)
                            diskInfo = _gson.fromJson(vol.getChainInfo(), VirtualMachineDiskInfo.class);

                        HostMO hostMo = vmMo.getRunningHost();
                        List<NetworkDetails> networks = vmMo.getNetworksWithDetails();

                        // tear down all devices first before we destroy the VM to avoid accidently delete disk backing files
                        if (VmwareResource.getVmState(vmMo) != PowerState.PowerOff) {
                            vmMo.safePowerOff(_shutdownWaitMs);
                        }

                        // call this before calling detachAllDisksExcept
                        // when expunging a VM, we need to see if any of its disks are serviced by managed storage
                        // if there is one or more disk serviced by managed storage, remove the iSCSI connection(s)
                        // don't remove the iSCSI connection(s) until the supported disk(s) is/are removed from the VM
                        // (removeManagedTargetsFromCluster should be called after detachAllDisksExcept and vm.destroy)
                        List<VirtualDisk> virtualDisks = vmMo.getVirtualDisks();
                        List<String> managedIqns = getManagedIqnsFromVirtualDisks(virtualDisks);

                        List<String> detachedDisks = vmMo.detachAllDisksExcept(vol.getPath(), diskInfo != null ? diskInfo.getDiskDeviceBusName() : null);
                        VmwareStorageLayoutHelper.moveVolumeToRootFolder(new DatacenterMO(context, morDc), detachedDisks);

                        // let vmMo.destroy to delete volume for us
                        // vmMo.tearDownDevices(new Class<?>[] { VirtualDisk.class, VirtualEthernetCard.class });

                        if (isManaged) {
                            vmMo.unregisterVm();
                        }
                        else {
                            vmMo.destroy();
                        }

                        // this.hostService.handleDatastoreAndVmdkDetach(iScsiName, storageHost, storagePort);
                        if (managedIqns != null && !managedIqns.isEmpty()) {
                            removeManagedTargetsFromCluster(managedIqns);
                        }

                        for (NetworkDetails netDetails : networks) {
                            if (netDetails.getGCTag() != null && netDetails.getGCTag().equalsIgnoreCase("true")) {
                                if (netDetails.getVMMorsOnNetwork() == null || netDetails.getVMMorsOnNetwork().length == 1) {
                                    resource.cleanupNetwork(hostMo, netDetails);
                                }
                            }
                        }
                    }

                    /*
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Destroy volume by original name: " + vol.getPath() + ".vmdk");
                    }

                    VmwareStorageLayoutHelper.deleteVolumeVmdkFiles(dsMo, vol.getPath(), new DatacenterMO(context, morDc));
                     */
                    return new Answer(cmd, true, "Success");
                }

                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Destroy root volume directly from datastore");
                }
            }

            VmwareStorageLayoutHelper.deleteVolumeVmdkFiles(dsMo, vol.getPath(), new DatacenterMO(context, morDc));

            return new Answer(cmd, true, "Success");
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                hostService.invalidateServiceContext(null);
            }

            String msg = "delete volume failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    public ManagedObjectReference prepareManagedDatastore(VmwareContext context, VmwareHypervisorHost hyperHost, String datastoreName,
            String iScsiName, String storageHost, int storagePort) throws Exception {
        return getVmfsDatastore(context, hyperHost, datastoreName, storageHost, storagePort, trimIqn(iScsiName), null, null, null, null);
    }

    private ManagedObjectReference prepareManagedDatastore(VmwareContext context, VmwareHypervisorHost hyperHost, String diskUuid, String iScsiName,
                String storageHost, int storagePort, String chapInitiatorUsername, String chapInitiatorSecret,
                String chapTargetUsername, String chapTargetSecret) throws Exception {
        if (storagePort == DEFAULT_NFS_PORT) {
            s_logger.info("creating the NFS datastore with the following configuration - storageHost: " + storageHost + ", storagePort: " + storagePort +
                          ", exportpath: " + iScsiName + "and diskUuid : " + diskUuid);
            ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
            ClusterMO cluster = new ClusterMO(context, morCluster);
            List<Pair<ManagedObjectReference, String>> lstHosts = cluster.getClusterHosts();

            HostMO host = new HostMO(context, lstHosts.get(0).first());
            HostDatastoreSystemMO hostDatastoreSystem = host.getHostDatastoreSystemMO();

            return hostDatastoreSystem.createNfsDatastore(storageHost, storagePort, iScsiName, diskUuid);
         } else {
             return getVmfsDatastore(context, hyperHost, VmwareResource.getDatastoreName(iScsiName), storageHost, storagePort,
                        trimIqn(iScsiName), chapInitiatorUsername, chapInitiatorSecret, chapTargetUsername, chapTargetSecret);
         }
    }

    private ManagedObjectReference getVmfsDatastore(VmwareContext context, VmwareHypervisorHost hyperHost, String datastoreName, String storageIpAddress, int storagePortNumber,
            String iqn, String chapName, String chapSecret, String mutualChapName, String mutualChapSecret) throws Exception {
        ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
        ClusterMO cluster = new ClusterMO(context, morCluster);
        List<Pair<ManagedObjectReference, String>> lstHosts = cluster.getClusterHosts();

        HostInternetScsiHbaStaticTarget target = new HostInternetScsiHbaStaticTarget();

        target.setAddress(storageIpAddress);
        target.setPort(storagePortNumber);
        target.setIScsiName(iqn);

        if (StringUtils.isNotBlank(chapName) && StringUtils.isNotBlank(chapSecret)) {
            HostInternetScsiHbaAuthenticationProperties auth = new HostInternetScsiHbaAuthenticationProperties();

            String strAuthType = "chapRequired";

            auth.setChapAuthEnabled(true);
            auth.setChapInherited(false);
            auth.setChapAuthenticationType(strAuthType);
            auth.setChapName(chapName);
            auth.setChapSecret(chapSecret);

            if (StringUtils.isNotBlank(mutualChapName) && StringUtils.isNotBlank(mutualChapSecret)) {
                auth.setMutualChapInherited(false);
                auth.setMutualChapAuthenticationType(strAuthType);
                auth.setMutualChapName(mutualChapName);
                auth.setMutualChapSecret(mutualChapSecret);
            }

            target.setAuthenticationProperties(auth);
        }

        final List<HostInternetScsiHbaStaticTarget> lstTargets = new ArrayList<HostInternetScsiHbaStaticTarget>();

        lstTargets.add(target);

        addRemoveInternetScsiTargetsToAllHosts(context, true, lstTargets, lstHosts);

        rescanAllHosts(context, lstHosts);

        HostMO host = new HostMO(context, lstHosts.get(0).first());
        HostDatastoreSystemMO hostDatastoreSystem = host.getHostDatastoreSystemMO();

        ManagedObjectReference morDs = hostDatastoreSystem.findDatastoreByName(datastoreName);

        if (morDs != null) {
            return morDs;
        }

        rescanAllHosts(context, lstHosts);

        HostStorageSystemMO hostStorageSystem = host.getHostStorageSystemMO();
        List<HostScsiDisk> lstHostScsiDisks = hostDatastoreSystem.queryAvailableDisksForVmfs();

        HostScsiDisk hostScsiDisk = getHostScsiDisk(hostStorageSystem.getStorageDeviceInfo().getScsiTopology(), lstHostScsiDisks, iqn);

        if (hostScsiDisk == null) {
            // check to see if the datastore actually does exist already
            morDs = hostDatastoreSystem.findDatastoreByName(datastoreName);

            if (morDs != null) {
                return morDs;
            }

            throw new Exception("A relevant SCSI disk could not be located to use to create a datastore.");
        }

        morDs = hostDatastoreSystem.createVmfsDatastore(datastoreName, hostScsiDisk);

        if (morDs != null) {
            rescanAllHosts(context, lstHosts);

            return morDs;
        }

        throw new Exception("Unable to create a datastore");
    }

    // the purpose of this method is to find the HostScsiDisk in the passed-in array that exists (if any) because
    // we added the static iqn to an iSCSI HBA
    private static HostScsiDisk getHostScsiDisk(HostScsiTopology hst, List<HostScsiDisk> lstHostScsiDisks, String iqn) {
        for (HostScsiTopologyInterface adapter : hst.getAdapter()) {
            if (adapter.getTarget() != null) {
                for (HostScsiTopologyTarget target : adapter.getTarget()) {
                    if (target.getTransport() instanceof HostInternetScsiTargetTransport) {
                        String iScsiName = ((HostInternetScsiTargetTransport)target.getTransport()).getIScsiName();

                        if (iqn.equals(iScsiName)) {
                            for (HostScsiDisk hostScsiDisk : lstHostScsiDisks) {
                                for (HostScsiTopologyLun hstl : target.getLun()) {
                                    if (hstl.getScsiLun().contains(hostScsiDisk.getUuid())) {
                                        return hostScsiDisk;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private void removeVmfsDatastore(VmwareHypervisorHost hyperHost, String datastoreName, String storageIpAddress, int storagePortNumber, String iqn) throws Exception {
        // hyperHost.unmountDatastore(datastoreName);

        VmwareContext context = hostService.getServiceContext(null);
        ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
        ClusterMO cluster = new ClusterMO(context, morCluster);
        List<Pair<ManagedObjectReference, String>> lstHosts = cluster.getClusterHosts();

        HostInternetScsiHbaStaticTarget target = new HostInternetScsiHbaStaticTarget();

        target.setAddress(storageIpAddress);
        target.setPort(storagePortNumber);
        target.setIScsiName(iqn);

        final List<HostInternetScsiHbaStaticTarget> lstTargets = new ArrayList<HostInternetScsiHbaStaticTarget>();

        lstTargets.add(target);

        addRemoveInternetScsiTargetsToAllHosts(context, false, lstTargets, lstHosts);

        rescanAllHosts(context, lstHosts);
    }

    private void createVmdk(Command cmd, DatastoreMO dsMo, String vmdkDatastorePath, Long volumeSize) throws Exception {
        VmwareContext context = hostService.getServiceContext(null);
        VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);

        String dummyVmName = hostService.getWorkerName(context, cmd, 0);

        VirtualMachineMO vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, dummyVmName);

        if (vmMo == null) {
            throw new Exception("Unable to create a dummy VM for volume creation");
        }

        Long volumeSizeToUse = volumeSize < dsMo.getSummary().getFreeSpace() ? volumeSize : dsMo.getSummary().getFreeSpace();

        vmMo.createDisk(vmdkDatastorePath, getMBsFromBytes(volumeSizeToUse), dsMo.getMor(), vmMo.getScsiDeviceControllerKey());
        vmMo.detachDisk(vmdkDatastorePath, false);
        vmMo.destroy();
    }

    private static int getMBsFromBytes(long bytes) {
        return (int)(bytes / (1024L * 1024L));
    }

    private void addRemoveInternetScsiTargetsToAllHosts(VmwareContext context, final boolean add, final List<HostInternetScsiHbaStaticTarget> lstTargets,
            List<Pair<ManagedObjectReference, String>> lstHosts) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(lstHosts.size());

        final List<Exception> exceptions = new ArrayList<Exception>();

        for (Pair<ManagedObjectReference, String> hostPair : lstHosts) {
            HostMO host = new HostMO(context, hostPair.first());
            HostStorageSystemMO hostStorageSystem = host.getHostStorageSystemMO();

            boolean iScsiHbaConfigured = false;

            for (HostHostBusAdapter hba : hostStorageSystem.getStorageDeviceInfo().getHostBusAdapter()) {
                if (hba instanceof HostInternetScsiHba) {
                    // just finding an instance of HostInternetScsiHba means that we have found at least one configured iSCSI HBA
                    // at least one iSCSI HBA must be configured before a CloudStack user can use this host for iSCSI storage
                    iScsiHbaConfigured = true;

                    final String iScsiHbaDevice = hba.getDevice();

                    final HostStorageSystemMO hss = hostStorageSystem;

                    executorService.submit(new Thread() {
                        @Override
                        public void run() {
                            try {
                                if (add) {
                                    hss.addInternetScsiStaticTargets(iScsiHbaDevice, lstTargets);
                                } else {
                                    hss.removeInternetScsiStaticTargets(iScsiHbaDevice, lstTargets);
                                }

                                hss.rescanHba(iScsiHbaDevice);
                                hss.rescanVmfs();
                            } catch (Exception ex) {
                                synchronized (exceptions) {
                                    exceptions.add(ex);
                                }
                            }
                        }
                    });
                }
            }

            if (!iScsiHbaConfigured) {
                throw new Exception("An iSCSI HBA must be configured before a host can use iSCSI storage.");
            }
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
            throw new Exception("The system timed out before completing the task 'rescanAllHosts'.");
        }

        if (exceptions.size() > 0) {
            throw new Exception(exceptions.get(0).getMessage());
        }
    }

    private void rescanAllHosts(VmwareContext context, List<Pair<ManagedObjectReference, String>> lstHosts) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(lstHosts.size());

        final List<Exception> exceptions = new ArrayList<Exception>();

        for (Pair<ManagedObjectReference, String> hostPair : lstHosts) {
            HostMO host = new HostMO(context, hostPair.first());
            HostStorageSystemMO hostStorageSystem = host.getHostStorageSystemMO();

            boolean iScsiHbaConfigured = false;

            for (HostHostBusAdapter hba : hostStorageSystem.getStorageDeviceInfo().getHostBusAdapter()) {
                if (hba instanceof HostInternetScsiHba) {
                    // just finding an instance of HostInternetScsiHba means that we have found at least one configured iSCSI HBA
                    // at least one iSCSI HBA must be configured before a CloudStack user can use this host for iSCSI storage
                    iScsiHbaConfigured = true;

                    final String iScsiHbaDevice = hba.getDevice();

                    final HostStorageSystemMO hss = hostStorageSystem;

                    executorService.submit(new Thread() {
                        @Override
                        public void run() {
                            try {
                                hss.rescanHba(iScsiHbaDevice);
                                hss.rescanVmfs();
                            } catch (Exception ex) {
                                synchronized (exceptions) {
                                    exceptions.add(ex);
                                }
                            }
                        }
                    });
                }
            }

            if (!iScsiHbaConfigured) {
                throw new Exception("An iSCSI HBA must be configured before a host can use iSCSI storage.");
            }
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
            throw new Exception("The system timed out before completing the task 'rescanAllHosts'.");
        }

        if (exceptions.size() > 0) {
            throw new Exception(exceptions.get(0).getMessage());
        }
    }

    private static String trimIqn(String iqn) {
        String[] tmp = iqn.split("/");

        if (tmp.length != 3) {
            String msg = "Wrong format for iScsi path: " + iqn + ". It should be formatted as '/targetIQN/LUN'.";

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }

        return tmp[1].trim();
    }

    public ManagedObjectReference prepareManagedStorage(VmwareContext context, VmwareHypervisorHost hyperHost, String diskUuid, String iScsiName,
            String storageHost, int storagePort, String volumeName, String chapInitiatorUsername, String chapInitiatorSecret,
            String chapTargetUsername, String chapTargetSecret, long size, Command cmd) throws Exception {

        ManagedObjectReference morDs = prepareManagedDatastore(context, hyperHost, diskUuid, iScsiName, storageHost, storagePort,
                chapInitiatorUsername, chapInitiatorSecret, chapTargetUsername, chapTargetSecret);

        DatastoreMO dsMo = new DatastoreMO(hostService.getServiceContext(null), morDs);

        String volumeDatastorePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumeName != null ? volumeName : dsMo.getName());

        if (!dsMo.fileExists(volumeDatastorePath)) {
            createVmdk(cmd, dsMo, volumeDatastorePath, size);
        }

        return morDs;
    }

    public void handleDatastoreAndVmdkDetach(String datastoreName, String iqn, String storageHost, int storagePort) throws Exception {
        VmwareContext context = hostService.getServiceContext(null);
        VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);

        removeVmfsDatastore(hyperHost, datastoreName, storageHost, storagePort, trimIqn(iqn));
    }

    private void handleDatastoreAndVmdkDetachManaged(String diskUuid, String iqn, String storageHost, int storagePort) throws Exception {
        if (storagePort == DEFAULT_NFS_PORT) {
            VmwareContext context = hostService.getServiceContext(null);
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
            // for managed NFS datastore
            hyperHost.unmountDatastore(diskUuid);
        } else {
            handleDatastoreAndVmdkDetach(VmwareResource.getDatastoreName(iqn), iqn, storageHost, storagePort);
        }
    }

    private void removeManagedTargetsFromCluster(List<String> iqns) throws Exception {
        List<HostInternetScsiHbaStaticTarget> lstManagedTargets = new ArrayList<HostInternetScsiHbaStaticTarget>();

        VmwareContext context = hostService.getServiceContext(null);
        VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, null);
        ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
        ClusterMO cluster = new ClusterMO(context, morCluster);
        List<Pair<ManagedObjectReference, String>> lstHosts = cluster.getClusterHosts();
        HostMO host = new HostMO(context, lstHosts.get(0).first());
        HostStorageSystemMO hostStorageSystem = host.getHostStorageSystemMO();

        for (HostHostBusAdapter hba : hostStorageSystem.getStorageDeviceInfo().getHostBusAdapter()) {
            if (hba instanceof HostInternetScsiHba) {
                List<HostInternetScsiHbaStaticTarget> lstTargets = ((HostInternetScsiHba)hba).getConfiguredStaticTarget();

                if (lstTargets != null) {
                    for (HostInternetScsiHbaStaticTarget target : lstTargets) {
                        if (iqns.contains(target.getIScsiName())) {
                            lstManagedTargets.add(target);
                        }
                    }
                }
            }
        }

        addRemoveInternetScsiTargetsToAllHosts(context, false, lstManagedTargets, lstHosts);

        rescanAllHosts(context, lstHosts);
    }

    private List<String> getManagedIqnsFromVirtualDisks(List<VirtualDisk> virtualDisks) {
        List<String> managedIqns = new ArrayList<String>();

        if (virtualDisks != null) {
            for (VirtualDisk virtualDisk : virtualDisks) {
                if (virtualDisk.getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
                    VirtualDiskFlatVer2BackingInfo backingInfo = (VirtualDiskFlatVer2BackingInfo)virtualDisk.getBacking();
                    String path = backingInfo.getFileName();

                    String search = "[-";
                    int index = path.indexOf(search);

                    if (index > -1) {
                        path = path.substring(index + search.length());

                        String search2 = "-0]";

                        index = path.lastIndexOf(search2);

                        if (index > -1) {
                            path = path.substring(0, index);

                            if (path.startsWith("iqn.")) {
                                managedIqns.add(path);
                            }
                        }
                    }
                }
            }
        }

        return managedIqns;
    }

    private Long restoreVolumeFromSecStorage(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName, String secStorageUrl, String secStorageDir,
            String backupName, long wait) throws Exception {

        String secondaryMountPoint = mountService.getMountPoint(secStorageUrl);
        String srcOVAFileName = null;
        String srcOVFFileName = null;

        srcOVAFileName = secondaryMountPoint + "/" + secStorageDir + "/" + backupName + "." + ImageFormat.OVA.getFileExtension();
        srcOVFFileName = secondaryMountPoint + "/" + secStorageDir + "/" + backupName + ".ovf";

        String snapshotDir = "";
        if (backupName.contains("/")) {
            snapshotDir = backupName.split("/")[0];
        }

        File ovafile = new File(srcOVAFileName);

        File ovfFile = new File(srcOVFFileName);
        // String srcFileName = getOVFFilePath(srcOVAFileName);
        if (!ovfFile.exists()) {
            srcOVFFileName = getOVFFilePath(srcOVAFileName);
            if (srcOVFFileName == null && ovafile.exists()) {  // volss: ova file exists; o/w can't do tar
                Script command = new Script("tar", wait, s_logger);
                command.add("--no-same-owner");
                command.add("-xf", srcOVAFileName);
                command.setWorkDir(secondaryMountPoint + "/" + secStorageDir + "/" + snapshotDir);
                s_logger.info("Executing command: " + command.toString());
                String result = command.execute();
                if (result != null) {
                    String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
                srcOVFFileName = getOVFFilePath(srcOVAFileName);
            } else if (srcOVFFileName == null) {
                String msg = "Unable to find snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        if (srcOVFFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        VirtualMachineMO clonedVm = null;
        try {
            hyperHost.importVmFromOVF(srcOVFFileName, newVolumeName, primaryDsMo, "thin");
            clonedVm = hyperHost.findVmOnHyperHost(newVolumeName);
            if (clonedVm == null) {
                throw new Exception("Unable to create container VM for volume creation");
            }

            clonedVm.moveAllVmDiskFiles(primaryDsMo, "", false);
            clonedVm.detachAllDisks();
            return _storage.getSize(srcOVFFileName);
        } finally {
            if (clonedVm != null) {
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
        DataStoreTO pool = destData.getDataStore();
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
        String newVolumeName = VmwareHelper.getVCenterSafeUuid();

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            ManagedObjectReference morPrimaryDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStorageNameLabel);
            if (morPrimaryDs == null) {
                String msg = "Unable to find datastore: " + primaryStorageNameLabel;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            // strip off the extension since restoreVolumeFromSecStorage internally will append suffix there.
            if (backedUpSnapshotUuid.endsWith(".ova")){
                backedUpSnapshotUuid = backedUpSnapshotUuid.replace(".ova", "");
            } else if (backedUpSnapshotUuid.endsWith(".ovf")){
                backedUpSnapshotUuid = backedUpSnapshotUuid.replace(".ovf", "");
            }
            DatastoreMO primaryDsMo = new DatastoreMO(hyperHost.getContext(), morPrimaryDs);
            restoreVolumeFromSecStorage(hyperHost, primaryDsMo, newVolumeName, secondaryStorageUrl, backupPath, backedUpSnapshotUuid, (long)cmd.getWait() * 1000);

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

    @Override
    public Answer introduceObject(IntroduceObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

    @Override
    public Answer forgetObject(ForgetObjectCmd cmd) {
        return new Answer(cmd, false, "not implememented yet");
    }

    private static String deriveTemplateUuidOnHost(VmwareHypervisorHost hyperHost, String storeIdentifier, String templateName) {
        String templateUuid;
        try{
            templateUuid = UUID.nameUUIDFromBytes((templateName + "@" + storeIdentifier + "-" + hyperHost.getMor().getValue()).getBytes("UTF-8")).toString();
        }catch(UnsupportedEncodingException e){
            s_logger.warn("unexpected encoding error, using default Charset: " + e.getLocalizedMessage());
            templateUuid = UUID.nameUUIDFromBytes((templateName + "@" + storeIdentifier + "-" + hyperHost.getMor().getValue()).getBytes(Charset.defaultCharset()))
                    .toString();
        }
        templateUuid = templateUuid.replaceAll("-", "");
        return templateUuid;
    }
}
