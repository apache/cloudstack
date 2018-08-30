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
package org.apache.cloudstack.engine.datacenter.entity.api.db.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface EngineHostPodDao extends GenericDao<EngineHostPodVO, Long>,
        StateDao<DataCenterResourceEntity.State, DataCenterResourceEntity.State.Event, DataCenterResourceEntity> {
    public List<EngineHostPodVO> listByDataCenterId(long id);

    public EngineHostPodVO findByName(String name, long dcId);

    public HashMap<Long, List<Object>> getCurrentPodCidrSubnets(long zoneId, long podIdToSkip);

    public List<Long> listDisabledPods(long zoneId);
}
