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

package org.apache.cloudstack.config;

import com.cloud.exception.InvalidParameterValueException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiServiceConfigurationTest {

    private static final String LOCALHOST = "http://localhost";

    private static final String ENDPOINT_URL = "https://acs.clouds.com/client/api";

    private static final String WHITE_SPACE = " ";

    private static final String BLANK_STRING = "";

    private static final String NULL_STRING = null;

    private static final String LOCALHOST_IP = "127.0.0.1";

    @Test(expected = InvalidParameterValueException.class)
    public void validateEndpointUrlTestIfEndpointUrlContainLocalhostShouldThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(LOCALHOST);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }

    @Test
    public void validateEndpointUrlTestIfEndpointUrlContainLocalhostShouldNotThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(ENDPOINT_URL);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateEndpointUrlTestIfEndpointUrlIsNullShouldThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(NULL_STRING);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateEndpointUrlTestIfEndpointUrlIsBlankShouldThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(BLANK_STRING);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateEndpointUrlTestIfEndpointUrlIsWhiteSpaceShouldThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(WHITE_SPACE);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateEndpointUrlTestIfEndpointUrlContainLocalhostIpShouldThrowInvalidParameterValueException() {
        try (MockedStatic<ApiServiceConfiguration> apiServiceConfigurationMockedStatic = Mockito.mockStatic(ApiServiceConfiguration.class)) {
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::getApiServletPathValue).thenReturn(LOCALHOST_IP);
            apiServiceConfigurationMockedStatic.when(ApiServiceConfiguration::validateEndpointUrl).thenCallRealMethod();
            ApiServiceConfiguration.validateEndpointUrl();
        }
    }
}
