//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.framework.extensions.vo;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "extension")
public class ExtensionVO implements Extension {

    public ExtensionVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public ExtensionVO(String name, String description, Type type, String relativePath, State state) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.type = type;
        this.relativePath = relativePath;
        this.userDefined = true;
        this.pathReady = true;
        this.state = state;
        this.created = new Date();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @Column(name = "relative_path", nullable = false, length = 2048)
    private String relativePath;

    @Column(name = "path_ready")
    private boolean pathReady;

    @Column(name = "is_user_defined")
    private boolean userDefined;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    @Override
    public boolean isPathReady() {
        return pathReady;
    }

    public void setPathReady(boolean pathReady) {
        this.pathReady = pathReady;
    }

    @Override
    public boolean isUserDefined() {
        return userDefined;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String toString() {
        return String.format("Extension %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "type"));
    }
}
