/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.element;

import java.util.List;
import com.cloud.api.commands.AddCiscoNexusVSMCmd;
import com.cloud.api.commands.ConfigureCiscoNexusVSMCmd;
import com.cloud.api.commands.DeleteCiscoNexusVSMCmd;
import com.cloud.api.commands.ListCiscoNexusVSMNetworksCmd;
import com.cloud.api.commands.ListCiscoNexusVSMCmd;
import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.PortProfile;
import com.cloud.utils.component.PluggableService;

public interface CiscoNexusVSMElementService extends PluggableService {

    /**
     * adds a Cisco Nexus VSM
     * @param AddCiscoNexusVSMCmd 
     * @return CiscoNexusVSMDeviceVO object for the device added
     */
    public CiscoNexusVSMDeviceVO addCiscoNexusVSM(AddCiscoNexusVSMCmd cmd);

    /**
     * removes a Cisco Nexus VSM
     * @param DeleteCiscoNexusVSMCmd 
     * @return true if VSM is deleted successfully
     */
    public boolean deleteCiscoNexusVSM(DeleteCiscoNexusVSMCmd cmd);

    /**
     * lists all the VSMs the Mgmt Server knows of.
     * @param ListCiscoNexusVSMCmd
     * @return list of CiscoNexusVSMDeviceVO for the VSMs the mgmt server knows of.
     */
    public List<CiscoNexusVSMDeviceVO> listCiscoNexusVSMs(ListCiscoNexusVSMCmd cmd);

    /**
     * lists all the networks (port profiles) configured on the VSM.
     * @param ListCiscoNexusVSMCmd
     * @return list of the guest networks that are using this Netscaler load balancer
     */
    public List<? extends PortProfile> listNetworks(ListCiscoNexusVSMNetworksCmd cmd);

    /**
     * creates API response object for netscaler load balancers
     * @param lbDeviceVO external load balancer VO object
     * @return NetscalerLoadBalancerResponse
     */
    public CiscoNexusVSMResponse createCiscoNexusVSMResponse(CiscoNexusVSMDeviceVO lbDeviceVO);
}
