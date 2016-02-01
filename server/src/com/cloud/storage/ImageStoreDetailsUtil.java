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

import com.cloud.utils.component.Manager;


public interface ImageStoreDetailsUtil extends Manager {

    /**
     * Obtain NFS protocol version (if provided) for a store id.<br/>
     * It can be set by adding an entry in {@code image_store_details} table, providing {@code name=nfs.version} and {@code value=X} (e.g. 3)
     * @param storeId image store id
     * @return {@code null} if {@code nfs.version} is not found for storeId <br/>
     * {@code X} if {@code nfs.version} is found found for storeId
     */
    public String getNfsVersion(long storeId);

    /**
     * Obtain NFS protocol version (if provided) for a store uuid.<br/>
     * It can be set by adding an entry in {@code image_store_details} table, providing {@code name=nfs.version} and {@code value=X} (e.g. 3)
     * @param storeId image store id
     * @return {@code null} if {@code nfs.version} is not found for storeUuid <br/>
     * {@code X} if {@code nfs.version} is found found for storeUuid
     */
    public String getNfsVersionByUuid(String storeUuid);
}
