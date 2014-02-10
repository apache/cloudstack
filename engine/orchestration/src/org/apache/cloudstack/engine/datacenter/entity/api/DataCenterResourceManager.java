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
package org.apache.cloudstack.engine.datacenter.entity.api;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;

import com.cloud.utils.fsm.NoTransitionException;

public interface DataCenterResourceManager {

    EngineDataCenterVO loadDataCenter(String dataCenterId);

    void saveDataCenter(EngineDataCenterVO dc);

    void savePod(EngineHostPodVO dc);

    void saveCluster(EngineClusterVO cluster);

    boolean changeState(DataCenterResourceEntity entity, Event event) throws NoTransitionException;

    EngineHostPodVO loadPod(String uuid);

    EngineClusterVO loadCluster(String uuid);

    EngineHostVO loadHost(String uuid);

    void saveHost(EngineHostVO hostVO);

}
