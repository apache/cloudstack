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
package com.cloud.hypervisor.xen.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.cloud.utils.Ternary;
import com.cloud.vm.VirtualMachine.State;

public class XenServerPoolVms {
    private static final Logger s_logger = Logger.getLogger(XenServerPoolVms.class);
    private final Map<String/* clusterId */, HashMap<String/* vm name */, Ternary<String/* host uuid */, State/* vm state */, String/* PV driver version*/>>> _cluster_vms =
        new ConcurrentHashMap<String, HashMap<String, Ternary<String, State, String>>>();

    public HashMap<String, Ternary<String, State, String>> getClusterVmState(String clusterId) {
        HashMap<String, Ternary<String, State, String>> _vms = _cluster_vms.get(clusterId);
        if (_vms == null) {
            HashMap<String, Ternary<String, State, String>> vmStates = new HashMap<String, Ternary<String, State, String>>();
            _cluster_vms.put(clusterId, vmStates);
            return vmStates;
        } else
            return _vms;
    }

    public void clear(String clusterId) {
        HashMap<String, Ternary<String, State, String>> _vms = getClusterVmState(clusterId);
        _vms.clear();
    }

    public State getState(String clusterId, String name) {
        HashMap<String, Ternary<String, State, String>> vms = getClusterVmState(clusterId);
        Ternary<String, State, String> pv = vms.get(name);
        return pv == null ? State.Stopped : pv.second(); // if a VM is absent on the cluster, it is effectively in stopped state.
    }

    public void put(String clusterId, String hostUuid, String name, State state, String platform){
        HashMap<String, Ternary<String, State, String>> vms= getClusterVmState(clusterId);
        vms.put(name, new Ternary<String, State, String>(hostUuid, state, platform));
    }

    public void put(String clusterId, String hostUuid, String name, State state) {
        HashMap<String, Ternary<String, State, String>> vms = getClusterVmState(clusterId);
        vms.put(name, new Ternary<String, State, String>(hostUuid, state, null));
    }

    public void remove(String clusterId, String hostUuid, String name) {
        HashMap<String, Ternary<String, State, String>> vms = getClusterVmState(clusterId);
        vms.remove(name);
    }

    public void putAll(String clusterId, HashMap<String, Ternary<String, State, String>> new_vms) {
        HashMap<String, Ternary<String, State, String>> vms = getClusterVmState(clusterId);
        vms.putAll(new_vms);
    }

    public int size(String clusterId) {
        HashMap<String, Ternary<String, State, String>> vms = getClusterVmState(clusterId);
        return vms.size();
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder("PoolVms=");
        for (HashMap<String/* vm name */, Ternary<String/* host uuid */, State/* vm state */, String>> clusterVM : _cluster_vms.values()) {
            for (String vmname : clusterVM.keySet()) {
                sbuf.append(vmname).append("-").append(clusterVM.get(vmname).second()).append(",");
            }
        }
        return sbuf.toString();
    }

}
