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

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;

public class UsageEventUtils {

    private static UsageEventDao s_usageEventDao;
    private static AccountDao s_accountDao;
    private static DataCenterDao s_dcDao;
    private static final Logger s_logger = Logger.getLogger(UsageEventUtils.class);
    protected static EventBus s_eventBus = null;
    protected static ConfigurationDao s_configDao;

    @Inject
    UsageEventDao usageEventDao;
    @Inject
    AccountDao accountDao;
    @Inject
    DataCenterDao dcDao;
    @Inject
    ConfigurationDao configDao;

    public UsageEventUtils() {
    }

    @PostConstruct
    void init() {
        s_usageEventDao = usageEventDao;
        s_accountDao = accountDao;
        s_dcDao = dcDao;
        s_configDao = configDao;
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
                                         Long size, String entityType, String entityUUID, Map<String, String> details) {
        saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size, details);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        Long size, String entityType, String entityUUID) {
        saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
                                         Long size, String entityType, String entityUUID, boolean displayResource) {
        if(displayResource){
            saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size);
        }
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);

    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
                                         Long size, Long virtualSize, String entityType, String entityUUID, Map<String, String> details) {
        saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size, virtualSize, details);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        Long size, Long virtualSize, String entityType, String entityUUID) {
        saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size, virtualSize);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, String entityType, String entityUUID) {
        saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, String entityType, String entityUUID, boolean diplayResource) {
        if (diplayResource){
            saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName);
            publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
        }
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType,
        boolean isSystem, String entityType, String entityUUID) {
        saveUsageEvent(usageType, accountId, zoneId, ipAddressId, ipAddress, isSourceNat, guestType, isSystem);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        String resourceType, String entityType, String entityUUID, boolean displayResource) {
        if(displayResource){
            saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType);
        }
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);

    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long vmId, long securityGroupId, String entityType, String entityUUID) {
        saveUsageEvent(usageType, accountId, zoneId, vmId, securityGroupId);
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        String resourceType, String entityType, String entityUUID, Map<String, String> details, boolean displayResource) {
        if(displayResource){
            saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType, details);
        }
        publishUsageEvent(usageType, accountId, zoneId, entityType, entityUUID);

    }

    private static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        String resourceType, Map<String, String> details) {
        UsageEventVO usageEvent = new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType);
        s_usageEventDao.persist(usageEvent);
        s_usageEventDao.saveDetails(usageEvent.getId(), details);
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size, Map<String, String> details) {
        UsageEventVO usageEventVO = new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size);
        s_usageEventDao.persist(usageEventVO);
        if (MapUtils.isNotEmpty(details)) {
            s_usageEventDao.saveDetails(usageEventVO.getId(), details);
        }
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size,
        Long virtualSize) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size, virtualSize));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size,
                                      Long virtualSize, Map<String, String> details) {
        UsageEventVO usageEventVO = new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size, virtualSize);
        s_usageEventDao.persist(usageEventVO);
        if (MapUtils.isNotEmpty(details)) {
            s_usageEventDao.saveDetails(usageEventVO.getId(), details);
        }
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType,
        boolean isSystem) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, ipAddressId, ipAddress, isSourceNat, guestType, isSystem));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
        String resourceType) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType));
    }

    public static void saveUsageEvent(String usageType, long accountId, long zoneId, long vmId, long securityGroupId) {
        s_usageEventDao.persist(new UsageEventVO(usageType, accountId, zoneId, vmId, securityGroupId));
    }

    private static void publishUsageEvent(String usageEventType, Long accountId, Long zoneId, String resourceType, String resourceUUID) {
        String configKey = "publish.usage.events";
        String value = s_configDao.getValue(configKey);
        boolean configValue = Boolean.parseBoolean(value);
        if( !configValue)
            return;
        try {
            s_eventBus = ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        Account account = s_accountDao.findById(accountId);
        DataCenterVO dc = s_dcDao.findById(zoneId);

        // if account has been deleted, this might be called during cleanup of resources and results in null pointer
        if (account == null)
            return;

        // if an invalid zone is passed in, create event without zone UUID
        String zoneUuid = null;
        if (dc != null)
            zoneUuid = dc.getUuid();

        Event event = new Event(Name, EventCategory.USAGE_EVENT.getName(), usageEventType, resourceType, resourceUUID);

        Map<String, String> eventDescription = new HashMap<String, String>();
        eventDescription.put("account", account.getUuid());
        eventDescription.put("zone", zoneUuid);
        eventDescription.put("event", usageEventType);
        eventDescription.put("resource", resourceType);
        eventDescription.put("id", resourceUUID);

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

        event.setDescription(eventDescription);

        try {
            s_eventBus.publish(event);
        } catch (EventBusException e) {
            s_logger.warn("Failed to publish usage event on the the event bus.");
        }
    }

    static final String Name = "management-server";

}
