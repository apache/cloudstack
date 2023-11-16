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
package org.apache.cloudstack.storage.datastore.util;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public class LinstorConfigurationManager implements Configurable
{
    public static final ConfigKey<Boolean> BackupSnapshots = new ConfigKey<>(Boolean.class, "lin.backup.snapshots", "Advanced", "false",
        "Backup Linstor primary storage snapshots to secondary storage (deleting ps snapshot), only works on hyperconverged setups.", true, ConfigKey.Scope.Global, null);

    public static final ConfigKey<?>[] CONFIG_KEYS = new ConfigKey<?>[] { BackupSnapshots };

    @Override
    public String getConfigComponentName()
    {
        return LinstorConfigurationManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys()
    {
        return CONFIG_KEYS;
    }
}
