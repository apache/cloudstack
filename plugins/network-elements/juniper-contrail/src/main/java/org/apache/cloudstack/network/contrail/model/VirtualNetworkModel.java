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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VirtualNetworkPolicyType;
import net.juniper.contrail.api.types.VnSubnetsType;

import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

public class VirtualNetworkModel extends ModelObjectBase {

    private String _uuid;
    private long _id;
    private final TrafficType _trafficType;

    /*
     * current state for object properties
     */
    private boolean _initialized;
    private String _name;
    private String _prefix;
    private String _gateway;
    private String _projectId;

    /*
     * cached API server objects
     */
    private VirtualNetwork _vn;
    private NetworkIpam _ipam;

    private FloatingIpPoolModel _fipPoolModel;
    private NetworkPolicyModel _policyModel;

    public VirtualNetworkModel(Network network, String uuid, String name, TrafficType trafficType) {
        _uuid = uuid;
        _name = name;
        _trafficType = trafficType;
        if (network != null) {
            _id = network.getId();
        }

        if (isDynamicNetwork()) {
            assert _uuid != null : "uuid is must for dynamic networks";
        } else {
            assert _name != null : "name is must for static networks";
        }
    }

    /*
     * Resynchronize internal state from the cloudstack DB object.
     */
    public void build(ModelController controller, Network network) {
        setProperties(controller, network);
    }

    /**
     * Determine whether this network is dynamically created by cloudstack or is created by default by the contrail
     * API server.
     *
     * @return
     */
    boolean isDynamicNetwork() {
        return (_trafficType == TrafficType.Guest) || (_trafficType == TrafficType.Public);
    }

    @Override
    public int compareTo(ModelObject o) {
        VirtualNetworkModel other;
        try {
            other = (VirtualNetworkModel)o;
        } catch (ClassCastException ex) {
            String clsname = o.getClass().getName();
            return VirtualNetworkModel.class.getName().compareTo(clsname);
        }

        if (!isDynamicNetwork()) {
            if (!other.isDynamicNetwork()) {
                // name is not unique since both management and storage networks may map to ip-fabric
                int cmp = _name.compareTo(other.getName());
                if (cmp != 0) {
                    return cmp;
                }
                return _trafficType.compareTo(other._trafficType);
            }
            return -1;
        } else if (!other.isDynamicNetwork()) {
            return 1;
        }

        return _uuid.compareTo(other._uuid);
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        for (ModelObject successor : successors()) {
            successor.delete(controller);
        }

        if (_policyModel != null) {
            _policyModel.removeSuccessor(this);
        }

        try {
            api.delete(VirtualNetwork.class, _uuid);
        } catch (IOException ex) {
            logger.warn("virtual-network delete", ex);
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

    public VirtualNetwork getVirtualNetwork() {
        return _vn;
    }

    /**
     * Initialize the object properties based on the DB object.
     * Common code between plugin calls and DBSync.
     */
    public void setProperties(ModelController controller, Network network) {
        ContrailManager manager = controller.getManager();
        _name = manager.getCanonicalName(network);
        _prefix = network.getCidr();
        _gateway = network.getGateway();

        // For non-cloudstack managed network, find the uuid at this stage.
        if (!isDynamicNetwork()) {
            try {
                _uuid = manager.findVirtualNetworkId(network);
            } catch (IOException ex) {
                logger.warn("Unable to read virtual-network", ex);
            }
        }

        _id = network.getId();

        try {
            _projectId = manager.getProjectId(network.getDomainId(), network.getAccountId());
        } catch (IOException ex) {
            logger.warn("project read", ex);
            throw new CloudRuntimeException(ex);
        }

        _initialized = true;
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {

        assert _initialized;

        ApiConnector api = controller.getApiAccessor();
        VlanDao vlanDao = controller.getVlanDao();
        VirtualNetwork vn = _vn;

        if (!isDynamicNetwork()) {
            _vn = (VirtualNetwork)controller.getApiAccessor().findById(VirtualNetwork.class, _uuid);
            return;
        }

        assert _uuid != null : "uuid is not set";

        if (_vn == null) {
            vn = _vn = (VirtualNetwork)controller.getApiAccessor().findById(VirtualNetwork.class, _uuid);
            if (vn == null) {
                vn = new VirtualNetwork();
                if (_projectId != null) {
                    Project project;
                    try {
                        project = (Project)api.findById(Project.class, _projectId);
                    } catch (IOException ex) {
                        logger.debug("project read", ex);
                        throw new CloudRuntimeException("Failed to read project", ex);
                    }
                    vn.setParent(project);
                }
                vn.setName(_name);
                vn.setUuid(_uuid);
            }
        }

        if (_policyModel == null) {
            vn.clearNetworkPolicy();
        } else if (!_policyModel.hasPolicyRules()) {
            vn.clearNetworkPolicy();
            _policyModel.removeSuccessor(this);
        } else {
            vn.setNetworkPolicy(_policyModel.getPolicy(), new VirtualNetworkPolicyType(
                    new VirtualNetworkPolicyType.SequenceType(1, 0), null));
        }

        if (_ipam == null) {
            NetworkIpam ipam = null;
            try {
                String ipam_id = api.findByName(NetworkIpam.class, null, "default-network-ipam");
                if (ipam_id == null) {
                    logger.debug("could not find default-network-ipam");
                    return;
                }
                ipam = (NetworkIpam)api.findById(NetworkIpam.class, ipam_id);
                if (ipam == null) {
                    logger.debug("could not find NetworkIpam with ipam_id: " + ipam_id);
                    return;
                }
            } catch (IOException ex) {
                logger.error(ex);
                return;
            }
            _ipam = ipam;
        }

        if (_prefix != null) {
            VnSubnetsType subnet = new VnSubnetsType();
            String[] addr_pair = _prefix.split("\\/");
            subnet.addIpamSubnets(new SubnetType(addr_pair[0], Integer.parseInt(addr_pair[1])), _gateway);
            vn.setNetworkIpam(_ipam, subnet);
        } else if (_trafficType == TrafficType.Public) {
            vn.clearNetworkIpam();
            /* Subnet information for Public is stored in the vlan table */
            List<VlanVO> vlan_list = vlanDao.listVlansByNetworkId(_id);
            for (VlanVO vlan : vlan_list) {
                String cidr = NetUtils.ipAndNetMaskToCidr(vlan.getVlanGateway(), vlan.getVlanNetmask());
                int slash = cidr.indexOf('/');
                String ip_addr = cidr.substring(0, slash);
                int plen = Integer.parseInt(cidr.substring(slash + 1));
                VnSubnetsType subnet = new VnSubnetsType();
                subnet.addIpamSubnets(new SubnetType(ip_addr, plen), vlan.getVlanGateway());
                vn.addNetworkIpam(_ipam, subnet);
            }
        }

        if (_vn == null) {
            try {
                api.create(vn);
            } catch (Exception ex) {
                logger.debug("virtual-network create", ex);
                throw new CloudRuntimeException("Failed to create virtual-network", ex);
            }
            _vn = vn;
        } else {
            try {
                api.update(vn);
            } catch (IOException ex) {
                logger.warn("virtual-network update", ex);
                throw new CloudRuntimeException("Unable to update virtual-network object", ex);
            }
        }

        for (ModelObject successor : successors()) {
            successor.update(controller);
        }
    }

    public void read(ModelController controller) {
        ApiConnector api = controller.getApiAccessor();
        VlanDao vlanDao = controller.getVlanDao();
        try {
            _vn = (VirtualNetwork)api.findById(VirtualNetwork.class, _uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (_vn == null) {
            return;
        }
        if (_ipam == null) {
            NetworkIpam ipam = null;
            try {
                String ipam_id = api.findByName(NetworkIpam.class, null, "default-network-ipam");
                if (ipam_id == null) {
                    logger.debug("could not find default-network-ipam");
                    return;
                }
                ipam = (NetworkIpam)api.findById(NetworkIpam.class, ipam_id);
                if (ipam == null) {
                    logger.debug("could not find NetworkIpam with ipam_id: " + ipam_id);
                    return;
                }
            } catch (IOException ex) {
                logger.error(ex);
                return;
            }
            _ipam = ipam;
        }

        if (_prefix != null) {
            VnSubnetsType subnet = new VnSubnetsType();
            String[] addr_pair = _prefix.split("\\/");
            subnet.addIpamSubnets(new SubnetType(addr_pair[0], Integer.parseInt(addr_pair[1])), _gateway);
            _vn.setNetworkIpam(_ipam, subnet);
        } else if (_trafficType == TrafficType.Public) {
            _vn.clearNetworkIpam();
            /* Subnet information for Public is stored in the vlan table */
            List<VlanVO> vlan_list = vlanDao.listVlansByNetworkId(_id);
            for (VlanVO vlan : vlan_list) {
                String cidr = NetUtils.ipAndNetMaskToCidr(vlan.getVlanGateway(), vlan.getVlanNetmask());
                int slash = cidr.indexOf('/');
                String ip_addr = cidr.substring(0, slash);
                int plen = Integer.parseInt(cidr.substring(slash + 1));
                VnSubnetsType subnet = new VnSubnetsType();
                subnet.addIpamSubnets(new SubnetType(ip_addr, plen), vlan.getVlanGateway());
                _vn.addNetworkIpam(_ipam, subnet);
            }
        }
        return;
    }

    @Override
    public boolean verify(ModelController controller) {
        assert _initialized : "initialized is false";
    assert _uuid != null : "uuid is not set";

    ApiConnector api = controller.getApiAccessor();
    VlanDao vlanDao = controller.getVlanDao();

    try {
        _vn = (VirtualNetwork)api.findById(VirtualNetwork.class, _uuid);
    } catch (IOException e) {
        e.printStackTrace();
    }

    if (_vn == null) {
        return false;
    }

    if (!isDynamicNetwork()) {
        return true;
    }

    List<String> dbSubnets = new ArrayList<String>();
    if (_trafficType == TrafficType.Public) {
        List<VlanVO> vlan_list = vlanDao.listVlansByNetworkId(_id);
        for (VlanVO vlan : vlan_list) {
            String cidr = NetUtils.ipAndNetMaskToCidr(vlan.getVlanGateway(), vlan.getVlanNetmask());
            dbSubnets.add(vlan.getVlanGateway() + cidr);
        }
    } else {
        dbSubnets.add(_gateway + _prefix);
    }

    List<ObjectReference<VnSubnetsType>> ipamRefs = _vn.getNetworkIpam();
    List<String> vncSubnets = new ArrayList<String>();

    if (ipamRefs == null && !dbSubnets.isEmpty()) {
        return false;
    }

    if (ipamRefs != null) {
        for (ObjectReference<VnSubnetsType> ref : ipamRefs) {
            VnSubnetsType vnSubnetType = ref.getAttr();
            if (vnSubnetType != null) {
                List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetType.getIpamSubnets();
                if (subnets != null && !subnets.isEmpty()) {
                    VnSubnetsType.IpamSubnetType ipamSubnet = subnets.get(0);
                    vncSubnets.add(ipamSubnet.getDefaultGateway() + ipamSubnet.getSubnet().getIpPrefix() + "/" + ipamSubnet.getSubnet().getIpPrefixLen());
                }
            }
        }
    }
    // unordered, no duplicates hence perform negation operation as set
    Set<String> diff = new HashSet<String>(dbSubnets);
    diff.removeAll(vncSubnets);

    if (!diff.isEmpty()) {
        logger.debug("Subnets changed, network: " + _name + "; db: " + dbSubnets + ", vnc: " + vncSubnets + ", diff: " + diff);
        return false;
    }

    List<ObjectReference<VirtualNetworkPolicyType>> policyRefs = _vn.getNetworkPolicy();
    if ((policyRefs == null || policyRefs.isEmpty()) && _policyModel != null) {
        return false;
    }

    if ((policyRefs != null && !policyRefs.isEmpty()) && _policyModel == null) {
        return false;
    }

    if (policyRefs != null && !policyRefs.isEmpty() && _policyModel != null) {
        ObjectReference<VirtualNetworkPolicyType> ref = policyRefs.get(0);
        if (!ref.getUuid().equals(_policyModel.getUuid())) {
            return false;
        }
    }

    for (ModelObject successor : successors()) {
        if (!successor.verify(controller)) {
            return false;
        }
    }
    return true;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject o) {
        VirtualNetworkModel latest;
        assert _vn != null : "vnc virtual network current is not initialized";

        try {
            latest = (VirtualNetworkModel)o;
        } catch (ClassCastException ex) {
            logger.warn("Invalid model object is passed to cast to VirtualNetworkModel");
            return false;
        }

        try {
            latest.read(controller);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        assert latest._vn != null : "vnc virtual network new is not initialized";

        List<ObjectReference<VnSubnetsType>> currentIpamRefs = _vn.getNetworkIpam();
        List<ObjectReference<VnSubnetsType>> newIpamRefs = latest._vn.getNetworkIpam();
        List<String> currentSubnets = new ArrayList<String>();
        List<String> newSubnets = new ArrayList<String>();

        if ((currentIpamRefs == null && newIpamRefs != null) || (currentIpamRefs != null && newIpamRefs == null)) {  //Check for existence only
            logger.debug("ipams differ: current=" + currentIpamRefs + ", new=" + newIpamRefs);
            return false;
        }
        if (currentIpamRefs == null) {
            return true;
        }

        for (ObjectReference<VnSubnetsType> ref : currentIpamRefs) {
            VnSubnetsType vnSubnetType = ref.getAttr();
            if (vnSubnetType != null) {
                List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetType.getIpamSubnets();
                if (subnets != null && !subnets.isEmpty()) {
                    VnSubnetsType.IpamSubnetType ipamSubnet = subnets.get(0);
                    currentSubnets.add(ipamSubnet.getDefaultGateway() + ipamSubnet.getSubnet().getIpPrefix() + "/" + ipamSubnet.getSubnet().getIpPrefixLen());
                }
            }
        }

        for (ObjectReference<VnSubnetsType> ref : newIpamRefs) {
            VnSubnetsType vnSubnetType = ref.getAttr();
            if (vnSubnetType != null) {
                List<VnSubnetsType.IpamSubnetType> subnets = vnSubnetType.getIpamSubnets();
                if (subnets != null && !subnets.isEmpty()) {
                    VnSubnetsType.IpamSubnetType ipamSubnet = subnets.get(0);
                    newSubnets.add(ipamSubnet.getDefaultGateway() + ipamSubnet.getSubnet().getIpPrefix() + "/" + ipamSubnet.getSubnet().getIpPrefixLen());
                }
            }
        }

        Set<String> diff = new HashSet<String>(currentSubnets);
        diff.removeAll(newSubnets);

        if (!diff.isEmpty()) {
            logger.debug("Subnets differ, network: " + _name + "; db: " + currentSubnets + ", vnc: " + newSubnets + ", diff: " + diff);
            return false;
        }

        List<ObjectReference<VirtualNetworkPolicyType>> currentPolicyRefs = _vn.getNetworkPolicy();
        List<ObjectReference<VirtualNetworkPolicyType>> latestPolicyRefs = latest._vn.getNetworkPolicy();

        if (currentPolicyRefs == null && latestPolicyRefs == null) {
            return true;
        }

        if ((currentPolicyRefs == null && latestPolicyRefs != null) || (currentPolicyRefs != null
                && latestPolicyRefs == null)) {
            return false;
        }

        if ((currentPolicyRefs != null && latestPolicyRefs != null) && (currentPolicyRefs.size() != latestPolicyRefs.size())) {
            return false;
        }

        if ((currentPolicyRefs != null && latestPolicyRefs != null) &&  currentPolicyRefs.isEmpty()
                && latestPolicyRefs.isEmpty()) {
            return true;
        }

        //both must be non empty lists
        ObjectReference<VirtualNetworkPolicyType> ref1 = null;

        if (currentPolicyRefs != null) {
            ref1 = currentPolicyRefs.get(0);
        }

        ObjectReference<VirtualNetworkPolicyType> ref2 = null;

        if (latestPolicyRefs != null) {
            ref2 = latestPolicyRefs.get(0);
        }

        if (ref1 == null && ref2 == null) {
            return true;
        }

        if ((ref1 != null && ref2 == null) || (ref1 == null && ref2 != null)) {
            return false;
        }

        if ((ref1 != null && ref2 != null) && ((ref1.getUuid() != null && ref2.getUuid() == null)
                || (ref1.getUuid() == null && ref2.getUuid() != null))) {
            return false;
        }
        if ((ref1 != null && ref2 != null) && (ref1.getUuid() == null && ref2.getUuid() == null)) {
            return true;
        }
        if ((ref1 != null && ref2 != null) && !ref1.getUuid().equals(ref2.getUuid())) {
            return false;
        }
        return true;
    }

    public FloatingIpPoolModel getFipPoolModel() {
        return _fipPoolModel;
    }

    public void setFipPoolModel(FloatingIpPoolModel fipPoolModel) {
        _fipPoolModel = fipPoolModel;
    }

    public NetworkPolicyModel getNetworkPolicyModel() {
        return _policyModel;
    }

    public void addToNetworkPolicy(NetworkPolicyModel policyModel) {
        if (_policyModel != null) {
            _policyModel.removeSuccessor(this);
        }
        _policyModel = policyModel;
        if (_policyModel != null) {
            _policyModel.addSuccessor(this);
        }
    }
}
