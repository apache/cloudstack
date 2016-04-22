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

import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;

public class ImageStoreDetailsUtil {

    @Inject
    protected ImageStoreDao imageStoreDao;
    @Inject
    protected ImageStoreDetailsDao imageStoreDetailsDao;

    /**
     * Obtain NFS protocol version (if provided) for a store id.<br/>
     * It can be set by adding an entry in {@code image_store_details} table, providing {@code name=nfs.version} and {@code value=X} (e.g. 3)
     * @param storeId image store id
     * @return {@code null} if {@code nfs.version} is not found for storeId <br/>
     * {@code X} if {@code nfs.version} is found found for storeId
     */
    public Integer getNfsVersion(long storeId) throws NumberFormatException {
        String nfsVersion = null;
        if (imageStoreDetailsDao.getDetails(storeId) != null){
            Map<String, String> storeDetails = imageStoreDetailsDao.getDetails(storeId);
            if (storeDetails != null && storeDetails.containsKey("nfs.version")){
                nfsVersion = storeDetails.get("nfs.version");
            }
        }
        return (nfsVersion != null ? Integer.valueOf(nfsVersion) : null);
    }

    /**
     * Obtain NFS protocol version (if provided) for a store uuid.<br/>
     * It can be set by adding an entry in {@code image_store_details} table, providing {@code name=nfs.version} and {@code value=X} (e.g. 3)
     * @param storeId image store id
     * @return {@code null} if {@code nfs.version} is not found for storeUuid <br/>
     * {@code X} if {@code nfs.version} is found found for storeUuid
     */
    public Integer getNfsVersionByUuid(String storeUuid){
        ImageStoreVO imageStore = imageStoreDao.findByUuid(storeUuid);
        if (imageStore != null){
            return getNfsVersion(imageStore.getId());
        }
        return null;
    }

}
