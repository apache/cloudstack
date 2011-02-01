/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.network.vpn;

import java.util.List;

import com.cloud.api.commands.ListRemoteAccessVpnsCmd;
import com.cloud.api.commands.ListVpnUsersCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;

public interface RemoteAccessVpnService {

    RemoteAccessVpn createRemoteAccessVpn(long vpnServerAddressId, String ipRange) throws NetworkRuleConflictException;
    void destroyRemoteAccessVpn(long vpnServerAddressId, long startEventId) throws ResourceUnavailableException;
    RemoteAccessVpn startRemoteAccessVpn(long vpnServerAddressId) throws ResourceUnavailableException;

    VpnUser addVpnUser(long vpnOwnerId, String userName, String password);
    boolean removeVpnUser(long vpnOwnerId, String userName);
    List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName);
    boolean applyVpnUsers(long vpnOwnerId);
    
    List<? extends RemoteAccessVpn> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd);
    List<? extends VpnUser> searchForVpnUsers(ListVpnUsersCmd cmd);
    
    List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId);

}
