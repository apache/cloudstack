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

import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.feign.FeignClientFactory;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Initiator;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnifiedSANStrategy extends SANStrategy {

    private static final Logger logger = LogManager.getLogger(UnifiedSANStrategy.class);
    private final FeignClientFactory feignClientFactory;
    private final SANFeignClient sanFeignClient;

    public UnifiedSANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
        String baseURL = OntapStorageConstants.HTTPS + ontapStorage.getManagementLIF();
        this.feignClientFactory = new FeignClientFactory();
        this.sanFeignClient = feignClientFactory.createClient(SANFeignClient.class, baseURL);
    }

    public void setOntapStorage(OntapStorage ontapStorage) {
        this.storage = ontapStorage;
    }

    @Override
    public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume) {
        return null;
    }

    @Override
    CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume) {
        return null;
    }

    @Override
    public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume) {}

    @Override
    public void copyCloudStackVolume(CloudStackVolume cloudstackVolume) {}

    @Override
    public CloudStackVolume getCloudStackVolume(Map<String, String> values) {
        return null;
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup accessGroup) {
        logger.info("createAccessGroup : Create Igroup");
        String igroupName = "unknown";
        logger.debug("createAccessGroup : Creating Igroup with access group request {} ", accessGroup);
        if (accessGroup == null) {
            logger.error("createAccessGroup: Igroup creation failed. Invalid request: {}", accessGroup);
            throw new CloudRuntimeException("createAccessGroup : Failed to create Igroup, invalid request");
        }
        try {
            if (accessGroup.getPrimaryDataStoreInfo() == null || accessGroup.getPrimaryDataStoreInfo().getDetails() == null
                    || accessGroup.getPrimaryDataStoreInfo().getDetails().isEmpty()) {
                throw new CloudRuntimeException("createAccessGroup : Failed to create Igroup, invalid datastore details in the request");
            }
            Map<String, String> dataStoreDetails = accessGroup.getPrimaryDataStoreInfo().getDetails();
            logger.debug("createAccessGroup: Successfully fetched datastore details.");

            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());

            Igroup igroupRequest = new Igroup();
            List<String> hostsIdentifier = new ArrayList<>();
            String svmName = dataStoreDetails.get(OntapStorageConstants.SVM_NAME);
            igroupName = OntapStorageUtils.getIgroupName(svmName, accessGroup.getScope().getScopeType(), accessGroup.getScope().getScopeId());
            Hypervisor.HypervisorType hypervisorType = accessGroup.getPrimaryDataStoreInfo().getHypervisor();

            ProtocolType protocol = ProtocolType.valueOf(dataStoreDetails.get(OntapStorageConstants.PROTOCOL));
            if (accessGroup.getHostsToConnect() == null || accessGroup.getHostsToConnect().isEmpty()) {
                throw new CloudRuntimeException("createAccessGroup : Failed to create Igroup, no hosts to connect provided in the request");
            }
            if (!validateProtocolSupportAndFetchHostsIdentifier(accessGroup.getHostsToConnect(), protocol, hostsIdentifier)) {
                String errMsg = "createAccessGroup: Not all hosts in the " +  accessGroup.getScope().getScopeType().toString()  + " support the protocol: " + protocol.name();
                throw new CloudRuntimeException(errMsg);
            }

            if (svmName != null && !svmName.isEmpty()) {
                Svm svm = new Svm();
                svm.setName(svmName);
                igroupRequest.setSvm(svm);
            }

            if (igroupName != null && !igroupName.isEmpty()) {
                igroupRequest.setName(igroupName);
            }

            igroupRequest.setOsType(Igroup.OsTypeEnum.Linux);

            if (hostsIdentifier != null && hostsIdentifier.size() > 0) {
                List<Initiator> initiators = new ArrayList<>();
                for (String hostIdentifier : hostsIdentifier) {
                    Initiator initiator = new Initiator();
                    initiator.setName(hostIdentifier);
                    initiators.add(initiator);
                }
                igroupRequest.setInitiators(initiators);
            }
            igroupRequest.setProtocol(Igroup.ProtocolEnum.valueOf("iscsi"));
            logger.debug("createAccessGroup: About to call sanFeignClient.createIgroup with igroupName: {}", igroupName);
            AccessGroup createdAccessGroup = new AccessGroup();
            OntapResponse<Igroup> createdIgroup = null;
            try {
                createdIgroup = sanFeignClient.createIgroup(authHeader, true, igroupRequest);
            } catch (Exception feignEx) {
                String errMsg = feignEx.getMessage();
                if (errMsg != null && errMsg.contains(("5374023"))) {
                    logger.warn("createAccessGroup: Igroup with name {} already exists. Fetching existing Igroup.", igroupName);
                    return createdAccessGroup;
                }
                logger.error("createAccessGroup: Exception during Feign call: {}", feignEx.getMessage(), feignEx);
                throw feignEx;
            }

            logger.debug("createAccessGroup: createdIgroup: {}", createdIgroup);
            logger.debug("createAccessGroup: createdIgroup Records: {}", createdIgroup.getRecords());
            if (createdIgroup == null || createdIgroup.getRecords() == null || createdIgroup.getRecords().isEmpty()) {
                logger.error("createAccessGroup: Igroup creation failed for Igroup Name {}", igroupName);
                throw new CloudRuntimeException("Failed to create Igroup: " + igroupName);
            }
            Igroup igroup = createdIgroup.getRecords().get(0);
            logger.debug("createAccessGroup: Successfully extracted igroup from response: {}", igroup);
            logger.info("createAccessGroup: Igroup created successfully. IgroupName: {}", igroup.getName());

            createdAccessGroup.setIgroup(igroup);
            logger.debug("createAccessGroup: Returning createdAccessGroup");
            return createdAccessGroup;
        } catch (Exception e) {
            logger.error("Exception occurred while creating Igroup: {}, Exception: {}", igroupName, e.getMessage(), e);
            throw new CloudRuntimeException("Failed to create Igroup: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAccessGroup(AccessGroup accessGroup) {
        logger.info("deleteAccessGroup: Deleting iGroup");

        if (accessGroup == null) {
            throw new CloudRuntimeException("deleteAccessGroup: Invalid accessGroup object - accessGroup is null");
        }

        PrimaryDataStoreInfo primaryDataStoreInfo = accessGroup.getPrimaryDataStoreInfo();
        if (primaryDataStoreInfo == null) {
            throw new CloudRuntimeException("deleteAccessGroup: PrimaryDataStoreInfo is null in accessGroup");
        }

        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());

            String svmName = storage.getSvmName();

            String igroupName;
            if (primaryDataStoreInfo.getClusterId() != null) {
                igroupName = OntapStorageUtils.getIgroupName(svmName, com.cloud.storage.ScopeType.CLUSTER, primaryDataStoreInfo.getClusterId());
                logger.info("deleteAccessGroup: Deleting cluster-scoped iGroup '{}'", igroupName);
            } else {
                igroupName = OntapStorageUtils.getIgroupName(svmName, com.cloud.storage.ScopeType.ZONE, primaryDataStoreInfo.getDataCenterId());
                logger.info("deleteAccessGroup: Deleting zone-scoped iGroup '{}'", igroupName);
            }

            Map<String, Object> igroupParams = Map.of(
                    OntapStorageConstants.SVM_DOT_NAME, svmName,
                    OntapStorageConstants.NAME, igroupName
            );

            try {
                OntapResponse<Igroup> igroupResponse = sanFeignClient.getIgroupResponse(authHeader, igroupParams);
                if (igroupResponse == null || igroupResponse.getRecords() == null || igroupResponse.getRecords().isEmpty()) {
                    logger.warn("deleteAccessGroup: iGroup '{}' not found, may have been already deleted", igroupName);
                    return;
                }

                Igroup igroup = igroupResponse.getRecords().get(0);
                String igroupUuid = igroup.getUuid();

                if (igroupUuid == null || igroupUuid.isEmpty()) {
                    throw new CloudRuntimeException("deleteAccessGroup: iGroup UUID is null or empty for iGroup: " + igroupName);
                }

                logger.info("deleteAccessGroup: Deleting iGroup '{}' with UUID '{}'", igroupName, igroupUuid);

                sanFeignClient.deleteIgroup(authHeader, igroupUuid);

                logger.info("deleteAccessGroup: Successfully deleted iGroup '{}'", igroupName);

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("5374852") || errorMsg.contains("not found"))) {
                    logger.warn("deleteAccessGroup: iGroup '{}' does not exist, skipping deletion", igroupName);
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            logger.error("deleteAccessGroup: Failed to delete iGroup. Exception: {}", e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete iGroup: " + e.getMessage(), e);
        }
    }

    private boolean validateProtocolSupportAndFetchHostsIdentifier(List<HostVO> hosts, ProtocolType protocolType, List<String> hostIdentifiers) {
        switch (protocolType) {
            case ISCSI:
                String protocolPrefix = OntapStorageConstants.IQN;
                for (HostVO host : hosts) {
                    if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().isEmpty()
                            || !host.getStorageUrl().startsWith(protocolPrefix)) {
                        return false;
                    }
                    hostIdentifiers.add(host.getStorageUrl());
                }
                break;
            default:
                throw new CloudRuntimeException("validateProtocolSupportAndFetchHostsIdentifier : Unsupported protocol: " + protocolType.name());
        }
        logger.info("validateProtocolSupportAndFetchHostsIdentifier: All hosts support the protocol: " + protocolType.name());
        return true;
    }

    @Override
    public AccessGroup updateAccessGroup(AccessGroup accessGroup) {
        return null;
    }

    public AccessGroup getAccessGroup(Map<String, String> values) {
        logger.info("getAccessGroup : fetch Igroup");
        logger.debug("getAccessGroup : fetching Igroup with params {} ", values);
        if (values == null || values.isEmpty()) {
            logger.error("getAccessGroup: get Igroup failed. Invalid request: {}", values);
            throw new CloudRuntimeException("getAccessGroup : get Igroup Failed, invalid request");
        }
        String svmName = values.get(OntapStorageConstants.SVM_DOT_NAME);
        String igroupName = values.get(OntapStorageConstants.NAME);
        if (svmName == null || igroupName == null || svmName.isEmpty() || igroupName.isEmpty()) {
            logger.error("getAccessGroup: get Igroup failed. Invalid svm:{} or igroup name: {}", svmName, igroupName);
            throw new CloudRuntimeException("getAccessGroup : Failed to get Igroup, invalid request");
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.SVM_DOT_NAME, svmName, OntapStorageConstants.NAME, igroupName, OntapStorageConstants.FIELDS, OntapStorageConstants.INITIATORS);
            OntapResponse<Igroup> igroupResponse = sanFeignClient.getIgroupResponse(authHeader, queryParams);
            if (igroupResponse == null || igroupResponse.getRecords() == null || igroupResponse.getRecords().isEmpty()) {
                logger.warn("getAccessGroup: Igroup '{}' not found on SVM '{}'. Returning null.", igroupName, svmName);
                return null;
            }
            Igroup igroup = igroupResponse.getRecords().get(0);
            AccessGroup accessGroup = new AccessGroup();
            accessGroup.setIgroup(igroup);
            return accessGroup;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.contains("not found")) {
                logger.warn("getAccessGroup: Igroup '{}' not found on SVM '{}' ({}). Returning null.", igroupName, svmName, errMsg);
                return null;
            }
            logger.error("Exception occurred while fetching Igroup, Exception: {}", errMsg);
            throw new CloudRuntimeException("Failed to fetch Igroup details: " + errMsg);
        }
    }

    public Map<String, String> enableLogicalAccess(Map<String, String> values) {
        return null;
    }

    public void disableLogicalAccess(Map<String, String> values) {}

    public Map<String, String> getLogicalAccess(Map<String, String> values) {
        return null;
    }
}
