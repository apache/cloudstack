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

package org.apache.cloudstack.veeam.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiSummary {

    private SummaryCount hosts;
    private SummaryCount storageDomains;
    private SummaryCount users;
    private SummaryCount vms;

    public SummaryCount getHosts() {
        return hosts;
    }

    public void setHosts(SummaryCount hosts) {
        this.hosts = hosts;
    }

    public SummaryCount getStorageDomains() {
        return storageDomains;
    }

    public void setStorageDomains(SummaryCount storageDomains) {
        this.storageDomains = storageDomains;
    }

    public SummaryCount getUsers() {
        return users;
    }

    public void setUsers(SummaryCount users) {
        this.users = users;
    }

    public SummaryCount getVms() {
        return vms;
    }

    public void setVms(SummaryCount vms) {
        this.vms = vms;
    }
}
