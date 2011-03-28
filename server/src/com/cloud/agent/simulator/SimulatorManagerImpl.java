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
package com.cloud.agent.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentResourceBase;
import com.cloud.agent.AgentRoutingResource;
import com.cloud.agent.AgentStorageResource;
import com.cloud.agent.SimulatorManager;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.Inject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

@Local(value = { SimulatorManager.class })
public class SimulatorManagerImpl implements SimulatorManager {
	private static final Logger s_logger = Logger
			.getLogger(SimulatorManagerImpl.class);

	public static final int DEFAULT_HOST_MEM_SIZE_MB = 4000; // 4G, unit of
	// Mbytes
	public static final int DEFAULT_HOST_CPU_CORES = 4; // 2 dual core CPUs (2 x
	// 2)
	public static final int DEFAULT_HOST_STORAGE_SIZE_MB = 500000; // 500G, unit
	// of Mbytes
	public static final int DEFAULT_HOST_SPEED_MHZ = 1000; // 1 GHz CPUs

	private static final long STARTUP_DELAY = 120000;
	private static final long SCAN_INTERVAL = 120000;

	private static SimulatorManagerImpl _instance;
	private Map<AgentResourceBase, Map<String, String>> _resources = new ConcurrentHashMap<AgentResourceBase, Map<String, String>>();

	private final Properties properties = new Properties();
	private String agentTypeSequence;
	private int agentCount;
	private int _latency;

	@Inject
	protected HostPodDao _podDao;	
	@Inject 
	protected HostDao _hostDao;

	private final Timer _timer = new Timer("AgentSimulatorReport Task");

	private Random random = new Random();
	private int nextAgentId = Math.abs(random.nextInt()) % 797;
	private int _runId = Math.abs(random.nextInt()) % 113;

	/**
	 * This no-args constructor is only for the gson serializer. Not to be used
	 * otherwise
	 */
	protected SimulatorManagerImpl() {
		loadProperties();
		_instance = this;
	}

	public static SimulatorManager getInstance() {
		if (_instance == null)
			_instance = new SimulatorManagerImpl();
		return _instance;
	}

	@Override
	public Map<AgentResourceBase, Map<String, String>> createServerResources(
			Map<String, Object> params) {
		properties.putAll(params);
		Map<String, String> args = new HashMap<String, String>();
		String name;
		AgentResourceBase agentResource;
		// make first ServerResource as SecondaryStorage (single one per zone)
		if (_hostDao.findSecondaryStorageHost(getZone()) == null) {
			agentResource = new AgentStorageResource(1,
					AgentType.Storage, this);
			if (agentResource != null) {
				try {
					agentResource.start();
					name = "SimulatedAgent." + 1;
					args.put(name, name);
					agentResource.configure(name, PropertiesUtil
							.toMap(properties));
				} catch (ConfigurationException e) {
					s_logger.error("error while configuring server resource"
							+ e.getMessage());
				}
				_resources.put(agentResource, args);
			}
		}

		synchronized (this) {
			for (int i = 0; i < getWorkers(); i++) {
				int agentId = getNextAgentId();
				if (getAgentType(i) == AgentType.Routing) {
					agentResource = new AgentRoutingResource(agentId,
							AgentType.Routing, this);
				} else {
					// rest of the storage resources are PrimaryPools
					agentResource = new AgentStorageResource(agentId,
							AgentType.Storage, this);
				}
				if (agentResource != null) {
					try {
						agentResource.start();
						agentResource.configure("SimulatedAgent." + agentId,
								PropertiesUtil.toMap(properties));
						name = "SimulatedAgent" + agentId;
						args.put(name, name);
					} catch (ConfigurationException e) {
						s_logger
								.error("error while configuring server resource"
										+ e.getMessage());
					}
					_resources.put(agentResource, args);
				}
			}
		}
		return _resources;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	public class FileAgentStateFilter implements FilenameFilter {
		String _ext;

		public FileAgentStateFilter() {
			_ext = ".json";
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(_ext);
		}
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		try {
			FileAgentStateFilter filter = new FileAgentStateFilter();
			File dir = new File(getAgentPath());
			if (!dir.exists()) {
				s_logger
						.error("The directory "
								+ getAgentPath()
								+ " containing agent states does not exist. Assuming fresh run.");
				return false;
			} else {
				File[] agentStateFiles = dir.listFiles(filter);
				if (agentStateFiles.length == 0) {
					s_logger
							.error("The directory "
									+ getAgentPath()
									+ " containing agent states is empty. Assuming fresh run");
					return false;
				}
				restoreAllResourceStates(agentStateFiles);
			}
		} catch (FileNotFoundException e) {
			s_logger.info("Failed to stop simulator because of "
					+ e.getStackTrace());
		} catch (IOException e) {
			s_logger.info("Failed to stop simulator because of "
					+ e.getStackTrace());
		}
		return true;
	}

	private void restoreAllResourceStates(File[] agentStateFiles)
			throws FileNotFoundException, JsonParseException, IOException {
		Map<String, String> args = new HashMap<String, String>();
		String name;
		Gson gson = new GsonBuilder().create();
		for (File agentStateFile : agentStateFiles) {
			Reader reader = new InputStreamReader(new FileInputStream(
					agentStateFile));

			name = "SimulatedAgent";
			args.put(name, name);

			if (agentStateFile.getName().startsWith("AgentRouting")) {
				AgentRoutingResource res = gson.fromJson(reader,
						AgentRoutingResource.class);
				reader.close();
				synchronized (_resources) {
					_resources.put(res, args);
				}

			} else {
				AgentStorageResource res = gson.fromJson(reader,
						AgentStorageResource.class);
				reader.close();
				synchronized (_resources) {
					_resources.put(res, args);
				}
			}
		}
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean start() {
		s_logger.info("Simulator started");
		loadProperties();
		_timer.schedule(new AgentSimulatorReportTask(), STARTUP_DELAY,
				SCAN_INTERVAL);
		return true;
	}

	@Override
	public boolean stop() {
		try {
			saveAllResourceStates(getAgentPath());
		} catch (FileNotFoundException e) {
			s_logger.info("Failed to stop simulator because of "
					+ e.getStackTrace());
		} catch (IOException e) {
			s_logger.info("Failed to stop simulator because of "
					+ e.getStackTrace());
		}
		s_logger.info("Simulator stopped successfully");
		return true;
	}

	@Override
	public synchronized boolean saveResourceState(String path,
			AgentResourceBase resource) {
		s_logger.info("Attempting to save resource state");
		File dir;
		if (path == null) {
			path = getAgentPath();
		}
		dir = new File(path);
		if (!dir.exists()) {
			s_logger.info("Creating new agent directory path: "
					+ dir.getAbsolutePath());
			dir.mkdir();
		}
		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues()
				.serializeNulls().create();
		Writer writer;
		try {
			String filePrefix = getFilePrefix(resource);
			s_logger.info("Saving resource with instanceid: "
					+ resource.getInstanceId() + " to path: "
					+ dir.getAbsolutePath());
			writer = new OutputStreamWriter(new FileOutputStream(new File(dir
					.getAbsolutePath(), filePrefix + "."
					+ resource.getInstanceId() + ".json")));
			gson.toJson(resource, writer);
			writer.close();
		} catch (FileNotFoundException e) {
			s_logger.error("FileNotFoundException " + e.getMessage());
		} catch (IOException e) {
			s_logger.error("IOException: " + e.getMessage());
		}
		s_logger.info("Finished save resource state");
		return true;
	}

	private String getFilePrefix(AgentResourceBase resource) {
		if (resource.getType() == Host.Type.Routing)
			return "AgentRouting";
		else
			return "AgentStorage";
	}

	public synchronized boolean saveAllResourceStates(String path)
			throws FileNotFoundException, IOException {
		for (Entry<AgentResourceBase, Map<String, String>> res : _resources
				.entrySet()) {
			AgentResourceBase resource = res.getKey();
			saveResourceState(path, resource);
		}
		return true;
	}

	private class AgentSimulatorReportTask extends TimerTask {
		@Override
		public void run() {
			try {
				int size = _hostDao.listBy(Host.Type.Routing, getZone()).size();
				s_logger.info("Simulator running with: " + size
						+ " hosts in zone: " + getZone());
			} catch (Throwable e) {
				s_logger.error("Unexpected exception " + e.getMessage(), e);
			}
		}
	}

	private void loadProperties() {
		File file = PropertiesUtil.findConfigFile("simulator.properties");
		if (file != null) {
			try {
				properties.load(new FileInputStream(file));
			} catch (FileNotFoundException ex) {
				s_logger
						.info("simulator.properties file was not found. using defaults");
			} catch (IOException ex) {
			}
		} else {
			s_logger
					.warn("Could not find simulator.properties for loading simulator args");
		}
	}

	@Override
	public synchronized int getNextAgentId() {
		return nextAgentId++;
	}

	@Override
	public AgentType getAgentType(int iteration) {
		switch (getSequence().charAt(iteration % getSequence().length())) {
		case 'r':
			return AgentType.Routing;

		case 's':
			return AgentType.Storage;

		default:
			s_logger
					.error("Invalid agent type character, default to routing agent");
			break;
		}
		return AgentType.Routing;
	}

	@Override
	public String getAgentPath() {
		return properties.getProperty("agent_save_path");
	}

	@Override
	public String getHost() {
		return properties.getProperty("host");
	}

	@Override
	public int getPort() {
		return NumbersUtil.parseInt(properties.getProperty("port"), 8250);
	}

	@Override
	public int getWorkers() {
		agentCount = NumbersUtil.parseInt(properties.getProperty("workers"), 3);
		return agentCount;
	}

	@Override
	public String getSequence() {
		agentTypeSequence = properties.getProperty("sequence", "rs");
		return agentTypeSequence;
	}

	@Override
	public Long getZone() {
		return Long.parseLong(properties.getProperty("zone"));
	}

	@Override
	public Long getPod() {
		return Long.parseLong(properties.getProperty("pod"));
	}

	@Override
	public int getRunId() {
		_runId = NumbersUtil.parseInt(properties.getProperty("run"), 0);
		return _runId;
	}

	@Override
	public int getHostLatencyInSeconds() {
		_latency = NumbersUtil.parseInt(properties.getProperty("latency"), 0);
		return _latency;

	}

	@Override
	public int getHostMemSizeInMB() {
		return DEFAULT_HOST_MEM_SIZE_MB;
	}

	@Override
	public int getHostCpuCores() {
		return DEFAULT_HOST_CPU_CORES;
	}

	@Override
	public int getHostCpuSpeedInMHz() {
		return DEFAULT_HOST_SPEED_MHZ;
	}

	@Override
	public int getHostStorageInMB() {
		return DEFAULT_HOST_STORAGE_SIZE_MB;
	}

	@Override
	public String getPodCidrPrefix() {
		try {
			long podId = getPod();
			HashMap<Long, List<Object>> podMap = _podDao
					.getCurrentPodCidrSubnets(getZone(), 0);
			List<Object> cidrPair = podMap.get(podId);
			String cidrAddress = (String) cidrPair.get(0);
			String prefix = cidrAddress.split("\\.")[0] + "."
					+ cidrAddress.split("\\.")[1] + "."
					+ cidrAddress.split("\\.")[2];
			return prefix;
		} catch (PatternSyntaxException e) {
			s_logger.error("Exception while splitting pod cidr");
			return null;
		} catch(IndexOutOfBoundsException e) {
			s_logger.error("Invalid pod cidr. Please check");
			return null;
		}
	}

	@Override
	public synchronized boolean checkPoolForResource(String name,
			Map<String, Object> params) {
		for (Entry<AgentResourceBase, Map<String, String>> res : _resources
				.entrySet()) {
			if (res.getKey().getName().equalsIgnoreCase(name)) {
				s_logger.info("server resource: " + name
						+ " already exists. reconnecting ...");
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized AgentResourceBase getResourceByName(String name) {
		for (Entry<AgentResourceBase, Map<String, String>> res : _resources
				.entrySet()) {
			if (res.getKey().getName().equalsIgnoreCase(name)) {
				return res.getKey();
			}
		}
		return null;
	}	
}