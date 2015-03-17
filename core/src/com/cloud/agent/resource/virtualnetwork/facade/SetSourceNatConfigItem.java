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

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;

public class SetSourceNatConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final LinkedList<ConfigItem> cfg = new LinkedList<>();

        /* FIXME This seems useless as we already pass this info with the ipassoc
         * SetSourceNatCommand command = (SetSourceNatCommand) cmd;
         * IpAddressTO pubIP = command.getIpAddress();
         * String dev = "eth" + pubIP.getNicDevId();
         * String args = "-A";
         * args += " -l ";
         * args += pubIP.getPublicIp();
         * args += " -c ";
         * args += dev;
         * cfg.add(new ScriptConfigItem(VRScripts.VPC_SOURCE_NAT, args));
         */

        return cfg;
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        return null;
    }
}