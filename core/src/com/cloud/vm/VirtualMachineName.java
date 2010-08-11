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
package com.cloud.vm;

import java.util.Formatter;

import com.cloud.dc.Vlan;

/**
 * This class contains the different ways to construct and deconstruct a
 * VM Name. 
 */
public class VirtualMachineName {
    public static final String SEPARATOR = "-";
    
    public static String getVnetName(long vnetId) {
        StringBuilder vnet = new StringBuilder();
        Formatter formatter = new Formatter(vnet);
        formatter.format("%04x", vnetId);
        return vnet.toString();
    }
    
    public static boolean isValidVmName(String vmName) {
        return isValidVmName(vmName, null);
    }
    
    public static boolean isValidVmName(String vmName, String instance) {
        String[] tokens = vmName.split(SEPARATOR);
        /*Some vms doesn't have vlan/vnet id*/
        if (tokens.length != 5 && tokens.length != 4) {
            return false;
        }

        if (!tokens[0].equals("i")) {
            return false;
        }
        
        try {
            Long.parseLong(tokens[1]);
            Long.parseLong(tokens[2]);
            if (tokens.length == 5 && !Vlan.UNTAGGED.equalsIgnoreCase(tokens[4])) {
            	Long.parseLong(tokens[4], 16);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        return instance == null || instance.equals(tokens[3]);
    }
    
    public static String getVmName(long vmId, long userId, String instance) {
        StringBuilder vmName = new StringBuilder("i");
        vmName.append(SEPARATOR).append(userId).append(SEPARATOR).append(vmId);
        vmName.append(SEPARATOR).append(instance);
        return vmName.toString();
    }
    
    public static long getVmId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        begin = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }
    
    public static long getRouterId(String routerName) {
        int begin = routerName.indexOf(SEPARATOR);
        int end = routerName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(routerName.substring(begin + 1, end));
    }
    
    public static long getConsoleProxyId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }
    
    public static long getSystemVmId(String vmName) {
        int begin = vmName.indexOf(SEPARATOR);
        int end = vmName.indexOf(SEPARATOR, begin + SEPARATOR.length());
        return Long.parseLong(vmName.substring(begin + 1, end));
    }
    
    public static String getVnet(String vmName) {
        return vmName.substring(vmName.lastIndexOf(SEPARATOR) + SEPARATOR.length());
    }
    
    public static String getRouterName(long routerId, String instance) {
        StringBuilder builder = new StringBuilder("r");
        builder.append(SEPARATOR).append(routerId).append(SEPARATOR).append(instance);
        return builder.toString();
    }
    
    public static String getConsoleProxyName(long vmId, String instance) {
        StringBuilder builder = new StringBuilder("v");
        builder.append(SEPARATOR).append(vmId).append(SEPARATOR).append(instance);
        return builder.toString();
    }
    
    public static String getSystemVmName(long vmId, String instance, String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(SEPARATOR).append(vmId).append(SEPARATOR).append(instance);
        return builder.toString();
    }
    
    public static String attachVnet(String name, String vnet) {
        return name + SEPARATOR + vnet;
    }
    
    public static boolean isValidRouterName(String name) {
        return isValidRouterName(name, null);
    }
    
    public static boolean isValidRouterName(String name, String instance) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 4) {
            return false;
        }
        
        if (!tokens[0].equals("r")) {
            return false;
        }
        
        try {
            Long.parseLong(tokens[1]);
            if (!Vlan.UNTAGGED.equalsIgnoreCase(tokens[3])) {
            	Long.parseLong(tokens[3], 16);
            }
        } catch (NumberFormatException ex) {
            return false;
        }
        
        return instance == null || tokens[2].equals(instance);
    }
    
    public static boolean isValidConsoleProxyName(String name) {
    	return isValidConsoleProxyName(name, null);
    }
    
    public static boolean isValidConsoleProxyName(String name, String instance) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 3) {
            return false;
        }
        
        if (!tokens[0].equals("v")) {
            return false;
        }
        
        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        
        return instance == null || tokens[2].equals(instance);
    }
    
    public static boolean isValidSecStorageVmName(String name, String instance) {
    	return isValidSystemVmName(name, instance, "s");
    }
    
    public static boolean isValidSystemVmName(String name, String instance, String prefix) {
        String[] tokens = name.split(SEPARATOR);
        if (tokens.length != 3) {
            return false;
        }
        
        if (!tokens[0].equals(prefix)) {
            return false;
        }
        
        try {
            Long.parseLong(tokens[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        
        return instance == null || tokens[2].equals(instance);
    }
}
