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

package com.cloud.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;

@Local(value={ClusterServiceAdapter.class})
public class ClusterServiceServletAdapter implements ClusterServiceAdapter {

    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletAdapter.class);
    private static final int DEFAULT_SERVICE_PORT = 9090;
    
    private ClusterManager manager;
    
    private ManagementServerHostDao _mshostDao;
    private ClusterServiceServletContainer _servletContainer;
    
    private String _name;
    private int _clusterServicePort = DEFAULT_SERVICE_PORT;
    
    @Override
	public ClusterService getPeerService(String strPeer) throws RemoteException {
    	String serviceUrl = getServiceEndpointName(strPeer);
    	if(serviceUrl == null)
    		return null;
    	
    	return new ClusterServiceServletImpl(serviceUrl);
	}
    
    @Override
	public String getServiceEndpointName(String strPeer) {
    	
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
    	_name = name;
    	
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        manager = locator.getManager(ClusterManager.class);
        if(manager == null) 
            throw new ConfigurationException("Unable to get " + ClusterManager.class.getName());

        _mshostDao = locator.getDao(ManagementServerHostDao.class);
        if(_mshostDao == null)
            throw new ConfigurationException("Unable to get " + ManagementServerHostDao.class.getName());
        
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
        
    	return true;
    }
    
    @Override
    public String getName() {
    	return _name;
    }
    
    @Override
    public boolean start() {
    	_servletContainer = new ClusterServiceServletContainer();
    	_servletContainer.start(new ClusterServiceServletHttpHandler(manager), _clusterServicePort);
    	return true;
    }
    
    @Override
    public boolean stop() {
    	if(_servletContainer != null)
    		_servletContainer.stop();
    	return true;
    }
}
