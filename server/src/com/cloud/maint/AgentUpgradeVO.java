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
package com.cloud.maint;

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="op_host_upgrade")
public class AgentUpgradeVO implements InternalIdentity {
    @Id
    @Column(name="host_id")
    private long id;
    
    @Column(name="version")
    private String version;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private UpgradeManager.State state;
    
    protected AgentUpgradeVO() {
    }
    
    public AgentUpgradeVO(long id, String version, UpgradeManager.State state) {
        this.id = id;
        this.version = version;
        this.state = state;
    }
    
    public long getId() {
        return id;
    }
    
    public String getVersion() {
        return version;
    }
    
    public UpgradeManager.State getState() {
        return state;
    }
}
