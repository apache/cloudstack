/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.cluster.ClusterDrsEventsVO;

public interface ClusterDrsEventsDao extends GenericDao<ClusterDrsEventsVO, Long> {

    /**
     * Retrieves the last automated DRS event for a given cluster within a specified
     * time interval.
     *
     * @param clusterId the ID of the cluster to retrieve the DRS event for
     * @param interval  the time interval in minutes
     * @return the last automated DRS event for the given cluster within the
     * specified time interval, or null if not found
     */
    ClusterDrsEventsVO lastAutomatedDrsEventInInterval(Long clusterId, int interval);

    /**
     * Removes all DRS events that occurred before a specified time interval.
     *
     * @param interval the time interval in days
     * @return the number of DRS events removed
     */
    int removeDrsEventsBeforeInterval(int interval);
}
