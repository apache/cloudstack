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

import com.cloud.utils.exception.CloudRuntimeException;
import feign.FeignException;
import org.apache.cloudstack.storage.feign.FeignClientFactory;
import org.apache.cloudstack.storage.feign.client.AggregateFeignClient;
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.NetworkFeignClient;
import org.apache.cloudstack.storage.feign.client.NASFeignClient;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.model.Aggregate;
import org.apache.cloudstack.storage.feign.model.IpInterface;
import org.apache.cloudstack.storage.feign.model.IscsiService;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.Nas;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Storage Strategy represents the communication path for all the ONTAP storage options
 *
 * ONTAP storage operation would vary based on
 *      Supported protocols: NFS3.0, NFS4.1, FC, iSCSI, Nvme/TCP and Nvme/FC
 *      Supported platform:  Unified and Disaggregated
 */
public abstract class StorageStrategy {
    // Replace @Inject Feign clients with FeignClientFactory
    protected FeignClientFactory feignClientFactory;
    protected AggregateFeignClient aggregateFeignClient;
    protected VolumeFeignClient volumeFeignClient;
    protected SvmFeignClient svmFeignClient;
    protected JobFeignClient jobFeignClient;
    protected NetworkFeignClient networkFeignClient;
    protected SANFeignClient sanFeignClient;
    protected NASFeignClient nasFeignClient;
    protected SnapshotFeignClient snapshotFeignClient;

    protected OntapStorage storage;

    /**
     * Presents aggregate object for the unified storage, not eligible for disaggregated
     */
    private List<Aggregate> aggregates;

    private static final Logger logger = LogManager.getLogger(StorageStrategy.class);

    public StorageStrategy(OntapStorage ontapStorage) {
        storage = ontapStorage;
        String baseURL = OntapStorageConstants.HTTPS + storage.getStorageIP();
        logger.info("Initializing StorageStrategy with base URL: " + baseURL);
        // Initialize FeignClientFactory and create clients
        this.feignClientFactory = new FeignClientFactory();
        this.aggregateFeignClient = feignClientFactory.createClient(AggregateFeignClient.class, baseURL);
        this.volumeFeignClient = feignClientFactory.createClient(VolumeFeignClient.class, baseURL);
        this.svmFeignClient = feignClientFactory.createClient(SvmFeignClient.class, baseURL);
        this.jobFeignClient = feignClientFactory.createClient(JobFeignClient.class, baseURL);
        this.networkFeignClient = feignClientFactory.createClient(NetworkFeignClient.class, baseURL);
        this.sanFeignClient = feignClientFactory.createClient(SANFeignClient.class, baseURL);
        this.nasFeignClient = feignClientFactory.createClient(NASFeignClient.class, baseURL);
        this.snapshotFeignClient = feignClientFactory.createClient(SnapshotFeignClient.class, baseURL);
    }

    // Connect method to validate ONTAP cluster, credentials, protocol, and SVM
    public boolean connect() {
        logger.info("Attempting to connect to ONTAP cluster at " + storage.getStorageIP() + " and validate SVM " +
                storage.getSvmName() + ", protocol " + storage.getProtocol());
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        String svmName = storage.getSvmName();
        try {
            // Call the SVM API to check if the SVM exists
            Svm svm = new Svm();
            logger.info("Fetching the SVM details...");
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.NAME, svmName, OntapStorageConstants.FIELDS, OntapStorageConstants.AGGREGATES +
                    OntapStorageConstants.COMMA + OntapStorageConstants.STATE);
            OntapResponse<Svm> svms = svmFeignClient.getSvmResponse(queryParams, authHeader);
            if (svms != null && svms.getRecords() != null && !svms.getRecords().isEmpty()) {
                svm = svms.getRecords().get(0);
            } else {
                logger.error("No SVM found on the ONTAP cluster by the name" + svmName + ".");
                return false;
            }

            logger.info("Validating SVM state and protocol settings...");
            if (!Objects.equals(svm.getState(), OntapStorageConstants.RUNNING)) {
                logger.error("SVM " + svmName + " is not in running state.");
                return false;
            }
            if (Objects.equals(storage.getProtocol(), OntapStorageConstants.NFS) && !svm.getNfsEnabled()) {
                logger.error("NFS protocol is not enabled on SVM " + svmName);
                return false;
            } else if (Objects.equals(storage.getProtocol(), OntapStorageConstants.ISCSI) && !svm.getIscsiEnabled()) {
                logger.error("iSCSI protocol is not enabled on SVM " + svmName);
                return false;
            }
            List<Aggregate> aggrs = svm.getAggregates();
            if (aggrs == null || aggrs.isEmpty()) {
                logger.error("No aggregates are assigned to SVM " + svmName);
                return false;
            }
            for (Aggregate aggr : aggrs) {
                logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
                Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid());
                if (aggrResp == null) {
                    logger.warn("Aggregate details response is null for aggregate " + aggr.getName() + ". Skipping.");
                    break;
                }
                if (!Objects.equals(aggrResp.getState(), Aggregate.StateEnum.ONLINE)) {
                    logger.warn("Aggregate " + aggr.getName() + " is not in online state. Skipping this aggregate.");
                    continue;
                } else if (aggrResp.getSpace() == null || aggrResp.getAvailableBlockStorageSpace() == null ||
                        aggrResp.getAvailableBlockStorageSpace() <= storage.getSize().doubleValue()) {
                    logger.warn("Aggregate " + aggr.getName() + " does not have sufficient available space. Skipping this aggregate.");
                    continue;
                }
                logger.info("Selected aggregate: " + aggr.getName() + " for volume operations.");
                this.aggregates = List.of(aggr);
                break;
            }
            if (this.aggregates == null || this.aggregates.isEmpty()) {
                logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
                return false;
            }

            logger.info("Successfully connected to ONTAP cluster and validated ONTAP details provided");
        } catch (Exception e) {
            logger.error("Failed to connect to ONTAP cluster: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    // Common methods like create/delete etc., should be here

    /**
     * Creates ONTAP Flex-Volume
     * Eligible only for Unified ONTAP storage
     * throw exception in case of disaggregated ONTAP storage
     *
     * @param volumeName the name of the volume to create
     * @param size the size of the volume in bytes
     * @return the created Volume object
     */
    public Volume createStorageVolume(String volumeName, Long size) {
        logger.info("Creating volume: " + volumeName + " of size: " + size + " bytes");

        String svmName = storage.getSvmName();
        if (aggregates == null || aggregates.isEmpty()) {
            logger.error("No aggregates available to create volume on SVM " + svmName);
            throw new CloudRuntimeException("No aggregates available to create volume on SVM " + svmName);
        }
        if (size == null || size <= 0) {
            throw new CloudRuntimeException("Invalid volume size provided: " + size);
        }

        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());

        // Generate the Create Volume Request
        Volume volumeRequest = new Volume();
        Svm svm = new Svm();
        svm.setName(svmName);
        Nas nas = new Nas();
        nas.setPath(OntapStorageConstants.SLASH + volumeName);

        volumeRequest.setName(volumeName);
        volumeRequest.setSvm(svm);

        // Pick the best aggregate for this specific request (largest available, online, and sufficient space).
        long maxAvailableAggregateSpaceBytes = -1L;
        Aggregate aggrChosen = null;
        for (Aggregate aggr : aggregates) {
            logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
            Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid());

            if (aggrResp == null) {
                logger.warn("Aggregate details response is null for aggregate " + aggr.getName() + ". Skipping.");
                break;
            }

            if (!Objects.equals(aggrResp.getState(), Aggregate.StateEnum.ONLINE)) {
                logger.warn("Aggregate " + aggr.getName() + " is not in online state. Skipping this aggregate.");
                continue;
            }

            if (aggrResp.getSpace() == null || aggrResp.getAvailableBlockStorageSpace() == null) {
                logger.warn("Aggregate " + aggr.getName() + " does not have space information. Skipping this aggregate.");
                continue;
            }

            final long availableBytes = aggrResp.getAvailableBlockStorageSpace().longValue();
            logger.debug("Aggregate " + aggr.getName() + " available bytes=" + availableBytes + ", requested=" + size);

            if (availableBytes <= size) {
                logger.warn("Aggregate " + aggr.getName() + " does not have sufficient available space. Required=" +
                        size + " bytes, available=" + availableBytes + " bytes. Skipping this aggregate.");
                continue;
            }

            if (availableBytes > maxAvailableAggregateSpaceBytes) {
                maxAvailableAggregateSpaceBytes = availableBytes;
                aggrChosen = aggr;
            }
        }

        if (aggrChosen == null) {
            logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
            throw new CloudRuntimeException("No suitable aggregates found on SVM " + svmName + " for volume operations.");
        }
        logger.info("Selected aggregate: " + aggrChosen.getName() + " for volume operations.");

        Aggregate aggr = new Aggregate();
        aggr.setName(aggrChosen.getName());
        aggr.setUuid(aggrChosen.getUuid());
        volumeRequest.setAggregates(List.of(aggr));
        volumeRequest.setSize(size);
        volumeRequest.setNas(nas);
        try {
            JobResponse jobResponse = volumeFeignClient.createVolumeWithJob(authHeader, volumeRequest);
            if (jobResponse == null || jobResponse.getJob() == null) {
                throw new CloudRuntimeException("Failed to initiate volume creation for " + volumeName);
            }
            String jobUUID = jobResponse.getJob().getUuid();

            Boolean jobSucceeded = jobPollForSuccess(jobUUID,10, 1);
            if (!jobSucceeded) {
                logger.error("Volume creation job failed for volume: " + volumeName);
                throw new CloudRuntimeException("Volume creation job failed for volume: " + volumeName);
            }
            logger.info("Volume creation job completed successfully for volume: " + volumeName);
        } catch (Exception e) {
            logger.error("Exception while creating volume: ", e);
            throw new CloudRuntimeException("Failed to create volume: " + e.getMessage());
        }
        // Verify if the Volume has been created and set the Volume object
        // Call the VolumeFeignClient to get the created volume details
        OntapResponse<Volume> volumesResponse = volumeFeignClient.getAllVolumes(authHeader, Map.of(OntapStorageConstants.NAME, volumeName));
        if (volumesResponse == null || volumesResponse.getRecords() == null || volumesResponse.getRecords().isEmpty()) {
            logger.error("Volume " + volumeName + " not found after creation.");
            throw new CloudRuntimeException("Volume " + volumeName + " not found after creation.");
        }
        Volume createdVolume = volumesResponse.getRecords().get(0);
        if (createdVolume == null) {
            logger.error("Failed to retrieve details of the created volume " + volumeName);
            throw new CloudRuntimeException("Failed to retrieve details of the created volume " + volumeName);
        } else if (createdVolume.getName() == null || !createdVolume.getName().equals(volumeName)) {
            logger.error("Mismatch in created volume name. Expected: " + volumeName + ", Found: " + createdVolume.getName());
            throw new CloudRuntimeException("Mismatch in created volume name. Expected: " + volumeName + ", Found: " + createdVolume.getName());
        }
        logger.info("Volume created successfully: " + volumeName);
        try {
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.NAME, volumeName);
            logger.debug("Fetching volume details for: " + volumeName);

            OntapResponse<Volume> ontapVolume = volumeFeignClient.getVolume(authHeader, queryParams);
            logger.debug("Feign call completed. Processing response...");

            if (ontapVolume == null) {
                logger.error("OntapResponse is null for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": Response is null");
            }
            logger.debug("OntapResponse is not null. Checking records field...");

            if (ontapVolume.getRecords() == null) {
                logger.error("OntapResponse.records is null for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": Records list is null");
            }
            logger.debug("Records field is not null. Size: " + ontapVolume.getRecords().size());

            if (ontapVolume.getRecords().isEmpty()) {
                logger.error("OntapResponse.records is empty for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": No records found");
            }

            Volume volume = ontapVolume.getRecords().get(0);
            logger.info("Volume retrieved successfully: " + volumeName + ", UUID: " + volume.getUuid());
            return volume;
        } catch (Exception e) {
            logger.error("Exception while retrieving volume details for: " + volumeName, e);
            throw new CloudRuntimeException("Failed to fetch volume: " + volumeName + ". Error: " + e.getMessage(), e);
        }
    }

     /**
     * Updates ONTAP Flex-Volume
     * Eligible only for Unified ONTAP storage
     * throw exception in case of disaggregated ONTAP storage
     *
     * @param volume the volume to update
     * @return the updated Volume object
     */
    public Volume updateStorageVolume(Volume volume) {
        return null;
    }

    /**
     * Delete ONTAP Flex-Volume
     * Eligible only for Unified ONTAP storage
     * throw exception in case of disaggregated ONTAP storage
     *
     * @param volume the volume to delete
     */
    public void deleteStorageVolume(Volume volume) {
        logger.info("Deleting ONTAP volume by name: " + volume.getName() + " and uuid: " + volume.getUuid());
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            // TODO: Implement lun and file deletion, if any, before deleting the volume
            JobResponse jobResponse = volumeFeignClient.deleteVolume(authHeader, volume.getUuid());
            Boolean jobSucceeded = jobPollForSuccess(jobResponse.getJob().getUuid(),10, 1);
            if (!jobSucceeded) {
                logger.error("Volume deletion job failed for volume: " + volume.getName());
                throw new CloudRuntimeException("Volume deletion job failed for volume: " + volume.getName());
            }
            logger.info("Volume deleted successfully: " + volume.getName());
        } catch (FeignException.FeignClientException e) {
            logger.error("Exception while deleting volume: ", e);
            throw new CloudRuntimeException("Failed to delete volume: " + e.getMessage());
        }
        logger.info("ONTAP volume deletion process completed for volume: " + volume.getName());
    }

    /**
     * Gets ONTAP Flex-Volume
     * Eligible only for Unified ONTAP storage
     * throw exception in case of disaggregated ONTAP storage
     *
     * @param volume the volume to retrieve
     * @return the retrieved Volume object
     */
    public Volume getStorageVolume(Volume volume) {
        return null;
    }

    /**
     * Get the storage path based on protocol.
     * For iSCSI: Returns the iSCSI target IQN (e.g., iqn.1992-08.com.netapp:sn.xxx:vs.3)
     * For NFS: Returns the mount path (to be implemented)
     *
     * @return the storage path as a String
     */
    public String getStoragePath() {
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        String targetIqn = null;
        try {
            if (storage.getProtocol() == ProtocolType.ISCSI) {
                logger.info("Fetching iSCSI target IQN for SVM: {}", storage.getSvmName());

                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put(OntapStorageConstants.SVM_DOT_NAME, storage.getSvmName());
                queryParams.put("fields", "enabled,target");
                queryParams.put("max_records", "1");

                OntapResponse<IscsiService> response = sanFeignClient.getIscsiServices(authHeader, queryParams);

                if (response == null || response.getRecords() == null || response.getRecords().isEmpty()) {
                    throw new CloudRuntimeException("No iSCSI service found for SVM: " + storage.getSvmName());
                }

                IscsiService iscsiService = response.getRecords().get(0);

                if (iscsiService.getTarget() == null || iscsiService.getTarget().getName() == null) {
                    throw new CloudRuntimeException("iSCSI target IQN not found for SVM: " + storage.getSvmName());
                }

                targetIqn = iscsiService.getTarget().getName();
                logger.info("Retrieved iSCSI target IQN: {}", targetIqn);
                return targetIqn;

            } else if (storage.getProtocol() == ProtocolType.NFS3) {
                // TODO: Implement NFS path retrieval logic
            } else {
                throw new CloudRuntimeException("Unsupported protocol for path retrieval: " + storage.getProtocol());
            }

        } catch (FeignException.FeignClientException e) {
            logger.error("Exception while retrieving storage path for protocol {}: {}", storage.getProtocol(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to retrieve storage path: " + e.getMessage());
        }
        return targetIqn;
    }



    /**
     * Get the network ip interface
     *
     * @return the network interface ip as a String
     */

    public String getNetworkInterface() {
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put(OntapStorageConstants.SVM_DOT_NAME, storage.getSvmName());
            if (storage.getProtocol() != null) {
                switch (storage.getProtocol()) {
                    case NFS3:
                        queryParams.put(OntapStorageConstants.SERVICES, OntapStorageConstants.DATA_NFS);
                        break;
                    case ISCSI:
                        queryParams.put(OntapStorageConstants.SERVICES, OntapStorageConstants.DATA_ISCSI);
                        break;
                    default:
                        logger.error("Unsupported protocol: " + storage.getProtocol());
                        throw new CloudRuntimeException("Unsupported protocol: " + storage.getProtocol());
                }
            }
            queryParams.put(OntapStorageConstants.FIELDS, OntapStorageConstants.IP_ADDRESS);
            queryParams.put(OntapStorageConstants.RETURN_RECORDS, OntapStorageConstants.TRUE);
            OntapResponse<IpInterface> response =
                    networkFeignClient.getNetworkIpInterfaces(authHeader, queryParams);
            if (response != null && response.getRecords() != null && !response.getRecords().isEmpty()) {
                IpInterface ipInterface = null;
                // For simplicity, return the first interface's name (Of IPv4 type for NFS3)
                if (storage.getProtocol() == ProtocolType.ISCSI) {
                    ipInterface = response.getRecords().get(0);
                } else if (storage.getProtocol() == ProtocolType.NFS3) {
                    for (IpInterface iface : response.getRecords()) {
                        if (iface.getIp().getAddress().contains(".")) {
                            ipInterface = iface;
                            break;
                        }
                    }
                }

                logger.info("Retrieved network interface: " + ipInterface.getIp().getAddress());
                return ipInterface.getIp().getAddress();
            } else {
                throw new CloudRuntimeException("No network interfaces found for SVM " + storage.getSvmName() +
                        " for protocol " + storage.getProtocol());
            }
        } catch (FeignException.FeignClientException e) {
            logger.error("Exception while retrieving network interfaces: ", e);
            throw new CloudRuntimeException("Failed to retrieve network interfaces: " + e.getMessage());
        }
    }

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses.
     * it is going to mimic
     * createLun       for iSCSI, FC protocols
     * createFile      for NFS3.0 and NFS4.1 protocols
     * createNameSpace for Nvme/TCP and Nvme/FC protocol
     *
     * @param cloudstackVolume the CloudStack volume to create
     * @return the created CloudStackVolume object
     */
    abstract public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses.
     * it is going to mimic
     * updateLun       for iSCSI, FC protocols
     * updateFile      for NFS3.0 and NFS4.1 protocols
     * updateNameSpace for Nvme/TCP and Nvme/FC protocol
     *
     * @param cloudstackVolume the CloudStack volume to update
     * @return the updated CloudStackVolume object
     */
    abstract CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses.
     * it is going to mimic
     * deleteLun       for iSCSI, FC protocols
     * deleteFile      for NFS3.0 and NFS4.1 protocols
     * deleteNameSpace for Nvme/TCP and Nvme/FC protocol
     *
     * @param cloudstackVolume the CloudStack volume to delete
     */
    abstract public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses.
     * it is going to mimic
     *     cloneLun       for iSCSI, FC protocols
     *     cloneFile      for NFS3.0 and NFS4.1 protocols
     *     cloneNameSpace for Nvme/TCP and Nvme/FC protocol
     * @param cloudstackVolume the CloudStack volume to copy
     */
    abstract public void copyCloudStackVolume(CloudStackVolume cloudstackVolume);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses.
     * it is going to mimic
     *     getLun       for iSCSI, FC protocols
     *     getFile      for NFS3.0 and NFS4.1 protocols
     *     getNameSpace for Nvme/TCP and Nvme/FC protocol
     * @param cloudStackVolumeMap the CloudStack volume to retrieve
     * @return the retrieved CloudStackVolume object
     */
    abstract public CloudStackVolume getCloudStackVolume(Map<String, String> cloudStackVolumeMap);

    /**
     * Reverts a CloudStack volume to a snapshot using protocol-specific ONTAP APIs.
     *
     * <p>This method encapsulates the snapshot revert behavior based on protocol:</p>
     * <ul>
     *   <li><b>iSCSI/FC:</b> Uses {@code POST /api/storage/luns/{lun.uuid}/restore}
     *       to restore LUN data from the FlexVolume snapshot.</li>
     *   <li><b>NFS:</b> Uses {@code POST /api/storage/volumes/{vol.uuid}/snapshots/{snap.uuid}/files/{path}/restore}
     *       to restore a single file from the FlexVolume snapshot.</li>
     * </ul>
     *
     * @param snapshotName     The ONTAP FlexVolume snapshot name
     * @param flexVolUuid      The FlexVolume UUID containing the snapshot
     * @param snapshotUuid     The ONTAP snapshot UUID (used for NFS file restore)
     * @param volumePath       The path of the file/LUN within the FlexVolume
     * @param lunUuid          The LUN UUID (only for iSCSI, null for NFS)
     * @param flexVolName      The FlexVolume name (only for iSCSI, for constructing destination path)
     * @return JobResponse for the async restore operation
     */
    public abstract JobResponse revertSnapshotForCloudStackVolume(String snapshotName, String flexVolUuid,
                                                                   String snapshotUuid, String volumePath,
                                                                   String lunUuid, String flexVolName);


    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     createiGroup       for iSCSI and FC protocols
     *     createExportPolicy for NFS 3.0 and NFS 4.1 protocols
     *     createSubsystem    for Nvme/TCP and Nvme/FC protocols
     * @param accessGroup the access group to create
     * @return the created AccessGroup object
     */
    abstract public AccessGroup createAccessGroup(AccessGroup accessGroup);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     deleteiGroup       for iSCSI and FC protocols
     *     deleteExportPolicy for NFS 3.0 and NFS 4.1 protocols
     *     deleteSubsystem    for Nvme/TCP and Nvme/FC protocols
     * @param accessGroup the access group to delete
     */
    abstract public void deleteAccessGroup(AccessGroup accessGroup);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     updateiGroup       example add/remove-Iqn   for iSCSI and FC protocols
     *     updateExportPolicy example add/remove-Rule for NFS 3.0 and NFS 4.1 protocols
     *     //TODO  for Nvme/TCP and Nvme/FC protocols
     * @param accessGroup the access group to update
     * @return the updated AccessGroup object
     */
    abstract AccessGroup updateAccessGroup(AccessGroup accessGroup);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     e.g., getIGroup for iSCSI and FC protocols
     *     e.g., getExportPolicy for NFS 3.0 and NFS 4.1 protocols
     *     //TODO  for Nvme/TCP and Nvme/FC protocols
      * @param values map to get access group values like name, svm name etc.
     */
    abstract public AccessGroup getAccessGroup(Map<String, String> values);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     lunMap  for iSCSI and FC protocols
     *     //TODO  for NFS 3.0 and NFS 4.1 protocols (e.g., export rule management)
     *     //TODO  for Nvme/TCP and Nvme/FC protocols
     * @param values map including SVM name, LUN name, and igroup name (for SAN) or equivalent for NAS
     * @return map containing logical unit number for the new/existing mapping (SAN) or relevant info for NAS
     */
    abstract public Map<String,String> enableLogicalAccess(Map<String,String> values);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     lunUnmap  for iSCSI and FC protocols
     * @param values map including LUN UUID and iGroup UUID (for SAN) or equivalent for NAS
     */
    abstract public void disableLogicalAccess(Map<String, String> values);

    /**
     * Method encapsulates the behavior based on the opted protocol in subclasses
     *     lunMap lookup for iSCSI/FC protocols (GET-only, no side-effects)
     * @param values map with SVM name, LUN name, and igroup name (for SAN) or equivalent for NAS
     * @return map containing logical unit number if mapping exists; otherwise null
     */
    abstract public Map<String, String> getLogicalAccess(Map<String, String> values);

    // ── FlexVolume Snapshot accessors ────────────────────────────────────────

    /**
     * Returns the {@link SnapshotFeignClient} for ONTAP FlexVolume snapshot operations.
     */
    public SnapshotFeignClient getSnapshotFeignClient() {
        return snapshotFeignClient;
    }

    /**
     * Returns the {@link NASFeignClient} for ONTAP NAS file operations
     * (including file clone for single-file SnapRestore).
     */
    public NASFeignClient getNasFeignClient() {
        return nasFeignClient;
    }

    /**
     * Generates the Basic-auth header for ONTAP REST calls.
     */
    public String getAuthHeader() {
        return OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
    }

    /**
     * Polls an ONTAP async job for successful completion.
     *
     * @param jobUUID          UUID of the ONTAP job to poll
     * @param maxRetries       maximum number of poll attempts
     * @param sleepTimeInSecs  seconds to sleep between poll attempts
     * @return true if the job completed successfully
     */
    public Boolean jobPollForSuccess(String jobUUID, int maxRetries, int sleepTimeInSecs) {
        //Create URI for GET Job API
        int jobRetryCount = 0;
        Job jobResp = null;
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            while (jobResp == null || !jobResp.getState().equals(OntapStorageConstants.JOB_SUCCESS)) {
                if (jobRetryCount >= maxRetries) {
                    logger.error("Job did not complete within expected time.");
                    throw new CloudRuntimeException("Job did not complete within expected time.");
                }

                try {
                    jobResp = jobFeignClient.getJobByUUID(authHeader, jobUUID);
                    if (jobResp == null) {
                        logger.warn("Job with UUID " + jobUUID + " not found. Retrying...");
                    } else if (jobResp.getState().equals(OntapStorageConstants.JOB_FAILURE)) {
                        throw new CloudRuntimeException("Job failed with error: " + jobResp.getMessage());
                    }
                } catch (FeignException.FeignClientException e) {
                    throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
                }

                jobRetryCount++;
                Thread.sleep(OntapStorageConstants.CREATE_VOLUME_CHECK_SLEEP_TIME);
            }
            if (jobResp == null || !jobResp.getState().equals(OntapStorageConstants.JOB_SUCCESS)) {
                return false;
            }
        } catch (FeignException.FeignClientException e) {
            throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
