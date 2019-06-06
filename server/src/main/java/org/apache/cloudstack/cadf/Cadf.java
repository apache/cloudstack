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

package org.apache.cloudstack.cadf;

import com.cloud.event.EventVO;
import com.cloud.exception.InvalidParameterValueException;
import com.google.gson.annotations.Expose;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;

public class Cadf {

    //private class variables have no underscore prefix (as instructed in Coding conventions document -
    // Naming Conventions - Instruction 6) because the whole class is being logged and is more readable
    // without underscores
    @Expose (serialize = false)
    private static final Logger s_logger = Logger.getLogger(Cadf.class);

    @Expose
    private String typeURI;
    @Expose
    private String id;
    @Expose
    private String eventType;
    @Expose
    private String action;
    @Expose (serialize = false)
    private Taxonomies.Action _tmpAction;

    @Expose
    private String csaction; //CloudStack original Action
    @Expose
    private String outcome;
    @Expose
    private String eventTime;

    @Expose
    private Resource observer;
    @Expose
    private Resource initiator;
    @Expose
    private Resource target;

    //TODO
    private String measurement;
    private String reason;

    @Expose (serialize = false)
    public static HashMap<String, String> eventExtraInformation = new HashMap<String, String>();

    /**
     * @param event is the generic CS event
     */
    public Cadf(EventVO event) {
        String eventtarget;
        String eventaction;

        typeURI = "http://schemas.dmtf.org/cloud/audit/1.0/event";

        //Event unique identifier
        id = event.getUuid();

        if (!event.getType().contains(".")) {
            eventaction = Taxonomies.Action.UNKNOWN.getValue();
            eventtarget = Taxonomies.Resource.UNKNOWN;
        } else {
            //Substrings "VM" from VM.CREATE, "ROLE.PERMISSION" from ROLE.PERMISSION.CREATE etc
            eventtarget = new String(event.getType().substring(0, event.getType().lastIndexOf(".")));

            //Substrings "CREATE" from VM.CREATE, "CREATE" from ROLE.PERMISSION.CREATE etc.
            // +1 is used to ignore the "." before substring
            eventaction = new String(event.getType().substring(event.getType().lastIndexOf(".") + 1));

            //eventtarget is event's Target
            //eventaction is event's Action
        }

        //sets EventType, Action and Target
        //Answers to WHAT, ONWHAT, TOWHERE
        setCADFAction(eventaction);

        eventType = getCADFEventType();


        //Answers to ONWHAT and TOWHERE
        target = new Resource(getCADFResourceName(eventtarget), getCADFResourceUUID(eventtarget));


        //sets Outcome
        //Answers to WHAT
        mapEventStateToCADFTaxonomy(event.getState().toString());

        //must add reason

        //Answers to WHEN
        //_eventTime must be in UTC format
        if (event.getCreateDate() != null) {
            eventTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Z").format(event.getCreateDate());
        } else {
            eventTime="";
        }

        //Answers to WHO and FROMWHERE
        //"USER" CS Resource corresponds to Taxonomies.Resource.DATA_SECURITY_ACCOUNT_USER CADF Resource

        initiator = new Resource(Taxonomies.Resource.DATA_SECURITY_ACCOUNT_USER,
                getCADFResourceUUID("USER"));


        //Answers to WHERE

        observer = new Resource(Taxonomies.Resource.DATA_SECURITY,
                getCADFResourceUUID("SYSTEM.MONITOR"));

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            observer.host = new Resource.Host();
            observer.host = new Resource.Host(UUID.nameUUIDFromBytes(inetAddress.getHostName().getBytes()).toString(),
                    inetAddress.getHostAddress(), null , null);

        } catch (UnknownHostException e) {
            s_logger.error(e.getMessage());
        }
        if (eventExtraInformation != null &&
                eventExtraInformation.get("initiator_host") != null &&
                eventExtraInformation.get("initiator_user-agent") != null &&
                !eventExtraInformation.get("initiator_host").isEmpty()) {
            initiator.host = new Resource.Host(UUID.nameUUIDFromBytes(eventExtraInformation.get("initiator_host").getBytes()).toString(),
                    eventExtraInformation.get("initiator_host"),
                    eventExtraInformation.get("initiator_user-agent"),
                    null);
        }
        if (eventExtraInformation != null &&
                eventExtraInformation.get("initiator_userid") != null &&
                !eventExtraInformation.get("initiator_userid").isEmpty()) {
            initiator.userid = eventExtraInformation.get("initiator_userid");
        }
        if (eventExtraInformation != null &&
                eventExtraInformation.get("initiator_csAccountName") != null &&
                !eventExtraInformation.get("initiator_csAccountName").isEmpty()) {
            initiator.csAccountName = eventExtraInformation.get("initiator_csAccountName");
        }
    }


    /**
     * Maps event state to CADF Outcome Taxonomy and sets outcome
     * to a CADF compatible value
     *
     * @param eventstate is the String value of CS Event State
     *                   use event.getState().asString
     */
    private void mapEventStateToCADFTaxonomy(String eventstate) {
        switch (eventstate) {
            case "Completed" :
                outcome = Taxonomies.Outcome.SUCCESS.getValue();
                break;
            case "Created" :
            case "Scheduled" :
                outcome = Taxonomies.Outcome.UNKNOWN.getValue();
                break;
            case "Started" :
                outcome = Taxonomies.Outcome.PENDING.getValue();
                break;
            default :
                outcome = Taxonomies.Outcome.FAILURE.getValue();
                break;
        }
    }


    /**
     * Maps eventaction to a CADF Action and sets action to a CADF compatible value
     *
     * @param eventaction CloudStack's action for every event type. eg for eventType
     *                    ROLE.PERMISSION.CREATE action is CREATE.
     */
    private void setCADFAction(String eventaction) {
        Boolean isFound = false;
        for (Taxonomies.Action ta : Taxonomies.Action.values() ) {
            if (ta.getValue().equalsIgnoreCase(eventaction)) { //exact match
                _tmpAction = ta;
                action = ta.getValue();
                csaction = ta.getValue() + " - Original Cloudstack actions is " + eventaction;
                isFound = true;
                break;
            } else if (ta.getValue().contains(eventaction.toLowerCase())) { //partial match
                _tmpAction = ta;
                action = ta.getValue();
                csaction = ta.getValue() + " - Original Cloudstack actions is " + eventaction;
                isFound = true;
                break;
            }
        }
        if (!isFound) {
            _tmpAction = Taxonomies.Action.UNKNOWN;
            action = Taxonomies.Action.UNKNOWN.getValue();
            csaction = Taxonomies.Action.UNKNOWN.getValue() + " - Original Cloudstack action is " + eventaction;
        }
    }

    /**
     * Maps eventType to a CADF compatible value based on the events Action
     */
    private String getCADFEventType() {
        Taxonomies.EventType _tmpEventType;
        _tmpEventType = Taxonomies.eventActionToTypeMapping.get(_tmpAction);
        if (_tmpEventType == null) {
            _tmpEventType = Taxonomies.EventType.MONITOR;
        }
        return _tmpEventType.getValue();
    }

    /**
     * Maps CloudStack Resource to a CADF compatible value and returns the name of the resource
     *
     * @param csResource is a String representation of CS Resource that started the event
     *                   eg for eventType ROLE.PERMISSION.CREATE csResource is ROLE.PERMISSION
     */
    private String getCADFResourceName(String csResource) {
        String tmpResourceName = Taxonomies.cstoCadfResourceMapping.get(csResource);
        if (tmpResourceName == null || tmpResourceName.isEmpty()) {
            tmpResourceName = Taxonomies.Resource.UNKNOWN;
        }
        return tmpResourceName;
    }

    /**
     * Maps CloudStack Resource to a CADF compatible value and returns the UUID value
     *
     * @param csResource is a String representation of CS Resource that started the event
     */
    private String getCADFResourceUUID(String csResource) {
        String tmpResourceUUID = Taxonomies.eventResourcetoUuidMapping.get(csResource);
        if (tmpResourceUUID == null || tmpResourceUUID.isEmpty()) {
            tmpResourceUUID = UUID.nameUUIDFromBytes("UNKNOWN".getBytes()).toString();
        }
        return tmpResourceUUID;
    }

    /**
     * Checks the number and name of mandatory fields differs depending on the EventType .
     * @throws InvalidParameterValueException
     */
    public void checkMandatoryFields() {
        if (eventType.equals(Taxonomies.EventType.MONITOR.getValue())) {
            if ((initiator == null) || (action == null) || (target == null) || (outcome == null) ||
                    (measurement == null)) {
                throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                        "fields are mandatory for MONITOR events");
            }
        } else if (eventType.equals(Taxonomies.EventType.CONTROL.getValue())) {
            if ((initiator == null) || (action == null) || (target == null) || (outcome == null) ||
                    (reason == null) || (measurement == null)) {
                throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Reason, Measurement " +
                        "fields are mandatory for CONTROL events");
            }
        } else {
            //if (eventType.equals(Taxonomies.EventType.ACTIVITY.getValue())) {
                if ((initiator == null) || (action == null) || (target == null) || (outcome == null) ||
                        (measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                            "fields are mandatory for ACTIVITY events");
                }
        }
    }
}
