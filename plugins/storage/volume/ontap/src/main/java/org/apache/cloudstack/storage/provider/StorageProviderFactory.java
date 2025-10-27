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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.service.UnifiedNASStrategy;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.SANStrategy;
import org.apache.cloudstack.storage.service.NASStrategy;
import org.apache.cloudstack.storage.utils.Constants.ProtocolType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class StorageProviderFactory {
    private static final Logger s_logger = (Logger) LogManager.getLogger(StorageProviderFactory.class);

    private StorageProviderFactory() {}

    public static StorageStrategy createStrategy(OntapStorage ontapStorage) {
        ProtocolType protocol = ontapStorage.getProtocol();
        s_logger.info("Initializing StorageProviderFactory with protocol: {}", protocol);
        switch (protocol) {
            case NFS:
                if(!ontapStorage.getIsDisaggregated()) {
                    return new UnifiedNASStrategy(ontapStorage);
                } else {
                    throw new CloudRuntimeException("Unsupported configuration: Disaggregated ONTAP is not supported.");
                }
            case ISCSI:
                if (!ontapStorage.getIsDisaggregated()) {
                    return new UnifiedSANStrategy(ontapStorage);
                } else {
                    throw new CloudRuntimeException("Unsupported configuration: Disaggregated ONTAP is not supported.");
                }
            default:
                throw new CloudRuntimeException("Unsupported configuration: " + protocol);
        }
    }

    public static SANStrategy getSANStrategy(OntapStorage ontapStorage) {
        return (SANStrategy) createStrategy(ontapStorage);
    }

    public static NASStrategy getNASStrategy(OntapStorage ontapStorage) {
        return (NASStrategy) createStrategy(ontapStorage);
    }
}