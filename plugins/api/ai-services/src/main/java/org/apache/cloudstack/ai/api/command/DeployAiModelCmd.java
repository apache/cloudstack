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
import org.apache.cloudstack.ai.api.response.AiDeploymentResponse;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.google.gson.JsonObject;

/** deployAiModel (SPEC.md section 9). Async: the engine provisions and serves. */
@APICommand(name = DeployAiModelCmd.APINAME,
        description = "Deploys an AI model, returning a job to poll.",
        responseObject = AiDeploymentResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        // Hand-written, so this list is not generated: the drift
        // test in tests/unit/test_commands.py holds it to
        // aisvc/api/commands.py like every other command's.
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.User})
public class DeployAiModelCmd extends AiServicesAsyncCmd {

    public static final String APINAME = "deployAiModel";

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "the catalog name of the model to deploy")
    private String modelName;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID,
            entityType = ZoneResponse.class, required = true,
            description = "the zone to deploy into")
    private Long zoneId;

    @Parameter(name = "replicas", type = CommandType.INTEGER,
            description = "the number of replicas to run; the model's minimum if unset")
    private Integer replicas;

    @Override
    protected String enginePath() {
        return "/v1/deployments";
    }

    @Override
    protected String engineBody() {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        // A CommandType.UUID parameter is handed back as CloudStack's
        // internal numeric id, so sending it raw gives the engine a
        // number that matches nothing — the same defect the generator
        // avoids with `resolve=` (CLAUDE.md). The engine knows zones by
        // the name `platform.yaml` records in `cloudstack_zone`.
        // Guarded like every generated resolve: the parameter is
        // required at the API layer, so a null here means a command
        // built outside dispatch, and the engine reads an absent zone
        // as "the model's own".
        if (zoneId != null) {
            DataCenter resolvedZone = _entityMgr.findById(DataCenter.class, zoneId);
            if (resolvedZone == null) {
                throw new InvalidParameterValueException(
                        "Unable to find a zone with the given id");
            }
            body.addProperty("zone_id", resolvedZone.getName());
        }
        if (replicas != null) {
            body.addProperty("replicas", replicas);
        }
        return body.toString();
    }

    @Override
    protected void onSuccess(EngineJob job) {
        AiDeploymentResponse response = AiDeploymentResponse.from(job.getResult());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getEventType() {
        return AiServicesEvents.MODEL_DEPLOY;
    }

    @Override
    public String getEventDescription() {
        return "deploying AI model " + modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public Long getZoneId() {
        return zoneId;
    }
}
