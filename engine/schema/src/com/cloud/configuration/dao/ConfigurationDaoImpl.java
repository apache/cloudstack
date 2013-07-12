// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.configuration.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.ConfigurationVO;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value={ConfigurationDao.class})
public class ConfigurationDaoImpl extends GenericDaoBase<ConfigurationVO, String> implements ConfigurationDao {
    private static final Logger s_logger = Logger.getLogger(ConfigurationDaoImpl.class);
    private Map<String, String> _configs = null;
    private boolean _premium;

    final SearchBuilder<ConfigurationVO> InstanceSearch;
    final SearchBuilder<ConfigurationVO> NameSearch;
    
    public static final String UPDATE_CONFIGURATION_SQL = "UPDATE configuration SET value = ? WHERE name = ?";

    public ConfigurationDaoImpl () {
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instance", InstanceSearch.entity().getInstance(), SearchCriteria.Op.EQ);
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        setRunLevel(ComponentLifecycle.RUN_LEVEL_SYSTEM_BOOTSTRAP);
    }

    @Override
    public boolean isPremium() {
        return _premium;
    }
    
    @Override
    public void invalidateCache() {
    	_configs = null;
    }

    @Override
    public Map<String, String> getConfiguration(String instance, Map<String, ? extends Object> params) {
        if (_configs == null) {
            _configs = new HashMap<String, String>();

            SearchCriteria<ConfigurationVO> sc = InstanceSearch.create();
            sc.setParameters("instance", "DEFAULT");

            List<ConfigurationVO> configurations = listIncludingRemovedBy(sc);

            for (ConfigurationVO config : configurations) {
            	if (config.getValue() != null)
            		_configs.put(config.getName(), config.getValue());
            }

            if(!"DEFAULT".equals(instance)){
            	//Default instance params are already added, need not add again 
            	sc = InstanceSearch.create();
            	sc.setParameters("instance", instance);

            	configurations = listIncludingRemovedBy(sc);

            	for (ConfigurationVO config : configurations) {
            		if (config.getValue() != null)
            			_configs.put(config.getName(), config.getValue());
            	}
            }

        }

        mergeConfigs(_configs, params);
        return _configs;
    }

    @Override
    public Map<String, String> getConfiguration(Map<String, ? extends Object> params) {
        return getConfiguration("DEFAULT", params);
    }
    
    @Override
    public Map<String, String> getConfiguration() {
        return getConfiguration("DEFAULT", new HashMap<String, Object>());
    }
    
    protected void mergeConfigs(Map<String, String> dbParams, Map<String, ? extends Object> xmlParams) {
        for (Map.Entry<String, ? extends Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	super.configure(name, params);

    	Object premium = params.get("premium");
        _premium = (premium != null) && ((String) premium).equals("true");

        return true;
    }

    //Use update method with category instead
    @Override @Deprecated
    public boolean update(String name, String value) {
    	Transaction txn = Transaction.currentTxn();
		try {
			PreparedStatement stmt = txn.prepareStatement(UPDATE_CONFIGURATION_SQL);
			stmt.setString(1, value);
			stmt.setString(2, name);
			stmt.executeUpdate();
			return true;
		} catch (Exception e) {
			s_logger.warn("Unable to update Configuration Value", e);
		}
		return false;
    }

    @Override
    public boolean update(String name, String category, String value) {
    	Transaction txn = Transaction.currentTxn();
		try {
			value = ("Hidden".equals(category) || "Secure".equals(category)) ? DBEncryptionUtil.encrypt(value) : value;
			PreparedStatement stmt = txn.prepareStatement(UPDATE_CONFIGURATION_SQL);
			stmt.setString(1, value);
			stmt.setString(2, name);
			stmt.executeUpdate();
			return true;
		} catch (Exception e) {
			s_logger.warn("Unable to update Configuration Value", e);
		}
		return false;
    }
    
    @Override
    public String getValue(String name) {
    	ConfigurationVO config =  findByName(name);
        return (config == null) ? null : config.getValue();
    }
    
    @Override
    @DB
    public String getValueAndInitIfNotExist(String name, String category, String initValue) {
        String returnValue = initValue;
        try {
            ConfigurationVO config = findByName(name);
            if (config != null) {
                if (config.getValue() != null) {
                    returnValue = config.getValue();
                } else {
                    update(name, category, initValue);
                }
            } else {
                if (category.equals("Hidden") || category.equals("Secure")) {
                    initValue = DBEncryptionUtil.encrypt(initValue);
                }
                ConfigurationVO newConfig = new ConfigurationVO(category, "DEFAULT", "management-server", name, initValue, "");
                persist(newConfig);
            }
            return returnValue;
        } catch (Exception e) {
            s_logger.warn("Unable to update Configuration Value", e);
            throw new CloudRuntimeException("Unable to initialize configuration variable: " + name);

        }
    }
    
    @Override
    public ConfigurationVO findByName(String name) {
        SearchCriteria<ConfigurationVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneIncludingRemovedBy(sc);
    }
    
}
