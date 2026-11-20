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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.FeignClientFactory;
import org.apache.cloudstack.storage.feign.client.AggregateFeignClient;
import org.apache.cloudstack.storage.feign.client.ClusterFeignClient;
import org.apache.cloudstack.storage.feign.client.EmsFeignClient;
import org.apache.cloudstack.storage.feign.client.JobFeignClient;
import org.apache.cloudstack.storage.feign.client.NASFeignClient;
import org.apache.cloudstack.storage.feign.client.NetworkFeignClient;
import org.apache.cloudstack.storage.feign.client.QosFeignClient;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.client.SnapshotFeignClient;
import org.apache.cloudstack.storage.feign.client.SvmFeignClient;
import org.apache.cloudstack.storage.feign.client.VolumeFeignClient;
import org.apache.cloudstack.storage.feign.model.Aggregate;
import org.apache.cloudstack.storage.feign.model.Cluster;
import org.apache.cloudstack.storage.feign.model.ClusterNode;
import org.apache.cloudstack.storage.feign.model.EmsApplicationLog;
import org.apache.cloudstack.storage.feign.model.IpInterface;
import org.apache.cloudstack.storage.feign.model.IscsiService;
import org.apache.cloudstack.storage.feign.model.Job;
import org.apache.cloudstack.storage.feign.model.Nas;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.Version;
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.VolumeQosPolicy;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

import feign.FeignException;

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
    protected QosFeignClient qosFeignClient;
    protected SANFeignClient sanFeignClient;
    protected NASFeignClient nasFeignClient;
    protected SnapshotFeignClient snapshotFeignClient;
    protected ClusterFeignClient clusterFeignClient;
    protected EmsFeignClient emsFeignClient;

    protected OntapStorage storage;

    /**
     * Presents aggregate object for the unified storage, not eligible for disaggregated
     */
    private List<Aggregate> aggregates;
    private String resolvedSvmUuid;

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
        this.qosFeignClient = feignClientFactory.createClient(QosFeignClient.class, baseURL);
        this.sanFeignClient = feignClientFactory.createClient(SANFeignClient.class, baseURL);
        this.nasFeignClient = feignClientFactory.createClient(NASFeignClient.class, baseURL);
        this.snapshotFeignClient = feignClientFactory.createClient(SnapshotFeignClient.class, baseURL);
        this.clusterFeignClient = feignClientFactory.createClient(ClusterFeignClient.class, baseURL);
        this.emsFeignClient = feignClientFactory.createClient(EmsFeignClient.class, baseURL);
    }

    /**
     * Fetches the ONTAP {@link Cluster} (name, uuid, version) and, for ASUP, the rolled-up node
     * {@code model} and {@code platformType} from {@code GET /api/cluster/nodes}. Best-effort:
     * returns {@code null} if the cluster GET fails. Node fields are left unset if the nodes GET
     * fails. Callers must never fail a storage operation because of this.
     *
     * @return the ONTAP {@link Cluster}, or {@code null} if it cannot be resolved
     */
    public Cluster getClusterInfo() {
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            Cluster cluster = clusterFeignClient.getCluster(authHeader, true);
            if (cluster != null) {
                populateNodeAsupFields(cluster, authHeader);
            }
            return cluster;
        } catch (Exception e) {
            logger.warn("getClusterInfo: failed to fetch ONTAP cluster info for storage IP {}: {}",
                    storage.getStorageIP(), e.getMessage());
            return null;
        }
    }

    /**
     * Sets {@link Cluster#setModel(String)} and {@link Cluster#setPlatformType(String)} from one
     * {@code GET /api/cluster/nodes}. Distinct models are joined with a comma. Platform type is
     * {@code performance}, {@code capacity}, {@code fas}, or {@code composite} when personalities mix.
     * A node that omits {@code is_all_flash_optimized} is left out of the rollup so a partial
     * response is not reported as {@code fas}.
     */
    private void populateNodeAsupFields(Cluster cluster, String authHeader) {
        try {
            Map<String, Object> query = new HashMap<>();
            query.put(OntapStorageConstants.FIELDS, OntapStorageConstants.CLUSTER_NODE_ASUP_FIELDS);
            OntapResponse<ClusterNode> response = clusterFeignClient.getClusterNodes(authHeader, query);
            if (response == null || response.getRecords() == null || response.getRecords().isEmpty()) {
                return;
            }
            LinkedHashSet<String> models = new LinkedHashSet<>();
            LinkedHashSet<String> platformTypes = new LinkedHashSet<>();
            for (ClusterNode node : response.getRecords()) {
                if (node == null) {
                    continue;
                }
                if (node.getModel() != null && !node.getModel().isBlank()) {
                    models.add(node.getModel().trim());
                }
                String platformType = classifyNodePlatformType(node);
                if (platformType != null) {
                    platformTypes.add(platformType);
                }
            }
            if (!models.isEmpty()) {
                cluster.setModel(String.join(OntapStorageConstants.COMMA, models));
            }
            cluster.setPlatformType(rollupPlatformType(platformTypes));
        } catch (Exception e) {
            logger.warn("getClusterInfo: failed to fetch ONTAP node hardware for storage IP {}: {}",
                    storage.getStorageIP(), e.getMessage());
        }
    }

    /**
     * Extracts a clean, parser-friendly ONTAP version string from a {@link Cluster}.
     *
     * <p>Prefers the compact "generation.major.minor" numeric form (e.g. "9.17.1"), which avoids
     * the colon/date noise in {@code version.full}. Falls back to the verbose {@code version.full}
     * banner only when the numeric fields are unavailable.</p>
     *
     * @param cluster the cluster (may be {@code null})
     * @return the ONTAP version string, or {@code null} if it cannot be resolved
     */
    public String getClusterVersion(Cluster cluster) {
        if (cluster == null || cluster.getVersion() == null) {
            return null;
        }
        Version version = cluster.getVersion();
        if (version.getGeneration() != null && version.getMajor() != null && version.getMinor() != null) {
            return version.getGeneration() + OntapStorageConstants.DOT + version.getMajor()
                    + OntapStorageConstants.DOT + version.getMinor();
        }
        if (version.getFull() != null && !version.getFull().isEmpty()) {
            return version.getFull();
        }
        return null;
    }

    /**
     * Classifies one node: not all-flash → {@code fas}; all-flash + capacity → {@code capacity};
     * otherwise all-flash (including performance-optimized or classic AFF) → {@code performance}.
     * Returns {@code null} when {@code is_all_flash_optimized} is absent, so unknown hardware is
     * not reported as {@code fas}.
     */
    private static String classifyNodePlatformType(ClusterNode node) {
        if (node.getAllFlashOptimized() == null) {
            return null;
        }
        if (!node.getAllFlashOptimized()) {
            return OntapStorageConstants.ASUP_PLATFORM_TYPE_FAS;
        }
        if (Boolean.TRUE.equals(node.getCapacityOptimized())) {
            return OntapStorageConstants.ASUP_PLATFORM_TYPE_CAPACITY;
        }
        return OntapStorageConstants.ASUP_PLATFORM_TYPE_PERFORMANCE;
    }

    private static String rollupPlatformType(LinkedHashSet<String> platformTypes) {
        if (platformTypes == null || platformTypes.isEmpty()) {
            return null;
        }
        if (platformTypes.size() == 1) {
            return platformTypes.iterator().next();
        }
        return OntapStorageConstants.ASUP_PLATFORM_TYPE_COMPOSITE;
    }

    /**
     * True when every cluster node reports {@code is_all_flash_optimized} (AFF, including C-series).
     * A FAS node, or a node that omits the flag, makes this false. Used for min-throughput QoS support.
     */
    public boolean isAff() {
        Map<String, Object> query = new HashMap<>();
        query.put(OntapStorageConstants.FIELDS, OntapStorageConstants.CLUSTER_NODE_ASUP_FIELDS);
        OntapResponse<ClusterNode> response = clusterFeignClient.getClusterNodes(getAuthHeader(), query);
        if (response == null || response.getRecords() == null
                || response.getRecords().stream().allMatch(node -> node == null)) {
            throw new CloudRuntimeException(
                    "Unable to determine whether the ONTAP cluster is AFF or FAS");
        }
        for (ClusterNode node : response.getRecords()) {
            if (node == null || !Boolean.TRUE.equals(node.getAllFlashOptimized())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pushes a single ASUP (AutoSupport) EMS application-log message to the ONTAP cluster.
     *
     * <p>This is strictly best-effort telemetry: any failure is logged and swallowed so that
     * it can never affect a storage operation or the periodic scheduler.</p>
     *
     * @param message the EMS message to send
     */
    public void sendAsupMessage(EmsApplicationLog message) {
        if (message == null) {
            return;
        }
        try {
            String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
            emsFeignClient.sendEmsApplicationLog(authHeader, message);
            logger.debug("sendAsupMessage: ASUP EMS message [event-id={}] sent to ONTAP cluster at {}",
                    message.getEventId(), storage.getStorageIP());
        } catch (Exception e) {
            logger.error("sendAsupMessage: failed to send ASUP EMS message [event-id={}] to ONTAP cluster at {}: {}",
                    message.getEventId(), storage.getStorageIP(), e.getMessage());
        }
    }

    /**
     * Validates ONTAP cluster reachability, credentials, SVM state, protocol, and aggregate capacity
     * for new FlexVol creation (primary pool provisioning).
     */
    public boolean connect() {
        return connect(true);
    }

    /**
     * Validates ONTAP cluster reachability and SVM/protocol settings.
     *
     * <p>Aggregate free-space checks apply only when {@code validateAggregatesForVolumeCreation} is
     * {@code true} (pool provisioning). Snapshot, delete, revert, and grant/revoke paths must use
     * {@code false} — they operate on an existing FlexVol and must not compare aggregate space to
     * the full pool capacity stored in pool details.</p>
     */
    public boolean connect(boolean validateAggregatesForVolumeCreation) {
        logger.info("Attempting to connect to ONTAP cluster at " + storage.getStorageIP() + " and validate SVM " +
                storage.getSvmName() + ", protocol " + storage.getProtocol()
                + (validateAggregatesForVolumeCreation ? " (with aggregate validation)" : " (operations only)"));
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        String svmName = storage.getSvmName();
        try {
            // Call the SVM API to check if the SVM exists
            Svm svm = new Svm();
            logger.info("Fetching the SVM details...");
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.NAME, svmName, OntapStorageConstants.FIELDS, OntapStorageConstants.AGGREGATES +
                    OntapStorageConstants.COMMA + OntapStorageConstants.STATE +
                    OntapStorageConstants.COMMA +  OntapStorageConstants.NFS_ENABLED + OntapStorageConstants.COMMA + OntapStorageConstants.ISCSI_ENABLED);
            OntapResponse<Svm> svms = svmFeignClient.getSvmResponse(queryParams, authHeader);
            if (svms != null && svms.getRecords() != null && !svms.getRecords().isEmpty()) {
                svm = svms.getRecords().get(0);
            } else {
                logger.error("No SVM found on the ONTAP cluster by the name " + svmName + ".");
                throw new CloudRuntimeException("No SVM found on the ONTAP cluster by the name " + svmName + ".");
            }

            logger.info("Validating SVM state and protocol settings...");
            if (!Objects.equals(svm.getState(), OntapStorageConstants.RUNNING)) {
                logger.error("SVM " + svmName + " is not in running state.");
                throw new CloudRuntimeException("SVM " + svmName + " is not in running state.");
            }
            if (Objects.equals(storage.getProtocol(), ProtocolType.NFS3) && !svm.getNfsEnabled()) {
                logger.error("NFS protocol is not enabled on SVM " + svmName);
                throw new CloudRuntimeException("NFS protocol is not enabled on SVM " + svmName);
            } else if (Objects.equals(storage.getProtocol(), ProtocolType.ISCSI) && !svm.getIscsiEnabled()) {
                logger.error("ISCSI protocol is not enabled on SVM " + svmName);
                throw new CloudRuntimeException("ISCSI protocol is not enabled on SVM " + svmName);
            }
            this.resolvedSvmUuid = svm.getUuid();

            if (validateAggregatesForVolumeCreation) {
                validateAndSelectAggregatesForVolumeCreation(authHeader, svmName, svm.getAggregates());
            } else {
                logger.debug("Skipping aggregate capacity validation — not required for existing-volume operations");
            }

            logger.info("Successfully connected to ONTAP cluster and validated ONTAP details provided");
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (FeignException.Unauthorized e) {
            String msg = "Authentication failed: Invalid credentials. Please verify the username and password.";
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            logger.error("Failed to connect to ONTAP cluster: " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to connect to ONTAP cluster: " + e.getMessage(), e);
        }
        return true;
    }

    /**
     * ONTAP SVM UUID resolved during the last successful {@link #connect(boolean)} call.
     */
    public String getResolvedSvmUuid() {
        return resolvedSvmUuid;
    }

    /**
     * Aggregates eligible for new FlexVol creation, populated by {@link #connect(boolean)} with aggregate validation enabled.
     */
    public List<Aggregate> getAggregates() {
        return aggregates;
    }

    public void setAggregates(List<Aggregate> aggregates) {
        this.aggregates = aggregates;
    }

    private void validateAndSelectAggregatesForVolumeCreation(String authHeader, String svmName, List<Aggregate> aggrs) {
        if (aggrs == null || aggrs.isEmpty()) {
            logger.error("No aggregates are assigned to SVM " + svmName);
            throw new CloudRuntimeException("No aggregates are assigned to SVM " + svmName);
        }
        List<Aggregate> eligibleAggregates = new ArrayList<>();
        for (Aggregate aggr : aggrs) {
            logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
            Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid(),
                        Map.of(OntapStorageConstants.FIELDS, OntapStorageConstants.AGGREGATE_NODE
                                + OntapStorageConstants.COMMA + OntapStorageConstants.AGGREGATE_SPACE
                                + OntapStorageConstants.COMMA + OntapStorageConstants.STATE));
            if (aggrResp == null) {
                logger.warn("Aggregate details response is null for aggregate " + aggr.getName() + ". Skipping.");
                continue;
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
            eligibleAggregates.add(aggr);
        }
        if (eligibleAggregates.isEmpty()) {
            logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
            throw new CloudRuntimeException("No suitable aggregates found on SVM " + svmName + " for volume creation.");
        }
        setAggregates(eligibleAggregates);
    }

    // Common methods like create/delete etc., should be here

    /**
     * Selects the best aggregate for a volume of the given size from the given candidate aggregates.
     *
     * <p>Picks the online aggregate with the largest available block space that can fit
     * {@code size}. The returned aggregate includes node information for LIF affinity.</p>
     *
     * @param aggregates candidate aggregates, for example {@link #getAggregates()}
     * @param size requested volume size in bytes
     * @return the chosen aggregate detail response
     */
    public Aggregate chooseAggregate(List<Aggregate> aggregates, Long size) {
        String svmName = storage.getSvmName();
        if (aggregates == null || aggregates.isEmpty()) {
            logger.error("No aggregates available to create volume on SVM " + svmName);
            throw new CloudRuntimeException("No aggregates available to create volume on SVM " + svmName);
        }
        if (size == null || size <= 0) {
            throw new CloudRuntimeException("Invalid volume size provided: " + size);
        }

        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        long maxAvailableAggregateSpaceBytes = -1L;
        Aggregate aggrChosen = null;
        for (Aggregate aggr : aggregates) {
            logger.debug("Found aggregate: " + aggr.getName() + " with UUID: " + aggr.getUuid());
            Aggregate aggrResp = aggregateFeignClient.getAggregateByUUID(authHeader, aggr.getUuid(),
                    Map.of(OntapStorageConstants.FIELDS, OntapStorageConstants.AGGREGATE_NODE
                            + OntapStorageConstants.COMMA + OntapStorageConstants.AGGREGATE_SPACE
                            + OntapStorageConstants.COMMA + OntapStorageConstants.STATE));

            if (aggrResp == null) {
                logger.warn("Aggregate details response is null for aggregate " + aggr.getName() + ". Skipping.");
                continue;
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
                aggrChosen = aggrResp;
            }
        }

        if (aggrChosen == null) {
            logger.error("No suitable aggregates found on SVM " + svmName + " for volume creation.");
            throw new CloudRuntimeException("No suitable aggregates found on SVM " + svmName + " for volume operations.");
        }
        if (aggrChosen.getNode() == null || aggrChosen.getNode().getName() == null
                || aggrChosen.getNode().getName().isEmpty()) {
            logger.error("Selected aggregate " + aggrChosen.getName() + " does not have a node name.");
            throw new CloudRuntimeException("Selected aggregate " + aggrChosen.getName()
                    + " does not have a node name required for LIF affinity.");
        }
        logger.info("Selected aggregate: " + aggrChosen.getName() + " on node "
                + aggrChosen.getNode().getName() + " for volume operations.");
        return aggrChosen;
    }

    /**
     * Creates ONTAP Flex-Volume on the given aggregate.
     * Eligible only for Unified ONTAP storage
     * throw exception in case of disaggregated ONTAP storage
     *
     * @param volumeName the name of the volume to create
     * @param size the size of the volume in bytes
     * @param aggregate the aggregate previously selected via {@link #chooseAggregate(Long)}
     * @return the created Volume object
     */
    public Volume createStorageVolume(String volumeName, Long size, Aggregate aggregate) {
        logger.info("Creating volume: " + volumeName + " of size: " + size + " bytes");

        String svmName = storage.getSvmName();
        if (size == null || size <= 0) {
            throw new CloudRuntimeException("Invalid volume size provided: " + size);
        }
        if (aggregate == null || aggregate.getName() == null || aggregate.getUuid() == null) {
            throw new CloudRuntimeException("Aggregate is required to create volume on SVM " + svmName);
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

        logger.info("Creating volume on aggregate: " + aggregate.getName() + " for volume operations.");

        Aggregate aggr = new Aggregate();
        aggr.setName(aggregate.getName());
        aggr.setUuid(aggregate.getUuid());
        volumeRequest.setAggregates(List.of(aggr));
        volumeRequest.setSize(size);
        volumeRequest.setNas(nas);
        volumeRequest.setGuarantee(new Volume.Guarantee(Volume.Guarantee.TypeEnum.NONE));
        try {
            JobResponse jobResponse = volumeFeignClient.createVolumeWithJob(authHeader, volumeRequest);
            if (jobResponse == null || jobResponse.getJob() == null) {
                throw new CloudRuntimeException("Failed to initiate volume creation for " + volumeName);
            }
            String jobUUID = jobResponse.getJob().getUuid();

            Boolean jobSucceeded = jobPollForSuccess(jobUUID,OntapStorageConstants.ONTAP_FLEXVOL_RESOLVE_MAX_RETRIES, OntapStorageConstants.ONTAP_FLEXVOL_JOB_POLL_INTERVAL_MS);
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
        logger.info("Resizing ONTAP FlexVolume '{}' (UUID: {}) to {} bytes", volume.getName(), volume.getUuid(), volume.getSize());
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            Volume resizeRequest = new Volume();
            resizeRequest.setSize(volume.getSize());
            JobResponse jobResponse = volumeFeignClient.updateVolume(authHeader, volume.getUuid(), resizeRequest);
            pollJobIfPresent(jobResponse, "resize FlexVolume [" + volume.getUuid() + "]",
                    OntapStorageConstants.ONTAP_VOLUME_JOB_MAX_RETRIES, OntapStorageConstants.ONTAP_VOLUME_JOB_POLL_INTERVAL_MS);
            logger.info("FlexVolume '{}' (UUID: {}) resized successfully to {} bytes", volume.getName(), volume.getUuid(), volume.getSize());
        } catch (FeignException e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                String msg = String.format("Cannot resize FlexVolume '%s' (UUID: %s): volume not found on ONTAP (404). ", volume.getName(), volume.getUuid());
                logger.error(msg);
                throw new CloudRuntimeException(msg, e);
            }
            logger.error("Exception while resizing FlexVolume '{}' (UUID: {}): {}", volume.getName(), volume.getUuid(), e.getMessage(), e);
            throw new CloudRuntimeException("Failed to resize ONTAP FlexVolume: " + e.getMessage(), e);
        }
        return volume;
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
            JobResponse jobResponse = volumeFeignClient.deleteVolume(authHeader, volume.getUuid());
            Boolean jobSucceeded = jobPollForSuccess(jobResponse.getJob().getUuid(), OntapStorageConstants.ONTAP_FLEXVOL_RESOLVE_MAX_RETRIES, OntapStorageConstants.ONTAP_FLEXVOL_JOB_POLL_INTERVAL_MS);
            if (!jobSucceeded) {
                logger.error("Volume deletion job failed for volume: " + volume.getName());
                throw new CloudRuntimeException("Volume deletion job failed for volume: " + volume.getName());
            }
            logger.info("Volume deleted successfully: " + volume.getName());
        } catch (FeignException e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                logger.warn("deleteStorageVolume: Volume '{}' not found in ONTAP, treating as no-op", volume.getName());
                return;
            }
            logger.error("Exception while deleting volume: ", e);
            throw new CloudRuntimeException("Failed to delete volume: " + e.getMessage());
        }
        logger.info("ONTAP volume deletion process completed for volume: " + volume.getName());
    }

    /**
     * Gets an ONTAP FlexVolume by UUID.
     * Eligible only for Unified ONTAP storage.
     * Throws exception in case of disaggregated ONTAP storage.
     *
     * <p>Callers pass ONTAP query parameters when they need a partial record. For example,
     * {@code fields=space.used} returns only used space instead of the full volume.
     * A null or empty map returns the default volume representation.</p>
     *
     * @param uuid the UUID of the volume to retrieve
     * @param queryParams ONTAP query parameters applied to {@code GET /storage/volumes/{uuid}};
     *                    may be null
     * @return the retrieved Volume object, or null if not found
     */
    public Volume getStorageVolume(String uuid, Map<String, Object> queryParams) {
        if (StringUtils.isBlank(uuid)) {
            throw new CloudRuntimeException("Cannot fetch ONTAP volume: UUID is null or empty");
        }
        Map<String, Object> params = queryParams != null ? queryParams : Collections.emptyMap();
        logger.info("getStorageVolume: Fetching ONTAP volume [{}] with query params {}", uuid, params);
        String authHeader = OntapStorageUtils.generateAuthHeader(storage.getUsername(), storage.getPassword());
        try {
            Volume fetchedVolume = volumeFeignClient.getVolumeByUUID(authHeader, uuid, params);
            logger.info("getStorageVolume: Volume [{}] fetched successfully", uuid);
            return fetchedVolume;
        } catch (FeignException e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                logger.warn("getStorageVolume: Volume [{}] not found in ONTAP", uuid);
                return null;
            }
            logger.error("getStorageVolume: Exception while fetching volume [{}]: ", uuid, e);
            throw new CloudRuntimeException("Failed to fetch volume: " + e.getMessage());
        }
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
     * Selects the best available data LIF for storage I/O, preferring one homed on the same node
     * as the given aggregate to avoid inter-node traffic.
     *
     * <p>Selection order:</p>
     * <ol>
     *   <li>LIF whose {@code location.home_node} matches the aggregate's node — no warning</li>
     *   <li>LIF currently running on that node (e.g. after failover) — returned with a warning</li>
     *   <li>Any UP and enabled LIF — returned with a warning</li>
     * </ol>
     *
     * @param aggregate the aggregate previously selected via {@link #chooseAggregate(Long)};
     *                  must include a node name for LIF affinity
     * @return map with {@link OntapStorageConstants#DATA_LIF} set to the LIF IP address, and
     *         optionally {@link OntapStorageConstants#LIF_WARNING} when a non-ideal LIF was selected
     */
    public Map<String, String> getNetworkInterface(Aggregate aggregate) {
        if (aggregate == null || aggregate.getNode() == null || aggregate.getNode().getName() == null
                || aggregate.getNode().getName().isEmpty()) {
            throw new CloudRuntimeException("Aggregate with a node name is required to select a network interface");
        }
        String aggregateNode = aggregate.getNode().getName();
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
            queryParams.put(OntapStorageConstants.FIELDS,
                    OntapStorageConstants.IP_ADDRESS + OntapStorageConstants.COMMA
                    + OntapStorageConstants.STATE + OntapStorageConstants.COMMA
                    + OntapStorageConstants.LIF_ENABLED + OntapStorageConstants.COMMA
                    + OntapStorageConstants.LIF_LOCATION_HOME_NODE + OntapStorageConstants.COMMA
                    + OntapStorageConstants.LIF_LOCATION_NODE);
            queryParams.put(OntapStorageConstants.RETURN_RECORDS, OntapStorageConstants.TRUE);
            OntapResponse<IpInterface> response =
                    networkFeignClient.getNetworkIpInterfaces(authHeader, queryParams);
            if (response == null || response.getRecords() == null || response.getRecords().isEmpty()) {
                throw new CloudRuntimeException("No network interfaces found for SVM " + storage.getSvmName() +
                        " for protocol " + storage.getProtocol());
            }

            IpInterface currentNodeInterface = null;
            IpInterface fallbackInterface = null;

            for (IpInterface iface : response.getRecords()) {
                if (iface == null || iface.getIp() == null) {
                    continue;
                }
                if (!Boolean.TRUE.equals(iface.getEnabled()) || !OntapStorageConstants.LIF_STATE_UP.equals(iface.getState())) {
                    continue;
                }
                if (!isIPv4Address(iface.getIp().getAddress())) {
                    continue;
                }
                // LIF is homed on the aggregate's node
                String homeNode = iface.getLocation() != null && iface.getLocation().getHomeNode() != null
                        ? iface.getLocation().getHomeNode().getName() : null;
                if (aggregateNode.equals(homeNode)) {
                    return networkInterfaceResult(iface.getIp().getAddress(), null);
                }
                // LIF has failed over and is currently running on the aggregate's node
                // (home_node differs). Keep as a candidate; returned with a warning if no match is found earlier.
                if (currentNodeInterface == null) {
                    String currentNode = iface.getLocation() != null && iface.getLocation().getNode() != null
                            ? iface.getLocation().getNode().getName() : null;
                    if (aggregateNode.equals(currentNode)) {
                        currentNodeInterface = iface;
                    }
                }
                if (fallbackInterface == null) {
                    fallbackInterface = iface;
                }
            }

            if (currentNodeInterface == null && fallbackInterface == null) {
                throw new CloudRuntimeException("No operationally UP and enabled LIF found for SVM '"
                        + storage.getSvmName() + "' with protocol " + storage.getProtocol()
                        + " — all " + response.getRecords().size() + " LIF(s) are either administratively disabled or operationally down");
            }

            if (currentNodeInterface != null) {
                String ip = currentNodeInterface.getIp().getAddress();
                String warning = "No home-node LIF found for aggregate node '" + aggregateNode
                        + "'; using LIF '" + ip + "' currently running on that node (home node LIF may be down).";
                logger.warn(warning);
                return networkInterfaceResult(ip, warning);
            }

            String ip = fallbackInterface.getIp().getAddress();
            String warning = "No operational LIF found on aggregate's home node '" + aggregateNode
                    + "'; using fallback LIF '" + ip + "' on a different node."
                    + " I/O will traverse an inter-node path, increasing latency.";
            logger.warn(warning);
            return networkInterfaceResult(ip, warning);
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Exception while retrieving network interfaces: ", e);
            throw new CloudRuntimeException("Failed to retrieve network interfaces: " + e.getMessage());
        }
    }

    private Map<String, String> networkInterfaceResult(String address, String warning) {
        Map<String, String> result = new HashMap<>();
        result.put(OntapStorageConstants.DATA_LIF, address);
        if (warning != null) {
            result.put(OntapStorageConstants.LIF_WARNING, warning);
        }
        return result;
    }

    /**
     * Returns true if the given IP address string is an IPv4 address.
     * IPv6 addresses contain colons; IPv4 addresses do not.
     * To extend LIF selection to support IPv6, update this method and its call site in getNetworkInterface().
     */
    private boolean isIPv4Address(String address) {
        return address != null && !address.contains(":");
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
     * Creates the protocol-specific backend object that caches a template on this pool.
     *
     * <p>iSCSI creates an empty LUN ({@code /vol/&lt;flexVol&gt;/cs_tmpl_&lt;id&gt;}) sized to
     * {@code sizeInBytes}. NFS is a no-op on the array: the KVM agent later writes the qcow2
     * into the mounted FlexVolume.</p>
     *
     * <p>Returns a {@link CloudStackVolume} so the driver can map it to {@code CreateCmdResult}
     * and update {@code template_spool_ref}. SAN populates {@code lun}; NAS returns an empty
     * volume (no LUN / file yet).</p>
     *
     * @param storagePool   CloudStack primary storage pool (one FlexVolume)
     * @param templateInfo  template being cached
     * @param details       pool details (SVM, protocol, etc.)
     * @param sizeInBytes   virtual size for the cache object (required for SAN; ignored for NAS)
     * @return created cache identity, or an empty {@link CloudStackVolume} when nothing is
     *         pre-created on the array
     */
    abstract public CloudStackVolume createTemplateCache(StoragePoolVO storagePool, TemplateInfo templateInfo,
            Map<String, String> details, long sizeInBytes);

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
    public abstract CloudStackVolume updateCloudStackVolume(CloudStackVolume cloudstackVolume);

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
     * Creates a space-efficient clone of an existing object inside the same FlexVolume.
     * it is going to mimic
     *     cloneLun       for iSCSI, FC protocols
     *     cloneFile      for NFS3.0 and NFS4.1 protocols
     *     cloneNameSpace for Nvme/TCP and Nvme/FC protocol
     *
     * <p>ONTAP requires the source and the destination to live in the same FlexVolume, which
     * holds because a CloudStack primary storage pool maps one-to-one onto a FlexVolume.</p>
     *
     * <p>The source may be an active object or one inside a FlexVolume snapshot:</p>
     * <ul>
     *   <li><b>SAN</b> — {@code lun.clone.source.name} set to
     *       {@code /vol/&lt;fv&gt;/.snapshot/&lt;snap&gt;/&lt;lun&gt;}</li>
     *   <li><b>NAS</b> — {@code file.path} set to
     *       {@code ./.snapshot/&lt;snapshot-name&gt;/&lt;source-file&gt;} and sent as
     *       {@code source_path} on {@code POST /api/storage/file/clone}</li>
     * </ul>
     *
     * @param cloudstackVolume describes the clone to create; the source is carried in the
     *                         protocol-specific clone reference (for SAN, {@code lun.clone.source})
     * @return the created CloudStackVolume, populated with the backend identity of the clone
     */
    abstract public CloudStackVolume cloneCloudStackVolume(CloudStackVolume cloudstackVolume);

    /**
     * Copies an existing volume onto the destination pool's FlexVolume as a full, independent object.
     * it is going to mimic
     *     copyLun       for iSCSI, FC protocols (ONTAP {@code copy.source} LUN create)
     *     copyFile      for NFS3.0 and NFS4.1 protocols
     *     copyNameSpace for Nvme/TCP and Nvme/FC protocol
     *
     * <p>Unlike {@link #cloneCloudStackVolume(CloudStackVolume)}, the copy may cross FlexVolumes on the
     * same SVM. Each strategy builds its own protocol-specific request from the given volumes.</p>
     *
     * @param srcVolumeInfo   volume being copied
     * @param destVolumeInfo  destination volume row created for the copy
     * @param destStoragePool destination primary storage pool
     * @param destDetails     destination pool details (SVM, protocol, etc.)
     * @param timeoutSeconds  maximum time to wait for the copy to finish
     * @return the copied CloudStackVolume, populated with the backend identity of the copy
     */
    abstract public CloudStackVolume copyCloudStackVolume(VolumeInfo srcVolumeInfo, VolumeInfo destVolumeInfo,
            StoragePoolVO destStoragePool, Map<String, String> destDetails, int timeoutSeconds);

    /**
     * Grows an existing backend object to {@code sizeInBytes}.
     *
     * <p>Needed after cloning a cached template, because a clone inherits the size of its source
     * while the service offering may ask for a larger disk.</p>
     *
     * <p>When the request already carries a QoS policy on the LUN or file, that policy is applied
     * as part of this resize. iSCSI sends one LUN update with the new size and the policy.
     * NFS grows the file on the host, then updates the file policy. A request with no policy
     * changes size only.</p>
     */
    abstract public void resizeCloudStackVolume(CloudStackVolume cloudstackVolume, long sizeInBytes);

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
     * Reverts a CloudStack volume to a snapshot using ONTAP CLI-based Single File Snap Restore (SFSR).
     *
     * <p>Both NFS and iSCSI use the CLI passthrough API:
     * {@code POST /api/private/cli/volume/snapshot/restore-file}</p>
     *
     * <p>Callers should invoke {@link #executeCliSfsrRestore(JobResponse, String)} after this
     * method returns to poll the async job when present, or treat a missing job as synchronous success.</p>
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
    public abstract AccessGroup updateAccessGroup(AccessGroup accessGroup);

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
     * @return String containing logical unit number for the new/existing mapping (SAN) or relevant info for NAS
     */
    abstract public String enableLogicalAccess(Map<String,String> values);

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
     * @return String containing logical unit number if mapping exists; otherwise null
     */
    abstract public String getLogicalAccess(Map<String, String> values);

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

    public VolumeQosPolicy createVolumeQosPolicy(String policyName, Long minIops, Long maxIops) {
        VolumeQosPolicy policy = buildVolumeQosPolicy(policyName, minIops, maxIops);
        Svm svm = new Svm();
        svm.setName(storage.getSvmName());
        policy.setSvm(svm);
        try {
            JobResponse response = qosFeignClient.createPolicy(getAuthHeader(), policy);
            pollJobIfPresent(response, "create QoS policy [" + policyName + "]");
        } catch (FeignException e) {
            if (e.status() != 409) {
                throw new CloudRuntimeException("Failed to create ONTAP QoS policy [" + policyName + "]: "
                        + e.getMessage(), e);
            }
            logger.info("QoS policy [{}] already exists; using the existing policy", policyName);
        }

        VolumeQosPolicy createdPolicy = getVolumeQosPolicy(policyName);
        return createdPolicy;
    }

    public void deleteVolumeQosPolicy(String policyUuid) {
        if (policyUuid == null || policyUuid.isEmpty()) {
            return;
        }
        VolumeQosPolicy policy = getVolumeQosPolicyByUuid(policyUuid);
        if (policy == null) {
            logger.info("QoS policy [{}] not found on ONTAP; skipping delete", policyUuid);
            return;
        }
        if (policy.getObjectCount() != null && policy.getObjectCount() > 0) {
            logger.info("QoS policy [{}] still has object_count={}; skipping delete",
                    policyUuid, policy.getObjectCount());
            return;
        }
        try {
            JobResponse response = qosFeignClient.deletePolicy(getAuthHeader(), policyUuid);
            pollJobIfPresent(response, "delete QoS policy [" + policyUuid + "]");
        } catch (Exception e) {
            if ((e instanceof FeignException && ((FeignException) e).status() == 409)
                    || OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                logger.info("QoS policy [{}] was not deleted on ONTAP (already absent or conflict): {}",
                        policyUuid, e.getMessage());
                return;
            }
            throw new CloudRuntimeException("Failed to delete ONTAP QoS policy [" + policyUuid + "]: "
                    + e.getMessage(), e);
        }
    }

    private VolumeQosPolicy getVolumeQosPolicyByUuid(String policyUuid) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put(OntapStorageConstants.FIELDS, OntapStorageConstants.QOS_POLICY_OBJECT_COUNT_FIELDS);
        try {
            VolumeQosPolicy policy = qosFeignClient.getPolicy(getAuthHeader(), policyUuid, queryParams);
            if (policy == null || StringUtils.isEmpty(policy.getUuid())) {
                throw new CloudRuntimeException("Failed to fetch ONTAP QoS policy [" + policyUuid
                        + "]: empty response");
            }
            return policy;
        } catch (FeignException e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                return null;
            }
            throw new CloudRuntimeException("Failed to fetch ONTAP QoS policy [" + policyUuid + "]: "
                    + e.getMessage(), e);
        }
    }

    private VolumeQosPolicy getVolumeQosPolicy(String policyName) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put(OntapStorageConstants.NAME, policyName);
        queryParams.put(OntapStorageConstants.SVM_DOT_NAME, storage.getSvmName());
        try {
            OntapResponse<VolumeQosPolicy> response = qosFeignClient.getPolicies(getAuthHeader(), queryParams);
            if (response == null || response.getRecords() == null || response.getRecords().size() <= 0) {
                throw new CloudRuntimeException("Unable to get ONTAP QoS policy [" + policyName + "] after creation");
            }
            VolumeQosPolicy policy = response.getRecords().get(0);
            if (policy == null || StringUtils.isEmpty(policy.getUuid())) {
                throw new CloudRuntimeException("Failed to fetch ONTAP QoS policy [" + policyName
                        + "]: empty response");
            }
            return policy;
        } catch (FeignException e) {
            throw new CloudRuntimeException("Failed to fetch ONTAP QoS policy [" + policyName + "]: "
                    + e.getMessage(), e);
        }
    }

    private VolumeQosPolicy buildVolumeQosPolicy(String policyName, Long minIops, Long maxIops) {
        VolumeQosPolicy.Fixed fixed = new VolumeQosPolicy.Fixed();
        fixed.setCapacityShared(false);
        // ONTAP rejects a policy whose throughput limit is zero, so only unlimited-side values are omitted.
        if (minIops != null && minIops > 0) {
            fixed.setMinThroughputIops(minIops);
        }
        if (maxIops != null && maxIops > 0) {
            fixed.setMaxThroughputIops(maxIops);
        }

        VolumeQosPolicy policy = new VolumeQosPolicy();
        policy.setName(policyName);
        policy.setFixed(fixed);
        return policy;
    }

    /**
     * Polls an ONTAP async job for successful completion.
     *
     * @param jobUUID          UUID of the ONTAP job to poll
     * @param maxRetries       maximum number of poll attempts
     * @param sleepTimeInMilliSecs  sleep between poll attempts
     * @return true if the job completed successfully
     */
    public Boolean jobPollForSuccess(String jobUUID, int maxRetries, int sleepTimeInMilliSecs) {
        return jobPollUntilSuccess(jobUUID, maxRetries, sleepTimeInMilliSecs) != null;
    }

    /**
     * Polls an ONTAP async job until it succeeds and returns the completed job record.
     */
    public Job jobPollUntilSuccess(String jobUUID, int maxRetries, int sleepTimeInMilliSecs) {
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
                Thread.sleep(sleepTimeInMilliSecs);
            }
            return jobResp;
        } catch (FeignException.FeignClientException e) {
            throw new CloudRuntimeException("Failed to fetch job status: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException("Interrupted while polling ONTAP job " + jobUUID, e);
        }
    }

    /**
     * Polls an ONTAP async job when the API response includes a job reference.
     *
     * <p>When no job is returned (common for CLI passthrough SFSR on synchronous completion),
     * the operation is treated as successful after HTTP 2xx.</p>
     *
     * @param response       ONTAP job response (may be null or without a job)
     * @param operationName  label for logging and error messages
     */
    public void pollJobIfPresent(JobResponse response, String operationName) {
        pollJobIfPresent(response, operationName,
                OntapStorageConstants.ONTAP_CG_JOB_MAX_RETRIES,
                OntapStorageConstants.ONTAP_CG_JOB_POLL_INTERVAL_MS);
    }

    /**
     * Polls an ONTAP async job when present, using caller-supplied retry settings.
     */
    public void pollJobIfPresent(JobResponse response, String operationName,
                                 int maxRetries, int pollIntervalMs) {
        if (response == null || response.getJob() == null || response.getJob().getUuid() == null) {
            logger.debug("pollJobIfPresent: No async job returned for operation [{}], continuing without polling",
                    operationName);
            return;
        }
        jobPollForSuccess(response.getJob().getUuid(), maxRetries, pollIntervalMs);
    }

    /**
     * Polls an ONTAP async job when present and returns the completed job (for extracting created resource UUIDs).
     */
    public Job pollJobIfPresentAndGetCompletedJob(JobResponse response, String operationName) {
        return pollJobIfPresentAndGetCompletedJob(response, operationName,
                OntapStorageConstants.ONTAP_CG_JOB_MAX_RETRIES,
                OntapStorageConstants.ONTAP_CG_JOB_POLL_INTERVAL_MS);
    }

    public Job pollJobIfPresentAndGetCompletedJob(JobResponse response, String operationName,
                                                  int maxRetries, int pollIntervalMs) {
        if (response == null || response.getJob() == null || response.getJob().getUuid() == null) {
            logger.debug("pollJobIfPresentAndGetCompletedJob: No async job for operation [{}]", operationName);
            return null;
        }
        return jobPollUntilSuccess(response.getJob().getUuid(), maxRetries, pollIntervalMs);
    }

    /**
     * Completes CLI-based SFSR ({@code restore-file}) orchestration: poll job when returned,
     * otherwise accept synchronous success.
     */
    public void executeCliSfsrRestore(JobResponse response, String operationName) {
        pollJobIfPresent(response, operationName,
                OntapStorageConstants.ONTAP_SFSR_JOB_MAX_RETRIES,
                OntapStorageConstants.ONTAP_SFSR_JOB_POLL_INTERVAL_MS);
    }

    /**
     * Deletes a FlexVolume snapshot on ONTAP for a CloudStack volume snapshot.
     *
     * <p>ONTAP volume snapshots (NFS and iSCSI) are FlexVol-level snapshots created by
     * {@code POST /storage/volumes/{uuid}/snapshots} during take. Delete uses the matching
     * REST {@code DELETE /storage/volumes/{uuid}/snapshots/{snapshot.uuid}} API regardless
     * of whether the CloudStack volume is a file (NFS) or LUN (iSCSI). Protocol-specific
     * subclasses ({@code UnifiedNASStrategy}, {@code UnifiedSANStrategy}) inherit this
     * implementation; revert/restore remains protocol-specific via SFSR CLI.</p>
     *
     * <p>Called from {@link org.apache.cloudstack.storage.driver.OntapPrimaryDatastoreDriver}
     * during the standard delete chain — not from a separate ONTAP snapshot strategy.</p>
     *
     * @param flexVolUuid   ONTAP FlexVolume UUID
     * @param snapshotUuid  ONTAP FlexVolume snapshot UUID
     * @param snapshotName  ONTAP FlexVolume snapshot name (for logging)
     */
    public void deleteFlexVolSnapshotForCloudStackVolume(String flexVolUuid, String snapshotUuid, String snapshotName) {
        if (flexVolUuid == null || flexVolUuid.isEmpty() || snapshotUuid == null || snapshotUuid.isEmpty()) {
            throw new CloudRuntimeException("FlexVolume UUID and snapshot UUID are required to delete an ONTAP snapshot");
        }

        logger.info("deleteFlexVolSnapshotForCloudStackVolume: issuing ONTAP REST delete for snapshot [{}] "
                + "(uuid={}) on FlexVol [{}]", snapshotName, snapshotUuid, flexVolUuid);

        try {
            JobResponse jobResponse = snapshotFeignClient.deleteSnapshot(getAuthHeader(), flexVolUuid, snapshotUuid);

            if (jobResponse == null || jobResponse.getJob() == null) {
                logger.debug("deleteFlexVolSnapshotForCloudStackVolume: no async job returned for snapshot [{}] "
                        + "(uuid={}); treating HTTP success as completion", snapshotName, snapshotUuid);
            } else {
                logger.debug("deleteFlexVolSnapshotForCloudStackVolume: polling ONTAP delete job [{}] for snapshot [{}]",
                        jobResponse.getJob().getUuid(), snapshotName);
            }

            pollJobIfPresent(jobResponse, "delete FlexVol snapshot [" + snapshotName + "] uuid [" + snapshotUuid + "]",
                    OntapStorageConstants.ONTAP_SNAPSHOT_DELETE_JOB_MAX_RETRIES,
                    OntapStorageConstants.ONTAP_SNAPSHOT_DELETE_JOB_POLL_INTERVAL_MS);

            logger.info("deleteFlexVolSnapshotForCloudStackVolume: ONTAP FlexVol snapshot [{}] (uuid={}) removed from [{}]",
                    snapshotName, snapshotUuid, flexVolUuid);
        } catch (Exception e) {
            if (OntapStorageUtils.isOntapObjectNotFoundError(e)) {
                logger.warn("deleteFlexVolSnapshotForCloudStackVolume: ONTAP snapshot [{}] (uuid={}) on FlexVol [{}] "
                        + "already absent; treating delete as success: {}", snapshotName, snapshotUuid, flexVolUuid,
                        e.getMessage());
                return;
            }
            if (e instanceof CloudRuntimeException) {
                throw (CloudRuntimeException) e;
            }
            throw new CloudRuntimeException("Failed to delete ONTAP FlexVol snapshot [" + snapshotName + "]: "
                    + e.getMessage(), e);
        }
    }
}
