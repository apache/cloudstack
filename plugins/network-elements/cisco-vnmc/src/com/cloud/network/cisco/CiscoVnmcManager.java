package com.cloud.network.cisco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import com.cloud.agent.AgentManager;
import com.cloud.api.commands.AddCiscoVnmcDeviceCmd;
import com.cloud.api.commands.DeleteCiscoVnmcDeviceCmd;
import com.cloud.api.commands.ListCiscoVnmcDevicesCmd;
import com.cloud.api.response.CiscoVnmcDeviceResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.ExternalNetworkDeviceManager.NetworkDevice;
import com.cloud.network.Network;
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
    
    @Override
	public String getPropertiesFile() {
		return null;
	}

	@Override
	public CiscoVnmcDeviceVO addCiscoVnmcDevice(AddCiscoVnmcDeviceCmd cmd) {
        String deviceName = CiscoVnmc.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        CiscoVnmcDeviceVO ciscoVnmcDevice = null;
        
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
        
		ServerResource resource = new CiscoVnmcResource(cmd.getHost(), cmd.getUsername(), cmd.getPassword());

        Transaction txn = Transaction.currentTxn();
        try {
            resource.configure(cmd.getHost(), hostdetails);
            
            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, params);
            if (host != null) {
                txn.start();
                
                ciscoVnmcDevice = new CiscoVnmcDeviceVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                _ciscoVnmcDao.persist(ciscoVnmcDevice);
                
                DetailVO detail = new DetailVO(host.getId(), "deviceid", String.valueOf(ciscoVnmcDevice.getId()));
                _hostDetailsDao.persist(detail);

                txn.commit();
                return ciscoVnmcDevice;
            } else {
                throw new CloudRuntimeException("Failed to add Cisco Vnmc Device due to internal error.");
            }            
        } catch (ConfigurationException e) {
            txn.rollback();
            throw new CloudRuntimeException(e.getMessage());
        }
	}

	@Override
	public CiscoVnmcDeviceResponse createCiscoVnmcDeviceResponse(
			CiscoVnmcDeviceVO ciscoVnmcDeviceVO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteCiscoVnmcDevice(DeleteCiscoVnmcDeviceCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	

	@Override
	public List<CiscoVnmcDeviceVO> listCiscoVnmcDevices(
			ListCiscoVnmcDevicesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
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

}
