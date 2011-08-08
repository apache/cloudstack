package com.cloud.storage;

import java.util.List;
import java.util.Map;

import com.cloud.host.HostVO;
import com.cloud.utils.component.Manager;

public interface OCFS2Manager extends Manager {
    static final String CLUSTER_NAME = "clusterName";

    boolean prepareNodes(List<HostVO> hosts, StoragePool pool, Map<String, String> params);
    
    boolean prepareNodes(Long clusterId);
}
