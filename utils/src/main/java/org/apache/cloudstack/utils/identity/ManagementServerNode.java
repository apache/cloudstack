//
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
//

package org.apache.cloudstack.utils.identity;



import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.exception.CloudRuntimeException;

// Holds the management server ID and shares it with other CloudStack components
public class ManagementServerNode extends AdapterBase implements SystemIntegrityChecker {

    // Load the ID once and keep the same value for the life of this process
    private static final long s_nodeId = ManagementServerId.get();

    // Register this component to run early during CloudStack startup
    public ManagementServerNode() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK_BOOTSTRAP);
    }

    // A valid server ID must be greater than zero
    @Override
    public void check() {
        if (s_nodeId <= 0) {
            throw new CloudRuntimeException("Unable to get the management server node id");
        }
    }

    // Other components call this to get the same server ID
    public static long getManagementServerId() {
        return s_nodeId;
    }

    // Check the ID during startup. Log an error and stop if the check fails
    @Override
    public boolean start() {
        try {
            check();
        } catch (Exception e) {
            logger.error("System integrity check exception", e);
            System.exit(1);
        }
        return true;
    }
}
