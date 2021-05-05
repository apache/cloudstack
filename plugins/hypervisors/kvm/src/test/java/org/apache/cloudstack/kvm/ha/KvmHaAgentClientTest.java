/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.kvm.ha;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.HostVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.class)
public class KvmHaAgentClientTest {

    private static final int ERROR_CODE = -1;
    private HostVO agent = Mockito.mock(HostVO.class);
    private KvmHaAgentClient kvmHaAgentClient = Mockito.spy(new KvmHaAgentClient(agent));
    private static final int DEFAULT_PORT = 8080;
    private static final String PRIVATE_IP_ADDRESS = "1.2.3.4";
    private static final String JSON_STRING_EXAMPLE_3VMs = "{\"count\":3,\"virtualmachines\":[\"r-123-VM\",\"v-134-VM\",\"s-111-VM\"]}";
    private static final int EXPECTED_RUNNING_VMS_EXAMPLE_3VMs = 3;
    private static final String JSON_STRING_EXAMPLE_0VMs = "{\"count\":0,\"virtualmachines\":[]}";
    private static final int EXPECTED_RUNNING_VMS_EXAMPLE_0VMs = 0;
    private static final String EXPECTED_URL = String.format("http://%s:%d", PRIVATE_IP_ADDRESS, DEFAULT_PORT);
    private static final HttpRequestBase HTTP_REQUEST_BASE = new HttpGet(EXPECTED_URL);
    private static final String VMS_COUNT = "count";
    private static final String VIRTUAL_MACHINES = "virtualmachines";
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final int KVM_HA_WEBSERVICE_PORT = 8080;

    @Mock
    HttpClient client;

    @Mock
    VMInstanceDaoImpl vmInstanceDao;

    @Test
    public void isKvmHaAgentHealthyTestAllGood() {
        boolean result = isKvmHaAgentHealthyTests(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, EXPECTED_RUNNING_VMS_EXAMPLE_3VMs);
        Assert.assertTrue(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestVMsDoNotMatchButDoNotReturnFalse() {
        boolean result = isKvmHaAgentHealthyTests(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, 1);
        Assert.assertTrue(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestExpectedRunningVmsButNoneListed() {
        boolean result = isKvmHaAgentHealthyTests(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, 0);
        Assert.assertFalse(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestReceivedErrorCode() {
        boolean result = isKvmHaAgentHealthyTests(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, ERROR_CODE);
        Assert.assertFalse(result);
    }

    private boolean isKvmHaAgentHealthyTests(int expectedNumberOfVms, int vmsRunningOnAgent) {
        List<VMInstanceVO> vmsOnHostList = new ArrayList<>();
        for (int i = 0; i < expectedNumberOfVms; i++) {
            VMInstanceVO vmInstance = Mockito.mock(VMInstanceVO.class);
            vmsOnHostList.add(vmInstance);
        }

        Mockito.doReturn(vmsOnHostList).when(kvmHaAgentClient).listVmsOnHost(Mockito.any(), Mockito.any());
        Mockito.doReturn(vmsRunningOnAgent).when(kvmHaAgentClient).countRunningVmsOnAgent();

        return kvmHaAgentClient.isKvmHaAgentHealthy(agent, vmInstanceDao);
    }

    @Test
    public void processHttpResponseIntoJsonTestNull() {
        JsonObject responseJson = kvmHaAgentClient.processHttpResponseIntoJson(null);
        Assert.assertNull(responseJson);
    }

    @Test
    public void processHttpResponseIntoJsonTest() throws IOException {
        prepareAndTestProcessHttpResponseIntoJson(JSON_STRING_EXAMPLE_3VMs, 3l);
    }

    @Test
    public void processHttpResponseIntoJsonTestOtherJsonExample() throws IOException {
        prepareAndTestProcessHttpResponseIntoJson(JSON_STRING_EXAMPLE_0VMs, 0l);
    }

    private void prepareAndTestProcessHttpResponseIntoJson(String jsonString, long expectedVmsCount) throws IOException {
        CloseableHttpResponse mockedResponse = mockResponse(HttpStatus.SC_OK, jsonString);
        JsonObject responseJson = kvmHaAgentClient.processHttpResponseIntoJson(mockedResponse);

        Assert.assertNotNull(responseJson);
        JsonElement jsonElementVmsCount = responseJson.get(VMS_COUNT);
        JsonElement jsonElementVmsArray = responseJson.get(VIRTUAL_MACHINES);
        JsonArray jsonArray = jsonElementVmsArray.getAsJsonArray();

        Assert.assertEquals(expectedVmsCount, jsonArray.size());
        Assert.assertEquals(expectedVmsCount, jsonElementVmsCount.getAsLong());
        Assert.assertEquals(jsonString, responseJson.toString());
    }

    private CloseableHttpResponse mockResponse(int httpStatusCode, String jsonString) throws IOException {
        BasicStatusLine basicStatusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1000, 123), httpStatusCode, "Status");
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        InputStream in = IOUtils.toInputStream(jsonString, StandardCharsets.UTF_8);
        Mockito.when(response.getStatusLine()).thenReturn(basicStatusLine);
        HttpEntity httpEntity = new InputStreamEntity(in);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        return response;
    }

    @Test
    public void countRunningVmsOnAgentTest() throws IOException {
        prepareAndRunCountRunningVmsOnAgent(JSON_STRING_EXAMPLE_3VMs, EXPECTED_RUNNING_VMS_EXAMPLE_3VMs);
    }

    @Test
    public void countRunningVmsOnAgentTestBlankNoVmsListed() throws IOException {
        prepareAndRunCountRunningVmsOnAgent(JSON_STRING_EXAMPLE_0VMs, EXPECTED_RUNNING_VMS_EXAMPLE_0VMs);
    }

    private void prepareAndRunCountRunningVmsOnAgent(String jsonStringExample, int expectedListedVms) throws IOException {
        Mockito.when(agent.getPrivateIpAddress()).thenReturn(PRIVATE_IP_ADDRESS);
        Mockito.doReturn(mockResponse(HttpStatus.SC_OK, JSON_STRING_EXAMPLE_3VMs)).when(kvmHaAgentClient).executeHttpRequest(EXPECTED_URL);

        JsonObject jObject = new JsonParser().parse(jsonStringExample).getAsJsonObject();
        Mockito.doReturn(jObject).when(kvmHaAgentClient).processHttpResponseIntoJson(Mockito.any(HttpResponse.class));

        int result = kvmHaAgentClient.countRunningVmsOnAgent();
        Assert.assertEquals(expectedListedVms, result);
    }

    @Test
    public void retryHttpRequestTest() throws IOException {
        kvmHaAgentClient.retryHttpRequest(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        Mockito.verify(client, Mockito.times(1)).execute(Mockito.any());
        Mockito.verify(kvmHaAgentClient, Mockito.times(1)).retryUntilGetsHttpResponse(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    public void retryHttpRequestTestNullResponse() throws IOException {
        Mockito.doReturn(null).when(kvmHaAgentClient).retryUntilGetsHttpResponse(Mockito.anyString(), Mockito.any(), Mockito.any());
        HttpResponse response = kvmHaAgentClient.retryHttpRequest(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        Assert.assertNull(response);
    }

    @Test
    public void retryHttpRequestTestForbidden() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_FORBIDDEN, true);
    }

    @Test
    public void retryHttpRequestTestMultipleChoices() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_MULTIPLE_CHOICES, true);
    }

    @Test
    public void retryHttpRequestTestProcessing() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_PROCESSING, true);
    }

    @Test
    public void retryHttpRequestTestTimeout() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_GATEWAY_TIMEOUT, true);
    }

    @Test
    public void retryHttpRequestTestVersionNotSupported() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED, true);
    }

    @Test
    public void retryHttpRequestTestOk() throws IOException {
        prepareAndRunRetryHttpRequestTest(HttpStatus.SC_OK, false);
    }

    private void prepareAndRunRetryHttpRequestTest(int scMultipleChoices, boolean expectNull) throws IOException {
        HttpResponse mockedResponse = mockResponse(scMultipleChoices, JSON_STRING_EXAMPLE_3VMs);
        Mockito.doReturn(mockedResponse).when(kvmHaAgentClient).retryUntilGetsHttpResponse(Mockito.anyString(), Mockito.any(), Mockito.any());
        HttpResponse response = kvmHaAgentClient.retryHttpRequest(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        if (expectNull) {
            Assert.assertNull(response);
        } else {
            Assert.assertEquals(mockedResponse, response);
        }
    }

    @Test
    public void retryHttpRequestTestHttpOk() throws IOException {
        HttpResponse mockedResponse = mockResponse(HttpStatus.SC_OK, JSON_STRING_EXAMPLE_3VMs);
        Mockito.doReturn(mockedResponse).when(kvmHaAgentClient).retryUntilGetsHttpResponse(Mockito.anyString(), Mockito.any(), Mockito.any());
        HttpResponse result = kvmHaAgentClient.retryHttpRequest(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        Mockito.verify(kvmHaAgentClient, Mockito.times(1)).retryUntilGetsHttpResponse(Mockito.anyString(), Mockito.any(), Mockito.any());
        Assert.assertEquals(mockedResponse, result);
    }

    @Test
    public void retryUntilGetsHttpResponseTestOneIOException() throws IOException {
        Mockito.when(client.execute(HTTP_REQUEST_BASE)).thenThrow(IOException.class).thenReturn(mockResponse(HttpStatus.SC_OK, JSON_STRING_EXAMPLE_3VMs));
        HttpResponse result = kvmHaAgentClient.retryUntilGetsHttpResponse(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        Mockito.verify(client, Mockito.times(MAX_REQUEST_RETRIES)).execute(Mockito.any());
        Assert.assertNotNull(result);
    }

    @Test
    public void retryUntilGetsHttpResponseTestTwoIOException() throws IOException {
        Mockito.when(client.execute(HTTP_REQUEST_BASE)).thenThrow(IOException.class).thenThrow(IOException.class);
        HttpResponse result = kvmHaAgentClient.retryUntilGetsHttpResponse(EXPECTED_URL, HTTP_REQUEST_BASE, client);
        Mockito.verify(client, Mockito.times(MAX_REQUEST_RETRIES)).execute(Mockito.any());
        Assert.assertNull(result);
    }

    @Test
    public void isKvmHaWebserviceEnabledTestDefault() {
        Assert.assertFalse(kvmHaAgentClient.isKvmHaWebserviceEnabled());
    }

    @Test
    public void getKvmHaMicroservicePortValueTestDefault() {
        Assert.assertEquals(KVM_HA_WEBSERVICE_PORT, kvmHaAgentClient.getKvmHaMicroservicePortValue());
    }

}
