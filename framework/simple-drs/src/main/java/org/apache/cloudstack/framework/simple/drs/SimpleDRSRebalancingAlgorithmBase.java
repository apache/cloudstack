package org.apache.cloudstack.framework.simple.drs;

import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.simple.drs.SimpleDRSResource;
import org.apache.cloudstack.simple.drs.SimpleDRSWorkload;

import java.util.List;

public abstract class SimpleDRSRebalancingAlgorithmBase extends AdapterBase implements SimpleDRSRebalancingAlgorithm {
    @Override
    public List<SimpleDRSResource> findResourcesToBalance(long clusterId) {
        return null;
    }

    @Override
    public List<SimpleDRSWorkload> findWorkloadsToBalance() {
        return null;
    }

    @Override
    public void sortRebalancingPlansByCost() {

    }

    @Override
    public void sortRebalancingPlansByBenefit() {

    }
}
