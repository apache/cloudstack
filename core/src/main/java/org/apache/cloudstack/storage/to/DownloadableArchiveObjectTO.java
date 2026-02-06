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

package org.apache.cloudstack.storage.to;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;

public class DownloadableArchiveObjectTO extends DownloadableObjectTO implements DataTO {
    private final DataStoreTO dataStore;
    private final String name;
    private final String checksum;
    private String path;
    private long accountId = 0L;
    private Storage.ImageFormat format = Storage.ImageFormat.ZIP;

    public DownloadableArchiveObjectTO(DataStoreTO store, String name, String checksum, String path) {
        this.dataStore = store;
        this.name = name;
        this.checksum = checksum;
        this.path = path;
    }

    @Override
    public DataStoreTO getDataStore() {
        return this.dataStore;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return null;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.ARCHIVE;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getChecksum() {
        return checksum;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return new StringBuilder("DownloadableArchiveObjectTO[datastore=").append(dataStore).append("|path").append(path).append("]").toString();
    }
}
