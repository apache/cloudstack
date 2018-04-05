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
import com.cloud.utils.SwiftUtil;

public class SwiftTO implements DataStoreTO, SwiftUtil.SwiftClientCfg {
    Long id;
    String url;
    String account;

    String userName;
    String key;
    private static final String pathSeparator = "/";

    public SwiftTO() {
    }

    public SwiftTO(Long id, String url, String account, String userName, String key) {
        this.id = id;
        this.url = url;
        this.account = account;
        this.userName = userName;
        this.key = key;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Image;
    }

    @Override
    public String getEndPoint() {
        return this.url;
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public String getPathSeparator() {
        return pathSeparator;
    }
}
