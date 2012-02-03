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

package com.cloud.network.ovs;

import com.cloud.agent.api.Command;

public class OvsSetTagAndFlowCommand extends Command {
    String vlans;
    String vmName;
    String seqno;
    String tag;
    Long vmId;

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getSeqNo() {
        return seqno;
    }

    public String getVlans() {
        return vlans;
    }

    public String getVmName() {
        return vmName;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getTag() {
        return tag;
    }

    public OvsSetTagAndFlowCommand(String vmName, String tag, String vlans, String seqno, Long vmId) {
        this.vmName = vmName;
        this.tag = tag;
        this.vlans = vlans;
        this.seqno = seqno;
        this.vmId = vmId;
    }
}
