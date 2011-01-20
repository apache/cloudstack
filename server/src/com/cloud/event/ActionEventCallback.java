/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.event;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.user.UserContext;
import com.cloud.utils.component.AnnotationInterceptor;

public class ActionEventCallback implements MethodInterceptor, AnnotationInterceptor<EventVO> {

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        EventVO event = interceptStart(method);;
        try {
            return methodProxy.invokeSuper(object, args);
        } finally {
            interceptComplete(method, event);
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
                EventUtils.saveStartedEvent(userId, accountId, actionEvent.eventType(), actionEvent.eventDescription(), startEventId);
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
            if(actionEvent.create()){
                //This start event has to be used for subsequent events of this action
                startEventId = EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully created entity for "+actionEvent.eventDescription());
                ctx.setStartEventId(startEventId);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, actionEvent.eventType(), "Successfully completed "+actionEvent.eventDescription(), startEventId);
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
            if(actionEvent.create()){
                long eventId = EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while creating entity for "+actionEvent.eventDescription());
                ctx.setStartEventId(eventId);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, actionEvent.eventType(), "Error while "+actionEvent.eventDescription(), startEventId);
            }
        }
    }

    @Override
    public Callback getCallback() {
        return this;
    }
    
}
