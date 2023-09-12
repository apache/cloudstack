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
// under the License

package org.apache.cloudstack.syslog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.naming.ConfigurationException;

import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;

public class AlertsSyslogAppenderTest {
    AlertsSyslogAppender _appender = new AlertsSyslogAppender();

    @Before
    public void setUp() throws ConfigurationException {
        _appender.setLayout(new PatternLayout("%-5p [%c{3}] (%t:%x) %m%n"));
        _appender.setFacility("LOCAL6");
    }

    @Test
    public void setSyslogAppendersTest() {
        _appender.setSyslogHosts("10.1.1.1,10.1.1.2");
        assertEquals(" error Syslog Appenders list size not as expected ", 2, _appender._syslogAppenders.size());
    }

    @Test
    public void setSyslogAppendersWithPortTest() {
        _appender.setSyslogHosts("10.1.1.1:897,10.1.1.2");
        assertEquals(" error Syslog Appenders list size not as expected ", 2, _appender._syslogAppenders.size());
    }

    @Test
    public void setSyslogAppendersNegativeTest() {
        //setting invalid IP for Syslog Hosts
        _appender.setSyslogHosts("10.1.1.");
        assertTrue(" list was expected to be empty", _appender._syslogAppenders.isEmpty());
    }

    @Test
    public void appendTest() {
        String message = "alertType:: 14 // dataCenterId:: 0 // podId:: 0 // clusterId:: null // message:: Management" + " server node 127.0.0.1 is up";
        _appender.parseMessage(message);
        String createdMessage = _appender.createSyslogMessage();
        assertTrue(" message is not as expected ",
            createdMessage.contains("alertType:: managementNode" + AlertsSyslogAppender.MESSAGE_DELIMITER_STRING + "message:: Management server node 127.0.0.1 is up"));
        assertTrue("severity level not as expected ", createdMessage.contains("WARN"));
    }

    @Test
    public void appendUnknownTest() {
        String message = "alertType:: 40 // dataCenterId:: 0 // podId:: 0 // clusterId:: null // message:: Management" + " server node 127.0.0.1 is up";
        _appender.parseMessage(message);
        String createdMessage = _appender.createSyslogMessage();
        assertTrue(" message is not as expected ",
                createdMessage.contains("alertType:: unknown" + AlertsSyslogAppender.MESSAGE_DELIMITER_STRING + "message:: Management server node 127.0.0.1 is up"));
        assertTrue("severity level not as expected ", createdMessage.contains("WARN"));
    }

    @Test
    public void appendFirstAlertTest() {
        String message = "alertType:: 0 // dataCenterId:: 0 // podId:: 0 // clusterId:: null // message:: Management" + " server node 127.0.0.1 is up";
        _appender.parseMessage(message);
        String createdMessage = _appender.createSyslogMessage();
        assertTrue(" message is not as expected ",
                createdMessage.contains("alertType:: availableMemory" + AlertsSyslogAppender.MESSAGE_DELIMITER_STRING + "message:: Management server node 127.0.0.1 is up"));
        assertTrue("severity level not as expected ", createdMessage.contains("WARN"));
    }
}
