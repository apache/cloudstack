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
package org.apache.cloudstack.storage.datastore.util;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;

/**
 * Management-server only component. Per-pool Linstor settings that the agent needs (the insecure-TLS
 * flag) are delivered to the agent inside the storage pool details of a ModifyStoragePoolCommand and
 * then cached in the agent's LinstorStoragePool. A dynamic {@code updateConfiguration} only updates the
 * database and the management server's own config cache; it does not refresh the agent. This listener
 * reacts to such changes and re-pushes the pool details to every connected host so the cached pool is
 * rebuilt with the new value, without requiring a host reconnect.
 */
public class LinstorConfigChangeListener extends ManagerBase {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private MessageBus messageBus;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private StoragePoolHostDao storagePoolHostDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private StorageManager storageManager;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        messageBus.subscribe(EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, new ConfigValueChangeSubscriber());
        return true;
    }

    private final class ConfigValueChangeSubscriber implements MessageSubscriber {
        @Override
        @SuppressWarnings("unchecked")
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            if (!(args instanceof Ternary)) {
                return;
            }
            final Ternary<String, ConfigKey.Scope, Long> updated = (Ternary<String, ConfigKey.Scope, Long>) args;
            // Only the per-pool insecure-TLS flag has to reach the agent; the API token is read from
            // the agent's local auth.json, so it never needs a re-push.
            if (ConfigKey.Scope.StoragePool != updated.second()
                    || !LinstorConfigurationManager.InsecureSsl.key().equals(updated.first())) {
                return;
            }

            final Long poolId = updated.third();
            final StoragePoolVO pool = primaryDataStoreDao.findById(poolId);
            if (pool == null || pool.getPoolType() != Storage.StoragePoolType.Linstor) {
                return;
            }

            logger.debug("Linstor: {} changed for storage pool {}, re-pushing pool details to connected hosts",
                    updated.first(), poolId);
            for (Long hostId : storagePoolHostDao.findHostsConnectedToPools(Collections.singletonList(poolId))) {
                final Host host = hostDao.findById(hostId);
                if (host == null) {
                    continue;
                }
                try {
                    storageManager.connectHostToSharedPool(host, poolId);
                    logger.debug("Linstor: re-pushed pool {} details to host {}", poolId, hostId);
                } catch (Exception e) {
                    logger.warn("Linstor: failed to re-push pool {} details to host {}", poolId, hostId, e);
                }
            }
        }
    }
}
