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

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.user.Account;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Local(value = { Ipv6AddressManager.class } )
public class Ipv6AddressManagerImpl extends ManagerBase implements Ipv6AddressManager {
    public static final Logger s_logger = Logger.getLogger(Ipv6AddressManagerImpl.class.getName());

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    
	@Override
	public UserIpv6Address assignDirectIp6Address(long dcId, Account owner, Long networkId, String requestedIp6)
			throws InsufficientAddressCapacityException {
    	Vlan vlan = _networkModel.getVlanForNetwork(networkId);
    	if (vlan == null) {
    		s_logger.debug("Cannot find related vlan or too many vlan attached to network " + networkId);
    		return null;
    	}
    	String ip = null;
    	if (requestedIp6 == null) {
    		int count = 0;
    		while (ip == null || count >= 10) {
    			ip = NetUtils.getIp6FromRange(vlan.getIp6Range());
    			//Check for duplicate IP
    			if (_ipv6Dao.findByNetworkIdAndIp(networkId, ip) == null) {
    				break;
    			} else {
    				ip = null;
    			}
    			count ++;
    		}
    		if (ip == null) {
    			throw new CloudRuntimeException("Fail to get unique ipv6 address after 10 times trying!");
    		}
    	} else {
    		if (!NetUtils.isIp6InRange(requestedIp6, vlan.getIp6Range())) {
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
    	UserIpv6AddressVO ipVO = new UserIpv6AddressVO(ip, dcId, macAddress, vlan.getId());
    	ipVO.setPhysicalNetworkId(vlan.getPhysicalNetworkId());
    	ipVO.setSourceNetworkId(vlan.getNetworkId());
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
