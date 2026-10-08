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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    public static StorageStrategy getStrategyByStoragePoolDetails(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            logger.error("getStrategyByStoragePoolDetails: Storage pool details are null or empty");
            throw new CloudRuntimeException("Storage pool details are null or empty");
        }
        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        OntapStorage ontapStorage = new OntapStorage(details.get(OntapStorageConstants.USERNAME), details.get(OntapStorageConstants.PASSWORD),
                details.get(OntapStorageConstants.STORAGE_IP), details.get(OntapStorageConstants.SVM_NAME), Long.parseLong(details.get(OntapStorageConstants.SIZE)),
                ProtocolType.valueOf(protocol));
        StorageStrategy storageStrategy = StorageProviderFactory.getStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect();
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
     * Converts a path stored in CloudStack (absolute LUN {@code /vol/&lt;flexVol&gt;/...} or already
     * relative NFS file path) into the FlexVol-relative path expected by
     * {@code POST /api/storage/file/clone}.
     */
    public static String toFlexVolRelativePath(String path, String flexVolName) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (flexVolName != null && !flexVolName.isEmpty()) {
            String prefix = OntapStorageConstants.VOLUME_PATH_PREFIX + flexVolName + OntapStorageConstants.SLASH;
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        // Already relative (typical NFS uuid path) or unexpected absolute form — strip a leading slash.
        return path.startsWith(OntapStorageConstants.SLASH) ? path.substring(1) : path;
    }

    /**
     * Builds the ONTAP LUN clone source name that points at a LUN inside a FlexVol snapshot.
     *
     * <p>Format required by {@code POST /api/storage/luns} when cloning from a snapshot:
     * {@code /vol/&lt;flexVol&gt;/.snapshot/&lt;snapshotName&gt;/&lt;relativeLunPath&gt;}.</p>
     *
     * <p>{@code clone.source.uuid} cannot identify a snapshot-resident LUN; name must be used.</p>
     */
    public static String toLunCloneSourcePathInSnapshot(String lunPath, String flexVolName, String snapshotName) {
        if (flexVolName == null || flexVolName.isEmpty()) {
            throw new InvalidParameterValueException("FlexVolume name is required to build a snapshot LUN path");
        }
        if (snapshotName == null || snapshotName.isEmpty()) {
            throw new InvalidParameterValueException("Snapshot name is required to build a snapshot LUN path");
        }
        String relativeLunPath = toFlexVolRelativePath(lunPath, flexVolName);
        if (relativeLunPath == null || relativeLunPath.isEmpty()) {
            throw new InvalidParameterValueException("LUN path is required to build a snapshot LUN path");
        }
        return OntapStorageConstants.VOLUME_PATH_PREFIX + flexVolName
                + OntapStorageConstants.SNAPSHOT_PATH_SEGMENT + snapshotName
                + OntapStorageConstants.SLASH + relativeLunPath;
    }
}
