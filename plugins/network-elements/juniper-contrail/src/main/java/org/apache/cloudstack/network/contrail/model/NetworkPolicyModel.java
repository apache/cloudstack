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

import org.apache.commons.lang3.StringUtils;

import com.cloud.network.Networks;

import net.juniper.contrail.api.types.NetworkPolicy;
import net.juniper.contrail.api.types.PolicyEntriesType;
import net.juniper.contrail.api.types.PolicyEntriesType.PolicyRuleType;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.ApiConnector;
import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.exception.InternalErrorException;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItem.Action;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

public class NetworkPolicyModel extends ModelObjectBase {

    private String _uuid;
    private String _fqName;
    private String _name;
    private Project _project;
    private NetworkPolicy _policy;
    PolicyEntriesType _policyMap;

    public NetworkPolicyModel(String uuid, String name) {
        _uuid = uuid;
        _name = name;
    }

     public String getQualifiedName() {
        return _fqName;
    }

    public String getName() {
        return _name;
    }

    public NetworkVO cidrToNetwork(ModelController controller, String cidr) {
        SearchBuilder<NetworkVO> searchBuilder = controller.getNetworkDao().createSearchBuilder();
        searchBuilder.and("trafficType", searchBuilder.entity().getTrafficType(), Op.EQ);
        searchBuilder.and("cidr", searchBuilder.entity().getCidr(), Op.EQ);
        searchBuilder.and("networkOfferingId", searchBuilder.entity().getNetworkOfferingId(), Op.EQ);

        SearchCriteria<NetworkVO> sc = searchBuilder.create();

        sc.setParameters("networkOfferingId", controller.getManager().getVpcRouterOffering().getId());
        sc.setParameters("cidr", cidr);
        sc.setParameters("trafficType", Networks.TrafficType.Guest);

        List<NetworkVO> dbNets = controller.getNetworkDao().search(sc, null);
        if (dbNets == null || dbNets.size() == 0) {
            return null;
        }
        if (dbNets.size() > 1) {
            logger.warn("more than one network found with cidr: " + cidr);
        }
        return dbNets.get(0);
    }

    public void build(ModelController controller, List<? extends NetworkACLItem> rules) throws Exception {
        String projectName = null;
        if (_project != null) {
            _fqName = StringUtils.join(_project.getQualifiedName(), ':') + ":" + _name;
            projectName = StringUtils.join(_project.getQualifiedName(), ':');
        } else {
            _fqName = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT + ":" + _name;
            projectName = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT;
        }

        PolicyEntriesType policyMap = new PolicyEntriesType();

        for (NetworkACLItem rule:rules) {
            if (rule.getState() != NetworkACLItem.State.Active &&
                 rule.getState() != NetworkACLItem.State.Add)  {
                 continue;
            }

            String action = null;
            if (rule.getAction() == Action.Allow) {
                action = "pass";
            } else if (rule.getAction() == Action.Deny) {
                action = "deny";
            }
            List<String> cidrList = rule.getSourceCidrList();
            String protocol = rule.getProtocol();
            if (protocol == null || protocol.equalsIgnoreCase("ALL") || protocol.isEmpty()) {
                protocol = "any";
            } else {
                protocol = protocol.toLowerCase();
            }

            Integer portStart = rule.getSourcePortStart();
            Integer portEnd = rule.getSourcePortEnd();
            if (portStart == null) {
                portStart = 0;
            }
            if (portEnd == null) {
                portEnd = 65535;
            }

            List<PolicyRuleType.AddressType> srcList = new ArrayList<PolicyRuleType.AddressType>();
            List<PolicyRuleType.AddressType> dstList = new ArrayList<PolicyRuleType.AddressType>();

            List<PolicyRuleType.PortType> srcPorts = new ArrayList<PolicyRuleType.PortType>();
            List<PolicyRuleType.PortType> dstPorts = new ArrayList<PolicyRuleType.PortType>();

            if (rule.getTrafficType() == NetworkACLItem.TrafficType.Egress){
                for (String cidr: cidrList) {
                    NetworkVO net = cidrToNetwork(controller, cidr);
                    /*String[] maskInfo = StringUtils.splitByWholeSeparator(cidr, "/");
                    SubnetType subnet = new SubnetType();
                    subnet.setIpPrefix(maskInfo[0]);
                    subnet.setIpPrefixLen(Integer.parseInt(maskInfo[1]));
                    */
                    String netName = projectName + ":" + controller.getManager().getCanonicalName(net);
                    dstList.add(new PolicyRuleType.AddressType(null, netName, null));
                }
                dstPorts.add(new PolicyRuleType.PortType(portStart, portEnd));
                srcList.add(new PolicyRuleType.AddressType(null, "local", null));
                srcPorts.add(new PolicyRuleType.PortType(0, 65535));
            } else {
                for (String cidr: cidrList) {
                    NetworkVO net = cidrToNetwork(controller, cidr);
                    String netName = projectName + ":" + controller.getManager().getCanonicalName(net);
                    srcList.add(new PolicyRuleType.AddressType(null, netName, null));
                }
                dstPorts.add(new PolicyRuleType.PortType(portStart, portEnd));

                dstList.add(new PolicyRuleType.AddressType(null, "local", null));
                srcPorts.add(new PolicyRuleType.PortType(0, 65535));
            }

            PolicyRuleType vnRule = new PolicyRuleType(
                    new PolicyRuleType.SequenceType(1, 0), rule.getUuid(), "<>", protocol,
                    srcList, srcPorts, null, dstList, dstPorts,
                    new PolicyRuleType.ActionListType(action, null, null, null));
            policyMap.addPolicyRule(vnRule);
        }
        _policyMap = policyMap;
    }

    /* for service instance policy */
    public void build(ModelController modelController, String leftVn, String rightVn, String gatewayName,
            List<String> siList, String action) {
        if (_project != null) {
            _fqName = StringUtils.join(_project.getQualifiedName(), ':') + ":" + _name;
        } else {
            _fqName = ContrailManager.VNC_ROOT_DOMAIN + ":" + ContrailManager.VNC_DEFAULT_PROJECT + ":" + _name;
        }

        PolicyEntriesType policyMap = new PolicyEntriesType();
        List<PolicyRuleType.AddressType> srcList = new ArrayList<PolicyRuleType.AddressType>();
        srcList.add(new PolicyRuleType.AddressType(null, leftVn, null));
        List<PolicyRuleType.AddressType> dstList = new ArrayList<PolicyRuleType.AddressType>();
        dstList.add(new PolicyRuleType.AddressType(null, rightVn, null));

        List<PolicyRuleType.PortType> portAny = new ArrayList<PolicyRuleType.PortType>();
        portAny.add(new PolicyRuleType.PortType(0, 65535));

        PolicyRuleType rule = new PolicyRuleType(
                new PolicyRuleType.SequenceType(1, 0),  null, "<>", "any",
                srcList, portAny, null, dstList, portAny,
                new PolicyRuleType.ActionListType(action, gatewayName, siList, null));
        policyMap.addPolicyRule(rule);
        _policyMap = policyMap;
    }

    public boolean hasPolicyRules() {
        if (_policyMap != null && _policyMap.getPolicyRule() != null && _policyMap.getPolicyRule().size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(ModelObject o) {
        NetworkPolicyModel other;
        try {
            other = (NetworkPolicyModel) o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return NetworkPolicyModel.class.getName().compareTo(clsname);
        }
        return _uuid.compareTo(other._uuid);
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        if (_policy != null) {
            api.delete(_policy);
            _policy = null;
        }
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
    }

    public String getUuid() {
        return _uuid;
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {
        ApiConnector api = controller.getApiAccessor();
        if (_project == null) {
            logger.debug("Project is null for the policy: " + _name);
            throw new IOException("Project is null for the policy: " + _name);
        }

        NetworkPolicy policy = _policy;

        if (policy == null) {
            try {
                String policyId = api.findByName(NetworkPolicy.class, _project, _name);
                if (policyId != null) {
                    policy = _policy = (NetworkPolicy) api.findById(NetworkPolicy.class, policyId);
                }
                if (policy == null) {
                    policy = new NetworkPolicy();
                    policy.setUuid(_uuid);
                    policy.setName(_name);
                    policy.setParent(_project);
                }
            } catch (IOException ex) {
                logger.warn("network-policy read", ex);
                return;
            }
        }

        policy.setEntries(_policyMap);
        if (_policy == null) {
            try {
                api.create(policy);
            } catch (Exception ex) {
                logger.debug("network policy create", ex);
                throw new CloudRuntimeException("Failed to create network policy", ex);
            }
            _policy = policy;
        } else {
            try {
                api.update(policy);
            } catch (IOException ex) {
                logger.warn("network policy update", ex);
                throw new CloudRuntimeException("Unable to update network policy", ex);
            }
        }
        for (ModelObject successor: successors()) {
            successor.update(controller);
        }
    }

    @Override
    public boolean verify(ModelController controller) {
        return false;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject current) {
        return true;
    }

    public void setProperties(ModelController controller, List<? extends NetworkACLItem> rules) {

    }

    public void setProject(Project project) {
        _project = project;
    }

    public NetworkPolicy getPolicy() {
        return _policy;
    }
}
