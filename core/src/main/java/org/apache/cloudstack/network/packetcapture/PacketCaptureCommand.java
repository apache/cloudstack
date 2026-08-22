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
package org.apache.cloudstack.network.packetcapture;

import com.cloud.agent.api.Command;

/**
 * Starts, stops or queries the packet capture of a VM NIC on the host the
 * VM is running on. The NIC is identified by its MAC address on the running
 * domain; the remaining fields are passed to the capture script as context.
 */
public class PacketCaptureCommand extends Command {

    public enum Action {
        START, STOP, STATUS
    }

    private Action action;
    private String vmName;
    private String vmUuid;
    private String nicUuid;
    private String macAddress;
    private String ip4Address;
    private String ip6Address;
    private String networkUuid;

    public PacketCaptureCommand() {
    }

    public PacketCaptureCommand(Action action, String vmName, String vmUuid, String nicUuid, String macAddress,
            String ip4Address, String ip6Address, String networkUuid) {
        this.action = action;
        this.vmName = vmName;
        this.vmUuid = vmUuid;
        this.nicUuid = nicUuid;
        this.macAddress = macAddress;
        this.ip4Address = ip4Address;
        this.ip6Address = ip6Address;
        this.networkUuid = networkUuid;
    }

    public Action getAction() {
        return action;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIp4Address() {
        return ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
