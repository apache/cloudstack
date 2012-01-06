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
package com.cloud.network;

import java.net.URI;
import java.net.URISyntaxException;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Network includes all of the enums used within networking.
 *
 */
public class Networks {
    
    public enum RouterPrivateIpStrategy {
        None,
        DcGlobal, //global to data center
        HostLocal;
        
        public static String DummyPrivateIp = "169.254.1.1";
    }
    
    /**
     * Different ways to assign ip address to this network.
     */
    public enum Mode {
        None,
        Static,
        Dhcp,
        ExternalDhcp;
    };
    
    public enum AddressFormat {
        Ip4,
        Ip6,
        Mixed
    }

    /**
     * Different types of broadcast domains. 
     */
    public enum BroadcastDomainType {
        Native(null, null),
        Vlan("vlan", Integer.class),
        Vswitch("vs", String.class),
        LinkLocal(null, null),
        Vnet("vnet", Long.class),
        Storage("storage", Integer.class),
        UnDecided(null, null);
        
        private String scheme;
        private Class<?> type;
        
        private BroadcastDomainType(String scheme, Class<?> type) {
            this.scheme = scheme;
            this.type = type;
        }
        
        /**
         * @return scheme to be used in broadcast uri.  Null indicates that this type does not have broadcast tags.
         */
        public String scheme() {
            return scheme;
        }
        
        /**
         * @return type of the value in the broadcast uri. Null indicates that this type does not have broadcast tags.
         */
        public Class<?> type() {
            return type;
        }
        
        public <T> URI toUri(T value) {
            try {
                return new URI(scheme + "://" + value);
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Unable to convert to broadcast URI: " + value);
            }
        }
    };
    
    /**
     * Different types of network traffic in the data center. 
     */
    public enum TrafficType {
    	None,
        Public,
        Guest,
        Storage,
        Management,
        Control,
        Vpn;
        
        public static boolean isSystemNetwork(TrafficType trafficType) {
            if(Storage.equals(trafficType)
                    || Management.equals(trafficType)
                    || Control.equals(trafficType)){
                return true;
            }
            return false;
        }
        
        public static TrafficType getTrafficType(String type) {
        	if (type.equals("Public")) {
        		return Public;
        	} else if (type.endsWith("Guest")) {
        		return Guest;
        	} else if (type.endsWith("Storage")) {
        		return Storage;
        	} else if (type.endsWith("Management")) {
        		return Management;
        	} else if (type.endsWith("Control")) {
        		return Control;
        	} else if (type.endsWith("Vpn")) {
        		return Vpn;
        	} else {
        		return None;
        	}
        }
    };
    
    public enum IsolationType {
        None(null, null),
        Ec2("ec2", String.class),
        Vlan("vlan", Integer.class),
        Vswitch("vs", String.class),
        Undecided(null, null),
        Vnet("vnet", Long.class);
        
        private final String scheme;
        private final Class<?> type;
        
        private IsolationType(String scheme, Class<?> type) {
            this.scheme = scheme;
            this.type = type;
        }
        
        public String scheme() {
            return scheme;
        }
        
        public Class<?> type() {
            return type;
        }
        
        public <T> URI toUri(T value) {
            try {
            	//assert(this!=Vlan || value.getClass().isAssignableFrom(Integer.class)) : "Why are you putting non integer into vlan url";
                return new URI(scheme + "://" + value.toString());
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Unable to convert to isolation type URI: " + value);
            }
        }
    }
    
    public enum BroadcastScheme {
        Vlan("vlan"),
        VSwitch("vswitch");
        
        private String scheme;
        
        private BroadcastScheme(String scheme) {
            this.scheme = scheme;
        }
        
        @Override
        public String toString() {
            return scheme;
        }
    }
}
