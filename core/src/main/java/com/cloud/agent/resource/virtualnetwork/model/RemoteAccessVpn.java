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

public class RemoteAccessVpn extends ConfigBase {

    public boolean create;
    public String ipRange, presharedKey, vpnServerIp, localIp, localCidr, publicInterface;

    // items related to VPN IKEv2 implementation
    private String vpnType;
    private String caCert;
    private String serverCert;
    private String serverKey;

    public RemoteAccessVpn() {
        super(ConfigBase.REMOTEACCESSVPN);
    }

    public RemoteAccessVpn(boolean create, String ipRange, String presharedKey, String vpnServerIp, String localIp, String localCidr, String publicInterface, String vpnType,
            String caCert, String serverCert, String serverKey) {
        super(ConfigBase.REMOTEACCESSVPN);
        this.create = create;
        this.ipRange = ipRange;
        this.presharedKey = presharedKey;
        this.vpnServerIp = vpnServerIp;
        this.localIp = localIp;
        this.localCidr = localCidr;
        this.publicInterface = publicInterface;
        this.vpnType = vpnType;
        this.caCert = caCert;
        this.serverCert = serverCert;
        this.serverKey = serverKey;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
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

    public String getVpnServerIp() {
        return vpnServerIp;
    }

    public void setVpnServerIp(String vpnServerIp) {
        this.vpnServerIp = vpnServerIp;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
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
