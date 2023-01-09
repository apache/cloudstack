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
package com.cloud.storage;

import java.io.Serializable;

public class MigrationOptions implements Serializable {

    private String srcPoolUuid;
    private Storage.StoragePoolType srcPoolType;
    private Type type;
    private ScopeType scopeType;
    private String srcBackingFilePath;
    private boolean copySrcTemplate;
    private String srcVolumeUuid;
    private int timeout;

    public enum Type {
        LinkedClone, FullClone
    }

    public MigrationOptions() {
    }

    public MigrationOptions(String srcPoolUuid, Storage.StoragePoolType srcPoolType, String srcBackingFilePath, boolean copySrcTemplate, ScopeType scopeType) {
        this.srcPoolUuid = srcPoolUuid;
        this.srcPoolType = srcPoolType;
        this.type = Type.LinkedClone;
        this.scopeType = scopeType;
        this.srcBackingFilePath = srcBackingFilePath;
        this.copySrcTemplate = copySrcTemplate;
    }

    public MigrationOptions(String srcPoolUuid, Storage.StoragePoolType srcPoolType, String srcVolumeUuid, ScopeType scopeType) {
        this.srcPoolUuid = srcPoolUuid;
        this.srcPoolType = srcPoolType;
        this.type = Type.FullClone;
        this.scopeType = scopeType;
        this.srcVolumeUuid = srcVolumeUuid;
    }

    public String getSrcPoolUuid() {
        return srcPoolUuid;
    }

    public Storage.StoragePoolType getSrcPoolType() {
        return srcPoolType;
    }

    public ScopeType getScopeType() { return scopeType; }

    public String getSrcBackingFilePath() {
        return srcBackingFilePath;
    }

    public boolean isCopySrcTemplate() {
        return copySrcTemplate;
    }

    public String getSrcVolumeUuid() {
        return srcVolumeUuid;
    }

    public Type getType() {
        return type;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
