package com.cloud.event;

public class Cadf {
    public enum EventType {
        MONITOR,
        ACTIVITY,
        CONTROL;
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        UNKNOWN,
        PENDING;
    }

    public enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        MONITOR,
        BACKUP,
        CAPTURE,
        CONFIGURE,
        DEPLOY,
        DISABLE,
        ENABLE,
        RESTORE,
        START,
        STOP,
        UNDEPLOY,
        RECEIVE,
        SEND,
        AUTHENTICATE,
        LOGIN,
        RENEW,
        REVOLKE,
        ALLOW,
        DENY,
        EVALUATE,
        NOTIFY,
        UNKNOWN,

    }

    private static String s_typeURI = "http://schemas.dmtf.org/cloud/audit/1.0/event";
    private EventType _eventType;
    private Resource _observer;
    private Resource _initiator;
    private Resource _target;
    private Action _action;
    private Outcome _outcome;

    public Cadf(EventType eventType, Resource observer, Resource initiator,
                     Resource target, Action action, Outcome outcome) {
        _eventType = eventType;
        _observer = observer;
        _initiator = initiator;
        _target = target;
        _action = action;
        _outcome = outcome;
    }

    /*

     */
    public Cadf(EventVO event) {
        //create a cadf mapping / cadf event from the generic cloudstack event
        //_eventType = event
        _eventType = EventType.MONITOR;
    }


}
