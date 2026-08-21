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

package org.apache.cloudstack.storage.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import feign.FeignException;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Base64Utils;

import com.cloud.alert.AlertManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;

public class OntapStorageUtils {

    private static final Logger logger = LogManager.getLogger(OntapStorageUtils.class);
    private static final String BASIC = "Basic";
    private static final String AUTH_HEADER_COLON = ":";

    /**
     * Method generates authentication headers using storage backend credentials passed as normal string
     *
     * @param username -->> username of the storage backend
     * @param password -->> normal decoded password of the storage backend
     * @return
     */
    public static String generateAuthHeader (String username, String password) {
        byte[] encodedBytes = Base64Utils.encode((username + AUTH_HEADER_COLON + password).getBytes(StandardCharsets.UTF_8));
        return BASIC + StringUtils.SPACE + new String(encodedBytes);
    }

    public static boolean isValidName(String name) {
        // Check for null and length constraint first
        if (name == null || name.length() > 200) {
            return false;
        }
        // Regex: Starts with a letter, followed by letters, digits, or underscores
        return name.matches(OntapStorageConstants.ONTAP_NAME_REGEX);
    }

    public static String getOSTypeFromHypervisor(String hypervisorType) {
        switch (hypervisorType) {
            case OntapStorageConstants.KVM:
                return Lun.OsTypeEnum.LINUX.name();
            default:
                String errMsg = "getOSTypeFromHypervisor : Unsupported hypervisor type " + hypervisorType + " for ONTAP storage";
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
        }
    }

    public static void sendStorageAlert(AlertManager alertMgr, Long zoneId, Long podId, String subject, String body) {
        if (alertMgr != null) {
            alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_STORAGE_MISC, zoneId != null ? zoneId : 0L, podId, subject, body);
        }
    }

    /**
     * Returns a connected {@link StorageStrategy} for operations on an existing pool (snapshots,
     * delete, revert, grant/revoke). Does not require aggregate free space for the full pool size.
     */
    public static StorageStrategy getStrategyByStoragePoolDetails(Map<String, String> details) {
        return getStrategyByStoragePoolDetails(details, false);
    }

    public static StorageStrategy getStrategyByStoragePoolDetails(Map<String, String> details,
            boolean validateAggregatesForVolumeCreation) {
        if (details == null || details.isEmpty()) {
            logger.error("getStrategyByStoragePoolDetails: Storage pool details are null or empty");
            throw new CloudRuntimeException("Storage pool details are null or empty");
        }
        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        OntapStorage ontapStorage = new OntapStorage(details.get(OntapStorageConstants.USERNAME), details.get(OntapStorageConstants.PASSWORD),
                details.get(OntapStorageConstants.STORAGE_IP), details.get(OntapStorageConstants.SVM_NAME), Long.parseLong(details.get(OntapStorageConstants.SIZE)),
                ProtocolType.valueOf(protocol));
        StorageStrategy storageStrategy = StorageProviderFactory.getStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect(validateAggregatesForVolumeCreation);
        if (isValid) {
            logger.info("Connection to Ontap SVM [{}] successful", details.get(OntapStorageConstants.SVM_NAME));
            return storageStrategy;
        } else {
            logger.error("getStrategyByStoragePoolDetails: Connection to Ontap SVM [" + details.get(OntapStorageConstants.SVM_NAME) + "] failed");
            throw new CloudRuntimeException("Connection to Ontap SVM [" + details.get(OntapStorageConstants.SVM_NAME) + "] failed");
        }
    }

    public static String getIgroupName(String svmName, String hostUuid) {
        //Igroup name format: cs_hostUuid_svmName
        String sanitizedHostUuid = hostUuid.replaceAll("[^a-zA-Z0-9_-]", "_");
        String igroupName = OntapStorageConstants.CS + OntapStorageConstants.UNDERSCORE + sanitizedHostUuid + OntapStorageConstants.UNDERSCORE + svmName;
        // ONTAP igroup names are limited to 96 characters; truncate if longer.
        if (igroupName.length() > OntapStorageConstants.IGROUP_NAME_MAX_LENGTH) {
            igroupName = igroupName.substring(0, OntapStorageConstants.IGROUP_NAME_MAX_LENGTH);
        }
        return igroupName;
    }

    public static String generateExportPolicyName(String svmName, String volumeName){
        return OntapStorageConstants.CS + OntapStorageConstants.HYPHEN + svmName + OntapStorageConstants.HYPHEN + volumeName;
    }

    public static String getLunName(String volName, String lunName) {
        //LUN name in ONTAP unified format: "/vol/VolumeName/LunName"
        return OntapStorageConstants.VOLUME_PATH_PREFIX + volName + OntapStorageConstants.SLASH + lunName;
    }

    /**
     * Builds an ONTAP-safe name token from user-provided snapshot text.
     */
    public static String getOntapSnapshotName(String cloudStackSnapshotName) {
        if (cloudStackSnapshotName == null || cloudStackSnapshotName.trim().isEmpty()) {
            throw new InvalidParameterValueException("Snapshot name cannot be null or blank");
        }
        String normalized = cloudStackSnapshotName.replaceAll("[^a-zA-Z0-9_]", "_");
        if (normalized.isEmpty()) {
            normalized = "snapshot";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "s_" + normalized;
        }
        if (normalized.length() > OntapStorageConstants.MAX_SNAPSHOT_NAME_LENGTH) {
            normalized = normalized.substring(0, OntapStorageConstants.MAX_SNAPSHOT_NAME_LENGTH);
        }
        return normalized;
    }

    /**
     * Builds an ONTAP-safe snapshot name that preserves the CloudStack UI snapshot name
     * and appends a uniqueness suffix.
     */
    public static String buildOntapSnapshotName(String cloudStackSnapshotName, String uniquenessSuffix) {
        String normalizedBase = (cloudStackSnapshotName == null || cloudStackSnapshotName.trim().isEmpty())
                ? "snapshot"
                : getOntapSnapshotName(cloudStackSnapshotName);
        String suffix = (uniquenessSuffix == null || uniquenessSuffix.isEmpty())
                ? ""
                : "_" + uniquenessSuffix.replaceAll("[^a-zA-Z0-9_]", "_");
        int maxLength = OntapStorageConstants.MAX_SNAPSHOT_NAME_LENGTH;
        int maxBaseLength = maxLength - suffix.length();
        if (maxBaseLength <= 0) {
            return normalizedBase.substring(0, maxLength);
        }
        if (normalizedBase.length() > maxBaseLength) {
            normalizedBase = normalizedBase.substring(0, maxBaseLength);
        }
        return normalizedBase + suffix;
    }

    /**
     * Extracts a resource UUID from an ONTAP job description path.
     *
     * <p>Example: {@code POST /api/application/consistency-groups/{cg}/snapshots/{uuid}}
     * with {@code pathSegment} {@code "/snapshots/"} returns the snapshot UUID.</p>
     */
    public static String extractUuidFromOntapJobDescription(String description, String pathSegment) {
        if (description == null || pathSegment == null || pathSegment.isEmpty()) {
            return null;
        }
        int idx = description.indexOf(pathSegment);
        if (idx < 0) {
            return null;
        }
        String remainder = description.substring(idx + pathSegment.length()).trim();
        if (remainder.isEmpty()) {
            return null;
        }
        int queryIdx = remainder.indexOf('?');
        if (queryIdx >= 0) {
            remainder = remainder.substring(0, queryIdx);
        }
        int slashIdx = remainder.indexOf('/');
        if (slashIdx >= 0) {
            remainder = remainder.substring(0, slashIdx);
        }
        return remainder.isEmpty() ? null : remainder;
    }

    /**
     * Returns true when the exception indicates the ONTAP Object was already removed.
     * Delete workflows treat a missing backend object as idempotent success.
     */
    public static boolean isOntapObjectNotFoundError(Throwable error) {
        if (error == null) {
            return false;
        }
        if(error instanceof FeignException) {
            FeignException feignException = (FeignException) error;
            if (feignException.status() == 404) {
                return true;
            }
        }
        String message = error.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("404") || lower.contains("not found") || lower.contains("does not exist")
                    || lower.contains("entry doesn't exist")) {
                return true;
            }
        } else {
            logger.warn("Error message is null for exception: {}", error.getClass().getName());
            return false;
        }
        return false;
    }

}
