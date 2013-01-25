package com.cloud.event;

import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class UsageEventGenerator {

    private static final Logger s_logger = Logger.getLogger(UsageEventGenerator.class);
    protected static EventBus _eventBus = null;
    protected static boolean _eventBusLoaded = false;

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size);
        publishOnEventBus(usageType, accountId, zoneId, resourceId, resourceName, null);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName);
        publishOnEventBus(usageType, accountId, zoneId, resourceId, resourceName, null);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType, boolean isSystem) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, ipAddressId, ipAddress, isSourceNat, guestType, isSystem);
        publishOnEventBus(usageType, accountId, zoneId, ipAddressId, "IP address", null);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, String resourceType) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType);
        publishOnEventBus(usageType, accountId, zoneId, resourceId, resourceName, resourceType);
    }

    public static void publishUsageEvent(String usageType, long accountId,long zoneId, long vmId, long securityGroupId) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, vmId, securityGroupId);
        publishOnEventBus(usageType, accountId, zoneId, vmId, null, null);
    }

    private static void publishOnEventBus(String usageType, Long accountId, Long zoneId, Long resourceId, String resourceName, String resourceType) {
        if (getEventBus() != null) {
            Map<String, String> eventDescription = new HashMap<String, String>();
            eventDescription.put("usage type", usageType);
            if (accountId != null) {
                eventDescription.put("accountId", usageType);
            }
            if (zoneId != null) {
                eventDescription.put("zoneId", String.valueOf(zoneId));
            }
            if (resourceId != null) {
                eventDescription.put("resourceId", String.valueOf(resourceId));
            }
            eventDescription.put("resourceName", resourceName);
            eventDescription.put("resourceType", resourceType);
            Event event = new Event(null, EventCategory.USAGE_EVENT.getName(), usageType, null, null);
            event.setDescription(eventDescription);
            try {
                _eventBus.publish(event);
            } catch (EventBusException e) {
                s_logger.warn("Failed to publish usage event on the the event bus.");
            }
        }
    }

    private static EventBus getEventBus() {
        if (_eventBus == null) {
            if (!_eventBusLoaded) {
                ComponentLocator locator = ComponentLocator.getLocator("management-server");
                Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
                if (eventBusImpls != null) {
                    Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                   _eventBus = eventBusenum.nextElement();
                }
                _eventBusLoaded = true;
            }
        }
        return _eventBus;
    }
}
