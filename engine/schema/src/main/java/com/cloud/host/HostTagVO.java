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
package com.cloud.host;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.BooleanUtils;

@Entity
@Table(name = "host_tags")
public class HostTagVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "tag")
    private String tag;

    @Column(name = "is_implicit")
    private boolean isImplicit = false;

    @Column(name = "is_tag_a_rule")
    private boolean isTagARule;

    protected HostTagVO() {
    }

    public HostTagVO(long hostId, String tag) {
        this.hostId = hostId;
        this.tag = tag;
        this.isTagARule = false;
    }

    public HostTagVO(long hostId, String tag, Boolean isTagARule) {
        this.hostId = hostId;
        this.tag = tag;
        this.isTagARule = BooleanUtils.toBooleanDefaultIfNull(isTagARule, false);
    }

    public long getHostId() {
        return hostId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean getIsTagARule() {
        return isTagARule;
    }

    public void setIsImplicit(boolean isImplicit) {
        this.isImplicit = isImplicit;
    }

    public boolean getIsImplicit() {
        return isImplicit;
    }

    @Override
    public long getId() {
        return id;
    }
}
