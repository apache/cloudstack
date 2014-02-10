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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;

import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.resource.ResourceState;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

/**
 * Data Access Object for server
 *
 */
public interface EngineHostDao extends GenericDao<EngineHostVO, Long>,
        StateDao<DataCenterResourceEntity.State, DataCenterResourceEntity.State.Event, DataCenterResourceEntity> {
    long countBy(long clusterId, ResourceState... states);

    /**
     * Mark all hosts associated with a certain management server
     * as disconnected.
     *
     * @param msId management server id.
     */
    void markHostsAsDisconnected(long msId, long lastPing);

    List<EngineHostVO> findLostHosts(long timeout);

    List<EngineHostVO> findAndUpdateDirectAgentToLoad(long lastPingSecondsAfter, Long limit, long managementServerId);

    List<RunningHostCountInfo> getRunningHostCounts(Date cutTime);

    long getNextSequence(long hostId);

    void loadDetails(EngineHostVO host);

    void saveDetails(EngineHostVO host);

    void loadHostTags(EngineHostVO host);

    List<EngineHostVO> listByHostTag(Host.Type type, Long clusterId, Long podId, long dcId, String hostTag);

    long countRoutingHostsByDataCenter(long dcId);

    List<EngineHostVO> findAndUpdateApplianceToLoad(long lastPingSecondsAfter, long managementServerId);

    boolean updateResourceState(ResourceState oldState, ResourceState.Event event, ResourceState newState, Host vo);

    EngineHostVO findByGuid(String guid);

    EngineHostVO findByTypeNameAndZoneId(long zoneId, String name, Host.Type type);

    List<EngineHostVO> findHypervisorHostInCluster(long clusterId);

    /**
     * @param type
     * @param clusterId
     * @param podId
     * @param dcId
     * @param haTag TODO
     * @return
     */
    List<EngineHostVO> listAllUpAndEnabledNonHAHosts(Type type, Long clusterId, Long podId, long dcId, String haTag);
}
