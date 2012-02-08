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

/**
 * 
 */
package com.cloud.dc;

import java.util.Map;

import com.cloud.org.Grouping;

/**
 *
 */
public interface DataCenter extends Grouping {
    public enum NetworkType {
        Basic,
        Advanced,
    }

    long getId();

    String getDns1();

    String getDns2();

    String getGuestNetworkCidr();

    String getName();

    Long getDomainId();

    String getDescription();

    String getDomain();

    NetworkType getNetworkType();

    String getInternalDns1();

    String getInternalDns2();

    String getDnsProvider();

    String getGatewayProvider();

    String getFirewallProvider();

    String getDhcpProvider();

    String getLoadBalancerProvider();

    String getUserDataProvider();

    String getVpnProvider();

    boolean isSecurityGroupEnabled();

    Map<String, String> getDetails();

    void setDetails(Map<String, String> details);

    AllocationState getAllocationState();

    String getZoneToken();

}
