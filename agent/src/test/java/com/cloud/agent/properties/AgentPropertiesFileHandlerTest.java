/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.agent.properties;

import com.cloud.utils.PropertiesUtil;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AgentPropertiesFileHandlerTest extends TestCase {

    @Mock
    AgentProperties.Property<String> agentPropertiesStringMock;

    @Mock
    AgentProperties.Property<Integer> agentPropertiesIntegerMock;

    @Mock
    AgentProperties.Property<Long> agentPropertiesLongMock;

    @Mock
    File fileMock;

    @Mock
    Properties propertiesMock;

    MockedStatic<PropertiesUtil> propertiesUtilMocked;

    @Override
    @Before
    public void setUp() throws Exception {
        propertiesUtilMocked = Mockito.mockStatic(PropertiesUtil.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        propertiesUtilMocked.close();
    }

    @Test
    public void getPropertyValueTestFileNotFoundReturnDefaultValueNull() throws Exception{
        String expectedResult = null;

        AgentProperties.Property<String> agentPropertiesStringMock = new AgentProperties.Property<String>("Test-null", null, String.class);

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(null);

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueFileNotFoundReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(null);

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueLoadFromFileThrowsIOExceptionReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenThrow(new IOException());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValuePropertyIsEmptyReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        propertiesUtilMocked.when(() -> propertiesMock.getProperty(Mockito.anyString())).thenReturn("");

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValuePropertyIsNullReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        propertiesUtilMocked.when(() -> propertiesMock.getProperty(Mockito.anyString())).thenReturn(null);

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueValidPropertyReturnPropertyValue() throws Exception{
        String expectedResult = "test";
        Mockito.doReturn("default value").when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        Mockito.doReturn(expectedResult).when(propertiesMock).getProperty(Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void getPropertyValueTestValidPropertyReturnPropertyValueWhenDefaultValueIsNull() throws Exception {
        String expectedResult = "test";

        AgentProperties.Property<String> agentPropertiesStringMock = new AgentProperties.Property<String>("Test-null", null, String.class);

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        Mockito.doReturn(expectedResult).when(propertiesMock).getProperty(Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueValidIntegerPropertyReturnPropertyValue() throws Exception{
        Integer expectedResult = 2;
        Mockito.doReturn(1).when(agentPropertiesIntegerMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesIntegerMock).getName();
        Mockito.doReturn(Integer.class).when(agentPropertiesIntegerMock).getTypeClass();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        Mockito.doReturn(String.valueOf(expectedResult)).when(propertiesMock).getProperty(Mockito.anyString());

        Integer result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesIntegerMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void getPropertyValueTestValidLongPropertyReturnPropertyValue() throws Exception{
        Long expectedResult = 600L;
        Mockito.doReturn(1L).when(agentPropertiesLongMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesLongMock).getName();
        Mockito.doReturn(Long.class).when(agentPropertiesLongMock).getTypeClass();

        propertiesUtilMocked.when(() -> PropertiesUtil.findConfigFile(Mockito.anyString())).thenReturn(fileMock);
        propertiesUtilMocked.when(() -> PropertiesUtil.loadFromFile(Mockito.any())).thenReturn(propertiesMock);
        Mockito.doReturn(String.valueOf(expectedResult)).when(propertiesMock).getProperty(Mockito.anyString());

        Long result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesLongMock);

        Assert.assertEquals(expectedResult, result);
    }

}
