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

import com.cloud.exception.InvalidParameterValueException;

import java.text.SimpleDateFormat;

public class Cadf {

    /* _typeURI should be static but Gson won't render it */
    private String _typeURI = "http://schemas.dmtf.org/cloud/audit/1.0/event";
    private String _id;
    private String _eventType;
    private Taxonomies.EventType _tmpEventType;
    private String _action;
    private Taxonomies.Action _tmpAction;

    private String _csaction; //CloudStack original Action
    private String _outcome;
    private String _eventTime;

    private Resource _observer;
    private Resource _initiator;
    private Resource _target;
    private String _tmpTarget;

    //TODO
    private String _measurement;
    private String _reason;

    public Cadf(EventVO event) {
        //Event unique identifier
        _id = event.getUuid();

        //sets EventType, Action and Target
        mapEventTypeToCADFTaxonomy(event.getType());

        //Answers to ONWHAT and TOWHERE
        _target = new Resource(_tmpTarget);

        _outcome = Taxonomies.Outcome.FAILURE.getValue();
        //must add reason

        //Answers to WHEN
        //_eventTime must be in UTC format
        if (event.getCreateDate() != null) {
            _eventTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Z").format(event.getCreateDate());
        } else {
            _eventTime="";
        }

        //Answers to WHO and FROMWHERE
        _initiator = new Resource(Taxonomies.Resource.SERVICE_SECURITY);

        //Answers to WHERE
        _observer = new Resource(Taxonomies.Resource.DATA_SECURITY);

    }

    //TODO
    //turn this into private
    public void mapEventTypeToCADFTaxonomy(String type) {
        String eventtarget;
        String eventaction;

        System.out.println("EventType " + type);

        if (!type.contains(".")) {
            eventaction = Taxonomies.Action.UNKNOWN.getValue();
            eventtarget = Taxonomies.Resource.UNKNOWN;
        } else {
            //Substrings "VM" from VM.CREATE, "ROLE.PERMISSION" from ROLE.PERMISSION.CREATE etc
            eventtarget = new String(type.substring(0, type.lastIndexOf(".")));
            //Substrings "CREATE" from VM.CREATE, "CREATE" from ROLE.PERMISSION.CREATE etc.
            // +1 is used to ignore the "." before substring
            eventaction = new String(type.substring(type.lastIndexOf(".") + 1));
            //eventtarget is event's Target
            //eventaction is event's Action
        }

        System.out.println("type " + type);
        System.out.println("part1 " + eventtarget);
        System.out.println("part2 " + eventaction);


        setCADFAction(eventaction);
        setCADFEventType();
        setCADFTarget(eventtarget);
    }

    /**
     * Maps eventaction to a CADF Action and sets _action to a CADF compatible value
     *
     * @param eventaction CloudStack's action for every event type. eg for eventType
     *                    ROLE.PERMISSION.CREATE action is CREATE.
     */
    private void setCADFAction(String eventaction) {
        Boolean found_match = false;
        for (Taxonomies.Action ta : Taxonomies.Action.values() ) {
            if (ta.getValue().equalsIgnoreCase(eventaction)) { //exact match
                _tmpAction = ta;
                _action = ta.getValue();
                _csaction = ta.getValue();
                found_match = true;
                break;
            } else if (ta.getValue().contains(eventaction.toLowerCase())) { //partial match
                _tmpAction = ta;
                _action = ta.getValue();
                _csaction = ta.getValue() + " - Original Cloudstack actions is " + eventaction;
                found_match = true;
                break;
            }
        }
        if (!found_match) {
            _tmpAction = Taxonomies.Action.UNKNOWN;
            _action = Taxonomies.Action.UNKNOWN.getValue();
            _csaction = Taxonomies.Action.UNKNOWN.getValue() + " - Original Cloudstack action is " + eventaction;
        }
    }

    /**
     * Maps eventType to a CADF compatible value based on the events Action
     */
    public void setCADFEventType() {
        if (_tmpAction == Taxonomies.Action.CREATE || _tmpAction == Taxonomies.Action.UPDATE ||
                _tmpAction == Taxonomies.Action.DELETE ||_tmpAction == Taxonomies.Action.BACKUP ||
                _tmpAction == Taxonomies.Action.CAPTURE || _tmpAction == Taxonomies.Action.CONFIGURE ||
                _tmpAction == Taxonomies.Action.DEPLOY || _tmpAction == Taxonomies.Action.RESTORE ||
                _tmpAction == Taxonomies.Action.START || _tmpAction == Taxonomies.Action.STOP ||
                _tmpAction == Taxonomies.Action.UNDEPLOY || _tmpAction == Taxonomies.Action.RECEIVE ||
                _tmpAction == Taxonomies.Action.SEND) {
            _tmpEventType = Taxonomies.EventType.ACTIVITY;
            _eventType = Taxonomies.EventType.ACTIVITY.getValue();
        } else if (_tmpAction == Taxonomies.Action.DISABLE || _tmpAction == Taxonomies.Action.ENABLE ||
                _tmpAction == Taxonomies.Action.AUTHENTICATE || _tmpAction == Taxonomies.Action.AUTHENTICATE_LOGIN ||
                _tmpAction == Taxonomies.Action.RENEW || _tmpAction == Taxonomies.Action.REVOKE ||
                _tmpAction == Taxonomies.Action.ALLOW || _tmpAction == Taxonomies.Action.DENY ||
                _tmpAction == Taxonomies.Action.EVALUATE || _tmpAction == Taxonomies.Action.NOTIFY) {
            _tmpEventType = Taxonomies.EventType.CONTROL;
            _eventType = Taxonomies.EventType.CONTROL.getValue();
        }
        else if (_tmpAction == Taxonomies.Action.MONITOR || _tmpAction == Taxonomies.Action.READ ||
                _tmpAction == Taxonomies.Action.UNKNOWN) {
            _tmpEventType = Taxonomies.EventType.MONITOR;
            _eventType = Taxonomies.EventType.MONITOR.getValue();
        }
    }


    /**
     * Maps eventtarget to a CADF compatible value
     *
     * @param eventtarget is a substring of CS EventType
     *                    eg for eventType ROLE.PERMISSION.CREATE target is ROLE.PERMISSION
     */
    public void setCADFTarget(String eventtarget) {
        _tmpTarget = Taxonomies.eventMapping.get(eventtarget);
        if (_tmpTarget == null || _tmpTarget.isEmpty()) {
            _tmpTarget = Taxonomies.Resource.UNKNOWN;
        }
    }


    /**
     * Checks the number and name of mandatory fields differs depending on the EventType .
     * @throws InvalidParameterValueException
     */
    public void checkMandatoryFields() {
        switch (_tmpEventType) {
            case MONITOR:
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                            "fields are mandatory for MONITOR events");
                }
            case CONTROL:
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_reason == null) || (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Reason, Measurement " +
                            "fields are mandatory for CONTROL events");
                }
            case ACTIVITY :
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                            "fields are mandatory for ACTIVITY events");
                }
        }
    }

}
