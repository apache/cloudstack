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
package org.apache.cloudstack.storage.datastore.provider;

import javax.inject.Inject;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.StoragePool;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;

import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdaptivePrimaryHostListener implements HypervisorHostListener {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    StoragePoolHostDao storagePoolHostDao;

    @Inject
    HostDao hostDao;

    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;

    public AdaptivePrimaryHostListener(AdaptivePrimaryDatastoreAdapterFactoryMap factoryMap) {

    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        logger.debug("hostAboutToBeRemoved called");
        return true;
    }

    @Override
    public boolean hostAdded(long hostId) {
        logger.debug("hostAdded called");
        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long poolId) {
        HostVO host = hostDao.findById(hostId);
        StoragePoolVO pool = primaryDataStoreDao.findById(poolId);
        return hostConnect(host, pool);
    }

    @Override
    public boolean hostConnect(Host host, StoragePool pool) {
        logger.debug("hostConnect called for host {}, pool {}", host, pool);
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(pool.getId(), host.getId());
        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(pool.getId(), host.getId(), "");
            storagePoolHostDao.persist(storagePoolHost);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        HostVO host = hostDao.findById(hostId);
        StoragePoolVO pool = primaryDataStoreDao.findById(poolId);
        return hostDisconnected(host, pool);
    }

    @Override
    public boolean hostDisconnected(Host host, StoragePool pool){
        logger.debug("hostDisconnected called for host {}, pool {}", host, pool);
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(pool.getId(), host.getId());

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(host.getId(), pool.getId());
        }
        return true;
    }

    @Override
    public boolean hostEnabled(long hostId) {
        logger.debug("hostEnabled called");
        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        logger.debug("hostRemoved called");
        return true;
    }
}
