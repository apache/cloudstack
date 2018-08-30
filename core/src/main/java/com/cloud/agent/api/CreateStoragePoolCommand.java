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

import java.util.Map;

import com.cloud.storage.StoragePool;

public class CreateStoragePoolCommand extends ModifyStoragePoolCommand {
    public static final String DATASTORE_NAME = "datastoreName";
    public static final String IQN = "iqn";
    public static final String STORAGE_HOST = "storageHost";
    public static final String STORAGE_PORT = "storagePort";

    private boolean _createDatastore;
    private Map<String, String> _details;

    public CreateStoragePoolCommand(boolean add, StoragePool pool) {
        super(add, pool);
    }

    public void setCreateDatastore(boolean createDatastore) {
        _createDatastore = createDatastore;
    }

    public boolean getCreateDatastore() {
        return _createDatastore;
    }

    public void setDetails(Map<String, String> details) {
        _details = details;
    }

    public Map<String, String> getDetails() {
        return _details;
    }
}
