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

package com.cloud.agent.resource.computing;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.kvm.KVMConnection;

public class LibvirtConnection {
	private static final Logger s_logger = Logger.getLogger(LibvirtConnection.class);
	static private Connect _connection;
	static private String _hypervisorURI;
	static public Connect getConnection() throws LibvirtException {
		if (_connection == null) {
			_connection = new Connect(_hypervisorURI, false);
    	} else {
    		try {
    			_connection.getVersion();
    		} catch (LibvirtException e) {
    			s_logger.debug("Connection with libvirtd is broken, due to " + e.getMessage());
    			_connection = new Connect(_hypervisorURI, false);
    		}
    	}
    	
    	return _connection;
	}
	
	static void initialize(String hypervisorURI) {
		_hypervisorURI = hypervisorURI;
	}
}
