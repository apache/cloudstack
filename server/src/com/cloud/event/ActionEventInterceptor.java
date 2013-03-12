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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.cloud.user.UserContext;

public class ActionEventInterceptor {

	public ActionEventInterceptor() {
	}

	public Object AroundAnyMethod(ProceedingJoinPoint call) throws Throwable {
		MethodSignature methodSignature = (MethodSignature)call.getSignature();
        Method targetMethod = methodSignature.getMethod();	
        if(needToIntercept(targetMethod)) {
            EventVO event = interceptStart(targetMethod);
        	
            boolean success = true;
			Object ret = null;
			try {
				 ret = call.proceed();
			} catch (Throwable e) {
	            success = false;
	            interceptException(targetMethod, event);
	            throw e;
			} finally {
	            if(success){
	                interceptComplete(targetMethod, event);
	            }
			}
			return ret;
        }
        return call.proceed();
	}

    public EventVO interceptStart(AnnotatedElement element) {
        EventVO event = null;
        Method method = (Method)element;
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            boolean async = actionEvent.async();
            if(async){
                UserContext ctx = UserContext.current();
                long userId = ctx.getCallerUserId();
                long accountId = ctx.getAccountId();
                long startEventId = ctx.getStartEventId();
                String eventDescription = actionEvent.eventDescription();
                if(ctx.getEventDetails() != null){
                    eventDescription += ". "+ctx.getEventDetails();
                }
                EventUtils.saveStartedEvent(userId, accountId, actionEvent.eventType(), eventDescription, startEventId);
            }
        }
        return event;
    }

    public void interceptComplete(AnnotatedElement element, EventVO event) {
        Method method = (Method)element;
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            UserContext ctx = UserContext.current();
            long userId = ctx.getCallerUserId();
            long accountId = ctx.getAccountId();
            long startEventId = ctx.getStartEventId();
            String eventDescription = actionEvent.eventDescription();
            if(ctx.getEventDetails() != null){
                eventDescription += ". "+ctx.getEventDetails();
            }            
            if(actionEvent.create()){
                //This start event has to be used for subsequent events of this action
                startEventId = EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully created entity for "+eventDescription);
                ctx.setStartEventId(startEventId);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully completed "+eventDescription, startEventId);
            }
        }
    }

    public void interceptException(AnnotatedElement element, EventVO event) {
        Method method = (Method)element;
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            UserContext ctx = UserContext.current();
            long userId = ctx.getCallerUserId();
            long accountId = ctx.getAccountId();
            long startEventId = ctx.getStartEventId();
            String eventDescription = actionEvent.eventDescription();
            if(ctx.getEventDetails() != null){
                eventDescription += ". "+ctx.getEventDetails();
            }
            if(actionEvent.create()){
                long eventId = EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while creating entity for "+eventDescription);
                ctx.setStartEventId(eventId);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while "+eventDescription, startEventId);
            }
        }
    }
	
	private boolean needToIntercept(Method method) {
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            return true;
        }
        
        return false;
    }
}
