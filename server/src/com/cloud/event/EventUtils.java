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
import com.cloud.event.dao.UsageEventDao;
import com.cloud.server.ManagementServer;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentLocator;

public class EventUtils {
	private static EventDao _eventDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(EventDao.class);
	private static AccountDao _accountDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(AccountDao.class);
    private static UsageEventDao _usageEventDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(UsageEventDao.class);

    public static Long saveActionEvent(Long userId, Long accountId, Long domainId, String type, String description) {
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
    public static Long saveScheduledActionEvent(Long userId, Long accountId, String type, String description, long startEventId) {
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
    public static Long saveStartedActionEvent(Long userId, Long accountId, String type, String description, long startEventId) {
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

    public static Long saveActionEvent(Long userId, Long accountId, String level, String type, String description, long startEventId) {
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
    
    public static Long saveCreatedActionEvent(Long userId, Long accountId, String level, String type, String description) {
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

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size) {
        _usageEventDao.persist( new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName) {
        _usageEventDao.persist( new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType, boolean isSystem) {
        _usageEventDao.persist( new UsageEventVO(usageType, accountId, zoneId, ipAddressId, ipAddress, isSourceNat, guestType, isSystem));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, String resourceType) {
        _usageEventDao.persist( new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType));
    }

    public static void saveUsageEvent(String usageType, long accountId,long zoneId, long vmId, long securityGroupId) {
        _usageEventDao.persist( new UsageEventVO( usageType, accountId, zoneId, vmId, securityGroupId));
    }
}
