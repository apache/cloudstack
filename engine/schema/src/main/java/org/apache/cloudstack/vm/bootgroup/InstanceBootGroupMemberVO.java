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

package org.apache.cloudstack.vm.bootgroup;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

@Entity
@Table(name = "instance_boot_group_member")
public class InstanceBootGroupMemberVO implements InstanceBootGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "boot_group_id")
    private long bootGroupId;

    @Column(name = "member_type")
    @Enumerated(EnumType.STRING)
    private MemberType memberType;

    @Column(name = "member_id")
    private long memberId;

    @Column(name = "boot_order")
    private int bootOrder;

    @Column(name = "created")
    private Date created;

    protected InstanceBootGroupMemberVO() {
    }

    public InstanceBootGroupMemberVO(long bootGroupId, MemberType memberType, long memberId, int bootOrder) {
        this.bootGroupId = bootGroupId;
        this.memberType = memberType;
        this.memberId = memberId;
        this.bootOrder = bootOrder;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getBootGroupId() {
        return bootGroupId;
    }

    @Override
    public MemberType getMemberType() {
        return memberType;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    @Override
    public int getBootOrder() {
        return bootOrder;
    }

    public void setBootOrder(int bootOrder) {
        this.bootOrder = bootOrder;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return String.format("BootGroupMember %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "bootGroupId", "memberType", "memberId", "bootOrder"));
    }
}
