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
package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.router.VirtualRouter;

/**
 */
@Entity
@Table(name = "domain_router")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(value = "DomainRouter")
public class DomainRouterVO extends VMInstanceVO implements VirtualRouter {
    @Column(name = "element_id")
    private long elementId;

    @Column(name = "public_ip_address")
    private String publicIpAddress;

    @Column(name = "public_mac_address")
    private String publicMacAddress;

    @Column(name = "public_netmask")
    private String publicNetmask;

    @Column(name = "is_redundant_router")
    boolean isRedundantRouter;

    @Column(name = "priority")
    int priority;

    @Column(name = "is_priority_bumpup")
    boolean isPriorityBumpUp;

    @Column(name = "redundant_state")
    @Enumerated(EnumType.STRING)
    private RedundantState redundantState;

    @Column(name = "stop_pending")
    boolean stopPending;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role = Role.VIRTUAL_ROUTER;

    @Column(name = "template_version")
    private String templateVersion;

    @Column(name = "scripts_version")
    private String scriptsVersion;

    @Column(name = "vpc_id")
    private Long vpcId;

    public DomainRouterVO(long id, long serviceOfferingId, long elementId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long domainId,
                          long accountId, long userId, boolean isRedundantRouter, int priority, boolean isPriorityBumpUp, RedundantState redundantState, boolean haEnabled, boolean stopPending,
                          Long vpcId) {
        super(id, serviceOfferingId, name, name, Type.DomainRouter, templateId, hypervisorType, guestOSId, domainId, accountId, userId, haEnabled);
        this.elementId = elementId;
        this.isRedundantRouter = isRedundantRouter;
        this.priority = priority;
        this.redundantState = redundantState;
        this.isPriorityBumpUp = isPriorityBumpUp;
        this.stopPending = stopPending;
        this.vpcId = vpcId;
    }

    public DomainRouterVO(long id, long serviceOfferingId, long elementId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long domainId,
                          long accountId, long userId, boolean isRedundantRouter, int priority, boolean isPriorityBumpUp, RedundantState redundantState, boolean haEnabled, boolean stopPending,
                          Type vmType, Long vpcId) {
        super(id, serviceOfferingId, name, name, vmType, templateId, hypervisorType, guestOSId, domainId, accountId, userId, haEnabled);
        this.elementId = elementId;
        this.isRedundantRouter = isRedundantRouter;
        this.priority = priority;
        this.redundantState = redundantState;
        this.isPriorityBumpUp = isPriorityBumpUp;
        this.stopPending = stopPending;
        this.vpcId = vpcId;
    }

    public long getElementId() {
        return elementId;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public String getPublicNetmask() {
        return publicNetmask;
    }

    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    protected DomainRouterVO() {
        super();
    }

    @Override
    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    @Override
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean getIsRedundantRouter() {
        return this.isRedundantRouter;
    }

    public void setIsRedundantRouter(boolean isRedundantRouter) {
        this.isRedundantRouter = isRedundantRouter;
    }

    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public RedundantState getRedundantState() {
        return this.redundantState;
    }

    public void setRedundantState(RedundantState redundantState) {
        this.redundantState = redundantState;
    }

    public boolean getIsPriorityBumpUp() {
        return this.isPriorityBumpUp;
    }

    public void setIsPriorityBumpUp(boolean isPriorityBumpUp) {
        this.isPriorityBumpUp = isPriorityBumpUp;
    }

    @Override
    public boolean isStopPending() {
        return this.stopPending;
    }

    @Override
    public void setStopPending(boolean stopPending) {
        this.stopPending = stopPending;
    }

    @Override
    public String getTemplateVersion() {
        return this.templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getScriptsVersion() {
        return this.scriptsVersion;
    }

    public void setScriptsVersion(String scriptsVersion) {
        this.scriptsVersion = scriptsVersion;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

}
