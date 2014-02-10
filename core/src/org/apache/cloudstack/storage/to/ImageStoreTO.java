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

import org.apache.cloudstack.storage.image.datastore.ImageStoreInfo;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.DataStoreRole;

public class ImageStoreTO implements DataStoreTO {
    private String type;
    private String uri;
    private String providerName;
    private DataStoreRole role;
    private String uuid;

    public ImageStoreTO() {

    }

    public ImageStoreTO(ImageStoreInfo dataStore) {
        this.type = dataStore.getType();
        this.uri = dataStore.getUri();
        this.providerName = null;
        this.role = dataStore.getRole();
    }

    public String getProtocol() {
        return this.type;
    }

    public String getUri() {
        return this.uri;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setRole(DataStoreRole role) {
        this.role = role;
    }

    @Override
    public DataStoreRole getRole() {
        return this.role;
    }

    @Override
    public String toString() {
        return new StringBuilder("ImageStoreTO[type=").append(type)
            .append("|provider=")
            .append(providerName)
            .append("|role=")
            .append(role)
            .append("|uri=")
            .append(uri)
            .append("]")
            .toString();
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getUrl() {
        return getUri();
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
