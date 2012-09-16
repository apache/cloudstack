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

package org.apache.cloudstack.framework.events;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.AnnotationInterceptor;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class EventPublisher implements MethodInterceptor, AnnotationInterceptor<Publish> {

    private static EventBus _eventBus = null;

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Publish event = interceptStart(method);
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
        Publish event = method.getAnnotation(Publish.class);
        if (event != null) {
            return true;
        }
        return false;
    }

    @Override
    public Publish interceptStart(AnnotatedElement element) {
        return null;
    }

    @Override
    public void interceptComplete(AnnotatedElement element, Publish event) {
        _eventBus = getEventBus();
        if (_eventBus != null) {
            Map<String, String> description = new HashMap<String, String>();
            description.put("description", event.eventDescription());
            _eventBus.publish(event.eventCategory(), event.eventType(), description);
        }
    }

    @Override
    public void interceptException(AnnotatedElement element, Publish attach) {
        return;
    }

    @Override
    public Callback getCallback() {
        return this;
    }
    
    private EventBus getEventBus() {
        if (_eventBus == null) {
            ComponentLocator locator = ComponentLocator.getLocator("management-server");
            Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
            if (eventBusImpls != null) {
                Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                _eventBus = eventBusenum.nextElement();
            }
        }
        return _eventBus;
    }
}