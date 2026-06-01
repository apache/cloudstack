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
package org.apache.cloudstack.agent.api;

import com.cloud.agent.api.Command;

public class NetrisCommand extends Command {
    private final long zoneId;
    private final Long accountId;
    private final Long domainId;
    private String name;
    private final Long id;
    private final boolean isVpc;

    public NetrisCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.name = name;
        this.id = id;
        this.isVpc = isVpc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public boolean isVpc() {
        return isVpc;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
