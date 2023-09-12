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

package org.apache.cloudstack.network.opendaylight.api.enums;

public enum NeutronNorthboundEnum {

    NETWORKS_URI("/controller/nb/v2/neutron/networks"),
    NETWORK_PARAM_URI("/controller/nb/v2/neutron/networks/{0}"),

    PORTS_URI("/controller/nb/v2/neutron/ports"),
    PORTS_PARAM_URI("/controller/nb/v2/neutron/ports/{0}"),

    NODES_URI("/controller/nb/v2/connectionmanager/nodes"),
    NODE_PARAM_URI("/controller/nb/v2/connectionmanager/node/{0}/{1}"),
    NODE_PORT_PER_NODE_URI("/controller/nb/v2/connectionmanager/node/{0}/address/{1}/port/{2}"),
    NODE_PORT_PER_TYPE_URI("/controller/nb/v2/connectionmanager/node/{0}/{1}/address/{2}/port/{3}");

    private String uri;

    private NeutronNorthboundEnum(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
}
