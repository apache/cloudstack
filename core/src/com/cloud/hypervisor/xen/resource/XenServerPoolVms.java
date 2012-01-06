/*  Copyright (C) 2012 Citrix.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.hypervisor.xen.resource;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

 
public class XenServerPoolVms {
    private static final Logger s_logger = Logger.getLogger(XenServerPoolVms.class);
    private HashMap<String/* clusterId */, HashMap<String/* vm name */, Pair<String/* host uuid */, State/* vm state */>>> _cluster_vms = 
        new HashMap<String, HashMap<String, Pair<String, State>>>();

    public HashMap<String, Pair<String, State>> getClusterVmState(String clusterId){
        HashMap<String, Pair<String, State>> _vms= _cluster_vms.get(clusterId);
        if (_vms==null) {
            HashMap<String, Pair<String, State>> vmStates =  new HashMap<String, Pair<String, State>>();
            _cluster_vms.put(clusterId, vmStates);
            return vmStates;
        }
        else return _vms;
    }
    
    public void clear(String clusterId){
        HashMap<String, Pair<String, State>> _vms= getClusterVmState(clusterId);
        _vms.clear();
    }
    
    public State getState(String clusterId, String name){
        HashMap<String, Pair<String, State>> vms = getClusterVmState(clusterId);
        Pair<String, State> pv = vms.get(name);
        return pv == null ? State.Stopped : pv.second(); // if a VM is absent on the cluster, it is effectively in stopped state.
    }

    public void put(String clusterId, String hostUuid, String name, State state){
        HashMap<String, Pair<String, State>> vms= getClusterVmState(clusterId);
        vms.put(name, new Pair<String, State>(hostUuid, state));
    }
    
    public void remove(String clusterId, String hostUuid, String name){
        HashMap<String, Pair<String, State>> vms= getClusterVmState(clusterId);
        vms.remove(name);
    }
    
    public void putAll(String clusterId, HashMap<String, Pair<String, State>> new_vms){
        HashMap<String, Pair<String, State>> vms= getClusterVmState(clusterId);
        vms.putAll(new_vms);
    }
    
    public int size(String clusterId){
        HashMap<String, Pair<String, State>> vms= getClusterVmState(clusterId);
        return vms.size();
    }
    
    @Override
    public String toString(){
        StringBuilder sbuf = new StringBuilder("PoolVms=");
        for (HashMap<String/* vm name */, Pair<String/* host uuid */, State/* vm state */>>  clusterVM: _cluster_vms.values()){
            for (String vmname: clusterVM.keySet()){
                sbuf.append(vmname).append("-").append(clusterVM.get(vmname).second()).append(",");
            }
        }
        return sbuf.toString();
    }
    
}

