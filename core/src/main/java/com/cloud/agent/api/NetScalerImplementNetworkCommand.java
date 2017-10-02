//
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
//

package com.cloud.agent.api;

public class NetScalerImplementNetworkCommand extends Command {
    private String _networkDetails;

    public NetScalerImplementNetworkCommand() {
        super();
    }

    private String dcId;
    private Long hostId;

    public NetScalerImplementNetworkCommand(String dcId) {
        super();
        this.dcId = dcId;
    }

    public NetScalerImplementNetworkCommand(String dcId, Long hostId, String networkDetails) {
        this(dcId);
        this.hostId = hostId;
        this._networkDetails = networkDetails;
    }

    public void setDetails(String details) {
        _networkDetails = details;
    }

    public String getDetails() {
        return _networkDetails;
    }

    public String getDataCenterId() {
        return dcId;
    }

    @Override
    public boolean executeInSequence() {
        //TODO checkout whether we need to mark it true ??
        //Marking it true is causing another guest network execution in queue
        return false;
    }

    public Long getHostId() {
        return hostId;
    }
}
