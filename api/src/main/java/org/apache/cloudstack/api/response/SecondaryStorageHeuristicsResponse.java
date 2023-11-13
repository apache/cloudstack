//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;

import java.util.Date;

import static org.apache.cloudstack.api.ApiConstants.HEURISTIC_TYPE_VALID_OPTIONS;

@EntityReference(value = {Heuristic.class})
public class SecondaryStorageHeuristicsResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the heuristic.")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the heuristic.")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the heuristic.")
    private String description;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The zone which the heuristic is valid upon.")
    private String zoneId;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The resource type directed to a specific secondary storage by the selector. " + HEURISTIC_TYPE_VALID_OPTIONS)
    private String type;

    @SerializedName(ApiConstants.HEURISTIC_RULE)
    @Param(description = "The heuristic rule, in JavaScript language, used to select a secondary storage to be directed.")
    private String heuristicRule;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "When the heuristic was created.")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "When the heuristic was removed.")
    private Date removed;


    public SecondaryStorageHeuristicsResponse(String id, String name, String description, String zoneId, String type, String heuristicRule, Date created, Date removed) {
        super("heuristics");
        this.id = id;
        this.name = name;
        this.description = description;
        this.zoneId = zoneId;
        this.type = type;
        this.heuristicRule = heuristicRule;
        this.created = created;
        this.removed = removed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHeuristicRule() {
        return heuristicRule;
    }

    public void setHeuristicRule(String heuristicRule) {
        this.heuristicRule = heuristicRule;
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
}
