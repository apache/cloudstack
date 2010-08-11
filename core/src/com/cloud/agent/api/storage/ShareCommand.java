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
package com.cloud.agent.api.storage;

import java.util.List;

import com.cloud.storage.VolumeVO;

public class ShareCommand extends StorageCommand {
    public static String UnshareAll = "unshare_all";
    
    private boolean share;
    private boolean removePreviousShare;
    private List<VolumeVO> volumes;
    private String vmName;
    private String initiatorIqn;
    
    protected ShareCommand() {
    }
    
    public ShareCommand(String vmName, List<VolumeVO> vols, String initiatorIqn, boolean removePreviousShare) {
        super();
        this.vmName = vmName;
        this.initiatorIqn = initiatorIqn;
        this.share = true;
        this.volumes = vols;
        this.removePreviousShare = removePreviousShare;
    }

    public ShareCommand(String vmName, List<VolumeVO> vols, String initiatorIqn) {
        super();
        this.vmName = vmName;
        this.initiatorIqn = initiatorIqn;
        this.share = false;
        this.volumes = vols;
        this.removePreviousShare = true;
    }
    
    public ShareCommand(String vmName, List<VolumeVO> vols) {
        super();
        this.vmName = vmName;
        this.initiatorIqn = UnshareAll;
        this.share = false;
        this.volumes = vols;
        this.removePreviousShare = true;
    }
    
    // NOTE: We set this to false because we leave it up to the business logic
    // to make sure it is already created before calling shared.
    @Override
    public boolean executeInSequence() {
    	return false;
    }

    public boolean isShare() {
        return share;
    }

    public List<VolumeVO> getVolumes() {
        return volumes;
    }

    public String getVmName() {
        return vmName;
    }

    public String getInitiatorIqn() {
        return initiatorIqn;
    }
    
    public boolean removePreviousShare() {
        return removePreviousShare;
    }
}
