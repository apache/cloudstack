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
package com.cloud.agent.resource.virtualnetwork.facade;

import com.cloud.agent.api.routing.SetBgpPeersCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.FileConfigItem;
import com.cloud.agent.resource.virtualnetwork.ScriptConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;

import org.apache.cloudstack.network.BgpPeerTO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class SetBgpPeersConfigItemTest {


    @Test
    public void testSetBgpPeersConfigItem() {
        BgpPeerTO bgpPeerTO = Mockito.mock(BgpPeerTO.class);
        List<BgpPeerTO> bgpPeerTOs = new ArrayList<>();
        bgpPeerTOs.add(bgpPeerTO);
        SetBgpPeersCommand command = new SetBgpPeersCommand(bgpPeerTOs);

        SetBgpPeersConfigItem setBgpPeersConfigItem = new SetBgpPeersConfigItem();

        List<ConfigItem> configItems = setBgpPeersConfigItem.generateConfig(command);
        Assert.assertNotNull(configItems);

        Assert.assertEquals(2, configItems.size());
        Assert.assertTrue(configItems.get(0) instanceof FileConfigItem);
        Assert.assertTrue(configItems.get(1) instanceof ScriptConfigItem);

        Assert.assertEquals(VRScripts.CONFIG_PERSIST_LOCATION, ((FileConfigItem) configItems.get(0)).getFilePath());
        Assert.assertTrue((((FileConfigItem) configItems.get(0)).getFileName().startsWith(VRScripts.BGP_PEERS_CONFIG)));
        Assert.assertEquals(VRScripts.UPDATE_CONFIG, ((ScriptConfigItem) configItems.get(1)).getScript());
    }
}
