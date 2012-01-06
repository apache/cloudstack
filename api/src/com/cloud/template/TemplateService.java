/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.template;

import java.net.URISyntaxException;
import java.util.List;

import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.ExtractIsoCmd;
import com.cloud.api.commands.ExtractTemplateCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoPermissionsCmd;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;

public interface TemplateService {

    VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd) throws URISyntaxException, ResourceAllocationException;

    VirtualMachineTemplate registerIso(RegisterIsoCmd cmd) throws IllegalArgumentException, ResourceAllocationException;

    VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd) throws StorageUnavailableException, ResourceAllocationException;
    
    VirtualMachineTemplate prepareTemplate(long templateId, long zoneId) ;

    boolean detachIso(long vmId);

    boolean attachIso(long isoId, long vmId);

    /**
     * Deletes a template
     * 
     * @param cmd
     *            - the command specifying templateId
     */
    boolean deleteTemplate(DeleteTemplateCmd cmd);

    /**
     * Deletes a template
     * 
     * @param cmd
     *            - the command specifying isoId
     * @return true if deletion is successful, false otherwise
     */
    boolean deleteIso(DeleteIsoCmd cmd);

    /**
     * Extracts an ISO
     * 
     * @param cmd
     *            - the command specifying the mode and id of the ISO
     * @return extractId.
     */
    Long extract(ExtractIsoCmd cmd) throws InternalErrorException;

    /**
     * Extracts a Template
     * 
     * @param cmd
     *            - the command specifying the mode and id of the template
     * @return extractId
     */
    Long extract(ExtractTemplateCmd cmd) throws InternalErrorException;

    VirtualMachineTemplate getTemplate(long templateId);

	List<String> listTemplatePermissions(ListTemplateOrIsoPermissionsCmd cmd);

	boolean updateTemplateOrIsoPermissions(UpdateTemplateOrIsoPermissionsCmd cmd);
}
