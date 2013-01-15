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
import org.apache.cloudstack.engine.datacenter.entity.api.db.ClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.ClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostPodDao;
import org.springframework.stereotype.Component;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class DataCenterResourceManagerImpl implements DataCenterResourceManager {

	@Inject
    DataCenterDao _dataCenterDao;

	@Inject
	HostPodDao _podDao;

	@Inject
	ClusterDao _clusterDao;
	
	@Inject
	HostDao _hostDao;
	
	
    protected StateMachine2<State, Event, DataCenterResourceEntity> _stateMachine = DataCenterResourceEntity.State.s_fsm;

	@Override
	public DataCenterVO loadDataCenter(String dataCenterId) {
    	DataCenterVO dataCenterVO = _dataCenterDao.findByUUID(dataCenterId);
    	if(dataCenterVO == null){
    		throw new InvalidParameterValueException("Zone does not exist");
    	}
    	return dataCenterVO;
	}

	@Override
	public void saveDataCenter(DataCenterVO dc) {
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
	public HostPodVO loadPod(String uuid) {
		HostPodVO pod = _podDao.findByUUID(uuid);
    	if(pod == null){
    		throw new InvalidParameterValueException("Pod does not exist");
    	}
		return pod;
	}

	@Override
	public ClusterVO loadCluster(String uuid) {
		ClusterVO cluster = _clusterDao.findByUUID(uuid);
    	if(cluster == null){
    		throw new InvalidParameterValueException("Pod does not exist");
    	}
		return cluster;
	}

	@Override
	public void savePod(HostPodVO pod) {
		_podDao.persist(pod);
	}

	@Override
	public void saveCluster(ClusterVO cluster) {
		_clusterDao.persist(cluster);		
	}

	@Override
	public HostVO loadHost(String uuid) {
		HostVO host = _hostDao.findByUUID(uuid);
    	if(host == null){
    		throw new InvalidParameterValueException("Host does not exist");
    	}
		return host;
	}

	@Override
	public void saveHost(HostVO hostVO) {
		_hostDao.persist(hostVO);		
	}

}
