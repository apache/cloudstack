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
package com.cloud.hypervisor.vmware.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.vmware.vim25.FileInfo;
import com.vmware.vim25.FileQueryFlags;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualDisk;

import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostDatastoreBrowserMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Volume;
import com.cloud.storage.template.OVAProcessor;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

public class VmwareStorageManagerImpl implements VmwareStorageManager {

    private Integer _nfsVersion;

    @Override
    public boolean execute(VmwareHostService hostService, CreateEntityDownloadURLCommand cmd) {
        DataTO data = cmd.getData();
        int timeout = NumbersUtil.parseInt(cmd.getContextParam(VmwareManager.s_vmwareOVAPackageTimeout.key()),
                    Integer.valueOf(VmwareManager.s_vmwareOVAPackageTimeout.defaultValue()) * VmwareManager.s_vmwareOVAPackageTimeout.multiplier());
        if (data == null) {
            return false;
        }

        String newPath = null;
        if (data.getObjectType() == DataObjectType.VOLUME) {
            newPath = createOvaForVolume((VolumeObjectTO)data, timeout);
        } else if (data.getObjectType() == DataObjectType.TEMPLATE) {
            newPath = createOvaForTemplate((TemplateObjectTO)data, timeout);
        } else if (data.getObjectType() == DataObjectType.ARCHIVE) {
            newPath = cmd.getInstallPath();
        }
        if (newPath != null) {
            cmd.setInstallPath(newPath);
            return true;
        }
        return false;

    }

    @Override
    public void createOva(String path, String name, int archiveTimeout) {
        Script commandSync = new Script(true, "sync", 0, s_logger);
        commandSync.execute();

        Script command = new Script(false, "tar", archiveTimeout, s_logger);
        command.setWorkDir(path);
        command.add("-cf", name + ".ova");
        command.add(name + ".ovf");        // OVF file should be the first file in OVA archive
        command.add(name + "-disk0.vmdk");

        s_logger.info("Package OVA with commmand: " + command.toString());
        command.execute();
    }

    private static final Logger s_logger = Logger.getLogger(VmwareStorageManagerImpl.class);

    private final VmwareStorageMount _mountService;
    private final StorageLayer _storage = new JavaStorageLayer();

    private int _timeout;

    public VmwareStorageManagerImpl(VmwareStorageMount mountService) {
        assert (mountService != null);
        _mountService = mountService;
    }

    public VmwareStorageManagerImpl(VmwareStorageMount mountService, Integer nfsVersion) {
        assert (mountService != null);
        _mountService = mountService;
        _nfsVersion = nfsVersion;
    }

    public void configure(Map<String, Object> params) {
        s_logger.info("Configure VmwareStorageManagerImpl");

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;
    }

    @Override
    public String createOvaForTemplate(TemplateObjectTO template, int archiveTimeout) {
        DataStoreTO storeTO = template.getDataStore();
        if (!(storeTO instanceof NfsTO)) {
            s_logger.debug("Can only handle NFS storage, while creating OVA from template");
            return null;
        }
        NfsTO nfsStore = (NfsTO)storeTO;
        String secStorageUrl = nfsStore.getUrl();
        assert (secStorageUrl != null);
        String installPath = template.getPath();
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsStore.getNfsVersion());
        String installFullPath = secondaryMountPoint + "/" + installPath;
        try {
            if (installFullPath.endsWith(".ova")) {
                if (new File(installFullPath).exists()) {
                    s_logger.debug("OVA file found at: " + installFullPath);
                } else {
                    if (new File(installFullPath + ".meta").exists()) {
                        createOVAFromMetafile(installFullPath + ".meta", archiveTimeout);
                    } else {
                        String msg = "Unable to find OVA or OVA MetaFile to prepare template.";
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }
                }
                return installPath;
            }
        } catch (Throwable e) {
            s_logger.debug("Failed to create OVA: " + e.toString());
        }
        return null;
    }

    //Fang: new command added;
    // Important! we need to sync file system before we can safely use tar to work around a linux kernal bug(or feature)
    public String createOvaForVolume(VolumeObjectTO volume, int archiveTimeout) {
        DataStoreTO storeTO = volume.getDataStore();
        if (!(storeTO instanceof NfsTO)) {
            s_logger.debug("can only handle nfs storage, when create ova from volume");
            return null;
        }
        NfsTO nfsStore = (NfsTO)storeTO;
        String secStorageUrl = nfsStore.getUrl();
        assert (secStorageUrl != null);
        //Note the volume path is volumes/accountId/volumeId/uuid/, the actual volume is uuid/uuid.vmdk
        String installPath = volume.getPath();
        int index = installPath.lastIndexOf(File.separator);
        String volumeUuid = installPath.substring(index + 1);
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsStore.getNfsVersion());
        //The real volume path
        String volumePath = installPath + File.separator + volumeUuid + ".ova";
        String installFullPath = secondaryMountPoint + "/" + installPath;

        try {
            if (new File(secondaryMountPoint + File.separator + volumePath).exists()) {
                s_logger.debug("ova already exists:" + volumePath);
                return volumePath;
            } else {
                Script commandSync = new Script(true, "sync", 0, s_logger);
                commandSync.execute();
                Script command = new Script(false, "tar", archiveTimeout, s_logger);
                command.setWorkDir(installFullPath);
                command.add("-cf", volumeUuid + ".ova");
                command.add(volumeUuid + ".ovf");        // OVF file should be the first file in OVA archive
                command.add(volumeUuid + "-disk0.vmdk");

                String result = command.execute();
                if (result != Script.ERR_TIMEOUT) {
                    return volumePath;
                }

            }
        } catch (Throwable e) {
            s_logger.info("Exception for createVolumeOVA");
        }
        return null;
    }

    @Override
    public Answer execute(VmwareHostService hostService, PrimaryStorageDownloadCommand cmd) {
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        assert (secondaryStorageUrl != null);

        String templateUrl = cmd.getUrl();

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
                templateName = cmd.getName();
            }
        } else {
            mountPoint = templateUrl.substring(secondaryStorageUrl.length() + 1);
            if (!mountPoint.endsWith("/")) {
                mountPoint = mountPoint + "/";
            }
            templateName = cmd.getName();
        }

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            String templateUuidName = UUID.nameUUIDFromBytes((templateName + "@" + cmd.getPoolUuid() + "-" + hyperHost.getMor().getValue()).getBytes("UTF-8")).toString();
            // truncate template name to 32 chars to ensure they work well with vSphere API's.
            templateUuidName = templateUuidName.replace("-", "");

            DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
            VirtualMachineMO templateMo = VmwareHelper.pickOneVmOnRunningHost(dcMo.findVmByNameAndLabel(templateUuidName), true);

            if (templateMo == null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Template " + templateName + " is not setup yet, setup template from secondary storage with uuid name: " + templateUuidName);
                }
                ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, cmd.getPoolUuid());
                assert (morDs != null);
                DatastoreMO primaryStorageDatastoreMo = new DatastoreMO(context, morDs);

                copyTemplateFromSecondaryToPrimary(hyperHost, primaryStorageDatastoreMo, secondaryStorageUrl, mountPoint, templateName, templateUuidName, cmd.getNfsVersion());
            } else {
                s_logger.info("Template " + templateName + " has already been setup, skip the template setup process in primary storage");
            }

            return new PrimaryStorageDownloadAnswer(templateUuidName, 0);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            String msg = "Unable to execute PrimaryStorageDownloadCommand due to exception";
            s_logger.error(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }
    }

    @Override
    @Deprecated
    public Answer execute(VmwareHostService hostService, BackupSnapshotCommand cmd) {
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String snapshotUuid = cmd.getSnapshotUuid(); // not null: Precondition.
        String prevSnapshotUuid = cmd.getPrevSnapshotUuid();
        String prevBackupUuid = cmd.getPrevBackupUuid();
        String searchExcludedFolders = cmd.getContextParam("searchexludefolders");
        VirtualMachineMO workerVm = null;
        String workerVMName = null;
        String volumePath = cmd.getVolumePath();
        ManagedObjectReference morDs = null;
        DatastoreMO dsMo = null;

        // By default assume failure
        String details = null;
        boolean success = false;
        String snapshotBackupUuid = null;

        VmwareContext context = hostService.getServiceContext(cmd);
        VirtualMachineMO vmMo = null;
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, cmd.getPool().getUuid());

            try {
                vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
                if (vmMo == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find owner VM for BackupSnapshotCommand on host " + hyperHost.getHyperHostName() + ", will try within datacenter");
                    }

                    vmMo = hyperHost.findVmOnPeerHyperHost(cmd.getVmName());
                    if (vmMo == null) {
                        dsMo = new DatastoreMO(hyperHost.getContext(), morDs);

                        workerVMName = hostService.getWorkerName(context, cmd, 0);
                        vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, workerVMName);

                        if (vmMo == null) {
                            throw new Exception("Failed to find the newly create or relocated VM. vmName: " + workerVMName);
                        }
                        workerVm = vmMo;

                        // attach volume to worker VM
                        String datastoreVolumePath = getVolumePathInDatastore(dsMo, volumePath + ".vmdk", searchExcludedFolders);
                        vmMo.attachDisk(new String[] {datastoreVolumePath}, morDs);
                    }
                }

                if (!vmMo.createSnapshot(snapshotUuid, "Snapshot taken for " + cmd.getSnapshotName(), false, false)) {
                    throw new Exception("Failed to take snapshot " + cmd.getSnapshotName() + " on vm: " + cmd.getVmName());
                }

                snapshotBackupUuid = backupSnapshotToSecondaryStorage(vmMo, accountId, volumeId, cmd.getVolumePath(), snapshotUuid, secondaryStorageUrl, prevSnapshotUuid,
                        prevBackupUuid, hostService.getWorkerName(context, cmd, 1), cmd.getNfsVersion());

                success = (snapshotBackupUuid != null);
                if (success) {
                    details = "Successfully backedUp the snapshotUuid: " + snapshotUuid + " to secondary storage.";
                }

            } finally {
                if (vmMo != null) {
                    ManagedObjectReference snapshotMor = vmMo.getSnapshotMor(snapshotUuid);
                    if (snapshotMor != null) {
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

            details = "BackupSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
            return new BackupSnapshotAnswer(cmd, false, details, snapshotBackupUuid, true);
        }

        return new BackupSnapshotAnswer(cmd, success, details, snapshotBackupUuid, true);
    }

    @Override
    public Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStoragePoolURL = cmd.getSecondaryStorageUrl();
        String volumePath = cmd.getVolumePath();
        Long accountId = cmd.getAccountId();
        Long templateId = cmd.getTemplateId();
        String details = null;

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
            if (vmMo == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find the owner VM for CreatePrivateTemplateFromVolumeCommand on host " + hyperHost.getHyperHostName() + ", try within datacenter");
                }
                vmMo = hyperHost.findVmOnPeerHyperHost(cmd.getVmName());

                if (vmMo == null) {
                    String msg = "Unable to find the owner VM for volume operation. vm: " + cmd.getVmName();
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            Ternary<String, Long, Long> result = createTemplateFromVolume(vmMo, accountId, templateId, cmd.getUniqueName(), secondaryStoragePoolURL, volumePath,
                    hostService.getWorkerName(context, cmd, 0), cmd.getNfsVersion());

            return new CreatePrivateTemplateAnswer(cmd, true, null, result.first(), result.third(), result.second(), cmd.getUniqueName(), ImageFormat.OVA);

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);

            details = "CreatePrivateTemplateFromVolumeCommand exception: " + StringUtils.getExceptionStackInfo(e);
            return new CreatePrivateTemplateAnswer(cmd, false, details);
        }
    }

    @Override
    public Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromSnapshotCommand cmd) {
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        Long newTemplateId = cmd.getNewTemplateId();
        String details;
        String uniqeName = UUID.randomUUID().toString();

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            Ternary<String, Long, Long> result = createTemplateFromSnapshot(accountId, newTemplateId, uniqeName, secondaryStorageUrl, volumeId, backedUpSnapshotUuid,
                    cmd.getNfsVersion());

            return new CreatePrivateTemplateAnswer(cmd, true, null, result.first(), result.third(), result.second(), uniqeName, ImageFormat.OVA);
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);

            details = "CreatePrivateTemplateFromSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
            return new CreatePrivateTemplateAnswer(cmd, false, details);
        }
    }

    @Override
    public Answer execute(VmwareHostService hostService, CopyVolumeCommand cmd) {
        Long volumeId = cmd.getVolumeId();
        String volumePath = cmd.getVolumePath();
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        String vmName = cmd.getVmName();

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            Pair<String, String> result;
            if (cmd.toSecondaryStorage()) {
                result = copyVolumeToSecStorage(hostService, hyperHost, cmd, vmName, volumeId, cmd.getPool().getUuid(), volumePath, secondaryStorageURL,
                        hostService.getWorkerName(context, cmd, 0), cmd.getNfsVersion());
            } else {
                StorageFilerTO poolTO = cmd.getPool();

                ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolTO.getUuid());
                if (morDatastore == null) {
                    morDatastore = hyperHost.mountDatastore(false, poolTO.getHost(), 0, poolTO.getPath(), poolTO.getUuid().replace("-", ""));

                    if (morDatastore == null) {
                        throw new Exception("Unable to mount storage pool on host. storeUrl: " + poolTO.getHost() + ":/" + poolTO.getPath());
                    }
                }

                result = copyVolumeFromSecStorage(hyperHost, volumeId, new DatastoreMO(context, morDatastore), secondaryStorageURL, volumePath, cmd.getNfsVersion());
                deleteVolumeDirOnSecondaryStorage(volumeId, secondaryStorageURL, cmd.getNfsVersion());
            }
            return new CopyVolumeAnswer(cmd, true, null, result.first(), result.second());
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            String msg = "Unable to execute CopyVolumeCommand due to exception";
            s_logger.error(msg, e);
            return new CopyVolumeAnswer(cmd, false, "CopyVolumeCommand failed due to exception: " + StringUtils.getExceptionStackInfo(e), null, null);
        }
    }

    @Override
    public Answer execute(VmwareHostService hostService, CreateVolumeFromSnapshotCommand cmd) {

        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();

        String details = null;
        boolean success = false;
        String newVolumeName = UUID.randomUUID().toString().replaceAll("-", "");

        VmwareContext context = hostService.getServiceContext(cmd);
        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            ManagedObjectReference morPrimaryDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, primaryStorageNameLabel);
            if (morPrimaryDs == null) {
                String msg = "Unable to find datastore: " + primaryStorageNameLabel;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO primaryDsMo = new DatastoreMO(hyperHost.getContext(), morPrimaryDs);
            details = createVolumeFromSnapshot(hyperHost, primaryDsMo, newVolumeName, accountId, volumeId, secondaryStorageUrl, backedUpSnapshotUuid, cmd.getNfsVersion());
            if (details == null) {
                success = true;
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                hostService.invalidateServiceContext(context);
            }

            s_logger.error("Unexpecpted exception ", e);
            details = "CreateVolumeFromSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
        }

        return new CreateVolumeFromSnapshotAnswer(cmd, success, details, newVolumeName);
    }


    // templateName: name in secondary storage
    // templateUuid: will be used at hypervisor layer
    private void copyTemplateFromSecondaryToPrimary(VmwareHypervisorHost hyperHost, DatastoreMO datastoreMo, String secondaryStorageUrl, String templatePathAtSecondaryStorage,
            String templateName, String templateUuid, Integer nfsVersion) throws Exception {

        s_logger.info("Executing copyTemplateFromSecondaryToPrimary. secondaryStorage: " + secondaryStorageUrl + ", templatePathAtSecondaryStorage: "
                + templatePathAtSecondaryStorage + ", templateName: " + templateName);

        String secondaryMountPoint = _mountService.getMountPoint(secondaryStorageUrl, nfsVersion);
        s_logger.info("Secondary storage mount point: " + secondaryMountPoint);

        String srcOVAFileName = secondaryMountPoint + "/" + templatePathAtSecondaryStorage + templateName + "." + ImageFormat.OVA.getFileExtension();

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
            String msg = "Failed to import OVA template. secondaryStorage: " + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage
                    + ", templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if (vmMo.createSnapshot("cloud.template.base", "Base snapshot", false, false)) {
            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, templateUuid);
            vmMo.markAsTemplate();
        } else {
            vmMo.destroy();
            String msg = "Unable to create base snapshot for template, templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }
    }

    private Ternary<String, Long, Long> createTemplateFromVolume(VirtualMachineMO vmMo, long accountId, long templateId, String templateUniqueName, String secStorageUrl,
            String volumePath, String workerVmName, Integer nfsVersion) throws Exception {

        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsVersion);
        String installPath = getTemplateRelativeDirInSecStorage(accountId, templateId);
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
            vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
            if (clonedVm == null) {
                String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            clonedVm.exportVm(secondaryMountPoint + "/" + installPath, templateUniqueName, true, false);

            long physicalSize = new File(installFullPath + "/" + templateUniqueName + ".ova").length();
            OVAProcessor processor = new OVAProcessor();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("OVA Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, templateUniqueName);

            postCreatePrivateTemplate(installFullPath, templateId, templateUniqueName, physicalSize, virtualSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);

        } finally {
            if (clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }

            vmMo.removeSnapshot(templateUniqueName, false);
        }
    }

    private Ternary<String, Long, Long> createTemplateFromSnapshot(long accountId, long templateId, String templateUniqueName, String secStorageUrl, long volumeId,
            String backedUpSnapshotUuid, Integer nfsVersion) throws Exception {

        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsVersion);
        String installPath = getTemplateRelativeDirInSecStorage(accountId, templateId);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        String installFullOVAName = installFullPath + "/" + templateUniqueName + ".ova";  //Note: volss for tmpl
        String snapshotRoot = secondaryMountPoint + "/" + getSnapshotRelativeDirInSecStorage(accountId, volumeId);
        String snapshotFullOVAName = snapshotRoot + "/" + backedUpSnapshotUuid + ".ova";
        String snapshotFullOvfName = snapshotRoot + "/" + backedUpSnapshotUuid + ".ovf";
        String result;
        Script command;
        String templateVMDKName = "";
        //String snapshotFullVMDKName = snapshotRoot + "/";
        // the backedUpSnapshotUuid field currently has the format: uuid/uuid. so we need to extract the uuid out
        String backupSSUuid = backedUpSnapshotUuid.substring(0, backedUpSnapshotUuid.indexOf('/'));
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
                command = new Script(false, "cp", _timeout, s_logger);
                command.add(snapshotFullOVAName);
                command.add(installFullOVAName);
                result = command.execute();
                if (result != null) {
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
                if (result != null) {
                    String msg = "unable to untar snapshot " + snapshotFullOVAName + " to " + installFullPath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

            } else {  // there is no ova file, only ovf originally;
                if (new File(snapshotFullOvfName).exists()) {
                    command = new Script(false, "cp", _timeout, s_logger);
                    command.add(snapshotFullOvfName);
                    //command.add(installFullOvfName);
                    command.add(installFullPath);
                    result = command.execute();
                    if (result != null) {
                        String msg = "unable to copy snapshot " + snapshotFullOvfName + " to " + installFullPath;
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }

                    s_logger.info("vmdkfile parent dir: " + snapshotFullVMDKName);
                    File snapshotdir = new File(snapshotFullVMDKName);
                    // File snapshotdir = new File(snapshotRoot);
                    File[] ssfiles = snapshotdir.listFiles();
                    // List<String> filenames = new ArrayList<String>();
                    for (int i = 0; i < ssfiles.length; i++) {
                        String vmdkfile = ssfiles[i].getName();
                        s_logger.info("vmdk file name: " + vmdkfile);
                        if (vmdkfile.toLowerCase().startsWith(backupSSUuid) && vmdkfile.toLowerCase().endsWith(".vmdk")) {
                            snapshotFullVMDKName += vmdkfile;
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
            writeMetaOvaForTemplate(installFullPath, backedUpSnapshotUuid + ".ovf", templateVMDKName, templateUniqueName, physicalSize);
            return new Ternary<String, Long, Long>(installPath + "/" + templateUniqueName + ".ova", physicalSize, virtualSize);
        } catch (Exception e) {
            // TODO, clean up left over files
            throw e;
        }
    }

    private void postCreatePrivateTemplate(String installFullPath, long templateId, String templateName, long size, long virtualSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/template.properties"), "UTF-8"));
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
            //out.write("ova=true");
            out.write("ova=false");  //volss: the real ova file is not created
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

    private void writeMetaOvaForTemplate(String installFullPath, String ovfFilename, String vmdkFilename, String templateName, long diskSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/" + templateName + ".ova.meta"), "UTF-8"));
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

    private String createVolumeFromSnapshot(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName, long accountId, long volumeId, String secStorageUrl,
            String snapshotBackupUuid, Integer nfsVersion) throws Exception {

        restoreVolumeFromSecStorage(hyperHost, primaryDsMo, newVolumeName, secStorageUrl, getSnapshotRelativeDirInSecStorage(accountId, volumeId), snapshotBackupUuid, nfsVersion);
        return null;
    }

    private void restoreVolumeFromSecStorage(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName, String secStorageUrl, String secStorageDir,
            String backupName, Integer nfsVersion) throws Exception {

        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsVersion);
        String srcOVAFileName = secondaryMountPoint + "/" + secStorageDir + "/" + backupName + "." + ImageFormat.OVA.getFileExtension();
        String snapshotDir = "";
        if (backupName.contains("/")) {
            snapshotDir = backupName.split("/")[0];
        }

        File ovafile = new File(srcOVAFileName);
        String srcOVFFileName = secondaryMountPoint + "/" + secStorageDir + "/" + backupName + ".ovf";
        File ovfFile = new File(srcOVFFileName);
        // String srcFileName = getOVFFilePath(srcOVAFileName);
        if (!ovfFile.exists()) {
            srcOVFFileName = getOVFFilePath(srcOVAFileName);
            if (srcOVFFileName == null && ovafile.exists()) {  // volss: ova file exists; o/w can't do tar
                Script command = new Script("tar", 0, s_logger);
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
            } else {
                String msg = "Unable to find snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }

            srcOVFFileName = getOVFFilePath(srcOVAFileName);
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
        } finally {
            if (clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }

    private String backupSnapshotToSecondaryStorage(VirtualMachineMO vmMo, long accountId, long volumeId, String volumePath, String snapshotUuid, String secStorageUrl,
            String prevSnapshotUuid, String prevBackupUuid, String workerVmName, Integer nfsVersion) throws Exception {

        String backupUuid = UUID.randomUUID().toString();
        exportVolumeToSecondaryStorage(vmMo, volumePath, secStorageUrl, getSnapshotRelativeDirInSecStorage(accountId, volumeId), backupUuid, workerVmName, nfsVersion, true);
        return backupUuid + "/" + backupUuid;
    }

    private void exportVolumeToSecondaryStorage(VirtualMachineMO vmMo, String volumePath, String secStorageUrl, String secStorageDir, String exportName, String workerVmName,
                                                Integer nfsVersion, boolean clonedWorkerVMNeeded) throws Exception {

        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsVersion);
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

            if (clonedWorkerVMNeeded) {
                // 4 MB is the minimum requirement for VM memory in VMware
                vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
                clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
                if (clonedVm == null) {
                    String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
                clonedVm.exportVm(exportPath, exportName, false, false);  //Note: volss: not to create ova.
            } else {
                vmMo.exportVm(exportPath, exportName, false, false);
            }
        } finally {
            if (clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }

    private Pair<String, String> copyVolumeToSecStorage(VmwareHostService hostService, VmwareHypervisorHost hyperHost, CopyVolumeCommand cmd, String vmName, long volumeId,
            String poolId, String volumePath, String secStorageUrl, String workerVmName, Integer nfsVersion) throws Exception {

        String volumeFolder = String.valueOf(volumeId) + "/";
        VirtualMachineMO workerVm = null;
        VirtualMachineMO vmMo = null;
        String exportName = UUID.randomUUID().toString();
        String searchExcludedFolders = cmd.getContextParam("searchexludefolders");


        try {
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolId);

            if (morDs == null) {
                String msg = "Unable to find volumes's storage pool for copy volume operation";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            boolean clonedWorkerVMNeeded = true;
            vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                // create a dummy worker vm for attaching the volume
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
                workerVm = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, workerVmName);

                if (workerVm == null) {
                    String msg = "Unable to create worker VM to execute CopyVolumeCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                }

                //attach volume to worker VM
                String datastoreVolumePath = getVolumePathInDatastore(dsMo, volumePath + ".vmdk", searchExcludedFolders);
                workerVm.attachDisk(new String[] {datastoreVolumePath}, morDs);
                vmMo = workerVm;
                clonedWorkerVMNeeded = false;
            } else {
                vmMo.createSnapshot(exportName, "Temporary snapshot for copy-volume command", false, false);
            }

            exportVolumeToSecondaryStorage(vmMo, volumePath, secStorageUrl, "volumes/" + volumeFolder, exportName, hostService.getWorkerName(hyperHost.getContext(), cmd, 1),
                    nfsVersion, clonedWorkerVMNeeded);
            return new Pair<String, String>(volumeFolder, exportName);

        } finally {
            if (vmMo != null && vmMo.getSnapshotMor(exportName) != null) {
                vmMo.removeSnapshot(exportName, false);
            }
            if (workerVm != null) {
                //detach volume and destroy worker vm
                workerVm.detachAllDisks();
                workerVm.destroy();
            }
        }
    }

    private String getVolumePathInDatastore(DatastoreMO dsMo, String volumeFileName, String searchExcludeFolders) throws Exception {
        String datastoreVolumePath = dsMo.searchFileInSubFolders(volumeFileName, true, searchExcludeFolders);
        if (datastoreVolumePath == null) {
            throw new CloudRuntimeException("Unable to find file " + volumeFileName + " in datastore " + dsMo.getName());
        }
        return datastoreVolumePath;
    }

    private Pair<String, String> copyVolumeFromSecStorage(VmwareHypervisorHost hyperHost, long volumeId, DatastoreMO dsMo, String secStorageUrl, String exportName,
            Integer nfsVersion) throws Exception {

        String volumeFolder = String.valueOf(volumeId) + "/";
        String newVolume = UUID.randomUUID().toString().replaceAll("-", "");
        restoreVolumeFromSecStorage(hyperHost, dsMo, newVolume, secStorageUrl, "volumes/" + volumeFolder, exportName, nfsVersion);

        return new Pair<String, String>(volumeFolder, newVolume);
    }

    // here we use a method to return the ovf and vmdk file names; Another way to do it:
    // create a new class, and like TemplateLocation.java and create templateOvfInfo.java to handle it;
    private String createOVAFromMetafile(String metafileName, int archiveTimeout) throws Exception {
        File ova_metafile = new File(metafileName);
        Properties props = null;
        String ovaFileName = "";
        s_logger.info("Creating OVA using MetaFile: " + metafileName);
        try (FileInputStream strm = new FileInputStream(ova_metafile);) {

            s_logger.info("loading properties from ova meta file: " + metafileName);
            props = new Properties();
            props.load(strm);
            ovaFileName = props.getProperty("ova.filename");
            s_logger.info("ovafilename: " + ovaFileName);
            String ovfFileName = props.getProperty("ovf");
            s_logger.info("ovffilename: " + ovfFileName);
            int diskNum = Integer.parseInt(props.getProperty("numDisks"));
            if (diskNum <= 0) {
                String msg = "VMDK disk file number is 0. Error";
                s_logger.error(msg);
                throw new Exception(msg);
            }
            String[] disks = new String[diskNum];
            for (int i = 0; i < diskNum; i++) {
                // String diskNameKey = "disk" + Integer.toString(i+1) + ".name"; // Fang use this
                String diskNameKey = "disk1.name";
                disks[i] = props.getProperty(diskNameKey);
                s_logger.info("diskname " + disks[i]);
            }
            String exportDir = ova_metafile.getParent();
            s_logger.info("exportDir: " + exportDir);
            // Important! we need to sync file system before we can safely use tar to work around a linux kernal bug(or feature)
            s_logger.info("Sync file system before we package OVA..., before tar ");
            s_logger.info("ova: " + ovaFileName + ", ovf:" + ovfFileName + ", vmdk:" + disks[0] + ".");
            Script commandSync = new Script(true, "sync", 0, s_logger);
            commandSync.execute();
            Script command = new Script(false, "tar", archiveTimeout, s_logger);
            command.setWorkDir(exportDir); // Fang: pass this in to the method?
            command.add("-cf", ovaFileName);
            command.add(ovfFileName); // OVF file should be the first file in OVA archive
            for (String diskName : disks) {
                command.add(diskName);
            }
            command.execute();
            s_logger.info("Package OVA for template in dir: " + exportDir + "cmd: " + command.toString());
            // to be safe, physically test existence of the target OVA file
            if ((new File(exportDir + File.separator + ovaFileName)).exists()) {
                s_logger.info("OVA file: " + ovaFileName + " is created and ready to extract.");
                return ovaFileName;
            } else {
                String msg = exportDir + File.separator + ovaFileName + " is not created as expected";
                s_logger.error(msg);
                throw new Exception(msg);
            }
        } catch (Exception e) {
            s_logger.error("Exception while creating OVA using Metafile", e);
            throw e;
        }

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

    private static String getTemplateRelativeDirInSecStorage(long accountId, long templateId) {
        return "template/tmpl/" + accountId + "/" + templateId;
    }

    private static String getSnapshotRelativeDirInSecStorage(long accountId, long volumeId) {
        return "snapshots/" + accountId + "/" + volumeId;
    }

    private long getVMSnapshotChainSize(VmwareContext context, VmwareHypervisorHost hyperHost, String fileName, ManagedObjectReference morDs, String exceptFileName)
            throws Exception {
        long size = 0;
        DatastoreMO dsMo = new DatastoreMO(context, morDs);
        HostDatastoreBrowserMO browserMo = dsMo.getHostDatastoreBrowserMO();
        String datastorePath = "[" + dsMo.getName() + "]";
        HostDatastoreBrowserSearchSpec searchSpec = new HostDatastoreBrowserSearchSpec();
        FileQueryFlags fqf = new FileQueryFlags();
        fqf.setFileSize(true);
        fqf.setFileOwner(true);
        fqf.setModification(true);
        searchSpec.setDetails(fqf);
        searchSpec.setSearchCaseInsensitive(false);
        searchSpec.getMatchPattern().add(fileName);
        ArrayList<HostDatastoreBrowserSearchResults> results = browserMo.searchDatastoreSubFolders(datastorePath, searchSpec);
        for (HostDatastoreBrowserSearchResults result : results) {
            if (result != null) {
                List<FileInfo> info = result.getFile();
                for (FileInfo fi : info) {
                    if (exceptFileName != null && fi.getPath().contains(exceptFileName)) {
                        continue;
                    } else {
                        size = size + fi.getFileSize();
                    }
                }
            }
        }
        return size;
    }

    @Override
    public CreateVMSnapshotAnswer execute(VmwareHostService hostService, CreateVMSnapshotCommand cmd) {
        List<VolumeObjectTO> volumeTOs = cmd.getVolumeTOs();
        String vmName = cmd.getVmName();
        String vmSnapshotName = cmd.getTarget().getSnapshotName();
        String vmSnapshotDesc = cmd.getTarget().getDescription();
        boolean snapshotMemory = cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory;
        boolean quiescevm = cmd.getTarget().getQuiescevm();
        VirtualMachineMO vmMo = null;
        VmwareContext context = hostService.getServiceContext(cmd);

        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            // wait if there are already VM snapshot task running
            ManagedObjectReference taskmgr = context.getServiceContent().getTaskManager();
            List<ManagedObjectReference> tasks = context.getVimClient().getDynamicProperty(taskmgr, "recentTask");

            for (ManagedObjectReference taskMor : tasks) {
                TaskInfo info = (TaskInfo)(context.getVimClient().getDynamicProperty(taskMor, "info"));

                if (info.getEntityName().equals(cmd.getVmName()) && StringUtils.isNotBlank(info.getName()) && info.getName().equalsIgnoreCase("CreateSnapshot_Task")) {
                    if (!(info.getState().equals(TaskInfoState.SUCCESS) || info.getState().equals(TaskInfoState.ERROR))) {
                        s_logger.debug("There is already a VM snapshot task running, wait for it");
                        context.getVimClient().waitForTask(taskMor);
                    }
                }
            }

            vmMo = hyperHost.findVmOnHyperHost(vmName);

            if (vmMo == null) {
                vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            }

            if (vmMo == null) {
                String msg = "Unable to find VM for CreateVMSnapshotCommand";
                s_logger.info(msg);

                return new CreateVMSnapshotAnswer(cmd, false, msg);
            } else {
                if (vmMo.getSnapshotMor(vmSnapshotName) != null) {
                    s_logger.info("VM snapshot " + vmSnapshotName + " already exists");
                } else if (!vmMo.createSnapshot(vmSnapshotName, vmSnapshotDesc, snapshotMemory, quiescevm)) {
                    return new CreateVMSnapshotAnswer(cmd, false, "Unable to create snapshot due to esxi internal failed");
                }

                Map<String, String> mapNewDisk = getNewDiskMap(vmMo);

                setVolumeToPathAndSize(volumeTOs, mapNewDisk, context, hyperHost, cmd.getVmName());

                return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), volumeTOs);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("failed to create snapshot for vm:" + vmName + " due to " + msg);

            try {
                if (vmMo.getSnapshotMor(vmSnapshotName) != null) {
                    vmMo.removeSnapshot(vmSnapshotName, false);
                }
            } catch (Exception e1) {
                s_logger.info("[ignored]" + "error during snapshot remove: " + e1.getLocalizedMessage());
            }

            return new CreateVMSnapshotAnswer(cmd, false, e.getMessage());
        }
    }

    private Map<String, String> getNewDiskMap(VirtualMachineMO vmMo) throws Exception {
        Map<String, String> mapNewDisk = new HashMap<String, String>();

        // find VM disk file path after creating snapshot
        VirtualDisk[] vdisks = vmMo.getAllDiskDevice();

        for (int i = 0; i < vdisks.length; i++) {
            List<Pair<String, ManagedObjectReference>> vmdkFiles = vmMo.getDiskDatastorePathChain(vdisks[i], false);

            for (Pair<String, ManagedObjectReference> fileItem : vmdkFiles) {
                String fullPath = fileItem.first();
                String baseName = null;
                String vmdkName = null;

                // if this is managed storage
                if (fullPath.startsWith("[-iqn.")) { // ex. [-iqn.2010-01.com.company:3y8w.vol-10.64-0] -iqn.2010-01.com.company:3y8w.vol-10.64-0-000001.vmdk
                    baseName = fullPath.split(" ")[0]; // ex. [-iqn.2010-01.com.company:3y8w.vol-10.64-0]

                    // remove '[' and ']'
                    baseName = baseName.substring(1, baseName.length() - 1);

                    vmdkName = fullPath; // for managed storage, vmdkName == fullPath
                } else {
                    vmdkName = fullPath.split("] ")[1];

                    if (vmdkName.endsWith(".vmdk")) {
                        vmdkName = vmdkName.substring(0, vmdkName.length() - (".vmdk").length());
                    }

                    String token = "/";

                    if (vmdkName.contains(token)) {
                        vmdkName = vmdkName.substring(vmdkName.indexOf(token) + token.length());
                    }

                    baseName = VmwareHelper.trimSnapshotDeltaPostfix(vmdkName);
                }

                mapNewDisk.put(baseName, vmdkName);
            }
        }

        return mapNewDisk;
    }

    private void setVolumeToPathAndSize(List<VolumeObjectTO> volumeTOs, Map<String, String> mapNewDisk, VmwareContext context, VmwareHypervisorHost hyperHost, String vmName)
            throws Exception {
        for (VolumeObjectTO volumeTO : volumeTOs) {
            String oldPath = volumeTO.getPath();

            final String baseName;

            // if this is managed storage
            if (oldPath.startsWith("[-iqn.")) { // ex. [-iqn.2010-01.com.company:3y8w.vol-10.64-0] -iqn.2010-01.com.company:3y8w.vol-10.64-0-000001.vmdk
                oldPath = oldPath.split(" ")[0]; // ex. [-iqn.2010-01.com.company:3y8w.vol-10.64-0]

                // remove '[' and ']'
                baseName = oldPath.substring(1, oldPath.length() - 1);
            } else {
                baseName = VmwareHelper.trimSnapshotDeltaPostfix(volumeTO.getPath());
            }

            String newPath = mapNewDisk.get(baseName);

            // get volume's chain size for this VM snapshot; exclude current volume vdisk
            DataStoreTO store = volumeTO.getDataStore();
            ManagedObjectReference morDs = getDatastoreAsManagedObjectReference(baseName, hyperHost, store);
            long size = getVMSnapshotChainSize(context, hyperHost, baseName + ".vmdk", morDs, newPath);
            size = getVMSnapshotChainSize(context, hyperHost, baseName + "-*.vmdk", morDs, newPath);

            if (volumeTO.getVolumeType() == Volume.Type.ROOT) {
                // add memory snapshot size
                size += getVMSnapshotChainSize(context, hyperHost, vmName + "-*.vmsn", morDs, null);
            }

            volumeTO.setSize(size);
            volumeTO.setPath(newPath);
        }
    }

    private ManagedObjectReference getDatastoreAsManagedObjectReference(String baseName, VmwareHypervisorHost hyperHost, DataStoreTO store) throws Exception {
        try {
            // if baseName equates to a datastore name, this should be managed storage
            ManagedObjectReference morDs = hyperHost.findDatastoreByName(baseName);

            if (morDs != null) {
                return morDs;
            }
        } catch (Exception ex) {
            s_logger.info("[ignored]" + "error getting managed object refference: " + ex.getLocalizedMessage());
        }

        // not managed storage, so use the standard way of getting a ManagedObjectReference for a datastore
        return HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, store.getUuid());
    }

    @Override
    public DeleteVMSnapshotAnswer execute(VmwareHostService hostService, DeleteVMSnapshotCommand cmd) {
        List<VolumeObjectTO> listVolumeTo = cmd.getVolumeTOs();
        VirtualMachineMO vmMo = null;
        VmwareContext context = hostService.getServiceContext(cmd);
        String vmName = cmd.getVmName();
        String vmSnapshotName = cmd.getTarget().getSnapshotName();

        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
            vmMo = hyperHost.findVmOnHyperHost(vmName);

            if (vmMo == null) {
                vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            }

            if (vmMo == null) {
                String msg = "Unable to find VM for RevertToVMSnapshotCommand";
                s_logger.debug(msg);

                return new DeleteVMSnapshotAnswer(cmd, false, msg);
            } else {
                if (vmMo.getSnapshotMor(vmSnapshotName) == null) {
                    s_logger.debug("can not find the snapshot " + vmSnapshotName + ", assume it is already removed");
                } else {
                    if (!vmMo.removeSnapshot(vmSnapshotName, false)) {
                        String msg = "delete vm snapshot " + vmSnapshotName + " due to error occured in vmware";
                        s_logger.error(msg);

                        return new DeleteVMSnapshotAnswer(cmd, false, msg);
                    }
                }

                s_logger.debug("snapshot: " + vmSnapshotName + " is removed");

                // after removed snapshot, the volumes' paths have been changed for the VM, needs to report new paths to manager

                Map<String, String> mapNewDisk = getNewDiskMap(vmMo);

                setVolumeToPathAndSize(listVolumeTo, mapNewDisk, context, hyperHost, cmd.getVmName());

                return new DeleteVMSnapshotAnswer(cmd, listVolumeTo);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("failed to delete vm snapshot " + vmSnapshotName + " of vm " + vmName + " due to " + msg);

            return new DeleteVMSnapshotAnswer(cmd, false, msg);
        }
    }

    @Override
    public RevertToVMSnapshotAnswer execute(VmwareHostService hostService, RevertToVMSnapshotCommand cmd) {
        String snapshotName = cmd.getTarget().getSnapshotName();
        String vmName = cmd.getVmName();
        Boolean snapshotMemory = cmd.getTarget().getType() == VMSnapshot.Type.DiskAndMemory;
        List<VolumeObjectTO> listVolumeTo = cmd.getVolumeTOs();
        VirtualMachine.PowerState vmState = VirtualMachine.PowerState.PowerOn;
        VirtualMachineMO vmMo = null;
        VmwareContext context = hostService.getServiceContext(cmd);

        try {
            VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

            // wait if there are already VM revert task running
            ManagedObjectReference taskmgr = context.getServiceContent().getTaskManager();
            List<ManagedObjectReference> tasks = context.getVimClient().getDynamicProperty(taskmgr, "recentTask");

            for (ManagedObjectReference taskMor : tasks) {
                TaskInfo info = (TaskInfo)(context.getVimClient().getDynamicProperty(taskMor, "info"));

                if (info.getEntityName().equals(cmd.getVmName()) && StringUtils.isNotBlank(info.getName()) && info.getName().equalsIgnoreCase("RevertToSnapshot_Task")) {
                    s_logger.debug("There is already a VM snapshot task running, wait for it");
                    context.getVimClient().waitForTask(taskMor);
                }
            }

            HostMO hostMo = (HostMO)hyperHost;
            vmMo = hyperHost.findVmOnHyperHost(vmName);

            if (vmMo == null) {
                vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            }

            if (vmMo == null) {
                String msg = "Unable to find VM for RevertToVMSnapshotCommand";
                s_logger.debug(msg);

                return new RevertToVMSnapshotAnswer(cmd, false, msg);
            } else {
                if (cmd.isReloadVm()) {
                    vmMo.reload();
                }

                boolean result = false;

                if (snapshotName != null) {
                    ManagedObjectReference morSnapshot = vmMo.getSnapshotMor(snapshotName);

                    result = hostMo.revertToSnapshot(morSnapshot);
                } else {
                    return new RevertToVMSnapshotAnswer(cmd, false, "Unable to find the snapshot by name " + snapshotName);
                }

                if (result) {
                    Map<String, String> mapNewDisk = getNewDiskMap(vmMo);

                    setVolumeToPathAndSize(listVolumeTo, mapNewDisk, context, hyperHost, cmd.getVmName());

                    if (!snapshotMemory) {
                        vmState = VirtualMachine.PowerState.PowerOff;
                    }

                    return new RevertToVMSnapshotAnswer(cmd, listVolumeTo, vmState);
                } else {
                    return new RevertToVMSnapshotAnswer(cmd, false, "Error while reverting to snapshot due to execute in ESXi");
                }
            }
        } catch (Exception e) {
            String msg = "revert vm " + vmName + " to snapshot " + snapshotName + " failed due to " + e.getMessage();
            s_logger.error(msg);

            return new RevertToVMSnapshotAnswer(cmd, false, msg);
        }
    }

    private String deleteVolumeDirOnSecondaryStorage(long volumeId, String secStorageUrl, Integer nfsVersion) throws Exception {
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl, nfsVersion);
        String volumeMountRoot = secondaryMountPoint + "/" + getVolumeRelativeDirInSecStroage(volumeId);

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

    private static String getVolumeRelativeDirInSecStroage(long volumeId) {
        return "volumes/" + volumeId;
    }
}
