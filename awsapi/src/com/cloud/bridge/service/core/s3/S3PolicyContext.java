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
package com.cloud.bridge.service.core.s3;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.cloud.bridge.service.core.s3.S3PolicyAction.PolicyActions;
import com.cloud.bridge.service.core.s3.S3PolicyCondition.ConditionKeys;

/**
 * The purpose of this class is to pass into the heart of the Bucket Policy evaluation logic
 * all the context (strings from the request) needed for each defined condition.
 */
public class S3PolicyContext {

    private HttpServletRequest request = null;;
    private String bucketName = null;
    private String keyName = null;
    private PolicyActions requestedAction;
    private Map<ConditionKeys, String> evalParams = new HashMap<ConditionKeys, String>();

    public S3PolicyContext(PolicyActions requestedAction, String bucketName) {
        this.requestedAction = requestedAction;
        this.bucketName = bucketName;
    }

    public HttpServletRequest getHttp() {
        return request;
    }

    public void setHttp(HttpServletRequest request) {
        this.request = request;
    }

    public String getRemoveAddr() {
        if (null == request)
            return null;
        else
            return request.getRemoteAddr();
    }

    public boolean getIsHTTPSecure() {
        if (null == request)
            return false;
        else
            return request.isSecure();
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public PolicyActions getRequestedAction() {
        return requestedAction;
    }

    public void setRequestedAction(PolicyActions action) {
        this.requestedAction = action;
    }

    public String getEvalParam(ConditionKeys key) {
        String param = null;

        if (ConditionKeys.UserAgent == key) {
            if (null != request)
                param = request.getHeader("User-Agent");
        } else if (ConditionKeys.Referer == key) {
            if (null != request)
                param = request.getHeader("Referer");
        } else
            param = evalParams.get(key);

        return param;
    }

    public void setEvalParam(ConditionKeys key, String value) {
        evalParams.put(key, value);
    }
}
