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


public class AttachVolumeAnswer extends Answer {
    private Long deviceId;
    private String vdiUuid;
    private String chainInfo;

    public AttachVolumeAnswer(AttachVolumeCommand cmd, String result) {
        super(cmd, false, result);
        this.deviceId = null;
    }

    public AttachVolumeAnswer(AttachVolumeCommand cmd, Long deviceId) {
        super(cmd);
        this.deviceId = deviceId;
        this.vdiUuid = "";
    }

    public AttachVolumeAnswer(AttachVolumeCommand cmd, Long deviceId, String vdiUuid) {
        super(cmd);
        this.deviceId = deviceId;
        this.vdiUuid = vdiUuid;
    }

    public AttachVolumeAnswer(AttachVolumeCommand cmd) {
        super(cmd);
        this.deviceId = null;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getVdiUuid() {
    	return vdiUuid;
    }
    
    public void setChainInfo(String chainInfo) {
    	this.chainInfo = chainInfo;
    }

    public String getChainInfo() {
    	return chainInfo;
    }
}
