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
package org.apache.cloudstack.engine.datacenter.entity.api;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;

import com.cloud.org.Cluster;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.utils.fsm.NoTransitionException;

public class PodEntityImpl implements PodEntity {


	private DataCenterResourceManager manager;

    private EngineHostPodVO podVO;

    public PodEntityImpl(String uuid, DataCenterResourceManager manager) {
	this.manager = manager;
	podVO = manager.loadPod(uuid);
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
        return podVO.getState();
    }

    @Override
    public String getUuid() {
        return podVO.getUuid();
    }

    @Override
    public long getId() {
        return podVO.getId();
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
        return podVO.getCreated();
    }

    @Override
    public Date getLastUpdatedTime() {
        return podVO.getLastUpdated();
    }

    @Override
    public String getOwner() {
        return podVO.getOwner();
    }


    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCidrAddress() {
        return podVO.getCidrAddress();
    }

    @Override
    public int getCidrSize() {
        return podVO.getCidrSize();
    }

    @Override
    public String getGateway() {
        return podVO.getGateway();
    }

    @Override
    public long getDataCenterId() {
        return podVO.getDataCenterId();
    }

    @Override
    public String getName() {
        return podVO.getName();
    }

    @Override
    public AllocationState getAllocationState() {
        return podVO.getAllocationState();
    }

    @Override
    public boolean getExternalDhcp() {
        return podVO.getExternalDhcp();
    }

    @Override
    public List<Cluster> listClusters() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public void persist() {
		manager.savePod(podVO);

	}

	@Override
	public Map<String, String> getDetails() {
		return null;
	}

	@Override
	public void addDetail(String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delDetail(String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDetail(String name, String value) {

	}

	public void setOwner(String owner) {
		podVO.setOwner(owner);
	}

	public void setName(String name) {
		podVO.setName(name);
	}

}
