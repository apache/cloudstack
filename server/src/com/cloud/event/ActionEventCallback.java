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

import com.cloud.user.UserContext;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.AnnotationInterceptor;
import com.cloud.utils.component.ComponentLocator;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.log4j.Logger;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ActionEventCallback implements MethodInterceptor, AnnotationInterceptor<EventVO> {

    protected static EventBus _eventBus = null;
    protected static boolean _eventBusLoaded = false;
    private static final Logger s_logger = Logger.getLogger(ActionEventCallback.class);

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        EventVO event = interceptStart(method);
        boolean success = true;
        try {
            return methodProxy.invokeSuper(object, args);
        } catch (Exception e){
            success = false;
            interceptException(method, event);
            throw e;
        } finally {
            if(success){
                interceptComplete(method, event);
            }
        }
    }

    @Override
    public boolean needToIntercept(AnnotatedElement element) {
        if (!(element instanceof Method)) {
            return false;
            
        }
        Method method = (Method)element;
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        if (actionEvent != null) {
            return true;
        }
        
        return false;
    }

    @Override
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
                EventUtils.saveStartedActionEvent(userId, accountId, actionEvent.eventType(), eventDescription, startEventId);
                publishOnEventBus(userId, accountId, actionEvent.eventType(), com.cloud.event.Event.State.Started, eventDescription);
            }
        }
        return event;
    }

    @Override
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
                startEventId = EventUtils.saveCreatedActionEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully created entity for "+eventDescription);
                publishOnEventBus(userId, accountId, actionEvent.eventType(), com.cloud.event.Event.State.Created, "Successfully created entity for " + eventDescription);
                ctx.setStartEventId(startEventId);
            } else {
                EventUtils.saveActionEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully completed "+eventDescription, startEventId);
                publishOnEventBus(userId, accountId, actionEvent.eventType(), com.cloud.event.Event.State.Completed, "Successfully completed " + eventDescription + startEventId);
            }
        }
    }

    @Override
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
                long eventId = EventUtils.saveCreatedActionEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while creating entity for " + eventDescription);
                ctx.setStartEventId(eventId);
            } else {
                EventUtils.saveActionEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while " + eventDescription, startEventId);
            }
        }
    }

    @Override
    public Callback getCallback() {
        return this;
    }

    void publishOnEventBus(long userId, long accountId, String eventType, com.cloud.event.Event.State state, String description) {
        if (getEventBus() != null) {
            Map<String, String> eventDescription = new HashMap<String, String>();
            eventDescription.put("user", String.valueOf(userId));
            eventDescription.put("account", String.valueOf(accountId));
            eventDescription.put("state", state.toString());
            eventDescription.put("description", description);

            int index = eventType.lastIndexOf("\\.");

            String resourceType = null;
            if (index != -1 ) {
                resourceType = eventType.substring(0, index);
            }

            Event event = new Event(null, EventCategory.ACTION_EVENT.getName(), eventType,
                    resourceType, null);
            event.setDescription(eventDescription);

            try {
                _eventBus.publish(event);
            } catch (EventBusException e) {
                s_logger.warn("Failed to publish action event on the the event bus.");
            }
        }
    }

    private EventBus getEventBus() {
        if (_eventBus == null) {
            if (!_eventBusLoaded) {
                ComponentLocator locator = ComponentLocator.getLocator("management-server");
                Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
                if (eventBusImpls != null) {
                    Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                   _eventBus = eventBusenum.nextElement();
                }
                _eventBusLoaded = true;
            }
        }
        return _eventBus;
    }
}
