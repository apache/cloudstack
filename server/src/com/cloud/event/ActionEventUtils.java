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

import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ActionEventUtils {

    private static final Logger s_logger = Logger.getLogger(ActionEventUtils.class);

    private static EventDao _eventDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(EventDao.class);
	private static AccountDao _accountDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(AccountDao.class);
    protected static UserDao _userDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(UserDao.class);;
    protected static EventBus _eventBus = null;
    protected static boolean _eventBusProviderLoaded = false;

    public static Long onActionEvent(Long userId, Long accountId, Long domainId, String type, String description) {

        publishActionEvent(userId, accountId, EventCategory.ACTION_EVENT.getName(),
                type, com.cloud.event.Event.State.Scheduled);

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setDomainId(domainId);
        event.setType(type);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after scheduling an async job
     */
    public static Long onScheduledActionEvent(Long userId, Long accountId, String type, String description, long startEventId) {

        publishActionEvent(userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Scheduled);

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setDomainId(getDomainId(accountId));
        event.setType(type);
        event.setStartId(startEventId);
        event.setState(Event.State.Scheduled);
        event.setDescription("Scheduled async job for "+description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after starting execution of an async job
     */
    public static Long onStartedActionEvent(Long userId, Long accountId, String type, String description, long startEventId) {

        publishActionEvent(userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Started);

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setDomainId(getDomainId(accountId));
        event.setType(type);
        event.setState(Event.State.Started);
        event.setDescription("Starting job for "+description);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
    	return event.getId();
    }    

    public static Long onCompletedActionEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {

        publishActionEvent(userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Completed);

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setDomainId(getDomainId(accountId));
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        return (event != null ? event.getId() : null);
    }
    
    public static Long onCreatedActionEvent(Long userId, Long accountId, String level, String type, String description) {

        publishActionEvent(userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Created);

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setDomainId(getDomainId(accountId));
        event.setType(type);
        event.setLevel(level);
        event.setState(Event.State.Created);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }

    private static long getDomainId(long accountId){
    	AccountVO account = _accountDao.findByIdIncludingRemoved(accountId);
    	return account.getDomainId();
    }

    public static void publishActionEvent(long userId, long accountId, String eventCategory,
                                          String eventType, Event.State state) {

        if (getEventBusProvider() == null) {
            return; // no provider is configured to provider events bus, so just return
        }

        Map<String, String> eventDescription = new HashMap<String, String>();
        Account account = _accountDao.findById(accountId);
        User user = _userDao.findById(userId);

        eventDescription.put("user", user.getUuid());
        eventDescription.put("account", account.getUuid());
        eventDescription.put("event", eventType);
        eventDescription.put("status", state.toString());

        int index = eventType.lastIndexOf(".");

        String resourceType = null;
        if (index != -1 ) {
            resourceType = eventType.substring(0, index);
        }

        org.apache.cloudstack.framework.events.Event event = new org.apache.cloudstack.framework.events.Event(
                ManagementServer.Name,
                eventCategory,
                eventType,
                resourceType, null);
        event.setDescription(eventDescription);

        try {
            _eventBus.publish(event);
        } catch (EventBusException e) {
            s_logger.warn("Failed to publish action event on the the event bus.");
        }
    }

    private static EventBus getEventBusProvider() {
        if (!_eventBusProviderLoaded) {
            ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
            Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
            if (eventBusImpls != null) {
                Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                if (eventBusenum != null && eventBusenum.hasMoreElements()) {
                    _eventBus = eventBusenum.nextElement();
                }
            }
            _eventBusProviderLoaded = true;
        }
        return _eventBus;
    }
}
