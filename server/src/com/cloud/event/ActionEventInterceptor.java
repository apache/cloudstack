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
package com.cloud.event;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import org.apache.cloudstack.context.CallContext;

import com.cloud.utils.component.ComponentMethodInterceptor;

public class ActionEventInterceptor implements ComponentMethodInterceptor {
	private static final Logger s_logger = Logger.getLogger(ActionEventInterceptor.class);

	public ActionEventInterceptor() {
	}

	@Override
    public Object interceptStart(Method method, Object target) {
        EventVO event = null;
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            boolean async = actionEvent.async();
            if(async){
                CallContext ctx = CallContext.current();
                long userId = ctx.getCallingUserId();
                long accountId = ctx.getCallingAccountId();
                long startEventId = ctx.getStartEventId();
                String eventDescription = actionEvent.eventDescription();
                if(ctx.getEventDetails() != null){
                    eventDescription += ". "+ctx.getEventDetails();
                }
                ActionEventUtils.onStartedActionEvent(userId, accountId, actionEvent.eventType(), eventDescription, startEventId);
            }
        }
        return event;
    }

	@Override
    public void interceptComplete(Method method, Object target, Object event) {
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            CallContext ctx = CallContext.current();
            long userId = ctx.getCallingUserId();
            long accountId = ctx.getCallingAccountId();
            long startEventId = ctx.getStartEventId();
            String eventDescription = actionEvent.eventDescription();
            if(ctx.getEventDetails() != null){
                eventDescription += ". "+ctx.getEventDetails();
            }            
            if(actionEvent.create()){
                //This start event has to be used for subsequent events of this action
                startEventId = ActionEventUtils.onCreatedActionEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully created entity for "+eventDescription);
                ctx.setStartEventId(startEventId);
            } else {
                ActionEventUtils.onCompletedActionEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully completed "+eventDescription, startEventId);
            }
        }
    }

	@Override
    public void interceptException(Method method, Object target, Object event) {
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            CallContext ctx = CallContext.current();
            long userId = ctx.getCallingUserId();
            long accountId = ctx.getCallingAccountId();
            long startEventId = ctx.getStartEventId();
            String eventDescription = actionEvent.eventDescription();
            if(ctx.getEventDetails() != null){
                eventDescription += ". "+ctx.getEventDetails();
            }
            if(actionEvent.create()){
                long eventId = ActionEventUtils.onCreatedActionEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while creating entity for "+eventDescription);
                ctx.setStartEventId(eventId);
            } else {
                ActionEventUtils.onCompletedActionEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while "+eventDescription, startEventId);
            }
        }
    }
	
	@Override
	public boolean needToIntercept(Method method) {
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            return true;
        }
        
        return false;
    }
}
