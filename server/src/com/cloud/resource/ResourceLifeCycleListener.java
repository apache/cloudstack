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
package com.cloud.resource;

import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.HostVO;

/**
 * Listener registered with the ResourceManager if you want to be informed
 * of a certain type of host's life cycles.
 * 
 */
public interface ResourceLifeCycleListener {
    /**
     * @return the type of resource this listener can process.
     */
    Host.Type getType();

    void add(HostVO host, StartupCommand cmd, boolean created);

    /**
     * Put the resource into maintenance mode.
     */
    void maintain(HostVO host, boolean force);

    void removed(HostVO host, boolean force);

    void enable(HostVO host);

    void disable(HostVO host);

}
