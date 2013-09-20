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
package org.apache.cloudstack.acl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("acl_group_role_map"))
public class AclGroupRoleMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_id")
    private long aclGroupId;

    @Column(name = "role_id")
    private long aclRoleId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public AclGroupRoleMapVO() {
    }

    public AclGroupRoleMapVO(long aclGroupId, long aclRoleId) {
        this.aclGroupId = aclGroupId;
        this.aclRoleId = aclRoleId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getAclGroupId() {
        return aclGroupId;
    }


    public long getAclRoleId() {
        return aclRoleId;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
