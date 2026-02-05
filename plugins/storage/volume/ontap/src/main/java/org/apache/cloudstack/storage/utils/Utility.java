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

import com.cloud.storage.ScopeType;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.provider.StorageProviderFactory;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Base64Utils;

import java.util.Map;

public class Utility {

    private static final Logger s_logger = LogManager.getLogger(Utility.class);

    private static final String BASIC = "Basic";
    private static final String AUTH_HEADER_COLON = ":";

    public static String generateAuthHeader (String username, String password) {
        byte[] encodedBytes = Base64Utils.encode((username + AUTH_HEADER_COLON + password).getBytes());
        return BASIC + StringUtils.SPACE + new String(encodedBytes);
    }

    public static StorageStrategy getStrategyByStoragePoolDetails(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            s_logger.error("getStrategyByStoragePoolDetails: Storage pool details are null or empty");
            throw new CloudRuntimeException("getStrategyByStoragePoolDetails: Storage pool details are null or empty");
        }
        String protocol = details.get(Constants.PROTOCOL);
        OntapStorage ontapStorage = new OntapStorage(details.get(Constants.USERNAME), details.get(Constants.PASSWORD),
                details.get(Constants.MANAGEMENT_LIF), details.get(Constants.SVM_NAME), Long.parseLong(details.get(Constants.SIZE)),
                ProtocolType.valueOf(protocol),
                Boolean.parseBoolean(details.get(Constants.IS_DISAGGREGATED)));
        StorageStrategy storageStrategy = StorageProviderFactory.getStrategy(ontapStorage);
        boolean isValid = storageStrategy.connect();
        if (isValid) {
            s_logger.info("Connection to Ontap SVM [{}] successful", details.get(Constants.SVM_NAME));
            return storageStrategy;
        } else {
            s_logger.error("getStrategyByStoragePoolDetails: Connection to Ontap SVM [" + details.get(Constants.SVM_NAME) + "] failed");
            throw new CloudRuntimeException("getStrategyByStoragePoolDetails: Connection to Ontap SVM [" + details.get(Constants.SVM_NAME) + "] failed");
        }
    }

    public static String getIgroupName(String svmName, ScopeType scopeType, Long scopeId) {
        return Constants.CS + Constants.UNDERSCORE + svmName + Constants.UNDERSCORE + scopeType.toString().toLowerCase() + Constants.UNDERSCORE + scopeId;
    }

    public static String generateExportPolicyName(String svmName, String volumeName){
        return Constants.EXPORT + Constants.HYPHEN + svmName + Constants.HYPHEN + volumeName;
    }
}
