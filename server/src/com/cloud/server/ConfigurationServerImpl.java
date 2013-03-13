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
package com.cloud.server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceCountDao;
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
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.guru.ControlNetworkGuru;
import com.cloud.network.guru.DirectPodBasedNetworkGuru;
import com.cloud.network.guru.PodBasedNetworkGuru;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.guru.StorageNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.test.IPRangeConfig;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.cloud.uuididentity.dao.IdentityDao;
import org.apache.cloudstack.region.RegionVO;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

@Component
public class ConfigurationServerImpl extends ManagerBase implements ConfigurationServer {
    public static final Logger s_logger = Logger.getLogger(ConfigurationServerImpl.class.getName());

    @Inject private ConfigurationDao _configDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private HostPodDao _podDao;
    @Inject private DiskOfferingDao _diskOfferingDao;
    @Inject private ServiceOfferingDao _serviceOfferingDao;
    @Inject private NetworkOfferingDao _networkOfferingDao;
    @Inject private DataCenterDao _dataCenterDao;
    @Inject private NetworkDao _networkDao;
    @Inject private VlanDao _vlanDao;
    private String _domainSuffix;
    @Inject private DomainDao _domainDao;
    @Inject private AccountDao _accountDao;
    @Inject private ResourceCountDao _resourceCountDao;
    @Inject private NetworkOfferingServiceMapDao _ntwkOfferingServiceMapDao;
    @Inject private IdentityDao _identityDao;

    public ConfigurationServerImpl() {
    	setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK_BOOTSTRAP);
    }

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {

		try {
			persistDefaultValues();
		} catch (InternalErrorException e) {
			throw new RuntimeException("Unhandled configuration exception", e);
		}
		return true;
    }

    @Override
    @DB
    public void persistDefaultValues() throws InternalErrorException {

    	fixupScriptFileAttribute();
    	
        // Create system user and admin user
        saveUser();

        // Get init
        String init = _configDao.getValue("init");

        // Get domain suffix - needed for network creation
        _domainSuffix = _configDao.getValue("guest.domain.suffix");

        if (init == null || init.equals("false")) {
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

                    // if the config value already present in the db, don't insert it again
                    if (_configDao.findByName(name) != null) {
                        continue;
                    }

                    String instance = "DEFAULT";
                    String component = c.getComponent();
                    String value = c.getDefaultValue();
                    value = ("Hidden".equals(category) || "Secure".equals(category)) ? DBEncryptionUtil.encrypt(value) : value;
                    String description = c.getDescription();
                    ConfigurationVO configVO = new ConfigurationVO(category, instance, component, name, value, description);
                    _configDao.persist(configVO);
                }
            }

            _configDao.update(Config.UseSecondaryStorageVm.key(), Config.UseSecondaryStorageVm.getCategory(), "true");
            s_logger.debug("ConfigurationServer made secondary storage vm required.");

            _configDao.update(Config.SecStorageEncryptCopy.key(), Config.SecStorageEncryptCopy.getCategory(), "true");
            s_logger.debug("ConfigurationServer made secondary storage copy encrypted.");

            _configDao.update("secstorage.secure.copy.cert", "realhostip");
            s_logger.debug("ConfigurationServer made secondary storage copy use realhostip.");

            // Save default service offerings
            createServiceOffering(User.UID_SYSTEM, "Small Instance", 1, 512, 500, "Small Instance", false, false, null);
            createServiceOffering(User.UID_SYSTEM, "Medium Instance", 1, 1024, 1000, "Medium Instance", false, false, null);
            // Save default disk offerings
            createdefaultDiskOffering(null, "Small", "Small Disk, 5 GB", 5, null, false, false);
            createdefaultDiskOffering(null, "Medium", "Medium Disk, 20 GB", 20, null, false, false);
            createdefaultDiskOffering(null, "Large", "Large Disk, 100 GB", 100, null, false, false);
            createdefaultDiskOffering(null, "Large", "Large Disk, 100 GB", 100, null, false, false);
            createdefaultDiskOffering(null, "Custom", "Custom Disk", 0, null, true, false);

            // Save the mount parent to the configuration table
            String mountParent = getMountParent();
            if (mountParent != null) {
                _configDao.update(Config.MountParent.key(), Config.MountParent.getCategory(), mountParent);
                s_logger.debug("ConfigurationServer saved \"" + mountParent + "\" as mount.parent.");
            } else {
                s_logger.debug("ConfigurationServer could not detect mount.parent.");
            }

            String hostIpAdr = NetUtils.getDefaultHostIp();
            if (hostIpAdr != null) {
                _configDao.update(Config.ManagementHostIPAdr.key(), Config.ManagementHostIPAdr.getCategory(), hostIpAdr);
                s_logger.debug("ConfigurationServer saved \"" + hostIpAdr + "\" as host.");
            }

            // generate a single sign-on key
            updateSSOKey();

            // Create default network offerings
            createDefaultNetworkOfferings();

            // Create default networks
            createDefaultNetworks();

            // Create userIpAddress ranges

            // Update existing vlans with networkId
            Transaction txn = Transaction.currentTxn();

            List<VlanVO> vlans = _vlanDao.listAll();
            if (vlans != null && !vlans.isEmpty()) {
                for (VlanVO vlan : vlans) {
                    if (vlan.getNetworkId().longValue() == 0) {
                        updateVlanWithNetworkId(vlan);
                    }

                    // Create vlan user_ip_address range
                    String ipPange = vlan.getIpRange();
                    String[] range = ipPange.split("-");
                    String startIp = range[0];
                    String endIp = range[1];

                    txn.start();
                    IPRangeConfig config = new IPRangeConfig();
                    long startIPLong = NetUtils.ip2Long(startIp);
                    long endIPLong = NetUtils.ip2Long(endIp);
                    config.savePublicIPRange(txn, startIPLong, endIPLong, vlan.getDataCenterId(), vlan.getId(), vlan.getNetworkId(), vlan.getPhysicalNetworkId());
                    txn.commit();
                }
            }
        }
        // Update resource count if needed
        updateResourceCount();

        // keystore for SSL/TLS connection
        updateSSLKeystore();

        // store the public and private keys in the database
        updateKeyPairs();

        // generate a random password for system vm
        updateSystemvmPassword();

        // generate a random password used to authenticate zone-to-zone copy
        generateSecStorageVmCopyPassword();

        // Update the cloud identifier
        updateCloudIdentifier();

        // We should not update seed data UUID column here since this will be invoked in upgrade case as well.
        //updateUuids();
        // Set init to true
        _configDao.update("init", "Hidden", "true");

        // invalidate cache in DAO as we have changed DB status
        _configDao.invalidateCache();
    }

    /*
    private void updateUuids() {
        _identityDao.initializeDefaultUuid("disk_offering");
        _identityDao.initializeDefaultUuid("network_offerings");
        _identityDao.initializeDefaultUuid("vm_template");
        _identityDao.initializeDefaultUuid("user");
        _identityDao.initializeDefaultUuid("domain");
        _identityDao.initializeDefaultUuid("account");
        _identityDao.initializeDefaultUuid("guest_os");
        _identityDao.initializeDefaultUuid("guest_os_category");
        _identityDao.initializeDefaultUuid("hypervisor_capabilities");
        _identityDao.initializeDefaultUuid("snapshot_policy");
        _identityDao.initializeDefaultUuid("security_group");
        _identityDao.initializeDefaultUuid("security_group_rule");
        _identityDao.initializeDefaultUuid("physical_network");
        _identityDao.initializeDefaultUuid("physical_network_traffic_types");
        _identityDao.initializeDefaultUuid("physical_network_service_providers");
        _identityDao.initializeDefaultUuid("virtual_router_providers");
        _identityDao.initializeDefaultUuid("networks");
        _identityDao.initializeDefaultUuid("user_ip_address");
        _identityDao.initializeDefaultUuid("counter");
    }
    */

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
    protected void saveUser() {
        int region_id = _configDao.getRegionId();
        // insert system account
        String insertSql = "INSERT INTO `cloud`.`account` (id, uuid, account_name, type, domain_id, region_id) VALUES (1, UUID(), 'system', '1', '1', ?)";
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.setInt(1, region_id);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }
        // insert system user
        insertSql = "INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname, lastname, created, region_id)" +
                " VALUES (1, UUID(), 'system', RAND(), 1, 'system', 'cloud', now(), ?)";
        txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.setInt(1, region_id);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }

        // insert admin user, but leave the account disabled until we set a
        // password with the user authenticator
        long id = 2;
        String username = "admin";
        String firstname = "admin";
        String lastname = "cloud";

        // create an account for the admin user first
        insertSql = "INSERT INTO `cloud`.`account` (id, uuid, account_name, type, domain_id, region_id) VALUES (" + id + ", UUID(), '" + username + "', '1', '1', ?)";
        txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.setInt(1, region_id);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }

        // now insert the user
        insertSql = "INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname, lastname, created, state, region_id) " +
                "VALUES (" + id + ", UUID(), '" + username + "', RAND(), 2, '" + firstname + "','" + lastname + "',now(), 'disabled', ?)";

        txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.setInt(1, region_id);
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }

        try {
            String tableName = "security_group";
            try {
                String checkSql = "SELECT * from network_group";
                PreparedStatement stmt = txn.prepareAutoCloseStatement(checkSql);
                stmt.executeQuery();
                tableName = "network_group";
            } catch (Exception ex) {
                // if network_groups table exists, create the default security group there
            }

            insertSql = "SELECT * FROM " + tableName + " where account_id=2 and name='default'";
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                // save default security group
                if (tableName.equals("security_group")) {
                    insertSql = "INSERT INTO " + tableName + " (uuid, name, description, account_id, domain_id) " +
                            "VALUES (UUID(), 'default', 'Default Security Group', 2, 1)";
                } else {
                    insertSql = "INSERT INTO " + tableName + " (name, description, account_id, domain_id, account_name) " +
                            "VALUES ('default', 'Default Security Group', 2, 1, 'admin')";
                }

                txn = Transaction.currentTxn();
                try {
                    stmt = txn.prepareAutoCloseStatement(insertSql);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    s_logger.warn("Failed to create default security group for default admin account due to ", ex);
                }
            }
            rs.close();
        } catch (Exception ex) {
            s_logger.warn("Failed to create default security group for default admin account due to ", ex);
        }
    }

    protected void updateCloudIdentifier() {
        // Creates and saves a UUID as the cloud identifier
        String currentCloudIdentifier = _configDao.getValue("cloud.identifier");
        if (currentCloudIdentifier == null || currentCloudIdentifier.isEmpty()) {
            String uuid = UUID.randomUUID().toString();
            _configDao.update(Config.CloudIdentifier.key(), Config.CloudIdentifier.getCategory(), uuid);
        }
    }

    private String getBase64Keystore(String keystorePath) throws IOException {
        byte[] storeBytes = new byte[4094];
        int len = 0;
        try {
            len = new FileInputStream(keystorePath).read(storeBytes);
        } catch (EOFException e) {
        } catch (Exception e) {
            throw new IOException("Cannot read the generated keystore file");
        }
        if (len > 3000) { // Base64 codec would enlarge data by 1/3, and we have 4094 bytes in database entry at most
            throw new IOException("KeyStore is too big for database! Length " + len);
        }

        byte[] encodeBytes = new byte[len];
        System.arraycopy(storeBytes, 0, encodeBytes, 0, len);

        return new String(Base64.encodeBase64(encodeBytes));
    }

    private void generateDefaultKeystore(String keystorePath) throws IOException {
        String cn = "Cloudstack User";
        String ou;

        try {
            ou = InetAddress.getLocalHost().getCanonicalHostName();
            String[] group = ou.split("\\.");

            // Simple check to see if we got IP Address...
            boolean isIPAddress = Pattern.matches("[0-9]$", group[group.length - 1]);
            if (isIPAddress) {
                ou = "cloud.com";
            } else {
                ou = group[group.length - 1];
                for (int i = group.length - 2; i >= 0 && i >= group.length - 3; i--)
                    ou = group[i] + "." + ou;
            }
        } catch (UnknownHostException ex) {
            s_logger.info("Fail to get user's domain name. Would use cloud.com. ", ex);
            ou = "cloud.com";
        }

        String o = ou;
        String c = "Unknown";
        String dname = "cn=\"" + cn + "\",ou=\"" + ou + "\",o=\"" + o + "\",c=\"" + c + "\"";
        Script script = new Script(true, "keytool", 5000, null);
        script.add("-genkey");
        script.add("-keystore", keystorePath);
        script.add("-storepass", "vmops.com");
        script.add("-keypass", "vmops.com");
        script.add("-keyalg", "RSA");
        script.add("-validity", "3650");
        script.add("-dname", dname);
        String result = script.execute();
        if (result != null) {
            throw new IOException("Fail to generate certificate!: " + result);
        }
    }

    protected void updateSSLKeystore() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Processing updateSSLKeyStore");
        }

        String dbString = _configDao.getValue("ssl.keystore");
        File confFile = PropertiesUtil.findConfigFile("db.properties");
        /* This line may throw a NPE, but that's due to fail to find db.properities, meant some bugs in the other places */
        String confPath = confFile.getParent();
        String keystorePath = confPath + "/cloud.keystore";
        File keystoreFile = new File(keystorePath);
        boolean dbExisted = (dbString != null && !dbString.isEmpty());

        s_logger.info("SSL keystore located at " + keystorePath);
        try {
            if (!dbExisted) {
                if (!keystoreFile.exists()) {
                    generateDefaultKeystore(keystorePath);
                    s_logger.info("Generated SSL keystore.");
                }
                String base64Keystore = getBase64Keystore(keystorePath);
                ConfigurationVO configVO = new ConfigurationVO("Hidden", "DEFAULT", "management-server", "ssl.keystore", DBEncryptionUtil.encrypt(base64Keystore), "SSL Keystore for the management servers");
                _configDao.persist(configVO);
                s_logger.info("Stored SSL keystore to database.");
            } else if (keystoreFile.exists()) { // and dbExisted
                // Check if they are the same one, otherwise override with local keystore
                String base64Keystore = getBase64Keystore(keystorePath);
                if (base64Keystore.compareTo(dbString) != 0) {
                    _configDao.update("ssl.keystore", "Hidden", base64Keystore);
                    s_logger.info("Updated database keystore with local one.");
                }
            } else { // !keystoreFile.exists() and dbExisted
                // Export keystore to local file
                byte[] storeBytes = Base64.decodeBase64(dbString);
                try {
                    String tmpKeystorePath = "/tmp/tmpkey";
                    FileOutputStream fo = new FileOutputStream(tmpKeystorePath);
                    fo.write(storeBytes);
                    fo.close();
                    Script script = new Script(true, "cp", 5000, null);
                    script.add(tmpKeystorePath);
                    script.add(keystorePath);
                    String result = script.execute();
                    if (result != null) {
                        throw new IOException();
                    }
                } catch (Exception e) {
                    throw new IOException("Fail to create keystore file!", e);
                }
                s_logger.info("Stored database keystore to local.");
            }
        } catch (Exception ex) {
            s_logger.warn("Would use fail-safe keystore to continue.", ex);
        }
    }

    @DB
    protected void updateSystemvmPassword() {
        String userid = System.getProperty("user.name");
        if (!userid.startsWith("cloud")) {
            return;
        }

        if (!Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            return;
        }

        String already = _configDao.getValue("system.vm.password");
        if (already == null) {
            Transaction txn = Transaction.currentTxn();
            try {
                String rpassword = PasswordGenerator.generatePresharedKey(8);
                String wSql = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) "
                        + "VALUES ('Secure','DEFAULT', 'management-server','system.vm.password', '" + DBEncryptionUtil.encrypt(rpassword)
                        + "','randmon password generated each management server starts for system vm')";
                PreparedStatement stmt = txn.prepareAutoCloseStatement(wSql);
                stmt.executeUpdate(wSql);
                s_logger.info("Updated systemvm password in database");
            } catch (SQLException e) {
                s_logger.error("Cannot retrieve systemvm password", e);
            }
        }

    }

    @Override
    @DB
    public void updateKeyPairs() {
        // Grab the SSH key pair and insert it into the database, if it is not present

        String username = System.getProperty("user.name");
        Boolean devel = Boolean.valueOf(_configDao.getValue("developer"));
        if (!username.equalsIgnoreCase("cloud") && !devel) {
            s_logger.warn("Systemvm keypairs could not be set. Management server should be run as cloud user, or in development mode.");
            return;
        }
        String already = _configDao.getValue("ssh.privatekey");
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            throw new CloudRuntimeException("Cannot get home directory for account: " + username);
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Processing updateKeyPairs");
        }

        if (homeDir != null && homeDir.startsWith("~")) {
            s_logger.error("No home directory was detected for the user '" + username + "'. Please check the profile of this user.");
            throw new CloudRuntimeException("No home directory was detected for the user '" + username + "'. Please check the profile of this user.");
        }

        // Using non-default file names (id_rsa.cloud and id_rsa.cloud.pub) in developer mode. This is to prevent SSH keys overwritten for user running management server
        File privkeyfile = null;
        File pubkeyfile = null;
        if (devel) {
            privkeyfile = new File(homeDir + "/.ssh/id_rsa.cloud");
            pubkeyfile = new File(homeDir + "/.ssh/id_rsa.cloud.pub");
        } else {
            privkeyfile = new File(homeDir + "/.ssh/id_rsa");
            pubkeyfile = new File(homeDir + "/.ssh/id_rsa.pub");
        }

        if (already == null || already.isEmpty()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Systemvm keypairs not found in database. Need to store them in the database");
            }
            // FIXME: take a global database lock here for safety.
            Script.runSimpleBashScript("if [ -f " + privkeyfile + " ]; then rm -f " + privkeyfile + "; fi; ssh-keygen -t rsa -N '' -f " + privkeyfile + " -q");

            byte[] arr1 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(privkeyfile)).readFully(arr1);
            } catch (EOFException e) {
            } catch (Exception e) {
                s_logger.error("Cannot read the private key file", e);
                throw new CloudRuntimeException("Cannot read the private key file");
            }
            String privateKey = new String(arr1).trim();
            byte[] arr2 = new byte[4094]; // configuration table column value size
            try {
                new DataInputStream(new FileInputStream(pubkeyfile)).readFully(arr2);
            } catch (EOFException e) {
            } catch (Exception e) {
                s_logger.warn("Cannot read the public key file", e);
                throw new CloudRuntimeException("Cannot read the public key file");
            }
            String publicKey = new String(arr2).trim();

            String insertSql1 = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                    "VALUES ('Hidden','DEFAULT', 'management-server','ssh.privatekey', '" + DBEncryptionUtil.encrypt(privateKey) + "','Private key for the entire CloudStack')";
            String insertSql2 = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                    "VALUES ('Hidden','DEFAULT', 'management-server','ssh.publickey', '" + DBEncryptionUtil.encrypt(publicKey) + "','Public key for the entire CloudStack')";

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
                stmt1.executeUpdate();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Private key inserted into database");
                }
            } catch (SQLException ex) {
                s_logger.error("SQL of the private key failed", ex);
                throw new CloudRuntimeException("SQL of the private key failed");
            }

            try {
                PreparedStatement stmt2 = txn.prepareAutoCloseStatement(insertSql2);
                stmt2.executeUpdate();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Public key inserted into database");
                }
            } catch (SQLException ex) {
                s_logger.error("SQL of the public key failed", ex);
                throw new CloudRuntimeException("SQL of the public key failed");
            }

        } else {
            s_logger.info("Keypairs already in database, updating local copy");
            updateKeyPairsOnDisk(homeDir);
        }
        s_logger.info("Going to update systemvm iso with generated keypairs if needed");
        try {
            injectSshKeysIntoSystemVmIsoPatch(pubkeyfile.getAbsolutePath(), privkeyfile.getAbsolutePath());
        } catch (CloudRuntimeException e) {
            if (!devel) {
                throw new CloudRuntimeException(e.getMessage());
            }
        }
    }

    private void writeKeyToDisk(String key, String keyPath) {
        File keyfile = new File(keyPath);
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
    
	private void fixupScriptFileAttribute() {
		// TODO : this is a hacking fix to workaround that executable bit is not preserved in WAR package 
        String scriptPath = Script.findScript("", "scripts/vm/systemvm/injectkeys.sh");
        if(scriptPath != null) {
        	File file = new File(scriptPath);
        	if(!file.canExecute()) {
        		s_logger.info("Some of the shell script files may not have executable bit set. Fixup...");
        		
        		String cmd = "chmod ugo+x " + scriptPath;
        		s_logger.info("Executing " + cmd);
                String result = Script.runSimpleBashScript(cmd);
                if (result != null) {
                    s_logger.warn("Failed to fixup shell script executable bits " + result);
                }
        	}
        }
	}

    private void updateKeyPairsOnDisk(String homeDir) {
        File keyDir = new File(homeDir + "/.ssh");
        Boolean devel = Boolean.valueOf(_configDao.getValue("developer"));
        if (!keyDir.isDirectory()) {
            s_logger.warn("Failed to create " + homeDir + "/.ssh for storing the SSH keypars");
            keyDir.mkdir();
        }
        String pubKey = _configDao.getValue("ssh.publickey");
        String prvKey = _configDao.getValue("ssh.privatekey");

        // Using non-default file names (id_rsa.cloud and id_rsa.cloud.pub) in developer mode. This is to prevent SSH keys overwritten for user running management server
        if( devel ) {
            writeKeyToDisk(prvKey, homeDir + "/.ssh/id_rsa.cloud");
            writeKeyToDisk(pubKey, homeDir + "/.ssh/id_rsa.cloud.pub");
        } else {
            writeKeyToDisk(prvKey, homeDir + "/.ssh/id_rsa");
            writeKeyToDisk(pubKey, homeDir + "/.ssh/id_rsa.pub");
        }
    }

    protected void injectSshKeysIntoSystemVmIsoPatch(String publicKeyPath, String privKeyPath) {
        String injectScript = "scripts/vm/systemvm/injectkeys.sh";
        String scriptPath = Script.findScript("", injectScript);
        String systemVmIsoPath = Script.findScript("", "vms/systemvm.iso");
        if (scriptPath == null) {
            throw new CloudRuntimeException("Unable to find key inject script " + injectScript);
        }
        if (systemVmIsoPath == null) {
            throw new CloudRuntimeException("Unable to find systemvm iso vms/systemvm.iso");
        }
        final Script command = new Script(scriptPath, s_logger);
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
                    "VALUES ('Hidden','DEFAULT', 'management-server','secstorage.copy.password', '" + DBEncryptionUtil.encrypt(password) + "','Password used to authenticate zone-to-zone template copy requests')";

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
                stmt1.executeUpdate();
                s_logger.debug("secondary storage vm copy password inserted into database");
            } catch (SQLException ex) {
                s_logger.warn("Failed to insert secondary storage vm copy password", ex);
            }

        }
    }

    private void updateSSOKey() {
        try {
            String encodedKey = null;

            // Algorithm for SSO Keys is SHA1, should this be configurable?
            KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
            SecretKey key = generator.generateKey();
            encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());

            _configDao.update(Config.SSOKey.key(), Config.SSOKey.getCategory(), encodedKey);
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating sso key", ex);
        }
    }

    @DB
    protected HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp) throws InternalErrorException {
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
            /* local link ip address starts from 169.254.0.2 - 169.254.(nums) */
            String[] linkLocalIpRanges = NetUtils.getLinkLocalIPRange(nums);
            if (linkLocalIpRanges == null) {
                throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
            } else {
                _zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
            }

            txn.commit();

        } catch (Exception e) {
            txn.rollback();
            s_logger.error("Unable to create new pod due to " + e.getMessage(), e);
            throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
        }

        return pod;
    }

    private DiskOfferingVO createdefaultDiskOffering(Long domainId, String name, String description, int numGibibytes, String tags, boolean isCustomized, boolean isSystemUse) {
        long diskSize = numGibibytes;
        diskSize = diskSize * 1024 * 1024 * 1024;
        tags = cleanupTags(tags);

        DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, diskSize, tags, isCustomized);
        newDiskOffering.setUniqueName("Cloud.Com-" + name);
        newDiskOffering.setSystemUse(isSystemUse);
        newDiskOffering = _diskOfferingDao.persistDeafultDiskOffering(newDiskOffering);
        return newDiskOffering;
    }

    private ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, String tags) {
        tags = cleanupTags(tags);
        ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, offerHA, displayText, localStorageRequired, false, tags, false, null, false);
        offering.setUniqueName("Cloud.Com-" + name);
        offering = _serviceOfferingDao.persistSystemServiceOffering(offering);
        return offering;
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

    @DB
    protected void createDefaultNetworkOfferings() {

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemPublicNetwork,
                TrafficType.Public, true);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemManagementNetwork,
                TrafficType.Management, false);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemControlNetwork,
                TrafficType.Control, false);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemStorageNetwork,
                TrafficType.Storage, true);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        NetworkOfferingVO privateGatewayNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemPrivateGatewayNetworkOffering, GuestType.Isolated);
        privateGatewayNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(privateGatewayNetworkOffering);

        //populate providers
        Map<Network.Service, Network.Provider> defaultSharedNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultSharedNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);

        Map<Network.Service, Network.Provider> defaultIsolatedNetworkOfferingProviders = defaultSharedNetworkOfferingProviders;

        Map<Network.Service, Network.Provider> defaultSharedSGNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultSharedSGNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.SecurityGroup, Provider.SecurityGroupProvider);

        Map<Network.Service, Network.Provider> defaultIsolatedSourceNatEnabledNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Firewall, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Gateway, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Lb, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.SourceNat, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.StaticNat, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.PortForwarding, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Vpn, Provider.VirtualRouter);

        Map<Network.Service, Network.Provider> netscalerServiceProviders = new HashMap<Network.Service, Network.Provider>();
        netscalerServiceProviders.put(Service.Dhcp, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.Dns, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.UserData, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.SecurityGroup, Provider.SecurityGroupProvider);
        netscalerServiceProviders.put(Service.StaticNat, Provider.Netscaler);
        netscalerServiceProviders.put(Service.Lb, Provider.Netscaler);

        // The only one diff between 1 and 2 network offerings is that the first one has SG enabled. In Basic zone only
        // first network offering has to be enabled, in Advance zone - the second one
        Transaction txn = Transaction.currentTxn();
        txn.start();

        // Offering #1
        NetworkOfferingVO defaultSharedSGNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultSharedNetworkOfferingWithSGService,
                "Offering for Shared Security group enabled networks",
                TrafficType.Guest,
                false, true, null, null, true, Availability.Optional,
                null, Network.GuestType.Shared, true, true, false);

        defaultSharedSGNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultSharedSGNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultSharedSGNetworkOffering);

        for (Service service : defaultSharedSGNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(defaultSharedSGNetworkOffering.getId(), service, defaultSharedSGNetworkOfferingProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #2
        NetworkOfferingVO defaultSharedNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultSharedNetworkOffering,
                "Offering for Shared networks",
                TrafficType.Guest,
                false, true, null, null, true, Availability.Optional,
                null, Network.GuestType.Shared, true, true, false);

        defaultSharedNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultSharedNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultSharedNetworkOffering);

        for (Service service : defaultSharedNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(defaultSharedNetworkOffering.getId(), service, defaultSharedNetworkOfferingProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #3
        NetworkOfferingVO defaultIsolatedSourceNatEnabledNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService,
                "Offering for Isolated networks with Source Nat service enabled",
                TrafficType.Guest,
                false, false, null, null, true, Availability.Required,
                null, Network.GuestType.Isolated, true, false, false);

        defaultIsolatedSourceNatEnabledNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultIsolatedSourceNatEnabledNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultIsolatedSourceNatEnabledNetworkOffering);

        for (Service service : defaultIsolatedSourceNatEnabledNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO
                    (defaultIsolatedSourceNatEnabledNetworkOffering.getId(), service, defaultIsolatedSourceNatEnabledNetworkOfferingProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #4
        NetworkOfferingVO defaultIsolatedEnabledNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultIsolatedNetworkOffering,
                "Offering for Isolated networks with no Source Nat service",
                TrafficType.Guest,
                false, true, null, null, true, Availability.Optional,
                null, Network.GuestType.Isolated, true, true, false);

        defaultIsolatedEnabledNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultIsolatedEnabledNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultIsolatedEnabledNetworkOffering);

        for (Service service : defaultIsolatedNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(defaultIsolatedEnabledNetworkOffering.getId(), service, defaultIsolatedNetworkOfferingProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #5
        NetworkOfferingVO defaultNetscalerNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultSharedEIPandELBNetworkOffering,
                "Offering for Shared networks with Elastic IP and Elastic LB capabilities",
                TrafficType.Guest,
                false, true, null, null, true, Availability.Optional,
                null, Network.GuestType.Shared, true, false, false, false, true, true, true, false, false);

        defaultNetscalerNetworkOffering.setState(NetworkOffering.State.Enabled);
        defaultNetscalerNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetscalerNetworkOffering);

        for (Service service : netscalerServiceProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(defaultNetscalerNetworkOffering.getId(), service, netscalerServiceProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #6
        NetworkOfferingVO defaultNetworkOfferingForVpcNetworks = new NetworkOfferingVO(
                NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworks,
                "Offering for Isolated Vpc networks with Source Nat service enabled",
                TrafficType.Guest,
                false, false, null, null, true, Availability.Optional,
                null, Network.GuestType.Isolated, false, false, false);

        defaultNetworkOfferingForVpcNetworks.setState(NetworkOffering.State.Enabled);
        defaultNetworkOfferingForVpcNetworks = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetworkOfferingForVpcNetworks);

        Map<Network.Service, Network.Provider> defaultVpcNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultVpcNetworkOfferingProviders.put(Service.Dhcp, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.Dns, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.UserData, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.NetworkACL, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.Gateway, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.Lb, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.SourceNat, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.StaticNat, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.PortForwarding, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProviders.put(Service.Vpn, Provider.VPCVirtualRouter);

        for (Service service : defaultVpcNetworkOfferingProviders.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO
                    (defaultNetworkOfferingForVpcNetworks.getId(), service, defaultVpcNetworkOfferingProviders.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }

        // Offering #7
        NetworkOfferingVO defaultNetworkOfferingForVpcNetworksNoLB = new NetworkOfferingVO(
                NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksNoLB,
                "Offering for Isolated Vpc networks with Source Nat service enabled and LB service Disabled",
                TrafficType.Guest,
                false, false, null, null, true, Availability.Optional,
                null, Network.GuestType.Isolated, false, false, false);

        defaultNetworkOfferingForVpcNetworksNoLB.setState(NetworkOffering.State.Enabled);
        defaultNetworkOfferingForVpcNetworksNoLB = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetworkOfferingForVpcNetworksNoLB);

        Map<Network.Service, Network.Provider> defaultVpcNetworkOfferingProvidersNoLB = new HashMap<Network.Service, Network.Provider>();
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.Dhcp, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.Dns, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.UserData, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.NetworkACL, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.Gateway, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.SourceNat, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.StaticNat, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.PortForwarding, Provider.VPCVirtualRouter);
        defaultVpcNetworkOfferingProvidersNoLB.put(Service.Vpn, Provider.VPCVirtualRouter);

        for (Service service : defaultVpcNetworkOfferingProvidersNoLB.keySet()) {
            NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO
                    (defaultNetworkOfferingForVpcNetworksNoLB.getId(), service, defaultVpcNetworkOfferingProvidersNoLB.get(service));
            _ntwkOfferingServiceMapDao.persist(offService);
            s_logger.trace("Added service for the network offering: " + offService);
        }



        txn.commit();
    }

    private void createDefaultNetworks() {
        List<DataCenterVO> zones = _dataCenterDao.listAll();
        long id = 1;

        HashMap<TrafficType, String> guruNames = new HashMap<TrafficType, String>();
        guruNames.put(TrafficType.Public, PublicNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Management, PodBasedNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Control, ControlNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Storage, StorageNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Guest, DirectPodBasedNetworkGuru.class.getSimpleName());

        for (DataCenterVO zone : zones) {
            long zoneId = zone.getId();
            long accountId = 1L;
            Long domainId = zone.getDomainId();

            if (domainId == null) {
                domainId = 1L;
            }
            // Create default networks - system only
            List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();

            for (NetworkOfferingVO offering : ntwkOff) {
                if (offering.isSystemOnly()) {
                    long related = id;
                    long networkOfferingId = offering.getId();
                    Mode mode = Mode.Static;
                    String networkDomain = null;

                    BroadcastDomainType broadcastDomainType = null;
                    TrafficType trafficType = offering.getTrafficType();

                    boolean specifyIpRanges = false;

                    if (trafficType == TrafficType.Management) {
                        broadcastDomainType = BroadcastDomainType.Native;
                    } else if (trafficType == TrafficType.Storage) {
                        broadcastDomainType = BroadcastDomainType.Native;
                        specifyIpRanges = true;
                    } else if (trafficType == TrafficType.Control) {
                        broadcastDomainType = BroadcastDomainType.LinkLocal;
                    } else if (offering.getTrafficType() == TrafficType.Public) {
                        if ((zone.getNetworkType() == NetworkType.Advanced && !zone.isSecurityGroupEnabled()) || zone.getNetworkType() == NetworkType.Basic) {
                            specifyIpRanges = true;
                            broadcastDomainType = BroadcastDomainType.Vlan;
                        } else {
                            continue;
                        }
                    }

                    if (broadcastDomainType != null) {
                        NetworkVO network = new NetworkVO(id, trafficType, mode, broadcastDomainType, networkOfferingId,
                                domainId, accountId, related, null, null, networkDomain, Network.GuestType.Shared, zoneId, null,
                                null, specifyIpRanges, null);
                        network.setGuruName(guruNames.get(network.getTrafficType()));
                        network.setDns1(zone.getDns1());
                        network.setDns2(zone.getDns2());
                        network.setState(State.Implemented);
                        _networkDao.persist(network, false, getServicesAndProvidersForNetwork(networkOfferingId));
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
        // find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering : offerings) {
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

    @DB
    public void updateResourceCount() {
        ResourceType[] resourceTypes = Resource.ResourceType.values();
        List<AccountVO> accounts = _accountDao.listAllIncludingRemoved();
        List<DomainVO> domains = _domainDao.listAllIncludingRemoved();
        List<ResourceCountVO> domainResourceCount = _resourceCountDao.listResourceCountByOwnerType(ResourceOwnerType.Domain);
        List<ResourceCountVO> accountResourceCount = _resourceCountDao.listResourceCountByOwnerType(ResourceOwnerType.Account);

        List<ResourceType> accountSupportedResourceTypes = new ArrayList<ResourceType>();
        List<ResourceType> domainSupportedResourceTypes = new ArrayList<ResourceType>();

        for (ResourceType resourceType : resourceTypes) {
            if (resourceType.supportsOwner(ResourceOwnerType.Account)) {
                accountSupportedResourceTypes.add(resourceType);
            }
            if (resourceType.supportsOwner(ResourceOwnerType.Domain)) {
                domainSupportedResourceTypes.add(resourceType);
            }
        }

        int accountExpectedCount = accountSupportedResourceTypes.size();
        int domainExpectedCount = domainSupportedResourceTypes.size();

        if ((domainResourceCount.size() < domainExpectedCount * domains.size())) {
            s_logger.debug("resource_count table has records missing for some domains...going to insert them");
            for (DomainVO domain : domains) {
                // Lock domain
                Transaction txn = Transaction.currentTxn();
                txn.start();
                _domainDao.lockRow(domain.getId(), true);
                List<ResourceCountVO> domainCounts = _resourceCountDao.listByOwnerId(domain.getId(), ResourceOwnerType.Domain);
                List<String> domainCountStr = new ArrayList<String>();
                for (ResourceCountVO domainCount : domainCounts) {
                    domainCountStr.add(domainCount.getType().toString());
                }

                if (domainCountStr.size() < domainExpectedCount) {
                    for (ResourceType resourceType : domainSupportedResourceTypes) {
                        if (!domainCountStr.contains(resourceType.toString())) {
                            ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, domain.getId(), ResourceOwnerType.Domain);
                            s_logger.debug("Inserting resource count of type " + resourceType + " for domain id=" + domain.getId());
                            _resourceCountDao.persist(resourceCountVO);
                        }
                    }
                }
                txn.commit();
            }
        }

        if ((accountResourceCount.size() < accountExpectedCount * accounts.size())) {
            s_logger.debug("resource_count table has records missing for some accounts...going to insert them");
            for (AccountVO account : accounts) {
                // lock account
                Transaction txn = Transaction.currentTxn();
                txn.start();
                _accountDao.lockRow(account.getId(), true);
                List<ResourceCountVO> accountCounts = _resourceCountDao.listByOwnerId(account.getId(), ResourceOwnerType.Account);
                List<String> accountCountStr = new ArrayList<String>();
                for (ResourceCountVO accountCount : accountCounts) {
                    accountCountStr.add(accountCount.getType().toString());
                }

                if (accountCountStr.size() < accountExpectedCount) {
                    for (ResourceType resourceType : accountSupportedResourceTypes) {
                        if (!accountCountStr.contains(resourceType.toString())) {
                            ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, account.getId(), ResourceOwnerType.Account);
                            s_logger.debug("Inserting resource count of type " + resourceType + " for account id=" + account.getId());
                            _resourceCountDao.persist(resourceCountVO);
                        }
                    }
                }

                txn.commit();
            }
        }
    }

    public Map<String, String> getServicesAndProvidersForNetwork(long networkOfferingId) {
        Map<String, String> svcProviders = new HashMap<String, String>();
        List<NetworkOfferingServiceMapVO> servicesMap = _ntwkOfferingServiceMapDao.listByNetworkOfferingId(networkOfferingId);

        for (NetworkOfferingServiceMapVO serviceMap : servicesMap) {
            if (svcProviders.containsKey(serviceMap.getService())) {
                continue;
            }
            svcProviders.put(serviceMap.getService(), serviceMap.getProvider());
        }

        return svcProviders;
    }

}
