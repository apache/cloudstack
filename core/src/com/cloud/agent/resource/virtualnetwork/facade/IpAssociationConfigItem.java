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

package com.cloud.agent.resource.virtualnetwork.facade;

import java.util.LinkedList;
import java.util.List;

import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.IpAddress;
import com.cloud.agent.resource.virtualnetwork.model.IpAssociation;

public class IpAssociationConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final IpAssocCommand command = (IpAssocCommand) cmd;

        final List<IpAddress> ips = new LinkedList<IpAddress>();

        for (final IpAddressTO ip : command.getIpAddresses()) {
            final IpAddress ipAddress = new IpAddress(ip.getPublicIp(), ip.isSourceNat(), ip.isAdd(), ip.isOneToOneNat(), ip.isFirstIP(), ip.getVlanGateway(), ip.getVlanNetmask(),
                    ip.getVifMacAddress(), ip.getNicDevId(), ip.isNewNic());
            ips.add(ipAddress);
        }

        final IpAssociation ipAssociation = new IpAssociation(ips.toArray(new IpAddress[ips.size()]));

        return generateConfigItems(ipAssociation);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.IP_ASSOCIATION_CONFIG;

        return super.generateConfigItems(configuration);
    }
}