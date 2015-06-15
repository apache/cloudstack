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

import java.util.HashMap;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.uservm.UserVm;

@Entity
@Table(name = "user_vm")
@DiscriminatorValue(value = "User")
@PrimaryKeyJoinColumn(name = "id")
public class UserVmVO extends VMInstanceVO implements UserVm {

    @Column(name = "iso_id", nullable = true, length = 17)
    private Long isoId = null;

    @Column(name = "user_data", updatable = true, nullable = true, length = 32768)
    @Basic(fetch = FetchType.LAZY)
    private String userData;

    @Column(name = "display_name", updatable = true, nullable = true)
    private String displayName;

    @Column(name = "update_parameters", updatable = true)
    protected boolean updateParameters = true;

    transient String password;

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Long getIsoId() {
        return isoId;
    }

    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public UserVmVO(long id, String instanceName, String displayName, long templateId, HypervisorType hypervisorType, long guestOsId, boolean haEnabled,
                    boolean limitCpuUse, long domainId, long accountId, long userId, long serviceOfferingId, String userData, String name, Long diskOfferingId) {
        super(id, serviceOfferingId, name, instanceName, Type.User, templateId, hypervisorType, guestOsId, domainId, accountId, userId, haEnabled, limitCpuUse, diskOfferingId);
        this.userData = userData;
        this.displayName = displayName;
        this.details = new HashMap<String, String>();
    }

    protected UserVmVO() {
        super();
    }

    public void setIsoId(Long id) {
        this.isoId = id;
    }

    @Override
    public void setUserData(String userData) {
        this.userData = userData;
    }

    @Override
    public String getUserData() {
        return userData;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDetail(String name) {
        return details != null ? details.get(name) : null ;
    }

    @Override
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public void setUpdateParameters(boolean updateParameters) {
        this.updateParameters = updateParameters;
    }

    public boolean isUpdateParameters() {
        return updateParameters;
    }
}
