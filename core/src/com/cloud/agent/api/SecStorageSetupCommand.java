//
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
//

package com.cloud.agent.api;

import org.apache.cloudstack.framework.security.keystore.KeystoreManager;

import com.cloud.agent.api.storage.StorageNfsVersionCommand;
import com.cloud.agent.api.to.DataStoreTO;

public class SecStorageSetupCommand extends StorageNfsVersionCommand {
    private DataStoreTO store;
    private String secUrl;
    private KeystoreManager.Certificates certs;
    private String postUploadKey;


    public SecStorageSetupCommand() {
        super();
    }

    public SecStorageSetupCommand(DataStoreTO store, String secUrl, KeystoreManager.Certificates certs) {
        super();
        this.secUrl = secUrl;
        this.certs = certs;
        this.store = store;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getSecUrl() {
        return secUrl;
    }

    public KeystoreManager.Certificates getCerts() {
        return certs;
    }

    public void setSecUrl(String secUrl) {
        this.secUrl = secUrl;

    }

    public DataStoreTO getDataStore() {
        return store;
    }

    public void setDataStore(DataStoreTO store) {
        this.store = store;
    }

    public String getPostUploadKey() {
        return postUploadKey;
    }

    public void setPostUploadKey(String postUploadKey) {
        this.postUploadKey = postUploadKey;
    }

}
