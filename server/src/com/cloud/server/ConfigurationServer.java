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

package com.cloud.server;

import com.cloud.exception.InternalErrorException;

/**
 * ConfigurationServer is the public interface to talk to the Configuration Server.
 * This is the Server that is run right before the Management Server.
 */
public interface ConfigurationServer {
    public static final String Name = "configuration-server";

    /**
     * Persists default values for the configuration table, pods/zones, and VLANs
     * 
     * @return
     */
    public void persistDefaultValues() throws InternalErrorException;
    public void updateKeyPairs();
}
