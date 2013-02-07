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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;

@Component
@Local(value={ClusterServiceAdapter.class})
public class ClusterServiceServletAdapter extends AdapterBase implements ClusterServiceAdapter {

    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletAdapter.class);
    private static final int DEFAULT_SERVICE_PORT = 9090;
    private static final int DEFAULT_REQUEST_TIMEOUT = 300;			// 300 seconds
    
    @Inject private ClusterManager _manager;
    
    @Inject private ManagementServerHostDao _mshostDao;
    
    @Inject private ConfigurationDao _configDao;
    
    private ClusterServiceServletContainer _servletContainer;
    
    private int _clusterServicePort = DEFAULT_SERVICE_PORT;
    
    private int _clusterRequestTimeoutSeconds = DEFAULT_REQUEST_TIMEOUT;
    
    @Override
	public ClusterService getPeerService(String strPeer) throws RemoteException {
    	try {
    		init();
    	} catch (ConfigurationException e) {
    		s_logger.error("Unable to init ClusterServiceServletAdapter");
    		throw new RemoteException("Unable to init ClusterServiceServletAdapter");
    	}
    	
    	String serviceUrl = getServiceEndpointName(strPeer);
    	if(serviceUrl == null)
    		return null;
    	
    	return new ClusterServiceServletImpl(serviceUrl, _clusterRequestTimeoutSeconds);
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
    	if(mshost == null)
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
    	if(_servletContainer != null)
    		_servletContainer.stop();
    	return true;
    }
    
    private void init() throws ConfigurationException {
    	if(_mshostDao != null)
    		return;
    	
        String value = _configDao.getValue(Config.ClusterMessageTimeOutSeconds.key());
    	_clusterRequestTimeoutSeconds = NumbersUtil.parseInt(value, DEFAULT_REQUEST_TIMEOUT);
    	s_logger.info("Configure cluster request time out. timeout: " + _clusterRequestTimeoutSeconds + " seconds");
        
        File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        Properties dbProps = new Properties();
        try {
			dbProps.load(new FileInputStream(dbPropsFile));
		} catch (FileNotFoundException e) {
            throw new ConfigurationException("Unable to find db.properties");
		} catch (IOException e) {
            throw new ConfigurationException("Unable to load db.properties content");
		}
		
        _clusterServicePort = NumbersUtil.parseInt(dbProps.getProperty("cluster.servlet.port"), DEFAULT_SERVICE_PORT);
        if(s_logger.isInfoEnabled())
        	s_logger.info("Cluster servlet port : " + _clusterServicePort);
    }
}
