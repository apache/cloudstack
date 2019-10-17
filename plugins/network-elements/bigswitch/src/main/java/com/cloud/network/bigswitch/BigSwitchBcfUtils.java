//
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
//

package com.cloud.network.bigswitch;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.IPAddress;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.BcfAnswer;
import com.cloud.agent.api.BcfCommand;
import com.cloud.agent.api.CacheBcfTopologyCommand;
import com.cloud.agent.api.GetControllerDataAnswer;
import com.cloud.agent.api.GetControllerDataCommand;
import com.cloud.agent.api.SyncBcfTopologyCommand;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.bigswitch.TopologyData.Port;
import com.cloud.network.dao.BigSwitchBcfDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesCidrsVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemCidrsVO;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class BigSwitchBcfUtils {
    private static final Logger s_logger = Logger.getLogger(BigSwitchBcfUtils.class);

    private final NetworkDao _networkDao;
    private final NicDao _nicDao;
    private final VMInstanceDao _vmDao;
    private final HostDao _hostDao;
    private final VpcDao _vpcDao;
    private final BigSwitchBcfDao _bigswitchBcfDao;
    private final AgentManager _agentMgr;
    private final VlanDao _vlanDao;
    private final IPAddressDao _ipAddressDao;
    private final FirewallRulesDao _fwRulesDao;
    private final FirewallRulesCidrsDao _fwCidrsDao;
    private final NetworkACLItemDao _aclItemDao;
    private final NetworkACLItemCidrsDao _aclItemCidrsDao;
    private final NetworkModel _networkModel;

    public BigSwitchBcfUtils(NetworkDao networkDao,
            NicDao nicDao, VMInstanceDao vmDao, HostDao hostDao,
            VpcDao vpcDao, BigSwitchBcfDao bigswitchBcfDao,
            AgentManager agentMgr, VlanDao vlanDao, IPAddressDao ipAddressDao,
            FirewallRulesDao fwRulesDao, FirewallRulesCidrsDao fwCidrsDao,
            NetworkACLItemDao aclItemDao, NetworkACLItemCidrsDao aclItemCidrsDao,
            NetworkModel networkModel){
        _networkDao = networkDao;
        _nicDao = nicDao;
        _vmDao = vmDao;
        _hostDao = hostDao;
        _vpcDao = vpcDao;
        _bigswitchBcfDao = bigswitchBcfDao;
        _agentMgr = agentMgr;
        _vlanDao = vlanDao;
        _ipAddressDao = ipAddressDao;
        _fwRulesDao = fwRulesDao;
        _fwCidrsDao = fwCidrsDao;
        _aclItemDao = aclItemDao;
        _aclItemCidrsDao = aclItemCidrsDao;
        _networkModel = networkModel;
    }

    public ControlClusterData getControlClusterData(long physicalNetworkId){
        ControlClusterData cluster = new ControlClusterData();

        // reusable command to query all devices
        GetControllerDataCommand cmd = new GetControllerDataCommand();

        // retrieve all registered BCF devices
        List<BigSwitchBcfDeviceVO> devices = _bigswitchBcfDao.listByPhysicalNetwork(physicalNetworkId);
        for (BigSwitchBcfDeviceVO d: devices){
            HostVO bigswitchBcfHost = _hostDao.findById(d.getHostId());
            if (bigswitchBcfHost == null){
                continue;
            }

            _hostDao.loadDetails(bigswitchBcfHost);
            GetControllerDataAnswer answer = (GetControllerDataAnswer) _agentMgr.easySend(bigswitchBcfHost.getId(), cmd);
            if (answer != null){
                if (answer.isMaster()) {
                    cluster.setMaster(bigswitchBcfHost);
                } else {
                    cluster.setSlave(bigswitchBcfHost);
                }
            }
        }
        return cluster;
    }

    public TopologyData getTopology(){
        long physicalNetworkId;
        List<BigSwitchBcfDeviceVO> devices = _bigswitchBcfDao.listAll();
        if(!devices.isEmpty()){
            physicalNetworkId = devices.get(0).getPhysicalNetworkId();
            return getTopology(physicalNetworkId);
        } else {
            return null;
        }
    }

    public NetworkVO getPublicNetwork(long physicalNetworkId){
        List<NetworkVO> pubNets = _networkDao.listByPhysicalNetworkTrafficType(physicalNetworkId, TrafficType.Public);
        return pubNets.get(0);
    }

    public TopologyData getTopology(long physicalNetworkId){
        List<NetworkVO> networks;
        List<NicVO> nics;

        networks = _networkDao.listByPhysicalNetworkTrafficType(physicalNetworkId, TrafficType.Guest);

        TopologyData topo = new TopologyData();

        // networks:
        // - all user created networks (VPC or non-VPC)
        // - one external network
        // routers:
        // - per VPC (handle in network loop)
        // - per stand-alone network (handle in network loop)
        // - one external router

        // handle external network first, only if NAT service is enabled
        if(networks != null) {
            if(!(networks.isEmpty()) && isNatEnabled()){
                // get public net info - needed to set up source nat gateway
                NetworkVO pubNet = getPublicNetwork(physicalNetworkId);

                // locate subnet info
                SearchCriteria<VlanVO> sc = _vlanDao.createSearchCriteria();
                sc.setParameters("network_id", pubNet.getId());
                VlanVO vlanVO = _vlanDao.findOneBy(sc);

                // add tenant external network external
                TopologyData.Network network = topo.new Network();
                network.setId("external");
                network.setName("external");
                network.setTenantId("external");
                network.setTenantName("external");

                String pubVlan = null;
                try {
                    pubVlan = BroadcastDomainType.getValue(vlanVO.getVlanTag());
                    if(StringUtils.isNumeric(pubVlan)){
                        network.setVlan(Integer.valueOf(pubVlan));
                    } else {
                        // untagged
                        pubVlan = "0";
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                topo.addNetwork(network);
            }
        }

        // routerMap used internally for multiple updates to same tenant's router
        // add back to topo.routers after loop
        HashMap<String, RouterData> routerMap = new HashMap<String, RouterData>();

        for (NetworkVO netVO: networks){
            TopologyData.Network network = topo.new Network();
            network.setId(netVO.getUuid());
            network.setName(netVO.getName());

            Integer vlan = null;
            if (netVO.getBroadcastUri() != null) {
                String vlanStr = BroadcastDomainType.getValue(netVO.getBroadcastUri());
                if(StringUtils.isNumeric(vlanStr)){
                    vlan = Integer.valueOf(vlanStr);
                } else {
                    // untagged
                    vlan = 0;
                }
            }
            network.setVlan(vlan);
            network.setState(netVO.getState().name());

            nics = _nicDao.listByNetworkId(netVO.getId());
            List<Port> ports = new ArrayList<Port>();
            String tenantId = null;
            String tenantName = null;

            // if VPC network, assign BCF tenant id with vpc uuid
            Vpc vpc = null;
            if(netVO.getVpcId()!=null){
                vpc = _vpcDao.acquireInLockTable(netVO.getVpcId());
            }

            if (vpc != null) {
                tenantId = vpc.getUuid();
                tenantName = vpc.getName();
            } else {
                tenantId = netVO.getUuid();
                tenantName = netVO.getName();
            }

            for(NicVO nic: nics){
                NetworkData netData = new NetworkData();
                TopologyData.Port p = topo.new Port();

                p.setAttachmentInfo(netData.new AttachmentInfo(nic.getUuid(),nic.getMacAddress()));

                VMInstanceVO vm = _vmDao.findById(nic.getInstanceId());
                HostVO host = _hostDao.findById(vm.getHostId());

                // if host not found, ignore this nic
                if (host == null) {
                    continue;
                }

                String hostname = host.getName();
                long zoneId = netVO.getDataCenterId();
                String vmwareVswitchLabel = _networkModel.getDefaultGuestTrafficLabel(zoneId, HypervisorType.VMware);
                String[] labelArray = null;
                String vswitchName = null;
                if(vmwareVswitchLabel!=null){
                    labelArray=vmwareVswitchLabel.split(",");
                    vswitchName = labelArray[0];
                }

                // hypervisor type:
                //   kvm: ivs port name
                //   vmware: specific portgroup naming convention
                String pgName = "";
                if (host.getHypervisorType() == HypervisorType.KVM){
                    pgName = hostname;
                } else if (host.getHypervisorType() == HypervisorType.VMware){
                    pgName = hostname + "-" + vswitchName;
                }

                p.setHostId(pgName);

                p.setSegmentInfo(netData.new SegmentInfo(BroadcastDomainType.Vlan.name(), vlan));

                p.setOwner(BigSwitchBcfApi.getCloudstackInstanceId());

                List<AttachmentData.Attachment.IpAddress> ipList = new ArrayList<AttachmentData.Attachment.IpAddress>();
                ipList.add(new AttachmentData().getAttachment().new IpAddress(nic.getIPv4Address()));
                p.setIpAddresses(ipList);

                p.setId(nic.getUuid());

                p.setMac(nic.getMacAddress());

                netData.getNetwork().setId(network.getId());
                netData.getNetwork().setName(network.getName());
                netData.getNetwork().setTenantId(tenantId);
                netData.getNetwork().setTenantName(tenantName);
                netData.getNetwork().setState(netVO.getState().name());

                p.setNetwork(netData.getNetwork());
                ports.add(p);
            }
            network.setTenantId(tenantId);
            network.setTenantName(tenantName);
            network.setPorts(ports);
            topo.addNetwork(network);

            // add router for network
            RouterData routerData;
            if(tenantId != null){
                if(!routerMap.containsKey(tenantId)){
                    routerData = new RouterData(tenantId);
                    routerMap.put(tenantId, routerData);
                } else {
                    routerData = routerMap.get(tenantId);
                }

                routerData.getRouter().getAcls().addAll(listACLbyNetwork(netVO));
                if(vpc != null){
                    routerData.getRouter().addExternalGateway(getPublicIpByVpc(vpc));
                } else {
                    routerData.getRouter().addExternalGateway(getPublicIpByNetwork(netVO));
                }

                RouterInterfaceData intf = new RouterInterfaceData(tenantId, netVO.getGateway(), netVO.getCidr(),
                        netVO.getUuid(), netVO.getName());
                routerData.getRouter().addInterface(intf);
            }
        }

        for(RouterData rd: routerMap.values()) {
            topo.addRouter(rd.getRouter());
        }

        return topo;
    }

    public String getPublicIpByNetwork(Network network){
        List<IPAddressVO> allocatedIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
        for (IPAddressVO ip: allocatedIps){
            if(ip.isSourceNat()){
                return ip.getAddress().addr();
            }
        }
        return null;
    }

    public String getPublicIpByVpc(Vpc vpc){
        List<IPAddressVO> allocatedIps = _ipAddressDao.listByAssociatedVpc(vpc.getId(), true);
        for (IPAddressVO ip: allocatedIps){
            if(ip.isSourceNat()){
                return ip.getAddress().addr();
            }
        }
        return null;
    }

    public List<AclData> listACLbyNetwork(Network network){
        List<AclData> aclList = new ArrayList<AclData>();

        List<FirewallRuleVO> fwRules;
        fwRules = _fwRulesDao.listByNetworkAndPurposeAndNotRevoked(network.getId(), Purpose.Firewall);
        List<FirewallRulesCidrsVO> fwCidrList = null;
        SubnetUtils utils;

        for(FirewallRuleVO rule: fwRules){
            AclData acl = new AclData();
            acl.setId(rule.getUuid());
            acl.setPriority((int)rule.getId()); // CloudStack Firewall interface does not have priority
            acl.setIpProto(rule.getProtocol());
            String cidr = null;
            Integer port = rule.getSourcePortStart();
            fwCidrList = _fwCidrsDao.listByFirewallRuleId(rule.getId());
            if(fwCidrList != null){
                if(fwCidrList.size()>1 || !rule.getSourcePortEnd().equals(port)){
                    continue;
                } else {
                    cidr = fwCidrList.get(0).getCidr();
                }
            }
            if (cidr == null || cidr.equalsIgnoreCase("0.0.0.0/0")) {
                cidr = "";
            } else {
                utils = new SubnetUtils(cidr);
                if(!utils.getInfo().getNetworkAddress().equals(utils.getInfo().getAddress())){
                    continue;
                }
            }
            acl.setSource(acl.new AclNetwork(cidr, port));
            acl.setAction("permit");

            aclList.add(acl);
        }

        List<NetworkACLItemVO> aclItems;
        List<NetworkACLItemCidrsVO> aclCidrList;

        if (network.getNetworkACLId() != null){
            aclItems = _aclItemDao.listByACL(network.getNetworkACLId());
            for(NetworkACLItem item: aclItems){
                AclData acl = new AclData();
                acl.setId(item.getUuid());
                acl.setPriority(item.getNumber());
                acl.setIpProto(item.getProtocol());
                String cidr = null; // currently BCF supports single cidr policy
                Integer port = item.getSourcePortStart(); // currently BCF supports single port policy
                aclCidrList = _aclItemCidrsDao.listByNetworkACLItemId(item.getId());
                if(aclCidrList != null){
                    if(aclCidrList.size()>1 || !item.getSourcePortEnd().equals(port)){
                        continue;
                    } else {
                        cidr = aclCidrList.get(0).getCidr();
                    }
                }
                if (cidr == null || cidr.equalsIgnoreCase("0.0.0.0/0")) {
                    cidr = "";
                } else {
                    utils = new SubnetUtils(cidr);
                    if(!utils.getInfo().getNetworkAddress().equals(utils.getInfo().getAddress())){
                        continue;
                    }
                }
                acl.setSource(acl.new AclNetwork(cidr, port));
                acl.setAction(item.getAction().name());

                aclList.add(acl);
            }
        }

        return aclList;
    }

    public String syncTopologyToBcfHost(HostVO bigswitchBcfHost){
        SyncBcfTopologyCommand syncCmd;
        if(isNatEnabled()){
            syncCmd = new SyncBcfTopologyCommand(true, true);
        } else {
            syncCmd = new SyncBcfTopologyCommand(true, false);
        }
        BcfAnswer syncAnswer = (BcfAnswer) _agentMgr.easySend(bigswitchBcfHost.getId(), syncCmd);
        if (syncAnswer == null || !syncAnswer.getResult()) {
            s_logger.error("SyncBcfTopologyCommand failed");
            return null;
        }
        return syncAnswer.getHash();
    }

    public String syncTopologyToBcfHost(HostVO bigswitchBcfHost, boolean natEnabled){
        SyncBcfTopologyCommand syncCmd;
        if(natEnabled){
            syncCmd = new SyncBcfTopologyCommand(true, true);
        } else {
            syncCmd = new SyncBcfTopologyCommand(true, false);
        }
        BcfAnswer syncAnswer = (BcfAnswer) _agentMgr.easySend(bigswitchBcfHost.getId(), syncCmd);
        if (syncAnswer == null || !syncAnswer.getResult()) {
            s_logger.error("SyncBcfTopologyCommand failed");
            return null;
        }
        return syncAnswer.getHash();
    }

    public BcfAnswer sendBcfCommandWithNetworkSyncCheck(BcfCommand cmd, Network network)throws IllegalArgumentException{
        // get registered Big Switch controller
        ControlClusterData cluster = getControlClusterData(network.getPhysicalNetworkId());
        if(cluster.getMaster()==null){
            return new BcfAnswer(cmd, new CloudRuntimeException("Big Switch Network controller temporarily unavailable"));
        }

        TopologyData topo = getTopology(network.getPhysicalNetworkId());

        cmd.setTopology(topo);
        BcfAnswer answer =  (BcfAnswer) _agentMgr.easySend(cluster.getMaster().getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error ("BCF API Command failed");
            throw new IllegalArgumentException("Failed API call to Big Switch Network plugin");
        }

        String newHash = answer.getHash();
        if (cmd.isTopologySyncRequested()) {
            newHash = syncTopologyToBcfHost(cluster.getMaster());
        }
        if(newHash != null){
            commitTopologyHash(network.getPhysicalNetworkId(), newHash);
        }

        HostVO slave = cluster.getSlave();
        if(slave != null){
            TopologyData newTopo = getTopology(network.getPhysicalNetworkId());
            CacheBcfTopologyCommand cacheCmd = new CacheBcfTopologyCommand(newTopo);
            _agentMgr.easySend(cluster.getSlave().getId(), cacheCmd);
        }

        return answer;
    }

    @DB
    public Boolean commitTopologyHash(long physicalNetworkId, final String hash) {
        final List<BigSwitchBcfDeviceVO> devices = _bigswitchBcfDao.listByPhysicalNetwork(physicalNetworkId);

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                for (BigSwitchBcfDeviceVO d: devices){
                    d.setHash(hash);
                    _bigswitchBcfDao.update(d.getId(), d);
                }
                return true;
            }
        });
    }

    public Boolean isNatEnabled(){
        List<BigSwitchBcfDeviceVO> devices = _bigswitchBcfDao.listAll();
        if(devices != null && !devices.isEmpty()){
            return devices.get(0).getNat();
        } else {
            return false;
        }
    }

    public Integer getSubnetMaskLength(String maskString){
        if(!IPAddress.isValidIPv4(maskString)){
            return null;
        }
        String[] octets = maskString.split("\\.");
        Integer bits = 0;
        for (String o: octets){
            switch(o){
            case "255":
                bits += 8;
                continue;
            case "254":
                bits += 7;
                return bits;
            case "252":
                bits += 6;
                return bits;
            case "248":
                bits += 5;
                return bits;
            case "240":
                bits += 4;
                return bits;
            case "224":
                bits += 3;
                return bits;
            case "192":
                bits += 2;
                return bits;
            case "128":
                bits += 1;
                return bits;
            case "0":
                return bits;
            default:
                throw new NumberFormatException("non-contiguous subnet mask not supported");
            }
        }
        return bits;
    }
}
