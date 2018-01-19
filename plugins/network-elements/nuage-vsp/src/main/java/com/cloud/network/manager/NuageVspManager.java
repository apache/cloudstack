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

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;

import com.cloud.agent.api.manager.EntityExistsCommand;
import com.cloud.api.commands.AddNuageVspDeviceCmd;
import com.cloud.api.commands.AssociateNuageVspDomainTemplateCmd;
import com.cloud.api.commands.DeleteNuageVspDeviceCmd;
import com.cloud.api.commands.ListNuageVspDevicesCmd;
import com.cloud.api.commands.ListNuageVspDomainTemplatesCmd;
import com.cloud.api.commands.UpdateNuageVspDeviceCmd;
import com.cloud.api.response.NuageVlanIpRangeResponse;
import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.dc.Vlan;
import com.cloud.api.response.NuageVspDomainTemplateResponse;
import com.cloud.host.HostVO;
import com.cloud.network.Network;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.utils.component.PluggableService;



public interface NuageVspManager extends PluggableService {

    String nuageVspSharedNetworkOfferingWithSGServiceName = "DefaultNuageVspSharedNetworkOfferingWithSGService";

    String nuageVPCOfferingName = "Nuage VSP VPC Offering";

    String nuageVPCOfferingDisplayText = "Nuage VSP VPC Offering";

    String nuageDomainTemplateDetailName = "domainTemplateName";

    String nuageUnderlayVlanIpRangeDetailKey = "nuage.underlay";

    ConfigKey<Boolean> NuageVspConfigDns = new ConfigKey<Boolean>(Boolean.class, "nuagevsp.configure.dns", "Advanced", "true",
            "Defines if NuageVsp plugin needs to configure DNS setting for a VM or not. True will configure the DNS and false will not configure the DNS settings", true,
            Scope.Global, null);

    ConfigKey<Boolean> NuageVspDnsExternal = new ConfigKey<Boolean>(
            Boolean.class,
            "nuagevsp.dns.external",
            "Advanced",
            "true",
            "Defines if NuageVsp plugin needs to configure either internal or external DNS server configured during Zone provisioning. "
                    + "Value true uses the external DNS and value false uses the internal DNS to configure in the VM. But, this flag depends on "
                    + "nuagevsp.configure.dns. Only if nuagevsp.configure.dns is set to true, DNS server will be configured in the VM. "
                    + "If nuagevsp.configure.dns is false, DNS server will not be configured in the VM. Default value for this flag is true",
            true, Scope.Global, null);

    ConfigKey<String> NuageVspConfigGateway = new ConfigKey<String>(String.class, "nuagevsp.configure.gateway.systemid", "Advanced", "",
            "Defines the systemID of the gateway configured in VSP", true, Scope.Global, null);

    ConfigKey<String> NuageVspSharedNetworkDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.sharedntwk.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for shared networks", true, Scope.Global, null);

    ConfigKey<String> NuageVspVpcDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.vpc.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for VPCs", true, Scope.Global, null);

    ConfigKey<String> NuageVspIsolatedNetworkDomainTemplateName = new ConfigKey<String>(String.class, "nuagevsp.isolatedntwk.domaintemplate.name",
            "Advanced", "", "Defines if NuageVsp plugin needs to use pre created Domain Template configured in VSP for isolated networks", true, Scope.Global, null);

    String NETWORK_METADATA_VSD_DOMAIN_ID = "vsdDomainId";

    String NETWORK_METADATA_VSD_ZONE_ID = "vsdZoneId";

    String NETWORK_METADATA_VSD_SUBNET_ID = "vsdSubnetId";

    String NETWORK_METADATA_VSD_MANAGED = "isVsdManaged";

    String CMSID_CONFIG_KEY = "nuagevsp.cms.id";

    String NUAGE_VSP_ISOLATION = "VSP";

    NuageVspDeviceVO addNuageVspDevice(AddNuageVspDeviceCmd cmd);

    NuageVspDeviceVO updateNuageVspDevice(UpdateNuageVspDeviceCmd cmd);

    NuageVspDeviceResponse createNuageVspDeviceResponse(NuageVspDeviceVO nuageVspDeviceVO);

    boolean deleteNuageVspDevice(DeleteNuageVspDeviceCmd cmd);

    List<NuageVspDeviceVO> listNuageVspDevices(ListNuageVspDevicesCmd cmd);

    List<String> getDnsDetails(long dataCenterId);

    List<String> getGatewaySystemIds();

    HostVO getNuageVspHost(long physicalNetworkId);

    boolean updateNuageUnderlayVlanIpRange(long vlanIpRangeId, boolean enabled);

    List<NuageVlanIpRangeResponse> filterNuageVlanIpRanges(List<? extends Vlan> vlanIpRanges, Boolean underlay);

    List<NuageVspDomainTemplateResponse> listNuageVspDomainTemplates(ListNuageVspDomainTemplatesCmd cmd);

    List<NuageVspDomainTemplateResponse> listNuageVspDomainTemplates(long domainId, String keyword, Long zoneId, Long physicalNetworkId);

    /**
     * Associates a Nuage Vsp domain template with a
     * @param cmd
     * @return
     */
    boolean associateNuageVspDomainTemplate(AssociateNuageVspDomainTemplateCmd cmd);

    /**
     * Queries the VSD to check if the entity provided in the entityCmd exists on the VSD
     * @param cmd entityCommand which contains the ACS class of the entity and the UUID
     * @param hostId the hostId of the VSD
     * @return true if an entity exists with the UUI on the VSD, otherwise false.
     */
    boolean entityExist(EntityExistsCommand cmd, Long hostId);

    /**
     * Sets the preconfigured domain template for a given network
     * @param network
     * @param domainTemplateName
     */
    void setPreConfiguredDomainTemplateName(Network network, String domainTemplateName);

    /**
     * Returns the current pre configured domain template for a given network
     * @param network
     * @return
     */
    String getPreConfiguredDomainTemplateName(Network network);

    /**
     * Checks if a given domain template exists or not on the VSD.
     * @param domainId
     * @param domainTemplate The name of the domain template for which we need to query the VSD.
     * @param zoneId zoneId OR PhysicalNetworkId needs to be provided.
     * @param physicalNetworkId zoneId OR PhysicalNetworkId needs to be provided.
     * @return true if the domain template exists on the VSD else false if it does not exist on the VSD
     */
    public boolean checkIfDomainTemplateExist(Long domainId, String domainTemplate, Long zoneId, Long physicalNetworkId);

}
