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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2SecurityGroup {

    private String id;
    private String name;
    private String description;
    private String accountName;
    private String domainId;
    private List<EC2IpPermission> permissionSet = new ArrayList<EC2IpPermission>();
    private List<EC2TagKeyValue> tagsSet = new ArrayList<EC2TagKeyValue>();

    public EC2SecurityGroup() {
        id = null;
        name = null;
        description = null;
        accountName = null;
        domainId = null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setAccount(String account) {
        this.accountName = account;
    }

    public String getAccount() {
        return this.accountName;
    }

    public void addIpPermission(EC2IpPermission param) {
        permissionSet.add(param);
    }

    public EC2IpPermission[] getIpPermissionSet() {
        return permissionSet.toArray(new EC2IpPermission[0]);
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void addResourceTag(EC2TagKeyValue param) {
        tagsSet.add(param);
    }

    public EC2TagKeyValue[] getResourceTags() {
        return tagsSet.toArray(new EC2TagKeyValue[0]);
    }

}
