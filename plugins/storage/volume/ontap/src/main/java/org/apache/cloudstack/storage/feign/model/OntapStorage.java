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

package org.apache.cloudstack.storage.feign.model;

import org.apache.cloudstack.storage.service.model.ProtocolType;

public class OntapStorage {
    private final String username;
    private final String password;
    private final String storageIP;
    private final String svmName;
    private final Long size;
    private final ProtocolType protocolType;

    public OntapStorage(String username, String password, String storageIP, String svmName, Long size, ProtocolType protocolType) {
        this.username = username;
        this.password = password;
        this.storageIP = storageIP;
        this.svmName = svmName;
        this.size = size;
        this.protocolType = protocolType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getStorageIP() { return storageIP; }

    public String getSvmName() { return svmName; }

    public Long getSize() {
        return size;
    }

    public ProtocolType getProtocol() {
        return protocolType;
    }
}
