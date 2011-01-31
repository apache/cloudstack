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

package com.cloud.server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.guru.ControlNetworkGuru;
import com.cloud.network.guru.DirectPodBasedNetworkGuru;
import com.cloud.network.guru.PodBasedNetworkGuru;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public class ConfigurationServerImpl implements ConfigurationServer {
	public static final Logger s_logger = Logger.getLogger(ConfigurationServerImpl.class.getName());
	
	private final ConfigurationDao _configDao;
	private final SnapshotPolicyDao _snapPolicyDao;
	private final DataCenterDao _zoneDao;
    private final HostPodDao _podDao;
    private final DiskOfferingDao _diskOfferingDao;
    private final ServiceOfferingDao _serviceOfferingDao;
    private final DomainDao _domainDao;
    private final NetworkOfferingDao _networkOfferingDao;
    private final DataCenterDao _dataCenterDao;
    private final NetworkDao _networkDao;
    private final VlanDao _vlanDao;

	
	public ConfigurationServerImpl() {
		ComponentLocator locator = ComponentLocator.getLocator(Name);
		_configDao = locator.getDao(ConfigurationDao.class);
		_snapPolicyDao = locator.getDao(SnapshotPolicyDao.class);
        _zoneDao = locator.getDao(DataCenterDao.class);
        _podDao = locator.getDao(HostPodDao.class);
        _diskOfferingDao = locator.getDao(DiskOfferingDao.class);
        _serviceOfferingDao = locator.getDao(ServiceOfferingDao.class);
        _networkOfferingDao = locator.getDao(NetworkOfferingDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _dataCenterDao = locator.getDao(DataCenterDao.class);
        _networkDao = locator.getDao(NetworkDao.class);
        _vlanDao = locator.getDao(VlanDao.class);
	}

	@Override
    public void persistDefaultValues() throws InvalidParameterValueException, InternalErrorException {
		
		// Create system user and admin user
		saveUser();
		
		// Get init
		String init = _configDao.getValue("init");
		
		if (init.equals("false")) {
			s_logger.debug("ConfigurationServer is saving default values to the database.");
			
			// Save default Configuration Table values
			List<String> categories = Config.getCategories();
			for (String category : categories) {
				// If this is not a premium environment, don't insert premium configuration values
				if (!_configDao.isPremium() && category.equals("Premium")) {
					continue;
				}
				
				List<Config> configs = Config.getConfigs(category);
				for (Config c : configs) {
					String name = c.key();
					
					// If the value is already in the table, don't reinsert it
					if (_configDao.getValue(name) != null) {
						continue;
					}
					
					String instance = "DEFAULT";
					String component = c.getComponent();
					String value = c.getDefaultValue();
					String description = c.getDescription();
					ConfigurationVO configVO = new ConfigurationVO(category, instance, component, name, value, description);
					_configDao.persist(configVO);
				}
			}
			
			// If this is a premium environment, set the network type to be "vlan"
			if (_configDao.isPremium()) {
//				_configDao.update("network.type", "vlan");
//				s_logger.debug("ConfigurationServer changed the network type to \"vlan\".");
				
				// Default value is set as KVM because of FOSS build, when we are
				// running under premium, autoset to XenServer if we know it is from FOSS settings
				String value = _configDao.getValue(Config.HypervisorDefaultType.key());
				if (value == null || value.equalsIgnoreCase(HypervisorType.KVM.toString())) {
					_configDao.update("hypervisor.type", "xenserver");
					s_logger.debug("ConfigurationServer changed the hypervisor type to \"xenserver\".");
				}
				
				_configDao.update("secondary.storage.vm", "true");
				s_logger.debug("ConfigurationServer made secondary storage vm required.");
				
				_configDao.update("secstorage.encrypt.copy", "true");
				s_logger.debug("ConfigurationServer made secondary storage copy encrypted.");
				
				_configDao.update("secstorage.secure.copy.cert", "realhostip");
				s_logger.debug("ConfigurationServer made secondary storage copy use realhostip.");					          	         
			} else {
				/*FOSS release, make external DHCP mode as default*/
				_configDao.update("direct.attach.network.externalIpAllocator.enabled", "true");
			}
			
			// Save Direct Networking service offerings
			createServiceOffering(User.UID_SYSTEM, "Small Instance", 1, 512, 500, "Small Instance, $0.05 per hour", false, false, false, null);			
			createServiceOffering(User.UID_SYSTEM, "Medium Instance", 1, 1024, 1000, "Medium Instance, $0.10 per hour", false, false, false, null);
			 // Save Virtual Networking service offerings
			//createServiceOffering(User.UID_SYSTEM, "Small Instance", 1, 512, 500, "Small Instance, Virtual Networking, $0.05 per hour", false, false, true, null);
			//createServiceOffering(User.UID_SYSTEM, "Medium Instance", 1, 1024, 1000, "Medium Instance, Virtual Networking, $0.10 per hour", false, false, true, null);
			// Save default disk offerings
			createDiskOffering(DomainVO.ROOT_DOMAIN, "Small", "Small Disk, 5 GB", 5, null);
			createDiskOffering(DomainVO.ROOT_DOMAIN, "Medium", "Medium Disk, 20 GB", 20, null);
			createDiskOffering(DomainVO.ROOT_DOMAIN, "Large", "Large Disk, 100 GB", 100, null);
			//_configMgr.createDiskOffering(User.UID_SYSTEM, DomainVO.ROOT_DOMAIN, "Private", "Private Disk", 0, null);
			
			   //Add default manual snapshot policy
            SnapshotPolicyVO snapPolicy = new SnapshotPolicyVO(0L, "00", "GMT", (short)4, 0);
            _snapPolicyDao.persist(snapPolicy);
            
			// Save the mount parent to the configuration table
			String mountParent = getMountParent();
			if (mountParent != null) {
			    _configDao.update("mount.parent", mountParent);
//				_configMgr.updateConfiguration(User.UID_SYSTEM, "mount.parent", mountParent);
				s_logger.debug("ConfigurationServer saved \"" + mountParent + "\" as mount.parent.");
			} else {
				s_logger.debug("ConfigurationServer could not detect mount.parent.");
			}

			String hostIpAdr = getHost();
			if (hostIpAdr != null) {
			    _configDao.update("host", hostIpAdr);
//				_configMgr.updateConfiguration(User.UID_SYSTEM, "host", hostIpAdr);
				s_logger.debug("ConfigurationServer saved \"" + hostIpAdr + "\" as host.");
			}

	        // generate a single sign-on key
	        updateSSOKey();
	        
	        //Create default network offerings
	        createDefaultNetworkOfferings();
	        
	        //Create default networks
	        createDefaultNetworks();
	        
	        //Update existing vlans with networkId
	        List<VlanVO> vlans = _vlanDao.listAll();
	        if (vlans != null && !vlans.isEmpty()) {
	            for (VlanVO vlan : vlans) {
	                if (vlan.getNetworkId().longValue() == 0) {
	                    updateVlanWithNetworkId(vlan);
	                }
	            }
	        }
		}

		// store the public and private keys in the database
		updateKeyPairs();

		// generate a random password used to authenticate zone-to-zone copy
		generateSecStorageVmCopyPassword();

		// Update the cloud identifier
		updateCloudIdentifier();
		
		// Set init to true
		_configDao.update("init", "true");
	}



	private String getEthDevice() {
		String defaultRoute = Script.runSimpleBashScript("/sbin/route | grep default");
		
		if (defaultRoute == null) {
			return null;
		}
		
		String[] defaultRouteList = defaultRoute.split("\\s+");
		
		if (defaultRouteList.length != 8) {
			return null;
		}
		
		return defaultRouteList[7];
	}
	
	private String getMountParent() {
		return getEnvironmentProperty("mount.parent");
	}
	
	private String getEnvironmentProperty(String name) {
		try {
			final File propsFile = PropertiesUtil.findConfigFile("environment.properties");
			
			if (propsFile == null) {
				return null;
			} else {
				final FileInputStream finputstream = new FileInputStream(propsFile);
				final Properties props = new Properties();
				props.load(finputstream);
				finputstream.close();
				return props.getProperty("mount.parent");
			}
		} catch (IOException e) {
			return null;
		}
	}
	
	
	
	@DB
	protected String getHost() {
		NetworkInterface nic = null;
		String pubNic = getEthDevice();
		
		if (pubNic == null) {
			return null;
		}
		
		try {
			nic = NetworkInterface.getByName(pubNic);
		} catch (final SocketException e) {
			return null;
		}
		
		String[] info = NetUtils.getNetworkParams(nic);
		return info[0];
	}
	
	@DB
	protected void saveUser() {
        // insert system account
        String insertSql = "INSERT INTO `cloud`.`account` (id, account_name, type, domain_id) VALUES (1, 'system', '1', '1')";
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }
        // insert system user
        insertSql = "INSERT INTO `cloud`.`user` (id, username, password, account_id, firstname, lastname, created) VALUES (1, 'system', '', 1, 'system', 'cloud', now())";
	    txn = Transaction.currentTxn();
		try {
		    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
		    stmt.executeUpdate();
		} catch (SQLException ex) {
		}
		
    	// insert admin user
        long id = 2;
        String username = "admin";
        String firstname = "admin";
        String lastname = "cloud";
        String password = "password";
        String email = "";
        
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return;
        }
        
        md5.reset();
        BigInteger pwInt = new BigInteger(1, md5.digest(password.getBytes()));
        String pwStr = pwInt.toString(16);
        int padding = 32 - pwStr.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < padding; i++) {
            sb.append('0'); // make sure the MD5 password is 32 digits long
        }
        sb.append(pwStr);

        // create an account for the admin user first
        insertSql = "INSERT INTO `cloud`.`account` (id, account_name, type, domain_id) VALUES (" + id + ", '" + username + "', '1', '1')";
        txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }

        // now insert the user
        insertSql = "INSERT INTO `cloud`.`user` (id, username, password, account_id, firstname, lastname, email, created) " +
                "VALUES (" + id + ",'" + username + "','" + sb.toString() + "', 2, '" + firstname + "','" + lastname + "','" + email + "',now())";

        txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }
    }

	protected void updateCloudIdentifier() {
		// Creates and saves a UUID as the cloud identifier
		String currentCloudIdentifier = _configDao.getValue("cloud.identifier");
		if (currentCloudIdentifier == null || currentCloudIdentifier.isEmpty()) {
			String uuid = UUID.randomUUID().toString();
			_configDao.update("cloud.identifier", uuid);
		}
	}

    @DB
    protected void updateKeyPairs() {
        // Grab the SSH key pair and insert it into the database, if it is not present

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Processing updateKeyPairs");
        }
        String already = _configDao.getValue("ssh.privatekey");
        String homeDir = Script.runSimpleBashScript("echo ~");
        String userid = System.getProperty("user.name");
        if (homeDir == "~") {
            s_logger.error("No home directory was detected.  Set the HOME environment variable to point to your user profile or home directory.");
            throw new CloudRuntimeException("No home directory was detected.  Set the HOME environment variable to point to your user profile or home directory.");
        }
        File privkeyfile = new File(homeDir + "/.ssh/id_rsa");
        File pubkeyfile  = new File(homeDir + "/.ssh/id_rsa.pub");

        if (already == null || already.isEmpty()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Systemvm keypairs not found in database. Need to store them in the database");
            }
            Script.runSimpleBashScript("if [ -f ~/.ssh/id_rsa ] ; then true ; else yes '' | ssh-keygen -t rsa -q ; fi");

            byte[] arr1 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(privkeyfile)).readFully(arr1);
            } catch (EOFException e) {
            } catch (Exception e) {
                s_logger.error("Cannot read the private key file",e);
                throw new CloudRuntimeException("Cannot read the private key file");
            }
            String privateKey = new String(arr1).trim();
            byte[] arr2 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(pubkeyfile)).readFully(arr2);
            } catch (EOFException e) {			    
            } catch (Exception e) {
                s_logger.warn("Cannot read the public key file",e);
                throw new CloudRuntimeException("Cannot read the public key file");
            }
            String publicKey  = new String(arr2).trim();

            String insertSql1 = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                                "VALUES ('Hidden','DEFAULT', 'management-server','ssh.privatekey', '"+privateKey+"','Private key for the entire CloudStack')";
            String insertSql2 = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                                "VALUES ('Hidden','DEFAULT', 'management-server','ssh.publickey', '"+publicKey+"','Public key for the entire CloudStack')";

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
                stmt1.executeUpdate();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Private key inserted into database");
                }
            } catch (SQLException ex) {
                s_logger.error("SQL of the private key failed",ex);
                throw new CloudRuntimeException("SQL of the private key failed");
            }

            try {
                PreparedStatement stmt2 = txn.prepareAutoCloseStatement(insertSql2);
                stmt2.executeUpdate();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Public key inserted into database");
                }
            } catch (SQLException ex) {
                s_logger.error("SQL of the public key failed",ex);
                throw new CloudRuntimeException("SQL of the public key failed");
            }
        
        } else {
            s_logger.info("Keypairs already in database");
            if (userid.startsWith("cloud")) {
                s_logger.info("Keypairs already in database, updating local copy");
                updateKeyPairsOnDisk(homeDir);
            } else {
                s_logger.info("Keypairs already in database, skip updating local copy (not running as cloud user)");
            }
        }
    
        
        if (userid.startsWith("cloud")){
            s_logger.info("Going to update systemvm iso with generated keypairs if needed");
            injectSshKeysIntoSystemVmIsoPatch(pubkeyfile.getAbsolutePath(), privkeyfile.getAbsolutePath());
        } else {
            s_logger.info("Skip updating keypairs on systemvm iso (not running as cloud user)");
        }
    }
    
    private void writeKeyToDisk(String key, String keyPath) {

        File keyfile = new File( keyPath);
        if (!keyfile.exists()) {
            try {
                keyfile.createNewFile();
            } catch (IOException e) {
                s_logger.warn("Failed to create file: " + e.toString());
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot create  key file " + keyPath);
            }
        }
        
        if (keyfile.exists()) {
            try {
                FileOutputStream kStream = new FileOutputStream(keyfile);
                kStream.write(key.getBytes());
                kStream.close();
            } catch (FileNotFoundException e) {
                s_logger.warn("Failed to write  key to " + keyfile.getAbsolutePath());
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot find  key file " + keyPath);
            } catch (IOException e) {
                s_logger.warn("Failed to write  key to " + keyfile.getAbsolutePath());
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot write to  key file " + keyPath);
            }
        }

    }
    
    private void updateKeyPairsOnDisk(String homeDir ) {
        
        String pubKey = _configDao.getValue("ssh.publickey");
        String prvKey = _configDao.getValue("ssh.privatekey");
        writeKeyToDisk(prvKey, homeDir + "/.ssh/id_rsa");
        writeKeyToDisk(pubKey, homeDir + "/.ssh/id_rsa.pub");
    }

    protected void injectSshKeysIntoSystemVmIsoPatch(String publicKeyPath, String privKeyPath) {
        String injectScript = "scripts/vm/systemvm/injectkeys.sh";    
        String scriptPath = Script.findScript("" , injectScript);
        String systemVmIsoPath = Script.findScript("", "vms/systemvm.iso");
        if ( scriptPath == null ) {
            throw new CloudRuntimeException("Unable to find key inject script " + injectScript);
        }
        if (systemVmIsoPath == null) {
            throw new CloudRuntimeException("Unable to find systemvm iso vms/systemvm.iso");
        }
        final Script command  = new Script(scriptPath,  s_logger);
        command.add(publicKeyPath);
        command.add(privKeyPath);
        command.add(systemVmIsoPath);
       
        final String result = command.execute();
        if (result != null) {
            s_logger.warn("Failed to inject generated public key into systemvm iso " + result);
            throw new CloudRuntimeException("Failed to inject generated public key into systemvm iso " + result);
        }
    }

	@DB
	protected void generateSecStorageVmCopyPassword() {
		String already = _configDao.getValue("secstorage.copy.password");
		
		if (already == null) {
		
			s_logger.info("Need to store secondary storage vm copy password in the database");
			String password = PasswordGenerator.generateRandomPassword(12);

			String insertSql1 = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
			"VALUES ('Hidden','DEFAULT', 'management-server','secstorage.copy.password', '" + password + "','Password used to authenticate zone-to-zone template copy requests')";

			Transaction txn = Transaction.currentTxn();
			try {
				PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
				stmt1.executeUpdate();
				s_logger.debug("secondary storage vm copy password inserted into database");
			} catch (SQLException ex) {
				s_logger.warn("Failed to insert secondary storage vm copy password",ex);
			}
	    
		}
	}

	private void updateSSOKey() {
        try {
            String encodedKey = null;

            // Algorithm for SSO Keys is SHA1, should this be configuable?
            KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
            SecretKey key = generator.generateKey();
            encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());

            _configDao.update("security.singlesignon.key", encodedKey);
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating sso key", ex);
        }
	}

    private DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String vnetRange, String guestCidr, String domain, Long domainId, NetworkType zoneType) throws InvalidParameterValueException, InternalErrorException {
        int vnetStart = 0;
        int vnetEnd = 0;
        if (vnetRange != null) {
            String[] tokens = vnetRange.split("-");
            try {
                vnetStart = Integer.parseInt(tokens[0]);
                if (tokens.length == 1) {
                    vnetEnd = vnetStart;
                } else {
                    vnetEnd = Integer.parseInt(tokens[1]);
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("Please specify valid integers for the vlan range.");
            }
        } 
        
        //checking the following params outside checkzoneparams method as we do not use these params for updatezone
        //hence the method below is generic to check for common params
        if ((guestCidr != null) && !NetUtils.isValidCIDR(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        if(domainId!=null){
        	DomainVO domainVo = _domainDao.findById(domainId);
        	
        	if(domainVo == null)
        		throw new InvalidParameterValueException("Please specify a valid domain id");
        }
        // Create the new zone in the database
        DataCenterVO zone = new DataCenterVO(zoneName, null, dns1, dns2, internalDns1, internalDns2, vnetRange, guestCidr, domain, domainId, zoneType);
        zone = _zoneDao.persist(zone);

        // Add vnet entries for the new zone
        if (vnetRange != null){
            _zoneDao.addVnet(zone.getId(), vnetStart, vnetEnd);
        }

        return zone;
    }

    @DB
    protected HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException {
        String[] cidrPair = cidr.split("\\/");
        String cidrAddress = cidrPair[0];
        int cidrSize = Integer.parseInt(cidrPair[1]);
        
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

            String ipNums = _configDao.getValue("linkLocalIp.nums");
            int nums = Integer.parseInt(ipNums);
            if (nums > 16 || nums <= 0) {
                throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
            }
            /*local link ip address starts from 169.254.0.2 - 169.254.(nums)*/
            String[] linkLocalIpRanges = NetUtils.getLinkLocalIPRange(nums);
            if (linkLocalIpRanges == null) {
                throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
            } else {
                _zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
            }

            txn.commit();

        } catch(Exception e) {
            txn.rollback();
            s_logger.error("Unable to create new pod due to " + e.getMessage(), e);
            throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
        }
        
        return pod;
    }

    private DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, String tags) throws InvalidParameterValueException {
        long diskSize = numGibibytes * 1024;
        tags = cleanupTags(tags);

        DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, diskSize,tags,false);
        return _diskOfferingDao.persist(newDiskOffering);
    }

    private ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags) {
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        int networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        int multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
        Network.GuestIpType guestIpType = useVirtualNetwork ? Network.GuestIpType.Virtual : Network.GuestIpType.Direct;        
        tags = cleanupTags(tags);
        ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, multicastRate, offerHA, displayText, guestIpType, localStorageRequired, false, tags, false);
        
        if ((offering = _serviceOfferingDao.persist(offering)) != null) {
            return offering;
        } else {
            return null;
        }
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
    
    private void createDefaultNetworkOfferings() {

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemPublicNetwork, TrafficType.Public);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemManagementNetwork, TrafficType.Management);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemControlNetwork, TrafficType.Control);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemStorageNetwork, TrafficType.Storage);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        NetworkOfferingVO guestNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemGuestNetwork, TrafficType.Guest);
        guestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(guestNetworkOffering);
        
        NetworkOfferingVO defaultGuestNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultVirtualizedNetworkOffering, "Virtual Vlan", TrafficType.Guest, false, false, null, null, null, true, Availability.Required, false, false, false, false, false, false, false);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestNetworkOffering);
        NetworkOfferingVO defaultGuestDirectNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultDirectNetworkOffering, "Direct", TrafficType.Public, false, false, null, null, null, true, Availability.Required, false, false, false, false, false, false, false);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestDirectNetworkOffering);
    }
    
    private Integer getIntegerConfigValue(String configKey, Integer dflt) {
        String value = _configDao.getValue(configKey);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return dflt;
    }
    
    private void createDefaultNetworks() {
        List<DataCenterVO> zones = _dataCenterDao.listAll();
        long id = 1;
        
        HashMap<TrafficType, String> guruNames = new HashMap<TrafficType, String>();
        guruNames.put(TrafficType.Public, PublicNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Management, PodBasedNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Control, ControlNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Storage, PodBasedNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Guest, DirectPodBasedNetworkGuru.class.getSimpleName());
        
        for (DataCenterVO zone : zones) {
            long zoneId = zone.getId();
            long accountId = 1L;
            Long domainId = zone.getDomainId();
            
            if (domainId == null) {
                domainId = 1L;
            }
            //Create default networks - system only
            List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();
            
            for (NetworkOfferingVO offering : ntwkOff) {
                if (offering.isSystemOnly()) {
                    long related = id;
                    long networkOfferingId = offering.getId();
                    Mode mode = Mode.Static;
                    
                    BroadcastDomainType broadcastDomainType = null;
                    TrafficType trafficType= offering.getTrafficType();
                    
                    boolean isNetworkDefault = false;
                    if (trafficType == TrafficType.Management || trafficType == TrafficType.Storage) {
                        broadcastDomainType = BroadcastDomainType.Native;
                    } else if (trafficType == TrafficType.Control) {
                        broadcastDomainType = BroadcastDomainType.LinkLocal;
                    } else if (offering.getTrafficType() == TrafficType.Public) {
                        if (zone.getNetworkType() == NetworkType.Advanced) {
                            broadcastDomainType = BroadcastDomainType.Vlan;
                        } else {
                            continue;
                        }
                    } else if (offering.getTrafficType() == TrafficType.Guest) {
                        if (zone.getNetworkType() == NetworkType.Basic) {
                            isNetworkDefault = true;
                            broadcastDomainType = BroadcastDomainType.Native;
                        } else {
                            continue;
                        }
                    }
                    
                    if (broadcastDomainType != null) {
                        NetworkVO network = new NetworkVO(id, trafficType, null, mode, broadcastDomainType, networkOfferingId, zoneId, domainId, accountId, related, null, null, true, isNetworkDefault);
                        network.setGuruName(guruNames.get(network.getTrafficType()));
                        network.setDns1(zone.getDns1());
                        network.setDns2(zone.getDns2());
                        network.setState(State.Implemented);
                        _networkDao.persist(network, false);
                        id++;
                    }
                } 
            }
        }
    }
    
    
    private void updateVlanWithNetworkId(VlanVO vlan) {
        long zoneId = vlan.getDataCenterId();
        long networkId = 0L;
        DataCenterVO zone = _zoneDao.findById(zoneId);
        
        if (zone.getNetworkType() == NetworkType.Advanced) {
            networkId = getSystemNetworkIdByZoneAndTrafficType(zoneId, TrafficType.Public); 
        } else {
            networkId = getSystemNetworkIdByZoneAndTrafficType(zoneId, TrafficType.Guest);
        }
        
        vlan.setNetworkId(networkId);
        _vlanDao.update(vlan.getId(), vlan);
    }
    
    private long getSystemNetworkIdByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        //find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering: offerings) {
            if (offering.getTrafficType() == trafficType) {
                networkOfferingId = offering.getId();
                break;
            }
        }
        
        if (networkOfferingId == null) {
            throw new InvalidParameterValueException("Unable to find system network offering with traffic type " + trafficType);
        }
        
        List<NetworkVO> networks = _networkDao.listBy(Account.ACCOUNT_ID_SYSTEM, networkOfferingId, zoneId);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with traffic type " + trafficType + " in zone " + zoneId);
        }
        return networks.get(0).getId();
    }
    
}
