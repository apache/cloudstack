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

import org.apache.cloudstack.entity.CloudResource;
import org.apache.cloudstack.entity.identity.AccountResource;
import org.apache.cloudstack.entity.infrastructure.HostResource;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.VirtualMachine.State;

/**
 * VirtualMachine entity resource
 */
public class VirtualMachineResource extends CloudResource {
    //attributes
    protected String name;
    protected State state = null;
    protected HypervisorType hypervisorType;
    protected long guestOsId;
    protected String vncPassword;
    protected boolean haEnabled;
    protected boolean limitCpuUse;


    //relationships
    protected HostResource host;
    protected AccountResource account;
    protected TemplateResource template;
    protected ComputeOfferingResource computeOffering;
    protected DiskOfferingResource diskOffering;
    protected List<NicResource> nics;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }
    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public long getGuestOsId() {
        return guestOsId;
    }
    public void setGuestOsId(long guestOsId) {
        this.guestOsId = guestOsId;
    }
    public String getVncPassword() {
        return vncPassword;
    }
    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }
    public boolean isHaEnabled() {
        return haEnabled;
    }
    public void setHaEnabled(boolean haEnabled) {
        this.haEnabled = haEnabled;
    }
    public boolean isLimitCpuUse() {
        return limitCpuUse;
    }
    public void setLimitCpuUse(boolean limitCpuUse) {
        this.limitCpuUse = limitCpuUse;
    }
    public HostResource getHost() {
        return host;
    }
    public void setHost(HostResource host) {
        this.host = host;
    }
    public AccountResource getAccount() {
        return account;
    }
    public void setAccount(AccountResource account) {
        this.account = account;
    }
    public TemplateResource getTemplate() {
        return template;
    }
    public void setTemplate(TemplateResource template) {
        this.template = template;
    }
    public ComputeOfferingResource getComputeOffering() {
        return computeOffering;
    }
    public void setComputeOffering(ComputeOfferingResource computeOffering) {
        this.computeOffering = computeOffering;
    }
    public DiskOfferingResource getDiskOffering() {
        return diskOffering;
    }
    public void setDiskOffering(DiskOfferingResource diskOffering) {
        this.diskOffering = diskOffering;
    }
    public List<NicResource> getNics() {
        return nics;
    }
    public void setNics(List<NicResource> nics) {
        this.nics = nics;
    }



}
