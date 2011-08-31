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

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Allows JMX access
 *
 */
public interface SecurityGroupManagerMBean {
    void enableUpdateMonitor(boolean enable);
    
    void disableSchedulerForVm(Long vmId);
    
    void enableSchedulerForVm(Long vmId);
    
    Long[] getDisabledVmsForScheduler();

    void enableSchedulerForAllVms();
    
    Map<Long, Date> getScheduledTimestamps();
    
    Map<Long, Date> getLastUpdateSentTimestamps();
    
    int getQueueSize();
    
    List<Long> getVmsInQueue();
    
    void scheduleRulesetUpdateForVm(Long vmId);
    
    void tryRulesetUpdateForVmBypassSchedulerVeryDangerous(Long vmId, Long seqno);

    void simulateVmStart(Long vmId);

    void disableSchedulerEntirelyVeryDangerous(boolean disable);
    
    boolean isSchedulerDisabledEntirely();

    void clearSchedulerQueueVeryDangerous();
}
