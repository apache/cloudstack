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
package org.apache.cloudstack.framework.extensions.vo;

import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.GenericDao;

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
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "extension_custom_action")
public class ExtensionCustomActionVO implements ExtensionCustomAction {

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

    @Column(name = "extension_id", nullable = false)
    private Long extensionId;

    @Column(name = "resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceType resourceType;

    @Column(name = "allowed_role_types")
    private Integer allowedRoleTypes;

    @Column(name = "success_message", length = 4096)
    private String successMessage;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    @Column(name = "timeout", nullable = false)
    private int timeout;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public ExtensionCustomActionVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public ExtensionCustomActionVO(String name, String description, long extensionId, String successMessage,
           String errorMessage, int timeout, boolean enabled) {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        this.name = name;
        this.description = description;
        this.extensionId = extensionId;
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
        this.timeout = timeout;
        this.enabled = enabled;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setAllowedRoleTypes(int allowedRoleTypes) {
        this.allowedRoleTypes = allowedRoleTypes;
    }

    @Override
    public Integer getAllowedRoleTypes() {
        return allowedRoleTypes;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(Long extensionId) {
        this.extensionId = extensionId;
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCreated(Date created) {
        this.created = created;
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
        return String.format("Extension Custom Action %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "extensionId", "resourceType"));
    }

}
