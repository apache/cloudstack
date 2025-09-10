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

package org.apache.cloudstack.quota.activationrule.presetvariables;

public class PresetVariables {

    @PresetVariableDefinition(description = "Account owner of the resource.")
    private Account account;

    @PresetVariableDefinition(description = "Domain owner of the resource.")
    private Domain domain;

    @PresetVariableDefinition(description = "Project owner of the resource. This field will not exist if the resource belongs to an account.")
    private GenericPresetVariable project;

    @PresetVariableDefinition(description = "Type of the record used. Examples for this are: VirtualMachine, DomainRouter, SourceNat, KVM.")
    private String resourceType;

    @PresetVariableDefinition(description = "Data related to the resource being processed.")
    private Value value;

    @PresetVariableDefinition(description = "Zone where the resource is.")
    private GenericPresetVariable zone;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public GenericPresetVariable getProject() {
        return project;
    }

    public void setProject(GenericPresetVariable project) {
        this.project = project;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public GenericPresetVariable getZone() {
        return zone;
    }

    public void setZone(GenericPresetVariable zone) {
        this.zone = zone;
    }
}
