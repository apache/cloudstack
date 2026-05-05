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

package org.apache.cloudstack.storage.provider;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

import java.nio.charset.StandardCharsets;

import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.UnifiedNASStrategy;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class StorageProviderFactory {
    private static final Logger logger = LogManager.getLogger(StorageProviderFactory.class);

    public static StorageStrategy getStrategy(OntapStorage ontapStorage) {
        ProtocolType protocol = ontapStorage.getProtocol();
        logger.info("Initializing StorageProviderFactory with protocol: " + protocol);
        String decodedPassword = new String(java.util.Base64.getDecoder().decode(ontapStorage.getPassword()), StandardCharsets.UTF_8);
        ontapStorage = new OntapStorage(
            ontapStorage.getUsername(),
            decodedPassword,
            ontapStorage.getStorageIP(),
            ontapStorage.getSvmName(),
            ontapStorage.getSize(),
            protocol);
        switch (protocol) {
            case NFS3:
                UnifiedNASStrategy unifiedNASStrategy = new UnifiedNASStrategy(ontapStorage);
                ComponentContext.inject(unifiedNASStrategy);
                unifiedNASStrategy.setOntapStorage(ontapStorage);
                return unifiedNASStrategy;
            case ISCSI:
                UnifiedSANStrategy unifiedSANStrategy = new UnifiedSANStrategy(ontapStorage);
                ComponentContext.inject(unifiedSANStrategy);
                unifiedSANStrategy.setOntapStorage(ontapStorage);
                return unifiedSANStrategy;
            default:
                throw new CloudRuntimeException("Unsupported protocol: " + protocol);
        }
    }
}
