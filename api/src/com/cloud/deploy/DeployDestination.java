/**
 * 
 */
package com.cloud.deploy;

import java.util.Map;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.utils.NumbersUtil;

public class DeployDestination {
    DataCenter _dc;
    Pod _pod;
    Cluster _cluster;
    Host _host;
    Map<Volume, StoragePool> _storage;

    public DataCenter getDataCenter() {
        return _dc;
    }
    
    public Pod getPod() {
        return _pod;
    }
    
    public Cluster getCluster() {
        return _cluster;
    }
    
    public Host getHost() {
        return _host;
    }
    
    public Map<Volume, StoragePool> getStorageForDisks() {
        return _storage;
    }

    public DeployDestination(DataCenter dc, Pod pod, Cluster cluster, Host host) {
        _dc = dc;
        _pod = pod;
        _cluster = cluster;
        _host = host;
    }
    
    public DeployDestination() {
    }
    
    @Override
    public int hashCode() {
        return NumbersUtil.hash(_host.getId());
    }
    
    @Override
    public boolean equals(Object obj) {
        DeployDestination that = (DeployDestination)obj;
        if (this._dc == null || that._dc == null) {
            return false;
        }
        if (this._dc.getId() != that._dc.getId()) {
            return false;
        }
        if (this._pod == null || that._pod == null) {
            return false;
        }
        if (this._pod.getId() != that._pod.getId()) {
            return false;
        }
        if (this._cluster == null || that._cluster == null) {
            return false;
        }
        if (this._cluster.getId() != that._cluster.getId()) {
            return false;
        }
        if (this._host == null || that._host == null) {
            return false;
        }
        return this._host.getId() == that._host.getId();
    }
}
