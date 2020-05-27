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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
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
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.StorageOperations;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VDI;

public class Xenserver625StorageProcessor extends XenServerStorageProcessor {
    private static final Logger s_logger = Logger.getLogger(XenServerStorageProcessor.class);

    public Xenserver625StorageProcessor(final CitrixResourceBase resource) {
        super(resource);
    }

    private void mountNfs(Connection conn, String remoteDir, String localDir) {
        if (localDir == null) {
            localDir = BASE_MOUNT_POINT_ON_REMOTE + UUID.nameUUIDFromBytes(remoteDir.getBytes());
        }
        String result = hypervisorResource.callHostPluginAsync(conn, "cloud-plugin-storage", "mountNfsSecondaryStorage", 100 * 1000, "localDir", localDir, "remoteDir", remoteDir);
        if (StringUtils.isBlank(result)) {
            String errMsg = "Could not mount secondary storage " + remoteDir + " on host " + localDir;
            s_logger.warn(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    protected boolean makeDirectory(Connection conn, String path) {
        String result = hypervisorResource.callHostPlugin(conn, "cloud-plugin-storage", "makeDirectory", "path", path);
        return StringUtils.isNotBlank(result);
    }

    /**
     *  Creates the file SR for the given path. If there already exist a file SR for the path, we return the existing one.
     *  This method uses a synchronized block to guarantee that only a single file SR is created per path.
     *  If it is not possible to retrieve one file SR or to create one, a runtime exception will be thrown.
     */
    protected SR createFileSR(Connection conn, String path) {
        String srPath = StringUtils.trim(path);
        synchronized (srPath) {
            SR sr = retrieveAlreadyConfiguredSrWithoutException(conn, srPath);
            if (sr == null) {
                sr = createNewFileSr(conn, srPath);
            }
            if (sr == null) {
                String hostUuid = this.hypervisorResource._host.getUuid();
                throw new CloudRuntimeException(String.format("Could not retrieve an already used file SR for path [%s] or create a new file SR on host [%s]", srPath, hostUuid));
            }
            return sr;
        }
    }

    /**
     * Creates a new file SR for the given path. If any of XenServer's checked exception occurs, we use method {@link #removeSrAndPbdIfPossible(Connection, SR, PBD)} to clean the created PBD and SR entries.
     * To avoid race conditions between management servers, we are using a deterministic srUuid for the file SR to be created (we are leaving XenServer with the burden of managing race conditions). The UUID is based on the SR file path, and is generated using {@link UUID#nameUUIDFromBytes(byte[])}.
     * If there is an SR with the generated UUID, this means that some other management server has just created it. An exception will occur and this exception will be an {@link InternalError}. The exception will contain {@link InternalError#message} a message saying 'Db_exn.Uniqueness_constraint_violation'.
     * For cases where the previous described error happens, we catch the exception and use the method {@link #retrieveAlreadyConfiguredSrWithoutException(Connection, String)}.
     */
    protected SR createNewFileSr(Connection conn, String srPath) {
        String hostUuid = hypervisorResource.getHost().getUuid();
        s_logger.debug(String.format("Creating file SR for path [%s] on host [%s]", srPath, this.hypervisorResource._host.getUuid()));
        SR sr = null;
        PBD pbd = null;
        try {
            Host host = Host.getByUuid(conn, hostUuid);
            String srUuid = UUID.nameUUIDFromBytes(srPath.getBytes()).toString();

            Map<String, String> smConfig = new HashMap<String, String>();
            sr = SR.introduce(conn, srUuid, srPath, srPath, "file", "file", false, smConfig);

            PBD.Record record = new PBD.Record();
            record.host = host;
            record.SR = sr;
            smConfig.put("location", srPath);
            record.deviceConfig = smConfig;
            pbd = PBD.create(conn, record);
            pbd.plug(conn);
            sr.scan(conn);
            return sr;
        } catch (XenAPIException | XmlRpcException e) {
            if (e instanceof Types.InternalError) {
                String expectedDuplicatedFileSrErrorMessage = "Db_exn.Uniqueness_constraint_violation";

                Types.InternalError internalErrorException = (Types.InternalError)e;
                if (StringUtils.contains(internalErrorException.message, expectedDuplicatedFileSrErrorMessage)) {
                    s_logger.debug(String.format(
                            "It seems that we have hit a race condition case here while creating file SR for [%s]. Instead of creating one, we will reuse the one that already exist in the XenServer pool.",
                            srPath));
                    return retrieveAlreadyConfiguredSrWithoutException(conn, srPath);
                }
            }
            removeSrAndPbdIfPossible(conn, sr, pbd);
            s_logger.debug(String.format("Could not create file SR [%s] on host [%s].", srPath, hostUuid), e);
            return null;
        }
    }

    /**
     * Calls {@link #unplugPbd(Connection, PBD)} and {@link #forgetSr(Connection, SR)}, if respective objects are not null.
     */
    protected void removeSrAndPbdIfPossible(Connection conn, SR sr, PBD pbd) {
        if (pbd != null) {
            unplugPbd(conn, pbd);
        }
        if (sr != null) {
            forgetSr(conn, sr);
        }
    }

    /**
     * This is a simple facade for {@link #retrieveAlreadyConfiguredSr(Connection, String)} method.
     * If we catch any of the checked exception of {@link #retrieveAlreadyConfiguredSr(Connection, String)}, we re-throw as a {@link CloudRuntimeException}.
     */
    protected SR retrieveAlreadyConfiguredSrWithoutException(Connection conn, String srPath) {
        try {
            return retrieveAlreadyConfiguredSr(conn, srPath);
        } catch (XenAPIException | XmlRpcException e) {
            throw new CloudRuntimeException("Unexpected exception while trying to retrieve an already configured file SR for path: " + srPath);
        }
    }

    /**
     *  This method will check if there is an already configured file SR for the given path. If by any chance we find more than one SR with the same name (mount point path) we throw a runtime exception because this situation should never happen.
     *  If we find an SR, we check if the SR is working properly (performing an {@link SR#scan(Connection)}). If everything is ok with the SR, we return it.
     *  Otherwise, we remove the SR using {@link #forgetSr(Connection, SR)} method;
     */
    protected SR retrieveAlreadyConfiguredSr(Connection conn, String path) throws XenAPIException, XmlRpcException {
        Set<SR> srs = SR.getByNameLabel(conn, path);
        if (CollectionUtils.isEmpty(srs)) {
            s_logger.debug("No file SR found for path: " + path);
            return null;
        }
        if (srs.size() > 1) {
            throw new CloudRuntimeException("There should be only one SR with name-label: " + path);
        }
        SR sr = srs.iterator().next();
        String srUuid = sr.getUuid(conn);
        s_logger.debug(String.format("SR [%s] was already introduced in XenServer. Checking if we can reuse it.", srUuid));
        Map<String, StorageOperations> currentOperations = sr.getCurrentOperations(conn);
        if (MapUtils.isEmpty(currentOperations)) {
            s_logger.debug(String.format("There are no current operation in SR [%s]. It looks like an unusual condition. We will check if it is usable before returning it.", srUuid));
        }
        try {
            sr.scan(conn);
        } catch (XenAPIException | XmlRpcException e) {
            s_logger.debug(String.format("Problems while checking if cached temporary SR [%s] is working properly (we executed sr-scan). We will not reuse it.", srUuid));
            forgetSr(conn, sr);
            return null;
        }
        s_logger.debug(String.format("Cached temporary SR [%s] is working properly. We will reuse it.", srUuid));
        return sr;
    }

    /**
     *  Forgets the given SR. Before executing the command {@link SR#forget(Connection)}, we will unplug all of its PBDs using {@link PBD#unplug(Connection)}.
     *  Checked exceptions are captured and re-thrown as {@link CloudRuntimeException}.
     */
    protected void forgetSr(Connection conn, SR sr) {
        String srUuid = StringUtils.EMPTY;
        try {
            srUuid = sr.getUuid(conn);
            Set<PBD> pbDs = sr.getPBDs(conn);
            for (PBD pbd : pbDs) {
                s_logger.debug(String.format("Unpluging PBD [%s] of SR [%s] as it is not working properly.", pbd.getUuid(conn), srUuid));
                unplugPbd(conn, pbd);
            }
            s_logger.debug(String.format("Forgetting SR [%s] as it is not working properly.", srUuid));
            sr.forget(conn);
        } catch (XenAPIException | XmlRpcException e) {
            throw new CloudRuntimeException("Exception while forgeting SR: " + srUuid, e);
        }
    }

    /**
     * Unplugs the given {@link PBD}. If checked exception happens, we re-throw as {@link CloudRuntimeException}
     */
    protected void unplugPbd(Connection conn, PBD pbd) {
        String pbdUuid = StringUtils.EMPTY;
        try {
            pbdUuid = pbd.getUuid(conn);
            pbd.unplug(conn);
        } catch (XenAPIException | XmlRpcException e) {
            throw new CloudRuntimeException(String.format("Exception while unpluging PBD [%s].", pbdUuid));
        }
    }

    protected SR createFileSr(Connection conn, String remotePath, String dir) {
        String localDir = BASE_MOUNT_POINT_ON_REMOTE + UUID.nameUUIDFromBytes(remotePath.getBytes());
        mountNfs(conn, remotePath, localDir);
        return createFileSR(conn, localDir + "/" + dir);
    }

    @Override
    public Answer copyTemplateToPrimaryStorage(final CopyCommand cmd) {
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWait();
        final DataStoreTO srcStore = srcData.getDataStore();
        final Connection conn = hypervisorResource.getConnection();
        SR srcSr = null;
        SR destSr = null;
        boolean removeSrAfterCopy = false;
        Task task = null;

        try {
            if (srcStore instanceof NfsTO && srcData.getObjectType() == DataObjectType.TEMPLATE) {
                final NfsTO srcImageStore = (NfsTO)srcStore;
                final TemplateObjectTO srcTemplate = (TemplateObjectTO)srcData;
                final String storeUrl = srcImageStore.getUrl();
                final URI uri = new URI(storeUrl);
                String volumePath = srcData.getPath();

                volumePath = StringUtils.stripEnd(volumePath, "/");

                final String[] splits = volumePath.split("/");
                String volumeDirectory = volumePath;

                if (splits.length > 4) {
                    // "template/tmpl/dcid/templateId/templatename"
                    final int index = volumePath.lastIndexOf("/");

                    volumeDirectory = volumePath.substring(0, index);
                }

                srcSr = createFileSr(conn, uri.getHost() + ":" + uri.getPath(), volumeDirectory);

                final Set<VDI> setVdis = srcSr.getVDIs(conn);

                if (setVdis.size() != 1) {
                    return new CopyCmdAnswer("Expected 1 VDI template, but found " + setVdis.size() + " VDI templates on: " + uri.getHost() + ":" + uri.getPath() + "/" + volumeDirectory);
                }

                final VDI srcVdi = setVdis.iterator().next();

                boolean managed = false;
                String storageHost = null;
                String managedStoragePoolName = null;
                String managedStoragePoolRootVolumeName = null;
                String managedStoragePoolRootVolumeSize = null;
                String chapInitiatorUsername = null;
                String chapInitiatorSecret = null;

                final PrimaryDataStoreTO destStore = (PrimaryDataStoreTO)destData.getDataStore();

                Map<String, String> details = destStore.getDetails();

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

                if (managed) {
                    details = new HashMap<String, String>();

                    details.put(DiskTO.STORAGE_HOST, storageHost);
                    details.put(DiskTO.IQN, managedStoragePoolName);
                    details.put(DiskTO.VOLUME_SIZE, managedStoragePoolRootVolumeSize);
                    details.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInitiatorUsername);
                    details.put(DiskTO.CHAP_INITIATOR_SECRET, chapInitiatorSecret);

                    destSr = hypervisorResource.prepareManagedSr(conn, details);
                } else {
                    final String srName = destStore.getUuid();
                    final Set<SR> srs = SR.getByNameLabel(conn, srName);

                    if (srs.size() != 1) {
                        final String msg = "There are " + srs.size() + " SRs with same name: " + srName;

                        s_logger.warn(msg);

                        return new CopyCmdAnswer(msg);
                    } else {
                        destSr = srs.iterator().next();
                    }
                }

                task = srcVdi.copyAsync(conn, destSr, null, null);

                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);

                final VDI tmplVdi = Types.toVDI(task, conn);

                final String uuidToReturn;
                final Long physicalSize = tmplVdi.getPhysicalUtilisation(conn);

                if (managed) {
                    uuidToReturn = tmplVdi.getUuid(conn);

                    tmplVdi.setNameLabel(conn, managedStoragePoolRootVolumeName);
                } else {
                    final VDI snapshotVdi = tmplVdi.snapshot(conn, new HashMap<String, String>());

                    uuidToReturn = snapshotVdi.getUuid(conn);

                    snapshotVdi.setNameLabel(conn, "Template " + srcTemplate.getName());

                    tmplVdi.destroy(conn);
                }

                destSr.scan(conn);

                try {
                    Thread.sleep(5000);
                } catch (final Exception e) {
                }

                final TemplateObjectTO newVol = new TemplateObjectTO();

                newVol.setUuid(uuidToReturn);
                newVol.setPath(uuidToReturn);

                if (physicalSize != null) {
                    newVol.setSize(physicalSize);
                }

                newVol.setFormat(Storage.ImageFormat.VHD);

                return new CopyCmdAnswer(newVol);
            }
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " for template due to " + e.toString();

            s_logger.warn(msg, e);

            return new CopyCmdAnswer(msg);
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e) {
                    s_logger.debug("unable to destroy task (" + task.toWireString() + ") due to " + e.toString());
                }
            }

            if (srcSr != null) {
                hypervisorResource.removeSR(conn, srcSr);
            }

            if (removeSrAfterCopy && destSr != null) {
                hypervisorResource.removeSR(conn, destSr);
            }
        }

        return new CopyCmdAnswer("not implemented yet");
    }

    protected String backupSnapshot(final Connection conn, final String primaryStorageSRUuid, final String localMountPoint, final String path, final String secondaryStorageMountPath,
            final String snapshotUuid, String prevBackupUuid, final String prevSnapshotUuid, final Boolean isISCSI, int wait) {
        boolean filesrcreated = false;
        // boolean copied = false;

        if (prevBackupUuid == null) {
            prevBackupUuid = "";
        }
        SR ssSR = null;

        final String remoteDir = secondaryStorageMountPath;
        try {
            ssSR = createFileSr(conn, remoteDir, path);
            filesrcreated = true;

            final VDI snapshotvdi = VDI.getByUuid(conn, snapshotUuid);
            if (wait == 0) {
                wait = 2 * 60 * 60;
            }
            VDI dvdi = null;
            Task task = null;
            try {
                VDI previousSnapshotVdi = null;
                if (prevSnapshotUuid != null) {
                    previousSnapshotVdi = VDI.getByUuid(conn, prevSnapshotUuid);
                }
                task = snapshotvdi.copyAsync(conn, ssSR, previousSnapshotVdi, null);
                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);
                dvdi = Types.toVDI(task, conn);
                ssSR.scan(conn);
                // copied = true;
            } finally {
                if (task != null) {
                    try {
                        task.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.warn("unable to destroy task(" + task.toWireString() + ") due to " + e.toString());
                    }
                }
            }
            final String result = dvdi.getUuid(conn).concat("#").concat(dvdi.getPhysicalUtilisation(conn).toString());
            return result;
        } catch (final Exception e) {
            final String msg = "Exception in backupsnapshot stage due to " + e.toString();
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (filesrcreated && ssSR != null) {
                    hypervisorResource.removeSR(conn, ssSR);
                }
            } catch (final Exception e) {
                s_logger.debug("Exception in backupsnapshot cleanup stage due to " + e.toString());
            }
        }
    }

    @Override
    protected String getVhdParent(final Connection conn, final String primaryStorageSRUuid, final String snapshotUuid, final Boolean isISCSI) {
        final String parentUuid = hypervisorResource.callHostPlugin(conn, "cloud-plugin-storage", "getVhdParent", "primaryStorageSRUuid", primaryStorageSRUuid, "snapshotUuid", snapshotUuid, "isISCSI",
                isISCSI.toString());

        if (parentUuid == null || parentUuid.isEmpty() || parentUuid.equalsIgnoreCase("None")) {
            s_logger.debug("Unable to get parent of VHD " + snapshotUuid + " in SR " + primaryStorageSRUuid);
            // errString is already logged.
            return null;
        }
        return parentUuid;
    }

    @Override
    public Answer backupSnapshot(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final DataTO cacheData = cmd.getCacheTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWait();
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)srcData.getDataStore();
        final String primaryStorageNameLabel = primaryStore.getUuid();
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

        final SnapshotObjectTO snapshotTO = (SnapshotObjectTO)srcData;
        final SnapshotObjectTO snapshotOnImage = (SnapshotObjectTO)destData;
        String snapshotUuid = snapshotTO.getPath();

        final String prevBackupUuid = snapshotOnImage.getParentSnapshotPath();
        final String prevSnapshotUuid = snapshotTO.getParentSnapshotPath();
        final Map<String, String> options = cmd.getOptions();
        // By default assume failure
        String details = null;
        String snapshotBackupUuid = null;
        boolean fullbackup = Boolean.parseBoolean(options.get("fullSnapshot"));
        Long physicalSize = null;
        try {

            SR primaryStorageSR = null;
            if (primaryStore.isManaged()) {
                fullbackup = true; // currently, managed storage only supports full backup

                final Map<String, String> srcDetails = cmd.getOptions();

                final String iScsiName = srcDetails.get(DiskTO.IQN);
                final String storageHost = srcDetails.get(DiskTO.STORAGE_HOST);
                final String chapInitiatorUsername = srcDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
                final String chapInitiatorSecret = srcDetails.get(DiskTO.CHAP_INITIATOR_SECRET);
                final String srType = CitrixResourceBase.SRType.LVMOISCSI.toString();

                primaryStorageSR = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, false, srType, true);

                final VDI srcVdi = primaryStorageSR.getVDIs(conn).iterator().next();
                if (srcVdi == null) {
                    throw new InternalErrorException("Could not Find a VDI on the SR: " + primaryStorageSR.getNameLabel(conn));
                }
                snapshotUuid = srcVdi.getUuid(conn);

            } else {
                primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            }

            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            // String psUuid = primaryStorageSR.getUuid(conn);
            final Boolean isISCSI = IsISCSI(primaryStorageSR.getType(conn));

            final VDI snapshotVdi = getVDIbyUuid(conn, snapshotUuid);
            final String snapshotPaUuid = snapshotVdi.getUuid(conn);

            final URI uri = new URI(secondaryStorageUrl);
            final String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            final DataStoreTO destStore = destData.getDataStore();
            final String folder = destPath;
            String finalPath = null;

            final String localMountPoint = BaseMountPointOnHost + File.separator + UUID.nameUUIDFromBytes(secondaryStorageUrl.getBytes()).toString();
            if (fullbackup) {
                SR snapshotSr = null;
                Task task = null;
                try {
                    final String localDir = BASE_MOUNT_POINT_ON_REMOTE + UUID.nameUUIDFromBytes(secondaryStorageMountPath.getBytes());
                    mountNfs(conn, secondaryStorageMountPath, localDir);
                    final boolean result = makeDirectory(conn, localDir + "/" + folder);
                    if (!result) {
                        details = " Failed to create folder " + folder + " in secondary storage";
                        s_logger.warn(details);
                        return new CopyCmdAnswer(details);
                    }

                    snapshotSr = createFileSr(conn, secondaryStorageMountPath, folder);

                    task = snapshotVdi.copyAsync(conn, snapshotSr, null, null);
                    // poll every 1 seconds ,
                    hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                    hypervisorResource.checkForSuccess(conn, task);
                    final VDI backedVdi = Types.toVDI(task, conn);
                    snapshotBackupUuid = backedVdi.getUuid(conn);
                    snapshotSr.scan(conn);
                    physicalSize = backedVdi.getPhysicalUtilisation(conn);

                    if (destStore instanceof SwiftTO) {
                        try {
                            final String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                            final String destSnapshotName = swiftBackupSnapshot(conn, (SwiftTO)destStore, snapshotSr.getUuid(conn), snapshotBackupUuid, container, false, wait);
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
                            finalPath = backupSnapshotToS3(conn, (S3TO)destStore, snapshotSr.getUuid(conn), folder, snapshotBackupUuid, isISCSI, wait);
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
                        // finalPath = folder + File.separator +
                        // snapshotBackupUuid;
                    } else {
                        finalPath = folder + File.separator + snapshotBackupUuid + ".vhd";
                    }

                } finally {
                    if (task != null) {
                        try {
                            task.destroy(conn);
                        } catch (final Exception e) {
                            s_logger.warn("unable to destroy task(" + task.toWireString() + ") due to " + e.toString());
                        }
                    }
                    if (snapshotSr != null) {
                        hypervisorResource.removeSR(conn, snapshotSr);
                    }

                    if (primaryStore.isManaged()) {
                        hypervisorResource.removeSR(conn, primaryStorageSR);
                    }
                }
            } else {
                final String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                if (destStore instanceof SwiftTO) {
                    final String container = "S-" + snapshotTO.getVolume().getVolumeId().toString();
                    snapshotBackupUuid = swiftBackupSnapshot(conn, (SwiftTO)destStore, primaryStorageSRUuid, snapshotPaUuid, "S-" + snapshotTO.getVolume().getVolumeId().toString(), isISCSI, wait);
                    finalPath = container + File.separator + snapshotBackupUuid;
                } else if (destStore instanceof S3TO) {
                    finalPath = backupSnapshotToS3(conn, (S3TO)destStore, primaryStorageSRUuid, folder, snapshotPaUuid, isISCSI, wait);
                    if (finalPath == null) {
                        throw new CloudRuntimeException("S3 upload of snapshots " + snapshotPaUuid + " failed");
                    }
                } else {
                    final String result = backupSnapshot(conn, primaryStorageSRUuid, localMountPoint, folder, secondaryStorageMountPath, snapshotUuid, prevBackupUuid, prevSnapshotUuid, isISCSI, wait);
                    final String[] tmp = result.split("#");
                    snapshotBackupUuid = tmp[0];
                    physicalSize = Long.parseLong(tmp[1]);
                    finalPath = folder + File.separator + snapshotBackupUuid + ".vhd";
                }
            }

            // remove every snapshot except this one from primary storage
            final String volumeUuid = snapshotTO.getVolume().getPath();
            destroySnapshotOnPrimaryStorageExceptThis(conn, volumeUuid, snapshotUuid);

            final SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(finalPath);
            newSnapshot.setPhysicalSize(physicalSize);
            if (fullbackup) {
                newSnapshot.setParentSnapshotPath(null);
            } else {
                newSnapshot.setParentSnapshotPath(prevBackupUuid);
            }
            s_logger.info("New snapshot details: " + newSnapshot.toString());
            s_logger.info("New snapshot physical utilization: " + physicalSize);

            return new CopyCmdAnswer(newSnapshot);
        } catch (final Exception e) {
            final String reason = e instanceof Types.XenAPIException ? e.toString() : e.getMessage();
            details = "BackupSnapshot Failed due to " + reason;
            s_logger.warn(details, e);

            // remove last bad primary snapshot when exception happens
            destroySnapshotOnPrimaryStorage(conn, snapshotUuid);
        }

        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer createTemplateFromVolume(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final VolumeObjectTO volume = (VolumeObjectTO)cmd.getSrcTO();
        final TemplateObjectTO template = (TemplateObjectTO)cmd.getDestTO();
        final NfsTO destStore = (NfsTO)cmd.getDestTO().getDataStore();
        final int wait = cmd.getWait();

        final String secondaryStoragePoolURL = destStore.getUrl();
        final String volumeUUID = volume.getPath();

        final String userSpecifiedName = template.getName();

        String details = null;
        SR tmpltSR = null;
        boolean result = false;
        String secondaryStorageMountPath = null;
        String installPath = null;
        Task task = null;
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
            tmpltSR = createFileSr(conn, uri.getHost() + ":" + uri.getPath(), installPath);

            // copy volume to template SR
            task = vol.copyAsync(conn, tmpltSR, null, null);
            // poll every 1 seconds ,
            hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
            hypervisorResource.checkForSuccess(conn, task);
            final VDI tmpltVDI = Types.toVDI(task, conn);
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
            result = hypervisorResource.postCreatePrivateTemplate(conn, templatePath, tmpltFilename, tmpltUUID, userSpecifiedName, null, physicalSize, virtualSize, template.getId());
            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir");
            }
            installPath = installPath + "/" + tmpltFilename;
            hypervisorResource.removeSR(conn, tmpltSR);
            tmpltSR = null;
            final TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(installPath);
            newTemplate.setFormat(Storage.ImageFormat.VHD);
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
        } finally {
            if (task != null) {
                try {
                    task.destroy(conn);
                } catch (final Exception e) {
                    s_logger.warn("unable to destroy task(" + task.toWireString() + ") due to " + e.toString());
                }
            }
        }
        return new CopyCmdAnswer(details);
    }

    protected String getSnapshotUuid(final String snapshotPath) {
        int index = snapshotPath.lastIndexOf(File.separator);
        String snapshotUuid = snapshotPath.substring(index + 1);
        index = snapshotUuid.lastIndexOf(".");
        if (index != -1) {
            snapshotUuid = snapshotUuid.substring(0, index);
        }
        return snapshotUuid;
    }

    @Override
    public Answer createVolumeFromSnapshot(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final SnapshotObjectTO snapshot = (SnapshotObjectTO)srcData;
        final DataTO destData = cmd.getDestTO();
        final PrimaryDataStoreTO pool = (PrimaryDataStoreTO)destData.getDataStore();
        final VolumeObjectTO volume = (VolumeObjectTO)destData;
        final DataStoreTO imageStore = srcData.getDataStore();

        if (isCreateManagedVolumeFromManagedSnapshot(cmd.getOptions2(), cmd.getOptions())) {
            return createManagedVolumeFromManagedSnapshot(cmd);
        }

        if (isCreateNonManagedVolumeFromManagedSnapshot(cmd.getOptions2(), cmd.getOptions())) {
            return createNonManagedVolumeFromManagedSnapshot(cmd);
        }

        if (!(imageStore instanceof NfsTO)) {
            return new CopyCmdAnswer("unsupported protocol");
        }

        final NfsTO nfsImageStore = (NfsTO)imageStore;
        final String primaryStorageNameLabel = pool.getUuid();
        final String secondaryStorageUrl = nfsImageStore.getUrl();
        final int wait = cmd.getWait();
        boolean result = false;
        // Generic error message.
        String details = null;
        String volumeUUID = null;

        if (secondaryStorageUrl == null) {
            details += " because the URL passed: " + secondaryStorageUrl + " is invalid.";
            return new CopyCmdAnswer(details);
        }
        SR srcSr = null;
        VDI destVdi = null;

        SR primaryStorageSR = null;

        try {
            if (pool.isManaged()) {
                Map<String, String> destDetails = cmd.getOptions2();

                final String iScsiName = destDetails.get(DiskTO.IQN);
                final String storageHost = destDetails.get(DiskTO.STORAGE_HOST);
                final String chapInitiatorUsername = destDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
                final String chapInitiatorSecret = destDetails.get(DiskTO.CHAP_INITIATOR_SECRET);
                final String srType = CitrixResourceBase.SRType.LVMOISCSI.toString();

                primaryStorageSR = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, false, srType, true);

            } else {
                primaryStorageSR = hypervisorResource.getSRByNameLabelandHost(conn, primaryStorageNameLabel);
            }

            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not create volume from snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }

            final String nameLabel = "cloud-" + UUID.randomUUID().toString();
            destVdi = createVdi(conn, nameLabel, primaryStorageSR, volume.getSize());
            volumeUUID = destVdi.getUuid(conn);
            final String snapshotInstallPath = snapshot.getPath();
            final int index = snapshotInstallPath.lastIndexOf(File.separator);
            final String snapshotDirectory = snapshotInstallPath.substring(0, index);
            final String snapshotUuid = getSnapshotUuid(snapshotInstallPath);

            final URI uri = new URI(secondaryStorageUrl);
            srcSr = createFileSr(conn, uri.getHost() + ":" + uri.getPath(), snapshotDirectory);

            final String[] parents = snapshot.getParents();
            final List<VDI> snapshotChains = new ArrayList<VDI>();
            if (parents != null) {
                for (int i = 0; i < parents.length; i++) {
                    final String snChainPath = parents[i];
                    final String uuid = getSnapshotUuid(snChainPath);
                    final VDI chain = VDI.getByUuid(conn, uuid);
                    snapshotChains.add(chain);
                }
            }

            final VDI snapshotVdi = VDI.getByUuid(conn, snapshotUuid);
            snapshotChains.add(snapshotVdi);

            for (final VDI snapChain : snapshotChains) {
                final Task task = snapChain.copyAsync(conn, null, null, destVdi);
                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);
                task.destroy(conn);
            }

            result = true;
            destVdi = VDI.getByUuid(conn, volumeUUID);
            final VDI.Record vdir = destVdi.getRecord(conn);
            final VolumeObjectTO newVol = new VolumeObjectTO();
            newVol.setPath(volumeUUID);
            newVol.setSize(vdir.virtualSize);

            return new CopyCmdAnswer(newVol);
        } catch (final Types.XenAPIException e) {
            details += " due to " + e.toString();
            s_logger.warn(details, e);
        } catch (final Exception e) {
            details += " due to " + e.getMessage();
            s_logger.warn(details, e);
        } finally {
            if (srcSr != null) {
                hypervisorResource.skipOrRemoveSR(conn, srcSr);
            }

            if (pool.isManaged()) {
                hypervisorResource.removeSR(conn, primaryStorageSR);
            }

            if (!result && destVdi != null) {
                try {
                    destVdi.destroy(conn);
                } catch (final Exception e) {
                    s_logger.debug("destroy dest vdi failed", e);
                }
            }
        }
        if (!result) {
            // Is this logged at a higher level?
            s_logger.error(details);
        }

        // In all cases return something.
        return new CopyCmdAnswer(details);
    }

    @Override
    public Answer copyVolumeFromPrimaryToSecondary(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final VolumeObjectTO srcVolume = (VolumeObjectTO)cmd.getSrcTO();
        final VolumeObjectTO destVolume = (VolumeObjectTO)cmd.getDestTO();
        final int wait = cmd.getWait();
        final DataStoreTO destStore = destVolume.getDataStore();

        if (destStore instanceof NfsTO) {
            SR secondaryStorage = null;
            Task task = null;
            try {
                final NfsTO nfsStore = (NfsTO)destStore;
                final URI uri = new URI(nfsStore.getUrl());
                // Create the volume folder
                if (!hypervisorResource.createSecondaryStorageFolder(conn, uri.getHost() + ":" + uri.getPath(), destVolume.getPath())) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }

                // Create a SR for the volume UUID folder
                secondaryStorage = createFileSr(conn, uri.getHost() + ":" + uri.getPath(), destVolume.getPath());
                // Look up the volume on the source primary storage pool
                final VDI srcVdi = getVDIbyUuid(conn, srcVolume.getPath());
                // Copy the volume to secondary storage
                task = srcVdi.copyAsync(conn, secondaryStorage, null, null);
                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);
                final VDI destVdi = Types.toVDI(task, conn);
                final String destVolumeUUID = destVdi.getUuid(conn);

                final VolumeObjectTO newVol = new VolumeObjectTO();
                newVol.setPath(destVolume.getPath() + File.separator + destVolumeUUID + ".vhd");
                newVol.setSize(srcVolume.getSize());
                return new CopyCmdAnswer(newVol);
            } catch (final Exception e) {
                s_logger.debug("Failed to copy volume to secondary: " + e.toString());
                return new CopyCmdAnswer("Failed to copy volume to secondary: " + e.toString());
            } finally {
                if (task != null) {
                    try {
                        task.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.warn("unable to destroy task(" + task.toWireString() + ") due to " + e.toString());
                    }
                }
                hypervisorResource.removeSR(conn, secondaryStorage);
            }
        }
        return new CopyCmdAnswer("unsupported protocol");
    }

    @Override
    public Answer copyVolumeFromImageCacheToPrimary(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();
        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();
        final int wait = cmd.getWait();
        final VolumeObjectTO srcVolume = (VolumeObjectTO)srcData;
        final VolumeObjectTO destVolume = (VolumeObjectTO)destData;
        final PrimaryDataStoreTO primaryStore = (PrimaryDataStoreTO)destVolume.getDataStore();
        final DataStoreTO srcStore = srcVolume.getDataStore();

        if (srcStore instanceof NfsTO) {
            final NfsTO nfsStore = (NfsTO)srcStore;
            final String volumePath = srcVolume.getPath();
            int index = volumePath.lastIndexOf("/");
            final String volumeDirectory = volumePath.substring(0, index);
            String volumeUuid = volumePath.substring(index + 1);
            index = volumeUuid.indexOf(".");
            if (index != -1) {
                volumeUuid = volumeUuid.substring(0, index);
            }
            URI uri = null;
            try {
                uri = new URI(nfsStore.getUrl());
            } catch (final Exception e) {
                return new CopyCmdAnswer(e.toString());
            }
            final SR srcSr = createFileSr(conn, uri.getHost() + ":" + uri.getPath(), volumeDirectory);
            Task task = null;
            try {
                final SR primaryStoragePool = hypervisorResource.getStorageRepository(conn, primaryStore.getUuid());
                final VDI srcVdi = VDI.getByUuid(conn, volumeUuid);
                task = srcVdi.copyAsync(conn, primaryStoragePool, null, null);
                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);
                final VDI destVdi = Types.toVDI(task, conn);
                final VolumeObjectTO newVol = new VolumeObjectTO();
                destVdi.setNameLabel(conn, srcVolume.getName());
                newVol.setPath(destVdi.getUuid(conn));
                newVol.setSize(srcVolume.getSize());

                return new CopyCmdAnswer(newVol);
            } catch (final Exception e) {
                final String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
                s_logger.warn(msg, e);
                return new CopyCmdAnswer(e.toString());
            } finally {
                if (task != null) {
                    try {
                        task.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.warn("unable to destroy task(" + task.toString() + ") due to " + e.toString());
                    }
                }
                if (srcSr != null) {
                    hypervisorResource.removeSR(conn, srcSr);
                }
            }
        }

        s_logger.debug("unsupported protocol");
        return new CopyCmdAnswer("unsupported protocol");
    }

    @Override
    public Answer createTemplateFromSnapshot(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();

        final DataTO srcData = cmd.getSrcTO();
        final DataTO destData = cmd.getDestTO();

        if (srcData.getDataStore() instanceof PrimaryDataStoreTO && destData.getDataStore() instanceof NfsTO) {
            return createTemplateFromSnapshot2(cmd);
        }

        final int wait = cmd.getWait();

        final SnapshotObjectTO srcObj = (SnapshotObjectTO)srcData;
        final TemplateObjectTO destObj = (TemplateObjectTO)destData;

        final NfsTO srcStore = (NfsTO)srcObj.getDataStore();
        final NfsTO destStore = (NfsTO)destObj.getDataStore();

        URI srcUri = null;
        URI destUri = null;

        try {
            srcUri = new URI(srcStore.getUrl());
            destUri = new URI(destStore.getUrl());
        } catch (final Exception e) {
            s_logger.debug("incorrect url", e);

            return new CopyCmdAnswer("incorrect url" + e.toString());
        }

        final String srcPath = srcObj.getPath();
        final int index = srcPath.lastIndexOf("/");
        final String srcDir = srcPath.substring(0, index);
        final String destDir = destObj.getPath();

        SR srcSr = null;
        SR destSr = null;

        VDI destVdi = null;

        boolean result = false;

        try {
            srcSr = createFileSr(conn, srcUri.getHost() + ":" + srcUri.getPath(), srcDir);

            final String destNfsPath = destUri.getHost() + ":" + destUri.getPath();
            final String localDir = BASE_MOUNT_POINT_ON_REMOTE + UUID.nameUUIDFromBytes(destNfsPath.getBytes());

            mountNfs(conn, destUri.getHost() + ":" + destUri.getPath(), localDir);
            makeDirectory(conn, localDir + "/" + destDir);

            destSr = createFileSR(conn, localDir + "/" + destDir);

            final String nameLabel = "cloud-" + UUID.randomUUID().toString();

            final String[] parents = srcObj.getParents();
            final List<VDI> snapshotChains = new ArrayList<VDI>();

            if (parents != null) {
                for (int i = 0; i < parents.length; i++) {
                    final String snChainPath = parents[i];
                    final String uuid = getSnapshotUuid(snChainPath);
                    final VDI chain = VDI.getByUuid(conn, uuid);

                    snapshotChains.add(chain);
                }
            }

            final String snapshotUuid = getSnapshotUuid(srcPath);
            final VDI snapshotVdi = VDI.getByUuid(conn, snapshotUuid);

            snapshotChains.add(snapshotVdi);

            final long templateVirtualSize = snapshotChains.get(0).getVirtualSize(conn);

            destVdi = createVdi(conn, nameLabel, destSr, templateVirtualSize);

            final String destVdiUuid = destVdi.getUuid(conn);

            for (final VDI snapChain : snapshotChains) {
                final Task task = snapChain.copyAsync(conn, null, null, destVdi);
                // poll every 1 seconds ,
                hypervisorResource.waitForTask(conn, task, 1000, wait * 1000);
                hypervisorResource.checkForSuccess(conn, task);

                task.destroy(conn);
            }

            destVdi = VDI.getByUuid(conn, destVdiUuid);

            // scan makes XenServer pick up VDI physicalSize
            destSr.scan(conn);

            final String templateUuid = destVdi.getUuid(conn);
            final String templateFilename = templateUuid + ".vhd";
            final long virtualSize = destVdi.getVirtualSize(conn);
            final long physicalSize = destVdi.getPhysicalUtilisation(conn);

            String templatePath = destNfsPath + "/" + destDir;

            templatePath = templatePath.replaceAll("//", "/");

            result = hypervisorResource.postCreatePrivateTemplate(conn, templatePath, templateFilename, templateUuid, nameLabel, null, physicalSize, virtualSize, destObj.getId());

            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir");
            }

            final TemplateObjectTO newTemplate = new TemplateObjectTO();

            newTemplate.setPath(destDir + "/" + templateFilename);
            newTemplate.setFormat(Storage.ImageFormat.VHD);
            newTemplate.setSize(destVdi.getVirtualSize(conn));
            newTemplate.setPhysicalSize(destVdi.getPhysicalUtilisation(conn));
            newTemplate.setName(destVdiUuid);

            result = true;

            return new CopyCmdAnswer(newTemplate);
        } catch (final Exception e) {
            s_logger.error("Failed create template from snapshot", e);

            return new CopyCmdAnswer("Failed create template from snapshot " + e.toString());
        } finally {
            if (!result) {
                if (destVdi != null) {
                    try {
                        destVdi.destroy(conn);
                    } catch (final Exception e) {
                        s_logger.debug("Clean up left over on dest storage failed: ", e);
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

    private Answer createTemplateFromSnapshot2(final CopyCommand cmd) {
        final Connection conn = hypervisorResource.getConnection();

        final SnapshotObjectTO snapshotObjTO = (SnapshotObjectTO)cmd.getSrcTO();
        final TemplateObjectTO templateObjTO = (TemplateObjectTO)cmd.getDestTO();

        if (!(snapshotObjTO.getDataStore() instanceof PrimaryDataStoreTO) || !(templateObjTO.getDataStore() instanceof NfsTO)) {
            return null;
        }

        NfsTO destStore;
        URI destUri;

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
            String srType;

            srType = CitrixResourceBase.SRType.LVMOISCSI.toString();

            srcSr = hypervisorResource.getIscsiSR(conn, iScsiName, storageHost, iScsiName, chapInitiatorUsername, chapInitiatorSecret, false, srType, true);

            final String destNfsPath = destUri.getHost() + ":" + destUri.getPath();
            final String localDir = BASE_MOUNT_POINT_ON_REMOTE + UUID.nameUUIDFromBytes(destNfsPath.getBytes());

            mountNfs(conn, destNfsPath, localDir);
            makeDirectory(conn, localDir + "/" + destDir);

            destSr = createFileSR(conn, localDir + "/" + destDir);

            // there should only be one VDI in this SR
            final VDI srcVdi = srcSr.getVDIs(conn).iterator().next();

            destVdi = srcVdi.copy(conn, destSr);

            final String nameLabel = "cloud-" + UUID.randomUUID().toString();

            destVdi.setNameLabel(conn, nameLabel);

            // scan makes XenServer pick up VDI physicalSize
            destSr.scan(conn);

            final String templateUuid = destVdi.getUuid(conn);
            final String templateFilename = templateUuid + ".vhd";
            final long virtualSize = destVdi.getVirtualSize(conn);
            final long physicalSize = destVdi.getPhysicalUtilisation(conn);

            // create the template.properties file
            String templatePath = destNfsPath + "/" + destDir;

            templatePath = templatePath.replaceAll("//", "/");

            result = hypervisorResource.postCreatePrivateTemplate(conn, templatePath, templateFilename, templateUuid, nameLabel, null, physicalSize, virtualSize, templateObjTO.getId());

            if (!result) {
                throw new CloudRuntimeException("Could not create the template.properties file on secondary storage dir");
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
        } catch (final BadServerResponse e) {
            s_logger.error("Failed to create a template from a snapshot due to incomprehensible server response", e);

            return new CopyCmdAnswer("Failed to create a template from a snapshot: " + e.toString());
        } catch (final XenAPIException e) {
            s_logger.error("Failed to create a template from a snapshot due to xenapi error", e);

            return new CopyCmdAnswer("Failed to create a template from a snapshot: " + e.toString());
        } catch (final XmlRpcException e) {
            s_logger.error("Failed to create a template from a snapshot due to rpc error", e);

            return new CopyCmdAnswer("Failed to create a template from a snapshot: " + e.toString());
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
}
