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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEvents;
import com.cloud.event.Event;
import com.cloud.event.EventCategory;
import com.cloud.event.EventTypes;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentMethodInterceptor;

@Component
public class EventUtils {
    protected static Logger LOGGER = LogManager.getLogger(EventUtils.class);

    protected static  EventBus s_eventBus = null;

    public EventUtils() {
    }

    private static void publishOnMessageBus(String eventCategory, String eventType, String details, Event.State state) {

        if (state != com.cloud.event.Event.State.Completed) {
            return;
        }

        try {
            s_eventBus = ComponentContext.getComponent(EventBus.class);
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
            s_eventBus.publish(event);
        } catch (EventBusException evx) {
            String errMsg = "Failed to publish contrail event.";
            LOGGER.warn(errMsg, evx);
        }

    }

    public static class EventInterceptor implements ComponentMethodInterceptor, MethodInterceptor {

    protected Logger LOGGER = LogManager.getLogger(getClass());

        public EventInterceptor() {

        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method m = invocation.getMethod();
            Object target = invocation.getThis();

            if ( getActionEvents(m).size() == 0 ) {
                /* Look for annotation on impl class */
                m = target.getClass().getMethod(m.getName(), m.getParameterTypes());
            }

            Object interceptorData = null;

            boolean success = true;
            try {
                interceptorData = interceptStart(m, target);

                Object result = invocation.proceed();
                success = true;

                return result;
            } finally {
                if ( success ) {
                    interceptComplete(m, target, interceptorData);
                } else {
                    interceptException(m, target, interceptorData);
                }
            }
        }

        protected List<ActionEvent> getActionEvents(Method m) {
            List<ActionEvent> result = new ArrayList<ActionEvent>();

            ActionEvents events = m.getAnnotation(ActionEvents.class);

            if ( events != null ) {
                for ( ActionEvent e : events.value() ) {
                    result.add(e);
                }
            }

            ActionEvent e = m.getAnnotation(ActionEvent.class);

            if ( e != null ) {
                result.add(e);
            }

            return result;
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
            LOGGER.debug("interceptException");
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
