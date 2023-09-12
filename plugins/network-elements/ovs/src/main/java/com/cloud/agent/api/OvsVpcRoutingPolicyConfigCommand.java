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
 * This command represents logical view of VM's connectivity in VPC.
 */
public class OvsVpcRoutingPolicyConfigCommand extends Command {

    VpcConfig vpcConfig =null;
    long hostId;
    String bridgeName;
    long sequenceNumber;
    String schemaVersion;

    public static class AclItem {
        int number;
        String uuid;
        String action;
        String direction;
        String sourcePortStart;
        String sourcePortEnd;
        String protocol;
        String[] sourceCidrs;
        public AclItem(int number, String uuid, String action, String direction, String sourcePortStart,
                       String sourcePortEnd, String protocol, String[] sourceCidrs) {
            this.number = number;
            this.uuid =uuid;
            this.action = action;
            this.direction = direction;
            this.sourceCidrs = sourceCidrs;
            this.sourcePortStart = sourcePortStart;
            this.sourcePortEnd = sourcePortEnd;
            this.protocol = protocol;
        }
    }

    public static class Acl {
        String id;
        AclItem[] aclItems;
        public Acl(String uuid, AclItem[] aclItems) {
            this.id = uuid;
            this.aclItems = aclItems;
        }
    }

    public static class Tier {
        String id;
        String cidr;
        String aclId;
        public Tier(String uuid, String cidr, String aclId) {
            this.id = uuid;
            this.cidr = cidr;
            this.aclId = aclId;
        }
    }

    public class Vpc {
        String cidr;
        String id;
        Acl[] acls;
        Tier[] tiers;
        public Vpc(String id, String cidr, Acl[] acls, Tier[] tiers) {
            this.id = id;
            this.cidr = cidr;
            this.acls = acls;
            this.tiers = tiers;
        }
    }

    public static class VpcConfig {
        Vpc vpc;
        public VpcConfig(Vpc vpc) {
            this.vpc = vpc;
        }
    }

    public OvsVpcRoutingPolicyConfigCommand(String id, String cidr, Acl[] acls, Tier[] tiers) {
        Vpc vpc = new Vpc(id, cidr, acls, tiers);
        vpcConfig = new VpcConfig(vpc);
    }

    public String getVpcConfigInJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(vpcConfig).toLowerCase();
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

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
