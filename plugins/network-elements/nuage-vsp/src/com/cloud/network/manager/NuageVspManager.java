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

package com.cloud.network.manager;

import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.commands.UpdateNuageVspDeviceCmd;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.network.Network;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;

import java.util.List;

public interface NuageVspManager extends PluggableService {

    static final String nuageVspSharedNetworkOfferingWithSGServiceName = "DefaultNuageVspSharedNetworkOfferingWithSGService";

    static final String nuageVPCOfferingName = "Nuage VSP VPC Offering";

    static final String nuageVPCOfferingDisplayText = "Nuage VSP VPC Offering";

    static final ConfigKey<Boolean> NuageVspConfigDns = new ConfigKey<Boolean>(Boolean.class, "nuagevsp.configure.dns", "Advanced", "true",
            "Defines if NuageVsp plugin needs to configure DNS setting for a VM or not. True will configure the DNS and false will not configure the DNS settings", true,
            Scope.Global, null);

    static final ConfigKey<Boolean> NuageVspDnsExternal = new ConfigKey<Boolean>(
            Boolean.class,
            "nuagevsp.dns.external",
            "Advanced",
            "true",
            "Defines if NuageVsp plugin needs to configure either internal or external DNS server configured during Zone provisioning. "
                    + "Value true uses the external DNS and value false uses the internal DNS to configure in the VM. But, this flag depends on "
                    + "nuagevsp.configure.dns. Only if nuagevsp.configure.dns is set to true, DNS server will be configured in the VM. "
                    + "If nuagevsp.configure.dns is false, DNS server will not be configured in the VM. Default value for this flag is true",
            true, Scope.Global, null);

    static final ConfigKey<String> NuageVspConfigGateway = new ConfigKey<String>(String.class, "nuagevsp.configure.gateway.systemid", "Advanced", "",
            "Defines the systemID of the gateway configured in VSP", true, Scope.Global, null);

    static final ConfigKey<String> NuageVspSharedNetworkDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.sharedntwk.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for shared networks", true, Scope.Global, null);

    static final ConfigKey<String> NuageVspVpcDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.vpc.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for VPCs", true, Scope.Global, null);

    static final ConfigKey<String> NuageVspIsolatedNetworkDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.isolatedntwk.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for isolated networks", true, Scope.Global, null);

    NuageVspDeviceVO addNuageVspDevice(AddNuageVspDeviceCmd cmd);

    NuageVspDeviceVO updateNuageVspDevice(UpdateNuageVspDeviceCmd cmd);

    NuageVspDeviceResponse createNuageVspDeviceResponse(NuageVspDeviceVO nuageVspDeviceVO);

    boolean deleteNuageVspDevice(DeleteNuageVspDeviceCmd cmd);

    List<NuageVspDeviceVO> listNuageVspDevices(ListNuageVspDevicesCmd cmd);

    List<String> getDnsDetails(Network network);

    List<String> getGatewaySystemIds();

}
