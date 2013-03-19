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
package org.apache.cloudstack.entity.cloud;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.cloudstack.entity.identity.AccountResource;


/**
 * InstanceGroup entity resource
 */
@XmlRootElement(name = "instancegroup")
public class InstanceGroupResource extends CloudResource {

    // attributes
    private long id;
    private String uuid;
    private String name;

    // relationships
    private AccountResource account;
    private List<VirtualMachineResource> vms;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountResource getAccount() {
        return account;
    }

    public void setAccount(AccountResource account) {
        this.account = account;
    }

    public List<VirtualMachineResource> getVms() {
        return vms;
    }

    public void setVms(List<VirtualMachineResource> vms) {
        this.vms = vms;
    }



}
