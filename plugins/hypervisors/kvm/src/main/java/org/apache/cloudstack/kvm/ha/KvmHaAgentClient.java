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

import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a client that checks Agent status via a webserver.
 * <br>
 * The additional webserver exposes a simple JSON API which returns a list
 * of Virtual Machines that are running on that host according to Libvirt.
 * <br>
 * This way, KVM HA can verify, via Libvirt, VMs status with an HTTP-call
 * to this simple webserver and determine if the host is actually down
 * or if it is just the Java Agent which has crashed.
 */
public class KvmHaAgentClient {

    private static final Logger LOGGER = Logger.getLogger(KvmHaAgentClient.class);
    private static final int ERROR_CODE = -1;
    private static final String EXPECTED_HTTP_STATUS = "2XX";
    private static final String VM_COUNT = "count";
    private static final String STATUS = "status";
    private static final String CHECK_NEIGHBOUR = "check-neighbour";
    private static final int WAIT_FOR_REQUEST_RETRY = 2;
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final JsonParser JSON_PARSER = new JsonParser();

    @Inject
    private VMInstanceDao vmInstanceDao;

    /**
     *  Returns the number of VMs running on the KVM host according to Libvirt.
     */
    public int countRunningVmsOnAgent(Host host) {
        String url = String.format("http://%s:%d", host.getPrivateIpAddress(), getKvmHaMicroservicePortValue(host));
        HttpResponse response = executeHttpRequest(url);

        if (response == null)
            return ERROR_CODE;

        JsonObject responseInJson = processHttpResponseIntoJson(response);
        if (responseInJson == null) {
            return ERROR_CODE;
        }

        return responseInJson.get(VM_COUNT).getAsInt();
    }

    protected int getKvmHaMicroservicePortValue(Host host) {
        Integer haAgentPort = KVMHAConfig.KvmHaWebservicePort.value();
        if (haAgentPort == null) {
            LOGGER.warn(String.format("Using default kvm.ha.webservice.port: %s as it was set to NULL for the cluster [id: %d] from %s.",
                    KVMHAConfig.KvmHaWebservicePort.defaultValue(), host.getClusterId(), host));
            haAgentPort = Integer.parseInt(KVMHAConfig.KvmHaWebservicePort.defaultValue());
        }
        return haAgentPort;
    }

    /**
     * Lists VMs on host according to vm_instance DB table. The states considered for such listing are: 'Running', 'Stopping', 'Migrating'.
     * <br>
     * <br>
     * Note that VMs on state 'Starting' are not common to be at the host, therefore this method does not list them.
     * However, there is still a probability of a VM in 'Starting' state be already listed on the KVM via '$virsh list',
     * but that's not likely and thus it is not relevant for this very context.
     */
    public List<VMInstanceVO> listVmsOnHost(Host host) {
        List<VMInstanceVO> listByHostAndStates = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Running, VirtualMachine.State.Stopping, VirtualMachine.State.Migrating);

        if (LOGGER.isTraceEnabled()) {
            List<VMInstanceVO> listByHostAndStateStarting = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Starting);
            int startingVMs = listByHostAndStateStarting.size();
            long runningVMs = listByHostAndStates.stream().filter(vm -> vm.getState() == VirtualMachine.State.Running).count();
            long stoppingVms = listByHostAndStates.stream().filter(vm -> vm.getState() == VirtualMachine.State.Stopping).count();
            long migratingVms = listByHostAndStates.stream().filter(vm -> vm.getState() == VirtualMachine.State.Migrating).count();
            int countRunningVmsOnAgent = countRunningVmsOnAgent(host);
            LOGGER.trace(
                    String.format("%s has (%d Starting) %d Running, %d Stopping, %d Migrating. Total listed via DB %d / %d (via libvirt)", host.getName(), startingVMs, runningVMs,
                            stoppingVms, migratingVms, listByHostAndStates.size(), countRunningVmsOnAgent));
        }

        return listByHostAndStates;
    }

    /**
     *  Sends HTTP GET request from the host executing the KVM HA Agent webservice to a target Host (expected to also be running the KVM HA Agent).
     *  The webserver serves a JSON Object such as {"status": "Up"} if the request gets a HTTP_OK OR {"status": "Down"} if HTTP GET failed
     */
    public boolean isHostReachableByNeighbour(Host neighbour, Host target) {
        String neighbourHostAddress = neighbour.getPrivateIpAddress();
        String targetHostAddress = target.getPrivateIpAddress();
        int port = getKvmHaMicroservicePortValue(neighbour);
        String url = String.format("http://%s:%d/%s/%s:%d", neighbourHostAddress, port, CHECK_NEIGHBOUR, targetHostAddress, port);
        HttpResponse response = executeHttpRequest(url);

        if (response == null)
            return false;

        JsonObject responseInJson = processHttpResponseIntoJson(response);
        if (responseInJson == null)
            return false;

        int statusCode = response.getStatusLine().getStatusCode();
        if (isHttpStatusCodNotOk(statusCode)) {
            LOGGER.error(
                    String.format("Failed HTTP %s Request %s; the expected HTTP status code is '%s' but it got '%s'.", HttpGet.METHOD_NAME, url, EXPECTED_HTTP_STATUS, statusCode));
            return false;
        }

        String hostStatusFromJson = responseInJson.get(STATUS).getAsString();
        return Status.Up.toString().equals(hostStatusFromJson);
    }

    protected boolean isHttpStatusCodNotOk(int statusCode) {
        return statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES;
    }

    /**
     * Executes a GET request for the given URL address.
     */
    @Nullable
    protected HttpResponse executeHttpRequest(String url) {
        HttpGet httpReq = prepareHttpRequestForUrl(url);
        if (httpReq == null) {
            return null;
        }

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = null;
        try {
            response = client.execute(httpReq);
        } catch (IOException e) {
            if (MAX_REQUEST_RETRIES == 0) {
                LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s] due to exception %s.", httpReq.getMethod(), url, e), e);
                return null;
            }
            response = retryHttpRequest(url, httpReq, client);
        }
        return response;
    }

    @Nullable
    private HttpGet prepareHttpRequestForUrl(String url) {
        try {
            URIBuilder builder = new URIBuilder(url);
            return new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            LOGGER.error(String.format("Failed to create URI for GET request [URL: %s] due to exception.", url), e);
            return null;
        }
    }

    /**
     * Re-executes the HTTP GET request until it gets a response or it reaches the maximum request retries {@link #MAX_REQUEST_RETRIES}.
     */
    @Nullable
    protected HttpResponse retryHttpRequest(String url, HttpRequestBase httpReq, HttpClient client) {
        LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s]. Executing the request again.", httpReq.getMethod(), url));
        HttpResponse response = retryUntilGetsHttpResponse(url, httpReq, client);

        if (response == null) {
            LOGGER.error(String.format("Failed to execute HTTP %s request [URL: %s].", httpReq.getMethod(), url));
            return response;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (isHttpStatusCodNotOk(statusCode)) {
            LOGGER.error(
                    String.format("Failed to get VMs information with a %s request to URL '%s'. The expected HTTP status code is '%s' but it got '%s'.", HttpGet.METHOD_NAME, url,
                            EXPECTED_HTTP_STATUS, statusCode));
            return null;
        }

        LOGGER.debug(String.format("Successfully executed HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        return response;
    }

    /**
     * Retry HTTP Request until success or it reaches {@link #MAX_REQUEST_RETRIES} retries. It can return null.
     */
    @Nullable
    protected HttpResponse retryUntilGetsHttpResponse(String url, HttpRequestBase httpReq, HttpClient client) {
        for (int attempt = 1; attempt <= MAX_REQUEST_RETRIES; attempt++) {
            try {
                TimeUnit.SECONDS.sleep(WAIT_FOR_REQUEST_RETRY);
                LOGGER.debug(String.format("Retry HTTP %s request [URL: %s], attempt %d/%d.", httpReq.getMethod(), url, attempt, MAX_REQUEST_RETRIES));
                return client.execute(httpReq);
            } catch (IOException | InterruptedException e) {
                String errorMessage = String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s",
                        httpReq.getMethod(), attempt, MAX_REQUEST_RETRIES, url, e);
                LOGGER.error(errorMessage, e);
            }
        }
        return null;
    }

    /**
     * Processes the response of request GET System ID as a JSON object.<br>
     * Json example: {"count": 3, "virtualmachines": ["r-123-VM", "v-134-VM", "s-111-VM"]}<br><br>
     *
     * Note: this method can return NULL JsonObject in case HttpResponse is NULL.
     */
    @Nullable
    protected JsonObject processHttpResponseIntoJson(HttpResponse response) {
        if (response == null) {
            return null;
        }
        try {
            InputStream in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return JSON_PARSER.parse(streamReader.readLine()).getAsJsonObject();
        } catch (UnsupportedOperationException | IOException e) {
            throw new CloudRuntimeException("Failed to process response", e);
        }
    }
}
