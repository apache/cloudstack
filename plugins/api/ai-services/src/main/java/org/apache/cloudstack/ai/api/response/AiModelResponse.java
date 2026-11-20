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

import com.google.gson.JsonObject;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

/** One catalog entry, as listAiModels returns it (SPEC.md section 9). */
public class AiModelResponse extends BaseResponse {

    @SerializedName("name")
    @Param(description = "the catalog name tenants address the model by")
    private String name;

    @SerializedName("task")
    @Param(description = "chat, embedding, rerank or vision-chat")
    private String task;

    @SerializedName("engine")
    @Param(description = "the serving engine: vllm, sglang or trtllm")
    private String engine;

    @SerializedName("license")
    @Param(description = "the model's licence, which must be on the allow-list")
    private String license;

    @SerializedName("pricetier")
    @Param(description = "the price tier this model is billed under")
    private String priceTier;

    @SerializedName("maxmodellen")
    @Param(description = "the maximum context length in tokens")
    private Integer maxModelLen;

    @SerializedName("mirrored")
    @Param(description = "whether the weights are mirrored and the model can be deployed")
    private Boolean mirrored;

    /** Map one engine model object onto this response. */
    public static AiModelResponse from(JsonObject body) {
        AiModelResponse mapped = new AiModelResponse();
        mapped.setObjectName("aimodel");
        if (body == null) {
            return mapped;
        }
        JsonObject source = body.has("model") && body.get("model").isJsonObject()
                ? body.getAsJsonObject("model")
                : body;
        mapped.name = text(source, "name");
        mapped.task = text(source, "task");
        mapped.engine = text(source, "engine");
        mapped.license = text(source, "license");
        mapped.priceTier = text(source, "price_tier");
        if (source.has("max_model_len") && !source.get("max_model_len").isJsonNull()) {
            mapped.maxModelLen = source.get("max_model_len").getAsInt();
        }
        mapped.mirrored = source.has("mirrored") && source.get("mirrored").getAsBoolean();
        return mapped;
    }

    private static String text(JsonObject body, String field) {
        return body.has(field) && !body.get(field).isJsonNull()
                ? body.get(field).getAsString()
                : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setPriceTier(String priceTier) {
        this.priceTier = priceTier;
    }

    public void setMaxModelLen(Integer maxModelLen) {
        this.maxModelLen = maxModelLen;
    }

    public void setMirrored(Boolean mirrored) {
        this.mirrored = mirrored;
    }

    public String getName() {
        return name;
    }

    public Boolean getMirrored() {
        return mirrored;
    }
}
