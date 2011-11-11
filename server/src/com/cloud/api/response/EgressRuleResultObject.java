/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.response;

import com.cloud.serializer.Param;

public class EgressRuleResultObject {
    @Param(name="id")
    private Long id;

    @Param(name="startport")
    private int startPort;

    @Param(name="endport")
    private int endPort;

    @Param(name="protocol")
    private String protocol;

    @Param(name="securitygroup")
    private String allowedSecurityGroup = null;

    @Param(name="account")
    private String allowedSecGroupAcct = null;

    @Param(name="cidr")
    private String allowedDestinationIpCidr = null;

    public EgressRuleResultObject() { }

    public EgressRuleResultObject(Long id, int startPort, int endPort, String protocol, String allowedSecurityGroup, String allowedSecGroupAcct, String allowedSourceIpCidr) {
        this.id = id;
        this.startPort = startPort;
        this.endPort = endPort;
        this.protocol = protocol;
        this.allowedSecurityGroup = allowedSecurityGroup;
        this.allowedSecGroupAcct = allowedSecGroupAcct;
        this.allowedDestinationIpCidr = allowedSourceIpCidr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAllowedSecurityGroup() {
        return allowedSecurityGroup;
    }

    public void setAllowedSecurityGroup(String allowedSecurityGroup) {
        this.allowedSecurityGroup = allowedSecurityGroup;
    }

    public String getAllowedSecGroupAcct() {
        return allowedSecGroupAcct;
    }

    public void setAllowedSecGroupAcct(String allowedSecGroupAcct) {
        this.allowedSecGroupAcct = allowedSecGroupAcct;
    }

    public String getAllowedDestinationIpCidr() {
        return allowedDestinationIpCidr;
    }

    public void setAllowedDestinationIpCidr(String allowedDestinationIpCidr) {
        this.allowedDestinationIpCidr = allowedDestinationIpCidr;
    }
}
