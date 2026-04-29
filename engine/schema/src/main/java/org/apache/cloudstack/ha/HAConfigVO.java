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

package org.apache.cloudstack.ha;

import com.cloud.utils.db.StateMachine;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "ha_config")
public class HAConfigVO implements HAConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "resource_id", updatable = false, nullable = false)
    private long resourceId;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private HAResource.ResourceType resourceType;

    @Column(name = "enabled")
    private boolean enabled = false;

    // There is no setter for status because it has to be set in the dao code
    @Enumerated(value = EnumType.STRING)
    @StateMachine(state = HAState.class, event = HAConfig.Event.class)
    @Column(name = "ha_state", updatable = true, nullable = false, length = 32)
    private HAState haState = null;

    @Column(name = "provider")
    private String haProvider;

    // This field should be updated every time the state is updated.
    // There's no set method in the vo object because it is done with in the dao code.
    @Column(name = "update_count", updatable = true, nullable = false)
    private long updateCount;

    @Column(name = "update_time", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updateTime;

    @Column(name = "mgmt_server_id")
    private Long managementServerId;

    public HAConfigVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    public long getResourceId() {
        return resourceId;
    }

    public HAResource.ResourceType getResourceType() {
        return resourceType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public long incrUpdateCount() {
        updateCount++;
        return updateCount;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    public HAState getHaState() {
        return haState;
    }

    @Override
    public HAState getState() {
        return haState;
    }

    public String getHaProvider() {
        return haProvider;
    }

    public void setHaProvider(String haProvider) {
        this.haProvider = haProvider;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceType(HAResource.ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }
}
