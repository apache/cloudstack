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

import com.cloud.event.dao.EventDao;
import com.cloud.server.ManagementServer;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentLocator;

public class EventUtils {
	private static EventDao _eventDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(EventDao.class);
	private static AccountDao _accountDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(AccountDao.class);

    public static Long saveEvent(Long userId, Long accountId, Long domainId, String type, String description) {
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
    public static Long saveScheduledEvent(Long userId, Long accountId, String type, String description, long startEventId) {
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
    public static Long saveStartedEvent(Long userId, Long accountId, String type, String description, long startEventId) {
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

    public static Long saveEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
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
    
    public static Long saveCreatedEvent(Long userId, Long accountId, String level, String type, String description) {
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
}
