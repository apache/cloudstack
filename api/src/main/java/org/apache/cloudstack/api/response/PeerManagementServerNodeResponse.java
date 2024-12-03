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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.management.ManagementServerHost.State;

import java.util.Date;

public class PeerManagementServerNodeResponse extends BaseResponse {

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the management server peer")
    private State state;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the last updated time of the management server peer state")
    private Date lastUpdated;

    @SerializedName(ApiConstants.PEER_ID)
    @Param(description = "the ID of the peer management server")
    private String peerId;

    @SerializedName(ApiConstants.PEER_NAME)
    @Param(description = "the name of the peer management server")
    private String peerName;

    @SerializedName(ApiConstants.PEER_MSID)
    @Param(description = "the management ID of the peer management server")
    private String peerMsId;

    @SerializedName(ApiConstants.PEER_RUNID)
    @Param(description = "the run ID of the peer management server")
    private String peerRunId;

    @SerializedName(ApiConstants.PEER_STATE)
    @Param(description = "the state of the peer management server")
    private String peerState;

    @SerializedName(ApiConstants.PEER_SERVICE_IP)
    @Param(description = "the IP Address for the peer Management Server")
    private String peerServiceIp;

    @SerializedName(ApiConstants.PEER_SERVICE_PORT)
    @Param(description = "the service port for the peer Management Server")
    private String peerServicePort;

    public void setState(State state) {
        this.state = state;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public void setPeerMsId(String peerMsId) {
        this.peerMsId = peerMsId;
    }

    public void setPeerRunId(String peerRunId) {
        this.peerRunId = peerRunId;
    }

    public void setPeerState(String peerState) {
        this.peerState = peerState;
    }

    public void setPeerServiceIp(String peerServiceIp) {
        this.peerServiceIp = peerServiceIp;
    }

    public void setPeerServicePort(String peerServicePort) {
        this.peerServicePort = peerServicePort;
    }
}
