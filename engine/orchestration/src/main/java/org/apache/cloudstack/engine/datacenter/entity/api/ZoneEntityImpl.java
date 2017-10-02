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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;

import com.cloud.utils.fsm.FiniteStateObject;
import com.cloud.utils.fsm.NoTransitionException;

@Path("/zone/{id}")
public class ZoneEntityImpl implements ZoneEntity, FiniteStateObject<DataCenterResourceEntity.State, DataCenterResourceEntity.State.Event> {

    private DataCenterResourceManager manager;

    private EngineDataCenterVO dataCenterVO;

    public ZoneEntityImpl(String dataCenterId, DataCenterResourceManager manager) {
        this.manager = manager;
        this.dataCenterVO = this.manager.loadDataCenter(dataCenterId);
    }

    @Override
    @GET
    public String getUuid() {
        return dataCenterVO.getUuid();
    }

    @Override
    public long getId() {
        return dataCenterVO.getId();
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
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return "state";
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return "desired_state";
    }

    @Override
    public Date getCreatedTime() {
        return dataCenterVO.getCreated();
    }

    @Override
    public Date getLastUpdatedTime() {
        return dataCenterVO.getLastUpdated();
    }

    @Override
    public String getOwner() {
        return dataCenterVO.getOwner();
    }

    public void setOwner(String owner) {
        dataCenterVO.setOwner(owner);
    }

    @Override
    public Map<String, String> getDetails() {
        return dataCenterVO.getDetails();
    }

    public void setDetails(Map<String, String> details) {
        dataCenterVO.setDetails(details);
    }

    @Override
    public void addDetail(String name, String value) {
        dataCenterVO.setDetail(name, value);
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
    public State getState() {
        return dataCenterVO.getState();
    }

    @Override
    public List<PodEntity> listPods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setState(State state) {
        //use FSM to set state.
    }

    @Override
    public void persist() {
        manager.saveDataCenter(dataCenterVO);
    }

    @Override
    public String getName() {
        return dataCenterVO.getName();
    }

    @Override
    public List<String> listPodIds() {
        List<String> podIds = new ArrayList<String>();
        podIds.add("pod-uuid-1");
        podIds.add("pod-uuid-2");
        return podIds;
    }

    public void setName(String name) {
        dataCenterVO.setName(name);
    }
}
