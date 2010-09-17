/**
 * 
 */
package com.cloud.network.router;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

@Local(value=DomainRouterManager.class)
public class DomainRouterManagerImpl implements DomainRouterManager {
    String _name;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }
}
