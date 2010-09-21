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
        assert false : "Not implemented correctly yet.";
        return false;
    }
}
