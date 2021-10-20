package org.apache.cloudstack.cluster;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.org.Cluster;

/**
 * Wrapper class around the config option 'drain.disabled.clusters' to enable injecting logic into multiple locations
 */
public interface ClusterDrainingManager {

    /**
     * Adds clusters that are draining to a deployment planner list of avoids
     * @param dc
     * @param avoids - modified in place
     */
    void addDrainingToAvoids(DataCenter dc, DeploymentPlanner.ExcludeList avoids);

    /**
     * Check if the given host is impacted by a cluster which is currently draining
     * @param hostId
     * @return true if the host should drain
     */
    boolean shouldDrainHost(Long hostId);

    /**
     * Check if a specific cluster is draining
     * @param cluster
     * @return
     */
    boolean isClusterDraining(Cluster cluster);
}
