// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.exception;

import java.util.ArrayList;

import com.cloud.utils.AnnotationHelper;
import com.cloud.utils.SerialVersionUID;

/**
 * wrap exceptions that you know there's no point in dealing with.
 */
public class CloudRuntimeException extends RuntimeException {

    private static final long serialVersionUID = SerialVersionUID.CloudRuntimeException;

    // This holds a list of uuids and their names. Add uuid:fieldname pairs
    protected ArrayList<String> idList = new ArrayList<String>();

    protected int csErrorCode;


    public CloudRuntimeException(String message) {
        super(message);
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(this.getClass().getName()));
    }

    public CloudRuntimeException(String message, Throwable th) {
        super(message, th);
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(this.getClass().getName()));
    }

    public CloudRuntimeException() {
        super();
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(this.getClass().getName()));
    }

    public void addProxyObject(String uuid) {
        idList.add(uuid);
        return;
    }

    public void addProxyObject(Object voObj, Long id, String idFieldName) {
        // Get the VO object's table name.
        String tablename = AnnotationHelper.getTableName(voObj);
        if (tablename != null) {
            addProxyObject(tablename, id, idFieldName);
        }
        return;
    }

    public ArrayList<String> getIdProxyList() {
        return idList;
    }

    public void setCSErrorCode(int cserrcode) {
        this.csErrorCode = cserrcode;
    }

    public int getCSErrorCode() {
        return this.csErrorCode;
    }
}
