package com.cloud.naming;

import java.util.List;
import com.cloud.utils.component.ComponentLifecycleBase;

public class ResourceNamingPolicyManagerImpl extends ComponentLifecycleBase implements ResourceNamingPolicyManager {

    @SuppressWarnings("rawtypes")
    protected List<ResourceNamingPolicy> registered;

    public ResourceNamingPolicyManagerImpl() {}

    @SuppressWarnings("rawtypes")
    public ResourceNamingPolicyManagerImpl(List<ResourceNamingPolicy> registered, String defaultName) {
        this.registered = registered;
    }

    @SuppressWarnings("rawtypes")
    public void setRegistered(List<ResourceNamingPolicy> policies) {
        registered = policies;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T extends ResourceNamingPolicy> T getPolicy(Class<T> policyClass) {
        for (ResourceNamingPolicy r : registered) {
            if (policyClass.isAssignableFrom(r.getClass())) {
                return (T)r;
            }
        }
        return null;
    }

}
