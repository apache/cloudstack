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

package com.cloud.agent.api;


public class ManageSnapshotCommand extends Command {
    // XXX: Should be an enum
    // XXX: Anyway there is something called inheritance in Java
    public static String CREATE_SNAPSHOT = "-c";
    public static String DESTROY_SNAPSHOT = "-d";
    
    private String _commandSwitch;
    
    // Information about the volume that the snapshot is based on
    private String _volumePath = null;
    
    // Information about the snapshot
    private String _snapshotPath = null;
    private String _snapshotName = null;
    private long _snapshotId;

    public ManageSnapshotCommand() {}

    public ManageSnapshotCommand(String commandSwitch, long snapshotId, String path, String preSnapshot, String snapshotName) {
        _commandSwitch = commandSwitch;
        if (commandSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
            _volumePath = path;
            _snapshotPath = preSnapshot;
        }
        else if (commandSwitch.equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
            _snapshotPath = path;
        }
        _snapshotName = snapshotName;
        _snapshotId = snapshotId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getCommandSwitch() {
        return _commandSwitch;
    }
    
    public String getVolumePath() {
        return _volumePath;
    }
    
    public String getSnapshotPath() {
    	return _snapshotPath;
    }

    public String getSnapshotName() {
        return _snapshotName;
    }

    public long getSnapshotId() {
        return _snapshotId;
    }
    
}