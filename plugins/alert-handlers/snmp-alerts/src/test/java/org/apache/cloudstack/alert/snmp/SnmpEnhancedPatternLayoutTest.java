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

package org.apache.cloudstack.alert.snmp;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.junit.Test;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SnmpEnhancedPatternLayoutTest {

    @Mock
    Message messageMock;
    @Mock
    LogEvent eventMock;

    @Spy
    @InjectMocks
    SnmpEnhancedPatternLayout _snmpEnhancedPatternLayout = new SnmpEnhancedPatternLayout();

    @Test
    public void parseAlertTest() {
        setMessage(" alertType:: 14 // dataCenterId:: 1 // podId:: 1 // " + "clusterId:: null // message:: Management"
            + " network CIDR is not configured originally. Set it default to 10.102.192.0/22", eventMock);
        SnmpTrapInfo info = _snmpEnhancedPatternLayout.parseEvent(eventMock);
        commonAssertions(info, "Management network CIDR is not configured originally. Set it default to 10.102.192" + ".0/22");
    }

    @Test
    public void ParseAlertWithPairDelimeterInMessageTest() {
        setMessage(" alertType:: 14 // dataCenterId:: 1 // podId:: 1 // " + "clusterId:: null // message:: Management"
            + " //network CIDR is not configured originally. Set it default to 10.102.192.0/22", eventMock);
        SnmpTrapInfo info = _snmpEnhancedPatternLayout.parseEvent(eventMock);
        commonAssertions(info, "Management //network CIDR is not configured originally. Set it default to 10.102.192" + ".0/22");
    }

    @Test
    public void ParseAlertWithKeyValueDelimeterInMessageTest() {
        setMessage(" alertType:: 14 // dataCenterId:: 1 // podId:: 1 // " + "clusterId:: null // message:: Management"
            + " ::network CIDR is not configured originally. Set it default to 10.102.192.0/22", eventMock);
        SnmpTrapInfo info = _snmpEnhancedPatternLayout.parseEvent(eventMock);
        commonAssertions(info, "Management ::network CIDR is not configured originally. Set it default to 10.102.192" + ".0/22");
    }

    @Test
    public void parseRandomTest() {
        setMessage("Problem clearing email alert", eventMock);
        assertNull(" Null value was expected ", _snmpEnhancedPatternLayout.parseEvent(eventMock));
    }

    private void commonAssertions(SnmpTrapInfo info, String message) {
        assertEquals(" alert type not as expected ", 14, info.getAlertType());
        assertEquals(" data center id not as expected ", 1, info.getDataCenterId());
        assertEquals(" pod id os not as expected ", 1, info.getPodId());
        assertEquals(" cluster id is not as expected ", 0, info.getClusterId());
        assertNotNull(" generation time is set to null", info.getGenerationTime());
        assertEquals(" message is not as expected ", message, info.getMessage());
    }

    private void setMessage(String message, LogEvent eventMock) {
        Mockito.doReturn(messageMock).when(eventMock).getMessage();
        Mockito.doReturn(message).when(messageMock).getFormattedMessage();
    }
}
