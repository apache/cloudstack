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

import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;

import com.cloud.exception.StorageConflictException;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdaptivePrimaryHostListener implements HypervisorHostListener {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    StoragePoolHostDao storagePoolHostDao;

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
    public boolean hostConnect(long hostId, long poolId) throws StorageConflictException {
        logger.debug("hostConnect called for hostid [" + hostId + "], poolId [" + poolId + "]");
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);
        if (storagePoolHost == null) {
            storagePoolHost = new StoragePoolHostVO(poolId, hostId, "");
            storagePoolHostDao.persist(storagePoolHost);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean hostDisconnected(long hostId, long poolId) {
        logger.debug("hostDisconnected called for hostid [" + hostId + "], poolId [" + poolId + "]");
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
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
