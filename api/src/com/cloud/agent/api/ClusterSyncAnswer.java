/*  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

public class ClusterSyncAnswer extends Answer {
    long _clusterId;
    HashMap<String, Pair<String, State>> _newStates;
    int _type = -1; // 0 for full, 1 for delta
    
    public static final int FULL_SYNC=0;
    public static final int DELTA_SYNC=1;
    
    public ClusterSyncAnswer() {
    }
    
    public ClusterSyncAnswer(long clusterId, HashMap<String, Pair<String, State>> newStates, int type){
        _clusterId = clusterId;
        _newStates = newStates;
        _type = type;
        result = true;
    }

    public long getClusterId() {
        return _clusterId;
    }
    
    public HashMap<String, Pair<String, State>> getNewStates() {
        return _newStates;
    }   
    
    public boolean isFull(){
        return _type==0;
    }
}