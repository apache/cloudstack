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
package com.cloud.agent.api.to;

import java.util.Map;

import com.cloud.storage.Volume;

public class DiskTO {
    public static final String CHAP_INITIATOR_USERNAME = "chapInitiatorUsername";
    public static final String CHAP_INITIATOR_SECRET = "chapInitiatorSecret";
    public static final String CHAP_TARGET_USERNAME = "chapTargetUsername";
    public static final String CHAP_TARGET_SECRET = "chapTargetSecret";
    public static final String MANAGED = "managed";
    public static final String IQN = "iqn";
    public static final String STORAGE_HOST = "storageHost";
    public static final String STORAGE_PORT = "storagePort";
    public static final String VOLUME_SIZE = "volumeSize";
    public static final String MOUNT_POINT = "mountpoint";
    public static final String PROTOCOL_TYPE = "protocoltype";
    public static final String PATH = "path";
    public static final String UUID = "uuid";

    private DataTO data;
    private Long diskSeq;
    private String path;
    private Volume.Type type;
    private Map<String, String> _details;

    public DiskTO() {

    }

    public DiskTO(DataTO data, Long diskSeq, String path, Volume.Type type) {
        this.data = data;
        this.diskSeq = diskSeq;
        this.path = path;
        this.type = type;
    }

    public DataTO getData() {
        return data;
    }

    public void setData(DataTO data) {
        this.data = data;
    }

    public Long getDiskSeq() {
        return diskSeq;
    }

    public void setDiskSeq(Long diskSeq) {
        this.diskSeq = diskSeq;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Volume.Type getType() {
        return type;
    }

    public void setType(Volume.Type type) {
        this.type = type;
    }

    public void setDetails(Map<String, String> details) {
        _details = details;
    }

    public Map<String, String> getDetails() {
        return _details;
    }
}
