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

import com.cloud.api.commands.StartRouter2Cmd;
import com.cloud.api.commands.StartRouterCmd;
import com.cloud.api.commands.StopRouter2Cmd;
import com.cloud.api.commands.StopRouterCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouter;

public interface DomainRouterService extends Manager {
    /**
     * Starts domain router
     * @param cmd the command specifying router's id
     * @return DomainRouter object
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    DomainRouter startRouter(StartRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Stops domain router
     * @param cmd the command specifying router's id
     * @return router if successful, null otherwise
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    DomainRouter stopRouter(StopRouterCmd cmd);
    
    DomainRouter startRouter(StartRouter2Cmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException;
    
    /**
     * Stops domain router
     * @param cmd the command specifying router's id
     * @return router if successful, null otherwise
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    DomainRouter stopRouter(StopRouter2Cmd cmd) throws ResourceUnavailableException, ConcurrentOperationException;
}
