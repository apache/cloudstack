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
package com.cloud.vm;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHost;

public class ClusteredVirtualMachineManagerImpl extends VirtualMachineManagerImpl implements ClusterManagerListener {

    @Inject
    ClusterManager _clusterMgr;

    protected ClusteredVirtualMachineManagerImpl() {
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {

    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (ManagementServerHost node : nodeList) {
            cancelWorkItems(node.getMsid());
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        super.configure(name, xmlParams);

        _clusterMgr.registerListener(this);

        return true;
    }

}
