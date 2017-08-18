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

package org.apache.cloudstack.outofbandmanagement;

import com.cloud.utils.db.Encrypt;
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
@Table(name = "oobm")
public class OutOfBandManagementVO implements OutOfBandManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "enabled")
    private boolean enabled = false;

    // There is no setter for status because it has to be set in the dao code
    @Enumerated(value = EnumType.STRING)
    @StateMachine(state = PowerState.class, event = PowerState.Event.class)
    @Column(name = "power_state", updatable = true, nullable = false, length = 32)
    private PowerState powerState = null;

    @Column(name = "driver")
    private String driver;

    @Column(name = "address")
    private String address;

    @Column(name = "port")
    private String port;

    @Column(name = "username")
    private String username;

    @Encrypt
    @Column(name = "password")
    private String password;

    // This field should be updated every time the state is updated.
    // There's no set method in the vo object because it is done with in the dao code.
    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updateCount;

    @Column(name = "update_time", updatable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    protected Date updateTime;

    @Column(name = "mgmt_server_id")
    private Long managementServerId;

    public OutOfBandManagementVO(Long hostId) {
        this.hostId = hostId;
        this.powerState = PowerState.Disabled;
    }

    public OutOfBandManagementVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public PowerState getState() {
        return powerState;
    }

    public Long getHostId() {
        return hostId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PowerState getPowerState() {
        return powerState;
    }

    @Override
    public String getDriver() {
        return driver;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public long incrUpdateCount() {
        updateCount++;
        return updateCount;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    public Long getManagementServerId() {
        return managementServerId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setDriver(String driver) {
        this.driver = driver;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }
}
