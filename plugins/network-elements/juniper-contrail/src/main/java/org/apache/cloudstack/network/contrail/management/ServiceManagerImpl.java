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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.contrail.api.response.ServiceInstanceResponse;
import org.apache.cloudstack.network.contrail.model.ServiceInstanceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.Gson;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.ServiceInstance;

public class ServiceManagerImpl implements ServiceManager {
    private static final Logger s_logger = Logger.getLogger(ServiceManager.class);

    @Inject
    UserDao _userDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    VirtualMachineManager _vmManager;
    @Inject
    NetworkModel _networkModel;
    @Inject
    AccountService _accountService;
    @Inject
    ContrailManager _manager;

    /**
     * In the case of service instance the primary object is in the contrail API server. This object stores the
     * service instance parameters in the database.
     *
     * @param owner     Used to determine the project.
     * @param name      Service instance name (user specified).
     * @param template  Image to execute.
     * @param serviceOffering
     * @param left      Inside network.
     * @param right     Outside network.
     * @return
     */

    /**
     * create a new ServiceVM object.
     * @return
     */
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "createServiceInstance", create = true)
    private ServiceVirtualMachine createServiceVM(DataCenter zone, Account owner, VirtualMachineTemplate template, ServiceOffering serviceOffering, String name,
        ServiceInstance siObj, Network left, Network right) {
        long id = _vmDao.getNextInSequence(Long.class, "id");

        DataCenterDeployment plan = new DataCenterDeployment(zone.getId());
        LinkedHashMap<NetworkVO, List<? extends NicProfile>> networks = new LinkedHashMap<NetworkVO, List<? extends NicProfile>>();
        NetworkVO linklocal = (NetworkVO) _networkModel.getSystemNetworkByZoneAndTrafficType(zone.getId(),
                TrafficType.Management);
        networks.put(linklocal, new ArrayList<NicProfile>());
        networks.put((NetworkVO)left, new ArrayList<NicProfile>());
        networks.put((NetworkVO)right, new ArrayList<NicProfile>());
        String instanceName = VirtualMachineName.getVmName(id, owner.getId(), "SRV");

        long userId = CallContext.current().getCallingUserId();
        if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
            List<UserVO> userVOs = _userDao.listByAccount(owner.getAccountId());
            if (!userVOs.isEmpty()) {
                userId =  userVOs.get(0).getId();
            }
        }

        ServiceVirtualMachine svm =
            new ServiceVirtualMachine(id, instanceName, name, template.getId(), serviceOffering.getId(), template.getHypervisorType(), template.getGuestOSId(),
                zone.getId(), owner.getDomainId(), owner.getAccountId(), userId, false);

        // database synchronization code must be able to distinguish service instance VMs.
        Map<String, String> kvmap = new HashMap<String, String>();
        kvmap.put("service-instance", siObj.getUuid());
        Gson json = new Gson();
        String userData = json.toJson(kvmap);
        svm.setUserData(userData);

        try {
            _vmManager.allocate(instanceName, template, serviceOffering, networks, plan, template.getHypervisorType());
        } catch (InsufficientCapacityException ex) {
            throw new CloudRuntimeException("Insufficient capacity", ex);
        }
        CallContext.current().setEventDetails("Vm Id: " + svm.getId());
        return svm;
    }

    @Override
    public ServiceVirtualMachine createServiceInstance(DataCenter zone, Account owner, VirtualMachineTemplate template, ServiceOffering serviceOffering, String name,
        Network left, Network right) {
        s_logger.debug("createServiceInstance by " + owner.getAccountName());
        // TODO: permission model.
        // service instances need to be able to access the public network.
        if (left.getTrafficType() == TrafficType.Guest) {
            _networkModel.checkNetworkPermissions(owner, left);
        }
        if (right.getTrafficType() == TrafficType.Guest) {
            _networkModel.checkNetworkPermissions(owner, right);
        }

        final ApiConnector api = _manager.getApiConnector();
        VirtualNetworkModel leftModel = _manager.getDatabase().lookupVirtualNetwork(left.getUuid(),
                _manager.getCanonicalName(left), left.getTrafficType());
        if (leftModel == null) {
            throw new CloudRuntimeException("Unable to read virtual-network object");
        }
        VirtualNetworkModel rightModel = _manager.getDatabase().lookupVirtualNetwork(right.getUuid(),
                _manager.getCanonicalName(right), right.getTrafficType());
        if (rightModel == null) {
            throw new CloudRuntimeException("Unable to read virtual-network object");
        }

        net.juniper.contrail.api.types.Project project;
        try {
            project = _manager.getVncProject(owner.getDomainId(), owner.getAccountId());
        } catch (IOException ex) {
            s_logger.warn("read project", ex);
            throw new CloudRuntimeException(ex);
        }

        try {
            final String srvid = api.findByName(ServiceInstance.class, project, name);
            if (srvid != null) {
                throw new InvalidParameterValueException("service-instance " + name + " already exists uuid=" + srvid);
            }
        } catch (IOException ex) {
            s_logger.warn("service-instance lookup", ex);
            throw new CloudRuntimeException(ex);
        }

        // 1. Create service-instance.
        ServiceInstanceModel serviceModel = new ServiceInstanceModel(project, name, template, serviceOffering,
                leftModel, rightModel);

        try {
            serviceModel.update(_manager.getModelController());
        } catch (Exception ex) {
            s_logger.warn("service-instance update", ex);
            throw new CloudRuntimeException(ex);
        }

        s_logger.debug("service-instance object created");

        ServiceInstance siObj;
        try {
            _manager.getDatabase().getServiceInstances().add(serviceModel);
            siObj = serviceModel.getServiceInstance();
        } catch (Exception ex) {
            s_logger.warn("DB add", ex);
            throw new CloudRuntimeException(ex);
        }

        // 2. Create one virtual-machine.
        String svmName = name.replace(" ", "_") + "-1";
        ServiceVirtualMachine svm = createServiceVM(zone, owner, template, serviceOffering, svmName, siObj, left, right);

        s_logger.debug("created VMInstance " + svm.getUuid());

        // 3. Create the virtual-machine model and push the update.
        VirtualMachineModel instanceModel = new VirtualMachineModel(svm, svm.getUuid());
        _manager.getDatabase().getVirtualMachines().add(instanceModel);
        try {
            instanceModel.setServiceInstance(_manager.getModelController(), svm, serviceModel);
            instanceModel.update(_manager.getModelController());
        } catch (Exception ex) {
            s_logger.warn("service virtual-machine update", ex);
            throw new CloudRuntimeException(ex);
        }

        return svm;
    }

    @Override
    public void startServiceInstance(long instanceId) {
        s_logger.debug("start service instance " + instanceId);

        UserVmVO vm = _vmDao.findById(instanceId);
        _vmManager.start(vm.getUuid(), null);
    }

    @Override
    public ServiceInstanceResponse createServiceInstanceResponse(long instanceId) {
        s_logger.debug("ServiceInstance response for id: " + instanceId);

        UserVmVO vm = _vmDao.findById(instanceId);
        ServiceInstanceResponse response = new ServiceInstanceResponse();
        response.setId(vm.getUuid());
        Account owner = _accountService.getAccount(vm.getAccountId());

        if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            Project project = ApiDBUtils.findProjectByProjectAccountIdIncludingRemoved(owner.getAccountId());
            response.setProjectId(project.getUuid());
            response.setProjectName(project.getName());
        } else {
            response.setAccountName(owner.getAccountName());
        }
        return response;
    }

}
