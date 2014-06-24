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
package com.cloud.hypervisor.xenserver.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

public class XenServerPoolVms {
    private static final Logger s_logger = Logger.getLogger(XenServerPoolVms.class);
    private final Map<String/* clusterId */, HashMap<String/* vm name */, Pair<String/* host uuid */, State/* vm state */>>> _clusterVms =
        new ConcurrentHashMap<String, HashMap<String, Pair<String, State>>>();

    public HashMap<String, Pair<String, State>> getClusterVmState(String clusterId) {
        HashMap<String, Pair<String, State>> _vms = _clusterVms.get(clusterId);
        if (_vms == null) {
            HashMap<String, Pair<String, State>> vmStates = new HashMap<String, Pair<String, State>>();
            _clusterVms.put(clusterId, vmStates);
            return vmStates;
        } else
            return _vms;
    }

    public void clear(String clusterId) {
        HashMap<String, Pair<String, State>> _vms = getClusterVmState(clusterId);
        _vms.clear();
    }

    public State getState(String clusterId, String name) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        Pair<String, State> pv = vms.get(name);
        return pv == null ? State.Stopped : pv.second(); // if a VM is absent on the cluster, it is effectively in stopped state.
    }

    public Pair<String, State> get(String clusterId, String name) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        return vms.get(name);
    }

    public void put(String clusterId, String hostUuid, String name, State state) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        vms.put(name, new Pair<String, State>(hostUuid, state));
    }

    public void remove(String clusterId, String hostUuid, String name) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        vms.remove(name);
    }

    public void putAll(String clusterId, HashMap<String, Pair<String, State>> newVms) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        vms.putAll(newVms);
    }

    public int size(String clusterId) {
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        return vms.size();
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder("PoolVms=");
        for (HashMap<String/* vm name */, Pair<String/* host uuid */, State/* vm state */>> clusterVM : _clusterVms.values()) {
            for (String vmname : clusterVM.keySet()) {
                sbuf.append(vmname).append("-").append(clusterVM.get(vmname).second()).append(",");
            }
        }
        return sbuf.toString();
    }

}
