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
package com.cloud.network.cisco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.agent.AgentManager;
import com.cloud.api.commands.AddCiscoVnmcResourceCmd;
import com.cloud.api.commands.DeleteCiscoVnmcResourceCmd;
import com.cloud.api.commands.ListCiscoVnmcResourcesCmd;
import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.network.dao.CiscoVnmcDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.element.CiscoVnmcElementService;
import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class CiscoVnmcManager implements Manager, CiscoVnmcElementService {
	ResourceManager _resourceMgr;    
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    CiscoVnmcDao _ciscoVnmcDao;
    @Inject 
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;  
    @Inject
    AgentManager _agentMgr;

    @Inject
    NetworkDao _networkDao;
    
    CiscoVnmcConnection _vnmcConnection;
    
   
	@Override
	public CiscoVnmcController addCiscoVnmcResource(AddCiscoVnmcResourceCmd cmd) {
        String deviceName = Provider.CiscoVnmc.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        CiscoVnmcController CiscoVnmcResource = null;
        
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() +
                    " is not enabled in the physical network: " + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() +
                    " is in shutdown state in the physical network: " + physicalNetworkId + "to add this device");
        }
        
        if (_ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A CiscoVnmc device is already configured on this physical network");
        }
        
        Map<String, String> params = new HashMap<String,String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Cisco VNMC Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("adminuser", cmd.getUsername());
        params.put("adminpass", cmd.getPassword());
        params.put("transportzoneisotype", physicalNetwork.getIsolationMethods().get(0).toLowerCase()); // FIXME What to do with multiple isolation types

        Map<String, Object> hostdetails = new HashMap<String,Object>();
        hostdetails.putAll(params);
        
		ServerResource resource = new CiscoVnmcResource();

        Transaction txn = Transaction.currentTxn();
        try {
            resource.configure(cmd.getHost(), hostdetails);
            
            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, params);
            if (host != null) {
                txn.start();
                
                CiscoVnmcResource = new CiscoVnmcControllerVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                _ciscoVnmcDao.persist((CiscoVnmcControllerVO) CiscoVnmcResource);
                
                DetailVO detail = new DetailVO(host.getId(), "deviceid", String.valueOf(CiscoVnmcResource.getId()));
                _hostDetailsDao.persist(detail);

                txn.commit();
                return CiscoVnmcResource;
            } else {
                throw new CloudRuntimeException("Failed to add Cisco Vnmc Device due to internal error.");
            }            
        } catch (ConfigurationException e) {
            txn.rollback();
            throw new CloudRuntimeException(e.getMessage());
        }
	}

	@Override
	public CiscoVnmcResourceResponse createCiscoVnmcResourceResponse(
			CiscoVnmcController CiscoVnmcResourceVO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteCiscoVnmcResource(DeleteCiscoVnmcResourceCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	

	@Override
	public List<CiscoVnmcControllerVO> listCiscoVnmcResources(
			ListCiscoVnmcResourcesCmd cmd) {
		Long physicalNetworkId = cmd.getPhysicalNetworkId();
		Long CiscoVnmcResourceId = cmd.getCiscoVnmcResourceId();
		List<CiscoVnmcControllerVO> responseList = new ArrayList<CiscoVnmcControllerVO>();

		if (physicalNetworkId == null && CiscoVnmcResourceId == null) {
			throw new InvalidParameterValueException("Either physical network Id or vnmc device Id must be specified");
		}

		if (CiscoVnmcResourceId != null) {
			CiscoVnmcControllerVO CiscoVnmcResource = _ciscoVnmcDao.findById(CiscoVnmcResourceId);
			if (CiscoVnmcResource == null) {
				throw new InvalidParameterValueException("Could not find Cisco Vnmc device with id: " + CiscoVnmcResource);
			}
			responseList.add(CiscoVnmcResource);
		}
		else {
			PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
			if (physicalNetwork == null) {
				throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
			}
			responseList = _ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId);
		}

		return responseList;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void assignAsa1000vToNetwork(Network network) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Class<?>> getCommands() {
		// TODO Auto-generated method stub
		return null;
	}

}
