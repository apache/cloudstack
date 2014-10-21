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

import com.cloud.utils.backoff.BackoffAlgorithm;

public interface IAgentShell {
    public Map<String, Object> getCmdLineProperties();

    public Properties getProperties();

    public String getPersistentProperty(String prefix, String name);

    public void setPersistentProperty(String prefix, String name, String value);

    public String getHost();

    public String getPrivateIp();

    public int getPort();

    public int getWorkers();

    public int getProxyPort();

    public String getGuid();

    public String getZone();

    public String getPod();

    public BackoffAlgorithm getBackoffAlgorithm();

    public int getPingRetries();

    public String getVersion();
}
