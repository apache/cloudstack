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
package com.cloud.api.query.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

/**
 * Storage Tags DB view.
 *
 */
@Entity
@Table(name = "host_tags")
public class HostTagVO extends BaseViewVO implements InternalIdentity {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "tag")
    private String name;

    @Column(name = "host_id")
    long hostId;

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
}
