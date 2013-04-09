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

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineDataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostPodDao;
import org.springframework.stereotype.Component;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class DataCenterResourceManagerImpl implements DataCenterResourceManager {

	@Inject
    EngineDataCenterDao _dataCenterDao;

	@Inject
	EngineHostPodDao _podDao;

	@Inject
	EngineClusterDao _clusterDao;

	@Inject
	EngineHostDao _hostDao;


    protected StateMachine2<State, Event, DataCenterResourceEntity> _stateMachine = DataCenterResourceEntity.State.s_fsm;

	@Override
	public EngineDataCenterVO loadDataCenter(String dataCenterId) {
	EngineDataCenterVO dataCenterVO = _dataCenterDao.findByUuid(dataCenterId);
	if(dataCenterVO == null){
		throw new InvalidParameterValueException("Zone does not exist");
	}
	return dataCenterVO;
	}

	@Override
	public void saveDataCenter(EngineDataCenterVO dc) {
		_dataCenterDao.persist(dc);

	}

	@Override
	public boolean changeState(DataCenterResourceEntity entity, Event event) throws NoTransitionException {

		if(entity instanceof ZoneEntity){
			return _stateMachine.transitTo(entity, event, null, _dataCenterDao);
		}else if(entity instanceof PodEntity){
			return _stateMachine.transitTo(entity, event, null, _podDao);
		}else if(entity instanceof ClusterEntity){
			return _stateMachine.transitTo(entity, event, null, _clusterDao);
		}else if(entity instanceof HostEntity){
			return _stateMachine.transitTo(entity, event, null, _hostDao);
		}

		return false;
	}

	@Override
	public EngineHostPodVO loadPod(String uuid) {
		EngineHostPodVO pod = _podDao.findByUuid(uuid);
	if(pod == null){
		throw new InvalidParameterValueException("Pod does not exist");
	}
		return pod;
	}

	@Override
	public EngineClusterVO loadCluster(String uuid) {
		EngineClusterVO cluster = _clusterDao.findByUuid(uuid);
	if(cluster == null){
		throw new InvalidParameterValueException("Pod does not exist");
	}
		return cluster;
	}

	@Override
	public void savePod(EngineHostPodVO pod) {
		_podDao.persist(pod);
	}

	@Override
	public void saveCluster(EngineClusterVO cluster) {
		_clusterDao.persist(cluster);
	}

	@Override
	public EngineHostVO loadHost(String uuid) {
		EngineHostVO host = _hostDao.findByUuid(uuid);
	if(host == null){
		throw new InvalidParameterValueException("Host does not exist");
	}
		return host;
	}

	@Override
	public void saveHost(EngineHostVO hostVO) {
		_hostDao.persist(hostVO);
	}

}
