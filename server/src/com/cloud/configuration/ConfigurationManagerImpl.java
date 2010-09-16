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

package com.cloud.configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value={ConfigurationManager.class})
public class ConfigurationManagerImpl implements ConfigurationManager {
    public static final Logger s_logger = Logger.getLogger(ConfigurationManagerImpl.class.getName());

	String _name;
	@Inject ConfigurationDao _configDao;
	@Inject HostPodDao _podDao;
	@Inject AccountVlanMapDao _accountVlanMapDao;
	@Inject PodVlanMapDao _podVlanMapDao;
	@Inject DataCenterDao _zoneDao;
	@Inject DomainRouterDao _domrDao;
	@Inject ServiceOfferingDao _serviceOfferingDao;
	@Inject DiskOfferingDao _diskOfferingDao;
	@Inject VlanDao _vlanDao;
	@Inject IPAddressDao _publicIpAddressDao;
	@Inject DataCenterIpAddressDaoImpl _privateIpAddressDao;
	@Inject VMInstanceDao _vmInstanceDao;
	@Inject AccountDao _accountDao;
	@Inject EventDao _eventDao;
	@Inject UserDao _userDao;
	public boolean _premium;
 
    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
    	_name = name;
        
        Object premium = params.get("premium");
        _premium = (premium != null) && ((String) premium).equals("true");
        
    	return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    @Override
    public boolean isPremium() {
    	return _premium;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    public void updateConfiguration(long userId, String name, String value) throws InvalidParameterValueException, InternalErrorException {
    	String validationMsg = validateConfigurationValue(name, value);
    	
    	if (validationMsg != null) {
    		s_logger.error("Invalid configuration option, name: " + name + ", value:" + value);
    		throw new InvalidParameterValueException(validationMsg);
    	}
    	
    	if (!_configDao.update(name, value)) {
    		s_logger.error("Failed to update configuration option, name: " + name + ", value:" + value);
    		throw new InternalErrorException("Failed to update configuration value. Please contact Cloud Support.");
    	}
    	
    	saveConfigurationEvent(userId, null, EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, "Successfully edited configuration value.", "name=" + name, "value=" + value);
    }
    
    private String validateConfigurationValue(String name, String value) {
    	Config c = Config.getConfig(name);
    	value = value.trim();
    	
    	if (c == null) {
    		s_logger.error("Missing configuration variable " + name + " in configuration table");
    		return "Invalid configuration variable.";
    	}
    	
    	Class<?> type = c.getType();
    	if (type.equals(Boolean.class)) {
    		if (!(value.equals("true") || value.equals("false"))) {
 	    		s_logger.error("Configuration variable " + name + " is expecting true or false in stead of " + value);
    			return "Please enter either \"true\" or \"false\".";
    		}
    		return null;
    	}
    	
		String range = c.getRange();
		if (range == null) {
			return null;
		}
		
    	if(type.equals(String.class)) {
			if (range.equals("privateip")) {
				if (!NetUtils.isSiteLocalAddress(value)) {
					s_logger.error("privateip range " + value
									+ " is not a site local address for configuration variable " + name);
					return "Please enter a site local IP address.";
				}
			} else if (range.equals("netmask")) {
				if (!NetUtils.isValidNetmask(value)) {
					s_logger.error("netmask " + value + " is not a valid net mask for configuration variable " + name);
					return "Please enter a valid netmask.";
				}
			} else {
				String [] options = range.split(",");
				for( String option : options) {
					if( option.trim().equals(value) ) {
						return null;
					}
				}
				s_logger.error("configuration value for " + name + " is invalid");
				return "Please enter : " + range;
				
			}
    	}else if(type.equals(Integer.class)) {
			String [] options = range.split("-");
			if( options.length != 2 ) {
				String msg = "configuration range " + range + " for " + name + " is invalid";
				s_logger.error(msg);
				return msg;
			}
			int min = Integer.parseInt(options[0]);
			int max = Integer.parseInt(options[1]);
			int val = Integer.parseInt(value);
			if( val < min || val > max ) {
				s_logger.error("configuration value for " + name + " is invalid");
				return "Please enter : " + range;
			}
    	}
    	return null;
    }
    
    private boolean podHasAllocatedPrivateIPs(long podId) {
    	HostPodVO pod = _podDao.findById(podId);
    	int count = _privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true);
    	return (count > 0);
    }
    
    @DB
    protected void checkIfPodIsDeletable(long podId) throws InternalErrorException {
    	List<List<String>> tablesToCheck = new ArrayList<List<String>>();
    	
    	HostPodVO pod = _podDao.findById(podId);
    	
    	// Check if there are allocated private IP addresses in the pod
    	if (_privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true) != 0) {
    		throw new InternalErrorException("There are private IP addresses allocated for this pod");
    	}

    	List<String> volumes = new ArrayList<String>();
    	volumes.add(0, "volumes");
    	volumes.add(1, "pod_id");
    	volumes.add(2, "there are storage volumes for this pod");
    	tablesToCheck.add(volumes);
    	
    	List<String> host = new ArrayList<String>();
    	host.add(0, "host");
    	host.add(1, "pod_id");
    	host.add(2, "there are servers running in this pod");
    	tablesToCheck.add(host);
    	
    	List<String> vmInstance = new ArrayList<String>();
    	vmInstance.add(0, "vm_instance");
    	vmInstance.add(1, "pod_id");
    	vmInstance.add(2, "there are virtual machines running in this pod");
    	tablesToCheck.add(vmInstance);
    	
    	List<String> alert = new ArrayList<String>();
		alert.add(0, "alert");
		alert.add(1, "pod_id");
		alert.add(2, "there are alerts for this pod");
		tablesToCheck.add(alert);
    	
    	for (List<String> table : tablesToCheck) {
    		String tableName = table.get(0);
    		String column = table.get(1);
    		String errorMsg = table.get(2);
    		
    		String dbName;
    		if (tableName.equals("event") || tableName.equals("cloud_usage") || tableName.equals("usage_vm_instance") ||
    			tableName.equals("usage_ip_address") || tableName.equals("usage_network") || tableName.equals("usage_job") ||
    			tableName.equals("account") || tableName.equals("user_statistics")) {
    			dbName = "cloud_usage";
    		} else {
    			dbName = "cloud";
    		}
    		
    		String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";
    		
            Transaction txn = Transaction.currentTxn();
    		try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, podId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                	throw new InternalErrorException("The pod cannot be edited because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new InternalErrorException("The Management Server failed to detect if pod is editable. Please contact Cloud Support.");
            }
    	}
    }
    
    private void checkPodAttributes(long podId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, boolean checkForDuplicates) throws InvalidParameterValueException {
    	// Check if the zone is valid
		if (!validZone(zoneId)) {
			throw new InvalidParameterValueException("Please specify a valid zone.");
		}

		if (checkForDuplicates) {
			// Check if the pod already exists
			if (validPod(podName, zoneId)) {
				throw new InvalidParameterValueException("A pod with name: " + podName + " already exists in zone " + zoneId + ". Please specify a different pod name. ");
			}
		}
		
		String cidrAddress;
		long cidrSize;
		// Get the individual cidrAddress and cidrSize values, if the CIDR is valid. If it's not valid, return an error.
		if (NetUtils.isValidCIDR(cidr)) {
			cidrAddress = getCidrAddress(cidr);
			cidrSize = getCidrSize(cidr);
		} else {
			throw new InvalidParameterValueException("Please enter a valid CIDR for pod: " + podName);
		}
		
		// Check if the IP range is valid
		if (startIp != null || endIp != null) {
			checkIpRange(startIp, endIp, cidrAddress, cidrSize);
		}
		
		// Check if the gateway is a valid IP address
		if (!NetUtils.isValidIp(gateway)) {
    		throw new InvalidParameterValueException("The gateway is not a valid IP address.");
    	}
		
		// Check if the gateway is in the CIDR subnet
		if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
    	}
		
		String checkPodCIDRs = _configDao.getValue("check.pod.cidrs");
		if (checkPodCIDRs == null || checkPodCIDRs.trim().isEmpty() || Boolean.parseBoolean(checkPodCIDRs)) {
			// Check if the CIDR conflicts with the Guest Network or other pods
			HashMap<Long, List<Object>> currentPodCidrSubnets = _podDao.getCurrentPodCidrSubnets(zoneId, podId);
			List<Object> newCidrPair = new ArrayList<Object>();
			newCidrPair.add(0, cidrAddress);
			newCidrPair.add(1, new Long(cidrSize));
			currentPodCidrSubnets.put(new Long(-1), newCidrPair);
			checkPodCidrSubnets(zoneId, currentPodCidrSubnets);
		}
    }
    
    @DB
    public void deletePod(long userId, long podId) throws InvalidParameterValueException, InternalErrorException {
    	// Make sure the pod exists
    	if (!validPod(podId)) {
    		throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
    	}
    	
    	checkIfPodIsDeletable(podId);
    	
    	HostPodVO pod = _podDao.findById(podId);
    	DataCenterVO zone = _zoneDao.findById(pod.getDataCenterId());

    	_podDao.delete(podId);
    	
    	// Delete private IP addresses in the pod
    	_privateIpAddressDao.deleteIpAddressByPod(podId);
    	
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_DELETE, "Successfully deleted pod with name: " + pod.getName() + " in zone: " + zone.getName() + ".", "podId=" + podId, "dcId=" + zone.getId());
    }
    
    @DB
    public HostPodVO editPod(long userId, long podId, String newPodName, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException {
    	// Make sure the pod exists
    	if (!validPod(podId)) {
    		throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
    	}
    	
    	// If the gateway, CIDR, private IP range is being updated, check if the pod has allocated private IP addresses
    	if (gateway!= null || cidr != null || startIp != null || endIp != null) {
    		if (podHasAllocatedPrivateIPs(podId)) {
    			throw new InternalErrorException("The specified pod has allocated private IP addresses, so its CIDR and IP address range cannot be changed.");
    		}
    	}
    	
    	HostPodVO pod = _podDao.findById(podId);
    	String oldPodName = pod.getName();
    	long zoneId = pod.getDataCenterId();
    	
    	if (newPodName == null) {
    		newPodName = oldPodName;
    	}
    	
    	if (gateway == null) {
    		gateway = pod.getGateway();
    	}
    	
    	if (cidr == null) {
    		cidr = pod.getCidrAddress() + "/" + pod.getCidrSize();
    	}
    	
    	boolean checkForDuplicates = !oldPodName.equals(newPodName);
    	checkPodAttributes(podId, newPodName, pod.getDataCenterId(), gateway, cidr, startIp, endIp, checkForDuplicates);
    	
    	String cidrAddress = getCidrAddress(cidr);
    	long cidrSize = getCidrSize(cidr);
    	
    	if (startIp != null && endIp == null) {
    		endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
		}
		
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			String ipRange;
			if (startIp != null) {
				// remove old private ip address
				_zoneDao.deletePrivateIpAddressByPod(pod.getId());
				
				// re-allocate private ip addresses for pod
				_zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
				
				ipRange = startIp + "-";
				if (endIp != null) {
					ipRange += endIp;
				}
			} else {
				ipRange = pod.getDescription();
			}
			
	    	pod.setName(newPodName);
	    	pod.setDataCenterId(zoneId);
	    	pod.setGateway(gateway);
	    	pod.setCidrAddress(cidrAddress);
	    	pod.setCidrSize(cidrSize);
	    	pod.setDescription(ipRange);
	    	
	    	if (!_podDao.update(podId, pod)) {
	    		throw new InternalErrorException("Failed to edit pod. Please contact Cloud Support.");
	    	}
    	
	    	txn.commit();
		} catch(Exception e) {
			s_logger.error("Unable to edit pod due to " + e.getMessage(), e);
			txn.rollback();
			throw new InternalErrorException("Failed to edit pod. Please contact Cloud Support.");
		}
		
		DataCenterVO zone = _zoneDao.findById(zoneId);
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_EDIT, "Successfully edited pod. New pod name is: " + newPodName + " and new zone name is: " + zone.getName() + ".", "podId=" + pod.getId(), "dcId=" + zone.getId(), "gateway=" + gateway, "cidr=" + cidr, "startIp=" + startIp, "endIp=" + endIp);
		
		return pod;
    }
    
    @DB
    public HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException {
    	checkPodAttributes(-1, podName, zoneId, gateway, cidr, startIp, endIp, true);
		
		String cidrAddress = getCidrAddress(cidr);
		long cidrSize = getCidrSize(cidr);
		
		if (startIp != null) {
			if (endIp == null) {
				endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
			}
		}
		
		// Create the new pod in the database
		String ipRange;
		if (startIp != null) {
			ipRange = startIp + "-";
			if (endIp != null) {
				ipRange += endIp;
			}
		} else {
			ipRange = "";
		}
		
		HostPodVO pod = new HostPodVO(podName, zoneId, gateway, cidrAddress, cidrSize, ipRange);
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			if (_podDao.persist(pod) == null) {
				txn.rollback();
				throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
			}
			
			if (startIp != null) {
				_zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
			}
			
			String[] linkLocalIpRanges = getLinkLocalIPRange();
			if (linkLocalIpRanges != null) {
				_zoneDao.addLinkLocalPrivateIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
			}

			txn.commit();

		} catch(Exception e) {
			txn.rollback();
			s_logger.error("Unable to create new pod due to " + e.getMessage(), e);
			throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
		}
		
		DataCenterVO zone = _zoneDao.findById(zoneId);
		saveConfigurationEvent(userId, null, EventTypes.EVENT_POD_CREATE, "Successfully created new pod with name: " + podName + " in zone: " + zone.getName() + ".", "podId=" + pod.getId(), "zoneId=" + zone.getId(), "gateway=" + gateway, "cidr=" + cidr, "startIp=" + startIp, "endIp=" + endIp);
		
		return pod;
    }
    
    private boolean zoneHasVMs(long zoneId) throws InternalErrorException {
    	List<VMInstanceVO> vmInstances = _vmInstanceDao.listByZoneId(zoneId);
    	return !vmInstances.isEmpty();
    }
    
    private boolean zoneHasAllocatedVnets(long zoneId) throws InternalErrorException {
    	return !_zoneDao.listAllocatedVnets(zoneId).isEmpty();
    }
    
    @DB
    protected void checkIfZoneIsDeletable(long zoneId) throws InternalErrorException {
    	List<List<String>> tablesToCheck = new ArrayList<List<String>>();
    	
    	List<String> alert = new ArrayList<String>();
		alert.add(0, "alert");
		alert.add(1, "data_center_id");
		alert.add(2, "there are alerts for this zone");
		tablesToCheck.add(alert);
    	
    	List<String> host = new ArrayList<String>();
    	host.add(0, "host");
    	host.add(1, "data_center_id");
    	host.add(2, "there are servers running in this zone");
    	tablesToCheck.add(host);
    	
    	List<String> hostPodRef = new ArrayList<String>();
		hostPodRef.add(0, "host_pod_ref");
		hostPodRef.add(1, "data_center_id");
		hostPodRef.add(2, "there are pods in this zone");
		tablesToCheck.add(hostPodRef);
    	
    	List<String> privateIP = new ArrayList<String>();
    	privateIP.add(0, "op_dc_ip_address_alloc");
    	privateIP.add(1, "data_center_id");
    	privateIP.add(2, "there are private IP addresses allocated for this zone");
    	tablesToCheck.add(privateIP);
    	
    	List<String> publicIP = new ArrayList<String>();
    	publicIP.add(0, "user_ip_address");
    	publicIP.add(1, "data_center_id");
    	publicIP.add(2, "there are public IP addresses allocated for this zone");
    	tablesToCheck.add(publicIP);
    	
    	List<String> vmInstance = new ArrayList<String>();
    	vmInstance.add(0, "vm_instance");
    	vmInstance.add(1, "data_center_id");
    	vmInstance.add(2, "there are virtual machines running in this zone");
    	tablesToCheck.add(vmInstance);
    	
    	List<String> volumes = new ArrayList<String>();
    	volumes.add(0, "volumes");
    	volumes.add(1, "data_center_id");
    	volumes.add(2, "there are storage volumes for this zone");
    	tablesToCheck.add(volumes);
    	
    	List<String> vnet = new ArrayList<String>();
    	vnet.add(0, "op_dc_vnet_alloc");
    	vnet.add(1, "data_center_id");
    	vnet.add(2, "there are allocated vnets for this zone");
    	tablesToCheck.add(vnet);

    	for (List<String> table : tablesToCheck) {
    		String tableName = table.get(0);
    		String column = table.get(1);
    		String errorMsg = table.get(2);
    		
    		String dbName = "cloud";
    		
    		String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";
    		
    		if (tableName.equals("op_dc_vnet_alloc")) {
    			selectSql += " AND taken IS NOT NULL";
    		}
    		
            Transaction txn = Transaction.currentTxn();
    		try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, zoneId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                	throw new InternalErrorException("The zone is not deletable because " + errorMsg);
                }
            } catch (SQLException ex) {
            	throw new InternalErrorException("The Management Server failed to detect if zone is deletable. Please contact Cloud Support.");
            }
    	}
    
    }
    
    private void checkZoneParameters(String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, boolean checkForDuplicates) throws InvalidParameterValueException {
    	if (checkForDuplicates) {
    		// Check if a zone with the specified name already exists
    		if (validZone(zoneName)) {
    			throw new InvalidParameterValueException("A zone with that name already exists. Please specify a unique zone name.");
    		}
    	}
    	
    	// Check IP validity for DNS addresses
    	
		if (dns1 != null  && !NetUtils.isValidIp(dns1)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for DNS1");
		}
		
		if (dns2 != null  && !NetUtils.isValidIp(dns2)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for DNS2");
		}
		
		if (internalDns1 != null  && !NetUtils.isValidIp(internalDns1)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS1");
		}
		
		if (internalDns2 != null  && !NetUtils.isValidIp(internalDns2)) {
			throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS2");
		}
    }
    
    private void checkIpRange(String startIp, String endIp, String cidrAddress, long cidrSize) throws InvalidParameterValueException {
    	if (!NetUtils.isValidIp(startIp)) {
    		throw new InvalidParameterValueException("The start address of the IP range is not a valid IP address.");
    	}
    	
    	if (endIp != null && !NetUtils.isValidIp(endIp)) {
    		throw new InvalidParameterValueException("The end address of the IP range is not a valid IP address.");
    	}
    	
    	if (!NetUtils.getCidrSubNet(startIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The start address of the IP range is not in the CIDR subnet.");
    	}
    	
    	if (endIp != null && !NetUtils.getCidrSubNet(endIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
    		throw new InvalidParameterValueException("The end address of the IP range is not in the CIDR subnet.");
    	}
    	
    	if (endIp != null && NetUtils.ip2Long(startIp) > NetUtils.ip2Long(endIp)) {
			throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
		}
    	
    }
    
    @DB
    public void deleteZone(long userId, long zoneId) throws InvalidParameterValueException, InternalErrorException {
    	// Make sure the zone exists
    	if (!validZone(zoneId)) {
    		throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
    	}
    	
    	checkIfZoneIsDeletable(zoneId);
    	
    	DataCenterVO zone = _zoneDao.findById(zoneId);
    	
    	_zoneDao.delete(zoneId);
    	
    	// Delete vNet
        _zoneDao.deleteVnet(zoneId);
        
        saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_DELETE, "Successfully deleted zone with name: " + zone.getName() + ".", "dcId=" + zoneId);
    }
    
    @Override
    public DataCenterVO editZone(long userId, long zoneId, String newZoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException {
    	// Make sure the zone exists
    	if (!validZone(zoneId)) {
    		throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
    	}
    	
    	// If DNS values are being changed, make sure there are no VMs in this zone
    	if (dns1 != null || dns2 != null || internalDns1 != null || internalDns2 != null) {
    		if (zoneHasVMs(zoneId)) {
    			throw new InternalErrorException("The zone is not editable because there are VMs in the zone.");
    		}
    	}
    	
    	// If the Vnet range is being changed, make sure there are no allocated VNets
    	if (vnetRange != null) {
    		if (zoneHasAllocatedVnets(zoneId)) {
    			throw new InternalErrorException("The vlan range is not editable because there are allocated vlans.");
    		}
    	}
    	
    	//To modify a zone, we need to make sure there are no domr's associated with it
    	//1. List all the domain router objs
    	//2. Check if any of these has the current data center associated
    	//3. If yes, throw exception
    	//4, If no, edit
    	List<DomainRouterVO> allDomainRoutersAvailable = _domrDao.listAll();
    	
    	for(DomainRouterVO domR : allDomainRoutersAvailable)
    	{
    		if(domR.getDataCenterId() == zoneId)
    		{
    			throw new InternalErrorException("The zone is not editable because there are domR's associated with the zone.");
    		}
    	}
    	
    	//5. Reached here, hence editable
    	
    	DataCenterVO zone = _zoneDao.findById(zoneId);
    	String oldZoneName = zone.getName();
    	
    	if (newZoneName == null) {
    		newZoneName = oldZoneName;
    	}
    	
    	if (dns1 == null) {
    		dns1 = zone.getDns1();
    	}
    	
    	if(internalDns1 == null)
    	{
    		internalDns1 = zone.getInternalDns1();
    	}

    	boolean checkForDuplicates = !newZoneName.equals(oldZoneName);
    	checkZoneParameters(newZoneName, dns1, dns2, internalDns1, internalDns2, checkForDuplicates);

    	zone.setName(newZoneName);
    	zone.setDns1(dns1);
    	zone.setDns2(dns2);
    	zone.setInternalDns1(internalDns1);
    	zone.setInternalDns2(internalDns2);
    	
    	if(guestCidr != null)
    		zone.setGuestNetworkCidr(guestCidr);
    	
    	if (vnetRange != null) {
    		zone.setVnet(vnetRange);
    	}
    	
    	if (!_zoneDao.update(zoneId, zone)) {
    		throw new InternalErrorException("Failed to edit zone. Please contact Cloud Support.");
    	}
    	
    	if (vnetRange != null) {
    		String[] tokens = vnetRange.split("-");
	    	int begin = Integer.parseInt(tokens[0]);
	    	int end = tokens.length == 1 ? (begin + 1) : Integer.parseInt(tokens[1]);
	    	
	    	_zoneDao.deleteVnet(zoneId);
	    	_zoneDao.addVnet(zone.getId(), begin, end);
    	}
    	
    	saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_EDIT, "Successfully edited zone with name: " + zone.getName() + ".", "dcId=" + zone.getId(), "dns1=" + dns1, "dns2=" + dns2, "internalDns1=" + internalDns1, "internalDns2=" + internalDns2, "vnetRange=" + vnetRange, "guestCidr=" + guestCidr);
    	
    	return zone;
    }
    
    @DB
    public DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException {
    	
        int vnetStart, vnetEnd;
        if (vnetRange != null) {
            String[] tokens = vnetRange.split("-");
            
            try {
            	vnetStart = Integer.parseInt(tokens[0]);
            	if (tokens.length == 1) {
            		vnetEnd = vnetStart + 1;
            	} else {
            		vnetEnd = Integer.parseInt(tokens[1]);
            	}
            } catch (NumberFormatException e) {
            	throw new InvalidParameterValueException("Please specify valid integers for the vlan range.");
            }
        } else {
        	String networkType = _configDao.getValue("network.type");
        	if (networkType != null && networkType.equals("vnet")) {
        		vnetStart = 1000;
                vnetEnd = 2000;
        	} else {
        		throw new InvalidParameterValueException("Please specify a vlan range.");
        	}
        }
        
        //checking the following params outside checkzoneparams method as we do not use these params for updatezone
        //hence the method below is generic to check for common params
        if(guestCidr!=null && !NetUtils.isValidCIDR(guestCidr))
        {
        	throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }
        
    	checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2,true);
		
		// Create the new zone in the database
		DataCenterVO zone = new DataCenterVO(null, zoneName, null, dns1, dns2, internalDns1, internalDns2, vnetRange, guestCidr);
		zone = _zoneDao.persist(zone);
		
		// Add vnet entries for the new zone
    	_zoneDao.addVnet(zone.getId(), vnetStart, vnetEnd);
		
		saveConfigurationEvent(userId, null, EventTypes.EVENT_ZONE_CREATE, "Successfully created new zone with name: " + zoneName + ".", "dcId=" + zone.getId(), "dns1=" + dns1, "dns2=" + dns2, "internalDns1=" + internalDns1, "internalDns2=" + internalDns2, "vnetRange=" + vnetRange, "guestCidr=" + guestCidr);
    	
		return zone;
    }
    
    public ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags) {
    	String networkRateStr = _configDao.getValue("network.throttling.rate");
    	String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
    	int networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
    	int multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
    	GuestIpType guestIpType = useVirtualNetwork ? GuestIpType.Virtualized : GuestIpType.DirectSingle;        
    	tags = cleanupTags(tags);
    	ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, multicastRate, offerHA, displayText, guestIpType, localStorageRequired, false, tags);
    	
    	if ((offering = _serviceOfferingDao.persist(offering)) != null) {
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_CREATE, "Successfully created new service offering with name: " + name + ".", "soId=" + offering.getId(), "name=" + name, "numCPUs=" + cpu, "ram=" + ramSize, "cpuSpeed=" + speed,
    				"displayText=" + displayText, "guestIPType=" + guestIpType, "localStorageRequired=" + localStorageRequired, "offerHA=" + offerHA, "useVirtualNetwork=" + useVirtualNetwork, "tags=" + tags);
    		return offering;
    	} else {
    		return null;
    	}
    }
    
    public ServiceOfferingVO updateServiceOffering(long userId, long serviceOfferingId, String name, String displayText, Boolean offerHA, Boolean useVirtualNetwork, String tags) {
    	boolean updateNeeded = (name != null || displayText != null || offerHA != null || useVirtualNetwork != null || tags != null);
    	if (!updateNeeded) {
    		return _serviceOfferingDao.findById(serviceOfferingId);
    	}
    	
        ServiceOfferingVO offering = _serviceOfferingDao.createForUpdate(serviceOfferingId);
        
        if (name != null) {
        	offering.setName(name);
        }
        
        if (displayText != null) {
        	offering.setDisplayText(displayText);
        }
        
	    if (offerHA != null) {
	    	offering.setOfferHA(offerHA);
        }
	    
        if (useVirtualNetwork != null) {
        	GuestIpType guestIpType = useVirtualNetwork ? GuestIpType.Virtualized : GuestIpType.DirectSingle;
            offering.setGuestIpType(guestIpType);
        }
        
        if (tags != null) {
        	if (tags.trim().isEmpty()) {
        		offering.setTagsArray(csvTagsToList(null));
        	} else {
        		offering.setTagsArray(csvTagsToList(tags));
        	}     	
        }
        
        if (_serviceOfferingDao.update(serviceOfferingId, offering)) {
        	offering = _serviceOfferingDao.findById(serviceOfferingId);
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_EDIT, "Successfully updated service offering with name: " + offering.getName() + ".", "soId=" + offering.getId(), "name=" + offering.getName(),
    				"displayText=" + offering.getDisplayText(), "offerHA=" + offering.getOfferHA(), "useVirtualNetwork=" + (offering.getGuestIpType() == GuestIpType.Virtualized), "tags=" + offering.getTags());
        	return offering;
        } else {
        	return null;
        }
    }
    
    public DiskOfferingVO updateDiskOffering(long userId, long diskOfferingId, String name, String displayText, String tags) {
    	boolean updateNeeded = (name != null || displayText != null || tags != null);
    	if (!updateNeeded) {
    		return _diskOfferingDao.findById(diskOfferingId);
    	}
    	
    	DiskOfferingVO diskOffering = _diskOfferingDao.createForUpdate(diskOfferingId);
    	
    	if (name != null) {
    		diskOffering.setName(name);
    	}
    	
    	if (displayText != null) {
    		diskOffering.setDisplayText(displayText);
    	}
    	
    	if (tags != null) {
        	if (tags.trim().isEmpty()) {
        		diskOffering.setTagsArray(csvTagsToList(null));
        	} else {
        		diskOffering.setTagsArray(csvTagsToList(tags));
        	}     	
        }
    	
    	if (_diskOfferingDao.update(diskOfferingId, diskOffering)) {
    		return _diskOfferingDao.findById(diskOfferingId);
    	} else { 
    		return null;
    	}
    }
    
    public boolean deleteServiceOffering(long userId, long serviceOfferingId) {
    	ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOfferingId);
    	
    	if (_serviceOfferingDao.remove(serviceOfferingId)) {
    		saveConfigurationEvent(userId, null, EventTypes.EVENT_SERVICE_OFFERING_EDIT, "Successfully deleted service offering with name: " + offering.getName(), "soId=" + serviceOfferingId, "name=" + offering.getName(),
    				"displayText=" + offering.getDisplayText(), "offerHA=" + offering.getOfferHA(), "useVirtualNetwork=" + (offering.getGuestIpType() == GuestIpType.Virtualized));
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, boolean mirrored, String tags) {
    	long diskSize = numGibibytes * 1024;
    	tags = cleanupTags(tags);
		DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, diskSize, mirrored, tags);
		return _diskOfferingDao.persist(newDiskOffering);
    }
    
    public String changePrivateIPRange(boolean add, long podId, String startIP, String endIP) throws InvalidParameterValueException {
    	checkPrivateIpRangeErrors(podId, startIP, endIP);
    	
		long zoneId = _podDao.findById(podId).getDataCenterId();
		List<String> problemIPs = null;
		if (add) {
			problemIPs = savePrivateIPRange(startIP, endIP, podId, zoneId);
		} else {
			problemIPs = deletePrivateIPRange(startIP, endIP, podId, zoneId);
		}
		
		if (problemIPs == null) {
			throw new InvalidParameterValueException ("Failed to change private IP range. Please contact Cloud Support.");
		} else {
			return genChangeRangeSuccessString(problemIPs, add);
		}
    }
    
    public VlanVO createVlanAndPublicIpRange(long userId, VlanType vlanType, Long zoneId, Long accountId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws InvalidParameterValueException, InternalErrorException {    		
    	
		//check for hypervisor type to be xenserver
		String hypervisorType = _configDao.getValue("hypervisor.type");
				
		if(hypervisorType.equalsIgnoreCase("xenserver"))
		{
	    	//check for the vlan being added before going to db, to see if it is untagged
	    	if(vlanType.toString().equalsIgnoreCase("VirtualNetwork") && vlanId.equalsIgnoreCase("untagged"))
	    	{
	    		if(_configDao.getValue("xen.public.network.device") == null || _configDao.getValue("xen.public.network.device").equals(""))
	    		{
	    			throw new InternalErrorException("For adding an untagged vlan, please set up xen.public.network.device");
	    		}
	    	}
	    	
		}
    	
    	DataCenterVO zone;
    	if (zoneId == null || ((zone = _zoneDao.findById(zoneId)) == null)) {
			throw new InvalidParameterValueException("Please specify a valid zone.");
		}    	    	
    	//remove this
    	if (vlanType.equals(VlanType.VirtualNetwork)) {
    		if (!(accountId == null && podId == null) && false) {
    			throw new InvalidParameterValueException("VLANs for the virtual network must be zone-wide.");
    		}
    	} 
    	else if (vlanType.equals(VlanType.DirectAttached)) 
    	{
			if (accountId!=null && podId!=null) 
			{
				throw new InvalidParameterValueException("Direct Attached VLANs must either be pod-wide,for one account or zone wide");
			}
    		if (podId != null) 
    		{
    			// Pod-wide VLANs must be untagged
        		if (!vlanId.equals(Vlan.UNTAGGED)) {
        			throw new InvalidParameterValueException("Direct Attached VLANs for a pod must be untagged.");
        		}
        		
        		// Check that the pod ID is valid
        		HostPodVO pod = null;
        		if (podId != null && ((pod = _podDao.findById(podId)) == null)) {
        			throw new InvalidParameterValueException("Please specify a valid pod.");
        		}
        		
        		// Make sure there aren't any account VLANs in this zone
        		List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAll();
        		for (AccountVlanMapVO accountVlanMap : accountVlanMaps) {
        			VlanVO vlan = _vlanDao.findById(accountVlanMap.getVlanDbId());
        			if (vlan.getDataCenterId() == zone.getId().longValue()) {
        				throw new InvalidParameterValueException("Zone " + zone.getName() + " already has account VLANs. A zone may contain either pod VLANs or account VLANs, but not both.");
        			}
        		}
        				
    		}
    		else 
    		{
    			// VLANs for an account must be tagged
        		if (vlanId.equals(Vlan.UNTAGGED)) {
        			throw new InvalidParameterValueException("Direct Attached VLANs for an account must be tagged.");
        		}
        		
        		if(accountId!=null)
        		{
	        		// Check that the account ID is valid
	        		AccountVO account;
	        		if ((account = _accountDao.findById(accountId)) == null) {
	        			throw new InvalidParameterValueException("Please specify a valid account.");
	        		}
	        		
	        		// Make sure there aren't any pod VLANs in this zone
	        		List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zone.getId());
	        		for (HostPodVO pod : podsInZone) {
	        			if (_podVlanMapDao.listPodVlanMapsByPod(pod.getId()).size() > 0) {
	        				throw new InvalidParameterValueException("Zone " + zone.getName() + " already has pod VLANs. A zone may contain either pod VLANs or account VLANs, but not both.");
	        			}
	        		}
	        		
	        		// Make sure the specified account isn't already assigned to a VLAN in this zone
	        		List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByAccount(accountId);
	        		for (AccountVlanMapVO accountVlanMap : accountVlanMaps) {
	        			VlanVO vlan = _vlanDao.findById(accountVlanMap.getVlanDbId());
	        			if (vlan.getDataCenterId() == zone.getId().longValue()) {
	        				throw new InvalidParameterValueException("The account " + account.getAccountName() + " is already assigned to the VLAN with ID " + vlan.getVlanId() + " in zone " + zone.getName() + ".");
	        			}
	        		}
        		}
    		} 
    	} 

    	// Make sure the gateway is valid
		if (!NetUtils.isValidIp(vlanGateway)) {
			throw new InvalidParameterValueException("Please specify a valid gateway");
		}
		
		// Make sure the netmask is valid
		if (!NetUtils.isValidIp(vlanNetmask)) {
			throw new InvalidParameterValueException("Please specify a valid netmask");
		}
		
		String newVlanSubnet = NetUtils.getSubNet(vlanGateway, vlanNetmask);
		    	    		
		// Check if the new VLAN's subnet conflicts with the guest network in the specified zone
		String guestNetworkCidr = zone.getGuestNetworkCidr();
    	String[] cidrPair = guestNetworkCidr.split("\\/");
		String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrPair[0],Long.parseLong(cidrPair[1]));
		long guestCidrSize = Long.parseLong(cidrPair[1]);
		long vlanCidrSize = NetUtils.getCidrSize(vlanNetmask);
		
		long cidrSizeToUse = -1;
		if (vlanCidrSize < guestCidrSize) {
			cidrSizeToUse = vlanCidrSize;
		} else {
			cidrSizeToUse = guestCidrSize;
		}
		
		String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
		
		if (newVlanSubnet.equals(guestSubnet)) {
			throw new InvalidParameterValueException("The new VLAN you have specified has the same subnet as the guest network in zone: " + zone.getName() + ". Please specify a different gateway/netmask.");
		}
		
		// Check if there are any errors with the IP range
		checkPublicIpRangeErrors(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);
		
		// Throw an exception if any of the following is true:
		// 1. Another VLAN in the same zone has a different tag but the same subnet as the new VLAN
		// 2. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has IPs that overlap with the IPs being added
		// 3. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has a different gateway than the new VLAN
		List<VlanVO> vlans = _vlanDao.findByZone(zone.getId());
		for (VlanVO vlan : vlans) {
			String otherVlanGateway = vlan.getVlanGateway();
			String otherVlanSubnet = NetUtils.getSubNet(vlan.getVlanGateway(), vlan.getVlanNetmask());
			String[] otherVlanIpRange = vlan.getIpRange().split("\\-");
			String otherVlanStartIP = otherVlanIpRange[0];
			String otherVlanEndIP = null;
			if (otherVlanIpRange.length > 1) {
				otherVlanEndIP = otherVlanIpRange[1];
			}
			
			if (!vlanId.equals(vlan.getVlanId()) && newVlanSubnet.equals(otherVlanSubnet)) {
				throw new InvalidParameterValueException("The VLAN with ID " + vlan.getVlanId() + " in zone " + zone.getName() + " has the same subnet. Please specify a different gateway/netmask.");
			}
			
			if (vlanId.equals(vlan.getVlanId()) && newVlanSubnet.equals(otherVlanSubnet)) {
				if (NetUtils.ipRangesOverlap(startIP, endIP, otherVlanStartIP, otherVlanEndIP)) {
					throw new InvalidParameterValueException("The VLAN with ID " + vlan.getVlanId() + " already has IPs that overlap with the new range. Please specify a different start IP/end IP.");
				}
				
				if (!vlanGateway.equals(otherVlanGateway)) {
					throw new InvalidParameterValueException("The VLAN with ID " + vlan.getVlanId() + " has already been added with gateway " + otherVlanGateway + ". Please specify a different VLAN ID.");
				}
			}
		}
		
		// Check if a guest VLAN is using the same tag
		if (_zoneDao.findVnet(zoneId, vlanId).size() > 0) {
			throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for the guest network in zone " + zone.getName());
		}
		
		// Everything was fine, so persist the VLAN
		String ipRange = startIP;
		if (endIP != null) {
			ipRange += "-" + endIP;
		}
		VlanVO vlan = new VlanVO(vlanType, vlanId, vlanGateway, vlanNetmask, zone.getId(), ipRange);
		vlan = _vlanDao.persist(vlan);
		
		// Persist the IP range
		if (accountId != null && vlanType.equals(VlanType.VirtualNetwork)){
			if(!savePublicIPRangeForAccount(startIP, endIP, zoneId, vlan.getId(), accountId, _accountDao.findById(accountId).getDomainId())){
				deletePublicIPRange(vlan.getId());
				_vlanDao.delete(vlan.getId());
				throw new InternalErrorException("Failed to save IP range. Please contact Cloud Support."); //It can be Direct IP or Public IP.
			}				
		}else if (!savePublicIPRange(startIP, endIP, zoneId, vlan.getId())) {
			deletePublicIPRange(vlan.getId());
			_vlanDao.delete(vlan.getId());
			throw new InternalErrorException("Failed to save IP range. Please contact Cloud Support."); //It can be Direct IP or Public IP.
		}
		
		if (accountId != null) {
			// This VLAN is account-specific, so create an AccountVlanMapVO entry
			AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(accountId, vlan.getId());
			_accountVlanMapDao.persist(accountVlanMapVO);
		} else if (podId != null) {
			// This VLAN is pod-wide, so create a PodVlanMapVO entry
			PodVlanMapVO podVlanMapVO = new PodVlanMapVO(podId, vlan.getId());
			_podVlanMapDao.persist(podVlanMapVO);
		}
		
		String eventMsg = "Successfully created new VLAN (tag = " + vlanId + ", gateway = " + vlanGateway + ", netmask = " + vlanNetmask + ", start IP = " + startIP;
		if (endIP != null) {
			eventMsg += ", end IP = " + endIP;
		}
		eventMsg += ".";
		saveConfigurationEvent(userId, accountId, EventTypes.EVENT_VLAN_IP_RANGE_CREATE, eventMsg, "vlanType=" + vlanType, "dcId=" + zoneId,
																												"accountId=" + accountId, "podId=" + podId,
																												"vlanId=" + vlanId, "vlanGateway=" + vlanGateway,
																												"vlanNetmask=" + vlanNetmask, "startIP=" + startIP,
																												"endIP=" + endIP);
		
		return vlan;
    }
    
    public boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) throws InvalidParameterValueException {
    	VlanVO vlan = _vlanDao.findById(vlanDbId);
    	if (vlan == null) {
    		throw new InvalidParameterValueException("Please specify a valid VLAN id.");
    	}
    	
    	// Check if the VLAN has any allocated public IPs
    	if (_publicIpAddressDao.countIPs(vlan.getDataCenterId(), vlanDbId, true) > 0) {
    		throw new InvalidParameterValueException("The VLAN can't be deleted because it has allocated public IP addresses.");
    	}
    	
    	// Check if the VLAN is being used by any domain router
    	if (_domrDao.listByVlanDbId(vlanDbId).size() > 0) {
    		throw new InvalidParameterValueException("The VLAN can't be deleted because it is being used by a domain router.");
    	}
    	
    	Long accountId = null;
		Long podId = null;
		List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
		List<PodVlanMapVO> podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(vlanDbId);
		
		if (accountVlanMaps.size() > 0) {
			accountId = accountVlanMaps.get(0).getAccountId();
		}
		
		if (podVlanMaps.size() > 0) {
			podId = podVlanMaps.get(0).getPodId();
		}

    	// Delete all public IPs in the VLAN
    	if (!deletePublicIPRange(vlanDbId)) {
    		return false;
    	}
    	
		// Delete the VLAN
		boolean success = _vlanDao.delete(vlanDbId);
		
		if (success) {
			String[] ipRange = vlan.getIpRange().split("\\-");
			String startIP = ipRange[0];
			String endIP = (ipRange.length > 1) ? ipRange[1] : null;
			String eventMsg = "Successfully deleted VLAN (tag = " + vlan.getVlanId() + ", gateway = " + vlan.getVlanGateway() + ", netmask = " + vlan.getVlanNetmask() + ", start IP = " + startIP;
			if (endIP != null) {
				eventMsg += ", end IP = " + endIP;
			}
			eventMsg += ".";
			saveConfigurationEvent(userId, null, EventTypes.EVENT_VLAN_IP_RANGE_DELETE, eventMsg, "vlanType=" + vlan.getVlanType(), "dcId=" + vlan.getDataCenterId(),
																												"accountId=" + accountId, "podId=" + podId,
																												"vlanId=" + vlan.getVlanId(), "vlanGateway=" + vlan.getVlanGateway(),
																												"vlanNetmask=" + vlan.getVlanNetmask(), "startIP=" + startIP,
																												"endIP=" + endIP);
		}
		
		return success;
    }
    
    public List<String> csvTagsToList(String tags) {
    	List<String> tagsList = new ArrayList<String>();
    	
    	if (tags != null) {
            String[] tokens = tags.split(",");
            for (int i = 0; i < tokens.length; i++) {
                tagsList.add(tokens[i].trim());
            }
        }
    	
    	return tagsList;
    }
    
    public String listToCsvTags(List<String> tagsList) {
    	String tags = "";
    	if (tagsList.size() > 0) {
    		for (int i = 0; i < tagsList.size(); i++) {
    			tags += tagsList.get(i);
    			if (i != tagsList.size() - 1) {
    				tags += ",";
    			}
    		}
    	} 
    	
    	return tags;
    }
    
    private String cleanupTags(String tags) {
    	if (tags != null) {
            String[] tokens = tags.split(",");
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                t.append(tokens[i].trim()).append(",");
            }
            t.delete(t.length() - 1, t.length());
            tags = t.toString();
        }
    	
    	return tags;
    }
    	
	private boolean isPrivateIPAllocated(String ip, long podId, long zoneId, PreparedStatement stmt) {
		try {
			stmt.clearParameters();
        	stmt.setString(1, ip);
        	stmt.setLong(2, zoneId);
        	stmt.setLong(3, podId);
        	ResultSet rs = stmt.executeQuery();
        	if (rs.next()) return (rs.getString("taken") != null);
        	else return false;
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
            return true;
        }
	}
	
	@DB
	protected boolean deletePublicIPRange(long vlanDbId) {
    	Transaction txn = Transaction.currentTxn();
		String deleteSql = "DELETE FROM `cloud`.`user_ip_address` WHERE vlan_db_id = ?";

		txn.start();
		try {
			PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteSql);
			stmt.setLong(1, vlanDbId);
			stmt.executeUpdate();
		} catch (Exception ex) {
			return false;
		}
		txn.commit();
			
        return true;
	}
	
	@DB
	protected List<String> deletePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
		long startIPLong = NetUtils.ip2Long(startIP);
		long endIPLong = NetUtils.ip2Long(endIP);
		Transaction txn = Transaction.currentTxn();
		String deleteSql = "DELETE FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND pod_id = ? AND data_center_id = ?";
		String isPrivateIPAllocatedSelectSql = "SELECT * FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND data_center_id = ? AND pod_id = ?";
		
		List<String> problemIPs = new ArrayList<String>();
		PreparedStatement deleteIPStmt = null;
		PreparedStatement isAllocatedStmt = null;
				
		txn.start();
		try {
			deleteIPStmt = txn.prepareAutoCloseStatement(deleteSql);
			isAllocatedStmt = txn.prepareAutoCloseStatement(isPrivateIPAllocatedSelectSql);
		} catch (SQLException e) {
			return null;
		}
		
		while (startIPLong <= endIPLong) {
			if (!isPrivateIPAllocated(NetUtils.long2Ip(startIPLong), podId, zoneId, isAllocatedStmt)) {
				try {
					deleteIPStmt.clearParameters();
					deleteIPStmt.setString(1, NetUtils.long2Ip(startIPLong));
					deleteIPStmt.setLong(2, podId);
					deleteIPStmt.setLong(3, zoneId);
					deleteIPStmt.executeUpdate();
				} catch (Exception ex) {
				}
			} else {
				problemIPs.add(NetUtils.long2Ip(startIPLong));
			}
        	startIPLong += 1;
		}
		txn.commit();

        return problemIPs;
	}
    
	@DB
    protected boolean savePublicIPRange(String startIP, String endIP, long zoneId, long vlanDbId) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = NetUtils.ip2Long(endIP);
    	Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id) VALUES (?, ?, ?)";
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, vlanDbId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		s_logger.debug("Exception saving public IP range: " + ex);
        		return false;
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return true;
	}
	
	@DB
    protected boolean savePublicIPRangeForAccount(String startIP, String endIP, long zoneId, long vlanDbId, long accountId, long domainId) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = NetUtils.ip2Long(endIP);
    	Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id, account_id, domain_id, allocated) VALUES (?, ?, ?, ?, ?, ?)";
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, vlanDbId);
        		stmt.setLong(4, accountId);
        		stmt.setLong(5, domainId);
        		stmt.setDate(6,  new java.sql.Date(new java.util.Date().getTime()));
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		s_logger.debug("Exception saving public IP range: " + ex);
        		return false;
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return true;
	}
	
	@DB
	protected List<String> savePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
		long startIPLong = NetUtils.ip2Long(startIP);
		long endIPLong = NetUtils.ip2Long(endIP);
		Transaction txn = Transaction.currentTxn();
		String insertSql = "INSERT INTO `cloud`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
		List<String> problemIPs = new ArrayList<String>();
		
		txn.start();
		PreparedStatement stmt = null;
        while (startIPLong <= endIPLong) {
        	try {
        		stmt = txn.prepareAutoCloseStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIPLong));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, podId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		 problemIPs.add(NetUtils.long2Ip(startIPLong));
        	}
        	startIPLong += 1;
        }
        txn.commit();
        
        return problemIPs;
	}
    
	private String genChangeRangeSuccessString(List<String> problemIPs, boolean add) {
		if (problemIPs == null) return "";
		
		if (problemIPs.size() == 0) {
			if (add) return "Successfully added all IPs in the specified range.";
			else return "Successfully deleted all IPs in the specified range.";
		} else {
			String successString = "";
			if (add) successString += "Failed to add the following IPs, because they are already in the database: ";
			else  successString += "Failed to delete the following IPs, because they are in use: ";
			
			for (int i = 0; i < problemIPs.size(); i++) {
				successString += problemIPs.get(i);
				if (i != (problemIPs.size() - 1)) successString += ", ";
			}
			
			successString += ". ";
			
			if (add) successString += "Successfully added all other IPs in the specified range.";
			else successString += "Successfully deleted all other IPs in the specified range.";
			
			return successString;
		}
	}
	
	private void checkPublicIpRangeErrors(long zoneId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws InvalidParameterValueException {
		// Check that the start and end IPs are valid
		if (!NetUtils.isValidIp(startIP)) {
			throw new InvalidParameterValueException("Please specify a valid start IP");
		}
		
		if (endIP != null && !NetUtils.isValidIp(endIP)) {
			throw new InvalidParameterValueException("Please specify a valid end IP");
		}
		
		if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
			throw new InvalidParameterValueException("Please specify a valid IP range.");
		}
		
		// Check that the IPs that are being added are compatible with the VLAN's gateway and netmask
		if (vlanNetmask == null) {
			throw new InvalidParameterValueException("Please ensure that your VLAN's netmask is specified");
		}
		
		if (endIP != null && !NetUtils.sameSubnet(startIP, endIP, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the VLAN's netmask.");
		}
		
		if (!NetUtils.sameSubnet(startIP, vlanGateway, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as your VLAN's gateway, as per the VLAN's netmask.");
		}
		
		if (endIP != null && !NetUtils.sameSubnet(endIP, vlanGateway, vlanNetmask)) {
			throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as your VLAN's gateway, as per the VLAN's netmask.");
		}
	}
	
	private void checkPrivateIpRangeErrors(Long podId, String startIP, String endIP) throws InvalidParameterValueException {
		HostPodVO pod = _podDao.findById(podId);
		if (pod == null) {
			throw new InvalidParameterValueException("Please specify a valid pod.");
		}
		
		// Check that the start and end IPs are valid
		if (!NetUtils.isValidIp(startIP)) {
			throw new InvalidParameterValueException("Please specify a valid start IP");
		}
		
		if (endIP != null && !NetUtils.isValidIp(endIP)) {
			throw new InvalidParameterValueException("Please specify a valid end IP");
		}
		
		if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
			throw new InvalidParameterValueException("Please specify a valid IP range.");
		}
		
		// Check that the IPs that are being added are compatible with the pod's CIDR
		String cidrAddress = getCidrAddress(podId);
		long cidrSize = getCidrSize(podId);

		if (endIP != null && !NetUtils.sameSubnetCIDR(startIP, endIP, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.");
		}
		
		if (!NetUtils.sameSubnetCIDR(startIP, cidrAddress, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as the pod's CIDR address.");
		}
		
		if (endIP != null && !NetUtils.sameSubnetCIDR(endIP, cidrAddress, cidrSize)) {
			throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as the pod's CIDR address.");
		}
	}
    
	private String getCidrAddress(String cidr) {
		String[] cidrPair = cidr.split("\\/");
		return cidrPair[0];
	}
	
	private long getCidrSize(String cidr) {
		String[] cidrPair = cidr.split("\\/");
		return Long.parseLong(cidrPair[1]);
	}
	
	private String getCidrAddress(long podId) {
		HostPodVO pod = _podDao.findById(podId);
		return pod.getCidrAddress();
	}
	
	private long getCidrSize(long podId) {
		HostPodVO pod = _podDao.findById(podId);
		return pod.getCidrSize();
	}
	
	private void checkPodCidrSubnets(long dcId, HashMap<Long, List<Object>> currentPodCidrSubnets) throws InvalidParameterValueException {
		// For each pod, return an error if any of the following is true:
		// 1. The pod's CIDR subnet conflicts with the guest network subnet
		// 2. The pod's CIDR subnet conflicts with the CIDR subnet of any other pod
		DataCenterVO dcVo = _zoneDao.findById(dcId);
		String guestNetworkCidr = dcVo.getGuestNetworkCidr();
		String[] cidrTuple = guestNetworkCidr.split("\\/");
		
		String zoneName = getZoneName(dcId);
		String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0],Long.parseLong(cidrTuple[1]));
		long guestCidrSize = Long.parseLong(cidrTuple[1]);
		
		// Iterate through all pods in this zone
		for (Long podId : currentPodCidrSubnets.keySet()) {
			String podName;
			if (podId.longValue() == -1) podName = "newPod";
			else podName = getPodName(podId.longValue());
			
			List<Object> cidrPair = currentPodCidrSubnets.get(podId);
			String cidrAddress = (String) cidrPair.get(0);
			long cidrSize = ((Long) cidrPair.get(1)).longValue();
			
			long cidrSizeToUse = -1;
			if (cidrSize < guestCidrSize) cidrSizeToUse = cidrSize;
			else cidrSizeToUse = guestCidrSize;
			
			String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
			String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
			
			// Check that cidrSubnet does not equal guestSubnet
			if (cidrSubnet.equals(guestSubnet)) {
				if (podName.equals("newPod")) {
					throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.");
				} else {
					throw new InvalidParameterValueException("Warning: The subnet of pod " + podName + " in zone " + zoneName + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.");
				}
			}
			
			// Iterate through the rest of the pods
			for (Long otherPodId : currentPodCidrSubnets.keySet()) {
				if (podId.equals(otherPodId)) continue;
				
				// Check that cidrSubnet does not equal otherCidrSubnet
				List<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
				String otherCidrAddress = (String) otherCidrPair.get(0);
				long otherCidrSize = ((Long) otherCidrPair.get(1)).longValue();
				
				if (cidrSize < otherCidrSize) cidrSizeToUse = cidrSize;
				else cidrSizeToUse = otherCidrSize;
				
				cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
				String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);
				
				if (cidrSubnet.equals(otherCidrSubnet)) {
					String otherPodName = getPodName(otherPodId.longValue());
					if (podName.equals("newPod")) {
						throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName + ". Please specify a different CIDR.");
					} else {
						throw new InvalidParameterValueException("Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName + " have conflicting CIDR subnets. Please change the CIDR of one of these pods.");
					}
				}
			}
		}
		
	}
	
	private boolean validPod(long podId) {
		return (_podDao.findById(podId) != null);
	}
    
    private boolean validPod(String podName, long zoneId) {
    	if (!validZone(zoneId)) {
    		return false;
    	}
    	
		return (_podDao.findByName(podName, zoneId) != null);
	}
    
    private String getPodName(long podId) {
    	return _podDao.findById(new Long(podId)).getName();
    }
    
    private boolean validZone(String zoneName) {
    	return(_zoneDao.findByName(zoneName) != null);
    }
    
    private boolean validZone(long zoneId) {
    	return (_zoneDao.findById(zoneId) != null);
    }
    
    private String getZoneName(long zoneId) {
    	DataCenterVO zone = _zoneDao.findById(new Long(zoneId));
    	if (zone != null)
    		return zone.getName();
    	else
    		return null;
    }
    
    private Long saveConfigurationEvent(long userId, Long accountId, String type, String description, String... paramsList) {
    	UserVO user = _userDao.findById(userId);
    	long accountIdToUse = (accountId != null) ? accountId : user.getAccountId();
    	
    	String eventParams = "";
    	String logParams = "";
    	for (int i = 0; i < paramsList.length; i++) {
    		String param = paramsList[i];
    		boolean lastParam = (i == (paramsList.length - 1));
    		
    		logParams += param;
    		if (!lastParam) {
    			logParams += ", ";
    		}
    		
    		String val = param.split("\\=")[1];
    		if (val.equals("null")) {
    			continue;
    		}
    		
    		eventParams += param;
    		if (!lastParam) {
    			eventParams += "\n";
    		}
    	}
    	
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountIdToUse);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(EventVO.LEVEL_INFO);
        event.setParameters(eventParams);
        event = _eventDao.persist(event);
        
        s_logger.debug("User " + user.getUsername() + " performed configuration action: " + type + ", " + description + " | params: " + logParams);
        
        return event.getId();
    }
    
    private String[] getLinkLocalIPRange() throws InvalidParameterValueException {
    	String ipNums = _configDao.getValue("linkLocalIp.nums");
    	int nums = Integer.parseInt(ipNums);
    	if (nums > 16 || nums <= 0) {
    		throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
    	}
    	/*local link ip address starts from 169.254.0.2 - 169.254.(nums)*/
    	String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
    	if (ipRanges == null)
    		throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
    	return ipRanges;
    }           

}
