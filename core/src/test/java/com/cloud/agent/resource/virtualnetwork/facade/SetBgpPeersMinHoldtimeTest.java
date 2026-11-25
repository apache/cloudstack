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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.cloudstack.network.BgpPeerTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.routing.SetBgpPeersCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.FileConfigItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The BGP peers command carries the minimum hold time to the router. The push file
 * holds it as min_holdtime beside the peers, so a router with BGP peers renders
 * bgp minimum-holdtime.
 */
public class SetBgpPeersMinHoldtimeTest {

    private SetBgpPeersCommand cmd;

    @Before
    public void setUp() {
        BgpPeerTO peer = new BgpPeerTO(1L, "198.51.100.1", null, 65100L, null, 10L, 64600L,
                "172.16.10.0/24", null, new HashMap<>());
        cmd = new SetBgpPeersCommand(Collections.singletonList(peer));
    }

    private JsonObject pushFile() {
        List<ConfigItem> items = new SetBgpPeersConfigItem().generateConfig(cmd);
        for (ConfigItem item : items) {
            if (item instanceof FileConfigItem) {
                return JsonParser.parseString(((FileConfigItem) item).getFileContents()).getAsJsonObject();
            }
        }
        throw new AssertionError("no push file in " + items);
    }

    @Test
    public void commandCarriesMinHoldtime() {
        cmd.setMinHoldtime(3);
        Assert.assertEquals(Integer.valueOf(3), cmd.getMinHoldtime());
    }

    // The push file holds the default minimum hold time next to the peers.
    @Test
    public void pushFileCarriesMinHoldtime() {
        cmd.setMinHoldtime(3);
        JsonObject file = pushFile();
        Assert.assertTrue("push file should hold min_holdtime: " + file, file.has("min_holdtime"));
        Assert.assertEquals(3, file.get("min_holdtime").getAsInt());
        Assert.assertEquals(1, file.getAsJsonArray("peers").size());
    }

    // A raised minimum hold time reaches the router as set.
    @Test
    public void pushFileCarriesRaisedMinHoldtime() {
        cmd.setMinHoldtime(9);
        Assert.assertEquals(9, pushFile().get("min_holdtime").getAsInt());
    }
}
