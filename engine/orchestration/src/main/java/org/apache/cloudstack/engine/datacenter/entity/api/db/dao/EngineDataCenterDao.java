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

import java.util.List;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface EngineDataCenterDao extends GenericDao<EngineDataCenterVO, Long>,
        StateDao<DataCenterResourceEntity.State, DataCenterResourceEntity.State.Event, DataCenterResourceEntity> {
    EngineDataCenterVO findByName(String name);

    List<EngineDataCenterVO> findZonesByDomainId(Long domainId);

    List<EngineDataCenterVO> listPublicZones(String keyword);

    List<EngineDataCenterVO> findChildZones(Object[] ids, String keyword);

    void loadDetails(EngineDataCenterVO zone);

    void saveDetails(EngineDataCenterVO zone);

    List<EngineDataCenterVO> listDisabledZones();

    List<EngineDataCenterVO> listEnabledZones();

    EngineDataCenterVO findByToken(String zoneToken);

    EngineDataCenterVO findByTokenOrIdOrName(String tokenIdOrName);

    List<EngineDataCenterVO> findZonesByDomainId(Long domainId, String keyword);

    List<EngineDataCenterVO> findByKeyword(String keyword);

}
