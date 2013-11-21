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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.cloud.utils.exception.ExceptionProxyObject;

public class ExceptionResponse extends BaseResponse {

    @SerializedName("uuidList")
    @Param(description = "List of uuids associated with this error")
    private List<ExceptionProxyObject> idList;

    @SerializedName("errorcode")
    @Param(description = "numeric code associated with this error")
    private Integer errorCode;

    @SerializedName("cserrorcode")
    @Param(description = "cloudstack exception error code associated with this error")
    private Integer csErrorCode;

    @SerializedName("errortext")
    @Param(description = "the text associated with this error")
    private String errorText = "Command failed due to Internal Server Error";

    public ExceptionResponse() {
        idList = new ArrayList<ExceptionProxyObject>();
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public void addProxyObject(ExceptionProxyObject id) {
        idList.add(id);
        return;
    }

    public List<ExceptionProxyObject> getIdProxyList() {
        return idList;
    }

    public void setCSErrorCode(int cserrcode) {
        this.csErrorCode = cserrcode;
    }

    @Override
    public String toString() {
        return ("Error Code: " + errorCode + " Error text: " + errorText);
    }
}
