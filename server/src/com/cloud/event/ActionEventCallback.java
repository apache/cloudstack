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

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.utils.component.AnnotationInterceptor;

public class ActionEventCallback implements MethodInterceptor, AnnotationInterceptor<EventVO> {
    boolean create = false;
    private String eventType = null;
    private long accountId = Account.ACCOUNT_ID_SYSTEM;
    private long userId = User.UID_SYSTEM;
    private String description = null;
    private long startEventId = 0;

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        ActionEvent actionEvent = method.getAnnotation(ActionEvent.class);
        EventVO event = null;
        if (actionEvent != null) {
            create = actionEvent.create();
            UserContext ctx = UserContext.current();
            Long userID = ctx.getCallerUserId();
            userId = (userID == null) ? User.UID_SYSTEM : userID;
            eventType = actionEvent.eventType();
            description = actionEvent.eventDescription();
            startEventId = ctx.getStartEventId();

            if(!create){
                event = interceptStart(method);
            }
        }
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
            create = actionEvent.create();
            return true;
        }
        
        Class<?> clazz = method.getDeclaringClass();
        do {
            actionEvent = clazz.getAnnotation(ActionEvent.class);
            if (actionEvent != null) {
                return true;
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        
        return false;
    }

    @Override
    public EventVO interceptStart(AnnotatedElement element) {
        EventVO event = null;
        if(eventType != null){            
            long eventId = EventUtils.saveStartedEvent(userId, accountId, eventType, description, startEventId);
            if(startEventId == 0){
                // There was no scheduled event. so Started event Id
                startEventId = eventId;
            }
        }
        return event;
    }

    @Override
    public void interceptComplete(AnnotatedElement element, EventVO event) {
        if(eventType != null){
            if(create){
                //This start event has to be used for subsequent events of this action
                startEventId = EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_INFO, eventType, "Successfully created entity for "+description);
                UserContext ctx = UserContext.current();
                ctx.setStartEventId(startEventId);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, eventType, "Successfully completed "+description, startEventId);
            }
        }
    }

    @Override
    public void interceptException(AnnotatedElement element, EventVO event) {
        if(eventType != null){
            if(create){
                EventUtils.saveCreatedEvent(userId, accountId, EventVO.LEVEL_ERROR, eventType, "Error while creating entity for "+description);
            } else {
                EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, eventType, "Error while "+description, startEventId);
            }
        }
    }

    @Override
    public Callback getCallback() {
        return this;
    }
    
}
