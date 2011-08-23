/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.cloud.network.security.SecurityGroupWork.Step;


/**
 * Security Group Work Queue that is not shared with other management servers
 *
 */
public class LocalSecurityGroupWorkQueue implements SecurityGroupWorkQueue {
    protected static Logger s_logger = Logger.getLogger(LocalSecurityGroupWorkQueue.class);

    protected LinkedBlockingQueue<SecurityGroupWork> _queue = new LinkedBlockingQueue<SecurityGroupWork>();
    
    @Override
    public void submitWorkForVm(long vmId, long sequenceNumber) {
        
        SecurityGroupWorkVO work = new SecurityGroupWorkVO(vmId, null, new Date(), SecurityGroupWork.Step.Scheduled, null);
        boolean result = _queue.offer(work);
        if (!result) {
            s_logger.warn("Failed to add work item into queue for vm id " + vmId);
        }

    }

   
    @Override
    public void submitWorkForVms(Set<Long> vmIds) {
        for (Long vmId: vmIds) {
            SecurityGroupWorkVO work = new SecurityGroupWorkVO(vmId, null, new Date(), SecurityGroupWork.Step.Scheduled, null);
            boolean result = _queue.offer(work);
            if (!result) {
                s_logger.warn("Failed to add work item into queue for vm id " + vmId);
            }
        }
    }

    
    @Override
    public List<SecurityGroupWork> getWork(int numberOfWorkItems) {
        List<SecurityGroupWork> work = new ArrayList<SecurityGroupWork>();
        _queue.drainTo(work, numberOfWorkItems);
        for (SecurityGroupWork w: work) {
            w.setStep(Step.Processing);
        }
        return work;
    }

}
