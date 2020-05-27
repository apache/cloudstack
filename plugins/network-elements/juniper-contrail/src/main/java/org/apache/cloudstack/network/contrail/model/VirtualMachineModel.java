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
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.ServiceInstance;
import net.juniper.contrail.api.types.VirtualMachine;

import org.apache.cloudstack.network.contrail.management.ContrailManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class VirtualMachineModel extends ModelObjectBase {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineModel.class);

    private final String _uuid;
    private long _instanceId;

    /*
     * current state for object properties
     */
    private boolean _initialized;
    private boolean _active;
    private String _serviceUuid;
    private String _instanceName;
    private String _projectId;

    /*
     * cached API server objects
     */
    private VirtualMachine _vm;
    private ServiceInstanceModel _serviceModel;

    public VirtualMachineModel(VMInstanceVO vm, String uuid) {
        _uuid = uuid;
        if (vm != null) {
            _instanceId = vm.getId();
            _instanceName = vm.getInstanceName();
        }
    }

    /**
     * Resynchronize internal state from the cloudstack DB object.
     * @param instance
     */
    public void build(ModelController controller, VMInstanceVO instance) {
        setProperties(controller, instance);
        UserVm userVm = controller.getVmDao().findById(instance.getId());
        if (userVm != null && userVm.getUserData() != null) {
            s_logger.debug("vm " + instance.getInstanceName() + " user data: " + userVm.getUserData());
            final Gson json = new Gson();
            Map<String, String> kvmap = json.fromJson(userVm.getUserData(), new TypeToken<Map<String, String>>() {
            }.getType());
            //Renamed "data" to "serviceUuid" because it's clearer.
            String serviceUuid = kvmap.get("service-instance");
            if (serviceUuid != null) {
                /*
                 * UUID.fromString() does not validate an UUID properly. I tried, for example, informing less digits in the UUID, where 12 were expected,
                 * and the UUID.fromstring() did not thrown the exception as expected. However, if you try UUID.fromString("aaa") it breaks, but if you try
                 * UUID.fromString("3dd4fa6e-2899-4429-b818-d34fe8df5") it doesn't (the last portion should have 12, instead of 9 digits).
                 *
                 * In other fix I added the validate UUID method to the UuidUtil classes.
                 */
                if (UuidUtils.validateUUID(serviceUuid)) {
                    /* link the object with the service instance */
                    buildServiceInstance(controller, serviceUuid);
                } else {
                    // Throw a CloudRuntimeException in case the UUID is not valid.
                    String message = "Invalid UUID ({0}) given for the service-instance for VM {1}.";
                    message = MessageFormat.format(message, instance.getId(), serviceUuid);
                    s_logger.warn(message);
                    throw new CloudRuntimeException(message);
                }
            }
        }
    }

    /**
     * Link the virtual machine with the service instance when recovering state from database.
     *
     * @param controller
     * @param serviceUuid
     */
    private void buildServiceInstance(ModelController controller, String serviceUuid) {
        ContrailManager manager = controller.getManager();
        ApiConnector api = controller.getApiAccessor();
        _serviceUuid = serviceUuid;

        ServiceInstance siObj;
        try {
            siObj = (ServiceInstance) api.findById(ServiceInstance.class, serviceUuid);
        } catch (IOException ex) {
            s_logger.warn("service-instance read", ex);
            throw new CloudRuntimeException("Unable to read service-instance object", ex);
        }

        ServiceInstanceModel siModel;
        String fqn = StringUtils.join(siObj.getQualifiedName(), ':');
        siModel = manager.getDatabase().lookupServiceInstance(fqn);
        if (siModel == null) {
            siModel = new ServiceInstanceModel(serviceUuid);
            siModel.build(controller, siObj);
            manager.getDatabase().getServiceInstances().add(siModel);
        }
        /*
         * The code that was under the ELSE was never executed and due to that has been removed.
         * Also, in the case siObj was null, it was going pass it as parameter to the build() method in the
         * siModel object.
         */
        _serviceModel = siModel;
    }

    @Override
    public int compareTo(ModelObject o) {
        VirtualMachineModel other;
        try {
            other = (VirtualMachineModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return VirtualMachineModel.class.getName().compareTo(clsname);
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
            api.delete(VirtualMachine.class, _uuid);
        } catch (IOException ex) {
            s_logger.warn("virtual-machine delete", ex);
        }

        if (_serviceModel != null) {
            _serviceModel.delete(controller);
        }
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
        delete(controller);

        for (ModelObject successor : successors()) {
            successor.destroy(controller);
        }

        clearSuccessors();

        if (_serviceModel != null) {
            _serviceModel.removeSuccessor(this);
            _serviceModel.destroy(controller);
            ContrailManager manager = controller.getManager();
            manager.getDatabase().getServiceInstances().remove(_serviceModel);
            _serviceModel = null;
        }
    }

    public String getInstanceName() {
        return _instanceName;
    }

    public String getUuid() {
        return _uuid;
    }

    public VirtualMachine getVirtualMachine() {
        return _vm;
    }

    public VMInterfaceModel getVMInterface(String uuid) {
        TreeSet<ModelObject> tree = successors();
        VMInterfaceModel vmiKey = new VMInterfaceModel(uuid);
        VMInterfaceModel current = (VMInterfaceModel)tree.ceiling(vmiKey);
        if (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }

    public boolean isActive() {
        return _active;
    }

    boolean isActiveInstance(VMInstanceVO instance) {
        switch (instance.getState()) {
            case Migrating:
            case Starting:
            case Running:
            case Shutdown:
            case Stopped:
            case Stopping:
                return true;

            case Destroyed:
            case Error:
            case Expunging:
                return false;

            default:
                s_logger.warn("Unknown VMInstance state " + instance.getState().getDescription());
        }
        return true;
    }

    /**
     * Initialize the object properties based on the DB object.
     * Common code between plugin calls and DBSync.
     */
    public void setProperties(ModelController controller, VMInstanceVO instance) {
        ContrailManager manager = controller.getManager();
        _instanceName = instance.getInstanceName();
        _active = isActiveInstance(instance);

        try {
            _projectId = manager.getProjectId(instance.getDomainId(), instance.getAccountId());
        } catch (IOException ex) {
            s_logger.warn("project read", ex);
            throw new CloudRuntimeException(ex);
        }
        _initialized = true;
    }

    /**
     * Link the virtual machine with a service instance via programmatic API call.
     * @throws IOException
     */
    public void setServiceInstance(ModelController controller, VMInstanceVO instance, ServiceInstanceModel serviceModel) throws IOException {
        _serviceUuid = serviceModel.getUuid();
        _serviceModel = serviceModel;
        serviceModel.addSuccessor(this);
        setServiceInstanceNics(controller, instance);
    }

    private void setServiceInstanceNics(ModelController controller, VMInstanceVO instance) throws IOException {
        NicDao nicDao = controller.getNicDao();
        ContrailManager manager = controller.getManager();
        NetworkDao networkDao = controller.getNetworkDao();

        List<NicVO> nics = nicDao.listByVmId(_instanceId);
        for (NicVO nic : nics) {
            String tag;

            switch (nic.getDeviceId()) {
                case 0:
                    tag = "management";
                    break;
                case 1:
                    tag = "left";
                    break;
                case 2:
                    tag = "right";
                    break;
                default:
                    tag = null;
            }

            VMInterfaceModel vmiModel = getVMInterface(nic.getUuid());
            if (vmiModel == null) {
                vmiModel = new VMInterfaceModel(nic.getUuid());
                vmiModel.addToVirtualMachine(this);
                NetworkVO network = networkDao.findById(nic.getNetworkId());
                VirtualNetworkModel vnModel = manager.getDatabase().lookupVirtualNetwork(network.getUuid(), manager.getCanonicalName(network), network.getTrafficType());
                assert vnModel != null;
                vmiModel.addToVirtualNetwork(vnModel);
            }
            vmiModel.setProperties(controller, instance, nic);
            vmiModel.setServiceTag(tag);
        }
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {
        assert _initialized;
        ApiConnector api = controller.getApiAccessor();

        VirtualMachine vm = _vm;
        if (vm == null) {
            _vm = vm = (VirtualMachine)api.findById(VirtualMachine.class, _uuid);
            if (vm == null) {
                vm = new VirtualMachine();
                if (_projectId != null) {
                    Project project;
                    try {
                        project = (Project)api.findById(Project.class, _projectId);
                    } catch (IOException ex) {
                        s_logger.debug("project read", ex);
                        throw new CloudRuntimeException("Failed to read project", ex);
                    }
                    vm.setParent(project);
                }
                vm.setName(_instanceName);
                vm.setUuid(_uuid);
            }
        }

        if (_serviceModel != null) {
            vm.setServiceInstance(_serviceModel.getServiceInstance());
        }

        if (_vm == null) {
            try {
                api.create(vm);
            } catch (Exception ex) {
                s_logger.debug("virtual-machine create", ex);
                throw new CloudRuntimeException("Failed to create virtual-machine", ex);
            }
            _vm = vm;
        } else {
            try {
                api.update(vm);
            } catch (IOException ex) {
                s_logger.warn("virtual-machine update", ex);
                throw new CloudRuntimeException("Unable to update virtual-machine object", ex);
            }
        }

        for (ModelObject successor : successors()) {
            successor.update(controller);
        }
    }

    @Override
    public boolean verify(ModelController controller) {
        assert _initialized : "initialized is false";
    assert _uuid != null : "uuid is not set";

    ApiConnector api = controller.getApiAccessor();

    try {
        _vm = (VirtualMachine) api.findById(VirtualMachine.class, _uuid);
    } catch (IOException e) {
        s_logger.error("virtual-machine verify", e);
    }

    if (_vm == null) {
        return false;
    }

    for (ModelObject successor: successors()) {
        if (!successor.verify(controller)) {
            return false;
        }
    }
    return true;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject current) {
        // TODO Auto-generated method stub
        return true;
    }
}
