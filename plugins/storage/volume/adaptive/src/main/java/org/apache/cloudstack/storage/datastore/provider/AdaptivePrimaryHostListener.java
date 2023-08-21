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
import org.apache.log4j.Logger;

import com.cloud.exception.StorageConflictException;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;

public class AdaptivePrimaryHostListener implements HypervisorHostListener {
    static final Logger s_logger = Logger.getLogger(AdaptivePrimaryHostListener.class);

    @Inject
    StoragePoolHostDao storagePoolHostDao;

    public AdaptivePrimaryHostListener(AdaptivePrimaryDatastoreAdapterFactoryMap factoryMap) {

    }

    @Override
    public boolean hostAboutToBeRemoved(long hostId) {
        s_logger.debug("hostAboutToBeRemoved called");
        return true;
    }

    @Override
    public boolean hostAdded(long hostId) {
        s_logger.debug("hostAdded called");
        return true;
    }

    @Override
    public boolean hostConnect(long hostId, long poolId) throws StorageConflictException {
        s_logger.debug("hostConnect called for hostid [" + hostId + "], poolId [" + poolId + "]");
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
        s_logger.debug("hostDisconnected called for hostid [" + hostId + "], poolId [" + poolId + "]");
        StoragePoolHostVO storagePoolHost = storagePoolHostDao.findByPoolHost(poolId, hostId);

        if (storagePoolHost != null) {
            storagePoolHostDao.deleteStoragePoolHostDetails(hostId, poolId);
        }
        return true;
    }

    @Override
    public boolean hostEnabled(long hostId) {
        s_logger.debug("hostEnabled called");
        return true;
    }

    @Override
    public boolean hostRemoved(long hostId, long clusterId) {
        s_logger.debug("hostRemoved called");
        return true;
    }
}
