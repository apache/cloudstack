//
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
//

package com.cloud.network.resource;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.cloud.utils.mgmt.ManagementBean;

/**
 * Created by sgoeminn on 1/24/17.
 */
public interface VspStatisticsMBean extends ManagementBean {

    /**
     * Returns the global count of all the VSD calls since start up
     * @return
     */
    long getVSDStatistics();

    /**
     * Returns the count of all the vsd calls where the entityType is equal to the entity
     * @param entity
     * @return
     */
    long getVsdStatisticsByEntityType(String entity);

    /**
     * Returns the count of all the vsd calls where the requestType is equal to the requestType
     * @param requestType
     * @return
     */
    long getVsdStatisticsByRequestType(String requestType);

    /**
     * Returns the count of all the vsd calls where the EntityType is equal to the entity
     * and the RequestType is equal to the requestType string.
     * @param entity
     * @param requestType
     * @return
     */
    long getVsdStatisticsByEntityAndRequestType(String entity, String requestType);

    /**
     * Returns the count of all VSD calls with EntityType entity and RequestType type
     * @return
     */
    Map<String, Map<String, AtomicLong>> getVsdStatisticsReport();
}
