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
package com.cloud.network;

import java.util.List;

import com.cloud.api.commands.AddVpnUserCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.CreateRemoteAccessVpnCmd;
import com.cloud.api.commands.DeleteRemoteAccessVpnCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.RemoveVpnUserCmd;
import com.cloud.api.commands.RestartNetworkCmd;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.offering.NetworkOffering;


public interface NetworkService {
    
    List<? extends Network> getVirtualNetworksOwnedByAccountInZone(String accountName, long domainId, long zoneId);
    
    List<? extends NetworkOffering> listNetworkOfferings();
    /**
     * Associates a public IP address for a router.
     * @param cmd - the command specifying ipAddress
     * @return ip address object
     * @throws ResourceAllocationException, InsufficientCapacityException 
     */
    IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException, ResourceUnavailableException;    
    boolean disassociateIpAddress(DisassociateIPAddrCmd cmd);

    /**
     * Create a remote access vpn from the given ip address and client ip range
     * @param cmd the command specifying the ip address, ip range
     * @return the newly created RemoteAccessVpnVO if successful, null otherwise
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     * @throws ConcurrentOperationException 
     */
    RemoteAccessVpn createRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Start a remote access vpn for the given ip address and client ip range
     * @param cmd the command specifying the ip address, ip range
     * @return the RemoteAccessVpnVO if successful, null otherwise
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     */
    RemoteAccessVpn startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Destroy a previously created remote access VPN
     * @param cmd the command specifying the account and zone
     * @return success if successful, false otherwise
     * @throws ConcurrentOperationException 
     */
    boolean destroyRemoteAccessVpn(DeleteRemoteAccessVpnCmd cmd) throws ConcurrentOperationException;

    VpnUser addVpnUser(AddVpnUserCmd cmd) throws ConcurrentOperationException, AccountLimitException;

    boolean removeVpnUser(RemoveVpnUserCmd cmd) throws ConcurrentOperationException;
    
    Network createNetwork(CreateNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    List<? extends Network> searchForNetworks(ListNetworksCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    boolean deleteNetwork(long networkId) throws InvalidParameterValueException, PermissionDeniedException;
    
    boolean restartNetwork(RestartNetworkCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException;
    
    int getActiveNicsInNetwork(long networkId);
}
