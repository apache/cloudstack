package org.apache.cloudstack.storage.provider;


import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.driver.OntapPrimaryDatastoreDriver;
import org.apache.cloudstack.storage.lifecycle.OntapPrimaryDatastoreLifecycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class OntapPrimaryDatastoreProvider implements PrimaryDataStoreProvider {

    private static final Logger s_logger = (Logger)LogManager.getLogger(OntapPrimaryDatastoreProvider.class);
    private OntapPrimaryDatastoreDriver primaryDatastoreDriver;
    private OntapPrimaryDatastoreLifecycle primaryDatastoreLifecycle;

    public OntapPrimaryDatastoreProvider() {
        s_logger.info("OntapPrimaryDatastoreProvider initialized");
    }
    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return primaryDatastoreLifecycle;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        return primaryDatastoreDriver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return null;
    }

    @Override
    public String getName() {
        s_logger.trace("OntapPrimaryDatastoreProvider: getName: Called");
        return "ONTAP Primary Datastore Provider";
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        s_logger.trace("OntapPrimaryDatastoreProvider: configure: Called");
        primaryDatastoreDriver = ComponentContext.inject(OntapPrimaryDatastoreDriver.class);
        primaryDatastoreLifecycle = ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class);
        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        s_logger.trace("OntapPrimaryDatastoreProvider: getTypes: Called");
        Set<DataStoreProviderType> typeSet = new HashSet<DataStoreProviderType>();
        typeSet.add(DataStoreProviderType.PRIMARY);
        return typeSet;
    }
}