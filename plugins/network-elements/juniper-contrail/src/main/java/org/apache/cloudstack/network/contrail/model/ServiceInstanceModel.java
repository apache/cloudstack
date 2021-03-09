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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.ServiceInstance;
import net.juniper.contrail.api.types.ServiceInstanceType;
import net.juniper.contrail.api.types.ServiceTemplate;
import net.juniper.contrail.api.types.ServiceTemplateType;

import org.apache.cloudstack.network.contrail.management.ContrailManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;

public class ServiceInstanceModel extends ModelObjectBase {
    private static final Logger s_logger = Logger.getLogger(ServiceInstanceModel.class);

    private String _uuid;
    private String _fqName;
    private String _projectId;
    private String _mgmtName;
    private String _leftName;
    private String _rightName;

    private String _templateName;
    private String _templateId;
    private String _templateUrl;
    private VirtualNetworkModel _left;
    private VirtualNetworkModel _right;
    private ServiceTemplate _tmpl;
    private ServiceInstance _serviceInstance;
    private NetworkPolicyModel _policy;

    /**
     * Create a ServiceInstance as result of an API call.
     *
     * @param owner
     * @param name
     * @param template
     * @param serviceOffering
     * @param left
     * @param right
     */
    public ServiceInstanceModel(Project project, String name, VirtualMachineTemplate template, ServiceOffering serviceOffering, VirtualNetworkModel left, VirtualNetworkModel right) {
        String parent_name;
        if (project != null) {
            parent_name = StringUtils.join(project.getQualifiedName(), ':');

            _projectId = project.getUuid();
        } else {
            parent_name = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT;

            //In the original code, if the projectId is null, it will simply throw NPE on the last line (nr. 90) of the method where the projectId.getUuid() is called.
            //This was added as a way to avoid NPE. Should we perhaps throw a CloudRuntimeException if the project object is null?
            _projectId = UUID.randomUUID().toString();
        }
        _fqName = parent_name + ":" + name;

        _mgmtName = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT + ":" + ContrailManager.managementNetworkName;
        _left = left;
        _right = right;
        _leftName = StringUtils.join(left.getVirtualNetwork().getQualifiedName(), ":");
        _rightName = StringUtils.join(right.getVirtualNetwork().getQualifiedName(), ":");

        _templateName = template.getName();
        _templateId = template.getUuid();
        _templateUrl = template.getUrl();
    }

    /**
     * Create an empty ServiceInstance.
     * @param uuid
     */
    public ServiceInstanceModel(String uuid) {
        _uuid = uuid;
    }

    public String getQualifiedName() {
        return _fqName;
    }

    public String getName() {
        return _fqName.substring(_fqName.lastIndexOf(':') + 1);
    }

    /**
     * Recreate the model object from the Contrail API which is main for this type of object.
     * @param siObj
     */
    public void build(ModelController controller, ServiceInstance siObj) {
        ApiConnector api = controller.getApiAccessor();
        _serviceInstance = siObj;
        _fqName = StringUtils.join(siObj.getQualifiedName(), ':');
        ServiceInstanceType props = siObj.getProperties();
        // TODO: read management network names and cache network objects.
        ObjectReference ref = siObj.getServiceTemplate().get(0);
        if (ref != null) {
            try {
                ServiceTemplate tmpl = (ServiceTemplate)api.findById(ServiceTemplate.class, ref.getUuid());
                _templateId = tmpl.getUuid();
            } catch (IOException ex) {
                s_logger.warn("service-template read", ex);
            }
        }
    }

    @Override
    public int compareTo(ModelObject o) {
        ServiceInstanceModel other;
        try {
            other = (ServiceInstanceModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return ServiceInstanceModel.class.getName().compareTo(clsname);
        }
        return _fqName.compareTo(other._fqName);
    }

    private ServiceInstance createServiceInstance(ModelController controller) {
        Project project = null;
        if (_projectId != null) {
            try {
                ApiConnector api = controller.getApiAccessor();
                project = (Project)api.findById(Project.class, _projectId);
            } catch (IOException ex) {
                s_logger.warn("project read", ex);
                throw new CloudRuntimeException("Unable to create service-instance object", ex);
            }
        }

        ServiceInstance si_obj = new ServiceInstance();
        if (project != null) {
            si_obj.setParent(project);
        }
        si_obj.setName(getName());
        si_obj.setServiceTemplate(_tmpl);
        si_obj.setProperties(new ServiceInstanceType(false, _mgmtName, _leftName, null, _rightName, null, new ServiceInstanceType.ServiceScaleOutType(1, false)));
        try {
            ApiConnector api = controller.getApiAccessor();
            api.create(si_obj);
        } catch (IOException ex) {
            s_logger.warn("service-instance create", ex);
            throw new CloudRuntimeException("Unable to create service-instance object", ex);
        }

        return si_obj;
    }

    private void clearServicePolicy(ModelController controller) {
        _left.addToNetworkPolicy(null);
        _right.addToNetworkPolicy(null);
        try {
            controller.getManager().getDatabase().getNetworkPolicys().remove(_policy);
            _policy.delete(controller.getManager().getModelController());
            _policy = null;
        } catch (Exception e) {
            s_logger.error(e);
        }
        try {
            _left.update(controller.getManager().getModelController());
            _right.update(controller.getManager().getModelController());
        } catch (Exception ex) {
            s_logger.error("virtual-network update for policy delete: ", ex);
        }
    }

    private NetworkPolicyModel setServicePolicy(ModelController controller) {
        NetworkPolicyModel policyModel = new NetworkPolicyModel(UUID.randomUUID().toString(), _serviceInstance.getName());
        policyModel.setProject((Project)_serviceInstance.getParent());
        _left.addToNetworkPolicy(policyModel);
        _right.addToNetworkPolicy(policyModel);
        List<String> siList = new ArrayList<String>();
        siList.add(StringUtils.join(_serviceInstance.getQualifiedName(), ':'));
        try {
            policyModel.build(controller.getManager().getModelController(), _leftName, _rightName, "in-network", siList, "pass");
        } catch (Exception e) {
            s_logger.error(e);
            return null;
        }
        try {
            if (!policyModel.verify(controller.getManager().getModelController())) {
                policyModel.update(controller.getManager().getModelController());
            }
            controller.getManager().getDatabase().getNetworkPolicys().add(policyModel);
        } catch (Exception ex) {
            s_logger.error("network-policy update: ", ex);
        }
        return policyModel;
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        clearServicePolicy(controller);
        if (_serviceInstance != null) {
            api.delete(_serviceInstance);
        }
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
    }

    public ServiceInstance getServiceInstance() {
        return _serviceInstance;
    }

    public String getUuid() {
        return _uuid;
    }

    private ServiceTemplate locateServiceTemplate(ModelController controller) {
        ServiceTemplate tmpl;
        try {
            ApiConnector api = controller.getApiAccessor();
            tmpl = (ServiceTemplate)api.findById(ServiceTemplate.class, _templateId);
        } catch (IOException ex) {
            s_logger.warn("service-template read", ex);
            throw new CloudRuntimeException("Unable to create service-template object", ex);
        }
        if (tmpl == null) {
            tmpl = new ServiceTemplate();
            tmpl.setName(_templateName);
            tmpl.setUuid(_templateId);
            ServiceTemplateType props = new ServiceTemplateType("in-network", null, _templateUrl, false, null);
            tmpl.setProperties(props);
            try {
                ApiConnector api = controller.getApiAccessor();
                api.create(tmpl);
            } catch (IOException ex) {
                throw new CloudRuntimeException("Unable to create service-template object", ex);
            }
        }
        return tmpl;
    }

    @Override
    public void update(ModelController controller) {
        _tmpl = locateServiceTemplate(controller);
        if (_serviceInstance == null) {
            _serviceInstance = createServiceInstance(controller);
        }
        _uuid = _serviceInstance.getUuid();
        if (_policy == null) {
            _policy = setServicePolicy(controller);
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
