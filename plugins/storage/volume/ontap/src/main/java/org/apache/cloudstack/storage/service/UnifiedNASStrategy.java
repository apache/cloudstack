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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.CliSnapshotRestoreRequest;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.ExportRule;
import org.apache.cloudstack.storage.feign.model.FileCloneRequest;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.Nas;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.host.HostVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;

import feign.FeignException;

public class UnifiedNASStrategy extends NASStrategy {
    private static final Logger logger = LogManager.getLogger(UnifiedNASStrategy.class);
    @Inject private VolumeDao volumeDao;
    @Inject private EndPointSelector epSelector;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private PrimaryDataStoreDao primaryDataStoreDao;

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
            updateCloudStackVolumeMetadata(cloudstackVolume.getDatastoreId(), cloudstackVolume.getVolumeInfo());
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
        } catch (Exception e) {
            logger.error("deleteCloudStackVolume: error occured " + e);
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Clones a file inside the FlexVolume using ONTAP's file clone API.
     *
     * <p>The source is taken from {@code file.path} and the destination from
     * {@code destinationPath}, both relative to the root of the FlexVolume backing the pool.</p>
     */
    @Override
    public CloudStackVolume cloneCloudStackVolume(CloudStackVolume cloudstackVolume) {
        if (cloudstackVolume == null || cloudstackVolume.getFile() == null
                || cloudstackVolume.getFile().getPath() == null || cloudstackVolume.getDestinationPath() == null) {
            logger.error("cloneCloudStackVolume: File clone failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException("Failed to clone file, invalid request");
        }
        if (cloudstackVolume.getDatastoreId() == null) {
            throw new CloudRuntimeException("Failed to clone file, no datastore id in the request");
        }

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(Long.parseLong(cloudstackVolume.getDatastoreId()));
        String svmName = details.get(OntapStorageConstants.SVM_NAME);
        String flexVolName = details.get(OntapStorageConstants.VOLUME_NAME);
        String sourcePath = cloudstackVolume.getFile().getPath();
        String destinationPath = cloudstackVolume.getDestinationPath();

        logger.info("cloneCloudStackVolume: Cloning file [{}] to [{}] in FlexVol [{}]", sourcePath, destinationPath, flexVolName);
        try {
            FileCloneRequest request = new FileCloneRequest(svmName, flexVolName, sourcePath, destinationPath);
            JobResponse jobResponse = nasFeignClient.cloneFile(getAuthHeader(), request);
            pollJobIfPresent(jobResponse, "clone file [" + sourcePath + "] to [" + destinationPath + "]");

            updateCloudStackVolumeMetadata(cloudstackVolume.getDatastoreId(), cloudstackVolume.getVolumeInfo());

            FileInfo clonedFile = new FileInfo();
            clonedFile.setPath(destinationPath);

            CloudStackVolume clonedCloudStackVolume = new CloudStackVolume();
            clonedCloudStackVolume.setFile(clonedFile);
            clonedCloudStackVolume.setDatastoreId(cloudstackVolume.getDatastoreId());
            clonedCloudStackVolume.setVolumeInfo(cloudstackVolume.getVolumeInfo());
            return clonedCloudStackVolume;
        } catch (FeignException e) {
            logger.error("FeignException occurred while cloning file [{}], Status: {}, Exception: {}",
                    sourcePath, e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to clone file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while cloning file [{}], Exception: {}", sourcePath, e.getMessage());
            throw new CloudRuntimeException("Failed to clone file: " + e.getMessage());
        }
    }

    /**
     * Grows the cloned qcow2 to the requested size via a host-side {@code qemu-img resize}.
     */
    @Override
    public void resizeCloudStackVolume(CloudStackVolume cloudstackVolume, long sizeInBytes) {
        if (cloudstackVolume == null || cloudstackVolume.getVolumeInfo() == null) {
            logger.error("resizeCloudStackVolume: Resize failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException("Failed to resize file, invalid request");
        }
        if (sizeInBytes <= 0) {
            throw new CloudRuntimeException("Failed to resize file, invalid size " + sizeInBytes);
        }

        DataObject volumeInfo = cloudstackVolume.getVolumeInfo();
        Answer answer = resizeVolumeOnKVMHost(volumeInfo, sizeInBytes);
        if (answer == null || !answer.getResult()) {
            String errMsg = answer != null ? answer.getDetails() : "Failed to resize qcow2 on KVM host";
            logger.error("resizeCloudStackVolume: " + errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        logger.info("resizeCloudStackVolume: Resized volume [{}] to {} bytes", volumeInfo.getUuid(), sizeInBytes);
    }

    private Answer resizeVolumeOnKVMHost(DataObject volumeInfo, long sizeInBytes) {
        VolumeObject volumeObject = (VolumeObject) volumeInfo;
        VolumeVO volume = volumeDao.findById(volumeObject.getId());
        if (volume == null) {
            throw new CloudRuntimeException("Volume not found with id: " + volumeObject.getId());
        }

        StoragePoolVO storagePool = primaryDataStoreDao.findById(volume.getPoolId());
        if (storagePool == null) {
            throw new CloudRuntimeException("Storage Pool not found for id: " + volume.getPoolId());
        }

        ResizeVolumeCommand cmd = new ResizeVolumeCommand(volume.getPath(), new StorageFilerTO(storagePool),
                volume.getSize(), sizeInBytes, false, null);
        EndPoint ep = epSelector.select(volumeInfo);
        if (ep == null) {
            String errMsg = "No remote endpoint to send ResizeVolumeCommand, check if host is up";
            logger.error(errMsg);
            return new Answer(cmd, false, errMsg);
        }
        logger.info("resizeVolumeOnKVMHost: Sending command to endpoint: {}", ep.getHostAddr());
        return ep.sendMessage(cmd);
    }

    @Override
    public CloudStackVolume getCloudStackVolume(Map<String, String> cloudStackVolumeMap) {
        logger.info("getCloudStackVolume: Get cloudstack volume " + cloudStackVolumeMap);
        CloudStackVolume cloudStackVolume = null;
        FileInfo fileInfo = getFile(cloudStackVolumeMap.get(OntapStorageConstants.VOLUME_UUID),cloudStackVolumeMap.get(OntapStorageConstants.FILE_PATH));

        if (fileInfo != null) {
            cloudStackVolume = new CloudStackVolume();
            cloudStackVolume.setFlexVolumeUuid(cloudStackVolumeMap.get(OntapStorageConstants.VOLUME_UUID));
            cloudStackVolume.setFile(fileInfo);
        }

        return cloudStackVolume;
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup accessGroup) {
        logger.info("createAccessGroup: Create access group {}: " , accessGroup);

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(accessGroup.getStoragePoolId());
        String svmName = details.get(OntapStorageConstants.SVM_NAME);
        String volumeUUID = details.get(OntapStorageConstants.VOLUME_UUID);
        String volumeName = details.get(OntapStorageConstants.VOLUME_NAME);

        // Create the export policy
        ExportPolicy policyRequest = createExportPolicyRequest(accessGroup,svmName,volumeName);
        try {
            ExportPolicy createdPolicy = createExportPolicy(svmName, policyRequest);
            logger.info("createAccessGroup: ExportPolicy created: {}, now attaching this policy to storage pool volume", createdPolicy.getName());
            // attach export policy to volume of storage pool
            assignExportPolicyToVolume(volumeUUID,createdPolicy.getName());
            // save the export policy details in storage pool details
            storagePoolDetailsDao.addDetail(accessGroup.getStoragePoolId(), OntapStorageConstants.EXPORT_POLICY_ID, String.valueOf(createdPolicy.getId()), true);
            storagePoolDetailsDao.addDetail(accessGroup.getStoragePoolId(), OntapStorageConstants.EXPORT_POLICY_NAME, createdPolicy.getName(), true);
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
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Determine export policy attached to the storage pool
            String exportPolicyName = details.get(OntapStorageConstants.EXPORT_POLICY_NAME);
            String exportPolicyId = details.get(OntapStorageConstants.EXPORT_POLICY_ID);

            try {
                nasFeignClient.deleteExportPolicyById(authHeader, exportPolicyId);
                logger.info("deleteAccessGroup: Successfully deleted export policy '{}'", exportPolicyName);
            } catch (FeignException e) {
                if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                    logger.warn("deleteAccessGroup: Export policy '{}' not found in ONTAP, treating as no-op", exportPolicyName);
                    return;
                }
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
        if (accessGroup == null) {
            throw new CloudRuntimeException("Invalid accessGroup object - accessGroup is null");
        }
        // Check if an AccessGroup was constructed without associating it to a storage pool.
        if (accessGroup.getStoragePoolId() == null) {
            throw new CloudRuntimeException("Invalid accessGroup object - storagePoolId is null");
        }
        // At least one host is required regardless of ADD or REMOVE action.
        // An empty list means there is nothing to add to or remove from the export policy client list.
        if (accessGroup.getHostsToConnect() == null || accessGroup.getHostsToConnect().isEmpty()) {
            throw new CloudRuntimeException("Invalid accessGroup object - hostsToConnect is null or empty");
        }

        Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(accessGroup.getStoragePoolId());
        if (details == null || details.isEmpty()) {
            throw new CloudRuntimeException("No storage pool details found for storagePoolId: " + accessGroup.getStoragePoolId());
        }
        String exportPolicyId = details.get(OntapStorageConstants.EXPORT_POLICY_ID);
        if (exportPolicyId == null || exportPolicyId.isEmpty()) {
            throw new CloudRuntimeException("No export policy found for storagePoolId: " + accessGroup.getStoragePoolId());
        }


        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            ExportPolicy existingPolicy = nasFeignClient.getExportPolicyById(authHeader, exportPolicyId);
            // Check if the export policy was deleted externally on ONTAP or the stored ID is stale.
            if (existingPolicy == null) {
                throw new CloudRuntimeException("Failed to fetch existing export policy with id: " + exportPolicyId);
            }

            List<ExportRule> rules = existingPolicy.getRules();
            if (rules == null || rules.isEmpty()) {
                throw new CloudRuntimeException("Export policy " + existingPolicy.getName() +
                        " has no rules — unexpected state, the plugin always creates a rule at pool registration");
            }

            ExportRule targetRule = rules.get(0);

            Set<String> hostMatches = new HashSet<>();
            for (HostVO host : accessGroup.getHostsToConnect()) {
                String hostStorageIp = host.getStorageIpAddress() != null ? host.getStorageIpAddress().trim() : null;
                String ip = (hostStorageIp != null && !hostStorageIp.isEmpty()) ? hostStorageIp
                        : (host.getPrivateIpAddress() != null ? host.getPrivateIpAddress().trim() : null);
                // Occurs when a CloudStack host has neither a storage IP nor a private IP configured
                // (misconfigured or partially registered host). Skip it to avoid inserting a broken
                // or empty match entry into the ONTAP export rule.
                if (ip == null || ip.isEmpty()) {
                    logger.warn("updateAccessGroup: Host {} has no storage/private IP, skipping export rule update", host.getName());
                    continue;
                }
                hostMatches.add(ip + "/32");
            }

            // Occurs when every host in hostsToConnect had no valid IP (all were skipped above).
            // There is nothing to add or remove, so skip the ONTAP API call and return early.
            if (hostMatches.isEmpty()) {
                accessGroup.setPolicy(existingPolicy);
                return accessGroup;
            }

            boolean updated = false;
            // Differentiates between removing hosts (e.g., host decommissioned or removed from the cluster)
            // and the default ADD path (e.g., new host being connected to the storage pool).
            List<ExportRule.ExportClient> exportClients = targetRule.getClients();
            // Existing rules can legitimately have no clients yet; treat that as an empty list.
            if (exportClients == null) {
                exportClients = new ArrayList<>();
                targetRule.setClients(exportClients);
            }

            if (AccessGroup.HostRuleAction.REMOVE.equals(accessGroup.getHostRuleAction())) {
                updated = exportClients.removeIf(c -> c != null && c.getMatch() != null && hostMatches.contains(c.getMatch()));
                // None of the requested host IPs were present in the policy — log for diagnostics
                // so operators can investigate whether the policy state is already correct or stale.
                if (!updated) {
                    logger.info("updateAccessGroup: No matching host IPs found in export policy {} for removal", existingPolicy.getName());
                }
            } else {
                Set<String> existingMatches = new HashSet<>();
                for (ExportRule.ExportClient exportClient : exportClients) {
                    // Skips null client entries or entries with a null match field that may have been
                    // inserted externally on ONTAP. Avoids polluting the dedup set with null values
                    // which would cause subsequent hosts to be incorrectly treated as duplicates.
                    if (exportClient != null && exportClient.getMatch() != null) {
                        existingMatches.add(exportClient.getMatch());
                    }
                }

                for (String match : hostMatches) {
                    // Set.add() returns false when the element was already present, acting as a dedup check.
                    // Prevents inserting a duplicate client match entry for a host that is already allowed
                    // in the export policy — ONTAP may reject or behave unpredictably with duplicate matches.
                    if (existingMatches.add(match)) {
                        ExportRule.ExportClient exportClient = new ExportRule.ExportClient();
                        exportClient.setMatch(match);
                        exportClients.add(exportClient);
                        updated = true;
                    }
                }
            }

            // Occurs when the export policy is already in the desired state:
            // ADD path — all provided host IPs were already present (all were duplicates).
            // REMOVE path — none of the provided host IPs matched any existing entry.
            // In both cases, skip the ONTAP PATCH call to avoid an unnecessary round-trip.
            if (!updated) {
                // Only log the "nothing to add" message for the ADD path; the REMOVE no-op
                // is already logged above in its own branch to avoid double-logging.
                if (!AccessGroup.HostRuleAction.REMOVE.equals(accessGroup.getHostRuleAction())) {
                    logger.info("updateAccessGroup: No new host IPs to add to export policy {}", existingPolicy.getName());
                }
                accessGroup.setPolicy(existingPolicy);
                return accessGroup;
            }

            ExportPolicy updateRequest = new ExportPolicy();
            updateRequest.setRules(rules);
            nasFeignClient.updateExportPolicy(authHeader, exportPolicyId, updateRequest);

            existingPolicy.setRules(rules);
            accessGroup.setPolicy(existingPolicy);
            logger.info("updateAccessGroup: Successfully updated export policy {} with new host client rules", existingPolicy.getName());
            return accessGroup;
        } catch (Exception e) {
            logger.error("updateAccessGroup: Failed to update export policy for pool {}", accessGroup.getStoragePoolId(), e);
            throw new CloudRuntimeException("Failed to update export policy for NFS host connection: " + e.getMessage(), e);
        }
    }

    @Override
    public AccessGroup getAccessGroup(Map<String, String> values) {
        return null;
    }

    @Override
    public String enableLogicalAccess(Map<String, String> values) {
        return null;
    }

    @Override
    public void disableLogicalAccess(Map<String, String> values) {
    }

    @Override
    public String getLogicalAccess(Map<String, String> values) {
        return null;
    }

    private ExportPolicy createExportPolicy(String svmName, ExportPolicy policy) {
        logger.info("createExportPolicy: Creating export policy: {} for SVM: {}", policy, svmName);

        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            nasFeignClient.createExportPolicy(authHeader,  policy);
            OntapResponse<ExportPolicy> policiesResponse = null;
            try {
                Map<String, Object> queryParams = Map.of(OntapStorageConstants.NAME, policy.getName());
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

    private ExportPolicy createExportPolicyRequest(AccessGroup accessGroup,String svmName , String volumeName){

        String exportPolicyName = OntapStorageUtils.generateExportPolicyName(svmName,volumeName);
        ExportPolicy exportPolicy = new ExportPolicy();

        List<ExportRule> rules = new ArrayList<>();
        ExportRule exportRule = new ExportRule();

        List<ExportRule.ExportClient> exportClients = new ArrayList<>();
        List<HostVO> hosts = accessGroup.getHostsToConnect();
        for (HostVO host : hosts) {
            String hostStorageIp = host.getStorageIpAddress() != null ? host.getStorageIpAddress().trim() : null;
            String ip = (hostStorageIp != null && !hostStorageIp.isEmpty())
                    ? hostStorageIp
                    : (host.getPrivateIpAddress() != null ? host.getPrivateIpAddress().trim() : null);
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
        } catch (Exception e){
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

    /**
     * Deletes a file from a FlexVolume, treating an already-absent file as success.
     */
    public void deleteFileByPath(String flexVolUuid, String filePath) {
        logger.info("deleteFileByPath: Deleting file [{}] from FlexVol [{}]", filePath, flexVolUuid);
        try {
            nasFeignClient.deleteFile(getAuthHeader(), flexVolUuid, filePath);
            logger.debug("deleteFileByPath: Deleted file [{}]", filePath);
        } catch (FeignException e) {
            if (e.status() == 404) {
                logger.warn("deleteFileByPath: File [{}] does not exist (status 404), skipping deletion", filePath);
                return;
            }
            logger.error("FeignException occurred while deleting file [{}], Status: {}, Exception: {}",
                    filePath, e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to delete file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while deleting file [{}], Exception: {}", filePath, e.getMessage());
            throw new CloudRuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    private FileInfo getFile(String volumeUuid, String filePath) {
        logger.info("Get File: {} for volume: {}", filePath, volumeUuid);

        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
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
