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

package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterDao extends GenericDao<DataCenterVO, Long> {
    DataCenterVO findByName(String name);
    
    /**
     * @param id data center id
     * @return a pair of mac address strings.  The first one is private and second is public.
     */
    String[] getNextAvailableMacAddressPair(long id);
    String[] getNextAvailableMacAddressPair(long id, long mask);
    String allocatePrivateIpAddress(long id, long podId, long instanceId);
    String allocateLinkLocalPrivateIpAddress(long id, long podId, long instanceId);
    String allocateVnet(long dcId, long accountId);
    
    void releaseVnet(String vnet, long dcId, long accountId);
    void releasePrivateIpAddress(String ipAddress, long dcId, Long instanceId);
    void releaseLinkLocalPrivateIpAddress(String ipAddress, long dcId, Long instanceId);
    boolean deletePrivateIpAddressByPod(long podId);
    boolean deleteLinkLocalPrivateIpAddressByPod(long podId);
    
    void addPrivateIpAddress(long dcId,long podId, String start, String end);
    void addLinkLocalPrivateIpAddress(long dcId,long podId, String start, String end);
    
    List<DataCenterVnetVO> findVnet(long dcId, String vnet);
    
    void addVnet(long dcId, int start, int end);
    
    void deleteVnet(long dcId);
    
    List<DataCenterVnetVO> listAllocatedVnets(long dcId);

    String allocatePodVlan(long podId, long accountId);
}
