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
package com.cloud.consoleproxy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

@RunWith(PowerMockRunner.class)
public class ConsoleProxyHttpHandlerHelperTest {

    @Mock
    ConsoleProxyPasswordBasedEncryptor encryptor;

    @Test
    @PrepareForTest({ConsoleProxy.class, ConsoleProxyHttpHandlerHelper.class})
    public void testQueryMapExtraParameter() throws Exception {
        PowerMockito.mockStatic(ConsoleProxy.class);
        PowerMockito.when(ConsoleProxy.getEncryptorPassword()).thenReturn("password");
        PowerMockito.whenNew(ConsoleProxyPasswordBasedEncryptor.class).withArguments(Mockito.anyString()).thenReturn(encryptor);
        Mockito.when(encryptor.decryptObject(Mockito.eq(ConsoleProxyClientParam.class), Mockito.anyString())).thenReturn(null);

        String extraValidationToken = "test-token";
        String query = String.format("token=SOME_TOKEN&extra=%s", extraValidationToken);

        Map<String, String> queryMap = ConsoleProxyHttpHandlerHelper.getQueryMap(query);
        Assert.assertTrue(queryMap.containsKey("extra"));
        Assert.assertEquals(extraValidationToken, queryMap.get("extra"));
    }
}
