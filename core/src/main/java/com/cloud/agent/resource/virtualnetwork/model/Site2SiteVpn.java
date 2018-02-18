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

public class Site2SiteVpn extends ConfigBase {

    private String localPublicIp, localGuestCidr, localPublicGateway, peerGatewayIp, peerGuestCidrList, espPolicy, ikePolicy, ipsecPsk;
    private Long ikeLifetime, espLifetime;
    private boolean create, dpd, passive, encap;

    public Site2SiteVpn() {
        super(ConfigBase.SITE2SITEVPN);
    }

    public Site2SiteVpn(String localPublicIp, String localGuestCidr, String localPublicGateway, String peerGatewayIp, String peerGuestCidrList, String espPolicy,
 String ikePolicy,
            String ipsecPsk, Long ikeLifetime, Long espLifetime, boolean create, Boolean dpd, boolean passive, boolean encap) {
        super(ConfigBase.SITE2SITEVPN);
        this.localPublicIp = localPublicIp;
        this.localGuestCidr = localGuestCidr;
        this.localPublicGateway = localPublicGateway;
        this.peerGatewayIp = peerGatewayIp;
        this.peerGuestCidrList = peerGuestCidrList;
        this.espPolicy = espPolicy;
        this.ikePolicy = ikePolicy;
        this.ipsecPsk = ipsecPsk;
        this.ikeLifetime = ikeLifetime;
        this.espLifetime = espLifetime;
        this.create = create;
        this.dpd = dpd;
        this.passive = passive;
        this.encap = encap;
    }

    public String getLocalPublicIp() {
        return localPublicIp;
    }

    public void setLocalPublicIp(String localPublicIp) {
        this.localPublicIp = localPublicIp;
    }

    public String getLocalGuestCidr() {
        return localGuestCidr;
    }

    public void setLocalGuestCidr(String localGuestCidr) {
        this.localGuestCidr = localGuestCidr;
    }

    public String getLocalPublicGateway() {
        return localPublicGateway;
    }

    public void setLocalPublicGateway(String localPublicGateway) {
        this.localPublicGateway = localPublicGateway;
    }

    public String getPeerGatewayIp() {
        return peerGatewayIp;
    }

    public void setPeerGatewayIp(String peerGatewayIp) {
        this.peerGatewayIp = peerGatewayIp;
    }

    public String getPeerGuestCidrList() {
        return peerGuestCidrList;
    }

    public void setPeerGuestCidrList(String peerGuestCidrList) {
        this.peerGuestCidrList = peerGuestCidrList;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public void setEspPolicy(String espPolicy) {
        this.espPolicy = espPolicy;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public void setIkePolicy(String ikePolicy) {
        this.ikePolicy = ikePolicy;
    }

    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    public Long getIkeLifetime() {
        return ikeLifetime;
    }

    public void setIkeLifetime(Long ikeLifetime) {
        this.ikeLifetime = ikeLifetime;
    }

    public Long getEspLifetime() {
        return espLifetime;
    }

    public void setEspLifetime(Long espLifetime) {
        this.espLifetime = espLifetime;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isDpd() {
        return dpd;
    }

    public void setDpd(boolean dpd) {
        this.dpd = dpd;
    }

    public boolean isPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public boolean getEncap() {
        return encap;
    }

    public void setEncap(boolean encap) {
        this.encap = encap;
    }

}
