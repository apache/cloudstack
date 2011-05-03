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
package com.cloud.cluster;

/**
 * CleanupMaid is implemented by tasks that needs to perform cleanup.
 * 
 * It can contain the actual information about the current state of the 
 * task.  The state is serialized and stored.  When cleanup is required
 * CleanupMaid is instantiated from the stored data and cleanup() is called.
 *
 */
public interface CleanupMaid {
    /**
     * cleanup according the state that was stored.
     * 
     * @return 0 indicates cleanup was successful.  Negative number
     * indicates the cleanup was unsuccessful but don't retry.  Positive number
     * indicates the cleanup was unsuccessful and retry in this many seconds.
     */
    int cleanup(CheckPointManager checkPointMgr);
    
    
    /**
     * If cleanup is unsuccessful and not to be retried, the cleanup procedure 
     * returned here is recorded. 
     * @return
     */
    String getCleanupProcedure();
}
