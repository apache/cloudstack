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
package org.apache.cloudstack.api;

import java.util.ArrayList;

import com.cloud.exception.CloudException;
import com.cloud.utils.exception.CSExceptionErrorCode;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionProxyObject;

@SuppressWarnings("serial")
public class ServerApiException extends CloudRuntimeException {
    private ApiErrorCode _errorCode;
    private String _description;

    public ServerApiException() {
        _errorCode = ApiErrorCode.INTERNAL_ERROR;
        _description = null;
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(ServerApiException.class.getName()));
    }

    public ServerApiException(ApiErrorCode errorCode, String description) {
        _errorCode = errorCode;
        _description = description;
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(ServerApiException.class.getName()));
    }

    // wrap a specific CloudRuntimeException to a ServerApiException
    public ServerApiException(ApiErrorCode errorCode, String description, Throwable cause) {
        super(description, cause);
        _errorCode = errorCode;
        _description = description;
        if (cause instanceof CloudRuntimeException) {
            CloudRuntimeException rt = (CloudRuntimeException)cause;
            ArrayList<ExceptionProxyObject> idList = rt.getIdProxyList();
            if (idList != null) {
                for (int i = 0; i < idList.size(); i++) {
                    addProxyObject(idList.get(i));
                }
            }
            setCSErrorCode(rt.getCSErrorCode());
        } else if (cause instanceof CloudException) {
            CloudException rt = (CloudException)cause;
            ArrayList<String> idList = rt.getIdProxyList();
            if (idList != null) {
                for (int i = 0; i < idList.size(); i++) {
                    addProxyObject(idList.get(i));
                }
            }
            setCSErrorCode(rt.getCSErrorCode());
        }
    }

    public ApiErrorCode getErrorCode() {
        return _errorCode;
    }

    public void setErrorCode(ApiErrorCode errorCode) {
        _errorCode = errorCode;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    @Override
    public String getMessage() {
        return _description;
    }
}
