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
package com.cloud.network.vpn;

import java.util.List;

import org.apache.cloudstack.api.command.user.vpn.ListRemoteAccessVpnsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnUsersCmd;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public interface RemoteAccessVpnService {
    static final String RemoteAccessVpnClientIpRangeCK = "remote.access.vpn.client.iprange";

    RemoteAccessVpn createRemoteAccessVpn(long vpnServerAddressId, String ipRange, boolean openFirewall, Boolean forDisplay) throws NetworkRuleConflictException;

    boolean destroyRemoteAccessVpnForIp(long ipId, Account caller, boolean forceCleanup) throws ResourceUnavailableException;

    RemoteAccessVpn startRemoteAccessVpn(long vpnServerAddressId, boolean openFirewall) throws ResourceUnavailableException;

    VpnUser addVpnUser(long vpnOwnerId, String userName, String password);

    boolean removeVpnUser(long vpnOwnerId, String userName, Account caller);

    List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName);

    boolean applyVpnUsers(long vpnOwnerId, String userName, boolean forRemove) throws ResourceUnavailableException;

    boolean applyVpnUsers(long vpnOwnerId, String userName) throws ResourceUnavailableException;

    Pair<List<? extends RemoteAccessVpn>, Integer> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd);

    Pair<List<? extends VpnUser>, Integer> searchForVpnUsers(ListVpnUsersCmd cmd);

    List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId);

    RemoteAccessVpn getRemoteAccessVpn(long vpnAddrId);

    RemoteAccessVpn getRemoteAccessVpnById(long vpnId);

    RemoteAccessVpn updateRemoteAccessVpn(long id, String customId, Boolean forDisplay);

}
