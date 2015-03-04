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
package org.apache.cloudstack.iam.server;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = ("iam_group_policy_map"))
public class IAMGroupPolicyMapVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_id")
    private long aclGroupId;

    @Column(name = "policy_id")
    private long aclPolicyId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public IAMGroupPolicyMapVO() {
    }

    public IAMGroupPolicyMapVO(long aclGroupId, long aclPolicyId) {
        this.aclGroupId = aclGroupId;
        this.aclPolicyId = aclPolicyId;
    }

    public long getId() {
        return id;
    }

    public long getAclGroupId() {
        return aclGroupId;
    }


    public long getAclPolicyId() {
        return aclPolicyId;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getCreated() {
        return created;
    }
}
