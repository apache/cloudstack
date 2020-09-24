//
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
//
package org.apache.cloudstack.utils.redfish;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class) public class RedfishClientTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String oobAddress = "oob.host.address";
    private static final String systemId = "SystemID.1";
    private final static String COMPUTER_SYSTEM_RESET_URL_PATH = "/Actions/ComputerSystem.Reset";

    RedfishClient redfishClientspy = Mockito.spy(new RedfishClient(USERNAME, PASSWORD, true, true));

    @Test(expected = RedfishException.class)
    public void validateAddressAndPrepareForUrlTestExpect() {
        redfishClientspy.validateAddressAndPrepareForUrl("1:1:2:3:1");
        redfishClientspy.validateAddressAndPrepareForUrl("1");
        redfishClientspy.validateAddressAndPrepareForUrl("hostname");
        redfishClientspy.validateAddressAndPrepareForUrl(oobAddress);
    }

    @Test
    public void validateAddressAndPrepareForUrlTestDomainName() {
        String result = redfishClientspy.validateAddressAndPrepareForUrl(oobAddress);
        Assert.assertEquals(oobAddress, result);
    }

    @Test
    public void validateAddressAndPrepareForUrlTestIpv4() {
        String ipv4 = "192.168.0.123";
        String result = redfishClientspy.validateAddressAndPrepareForUrl(ipv4);
        Assert.assertEquals(ipv4, result);
    }

    @Test
    public void validateAddressAndPrepareForUrlTestIpv6() {
        String ipv6 = "100::ffff:ffff:ffff:ffff";
        String expected = "[" + ipv6 + "]";
        String result = redfishClientspy.validateAddressAndPrepareForUrl(ipv6);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestHttpsGetSystemId() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, true, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.GetSystemId, systemId);
        String expected = String.format("https://%s/redfish/v1/Systems/", oobAddress, systemId);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestGetSystemId() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, false, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.GetSystemId, systemId);
        String expected = String.format("http://%s/redfish/v1/Systems/", oobAddress, systemId);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestHttpsComputerSystemReset() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, true, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.ComputerSystemReset, systemId);
        String expected = String.format("https://%s/redfish/v1/Systems/%s%s", oobAddress, systemId, COMPUTER_SYSTEM_RESET_URL_PATH);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestComputerSystemReset() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, false, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.ComputerSystemReset, systemId);
        String expected = String.format("http://%s/redfish/v1/Systems/%s%s", oobAddress, systemId, COMPUTER_SYSTEM_RESET_URL_PATH);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestHttpsGetPowerState() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, true, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.GetPowerState, systemId);
        String expected = String.format("https://%s/redfish/v1/Systems/%s", oobAddress, systemId);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void buildRequestUrlTestGetPowerState() {
        RedfishClient redfishclient = new RedfishClient(USERNAME, PASSWORD, false, false);
        String result = redfishclient.buildRequestUrl(oobAddress, RedfishClient.RedfishCmdType.GetPowerState, systemId);
        String expected = String.format("http://%s/redfish/v1/Systems/%s", oobAddress, systemId);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getSystemPowerStateTest() {
        Mockito.doReturn(systemId).when(redfishClientspy).getSystemId(Mockito.anyString());
        mockResponse(HttpStatus.SC_OK);
        RedfishClient.RedfishPowerState expectedState = RedfishClient.RedfishPowerState.On;
        Mockito.doReturn(expectedState).when(redfishClientspy).processGetSystemRequestResponse(Mockito.any(CloseableHttpResponse.class));

        RedfishClient.RedfishPowerState result = redfishClientspy.getSystemPowerState(oobAddress);

        Assert.assertEquals(expectedState, result);
    }

    @Test(expected = RedfishException.class)
    public void getSystemPowerStateTestHttpStatusNotOk() {
        Mockito.doReturn(systemId).when(redfishClientspy).getSystemId(Mockito.anyString());
        mockResponse(HttpStatus.SC_BAD_REQUEST);
        redfishClientspy.getSystemPowerState(oobAddress);
    }

    private CloseableHttpResponse mockResponse(int httpStatusCode) {
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.doReturn(httpStatusCode).when(statusLine).getStatusCode();
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        Mockito.doReturn(statusLine).when(response).getStatusLine();
        Mockito.doReturn(response).when(redfishClientspy).executeGetRequest(Mockito.anyString());
        return response;
    }

    @Test
    public void getSystemIdTest() {
        CloseableHttpResponse mockedResponse = mockResponse(HttpStatus.SC_OK);
        Mockito.doReturn(mockedResponse).when(redfishClientspy).executeGetRequest(Mockito.anyString());
        Mockito.doReturn(systemId).when(redfishClientspy).processGetSystemIdResponse(Mockito.any(CloseableHttpResponse.class));

        String result = redfishClientspy.getSystemId(oobAddress);

        Assert.assertEquals(systemId, result);
    }

    @Test(expected = RedfishException.class)
    public void getSystemIdTestHttpStatusNotOk() {
        CloseableHttpResponse mockedResponse = mockResponse(HttpStatus.SC_UNAUTHORIZED);
        Mockito.doReturn(mockedResponse).when(redfishClientspy).executeGetRequest(Mockito.anyString());
        redfishClientspy.getSystemId(oobAddress);
    }

}
