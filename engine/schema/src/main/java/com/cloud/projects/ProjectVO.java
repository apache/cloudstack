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
package com.cloud.projects;

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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "projects")
public class ProjectVO implements Project, Identity, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "display_text")
    String displayText;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "project_account_id")
    long projectAccountId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "uuid")
    private String uuid;

    protected ProjectVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public ProjectVO(String name, String displayText, long domainId, long projectAccountId) {
        this.name = name;
        this.displayText = displayText;
        this.projectAccountId = projectAccountId;
        this.domainId = domainId;
        this.state = State.Disabled;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return String.format("Project %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "name", "uuid", "domainId"));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProjectVO)) {
            return false;
        }
        ProjectVO that = (ProjectVO)obj;
        if (this.id != that.id) {
            return false;
        }

        return true;
    }

    @Override
    public long getProjectAccountId() {
        return projectAccountId;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
}
