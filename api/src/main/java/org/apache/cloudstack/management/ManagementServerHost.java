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
package org.apache.cloudstack.management;

import com.cloud.utils.StringUtils;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface ManagementServerHost extends InternalIdentity, Identity, ControlledEntity {
    enum State {
        Up, Down, PreparingForMaintenance, Maintenance, PreparingForShutDown, ReadyToShutDown, ShuttingDown
    }

    long getMsid();

    State getState();

    String getName();

    String getVersion();

    String getServiceIP();

    /**
     * Returns the node's hostname when set, otherwise falls back to the service IP, so log and
     * alert messages always carry a usable identifier even though the name column is nullable.
     *
     * @return the hostname if not blank, else the service IP
     */
    default String getHostIdentifier() {
        String name = getName();
        return StringUtils.isNotBlank(name) ? name : getServiceIP();
    }
}
