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
package com.cloud.vm;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.serializer.GsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class VmWork implements Serializable {
    protected transient Logger logger = LogManager.getLogger(getClass());
    private static final long serialVersionUID = -6946320465729853589L;
    private static final Gson gsonLogger = GsonHelper.getGsonLogger();

    long userId;
    long accountId;
    long vmId;

    String handlerName;

    public VmWork(long userId, long accountId, long vmId, String handlerName) {
        this.userId = userId;
        this.accountId = accountId;
        this.vmId = vmId;
        this.handlerName = handlerName;
    }

    public VmWork(VmWork vmWork) {
        this.userId = vmWork.getUserId();
        this.accountId = vmWork.getAccountId();
        this.vmId = vmWork.getVmId();
        this.handlerName = vmWork.getHandlerName();
    }

    public long getUserId() {
        return userId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getVmId() {
        return vmId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    @Override
    public String toString() {
        return gsonLogger.toJson(this);
    }

    protected String toStringAfterRemoveParams(String paramsObjName, List<String> params) {
        String ObjJsonStr = gsonLogger.toJson(this);
        if (StringUtils.isBlank(ObjJsonStr) || StringUtils.isBlank(paramsObjName) || CollectionUtils.isEmpty(params)) {
            return ObjJsonStr;
        }

        try {
            Map<String, Object> ObjMap = new ObjectMapper().readValue(ObjJsonStr, HashMap.class);
            if (ObjMap != null && ObjMap.containsKey(paramsObjName)) {
                for (String param : params) {
                    ((Map<String, String>)ObjMap.get(paramsObjName)).remove(param);
                }
                String resultJson = new ObjectMapper().writeValueAsString(ObjMap);
                return resultJson;
            }
        } catch (final JsonProcessingException e) {
            // Ignore json exception
        }

        return ObjJsonStr;
    }
}
