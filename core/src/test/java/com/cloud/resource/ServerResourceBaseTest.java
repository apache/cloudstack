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
package com.cloud.resource;

import com.cloud.utils.net.NetUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.ConfigurationException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RunWith(PowerMockRunner.class)
public class ServerResourceBaseTest {

    private static final String[] NIC_NAME_STARTS_TO_AVOID = {"vnif", "vnbr", "peth", "vif", "virbr"};

    ServerResourceBase serverResourceBaseSpy = Mockito.spy(ServerResourceBase.class);

    NetworkInterface networkInterfaceMock1, networkInterfaceMock2, networkInterfaceMock3, networkInterfaceMock4;

    @Captor
    ArgumentCaptor<String> keyCaptor;

    @Before
    public void setup() {
        networkInterfaceMock1 = PowerMockito.mock(NetworkInterface.class);
        networkInterfaceMock2 = PowerMockito.mock(NetworkInterface.class);
        networkInterfaceMock3 = PowerMockito.mock(NetworkInterface.class);
        networkInterfaceMock4 = PowerMockito.mock(NetworkInterface.class);
    }

    @Test
    @PrepareForTest(ServerResourceBase.class)
    public void isValidNicToUseAsPrivateNicTestReturnFalseWhenNicIsVirtual() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(true);

        Assert.assertFalse(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock));
    }

    @Test
    @PrepareForTest(ServerResourceBase.class)
    public void isValidNicToUseAsPrivateNicTestReturnFalseWhenNicNameStartsWithOneOfTheAvoidList() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(false);
        PowerMockito.when(networkInterfaceMock.getName()).thenReturn("vniftest", "vnbrtest", "pethtest", "viftest", "virbrtest");

        Arrays.asList(NIC_NAME_STARTS_TO_AVOID).forEach(type -> Assert.assertFalse(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock)));
    }

    @Test
    @PrepareForTest(ServerResourceBase.class)
    public void isValidNicToUseAsPrivateNicTestReturnFalseWhenNicNameContainsColon() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(false);
        PowerMockito.when(networkInterfaceMock.getName()).thenReturn("te:st");

        Assert.assertFalse(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock));
    }

    @Test
    @PrepareForTest({ServerResourceBase.class, NetUtils.class})
    public void isValidNicToUseAsPrivateNicTestReturnFalseWhenNetUtilsGetNicParamsReturnsNull() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(false);
        PowerMockito.when(networkInterfaceMock.getName()).thenReturn("testvnif", "testvnbr", "testpeth", "testvif", "testvirbr");

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNicParams(Mockito.anyString())).thenReturn(null);

        Arrays.asList(NIC_NAME_STARTS_TO_AVOID).forEach(type -> {
            Assert.assertFalse(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock));
        });
    }

    @Test
    @PrepareForTest({ServerResourceBase.class, NetUtils.class})
    public void isValidNicToUseAsPrivateNicTestReturnFalseWhenNetUtilsGetNicParamsReturnsFirstElementNull() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(false);
        PowerMockito.when(networkInterfaceMock.getName()).thenReturn("testvnif", "testvnbr", "testpeth", "testvif", "testvirbr");

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNicParams(Mockito.anyString())).thenReturn(new String[]{null});

        Arrays.asList(NIC_NAME_STARTS_TO_AVOID).forEach(type -> {
            Assert.assertFalse(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock));
        });
    }

    @Test
    @PrepareForTest({ServerResourceBase.class, NetUtils.class})
    public void isValidNicToUseAsPrivateNicTestReturnTrueWhenNetUtilsGetNicParamsReturnsAValidFirstElement() {
        NetworkInterface networkInterfaceMock = PowerMockito.mock(NetworkInterface.class);
        PowerMockito.when(networkInterfaceMock.isVirtual()).thenReturn(false);
        PowerMockito.when(networkInterfaceMock.getName()).thenReturn("testvnif", "testvnbr", "testpeth", "testvif", "testvirbr");

        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNicParams(Mockito.anyString())).thenReturn(new String[]{"test"});

        Arrays.asList(NIC_NAME_STARTS_TO_AVOID).forEach(type -> {
            Assert.assertTrue(serverResourceBaseSpy.isValidNicToUseAsPrivateNic(networkInterfaceMock));
        });
    }

    @Test(expected = ConfigurationException.class)
    @PrepareForTest(ServerResourceBase.class)
    public void tryToAutoDiscoverResourcePrivateNetworkInterfaceTestThrowConfigurationExceptionWhenNicsDoesNotHaveMoreElements() throws SocketException, ConfigurationException {
        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenReturn(Collections.enumeration(new ArrayList<>()));

        serverResourceBaseSpy.tryToAutoDiscoverResourcePrivateNetworkInterface();
    }

    @Test(expected = ConfigurationException.class)
    @PrepareForTest(ServerResourceBase.class)
    public void tryToAutoDiscoverResourcePrivateNetworkInterfaceTestThrowConfigurationExceptionWhenNicsGetNetworkInterfacesThrowsSocketException() throws SocketException, ConfigurationException {
        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenThrow(SocketException.class);

        serverResourceBaseSpy.tryToAutoDiscoverResourcePrivateNetworkInterface();
    }

    @Test(expected = ConfigurationException.class)
    @PrepareForTest(ServerResourceBase.class)
    public void tryToAutoDiscoverResourcePrivateNetworkInterfaceTestThrowConfigurationExceptionWhenThereIsNoValidNics() throws SocketException, ConfigurationException {
        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenReturn(Collections.enumeration(Arrays.asList(networkInterfaceMock1, networkInterfaceMock2)));
        Mockito.doReturn(false).when(serverResourceBaseSpy).isValidNicToUseAsPrivateNic(Mockito.any());

        serverResourceBaseSpy.tryToAutoDiscoverResourcePrivateNetworkInterface();

        Mockito.verify(serverResourceBaseSpy, Mockito.times(2)).isValidNicToUseAsPrivateNic(Mockito.any());
    }

    @Test
    @PrepareForTest(ServerResourceBase.class)
    public void tryToAutoDiscoverResourcePrivateNetworkInterfaceTestReturnNic() throws SocketException, ConfigurationException {
        Enumeration<NetworkInterface> interfaces = Collections.enumeration(Arrays.asList(networkInterfaceMock1, networkInterfaceMock2));

        PowerMockito.mockStatic(NetworkInterface.class);
        PowerMockito.when(NetworkInterface.getNetworkInterfaces()).thenReturn(interfaces);
        Mockito.doReturn(false, true).when(serverResourceBaseSpy).isValidNicToUseAsPrivateNic(Mockito.any());

        serverResourceBaseSpy.tryToAutoDiscoverResourcePrivateNetworkInterface();

        Assert.assertEquals(networkInterfaceMock2, serverResourceBaseSpy._privateNic);
        Mockito.verify(serverResourceBaseSpy, Mockito.times(2)).isValidNicToUseAsPrivateNic(Mockito.any());
    }

    @Test
    @PrepareForTest(NetUtils.class)
    public void defineResourceNetworkInterfacesTestUseXenbr0WhenPrivateNetworkInterfaceNotConfigured() {
        Map<String, Object> params = createParamsMap(null, "cloudbr1", "cloudbr2", "cloudbr3");
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNetworkInterface(Mockito.anyString())).thenReturn(networkInterfaceMock1, networkInterfaceMock2, networkInterfaceMock3, networkInterfaceMock4);

        serverResourceBaseSpy.defineResourceNetworkInterfaces(params);

        verifyAndAssertNetworkInterfaces("xenbr0", "cloudbr1", "cloudbr2", "cloudbr3");
    }

    @Test
    @PrepareForTest(NetUtils.class)
    public void defineResourceNetworkInterfacesTestUseXenbr1WhenPublicNetworkInterfaceNotConfigured() {
        Map<String, Object> params = createParamsMap("cloudbr0", null, "cloudbr2", "cloudbr3");
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNetworkInterface(Mockito.anyString())).thenReturn(networkInterfaceMock1, networkInterfaceMock2, networkInterfaceMock3, networkInterfaceMock4);

        serverResourceBaseSpy.defineResourceNetworkInterfaces(params);

        verifyAndAssertNetworkInterfaces("cloudbr0", "xenbr1", "cloudbr2", "cloudbr3");
    }

    @Test
    @PrepareForTest(NetUtils.class)
    public void defineResourceNetworkInterfacesTestUseConfiguredNetworkInterfaces() {
        Map<String, Object> params = createParamsMap("cloudbr0", "cloudbr1", "cloudbr2", "cloudbr3");
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.getNetworkInterface(Mockito.anyString())).thenReturn(networkInterfaceMock1, networkInterfaceMock2, networkInterfaceMock3, networkInterfaceMock4);

        serverResourceBaseSpy.defineResourceNetworkInterfaces(params);

        verifyAndAssertNetworkInterfaces("cloudbr0", "cloudbr1", "cloudbr2", "cloudbr3");
    }

    private Map<String, Object> createParamsMap(String... params) {
        Map<String, Object> result = new HashMap<>();
        result.put("private.network.device", params[0]);
        result.put("public.network.device", params[1]);
        result.put("storage.network.device", params[2]);
        result.put("storage.network.device.2", params[3]);
        return result;
    }

    private void verifyAndAssertNetworkInterfaces(String... expectedResults) {
        PowerMockito.verifyStatic(NetUtils.class, Mockito.times(4));
        NetUtils.getNetworkInterface(keyCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();

        for (int i = 0; i < expectedResults.length; i++) {
            Assert.assertEquals(expectedResults[i], keys.get(i));
        }

        Assert.assertEquals(networkInterfaceMock1, serverResourceBaseSpy._privateNic);
        Assert.assertEquals(networkInterfaceMock2, serverResourceBaseSpy._publicNic);
        Assert.assertEquals(networkInterfaceMock3, serverResourceBaseSpy._storageNic);
        Assert.assertEquals(networkInterfaceMock4, serverResourceBaseSpy._storageNic2);
    }
}
