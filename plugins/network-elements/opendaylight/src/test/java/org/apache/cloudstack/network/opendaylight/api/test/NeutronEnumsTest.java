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

package org.apache.cloudstack.network.opendaylight.api.test;

import java.text.MessageFormat;

import junit.framework.Assert;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.enums.NeutronNorthboundEnum;
import org.junit.Test;

public class NeutronEnumsTest {

    @Test
    public <T> void enumsUrlFormatTest1() throws NeutronRestApiException {
        String netUrl = NeutronNorthboundEnum.NETWORK_PARAM_URI.getUri();
        netUrl = MessageFormat.format(netUrl, netId);

        Assert.assertEquals(NETWORK_PARAM_URI, netUrl);
    }

    @Test
    public <T> void enumsUrlFormatTest2() throws NeutronRestApiException {
        String portUrl = NeutronNorthboundEnum.PORTS_PARAM_URI.getUri();
        portUrl = MessageFormat.format(portUrl, portId);

        Assert.assertEquals(PORTS_PARAM_URI, portUrl);
    }

    @Test
    public <T> void enumsUrlFormatTest3() throws NeutronRestApiException {
        String nodedelUrl = NeutronNorthboundEnum.NODE_PARAM_URI.getUri();
        nodedelUrl = MessageFormat.format(nodedelUrl, "test", nodeId);

        Assert.assertEquals(NODE_PARAM_URI, nodedelUrl);
    }

    @Test
    public <T> void enumsUrlFormatTest4() throws NeutronRestApiException {
        String nodeV1Url = NeutronNorthboundEnum.NODE_PORT_PER_NODE_URI.getUri();
        nodeV1Url = MessageFormat.format(nodeV1Url, nodeId, ip, String.valueOf(port));

        Assert.assertEquals(NODE_PORT_PER_NODE_URI, nodeV1Url);
    }

    @Test
    public <T> void enumsUrlFormatTest5() throws NeutronRestApiException {
        String nodeV2Url = NeutronNorthboundEnum.NODE_PORT_PER_TYPE_URI.getUri();
        nodeV2Url = MessageFormat.format(nodeV2Url, "test", nodeId, ip, String.valueOf(port));

        Assert.assertEquals(NODE_PORT_PER_TYPE_URI, nodeV2Url);
    }

    static String NETWORK_PARAM_URI = "/controller/nb/v2/neutron/networks/0AACEED5-A688-429A-92FC-E1C9E4EEEE98";

    static String PORTS_PARAM_URI = "/controller/nb/v2/neutron/ports/F4267875-0C85-4829-8434-901A08691C6E";

    static String NODE_PARAM_URI = "/controller/nb/v2/connectionmanager/node/test/ca31aa7f-84c7-416d-bc00-1f84927367e0";
    static String NODE_PORT_PER_NODE_URI = "/controller/nb/v2/connectionmanager/node/ca31aa7f-84c7-416d-bc00-1f84927367e0/address/1.1.1.1/port/6400";
    static String NODE_PORT_PER_TYPE_URI = "/controller/nb/v2/connectionmanager/node/test/ca31aa7f-84c7-416d-bc00-1f84927367e0/address/1.1.1.1/port/6400";

    static String netId = "0AACEED5-A688-429A-92FC-E1C9E4EEEE98";
    static String portId = "F4267875-0C85-4829-8434-901A08691C6E";
    static String nodeId = "ca31aa7f-84c7-416d-bc00-1f84927367e0";
    static String ip = "1.1.1.1";
    static int port = 6400;
}
