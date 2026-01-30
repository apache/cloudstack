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
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
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
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class StorageStrategy {
    private final FeignClientFactory feignClientFactory;
    private final AggregateFeignClient aggregateFeignClient;
    private final VolumeFeignClient volumeFeignClient;
    private final SvmFeignClient svmFeignClient;
    private final JobFeignClient jobFeignClient;
    private final NetworkFeignClient networkFeignClient;
    private final SANFeignClient sanFeignClient;

    protected OntapStorage storage;

    private List<Aggregate> aggregates;

    private static final Logger s_logger = LogManager.getLogger(StorageStrategy.class);

    public StorageStrategy(OntapStorage ontapStorage) {
        storage = ontapStorage;
        String baseURL = Constants.HTTPS + storage.getManagementLIF();
        s_logger.info("Initializing StorageStrategy with base URL: " + baseURL);
        this.feignClientFactory = new FeignClientFactory();
        this.aggregateFeignClient = feignClientFactory.createClient(AggregateFeignClient.class, baseURL);
        this.volumeFeignClient = feignClientFactory.createClient(VolumeFeignClient.class, baseURL);
        this.svmFeignClient = feignClientFactory.createClient(SvmFeignClient.class, baseURL);
        this.jobFeignClient = feignClientFactory.createClient(JobFeignClient.class, baseURL);
        this.networkFeignClient = feignClientFactory.createClient(NetworkFeignClient.class, baseURL);
        this.sanFeignClient = feignClientFactory.createClient(SANFeignClient.class, baseURL);
    }

    public boolean connect() {
        s_logger.info("Attempting to connect to ONTAP cluster at " + storage.getManagementLIF() + " and validate SVM " +
                storage.getSvmName() + ", protocol " + storage.getProtocol());
        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
        String svmName = storage.getSvmName();
        try {
            Svm svm = new Svm();
            s_logger.info("Fetching the SVM details...");
            Map<String, Object> queryParams = Map.of(Constants.NAME, svmName, Constants.FIELDS, Constants.AGGREGATES +
                    Constants.COMMA + Constants.STATE);
            OntapResponse<Svm> svms = svmFeignClient.getSvmResponse(queryParams, authHeader);
            if (svms != null && svms.getRecords() != null && !svms.getRecords().isEmpty()) {
                svm = svms.getRecords().get(0);
            } else {
                s_logger.error("No SVM found on the ONTAP cluster by the name" + svmName + ".");
                return false;
            }

            s_logger.info("Validating SVM state and protocol settings...");
            if (!Objects.equals(svm.getState(), Constants.RUNNING)) {
                s_logger.error("SVM " + svmName + " is not in running state.");
                return false;
            }
            if (Objects.equals(storage.getProtocol(), Constants.NFS) && !svm.getNfsEnabled()) {
                s_logger.error("NFS protocol is not enabled on SVM " + svmName);
                return false;
            } else if (Objects.equals(storage.getProtocol(), Constants.ISCSI) && !svm.getIscsiEnabled()) {
                s_logger.error("iSCSI protocol is not enabled on SVM " + svmName);
                return false;
            }
            List<Aggregate> aggrs = svm.getAggregates();
            if (aggrs == null || aggrs.isEmpty()) {
                s_logger.error("No aggregates are assigned to SVM " + svmName);
                return false;
            }
            for (Aggregate aggr : aggrs) {
                s_logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
                Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid());
                if (!Objects.equals(aggrResp.getState(), Aggregate.StateEnum.ONLINE)) {
                    s_logger.warn("Aggregate " + aggr.getName() + " is not in online state. Skipping this aggregate.");
                    continue;
                } else if (aggrResp.getSpace() == null || aggrResp.getAvailableBlockStorageSpace() == null ||
                        aggrResp.getAvailableBlockStorageSpace() <= storage.getSize().doubleValue()) {
                    s_logger.warn("Aggregate " + aggr.getName() + " does not have sufficient available space. Skipping this aggregate.");
                    continue;
                }
                s_logger.info("Selected aggregate: " + aggr.getName() + " for volume operations.");
                this.aggregates = List.of(aggr);
                break;
            }
            if (this.aggregates == null || this.aggregates.isEmpty()) {
                s_logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
                return false;
            }

            this.aggregates = aggrs;
            s_logger.info("Successfully connected to ONTAP cluster and validated ONTAP details provided");
        } catch (Exception e) {
            s_logger.error("Failed to connect to ONTAP cluster: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public Volume createStorageVolume(String volumeName, Long size) {
        s_logger.info("Creating volume: " + volumeName + " of size: " + size + " bytes");

        String svmName = storage.getSvmName();
        if (aggregates == null || aggregates.isEmpty()) {
            s_logger.error("No aggregates available to create volume on SVM " + svmName);
            throw new CloudRuntimeException("No aggregates available to create volume on SVM " + svmName);
        }
        if (size == null || size <= 0) {
            throw new CloudRuntimeException("Invalid volume size provided: " + size);
        }

        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());

        Volume volumeRequest = new Volume();
        Svm svm = new Svm();
        svm.setName(svmName);
        Nas nas = new Nas();
        nas.setPath(Constants.SLASH + volumeName);

        volumeRequest.setName(volumeName);
        volumeRequest.setSvm(svm);

        long maxAvailableAggregateSpaceBytes = -1L;
        Aggregate aggrChosen = null;
        for (Aggregate aggr : aggregates) {
            s_logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
            Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid());

            if (aggrResp == null) {
                s_logger.warn("Aggregate details response is null for aggregate " + aggr.getName() + ". Skipping.");
                continue;
            }

            if (!Objects.equals(aggrResp.getState(), Aggregate.StateEnum.ONLINE)) {
                s_logger.warn("Aggregate " + aggr.getName() + " is not in online state. Skipping this aggregate.");
                continue;
            }

            if (aggrResp.getSpace() == null || aggrResp.getAvailableBlockStorageSpace() == null) {
                s_logger.warn("Aggregate " + aggr.getName() + " does not have space information. Skipping this aggregate.");
                continue;
            }

            final long availableBytes = aggrResp.getAvailableBlockStorageSpace().longValue();
            s_logger.debug("Aggregate " + aggr.getName() + " available bytes=" + availableBytes + ", requested=" + size);

            if (availableBytes <= size) {
                s_logger.warn("Aggregate " + aggr.getName() + " does not have sufficient available space. Required=" +
                        size + " bytes, available=" + availableBytes + " bytes. Skipping this aggregate.");
                continue;
            }

            if (availableBytes > maxAvailableAggregateSpaceBytes) {
                maxAvailableAggregateSpaceBytes = availableBytes;
                aggrChosen = aggr;
            }
        }

        if (aggrChosen == null) {
            s_logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
            throw new CloudRuntimeException("No suitable aggregates found on SVM " + svmName + " for volume operations.");
        }
        s_logger.info("Selected aggregate: " + aggrChosen.getName() + " for volume operations.");

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

            Boolean jobSucceeded = jobPollForSuccess(jobUUID);
            if (!jobSucceeded) {
                s_logger.error("Volume creation job failed for volume: " + volumeName);
                throw new CloudRuntimeException("Volume creation job failed for volume: " + volumeName);
            }
            s_logger.info("Volume creation job completed successfully for volume: " + volumeName);
        } catch (Exception e) {
            s_logger.error("Exception while creating volume: ", e);
            throw new CloudRuntimeException("Failed to create volume: " + e.getMessage());
        }
        OntapResponse<Volume> volumesResponse = volumeFeignClient.getAllVolumes(authHeader, Map.of(Constants.NAME, volumeName));
        if (volumesResponse == null || volumesResponse.getRecords() == null || volumesResponse.getRecords().isEmpty()) {
            s_logger.error("Volume " + volumeName + " not found after creation.");
            throw new CloudRuntimeException("Volume " + volumeName + " not found after creation.");
        }
        Volume createdVolume = volumesResponse.getRecords().get(0);
        if (createdVolume == null) {
            s_logger.error("Failed to retrieve details of the created volume " + volumeName);
            throw new CloudRuntimeException("Failed to retrieve details of the created volume " + volumeName);
        } else if (createdVolume.getName() == null || !createdVolume.getName().equals(volumeName)) {
            s_logger.error("Mismatch in created volume name. Expected: " + volumeName + ", Found: " + createdVolume.getName());
            throw new CloudRuntimeException("Mismatch in created volume name. Expected: " + volumeName + ", Found: " + createdVolume.getName());
        }
        s_logger.info("Volume created successfully: " + volumeName);
        try {
            Map<String, Object> queryParams = Map.of(Constants.NAME, volumeName);
            s_logger.debug("Fetching volume details for: " + volumeName);

            OntapResponse<Volume> ontapVolume = volumeFeignClient.getVolume(authHeader, queryParams);
            s_logger.debug("Feign call completed. Processing response...");

            if (ontapVolume == null) {
                s_logger.error("OntapResponse is null for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": Response is null");
            }
            s_logger.debug("OntapResponse is not null. Checking records field...");

            if (ontapVolume.getRecords() == null) {
                s_logger.error("OntapResponse.records is null for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": Records list is null");
            }
            s_logger.debug("Records field is not null. Size: " + ontapVolume.getRecords().size());

            if (ontapVolume.getRecords().isEmpty()) {
                s_logger.error("OntapResponse.records is empty for volume: " + volumeName);
                throw new CloudRuntimeException("Failed to fetch volume " + volumeName + ": No records found");
            }

            Volume volume = ontapVolume.getRecords().get(0);
            s_logger.info("Volume retrieved successfully: " + volumeName + ", UUID: " + volume.getUuid());
            return volume;
        } catch (Exception e) {
            s_logger.error("Exception while retrieving volume details for: " + volumeName, e);
            throw new CloudRuntimeException("Failed to fetch volume: " + volumeName + ". Error: " + e.getMessage(), e);
        }
    }

    public Volume updateStorageVolume(Volume volume) {
        return null;
    }

    public void deleteStorageVolume(Volume volume) {
        s_logger.info("Deleting ONTAP volume by name: " + volume.getName() + " and uuid: " + volume.getUuid());
        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            JobResponse jobResponse = volumeFeignClient.deleteVolume(authHeader, volume.getUuid());
            Boolean jobSucceeded = jobPollForSuccess(jobResponse.getJob().getUuid());
            if (!jobSucceeded) {
                s_logger.error("Volume deletion job failed for volume: " + volume.getName());
                throw new CloudRuntimeException("Volume deletion job failed for volume: " + volume.getName());
            }
            s_logger.info("Volume deleted successfully: " + volume.getName());
        } catch (FeignException.FeignClientException e) {
            s_logger.error("Exception while deleting volume: ", e);
            throw new CloudRuntimeException("Failed to delete volume: " + e.getMessage());
        }
        s_logger.info("ONTAP volume deletion process completed for volume: " + volume.getName());
    }

    public Volume getStorageVolume(Volume volume) {
        return null;
    }

    public String getStoragePath() {
        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
        String targetIqn = null;
        try {
            if (storage.getProtocol() == ProtocolType.ISCSI) {
                s_logger.info("Fetching iSCSI target IQN for SVM: {}", storage.getSvmName());

                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put(Constants.SVM_DOT_NAME, storage.getSvmName());
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
                s_logger.info("Retrieved iSCSI target IQN: {}", targetIqn);
                return targetIqn;

            } else if (storage.getProtocol() == ProtocolType.NFS3) {
            } else {
                throw new CloudRuntimeException("Unsupported protocol for path retrieval: " + storage.getProtocol());
            }

        } catch (FeignException.FeignClientException e) {
            s_logger.error("Exception while retrieving storage path for protocol {}: {}", storage.getProtocol(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to retrieve storage path: " + e.getMessage());
        }
        return targetIqn;
    }

    public String getNetworkInterface() {
        String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put(Constants.SVM_DOT_NAME, storage.getSvmName());
            if (storage.getProtocol() != null) {
                switch (storage.getProtocol()) {
                    case NFS3:
                        queryParams.put(Constants.SERVICES, Constants.DATA_NFS);
                        break;
                    case ISCSI:
                        queryParams.put(Constants.SERVICES, Constants.DATA_ISCSI);
                        break;
                    default:
                        s_logger.error("Unsupported protocol: " + storage.getProtocol());
                        throw new CloudRuntimeException("Unsupported protocol: " + storage.getProtocol());
                }
            }
            queryParams.put(Constants.FIELDS, Constants.IP_ADDRESS);
            queryParams.put(Constants.RETURN_RECORDS, Constants.TRUE);
            OntapResponse<IpInterface> response =
                    networkFeignClient.getNetworkIpInterfaces(authHeader, queryParams);
            if (response != null && response.getRecords() != null && !response.getRecords().isEmpty()) {
                IpInterface ipInterface = null;
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

                s_logger.info("Retrieved network interface: " + ipInterface.getIp().getAddress());
                return ipInterface.getIp().getAddress();
            } else {
                throw new CloudRuntimeException("No network interfaces found for SVM " + storage.getSvmName() +
                        " for protocol " + storage.getProtocol());
            }
        } catch (FeignException.FeignClientException e) {
            s_logger.error("Exception while retrieving network interfaces: ", e);
            throw new CloudRuntimeException("Failed to retrieve network interfaces: " + e.getMessage());
        }
    }

    abstract public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume);

    abstract CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume);

    abstract public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume);

    abstract public void copyCloudStackVolume(CloudStackVolume cloudstackVolume);

    abstract public CloudStackVolume getCloudStackVolume(Map<String, String> cloudStackVolumeMap);

    abstract public AccessGroup createAccessGroup(AccessGroup accessGroup);

    abstract public void deleteAccessGroup(AccessGroup accessGroup);

    abstract AccessGroup updateAccessGroup(AccessGroup accessGroup);

    abstract public AccessGroup getAccessGroup(Map<String, String> values);

    abstract public Map<String,String> enableLogicalAccess(Map<String,String> values);

    abstract public void disableLogicalAccess(Map<String, String> values);

    abstract public Map<String, String> getLogicalAccess(Map<String, String> values);

    private Boolean jobPollForSuccess(String jobUUID) {
        int jobRetryCount = 0;
        Job jobResp = null;
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            while (jobResp == null || !jobResp.getState().equals(Constants.JOB_SUCCESS)) {
                if (jobRetryCount >= Constants.JOB_MAX_RETRIES) {
                    s_logger.error("Job did not complete within expected time.");
                    throw new CloudRuntimeException("Job did not complete within expected time.");
                }

                try {
                    jobResp = jobFeignClient.getJobByUUID(authHeader, jobUUID);
                    if (jobResp == null) {
                        s_logger.warn("Job with UUID " + jobUUID + " not found. Retrying...");
                    } else if (jobResp.getState().equals(Constants.JOB_FAILURE)) {
                        throw new CloudRuntimeException("Job failed with error: " + jobResp.getMessage());
                    }
                } catch (FeignException.FeignClientException e) {
                    throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
                }

                jobRetryCount++;
                Thread.sleep(Constants.CREATE_VOLUME_CHECK_SLEEP_TIME);
            }
            if (jobResp == null || !jobResp.getState().equals(Constants.JOB_SUCCESS)) {
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
