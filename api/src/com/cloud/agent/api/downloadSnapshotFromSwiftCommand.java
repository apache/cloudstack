/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.agent.api;

import com.cloud.agent.api.to.SwiftTO;

/**
 * When a snapshot of a VDI is taken, it creates two new files,
 * a 'base copy' which contains all the new data since the time of the last snapshot and an 'empty snapshot' file.
 * Any new data is again written to the VDI with the same UUID. 
 * This class issues a command for copying the 'base copy' vhd file to secondary storage.
 * This currently assumes that both primary and secondary storage are mounted on the XenServer.  
 */
public class downloadSnapshotFromSwiftCommand extends SnapshotCommand {
    private SwiftTO _swift;

    private String _parent;

    protected downloadSnapshotFromSwiftCommand() {
        
    }
   
    public downloadSnapshotFromSwiftCommand(SwiftTO swift, String secondaryStorageUrl, Long dcId, Long accountId, Long volumeId, String parent, String BackupUuid, int wait) {

        super("", secondaryStorageUrl, BackupUuid, "", dcId, accountId, volumeId);
        setParent(parent);
        setSwift(swift);
        setWait(wait);
    }


    public SwiftTO getSwift() {
        return this._swift;
    }

    public void setSwift(SwiftTO swift) {
        this._swift = swift;
    }

    public String getParent() {
        return _parent;
    }

    public void setParent(String parent) {
        this._parent = parent;
    }

}