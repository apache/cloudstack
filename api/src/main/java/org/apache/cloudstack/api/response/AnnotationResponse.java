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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.annotation.Annotation;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

/**
 * @since 4.11
 */
@EntityReference(value = Annotation.class)
public class AnnotationResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the (uu)id of the annotation")
    private String uuid;

    @SerializedName(ApiConstants.ENTITY_TYPE)
    @Param(description = "the type of the annotated entity")
    private String entityType;

    @SerializedName(ApiConstants.ENTITY_ID)
    @Param(description = "the (uu)id of the entity to which this annotation pertains")
    private String entityUuid;

    @SerializedName(ApiConstants.ENTITY_NAME)
    @Param(description = "the name of the entity to which this annotation pertains")
    private String entityName;

    @SerializedName(ApiConstants.ANNOTATION)
    @Param(description = "the contents of the annotation")
    private String annotation;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "The (uu)id of the user that entered the annotation")
    private String userUuid;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "The username of the user that entered the annotation")
    private String username;

    @SerializedName(ApiConstants.ADMINS_ONLY)
    @Param(description = "True if the annotation is available for admins only")
    private Boolean adminsOnly;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the creation timestamp for this annotation")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the removal timestamp for this annotation")
    private Date removed;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityType(AnnotationService.EntityType entityType) {
        this.entityType = entityType.toString();
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getAdminsOnly() {
        return adminsOnly;
    }

    public void setAdminsOnly(Boolean adminsOnly) {
        this.adminsOnly = adminsOnly;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}
