package com.cloud.agent.resource.computing;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

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
