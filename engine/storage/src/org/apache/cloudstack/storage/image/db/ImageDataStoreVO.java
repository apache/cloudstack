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
package org.apache.cloudstack.storage.image.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;

@Entity
@Table(name = "image_data_store")
public class ImageDataStoreVO {
    @Id
    @TableGenerator(name = "image_data_store_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "image_data_store_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "uuid", nullable = false)
    private String uuid;
    
    @Column(name = "protocol", nullable = false)
    private String protocol;

    @Column(name = "image_provider_id", nullable = false)
    private long provider;
    
    @Column(name = "data_center_id")
    private long dcId;
    
    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;
    
    
    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public long getProvider() {
        return this.provider;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProvider(long provider) {
        this.provider = provider;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getProtocol() {
        return this.protocol;
    }
    
    public void setDcId(long dcId) {
        this.dcId = dcId;
    }
    
    public long getDcId() {
        return this.dcId;
    }
    
    public ScopeType getScope() {
        return this.scope;
    }
    
    public void setScope(ScopeType scope) {
        this.scope = scope;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getUuid() {
        return this.uuid;
    }
}
