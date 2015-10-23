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

import org.apache.cloudstack.network.contrail.management.ContrailManager;
import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.network.Network;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualMachineInterfacePropertiesType;

public class VMInterfaceModel extends ModelObjectBase {
    private static final Logger s_logger = Logger.getLogger(VMInterfaceModel.class);

    private String _uuid;

    /**
     * properties
     */
    private String _vmName;
    private int _deviceId;
    private boolean _netActive;
    private boolean _nicActive;
    private String _serviceTag;
    private String _networkId;
    private String _macAddress;

    /**
     * cached objects
     */
    private VirtualMachineModel _vmModel;
    private VirtualNetworkModel _vnModel;
    private VirtualMachineInterface _vmi;

    public VMInterfaceModel(String uuid) {
        _uuid = uuid;
    }

    public void addToVirtualMachine(VirtualMachineModel vmModel) {
        _vmModel = vmModel;
        if (vmModel != null) {
            vmModel.addSuccessor(this);
        }
    }

    public void addToVirtualNetwork(VirtualNetworkModel vnModel) {
        _vnModel = vnModel;
        if (vnModel != null) {
            vnModel.addSuccessor(this);
        }
    }

    public void build(ModelController controller, VMInstanceVO instance, NicVO nic) throws IOException {
        setProperties(controller, instance, nic);

        InstanceIpModel ipModel = getInstanceIp();
        String ipAddress = nic.getIPv4Address();
        if (ipAddress != null) {
            if (ipModel == null) {
                ipModel = new InstanceIpModel(_vmName, _deviceId);
                ipModel.addToVMInterface(this);
            }
            ipModel.setAddress(ipAddress);
        } else if (ipModel != null) {
            removeSuccessor(ipModel);
        }

        _macAddress = nic.getMacAddress();
    }

    @Override
    public int compareTo(ModelObject o) {
        VMInterfaceModel other;
        try {
            other = (VMInterfaceModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return VMInterfaceModel.class.getName().compareTo(clsname);
        }
        return _uuid.compareTo(other._uuid);
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        for (ModelObject successor : successors()) {
            successor.delete(controller);
        }

        ApiConnector api = controller.getApiAccessor();
        api.delete(VirtualMachineInterface.class, _uuid);
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
        delete(controller);

        for (ModelObject successor : successors()) {
            successor.destroy(controller);
        }
        clearSuccessors();
    }

    public InstanceIpModel getInstanceIp() {
        for (ModelObject successor : successors()) {
            if (successor.getClass() == InstanceIpModel.class) {
                return (InstanceIpModel)successor;
            }
        }
        return null;
    }

    public String getNetworkUuid() {
        return _networkId;
    }

    public VirtualNetworkModel getVirtualNetworkModel() {
        return _vnModel;
    }

    public String getUuid() {
        return _uuid;
    }

    public VirtualMachineInterface getVMInterface() {
        return _vmi;
    }

    public void setProperties(ModelController controller, VMInstanceVO instance, NicVO nic) throws IOException {
        _vmName = instance.getInstanceName();
        _deviceId = nic.getDeviceId();
        Network network = controller.getNetworkDao().findById(nic.getNetworkId());

        switch (nic.getState()) {
            case Allocated:
            case Reserved:
                _nicActive = true;
                break;
            default:
                _nicActive = false;
                break;
        }

        switch (network.getState()) {
            case Implemented:
            case Setup:
                _netActive = true;
                break;
            default:
                _netActive = false;
                break;
        }
        assert _vnModel != null;
        _networkId = _vnModel.getUuid();
    }

    public void setActive() {
        _nicActive = true;
    }

    void setServiceTag(String tag) {
        _serviceTag = tag;
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {
        if (!_netActive || !_nicActive) {
            s_logger.debug("vm interface update, _netActive: " + _netActive + ", _nicActive: " + _nicActive);
            delete(controller);
            return;
        }
        if (_vmModel == null) {
            throw new InternalErrorException("virtual-machine not set on VMI: " + _uuid);
        }
        if (_vnModel == null) {
            throw new InternalErrorException("virtual-network not set on VMI: " + _uuid);
        }
        ContrailManager manager = controller.getManager();
        ApiConnector api = controller.getApiAccessor();

        VirtualMachineInterface vmi = (VirtualMachineInterface)api.findById(VirtualMachineInterface.class, _uuid);
        boolean create = false;
        if (vmi == null) {
            create = true;
            vmi = new VirtualMachineInterface();
            vmi.setParent(_vmModel.getVirtualMachine());
            vmi.setName(manager.getVifNameByVmName(_vmModel.getInstanceName(), _deviceId));
            vmi.setUuid(_uuid);
            vmi.setVirtualNetwork(_vnModel.getVirtualNetwork());
        } else {
            // Do not try to update VMI to routing-instance references. These are managed by schema-transformer.
            vmi.clearRoutingInstance();
        }
        _vmi = vmi;
        if (_macAddress != null) {
            MacAddressesType mac = new MacAddressesType();
            mac.addMacAddress(_macAddress);
            vmi.setMacAddresses(mac);
        }

        if (_serviceTag != null) {
            vmi.setProperties(new VirtualMachineInterfacePropertiesType(_serviceTag, null));
        }

        if (create) {
            if (!api.create(vmi)) {
                throw new InternalErrorException("Unable to create virtual-machine-interface " + _uuid);
            }
        } else {
            if (!api.update(vmi)) {
                throw new InternalErrorException("Unable to update virtual-machine-interface " + _uuid);
            }
        }

        api.read(vmi);

        int ipCount = 0;
        for (ModelObject successor : successors()) {
            if (successor.getClass() == InstanceIpModel.class) {
                ipCount++;
            }
            successor.update(controller);
        }
        // TODO: if there are no instance-ip successors present and we have an instance-ip object reference
        // delete the object.
        if (ipCount == 0) {
            s_logger.warn("virtual-machine-interface " + _uuid + " has no instance-ip");
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
        return true;
    }

}
