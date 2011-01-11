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
package com.cloud.vm;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine.State;

public interface ItWorkDao extends GenericDao<ItWorkVO, String> {
    /**
     * find a work item based on the instanceId and the state.
     * 
     * @param instanceId vm instance id
     * @param state state
     * @return ItWorkVO if found; null if not.
     */
    ItWorkVO findByInstance(long instanceId, State state);
    
    /**
     * cleanup rows that are either Done or Cancelled and been that way 
     * for at least wait time.
     */
    void cleanup(long wait);
}
