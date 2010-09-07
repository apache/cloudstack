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
import java.io.IOException;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.user.User;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public class ConfigurationServerImpl implements ConfigurationServer {
	public static final Logger s_logger = Logger.getLogger(ConfigurationServerImpl.class.getName());
	
	private final ConfigurationDao _configDao;
	private final ConfigurationManager _configMgr;
	private final SnapshotPolicyDao _snapPolicyDao;
	
	public ConfigurationServerImpl() {
		ComponentLocator locator = ComponentLocator.getLocator(Name);
		_configDao = locator.getDao(ConfigurationDao.class);
		_configMgr = locator.getManager(ConfigurationManager.class);
		_snapPolicyDao = locator.getDao(SnapshotPolicyDao.class);
	}

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
				if (!_configMgr.isPremium() && category.equals("Premium")) {
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
			if (_configMgr.isPremium()) {
				_configDao.update("network.type", "vlan");
				s_logger.debug("ConfigurationServer changed the network type to \"vlan\".");
				
				_configDao.update("hypervisor.type", "xenserver");
				s_logger.debug("ConfigurationServer changed the hypervisor type to \"xenserver\".");
				
				_configDao.update("secondary.storage.vm", "true");
				s_logger.debug("ConfigurationServer made secondary storage vm required.");
				
				_configDao.update("secstorage.encrypt.copy", "true");
				s_logger.debug("ConfigurationServer made secondary storage copy encrypted.");
				
				_configDao.update("secstorage.secure.copy.cert", "realhostip");
				s_logger.debug("ConfigurationServer made secondary storage copy use realhostip.");
				
	             //Add default manual snapshot policy
	            SnapshotPolicyVO snapPolicy = new SnapshotPolicyVO(0L, "00", "GMT", (short)4, 0);
	            _snapPolicyDao.persist(snapPolicy);
	            
	            // Save Virtual Networking service offerings
	            _configMgr.createServiceOffering(User.UID_SYSTEM, "Small Instance, Virtual Networking", 1, 512, 500, "Small Instance, Virtual Networking, $0.05 per hour", false, false, true, null);
	            _configMgr.createServiceOffering(User.UID_SYSTEM, "Medium Instance, Virtual Networking", 1, 1024, 1000, "Medium Instance, Virtual Networking, $0.10 per hour", false, false, true, null);
			}
			
			boolean externalIpAlloator = Boolean.parseBoolean(_configDao.getValue("direct.attach.network.externalIpAllocator.enabled"));
			String hyperVisor = _configDao.getValue("hypervisor.type");
			if (hyperVisor.equalsIgnoreCase("KVM") && !externalIpAlloator) {
				/*For KVM, it's enabled by default*/
				_configDao.update("direct.attach.network.externalIpAllocator.enabled", "true");
			}
			
			// Save Direct Networking service offerings
			_configMgr.createServiceOffering(User.UID_SYSTEM, "Small Instance, Direct Networking", 1, 512, 500, "Small Instance, Direct Networking, $0.05 per hour", false, false, false, null);			
			_configMgr.createServiceOffering(User.UID_SYSTEM, "Medium Instance, Direct Networking", 1, 1024, 1000, "Medium Instance, Direct Networking, $0.10 per hour", false, false, false, null);			
			
			// Save default disk offerings
			_configMgr.createDiskOffering(DomainVO.ROOT_DOMAIN, "Small", "Small Disk, 5 GB", 5, false, null);
			_configMgr.createDiskOffering(DomainVO.ROOT_DOMAIN, "Medium", "Medium Disk, 20 GB", 20, false, null);
			_configMgr.createDiskOffering(DomainVO.ROOT_DOMAIN, "Large", "Large Disk, 100 GB", 100, false, null);

			// Save the mount parent to the configuration table
			String mountParent = getMountParent();
			if (mountParent != null) {
				_configMgr.updateConfiguration(User.UID_SYSTEM, "mount.parent", mountParent);
				s_logger.debug("ConfigurationServer saved \"" + mountParent + "\" as mount.parent.");
			} else {
				s_logger.debug("ConfigurationServer could not detect mount.parent.");
			}

			String hostIpAdr = getHost();
			if (hostIpAdr != null) {
				_configMgr.updateConfiguration(User.UID_SYSTEM, "host", hostIpAdr);
				s_logger.debug("ConfigurationServer saved \"" + hostIpAdr + "\" as host.");
			}

			// Get the gateway and netmask of this machine
			String[] gatewayAndNetmask = getGatewayAndNetmask();

			if (gatewayAndNetmask != null) {
				String gateway = gatewayAndNetmask[0];
				String netmask = gatewayAndNetmask[1];
				long cidrSize = NetUtils.getCidrSize(netmask);

				// Create a default zone
				String dns = getDNS();
				if (dns == null) {
					dns = "4.2.2.2";
				}
				DataCenterVO zone = _configMgr.createZone(User.UID_SYSTEM, "Default", dns, null, dns, null, "1000-2000","10.1.1.0/24");

				// Create a default pod
				String networkType = _configDao.getValue("network.type");
				if (networkType != null && networkType.equals("vnet")) {
					_configMgr.createPod(User.UID_SYSTEM, "Default", zone.getId(), "169.254.1.1", "169.254.1.0/24", "169.254.1.2", "169.254.1.254");
				} else {
					_configMgr.createPod(User.UID_SYSTEM, "Default", zone.getId(), gateway, gateway + "/" + cidrSize, null, null);
				}

				s_logger.debug("ConfigurationServer saved a default pod and zone, with gateway: " + gateway + " and netmask: " + netmask);
			} else {
				s_logger.debug("ConfigurationServer could not detect the gateway and netmask of the management server.");
			}

	        // generate a single sign-on key
	        updateSSOKey();
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

	/*
	private String getManagementNetworkCIDR() {
		String[] gatewayAndNetmask = getGatewayAndNetmask();
		
		if (gatewayAndNetmask == null) {
			return null;
		} else {
			String gateway = gatewayAndNetmask[0];
			String netmask = gatewayAndNetmask[1];
			
			String subnet = NetUtils.getSubNet(gateway, netmask);
			long cidrSize = NetUtils.getCidrSize(netmask);
			
			return subnet + "/" + cidrSize;
		}
	}
	*/

	private String[] getGatewayAndNetmask() {
		String defaultRoute = Script.runSimpleBashScript("/sbin/route | grep default");
		
		if (defaultRoute == null) {
			return null;
		}
		
		String[] defaultRouteList = defaultRoute.split("\\s+");
		
		if (defaultRouteList.length < 5) {
			return null;
		}
		
		String gateway = defaultRouteList[1];
		String ethDevice = defaultRouteList[7];
		String netmask = null;
		
		if (ethDevice != null) {
			netmask = Script.runSimpleBashScript("/sbin/ifconfig " + ethDevice + " | grep Mask | awk '{print $4}' | cut -d':' -f2");
		}
			
		if (gateway == null || netmask == null) {
			return null;
		} else if (!NetUtils.isValidIp(gateway) || !NetUtils.isValidNetmask(netmask)) {
			return null;
		} else {
			return new String[] {gateway, netmask};
		}
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
	
	private String getDNS() {
		String dnsLine = Script.runSimpleBashScript("grep nameserver /etc/resolv.conf");
		if (dnsLine == null) {
			return null;
		} else {
			String[] dnsLineArray = dnsLine.split(" ");
			if (dnsLineArray.length != 2) {
				return null;
			} else {
				return dnsLineArray[1];
			}
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

        if (already == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Need to store in the database");
            }

            String homeDir = Script.runSimpleBashScript("echo ~");
            if (homeDir == "~") {
                s_logger.warn("No home directory was detected.  Trouble with SSH keys ahead.");
                return;
            }

            File privkeyfile = new File(homeDir + "/.ssh/id_rsa");
            File pubkeyfile  = new File(homeDir + "/.ssh/id_rsa.pub");
            byte[] arr1 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(privkeyfile)).readFully(arr1);
            } catch (EOFException e) {
            } catch (Exception e) {
                s_logger.warn("Cannot read the private key file",e);
                return;
            }
            String privateKey = new String(arr1).trim();
            byte[] arr2 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(pubkeyfile)).readFully(arr2);
            } catch (EOFException e) {			    
            } catch (Exception e) {
                s_logger.warn("Cannot read the public key file",e);
                return;
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
                s_logger.warn("SQL of the private key failed",ex);
            }

            try {
                PreparedStatement stmt2 = txn.prepareAutoCloseStatement(insertSql2);
                stmt2.executeUpdate();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Public key inserted into database");
                }
            } catch (SQLException ex) {
                s_logger.warn("SQL of the public key failed",ex);
            }
        }
    }

	@DB
	protected void generateSecStorageVmCopyPassword() {
		String already = _configDao.getValue("secstorage.copy.password");
		
		if (already == null) {
		
			s_logger.info("Need to store secondary storage vm copy password in the database");
			String password = PasswordGenerator.generateRandomPassword();

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
}
