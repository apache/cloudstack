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
package com.cloud.host;

import java.util.Date;

import com.cloud.hypervisor.Hypervisor;


/**
 *  Host represents one particular host server.
 */
public interface Host {
    public enum Type {
        Storage(false),
        Routing(false),
        SecondaryStorage(false),
        ConsoleProxy(true);
        
        boolean _virtual;
        private Type(boolean virtual) {
            _virtual = virtual;
        }
        
        public boolean isVirtual() {
            return _virtual;
        }
        
        public static String[] toStrings(Host.Type... types) {
            String[] strs = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                strs[i] = types[i].toString();
            }
            return strs;
        }
    }
    
    /**
     * @return id of the host.
     */
    Long getId();
    
    /**
     * @return name of the machine.
     */
    String getName();
    
    /**
     * @return the type of host.
     */
    Type getType();
    
    /**
     * @return the date the host first registered
     */
    Date getCreated();

    /**
     * @return current state of this machine.
     */
    Status getStatus();
    
    /**
     * @return the ip address of the host.
     */
    String getPrivateIpAddress();
    
    /**
     * @return the ip address of the host attached to the storage network.
     */
    String getStorageIpAddress();
    
    /**
     * @return the mac address of the host.
     */
    String getGuid();
    
    /**
     * @return total amount of memory.
     */
    Long getTotalMemory();
    
    /**
     * @return # of cores in a machine.  Note two cpus with two cores each returns 4.
     */
    Integer getCpus();
    
    /**
     * @return speed of each cpu in mhz.
     */
    Long getSpeed();

    /**
     * @return the proxy port that is being listened at the agent host
     */
    Integer getProxyPort();
 
    /**
     * @return the pod.
     */
    Long getPodId();
    
    /**
     * @return availability zone.
     */
    long getDataCenterId();
    
    /**
     * @return parent path.  only used for storage server.
     */
    String getParent();
    
    /**
     * @return storage ip address.
     */
    String getStorageIpAddressDeux();
    
    /**
     * @return type of hypervisor
     */
    Hypervisor.Type getHypervisorType();
    
    /**
     * @return disconnection date
     */
    Date getDisconnectedOn();
    /**
     * @return version
     */
    public String getVersion();
    /*
     * @return total size
     */
    public long getTotalSize();
    /*
     * @return capabilities
     */
    public String getCapabilities();
    /*
     * @return last pinged time
     */
    public long getLastPinged();
    /*
     * @return management server id
     */
    public Long getManagementServerId();
    /*
     *@return removal date
     */
    public Date getRemoved();
    
    public Long getClusterId();
}
