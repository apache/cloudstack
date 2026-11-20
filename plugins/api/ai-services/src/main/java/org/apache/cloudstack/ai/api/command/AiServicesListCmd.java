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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.response.ListResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A command that lists what the engine returns (SPEC.md section 9).
 *
 * The engine has already filtered the collection by caller context, so
 * the count here is the caller's count — which is what makes "two
 * tenants cannot see each other's endpoints" (section 14) a property of
 * one place rather than of every list command.
 */
public abstract class AiServicesListCmd<T extends BaseResponse> extends AiServicesSyncCmd {

    /**
     * The row CloudStack is opening a detail view for.
     *
     * AutogenView renders a detail page by calling the same list command
     * with `id=<the row's key>`. A command that does not declare the
     * parameter has it dropped in silence, so the engine answers with
     * everything and the view draws the first row — which is what every
     * detail page in this section used to do, whatever had been clicked.
     */
    @Parameter(name = ApiConstants.ID, type = CommandType.STRING,
            description = "the record to show, when opening one from a list")
    private String id;

    /** The JSON array field holding the items, for example "models". */
    protected abstract String itemsKey();

    /**
     * Whether this command's screen opens a detail view at all. A list with
     * no detail page is never called with an `id`, and filtering one that is
     * would silently empty it.
     */
    protected boolean filtersById() {
        return false;
    }

    /**
     * The engine field an `id` is matched against. A row's key is
     * `record.id || record.name`, so this is "id" wherever the response
     * carries one, and the field the screen builds its name from otherwise.
     */
    protected String identityField() {
        return "id";
    }

    /** Whether this engine object is the one that was asked for. */
    private boolean wanted(JsonObject item) {
        if (!filtersById() || id == null || id.isEmpty()) {
            return true;
        }
        JsonElement value = item.get(identityField());
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return false;
        }
        return id.equals(value.getAsString());
    }

    /** Map one engine object onto a CloudStack response. */
    protected abstract T mapItem(JsonObject item);

    @Override
    protected void onSuccess(JsonObject body) {
        List<T> items = new ArrayList<>();
        if (body.has(itemsKey()) && body.get(itemsKey()).isJsonArray()) {
            JsonArray entries = body.getAsJsonArray(itemsKey());
            for (JsonElement element : entries) {
                if (element.isJsonObject() && wanted(element.getAsJsonObject())) {
                    items.add(mapItem(element.getAsJsonObject()));
                }
            }
        }

        ListResponse<T> response = new ListResponse<>();
        response.setResponses(items, items.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
