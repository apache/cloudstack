package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements PrimaryDataStoreProvider {
    private final String providerName = "default primary data store provider";
    protected PrimaryDataStoreDriver driver;
    private PrimaryDataStoreProviderVO providerVO;
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    @Inject
    protected PrimaryDataStoreProviderDao providerDao;

    public DefaultPrimaryDatastoreProviderImpl() {
        
    }

    @Override
    public PrimaryDataStore getDataStore(long dataStoreId) {
        PrimaryDataStoreVO dsv = dataStoreDao.findById(dataStoreId);
        if (dsv == null) {
            return null;
        }

        DefaultPrimaryDataStore pds = DefaultPrimaryDataStore.createDataStore(dsv);
        
        PrimaryDataStoreDriver driver = new DefaultPrimaryDataStoreDriverImpl(pds);
        pds.setDriver(driver);
        
        DefaultPrimaryDataStoreLifeCycleImpl lifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(dataStoreDao, pds);
        pds.setLifeCycle(lifeCycle);
        
        return pds;
    }
    
    @Override
    public PrimaryDataStore registerDataStore(Map<String, String> dsInfos) {
        PrimaryDataStoreVO dataStoreVO = new PrimaryDataStoreVO();
        dataStoreVO.setStorageProviderId(this.getId());
        dataStoreVO = dataStoreDao.persist(dataStoreVO);
        
        PrimaryDataStore dataStore = this.getDataStore(dataStoreVO.getId());
        PrimaryDataStoreLifeCycle lifeCycle = dataStore.getLifeCycle();
        lifeCycle.initialize(dsInfos);
        return getDataStore(dataStore.getId());
    }

    @Override
    public long getId() {
        return this.providerVO.getId();
    }

    @Override
    public boolean configure() {
        this.providerVO = providerDao.findByName(this.providerName);
        return true;
    }

    @Override
    public String getName() {
        return providerName;
    }

}
