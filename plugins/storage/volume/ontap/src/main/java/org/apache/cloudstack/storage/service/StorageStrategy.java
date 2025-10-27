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
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.model.Aggregate;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Objects;

public abstract class StorageStrategy {
    @Inject
    private Utility utils;

    @Inject
    private VolumeFeignClient volumeFeignClient;

    @Inject
    private SvmFeignClient svmFeignClient;

    @Inject
    private JobFeignClient jobFeignClient;

    private final OntapStorage storage;

    private List<Aggregate> aggregates;

    private static final Logger s_logger = (Logger) LogManager.getLogger(StorageStrategy.class);

    public StorageStrategy(OntapStorage ontapStorage) {
        storage = ontapStorage;
    }

    // Connect method to validate ONTAP cluster, credentials, protocol, and SVM
    public boolean connect() {
        s_logger.info("Attempting to connect to ONTAP cluster at " + storage.getManagementLIF());
        //Get AuthHeader
        String authHeader = utils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            // Call the SVM API to check if the SVM exists
            Svm svm = null;
            URI url = URI.create(Constants.HTTPS + storage.getManagementLIF() + Constants.GET_SVMs);
            OntapResponse<Svm> svms = svmFeignClient.getSvmResponse(url, authHeader);
            for (Svm storageVM : svms.getRecords()) {
                if (storageVM.getName().equals(storage.getSvmName())) {
                    svm = storageVM;
                    s_logger.info("Found SVM: " + storage.getSvmName());
                    break;
                }
            }

            // Validations
            if (svm == null) {
                s_logger.error("SVM with name " + storage.getSvmName() + " not found.");
                throw new CloudRuntimeException("SVM with name " + storage.getSvmName() + " not found.");
            } else {
                if (svm.getState() != Constants.RUNNING) {
                    s_logger.error("SVM " + storage.getSvmName() + " is not in running state.");
                    throw new CloudRuntimeException("SVM " + storage.getSvmName() + " is not in running state.");
                }
                if (Objects.equals(storage.getProtocol(), Constants.NFS) && !svm.getNfsEnabled()) {
                    s_logger.error("NFS protocol is not enabled on SVM " + storage.getSvmName());
                    throw new CloudRuntimeException("NFS protocol is not enabled on SVM " + storage.getSvmName());
                } else if (Objects.equals(storage.getProtocol(), Constants.ISCSI) && !svm.getIscsiEnabled()) {
                    s_logger.error("iSCSI protocol is not enabled on SVM " + storage.getSvmName());
                    throw new CloudRuntimeException("iSCSI protocol is not enabled on SVM " + storage.getSvmName());
                }
                List<Aggregate> aggrs = svm.getAggregates();
                if (aggrs == null || aggrs.isEmpty()) {
                    s_logger.error("No aggregates are assigned to SVM " + storage.getSvmName());
                    throw new CloudRuntimeException("No aggregates are assigned to SVM " + storage.getSvmName());
                }
                this.aggregates = aggrs;
            }
            s_logger.info("Successfully connected to ONTAP cluster and validated ONTAP details provided");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to connect to ONTAP cluster: " + e.getMessage());
        }
        return true;
    }

    // Common methods like create/delete etc., should be here
    public void createVolume(String volumeName, Long size) {
        s_logger.info("Creating volume: " + volumeName + " of size: " + size + " bytes");

        if (aggregates == null || aggregates.isEmpty()) {
            s_logger.error("No aggregates available to create volume on SVM " + storage.getSvmName());
            throw new CloudRuntimeException("No aggregates available to create volume on SVM " + storage.getSvmName());
        }
        // Get the AuthHeader
        String authHeader = utils.generateAuthHeader(storage.getUsername(), storage.getPassword());

        // Generate the Create Volume Request
        Volume volumeRequest = new Volume();
        Svm svm = new Svm();
        svm.setName(storage.getSvmName());

        volumeRequest.setName(volumeName);
        volumeRequest.setSvm(svm);
        volumeRequest.setAggregates(aggregates);
        volumeRequest.setSize(size);
        // Make the POST API call to create the volume
        try {
            // Create URI for POST CreateVolume API
            URI url = utils.generateURI(Constants.CREATE_VOLUME);
            // Call the VolumeFeignClient to create the volume
            JobResponse jobResponse = volumeFeignClient.createVolumeWithJob(url, authHeader, volumeRequest);
            String jobUUID = jobResponse.getJob().getUuid();

            //Create URI for GET Job API
            url = utils.generateURI(Constants.GET_JOB_BY_UUID);
            int jobRetryCount = 0, maxJobRetries = Constants.JOB_MAX_RETRIES;
            Job createVolumeJob = null;
            while(createVolumeJob == null || createVolumeJob.getState().equals(Constants.JOB_RUNNING) || createVolumeJob.getState().equals(Constants.JOB_QUEUE) || createVolumeJob.getState().equals(Constants.JOB_PAUSED)) {
                if(jobRetryCount >= maxJobRetries) {
                    s_logger.error("Job to create volume " + volumeName + " did not complete within expected time.");
                    throw new CloudRuntimeException("Job to create volume " + volumeName + " did not complete within expected time.");
                }

                try {
                    createVolumeJob = jobFeignClient.getJobByUUID(url, authHeader, jobUUID);
                    if (createVolumeJob == null) {
                        s_logger.warn("Job with UUID " + jobUUID + " not found. Retrying...");
                    } else if (createVolumeJob.getState().equals(Constants.JOB_FAILURE)) {
                        throw new CloudRuntimeException("Job to create volume " + volumeName + " failed with error: " + createVolumeJob.getMessage());
                    }
                } catch (FeignException.FeignClientException e) {
                    throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
                }

                jobRetryCount++;
                Thread.sleep(Constants.CREATE_VOLUME_CHECK_SLEEP_TIME); // Sleep for 2 seconds before polling again
            }
        } catch (Exception e) {
            s_logger.error("Exception while creating volume: ", e);
            throw new CloudRuntimeException("Failed to create volume: " + e.getMessage());
        }
        s_logger.info("Volume created successfully: " + volumeName);
    }
}