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

package com.cloud.agent.api.routing;

public class RemoteAccessVpnCfgCommand extends NetworkElementCommand {

    boolean create;
    private boolean vpcEnabled;
    String vpnServerIp;
    String ipRange;
    String presharedKey;
    String localIp;
    private String localCidr;
    private String publicInterface;

    // items related to VPN IKEv2 implementation
    private String vpnType;
    private String caCert;
    private String serverCert;
    private String serverKey;

    protected RemoteAccessVpnCfgCommand() {
        this.create = false;
    }

    public boolean isCreate() {
        return create;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public RemoteAccessVpnCfgCommand(boolean create, String vpnServerAddress, String localIp, String ipRange, String ipsecPresharedKey, boolean vpcEnabled, String vpnType,
            String caCert, String serverCert, String serverKey) {
        this.vpnServerIp = vpnServerAddress;
        this.ipRange = ipRange;
        this.presharedKey = ipsecPresharedKey;
        this.localIp = localIp;
        this.create = create;
        this.vpcEnabled = vpcEnabled;
        if (vpcEnabled) {
            this.setPublicInterface("eth1");
        } else {
            this.setPublicInterface("eth2");
        }
        this.vpnType = vpnType;
        this.caCert = caCert;
        this.serverCert = serverCert;
        this.serverKey = serverKey;
    }

    public String getVpnServerIp() {
        return vpnServerIp;
    }

    public void setVpnServerIp(String vpnServerIp) {
        this.vpnServerIp = vpnServerIp;
    }

    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    public String getPresharedKey() {
        return presharedKey;
    }

    public void setPresharedKey(String presharedKey) {
        this.presharedKey = presharedKey;
    }

    public String getLocalIp() {
        return localIp;
    }

    public boolean isVpcEnabled() {
        return vpcEnabled;
    }

    public void setVpcEnabled(boolean vpcEnabled) {
        this.vpcEnabled = vpcEnabled;
    }

    public String getLocalCidr() {
        return localCidr;
    }

    public void setLocalCidr(String localCidr) {
        this.localCidr = localCidr;
    }

    public String getPublicInterface() {
        return publicInterface;
    }

    public void setPublicInterface(String publicInterface) {
        this.publicInterface = publicInterface;
    }

    public String getVpnType() {
        return vpnType;
    }

    public void setVpnType(String vpnType) {
        this.vpnType = vpnType;
    }

    public String getCaCert() {
        return caCert;
    }

    public void setCaCert(String caCert) {
        this.caCert = caCert;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public String getServerKey() {
        return serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

}
