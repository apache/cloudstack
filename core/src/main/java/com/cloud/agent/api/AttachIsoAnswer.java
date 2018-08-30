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
package com.cloud.agent.api;


public class AttachIsoAnswer extends Answer {
    private Integer deviceKey;


    public AttachIsoAnswer(AttachIsoCommand cmd, String result) {
        super(cmd, false, result);
        this.deviceKey = null;
    }

    public AttachIsoAnswer(AttachIsoCommand cmd, Integer deviceId) {
        super(cmd);
        this.deviceKey = deviceId;
    }

    public AttachIsoAnswer(AttachIsoCommand cmd) {
        super(cmd);
        this.deviceKey = null;
    }

    public AttachIsoAnswer(AttachIsoCommand command, boolean success, String details) {
        super(command,success,details);
        this.deviceKey = null;
    }

    public AttachIsoAnswer(Command command, Exception e) {
        super(command, e);
    }

    public Integer getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(Integer deviceKey) {
        this.deviceKey = deviceKey;
    }
}
