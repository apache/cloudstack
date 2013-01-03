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

import javax.ws.rs.GET;


import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import org.apache.cloudstack.engine.entity.api.CloudStackEntity;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

/**
 * This interface specifies the states and operations all physical 
 * and virtual resources in the data center must implement. 
 */
@Produces({"application/json", "application/xml"})
public interface DataCenterResourceEntity extends CloudStackEntity, StateObject<DataCenterResourceEntity.State> {

    /**
     * This is the state machine for how CloudStack should interact with 
     *
     */
    public enum State {
        Disabled("The resource is disabled so CloudStack should not use it.  This is the initial state of all resources added to CloudStack."),
        Enabled("The resource is now enabled for CloudStack to use."),
        Deactivated("The resource is deactivated so CloudStack should not use it for new resource needs.");

        String _description;

        private State(String description) {
            _description = description;
        }

        public enum Event {
            EnableRequest,
            DisableRequest,
            DeactivateRequest,
            ActivatedRequest
        }

        protected static final StateMachine2<State, Event, DataCenterResourceEntity> s_fsm = new StateMachine2<State, Event, DataCenterResourceEntity>();
        static {
            s_fsm.addTransition(Disabled, Event.EnableRequest, Enabled);
            s_fsm.addTransition(Enabled, Event.DisableRequest, Disabled);
            s_fsm.addTransition(Enabled, Event.DeactivateRequest, Deactivated);
            s_fsm.addTransition(Deactivated, Event.ActivatedRequest, Enabled);
        }

    }

    /**
     * Prepare the resource to take new on new demands.
     */
    @POST 
    boolean enable();

    /**
     * Disables the resource.  Cleanup.  Prepare for the resource to be removed.
     */
    @POST
    boolean disable();

    /**
     * Do not use the resource for new demands.
     */
    @POST
    boolean deactivate();

    /**
     * Reactivates a deactivated resource.
     */
    @POST
    boolean reactivate();


    @Override
    @GET
    State getState();

    
    public void persist();
    
    String getName();
}
