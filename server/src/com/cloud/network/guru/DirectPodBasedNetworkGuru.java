
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
package com.cloud.network.guru;

import java.net.URI;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.configuration.ZoneConfig;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class DirectPodBasedNetworkGuru extends DirectNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(DirectPodBasedNetworkGuru.class);

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    PodVlanMapDao _podVlanDao;

    @Override
    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        // this guru handles system Direct pod based network
        if (dc.getNetworkType() == NetworkType.Basic && isMyTrafficType(offering.getTrafficType())) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest Direct Pod based networks");
            return false;
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {

        DataCenterVO dc = _dcDao.findById(network.getDataCenterId());
        ReservationStrategy rsStrategy = ReservationStrategy.Start;
        _dcDao.loadDetails(dc);
        String dhcpStrategy = dc.getDetail(ZoneConfig.DhcpStrategy.key());
        if ("external".equalsIgnoreCase(dhcpStrategy)) {
            rsStrategy = ReservationStrategy.Create;
        }
        
        if (nic != null && nic.getRequestedIpv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }
       
        if (nic == null) {
            nic = new NicProfile(rsStrategy, null, null, null, null);
        } else if (nic.getIp4Address() == null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }
        
        if (rsStrategy == ReservationStrategy.Create) {
            String mac = _networkModel.getNextAvailableMacAddressInNetwork(network.getId());
            nic.setMacAddress(mac);
        }
        return nic;
    }

    @Override @DB
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        
        String oldIp = nic.getIp4Address();
        boolean getNewIp = false;
        
        if (oldIp == null) {
            getNewIp = true;
        } else {
            // we need to get a new ip address if we try to deploy a vm in a different pod
            IPAddressVO ipVO = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), oldIp);
            if (ipVO != null) {
                PodVlanMapVO mapVO = _podVlanDao.listPodVlanMapsByVlan(ipVO.getVlanId());
                if (mapVO.getPodId() != dest.getPod().getId()) {
                    Transaction txn = Transaction.currentTxn();
                    txn.start();
                    
                    //release the old ip here
                    _networkMgr.markIpAsUnavailable(ipVO.getId());
                    _ipAddressDao.unassignIpAddress(ipVO.getId());
                    
                    txn.commit();
                    
                    nic.setIp4Address(null);
                    getNewIp = true;
                }
            }
        }
        
        if (getNewIp) {
            //we don't set reservationStrategy to Create because we need this method to be called again for the case when vm fails to deploy in Pod1, and we try to redeploy it in Pod2 
            getIp(nic, dest.getPod(), vm, network);
        }
        
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }

    @DB
    protected void getIp(NicProfile nic, Pod pod, VirtualMachineProfile<? extends VirtualMachine> vm, Network network) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
        DataCenter dc = _dcDao.findById(pod.getDataCenterId());
        if (nic.getIp4Address() == null) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            
            PublicIp ip = null;
            List<PodVlanMapVO> podRefs = _podVlanDao.listPodVlanMapsByPod(pod.getId());
            String podRangeGateway = null;
            if (!podRefs.isEmpty()) {
                podRangeGateway = _vlanDao.findById(podRefs.get(0).getVlanDbId()).getVlanGateway();
            }
            //Get ip address from the placeholder and don't allocate a new one
            if (vm.getType() == VirtualMachine.Type.DomainRouter) {
                Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, pod.getId());
                if (placeholderNic != null) {
                    IPAddressVO userIp = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), placeholderNic.getIp4Address());
                    ip = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                    s_logger.debug("Nic got an ip address " + placeholderNic.getIp4Address() + " stored in placeholder nic for the network " + network + " and gateway " + podRangeGateway);
                }
            }
            
            if (ip == null) {
                ip = _networkMgr.assignPublicIpAddress(dc.getId(), pod.getId(), vm.getOwner(), VlanType.DirectAttached, network.getId(), null, false);
            }
            
            nic.setIp4Address(ip.getAddress().toString());
            nic.setFormat(AddressFormat.Ip4);
            nic.setGateway(ip.getGateway());
            nic.setNetmask(ip.getNetmask());
            if (ip.getVlanTag() != null && ip.getVlanTag().equalsIgnoreCase(Vlan.UNTAGGED)) {
                nic.setIsolationUri(URI.create("ec2://" + Vlan.UNTAGGED));
                nic.setBroadcastUri(URI.create("vlan://" + Vlan.UNTAGGED));
                nic.setBroadcastType(BroadcastDomainType.Native);
            }
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
            
            //save the placeholder nic if the vm is the Virtual router
            if (vm.getType() == VirtualMachine.Type.DomainRouter) {
                Nic placeholderNic = _networkModel.getPlaceholderNicForRouter(network, pod.getId());
                if (placeholderNic == null) {
                    s_logger.debug("Saving placeholder nic with ip4 address " + nic.getIp4Address() + " for the network " + network);
                    _networkMgr.savePlaceholderNic(network, nic.getIp4Address(), VirtualMachine.Type.DomainRouter);
                }
            }
            txn.commit();
        }
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }
    
}
