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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "updateAnnotationVisibility", description = "update an annotation visibility.",
        responseObject = AnnotationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.16", authorized = {RoleType.Admin})
public class UpdateAnnotationVisibilityCmd extends BaseCmd {


    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true,
            description = "the id of the annotation")
    private String uuid;

    @Parameter(name = ApiConstants.ADMINS_ONLY, type = CommandType.BOOLEAN, required = true,
            description = "the annotation is visible for admins only")
    private Boolean adminsOnly;

    public String getUuid() {
        return uuid;
    }

    public Boolean getAdminsOnly() {
        return adminsOnly;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        AnnotationResponse annotationResponse = annotationService.updateAnnotationVisibility(this);
        annotationResponse.setResponseName(getCommandName());
        this.setResponseObject(annotationResponse);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getAccountId();
    }
}
