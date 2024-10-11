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
package com.cloud.resource;

import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkModel;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.net.UrlUtil;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class DiscovererBase extends AdapterBase implements Discoverer {
    protected Map<String, String> _params;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    protected NetworkModel _networkMgr;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected DataCenterDao _dcDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _params = _configDao.getConfiguration(params);

        return true;
    }

    protected Map<String, String> resolveInputParameters(URL url) {
        Map<String, String> params = UrlUtil.parseQueryParameters(url);

        return null;
    }

    @Override
    public void putParam(Map<String, String> params) {
        if (_params == null) {
            _params = new HashMap<String, String>();
        }
        _params.putAll(params);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected ServerResource getResource(String resourceName) {
        ServerResource resource = null;
        try {
            Class<?> clazz = Class.forName(resourceName);
            Constructor constructor = clazz.getConstructor();
            resource = (ServerResource)constructor.newInstance();
        } catch (ClassNotFoundException e) {
            logger.warn("Unable to find class " + resourceName, e);
        } catch (InstantiationException e) {
            logger.warn("Unable to instantiate class " + resourceName, e);
        } catch (IllegalAccessException e) {
            logger.warn("Illegal access " + resourceName, e);
        } catch (SecurityException e) {
            logger.warn("Security error on " + resourceName, e);
        } catch (NoSuchMethodException e) {
            logger.warn("NoSuchMethodException error on " + resourceName, e);
        } catch (IllegalArgumentException e) {
            logger.warn("IllegalArgumentException error on " + resourceName, e);
        } catch (InvocationTargetException e) {
            logger.warn("InvocationTargetException error on " + resourceName, e);
        }

        return resource;
    }

    protected HashMap<String, Object> buildConfigParams(HostVO host) {
        HashMap<String, Object> params = new HashMap<String, Object>(host.getDetails().size() + 5);
        params.putAll(host.getDetails());

        params.put("guid", host.getGuid());
        params.put("zone", Long.toString(host.getDataCenterId()));
        if (host.getPodId() != null) {
            params.put("pod", Long.toString(host.getPodId()));
        }
        if (host.getClusterId() != null) {
            params.put("cluster", Long.toString(host.getClusterId()));
            String guid = null;
            ClusterVO cluster = _clusterDao.findById(host.getClusterId());
            if (cluster.getGuid() == null) {
                guid = host.getDetail("pool");
            } else {
                guid = cluster.getGuid();
            }
            if (guid != null && !guid.isEmpty()) {
                params.put("pool", guid);
            }
        }

        params.put("ipaddress", host.getPrivateIpAddress());
        params.put("secondary.storage.vm", "false");
        params.put("max.template.iso.size", _configDao.getValue(Config.MaxTemplateAndIsoSize.toString()));
        params.put("migratewait", _configDao.getValue(Config.MigrateWait.toString()));
        params.put(Config.XenServerMaxNics.toString().toLowerCase(), _configDao.getValue(Config.XenServerMaxNics.toString()));
        params.put(Config.XenServerHeartBeatInterval.toString().toLowerCase(), _configDao.getValue(Config.XenServerHeartBeatInterval.toString()));
        params.put(Config.XenServerHeartBeatTimeout.toString().toLowerCase(), _configDao.getValue(Config.XenServerHeartBeatTimeout.toString()));
        params.put("router.aggregation.command.each.timeout", _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));

        return params;

    }

    @Override
    public ServerResource reloadResource(HostVO host) {
        String resourceName = host.getResource();
        ServerResource resource = getResource(resourceName);

        if (resource != null) {
            _hostDao.loadDetails(host);
            updateNetworkLabels(host);

            HashMap<String, Object> params = buildConfigParams(host);
            try {
                resource.configure(host.getName(), params);
            } catch (ConfigurationException e) {
                logger.warn("Unable to configure resource due to " + e.getMessage());
                return null;
            }
            if (!resource.start()) {
                logger.warn("Unable to start the resource");
                return null;
            }
        }
        return resource;
    }

    private void updateNetworkLabels(HostVO host) {
        //check if networkLabels need to be updated in details
        //we send only private and storage network label to the resource.
        String privateNetworkLabel = _networkMgr.getDefaultManagementTrafficLabel(host.getDataCenterId(), host.getHypervisorType());
        String storageNetworkLabel = _networkMgr.getDefaultStorageTrafficLabel(host.getDataCenterId(), host.getHypervisorType());

        String privateDevice = host.getDetail("private.network.device");
        String storageDevice = host.getDetail("storage.network.device1");

        boolean update = false;

        if (privateNetworkLabel != null && !privateNetworkLabel.equalsIgnoreCase(privateDevice)) {
            host.setDetail("private.network.device", privateNetworkLabel);
            update = true;
        }
        if (storageNetworkLabel != null && !storageNetworkLabel.equalsIgnoreCase(storageDevice)) {
            host.setDetail("storage.network.device1", storageNetworkLabel);
            update = true;
        }
        if (update) {
            _hostDao.saveDetails(host);
        }
    }

}
