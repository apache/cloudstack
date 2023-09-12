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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This command represents physical view of how a VPC is laid out on the physical infrastructure. Contains information:
 *   - on which hypervisor hosts VPC spans (host running at least one VM from the VPC)
 *   - information of tiers, so we can figure how one VM can talk to a different VM in same tier or different tier
 *   - information on all the VM's in the VPC.
 *   - information of NIC's of each VM in the VPC
 */
public class OvsVpcPhysicalTopologyConfigCommand extends Command {

    VpcConfig vpcConfig =null;
    long hostId;
    String bridgeName;

    long sequenceNumber;
    String schemaVersion;

    public static class Host {
        long hostId;
        String ipAddress;

        public Host (long hostId, String ipAddress) {
            this.hostId = hostId;
            this.ipAddress = ipAddress;
        }
    }

    public static class Nic {
        String ipAddress;
        String macAddress;
        String networkUuid;
        public Nic (String ipAddress, String macAddress, String networkUuid) {
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
            this.networkUuid = networkUuid;
        }
    }

    public static class Tier {
        long greKey;
        String networkUuid;
        String gatewayIp;
        String gatewayMac;
        String cidr;
        public Tier(long greKey, String networkUuid, String gatewayIp, String gatewayMac, String cidr) {
            this.greKey = greKey;
            this.networkUuid = networkUuid;
            this.gatewayIp = gatewayIp;
            this.gatewayMac = gatewayMac;
            this.cidr = cidr;
        }
    }

    public static class Vm {
        long hostId;
        Nic[] nics;
        public Vm(long hostId, Nic[] nics) {
            this.hostId = hostId;
            this.nics = nics;
        }
    }

    public static class Vpc {
        String cidr;
        Host[] hosts;
        Tier[] tiers;
        Vm[]  vms;
        public Vpc(Host[] hosts, Tier[] tiers, Vm[] vms, String cidr) {
            this.hosts = hosts;
            this.tiers = tiers;
            this.vms = vms;
            this.cidr = cidr;
        }
    }

    public static class VpcConfig {
        Vpc vpc;
        public VpcConfig(Vpc vpc) {
            this.vpc = vpc;
        }
    }

    public OvsVpcPhysicalTopologyConfigCommand(Host[] hosts, Tier[] tiers, Vm[] vms, String cidr) {
        Vpc vpc = new Vpc(hosts, tiers, vms, cidr);
        vpcConfig = new VpcConfig(vpc);
    }

    public String getVpcConfigInJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(vpcConfig).toLowerCase();
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getHostId() {
        return hostId;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
