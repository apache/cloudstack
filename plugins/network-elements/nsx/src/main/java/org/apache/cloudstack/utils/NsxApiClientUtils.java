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
package org.apache.cloudstack.utils;

import com.vmware.vapi.bindings.StubConfiguration;
import com.vmware.vapi.cis.authn.SecurityContextFactory;
import com.vmware.vapi.client.ApiClient;
import com.vmware.vapi.client.ApiClients;
import com.vmware.vapi.client.Configuration;
import com.vmware.vapi.core.ExecutionContext.SecurityContext;
import com.vmware.vapi.internal.protocol.RestProtocol;
import com.vmware.vapi.internal.protocol.client.rest.authn.BasicAuthenticationAppender;
import com.vmware.vapi.protocol.HttpConfiguration;
import org.apache.log4j.Logger;

public class NsxApiClientUtils {
    private static final Logger S_LOGGER = Logger.getLogger(NsxApiClientUtils.class);
    public static ApiClient apiClient = null;
    public static final int RESPONSE_TIMEOUT_SECONDS = 60;

    public enum PoolAllocation {
        ROUTING,
        LB_SMALL,
        LB_MEDIUM,
        LB_LARGE,
        LB_XLARGE
    }

    public enum HAMode {
        ACTIVE_STANDBY,
        ACTIVE_ACTIVE
    }
    public static ApiClient createApiClient(String hostname, String port, String username, char[] password) {
        String controllerUrl = String.format("https://%s:%s", hostname, port);
        HttpConfiguration.SslConfiguration.Builder sslConfigBuilder = new HttpConfiguration.SslConfiguration.Builder();
        sslConfigBuilder
                .disableCertificateValidation()
                .disableHostnameVerification();
        HttpConfiguration.SslConfiguration sslConfig = sslConfigBuilder.getConfig();

        HttpConfiguration httpConfig = new HttpConfiguration.Builder()
                .setSoTimeout(RESPONSE_TIMEOUT_SECONDS * 1000)
                .setSslConfiguration(sslConfig).getConfig();

        StubConfiguration stubConfig = new StubConfiguration();
        SecurityContext securityContext = SecurityContextFactory
                .createUserPassSecurityContext(username, password);
        stubConfig.setSecurityContext(securityContext);

        Configuration.Builder configBuilder = new Configuration.Builder()
                .register(Configuration.HTTP_CONFIG_CFG, httpConfig)
                .register(Configuration.STUB_CONFIG_CFG, stubConfig)
                .register(RestProtocol.REST_REQUEST_AUTHENTICATOR_CFG, new BasicAuthenticationAppender());
        Configuration config = configBuilder.build();
        apiClient = ApiClients.newRestClient(controllerUrl, config);

        return apiClient;
    }
}
