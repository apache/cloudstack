package org.apache.cloudstack.storage.datastore.provider;

import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.HuaweiObsObjectStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.HuaweiObsObjectStoreLifeCycleImpl;
import org.apache.cloudstack.storage.object.ObjectStoreDriver;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class HuaweiObsObjectStoreProviderImpl implements ObjectStoreProvider {

    @Inject
    ObjectStoreProviderManager storeMgr;

    private final String providerName = "Huawei OBS";
    protected ObjectStoreLifeCycle lifeCycle;
    protected ObjectStoreDriver driver;

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifeCycle;
    }

    @Override
    public String getName() {
        return this.providerName;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifeCycle = ComponentContext.inject(HuaweiObsObjectStoreLifeCycleImpl.class);
        driver = ComponentContext.inject(HuaweiObsObjectStoreDriverImpl.class);
        storeMgr.registerDriver(this.getName(), driver);
        return true;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        return this.driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return null;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<>();
        types.add(DataStoreProviderType.OBJECT);
        return types;
    }
}
