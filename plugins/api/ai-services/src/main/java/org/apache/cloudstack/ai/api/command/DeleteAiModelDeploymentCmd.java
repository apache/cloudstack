// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.api.command;

import org.apache.cloudstack.ai.AiServicesEvents;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SuccessResponse;

/** deleteAiModelDeployment (SPEC.md section 9). */
@APICommand(name = DeleteAiModelDeploymentCmd.APINAME,
        description = "Deletes an AI model deployment.",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        // Hand-written, so this list is not generated: the drift
        // test in tests/unit/test_commands.py holds it to
        // aisvc/api/commands.py like every other command's.
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.User})
public class DeleteAiModelDeploymentCmd extends AiServicesAsyncCmd {

    public static final String APINAME = "deleteAiModelDeployment";

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true,
            description = "the deployment to delete")
    private String deploymentId;

    @Override
    protected String enginePath() {
        return "/v1/deployments/" + deploymentId;
    }

    @Override
    protected String engineMethod() {
        return "DELETE";
    }

    @Override
    protected void onSuccess(EngineJob job) {
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(true);
        setResponseObject(response);
    }

    @Override
    public String getEventType() {
        return AiServicesEvents.MODEL_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "deleting AI model deployment " + deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }
}
