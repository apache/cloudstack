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

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.log4j.Logger;

import com.cloud.storage.StorageManager;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.google.common.base.Preconditions;

public class ScaleIOGatewayClientConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(ScaleIOGatewayClientConnectionPool.class);

    private ConcurrentHashMap<Long, ScaleIOGatewayClient> gatewayClients;

    private static final ScaleIOGatewayClientConnectionPool instance;

    static {
        instance = new ScaleIOGatewayClientConnectionPool();
    }

    public static ScaleIOGatewayClientConnectionPool getInstance() {
        return instance;
    }

    private ScaleIOGatewayClientConnectionPool() {
        gatewayClients = new ConcurrentHashMap<Long, ScaleIOGatewayClient>();
    }

    public ScaleIOGatewayClient getClient(Long storagePoolId, StoragePoolDetailsDao storagePoolDetailsDao)
            throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        Preconditions.checkArgument(storagePoolId != null && storagePoolId > 0, "Invalid storage pool id");

        ScaleIOGatewayClient client = null;
        synchronized (gatewayClients) {
            client = gatewayClients.get(storagePoolId);
            if (client == null) {
                final String url = storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_ENDPOINT).getValue();
                final String encryptedUsername = storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_USERNAME).getValue();
                final String username = DBEncryptionUtil.decrypt(encryptedUsername);
                final String encryptedPassword = storagePoolDetailsDao.findDetail(storagePoolId, ScaleIOGatewayClient.GATEWAY_API_PASSWORD).getValue();
                final String password = DBEncryptionUtil.decrypt(encryptedPassword);
                final int clientTimeout = StorageManager.STORAGE_POOL_CLIENT_TIMEOUT.valueIn(storagePoolId);
                final int clientMaxConnections = StorageManager.STORAGE_POOL_CLIENT_MAX_CONNECTIONS.valueIn(storagePoolId);

                client = new ScaleIOGatewayClientImpl(url, username, password, false, clientTimeout, clientMaxConnections);
                gatewayClients.put(storagePoolId, client);
                LOGGER.debug("Added gateway client for the storage pool: " + storagePoolId);
            }
        }

        return client;
    }

    public boolean removeClient(Long storagePoolId) {
        Preconditions.checkArgument(storagePoolId != null && storagePoolId > 0, "Invalid storage pool id");

        ScaleIOGatewayClient client = null;
        synchronized (gatewayClients) {
            client = gatewayClients.remove(storagePoolId);
        }

        if (client != null) {
            LOGGER.debug("Removed gateway client for the storage pool: " + storagePoolId);
            return true;
        }

        return false;
    }
}
