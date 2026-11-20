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
package org.apache.cloudstack.ai.api.response;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/** One deployment, as the engine reports it (SPEC.md section 9). */
public class AiDeploymentResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "the deployment's identifier")
    private String id;

    @SerializedName("model")
    @Param(description = "the catalog name of the deployed model")
    private String model;

    @SerializedName("colour")
    @Param(description = "blue or green, during a rollout")
    private String colour;

    @SerializedName("replicasdesired")
    @Param(description = "replicas the deployment should run")
    private Integer replicasDesired;

    @SerializedName("replicasready")
    @Param(description = "replicas currently passing their health checks")
    private Integer replicasReady;

    @SerializedName("state")
    @Param(description = "the deployment's state as the engine reports it")
    private String state;

    /** Builds a response from an engine job result or deployment object. */
    public static AiDeploymentResponse from(JsonObject source) {
        AiDeploymentResponse response = new AiDeploymentResponse();
        response.setObjectName("aideployment");
        if (source == null) {
            return response;
        }
        JsonObject body = source.has("deployment") && source.get("deployment").isJsonObject()
                ? source.getAsJsonObject("deployment")
                : source;

        response.id = string(body, "id");
        response.model = string(body, "model");
        response.colour = string(body, "colour");
        response.state = string(body, "state");
        response.replicasDesired = integer(body, "replicas_desired");
        response.replicasReady = integer(body, "replicas_ready");
        return response;
    }

    private static String string(JsonObject body, String field) {
        return body.has(field) && !body.get(field).isJsonNull()
                ? body.get(field).getAsString()
                : null;
    }

    private static Integer integer(JsonObject body, String field) {
        return body.has(field) && !body.get(field).isJsonNull()
                ? body.get(field).getAsInt()
                : null;
    }

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public String getColour() {
        return colour;
    }

    public Integer getReplicasDesired() {
        return replicasDesired;
    }

    public Integer getReplicasReady() {
        return replicasReady;
    }

    public String getState() {
        return state;
    }
}
