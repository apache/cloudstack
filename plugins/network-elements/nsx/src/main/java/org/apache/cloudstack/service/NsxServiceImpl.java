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

import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.utils.NsxControllerUtils;

import javax.inject.Inject;
import java.util.Objects;

public class NsxServiceImpl implements NsxService {
    @Inject
    NsxControllerUtils nsxControllerUtils;
    @Inject
    VpcDao vpcDao;

    public boolean createVpcNetwork(Long zoneId, long accountId, long domainId, long vpcId, String vpcName) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId, vpcId, vpcName);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteVpcNetwork(Long zoneId, long accountId, long domainId, Long vpcId, String vpcName) {
        DeleteNsxTier1GatewayCommand deleteNsxTier1GatewayCommand =
                new DeleteNsxTier1GatewayCommand(domainId, accountId, zoneId, vpcId, vpcName);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteNetwork(long zoneId, long accountId, long domainId, NetworkVO network) {
        String vpcName = null;
        if (Objects.nonNull(network.getVpcId())) {
            VpcVO vpc = vpcDao.findById(network.getVpcId());
            vpcName = Objects.nonNull(vpc) ? vpc.getName() : null;
        }
        DeleteNsxSegmentCommand deleteNsxSegmentCommand = new DeleteNsxSegmentCommand(domainId, accountId, zoneId,
                network.getVpcId(), vpcName, network.getId(), network.getName());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxSegmentCommand, network.getDataCenterId());
        return result.getResult();
    }

    public boolean createStaticNatRule(long zoneId, long domainId, long accountId, long vpcId,
                                       long vmId, String publicIp, String vmIp) {
        VpcVO vpc = vpcDao.findById(vpcId);
        if (Objects.isNull(vpc)) {
            throw new CloudRuntimeException(String.format("Failed to find VPC with id: %s", vpcId));
        }
        CreateNsxStaticNatCommand createNsxStaticNatCommand = new CreateNsxStaticNatCommand(domainId, accountId, zoneId,
                vpcId, vpc.getName(), vmId, publicIp, vmIp);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxStaticNatCommand, zoneId);
        return result.getResult();
    }

    public boolean deleteStaticNatRule(long zoneId, long domainId, long accountId, long vpcId) {
        VpcVO vpc = vpcDao.findById(vpcId);
        if (Objects.isNull(vpc)) {
            throw new CloudRuntimeException(String.format("Failed to find VPC with id: %s", vpcId));
        }
        DeleteNsxStaticNatCommand deleteNsxStaticNatCommand = new DeleteNsxStaticNatCommand(domainId, accountId, zoneId,
                vpcId, vpc.getName());
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(deleteNsxStaticNatCommand, zoneId);
        return result.getResult();
    }
}
