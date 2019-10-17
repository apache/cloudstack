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
package com.cloud.event;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "usage_event_details")
public class UsageEventDetailsVO {

    @Id
    @Column(name = "id")
    long id;

    @Column(name = "usage_event_id", nullable = false)
    long usageEventId;

    @Column(name = "name", nullable = false)
    String key;

    @Column(name = "value")
    String value;

    public UsageEventDetailsVO() {
    }

    public UsageEventDetailsVO(long usageEventId, String key, String value) {
        this.key = key;
        this.value = value;
        this.usageEventId = usageEventId;
    }

    public long getId() {
        return id;
    }

    public void setUsageEventId(long usageEventId) {
        this.usageEventId = usageEventId;
    }

    public long getUsageEventId() {
        return usageEventId;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

}
