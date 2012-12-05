package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.datastore.lifecycle.PrimaryDataStoreLifeCycle;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    private final String providerName = "default primary data store provider";
    protected PrimaryDataStoreDriver driver;
    private PrimaryDataStoreProviderVO provider;
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    protected PrimaryDataStoreLifeCycle dataStoreLifeCycle;

    public DefaultPrimaryDatastoreProviderImpl() {
        this.driver = new DefaultPrimaryDataStoreDriverImpl();
        this.dataStoreLifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(this, dataStoreDao);
    }

    @Override
    public PrimaryDataStore getDataStore(long dataStoreId) {
        PrimaryDataStoreVO dsv = dataStoreDao.findById(dataStoreId);
        if (dsv == null) {
            return null;
        }

        PrimaryDataStore pds = DefaultPrimaryDataStore.createDataStore(driver, dsv, null);
        return pds;
    }

    @Override
    public PrimaryDataStoreLifeCycle getDataStoreLifeCycle() {
        return dataStoreLifeCycle;
    }

    @Override
    public long getId() {
        return this.provider.getId();
    }

    @Override
    public boolean register(PrimaryDataStoreProviderVO provider, Map<String, Object> params) {
        this.provider = provider;
        return true;
    }

    @Override
    public boolean init(PrimaryDataStoreProviderVO provider) {
        this.provider = provider;
        return true;
    }

    @Override
    public String getName() {
        return providerName;
    }

}
