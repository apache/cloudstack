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
package org.apache.cloudstack.framework.config;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.utils.db.EntityManager;

public class ScopedConfigValue<T> extends ConfigValue<T> {
    public T getValueForScope(long scopeId) {
        // TODO: In order to complete this the details for zone, pod, cluster
        // needs to have interfaces.  Then you can use the EntityManager to
        // retrieve those information.
        Class<? extends Grouping> scope = _config.scope();
        if (scope == DataCenter.class) {
        } else if (scope == Pod.class) {

        } else if (scope == Cluster.class) {

        }
        return null;
    }
    
    protected ScopedConfigValue(EntityManager entityMgr, ConfigKey<T> key) {
        super(entityMgr, key);
    }
}
