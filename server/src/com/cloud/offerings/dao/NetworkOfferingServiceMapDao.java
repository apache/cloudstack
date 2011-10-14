/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.offerings.dao;

import java.util.List;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkOfferingServiceDao deals with searches and operations done on the
 * ntwk_offering_service_map table.
 *
 */
public interface NetworkOfferingServiceMapDao extends GenericDao<NetworkOfferingServiceMapVO, Long> {
   boolean isServiceSupported(long networkOfferingId, Service service);
   boolean isProviderSupported(long networkOfferingId, Service service, Provider provider);
   List<String> getServicesForProvider(long networkOfferingId, Provider provider);
   List<String> getProvidersForService(long networkOfferingid, Service service);
   List<NetworkOfferingServiceMapVO> getServices(long networkOfferingId);
   void deleteByOfferingId(long networkOfferingId);
}





