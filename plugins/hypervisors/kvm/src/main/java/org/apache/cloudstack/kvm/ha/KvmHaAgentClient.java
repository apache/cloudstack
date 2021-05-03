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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a client that checks Agent status via a webserver.
 *
 * The additional webserver exposes a simple JSON API which returns a list
 * of Virtual Machines that are running on that host according to libvirt.
 *
 * This way, KVM HA can verify, via libvirt, VMs status with a HTTP-call
 * to this simple webserver and determine if the host is actually down
 * or if it is just the Java Agent which has crashed.
 */
public class KvmHaAgentClient {

    private static final Logger LOGGER = Logger.getLogger(KvmHaAgentClient.class);
    private static final int ERROR_CODE = -1;
    private static final String EXPECTED_HTTP_STATUS = "2XX";
    private static final String VM_COUNT = "count";
    private static final int WAIT_FOR_REQUEST_RETRY = 2;
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final int CAUTIOUS_MARGIN_OF_VMS_ON_HOST = 1;
    private Host agent;

    /**
     * Instantiates a webclient that checks, via a webserver running on the KVM host, the VMs running
     */
    public KvmHaAgentClient(Host agent) {
        this.agent = agent;
    }

    /**
     *  Returns the number of VMs running on the KVM host according to libvirt.
     */
    protected int countRunningVmsOnAgent() {
        String url = String.format("http://%s:%d", agent.getPrivateIpAddress(), getKvmHaMicroservicePortValue());
        HttpResponse response = executeHttpRequest(url);

        if (response == null)
            return ERROR_CODE;

        JsonObject responseInJson = processHttpResponseIntoJson(response);
        if (responseInJson == null) {
            return ERROR_CODE;
        }

        return responseInJson.get(VM_COUNT).getAsInt();
    }

    protected int getKvmHaMicroservicePortValue() {
        Integer haAgentPort = KVMHAConfig.KvmHaWebservicePort.value();
        if (haAgentPort == null) {
            LOGGER.warn(String.format("Using default kvm.ha.webservice.port: %s as it was set to NULL for the cluster [id: %d] from %s.", KVMHAConfig.KvmHaWebservicePort.defaultValue(), agent.getClusterId(), agent));
            haAgentPort = Integer.parseInt(KVMHAConfig.KvmHaWebservicePort.defaultValue());
        }
        return haAgentPort;
    }

    /**
     * Checks if the KVM HA Webservice is enabled or not; if disabled then CloudStack ignores HA validation via the webservice.
     */
    public boolean isKvmHaWebserviceEnabled() {
        return KVMHAConfig.IsKvmHaWebserviceEnabled.value();
    }

    /**
     * Lists VMs on host according to vm_instance DB table. The states considered for such listing are: 'Running', 'Stopping', 'Migrating'.
     * <br>
     * <br>
     * Note that VMs on state 'Starting' are not common to be at the host, therefore this method does not list them.
     * However, there is still a probability of a VM in 'Starting' state be already listed on the KVM via '$virsh list',
     * but that's not likely and thus it is not relevant for this very context.
     */
    protected List<VMInstanceVO> listVmsOnHost(Host host, VMInstanceDao vmInstanceDao) {
        List<VMInstanceVO> listByHostAndStateRunning = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Running);
        List<VMInstanceVO> listByHostAndStateStopping = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Stopping);
        List<VMInstanceVO> listByHostAndStateMigrating = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Migrating);

        List<VMInstanceVO> listByHostAndState = new ArrayList<>();
        listByHostAndState.addAll(listByHostAndStateRunning);
        listByHostAndState.addAll(listByHostAndStateStopping);
        listByHostAndState.addAll(listByHostAndStateMigrating);

        if (LOGGER.isTraceEnabled()) {
            List<VMInstanceVO> listByHostAndStateStarting = vmInstanceDao.listByHostAndState(host.getId(), VirtualMachine.State.Starting);
            int startingVMs = listByHostAndStateStarting.size();
            int runningVMs = listByHostAndStateRunning.size();
            int stoppingVms = listByHostAndStateStopping.size();
            int migratingVms = listByHostAndStateMigrating.size();
            int countRunningVmsOnAgent = countRunningVmsOnAgent();
            LOGGER.trace(
                    String.format("%s has (%d Starting) %d Running, %d Stopping, %d Migrating. Total listed via DB %d / %d (via libvirt)", agent.getName(), startingVMs, runningVMs, stoppingVms,
                            migratingVms, listByHostAndState.size(), countRunningVmsOnAgent));
        }

        return listByHostAndState;
    }

    /**
     *  Returns true in case of the expected number of VMs matches with the VMs running on the KVM host according to Libvirt. <br><br>
     *
     *  IF: <br>
     *  (i) KVM HA agent finds 0 running but CloudStack considers that the host has 2 or more VMs running: returns false as could not find VMs running but it expected at least
     *    2 VMs running, fencing/recovering host would avoid downtime to VMs in this case.<br>
     *  (ii) KVM HA agent finds 0 VM running but CloudStack considers that the host has 1 VM running: return true and log WARN messages and avoids triggering HA recovery/fencing
     *    when it could be a inconsistency when migrating a VM.<br>
     *  (iii) amount of listed VMs is different than expected: return true and print WARN messages so Admins can monitor and react accordingly
     */
    public boolean isKvmHaAgentHealthy(Host host, VMInstanceDao vmInstanceDao) {
        int numberOfVmsOnHostAccordingToDB = listVmsOnHost(host, vmInstanceDao).size();
        int numberOfVmsOnAgent = countRunningVmsOnAgent();
        if (numberOfVmsOnAgent < 0) {
            LOGGER.error(String.format("KVM HA Agent health check failed, either the KVM Agent %s is unreachable or Libvirt validation failed.", agent));
            LOGGER.warn(String.format("Host %s is not considered healthy and HA fencing/recovering process might be triggered.", agent.getName(), numberOfVmsOnHostAccordingToDB));
            return false;
        }
        if (numberOfVmsOnHostAccordingToDB == numberOfVmsOnAgent) {
            return true;
        }
        if (numberOfVmsOnAgent == 0 && numberOfVmsOnHostAccordingToDB > CAUTIOUS_MARGIN_OF_VMS_ON_HOST) {
            // Return false as could not find VMs running but it expected at least one VM running, fencing/recovering host would avoid downtime to VMs in this case.
            // There is cautious margin added on the conditional. This avoids fencing/recovering hosts when there is one VM migrating to a host that had zero VMs.
            // If there are more VMs than the CAUTIOUS_MARGIN_OF_VMS_ON_HOST) the Host should be treated as not healthy and fencing/recovering process might be triggered.
            LOGGER.warn(String.format("KVM HA Agent %s could not find VMs; it was expected to list %d VMs.", agent, numberOfVmsOnHostAccordingToDB));
            LOGGER.warn(String.format("Host %s is not considered healthy and HA fencing/recovering process might be triggered.", agent.getName(), numberOfVmsOnHostAccordingToDB));
            return false;
        }
        // In order to have a less "aggressive" health-check, the KvmHaAgentClient will not return false; fencing/recovering could bring downtime to existing VMs
        // Additionally, the inconsistency can also be due to jobs in progress to migrate/stop/start VMs
        // Either way, WARN messages should be presented to Admins so they can look closely to what is happening on the host
        LOGGER.warn(String.format("KVM HA Agent %s listed %d VMs; however, it was expected %d VMs.", agent, numberOfVmsOnAgent, numberOfVmsOnHostAccordingToDB));
        return true;
    }

    /**
     * Executes a GET request for the given URL address.
     */
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
            retryHttpRequest(url, httpReq, client);
        }
        return response;
    }

    @Nullable
    private HttpGet prepareHttpRequestForUrl(String url) {
        HttpGet httpReq = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            httpReq = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            LOGGER.error(String.format("Failed to create URI for GET request [URL: %s] due to exception.", url), e);
            return null;
        }
        return httpReq;
    }

    /**
     * Re-executes the HTTP GET request until it gets a response or it reaches the maximum request retries {@link #MAX_REQUEST_RETRIES}
     */
    protected HttpResponse retryHttpRequest(String url, HttpRequestBase httpReq, HttpClient client) {
        LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s]. Executing the request again.", httpReq.getMethod(), url));
        HttpResponse response = retryUntilGetsHttpResponse(url, httpReq, client);

        if (response == null) {
            LOGGER.error(String.format("Failed to execute HTTP %s request [URL: %s].", httpReq.getMethod(), url));
            return response;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            throw new CloudRuntimeException(
                    String.format("Failed to get VMs information with a %s request to URL '%s'. The expected HTTP status code is '%s' but it got '%s'.", HttpGet.METHOD_NAME, url,
                            EXPECTED_HTTP_STATUS, statusCode));
        }

        LOGGER.debug(String.format("Successfully executed HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        return response;
    }

    protected HttpResponse retryUntilGetsHttpResponse(String url, HttpRequestBase httpReq, HttpClient client) {
        for (int attempt = 1; attempt < MAX_REQUEST_RETRIES + 1; attempt++) {
            try {
                TimeUnit.SECONDS.sleep(WAIT_FOR_REQUEST_RETRY);
                LOGGER.debug(String.format("Retry HTTP %s request [URL: %s], attempt %d/%d.", httpReq.getMethod(), url, attempt, MAX_REQUEST_RETRIES));
                return client.execute(httpReq);
            } catch (IOException | InterruptedException e) {
                if (attempt == MAX_REQUEST_RETRIES) {
                    throw new CloudRuntimeException(String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, MAX_REQUEST_RETRIES, url, e));
                } else {
                    LOGGER.error(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, MAX_REQUEST_RETRIES,
                                    url, e));
                }
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
    protected JsonObject processHttpResponseIntoJson(HttpResponse response) {
        InputStream in;
        String jsonString;
        if (response == null) {
            return null;
        }
        try {
            in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            jsonString = streamReader.readLine();
        } catch (UnsupportedOperationException | IOException e) {
            throw new CloudRuntimeException("Failed to process response", e);
        }

        return new JsonParser().parse(jsonString).getAsJsonObject();
    }
}
