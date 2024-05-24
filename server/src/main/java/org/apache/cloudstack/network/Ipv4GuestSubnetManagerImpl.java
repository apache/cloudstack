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

package org.apache.cloudstack.network;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLifecycleBase;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4GuestSubnetsCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.datacenter.dao.DataCenterIpv4GuestSubnetDao;
import org.apache.cloudstack.network.dao.Ipv4GuestSubnetNetworkMapDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


public class Ipv4GuestSubnetManagerImpl extends ComponentLifecycleBase implements Ipv4GuestSubnetManager {

    @Inject
    DataCenterIpv4GuestSubnetDao dataCenterIpv4GuestSubnetDao;
    @Inject
    Ipv4GuestSubnetNetworkMapDao ipv4GuestSubnetNetworkMapDao;


    @Override
    public String getConfigComponentName() {
        return Ipv4GuestSubnetManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {};
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv4GuestSubnetCmd.class);
        cmdList.add(DeleteIpv4GuestSubnetCmd.class);
        cmdList.add(ListIpv4GuestSubnetsCmd.class);
        cmdList.add(UpdateIpv4GuestSubnetCmd.class);
        cmdList.add(CreateIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(ListIpv4SubnetsForGuestNetworkCmd.class);
        cmdList.add(DeleteIpv4SubnetForGuestNetworkCmd.class);
        return cmdList;
    }


    @Override
    public DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4GuestSubnetCmd cmd) {
        return null;
    }

    @Override
    public DataCenterIpv4SubnetResponse createDataCenterIpv4SubnetResponse(DataCenterIpv4GuestSubnet subnet) {
        return null;
    }

    @Override
    public boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4GuestSubnetCmd cmd) {
        return false;
    }

    @Override
    public DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4GuestSubnetCmd cmd) {
        return null;
    }

    @Override
    public List<? extends DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4GuestSubnetsCmd cmd) {
        return null;
    }

    @Override
    public DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4GuestSubnetCmd cmd) {
        return null;
    }

    @Override
    public DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4GuestSubnetCmd cmd) {
        return null;
    }

    @Override
    public Ipv4GuestSubnetNetworkMap createIpv4SubnetForGuestNetwork(CreateIpv4SubnetForGuestNetworkCmd cmd) {
        if (ObjectUtils.allNotNull(cmd.getSubnet(), cmd.getCidrSize())) {
            throw new InvalidParameterValueException("subnet and cidrsize are mutually exclusive");
        }
        DataCenterIpv4GuestSubnet parent = dataCenterIpv4GuestSubnetDao.findById(cmd.getParentId());
        if (parent == null) {
            throw new InvalidParameterValueException("the parent subnet is invalid");
        }
        if (cmd.getSubnet() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getSubnet());
        } else if (cmd.getCidrSize() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getCidrSize());
        }
        return null;
    }

    @Override
    public boolean deleteIpv4SubnetForGuestNetwork(DeleteIpv4SubnetForGuestNetworkCmd cmd) {
        return false;
    }

    @Override
    public List<? extends Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd cmd) {
        return null;
    }

    @Override
    public Ipv4SubnetForGuestNetworkResponse createIpv4SubnetForGuestNetworkResponse(Ipv4GuestSubnetNetworkMap subnet) {
        return null;
    }

    @Override
    public void getOrCreateIpv4SubnetForGuestNetwork(Network network, String networkCidr) {

    }

    @Override
    public void getOrCreateIpv4SubnetForGuestNetwork(Network network, Integer networkCidrSize) {

    }

    private Ipv4GuestSubnetNetworkMap getIpv4SubnetForAccount(Account account, Integer networkCidrSize) {
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetForAccount(Account account, Integer networkCidrSize) {
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, Integer networkCidrSize) {
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, String networkCidr) {
        return null;
    }
}
