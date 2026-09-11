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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.ResourceDetail;

/**
 * Generic key/value config for a readiness rule (port/protocol, script content, threshold, ...).
 * The {@code script} key is encrypted at rest by {@code InstanceBootGroupReadinessRuleDetailsDaoImpl}
 * for CustomScript rules, the same manual encrypt-by-known-key-name pattern used for S3/Swift
 * secrets in {@code ImageStoreDetailVO} — there is no boolean "encrypted" flag column convention in
 * this codebase.
 */
@Entity
@Table(name = "instance_boot_group_readiness_rule_details")
public class InstanceBootGroupReadinessRuleDetailsVO implements ResourceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "rule_id")
    private long resourceId;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @Column(name = "display")
    private boolean display = true;

    protected InstanceBootGroupReadinessRuleDetailsVO() {
    }

    public InstanceBootGroupReadinessRuleDetailsVO(long ruleId, String name, String value) {
        this.resourceId = ruleId;
        this.name = name;
        this.value = value;
    }

    public InstanceBootGroupReadinessRuleDetailsVO(long ruleId, String name, String value, boolean display) {
        this(ruleId, name, value);
        this.display = display;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }
}
