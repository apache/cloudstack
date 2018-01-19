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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.utils.ReflectUtil;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.Identity;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.configuration.Config;
import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.projects.Project;
import com.cloud.utils.component.ComponentContext;

public class ActionEventUtils {
    private static final Logger s_logger = Logger.getLogger(ActionEventUtils.class);

    private static EventDao s_eventDao;
    private static AccountDao s_accountDao;
    private static ProjectDao s_projectDao;
    protected static UserDao s_userDao;
    protected static EventBus s_eventBus = null;
    protected static EntityManager s_entityMgr;
    protected static ConfigurationDao s_configDao;

    public static final String EventDetails = "event_details";
    public static final String EventId = "event_id";
    public static final String EntityType = "entity_type";
    public static final String EntityUuid = "entity_uuid";
    public static final String EntityDetails = "entity_details";

    @Inject
    EventDao eventDao;
    @Inject
    AccountDao accountDao;
    @Inject
    UserDao userDao;
    @Inject
    ProjectDao projectDao;
    @Inject
    EntityManager entityMgr;
    @Inject
    ConfigurationDao configDao;

    public ActionEventUtils() {
    }

    @PostConstruct
    void init() {
        s_eventDao = eventDao;
        s_accountDao = accountDao;
        s_userDao = userDao;
        s_projectDao = projectDao;
        s_entityMgr = entityMgr;
        s_configDao = configDao;
    }

    public static Long onActionEvent(Long userId, Long accountId, Long domainId, String type, String description) {

        publishOnEventBus(userId, accountId, EventCategory.ACTION_EVENT.getName(), type, com.cloud.event.Event.State.Completed, description);

        Event event = persistActionEvent(userId, accountId, domainId, null, type, Event.State.Completed, true, description, null);

        return event.getId();
    }

    /*
     * Save event after scheduling an async job
     */
    public static Long onScheduledActionEvent(Long userId, Long accountId, String type, String description, boolean eventDisplayEnabled, long startEventId) {

        publishOnEventBus(userId, accountId, EventCategory.ACTION_EVENT.getName(), type, com.cloud.event.Event.State.Scheduled, description);

        Event event = persistActionEvent(userId, accountId, null, null, type, Event.State.Scheduled, eventDisplayEnabled, description, startEventId);

        return event.getId();
    }

    public static void startNestedActionEvent(String eventType, String eventDescription) {
        CallContext.setActionEventInfo(eventType, eventDescription);
        onStartedActionEventFromContext(eventType, eventDescription, true);
    }

    public static void onStartedActionEventFromContext(String eventType, String eventDescription, boolean eventDisplayEnabled) {
        CallContext ctx = CallContext.current();
        long userId = ctx.getCallingUserId();
        long accountId = ctx.getProject() != null ? ctx.getProject().getProjectAccountId() : ctx.getCallingAccountId();    //This should be the entity owner id rather than the Calling User Account Id.
        long startEventId = ctx.getStartEventId();

        if (!eventType.equals(""))
            ActionEventUtils.onStartedActionEvent(userId, accountId, eventType, eventDescription, eventDisplayEnabled, startEventId);
    }

    /*
     * Save event after starting execution of an async job
     */
    public static Long onStartedActionEvent(Long userId, Long accountId, String type, String description, boolean eventDisplayEnabled, long startEventId) {

        publishOnEventBus(userId, accountId, EventCategory.ACTION_EVENT.getName(), type, com.cloud.event.Event.State.Started, description);

        Event event = persistActionEvent(userId, accountId, null, null, type, Event.State.Started, eventDisplayEnabled, description, startEventId);

        return event.getId();
    }

    public static Long onCompletedActionEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {

        return onCompletedActionEvent(userId, accountId, level, type, true, description, startEventId);
    }

    public static Long onCompletedActionEvent(Long userId, Long accountId, String level, String type, boolean eventDisplayEnabled, String description, long startEventId) {
        publishOnEventBus(userId, accountId, EventCategory.ACTION_EVENT.getName(), type, com.cloud.event.Event.State.Completed, description);

        Event event = persistActionEvent(userId, accountId, null, level, type, Event.State.Completed, eventDisplayEnabled, description, startEventId);

        return event.getId();

    }

    public static Long onCreatedActionEvent(Long userId, Long accountId, String level, String type, boolean eventDisplayEnabled, String description) {

        publishOnEventBus(userId, accountId, EventCategory.ACTION_EVENT.getName(), type, com.cloud.event.Event.State.Created, description);

        Event event = persistActionEvent(userId, accountId, null, level, type, Event.State.Created, eventDisplayEnabled, description, null);

        return event.getId();
    }

    private static Event persistActionEvent(Long userId, Long accountId, Long domainId, String level, String type,
                                            Event.State state, boolean eventDisplayEnabled, String description, Long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(state);
        event.setDescription(description);
        event.setDisplay(eventDisplayEnabled);

        if (domainId != null) {
            event.setDomainId(domainId);
        } else {
            event.setDomainId(getDomainId(accountId));
        }
        if (level != null && !level.isEmpty()) {
            event.setLevel(level);
        }
        if (startEventId != null) {
            event.setStartId(startEventId);
        }
        event = s_eventDao.persist(event);
        return event;
    }

    private static void publishOnEventBus(long userId, long accountId, String eventCategory, String eventType, Event.State state, String description) {
        String configKey = Config.PublishActionEvent.key();
        String value = s_configDao.getValue(configKey);
        boolean configValue = Boolean.parseBoolean(value);
        if(!configValue)
            return;
        try {
            s_eventBus = ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        // get the entity details for which ActionEvent is generated
        String entityType = null;
        String entityUuid = null;
        CallContext context = CallContext.current();
        //Get entity Class(Example - VirtualMachine.class) from the event Type eg. - VM.CREATE
        Class<?> entityClass = EventTypes.getEntityClassForEvent(eventType);
        if (entityClass != null){
            //Get uuid from id
            Object param = context.getContextParameter(entityClass);
            if(param != null){
                try {
                    entityUuid = getEntityUuid(entityClass, param);
                    entityType = entityClass.getName();
                } catch (Exception e){
                    s_logger.debug("Caught exception while finding entityUUID, moving on");
                }
            }
        }

        org.apache.cloudstack.framework.events.Event event =
            new org.apache.cloudstack.framework.events.Event(ManagementService.Name, eventCategory, eventType, EventTypes.getEntityForEvent(eventType), entityUuid);

        Map<String, String> eventDescription = new HashMap<String, String>();
        Project project = s_projectDao.findByProjectAccountId(accountId);
        Account account = s_accountDao.findById(accountId);
        User user = s_userDao.findById(userId);
        // if account has been deleted, this might be called during cleanup of resources and results in null pointer
        if (account == null)
            return;
        if (user == null)
            return;
        if (project != null)
            eventDescription.put("project", project.getUuid());
        eventDescription.put("user", user.getUuid());
        eventDescription.put("account", account.getUuid());
        eventDescription.put("event", eventType);
        eventDescription.put("status", state.toString());
        eventDescription.put("entity", entityType);
        eventDescription.put("entityuuid", entityUuid);
        //Put all the first class entities that are touched during the action. For now atleast put in the vmid.
        populateFirstClassEntities(eventDescription);
        eventDescription.put("description", description);

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

        event.setDescription(eventDescription);

        try {
            s_eventBus.publish(event);
        } catch (EventBusException e) {
            s_logger.warn("Failed to publish action event on the the event bus.");
        }
    }

    private static String getEntityUuid(Class<?> entityType, Object entityId){

        // entityId can be internal db id or UUID so accordingly call findbyId or return uuid directly

        if (entityId instanceof Long){
            // Its internal db id - use findById
            final Object objVO = s_entityMgr.findById(entityType, (Long)entityId);
            return ((Identity)objVO).getUuid();
        } else if(entityId instanceof String){
            try{
                // In case its an async job the internal db id would be a string because of json deserialization
                Long internalId = Long.valueOf((String) entityId);
                final Object objVO = s_entityMgr.findById(entityType, internalId);
                return ((Identity)objVO).getUuid();
            } catch (NumberFormatException e){
                // It is uuid - so return it
                return (String)entityId;
            }
        }

        return null;
    }

    private static long getDomainId(long accountId) {
        AccountVO account = s_accountDao.findByIdIncludingRemoved(accountId);
        if (account == null) {
            s_logger.error("Failed to find account(including removed ones) by id '" + accountId + "'");
            return 0;
        }
        return account.getDomainId();
    }

    private static void populateFirstClassEntities(Map<String, String> eventDescription){

        CallContext context = CallContext.current();
        Map<Object, Object> contextMap = context.getContextParameters();

        for(Map.Entry<Object, Object> entry : contextMap.entrySet()){
            try{
                Class<?> clz = (Class<?>)entry.getKey();
                if(clz != null && Identity.class.isAssignableFrom(clz)){
                    String uuid = getEntityUuid(clz, entry.getValue());
                    eventDescription.put(ReflectUtil.getEntityName(clz), uuid);
                }
            } catch (Exception e){
                s_logger.trace("Caught exception while populating first class entities for event bus, moving on");
            }
        }

    }

}
