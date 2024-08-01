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
import net.juniper.contrail.api.types.FloatingIp;


import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.exception.InternalErrorException;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;

public class FloatingIpModel extends ModelObjectBase {

    private String _uuid;
    private long _id;
    private String _name;
    private String _addr;
    private boolean _initialized;

    /*
     * cached API server objects
     */
    private FloatingIp _fip;
    private FloatingIpPoolModel _fipPoolModel;

    public FloatingIpModel(String uuid) {
        _uuid = uuid;
    }

    public void addToFloatingIpPool(FloatingIpPoolModel fipPoolModel) {
        _fipPoolModel = fipPoolModel;
        if (fipPoolModel != null) {
            fipPoolModel.addSuccessor(this);
        }
    }

    public void addToVMInterface(VMInterfaceModel vmiModel) {
        if (vmiModel != null) {
            vmiModel.addSuccessor(this);
        }
    }

    /*
     * Resynchronize internal state from the cloudstack DB object.
     */
    public void build(ModelController controller, PublicIpAddress ip) {
        setProperties(controller, ip);
    }

    @Override
    public int compareTo(ModelObject o) {
        FloatingIpModel other;
        try {
            other = (FloatingIpModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return FloatingIpModel.class.getName().compareTo(clsname);
        }

        return _uuid.compareTo(other._uuid);
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        for (ModelObject successor : successors()) {
            successor.delete(controller);
        }

        try {
            api.delete(FloatingIp.class, _uuid);
        } catch (IOException ex) {
            logger.warn("floating ip delete", ex);
        }
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
        delete(controller);

        for (ModelObject successor : successors()) {
            successor.destroy(controller);
        }
        clearSuccessors();
    }

    public String getName() {
        return _name;
    }

    public String getUuid() {
        return _uuid;
    }

    public FloatingIp getFloatingIp() {
        return _fip;
    }

    /**
     * Initialize the object properties based on the DB object.
     * Common code between plugin calls and DBSync.
     */
    public void setProperties(ModelController controller, PublicIpAddress ip) {
        _uuid = ip.getUuid();
        _name = Long.toString(ip.getId());
        _addr = ip.getAddress().addr();
        _id = ip.getId();
        assert _fipPoolModel != null : "floating ip uuid is not set";
        _initialized = true;
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {

        assert _initialized;

        ApiConnector api = controller.getApiAccessor();
        ContrailManager manager = controller.getManager();
        FloatingIp fip = _fip;

        if (_fip == null) {
            _fip = fip = (FloatingIp)controller.getApiAccessor().findById(FloatingIp.class, _uuid);
            if (fip == null) {
                fip = new FloatingIp();
                fip.setUuid(_uuid);
                fip.setAddress(_addr);
                fip.setName(_name);
                fip.setParent(_fipPoolModel.getFloatingIpPool());
            }
        }

        IPAddressVO ipAddrVO = controller.getIPAddressDao().findById(_id);
        assert ipAddrVO != null : "can not find address object in db";
        Long vmId = ipAddrVO.getAssociatedWithVmId();
        Long networkId = ipAddrVO.getAssociatedWithNetworkId();
        if (vmId == null || networkId == null) {
            logger.debug("Floating ip is not yet associated to either vm or network");
            return;
        }
        NicVO nic = controller.getNicDao().findByNtwkIdAndInstanceId(networkId, vmId);
        assert nic != null : "can not find nic for the given network and vm in db";

        VMInstanceVO vm = controller.getVmDao().findById(vmId);
        assert vm != null : "can not find vm in db";

        VirtualMachineModel vmModel = manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        assert vmModel != null : "can not find vm model";

        VMInterfaceModel vmiModel = vmModel.getVMInterface(nic.getUuid());
        assert vmiModel != null && vmiModel.getVMInterface() != null : "can not find virtual machine interface";

        fip.setVirtualMachineInterface(vmiModel.getVMInterface());

        if (_fip == null) {
            try {
                api.create(fip);
            } catch (Exception ex) {
                logger.debug("floating ip create", ex);
                throw new CloudRuntimeException("Failed to create floating ip", ex);
            }
            _fip = fip;
        } else {
            try {
                api.update(fip);
            } catch (IOException ex) {
                logger.warn("floating ip update", ex);
                throw new CloudRuntimeException("Unable to update floating ip object", ex);
            }
        }

        addToVMInterface(vmiModel);

        for (ModelObject successor : successors()) {
            successor.update(controller);
        }
    }

    @Override
    public boolean verify(ModelController controller) {
        assert _initialized : "initialized is false";
        return false;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject o) {
        return true;
    }
}
