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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.EventDistributor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.configuration.Config;
import com.cloud.event.dao.EventDao;
import com.cloud.projects.Project;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;

public class ActionEventUtils {
    protected static Logger LOGGER = LogManager.getLogger(ActionEventUtils.class);

    private static EventDao s_eventDao;
    private static AccountDao s_accountDao;
    private static ProjectDao s_projectDao;
    protected static UserDao s_userDao;
    private static EventDistributor eventDistributor;
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

    public static Long onActionEvent(Long userId, Long accountId, Long domainId, String type, String description, Long resourceId, String resourceType) {
        Ternary<Long, String, String> resourceDetails = getResourceDetails(resourceId, resourceType, type);
        Event event = persistActionEvent(userId, accountId, domainId, null, type, Event.State.Completed,
                true, description, resourceDetails.first(), resourceDetails.third(), null);
        publishOnEventBus(event, userId, accountId, domainId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Completed, description, resourceDetails.second(), resourceDetails.third());
        return event.getId();
    }

    /*
     * Save event after scheduling an async job
     */
    public static Long onScheduledActionEvent(Long userId, Long accountId, String type, String description, Long resourceId, String resourceType, boolean eventDisplayEnabled, long startEventId) {
        Ternary<Long, String, String> resourceDetails = getResourceDetails(resourceId, resourceType, type);
        CallContext ctx = CallContext.current();
        accountId = getOwnerAccountId(ctx, type, accountId);
        Event event = persistActionEvent(userId, accountId, null, null, type, Event.State.Scheduled,
                eventDisplayEnabled, description, resourceDetails.first(), resourceDetails.third(), startEventId);
        publishOnEventBus(event, userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Scheduled, description, resourceDetails.second(), resourceDetails.third());
        return event.getId();
    }

    public static void startNestedActionEvent(String eventType, String eventDescription, Long resourceId, String resourceType) {
        CallContext.setActionEventInfo(eventType, eventDescription);
        onStartedActionEventFromContext(eventType, eventDescription, resourceId, resourceType, true);
    }

    public static void onStartedActionEventFromContext(String eventType, String eventDescription, Long resourceId, String resourceType, boolean eventDisplayEnabled) {
        CallContext ctx = CallContext.current();
        long userId = ctx.getCallingUserId();
        long accountId = getOwnerAccountId(ctx, eventType, ctx.getCallingAccountId());
        long startEventId = ctx.getStartEventId();

        if (!eventType.equals(""))
            ActionEventUtils.onStartedActionEvent(userId, accountId, eventType, eventDescription, resourceId, resourceType, eventDisplayEnabled, startEventId);
    }

    /*
     * Save event after starting execution of an async job
     */
    public static Long onStartedActionEvent(Long userId, Long accountId, String type, String description, Long resourceId, String resourceType, boolean eventDisplayEnabled, long startEventId) {
        Ternary<Long, String, String> resourceDetails = getResourceDetails(resourceId, resourceType, type);
        Event event = persistActionEvent(userId, accountId, null, null, type, Event.State.Started,
                eventDisplayEnabled, description, resourceDetails.first(), resourceDetails.third(), startEventId);
        publishOnEventBus(event, userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Started, description, resourceDetails.second(), resourceDetails.third());
        return event.getId();
    }

    public static Long onCompletedActionEvent(Long userId, Long accountId, String level, String type, String description, Long resourceId, String resourceType, long startEventId) {

        return onCompletedActionEvent(userId, accountId, level, type, true, description, resourceId, resourceType, startEventId);
    }

    public static Long onCompletedActionEvent(Long userId, Long accountId, String level, String type, boolean eventDisplayEnabled, String description, Long resourceId, String resourceType, long startEventId) {
        Ternary<Long, String, String> resourceDetails = getResourceDetails(resourceId, resourceType, type);
        Event event = persistActionEvent(userId, accountId, null, level, type, Event.State.Completed,
                eventDisplayEnabled, description, resourceDetails.first(), resourceDetails.third(), startEventId);
        publishOnEventBus(event, userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Completed, description, resourceDetails.second(), resourceDetails.third());
        return event.getId();

    }

    public static Long onCreatedActionEvent(Long userId, Long accountId, String level, String type, boolean eventDisplayEnabled, String description, Long resourceId, String resourceType) {
        Ternary<Long, String, String> resourceDetails = getResourceDetails(resourceId, resourceType, type);
        Event event = persistActionEvent(userId, accountId, null, level, type, Event.State.Created,
                eventDisplayEnabled, description, resourceDetails.first(), resourceDetails.third(), null);
        publishOnEventBus(event, userId, accountId, EventCategory.ACTION_EVENT.getName(), type,
                com.cloud.event.Event.State.Created, description, resourceDetails.second(), resourceDetails.third());
        return event.getId();
    }

    private static Event persistActionEvent(Long userId, Long accountId, Long domainId, String level, String type,
                                            Event.State state, boolean eventDisplayEnabled, String description,
                                            Long resourceId, String resourceType, Long startEventId) {
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
        if (resourceId != null) {
            event.setResourceId(resourceId);
        }
        if (resourceType != null) {
            event.setResourceType(resourceType);
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

    private static void publishOnEventBus(Event eventRecord, long userId, long accountId, Long domainId,
          String eventCategory, String eventType, Event.State state, String description, String resourceUuid,
          String resourceType) {
        String configKey = Config.PublishActionEvent.key();
        String value = s_configDao.getValue(configKey);
        boolean configValue = Boolean.parseBoolean(value);
        if(!configValue)
            return;

        try {
            eventDistributor = ComponentContext.getComponent(EventDistributor.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        org.apache.cloudstack.framework.events.Event event =
                new org.apache.cloudstack.framework.events.Event(ManagementService.Name, eventCategory, eventType, resourceType, resourceUuid);
        event.setEventId(eventRecord.getId());
        event.setEventUuid(eventRecord.getUuid());

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
        event.setResourceAccountId(accountId);
        event.setResourceAccountUuid(account.getUuid());
        event.setResourceDomainId(domainId == null ? account.getDomainId() : domainId);
        eventDescription.put("user", user.getUuid());
        eventDescription.put("account", account.getUuid());
        eventDescription.put("event", eventType);
        eventDescription.put("status", state.toString());
        eventDescription.put("entity", resourceType);
        eventDescription.put("entityuuid", resourceUuid);
        //Put all the first class entities that are touched during the action. For now at least put in the vmid.
        populateFirstClassEntities(eventDescription);
        eventDescription.put("description", description);

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

        event.setDescription(eventDescription);

        eventDistributor.publish(event);
    }

    private static void publishOnEventBus(Event event, long userId, long accountId, String eventCategory,
          String eventType, Event.State state, String description, String resourceUuid, String resourceType) {
        publishOnEventBus(event, userId, accountId, null, eventCategory, eventType, state, description,
                resourceUuid, resourceType);
    }

    private static Ternary<Long, String, String> getResourceDetailsUsingEntityClassAndContext(Class<?> entityClass, ApiCommandResourceType resourceType) {
        CallContext context = CallContext.current();
        ApiCommandResourceType alternateResourceType = ApiCommandResourceType.valueFromAssociatedClass(entityClass);
        if (resourceType == null ||
                (ObjectUtils.allNotNull(resourceType, alternateResourceType) &&
                        resourceType.getAssociatedClass() != alternateResourceType.getAssociatedClass())) {
            resourceType = alternateResourceType;
        }
        String entityType = resourceType == null ? entityClass.getSimpleName() : resourceType.toString();
        String entityUuid = null;
        Long entityId = null;
        Object param = context.getContextParameter(entityClass);
        if(param != null){
            try {
                entityUuid = getEntityUuid(entityClass, param);
            } catch (Exception e){
                LOGGER.debug("Caught exception while finding entityUUID, moving on");
            }
        }
        if (param instanceof Long) {
            entityId = (Long)param;
        } else if (entityUuid != null) {
            Object obj = s_entityMgr.findByUuidIncludingRemoved(entityClass, entityUuid);
            if (obj instanceof InternalIdentity) {
                entityId = ((InternalIdentity)obj).getId();
            }
        }
        return new Ternary<>(entityId, entityUuid, entityType);
    }

    private static Ternary<Long, String, String> getResourceDetailsUsingEventTypeAndContext(ApiCommandResourceType resourceType, String eventType) {
        Class<?> entityClass = EventTypes.getEntityClassForEvent(eventType);
        if (entityClass != null && s_entityMgr.validEntityType(entityClass)) {
            return getResourceDetailsUsingEntityClassAndContext(entityClass, resourceType);
        } else if (resourceType != null && resourceType.getAssociatedClass() != null && s_entityMgr.validEntityType(resourceType.getAssociatedClass())) {
            return getResourceDetailsUsingEntityClassAndContext(resourceType.getAssociatedClass(), resourceType);
        }
        return new Ternary<Long, String, String>(null, null, null);
    }

    private static String getEntityUuid(Class<?> entityType, Object entityId){

        // entityId can be internal db id or UUID so accordingly call findById or return uuid directly

        if (entityId instanceof Long){
            // Its internal db id - use findById
            if (!s_entityMgr.validEntityType(entityType)) {
                return null;
            }
            final Object objVO = s_entityMgr.findByIdIncludingRemoved(entityType, (Long)entityId);
            if (objVO != null) {
                return ((Identity) objVO).getUuid();
            }
        } else if(entityId instanceof String) {
            try{
                // In case its an async job the internal db id would be a string because of json deserialization
                Long internalId = Long.valueOf((String) entityId);
                if (!s_entityMgr.validEntityType(entityType)) {
                    return null;
                }
                final Object objVO = s_entityMgr.findByIdIncludingRemoved(entityType, internalId);
                if (objVO != null) {
                    return ((Identity) objVO).getUuid();
                }
            } catch (NumberFormatException e) {
                // It is uuid - so return it
                return (String)entityId;
            }
        }

        return null;
    }

    private static Ternary<Long, String, String> updateParentResourceCases(Ternary<Long, String, String> details) {
        if (!ObjectUtils.allNotNull(details, details.first(), details.second(), details.third())) {
            return details;
        }
        HashMap<String, Pair<ApiCommandResourceType, String>> typeParentMethodMap = new HashMap<>();
        typeParentMethodMap.put(ApiCommandResourceType.VmSnapshot.toString(), new Pair<>(ApiCommandResourceType.VirtualMachine, "getVmId"));
        if (!typeParentMethodMap.containsKey(details.third())) {
            return details;
        }
        ApiCommandResourceType type = ApiCommandResourceType.fromString(details.third());
        if (type == null || !s_entityMgr.validEntityType(type.getAssociatedClass())) {
            return details;
        }
        Object objVO = s_entityMgr.findByIdIncludingRemoved(type.getAssociatedClass(), details.first());
        if (objVO == null) {
            return details;
        }
        String methodName = typeParentMethodMap.get(type.toString()).second();
        try {
            Method m = objVO.getClass().getMethod(methodName);
            Long id = (Long)m.invoke(objVO);
            if (id == null) {
                return details;
            }
            type = typeParentMethodMap.get(type.toString()).first();
            objVO = s_entityMgr.findByIdIncludingRemoved(type.getAssociatedClass(), id);
            if (objVO == null) {
                return details;
            }
            return new Ternary<>(id, ((Identity)objVO).getUuid(), type.toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.debug(String.format("Parent resource for resource ID: %d, type: %s can not be found using method %s", details.first(), type, methodName));
        }
        return details;
    }

    private static Ternary<Long, String, String> getResourceDetails(Long resourceId, String resourceType, String eventType) {
        Ternary<Long, String, String> details;
        Class<?> clazz = null;
        ApiCommandResourceType type = null;
        if (StringUtils.isNotEmpty(resourceType)) {
            type = ApiCommandResourceType.fromString(resourceType);
            if (type != null) {
                clazz = type.getAssociatedClass();
            }
        }
        if (ObjectUtils.allNotNull(resourceId, clazz)) {
            String uuid = getEntityUuid(clazz, resourceId);
            details = new Ternary<>(resourceId, uuid, resourceType);
        } else {
            details = getResourceDetailsUsingEventTypeAndContext(type, eventType);
        }
        return updateParentResourceCases(details);
    }

    private static long getDomainId(long accountId) {
        AccountVO account = s_accountDao.findByIdIncludingRemoved(accountId);
        if (account == null) {
            LOGGER.error("Failed to find account(including removed ones) by id '" + accountId + "'");
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
                LOGGER.trace("Caught exception while populating first class entities for event bus, moving on");
            }
        }
    }

    public static long getOwnerAccountId(CallContext ctx, String eventType, long callingAccountId) {
        List<String> mainProjectEvents = List.of(EventTypes.EVENT_PROJECT_CREATE, EventTypes.EVENT_PROJECT_UPDATE, EventTypes.EVENT_PROJECT_DELETE);
        long accountId = ctx.getProject() != null && !mainProjectEvents.stream().anyMatch(eventType::equalsIgnoreCase) ? ctx.getProject().getProjectAccountId() : callingAccountId;    //This should be the entity owner id rather than the Calling User Account Id.
        return accountId;
    }
}
