package com.cloud.event;

import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.Event;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AlertGenerator {

    protected static EventBus _eventBus = null;
    protected static boolean _eventBusLoaded = false;

    public static void publishAlert(String alertType, long dataCenterId, Long podId, String subject, String body) {
    }

    void publishOnEventBus(String alertType, long dataCenterId, Long podId, String subject, String body) {
        if (getEventBus() != null) {
            Map<String, String> eventDescription = new HashMap<String, String>();
            eventDescription.put("alertType", alertType);
            eventDescription.put("dataCenterId", Long.toString(dataCenterId));
            eventDescription.put("podId", Long.toString(podId));
            eventDescription.put("subject", subject);
            eventDescription.put("body", body);
            Event event = new Event(EventCategory.ALERT_EVENT.getName(), alertType, alertType);
            event.setDescription(eventDescription);
            _eventBus.publish(event);
        }
    }

    private EventBus getEventBus() {
        //TODO: check if there is way of getting single adapter
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
