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
package org.apache.cloudstack.api;

import com.cloud.exception.ResourceAllocationException;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    private Long id;

    private String uuid;

    public abstract void create() throws ResourceAllocationException;

    public Long getEntityId() {
        return id;
    }

    public void setEntityId(Long id) {
        this.id = id;
    }

    public String getEntityUuid() {
        return uuid;
    }

    public void setEntityUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCreateEventType() {
        return null;
    }

    public String getCreateEventDescription() {
        return null;
    }

}
