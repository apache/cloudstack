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

package com.cloud.dc;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.Encrypt;

/**
 * VmwareDatacenterVO contains information of Vmware Datacenter associated with a CloudStack zone.
 */

@Entity
@Table(name = "vmware_data_center")
public class VmwareDatacenterVO implements VmwareDatacenter {

    private static final long serialVersionUID = -9114941929893819232L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "guid")
    private String guid;

    @Column(name = "name")
    private String vmwareDatacenterName;

    @Column(name = "vcenter_host")
    private String vcenterHost;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "username")
    private String user;

    @Encrypt
    @Column(name = "password")
    private String password;

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getVmwareDatacenterName() {
        return vmwareDatacenterName;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getVcenterHost() {
        return vcenterHost;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setVmwareDatacenterName(String name) {
        vmwareDatacenterName = name;
    }

    public void setVcenterHost(String vCenterHost) {
        vcenterHost = vCenterHost;
    }

    public void setUser(String user) {
        this.user = user;
        ;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return new StringBuilder("VmwareDatacenter[").append(guid).append("]").toString();
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VmwareDatacenterVO) {
            return ((VmwareDatacenterVO)obj).getId() == getId();
        } else {
            return false;
        }
    }

    public VmwareDatacenterVO(String guid, String name, String vCenterHost, String user, String password) {
        uuid = UUID.randomUUID().toString();
        vmwareDatacenterName = name;
        this.guid = guid;
        vcenterHost = vCenterHost;
        this.user = user;
        this.password = password;
    }

    public VmwareDatacenterVO(long id, String guid, String name, String vCenterHost, String user, String password) {
        this(guid, name, vCenterHost, user, password);
        this.id = id;
    }

    public VmwareDatacenterVO() {
        uuid = UUID.randomUUID().toString();
    }

}
