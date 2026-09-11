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
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "instance_boot_group_readiness_rule")
public class InstanceBootGroupReadinessRuleVO implements InstanceBootGroupReadinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "boot_group_id")
    private long bootGroupId;

    @Column(name = "item_type")
    @Enumerated(EnumType.STRING)
    private InstanceBootGroupMember.MemberType itemType;

    @Column(name = "item_id")
    private long itemId;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    protected InstanceBootGroupReadinessRuleVO() {
    }

    public InstanceBootGroupReadinessRuleVO(String name, long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId, RuleType ruleType, boolean enabled) {
        this.name = name;
        this.bootGroupId = bootGroupId;
        this.itemType = itemType;
        this.itemId = itemId;
        this.ruleType = ruleType;
        this.enabled = enabled;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getBootGroupId() {
        return bootGroupId;
    }

    @Override
    public InstanceBootGroupMember.MemberType getItemType() {
        return itemType;
    }

    @Override
    public long getItemId() {
        return itemId;
    }

    @Override
    public RuleType getRuleType() {
        return ruleType;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return String.format("ReadinessRule %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "itemType", "itemId", "ruleType", "enabled"));
    }
}
