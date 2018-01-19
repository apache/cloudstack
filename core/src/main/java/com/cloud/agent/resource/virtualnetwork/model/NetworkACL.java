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

package com.cloud.agent.resource.virtualnetwork.model;

public class NetworkACL extends ConfigBase {
    private String device;
    private String macAddress;
    private boolean privateGatewayAcl;
    private String nicIp;
    private String nicNetmask;
    private AclRule[] ingressRules;
    private AclRule[] egressRules;

    public NetworkACL() {
        super(ConfigBase.NETWORK_ACL);
    }

    public NetworkACL(String device, String macAddress, boolean privateGatewayAcl, String nicIp, String nicNetmask, AclRule[] ingressRules, AclRule[] egressRules) {
        super(ConfigBase.NETWORK_ACL);
        this.device = device;
        this.macAddress = macAddress;
        this.privateGatewayAcl = privateGatewayAcl;
        this.nicIp = nicIp;
        this.nicNetmask = nicNetmask;
        this.ingressRules = ingressRules;
        this.egressRules = egressRules;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isPrivateGatewayAcl() {
        return privateGatewayAcl;
    }

    public void setPrivateGatewayAcl(boolean privateGatewayAcl) {
        this.privateGatewayAcl = privateGatewayAcl;
    }

    public String getNicIp() {
        return nicIp;
    }

    public void setNicIp(String nicIp) {
        this.nicIp = nicIp;
    }

    public String getNicNetmask() {
        return nicNetmask;
    }

    public void setNicNetmask(String nicNetmask) {
        this.nicNetmask = nicNetmask;
    }

    public AclRule[] getIngressRules() {
        return ingressRules;
    }

    public void setIngressRules(AclRule[] ingressRules) {
        this.ingressRules = ingressRules;
    }

    public AclRule[] getEgressRules() {
        return egressRules;
    }

    public void setEgressRules(AclRule[] egressRules) {
        this.egressRules = egressRules;
    }

}
