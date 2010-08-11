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
package com.cloud.utils.component;

import java.util.Map;

import javax.naming.ConfigurationException;

/**
 * 
 * For now we only expose some simple methods. In the future, we can use this
 * interface to implement the management beans to manage the managers.
 **/
public interface Manager {
    /**
     * Configuration with parameters. If there are background tasks, they
     * shouldn't be started yet. Wait for the start() call.
     * 
     * @param name
     *            The managers name.
     * @param params
     *            Configuration parameters.
     * @return true if the configuration was successful, false otherwise.
     */
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    /**
     * Start any background tasks.
     * 
     * @return true if the tasks were started, false otherwise.
     */
    public boolean start();

    /**
     * Stop any background tasks.
     * 
     * @return true background tasks were stopped, false otherwise.
     */
    public boolean stop();

    /**
     * Get the name of this manager.
     * 
     * @return the name.
     */
    public String getName();
}
