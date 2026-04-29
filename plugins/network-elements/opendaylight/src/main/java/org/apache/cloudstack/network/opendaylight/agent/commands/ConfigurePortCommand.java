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

package org.apache.cloudstack.network.opendaylight.agent.commands;

import java.util.UUID;

import com.cloud.agent.api.Command;

public class ConfigurePortCommand extends Command {
    private UUID networkId;
    private String tennantId;
    private String macAddress;
    private UUID portId;

    public ConfigurePortCommand() {
    }

    public ConfigurePortCommand(UUID portId, UUID networkId, String tennantId, String macAddress) {
        this.portId = portId;
        this.networkId = networkId;
        this.tennantId = tennantId;
        this.macAddress = macAddress;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public String getTennantId() {
        return tennantId;
    }

    public void setTennantId(String tennantId) {
        this.tennantId = tennantId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public UUID getPortId() {
        return portId;
    }

    public void setPortId(UUID portId) {
        this.portId = portId;
    }

    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return false;
    }

}
