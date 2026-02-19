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
import feign.FeignException;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.feign.FeignClientFactory;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Initiator;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunMap;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnifiedSANStrategy extends SANStrategy {

    private static final Logger s_logger = LogManager.getLogger(UnifiedSANStrategy.class);
    // Replace @Inject Feign client with FeignClientFactory
    private final FeignClientFactory feignClientFactory;
    private final SANFeignClient sanFeignClient;

    public UnifiedSANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
        String baseURL = Constants.HTTPS + ontapStorage.getStorageIP();
        // Initialize FeignClientFactory and create SAN client
        this.feignClientFactory = new FeignClientFactory();
        this.sanFeignClient = feignClientFactory.createClient(SANFeignClient.class, baseURL);
    }

    public void setOntapStorage(OntapStorage ontapStorage) {
        this.storage = ontapStorage;
    }

    @Override
    public CloudStackVolume createCloudStackVolume(CloudStackVolume cloudstackVolume) {
        s_logger.info("createCloudStackVolume : Creating Lun with cloudstackVolume request {} ", cloudstackVolume);
        if (cloudstackVolume == null || cloudstackVolume.getLun() == null) {
            s_logger.error("createCloudStackVolume: LUN creation failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException("createCloudStackVolume : Failed to create Lun, invalid request");
        }
        try {
            // Get AuthHeader
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Create URI for lun creation
            //TODO: It is possible that Lun creation will take time and we may need to handle through async job.
            OntapResponse<Lun> createdLun = sanFeignClient.createLun(authHeader, true, cloudstackVolume.getLun());
            if (createdLun == null || createdLun.getRecords() == null || createdLun.getRecords().size() == 0) {
                s_logger.error("createCloudStackVolume: LUN creation failed for Lun {}", cloudstackVolume.getLun().getName());
                throw new CloudRuntimeException("Failed to create Lun: " + cloudstackVolume.getLun().getName());
            }
            Lun lun = createdLun.getRecords().get(0);
            s_logger.debug("createCloudStackVolume: LUN created successfully. Lun: {}", lun);
            s_logger.info("createCloudStackVolume: LUN created successfully. LunName: {}", lun.getName());

            CloudStackVolume createdCloudStackVolume = new CloudStackVolume();
            createdCloudStackVolume.setLun(lun);
            return createdCloudStackVolume;
        } catch (FeignException e) {
            s_logger.error("FeignException occurred while creating LUN: {}, Status: {}, Exception: {}",
                    cloudstackVolume.getLun().getName(), e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun: " + e.getMessage());
        } catch (Exception e) {
            s_logger.error("Exception occurred while creating LUN: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun: " + e.getMessage());
        }
    }

    @Override
    CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume) {
        //TODO
        return null;
    }

    @Override
    public void deleteCloudStackVolume(CloudStackVolume cloudstackVolume) {
        if (cloudstackVolume == null || cloudstackVolume.getLun() == null) {
            s_logger.error("deleteCloudStackVolume: Lun deletion failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException("deleteCloudStackVolume : Failed to delete Lun, invalid request");
        }
        s_logger.info("deleteCloudStackVolume : Deleting Lun: {}", cloudstackVolume.getLun().getName());
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of("allow_delete_while_mapped", "true");
            try {
                sanFeignClient.deleteLun(authHeader, cloudstackVolume.getLun().getUuid(), queryParams);
            } catch (FeignException feignEx) {
                if (feignEx.status() == 404) {
                    s_logger.warn("deleteCloudStackVolume: Lun {} does not exist (status 404), skipping deletion", cloudstackVolume.getLun().getName());
                    return;
                }
                throw feignEx;
            }
            s_logger.info("deleteCloudStackVolume: Lun deleted successfully. LunName: {}", cloudstackVolume.getLun().getName());
        } catch (Exception e) {
            s_logger.error("Exception occurred while deleting Lun: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.getMessage());
            throw new CloudRuntimeException("Failed to delete Lun: " + e.getMessage());
        }
    }

    @Override
    public void copyCloudStackVolume(CloudStackVolume cloudstackVolume) {
        if (cloudstackVolume == null || cloudstackVolume.getLun() == null) {
            s_logger.error("copyCloudStackVolume: Lun clone creation failed. Invalid request: {}", cloudstackVolume);
            throw new CloudRuntimeException("copyCloudStackVolume : Failed to create Lun clone, invalid request");
        }
        s_logger.debug("copyCloudStackVolume: Creating clone of the cloudstack volume: {}", cloudstackVolume.getLun().getName());

        try {
            // Get AuthHeader
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            // Create URI for lun clone creation
            Lun lunCloneRequest = cloudstackVolume.getLun();
            Lun.Clone clone = new Lun.Clone();
            Lun.Source source = new Lun.Source();
            source.setName(cloudstackVolume.getLun().getName());
            clone.setSource(source);
            lunCloneRequest.setClone(clone);
            String lunCloneName = cloudstackVolume.getLun().getName() + "_clone";
            lunCloneRequest.setName(lunCloneName);
            sanFeignClient.createLun(authHeader, true, lunCloneRequest);
        } catch (FeignException e) {
            s_logger.error("FeignException occurred while creating Lun clone: {}, Status: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun clone: " + e.getMessage());
        } catch (Exception e) {
            s_logger.error("Exception occurred while creating Lun clone: {}, Exception: {}", cloudstackVolume.getLun().getName(), e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun clone: " + e.getMessage());
        }
    }

    @Override
    public CloudStackVolume getCloudStackVolume(Map<String, String> values) {
        s_logger.info("getCloudStackVolume : fetching Lun");
        s_logger.debug("getCloudStackVolume : fetching Lun with params {} ", values);
        if (values == null || values.isEmpty()) {
            s_logger.error("getCloudStackVolume: get Lun failed. Invalid request: {}", values);
            throw new CloudRuntimeException("getCloudStackVolume : get Lun Failed, invalid request");
        }
        String svmName = values.get(Constants.SVM_DOT_NAME);
        String lunName = values.get(Constants.NAME);
        if (svmName == null || lunName == null || svmName.isEmpty() || lunName.isEmpty()) {
            s_logger.error("getCloudStackVolume: get Lun failed. Invalid svm:{} or Lun name: {}", svmName, lunName);
            throw new CloudRuntimeException("getCloudStackVolume : Failed to get Lun, invalid request");
        }
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of(Constants.SVM_DOT_NAME, svmName, Constants.NAME, lunName);
            OntapResponse<Lun> lunResponse = sanFeignClient.getLunResponse(authHeader, queryParams);
            if (lunResponse == null || lunResponse.getRecords() == null || lunResponse.getRecords().isEmpty()) {
                s_logger.warn("getCloudStackVolume: Lun '{}' on SVM '{}' not found. Returning null.", lunName, svmName);
                return null;
            }
            Lun lun = lunResponse.getRecords().get(0);
            s_logger.debug("getCloudStackVolume: Lun Details : {}", lun);
            s_logger.info("getCloudStackVolume: Fetched the Lun successfully. LunName: {}", lun.getName());

            CloudStackVolume cloudStackVolume = new CloudStackVolume();
            cloudStackVolume.setLun(lun);
            return cloudStackVolume;
        } catch (FeignException e) {
            if (e.status() == 404) {
                s_logger.warn("getCloudStackVolume: Lun '{}' on SVM '{}' not found (status 404). Returning null.", lunName, svmName);
                return null;
            }
            s_logger.error("FeignException occurred while fetching Lun, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Lun details: " + e.getMessage());
        } catch (Exception e) {
            s_logger.error("Exception occurred while fetching Lun, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Lun details: " + e.getMessage());
        }
    }

    @Override
    public CloudStackVolume snapshotCloudStackVolume(CloudStackVolume cloudstackVolume) {
        return null;
    }

    @Override
    public AccessGroup createAccessGroup(AccessGroup accessGroup) {
        s_logger.info("createAccessGroup : Create Igroup");
        String igroupName = "unknown";
        s_logger.debug("createAccessGroup : Creating Igroup with access group request {} ", accessGroup);
        if (accessGroup == null) {
            s_logger.error("createAccessGroup: Igroup creation failed. Invalid request: {}", accessGroup);
            throw new CloudRuntimeException("createAccessGroup : Failed to create Igroup, invalid request");
        }
        try {
            // Get StoragePool details
            if (accessGroup.getPrimaryDataStoreInfo() == null || accessGroup.getPrimaryDataStoreInfo().getDetails() == null
                    || accessGroup.getPrimaryDataStoreInfo().getDetails().isEmpty()) {
                throw new CloudRuntimeException("createAccessGroup : Failed to create Igroup, invalid datastore details in the request");
            }
            Map<String, String> dataStoreDetails = accessGroup.getPrimaryDataStoreInfo().getDetails();
            s_logger.debug("createAccessGroup: Successfully fetched datastore details.");

            // Get AuthHeader
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());

            // Generate Igroup request
            Igroup igroupRequest = new Igroup();
            List<String> hostsIdentifier = new ArrayList<>();
            String svmName = dataStoreDetails.get(Constants.SVM_NAME);
            String storagePoolUuid = accessGroup.getPrimaryDataStoreInfo().getUuid();
            igroupName = Utility.getIgroupName(svmName, storagePoolUuid);
            Hypervisor.HypervisorType hypervisorType = accessGroup.getPrimaryDataStoreInfo().getHypervisor();

            ProtocolType protocol = ProtocolType.valueOf(dataStoreDetails.get(Constants.PROTOCOL));
            // Check if all hosts support the protocol
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

//            if (hypervisorType != null) {
//                String hypervisorName = hypervisorType.name();
//                igroupRequest.setOsType(Igroup.OsTypeEnum.valueOf(Utility.getOSTypeFromHypervisor(hypervisorName)));
//            } else if ( accessGroup.getScope().getScopeType() == ScopeType.ZONE) {
//                igroupRequest.setOsType(Igroup.OsTypeEnum.linux); // TODO: Defaulting to LINUX for zone scope for now, this has to be revisited when we support other hypervisors
//            }
            igroupRequest.setOsType(Igroup.OsTypeEnum.linux);

            if (hostsIdentifier != null && hostsIdentifier.size() > 0) {
                List<Initiator> initiators = new ArrayList<>();
                for (String hostIdentifier : hostsIdentifier) {
                    Initiator initiator = new Initiator();
                    initiator.setName(hostIdentifier);
                    initiators.add(initiator);
                }
                igroupRequest.setInitiators(initiators);
            }
            igroupRequest.setProtocol(Igroup.ProtocolEnum.valueOf(Constants.ISCSI));
            // Create Igroup
            s_logger.debug("createAccessGroup: About to call sanFeignClient.createIgroup with igroupName: {}", igroupName);
            AccessGroup createdAccessGroup = new AccessGroup();
            OntapResponse<Igroup> createdIgroup = null;
            try {
                createdIgroup = sanFeignClient.createIgroup(authHeader, true, igroupRequest);
            } catch (FeignException feignEx) {
                if (feignEx.status() == 409) {
                    s_logger.warn("createAccessGroup: Igroup with name {} already exists (status 409). Fetching existing Igroup.", igroupName);
                    // TODO: Currently we aren't doing anything with the returned AccessGroup object, so, haven't added code here to fetch the existing Igroup and set it in AccessGroup.
                    return createdAccessGroup;
                }
                s_logger.error("createAccessGroup: FeignException during Igroup creation: Status: {}, Exception: {}", feignEx.status(), feignEx.getMessage(), feignEx);
                throw feignEx;
            }

            s_logger.debug("createAccessGroup: createdIgroup: {}", createdIgroup);
            s_logger.debug("createAccessGroup: createdIgroup Records: {}", createdIgroup.getRecords());
            if (createdIgroup == null || createdIgroup.getRecords() == null || createdIgroup.getRecords().isEmpty()) {
                s_logger.error("createAccessGroup: Igroup creation failed for Igroup Name {}", igroupName);
                throw new CloudRuntimeException("Failed to create Igroup: " + igroupName);
            }
            Igroup igroup = createdIgroup.getRecords().get(0);
            s_logger.debug("createAccessGroup: Successfully extracted igroup from response: {}", igroup);
            s_logger.info("createAccessGroup: Igroup created successfully. IgroupName: {}", igroup.getName());

            createdAccessGroup.setIgroup(igroup);
            s_logger.debug("createAccessGroup: Returning createdAccessGroup");
            return createdAccessGroup;
        } catch (Exception e) {
            s_logger.error("Exception occurred while creating Igroup: {}, Exception: {}", igroupName, e.getMessage(), e);
            throw new CloudRuntimeException("Failed to create Igroup: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteAccessGroup(AccessGroup accessGroup) {
        s_logger.info("deleteAccessGroup: Deleting iGroup");

        if (accessGroup == null) {
            throw new CloudRuntimeException("deleteAccessGroup: Invalid accessGroup object - accessGroup is null");
        }

        // Get PrimaryDataStoreInfo from accessGroup
        PrimaryDataStoreInfo primaryDataStoreInfo = accessGroup.getPrimaryDataStoreInfo();
        if (primaryDataStoreInfo == null) {
            throw new CloudRuntimeException("deleteAccessGroup: PrimaryDataStoreInfo is null in accessGroup");
        }

        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());

            // Extract SVM name from storage (already initialized in constructor via OntapStorage)
            String svmName = storage.getSvmName();
            String storagePoolUuid = primaryDataStoreInfo.getUuid();

            // Determine scope and generate iGroup name
            String igroupName = Utility.getIgroupName(svmName, storagePoolUuid);
            s_logger.info("deleteAccessGroup: Generated iGroup name '{}'", igroupName);
            if (primaryDataStoreInfo.getClusterId() != null) {
                igroupName = Utility.getIgroupName(svmName, storagePoolUuid);
                s_logger.info("deleteAccessGroup: Deleting cluster-scoped iGroup '{}'", igroupName);
            } else {
                igroupName = Utility.getIgroupName(svmName, storagePoolUuid);
                s_logger.info("deleteAccessGroup: Deleting zone-scoped iGroup '{}'", igroupName);
            }

            // Get the iGroup to retrieve its UUID
            Map<String, Object> igroupParams = Map.of(
                    Constants.SVM_DOT_NAME, svmName,
                    Constants.NAME, igroupName
            );

            try {
                OntapResponse<Igroup> igroupResponse = sanFeignClient.getIgroupResponse(authHeader, igroupParams);
                if (igroupResponse == null || igroupResponse.getRecords() == null || igroupResponse.getRecords().isEmpty()) {
                    s_logger.warn("deleteAccessGroup: iGroup '{}' not found, may have been already deleted", igroupName);
                    return;
                }

                Igroup igroup = igroupResponse.getRecords().get(0);
                String igroupUuid = igroup.getUuid();

                if (igroupUuid == null || igroupUuid.isEmpty()) {
                    throw new CloudRuntimeException("deleteAccessGroup: iGroup UUID is null or empty for iGroup: " + igroupName);
                }

                s_logger.info("deleteAccessGroup: Deleting iGroup '{}' with UUID '{}'", igroupName, igroupUuid);

                // Delete the iGroup using the UUID
                sanFeignClient.deleteIgroup(authHeader, igroupUuid);

                s_logger.info("deleteAccessGroup: Successfully deleted iGroup '{}'", igroupName);

            } catch (FeignException e) {
                if (e.status() == 404) {
                    s_logger.warn("deleteAccessGroup: iGroup '{}' does not exist (status 404), skipping deletion", igroupName);
                } else {
                    s_logger.error("deleteAccessGroup: FeignException occurred: Status: {}, Exception: {}", e.status(), e.getMessage(), e);
                    throw e;
                }
            } catch (Exception e) {
                s_logger.error("deleteAccessGroup: Exception occurred: {}", e.getMessage(), e);
                throw e;
            }

        } catch (FeignException e) {
            s_logger.error("deleteAccessGroup: FeignException occurred while deleting iGroup. Status: {}, Exception: {}", e.status(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete iGroup: " + e.getMessage(), e);
        } catch (Exception e) {
            s_logger.error("deleteAccessGroup: Failed to delete iGroup. Exception: {}", e.getMessage(), e);
            throw new CloudRuntimeException("Failed to delete iGroup: " + e.getMessage(), e);
        }
    }

    private boolean validateProtocolSupportAndFetchHostsIdentifier(List<HostVO> hosts, ProtocolType protocolType, List<String> hostIdentifiers) {
        switch (protocolType) {
            case ISCSI:
                String protocolPrefix = Constants.IQN;
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
        s_logger.info("validateProtocolSupportAndFetchHostsIdentifier: All hosts support the protocol: " + protocolType.name());
        return true;
    }

    @Override
    public AccessGroup updateAccessGroup(AccessGroup accessGroup) {
        //TODO
        return null;
    }

    @Override
    public AccessGroup getAccessGroup(Map<String, String> values) {
        s_logger.info("getAccessGroup : fetch Igroup");
        s_logger.debug("getAccessGroup : fetching Igroup with params {} ", values);
        if (values == null || values.isEmpty()) {
            s_logger.error("getAccessGroup: get Igroup failed. Invalid request: {}", values);
            throw new CloudRuntimeException("getAccessGroup : get Igroup Failed, invalid request");
        }
        String svmName = values.get(Constants.SVM_DOT_NAME);
        String igroupName = values.get(Constants.NAME);
        if (svmName == null || igroupName == null || svmName.isEmpty() || igroupName.isEmpty()) {
            s_logger.error("getAccessGroup: get Igroup failed. Invalid svm:{} or igroup name: {}", svmName, igroupName);
            throw new CloudRuntimeException("getAccessGroup : Failed to get Igroup, invalid request");
        }
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Map<String, Object> queryParams = Map.of(Constants.SVM_DOT_NAME, svmName, Constants.NAME, igroupName, Constants.FIELDS, Constants.INITIATORS);
            OntapResponse<Igroup> igroupResponse = sanFeignClient.getIgroupResponse(authHeader, queryParams);
            if (igroupResponse == null || igroupResponse.getRecords() == null || igroupResponse.getRecords().isEmpty()) {
                s_logger.warn("getAccessGroup: Igroup '{}' not found on SVM '{}'. Returning null.", igroupName, svmName);
                return null;
            }
            Igroup igroup = igroupResponse.getRecords().get(0);
            AccessGroup accessGroup = new AccessGroup();
            accessGroup.setIgroup(igroup);
            return accessGroup;
        } catch (FeignException e) {
            if (e.status() == 404) {
                s_logger.warn("getAccessGroup: Igroup '{}' not found on SVM '{}' (status 404). Returning null.", igroupName, svmName);
                return null;
            }
            s_logger.error("FeignException occurred while fetching Igroup, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Igroup details: " + e.getMessage());
        } catch (Exception e) {
            s_logger.error("Exception occurred while fetching Igroup, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to fetch Igroup details: " + e.getMessage());
        }
    }

    public Map<String, String> enableLogicalAccess(Map<String, String> values) {
        s_logger.info("enableLogicalAccess : Create LunMap");
        s_logger.debug("enableLogicalAccess : Creating LunMap with values {} ", values);
        Map<String, String> response = null;
        if (values == null) {
            s_logger.error("enableLogicalAccess: LunMap creation failed. Invalid request values: null");
            throw new CloudRuntimeException("enableLogicalAccess : Failed to create LunMap, invalid request");
        }
        String svmName = values.get(Constants.SVM_DOT_NAME);
        String lunName = values.get(Constants.LUN_DOT_NAME);
        String igroupName = values.get(Constants.IGROUP_DOT_NAME);
        if (svmName == null || lunName == null || igroupName == null || svmName.isEmpty() || lunName.isEmpty() || igroupName.isEmpty()) {
            s_logger.error("enableLogicalAccess: LunMap creation failed. Invalid request values: {}", values);
            throw new CloudRuntimeException("enableLogicalAccess : Failed to create LunMap, invalid request");
        }
        try {
            // Get AuthHeader
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
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
                    s_logger.warn("enableLogicalAccess: LunMap for Lun: {} and igroup: {} already exists.", lunName, igroupName);
                } else {
                    s_logger.error("enableLogicalAccess: Exception during Feign call: {}", feignEx.getMessage(), feignEx);
                    throw feignEx;
                }
            }
            // Get the LunMap details
            OntapResponse<LunMap> lunMapResponse = null;
            try {
                lunMapResponse = sanFeignClient.getLunMapResponse(authHeader,
                        Map.of(
                                Constants.SVM_DOT_NAME, svmName,
                                Constants.LUN_DOT_NAME, lunName,
                                Constants.IGROUP_DOT_NAME, igroupName,
                                Constants.FIELDS, Constants.LOGICAL_UNIT_NUMBER
                        ));
                response = Map.of(
                        Constants.LOGICAL_UNIT_NUMBER, lunMapResponse.getRecords().get(0).getLogicalUnitNumber().toString()
                );
            } catch (Exception e) {
                s_logger.error("enableLogicalAccess: Failed to fetch LunMap details for Lun: {} and igroup: {}, Exception: {}", lunName, igroupName, e);
                throw new CloudRuntimeException("Failed to fetch LunMap details for Lun: " + lunName + " and igroup: " + igroupName);
            }
            s_logger.debug("enableLogicalAccess: LunMap created successfully, LunMap: {}", lunMapResponse.getRecords().get(0));
            s_logger.info("enableLogicalAccess: LunMap created successfully.");
        } catch (Exception e) {
            s_logger.error("Exception occurred while creating LunMap", e);
            throw new CloudRuntimeException("Failed to create LunMap: " + e.getMessage());
        }
        return response;
    }

    public void disableLogicalAccess(Map<String, String> values) {
        s_logger.info("disableLogicalAccess : Delete LunMap");
        s_logger.debug("disableLogicalAccess : Deleting LunMap with values {} ", values);
        if (values == null) {
            s_logger.error("disableLogicalAccess: LunMap deletion failed. Invalid request values: null");
            throw new CloudRuntimeException("disableLogicalAccess : Failed to delete LunMap, invalid request");
        }
        String lunUUID = values.get(Constants.LUN_DOT_UUID);
        String igroupUUID = values.get(Constants.IGROUP_DOT_UUID);
        if (lunUUID == null || igroupUUID == null || lunUUID.isEmpty() || igroupUUID.isEmpty()) {
            s_logger.error("disableLogicalAccess: LunMap deletion failed. Invalid request values: {}", values);
            throw new CloudRuntimeException("disableLogicalAccess : Failed to delete LunMap, invalid request");
        }
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            sanFeignClient.deleteLunMap(authHeader, lunUUID, igroupUUID);
            s_logger.info("disableLogicalAccess: LunMap deleted successfully.");
        } catch (FeignException e) {
            if (e.status() == 404) {
                s_logger.warn("disableLogicalAccess: LunMap with Lun UUID: {} and igroup UUID: {} does not exist, skipping deletion", lunUUID, igroupUUID);
                return;
            }
            s_logger.error("FeignException occurred while deleting LunMap, Status: {}, Exception: {}", e.status(), e.getMessage());
            throw new CloudRuntimeException("Failed to delete LunMap: " + e.getMessage());
        } catch (Exception e) {
            s_logger.error("Exception occurred while deleting LunMap, Exception: {}", e.getMessage());
            throw new CloudRuntimeException("Failed to delete LunMap: " + e.getMessage());
        }
    }

    // GET-only helper: fetch LUN-map and return logical unit number if it exists; otherwise return null
    public Map<String, String> getLogicalAccess(Map<String, String> values) {
        s_logger.info("getLogicalAccess : Fetch LunMap");
        s_logger.debug("getLogicalAccess : Fetching LunMap with values {} ", values);
        if (values == null) {
            s_logger.error("getLogicalAccess: Invalid request values: null");
            throw new CloudRuntimeException("getLogicalAccess : Invalid request");
        }
        String svmName = values.get(Constants.SVM_DOT_NAME);
        String lunName = values.get(Constants.LUN_DOT_NAME);
        String igroupName = values.get(Constants.IGROUP_DOT_NAME);
        if (svmName == null || lunName == null || igroupName == null || svmName.isEmpty() || lunName.isEmpty() || igroupName.isEmpty()) {
            s_logger.error("getLogicalAccess: Invalid request values: {}", values);
            throw new CloudRuntimeException("getLogicalAccess : Invalid request");
        }
        try {
            String authHeader = Utility.generateAuthHeader(storage.getUsername(), storage.getPassword());
            OntapResponse<LunMap> lunMapResponse = sanFeignClient.getLunMapResponse(authHeader,
                    Map.of(
                            Constants.SVM_DOT_NAME, svmName,
                            Constants.LUN_DOT_NAME, lunName,
                            Constants.IGROUP_DOT_NAME, igroupName,
                            Constants.FIELDS, Constants.LOGICAL_UNIT_NUMBER
                    ));
            if (lunMapResponse != null && lunMapResponse.getRecords() != null && !lunMapResponse.getRecords().isEmpty()) {
                String lunNumber = lunMapResponse.getRecords().get(0).getLogicalUnitNumber() != null ?
                        lunMapResponse.getRecords().get(0).getLogicalUnitNumber().toString() : null;
                return lunNumber != null ? Map.of(Constants.LOGICAL_UNIT_NUMBER, lunNumber) : null;
            }
        } catch (Exception e) {
            s_logger.warn("getLogicalAccess: LunMap not found for Lun: {} and igroup: {} ({}).", lunName, igroupName, e.getMessage());
        }
        return null;
    }

    @Override
    public String ensureLunMapped(String svmName, String lunName, String accessGroupName) {
        s_logger.info("ensureLunMapped: Ensuring LUN [{}] is mapped to igroup [{}] on SVM [{}]", lunName, accessGroupName, svmName);

        // Check existing map first
        Map<String, String> getMap = Map.of(
                Constants.LUN_DOT_NAME, lunName,
                Constants.SVM_DOT_NAME, svmName,
                Constants.IGROUP_DOT_NAME, accessGroupName
        );
        Map<String, String> mapResp = getLogicalAccess(getMap);
        if (mapResp != null && mapResp.containsKey(Constants.LOGICAL_UNIT_NUMBER)) {
            String lunNumber = mapResp.get(Constants.LOGICAL_UNIT_NUMBER);
            s_logger.info("ensureLunMapped: Existing LunMap found for LUN [{}] in igroup [{}] with LUN number [{}]", lunName, accessGroupName, lunNumber);
            return lunNumber;
        }

        // Create if not exists
        Map<String, String> enableMap = Map.of(
                Constants.LUN_DOT_NAME, lunName,
                Constants.SVM_DOT_NAME, svmName,
                Constants.IGROUP_DOT_NAME, accessGroupName
        );
        Map<String, String> response = enableLogicalAccess(enableMap);
        if (response == null || !response.containsKey(Constants.LOGICAL_UNIT_NUMBER)) {
            throw new CloudRuntimeException("ensureLunMapped: Failed to map LUN [" + lunName + "] to iGroup [" + accessGroupName + "]");
        }
        s_logger.info("ensureLunMapped: Successfully mapped LUN [{}] to igroup [{}] with LUN number [{}]", lunName, accessGroupName, response.get(Constants.LOGICAL_UNIT_NUMBER));
        return response.get(Constants.LOGICAL_UNIT_NUMBER);
    }

    @Override
    public boolean validateInitiatorInAccessGroup(String hostInitiator, String svmName, String accessGroupName) {
        s_logger.info("validateInitiatorInAccessGroup: Validating initiator [{}] is in igroup [{}] on SVM [{}]", hostInitiator, accessGroupName, svmName);

        if (hostInitiator == null || hostInitiator.isEmpty()) {
            s_logger.warn("validateInitiatorInAccessGroup: host initiator is null or empty");
            return false;
        }

        Map<String, String> getAccessGroupMap = Map.of(
                Constants.NAME, accessGroupName,
                Constants.SVM_DOT_NAME, svmName
        );
        AccessGroup accessGroup = getAccessGroup(getAccessGroupMap);
        if (accessGroup == null || accessGroup.getIgroup() == null) {
            s_logger.warn("validateInitiatorInAccessGroup: iGroup [{}] not found on SVM [{}]", accessGroupName, svmName);
            return false;
        }

        Igroup igroup = accessGroup.getIgroup();
        if (igroup.getInitiators() != null) {
            for (Initiator initiator : igroup.getInitiators()) {
                if (initiator.getName().equalsIgnoreCase(hostInitiator)) {
                    s_logger.info("validateInitiatorInAccessGroup: Initiator [{}] validated successfully in igroup [{}]", hostInitiator, accessGroupName);
                    return true;
                }
            }
        }
        s_logger.warn("validateInitiatorInAccessGroup: Initiator [{}] NOT found in igroup [{}]", hostInitiator, accessGroupName);
        return false;
    }
}
