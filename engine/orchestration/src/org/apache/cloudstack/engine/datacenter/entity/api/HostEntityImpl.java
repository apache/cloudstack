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

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.fsm.NoTransitionException;

public class HostEntityImpl implements HostEntity {

	private DataCenterResourceManager manager;

    private EngineHostVO hostVO;

    public HostEntityImpl(String uuid, DataCenterResourceManager manager) {
	this.manager = manager;
	hostVO = manager.loadHost(uuid);
    }

	@Override
	public boolean enable() {
	try {
			manager.changeState(this, Event.EnableRequest);
		} catch (NoTransitionException e) {
			return false;
		}
	return true;
	}

	@Override
	public boolean disable() {
	try {
			manager.changeState(this, Event.DisableRequest);
		} catch (NoTransitionException e) {
			return false;
		}
	return true;
	}

	@Override
	public boolean deactivate() {
	try {
			manager.changeState(this, Event.DeactivateRequest);
		} catch (NoTransitionException e) {
			return false;
		}
	return true;
	}

	@Override
	public boolean reactivate() {
	try {
			manager.changeState(this, Event.ActivatedRequest);
		} catch (NoTransitionException e) {
			return false;
		}
	return true;
	}

	@Override
	public State getState() {
		return hostVO.getOrchestrationState();
	}

	@Override
	public void persist() {
		manager.saveHost(hostVO);
	}

	@Override
	public String getName() {
		return hostVO.getName();
	}

	@Override
	public String getUuid() {
		return hostVO.getUuid();
	}

	@Override
	public long getId() {
		return hostVO.getId();
	}

	@Override
	public String getCurrentState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDesiredState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreatedTime() {
		return hostVO.getCreated();
	}

	@Override
	public Date getLastUpdatedTime() {
		return hostVO.getLastUpdated();
	}

	@Override
	public String getOwner() {
		// TODO Auto-generated method stub
		return hostVO.getOwner();
	}


    public void setDetails(Map<String,String> details) {
	hostVO.setDetails(details);
    }

	@Override
	public Map<String, String> getDetails() {
		return hostVO.getDetails();
	}

	@Override
	public void addDetail(String name, String value) {
		hostVO.setDetail(name, value);
	}

	@Override
	public void delDetail(String name, String value) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateDetail(String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Method> getApplicableActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTotalMemory() {
		return hostVO.getTotalMemory();
	}

	@Override
	public Integer getCpus() {
		return hostVO.getCpus();
	}

	@Override
	public Long getSpeed() {
		return hostVO.getSpeed();
	}

	@Override
	public Long getPodId() {
		return hostVO.getPodId();
	}

	@Override
	public long getDataCenterId() {
		return hostVO.getDataCenterId();
	}

	@Override
	public HypervisorType getHypervisorType() {
		return hostVO.getHypervisorType();
	}

	@Override
	public String getGuid() {
		return hostVO.getGuid();
	}

	@Override
	public Long getClusterId() {
		return hostVO.getClusterId();
	}

	public void setOwner(String owner) {
		hostVO.setOwner(owner);
	}

	public void setName(String name) {
		hostVO.setName(name);
	}


}
