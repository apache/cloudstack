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
package com.cloud.async;

import java.util.Date;

import com.cloud.api.Identity;

public interface AsyncJob extends Identity {
    public enum Type {
        None,
        VirtualMachine,
        DomainRouter,
        Volume,
        ConsoleProxy,
        Snapshot,
        Template,
        Iso,
        SystemVm,
        Host,
        StoragePool,
        IpAddress,
        SecurityGroup,
        PhysicalNetwork,
        TrafficType,
        PhysicalNetworkServiceProvider,
        FirewallRule
    }

    Long getId();

    long getUserId();

    long getAccountId();

    String getCmd();

    int getCmdVersion();

    String getCmdInfo();

    int getCallbackType();

    String getCallbackAddress();

    int getStatus();

    int getProcessStatus();

    int getResultCode();

    String getResult();

    Long getInitMsid();

    Long getCompleteMsid();

    Date getCreated();

    Date getLastUpdated();

    Date getLastPolled();

    Date getRemoved();

    Type getInstanceType();

    Long getInstanceId();

    String getSessionKey();

    String getCmdOriginator();

    boolean isFromPreviousSession();

    SyncQueueItem getSyncSource();
}
