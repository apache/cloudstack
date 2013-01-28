package com.cloud.storage.listener;

import com.cloud.event.EventCategory;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.fsm.StateListener;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class VolumeStateListener implements StateListener<State, Event, Volume> {

    protected static EventBus _eventBus = null;

    private static final Logger s_logger = Logger.getLogger(VolumeStateListener.class);

    public VolumeStateListener() {
        initEventBusProvider();
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, Volume vo, boolean status, Object opaque) {
        pubishOnEventBus(event.name(), "preStateTransitionEvent", vo, oldState, newState);
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, Volume vo, boolean status, Object opaque) {
        pubishOnEventBus(event.name(), "postStateTransitionEvent", vo, oldState, newState);
        return true;
    }

    private void pubishOnEventBus(String event, String status, Volume vo, State oldState, State newState) {
        if (_eventBus != null) {
            String resourceName = getEntityFromClassName(Volume.class.getName());
            org.apache.cloudstack.framework.events.Event eventMsg =  new org.apache.cloudstack.framework.events.Event(
                    ManagementServer.Name,
                    EventCategory.RESOURCE_STATE_CHANGE_EVENT.getName(),
                    event,
                    resourceName,
                    vo.getUuid());
            Map<String, String> eventDescription = new HashMap<String, String>();
            eventDescription.put("resource", resourceName);
            eventDescription.put("id", vo.getUuid());
            eventDescription.put("old-state", oldState.name());
            eventDescription.put("new-state", newState.name());
            eventMsg.setDescription(eventDescription);
            try {
                _eventBus.publish(eventMsg);
            } catch (EventBusException e) {
                s_logger.warn("Failed to publish action event on the the event bus.");
            }
        }
    }

    private String getEntityFromClassName(String entityClassName) {
        int index = entityClassName.lastIndexOf(".");
        String entityName = entityClassName;
        if (index != -1) {
            entityName = entityClassName.substring(index+1);
        }
        return entityName;
    }

    private void initEventBusProvider() {
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
        if (eventBusImpls != null) {
            Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
            if (eventBusenum != null && eventBusenum.hasMoreElements()) {
                _eventBus = eventBusenum.nextElement();
            }
        }
    }

}
