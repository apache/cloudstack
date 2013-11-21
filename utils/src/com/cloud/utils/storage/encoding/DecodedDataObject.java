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
package com.cloud.utils.storage.encoding;

public class DecodedDataObject {
    private String objType;
    private Long size;
    private String name;
    private String path;
    private DecodedDataStore store;

    public DecodedDataObject(String objType, Long size, String name, String path, DecodedDataStore store) {
        this.objType = objType;
        this.size = size;
        this.path = path;
        this.store = store;
    }

    public String getObjType() {
        return this.objType;
    }

    public Long getSize() {
        return this.size;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public DecodedDataStore getStore() {
        return this.store;
    }
}
