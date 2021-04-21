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
package com.cloud.storage;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;

import com.cloud.capacity.CapacityManager;
import com.google.common.base.Preconditions;

public class ImageStoreDetailsUtil {

    @Inject
    protected ImageStoreDao imageStoreDao;
    @Inject
    protected ImageStoreDetailsDao imageStoreDetailsDao;
    @Inject
    protected ConfigurationDao configurationDao;

    /**
     * Retrieve global secondary storage NFS version default value
     * @return global default value
     */
    protected String getGlobalDefaultNfsVersion(){
        ConfigurationVO globalNfsVersion = configurationDao.findByName(CapacityManager.ImageStoreNFSVersion.key());
        Preconditions.checkState(globalNfsVersion != null, "Unable to find global NFS version for version key " + CapacityManager.ImageStoreNFSVersion.key());
        return globalNfsVersion.getValue();
    }
    /**
     * Obtain NFS protocol version (if provided) for a store id, if not use default config value<br/>
     * @param storeId image store id
     * @return {@code null} if {@code secstorage.nfs.version} is not found for storeId <br/>
     * {@code X} if {@code secstorage.nfs.version} is found found for storeId
     */
    public String getNfsVersion(long storeId) throws NumberFormatException {

        final Map<String, String> storeDetails = imageStoreDetailsDao.getDetails(storeId);
        if (storeDetails != null && storeDetails.containsKey(CapacityManager.ImageStoreNFSVersion.key())) {
            return storeDetails.get(CapacityManager.ImageStoreNFSVersion.key());
        }

        return getGlobalDefaultNfsVersion();

    }

    /**
     * Obtain NFS protocol version (if provided) for a store uuid.<br/>
     * @param storeUuid image store id
     * @return {@code null} if {@code secstorage.nfs.version} is not found for storeUuid <br/>
     * {@code X} if {@code secstorage.nfs.version} is found found for storeUuid
     */
    public String getNfsVersionByUuid(String storeUuid){
        ImageStoreVO imageStore = imageStoreDao.findByUuid(storeUuid);
        if (imageStore != null){
            return getNfsVersion(imageStore.getId());
        }
        return getGlobalDefaultNfsVersion();
    }

}
