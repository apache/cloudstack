package com.cloud.naming;

import com.cloud.utils.component.Manager;

public interface ResourceNamingPolicyManager extends Manager {


    /**
     * Finds a resource naming policy among the registered ones by its class
     * @param policyClass
     * @return
     */
    @SuppressWarnings("rawtypes")
    public <T extends ResourceNamingPolicy> T getPolicy(Class<T> policyClass);

}
