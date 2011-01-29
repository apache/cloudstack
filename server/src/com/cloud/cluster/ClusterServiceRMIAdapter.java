package com.cloud.cluster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterService;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;

@Local(value={ClusterServiceAdapter.class})
public class ClusterServiceRMIAdapter implements ClusterServiceAdapter {
    private static final Logger s_logger = Logger.getLogger(ClusterServiceRMIAdapter.class);
    private static final int DEFAULT_SERVICE_PORT = 1099;
	
    private ClusterManager manager;
    
    private ManagementServerHostDao _mshostDao;
    
    private String _name;
    private int _clusterServicePort = DEFAULT_SERVICE_PORT;
    
    @Override
	public ClusterService getPeerService(String strPeer) throws RemoteException {
		try {
			return (ClusterService)Naming.lookup(getServiceEndpointName(strPeer));
		} catch (MalformedURLException e) {
			s_logger.error("Malformed URL in cluster peer name");
		} catch (NotBoundException e) {
			s_logger.error("Unbound RMI exception");
		} 
		
		return null;
	}

    @Override
	public String getServiceEndpointName(String strPeer) {
    	long msid = Long.parseLong(strPeer);
    	
    	ManagementServerHostVO mshost = _mshostDao.findByMsid(msid);
    	if(mshost == null)
    		return null;
    	
    	return  composeEndpointName(mshost.getServiceIP(), mshost.getServicePort());
    }
    
    @Override
	public int getServicePort() {
    	return _clusterServicePort;
    }
    
    private String composeEndpointName(String nodeIP, int port) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("//").append(nodeIP).append(":").append(port).append("/clusterservice");
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
        
        _clusterServicePort = NumbersUtil.parseInt(dbProps.getProperty("cluster.rmi.port"), DEFAULT_SERVICE_PORT);
        if(s_logger.isInfoEnabled())
        	s_logger.info("Cluster RMI port : " + _clusterServicePort);
        
        // configuration and initialization
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        
        try {
        	ClusterService service = (ClusterService) UnicastRemoteObject.exportObject(
        		new ClusterServiceRMIImpl(manager), 0);
            Registry registry = LocateRegistry.getRegistry(_clusterServicePort);
            registry.rebind(composeEndpointName(manager.getSelfNodeIP(), getServicePort()), service);
        } catch (Exception e) {
        	throw new ConfigurationException("Unable to register RMI cluster service");
        }
        
    	return true;
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
