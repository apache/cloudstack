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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.utils.net.Ip;

@Implementation(description="Destroys a l2tp/ipsec remote access vpn", responseObject=SuccessResponse.class)
public class DeleteRemoteAccessVpnCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteRemoteAccessVpnCmd.class.getName());

    private static final String s_name = "deleteremoteaccessvpnresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name="publicip", type=CommandType.STRING, required=true, description="public ip address of the vpn server")
    private String publicIp;
    
    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.LONG, expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
    public String getCommandName() {
        return s_name;
    }

	@Override
	public long getEntityOwnerId() {
	    if (ownerId == null) {
	        ownerId = _entityMgr.findById(RemoteAccessVpn.class, new Ip(publicIp)).getAccountId();
	    }
	    return ownerId;
    }

	@Override
	public String getEventDescription() {
		return "Delete Remote Access VPN for account " + getEntityOwnerId() + " for  " + publicIp;
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY;
	}

    @Override
    public void execute() throws ResourceUnavailableException {
        _ravService.destroyRemoteAccessVpn(new Ip(publicIp), getStartEventId());
    }
	
}
