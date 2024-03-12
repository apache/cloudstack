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

package org.apache.cloudstack.network.contrail.model;

import java.io.IOException;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;


import com.cloud.exception.InternalErrorException;

public class InstanceIpModel extends ModelObjectBase {

    private String _name;
    private String _uuid;

    private String _ipAddress;

    private VMInterfaceModel _vmiModel;

    public InstanceIpModel(String vmName, int deviceId) {
        _name = vmName + '-' + deviceId;
    }

    public void addToVMInterface(VMInterfaceModel vmiModel) {
        _vmiModel = vmiModel;
        if (vmiModel != null) {
            vmiModel.addSuccessor(this);
            logger.debug("vmiModel has " + vmiModel.successors().size() + " IP addresses");
        }
    }

    @Override
    public int compareTo(ModelObject o) {
        InstanceIpModel other;
        try {
            other = (InstanceIpModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return InstanceIpModel.class.getName().compareTo(clsname);
        }
        return _name.compareTo(other._name);
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        if (_uuid != null) {
            api.delete(InstanceIp.class, _uuid);
        }
        _uuid = null;
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
    }

    public String getAddress() {
        return _ipAddress;
    }

    public String getName() {
        return _name;
    }

    public void setAddress(String ipaddress) {
        _ipAddress = ipaddress;
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {
        assert _vmiModel != null;

        ApiConnector api = controller.getApiAccessor();
        VirtualNetworkModel vnModel = _vmiModel.getVirtualNetworkModel();
        assert vnModel != null;

        VirtualMachineInterface vmi = _vmiModel.getVMInterface();
        VirtualNetwork vnet = vnModel.getVirtualNetwork();
        if (vnet == null) {
            vnet = (VirtualNetwork)api.findById(VirtualNetwork.class, _vmiModel.getNetworkUuid());
        }

        String ipid = api.findByName(InstanceIp.class, null, _name);
        if (ipid == null) {
            InstanceIp ip_obj = new InstanceIp();
            ip_obj.setName(_name);
            ip_obj.setVirtualNetwork(vnet);
            if (_ipAddress != null) {
                ip_obj.setAddress(_ipAddress);
            }
            ip_obj.setVirtualMachineInterface(vmi);
            if (!api.create(ip_obj)) {
                throw new InternalErrorException("Unable to create instance-ip " + _name);
            }
            api.read(ip_obj);
            _uuid = ip_obj.getUuid();
            if (_ipAddress == null) {
                if (!api.read(ip_obj)) {
                    throw new InternalErrorException("Unable to read instance-ip " + _name);
                }
            }
            _ipAddress = ip_obj.getAddress();
        } else {
            // Ensure that the instance-ip has the correct value and is pointing at the VMI.
            InstanceIp ip_obj = (InstanceIp)api.findById(InstanceIp.class, ipid);
            if (ip_obj == null) {
                throw new InternalErrorException("Unable to read instance-ip " + _name);
            }
            boolean update = false;
            String ipnet_id = ObjectReference.getReferenceListUuid(ip_obj.getVirtualNetwork());
            if (ipnet_id == null || !ipnet_id.equals(_vmiModel.getNetworkUuid())) {
                ip_obj.setVirtualNetwork(vnet);
                update = true;
            }

            if (_ipAddress != null && !ip_obj.getAddress().equals(_ipAddress)) {
                ip_obj.setAddress(_ipAddress);
                update = true;
            }

            String vmi_id = ObjectReference.getReferenceListUuid(ip_obj.getVirtualMachineInterface());
            if (vmi_id == null || !vmi_id.equals(_vmiModel.getUuid())) {
                if (vmi != null) {
                    ip_obj.setVirtualMachineInterface(vmi);
                    update = true;
                }
            }

            if (update && !api.update(ip_obj)) {
                throw new InternalErrorException("Unable to update instance-ip: " + ip_obj.getName());
            }
            api.read(ip_obj);
            _uuid = ip_obj.getUuid();
            _ipAddress = ip_obj.getAddress();
        }
    }

    @Override
    public boolean verify(ModelController controller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject current) {
        // TODO Auto-generated method stub
        return false;
    }

}
