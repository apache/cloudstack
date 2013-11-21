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

package org.apache.cloudstack.network.contrail.management;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageBusBase;

import com.cloud.event.ActionEvent;
import com.cloud.event.Event;
import com.cloud.event.EventCategory;
import com.cloud.event.EventTypes;
import com.cloud.server.ManagementServer;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentMethodInterceptor;

@Component
public class EventUtils {
    private static final Logger s_logger = Logger.getLogger(EventUtils.class);

    protected static MessageBus _messageBus = null;

    public EventUtils() {
    }

    private static void publishOnMessageBus(String eventCategory, String eventType, String details, Event.State state) {

        if (state != com.cloud.event.Event.State.Completed) {
            return;
        }

        try {
            _messageBus = ComponentContext.getComponent(MessageBusBase.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        org.apache.cloudstack.framework.events.Event event =
            new org.apache.cloudstack.framework.events.Event(ManagementService.Name, eventCategory, eventType, EventTypes.getEntityForEvent(eventType), null);

        Map<String, String> eventDescription = new HashMap<String, String>();
        eventDescription.put("event", eventType);
        eventDescription.put("status", state.toString());
        eventDescription.put("details", details);
        event.setDescription(eventDescription);
        try {
            _messageBus.publish(EventTypes.getEntityForEvent(eventType), eventType, null, event);
        } catch (Exception e) {
            s_logger.warn("Failed to publish action event on the the event bus.");
        }

    }

    public static class EventInterceptor implements ComponentMethodInterceptor {

        private static final Logger s_logger = Logger.getLogger(EventInterceptor.class);

        public EventInterceptor() {

        }

        @Override
        public Object interceptStart(Method method, Object target) {
            return null;
        }

        @Override
        public void interceptComplete(Method method, Object target, Object event) {
            ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
            if (actionEvent != null) {
                CallContext ctx = CallContext.current();
                if (!actionEvent.create()) {
                    publishOnMessageBus(EventCategory.ACTION_EVENT.getName(), actionEvent.eventType(), ctx.getEventDetails(), com.cloud.event.Event.State.Completed);
                }
            }
        }

        @Override
        public void interceptException(Method method, Object target, Object event) {
            s_logger.debug("interceptException");
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
}
