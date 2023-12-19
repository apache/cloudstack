
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
package org.apache.cloudstack.storage.datastore.adapter;

/**
 * Represents a translation object for transmitting meta-data about a volume,
 * snapshot or template between cloudstack and the storage provider
 */
public class ProviderAdapterDataObject {
    public enum Type {
        VOLUME(),
        SNAPSHOT(),
        TEMPLATE(),
        ARCHIVE()
    }
    /**
     * The cloudstack UUID of the object
     */
    private String uuid;
    /**
     * The cloudstack name of the object (generated or user provided)
     */
    private String name;
    /**
     * The type of the object
     */
    private Type type;
    /**
     * The internal local ID of the object (not globally unique)
     */
    private Long id;
    /**
     * The external name assigned on the storage array. it may be dynamiically
     * generated or derived from cloudstack data
     */
    private String externalName;

    /**
     * The external UUID of the object on the storage array. This may be different
     * or the same as the cloudstack UUID depending on implementation.
     */
    private String externalUuid;

    /**
     * The internal (non-global) ID of the datastore this object is defined in
     */
    private Long dataStoreId;

    /**
     * The global ID of the datastore this object is defined in
     */
    private String dataStoreUuid;

    /**
     * The name of the data store this object is defined in
     */
    private String dataStoreName;

    /**
     * Represents the device connection id, typically a LUN, used to find the volume in conjunction with Address and AddressType.
     */
    private String externalConnectionId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public String getExternalUuid() {
        return externalUuid;
    }

    public void setExternalUuid(String externalUuid) {
        this.externalUuid = externalUuid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDataStoreId() {
        return dataStoreId;
    }

    public void setDataStoreId(Long dataStoreId) {
        this.dataStoreId = dataStoreId;
    }

    public String getDataStoreUuid() {
        return dataStoreUuid;
    }

    public void setDataStoreUuid(String dataStoreUuid) {
        this.dataStoreUuid = dataStoreUuid;
    }

    public String getDataStoreName() {
        return dataStoreName;
    }

    public void setDataStoreName(String dataStoreName) {
        this.dataStoreName = dataStoreName;
    }

    public String getExternalConnectionId() {
        return externalConnectionId;
    }

    public void setExternalConnectionId(String externalConnectionId) {
        this.externalConnectionId = externalConnectionId;
    }

}
