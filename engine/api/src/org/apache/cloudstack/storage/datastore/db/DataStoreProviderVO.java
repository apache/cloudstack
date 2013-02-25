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
package org.apache.cloudstack.storage.datastore.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "data_store_provider")
public class DataStoreProviderVO {
    @Id
    @TableGenerator(name = "data_store_provider_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "data_store_provider_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "uuid", nullable = false)
    private String uuid;
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getUuid() {
        return this.uuid;
    }
}
