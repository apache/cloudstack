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
package com.cloud.tungsten;

import com.cloud.network.Networks;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiConnectorFactory;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class TungstenResourceManagerImpl implements TungstenResourceManager {

    private static final Logger S_LOGGER = Logger.getLogger(TungstenResourceManagerImpl.class);

    @Inject
    NetworkDao _networksDao;

    public ApiConnector getApiConnector(TungstenProvider tungstenProvider) {
        return ApiConnectorFactory.build(tungstenProvider.getHostname(), Integer.parseInt(tungstenProvider.getPort()));
    }

    public void deleteTungstenPublicNetwork(PhysicalNetworkVO pNetwork, TungstenProvider tungstenProvider) throws IOException {

        ApiConnector _api = getApiConnector(tungstenProvider);
        List<NetworkVO> publicNetworks = _networksDao.listByZoneAndTrafficType(pNetwork.getDataCenterId(), Networks.TrafficType.Public);

        for(NetworkVO publicNetwork : publicNetworks) {
            VirtualNetwork virtualNetwork = (VirtualNetwork) _api.findById(VirtualNetwork.class, publicNetwork.getUuid());
            List<ObjectReference<ApiPropertyBase>> vmiList = virtualNetwork.getVirtualMachineInterfaceBackRefs();
            for (ObjectReference<ApiPropertyBase> vmi : vmiList) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) _api.findById(VirtualMachineInterface.class, vmi.getUuid());
                //Remove virtual machine interface of the public network from tungsten
                deleteVmiFromTungsten(_api, virtualMachineInterface);
            }
            //Remove the logical router from tungsten
            _api.delete(LogicalRouter.class, virtualNetwork.getLogicalRouterBackRefs().get(0).getUuid());
            //Remove the floating ips of the public network from tungsten
            deleteFloatingIpsFromTungsten(_api, virtualNetwork.getFloatingIpPools());
            //Remove public network from tungsten
            _api.delete(VirtualNetwork.class, virtualNetwork.getUuid());
        }
    }

    private void deleteVmiFromTungsten(ApiConnector api, VirtualMachineInterface vmi) throws IOException {
        List<ObjectReference<ApiPropertyBase>> instanceIps = vmi.getInstanceIpBackRefs();
        if(instanceIps != null) {
            for (ObjectReference<ApiPropertyBase> instanceIp : instanceIps) {
                api.delete(InstanceIp.class, instanceIp.getUuid());
            }
        }
        api.delete(VirtualMachineInterface.class, vmi.getUuid());
    }

    private void deleteFloatingIpsFromTungsten(ApiConnector api, List<ObjectReference<ApiPropertyBase>> floatingIps) throws IOException {
        if(floatingIps != null) {
            for (ObjectReference<ApiPropertyBase> floatingIp : floatingIps) {
                api.delete(FloatingIpPool.class, floatingIp.getUuid());
            }
        }
    }
}
