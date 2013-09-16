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

package com.cloud.network;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.user.Account;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Local(value = { Ipv6AddressManager.class } )
public class Ipv6AddressManagerImpl extends ManagerBase implements Ipv6AddressManager {
    public static final Logger s_logger = Logger.getLogger(Ipv6AddressManagerImpl.class.getName());

	String _name = null;
	int _ipv6RetryMax = 0;
			
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    ConfigurationDao _configDao;
    
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
        Map<String, String> configs = _configDao.getConfiguration(params);
        _ipv6RetryMax = NumbersUtil.parseInt(configs.get(Config.NetworkIPv6SearchRetryMax.key()), 10000);
		return true;
	}

	public UserIpv6Address assignDirectIp6Address(long dcId, Account owner, Long networkId, String requestedIp6)
			throws InsufficientAddressCapacityException {
		Network network = _networkDao.findById(networkId);
		if (network == null) {
			return null;
		}
    	List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
    	if (vlans == null) {
    		s_logger.debug("Cannot find related vlan attached to network " + networkId);
    		return null;
    	}
    	String ip = null; 
    	Vlan ipVlan = null;
    	if (requestedIp6 == null) {
    		if (!_networkModel.isIP6AddressAvailableInNetwork(networkId)) {
    			throw new InsufficientAddressCapacityException("There is no more address available in the network " + network.getName(), DataCenter.class, network.getDataCenterId());
    		}
    		for (Vlan vlan : vlans) {
    			if (!_networkModel.isIP6AddressAvailableInVlan(vlan.getId())) {
    				continue;
    			}
    			ip = NetUtils.getIp6FromRange(vlan.getIp6Range());
    			int count = 0;
    			while (_ipv6Dao.findByNetworkIdAndIp(networkId, ip) != null) {
    				ip = NetUtils.getNextIp6InRange(ip, vlan.getIp6Range());
    				count ++;
    				// It's an arbitrate number to prevent the infinite loop 
    				if (count > _ipv6RetryMax) {
    					ip = null;
    					break;
    				}
    			}
    			if (ip != null) {
    				ipVlan = vlan;
    			}
    		}
    		if (ip == null) {
    			throw new InsufficientAddressCapacityException("Cannot find a usable IP in the network " + network.getName() + " after " + _ipv6RetryMax + "(network.ipv6.search.retry.max) times retry!",
    						DataCenter.class, network.getDataCenterId());
    		}
    	} else {
    		for (Vlan vlan : vlans) {
    			if (NetUtils.isIp6InRange(requestedIp6, vlan.getIp6Range())) {
    				ipVlan = vlan;
    				break;
    			}
    		}
    		if (ipVlan == null) {
    			throw new CloudRuntimeException("Requested IPv6 is not in the predefined range!");
    		}
    		ip = requestedIp6;
    		if (_ipv6Dao.findByNetworkIdAndIp(networkId, ip) != null) {
    			throw new CloudRuntimeException("The requested IP is already taken!");
    		}
    	}
    	DataCenterVO dc = _dcDao.findById(dcId);
        Long mac = dc.getMacAddress();
        Long nextMac = mac + 1;
        dc.setMacAddress(nextMac);
        _dcDao.update(dc.getId(), dc);
        
    	String macAddress = NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(mac));
    	UserIpv6AddressVO ipVO = new UserIpv6AddressVO(ip, dcId, macAddress, ipVlan.getId());
    	ipVO.setPhysicalNetworkId(network.getPhysicalNetworkId());
    	ipVO.setSourceNetworkId(networkId);
    	ipVO.setState(UserIpv6Address.State.Allocated);
    	ipVO.setDomainId(owner.getDomainId());
    	ipVO.setAccountId(owner.getAccountId());
    	_ipv6Dao.persist(ipVO);
    	return ipVO;
	}

	@Override
	public void revokeDirectIpv6Address(long networkId, String ip6Address) {
		UserIpv6AddressVO ip = _ipv6Dao.findByNetworkIdAndIp(networkId, ip6Address);
		if (ip != null) {
			_ipv6Dao.remove(ip.getId());
		}
	}
}
