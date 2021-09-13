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
package org.apache.cloudstack.annotation;

import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

/**
 * @since 4.11
 */
@Entity
@Table(name = "annotations")
public class AnnotationVO implements Annotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "annotation")
    private String annotation;

    @Column(name = "entity_uuid")
    private String entityUuid;

    @Column(name = "entity_type")
    private AnnotationService.EntityType entityType;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "admins_only")
    private boolean adminsOnly;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    // construct
    public AnnotationVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public AnnotationVO(String text, AnnotationService.EntityType type, String uuid, boolean adminsOnly) {
        this();
        setAnnotation(text);
        setEntityType(type);
        setEntityUuid(uuid);
        setAdminsOnly(adminsOnly);
    }

    // access

    @Override
    public long getId() {
        return id;
    }


    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getAnnotation() {
        return annotation;
    }

    @Override
    public String getEntityUuid() {
        return entityUuid;
    }

    @Override
    public AnnotationService.EntityType getEntityType() {
        return entityType;
    }

    @Override
    public String getUserUuid() {
        return userUuid;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public void setEntityType(String entityType) {
        this.entityType = AnnotationService.EntityType.valueOf(entityType);
    }

    public void setEntityType(AnnotationService.EntityType entityType) {
        this.entityType = entityType;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public boolean isAdminsOnly() {
        return adminsOnly;
    }

    public void setAdminsOnly(boolean adminsOnly) {
        this.adminsOnly = adminsOnly;
    }
}
