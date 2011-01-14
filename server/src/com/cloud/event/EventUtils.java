package com.cloud.event;

import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.ComponentLocator;

public class EventUtils {
	private static EventDao _eventDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(EventDao.class);

    public static Long saveEvent(Long userId, Long accountId, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after scheduling an async job
     */
    public static Long saveScheduledEvent(Long userId, Long accountId, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(Event.State.Scheduled);
        event.setDescription("Scheduled async job for "+description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    /*
     * Save event after starting execution of an async job
     */
    public static Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(Event.State.Started);
        event.setDescription("Starting job for "+description);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
    	return event.getId();
    }

    public static Long saveStartedEvent(Long userId, Long accountId, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(Event.State.Started);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    public static Long saveEvent(Long userId, Long accountId, String level, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    public static Long saveEvent(Long userId, Long accountId, String level, String type, String description, String params) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setParameters(params);
        event = _eventDao.persist(event);
        return event.getId();
    }

    public static Long saveEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        return (event != null ? event.getId() : null);
    }
    
    public static Long saveEvent(Long userId, Long accountId, String level, String type, String description, String params, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event.setParameters(params);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        return event.getId();
    }
    
    public static Long saveCreatedEvent(Long userId, Long accountId, String level, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setLevel(level);
        event.setState(Event.State.Created);
        event.setDescription(description);
        event = _eventDao.persist(event);
        return event.getId();
    }
}
