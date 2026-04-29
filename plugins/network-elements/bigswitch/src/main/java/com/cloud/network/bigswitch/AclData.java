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

package com.cloud.network.bigswitch;

import java.util.Locale;

import com.cloud.network.vpc.NetworkACLItem;
import com.google.gson.annotations.SerializedName;

/**
 * AclData contains information expected by Big Switch Controller
 * in ApplyBcfAclCommand
 */

public class AclData {
    @SerializedName("id") private String id;
    @SerializedName("priority") private int priority;
    @SerializedName("action") private String action;
    @SerializedName("ipproto") private String ipProto;
    @SerializedName("source") private AclNetwork source;
    @SerializedName("destination") private AclNetwork destination;

    public AclData(){
        this.id = null;
        this.priority = 0;
        this.action = null;
        this.ipProto = null;
        this.source = new AclNetwork();
        this.destination = new AclNetwork();
    }

    public class AclNetwork{
        @SerializedName("cidr") final private String cidr;
        @SerializedName("port") final private Integer port;

        public AclNetwork(){
            this.cidr = null;
            this.port = null;
        }

        public AclNetwork(final String cidr, final Integer port){
            this.cidr = cidr;
            this.port = port;
        }
    }

    public String getId() {
        return id;
    }
    public void setId(final String id) {
        this.id = id;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(final int priority) {
        this.priority = priority;
    }
    public String getAction() {
        return action;
    }
    public void setAction(final String action) {
        if(action.equalsIgnoreCase(NetworkACLItem.Action.Allow.name())){
            this.action = "permit";
        } else {
            this.action = "deny";
        }
    }
    public String getIpProto() {
        return ipProto;
    }
    public void setIpProto(final String ipProto) {
        if (ipProto != null && !ipProto.equalsIgnoreCase("all")){
            switch(ipProto.toLowerCase(Locale.ENGLISH)){
            case "tcp":
                this.ipProto = "6";
                break;
            case "udp":
                this.ipProto = "17";
                break;
            case "icmp":
                this.ipProto = "1";
                break;
            default:
                throw new IllegalArgumentException("Protocol in ACL rule not supported");
            }
        }
    }
    public AclNetwork getSource() {
        return source;
    }
    public void setSource(final AclNetwork source) {
        this.source = source;
    }
    public AclNetwork getDestination() {
        return destination;
    }
    public void setDestination(final AclNetwork destination) {
        this.destination = destination;
    }
}
