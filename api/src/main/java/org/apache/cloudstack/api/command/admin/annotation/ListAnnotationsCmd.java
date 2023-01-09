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
package org.apache.cloudstack.api.command.admin.annotation;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "listAnnotations", description = "Lists annotations.", responseObject = AnnotationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.11", authorized = {RoleType.Admin})
public class ListAnnotationsCmd extends BaseListCmd {


    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, description = "the id of the annotation")
    private String uuid;

    @Parameter(name = ApiConstants.ENTITY_TYPE, type = CommandType.STRING, description = "the entity type")
    private String entityType;

    @Parameter(name = ApiConstants.ENTITY_ID, type = CommandType.STRING, description = "the id of the entity for which to show annotations")
    private String entityUuid;

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.STRING, since = "4.16.0",
            description = "optional: the id of the user of the annotation", required = false)
    private String userUuid;

    @Parameter(name = ApiConstants.ANNOTATION_FILTER,
            type = CommandType.STRING, since = "4.16.0",
            description = "possible values are \"self\" and \"all\". "
                    + "* self : annotations that have been created by the calling user. "
                    + "* all : all the annotations the calling user can access")
    private String annotationFilter;

    public String getUuid() {
        return uuid;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public String getAnnotationFilter() {
        return annotationFilter;
    }

    @Override public void execute()
            throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        // preconditions to check:
        // if entity type is null entity uuid can not have a value
        Preconditions.checkArgument(StringUtils.isNotBlank(entityType) ? StringUtils.isBlank(uuid) : true,
                "I can search for an annotation on an entity or for a specific annotation, not both");
        // if uuid has a value entity type and entity uuid can not have a value
        Preconditions.checkArgument(StringUtils.isNotBlank(uuid) ? entityType == null && entityUuid == null : true,
                "I will either search for a specific annotation or for annotations on an entity, not both");

        ListResponse<AnnotationResponse> response = annotationService.searchForAnnotations(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
        response.setObjectName("annotations");
    }}
