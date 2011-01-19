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
package com.cloud.network;

import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.router.VirtualRouter;

public interface VirtualNetworkApplianceService{
    /**
     * Starts domain router
     * @param cmd the command specifying router's id
     * @return DomainRouter object
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    VirtualRouter startRouter(long routerId) throws InvalidParameterValueException, PermissionDeniedException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;
    
    /**
     * Reboots domain router
     * @param cmd the command specifying router's id
     * @return router if successful
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    VirtualRouter rebootRouter(long routerId) throws InvalidParameterValueException, PermissionDeniedException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;
    
    VirtualRouter upgradeRouter(UpgradeRouterCmd cmd);
    
    /**
     * Stops domain router
     * @param  id of the router
     * @return router if successful, null otherwise
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    VirtualRouter stopRouter(long routerId) throws InvalidParameterValueException, PermissionDeniedException, ResourceUnavailableException, ConcurrentOperationException;
    
}
