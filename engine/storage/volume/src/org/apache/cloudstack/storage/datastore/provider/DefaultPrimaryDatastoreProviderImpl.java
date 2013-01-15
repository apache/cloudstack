package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentContext;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    private final String providerName = "default primary data store provider";
    protected PrimaryDataStoreDriver driver;
    @Inject
    PrimaryDataStoreProviderManager storeMgr;
    protected DataStoreLifeCycle lifecyle;
    protected String uuid;
    protected long id;
    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public DataStoreLifeCycle getLifeCycle() {
        return this.lifecyle;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifecyle = ComponentContext.inject(DefaultPrimaryDataStoreLifeCycleImpl.class);
        driver = ComponentContext.inject(DefaultPrimaryDataStoreDriverImpl.class);
        uuid = (String)params.get("uuid");
        id = (Long)params.get("id");
        storeMgr.registerDriver(uuid, this.driver);
        return true;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public long getId() {
        return this.id;
    }

}
