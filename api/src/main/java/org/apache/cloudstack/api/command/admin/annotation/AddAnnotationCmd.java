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
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.BooleanUtils;

@APICommand(name = "addAnnotation", description = "add an annotation.", responseObject = AnnotationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.11", authorized = {RoleType.Admin})
public class AddAnnotationCmd extends BaseCmd {


    @Parameter(name = ApiConstants.ANNOTATION, type = CommandType.STRING, description = "the annotation text")
    private String annotation;

    @Parameter(name = ApiConstants.ENTITY_TYPE, type = CommandType.STRING, description = "the entity type (only HOST is allowed atm)")
    private String entityType;

    @Parameter(name = ApiConstants.ENTITY_ID, type = CommandType.STRING, description = "the id of the entity to annotate")
    private String entityUuid;

    @Parameter(name = ApiConstants.ADMINS_ONLY, type = CommandType.BOOLEAN, since = "4.16.0",
            description = "the annotation is visible for admins only")
    private Boolean adminsOnly;

    public String getAnnotation() {
        return annotation;
    }

    protected void setEntityType(String newType) {
        entityType = newType;
    }
    public AnnotationService.EntityType getEntityType() {
        return AnnotationService.EntityType.valueOf(entityType);
    }

    protected void setEntityUuid(String newUuid) {
        entityUuid = newUuid;
    }
    public String getEntityUuid() {
        return entityUuid;
    }

    public boolean isAdminsOnly() {
        return BooleanUtils.toBoolean(adminsOnly);
    }

    @Override
    public void execute()
            throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        Preconditions.checkNotNull(getEntityUuid(),"I have to have an entity to set an annotation on!");
        Preconditions.checkState(AnnotationService.EntityType.contains(entityType),(java.lang.String)"'%s' is ot a valid EntityType to put annotations on", entityType);
        AnnotationResponse annotationResponse = annotationService.addAnnotation(this);
        annotationResponse.setResponseName(getCommandName());
        this.setResponseObject(annotationResponse);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getAccountId();
    }
}
