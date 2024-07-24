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
package com.cloud.agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.utils.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AgentShellTest {

    @InjectMocks
    @Spy
    AgentShell agentShellSpy = new AgentShell();

    @Mock
    AgentProperties agentPropertiesMock;

    @Mock
    AgentProperties.Property<Integer> propertyIntegerMock;

    @Mock
    AgentProperties.Property<String> propertyStringMock;

    @Mock
    UUID uuidMock;

    MockedStatic<AgentPropertiesFileHandler> agentPropertiesFileHandlerMocked;

    @Before
    public void setUp() throws Exception {
         agentPropertiesFileHandlerMocked = Mockito.mockStatic(AgentPropertiesFileHandler.class, Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void tearDown() throws Exception {
        agentPropertiesFileHandlerMocked.close();
    }

    @Test
    public void parseCommand() throws ConfigurationException {
        AgentShell shell = new AgentShell();
        UUID anyUuid = UUID.randomUUID();
        shell.parseCommand(new String[] {"port=55555", "threads=4", "host=localhost", "pod=pod1", "guid=" + anyUuid, "zone=zone1"});
        Assert.assertEquals(55555, shell.getPort());
        Assert.assertEquals(4, shell.getWorkers());
        Assert.assertEquals("localhost", shell.getNextHost());
        Assert.assertEquals(anyUuid.toString(), shell.getGuid());
        Assert.assertEquals("pod1", shell.getPod());
        Assert.assertEquals("zone1", shell.getZone());
    }

    @Test
    public void loadProperties() throws ConfigurationException {
        AgentShell shell = new AgentShell();
        shell.loadProperties();
        Assert.assertNotNull(shell.getProperties());
        Assert.assertFalse(shell.getProperties().entrySet().isEmpty());
    }

    @Test
    public void testGetHost() {
        AgentShell shell = new AgentShell();
        List<String> hosts = Arrays.asList("10.1.1.1", "20.2.2.2", "30.3.3.3", "2001:db8::1");
        shell.setHosts(StringUtils.listToCsvTags(hosts));
        for (String host : hosts) {
            Assert.assertEquals(host, shell.getNextHost());
        }
        Assert.assertEquals(shell.getNextHost(), hosts.get(0));
    }

    @Test
    public void isValueStartingAndEndingWithAtSignTestValues() {
        Map<String, Boolean> valuesAndExpects = new HashMap<>();
        valuesAndExpects.put("@test@", true);
        valuesAndExpects.put("test@", false);
        valuesAndExpects.put("@test", false);
        valuesAndExpects.put("test", false);
        valuesAndExpects.put("te@st", false);

        valuesAndExpects.forEach((value, expected) -> {
            boolean result = agentShellSpy.isValueStartingAndEndingWithAtSign(value);
            Assert.assertEquals(String.format("Test with value [%s] does not return as expected.", value), expected, result);
        });
    }

    @Test
    public void getPortOrWorkersTestValueIsNullGetFromProperty() {
        int expected = 195;
        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        int result = agentShellSpy.getPortOrWorkers(null, propertyIntegerMock);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getPortOrWorkersTestValueIsNotAValidIntegerReturnDefaultFromProperty() {
        int expected = 42;
        Mockito.doReturn(expected).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getPortOrWorkers("test", propertyIntegerMock);
        Assert.assertEquals(expected, result);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());
    }

    @Test
    public void getPortOrWorkersTestValueIsAValidIntegerReturnValue() {
        int expected = 42;

        Mockito.doReturn(79).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getPortOrWorkers(String.valueOf(expected), propertyIntegerMock);
        Assert.assertEquals(expected, result);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());
    }

    @Test
    public void getWorkersTestWorkersLessThan0ReturnDefault() {
        int expected = 42;

        Mockito.doReturn(propertyIntegerMock).when(agentPropertiesMock).getWorkers();
        Mockito.doReturn(-1).when(agentShellSpy).getPortOrWorkers(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getWorkers("");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getWorkersTestWorkersEqualTo0ReturnDefault() {
        int expected = 42;

        Mockito.doReturn(propertyIntegerMock).when(agentPropertiesMock).getWorkers();
        Mockito.doReturn(0).when(agentShellSpy).getPortOrWorkers(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getWorkers("");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getWorkersTestWorkersHigherThan0ReturnValue() {
        int expected = 1;
        Mockito.doReturn(expected).when(agentShellSpy).getPortOrWorkers(Mockito.any(), Mockito.any());

        int result = agentShellSpy.getWorkers("");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getZoneOrPodTestValueIsNullAndPropertyStartsAndEndsWithAtSignReturnPropertyDefaultValue() {
        String expected = "default";
        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn("test");

        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn(expected).when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod(null, propertyStringMock);
        Assert.assertEquals(expected, result);

    }

    @Test
    public void getZoneOrPodTestValueIsNullAndPropertyDoesNotStartAndEndWithAtSignReturnPropertyDefaultValue() {
        String expected = "test";

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());

        String result = agentShellSpy.getZoneOrPod(null, propertyStringMock);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getZoneOrPodTestValueIsNotNullAndStartsAndEndsWithAtSignReturnPropertyDefaultValue() {
        String expected = "default";


        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn(expected).when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod("test", propertyStringMock);
        Assert.assertEquals(expected, result);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());
    }

    @Test
    public void getZoneOrPodTestValueIsNotNullAndDoesNotStartAndEndWithAtSignReturnPropertyDefaultValue() {
        String expected = "test";


        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());

        String result = agentShellSpy.getZoneOrPod(expected, propertyStringMock);
        Assert.assertEquals(expected, result);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());
    }

    @Test
    public void getGuidTestGuidNotNullReturnIt() throws ConfigurationException {
        String expected = "test";
        String result = agentShellSpy.getGuid(expected);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getGuidTestGuidIsNullReturnProperty() throws ConfigurationException {
        String expected = "test";

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        String result = agentShellSpy.getGuid(null);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getGuidTestGuidAndPropertyAreNullIsDeveloperGenerateNewUuid() throws ConfigurationException {
        String expected = "test";

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(null, true);
        MockedStatic<UUID> uuidMocked = Mockito.mockStatic(UUID.class);
        uuidMocked.when(() -> UUID.randomUUID()).thenReturn(uuidMock);
        Mockito.doReturn(expected).when(uuidMock).toString();

        String result = agentShellSpy.getGuid(null);

        Assert.assertEquals(expected, result);
        uuidMocked.close();
    }

    @Test(expected = ConfigurationException.class)
    public void getGuidTestGuidAndPropertyAreNullIsNotDeveloperThrowConfigurationException() throws ConfigurationException {

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(null, false);

        agentShellSpy.getGuid(null);
    }

    @Test
    public void setHostTestValueIsNotNullAndStartsAndEndsWithAtSignThrowConfigurationException(){
        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());




        boolean error = false;

        try {
            agentShellSpy.setHost("test");
        } catch (ConfigurationException e) {
            error = true;
        }

        if (!error) {
            throw new AssertionError("This test expects a ConfigurationException.");
        }

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());
    }

    @Test
    public void setHostTestValueIsNullPropertyStartsAndEndsWithAtSignThrowConfigurationException(){
        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn("test");

        boolean error = false;

        try {
            agentShellSpy.setHost(null);
        } catch (ConfigurationException e) {
            error = true;
        }

        if (!error) {
            throw new AssertionError("This test expects a ConfigurationException.");
        }

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()));
    }

    @Test
    public void setHostTestValueIsNotNullAndDoesNotStartAndEndWithAtSignSetHosts() throws ConfigurationException {
        String expected = "test";
        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());

        agentShellSpy.setHost(expected);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()), Mockito.never());

        Mockito.verify(agentShellSpy).setHosts(expected);
    }

    @Test
    public void setHostTestValueIsNullPropertyDoesNotStartAndEndWithAtSignSetHosts() throws ConfigurationException {
        String expected = "test";

        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());

        agentPropertiesFileHandlerMocked.when(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        agentShellSpy.setHost(null);

        agentPropertiesFileHandlerMocked.verify(() -> AgentPropertiesFileHandler.getPropertyValue(Mockito.any()));
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());

        Mockito.verify(agentShellSpy).setHosts(expected);
    }
}
