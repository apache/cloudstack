/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.bridge.service;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.hibernate.SessionException;

import com.amazon.s3.AmazonS3SkeletonInterface;
import com.amazon.ec2.AmazonEC2SkeletonInterface;
import com.cloud.bridge.model.MHost;
import com.cloud.bridge.model.SHost;
import com.cloud.bridge.model.UserCredentials;
import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.persist.PersistException;
import com.cloud.bridge.persist.dao.MHostDao;
import com.cloud.bridge.persist.dao.SHostDao;
import com.cloud.bridge.persist.dao.UserCredentialsDao;
import com.cloud.bridge.service.core.ec2.EC2Engine;
import com.cloud.bridge.service.core.s3.S3BucketPolicy;
import com.cloud.bridge.service.core.s3.S3Engine;
import com.cloud.bridge.service.exception.ConfigurationException;
import com.cloud.bridge.util.ConfigurationHelper;
import com.cloud.bridge.util.DateHelper;
import com.cloud.bridge.util.NetHelper;
import com.cloud.bridge.util.Tuple;

/**
 * @author Kelven Yang
 */
public class ServiceProvider {
	protected final static Logger logger = Logger.getLogger(ServiceProvider.class);

	public final static long HEARTBEAT_INTERVAL = 10000;

	private static ServiceProvider instance;

	private Map<Class<?>, Object> serviceMap = new HashMap<Class<?>, Object>();
	private Timer timer = new Timer();
	private MHost mhost;
	private Properties properties;
	private boolean useSubDomain = false;		 // use DNS sub domain for bucket name
	private String serviceEndpoint = null;
	private String multipartDir = null;          // illegal bucket name used as a folder for storing multiparts
	private String masterDomain = ".s3.amazonaws.com";
	private S3Engine engine;
	private EC2Engine EC2_engine = null;

	// -> cache Bucket Policies here so we don't have to load from db on every access
	private Map<String,S3BucketPolicy> policyMap = new HashMap<String,S3BucketPolicy>();

	protected ServiceProvider() throws IOException {
		// register service implementation object
		engine = new S3Engine();
		EC2_engine = new EC2Engine();
		serviceMap.put(AmazonS3SkeletonInterface.class, new S3SoapServiceImpl(engine));
		serviceMap.put(AmazonEC2SkeletonInterface.class, new EC2SoapServiceImpl(EC2_engine));
	}

	public synchronized static ServiceProvider getInstance() {
		if(instance == null) 
		{
			try {
				instance = new ServiceProvider();
				instance.initialize();
				PersistContext.commitTransaction();
			} catch(Throwable e) {
				logger.error("Unexpected exception " + e.getMessage(), e);
			} finally {
				PersistContext.closeSession();
			}
		}
		return instance;
	}

	public long getManagementHostId() {
		// we want to limit mhost within its own session, id of the value will be returned 
		long mhostId = 0;
		if(mhost != null)
			mhostId = mhost.getId() != null ? mhost.getId().longValue() : 0L;
			return mhostId;
	}

	/** 
	 * We return a tuple to distinguish between two cases:
	 * (1) there is no entry in the map for bucketName, and (2) there is a null entry
	 * in the map for bucketName.   In case 2, the database was inspected for the
	 * bucket policy but it had none so we remember it here to reduce database lookups.
	 * @param bucketName
	 * @return Integer in the tuple means: -1 if no policy defined for the bucket, 0 if one defined
	 *         even if its set at null.
	 */
	public Tuple<S3BucketPolicy,Integer> getBucketPolicy(String bucketName) {

		if (policyMap.containsKey( bucketName )) {
			S3BucketPolicy policy = policyMap.get( bucketName );
			return new Tuple<S3BucketPolicy,Integer>( policy, 0 );
		}
		else return new Tuple<S3BucketPolicy,Integer>( null, -1 );
	}

	/**
	 * The policy parameter can be set to null, which means that there is no policy
	 * for the bucket so a database lookup is not necessary.
	 * 
	 * @param bucketName
	 * @param policy
	 */
	public void setBucketPolicy(String bucketName, S3BucketPolicy policy) {
		policyMap.put(bucketName, policy);
	}

	public void deleteBucketPolicy(String bucketName) {
		policyMap.remove(bucketName);
	}

	public S3Engine getS3Engine() {
		return engine;
	}

	public EC2Engine getEC2Engine() {
		return EC2_engine;
	}

	public String getMasterDomain() {
		return masterDomain;
	}

	public boolean getUseSubDomain() {
		return useSubDomain;
	}

	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	public String getMultipartDir() {
		return multipartDir;
	}

	public Properties getStartupProperties() {
		return properties;
	}

	public UserInfo getUserInfo(String accessKey) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		UserInfo info = new UserInfo();

		UserCredentialsDao credentialDao = new UserCredentialsDao();
		UserCredentials cloudKeys = credentialDao.getByAccessKey( accessKey ); 
		if ( null == cloudKeys ) {
			logger.debug( accessKey + " is not defined in the S3 service - call SetUserKeys" );
			return null; 
		} else {
			info.setAccessKey( accessKey );
			info.setSecretKey( cloudKeys.getSecretKey());
			info.setCanonicalUserId(accessKey);
			info.setDescription( "S3 REST request" );
			return info;
		}
	}

	protected void initialize() {
		if(logger.isInfoEnabled())
			logger.info("Initializing ServiceProvider...");

		File file = ConfigurationHelper.findConfigurationFile("log4j-cloud-bridge.xml");
		if(file != null) {
			System.out.println("Log4j configuration from : " + file.getAbsolutePath());
			DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
		} else {
			System.out.println("Configure log4j with default properties");
		}

		loadStartupProperties();
		String hostKey = properties.getProperty("host.key");
		if(hostKey == null) {
			InetAddress inetAddr = NetHelper.getFirstNonLoopbackLocalInetAddress();
			if(inetAddr != null)
				hostKey = NetHelper.getMacAddress(inetAddr);
		}
		if(hostKey == null) 
			throw new ConfigurationException("Please configure host.key property in cloud-bridge.properites");
		String host = properties.getProperty("host");
		if(host == null)
			host = NetHelper.getHostName();

		if(properties.get("bucket.dns") != null && 
				((String)properties.get("bucket.dns")).equalsIgnoreCase("true")) {
			useSubDomain = true;
		}

		serviceEndpoint = (String)properties.get("serviceEndpoint");
		masterDomain = new String( "." + serviceEndpoint );

		setupHost(hostKey, host);

		// we will commit and start a new transaction to allow host info be flushed to DB
		PersistContext.flush();

		String localStorageRoot = properties.getProperty("storage.root");
		if (localStorageRoot != null) setupLocalStorage(localStorageRoot);

		multipartDir = properties.getProperty("storage.multipartDir");

		timer.schedule(getHeartbeatTask(), HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL);

		if(logger.isInfoEnabled())
			logger.info("ServiceProvider initialized");
	}

	private void loadStartupProperties() {
		File propertiesFile = ConfigurationHelper.findConfigurationFile("cloud-bridge.properties");
		properties = new Properties(); 
		if(propertiesFile != null) {
			try {
				properties.load(new FileInputStream(propertiesFile));
			} catch (FileNotFoundException e) {
				logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
			} catch (IOException e) {
				logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
			}

			logger.info("Use startup properties file: " + propertiesFile.getAbsolutePath());
		} else {
			if(logger.isInfoEnabled())
				logger.info("Startup properties is not found.");
		}
	}

	private TimerTask getHeartbeatTask() {
		return new TimerTask() {

			@Override
			public void run() {
				try {
					MHostDao mhostDao = new MHostDao();
					mhost.setLastHeartbeatTime(DateHelper.currentGMTTime());
					mhostDao.update(mhost);
					PersistContext.commitTransaction();
				} catch(Throwable e){
					logger.error("Unexpected exception " + e.getMessage(), e);
				} finally {
					PersistContext.closeSession();
				}
			}
		};
	}

	private void setupHost(String hostKey, String host) {
		MHostDao mhostDao = new MHostDao();
		mhost = mhostDao.getByHostKey(hostKey);
		if(mhost == null) {
			mhost = new MHost();
			mhost.setHostKey(hostKey);
			mhost.setHost(host);
			mhost.setLastHeartbeatTime(DateHelper.currentGMTTime());
			mhostDao.save(mhost);
		} else {
			mhost.setHost(host);
			mhostDao.update(mhost);
		}
	}

	private void setupLocalStorage(String storageRoot) {
		SHostDao shostDao = new SHostDao();
		SHost shost = shostDao.getLocalStorageHost(mhost.getId(), storageRoot);
		if(shost == null) {
			shost = new SHost();
			shost.setMhost(mhost);
			mhost.getLocalSHosts().add(shost);
			shost.setHostType(SHost.STORAGE_HOST_TYPE_LOCAL);
			shost.setHost(NetHelper.getHostName());
			shost.setExportRoot(storageRoot);
			PersistContext.getSession().save(shost);
		}
	}

	public void shutdown() {
		timer.cancel();

		if(logger.isInfoEnabled())
			logger.info("ServiceProvider stopped");
	}

	@SuppressWarnings("unchecked")
	private static <T> T getProxy(Class<?> serviceInterface, final T serviceObject) {
		return (T) Proxy.newProxyInstance(serviceObject.getClass().getClassLoader(),
				new Class[] { serviceInterface },
				new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Object result = null;
				try {
					result = method.invoke(serviceObject, args);
					PersistContext.commitTransaction();
				} catch (PersistException e) {
				} catch (SessionException e) {
				} catch(Throwable e) {
					// Rethrow the exception to Axis:
						// Check if the exception is an AxisFault or a RuntimeException
						// enveloped AxisFault and if so, pass it on as such. Otherwise
					// log to help debugging and throw as is.
					if (e.getCause() != null && e.getCause() instanceof AxisFault)
						throw e.getCause();
					else if (e.getCause() != null && e.getCause().getCause() != null
							&& e.getCause().getCause() instanceof AxisFault)
						throw e.getCause().getCause();
					else {
						logger.warn("Unhandled exception " + e.getMessage(), e);
						throw e;
					}
				} finally {
					PersistContext.closeSession();
				}
				return result;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T> T getServiceImpl(Class<?> serviceInterface) {
		return getProxy(serviceInterface, (T)serviceMap.get(serviceInterface));
	}
}
