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
package org.apache.cloudstack.storage.image;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;

import com.cloud.capacity.CapacityManager;

public abstract class NfsImageStoreDriverImpl extends BaseImageStoreDriverImpl {

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;

    /**
     * Retrieve NFS version to be used for imgStoreId store, if provided in image_store_details table
     * @param imgStoreId store id
     * @return "secstorage.nfs.version" associated value for imgStoreId in image_store_details table if exists, null if not
     */
    protected String getNfsVersion(long imgStoreId){
        Map<String, String> imgStoreDetails = _imageStoreDetailsDao.getDetails(imgStoreId);
        String nfsVersionKey = CapacityManager.ImageStoreNFSVersion.key();
        if (imgStoreDetails != null && imgStoreDetails.containsKey(nfsVersionKey)){
            return imgStoreDetails.get(nfsVersionKey);
        }
        return null;
    }

}
