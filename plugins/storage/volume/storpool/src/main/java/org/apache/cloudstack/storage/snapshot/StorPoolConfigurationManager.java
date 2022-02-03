/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.snapshot;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public class StorPoolConfigurationManager implements Configurable {

    public static final ConfigKey<Boolean> BypassSecondaryStorage = new ConfigKey<Boolean>(Boolean.class, "sp.bypass.secondary.storage", "Advanced", "false",
            "For StorPool Managed storage backup to secondary", true, ConfigKey.Scope.Global, null);
    public static final ConfigKey<String> StorPoolClusterId = new ConfigKey<String>(String.class, "sp.cluster.id", "Advanced", "n/a",
                                    "For StorPool multi cluster authorization", true, ConfigKey.Scope.Cluster, null);

    @Override
    public String getConfigComponentName() {
        return StorPoolConfigurationManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { BypassSecondaryStorage, StorPoolClusterId };
    }
}
