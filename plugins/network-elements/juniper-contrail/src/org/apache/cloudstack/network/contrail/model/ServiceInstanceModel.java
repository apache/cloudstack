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

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkPolicy;
import net.juniper.contrail.api.types.PolicyEntriesType;
import net.juniper.contrail.api.types.PolicyEntriesType.PolicyRuleType;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.ServiceInstance;
import net.juniper.contrail.api.types.ServiceInstanceType;
import net.juniper.contrail.api.types.ServiceTemplate;
import net.juniper.contrail.api.types.ServiceTemplateType;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VirtualNetworkPolicyType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;

public class ServiceInstanceModel extends ModelObjectBase {
    private static final Logger s_logger = Logger.getLogger(ServiceInstanceModel.class);

    private String _uuid;
    private String _fq_name;
    private String _projectId;
    private String _mgmtName;
    private String _leftName;
    private String _rightName;

    private String _templateName;
    private String _templateId;
    private String _templateUrl;
    private VirtualNetwork _left;
    private VirtualNetwork _right;
    private ServiceTemplate _tmpl;
    private ServiceInstance _serviceInstance;
    private NetworkPolicy _policy;

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
    public ServiceInstanceModel(Project project, String name, VirtualMachineTemplate template, ServiceOffering serviceOffering, VirtualNetwork left, VirtualNetwork right) {
        String parent_name;
        if (project != null) {
            parent_name = StringUtils.join(project.getQualifiedName(), ':');
        } else {
            parent_name = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT;
        }
        _fq_name = parent_name + ":" + name;

        _mgmtName = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT + ":" + ContrailManager.managementNetworkName;
        _left = left;
        _right = right;
        _leftName = StringUtils.join(left.getQualifiedName(), ":");
        _rightName = StringUtils.join(right.getQualifiedName(), ":");

        _templateName = template.getName();
        _templateId = template.getUuid();
        _templateUrl = template.getUrl();

        _projectId = project.getUuid();
    }

    /**
     * Create an empty ServiceInstance.
     * @param uuid
     */
    public ServiceInstanceModel(String uuid) {
        _uuid = uuid;
    }

    public String getQualifiedName() {
        return _fq_name;
    }

    public String getName() {
        return _fq_name.substring(_fq_name.lastIndexOf(':') + 1);
    }

    private void applyNetworkPolicy(ModelController controller, NetworkPolicy policy, VirtualNetwork left, VirtualNetwork right) {
        left.setNetworkPolicy(policy, new VirtualNetworkPolicyType(new VirtualNetworkPolicyType.SequenceType(1, 0), null));
        // TODO: network_ipam_refs attr is missing
        left.clearNetworkIpam();
        try {
            ApiConnector api = controller.getApiAccessor();
            api.update(left);
        } catch (IOException ex) {
            throw new CloudRuntimeException("Unable to update virtual-network", ex);
        }

        right.setNetworkPolicy(policy, new VirtualNetworkPolicyType(new VirtualNetworkPolicyType.SequenceType(1, 0), null));
        // TODO: network_ipam_refs attr is missing
        right.clearNetworkIpam();
        try {
            ApiConnector api = controller.getApiAccessor();
            api.update(right);
        } catch (IOException ex) {
            throw new CloudRuntimeException("Unable to update virtual-network", ex);
        }
    }

    /**
     * Recreate the model object from the Contrail API which is the master for this type of object.
     * @param siObj
     */
    public void build(ModelController controller, ServiceInstance siObj) {
        ApiConnector api = controller.getApiAccessor();
        _serviceInstance = siObj;
        _fq_name = StringUtils.join(siObj.getQualifiedName(), ':');
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
        try {
            Project project = (Project)api.findById(Project.class, siObj.getParentUuid());
            if (project != null) {
                _projectId = project.getUuid();
            }
            String policyId = api.findByName(NetworkPolicy.class, project, siObj.getName());
            if (policyId != null) {
                _policy = (NetworkPolicy)api.findById(NetworkPolicy.class, policyId);
            }
        } catch (IOException ex) {
            s_logger.warn("network-policy read", ex);
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
        return _fq_name.compareTo(other._fq_name);
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

    private NetworkPolicy createServicePolicy(ModelController controller) {
        NetworkPolicy policy = new NetworkPolicy();
        policy.setParent(_serviceInstance.getParent());
        policy.setName(_serviceInstance.getName());
        PolicyEntriesType policy_map = new PolicyEntriesType();
        List<PolicyRuleType.AddressType> srcList = new ArrayList<PolicyRuleType.AddressType>();
        srcList.add(new PolicyRuleType.AddressType(null, _leftName, null));
        List<PolicyRuleType.AddressType> dstList = new ArrayList<PolicyRuleType.AddressType>();
        dstList.add(new PolicyRuleType.AddressType(null, _rightName, null));
        List<String> siList = new ArrayList<String>();
        siList.add(StringUtils.join(_serviceInstance.getQualifiedName(), ':'));
        List<PolicyRuleType.PortType> portAny = new ArrayList<PolicyRuleType.PortType>();
        portAny.add(new PolicyRuleType.PortType(0, 65535));

        PolicyRuleType rule =
            new PolicyRuleType(new PolicyRuleType.SequenceType(1, 0), /* uuid */null, "<>", "any", srcList, portAny, /* application */null, dstList, portAny,
                new PolicyRuleType.ActionListType("pass", "in-network", siList, null));
        policy_map.addPolicyRule(rule);
        policy.setEntries(policy_map);

        try {
            ApiConnector api = controller.getApiAccessor();
            if (!api.create(policy)) {
                throw new CloudRuntimeException("Unable to create network-policy");
            }
        } catch (IOException ex) {
            throw new CloudRuntimeException("Unable to create network-policy", ex);
        }
        return policy;
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
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
            _policy = createServicePolicy(controller);
            // TODO: update the network model objects and call update
            applyNetworkPolicy(controller, _policy, _left, _right);
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
