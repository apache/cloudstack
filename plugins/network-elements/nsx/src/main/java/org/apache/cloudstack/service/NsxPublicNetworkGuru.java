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
package org.apache.cloudstack.service;

import com.cloud.dc.VlanDetailsVO;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxTier1NatRuleCommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.cloudstack.utils.NsxHelper;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class NsxPublicNetworkGuru extends PublicNetworkGuru {

    @Inject
    private VlanDetailsDao vlanDetailsDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Inject
    private NsxControllerUtils nsxControllerUtils;
    @Inject
    private NsxService nsxService;

    private static final Logger s_logger = Logger.getLogger(NsxPublicNetworkGuru.class);

    public NsxPublicNetworkGuru() {
        super();
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("NSX Public network guru: allocate");
        // NicProfile createdProfile = super.allocate(network, nic, vm);

        IPAddressVO ipAddress = _ipAddressDao.findByIp(nic.getIPv4Address());
        if (ipAddress == null) {
            String err = String.format("Cannot find the IP address %s", nic.getIPv4Address());
            s_logger.error(err);
            throw new CloudRuntimeException(err);
        }
        Long vpcId = ipAddress.getVpcId();
        if (vpcId == null) {
            // TODO: Pass network.getId() to support isolated networks
            throw new CloudRuntimeException("TODO: Add support for isolated networks public network");
        }
        VpcVO vpc = vpcDao.findById(vpcId);
        if (vpc == null) {
            String err = String.format("Cannot find a VPC with ID %s", vpcId);
            s_logger.error(err);
            throw new CloudRuntimeException(err);
        }

        if (ipAddress.isSourceNat() && !ipAddress.isForSystemVms()) {
            VlanDetailsVO detail = vlanDetailsDao.findDetail(ipAddress.getVlanId(), ApiConstants.NSX_DETAIL_KEY);
            if (detail != null && detail.getValue().equalsIgnoreCase("true")) {
                long accountId = vpc.getAccountId();
                long domainId = vpc.getDomainId();
                long dataCenterId = vpc.getZoneId();
                boolean isForVpc = true;
                long resourceId = isForVpc ? vpc.getId() : network.getId();
                Network.Service[] services = { Network.Service.SourceNat };
                boolean sourceNatEnabled = vpcOfferingServiceMapDao.areServicesSupportedByVpcOffering(vpc.getVpcOfferingId(), services);

                s_logger.info(String.format("Creating Tier 1 Gateway for VPC %s", vpc.getName()));
                boolean result = nsxService.createVpcNetwork(dataCenterId, accountId, domainId, resourceId, vpc.getName(), sourceNatEnabled);
                if (!result) {
                    String msg = String.format("Error creating Tier 1 Gateway for VPC %s", vpc.getName());
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }

                String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(domainId, accountId, dataCenterId, resourceId, isForVpc);
                String translatedIp = ipAddress.getAddress().addr();
                s_logger.debug(String.format("Creating NSX Nat Rule for Tier1 GW %s for translated IP %s", tier1GatewayName, translatedIp));
                String natRuleId = NsxControllerUtils.getNsxNatRuleId(domainId, accountId, dataCenterId, resourceId, isForVpc);
                CreateNsxTier1NatRuleCommand cmd = NsxHelper.createNsxNatRuleCommand(domainId, accountId, dataCenterId, tier1GatewayName, "SNAT", translatedIp, natRuleId);
                NsxAnswer nsxAnswer = nsxControllerUtils.sendNsxCommand(cmd, dataCenterId);
                if (!nsxAnswer.getResult()) {
                    String msg = String.format("Could not create NSX Nat Rule on Tier1 Gateway %s for IP %s", tier1GatewayName, translatedIp);
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
            }
        }
        return nic;
    }
}
