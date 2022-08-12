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
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.StringUtils;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
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
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getPortOrWorkersTestValueIsNullGetFromProperty() {
        int expected = 195;
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        int result = agentShellSpy.getPortOrWorkers(null, propertyIntegerMock);
        Assert.assertEquals(expected, result);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getPortOrWorkersTestValueIsNotAValidIntegerReturnDefaultFromProperty() {
        int expected = 42;

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        Mockito.doReturn(expected).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getPortOrWorkers("test", propertyIntegerMock);
        Assert.assertEquals(expected, result);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getPortOrWorkersTestValueIsAValidIntegerReturnValue() {
        int expected = 42;

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        Mockito.doReturn(79).when(propertyIntegerMock).getDefaultValue();

        int result = agentShellSpy.getPortOrWorkers(String.valueOf(expected), propertyIntegerMock);
        Assert.assertEquals(expected, result);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
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
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getZoneOrPodTestValueIsNullAndPropertyStartsAndEndsWithAtSignReturnPropertyDefaultValue() {
        String expected = "default";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn("test");

        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn(expected).when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod(null, propertyStringMock);
        Assert.assertEquals(expected, result);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getZoneOrPodTestValueIsNullAndPropertyDoesNotStartAndEndWithAtSignReturnPropertyDefaultValue() {
        String expected = "test";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn("default").when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod(null, propertyStringMock);
        Assert.assertEquals(expected, result);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getZoneOrPodTestValueIsNotNullAndStartsAndEndsWithAtSignReturnPropertyDefaultValue() {
        String expected = "default";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);

        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn(expected).when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod("test", propertyStringMock);
        Assert.assertEquals(expected, result);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getZoneOrPodTestValueIsNotNullAndDoesNotStartAndEndWithAtSignReturnPropertyDefaultValue() {
        String expected = "test";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);

        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        Mockito.doReturn("default").when(propertyStringMock).getDefaultValue();

        String result = agentShellSpy.getZoneOrPod(expected, propertyStringMock);
        Assert.assertEquals(expected, result);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
    }

    @Test
    public void getGuidTestGuidNotNullReturnIt() throws ConfigurationException {
        String expected = "test";
        String result = agentShellSpy.getGuid(expected);

        Assert.assertEquals(expected, result);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getGuidTestGuidIsNullReturnProperty() throws ConfigurationException {
        String expected = "test";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        String result = agentShellSpy.getGuid(null);

        Assert.assertEquals(expected, result);
    }

    @Test
    @PrepareForTest({AgentShell.class, AgentPropertiesFileHandler.class})
    public void getGuidTestGuidAndPropertyAreNullIsDeveloperGenerateNewUuid() throws ConfigurationException {
        String expected = "test";

        PowerMockito.mockStatic(AgentPropertiesFileHandler.class, UUID.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(null, true);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuidMock);
        Mockito.doReturn(expected).when(uuidMock).toString();

        String result = agentShellSpy.getGuid(null);

        Assert.assertEquals(expected, result);
    }

    @Test(expected = ConfigurationException.class)
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void getGuidTestGuidAndPropertyAreNullIsNotDeveloperThrowConfigurationException() throws ConfigurationException {
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(null, false);

        agentShellSpy.getGuid(null);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void setHostTestValueIsNotNullAndStartsAndEndsWithAtSignThrowConfigurationException(){
        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);

        boolean error = false;

        try {
            agentShellSpy.setHost("test");
        } catch (ConfigurationException e) {
            error = true;
        }

        if (!error) {
            throw new AssertionError("This test expects a ConfigurationException.");
        }

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void setHostTestValueIsNullPropertyStartsAndEndsWithAtSignThrowConfigurationException(){
        Mockito.doReturn(true).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn("test");

        boolean error = false;

        try {
            agentShellSpy.setHost(null);
        } catch (ConfigurationException e) {
            error = true;
        }

        if (!error) {
            throw new AssertionError("This test expects a ConfigurationException.");
        }

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class);
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void setHostTestValueIsNotNullAndDoesNotStartAndEndWithAtSignSetHosts() throws ConfigurationException {
        String expected = "test";
        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);

        agentShellSpy.setHost(expected);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class, Mockito.never());
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());

        Mockito.verify(agentShellSpy).setHosts(expected);
    }

    @Test
    @PrepareForTest(AgentPropertiesFileHandler.class)
    public void setHostTestValueIsNullPropertyDoesNotStartAndEndWithAtSignSetHosts() throws ConfigurationException {
        String expected = "test";

        Mockito.doReturn(false).when(agentShellSpy).isValueStartingAndEndingWithAtSign(Mockito.any());
        PowerMockito.mockStatic(AgentPropertiesFileHandler.class);
        PowerMockito.when(AgentPropertiesFileHandler.getPropertyValue(Mockito.any())).thenReturn(expected);

        agentShellSpy.setHost(null);

        PowerMockito.verifyStatic(AgentPropertiesFileHandler.class);
        AgentPropertiesFileHandler.getPropertyValue(Mockito.any());

        Mockito.verify(agentShellSpy).setHosts(expected);
    }
}
