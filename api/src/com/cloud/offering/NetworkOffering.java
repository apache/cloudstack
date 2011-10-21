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
package com.cloud.offering;

import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Type;
import com.cloud.network.Networks.TrafficType;

/**
 * Describes network offering
 *
 */
public interface NetworkOffering {
    
    public enum Availability {
        Required,
        Optional,
        Unavailable;
    }
    
    public enum State {
        Disabled,
        Enabled, 
        Inactive
    }
    
    public final static String SystemPublicNetwork = "System-Public-Network";
    public final static String SystemControlNetwork = "System-Control-Network";
    public final static String SystemManagementNetwork = "System-Management-Network";
    public final static String SystemStorageNetwork = "System-Storage-Network";
    public final static String SystemGuestNetwork = "System-Guest-Network";
    
    public final static String DefaultVirtualizedNetworkOffering = "DefaultVirtualizedNetworkOffering";
    public final static String DefaultDirectNetworkOffering = "DefaultDirectNetworkOffering";

    long getId();

    /**
     * @return name for the network offering.
     */
    String getName();
    
    /**
     * @return text to display to the end user.
     */
    String getDisplayText();
    
    /**
     * @return the rate in megabits per sec to which a VM's network interface is throttled to
     */
    Integer getRateMbps();
    
    /**
     * @return the rate megabits per sec to which a VM's multicast&broadcast traffic is throttled to
     */
    Integer getMulticastRateMbps();
    
    /**
     * @return concurrent connections to be supported.
     */
    Integer getConcurrentConnections();
    
    TrafficType getTrafficType();
    
    boolean getSpecifyVlan();
    
    String getTags();
    
    boolean isDefault();
    
    boolean isSystemOnly();
    
    Availability getAvailability();
    
    boolean isSharedSourceNatService();
    
    @Deprecated
    GuestIpType getGuestType();

    String getUniqueName();

    boolean isSecurityGroupEnabled();

    void setState(State state);

    State getState();

    void setSecurityGroupEnabled(boolean securityGroupEnabled);

    Type getType();
}
