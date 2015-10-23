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
package com.cloud.cluster;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.cloudstack.framework.config.ConfigDepot;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.db.DbProperties;

public class ClusterServiceServletAdapter extends AdapterBase implements ClusterServiceAdapter {

    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletAdapter.class);
    private static final int DEFAULT_SERVICE_PORT = 9090;
    private static final int DEFAULT_REQUEST_TIMEOUT = 300;            // 300 seconds

    @Inject
    private ClusterManager _manager;

    @Inject
    private ManagementServerHostDao _mshostDao;
    @Inject
    protected ConfigDepot _configDepot;

    private ClusterServiceServletContainer _servletContainer;

    private int _clusterServicePort = DEFAULT_SERVICE_PORT;

    public ClusterServiceServletAdapter() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK);
    }

    @Override
    public ClusterService getPeerService(String strPeer) throws RemoteException {
        try {
            init();
        } catch (ConfigurationException e) {
            s_logger.error("Unable to init ClusterServiceServletAdapter");
            throw new RemoteException("Unable to init ClusterServiceServletAdapter");
        }

        String serviceUrl = getServiceEndpointName(strPeer);
        if (serviceUrl == null)
            return null;

        return new ClusterServiceServletImpl(serviceUrl);
    }

    @Override
    public String getServiceEndpointName(String strPeer) {
        try {
            init();
        } catch (ConfigurationException e) {
            s_logger.error("Unable to init ClusterServiceServletAdapter");
            return null;
        }

        long msid = Long.parseLong(strPeer);

        ManagementServerHostVO mshost = _mshostDao.findByMsid(msid);
        if (mshost == null)
            return null;

        return composeEndpointName(mshost.getServiceIP(), mshost.getServicePort());
    }

    @Override
    public int getServicePort() {
        return _clusterServicePort;
    }

    private String composeEndpointName(String nodeIP, int port) {
        StringBuffer sb = new StringBuffer();
        sb.append("http://").append(nodeIP).append(":").append(port).append("/clusterservice");
        return sb.toString();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        init();
        return true;
    }

    @Override
    public boolean start() {
        _servletContainer = new ClusterServiceServletContainer();
        _servletContainer.start(new ClusterServiceServletHttpHandler(_manager), _clusterServicePort);
        return true;
    }

    @Override
    public boolean stop() {
        if (_servletContainer != null)
            _servletContainer.stop();
        return true;
    }

    private void init() throws ConfigurationException {
        if (_mshostDao != null)
            return;

        Properties dbProps = DbProperties.getDbProperties();

        _clusterServicePort = NumbersUtil.parseInt(dbProps.getProperty("cluster.servlet.port"), DEFAULT_SERVICE_PORT);
        if (s_logger.isInfoEnabled())
            s_logger.info("Cluster servlet port : " + _clusterServicePort);
    }
}
