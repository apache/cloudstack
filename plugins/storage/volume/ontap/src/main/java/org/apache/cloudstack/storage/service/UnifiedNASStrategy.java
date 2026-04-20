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

package org.apache.cloudstack.storage.service;

import com.cloud.agent.api.Answer;
import com.cloud.host.HostVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import feign.FeignException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.ExportRule;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.Nas;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.feign.model.CliSnapshotRestoreRequest;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnifiedNASStrategy extends NASStrategy {
    private static final Logger logger = LogManager.getLogger(UnifiedNASStrategy.class);
    @Inject private VolumeDao volumeDao;
    @Inject private EndPointSelector epSelector;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;

    public UnifiedNASStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
    }

    public void setOntapStorage(OntapStorage ontapStorage) {
        this.storage = ontapStorage;
    }

    @Override
    public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume) {
        logger.info("createCloudStackVolume: Create cloudstack volume " + cloudstackVolume);
        try {
            // Step 1: set cloudstack volume metadata
            String volumeUuid = updateCloudStackVolumeMetadata(cloudstackVolume.getDatastoreId(), cloudstackVolume.getVolumeInfo());
            // Step 2: Send command to KVM host to create qcow2 file using qemu-img
            Answer answer = createVolumeOnKVMHost(cloudstackVolume.getVolumeInfo());
            if (answer == null || !answer.getResult()) {
                String errMsg = answer != null ? answer.getDetails() : "Failed to create qcow2 on KVM host";
                logger.error("createCloudStackVolume: " + errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            return cloudstackVolume;
        }catch (Exception e) {
            logger.error("createCloudStackVolume: error occured " + e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume) {
        return null;
    }

    @Override
    public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume) {
        logger.info("deleteCloudStackVolume: Delete cloudstack volume " + cloudstackVolume);
        try {
            // Step 1: Send command to KVM host to delete qcow2 file using qemu-img
            Answer answer = deleteVolumeOnKVMHost(cloudstackVolume.getVolumeInfo());
            if (answer == null || !answer.getResult()) {
                String errMsg = answer != null ? answer.getDetails() : "Failed to delete qcow2 on KVM host";
                logger.error("deleteCloudStackVolume: " + errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        }catch (Exception e) {
            logger.error("deleteCloudStackVolume: error occured " + e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void copyCloudStackVolume(CloudStackVolume cloudstackVolume) {

    }

    @Override
    public CloudStackVolume getCloudStackVolume(Map<String, String> cloudStackVolumeMap) {
        logger.info("getCloudStackVolume: Get cloudstack volume " + cloudStackVolumeMap);
        CloudStackVolume cloudStackVolume = null;
        FileInfo fileInfo = getFile(cloudStackVolumeMap.get(Constants.VOLUME_UUID),cloudStackVolumeMap.get(Constants.FILE_PATH));

        if(fileInfo != null){
            cloudStackVolume = new CloudStackVolume();
            cloudStackVolume.setFlexVolumeUuid(cloudStackVolumeMap.get(Constants.VOLUME_UUID));
            cloudStackVolume.setFile(fileInfo);
        } else {
            logger.warn("getCloudStackVolume: File not found for volume UUID: {} and file path: {}", cloudStackVolumeMap.get(Constants.VOLUME_UUID), cloudStackVolumeMap.get(Constants.FILE_PATH));
        }

        return cloudStackVolume;
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup accessGroup) {
        logger.info("createAccessGroup: Create access group {}: " , accessGroup);

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(accessGroup.getStoragePoolId());
        String svmName = details.get(Constants.SVM_NAME);
        String volumeUUID = details.get(Constants.VOLUME_UUID);
        String volumeName = details.get(Constants.VOLUME_NAME);

        // Create the export policy
        ExportPolicy policyRequest = createExportPolicyRequest(accessGroup,svmName,volumeName);
        try {
            ExportPolicy createdPolicy = createExportPolicy(svmName, policyRequest);
            logger.info("createAccessGroup: ExportPolicy created: {}, now attaching this policy to storage pool volume", createdPolicy.getName());
            // attach export policy to volume of storage pool
            assignExportPolicyToVolume(volumeUUID,createdPolicy.getName());
            // save the export policy details in storage pool details
            storagePoolDetailsDao.addDetail(accessGroup.getStoragePoolId(), Constants.EXPORT_POLICY_ID, String.valueOf(createdPolicy.getId()), true);
            storagePoolDetailsDao.addDetail(accessGroup.getStoragePoolId(), Constants.EXPORT_POLICY_NAME, createdPolicy.getName(), true);
            logger.info("Successfully assigned exportPolicy {} to volume {}", policyRequest.getName(), volumeName);
            accessGroup.setPolicy(policyRequest);
            return accessGroup;
        }catch(Exception e){
            logger.error("Exception occurred while creating access group: " +  e);
            throw new CloudRuntimeException("Failed to create access group: " + e);
        }
    }

    @Override
    public void deleteAccessGroup(AccessGroup accessGroup) {
        logger.info("deleteAccessGroup: Deleting export policy");

        if (accessGroup == null) {
            throw new CloudRuntimeException("Invalid accessGroup object - accessGroup is null");
        }

        try {
            Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(accessGroup.getStoragePoolId());
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Determine export policy attached to the storage pool
            String exportPolicyName = details.get(Constants.EXPORT_POLICY_NAME);
            String exportPolicyId = details.get(Constants.EXPORT_POLICY_ID);

            try {
                nasFeignClient.deleteExportPolicyById(authHeader,exportPolicyId);
                logger.info("deleteAccessGroup: Successfully deleted export policy '{}'", exportPolicyName);
            } catch (Exception e) {
                logger.error("deleteAccessGroup: Failed to delete export policy. Exception: {}", e.getMessage(), e);
                throw new CloudRuntimeException("Failed to delete export policy: " + e.getMessage(), e);

            }
        } catch (Exception e) {
            logger.error("deleteAccessGroup: Failed to delete export policy. Exception: {}", e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete export policy: " + e.getMessage(), e);
        }
    }

    @Override
    public AccessGroup updateAccessGroup(AccessGroup accessGroup) {
        return null;
    }

    @Override
    public AccessGroup getAccessGroup(Map<String, String> values) {
        return null;
    }

    @Override
    public Map <String, String> enableLogicalAccess(Map<String, String> values) {
        return null;
    }

    @Override
    public void disableLogicalAccess(Map<String, String> values) {
    }

    @Override
    public Map<String, String> getLogicalAccess(Map<String, String> values) {
        return Map.of();
    }

    private ExportPolicy createExportPolicy(String svmName, ExportPolicy policy) {
        logger.info("createExportPolicy: Creating export policy: {} for SVM: {}", policy, svmName);

        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            nasFeignClient.createExportPolicy(authHeader,  policy);
            OntapResponse<ExportPolicy> policiesResponse = null;
            try {
                Map<String, Object> queryParams = Map.of(Constants.NAME, policy.getName());
                policiesResponse = nasFeignClient.getExportPolicyResponse(authHeader, queryParams);
                if (policiesResponse == null || policiesResponse.getRecords().isEmpty()) {
                    throw new CloudRuntimeException("Export policy " + policy.getName() + " was not created on ONTAP. " +
                            "Received successful response but policy does not exist.");
                }
                logger.info("createExportPolicy: Export policy created and verified successfully: " + policy.getName());
            } catch (FeignException e) {
                logger.error("createExportPolicy: Failed to verify export policy creation: " + policy.getName(), e);
                throw new CloudRuntimeException("Export policy creation verification failed: " + e.getMessage());
            }
            logger.info("createExportPolicy: Export policy created successfully with name {}", policy.getName());
            return policiesResponse.getRecords().get(0);
        } catch (FeignException e) {
            logger.error("createExportPolicy: Failed to create export policy: {}", policy, e);
            throw new CloudRuntimeException("Failed to create export policy: " + e.getMessage());
        } catch (Exception e) {
            logger.error("createExportPolicy: Exception while creating export policy: {}", policy, e);
            throw new CloudRuntimeException("Failed to create export policy: " + e.getMessage());
        }
    }

    private void assignExportPolicyToVolume(String volumeUuid, String policyName) {
        logger.info("Assigning export policy: {} to volume: {}", policyName, volumeUuid);

        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Volume volumeUpdate = new Volume();
            Nas nas = new Nas();
            ExportPolicy policy = new ExportPolicy();
            policy.setName(policyName);
            nas.setExportPolicy(policy);
            volumeUpdate.setNas(nas);

            try {
                JobResponse jobResponse = volumeFeignClient.updateVolumeRebalancing(authHeader, volumeUuid, volumeUpdate);
                if (jobResponse == null || jobResponse.getJob() == null) {
                    throw new CloudRuntimeException("Failed to attach policy " + policyName + "to volume " + volumeUuid);
                }
                String jobUUID = jobResponse.getJob().getUuid();
                //Create URI for GET Job API
                int jobRetryCount = 0;
                Job createVolumeJob = null;
                while(createVolumeJob == null || !createVolumeJob.getState().equals(OntapStorageConstants.JOB_SUCCESS)) {
                    if(jobRetryCount >= OntapStorageConstants.JOB_MAX_RETRIES) {
                        logger.error("Job to update volume " + volumeUuid + " did not complete within expected time.");
                        throw new CloudRuntimeException("Job to update volume " + volumeUuid + " did not complete within expected time.");
                    }
                    try {
                        createVolumeJob = jobFeignClient.getJobByUUID(authHeader, jobUUID);
                        if (createVolumeJob == null) {
                            logger.warn("Job with UUID " + jobUUID + " not found. Retrying...");
                        } else if (createVolumeJob.getState().equals(OntapStorageConstants.JOB_FAILURE)) {
                            throw new CloudRuntimeException("Job to update volume " + volumeUuid + " failed with error: " + createVolumeJob.getMessage());
                        }
                    } catch (FeignException.FeignClientException e) {
                        throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
                    }
                    jobRetryCount++;
                    Thread.sleep(OntapStorageConstants.CREATE_VOLUME_CHECK_SLEEP_TIME);
                }
            } catch (Exception e) {
                logger.error("assignExportPolicyToVolume: Exception while updating volume: ", e);
                throw new CloudRuntimeException("Failed to update volume: " + e.getMessage());
            }
            logger.info("assignExportPolicyToVolume: Export policy successfully assigned to volume: {}", volumeUuid);
        } catch (FeignException e) {
            logger.error("assignExportPolicyToVolume: Failed to assign export policy to volume: {}", volumeUuid, e);
            throw new CloudRuntimeException("Failed to assign export policy: " + e.getMessage());
        } catch (Exception e) {
            logger.error("assignExportPolicyToVolume: Exception while assigning export policy to volume: {}", volumeUuid, e);
            throw new CloudRuntimeException("Failed to assign export policy: " + e.getMessage());
        }
    }

    private boolean createFile(String volumeUuid, String filePath, FileInfo fileInfo) {
        logger.info("createFile: Creating file: {} in volume: {}", filePath, volumeUuid);
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            nasFeignClient.createFile(authHeader, volumeUuid, filePath, fileInfo);
            logger.info("createFile: File created successfully: {} in volume: {}", filePath, volumeUuid);
            return true;
        } catch (FeignException e) {
            logger.error("createFile: Failed to create file: {} in volume: {}", filePath, volumeUuid, e);
            return false;
        } catch (Exception e) {
            logger.error("createFile: Exception while creating file: {} in volume: {}", filePath, volumeUuid, e);
            return false;
        }
    }

    private boolean deleteFile(String volumeUuid, String filePath) {
        logger.info("deleteFile: Deleting file: {} from volume: {}", filePath, volumeUuid);
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            nasFeignClient.deleteFile(authHeader, volumeUuid, filePath);
            logger.info("deleteFile: File deleted successfully: {} from volume: {}", filePath, volumeUuid);
            return true;
        } catch (FeignException e) {
            logger.error("deleteFile: Failed to delete file: {} from volume: {}", filePath, volumeUuid, e);
            return false;
        } catch (Exception e) {
            logger.error("deleteFile: Exception while deleting file: {} from volume: {}", filePath, volumeUuid, e);
            return false;
        }
    }

    private OntapResponse<FileInfo> getFileInfo(String volumeUuid, String filePath) {
        logger.debug("getFileInfo: Getting file info for: {} in volume: {}", filePath, volumeUuid);
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            OntapResponse<FileInfo> response = nasFeignClient.getFileResponse(authHeader, volumeUuid, filePath);
            logger.debug("getFileInfo: Retrieved file info for: {} in volume: {}", filePath, volumeUuid);
            return response;
        } catch (FeignException e){
            if (e.status() == 404) {
                logger.debug("getFileInfo: File not found: {} in volume: {}", filePath, volumeUuid);
                return null;
            }
            logger.error("getFileInfo: Failed to get file info: {} in volume: {}", filePath, volumeUuid, e);
            throw new CloudRuntimeException("Failed to get file info: " + e.getMessage());
        } catch (Exception e){
            logger.error("getFileInfo: Exception while getting file info: {} in volume: {}", filePath, volumeUuid, e);
            throw new CloudRuntimeException("Failed to get file info: " + e.getMessage());
        }
    }

    private boolean updateFile(String volumeUuid, String filePath, FileInfo fileInfo) {
        logger.info("updateFile: Updating file: {} in volume: {}", filePath, volumeUuid);
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            nasFeignClient.updateFile( authHeader, volumeUuid, filePath, fileInfo);
            logger.info("updateFile: File updated successfully: {} in volume: {}", filePath, volumeUuid);
            return true;
        } catch (FeignException e) {
            logger.error("updateFile: Failed to update file: {} in volume: {}", filePath, volumeUuid, e);
            return false;
        } catch (Exception e){
            logger.error("updateFile: Exception while updating file: {} in volume: {}", filePath, volumeUuid, e);
            return false;
        }
    }


    private ExportPolicy createExportPolicyRequest(AccessGroup accessGroup,String svmName , String volumeName){

        String exportPolicyName = OntapStorageUtils.generateExportPolicyName(svmName,volumeName);
        ExportPolicy exportPolicy = new ExportPolicy();

        List<ExportRule> rules = new ArrayList<>();
        ExportRule exportRule = new ExportRule();

        List<ExportRule.ExportClient> exportClients = new ArrayList<>();
        List<HostVO> hosts = accessGroup.getHostsToConnect();
        for (HostVO host : hosts) {
            String hostStorageIp = host.getStorageIpAddress();
            String ip = (hostStorageIp != null && !hostStorageIp.isEmpty())
                    ? hostStorageIp
                    : host.getPrivateIpAddress();
            String ipToUse = ip + "/32";
            ExportRule.ExportClient exportClient = new ExportRule.ExportClient();
            exportClient.setMatch(ipToUse);
            exportClients.add(exportClient);
        }
        exportRule.setClients(exportClients);
        exportRule.setProtocols(List.of(ExportRule.ProtocolsEnum.NFS3));
        exportRule.setRoRule(List.of("sys"));
        exportRule.setRwRule(List.of("sys"));
        exportRule.setSuperuser(List.of("sys"));
        rules.add(exportRule);

        Svm svm = new Svm();
        svm.setName(svmName);
        exportPolicy.setSvm(svm);
        exportPolicy.setRules(rules);
        exportPolicy.setName(exportPolicyName);

        return exportPolicy;
    }

    private String updateCloudStackVolumeMetadata(String dataStoreId, DataObject volumeInfo) {
        logger.info("updateCloudStackVolumeMetadata called with datastoreID: {} volumeInfo: {} ", dataStoreId, volumeInfo );
       try {
           VolumeObject volumeObject = (VolumeObject) volumeInfo;
           long volumeId = volumeObject.getId();
           logger.info("updateCloudStackVolumeMetadata: VolumeInfo ID from VolumeObject: {}", volumeId);
           VolumeVO volume = volumeDao.findById(volumeId);
           if (volume == null) {
               throw new CloudRuntimeException("Volume not found with id: " + volumeId);
           }
           String volumeUuid = volumeInfo.getUuid();
           volume.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
           volume.setPoolId(Long.parseLong(dataStoreId));
           volume.setPath(volumeUuid);  // Filename for qcow2 file
           volumeDao.update(volume.getId(), volume);
           logger.info("Updated volume path to {} for volume ID {}", volumeUuid, volumeId);
           return volumeUuid;
       }catch (Exception e){
           logger.error("updateCloudStackVolumeMetadata: Exception while updating volumeInfo: {} in volume: {}", dataStoreId, volumeInfo.getUuid(), e);
           throw new CloudRuntimeException("Exception while updating volumeInfo: " + e.getMessage());
       }
    }

    private Answer createVolumeOnKVMHost(DataObject volumeInfo) {
        logger.info("createVolumeOnKVMHost called with volumeInfo: {} ", volumeInfo);

        try {
            logger.info("createVolumeOnKVMHost: Sending CreateObjectCommand to KVM agent for volume: {}", volumeInfo.getUuid());
            CreateObjectCommand cmd = new CreateObjectCommand(volumeInfo.getTO());
            EndPoint ep = epSelector.select(volumeInfo);
            if (ep == null) {
                String errMsg = "No remote endpoint to send CreateObjectCommand, check if host is up";
                logger.error(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            logger.info("createVolumeOnKVMHost: Sending command to endpoint: {}", ep.getHostAddr());
            Answer answer = ep.sendMessage(cmd);
            if (answer != null && answer.getResult()) {
                logger.info("createVolumeOnKVMHost: Successfully created qcow2 file on KVM host");
            } else {
                logger.error("createVolumeOnKVMHost: Failed to create qcow2 file: {}",
                        answer != null ? answer.getDetails() : "null answer");
            }
            return answer;
        } catch (Exception e) {
            logger.error("createVolumeOnKVMHost: Exception sending CreateObjectCommand", e);
            return new Answer(null, false, e.toString());
        }
    }

    private Answer deleteVolumeOnKVMHost(DataObject volumeInfo) {
        logger.info("deleteVolumeOnKVMHost called with volumeInfo: {} ", volumeInfo);

        try {
            logger.info("deleteVolumeOnKVMHost: Sending DeleteCommand to KVM agent for volume: {}", volumeInfo.getUuid());
            DeleteCommand cmd = new DeleteCommand(volumeInfo.getTO());
            EndPoint ep = epSelector.select(volumeInfo);
            if (ep == null) {
                String errMsg = "No remote endpoint to send DeleteCommand, check if host is up";
                logger.error(errMsg);
                return new Answer(cmd, false, errMsg);
            }
            logger.info("deleteVolumeOnKVMHost: Sending command to endpoint: {}", ep.getHostAddr());
            Answer answer = ep.sendMessage(cmd);
            if (answer != null && answer.getResult()) {
                logger.info("deleteVolumeOnKVMHost: Successfully deleted qcow2 file on KVM host");
            } else {
                logger.error("deleteVolumeOnKVMHost: Failed to delete qcow2 file: {}",
                        answer != null ? answer.getDetails() : "null answer");
            }
            return answer;
        } catch (Exception e) {
            logger.error("deleteVolumeOnKVMHost: Exception sending DeleteCommand", e);
            return new Answer(null, false, e.toString());
        }
    }

    private FileInfo getFile(String volumeUuid, String filePath) {
        logger.info("Get File: {} for volume: {}", filePath, volumeUuid);

        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
        OntapResponse<FileInfo> fileResponse = null;
        try {
            fileResponse = nasFeignClient.getFileResponse(authHeader, volumeUuid, filePath);
            if (fileResponse == null || fileResponse.getRecords().isEmpty()) {
                throw new CloudRuntimeException("File " + filePath + " not found on ONTAP. " +
                        "Received successful response but file does not exist.");
            }
        } catch (FeignException e) {
            logger.error("getFile: Failed to get file response: " + filePath, e);
            throw new CloudRuntimeException("File not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("getFile: Exception to get file: {}", filePath, e);
            throw new CloudRuntimeException("Failed to get the file: " + e.getMessage());
        }
        logger.info("getFile: File retrieved successfully with name {}", filePath);
        return fileResponse.getRecords().get(0);
    }

    /**
     * Reverts a file to a snapshot using the ONTAP CLI-based snapshot file restore API.
     *
     * <p>ONTAP REST API (CLI passthrough):
     * {@code POST /api/private/cli/volume/snapshot/restore-file}</p>
     *
     * <p>This method uses the CLI native API which is more reliable and works
     * consistently for both NFS files and iSCSI LUNs.</p>
     *
     * @param snapshotName  The ONTAP FlexVolume snapshot name
     * @param flexVolUuid   The FlexVolume UUID (not used in CLI API, kept for interface consistency)
     * @param snapshotUuid  The ONTAP snapshot UUID (not used in CLI API, kept for interface consistency)
     * @param volumePath    The file path within the FlexVolume
     * @param lunUuid       Not used for NFS (null)
     * @param flexVolName   The FlexVolume name (required for CLI API)
     * @return JobResponse for the async restore operation
     */
    @Override
    public JobResponse revertSnapshotForCloudStackVolume(String snapshotName, String flexVolUuid,
                                                          String snapshotUuid, String volumePath,
                                                          String lunUuid, String flexVolName) {
        logger.info("revertSnapshotForCloudStackVolume [NFS]: Restoring file [{}] from snapshot [{}] on FlexVol [{}]",
                volumePath, snapshotName, flexVolName);

        if (snapshotName == null || snapshotName.isEmpty()) {
            throw new CloudRuntimeException("Snapshot name is required for NFS snapshot revert");
        }
        if (volumePath == null || volumePath.isEmpty()) {
            throw new CloudRuntimeException("File path is required for NFS snapshot revert");
        }
        if (flexVolName == null || flexVolName.isEmpty()) {
            throw new CloudRuntimeException("FlexVolume name is required for NFS snapshot revert");
        }

        String authHeader = getAuthHeader();
        String svmName = storage.getSvmName();

        // Prepare the file path for ONTAP CLI API (ensure it starts with "/")
        String ontapFilePath = volumePath.startsWith("/") ? volumePath : "/" + volumePath;

        // Create CLI snapshot restore request
        CliSnapshotRestoreRequest restoreRequest = new CliSnapshotRestoreRequest(
                svmName, flexVolName, snapshotName, ontapFilePath);

        logger.info("revertSnapshotForCloudStackVolume: Calling CLI file restore API with vserver={}, volume={}, snapshot={}, path={}",
                svmName, flexVolName, snapshotName, ontapFilePath);

        return getSnapshotFeignClient().restoreFileFromSnapshotCli(authHeader, restoreRequest);
    }
}
