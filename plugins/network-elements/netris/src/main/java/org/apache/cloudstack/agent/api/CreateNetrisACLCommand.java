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
package org.apache.cloudstack.agent.api;

import org.apache.cloudstack.resource.NetrisPortGroup;

public class CreateNetrisACLCommand extends NetrisCommand {
    private String vpcName;
    private Long vpcId;
    private String action;
    private String destPrefix;
    private String sourcePrefix;
    private Integer destPortStart;
    private Integer destPortEnd;
    private Integer icmpType;
    private String protocol;
    private NetrisPortGroup portGroup;
    private String netrisAclName;
    private String reason;

    public CreateNetrisACLCommand(long zoneId, Long accountId, Long domainId, String name, Long id, String vpcName, Long vpcId, boolean isVpc, String action,
                                  String sourcePrefix, String destPrefix, Integer destPortStart, Integer destPortEnd, String protocol) {
        super(zoneId, accountId, domainId, name, id, isVpc);
        this.vpcName = vpcName;
        this.vpcId = vpcId;
        this.action = action;
        this.sourcePrefix = sourcePrefix;
        this.destPrefix = destPrefix;
        this.destPortStart = destPortStart;
        this.destPortEnd = destPortEnd;
        this.protocol = protocol;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    public String getAction() {
        return action;
    }

    public String getDestPrefix() {
        return destPrefix;
    }

    public String getSourcePrefix() {
        return sourcePrefix;
    }

    public Integer getDestPortStart() {
        return destPortStart;
    }

    public Integer getDestPortEnd() {
        return destPortEnd;
    }

    public String getProtocol() {
        return protocol;
    }

    public NetrisPortGroup getPortGroup() {
        return portGroup;
    }

    public void setPortGroup(NetrisPortGroup portGroup) {
        this.portGroup = portGroup;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public String getNetrisAclName() {
        return netrisAclName;
    }

    public void setNetrisAclName(String netrisAclName) {
        this.netrisAclName = netrisAclName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
