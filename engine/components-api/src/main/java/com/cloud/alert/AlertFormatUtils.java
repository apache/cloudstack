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
package com.cloud.alert;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.host.Host;

/**
 * Shared formatting for the host/zone/pod description that recurs, independently
 * hand-rolled and inconsistently worded (and occasionally mislabelled), across the
 * HA and agent-management alert call sites. See CLOUDSTACK-7297.
 */
public final class AlertFormatUtils {

    private AlertFormatUtils() {
    }

    public static String describeHostLocation(Host host, DataCenter zone, Pod pod) {
        if (host == null) {
            // we should never get here, but if we do, at least we won't get an NPE
            return String.format("No host to describe for availability zone: %s, pod: %s",
                    zone != null ? zone.getName() : "unknown",
                    pod != null ? pod.getName() : "unknown");
        }
        return String.format("name: %s (id: %d, uuid: %s), availability zone: %s, pod: %s",
                host.getName(), host.getId(), host.getUuid(),
                zone != null ? zone.getName() : "unknown",
                pod != null ? pod.getName() : "unknown");
    }
}
