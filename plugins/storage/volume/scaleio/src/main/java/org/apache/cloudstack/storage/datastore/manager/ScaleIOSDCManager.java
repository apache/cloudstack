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

package org.apache.cloudstack.storage.datastore.manager;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.host.Host;

public interface ScaleIOSDCManager {
    ConfigKey<Boolean> ConnectOnDemand = new ConfigKey<>("Storage",
            Boolean.class,
            "powerflex.connect.on.demand",
            Boolean.FALSE.toString(),
            "When true, connects PowerFlex client on Host when first Volume is mapped to SDC & client connections configured 'storage.pool.connected.clients.limit' are within the limit and disconnects when last Volume is unmapped from SDC; " +
                    "and When false, connects PowerFlex client on Host when host connects to storage pool & client connections configured 'storage.pool.connected.clients.limit' are within the limit and disconnects when host disconnects from storage pool & no volumes mapped to SDC.",
            Boolean.TRUE,
            ConfigKey.Scope.Zone);

    /**
     * Checks SDC connections limit.
     * @param storagePoolId the storage pool id
     * @return true if SDC connections are within limit
     */
    boolean areSDCConnectionsWithinLimit(Long storagePoolId);

    /**
     * Returns connected SDC Id.
     * @param host the host
     * @param dataStore the datastore
     * @return SDC Id of the host
     */
    String getConnectedSdc(Host host, DataStore dataStore);

    /**
     * Prepares the SDC on the host (adds the MDM IPs to SDC, starts scini service if required).
     * @param host the host
     * @param dataStore the datastore
     * @return SDC Id of the host if SDC is successfully prepared-ed on the host
     */
    String prepareSDC(Host host, DataStore dataStore);

    /**
     * Unprepares the SDC on the host (removes the MDM IPs from SDC, restarts scini service).
     * @param host the host
     * @param dataStore the datastore
     * @return true if SDC is successfully unprepared-ed on the host
     */
    boolean unprepareSDC(Host host, DataStore dataStore);

    /**
     * Checks if the SDC can be unprepared on the host (don't remove MDM IPs from SDC if any volumes mapped to SDC).
     * @param host the host
     * @param dataStore the datastore
     * @return true if SDC can be unprepared on the host
     */
    boolean canUnprepareSDC(Host host, DataStore dataStore);

    /**
     * Returns the SDC Id of the host for the pool.
     * @param sdcGuid the SDC GUID
     * @param dataStore the datastore
     * @return SDC Id of the host for the pool
     */
    String getHostSdcId(String sdcGuid, DataStore dataStore);

    /**
     * Returns the connection status of host SDC of the pool.
     * @param sdcId the SDC id
     * @param dataStore the datastore
     * @return true if Host SDC is connected to the pool
     */
    boolean isHostSdcConnected(String sdcId, DataStore dataStore, int waitTimeInSecs);

    /**
     * Returns the comma-separated list of MDM IPs of the pool.
     * @param poolId the pool id
     * @return Comma-separated list of MDM IPs of the pool
     */
    String getMdms(long poolId);
}
