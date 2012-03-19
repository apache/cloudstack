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

import java.util.Date;

/**
 * ServiceOffering models the different types of service contracts to be
 * offered.
 */
public interface ServiceOffering {
    public static final String consoleProxyDefaultOffUniqueName = "Cloud.com-ConsoleProxy";
    public static final String ssvmDefaultOffUniqueName = "Cloud.com-SecondaryStorage";
    public static final String routerDefaultOffUniqueName = "Cloud.Com-SoftwareRouter";
    public static final String elbVmDefaultOffUniqueName = "Cloud.Com-ElasticLBVm";

    public enum StorageType {
        local,
        shared
    }

    long getId();

    String getDisplayText();

    Date getCreated();

    String getTags();

    /**
     * @return user readable description
     */
    String getName();

    /**
     * @return is this a system service offering
     */
    boolean getSystemUse();

    /**
     * @return # of cpu.
     */
    int getCpu();

    /**
     * @return speed in mhz
     */
    int getSpeed();

    /**
     * @return ram size in megabytes
     */
    int getRamSize();

    /**
     * @return Does this service plan offer HA?
     */
    boolean getOfferHA();

    /**
     * @return Does this service plan offer VM to use CPU resources beyond the service offering limits?
     */
    boolean getLimitCpuUse();

    /**
     * @return the rate in megabits per sec to which a VM's network interface is throttled to
     */
    Integer getRateMbps();

    /**
     * @return the rate megabits per sec to which a VM's multicast&broadcast traffic is throttled to
     */
    Integer getMulticastRateMbps();

    /**
     * @return whether or not the service offering requires local storage
     */
    boolean getUseLocalStorage();

    Long getDomainId();

    /**
     * @return tag that should be present on the host needed, optional parameter
     */
    String getHostTag();

    boolean getDefaultUse();

    String getSystemVmType();
}
