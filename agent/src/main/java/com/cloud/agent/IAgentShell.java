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
package com.cloud.agent;

import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import com.cloud.resource.ServerResource;
import com.cloud.utils.backoff.BackoffAlgorithm;

public interface IAgentShell {
    String hostLbAlgorithmSeparator = "@";

    Map<String, Object> getCmdLineProperties();

    Properties getProperties();

    String getPersistentProperty(String prefix, String name);

    void setPersistentProperty(String prefix, String name, String value);

    String getNextHost();

    String getPrivateIp();

    int getPort();

    int getWorkers();

    int getProxyPort();

    String getGuid();

    String getZone();

    String getPod();

    BackoffAlgorithm getBackoffAlgorithm();

    int getPingRetries();

    String getVersion();

    void setHosts(String hosts);

    void resetHostCounter();

    String[] getHosts();

    long getLbCheckerInterval(Long receivedLbInterval);

    void updateConnectedHost();

    String getConnectedHost();

    void launchNewAgent(ServerResource resource) throws ConfigurationException;
}
