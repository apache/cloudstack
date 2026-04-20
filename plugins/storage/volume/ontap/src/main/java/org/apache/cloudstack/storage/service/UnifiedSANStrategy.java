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
import com.cloud.utils.exception.CloudRuntimeException;
import feign.FeignException;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Initiator;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunMap;
import org.apache.cloudstack.storage.feign.model.CliSnapshotRestoreRequest;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnifiedSANStrategy extends SANStrategy {

    private static final Logger logger = LogManager.getLogger(UnifiedSANStrategy.class);
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;

    public UnifiedSANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
        String baseURL = OntapStorageConstants.HTTPS + ontapStorage.getStorageIP();
    }

    public void setOntapStorage(OntapStorage ontapStorage) {
        this.storage = ontapStorage;
    }

    @Override
    public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume) {
        logger.info("createCloudStackVolume : Creating Lun with cloudstackVolume request {} ", cloudstackVolume);
        if (cloudstackVolume == null || cloudstackVolume.getLun() == null) {
            logger.error("createCloudStackVolume: LUN creation failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException(" Failed to create Lun, invalid request");
        }
        try {
            // Get AuthHeader
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Create URI for lun creation
            //TODO: It is possible that Lun creation will take time and we may need to handle through async job.
            OntapResponse<Lun> createdLun = sanFeignClient.createLun(authHeader, true, cloudstackVolume.getLun());
            if (createdLun == null || createdLun.getRecords() == null || createdLun.getRecords().size() == 0) {
                logger.error("createCloudStackVolume: LUN creation failed for Lun {}", cloudstackVolume.getLun().getName());
                throw new CloudRuntimeException("Failed to create Lun: " + cloudstackVolume.getLun().getName());
            }
            Lun lun = createdLun.getRecords().get(0);
            logger.debug("createCloudStackVolume: LUN created successfully. Lun: {}", lun);
            logger.info("createCloudStackVolume: LUN created successfully. LunName: {}", lun.getName());

            CloudStackVolume createdCloudStackVolume = new CloudStackVolume();
            createdCloudStackVolume.setLun(lun);
            return createdCloudStackVolume;
        } catch (FeignException e) {
            logger.error("FeignException occurred while creating LUN: {}, Status: {}, Exception: {}",
                    cloudstackVolume.getLun().getName(), e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while creating LUN: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun: " + e.getMessage());
        }
    }

    @Override
    CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume) {
        return null;
    }

    @Override
    public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume) {
        if (cloudstackVolume == null || cloudstackVolume.getLun() == null) {
            logger.error("deleteCloudStackVolume: Lun deletion failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException(" Failed to delete Lun, invalid request");
        }
        logger.info("deleteCloudStackVolume : Deleting Lun: {}", cloudstackVolume.getLun().getName());
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of("allow_delete_while_mapped", "true");
            try {
                sanFeignClient.deleteLun(authHeader, cloudstackVolume.getLun().getUuid(), queryParams);
            } catch (FeignException feignEx) {
                if (feignEx.status() == 404) {
                    logger.warn("deleteCloudStackVolume: Lun {} does not exist (status 404), skipping deletion", cloudstackVolume.getLun().getName());
                    return;
                }
                throw feignEx;
            }
            logger.info("deleteCloudStackVolume: Lun deleted successfully. LunName: {}", cloudstackVolume.getLun().getName());
        } catch (Exception e) {
            logger.error("Exception occurred while deleting Lun: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.getMessage());
            throw new CloudRuntimeException("Failed to delete Lun: " + e.getMessage());
        }
    }

    @Override
    public void copyCloudStackVolume(CloudStackVolume cloudstackVolume) {}

    @Override
    public CloudStackVolume getCloudStackVolume(Map<String, String> values) {
        logger.info("getCloudStackVolume : fetching Lun");
        logger.debug("getCloudStackVolume : fetching Lun with params {} ", values);
        if (values == null || values.isEmpty()) {
            logger.error("getCloudStackVolume: get Lun failed. Invalid request: {}", values);
            throw new CloudRuntimeException(" get Lun Failed, invalid request");
        }
        String svmName = values.get(OntapStorageConstants.SVM_DOT_NAME);
        String lunName = values.get(OntapStorageConstants.NAME);
        if (svmName == null || lunName == null || svmName.isEmpty() || lunName.isEmpty()) {
            logger.error("getCloudStackVolume: get Lun failed. Invalid svm:{} or Lun name: {}", svmName, lunName);
            throw new CloudRuntimeException("Failed to get Lun, invalid request");
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.SVM_DOT_NAME, svmName, OntapStorageConstants.NAME, lunName);
            OntapResponse<Lun> lunResponse = sanFeignClient.getLunResponse(authHeader, queryParams);
            if (lunResponse == null || lunResponse.getRecords() == null || lunResponse.getRecords().isEmpty()) {
                logger.warn("getCloudStackVolume: Lun '{}' on SVM '{}' not found. Returning null.", lunName, svmName);
                return null;
            }
            Lun lun = lunResponse.getRecords().get(0);
            logger.debug("getCloudStackVolume: Lun Details : {}", lun);
            logger.info("getCloudStackVolume: Fetched the Lun successfully. LunName: {}", lun.getName());

            CloudStackVolume cloudStackVolume = new CloudStackVolume();
            cloudStackVolume.setLun(lun);
            return cloudStackVolume;
        } catch (FeignException e) {
            if (e.status() == 404) {
                logger.warn("getCloudStackVolume: Lun '{}' on SVM '{}' not found (status 404). Returning null.", lunName, svmName);
                return null;
            }
            logger.error("FeignException occurred while fetching Lun, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Lun details: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while fetching Lun, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Lun details: " + e.getMessage());
        }
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup accessGroup) {
        logger.debug("createAccessGroup : Creating Igroup with access group request {} ", accessGroup);
        if (accessGroup == null) {
            logger.error("createAccessGroup: Igroup creation failed. Invalid request: {}", accessGroup);
            throw new CloudRuntimeException(" Failed to create Igroup, invalid request");
        }
        // Get StoragePool details
        if (accessGroup.getStoragePoolId() == null) {
            throw new CloudRuntimeException(" Failed to create Igroup, invalid datastore details in the request");
        }
        if (accessGroup.getHostsToConnect() == null || accessGroup.getHostsToConnect().isEmpty()) {
            throw new CloudRuntimeException(" Failed to create Igroup, no hosts to connect provided in the request");
        }

        String igroupName = null;
        try {
            Map<String, String> dataStoreDetails = storagePoolDetailsDao.listDetailsKeyPairs(accessGroup.getStoragePoolId());
            logger.debug("createAccessGroup: Successfully fetched datastore details.");

            // Generate Igroup request
            Igroup igroupRequest = new Igroup();
            String svmName = dataStoreDetails.get(OntapStorageConstants.SVM_NAME);
            ProtocolType protocol = ProtocolType.valueOf(dataStoreDetails.get(OntapStorageConstants.PROTOCOL));

            // Check if all hosts support the protocol
            if (!validateProtocolSupport(accessGroup.getHostsToConnect(), protocol)) {
                String errMsg = " Not all hosts " + " support the protocol: " + protocol.name();
                throw new CloudRuntimeException(errMsg);
            }

            if (svmName != null && !svmName.isEmpty()) {
                Svm svm = new Svm();
                svm.setName(svmName);
                igroupRequest.setSvm(svm);
            }
            // TODO: Defaulting to LINUX for zone scope for now, this has to be revisited when we support other hypervisors
            igroupRequest.setOsType(Igroup.OsTypeEnum.Linux);

            for (HostVO host : accessGroup.getHostsToConnect()) {
                igroupName = OntapStorageUtils.getIgroupName(svmName, host.getName());
                igroupRequest.setName(igroupName);

                List<Initiator> initiators = new ArrayList<>();
                Initiator initiator = new Initiator();
                initiator.setName(host.getStorageUrl());// CloudStack has one iqn for one host
                initiators.add(initiator);
                igroupRequest.setInitiators(initiators);
                igroupRequest.setDeleteOnUnmap(true);
                igroupRequest.setDeleteOnUnmap(true);
            }
            igroupRequest.setProtocol(Igroup.ProtocolEnum.valueOf(OntapStorageConstants.ISCSI));
            // Create Igroup
            logger.debug("createAccessGroup: About to call sanFeignClient.createIgroup with igroupName: {}", igroupName);
            AccessGroup createdAccessGroup = new AccessGroup();
            OntapResponse<Igroup> createdIgroup = null;
            try {
                // Get AuthHeader
                String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
                createdIgroup = sanFeignClient.createIgroup(authHeader, true, igroupRequest);
            } catch (FeignException feignEx) {
                if (feignEx.status() == 409) {
                    logger.warn("createAccessGroup: Igroup with name {} already exists (status 409). Fetching existing Igroup.", igroupName);
                    // TODO: Currently we aren't doing anything with the returned AccessGroup object, so, haven't added code here to fetch the existing Igroup and set it in AccessGroup.
                    return createdAccessGroup;
                }
                logger.error("createAccessGroup: FeignException during Igroup creation: Status: {}, Exception: {}", feignEx.status(), feignEx.getMessage(), feignEx);
                throw feignEx;
            }

            logger.debug("createAccessGroup: createdIgroup: {}", createdIgroup);
            logger.debug("createAccessGroup: createdIgroup Records: {}", createdIgroup.getRecords());
            if (createdIgroup.getRecords() == null || createdIgroup.getRecords().isEmpty()) {
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
            logger.error("deleteAccessGroup: Igroup deletion failed. Invalid request: {}", accessGroup);
            throw new CloudRuntimeException(" Failed to delete Igroup, invalid request");
        }
        // Get StoragePool details
        if (accessGroup.getStoragePoolId() == null) {
            throw new CloudRuntimeException(" Failed to delete Igroup, invalid datastore details in the request");
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            String svmName = storage.getSvmName();
            //Get iGroup name per host
            for(HostVO host : accessGroup.getHostsToConnect()) {
                String igroupName = OntapStorageUtils.getIgroupName(svmName, host.getName());
                logger.info("deleteAccessGroup: iGroup name '{}'", igroupName);

                // Get the iGroup to retrieve its UUID
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
                        throw new CloudRuntimeException(" iGroup UUID is null or empty for iGroup: " + igroupName);
                    }

                    logger.info("deleteAccessGroup: Deleting iGroup '{}' with UUID '{}'", igroupName, igroupUuid);

                    // Delete the iGroup using the UUID
                    sanFeignClient.deleteIgroup(authHeader, igroupUuid);

                    logger.info("deleteAccessGroup: Successfully deleted iGroup '{}'", igroupName);

                } catch (FeignException e) {
                    if (e.status() == 404) {
                        logger.warn("deleteAccessGroup: iGroup '{}' does not exist (status 404), skipping deletion", igroupName);
                    } else {
                        logger.error("deleteAccessGroup: FeignException occurred: Status: {}, Exception: {}", e.status(), e.getMessage(), e);
                        throw e;
                    }
                } catch (Exception e) {
                    logger.error("deleteAccessGroup: Exception occurred: {}", e.getMessage(), e);
                    throw e;
                }
            }
        } catch (FeignException e) {
            logger.error("deleteAccessGroup: FeignException occurred while deleting iGroup. Status: {}, Exception: {}", e.status(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete iGroup: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("deleteAccessGroup: Failed to delete iGroup. Exception: {}", e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete iGroup: " + e.getMessage(), e);
        }
    }

    private boolean validateProtocolSupport(List<HostVO> hosts, ProtocolType protocolType) {
        String protocolPrefix = OntapStorageConstants.IQN;
        for (HostVO host : hosts) {
            if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().isEmpty() || !host.getStorageUrl().startsWith(protocolPrefix)) {
                return false;
            }
        }
        logger.info("validateProtocolSupportAndFetchHostsIdentifier: All hosts support the protocol: " + protocolType.name());
        return true;
    }

    @Override
    public AccessGroup updateAccessGroup(AccessGroup accessGroup) {
        return null;
    }

    @Override
    public AccessGroup getAccessGroup(Map<String, String> values) {
        logger.info("getAccessGroup : fetch Igroup");
        logger.debug("getAccessGroup : fetching Igroup with params {} ", values);
        if (values == null || values.isEmpty()) {
            logger.error("getAccessGroup: get Igroup failed. Invalid request: {}", values);
            throw new CloudRuntimeException(" get Igroup Failed, invalid request");
        }
        String svmName = values.get(OntapStorageConstants.SVM_DOT_NAME);
        String igroupName = values.get(OntapStorageConstants.NAME);
        if (svmName == null || igroupName == null || svmName.isEmpty() || igroupName.isEmpty()) {
            logger.error("getAccessGroup: get Igroup failed. Invalid svm:{} or igroup name: {}", svmName, igroupName);
            throw new CloudRuntimeException(" Failed to get Igroup, invalid request");
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
        } catch (FeignException e) {
            if (e.status() == 404) {
                logger.warn("getAccessGroup: Igroup '{}' not found on SVM '{}' (status 404). Returning null.", igroupName, svmName);
                return null;
            }
            logger.error("FeignException occurred while fetching Igroup, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Igroup details: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while fetching Igroup, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Igroup details: " + e.getMessage());
        }
    }

    public Map<String, String> enableLogicalAccess(Map<String, String> values) {
        logger.info("enableLogicalAccess : Create LunMap");
        logger.debug("enableLogicalAccess : Creating LunMap with values {} ", values);
        Map<String, String> response = null;
        if (values == null) {
            logger.error("enableLogicalAccess: LunMap creation failed. Invalid request values: null");
            throw new CloudRuntimeException(" Failed to create LunMap, invalid request");
        }
        String svmName = values.get(OntapStorageConstants.SVM_DOT_NAME);
        String lunName = values.get(OntapStorageConstants.LUN_DOT_NAME);
        String igroupName = values.get(OntapStorageConstants.IGROUP_DOT_NAME);
        if (svmName == null || lunName == null || igroupName == null || svmName.isEmpty() || lunName.isEmpty() || igroupName.isEmpty()) {
            logger.error("enableLogicalAccess: LunMap creation failed. Invalid request values: {}", values);
            throw new CloudRuntimeException(" Failed to create LunMap, invalid request");
        }
        try {
            // Get AuthHeader
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Create LunMap
            LunMap lunMapRequest = new LunMap();
            Svm svm = new Svm();
            svm.setName(svmName);
            lunMapRequest.setSvm(svm);
            //Set Lun name
            Lun lun = new Lun();
            lun.setName(lunName);
            lunMapRequest.setLun(lun);
            //Set Igroup name
            Igroup igroup = new Igroup();
            igroup.setName(igroupName);
            lunMapRequest.setIgroup(igroup);
            try {
                sanFeignClient.createLunMap(authHeader, true, lunMapRequest);
            } catch (Exception feignEx) {
                String errMsg = feignEx.getMessage();
                if (errMsg != null && errMsg.contains(("LUN already mapped to this group"))) {
                    logger.warn("enableLogicalAccess: LunMap for Lun: {} and igroup: {} already exists.", lunName, igroupName);
                } else {
                    logger.error("enableLogicalAccess: Exception during Feign call: {}", feignEx.getMessage(), feignEx);
                    throw feignEx;
                }
            }
            // Get the LunMap details
            OntapResponse<LunMap> lunMapResponse = null;
            try {
                lunMapResponse = sanFeignClient.getLunMapResponse(authHeader,
                        Map.of(
                                OntapStorageConstants.SVM_DOT_NAME, svmName,
                                OntapStorageConstants.LUN_DOT_NAME, lunName,
                                OntapStorageConstants.IGROUP_DOT_NAME, igroupName,
                                OntapStorageConstants.FIELDS, OntapStorageConstants.LOGICAL_UNIT_NUMBER
                        ));
                response = Map.of(
                        OntapStorageConstants.LOGICAL_UNIT_NUMBER, lunMapResponse.getRecords().get(0).getLogicalUnitNumber().toString()
                );
            } catch (Exception e) {
                logger.error("enableLogicalAccess: Failed to fetch LunMap details for Lun: {} and igroup: {}, Exception: {}", lunName, igroupName, e);
                throw new CloudRuntimeException("Failed to fetch LunMap details for Lun: " + lunName + " and igroup: " + igroupName);
            }
            logger.debug("enableLogicalAccess: LunMap created successfully, LunMap: {}", lunMapResponse.getRecords().get(0));
            logger.info("enableLogicalAccess: LunMap created successfully.");
        } catch (Exception e) {
            logger.error("Exception occurred while creating LunMap", e);
            throw new CloudRuntimeException("Failed to create LunMap: " + e.getMessage());
        }
        return response;
    }

    public void disableLogicalAccess(Map<String, String> values) {
        logger.info("disableLogicalAccess : Delete LunMap");
        logger.debug("disableLogicalAccess : Deleting LunMap with values {} ", values);
        if (values == null) {
            logger.error("disableLogicalAccess: LunMap deletion failed. Invalid request values: null");
            throw new CloudRuntimeException(" Failed to delete LunMap, invalid request");
        }
        String lunUUID = values.get(OntapStorageConstants.LUN_DOT_UUID);
        String igroupUUID = values.get(OntapStorageConstants.IGROUP_DOT_UUID);
        if (lunUUID == null || igroupUUID == null || lunUUID.isEmpty() || igroupUUID.isEmpty()) {
            logger.error("disableLogicalAccess: LunMap deletion failed. Invalid request values: {}", values);
            throw new CloudRuntimeException(" Failed to delete LunMap, invalid request");
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            sanFeignClient.deleteLunMap(authHeader, lunUUID, igroupUUID);
            logger.info("disableLogicalAccess: LunMap deleted successfully.");
        } catch (FeignException e) {
            if (e.status() == 404) {
                logger.warn("disableLogicalAccess: LunMap with Lun UUID: {} and igroup UUID: {} does not exist, skipping deletion", lunUUID, igroupUUID);
                return;
            }
            logger.error("FeignException occurred while deleting LunMap, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to delete LunMap: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception occurred while deleting LunMap, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to delete LunMap: " + e.getMessage());
        }
    }

    // GET-only helper: fetch LUN-map and return logical unit number if it exists; otherwise return null
    public Map<String, String> getLogicalAccess(Map<String, String> values) {
        logger.info("getLogicalAccess : Fetch LunMap");
        logger.debug("getLogicalAccess : Fetching LunMap with values {} ", values);
        if (values == null) {
            logger.error("getLogicalAccess: Invalid request values: null");
            throw new CloudRuntimeException(" Invalid request");
        }
        String svmName = values.get(OntapStorageConstants.SVM_DOT_NAME);
        String lunName = values.get(OntapStorageConstants.LUN_DOT_NAME);
        String igroupName = values.get(OntapStorageConstants.IGROUP_DOT_NAME);
        if (svmName == null || lunName == null || igroupName == null || svmName.isEmpty() || lunName.isEmpty() || igroupName.isEmpty()) {
            logger.error("getLogicalAccess: Invalid request values: {}", values);
            throw new CloudRuntimeException(" Invalid request");
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            OntapResponse<LunMap> lunMapResponse = sanFeignClient.getLunMapResponse(authHeader,
                    Map.of(
                            OntapStorageConstants.SVM_DOT_NAME, svmName,
                            OntapStorageConstants.LUN_DOT_NAME, lunName,
                            OntapStorageConstants.IGROUP_DOT_NAME, igroupName,
                            OntapStorageConstants.FIELDS, OntapStorageConstants.LOGICAL_UNIT_NUMBER
                    ));
            if (lunMapResponse != null && lunMapResponse.getRecords() != null && !lunMapResponse.getRecords().isEmpty()) {
                String lunNumber = lunMapResponse.getRecords().get(0).getLogicalUnitNumber() != null ?
                        lunMapResponse.getRecords().get(0).getLogicalUnitNumber().toString() : null;
                return lunNumber != null ? Map.of(OntapStorageConstants.LOGICAL_UNIT_NUMBER, lunNumber) : null;
            }
        } catch (Exception e) {
            logger.warn("getLogicalAccess: LunMap not found for Lun: {} and igroup: {} ({}).", lunName, igroupName, e.getMessage());
        }
        return null;
    }

    @Override
    public String ensureLunMapped(String svmName, String lunName, String accessGroupName) {
        logger.info("ensureLunMapped: Ensuring LUN [{}] is mapped to igroup [{}] on SVM [{}]", lunName, accessGroupName, svmName);

        // Check existing map first
        Map<String, String> getMap = Map.of(
                OntapStorageConstants.LUN_DOT_NAME, lunName,
                OntapStorageConstants.SVM_DOT_NAME, svmName,
                OntapStorageConstants.IGROUP_DOT_NAME, accessGroupName
        );
        Map<String, String> mapResp = getLogicalAccess(getMap);
        if (mapResp != null && mapResp.containsKey(OntapStorageConstants.LOGICAL_UNIT_NUMBER)) {
            String lunNumber = mapResp.get(OntapStorageConstants.LOGICAL_UNIT_NUMBER);
            logger.info("ensureLunMapped: Existing LunMap found for LUN [{}] in igroup [{}] with LUN number [{}]", lunName, accessGroupName, lunNumber);
            return lunNumber;
        }

        // Create if not exists
        Map<String, String> enableMap = Map.of(
                OntapStorageConstants.LUN_DOT_NAME, lunName,
                OntapStorageConstants.SVM_DOT_NAME, svmName,
                OntapStorageConstants.IGROUP_DOT_NAME, accessGroupName
        );
        Map<String, String> response = enableLogicalAccess(enableMap);
        if (response == null || !response.containsKey(OntapStorageConstants.LOGICAL_UNIT_NUMBER)) {
            throw new CloudRuntimeException("Failed to map LUN [" + lunName + "] to iGroup [" + accessGroupName + "]");
        }
        logger.info("ensureLunMapped: Successfully mapped LUN [{}] to igroup [{}] with LUN number [{}]", lunName, accessGroupName, response.get(OntapStorageConstants.LOGICAL_UNIT_NUMBER));
        return response.get(OntapStorageConstants.LOGICAL_UNIT_NUMBER);
    }
    /**
     * Reverts a LUN to a snapshot using the ONTAP CLI-based snapshot file restore API.
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
     * @param volumePath    The LUN name (used to construct the path)
     * @param lunUuid       The LUN UUID (not used in CLI API, kept for interface consistency)
     * @param flexVolName   The FlexVolume name (required for CLI API)
     * @return JobResponse for the async restore operation
     */
    @Override
    public JobResponse revertSnapshotForCloudStackVolume(String snapshotName, String flexVolUuid,
                                                          String snapshotUuid, String volumePath,
                                                          String lunUuid, String flexVolName) {
        logger.info("revertSnapshotForCloudStackVolume [iSCSI]: Restoring LUN [{}] from snapshot [{}] on FlexVol [{}]",
                volumePath, snapshotName, flexVolName);

        if (snapshotName == null || snapshotName.isEmpty()) {
            throw new CloudRuntimeException("Snapshot name is required for iSCSI snapshot revert");
        }
        if (flexVolName == null || flexVolName.isEmpty()) {
            throw new CloudRuntimeException("FlexVolume name is required for iSCSI snapshot revert");
        }
        if (volumePath == null || volumePath.isEmpty()) {
            throw new CloudRuntimeException("LUN path is required for iSCSI snapshot revert");
        }

        String authHeader = getAuthHeader();
        String svmName = storage.getSvmName();

        // Prepare the LUN path for ONTAP CLI API (ensure it starts with "/")
        String ontapLunPath = volumePath.startsWith("/") ? volumePath : "/" + volumePath;

        // Create CLI snapshot restore request
        CliSnapshotRestoreRequest restoreRequest = new CliSnapshotRestoreRequest(
                svmName, flexVolName, snapshotName, ontapLunPath);

        logger.info("revertSnapshotForCloudStackVolume: Calling CLI file restore API with vserver={}, volume={}, snapshot={}, path={}",
                svmName, flexVolName, snapshotName, ontapLunPath);

        return getSnapshotFeignClient().restoreFileFromSnapshotCli(authHeader, restoreRequest);
    }
}
