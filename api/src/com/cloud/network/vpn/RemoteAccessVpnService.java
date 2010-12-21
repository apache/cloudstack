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

import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;


public interface RemoteAccessVpnService {

    RemoteAccessVpn createRemoteAccessVpn(long zoneId, long ownerId, String publicIp, String ipRange);
    RemoteAccessVpn destroyRemoteAccessVpn(long zoneId, long ownerId);
    List<? extends RemoteAccessVpn> listRemoteAccessVpns(long vpnOwnerId, long zoneId, String publicIp);

    VpnUser addVpnUser(long vpnOwnerId, String userName, String password);
    VpnUser removeVpnUser(long vpnOwnerId, String userName);
    List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName);
    
}
