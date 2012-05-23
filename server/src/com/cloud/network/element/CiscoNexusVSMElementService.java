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

import com.cloud.api.commands.DeleteCiscoNexusVSMCmd;
import com.cloud.api.commands.EnableCiscoNexusVSMCmd;
import com.cloud.api.commands.DisableCiscoNexusVSMCmd;
import com.cloud.api.commands.ListCiscoNexusVSMsCmd;
import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.CiscoNexusVSMDevice;
import com.cloud.utils.component.PluggableService;

public interface CiscoNexusVSMElementService extends PluggableService {
    /**
     * removes a Cisco Nexus VSM
     * @param DeleteCiscoNexusVSMCmd 
     * @return true if VSM is deleted successfully
     */
    public boolean deleteCiscoNexusVSM(DeleteCiscoNexusVSMCmd cmd);

    /**
     * Enables a Cisco Nexus VSM. 
     */
    public boolean enableCiscoNexusVSM(EnableCiscoNexusVSMCmd cmd);
    
    
    /**
     * Disables a Cisco Nexus VSM.
     */
    public boolean disableCiscoNexusVSM(DisableCiscoNexusVSMCmd cmd);
    
    /**
     * Returns a list of VSMs.
     * @param ListCiscoNexusVSMsCmd
     * @return List<CiscoNexusVSMDeviceVO>
     */
    public List<CiscoNexusVSMDeviceVO> getCiscoNexusVSMs(ListCiscoNexusVSMsCmd cmd);
    
    /**
     * creates API response object for Cisco Nexus VSMs
     * @param vsmDeviceVO VSM VO object
     * @return CiscoNexusVSMResponse
     */
    
    public CiscoNexusVSMResponse createCiscoNexusVSMResponse(CiscoNexusVSMDevice vsmDeviceVO);
    
    /**
     * Creates a detailed API response object for Cisco Nexus VSMs
     * @param CiscoNexusVSMDeviceVO
     * @return CiscoNexusVSMResponse
     */
    public CiscoNexusVSMResponse createCiscoNexusVSMDetailedResponse(CiscoNexusVSMDevice vsmDeviceVO);
}
