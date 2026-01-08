// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.storage.datastore.client;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.cloud.storage.StoragePool;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.storage.StorageManager;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.google.common.base.Preconditions;

public class ScaleIOGatewayClientConnectionPool {
    protected Logger logger = LogManager.getLogger(getClass());

    private Map<Long, ScaleIOGatewayClient> gatewayClients;
    private static final ScaleIOGatewayClientConnectionPool instance;
    private final Object lock = new Object();

    static {
        instance = new ScaleIOGatewayClientConnectionPool();
    }

    public static ScaleIOGatewayClientConnectionPool getInstance() {
        return instance;
    }

    private ScaleIOGatewayClientConnectionPool() {
        gatewayClients = new ConcurrentHashMap<>();
    }

    public ScaleIOGatewayClient getClient(StoragePool storagePool,
                                          StoragePoolDetailsDao storagePoolDetailsDao) {
        return getClient(storagePool.getId(), storagePool.getUuid(), storagePoolDetailsDao);
    }


    public ScaleIOGatewayClient getClient(DataStore dataStore,
                                          StoragePoolDetailsDao storagePoolDetailsDao) {
        return getClient(dataStore.getId(), dataStore.getUuid(), storagePoolDetailsDao);
    }

    private ScaleIOGatewayClient getClient(Long storagePoolId, String storagePoolUuid,
                                           StoragePoolDetailsDao storagePoolDetailsDao) {

        Preconditions.checkArgument(storagePoolId != null && storagePoolId > 0,
                "Invalid storage pool id");

        logger.debug("Getting ScaleIO client for {} ({})", storagePoolId, storagePoolUuid);

        ScaleIOGatewayClient client = gatewayClients.get(storagePoolId);
        if (client == null) {
            logger.debug("Before acquiring lock to create ScaleIO client for {} ({})", storagePoolId, storagePoolUuid);
            synchronized (lock) {
                logger.debug("Acquired lock to create ScaleIO client for {} ({})", storagePoolId, storagePoolUuid);
                client = gatewayClients.get(storagePoolId);
                if (client == null) {
                    logger.debug("Initializing ScaleIO client for {} ({})", storagePoolId, storagePoolUuid);

                    String url = Optional.ofNullable(storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_ENDPOINT))
                            .map(StoragePoolDetailVO::getValue)
                            .orElse(null);

                    String username = Optional.ofNullable(storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_USERNAME))
                            .map(StoragePoolDetailVO::getValue)
                            .map(DBEncryptionUtil::decrypt)
                            .orElse(null);

                    String password = Optional.ofNullable(storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_PASSWORD))
                            .map(StoragePoolDetailVO::getValue)
                            .map(DBEncryptionUtil::decrypt)
                            .orElse(null);

                    int clientTimeout = StorageManager.STORAGE_POOL_CLIENT_TIMEOUT.valueIn(storagePoolId);
                    int clientMaxConnections = StorageManager.STORAGE_POOL_CLIENT_MAX_CONNECTIONS.valueIn(storagePoolId);

                    try {
                        client = new ScaleIOGatewayClientImpl(url, username, password, false, clientTimeout, clientMaxConnections);
                        logger.debug("Created ScaleIO client for the storage pool [id: {}, uuid: {}]", storagePoolId, storagePoolUuid);
                        gatewayClients.put(storagePoolId, client);
                    } catch (Exception e) {
                        String msg = String.format("Failed to create ScaleIO client for the storage pool [id: %d, uuid: %s]", storagePoolId, storagePoolUuid);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
            }
        }

        logger.debug("Returning ScaleIO client for {} ({})", storagePoolId, storagePoolUuid);
        return client;
    }

    public boolean removeClient(DataStore dataStore) {
        Preconditions.checkArgument(dataStore != null && dataStore.getId() > 0,
                "Invalid storage pool id");

        ScaleIOGatewayClient client;
        synchronized (lock) {
            client = gatewayClients.remove(dataStore.getId());
        }

        if (client != null) {
            logger.debug("Removed gateway client for the storage pool: {}", dataStore);
            return true;
        }

        return false;
    }
}
