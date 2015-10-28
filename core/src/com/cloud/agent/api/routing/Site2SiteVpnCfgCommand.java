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

public class Site2SiteVpnCfgCommand extends NetworkElementCommand {

    private Boolean create;
    private String localPublicIp;
    private String localGuestCidr;
    private String localPublicGateway;
    private String peerGatewayIp;
    private String peerGuestCidrList;
    private String ipsecPsk;
    private String ikePolicy;
    private String espPolicy;
    private Long ikeLifetime;
    private Long espLifetime;
    private Boolean dpd;
    private Boolean passive;

    @Override
    public Boolean executeInSequence() {
        return true;
    }

    public Site2SiteVpnCfgCommand() {
        this.create = false;
    }

    public Site2SiteVpnCfgCommand(Boolean create, String localPublicIp, String localPublicGateway, String localGuestCidr, String peerGatewayIp, String peerGuestCidrList,
            String ikePolicy, String espPolicy, String ipsecPsk, Long ikeLifetime, Long espLifetime, Boolean dpd, Boolean passive) {
        this.create = create;
        this.setLocalPublicIp(localPublicIp);
        this.setLocalPublicGateway(localPublicGateway);
        this.setLocalGuestCidr(localGuestCidr);
        this.setPeerGatewayIp(peerGatewayIp);
        this.setPeerGuestCidrList(peerGuestCidrList);
        this.ipsecPsk = ipsecPsk;
        this.ikePolicy = ikePolicy;
        this.espPolicy = espPolicy;
        this.ikeLifetime = ikeLifetime;
        this.espLifetime = espLifetime;
        this.dpd = dpd;
        this.passive = passive;
    }

    public Boolean isCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public void setIkePolicy(String ikePolicy) {
        this.ikePolicy = ikePolicy;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public void setEspPolicy(String espPolicy) {
        this.espPolicy = espPolicy;
    }

    public Long getIkeLifetime() {
        return ikeLifetime;
    }

    public void setikeLifetime(Long ikeLifetime) {
        this.ikeLifetime = ikeLifetime;
    }

    public Long getEspLifetime() {
        return espLifetime;
    }

    public void setEspLifetime(Long espLifetime) {
        this.espLifetime = espLifetime;
    }

    public Boolean getDpd() {
        return dpd;
    }

    public void setDpd(Boolean dpd) {
        this.dpd = dpd;
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

    public Boolean isPassive() {
        return passive;
    }

    public void setPassive(Boolean passive) {
        this.passive = passive;
    }
}
