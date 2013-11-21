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

package org.apache.cloudstack.network.contrail.management;

import java.util.TreeSet;

import org.apache.cloudstack.network.contrail.model.ModelObjectBase;
import org.apache.cloudstack.network.contrail.model.ServiceInstanceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;

import com.cloud.network.Networks.TrafficType;

public class ModelDatabase {
    TreeSet<ServiceInstanceModel> _serviceInstanceTable;
    TreeSet<VirtualMachineModel> _vmTable;
    TreeSet<VirtualNetworkModel> _vnTable;

    ModelDatabase() {
        initDb();
    }

    public void initDb() {
        _serviceInstanceTable = new TreeSet<ServiceInstanceModel>(new ModelObjectBase.UuidComparator());
        _vmTable = new TreeSet<VirtualMachineModel>(new ModelObjectBase.UuidComparator());
        _vnTable = new TreeSet<VirtualNetworkModel>(new ModelObjectBase.UuidComparator());
    }

    public TreeSet<ServiceInstanceModel> getServiceInstances() {
        return _serviceInstanceTable;
    }

    public ServiceInstanceModel lookupServiceInstance(String uuid) {
        ServiceInstanceModel siKey = new ServiceInstanceModel(uuid);
        ServiceInstanceModel current = _serviceInstanceTable.ceiling(siKey);
        if (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }

    public TreeSet<VirtualMachineModel> getVirtualMachines() {
        return _vmTable;
    }

    public VirtualMachineModel lookupVirtualMachine(String uuid) {
        VirtualMachineModel vmKey = new VirtualMachineModel(null, uuid);
        VirtualMachineModel current = _vmTable.ceiling(vmKey);
        if (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }

    public TreeSet<VirtualNetworkModel> getVirtualNetworks() {
        return _vnTable;
    }

    public VirtualNetworkModel lookupVirtualNetwork(String uuid, String name, TrafficType ttype) {
        VirtualNetworkModel vnKey = new VirtualNetworkModel(null, uuid, name, ttype);
        VirtualNetworkModel current = _vnTable.ceiling(vnKey);
        if (current != null) {
            if (ttype == TrafficType.Management || ttype == TrafficType.Storage || ttype == TrafficType.Control) {
                if (current.getName().equals(name)) {
                    return current;
                }
            } else if (current.getUuid().equals(uuid)) {
                return current;
            }
        }
        return null;
    }
}
