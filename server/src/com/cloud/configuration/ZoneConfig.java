/**
 *  Copyright (C) 2011 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.configuration;

import java.util.ArrayList;
import java.util.List;

public enum ZoneConfig {
    EnableSecStorageVm( Boolean.class, "enable.secstorage.vm", "true", "Enables secondary storage vm service", null),
    EnableConsoleProxyVm( Boolean.class, "enable.consoleproxy.vm", "true", "Enables console proxy vm service", null),
    MaxHosts( Long.class, "max.hosts", null, "Maximum number of hosts the Zone can have", null),
    MaxVirtualMachines( Long.class, "max.vms", null, "Maximum number of VMs the Zone can have", null),
    ZoneMode( String.class, "zone.mode", null, "Mode of the Zone", "Free,Basic,Advanced"),
    HasNoPublicIp(Boolean.class, "has.no.public.ip", "false", "True if Zone has no public IP", null),
    DhcpStrategy(String.class, "zone.dhcp.strategy", "cloudstack-systemvm",  "Who controls DHCP", "cloudstack-systemvm,cloudstack-external,external");
   

    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    
    private static final List<String> _zoneConfigKeys = new ArrayList<String>();
    
    static {
    	// Add keys into List
        for (ZoneConfig c : ZoneConfig.values()) {
        	String key = c.key();
        	_zoneConfigKeys.add(key);
        }
    }    
    
    private ZoneConfig( Class<?> type, String name, String defaultValue, String description, String range) {

        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
    }
    
    public Class<?> getType() {
        return _type;
    }
    
    public String getName() {
        return _name;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public String getDescription() {
        return _description;
    }

    public String getRange() {
        return _range;
    }
    
    public String key() {
        return _name;
    }
    
    public static boolean doesKeyExist(String key){
    	return _zoneConfigKeys.contains(key);
    }    	

}
