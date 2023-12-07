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
package com.cloud.storage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.NumbersUtil;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.BooleanUtils;

@Entity
@Table(name = "storage_pool_tags")
public class StoragePoolTagVO implements InternalIdentity {

    protected StoragePoolTagVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "pool_id")
    private long poolId;

    @Column(name = "tag")
    private String tag;

    @Column(name = "is_tag_a_rule")
    private boolean isTagARule;

    public StoragePoolTagVO(long poolId, String tag) {
        this.poolId = poolId;
        this.tag = tag;
        this.isTagARule = false;
    }

    public StoragePoolTagVO(long poolId, String tag, Boolean isTagARule) {
        this.poolId = poolId;
        this.tag = tag;
        this.isTagARule = BooleanUtils.toBooleanDefaultIfNull(isTagARule, false);
    }

    @Override
    public long getId() {
        return this.id;
    }

    public long getPoolId() {
        return poolId;
    }

    public String getTag() {
        return tag;
    }

    public boolean isTagARule() {
        return this.isTagARule;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StoragePoolTagVO) {
            return this.poolId == ((StoragePoolTagVO)obj).getPoolId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
}
