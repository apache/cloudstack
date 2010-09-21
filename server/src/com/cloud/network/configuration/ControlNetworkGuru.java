/**
 * 
 */
package com.cloud.network.configuration;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

@Local(value={NetworkGuru.class})
public class ControlNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(ControlNetworkGuru.class);
    @Inject DataCenterDao _dcDao;
    String _cidr;
    String _gateway;

    @Override
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration specifiedConfig, Account owner) {
        if (offering.getTrafficType() != TrafficType.Control) {
            return null;
        }
        
        NetworkConfigurationVO config = new NetworkConfigurationVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.LinkLocal, offering.getId(), plan.getDataCenterId());
        config.setCidr(_cidr);
        config.setGateway(_gateway);
        
        return config;
    }
    
    protected ControlNetworkGuru() {
        super();
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> dbParams = configDao.getConfiguration(params);
        
        _cidr = dbParams.get(Config.ControlCidr);
        if (_cidr == null) {
            _cidr = "169.254.0.0/16";
        }
        
        _gateway = dbParams.get(Config.ControlGateway);
        if (_gateway == null) {
            _gateway = "169.254.0.1";
        }
        
        s_logger.info("Control network setup: cidr=" + _cidr + "; gateway = " + _gateway);
        
        return true;
    }

    @Override
    public NicProfile allocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Control) {
            return null;
        }
        
        if (nic != null) {
            throw new CloudRuntimeException("Does not support nic specification at this time: " + nic);
        }
        
        return new NicProfile(null, null, null);
    }

    @Override
    public boolean create(NicProfile nic, VirtualMachineProfile profile) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        return true;
    }

    @Override
    public String reserve(NicProfile nic, VirtualMachineProfile vm, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        String ip = _dcDao.allocateLinkLocalPrivateIpAddress(dest.getDataCenter().getId(), dest.getPod().getId(), vm.getId());
        nic.setIp4Address(ip);
        nic.setMacAddress("FE:FF:FF:FF:FF:FF");
        nic.setNetmask("255.255.0.0");
        return Long.toString(nic.getId());
    }

    @Override
    public boolean release(String uniqueId) {
        _dcDao.releaseLinkLocalPrivateIpAddress(Long.parseLong(uniqueId));
        return true;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        return config;
    }
    
    @Override
    public void destroy(NetworkConfiguration config, NetworkOffering offering) {
    }
}
