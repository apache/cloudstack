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
package org.apache.cloudstack.framework.config.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.config.ConfigurationSubGroup;

@Entity
@Table(name = "configuration_subgroup")
public class ConfigurationSubGroupVO implements ConfigurationSubGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id = 1;

    @Column(name = "name")
    private String name;

    @Column(name = "keywords")
    private String keywords = null;

    @Column(name = "precedence")
    private Long precedence = 999L;

    @Column(name = "group_id")
    private Long groupId;

    protected ConfigurationSubGroupVO() {
    }

    public ConfigurationSubGroupVO(String name, String keywords) {
        this.name = name;
        this.keywords = keywords;
        this.precedence = 999L;
    }

    public ConfigurationSubGroupVO(String name, String keywords, Long precedence) {
        this.name = name;
        this.keywords = keywords;
        this.precedence = precedence;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    @Override
    public Long getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Long precedence) {
        this.precedence = precedence;
    }

    @Override
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
