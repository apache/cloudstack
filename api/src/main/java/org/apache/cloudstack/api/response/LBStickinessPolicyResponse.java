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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.network.rules.StickinessPolicy;
import com.cloud.serializer.Param;
import com.cloud.utils.Pair;

public class LBStickinessPolicyResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the LB Stickiness policy ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the name of the Stickiness policy")
    private String name;

    @SerializedName("methodname")
    @Param(description = "the method name of the Stickiness policy")
    private String methodName;

    @SerializedName("description")
    @Param(description = "the description of the Stickiness policy")
    private String description;;

    @SerializedName("state")
    @Param(description = "the state of the policy")
    private String state;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is policy for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    // FIXME : if prams with the same name exists more then once then value are concatinated with ":" as delimiter .
    // Reason: Map does not support duplicate keys, need to look for the alernate data structure
    // Example: <params>{indirect=null, name=testcookie, nocache=null, domain=www.yahoo.com:www.google.com, postonly=null}</params>
    // in the above there are two domains with values www.yahoo.com and www.google.com
    @SerializedName("params")
    @Param(description = "the params of the policy")
    private Map<String, String> params;

    public Map<String, String> getParams() {
        return params;
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

    public String getMethodName() {
        return methodName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LBStickinessPolicyResponse(StickinessPolicy stickinesspolicy) {
        this.name = stickinesspolicy.getName();
        List<Pair<String, String>> paramsList = stickinesspolicy.getParams();
        this.methodName = stickinesspolicy.getMethodName();
        this.description = stickinesspolicy.getDescription();
        this.forDisplay = stickinesspolicy.isDisplay();
        if (stickinesspolicy.isRevoke()) {
            this.setState("Revoked");
        }
        if (stickinesspolicy.getUuid() != null)
            setId(stickinesspolicy.getUuid());

        /* Get the param and values from the database and fill the response object
         *  The following loop is to
         *    1) convert from List of Pair<String,String> to Map<String, String>
         *    2)  combine all params with name with ":" , currently we have one param called "domain" that can appear multiple times.
         * */

        Map<String, String> tempParamList = new HashMap<String, String>();
        for (Pair<String, String> paramKV : paramsList) {
            String key = paramKV.first();
            String value = paramKV.second();
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            if (tempParamList.get(key) != null) {
                sb.append(":").append(tempParamList.get(key));
            }

            tempParamList.put(key, sb.toString());
        }

        this.params = tempParamList;
        setObjectName("stickinesspolicy");
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
