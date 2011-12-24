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
package com.cloud.server;

import java.util.Date;
import java.util.List;

import com.cloud.event.EventVO;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;

/**
 * ManagementServer is the interface to talk to the Managment Server. This will be the line drawn between the UI and MS. If we
 * need to build a wire protocol, it will be built on top of this java interface.
 */
public interface ManagementServer extends ManagementService {
    
    /**
     * returns the instance id of this management server.
     * 
     * @return id of the management server
     */
    long getId();
    
    /**
     * Fetches the version of cloud stack
    */
    @Override
    String getVersion();
    
    String[] getApiConfig();

    /**
     * Retrieves a host by id
     * 
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);

    /**
     * Retrieves all Events between the start and end date specified
     * 
     * @param userId
     *            unique id of the user, pass in -1 to retrieve events for all users
     * @param accountId
     *            unique id of the account (which could be shared by many users), pass in -1 to retrieve events for all accounts
     * @param domainId
     *            the id of the domain in which to search for users (useful when -1 is passed in for userId)
     * @param type
     *            the type of the event.
     * @param level
     *            INFO, WARN, or ERROR
     * @param startDate
     *            inclusive.
     * @param endDate
     *            inclusive. If date specified is greater than the current time, the system will use the current time.
     * @return List of events
     */
    List<EventVO> getEvents(long userId, long accountId, Long domainId, String type, String level, Date startDate, Date endDate);

    //FIXME - move all console proxy related commands to corresponding managers
    ConsoleProxyInfo getConsoleProxyForVm(long dataCenterId, long userVmId);

    String getConsoleAccessUrlRoot(long vmId);
    
    GuestOSVO getGuestOs(Long guestOsId);

    /**
     * Returns the vnc port of the vm.
     * 
     * @param VirtualMachine vm
     * @return the vnc port if found; -1 if unable to find.
     */
    Pair<String, Integer> getVncPort(VirtualMachine vm);

    public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType);

    List<? extends StoragePoolVO> searchForStoragePools(Criteria c);

    String getHashKey();
}
