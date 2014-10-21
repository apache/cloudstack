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
package org.apache.cloudstack.region;

import com.cloud.domain.DomainVO;

public class RegionDomain extends DomainVO {
    String accountUuid;
    String parentUuid;
    String parentdomainname;
    Boolean haschild;

    public RegionDomain() {
    }

    public String getAccountuuid() {
        return accountUuid;
    }

    public void setAccountuuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getParentdomainname() {
        return parentdomainname;
    }

    public void setParentdomainname(String parentdomainname) {
        this.parentdomainname = parentdomainname;
    }

    public Boolean getHaschild() {
        return haschild;
    }

    public void setHaschild(Boolean haschild) {
        this.haschild = haschild;
    }
}
