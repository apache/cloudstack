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

package com.cloud.agent.api;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * Unmanaged instances are those virtual machines which are present at hypervisor end but not available in CloudStack.
 * Such virtual machines might have been created out of band, directly from hypervisor.
 * Managed instances will be those virtual machines that are created from CloudStack or CloudStack have database record for them.
 * All managed instances will be filtered while trying to find unmanaged instances.
 */

@LogLevel(LogLevel.Log4jLevel.Trace)
public class GetUnmanagedInstancesCommand extends Command {

    String instanceName;
    List<String> managedInstancesNames;

    public GetUnmanagedInstancesCommand() {
    }

    public GetUnmanagedInstancesCommand(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public List<String> getManagedInstancesNames() {
        return managedInstancesNames;
    }

    public void setManagedInstancesNames(List<String> managedInstancesNames) {
        this.managedInstancesNames = managedInstancesNames;
    }

    public boolean hasManagedInstance(String name) {
        if (CollectionUtils.isNotEmpty(managedInstancesNames)) {
            return managedInstancesNames.contains(name);
        }
        return false;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getString() {
        return "GetUnmanagedInstancesCommand [instanceName=" + instanceName + "]";
    }
}
