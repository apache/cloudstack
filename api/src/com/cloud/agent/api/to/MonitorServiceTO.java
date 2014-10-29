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
package com.cloud.agent.api.to;

import org.apache.cloudstack.api.InternalIdentity;

public class MonitorServiceTO implements InternalIdentity {
    long id;
    String service;
    String processname;
    String serviceName;
    String servicePath;
    String pidFile;
    boolean isDefault;

    protected MonitorServiceTO() {
    }

    public MonitorServiceTO(String service, String processname, String serviceName, String servicepath, String pidFile, boolean isDefault) {
        this.service = service;
        this.processname = processname;
        this.serviceName = serviceName;
        this.servicePath = servicepath;
        this.pidFile = pidFile;
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getPidFile() {
        return pidFile;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServicePath() {
        return servicePath;
    }

    public String getProcessname() {
        return processname;
    }

}
