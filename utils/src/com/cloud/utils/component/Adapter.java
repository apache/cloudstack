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
 * Adapter defines methods for pluggable code within the Cloud Stack. An
 * adapter encapsulates code that performs to the specification of an interface.
 * Adapters are a departure from regular structured programming.
 */
public interface Adapter {

    /**
     * configure is called when an adapter is initialized.
     * 
     * @param name
     *            The name of the adapter.
     * @param params
     *            A map of configuration parameters.
     * @return Returning false means the configuration did not go well and the
     *         adapter can not be used.
     */
    boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    /**
     * Get the name of this Adapter.
     * 
     * @return the name assigned to this adapter in the config file.
     */
    String getName();

    /**
     * startAdapter() signals the adapter that it can start.
     * 
     * @return true if the adapter can start, false otherwise.
     */
    boolean start();

    /**
     * stopAdapter() signals the adapter that it should be shutdown. Returns
     * false means that the adapter is not ready to be stopped and should be
     * called again.
     * 
     * @return true if the adapter can stop, false indicates the adapter is not
     *         ready to stop.
     */
    boolean stop();
}
