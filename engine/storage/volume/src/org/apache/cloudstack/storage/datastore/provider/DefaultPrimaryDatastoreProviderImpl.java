package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
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

import com.cloud.utils.component.ComponentInject;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    private final String providerName = "default primary data store provider";
    protected PrimaryDataStoreDriver driver;
    private PrimaryDataStoreProviderVO provider;
    protected final PrimaryDataStoreDao dataStoreDao;
    protected PrimaryDataStoreLifeCycle dataStoreLifeCycle;

    @Inject
    public DefaultPrimaryDatastoreProviderImpl(PrimaryDataStoreDao dataStoreDao) {
        this.driver = new DefaultPrimaryDataStoreDriverImpl();
        this.dataStoreDao = dataStoreDao;
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
    public PrimaryDataStoreInfo getDataStoreInfo(long dataStoreId) {
        // TODO Auto-generated method stub
        return null;
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
