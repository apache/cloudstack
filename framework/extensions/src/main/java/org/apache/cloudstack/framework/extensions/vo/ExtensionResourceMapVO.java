//
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
//

package org.apache.cloudstack.framework.extensions.vo;

import org.apache.cloudstack.extension.ExtensionResourceMap;

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

@Entity
@Table(name = "extension_resource_map")
public class ExtensionResourceMapVO implements ExtensionResourceMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "extension_id", nullable = false)
    private Long extensionId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ResourceType resourceType;

    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    public ExtensionResourceMapVO() {
    }

    public ExtensionResourceMapVO(long extensionId, long resourceId, ResourceType resourceType) {
        this.extensionId = extensionId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    @Override
    public long getExtensionId() {
        return extensionId;
    }


    @Override
    public long getResourceId() {
        return resourceId;
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setExtensionId(Long extensionId) {
        this.extensionId = extensionId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return null;
    }
}
