/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.api.response;

import com.cloud.utils.IdentityProxy;
import com.cloud.network.rules.StickinessPolicy;
import com.cloud.serializer.Param;
import com.cloud.utils.Pair;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LBStickinessPolicyResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the LB Stickiness policy ID")
    private IdentityProxy id = new IdentityProxy("load_balancer_stickiness_policies");
    
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

    // FIXME : if prams with the same name exists more then once then value are concatinated with ":" as delimitor .
    // Reason: Map does not support duplicate keys, need to look for the alernate data structure
    // Example: <params>{indirect=null, name=testcookie, nocache=null, domain=www.yahoo.com:www.google.com, postonly=null}</params>
    // in the above there are two domains with values www.yahoo.com and www.google.com
    @SerializedName("params")
    @Param(description = "the params of the policy")
    private Map<String, String> params;
    
    public Map<String, String> getParams() {
        return params;
    }

    public void setId(Long id) {
        this.id.setValue(id);
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
        if (stickinesspolicy.isRevoke()) {
            this.setState("Revoked");
        }
        if (stickinesspolicy.getId() != 0)
            setId(stickinesspolicy.getId());

        /* Get the param and values from the database and fill the response object 
         *  The following loop is to 
         *    1) convert from List of Pair<String,String> to Map<String, String> 
         *    2)  combine all params with name with ":" , currently we have one param called "domain" that can appear multiple times.
         * */

        Map<String, String> tempParamList =  new HashMap<String, String>();
        for(Pair<String,String> paramKV :paramsList){
            String key = paramKV.first();
            String value = paramKV.second();
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            if (tempParamList.get(key) != null)
            {
                sb.append(":").append(tempParamList.get(key));
            }
                
            tempParamList.put(key,sb.toString());
        }

        this.params = tempParamList;
        setObjectName("stickinesspolicy");
    }
}
