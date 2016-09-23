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
package com.cloud.agent.api.to;

import com.cloud.storage.DataStoreRole;

public class NfsTO implements DataStoreTO {

    private String _url;
    private DataStoreRole _role;
    private String uuid;
    private static final String pathSeparator = "/";
    private Integer nfsVersion;

    public NfsTO() {

        super();

    }

    public NfsTO(String url, DataStoreRole role) {

        super();

        this._url = url;
        this._role = role;

    }

    @Override
    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        this._url = url;
    }

    @Override
    public DataStoreRole getRole() {
        return _role;
    }

    public void setRole(DataStoreRole role) {
        this._role = role;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPathSeparator() {
        return pathSeparator;
    }

    public Integer getNfsVersion() {
        return nfsVersion;
    }

    public void setNfsVersion(Integer nfsVersion) {
        this.nfsVersion = nfsVersion;
    }
}
