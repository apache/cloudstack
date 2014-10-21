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
package com.cloud.offerings.dao;

import java.util.List;
import java.util.Map;

import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkOfferingDao deals with searches and operations done on the
 * network_offering table.
 *
 */
public interface NetworkOfferingDao extends GenericDao<NetworkOfferingVO, Long> {
    /**
     * Returns the network offering that matches the name.
     *
     * @param uniqueName
     *            name
     * @return NetworkOfferingVO
     */
    NetworkOfferingVO findByUniqueName(String uniqueName);

    /**
     * If not, then it persists it into the database.
     *
     * @param offering
     *            network offering to persist if not in the database.
     * @return NetworkOfferingVO backed by a row in the database
     */
    NetworkOfferingVO persistDefaultNetworkOffering(NetworkOfferingVO offering);

    List<NetworkOfferingVO> listSystemNetworkOfferings();

    List<NetworkOfferingVO> listByAvailability(Availability availability, boolean isSystem);

    List<Long> getOfferingIdsToUpgradeFrom(NetworkOffering originalOffering);

    List<NetworkOfferingVO> listByTrafficTypeGuestTypeAndState(NetworkOffering.State state, TrafficType trafficType, Network.GuestType type);

    NetworkOfferingVO persist(NetworkOfferingVO off, Map<Detail, String> details);

}
