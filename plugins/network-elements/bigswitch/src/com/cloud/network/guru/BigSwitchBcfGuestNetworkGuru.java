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

package com.cloud.network.guru;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateBcfAttachmentCommand;
import com.cloud.agent.api.CreateBcfRouterCommand;
import com.cloud.agent.api.CreateBcfRouterInterfaceCommand;
import com.cloud.agent.api.CreateBcfSegmentCommand;
import com.cloud.agent.api.DeleteBcfSegmentCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.BigSwitchBcfDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.bigswitch.BigSwitchBcfUtils;
import com.cloud.network.dao.BigSwitchBcfDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * BigSwitchBcfGuestNetworkGuru is responsible for creating and removing virtual networks.
 * Current implementation leverages GuestNetworkGuru to create VLAN based virtual networks
 * and map it to a Big Switch Big Cloud Fabric (BCF) Segment.  It is called by the NetworkOrchestrator.
 *
 * When a VM is created and needs to be plugged into a BCF segment network, BigSwitchBcfElement is
 * called by NetworkOrchestrator to create a "port" and an "attachment" for each nic, and
 * register them with the controller to be plugged into the corresponding network. It also
 * removes them when the VM is destroyed.
 */
@Local(value = NetworkGuru.class)
public class BigSwitchBcfGuestNetworkGuru extends GuestNetworkGuru implements NetworkMigrationResponder {
    private static final Logger s_logger = Logger.getLogger(BigSwitchBcfGuestNetworkGuru.class);

    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    BigSwitchBcfDao _bigswitchBcfDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    HostPodDao _podDao;
    @Inject
    NetworkService _netService;
    @Inject
    VlanDao _vlanDao;
    @Inject
    FirewallRulesDao _fwRulesDao;
    @Inject
    FirewallRulesCidrsDao _fwCidrsDao;
    @Inject
    NetworkACLItemDao _aclItemDao;
    @Inject
    NetworkACLItemCidrsDao _aclItemCidrsDao;

    private BigSwitchBcfUtils _bcfUtils = null;

    public BigSwitchBcfGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {IsolationMethod.BCF_SEGMENT};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, NetworkType networkType, PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated &&
            isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        // Check if the isolation type of the physical network is BCF_SEGMENT, then delegate GuestNetworkGuru to design
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        if (physnet == null || physnet.getIsolationMethods() == null || !physnet.getIsolationMethods().contains("BCF_SEGMENT")) {
            s_logger.debug("Refusing to design this network, the physical isolation type is not BCF_SEGMENT");
            return null;
        }

        List<BigSwitchBcfDeviceVO> devices = _bigswitchBcfDao.listByPhysicalNetwork(physnet.getId());
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitch Controller on physical network " + physnet.getName());
            return null;
        }
        for (BigSwitchBcfDeviceVO d: devices){
            s_logger.debug("BigSwitch Controller " + d.getUuid()
                    + " found on physical network " + physnet.getId());
        }

        s_logger.debug("Physical isolation type is BCF_SEGMENT, asking GuestNetworkGuru to design this network");
        NetworkVO networkObject = (NetworkVO)super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }
        // Override the broadcast domain type
        networkObject.setBroadcastDomainType(BroadcastDomainType.Vlan);

        return networkObject;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        bcfUtilsInit();

        long dcId = dest.getDataCenter().getId();

        long physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());

        NetworkVO implemented =
            new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId, false);

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        String vnetId = "";
        if (network.getBroadcastUri() == null) {
            vnetId = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), context.getReservationId(), UseSystemGuestVlans.valueIn(network.getAccountId()));
            if (vnetId == null) {
                throw new InsufficientVirtualNetworkCapacityException("Unable to allocate vnet as a " +
                        "part of network " + network + " implement ", DataCenter.class, dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnetId));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(),
                    EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assigned Zone Vlan: " + vnetId + " Network Id: " + network.getId(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }

        // Name is either the given name or the uuid
        String name = network.getName();
        if (name == null || name.isEmpty()) {
            name = ((NetworkVO)network).getUuid();
        }
        if (name.length() > 64) {
            name = name.substring(0, 63); // max length 64
        }

        // update fields in network object
        NetworkVO networkObject = (NetworkVO) network;

        // determine whether this is VPC network or stand-alone network
        Vpc vpc = null;
        if(network.getVpcId()!=null){
            vpc = _vpcDao.acquireInLockTable(network.getVpcId());
        }

        // use uuid of networkVO as network id in BSN
        String networkId = networkObject.getUuid();

        String tenantId;
        String tenantName;
        if (vpc != null) {
            tenantId = vpc.getUuid();
            tenantName = vpc.getName();
            _vpcDao.releaseFromLockTable(vpc.getId());
        } else {
            // use network in CS as tenant in BSN
            // for non-VPC networks, use network name and id as tenant name and id
            tenantId = networkId;
            tenantName = name;
        }

        // store tenantId in networkObject for NetworkOrchestrator implementNetwork use
        networkObject.setNetworkDomain(tenantId);
        // store tenant Id in implemented object for future actions (e.g., shutdown)
        implemented.setNetworkDomain(tenantId);
        String vlanStr = BroadcastDomainType.getValue(implemented.getBroadcastUri());

        // get public net info - needed to set up source nat gateway
        NetworkVO pubNet = _bcfUtils.getPublicNetwork(physicalNetworkId);

        // locate subnet info
        SearchCriteria<VlanVO> sc = _vlanDao.createSearchCriteria();
        sc.setParameters("network_id", pubNet.getId());
        VlanVO pubVlanVO = _vlanDao.findOneBy(sc);
        String pubVlanStr = pubVlanVO.getVlanTag();

        Integer vlan;
        if(StringUtils.isNumeric(vlanStr)){
            vlan = Integer.valueOf(vlanStr);
        } else {
            vlan = 0;
        }
        CreateBcfSegmentCommand cmd1 = new CreateBcfSegmentCommand(tenantId, tenantName, networkId, name, vlan);

        _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd1, networkObject);

        if(_bcfUtils.isNatEnabled()){
            Integer pvlan;
            if(StringUtils.isNumeric(pubVlanStr)){
                pvlan = Integer.valueOf(pubVlanStr);
            } else {
                pvlan = 0;
            }
            CreateBcfSegmentCommand cmd2 = new CreateBcfSegmentCommand("external", "external", "external", "external", pvlan);

            _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd2, networkObject);

            CreateBcfRouterCommand cmd3 = new CreateBcfRouterCommand(tenantId);

            _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd3, network);

            CreateBcfRouterInterfaceCommand cmd5 = new CreateBcfRouterInterfaceCommand(tenantId, network.getUuid(), network.getCidr(),
                    network.getGateway(), network.getName());

            _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd5, network);
        }
        return implemented;
    }



    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        // Have GuestNetworkGuru reserve for us
        super.reserve(nic, network, vm, dest, context);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        NetworkVO networkObject = _networkDao.findById(profile.getId());
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.Vlan || networkObject.getBroadcastUri() == null) {
            s_logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        bcfUtilsInit();

        // tenantId stored in network domain field at creation
        String tenantId = networkObject.getNetworkDomain();

        String networkId = networkObject.getUuid();

        DeleteBcfSegmentCommand cmd = new DeleteBcfSegmentCommand(tenantId, networkId);

        _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, networkObject);

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return super.trash(network, offering);
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network,
            VirtualMachineProfile vm, DeployDestination dest,
            ReservationContext context) {
        bcfUtilsInit();

        // get arguments for CreateBcfAttachmentCommand
        // determine whether this is VPC network or stand-alone network
        Vpc vpc = null;
        if(network.getVpcId()!=null){
            vpc = _vpcDao.acquireInLockTable(network.getVpcId());
        }

        String networkId = network.getUuid();

        String tenantId;
        String tenantName;
        if (vpc != null) {
            tenantId = vpc.getUuid();
            tenantName = vpc.getName();
            boolean released = _vpcDao.releaseFromLockTable(vpc.getId());
            s_logger.debug("BCF guru release lock vpc id: " + vpc.getId()
                    + " released? " + released);
        } else {
            // use network id in CS as tenant in BSN
            // use network uuid as tenant id for non-VPC networks
            tenantId = networkId;
            tenantName = network.getName();
        }

        String hostname = dest.getHost().getName();
        long zoneId = network.getDataCenterId();
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
        if (dest.getHost().getHypervisorType() == HypervisorType.KVM){
            pgName = hostname;
        } else if (dest.getHost().getHypervisorType() == HypervisorType.VMware){
            pgName = hostname + "-" + vswitchName;
        }

        String nicId = nic.getUuid();
        Integer vlan = Integer.valueOf(BroadcastDomainType.getValue(nic.getIsolationUri()));
        String ipv4 = nic.getIPv4Address();
        String mac = nic.getMacAddress();

        CreateBcfAttachmentCommand cmd = new CreateBcfAttachmentCommand(tenantId,
                tenantName, networkId, pgName, nicId, vlan, ipv4, mac);

        _bcfUtils.sendBcfCommandWithNetworkSyncCheck(cmd, network);

        return true;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network,
            VirtualMachineProfile vm, ReservationContext src,
            ReservationContext dst) {
        s_logger.debug("BCF guru rollback migration");
    }

    @Override
    public void commitMigration(NicProfile nic, Network network,
            VirtualMachineProfile vm, ReservationContext src,
            ReservationContext dst) {
        s_logger.debug("BCF guru commit migration");
    }

    private void bcfUtilsInit(){
        if (_bcfUtils == null) {
            _bcfUtils = new BigSwitchBcfUtils(_networkDao, _nicDao,
                    _vmDao, _hostDao, _vpcDao, _bigswitchBcfDao,
                    _agentMgr, _vlanDao, _ipAddressDao, _fwRulesDao,
                    _fwCidrsDao, _aclItemDao, _aclItemCidrsDao, _networkModel);
        }
    }
}
