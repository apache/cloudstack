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
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

public interface DataCenterDao extends GenericDao<DataCenterVO, Long> {
    DataCenterVO findByName(String name);
    
    /**
     * @param id data center id
     * @return a pair of mac address strings.  The first one is private and second is public.
     */
    String[] getNextAvailableMacAddressPair(long id);
    String[] getNextAvailableMacAddressPair(long id, long mask);
    Pair<String, Long> allocatePrivateIpAddress(long id, long podId, long instanceId, String reservationId);
    String allocateLinkLocalIpAddress(long id, long podId, long instanceId, String reservationId);
    String allocateVnet(long dcId, long physicalNetworkId, long accountId, String reservationId);
    
    void releaseVnet(String vnet, long dcId, long physicalNetworkId, long accountId, String reservationId);
    void releasePrivateIpAddress(String ipAddress, long dcId, Long instanceId);
    void releasePrivateIpAddress(long nicId, String reservationId);
    void releaseLinkLocalIpAddress(String ipAddress, long dcId, Long instanceId);
    void releaseLinkLocalIpAddress(long nicId, String reservationId);
    
    boolean deletePrivateIpAddressByPod(long podId);
    boolean deleteLinkLocalIpAddressByPod(long podId);
    
    void addPrivateIpAddress(long dcId,long podId, String start, String end);
    void addLinkLocalIpAddress(long dcId,long podId, String start, String end);
    
    List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet);

    String allocatePodVlan(long podId, long accountId);

	List<DataCenterVO> findZonesByDomainId(Long domainId);

	List<DataCenterVO> listPublicZones();

	List<DataCenterVO> findChildZones(Object[] ids);

    void loadDetails(DataCenterVO zone);
    void saveDetails(DataCenterVO zone);
    
    List<DataCenterVO> listDisabledZones();
    List<DataCenterVO> listEnabledZones();
    DataCenterVO findByToken(String zoneToken);    
    DataCenterVO findByTokenOrIdOrName(String tokenIdOrName);
    
    void addVnet(long dcId, long physicalNetworkId, int start, int end);
    void deleteVnet(long physicalNetworkId);
    List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId);
}
