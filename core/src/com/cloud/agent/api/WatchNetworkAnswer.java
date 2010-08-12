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

import java.util.Collection;
import java.util.HashMap;

public class WatchNetworkAnswer extends Answer {
    HashMap<String, Long> transmitted;
    HashMap<String, Long> received;
    
    protected WatchNetworkAnswer() {
    }
    
    public WatchNetworkAnswer(WatchNetworkCommand cmd) {
        super(cmd);
        transmitted = new HashMap<String, Long>();
        received = new HashMap<String, Long>();
    }
    
    public void addStats(String vmName, long txn, long rcvd) {
        transmitted.put(vmName, txn);
        received.put(vmName, rcvd);
    }
    
    public long[] getStats(String vmName) {
        long[] stats = new long[2];
        stats[0] = transmitted.get(vmName);
        stats[1] = received.get(vmName);
        return stats;
    }
    
    public Collection<String> getAllVms() {
        return transmitted.keySet();
    }
    
}
