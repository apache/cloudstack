package com.cloud.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloud.upgrade.DatabaseUpgradeChecker;
import com.cloud.utils.component.Adapter;
import com.cloud.utils.component.ComponentLibraryBase;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.db.GenericDao;

public class ConfigurationCompoentLibrary extends ComponentLibraryBase {

    @Override
    public List<SystemIntegrityChecker> getSystemIntegrityCheckers() {
        ArrayList<SystemIntegrityChecker> checkers = new ArrayList<SystemIntegrityChecker>();
        checkers.add(new DatabaseUpgradeChecker());
        return checkers;
    }

    @Override
    public Map<String, ComponentInfo<GenericDao<?, ?>>> getDaos() {
        return new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>(0);
    }

    @Override
    public Map<String, ComponentInfo<Manager>> getManagers() {
        return new LinkedHashMap<String, ComponentInfo<Manager>>(0);
    }

    @Override
    public Map<String, List<ComponentInfo<Adapter>>> getAdapters() {
        return  new LinkedHashMap<String, List<ComponentInfo<Adapter>>>(0);
    }

    @Override
    public Map<Class<?>, Class<?>> getFactories() {
        return new HashMap<Class<?>, Class<?>>(0);
    }

}
