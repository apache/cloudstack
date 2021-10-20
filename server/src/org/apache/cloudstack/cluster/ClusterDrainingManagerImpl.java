package org.apache.cloudstack.cluster;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import javax.inject.Inject;
import java.util.List;

public class ClusterDrainingManagerImpl implements ClusterDrainingManager, Configurable {
    private static final ConfigKey<Boolean> drainDisabledClusters = new ConfigKey<>("Advanced", Boolean.class, "drain.disabled.clusters", "false",
            "Drain VMs running on disabled clusters during reboot or start", true, ConfigKey.Scope.Cluster);

    @Inject
    private HostDao _hostDao;
    @Inject
    private ClusterDao _clusterDao;

    @Override
    public void addDrainingToAvoids(DataCenter dc, DeploymentPlanner.ExcludeList avoids) {
        List<Long> disabledClusters = _clusterDao.listDisabledClusters(dc.getId(), null);
        for (Long clusterId : disabledClusters) {
            if (isClusterDrainOptionOn(clusterId)) {
                avoids.addCluster(clusterId);
            }
        }
    }

    @Override
    public boolean shouldDrainHost(Long hostId) {
        Host host = _hostDao.findById(hostId);
        if (isClusterDrainOptionOn(host.getClusterId())) {
            Cluster cluster = _clusterDao.findById(host.getClusterId());
            return host.isDisabled() || isClusterDisabled(cluster);
        }

        return false;
    }

    @Override
    public boolean isClusterDraining(Cluster cluster) {
        return isClusterDrainOptionOn(cluster.getId()) && isClusterDisabled(cluster);
    }

    private boolean isClusterDisabled(Cluster cluster) {
        return cluster.getAllocationState() == Grouping.AllocationState.Disabled;
    }

    private Boolean isClusterDrainOptionOn(long id) {
        return drainDisabledClusters.valueIn(id);
    }

    @Override
    public String getConfigComponentName() {
        return ClusterDrainingManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {drainDisabledClusters};
    }
}
