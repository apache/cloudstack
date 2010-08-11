/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.resource;

import java.net.URL;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.net.UrlUtil;

public abstract class DiscovererBase implements Discoverer {
    protected String _name;
    protected Map<String, String> _params;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        ConfigurationDao dao = ComponentLocator.getCurrentLocator().getDao(ConfigurationDao.class);
        _params = dao.getConfiguration(params);
        _name = name;
        
        return true;
    }
    
    protected Map<String, String> resolveInputParameters(URL url) {
        Map<String, String> params = UrlUtil.parseQueryParameters(url);
        
        return null;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
