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
package com.cloud.host.dao;

import com.cloud.host.VmHostVO;
import com.cloud.utils.db.GenericDao;

/**
 * @author ahuang
 *
 */
public interface VmHostDao extends GenericDao<VmHostVO, Long> {
    /**
     * @return the next available vnc port;
     */
    public Integer getAvailableVncPort(long hostId);

    /**
     * @param hostId host to return the vncport to.
     * @param vncPort vnc port to return.
     */
    public void freeVncPort(long hostId, int vncPort); 

    /**
     * Set the vnc port.
     * @param hostId
     * @param vncPort
     */
    public void setVncPort(long hostId, int vncPort);
}
