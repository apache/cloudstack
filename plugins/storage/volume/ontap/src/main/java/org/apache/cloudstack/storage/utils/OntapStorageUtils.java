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
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunSpace;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
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

    public static CloudStackVolume createCloudStackVolumeRequestByProtocol(StoragePoolVO storagePool, Map<String, String> details, DataObject volumeObject) {
        CloudStackVolume cloudStackVolumeRequest = null;

        String protocol = details.get(OntapStorageConstants.PROTOCOL);
        ProtocolType protocolType = ProtocolType.valueOf(protocol);
        switch (protocolType) {
            case NFS3:
                cloudStackVolumeRequest = new CloudStackVolume();
                cloudStackVolumeRequest.setDatastoreId(String.valueOf(storagePool.getId()));
                cloudStackVolumeRequest.setVolumeInfo(volumeObject);
                break;
            case ISCSI:
                Svm svm = new Svm();
                svm.setName(details.get(OntapStorageConstants.SVM_NAME));
                cloudStackVolumeRequest = new CloudStackVolume();
                Lun lunRequest = new Lun();
                lunRequest.setSvm(svm);

                LunSpace lunSpace = new LunSpace();
                lunSpace.setSize(volumeObject.getSize());
                lunRequest.setSpace(lunSpace);
                //Lun name is full path like in unified "/vol/VolumeName/LunName"
                String lunName = volumeObject.getName().replace(OntapStorageConstants.HYPHEN, OntapStorageConstants.UNDERSCORE);
                if(!isValidName(lunName)) {
                    String errMsg = "createAsync: Invalid dataObject name [" + lunName + "]. It must start with a letter and can only contain letters, digits, and underscores, and be up to 200 characters long.";
                    throw new InvalidParameterValueException(errMsg);
                }
                String lunFullName = getLunName(storagePool.getName(), lunName);
                lunRequest.setName(lunFullName);

                String osType = getOSTypeFromHypervisor(storagePool.getHypervisor().name());
                lunRequest.setOsType(Lun.OsTypeEnum.valueOf(osType));

                cloudStackVolumeRequest.setLun(lunRequest);
                break;
            default:
                throw new CloudRuntimeException("Unsupported protocol " + protocol);

        }
        return cloudStackVolumeRequest;
    }

    public static boolean isValidName(String name) {
        // Check for null and length constraint first
        if (name == null || name.length() > 200) {
            return false;
        }
        // Regex: Starts with a letter, followed by letters, digits, or underscores
        return name.matches(OntapStorageConstants.ONTAP_NAME_REGEX);
    }

    public static String getOSTypeFromHypervisor(String hypervisorType){
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

    public static String getIgroupName(String svmName, String hostName) {
        //Igroup name format: cs_svmName_hostName
        String sanitizedHostName = hostName.split("\\.")[0].replaceAll("[^a-zA-Z0-9_-]", "_");
        return OntapStorageConstants.CS + OntapStorageConstants.UNDERSCORE + svmName + OntapStorageConstants.UNDERSCORE + sanitizedHostName;
    }

    public static String generateExportPolicyName(String svmName, String volumeName){
        return OntapStorageConstants.CS + OntapStorageConstants.HYPHEN + svmName + OntapStorageConstants.HYPHEN + volumeName;
    }

    public static String getLunName(String volName, String lunName) {
        //LUN name in ONTAP unified format: "/vol/VolumeName/LunName"
        return OntapStorageConstants.VOLUME_PATH_PREFIX + volName + OntapStorageConstants.SLASH + lunName;
    }

}
