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
package com.cloud.dc;

public class PodCluster {
    HostPodVO _pod;
    ClusterVO _cluster;
    
    protected PodCluster() {
        super();
    }
    
    public PodCluster(HostPodVO pod, ClusterVO cluster) {
        _pod = pod;
        _cluster = cluster;
    }
    
    public HostPodVO getPod() {
        return _pod;
    }
    
    public ClusterVO getCluster() {
        return _cluster;
    }
    
    
    @Override
    public int hashCode() {
        return _pod.hashCode() ^ (_cluster != null ? _cluster.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PodCluster)) {
            return false;
        }
        
        PodCluster that = (PodCluster)obj;
        if (!this._pod.equals(that._pod)) {
            return false;
        }
        
        if (this._cluster == null && that._cluster == null) {
            return true;
        }
        
        if (this._cluster == null || that._cluster == null) {
            return false;
        }
        
        return this._cluster.equals(that._cluster);
    }
}
