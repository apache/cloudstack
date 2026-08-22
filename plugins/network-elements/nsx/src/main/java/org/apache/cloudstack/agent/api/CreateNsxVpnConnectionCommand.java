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

import java.util.List;
import java.util.Objects;

import com.cloud.agent.api.LogLevel;

public class CreateNsxVpnConnectionCommand extends NsxCommand {

    private Long vpcId;
    private String vpcName;
    private long connectionId;
    private String peerAddress;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String psk;
    private String ikePolicy;
    private String espPolicy;
    private Long ikeLifetime;
    private Long espLifetime;
    private boolean dpdEnabled;
    private String ikeVersion;
    private boolean passive;
    private List<String> peerCidrs;
    private String vtiLocalIp;
    private String vtiPeerIp;
    private int vtiPrefixLength;
    private String vpcCidr;
    private String localEndpointIp;

    public CreateNsxVpnConnectionCommand(long domainId, long accountId, long zoneId,
                                         Long vpcId, String vpcName, long connectionId, String peerAddress,
                                         String psk, String ikePolicy, String espPolicy, Long ikeLifetime,
                                         Long espLifetime, boolean dpdEnabled, String ikeVersion, boolean passive,
                                         List<String> peerCidrs, String vtiLocalIp, String vtiPeerIp,
                                         int vtiPrefixLength, String vpcCidr, String localEndpointIp) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.connectionId = connectionId;
        this.peerAddress = peerAddress;
        this.psk = psk;
        this.ikePolicy = ikePolicy;
        this.espPolicy = espPolicy;
        this.ikeLifetime = ikeLifetime;
        this.espLifetime = espLifetime;
        this.dpdEnabled = dpdEnabled;
        this.ikeVersion = ikeVersion;
        this.passive = passive;
        this.peerCidrs = peerCidrs;
        this.vtiLocalIp = vtiLocalIp;
        this.vtiPeerIp = vtiPeerIp;
        this.vtiPrefixLength = vtiPrefixLength;
        this.vpcCidr = vpcCidr;
        this.localEndpointIp = localEndpointIp;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public String getPsk() {
        return psk;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public Long getIkeLifetime() {
        return ikeLifetime;
    }

    public Long getEspLifetime() {
        return espLifetime;
    }

    public boolean isDpdEnabled() {
        return dpdEnabled;
    }

    public String getIkeVersion() {
        return ikeVersion;
    }

    public boolean isPassive() {
        return passive;
    }

    public List<String> getPeerCidrs() {
        return peerCidrs;
    }

    public String getVtiLocalIp() {
        return vtiLocalIp;
    }

    public String getVtiPeerIp() {
        return vtiPeerIp;
    }

    public int getVtiPrefixLength() {
        return vtiPrefixLength;
    }

    public String getVpcCidr() {
        return vpcCidr;
    }

    public String getLocalEndpointIp() {
        return localEndpointIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        CreateNsxVpnConnectionCommand that = (CreateNsxVpnConnectionCommand) o;
        return connectionId == that.connectionId && dpdEnabled == that.dpdEnabled && passive == that.passive
                && vtiPrefixLength == that.vtiPrefixLength
                && Objects.equals(vpcId, that.vpcId) && Objects.equals(vpcName, that.vpcName)
                && Objects.equals(peerAddress, that.peerAddress)
                && Objects.equals(psk, that.psk) && Objects.equals(ikePolicy, that.ikePolicy)
                && Objects.equals(espPolicy, that.espPolicy) && Objects.equals(ikeLifetime, that.ikeLifetime)
                && Objects.equals(espLifetime, that.espLifetime) && Objects.equals(ikeVersion, that.ikeVersion)
                && Objects.equals(peerCidrs, that.peerCidrs) && Objects.equals(vtiLocalIp, that.vtiLocalIp)
                && Objects.equals(vtiPeerIp, that.vtiPeerIp) && Objects.equals(vpcCidr, that.vpcCidr)
                && Objects.equals(localEndpointIp, that.localEndpointIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, vpcName, connectionId, peerAddress, psk, ikePolicy, espPolicy,
                ikeLifetime, espLifetime, dpdEnabled, ikeVersion, passive, peerCidrs, vtiLocalIp, vtiPeerIp,
                vtiPrefixLength, vpcCidr, localEndpointIp);
    }
}
