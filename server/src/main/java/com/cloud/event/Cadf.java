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

public class Cadf {

    /* _typeURI should be static but Gson won't render it */
    private String _typeURI = "http://schemas.dmtf.org/cloud/audit/1.0/event";
    private String _id;
    private String _eventType;
    private String _action;
    private String _outcome;
    private String _eventTime;

    private Resource _observer;
    private Resource _initiator;
    private Resource _target;

    //TODO
    private String _measurement;
    private String _reason;

    public Cadf(EventVO event) {
        //Event unique identifier
        _id = event.getUuid();

        //Answers to WHAT
        _eventType = Taxonomies.EventType.ACTIVITY;
        _action = Taxonomies.Action.CONFIGURE;
        _outcome = Taxonomies.Outcome.FAILURE;
        //must add reason

        //Answers to WHEN
        //_eventTime must be in UTC format
        //_eventTime = event.getCreateDate().toString();

        //Answers to WHO and FROMWHERE
        _initiator = new Resource(Taxonomies.Resource.SERVICE_SECURITY);

        //Answers to ONWHAT and TOWHERE
        _target = new Resource(Taxonomies.Resource.COMPUTE);

        //Answers to WHERE
        _observer = new Resource(Taxonomies.Resource.SERVICE_OSS_MONITORING);


    }

    private String mapEventTypeToInitiatorTaxonomy(String eventtype) {
        return " ";
    }

    //based on the type of the event different components
    public void checkMandatoryFields() {
        switch (_eventType) {
            case Taxonomies.EventType.MONITOR :
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                            "fields are mandatory for MONITOR events");
                }
            case Taxonomies.EventType.CONTROL :
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_reason == null) || (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Reason, Measurement " +
                            "fields are mandatory for CONTROL events");
                }
            case Taxonomies.EventType.ACTIVITY :
                if ((_initiator == null) || (_action == null) || (_target == null) || (_outcome == null) ||
                        (_measurement == null)) {
                    throw new InvalidParameterValueException("Initiator, Action, Target, Outcome, Measurement " +
                            "fields are mandatory for ACTIVITY events");
                }
        }
    }

}
