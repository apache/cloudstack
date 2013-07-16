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
import java.util.List;

import com.cloud.utils.Pair;
import com.cloud.utils.SerialVersionUID;

/**
 * wrap exceptions that you know there's no point in dealing with.
 */
public class CloudRuntimeException extends RuntimeException implements ErrorContext {

    private static final long serialVersionUID = SerialVersionUID.CloudRuntimeException;

    protected ArrayList<Pair<Class<?>, String>> uuidList = new ArrayList<Pair<Class<?>, String>>();

    // This holds a list of uuids and their descriptive names.
    protected ArrayList<ExceptionProxyObject> idList = new ArrayList<ExceptionProxyObject>();

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

    public void addProxyObject(ExceptionProxyObject obj){
        idList.add(obj);
    }
    
    public void addProxyObject(String uuid) {
        idList.add(new ExceptionProxyObject(uuid, null));
    }

    public void addProxyObject(String voObjUuid, String description) {
        ExceptionProxyObject proxy = new ExceptionProxyObject(voObjUuid, description);
        idList.add(proxy);
    }

    public ArrayList<ExceptionProxyObject> getIdProxyList() {
        return idList;
    }

    public void setCSErrorCode(int cserrcode) {
        csErrorCode = cserrcode;
    }

    public int getCSErrorCode() {
        return csErrorCode;
    }

    public CloudRuntimeException(Throwable t) {
        super(t);
    }

    @Override
    public CloudRuntimeException add(Class<?> entity, String uuid) {
        uuidList.add(new Pair<Class<?>, String>(entity, uuid));
        return this;
    }

    @Override
    public List<Pair<Class<?>, String>> getEntitiesInError() {
        return uuidList;
    }
}
