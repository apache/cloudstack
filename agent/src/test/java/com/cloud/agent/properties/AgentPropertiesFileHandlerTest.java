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
import org.apache.commons.beanutils.ConvertUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesUtil.class, ConvertUtils.class})
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

    @Test
    public void getPropertyValueTestFileNotFoundReturnDefaultValueNull() throws Exception{
        String expectedResult = null;

        AgentProperties.Property<String> agentPropertiesStringMock = new AgentProperties.Property<String>("Test-null", null, String.class);

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(null).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueFileNotFoundReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(null).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueLoadFromFileThrowsIOExceptionReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doThrow(new IOException()).when(PropertiesUtil.class, "loadFromFile", Mockito.any());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValuePropertyIsEmptyReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
        PowerMockito.doReturn("").when(propertiesMock).getProperty(Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValuePropertyIsNullReturnDefaultValue() throws Exception{
        String expectedResult = "default value";
        Mockito.doReturn(expectedResult).when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
        PowerMockito.doReturn(null).when(propertiesMock).getProperty(Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetPropertyValueValidPropertyReturnPropertyValue() throws Exception{
        String expectedResult = "test";
        Mockito.doReturn("default value").when(agentPropertiesStringMock).getDefaultValue();
        Mockito.doReturn("name").when(agentPropertiesStringMock).getName();

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
        Mockito.doReturn(expectedResult).when(propertiesMock).getProperty(Mockito.anyString());

        String result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesStringMock);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void getPropertyValueTestValidPropertyReturnPropertyValueWhenDefaultValueIsNull() throws Exception {
        String expectedResult = "test";

        AgentProperties.Property<String> agentPropertiesStringMock = new AgentProperties.Property<String>("Test-null", null, String.class);

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
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

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
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

        PowerMockito.mockStatic(PropertiesUtil.class);
        PowerMockito.doReturn(fileMock).when(PropertiesUtil.class, "findConfigFile", Mockito.anyString());
        PowerMockito.doReturn(propertiesMock).when(PropertiesUtil.class, "loadFromFile", Mockito.any());
        Mockito.doReturn(String.valueOf(expectedResult)).when(propertiesMock).getProperty(Mockito.anyString());

        Long result = AgentPropertiesFileHandler.getPropertyValue(agentPropertiesLongMock);

        Assert.assertEquals(expectedResult, result);
    }

}
