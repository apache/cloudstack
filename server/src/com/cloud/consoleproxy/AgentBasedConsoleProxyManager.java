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

package com.cloud.consoleproxy;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value={ConsoleProxyManager.class})
public class AgentBasedConsoleProxyManager implements ConsoleProxyManager, VirtualMachineManager<ConsoleProxyVO> {
	private static final Logger s_logger = Logger.getLogger(AgentBasedConsoleProxyManager.class);
	
    private String _name;
	protected HostDao _hostDao;
	protected UserVmDao _userVmDao;
	private String _instance;
	private VMInstanceDao _instanceDao;
	private ConsoleProxyListener _listener;
	
	protected int _consoleProxyUrlPort = ConsoleProxyManager.DEFAULT_PROXY_URL_PORT;
	protected boolean _sslEnabled = false;
	AgentManager _agentMgr;
	
	public int getVncPort(VMInstanceVO vm) {
        if (vm.getHostId() == null) {
            return -1;
        }
        GetVncPortAnswer answer = (GetVncPortAnswer)_agentMgr.easySend(vm.getHostId(), new GetVncPortCommand(vm.getId(), vm.getName()));
        return answer == null ? -1 : answer.getPort();
    }
	
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

    	if(s_logger.isInfoEnabled())
    		s_logger.info("Start configuring AgentBasedConsoleProxyManager");
    	
        _name = name;
        
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
		if (configDao == null) {
			throw new ConfigurationException(
					"Unable to get the configuration dao.");
		}
		
		Map<String, String> configs = configDao.getConfiguration(
				"management-server", params);
		String value = configs.get("consoleproxy.url.port");
		if (value != null)
			_consoleProxyUrlPort = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_URL_PORT);
        
		_hostDao = locator.getDao(HostDao.class);
		if (_hostDao == null) {
			throw new ConfigurationException("Unable to get "
					+ HostDao.class.getName());
		}
		
		_instanceDao = locator.getDao(VMInstanceDao.class);
		if (_instanceDao == null)
			throw new ConfigurationException("Unable to get " + VMInstanceDao.class.getName());
		
		_userVmDao = locator.getDao(UserVmDao.class);
		if (_userVmDao == null)
			throw new ConfigurationException("Unable to get " + UserVmDao.class.getName());
		
		_agentMgr = locator.getManager(AgentManager.class);
		if (_agentMgr == null)
            throw new ConfigurationException("Unable to get " + AgentManager.class.getName());
		
		value = configs.get("consoleproxy.sslEnabled");
		if(value != null && value.equalsIgnoreCase("true"))
			_sslEnabled = true;
		
		_instance = configs.get("instance.name");
		
		_listener = new ConsoleProxyListener(this);
		_agentMgr.registerForHostEvents(_listener, true, true, false);
		
		HighAvailabilityManager haMgr = locator.getManager(HighAvailabilityManager.class);
		haMgr.registerHandler(Type.ConsoleProxy, this);

    	if(s_logger.isInfoEnabled())
    		s_logger.info("AgentBasedConsoleProxyManager has been configured. SSL enabled: " + _sslEnabled);
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

    HostVO findHost(VMInstanceVO vm) {
        return _hostDao.findById(vm.getHostId());
    }
    
    protected ConsoleProxyVO allocateProxy(HostVO host, long dataCenterId) {
        // only private IP, public IP, host id have meaningful values, rest of all are place-holder values
        String publicIp = host.getPublicIpAddress();
        if(publicIp == null) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Host " + host.getName() + "/" + host.getPrivateIpAddress() +
                    " does not have public interface, we will return its private IP for cosole proxy.");
            publicIp = host.getPrivateIpAddress();
        }
        
        return new ConsoleProxyVO(1l, "EmbeddedProxy", State.Running, null, null, null,
                "02:02:02:02:02:02",
                host.getPrivateIpAddress(),
                "255.255.255.0",
                1l,
                1l,
                "03:03:03:03:03:03",
                publicIp,
                "255.255.255.0",
                null,
                "untagged",
                1l,
                dataCenterId,
                "0.0.0.0",
                host.getId(),
                "dns1",
                "dn2",
                "domain",
                0,
                0);
    }
    
    @Override
    public ConsoleProxyVO assignProxy(long dataCenterId, long userVmId) {
    	UserVmVO userVm = _userVmDao.findById(userVmId);
		if (userVm == null) {
			s_logger.warn("User VM " + userVmId
					+ " no longer exists, return a null proxy for user vm:"
					+ userVmId);
			return null;
		}
		
		HostVO host = findHost(userVm);
		if(host != null) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Assign embedded console proxy running at " + host.getName() + " to user vm " + userVmId + " with public IP " + host.getPublicIpAddress());
			
			// only private IP, public IP, host id have meaningful values, rest of all are place-holder values
			String publicIp = host.getPublicIpAddress();
			if(publicIp == null) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Host " + host.getName() + "/" + host.getPrivateIpAddress() +
						" does not have public interface, we will return its private IP for cosole proxy.");
				publicIp = host.getPrivateIpAddress();
			}
			
			ConsoleProxyVO proxy = allocateProxy(host, dataCenterId);
			
			if(host.getProxyPort() != null && host.getProxyPort().intValue() > 0)
				proxy.setPort(host.getProxyPort().intValue());
			else
				proxy.setPort(_consoleProxyUrlPort);
			
			proxy.setSslEnabled(_sslEnabled);
			return proxy;
		} else {
			s_logger.warn("Host that VM is running is no longer available, console access to VM " + userVmId + " will be temporarily unavailable.");
		}
    	return null;
    }

    @Override
	public void onLoadReport(ConsoleProxyLoadReportCommand cmd) {
    }

    @Override
	public AgentControlAnswer onConsoleAccessAuthentication(ConsoleAccessAuthenticationCommand cmd) {
    	long vmId = 0;
		
		if(cmd.getVmId() != null && cmd.getVmId().isEmpty()) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Invalid vm id sent from proxy(happens when proxy session has terminated)");
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		try {
			vmId = Long.parseLong(cmd.getVmId());
		} catch(NumberFormatException e) {
			s_logger.error("Invalid vm id " + cmd.getVmId() + " sent from console access authentication", e);
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		// TODO authentication channel between console proxy VM and management server needs to be secured,
		// the data is now being sent through private network, but this is apparently not enough
		VMInstanceVO vm = _instanceDao.findById(vmId);
		if(vm == null) {
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		if(vm.getHostId() == null) {
			s_logger.warn("VM " + vmId + " lost host info, failed authentication request");
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		HostVO host = _hostDao.findById(vm.getHostId());
		if(host == null) {
			s_logger.warn("VM " + vmId + "'s host does not exist, fail authentication request");
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		String sid = cmd.getSid();
		if(sid == null || !sid.equals(vm.getVncPassword())) {
			s_logger.warn("sid " + sid + " in url does not match stored sid " + vm.getVncPassword());
			return new ConsoleAccessAuthenticationAnswer(cmd, false);
		}
		
		return new ConsoleAccessAuthenticationAnswer(cmd, true);
    }

    @Override
    public void onAgentConnect(HostVO host, StartupCommand cmd) {
    }
    
    @Override
	public void onAgentDisconnect(long agentId, Status state) {
    }

    @Override
    public ConsoleProxyVO startProxy(long proxyVmId, long startEventId) {
        return null;
    }

	@Override
	public boolean destroyProxy(long proxyVmId, long startEventId) {
		return false;
	}

	@Override
	public boolean rebootProxy(long proxyVmId, long startEventId) {
		return false;
	}

	@Override
	public boolean stopProxy(long proxyVmId, long startEventId) {
		return false;
	}

	@Override
	public String getName() {
		return _name;
	}

    @Override
    public Command cleanup(ConsoleProxyVO vm, String vmName) {
        return new StopCommand(vm, vmName, null);
    }

    @Override
    public boolean completeMigration(ConsoleProxyVO vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException {
        return false;
    }

    @Override
    public void completeStartCommand(ConsoleProxyVO vm) {
    }

    @Override
    public void completeStopCommand(ConsoleProxyVO vm) {
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidConsoleProxyName(vmName, _instance)) {
            return null;
        }
        return VirtualMachineName.getConsoleProxyId(vmName);
    }

    @Override
    public boolean destroy(ConsoleProxyVO vm) throws AgentUnavailableException {
        return false;
    }

    @Override
    public ConsoleProxyVO get(long id) {
        return null;
    }

    @Override
    public boolean migrate(ConsoleProxyVO vm, HostVO host) throws AgentUnavailableException, OperationTimedoutException {
        return false;
    }

    @Override
    public HostVO prepareForMigration(ConsoleProxyVO vm) throws InsufficientCapacityException, StorageUnavailableException {
        return null;
    }

    @Override
    public ConsoleProxyVO start(long vmId, long startEventId) throws InsufficientCapacityException, StorageUnavailableException, ConcurrentOperationException {
        return null;
    }

    @Override
    public boolean stop(ConsoleProxyVO vm, long startEventId) throws AgentUnavailableException {
        return false;
    }
}
