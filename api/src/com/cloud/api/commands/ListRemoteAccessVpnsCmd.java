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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.network.RemoteAccessVpn;

@Implementation(description="Lists remote access vpns", responseObject=RemoteAccessVpnResponse.class)
public class ListRemoteAccessVpnsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger (ListRemoteAccessVpnsCmd.class.getName());

    private static final String s_name = "listremoteaccessvpnsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="the account of the remote access vpn. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="the domain ID of the remote access vpn rule. If used with the account parameter, lists remote access vpns for the account in the specified domain.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, description="the ID of the remote access vpn")
    private Long id;

    @Parameter(name="zoneid", type=CommandType.LONG, description="the zone ID of the remote access vpn rule")
    private Long zoneId;
 
    @Parameter(name="publicip", type=CommandType.STRING, description="the public IP address of the remote access vpn ")
    private String publicIp;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}

	public Long getZoneId() {
		return zoneId;
	}

    public String getPublicIp() {
        return publicIp;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends RemoteAccessVpn> vpns = _mgr.searchForRemoteAccessVpns(this);
        ListResponse<RemoteAccessVpnResponse> response = new ListResponse<RemoteAccessVpnResponse>();
        List<RemoteAccessVpnResponse> vpnResponses = new ArrayList<RemoteAccessVpnResponse>();
        for (RemoteAccessVpn vpn : vpns) {
            vpnResponses.add(_responseGenerator.createRemoteAccessVpnResponse(vpn));
        }

        response.setResponses(vpnResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
